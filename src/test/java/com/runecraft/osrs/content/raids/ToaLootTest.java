package com.runecraft.osrs.content.raids;

import com.runecraft.osrs.ItemCatalog;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Tombs of Amascut reward points and chest against the OSRS wiki ("Chest (Tombs of Amascut)"). */
class ToaLootTest {
    @Test
    void rewardPoints() {
        // Start on 5,000; deaths cost 20%, at least 1,000; rooms are capped at 20,000 and the total at 64,000.
        assertEquals(4_000, ToaLoot.afterDeath(ToaLoot.START_POINTS));
        assertEquals(3_000, ToaLoot.afterDeath(4_000));
        assertEquals(24_000, ToaLoot.afterDeath(30_000));
        assertEquals(0, ToaLoot.afterDeath(500));
        assertEquals(25_000, ToaLoot.addRoom(5_000, 30_000), "room points cap at 20,000");
        assertEquals(64_000, ToaLoot.addRoom(60_000, 10_000), "total caps at 64,000");
        assertEquals(300, ToaLoot.mvpBonus(1));
        assertEquals(2_400, ToaLoot.mvpBonus(8));
        assertEquals(0, ToaLoot.rewardPoints(4_000), "the starting 5,000 come off for the loot");
        assertEquals(15_000, ToaLoot.rewardPoints(20_000));
    }

    @Test
    void uniquesBelowTheirRaidLevelNeedAFurther50Roll() {
        assertEquals(50, ToaLoot.minLevel("osmumtens_fang"));
        assertEquals(50, ToaLoot.minLevel("lightbearer"));
        assertEquals(150, ToaLoot.minLevel("tumekens_shadow"));
        Random r = new Random(5);
        int shadows = 0;
        int nothing = 0;
        int fangs = 0;
        int n = 200_000;
        for (int i = 0; i < n; i++) {
            String item = ToaLoot.rollUnique(100, r);
            if (item == null) {
                nothing++;
            } else if (item.equals("tumekens_shadow")) {
                shadows++;
            } else if (item.equals("osmumtens_fang")) {
                fangs++;
            }
        }
        // At raid level 100: fang and Lightbearer 70/240 each; the other 100/240 need a 1/50 (Module:Tombs of Amascut loot).
        assertEquals(70 / 240.0, fangs / (double) n, 0.01);
        assertEquals(100 / 240.0 * 49 / 50, nothing / (double) n, 0.01);
        assertEquals(10 / 240.0 / 50, shadows / (double) n, 0.0005);
        assertTrue(ToaLoot.trueUniqueChance(10_000, 100) < RaidScaling.toaPurpleChance(10_000, 100));
        assertEquals(RaidScaling.toaPurpleChance(10_000, 300), ToaLoot.trueUniqueChance(10_000, 300), 1e-12);
    }

    @Test
    void tertiaries() {
        assertEquals(0.25, ToaLoot.eliteClueChance(64_000), 1e-12, "capped at 25%");
        assertEquals(0.05, ToaLoot.eliteClueChance(10_000), 1e-12, "1% per 2,000 points");
        assertEquals(0.1, ToaLoot.threadChance(0, false), 1e-12);
        assertEquals(0.3, ToaLoot.threadChance(15, false), 1e-12, "three times as likely at 15 completions");
        assertEquals(0.3, ToaLoot.threadChance(40, false), 1e-12, "no further rise after 15 completions");
        assertEquals(0.1 * (1 + 2 * 6 / 15.0), ToaLoot.threadChance(6, false), 1e-12, "linear in between");
        assertEquals(1 / 50.0, ToaLoot.threadChance(100, true), 1e-12, "1/50 once one has been received");
        Random r = new Random(9);
        int got = 0;
        Set<String> owned = Set.of("eye_of_the_corruptor", "jewel_of_the_sun", "breach_of_the_scarab");
        for (int i = 0; i < 10_000; i++) {
            String j = ToaLoot.rollJewel(owned, 75, r);
            if (j != null) {
                assertEquals("jewel_of_amascut", j, "a missing jewel always comes first");
                got++;
            }
        }
        assertEquals(12 / 50.0, got / 10_000.0, 0.02, "any jewel: 4/50 rising to 12/50 at 75 completions");
        for (String id : ToaLoot.JEWELS) {
            assertTrue(ItemCatalog.exists(id), id);
        }
        assertTrue(ItemCatalog.exists(ToaLoot.THREAD));
        assertTrue(ItemCatalog.exists(ToaLoot.DUNG));
    }

