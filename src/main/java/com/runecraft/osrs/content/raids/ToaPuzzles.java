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

    /**
     * A Path of Het puzzle on a (2 x half + 1)-square grid: the beam enters at (-half, startZ) heading east; mirrors at
     * the path's turns ({x, z, correct orientation, starting orientation}), decoy mirrors ({x, z, 0, starting
     * orientation}), worn barriers on the path to break ({x, z}), solid obstacles ({x, z}) and the shielded statue.
     */
    public record HetPuzzle(int half, int startZ, List<int[]> mirrors, List<int[]> decoys, List<int[]> barriers,
                            List<int[]> obstacles, int[] target) {
        /** The mirror (path or decoy) at a cell, or null. */
        public int[] mirrorAt(int x, int z) {
            for (int[] m : mirrors) {
                if (m[0] == x && m[1] == z) {
                    return m;
                }
            }
            for (int[] m : decoys) {
                if (m[0] == x && m[1] == z) {
                    return m;
                }
            }
            return null;
        }
    }

    /** Generates a solvable puzzle: one to three turns, a worn barrier or two on the path, decoys and obstacles. */
    public static HetPuzzle hetPuzzle(int half, Random random) {
        for (int attempt = 0; attempt < 500; attempt++) {
            HetPuzzle p = tryHet(half, random);
            if (p != null) {
                return p;
            }
        }
        throw new IllegalStateException("no Het puzzle");
    }

    private static boolean inGrid(int half, int x, int z) {
        return Math.abs(x) <= half && Math.abs(z) <= half;
    }

    private static HetPuzzle tryHet(int half, Random random) {
        int startZ = random.nextInt(2 * half - 1) - half + 1;
        int x = -half - 1;
        int z = startZ;
        int[] dir = DIRS[0];
        Set<Long> visited = new HashSet<>();
        List<int[]> pathCells = new ArrayList<>();
        List<int[]> mirrors = new ArrayList<>();
        int turns = 1 + random.nextInt(3);
        for (int t = 0; t <= turns; t++) {
            int len = 2 + random.nextInt(half);
            for (int i = 0; i < len; i++) {
                x += dir[0];
                z += dir[1];
                if (!inGrid(half, x, z) || !visited.add(key(x, z))) {
                    return null;
                }
                pathCells.add(new int[]{x, z});
            }
            if (t == turns) {
                break;
            }
            int[] left = {dir[1], -dir[0]};
            int[] right = {-dir[1], dir[0]};
            int[] next = random.nextBoolean() ? left : right;
            int o = orientationFor(dir, next);
            mirrors.add(new int[]{x, z, o, o});
            dir = next;
        }
        int[] target = pathCells.remove(pathCells.size() - 1);
        // Mirror cells are turns, not free path cells.
        List<int[]> free = new ArrayList<>();
        for (int[] c : pathCells) {
            boolean mirror = false;
            for (int[] m : mirrors) {
                mirror |= m[0] == c[0] && m[1] == c[1];
            }
            if (!mirror) {
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
        Set<Long> used = new HashSet<>(visited);
        used.add(key(-half - 1, startZ));
        List<int[]> decoys = new ArrayList<>();
        List<int[]> obstacles = new ArrayList<>();
        int guard = 0;
        while ((decoys.size() < 3 || obstacles.size() < 4) && guard++ < 200) {
            int dx = random.nextInt(2 * half + 1) - half;
            int dz = random.nextInt(2 * half + 1) - half;
            if (!used.add(key(dx, dz))) {
                continue;
            }
            if (decoys.size() < 3) {
                decoys.add(new int[]{dx, dz, 0, 1 + random.nextInt(2)});
            } else {
                obstacles.add(new int[]{dx, dz});
            }
        }
        for (int[] m : mirrors) {
            m[3] = 1 + random.nextInt(2);
        }
        HetPuzzle p = new HetPuzzle(half, startZ, mirrors, decoys, barriers, obstacles, target);
        // The puzzle must not start solved: turn the first path mirror the wrong way if it does.
        Set<Long> none = new HashSet<>();
        if (trace(p, none).hit()) {
            mirrors.get(0)[3] = mirrors.get(0)[2] == SLASH ? BACKSLASH : SLASH;
        }
        return p;
    }

    static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    /** Where a fired beam goes: its cells (in order) and whether it reached the shielded statue. */
    public record Beam(List<int[]> cells, boolean hit) {
    }

    /**
     * Traces the caster statue's beam through the mirrors' current orientations ({@code m[3]}); worn barriers block it
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
                int[] d = reflect(m[3], dx, dz);
                dx = d[0];
                dz = d[1];
            }
        }
        return new Beam(cells, false);
    }

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
            case "toa_baba_baboon", "toa_scarab_swarm" -> true;
            case "toa_energy_siphon" -> melee;
            default -> false;
        };
    }
}
