package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.content.systems.CombatAchievementService;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.bosses.BossMechanics;
import com.runecraft.osrs.content.bosses.Dt2Bosses;
import com.runecraft.osrs.content.systems.CombatAchievements;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The Whisperer (OSRS Wiki "The Whisperer/Strategies"; the Desert Treasure II quest version hits for less).
 * <ul>
 *   <li>Volleys of three projectiles a tick apart (five awakened), soul bolts (magic, 36) and tentacle lashes (ranged, 40),
 *   all one style before her first special, then two and one, after the third alternating; protection prayers block them.
 *   After each volley her tentacles converge on the player's tile: move off it or take 20 (awakened 30).</li>
 *   <li>Next to her she whacks with her tentacles instead: two hits, the second the same as the first, through prayer (which
 *   only trims it) - stay away from her.</li>
 *   <li>At 80, 55 and 30% (awakened about every tenth of her 2,700 hitpoints) a special, in the order Corrupted Seeds,
 *   Screech, Soul Siphon (or starting at the Screech), taking 70% less damage meanwhile:
 *   <b>Screech</b>: from the south end of the arena, three blasts (five awakened) of 45 (70) at anyone she can see - hide
 *   behind the floating columns, which crumble as she screeches and break when someone hides behind them;
 *   <b>Soul Siphon</b>: lost souls in four sets (Vita 2: heal 20% hitpoints, Oratio 3: 20% prayer, Sanitas 3, Mors 4: 50 to
 *   her, 75 if every soul dies); kill a full set within 20 ticks or take 50 while she heals 100 (awakened: every soul, 35
 *   ticks); <b>Corrupted Seeds</b>: step on every light seed before time runs out (or take 75), never on a dark one (15-25,
 *   awakened 60-75). After each special her binding orb holds the player for ~8 seconds while she comes for them.</li>
 *   <li>At 0 hitpoints she heals 140 (awakened 250), pulls the player into the Shadow Realm and attacks every 2 ticks, one
 *   projectile at a time (ranged first, switching every two; awakened magic first, switching every attack), a tentacle
 *   with each.</li>
 * </ul>
 * approx: sanity, the blackstone fragment and the Shadow Realm are not modelled (seeds, columns and souls are always visible);
 * for Dark Memories a player's pre-enrage realm time is the Soul Siphon up to their last lost-soul kill (souls are only hit
 * from the realm), see {@link BossMechanics#whispererRealmTicks}.
 */
final class WhispererScript extends ScriptedBoss {
    private enum Special { SEEDS, SCREECH, SIPHON }

    private record Seed(Vec3 pos, boolean light) {
    }

    private static final class Column {
        final Vec3 pos;
        final List<BlockPos> blocks = new ArrayList<>();
        int health;

        Column(Vec3 pos, int health) {
            this.pos = pos;
            this.health = health;
        }
    }

    private static final String NBT_COLUMNS = "WhispererColumns";

    /** Max hit scale: 1 for the boss, below 1 for the weaker Desert Treasure II quest version. */
    private final double scale;
    /** 0: seeds first, 1: screech first (-1 until her first game tick). */
    private int rotation = -1;
    private int specialsDone;
    private Special special;
    private int specialTick;
    private boolean nextMagic;
    private final List<AttackDef> volley = new ArrayList<>();
    private boolean enraged;
    private boolean enragePending;
    private int enrageAttacks;
    /** Game ticks she walks after the player once her binding orb has landed. */
    private int chase;
    /** The second hit of her tentacle whack (the first's damage), landing next tick, or -1. */
    private int echo = -1;
    private ServerPlayer echoTarget;
    private final List<Column> columns = new ArrayList<>();
    private int screechesLeft;
    private int nextScreech;
    private final List<Mob> souls = new ArrayList<>();
    private final Map<Mob, Integer> soulSet = new HashMap<>();
    private final Set<Integer> setsDone = new LinkedHashSet<>();
    /** Lost souls whose kill was counted as Shadow Realm time, and each killer's realm ticks counted this siphon. */
    private final Set<Mob> soulsCounted = new java.util.HashSet<>();
    private final Map<java.util.UUID, Integer> realmTicks = new HashMap<>();
    private final List<Seed> seeds = new ArrayList<>();
    /** Column blocks saved with her when she was unloaded mid-Screech: cleared on her first game tick back. */
    private long[] savedColumns;

    WhispererScript() {
        this(1.0);
    }

    WhispererScript(double scale) {
        this.scale = scale;
    }

    @Override
    protected double multiplier(DataBossEntity boss) {
        return super.multiplier(boss) * scale;
    }

    @Override
    protected boolean driven() {
        return true;
    }

    @Override
    protected boolean wantsMelee(DataBossEntity boss, ServerPlayer target) {
        return chase > 0;
    }

    @Override
    protected boolean canAttack(DataBossEntity boss) {
        return special == null && !enragePending;
    }

    @Override
    protected int attackDelay(DataBossEntity boss, AttackDef attack) {
        if (enraged) {
            return 2;
        }
        return volley.isEmpty() ? super.attackDelay(boss, attack) : 1;
    }

    private int scaled(DataBossEntity boss, AttackDef a) {
        return (int) Math.round(maxHit(boss, a) * scale);
    }

    private int scaled(DataBossEntity boss, int normal, int awakened) {
        return (int) Math.round((boss.awakened() ? awakened : normal) * scale);
    }

    /** Players in the fight: those around her plus her target (which may be a test player). */
    private static List<ServerPlayer> fighters(DataBossEntity boss, ServerPlayer target) {
        List<ServerPlayer> list = new ArrayList<>(BossFx.players(boss, 24));
        if (target != null && target.isAlive() && !list.contains(target)) {
            list.add(target);
        }
        return list;
    }

    /** Her hitpoints in her own (variant's) terms. */
    private static double trueHp(DataBossEntity boss) {
        return percent(boss) / 100.0 * boss.trueHitpoints();
    }

    @Override
    protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
        if (enragePending) {
            return 0; // between her "death" and the enrage
        }
        double d = special != null ? damage * BossMechanics.WHISPERER_SPECIAL_DAMAGE : damage;
        int out = (int) d + (boss.getRandom().nextDouble() < d - (int) d ? 1 : 0);
        if (!enraged && !enragePending && out > 0) {
            int hp = boss.trueHitpoints();
            double mc = hp > MAX_MC_HEALTH ? out * (double) MAX_MC_HEALTH / hp : out;
            if (mc >= boss.getHealth()) {
                // 0 hitpoints the first time: she heals and enrages next tick instead of dying
                enragePending = true;
                boss.setHealth(1f);
                return 0;
            }
        }
        return out;
    }

    @Override
    protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
        if (distance < 1.6 && echo < 0) {
            volley.clear(); // she cancels a volley for an immediate melee attack
            return Dt2Bosses.WHISPER_MELEE;
        }
        if (enraged) {
            enrageAttacks++;
            boolean magic = boss.awakened() ? enrageAttacks % 2 == 1 : (enrageAttacks - 1) / 2 % 2 == 1;
            tentacles(boss, target);
            return magic ? Dt2Bosses.WHISPER_MAGIC : Dt2Bosses.WHISPER_RANGED;
        }
        if (volley.isEmpty()) {
            for (boolean magic : BossMechanics.whispererVolley(specialsDone, boss.awakened(), nextMagic)) {
                volley.add(magic ? Dt2Bosses.WHISPER_MAGIC : Dt2Bosses.WHISPER_RANGED);
            }
            nextMagic = boss.getRandom().nextBoolean(); // approx: which style leads each volley
        }
        AttackDef a = volley.remove(0);
        if (volley.isEmpty()) {
            tentacles(boss, target);
        }
        return a;
    }

    /** Her tentacles surface around the player and converge on the tile they stood on (fixed damage, unnamed: "mechanic"). */
    private void tentacles(DataBossEntity boss, ServerPlayer target) {
        int dmg = scaled(boss, BossMechanics.WHISPERER_TENTACLE, BossMechanics.WHISPERER_TENTACLE_AWAKENED);
        strike(boss, target.position(), 3, 0, 0.9, ParticleTypes.SQUID_INK, (hit, pos) -> {
            for (ServerPlayer h : hit) {
                CombatHandler.applyDamage(boss, h, dmg);
            }
        });
    }

    @Override
    public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
        if (last != Dt2Bosses.WHISPER_MELEE) {
            return damage;
        }
        if (damage <= 0) {
            damage = boss.getRandom().nextInt(scaled(boss, Dt2Bosses.WHISPER_MELEE) + 1); // it always lands
        }
        if (BossFx.protects(player, Prayer.PROTECT_FROM_MELEE)) {
            damage = (int) Math.round(damage * BossMechanics.WHISPERER_MELEE_THROUGH_PRAYER);
        }
        echo = damage;
        echoTarget = player;
        return damage;
    }

    @Override
    protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
        ServerLevel w = BossFx.world(boss);
        if (savedColumns != null) {
            for (long pos : savedColumns) {
                clearColumnBlock(w, BlockPos.of(pos));
            }
            savedColumns = null;
        }
        if (rotation < 0) {
            rotation = boss.getRandom().nextInt(2);
            nextMagic = boss.getRandom().nextBoolean();
        }
        if (echo >= 0) {
            if (echoTarget != null && echoTarget.isAlive() && echoTarget.distanceToSqr(boss) < 6 * 6) {
                CombatHooks.nameAttack(Dt2Bosses.WHISPER_MELEE.name(), AttackType.CRUSH);
                CombatHandler.applyDamage(boss, echoTarget, echo);
            }
            echo = -1;
            echoTarget = null;
        }
        if (enragePending) {
            enrage(boss, target);
            return;
        }
        if (chase > 0) {
            chase--;
        }
        if (special != null) {
            specialTick++;
            switch (special) {
                case SCREECH -> tickScreech(boss, target);
                case SIPHON -> tickSiphon(boss, target);
                case SEEDS -> tickSeeds(boss, target);
            }
            return;
        }
        if (!enraged && target != null
                && BossMechanics.whispererSpecialDue(specialsDone, trueHp(boss), boss.trueHitpoints(), boss.awakened())) {
            startSpecial(boss, target);
        }
    }

    private void enrage(DataBossEntity boss, ServerPlayer target) {
        enragePending = false;
        enraged = true;
        endSpecial(boss, null, false);
        volley.clear();
        boss.setHealth(Math.max(1f, mcHp(boss, boss.awakened() ? BossMechanics.WHISPERER_ENRAGE_HEAL_AWAKENED
                : BossMechanics.WHISPERER_ENRAGE_HEAL)));
        BossFx.say(boss, "The Whisperer drags you into the Shadow Realm and attacks in a frenzy!");
        for (ServerPlayer p : fighters(boss, target)) {
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 60 * 3, 0, false, false));
        }
        cooldown = 2;
    }

    // ---------------- specials ----------------

    private void startSpecial(DataBossEntity boss, ServerPlayer target) {
        String which = BossMechanics.whispererSpecial(rotation, specialsDone);
        specialsDone++;
        specialTick = 0;
        volley.clear();
        Vec3 c = homeVec();
        ServerLevel w = BossFx.world(boss);
        switch (which) {
            case "screech" -> {
                special = Special.SCREECH;
                Vec3 south = BossFx.ground(w, c.x, c.z + 9, c.y);
                boss.getNavigation().stop();
                boss.teleportTo(south.x, south.y, south.z);
                raiseColumns(boss);
                screechesLeft = boss.awakened() ? BossMechanics.WHISPERER_SCREECHES_AWAKENED : BossMechanics.WHISPERER_SCREECHES;
                nextScreech = 5;
                BossFx.say(boss, "The Whisperer gathers energy for a screech! Hide behind the floating columns!");
            }
            case "siphon" -> {
                special = Special.SIPHON;
                boss.getNavigation().stop();
                boss.teleportTo(c.x, c.y, c.z);
                summonSouls(boss, target);
                BossFx.say(boss, "The Whisperer siphons the lost souls! Kill a whole set before her chant completes!");
            }
            default -> {
                special = Special.SEEDS;
                boss.getNavigation().stop();
                boss.teleportTo(c.x, c.y, c.z);
                scatterSeeds(boss);
                BossFx.say(boss, "The Whisperer scatters corrupted seeds! Step on every light seed - avoid the dark ones!");
            }
        }
    }

    /** Ends the special: clears what it left, and (unless enraging) her binding orb holds the target while she comes. */
    private void endSpecial(DataBossEntity boss, ServerPlayer target, boolean bind) {
        if (special == null) {
            return;
        }
        ServerLevel w = BossFx.world(boss);
        for (Column col : columns) {
            col.blocks.forEach(p -> clearColumnBlock(w, p));
        }
        columns.clear();
        souls.forEach(Mob::discard);
        souls.clear();
        soulSet.clear();
        setsDone.clear();
        soulsCounted.clear();
        realmTicks.clear();
        seeds.clear();
        if (special == Special.SCREECH) {
            Vec3 c = homeVec();
            boss.teleportTo(c.x, c.y, c.z);
        }
        special = null;
        if (bind && target != null) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BossMechanics.WHISPERER_BIND_TICKS * 12, 10, false, true));
            Msg.red(target, "The Whisperer's orb binds you in place!");
            chase = 6;
        }
        cooldown = 2;
    }

    // Screech

    private void raiseColumns(DataBossEntity boss) {
        ServerLevel w = BossFx.world(boss);
        Vec3 c = homeVec();
        // between her (at the south end) and the rest of the arena; 20/40/60 health as in OSRS (awakened up to 100, approx)
        double[][] spots = {{-6, -1}, {-3, -4}, {3, -4}, {6, -1}, {0, -2}, {0, -7}};
        int[] health = boss.awakened() ? new int[]{20, 40, 60, 80, 100, 100} : new int[]{20, 20, 40, 40, 60, 60};
        for (int i = 0; i < spots.length; i++) {
            Vec3 at = BossFx.ground(w, c.x + spots[i][0], c.z + spots[i][1], c.y);
            Column col = new Column(at, health[i]);
            BlockPos base = BlockPos.containing(at);
            for (int y = 0; y < 4; y++) {
                BlockPos p = base.above(y);
                if (w.getBlockState(p).isAir()) {
                    w.setBlock(p, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), Block.UPDATE_ALL);
                    col.blocks.add(p.immutable());
                }
            }
            columns.add(col);
        }
    }

    private static void clearColumnBlock(ServerLevel w, BlockPos p) {
        if (w.getBlockState(p).is(Blocks.POLISHED_BLACKSTONE_BRICKS)) {
            w.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private void tickScreech(DataBossEntity boss, ServerPlayer target) {
        if (--nextScreech > 0) {
            BossFx.burst(boss, ParticleTypes.SONIC_BOOM, 1, 0.5);
            return;
        }
        nextScreech = 4;
        screechesLeft--;
        ServerLevel w = BossFx.world(boss);
        BossFx.sound(boss, net.minecraft.sounds.SoundEvents.WARDEN_SONIC_BOOM, 1.0f);
        int dmg = scaled(boss, Dt2Bosses.WHISPER_SCREECH);
        Set<Column> hidingBehind = new java.util.HashSet<>();
        for (ServerPlayer p : fighters(boss, target)) {
            if (boss.hasLineOfSight(p)) {
                CombatHooks.nameAttack(Dt2Bosses.WHISPER_SCREECH.name(), null);
                CombatHandler.applyDamage(boss, p, dmg);
                Msg.red(p, "The screech tears through you!");
            } else {
                Column nearest = null;
                for (Column col : columns) {
                    if (nearest == null || col.pos.distanceToSqr(p.position()) < nearest.pos.distanceToSqr(p.position())) {
                        nearest = col;
                    }
                }
                if (nearest != null) {
                    hidingBehind.add(nearest);
                }
            }
        }
        // every column takes 20 from each screech; the one someone hid behind breaks
        columns.removeIf(col -> {
            col.health -= 20;
            if (col.health > 0 && !hidingBehind.contains(col)) {
                return false;
            }
            col.blocks.forEach(p -> clearColumnBlock(w, p));
            w.sendParticles(ParticleTypes.LARGE_SMOKE, col.pos.x, col.pos.y + 1.5, col.pos.z, 15, 0.3, 1.0, 0.3, 0.02);
            return true;
        });
        if (screechesLeft <= 0) {
            endSpecial(boss, target, true);
        }
    }

    // Soul Siphon

    private void summonSouls(DataBossEntity boss, ServerPlayer target) {
        Vec3 c = homeVec();
        int extra = boss.awakened() ? 1 : 0; // awakened: one more soul in each set
        for (int set = 0; set < BossMechanics.WHISPERER_SOUL_SETS.length; set++) {
            double a = Math.PI / 4 + set * Math.PI / 2;
            int n = BossMechanics.WHISPERER_SOUL_SET_SIZES[set] + extra;
            for (int i = 0; i < n; i++) {
                double along = (i - (n - 1) / 2.0) * 1.4;
                double x = c.x + Math.cos(a) * 6 - Math.sin(a) * along;
                double z = c.z + Math.sin(a) * 6 + Math.cos(a) * along;
                Mob soul = BossFx.spawnMinion(boss, BossMechanics.WHISPERER_LOST_SOUL, BossFx.ground(BossFx.world(boss), x, z, c.y), null);
                if (soul == null) {
                    continue;
                }
                soul.setCustomName(net.minecraft.network.chat.Component.literal("Lost Soul ("
                        + Character.toUpperCase(BossMechanics.WHISPERER_SOUL_SETS[set].charAt(0))
                        + BossMechanics.WHISPERER_SOUL_SETS[set].substring(1) + "!)"));
                MinionRules.set(soul, new MinionRules.Rule() {
                    @Override
                    public double accuracy(ServerPlayer p, net.minecraft.world.entity.LivingEntity mob, AttackType type) {
                        return MinionRules.ALWAYS_HITS;
                    }

                    @Override
                    public int damage(ServerPlayer p, net.minecraft.world.entity.LivingEntity mob, AttackType type, int d) {
                        return MinionRules.lethal(mob);
                    }
                });
                souls.add(soul);
                soulSet.put(soul, set);
            }
        }
    }

    private void tickSiphon(DataBossEntity boss, ServerPlayer target) {
        for (Mob soul : souls) {
            BossFx.world(boss).sendParticles(ParticleTypes.SOUL, soul.getX(), soul.getY() + 1, soul.getZ(), 2, 0.2, 0.4, 0.2, 0.01);
            if (!soul.isAlive() && soulsCounted.add(soul)) {
                // souls are only hit from the Shadow Realm: its killer was in there until now (Dark Memories)
                ServerPlayer killer = soul.getLastHurtByMob() instanceof ServerPlayer p ? p : target;
                if (killer != null) {
                    int counted = realmTicks.getOrDefault(killer.getUUID(), 0);
                    int add = BossMechanics.whispererRealmTicks(specialTick, counted);
                    if (add > 0) {
                        CombatAchievementService.count(killer, CombatAchievements.COUNT_SHADOW_REALM_TICKS, add);
                        realmTicks.put(killer.getUUID(), counted + add);
                    }
                }
            }
        }
        boolean allDead = true;
        for (int set = 0; set < BossMechanics.WHISPERER_SOUL_SETS.length; set++) {
            boolean dead = true;
            for (Map.Entry<Mob, Integer> e : soulSet.entrySet()) {
                if (e.getValue() == set && e.getKey().isAlive()) {
                    dead = false;
                    allDead = false;
                }
            }
            if (dead && setsDone.add(set)) {
                soulEffect(boss, target, set, false);
            }
        }
        int due = boss.awakened() ? BossMechanics.WHISPERER_SIPHON_TICKS_AWAKENED : BossMechanics.WHISPERER_SIPHON_TICKS;
        if (!allDead && specialTick < due) {
            return;
        }
        if (allDead && setsDone.contains(3)) {
            // every soul gone: Mors hits harder
            int bonus = BossMechanics.WHISPERER_MORS_DAMAGE_ALL - BossMechanics.WHISPERER_MORS_DAMAGE;
            boss.setHealth(Math.max(1f, boss.getHealth() - mcHp(boss, bonus)));
        }
        boolean failed = boss.awakened() ? !allDead : setsDone.isEmpty();
        if (failed && target != null) {
            int left = BossMechanics.WHISPERER_SOUL_SETS.length - setsDone.size();
            CombatHooks.nameAttack(Dt2Bosses.WHISPER_SIPHON.name(), null);
            CombatHandler.applyDamage(boss, target, scaled(boss, Dt2Bosses.WHISPER_SIPHON));
            boss.heal(mcHp(boss, BossMechanics.WHISPERER_SIPHON_HEAL * Math.max(1, boss.awakened() ? left : 1)));
            Msg.red(target, "The Whisperer's chant completes, siphoning your soul!");
        }
        endSpecial(boss, target, true);
    }

    /** A full set of lost souls killed: its chant turns on the Whisperer. */
    private void soulEffect(DataBossEntity boss, ServerPlayer target, int set, boolean quiet) {
        if (target == null) {
            return;
        }
        PlayerData data = Players.data(target);
        switch (set) {
            case 0 -> { // Vita!
                target.heal(target.getMaxHealth() * 0.2f);
                Msg.game(target, "Vita! The souls restore your health.");
            }
            case 1 -> { // Oratio!
                int level = data.skills().level(Skill.PRAYER);
                data.setPrayerPoints(Math.min(level, data.prayerPoints() + (int) Math.round(level * 0.2)));
                com.runecraft.network.ServerNetworking.sync(target);
                Msg.game(target, "Oratio! The souls restore your prayer.");
            }
            case 2 -> Msg.game(target, "Sanitas! The souls steady your mind.");
            default -> { // Mors!
                boss.setHealth(Math.max(1f, boss.getHealth() - mcHp(boss, BossMechanics.WHISPERER_MORS_DAMAGE)));
                BossFx.burst(boss, ParticleTypes.SCULK_SOUL, 20, 1.0);
                Msg.game(target, "Mors! The souls turn on the Whisperer.");
            }
        }
    }

    // Corrupted Seeds

    private void scatterSeeds(DataBossEntity boss) {
        seeds.clear();
        Vec3 c = homeVec();
        for (int i = 0; i < 16; i++) {
            Vec3 at = around(boss, c, 10);
            if (at.distanceToSqr(c) < 9) {
                at = around(boss, c, 10);
            }
            seeds.add(new Seed(at, i < 6));
        }
    }

    private void tickSeeds(DataBossEntity boss, ServerPlayer target) {
        ServerLevel w = BossFx.world(boss);
        for (Seed s : seeds) {
            w.sendParticles(s.light() ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.SQUID_INK, s.pos().x, s.pos().y + 0.1, s.pos().z,
                    2, 0.2, 0.02, 0.2, 0);
        }
        int[] dark = boss.awakened() ? BossMechanics.WHISPERER_DARK_SEED_AWAKENED : BossMechanics.WHISPERER_DARK_SEED;
        for (ServerPlayer p : fighters(boss, target)) {
            seeds.removeIf(s -> {
                if (p.position().distanceToSqr(s.pos()) > 0.8 * 0.8) {
                    return false;
                }
                w.sendParticles(ParticleTypes.POOF, s.pos().x, s.pos().y + 0.3, s.pos().z, 6, 0.2, 0.2, 0.2, 0.02);
                if (!s.light()) {
                    CombatHandler.applyDamage(boss, p, (int) Math.round((dark[0] + boss.getRandom().nextInt(dark[1] - dark[0] + 1)) * scale));
                    Msg.red(p, "A corrupted seed bursts under you!");
                }
                return true;
            });
        }
        boolean lightLeft = seeds.stream().anyMatch(Seed::light);
        int due = boss.awakened() ? BossMechanics.WHISPERER_SEED_TICKS_AWAKENED : BossMechanics.WHISPERER_SEED_TICKS;
        if (!lightLeft) {
            BossFx.say(boss, "The corrupted seeds wither.");
            endSpecial(boss, target, true);
        } else if (specialTick >= due) {
            if (target != null) {
                CombatHandler.applyDamage(boss, target, (int) Math.round(BossMechanics.WHISPERER_SEED_FAIL * scale));
                Msg.red(target, "The corrupted seeds erupt around you!");
            }
            endSpecial(boss, target, true);
        }
    }

    // ---------------- clean-up ----------------

    @Override
    public void writeNbt(DataBossEntity boss, CompoundTag nbt) {
        List<Long> all = new ArrayList<>();
        for (Column col : columns) {
            col.blocks.forEach(p -> all.add(p.asLong()));
        }
        if (savedColumns != null) {
            for (long p : savedColumns) {
                all.add(p);
            }
        }
        if (!all.isEmpty()) {
            nbt.putLongArray(NBT_COLUMNS, all.stream().mapToLong(Long::longValue).toArray());
        }
    }

    @Override
    public void readNbt(DataBossEntity boss, CompoundTag nbt) {
        if (nbt.contains(NBT_COLUMNS)) {
            savedColumns = nbt.getLongArray(NBT_COLUMNS);
        }
    }

    @Override
    public void onDeath(DataBossEntity boss, ServerPlayer killer) {
        onRemoved(boss);
        if (killer != null) {
            killer.removeEffect(MobEffects.DARKNESS);
        }
    }

    @Override
    public void onRemoved(DataBossEntity boss) {
        endSpecial(boss, null, false);
        for (ServerPlayer p : BossFx.players(boss, 32)) {
            p.removeEffect(MobEffects.DARKNESS);
        }
    }
}
