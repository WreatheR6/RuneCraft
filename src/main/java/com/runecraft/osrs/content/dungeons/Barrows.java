package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.data.Boss;
import com.runecraft.osrs.world.StructureDef;

import java.util.List;

import static com.runecraft.osrs.content.dungeons.Cave.mc;
import static com.runecraft.osrs.content.dungeons.Cave.rc;

/**
 * The Barrows underground, as in OSRS: a crypt beneath each of the six burial mounds (dig on top of
 * the mound with a spade to drop in; the staircase leads back up to the mound top), the brother's
 * sarcophagus in each crypt, and the tunnels - a 3x3 grid of rooms joined by doors, some of them
 * locked each run, with the Barrows chest in the centre room. One brother per run (see
 * {@link BarrowsRun}) hides the tunnel entrance in his crypt and comes out of the chest instead.
 *
 * <p>All geometry is here in block coordinates so the Minecraft side can look up which mound a
 * player is digging on, which crypt a sarcophagus belongs to and which tunnel door was opened.
 */
public final class Barrows {
    /** The six brothers, in the order of the run-state bit masks. */
    public static final List<Boss> BROTHERS = List.of(Boss.AHRIM, Boss.DHAROK, Boss.GUTHAN, Boss.KARIL, Boss.TORAG, Boss.VERAC);
    /** OSRS tiles of the six mounds (same order as {@link #BROTHERS}); the world module builds the mounds there. */
    private static final int[][] MOUND_TILES = {{3565, 3289}, {3575, 3298}, {3577, 3283}, {3566, 3275}, {3553, 3283}, {3557, 3298}};

    /** Crypt floor block height (air from y = 41 to 45). */
    public static final int CRYPT_FLOOR = 40;
    public static final int CRYPT_HEIGHT = 5;
    /** Half size of a crypt chamber (11x11 blocks). */
    public static final int CRYPT_HALF = 5;
    /** Tunnels floor block height (air from y = 25 to 29). */
    public static final int TUNNEL_FLOOR = 24;
    public static final int TUNNEL_HEIGHT = 5;
    /** Half size of a tunnel room (9x9 blocks) and distance between room centres. */
    public static final int ROOM_HALF = 4;
    public static final int ROOM_SPACING = 16;
    /** Centre room index of the 3x3 grid: the chest room. */
    public static final int CHEST_ROOM = 4;
    /** Horizontal radius (blocks) around a mound's centre in which digging drops into its crypt. */
    public static final int DIG_RADIUS = 7;

    public static final String SARCOPHAGUS = "dng_barrows_sarcophagus";
    public static final String CRYPT_STAIRS = "dng_barrows_crypt_stairs";
    public static final String TUNNEL_DOOR = "dng_barrows_door";
    public static final String CHEST = "dng_barrows_chest";

    private static final String WALL = mc("deepslate_bricks");
    private static final String FLOOR = mc("deepslate_tiles");
    private static final String TUNNEL_WALL = mc("cracked_deepslate_bricks");
    private static final String TUNNEL_FLOOR_BLOCK = mc("cobbled_deepslate");

    private Barrows() {
    }

    // ------------------------------------------------------------------ geometry

    /** A mound/crypt: the mound centre column (block coordinates) is also the crypt centre. */
    public record Crypt(int index, Boss brother, int x, int z) {
        /** The sarcophagus block (north side of the chamber). */
        public int[] sarcophagus() {
            return new int[]{x, CRYPT_FLOOR + 1, z - 3};
        }

        /** Floor block the brother is summoned onto (he stands on it, in front of his sarcophagus). */
        public int[] spawnFloor() {
            return new int[]{x, CRYPT_FLOOR, z - 1};
        }

        /** Where a player lands after digging (feet position). */
        public int[] arrival() {
            return new int[]{x, CRYPT_FLOOR + 1, z + 2};
        }

