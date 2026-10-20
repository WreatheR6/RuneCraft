package com.runecraft.osrs.content.raids;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Pure rules of the Tombs of Amascut's challenge rooms (OSRS wiki, "Tombs of Amascut/Strategies"): the Path of Scabaras'
 * puzzles (tile sequence, lights, matching symbols, the addition tablet), the Path of Het's mirror and beam puzzle and
 * Het's seal, the Path of Apmeken's baboon waves and the Path of Crondis' water. The Minecraft rooms
 * ({@code content.raids.ToaPuzzleRooms}) build and run them.
 */
public final class ToaPuzzles {
    private ToaPuzzles() {
    }

    // ------------------------------------------------------------------ Path of Scabaras

    /** Lights puzzle: all nine plates lit (bit i = plate i of the 3x3 board, row by row). */
    public static final int LIGHTS_SOLVED = 0x1FF;

    /** Stepping on a plate flips it and the plates next to it (up, down, left, right). */
    public static int lightsPress(int state, int plate) {
        int row = plate / 3;
        int col = plate % 3;
        int s = state ^ (1 << plate);
        if (row > 0) {
            s ^= 1 << (plate - 3);
        }
        if (row < 2) {
            s ^= 1 << (plate + 3);
        }
        if (col > 0) {
            s ^= 1 << (plate - 1);
        }
        if (col < 2) {
            s ^= 1 << (plate + 1);
        }
        return s;
    }

    /** A starting board that can be solved (random presses away from the solved board), never already solved. */
    public static int lightsStart(Random random) {
        int s;
        do {
            s = LIGHTS_SOLVED;
            for (int i = 0; i < 9; i++) {
                if (random.nextBoolean()) {
                    s = lightsPress(s, i);
                }
            }
        } while (s == LIGHTS_SOLVED);
        return s;
    }

    /** The plates to step on (each once, any order) to light every plate from {@code state}, or null if impossible. */
    public static List<Integer> lightsSolution(int state) {
        for (int mask = 0; mask < 512; mask++) {
            int s = state;
            for (int i = 0; i < 9; i++) {
                if ((mask & (1 << i)) != 0) {
                    s = lightsPress(s, i);
                }
            }
            if (s == LIGHTS_SOLVED) {
                List<Integer> out = new ArrayList<>();
                for (int i = 0; i < 9; i++) {
                    if ((mask & (1 << i)) != 0) {
                        out.add(i);
                    }
                }
                return out;
            }
        }
        return null;
    }

    /** Sequence puzzle: tiles of the 3x3 board to repeat, never the same tile twice in a row. // approx: five tiles */
    public static List<Integer> sequence(int length, Random random) {
        List<Integer> out = new ArrayList<>();
        while (out.size() < length) {
            int t = random.nextInt(9);
            if (out.isEmpty() || out.get(out.size() - 1) != t) {
                out.add(t);
            }
        }
        return out;
    }

    public static final int SEQUENCE_LENGTH = 5;

