package com.runecraft.osrs.content.world;

import com.runecraft.osrs.world.StructureDef;

import java.util.List;

import static com.runecraft.osrs.content.world.KandarinKit.*;

import com.runecraft.osrs.content.world.KandarinKit.Bld;
import com.runecraft.osrs.content.world.KandarinKit.Pal;

/** The Tree Gnome Stronghold: the Grand Tree, gnome huts, the agility course, the swamp, patches, restaurant and gate. */
final class KandarinGnomes {
    private KandarinGnomes() {
    }

    static void addAll(List<StructureDef> out) {
        out.add(stronghold());
        out.add(restaurant());
        out.add(mine());
        out.add(patches());
        out.add(gate());
    }

    private static final String BARK = "minecraft:jungle_wood";
    private static final String PLANK = "minecraft:jungle_planks";
    private static final String LEAF = "minecraft:oak_leaves[persistent=true]";

    /** Reserved tile rects (no forest trees): x1, y1, x2, y2. */
    private static final int[][] RESERVED = {
            {2448, 3478, 2486, 3512}, {2466, 3416, 2494, 3442}, {2436, 3494, 2452, 3510}, {2442, 3480, 2470, 3492},
            {2418, 3476, 2436, 3502}, {2450, 3436, 2470, 3454}, {2426, 3406, 2444, 3422}, {2474, 3482, 2490, 3498},
            {2455, 3405, 2466, 3520}, {2418, 3444, 2500, 3449}};

