package com.runecraft.osrs;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.dungeons.BarrowsRun;
import com.runecraft.osrs.content.minigames.MinigameRules;
import com.runecraft.osrs.content.systems.Libation;
import com.runecraft.osrs.content.systems.LootingBag;
import com.runecraft.osrs.data.Drop;
import com.runecraft.osrs.data.Potion;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Round 29: looting bag storage, the Barrows chest table, Yama/Maggot King/Mimic tables, holy wrench and bone shards. */
class Round29ItemsTest {
    private static List<Drop> boss(String id) {
        for (BossDef b : ContentRegistry.bosses()) {
            if (b.id.equals(id)) {
                return b.drops;
            }
        }
        throw new AssertionError("no boss " + id);
    }

    private static Drop line(List<Drop> drops, String item) {
        return drops.stream().filter(d -> d.item().equals(item)).findFirst().orElse(null);
    }

    @Test
    void lootingBagHolds28StacksOfTradeablesInTheWilderness() {
        assertTrue(LootingBag.canStore(true, 1));
        assertFalse(LootingBag.canStore(true, 0), "only in the Wilderness");
        assertFalse(LootingBag.canStore(false, 30), "only tradeable items");
        LootingBag.Contents bag = new LootingBag.Contents();
        assertEquals(500, bag.store("death_rune", "", false, true, 500));
        assertEquals(250, bag.store("death_rune", "", false, true, 250));
        assertEquals(1, bag.slotsUsed(), "stackable items share a slot");
        assertEquals(10, bag.store("lobster", "", true, false, 10));
        assertEquals(2, bag.slotsUsed(), "noted items stack");
        assertEquals(26, bag.store("rune_platebody", "", false, false, 30), "unstackable items take a slot each");
        assertEquals(LootingBag.CAPACITY, bag.slotsUsed());
        assertEquals(0, bag.store("chaos_rune", "", false, true, 5), "a full bag takes no new stack");
        assertEquals(5, bag.store("death_rune", "", false, true, 5), "an existing stack still grows");
        assertEquals(755, bag.count("death_rune"));
        List<LootingBag.Stack> all = bag.takeAll();
        assertTrue(bag.isEmpty());
        assertEquals(LootingBag.CAPACITY, all.size());
        assertNotNull(ItemCatalog.get(LootingBag.ID));
    }

    /** OSRS Wiki "Chest (Barrows)": the reward potential bands and quantity ranges at full potential. */
    @Test
    void barrowsChestMatchesTheWikiTable() throws Exception {
        try (BufferedReader r = java.nio.file.Files.newBufferedReader(java.nio.file.Path.of("src/test/resources/osrs/items/barrows_chest.tsv"))) {
            String l;
            int rows = 0;
            while ((l = r.readLine()) != null) {
                if (l.startsWith("#") || l.isBlank()) {
                    continue;
                }
                String[] f = l.split("\t");
                int from = Integer.parseInt(f[1]);
                int to = Integer.parseInt(f[2]);
                BarrowsRun.Band b = BarrowsRun.band(from);
                assertNotNull(b, f[0]);
                assertEquals(f[0], b.item());
                assertEquals(to, b.to(), f[0]);
                assertEquals(Integer.parseInt(f[3]), b.quantity(from), f[0] + " minimum");
                assertEquals(Integer.parseInt(f[4]), b.quantity(to), f[0] + " maximum");
                assertNotNull(ItemCatalog.get(b.item()), b.item());
                rows++;
            }
            assertEquals(BarrowsRun.BANDS.size(), rows);
        }
        assertEquals(1012, BarrowsRun.MAX_POTENTIAL);
        assertEquals(1000 + 12, BarrowsRun.of(63, 63, 0, 1L, true, 0, 40, 900).rewardPotential());
        // One roll per brother plus one; no piece twice in a chest; only killed brothers' pieces.
        Random random = new Random(7);
        int pieces = 0;
        for (int i = 0; i < 20000; i++) {
            Set<String> seen = new HashSet<>();
            for (BarrowsRun.Reward rw : BarrowsRun.of(63, 63, 0, i, true, 0, 30, 400).rollRewards(random)) {
                assertTrue(seen.add(rw.item()), "duplicate " + rw.item());
                if (BarrowsRun.band(1) != null && BarrowsRun.BANDS.stream().noneMatch(b -> b.item().equals(rw.item()))
                        && !rw.item().equals("clue_scroll_elite")) {
                    pieces++;
                }
            }
        }
        // 7 rolls at 1/102: about 20000 * 7 / 102 = 1373 pieces.
        assertTrue(pieces > 1200 && pieces < 1550, "pieces " + pieces);
    }

