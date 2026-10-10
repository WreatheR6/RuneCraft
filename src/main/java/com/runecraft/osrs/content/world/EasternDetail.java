package com.runecraft.osrs.content.world;

import com.runecraft.osrs.world.StructureDef;

import java.util.Random;

/**
 * Detail kit shared by the eastern regions (Kharidian Desert, Morytania, the Wilderness), Great Kourend and
 * Varlamore, and the islands: dresses a plain {@link StructureDef.Builder#tBuilding} shell with plinths,
 * storey bands, timber framing, tall windows with shutters and sills, door hoods and lamps, gables, eave
 * cornices, ridge caps, chimneys with smoke and crooked or patched roofs; furnishes rooms; and adds street
 * furniture, trees, rubble, domes, awnings and weathering. Block coordinates; heights absolute unless noted.
 *
 * <p>Everything here stays inside (or one block outside) the rectangle it is given, so footprints don't grow.
 */
public final class EasternDetail {
    /**
     * How a building's shell is dressed. {@code wall} is the shell material the dressing replaces;
     * {@code null} fields are skipped. Log-like blocks ({@code _log}, {@code _wood}, {@code _stem}) get an
     * axis along the wall automatically.
     */
    public record Look(String wall, String plinth, String band, String post, boolean timber, String sill,
                       String shutter, String gable, String chimney, String lamp, String lintel, boolean tallWindows,
                       String roofAlt, boolean crooked) {
        public Look withWall(String w) {
            return new Look(w, plinth, band, post, timber, sill, shutter, gable, chimney, lamp, lintel, tallWindows,
                    roofAlt, crooked);
        }

        public Look withChimney(String c) {
            return new Look(wall, plinth, band, post, timber, sill, shutter, gable, c, lamp, lintel, tallWindows,
                    roofAlt, crooked);
        }
    }

    /** What a furnished room is for (decides the furniture). */
    public enum Room { HOUSE, BEDROOM, SHOP, PUB, BANK, TEMPLE, STORE, GUARD, LIBRARY, KITCHEN, CRYPT }

    /** Furniture palette: wood for tables/chairs/shelves, rug colours, light, and a gloomy flag (cobwebs). */
    public record Furniture(String wood, String rug, String rugBorder, String bed, String light, boolean gloomy) {
    }

    public static final Furniture OAK_FURN = new Furniture("minecraft:oak", "minecraft:red_carpet", "minecraft:brown_carpet",
            "minecraft:red_bed", "minecraft:lantern", false);

    final StructureDef.Builder b;
    final int g;

    public EasternDetail(StructureDef.Builder b) {
        this.b = b;
        this.g = b.ground();
    }

    // ------------------------------------------------------------------ small helpers

    static String axis(String block, boolean alongX) {
        if (block == null || block.contains("[")) {
            return block;
        }
        if (block.endsWith("_log") || block.endsWith("_wood") || block.endsWith("_stem") || block.endsWith("basalt")
                || block.endsWith("_block") && (block.contains("bamboo") || block.contains("hay"))) {
            return block + "[axis=" + (alongX ? "x" : "z") + "]";
        }
        return block;
    }

    /** A log-like block standing upright. */
    static String vert(String block) {
        if (block == null || block.contains("[")) {
            return block;
        }
        return block.endsWith("_log") || block.endsWith("_wood") || block.endsWith("_stem") || block.endsWith("basalt")
                ? block + "[axis=y]" : block;
    }

    static String id(String block) {
        int i = block.indexOf('[');
        return i < 0 ? block : block.substring(0, i);
    }

    static String opposite(String side) {
        return switch (side) {
            case "north" -> "south";
            case "south" -> "north";
            case "east" -> "west";
            default -> "east";
        };
    }

    /** Stairs block id for a roof material base ("minecraft:spruce" -> spruce_stairs). */
    public static String stairsOf(String material) {
        return material + "_stairs";
    }

    /** Full block for a roof material base, as {@code Builder.roof} uses. */
    public static String fullOf(String material) {
        return material.endsWith("brick") || material.endsWith("tile") ? material + "s" : material + "_planks";
    }

    public static String slabOf(String material) {
        return material + "_slab";
    }

    void set(int x, int y, int z, String s) {
        b.set(x, y, z, s);
    }

    void fill(int x1, int y1, int z1, int x2, int y2, int z2, String s) {
        b.fill(x1, y1, z1, x2, y2, z2, s);
    }

    void replace(int x1, int y1, int z1, int x2, int y2, int z2, String from, String to) {
        b.replace(x1, y1, z1, x2, y2, z2, id(from), to);
    }

    /** Replaces {@code from} with {@code to} on the four wall strips of a rectangle between y1 and y2. */
    void ringReplace(int[] q, int y1, int y2, String from, String to) {
        replace(q[0], y1, q[1], q[2], y2, q[1], from, axis(to, true));
        replace(q[0], y1, q[3], q[2], y2, q[3], from, axis(to, true));
        replace(q[0], y1, q[1] + 1, q[0], y2, q[3] - 1, from, axis(to, false));
        replace(q[2], y1, q[1] + 1, q[2], y2, q[3] - 1, from, axis(to, false));
    }

    // ------------------------------------------------------------------ building shells