    /** Addition puzzle: the nine tiles' values (a shuffle of 1-9). */
    public static int[] additionValues(Random random) {
        List<Integer> v = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9));
        Collections.shuffle(v, random);
        int[] out = new int[9];
        for (int i = 0; i < 9; i++) {
            out[i] = v.get(i);
        }
        return out;
    }

    /** The tablet's number (OSRS: 20-45). */
    public static int additionTarget(Random random) {
        return 20 + random.nextInt(26);
    }

    /** Tiles (indexes) whose values add up to {@code target}, each used once, or null. */
    public static List<Integer> additionSolution(int[] values, int target) {
        List<Integer> out = new ArrayList<>();
        int left = target;
        for (int v = 9; v >= 1 && left > 0; v--) {
            if (v <= left) {
                for (int i = 0; i < values.length; i++) {
                    if (values[i] == v) {
                        out.add(i);
                        left -= v;
                    }
                }
            }
        }
        return left == 0 ? out : null;
    }

    /** Matching puzzle: the symbol (0-8) under each tile of the two boards. */
    public static int[][] matchingBoards(Random random) {
        int[][] boards = new int[2][9];
        for (int b = 0; b < 2; b++) {
            List<Integer> v = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8));
            Collections.shuffle(v, random);
            for (int i = 0; i < 9; i++) {
                boards[b][i] = v.get(i);
            }
        }
        return boards;
    }

    /** Symbol sets already matched when the puzzle starts (OSRS: four of the nine in solo raids). */
    public static int matchingPresolved(int partySize) {
        return partySize <= 1 ? 4 : 0;
    }

    // ------------------------------------------------------------------ Path of Scabaras: the two branches

    /** The four puzzle rooms' puzzles (the addition tablet always comes last, where the branches meet). */
    public enum ScabarasPuzzle { SEQUENCE, OBELISKS, LIGHTS, MATCHING }

    /** Puzzle rooms: the north branch's first and second room, then the south branch's. */
    public static final int NORTH_FIRST = 0;
    public static final int NORTH_SECOND = 1;
    public static final int SOUTH_FIRST = 2;
    public static final int SOUTH_SECOND = 3;

    /**
     * Which puzzle each of the four rooms holds (OSRS wiki: five puzzles "between two branched paths": the sequence, the
     * obelisks, the lights and the matching symbols in the branches' rooms, then the addition tablet). // approx: shuffled
     * at random
     */
    public static List<ScabarasPuzzle> scabarasLayout(Random random) {
        List<ScabarasPuzzle> out = new ArrayList<>(List.of(ScabarasPuzzle.values()));
        Collections.shuffle(out, random);
        return out;
    }

    /**
     * Whether a puzzle room's door is open (OSRS wiki): the first rooms of both branches always are; in a group each
     * branch's second room opens once its first is solved, and the party must clear both branches. Solo, clearing a first
     * room opens the other branch's second room ("head to the other side via the shortcuts, pass the opened door"), so a
     * solo raider solves two of the four puzzles.
     */
    public static boolean scabarasRoomOpen(int room, boolean[] solved, boolean solo) {
        return switch (room) {
            case NORTH_FIRST, SOUTH_FIRST -> true;
            case NORTH_SECOND -> solo ? solved[SOUTH_FIRST] : solved[NORTH_FIRST];
            default -> solo ? solved[NORTH_FIRST] : solved[SOUTH_FIRST];
        };
    }

    /** Whether the addition tablet can be read: every room solved in a group, a first room and its partner solo. */
    public static boolean scabarasAdditionOpen(boolean[] solved, boolean solo) {
        if (!solo) {
            return solved[0] && solved[1] && solved[2] && solved[3];
        }
        return (solved[NORTH_FIRST] && solved[SOUTH_SECOND]) || (solved[SOUTH_FIRST] && solved[NORTH_SECOND]);
    }

    /** The obelisks puzzle: five obelisks to strike in one order (OSRS: 5! = 120 orders). */
    public static final int OBELISKS = 5;

    public static List<Integer> obeliskOrder(Random random) {
        List<Integer> out = new ArrayList<>(List.of(0, 1, 2, 3, 4));
        Collections.shuffle(out, random);
        return out;
    }

    /**
     * Obelisks lit after striking {@code struck} with {@code lit} already lit (OSRS): the next one in the order lights up;
     * a wrong one stays dark, rocks fall and the lit ones go out again (-1 means a wrong strike). An obelisk already lit
     * does nothing.
     */
    public static int obeliskStrike(List<Integer> order, int lit, int struck) {
        for (int i = 0; i < lit; i++) {
            if (order.get(i) == struck) {
                return lit;
            }
        }
        return lit < order.size() && order.get(lit) == struck ? lit + 1 : -1;
    }

    // ------------------------------------------------------------------ Path of Het

    /** Mirror orientations: '/' and '\' as seen from above with north up. */
    public static final int SLASH = 1;
    public static final int BACKSLASH = 2;

    /** Directions as (dx, dz) on the grid: north is -z. */
    public static final int[][] DIRS = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};

    /** The direction a beam leaves a mirror with orientation {@code o}, entering with direction (dx, dz). */
    public static int[] reflect(int o, int dx, int dz) {
        return o == SLASH ? new int[]{-dz, -dx} : new int[]{dz, dx};
    }

    /** The orientation that turns a beam from direction a to direction b (they must be perpendicular). */
    static int orientationFor(int[] a, int[] b) {
        int[] s = reflect(SLASH, a[0], a[1]);
        return s[0] == b[0] && s[1] == b[1] ? SLASH : BACKSLASH;
    }

    /** Mirrors the party can pick up, place and rotate (OSRS: three). */
    public static final int MOVABLE_MIRRORS = 3;
    /** A movable mirror's position while a player carries it. */
    public static final int CARRIED = Integer.MIN_VALUE;

    /**
     * A Path of Het puzzle on a (2 x half + 1)-square grid (OSRS wiki): the caster statue's beam enters at (-half-1,
     * startZ) heading east and must reach the shielded statue ({@code target}). The path turns at {@code turns}
     * ({x, z, orientation needed}); some turns hold fixed mirrors already facing the right way, the others (one to three)
     * need the party's three movable mirrors. {@code fixed}: fixed mirrors ({x, z, orientation, dirty}), on turns or
     * decoys elsewhere; {@code movable}: the movable mirrors ({x, z, orientation, dirty}, x = {@link #CARRIED} while
     * carried); a dirty mirror (groups) must be cleaned before it reflects. Worn barriers on the path ({x, z}) must be
     * broken with a pickaxe; obstacles ({x, z}) always block.
     */
    public record HetPuzzle(int half, int startZ, List<int[]> turns, List<int[]> fixed, List<int[]> movable,
                            List<int[]> barriers, List<int[]> obstacles, int[] target) {
        /** The fixed mirror at a cell, or null. */
        public int[] fixedAt(int x, int z) {
            for (int[] m : fixed) {
                if (m[0] == x && m[1] == z) {
                    return m;
                }
            }
            return null;
        }

        /** The movable mirror standing at a cell, or null. */
        public int[] movableAt(int x, int z) {
            for (int[] m : movable) {
                if (m[0] == x && m[1] == z) {
                    return m;
                }
            }
            return null;
        }

        /** The mirror (fixed or movable) at a cell, or null. */
        public int[] mirrorAt(int x, int z) {
            int[] m = fixedAt(x, z);
            return m != null ? m : movableAt(x, z);
        }

        /** Whether a cell is free for placing a mirror: in the grid, no mirror, obstacle, barrier, statue or path start. */
        public boolean canPlace(int x, int z, java.util.Set<Long> broken) {
            if (Math.abs(x) > half || Math.abs(z) > half || mirrorAt(x, z) != null || (x == target[0] && z == target[1])) {
                return false;
            }
            for (int[] o : obstacles) {
                if (o[0] == x && o[1] == z) {
                    return false;
                }
            }
            for (int[] b : barriers) {
                if (b[0] == x && b[1] == z && !broken.contains(key(x, z))) {
                    return false;
                }
            }
            return true;
        }
    }

    /** Generates a solvable puzzle for a party (groups get dirty fixed mirrors): one to three mirrors to place. */
    public static HetPuzzle hetPuzzle(int half, int partySize, Random random) {
        for (int attempt = 0; attempt < 1000; attempt++) {
            HetPuzzle p = tryHet(half, partySize, random);
            if (p != null) {
                return p;
            }
        }
        throw new IllegalStateException("no Het puzzle");
    }

    public static HetPuzzle hetPuzzle(int half, Random random) {
        return hetPuzzle(half, 1, random);
    }

    private static boolean inGrid(int half, int x, int z) {
        return Math.abs(x) <= half && Math.abs(z) <= half;
    }

    private static HetPuzzle tryHet(int half, int partySize, Random random) {
        int startZ = random.nextInt(2 * half - 1) - half + 1;
        int x = -half - 1;
        int z = startZ;
        int[] dir = DIRS[0];
        Set<Long> visited = new HashSet<>();
        List<int[]> pathCells = new ArrayList<>();
        List<int[]> turns = new ArrayList<>();
        int toPlace = 1 + random.nextInt(MOVABLE_MIRRORS);
        int fixedTurns = random.nextInt(2);
        int turnCount = toPlace + fixedTurns;
        for (int t = 0; t <= turnCount; t++) {
            int len = 2 + random.nextInt(half);
            for (int i = 0; i < len; i++) {
                x += dir[0];
                z += dir[1];
                if (!inGrid(half, x, z) || !visited.add(key(x, z))) {
                    return null;
                }
                pathCells.add(new int[]{x, z});
            }
            if (t == turnCount) {
                break;
            }
            int[] left = {dir[1], -dir[0]};
            int[] right = {-dir[1], dir[0]};
            int[] next = random.nextBoolean() ? left : right;
            turns.add(new int[]{x, z, orientationFor(dir, next)});
            dir = next;
        }
        int[] target = pathCells.remove(pathCells.size() - 1);
        List<int[]> free = new ArrayList<>();
        for (int[] c : pathCells) {
            boolean turn = false;
            for (int[] m : turns) {
                turn |= m[0] == c[0] && m[1] == c[1];
            }
            if (!turn) {
                free.add(c);
            }
        }
        if (free.size() < 2) {
            return null;
        }
        Collections.shuffle(free, random);
        List<int[]> barriers = new ArrayList<>();
        int nb = 1 + random.nextInt(2);
        for (int i = 0; i < nb && i < free.size() - 1; i++) {
            barriers.add(free.get(i));
        }
        // Fixed mirrors on some turns, already facing the right way (dirty in groups now and then).
        List<int[]> fixed = new ArrayList<>();
        List<int[]> shuffledTurns = new ArrayList<>(turns);
        Collections.shuffle(shuffledTurns, random);
        for (int i = 0; i < fixedTurns; i++) {
            int[] t = shuffledTurns.get(i);
            fixed.add(new int[]{t[0], t[1], t[2], partySize > 1 && random.nextBoolean() ? 1 : 0});
        }
        Set<Long> used = new HashSet<>(visited);
        used.add(key(-half - 1, startZ));
        List<int[]> obstacles = new ArrayList<>();
        List<int[]> movable = new ArrayList<>();
        int decoys = 0;
        int guard = 0;
        while ((decoys < 2 || obstacles.size() < 4 || movable.size() < MOVABLE_MIRRORS) && guard++ < 400) {
            int dx = random.nextInt(2 * half + 1) - half;
            int dz = random.nextInt(2 * half + 1) - half;
            // Keep the beam's own row clear so the caster's beam isn't trivially blocked or deflected.
            if (dz == startZ || !used.add(key(dx, dz))) {
                continue;
            }
            if (movable.size() < MOVABLE_MIRRORS) {
                movable.add(new int[]{dx, dz, 1 + random.nextInt(2), 0});
            } else if (decoys < 2) {
                fixed.add(new int[]{dx, dz, 1 + random.nextInt(2), 0});
                decoys++;
            } else {
                obstacles.add(new int[]{dx, dz});
            }
        }
        if (movable.size() < MOVABLE_MIRRORS) {
            return null;
        }
        HetPuzzle p = new HetPuzzle(half, startZ, turns, fixed, movable, barriers, obstacles, target);
        // The puzzle must not start solved, whatever barriers are broken.
        Set<Long> all = new HashSet<>();
        for (int[] b : barriers) {
            all.add(key(b[0], b[1]));
        }
        if (trace(p, all).hit()) {
            return null;
        }
        return p;
    }

    static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    /** Public cell key (x, z) for sets of broken barriers. */
    public static long cellKey(int x, int z) {
        return key(x, z);
    }

    /**
     * How to solve a puzzle: for each turn without a fixed mirror, which movable mirror to place there and the orientation
     * it needs ({movable index, x, z, orientation}).
     */
    public static List<int[]> hetSolution(HetPuzzle p) {
        List<int[]> out = new ArrayList<>();
        int next = 0;
        for (int[] t : p.turns()) {
            if (p.fixedAt(t[0], t[1]) == null) {
                out.add(new int[]{next++, t[0], t[1], t[2]});
            }
        }
        return out;
    }

    /** Where a fired beam goes: its cells (in order) and whether it reached the shielded statue. */
    public record Beam(List<int[]> cells, boolean hit) {
    }

    /**
     * Traces the caster statue's beam through the mirrors as they stand; a dirty mirror stops it, worn barriers block it
     * unless their cell is in {@code broken}; obstacles always do.
     */
    public static Beam trace(HetPuzzle p, Set<Long> broken) {
        List<int[]> cells = new ArrayList<>();
        int x = -p.half() - 1;
        int z = p.startZ();
        int dx = 1;
        int dz = 0;
        for (int steps = 0; steps < 400; steps++) {
            x += dx;
            z += dz;
            if (!inGrid(p.half(), x, z)) {
                return new Beam(cells, false);
            }
            cells.add(new int[]{x, z});
            if (x == p.target()[0] && z == p.target()[1]) {
                return new Beam(cells, true);
            }
            for (int[] o : p.obstacles()) {
                if (o[0] == x && o[1] == z) {
                    return new Beam(cells, false);
                }
            }
            for (int[] b : p.barriers()) {
                if (b[0] == x && b[1] == z && !broken.contains(key(x, z))) {
                    return new Beam(cells, false);
                }
            }
            int[] m = p.mirrorAt(x, z);
            if (m != null) {
                if (m[3] != 0) {
                    return new Beam(cells, false);
                }
                int[] d = reflect(m[2], dx, dz);
                dx = d[0];
                dz = d[1];
            }
        }
        return new Beam(cells, false);
    }

    /** Chance (one in n) that an orb of darkness crossing a mirror dirties it, in groups only (OSRS). // approx */
    public static final int ORB_DIRTIES_ONE_IN = 2;

    /** Het's seal hitpoints (OSRS: 119 solo, more for larger groups but not with raid level). // approx: +88 a player */
    public static int sealHitpoints(int partySize) {
        return 119 + 88 * Math.max(0, Math.min(8, partySize) - 1);
    }

    /**
     * Damage of one pickaxe strike on the weakened seal (Mod Ash): 8, 15 or 17 for a visible Mining level below 85, below
     * 100, or 100+; +2 for a dragon pickaxe or better, -3 below rune; plus 0-2.
     */
    public static int sealDamage(int miningLevel, String pickaxe, Random random) {
        int base = miningLevel < 85 ? 8 : miningLevel < 100 ? 15 : 17;
        return Math.max(1, base + pickaxeBonus(pickaxe) + random.nextInt(3));
    }

    /** +2 for dragon-tier pickaxes (dragon, infernal, crystal, 3rd age), 0 for rune and gilded, -3 below. */
    public static int pickaxeBonus(String pickaxe) {
        if (pickaxe == null) {
            return -3;
        }
        if (pickaxe.contains("dragon") || pickaxe.contains("infernal") || pickaxe.contains("crystal") || pickaxe.contains("3rd_age")) {
            return 2;
        }
        return pickaxe.contains("rune") || pickaxe.contains("gilded") ? 0 : -3;
    }

    /** Game ticks the seal stays weakened after the beam strikes the shielded statue (OSRS: 15). */
    public static final int SEAL_WEAKENED_TICKS = 15;
    /** Game ticks between the caster statue's beams. // approx */
    public static final int BEAM_CYCLE_TICKS = 18;
    /** Mining XP per strike on the seal (OSRS: 35). */
    public static final int SEAL_MINING_XP = 35;

    // ------------------------------------------------------------------ Path of Apmeken

    public static final String BRAWLER = "toa_baboon_brawler";
    public static final String THROWER = "toa_baboon_thrower";
    public static final String MAGE = "toa_baboon_mage";
    public static final String SHAMAN = "toa_baboon_shaman";
    public static final String THRALL = "toa_baboon_thrall";
    public static final String VOLATILE = "toa_volatile_baboon";
    public static final String CURSED = "toa_cursed_baboon";

    /**
     * The eight baboon waves (OSRS wiki, solo; groups add one combat baboon a wave). "Special" baboons are volatile or
     * cursed.
     */
    public static List<List<String>> apmekenWaves(int partySize, Random random) {
        String[][] base = {
                {BRAWLER, BRAWLER, SHAMAN},
                {THROWER, THROWER, VOLATILE},
                {MAGE, MAGE, CURSED},
                {THROWER, THROWER, "*", "*", "*"},
                {MAGE, MAGE, SHAMAN, "*", "*"},
                {BRAWLER, BRAWLER, SHAMAN, "*", "*"},
                {BRAWLER, THROWER, SHAMAN, CURSED, CURSED},
                {SHAMAN, VOLATILE, VOLATILE, "*", "*"}};
        String[] combat = {BRAWLER, THROWER, MAGE, THROWER, MAGE, BRAWLER, BRAWLER, MAGE};
        List<List<String>> out = new ArrayList<>();
        for (int w = 0; w < base.length; w++) {
            List<String> wave = new ArrayList<>();
            for (String s : base[w]) {
                wave.add(s.equals("*") ? (random.nextBoolean() ? VOLATILE : CURSED) : s);
            }
            if (partySize > 1) {
                wave.add(combat[w]);
            }
            out.add(wave);
        }
        return out;
    }

    /** Thralls a shaman keeps up at most (OSRS: four or five). // approx: four */
    public static final int SHAMAN_THRALLS = 4;
    /** Game ticks players get to fix a problem Apmeken's sight warns of. // approx */
    public static final int APMEKEN_FIX_TICKS = 14;
    /** Game ticks between problems. // approx */
    public static final int APMEKEN_PROBLEM_EVERY = 24;

    /** Fixes a problem needs (OSRS: one per player). */
    public static int apmekenFixesNeeded(int partySize) {
        return Math.max(1, partySize);
    }

    // ------------------------------------------------------------------ Path of Crondis

    /** Water a full container holds (its "100%"). */
    public static final int CONTAINER_WATER = 100;

    /**
     * Water the Palm of Resourcefulness needs (OSRS: 175, plus 125 for each extra player before June 2025, since then not
     * linear). // approx: +125 for the second and third player, +60 beyond
     */
    public static int palmWaterNeeded(int partySize) {
        int n = Math.max(1, Math.min(8, partySize));
        return 175 + 125 * Math.min(n - 1, 2) + 60 * Math.max(0, n - 3);
    }

    /** Water left after an obstacle hit (OSRS: half, now and then only 16%). */
    public static int spillFromTrap(int water, Random random) {
        return random.nextInt(8) == 0 ? water * 84 / 100 : water / 2;
    }

    /** Water left after a crocodile bite (OSRS: half, or 10 points under 15%). */
    public static int spillFromBite(int water) {
        return water < 15 ? Math.max(0, water - 10) : water / 2;
    }

    /** A crocodile's damage on a player: 18, +3 for each obstacle that hit them in the last 30 seconds, at most 36. */
    public static int crocodileDamage(int recentObstacleHits) {
        return Math.min(36, 18 + 3 * Math.max(0, recentObstacleHits));
    }

    /** At most this many crocodiles at once (OSRS). */
    public static final int MAX_CROCODILES = 8;

    // ------------------------------------------------------------------ guaranteed maximum hits

    /**
     * Raid creatures that always take a player's maximum hit from a style (OSRS): the Path of Apmeken's combat baboons from
     * the style that beats them (brawler: magic, thrower: melee, mage: ranged), its shamans and special baboons from
     * ranged, Ba-Ba's troupe from anything, Kephri's scarab swarms from anything, and the Wardens' energy siphons from
     * melee.
     */
    public static boolean alwaysMaxHit(String id, com.runecraft.osrs.AttackType type) {
        boolean melee = type.isMelee();
        return switch (id) {
            case BRAWLER -> type == com.runecraft.osrs.AttackType.MAGIC;
            case THROWER -> melee;
            case MAGE, SHAMAN, VOLATILE, CURSED -> type == com.runecraft.osrs.AttackType.RANGED;
            case "toa_baba_baboon", "toa_scarab_swarm", ToaBosses.KEPHRI_EGG, ToaBosses.KEPHRI_DARK_EGG -> true;
            case "toa_energy_siphon" -> melee;
            default -> false;
        };
    }
}
