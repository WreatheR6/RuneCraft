package com.runecraft.content.systems;

import com.runecraft.combat.MonsterStats;
import com.runecraft.content.raids.RaidsState;
import com.runecraft.osrs.content.dungeons.Dungeons;
import com.runecraft.osrs.content.raids.ToaLoot;
import com.runecraft.osrs.content.systems.CombatAchievementRules;
import com.runecraft.osrs.content.systems.CombatAchievementRules.Metrics;
import com.runecraft.osrs.content.systems.CombatAchievements;
import com.runecraft.osrs.content.systems.CombatAchievements.Track;
import com.runecraft.osrs.world.StructureDef;
import com.runecraft.registry.ModEntities;
import com.runecraft.selftest.SelfTest;
import com.runecraft.selftest.TestContext;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Round 39 self-test of the Combat Achievement tasks tracked in the sweep, one per content area, through the service's
 * own paths: each check meets a task's condition and expects it completed, and first misses it and expects it open.
 * Inferno (Playing with Jads on its wave-68 checkpoint), Fight Caves (only melee weapons held), Tombs of Amascut (perfect
 * rooms at raid level 150+), raid kill counts read from the completion counts (ToB without Entry mode, CoX with
 * Challenge Mode, ToA by mode), thralls (100 damage) and the Giants' Den collection.
 */
public final class CombatAchievementSelfTest {
    private CombatAchievementSelfTest() {
    }

    public static List<SelfTest.Check> checks(TestContext c) {
        List<SelfTest.Check> checks = new ArrayList<>();
        checks.add(new SelfTest.Check("ca:inferno_jads_checkpoint", () -> infernoJads(c)));
        checks.add(new SelfTest.Check("ca:fight_caves_melee_only", () -> fightCavesMelee(c)));
        checks.add(new SelfTest.Check("ca:toa_perfect_rooms", () -> toaPerfectRooms(c)));
        checks.add(new SelfTest.Check("ca:raid_completion_counts", () -> raidCounts(c)));
        checks.add(new SelfTest.Check("ca:thrall_damage", () -> thralls(c)));
        checks.add(new SelfTest.Check("ca:giants_den", () -> giantsDen(c)));
        return checks;
    }

    private static boolean done(ServerPlayer p, int wikiId) {
        return SysState.of(p).caDone.contains("ca_" + wikiId);
    }

    private static void expect(boolean ok, String what) {
        if (!ok) {
            throw new AssertionError(what);
        }
    }

    /** Playing with Jads completes the moment wave 68 is cleared in time, not at the end of the run; other flags don't. */
    private static String infernoJads(TestContext c) {
        ServerPlayer p = c.player;
        CombatAchievementService.selfTestStartRun(p, "mg:inferno");
        try {
            CombatAchievementService.flag(p, CombatAchievements.FLAG_PILLAR_EARLY);
            expect(!done(p, 344), "Playing with Jads completed without wave 68 cleared in time");
            CombatAchievementService.flag(p, CombatAchievements.FLAG_JADS_QUICK);
            expect(done(p, 344), "Playing with Jads not completed when wave 68 was cleared within 30 seconds");
            expect(!done(p, 343) && !done(p, 346), "run-end tasks were judged at the wave-68 checkpoint");
        } finally {
            CombatAchievementService.selfTestEndRun(p, false);
        }
        return "344 on the checkpoint";
    }

    /** Facing Jad Head-on: a run with only a melee weapon held completes; holding a bow at any point fails it. */
    private static String fightCavesMelee(TestContext c) {
        ServerPlayer p = c.player;
        ItemStack before = p.getItemBySlot(EquipmentSlot.MAINHAND).copy();
        try {
            hold(p, "rune_crossbow");
            CombatAchievementService.selfTestStartRun(p, "mg:fight_caves");
            hold(p, "abyssal_whip");
            CombatAchievementService.selfTestMeasure(p);
            CombatAchievementService.selfTestEndRun(p, true);
            expect(!done(p, 153), "Facing Jad Head-on completed after holding a crossbow");
            hold(p, "abyssal_whip");
            CombatAchievementService.selfTestStartRun(p, "mg:fight_caves");
            CombatAchievementService.selfTestMeasure(p);
            CombatAchievementService.selfTestEndRun(p, true);
            expect(done(p, 153), "Facing Jad Head-on not completed with only a whip held");
        } finally {
            CombatAchievementService.selfTestEndRun(p, false);
            p.setItemSlot(EquipmentSlot.MAINHAND, before);
        }
        return "153 by the weapons held";
    }

    private static void hold(ServerPlayer p, String id) {
        ItemStack st = Inv.stack(id, 1);
        expect(!st.isEmpty(), "no item " + id);
        p.setItemSlot(EquipmentSlot.MAINHAND, st);
    }

