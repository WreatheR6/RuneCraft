package com.runecraft.osrs.content.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The Grand Tree of the Tree Gnome Stronghold at landmark detail. The trunk, its flared root
 * buttresses and the great limbs are modelled block by block (twisting streaks of spruce, dark oak,
 * jungle and stripped bark) and written as vertical runs; the canopy is a layered union of leaf
 * domes and branch-end lobes written only into air.
 *
 * <p>Inside, the hollow trunk keeps an atrium open to the sky (only leaves above it) where King
 * Narnode and the gnomes stand. Six ring galleries climb the inside of the trunk, joined by straight
 * flights of stairs: the bank and the general store on the first, the kitchen on the second,
 * sleeping quarters, the library, a workshop and the look-out at the top, which opens onto a
 * wrap-around balcony and the walkway to the gnome glider. Two more balconies hang off the trunk.
 */
final class KandarinGrandTree {
    /** Top of the trunk wall (relative y). */
    private static final int TOP = 80;
    /** Walking heights of the ground floor and the seven galleries. */
    private static final int[] W = {0, 10, 19, 28, 37, 46, 55, 64};
    /** The top level (the look-out), which opens onto the wrap-around balcony. */
    private static final int LOOKOUT = 7;
    /** Radius of the atrium kept open to the sky. */
    private static final double ATRIUM = 8.5;
    /** Levels with an outer balcony, and the arc each one covers (degrees, 0 = east, 90 = south). */
    private static final int[][] BALCONIES = {{2, -50, 70}, {4, 110, 250}, {LOOKOUT, -180, 180}};
    /** Direction of the glider walkway from the top balcony (degrees). */
    private static final int GLIDER = -15;

    private static final String PLANK = "minecraft:jungle_planks";
    private static final String FENCE = "minecraft:jungle_fence";
    private static final String INNER = "minecraft:stripped_jungle_wood";
    private static final String LANTERN_HANG = "minecraft:lantern[hanging=true]";

    private final KandarinKit k;
    private final int gx;
    private final int gz;
    private final Vox wood = new Vox();
    private final Vox leaves = new Vox();
    private final List<double[]> tips = new ArrayList<>();
    private final List<double[]> branchMids = new ArrayList<>();

    private KandarinGrandTree(KandarinKit k, int gx, int gz) {
        this.k = k;
        this.gx = gx;
        this.gz = gz;
    }

    /** Builds the Grand Tree centred on block (gx, gz). */
    static void build(KandarinKit k, int gx, int gz) {
        new KandarinGrandTree(k, gx, gz).run();
    }

    private void run() {
        trunk();
        roots();
        branches();
        carve();
        wood.emit(k, false);
        groundFloor();
        galleries();
        stairs();
        furnish();
        balconies();
        canopy();
        leaves.emit(k, true);
        hangings();
        trunkVines();
    }

    /**
     * Clears the meadow flowers the stronghold scatters over open ground from the floor inside the
     * trunk (they cannot stand on planks). Call after the meadow.
     */
    static void tidy(KandarinKit k, int gx, int gz) {
        String[] meadow = {"minecraft:short_grass", "minecraft:dandelion", "minecraft:poppy", "minecraft:oxeye_daisy", "minecraft:azure_bluet"};
        for (String f : meadow) {
            k.replace(gx - 13, 0, gz - 13, gx + 13, 0, gz + 13, f, "minecraft:air");
            k.replace(gx - 17, 0, gz - 6, gx + 17, 0, gz + 6, f, "minecraft:air");
            k.replace(gx - 6, 0, gz - 17, gx + 6, 0, gz + 17, f, "minecraft:air");
        }
    }

    // ------------------------------------------------------------------ shape

    /** Outer radius of the trunk at height y and angle th (radians, 0 = east, pi/2 = south). */
    private static double radius(int y, double th) {
        double flare = y < 12 ? 5 * sq((12 - y) / 12.0) : 0;
        // less flare towards the bar south-east of the tree
        double deg = Math.toDegrees(th);
        double off = Math.abs(deg - 25);
        if (off < 45) {
            flare *= 1 - 0.75 * 0.5 * (1 + Math.cos(Math.PI * off / 45));
        }
        double base = y < 12 ? 16 : 16 - 3.5 * (y - 12) / (double) (TOP - 12);
        double flute = (y < 12 ? 0.9 * (12 - y) / 12.0 + 0.35 : 0.35) * Math.cos(7 * th + 0.5 * Math.sin(y / 13.0));
        return base + flare + flute;
    }

    private static double inner(int y, double th) {
        return radius(y, th) - 2.4;
    }

    private static double sq(double v) {
        return v * v;
    }

    private double dist(int x, int z) {
        return Math.sqrt(sq(x - gx) + sq(z - gz));
    }

    private double angle(int x, int z) {
        return Math.atan2(z - gz, x - gx);
    }

    /** Bark in twisting vertical streaks, with the odd knot. */
    private static String bark(double th, int y) {
        double s = (th + Math.PI) / (2 * Math.PI) * 40 + 0.8 * Math.sin(y / 10.0) + y / 40.0;
        int i = (int) Math.floor(s);
        if (KandarinKit.hash(i * 13 + Math.floorDiv(y, 5), Math.floorDiv(y, 5) + 99) % 100 < 3) {
            return "minecraft:stripped_dark_oak_wood";
        }
        int h = KandarinKit.hash(i * 7 + 3, 11) % 100;
        return h < 40 ? "minecraft:spruce_wood" : h < 60 ? "minecraft:dark_oak_wood"
                : h < 85 ? "minecraft:jungle_wood" : "minecraft:stripped_spruce_wood";
    }