    /**
     * Dresses the walls of a {@code tBuilding} shell over block rectangle q ({x1, z1, x2, z2}) of
     * {@code storeys} 4-block storeys: plinth, bands or timber frame, corner posts, windows with frames,
     * shutters and sills, and a hooded, lamp-lit doorway. Call right after the shell, before furnishing.
     */
    public void facade(int[] q, int storeys, Look l, String doorSide, String windowBlock) {
        int top = g + storeys * 4;
        int x1 = q[0];
        int z1 = q[1];
        int x2 = q[2];
        int z2 = q[3];
        if (l.plinth() != null) {
            ringReplace(q, g, g, l.wall(), l.plinth());
        }
        if (l.timber() && l.post() != null) {
            for (int x = x1 + 3; x < x2; x += 3) {
                replace(x, g + 1, z1, x, top - 2, z1, l.wall(), vert(l.post()));
                replace(x, g + 1, z2, x, top - 2, z2, l.wall(), vert(l.post()));
            }
            for (int z = z1 + 3; z < z2; z += 3) {
                replace(x1, g + 1, z, x1, top - 2, z, l.wall(), vert(l.post()));
                replace(x2, g + 1, z, x2, top - 2, z, l.wall(), vert(l.post()));
            }
        }
        if (l.band() != null) {
            for (int s = 1; s < storeys; s++) {
                ringReplace(q, g + s * 4 - 1, g + s * 4 - 1, l.wall(), l.band());
            }
            ringReplace(q, top - 1, top - 1, l.wall(), l.band());
        }
        if (l.post() != null) {
            String p = vert(l.post());
            fill(x1, g, z1, x1, top - 1, z1, p);
            fill(x2, g, z1, x2, top - 1, z1, p);
            fill(x1, g, z2, x1, top - 1, z2, p);
            fill(x2, g, z2, x2, top - 1, z2, p);
        }
        int mx = (x1 + x2) / 2;
        int mz = (z1 + z2) / 2;
        for (int s = 0; s < storeys; s++) {
            int wy = g + s * 4 + 1;
            for (int x = x1 + 2; x <= x2 - 2; x += 3) {
                boolean nearDoor = s == 0 && x >= mx - 2 && x <= mx + 3;
                if (!(nearDoor && "north".equals(doorSide))) {
                    window(x, wy, z1, "north", l, windowBlock);
                }
                if (!(nearDoor && "south".equals(doorSide))) {
                    window(x, wy, z2, "south", l, windowBlock);
                }
            }
            for (int z = z1 + 2; z <= z2 - 2; z += 3) {
                boolean nearDoor = s == 0 && z >= mz - 2 && z <= mz + 3;
                if (!(nearDoor && "west".equals(doorSide))) {
                    window(x1, wy, z, "west", l, windowBlock);
                }
                if (!(nearDoor && "east".equals(doorSide))) {
                    window(x2, wy, z, "east", l, windowBlock);
                }
            }
        }
        if (doorSide != null) {
            doorHood(q, doorSide, l);
        }
    }

    private static int nx(String side) {
        return side.equals("east") ? 1 : side.equals("west") ? -1 : 0;
    }

    private static int nz(String side) {
        return side.equals("south") ? 1 : side.equals("north") ? -1 : 0;
    }

    /** Frames one window (whose lower pane is at (x, wy, z) in the wall facing {@code side}). */
    private void window(int x, int wy, int z, String side, Look l, String pane) {
        int ox = nx(side);
        int oz = nz(side);
        int tx = oz != 0 ? 1 : 0;
        int tz = ox != 0 ? 1 : 0;
        boolean alongX = oz != 0;
        if (l.tallWindows()) {
            // The upper pane copies whatever the lower opening holds (pane, bars or air).
            b.op(StructureDef.Op.Type.REPLACE, x, wy + 1, z, x, wy + 1, z, pane == null ? "minecraft:glass_pane" : pane, id(l.wall()));
        }
        int h = l.tallWindows() ? 1 : 0;
        if (l.lintel() != null) {
            replace(x - tx, wy + h + 1, z - tz, x + tx, wy + h + 1, z + tz, l.wall(), axis(l.lintel(), alongX));
        }
        if (l.shutter() != null) {
            String sh = l.shutter() + "[facing=" + side + ",open=true,half=bottom]";
            fill(x - tx + ox, wy, z - tz + oz, x - tx + ox, wy + h, z - tz + oz, sh);
            fill(x + tx + ox, wy, z + tz + oz, x + tx + ox, wy + h, z + tz + oz, sh);
        }
        if (l.sill() != null) {
            set(x + ox, wy - 1, z + oz, l.sill() + "[facing=" + opposite(side) + ",half=top]");
        }
    }

    /** Door frame posts, a small stair hood over the door and a lantern beside it. */
    private void doorHood(int[] q, String side, Look l) {
        int mx = (q[0] + q[2]) / 2;
        int mz = (q[1] + q[3]) / 2;
        int ox = nx(side);
        int oz = nz(side);
        int wx = side.equals("east") ? q[2] : side.equals("west") ? q[0] : mx;
        int wz = side.equals("south") ? q[3] : side.equals("north") ? q[1] : mz;
        int tx = oz != 0 ? 1 : 0;
        int tz = ox != 0 ? 1 : 0;
        // posts either side of the 2-wide opening (in the wall plane)
        if (l.post() != null) {
            String p = vert(l.post());
            fill(wx - tx, g, wz - tz, wx - tx, g + 2, wz - tz, p);
            fill(wx + 2 * tx, g, wz + 2 * tz, wx + 2 * tx, g + 2, wz + 2 * tz, p);
        }
        if (l.sill() != null) {
            String hood = l.sill() + "[facing=" + opposite(side) + "]";
            fill(wx - tx + ox, g + 3, wz - tz + oz, wx + 2 * tx + ox, g + 3, wz + 2 * tz + oz, hood);
        }
        if (l.lamp() != null) {
            String lamp = l.lamp() + "[hanging=true]";
            if (l.sill() != null) {
                set(wx - tx + ox, g + 2, wz - tz + oz, lamp);
            } else {
                set(wx - tx + ox, g + 2, wz - tz + oz, "minecraft:chain");
                set(wx - tx + ox, g + 3, wz - tz + oz, axis("minecraft:spruce_fence", true));
                set(wx - tx + ox, g + 1, wz - tz + oz, lamp);
            }
        }
    }

    // ------------------------------------------------------------------ roofs

