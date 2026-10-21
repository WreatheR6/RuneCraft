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

    // ------------------------------------------------------------------ Trouble Brewing

    @Test
    void troubleBrewingFollowsTheWiki() {
        assertEquals(5, MinigameRules.TroubleBrewing.WATER_PER_RUM);
        assertEquals(3, MinigameRules.TroubleBrewing.COLOURED_PER_RUM);
        assertEquals(3, MinigameRules.TroubleBrewing.BOILERS);
        assertEquals(3, MinigameRules.TroubleBrewing.BOILER_MIN_LOGS);
        assertEquals(10, MinigameRules.TroubleBrewing.BOILER_MAX_LOGS);
        assertEquals(75, MinigameRules.TroubleBrewing.LOG_STORE);
        assertEquals(2000, MinigameRules.TroubleBrewing.GAME_TICKS);
        assertEquals(64, MinigameRules.TroubleBrewing.BREW_TICKS);
        assertEquals(200.0, MinigameRules.TroubleBrewing.BITTERNUT_XP);
        assertEquals(100.0, MinigameRules.TroubleBrewing.DEPOSIT_XP);
        assertEquals(50.0, MinigameRules.TroubleBrewing.BITTERNUT_HUNTER_XP);
        assertEquals(40.0, MinigameRules.TroubleBrewing.SWEETGRUB_HUNTER_XP);
        assertEquals(50.0, MinigameRules.TroubleBrewing.BARK_FLETCHING_XP);
        assertEquals(100.0, MinigameRules.TroubleBrewing.LIGHT_FIREMAKING_XP);
        assertEquals(3, MinigameRules.TroubleBrewing.rumThirds("bitternut"));
        assertEquals(1, MinigameRules.TroubleBrewing.rumThirds("bowl_of_red_water"));
        assertEquals(0, MinigameRules.TroubleBrewing.rumThirds("minecraft:water_bucket"));
        assertEquals(10 * 3 + 100, MinigameRules.TroubleBrewing.piecesOfEight(3, 250));
        String e = MinigameCurrencies.EIGHT;
        for (String c : MinigameItems.NAVAL_COLOURS) {
            assertCost("trouble_brewing", c + "_naval_shirt", e, 1000);
            assertCost("trouble_brewing", c + "_tricorn_hat", e, 500);
            assertCost("trouble_brewing", c + "_navy_slacks", e, 1000);
        }
        assertCost("trouble_brewing", "cutthroat_flag", e, 2000);
        assertCost("trouble_brewing", "gilded_smile_flag", e, 2000);
        assertCost("trouble_brewing", "bronze_fist_flag", e, 3000);
        assertCost("trouble_brewing", "lucky_shot_flag", e, 4000);
        assertCost("trouble_brewing", "treasure_flag", e, 5000);
        assertCost("trouble_brewing", "phasmatys_flag", e, 6000);
        assertCost("trouble_brewing", "the_stuff", e, 50);
        assertCost("trouble_brewing", "rum_red", e, 20);
        assertCost("trouble_brewing", "rum_blue", e, 20);
        // 39,590 pieces of eight buy everything once (wiki).
        int all = RewardShops.get("trouble_brewing").entries().stream().mapToInt(x -> x.costs().get(0).amount()).sum();
        assertEquals(39_590, all);
        for (String item : List.of("tb_red_flowers", "bowl_of_red_water", "scrapey_tree_logs", "bitternut", "sweetgrubs", "scrapey_tree_bark")) {
            assertTrue(ItemCatalog.exists(item), item);
        }
    }

    // ------------------------------------------------------------------ Gnome Restaurant

    @Test
    void gnomeRestaurantFollowsTheWiki() {
        assertEquals(20, MinigameRules.GnomeRestaurant.EASY_CUSTOMERS.size());
        assertEquals(20, MinigameRules.GnomeRestaurant.HARD_CUSTOMERS.size());
        assertEquals(600, MinigameRules.GnomeRestaurant.EASY_TICKS);
        assertEquals(1100, MinigameRules.GnomeRestaurant.HARD_TICKS);
        assertEquals(550, MinigameRules.GnomeRestaurant.HARD_DECLINE_TICKS);
        assertEquals(120, MinigameRules.GnomeRestaurant.MAX_POINTS);
        for (MinigameRules.Loot l : MinigameRules.GnomeRestaurant.HARD_TIPS) {
            assertTrue(ItemCatalog.exists(l.item()), l.item());
        }
        for (MinigameRules.GnomeRestaurant.Special sp : MinigameRules.GnomeRestaurant.SPECIALS) {
            assertTrue(ItemCatalog.exists(sp.item()), sp.item());
            for (String c : sp.customers()) {
                assertTrue(MinigameRules.GnomeRestaurant.HARD_CUSTOMERS.contains(c), c);
            }
        }
        // The x/128 entries of the hard table add up to 105/128 (the bolts fill the rest).
        int base = MinigameRules.GnomeRestaurant.HARD_TIPS.stream().filter(l -> !l.item().endsWith("_bolts"))
                .mapToInt(MinigameRules.Loot::weight).sum();
        assertEquals(105 * 4, base);
        assertEquals(512, MinigameRules.GnomeRestaurant.HARD_TIPS.stream().mapToInt(MinigameRules.Loot::weight).sum());
        // Only Captain Ninto and Captain Daerkin tip the scarf and goggles.
        assertTrue(MinigameRules.GnomeRestaurant.specials("Captain Ninto").stream().anyMatch(sp -> sp.item().equals("gnome_scarf")));
        assertTrue(MinigameRules.GnomeRestaurant.specials("King Bolren").isEmpty());
        java.util.Random r = new java.util.Random(4);
        for (int i = 0; i < 300; i++) {
            String item = MinigameRules.GnomeRestaurant.hardTip("King Bolren", r::nextDouble).item();
            assertTrue(!item.equals("gnome_scarf") && !item.equals("grand_seed_pod"), item);
        }
        for (Object[] d : MinigameItems.GNOME_DISHES) {
            assertTrue(MinigameRules.GnomeRestaurant.dishXp((String) d[0]) >= 150, (String) d[0]);
        }
        assertEquals(190.0, MinigameRules.GnomeRestaurant.dishXp("chocolate_bomb"));
        assertCost("gnome_restaurant", MinigameRules.GnomeRestaurant.RANDOM_DISH, MinigameCurrencies.GNOME_TOKENS, 1);
    }

    // ------------------------------------------------------------------ Sorceress's Garden

    @Test
    void everyGardenHasAnUnseenRouteTheWalkCanTake() {
        int goal = SorceressGarden.CORRIDOR + 2;
        for (SorceressGarden.Season s : SorceressGarden.Season.values()) {
            int found = 0;
            for (long t0 = 0; t0 < 60; t0 += 7) {
                List<int[]> route = SorceressGarden.route(s, t0, 2, goal, 400);
                if (route == null) {
                    continue;
                }
                found++;
                int[] at = {1, 0};
                for (int i = 0; i < route.size(); i++) {
                    int[] sq = route.get(i);
                    assertTrue(Math.abs(sq[0] - at[0]) + Math.abs(sq[1] - at[1]) <= 2, s + " route jumps at step " + i);
                    assertTrue(SorceressGarden.walkable(s, sq[0], sq[1]), s + " route goes through a hedge");
                    assertTrue(!SorceressGarden.seen(s, t0 + i + 1, sq[0], sq[1]), s + " route is seen at step " + i);
                    at = sq;
                }
                assertTrue(at[0] >= goal, s + " route ends at " + at[0]);
            }
            assertTrue(found >= 5, s + ": routes from only " + found + " of 9 start ticks");
        }
    }

    @Test
    void gnomeTokenHoldsTenDishesAtOnceNotForLife() {
        // The points counter is lifetime: past 120 points a delivery that crosses 12 still makes a dish if one was used.
        assertEquals(10, MinigameRules.GnomeRestaurant.MAX_TOKENS);
        assertEquals(1, MinigameRules.GnomeRestaurant.tokensToGive(119, 122, 9));
        assertEquals(0, MinigameRules.GnomeRestaurant.tokensToGive(119, 122, 10), "ten unused dishes: the token is full");
        assertEquals(1, MinigameRules.GnomeRestaurant.tokensToGive(359, 360, 0), "no lifetime cap");
        assertEquals(0, MinigameRules.GnomeRestaurant.tokensToGive(360, 361, 0));
        assertEquals(0, MinigameRules.GnomeRestaurant.tokensToGive(11, 14, 12), "more than ten held never goes negative");
        // Gnome cooking levels (wiki "Gnome cooking"): every restaurant dish needs 25 Cooking or more.
        for (Object[] d : MinigameItems.GNOME_DISHES) {
            assertTrue(MinigameRules.GnomeRestaurant.dishLevel((String) d[0]) >= 25, (String) d[0]);
        }
        assertEquals(42, MinigameRules.GnomeRestaurant.dishLevel("chocolate_bomb"));
    }

    @Test
    void anglerPiecesComeInTwoSetsInOrder() {
        int[] received = new int[MinigameRules.Trawler.OUTFIT.length];
        java.util.Random random = new java.util.Random(7);
        for (int n = 0; n < received.length * MinigameRules.Trawler.ANGLER_SETS; n++) {
            int i = MinigameRules.Trawler.anglerPiece(received, random::nextInt);
            assertTrue(i >= 0, "roll " + n + " gave nothing");
            // The first set completes before any piece of the second.
            assertEquals(n / received.length, received[i], "roll " + n + " gave a piece of the wrong set");
            received[i]++;
        }
        assertEquals(-1, MinigameRules.Trawler.anglerPiece(received, random::nextInt), "a third set");
        assertEquals(2, MinigameRules.Trawler.anglerPiece(new int[]{1, 1, 0, 1}, random::nextInt), "the missing waders");
        assertEquals(0, MinigameRules.Trawler.anglerPiece(new int[]{1, 2, 2, 2}, random::nextInt), "the second hat");
    }

    @Test
    void foundryPreformStorageRoundTrips() {
        GiantsFoundry.Commission c = GiantsFoundry.COMMISSIONS.get(5);
        GiantsFoundry.Mould[] moulds = GiantsFoundry.bestMoulds(c, m -> true);
        int[] metal = new int[GiantsFoundry.Metal.values().length];
        metal[GiantsFoundry.Metal.ADAMANT.ordinal()] = 14;
        metal[GiantsFoundry.Metal.RUNE.ordinal()] = 14;
        GiantsFoundry.Tool[] plan = {GiantsFoundry.Tool.TRIP_HAMMER, GiantsFoundry.Tool.POLISHING_WHEEL, GiantsFoundry.Tool.GRINDSTONE,
                GiantsFoundry.Tool.TRIP_HAMMER, GiantsFoundry.Tool.GRINDSTONE, GiantsFoundry.Tool.POLISHING_WHEEL, GiantsFoundry.Tool.TRIP_HAMMER};
        GiantsFoundry.Stored s = new GiantsFoundry.Stored(c, moulds, metal, 28, true, 130, plan, 612, 455, 160, 170, 1, 2, 3);
        java.util.Map<String, Integer> stats = new java.util.HashMap<>(s.encode());
        GiantsFoundry.Stored back = GiantsFoundry.Stored.decode(k -> stats.getOrDefault(k, 0));
        assertNotNull(back);
        assertEquals(c, back.commission());
        assertEquals(List.of(moulds), List.of(back.moulds()));
        assertEquals(List.of(plan), List.of(back.plan()));
        assertTrue(java.util.Arrays.equals(metal, back.metal()));
        assertEquals(List.of(28, 130, 612, 455, 160, 170, 1, 2, 3), List.of(back.barsIn(), back.metalScore(), back.progress(), back.heat(),
                back.quality(), back.startQuality(), back.mistakes(), back.bonusesLeft(), back.bonusesTotal()));
        assertTrue(back.poured());
        // A crucible of bars without a commission is kept too; nothing at all is not.
        GiantsFoundry.Stored bars = new GiantsFoundry.Stored(null, null, metal, 9, false, 0, new GiantsFoundry.Tool[0], 0, 0, 0, 0, 0, 0, 0);
        assertTrue(bars.worthKeeping());
        java.util.Map<String, Integer> barStats = new java.util.HashMap<>(bars.encode());
        GiantsFoundry.Stored barsBack = GiantsFoundry.Stored.decode(k -> barStats.getOrDefault(k, 0));
        assertEquals(9, barsBack.barsIn());
        assertEquals(null, barsBack.commission());
        assertEquals(null, barsBack.moulds());
        assertTrue(!barsBack.poured());
        assertTrue(!new GiantsFoundry.Stored(null, null, new int[6], 0, false, 0, new GiantsFoundry.Tool[0], 0, 0, 0, 0, 0, 0, 0).worthKeeping());
        stats.put(GiantsFoundry.STORE_KEPT, 0);
        assertEquals(null, GiantsFoundry.Stored.decode(k -> stats.getOrDefault(k, 0)), "a collected commission isn't restored twice");
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
