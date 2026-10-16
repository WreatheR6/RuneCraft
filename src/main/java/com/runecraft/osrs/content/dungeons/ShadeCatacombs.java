package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.content.CommonItems;
import com.runecraft.osrs.world.StructureDef;

import java.util.List;

import static com.runecraft.osrs.content.dungeons.Cave.mc;
import static com.runecraft.osrs.content.dungeons.Cave.rc;

/**
 * The shade catacombs below Mort'ton, entered just north of the general store (tile 3482, 3313; in OSRS the entrance
 * is inside the walls - here it is just outside them): behind wooden doors that open for any shade key of their metal
 * (the key is kept, as in the minigames module's instanced catacombs), the phrin shades' crypt (bronze), the riyl shades
 * to the east (steel), the asyn shades to the south-east (black), the fiyr shades in the crypt to the south-west (silver)
 * and, since the 2021 rework, the urium shades in the deepest crypt behind the gold doors. Each crypt is lined with
 * coffins. Each crypt has two key chests of its metal (bronze in the phrin crypt ... gold in the urium crypt; approx: OSRS
 * scatters more chests of several metals); a chest takes one shade key of its metal and rolls the wiki chest tables.
 */
public final class ShadeCatacombs {
    /** Floor height of the catacombs. */
    static final int FLOOR = 28;

    /**
     * The key chest block ids, bronze to gold: the Shades of Mort'ton chest blocks, registered and opened (inside or outside
     * the minigame's instance) by the minigames module.
     */
    public static final List<String> CHESTS = List.of("shade_chest", "shade_chest_steel", "shade_chest_black",
            "shade_chest_silver", "shade_chest_gold");

    /**
     * A key door: the metal of the shade keys that open it, its block column (x, z; the door stands at FLOOR + 1) and
     * the direction (dx, dz) of the crypt it guards. Leaving a crypt needs no key.
     */
    public record Door(String metal, int x, int z, int dx, int dz) {
        public String block() {
            return DungeonBlocks.shadeDoor(metal);
        }

        /** True if the block column (px, pz) lies on the crypt side of the door. */
        public boolean inside(double px, double pz) {
            return (px - (x + 0.5)) * dx + (pz - (z + 0.5)) * dz > 0;
        }
    }

    private ShadeCatacombs() {
    }

    private static int bx(int t) {
        return StructureDef.bx(t);
    }

    private static int bz(int t) {
        return StructureDef.bz(t);
    }

    /** The five key doors, bronze to gold. */
    public static List<Door> doors() {
        String[] m = CommonItems.SHADE_KEY_METALS;
        return List.of(
                new Door(m[0], bx(3482), bz(3305), 0, 1),    // the stair hall, south into the phrin crypt
                new Door(m[1], bx(3492), bz(3294), 1, 0),    // east into the riyl crypt
                new Door(m[2], bx(3502), bz(3284), 0, 1),    // south into the asyn crypt
                new Door(m[3], bx(3492), bz(3274), -1, 0),   // west into the fiyr crypt
                new Door(m[4], bx(3481), bz(3262), 0, 1));   // south into the urium crypt
    }

    /** The door with its block at column (x, z), or null. */
    public static Door doorAt(int x, int z) {
        for (Door d : doors()) {
            if (d.x() == x && d.z() == z) {
                return d;
            }
        }
        return null;
    }