    /**
     * Details a pitched {@code Builder.roof} of {@code material} over q starting at {@code y} (= top of the
     * walls): gable infill with an attic window, a cornice under the eaves, a ridge cap, patches of an
     * alternative material (moss, repairs), an optionally crooked ridge and a chimney with smoke on the gable
     * away from {@code doorSide}.
     */
    public void roof(int[] q, int y, String material, Look l, String doorSide, long seed) {
        Random rnd = new Random(seed);
        int x1 = q[0];
        int z1 = q[1];
        int x2 = q[2];
        int z2 = q[3];
        boolean alongX = (x2 - x1) >= (z2 - z1);
        int depth = alongX ? z2 - z1 : x2 - x1;
        int layers = (depth + 2) / 2;
        String stairs = stairsOf(material);
        String full = fullOf(material);
        int ridgeY = y + layers - 1;
        // cornice under the overhang
        if (alongX) {
            fill(x1, y - 1, z1 - 1, x2, y - 1, z1 - 1, stairs + "[facing=south,half=top]");
            fill(x1, y - 1, z2 + 1, x2, y - 1, z2 + 1, stairs + "[facing=north,half=top]");
        } else {
            fill(x1 - 1, y - 1, z1, x1 - 1, y - 1, z2, stairs + "[facing=east,half=top]");
            fill(x2 + 1, y - 1, z1, x2 + 1, y - 1, z2, stairs + "[facing=west,half=top]");
        }
        // gable infill and attic windows
        if (l.gable() != null) {
            for (int gx : alongX ? new int[]{x1, x2} : new int[]{z1, z2}) {
                if (alongX) {
                    replace(gx, y, z1, gx, ridgeY, z2, full, axis(l.gable(), false));
                } else {
                    replace(x1, y, gx, x2, ridgeY, gx, full, axis(l.gable(), true));
                }
            }
        }
        if (layers >= 3) {
            int c = alongX ? (z1 + z2) / 2 : (x1 + x2) / 2;
            String pane = "minecraft:glass_pane";
            for (int gx : alongX ? new int[]{x1, x2} : new int[]{z1, z2}) {
                if (alongX) {
                    fill(gx, y + 1, c, gx, y + 1, c + ((z2 - z1) % 2 == 1 ? 1 : 0), pane);
                } else {
                    fill(c, y + 1, gx, c + ((x2 - x1) % 2 == 1 ? 1 : 0), y + 1, gx, pane);
                }
            }
        }
        // patches of another material along the slopes (same facing)
        if (l.roofAlt() != null) {
            String alt = stairsOf(l.roofAlt());
            for (int i = 0; i < layers - 1; i++) {
                if (rnd.nextInt(3) == 0) {
                    continue;
                }
                int yy = y + i;
                int len = alongX ? x2 - x1 + 2 : z2 - z1 + 2;
                for (int n = 0; n < Math.max(1, len / 14); n++) {
                    int a = rnd.nextInt(len);
                    int w = 1 + rnd.nextInt(3);
                    if (alongX) {
                        int xa = x1 - 1 + a;
                        int xb = Math.min(x2 + 1, xa + w);
                        fill(xa, yy, z1 - 1 + i, xb, yy, z1 - 1 + i, alt + "[facing=south]");
                        int xc = x1 - 1 + rnd.nextInt(len);
                        fill(xc, yy, z2 + 1 - i, Math.min(x2 + 1, xc + w), yy, z2 + 1 - i, alt + "[facing=north]");
                    } else {
                        int za = z1 - 1 + a;
                        int zb = Math.min(z2 + 1, za + w);
                        fill(x1 - 1 + i, yy, za, x1 - 1 + i, yy, zb, alt + "[facing=east]");
                        int zc = z1 - 1 + rnd.nextInt(len);
                        fill(x2 + 1 - i, yy, zc, x2 + 1 - i, yy, Math.min(z2 + 1, zc + w), alt + "[facing=west]");
                    }
                }
            }
        }
        // ridge cap (slabs) and a crooked ridge (raised humps)
        String slab = slabOf(material);
        boolean meet = depth % 2 == 1;
        if (alongX) {
            int zr = z1 - 1 + layers - 1;
            fill(x1 - 1, ridgeY + 1, zr, x2 + 1, ridgeY + 1, meet ? zr + 1 : zr, slab);
            if (l.crooked()) {
                for (int n = 0; n < 2; n++) {
                    int xa = x1 + rnd.nextInt(Math.max(1, x2 - x1 - 2));
                    fill(xa, ridgeY + 1, zr, xa + 1 + rnd.nextInt(2), ridgeY + 1, meet ? zr + 1 : zr, full);
                    fill(xa, ridgeY + 2, zr, xa + 1, ridgeY + 2, meet ? zr + 1 : zr, slab);
                }
            }
        } else {
            int xr = x1 - 1 + layers - 1;
            fill(xr, ridgeY + 1, z1 - 1, meet ? xr + 1 : xr, ridgeY + 1, z2 + 1, slab);
            if (l.crooked()) {
                for (int n = 0; n < 2; n++) {
                    int za = z1 + rnd.nextInt(Math.max(1, z2 - z1 - 2));
                    fill(xr, ridgeY + 1, za, meet ? xr + 1 : xr, ridgeY + 1, za + 1 + rnd.nextInt(2), full);
                    fill(xr, ridgeY + 2, za, meet ? xr + 1 : xr, ridgeY + 2, za + 1, slab);
                }
            }
        }
        if (l.chimney() != null) {
            chimney(q, y, ridgeY, alongX, doorSide, l, rnd);
        }
    }

