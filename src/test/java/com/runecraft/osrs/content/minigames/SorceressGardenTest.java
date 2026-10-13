package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.minigames.SorceressGarden.Patrol;
import com.runecraft.osrs.content.minigames.SorceressGarden.Season;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SorceressGardenTest {
    @Test
    void seasonsMatchTheWiki() {
        // Level, Farming XP per sq'irk, sq'irks per glass, Thieving boost, run energy, Osman's XP (OSRS Wiki).
        Object[][] wiki = {
                {Season.WINTER, 1, 30.0, 5, 0, 5, 350},
                {Season.SPRING, 25, 40.0, 4, 1, 10, 1350},
                {Season.AUTUMN, 45, 50.0, 3, 2, 15, 2350},
                {Season.SUMMER, 65, 60.0, 2, 3, 20, 3000}};
        for (Object[] w : wiki) {
            Season s = (Season) w[0];
            assertEquals(w[1], s.level(), s + " level");
            assertEquals((Double) w[2], s.farmingXp(), 1e-9, s + " farming xp");
            assertEquals(w[3], s.perGlass(), s + " sq'irks per glass");
            assertEquals(w[4], s.boost(), s + " boost");
            assertEquals(w[5], s.energy(), s + " energy");
            assertEquals(w[6], s.osmanXp(), s + " Osman xp");
        }
        assertEquals(5.0, SorceressGarden.JUICE_COOKING_XP);
        assertEquals(50.0, SorceressGarden.HERB_FARMING_XP);
        assertEquals(2, SorceressGarden.HERBS_PER_PICK);
        // An inventory of 22 winter juices: 8,050 Thieving XP (wiki).
        assertEquals(8050, 23 * Season.WINTER.osmanXp()); // 22 glasses and 5 spare sq'irks
        assertEquals(69000, 23 * Season.SUMMER.osmanXp()); // 22 glasses and 2 spare sq'irks
    }

    @Test
    void herbTablesMatchModAsh() {
        int[] totals = {54, 75, 99, 111};
        int[] sizes = {4, 6, 9, 11};
        for (Season s : Season.values()) {
            int total = s.herbTable().stream().mapToInt(MinigameRules.Loot::weight).sum();
            assertEquals(totals[s.ordinal()], total, s + " herb total");
            assertEquals(sizes[s.ordinal()], s.herbTable().size(), s + " herb count");
        }
        assertEquals("dwarf_weed", Season.SUMMER.herbTable().get(10).item());
        assertEquals(5, Season.SUMMER.herbTable().get(10).weight());
    }

    @Test
    void itemsAndElementalsExist() {
        for (Season s : Season.values()) {
            assertTrue(ItemCatalog.exists(s.fruit()), s.fruit());
            assertTrue(ItemCatalog.exists(s.juice()), s.juice());
            assertNotNull(ContentRegistry.monster(s.elemental()), s.elemental());
            for (MinigameRules.Loot l : s.herbTable()) {
                assertTrue(ItemCatalog.exists(l.item()), l.item());
            }
        }
        assertEquals(Minigame.SORCERESS_GARDEN, Minigame.byId("sorceress_garden"));
        assertEquals(3320, Minigame.SORCERESS_GARDEN.entranceX());
        assertEquals(3139, Minigame.SORCERESS_GARDEN.entranceY());
    }

    @Test
    void rockyChance() {
        assertEquals(1.0 / (10_339 - 25 * 65), SorceressGarden.rockyChance(65), 1e-12);
    }

    @Test
    void patrolsPingPong() {
        Patrol p = new Patrol(2, 0, 5, 0, 1, 0);
        int[] expected = {2, 3, 4, 5, 4, 3, 2, 3};
        for (int t = 0; t < expected.length; t++) {
            assertEquals(expected[t], p.u(t));
            assertEquals(0, p.v(t));
        }
        Patrol across = new Patrol(9, 4, 9, -4, 2, 0);
        assertEquals(List.of(4, 2, 0, -2, -4, -2), List.of(across.v(0), across.v(1), across.v(2), across.v(3), across.v(4),
                across.v(5)));
        assertEquals(9, across.u(3));
    }

    /**
     * Breadth-first search over (square, game tick): a player entering at the gate and moving up to
     * {@code speed} squares per tick must be able to reach the clearing unseen, but not by running
     * straight through without waiting.
     */
    @Test
    void everyGardenCanBeCrossed() {
        int speed = 2; // OSRS running pace (Minecraft sprinting is a little faster)
        int period = 240;
        int goal = SorceressGarden.CORRIDOR + 2;
        for (Season s : Season.values()) {
            for (int[] a : SorceressGarden.alcoves(s)) {
                for (long t = 0; t < period; t++) {
                    assertFalse(SorceressGarden.seen(s, t, a[0], 3 * a[1]), s + " alcove " + a[0] + " is watched at tick " + t);
                }
            }
            for (long t = 0; t < period; t++) {
                assertFalse(SorceressGarden.seen(s, t, goal, 0), s + " clearing is watched at tick " + t);
            }
            int solvable = 0;
            int straight = 0;
            for (int t0 = 0; t0 < period; t0++) {
                if (reachable(s, t0, speed, goal, period)) {
                    solvable++;
                }
                boolean clear = true;
                int u = 1;
                for (long t = t0 + 1; u < goal && clear; t++) {
                    u = Math.min(goal, u + speed);
                    clear = !SorceressGarden.seen(s, t, u, 0);
                }
                if (clear) {
                    straight++;
                }
            }
            assertTrue(solvable > period / 2, s + " can be crossed from only " + solvable + " entry ticks");
            assertEquals(0, straight, s + " can be run straight through");
        }
    }

    private static boolean reachable(Season s, int t0, int speed, int goal, int period) {
        Set<Long> visited = new HashSet<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{1, 0, t0});
        while (!queue.isEmpty()) {
            int[] st = queue.poll();
            if (st[0] >= goal) {
                return true;
            }
            if (st[2] - t0 > 400 || !visited.add(((long) st[0] * 64 + st[1] + 32) * 1000 + st[2] % period)) {
                continue;
            }
            Set<Long> reach = new HashSet<>();
            List<int[]> frontier = new ArrayList<>(List.of(new int[]{st[0], st[1]}));
            reach.add(key(st[0], st[1]));
            for (int step = 0; step < speed; step++) {
                List<int[]> next = new ArrayList<>();
                for (int[] sq : frontier) {
                    for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                        int u = sq[0] + d[0];
                        int v = sq[1] + d[1];
                        if (SorceressGarden.walkable(s, u, v) && reach.add(key(u, v))) {
                            next.add(new int[]{u, v});
                        }
                    }
                }
                frontier = next;
            }
            for (long k : reach) {
                int u = (int) (k >> 8);
                int v = (int) (k & 0xFF) - 32;
                if (!SorceressGarden.seen(s, st[2] + 1, u, v)) {
                    queue.add(new int[]{u, v, st[2] + 1});
                }
            }
        }
        return false;
    }

    private static long key(int u, int v) {
        return ((long) u << 8) | (v + 32);
    }
}
