package com.runecraft.osrs.content.minigames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.minigames.GiantsFoundry.Commission;
import com.runecraft.osrs.content.minigames.GiantsFoundry.Mould;
import com.runecraft.osrs.content.minigames.GiantsFoundry.Style;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Giants' Foundry against the OSRS Wiki (round 40): moulds snapshot, metal score, sections, rewards and the shop. */
class GiantsFoundryTest {
    @Test
    void mouldsMatchTheWikiMouldList() throws IOException {
        int rows = 0;
        Set<String> seen = new HashSet<>();
        for (String line : Files.readAllLines(Path.of("src/test/resources/osrs/minigames/giants_foundry_moulds.tsv"),
                StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] c = line.split("\t");
            Mould m = GiantsFoundry.MOULDS.stream().filter(x -> x.name().equals(c[1])).findFirst().orElse(null);
            assertNotNull(m, "missing mould " + c[1]);
            assertEquals(GiantsFoundry.Part.valueOf(c[0]), m.part(), c[1]);
            assertEquals(Integer.parseInt(c[2]), m.level(), c[1] + " level");
            assertEquals(Integer.parseInt(c[3]), m.cost(), c[1] + " cost");
            for (Style s : Style.values()) {
                assertEquals(Integer.parseInt(c[4 + s.ordinal()]), m.score(s), c[1] + " " + s);
            }
            seen.add(m.id());
            rows++;
        }
        assertEquals(33, rows);
        assertEquals(GiantsFoundry.MOULDS.size(), seen.size());
        // Buying every mould costs 5,400 reputation (wiki).
        assertEquals(5400, GiantsFoundry.MOULDS.stream().mapToInt(Mould::cost).sum());
    }

    @Test
    void twelveCommissionsAndTheWikiBestMouldScores() {
        assertEquals(12, GiantsFoundry.COMMISSIONS.size());
        // Default moulds only (wiki "Best mould combinations"): Narrow Heavy 46, Broad Light 24, average 38.
        int sum = 0;
        for (Commission c : GiantsFoundry.COMMISSIONS) {
            sum += GiantsFoundry.mouldScore(c, GiantsFoundry.bestMoulds(c, m -> !m.purchasable()));
        }
        assertEquals(46, best(Style.NARROW, Style.HEAVY, false));
        assertEquals(24, best(Style.BROAD, Style.LIGHT, false));
        assertEquals(44, best(Style.BROAD, Style.HEAVY, false));
        assertEquals(38, Math.round(sum / 12.0));
        // All moulds: Light Flat 69 (the best), Broad Light 49 (the worst), average 59.
        assertEquals(69, best(Style.LIGHT, Style.FLAT, true));
        assertEquals(49, best(Style.BROAD, Style.LIGHT, true));
        assertEquals(65, best(Style.BROAD, Style.FLAT, true));
        int all = 0;
        for (Commission c : GiantsFoundry.COMMISSIONS) {
            all += GiantsFoundry.mouldScore(c, GiantsFoundry.bestMoulds(c, m -> true));
        }
        assertEquals(59, Math.round(all / 12.0));
    }

    private static int best(Style a, Style b, boolean all) {
        Commission c = new Commission(a, b);
        return GiantsFoundry.mouldScore(c, GiantsFoundry.bestMoulds(c, m -> all || !m.purchasable()));
    }

    private static int score(int... perTier) {
        return GiantsFoundry.metalScore(perTier);
    }

    @Test
    void metalScoresMatchTheWikiAlloyTable() {
        assertEquals(10, score(28, 0, 0, 0, 0, 0));
        assertEquals(20, score(0, 28, 0, 0, 0, 0));
        assertEquals(30, score(0, 0, 28, 0, 0, 0));
        assertEquals(40, score(0, 0, 0, 28, 0, 0));
        assertEquals(50, score(0, 0, 0, 0, 28, 0));
        assertEquals(60, score(0, 0, 0, 0, 0, 28));
        assertEquals(59, score(0, 0, 19, 9, 0, 0));
        assertEquals(89, score(0, 0, 0, 18, 10, 0));
        assertEquals(118, score(0, 0, 0, 0, 19, 9));
        assertEquals(130, score(0, 0, 0, 0, 14, 14));
        // A third metal is ignored: its bars count for nothing.
        assertEquals(score(0, 0, 0, 0, 14, 10), score(0, 0, 0, 4, 14, 10));
        // Ties prefer the more valuable metal.
        assertEquals(score(0, 0, 0, 0, 10, 10), score(0, 0, 0, 10, 10, 10));
        assertEquals(3, GiantsFoundry.sections(10));
        assertEquals(4, GiantsFoundry.sections(20));
        assertEquals(4, GiantsFoundry.sections(59));
        assertEquals(5, GiantsFoundry.sections(60));
        assertEquals(6, GiantsFoundry.sections(118));
        assertEquals(7, GiantsFoundry.sections(130));
        assertEquals(2, GiantsFoundry.bonuses(5));
        assertEquals(3, GiantsFoundry.bonuses(6));
    }

