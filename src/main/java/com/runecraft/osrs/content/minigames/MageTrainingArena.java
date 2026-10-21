package com.runecraft.osrs.content.minigames;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The Mage Training Arena's four rooms after the OSRS Wiki (round 40): the spells they use and their runes, the Telekinetic
 * Theatre's sliding mazes, the Alchemists' Playground's rotating cupboards and values, the Enchanting Chamber's shapes,
 * dragonstones and bonus shape, and the Creature Graveyard's bone piles. Points, caps and the reward shop live in
 * {@link MinigameRules.MageArena} and {@link RewardShops}.
 */
public final class MageTrainingArena {
    private MageTrainingArena() {
    }

    /** A spell cast in the arena: id, Magic level, experience per cast and its runes (OSRS Wiki spell pages). */
    public record Spell(String id, String name, int level, double xp, Map<String, Integer> runes) {
    }

    private static Map<String, Integer> runes(Object... pairs) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put((String) pairs[i], (Integer) pairs[i + 1]);
        }
        return m;
    }

    public static final Spell TELEKINETIC_GRAB = new Spell("telekinetic_grab", "Telekinetic Grab", 33, 43,
            runes("law_rune", 1, "air_rune", 1));
    public static final Spell LOW_ALCHEMY = new Spell("low_level_alchemy", "Low Level Alchemy", 21, 31,
            runes("nature_rune", 1, "fire_rune", 3));
    public static final Spell HIGH_ALCHEMY = new Spell("high_level_alchemy", "High Level Alchemy", 55, 65,
            runes("nature_rune", 1, "fire_rune", 5));
    public static final Spell BONES_TO_BANANAS = new Spell("bones_to_bananas", "Bones to Bananas", 15, 25,
            runes("nature_rune", 1, "water_rune", 2, "earth_rune", 2));
    public static final Spell BONES_TO_PEACHES = new Spell("bones_to_peaches", "Bones to Peaches", 60, 35.5,
            runes("nature_rune", 2, "water_rune", 4, "earth_rune", 4));
    /** Lvl-1 to Lvl-7 Enchant (the Enchanting Chamber's table: Magic level, experience, runes). */
    public static final List<Spell> ENCHANTS = List.of(
            new Spell("lvl_1_enchant", "Lvl-1 Enchant", 7, 17.5, runes("cosmic_rune", 1, "water_rune", 1)),
            new Spell("lvl_2_enchant", "Lvl-2 Enchant", 27, 37, runes("cosmic_rune", 1, "air_rune", 3)),
            new Spell("lvl_3_enchant", "Lvl-3 Enchant", 49, 59, runes("cosmic_rune", 1, "fire_rune", 5)),
            new Spell("lvl_4_enchant", "Lvl-4 Enchant", 57, 67, runes("cosmic_rune", 1, "earth_rune", 10)),
            new Spell("lvl_5_enchant", "Lvl-5 Enchant", 68, 78, runes("cosmic_rune", 1, "earth_rune", 15, "water_rune", 15)),
            new Spell("lvl_6_enchant", "Lvl-6 Enchant", 87, 97, runes("cosmic_rune", 1, "earth_rune", 20, "fire_rune", 20)),
            new Spell("lvl_7_enchant", "Lvl-7 Enchant", 93, 110, runes("cosmic_rune", 1, "blood_rune", 20, "soul_rune", 20)));

    /**
     * The first spell of {@code bestFirst} the Magic level allows and the player can pay for, or null. Players choose their spell
     * in the arena (OSRS Wiki "Enchanting Chamber": Lvl-7 Enchant "is not recommended due to its extremely high rune cost"), so a
     * player without the best spell's runes casts the next one down: Lvl-6 Enchant without blood and soul runes, Bones to
     * Bananas without the peach runes, Low Level Alchemy without High's fire runes.
     */
    public static Spell bestCastable(int magic, List<Spell> bestFirst, java.util.function.Predicate<Spell> canPay) {
        for (Spell s : bestFirst) {
            if (magic >= s.level() && canPay.test(s)) {
                return s;
            }
        }
        return null;
    }

    /** The enchantment spells best first (Lvl-7 down to Lvl-1). */
    public static List<Spell> enchantsBestFirst() {
        List<Spell> out = new ArrayList<>(ENCHANTS);
        java.util.Collections.reverse(out);
        return out;
    }

    /** The enchantment level (1-7) of an enchantment spell, or 0. */
    public static int enchantLevel(Spell spell) {
        return ENCHANTS.indexOf(spell) + 1;
    }

    /** The best enchantment spell (1-7) a Magic level allows, or 0. */
    public static int enchantLevel(int magic) {
        int best = 0;
        for (int i = 0; i < ENCHANTS.size(); i++) {
            if (magic >= ENCHANTS.get(i).level()) {
                best = i + 1;
            }
        }
        return best;
    }

    // ================================================================== Telekinetic Theatre

    /** Each maze solved: 2 points and 250 Magic XP; every fifth in a row 8 more points, 10 law runes, 1,000 XP, full run energy. */
    public static final int MAZE_XP = 250;
    public static final int STREAK_BONUS_XP = 1000;
    /** The theatre's ten mazes take 7 to 10 casts each (OSRS Wiki). */
    public static final int MIN_MOVES = 7;
    public static final int MAX_MOVES = 10;
    public static final int MAZE_SIZE = 10;

    /** The four directions a grab pulls the statue: towards the side of the maze the caster stands on. */
    public enum Dir {
        NORTH(0, -1), SOUTH(0, 1), EAST(1, 0), WEST(-1, 0);

        public final int dx;
        public final int dz;

        Dir(int dx, int dz) {
            this.dx = dx;
            this.dz = dz;
        }
    }

    /**
     * A sliding maze: a square of cells (x east, z south) with blocked cells; the statue slides in the pulled direction until
     * a blocked cell or the edge stops it, and the maze is solved when it stops on the goal.
     */
    public record Maze(int size, boolean[][] walls, int startX, int startZ, int goalX, int goalZ) {
        public boolean blocked(int x, int z) {
            return x < 0 || z < 0 || x >= size || z >= size || walls[x][z];
        }

        /** Where the statue stops when pulled from (x, z) in {@code d}. */
        public int[] slide(int x, int z, Dir d) {
            while (!blocked(x + d.dx, z + d.dz)) {
                x += d.dx;
                z += d.dz;
            }
            return new int[]{x, z};
        }

        /** The shortest list of pulls from the start to the goal, or null when there is none. */
        public List<Dir> solve() {
            return solve(startX, startZ);
        }

        public List<Dir> solve(int fromX, int fromZ) {
            int[][] prev = new int[size * size][];
            boolean[] seen = new boolean[size * size];
            ArrayDeque<int[]> queue = new ArrayDeque<>();
            queue.add(new int[]{fromX, fromZ});
            seen[fromX * size + fromZ] = true;
            while (!queue.isEmpty()) {
                int[] at = queue.poll();
                if (at[0] == goalX && at[1] == goalZ) {
                    List<Dir> path = new ArrayList<>();
                    int[] cur = at;
                    while (prev[cur[0] * size + cur[1]] != null) {
                        int[] p = prev[cur[0] * size + cur[1]];
                        path.add(0, Dir.values()[p[2]]);
                        cur = p;
                    }
                    return path;
                }
                for (Dir d : Dir.values()) {
                    int[] to = slide(at[0], at[1], d);
                    int key = to[0] * size + to[1];
                    if (!seen[key]) {
                        seen[key] = true;
                        prev[key] = new int[]{at[0], at[1], d.ordinal()};
                        queue.add(to);
                    }
                }
            }
            return null;
        }
    }

    /**
     * A new random maze whose shortest solution takes 7 to 10 pulls, like the theatre's ten (OSRS Wiki). // approx: the layouts
     * are generated, not the wiki's ten
     */
    public static Maze maze(Random random) {
        Maze m = tryMaze(random, 5000);
        for (long seed = 1; m == null; seed++) {
            m = tryMaze(new Random(seed), 5000);
        }
        return m;
    }

    private static Maze tryMaze(Random random, int attempts) {
        for (int attempt = 0; attempt < attempts; attempt++) {
            boolean[][] walls = new boolean[MAZE_SIZE][MAZE_SIZE];
            for (int x = 0; x < MAZE_SIZE; x++) {
                for (int z = 0; z < MAZE_SIZE; z++) {
                    walls[x][z] = random.nextInt(100) < 18;
                }
            }
            int sx = random.nextInt(MAZE_SIZE);
            int sz = random.nextInt(MAZE_SIZE);
            int gx = random.nextInt(MAZE_SIZE);
            int gz = random.nextInt(MAZE_SIZE);
            if (walls[sx][sz] || walls[gx][gz] || sx == gx && sz == gz) {
                continue;
            }
            Maze m = new Maze(MAZE_SIZE, walls, sx, sz, gx, gz);
            List<Dir> path = m.solve();
            if (path != null && path.size() >= MIN_MOVES && path.size() <= MAX_MOVES) {
                return m;
            }
        }
        return null;
    }

    /** Points for solving a maze: 2, plus 8 on every fifth maze of the streak ({@code streak} counts this one). */
    public static int mazePoints(int streak) {
        return MinigameRules.MageArena.TELEKINETIC_MAZE + (streak > 0 && streak % MinigameRules.MageArena.TELEKINETIC_BONUS_EVERY == 0
                ? MinigameRules.MageArena.TELEKINETIC_BONUS : 0);
    }

    // ================================================================== Alchemists' Playground

    /** The five items, always in this order clockwise through the six cupboards, the sixth empty (OSRS Wiki). */
    public static final List<String> ALCHEMY_ITEMS = List.of("mta_leather_boots", "mta_adamant_kiteshield", "mta_adamant_med_helm",
            "mta_emerald", "mta_rune_longsword");
    public static final int CUPBOARDS = 6;
    /** Up to five items can be taken from a cupboard at once. */
    public static final int TAKE_MAX = 5;
    /** The values the items take, reshuffled with the cupboards every minute. */
    public static final int[] VALUES = {1, 5, 8, 15, 30};
    /** Chance a rotation marks one item free to alch (no runes used). // approx: the wiki says "occasionally" */
    public static final double FREE_CHANCE = 0.25;

    /** The cupboards' state after a rotation: which item is in each cupboard, each item's value and the free item (-1 none). */
    public record Cupboards(int offset, int[] values, int free) {
        /** The item in cupboard {@code index} (0-5), or null for the empty one. */
        public String item(int index) {
            int i = Math.floorMod(index - offset, CUPBOARDS);
            return i < ALCHEMY_ITEMS.size() ? ALCHEMY_ITEMS.get(i) : null;
        }

        /** Coins an alchemy spell turns {@code item} into right now (0 for anything else). */
        public int value(String item) {
            int i = ALCHEMY_ITEMS.indexOf(item);
            return i < 0 ? 0 : values[i];
        }

        public boolean free(String item) {
            return free >= 0 && ALCHEMY_ITEMS.indexOf(item) == free;
        }

        /** The cupboard holding {@code item}. */
        public int cupboardOf(String item) {
            for (int c = 0; c < CUPBOARDS; c++) {
                if (item.equals(item(c))) {
                    return c;
                }
            }
            return -1;
        }
    }

    /** A rotation: the items keep their clockwise order from a new starting cupboard, the five values are reshuffled. */
    public static Cupboards rotate(Random random) {
        List<Integer> v = new ArrayList<>();
        for (int x : VALUES) {
            v.add(x);
        }
        java.util.Collections.shuffle(v, random);
        int[] values = v.stream().mapToInt(Integer::intValue).toArray();
        int free = random.nextDouble() < FREE_CHANCE ? random.nextInt(ALCHEMY_ITEMS.size()) : -1;
        return new Cupboards(random.nextInt(CUPBOARDS), values, free);
    }

    // ================================================================== Enchanting Chamber

    /** The four shapes, one pile each; the bonus shape changes every 24 seconds (OSRS Wiki). */
    public static final List<String> SHAPES = List.of("mta_cube", "mta_cylinder", "mta_pentamid", "mta_icosahedron");
    public static final String DRAGONSTONE = "mta_dragonstone";
    public static final String ORB = "mta_orb";
    public static final int BONUS_SHAPE_TICKS = 40;
    /** 1/32 chance of a dragonstone when searching a pile; more appear in the middle of the room, slowly. */
    public static final int PILE_DRAGONSTONE_CHANCE = 32;
    /** A dragonstone appears in the middle every 50 ticks. // approx: "respawn slowly" */
    public static final int DRAGONSTONE_RESPAWN_TICKS = 50;
    /** Each orb deposited in the hole: 20 Magic XP; every 20 orbs, 3 blood, death or cosmic runes on leaving, up to 4,000 orbs. */
    public static final int ORB_XP = 20;
    public static final int ORBS_PER_REWARD = 20;
    public static final int ORB_REWARD_RUNES = 3;
    public static final int ORB_CAP = 4000;
    public static final List<String> ORB_RUNES = List.of("blood_rune", "death_rune", "cosmic_rune");

    // ================================================================== Creature Graveyard

    /** The four bone kinds, worth 1, 2, 3 and 4 fruit each; each pile gives four of one kind before moving to the next. */
    public static final List<String> BONES = List.of("graveyard_bones", "graveyard_bones_2", "graveyard_bones_3", "graveyard_bones_4");
    public static final int BONES_PER_KIND = 4;
    /** Every point also gives one water, earth, nature, death or blood rune on leaving. */
    public static final List<String> GRAVEYARD_RUNES = List.of("water_rune", "earth_rune", "nature_rune", "death_rune", "blood_rune");
    /** Magic XP per fruit deposited: 5 per banana, 10 per peach (OSRS Wiki "Mage Training Arena"). */
    public static final int BANANA_XP = 5;
    public static final int PEACH_XP = 10;

    /** Fruit one bone kind turns into. */
    public static int fruit(String bone) {
        return BONES.indexOf(bone) + 1;
    }

    /** The bone kind a pile gives on its {@code n}th take (0-based): four of each kind in turn. */
    public static String pileBone(int start, int n) {
        return BONES.get(Math.floorMod(start + n / BONES_PER_KIND, BONES.size()));
    }

    /** Random runes as {rune, count}: one per entry of {@code from} picked uniformly, {@code n} times. */
    public static Map<String, Integer> randomRunes(List<String> from, int n, Random random) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            out.merge(from.get(random.nextInt(from.size())), 1, Integer::sum);
        }
        return out;
    }

    /** For tests: a maze from rows of '#' (wall) and '.', with S and G marking start and goal. */
    public static Maze parse(String... rows) {
        int n = rows.length;
        boolean[][] walls = new boolean[n][n];
        int sx = 0;
        int sz = 0;
        int gx = 0;
        int gz = 0;
        for (int z = 0; z < n; z++) {
            for (int x = 0; x < n; x++) {
                char c = rows[z].charAt(x);
                walls[x][z] = c == '#';
                if (c == 'S') {
                    sx = x;
                    sz = z;
                } else if (c == 'G') {
                    gx = x;
                    gz = z;
                }
            }
        }
        return new Maze(n, walls, sx, sz, gx, gz);
    }

    /** Whether two mazes have the same walls (tests). */
    public static boolean sameWalls(Maze a, Maze b) {
        return Arrays.deepEquals(a.walls(), b.walls());
    }
}