    static StructureDef stronghold() {
        KandarinKit k = new KandarinKit("gnome_stronghold", "Tree Gnome Stronghold").replaces("GNOME_STRONGHOLD");
        k.flatten(2418, 3405, 2500, 3520);
        k.road(2460, 3405, 2461, 3480, PATH, GRAVEL);
        k.road(2420, 3446, 2499, 3447, PATH, GRAVEL);
        k.road(2462, 3478, 2470, 3484, PATH, null);
        k.road(2440, 3448, 2441, 3470, PATH, null);
        k.road(2480, 3448, 2481, 3482, PATH, null);

        grandTree(k);
        agilityCourse(k);

        // Spirit tree by the centre, herb patch garden
        int sx = KandarinKit.X(2465);
        int sz = KandarinKit.Z(3441);
        k.blob(sx, 9, sz, 4, 3, 4, LEAF, true);
        k.fill(sx - 1, 0, sz - 1, sx + 1, 8, sz + 1, "minecraft:oak_wood");
        k.set(sx, 3, sz + 2, "minecraft:carved_pumpkin[facing=south]");
        k.set(sx - 1, 4, sz + 2, "minecraft:oak_button[face=wall,facing=south]");
        k.fence(2450, 3436, 2454, 3440, "minecraft:jungle_fence");
        k.tClear(2452, 3440, 2452, 3440, 0, 0);
        k.floor(2451, 3437, 2453, 3439, "minecraft:farmland[moisture=7]");
        k.tFill(2452, 3438, 2452, 3438, 0, 0, "minecraft:wheat[age=7]"); // a vegetable plot (no herb patch in OSRS)
        k.at(2451, 3437, -1, WATER);

        // Gnome huts
        int[][] huts = {{2445, 3460, 4}, {2436, 3470, 3}, {2452, 3472, 4}, {2474, 3458, 4}, {2488, 3468, 3}, {2492, 3456, 4},
                {2489, 3478, 3}, {2434, 3456, 3}, {2426, 3462, 4}, {2496, 3434, 3}, {2446, 3426, 4}, {2436, 3436, 3},
                {2470, 3470, 3}, {2425, 3438, 3}, {2490, 3505, 4}, {2496, 3515, 3}, {2430, 3510, 3}};
        String[] roofs = {HAY, "minecraft:jungle_leaves[persistent=true]", "minecraft:oak_leaves[persistent=true]"};
        char[] doors = {'s', 'e', 'w', 'n'};
        for (int i = 0; i < huts.length; i++) {
            int[] h = huts[i];
            k.gnomeHut(KandarinKit.X(h[0]), KandarinKit.Z(h[1]), h[2], i % 2 == 0 ? PLANK : "minecraft:stripped_jungle_log", roofs[i % 3], doors[i % 4]);
        }
        // Blurberry's bar and the archery shop (two-storey gnome house)
        Pal gp = GNOME.roof("thatch");
        Bld bar = k.house(2476, 3484, 2486, 3492, 2, gp, 'w', FURN);
        k.fill(bar.x1() + 2, 0, bar.z1() + 2, bar.x1() + 2, 0, bar.z2() - 2, "minecraft:jungle_slab[type=top]");
        k.fill(bar.x1() + 1, 0, bar.z1() + 1, bar.x1() + 1, 1, bar.z2() - 1, "minecraft:barrel[facing=east]");
        k.set(bar.x1() + 1, 0, bar.cz(), RANGE);
        k.set(bar.x2() - 2, 4, bar.z2() - 2, ARCHERY);
        k.set(bar.x2() - 3, 4, bar.z2() - 2, "minecraft:fletching_table");
        // Gnome ball field
        k.floor(2420, 3478, 2434, 3500, GRASS);
        k.tFill(2420, 3489, 2434, 3489, -1, -1, "minecraft:white_concrete");
        for (int[] g : new int[][]{{2427, 3479}, {2427, 3499}}) {
            int x = KandarinKit.X(g[0]);
            int z = KandarinKit.Z(g[1]);
            k.fill(x, 0, z, x, 4, z, "minecraft:jungle_fence");
            k.set(x, 5, z, "minecraft:jungle_trapdoor[half=top]");
        }
        // The swamp south-west
        int wx = KandarinKit.X(2422);
        int wz = KandarinKit.Z(3422);
        k.blob(wx + 8, -2, wz, 9, 1, 7, WATER, false);
        k.fill(wx - 2, 0, wz - 8, wx + 18, 2, wz + 8, AIR);
        for (int i = 0; i < 18; i++) {
            int x = wx + (KandarinKit.hash(i, 7) % 17);
            int z = wz - 6 + (KandarinKit.hash(i, 9) % 13);
            k.set(x, 0, z, i % 3 == 0 ? "minecraft:lily_pad" : "minecraft:air");
            k.set(x, -1, z + 9, "minecraft:mud");
        }
        // Forest
        for (int x = 2420; x <= 2498; x += 5) {
            for (int y = 3407; y <= 3518; y += 5) {
                int h = KandarinKit.hash(x * 7, y * 13);
                int tx = x + h % 3;
                int ty = y + (h / 3) % 3;
                if (reserved(tx, ty) || h % 3 == 0) {
                    continue;
                }
                int bx = KandarinKit.X(tx);
                int bz = KandarinKit.Z(ty);
                switch (h % 4) {
                    case 0 -> k.bigTree(bx, bz, 7, 3, "minecraft:jungle_log", "minecraft:jungle_leaves");
                    case 1 -> k.oak(bx, bz, 6);
                    case 2 -> k.bigTree(bx, bz, 6, 2, "minecraft:oak_log", "minecraft:oak_leaves");
                    default -> k.tree(bx, bz, 5, "minecraft:birch_log", "minecraft:birch_leaves");
                }
            }
        }
        k.meadow(KandarinKit.X(2418), KandarinKit.Z(3520), KandarinKit.X(2500), KandarinKit.Z(3405), 9);
        KandarinGrandTree.tidy(k, KandarinKit.X(2466), KandarinKit.Z(3494));
        return k.build();
    }

    private static boolean reserved(int x, int y) {
        for (int[] r : RESERVED) {
            if (x >= r[0] - 2 && x <= r[2] + 2 && y >= r[1] - 2 && y <= r[3] + 2) {
                return true;
            }
        }
        for (int[] h : new int[][]{{2445, 3460}, {2436, 3470}, {2452, 3472}, {2474, 3458}, {2488, 3468}, {2492, 3456}, {2489, 3478},
                {2434, 3456}, {2426, 3462}, {2496, 3434}, {2446, 3426}, {2436, 3436}, {2470, 3470}, {2425, 3438}, {2490, 3505},
                {2496, 3515}, {2430, 3510}, {2422, 3422}, {2426, 3422}}) {
            if (Math.abs(x - h[0]) <= 5 && Math.abs(y - h[1]) <= 5) {
                return true;
            }
        }
        return false;
    }

