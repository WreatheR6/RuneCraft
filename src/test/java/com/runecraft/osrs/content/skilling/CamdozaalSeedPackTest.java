package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Camdozaal fishing, preparing, offering and golems against the wiki, and the seed pack tables against a wiki snapshot. */
class CamdozaalSeedPackTest {
    @Test
    void camdozaalFishMatchTheWiki() {
        // id, Fishing level, Fishing XP, Cooking XP, stops being ruined, Prayer XP (wiki "Ruins of Camdozaal", fish pages).
        Object[][] wiki = {{"guppy", 7, 8.0, 12.0, 42, 4.0}, {"cavefish", 20, 16.0, 23.0, 53, 7.0}, {"tetra", 33, 24.0, 31.0, 61, 10.0},
                {"catfish", 46, 33.0, 43.0, 78, 16.0}};
        assertEquals(wiki.length, Camdozaal.FISH.size());
        for (Object[] w : wiki) {
            Camdozaal.Fish f = Camdozaal.fish((String) w[0]);
            assertNotNull(f, (String) w[0]);
            assertEquals(w[1], f.level());
            assertEquals(w[2], f.fishingXp());
            assertEquals(w[3], f.cookingXp());
            assertEquals(w[4], f.stopRuin());
            assertEquals(w[5], f.prayerXp());
            assertEquals(1.0, f.prepareChance(f.stopRuin()));
            assertTrue(f.prepareChance(f.level()) < 1.0 && f.prepareChance(f.level()) > 0.2, f.id());
            assertTrue(ItemCatalog.exists(f.raw()) && ItemCatalog.exists(f.prepared()) && ItemCatalog.exists(f.ruined()), f.id());
            assertSame(f, Camdozaal.byRaw(f.raw()));
        }
        // Small net: guppy, cavefish, tetra; big net: tetra, catfish. Both on one spot block.
        assertEquals(List.of("raw_tetra", "raw_cavefish", "raw_guppy"), Camdozaal.SMALL_NET.catches().stream().map(GatheringData.Catch::item).toList());
        assertEquals(List.of("raw_catfish", "raw_tetra"), Camdozaal.BIG_NET.catches().stream().map(GatheringData.Catch::item).toList());
        assertEquals(Camdozaal.SMALL_NET.blockId(), Camdozaal.BIG_NET.blockId());
        assertSame(Camdozaal.BIG_NET, Camdozaal.otherNet(Camdozaal.SMALL_NET));
        assertNull(Camdozaal.otherNet(GatheringData.SPOTS.get(0)));
        assertTrue(GatheringData.SPOTS.contains(Camdozaal.SMALL_NET));
        assertEquals("big_fishing_net", Camdozaal.BIG_NET.tool());
    }

    @Test
    void golemsMatchTheWiki() {
        Map<String, int[]> wiki = Map.of("flawed_golem", new int[]{13, 25, 3, 8}, "mind_golem", new int[]{30, 40, 5, 6},
                "body_golem", new int[]{50, 60, 7, 6}, "chaos_golem", new int[]{70, 94, 10, 7});
        for (Camdozaal.Golem g : Camdozaal.GOLEMS) {
            int[] w = wiki.get(g.id());
            MonsterDef m = ContentRegistry.monster(g.id());
            assertNotNull(m, g.id());
            assertEquals(w[0], m.combatLevel, g.id());
            assertEquals(w[1], m.hitpoints, g.id());
            assertEquals(w[2], m.maxHit(), g.id());
            assertEquals(w[3] * 2, g.tiles().length, g.id());
            assertTrue(m.hasAttribute("golem") && m.underground && !m.aggressive, g.id());
            for (Drop d : m.drops) {
                assertTrue(ItemCatalog.exists(d.item()), g.id() + " drops " + d.item());
            }
        }
        assertTrue(ItemCatalog.exists(Camdozaal.MACE));
        assertEquals(40, ItemCatalog.get(Camdozaal.MACE).equipment().requirements().get(com.runecraft.osrs.Skill.ATTACK));
    }

    /** The low, medium and high seed tables match the wiki's "Seed pack" page line for line (seed, quantity, weight). */
    @Test
    void seedPackTablesMatchTheWikiSnapshot() throws IOException {
        List<String> lines = Files.readAllLines(Path.of("src/test/resources/osrs/skilling/seed_pack.tsv"), StandardCharsets.UTF_8);
        int[] counts = new int[3];
        for (String line : lines) {
            if (line.startsWith("#") || line.isBlank()) {
                continue;
            }
            String[] f = line.split("\t");
            if (f[1].equals("White lily seed")) {
                continue; // Tithe Farm packs only
            }
            int table = switch (f[0]) {
                case "low" -> FarmingContracts.LOW;
                case "medium" -> FarmingContracts.MEDIUM;
                default -> FarmingContracts.HIGH;
            };
            String id = f[1].toLowerCase(Locale.ROOT).replace(' ', '_');
            Loot loot = find(table, id);
            if (loot == null) {
                loot = find(table, id.replace("_tree_seed", "_seed"));
            }
            assertNotNull(loot, f[0] + " table lacks " + f[1]);
            assertEquals(Integer.parseInt(f[2]), loot.min(), f[1]);
            assertEquals(Integer.parseInt(f[3]), loot.max(), f[1]);
            assertEquals(Integer.parseInt(f[4]), loot.weight(), f[1]);
            counts[table]++;
        }
        for (int t = 0; t < 3; t++) {
            assertEquals(counts[t], FarmingContracts.table(t).size(), "table " + t);
        }
    }

    private static Loot find(int table, String id) {
        for (Loot l : FarmingContracts.table(table)) {
            if (l.item().equals(id)) {
                return l;
            }
        }
        return null;
    }
}