    /** A chimney stack against the gable away from the door, fireplace inside, smoke on top. */
    private void chimney(int[] q, int y, int ridgeY, boolean alongX, String doorSide, Look l, Random rnd) {
        int x1 = q[0];
        int z1 = q[1];
        int x2 = q[2];
        int z2 = q[3];
        int mx = (x1 + x2) / 2;
        int mz = (z1 + z2) / 2;
        int cx;
        int cz;
        int fx;
        int fz;
        if (alongX) {
            boolean east = !"east".equals(doorSide);
            cx = east ? x2 + 1 : x1 - 1;
            fx = east ? x2 : x1;
            cz = mz;
            fz = mz;
        } else {
            boolean south = !"south".equals(doorSide);
            cz = south ? z2 + 1 : z1 - 1;
            fz = south ? z2 : z1;
            cx = mx;
            fx = mx;
        }
        int h = ridgeY + 2 + (l.crooked() ? rnd.nextInt(2) : 0);
        fill(cx, g, cz, cx, h, cz, l.chimney());
        fill(fx, g + 1, fz, fx, g + 2, fz, l.chimney());
        set(fx, g, fz, "minecraft:campfire[lit=true,signal_fire=false]");
        set(cx, h + 1, cz, "minecraft:campfire[lit=true]");
        if (l.crooked()) {
            set(cx, h - 1, cz, l.chimney());
        }
    }

    /**
     * Flat-roof dressing (desert/adobe): merlons of {@code cap} on the parapet, projecting beam ends
     * ({@code beam}) under the roof line, and roof clutter (rugs drying, pots, a barrel, a shade canopy).
     */
    public void flatRoof(int[] q, int top, String wall, String cap, String beam, String canopy, long seed) {
        Random rnd = new Random(seed);
        int x1 = q[0];
        int z1 = q[1];
        int x2 = q[2];
        int z2 = q[3];
        if (cap != null) {
            for (int x = x1; x <= x2; x += 2) {
                set(x, top, z1, cap);
                set(x, top, z2, cap);
            }
            for (int z = z1; z <= z2; z += 2) {
                set(x1, top, z, cap);
                set(x2, top, z, cap);
            }
            set(x2, top, z2, cap);
        }
        if (beam != null) {
            for (int x = x1 + 2; x <= x2 - 2; x += 3) {
                set(x, top - 2, z1 - 1, axis(beam, false));
                set(x, top - 2, z2 + 1, axis(beam, false));
            }
            for (int z = z1 + 2; z <= z2 - 2; z += 3) {
                set(x1 - 1, top - 2, z, axis(beam, true));
                set(x2 + 1, top - 2, z, axis(beam, true));
            }
        }
        if (x2 - x1 < 5 || z2 - z1 < 5) {
            return;
        }
        String[] rugs = {"minecraft:red_carpet", "minecraft:orange_carpet", "minecraft:yellow_carpet", "minecraft:white_carpet",
                "minecraft:blue_carpet"};
        String rug = rugs[rnd.nextInt(rugs.length)];
        int rx = x1 + 2 + rnd.nextInt(Math.max(1, x2 - x1 - 5));
        int rz = z1 + 2 + rnd.nextInt(Math.max(1, z2 - z1 - 5));
        fill(rx, top, rz, rx + 1, top, rz + 1, rug);
        set(x2 - 1, top, z1 + 1, rnd.nextBoolean() ? "minecraft:decorated_pot" : "minecraft:barrel[facing=up]");
        set(x1 + 1, top, z2 - 1, "minecraft:flower_pot");
        if (canopy != null && rnd.nextInt(3) > 0) {
            int cx = x1 + 1;
            int cz = z1 + 1;
            fill(cx, top, cz, cx, top + 1, cz, "minecraft:jungle_fence");
            fill(cx + 2, top, cz, cx + 2, top + 1, cz, "minecraft:jungle_fence");
            fill(cx, top, cz + 2, cx, top + 1, cz + 2, "minecraft:jungle_fence");
            fill(cx + 2, top, cz + 2, cx + 2, top + 1, cz + 2, "minecraft:jungle_fence");
            fill(cx, top + 2, cz, cx + 2, top + 2, cz + 2, canopy);
            set(cx + 1, top, cz + 1, "minecraft:jungle_stairs[facing=north]");
        }
    }

    // ------------------------------------------------------------------ interiors

