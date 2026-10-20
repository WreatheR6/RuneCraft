package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.bosses.RoyalTitansLoot.Choice;
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

/** The Royal Titans' loot against the OSRS Wiki snapshot {@code osrs/bosses/royal_titans_loot.tsv} and its contribution rules. */
class RoyalTitansLootTest {
    private record Row(String titan, String section, String name, int min, int max, int num, int den, String id) {
    }

    private static List<Row> rows() throws Exception {
        List<Row> out = new ArrayList<>();
        try (BufferedReader r = Files.newBufferedReader(Path.of("src/test/resources/osrs/bosses/royal_titans_loot.tsv"),
                StandardCharsets.UTF_8)) {
            for (String line; (line = r.readLine()) != null; ) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] f = line.split("\\|");
                String[] q = f[3].split("-");
                int num = 1;
                int den = 1;
                if (!f[4].equals("Always")) {
                    String[] rr = f[4].split("/");
                    num = Integer.parseInt(rr[0]);
                    den = Integer.parseInt(rr[1]);
                }
                String titan = f[0].equals("branda") ? RoyalTitansLoot.BRANDA : RoyalTitansLoot.ELDRIC;
                out.add(new Row(titan, f[1], f[2], Integer.parseInt(q[0]), Integer.parseInt(q[q.length - 1]), num, den, f[5]));
            }
        }
        return out;
    }

    @Test
    void tablesMatchTheWiki() throws Exception {
        List<Row> rows = rows();
        assertTrue(rows.size() > 60, "snapshot rows " + rows.size());
        for (String titan : new String[]{RoyalTitansLoot.BRANDA, RoyalTitansLoot.ELDRIC}) {
            Map<String, RoyalTitansLoot.Line> main = new HashMap<>();
            for (RoyalTitansLoot.Line l : RoyalTitansLoot.main(titan)) {
                main.put(l.item(), l);
                assertTrue(ItemCatalog.exists(l.item()), l.item());
            }
            Map<String, RoyalTitansLoot.Line> pre = new HashMap<>();
            for (RoyalTitansLoot.Line l : RoyalTitansLoot.pre(titan)) {
                pre.put(l.item(), l);
                assertTrue(ItemCatalog.exists(l.item()), l.item());
            }
            int mainRows = 0;
            for (Row row : rows) {
                if (!row.titan().equals(titan)) {
                    continue;
                }
                switch (row.section()) {
                    case "main" -> {
                        mainRows++;
                        RoyalTitansLoot.Line l = main.get(row.id());
                        assertNotNull(l, titan + " misses " + row.name());
                        assertEquals(row.num(), l.weight(), row.name() + " slots");
                        assertEquals(row.den(), RoyalTitansLoot.mainWeight(titan), titan + " table size");
                        assertEquals(row.min(), l.min(), row.name() + " min");
                        assertEquals(row.max(), l.max(), row.name() + " max");
                    }
                    case "pre" -> {
                        RoyalTitansLoot.Line l = pre.get(row.id());
                        assertNotNull(l, titan + " misses pre-roll " + row.name());
                        assertEquals(row.den() / row.num(), l.weight(), row.name());
                    }
                    case "pages" -> {
                        assertEquals(RoyalTitansLoot.PAGES_MIN, row.min());
                        assertEquals(RoyalTitansLoot.PAGES_MAX, row.max());
                    }
                    default -> throw new AssertionError(row.section());
                }
            }
            assertEquals(main.size(), mainRows, titan + " main lines");
        }
    }

    @Test
    void contributionScalesQuantitiesAndUniques() {
        // "a player who deals 50% of the damage to the titans will roll twice for 140-160 coal"
        assertEquals(50, RoyalTitansLoot.percent(600));
        assertEquals(140, RoyalTitansLoot.scaled(280, 50));
        assertEquals(160, RoyalTitansLoot.scaled(320, 50));
        // more than 0% but under 1% (1-11 damage): one of each item
        assertEquals(0, RoyalTitansLoot.percent(11));
        assertEquals(1, RoyalTitansLoot.scaled(300, 0));
        assertEquals(1, RoyalTitansLoot.percent(12));
        Random r = new Random(9);
        int n = 200_000;
        int crownsFull = 0;
        int crownsHalf = 0;
        for (int i = 0; i < n; i++) {
            crownsFull += count(RoyalTitansLoot.roll(RoyalTitansLoot.BRANDA, Choice.LOOT, 1200, r), "fire_element_staff_crown");
            crownsHalf += count(RoyalTitansLoot.roll(RoyalTitansLoot.BRANDA, Choice.LOOT, 600, r), "fire_element_staff_crown");
        }
        assertEquals(n / 75.0, crownsFull, n / 75.0 * 0.1);
        assertEquals(n / 150.0, crownsHalf, n / 150.0 * 0.12);
    }

    @Test
    void choices() {
        Random r = new Random(2);
        for (int i = 0; i < 2000; i++) {
            List<RoyalTitansLoot.Loot> loot = RoyalTitansLoot.roll(RoyalTitansLoot.ELDRIC, Choice.LOOT, 1200, r);
            long main = loot.stream().filter(l -> !l.item().startsWith("clue_scroll")).count();
            boolean unique = loot.stream().anyMatch(l -> RoyalTitansLoot.ELDRIC_PRE.stream().anyMatch(u -> u.item().equals(l.item())));
            assertEquals(unique ? 1 : 2, main, loot.toString()); // a unique, else two standard rolls
            assertTrue(loot.stream().noneMatch(l -> l.item().equals("fire_element_staff_crown")), "Branda's crown from Eldric");
            List<RoyalTitansLoot.Loot> sacrificed = RoyalTitansLoot.roll(RoyalTitansLoot.BRANDA, Choice.SACRIFICE, 1200, r);
            assertTrue(sacrificed.stream().allMatch(l -> l.item().startsWith("clue_scroll")), sacrificed.toString());
            List<RoyalTitansLoot.Loot> pages = RoyalTitansLoot.roll(RoyalTitansLoot.BRANDA, Choice.PAGES, 1200, r);
            RoyalTitansLoot.Loot p = pages.get(0);
            assertEquals(RoyalTitansLoot.PAGE, p.item());
            assertTrue(p.count() >= 10 && p.count() <= 19, "pages " + p.count());
        }
        // 120 damage (10%) always gets at least one page; 71 (5%) never does
        for (int i = 0; i < 200; i++) {
            assertTrue(count(RoyalTitansLoot.roll(RoyalTitansLoot.BRANDA, Choice.PAGES, 120, r), RoyalTitansLoot.PAGE) >= 1);
            assertEquals(0, count(RoyalTitansLoot.roll(RoyalTitansLoot.BRANDA, Choice.PAGES, 71, r), RoyalTitansLoot.PAGE));
        }
        assertEquals(1500, RoyalTitansLoot.petRate(Choice.SACRIFICE));
        assertEquals(3000, RoyalTitansLoot.petRate(Choice.LOOT));
        assertTrue(RoyalTitansLoot.roll(RoyalTitansLoot.BRANDA, Choice.LOOT, 0, r).isEmpty());
    }

    private static int count(List<RoyalTitansLoot.Loot> loot, String item) {
        int n = 0;
        for (RoyalTitansLoot.Loot l : loot) {
            if (l.item().equals(item)) {
                n += Math.max(1, l.count() > 0 ? 1 : 0);
            }
        }
        return n;
    }
}
