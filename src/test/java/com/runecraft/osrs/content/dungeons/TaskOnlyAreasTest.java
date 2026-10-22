package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.MultiCombat;
import com.runecraft.osrs.content.slayer.SlayerCategories;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Slayer task-only areas (OSRS Wiki "Slayer guardians" and each dungeon's page, July 2026): every one holds the wiki's
 * monsters in the right numbers, is guarded by its guardian, and its public neighbours stay open to all.
 */
class TaskOnlyAreasTest {
    /** Monster counts (camp caps) of the camps whose centre lies in a task-only area of the dungeon, by creature. */
    private static Map<String, Integer> taskOnlyCaps(String dungeon, boolean taskOnly) {
        Map<String, Integer> out = new HashMap<>();
        for (Dungeons.Camp c : Dungeons.camps()) {
            if (!c.dungeon().equals(dungeon)) {
                continue;
            }
            Dungeons.TaskOnlyArea a = Dungeons.taskOnlyArea(c.centreX(), c.feetY() + 1, c.centreZ());
            if ((a != null) == taskOnly) {
                for (String m : c.creatures()) {
                    out.merge(m, c.cap(), Integer::sum);
                }
            }
        }
        return out;
    }

    private static Dungeons.TaskOnlyArea areaOf(String dungeon, String creature, boolean taskOnly) {
        for (Dungeons.Camp c : Dungeons.camps()) {
            if (c.dungeon().equals(dungeon) && c.creatures().contains(creature)) {
                Dungeons.TaskOnlyArea a = Dungeons.taskOnlyArea(c.centreX(), c.feetY() + 1, c.centreZ());
                if ((a != null) == taskOnly) {
                    return a;
                }
            }
        }
        throw new AssertionError("no " + (taskOnly ? "task-only " : "open ") + creature + " camp in " + dungeon);
    }

    @Test
    void karuulmTaskOnlyRooms() {
        String d = "dng_karuulm_slayer_dungeon";
        assertEquals(Map.of("wyrm", 7, "hydra", 8, "drake", 7), taskOnlyCaps(d, true));
        assertEquals(Map.of("wyrm", 9, "hydra", 9, "drake", 7, "sulphur_lizard", 13, "greater_demon", 9, "fire_giant", 13,
                "hellhound", 12), taskOnlyCaps(d, false));
        assertEquals("Garbek quo Maten", areaOf(d, "wyrm", true).guardian());
        assertEquals("wyrms", areaOf(d, "wyrm", true).task());
        assertEquals("Tarfol quo Maten", areaOf(d, "hydra", true).guardian());
        assertEquals("Kormar quo Maten", areaOf(d, "drake", true).guardian());
    }

    @Test
    void wildernessSlayerCaveTaskOnlyRooms() {
        String d = "dng_wilderness_slayer_cave";
        assertEquals(Map.of("abyssal_demon", 8, "dust_devil", 9, "greater_nechryael", 8, "jelly", 14), taskOnlyCaps(d, true));
        for (String m : List.of("abyssal_demon", "dust_devil", "greater_nechryael", "jelly")) {
            Dungeons.TaskOnlyArea a = areaOf(d, m, true);
            assertEquals("Peeve", a.guardian());
            // the whole cave is multicombat, the new rooms too
            assertTrue(MultiCombat.isMultiAtBlock(a.centreX(), a.y1() + 1, a.centreZ()), m);
        }
        assertTrue(taskOnlyCaps(d, false).containsKey("lesser_demon"), "the lesser demons stay open to all");
    }

    @Test
    void taverleyUpperLevel() {
        String d = "dng_taverley_dungeon";
        assertEquals(Map.of("baby_black_dragon", 5, "black_dragon", 12, "blue_dragon", 12), taskOnlyCaps(d, true));
        assertEquals("Slieve", areaOf(d, "baby_black_dragon", true).guardian());
        assertEquals("black_dragons", areaOf(d, "black_dragon", true).task());
        assertEquals("Eve", areaOf(d, "blue_dragon", true).guardian());
        // The main level keeps its blue dragons and two black dragons open to all.
        Map<String, Integer> open = taskOnlyCaps(d, false);
        assertEquals(2, open.get("black_dragon"));
        assertTrue(open.containsKey("blue_dragon"));
        assertFalse(open.containsKey("baby_black_dragon"), "the baby black dragons live upstairs");
    }

    @Test
    void strongholdSlayerCaveIsTaskOnlyThroughout() {
        String d = "dng_stronghold_slayer_cave";
        assertEquals(Map.of("bloodveld", 12, "ankou", 8, "aberrant_spectre", 13, "hellhound", 13, "fire_giant", 12),
                taskOnlyCaps(d, true));
        assertTrue(taskOnlyCaps(d, false).isEmpty());
        Dungeons.TaskOnlyArea a = areaOf(d, "hellhound", true);
        assertEquals(Dungeons.ANY_TASK, a.task());
        assertEquals("Nieve", a.guardian());
    }

    @Test
    void morytaniaSpiderCave() {
        String d = "dng_morytania_spider_cave";
        assertEquals(Map.of("araxyte", 12, "araxyte_lvl146", 21), taskOnlyCaps(d, true));
        assertEquals(Map.of("araxyte", 15, "araxyte_lvl146", 7), taskOnlyCaps(d, false));
        assertEquals("Weave", areaOf(d, "araxyte", true).guardian());
        // Multicombat everywhere but the northern task-only chamber (cache tiles 3663-3702, 9850-9862).
        int x = com.runecraft.osrs.world.StructureDef.bx(3680);
        assertFalse(MultiCombat.isMultiAtBlock(x, 17, com.runecraft.osrs.world.StructureDef.bz(9856 - 6400)), "north chamber");
        assertTrue(MultiCombat.isMultiAtBlock(x, 17, com.runecraft.osrs.world.StructureDef.bz(9830 - 6400)), "main cavern");
        assertTrue(MultiCombat.isMultiAtBlock(com.runecraft.osrs.world.StructureDef.bx(3700), 17,
                com.runecraft.osrs.world.StructureDef.bz(9840 - 6400)), "middle chamber");
    }

    @Test
    void smokeDungeonWest() {
        String d = "dng_smoke_dungeon";
        assertEquals(Map.of("dust_devil", 13), taskOnlyCaps(d, true));
        assertEquals(Map.of("dust_devil", 31, "pyrefiend", 23, "fire_giant", 6), taskOnlyCaps(d, false));
        Dungeons.TaskOnlyArea a = areaOf(d, "dust_devil", true);
        assertEquals("Dusty Aliv", a.guardian());
        assertTrue(MultiCombat.isMultiAtBlock(a.centreX(), a.y1() + 1, a.centreZ()), "the task-only cavern is multicombat");
        Dungeons.TaskOnlyArea none = Dungeons.taskOnlyArea(a.centreX() + 80, a.y1() + 1, a.centreZ());
        assertNull(none);
    }

    @Test
    void brimhavenMetalDragons() {
        String d = "dng_brimhaven_dungeon";
        assertEquals(Map.of("bronze_dragon", 5, "iron_dragon", 8, "steel_dragon", 9), taskOnlyCaps(d, true));
        Map<String, Integer> open = taskOnlyCaps(d, false);
        assertEquals(3, open.get("bronze_dragon"));
        assertEquals(13, open.get("iron_dragon"));
        assertEquals(4, open.get("steel_dragon"));
        assertEquals("Hieve", areaOf(d, "steel_dragon", true).guardian());
        assertEquals("metal_dragons", areaOf(d, "steel_dragon", true).task());
    }

    @Test
    void fossilIslandWyvernCaves() {
        assertEquals(Map.of("spitting_wyvern", 4, "taloned_wyvern", 4, "long_tailed_wyvern", 4, "ancient_wyvern", 3),
                taskOnlyCaps("dng_wyvern_cave", true));
        assertTrue(taskOnlyCaps("dng_wyvern_cave", false).isEmpty());
        assertEquals(Map.of("spitting_wyvern", 4, "taloned_wyvern", 4, "long_tailed_wyvern", 4, "ancient_wyvern", 2, "ice_spider", 5),
                taskOnlyCaps("dng_wyvern_cave_south", false));
        assertEquals("Weve", areaOf("dng_wyvern_cave", "ancient_wyvern", true).guardian());
    }

    @Test
    void grimstoneFrostDragons() {
        // OSRS Wiki "Grimstone Dungeon" (3 December 2025): the western cave's frost dragons are task-only, without a
        // guardian but with their own line; the central cavern's and the chilled jellies are open to all.
        String d = "dng_grimstone_dungeon";
        assertEquals(Map.of("frost_dragon", 7), taskOnlyCaps(d, true));
        assertEquals(Map.of("frost_dragon", 13, "chilled_jelly", 10), taskOnlyCaps(d, false));
        Dungeons.TaskOnlyArea a = areaOf(d, "frost_dragon", true);
        assertEquals("frost_dragons", a.task());
        assertNull(a.guardian());
        assertTrue(Dungeons.TASK_ONLY_LINES.get(d).startsWith("You feel a strange coldness"));
        assertFalse(MultiCombat.isMultiAtBlock(a.centreX(), a.y1() + 1, a.centreZ()), "single-way");
    }

    @Test
    void greatConchGryphonCaves() {
        // OSRS Wiki "Gryphons (dungeon)": the south-eastern cave is task-only (Elder Kelmo), the north-western one open;
        // the southern room of each (the south-western part of the eastern cave) is multicombat.
        assertEquals(Map.of("gryphon", 18), taskOnlyCaps("dng_gryphon_cave_east", true));
        assertTrue(taskOnlyCaps("dng_gryphon_cave_east", false).isEmpty());
        assertEquals(Map.of("gryphon", 18), taskOnlyCaps("dng_gryphon_cave_west", false));
        assertTrue(taskOnlyCaps("dng_gryphon_cave_west", true).isEmpty());
        Dungeons.TaskOnlyArea a = areaOf("dng_gryphon_cave_east", "gryphon", true);
        assertEquals("Elder Kelmo", a.guardian());
        assertEquals("gryphons", a.task());
    }

    @Test
    void asgarnianIceDungeonUpperArea() {
        // OSRS Wiki "Asgarnian Ice Dungeon": fourteen more skeletal wyverns up the icy steps, task-only (Pieve).
        String d = "dng_asgarnian_ice_dungeon";
        assertEquals(Map.of("skeletal_wyvern", 14), taskOnlyCaps(d, true));
        assertEquals(4, taskOnlyCaps(d, false).get("skeletal_wyvern"));
        Dungeons.TaskOnlyArea a = areaOf(d, "skeletal_wyvern", true);
        assertEquals("Pieve", a.guardian());
        assertEquals("skeletal_wyverns", a.task());
    }

    @Test
    void fremennikSlayerDungeonKurask() {
        // OSRS Wiki "Fremennik Slayer Dungeon": 5 kurask in the open chamber, 9 in the task-only cavern (Jelly).
        String d = "dng_fremennik_slayer_dungeon";
        assertEquals(Map.of("kurask", 9), taskOnlyCaps(d, true));
        assertEquals(5, taskOnlyCaps(d, false).get("kurask"));
        Dungeons.TaskOnlyArea a = areaOf(d, "kurask", true);
        assertEquals("Jelly", a.guardian());
        assertEquals("kurask", a.task());
    }

    @Test
    void chasmOfFireIsOpenToAll() {
        // OSRS Wiki "Chasm of Fire": not task-only, with 9 lesser, 17 greater and 16 black demons.
        assertEquals(Map.of("lesser_demon", 9, "greater_demon", 17, "black_demon", 16), taskOnlyCaps("dng_chasm_of_fire", false));
        assertTrue(taskOnlyCaps("dng_chasm_of_fire", true).isEmpty());
    }

    @Test
    void everyAreaNamesARealTaskAndGuardian() {
        assertFalse(Dungeons.taskOnlyAreas().isEmpty());
        for (Dungeons.TaskOnlyArea a : Dungeons.taskOnlyAreas()) {
            // a guardian speaks for the area, or (Grimstone's frost dragons) the area has its own line
            assertTrue(a.guardian() != null || Dungeons.TASK_ONLY_LINES.containsKey(a.dungeon()), a.toString());
            assertTrue(Dungeons.ANY_TASK.equals(a.task()) || SlayerCategories.get(a.task()) != null, a.task());
            assertTrue(a.y2() <= Dungeons.DEEP_TOP, a.toString());
        }
        for (Map.Entry<String, String> e : Dungeons.TASK_ONLY.entrySet()) {
            assertNotNull(Dungeons.GUARDIANS.get(e.getKey()), e.getKey());
            assertTrue(Dungeons.ANY_TASK.equals(e.getValue()) || SlayerCategories.get(e.getValue()) != null, e.getValue());
        }
    }
}