    // ------------------------------------------------------------------ trunk, roots and limbs

    private void trunk() {
        for (int y = -1; y <= TOP; y++) {
            for (int dx = -24; dx <= 24; dx++) {
                for (int dz = -24; dz <= 24; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    double th = Math.atan2(dz, dx);
                    double r = radius(y, th);
                    if (d > r + 0.5) {
                        continue;
                    }
                    int x = gx + dx;
                    int z = gz + dz;
                    if (d > r - 1.2) {
                        boolean moss = y <= 2 && KandarinKit.hash(x * 3 + y, z) % 5 == 0;
                        wood.put(x, y, z, moss ? "minecraft:moss_block" : bark(th, y));
                    } else if (d > r - 2.4) {
                        wood.put(x, y, z, y == TOP ? "minecraft:jungle_wood" : INNER);
                    } else if (y == -1) {
                        wood.put(x, y, z, PLANK);
                    }
                }
            }
        }
    }

    /** Flared root buttresses: tapering ridges running out from the trunk and into the ground. */
    private void roots() {
        int[] angles = {-10, -55, -100, -145, 170, 140, 112, 68};
        for (int n = 0; n < angles.length; n++) {
            double base = Math.toRadians(angles[n]);
            int len = 10 + KandarinKit.hash(n, 5) % 5;
            double h0 = 9 + KandarinKit.hash(n, 6) % 4;
            double r0 = radius(0, base) - 3;
            double bend = (n % 2 == 0 ? 1 : -1) * 0.16;
            for (double t = 0; t <= len; t += 0.5) {
                double u = t / len;
                double th = base + bend * Math.sin(u * Math.PI);
                double d = r0 + t;
                double px = gx + d * Math.cos(th);
                double pz = gz + d * Math.sin(th);
                double top = h0 * Math.pow(1 - u, 1.7) - (u > 0.85 ? 1 : 0);
                double w = 2.6 * (1 - u) + 0.7;
                for (int x = (int) Math.floor(px - w); x <= (int) Math.ceil(px + w); x++) {
                    for (int z = (int) Math.floor(pz - w); z <= (int) Math.ceil(pz + w); z++) {
                        double e = Math.sqrt(sq(x - px) + sq(z - pz));
                        if (e > w) {
                            continue;
                        }
                        double cd = dist(x, z);
                        double ca = angle(x, z);
                        int ty = (int) Math.round(top - e * 1.3);
                        for (int y = -2; y <= ty; y++) {
                            if (y >= -1 && cd <= inner(y, ca) + 0.3) {
                                continue;
                            }
                            if (wood.get(x, y, z) == null) {
                                wood.put(x, y, z, bark(ca, y));
                            }
                        }
                    }
                }
            }
        }
    }

    /** The great limbs reaching out from the top of the trunk into the canopy, some forked. */
    private void branches() {
        for (int b = 0; b < 9; b++) {
            boolean glider = b == 0;
            double a = Math.toRadians(glider ? GLIDER : GLIDER + b * 40 + KandarinKit.hash(b, 21) % 15 - 7);
            int ys = glider ? W[LOOKOUT] - 5 : W[LOOKOUT] + 5 + (b * 4) % 9;
            int len = glider ? 24 : 16 + KandarinKit.hash(b, 22) % 7;
            double rise = glider ? 1.5 : 6 + KandarinKit.hash(b, 23) % 4;
            double thick = glider ? 1.6 : 0.9;
            limb(a, ys, radius(ys, a) - 1.5, len, rise, thick, b % 2 == 0 ? 0.12 : -0.12, true);
            if (!glider && b % 2 == 0) {
                // a fork from the middle of the limb
                double u = 0.55;
                double d0 = radius(ys, a) - 1.5 + len * u;
                double y0 = ys + rise * Math.sin(u * Math.PI / 2);
                limb(a + Math.toRadians(b % 4 == 0 ? 38 : -38), (int) Math.round(y0), d0, (int) (len * 0.5), 4, 0.7, 0, false);
            }
        }
    }

    private void limb(double a, int ys, double r0, int len, double rise, double thick, double bend, boolean main) {
        double px = 0;
        double py = 0;
        double pz = 0;
        for (double t = 0; t <= len; t += 0.5) {
            double u = t / len;
            double th = a + bend * Math.sin(u * Math.PI * 1.3);
            double d = r0 + t;
            px = gx + d * Math.cos(th);
            pz = gz + d * Math.sin(th);
            py = ys + rise * Math.sin(u * Math.PI / 2);
            double w = (main ? 2.2 : 1.4) * (1 - u) + thick;
            for (int x = (int) Math.floor(px - w); x <= (int) Math.ceil(px + w); x++) {
                for (int z = (int) Math.floor(pz - w); z <= (int) Math.ceil(pz + w); z++) {
                    for (int y = (int) Math.floor(py - w); y <= (int) Math.ceil(py + w); y++) {
                        if (sq(x - px) + sq(z - pz) + sq(y - py) > w * w || dist(x, z) < ATRIUM + 2.5) {
                            continue;
                        }
                        if (wood.get(x, y, z) == null) {
                            String bl = KandarinKit.hash(x * 5 + y, z * 3) % 7 == 0 ? "minecraft:dark_oak_wood" : "minecraft:spruce_wood";
                            wood.put(x, y, z, bl);
                        }
                    }
                }
            }
            if (Math.abs(u - 0.5) < 0.02) {
                branchMids.add(new double[]{px, py, pz});
            }
        }
        tips.add(new double[]{px, py, pz, main ? 1 : 0});
    }

