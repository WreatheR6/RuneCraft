package com.runecraft.osrs.content.minigames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.minigames.MahoganyHomesLayouts.Hotspot;
import com.runecraft.osrs.content.minigames.MahoganyHomesLayouts.Layout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Mahogany Homes against the OSRS Wiki (round 40): every contract layout, its XP, the per-tier averages and the shop. */
class MahoganyHomesLayoutsTest {
    private static List<String[]> rows() throws IOException {
        List<String[]> rows = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of("src/test/resources/osrs/minigames/mahogany_homes_layouts.tsv"),
                StandardCharsets.UTF_8)) {
            if (!line.isBlank() && !line.startsWith("#")) {
                rows.add(line.split("\t"));
            }
        }
        return rows;
    }

    @Test
    void layoutsMatchTheWikiSnapshot() throws IOException {
        List<String[]> rows = rows();
        assertEquals(rows.size(), MahoganyHomesLayouts.LAYOUTS.size());
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            Layout l = MahoganyHomesLayouts.LAYOUTS.get(i);
            String where = r[0] + " " + r[2] + " #" + i;
            assertEquals(List.of(MinigameRules.MahoganyHomes.TIER_IDS).indexOf(r[0]), l.tier(), where);
            assertEquals(r[1], l.region(), where);
            assertEquals(r[2], l.owner(), where);
            String[] spots = r[3].split(";");
            assertEquals(spots.length, l.hotspots().size(), where);
            for (int k = 0; k < spots.length; k++) {
                String[] f = spots[k].split(":");
                Hotspot h = l.hotspots().get(k);
                assertEquals(Integer.parseInt(f[0]), h.slot(), where);
                assertEquals(f[1], h.name(), where);
                assertEquals(Integer.parseInt(f[2]), h.count(), where);
                assertEquals(f[3].equals("BAR"), h.steelBar(), where);
                assertEquals(f[4].equals("REPAIR"), h.repair(), where);
            }
            // The wiki's plank, bar and XP columns follow from the furniture: build 22.5/48/72/112 and repair 127.5/160/190/240
            // per plank, 120 per steel bar, plus the completion XP.
            assertEquals(Integer.parseInt(r[4]), l.planks(), where);
            assertEquals(Integer.parseInt(r[5]), l.bars(), where);
            assertEquals(Double.parseDouble(r[6].replace(",", "")), l.totalXp(), 0.01, where);
        }
    }

    @Test
    void twelveHomeownersWithLayoutsInEveryTier() {
        assertEquals(List.of("Jess", "Noella", "Ross", "Larry", "Norman", "Tau", "Barbara", "Leela", "Mariah", "Bob", "Jeff", "Sarah"),
                MahoganyHomesLayouts.owners());
        for (int tier = 0; tier < 4; tier++) {
            for (String owner : MahoganyHomesLayouts.owners()) {
                assertTrue(!MahoganyHomesLayouts.layouts(tier, owner).isEmpty(), owner + " tier " + tier);
            }
        }
        // Average planks and XP per contract (wiki "Experience costs"): 10.06 / 10.09 / 11.63 / 12.85 planks and
        // 879.3 / 1,894.1 / 3,260.2 / 4,378.8 XP, averaged over homeowners then layouts.
        double[] planks = {10.06, 10.09, 11.63, 12.85};
        double[] xp = {879.3, 1894.1, 3260.2, 4378.8};
        for (int tier = 0; tier < 4; tier++) {
            double p = 0;
            double x = 0;
            for (String owner : MahoganyHomesLayouts.owners()) {
                List<Layout> ls = MahoganyHomesLayouts.layouts(tier, owner);
                p += ls.stream().mapToInt(Layout::planks).average().orElse(0) / 12;
                x += ls.stream().mapToDouble(Layout::totalXp).average().orElse(0) / 12;
            }
            assertEquals(planks[tier], p, 0.15, "tier " + tier + " planks");
            assertEquals(xp[tier], x, 15, "tier " + tier + " XP");
        }
    }

    @Test
    void amysShopMatchesTheWiki() {
        String c = MinigameCurrencies.CARPENTER;
        SkillingMinigamesRound40Test.assertCost("mahogany_homes", MinigameRules.MahoganyHomes.SUPPLY_CRATE, c, 25);
        SkillingMinigamesRound40Test.assertCost("mahogany_homes", "amys_saw", c, 500);
        SkillingMinigamesRound40Test.assertCost("mahogany_homes", "plank_sack", c, 350);
        SkillingMinigamesRound40Test.assertCost("mahogany_homes", "hosidius_blueprints", c, 2000);
        SkillingMinigamesRound40Test.assertCost("mahogany_homes", "carpenters_helmet", c, 400);
        SkillingMinigamesRound40Test.assertCost("mahogany_homes", "carpenters_shirt", c, 800);
        SkillingMinigamesRound40Test.assertCost("mahogany_homes", "carpenters_trousers", c, 600);
        SkillingMinigamesRound40Test.assertCost("mahogany_homes", "carpenters_boots", c, 200);
        assertEquals(8, RewardShops.get("mahogany_homes").entries().size());
        int weight = MinigameRules.MahoganyHomes.SUPPLY_CRATE_TABLE.stream().mapToInt(MinigameRules.Loot::weight).sum();
        assertEquals(144, weight);
        for (MinigameRules.Loot l : MinigameRules.MahoganyHomes.SUPPLY_CRATE_TABLE) {
            assertTrue(ItemCatalog.exists(l.item()), l.item());
        }
        assertEquals(List.of(2, 3, 4, 5), List.of(MinigameRules.MahoganyHomes.POINTS[0], MinigameRules.MahoganyHomes.POINTS[1],
                MinigameRules.MahoganyHomes.POINTS[2], MinigameRules.MahoganyHomes.POINTS[3]));
    }
}
