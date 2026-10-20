package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.minigames.MageTrainingArena.Dir;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Mage Training Arena's rooms against the OSRS Wiki (round 40). */
class MageTrainingArenaTest {
    @Test
    void theStatueSlidesUntilAWallStopsIt() {
        MageTrainingArena.Maze m = MageTrainingArena.parse(
                "S...",
                ".#..",
                "...#",
                "..G.");
        assertArrayEquals(new int[]{3, 0}, m.slide(0, 0, Dir.EAST));
        assertArrayEquals(new int[]{0, 3}, m.slide(0, 0, Dir.SOUTH));
        assertArrayEquals(new int[]{1, 2}, m.slide(1, 3, Dir.NORTH), "stops under the wall");
        List<Dir> path = m.solve();
        assertNotNull(path);
        int[] at = {m.startX(), m.startZ()};
        for (Dir d : path) {
            at = m.slide(at[0], at[1], d);
        }
        assertArrayEquals(new int[]{m.goalX(), m.goalZ()}, at);
        // A goal in the middle of an open row can never be stopped on.
        assertNull(MageTrainingArena.parse("S...", "....", "..G.", "....").solve());
    }

    @Test
    void mazesTakeSevenToTenPulls() {
        for (long seed = 0; seed < 40; seed++) {
            MageTrainingArena.Maze m = MageTrainingArena.maze(new Random(seed));
            List<Dir> path = m.solve();
            assertNotNull(path, "seed " + seed);
            assertTrue(path.size() >= 7 && path.size() <= 10, "seed " + seed + ": " + path.size());
            assertEquals(MageTrainingArena.MAZE_SIZE, m.size());
        }
        // 2 points a maze, 10 on every fifth; 250 XP, 1,000 more on the fifth (OSRS Wiki "Telekinetic Theatre").
        assertEquals(2, MageTrainingArena.mazePoints(1));
        assertEquals(10, MageTrainingArena.mazePoints(5));
        assertEquals(250, MageTrainingArena.MAZE_XP);
        assertEquals(43, MageTrainingArena.TELEKINETIC_GRAB.xp(), 1e-9);
    }

    @Test
    void cupboardsKeepTheirClockwiseOrder() {
        Random r = new Random(7);
        for (int i = 0; i < 30; i++) {
            MageTrainingArena.Cupboards c = MageTrainingArena.rotate(r);
            int empty = 0;
            List<String> order = new ArrayList<>();
            int start = c.cupboardOf(MageTrainingArena.ALCHEMY_ITEMS.get(0));
            for (int k = 0; k < MageTrainingArena.CUPBOARDS; k++) {
                String item = c.item((start + k) % MageTrainingArena.CUPBOARDS);
                if (item == null) {
                    empty++;
                } else {
                    order.add(item);
                }
            }
            assertEquals(1, empty);
            assertEquals(MageTrainingArena.ALCHEMY_ITEMS, order, "boots, kiteshield, helm, emerald, longsword, empty");
            int[] values = MageTrainingArena.ALCHEMY_ITEMS.stream().mapToInt(c::value).sorted().toArray();
            assertArrayEquals(MageTrainingArena.VALUES, values);
        }
        assertEquals(0, MageTrainingArena.rotate(r).value("rune_longsword"));
        for (String item : MageTrainingArena.ALCHEMY_ITEMS) {
            assertTrue(ItemCatalog.exists(item), item);
        }
    }

    @Test
    void enchantingAndGraveyard() {
        assertEquals(7, MageTrainingArena.enchantLevel(93));
        assertEquals(6, MageTrainingArena.enchantLevel(92));
        assertEquals(0, MageTrainingArena.enchantLevel(6));
        assertEquals(110, MageTrainingArena.ENCHANTS.get(6).xp(), 1e-9);
        assertEquals(10, MageTrainingArena.ENCHANTS.get(3).runes().get("earth_rune"));
        // Points: Lvl-4 gives 4, 8 on the tenth shape, +2 for the bonus shape after doubling, 8 on a dragonstone.
        assertEquals(4, MinigameRules.MageArena.enchantPoints(4, 3, false, false));
        assertEquals(10, MinigameRules.MageArena.enchantPoints(4, 10, true, false));
        assertEquals(8, MinigameRules.MageArena.enchantPoints(4, 0, false, true));
        assertEquals(40, MageTrainingArena.BONUS_SHAPE_TICKS);
        for (String s : MageTrainingArena.SHAPES) {
            assertTrue(ItemCatalog.exists(s), s);
        }
        assertTrue(ItemCatalog.exists(MageTrainingArena.DRAGONSTONE) && ItemCatalog.exists(MageTrainingArena.ORB));
        // Each pile gives four bones of one kind, then the next kind (1 to 4 fruit each).
        List<String> taken = new ArrayList<>();
        for (int n = 0; n < 16; n++) {
            taken.add(MageTrainingArena.pileBone(2, n));
        }
        assertEquals(List.of("graveyard_bones_3", "graveyard_bones_3", "graveyard_bones_3", "graveyard_bones_3"), taken.subList(0, 4));
        assertEquals("graveyard_bones_4", taken.get(4));
        assertEquals("graveyard_bones", taken.get(8));
        assertEquals(new HashSet<>(MageTrainingArena.BONES), new HashSet<>(taken));
        assertEquals(4, MageTrainingArena.fruit("graveyard_bones_4"));
        for (String b : MageTrainingArena.BONES) {
            assertTrue(ItemCatalog.exists(b), b);
        }
        // 16 bananas' worth (a peach counts twice) is a point, at most 3 a deposit.
        assertEquals(1, MinigameRules.MageArena.graveyardPoints(16, false, 0.99));
        assertEquals(3, MinigameRules.MageArena.graveyardPoints(2 * 27, false, 0.0));
        Set<String> runes = MageTrainingArena.randomRunes(MageTrainingArena.GRAVEYARD_RUNES, 50, new Random(1)).keySet();
        assertTrue(MageTrainingArena.GRAVEYARD_RUNES.containsAll(runes));
        assertEquals(60, MageTrainingArena.BONES_TO_PEACHES.level());
    }
}
