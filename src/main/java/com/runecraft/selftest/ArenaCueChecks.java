package com.runecraft.selftest;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.BossScript;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.content.bosses.ScriptedBoss;
import com.runecraft.content.godwars.GwdScript;
import com.runecraft.content.raids.RaidSelfTest;
import com.runecraft.osrs.ArenaTiming;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

/**
 * Human-budget checks of the scripted arenas (round 41 playability plan, scope arena-strikes-warnings; {@code arena:*}, plus
 * {@code boss:playability_arena_strike} for the real player): a boss's ground strike is drawn the tick it is made, redrawn
 * while pending and lands no sooner than {@link ArenaTiming#MIN_LEAD_MC} Minecraft ticks later, on a horizontal test that
 * matches its ring; raid strikes keep the same floor; boss warnings reach each player once, on the centre-screen line, and
 * warnings close together merge; the God Wars and Wilderness bosses' own strike list ({@link GwdScript}) keeps the same floor.
 * A probe script makes the strikes on a host boss entity that is never added to the world, so nothing else of a real boss runs.
 */
final class ArenaCueChecks {
    private static final int MC = ActionManager.MC_TICKS_PER_GAME_TICK;
    private static final String PROBE_ATTACK = "arena_probe";

    /** A scripted boss with nothing of its own: the checks make its strikes. */
    static final class Probe extends ScriptedBoss {
        void fire(DataBossEntity host, Vec3 at, int delay, int maxDamage, double radius, BiConsumer<List<ServerPlayer>, Vec3> onLand) {
            if (onLand == null) {
                namedStrike(PROBE_ATTACK, host, at, delay, maxDamage, radius, ParticleTypes.FLAME);
            } else {
                namedStrike(PROBE_ATTACK, host, at, delay, maxDamage, radius, ParticleTypes.FLAME, onLand);
            }
        }

        boolean pending() {
            return strikesPending();
        }
    }

    /** A God Wars/Wilderness boss script with nothing of its own: the checks make strikes on its own strike list. */
    static final class GwdProbe extends GwdScript {
        long fire(DataBossEntity host, Vec3 at, int delay, int maxHit, double radius) {
            return strike(PROBE_ATTACK, host, at, delay, maxHit, radius, ParticleTypes.SNOWFLAKE, ParticleTypes.ITEM_SNOWBALL, null);
        }

        boolean pending() {
            return strikesPending();
        }
    }

    private static boolean hooked;
    private static ServerPlayer watched;
    private static long probeHitMc = -1;

    private final TestContext c;

    private ArenaCueChecks(TestContext c) {
        this.c = c;
    }

    static List<SelfTest.Check> all(TestContext context) {
        ArenaCueChecks a = new ArenaCueChecks(context);
        List<SelfTest.Check> checks = new ArrayList<>();
        checks.add(new SelfTest.Check("arena:strike_floor", a::strikeFloor));
        checks.add(new SelfTest.Check("arena:strike_horizontal", a::strikeHorizontal));
        checks.add(new SelfTest.Check("arena:raid_strike_floor", a::raidStrikeFloor));
        checks.add(new SelfTest.Check("arena:gwd_strike_floor", a::gwdStrikeFloor));
        checks.add(new SelfTest.Check("arena:warn_telegraph", a::warnTelegraph));
        checks.add(new SelfTest.Check("boss:playability_arena_strike", a::playabilityStrike));
        return checks;
    }

    // ------------------------------------------------------------------ driving the probe

    /** A data boss entity to own the probe's strikes, created but never added to the world (so it never ticks). */
    private DataBossEntity host(Vec3 at) {
        List<String> ids = new ArrayList<>(ModEntities.DATA_BOSS_TYPES.keySet());
        java.util.Collections.sort(ids);
        for (String id : ids) {
            EntityType<DataBossEntity> type = ModEntities.DATA_BOSS_TYPES.get(id);
            if (ContentRegistry.boss(id) == null || type == null) {
                continue;
            }
            DataBossEntity e = type.create(c.world);
            if (e != null) {
                e.moveTo(at.x, at.y, at.z, 0, 0);
                // Not alive: its strikes land as typeless hits with no attacker, so the player is left under attack by
                // nothing (single-way combat, auto-retaliate) for the checks that follow.
                e.setHealth(0);
                return e;
            }
        }
        throw new AssertionError("no data boss type to host the probe");
    }

    /** Moves the clock on to the next game-tick boundary and runs the probe's game tick there. */
    private static void align(BossScript probe, DataBossEntity host) {
        do {
            ActionManager.advanceForSelfTest();
            probe.tick(host);
        } while (ActionManager.mcTick() % MC != 0);
        probe.gameTick(host, ActionManager.gameTick());
    }

