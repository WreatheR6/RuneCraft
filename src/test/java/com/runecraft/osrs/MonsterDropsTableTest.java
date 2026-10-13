package com.runecraft.osrs;

import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;
import com.runecraft.osrs.data.DropTables;
import com.runecraft.osrs.data.Monster;
import com.runecraft.osrs.data.MonsterDropsTable;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The wiki drop-table sweep ({@link MonsterDropsTable}) and the shared herb/seed tables it rolls into. */
class MonsterDropsTableTest {
    private static MonsterDef monster(String id) {
        for (MonsterDef m : ContentRegistry.monsters()) {
            if (m.id.equals(id)) {
                return m;
            }
        }
        throw new AssertionError("no monster " + id);
    }

    /** Rarity of the single line for {@code item}, or -1 when there is none. */
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
    void everyPatchTargetsARealMonsterAndItem() {
        Set<String> ids = new HashSet<>();
        ContentRegistry.monsters().forEach(m -> ids.add(m.id));
        for (Monster m : Monster.values()) {
            ids.add(MonsterDropsTable.key(m));
        }
        Set<String> bad = new TreeSet<>();
        for (Map.Entry<String, MonsterDropsTable.Patch> e : MonsterDropsTable.patches().entrySet()) {
            if (!ids.contains(e.getKey())) {
                bad.add("unknown monster " + e.getKey());
            }
            for (Drop d : e.getValue().put()) {
                boolean ok = d.item().startsWith("#") ? DropTables.MARKERS.contains(d.item()) : ItemCatalog.exists(d.item());
                if (!ok || d.rarity() < 1 || d.min() < 1 || d.min() > d.max()) {
                    bad.add(e.getKey() + ": " + d);
                }
                if (d.item().startsWith("clue_scroll")) {
                    bad.add(e.getKey() + " touches clue scrolls (owned by the clue pass)");
                }
            }
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }

    @Test
    void sharedTableMarkersAreAllRollable() {
        for (MonsterDef m : ContentRegistry.monsters()) {
            for (Drop d : m.drops) {
                if (d.item().startsWith("#")) {
                    assertTrue(DropTables.MARKERS.contains(d.item()), m.id + " uses " + d.item());
                }
            }
        }
        for (Monster m : Monster.values()) {
            for (Drop d : m.drops()) {
                if (d.item().startsWith("#")) {
                    assertTrue(DropTables.MARKERS.contains(d.item()), m + " uses " + d.item());
                }
            }
        }
    }

    @Test
    void herbAndSeedTablesMatchTheWiki() {
        for (String t : DropTables.MARKERS) {
            int total = DropTables.table(t).stream().mapToInt(DropTables.Entry::weight).sum();
            assertTrue(total <= DropTables.denominator(t), t + " weighs " + total);
        }
        assertEquals(128, DropTables.HERB.stream().mapToInt(DropTables.Entry::weight).sum());
        assertEquals(1008, DropTables.ALLOTMENT_SEEDS.stream().mapToInt(DropTables.Entry::weight).sum());
        assertEquals(32, DropTables.HERB.get(0).weight());
        assertEquals("grimy_guam_leaf", DropTables.HERB.get(0).item());
        // The herb drop table never gives toadflax, snapdragon or torstol.
        assertFalse(DropTables.HERB.stream().anyMatch(e -> e.item().matches("grimy_(toadflax|snapdragon|torstol)")));
        // Every roll of the herb table gives a herb.
        for (int r = 0; r < 128; r++) {
            final int roll = r;
            assertNotNull(DropTables.roll("#herbs", bound -> roll));
        }
        DropTables.Result potato = DropTables.roll("#allotment_seeds", bound -> 0);
        assertEquals("potato_seed", potato.item());
        assertEquals(4, potato.count());
    }

    @Test
    void referenceMonsterRates() {
        List<Drop> moss = Monster.MOSS_GIANT.drops();
        assertEquals(32, rarity(moss, "#gems"));            // was 1/64
        assertEquals(26, rarity(moss, "#herbs"));           // 1/25.6, was 1/16
        assertEquals(4, rarity(moss, "#rare_seeds"));       // rare seed table 1/3.7
        assertEquals(-1, rarity(moss, "#seeds"));
        assertEquals(-1, rarity(moss, "mithril_scimitar")); // not on the wiki table
        assertEquals(150, rarity(moss, "mossy_key"));
        assertEquals(5000, rarity(moss, "giant_champion_scroll"));

        List<Drop> hill = Monster.HILL_GIANT.drops();
        assertEquals(43, rarity(hill, "#gems"));            // 1/42.7, was 1/128
        assertEquals(7, rarity(hill, "#allotment_seeds"));
        assertEquals(12, rarity(hill, "limpwurt_root"));
        assertEquals(128, rarity(hill, "giant_key"));
        assertEquals(1, rarity(hill, "big_bones"));

        List<Drop> lesser = Monster.LESSER_DEMON.drops();
        assertEquals(128, rarity(lesser, "#herbs"));        // was 1/12
        assertEquals(128, rarity(lesser, "rune_med_helm"));
        assertEquals(-1, rarity(lesser, "rune_full_helm"));

        MonsterDef abyssal = monster("abyssal_demon");
        assertEquals(512, rarity(abyssal.drops, "abyssal_whip"));
        assertEquals(65, rarity(abyssal.drops, "#rare"));
        assertEquals(26, rarity(abyssal.drops, "#gems"));

        MonsterDef hob = monster("hobgoblin");
        assertEquals(6, rarity(hob.drops, "limpwurt_root")); // was 1/18

        MonsterDef turoth = monster("turoth");
        assertEquals(500, rarity(turoth.drops, "leaf_bladed_sword"));
        assertEquals(7, rarity(turoth.drops, "#uncommon_seeds"));

        assertEquals(1, rarity(monster("giant_bat").drops, "bat_bones"));
        assertEquals(-1, rarity(monster("giant_bat").drops, "bones"));
    }

    /** Drop lines the sweep skipped because the item was missing (or named differently) are now wired. */
    @Test
    void previouslySkippedDrops() {
        MonsterDef wb88 = monster("dagannoth_lvl88");
        assertEquals(2048, rarity(wb88.drops, "berserker_helm"));
        assertEquals(2048, rarity(wb88.drops, "warrior_helm"));
        assertEquals(64, rarity(wb88.drops, "spined_gloves"));
        assertEquals(64, rarity(wb88.drops, "fremennik_brown_shirt"));
        assertEquals(26, rarity(wb88.drops, "oyster_pearls"));    // 5/128 oyster pearls, not a single pearl
        assertEquals(-1, rarity(wb88.drops, "oyster_pearl"));
        assertEquals(43, rarity(monster("dagannoth_lvl90").drops, "fremennik_brown_cloak"));
        assertEquals(64, rarity(monster("giant_rock_crab").drops, "rock_shell_gloves"));
        assertEquals(64, rarity(monster("giant_rock_crab").drops, "rock_shell_boots"));
        assertEquals(64, rarity(monster("wallasalki").drops, "skeletal_boots"));
        assertEquals(128, rarity(monster("terror_dog").drops, "granite_helm"));
        assertEquals(400, rarity(Monster.HILL_GIANT.drops(), "long_bone"));
        assertEquals(5012, rarity(Monster.HILL_GIANT.drops(), "curved_bone"));
        assertEquals(43, rarity(Monster.DARK_WIZARD.drops(), "black_robe"));
        // Monster-specific bones replace the plain/big bones stand-ins.
        assertEquals(1, rarity(monster("wyrm").drops, "wyrm_bones"));
        assertEquals(-1, rarity(monster("wyrm").drops, "bones"));
        assertEquals(1, rarity(monster("drake").drops, "drake_bones"));
        assertEquals(1, rarity(monster("maniacal_monkey").drops, "small_ninja_monkey_bones"));
        assertEquals(-1, rarity(monster("maniacal_monkey").drops, "bones"));
        assertEquals(1, rarity(monster("monkey_zombie_lvl129").drops, "large_zombie_monkey_bones"));
        assertEquals(-1, rarity(monster("monkey_zombie_lvl129").drops, "big_bones"));
        assertEquals(1, rarity(monster("monkey_guard").drops, "gorilla_bones"));
        assertEquals(1, rarity(monster("jogre").drops, "jogre_bones"));
        assertEquals(80.0, com.runecraft.osrs.data.Bone.byItem("drake_bones").xp());
        assertEquals(16.0, com.runecraft.osrs.data.Bone.byItem("small_ninja_monkey_bones").xp());
        assertEquals(ItemDef.Kind.BONES, ItemCatalog.get("gorilla_bones").kind());
        // Javelin "tips" on the wiki are RuneCraft's javelin heads.
        assertTrue(rarity(monster("rune_dragon").drops, "dragon_javelin_heads") > 0);
        assertTrue(rarity(monster("adamant_dragon").drops, "adamant_javelin_heads") > 0);
        // Ancient ceremonial robes only come from the Ancient Prison, not the Wilderness spiritual creatures.
        assertEquals(-1, rarity(monster("spiritual_warrior").drops, "ancient_ceremonial_top"));
        assertEquals(640, rarity(monster("blood_reaver").drops, "ancient_ceremonial_top"));
    }

    @Test
    void patchesKeepTertiaryClueDrops() {
        for (MonsterDef m : ContentRegistry.monsters()) {
            if (MonsterDropsTable.patch(m.id) != null) {
                assertTrue(MonsterDropsTable.patch(m.id).remove().stream().noneMatch(i -> i.startsWith("clue_scroll")), m.id);
            }
        }
        // Applying a patch keeps lines it does not name, in order.
        List<Drop> before = List.of(Drop.always("bones"), Drop.of("clue_scroll_hard", 1, 1, 128), Drop.of("mithril_scimitar", 1, 1, 128));
        List<Drop> after = MonsterDropsTable.patched("Monster.MOSS_GIANT", before);
        assertTrue(after.contains(Drop.of("clue_scroll_hard", 1, 1, 128)));
        assertFalse(after.stream().anyMatch(d -> d.item().equals("mithril_scimitar")));
    }
}