    /** Cuts the entrance, the windows and the balcony doors out of the trunk model. */
    private void carve() {
        // the grand south entrance: five wide with a rounded head
        opening(90, 0, 3, 2.6);
        opening(90, 4, 4, 1.6);
        opening(90, 5, 5, 0.6);
        for (int lv = 0; lv < W.length; lv++) {
            for (int j = 0; j < 6; j++) {
                int deg = 30 + 60 * j + (lv % 2) * 30;
                if (lv == 0 && Math.abs(deg - 90) < 40) {
                    continue;
                }
                opening(deg, W[lv] + 1, W[lv] + 3, 0.6);
            }
        }
        for (int[] bal : BALCONIES) {
            for (int deg : doorAngles(bal)) {
                opening(deg, W[bal[0]], W[bal[0]] + 2, 1.1);
            }
        }
        // limbs never cross the head room of the top balcony or the glider walkway
        for (Map.Entry<Long, TreeMap<Integer, String>> e : wood.cols.entrySet()) {
            int x = (int) (e.getKey() >> 32);
            int z = (int) e.getKey().longValue();
            e.getValue().keySet().removeIf(y -> y >= W[LOOKOUT] && keepClear(x, y, z));
        }
    }

    private static int[] doorAngles(int[] bal) {
        if (bal[0] == LOOKOUT) {
            return new int[]{GLIDER, 75, 165, -105};
        }
        return new int[]{bal[1] + 25, bal[2] - 25};
    }

    /** Removes the trunk shell along a radial line at {@code deg} between heights y1..y2. */
    private void opening(double deg, int y1, int y2, double halfW) {
        double th = Math.toRadians(deg);
        double c = Math.cos(th);
        double s = Math.sin(th);
        for (int y = y1; y <= y2; y++) {
            for (double d = 6; d <= 26; d += 0.4) {
                for (double w = -halfW; w <= halfW; w += 0.4) {
                    int x = (int) Math.round(gx + d * c - w * s);
                    int z = (int) Math.round(gz + d * s + w * c);
                    if (dist(x, z) <= radius(y, angle(x, z)) + 0.6) {
                        wood.remove(x, y, z);
                    }
                }
            }
        }
    }

    /** Places a fitting only where it stands inside the hollow of the trunk (never in its wall). */
    private KandarinGrandTree place(int x, int y, int z, String b) {
        if (dist(x, z) <= inner(y, angle(x, z)) - 0.05) {
            k.set(x, y, z, b);
        }
        return this;
    }

    private KandarinGrandTree set(int x, int y, int z, String b) {
        return place(x, y, z, b);
    }

    private boolean inside(int x, int y, int z) {
        return dist(x, z) <= inner(y, angle(x, z)) - 0.05;
    }

    private void table(int x, int y, int z, String wood) {
        if (inside(x - 1, y, z) && inside(x + 1, y, z)) {
            k.table(x, y, z, wood);
        }
    }

    private void bed(int x, int y, int z, String colour) {
        if (inside(x, y, z) && inside(x, y, z + 1)) {
            k.bed(x, y, z, colour);
        }
    }

