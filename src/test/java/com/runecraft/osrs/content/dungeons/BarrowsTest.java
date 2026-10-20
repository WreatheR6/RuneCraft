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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
                if (!mainTable(r.item())) {
                    assertTrue(karilOnly.contains(r.item()), "piece from a brother not killed: " + r.item());
                    pieces++;
                }
            }
            assertTrue(BarrowsRun.of(0, 0, 0, i, true).rollRewards(random).stream()
                    .allMatch(r -> r.item().equals("clue_scroll_elite")), "no potential, no brothers: only the clue roll");
        }
        assertTrue(pieces > 0 && pieces < 60, "about 3000 * 2 / 392 pieces, got " + pieces);
        int full = 0;
        for (int i = 0; i < 3000; i++) {
            BarrowsRun all = BarrowsRun.of(63, 63, 0, i, true);
            List<BarrowsRun.Reward> rewards = all.rollRewards(random);
            assertTrue(rewards.size() >= 1 && rewards.size() <= 7);
            for (BarrowsRun.Reward r : rewards) {
                if (!mainTable(r.item())) {
                    full++;
                }
            }
        }
        assertTrue(full > pieces, "a full run gives far more pieces");
    }

    @Test
    void puzzleBankIsWellFormed() {
        assertTrue(BarrowsPuzzles.BANK.size() >= 10);
        Set<String> questions = new HashSet<>();
        for (BarrowsPuzzles.Puzzle p : BarrowsPuzzles.BANK) {
            assertEquals(3, p.sequence().size(), p.toString());
            assertEquals(2, p.wrong().size(), p.toString());
            Set<String> all = new HashSet<>(p.sequence());
            all.add(p.answer());
            all.addAll(p.wrong());
            assertEquals(6, all.size(), "duplicate item in " + p);
            assertTrue(p.question().endsWith(", ?"));
            assertTrue(questions.add(p.question()), "duplicate puzzle " + p);
        }
        Random random = new Random(3);
        Set<Integer> positions = new HashSet<>();
        BarrowsPuzzles.Puzzle previous = null;
        for (int i = 0; i < 500; i++) {
            BarrowsPuzzles.Asked asked = BarrowsPuzzles.roll(random, previous);
            assertNotEquals(previous, asked.puzzle(), "a wrong answer must re-roll a different puzzle");
            assertEquals(3, asked.options().size());
            assertEquals(asked.puzzle().answer(), asked.options().get(asked.correct()));
            assertTrue(asked.isCorrect(asked.correct()));
            assertFalse(asked.isCorrect((asked.correct() + 1) % 3));
            assertTrue(asked.options().containsAll(asked.puzzle().wrong()));
            positions.add(asked.correct());
            previous = asked.puzzle();
        }
        assertEquals(3, positions.size(), "the right answer moves between the options");
    }

    @Test
    void aboutHalfOfTheChestRoomDoorsPoseAPuzzleUntilSolved() {
        Set<Integer> chestDoors = new HashSet<>();
        for (int e = 0; e < BarrowsRun.EDGES; e++) {
            if (BarrowsRun.chestDoor(e)) {
                chestDoors.add(e);
            }
        }
        assertEquals(Set.of(2, 3, 7, 10), chestDoors);
        Random random = new Random(11);
        int puzzles = 0;
        int total = 0;
        for (int i = 0; i < 1000; i++) {
            BarrowsRun run = BarrowsRun.fresh(random);
            for (int e = 0; e < BarrowsRun.EDGES; e++) {
                boolean puzzle = run.puzzleDoor(e);
                assertEquals(puzzle, run.puzzleDoor(e), "fixed for the run");
                if (!chestDoors.contains(e)) {
                    assertFalse(puzzle);
                    continue;
                }
                total++;
                puzzles += puzzle ? 1 : 0;
                assertEquals(puzzle && run.doorOpen(e), run.needsPuzzle(e));
            }
        }
        assertTrue(puzzles > total * 0.4 && puzzles < total * 0.6, puzzles + " of " + total);
        // Solving keeps a door open for the run (and survives saving); looting resets it.
        BarrowsRun run = null;
        int edge = -1;
        for (long seed = 0; edge < 0; seed++) {
            run = BarrowsRun.of(0, 0, 0, seed, false);
            for (int e : chestDoors) {
                if (run.needsPuzzle(e)) {
                    edge = e;
                }
            }
        }
        run.solve(edge);
        assertTrue(run.solved(edge));
        assertFalse(run.needsPuzzle(edge));
        BarrowsRun copy = BarrowsRun.of(run.killedMask(), run.summonedMask(), run.tunnel(), run.seed(), run.hiddenSummoned(),
                run.solvedMask(), run.monsterKills());
        assertFalse(copy.needsPuzzle(edge));
        run.reset(new Random(1));
        assertEquals(0, run.solvedMask());
        // The puzzle is asked only on the way into the chest room.
        int[] rooms = BarrowsRun.edgeRooms(7);
        int[] d = Barrows.doorAt(7);
        int outer = rooms[0] == Barrows.CHEST_ROOM ? rooms[1] : rooms[0];
        int[] oc = Barrows.roomCentre(outer);
        int[] cc = Barrows.roomCentre(Barrows.CHEST_ROOM);
        assertEquals(Barrows.CHEST_ROOM, Barrows.roomBeyondDoor(7, d[0] + Integer.signum(oc[0] - d[0]), d[1] + Integer.signum(oc[1] - d[1])));
        assertEquals(outer, Barrows.roomBeyondDoor(7, d[0] + Integer.signum(cc[0] - d[0]), d[1] + Integer.signum(cc[1] - d[1])));
        for (int room = 0; room < 9; room++) {
            int[] c = Barrows.roomCentre(room);
            assertEquals(room, Barrows.roomAt(c[0] + 3, c[1] - 3));
        }
        assertEquals(-1, Barrows.roomAt(d[0], d[1]));
    }

    @Test
    void tunnelDoorsWakeBrothersSkeletonsBloodwormsAndCryptRats() {
        // OSRS Wiki: 12/128 a remaining brother, 52/128 a skeleton, 32/128 a bloodworm, 32/128 a crypt rat.
        int[] counts = new int[4];
        for (int r = 0; r < 128; r++) {
            counts[BarrowsRun.doorSpawn(r).ordinal()]++;
        }
        assertArrayEquals(new int[]{12, 52, 32, 32}, counts);
        // Every brother still alive may come out of a door, the hidden one too.
        BarrowsRun run = BarrowsRun.of(1 << 1, 1 << 1, 4, 99L, false);
        assertEquals(List.of(0, 2, 3, 4, 5), run.tunnelCandidates());
        Random random = new Random(5);
        int brothers = 0;
        BarrowsRun.DoorSpawn[] spawn = new BarrowsRun.DoorSpawn[1];
        for (int i = 0; i < 12800; i++) {
            int b = run.doorEncounter(random, false, spawn);
            if (b >= 0) {
                brothers++;
                assertEquals(BarrowsRun.DoorSpawn.BROTHER, spawn[0]);
                assertTrue(run.tunnelCandidates().contains(b));
            } else {
                assertNotEquals(BarrowsRun.DoorSpawn.BROTHER, spawn[0]);
            }
        }
        assertTrue(brothers > 1000 && brothers < 1400, "about 12/128 of the doors, got " + brothers + "/12800");
        // A brother already up: a brother roll wakes nothing.
        for (int i = 0; i < 500; i++) {
            assertEquals(-1, run.doorEncounter(random, true, spawn));
        }
        // The hidden brother out of a door: the chest no longer summons him.
        run.summon(4);
        assertTrue(run.hiddenSummoned());
        assertEquals(BarrowsRun.Open.LOOT, run.open());
        // A tunnel brother is summoned like any other: his kill counts, and he can't come back.
        run.summon(3);
        assertTrue(run.kill(3));
        assertFalse(run.tunnelCandidates().contains(3));
        assertEquals(2, run.killedCount());
        // Every brother dead: no more brothers from the doors.
        BarrowsRun done = BarrowsRun.of(63, 63, 4, 1L, true);
        for (int i = 0; i < 500; i++) {
            assertEquals(-1, done.doorEncounter(random, false, spawn));
        }
    }

    @Test
    void prayerDrainsEighteenSecondsApartAndGrowsWithEachBrother() {
        assertEquals(30, BarrowsRun.PRAYER_DRAIN_TICKS);
        assertEquals(8, BarrowsRun.prayerDrain(0));
        assertEquals(11, BarrowsRun.prayerDrain(3));
        assertEquals(14, BarrowsRun.prayerDrain(6));
    }

    private static boolean mainTable(String item) {
        return item.equals("clue_scroll_elite") || BarrowsRun.BANDS.stream().anyMatch(b -> b.item().equals(item));
    }

    @Test
    void tunnelMonsterKillsAddTheirCombatLevelsToRewardPotential() {
        BarrowsRun run = BarrowsRun.of(1, 1, 3, 5L, true);
        assertEquals(98 + 2, run.rewardPotential(), "Ahrim (98) + 2 per brother");
        for (int i = 0; i < 25; i++) {
            run.addMonsterKill(68);
        }
        assertEquals(25, run.monsterKills());
        assertEquals(BarrowsRun.LEVEL_SUM_CAP + 2, run.rewardPotential(), "levels capped at 1000, plus 2 per brother");
        assertEquals(656 + 12, BarrowsRun.of(63, 63, 3, 5L, true).rewardPotential(), "all six brothers: 656 + 12");
        long plain = 0;
        long boosted = 0;
        for (int i = 0; i < 2000; i++) {
            for (BarrowsRun.Reward r : BarrowsRun.of(1, 1, 3, 5L, true).rollRewards(new Random(i))) {
                plain += r.item().equals("coins") ? r.count() : 0;
            }
            for (BarrowsRun.Reward r : BarrowsRun.of(1, 1, 3, 5L, true, 0, 10, 500).rollRewards(new Random(i))) {
                boosted += r.item().endsWith("_rune") ? 1 : 0;
            }
        }
        assertTrue(plain > 0 && boosted > 0, "potential 100 gives coins only; 600 reaches the rune bands");
        run.reset(new Random(2));
        assertEquals(0, run.monsterKills());
        assertEquals(0, run.monsterLevels());
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
