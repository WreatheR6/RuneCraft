package com.runecraft.selftest;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.Mechanics;
import com.runecraft.boss.ThreatCues;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.CombatZones;
import com.runecraft.combat.PrayerService;
import com.runecraft.entity.BossEntity;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Experience;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.ThreatTiming;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Boss;
import com.runecraft.osrs.data.Monster;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Round 41 real-time delivery of monster hits ({@code npc:*}, see {@code Mechanics} and {@code ThreatTiming}): OSRS hits never
 * push, projectiles are judged on impact and fizzle on walls, telegraphs keep their 36 MC floor with redrawn cues, melee winds up
 * and misses a target that stepped away, Jad's claw follows his magic, hazards arm and hit once a game tick, the attack count is
 * the OSRS rate, a mixed-style monster doesn't stutter, and a chasing monster walks at a player's pace.
 *
 * <p>Time is driven as the boss fights do: {@link ActionManager#advanceForSelfTest} plus {@link Mechanics#tickForSelfTest} every
 * Minecraft tick (and the monster's own tick where its AI matters). Hits are counted through the damage hooks and the pending-hit
 * recorder, never through health: the self-test player can't be hurt.
 */
final class NpcDeliveryChecks {
    private final TestContext c;
    /** Monsters whose own tick (AI, movement, attacks) runs every Minecraft tick of {@link #step}. */
    private final List<Mob> ticking = new ArrayList<>();
    private final List<BossEntity> telegraphing = new ArrayList<>();

    private record Hit(long mc, LivingEntity attacker, String attack, AttackType style, int damage) {
    }

    /** What the checked player went through during one check. */
    private static final class Rec {
        final ServerPlayer player;
        final List<Hit> hits = new ArrayList<>();
        final List<Mechanics.Threat> created = new ArrayList<>();
        final Map<Long, Integer> cues = new HashMap<>();
        final Map<Long, Long> landedAt = new HashMap<>();
        final Map<Long, Long> fizzledAt = new HashMap<>();
        final List<String> warnings = new ArrayList<>();

        Rec(ServerPlayer player) {
            this.player = player;
        }

        List<Hit> from(LivingEntity attacker) {
            List<Hit> out = new ArrayList<>();
            for (Hit h : hits) {
                if (h.attacker() == attacker) {
                    out.add(h);
                }
            }
            return out;
        }

        List<Hit> named(String attack) {
            List<Hit> out = new ArrayList<>();
            for (Hit h : hits) {
                if (attack.equals(h.attack())) {
                    out.add(h);
                }
            }
            return out;
        }

        Mechanics.Threat last() {
            return created.isEmpty() ? null : created.get(created.size() - 1);
        }
    }

    private static Rec rec;
    private static boolean hooked;

    private NpcDeliveryChecks(TestContext c) {
        this.c = c;
    }

    static List<SelfTest.Check> all(TestContext context) {
        NpcDeliveryChecks n = new NpcDeliveryChecks(context);
        List<SelfTest.Check> checks = new ArrayList<>();
        checks.add(new SelfTest.Check("npc:no_knockback", () -> n.run(n::knockback)));
        checks.add(new SelfTest.Check("npc:projectile_flight", () -> n.run(n::projectile)));
        checks.add(new SelfTest.Check("npc:telegraph_aoe", () -> n.run(n::telegraphAoe)));
        checks.add(new SelfTest.Check("npc:melee_windup", () -> n.run(n::meleeWindup)));
        checks.add(new SelfTest.Check("npc:jad_claw_after_magic", () -> n.run(n::jad)));
        checks.add(new SelfTest.Check("npc:cadence", () -> n.run(n::cadence)));
        checks.add(new SelfTest.Check("npc:hazard", () -> n.run(n::hazard)));
        checks.add(new SelfTest.Check("npc:mixed_style_navigation", () -> n.run(n::mixedNavigation)));
        checks.add(new SelfTest.Check("npc:chase_speed", () -> n.run(n::chaseSpeed)));
        checks.add(new SelfTest.Check("npc:style_spacing", () -> n.run(n::styleSpacing)));
        checks.add(new SelfTest.Check("npc:landing_sees_its_attack", () -> n.run(n::landingSeesItsAttack)));
        // with the real player too (the client walkthrough runs boss:*): a projectile prayed against through the real toggle
        // a few ticks after its cue, and a telegraph's floor, cues and warning line
        checks.add(new SelfTest.Check("boss:playability_npc_delivery", () -> n.run(n::playability)));
        return checks;
    }

    // ------------------------------------------------------------------ harness

    private interface Body {
        String run() throws Exception;
    }

    private String run(Body body) throws Exception {
        hook();
        settle(c.player); // what earlier checks left on its way (or just landed) must not space this check's hits
        Rec r = new Rec(c.player);
        Mechanics.PendingListener before = Mechanics.pendingListener;
        java.util.function.Function<net.minecraft.server.level.ServerLevel, List<ServerPlayer>> extraBefore = Mechanics.extraPlayers;
        ThreatCues.WarningListener warnBefore = ThreatCues.warningListener;
        Mechanics.pendingListener = new Mechanics.PendingListener() {
            @Override
            public void created(Mechanics.Threat t) {
                before.created(t);
                r.created.add(t);
            }

            @Override
            public void cue(Mechanics.Threat t, long mc) {
                before.cue(t, mc);
                r.cues.merge(t.id(), 1, Integer::sum);
            }

            @Override
            public void resolved(Mechanics.Threat t, long mc, boolean landed) {
                before.resolved(t, mc, landed);
                (landed ? r.landedAt : r.fizzledAt).put(t.id(), mc);
            }
        };
        ServerPlayer p = c.player;
        // the fake player isn't in the level's player list: let the area tests (AoE, hazards) see it
        Mechanics.extraPlayers = w -> w == p.level() ? List.of(p) : List.of();
        ThreatCues.warningListener = (player, text, subtitle) -> {
            warnBefore.onWarning(player, text, subtitle);
            if (player == p) {
                r.warnings.add((subtitle ? "subtitle: " : "action bar: ") + text);
            }
        };
        CombatZones.forget(p);
        rec = r;
        try {
            return body.run();
        } finally {
            rec = null;
            Mechanics.pendingListener = before;
            Mechanics.extraPlayers = extraBefore;
            ThreatCues.warningListener = warnBefore;
            for (Mob m : ticking) {
                m.discard();
            }
            for (BossEntity b : telegraphing) {
                b.discard();
            }
            ticking.clear();
            telegraphing.clear();
            clearStill();
            Mechanics.clearHazards(c.world, new net.minecraft.world.phys.AABB(TestContext.PLATFORM).inflate(40));
            settle(p);
            p.setDeltaMovement(Vec3.ZERO);
            c.home();
        }
    }

    /**
     * Lands or drops whatever is still on its way to {@code p}, then lets the style spacing forget the hits that just landed (it
     * keeps another style 20 MC from them), so one check's hits never move the next one's.
     */
    private static void settle(ServerPlayer p) {
        for (int i = 0; i < 80 && Mechanics.pendingOn(p) > 0; i++) {
            ActionManager.advanceForSelfTest();
            Mechanics.tickForSelfTest();
        }
        for (int i = 0; i < ThreatTiming.STYLE_SPACING_MC; i++) {
            ActionManager.advanceForSelfTest();
            Mechanics.tickForSelfTest();
        }
    }

    private static void hook() {
        if (hooked) {
            return;
        }
        hooked = true;
        CombatHooks.addDamageListener((player, attacker, attack, style, damage) -> {
            Rec r = rec;
            if (r != null && player == r.player) {
                r.hits.add(new Hit(ActionManager.mcTick(), attacker, attack, style, damage));
            }
        });
    }

    /** One Minecraft tick of the world as the checks see it: the clock, the ticking monsters, the pending hits and hazards. */
    private void step(int mc) {
        for (int i = 0; i < mc; i++) {
            ActionManager.advanceForSelfTest();
            for (Mob m : ticking) {
                if (m.isAlive()) {
                    if (m.getTarget() != c.player) {
                        m.setTarget(c.player);
                    }
                    m.tickCount++; // as ServerLevel.tickNonPassenger does (see BossFightChecks)
                    m.tick();
                }
            }
            for (BossEntity b : telegraphing) {
                b.tickTelegraphForSelfTest();
            }
            Mechanics.tickForSelfTest();
            c.heal();
        }
    }

    private static long now() {
        return ActionManager.mcTick();
    }

    private LivingEntity spawn(EntityType<?> type, double dx, double dz) {
        Entity e = type.create(c.world);
        if (!(e instanceof LivingEntity living)) {
            throw new AssertionError(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type) + " is not a living entity");
        }
        BlockPos base = TestContext.PLATFORM;
        living.moveTo(base.getX() + 0.5 + dx, base.getY(), base.getZ() + 0.5 + dz, 0f, 0f);
        if (living instanceof Mob mob) {
            mob.finalizeSpawn(c.world, c.world.getCurrentDifficultyAt(base), MobSpawnType.SPAWNER, null);
            mob.setPersistenceRequired();
        }
        c.world.addFreshEntity(living);
        if (living instanceof Mob mob) {
            ticking.add(mob); // discarded after the check (only ticked when a check asks for it)
            mob.getNavigation().stop();
        }
        return living;
    }

    private OsrsMonsterEntity spawn(String id, double dx, double dz) {
        EntityType<?> type = ModEntities.typeById(id);
        if (type == null) {
            throw new AssertionError("no entity type " + id);
        }
        LivingEntity e = spawn(type, dx, dz);
        if (!(e instanceof OsrsMonsterEntity m)) {
            throw new AssertionError(id + " is not an OSRS monster");
        }
        return m;
    }

    /** Spawns a monster that only acts when a check calls it (its AI isn't ticked). */
    private OsrsMonsterEntity still(String id, double dx, double dz) {
        OsrsMonsterEntity m = spawn(id, dx, dz);
        ticking.remove(m);
        stillMobs.add(m);
        return m;
    }

    private final List<Mob> stillMobs = new ArrayList<>();

    private void clearStill() {
        for (Mob m : stillMobs) {
            m.discard();
        }
        stillMobs.clear();
    }

    private void moveTo(double dx, double dz) {
        BlockPos base = TestContext.PLATFORM;
        c.player.teleportTo(c.world, base.getX() + 0.5 + dx, base.getY(), base.getZ() + 0.5 + dz, c.player.getYRot(), c.player.getXRot());
    }

    private static void expect(boolean ok, String failure) {
        if (!ok) {
            throw new AssertionError(failure);
        }
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static double horizontal(Vec3 v) {
        return Math.sqrt(v.x * v.x + v.z * v.z);
    }

    private void setLevel(Skill skill, int level) {
        Players.data(c.player).skills().setXpTenths(skill, Experience.xpForLevel(level) * 10);
    }

    /** A module monster with exactly one plain (untelegraphed, no special) attack of {@code style}'s group. */
    private static MonsterDef plainMonster(boolean melee, AttackType style, double minRange) {
        for (MonsterDef m : ContentRegistry.monsters()) {
            if (m.attacks.size() != 1 || m.width * m.scale > 1.2 || ModEntities.typeById(m.id) == null) {
                continue;
            }
            AttackDef a = m.attacks.get(0);
            boolean kind = melee ? a.melee() : style == null ? !a.melee() : a.style() == style;
            if (kind && a.special() == AttackDef.Special.NONE && a.telegraphTicks() == 0 && a.maxHit() > 0 && a.range() >= minRange) {
                return m;
            }
        }
        throw new SelfTest.Skip("no module monster with a single plain " + (melee ? "melee" : String.valueOf(style)) + " attack");
    }

    // ------------------------------------------------------------------ the checks

    /** OSRS hits never push; the KNOCKBACK special still does. */
    private String knockback() {
        OsrsMonsterEntity giant = still(Monster.HILL_GIANT.entity(), 3, 0);
        OsrsMonsterEntity goblin = still(Monster.GOBLIN.entity(), -3, 0);
        giant.setDeltaMovement(Vec3.ZERO);
        int dealt = CombatHandler.applyDamage(c.player, giant, 1);
        expect(dealt == 1, "the player's 1-damage hit on the hill giant dealt " + dealt + " (nothing to measure)");
        double byPlayer = horizontal(giant.getDeltaMovement());
        expect(byPlayer < 0.01, "an OSRS hit from the player pushed the hill giant " + fmt(byPlayer));
        giant.setDeltaMovement(Vec3.ZERO);
        CombatHandler.applyDamage(goblin, giant, 1);
        double byMonster = horizontal(giant.getDeltaMovement());
        expect(byMonster < 0.01, "an OSRS hit from a monster pushed the hill giant " + fmt(byMonster));
        // control: a vanilla hurt from the same monster still pushes, so the measurement can see a push
        giant.setDeltaMovement(Vec3.ZERO);
        giant.invulnerableTime = 0;
        giant.hurt(c.world.damageSources().mobAttack(goblin), 1f);
        double vanilla = horizontal(giant.getDeltaMovement()); // reported only: another module's damage gate may refuse it
        // the KNOCKBACK special, through Mechanics.perform and its wind-up
        AttackDef shove = AttackDef.of("selftest_knockback", AttackType.CRUSH, 0, 3, 1).special(AttackDef.Special.KNOCKBACK, 2);
        c.player.setDeltaMovement(Vec3.ZERO);
        Mechanics.perform(giant, c.player, shove, 1.0);
        Mechanics.Threat t = rec.last();
        expect(t != null && t.kind() == Mechanics.Kind.WINDUP, "the knockback swing has no wind-up");
        for (int i = 0; i < 20 && !rec.landedAt.containsKey(t.id()) && !rec.fizzledAt.containsKey(t.id()); i++) {
            step(1);
        }
        expect(rec.landedAt.containsKey(t.id()), "the knockback swing never landed (target in reach)");
        double pushed = horizontal(c.player.getDeltaMovement());
        expect(pushed > 0.3, "the KNOCKBACK special moved the player only " + fmt(pushed));
        clearStill();
        return "OSRS hits push " + fmt(byPlayer) + " / " + fmt(byMonster) + " (vanilla " + fmt(vanilla) + "), the KNOCKBACK special "
                + fmt(pushed);
    }

    /** A projectile lands once, at its due tick, never on the launch tick; a wall built mid-flight stops it. */
    private String projectile() {
        MonsterDef def = plainMonster(false, null, 8);
        OsrsMonsterEntity mob = still(def.id, 10, 0);
        AttackDef a = def.attacks.get(0);
        long t0 = now();
        Mechanics.perform(mob, c.player, a, 1.0);
        expect(rec.from(mob).isEmpty(), "the " + def.id + "'s " + a.name() + " hit on its launch tick");
        Mechanics.Threat t = rec.last();
        expect(t != null && t.kind() == Mechanics.Kind.PROJECTILE, "the " + a.style() + " attack is not a projectile");
        expect(t.firstCueMc() == t0, "the projectile was first drawn at " + (t.firstCueMc() - t0) + " MC, not at launch");
        long flight = t.dueMc() - t0;
        expect(flight >= ThreatTiming.FLIGHT_MIN_MC && flight <= ThreatTiming.FLIGHT_MAX_MC, "a 10-block flight of " + flight + " MC");
        step((int) flight + 3);
        List<Hit> hits = rec.from(mob);
        expect(hits.size() == 1, hits.size() + " resolutions of one projectile");
        expect(hits.get(0).mc() == t.dueMc(), "it resolved at " + (hits.get(0).mc() - t0) + " MC, not at its due " + flight);
        // a stone wall built between them after launch: the projectile fizzles on it
        Mechanics.perform(mob, c.player, a, 1.0);
        Mechanics.Threat walled = rec.last();
        step(3);
        for (int dz = -2; dz <= 2; dz++) {
            for (int dy = 0; dy <= 4; dy++) {
                c.place(TestContext.PLATFORM.offset(5, dy, dz), Blocks.STONE.defaultBlockState());
            }
        }
        step((int) (walled.dueMc() - now()) + 3);
        expect(rec.from(mob).size() == 1, "a projectile went through a stone wall built in its way");
        expect(rec.fizzledAt.containsKey(walled.id()), "the walled-off projectile didn't fizzle");
        clearStill();
        return def.id + " " + a.style() + ": launched, resolved once at " + flight + " MC; walled off mid-flight: fizzled";
    }

    /** A telegraph(1) AoE: 36 MC lead (not 12), cues redrawn, dodged by stepping out of its true radius at MC 30. */
    private String telegraphAoe() {
        OsrsMonsterEntity mob = still(Monster.HILL_GIANT.entity(), 2, 0);
        AttackDef aoe = AttackDef.of("selftest_aoe", AttackType.MAGIC, 20, 8, 1).special(AttackDef.Special.AOE, 3)
                .telegraph(1, "The hill giant gathers a blast!");
        long t0 = now();
        Mechanics.perform(mob, c.player, aoe, 1.0);
        Mechanics.Threat t = rec.last();
        expect(t != null && t.kind() == Mechanics.Kind.TELEGRAPH, "a telegraph(1) attack is not telegraphed");
        expect(t.dueMc() - t0 == ThreatTiming.TELEGRAPH_FLOOR_MC, "a telegraph(1) lead of " + (t.dueMc() - t0) + " MC (floor 36)");
        step(ThreatTiming.TELEGRAPH_FLOOR_MC - 1);
        expect(rec.from(mob).isEmpty(), "the AoE hit before its 36 MC lead");
        step(1);
        List<Hit> stood = rec.from(mob);
        expect(stood.size() == 1 && stood.get(0).mc() == t0 + 36, "standing still in its ring: " + stood.size() + " hit(s)"
                + (stood.isEmpty() ? "" : " at " + (stood.get(0).mc() - t0) + " MC"));
        int cues = rec.cues.getOrDefault(t.id(), 0);
        expect(cues >= 9, "only " + cues + " cue draws over a 36 MC telegraph (every 4 MC from creation: 9)");
        // the same attack, stepped out of at MC 30 to 3.5 blocks (its radius is 3)
        long t1 = now();
        Mechanics.perform(mob, c.player, aoe, 1.0);
        Mechanics.Threat dodge = rec.last();
        step(30);
        moveTo(mob.getX() - TestContext.PLATFORM.getX() - 0.5 - 3.5, 0);
        step((int) (dodge.dueMc() - now()) + 2);
        expect(rec.from(mob).size() == 1, "stepping 3.5 blocks out of a radius-3 AoE at MC 30 was still hit");
        return "lead 36 MC (" + cues + " cue draws), hit at MC " + (stood.get(0).mc() - t0) + " standing still, dodged at MC 30 (t "
                + (now() - t1) + ")";
    }

    /** A melee swing misses a target that stepped away during the wind-up; a monster's first hit lands 10+ MC after it arrives. */
    private String meleeWindup() {
        OsrsMonsterEntity goblin = still(Monster.GOBLIN.entity(), 1, 0);
        goblin.performAttack(c.player, AttackType.CRUSH);
        Mechanics.Threat t = rec.last();
        expect(t != null && t.kind() == Mechanics.Kind.WINDUP, "the goblin's swing has no wind-up");
        expect(rec.from(goblin).isEmpty(), "the goblin hit on the tick it swung");
        step(2);
        moveTo(-4, 0); // 5 blocks from the goblin
        step(ThreatTiming.meleeWindupMc(goblin.getBbWidth()) + 2);
        expect(rec.from(goblin).isEmpty(), "the swing hit a player who stepped 5 blocks away during its wind-up");
        expect(rec.fizzledAt.containsKey(t.id()), "the missed swing didn't resolve as a miss");
        clearStill();
        c.home();
        // arrival: a goblin walks up from 6 blocks away and attacks through its goal
        CombatZones.forget(c.player);
        OsrsMonsterEntity walker = spawn(Monster.GOBLIN.entity(), -6, 0);
        long arrived = -1;
        long first = -1;
        for (int i = 0; i < 400 && first < 0; i++) {
            step(1);
            double gap = ThreatTiming.gap(c.player.getX() - walker.getX(), c.player.getZ() - walker.getZ(), walker.getBbWidth(),
                    c.player.getBbWidth());
            if (arrived < 0 && ThreatTiming.inMeleeReach(gap, c.player.getY() - walker.getY())) {
                arrived = now();
            }
            if (!rec.from(walker).isEmpty()) {
                first = rec.from(walker).get(0).mc();
            }
        }
        expect(arrived >= 0, "the goblin never came into melee reach (at " + fmt(walker.distanceTo(c.player)) + " blocks)");
        expect(first >= 0, "the goblin came into reach but never hit");
        expect(first - arrived >= ThreatTiming.ENGAGE_MIN_MC, "the goblin's first hit landed " + (first - arrived)
                + " MC after it arrived (at least 10)");
        return "a swing whooshes past a player who stepped away; walking up, the first hit landed " + (first - arrived)
                + " MC after arriving";
    }

    /** Jad's magic, then his claw 8 game ticks after launch: both land (the double cooldown dropped the claw). */
    private String jad() {
        EntityType<?> type = ModEntities.BOSS_TYPES.get(Boss.TZTOK_JAD);
        if (type == null) {
            throw new SelfTest.Skip("no TzTok-Jad entity type");
        }
        BossEntity jad = (BossEntity) spawn(type, 5, 0);
        ticking.remove(jad);
        telegraphing.add(jad);
        long t0 = now();
        jad.performAttack(c.player, AttackType.MAGIC);
        expect(rec.from(jad).isEmpty(), "Jad's magic hit on the tick he reared up");
        step(8 * ActionManager.MC_TICKS_PER_GAME_TICK);
        List<Hit> magic = rec.from(jad);
        expect(magic.size() == 1 && magic.get(0).style() == AttackType.MAGIC, "Jad's magic resolved " + magic.size() + " time(s)");
        expect(magic.get(0).mc() - t0 >= ThreatTiming.TELEGRAPH_FLOOR_MC, "Jad's magic landed " + (magic.get(0).mc() - t0)
                + " MC after he reared up");
        // the player steps up beside him; his claw is the next attack, 8 ticks after the magic launched
        moveTo(jad.getX() - TestContext.PLATFORM.getX() - 0.5 - (jad.getBbWidth() / 2 + c.player.getBbWidth() / 2 + 0.4), 0);
        jad.performAttack(c.player, AttackType.STAB);
        step(ThreatTiming.meleeWindupMc(jad.getBbWidth()) + 2);
        List<Hit> both = rec.from(jad);
        expect(both.size() == 2, "Jad's claw 8 ticks after his magic resolved " + (both.size() - 1) + " time(s) (dropped by a cooldown?)");
        expect(both.get(1).style() != null && both.get(1).style().isMelee(), "Jad's second hit was " + both.get(1).style());
        return "magic at " + (magic.get(0).mc() - t0) + " MC, claw at " + (both.get(1).mc() - t0) + " MC: both resolved";
    }

    /** A monster against a stationary player over 100 game ticks: the OSRS attack count, and every attack started lands. */
    private String cadence() {
        MonsterDef def = plainMonster(true, null, 1.5);
        OsrsMonsterEntity mob = spawn(def.id, 1.2, 0);
        mob.getRandom().setSeed(41L);
        int speed = mob.stats().attackSpeed();
        List<Long> launches = new ArrayList<>();
        Mechanics.AttackListener before = Mechanics.attackListener;
        Mechanics.attackListener = (m, target, attack) -> {
            before.onAttack(m, target, attack);
            if (m == mob) {
                launches.add(now());
            }
        };
        Set<Long> window = new LinkedHashSet<>();
        long start = -1;
        try {
            for (int i = 0; i < 1600; i++) {
                int createdBefore = rec.created.size();
                step(1);
                if (start < 0 && !launches.isEmpty()) {
                    start = launches.get(0);
                }
                if (start >= 0 && now() <= start + 1200) {
                    for (int k = createdBefore; k < rec.created.size(); k++) {
                        window.add(rec.created.get(k).id());
                    }
                }
                if (start >= 0 && now() > start + 1200 + ThreatTiming.FLIGHT_MAX_MC + ThreatTiming.MAX_SPACING_PUSH_MC) {
                    break;
                }
            }
        } finally {
            Mechanics.attackListener = before;
        }
        expect(start >= 0, "the " + def.id + " never attacked (" + fmt(mob.distanceTo(c.player)) + " blocks away)");
        long started = launches.stream().filter(l -> l <= launchesStart(launches) + 1200).count();
        int expected = 1200 / (speed * ActionManager.MC_TICKS_PER_GAME_TICK) + 1;
        expect(started == expected, "the " + def.id + " (speed " + speed + ") started " + started + " attacks in 100 game ticks, OSRS "
                + expected);
        int landed = 0;
        for (long id : window) {
            if (rec.landedAt.containsKey(id)) {
                landed++;
            }
        }
        expect(window.size() == started, window.size() + " hits delivered for " + started + " attacks");
        expect(landed == started, "only " + landed + " of the " + started + " attacks landed on a stationary player");
        return def.id + " speed " + speed + ": " + started + " attacks in 100 game ticks (OSRS " + expected + "), all " + landed
                + " landed";
    }

    private static long launchesStart(List<Long> launches) {
        return launches.get(0);
    }

    /** A hazard arms after 12 MC, then hits a player standing in it once per 12 MC; nobody beside it. */
    private String hazard() {
        // standing in it from its placement to 24 MC: hits at 12 and 24
        Vec3 at = c.player.position();
        Mechanics.addHazard(c.world, at, 5, 20, null);
        step(24);
        int stood = rec.named("hazard").size();
        expect(stood == 2, "standing in a hazard from placement to 24 MC: " + stood + " hits (expected 2: armed at 12, again at 24)");
        Mechanics.clearHazards(c.world, new net.minecraft.world.phys.AABB(TestContext.PLATFORM).inflate(40));
        rec.hits.clear();
        // crossing an armed one in 4 MC: one hit
        Mechanics.addHazard(c.world, at.add(3, 0, 0), 5, 20, null);
        step(ThreatTiming.HAZARD_ARM_MC);
        moveTo(3, 0);
        step(4);
        c.home();
        step(20);
        int crossed = rec.named("hazard").size();
        expect(crossed == 1, "crossing an armed hazard in 4 MC: " + crossed + " hits (expected 1)");
        Mechanics.clearHazards(c.world, new net.minecraft.world.phys.AABB(TestContext.PLATFORM).inflate(40));
        rec.hits.clear();
        // stepping off before it arms: none
        Mechanics.addHazard(c.world, at, 5, 20, null);
        step(ThreatTiming.HAZARD_ARM_MC - 1);
        moveTo(3, 0);
        step(20);
        int early = rec.named("hazard").size();
        expect(early == 0, "leaving a hazard before it armed: " + early + " hits");
        Mechanics.clearHazards(c.world, new net.minecraft.world.phys.AABB(TestContext.PLATFORM).inflate(40));
        c.home();
        rec.hits.clear();
        // 1.2 blocks beside it: none
        Mechanics.addHazard(c.world, at.add(1.2, 0, 0), 5, 20, null);
        step(36);
        int beside = rec.named("hazard").size();
        expect(beside == 0, "standing 1.2 blocks beside a hazard: " + beside + " hits");
        return "stood 24 MC: 2 hits; crossed in 4 MC: 1; left before arming: 0; 1.2 blocks beside: 0";
    }

    /** A mixed melee/magic monster at mid range doesn't stop and start walking more than once per attack. */
    private String mixedNavigation() {
        MonsterDef def = null;
        AttackDef melee = null;
        for (MonsterDef m : ContentRegistry.monsters()) {
            if (m.attacks.size() != 2 || m.width * m.scale > 1.2 || ModEntities.typeById(m.id) == null) {
                continue;
            }
            AttackDef mel = null;
            AttackDef mag = null;
            for (AttackDef a : m.attacks) {
                if (a.special() != AttackDef.Special.NONE || a.telegraphTicks() > 0) {
                    continue;
                }
                if (a.melee()) {
                    mel = a;
                } else if (a.style() == AttackType.MAGIC) {
                    mag = a;
                }
            }
            if (mel != null && mag != null && mel.range() > ThreatTiming.MELEE_REACH + 0.3 && mag.range() > mel.range()) {
                def = m;
                melee = mel;
                break;
            }
        }
        if (def == null) {
            throw new SelfTest.Skip("no module monster with one plain melee and one plain magic attack");
        }
        // both attacks usable, but outside the goal's melee reach: the old per-tick re-roll walked and stopped every tick
        double gap = (ThreatTiming.MELEE_REACH + Math.min(melee.range(), 3.0)) / 2;
        float w = def.width * def.scale;
        OsrsMonsterEntity mob = spawn(def.id, -(gap + w / 2 + c.player.getBbWidth() / 2), 0);
        mob.getRandom().setSeed(7L);
        int[] launched = {0};
        Mechanics.AttackListener before = Mechanics.attackListener;
        Mechanics.attackListener = (m, target, attack) -> {
            before.onAttack(m, target, attack);
            if (m == mob) {
                launched[0]++;
            }
        };
        int transitions = 0;
        int flips = 0;
        try {
            boolean moving = mob.getNavigation().isInProgress();
            AttackType style = null;
            int cycles = 8;
            for (int i = 0; i < cycles * mob.stats().attackSpeed() * ActionManager.MC_TICKS_PER_GAME_TICK + 20; i++) {
                step(1);
                boolean now = mob.getNavigation().isInProgress();
                if (now != moving) {
                    transitions++;
                    moving = now;
                }
                // what the goal will follow next tick (the kept choice: asking again doesn't re-roll it)
                AttackType next = mob.chooseStyle(c.player, mob.distanceToSqr(c.player));
                if (style != null && ThreatTiming.prayerGroup(next) != ThreatTiming.prayerGroup(style)) {
                    flips++;
                }
                style = next;
            }
        } finally {
            Mechanics.attackListener = before;
        }
        expect(launched[0] > 0, "the " + def.id + " never attacked");
        expect(flips <= launched[0] + 2, "the " + def.id + "'s next style changed " + flips + " times for " + launched[0] + " attacks");
        expect(transitions <= 3 * launched[0] + 4, "the " + def.id + " started/stopped walking " + transitions + " times for "
                + launched[0] + " attacks");
        return def.id + ": " + launched[0] + " attacks, " + flips + " style changes, " + transitions + " walk/stop changes";
    }

    /** A monster with the default speed chases at 3.8-4.6 blocks a second (a walking player's pace; OSRS: a tile a tick each). */
    private String chaseSpeed() {
        moveTo(8, 0);
        OsrsMonsterEntity goblin = spawn(Monster.GOBLIN.entity(), -10, 0);
        double speedAttr = goblin.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        expect(Math.abs(speedAttr - ModEntities.DEFAULT_MOVE_SPEED) < 1e-6, "the goblin's speed attribute is " + speedAttr);
        double startX = 0;
        long startMc = -1;
        double endX = 0;
        long endMc = -1;
        for (int i = 0; i < 200; i++) {
            step(1);
            moveTo(8, 0);
            double gap = ThreatTiming.gap(c.player.getX() - goblin.getX(), c.player.getZ() - goblin.getZ(), goblin.getBbWidth(),
                    c.player.getBbWidth());
            if (i == 20) {
                startX = goblin.getX();
                startMc = now();
            }
            if (startMc >= 0 && gap > 3) {
                endX = goblin.getX();
                endMc = now();
            }
            if (gap <= 3) {
                break;
            }
        }
        expect(startMc >= 0 && endMc > startMc + 10, "the goblin didn't chase long enough to measure (" + fmt(goblin.getX() - startX)
                + " blocks)");
        double perSecond = (endX - startX) / (endMc - startMc) * 20;
        expect(perSecond >= 3.8 && perSecond <= 4.6, "a chasing goblin walks " + fmt(perSecond) + " blocks a second (3.8-4.6)");
        return "chases at " + fmt(perSecond) + " blocks a second";
    }

    /**
     * A melee swing started 12 MC after a magic projectile lands at least 20 MC from it (one prayer switch at a time), whether the
     * bolt is still flying (a boss's: 16+ MC) or already landed (a monster's over 2 blocks: 10 MC) when the swing starts.
     */
    private String styleSpacing() {
        OsrsMonsterEntity mob = still(Monster.HILL_GIANT.entity(), 2, 0);
        AttackDef bolt = AttackDef.of("selftest_spacing_bolt", AttackType.MAGIC, 5, 12, 1);
        AttackDef swing = AttackDef.of("selftest_spacing_swing", AttackType.CRUSH, 5, 3, 1);
        StringBuilder out = new StringBuilder();
        for (boolean flying : new boolean[]{true, false}) {
            long t0 = now();
            if (flying) {
                Mechanics.deliverProjectile(mob, c.player, AttackType.MAGIC, true, 12, () -> {
                });
            } else {
                Mechanics.perform(mob, c.player, bolt, 1.0);
            }
            Mechanics.Threat magic = rec.last();
            expect(magic != null && magic.kind() == Mechanics.Kind.PROJECTILE && magic.style() == AttackType.MAGIC,
                    "the bolt is not a magic projectile");
            expect(flying == magic.dueMc() > t0 + ActionManager.MC_TICKS_PER_GAME_TICK, (flying ? "the boss's" : "the monster's")
                    + " bolt flies " + (magic.dueMc() - t0) + " MC (the setup needs it " + (flying ? "still flying" : "landed")
                    + " when the swing starts 12 MC later)");
            step(ActionManager.MC_TICKS_PER_GAME_TICK);
            Mechanics.perform(mob, c.player, swing, 1.0);
            Mechanics.Threat melee = rec.last();
            expect(melee != null && melee.kind() == Mechanics.Kind.WINDUP, "the swing has no wind-up");
            step((int) (Math.max(magic.dueMc(), melee.dueMc()) - now()) + 2);
            expect(rec.landedAt.containsKey(magic.id()), "the bolt never landed");
            expect(rec.landedAt.containsKey(melee.id()), "the swing never landed on a player standing in reach");
            long apart = rec.landedAt.get(melee.id()) - rec.landedAt.get(magic.id());
            expect(apart >= ThreatTiming.STYLE_SPACING_MC, "a swing started 12 MC after a " + (flying ? "flying" : "landed")
                    + " magic bolt landed " + apart + " MC after it (at least " + ThreatTiming.STYLE_SPACING_MC + ")");
            out.append(out.length() == 0 ? "" : "; ").append(flying ? "bolt in flight" : "bolt landed").append(": swing ").append(apart)
                    .append(" MC after it");
            settle(c.player);
        }
        clearStill();
        return out.toString();
    }

    /**
     * A hit still on its way when its boss starts another attack is judged as its own attack (a script's on-hit hook reads the
     * attack that landed): the Whisperer's soul bolt, prayed against, deals nothing although her melee, which hits through prayer,
     * started before the bolt arrived.
     */
    private String landingSeesItsAttack() {
        com.runecraft.osrs.content.BossDef def = ContentRegistry.boss("the_whisperer");
        if (def == null) {
            throw new SelfTest.Skip("no BossDef the_whisperer");
        }
        com.runecraft.boss.DataBossEntity boss = com.runecraft.boss.DataBossService.spawn(c.world, c.near(3, 0), def, c.player);
        expect(boss != null, "the Whisperer didn't spawn");
        stillMobs.add(boss); // discarded after the check; her own tick never runs
        AttackDef bolt = com.runecraft.osrs.content.bosses.Dt2Bosses.WHISPER_MAGIC;
        AttackDef whack = com.runecraft.osrs.content.bosses.Dt2Bosses.WHISPER_MELEE;
        PlayerData data = Players.data(c.player);
        if (!data.isActive(Prayer.PROTECT_FROM_MAGIC)) {
            PrayerService.toggle(c.player, Prayer.PROTECT_FROM_MAGIC);
        }
        expect(data.isActive(Prayer.PROTECT_FROM_MAGIC), "Protect from Magic didn't come on through the toggle");
        int bolts = 0;
        int hurt = 0;
        int whacks = 0;
        try {
            for (int i = 0; i < 6; i++) {
                expect(com.runecraft.content.bosses.BossSelfTests.performScripted(boss, c.player, bolt),
                        "the Whisperer's script is not a scripted boss");
                Mechanics.Threat flying = rec.last();
                expect(flying != null && flying.kind() == Mechanics.Kind.PROJECTILE, "her soul bolt is not a projectile");
                step(ActionManager.MC_TICKS_PER_GAME_TICK);
                expect(flying.dueMc() > now(), "her soul bolt landed within a game tick (nothing to check)");
                com.runecraft.content.bosses.BossSelfTests.performScripted(boss, c.player, whack); // her script's last attack now
                Mechanics.Threat swing = rec.last();
                step((int) (Math.max(flying.dueMc(), swing.dueMc()) - now()) + 2);
                expect(rec.landedAt.containsKey(flying.id()), "her soul bolt never landed");
                for (Hit h : rec.named(bolt.name())) {
                    if (h.mc() == flying.dueMc()) {
                        bolts++;
                        if (h.damage() > 0) {
                            hurt++;
                        }
                    }
                }
                whacks += rec.named(whack.name()).size();
                rec.hits.clear();
                settle(c.player);
            }
        } finally {
            if (data.isActive(Prayer.PROTECT_FROM_MAGIC)) {
                PrayerService.toggle(c.player, Prayer.PROTECT_FROM_MAGIC);
            }
        }
        expect(bolts == 6, bolts + " of 6 soul bolts resolved at their due tick");
        expect(hurt == 0, hurt + " of 6 soul bolts hurt through Protect from Magic: judged as the melee she started while they flew");
        clearStill();
        return "6 soul bolts landing after her melee started: all blocked by Protect from Magic (" + whacks + " whacks landed)";
    }

    /**
     * With the real player too: a magic projectile, prayed against through the real toggle 6 MC after its first cue, is blocked at
     * impact; a telegraph keeps its 36 MC floor with cues redrawn and its warning line sent to the target.
     */
    private String playability() {
        setLevel(Skill.DEFENCE, 1); // a weak defender, so the unprayed bolt can be seen to hurt
        setLevel(Skill.MAGIC, 1);
        // the strongest plain magic caster among the module monsters, so its bolts usually hit an unprayed defender
        MonsterDef mage = null;
        for (MonsterDef m : ContentRegistry.monsters()) {
            if (ModEntities.typeById(m.id) == null || m.width * m.scale > 2.5) {
                continue;
            }
            for (AttackDef a : m.attacks) {
                if (a.style() == AttackType.MAGIC && (mage == null || m.magic + m.magicAttackBonus > mage.magic + mage.magicAttackBonus)) {
                    mage = m;
                }
            }
        }
        if (mage == null) {
            throw new SelfTest.Skip("no module monster with a magic attack");
        }
        OsrsMonsterEntity mob = still(mage.id, 6, 0);
        AttackDef bolt = AttackDef.of("selftest_bolt", AttackType.MAGIC, 30, 12, 1);
        PlayerData data = Players.data(c.player);
        int unprayedHurt = 0;
        for (int i = 0; i < 6; i++) {
            Mechanics.perform(mob, c.player, bolt, 1.0);
            Mechanics.Threat t = rec.last();
            expect(t.kind() == Mechanics.Kind.PROJECTILE && t.firstCueMc() == t.createdMc() && t.prayerCheckable(),
                    "the bolt is not a projectile drawn at launch");
            step((int) (t.dueMc() - now()) + 1);
            for (Hit h : rec.from(mob)) {
                if (h.mc() == t.dueMc() && h.damage() > 0) {
                    unprayedHurt++;
                }
            }
        }
        rec.hits.clear();
        int blocked = 0;
        long reaction = 0;
        for (int i = 0; i < 6; i++) {
            if (data.isActive(Prayer.PROTECT_FROM_MAGIC)) {
                PrayerService.toggle(c.player, Prayer.PROTECT_FROM_MAGIC); // off: switched on again after the cue
            }
            Mechanics.perform(mob, c.player, bolt, 1.0);
            Mechanics.Threat t = rec.last();
            step(6);
            PrayerService.toggle(c.player, Prayer.PROTECT_FROM_MAGIC);
            expect(data.isActive(Prayer.PROTECT_FROM_MAGIC), "Protect from Magic didn't come on through the toggle");
            reaction = t.dueMc() - t.firstCueMc();
            step((int) (t.dueMc() - now()) + 1);
            List<Hit> hits = rec.from(mob);
            expect(!hits.isEmpty() && hits.get(hits.size() - 1).mc() == t.dueMc(), "the prayed-against bolt didn't resolve at its due");
            if (hits.get(hits.size() - 1).damage() == 0) {
                blocked++;
            }
        }
        expect(unprayedHurt > 0, "none of 6 unprayed bolts from the " + mage.id + " hurt a Defence 1 player: the prayer can't be seen");
        expect(blocked == 6, "only " + blocked + " of 6 bolts were blocked by Protect from Magic switched on 6 MC after the cue");
        if (data.isActive(Prayer.PROTECT_FROM_MAGIC)) {
            PrayerService.toggle(c.player, Prayer.PROTECT_FROM_MAGIC);
        }
        // a telegraphed blast: 36 MC floor from its first cue, redrawn, its warning line sent to the target
        AttackDef blast = AttackDef.of("selftest_blast", AttackType.RANGED, 10, 12, 1).telegraph(1, "The hill giant hurls a boulder!");
        rec.warnings.clear();
        Mechanics.perform(mob, c.player, blast, 1.0);
        Mechanics.Threat tel = rec.last();
        expect(tel.kind() == Mechanics.Kind.TELEGRAPH && tel.dueMc() - tel.firstCueMc() >= ThreatTiming.TELEGRAPH_FLOOR_MC,
                "the telegraph's lead from its first cue is " + (tel.dueMc() - tel.firstCueMc()) + " MC");
        step((int) (tel.dueMc() - now()) + 1);
        int cues = rec.cues.getOrDefault(tel.id(), 0);
        expect(cues >= 9, "the telegraph was drawn " + cues + " times over its lead");
        expect(rec.landedAt.containsKey(tel.id()), "the telegraph never landed");
        expect(!rec.warnings.isEmpty(), "the telegraph's warning line never reached its target");
        clearStill();
        return mage.id + " bolts: " + unprayedHurt + "/6 hurt unprayed, 6/6 blocked by a prayer switched 6 MC after the cue (" + reaction
                + " MC to react); telegraph " + (tel.dueMc() - tel.firstCueMc()) + " MC, " + cues + " cue draws, " + rec.warnings.get(0)
                + (c.realPlayer() ? " (real player)" : "");
    }
}