        /** The exit: a ladder up the south wall to the crypt stairs block set in the ceiling. */
        public int[] stairs() {
            return new int[]{x, CRYPT_FLOOR + CRYPT_HEIGHT + 1, z + CRYPT_HALF};
        }

        /** True if a player standing with feet at (bx, by, bz) is on top of this mound. */
        public boolean onMound(int bx, int by, int bz) {
            int dx = bx - x;
            int dz = bz - z;
            return dx * dx + dz * dz <= DIG_RADIUS * DIG_RADIUS && by >= StructureDef.GROUND - 2 && by <= StructureDef.GROUND + 12;
        }

        /** True if the block is inside this crypt's chamber (walls included). */
        public boolean contains(int bx, int by, int bz) {
            return Math.abs(bx - x) <= CRYPT_HALF + 1 && Math.abs(bz - z) <= CRYPT_HALF + 1
                    && by >= CRYPT_FLOOR - 1 && by <= CRYPT_FLOOR + CRYPT_HEIGHT + 2;
        }
    }

    private static List<Crypt> crypts;

    public static synchronized List<Crypt> crypts() {
        if (crypts == null) {
            Crypt[] list = new Crypt[BROTHERS.size()];
            for (int i = 0; i < list.length; i++) {
                list[i] = new Crypt(i, BROTHERS.get(i), StructureDef.bx(MOUND_TILES[i][0]) + 1, StructureDef.bz(MOUND_TILES[i][1]) + 1);
            }
            crypts = List.of(list);
        }
        return crypts;
    }

    /** The index of the brother (in {@link #BROTHERS}), or -1. */
    public static int indexOf(Boss boss) {
        return boss == null ? -1 : BROTHERS.indexOf(boss);
    }

    /** The mound a player standing at (x, y, z) is on, or null. */
    public static Crypt moundAt(int x, int y, int z) {
        for (Crypt c : crypts()) {
            if (c.onMound(x, y, z)) {
                return c;
            }
        }
        return null;
    }

    /** The crypt containing the block, or null. */
    public static Crypt cryptAt(int x, int y, int z) {
        for (Crypt c : crypts()) {
            if (c.contains(x, y, z)) {
                return c;
            }
        }
        return null;
    }

    /** Centre block of the tunnels (under the middle of the Barrows). */
    public static int tunnelX() {
        return StructureDef.bx(3565);
    }

    public static int tunnelZ() {
        return StructureDef.bz(3286);
    }

    /** Centre {x, z} of tunnel room {@code room} (row-major 0..8, row 0 is the north row). */
    public static int[] roomCentre(int room) {
        int r = room / 3;
        int c = room % 3;
        return new int[]{tunnelX() + (c - 1) * ROOM_SPACING, tunnelZ() + (r - 1) * ROOM_SPACING};
    }

    /** The tunnel room (0..8) whose floor area contains column (x, z), or -1 (corridors and doors too). */
    public static int roomAt(int x, int z) {
        for (int room = 0; room < 9; room++) {
            int[] c = roomCentre(room);
            if (Math.abs(x - c[0]) <= ROOM_HALF && Math.abs(z - c[1]) <= ROOM_HALF) {
                return room;
            }
        }
        return -1;
    }

    /** The door {x, z} (block column of the door's centre) of tunnel edge {@code edge}. */
    public static int[] doorAt(int edge) {
        int[] rooms = BarrowsRun.edgeRooms(edge);
        int[] a = roomCentre(rooms[0]);
        int[] b = roomCentre(rooms[1]);
        return new int[]{(a[0] + b[0]) / 2, (a[1] + b[1]) / 2};
    }

    /**
     * The room on the far side of edge {@code e}'s door from a player standing at column (x, z): the
     * room the player is heading into when he opens that door.
     */
    public static int roomBeyondDoor(int e, int x, int z) {
        int[] rooms = BarrowsRun.edgeRooms(e);
        int[] a = roomCentre(rooms[0]);
        int[] b = roomCentre(rooms[1]);
        long da = (long) (x - a[0]) * (x - a[0]) + (long) (z - a[1]) * (z - a[1]);
        long db = (long) (x - b[0]) * (x - b[0]) + (long) (z - b[1]) * (z - b[1]);
        return da <= db ? rooms[1] : rooms[0];
    }

