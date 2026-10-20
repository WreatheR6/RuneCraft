package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.boss.Mechanics;
import com.runecraft.collection.CollectionLog;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.content.systems.CombatAchievementService;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.CombatFormulas;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.bosses.BossLoot;
import com.runecraft.osrs.content.bosses.BossMechanics;
import com.runecraft.osrs.content.bosses.MinigameBosses;
import com.runecraft.osrs.content.bosses.NightmareRules;
import com.runecraft.osrs.content.bosses.NightmareRules.Special;
import com.runecraft.osrs.data.Drop;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.skill.Drops;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * The Nightmare and Phosani's Nightmare (OSRS Wiki; numbers in {@link NightmareRules}).
 *
 * <p>Disturbed (summoned), she sleeps 30 seconds (Phosani 10) while players join her dream; the players there when she wakes
 * set the fight's scale. Each phase players break her shield (her boss bar), she becomes immune and they charge the four
 * totems in the corners (magic charges twice as fast); charged, the totems blast her. The Nightmare dies to the third blast;
 * sleepwalkers come at the start of her second and third phases. Phosani survives hers: sleepwalkers come after each, then
 * her desperation phase (150 health, Grasping Claws over and over, a sleepwalker at a time).
 *
 * <p>Her standard attacks wind up (melee 2 ticks, magic and ranged 3) and land on whoever stays in reach: the right
 * protection prayer takes The Nightmare's to 20% (a wrong one raises them to 120%) and blocks Phosani's. Her specials by phase:
 * Grasping Claws, Husks and Corpse Flowers; Grasping Claws, Curse (prayers shuffled for 5 attacks) and Parasites; Grasping
 * Claws, Spores and Surge. Phosani uses a set per phase (A1/A2/B1/B2), spores and flowers with six Grasping Claws in a row, and
 * switches off a protection prayer already up when her phase-three attacks start.
 *
 * <p>Loot is shared (The Nightmare): every player who hurt both her shield and the totems rolls the common table and gets
 * regular bones; the most damaging player (MVP) gets the kill, big bones and the better tertiary rates; each unique goes to
 * one player, weighted by damage.
 */
final class NightmareScript extends ScriptedBoss {
    enum Stage { ASLEEP, SHIELD, TOTEMS, WALKERS, DESPERATION, DEAD }

    /** Players who drank a sanfew serum or relicym's balm since their parasite landed (it bursts out weakened). */
    static final Set<UUID> CURED = ConcurrentHashMap.newKeySet();
    /**
     * Weary players' slower attacks: uuid -> {extra game ticks, Minecraft tick the weariness wears off}. The attack a weary
     * player starts pushes their next one back (approx: melee and magic only; bows have no attack timer here).
     */
    static final Map<UUID, long[]> SLOWED = new ConcurrentHashMap<>();
    /** Cursed players: her attacks left before their protection prayers stop being shuffled. */
    static final Map<UUID, Integer> CURSED = new ConcurrentHashMap<>();
    /** Tag of everything the script places in the world (cleared when a reloaded boss finds what it no longer tracks). */
    static final String MINION_TAG = "rc_nightmare_minion";
    private static boolean registered;

    /** Hooks shared by every Nightmare: cures, weary attack timers, the curse's prayer shuffle and the shared loot roll. */
    static void register() {
        if (registered) {
            return;
        }
        registered = true;
        com.runecraft.content.skilling.ItemHooks.DISEASE_CURE.add(p -> CURED.add(p.getUUID()));
        MinionRules.watch((p, t, type, d) -> {
            long[] slow = SLOWED.get(p.getUUID());
            if (slow == null) {
                return;
            }
            long now = com.runecraft.action.ActionManager.mcTick();
            if (now >= slow[1]) {
                SLOWED.remove(p.getUUID());
                return;
            }
            PlayerData data = Players.data(p);
            if (data.lastAttackTick == now) { // the attack just started (pushed back once, even for multi-hit specials)
                data.lastAttackTick = now + slow[0] * com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK;
            }
        });
        com.runecraft.combat.PrayerService.REDIRECTS.add((p, prayer) -> CURSED.containsKey(p.getUUID()) ? NightmareRules.cursed(prayer) : prayer);
        DataBossService.loot("the_nightmare", NightmareScript::rollLoot);
        DataBossService.loot("phosanis_nightmare", NightmareScript::rollLoot);
    }

    private final boolean phosani;
    Stage stage = Stage.ASLEEP;
    private int wakeIn;
    /** Shield phase (0-2); the number of totem blasts so far. */
    int phase;
    /** Players in her dream when she woke (at least one): the shield, totems, sleepwalkers, parasites and pet scale with it. */
    int players = 1;
    int shieldMax;
    int shieldLeft;
    int desperationLeft = NightmareRules.PHOSANI_DESPERATION_HEALTH;
    int totemCharge;
    final List<Mob> totems = new ArrayList<>();
    private final List<Vec3> charged = new ArrayList<>();
    /** Damage each player dealt to her shield (and desperation health) and charge they put into totems. */
    final Map<UUID, int[]> contribution = new LinkedHashMap<>();
    List<NightmareRules.PhosaniSet> sets;
    private final Deque<Special> round = new ArrayDeque<>();
    private int sinceSpecial;
    int clawChain;
    private final List<Pending> pending = new ArrayList<>();
    // Husks
    final List<Mob> husks = new ArrayList<>();
    private ServerPlayer bound;
    private int huskTick;
    // Parasites
    final Map<UUID, Integer> impregnated = new LinkedHashMap<>();
    final List<Parasite> parasites = new ArrayList<>();
    private int parasiteHealTimer = BossMechanics.PARASITE_HEAL_INTERVAL;
    // Corpse flowers
    int flowerSafe = -1;
    int flowerTicks;
    private Vec3 flowerCentre;
    // Surge
    private Vec3 surgeFrom;
    private Vec3 surgeTo;
    int surgeIn;
    // Sleepwalkers
    final List<Mob> walkers = new ArrayList<>();
    private long walkersMoveAt;
    private int setTotal;
    private int setReached;
    boolean desperate;
    private int walkerTimer;
    // Spores
    final List<Vec3> spores = new ArrayList<>();
    private int sporesLeft;
    private final Map<UUID, Integer> weary = new HashMap<>();
    /** Phosani's Prayer Deactivation (phase three): game ticks until one of her standard attacks can switch a prayer off. */
    int deactivateTimer;
    private boolean cleanedUp;
    /** Self-test: only standard attacks (the phase walk performs her specials itself). */
    boolean selfTestQuiet;

    /** A standard attack winding up: lands at {@code due} on whoever is still in its reach. */
    private record Pending(AttackDef attack, long due, Vec3 facing, List<ServerPlayer> victims) {
    }

    /** A parasite out of its host. */
    record Parasite(Mob mob, boolean weakened, UUID host, boolean[] magicNext) {
    }

    NightmareScript() {
        this(false);
    }

    NightmareScript(boolean phosani) {
        this.phosani = phosani;
        register();
    }

    boolean phosani() {
        return phosani;
    }

    // ------------------------------------------------------------------ waking, scale, stages

    @Override
    public void onSpawn(DataBossEntity boss) {
        home = boss.blockPosition();
        wakeIn = phosani ? NightmareRules.PHOSANI_WAKE_TICKS : NightmareRules.NIGHTMARE_WAKE_TICKS;
        stage = Stage.ASLEEP;
        BossFx.say(boss, phosani ? "Phosani's Nightmare stirs... she wakes in 10 seconds."
                : "The Nightmare stirs... she wakes in 30 seconds. Others may join her dream until then.");
    }

    /** She wakes: the players in her dream now set the fight's scale. */
    void wake(DataBossEntity boss) {
        Set<ServerPlayer> in = fighters(boss, target(boss));
        players = phosani ? 1 : Math.max(1, in.size());
        shieldMax = NightmareRules.shield(phosani, players);
        totemCharge = NightmareRules.totemCharge(phosani, players);
        if (phosani) {
            sets = NightmareRules.phosaniSets(new Random(boss.getRandom().nextLong()));
        }
        phase = 0;
        startShield(boss);
        BossFx.sound(boss, SoundEvents.WITHER_AMBIENT, 0.6f);
        BossFx.say(boss, (phosani ? "Phosani's Nightmare" : "The Nightmare") + " awakens!"
                + (phosani ? "" : " (" + players + (players == 1 ? " player" : " players") + " in her dream)"));
    }

