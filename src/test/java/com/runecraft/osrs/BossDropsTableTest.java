package com.runecraft.osrs;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.data.Boss;
import com.runecraft.osrs.data.BossDropsTable;
import com.runecraft.osrs.data.Drop;
import com.runecraft.osrs.data.DropTables;
import com.runecraft.osrs.data.MonsterDropsTable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Boss drop tables against the wiki snapshot ({@code osrs/bosses/boss_drops.tsv}) and {@link BossDropsTable}. */
class BossDropsTableTest {
    private static final Path SNAPSHOT = Path.of("src/test/resources/osrs/bosses/boss_drops.tsv");

    /** Drops of every boss by patch key ({@code Boss.<NAME>} for the built-in ones). */
    private static Map<String, List<Drop>> bossDrops() {
        Map<String, List<Drop>> out = new HashMap<>();
        for (Boss b : Boss.values()) {
            out.put(BossDropsTable.key(b), b.drops());
        }
        for (BossDef b : ContentRegistry.bosses()) {
            out.put(b.id, b.drops);
        }
        return out;
    }

    private static int rarity(List<Drop> drops, String item) {
        int found = -1;
        for (Drop d : drops) {
            if (d.item().equals(item)) {
                assertEquals(-1, found, "several lines for " + item);
                found = d.rarity();
            }
        }
        return found;
    }

    @Test
    void everyWikiLineIsOnTheBossTable() throws IOException {
        Map<String, List<Drop>> drops = bossDrops();
        List<String> wrong = new ArrayList<>();
        int checked = 0;
        Set<String> bosses = new TreeSet<>();
        for (String line : Files.readAllLines(SNAPSHOT)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\\|", -1);
            assertEquals(7, f.length, line);
            String drop = f[6];
            bosses.add(f[0]);
            if (drop.startsWith("-")) {
                continue;
            }
            int colon = drop.lastIndexOf(':');
            int dash = drop.indexOf('-', colon);
            int slash = drop.indexOf('/', colon);
            Drop want = new Drop(drop.substring(0, colon), Integer.parseInt(drop.substring(colon + 1, dash)),
                    Integer.parseInt(drop.substring(dash + 1, slash)), Integer.parseInt(drop.substring(slash + 1)));
            List<Drop> table = drops.get(f[0]);
            if (table == null) {
                wrong.add("no boss " + f[0]);
            } else if (!table.contains(want)) {
                wrong.add(f[0] + ": missing " + want + " (wiki " + f[3] + " " + f[4] + " " + f[5] + ")");
            }
            checked++;
        }
        assertTrue(wrong.isEmpty(), wrong.size() + " wrong:\n" + String.join("\n", wrong));
        assertTrue(checked > 1000, "checked " + checked);
        // Every patched boss is in the snapshot, and the other way round.
        assertEquals(bosses, new TreeSet<>(BossDropsTable.patches().keySet()));
    }

    /**
     * A boss whose main table BossLoot rolls more than once (Zulrah, Vorkath...) takes every line per roll, its shared tables
     * too: their snapshot line is the wiki's per-roll rate, not the rate times the rolls.
     */
    @Test
    void multiRollBossesTakeSharedTablesPerRoll() throws IOException {
        int checked = 0;
        for (String line : Files.readAllLines(SNAPSHOT)) {
            String[] f = line.split("\\|", -1);
            if (line.startsWith("#") || f.length < 7 || !f[3].startsWith("<") || f[6].startsWith("-")
                    || com.runecraft.osrs.content.bosses.BossLoot.rolls(f[0]) == 1) {
                continue;
            }
            String[] rate = f[5].split("/");
            double perRoll = Double.parseDouble(rate[1]) / Double.parseDouble(rate[0]);
            int n = Integer.parseInt(f[6].substring(f[6].lastIndexOf('/') + 1));
            assertTrue(Math.abs(n - perRoll) <= 1, line + ": 1/" + n + " is not the per-roll 1/" + perRoll);
            checked++;
        }
        assertTrue(checked >= 5, "checked " + checked);
    }

    @Test
    void patchesTargetRealBossesAndItems() {
        Map<String, List<Drop>> drops = bossDrops();
        Set<String> bad = new TreeSet<>();
        for (Map.Entry<String, MonsterDropsTable.Patch> e : BossDropsTable.patches().entrySet()) {
            if (!drops.containsKey(e.getKey())) {
                bad.add("unknown boss " + e.getKey());
            }
            for (Drop d : e.getValue().put()) {
                boolean ok = d.item().startsWith("#") ? DropTables.MARKERS.contains(d.item()) : ItemCatalog.exists(d.baseItem());
                if (!ok || d.rarity() < 1 || d.min() < 1 || d.min() > d.max() || d.item().startsWith("clue_scroll")
                        || d.item().startsWith("pet_")) {
                    bad.add(e.getKey() + ": " + d);
                }
            }
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }

    @Test
    void referenceBossRates() {
        Map<String, List<Drop>> drops = bossDrops();
        List<Drop> vorkath = drops.get("vorkath");
        assertEquals(-1, rarity(vorkath, "runite_ore"));              // invented line removed
        assertEquals(50, rarity(vorkath, "vorkaths_head"));           // uniques the dump lacks are kept
        assertEquals(5000, rarity(vorkath, "draconic_visage"));
        assertTrue(vorkath.contains(new Drop("wrath_rune", 30, 60, 50)));
        List<Drop> corp = drops.get("corporeal_beast");
        assertEquals(64, rarity(corp, "spirit_shield"));
        assertEquals(171, rarity(corp, "holy_elixir"));
        assertTrue(rarity(corp, "elysian_sigil") > 0);
        assertEquals(-1, rarity(corp, "shark"));
        // The built-in generals keep the hilts and shards from GwdBosses, not the patch.
        List<Drop> graardor = Boss.GENERAL_GRAARDOR.drops();
        assertEquals(381, rarity(graardor, "bandos_chestplate"));
        assertEquals(-1, rarity(graardor, "bandos_hilt"));
        // Potions keep the wiki's doses.
        assertTrue(drops.values().stream().flatMap(List::stream).anyMatch(d -> d.doses() > 0));
        // Phantom Muspah's three ancient essence lines.
        assertEquals(3, drops.get("phantom_muspah").stream().filter(d -> d.item().equals("ancient_essence")).count());
        // Clue scrolls are never patched away.
        for (BossDef b : ContentRegistry.bosses()) {
            MonsterDropsTable.Patch p = BossDropsTable.patches().get(b.id);
            if (p != null) {
                assertFalse(p.remove().stream().anyMatch(i -> i.startsWith("clue_scroll")), b.id);
            }
        }
    }
}