    /** One Minecraft tick of the probe: its tick, and its game tick on each boundary (as DataBossEntity runs a script). */
    private static void mc(BossScript probe, DataBossEntity host) {
        ActionManager.advanceForSelfTest();
        probe.tick(host);
        if (ActionManager.mcTick() % MC == 0) {
            probe.gameTick(host, ActionManager.gameTick());
        }
    }

    /** Runs Minecraft ticks until the probe's strikes have all landed (at most {@code max}); returns how many ran. */
    private static int runUntilLanded(Probe probe, DataBossEntity host, int max) {
        return runUntilLanded(probe, probe::pending, host, max);
    }

    private static int runUntilLanded(BossScript probe, BooleanSupplier pending, DataBossEntity host, int max) {
        int n = 0;
        while (pending.getAsBoolean() && n < max) {
            mc(probe, host);
            n++;
        }
        if (pending.getAsBoolean()) {
            throw new AssertionError("the probe's strike never landed in " + max + " Minecraft ticks");
        }
        return n;
    }

    /** Records the probe's strike traces while {@code body} runs. */
    private static List<ArenaTiming.StrikeTrace> traced(Runnable body) {
        List<ArenaTiming.StrikeTrace> traces = new ArrayList<>();
        Consumer<ArenaTiming.StrikeTrace> before = ScriptedBoss.strikeListener;
        ScriptedBoss.strikeListener = before.andThen(traces::add);
        try {
            body.run();
        } finally {
            ScriptedBoss.strikeListener = before;
        }
        return traces;
    }

    /** Records the God Wars/Wilderness probe's strike traces while {@code body} runs. */
    private static List<ArenaTiming.StrikeTrace> tracedGwd(Runnable body) {
        List<ArenaTiming.StrikeTrace> traces = new ArrayList<>();
        Consumer<ArenaTiming.StrikeTrace> before = GwdScript.strikeListener;
        GwdScript.strikeListener = before.andThen(traces::add);
        try {
            body.run();
        } finally {
            GwdScript.strikeListener = before;
        }
        return traces;
    }

    private static void expect(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
    }

    // ------------------------------------------------------------------ checks

    /**
     * A delay-0 strike of radius 2 is drawn the tick it is made and at least three more times, and lands no sooner than 3
     * game ticks (36 Minecraft ticks) later; one made between game ticks still gets its full 36.
     */
    private String strikeFloor() {
        Vec3 far = Vec3.atBottomCenterOf(c.near(0, 0)).add(0, 40, 0); // above everyone: nobody is hit
        DataBossEntity host = host(far);
        Probe probe = new Probe();
        long[] mcs = new long[2];
        List<ArenaTiming.StrikeTrace> onTick = traced(() -> {
            align(probe, host);
            mcs[0] = ActionManager.mcTick();
            probe.fire(host, far, 0, 10, 2.0, null);
            mcs[1] = runUntilLanded(probe, host, 200);
        });
        expect(onTick.size() == 1, onTick.size() + " strikes landed (expected 1)");
        ArenaTiming.StrikeTrace t = onTick.get(0);
        expect(t.firstDrawnMc() == t.createdMc(), "the ring was first drawn " + (t.firstDrawnMc() - t.createdMc()) + " MC after it was made");
        expect(t.draws() >= 4, "the ring was drawn " + t.draws() + " times (on creation and at least 3 redraws expected)");
        expect(t.leadMc() >= ArenaTiming.MIN_LEAD_MC && t.leadMc() / MC >= ArenaTiming.STRIKE_FLOOR_TICKS,
                "a delay-0 strike landed " + t.leadMc() + " MC after it was made");
        expect(mcs[1] >= ArenaTiming.MIN_LEAD_MC, "a delay-0 strike landed " + mcs[1] + " real Minecraft ticks after it was made");
        // Made 5 Minecraft ticks into a game tick (a script's per-tick logic, a reaction to a player's hit).
        long[] mid = new long[1];
        List<ArenaTiming.StrikeTrace> between = traced(() -> {
            align(probe, host);
            for (int i = 0; i < 5; i++) {
                mc(probe, host);
            }
            probe.fire(host, far, 1, 10, 2.0, null);
            mid[0] = runUntilLanded(probe, host, 200);
        });
        expect(between.size() == 1 && between.get(0).unfair() == null, "a strike made between game ticks: "
                + (between.isEmpty() ? "never landed" : between.get(0).unfair()));
        expect(mid[0] >= ArenaTiming.MIN_LEAD_MC, "a strike made 5 MC into a game tick landed " + mid[0] + " MC later");
        return "delay 0 landed after " + mcs[1] + " MC (drawn " + t.draws() + " times); made mid-tick, after " + mid[0] + " MC";
    }