    static Dungeons.Dungeon build() {
        Cave c = new Cave("dng_shade_catacombs", "Mort'ton catacombs", mc("deepslate_bricks"), mc("cobbled_deepslate"),
                mc("pearlescent_froglight"));
        int f = FLOOR;
        int ex = bx(3482);
        int ez = bz(3313);
        c.room(ex - 6, ez - 5, ex + 6, ez + 5, f, 5);
        c.hallZ(ex, ez + 5, ex, bz(3300), f, 3, 4);
        DungeonsLairs.tRoom(c, 3472, 3288, 3490, 3300, f, 6);                  // phrin shades
        c.hall(bx(3490), bz(3294), bx(3494), bz(3294), f, 3, 4);
        DungeonsLairs.tRoom(c, 3494, 3286, 3510, 3302, f, 6);                  // riyl shades
        c.hallZ(bx(3502), bz(3286), bx(3502), bz(3282), f, 3, 4);
        DungeonsLairs.tRoom(c, 3494, 3266, 3510, 3282, f, 6);                  // asyn shades
        c.hall(bx(3494), bz(3274), bx(3490), bz(3274), f, 3, 4);
        DungeonsLairs.tRoom(c, 3472, 3264, 3490, 3284, f, 7);                  // fiyr shades
        c.hallZ(bx(3481), bz(3264), bx(3481), bz(3260), f, 3, 4);
        DungeonsLairs.tRoom(c, 3472, 3246, 3490, 3260, f, 7);                  // urium shades
        c.carve();
        for (Door d : doors()) {
            door(c, d, f);
        }
        // Coffins along the crypt walls, cobwebs and soul fires; the urium crypt's black flame.
        coffins(c, 3473, 3289, 3489, 3299, f);
        coffins(c, 3495, 3287, 3509, 3301, f);
        coffins(c, 3495, 3267, 3509, 3281, f);
        coffins(c, 3473, 3265, 3489, 3283, f);
        coffins(c, 3473, 3247, 3489, 3259, f);
        // Two key chests of the crypt's metal in its western corners (the minigames module opens them; see CHESTS).
        int[][] crypts = {{3473, 3289, 3299}, {3495, 3287, 3301}, {3495, 3267, 3281}, {3473, 3265, 3283}, {3473, 3247, 3259}};
        for (int i = 0; i < crypts.length; i++) {
            c.set(bx(crypts[i][0]), f + 1, bz(crypts[i][2]), rc(CHESTS.get(i)));
            c.set(bx(crypts[i][0]), f + 1, bz(crypts[i][1]) + 1, rc(CHESTS.get(i)));
        }
        for (int[] p : new int[][]{{3481, 3294}, {3502, 3294}, {3502, 3274}, {3481, 3274}, {3481, 3253}}) {
            c.set(bx(p[0]), f + 1, bz(p[1]), mc("soul_campfire"));
        }
        for (int x = bx(3474); x <= bx(3508); x += 7) {
            c.set(x, f + 6, bz(3299), mc("cobweb")).set(x + 2, f + 6, bz(3267), mc("cobweb"));
        }
        c.fill(bx(3474), f + 1, bz(3249), bx(3474), f + 3, bz(3249), mc("crying_obsidian"));
        c.fill(bx(3488), f + 1, bz(3249), bx(3488), f + 3, bz(3249), mc("crying_obsidian"));
        c.entrance(ex, ez, f, mc("deepslate_bricks"), mc("cobbled_deepslate"), 'n');
        DungeonsLairs.tCamp(c, 3473, 3289, 3489, 3299, f, 3, 35, "phrin_shade");
        DungeonsLairs.tCamp(c, 3495, 3287, 3509, 3301, f, 3, 35, "riyl_shade");
        DungeonsLairs.tCamp(c, 3495, 3267, 3509, 3281, f, 3, 35, "asyn_shade");
        DungeonsLairs.tCamp(c, 3473, 3265, 3489, 3283, f, 3, 50, "fiyr_shade");
        DungeonsLairs.tCamp(c, 3473, 3248, 3489, 3258, f, 3, 50, "urium_shade"); // approx: count and respawn
        return c.build();
    }

    /** A key door across a 3-wide hall: rock on either side and above, the door block in the middle. */
    private static void door(Cave c, Door d, int f) {
        int x = d.x();
        int z = d.z();
        if (d.dz() != 0) {
            c.fill(x - 1, f + 1, z, x - 1, f + 4, z, c.wall).fill(x + 1, f + 1, z, x + 1, f + 4, z, c.wall);
        } else {
            c.fill(x, f + 1, z - 1, x, f + 4, z - 1, c.wall).fill(x, f + 1, z + 1, x, f + 4, z + 1, c.wall);
        }
        c.fill(x, f + 2, z, x, f + 4, z, c.wall);
        c.set(x, f + 1, z, rc(d.block()));
    }

    /** Rows of stone coffins against the north and south walls of a crypt (tile rectangle inside it). */
    private static void coffins(Cave c, int tx1, int ty1, int tx2, int ty2, int f) {
        for (int x = bx(tx1) + 2; x <= bx(tx2) - 1; x += 4) {
            c.fill(x, f + 1, bz(ty2), x + 1, f + 1, bz(ty2), mc("polished_deepslate_slab"));
            c.fill(x, f + 1, bz(ty1) + 1, x + 1, f + 1, bz(ty1) + 1, mc("polished_deepslate_slab"));
        }
    }
}
