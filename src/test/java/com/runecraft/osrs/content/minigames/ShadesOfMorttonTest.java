package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.minigames.ShadesOfMortton.Chest;
import com.runecraft.osrs.content.minigames.ShadesOfMortton.Line;
import com.runecraft.osrs.content.minigames.ShadesOfMortton.PyreLog;
import com.runecraft.osrs.content.minigames.ShadesOfMortton.Shade;
import com.runecraft.osrs.data.Drop;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadesOfMorttonTest {
    private static List<String[]> snapshot() throws IOException {
        String fixture = "osrs/minigames/shade_chests.tsv";
        Path path = Path.of("src/test/resources", fixture);
        try (InputStream in = Files.exists(path) ? Files.newInputStream(path)
                : ShadesOfMorttonTest.class.getClassLoader().getResourceAsStream(fixture)) {
            assertNotNull(in, "snapshot missing");
            List<String[]> rows = new ArrayList<>();
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (!line.isBlank() && !line.startsWith("#")) {
                    rows.add(line.split("\t", -1));
                }
            }
            return rows;
        }
    }

    /** Every chest's tables match the wiki snapshot (main table weights, locks, swamp paste). */
    @Test
    void chestsMatchTheWikiSnapshot() throws IOException {
        Map<String, List<String[]>> byChest = new LinkedHashMap<>();
        for (String[] r : snapshot()) {
            byChest.computeIfAbsent(r[0] + " " + r[1], k -> new ArrayList<>()).add(r);
        }
        assertEquals(25, byChest.size());
        assertEquals(25, ShadesOfMortton.chests().size());
        for (Map.Entry<String, List<String[]>> e : byChest.entrySet()) {
            String[] mc = e.getKey().split(" ");
            Chest chest = ShadesOfMortton.chest(mc[0], mc[1]);
            assertNotNull(chest, e.getKey());
            Map<Double, Integer> dens = new HashMap<>();
            List<String[]> main = new ArrayList<>();
            for (String[] r : e.getValue()) {
                switch (r[2]) {
                    case "Locks" -> {
                        assertEquals(mc[0] + "_locks", r[4]);
                        assertEquals(Integer.parseInt(r[8].split("/")[1]), chest.locks(), e.getKey() + " locks");
                    }
                    case "Zealot robes" -> assertEquals(128, chest.zealot(), e.getKey() + " zealot");
                    case "Splitbark scrolls" -> assertTrue(chest.splitbark() > 0, e.getKey());
                    case "Tertiary" -> {
                        if (r[3].equals("Swamp paste")) {
                            assertEquals(Integer.parseInt(r[5]), chest.pasteMin(), e.getKey());
                            assertEquals(Integer.parseInt(r[6]), chest.pasteMax(), e.getKey());
                        }
                    }
                    default -> {
                        main.add(r);
                        if (!r[3].startsWith("Clue")) {
                            dens.merge(Double.parseDouble(r[8].split("/")[1]), 1, Integer::sum);
                        }
                    }
                }
            }
            double den = dens.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
            assertEquals(main.size(), chest.main().size(), e.getKey() + " main table size");
            for (int i = 0; i < main.size(); i++) {
                String[] r = main.get(i);
                Line l = chest.main().get(i);
                assertEquals(r[4], l.item(), e.getKey() + " line " + i);
                assertEquals(Integer.parseInt(r[5]), l.min(), e.getKey() + " " + r[3]);
                assertEquals(Integer.parseInt(r[6]), l.max(), e.getKey() + " " + r[3]);
                String[] frac = r[8].split("/");
                double w = Double.parseDouble(frac[0]) * den / Double.parseDouble(frac[1]);
                assertEquals(w, l.weight(), 1e-3, e.getKey() + " " + r[3]);
                assertTrue(ItemCatalog.exists(l.item()), l.item());
            }
            // The main table's lines add up to its wiki denominator.
            assertEquals(den, chest.total(), den * 0.04, e.getKey() + " total"); // the wiki denominators also cover the pre-rolls
            assertEquals(chest.zealot() > 0, mc[0].equals("gold"));
            assertEquals(mc[0].equals("bronze"), chest.splitbark() == 0, e.getKey());
        }
        // Splitbark tables: 1/599 (steel red) .. 1/50 (gold purple).
        assertEquals(599, ShadesOfMortton.chest("steel", "red").splitbark());
        assertEquals(182, ShadesOfMortton.chest("black", "purple").splitbark());
        assertEquals(76, ShadesOfMortton.chest("silver", "red").splitbark());
        assertEquals(50, ShadesOfMortton.chest("gold", "purple").splitbark());
        for (String id : ShadeChestRewards.UNIQUES) {
            assertTrue(ItemCatalog.exists(id), id);
        }
    }

    @Test
    void coinsFollowTheWikiFormula() {
        assertEquals((5000 - 2700) / 3, ShadesOfMortton.coins(5000, 2700));
        assertEquals(0, ShadesOfMortton.coins(800, 800));
        assertEquals(0, ShadesOfMortton.coins(400, 900));
        assertEquals(400, ShadesOfMortton.coinRolls("bronze")[0]);
        assertEquals(9500, ShadesOfMortton.coinRolls("gold")[1]);
    }

    @Test
    void openingAChestGivesLootPasteAndMaybeCoins() {
        Random random = new Random(7);
        int zealot = 0;
        for (Chest c : ShadesOfMortton.chests()) {
            for (int i = 0; i < 400; i++) {
                ShadesOfMortton.Opening o = ShadesOfMortton.open(c, Set.of(), id -> ItemCatalog.exists(id) ? ItemCatalog.get(id).value() : 0,
                        random::nextDouble);
                assertFalse(o.items().isEmpty());
                for (MinigameReward r : o.items()) {
                    assertTrue(ItemCatalog.exists(r.item()), r.item());
                    assertTrue(r.amount() > 0, r.item());
                }
                assertTrue(o.items().stream().anyMatch(r -> r.item().equals("swamp_paste") || r.item().equals("flamtaer_bag")));
                if (o.items().get(0).item().equals("zealots_boots")) {
                    zealot++;
                }
            }
        }
        assertTrue(zealot > 0, "zealot's boots never rolled");
        // Owning every piece skips the zealot table.
        Chest gold = ShadesOfMortton.chest("gold", "purple");
        assertNull(firstZealot(ShadesOfMortton.open(gold, Set.copyOf(ShadesOfMortton.ZEALOT_ORDER), id -> 0, () -> 0.0)));
        assertEquals("zealots_boots", firstZealot(ShadesOfMortton.open(gold, Set.of(), id -> 0, () -> 0.0)));
        assertEquals("zealots_helm", firstZealot(ShadesOfMortton.open(gold, Set.of("zealots_boots"), id -> 0, () -> 0.0)));
    }

    private static String firstZealot(ShadesOfMortton.Opening o) {
        String first = o.items().get(0).item();
        return first.startsWith("zealots_") ? first : null;
    }

    @Test
    void shadesMatchTheWiki() {
        // level, hitpoints, attack, strength, defence, max hit (OSRS Wiki / weirdgloop dps calc).
        int[][] wiki = {{40, 38, 45, 30, 26, 4}, {60, 56, 64, 47, 42, 6}, {80, 76, 88, 55, 60, 6}, {100, 90, 102, 84, 70, 9},
                {120, 110, 120, 100, 85, 11}, {140, 130, 138, 116, 100, 13}};
        for (Shade s : Shade.values()) {
            int[] w = wiki[s.ordinal()];
            assertEquals(w[0], s.level(), s.name());
            assertEquals(w[1], s.hitpoints(), s.name());
            assertEquals(w[2], s.attack(), s.name());
            assertEquals(w[3], s.strength(), s.name());
            assertEquals(w[4], s.defence(), s.name());
            assertEquals(w[5], s.maxHit(), s.name());
            MonsterDef m = ContentRegistry.monster(s.creature());
            assertNotNull(m, s.creature());
            assertTrue(ItemCatalog.exists(s.remains()), s.remains());
            // Shades drop only their remains; keys come from the pyre.
            assertEquals(1, m.drops.size(), s.creature());
            for (Drop d : m.drops) {
                assertEquals(s.remains(), d.item());
            }
            for (ShadesOfMortton.KeyWeight k : s.keys()) {
                assertTrue(ItemCatalog.exists(k.key()), k.key());
            }
        }
        assertEquals(86, Shade.LOAR.keys().stream().mapToInt(ShadesOfMortton.KeyWeight::weight).sum());
        assertEquals(101, Shade.URIUM.keys().stream().mapToInt(ShadesOfMortton.KeyWeight::weight).sum());
        assertEquals("bronze_shade_key", ShadesOfMortton.key("bronze", "red"));
        assertEquals("gold_shade_key_purple", ShadesOfMortton.key("gold", "purple"));
    }

    @Test
    void cremationGivesKeysOrCoins() {
        Random random = new Random(3);
        int coins = 0;
        int n = 20000;
        for (int i = 0; i < n; i++) {
            ShadesOfMortton.Cremation c = ShadesOfMortton.cremate(Shade.FIYR, random::nextDouble);
            if (c.key() == null) {
                coins++;
                assertTrue(c.coins() >= 2000 && c.coins() <= 4000);
            } else {
                assertTrue(c.key().startsWith("silver_") || c.key().equals("gold_shade_key"), c.key());
            }
        }
        assertEquals(0.21, coins / (double) n, 0.015);
    }

    @Test
    void pyreLogsMatchTheWiki() {
        assertEquals(9, PyreLog.values().length);
        assertEquals(5, PyreLog.NORMAL.level());
        assertEquals(50.0, PyreLog.NORMAL.firemakingXp());
        assertEquals(10.0, PyreLog.NORMAL.makeXp());
        assertEquals(404.5, PyreLog.MAGIC.firemakingXp());
        assertEquals(20.0, PyreLog.MAGIC.makeXp());
        assertEquals(95, PyreLog.REDWOOD.level());
        assertEquals(25.0, PyreLog.NORMAL.prayerXp(Shade.LOAR));
        assertEquals(37.5, PyreLog.NORMAL.prayerXp(Shade.PHRIN));
        assertFalse(PyreLog.OAK.burns(Shade.RIYL));
        assertTrue(PyreLog.WILLOW.burns(Shade.RIYL));
        assertFalse(PyreLog.MAHOGANY.burns(Shade.ASYN));
        assertEquals(79.5, PyreLog.YEW.prayerXp(Shade.ASYN));
        assertEquals(100.0, PyreLog.MAGIC.prayerXp(Shade.FIYR));
        assertFalse(PyreLog.MAGIC.burns(Shade.URIUM));
        assertEquals(120.5, PyreLog.REDWOOD.prayerXp(Shade.URIUM));
        for (PyreLog p : PyreLog.values()) {
            assertTrue(ItemCatalog.exists(p.id()), p.id());
            assertTrue(ItemCatalog.exists(p.log()), p.log());
            assertTrue(p.burns(Shade.LOAR), p.id());
        }
        assertTrue(ItemCatalog.exists("sacred_oil"));
        assertTrue(ItemCatalog.exists("olive_oil"));
        assertTrue(ItemCatalog.exists("limestone_brick"));
    }
}