    private void startShield(DataBossEntity boss) {
        stage = Stage.SHIELD;
        shieldLeft = shieldMax;
        round.clear();
        sinceSpecial = 0;
        deactivateTimer = 0;
        if (phase > 0) {
            BossFx.say(boss, "The Nightmare's shield reforms! (phase " + (phase + 1) + ")");
        }
    }

    /** Her shield is broken: she can't be hurt, the four totems can be charged. */
    void breakShield(DataBossEntity boss) {
        stage = Stage.TOTEMS;
        shieldLeft = 0;
        totems.forEach(Mob::discard);
        totems.clear();
        charged.clear();
        Vec3 c = homeVec();
        for (int[] o : new int[][]{{-8, -8}, {8, -8}, {-8, 8}, {8, 8}}) { // the four corners of the arena
            Mob t = BossFx.spawnMinion(boss, "nightmare_totem", BossFx.ground(BossFx.world(boss), c.x + o[0], c.z + o[1], c.y), null);
            if (t == null) {
                continue;
            }
            t.addTag(MINION_TAG);
            int hp = Math.min(DataBossEntity.MAX_MC_HEALTH, totemCharge);
            var max = t.getAttribute(Attributes.MAX_HEALTH);
            if (max != null) {
                max.setBaseValue(hp);
            }
            t.setHealth(hp);
            MinionRules.set(t, new MinionRules.Rule() {
                @Override
                public double accuracy(ServerPlayer p, LivingEntity mob, AttackType type) {
                    return MinionRules.ALWAYS_HITS; // no stats to miss against
                }

                @Override
                public int damage(ServerPlayer p, LivingEntity mob, AttackType type, int d) {
                    return totemDamage(p, mob, type, d);
                }
            });
            totems.add(t);
        }
        BossFx.say(boss, "The Nightmare's shield is broken! Charge the four totems!");
        BossFx.sound(boss, SoundEvents.GLASS_BREAK, 0.7f);
    }

    /** A hit on a totem: magic charges it twice as fast; the charge is the player's contribution. Returns the damage it takes. */
    int totemDamage(ServerPlayer p, LivingEntity totem, AttackType type, int d) {
        double scale = totemCharge > DataBossEntity.MAX_MC_HEALTH ? DataBossEntity.MAX_MC_HEALTH / (double) totemCharge : 1.0;
        int charge = NightmareRules.totemCharge(d, type == AttackType.MAGIC);
        if (p != null) {
            contribution(p)[1] += Math.min(charge, (int) Math.ceil(totem.getHealth() / scale));
        }
        return (int) Math.round(charge * scale);
    }

    /** All four totems are charged: they blast her for 800 of her health. */
    void blast(DataBossEntity boss) {
        totems.forEach(Mob::discard);
        totems.clear();
        charged.clear();
        phase++;
        BossFx.say(boss, "The totems blast " + (phosani ? "Phosani's Nightmare" : "the Nightmare") + "!");
        BossFx.sound(boss, SoundEvents.GENERIC_EXPLODE.value(), 0.7f);
        BossFx.burst(boss, ParticleTypes.END_ROD, 40, 1.2);
        if (!phosani && phase >= NightmareRules.SHIELD_PHASES) {
            kill(boss);
            return;
        }
        shieldLeft = shieldMax; // the next phase's shield, already on her bar
        if (NightmareRules.sleepwalkersAfter(phosani, phase)) {
            stage = Stage.WALKERS;
            summonWalkers(boss, NightmareRules.sleepwalkers(phosani, phase, players));
        } else {
            startShield(boss);
        }
    }

    private void startDesperation(DataBossEntity boss) {
        stage = Stage.DESPERATION;
        desperate = true;
        desperationLeft = NightmareRules.PHOSANI_DESPERATION_HEALTH;
        Vec3 c = homeVec();
        boss.teleportTo(c.x, c.y, c.z);
        walkerTimer = 3; // approx: she moves to the centre first
        BossFx.warn(boss, "Phosani's Nightmare lashes out in desperation!");
        BossFx.say(boss, "Phosani's Nightmare lashes out in desperation!");
    }

    /** The killing blast: the kill goes to the player who dealt the most damage (the MVP). */
    void kill(DataBossEntity boss) {
        stage = Stage.DEAD;
        ServerPlayer mvp = mvp(boss);
        if (mvp == null) {
            boss.discard();
            return;
        }
        CombatHandler.applyDamage(mvp, boss, (int) Math.ceil(boss.getHealth()) + 1);
    }

    /** The online player with the most damage on her shield and totems, else her target. */
    ServerPlayer mvp(DataBossEntity boss) {
        ServerPlayer best = null;
        int bestDmg = -1;
        for (Map.Entry<UUID, int[]> e : contribution.entrySet()) {
            ServerPlayer p = BossFx.world(boss).getServer().getPlayerList().getPlayer(e.getKey());
            int d = e.getValue()[0] + e.getValue()[1];
            if (p != null && p.isAlive() && d > bestDmg) {
                best = p;
                bestDmg = d;
            }
        }
        if (best == null) {
            best = target(boss);
        }
        if (best == null && boss.getTarget() instanceof ServerPlayer p) {
            best = p;
        }
        return best;
    }

    int[] contribution(ServerPlayer p) {
        return contribution.computeIfAbsent(p.getUUID(), k -> new int[2]);
    }

    @Override
    public boolean isVulnerable(DataBossEntity boss) {
        return stage == Stage.SHIELD || stage == Stage.DESPERATION;
    }

    @Override
    public float bossBarProgress(DataBossEntity boss) {
        return switch (stage) {
            case SHIELD -> shieldMax <= 0 ? 1f : shieldLeft / (float) shieldMax;
            case TOTEMS -> {
                float left = 0;
                for (Mob t : totems) {
                    left += t.isAlive() ? t.getHealth() / t.getMaxHealth() : 0;
                }
                yield left / NightmareRules.TOTEMS;
            }
            case DESPERATION -> desperationLeft / (float) NightmareRules.PHOSANI_DESPERATION_HEALTH;
            case DEAD -> 0f;
            default -> 1f;
        };
    }

    /** Her Minecraft health for what is left to deal to her (never 0 before the kill). */
    private float mappedHealth(DataBossEntity boss) {
        if (stage == Stage.ASLEEP || shieldMax <= 0) {
            return boss.getMaxHealth();
        }
        int total = NightmareRules.totalWork(phosani, players);
        int left = NightmareRules.workLeft(phosani, players, phase, shieldLeft, desperate, desperationLeft);
        return 1f + (boss.getMaxHealth() - 1f) * left / Math.max(1, total);
    }

