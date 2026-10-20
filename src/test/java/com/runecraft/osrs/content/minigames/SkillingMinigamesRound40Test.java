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