    /** The landing test matches the ring: 1.9 blocks off (0.5 higher) inside a radius-2 strike is hit, 2.1 is not. */
    private String strikeHorizontal() {
        expect(ArenaTiming.inStrike(1.9, 0.5, 0, 2.0) && !ArenaTiming.inStrike(2.1, 0.5, 0, 2.0)
                && !ArenaTiming.inStrike(0, 2.0, 0, 2.0), "the horizontal test with its height tolerance is wrong");
        ServerPlayer p = c.player;
        if (!c.realPlayer()) {
            return "geometry only (the fake player is not in the world's player list, which strikes search)";
        }
        Vec3 at = p.position();
        DataBossEntity host = host(at.add(6, 0, 0));
        Probe probe = new Probe();
        boolean[] hit = new boolean[2];
        traced(() -> {
            align(probe, host);
            probe.fire(host, at.add(-1.9, -0.5, 0), 0, 0, 2.0, (players, pos) -> hit[0] = players.contains(p));
            runUntilLanded(probe, host, 200);
            align(probe, host);
            probe.fire(host, at.add(-2.1, -0.5, 0), 0, 0, 2.0, (players, pos) -> hit[1] = players.contains(p));
            runUntilLanded(probe, host, 200);
        });
        expect(hit[0], "a player 1.9 blocks from a radius-2 strike's centre (0.5 above it) was not hit");
        expect(!hit[1], "a player 2.1 blocks from a radius-2 strike's centre was hit");
        return "1.9 blocks off (0.5 up) hit, 2.1 missed";
    }

    /**
     * The God Wars and Wilderness bosses' own strike list (GwdScript): Nex's 'Contain this!' (2 ticks, up to 60, radius 5) is
     * drawn the tick it is made and redrawn, and lands no sooner than 36 Minecraft ticks later; one made between game ticks
     * (Vet'ion's bolts come with his attack choice) keeps its full lead, and the tick it lands on is the one returned (his
     * cosmetic lightning lands then too).
     */
    private String gwdStrikeFloor() {
        Vec3 far = Vec3.atBottomCenterOf(c.near(0, 0)).add(0, 40, 0); // above everyone: nobody is hit
        DataBossEntity host = host(far);
        GwdProbe probe = new GwdProbe();
        long[] lands = new long[2];
        long[] mcs = new long[2];
        List<ArenaTiming.StrikeTrace> onTick = tracedGwd(() -> {
            align(probe, host);
            lands[0] = probe.fire(host, far, 2, 60, 5.0) - ActionManager.gameTick();
            mcs[0] = runUntilLanded(probe, probe::pending, host, 200);
        });
        expect(onTick.size() == 1, onTick.size() + " strikes landed (expected 1)");
        ArenaTiming.StrikeTrace t = onTick.get(0);
        expect(t.unfair() == null, "'Contain this!': " + t.unfair());
        expect(t.draws() >= 4, "the ring was drawn " + t.draws() + " times (on creation and at least 3 redraws expected)");
        expect(lands[0] == ArenaTiming.STRIKE_FLOOR_TICKS && mcs[0] >= ArenaTiming.MIN_LEAD_MC, "a 2-tick strike made on a game tick"
                + " is due " + lands[0] + " game ticks later and landed after " + mcs[0] + " Minecraft ticks");
        List<ArenaTiming.StrikeTrace> between = tracedGwd(() -> {
            align(probe, host);
            for (int i = 0; i < 5; i++) {
                mc(probe, host);
            }
            lands[1] = probe.fire(host, far, 3, 20, 1.5);
            mcs[1] = runUntilLanded(probe, probe::pending, host, 200);
        });
        expect(between.size() == 1 && between.get(0).unfair() == null, "a strike made between game ticks: "
                + (between.isEmpty() ? "never landed" : between.get(0).unfair()));
        expect(mcs[1] >= ArenaTiming.MIN_LEAD_MC, "a strike made 5 MC into a game tick landed " + mcs[1] + " MC later");
        expect(lands[1] * MC == between.get(0).landedMc(), "it landed on game tick " + between.get(0).landedMc() / MC
                + ", not the " + lands[1] + " its maker was told");
        return "'Contain this!' landed after " + mcs[0] + " MC (drawn " + t.draws() + " times); made mid-tick, after " + mcs[1] + " MC";
    }

    /** RaidEffects: a damaging 1-tick raid strike gets the same floor and is drawn as it is made. */
    private String raidStrikeFloor() {
        return RaidSelfTest.arenaStrikeCheck(c.world, Vec3.atBottomCenterOf(c.near(0, 0)).add(0, 40, 0));
    }

