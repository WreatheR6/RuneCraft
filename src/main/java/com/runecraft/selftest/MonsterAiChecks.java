package com.runecraft.selftest;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.Mechanics;
import com.runecraft.combat.Aggression;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.CombatZones;
import com.runecraft.combat.KillCredit;
import com.runecraft.combat.LootDrops;
import com.runecraft.combat.MonsterStats;
import com.runecraft.entity.MonsterHome;
import com.runecraft.entity.OsrsAttackGoal;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.entity.OsrsTargetGoal;
import com.runecraft.entity.VanillaOsrsAi;
import com.runecraft.osrs.AttackClock;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Experience;
import com.runecraft.osrs.MonsterAi;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SingleCombat;
import com.runecraft.osrs.ThreatTiming;
import com.runecraft.osrs.data.Monster;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Round 42 open-world monster AI ({@code boss:playability_monster_*}, so the client walkthrough runs them with the real player
 * too): the vanilla stand-ins fight on OSRS AI with OSRS stats through the real-time delivery, aggression follows the combat-level
 * rule and tolerance, a monster leashes home and heals (but holds at the edge of its range, unhealed, while a player shoots it from
 * beyond), retaliation doesn't flip-flop between two attackers, a monster blocked by single-way combat drops its target and the
 * blocker is named, the top damager wins the kill with a private, unflung drop, and encounter spawns (God Wars bodyguards, a boss's
 * vanilla summons) keep their own rules.
 *
 * <p>Round 43 adds the second pass: bosses the wiki calls not aggressive leave a player alone until disturbed or attacked; the
 * passive vanilla stand-ins (cow, chicken, sheep, pig) fight back with their OSRS stats and hitpoints instead of panicking, the
 * chicken retreats at its last hitpoint and the zombie doesn't burn in daylight; a stray villager becomes a Man or Woman; a monster
 * keeps its damage at home and regenerates a hitpoint a minute; encounter monsters stay in their arena and attack any level; a
 * monster walled off from the player (in sight) lets the single-way lock go, and a held attack doesn't re-send the refusal line.
 *
 * <p>Time is driven as {@link NpcDeliveryChecks} does: {@link ActionManager#advanceForSelfTest}, the monsters' own ticks and
 * {@link Mechanics#tickForSelfTest} every Minecraft tick. Hits are counted through the damage hook and the pending-hit recorder,
 * never through health: the self-test player can't be hurt.
 */
final class MonsterAiChecks {
    private final TestContext c;
    /** Monsters whose own tick (AI, movement, attacks) runs every Minecraft tick of {@link #step}. */
    private final List<Mob> ticking = new ArrayList<>();
    /** Monsters spawned for a check that only act when the check makes them. */
    private final List<Mob> still = new ArrayList<>();

    private record Hit(long mc, LivingEntity attacker, String attack, AttackType style, int damage) {
    }

    private static List<Hit> hits;
    private static List<Mechanics.Threat> threats;
    private static List<ServerPlayer> kills;
    private static boolean hooked;

    private MonsterAiChecks(TestContext c) {
        this.c = c;
    }

    static List<SelfTest.Check> all(TestContext context) {
        MonsterAiChecks m = new MonsterAiChecks(context);
        List<SelfTest.Check> checks = new ArrayList<>();
        checks.add(new SelfTest.Check("boss:playability_monster_vanilla_osrs_ai", () -> m.run(m::vanillaOnOsrsAi)));
        checks.add(new SelfTest.Check("boss:playability_monster_aggression", () -> m.run(m::aggression)));
        checks.add(new SelfTest.Check("boss:playability_monster_leash", () -> m.run(m::leash)));
        checks.add(new SelfTest.Check("boss:playability_monster_leash_under_fire", () -> m.run(m::leashUnderFire)));
        checks.add(new SelfTest.Check("boss:playability_monster_encounter_spawns", () -> m.run(m::encounterSpawns)));
        checks.add(new SelfTest.Check("boss:playability_monster_retaliation", () -> m.run(m::retaliation)));
        checks.add(new SelfTest.Check("boss:playability_monster_single_way", () -> m.run(m::singleWay)));
        checks.add(new SelfTest.Check("boss:playability_monster_top_damager", () -> m.run(m::topDamager)));
        checks.add(new SelfTest.Check("boss:playability_monster_passive_bosses", () -> m.run(m::passiveBosses)));
        checks.add(new SelfTest.Check("boss:playability_monster_passive_animals", () -> m.run(m::passiveAnimals)));
        checks.add(new SelfTest.Check("boss:playability_monster_no_sunburn", () -> m.run(m::noSunburn)));
        checks.add(new SelfTest.Check("boss:playability_monster_villager", () -> m.run(m::villager)));
        checks.add(new SelfTest.Check("boss:playability_monster_regen", () -> m.run(m::regen)));
        checks.add(new SelfTest.Check("boss:playability_monster_encounter_arena", () -> m.run(m::encounterArena)));
        checks.add(new SelfTest.Check("boss:playability_monster_walled_off", () -> m.run(m::walledOff)));
        return checks;
    }

    // ------------------------------------------------------------------ harness

    private interface Body {
        String run() throws Exception;
    }

    private String run(Body body) throws Exception {
        hook();
        ServerPlayer p = c.player;
        ServerPlayer partner = com.runecraft.platform.FakePlayer.partner(c.world);
        settle(p);
        hits = new ArrayList<>();
        threats = new ArrayList<>();
        kills = new ArrayList<>();
        Mechanics.PendingListener before = Mechanics.pendingListener;
        java.util.function.Function<net.minecraft.server.level.ServerLevel, List<ServerPlayer>> huntBefore = OsrsTargetGoal.extraPlayers;
        java.util.function.Function<net.minecraft.server.level.ServerLevel, List<ServerPlayer>> areaBefore = Mechanics.extraPlayers;
        Mechanics.pendingListener = new Mechanics.PendingListener() {
            @Override
            public void created(Mechanics.Threat t) {
                before.created(t);
                threats.add(t);
            }

            @Override
            public void cue(Mechanics.Threat t, long mc) {
                before.cue(t, mc);
            }

            @Override
            public void resolved(Mechanics.Threat t, long mc, boolean landed) {
                before.resolved(t, mc, landed);
            }
        };
        // the fake player isn't in the level's player list: let the hunt see it (the real player is found on its own)
        OsrsTargetGoal.extraPlayers = w -> w == p.level() && !c.realPlayer() ? List.of(p) : List.of();
        Mechanics.extraPlayers = w -> w == p.level() ? List.of(p) : List.of();
        Aggression.forget(p.getUUID()); // the real player has stood on the platform for minutes: no tolerance carried in
        CombatZones.forget(p);
        CombatZones.forget(partner);
        try {
            return body.run();
        } finally {
            hits = null;
            threats = null;
            kills = null;
            Mechanics.pendingListener = before;
            OsrsTargetGoal.extraPlayers = huntBefore;
            Mechanics.extraPlayers = areaBefore;
            for (Mob m : ticking) {
                m.discard();
            }
            for (Mob m : still) {
                m.discard();
            }
            ticking.clear();
            still.clear();
            settle(p);
            CombatZones.forget(p);
            CombatZones.forget(partner);
            Aggression.forget(p.getUUID());
            p.setDeltaMovement(Vec3.ZERO);
            c.home();
        }
    }

    /** Lands or drops whatever is still on its way to {@code p}, then lets the style spacing forget the hits that just landed. */
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
            List<Hit> h = hits;
            if (h != null) {
                h.add(new Hit(ActionManager.mcTick(), attacker, attack, style, damage));
            }
        });
        com.runecraft.content.GameEvents.onKill((player, entity, stats) -> {
            List<ServerPlayer> k = kills;
            if (k != null) {
                k.add(player);
            }
        });
    }

    /** One Minecraft tick of the world as the checks see it: the clock, the ticking monsters, the pending hits. */
    private void step(int mc) {
        for (int i = 0; i < mc; i++) {
            ActionManager.advanceForSelfTest();
            for (Mob m : ticking) {
                if (m.isAlive()) {
                    m.tickCount++; // as ServerLevel.tickNonPassenger does (see BossFightChecks)
                    m.tick();
                }
            }
            Mechanics.tickForSelfTest();
            c.heal();
        }
    }

    private <T extends Mob> T spawn(EntityType<T> type, double dx, double dz, boolean ticks) {
        T mob = type.create(c.world);
        if (mob == null) {
            throw new AssertionError("can't create " + net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type));
        }
        return place(mob, dx, dz, ticks);
    }

    /** Puts a new monster on the platform as a world spawner would (SPAWNER: an open-world creature gets its spawn point). */
    private <T extends Mob> T place(T mob, double dx, double dz, boolean ticks) {
        BlockPos base = TestContext.PLATFORM;
        mob.moveTo(base.getX() + 0.5 + dx, base.getY(), base.getZ() + 0.5 + dz, 0f, 0f);
        mob.finalizeSpawn(c.world, c.world.getCurrentDifficultyAt(base), MobSpawnType.SPAWNER, null);
        mob.ejectPassengers();
        mob.setPersistenceRequired();
        c.world.addFreshEntity(mob);
        (ticks ? ticking : still).add(mob);
        mob.getNavigation().stop();
        return mob;
    }

    /** A vanilla stand-in, put on OSRS AI as it joins the world; {@code home}: where a spawn point placed it. */
    private Mob vanilla(EntityType<? extends Mob> type, double dx, double dz, boolean home) {
        Mob mob = spawn(type, dx, dz, true);
        expect(VanillaOsrsAi.isInstalled(mob) || VanillaOsrsAi.install(mob),
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type) + " is not on OSRS AI");
        MonsterHome.set(mob, home ? mob.blockPosition() : null);
        return mob;
    }

    private OsrsMonsterEntity module(String id, double dx, double dz, boolean ticks) {
        EntityType<?> type = ModEntities.typeById(id);
        if (type == null) {
            throw new SelfTest.Skip("no creature " + id);
        }
        Entity e = type.create(c.world);
        if (!(e instanceof OsrsMonsterEntity m)) {
            throw new AssertionError(id + " is not an OSRS monster");
        }
        return place(m, dx, dz, ticks);
    }

    private void moveTo(ServerPlayer p, double dx, double dz) {
        BlockPos base = TestContext.PLATFORM;
        p.teleportTo(c.world, base.getX() + 0.5 + dx, base.getY(), base.getZ() + 0.5 + dz, p.getYRot(), p.getXRot());
    }

    private void moveTo(Mob m, double dx, double dz) {
        BlockPos base = TestContext.PLATFORM;
        m.teleportTo(base.getX() + 0.5 + dx, base.getY(), base.getZ() + 0.5 + dz);
        m.getNavigation().stop();
    }

    /** Sets every combat skill (combat level 3: the level a fresh account starts at). */
    private void lowLevel() {
        for (Skill s : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC, Skill.PRAYER}) {
            Players.data(c.player).skills().setXpTenths(s, Experience.xpForLevel(1) * 10);
        }
        Players.data(c.player).skills().setXpTenths(Skill.HITPOINTS, Experience.xpForLevel(10) * 10);
    }

    private int combatLevel() {
        return Players.data(c.player).skills().combatLevel();
    }

    private List<Hit> from(LivingEntity attacker) {
        List<Hit> out = new ArrayList<>();
        for (Hit h : hits) {
            if (h.attacker() == attacker) {
                out.add(h);
            }
        }
        return out;
    }

    private static boolean has(List<Goal> goals, Class<?> kind) {
        for (Goal g : goals) {
            if (kind.isInstance(g)) {
                return true;
            }
        }
        return false;
    }

    private static void expect(boolean ok, String failure) {
        if (!ok) {
            throw new AssertionError(failure);
        }
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static double fromHome(Mob m) {
        return MonsterHome.fromHome(MonsterHome.get(m), m.getX(), m.getZ());
    }

    // ------------------------------------------------------------------ checks

    /**
     * The vanilla stand-ins lose their vanilla attack, target, leap and potion goals and fight on OSRS AI: a giant spider hits with
     * its OSRS stats (level 27, max hit 3, stab, every 4 game ticks) through melee wind-ups, a witch casts the level 20 dark wizard's
     * spell as a projectile, and a beneficial potion effect never takes on them.
     */
    private String vanillaOnOsrsAi() {
        ServerPlayer p = c.player;
        StringBuilder out = new StringBuilder();
        for (EntityType<? extends Mob> type : List.<EntityType<? extends Mob>>of(EntityType.SPIDER, EntityType.CAVE_SPIDER,
                EntityType.SILVERFISH, EntityType.WOLF, EntityType.WITCH, EntityType.ZOMBIE)) {
            Mob m = vanilla(type, 4, 4, false);
            List<Goal> goals = VanillaOsrsAi.goals(m, false);
            List<Goal> targets = VanillaOsrsAi.goals(m, true);
            String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
            expect(has(goals, OsrsAttackGoal.class), id + " has no OSRS attack goal");
            expect(!has(goals, net.minecraft.world.entity.ai.goal.MeleeAttackGoal.class)
                    && !has(goals, net.minecraft.world.entity.ai.goal.RangedAttackGoal.class)
                    && !has(goals, net.minecraft.world.entity.ai.goal.LeapAtTargetGoal.class)
                    && !has(goals, net.minecraft.world.entity.ai.goal.RangedBowAttackGoal.class), id + " kept a vanilla attack goal: " + goals);
            expect(targets.size() == 1 && targets.get(0) instanceof OsrsTargetGoal, id + " target goals: " + targets);
            m.discard();
        }
        ticking.clear();

        // a giant spider against the player: OSRS stats, real-time melee wind-ups
        Mob spider = vanilla(EntityType.SPIDER, 1.5, 0, false);
        MonsterStats s = MonsterStats.of(spider);
        expect(s.combatLevel() == 27 && s.maxHit() == 3 && s.attackSpeed() == 4 && s.style() == AttackType.STAB,
                "the giant spider's stats: level " + s.combatLevel() + ", max " + s.maxHit() + ", speed " + s.attackSpeed() + ", " + s.style());
        spider.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200));
        expect(!spider.hasEffect(MobEffects.REGENERATION), "a beneficial effect took on the giant spider");
        spider.setTarget(p);
        int windupsBefore = count(Mechanics.Kind.WINDUP);
        step(240);
        List<Hit> bites = from(spider);
        int windups = count(Mechanics.Kind.WINDUP) - windupsBefore;
        expect(bites.size() >= 3 && bites.size() <= 6, "the giant spider hit " + bites.size() + " times in 12 s (OSRS: every 2.4 s)");
        expect(windups >= bites.size(), "hits without a melee wind-up: " + bites.size() + " hits, " + windups + " wind-ups");
        int max = 0;
        for (Hit h : bites) {
            expect(h.style() == AttackType.STAB, "the giant spider hit with " + h.style());
            expect(h.damage() >= 0 && h.damage() <= 3, "the giant spider hit " + h.damage() + " (max 3)");
            max = Math.max(max, h.damage());
        }
        out.append("giant spider: ").append(bites.size()).append(" stab hits (max seen ").append(max).append("/3) through ")
                .append(windups).append(" wind-ups");
        spider.discard();

        // the witch stands in for the level 20 dark wizard: magic, as a projectile, max 6
        Mob witch = vanilla(EntityType.WITCH, -3, 0, false);
        witch.setTarget(p);
        int boltsBefore = count(Mechanics.Kind.PROJECTILE);
        step(160);
        int bolts = count(Mechanics.Kind.PROJECTILE) - boltsBefore;
        List<Hit> spells = from(witch);
        expect(bolts >= 2, "the witch cast " + bolts + " projectiles in 8 s");
        for (Hit h : spells) {
            expect(h.style() == AttackType.MAGIC && h.damage() <= 6, "the witch hit " + h.damage() + " " + h.style());
        }
        expect(!witch.hasEffect(MobEffects.MOVEMENT_SPEED) && !witch.hasEffect(MobEffects.REGENERATION), "the witch drank a potion");
        return out + "; witch: " + bolts + " magic projectiles, " + spells.size() + " landed";
    }

    private static int count(Mechanics.Kind kind) {
        int n = 0;
        for (Mechanics.Threat t : threats) {
            if (t.kind() == kind) {
                n++;
            }
        }
        return n;
    }

    /**
     * An aggressive giant spider (level 27) ignores a player above level 54 and attacks a level 3 one within its hunt range, stops
     * once the player has been tolerated for 10 minutes, and a creature the wiki calls passive (an abyssal demon) never starts a fight.
     */
    private String aggression() {
        ServerPlayer p = c.player;
        // monsters never hunt an invulnerable player (as vanilla's targeting): the real self-test player is one between checks
        boolean invulnerable = p.isInvulnerable();
        p.setInvulnerable(false);
        try {
            return aggressionBody(p);
        } finally {
            p.setInvulnerable(invulnerable);
        }
    }

    private String aggressionBody(ServerPlayer p) {
        Mob spider = vanilla(EntityType.SPIDER, 6, 0, true);
        expect(combatLevel() > 54, "the self-test player is level " + combatLevel());
        step(60);
        expect(spider.getTarget() == null, "the giant spider attacked a level " + combatLevel() + " player (OSRS: above twice its level is ignored)");
        lowLevel();
        expect(combatLevel() <= 54, "the low-level player is level " + combatLevel());
        long t0 = ActionManager.mcTick();
        for (int i = 0; i < 40 && spider.getTarget() == null; i++) {
            step(1);
        }
        expect(spider.getTarget() == p, "the giant spider ignored a level " + combatLevel() + " player 6 blocks away");
        long took = ActionManager.mcTick() - t0;
        // tolerance: 10 minutes in the same area
        spider.setTarget(null);
        long g = ActionManager.gameTick();
        Aggression.gameTick(p, g - com.runecraft.osrs.CombatBonuses.TOLERANCE_TICKS);
        Aggression.gameTick(p, g);
        expect(Aggression.tolerant(p, g), "the player isn't tolerated after 10 minutes");
        step(40);
        expect(spider.getTarget() == null, "the giant spider attacked a tolerated player");
        Aggression.forget(p.getUUID());
        spider.discard();

        // the wiki's passive Slayer creature: never starts a fight, even with a level 3 player next to it
        OsrsMonsterEntity demon = module("abyssal_demon", -5, 0, true);
        expect(demon.isOpenWorld(), "the abyssal demon has no spawn point");
        step(40);
        expect(demon.getTarget() == null, "an abyssal demon (wiki: not aggressive) attacked unprovoked");
        return "a level 27 giant spider ignores level 126 and attacks level " + combatLevel() + " after " + took
                + " MC; tolerated after 10 min; an abyssal demon stays passive";
    }

    /**
     * A giant spider that chased a player past its aggression range gives up and walks back to its spawn point, keeping its damage
     * (round 43: OSRS has no reset; it regenerates a hitpoint a minute, see boss:playability_monster_regen); it doesn't come after
     * the player while they stay out of its range.
     */
    private String leash() {
        ServerPlayer p = c.player;
        lowLevel();
        Mob spider = vanilla(EntityType.SPIDER, -10, 0, true);
        moveTo(p, -3, 0);
        spider.setTarget(p); // as its hunt does for a vulnerable low-level player (see boss:playability_monster_aggression)
        step(40); // it closes in
        expect(spider.getTarget() == p, "the giant spider dropped a player 7 blocks from its spawn point");
        spider.setHealth(spider.getMaxHealth() / 2);
        moveTo(p, 11, 8); // 22.5 blocks from its spawn point: beyond its 16-block aggression range and the 4 of slack
        OsrsTargetGoal goal = OsrsTargetGoal.of(spider);
        expect(goal != null, "no OSRS target goal");
        step(3);
        expect(spider.getTarget() == null && goal.returning(), "the giant spider kept chasing a player "
                + fmt(MonsterHome.fromHome(MonsterHome.get(spider), p.getX(), p.getZ())) + " blocks from its spawn point");
        double startedAt = fromHome(spider);
        int t = 0;
        while (goal.returning() && t < MonsterAi.RETURN_TIMEOUT_MC + 20) {
            step(1);
            t++;
        }
        expect(!goal.returning(), "the giant spider never got home");
        expect(fromHome(spider) <= MonsterAi.HOME_BLOCKS + 0.5, "the giant spider stopped " + fmt(fromHome(spider)) + " blocks from home");
        expect(spider.getHealth() <= spider.getMaxHealth() / 2 + 1, "the giant spider healed to " + spider.getHealth() + "/"
                + spider.getMaxHealth() + " at home (OSRS: no reset, 1 hitpoint a minute)");
        step(40);
        expect(spider.getTarget() == null, "the giant spider came back for a player outside its range");
        return "gave up at " + fmt(startedAt) + " blocks from home, walked back in " + t + " MC keeping its damage ("
                + fmt(spider.getHealth()) + "/" + fmt(spider.getMaxHealth()) + ")";
    }

    /**
     * A giant spider shot from beyond its aggression range (the player 26 blocks off its spawn point; spells reach 20) doesn't give
     * up: it walks out to the edge of its 7-tile max range and stands there, keeping its damage, while the shots come at the dark
     * bow's 9 ticks (the slowest weapon); only once they have stopped for {@link MonsterAi#UNDER_FIRE_MC} (6 s) does it walk home,
     * still damaged (round 43: no heal at home).
     */
    private String leashUnderFire() {
        ServerPlayer p = c.player;
        Mob spider = vanilla(EntityType.SPIDER, -10, -8, true);
        OsrsTargetGoal goal = OsrsTargetGoal.of(spider);
        expect(goal != null, "no OSRS target goal");
        float half = spider.getMaxHealth() / 2;
        spider.setHealth(half);
        moveTo(p, 11, 8);
        double playerFromHome = MonsterHome.fromHome(MonsterHome.get(spider), p.getX(), p.getZ());
        expect(playerFromHome > MonsterAi.aggressionRange(ThreatTiming.MELEE_REACH) + MonsterAi.GIVE_UP_SLACK_BLOCKS,
                "the player is only " + fmt(playerFromHome) + " blocks from the spider's spawn point");
        int held = 0;
        double furthest = 0;
        int shots = 0;
        int period = AttackClock.periodMc(9); // a dark bow's volley (108 MC), longer than vanilla's 100 MC hurt-by memory
        int since = 20; // MC from the last shot to the end of the shooting
        for (int i = 0; i < 3 * period + since; i++) {
            if (i % period == 0) {
                CombatHandler.applyDamage(p, spider, 0); // a miss is still an attack
                shots++;
            }
            step(1);
            expect(!goal.returning(), "the giant spider gave up " + i + " MC in while the player kept shooting it from "
                    + fmt(playerFromHome) + " blocks off its spawn point (it stood " + fmt(fromHome(spider)) + " blocks out; max range "
                    + fmt(MonsterAi.MAX_RANGE_BLOCKS) + ", leash " + fmt(MonsterAi.LEASH_BLOCKS) + ")");
            expect(spider.getTarget() == p, "the giant spider dropped the player shooting it");
            expect(spider.getHealth() <= half, "the giant spider healed to " + spider.getHealth() + " while being shot");
            furthest = Math.max(furthest, fromHome(spider));
            if (goal.holding()) {
                held++;
            }
        }
        expect(held > 0, "the giant spider never stood at the edge of its range (got " + fmt(furthest) + " blocks out)");
        expect(furthest <= MonsterAi.MAX_RANGE_BLOCKS + 2, "the giant spider walked " + fmt(furthest)
                + " blocks from its spawn point (max range " + fmt(MonsterAi.MAX_RANGE_BLOCKS) + ")");
        // the shots stop: UNDER_FIRE_MC later it gives up and walks home, still damaged
        int quiet = since;
        while (!goal.returning() && quiet < MonsterAi.UNDER_FIRE_MC + 40) {
            step(1);
            quiet++;
        }
        expect(goal.returning(), "the giant spider never gave up the player " + quiet + " MC after the last shot");
        expect(quiet >= MonsterAi.UNDER_FIRE_MC - 5, "the giant spider gave up only " + quiet + " MC after the last shot");
        int t = 0;
        while (goal.returning() && t < MonsterAi.RETURN_TIMEOUT_MC + 20) {
            step(1);
            t++;
        }
        expect(!goal.returning(), "the giant spider never got home");
        expect(spider.getHealth() <= half + 1, "the giant spider healed to " + spider.getHealth() + "/" + spider.getMaxHealth()
                + " at home (OSRS: no reset, 1 hitpoint a minute)");
        return shots + " shots from " + fmt(playerFromHome) + " blocks: held " + held + " MC at " + fmt(furthest)
                + " blocks out, unhealed; gave up " + quiet + " MC after the last shot, home in " + t + " MC still damaged";
    }

    /**
     * Encounter spawns keep their own rules: a God Wars bodyguard placed by its chamber's spawner gets no spawn point (it stays an
     * encounter monster, kept in the chamber by the lair's leash); a boss's vanilla summon (Persten's leeches: silverfish as giant
     * rats) keeps the target it was given as it joins the world and attacks a maxed player, whom an open-world level 3 giant rat
     * ignores; a vanilla mob a camp placed (SPAWNER) gets a spawn point.
     */
    private String encounterSpawns() {
        ServerPlayer p = c.player;
        boolean invulnerable = p.isInvulnerable();
        p.setInvulnerable(false); // as the aggression check: monsters never hunt an invulnerable player
        try {
            return encounterSpawnsBody(p);
        } finally {
            p.setInvulnerable(invulnerable);
        }
    }

    private String encounterSpawnsBody(ServerPlayer p) {
        StringBuilder out = new StringBuilder();
        OsrsMonsterEntity starlight = module("starlight", 6, 6, false);
        expect(!starlight.isOpenWorld(), "Commander Zilyana's bodyguard Starlight got a spawn point (open-world rules in a boss chamber)");
        expect(starlight.huntRange() == MonsterAi.ENCOUNTER_HUNT_BLOCKS, "Starlight hunts " + fmt(starlight.huntRange()) + " blocks");
        out.append("Starlight: no spawn point, hunts ").append(fmt(starlight.huntRange())).append(" blocks");

        Mob camp = spawn(EntityType.SKELETON, -6, 6, false);
        expect(VanillaOsrsAi.isInstalled(camp), "a camp's skeleton is not on OSRS AI");
        expect(MonsterHome.get(camp) != null, "a skeleton a camp placed (SPAWNER) has no spawn point");
        camp.discard();
        out.append("; a camp's skeleton has a spawn point");

        expect(combatLevel() > 6, "the self-test player is level " + combatLevel());
        // a boss summons its minions with its own target set before they join the world (DataBossEntity.summonMinions)
        Mob leech = EntityType.SILVERFISH.create(c.world);
        expect(leech != null, "can't create a silverfish");
        BlockPos base = TestContext.PLATFORM;
        leech.moveTo(base.getX() + 3.5, base.getY(), base.getZ() + 0.5, 0f, 0f);
        leech.finalizeSpawn(c.world, c.world.getCurrentDifficultyAt(base), MobSpawnType.MOB_SUMMONED, null);
        leech.setTarget(p);
        c.world.addFreshEntity(leech);
        ticking.add(leech);
        expect(VanillaOsrsAi.isInstalled(leech), "the summoned silverfish is not on OSRS AI");
        expect(MonsterHome.get(leech) == null, "a boss's summon got a spawn point");
        expect(leech.getTarget() == p, "the summoned silverfish lost the target its boss gave it");
        step(20);
        expect(leech.getTarget() == p, "the summoned silverfish dropped a level " + combatLevel() + " player (it attacks any level)");
        leech.setTarget(null);
        int took = -1;
        for (int i = 0; i < 40; i++) {
            step(1);
            if (leech.getTarget() == p) {
                took = i + 1;
                break;
            }
        }
        expect(took > 0, "the summoned silverfish ignored a level " + combatLevel() + " player 3 blocks away");
        out.append("; a boss's silverfish keeps its target and hunts level ").append(combatLevel()).append(" (").append(took).append(" MC)");

        // the same giant rat placed by a spawn point follows the combat-level rule
        leech.discard();
        settle(p);
        CombatZones.forget(p);
        CombatZones.forget(leech);
        Mob rat = vanilla(EntityType.SILVERFISH, -3, 0, true);
        expect(CombatZones.npcMayAttack(rat, p), "single-way combat still holds the player");
        step(40);
        expect(rat.getTarget() == null, "an open-world giant rat (level 3) attacked a level " + combatLevel() + " player");
        return out + "; an open-world one ignores them";
    }

    /**
     * A giant spider fighting the player keeps fighting them while another player hits it from out of melee reach (vanilla switched
     * to every new attacker), stays in melee reach of its target meanwhile (the reachability test of the other player must not
     * steer it: its wall-climber navigation used to walk it toward them), and only switches once its target is gone.
     */
    private String retaliation() {
        ServerPlayer p = c.player;
        ServerPlayer partner = com.runecraft.platform.FakePlayer.partner(c.world);
        moveTo(partner, 0, 6);
        // an open-world spider: an encounter spawn would hunt the maxed player again once its target is gone
        Mob spider = vanilla(EntityType.SPIDER, 2, 0, true);
        spider.setTarget(p);
        int switches = 0;
        double widest = 0; // the spider's gap to its target
        LivingEntity last = spider.getTarget();
        for (int round = 0; round < 6; round++) {
            CombatHandler.applyDamage(partner, spider, 0); // the other player swings at it
            forgetSingleWay(spider, partner); // multi-way: only the target choice is tested here
            for (int i = 0; i < 24; i++) {
                if (i == 20) {
                    CombatHandler.applyDamage(p, spider, 0);
                    forgetSingleWay(spider, partner);
                }
                step(1);
                if (spider.getTarget() != last) {
                    switches++;
                    last = spider.getTarget();
                }
                widest = Math.max(widest, ThreatTiming.gap(p.getX() - spider.getX(), p.getZ() - spider.getZ(),
                        spider.getBbWidth(), p.getBbWidth()));
            }
        }
        expect(switches == 0 && spider.getTarget() == p, "the giant spider switched targets " + switches + " times (now "
                + (spider.getTarget() == null ? "none" : spider.getTarget().getName().getString()) + ")");
        expect(widest <= ThreatTiming.MELEE_REACH, "the giant spider drifted " + fmt(widest) + " blocks off its target while the other"
                + " player hit it (melee reach " + fmt(ThreatTiming.MELEE_REACH) + ")");
        // its target gone: the next attacker takes over
        OsrsMonsterEntity goblin = module(Monster.GOBLIN.entity(), -4, 0, false);
        spider.setTarget(goblin);
        goblin.discard();
        step(2);
        expect(spider.getTarget() == null, "the giant spider kept a target that is gone");
        CombatHandler.applyDamage(partner, spider, 0);
        forgetSingleWay(spider, partner);
        step(2);
        expect(spider.getTarget() == partner, "the giant spider didn't take on the next attacker once its target was gone");
        return "kept its target through 6 hits from another player, at most " + fmt(widest)
                + " blocks off it; switched once its target was gone";
    }

    private void forgetSingleWay(Mob spider, ServerPlayer partner) {
        CombatZones.forget(spider);
        CombatZones.forget(partner);
    }

    /**
     * Single-way: a giant spider after a player who is fighting a goblin drops its target after about a second instead of hovering;
     * the player trying to attack the spider is told "I'm already under attack." with the goblin named; the lock is released at
     * once when the goblin wanders more than 6 blocks off.
     */
    private String singleWay() {
        ServerPlayer p = c.player;
        if (CombatZones.isMulti(p)) {
            throw new SelfTest.Skip("the self-test platform is in a multicombat area");
        }
        OsrsMonsterEntity goblin = module(Monster.GOBLIN.entity(), 1.5, 0, false);
        goblin.setTarget(p);
        CombatHandler.applyDamage(goblin, p, 0); // the goblin swings at the player: they are in combat
        Mob spider = vanilla(EntityType.SPIDER, -3, 0, false);
        spider.setTarget(p);
        expect(!CombatZones.npcMayAttack(spider, p), "single-way combat let a second monster attack the player");
        int dropped = -1;
        for (int i = 0; i < 40; i++) {
            step(1);
            if (spider.getTarget() == null) {
                dropped = i + 1;
                break;
            }
        }
        expect(dropped > 0 && dropped <= MonsterAi.BLOCKED_DROP_MC + 2, "the blocked giant spider hovered " + (dropped < 0 ? ">40" : dropped)
                + " MC (drops after " + MonsterAi.BLOCKED_DROP_MC + ")");
        expect(from(spider).isEmpty(), "the blocked giant spider hit the player");
        String[] told = new String[1];
        CombatZones.RefusalListener before = CombatZones.refusalListener;
        CombatZones.refusalListener = (player, r, line) -> told[0] = r.verdict() + ": " + line;
        try {
            expect(!CombatHandler.singleWayAllows(p, spider), "the player could attack the spider while the goblin is on them");
        } finally {
            CombatZones.refusalListener = before;
        }
        expect(told[0] != null && told[0].startsWith(SingleCombat.Verdict.ATTACKER_BUSY.name()) && told[0].toLowerCase(Locale.ROOT).contains("goblin"),
                "the refusal didn't name the goblin: " + told[0]);
        SingleCombat.Refusal r = CombatZones.refusal(p, spider);
        expect(r.blocker() == goblin.getId(), "the blocker is " + r.blocker() + ", not the goblin " + goblin.getId());
        moveTo(goblin, 9, 0); // wandered off, still after the player
        expect(CombatZones.check(p, spider) == SingleCombat.Verdict.OK, "the lock held with the goblin 9 blocks away");
        moveTo(goblin, 1.5, 0);
        expect(CombatZones.check(p, spider) != SingleCombat.Verdict.OK, "the lock didn't come back with the goblin next to the player");
        goblin.setTarget(null);
        expect(CombatZones.check(p, spider) == SingleCombat.Verdict.OK, "the lock held after the goblin gave up the player");
        return "dropped after " + dropped + " MC; told \"" + told[0] + "\"; released with the goblin 9 blocks off or gone home";
    }

    /**
     * The top damager gets the kill, not the last hit: the partner deals more damage, the player lands the killing blow, and the
     * drop lands still on the goblin's tile, the partner's alone until its 60 seconds are up.
     */
    private String topDamager() {
        ServerPlayer p = c.player;
        ServerPlayer partner = com.runecraft.platform.FakePlayer.partner(c.world);
        moveTo(partner, 0, 3);
        OsrsMonsterEntity goblin = module(Monster.GOBLIN.entity(), 2, 0, false);
        int hp = (int) Math.ceil(goblin.getHealth());
        expect(hp >= 3, "the goblin has " + hp + " hitpoints");
        CombatHandler.applyDamage(partner, goblin, hp - 2);
        CombatZones.forget(goblin);
        CombatZones.forget(partner);
        expect(KillCredit.damage(goblin).getOrDefault(partner.getUUID(), 0L) == hp - 2, "the partner's damage wasn't recorded: "
                + KillCredit.damage(goblin));
        Vec3 at = goblin.position();
        CombatHandler.applyDamage(p, goblin, 2); // the killing blow, 2 damage against the partner's hp - 2
        expect(!goblin.isAlive(), "the goblin survived");
        expect(kills.size() == 1 && kills.get(0) == partner, "the kill went to " + (kills.isEmpty() ? "nobody"
                : kills.get(0).getGameProfile().getName()) + ", not the top damager");
        List<ItemEntity> drops = c.world.getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(2),
                e -> e.isAlive() && "bones".equals(Inv.idOf(e.getItem())));
        expect(!drops.isEmpty(), "the goblin dropped no bones");
        ItemEntity bones = drops.get(0);
        expect(partner.getUUID().equals(bones.getTarget()), "the bones are " + (bones.getTarget() == null ? "everyone's" : "someone else's")
                + ", not the partner's");
        double flung = Math.sqrt(bones.getDeltaMovement().x * bones.getDeltaMovement().x + bones.getDeltaMovement().z * bones.getDeltaMovement().z);
        expect(flung < 0.01, "the bones were flung at " + fmt(flung));
        expect(bones.position().distanceTo(at) < 0.75, "the bones landed " + fmt(bones.position().distanceTo(at)) + " blocks off");
        LootDrops.advanceForSelfTest(c.server, MonsterAi.PRIVATE_LOOT_MC);
        expect(bones.getTarget() == null, "the bones were still private after 60 s");
        return "kill and drop to the partner (" + (hp - 2) + " damage) over the killing blow (2); bones still, private, public after 60 s";
    }

    // ------------------------------------------------------------------ round 43

    /** Runs {@code body} with the player vulnerable (monsters never hunt an invulnerable player, as vanilla's targeting). */
    private String vulnerable(Body body) throws Exception {
        ServerPlayer p = c.player;
        boolean invulnerable = p.isInvulnerable();
        p.setInvulnerable(false);
        try {
            return body.run();
        } finally {
            p.setInvulnerable(invulnerable);
        }
    }

    /**
     * Bosses the wiki infobox calls not aggressive (the Kraken, Vorkath, the Abyssal Sire, the Nightmare, Dusk, Delrith) never pick a
     * player on their own, where an aggressive one (Obor) would; the Giant Mole, spawned with nobody to fight next to a level 3
     * player, leaves them alone for 2 seconds and fights back the moment she is hit.
     */
    private String passiveBosses() throws Exception {
        return vulnerable(this::passiveBossesBody);
    }

    private String passiveBossesBody() {
        ServerPlayer p = c.player;
        lowLevel();
        StringBuilder out = new StringBuilder("passive:");
        for (String id : List.of("kraken", "vorkath", "abyssal_sire", "the_nightmare", "dusk")) {
            EntityType<com.runecraft.boss.DataBossEntity> type = ModEntities.DATA_BOSS_TYPES.get(id);
            expect(type != null, "no boss " + id);
            com.runecraft.boss.DataBossEntity b = type.create(c.world);
            expect(b != null, "can't create " + id);
            try {
                expect(!b.wantsToAttack(p), id + " (wiki: not aggressive) would attack a player unprovoked");
            } finally {
                b.discard();
            }
            out.append(' ').append(id);
        }
        com.runecraft.entity.BossEntity obor = ModEntities.BOSS_TYPES.get(com.runecraft.osrs.data.Boss.OBOR).create(c.world);
        com.runecraft.entity.BossEntity delrith = ModEntities.BOSS_TYPES.get(com.runecraft.osrs.data.Boss.DELRITH).create(c.world);
        expect(obor != null && delrith != null, "can't create Obor or Delrith");
        try {
            expect(obor.wantsToAttack(p), "Obor (wiki: aggressive) would leave a player alone");
            expect(!delrith.wantsToAttack(p), "Delrith (wiki: not aggressive) would attack a player unprovoked");
        } finally {
            obor.discard();
            delrith.discard();
        }
        out.append(", Delrith; Obor aggressive");
        com.runecraft.osrs.content.BossDef def = com.runecraft.osrs.content.ContentRegistry.boss("giant_mole");
        expect(def != null, "no Giant Mole");
        com.runecraft.boss.DataBossEntity mole = com.runecraft.boss.DataBossService.spawn(c.world, c.near(-4, 0), def, null);
        expect(mole != null, "the Giant Mole didn't spawn");
        ticking.add(mole);
        step(40);
        expect(mole.getTarget() == null, "the Giant Mole (wiki: not aggressive) went for a level " + combatLevel() + " player 4 blocks away");
        CombatHandler.applyDamage(p, mole, 0); // disturbed: a swing at her, even a miss
        step(2);
        expect(mole.getTarget() == p, "the Giant Mole didn't fight back when attacked");
        return out + "; the Giant Mole ignored a level " + combatLevel() + " player for 40 MC and fought back when hit";
    }

    /**
     * The passive vanilla stand-ins are on OSRS AI: no panic, breeding or tempting goals, their OSRS hitpoints (cow 8, sheep 5, wolf
     * 34 against vanilla's 10, 8 and 8); a cow never starts a fight with a level 3 player but hits back when attacked, with its OSRS
     * crush and max hit; a chicken at its last hitpoint runs from the player instead of fighting (wiki, Chicken).
     */
    private String passiveAnimals() throws Exception {
        return vulnerable(this::passiveAnimalsBody);
    }

    private String passiveAnimalsBody() {
        ServerPlayer p = c.player;
        lowLevel();
        StringBuilder out = new StringBuilder();
        Mob cow = vanilla(EntityType.COW, 2.5, 0, true);
        Mob sheep = vanilla(EntityType.SHEEP, -6, 6, true);
        Mob wolf = vanilla(EntityType.WOLF, 6, 6, true);
        List<Goal> goals = VanillaOsrsAi.goals(cow, false);
        expect(has(goals, OsrsAttackGoal.class) && has(goals, com.runecraft.entity.OsrsRetreatGoal.class), "the cow's goals: " + goals);
        expect(!has(goals, net.minecraft.world.entity.ai.goal.PanicGoal.class) && !has(goals, net.minecraft.world.entity.ai.goal.BreedGoal.class)
                && !has(goals, net.minecraft.world.entity.ai.goal.TemptGoal.class), "the cow kept vanilla panic/breed/tempt goals: " + goals);
        expect(has(VanillaOsrsAi.goals(sheep, false), net.minecraft.world.entity.ai.goal.EatBlockGoal.class), "the sheep stopped grazing");
        for (Object[] row : new Object[][]{{cow, Monster.COW}, {sheep, Monster.SHEEP}, {wolf, Monster.WOLF}}) {
            Mob m = (Mob) row[0];
            Monster def = (Monster) row[1];
            expect(Math.round(m.getMaxHealth()) == def.hitpoints() && Math.round(m.getHealth()) == def.hitpoints(),
                    def.displayName() + " has " + m.getHealth() + "/" + m.getMaxHealth() + " hitpoints (OSRS " + def.hitpoints() + ")");
        }
        out.append("cow ").append(fmt(cow.getMaxHealth())).append(", sheep ").append(fmt(sheep.getMaxHealth())).append(", wolf ")
                .append(fmt(wolf.getMaxHealth())).append(" hitpoints");
        sheep.discard();
        wolf.discard();
        step(40);
        expect(cow.getTarget() == null, "a cow (passive) went for a level " + combatLevel() + " player");
        CombatHandler.applyDamage(p, cow, 0); // the player swings at it
        step(160);
        List<Hit> butts = from(cow);
        expect(cow.getTarget() == p, "the cow didn't fight back");
        expect(!butts.isEmpty(), "the cow fought back but never hit in 8 s");
        for (Hit h : butts) {
            expect(h.style() == AttackType.CRUSH && h.damage() <= Monster.COW.maxHit(), "the cow hit " + h.damage() + " " + h.style());
        }
        out.append("; the cow ignored level ").append(combatLevel()).append(" and hit back ").append(butts.size()).append(" times");
        cow.discard();
        settle(p);
        CombatZones.forget(p);
        CombatZones.forget(cow);
        // the chicken retreats at its last hitpoint (of 3)
        Mob chicken = vanilla(EntityType.CHICKEN, 1.5, 0, true);
        expect(Math.round(chicken.getMaxHealth()) == Monster.CHICKEN.hitpoints(), "a chicken has " + chicken.getMaxHealth() + " hitpoints");
        chicken.setTarget(p);
        chicken.setHealth(1);
        double before = chicken.distanceTo(p);
        step(60);
        expect(chicken.getTarget() == p, "the retreating chicken forgot the player");
        expect(chicken.distanceTo(p) > before + 1.5, "the chicken at 1 hitpoint stayed " + fmt(chicken.distanceTo(p))
                + " blocks from the player (was " + fmt(before) + ")");
        expect(from(chicken).isEmpty(), "the retreating chicken attacked");
        return out + "; a chicken at 1 hitpoint ran from " + fmt(before) + " to " + fmt(chicken.distanceTo(p)) + " blocks";
    }

    /** A vanilla zombie and skeleton on OSRS AI put out the fire the sun sets on them before it hurts (OSRS monsters don't burn). */
    private String noSunburn() {
        Mob zombie = vanilla(EntityType.ZOMBIE, 4, 4, true);
        Mob skeleton = vanilla(EntityType.SKELETON, -4, 4, true);
        float zh = zombie.getHealth();
        float sh = skeleton.getHealth();
        step(4); // their goals start
        for (int i = 0; i < 5; i++) {
            // the sun's 8 s fire (Zombie.aiStep and AbstractSkeleton.aiStep set it mid-tick, before their goals run; set between
            // ticks here, a tick off the fire's every-20-ticks hurt)
            zombie.setRemainingFireTicks(159);
            skeleton.setRemainingFireTicks(159);
            step(20);
        }
        expect(!zombie.isOnFire() && zombie.getRemainingFireTicks() <= 0, "the zombie is still burning");
        expect(!skeleton.isOnFire() && skeleton.getRemainingFireTicks() <= 0, "the skeleton is still burning");
        expect(zombie.getHealth() >= zh && skeleton.getHealth() >= sh, "burnt: zombie " + zombie.getHealth() + "/" + zh + ", skeleton "
                + skeleton.getHealth() + "/" + sh);
        return "set alight 5 times in 5 s, put out every time: zombie " + fmt(zombie.getHealth()) + ", skeleton "
                + fmt(skeleton.getHealth()) + " hitpoints (OSRS " + Monster.ZOMBIE.hitpoints() + " and " + Monster.SKELETON.hitpoints() + ")";
    }

    /** A stray vanilla villager in Gielinor becomes the Man or Woman it stands for: OSRS AI, a spawn point, fights back when hit. */
    private String villager() {
        ServerPlayer p = c.player;
        net.minecraft.world.entity.npc.Villager v = EntityType.VILLAGER.create(c.world);
        expect(v != null, "can't create a villager");
        BlockPos base = TestContext.PLATFORM;
        v.moveTo(base.getX() + 3.5, base.getY(), base.getZ() + 0.5, 0f, 0f);
        c.world.addFreshEntity(v);
        Mob man = VanillaOsrsAi.convertVillager(v);
        expect(man instanceof OsrsMonsterEntity, "the villager became " + man);
        still.add(man);
        expect(!v.isAlive() || v.isRemoved(), "the villager is still there beside its Man");
        MonsterStats s = MonsterStats.of(man);
        expect(s.combatLevel() == 2 && Math.round(man.getMaxHealth()) == 7, "the villager became a level " + s.combatLevel() + " "
                + s.name() + " with " + man.getMaxHealth() + " hitpoints");
        expect(MonsterHome.get(man) != null, "the converted villager has no spawn point");
        CombatHandler.applyDamage(p, man, 0);
        expect(man.getTarget() == p, "the converted " + s.name() + " didn't fight back");
        return "a villager became a level " + s.combatLevel() + " " + s.name() + " (" + fmt(man.getMaxHealth())
                + " hitpoints, spawn point kept) that fights back";
    }

    /**
     * Monsters regenerate a hitpoint every 100 game ticks (wiki: Mod Ash's default NPC rate) on their own clock, never above full, and
     * forget who damaged them once they are whole again (the next kill's credit starts afresh).
     */
    private String regen() {
        ServerPlayer p = c.player;
        OsrsMonsterEntity goblin = module(Monster.GOBLIN.entity(), 3, 0, false);
        float max = goblin.getMaxHealth();
        expect(max >= 3, "the goblin has " + max + " hitpoints");
        CombatHandler.applyDamage(p, goblin, 2);
        expect(KillCredit.damage(goblin).getOrDefault(p.getUUID(), 0L) == 2, "the player's damage wasn't recorded");
        goblin.tickCount = MonsterAi.REGEN_MC - 1;
        OsrsTargetGoal.regenerate(goblin); // a tick short of a minute: nothing yet
        expect(goblin.getHealth() == max - 2, "the goblin regenerated early: " + goblin.getHealth() + "/" + max);
        goblin.tickCount++;
        OsrsTargetGoal.regenerate(goblin);
        expect(goblin.getHealth() == max - 1, "a minute on, the goblin has " + goblin.getHealth() + "/" + max + " (OSRS: +1)");
        expect(!KillCredit.damage(goblin).isEmpty(), "the goblin forgot its damage before it was whole");
        goblin.tickCount = 2 * MonsterAi.REGEN_MC;
        OsrsTargetGoal.regenerate(goblin);
        expect(goblin.getHealth() == max, "two minutes on, the goblin has " + goblin.getHealth() + "/" + max);
        expect(KillCredit.damage(goblin).isEmpty(), "the goblin whole again still credits " + KillCredit.damage(goblin));
        goblin.tickCount = 3 * MonsterAi.REGEN_MC;
        OsrsTargetGoal.regenerate(goblin);
        expect(goblin.getHealth() == max, "the goblin regenerated past its maximum: " + goblin.getHealth() + "/" + max);
        // a vanilla stand-in regenerates through its upkeep goal on its own ticks
        Mob spider = vanilla(EntityType.SPIDER, -4, 0, true);
        step(4); // its goals start
        spider.setHealth(spider.getMaxHealth() - 3);
        spider.tickCount = MonsterAi.REGEN_MC - 1; // the next step's tick is the minute
        step(1);
        expect(spider.getHealth() == spider.getMaxHealth() - 2, "the giant spider has " + spider.getHealth() + "/" + spider.getMaxHealth()
                + " a minute on (OSRS: +1)");
        return "goblin " + fmt(max - 2) + " -> " + fmt(max - 1) + " -> " + fmt(max) + " (damage forgotten when whole), giant spider +1 a minute";
    }

    /**
     * Encounter monsters stay in their arena: a boss's summon whose arena centre lies 40 blocks off drops the player it was given
     * and won't pick them there, while one in its arena keeps fighting; an aggressive encounter creature (Scorpia's offspring, level
     * 15) attacks a maxed player, whom the combat-level rule would spare.
     */
    private String encounterArena() throws Exception {
        return vulnerable(this::encounterArenaBody);
    }

    private String encounterArenaBody() {
        ServerPlayer p = c.player;
        BlockPos base = TestContext.PLATFORM;
        Mob far = summon(EntityType.SILVERFISH, 3, base.offset(40, 0, 0));
        Mob near = summon(EntityType.SILVERFISH, -3, null);
        step(2);
        expect(far.getTarget() == null, "a summon kept a player " + fmt(MonsterHome.fromHome(base.offset(40, 0, 0), p.getX(), p.getZ()))
                + " blocks from its arena centre (arena " + fmt(MonsterAi.ENCOUNTER_LEASH_BLOCKS) + ")");
        expect(near.getTarget() == p, "a summon in its arena dropped the player");
        far.discard();
        near.discard();
        settle(p);
        CombatZones.forget(p);
        Mob farAgain = summon(EntityType.SILVERFISH, 3, base.offset(40, 0, 0));
        farAgain.setTarget(null);
        step(40);
        expect(farAgain.getTarget() == null, "a summon picked a player outside its arena");
        farAgain.discard();
        OsrsMonsterEntity offspring = module("scorpias_offspring", -5, 5, false);
        expect(!offspring.isOpenWorld(), "Scorpia's offspring got a spawn point");
        expect(combatLevel() > 2 * MonsterStats.of(offspring).combatLevel(), "the self-test player is level " + combatLevel());
        expect(offspring.wantsToAttack(p), "Scorpia's offspring (an encounter monster) spared a level " + combatLevel() + " player");
        return "out of its arena: target dropped, none picked; in it: kept; Scorpia's offspring (level "
                + MonsterStats.of(offspring).combatLevel() + ") attacks level " + combatLevel();
    }

    /** A boss's vanilla summon (as DataBossEntity.summonMinions makes it) given the player, its arena centre at {@code anchor}. */
    private Mob summon(EntityType<? extends Mob> type, double dx, BlockPos anchor) {
        Mob mob = type.create(c.world);
        expect(mob != null, "can't create a summon");
        BlockPos base = TestContext.PLATFORM;
        mob.moveTo(base.getX() + 0.5 + dx, base.getY(), base.getZ() + 0.5, 0f, 0f);
        mob.finalizeSpawn(c.world, c.world.getCurrentDifficultyAt(base), MobSpawnType.MOB_SUMMONED, null);
        if (anchor != null) {
            mob.getPersistentData().putLong(OsrsTargetGoal.ANCHOR_KEY, anchor.asLong());
        }
        mob.setTarget(c.player);
        c.world.addFreshEntity(mob);
        ticking.add(mob);
        expect(VanillaOsrsAi.isInstalled(mob) && MonsterHome.get(mob) == null, "the summon is not an encounter monster on OSRS AI");
        return mob;
    }

    /**
     * Single-way: a zombie fenced in 4 blocks from the player (in sight over the fence, but with no way out) that hit them holds the
     * lock only until it has found no path for a second, well before OSRS's 8 game ticks; while it holds, a held attack on a goblin
     * sends the refusal line once, not on every click.
     */
    private String walledOff() {
        ServerPlayer p = c.player;
        if (CombatZones.isMulti(p)) {
            throw new SelfTest.Skip("the self-test platform is in a multicombat area");
        }
        moveTo(p, 0, 0);
        Mob zombie = vanilla(EntityType.ZOMBIE, -4, 0, true);
        BlockPos at = zombie.blockPosition();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    c.place(at.offset(dx, 0, dz), net.minecraft.world.level.block.Blocks.OAK_FENCE.defaultBlockState());
                }
            }
        }
        zombie.setTarget(p);
        CombatHandler.applyDamage(zombie, p, 0); // it swung at the player: single-way combat holds them
        int swung = from(zombie).size();
        OsrsMonsterEntity goblin = module(Monster.GOBLIN.entity(), 3, 0, false);
        expect(p.hasLineOfSight(zombie), "the player can't see the fenced zombie");
        expect(CombatZones.check(p, goblin) == SingleCombat.Verdict.ATTACKER_BUSY, "the zombie didn't hold the player ("
                + CombatZones.check(p, goblin) + ")");
        List<String> lines = new ArrayList<>();
        CombatZones.RefusalListener before = CombatZones.refusalListener;
        CombatZones.refusalListener = (player, r, line) -> lines.add(line);
        try {
            for (int i = 0; i < 4; i++) {
                expect(!CombatZones.allows(p, goblin), "the held attack on the goblin went through");
                step(5);
            }
        } finally {
            CombatZones.refusalListener = before;
        }
        long shown = lines.stream().filter(java.util.Objects::nonNull).count();
        expect(lines.size() == 4 && shown == 1, "4 held attacks sent " + shown + " refusal lines: " + lines);
        int released = -1;
        for (int i = 0; i < 120; i++) {
            step(1);
            if (CombatZones.check(p, goblin) == SingleCombat.Verdict.OK) {
                released = i + 1;
                break;
            }
        }
        int bound = MonsterAi.PATH_BLOCKED_MC + 2 * MonsterAi.REACH_CHECK_MC;
        expect(released > 0 && released <= bound, "the walled-off zombie held the lock " + (released < 0 ? ">120" : released)
                + " MC more (it lets go after " + MonsterAi.PATH_BLOCKED_MC + " MC without a path)");
        expect(from(zombie).size() == swung, "the fenced zombie hit the player");
        return "4 held attacks, 1 refusal line; the fenced zombie (in sight) let the lock go " + released + " MC later";
    }
}