    /** The tunnel edge whose door is at column (x, z), or -1. */
    public static int edgeAtDoor(int x, int z) {
        for (int e = 0; e < BarrowsRun.EDGES; e++) {
            int[] d = doorAt(e);
            if (Math.abs(d[0] - x) <= 1 && Math.abs(d[1] - z) <= 1) {
                return e;
            }
        }
        return -1;
    }

    /** The chest block of the centre room. */
    public static int[] chest() {
        int[] c = roomCentre(CHEST_ROOM);
        return new int[]{c[0], TUNNEL_FLOOR + 1, c[1]};
    }

    /** True if the block is in the tunnels (walls included). */
    public static boolean inTunnels(int x, int y, int z) {
        int reach = ROOM_SPACING + ROOM_HALF + 1;
        return Math.abs(x - tunnelX()) <= reach && Math.abs(z - tunnelZ()) <= reach
                && y >= TUNNEL_FLOOR - 1 && y <= TUNNEL_FLOOR + TUNNEL_HEIGHT + 1;
    }

    /** True anywhere underground at the Barrows (crypts or tunnels): prayer drains there. */
    public static boolean underground(int x, int y, int z) {
        return inTunnels(x, y, z) || cryptAt(x, y, z) != null;
    }

    /** OSRS tile of the surface spot the chest sends players back to (in front of the mausoleum). */
    public static final int[] SURFACE_TILE = {3558, 3278};

    // ------------------------------------------------------------------ structure