    @Override
    protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
        if (stage != Stage.SHIELD && stage != Stage.DESPERATION) {
            return 0;
        }
        if (attacker != null && flowersBloomed() && NightmareRules.berrySides(flowerSafe,
                NightmareRules.quadrant(attacker.getX() - flowerCentre.x, attacker.getZ() - flowerCentre.z)) > 0) {
            heal(boss, damage);
            Msg.action(attacker, "The corpse flowers feed your attack to the Nightmare!");
            return 0;
        }
        int d = Math.max(0, damage);
        if (stage == Stage.SHIELD) {
            int take = Math.min(d, shieldLeft);
            shieldLeft -= take;
            if (attacker != null) {
                contribution(attacker)[0] += take;
            }
            int mc = mcDamage(boss);
            if (shieldLeft <= 0) {
                breakShield(boss);
            }
            return mc;
        }
        int take = Math.min(d, desperationLeft);
        desperationLeft -= take;
        if (attacker != null) {
            contribution(attacker)[0] += take;
        }
        if (desperationLeft <= 0) {
            stage = Stage.DEAD;
            // the killing blow: enough that the health scaling still leaves nothing
            return (int) Math.ceil((boss.getHealth() + 1) * boss.trueHitpoints() / (double) DataBossEntity.MAX_MC_HEALTH) + 1;
        }
        return mcDamage(boss);
    }

    /** The damage (before the health scaling) that takes her health bar to what is left to deal. */
    private int mcDamage(DataBossEntity boss) {
        double loss = boss.getHealth() - mappedHealth(boss);
        if (loss <= 0) {
            return 0;
        }
        return (int) Math.floor(loss * Math.max(DataBossEntity.MAX_MC_HEALTH, boss.trueHitpoints()) / DataBossEntity.MAX_MC_HEALTH);
    }

    /** Healing (parasites, corpse flowers): her shield, or her desperation health. */
    void heal(DataBossEntity boss, int amount) {
        if (stage == Stage.SHIELD) {
            shieldLeft = Math.min(shieldMax, shieldLeft + Math.max(0, amount));
        } else if (stage == Stage.DESPERATION) {
            desperationLeft = Math.min(NightmareRules.PHOSANI_DESPERATION_HEALTH, desperationLeft + Math.max(0, amount));
        }
    }

    // ------------------------------------------------------------------ attacks

    @Override
    protected boolean driven() {
        return true;
    }

    @Override
    protected boolean canAttack(DataBossEntity boss) {
        return stage != Stage.ASLEEP && stage != Stage.WALKERS && stage != Stage.DEAD && surgeIn <= 0;
    }

    @Override
    protected boolean wantsMelee(DataBossEntity boss, ServerPlayer target) {
        return (stage == Stage.SHIELD || stage == Stage.TOTEMS) && flowerTicks <= 0 && clawChain <= 0 && surgeIn <= 0;
    }

    @Override
    protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
        if (desperate) {
            return claws(); // Grasping Claws over and over until she dies
        }
        if (selfTestQuiet) {
            return standard(distance <= standard(0).range() ? 0 : 1); // the phase walk does her specials itself
        }
        if (clawChain > 0) {
            clawChain--;
            return claws();
        }
        if (sinceSpecial >= NightmareRules.ATTACKS_PER_SPECIAL) {
            sinceSpecial = 0;
            return specialAttack(nextSpecial(boss));
        }
        sinceSpecial++;
        Random r = new Random(boss.getRandom().nextLong());
        if (phosani && r.nextInt(NightmareRules.PHOSANI_RANDOM_CLAWS) == 0) {
            return claws();
        }
        boolean inReach = distance <= standard(0).range();
        if (inReach && r.nextInt(3) < NightmareRules.meleeThirds(phosani, phase)) {
            return standard(0);
        }
        return standard(r.nextBoolean() ? 1 : 2);
    }

    private AttackDef standard(int i) {
        if (phosani) {
            return i == 0 ? MinigameBosses.PHOSANI_MELEE : i == 1 ? MinigameBosses.PHOSANI_MAGIC : MinigameBosses.PHOSANI_RANGED;
        }
        return i == 0 ? MinigameBosses.NIGHTMARE_MELEE : i == 1 ? MinigameBosses.NIGHTMARE_MAGIC : MinigameBosses.NIGHTMARE_RANGED;
    }

    private AttackDef claws() {
        return phosani ? MinigameBosses.PHOSANI_CLAWS : MinigameBosses.NIGHTMARE_CLAWS;
    }

    /** The phase's next special: each one in a round before any repeats. */
    Special nextSpecial(DataBossEntity boss) {
        if (round.isEmpty()) {
            List<Special> specials = phosani ? sets.get(Math.min(phase, sets.size() - 1)).specials()
                    : NightmareRules.nightmareSpecials(phase);
            round.addAll(NightmareRules.specialRound(specials, new Random(boss.getRandom().nextLong())));
        }
        Special s = round.poll();
        // She doesn't lay corpse flowers while a player is bound by husks: the flowers wait for the next special.
        if (s == Special.CORPSE_FLOWERS && husks.stream().anyMatch(Mob::isAlive) && !round.isEmpty()) {
            Special other = round.poll();
            round.addFirst(s);
            s = other;
        }
        return s;
    }

    AttackDef specialAttack(Special s) {
        return switch (s) {
            case GRASPING_CLAWS -> claws();
            case HUSKS -> MinigameBosses.NIGHTMARE_HUSKS;
            case CORPSE_FLOWERS -> MinigameBosses.NIGHTMARE_FLOWERS;
            case CURSE -> MinigameBosses.NIGHTMARE_CURSE;
            case PARASITES -> MinigameBosses.NIGHTMARE_PARASITES;
            case SURGE -> phosani ? MinigameBosses.PHOSANI_SURGE : MinigameBosses.NIGHTMARE_SURGE;
            case SPORES -> MinigameBosses.NIGHTMARE_SPORES;
        };
    }

    @Override
    protected void perform(DataBossEntity boss, ServerPlayer target, AttackDef a) {
        last = a;
        attackCount++;
        Mechanics.attackListener.onAttack(boss, target, a);
        curseCountdown(boss);
        switch (a.name()) {
            case "claw_swipe", "dream_blast", "thrown_soul" -> standardAttack(boss, target, a);
            case "grasping_claws" -> graspingClaws(boss, target);
            case "husks" -> husks(boss, target);
            case "corpse_flowers" -> corpseFlowers(boss);
            case "curse" -> curse(boss, target);
            case "parasites" -> parasites(boss, target);
            case "surge" -> surge(boss);
            case "spores" -> {
                placeSpores(boss);
                if (phosani) {
                    clawChain = NightmareRules.PHOSANI_CLAW_CHAIN;
                }
            }
            default -> super.perform(boss, target, a);
        }
    }

    /** A standard attack: wound up now, it lands on whoever is still in its reach. */
    void standardAttack(DataBossEntity boss, ServerPlayer target, AttackDef a) {
        if (phosani && phase >= 2 && !desperate && deactivateTimer <= 0) {
            // Phase three: a protection prayer already up against this attack as it starts is switched off (the counter is
            // to pray only after the wind-up). approx: at most every 16-25 ticks.
            Prayer counter = BossHits.protection(a.style());
            deactivateTimer = 16 + boss.getRandom().nextInt(10);
            if (BossFx.protects(target, counter)) {
                BossFx.disablePrayer(target, counter, "Phosani's Nightmare saw your prayer coming and switches it off!");
                CombatAchievementService.flag(target, BossMechanics.FLAG_PHOSANI_PRAYER_DISABLED);
            }
        }
        if (a.message() != null) {
            BossFx.warn(boss, a.message());
        }
        Vec3 facing = target.position().subtract(boss.position()).multiply(1, 0, 1);
        facing = facing.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : facing.normalize();
        List<ServerPlayer> victims = a.style().isMelee() ? List.of() : new ArrayList<>(fighters(boss, target));
        int windUp = a.style().isMelee() ? NightmareRules.MELEE_WIND_UP : NightmareRules.RANGED_WIND_UP;
        pending.add(new Pending(a, now + windUp, facing, victims));
        BossFx.world(boss).sendParticles(a.style() == AttackType.MAGIC ? ParticleTypes.WITCH : a.style() == AttackType.RANGED
                ? ParticleTypes.SOUL : ParticleTypes.CRIT, boss.getX(), boss.getY() + boss.getBbHeight(), boss.getZ(), 20, 0.6, 0.4, 0.6, 0.05);
    }

    private void tickPending(DataBossEntity boss, ServerPlayer target) {
        Iterator<Pending> it = pending.iterator();
        List<Pending> due = new ArrayList<>();
        while (it.hasNext()) {
            Pending p = it.next();
            if (now >= p.due()) {
                due.add(p);
                it.remove();
            }
        }
        for (Pending p : due) {
            land(boss, target, p);
        }
    }

    private void land(DataBossEntity boss, ServerPlayer target, Pending p) {
        AttackDef a = p.attack();
        List<ServerPlayer> victims = new ArrayList<>();
        if (a.style().isMelee()) {
            // In front of her (diagonals too) or under her; a step out of reach dodges it.
            double half = boss.getBbWidth() / 2;
            Set<ServerPlayer> near = fighters(boss, target);
            for (ServerPlayer pl : near) {
                Vec3 to = pl.position().subtract(boss.position()).multiply(1, 0, 1);
                double dist = to.horizontalDistance();
                boolean under = dist <= half + 0.3;
                boolean front = dist <= half + 2.0 && dist > 1e-6 && to.normalize().dot(p.facing()) >= 0.4;
                if (under || front) {
                    victims.add(pl);
                }
            }
        } else {
            for (ServerPlayer pl : p.victims()) {
                if (pl.isAlive() && pl.distanceToSqr(boss) <= 32 * 32) {
                    victims.add(pl);
                }
            }
        }
        Random r = new Random(boss.getRandom().nextLong());
        long attackRoll = BossHits.attackRoll(boss, a.style());
        int max = maxHit(boss, a);
        for (ServerPlayer pl : victims) {
            // Her melee always hits; Phosani's too when it isn't prayed against.
            int rolled = BossHits.roll(pl, a.style(), max, attackRoll, a.style().isMelee(), r);
            NightmareRules.Protection prot = BossHits.protectedFrom(pl, a.style()) ? NightmareRules.Protection.CORRECT
                    : BossHits.wrongProtection(pl, a.style()) ? NightmareRules.Protection.WRONG : NightmareRules.Protection.NONE;
            BossHits.deal(boss, pl, a.name(), a.style(), NightmareRules.standardDamage(phosani, rolled, prot));
        }
        if (!a.style().isMelee()) {
            BossFx.world(boss).sendParticles(a.style() == AttackType.MAGIC ? ParticleTypes.WITCH : ParticleTypes.SOUL,
                    boss.getX(), boss.getY() + boss.getBbHeight() * 0.7, boss.getZ(), 15, 1.0, 0.5, 1.0, 0.1);
        }
    }

    // ---------------- Grasping Claws

    /** Portals under her and over the arena (Phosani's clumped around the player); the claws come up 3 ticks later. */
    void graspingClaws(DataBossEntity boss, ServerPlayer target) {
        List<Vec3> spots = new ArrayList<>();
        spots.add(boss.position());
        Vec3 c = homeVec();
        int scattered = phosani ? 8 : 12;
        for (int i = 0; i < scattered; i++) {
            spots.add(around(boss, c, 10));
        }
        if (phosani && target != null) {
            for (int i = 0; i < 5; i++) {
                spots.add(around(boss, target.position(), 3));
            }
        }
        // No portals under a player bound by husks (but her own, under her, still hits them).
        if (bound != null && husks.stream().anyMatch(Mob::isAlive)) {
            Vec3 b = bound.position();
            Vec3 first = spots.get(0);
            spots.removeIf(s -> s != first && s.distanceToSqr(b) < 2.0);
        }
        Set<UUID> hit = new java.util.HashSet<>();
        Random r = new Random(boss.getRandom().nextLong());
        for (int i = 0; i < spots.size(); i++) {
            double radius = i == 0 ? boss.getBbWidth() / 2 + 0.3 : 1.0;
            strike(boss, spots.get(i), NightmareRules.CLAWS_DELAY, 0, radius, ParticleTypes.SQUID_INK, (players, at) -> {
                for (ServerPlayer p : players) {
                    if (hit.add(p.getUUID())) {
                        BossHits.deal(boss, p, "grasping_claws", null, NightmareRules.clawDamage(phosani, r));
                    }
                }
            });
        }
    }

    // ---------------- Husks

    /** Two husks (magic and ranged) bind the player until both are dead. */
    void husks(DataBossEntity boss, ServerPlayer target) {
        husks.forEach(Mob::discard);
        husks.clear();
        bound = target;
        Vec3 at = target.position();
        for (int i = 0; i < NightmareRules.HUSKS; i++) {
            double a = boss.getRandom().nextDouble() * Math.PI * 2;
            Mob h = BossFx.spawnMinion(boss, "nightmare_husk",
                    BossFx.ground(BossFx.world(boss), at.x + Math.cos(a) * 1.5, at.z + Math.sin(a) * 1.5, at.y), target);
            if (h == null) {
                continue;
            }
            h.addTag(MINION_TAG);
            h.setNoAi(true); // the script performs their attacks
            h.setCustomName(Component.literal(i == 0 ? "Husk (magic)" : "Husk (ranged)"));
            MinionRules.set(h, new MinionRules.Rule() {
                @Override
                public boolean alwaysMaxHit(ServerPlayer p, LivingEntity mob, AttackType type) {
                    return phosani && type == AttackType.CRUSH; // crush always max hits Phosani's husks
                }

                @Override
                public int damage(ServerPlayer p, LivingEntity mob, AttackType type, int d) {
                    if (phosani && d > 0 && d < mob.getHealth()) {
                        CombatAchievementService.flag(p, BossMechanics.FLAG_PHOSANI_MINION_SURVIVED);
                    }
                    return d;
                }
            });
            husks.add(h);
        }
        huskTick = 0;
        BossFx.stun(target, 200);
        Msg.red(target, "Husks grab hold of you! Kill them to break free.");
    }

    private void tickHusks(DataBossEntity boss) {
        if (husks.isEmpty()) {
            return;
        }
        if (husks.stream().noneMatch(Mob::isAlive) || bound == null || !bound.isAlive()) {
            husks.forEach(Mob::discard);
            husks.clear();
            if (bound != null) {
                BossFx.unstun(bound);
                Msg.action(bound, "You break free of the husks' grip.");
            }
            bound = null;
            return;
        }
        BossFx.stun(bound, 4);
        huskTick++;
        Random r = new Random(boss.getRandom().nextLong());
        for (int i = 0; i < husks.size(); i++) {
            Mob h = husks.get(i);
            // The magic husk hits a tick before the ranged one; each every 4 ticks.
            if (!h.isAlive() || (huskTick - 1 - i) % NightmareRules.HUSK_SPEED != 0) {
                continue;
            }
            AttackType style = i == 0 ? AttackType.MAGIC : AttackType.RANGED;
            long roll = CombatFormulas.npcRoll(NightmareRules.HUSK_LEVEL, NightmareRules.huskAttackBonus(phosani));
            int d = BossHits.roll(bound, style, NightmareRules.huskMaxHit(phosani), roll, false, r);
            if (BossHits.protectedFrom(bound, style)) {
                d = 0; // their hits are negated by the right protection prayer
            }
            BossFx.world(boss).sendParticles(style == AttackType.MAGIC ? ParticleTypes.WITCH : ParticleTypes.CRIT,
                    h.getX(), h.getY() + 1.2, h.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
            BossHits.deal(h, bound, "husk", style, d);
        }
    }

    // ---------------- Corpse flowers

    /** She moves to the centre and splits the arena in four: only the quadrant between two rows of blossoms is safe. */
    void corpseFlowers(DataBossEntity boss) {
        flowerCentre = homeVec();
        boss.teleportTo(flowerCentre.x, flowerCentre.y, flowerCentre.z);
        flowerSafe = boss.getRandom().nextInt(4);
        flowerTicks = NightmareRules.FLOWER_BLOOM_TICKS + NightmareRules.FLOWER_TICKS;
        clawChain = phosani ? NightmareRules.PHOSANI_CLAW_CHAIN : NightmareRules.NIGHTMARE_CLAW_CHAIN;
        BossFx.warn(boss, "Corpse flowers split the arena! Stand between the white blossoms.");
    }

    boolean flowersBloomed() {
        return flowerSafe >= 0 && flowerTicks > 0 && flowerTicks <= NightmareRules.FLOWER_TICKS && flowerCentre != null;
    }

    private void tickFlowers(DataBossEntity boss, ServerPlayer target) {
        if (flowerSafe < 0 || flowerCentre == null) {
            return;
        }
        if (flowerTicks <= 0) {
            flowerSafe = -1;
            return;
        }
        ServerLevel w = BossFx.world(boss);
        if (flowerTicks % 2 == 0) {
            // Half-lines north, west, south, east of her: blossoms (white) border the safe quadrant, berries (red) the others.
            double[][] dirs = {{0, -1}, {-1, 0}, {0, 1}, {1, 0}};
            for (int d = 0; d < 4; d++) {
                // half-line d (N, W, S, E) borders quadrants d and d-1 (NE=0 N|E, NW=1 N|W, SW=2 S|W, SE=3 S|E)
                boolean blossom = borders(d, flowerSafe);
                ParticleOptions particle = blossom ? ParticleTypes.END_ROD : ParticleTypes.CRIMSON_SPORE;
                for (double s = 2; s <= 13; s += 1.5) {
                    w.sendParticles(particle, flowerCentre.x + dirs[d][0] * s, flowerCentre.y + 0.2, flowerCentre.z + dirs[d][1] * s,
                            1, 0.05, 0.05, 0.05, 0);
                }
            }
        }
        flowerTicks--;
        if (!flowersBloomed()) {
            return;
        }
        Random r = new Random(boss.getRandom().nextLong());
        for (ServerPlayer p : fighters(boss, target)) {
            int sides = NightmareRules.berrySides(flowerSafe, NightmareRules.quadrant(p.getX() - flowerCentre.x, p.getZ() - flowerCentre.z));
            int d = 0;
            for (int i = 0; i < sides; i++) {
                d += r.nextInt(NightmareRules.FLOWER_MAX + 1);
            }
            if (d > 0) {
                int dealt = BossHits.deal(boss, p, "corpse_flowers", null, d);
                heal(boss, dealt * NightmareRules.FLOWER_HEAL_MULTIPLIER);
            }
        }
    }

    /** Whether half-line {@code line} (0 north, 1 west, 2 south, 3 east) is a side of quadrant {@code q} (0 NE, 1 NW, 2 SW, 3 SE). */
    static boolean borders(int line, int q) {
        return switch (q) {
            case 0 -> line == 0 || line == 3;
            case 1 -> line == 0 || line == 1;
            case 2 -> line == 2 || line == 1;
            default -> line == 2 || line == 3;
        };
    }

    // ---------------- Curse

    /** Her curse: the players' protection prayers are shuffled for her next five attacks. */
    void curse(DataBossEntity boss, ServerPlayer target) {
        for (ServerPlayer p : fighters(boss, target)) {
            CURSED.put(p.getUUID(), NightmareRules.CURSE_ATTACKS); // counted from her next attack (perform counts before it acts)
            Msg.red(p, "The Nightmare curses you, shuffling your prayers!");
            BossFx.world(boss).sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 0.8, 0.5, 0.5);
        }
    }

    private void curseCountdown(DataBossEntity boss) {
        if (CURSED.isEmpty()) {
            return;
        }
        for (ServerPlayer p : fighters(boss, target(boss))) {
            Integer left = CURSED.get(p.getUUID());
            if (left == null) {
                continue;
            }
            if (left <= 1) {
                CURSED.remove(p.getUUID());
                Msg.action(p, "You feel the effects of the Nightmare's curse wear off.");
            } else {
                CURSED.put(p.getUUID(), left - 1);
            }
        }
    }

    // ---------------- Parasites

    /** Parasites land on players (one per five in the fight); they burst out ~18 ticks later unless weakened. */
    void parasites(DataBossEntity boss, ServerPlayer target) {
        List<ServerPlayer> hosts = new ArrayList<>(fighters(boss, target));
        java.util.Collections.shuffle(hosts, new Random(boss.getRandom().nextLong()));
        if (target != null) {
            hosts.remove(target);
            hosts.add(0, target);
        }
        int n = Math.min(hosts.size(), NightmareRules.parasites(players));
        for (int i = 0; i < n; i++) {
            ServerPlayer host = hosts.get(i);
            impregnated.put(host.getUUID(), BossMechanics.PARASITE_BURST_TICKS);
            CURED.remove(host.getUUID());
            Msg.red(host, "The Nightmare has impregnated you with a deadly parasite! Drink a sanfew serum or relicym's balm.");
        }
    }

    private void tickParasites(DataBossEntity boss, ServerPlayer target) {
        Iterator<Map.Entry<UUID, Integer>> it = impregnated.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> e = it.next();
            int left = e.getValue() - 1;
            if (left > 0) {
                e.setValue(left);
                continue;
            }
            it.remove();
            ServerPlayer host = BossFx.world(boss).getServer().getPlayerList().getPlayer(e.getKey());
            if (host == null && target != null && target.getUUID().equals(e.getKey())) {
                host = target;
            }
            if (host != null && host.isAlive()) {
                burst(boss, host);
            }
        }
        parasites.removeIf(p -> !p.mob().isAlive());
        if (parasites.isEmpty() || --parasiteHealTimer > 0) {
            return;
        }
        parasiteHealTimer = BossMechanics.PARASITE_HEAL_INTERVAL;
        Random r = new Random(boss.getRandom().nextLong());
        for (Parasite parasite : parasites) {
            int heal = NightmareRules.parasiteHeal(phosani, r);
            LivingEntity healed = null;
            if (stage == Stage.SHIELD && shieldLeft < shieldMax) {
                heal(boss, heal);
                healed = boss;
            } else if (stage == Stage.DESPERATION && desperationLeft < NightmareRules.PHOSANI_DESPERATION_HEALTH) {
                heal(boss, heal);
                healed = boss;
            } else if (stage == Stage.TOTEMS) {
                // the least charged of the partly charged totems
                Mob least = null;
                for (Mob t : totems) {
                    if (t.isAlive() && t.getHealth() < t.getMaxHealth() && (least == null || t.getHealth() > least.getHealth())) {
                        least = t;
                    }
                }
                if (least != null) {
                    least.heal(heal);
                    healed = least;
                }
            }
            if (healed == null) {
                parasiteAttack(boss, parasite, r);
                continue;
            }
            BossFx.burst(parasite.mob(), ParticleTypes.HEART, 4, 0.4);
            BossFx.burst(healed, ParticleTypes.HEART, 6, 0.8);
            for (ServerPlayer p : fighters(boss, target)) {
                CombatAchievementService.count(p, BossMechanics.COUNT_NIGHTMARE_PARASITE_HEAL, heal);
            }
        }
    }

    /** The parasite bursts out of its host: much weaker (and less damaging) when a cure was drunk in time. */
    void burst(DataBossEntity boss, ServerPlayer host) {
        boolean weakened = CURED.remove(host.getUUID());
        Random r = new Random(boss.getRandom().nextLong());
        int burst = NightmareRules.parasiteBurst(phosani, weakened, r);
        BossHits.deal(boss, host, weakened ? BossMechanics.WEAKENED_PARASITE_BURST : BossMechanics.PARASITE_BURST, null, burst);
        Vec3 at = host.position();
        if (phosani) {
            Vec3 look = host.getLookAngle().multiply(1, 0, 1);
            at = look.lengthSqr() < 1e-6 ? at : at.add(look.normalize().scale(2)); // one tile the way the player faces
        }
        Mob mob = BossFx.spawnMinion(boss, "nightmare_parasite", BossFx.ground(BossFx.world(boss), at.x, at.z, at.y), host);
        if (mob != null) {
            mob.addTag(MINION_TAG);
            mob.setNoAi(true); // the script performs its blasts and attacks
            int hp = Math.min(DataBossEntity.MAX_MC_HEALTH, NightmareRules.parasiteHitpoints(phosani, weakened, burst));
            var max = mob.getAttribute(Attributes.MAX_HEALTH);
            if (max != null) {
                max.setBaseValue(hp);
            }
            mob.setHealth(hp);
            MinionRules.set(mob, new MinionRules.Rule() {
                @Override
                public boolean alwaysMaxHit(ServerPlayer p, LivingEntity m, AttackType type) {
                    return phosani && weakened && type == AttackType.CRUSH; // crush always max hits Phosani's weakened parasites
                }

                @Override
                public int damage(ServerPlayer p, LivingEntity m, AttackType type, int d) {
                    if (phosani && d > 0 && d < m.getHealth()) {
                        CombatAchievementService.flag(p, BossMechanics.FLAG_PHOSANI_MINION_SURVIVED);
                    }
                    return d;
                }
            });
            parasites.add(new Parasite(mob, weakened, host.getUUID(), new boolean[]{false}));
        }
        Msg.red(host, weakened ? "The weakened parasite bursts out of you!" : "The parasite bursts out of you!");
    }

    /** With nothing to heal, a parasite attacks its host: ranged first, then magic, alternating; prayer negates it. */
    private void parasiteAttack(DataBossEntity boss, Parasite parasite, Random r) {
        ServerPlayer host = BossFx.world(boss).getServer().getPlayerList().getPlayer(parasite.host());
        if (host == null && boss.getTarget() instanceof ServerPlayer t && t.getUUID().equals(parasite.host())) {
            host = t;
        }
        if (host == null || !host.isAlive() || host.distanceToSqr(parasite.mob()) > 20 * 20) {
            return;
        }
        AttackType style = parasite.magicNext()[0] ? AttackType.MAGIC : AttackType.RANGED;
        parasite.magicNext()[0] = !parasite.magicNext()[0];
        long roll = CombatFormulas.npcRoll(60, NightmareRules.parasiteAttackBonus(phosani, parasite.weakened()));
        int d = BossHits.roll(host, style, NightmareRules.parasiteMaxHit(phosani, parasite.weakened()), roll, false, r);
        if (BossHits.protectedFrom(host, style)) {
            d = 0;
        }
        BossHits.deal(parasite.mob(), host, "parasite", style, d);
    }

    // ---------------- Surge

    /** She teleports to one edge of the arena and surges across to the other, hitting anyone in her path. */
    void surge(DataBossEntity boss) {
        Vec3 c = homeVec();
        double[][] dirs = {{0, -1}, {-1, 0}, {0, 1}, {1, 0}};
        double[] d = dirs[boss.getRandom().nextInt(4)];
        surgeFrom = BossFx.ground(BossFx.world(boss), c.x + d[0] * 8, c.z + d[1] * 8, c.y);
        surgeTo = BossFx.ground(BossFx.world(boss), c.x - d[0] * 8, c.z - d[1] * 8, c.y);
        boss.teleportTo(surgeFrom.x, surgeFrom.y, surgeFrom.z);
        surgeIn = NightmareRules.SURGE_WIND_UP;
        BossFx.warn(boss, (phosani ? "Phosani's Nightmare" : "The Nightmare") + " prepares to surge across the arena!");
    }

    private void tickSurge(DataBossEntity boss, ServerPlayer target) {
        if (surgeIn <= 0 || surgeFrom == null) {
            return;
        }
        ServerLevel w = BossFx.world(boss);
        Vec3 path = surgeTo.subtract(surgeFrom);
        for (double s = 0; s <= 1; s += 0.05) {
            Vec3 p = surgeFrom.add(path.scale(s));
            w.sendParticles(ParticleTypes.PORTAL, p.x, p.y + 0.3, p.z, 1, 0.6, 0.1, 0.6, 0);
        }
        if (--surgeIn > 0) {
            return;
        }
        double half = boss.getBbWidth() / 2 + 0.6;
        double len = path.horizontalDistance();
        Vec3 dir = path.multiply(1, 0, 1).normalize();
        Random r = new Random(boss.getRandom().nextLong());
        int max = phosani ? NightmareRules.PHOSANI_SURGE_MAX : NightmareRules.SURGE_MAX;
        for (ServerPlayer p : fighters(boss, target)) {
            Vec3 rel = p.position().subtract(surgeFrom).multiply(1, 0, 1);
            double along = rel.dot(dir);
            double off = rel.subtract(dir.scale(along)).horizontalDistance();
            if (along >= -half && along <= len + half && off <= half) {
                BossHits.deal(boss, p, "surge", null, r.nextInt(max + 1));
            }
        }
        boss.teleportTo(surgeTo.x, surgeTo.y, surgeTo.z);
        w.sendParticles(ParticleTypes.PORTAL, surgeTo.x, surgeTo.y + 1, surgeTo.z, 40, 0.8, 1, 0.8, 0.3);
        surgeFrom = null;
    }

    // ---------------- Sleepwalkers

    /** Sleepwalkers walk in from the edges of the arena; any hit kills one. */
    void summonWalkers(DataBossEntity boss, int count) {
        Vec3 c = homeVec();
        for (int i = 0; i < count; i++) {
            double a = boss.getRandom().nextDouble() * Math.PI * 2;
            Mob m = BossFx.spawnMinion(boss, BossMechanics.PHOSANI_SLEEPWALKER,
                    BossFx.ground(BossFx.world(boss), c.x + Math.cos(a) * 11, c.z + Math.sin(a) * 11, c.y), null);
            if (m == null) {
                continue;
            }
            m.addTag(MINION_TAG);
            walkers.add(m);
            MinionRules.set(m, new MinionRules.Rule() {
                @Override
                public double accuracy(ServerPlayer p, LivingEntity mob, AttackType type) {
                    return MinionRules.ALWAYS_HITS;
                }

                @Override
                public int damage(ServerPlayer p, LivingEntity mob, AttackType type, int d) {
                    return MinionRules.lethal(mob);
                }
            });
        }
        if (!desperate) {
            setTotal = Math.max(1, count);
            setReached = 0;
            walkersMoveAt = now + 4;
            BossFx.warn(boss, "Sleepwalkers stumble towards the Nightmare - stop them before they reach her!");
        }
    }

    private void tickWalkers(DataBossEntity boss, ServerPlayer target) {
        Iterator<Mob> it = walkers.iterator();
        while (it.hasNext()) {
            Mob m = it.next();
            if (!m.isAlive()) {
                it.remove();
                continue;
            }
            if (now < walkersMoveAt) {
                continue;
            }
            Vec3 d = boss.position().subtract(m.position());
            double len = d.horizontalDistance();
            if (len <= boss.getBbWidth() / 2 + 1.0) {
                m.discard();
                it.remove();
                if (desperate) {
                    CombatAchievementService.flagNear(boss, BossMechanics.FLAG_PHOSANI_SLEEPWALKER);
                    CombatAchievementService.flagNear(boss, BossMechanics.FLAG_PHOSANI_SLEEPWALKER_ANY);
                    for (ServerPlayer p : fighters(boss, target)) {
                        BossHits.deal(boss, p, "sleepwalker", null, BossMechanics.DESPERATION_SLEEPWALKER_DAMAGE);
                    }
                } else {
                    setReached++;
                    if (phosani) {
                        CombatAchievementService.flagNear(boss, BossMechanics.FLAG_PHOSANI_SLEEPWALKER_ANY);
                    }
                }
                BossFx.burst(boss, ParticleTypes.WITCH, 10, 1.0);
                continue;
            }
            // Desperation sleepwalkers reach her ~20 ticks after appearing at the arena's edge (11 blocks away).
            double step = Math.min(desperate ? 11.0 / BossMechanics.DESPERATION_WALKER_TICKS : 0.35, len);
            m.teleportTo(m.getX() + d.x / len * step, m.getY(), m.getZ() + d.z / len * step);
        }
        if (stage == Stage.WALKERS && setTotal > 0 && walkers.isEmpty()) {
            // Every sleepwalker of the set is dead or arrived: the power blast.
            int dmg = BossMechanics.sleepwalkerBlast(setReached, setTotal);
            setTotal = 0;
            BossFx.say(boss, setReached == 0 ? "The Nightmare's power blast fizzles." : "The Nightmare unleashes a power blast!");
            for (ServerPlayer p : fighters(boss, target)) {
                BossHits.deal(boss, p, "power_blast", null, dmg);
            }
            if (phosani && phase >= NightmareRules.SHIELD_PHASES) {
                startDesperation(boss);
            } else {
                startShield(boss);
            }
        }
    }

    // ---------------- Spores

    void placeSpores(DataBossEntity boss) {
        spores.clear();
        Vec3 c = homeVec();
        for (int i = 0; i < BossMechanics.SPORES; i++) {
            spores.add(around(boss, c, 8));
        }
        sporesLeft = 30; // approx: how long they stay
        BossFx.warn(boss, "Spores sprout around the arena! Don't stand next to them.");
    }

    /** Spores burst when a player comes within a tile; weary players can't run and their yawns drain prayer (Phosani). */
    void tickSpores(DataBossEntity boss) {
        if (!spores.isEmpty()) {
            List<ServerPlayer> near = BossFx.players(boss, 32);
            if (boss.getTarget() instanceof ServerPlayer t && !near.contains(t)) {
                near = new ArrayList<>(near);
                near.add(t);
            }
            Iterator<Vec3> it = spores.iterator();
            while (it.hasNext()) {
                Vec3 sp = it.next();
                BossFx.world(boss).sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, sp.x, sp.y + 0.4, sp.z, 2, 0.2, 0.2, 0.2, 0);
                for (ServerPlayer p : near) {
                    if (Math.abs(p.getX() - sp.x) <= 1.5 && Math.abs(p.getZ() - sp.z) <= 1.5) {
                        burstSpore(boss, p, sp);
                        it.remove();
                        break;
                    }
                }
            }
            if (--sporesLeft <= 0) {
                spores.clear();
            }
        }
        if (weary.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Integer>> wi = weary.entrySet().iterator();
        while (wi.hasNext()) {
            Map.Entry<UUID, Integer> e = wi.next();
            ServerPlayer p = BossFx.world(boss).getServer().getPlayerList().getPlayer(e.getKey());
            if (p == null && boss.getTarget() instanceof ServerPlayer t && t.getUUID().equals(e.getKey())) {
                p = t;
            }
            int left = e.getValue() - 1;
            if (p == null || left <= 0) {
                SLOWED.remove(e.getKey());
                wi.remove();
                continue;
            }
            e.setValue(left);
            if (p.isSprinting()) {
                // No running (as when out of run energy): about walking pace while they try to sprint (renewed each
                // game tick they do, so it wears off as they stop); walking itself is not slowed.
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK + 2, 1, false, false));
                p.setSprinting(false);
            }
            if (phosani && left % 5 == 0) {
                BossFx.drainPrayer(p, BossMechanics.SPORE_YAWN_PRAYER);
                Msg.action(p, "You yawn...");
            }
        }
    }

    void burstSpore(DataBossEntity boss, ServerPlayer p, Vec3 at) {
        BossFx.world(boss).sendParticles(ParticleTypes.SNEEZE, at.x, at.y + 0.6, at.z, 20, 0.6, 0.4, 0.6, 0.02);
        weary.put(p.getUUID(), BossMechanics.SPORE_WEARY_TICKS);
        SLOWED.put(p.getUUID(), new long[]{phosani ? BossMechanics.SPORE_SLOW_TICKS_PHOSANI : BossMechanics.SPORE_SLOW_TICKS,
                com.runecraft.action.ActionManager.mcTick()
                        + (long) BossMechanics.SPORE_WEARY_TICKS * com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK});
        CombatAchievementService.flag(p, BossMechanics.FLAG_NIGHTMARE_SPORED);
        Msg.red(p, "A spore bursts in your face and you feel weary.");
    }

    boolean weary(ServerPlayer p) {
        return weary.containsKey(p.getUUID());
    }

    // ------------------------------------------------------------------ the game tick

    @Override
    protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
        if (!cleanedUp) {
            cleanedUp = true;
            clearStrays(boss); // a boss reloaded from disk: what its old script placed is no longer tracked
        }
        if (stage == Stage.DEAD) {
            if (boss.isAlive()) {
                kill(boss); // a killing blow that didn't land (a hit refused on its way): she still dies
            }
            return;
        }
        if (stage == Stage.ASLEEP) {
            BossFx.world(boss).sendParticles(ParticleTypes.WITCH, boss.getX(), boss.getY() + boss.getBbHeight() + 0.3, boss.getZ(),
                    2, 0.4, 0.2, 0.4, 0.01);
            if (--wakeIn <= 0) {
                wake(boss);
            }
            return;
        }
        tickPending(boss, target);
        tickSurge(boss, target);
        tickFlowers(boss, target);
        tickSpores(boss);
        tickHusks(boss);
        tickParasites(boss, target);
        tickWalkers(boss, target);
        if (deactivateTimer > 0) {
            deactivateTimer--;
        }
        if (stage == Stage.SHIELD) {
            BossFx.ring(BossFx.world(boss), boss.position(), 2.0, ParticleTypes.WITCH);
        } else if (stage == Stage.TOTEMS) {
            for (Mob t : totems) {
                if (!t.isAlive() && charged.stream().noneMatch(v -> v.distanceToSqr(t.position()) < 0.5)) {
                    charged.add(t.position());
                    BossFx.warn(boss, "A totem is fully charged! (" + charged.size() + "/" + NightmareRules.TOTEMS + ")");
                }
            }
            for (Vec3 v : charged) {
                BossFx.column(BossFx.world(boss), v, ParticleTypes.END_ROD, 4);
            }
            if (!totems.isEmpty() && totems.stream().noneMatch(Mob::isAlive)) {
                blast(boss);
                if (stage == Stage.DEAD) {
                    return;
                }
            }
        }
        if (desperate && walkers.isEmpty() && --walkerTimer <= 0) {
            // Desperation: one sleepwalker at a time, the next as soon as the last is gone (approx: a 2-tick gap).
            walkerTimer = 2;
            summonWalkers(boss, 1);
            walkersMoveAt = now;
        }
        float h = mappedHealth(boss);
        if (Math.abs(boss.getHealth() - h) > 0.5f && boss.isAlive()) {
            boss.setHealth(h);
        }
    }

    /** The players fighting her: everyone in her dream (the arena), and her target. */
    Set<ServerPlayer> fighters(DataBossEntity boss, ServerPlayer target) {
        Set<ServerPlayer> out = new LinkedHashSet<>(BossFx.players(boss, 24));
        if (target != null && target.isAlive()) {
            out.add(target);
        }
        return out;
    }

    /**
     * Her target: she focuses on the player whose best defence bonus is the highest of all (Mod Ash); a tank in the best
     * defensive gear keeps her attention.
     */
    @Override
    protected ServerPlayer target(DataBossEntity boss) {
        ServerPlayer current = super.target(boss);
        if (phosani || now % 10 != 0) {
            return current;
        }
        ServerPlayer best = current;
        int bestDef = current == null ? Integer.MIN_VALUE : bestDefence(current);
        for (ServerPlayer p : BossFx.players(boss, 24)) {
            int d = bestDefence(p);
            if (d > bestDef) {
                best = p;
                bestDef = d;
            }
        }
        if (best != null && best != current) {
            boss.setTarget(best);
        }
        return best;
    }

    private static int bestDefence(ServerPlayer p) {
        var s = com.runecraft.combat.Equipment.total(p, Players.data(p).skills());
        return Math.max(Math.max(s.stabDefence(), s.slashDefence()), Math.max(Math.max(s.crushDefence(), s.magicDefence()), s.rangedDefence()));
    }

    // ------------------------------------------------------------------ the end of the fight

    @Override
    public void onDeath(DataBossEntity boss, ServerPlayer killer) {
        stage = Stage.DEAD;
        cleanUp(boss);
    }

    @Override
    public void onRemoved(DataBossEntity boss) {
        cleanUp(boss); // reset: totems, husks, sleepwalkers and parasites go, and nobody stays slowed or cursed
    }

    private void cleanUp(DataBossEntity boss) {
        spores.clear();
        weary.keySet().forEach(SLOWED::remove);
        weary.clear();
        totems.forEach(Mob::discard);
        husks.forEach(Mob::discard);
        walkers.forEach(Mob::discard);
        parasites.forEach(p -> p.mob().discard());
        totems.clear();
        husks.clear();
        walkers.clear();
        parasites.clear();
        impregnated.clear();
        if (bound != null) {
            BossFx.unstun(bound);
            bound = null;
        }
        for (UUID id : contribution.keySet()) {
            CURSED.remove(id);
        }
        if (boss.getTarget() instanceof ServerPlayer t) {
            CURSED.remove(t.getUUID());
        }
        for (ServerPlayer p : BossFx.players(boss, 24)) {
            CURSED.remove(p.getUUID());
        }
    }

    /** Discards totems, husks, parasites and sleepwalkers near her that this script doesn't track (left by a reload). */
    private void clearStrays(DataBossEntity boss) {
        for (Mob m : BossFx.world(boss).getEntitiesOfClass(Mob.class, boss.getBoundingBox().inflate(32),
                e -> e.getTags().contains(MINION_TAG))) {
            m.discard();
        }
    }

    @Override
    public void writeNbt(DataBossEntity boss, CompoundTag nbt) {
        if (stage == Stage.ASLEEP || stage == Stage.DEAD) {
            return;
        }
        nbt.putInt("NmStage", stage.ordinal());
        nbt.putInt("NmPhase", phase);
        nbt.putInt("NmPlayers", players);
        nbt.putInt("NmShield", shieldLeft);
        nbt.putInt("NmDesperation", desperationLeft);
    }

    @Override
    public void readNbt(DataBossEntity boss, CompoundTag nbt) {
        if (!nbt.contains("NmStage")) {
            return;
        }
        players = Math.max(1, nbt.getInt("NmPlayers"));
        phase = Math.max(0, Math.min(NightmareRules.SHIELD_PHASES - 1, nbt.getInt("NmPhase")));
        shieldMax = NightmareRules.shield(phosani, players);
        totemCharge = NightmareRules.totemCharge(phosani, players);
        if (phosani) {
            sets = NightmareRules.phosaniSets(new Random(boss.getRandom().nextLong()));
        }
        Stage saved = Stage.values()[Math.max(0, Math.min(Stage.values().length - 1, nbt.getInt("NmStage")))];
        if (saved == Stage.DESPERATION) {
            phase = NightmareRules.SHIELD_PHASES;
            stage = Stage.DESPERATION;
            desperate = true;
            desperationLeft = Math.max(1, nbt.getInt("NmDesperation"));
        } else {
            // A reload picks the phase up at its shield (the totems and sleepwalkers it had are gone): what was left of it,
            // or a fresh one after a blast.
            stage = Stage.SHIELD;
            shieldLeft = saved == Stage.SHIELD ? Math.max(1, nbt.getInt("NmShield")) : shieldMax;
        }
    }

    // ------------------------------------------------------------------ shared loot

    /**
     * A kill's loot. The killer (the MVP for The Nightmare) rolls the common table with the boss's tertiaries; the two
     * unique tables are rolled once a kill (a second time with a chance of party size - 5 %) and each unique goes to one
     * eligible player weighted by damage. Every other eligible player gets their own common roll, regular bones, a kill count,
     * the 1/200 clue and 1/2,000 jar and their pet roll.
     */
    static List<Drops.Rolled> rollLoot(ServerPlayer killer, DataBossEntity boss, List<Drop> table) {
        String id = boss.def() == null ? "the_nightmare" : boss.def().id;
        boolean phosani = "phosanis_nightmare".equals(id);
        NightmareScript n = boss.script() instanceof NightmareScript s ? s : null;
        int players = n == null ? 1 : n.players;
        net.minecraft.util.RandomSource random = boss.getRandom();
        Set<String> uniqueIds = new java.util.HashSet<>(NightmareRules.INQUISITOR_TABLE);
        uniqueIds.addAll(NightmareRules.ORB_TABLE);
        List<Drop> commons = new ArrayList<>();
        List<Drop> inquisitor = new ArrayList<>();
        List<Drop> orbs = new ArrayList<>();
        for (Drop d : table) {
            if (NightmareRules.INQUISITOR_TABLE.contains(d.item())) {
                inquisitor.add(d);
            } else if (NightmareRules.ORB_TABLE.contains(d.item())) {
                orbs.add(d);
            } else {
                commons.add(d);
            }
        }
        // the uniques of the kill
        List<Drop> uniques = new ArrayList<>();
        int rolls = random.nextDouble() < NightmareRules.secondUniqueRoll(players) ? 2 : 1;
        for (int i = 0; i < rolls; i++) {
            uniques.addAll(BossLoot.pick(inquisitor, 1, random::nextInt));
            uniques.addAll(BossLoot.pick(orbs, 1, random::nextInt));
        }
        // who shares it: the killer and every other eligible player still around
        List<ServerPlayer> others = new ArrayList<>();
        Map<ServerPlayer, Integer> weight = new LinkedHashMap<>();
        weight.put(killer, n == null ? 1 : Math.max(1, sum(n.contribution.get(killer.getUUID()))));
        if (n != null && !phosani) {
            for (Map.Entry<UUID, int[]> e : n.contribution.entrySet()) {
                ServerPlayer p = BossFx.world(boss).getServer().getPlayerList().getPlayer(e.getKey());
                if (p != null && p != killer && p.isAlive() && NightmareRules.eligible(e.getValue()[0], e.getValue()[1])) {
                    others.add(p);
                    weight.put(p, Math.max(1, sum(e.getValue())));
                }
            }
        }
        Map<ServerPlayer, List<Drop>> won = new HashMap<>();
        for (Drop u : uniques) {
            won.computeIfAbsent(weighted(weight, random), k -> new ArrayList<>()).add(new Drop(u.item(), 1, 1, 1));
        }
        // the killer's roll
        List<Drop> mine = new ArrayList<>(BossLoot.pick(id, commons, random::nextInt));
        mine.addAll(won.getOrDefault(killer, List.of()));
        if (phosani) {
            slepeyTablet(killer, mine);
        } else {
            nightmarePetTopUp(killer, boss, players);
        }
        // the others' rolls
        BossDef def = boss.def();
        List<Drop> theirTable = new ArrayList<>();
        for (Drop d : commons) {
            if (d.item().equals("big_bones")) {
                continue; // the MVP's
            }
            theirTable.add(d.item().startsWith("clue_scroll") ? new Drop(d.item(), d.min(), d.max(), NightmareRules.CLUE)
                    : d.item().equals("jar_of_dreams") ? new Drop(d.item(), d.min(), d.max(), NightmareRules.JAR) : d);
        }
        for (ServerPlayer p : others) {
            List<Drop> lines = new ArrayList<>(BossLoot.pick(id, theirTable, random::nextInt));
            lines.addAll(won.getOrDefault(p, List.of()));
            for (Drops.Rolled r : Drops.resolve(lines, random)) {
                Inv.give(p, Drops.withDoses(CombatHooks.dropStack(p, boss, r.item(), r.count()), r.doses()));
                CollectionLog.obtain(p, r.item());
            }
            if (def != null && def.pet != null && random.nextInt(NightmareRules.petRate(false, players)) == 0) {
                Inv.give(p, def.pet, 1);
                CollectionLog.obtain(p, def.pet);
                p.getServer().getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString()
                        + " has a funny feeling like they're being followed.").withStyle(ChatFormatting.GOLD), false);
            }
            int kc = Players.data(p).incrementKillCount(id);
            Msg.game(p, "Your The Nightmare kill count is: " + kc + ".");
            if (def != null) {
                com.runecraft.content.GameEvents.fireKill(p, boss, com.runecraft.combat.MonsterStats.from(def));
            }
        }
        return Drops.resolve(mine, random);
    }

    private static int sum(int[] c) {
        return c == null ? 0 : c[0] + c[1];
    }

    private static ServerPlayer weighted(Map<ServerPlayer, Integer> weight, net.minecraft.util.RandomSource random) {
        int total = 0;
        for (int w : weight.values()) {
            total += w;
        }
        int r = random.nextInt(Math.max(1, total));
        for (Map.Entry<ServerPlayer, Integer> e : weight.entrySet()) {
            r -= e.getValue();
            if (r < 0) {
                return e.getKey();
            }
        }
        return weight.keySet().iterator().next();
    }

    /** Phosani's Slepey tablet: always by the 25th kill while the player has none; a second one crumbles. */
    private static void slepeyTablet(ServerPlayer killer, List<Drop> lines) {
        boolean has = Inv.has(killer, "slepey_tablet", 1)
                || com.runecraft.content.systems.SysState.of(killer).unlocks.contains("Slepe");
        int kcAfter = Players.data(killer).killCount("phosanis_nightmare") + 1;
        boolean rolled = lines.removeIf(d -> d.item().equals("slepey_tablet"));
        if (has) {
            if (rolled) {
                Msg.game(killer, "The tablet crumbles to dust as you touch it.");
            }
            return;
        }
        if (rolled || NightmareRules.slepeyTabletGuaranteed(kcAfter, false)) {
            lines.add(new Drop("slepey_tablet", 1, 1, 1));
        }
    }

    /**
     * The Nightmare's pet is 1/800 for each player in the fight (counted at the start), 1/4,000 from five: the engine rolls
     * the 1/4,000 for the killer, this tops it up for smaller teams.
     */
    private static void nightmarePetTopUp(ServerPlayer killer, DataBossEntity boss, int players) {
        int base = com.runecraft.osrs.content.extras.Pets.engineRate("the_nightmare");
        int want = NightmareRules.petRate(false, players);
        if (base <= want || boss.def() == null || boss.def().pet == null) {
            return;
        }
        double extra = 1.0 / want - 1.0 / base; // P(engine or top-up) ~ 1/want
        if (boss.getRandom().nextDouble() < extra) {
            Inv.give(killer, boss.def().pet, 1);
            CollectionLog.obtain(killer, boss.def().pet);
            killer.getServer().getPlayerList().broadcastSystemMessage(Component.literal(killer.getName().getString()
                    + " has a funny feeling like they're being followed.").withStyle(ChatFormatting.GOLD), false);
        }
    }
}