    @Test
    void yamaAndMaggotKingCommonsComeFromTheirWikiTables() {
        List<Drop> yama = boss("yama");
        assertEquals(12, line(yama, "dossier").rarity());
        assertEquals(33, line(yama, "forgotten_lockbox").rarity());
        assertEquals(17, line(yama, "oathplate_shards").rarity());
        assertEquals(850, line(yama, "aether_catalyst").min());
        assertEquals(40000, line(yama, "fire_rune").min());
        assertEquals(30, line(yama, "clue_scroll_elite").rarity());
        assertNull(line(yama, "coins"), "Yama drops no coins");
        assertNull(line(yama, "dragon_bones"));
        List<Drop> mk = boss("maggot_king");
        assertEquals(4, line(mk, "stymphike_tartare").rarity());
        assertEquals(13, line(mk, "tarnished_spear").rarity());
        assertEquals(178, line(mk, "venator_tooth").min());
        assertEquals(40, line(mk, "clue_scroll_elite").rarity());
        assertNull(line(mk, "coins"), "the Maggot King drops no coins");
        for (List<Drop> table : List.of(yama, mk, boss("the_mimic"))) {
            assertFalse(table.isEmpty());
            for (Drop d : table) {
                assertNotNull(ItemCatalog.get(d.baseItem()), d.item());
            }
        }
    }

    @Test
    void mimicUsesTheMasterTable() {
        List<Drop> m = boss("the_mimic");
        assertEquals(1, line(m, "mahogany_plank").rarity());
        assertNull(line(m, "ring_of_3rd_age"), "the pre-roll is MimicRewards'");
        Random random = new Random(3);
        int rings = 0;
        int pieces = 0;
        for (int i = 0; i < 228_000; i++) {
            String u = com.runecraft.osrs.content.systems.MimicRewards.roll(random);
            if (u == null) {
                continue;
            }
            assertNotNull(ItemCatalog.get(u), u);
            if (u.equals("ring_of_3rd_age")) {
                rings++;
            } else {
                pieces++;
            }
        }
        assertTrue(rings > 5500 && rings < 6500, "6/228: " + rings);
        assertTrue(pieces > 850 && pieces < 1150, "1/228: " + pieces);
        assertEquals(600, line(m, "death_rune").min());
        assertEquals(15, line(m, "raw_manta_ray").max());
        assertNull(line(m, "coins"));
        assertNull(line(m, "dragon_scimitar"));
    }

    /** OSRS Wiki "Holy wrench" restore table, every row. */
    @Test
    void holyWrenchAddsTwoPercentOfPrayer() throws Exception {
        try (BufferedReader r = java.nio.file.Files.newBufferedReader(java.nio.file.Path.of("src/test/resources/osrs/items/holy_wrench_restore.tsv"))) {
            String l;
            int rows = 0;
            while ((l = r.readLine()) != null) {
                if (l.startsWith("#") || l.isBlank()) {
                    continue;
                }
                String[] f = l.split("\t");
                String[] range = f[0].split("-");
                for (int lvl = Integer.parseInt(range[0]); lvl <= Integer.parseInt(range[range.length - 1]); lvl++) {
                    SkillSet s = new SkillSet();
                    s.setXpTenths(Skill.PRAYER, Experience.xpForLevel(lvl) * 10);
                    assertEquals(Integer.parseInt(f[1]), Potion.PRAYER.apply(s), "prayer potion at " + lvl);
                    assertEquals(Integer.parseInt(f[3]), Potion.wrenchBonus(lvl, 25), "wrench 25% at " + lvl);
                    assertEquals(Integer.parseInt(f[4]), com.runecraft.osrs.content.skilling.HerbloreData.Effect.SANFEW.apply(s),
                            "sanfew at " + lvl);
                    assertEquals(Integer.parseInt(f[5]), Potion.wrenchBonus(lvl, 30), "wrench 30% at " + lvl);
                }
                rows++;
            }
            assertTrue(rows > 70);
        }
        assertEquals("holy_wrench", MinigameRules.Imbues.tool("ring_of_the_gods"));
        assertNull(MinigameRules.Imbues.tool("berserker_ring"));
        assertNotNull(ItemCatalog.get("holy_wrench"));
    }

    @Test
    void blessedBoneShardSacrifices() {
        Libation.Offer o = Libation.offer(250, 0, 1);
        assertEquals(100, o.shards(), "100 per action");
        assertEquals(1, o.jugsUsed());
        assertEquals(300, o.allowanceLeft(), "a jug allows 400 shards");
        assertEquals(500.0, o.xp(), 1e-9, "5 XP a shard");
        Libation.Offer none = Libation.offer(50, 0, 0);
        assertEquals(0, none.shards(), "no wine, no sacrifice");
        Libation.Offer rest = Libation.offer(30, 300, 0);
        assertEquals(30, rest.shards());
        assertEquals(270, rest.allowanceLeft());
    }
}