    /**
     * Furnishes the room inside q (walls on q's edges) whose floor is at y-1. Keeps the north-west corner
     * (ladder) and the middle of every wall (doorways) clear.
     */
    public void room(int[] q, int y, Room type, Furniture f, long seed) {
        Random rnd = new Random(seed);
        int x1 = q[0] + 1;
        int z1 = q[1] + 1;
        int x2 = q[2] - 1;
        int z2 = q[3] - 1;
        int w = x2 - x1;
        int d = z2 - z1;
        int mx = (x1 + x2) / 2;
        int mz = (z1 + z2) / 2;
        String slabTop = f.wood() + "_slab[type=top]";
        String stairs = f.wood() + "_stairs";
        if (w < 3 || d < 3) {
            set(x2, y, z2, "minecraft:barrel[facing=up]");
            return;
        }
        // rug with a border
        if (type != Room.STORE && type != Room.KITCHEN && w >= 5 && d >= 5) {
            fill(mx - 2, y, mz - 1, mx + 2, y, mz + 2, f.rugBorder());
            fill(mx - 1, y, mz, mx + 1, y, mz + 1, f.rug());
        }
        switch (type) {
            case HOUSE, BEDROOM -> {
                set(mx, y, mz + 1, slabTop);
                set(mx, y + 1, mz + 1, rnd.nextBoolean() ? "minecraft:candle[candles=2,lit=true]" : "minecraft:potted_red_tulip");
                set(mx - 1, y, mz + 1, stairs + "[facing=east]");
                set(mx + 1, y, mz + 1, stairs + "[facing=west]");
                set(x2, y, z1, "minecraft:chest[facing=west]");
                set(x2, y, z2, "minecraft:barrel[facing=up]");
                set(x2, y + 1, z2, "minecraft:barrel[facing=up]");
                set(x2 - 1, y, z2, "minecraft:barrel[facing=north]");
                if (type == Room.BEDROOM || rnd.nextBoolean()) {
                    set(x1, y, z2, f.bed() + "[facing=north,part=foot]");
                    set(x1 + 1, y, z2 - 1, slabTop);
                    set(x1 + 1, y + 1, z2 - 1, "minecraft:candle[candles=1,lit=true]");
                    set(x1 + 1, y, z2, "minecraft:white_carpet");
                } else {
                    fill(x1, y, z2, x1, y + 1, z2 - 1, "minecraft:bookshelf");
                    set(x1, y + 2, z2, "minecraft:flower_pot");
                }
                if (w >= 5 && d >= 6) {
                    set(x2, y, z1 + 1, "minecraft:crafting_table");
                    set(x2, y, z1 + 2, rnd.nextBoolean() ? "minecraft:cauldron" : "minecraft:loom[facing=west]");
                }
                set(x1 + 2, y + 1, z1, slabTop);
                set(x1 + 2, y + 2, z1, "minecraft:decorated_pot");
            }
            case PUB -> {
                for (int x = x1 + 2; x <= x2 - 2; x += 4) {
                    for (int z = mz; z <= z2 - 1; z += 4) {
                        set(x, y, z, slabTop);
                        set(x, y + 1, z, "minecraft:candle[candles=3,lit=true]");
                        set(x - 1, y, z, stairs + "[facing=east]");
                        set(x + 1, y, z, stairs + "[facing=west]");
                    }
                }
                fill(x2, y, z1, x2, y + 1, z1 + 1, "minecraft:barrel[facing=west]");
                set(x2, y + 2, z1, "minecraft:barrel[facing=up]");
            }
            case SHOP -> {
                fill(x1, y, z1 + 1, x1, y + 1, z1 + 1, "minecraft:barrel[facing=east]");
                set(x2, y, z2, "minecraft:barrel[facing=up]");
                set(x2, y + 1, z2, "minecraft:decorated_pot");
                set(x2 - 1, y, z2, "minecraft:chest[facing=north]");
                fill(x1, y + 1, z2, x1, y + 1, Math.max(z1 + 2, z2 - 2), slabTop);
                set(x1, y + 2, z2, "minecraft:flower_pot");
                set(x1, y, z2, "minecraft:barrel[facing=east]");
            }
            case BANK -> {
                set(x1, y, z1 + 1, "minecraft:chest[facing=east]");
                set(x2, y, z2, "minecraft:chest[facing=west]");
                set(x2, y, z1, "minecraft:chest[facing=west]");
                set(x1, y, z2, "minecraft:potted_fern");
            }
            case TEMPLE -> {
                set(x1, y, z1 + 1, "minecraft:candle[candles=4,lit=true]");
                set(x2, y, z1 + 1, "minecraft:candle[candles=4,lit=true]");
            }
            case STORE -> {
                fill(x1 + 1, y, z2, x2, y, z2, "minecraft:barrel[facing=north]");
                fill(x1 + 1, y + 1, z2, x1 + 3, y + 1, z2, "minecraft:barrel[facing=up]");
                set(x2, y, z1, "minecraft:chest[facing=west]");
                set(x2, y, z1 + 1, "minecraft:hay_block");
            }
            case GUARD -> {
                set(x2, y, z1, "minecraft:chest[facing=west]");
                set(x1, y, z2, f.bed() + "[facing=north,part=foot]");
                set(x2, y, z2, "minecraft:smithing_table");
                set(x2 - 1, y, z2, "minecraft:grindstone[face=floor,facing=north]");
                set(mx, y, mz + 1, slabTop);
                set(mx - 1, y, mz + 1, stairs + "[facing=east]");
            }
            case LIBRARY -> {
                fill(x1 + 1, y, z2, x2 - 1, y + 1, z2, "minecraft:bookshelf");
                fill(x2, y, z1 + 1, x2, y + 1, z2 - 1, "minecraft:bookshelf");
                set(mx, y, mz, "minecraft:lectern[facing=north]");
                set(mx - 1, y, mz - 1, stairs + "[facing=east]");
            }
            case KITCHEN -> {
                fill(x1 + 1, y, z2, x2 - 1, y, z2, slabTop);
                set(x1 + 1, y + 1, z2, "minecraft:cake");
                set(mx, y, z2, "minecraft:smoker[facing=north]");
                set(x2, y, z2, "minecraft:barrel[facing=up]");
                set(x2, y, z1, "minecraft:water_cauldron[level=3]");
                set(mx, y + 2, mz, "minecraft:chain");
            }
            case CRYPT -> {
                set(x1, y, z2, "minecraft:skeleton_skull");
                set(x2, y, z2, "minecraft:candle[candles=3,lit=true]");
                set(x2, y, z1, "minecraft:bone_block");
            }
        }
        if (f.gloomy()) {
            int top = y + 2;
            set(x2, top, z1, "minecraft:cobweb");
            set(x1 + 1, top, z2, "minecraft:cobweb");
            if (rnd.nextBoolean()) {
                set(x2, top, z2, "minecraft:cobweb");
            }
        }
    }

    // ------------------------------------------------------------------ street furniture

    /** A lamp post: base, post and a lantern on top (or hanging from an arm when {@code arm}). */
    public void lampPost(int x, int z, String base, String post, String lantern, boolean arm) {
        if (base != null) {
            set(x, g, z, base);
        }
        fill(x, base != null ? g + 1 : g, z, x, g + 2, z, post);
        if (arm) {
            set(x, g + 3, z, post);
            set(x + 1, g + 3, z, post);
            set(x + 1, g + 2, z, lantern + "[hanging=true]");
        } else {
            set(x, g + 3, z, lantern);
        }
    }

    /** A signpost: a fence post with two arms of trapdoor "boards". */
    public void signpost(int x, int z, String wood) {
        fill(x, g, z, x, g + 2, z, wood + "_fence");
        set(x, g + 2, z - 1, wood + "_trapdoor[facing=north,open=true]");
        set(x + 1, g + 1, z, wood + "_trapdoor[facing=east,open=true]");
        set(x, g + 3, z, wood + "_pressure_plate");
    }

