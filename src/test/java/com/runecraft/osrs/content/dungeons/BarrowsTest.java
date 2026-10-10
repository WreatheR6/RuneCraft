package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.data.Boss;
import com.runecraft.osrs.world.StructureDef;
import com.runecraft.osrs.world.StructureDef.Op;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BarrowsTest {
    private static Dungeons.Dungeon dungeon() {
        for (Dungeons.Dungeon d : Dungeons.all()) {
            if (d.id().equals("dng_barrows")) {
                return d;
            }
        }
        throw new AssertionError("no Barrows dungeon");
    }

    private static Op blockAt(StructureDef s, int x, int y, int z) {
        Op last = null;
        for (Op op : s.ops()) {
            if (x >= op.x1() && x <= op.x2() && y >= op.y1() && y <= op.y2() && z >= op.z1() && z <= op.z2()) {
                last = op;
            }
        }
        return last;
    }

    private static boolean isAir(StructureDef s, int x, int y, int z) {
        Op op = blockAt(s, x, y, z);
        return op != null && op.type() == Op.Type.CLEAR;
    }

    @Test
    void sixCryptsLieBelowTheirMoundsWithASarcophagusAndAWayUp() {
        StructureDef s = dungeon().structure();
        assertEquals(6, Barrows.crypts().size());
        for (Barrows.Crypt c : Barrows.crypts()) {
            assertEquals(Barrows.BROTHERS.get(c.index()), c.brother());
            int[] sar = c.sarcophagus();
            assertEquals("runecraft:" + Barrows.SARCOPHAGUS, blockAt(s, sar[0], sar[1], sar[2]).block());
            int[] st = c.stairs();
            assertEquals("runecraft:" + Barrows.CRYPT_STAIRS, blockAt(s, st[0], st[1], st[2]).block());
            assertTrue(st[1] <= Dungeons.DEEP_TOP);
            int[] a = c.arrival();
            assertTrue(isAir(s, a[0], a[1], a[2]) && isAir(s, a[0], a[1] + 1, a[2]), "arrival blocked in " + c);
            int[] f = c.spawnFloor();
            assertTrue(isAir(s, f[0], f[1] + 1, f[2]) && isAir(s, f[0], f[1] + 2, f[2]), "brother spawn blocked in " + c);
            assertTrue(c.contains(sar[0], sar[1], sar[2]));
            assertSame(c, Barrows.cryptAt(a[0], a[1], a[2]));
            // Standing on top of the mound (y 69) digs into this crypt; walking around at its foot does not.
            assertSame(c, Barrows.moundAt(c.x() + 2, StructureDef.GROUND + 5, c.z() - 1));
            assertNull(Barrows.moundAt(c.x() + 12, StructureDef.GROUND, c.z()));
            assertTrue(Barrows.underground(a[0], a[1], a[2]));
            assertFalse(Barrows.underground(c.x(), StructureDef.GROUND + 5, c.z()));
        }
        // Every op is underground (the crypt stairs exit teleports up to the mound top).
        for (Op op : s.ops()) {
            assertTrue(op.y2() <= Dungeons.DEEP_TOP, op.toString());
        }
    }

    @Test
    void tunnelsAreA3x3GridWithDoorsAndTheChestInTheMiddle() {
        StructureDef s = dungeon().structure();
        int[] ch = Barrows.chest();
        assertEquals("runecraft:" + Barrows.CHEST, blockAt(s, ch[0], ch[1], ch[2]).block());
        assertTrue(Barrows.inTunnels(ch[0], ch[1], ch[2]));
        Set<Integer> doors = new HashSet<>();
        for (int e = 0; e < BarrowsRun.EDGES; e++) {
            int[] d = Barrows.doorAt(e);
            assertEquals("runecraft:" + Barrows.TUNNEL_DOOR, blockAt(s, d[0], Barrows.TUNNEL_FLOOR + 1, d[1]).block());
            assertEquals(e, Barrows.edgeAtDoor(d[0], d[1]));
            assertTrue(doors.add(d[0] * 31 + d[1]));
            int[] rooms = BarrowsRun.edgeRooms(e);
            assertNotEquals(rooms[0], rooms[1]);
            int[] a = Barrows.roomCentre(rooms[0]);
            int[] b = Barrows.roomCentre(rooms[1]);
            assertEquals(Barrows.ROOM_SPACING, Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]), "edge " + e + " joins non-adjacent rooms");
            // Both sides of the door are carved, so a player can pass through.
            int dx = Integer.signum(b[0] - a[0]);
            int dz = Integer.signum(b[1] - a[1]);
            assertTrue(isAir(s, d[0] - dx, Barrows.TUNNEL_FLOOR + 1, d[1] - dz));
            assertTrue(isAir(s, d[0] + dx, Barrows.TUNNEL_FLOOR + 1, d[1] + dz));
        }
        for (int room = 0; room < 9; room++) {
            int[] c = Barrows.roomCentre(room);
            assertTrue(isAir(s, c[0] + 1, Barrows.TUNNEL_FLOOR + 1, c[1] + 1), "room " + room);
        }
        // Tunnels and crypts don't touch.
        for (Barrows.Crypt c : Barrows.crypts()) {
            assertTrue(Barrows.TUNNEL_FLOOR + Barrows.TUNNEL_HEIGHT + 1 < c.sarcophagus()[1] - 2);
        }
    }

    @Test
    void everyRunOpensAPathToEveryRoomButLocksSomeDoors() {
        Random random = new Random(42);
        Set<String> layouts = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            BarrowsRun run = BarrowsRun.fresh(random);
            boolean[] open = run.openDoors();
            int count = 0;
            for (boolean o : open) {
                count += o ? 1 : 0;
            }
            assertEquals(9, count, "8-door spanning tree plus one extra");
            // The same run always has the same layout.
            assertEquals(java.util.Arrays.toString(open), java.util.Arrays.toString(run.openDoors()));
            layouts.add(java.util.Arrays.toString(open));
            // Every room is reachable from the start room.
            Set<Integer> seen = new HashSet<>();
            Deque<Integer> todo = new ArrayDeque<>();
            todo.add(run.startRoom());
            seen.add(run.startRoom());
            while (!todo.isEmpty()) {
                int r = todo.poll();
                for (int e = 0; e < BarrowsRun.EDGES; e++) {
                    int[] rooms = BarrowsRun.edgeRooms(e);
                    if (!open[e] || (rooms[0] != r && rooms[1] != r)) {
                        continue;
                    }
                    int other = rooms[0] == r ? rooms[1] : rooms[0];
                    if (seen.add(other)) {
                        todo.add(other);
                    }
                }
            }
            assertEquals(9, seen.size());
            assertTrue(List.of(0, 2, 6, 8).contains(run.startRoom()));
            assertTrue(run.tunnel() >= 0 && run.tunnel() < 6);
        }
        assertTrue(layouts.size() > 20, "the route should change between runs");
    }

    @Test
    void runStateFollowsOsrsRules() {
        BarrowsRun run = BarrowsRun.of(0, 0, 2, 1234L, false);
        assertEquals(Boss.GUTHAN, run.tunnelBrother());
        assertEquals(BarrowsRun.Search.TUNNEL, run.search(2));
        assertEquals(BarrowsRun.Search.SUMMON, run.search(0));
        // A kill only counts once the brother was summoned this run.
        assertFalse(run.kill(0));
        run.summon(0);
        assertTrue(run.kill(0));
        assertFalse(run.kill(0));
        assertEquals(BarrowsRun.Search.EMPTY, run.search(0));
        assertEquals(1, run.killedCount());
        // The chest summons the hidden brother once, then gives the loot (even if he wasn't killed).
        assertEquals(BarrowsRun.Open.SUMMON_HIDDEN, run.open());
        run.summon(run.tunnel());
        assertTrue(run.hiddenSummoned());
        assertEquals(BarrowsRun.Open.LOOT, run.open());
        assertTrue(run.kill(2));
        assertEquals(2, run.killedCount());
        // Saving and restoring keeps everything.
        BarrowsRun copy = BarrowsRun.of(run.killedMask(), run.summonedMask(), run.tunnel(), run.seed(), run.hiddenSummoned());
        assertEquals(run.killedMask(), copy.killedMask());
        assertEquals(run.startRoom(), copy.startRoom());
        // Looting resets the run.
        run.reset(new Random(7));
        assertEquals(0, run.killedCount());
        assertEquals(0, run.summonedMask());
        assertFalse(run.hiddenSummoned());
        assertEquals(BarrowsRun.Search.SUMMON, run.search((run.tunnel() + 1) % 6));
    }

    @Test
    void chestRewardsScaleWithBrothersKilled() {
        assertEquals(450, BarrowsRun.barrowsItemChance(0));
        assertEquals(102, BarrowsRun.barrowsItemChance(6));
        assertEquals(7, BarrowsRun.rolls(6));
        Random random = new Random(1);
        Set<String> karilOnly = new HashSet<>(List.of("karils_coif", "karils_leathertop", "karils_leatherskirt", "karils_crossbow"));
        int pieces = 0;
        for (int i = 0; i < 3000; i++) {
            BarrowsRun one = BarrowsRun.of(1 << 3, 1 << 3, 0, i, true);
            for (BarrowsRun.Reward r : one.rollRewards(random)) {
                assertNotNull(ItemCatalog.get(r.item()), r.item());
                assertTrue(r.count() > 0);
                if (!r.item().equals("coins") && !r.item().endsWith("_rune")) {
                    assertTrue(karilOnly.contains(r.item()), "piece from a brother not killed: " + r.item());
                    pieces++;
                }
            }
            assertTrue(BarrowsRun.of(0, 0, 0, i, true).rollRewards(random).stream()
                    .allMatch(r -> r.item().equals("coins") || r.item().endsWith("_rune")));
        }
        assertTrue(pieces > 0 && pieces < 60, "about 3000 * 2 / 392 pieces, got " + pieces);
        int full = 0;
        for (int i = 0; i < 3000; i++) {
            BarrowsRun all = BarrowsRun.of(63, 63, 0, i, true);
            List<BarrowsRun.Reward> rewards = all.rollRewards(random);
            assertTrue(rewards.size() >= 1 && rewards.size() <= 7);
            for (BarrowsRun.Reward r : rewards) {
                if (!r.item().equals("coins") && !r.item().endsWith("_rune")) {
                    full++;
                }
            }
        }
        assertTrue(full > pieces, "a full run gives far more pieces");
    }

    @Test
    void barrowsBlocksAndSurfaceAreConsistent() {
        Set<String> ids = new HashSet<>();
        DungeonBlocks.all().forEach(b -> ids.add(b.id()));
        for (String id : List.of(Barrows.SARCOPHAGUS, Barrows.CRYPT_STAIRS, Barrows.TUNNEL_DOOR, Barrows.CHEST)) {
            assertTrue(ids.contains(id), id);
        }
        // The world module's mounds no longer carry the surface summoning stones or crypt doors.
        for (StructureDef s : ContentRegistry.structures()) {
            if (!s.id().equals("barrows")) {
                continue;
            }
            for (Op op : s.ops()) {
                assertFalse(op.block().endsWith("_summoning_stone"), op.toString());
                assertFalse(op.block().equals("minecraft:iron_bars"), "crypt door bars left: " + op);
            }
        }
        assertNotNull(ItemCatalog.get("spade"));
    }
}