    static Dungeons.Dungeon dungeon() {
        Cave c = new Cave("dng_barrows", "Barrows crypts and tunnels", WALL, FLOOR, null);
        int f = CRYPT_FLOOR;
        for (Crypt k : crypts()) {
            c.room(k.x() - CRYPT_HALF, k.z() - CRYPT_HALF, k.x() + CRYPT_HALF, k.z() + CRYPT_HALF, f, CRYPT_HEIGHT);
        }
        int t = TUNNEL_FLOOR;
        for (int room = 0; room < 9; room++) {
            int[] rc = roomCentre(room);
            c.room(rc[0] - ROOM_HALF, rc[1] - ROOM_HALF, rc[0] + ROOM_HALF, rc[1] + ROOM_HALF, t, TUNNEL_HEIGHT, TUNNEL_FLOOR_BLOCK);
        }
        for (int e = 0; e < BarrowsRun.EDGES; e++) {
            int[] rooms = BarrowsRun.edgeRooms(e);
            int[] a = roomCentre(rooms[0]);
            int[] b = roomCentre(rooms[1]);
            if (a[1] == b[1]) {
                c.room(a[0] + ROOM_HALF, a[1] - 1, b[0] - ROOM_HALF, a[1] + 1, t, 3, TUNNEL_FLOOR_BLOCK);
            } else {
                c.room(a[0] - 1, a[1] + ROOM_HALF, a[0] + 1, b[1] - ROOM_HALF, t, 3, TUNNEL_FLOOR_BLOCK);
            }
        }
        c.carve();

        // The crypts: sarcophagus, candles, soul lanterns, pillars and the way back up.
        for (Crypt k : crypts()) {
            int x = k.x();
            int z = k.z();
            int[] s = k.sarcophagus();
            c.fill(x - 2, f, z - 4, x + 2, f, z - 2, mc("polished_blackstone"));
            c.set(s[0], s[1], s[2], rc(SARCOPHAGUS));
            c.set(x - 1, f + 1, z - 3, mc("polished_blackstone_slab"));
            c.set(x + 1, f + 1, z - 3, mc("polished_blackstone_slab"));
            c.set(x - 2, f + 1, z - 4, mc("candle[candles=3,lit=true]"));
            c.set(x + 2, f + 1, z - 4, mc("candle[candles=3,lit=true]"));
            for (int[] p : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
                c.pillar(x + p[0], z + p[1], f, CRYPT_HEIGHT, mc("chiseled_deepslate"));
            }
            for (int[] p : new int[][]{{-3, -1}, {3, -1}, {-3, 3}, {3, 3}}) {
                c.set(x + p[0], f + CRYPT_HEIGHT, z + p[1], mc("soul_lantern[hanging=true]"));
            }
            c.set(x - 4, f + 1, z, mc("skeleton_skull"));
            c.set(x + 4, f + 1, z + 1, mc("cobweb"));
            c.set(x - 3, f + CRYPT_HEIGHT, z + 4, mc("cobweb"));
            c.fill(x - 5, f, z + 1, x - 4, f, z + 3, mc("mossy_cobblestone"));
            // Ladder up the south wall into the crypt stairs (which lead to the top of the mound).
            int[] st = k.stairs();
            c.ladder(x, z + CRYPT_HALF, f + 1, f + CRYPT_HEIGHT, "north", f + CRYPT_HEIGHT, f + CRYPT_HEIGHT);
            c.set(st[0], st[1], st[2], rc(CRYPT_STAIRS));
            c.entrances.add(new Dungeons.Entrance(x - 1, z + CRYPT_HALF - 1, x + 1, z + CRYPT_HALF + 1, st[1], false));
        }

        // The tunnels: doors on every corridor, the chest, lanterns and monsters.
        for (int e = 0; e < BarrowsRun.EDGES; e++) {
            int[] rooms = BarrowsRun.edgeRooms(e);
            int[] a = roomCentre(rooms[0]);
            int[] b = roomCentre(rooms[1]);
            int[] d = doorAt(e);
            if (a[1] == b[1]) {
                c.fill(d[0], t + 1, d[1] - 1, d[0], t + 3, d[1] + 1, TUNNEL_WALL);
            } else {
                c.fill(d[0] - 1, t + 1, d[1], d[0] + 1, t + 3, d[1], TUNNEL_WALL);
            }
            c.set(d[0], t + 1, d[1], rc(TUNNEL_DOOR));
            c.set(d[0], t + 2, d[1], rc(TUNNEL_DOOR));
        }
        for (int room = 0; room < 9; room++) {
            int[] rc = roomCentre(room);
            for (int[] p : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
                c.set(rc[0] + p[0], t + TUNNEL_HEIGHT, rc[1] + p[1], mc("soul_lantern[hanging=true]"));
            }
            if (room == CHEST_ROOM) {
                continue;
            }
            c.set(rc[0] - ROOM_HALF, t + 1, rc[1] - ROOM_HALF, mc("skeleton_skull"));
            c.set(rc[0] + ROOM_HALF, t + 1, rc[1] + ROOM_HALF, mc("cobweb"));
            c.set(rc[0] + ROOM_HALF, t + TUNNEL_HEIGHT, rc[1] - ROOM_HALF, mc("cobweb"));
            // OSRS tunnel monsters: crypt rats and spiders, their giant kin, bloodworms and skeletons.
            c.camp(rc[0] - 3, rc[1] - 3, rc[0] + 3, rc[1] + 3, t, 2, 50, "crypt_rat", "giant_crypt_rat", "crypt_spider",
                    "giant_crypt_spider", "bloodworm", "minecraft:skeleton");
        }
        int[] ch = chest();
        int[] cc = roomCentre(CHEST_ROOM);
        c.fill(cc[0] - 2, t, cc[1] - 2, cc[0] + 2, t, cc[1] + 2, mc("polished_blackstone_bricks"));
        c.set(ch[0], ch[1], ch[2], rc(CHEST));
        for (int[] p : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            c.set(cc[0] + p[0], t + 1, cc[1] + p[1], mc("candle[candles=4,lit=true]"));
        }
        return c.build();
    }
}