    /** A pile of crates and barrels around (x, z). */
    public void crates(int x, int z, long seed) {
        Random rnd = new Random(seed);
        set(x, g, z, "minecraft:barrel[facing=up]");
        set(x + 1, g, z, rnd.nextBoolean() ? "minecraft:barrel[facing=east]" : "minecraft:spruce_planks");
        set(x, g, z + 1, "minecraft:spruce_planks");
        set(x, g + 1, z, rnd.nextBoolean() ? "minecraft:barrel[facing=up]" : "minecraft:spruce_slab");
        if (rnd.nextBoolean()) {
            set(x + 1, g, z + 1, "minecraft:hay_block");
        }
    }

    /** A bench of stairs with arm rests, facing {@code facing} (the direction a sitter looks). */
    public void bench(int x, int z, boolean alongX, String facing, String wood) {
        String st = wood + "_stairs[facing=" + opposite(facing) + "]";
        if (alongX) {
            fill(x, g, z, x + 1, g, z, st);
            set(x - 1, g, z, wood + "_trapdoor[facing=west,open=true]");
            set(x + 2, g, z, wood + "_trapdoor[facing=east,open=true]");
        } else {
            fill(x, g, z, x, g, z + 1, st);
            set(x, g, z - 1, wood + "_trapdoor[facing=north,open=true]");
            set(x, g, z + 2, wood + "_trapdoor[facing=south,open=true]");
        }
    }

    /** A flower bed edged with {@code edge} over a block rectangle. */
    public void flowerBed(int x1, int z1, int x2, int z2, String edge, long seed) {
        Random rnd = new Random(seed);
        String[] fl = {"minecraft:poppy", "minecraft:dandelion", "minecraft:cornflower", "minecraft:allium", "minecraft:oxeye_daisy",
                "minecraft:azure_bluet", "minecraft:red_tulip", "minecraft:fern"};
        b.walls(x1, g - 1, z1, x2, g - 1, z2, edge);
        fill(x1 + 1, g - 1, z1 + 1, x2 - 1, g - 1, z2 - 1, "minecraft:grass_block");
        for (int x = x1 + 1; x < x2; x++) {
            for (int z = z1 + 1; z < z2; z++) {
                if (rnd.nextInt(3) > 0) {
                    set(x, g, z, fl[rnd.nextInt(fl.length)]);
                }
            }
        }
    }

    /** Edges a path rectangle with a kerb of {@code edge} along its long sides (in the ground). */
    public void kerb(int x1, int z1, int x2, int z2, String edge) {
        if (x2 - x1 >= z2 - z1) {
            fill(x1, g - 1, z1, x2, g - 1, z1, edge);
            fill(x1, g - 1, z2, x2, g - 1, z2, edge);
        } else {
            fill(x1, g - 1, z1, x1, g - 1, z2, edge);
            fill(x2, g - 1, z1, x2, g - 1, z2, edge);
        }
    }

    /** A covered market stall (3x3 at x..x+2, z..z+2) with a striped awning, counter, goods and the station. */
    public void stall(int x, int z, String station, String wool, String stripe, String post) {
        for (int dx : new int[]{0, 2}) {
            for (int dz : new int[]{0, 2}) {
                fill(x + dx, g, z + dz, x + dx, g + 2, z + dz, post);
            }
        }
        for (int dx = 0; dx <= 2; dx++) {
            fill(x + dx, g + 3, z, x + dx, g + 3, z + 2, dx % 2 == 0 ? wool : stripe);
        }
        // the awning overhangs the counter side by a block
        for (int dx = 0; dx <= 2; dx++) {
            set(x + dx, g + 3, z + 3, dx % 2 == 0 ? wool : stripe);
        }
        fill(x, g, z + 2, x + 2, g, z + 2, "minecraft:spruce_slab[type=top]");
        set(x, g + 1, z + 2, goods(station, 0));
        set(x + 2, g + 1, z + 2, goods(station, 1));
        set(x + 1, g, z + 1, station);
        set(x + 1, g, z, "minecraft:barrel[facing=up]");
        set(x + 1, g + 2, z + 1, "minecraft:lantern[hanging=true]");
    }

    /** Something to show on a stall counter for a given station block. */
    static String goods(String station, int i) {
        String s = station == null ? "" : station;
        if (s.contains("fish")) {
            return i == 0 ? "minecraft:barrel[facing=up]" : "minecraft:sea_pickle";
        }
        if (s.contains("silk") || s.contains("fur")) {
            return i == 0 ? "minecraft:white_carpet" : "minecraft:pink_carpet";
        }
        if (s.contains("spice")) {
            return i == 0 ? "minecraft:decorated_pot" : "minecraft:potted_dead_bush";
        }
        if (s.contains("gem") || s.contains("magic") || s.contains("rune")) {
            return i == 0 ? "minecraft:amethyst_cluster" : "minecraft:candle[candles=2,lit=true]";
        }
        if (s.contains("veg") || s.contains("fruit") || s.contains("food")) {
            return i == 0 ? "minecraft:melon" : "minecraft:pumpkin";
        }
        if (s.contains("bake") || s.contains("cake")) {
            return i == 0 ? "minecraft:cake" : "minecraft:hay_block";
        }
        if (s.contains("weapon") || s.contains("scimitar") || s.contains("archery")) {
            return i == 0 ? "minecraft:grindstone[face=floor,facing=north]" : "minecraft:anvil";
        }
        return i == 0 ? "minecraft:decorated_pot" : "minecraft:flower_pot";
    }

    /** Striped wool awning over a block rectangle at height y, stripes running across {@code alongX}. */
    public void awning(int x1, int z1, int x2, int z2, int y, String wool, String stripe, boolean alongX) {
        if (alongX) {
            for (int x = x1; x <= x2; x++) {
                fill(x, y, z1, x, y, z2, (x - x1) % 2 == 0 ? wool : stripe);
            }
        } else {
            for (int z = z1; z <= z2; z++) {
                fill(x1, y, z, x2, y, z, (z - z1) % 2 == 0 ? wool : stripe);
            }
        }
    }

