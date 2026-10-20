package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.data.Drop;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Diffs the Doom of Mokhaiotl's delve loot against the OSRS Wiki snapshot {@code osrs/bosses/doom_delve_table.tsv}
 * (Kiyogitpy/osrs-wiki-data): every regular line with its delve-3 quantities and its share of 312, and per delve level
 * the quantity multiplier, the guaranteed demon tears, the overall unique rate and Dom's rate.
 */
class DoomDelveTableTest {
    private record Line(String name, int min, int max, int weight, String id) {
    }

    private record Level(int level, double multiplier, int tears, int unique, int dom) {
    }

    private static final List<Line> LINES = new ArrayList<>();
    private static final List<Level> LEVELS = new ArrayList<>();

    static {
        try (BufferedReader r = Files.newBufferedReader(Path.of("src/test/resources/osrs/bosses/doom_delve_table.tsv"),
                StandardCharsets.UTF_8)) {
            boolean levels = false;
            for (String line; (line = r.readLine()) != null; ) {
                if (line.startsWith("# delve|")) {
                    levels = true;
                }
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] f = line.split("\\|");
                if (levels) {
                    LEVELS.add(new Level(Integer.parseInt(f[0]), Double.parseDouble(f[1]), Integer.parseInt(f[2]),
                            Integer.parseInt(f[3]), Integer.parseInt(f[4])));
                } else {
                    String[] q = f[1].split("-");
                    String[] rarity = f[2].split("/");
                    int weight = Integer.parseInt(rarity[0]) * DoomDelves.TABLE_WEIGHT / Integer.parseInt(rarity[1]);
                    LINES.add(new Line(f[0], Integer.parseInt(q[0]), Integer.parseInt(q[q.length - 1]), weight, f[3]));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void regularTableMatchesTheWiki() {
        assertEquals(24, LINES.size(), "snapshot lines");
        Map<String, Drop> mine = new HashMap<>();
        int total = 0;
        for (Drop d : DoomDelves.table()) {
            mine.put(d.item(), d);
            total += d.rarity();
            assertNotNull(ItemCatalog.get(d.item()), "no item " + d.item());
        }
        assertEquals(DoomDelves.TABLE_WEIGHT, total, "table weight");
        assertEquals(LINES.size(), mine.size(), "lines in DoomDelves.TABLE");
        for (Line l : LINES) {
            Drop d = mine.get(l.id());
            assertNotNull(d, "missing wiki line " + l.name());
            assertEquals(l.weight(), d.rarity(), l.name() + " weight /312");
            assertEquals(l.min(), d.min(), l.name() + " min");
            assertEquals(l.max(), d.max(), l.name() + " max");
        }
    }

    @Test
    void levelsMatchTheWiki() {
        assertEquals(9, LEVELS.size());
        for (Level lv : LEVELS) {
            assertEquals(lv.multiplier(), DoomDelves.quantityMultiplier(lv.level()), 1e-9, "multiplier, delve " + lv.level());
            assertEquals(lv.tears(), DoomDelves.guaranteedTears(lv.level()), "tears, delve " + lv.level());
            assertEquals(lv.unique(), DoomDelves.uniqueRate(lv.level()), "unique rate, delve " + lv.level());
            assertEquals(lv.dom(), DoomDelves.petRate(lv.level()), "Dom, delve " + lv.level());
        }
    }

    @Test
    void sharkLuresDropInEvenQuantitiesAtDelveThree() {
        Random r = new Random(7);
        int lures = 0;
        for (int i = 0; i < 40000 && lures < 50; i++) {
            for (DoomDelves.Loot l : DoomDelves.roll(3, r)) {
                if (l.item().equals(DoomDelves.SHARK_LURE)) {
                    lures++;
                    assertEquals(0, l.count() % 2, "odd shark lure stack " + l.count());
                    assertTrue(l.count() >= 40 && l.count() <= 70, "lures " + l.count());
                }
            }
        }
        assertTrue(lures >= 50, "shark lures rolled " + lures);
    }

    @Test
    void listedDropsShowADeepDelve() {
        var doom = ContentRegistry.boss(DoomDelves.BOSS);
        Map<String, Integer> rates = new HashMap<>();
        for (Drop d : doom.drops) {
            rates.putIfAbsent(d.item(), d.rarity());
        }
        for (String u : DoomDelves.UNIQUES) {
            assertEquals(540, rates.get(u), u);
        }
        assertEquals(1, rates.get("demon_tear"));
        assertEquals(15, rates.get(DoomDelves.WAYSTONE)); // 21/312
        assertEquals(50, rates.get("clue_scroll_elite"));
        assertTrue(ItemCatalog.exists(DoomDelves.WAYSTONE) && ItemCatalog.exists(DoomDelves.SHARK_LURE)
                && ItemCatalog.exists(DoomDelves.SUN_KISSED_BONES) && ItemCatalog.exists("aether_catalyst"));
    }
}