    @Test
    void challengeRewards() {
        Set<Invocation> all = EnumSet.allOf(Invocation.class);
        Map<String, Integer> level4 = Map.of("akkha", 4, "kephri", 4, "baba", 4, "zebak", 4);
        assertEquals(List.of(), ToaLoot.challengeRewards(600, false, all, level4), "only deathless raids");
        assertEquals(List.of(), ToaLoot.challengeRewards(345, true, all, level4));
        assertEquals(List.of("masori_crafting_kit"), ToaLoot.challengeRewards(350, true, Set.of(), Map.of()));
        assertEquals(List.of("masori_crafting_kit", "menaphite_ornament_kit"), ToaLoot.challengeRewards(445, true, all, level4));
        assertEquals(List.of("masori_crafting_kit", "menaphite_ornament_kit", "remnant_of_akkha", "cursed_phalanx"),
                ToaLoot.challengeRewards(500, true, all, level4), "Akkha's remnant is checked first");
        Set<Invocation> zebakOnly = EnumSet.copyOf(ToaLoot.ZEBAK_INVOCATIONS);
        assertEquals(List.of("masori_crafting_kit", "menaphite_ornament_kit", "remnant_of_zebak"),
                ToaLoot.challengeRewards(450, true, zebakOnly, level4));
        assertEquals(List.of("masori_crafting_kit", "menaphite_ornament_kit"),
                ToaLoot.challengeRewards(450, true, zebakOnly, Map.of("zebak", 3)), "the path must be level 4");
        assertTrue(ToaLoot.challengeRewards(450, true, EnumSet.copyOf(ToaLoot.WARDEN_INVOCATIONS), Map.of())
                .contains("ancient_remnant"));
        for (String id : ToaLoot.challengeRewards(600, true, all, level4)) {
            assertTrue(ItemCatalog.exists(id), id);
        }
        for (String id : List.of("remnant_of_baba", "remnant_of_kephri", "remnant_of_zebak", "ancient_remnant")) {
            assertTrue(ItemCatalog.exists(id), id);
        }
    }

    @Test
    void chest() {
        Random r = new Random(1);
        Map<String, Integer> pts = new HashMap<>();
        pts.put("leech", 1_000);
        pts.put("carry", 30_000);
        int dung = 0;
        int purples = 0;
        for (int i = 0; i < 2_000; i++) {
            RaidRewards.Result res = ToaLoot.roll(pts, 300, Map.of(), false, Set.of(), Map.of(), r);
            for (String p : pts.keySet()) {
                List<RaidRewards.Loot> loot = res.lootFor(p);
                assertFalse(loot.isEmpty(), p + " always finds something");
                boolean purple = res.purple(p);
                long commons = loot.stream().filter(l -> RaidRewards.TOA_COMMONS.stream().anyMatch(c -> c.item().equals(l.item()))).count();
                if (purple) {
                    purples++;
                    assertEquals(0, commons, "a purple replaces the commons");
                } else if (p.equals("leech")) {
                    assertTrue(loot.stream().anyMatch(l -> l.item().equals(ToaLoot.DUNG)), "under 1,500 points: dung");
                    assertEquals(0, commons);
                    dung++;
                } else {
                    assertEquals(3, commons, "three common rolls");
                }
            }
            assertTrue(res.purpleRecipients().size() <= 1, "one unique per raid");
        }
        assertTrue(dung > 1_500);
        assertTrue(purples > 0);
        // Owned challenge rewards are not given again.
        Map<String, ToaLoot.Raider> raiders = Map.of("carry", new ToaLoot.Raider(10, false, Set.of(), Set.of("masori_crafting_kit")));
        RaidRewards.Result res = ToaLoot.roll(Map.of("carry", 30_000), 400, raiders, true, Set.of(), Map.of(), r);
        assertFalse(res.lootFor("carry").stream().anyMatch(l -> l.item().equals("masori_crafting_kit")));
        assertTrue(res.lootFor("carry").stream().anyMatch(l -> l.item().equals("menaphite_ornament_kit")) == false);
        RaidRewards.Result fresh = ToaLoot.roll(Map.of("carry", 30_000), 430, Map.of(), true, Set.of(), Map.of(), r);
        assertTrue(fresh.lootFor("carry").stream().anyMatch(l -> l.item().equals("menaphite_ornament_kit")));
        assertNull(ToaLoot.challengeRewards(0, true, Set.of(), Map.of()).stream().findFirst().orElse(null));
    }

    /** Icthlarin's shroud tiers (OSRS: 100/500/1,000/1,500/2,000 Normal or Expert completions) and the mode counts. */
    @Test
    void shroudsAndModeCounts() {
        assertEquals(0, ToaLoot.shroudTier(99));
        assertEquals(1, ToaLoot.shroudTier(100));
        assertEquals(1, ToaLoot.shroudTier(499));
        assertEquals(2, ToaLoot.shroudTier(500));
        assertEquals(3, ToaLoot.shroudTier(1_000));
        assertEquals(4, ToaLoot.shroudTier(1_500));
        assertEquals(5, ToaLoot.shroudTier(50_000));
        assertEquals(5, ToaLoot.SHROUDS.size());
        for (String s : ToaLoot.SHROUDS) {
            assertTrue(com.runecraft.osrs.ItemCatalog.get(s) != null, s);
        }
        assertEquals("tombs_of_amascut_entry", ToaLoot.modeKey(145));
        assertEquals("tombs_of_amascut_normal", ToaLoot.modeKey(150));
        assertEquals("tombs_of_amascut_expert", ToaLoot.modeKey(300));
        assertEquals("Tombs of Amascut: Expert Mode", ToaLoot.modeCountName(500));
        assertEquals("Tombs of Amascut", ToaLoot.modeCountName(299));
    }