    /** The Grand Tree (see {@link KandarinGrandTree}). */
    private static void grandTree(KandarinKit k) {
        KandarinGrandTree.build(k, KandarinKit.X(2466), KandarinKit.Z(3494));
    }

    /** Gnome Stronghold Agility Course (7 obstacles). */
    private static void agilityCourse(KandarinKit k) {
        int c = 0;
        // swamp channel and the log balance (north to south)
        k.water(2472, 3430, 2476, 3437, 2);
        int lx = KandarinKit.X(2474);
        k.fill(lx, -1, KandarinKit.Z(3438), lx, -1, KandarinKit.Z(3429) + 1, "minecraft:oak_log[axis=z]");
        k.set(lx, -1, KandarinKit.Z(3434), obstacle(c, 0));
        k.set(lx - 3, -1, KandarinKit.Z(3432), "minecraft:lily_pad");
        k.set(lx + 4, -1, KandarinKit.Z(3435), "minecraft:lily_pad");
        // tree T1 with deck A (net climb) and deck B (branch)
        int t1x = KandarinKit.X(2473);
        int tz = KandarinKit.Z(3423);
        k.fill(t1x, 0, tz, t1x, 15, tz, "minecraft:jungle_log");
        k.disc(t1x, tz, 6, 6, 3, PLANK);
        k.disc(t1x, tz, 11, 11, 2, PLANK);
        k.blob(t1x, 16, tz, 4, 2, 4, "minecraft:jungle_leaves[persistent=true]", true);
        k.fill(t1x - 2, 0, tz + 4, t1x + 2, 6, tz + 4, "minecraft:scaffolding[distance=0]");
        k.set(t1x, 6, tz + 3, obstacle(c, 1));
        if (!k.ladder(t1x + 1, tz, 7, 11, "east")) {
            k.ladder(t1x - 1, tz, 7, 11, "west");
        }
        k.set(t1x, 11, tz + 2, obstacle(c, 2));
        // balancing rope east to tree T2
        int t2x = KandarinKit.X(2484);
        k.fill(t2x, 0, tz, t2x, 15, tz, "minecraft:jungle_log");
        k.disc(t2x, tz, 11, 11, 2, PLANK);
        k.blob(t2x, 16, tz, 4, 2, 4, "minecraft:jungle_leaves[persistent=true]", true);
        k.fill(t1x + 3, 11, tz, t2x - 3, 11, tz, "minecraft:jungle_slab[type=top]");
        k.set(t2x - 2, 11, tz, obstacle(c, 3));
        // down tree T2
        if (!k.ladder(t2x + 1, tz, 0, 11, "east")) {
            k.ladder(t2x - 1, tz, 0, 11, "west");
        }
        k.set(t2x + 2, -1, tz, obstacle(c, 4));
        // obstacle net
        int nz = KandarinKit.Z(3426);
        k.fill(KandarinKit.X(2482), 0, nz, KandarinKit.X(2487), 3, nz, "minecraft:scaffolding[distance=0]");
        k.fill(KandarinKit.X(2482), 4, nz, KandarinKit.X(2487), 4, nz, obstacle(c, 5));
        // obstacle pipes (south to north)
        for (int px : new int[]{KandarinKit.X(2484), KandarinKit.X(2487)}) {
            int za = KandarinKit.Z(3434);
            int zb = KandarinKit.Z(3428) + 1;
            k.walls(px - 1, 0, za, px + 1, 2, zb, "minecraft:iron_block");
            k.fill(px - 1, 2, za, px + 1, 2, zb, "minecraft:iron_block");
            k.clear(px, 0, za, px, 1, zb);
            k.set(px, -1, za - 2, obstacle(c, 6));
        }
        k.fence(2468, 3418, 2492, 3442, "minecraft:jungle_fence");
        k.tClear(2474, 3442, 2474, 3442, 0, 0);
        k.tClear(2492, 3436, 2492, 3437, 0, 0);
    }

