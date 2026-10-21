package com.runecraft.osrs.content.minigames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Skill;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The skilling minigames against the OSRS Wiki (round 40): Tithe Farm, Mahogany Homes, the trawler, Trouble Brewing, gnomes. */
class SkillingMinigamesRound40Test {
    static RewardShop.Entry entry(String shop, String item) {
        RewardShop.Entry e = RewardShops.get(shop).entries().stream().filter(x -> x.item().equals(item)).findFirst().orElse(null);
        assertNotNull(e, shop + " sells no " + item);
        return e;
    }

    static void assertCost(String shop, String item, String currency, int cost) {
        RewardShop.Entry e = entry(shop, item);
        assertEquals(cost, e.costs().get(0).amount(), item);
        assertEquals(currency, e.costs().get(0).currency(), item);
        if (!item.startsWith("#")) {
            assertTrue(ItemCatalog.exists(item), item);
        }
    }

    // ------------------------------------------------------------------ Tithe Farm

    @Test
    void titheFarmFollowsTheWiki() {
        // A plant grows in three one-minute stages (fertiliser halves it) and yields one fruit; planting gives no XP.
        assertEquals(100, MinigameRules.TitheFarm.GROW_TICKS);
        assertEquals(50, MinigameRules.TitheFarm.FERTILISED_GROW_TICKS);
        assertEquals(1, MinigameRules.TitheFarm.YIELD);
        assertEquals(3, MinigameRules.TitheFarm.WATERINGS);
        assertEquals(8, MinigameRules.TitheFarm.CAN_DOSES);
        assertEquals(1000, MinigameRules.TitheFarm.GRICOLLER_DOSES);
        // A full batch of golovanova: 9,660 XP (6 harvest + deposits, the 1,500 bonus included) and 35 points.
        double total = 0;
        for (int n = 1; n <= 100; n++) {
            total += 6 + MinigameRules.TitheFarm.depositXp(0, n);
        }
        assertEquals(9_660.0, total, 0.01);
        assertEquals(1_500.0, MinigameRules.TitheFarm.bonusXp(0, 75));
        assertEquals(0.0, MinigameRules.TitheFarm.bonusXp(0, 74));
        assertEquals(35, MinigameRules.TitheFarm.points(100));
        // With the full outfit each harvest and deposit (not the bonus) gains 2.5%, in tenths: 6.1 and 61.5 XP, 9,859 per
        // batch (wiki, "Golovanova seed").
        assertEquals(6.1, SkillingOutfits.boosted(6, 1.025), 1e-9);
        assertEquals(61.5, SkillingOutfits.boosted(60, 1.025), 1e-9);
        double outfit = 0;
        for (int n = 1; n <= 100; n++) {
            double bonus = MinigameRules.TitheFarm.bonusXp(0, n);
            outfit += SkillingOutfits.boosted(6, 1.025) + SkillingOutfits.boosted(MinigameRules.TitheFarm.depositXp(0, n) - bonus, 1.025)
                    + bonus;
        }
        assertEquals(9_859.0, outfit, 0.01);
        assertTrue(ItemCatalog.exists("gricollers_fertiliser"));
    }

    @Test
    void farmerGricollersRewardsMatchTheWiki() {
        String t = MinigameCurrencies.TITHE;
        assertCost("tithe_farm", "farmers_strawhat", t, 75);
        assertCost("tithe_farm", "farmers_jacket", t, 150);
        assertCost("tithe_farm", "farmers_boro_trousers", t, 125);
        assertCost("tithe_farm", "farmers_boots", t, 50);
        assertCost("tithe_farm", "compost", t, 1);
        assertCost("tithe_farm", "supercompost", t, 5);
        assertCost("tithe_farm", "grape_seed", t, 2);
        assertCost("tithe_farm", "herb_box", t, 30);
        assertCost("tithe_farm", "seed_pack", t, 30);
        assertCost("tithe_farm", "seed_box", t, 250);
        assertCost("tithe_farm", "herb_sack", t, 250);
        assertCost("tithe_farm", "gricollers_can", t, 200);
        assertEquals(Skill.HERBLORE, entry("tithe_farm", "herb_sack").requiredSkill());
        assertEquals(58, entry("tithe_farm", "herb_sack").requiredLevel());
        // The full farmer's outfit costs 400 points.
        int outfit = 0;
        for (String piece : SkillingOutfits.FARMER) {
            outfit += entry("tithe_farm", piece).costs().get(0).amount();
        }
        assertEquals(400, outfit);
    }