    /**
     * A boss warning reaches each player once, on the telegraph line; two within 5 Minecraft ticks merge into one line; one
     * 11 later starts a new line. Colours follow the style named.
     */
    private String warnTelegraph() {
        expect(Msg.colourFor("Blue orb: magic!") == Msg.MAGIC_RGB && Msg.colourFor("Ranged attacks!") == Msg.RANGED_RGB
                && Msg.colourFor("Protect from Melee!") == Msg.MELEE_RGB && Msg.colourFor("The ground shakes!") != Msg.MELEE_RGB,
                "telegraph colours don't follow the style named");
        ServerPlayer p = c.player;
        List<String> lines = new ArrayList<>();
        int[] fresh = {0};
        Msg.TelegraphListener before = Msg.telegraphListener;
        Msg.telegraphListener = (player, line, merged) -> {
            before.shown(player, line, merged);
            if (player == p) {
                lines.add(line);
                if (!merged) {
                    fresh[0]++;
                }
            }
        };
        try {
            for (int i = 0; i < ArenaTiming.TELEGRAPH_MERGE_MC + 1; i++) {
                ActionManager.advanceForSelfTest(); // nothing merges with an earlier check's line
            }
            boolean viaBoss = c.realPlayer();
            DataBossEntity host = host(p.position().add(3, 0, 0));
            Runnable one = () -> warn(viaBoss, host, p, "Arena check one!");
            one.run();
            expect(lines.size() == 1 && fresh[0] == 1, "one warning showed " + lines.size() + " line(s) to the player");
            for (int i = 0; i < 5; i++) {
                ActionManager.advanceForSelfTest();
            }
            warn(viaBoss, host, p, "Arena check two!");
            expect(fresh[0] == 1 && lines.size() == 2, "a warning 5 MC after another started a new line (" + fresh[0] + " lines)");
            String merged = lines.get(1);
            expect(merged.contains("Arena check one!") && merged.contains("Arena check two!") && merged.contains(" · "),
                    "the merged line is '" + merged + "'");
            for (int i = 0; i < ArenaTiming.TELEGRAPH_MERGE_MC + 1; i++) {
                ActionManager.advanceForSelfTest();
            }
            warn(viaBoss, host, p, "Arena check three!");
            expect(fresh[0] == 2 && lines.get(2).equals("Arena check three!"), "a warning " + (ArenaTiming.TELEGRAPH_MERGE_MC + 1)
                    + " MC later did not start its own line ('" + lines.get(lines.size() - 1) + "')");
            return (viaBoss ? "BossFx.warn" : "Msg.telegraph (fake player: not in the boss's player list)")
                    + ": one line each, two 5 MC apart merged ('" + merged + "')";
        } finally {
            Msg.telegraphListener = before;
        }
    }

    private static void warn(boolean viaBoss, DataBossEntity host, ServerPlayer p, String text) {
        if (viaBoss) {
            ScriptedBoss.selfTestWarn(host, text);
        } else {
            Msg.telegraph(p, text, null);
        }
    }

    /**
     * The real player stands in a boss's ground strike: the hit reaches them (through the damage hooks) no sooner than 36
     * Minecraft ticks after the ring appeared, and the strike recorder saw it drawn on creation.
     */
    private String playabilityStrike() {
        if (!c.realPlayer()) {
            throw new SelfTest.Skip("needs the real player (strikes search the world's player list)");
        }
        ServerPlayer p = c.player;
        if (!hooked) {
            hooked = true;
            com.runecraft.combat.CombatHooks.addDamageListener((player, attacker, attack, style, damage) -> {
                if (player == watched && PROBE_ATTACK.equals(attack) && probeHitMc < 0) {
                    probeHitMc = ActionManager.mcTick();
                }
            });
        }
        Vec3 at = p.position();
        DataBossEntity host = host(at.add(6, 0, 0));
        Probe probe = new Probe();
        long[] firedMc = new long[1];
        watched = p;
        probeHitMc = -1;
        List<ArenaTiming.StrikeTrace> traces;
        try {
            traces = traced(() -> {
                align(probe, host);
                firedMc[0] = ActionManager.mcTick();
                probe.fire(host, at, 0, 5, 1.5, null);
                runUntilLanded(probe, host, 200);
            });
        } finally {
            watched = null;
            p.setHealth(p.getMaxHealth());
        }
        expect(probeHitMc >= 0, "the strike under the real player never reached the damage hooks");
        long lead = probeHitMc - firedMc[0];
        expect(lead >= ArenaTiming.MIN_LEAD_MC, "the strike hit the player " + lead + " MC after its ring appeared");
        expect(traces.size() == 1 && traces.get(0).unfair() == null && traces.get(0).draws() >= 4, "strike recorder: "
                + (traces.isEmpty() ? "nothing landed" : traces.get(0).unfair() + ", drawn " + traces.get(0).draws() + " times"));
        return "hit " + lead + " MC after the ring appeared, drawn " + traces.get(0).draws() + " times";
    }
}
