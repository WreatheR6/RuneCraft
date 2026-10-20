package com.runecraft.osrs.content.godwars;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.content.godwars.GodWarsFactions.God;
import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The God Wars Dungeon at its real OSRS tiles (x 2820-2943, y 5189-5373), carved deep under the sea north of the map
 * like the King Black Dragon's lair: the floor plan of each height level is traced from the OSRS map ({@link GwdMaps}),
 * and every way between its areas is one of the dungeon's real obstacles ({@link Passage}): the rope down the hole north
 * of Trollheim, Bandos's big door (70 Strength and a hammer), the pillar over the chasm to Armadyl's Eyrie (70 Ranged, a
 * crossbow and a mith grapple), the two roped rocks down to Saradomin's Encampment (70 Agility), the icy river before
 * Zamorak's Fortress (70 Hitpoints; it drains Prayer), the four generals' doors (kill count), the frozen door to the
 * Ancient Prison (The Frozen Door), the prison's doors (40 Zarosian essence for the refuge) and the ancient barrier
 * before Nex.
 *
 * <p>The three OSRS height levels are three floors: level 2 (the main dungeon, the Eyrie, Bandos's Stronghold,
 * Zamorak's Fortress and their chambers), level 1 (the ledge of the Encampment) and level 0 (the Encampment's floor with
 * Zilyana's chamber, and the Ancient Prison). Their footprints don't overlap, so each floor's height decides its level.
 */
public final class GwdDungeon {
    private GwdDungeon() {
    }

    /** Floor block heights per OSRS height level (the first air block is one above). Level 2's ceiling stays under y 34. */
    private static final int[] FLOOR = {-6, 7, 20};
    /** Air above the floor per level. */
    private static final int[] HEIGHT = {9, 7, 10};

    /** The hole north of Trollheim (OSRS: tied with a rope the first time, then climbed down). */
    public static final int[] HOLE_TILE = {2917, 3745};
    /** Where the rope out of the dungeon comes up (beside the hole). */
    public static final int[] SURFACE_EXIT = {2916, 3748};
    /** The bottom of the rope (OSRS: the climb lands on 2882, 5311). */
    public static final Spot LANDING = new Spot(2, 2882, 5311);
    /** The rope back up (its object stands at 2881, 5311). */
    public static final Spot ROPE = new Spot(2, 2881, 5311);

    // ------------------------------------------------------------------ the floor plan

    /** A tile of one height level. */
    public record Spot(int plane, int x, int y) {
        /** West block column of the tile. */
        public int blockX() {
            return StructureDef.bx(x);
        }

        /** North block row of the tile. */
        public int blockZ() {
            return StructureDef.bz(y);
        }

        /** Feet height of a player standing on the tile. */
        public int feetY() {
            return floorY(plane) + 1;
        }
    }

    private static String[] rows(int plane) {
        return switch (plane) {
            case 0 -> GwdMaps.P0;
            case 1 -> GwdMaps.P1;
            default -> GwdMaps.P2;
        };
    }

    private static int x0(int plane) {
        return switch (plane) {
            case 0 -> GwdMaps.X0_P0;
            case 1 -> GwdMaps.X0_P1;
            default -> GwdMaps.X0_P2;
        };
    }

    private static int top(int plane) {
        return switch (plane) {
            case 0 -> GwdMaps.TOP_P0;
            case 1 -> GwdMaps.TOP_P1;
            default -> GwdMaps.TOP_P2;
        };
    }

    /** The traced map character of a tile ('.' outside the map). */
    public static char tile(int plane, int x, int y) {
        if (plane < 0 || plane > 2) {
            return '.';
        }
        String[] r = rows(plane);
        int j = top(plane) - y;
        int i = x - x0(plane);
        if (j < 0 || j >= r.length || i < 0 || i >= r[j].length()) {
            return '.';
        }
        return r[j].charAt(i);
    }

    /** True if a player can stand on the tile (floor, a walled floor tile or a doorway). */
    public static boolean walkable(int plane, int x, int y) {
        char c = tile(plane, x, y);
        return c == 'o' || c == 'w' || c == 'n' || c == 'b' || c == 'd';
    }

    /** A wall on the tile's west edge (between it and the tile west of it). */
    public static boolean wallWest(int plane, int x, int y) {
        char c = tile(plane, x, y);
        return c == 'w' || c == 'b';
    }

    /** A wall on the tile's north edge. */
    public static boolean wallNorth(int plane, int x, int y) {
        char c = tile(plane, x, y);
        return c == 'n' || c == 'b';
    }

    public static int floorY(int plane) {
        return FLOOR[plane];
    }

    public static int height(int plane) {
        return HEIGHT[plane];
    }

    /** The OSRS height level a block height lies on inside the dungeon, or -1. */
    public static int planeAt(int blockY) {
        for (int p = 0; p < 3; p++) {
            if (blockY > FLOOR[p] - 2 && blockY <= FLOOR[p] + HEIGHT[p] + 1) {
                return p;
            }
        }
        return -1;
    }

    /** The dungeon's block box {x1, z1, x2, z2} (the whole traced map). */
    public static int[] blockBox() {
        int x1 = Integer.MAX_VALUE;
        int z1 = Integer.MAX_VALUE;
        int x2 = Integer.MIN_VALUE;
        int z2 = Integer.MIN_VALUE;
        for (int p = 0; p < 3; p++) {
            String[] r = rows(p);
            x1 = Math.min(x1, StructureDef.bx(x0(p)));
            x2 = Math.max(x2, StructureDef.bx(x0(p) + r[0].length() - 1) + 1);
            z1 = Math.min(z1, StructureDef.bz(top(p)));
            z2 = Math.max(z2, StructureDef.bz(top(p) - r.length + 1) + 1);
        }
        return new int[]{x1, z1, x2, z2};
    }

    /** True if the block lies inside the dungeon (its box, from the lowest floor to the highest ceiling). */
    public static boolean contains(int blockX, int blockY, int blockZ) {
        int[] b = blockBox();
        return blockX >= b[0] && blockX <= b[2] && blockZ >= b[1] && blockZ <= b[3] && planeAt(blockY) >= 0;
    }

    // ------------------------------------------------------------------ areas

    /** The dungeon's areas, each found by flood-filling the traced floor from one of its tiles. */
    public enum Area {
        MAIN("the God Wars Dungeon", null, new Spot(2, 2882, 5311)),
        ARMADYL_EYRIE("Armadyl's Eyrie", God.ARMADYL, new Spot(2, 2871, 5268)),
        BANDOS_STRONGHOLD("Bandos' Stronghold", God.BANDOS, new Spot(2, 2840, 5340)),
        SARADOMIN_ENCAMPMENT("Saradomin's Encampment", God.SARADOMIN, new Spot(1, 2914, 5300), new Spot(0, 2914, 5270)),
        ZAMORAK_FORTRESS("Zamorak's Fortress", God.ZAMORAK, new Spot(2, 2885, 5345)),
        KREEARRA_CHAMBER("Kree'arra's chamber", God.ARMADYL, new Spot(2, 2832, 5302)),
        GRAARDOR_CHAMBER("General Graardor's chamber", God.BANDOS, new Spot(2, 2872, 5358)),
        ZILYANA_CHAMBER("Commander Zilyana's chamber", God.SARADOMIN, new Spot(0, 2897, 5269)),
        KRIL_CHAMBER("K'ril Tsutsaroth's chamber", God.ZAMORAK, new Spot(2, 2925, 5322)),
        PRISON_ENTRANCE("the Ancient Prison's entrance", null, new Spot(0, 2856, 5222)),
        ANCIENT_PRISON("the Ancient Prison", null, new Spot(0, 2880, 5210)),
        PRISON_REFUGE("the Ancient Prison's refuge", null, new Spot(0, 2903, 5205)),
        NEX_CHAMBER("Nex's chamber", null, new Spot(0, 2925, 5202));

        private final String displayName;
        private final God god;
        private final List<Spot> seeds;

        Area(String displayName, God god, Spot... seeds) {
            this.displayName = displayName;
            this.god = god;
            this.seeds = List.of(seeds);
        }

        public String displayName() {
            return displayName;
        }

        /** The god whose followers hold the area (null for the shared main dungeon and the prison). */
        public God god() {
            return god;
        }

        public List<Spot> seeds() {
            return seeds;
        }

        /** True for the four generals' chambers and Nex's. */
        public boolean chamber() {
            return this == KREEARRA_CHAMBER || this == GRAARDOR_CHAMBER || this == ZILYANA_CHAMBER || this == KRIL_CHAMBER
                    || this == NEX_CHAMBER;
        }
    }

    private static Map<Long, Area> areas;

    private static long key(int plane, int x, int y) {
        return ((long) plane << 40) | ((long) (x & 0xFFFFF) << 20) | (y & 0xFFFFF);
    }

    private static synchronized Map<Long, Area> areaMap() {
        if (areas == null) {
            Map<Long, Area> m = new java.util.HashMap<>();
            for (Area a : Area.values()) {
                for (Spot s : a.seeds) {
                    fill(m, a, s);
                }
            }
            areas = m;
        }
        return areas;
    }

    /** Flood fill over floor tiles (not doorways), never crossing a wall edge. */
    private static void fill(Map<Long, Area> m, Area area, Spot seed) {
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{seed.x, seed.y});
        m.put(key(seed.plane, seed.x, seed.y), area);
        while (!q.isEmpty()) {
            int[] c = q.poll();
            int[][] next = {{c[0] + 1, c[1]}, {c[0] - 1, c[1]}, {c[0], c[1] + 1}, {c[0], c[1] - 1}};
            for (int[] n : next) {
                if (!walkable(seed.plane, n[0], n[1]) || tile(seed.plane, n[0], n[1]) == 'd' || m.containsKey(key(seed.plane, n[0], n[1]))) {
                    continue;
                }
                if (blockedEdge(seed.plane, c[0], c[1], n[0], n[1])) {
                    continue;
                }
                m.put(key(seed.plane, n[0], n[1]), area);
                q.add(n);
            }
        }
    }

    /** Whether a wall stands between two neighbouring tiles. */
    static boolean blockedEdge(int plane, int ax, int ay, int bx, int by) {
        if (bx == ax - 1) {
            return wallWest(plane, ax, ay);
        }
        if (bx == ax + 1) {
            return wallWest(plane, bx, by);
        }
        if (by == ay + 1) {
            return wallNorth(plane, ax, ay);
        }
        return wallNorth(plane, bx, by);
    }

    /** The area a tile belongs to, or null (rock, a doorway, outside the dungeon). */
    public static Area areaOf(int plane, int x, int y) {
        return areaMap().get(key(plane, x, y));
    }

    /** The area of a block position, or null. */
    public static Area areaAt(int blockX, int blockY, int blockZ) {
        int plane = planeAt(blockY);
        if (plane < 0) {
            return null;
        }
        return areaOf(plane, GielinorMap.toOsrsX(blockX), GielinorMap.toOsrsY(blockZ));
    }

    /**
     * Whether a block position is reached by a room-wide attack made in {@code room}: OSRS generals' and Nex's area
     * attacks hit only the players in their own room, never through its walls (the doorway is no part of the room). A null
     * room (a boss outside the dungeon) reaches everywhere.
     */
    public static boolean inRoom(Area room, int blockX, int blockY, int blockZ) {
        return room == null || areaAt(blockX, blockY, blockZ) == room;
    }

    /** Number of tiles in an area. */
    public static int size(Area area) {
        int n = 0;
        for (Area a : areaMap().values()) {
            if (a == area) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ passages

    /** What a passage is, which decides its requirement. */
    public enum Kind {
        /** Bandos's big door: 70 Strength and a hammer to go in (bang the gong). */
        BIG_DOOR,
        /** The pillar over the chasm: 70 Ranged, a crossbow and a mith grapple. */
        GRAPPLE,
        /** A rock with a rope down the Encampment's cliff: 70 Agility, and a rope to tie it the first time. */
        ROCK,
        /** The icy river: 70 Hitpoints (boostable); crossing to the Fortress drains Prayer. */
        ICE_BRIDGE,
        /** A general's door: the god's kill count (or an ecumenical key) to go in. */
        CHAMBER_DOOR,
        /** The frozen door to the Ancient Prison: The Frozen Door. */
        FROZEN_DOOR,
        /** The door from the prison's entrance room to its main hall: free. */
        PRISON_DOOR,
        /** The door to the refuge before Nex: 40 Zarosian essence (or an ecumenical key) to go in. */
        ESSENCE_DOOR,
        /** The ancient barrier into Nex's chamber: free while nobody fights her; she is left by the altar. */
        BARRIER
    }

    /**
     * A way between two areas: its scripted block stands on {@code blocks} (whole tiles, {@code blockHeight} blocks high;
     * for an edge door only the tile's west block column), side {@code a} is the outer side and {@code b} the inner one;
     * using the block from one side lands on the other.
     */
    public record Passage(String id, Kind kind, God god, String block, List<Spot> blocks, int blockHeight, boolean westEdge,
                          Spot a, Spot b, String message) {
    }

    public static final String BLOCK_DOOR = "gwd_door";
    public static final String BLOCK_PILLAR = "gwd_pillar";
    public static final String BLOCK_ROCK = "gwd_rock";
    public static final String BLOCK_ICE_BRIDGE = "gwd_ice_bridge";
    public static final String BLOCK_FROZEN_DOOR = "gwd_frozen_door";
    public static final String BLOCK_BARRIER = "gwd_ancient_barrier";
    public static final String BLOCK_HOLE = "gwd_hole";
    public static final String BLOCK_ROPE = "gwd_rope";

    private static final List<Passage> PASSAGES = List.of(
            new Passage("bandos_big_door", Kind.BIG_DOOR, God.BANDOS, BLOCK_DOOR, List.of(new Spot(2, 2851, 5333)), 3, true,
                    new Spot(2, 2852, 5333), new Spot(2, 2849, 5333), "You bang on the big door's gong with your hammer. The door swings open."),
            new Passage("armadyl_pillar", Kind.GRAPPLE, God.ARMADYL, BLOCK_PILLAR, List.of(new Spot(2, 2871, 5278), new Spot(2, 2871, 5270)),
                    3, false, new Spot(2, 2871, 5280), new Spot(2, 2871, 5268), "You fire your grapple at the pillar and swing across the chasm."),
            new Passage("saradomin_upper_rock", Kind.ROCK, God.SARADOMIN, BLOCK_ROCK,
                    List.of(new Spot(2, 2913, 5300), new Spot(1, 2913, 5300)), 2, false,
                    new Spot(2, 2912, 5300), new Spot(1, 2915, 5300), "You climb down the rope."),
            new Passage("saradomin_lower_rock", Kind.ROCK, God.SARADOMIN, BLOCK_ROCK,
                    List.of(new Spot(1, 2920, 5275), new Spot(0, 2920, 5275)), 2, false,
                    new Spot(1, 2920, 5277), new Spot(0, 2919, 5273), "You climb down the rope."),
            new Passage("zamorak_ice_bridge", Kind.ICE_BRIDGE, God.ZAMORAK, BLOCK_ICE_BRIDGE,
                    List.of(new Spot(2, 2885, 5334), new Spot(2, 2885, 5343)), 1, false,
                    new Spot(2, 2885, 5333), new Spot(2, 2885, 5344), "Dripping, you climb out of the water."),
            new Passage("kreearra_door", Kind.CHAMBER_DOOR, God.ARMADYL, BLOCK_DOOR, List.of(new Spot(2, 2839, 5295)), 3, false,
                    new Spot(2, 2839, 5293), new Spot(2, 2839, 5297), null),
            new Passage("graardor_door", Kind.CHAMBER_DOOR, God.BANDOS, BLOCK_DOOR,
                    List.of(new Spot(2, 2863, 5354), new Spot(2, 2863, 5355)), 3, false,
                    new Spot(2, 2861, 5355), new Spot(2, 2865, 5355), null),
            new Passage("zilyana_door", Kind.CHAMBER_DOOR, God.SARADOMIN, BLOCK_DOOR, List.of(new Spot(0, 2908, 5265)), 3, false,
                    new Spot(0, 2910, 5265), new Spot(0, 2906, 5265), null),
            new Passage("kril_door", Kind.CHAMBER_DOOR, God.ZAMORAK, BLOCK_DOOR, List.of(new Spot(2, 2925, 5332)), 3, false,
                    new Spot(2, 2925, 5334), new Spot(2, 2925, 5330), null),
            new Passage("frozen_door", Kind.FROZEN_DOOR, null, BLOCK_FROZEN_DOOR,
                    List.of(new Spot(2, 2883, 5279), new Spot(2, 2884, 5279), new Spot(0, 2855, 5228), new Spot(0, 2856, 5228)), 3,
                    false, new Spot(2, 2883, 5281), new Spot(0, 2856, 5226), "The frozen door opens and you step down into the Ancient Prison."),
            new Passage("prison_door", Kind.PRISON_DOOR, null, BLOCK_DOOR, List.of(new Spot(0, 2862, 5219)), 3, false,
                    new Spot(0, 2860, 5219), new Spot(0, 2864, 5219), "You pass through the door."),
            new Passage("refuge_door", Kind.ESSENCE_DOOR, null, BLOCK_DOOR, List.of(new Spot(0, 2899, 5203)), 3, false,
                    new Spot(0, 2897, 5203), new Spot(0, 2901, 5203), null),
            new Passage("ancient_barrier", Kind.BARRIER, null, BLOCK_BARRIER, List.of(new Spot(0, 2909, 5202)), 3, false,
                    new Spot(0, 2907, 5202), new Spot(0, 2912, 5202), "You pass through the ancient barrier..."));

    public static List<Passage> passages() {
        return PASSAGES;
    }

    public static Passage passage(String id) {
        for (Passage p : PASSAGES) {
            if (p.id().equals(id)) {
                return p;
            }
        }
        return null;
    }

    /** The passage whose block stands at a block position, or null. */
    public static Passage passageAt(int blockX, int blockY, int blockZ) {
        int plane = planeAt(blockY);
        if (plane < 0) {
            return null;
        }
        int x = GielinorMap.toOsrsX(blockX);
        int y = GielinorMap.toOsrsY(blockZ);
        for (Passage p : PASSAGES) {
            for (Spot s : p.blocks()) {
                if (s.plane() == plane && s.x() == x && s.y() == y) {
                    return p;
                }
            }
        }
        return null;
    }

    /** The passage's door into a general's chamber, or null. */
    public static Passage chamberDoor(God god) {
        for (Passage p : PASSAGES) {
            if (p.kind() == Kind.CHAMBER_DOOR && p.god() == god) {
                return p;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ altars

    /**
     * An altar by a general (OSRS: recharges Prayer every 10 minutes, one point above the maximum per item of its god worn,
     * and teleports out of the chamber to {@code outside}). {@code god} is null for Nex's Zarosian altar.
     */
    public record Altar(String block, God god, Spot spot, Spot outside, String name) {
    }

    private static final List<Altar> ALTARS = List.of(
            new Altar("gwd_armadyl_altar", God.ARMADYL, new Spot(2, 2821, 5301), new Spot(2, 2839, 5293), "Armadyl altar"),
            new Altar("gwd_bandos_altar", God.BANDOS, new Spot(2, 2869, 5370), new Spot(2, 2861, 5355), "Bandos altar"),
            new Altar("gwd_saradomin_altar", God.SARADOMIN, new Spot(0, 2885, 5267), new Spot(0, 2910, 5265), "Saradomin altar"),
            new Altar("gwd_zamorak_altar", God.ZAMORAK, new Spot(2, 2937, 5323), new Spot(2, 2925, 5334), "Zamorak altar"),
            new Altar("gwd_ancient_altar", null, new Spot(0, 2939, 5202), new Spot(0, 2897, 5203), "Ancient altar"));

    public static List<Altar> altars() {
        return ALTARS;
    }

    public static Altar altar(String block) {
        for (Altar a : ALTARS) {
            if (a.block().equals(block)) {
                return a;
            }
        }
        return null;
    }

    /** Game ticks between two uses of an altar (OSRS: 10 minutes). */
    public static final int ALTAR_COOLDOWN_TICKS = 1000;

    // ------------------------------------------------------------------ generals

    /** Where a general or bodyguard stands when it (re)spawns (OSRS cache spawn tiles). */
    public record Lair(String creature, boolean general, Spot spot, Area area) {
    }

    /** A general's (and a bodyguard's) respawn time after death: 150 game ticks (OSRS Wiki infobox; bodyguards approx). */
    public static final int GENERAL_RESPAWN_TICKS = 150;

    private static final List<Lair> LAIRS = List.of(
            new Lair("kreearra", true, new Spot(2, 2832, 5302), Area.KREEARRA_CHAMBER),
            new Lair("flight_kilisa", false, new Spot(2, 2833, 5297), Area.KREEARRA_CHAMBER),
            new Lair("flockleader_geerin", false, new Spot(2, 2828, 5299), Area.KREEARRA_CHAMBER),
            new Lair("wingman_skree", false, new Spot(2, 2840, 5303), Area.KREEARRA_CHAMBER),
            new Lair("general_graardor", true, new Spot(2, 2872, 5358), Area.GRAARDOR_CHAMBER),
            new Lair("sergeant_strongstack", false, new Spot(2, 2866, 5358), Area.GRAARDOR_CHAMBER),
            new Lair("sergeant_steelwill", false, new Spot(2, 2872, 5352), Area.GRAARDOR_CHAMBER),
            new Lair("sergeant_grimspike", false, new Spot(2, 2868, 5362), Area.GRAARDOR_CHAMBER),
            new Lair("commander_zilyana", true, new Spot(0, 2897, 5269), Area.ZILYANA_CHAMBER),
            new Lair("starlight", false, new Spot(0, 2903, 5261), Area.ZILYANA_CHAMBER),
            new Lair("growler", false, new Spot(0, 2896, 5264), Area.ZILYANA_CHAMBER),
            new Lair("bree", false, new Spot(0, 2902, 5274), Area.ZILYANA_CHAMBER),
            new Lair("kril_tsutsaroth", true, new Spot(2, 2925, 5322), Area.KRIL_CHAMBER),
            new Lair("balfrug_kreeyath", false, new Spot(2, 2921, 5319), Area.KRIL_CHAMBER),
            new Lair("tstanon_karlak", false, new Spot(2, 2932, 5328), Area.KRIL_CHAMBER),
            new Lair("zakln_gritch", false, new Spot(2, 2919, 5327), Area.KRIL_CHAMBER));

    /** The four generals and their bodyguards (Nex is fought when a team passes the ancient barrier). */
    public static List<Lair> lairs() {
        return LAIRS;
    }

    public static Lair lair(String creature) {
        for (Lair l : LAIRS) {
            if (l.creature().equals(creature)) {
                return l;
            }
        }
        return null;
    }

    /** Where Nex appears: the middle of her cross-shaped chamber. */
    public static final Spot NEX_SPAWN = new Spot(0, 2925, 5203);
    /** Her four mages in the corners of the cross: Fumus (north-west), Umbra (north-east), Cruor (south-west), Glacies (south-east). approx */
    public static final List<Spot> NEX_MAGES = List.of(new Spot(0, 2914, 5213), new Spot(0, 2936, 5213), new Spot(0, 2914, 5193),
            new Spot(0, 2936, 5193));

    // ------------------------------------------------------------------ the carving

    /** Wall, floor and ceiling-light blocks per area. */
    private static final Map<Area, String[]> LOOK = new EnumMap<>(Area.class);

    static {
        String[] main = {"minecraft:packed_ice", "minecraft:snow_block", "minecraft:sea_lantern"};
        LOOK.put(Area.MAIN, main);
        LOOK.put(Area.ARMADYL_EYRIE, new String[]{"minecraft:calcite", "minecraft:smooth_stone", "minecraft:sea_lantern"});
        LOOK.put(Area.KREEARRA_CHAMBER, new String[]{"minecraft:calcite", "minecraft:smooth_quartz", "minecraft:sea_lantern"});
        LOOK.put(Area.BANDOS_STRONGHOLD, new String[]{"minecraft:mud_bricks", "minecraft:coarse_dirt", "minecraft:shroomlight"});
        LOOK.put(Area.GRAARDOR_CHAMBER, new String[]{"minecraft:mud_bricks", "minecraft:packed_mud", "minecraft:shroomlight"});
        LOOK.put(Area.SARADOMIN_ENCAMPMENT, new String[]{"minecraft:stone_bricks", "minecraft:moss_block", "minecraft:sea_lantern"});
        LOOK.put(Area.ZILYANA_CHAMBER, new String[]{"minecraft:quartz_bricks", "minecraft:smooth_quartz", "minecraft:sea_lantern"});
        LOOK.put(Area.ZAMORAK_FORTRESS, new String[]{"minecraft:blackstone", "minecraft:netherrack", "minecraft:magma_block"});
        LOOK.put(Area.KRIL_CHAMBER, new String[]{"minecraft:polished_blackstone_bricks", "minecraft:crimson_nylium", "minecraft:magma_block"});
        String[] prison = {"minecraft:blue_ice", "minecraft:packed_ice", "minecraft:sea_lantern"};
        LOOK.put(Area.PRISON_ENTRANCE, prison);
        LOOK.put(Area.ANCIENT_PRISON, prison);
        LOOK.put(Area.PRISON_REFUGE, new String[]{"minecraft:deepslate_bricks", "minecraft:polished_deepslate", "minecraft:sea_lantern"});
        LOOK.put(Area.NEX_CHAMBER, new String[]{"minecraft:purpur_block", "minecraft:packed_ice", "minecraft:sea_lantern"});
    }

    private static final String ROCK = "minecraft:packed_ice";

    private static String[] look(int plane, int x, int y) {
        Area a = areaOf(plane, x, y);
        String[] l = a == null ? null : LOOK.get(a);
        return l != null ? l : LOOK.get(Area.MAIN);
    }

    /**
     * The carved dungeon: a shell of rock around every row run of floor tiles, then the runs cleared, floors laid, inner
     * walls, the river (water under invisible barriers, crossed only by the ice bridge), obstacles, ceiling lights, the
     * passage and altar blocks and the bank in the prison's refuge.
     */
    public static StructureDef structure() {
        StructureDef.Builder b = StructureDef.builder("gwd_dungeon", "God Wars Dungeon");
        List<int[]> runs = new ArrayList<>(); // plane, x1, x2, y
        for (int p = 0; p < 3; p++) {
            String[] r = rows(p);
            for (int j = 0; j < r.length; j++) {
                int y = top(p) - j;
                int i = 0;
                while (i < r[j].length()) {
                    if (r[j].charAt(i) == '.') {
                        i++;
                        continue;
                    }
                    int s = i;
                    while (i < r[j].length() && r[j].charAt(i) != '.') {
                        i++;
                    }
                    runs.add(new int[]{p, x0(p) + s, x0(p) + i - 1, y});
                }
            }
        }
        for (int[] run : runs) {
            int f = FLOOR[run[0]];
            b.fill(StructureDef.bx(run[1]) - 1, f - 1, StructureDef.bz(run[3]) - 1, StructureDef.bx(run[2]) + 2, f + HEIGHT[run[0]] + 1,
                    StructureDef.bz(run[3]) + 2, look(run[0], run[1], run[3])[0]);
        }
        for (int[] run : runs) {
            int f = FLOOR[run[0]];
            b.clear(StructureDef.bx(run[1]), f + 1, StructureDef.bz(run[3]), StructureDef.bx(run[2]) + 1, f + HEIGHT[run[0]],
                    StructureDef.bz(run[3]) + 1);
        }
        for (int[] run : runs) {
            int p = run[0];
            int f = FLOOR[p];
            int y = run[3];
            for (int x = run[1]; x <= run[2]; x++) {
                char c = tile(p, x, y);
                int bx = StructureDef.bx(x);
                int bz = StructureDef.bz(y);
                String[] l = look(p, x, y);
                switch (c) {
                    case '~' -> {
                        b.fill(bx, f - 1, bz, bx + 1, f - 1, bz + 1, "minecraft:packed_ice");
                        b.fill(bx, f, bz, bx + 1, f, bz + 1, "minecraft:water");
                        b.fill(bx, f + 1, bz, bx + 1, f + 3, bz + 1, "minecraft:barrier");
                    }
                    case 'x' -> {
                        b.fill(bx, f, bz, bx + 1, f, bz + 1, l[1]);
                        b.fill(bx, f + 1, bz, bx + 1, f + 2, bz + 1, "minecraft:cobblestone_wall");
                    }
                    default -> b.fill(bx, f, bz, bx + 1, f, bz + 1, l[1]);
                }
                if (wallWest(p, x, y)) {
                    b.fill(bx, f + 1, bz, bx, f + HEIGHT[p], bz + 1, l[0]);
                }
                if (wallNorth(p, x, y)) {
                    b.fill(bx, f + 1, bz, bx + 1, f + HEIGHT[p], bz, l[0]);
                }
                // ceiling lights every few tiles, set into the rock above the room
                if (Math.floorMod(x, 4) == 0 && Math.floorMod(y, 4) == 0 && c != '~') {
                    b.set(bx, f + HEIGHT[p] + 1, bz, l[2]);
                }
            }
        }
        for (Passage p : PASSAGES) {
            for (Spot s : p.blocks()) {
                int f = FLOOR[s.plane()];
                int bx = s.blockX();
                int bz = s.blockZ();
                String block = "runecraft:" + p.block();
                if (p.westEdge()) {
                    b.fill(bx, f + 1, bz, bx, f + p.blockHeight(), bz + 1, block);
                } else {
                    b.fill(bx, f + 1, bz, bx + 1, f + p.blockHeight(), bz + 1, block);
                }
            }
        }
        for (Altar a : ALTARS) {
            int f = FLOOR[a.spot().plane()];
            b.set(a.spot().blockX(), f + 1, a.spot().blockZ(), "runecraft:" + a.block());
        }
        // The rope hanging down the hole, beside the spot the climb lands on.
        b.fill(ROPE.blockX(), FLOOR[2] + 1, ROPE.blockZ(), ROPE.blockX(), FLOOR[2] + 3, ROPE.blockZ(), "runecraft:" + BLOCK_ROPE);
        // Ashuelot Reis keeps the bank in the refuge before Nex's chamber (here a bank booth).
        Spot bank = new Spot(0, 2905, 5207);
        b.set(bank.blockX(), FLOOR[0] + 1, bank.blockZ(), "runecraft:bank_booth");
        return b.build();
    }

    /** Every tile of a level with a given map character (for tests). */
    static List<Spot> tilesOf(int plane, char c) {
        List<Spot> out = new ArrayList<>();
        String[] r = rows(plane);
        for (int j = 0; j < r.length; j++) {
            for (int i = 0; i < r[j].length(); i++) {
                if (r[j].charAt(i) == c) {
                    out.add(new Spot(plane, x0(plane) + i, top(plane) - j));
                }
            }
        }
        return Collections.unmodifiableList(out);
    }
}