    @Test
    void crucibleTakesBarsAndEquipmentAtTheWikiValues() {
        assertEquals(1, GiantsFoundry.barValue("runite_bar"));
        assertEquals(GiantsFoundry.Metal.ADAMANT, GiantsFoundry.metalOf("adamantite_bar"));
        assertEquals(1, GiantsFoundry.barValue("rune_scimitar"));
        assertEquals(2, GiantsFoundry.barValue("mithril_platelegs"));
        assertEquals(2, GiantsFoundry.barValue("steel_2h_sword"));
        assertEquals(4, GiantsFoundry.barValue("adamant_platebody"));
        assertEquals(0, GiantsFoundry.barValue("gold_bar"));
        assertEquals(0, GiantsFoundry.barValue("rune_dagger"));
        for (GiantsFoundry.Metal m : GiantsFoundry.Metal.values()) {
            assertTrue(ItemCatalog.exists(m.bar()), m.bar());
            for (List<String> list : List.of(GiantsFoundry.EQUIPMENT_1, GiantsFoundry.EQUIPMENT_2, GiantsFoundry.EQUIPMENT_4)) {
                for (String suffix : list) {
                    assertTrue(ItemCatalog.exists(m.prefix() + "_" + suffix), m.prefix() + "_" + suffix);
                }
            }
        }
    }

    @Test
    void handInRewardsFollowTheWikiFormula() {
        assertEquals(30, GiantsFoundry.xp(0));
        assertEquals(25_230, GiantsFoundry.xp(199));
        assertEquals(12_810, GiantsFoundry.xp(130));
        assertEquals(2 * GiantsFoundry.xp(150), GiantsFoundry.coins(150));
        assertEquals(60, GiantsFoundry.coins(0));
        assertEquals(150, GiantsFoundry.reputation(150));
    }

    @Test
    void heatRangesShrinkToHalfAt130() {
        int full = GiantsFoundry.rangeWidth(0);
        assertEquals(full / 2, GiantsFoundry.rangeWidth(130));
        int[] high = GiantsFoundry.Heat.HIGH.range(60);
        int[] low = GiantsFoundry.Heat.LOW.range(60);
        assertTrue(low[1] < GiantsFoundry.Heat.MEDIUM.range(60)[0] && GiantsFoundry.Heat.MEDIUM.range(60)[1] < high[0]);
        assertTrue(GiantsFoundry.Heat.HIGH.contains(833, 130));
        assertEquals(GiantsFoundry.Heat.HIGH, GiantsFoundry.Tool.TRIP_HAMMER.heat());
        assertEquals(5, GiantsFoundry.Tool.TRIP_HAMMER.cycle());
        assertEquals(-25, GiantsFoundry.Tool.TRIP_HAMMER.heatChange());
        assertEquals(15, GiantsFoundry.Tool.GRINDSTONE.heatChange());
        assertEquals(1.0, GiantsFoundry.uniformChance(4));
        assertEquals(0.4, GiantsFoundry.uniformChance(2), 1e-9);
        GiantsFoundry.Tool[] plan = GiantsFoundry.plan(7, new java.util.Random(3));
        for (int i = 1; i < plan.length; i++) {
            assertTrue(plan[i] != plan[i - 1]);
        }
        assertEquals(0, GiantsFoundry.section(0, 4));
        assertEquals(3, GiantsFoundry.section(999, 4));
        assertEquals(1, GiantsFoundry.section(250, 4));
    }

    @Test
    void kovacsShopMatchesTheWiki() {
        RewardShop shop = RewardShops.get("giants_foundry");
        assertCost(shop, "double_ammo_mould", 2000);
        assertCost(shop, "kovacs_grog", 300);
        assertCost(shop, GiantsFoundry.ORE_PACK_ITEM, 200);
        assertCost(shop, "smiths_tunic", 4000);
        assertCost(shop, "smiths_trousers", 4000);
        assertCost(shop, "smiths_boots", 3500);
        assertCost(shop, "smiths_gloves", 3500);
        assertCost(shop, "colossal_blade", 5000);
        for (Mould m : GiantsFoundry.MOULDS) {
            if (m.purchasable()) {
                RewardShop.Entry e = shop.entries().stream().filter(x -> x.item().equals(m.flag())).findFirst().orElse(null);
                assertNotNull(e, m.name());
                assertEquals(RewardShop.Reward.UNLOCK, e.reward());
                assertEquals(m.cost(), e.costs().get(0).amount(), m.name());
            }
        }
        int weights = GiantsFoundry.ORE_PACK.stream().mapToInt(MinigameRules.Loot::weight).sum();
        // The coal share: about 24.5 of 30 ores (wiki).
        assertEquals(24.5, 30.0 * GiantsFoundry.ORE_PACK.get(0).weight() / weights, 0.1);
        for (MinigameRules.Loot l : GiantsFoundry.ORE_PACK) {
            assertTrue(ItemCatalog.exists(l.item()), l.item());
        }
    }

    private static void assertCost(RewardShop shop, String item, int cost) {
        RewardShop.Entry e = shop.entries().stream().filter(x -> x.item().equals(item)).findFirst().orElse(null);
        assertNotNull(e, item);
        assertEquals(cost, e.costs().get(0).amount(), item);
        assertEquals(MinigameCurrencies.FOUNDRY, e.costs().get(0).currency(), item);
    }
}
