package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.Mechanics;
import com.runecraft.content.systems.CombatAchievementService;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.bosses.BossMechanics;
import com.runecraft.osrs.content.bosses.MinigameBosses;
import com.runecraft.osrs.content.bosses.MuspahRules;
import com.runecraft.osrs.content.bosses.MuspahRules.Form;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * The Phantom Muspah (OSRS Wiki; numbers in {@link MuspahRules}).
 *
 * <p>It spawns in its ranged or melee form and swaps after 100 damage in the ranged form or 80 in the melee form (from its
 * second form on, also only after four hits), slamming up spikes as it does, one marked under the player: step off before
 * it erupts or be hit, knocked aside and heal it. Walking onto a standing spike hurts too. Ranged form: ice shards (61) and
 * now and then a charged magic orb (72, corruption: 3, 6 and 9 prayer) that the right prayer only blocks when it is up as the
 * orb is launched. Melee form: it chases, and the longer it stands still the more of its stab gets through the prayer.
 *
 * <p>At 637 and 425 health: Lightning Clouds (it teleports around a darkened arena, leaving clouds that drift across it) and
 * Homing Spikes (2-4 spikes crawl towards the player a tile every two ticks for 34 ticks, never diagonally, stopped by
 * standing spikes), Lightning Clouds first if it spawned ranged. Below 127 it teleports to the centre and, five ticks later,
 * sends out a shockwave that only a spike between it and the player stops; a kill in those ticks skips the shield (it keeps
 * one health). Then its 75-point prayer shield (Smite, sapphire bolts and corruption spells drain it faster) and Soul Split;
 * after it, its ranged form at speed 5 with spikes growing over the arena every four attacks.
 */
final class MuspahScript extends ScriptedBoss {
    enum Stage { FORMS, CLOUDS, SHOCKWAVE, SHIELDED, POST }

    Stage stage = Stage.FORMS;
    Form spawnForm;
    Form form;
    int formDamage;
    int formHits;
    boolean swapped;
    int specialsDone;
    private boolean specialQueued;
    private boolean shockwaveQueued;
    boolean shieldSkip;
    int shockwaveIn;
    int shield;
    final Set<String> drains = new HashSet<>();
    /** Standing spikes (block tiles). */
    final Set<Long> spikes = new HashSet<>();
    /** Spikes about to erupt: tile -> game tick. */
    final Map<Long, Long> erupting = new HashMap<>();
    /** Homing spikes: {x, z}. */
    final List<int[]> homing = new ArrayList<>();
    int homingTicks;
    /** Lightning clouds drifting over the arena. */
    final List<Cloud> clouds = new ArrayList<>();
    int cloudTeleports;
    private int attacksPost;
    private int attacksShielded;
    private int inPlace;
    private Vec3 lastMeleeAt;
    private int spikeFx;
    /** Corruption on players: uuid -> {pulses done, ticks to the next pulse, shockwave (1) or magic (0)}. */
    private final Map<UUID, int[]> corruption = new HashMap<>();
    private final Map<UUID, Long> walkedOn = new HashMap<>();
    private final List<Orb> orbs = new ArrayList<>();

    /** A lightning cloud: where it is, which way it drifts, ticks left, who it already hit. */
    record Cloud(double[] pos, double dx, double dz, int[] life, Set<UUID> hit) {
    }

    /** A magic orb in flight: lands at {@code due}; whether Protect from Magic was up as it was launched. */
    private record Orb(ServerPlayer target, long due, boolean prayedAtLaunch, int max) {
    }

