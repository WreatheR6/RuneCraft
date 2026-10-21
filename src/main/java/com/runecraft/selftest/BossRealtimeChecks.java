package com.runecraft.selftest;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.boss.FloorMarkers;
import com.runecraft.boss.HazardKind;
import com.runecraft.boss.Mechanics;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.PrayerService;
import com.runecraft.content.bosses.BossRealtimeHooks;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.ThreatTiming;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.bosses.BossRealtime;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The round 42 real-time boss patches played in real time ({@code boss:realtime_*}; they work with the real player too, and
 * read hits through the combat hooks and the delivery recorder, never health): the Whisperer's mixed volley lands each
 * projectile 20+ MC after the last and keeps her OSRS cycle, every bolt blocked by a prayer switched through the prayer key a few
 * MC after its cue; Cerberus shows her triple attack's order before it starts and her souls glow 36 MC before they attack, all
 * blocked the same way; Vardorvis' head pair lands 24 MC apart, both blocked; the Nightmare's and Phosani's wind-ups meet the
 * floor (Phosani's phase three waits longer and its attack, prayed only after the wind-up, is blocked); the ground hazards' and
 * marks' coloured floor blocks come and are put back.
 */
final class BossRealtimeChecks {
    /** A player reacts this many Minecraft ticks after a cue appears (0.3 s). */
    private static final int REACTION_MC = 6;

    /** A pending hit the delivery recorder saw, and how it ended. */
    private static final class Seen {
        final Mechanics.Threat threat;
        long resolvedMc = -1;
        boolean landed;
        Prayer prayerAtImpact;

        Seen(Mechanics.Threat threat) {
            this.threat = threat;
        }
    }

    private static boolean hooked;
    private static ServerPlayer watched;
    /** Damage rolls against the watched player by attack name: {total, rolls, hurt rolls}. */
    private static final Map<String, int[]> DAMAGE = new HashMap<>();

    private final TestContext c;
    private final List<Seen> seen = new ArrayList<>();

    private BossRealtimeChecks(TestContext c) {
        this.c = c;
    }

    static List<SelfTest.Check> all(TestContext context) {
        BossRealtimeChecks b = new BossRealtimeChecks(context);
        List<SelfTest.Check> checks = new ArrayList<>();
        checks.add(new SelfTest.Check("boss:realtime_whisperer_volley", b::whispererVolley));
        checks.add(new SelfTest.Check("boss:realtime_cerberus", b::cerberus));
        checks.add(new SelfTest.Check("boss:realtime_vardorvis_gaze", b::vardorvisGaze));
        checks.add(new SelfTest.Check("boss:realtime_nightmare_windup", b::nightmareWindUp));
        checks.add(new SelfTest.Check("boss:realtime_zulrah_jad", b::zulrahJad));
        checks.add(new SelfTest.Check("boss:realtime_floor_markers", b::floorMarkers));
        checks.add(new SelfTest.Check("boss:realtime_landed_attack", b::landedAttack));
        return checks;
    }

    private static void expect(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
    }

    // ------------------------------------------------------------------ harness

    private ServerPlayer p() {
        return c.player;
    }

    private DataBossEntity spawn(String id, int dx, int dz) {
        BossDef def = ContentRegistry.boss(id);
        if (def == null) {
            throw new SelfTest.Skip("no BossDef " + id);
        }
        DataBossEntity boss = DataBossService.spawn(c.world, c.near(dx, dz), def, p());
        expect(boss != null, "DataBossService.spawn returned null for " + id);
        boss.setTarget(p());
        return boss;
    }

    private void watch() {
        if (!hooked) {
            hooked = true;
            CombatHooks.addDamageListener((player, attacker, attack, style, damage) -> {
                if (player == watched) {
                    int[] t = DAMAGE.computeIfAbsent(attack == null ? "?" : attack, k -> new int[3]);
                    t[0] += Math.max(0, damage);
                    t[1]++;
                    if (damage > 0) {
                        t[2]++;
                    }
                }
            });
        }
        DAMAGE.clear();
        watched = p();
    }

    private static int[] damage(String attack) {
        return DAMAGE.getOrDefault(attack, new int[3]).clone();
    }

    /** Records every pending hit made while {@code body} runs (the boss's own and the check's). */
    private void recording(Runnable body) {
        Mechanics.PendingListener before = Mechanics.pendingListener;
        seen.clear();
        Mechanics.pendingListener = new Mechanics.PendingListener() {
            @Override
            public void created(Mechanics.Threat threat) {
                seen.add(new Seen(threat));
                before.created(threat);
            }

            @Override
            public void cue(Mechanics.Threat threat, long mc) {
                before.cue(threat, mc);
            }

            @Override
            public void resolved(Mechanics.Threat threat, long mc, boolean landed) {
                for (Seen s : seen) {
                    if (s.threat.id() == threat.id()) {
                        s.resolvedMc = mc;
                        s.landed = landed;
                        s.prayerAtImpact = activeOverhead();
                    }
                }
                before.resolved(threat, mc, landed);
            }
        };
        try {
            body.run();
        } finally {
            Mechanics.pendingListener = before;
        }
    }

    private Prayer activeOverhead() {
        PlayerData data = Players.data(p());
        for (Prayer pr : new Prayer[]{Prayer.PROTECT_FROM_MAGIC, Prayer.PROTECT_FROM_MISSILES, Prayer.PROTECT_FROM_MELEE}) {
            if (data.isActive(pr)) {
                return pr;
            }
        }
        return null;
    }

    static Prayer against(AttackType style) {
        return style == AttackType.MAGIC ? Prayer.PROTECT_FROM_MAGIC : style == AttackType.RANGED ? Prayer.PROTECT_FROM_MISSILES
                : Prayer.PROTECT_FROM_MELEE;
    }

    /** As a player would: pray (through the prayer key's path) against the next hit to land whose cue they've had time to read. */
    private void prayAgainstNext() {
        long now = ActionManager.mcTick();
        Seen next = null;
        for (Seen s : seen) {
            if (s.resolvedMc < 0 && s.threat.prayerCheckable() && now - s.threat.firstCueMc() >= REACTION_MC
                    && (next == null || s.threat.dueMc() < next.threat.dueMc())) {
                next = s;
            }
        }
        if (next != null) {
            pray(against(next.threat.style()));
        }
    }

    private void pray(Prayer pr) {
        PlayerData data = Players.data(p());
        data.setPrayerPoints(Math.max(data.prayerPoints(), 50)); // prayer upkeep isn't what these check
        if (!data.isActive(pr)) {
            PrayerService.set(p(), pr, true);
            expect(data.isActive(pr), "the prayer key refused " + pr + " (" + PrayerService.lastRefusal + ")");
        }
    }

    private void prayersOff() {
        PlayerData data = Players.data(p());
        for (Prayer pr : new Prayer[]{Prayer.PROTECT_FROM_MAGIC, Prayer.PROTECT_FROM_MISSILES, Prayer.PROTECT_FROM_MELEE}) {
            if (data.isActive(pr)) {
                PrayerService.set(p(), pr, false);
            }
        }
    }

    /** Minecraft ticks of a fight: the boss's own tick (its game ticks every 12), the delivery, then {@code each}. */
    private void mc(DataBossEntity boss, int n, Runnable each) {
        for (int i = 0; i < n; i++) {
            ActionManager.advanceForSelfTest();
            if (boss != null && boss.isAlive()) {
                if (boss.getTarget() != p()) {
                    boss.setTarget(p());
                }
                boss.tickCount++; // as ServerLevel.tickNonPassenger does (see BossFightChecks)
                boss.tick();
            }
            Mechanics.tickForSelfTest();
            if (each != null) {
                each.run();
            }
            p().setHealth(p().getMaxHealth());
        }
    }

    private void finish(DataBossEntity boss) {
        prayersOff();
        if (boss != null && boss.isAlive()) {
            boss.discard();
        }
        // let what is still in the air land (or fizzle) before the next check
        for (int i = 0; i < 60 && Mechanics.pendingOn(p()) > 0; i++) {
            ActionManager.advanceForSelfTest();
            Mechanics.tickForSelfTest();
        }
        watched = null;
        c.heal();
    }

    // ------------------------------------------------------------------ the Whisperer

    /**
     * The awakened Whisperer's alternating volley (magic, ranged, magic, ranged, magic): each projectile lands 20+ MC after the one
     * before it (OSRS one tick, a flick), her next volley starts 9 game ticks after the first projectile (OSRS 4 + 5), and a player
     * praying against each one REACTION_MC after it appears blocks all five.
     */
    private String whispererVolley() {
        DataBossEntity boss = spawn("the_whisperer", 0, -8);
        boss.applyVariant(com.runecraft.osrs.content.bosses.AwakenedBosses.variant(boss.def()));
        watch();
        boolean[] volley = {true, false, true, false, true};
        BossRealtimeHooks.whispererVolley(boss, volley);
        try {
            recording(() -> {
                for (int i = 0; i < 260; i++) {
                    mc(boss, 1, this::prayAgainstNext);
                    long projectiles = seen.stream().filter(s -> s.threat.kind() == Mechanics.Kind.PROJECTILE).count();
                    if (projectiles >= 6 && seen.get(4).resolvedMc >= 0) {
                        break;
                    }
                }
            });
            List<Seen> bolts = new ArrayList<>();
            for (Seen s : seen) {
                if (s.threat.kind() == Mechanics.Kind.PROJECTILE) {
                    bolts.add(s);
                }
            }
            expect(bolts.size() >= 6, "only " + bolts.size() + " projectiles in 260 MC (a volley of 5 and the next one's first)");
            for (int i = 0; i < 5; i++) {
                AttackType want = volley[i] ? AttackType.MAGIC : AttackType.RANGED;
                expect(bolts.get(i).threat.style() == want, "projectile " + (i + 1) + " was " + bolts.get(i).threat.style());
                expect(bolts.get(i).landed, "projectile " + (i + 1) + " fizzled (the player stood still in the open)");
                expect(bolts.get(i).prayerAtImpact == against(want), "projectile " + (i + 1) + " landed with "
                        + bolts.get(i).prayerAtImpact + " up: the switch after its cue didn't make it");
                if (i > 0) {
                    long apart = bolts.get(i).threat.dueMc() - bolts.get(i - 1).threat.dueMc();
                    expect(apart >= ThreatTiming.STYLE_SPACING_MC, "projectile " + (i + 1) + " landed " + apart + " MC after the one before");
                }
            }
            long cycle = bolts.get(5).threat.createdMc() - bolts.get(0).threat.createdMc();
            long osrs = (long) (volley.length - 1 + boss.def().attackSpeed) * ActionManager.MC_TICKS_PER_GAME_TICK;
            expect(cycle == osrs, "her next volley came " + cycle + " MC after the first projectile (OSRS cycle " + osrs + ")");
            int[] magic = damage(com.runecraft.osrs.content.bosses.Dt2Bosses.WHISPER_MAGIC.name());
            int[] ranged = damage(com.runecraft.osrs.content.bosses.Dt2Bosses.WHISPER_RANGED.name());
            expect(magic[2] + ranged[2] == 0, "prayed-against projectiles hurt (" + magic[0] + " magic, " + ranged[0] + " ranged)");
            return "5 alternating projectiles " + (bolts.get(1).threat.dueMc() - bolts.get(0).threat.dueMc()) + "+ MC apart, all blocked by"
                    + " prayers switched " + REACTION_MC + " MC after each cue; cycle " + cycle / 12 + " ticks as in OSRS";
        } finally {
            finish(boss);
        }
    }

    // ------------------------------------------------------------------ Cerberus

    /**
     * Cerberus: her opening triple attack's order shows before it (36 MC before the first attack), its three hits land 20+ MC apart
     * and are all blocked by switching after each cue; her souls glow 36 MC before each attacks, 24 MC apart, all drained by the
     * prayer switched after each cue (no soul damage).
     */
    private String cerberus() {
        DataBossEntity boss = spawn("cerberus", 0, 6);
        watch();
        StringBuilder out = new StringBuilder();
        long[] cueMc = {-1};
        try {
            recording(() -> {
                for (int i = 0; i < 24 && cueMc[0] < 0; i++) {
                    mc(boss, 1, null);
                    if (BossRealtimeHooks.cerberusTripleShown(boss).size() == 3) {
                        cueMc[0] = ActionManager.mcTick();
                    }
                }
                expect(cueMc[0] >= 0, "her opening triple attack's order never showed");
                expect(BossRealtimeHooks.cerberusTripleShown(boss).equals(List.of(AttackType.MAGIC, AttackType.RANGED, AttackType.STAB)),
                        "the order shown was " + BossRealtimeHooks.cerberusTripleShown(boss));
                expect(seen.isEmpty(), "she attacked as the order showed");
                for (int i = 0; i < 200; i++) {
                    mc(boss, 1, this::prayAgainstNext);
                    if (seen.size() >= 3 && seen.get(2).resolvedMc >= 0) {
                        break;
                    }
                }
            });
            expect(seen.size() >= 3, "only " + seen.size() + " hits of her triple attack");
            AttackType[] order = {AttackType.MAGIC, AttackType.RANGED, AttackType.STAB};
            expect(seen.get(0).threat.createdMc() - cueMc[0] >= BossRealtime.CUE_FLOOR_MC,
                    "the triple attack started " + (seen.get(0).threat.createdMc() - cueMc[0]) + " MC after its order showed");
            for (int i = 0; i < 3; i++) {
                Seen s = seen.get(i);
                expect(s.threat.style() == order[i], "hit " + (i + 1) + " of the triple was " + s.threat.style());
                expect(s.landed && s.prayerAtImpact == against(order[i]), "hit " + (i + 1) + " landed " + s.landed + " with "
                        + s.prayerAtImpact);
                if (i > 0) {
                    long apart = s.threat.dueMc() - seen.get(i - 1).threat.dueMc();
                    expect(apart >= ThreatTiming.STYLE_SPACING_MC, "hit " + (i + 1) + " landed " + apart + " MC after the one before");
                }
            }
            int hurt = damage("soul_blast")[2] + damage("fireball")[2] + damage("bite")[2];
            expect(hurt == 0, hurt + " hits of the prayed-against triple attack hurt");
            out.append("triple attack shown 36+ MC ahead, its hits 20+ MC apart, all blocked");
            // her souls: each cued 36 MC before it attacks, the player switching after each cue
            BossRealtimeHooks.hold(boss, 40);
            prayersOff();
            long[] cues = new long[3];
            long[] attacks = new long[3];
            int[] styles = new int[3];
            java.util.Arrays.fill(cues, -1);
            java.util.Arrays.fill(attacks, -1);
            BossRealtimeHooks.cerberusSouls(boss, (place, style, attack, mc) -> {
                styles[place] = style;
                if (attack) {
                    attacks[place] = mc;
                } else {
                    cues[place] = mc;
                }
            });
            PlayerData data = Players.data(p());
            data.setPrayerPoints(99);
            Prayer[] soulPrayers = {Prayer.PROTECT_FROM_MISSILES, Prayer.PROTECT_FROM_MAGIC, Prayer.PROTECT_FROM_MELEE};
            try {
                for (int i = 0; i < 14 * ActionManager.MC_TICKS_PER_GAME_TICK && attacks[2] < 0; i++) {
                    mc(boss, 1, () -> {
                        long now = ActionManager.mcTick();
                        for (int k = 0; k < 3; k++) {
                            if (attacks[k] < 0) {
                                if (cues[k] >= 0 && now - cues[k] >= REACTION_MC) {
                                    PlayerData d = Players.data(p());
                                    d.setPrayerPoints(Math.max(d.prayerPoints(), 50));
                                    if (!d.isActive(soulPrayers[styles[k]])) {
                                        PrayerService.set(p(), soulPrayers[styles[k]], true);
                                    }
                                }
                                break; // the next soul to attack
                            }
                        }
                    });
                }
            } finally {
                BossRealtimeHooks.cerberusSoulsDone();
            }
            for (int k = 0; k < 3; k++) {
                expect(cues[k] >= 0 && attacks[k] >= 0, "soul " + (k + 1) + " cued at " + cues[k] + ", attacked at " + attacks[k]
                        + " (did it spawn on the platform?)");
                expect(attacks[k] - cues[k] >= BossRealtime.CUE_FLOOR_MC, "soul " + (k + 1) + " attacked " + (attacks[k] - cues[k])
                        + " MC after its cue");
                if (k > 0) {
                    expect(attacks[k] - attacks[k - 1] == (long) BossRealtime.CERB_SOUL_GAP_TICKS * ActionManager.MC_TICKS_PER_GAME_TICK,
                            "souls " + k + " and " + (k + 1) + " attacked " + (attacks[k] - attacks[k - 1]) + " MC apart (OSRS 2 ticks)");
                }
            }
            int[] souls = damage("summoned_soul");
            expect(souls[1] == 0, souls[1] + " souls struck through the prayer switched after their cue");
            out.append("; 3 souls cued 36 MC ahead, 24 MC apart, every one prayed against");
            return out.toString();
        } finally {
            finish(boss);
        }
    }

    // ------------------------------------------------------------------ Vardorvis

    /** The awakened Vardorvis' head pair (below half): the two styles land 24 MC apart, both blocked; no prayer drained. */
    private String vardorvisGaze() {
        DataBossEntity boss = spawn("vardorvis", 0, -5);
        boss.applyVariant(com.runecraft.osrs.content.bosses.AwakenedBosses.variant(boss.def()));
        try {
            mc(boss, ActionManager.MC_TICKS_PER_GAME_TICK, null); // its home and first tick
            boss.setHealth(boss.getMaxHealth() * 0.4f);
            BossRealtimeHooks.hold(boss, 12);
            BossRealtimeHooks.vardorvisGaze(boss, p());
            List<long[]> gazes = BossRealtimeHooks.vardorvisGazes(boss);
            expect(gazes.size() == 2, gazes.size() + " gazes below half (awakened: a pair)");
            long apart = gazes.get(1)[0] - gazes.get(0)[0];
            expect(gazes.get(0)[1] != gazes.get(1)[1], "the pair was one style");
            expect(apart >= (long) BossRealtime.VARDORVIS_GAZE_SWITCH_GAP_TICKS * ActionManager.MC_TICKS_PER_GAME_TICK,
                    "the pair lands " + apart + " MC apart");
            expect(gazes.get(0)[0] - gazes.get(0)[2] >= BossRealtime.CUE_FLOOR_MC, "the first gaze flies "
                    + (gazes.get(0)[0] - gazes.get(0)[2]) + " MC");
            PlayerData data = Players.data(p());
            data.setPrayerPoints(99);
            int points = data.prayerPoints();
            for (int i = 0; i < 8 * ActionManager.MC_TICKS_PER_GAME_TICK && !BossRealtimeHooks.vardorvisGazes(boss).isEmpty(); i++) {
                mc(boss, 1, () -> {
                    long now = ActionManager.mcTick();
                    List<long[]> left = BossRealtimeHooks.vardorvisGazes(boss);
                    if (!left.isEmpty() && now - left.get(0)[2] >= REACTION_MC) {
                        pray(left.get(0)[1] == 1 ? Prayer.PROTECT_FROM_MAGIC : Prayer.PROTECT_FROM_MISSILES);
                    }
                });
            }
            expect(BossRealtimeHooks.vardorvisGazes(boss).isEmpty(), "the gazes never landed");
            expect(data.prayerPoints() >= points - 1, "a prayed-against gaze drained prayer (" + points + " -> " + data.prayerPoints() + ")");
            return "the pair landed " + apart + " MC apart, both blocked by switching after the first";
        } finally {
            finish(boss);
        }
    }

    // ------------------------------------------------------------------ the Nightmare and Phosani

    /**
     * Her wind-ups: every standard attack lands 36+ MC after its cue (OSRS melee 2 ticks); Phosani's phase three waits 3 (melee) and
     * 4 (magic, ranged) ticks, and her magic attack, prayed against only after its wind-up shows, is blocked.
     */
    private String nightmareWindUp() {
        List<long[]> windUps = new ArrayList<>();
        BossRealtimeHooks.nightmareWindUps((attack, created, due) -> windUps.add(new long[]{created, due, attack.style().isMelee() ? 1 : 0}));
        StringBuilder out = new StringBuilder();
        try {
            DataBossEntity boss = spawn("the_nightmare", 0, -4);
            try {
                BossRealtimeHooks.nightmareWake(boss, 0);
                BossRealtimeHooks.hold(boss, 40);
                for (int style = 0; style < 3; style++) {
                    BossRealtimeHooks.nightmareStandard(boss, p(), style);
                }
            } finally {
                boss.discard();
            }
            expect(windUps.size() == 3, windUps.size() + " wind-ups for 3 standard attacks");
            for (long[] w : windUps) {
                expect(w[1] - w[0] >= BossRealtime.CUE_FLOOR_MC, (w[2] == 1 ? "melee" : "ranged/magic") + " wound up " + (w[1] - w[0]) + " MC");
            }
            out.append("the Nightmare: 3 standard attacks wound up 36+ MC");
            windUps.clear();
            DataBossEntity phosani = spawn("phosanis_nightmare", 0, -4);
            watch();
            prayersOff();
            try {
                BossRealtimeHooks.nightmareWake(phosani, 2);
                BossRealtimeHooks.hold(phosani, 40);
                mc(phosani, ActionManager.MC_TICKS_PER_GAME_TICK, null);
                AttackDef blast = BossRealtimeHooks.nightmareStandard(phosani, p(), 1);
                expect(windUps.size() == 1, "no wind-up for her phase-three magic attack");
                long created = windUps.get(0)[0];
                long due = windUps.get(0)[1];
                expect(due - created == (long) BossRealtime.PHOSANI_P3_RANGED_WIND_UP * ActionManager.MC_TICKS_PER_GAME_TICK,
                        "her phase-three magic attack wound up " + (due - created) + " MC (4 ticks)");
                for (int i = 0; i < (due - created) + 2 * ActionManager.MC_TICKS_PER_GAME_TICK; i++) {
                    mc(phosani, 1, () -> {
                        if (ActionManager.mcTick() - created >= REACTION_MC) {
                            pray(Prayer.PROTECT_FROM_MAGIC); // only after the wind-up shows, as her phase three demands
                        }
                    });
                }
                int[] hits = damage(blast.name());
                expect(hits[1] >= 1, "her phase-three magic attack never landed on the player");
                expect(hits[2] == 0, "her magic attack hurt through Protect from Magic switched on after its wind-up (" + hits[0] + ")");
                BossRealtimeHooks.nightmareStandard(phosani, p(), 0);
                long melee = windUps.get(windUps.size() - 1)[1] - windUps.get(windUps.size() - 1)[0];
                expect(melee == (long) BossRealtime.PHOSANI_P3_MELEE_WIND_UP * ActionManager.MC_TICKS_PER_GAME_TICK,
                        "her phase-three melee wound up " + melee + " MC (3 ticks)");
            } finally {
                finish(phosani);
            }
            out.append("; Phosani's phase three: magic 48 MC, melee 36 MC, her magic blocked by praying after the wind-up");
            return out.toString();
        } finally {
            BossRealtimeHooks.nightmareWindUps(null);
        }
    }

    // ------------------------------------------------------------------ Zulrah

    /** Zulrah's Jad-phase attacks are telegraphs cued 36 MC ahead in their style's colour, redrawn, and blocked when prayed. */
    private String zulrahJad() {
        DataBossEntity boss = spawn("zulrah", 0, -8);
        watch();
        try {
            int[] cues = {0};
            Mechanics.PendingListener before = Mechanics.pendingListener;
            recording(() -> {
                Mechanics.PendingListener rec = Mechanics.pendingListener;
                Mechanics.pendingListener = new Mechanics.PendingListener() {
                    @Override
                    public void created(Mechanics.Threat threat) {
                        rec.created(threat);
                    }

                    @Override
                    public void cue(Mechanics.Threat threat, long mc) {
                        cues[0]++;
                        rec.cue(threat, mc);
                    }

                    @Override
                    public void resolved(Mechanics.Threat threat, long mc, boolean landed) {
                        rec.resolved(threat, mc, landed);
                    }
                };
                try {
                    for (AttackDef a : BossRealtimeHooks.zulrahJadAttacks()) {
                        expect(com.runecraft.content.bosses.BossSelfTests.performScripted(boss, p(), a), "Zulrah is not a scripted boss");
                        for (int i = 0; i < 80 && seen.get(seen.size() - 1).resolvedMc < 0; i++) {
                            mc(null, 1, this::prayAgainstNext);
                        }
                    }
                } finally {
                    Mechanics.pendingListener = rec;
                }
            });
            Mechanics.pendingListener = before;
            expect(seen.size() == 2, seen.size() + " Jad-phase attacks delivered");
            for (Seen s : seen) {
                expect(s.threat.kind() == Mechanics.Kind.TELEGRAPH, "a Jad-phase attack came as a " + s.threat.kind());
                expect(s.threat.dueMc() - s.threat.firstCueMc() >= BossRealtime.CUE_FLOOR_MC, "a Jad-phase attack was cued "
                        + (s.threat.dueMc() - s.threat.firstCueMc()) + " MC ahead");
                expect(s.landed && s.prayerAtImpact == against(s.threat.style()), "a Jad-phase attack landed " + s.landed + " with "
                        + s.prayerAtImpact);
            }
            expect(cues[0] >= 16, "the two cues were drawn " + cues[0] + " times (redrawn every 4 MC over 36)");
            int hurt = damage("venom_bolt")[2] + damage("venom_spit")[2];
            expect(hurt == 0, hurt + " prayed-against Jad-phase attacks hurt");
            return "magic and ranged cued 36 MC ahead (" + cues[0] + " redraws), both blocked";
        } finally {
            finish(boss);
        }
    }

    // ------------------------------------------------------------------ the attack that landed

    /**
     * Hits land in real time, after a boss may have started its next attack: the Corporeal Beast's draining orb heals it when it
     * lands although its melee has started meanwhile (and a plain orb landing while it makes a draining one doesn't); Elvarg's
     * breath drains prayer by the breath that landed; the Fragment of Seren's special comes every fourth attack she makes, however
     * often her attack goal asks for her next one.
     */
    private String landedAttack() {
        StringBuilder out = new StringBuilder();
        DataBossEntity corp = spawn("corporeal_beast", 0, -6);
        try {
            com.runecraft.boss.BossScript s = corp.script();
            corp.setHealth(corp.getMaxHealth() / 2);
            float hp = corp.getHealth();
            s.beforeAttack(corp, p(), com.runecraft.osrs.content.bosses.ClassicBosses.CORP_MELEE); // its melee starts
            s.onHitPlayer(corp, p(), 20, AttackType.MAGIC, com.runecraft.osrs.content.bosses.ClassicBosses.CORP_DRAIN);
            expect(corp.getHealth() > hp, "its draining orb landing after its melee started didn't heal it (" + hp + " -> "
                    + corp.getHealth() + ")");
            float healed = corp.getHealth();
            s.beforeAttack(corp, p(), com.runecraft.osrs.content.bosses.ClassicBosses.CORP_DRAIN); // a draining orb starts
            s.onHitPlayer(corp, p(), 20, AttackType.MAGIC, com.runecraft.osrs.content.bosses.ClassicBosses.CORP_ORB);
            expect(corp.getHealth() == healed, "a plain orb landing while a draining one started healed it");
            out.append("Corp's drain read from the orb that landed");
        } finally {
            corp.discard();
        }
        DataBossEntity elvarg = spawn("elvarg", 0, -6);
        try {
            AttackDef breath = null;
            AttackDef other = null;
            for (AttackDef a : elvarg.def().attacks) {
                if (a.special() == AttackDef.Special.DRAGONFIRE) {
                    breath = a;
                } else {
                    other = a;
                }
            }
            if (breath != null && other != null) {
                com.runecraft.boss.BossScript s = elvarg.script();
                PlayerData data = Players.data(p());
                data.setPrayerPoints(99); // capped at the tester's Prayer level
                int full = data.prayerPoints();
                int want = full - full * com.runecraft.osrs.content.quests.QuestBossRules.ELVARG_PRAYER_DRAIN_PERCENT / 100;
                expect(want < full, "the tester has too little Prayer (" + full + ") for a tenth to show");
                s.beforeAttack(elvarg, p(), other);
                s.onHitPlayer(elvarg, p(), 5, breath.style(), breath);
                expect(data.prayerPoints() == want, "her breath landing after another attack started left " + data.prayerPoints()
                        + " of " + full + " prayer (a tenth off: " + want + ")");
                s.beforeAttack(elvarg, p(), breath);
                s.onHitPlayer(elvarg, p(), 5, other.style(), other);
                expect(data.prayerPoints() == want, "another attack landing while her breath started drained prayer");
                out.append("; Elvarg's prayer drain read from the breath that landed");
            }
        } finally {
            elvarg.discard();
        }
        DataBossEntity fragment = spawn("fragment_of_seren", 0, -6);
        try {
            com.runecraft.boss.BossScript s = fragment.script();
            java.util.Set<String> specials = java.util.Set.of("pull", "shadows", "healing_beams", "crystal_judgement");
            for (int i = 0; i < 40; i++) {
                AttackDef a = s.chooseAttack(fragment, p(), 8);
                expect(a != null && !specials.contains(a.name()), "the goal asking for her next attack " + (i + 1) + " times made "
                        + (a == null ? "nothing" : a.name()));
            }
            List<String> made = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                AttackDef a = s.chooseAttack(fragment, p(), 8);
                s.beforeAttack(fragment, p(), a);
                made.add(a.name());
            }
            expect(made.get(3).equals("pull") && made.get(7).equals("shadows")
                    && made.stream().filter(specials::contains).count() == 2, "her first 8 attacks: " + made);
            out.append("; the Fragment's specials on her 4th and 8th attacks");
        } finally {
            fragment.discard();
            c.heal();
        }
        return out.toString();
    }

    // ------------------------------------------------------------------ floor markers

    /**
     * Coloured floor blocks: an acid hazard's lime block comes with it and the floor is put back when it expires; overlapping marks
     * share the block until the last goes; a block entity is never marked; the Muspah's spike marks (light blue, then white) go
     * with it; the Moons' lit glyph is yellow and goes with her.
     */
    private String floorMarkers() {
        // off the Moon's glyph ring (radius 4 round the platform centre) checked below
        BlockPos floor = c.near(6, 6).below();
        BlockState original = c.world.getBlockState(floor);
        Vec3 at = Vec3.atBottomCenterOf(c.near(6, 6));
        int before = FloorMarkers.live();
        Mechanics.addHazard(c.world, at, 3, 2, null, HazardKind.ACID);
        expect(c.world.getBlockState(floor).is(Blocks.LIME_CONCRETE), "the acid hazard's floor isn't lime ("
                + c.world.getBlockState(floor) + ")");
        Mechanics.addHazard(c.world, at, 3, 5, null, HazardKind.FIRE); // overlaps: the block stays lime until both are gone
        for (int i = 0; i < 4 * ActionManager.MC_TICKS_PER_GAME_TICK; i++) {
            ActionManager.advanceForSelfTest();
            Mechanics.tickForSelfTest();
        }
        expect(c.world.getBlockState(floor).is(Blocks.LIME_CONCRETE), "the floor came back while a second hazard still lay on it");
        for (int i = 0; i < 4 * ActionManager.MC_TICKS_PER_GAME_TICK; i++) {
            ActionManager.advanceForSelfTest();
            Mechanics.tickForSelfTest();
        }
        expect(c.world.getBlockState(floor) == original, "the floor wasn't put back after the hazards (" + c.world.getBlockState(floor) + ")");
        expect(FloorMarkers.live() == before, "marks left behind: " + (FloorMarkers.live() - before));
        // a block entity is never swapped
        BlockPos chest = c.place(c.near(-6, 6).below(), Blocks.CHEST.defaultBlockState());
        Mechanics.addHazard(c.world, Vec3.atBottomCenterOf(c.near(-6, 6)), 3, 1, null, HazardKind.POISON);
        expect(c.world.getBlockState(chest).is(Blocks.CHEST), "a chest was swapped for a floor mark");
        StringBuilder out = new StringBuilder("hazard floor marked lime, shared, put back; chests left alone");
        // the Muspah's spikes
        DataBossEntity muspah = spawn("phantom_muspah", 0, -6);
        try {
            mc(muspah, ActionManager.MC_TICKS_PER_GAME_TICK, null);
            BossRealtimeHooks.hold(muspah, 40);
            BlockPos under = p().blockPosition().below();
            int tx = p().getBlockX();
            int tz = p().getBlockZ(); // the spike knocks the player aside when it erupts
            BlockState was = c.world.getBlockState(under);
            BossRealtimeHooks.muspahSpikeUnder(muspah, p());
            expect(c.world.getBlockState(under).is(Blocks.LIGHT_BLUE_CONCRETE), "the spike about to erupt isn't marked light blue");
            mc(muspah, (BossRealtime.muspahSpikeEruptTicks() - 1) * ActionManager.MC_TICKS_PER_GAME_TICK, null);
            expect(!BossRealtimeHooks.muspahSpike(muspah, tx, tz), "the spike erupted before 3 ticks");
            mc(muspah, ActionManager.MC_TICKS_PER_GAME_TICK, null);
            expect(BossRealtimeHooks.muspahSpike(muspah, tx, tz), "the spike never erupted");
            expect(c.world.getBlockState(under).is(Blocks.WHITE_CONCRETE), "the standing spike isn't marked white");
            muspah.discard();
            FloorMarkers.sweepOwners();
            expect(c.world.getBlockState(under) == was, "the spike's floor wasn't put back with the Muspah gone");
            out.append("; Muspah spike light blue then white, put back");
        } finally {
            if (muspah.isAlive()) {
                muspah.discard();
            }
        }
        DataBossEntity moon = spawn("blue_moon", 0, 0);
        try {
            mc(moon, ActionManager.MC_TICKS_PER_GAME_TICK, null);
            Vec3 glyph = BossRealtimeHooks.moonGlyph(moon);
            expect(glyph != null, "no lit glyph");
            BlockPos g = BlockPos.containing(glyph.x, glyph.y - 0.5, glyph.z);
            expect(c.world.getBlockState(g).is(Blocks.YELLOW_CONCRETE), "the lit glyph isn't marked yellow (" + c.world.getBlockState(g) + ")");
            moon.discard();
            FloorMarkers.sweepOwners();
            expect(!c.world.getBlockState(g).is(Blocks.YELLOW_CONCRETE), "the glyph's floor wasn't put back with the Moon gone");
            out.append("; the Moon's lit glyph yellow, put back");
        } finally {
            if (moon.isAlive()) {
                moon.discard();
            }
            finish(null);
        }
        expect(FloorMarkers.live() == before, "marks left behind after the bosses: " + (FloorMarkers.live() - before));
        return out.toString();
    }
}