    /**
     * The lobby chest gives one shroud only while the player owns none (worn included), and the shroud is worth nothing
     * (OSRS: untradeable and can't be alchemised), so taking another after losing one is no source of coins.
     */
    /** The room perfection tasks and the mechanical tasks the round 36 rooms report (CombatAchievementRules). */
    @Test
    void roomPerfectionTasksAreTracked() {
        for (int id : new int[]{432, 434, 435, 437, 438, 440, 441, 443, 449, 451, 457}) {
            assertTrue(com.runecraft.osrs.content.systems.CombatAchievements.task("ca_" + id).track()
                    != com.runecraft.osrs.content.systems.CombatAchievements.Track.UNTRACKED, "ca_" + id);
        }
        var m = new com.runecraft.osrs.content.systems.CombatAchievementRules.Metrics();
        m.raid = new com.runecraft.osrs.content.systems.CombatAchievementRules.Raid("TOA", "NORMAL", 2, 300, Set.of(), 0, 0, 0, true,
                30_000, true);
        String perfectionOfHet = com.runecraft.osrs.content.systems.CombatAchievements.task("ca_434").rule();
        String perfectApmeken = com.runecraft.osrs.content.systems.CombatAchievements.task("ca_435").rule();
        assertFalse(com.runecraft.osrs.content.systems.CombatAchievementRules.meets(perfectionOfHet, m));
        m.flags.add(ToaLoot.FLAG_PERFECT_HET);
        assertFalse(com.runecraft.osrs.content.systems.CombatAchievementRules.meets(perfectionOfHet, m), "needs Perfect Akkha too");
        m.flags.add(ToaLoot.FLAG_PERFECT_AKKHA);
        assertTrue(com.runecraft.osrs.content.systems.CombatAchievementRules.meets(perfectionOfHet, m));
        m.flags.add(ToaLoot.FLAG_PERFECT_APMEKEN);
        assertTrue(com.runecraft.osrs.content.systems.CombatAchievementRules.meets(perfectApmeken, m));
        m.raid = new com.runecraft.osrs.content.systems.CombatAchievementRules.Raid("TOA", "NORMAL", 1, 300, Set.of(), 0, 0, 0, true,
                30_000, true);
        assertFalse(com.runecraft.osrs.content.systems.CombatAchievementRules.meets(perfectApmeken, m), "Perfect Apmeken needs a group");
    }

    /** The shroud's Change option cycles through the unlocked tiers (OSRS wiki). */
    @Test
    void shroudChangesBetweenUnlockedTiers() {
        assertNull(ToaLoot.shroudChange("icthlarins_shroud_tier_1", 499), "only tier 1 unlocked");
        assertEquals("icthlarins_shroud_tier_2", ToaLoot.shroudChange("icthlarins_shroud_tier_1", 500));
        assertEquals("icthlarins_shroud_tier_1", ToaLoot.shroudChange("icthlarins_shroud_tier_2", 500), "wraps round");
        assertEquals("icthlarins_shroud_tier_4", ToaLoot.shroudChange("icthlarins_shroud_tier_3", 2_000));
        assertEquals("icthlarins_shroud_tier_1", ToaLoot.shroudChange("icthlarins_shroud_tier_5", 2_000));
        assertNull(ToaLoot.shroudChange("cape_of_legends", 2_000));
    }

    @Test
    void shroudChestGivesOneAndShroudsAreWorthless() {
        assertNull(ToaLoot.shroudFromChest(99, s -> false));
        assertEquals("icthlarins_shroud_tier_1", ToaLoot.shroudFromChest(100, s -> false));
        assertEquals("icthlarins_shroud_tier_3", ToaLoot.shroudFromChest(1_200, s -> false));
        // Owning any tier (here worn in the cape slot) means the chest gives nothing more.
        java.util.Set<String> worn = Set.of("icthlarins_shroud_tier_1");
        assertNull(ToaLoot.shroudFromChest(100, worn::contains));
        assertNull(ToaLoot.shroudFromChest(2_000, worn::contains));
        for (String s : ToaLoot.SHROUDS) {
            com.runecraft.osrs.ItemDef def = com.runecraft.osrs.ItemCatalog.get(s);
            assertEquals(0, def.highAlch(), s + " alchemises for coins");
            assertEquals(0, def.lowAlch(), s);
        }
    }
}