    /** Perfect Het needs raid level 150; Perfect Akkha needs a group with every Akkha invocation. */
    private static String toaPerfectRooms(TestContext c) {
        ServerPlayer p = c.player;
        Metrics low = new Metrics();
        low.flags.add(ToaLoot.FLAG_PERFECT_HET);
        low.raid = new CombatAchievementRules.Raid("TOA", "NORMAL", 1, 100, Set.of(), 0, 0, 0, false, 0, false);
        CombatAchievementService.judge(p, Track.RAID, "TOA", low, true);
        expect(!done(p, 432), "Perfect Het completed at raid level 100");
        Metrics m = new Metrics();
        m.flags.add(ToaLoot.FLAG_PERFECT_HET);
        m.flags.add(ToaLoot.FLAG_PERFECT_AKKHA);
        m.raid = new CombatAchievementRules.Raid("TOA", "NORMAL", 1, 150, Set.of(), 0, 0, 0, false, 0, false);
        CombatAchievementService.judge(p, Track.RAID, "TOA", m, true);
        expect(done(p, 432), "Perfect Het not completed at raid level 150");
        expect(!done(p, 433), "Perfect Akkha completed solo without its invocations");
        expect(!done(p, 434), "Perfection of Het completed below Expert mode");
        Metrics group = new Metrics();
        group.flags.add(ToaLoot.FLAG_PERFECT_AKKHA);
        group.raid = new CombatAchievementRules.Raid("TOA", "EXPERT", 2, 300, Set.of(CombatAchievementRules.AKKHA.split(",")), 0, 0, 0,
                false, 0, false);
        CombatAchievementService.judge(p, Track.RAID, "TOA", group, true);
        expect(done(p, 433), "Perfect Akkha not completed in a group with every Akkha invocation");
        return "432, 433";
    }

    /** Raid kill count tasks read the saved completion counts: ToB without Entry mode, CoX with CM, ToA by mode. */
    private static String raidCounts(TestContext c) {
        ServerPlayer p = c.player;
        Map<String, Integer> completions = RaidsState.get(c.server).stats(p.getUUID()).completions;
        Map<String, Integer> saved = new HashMap<>(completions);
        try {
            completions.clear();
            completions.put("theatre_of_blood", 25);
            completions.put("tob_entry", 10);
            completions.put("chambers_of_xeric", 20);
            completions.put("cox_cm", 4);
            completions.put("tombs_of_amascut", 1);
            completions.put("tombs_of_amascut_entry", 1);
            CombatAchievementService.checkKillCounts(p);
            expect(!done(p, 237), "Theatre of Blood Veteran counted Entry mode runs");
            expect(done(p, 397), "Theatre of Blood: SM Adept not completed after an Entry mode run");
            expect(!done(p, 299), "Chambers of Xeric Veteran completed at 24 completions");
            expect(done(p, 461), "Novice Tomb Explorer not completed after an Entry mode run");
            expect(!done(p, 468), "Tomb Explorer completed by an Entry mode run");
            completions.put("tob_entry", 0);
            completions.put("cox_cm", 5);
            CombatAchievementService.checkKillCounts(p);
            expect(done(p, 237), "Theatre of Blood Veteran not completed at 25 Normal mode runs");
            expect(done(p, 299), "Chambers of Xeric Veteran not completed at 20 normal + 5 Challenge Mode runs");
        } finally {
            completions.clear();
            completions.putAll(saved);
        }
        return "237, 299, 397, 461";
    }

    /** Sit Back and Relax: thralls' damage adds up over fights until it reaches 100. */
    private static String thralls(TestContext c) {
        ServerPlayer p = c.player;
        CombatAchievementService.counter(p, CombatAchievements.COUNTER_THRALL_DAMAGE, 60);
        expect(!done(p, 399), "Sit Back and Relax completed at 60 thrall damage");
        expect(SysState.of(p).caCounters.getOrDefault("ca_399", 0) == 60, "thrall damage not kept");
        CombatAchievementService.counter(p, CombatAchievements.COUNTER_THRALL_DAMAGE, 40);
        expect(done(p, 399), "Sit Back and Relax not completed at 100 thrall damage");
        return "399";
    }

    /** Into the Den of Giants: giants killed outside the den don't count; a hill, moss and fire giant inside do. */
    private static String giantsDen(TestContext c) {
        ServerPlayer p = c.player;
        int[] den = denBlock();
        kill(c, "moss_giant", p.getBlockX(), p.getBlockY(), p.getBlockZ()); // on the test platform
        expect(SysState.of(p).caCounters.getOrDefault("ca_398", 0) == 0, "a moss giant outside the Giants' Den counted");
        kill(c, "hill_giant", den[0], den[1], den[2]);
        kill(c, "moss_giant", den[0], den[1], den[2]);
        expect(!done(p, 398), "Into the Den of Giants completed without a fire giant");
        kill(c, "fire_giant_lvl104", den[0], den[1], den[2]);
        expect(done(p, 398), "Into the Den of Giants not completed after a hill, moss and fire giant in the den");
        return "398";
    }

    /** A block inside the Giants' Den's carved rooms. */
    private static int[] denBlock() {
        for (Dungeons.Dungeon d : Dungeons.all()) {
            if (!d.id().equals("dng_giants_den")) {
                continue;
            }
            for (StructureDef.Op op : d.structure().ops()) {
                if (op.type() != StructureDef.Op.Type.CLEAR) {
                    continue;
                }
                int x = (op.x1() + op.x2()) / 2;
                int y = op.y1();
                int z = (op.z1() + op.z2()) / 2;
                if (Dungeons.inside(d.id(), x, y, z)) {
                    return new int[]{x, y, z};
                }
            }
        }
        throw new AssertionError("the Giants' Den has no carved room");
    }

    /** Reports a kill of a fresh creature of {@code id} at a block to the Combat Achievement tracker. */
    private static void kill(TestContext c, String id, int x, int y, int z) {
        EntityType<?> type = ModEntities.typeById(id);
        expect(type != null, "no creature " + id);
        if (!(type.create(c.world) instanceof LivingEntity e)) {
            throw new AssertionError("couldn't create " + id);
        }
        e.setPos(x + 0.5, y, z + 0.5);
        CombatAchievementService.onKill(c.player, e, MonsterStats.of(e));
    }
}
