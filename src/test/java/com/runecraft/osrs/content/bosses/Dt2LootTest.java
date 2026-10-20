package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.systems.CombatAchievements;
import com.runecraft.osrs.data.Drop;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Desert Treasure II kill loot (OSRS Wiki Drops/Mechanics of the four bosses). */
class Dt2LootTest {
    private static final Path SNAPSHOT = Path.of("src/test/resources/osrs/bosses/boss_drops.tsv");

    /** The wiki's "normal,perfect" standard-table quantities are the table's quantity and the perfect-kill one. */
    @Test
    void perfectQuantitiesMatchTheWikiSnapshot() throws IOException {
        int checked = 0;
        for (String line : Files.readAllLines(SNAPSHOT)) {
            if (line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\\|");
            // "normal,perfect" (once "280; 420", the Whisperer's pure essence)
            if (!AwakenedBosses.isDt2(f[0]) || !f[4].matches("\\d+(,|; )\\d+.*") || f[6].startsWith("-")) {
                continue;
            }
            String[] q = f[4].replace(" (noted)", "").split(",|; ");
            int normal = Integer.parseInt(q[0]);
            int perfect = Integer.parseInt(q[1]);
            String item = f[6].substring(0, f[6].indexOf(':'));
            assertEquals(perfect, Dt2Bosses.perfectQuantity(f[0], item, normal), f[0] + " " + item);
            BossDef b = ContentRegistry.boss(f[0]);
            assertTrue(b.drops.stream().anyMatch(d -> d.item().equals(item) && d.min() == normal && d.max() == normal),
                    f[0] + " drops " + item + " x" + normal);
            checked++;
        }
        assertEquals(98, checked);
        for (String boss : AwakenedBosses.BOSSES) {
            for (Map.Entry<String, int[]> e : Dt2Bosses.perfectQuantities(boss).entrySet()) {
                int[] q = e.getValue();
                assertTrue(Math.abs(q[1] - q[0] * 1.5) <= 1, boss + " " + e.getKey() + ": 50% more"); // wiki rounds
            }
        }
    }

    @Test
    void killsRollInTheWikiOrder() {
        for (String boss : AwakenedBosses.BOSSES) {
            BossDef b = ContentRegistry.boss(boss);
            Random rnd = new Random(boss.hashCode());
            Map<Dt2Loot.Kind, Integer> kinds = new EnumMap<>(Dt2Loot.Kind.class);
            int n = 400_000;
            int clues = 0;
            for (int i = 0; i < n; i++) {
                Dt2Loot.Result r = Dt2Loot.roll(boss, b.drops, false, false, rnd::nextInt);
                kinds.merge(r.kind(), 1, Integer::sum);
                List<Drop> main = r.items().stream().filter(d -> !d.item().startsWith("clue_scroll_")).toList();
                clues += r.items().size() - main.size();
                // Exactly one main drop, except the supply table, which drops its whole set.
                assertEquals(r.kind() == Dt2Loot.Kind.SUPPLIES ? Dt2Bosses.supplies(boss).size() : 1, main.size(), boss + " " + r);
                for (Drop d : r.items()) {
                    assertNotNull(ItemCatalog.get(d.baseItem()), d.item());
                    assertTrue(d.min() > 0 && d.min() == d.max(), d.toString());
                }
            }
            double u = 1.0 / AwakenedBosses.uniqueTableRate(boss);
            double orb = (1 - u) / AwakenedBosses.orbRate(boss);
            double tablet = (1 - u - orb) / Dt2Bosses.TABLET_RATE;
            double quartz = (1 - u - orb - tablet) / Dt2Bosses.QUARTZ_RATE;
            double supplies = (1 - u - orb - tablet - quartz) / Dt2Bosses.SUPPLY_RATE;
            close(u, kinds.get(Dt2Loot.Kind.UNIQUE), n, boss + " unique");
            close(orb, kinds.get(Dt2Loot.Kind.ORB), n, boss + " orb");
            close(tablet, kinds.get(Dt2Loot.Kind.TABLET), n, boss + " tablet");
            close(quartz, kinds.get(Dt2Loot.Kind.QUARTZ), n, boss + " quartz");
            close(supplies, kinds.get(Dt2Loot.Kind.SUPPLIES), n, boss + " supplies");
            close(1.0 / Dt2Loot.CLUE_RATE, clues, n, boss + " clue");
        }
    }

    private static void close(double expected, Integer got, int n, String what) {
        double rate = (got == null ? 0 : got) / (double) n;
        double sd = Math.sqrt(expected * (1 - expected) / n);
        assertTrue(Math.abs(rate - expected) < 5 * sd + 1e-6, what + ": " + rate + " vs " + expected);
    }

    @Test
    void uniqueTableSplitsAndAwakenedTriples() {
        String boss = "vardorvis";
        BossDef b = ContentRegistry.boss(boss);
        Map<String, Integer> got = new HashMap<>();
        Random rnd = new Random(7);
        int uniques = 0;
        int awakenedUniques = 0;
        int n = 200_000;
        for (int i = 0; i < n; i++) {
            if (Dt2Loot.roll(boss, b.drops, true, false, rnd::nextInt).kind() == Dt2Loot.Kind.UNIQUE) {
                awakenedUniques++;
            }
            Dt2Loot.Result r = Dt2Loot.roll(boss, b.drops, false, false, rnd::nextInt);
            if (r.kind() == Dt2Loot.Kind.UNIQUE) {
                uniques++;
            }
        }
        close(3.0 / 136, awakenedUniques, n, "awakened unique table");
        close(1.0 / 136, uniques, n, "unique table");
        for (int i = 0; i < 80_000; i++) {
            got.merge(Dt2Loot.unique(boss, rnd::nextInt).item(), 1, Integer::sum);
        }
        close(1 / 8.0, got.get("ultor_vestige"), 80_000, "vestige");
        close(3 / 8.0, got.get("chromium_ingot"), 80_000, "chromium");
        close(1 / 8.0, got.get("executioners_axe_head"), 80_000, "axe head");
        close(1 / 24.0, got.get("virtus_robe_top"), 80_000, "virtus top");
        close(2 / 8.0, got.get(Dt2Loot.GOLD_RING), 80_000, "vestige progress");
    }

    @Test
    void perfectKillsGetFiftyPercentMoreStandardLoot() {
        BossDef b = ContentRegistry.boss("duke_sucellus");
        List<Drop> standard = Dt2Loot.standardLines("duke_sucellus", b.drops);
        assertEquals(24, standard.size()); // the wiki's resources, runes and ammunition rows
        assertFalse(standard.stream().anyMatch(d -> d.item().equals("coins") || d.item().equals("runite_bar")));
        // A roller that always fails the special rolls and picks the last standard line (soul runes).
        Dt2Loot.Result normal = Dt2Loot.roll("duke_sucellus", b.drops, false, false, bound -> bound - 1);
        Dt2Loot.Result perfect = Dt2Loot.roll("duke_sucellus", b.drops, false, true, bound -> bound - 1);
        assertEquals(Dt2Loot.Kind.STANDARD, perfect.kind());
        Drop d = normal.items().get(0);
        assertEquals(Dt2Bosses.perfectQuantity("duke_sucellus", d.item(), d.min()), perfect.items().get(0).min());
        assertTrue(perfect.items().get(0).min() > d.min());
    }

    /** The bosses' "Perfect" Combat Achievements use the same avoidable-damage rule as perfect-kill loot. */
    @Test
    void perfectTasksUseThePerfectKillRule() {
        Map<String, String> task = Map.of("duke_sucellus", "ca_518", "the_leviathan", "ca_509", "the_whisperer", "ca_500",
                "vardorvis", "ca_491");
        for (String boss : AwakenedBosses.BOSSES) {
            CombatAchievements.Task t = CombatAchievements.task(task.get(boss));
            assertNotNull(t, boss);
            assertEquals(Dt2Bosses.PERFECT_RULE.get(boss), t.rule(), boss);
        }
    }
}
