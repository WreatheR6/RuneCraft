package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sailing at sea diffed against the OSRS Wiki snapshots in src/test/resources/osrs/sailing (July 2026 dump): salvage
 * sorting loot, sea creatures (Boat combat table and infoboxes), their drops and bounty tasks.
 */
class SailingSeaReferenceTest {

    private static List<String[]> tsv(String name) throws Exception {
        List<String[]> out = new ArrayList<>();
        Path p = Path.of("src/test/resources/osrs/sailing", name);
        assertTrue(Files.exists(p), p.toString());
        for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
            if (!line.isBlank() && !line.startsWith("#")) {
                out.add(line.split("\t", -1));
            }
        }
        return out;
    }

    @Test
    void salvageLootMatchesTheWiki() throws Exception {
        List<String[]> rows = tsv("salvage_loot.tsv");
        assertEquals(192, rows.size());
        int used = 0;
        for (String[] r : rows) {
            double chance = SailingSea.rarity(r[4]);
            String salvage = SailingSea.itemId(r[0]);
            assertNotNull(SailingData.wreckBySalvage(salvage), r[0]);
            boolean found = SailingSea.SALVAGE.get(salvage).stream().anyMatch(l -> l.wikiName().equals(r[2]) && l.section().equals(r[1])
                    && Math.abs(l.chance() - chance) < 1e-12 && l.min() == SailingSea.qty(r[3])[0] && l.max() == SailingSea.qty(r[3])[1]);
            if (SailingSea.SKIPPED.contains(r[2]) || chance <= 0) {
                assertFalse(found, r[2]);
            } else {
                assertTrue(found, r[0] + " " + r[2]);
                used++;
            }
        }
        int total = SailingSea.SALVAGE.values().stream().mapToInt(List::size).sum();
        assertEquals(used, total);
        assertTrue(used >= 175, "lines kept: " + used);
        for (SailingData.Wreck w : SailingData.WRECKS) {
            List<SailingSea.SalvageLine> lines = SailingSea.SALVAGE.get(w.salvage());
            assertNotNull(lines, w.salvage());
            double main = lines.stream().filter(l -> !l.preRoll() && !l.tertiary()).mapToDouble(SailingSea.SalvageLine::chance).sum();
            // Skipped lines (feathers, platinum tokens) and the gem drop table are left out and the rest renormalised.
            assertTrue(main > 0.75 && main < 1.02, w.salvage() + " main table " + main);
            lines.forEach(l -> assertTrue(ItemCatalog.exists(l.item()), w.salvage() + " " + l.item()));
            Random random = new Random(7);
            for (int i = 0; i < 500; i++) {
                assertFalse(SailingSea.sort(w.salvage(), random).isEmpty(), w.salvage());
            }
        }
        // Spot checks (wiki): small salvage gives bronze bars 200/1721 and a beginner clue 1/300.
        assertTrue(SailingSea.SALVAGE.get("small_salvage").stream().anyMatch(l -> l.item().equals("bronze_bar")
                && Math.abs(l.chance() - 200.0 / 1721) < 1e-12));
        assertTrue(SailingSea.SALVAGE.get("small_salvage").stream().anyMatch(l -> l.item().equals("clue_scroll_beginner")
                && Math.abs(l.chance() - 1.0 / 300) < 1e-12));
    }

    @Test
    void seaCreaturesMatchTheWiki() throws Exception {
        List<String[]> rows = tsv("sea_creatures.tsv");
        assertEquals(21, rows.size());
        assertEquals(rows.size(), SailingSea.CREATURES.size());
        int[] armour = {0, 1, 2, 3, 4, 6, 8, 10};
        for (String[] r : rows) {
            SailingSea.SeaCreature c = SailingSea.creature(SailingSea.creatureId(r[0]));
            assertNotNull(c, r[0]);
            assertEquals(Integer.parseInt(r[1]), c.combat(), r[0]);
            assertEquals(Integer.parseInt(r[2]), c.hp(), r[0]);
            assertEquals(Integer.parseInt(r[5]), c.def(), r[0]);
            assertEquals(Integer.parseInt(r[7]), c.speed(), r[0]);
            String[] hits = r[6].split(",");
            assertEquals(8, hits.length, r[0]);
            for (int k = 0; k < 8; k++) {
                assertEquals(Integer.parseInt(hits[k]), c.maxHitAgainst(k), r[0] + " keel " + k);
                // Keels reduce damage by 1 per 100 armour (wiki Keel page).
                assertEquals(Math.max(0, c.maxHitAgainst(0) - armour[k]), c.maxHitAgainst(k), r[0] + " keel " + k);
            }
            assertTrue(c.x() > 1000 && c.y() > 2000, r[0]);
        }
        List<MonsterDef> monsters = SailingSea.monsters();
        assertEquals(21, monsters.size());
        Set<String> ids = new HashSet<>();
        for (MonsterDef m : monsters) {
            assertTrue(ids.add(m.id), m.id);
            for (Drop d : m.drops) {
                assertTrue(ItemCatalog.exists(d.item()), m.id + " drops " + d.item());
            }
        }
        // Bounty parts only drop on a task, so they are not in the monster tables.
        Set<String> parts = SailingSea.bountyParts();
        monsters.forEach(m -> m.drops.forEach(d -> assertFalse(parts.contains(d.item()), m.id + " " + d.item())));
        assertEquals(21, tsv("sea_creature_drops.tsv").stream().map(r -> r[0]).distinct().count());
    }

    @Test
    void bountyTasksMatchTheWiki() throws Exception {
        List<String[]> rows = tsv("bounty_tasks.tsv");
        assertEquals(163, rows.size());
        assertEquals(rows.size(), SailingSea.BOUNTIES.size());
        for (String[] r : rows) {
            SailingSea.Bounty b = SailingSea.bounty(Integer.parseInt(r[0]));
            assertNotNull(b, r[0]);
            assertEquals(Integer.parseInt(r[1]), b.level(), r[0]);
            assertEquals(Double.parseDouble(r[2]), b.xp(), 1e-9, r[0]);
            assertNotNull(SailingData.port(b.board()), r[0] + " board " + r[3]);
            assertNotNull(SailingSea.creature(b.creature()), r[0] + " " + r[4]);
            assertEquals(Integer.parseInt(r[6]), b.qty(), r[0]);
            assertTrue(b.chance() > 0 && b.chance() <= 1, r[0]);
            assertTrue(b.level() >= SailingSea.BOUNTY_LEVEL, r[0]);
            assertTrue(ItemCatalog.exists(b.item()), b.item());
        }
        // Every bounty creature appears on at least one notice board.
        Set<String> boards = new HashSet<>();
        SailingSea.BOUNTIES.forEach(b -> boards.add(b.creature()));
        assertEquals(SailingSea.CREATURES.size() - 1, boards.size()); // dolphins have no bounty
    }

    @Test
    void hooksCannonsAndBallsMatchTheWiki() {
        Map<String, int[]> hooks = Map.of("bronze_salvaging_hook", new int[]{15, 1, 151}, "rune_salvaging_hook", new int[]{74, 66, 1233},
                "dragon_salvaging_hook", new int[]{86, 78, 1537});
        Map<String, int[]> cannons = Map.of("bronze_cannon", new int[]{28, 21, 157}, "mithril_cannon", new int[]{57, 50, 816},
                "dragon_cannon", new int[]{92, 84, 1857});
        int checked = 0;
        for (SailingTraining.Facility f : SailingSea.FACILITIES) {
            f.materials().keySet().forEach(item -> assertTrue(ItemCatalog.exists(item), f.id() + " needs " + item));
            assertNull(SailingTraining.facility(f.id()), "duplicate facility " + f.id());
            int[] e = hooks.containsKey(f.id()) ? hooks.get(f.id()) : cannons.get(f.id());
            if (e != null) {
                assertEquals(e[0], f.sailing(), f.id());
                assertEquals(e[1], f.construction(), f.id());
                assertEquals(e[2], f.conXp(), 1e-9, f.id());
                checked++;
            }
        }
        assertEquals(6, checked);
        assertEquals(15, SailingSea.FACILITIES.size());
        SailingSea.BALLS.forEach(b -> assertTrue(ItemCatalog.exists(b.item()), b.item()));
        // Wiki crewmate formula: floor((99 x (8 + 43 + 64) + 320) / 640) = 18 for a bronze cannon and bronze balls.
        assertEquals(18, SailingSea.cannonMaxHit(99, SailingSea.CANNONS.get(0), SailingSea.ball("bronze_cannonball"), false));
        assertEquals(0, SailingSea.hookTier(Set.of("bronze_salvaging_hook")));
        assertEquals(6, SailingSea.hookTier(Set.of("bronze_salvaging_hook", "dragon_salvaging_hook")));
        SailingData.Wreck small = SailingData.WRECKS.get(0);
        assertTrue(SailingSea.salvageChance(small, 50, 6) > SailingSea.salvageChance(small, 50, 0));
    }

    @Test
    void boatHitpointsEncountersAndHazards() {
        assertEquals(20, SailingSea.boatHp("raft", 0, 0));
        assertEquals(30 + 50, SailingSea.boatHp("skiff", 0, 0));
        assertEquals(240 + 120, SailingSea.boatHp("sloop", 6, 6));
        assertEquals(10, SailingSea.keelReduction("sloop", 6));
        assertEquals(0, SailingSea.keelIndex("raft", 3));
        Random random = new Random(1);
        for (int i = 0; i < 100; i++) {
            assertTrue(SailingSea.encounterSpawns(6, random));
        }
        assertEquals(4088, SailingSea.glowXp(99));
        assertEquals("clue_scroll_easy", SailingSea.clueTurtle(10, random));
        SailingSea.lostCrateItems().forEach(i -> assertTrue(ItemCatalog.exists(i), i));
        assertTrue(ItemCatalog.exists("castaway"));
        for (SailingTraining.ChartTask t : SailingTraining.CHARTS) {
            if (!t.hazard().isEmpty()) {
                assertNotNull(SailingSea.Hazard.byWiki(t.hazard()), t.hazard());
            }
        }
    }
}