    /** A well with a stone rim, a wooden frame and a little pitched roof of {@code roof} stairs. */
    public void well(int x, int z, String rim, String post, String roof) {
        b.walls(x - 1, g, z - 1, x + 1, g, z + 1, rim);
        fill(x, g - 6, z, x, g - 1, z, "minecraft:water");
        set(x, g, z, "minecraft:water");
        fill(x - 1, g - 7, z - 1, x + 1, g - 7, z + 1, rim);
        fill(x - 1, g + 1, z, x - 1, g + 2, z, post);
        fill(x + 1, g + 1, z, x + 1, g + 2, z, post);
        set(x, g + 3, z, slabOf(roof));
        fill(x - 1, g + 3, z - 1, x + 1, g + 3, z - 1, stairsOf(roof) + "[facing=south]");
        fill(x - 1, g + 3, z + 1, x + 1, g + 3, z + 1, stairsOf(roof) + "[facing=north]");
        set(x, g + 2, z, "minecraft:chain");
        set(x, g + 1, z, "minecraft:lantern[hanging=true]");
    }

    // ------------------------------------------------------------------ nature & ruin

    /** A palm with a leaning trunk, drooping fronds and coconuts (cocoa). */
    public void palm(int x, int z, int h, long seed) {
        Random rnd = new Random(seed);
        int lx = rnd.nextInt(3) - 1;
        int lz = lx == 0 ? (rnd.nextBoolean() ? 1 : -1) : 0;
        int bend = h / 2;
        fill(x, g, z, x, g + bend - 1, z, "minecraft:jungle_log");
        int tx = x + lx;
        int tz = z + lz;
        fill(tx, g + bend, tz, tx, g + h - 1, tz, "minecraft:jungle_log");
        String leaf = "minecraft:jungle_leaves[persistent=true]";
        int y = g + h;
        set(tx, y, tz, leaf);
        set(tx, y + 1, tz, leaf);
        for (int[] dir : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            fill(tx + dir[0], y, tz + dir[1], tx + 2 * dir[0], y, tz + 2 * dir[1], leaf);
            set(tx + 3 * dir[0], y - 1, tz + 3 * dir[1], leaf);
            set(tx + 4 * dir[0], y - 2, tz + 4 * dir[1], leaf);
        }
        for (int[] dir : new int[][]{{1, 1}, {-1, 1}, {1, -1}, {-1, -1}}) {
            set(tx + dir[0], y, tz + dir[1], leaf);
            set(tx + 2 * dir[0], y - 1, tz + 2 * dir[1], leaf);
        }
        set(tx, y - 1, tz + 1, "minecraft:cocoa[age=2,facing=north]");
        set(tx + 1, y - 1, tz, "minecraft:cocoa[age=2,facing=west]");
    }

    /** A crooked dead tree with a forked crown and drooping twigs. */
    public void deadTree(int x, int z, int h, long seed) {
        Random rnd = new Random(seed);
        String log = "minecraft:dark_oak_log";
        String twig = "minecraft:dark_oak_fence";
        set(x, g - 1, z, "minecraft:podzol");
        // Stays within x-1..x+2, z-2..z like the plain dead tree it replaces.
        int kink = 2 + rnd.nextInt(2);
        fill(x, g, z, x, g + kink - 1, z, log);
        int tx = x + 1;
        set(tx, g + kink - 1, z, log + "[axis=x]");
        fill(tx, g + kink, z, tx, g + h - 1, z, log);
        int y = g + h - 1;
        // branches
        set(tx + 1, y - 1, z, log + "[axis=x]");
        set(tx + 1, y, z, twig);
        set(tx + 1, y + 1, z, twig);
        fill(tx, y - 2, z - 2, tx, y - 2, z - 1, log + "[axis=z]");
        set(tx, y - 1, z - 2, twig);
        set(tx, y, z - 2, twig);
        set(x, y, z, log + "[axis=x]");
        set(x - 1, y, z, twig);
        set(x - 1, y + 1, z, twig);
        set(x, y - 3, z - 1, twig);
        set(tx, y + 1, z, twig);
        set(tx, y + 2, z, twig);
        set(x - 1, g, z, "minecraft:dark_oak_log[axis=x]");
        if (rnd.nextBoolean()) {
            set(tx + 1, y - 2, z, "minecraft:cobweb");
        }
    }