    static StructureDef restaurant() {
        KandarinKit k = new KandarinKit("gnome_restaurant", "Gnome restaurant").replaces("mg_grand_tree_restaurant");
        Bld r = k.house(2440, 3498, 2448, 3506, 2, GNOME.roof("thatch"), 'e', FURN | WIDE_DOOR);
        k.set(r.x1() + 1, 0, r.z1() + 1, RANGE);
        k.set(r.x1() + 1, 0, r.z1() + 2, "minecraft:smoker[facing=east]");
        k.set(r.x1() + 2, 0, r.z2() - 1, "runecraft:gnome_restaurant_rewards");
        k.set(r.x2() + 2, 0, r.z1() + 2, "runecraft:gnome_restaurant_entrance");
        k.flag(r.x2() + 2, 0, r.z2(), "lime", "yellow");
        return k.build();
    }

    static StructureDef mine() {
        KandarinKit k = new KandarinKit("grand_tree_mine_entrance", "Grand Tree mine").replaces("grand_tree_mine");
        int x = KandarinKit.X(2446);
        int z = KandarinKit.Z(3486);
        k.blob(x - 2, 0, z, 4, 4, 5, "minecraft:stone", false);
        k.clear(x - 3, 0, z - 1, x + 3, 2, z + 1);
        k.fill(x + 2, 0, z - 2, x + 2, 3, z - 2, "minecraft:spruce_log");
        k.fill(x + 2, 0, z + 2, x + 2, 3, z + 2, "minecraft:spruce_log");
        k.fill(x + 2, 3, z - 2, x + 2, 3, z + 2, "minecraft:spruce_log[axis=z]");
        k.fill(x - 3, -1, z, x + 4, -1, z, GRAVEL);
        k.fill(x - 3, 0, z, x + 4, 0, z, "minecraft:rail[shape=east_west]");
        k.set(x - 3, 0, z - 1, "minecraft:lantern");
        return k.build();
    }

    static StructureDef patches() {
        KandarinKit k = new KandarinKit("gnome_stronghold_patches", "Gnome Stronghold patches").replaces("gnome_stronghold_patches");
        k.fence(2428, 3408, 2441, 3420, "minecraft:jungle_fence");
        k.tClear(2434, 3420, 2435, 3420, 0, 0);
        // No spirit tree patch here: OSRS spirit tree patches are Port Sarim, Etceteria, Brimhaven, Hosidius and the Farming Guild.
        for (int[] p : new int[][]{{2430, 3410, 0}, {2439, 3410, 1}}) {
            k.floor(p[0] - 1, p[1] - 1, p[0] + 1, p[1] + 1, "minecraft:podzol");
            k.at(p[0], p[1], 0, "runecraft:" + new String[]{"tree_patch", "fruit_tree_patch"}[p[2]]);
        }
        k.at(2439, 3418, 0, "runecraft:compost_bin");
        return k.build();
    }

    static StructureDef gate() {
        KandarinKit k = new KandarinKit("gnome_stronghold_gate", "Gnome Stronghold gate");
        k.flatten(2452, 3378, 2470, 3394);
        k.road(2460, 3378, 2461, 3394, PATH, GRAVEL);
        for (int tx : new int[]{2457, 2464}) {
            int x = KandarinKit.X(tx);
            int z = KandarinKit.Z(3386);
            k.disc(x, z, 0, 12, 2, BARK);
            k.blob(x, 14, z, 5, 3, 5, LEAF, true);
        }
        int z = KandarinKit.Z(3386);
        k.fill(KandarinKit.X(2459), 0, z, KandarinKit.X(2462) + 1, 0, z, "minecraft:jungle_fence_gate[facing=north,open=true]");
        k.fill(KandarinKit.X(2459), 4, z, KandarinKit.X(2462) + 1, 5, z, BARK);
        k.fill(KandarinKit.X(2452), 0, z, KandarinKit.X(2455), 2, z, "minecraft:jungle_fence");
        k.fill(KandarinKit.X(2467), 0, z, KandarinKit.X(2470), 2, z, "minecraft:jungle_fence");
        return k.build();
    }
}