    private KandarinGrandTree pfill(int x1, int y1, int z1, int x2, int y2, int z2, String b) {
        boolean all = true;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                for (int y = y1; y <= y2; y++) {
                    all &= dist(x, z) <= inner(y, angle(x, z)) - 0.05;
                }
            }
        }
        if (all) {
            k.fill(x1, y1, z1, x2, y2, z2, b);
            return this;
        }
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                for (int y = y1; y <= y2; y++) {
                    place(x, y, z, b);
                }
            }
        }
        return this;
    }

    private KandarinGrandTree fill(int x1, int y1, int z1, int x2, int y2, int z2, String b) {
        return pfill(x1, y1, z1, x2, y2, z2, b);
    }

    // ------------------------------------------------------------------ inside

    private void groundFloor() {
        // the atrium: a mossy floor round a flowering azalea, lit from below
        k.disc(gx, gz, -1, -1, 7, "minecraft:moss_block");
        k.ring(gx, gz, -1, -1, 8, "minecraft:rooted_dirt");
        k.ring(gx, gz, -1, -1, 9, INNER);
        for (int[] p : new int[][]{{3, 3}, {-3, 3}, {3, -3}, {-3, -3}, {6, 0}, {-6, 0}, {0, 6}}) {
            place(gx + p[0], -1, gz + p[1], "minecraft:shroomlight");
        }
        place(gx, 0, gz, "minecraft:flowering_azalea");
        String[] flowers = {"minecraft:moss_carpet", "minecraft:cornflower", "minecraft:moss_carpet", "minecraft:lily_of_the_valley",
                "minecraft:moss_carpet", "minecraft:blue_orchid"};
        for (int i = 0; i < 26; i++) {
            int x = gx - 7 + KandarinKit.hash(i, 41) % 15;
            int z = gz - 7 + KandarinKit.hash(i, 42) % 15;
            if (dist(x, z) <= 7 && (x != gx || z != gz)) {
                place(x, 0, z, flowers[i % flowers.length]);
            }
        }
        // King Narnode's throne on a dais against the north wall
        pfill(gx - 4, 0, gz - 17, gx + 4, 0, gz - 14, PLANK);
        pfill(gx - 4, 0, gz - 13, gx + 4, 0, gz - 13, "minecraft:jungle_stairs[facing=north]");
        pfill(gx - 3, 1, gz - 16, gx + 3, 1, gz - 14, "minecraft:lime_carpet");
        place(gx, 1, gz - 16, "minecraft:jungle_stairs[facing=north]");
        place(gx - 1, 1, gz - 16, "minecraft:jungle_trapdoor[facing=west,open=true]");
        place(gx + 1, 1, gz - 16, "minecraft:jungle_trapdoor[facing=east,open=true]");
        pfill(gx, 1, gz - 17, gx, 2, gz - 17, "minecraft:gold_block");
        place(gx, 3, gz - 17, "minecraft:lime_banner[rotation=0]");
        place(gx - 3, 1, gz - 17, "minecraft:yellow_banner[rotation=0]").set(gx + 3, 1, gz - 17, "minecraft:yellow_banner[rotation=0]");
        place(gx - 4, 1, gz - 14, "minecraft:lantern").set(gx + 4, 1, gz - 14, "minecraft:lantern");
        place(gx - 2, 1, gz - 17, "minecraft:potted_flowering_azalea_bush").set(gx + 2, 1, gz - 17, "minecraft:potted_flowering_azalea_bush");
        // Charlie's cell (west) behind iron bars
        int jx = gx - 15;
        k.clear(jx - 1, 0, gz - 2, jx + 1, 2, gz + 2);
        for (int pass = 0; pass < 2; pass++) {
            pfill(jx + 2, 0, gz - 3, jx + 2, 2, gz + 3, "minecraft:iron_bars");
            pfill(jx - 1, 0, gz - 3, jx + 1, 2, gz - 3, "minecraft:iron_bars");
            pfill(jx - 1, 0, gz + 3, jx + 1, 2, gz + 3, "minecraft:iron_bars");
        }
        pfill(jx - 1, 3, gz - 3, jx + 2, 3, gz + 3, "minecraft:jungle_slab");
        place(jx, 0, gz - 2, "minecraft:hay_block").set(jx - 1, 0, gz + 2, "minecraft:cauldron");
        place(jx + 2, 0, gz, "minecraft:jungle_door[facing=east,half=lower,hinge=left]");
        // stores and the gnome guards' post (east), lanterns round the hall
        place(gx + 14, 0, gz + 1, "minecraft:barrel[facing=up]").set(gx + 15, 0, gz + 1, "minecraft:barrel[facing=up]");
        place(gx + 14, 1, gz + 1, "minecraft:hay_block").set(gx + 14, 0, gz + 2, "minecraft:chest[facing=west]");
        place(gx + 13, 0, gz - 8, "minecraft:barrel[facing=up]").set(gx + 14, 0, gz - 8, "minecraft:crafting_table");
        place(gx + 13, 0, gz - 9, "minecraft:fletching_table");
        for (int j = 0; j < 8; j++) {
            double th = Math.toRadians(22.5 + 45 * j);
            int x = (int) Math.round(gx + 16 * Math.cos(th));
            int z = (int) Math.round(gz + 16 * Math.sin(th));
            if (Math.abs(Math.toDegrees(th) - 90) > 30) {
                pfill(x, 0, z, x, 1, z, FENCE);
                place(x, 2, z, "minecraft:lantern");
            }
        }
    }

    /** Ring galleries round the atrium: plank floors with a stripped-wood edge beam and a rail. */
    private void galleries() {
        for (int lv = 1; lv < W.length; lv++) {
            int y = W[lv] - 1;
            Rows floor = new Rows();
            Rows rail = new Rows();
            for (int dx = -16; dx <= 16; dx++) {
                for (int dz = -16; dz <= 16; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d <= ATRIUM || d > inner(y, Math.atan2(dz, dx)) + 0.5) {
                        continue;
                    }
                    floor.put(gx + dx, gz + dz, d <= ATRIUM + 1 ? INNER : PLANK);
                    if (d <= ATRIUM + 1) {
                        rail.put(gx + dx, gz + dz, FENCE);
                    }
                }
            }
            floor.emit(k, y, false);
            rail.emit(k, y + 1, false);
            rail.emit(k, y + 1, false);
            // lanterns under the gallery (never over the atrium or a stair)
            for (int j = 0; j < 8; j++) {
                double th = Math.toRadians(22.5 + 45 * j);
                k.set((int) Math.round(gx + 11.3 * Math.cos(th)), y - 1, (int) Math.round(gz + 11.3 * Math.sin(th)), LANTERN_HANG);
            }
        }
    }

    /** Side of the trunk (0 east, 1 south, 2 west, 3 north) holding the flight from level lv. */
    private static int side(int lv) {
        return lv % 4;
    }

    /** Straight flights of stairs up the inside of the trunk, one per level, round the compass. */
    private void stairs() {
        for (int lv = 0; lv + 1 < W.length; lv++) {
            int n = W[lv + 1] - W[lv];
            int s = side(lv);
            String facing = new String[]{"south", "west", "north", "east"}[s];
            String back = new String[]{"north", "east", "south", "west"}[s];
            for (int i = 0; i < n; i++) {
                int along = -n / 2 + i;
                int[] p = stairCell(s, along);
                int y = W[lv] + i;
                k.clear(p[0], y + 1, p[1], p[0], y + 3, p[1]);
                k.set(p[0], y, p[1], "minecraft:jungle_stairs[facing=" + facing + "]");
                if (i > 0) {
                    k.set(p[0], y - 1, p[1], "minecraft:jungle_stairs[facing=" + back + ",half=top]");
                }
            }
        }
    }

    /** The stair cell {@code along} blocks along the flight on side s (10 blocks from the centre). */
    private int[] stairCell(int s, int along) {
        return switch (s) {
            case 0 -> new int[]{gx + 10, gz + along};
            case 1 -> new int[]{gx - along, gz + 10};
            case 2 -> new int[]{gx - 10, gz - along};
            default -> new int[]{gx + along, gz - 10};
        };
    }

    private void furnish() {
        // ---- first gallery: the bank (west) and the general store (north)
        int y = W[1];
        for (int dz = -2; dz <= 2; dz += 2) {
            place(gx - 12, y, gz + dz, KandarinKit.BANK);
        }
        place(gx - 12, y, gz - 1, "minecraft:jungle_slab[type=top]").set(gx - 12, y, gz + 1, "minecraft:jungle_slab[type=top]");
        pfill(gx - 12, y, gz - 4, gx - 12, y, gz - 3, PLANK).fill(gx - 12, y, gz + 3, gx - 12, y, gz + 4, PLANK);
        place(gx - 12, y + 1, gz - 4, "minecraft:lantern").set(gx - 12, y + 1, gz + 4, "minecraft:lantern");
        place(gx - 13, y, gz - 2, "minecraft:chest[facing=east]").set(gx - 13, y, gz + 2, "minecraft:chest[facing=east]");
        place(gx - 13, y, gz, "minecraft:gold_block");
        place(gx - 10, y, gz - 3, "minecraft:lime_carpet").set(gx - 10, y, gz + 3, "minecraft:lime_carpet");
        pfill(gx - 2, y, gz - 12, gx + 2, y, gz - 12, PLANK);
        place(gx, y, gz - 12, KandarinKit.GENERAL);
        pfill(gx - 2, y + 1, gz - 12, gx - 1, y + 1, gz - 12, "minecraft:jungle_slab");
        pfill(gx + 1, y + 1, gz - 12, gx + 2, y + 1, gz - 12, "minecraft:jungle_slab");
        place(gx - 1, y, gz - 13, "minecraft:barrel[facing=up]").set(gx + 1, y, gz - 13, "minecraft:barrel[facing=up]");
        place(gx, y, gz - 13, "minecraft:chest[facing=south]");
        place(gx + 8, y, gz + 9, "minecraft:barrel[facing=up]").set(gx + 9, y, gz + 8, "minecraft:composter");
        // ---- second gallery: the kitchen (north) and the dining tables (west)
        y = W[2];
        place(gx, y, gz - 12, KandarinKit.RANGE);
        place(gx + 1, y, gz - 12, "minecraft:smoker[facing=south]");
        place(gx - 1, y, gz - 12, "minecraft:barrel[facing=up]").set(gx - 2, y, gz - 12, "minecraft:cauldron");
        place(gx + 2, y, gz - 12, "minecraft:crafting_table").set(gx + 3, y, gz - 11, "minecraft:barrel[facing=up]");
        place(gx - 3, y, gz - 11, "minecraft:jungle_slab[type=top]").set(gx - 3, y + 1, gz - 11, "minecraft:cake");
        table(gx + 11, y, gz - 4, "jungle");
        table(gx + 11, y, gz + 3, "jungle");
        place(gx - 8, y, gz + 8, "minecraft:jungle_fence").set(gx - 8, y + 1, gz + 8, "minecraft:jungle_pressure_plate");
        // ---- third gallery: the gnomes' sleeping quarters
        y = W[3];
        bed(gx + 11, y, gz - 5, "lime");
        bed(gx + 11, y, gz - 1, "lime");
        bed(gx + 11, y, gz + 3, "lime");
        bed(gx - 3, y, gz - 12, "yellow");
        bed(gx + 1, y, gz - 12, "yellow");
        place(gx + 11, y, gz - 2, "minecraft:chest[facing=west]").set(gx + 11, y, gz + 2, "minecraft:chest[facing=west]");
        place(gx - 1, y, gz - 12, "minecraft:barrel[facing=up]");
        // ---- fourth gallery: the library and council table
        y = W[4];
        pfill(gx - 3, y, gz + 11, gx + 3, y + 1, gz + 11, "minecraft:bookshelf");
        pfill(gx - 11, y, gz - 3, gx - 11, y + 1, gz + 3, "minecraft:bookshelf");
        place(gx - 10, y, gz + 5, "minecraft:lectern[facing=east]");
        place(gx + 4, y, gz + 10, "minecraft:cartography_table");
        pfill(gx - 2, y, gz + 10, gx + 2, y, gz + 10, "minecraft:jungle_slab[type=top]");
        place(gx, y + 1, gz + 10, "minecraft:candle[candles=3,lit=true]");
        // ---- fifth gallery: the workshop
        y = W[5];
        place(gx - 11, y, gz - 2, "minecraft:crafting_table").set(gx - 11, y, gz - 1, "minecraft:fletching_table");
        place(gx - 11, y, gz + 1, "minecraft:loom[facing=east]").set(gx - 11, y, gz + 2, "minecraft:barrel[facing=up]");
        place(gx - 2, y, gz - 11, "minecraft:smithing_table").set(gx, y, gz - 11, "minecraft:barrel[facing=up]");
        place(gx + 2, y, gz - 11, "minecraft:white_wool").set(gx + 2, y + 1, gz - 11, "minecraft:white_wool");
        // ---- sixth gallery: King Narnode's private chamber
        y = W[6];
        bed(gx + 11, y, gz - 3, "purple");
        place(gx + 11, y, gz - 4, "minecraft:gold_block").set(gx + 11, y, gz - 1, "minecraft:chest[facing=west]");
        place(gx + 10, y, gz + 2, "minecraft:lime_banner[rotation=4]").set(gx + 10, y, gz + 5, "minecraft:yellow_banner[rotation=4]");
        pfill(gx - 2, y, gz - 11, gx + 2, y, gz - 11, "minecraft:bookshelf");
        place(gx, y, gz - 10, "minecraft:lectern[facing=south]");
        pfill(gx - 3, y, gz + 11, gx + 3, y, gz + 11, "minecraft:purple_carpet");
        // ---- the look-out at the top
        y = W[LOOKOUT];
        place(gx + 8, y, gz - 6, "minecraft:chest[facing=west]").set(gx + 8, y, gz + 6, "minecraft:cartography_table");
        place(gx - 5, y, gz - 9, "minecraft:lime_banner[rotation=8]").set(gx + 5, y, gz - 9, "minecraft:yellow_banner[rotation=8]");
        place(gx + 6, y, gz + 8, "minecraft:barrel[facing=up]");
    }

    /** Outer balconies hung off the trunk, with corbels below, a rail and the glider walkway. */
    private void balconies() {
        for (int[] bal : BALCONIES) {
            int y = W[bal[0]] - 1;
            Rows floor = new Rows();
            Rows rail = new Rows();
            Rows corbel = new Rows();
            for (int dx = -24; dx <= 24; dx++) {
                for (int dz = -24; dz <= 24; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    double th = Math.atan2(dz, dx);
                    double deg = Math.toDegrees(th);
                    if (!inArc(deg, bal[1], bal[2])) {
                        continue;
                    }
                    double r = radius(y, th);
                    if (d <= r + 0.5 || d > r + 4.5) {
                        continue;
                    }
                    int x = gx + dx;
                    int z = gz + dz;
                    if (wood.get(x, y, z) != null) {
                        continue;
                    }
                    floor.put(x, z, d > r + 3.5 ? INNER : PLANK);
                    if (d > r + 3.5) {
                        rail.put(x, z, FENCE);
                    }
                    if (d <= r + 1.5) {
                        corbel.put(x, z, "minecraft:jungle_stairs[facing=" + outward(dx, dz) + ",half=top]");
                    }
                }
            }
            floor.emit(k, y, false);
            corbel.emit(k, y - 1, true);
            if (bal[0] == LOOKOUT) {
                // leave the rail open where the glider walkway leaves
                double th = Math.toRadians(GLIDER);
                for (double w = -1.5; w <= 1.5; w += 0.5) {
                    for (double d = 14; d <= 22; d += 0.5) {
                        rail.remove((int) Math.round(gx + d * Math.cos(th) - w * Math.sin(th)),
                                (int) Math.round(gz + d * Math.sin(th) + w * Math.cos(th)));
                    }
                }
            }
            rail.emit(k, y + 1, false);
            rail.emit(k, y + 1, false);
            // lanterns on posts round the rail
            for (int deg = bal[1] + 10; deg < bal[2]; deg += 40) {
                double th = Math.toRadians(deg);
                double r = radius(y, th) + 4;
                int x = (int) Math.round(gx + r * Math.cos(th));
                int z = (int) Math.round(gz + r * Math.sin(th));
                if (Math.abs(deg - GLIDER) > 12) {
                    k.set(x, y + 2, z, "minecraft:lantern");
                }
            }
        }
        // the walkway out along the glider limb to Captain Errdo's platform
        double th = Math.toRadians(GLIDER);
        int y = W[LOOKOUT] - 1;
        Rows deck = new Rows();
        Rows rail = new Rows();
        double r0 = radius(y, th) + 3;
        for (double d = r0; d <= 30; d += 0.4) {
            for (double w = -2.4; w <= 2.4; w += 0.4) {
                int x = (int) Math.round(gx + d * Math.cos(th) - w * Math.sin(th));
                int z = (int) Math.round(gz + d * Math.sin(th) + w * Math.cos(th));
                if (Math.abs(w) <= 1.3) {
                    deck.put(x, z, PLANK);
                    rail.remove(x, z);
                } else if (deck.get(x, z) == null) {
                    rail.put(x, z, FENCE);
                }
            }
        }
        int px = (int) Math.round(gx + 32 * Math.cos(th));
        int pz = (int) Math.round(gz + 32 * Math.sin(th));
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d <= 3.6) {
                    deck.put(px + dx, pz + dz, d > 2.6 ? INNER : PLANK);
                    rail.remove(px + dx, pz + dz);
                } else if (d <= 4.5 && deck.get(px + dx, pz + dz) == null) {
                    rail.put(px + dx, pz + dz, FENCE);
                }
            }
        }
        // the platform rail opens towards the walkway
        for (int[] cell : List.of(new int[]{-3, 0}, new int[]{-4, 0}, new int[]{-4, 1}, new int[]{-3, 1}, new int[]{-4, -1}, new int[]{-3, 2}, new int[]{-4, 2})) {
            rail.remove(px + cell[0], pz + cell[1]);
        }
        deck.emit(k, y, false);
        rail.emit(k, y + 1, false);
        rail.emit(k, y + 1, false);
        // a support post down to the limb below, lanterns and the glider itself
        k.fill(px, y - 4, pz, px, y - 1, pz, "minecraft:jungle_log");
        k.set(px - 3, y + 1, pz - 2, "minecraft:jungle_fence").set(px - 3, y + 2, pz - 2, "minecraft:lantern");
        k.set(px - 3, y + 1, pz + 3, "minecraft:jungle_fence").set(px - 3, y + 2, pz + 3, "minecraft:lantern");
        k.set(px, y + 1, pz, "minecraft:jungle_fence");
        k.fill(px, y + 2, pz - 3, px, y + 2, pz + 3, "minecraft:white_wool");
        k.set(px, y + 2, pz - 3, "minecraft:red_wool").set(px, y + 2, pz + 3, "minecraft:red_wool");
        k.fill(px + 1, y + 2, pz, px + 2, y + 2, pz, "minecraft:white_wool");
        k.set(px + 2, y + 3, pz, "minecraft:white_wool");
        k.set(px - 1, y + 2, pz, "minecraft:jungle_trapdoor[facing=east,half=bottom]");
    }

    private static boolean inArc(double deg, double a, double b) {
        double t = deg;
        while (t < a) {
            t += 360;
        }
        while (t >= a + 360) {
            t -= 360;
        }
        return t <= b;
    }

    private static String outward(int dx, int dz) {
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx > 0 ? "west" : "east";
        }
        return dz > 0 ? "north" : "south";
    }

    // ------------------------------------------------------------------ canopy

    /** Is this cell kept free of leaves (balconies, walkway and the glider platform)? */
    private boolean keepClear(int x, int y, int z) {
        int top = W[LOOKOUT];
        if (y >= top - 1 && y <= top + 4) {
            double d = dist(x, z);
            double r = radius(y, angle(x, z));
            if (d <= r + 0.3) {
                return false;
            }
            if (d <= r + 5.5) {
                return true;
            }
            double g = Math.toRadians(GLIDER);
            double along = (x - gx) * Math.cos(g) + (z - gz) * Math.sin(g);
            double across = -(x - gx) * Math.sin(g) + (z - gz) * Math.cos(g);
            return along > 0 && along < 37 && Math.abs(across) < 5.5;
        }
        return false;
    }

    /** Leaf domes: [cx, cy, cz, rx, ry, rz] relative to the trunk centre. */
    private List<double[]> domes() {
        List<double[]> list = new ArrayList<>();
        list.add(new double[]{0, 86, 0, 28, 6, 28});
        list.add(new double[]{2, 95, -1, 20, 5, 20});
        list.add(new double[]{-1, 102, 1, 12, 4, 12});
        list.add(new double[]{-14, 85, 12, 10, 5, 10});
        list.add(new double[]{-8, 83, -16, 9, 4, 9});
        list.add(new double[]{13, 87, 10, 9, 5, 9});
        for (double[] t : tips) {
            double r = t[3] > 0 ? 8 : 6;
            list.add(new double[]{t[0] - gx, t[1] + 3, t[2] - gz, r, 4, r});
        }
        return list;
    }

    private void canopy() {
        List<double[]> domes = domes();
        String[] kinds = {"minecraft:oak_leaves[persistent=true]", "minecraft:oak_leaves[persistent=true]", "minecraft:jungle_leaves[persistent=true]",
                "minecraft:oak_leaves[persistent=true]", "minecraft:azalea_leaves[persistent=true]", "minecraft:jungle_leaves[persistent=true]",
                "minecraft:flowering_azalea_leaves[persistent=true]", "minecraft:oak_leaves[persistent=true]"};
        String under = "minecraft:dark_oak_leaves[persistent=true]";
        for (int dx = -34; dx <= 40; dx++) {
            for (int dz = -36; dz <= 32; dz++) {
                int x = gx + dx;
                int z = gz + dz;
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > 35) {
                    continue;
                }
                int n1 = KandarinKit.hash(x, z * 7) % 3;
                int n2 = KandarinKit.hash(x * 7, z) % 3;
                // union of the vertical spans of every dome over this column
                boolean[] col = new boolean[200];
                boolean any = false;
                for (double[] e : domes) {
                    double fx = (dx - e[0]) / (e[3] + 0.5);
                    double fz = (dz - e[2]) / (e[5] + 0.5);
                    double v = 1 - fx * fx - fz * fz;
                    if (v <= 0) {
                        continue;
                    }
                    double half = e[4] * Math.sqrt(v);
                    int lo = (int) Math.round(e[1] - half * 0.65) - n2;
                    int hi = (int) Math.round(e[1] + half) + (half > 1.5 ? n1 - 1 : 0);
                    for (int y = Math.max(0, lo); y <= Math.min(199, hi); y++) {
                        col[y] = true;
                        any = true;
                    }
                }
                if (!any) {
                    continue;
                }
                String kind = kinds[KandarinKit.hash(Math.floorDiv(x, 6) + 17, Math.floorDiv(z, 6) - 5) % kinds.length];
                for (int y = 0; y < 200; y++) {
                    if (!col[y] || keepClear(x, y, z) || wood.get(x, y, z) != null) {
                        continue;
                    }
                    boolean bottom = y == 0 || !col[y - 1];
                    boolean low = bottom || y > 0 && (y < 2 || !col[y - 2]);
                    String b = low ? under : kind;
                    if (bottom && d > 12 && KandarinKit.hash(x * 11, z * 13 + y) % 29 == 0) {
                        b = "minecraft:shroomlight";
                    }
                    leaves.put(x, y, z, b);
                }
            }
        }
    }

    /** Glow-berry vines and glow lichen under the canopy, lanterns under the limbs. */
    private void hangings() {
        for (Map.Entry<Long, TreeMap<Integer, String>> e : leaves.cols.entrySet()) {
            int x = (int) (e.getKey() >> 32);
            int z = (int) e.getKey().longValue();
            double d = dist(x, z);
            if (d <= ATRIUM + 1.5) {
                continue;
            }
            for (Map.Entry<Integer, String> c : e.getValue().entrySet()) {
                int y = c.getKey();
                if (leaves.get(x, y - 1, z) != null || !c.getValue().contains("leaves")) {
                    continue;
                }
                int h = KandarinKit.hash(x * 31 + y, z * 17);
                if (h % 19 == 0) {
                    int len = 2 + h % 5;
                    int bottom = y - 1 - len;
                    if (bottom < W[LOOKOUT] + 5 || !free(x, bottom, y - 1, z)) {
                        continue;
                    }
                    k.fill(x, bottom + 1, z, x, y - 1, z, "minecraft:cave_vines_plant[berries=true]");
                    k.set(x, bottom, z, "minecraft:cave_vines[berries=true]");
                } else if (h % 13 == 0 && y - 1 > W[LOOKOUT] + 4 && free(x, y - 1, y - 1, z)) {
                    k.set(x, y - 1, z, "minecraft:glow_lichen[up=true]");
                }
            }
        }
        for (double[] m : branchMids) {
            int x = (int) Math.round(m[0]);
            int z = (int) Math.round(m[2]);
            int y = (int) Math.round(m[1]);
            while (wood.get(x, y - 1, z) != null) {
                y--;
            }
            if (wood.get(x, y, z) != null && !keepClear(x, y - 1, z)) {
                k.set(x, y - 1, z, LANTERN_HANG);
            }
        }
    }

    private boolean free(int x, int y1, int y2, int z) {
        for (int y = y1; y <= y2; y++) {
            if (wood.get(x, y, z) != null || leaves.get(x, y, z) != null || keepClear(x, y, z)) {
                return false;
            }
        }
        return true;
    }

    /** Strands of vine climbing the outside of the trunk. */
    private void trunkVines() {
        String[] faces = {"east", "west", "south", "north"};
        int[][] off = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int j = 0; j < 48; j++) {
            if (KandarinKit.hash(j, 61) % 3 != 0) {
                continue;
            }
            double th = Math.toRadians(j * 7.5 + 3);
            int y0 = 6 + KandarinKit.hash(j, 62) % 46;
            int len = 5 + KandarinKit.hash(j, 63) % 10;
            for (int y = y0; y < y0 + len && y < TOP; y++) {
                if (nearBalcony(y)) {
                    continue;
                }
                double r = radius(y, th) + 0.9;
                int x = (int) Math.round(gx + r * Math.cos(th));
                int z = (int) Math.round(gz + r * Math.sin(th));
                if (wood.get(x, y, z) != null) {
                    continue;
                }
                for (int f = 0; f < 4; f++) {
                    String nb = wood.get(x + off[f][0], y, z + off[f][1]);
                    if (nb != null && nb.endsWith("_wood")) {
                        k.set(x, y, z, "minecraft:vine[" + faces[f] + "=true]");
                        break;
                    }
                }
            }
        }
    }

    private static boolean nearBalcony(int y) {
        for (int[] b : BALCONIES) {
            if (y >= W[b[0]] - 2 && y <= W[b[0]] + 3) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ voxel helpers

    /** A sparse block model stored as columns, written out as vertical runs. */
    private static final class Vox {
        final TreeMap<Long, TreeMap<Integer, String>> cols = new TreeMap<>();

        static long xz(int x, int z) {
            return ((long) x << 32) ^ (z & 0xffffffffL);
        }

        void put(int x, int y, int z, String b) {
            cols.computeIfAbsent(xz(x, z), q -> new TreeMap<>()).put(y, b);
        }

        String get(int x, int y, int z) {
            TreeMap<Integer, String> c = cols.get(xz(x, z));
            return c == null ? null : c.get(y);
        }

        void remove(int x, int y, int z) {
            TreeMap<Integer, String> c = cols.get(xz(x, z));
            if (c != null) {
                c.remove(y);
            }
        }

        void emit(KandarinKit k, boolean onlyAir) {
            for (Map.Entry<Long, TreeMap<Integer, String>> e : cols.entrySet()) {
                int x = (int) (e.getKey() >> 32);
                int z = (int) e.getKey().longValue();
                String cur = null;
                int start = 0;
                int prev = 0;
                for (Map.Entry<Integer, String> c : e.getValue().entrySet()) {
                    int y = c.getKey();
                    if (cur != null && c.getValue().equals(cur) && y == prev + 1) {
                        prev = y;
                        continue;
                    }
                    if (cur != null) {
                        out(k, onlyAir, x, start, prev, z, cur);
                    }
                    cur = c.getValue();
                    start = y;
                    prev = y;
                }
                if (cur != null) {
                    out(k, onlyAir, x, start, prev, z, cur);
                }
            }
        }

        private static void out(KandarinKit k, boolean onlyAir, int x, int y1, int y2, int z, String b) {
            if (onlyAir) {
                k.fillAir(x, y1, z, x, y2, z, b);
            } else {
                k.fill(x, y1, z, x, y2, z, b);
            }
        }
    }

    /** One horizontal layer of cells, written out as runs along x. */
    private static final class Rows {
        final TreeMap<Long, String> cells = new TreeMap<>();

        static long key(int x, int z) {
            return ((long) z << 32) ^ (x & 0xffffffffL);
        }

        void put(int x, int z, String b) {
            cells.put(key(x, z), b);
        }

        String get(int x, int z) {
            return cells.get(key(x, z));
        }

        void remove(int x, int z) {
            cells.remove(key(x, z));
        }

        void emit(KandarinKit k, int y, boolean onlyAir) {
            Integer rz = null;
            int rx1 = 0;
            int rx2 = 0;
            String rb = null;
            for (Map.Entry<Long, String> e : cells.entrySet()) {
                int z = (int) (e.getKey() >> 32);
                int x = (int) e.getKey().longValue();
                if (rb != null && rz == z && x == rx2 + 1 && rb.equals(e.getValue())) {
                    rx2 = x;
                    continue;
                }
                if (rb != null) {
                    out(k, onlyAir, rx1, rx2, y, rz, rb);
                }
                rz = z;
                rx1 = x;
                rx2 = x;
                rb = e.getValue();
            }
            if (rb != null) {
                out(k, onlyAir, rx1, rx2, y, rz, rb);
            }
        }

        private static void out(KandarinKit k, boolean onlyAir, int x1, int x2, int y, int z, String b) {
            if (onlyAir) {
                k.fillAir(x1, y, z, x2, y, z, b);
            } else {
                k.fill(x1, y, z, x2, y, z, b);
            }
        }
    }
}