    /** The frozen cache opens (used) into one roll of its table (OSRS Wiki "Frozen cache"). */
    static void register() {
        com.runecraft.platform.UseItemCallback.EVENT.register((player, world, hand) -> {
            net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
            if (world.isClientSide() || stack.isEmpty() || !(player instanceof ServerPlayer p)
                    || !MuspahRules.FROZEN_CACHE.equals(com.runecraft.util.Inv.idOf(stack))) {
                return net.minecraft.world.InteractionResultHolder.pass(stack);
            }
            openCache(p);
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, false);
        });
    }

    /** Opens one frozen cache; returns what it gave (null when the player has none). Public for the self-test. */
    static String openCache(ServerPlayer p) {
        if (!com.runecraft.util.Inv.has(p, MuspahRules.FROZEN_CACHE, 1)) {
            return null;
        }
        com.runecraft.util.Inv.remove(p, MuspahRules.FROZEN_CACHE, 1);
        Random r = new Random(p.getRandom().nextLong());
        MuspahRules.CacheLine line = MuspahRules.openCache(r);
        int n = line.min() + r.nextInt(line.max() - line.min() + 1);
        if (line.item().equals("ancient_essence") && n > 600) {
            n -= n % (n > 1000 ? 10 : 5); // multiples of 5 (885-995) or 10 (1970-2060)
        }
        com.runecraft.util.Inv.give(p, line.item(), n);
        com.runecraft.collection.CollectionLog.obtain(p, line.item());
        com.runecraft.osrs.ItemDef def = com.runecraft.osrs.ItemCatalog.get(line.item());
        if (line.item().equals(MuspahRules.FROZEN_CACHE)) {
            Msg.game(p, "You open the frozen cache and find... another frozen cache inside! How peculiar.");
        } else {
            Msg.game(p, "You open the frozen cache and find " + n + " x " + (def == null ? line.item() : def.name()) + ".");
        }
        return line.item();
    }

    @Override
    public void onSpawn(DataBossEntity boss) {
        home = boss.blockPosition();
        spawnForm = boss.getRandom().nextBoolean() ? Form.RANGED : Form.MELEE;
        setForm(boss, spawnForm);
        BossFx.say(boss, "The Phantom Muspah takes its " + (spawnForm == Form.RANGED ? "ranged" : "melee") + " form!");
    }

    private void setForm(DataBossEntity boss, Form f) {
        form = f;
        formDamage = 0;
        formHits = 0;
        var speed = boss.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(f == Form.MELEE && stage == Stage.FORMS ? 0.3 : 0.18);
        }
        inPlace = 0;
        lastMeleeAt = null;
    }

    boolean shieldedOrAfter() {
        return stage == Stage.SHIELDED || stage == Stage.POST || stage == Stage.SHOCKWAVE && !shieldSkip;
    }

    /** The melee form is weak to magic and strong against ranged (DPS calculator forms). */
    @Override
    public void adjustDefence(DataBossEntity boss, ServerPlayer attacker, AttackType type, com.runecraft.osrs.PlayerHitCalc.Input in) {
        if (stage != Stage.FORMS || form != Form.MELEE) {
            return;
        }
        if (type == AttackType.MAGIC) {
            in.targetDefenceBonus = MuspahRules.MELEE_FORM_MAGIC_DEFENCE;
        } else if (type == AttackType.RANGED) {
            in.targetDefenceBonus = MuspahRules.MELEE_FORM_RANGED_DEFENCE;
            in.targetRangedDefence = new int[]{MuspahRules.MELEE_FORM_RANGED_DEFENCE, MuspahRules.MELEE_FORM_RANGED_DEFENCE,
                    MuspahRules.MELEE_FORM_RANGED_DEFENCE};
        }
    }

    /** Its attack roll: the shielded and post-shield forms aim better (Ranged 336/335, Magic 180/179). */
    private long attackRoll(DataBossEntity boss, AttackType style) {
        if (!shieldedOrAfter() || style.isMelee()) {
            return BossHits.attackRoll(boss, style);
        }
        boolean shielded = stage != Stage.POST;
        int level = style == AttackType.RANGED ? (shielded ? MuspahRules.SHIELDED_RANGED_LEVEL : MuspahRules.POST_RANGED_LEVEL)
                : (shielded ? MuspahRules.SHIELDED_MAGIC_LEVEL : MuspahRules.POST_MAGIC_LEVEL);
        return com.runecraft.combat.CombatHandler.npcAttackRoll(boss, boss.stats(), style, level);
    }

    // ------------------------------------------------------------------ attacks

    @Override
    protected boolean driven() {
        return true;
    }

    @Override
    protected boolean canAttack(DataBossEntity boss) {
        return stage != Stage.CLOUDS && stage != Stage.SHOCKWAVE;
    }

    @Override
    protected boolean wantsMelee(DataBossEntity boss, ServerPlayer target) {
        return stage == Stage.FORMS && form == Form.MELEE;
    }

    @Override
    protected int attackDelay(DataBossEntity boss, AttackDef attack) {
        return stage == Stage.FORMS && form == Form.RANGED ? MuspahRules.RANGED_SPEED : MuspahRules.FAST_SPEED;
    }

    @Override
    protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
        if (stage == Stage.FORMS && form == Form.MELEE) {
            return distance <= MinigameBosses.MUSPAH_MELEE.range() ? MinigameBosses.MUSPAH_MELEE : null;
        }
        return boss.getRandom().nextInt(MuspahRules.MAGIC_ONE_IN) == 0 ? MinigameBosses.MUSPAH_SMITE : MinigameBosses.MUSPAH_RANGED;
    }

    @Override
    protected void perform(DataBossEntity boss, ServerPlayer target, AttackDef a) {
        last = a;
        attackCount++;
        Mechanics.attackListener.onAttack(boss, target, a);
        Random r = new Random(boss.getRandom().nextLong());
        switch (a.name()) {
            case "ice_shard" -> {
                // Calculated as the attack starts: Protect from Missiles must already be up.
                int d = BossHits.roll(target, AttackType.RANGED, MuspahRules.rangedMax(shieldedOrAfter()),
                        attackRoll(boss, AttackType.RANGED), false, r);
                if (BossHits.protectedFrom(target, AttackType.RANGED)) {
                    d = 0;
                }
                BossFx.world(boss).sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 1, target.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
                hit(boss, target, "ice_shard", AttackType.RANGED, d);
            }
            case "smite" -> {
                boolean prayed = BossHits.protectedFrom(target, AttackType.MAGIC);
                BossFx.warn(boss, "The Phantom Muspah launches a purple orb!");
                orbs.add(new Orb(target, now + MuspahRules.MAGIC_FLIGHT_TICKS, prayed, MuspahRules.magicMax(shieldedOrAfter())));
            }
            case "stomp" -> {
                // It chased the player into reach: Protect from Melee blocks it; standing still, more gets through.
                boolean moved = lastMeleeAt == null || lastMeleeAt.distanceToSqr(boss.position()) > 0.25;
                inPlace = moved ? 0 : inPlace + 1;
                lastMeleeAt = boss.position();
                int d = BossHits.roll(target, AttackType.STAB, MuspahRules.MELEE, BossHits.attackRoll(boss, AttackType.STAB), false, r);
                if (BossHits.protectedFrom(target, AttackType.STAB)) {
                    d = (int) Math.floor(d * MuspahRules.meleeThroughPrayer(inPlace));
                }
                boss.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                hit(boss, target, "stomp", AttackType.STAB, d);
            }
            default -> super.perform(boss, target, a);
        }
        if (stage == Stage.SHIELDED && ++attacksShielded % MuspahRules.SHIELD_SPIKE_EVERY == 0) {
            markSpikes(boss, target, List.of()); // a spike under the player
        }
        if (stage == Stage.POST && ++attacksPost % BossMechanics.MUSPAH_SPIKE_EVERY == 0) {
            Vec3 c = homeVec();
            List<Long> grown = BossMechanics.muspahSpikeGrowth(spikes, (int) Math.floor(c.x), (int) Math.floor(c.z), 9,
                    0.35, new Random(boss.getRandom().nextLong())); // approx: growth rate
            markSpikes(boss, target, grown);
        }
    }

    /** One of its hits; while shielded, Soul Split heals it half the damage (rounded down). */
    private int hit(DataBossEntity boss, ServerPlayer p, String name, AttackType style, int damage) {
        int dealt = BossHits.deal(boss, p, name, style, damage);
        if (stage == Stage.SHIELDED && dealt > 1) {
            boss.heal(mcHp(boss, dealt / 2));
        }
        return dealt;
    }

    private void tickOrbs(DataBossEntity boss) {
        Iterator<Orb> it = orbs.iterator();
        while (it.hasNext()) {
            Orb o = it.next();
            if (now < o.due()) {
                continue;
            }
            it.remove();
            if (!o.target().isAlive() || o.prayedAtLaunch()) {
                continue; // prayed against before it was launched: no damage, no corruption
            }
            Random r = new Random(boss.getRandom().nextLong());
            int d = BossHits.roll(o.target(), AttackType.MAGIC, o.max(), attackRoll(boss, AttackType.MAGIC), false, r);
            if (BossHits.protectedFrom(o.target(), AttackType.MAGIC)) {
                d = (int) Math.floor(d * MuspahRules.MAGIC_LATE_PRAYER); // prayed in flight: weakened, but it still corrupts
            }
            hit(boss, o.target(), "smite", AttackType.MAGIC, d);
            corrupt(o.target(), false);
        }
    }

    /** Corruption: 3, 6 and 9 prayer over 18 seconds (the shockwave's more); Ward of Arceuus keeps it off. */
    void corrupt(ServerPlayer p, boolean shockwave) {
        var sp = com.runecraft.content.systems.SysState.of(p);
        if (sp.wardUntil >= com.runecraft.content.systems.SysTicks.now()) {
            Msg.action(p, "Your ward protects you from the corruption.");
            return;
        }
        corruption.put(p.getUUID(), new int[]{0, MuspahRules.CORRUPTION_INTERVAL, shockwave ? 1 : 0});
        Msg.red(p, "You have been corrupted!");
    }

    private void tickCorruption(DataBossEntity boss) {
        Iterator<Map.Entry<UUID, int[]>> it = corruption.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, int[]> e = it.next();
            int[] c = e.getValue();
            if (--c[1] > 0) {
                continue;
            }
            ServerPlayer p = player(boss, e.getKey());
            c[0]++;
            c[1] = MuspahRules.CORRUPTION_INTERVAL;
            if (p != null && p.isAlive()) {
                BossFx.drainPrayer(p, MuspahRules.corruptionDrain(c[0], c[2] == 1));
            }
            if (p == null || c[0] >= MuspahRules.CORRUPTION_PULSES) {
                it.remove();
            }
        }
    }

    private ServerPlayer player(DataBossEntity boss, UUID id) {
        ServerPlayer p = BossFx.world(boss).getServer().getPlayerList().getPlayer(id);
        if (p == null && boss.getTarget() instanceof ServerPlayer t && t.getUUID().equals(id)) {
            return t;
        }
        return p;
    }

    // ------------------------------------------------------------------ damage taken

    @Override
    protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
        if (stage == Stage.SHIELDED) {
            // OSRS Wiki: the shielded form is immune to damage; its 75-point prayer shield loses 2 per attack, and
            // prayer drains speed it up.
            shield -= BossMechanics.MUSPAH_SHIELD_PER_ATTACK;
            if (attacker != null) {
                shield -= drainShield(attacker, damage, type);
                if (BossMechanics.versatileDrainer(drains)) {
                    CombatAchievementService.flag(attacker, BossMechanics.FLAG_MUSPAH_THREE_DRAINS);
                }
            }
            BossFx.burst(boss, ParticleTypes.SNOWFLAKE, 8, 1.0);
            if (shield <= 0) {
                shield = 0;
                breakShield(boss, attacker);
            } else if (attacker != null) {
                Msg.action(attacker, "The Phantom Muspah's prayer shield absorbs your attack. (" + shield + " left)");
            }
            return 0;
        }
        int d = Math.max(0, damage);
        int health = (int) Math.ceil(boss.getHealth());
        if (stage == Stage.POST) {
            return d;
        }
        if (d >= health) {
            // Its health reaching zero before the shield goes up skips the shield: it keeps one health (OSRS Wiki).
            d = Math.max(0, health - 1);
            shieldSkip = true;
            if (stage == Stage.FORMS || stage == Stage.CLOUDS) {
                shockwaveQueued = true;
            }
            return d;
        }
        int after = health - d;
        if (stage == Stage.FORMS) {
            if (d > 0) {
                formDamage += d;
                formHits++;
            }
            if (after < MuspahRules.SHIELD_BELOW) {
                shockwaveQueued = true;
            } else if (MuspahRules.specialsDue(after) > specialsDone) {
                specialQueued = true;
            } else if (MuspahRules.swaps(form, formDamage, formHits, swapped)) {
                swapped = true;
                setForm(boss, form == Form.RANGED ? Form.MELEE : Form.RANGED);
                BossFx.say(boss, "The Phantom Muspah shifts into its " + (form == Form.RANGED ? "ranged" : "melee") + " form!");
                markSpikes(boss, attacker != null ? attacker : target(boss), randomTiles(boss, 6));
            }
        } else if (stage == Stage.CLOUDS && after < MuspahRules.SHIELD_BELOW) {
            shockwaveQueued = true; // damage during the clouds doesn't count for its forms, but its health still falls
        }
        return d;
    }

    /** Extra shield points this attack's prayer drains take: Smite, an enchanted sapphire bolt, a pending corruption spell. */
    private int drainShield(ServerPlayer attacker, int damage, AttackType type) {
        int drained = 0;
        if (com.runecraft.player.Players.data(attacker).isActive(Prayer.SMITE) && damage > 0) {
            drained += BossMechanics.muspahSmiteDrain(damage);
            drains.add(BossMechanics.DRAIN_SMITE);
        }
        if (type == AttackType.RANGED && damage > 0) {
            String ammo = com.runecraft.content.systems.BoltProcs.takeShot();
            var en = com.runecraft.osrs.content.systems.BoltEffects.ofItem(ammo);
            if (en != null && en.effect() == com.runecraft.osrs.content.systems.BoltEffects.Effect.CLEAR_MIND) {
                boolean kandarin = com.runecraft.osrs.content.systems.Diaries.tierComplete(
                        com.runecraft.content.systems.SysState.of(attacker).diaryDone, "kandarin",
                        com.runecraft.osrs.content.systems.Diaries.Tier.HARD);
                double chance = BossMechanics.MUSPAH_SAPPHIRE_CHANCE
                        * (kandarin ? com.runecraft.osrs.content.systems.BoltEffects.KANDARIN_FACTOR : 1.0);
                if (attacker.getRandom().nextDouble() < chance) {
                    drained += BossMechanics.MUSPAH_SAPPHIRE_DRAIN;
                    drains.add(BossMechanics.DRAIN_SAPPHIRE);
                }
            }
        }
        var sp = com.runecraft.content.systems.SysState.of(attacker);
        if (sp.corruptionPending > 0 && sp.corruptionPendingUntil >= com.runecraft.content.systems.SysTicks.now()) {
            drained += BossMechanics.muspahCorruptionDrain(sp.corruptionPending == 2);
            sp.corruptionPending = 0;
            drains.add(BossMechanics.DRAIN_CORRUPTION);
        }
        return drained;
    }

    // ------------------------------------------------------------------ specials and stages

    void startSpecial(DataBossEntity boss, ServerPlayer target) {
        MuspahRules.Special s = MuspahRules.special(specialsDone, spawnForm);
        specialsDone++;
        if (s == MuspahRules.Special.LIGHTNING_CLOUDS) {
            stage = Stage.CLOUDS;
            cloudTeleports = MuspahRules.CLOUD_TELEPORTS;
            BossFx.say(boss, "The Phantom Muspah vanishes into the darkness!");
        } else {
            homingSpikes(boss, target);
        }
    }

    /** 2-4 spikes rise around the arena and crawl towards the player (not next to the player, nor under the Muspah). */
    void homingSpikes(DataBossEntity boss, ServerPlayer target) {
        homing.clear();
        Random r = new Random(boss.getRandom().nextLong());
        int n = MuspahRules.HOMING_MIN + r.nextInt(MuspahRules.HOMING_MAX - MuspahRules.HOMING_MIN + 1);
        double half = boss.getBbWidth() / 2 + 1;
        for (int i = 0, tries = 0; i < n && tries < 40; tries++) {
            Vec3 v = around(boss, homeVec(), 8);
            if (target != null && v.distanceToSqr(target.position()) < 9 || Math.abs(v.x - boss.getX()) <= half && Math.abs(v.z - boss.getZ()) <= half) {
                continue;
            }
            homing.add(new int[]{(int) Math.floor(v.x), (int) Math.floor(v.z)});
            i++;
        }
        homingTicks = MuspahRules.HOMING_TICKS;
        BossFx.say(boss, "The Phantom Muspah slams the ground - spikes crawl towards you!");
        BossFx.sound(boss, SoundEvents.ANVIL_LAND, 0.6f);
    }

    private void tickHoming(DataBossEntity boss, ServerPlayer target) {
        if (homing.isEmpty()) {
            return;
        }
        ServerLevel w = BossFx.world(boss);
        for (int[] h : homing) {
            w.sendParticles(ParticleTypes.SNOWFLAKE, h[0] + 0.5, boss.getY() + 0.4, h[1] + 0.5, 4, 0.2, 0.5, 0.2, 0.01);
        }
        if (--homingTicks <= 0) {
            hardenHoming();
            return;
        }
        if (target == null || homingTicks % MuspahRules.HOMING_STEP_TICKS != 0) {
            return;
        }
        int tx = target.getBlockX();
        int tz = target.getBlockZ();
        Iterator<int[]> it = homing.iterator();
        while (it.hasNext()) {
            int[] h = it.next();
            int[] next = MuspahRules.homingStep(h[0], h[1], tx, tz, spikes);
            h[0] = next[0];
            h[1] = next[1];
            if (h[0] == tx && h[1] == tz) {
                spikeHit(boss, target);
                spikes.add(BossMechanics.tile(h[0], h[1]));
                it.remove();
            }
        }
    }

    private void hardenHoming() {
        for (int[] h : homing) {
            spikes.add(BossMechanics.tile(h[0], h[1]));
        }
        homing.clear();
        homingTicks = 0;
    }

    private void tickClouds(DataBossEntity boss, ServerPlayer target) {
        ServerLevel w = BossFx.world(boss);
        Set<ServerPlayer> near = fighters(boss, target);
        if (stage == Stage.CLOUDS) {
            for (ServerPlayer p : near) {
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false)); // the screen dims
            }
            if (cloudTeleports > 0 && now % MuspahRules.CLOUD_INTERVAL == 0) {
                cloudTeleports--;
                Random r = new Random(boss.getRandom().nextLong());
                double[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                double[] d = dirs[r.nextInt(4)];
                clouds.add(new Cloud(new double[]{boss.getX(), boss.getY() + 0.5, boss.getZ()}, d[0], d[1],
                        new int[]{MuspahRules.CLOUD_LIFE}, new HashSet<>()));
                Vec3 to = cloudTeleports == 0 ? BossFx.ground(w, homeVec().x + 8, homeVec().z - 8, homeVec().y) // it ends north-east
                        : around(boss, homeVec(), 8);
                boss.teleportTo(to.x, to.y, to.z);
                w.sendParticles(ParticleTypes.PORTAL, to.x, to.y + 1, to.z, 20, 0.6, 1, 0.6, 0.2);
            }
            if (cloudTeleports <= 0 && clouds.isEmpty()) {
                stage = Stage.FORMS;
            }
        }
        Iterator<Cloud> it = clouds.iterator();
        Random r = new Random(boss.getRandom().nextLong());
        while (it.hasNext()) {
            Cloud c = it.next();
            c.pos()[0] += c.dx();
            c.pos()[2] += c.dz();
            w.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.pos()[0], c.pos()[1] + 0.5, c.pos()[2], 8, 0.5, 0.4, 0.5, 0.05);
            w.sendParticles(ParticleTypes.CLOUD, c.pos()[0], c.pos()[1] + 0.8, c.pos()[2], 3, 0.4, 0.2, 0.4, 0.0);
            for (ServerPlayer p : near) {
                double dx = p.getX() - c.pos()[0];
                double dz = p.getZ() - c.pos()[2];
                if (dx * dx + dz * dz <= 1.44 && c.hit().add(p.getUUID())) {
                    hit(boss, p, "lightning_cloud", null, r.nextInt(MuspahRules.LIGHTNING_CLOUD + 1));
                }
            }
            if (--c.life()[0] <= 0) {
                it.remove();
            }
        }
    }

    /** Below 127: it teleports to the centre; the shockwave follows five ticks later. */
    void startShockwave(DataBossEntity boss) {
        stage = Stage.SHOCKWAVE;
        shockwaveIn = MuspahRules.SHOCKWAVE_WIND_UP;
        clouds.clear();
        cloudTeleports = 0;
        Vec3 c = homeVec();
        boss.teleportTo(c.x, c.y, c.z);
        boss.getNavigation().stop();
        BossFx.say(boss, "The Phantom Muspah teleports to the centre and gathers its power - hide behind a spike!");
        BossFx.sound(boss, SoundEvents.WARDEN_SONIC_CHARGE, 0.8f);
    }

    private void tickShockwave(DataBossEntity boss, ServerPlayer target) {
        BossFx.ring(BossFx.world(boss), boss.position(), 1.5 + (MuspahRules.SHOCKWAVE_WIND_UP - shockwaveIn) * 2.0, ParticleTypes.SONIC_BOOM);
        if (--shockwaveIn > 0) {
            return;
        }
        Random r = new Random(boss.getRandom().nextLong());
        int cx = boss.getBlockX();
        int cz = boss.getBlockZ();
        int half = (int) Math.ceil(boss.getBbWidth() / 2);
        for (ServerPlayer p : fighters(boss, target)) {
            if (MuspahRules.sheltered(spikes, cx, cz, p.getBlockX(), p.getBlockZ(), half)) {
                Msg.action(p, "The spike shelters you from the shockwave.");
                continue;
            }
            hit(boss, p, "shockwave", null, r.nextInt(MuspahRules.SHOCKWAVE + 1));
            corrupt(p, true);
        }
        BossFx.sound(boss, SoundEvents.WARDEN_SONIC_BOOM, 0.8f);
        hardenHoming(); // entering the shielded form ends its homing spikes
        if (shieldSkip) {
            stage = Stage.POST;
            BossFx.say(boss, "The Phantom Muspah has no strength left to raise its shield!");
            return;
        }
        stage = Stage.SHIELDED;
        shield = BossMechanics.MUSPAH_SHIELD;
        drains.clear();
        attacksShielded = 0;
        setForm(boss, Form.RANGED);
        BossFx.say(boss, "The Phantom Muspah surrounds itself with a frozen prayer shield!");
    }

    void breakShield(DataBossEntity boss, ServerPlayer attacker) {
        stage = Stage.POST;
        attacksPost = 0;
        BossFx.say(boss, "The Phantom Muspah's prayer shield shatters!");
        markSpikes(boss, attacker != null ? attacker : target(boss), randomTiles(boss, 10));
    }

    // ------------------------------------------------------------------ spikes

    private List<Long> randomTiles(DataBossEntity boss, int n) {
        List<Long> tiles = new ArrayList<>();
        Vec3 c = homeVec();
        for (int i = 0; i < n; i++) {
            Vec3 v = around(boss, c, 9);
            tiles.add(BossMechanics.tile((int) Math.floor(v.x), (int) Math.floor(v.z)));
        }
        return tiles;
    }

    /** Marks spikes (plus one under the target): they erupt two ticks later, hitting whoever still stands there. */
    void markSpikes(DataBossEntity boss, ServerPlayer target, List<Long> tiles) {
        List<Long> fresh = new ArrayList<>(tiles);
        if (target != null) {
            fresh.add(BossMechanics.tile(target.getBlockX(), target.getBlockZ()));
        }
        for (long t : fresh) {
            if (!spikes.contains(t)) {
                erupting.put(t, now + MuspahRules.SPIKE_ERUPT_TICKS);
            }
        }
        BossFx.sound(boss, SoundEvents.GLASS_BREAK, 0.6f);
    }

    private void tickSpikes(DataBossEntity boss, ServerPlayer target) {
        ServerLevel w = BossFx.world(boss);
        Set<ServerPlayer> near = fighters(boss, target);
        if (!erupting.isEmpty()) {
            List<Long> due = new ArrayList<>();
            for (Map.Entry<Long, Long> e : erupting.entrySet()) {
                long t = e.getKey();
                w.sendParticles(ParticleTypes.ITEM_SNOWBALL, (int) (t >> 32) + 0.5, boss.getY() + 0.1, (int) t + 0.5, 3, 0.25, 0.02, 0.25, 0.0);
                if (now >= e.getValue()) {
                    due.add(t);
                }
            }
            for (long t : due) {
                erupting.remove(t);
                spikes.add(t);
                for (ServerPlayer p : near) {
                    if (BossMechanics.tile(p.getBlockX(), p.getBlockZ()) == t) {
                        spikeHit(boss, p);
                    }
                }
            }
        }
        // Walking onto a standing spike hurts too.
        for (ServerPlayer p : near) {
            long t = BossMechanics.tile(p.getBlockX(), p.getBlockZ());
            Long last = walkedOn.get(p.getUUID());
            if (spikes.contains(t) && (last == null || now - last >= MuspahRules.SPIKE_WALK_COOLDOWN)) {
                walkedOn.put(p.getUUID(), now);
                spikeHit(boss, p);
            }
        }
        if (!spikes.isEmpty() && ++spikeFx % 3 == 0) {
            int shown = 0;
            for (long t : spikes) {
                if (++shown > 120) {
                    break;
                }
                w.sendParticles(ParticleTypes.SNOWFLAKE, (int) (t >> 32) + 0.5, boss.getY() + 0.3, (int) t + 0.5, 2, 0.15, 0.4, 0.15, 0.0);
            }
        }
    }

    /** A spike hits a player: damage, knocked aside, and it heals the Muspah ~75% of the damage. */
    void spikeHit(DataBossEntity boss, ServerPlayer p) {
        int dmg = 1 + boss.getRandom().nextInt(BossMechanics.MUSPAH_SPIKE_MAX);
        int dealt = hit(boss, p, "spike", null, dmg);
        boss.heal(mcHp(boss, BossMechanics.muspahSpikeHeal(dealt)));
        Vec3 away = p.position().subtract(boss.position()).multiply(1, 0, 1);
        away = away.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : away.normalize();
        p.knockback(0.8, -away.x, -away.z);
        p.hurtMarked = true;
        Msg.red(p, "A spike erupts beneath you!");
    }

    // ------------------------------------------------------------------ the game tick

    @Override
    protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
        if (shockwaveQueued && (stage == Stage.FORMS || stage == Stage.CLOUDS)) {
            shockwaveQueued = false;
            specialQueued = false;
            startShockwave(boss);
        } else if (specialQueued && stage == Stage.FORMS) {
            specialQueued = false;
            startSpecial(boss, target);
        }
        if (stage == Stage.SHOCKWAVE) {
            tickShockwave(boss, target);
        }
        tickOrbs(boss);
        tickCorruption(boss);
        tickClouds(boss, target);
        tickHoming(boss, target);
        tickSpikes(boss, target);
        if (stage == Stage.SHIELDED) {
            BossFx.ring(BossFx.world(boss), boss.position(), 2.2, ParticleTypes.SNOWFLAKE);
        }
        if (stage == Stage.POST) {
            // "Space is Tight": spikes on every tile around its footprint.
            int size = Math.max(1, (int) Math.ceil(boss.getBbWidth()));
            int x = (int) Math.floor(boss.getX() - size / 2.0);
            int z = (int) Math.floor(boss.getZ() - size / 2.0);
            boolean surrounded = BossMechanics.muspahSurrounded(spikes, x, z, size);
            for (ServerPlayer p : fighters(boss, target)) {
                if (surrounded) {
                    CombatAchievementService.flag(p, BossMechanics.FLAG_MUSPAH_SURROUNDED);
                } else {
                    CombatAchievementService.unflag(p, BossMechanics.FLAG_MUSPAH_SURROUNDED);
                }
            }
        }
    }

    private Set<ServerPlayer> fighters(DataBossEntity boss, ServerPlayer target) {
        Set<ServerPlayer> out = new LinkedHashSet<>(BossFx.players(boss, 24));
        if (target != null && target.isAlive()) {
            out.add(target);
        }
        return out;
    }

    @Override
    public void onDeath(DataBossEntity boss, ServerPlayer killer) {
        cleanUp(boss);
    }

    @Override
    public void onRemoved(DataBossEntity boss) {
        cleanUp(boss);
    }

    private void cleanUp(DataBossEntity boss) {
        clouds.clear();
        homing.clear();
        erupting.clear();
        orbs.clear();
        corruption.clear();
    }

    /** The spikes it stands in now (for the self-test). */
    Set<Long> spikes() {
        return spikes;
    }

    int shield() {
        return shield;
    }
}