    // ------------------------------------------------------------------ Fishing Trawler

    @Test
    void trawlerCatchFollowsTheWiki() {
        MinigameRules.Trawler.Catch manta = MinigameRules.Trawler.CATCHES.get(0);
        assertEquals("raw_manta_ray", manta.item());
        assertEquals(81, manta.level());
        assertEquals(69.0, manta.xp());
        for (MinigameRules.Trawler.Catch c : MinigameRules.Trawler.CATCHES) {
            assertTrue(ItemCatalog.exists(c.item()), c.item());
        }
        for (String junk : MinigameRules.Trawler.JUNK) {
            assertTrue(ItemCatalog.exists(junk), junk);
        }
        // At 99 Fishing the wiki's value of a share is 78.5% manta, 16.9% turtle, 3.62% shark.
        assertEquals(0.785, MinigameRules.Trawler.tierChance(99), 0.001);
        double p = MinigameRules.Trawler.tierChance(99);
        assertEquals(0.169, (1 - p) * p, 0.001);
        assertEquals(0.0362, (1 - p) * (1 - p) * p, 0.0005);
        assertEquals(150 / 256.0, MinigameRules.Trawler.tierChance(1), 0.005);
        // A level-15 angler only gets shrimp, sardines and anchovies; level 16 adds oysters.
        java.util.Random r = new java.util.Random(1);
        for (int i = 0; i < 200; i++) {
            assertTrue(MinigameRules.Trawler.fish(15, r::nextDouble).level() == 15);
            assertTrue(MinigameRules.Trawler.fish(40, r::nextDouble).level() <= 40);
        }
        assertEquals("raw_shark", MinigameRules.Trawler.fishFor(76));
        // Solo 20-22 fish with a full meter, half at the 50-point minimum; nothing below 50.
        assertEquals(21, MinigameRules.Trawler.fishCount(1, 255, 1, 0));
        assertEquals(10, MinigameRules.Trawler.fishCount(1, 50, 1, 0));
        assertEquals(0, MinigameRules.Trawler.fishCount(1, 49, 1, 0));
        int crowd = MinigameRules.Trawler.fishCount(20, 255, 1, 0);
        assertTrue(crowd >= 13 && crowd <= 15, "a full meter on a busy trawler gives " + crowd);
        assertEquals(1 / 5000.0, MinigameRules.Trawler.heronChance(50), 1e-12);
        assertEquals(1 / 2500.0, MinigameRules.Trawler.heronChance(255), 1e-12);
        assertEquals(0, MinigameRules.Trawler.junkCount(0.2));
        assertEquals(1, MinigameRules.Trawler.junkCount(0.6));
        assertEquals(2, MinigameRules.Trawler.junkCount(0.9));
        assertEquals(5, MinigameRules.Trawler.LEAK_POINTS);
        assertEquals(7, MinigameRules.Trawler.NET_POINTS);
        assertEquals(10, MinigameRules.Trawler.CHOP_POINTS);
        assertEquals(2, MinigameRules.Trawler.BAIL_POINTS);
        assertTrue(ItemCatalog.exists("pet_heron"));
    }

    @Test
    void outfitsAddUpTo2Point5Percent() {
        assertEquals(1.025, SkillingOutfits.multiplier(SkillingOutfits.FARMER, SkillingOutfits.FARMER), 1e-9);
        assertEquals(1.004, SkillingOutfits.multiplier(SkillingOutfits.ANGLER, List.of("angler_hat")), 1e-9);
        assertEquals(1.016, SkillingOutfits.multiplier(SkillingOutfits.CARPENTER, List.of("carpenters_shirt", "carpenters_trousers",
                "carpenters_boots")), 1e-9);
        for (List<String> o : List.of(SkillingOutfits.FARMER, SkillingOutfits.CARPENTER, SkillingOutfits.ANGLER)) {
            for (String piece : o) {
                assertTrue(ItemCatalog.exists(piece), piece);
            }
        }
    }
}