    /** A leafy broadleaf tree with a lumpy two-tier crown. */
    public void tree(int x, int z, int h, String log, String leaves, long seed) {
        Random rnd = new Random(seed);
        String leaf = leaves.contains("[") ? leaves : leaves + "[persistent=true]";
        fill(x - 2, g + h - 3, z - 2, x + 2, g + h - 2, z + 2, leaf);
        fill(x - 1, g + h - 1, z - 1, x + 1, g + h, z + 1, leaf);
        set(x, g + h + 1, z, leaf);
        for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            if (rnd.nextBoolean()) {
                set(x + c[0], g + h - 2, z + c[1], "minecraft:air");
            }
        }
        set(x + 3, g + h - 3, z, leaf);
        set(x - 3, g + h - 2, z + rnd.nextInt(3) - 1, leaf);
        fill(x, g, z, x, g + h - 1, z, log);
        set(x + 1, g, z, log + "[axis=x]");
    }

    /** A low leafy bush. */
    public void bush(int x, int z, String leaves) {
        String leaf = leaves.contains("[") ? leaves : leaves + "[persistent=true]";
        fill(x, g, z, x + 1, g, z, leaf);
        set(x, g + 1, z, leaf);
    }

    /** A rubble pile of mixed slabs, stairs and full blocks around (x, z). */
    public void rubble(int x, int z, String[] mats, long seed) {
        Random rnd = new Random(seed);
        for (int i = 0; i < 5; i++) {
            int px = x + rnd.nextInt(3) - 1;
            int pz = z + rnd.nextInt(3) - 1;
            String m = mats[rnd.nextInt(mats.length)];
            String alt = rnd.nextInt(3) == 0 ? "minecraft:cobblestone_slab" : rnd.nextBoolean() ? m : "minecraft:cobblestone_stairs[facing="
                    + new String[]{"north", "south", "east", "west"}[rnd.nextInt(4)] + "]";
            set(px, g, pz, alt);
        }
        set(x, g, z, mats[0]);
        set(x, g + 1, z, "minecraft:cobblestone_slab");
    }

    /**
     * Weathering: {@code count} small random boxes in the volume where {@code from} becomes {@code to}
     * (mossy or cracked variants, repairs, soot...).
     */
    public void weather(int x1, int y1, int z1, int x2, int y2, int z2, String from, String to, int count, long seed) {
        Random rnd = new Random(seed);
        for (int i = 0; i < count; i++) {
            int x = x1 + rnd.nextInt(Math.max(1, x2 - x1 + 1));
            int y = y1 + rnd.nextInt(Math.max(1, y2 - y1 + 1));
            int z = z1 + rnd.nextInt(Math.max(1, z2 - z1 + 1));
            replace(x, y, z, Math.min(x2, x + rnd.nextInt(3)), Math.min(y2, y + rnd.nextInt(3)), Math.min(z2, z + rnd.nextInt(3)),
                    from, to);
        }
    }

    /** Overgrowth: vines/moss carpets/grass dropped only into empty air cells just above the ground. */
    public void overgrowth(int x1, int z1, int x2, int z2, int count, long seed) {
        Random rnd = new Random(seed);
        String[] plants = {"minecraft:grass", "minecraft:fern", "minecraft:moss_carpet", "minecraft:grass", "minecraft:sweet_berry_bush[age=1]"};
        for (int i = 0; i < count; i++) {
            int x = x1 + rnd.nextInt(Math.max(1, x2 - x1 + 1));
            int z = z1 + rnd.nextInt(Math.max(1, z2 - z1 + 1));
            b.op(StructureDef.Op.Type.FILL_AIR, x, g, z, x, g, z, plants[rnd.nextInt(plants.length)], null);
        }
    }

    // ------------------------------------------------------------------ domes

    /**
     * A smooth dome: each layer's rim made of stairs facing outward (so the curve reads round), solid inside,
     * hollow below {@code y0} (sits on a roof). {@code cap} crowns it.
     */
    public void stairDome(int cx, int cz, int y0, double radius, String full, String stairs, String cap) {
        int h = (int) Math.ceil(radius);
        for (int i = 0; i < h; i++) {
            double r = radius * Math.sqrt(1 - Math.pow((double) i / h, 2));
            double rn = radius * Math.sqrt(Math.max(0, 1 - Math.pow((double) (i + 1) / h, 2)));
            int rr = (int) Math.floor(r);
            for (int dz = -rr; dz <= rr; dz++) {
                int w = (int) Math.floor(Math.sqrt(Math.max(0, r * r - dz * dz)));
                if (w < 0) {
                    continue;
                }
                fill(cx - w, y0 + i, cz + dz, cx + w, y0 + i, cz + dz, full);
            }
            if (stairs != null) {
                for (int dz = -rr; dz <= rr; dz++) {
                    int w = (int) Math.floor(Math.sqrt(Math.max(0, r * r - dz * dz)));
                    for (int dx = -w; dx <= w; dx++) {
                        double dd = Math.sqrt(dx * dx + dz * dz);
                        if (dd > rn - 0.2 && dd > 0.5) {
                            String face;
                            if (Math.abs(dx) >= Math.abs(dz)) {
                                face = dx > 0 ? "west" : "east";
                            } else {
                                face = dz > 0 ? "north" : "south";
                            }
                            set(cx + dx, y0 + i, cz + dz, stairs + "[facing=" + face + "]");
                        }
                    }
                }
            }
        }
        if (cap != null) {
            set(cx, y0 + h, cz, cap);
        }
    }

    // ------------------------------------------------------------------ docks & boats

    /** Mooring posts with rope (chains/fences) and lanterns along the edge of a deck from (x1,z) to (x2,z). */
    public void mooringX(int x1, int x2, int z, int deckY, int step) {
        for (int x = x1; x <= x2; x += step) {
            fill(x, deckY - 6, z, x, deckY + 1, z, "minecraft:spruce_log");
            set(x, deckY + 2, z, "minecraft:spruce_slab");
            if (x + step <= x2) {
                fill(x + 1, deckY + 1, z, x + step - 1, deckY + 1, z, "minecraft:spruce_fence");
            }
        }
    }

    /** Same along z. */
    public void mooringZ(int x, int z1, int z2, int deckY, int step) {
        for (int z = z1; z <= z2; z += step) {
            fill(x, deckY - 6, z, x, deckY + 1, z, "minecraft:spruce_log");
            set(x, deckY + 2, z, "minecraft:spruce_slab");
            if (z + step <= z2) {
                fill(x, deckY + 1, z + 1, x, deckY + 1, z + step - 1, "minecraft:spruce_fence");
            }
        }
    }

    /**
     * A rowing boat (5 long, along x) floating at sea level beside a dock: hull, benches, oars and a rope.
     * {@code y} is the waterline (top of the water).
     */
    public void rowboat(int x, int z, int y, String wood) {
        String planks = wood + "_planks";
        String stairs = wood + "_stairs";
        fill(x + 1, y, z, x + 3, y, z + 2, planks);
        set(x, y, z + 1, stairs + "[facing=east]");
        set(x + 4, y, z + 1, stairs + "[facing=west]");
        fill(x + 1, y + 1, z, x + 3, y + 1, z, wood + "_slab");
        fill(x + 1, y + 1, z + 2, x + 3, y + 1, z + 2, wood + "_slab");
        set(x, y + 1, z + 1, wood + "_trapdoor[facing=east,half=bottom]");
        set(x + 4, y + 1, z + 1, wood + "_trapdoor[facing=west,half=bottom]");
        set(x + 2, y + 1, z + 1, wood + "_trapdoor[half=bottom,facing=north]");
        set(x + 2, y + 1, z - 1, wood + "_fence");
        set(x + 2, y + 1, z + 3, wood + "_fence");
    }
}
