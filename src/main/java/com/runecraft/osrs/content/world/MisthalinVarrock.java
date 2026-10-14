package com.runecraft.osrs.content.world;

import com.runecraft.osrs.content.world.MisthalinKit.Box;
import com.runecraft.osrs.content.world.MisthalinKit.Roof;
import com.runecraft.osrs.world.StructureDef;

import java.util.List;

import static com.runecraft.osrs.content.world.MisthalinKit.*;

/**
 * Varrock: the city walls with their west, south, east and north gates, the central square and
 * fountain, Varrock Palace with its towers and gardens, both banks, the shops (Aubury, Zaff,
 * Horvik, Lowe, the general store, the sword shop, Thessalia), the Blue Moon Inn, the church, the
 * museum and the houses; outside the walls the Grand Exchange, the Cooks' Guild, the Champions'
 * Guild, the east mine, the lumber yard, Gertrude's and Juliet's houses and the stone circle.
 */
final class MisthalinVarrock {
    private MisthalinVarrock() {
    }

    static final String WALL = STONE_BRICKS;

    static void addAll(List<StructureDef> out) {
        out.add(city());
        out.add(grandExchange());
        out.add(outskirts());
        out.add(teaStall());
        out.add(sewers());
        out.add(treePatch());
        out.add(championsGuild());
        out.add(championsBush());
        out.add(stoneCircle());
    }

    /** A Varrock house: stone ground floor, spruce upper storeys framed with dark oak, spruce roof. */
    static MisthalinKit.House house(MisthalinKit k, Box b, int storeys) {
        return k.house(b).storeys(storeys).wall(STONE_BRICKS).upperWall(SPRUCE_PLANKS).corner(DARK_LOG)
                .beam(STRIPPED_DARK).base(COBBLE).floor(SPRUCE_PLANKS).roof(Roof.SPRUCE).gable(SPRUCE_PLANKS);
    }

    /** A shop sign: a banner over the door on the outside of the wall. */
    static void sign(MisthalinKit k, Box b, char side, int along, String colour) {
        switch (side) {
            case 'n' -> k.set(along, 3, b.z1() - 1, "minecraft:" + colour + "_wall_banner[facing=north]");
            case 's' -> k.set(along, 3, b.z2() + 1, "minecraft:" + colour + "_wall_banner[facing=south]");
            case 'e' -> k.set(b.x2() + 1, 3, along, "minecraft:" + colour + "_wall_banner[facing=east]");
            default -> k.set(b.x1() - 1, 3, along, "minecraft:" + colour + "_wall_banner[facing=west]");
        }
    }

    // ------------------------------------------------------------------ the city

    static StructureDef city() {
        MisthalinKit k = new MisthalinKit("varrock", "Varrock", "VARROCK");
        k.tFlatten(3166, 3374, 3284, 3514);
        k.meadow(t(3166, 3374, 3284, 3514), 14);
        streets(k);
        walls(k);
        square(k);
        palace(k);
        banks(k);
        shops(k);
        blueMoonInn(k);
        church(k);
        museum(k);
        homes(k);
        // street trees and lamps
        int[][] trees = {{3196, 3420}, {3226, 3445}, {3240, 3402}, {3262, 3440}, {3188, 3466}, {3240, 3470},
                {3268, 3500}, {3180, 3398}, {3262, 3408}, {3196, 3470}, {3270, 3388}, {3236, 3460}};
        for (int[] tr : trees) {
            k.tOak(tr[0], tr[1]);
        }
        for (int x = 3180; x <= 3270; x += 9) {
            k.lampPost(X(x), Z(3431) + 1);
            k.lampPost(X(x), Z(3426));
        }
        for (int y = 3390; y <= 3452; y += 9) {
            k.lampPost(X(3209), Z(y));
            k.lampPost(X(3214) + 1, Z(y));
        }
        return k.build();
    }

    private static void streets(MisthalinKit k) {
        k.tStreet(3176, 3430, 3273, 3430, 4);
        k.tStreet(3210, 3384, 3210, 3456, 4);
        k.tStreet(3244, 3384, 3244, 3504, 3);
        k.tStreet(3176, 3406, 3243, 3406, 2);
        k.tStreet(3247, 3406, 3273, 3406, 2);
        k.tStreet(3233, 3467, 3243, 3467, 2);
        k.tStreet(3247, 3467, 3273, 3467, 2);
        k.tStreet(3176, 3448, 3200, 3448, 2);
        // roads out of the gates
        k.tStreet(3166, 3430, 3175, 3430, 4);
        k.tStreet(3275, 3430, 3284, 3430, 4);
        k.tStreet(3210, 3374, 3210, 3383, 4);
        k.tStreet(3244, 3505, 3244, 3514, 4);
    }

    // ---- walls

    private static void wallLine(MisthalinKit k, int tx1, int ty1, int tx2, int ty2, char outer) {
        Box b = t(tx1, ty1, tx2, ty2);
        k.fill(b, -3, 7, WALL);
        for (int y = 0; y <= 7; y += 3) {
            k.scatter(b, y, MOSSY_BRICKS, 9);
            k.scatter(b, y + 1, CRACKED_BRICKS, 14);
        }
        switch (outer) {
            case 'w' -> {
                for (int z = b.z1(); z <= b.z2(); z += 2) {
                    k.set(b.x1(), 8, z, WALL);
                }
            }
            case 'e' -> {
                for (int z = b.z1(); z <= b.z2(); z += 2) {
                    k.set(b.x2(), 8, z, WALL);
                }
            }
            case 'n' -> {
                for (int x = b.x1(); x <= b.x2(); x += 2) {
                    k.set(x, 8, b.z1(), WALL);
                }
            }
            default -> {
                for (int x = b.x1(); x <= b.x2(); x += 2) {
                    k.set(x, 8, b.z2(), WALL);
                }
            }
        }
    }

    /** A round wall tower whose middle floor is level with the wall walk (standing height 8). */
    private static void wallTower(MisthalinKit k, int tx, int ty, boolean ns, boolean ew) {
        int cx = X(tx);
        int cz = Z(ty);
        k.tower(cx, cz, 4, 14, 8, WALL, SPRUCE_PLANKS, null, false);
        k.ladder(cx - 2, cz + 2, "east", 0, 13);
        if (ns) {
            k.clear(cx, 8, cz - 4, cx + 1, 10, cz - 4).clear(cx, 8, cz + 4, cx + 1, 10, cz + 4);
            k.clear(cx, 8, cz + 5, cx + 1, 8, cz + 5);
        }
        if (ew) {
            k.clear(cx - 4, 8, cz, cx - 4, 10, cz + 1).clear(cx + 4, 8, cz, cx + 4, 10, cz + 1);
        }
        k.set(cx, 14, cz, "minecraft:spruce_fence");
        k.set(cx, 15, cz, "minecraft:spruce_fence");
        k.set(cx, 16, cz, "minecraft:red_wool");
        k.set(cx + 1, 16, cz, "minecraft:red_wool");
    }

    /**
     * A gatehouse: two square towers flanking a four-tile opening with an arch, portcullis bars and
     * torches. {@code alongX} is true for gates in east-west walls (the road runs north-south).
     */
    private static void gate(MisthalinKit k, int tx, int ty, boolean alongX) {
        // tx,ty = the first tile of the 4-tile opening (west-most or south-most)
        Box open;
        Box a;
        Box c;
        if (alongX) {
            open = t(tx, ty, tx + 3, ty);
            a = t(tx - 3, ty - 1, tx - 1, ty + 1);
            c = t(tx + 4, ty - 1, tx + 6, ty + 1);
        } else {
            open = t(tx, ty, tx, ty + 3);
            a = t(tx - 1, ty - 3, tx + 1, ty - 1);
            c = t(tx - 1, ty + 4, tx + 1, ty + 6);
        }
        k.clear(open, 0, 4);
        k.fill(open, 5, 7, WALL);
        k.floor(open, POLISHED_ANDESITE);
        if (alongX) {
            k.connect(open.x1(), 4, open.z1(), open.x2(), 4, open.z1(), BARS);
            for (int x = open.x1(); x <= open.x2(); x += 2) {
                k.set(x, 8, open.z1(), WALL).set(x, 8, open.z2(), WALL);
            }
        } else {
            k.connect(open.x1(), 4, open.z1(), open.x1(), 4, open.z2(), BARS);
            for (int z = open.z1(); z <= open.z2(); z += 2) {
                k.set(open.x1(), 8, z, WALL).set(open.x2(), 8, z, WALL);
            }
        }
        for (Box tw : new Box[]{a, c}) {
            k.house(tw).storeys(3).height(4).wall(WALL).corner(CHISELED).base(COBBLE).floor(SPRUCE_PLANKS)
                    .roof(Roof.STONE).crenel(WALL).window(null).build();
            Box in = tw.inset(1);
            k.ladder(in.x1(), in.z1(), "south", 0, 11);
            // arrow slits
            for (int y = 5; y <= 9; y += 4) {
                k.set(tw.cx(), y, tw.z1(), "minecraft:iron_bars").set(tw.cx(), y, tw.z2(), "minecraft:iron_bars");
                k.set(tw.x1(), y, tw.cz(), "minecraft:iron_bars").set(tw.x2(), y, tw.cz(), "minecraft:iron_bars");
            }
            k.hang(in.cx(), 2, in.cz());
            k.hang(in.cx(), 6, in.cz());
        }
        // ground doors into the towers from the city side and wall-walk doors at standing height 8
        if (alongX) {
            k.clear(a.x2(), 0, a.cz(), a.x2(), 2, a.cz()).clear(c.x1(), 0, c.cz(), c.x1(), 2, c.cz());
            k.clear(a.x1(), 8, a.cz(), a.x1(), 10, a.cz() + 1).clear(c.x2(), 8, c.cz(), c.x2(), 10, c.cz() + 1);
            k.clear(a.x2(), 8, a.cz(), a.x2(), 10, a.cz() + 1).clear(c.x1(), 8, c.cz(), c.x1(), 10, c.cz() + 1);
            k.set(a.x2(), 3, a.z1() - 1, "minecraft:wall_torch[facing=north]");
            k.set(c.x1(), 3, c.z1() - 1, "minecraft:wall_torch[facing=north]");
            k.set(a.x2(), 3, a.z2() + 1, "minecraft:wall_torch[facing=south]");
            k.set(c.x1(), 3, c.z2() + 1, "minecraft:wall_torch[facing=south]");
        } else {
            k.clear(a.cx(), 0, a.z2(), a.cx(), 2, a.z2()).clear(c.cx(), 0, c.z1(), c.cx(), 2, c.z1());
            k.clear(a.cx(), 8, a.z1(), a.cx() + 1, 10, a.z1()).clear(c.cx(), 8, c.z2(), c.cx() + 1, 10, c.z2());
            k.clear(a.cx(), 8, a.z2(), a.cx() + 1, 10, a.z2()).clear(c.cx(), 8, c.z1(), c.cx() + 1, 10, c.z1());
            k.set(a.x1() - 1, 3, a.z2(), "minecraft:wall_torch[facing=west]");
            k.set(c.x1() - 1, 3, c.z1(), "minecraft:wall_torch[facing=west]");
            k.set(a.x2() + 1, 3, a.z2(), "minecraft:wall_torch[facing=east]");
            k.set(c.x2() + 1, 3, c.z1(), "minecraft:wall_torch[facing=east]");
        }
    }

    private static void walls(MisthalinKit k) {
        // west wall x 3175 (gate 3427-3430) to the north-west corner at 3466, east to the palace,
        // then north along x 3196 (the Grand Exchange lies outside the north-west corner)
        wallLine(k, 3175, 3383, 3175, 3423, 'w');
        wallLine(k, 3175, 3434, 3175, 3466, 'w');
        wallLine(k, 3175, 3466, 3196, 3466, 'n');
        wallLine(k, 3196, 3466, 3196, 3505, 'w');
        // south wall y 3383 (gate 3210-3213)
        wallLine(k, 3175, 3383, 3206, 3383, 's');
        wallLine(k, 3217, 3383, 3274, 3383, 's');
        // east wall x 3274 (gate 3427-3430)
        wallLine(k, 3274, 3383, 3274, 3423, 'e');
        wallLine(k, 3274, 3434, 3274, 3505, 'e');
        // north wall y 3505 (gate 3244-3247)
        wallLine(k, 3196, 3505, 3240, 3505, 'n');
        wallLine(k, 3251, 3505, 3274, 3505, 'n');
        // postern towards the Grand Exchange
        k.clear(t(3189, 3466, 3190, 3466), 0, 3);
        k.floor(t(3189, 3466, 3190, 3466), POLISHED_ANDESITE);
        k.fill(t(3189, 3466, 3190, 3466), 4, 4, CHISELED);
        k.tStreet(3189, 3465, 3189, 3449, 2);
        // round towers at the corners and along the walls
        wallTower(k, 3175, 3383, true, true);
        wallTower(k, 3274, 3383, true, true);
        wallTower(k, 3175, 3466, true, true);
        wallTower(k, 3196, 3466, true, true);
        wallTower(k, 3196, 3505, true, true);
        wallTower(k, 3274, 3505, true, true);
        wallTower(k, 3175, 3405, true, false);
        wallTower(k, 3175, 3448, true, false);
        wallTower(k, 3274, 3405, true, false);
        wallTower(k, 3274, 3458, true, false);
        wallTower(k, 3190, 3383, false, true);
        wallTower(k, 3240, 3383, false, true);
        wallTower(k, 3220, 3505, false, true);
        wallTower(k, 3265, 3505, false, true);
        // gates
        gate(k, 3175, 3427, false);
        gate(k, 3274, 3427, false);
        gate(k, 3210, 3383, true);
        gate(k, 3244, 3505, true);
        // steps up to the wall walk on the inside
        k.stairsSolid(X(3176) + 1, 0, Z(3440), 'n', 8, "minecraft:stone_brick_stairs", WALL);
        k.stairsSolid(X(3273), 0, Z(3440), 'n', 8, "minecraft:stone_brick_stairs", WALL);
        k.stairsSolid(X(3226), 0, Z(3384), 'e', 8, "minecraft:stone_brick_stairs", WALL);
        k.stairsSolid(X(3256), 0, Z(3504) + 1, 'e', 8, "minecraft:stone_brick_stairs", WALL);
    }

    // ---- square and fountain

    private static void square(MisthalinKit k) {
        Box sq = t(3201, 3419, 3224, 3438);
        k.clear(sq, 0, 0);
        k.floor(sq, POLISHED_ANDESITE);
        k.scatter(sq, -1, ANDESITE, 6);
        k.walls(sq, -1, -1, STONE_BRICKS);
        k.walls(sq.inset(1), -1, -1, CHISELED);
        int cx = X(3213);
        int cz = Z(3429);
        k.disc(cx, cz, 8, -1, -1, "minecraft:smooth_stone");
        k.ring(cx, cz, 8, 1, -1, -1, STONE_BRICKS);
        // the fountain: stone basin, water, a tiered column with a spout
        k.disc(cx, cz, 4, -3, -2, STONE_BRICKS);
        k.ring(cx, cz, 4, 1, -1, 0, POLISHED_ANDESITE);
        k.disc(cx, cz, 3, -1, 0, WATER);
        k.fill(cx, -1, cz, cx, 2, cz, CHISELED);
        k.set(cx, 3, cz, "minecraft:stone_brick_slab[type=bottom]");
        k.set(cx, 4, cz, WATER);
        // benches facing the fountain
        for (int d = -2; d <= 2; d++) {
            k.set(cx + d, 0, cz - 7, "minecraft:spruce_stairs[facing=north]");
            k.set(cx + d, 0, cz + 7, "minecraft:spruce_stairs[facing=south]");
            k.set(cx - 7, 0, cz + d, "minecraft:spruce_stairs[facing=west]");
            k.set(cx + 7, 0, cz + d, "minecraft:spruce_stairs[facing=east]");
        }
        k.set(cx + 6, 0, cz - 6, LANTERN).set(cx - 6, 0, cz - 6, LANTERN).set(cx + 6, 0, cz + 6, LANTERN).set(cx - 6, 0, cz + 6, LANTERN);
        // Baraek's fur stall on the square
        k.stall(X(3218), Z(3435), "runecraft:fur_stall", "brown");
        k.crates(X(3221), Z(3436));
        k.stall(X(3205), Z(3422), "runecraft:cake_stall", "yellow");
        // flower tubs on the square's corners, a sign post and sacks by the stalls
        int[][] tubs = {{sq.x1() + 3, sq.z1() + 3}, {sq.x2() - 3, sq.z1() + 3}, {sq.x1() + 3, sq.z2() - 3}, {sq.x2() - 3, sq.z2() - 3}};
        int i = 0;
        for (int[] p : tubs) {
            k.d.planter(p[0], k.g, p[1], i++);
            k.d.planter(p[0] + (p[0] < cx ? 1 : -1), k.g, p[1], i++);
        }
        k.d.signpost(cx + 9, k.g, cz - 9, "spruce");
        k.d.crates(X(3207), k.g, Z(3421));
        k.d.bench(cx - 10, k.g, cz + 9, 'e', 3, 'n', "spruce");
        k.d.bench(cx + 8, k.g, cz + 9, 'e', 3, 'n', "spruce");
    }

    // ---- palace

    private static void palace(MisthalinKit k) {
        // grounds wall with the south gate
        Box g = t(3199, 3456, 3232, 3500);
        k.floor(g.inset(1), GRASS);
        k.walls(g, -2, 4, WALL);
        for (int x = g.x1(); x <= g.x2(); x += 2) {
            k.set(x, 5, g.z1(), WALL).set(x, 5, g.z2(), WALL);
        }
        for (int z = g.z1(); z <= g.z2(); z += 2) {
            k.set(g.x1(), 5, z, WALL).set(g.x2(), 5, z, WALL);
        }
        k.clear(X(3210), 0, g.z2(), X(3213) + 1, 4, g.z2());
        k.fill(X(3209) + 1, 0, g.z2(), X(3209) + 1, 6, g.z2(), CHISELED).fill(X(3214), 0, g.z2(), X(3214), 6, g.z2(), CHISELED);
        k.set(X(3209) + 1, 7, g.z2(), LANTERN).set(X(3214), 7, g.z2(), LANTERN);
        // palace keep: 3 storeys of 5, crenellated, corner towers
        Box keep = t(3202, 3466, 3223, 3496);
        k.house(keep).storeys(3).height(5).wall(WALL).corner(CHISELED).beam(POLISHED_ANDESITE).base(COBBLE)
                .floor(POLISHED_ANDESITE).upperFloor(SPRUCE_PLANKS).roof(Roof.STONE).crenel(WALL)
                .doorAt('s', X(3211), 4, 4).build();
        int[][] towers = {{keep.x1(), keep.z1()}, {keep.x2(), keep.z1()}, {keep.x1(), keep.z2()}, {keep.x2(), keep.z2()}};
        for (int[] tw : towers) {
            // the palace towers rise two floors above the keep
            k.tower(tw[0], tw[1], 6, 30, 5, WALL, SPRUCE_PLANKS, null, true);
            k.fill(tw[0], 30, tw[1], tw[0], 33, tw[1], "minecraft:spruce_fence");
            k.set(tw[0], 34, tw[1], "minecraft:red_wool").set(tw[0] + 1, 34, tw[1], "minecraft:red_wool");
            k.set(tw[0] + 1, 33, tw[1], "minecraft:yellow_wool").set(tw[0] + 2, 33, tw[1], "minecraft:red_wool");
            k.set(tw[0] + 2, 34, tw[1], "minecraft:yellow_wool");
        }
        palaceHall(k, keep);
        // doors from the keep roof into the towers
        k.clear(keep.x1() + 6, 15, keep.z1() + 2, keep.x1() + 6, 17, keep.z1() + 2);
        k.clear(keep.x2() - 6, 15, keep.z1() + 2, keep.x2() - 6, 17, keep.z1() + 2);
        k.clear(keep.x1() + 6, 15, keep.z2() - 2, keep.x1() + 6, 17, keep.z2() - 2);
        k.clear(keep.x2() - 6, 15, keep.z2() - 2, keep.x2() - 6, 17, keep.z2() - 2);
        Box in = keep.inset(1);
        int mx = X(3211);
        int midZ = Z(3482);
        // ground floor: entrance hall (south) and King Roald's throne room (north)
        k.fill(in.x1(), 0, midZ, in.x2(), 3, midZ, WALL);
        k.clear(mx - 1, 0, midZ, mx + 4, 3, midZ);
        k.fill(mx - 2, 0, midZ, mx - 2, 3, midZ, CHISELED).fill(mx + 5, 0, midZ, mx + 5, 3, midZ, CHISELED);
        k.carpet(new Box(mx, in.z1() + 3, mx + 3, in.z2()), 0, "red");
        k.carpet(new Box(in.x1() + 3, in.z1() + 1, in.x2() - 3, in.z1() + 4), 0, "red");
        k.fill(mx - 2, -1, in.z1(), mx + 5, -1, in.z1() + 3, "minecraft:polished_andesite");
        k.fill(mx - 2, 0, in.z1(), mx + 5, 0, in.z1() + 1, "minecraft:quartz_block");
        k.fill(mx - 2, 0, in.z1() + 2, mx + 5, 0, in.z1() + 2, "minecraft:quartz_stairs[facing=south]");
        k.set(mx + 1, 1, in.z1(), "minecraft:gold_block").set(mx + 2, 1, in.z1(), "minecraft:gold_block");
        k.set(mx + 1, 1, in.z1() + 1, "minecraft:dark_oak_stairs[facing=south]");
        k.set(mx + 2, 1, in.z1() + 1, "minecraft:dark_oak_stairs[facing=south]");
        k.fill(mx + 1, 2, in.z1(), mx + 2, 3, in.z1(), "minecraft:gold_block");
        for (int z = in.z1() + 4; z < midZ; z += 4) {
            k.fill(mx - 3, 0, z, mx - 3, 3, z, "minecraft:quartz_pillar");
            k.fill(mx + 6, 0, z, mx + 6, 3, z, "minecraft:quartz_pillar");
            k.set(mx - 4, 2, z, "minecraft:red_wall_banner[facing=west]");
            k.set(mx + 7, 2, z, "minecraft:red_wall_banner[facing=east]");
            k.hang(mx - 6, 3, z).hang(mx + 9, 3, z);
        }
        // entrance hall: twin staircases up to the first floor (gallery carpet first so the stairs cut it)
        k.carpet(new Box(in.x1() + 6, in.z2() - 8, in.x2() - 6, in.z2() - 1), 5, "red");
        for (int x0 : new int[]{in.x1() + 7, in.x2() - 8}) {
            k.stairs2(x0, 0, in.z2() - 3, 'n', 5, "minecraft:stone_brick_stairs", 'e');
            k.connect(x0 - 1, 5, in.z2() - 7, x0 - 1, 5, in.z2() - 3, "minecraft:spruce_fence");
            k.connect(x0 + 2, 5, in.z2() - 7, x0 + 2, 5, in.z2() - 3, "minecraft:spruce_fence");
            k.connect(x0, 5, in.z2() - 3, x0 + 1, 5, in.z2() - 3, "minecraft:spruce_fence");
            k.clear(x0, 5, in.z2() - 4, x0 + 1, 5, in.z2() - 4);
        }
        k.set(mx - 4, 0, in.z2() - 1, "minecraft:stone_brick_wall").set(mx + 7, 0, in.z2() - 1, "minecraft:stone_brick_wall");
        k.hang(mx + 1, 3, in.z2() - 6).hang(mx + 1, 3, midZ + 6);
        // the guard rooms either side of the hall
        k.fill(mx - 4, 0, midZ + 1, mx - 4, 3, in.z2() - 8, WALL);
        k.fill(mx + 7, 0, midZ + 1, mx + 7, 3, in.z2() - 8, WALL);
        k.clear(mx - 4, 0, midZ + 4, mx - 4, 2, midZ + 5).clear(mx + 7, 0, midZ + 4, mx + 7, 2, midZ + 5);
        k.diningZ(in.x1() + 5, 0, midZ + 3, midZ + 8);
        k.diningZ(in.x2() - 5, 0, midZ + 3, midZ + 8);
        k.set(in.x1(), 0, midZ + 1, "minecraft:grindstone[face=floor,facing=east]").set(in.x2(), 0, midZ + 1, "minecraft:grindstone[face=floor,facing=west]");
        // first floor: Reldo's library (west) and the palace guards (east)
        k.fill(mx, 5, in.z1(), mx, 8, in.z2() - 8, WALL);
        k.clear(mx, 5, midZ, mx, 7, midZ + 1);
        for (int z = in.z1() + 1; z < in.z2() - 9; z += 3) {
            k.fill(in.x1() + 2, 5, z, mx - 3, 6, z, BOOKSHELF);
        }
        k.shelvesZ(in.x1(), 5, in.z1(), in.z2() - 9, "east");
        k.set(mx - 2, 5, midZ + 2, "minecraft:lectern[facing=west]");
        k.table(mx - 2, 5, midZ + 4).chair(mx - 1, 5, midZ + 4, "west");
        for (int x = mx + 3; x < in.x2() - 1; x += 3) {
            k.bed(x, 5, in.z1(), 's', "red");
            k.set(x + 1, 5, in.z1(), "minecraft:chest[facing=south]");
        }
        k.diningX(mx + 4, in.x2() - 4, 5, midZ);
        for (int z = in.z1() + 4; z < in.z2(); z += 8) {
            k.hang(in.x1() + 8, 8, z).hang(in.x2() - 8, 8, z);
        }
        // stairs to the second floor (royal chambers)
        k.stairs2(mx + 1, 5, in.z2() - 9, 'n', 5, "minecraft:spruce_stairs", 'e');
        k.connect(mx, 10, in.z2() - 13, mx, 10, in.z2() - 9, "minecraft:spruce_fence");
        k.connect(mx + 3, 10, in.z2() - 13, mx + 3, 10, in.z2() - 9, "minecraft:spruce_fence");
        k.carpet(new Box(in.x1(), in.z1(), in.x2(), in.z2()), 10, "purple");
        k.clear(mx + 1, 10, in.z2() - 13, mx + 2, 10, in.z2() - 9);
        k.bed(in.x1() + 3, 10, in.z1(), 's', "red").bed(in.x1() + 4, 10, in.z1(), 's', "red");
        k.set(in.x1() + 3, 10, in.z1() + 2, "minecraft:gold_block");
        k.shelvesX(in.x1() + 7, in.x2() - 7, 10, in.z1(), "south");
        k.diningX(in.x1() + 6, in.x1() + 14, 10, in.cz() + 4);
        k.set(in.x2() - 2, 10, in.z1() + 2, "minecraft:enchanting_table");
        k.set(in.x2() - 3, 10, in.z1() + 2, "minecraft:lectern[facing=west]");
        for (int z = in.z1() + 4; z < in.z2(); z += 8) {
            k.hang(in.x1() + 8, 13, z).hang(in.x2() - 8, 13, z);
        }
        k.ladder(in.x1(), keep.z1() + 9, "east", 10, 14);
        // the gardens in front of the palace and the east lawn
        Box front = t(3200, 3457, 3231, 3465);
        k.floor(front, GRASS);
        k.floor(t(3210, 3457, 3213, 3465), "minecraft:smooth_stone");
        for (int x : new int[]{3206, 3214}) {
            k.fill(X(x), 0, Z(3462), X(x) + 7, 0, Z(3462), OAK_LEAVES);
            k.fill(X(x), 0, Z(3458), X(x) + 7, 0, Z(3458), OAK_LEAVES);
            k.fill(X(x), -1, Z(3460), X(x) + 7, -1, Z(3460) + 1, "minecraft:rooted_dirt");
            for (int i = 0; i < 8; i++) {
                String[] fl = {"minecraft:red_tulip", "minecraft:allium", "minecraft:blue_orchid", "minecraft:white_tulip"};
                k.set(X(x) + i, 0, Z(3460) + (i % 2), fl[i % 4]);
            }
        }
        k.fill(X(3201), 0, Z(3459), X(3203) + 1, 0, Z(3459), OAK_LEAVES);
        k.fill(X(3228), 0, Z(3459), X(3230) + 1, 0, Z(3459), OAK_LEAVES);
        k.meadow(t(3224, 3466, 3231, 3499), 5);
        k.discClear(X(3226), Z(3478), 3, 0, 0);
        k.disc(X(3226), Z(3478), 2, -1, -1, WATER);
        k.ring(X(3226), Z(3478), 3, 1, -1, 0, POLISHED_ANDESITE);
        k.tOak(3229, 3470).tOak(3226, 3490).tOak(3229, 3496).tOak(3201, 3498);
    }

    /**
     * The palace's raised great hall on the keep roof: a fourth storey with pilasters, tall windows,
     * a slate roof and a banqueting table, plus buttresses and banners on the keep's faces.
     */
    private static void palaceHall(MisthalinKit k, Box keep) {
        Box hall = new Box(keep.x1() + 12, keep.z1() + 10, keep.x2() - 12, keep.z2() - 10);
        k.walls(hall, 15, 18, WALL);
        k.walls(hall, 15, 15, COBBLE);
        k.fill(hall.inset(1), 18, 18, SPRUCE_PLANKS);
        for (int z = hall.z1(); z <= hall.z2(); z += 4) {
            k.fill(hall.x1(), 15, z, hall.x1(), 18, z, CHISELED).fill(hall.x2(), 15, z, hall.x2(), 18, z, CHISELED);
        }
        for (int x = hall.x1(); x <= hall.x2(); x += 4) {
            k.fill(x, 15, hall.z1(), x, 18, hall.z1(), CHISELED).fill(x, 15, hall.z2(), x, 18, hall.z2(), CHISELED);
        }
        for (int z = hall.z1() + 2; z <= hall.z2() - 2; z += 4) {
            k.connect(hall.x1(), 16, z, hall.x1(), 17, z, GLASS).connect(hall.x2(), 16, z, hall.x2(), 17, z, GLASS);
            k.d.castleWindow(hall.x1(), k.g + 16, k.g + 17, z, 'w', WALL, POLISHED_ANDESITE, k.g + 15);
            k.d.castleWindow(hall.x2(), k.g + 16, k.g + 17, z, 'e', WALL, POLISHED_ANDESITE, k.g + 15);
        }
        k.clear(hall.cx(), 15, hall.z2(), hall.cx() + 1, 17, hall.z2());
        k.set(hall.cx(), 15, hall.z2(), "minecraft:dark_oak_door[facing=south,half=lower,hinge=right]");
        k.set(hall.cx() + 1, 15, hall.z2(), "minecraft:dark_oak_door[facing=south,half=lower,hinge=left]");
        k.d.banner(hall.cx() - 1, k.g + 17, hall.z2() + 1, 's', "red");
        k.d.banner(hall.cx() + 2, k.g + 17, hall.z2() + 1, 's', "red");
        k.roof(hall, 19, Roof.SLATE, WALL);
        k.d.texture(hall.x1(), hall.z1(), hall.x2(), hall.z2(), k.g + 16, k.g + 18, WALL, 6);
        // inside: a red runner, the banqueting table, chandeliers and banners
        Box in = hall.inset(1);
        k.carpet(new Box(in.cx() - 2, in.z1() + 1, in.cx() + 2, in.z2()), 15, "red");
        k.diningZ(in.cx(), 15, in.z1() + 4, in.z2() - 6);
        k.set(in.cx(), 15, in.z1() + 2, "minecraft:dark_oak_stairs[facing=south]");
        k.fill(in.cx() - 1, 15, in.z1(), in.cx() + 1, 16, in.z1(), "minecraft:gold_block");
        for (int z = in.z1() + 3; z <= in.z2() - 3; z += 6) {
            k.hang(in.cx(), 17, z);
            k.set(in.x1(), 16, z, "minecraft:red_wall_banner[facing=east]").set(in.x2(), 16, z, "minecraft:red_wall_banner[facing=west]");
        }
        k.shelvesZ(in.x1(), 15, in.z2() - 4, in.z2() - 1, "east");
        k.set(in.x2(), 15, in.z2() - 2, "minecraft:chest[facing=west]").set(in.x2(), 15, in.z2() - 3, BARREL);
        // buttresses down the long faces of the keep and banners over the south door
        k.d.buttresses(keep.x1(), keep.z1() + 9, keep.x1(), keep.z2() - 9, 'w', 8, k.g, k.g + 12, WALL, "minecraft:stone_brick_stairs");
        k.d.buttresses(keep.x2(), keep.z1() + 9, keep.x2(), keep.z2() - 9, 'e', 8, k.g, k.g + 12, WALL, "minecraft:stone_brick_stairs");
        for (int x = keep.x1() + 9; x <= keep.x2() - 9; x += 6) {
            if (Math.abs(x - X(3211)) > 4) {
                k.d.banner(x, k.g + 8, keep.z2() + 1, 's', "red");
                k.d.banner(x, k.g + 13, keep.z2() + 1, 's', "yellow");
            }
        }
    }

    // ---- banks

    private static void banks(MisthalinKit k) {
        // Varrock West Bank
        Box w = t(3180, 3433, 3191, 3445);
        k.house(w).storeys(2).height(5).wall(STONE_BRICKS).corner(CHISELED).beam(POLISHED_ANDESITE).base(COBBLE)
                .floor(POLISHED_ANDESITE).upperFloor(SPRUCE_PLANKS).roof(Roof.SLATE).doorAt('s', X(3185), 4, 4).build();
        Box wi = w.inset(1);
        k.boothsX(wi.x1() + 1, wi.x2() - 1, 0, wi.z1() + 6);
        k.fill(wi.x1(), 0, wi.z1() + 6, wi.x1(), 1, wi.z1() + 6, POLISHED_ANDESITE).fill(wi.x2(), 0, wi.z1() + 6, wi.x2(), 1, wi.z1() + 6, POLISHED_ANDESITE);
        k.carpet(new Box(wi.x1(), wi.z1() + 7, wi.x2(), wi.z2()), 0, "red");
        for (int x = wi.x1(); x <= wi.x2(); x += 2) {
            k.set(x, 0, wi.z1(), "minecraft:chest[facing=south]");
            k.set(x + 1, 0, wi.z1(), BARREL);
        }
        k.ladder(wi.x2(), wi.z1() + 2, "west", 0, 4);
        k.shelvesX(wi.x1(), wi.x2() - 2, 5, wi.z1(), "south");
        k.diningX(wi.x1() + 3, wi.x2() - 4, 5, wi.cz() + 2);
        k.hang(wi.cx(), 3, wi.z1() + 9).hang(wi.cx(), 3, wi.z1() + 3).hang(wi.cx(), 8, wi.cz());
        sign(k, w, 's', X(3185) - 1, "yellow");
        // Varrock East Bank
        Box e = t(3248, 3413, 3257, 3423);
        k.house(e).storeys(2).height(5).wall(STONE_BRICKS).corner(CHISELED).beam(POLISHED_ANDESITE).base(COBBLE)
                .floor(POLISHED_ANDESITE).upperFloor(SPRUCE_PLANKS).roof(Roof.SLATE).doorAt('w', Z(3418), 4, 4).build();
        Box ei = e.inset(1);
        k.boothsZ(ei.x1() + 6, 0, ei.z1() + 1, ei.z2() - 1);
        k.fill(ei.x1() + 6, 0, ei.z1(), ei.x1() + 6, 1, ei.z1(), POLISHED_ANDESITE).fill(ei.x1() + 6, 0, ei.z2(), ei.x1() + 6, 1, ei.z2(), POLISHED_ANDESITE);
        k.carpet(new Box(ei.x1(), ei.z1(), ei.x1() + 5, ei.z2()), 0, "red");
        for (int z = ei.z1(); z <= ei.z2(); z += 2) {
            k.set(ei.x2(), 0, z, "minecraft:chest[facing=west]");
            k.set(ei.x2(), 0, z + 1, BARREL);
        }
        k.ladder(ei.x2(), ei.z1(), "south", 0, 4);
        k.shelvesX(ei.x1(), ei.x2() - 3, 5, ei.z1(), "south");
        k.diningZ(ei.cx(), 5, ei.z1() + 4, ei.z2() - 3);
        k.hang(ei.x1() + 3, 3, ei.cz()).hang(ei.x2() - 2, 3, ei.cz()).hang(ei.cx(), 8, ei.cz());
        sign(k, e, 'w', Z(3418) - 1, "yellow");
    }

    // ---- shops

    private static void shops(MisthalinKit k) {
        // Aubury's Rune Shop
        Box au = t(3248, 3398, 3255, 3404);
        house(k, au, 2).doorAt('w', Z(3401), 2, 3).build();
        Box ai = au.inset(1);
        k.counterZ(ai.x1() + 4, 0, ai.z1() + 1, ai.z2() - 1, RUNE_SHOP);
        k.shelvesZ(ai.x2(), 0, ai.z1(), ai.z2(), "west");
        k.set(ai.x1(), 0, ai.z1(), "minecraft:enchanting_table");
        k.carpet(new Box(ai.x1(), ai.z1() + 1, ai.x1() + 3, ai.z2()), 0, "purple");
        k.ladder(ai.x2() - 1, ai.z1(), "south", 0, 4);
        k.bed(ai.x1(), 4, ai.z2() - 1, 'n', "purple");
        k.set(ai.x1() + 2, 4, ai.z1(), "minecraft:brewing_stand").set(ai.x1() + 3, 4, ai.z1(), BOOKSHELF);
        k.hang(ai.cx(), 2, ai.cz()).hang(ai.cx(), 6, ai.cz());
        sign(k, au, 'w', Z(3401) - 1, "purple");
        // Zaff's Superior Staffs
        Box za = t(3200, 3439, 3206, 3445);
        house(k, za, 2).doorAt('s', X(3203), 2, 3).build();
        Box zi = za.inset(1);
        k.counterX(zi.x1() + 1, zi.x2() - 1, 0, zi.z1() + 4, "runecraft:weapon_shop");
        for (int x = zi.x1(); x <= zi.x2(); x += 2) {
            k.fill(x, 0, zi.z1(), x, 2, zi.z1(), "minecraft:stripped_spruce_log");
            k.set(x, 3, zi.z1(), "minecraft:end_rod");
        }
        k.ladder(zi.x2(), zi.z1() + 2, "west", 0, 4);
        k.bed(zi.x1(), 4, zi.z1(), 's', "blue");
        k.shelvesX(zi.x1() + 2, zi.x2() - 1, 4, zi.z1(), "south");
        k.hang(zi.cx(), 2, zi.z2() - 1).hang(zi.cx(), 6, zi.cz());
        sign(k, za, 's', X(3203) - 1, "blue");
        // Horvik's Armour Shop (with his anvil)
        Box hv = t(3228, 3433, 3234, 3440);
        house(k, hv, 2).doorAt('s', X(3231), 2, 3).build();
        Box hi = hv.inset(1);
        k.counterX(hi.x1(), hi.x2() - 2, 0, hi.z1() + 5, "runecraft:weapon_shop");
        k.set(hi.x1() + 1, 0, hi.z1() + 2, ANVIL);
        k.set(hi.x2(), 0, hi.z1(), "runecraft:furnace");
        k.set(hi.x1(), 0, hi.z1(), "minecraft:grindstone[face=floor,facing=south]");
        k.set(hi.x2() - 1, 0, hi.z1(), BARREL);
        k.ladder(hi.x2(), hi.z2(), "west", 0, 4);
        k.bed(hi.x1(), 4, hi.z1(), 's', "gray");
        k.hang(hi.cx(), 2, hi.z1() + 3).hang(hi.cx(), 6, hi.cz());
        sign(k, hv, 's', X(3231) - 1, "gray");
        // Lowe's Archery Emporium
        Box lo = t(3229, 3418, 3235, 3424);
        house(k, lo, 1).doorAt('n', X(3232), 2, 3).build();
        Box li = lo.inset(1);
        k.counterX(li.x1() + 1, li.x2() - 1, 0, li.z2() - 3, ARCHERY_SHOP);
        k.set(li.x1(), 0, li.z2(), "minecraft:fletching_table").set(li.x2(), 0, li.z2(), "minecraft:target");
        k.set(li.cx(), 0, li.z2(), BARREL);
        k.hang(li.cx(), 2, li.cz());
        sign(k, lo, 'n', X(3232) - 1, "green");
        // Varrock General Store
        Box gs = t(3214, 3410, 3220, 3417);
        house(k, gs, 1).doorAt('w', Z(3413), 2, 3).build();
        Box gi = gs.inset(1);
        k.counterZ(gi.x1() + 4, 0, gi.z1() + 1, gi.z2() - 1, GENERAL);
        k.shelvesZ(gi.x2(), 0, gi.z1(), gi.z2(), "west");
        k.shelvesX(gi.x1(), gi.x1() + 3, 0, gi.z1(), "south");
        k.hang(gi.cx(), 2, gi.cz());
        sign(k, gs, 'w', Z(3413) - 1, "white");
        // Varrock Swordshop
        Box sw = t(3203, 3395, 3208, 3402);
        house(k, sw, 1).doorAt('e', Z(3398), 2, 3).build();
        Box si = sw.inset(1);
        k.counterZ(si.x2() - 3, 0, si.z1() + 1, si.z2() - 1, WEAPON_SHOP);
        k.set(si.x1(), 0, si.z1(), ANVIL);
        k.set(si.x1(), 0, si.z2(), "minecraft:grindstone[face=floor,facing=east]");
        k.hang(si.cx(), 2, si.cz());
        sign(k, sw, 'e', Z(3398) - 1, "red");
        // Thessalia's Fine Clothes (with the tanning rack)
        Box th = t(3201, 3409, 3208, 3416);
        house(k, th, 2).doorAt('e', Z(3413), 2, 3).build();
        Box ti = th.inset(1);
        k.counterZ(ti.x2() - 3, 0, ti.z1() + 1, ti.z2() - 1, null);
        k.set(ti.x1(), 0, ti.z1() + 2, TANNER).set(ti.x1(), 0, ti.z1() + 4, "minecraft:loom[facing=east]");
        k.fill(ti.x1(), 0, ti.z2() - 1, ti.x1() + 2, 0, ti.z2(), "minecraft:white_wool");
        k.set(ti.x1() + 1, 1, ti.z2(), "minecraft:red_wool").set(ti.x1(), 1, ti.z2() - 1, "minecraft:blue_wool");
        k.ladder(ti.x1(), ti.z1(), "south", 0, 4);
        k.bed(ti.x2(), 4, ti.z1(), 's', "magenta");
        k.carpet(new Box(ti.x1() + 1, ti.z1() + 1, ti.x2() - 1, ti.z2() - 1), 4, "pink");
        k.hang(ti.cx(), 2, ti.cz()).hang(ti.cx(), 6, ti.cz());
        sign(k, th, 'e', Z(3413) - 1, "magenta");
        // west smithy with the two Varrock anvils
        Box sm = t(3184, 3416, 3192, 3425);
        k.house(sm).storeys(1).height(5).wall(COBBLE).corner(STRIPPED_SPRUCE).base(COBBLE).floor(STONE_BRICKS)
                .roof(Roof.SLATE).doorAt('n', X(3188), 4, 4).build();
        Box mi = sm.inset(1);
        k.set(mi.x1() + 3, 0, mi.cz(), ANVIL).set(mi.x2() - 3, 0, mi.cz(), ANVIL);
        k.fill(mi.x1(), 0, mi.z2(), mi.x1() + 3, 3, mi.z2(), "minecraft:bricks");
        k.set(mi.x1() + 1, 0, mi.z2(), "minecraft:blast_furnace[facing=north]");
        k.fill(mi.x1(), 4, mi.z2(), mi.x1() + 1, 8, mi.z2(), "minecraft:bricks");
        k.set(mi.x2(), 0, mi.z2(), "minecraft:water_cauldron[level=3]").set(mi.x2(), 0, mi.z2() - 1, BARREL);
        k.set(mi.x2(), 0, mi.z1() + 2, "minecraft:chest[facing=west]");
        k.hang(mi.cx(), 3, mi.cz());
        // the carpenter (construction workbench)
        Box cp = t(3186, 3395, 3193, 3402);
        house(k, cp, 2).doorAt('n', X(3189), 2, 3).build();
        Box ci = cp.inset(1);
        k.set(ci.cx(), 0, ci.cz(), WORKBENCH);
        k.set(ci.x1(), 0, ci.z2(), "minecraft:stonecutter").set(ci.x1() + 1, 0, ci.z2(), CRAFTING);
        k.fill(ci.x2() - 2, 0, ci.z2(), ci.x2(), 1, ci.z2(), "minecraft:oak_log[axis=x]");
        k.ladder(ci.x2(), ci.z1(), "south", 0, 4);
        k.bed(ci.x1(), 4, ci.z1(), 's', "orange");
        k.hang(ci.cx() + 2, 2, ci.cz()).hang(ci.cx(), 6, ci.cz());
        sign(k, cp, 'n', X(3189) - 1, "orange");
        // the Apothecary
        Box ap = t(3192, 3408, 3199, 3415);
        house(k, ap, 1).doorAt('s', X(3195), 2, 3).build();
        Box pi = ap.inset(1);
        k.counterX(pi.x1() + 1, pi.x2() - 1, 0, pi.z1() + 4, "runecraft:apothecary_table");
        k.set(pi.x1(), 0, pi.z1(), "minecraft:brewing_stand").set(pi.x1() + 1, 0, pi.z1(), "minecraft:cauldron");
        k.shelvesX(pi.x1() + 2, pi.x2(), 0, pi.z1(), "south");
        k.hang(pi.cx(), 2, pi.z2() - 2);
        sign(k, ap, 's', X(3195) - 1, "lime");
    }

    private static void blueMoonInn(MisthalinKit k) {
        // the Blue Moon Inn: bar, tables, kitchen with the range, rooms upstairs
        Box b = t(3215, 3389, 3230, 3402);
        house(k, b, 2).doorAt('w', Z(3396), 2, 3).doorAt('n', X(3226), 2, 3).build();
        Box in = b.inset(1);
        // bar counter
        k.fill(in.x1() + 2, 0, in.z1() + 5, in.x1() + 12, 0, in.z1() + 5, "minecraft:spruce_planks");
        k.fill(in.x1() + 2, 1, in.z1() + 5, in.x1() + 12, 1, in.z1() + 5, "minecraft:spruce_slab");
        k.fill(in.x1() + 2, 0, in.z1(), in.x1() + 12, 0, in.z1(), BARREL);
        k.fill(in.x1() + 2, 1, in.z1(), in.x1() + 12, 1, in.z1(), BARREL);
        k.set(in.x1() + 4, 2, in.z1() + 5, "minecraft:flower_pot").set(in.x1() + 8, 2, in.z1() + 5, "minecraft:candle[candles=2,lit=true]");
        // tables
        for (int x = in.x1() + 3; x <= in.x2() - 3; x += 5) {
            for (int z = in.z1() + 9; z <= in.z2() - 2; z += 5) {
                k.table(x, 0, z).chair(x - 1, 0, z, "east").chair(x + 1, 0, z, "west").chair(x, 0, z - 1, "south").chair(x, 0, z + 1, "north");
            }
        }
        k.fill(in.x1(), 0, in.z2() - 3, in.x1(), 2, in.z2() - 1, "minecraft:bricks");
        k.set(in.x1(), 0, in.z2() - 2, "minecraft:campfire[lit=true]");
        k.set(in.x1(), 3, in.z2() - 2, "minecraft:bricks");
        k.set(in.x2(), 2, in.cz(), "minecraft:blue_wall_banner[facing=west]");
        // kitchen in the north-east corner
        k.fill(in.x2() - 7, 0, in.z1(), in.x2() - 7, 3, in.z1() + 6, SPRUCE_PLANKS);
        k.clear(in.x2() - 7, 0, in.z1() + 5, in.x2() - 7, 2, in.z1() + 5);
        k.set(in.x2(), 0, in.z1(), RANGE).set(in.x2() - 1, 0, in.z1(), "minecraft:smoker[facing=south]");
        k.set(in.x2() - 2, 0, in.z1(), "minecraft:cauldron").set(in.x2(), 0, in.z1() + 1, BARREL);
        k.table(in.x2() - 3, 0, in.z1() + 3);
        for (int x = in.x1() + 3; x <= in.x2(); x += 5) {
            k.hang(x, 2, in.z1() + 3).hang(x, 2, in.z2() - 3);
        }
        // rooms upstairs
        k.stairs2(in.x1() + 1, 0, in.z2() - 6, 'n', 4, "minecraft:spruce_stairs", 'e');
        k.connect(in.x1() + 3, 4, in.z2() - 9, in.x1() + 3, 4, in.z2() - 6, "minecraft:spruce_fence");
        k.carpet(new Box(in.x1(), in.cz() - 1, in.x2(), in.cz() + 1), 4, "blue");
        for (int x = in.x1() + 5; x < in.x2() - 1; x += 6) {
            k.fill(x, 4, in.z1(), x, 6, in.cz() - 2, SPRUCE_PLANKS);
            k.fill(x, 4, in.cz() + 2, x, 6, in.z2(), SPRUCE_PLANKS);
            k.bed(x - 2, 4, in.z1(), 's', "blue");
            k.bed(x - 2, 4, in.z2(), 'n', "blue");
            k.set(x - 4, 4, in.z1(), "minecraft:chest[facing=south]");
            k.hang(x - 3, 6, in.z1() + 2).hang(x - 3, 6, in.z2() - 2);
        }
        sign(k, b, 'w', Z(3396) - 1, "blue");
    }

    private static void church(MisthalinKit k) {
        // Varrock church (Saradomin), north-east of the city
        Box b = t(3250, 3472, 3261, 3490);
        k.house(b).storeys(1).height(9).wall(STONE_BRICKS).corner(CHISELED).base(COBBLE).floor(POLISHED_ANDESITE)
                .roof(Roof.SLATE).window(null).doorAt('w', Z(3480), 4, 5).build();
        Box in = b.inset(1);
        for (int z = in.z1() + 2; z <= in.z2() - 1; z += 4) {
            k.connect(b.x2(), 2, z, b.x2(), 6, z, "minecraft:blue_stained_glass_pane");
            if (Math.abs(z - Z(3480)) > 3) {
                k.connect(b.x1(), 2, z, b.x1(), 6, z, "minecraft:yellow_stained_glass_pane");
            }
        }
        k.connect(in.cx() - 1, 3, b.z1(), in.cx() + 1, 7, b.z1(), "minecraft:white_stained_glass_pane");
        k.fill(in.x1(), -1, in.z1(), in.x2(), -1, in.z1() + 4, "minecraft:quartz_block");
        k.fill(in.x1(), 0, in.z1() + 4, in.x2(), 0, in.z1() + 4, "minecraft:quartz_slab");
        k.clear(in.cx() - 1, 0, in.z1() + 4, in.cx() + 1, 0, in.z1() + 4);
        k.set(in.cx(), 0, in.z1() + 1, ALTAR);
        k.set(in.cx() - 2, 0, in.z1(), "minecraft:gold_block").set(in.cx() + 2, 0, in.z1(), "minecraft:gold_block");
        k.set(in.cx() - 2, 1, in.z1(), LANTERN).set(in.cx() + 2, 1, in.z1(), LANTERN);
        k.carpet(new Box(in.cx() - 1, in.z1() + 5, in.cx() + 1, in.z2()), 0, "blue");
        for (int z = in.z1() + 7; z <= in.z2() - 2; z += 2) {
            k.fill(in.x1() + 1, 0, z, in.cx() - 3, 0, z, "minecraft:spruce_stairs[facing=north]");
            k.fill(in.cx() + 3, 0, z, in.x2() - 1, 0, z, "minecraft:spruce_stairs[facing=north]");
        }
        for (int z = in.z1() + 3; z <= in.z2(); z += 6) {
            k.hang(in.cx() - 3, 7, z).hang(in.cx() + 3, 7, z);
        }
        // steeple over the entrance end
        Box st = new Box(in.cx() - 2, b.z2() - 4, in.cx() + 2, b.z2());
        k.walls(st, 0, 16, STONE_BRICKS);
        k.clear(st.inset(1), 0, 16);
        k.clear(st.cx(), 0, st.z1(), st.cx(), 2, st.z1());
        k.clear(st.x1(), 13, st.cz(), st.x1(), 15, st.cz()).clear(st.x2(), 13, st.cz(), st.x2(), 15, st.cz());
        k.clear(st.cx(), 13, st.z1(), st.cx(), 15, st.z1()).clear(st.cx(), 13, st.z2(), st.cx(), 15, st.z2());
        k.fill(st, 17, 17, STONE_BRICKS);
        k.set(st.cx(), 16, st.cz(), "minecraft:bell[attachment=ceiling,facing=north]");
        for (int i = 0; i < 3; i++) {
            k.fill(st.x1() + i, 18 + i, st.z1() + i, st.x2() - i, 18 + i, st.z2() - i, "minecraft:deepslate_tiles");
        }
        k.fill(st.cx(), 21, st.cz(), st.cx(), 23, st.cz(), "minecraft:deepslate_tiles");
        k.set(st.cx(), 24, st.cz(), "minecraft:gold_block");
        k.ladder(st.cx(), st.z2() - 1, "north", 0, 12);
        k.fill(st.inset(1), 12, 12, SPRUCE_PLANKS);
        k.ladder(st.cx(), st.z2() - 1, "north", 0, 12);
        k.tPath(3247, 3481, 3249, 3481, 2);
        // graveyard
        Box gy = t(3263, 3472, 3272, 3490);
        k.floor(gy, COARSE_DIRT);
        k.scatter(gy, -1, GRASS, 2);
        k.connect(gy.x1(), 0, gy.z1(), gy.x1(), 0, gy.z2(), "minecraft:cobblestone_wall");
        k.connect(gy.x1(), 0, gy.z1(), gy.x2(), 0, gy.z1(), "minecraft:cobblestone_wall");
        k.connect(gy.x1(), 0, gy.z2(), gy.x2(), 0, gy.z2(), "minecraft:cobblestone_wall");
        k.clear(gy.x1(), 0, gy.cz(), gy.x1(), 0, gy.cz() + 1);
        for (int x = gy.x1() + 3; x < gy.x2(); x += 4) {
            for (int z = gy.z1() + 3; z < gy.z2() - 1; z += 4) {
                k.grave(x, z);
            }
        }
    }

    private static void museum(MisthalinKit k) {
        // Varrock Museum: two storeys of display cases, the dig-site crates out back
        Box b = t(3252, 3440, 3268, 3455);
        k.house(b).storeys(2).height(5).wall("minecraft:smooth_sandstone").corner("minecraft:cut_sandstone")
                .beam("minecraft:cut_sandstone").base("minecraft:sandstone").floor("minecraft:smooth_quartz")
                .upperFloor(SPRUCE_PLANKS).roof(Roof.SLATE).gable("minecraft:smooth_sandstone")
                .doorAt('w', Z(3448), 4, 4).build();
        Box in = b.inset(1);
        k.fill(b.x1() - 1, 0, Z(3448) - 1, b.x1() - 1, 4, Z(3448) - 1, "minecraft:quartz_pillar");
        k.fill(b.x1() - 1, 0, Z(3447) + 2, b.x1() - 1, 4, Z(3447) + 2, "minecraft:quartz_pillar");
        k.tPath(3247, 3448, 3251, 3448, 2);
        // display cases: glass over exhibits
        String[] exhibits = {"minecraft:bone_block", "minecraft:skeleton_skull", "minecraft:gold_block", "minecraft:amethyst_cluster",
                "minecraft:decorated_pot", "minecraft:chiseled_sandstone", "minecraft:emerald_block", "minecraft:diamond_block"};
        int i = 0;
        for (int x = in.x1() + 3; x <= in.x2() - 3; x += 5) {
            for (int z = in.z1() + 3; z <= in.z2() - 3; z += 6) {
                k.set(x, 0, z, "minecraft:smooth_quartz");
                k.set(x, 1, z, exhibits[i % exhibits.length]);
                k.set(x, 2, z, "minecraft:glass");
                i++;
            }
        }
        k.fill(in.x1() + 8, 0, in.cz() - 1, in.x1() + 12, 0, in.cz() + 1, "minecraft:bone_block[axis=x]");
        k.set(in.x1() + 13, 1, in.cz(), "minecraft:skeleton_skull");
        k.shelvesX(in.x1() + 2, in.x2() - 2, 0, in.z1(), "south");
        k.stairs2(in.x2() - 2, 0, in.z2() - 1, 'n', 5, "minecraft:spruce_stairs", 'e');
        k.connect(in.x2() - 3, 5, in.z2() - 5, in.x2() - 3, 5, in.z2() - 1, "minecraft:spruce_fence");
        // natural history floor upstairs
        k.carpet(new Box(in.x1(), in.z1(), in.x2() - 4, in.z2()), 5, "green");
        for (int x = in.x1() + 2; x <= in.x2() - 6; x += 4) {
            k.set(x, 5, in.z1() + 2, "minecraft:moss_block");
            k.set(x, 6, in.z1() + 2, "minecraft:azalea");
            k.set(x, 5, in.z2() - 2, "minecraft:bone_block");
            k.set(x, 6, in.z2() - 2, "minecraft:skeleton_skull");
        }
        for (int x = in.x1() + 3; x <= in.x2(); x += 6) {
            k.hang(x, 3, in.cz()).hang(x, 8, in.cz());
        }
        // dig site crates behind
        Box yard = t(3253, 3436, 3268, 3439);
        k.floor(yard, COARSE_DIRT);
        for (int x = yard.x1() + 1; x < yard.x2(); x += 4) {
            k.crates(x, yard.cz());
        }
        k.set(yard.x2() - 1, 0, yard.z1(), "minecraft:suspicious_sand");
    }

    // ---- homes

    private static void homes(MisthalinKit k) {
        int[][] hs = {
                // x1, y1, x2, y2, storeys, door side
                {3177, 3386, 3183, 3392, 2, 'n'}, {3195, 3386, 3201, 3392, 1, 'n'}, {3177, 3409, 3183, 3415, 2, 's'},
                {3177, 3417, 3182, 3424, 1, 'e'}, {3180, 3450, 3187, 3457, 2, 's'}, {3193, 3450, 3199, 3457, 2, 's'},
                {3222, 3409, 3228, 3415, 2, 'n'}, {3233, 3388, 3241, 3395, 2, 'e'},
                {3233, 3409, 3241, 3415, 2, 'e'}, {3259, 3386, 3266, 3393, 1, 'n'}, {3259, 3396, 3265, 3402, 2, 'w'},
                {3260, 3415, 3266, 3422, 2, 'n'}, {3236, 3433, 3242, 3440, 2, 's'}, {3236, 3446, 3242, 3453, 1, 'e'},
                {3256, 3458, 3264, 3464, 2, 'n'}, {3236, 3478, 3242, 3486, 2, 'e'}, {3236, 3490, 3242, 3500, 2, 'e'},
                {3252, 3494, 3260, 3500, 2, 'w'}, {3264, 3494, 3271, 3501, 1, 'w'}, {3196, 3375 + 100, 3197, 3375 + 100, 1, 'n'}};
        String[] colours = {"red", "blue", "green", "yellow", "orange", "cyan", "brown", "lime"};
        int i = 0;
        for (int[] h : hs) {
            Box b = t(h[0], h[1], h[2], h[3]);
            if (b.w() < 8 || b.d() < 8) {
                continue;
            }
            char side = (char) h[5];
            MisthalinKit.House hh = house(k, b, h[4]);
            hh.doorAt(side, side == 'n' || side == 's' ? b.cx() : b.cz(), 2, 3).build();
            k.furnishHome(hh, side, colours[i % colours.length]);
            i++;
        }
    }

    // ------------------------------------------------------------------ the Grand Exchange

    static StructureDef grandExchange() {
        MisthalinKit k = new MisthalinKit("grand_exchange", "Grand Exchange", "grand_exchange");
        int cx = X(3165);
        int cz = Z(3489);
        k.tFlatten(3147, 3473, 3183, 3507);
        // the plaza: concentric rings of paving with four avenues
        k.disc(cx, cz, 30, -1, -1, POLISHED_ANDESITE);
        k.ring(cx, cz, 30, 2, -1, -1, STONE_BRICKS);
        k.ring(cx, cz, 23, 1, -1, -1, CHISELED);
        k.ring(cx, cz, 16, 1, -1, -1, "minecraft:smooth_stone");
        k.fill(cx - 2, -1, cz - 32, cx + 2, -1, cz + 32, "minecraft:smooth_stone");
        k.fill(cx - 32, -1, cz - 2, cx + 32, -1, cz + 2, "minecraft:smooth_stone");
        // lawns, trees and benches between the avenues
        for (int[] q : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
            int qx = cx + q[0] * 20;
            int qz = cz + q[1] * 20;
            k.disc(qx, qz, 4, -1, -1, GRASS);
            k.oak(qx, qz);
            k.set(qx - 3 * q[0], 0, qz - 3 * q[1], "minecraft:spruce_stairs[facing=" + (q[0] > 0 ? "east" : "west") + "]");
            k.lampPost(cx + q[0] * 14, cz + q[1] * 14);
        }
        // eight pillars carrying a domed green canopy
        int R = 13;
        for (int a = 0; a < 8; a++) {
            double ang = Math.PI / 8 + a * Math.PI / 4;
            int px = cx + (int) Math.round(Math.cos(ang) * R);
            int pz = cz + (int) Math.round(Math.sin(ang) * R);
            k.fill(px, 0, pz, px, 8, pz, "minecraft:quartz_pillar");
            k.set(px, 0, pz, "minecraft:chiseled_quartz_block");
            k.set(px, 8, pz, "minecraft:chiseled_quartz_block");
        }
        k.ring(cx, cz, R + 2, 3, 9, 9, "minecraft:smooth_quartz");
        k.ring(cx, cz, R + 2, 1, 10, 10, "minecraft:smooth_quartz_slab");
        k.ring(cx, cz, R - 1, 3, 10, 10, "minecraft:green_terracotta");
        k.ring(cx, cz, R - 4, 3, 11, 11, "minecraft:green_terracotta");
        k.ring(cx, cz, R - 7, 3, 12, 12, "minecraft:green_terracotta");
        k.disc(cx, cz, R - 10, 13, 13, "minecraft:green_terracotta");
        k.set(cx, 14, cz, "minecraft:gold_block");
        k.set(cx, 15, cz, "minecraft:lightning_rod");
        // the central counter ring with the exchange booths, open at the four avenues
        k.ring(cx, cz, 7, 1, 0, 0, "minecraft:spruce_planks");
        k.ring(cx, cz, 7, 1, 1, 1, "minecraft:spruce_slab");
        k.clear(cx - 1, 0, cz - 8, cx + 1, 1, cz + 8);
        k.clear(cx - 8, 0, cz - 1, cx + 8, 1, cz + 1);
        int[][] booths = {{-5, -4}, {5, -4}, {-5, 4}, {5, 4}, {-4, -5}, {4, -5}, {-4, 5}, {4, 5}};
        for (int[] bo : booths) {
            k.set(cx + bo[0], 0, cz + bo[1], "runecraft:grand_exchange_booth");
        }
        // centre: the clerks' platform and statue
        k.disc(cx, cz, 3, -1, -1, "minecraft:smooth_quartz");
        k.disc(cx, cz, 1, 0, 0, "minecraft:quartz_block");
        k.fill(cx, 1, cz, cx, 3, cz, "minecraft:quartz_pillar");
        k.set(cx, 4, cz, "minecraft:gold_block");
        k.hang(cx + 9, 8, cz).hang(cx - 9, 8, cz).hang(cx, 8, cz + 9).hang(cx, 8, cz - 9);
        // bank booths on the north-east and south-west, cape stand and diary board
        k.fill(cx + 15, -1, cz - 19, cx + 23, -1, cz - 16, POLISHED_ANDESITE);
        k.fill(cx - 23, -1, cz + 16, cx - 15, -1, cz + 19, POLISHED_ANDESITE);
        k.boothsX(cx + 16, cx + 22, 0, cz - 17);
        k.boothsX(cx - 22, cx - 16, 0, cz + 17);
        k.set(cx + 18, 0, cz + 18, "runecraft:skillcape_stand");
        k.set(cx - 18, 0, cz - 18, "runecraft:achievement_diary_board");
        // low wall around the plaza with four gateways
        k.ring(cx, cz, 32, 1, 0, 0, "minecraft:stone_brick_wall");
        k.ring(cx, cz, 32, 1, 0, 0, "minecraft:stone_brick_wall");
        k.clear(cx - 3, 0, cz - 33, cx + 3, 0, cz + 33);
        k.clear(cx - 33, 0, cz - 3, cx + 33, 0, cz + 3);
        // road south to the Varrock west gate road, and east to the postern
        k.tFlatten(3162, 3431, 3167, 3472);
        k.tStreet(3164, 3473, 3164, 3431, 2);
        k.tStreet(3181, 3470, 3190, 3470, 2);
        k.tStreet(3189, 3470, 3189, 3467, 2);
        return k.build();
    }

    // ------------------------------------------------------------------ outside the walls

    static StructureDef outskirts() {
        MisthalinKit k = new MisthalinKit("varrock_outskirts", "Varrock outskirts");
        cooksGuild(k);
        gertrude(k);
        juliet(k);
        eastMine(k);
        lumberYard(k);
        return k.build();
    }

    private static void cooksGuild(MisthalinKit k) {
        // Cooks' Guild west of Varrock: tall stone hall with a chimney, ranges on every floor
        k.tFlatten(3134, 3434, 3154, 3454);
        Box b = t(3138, 3439, 3148, 3448);
        k.house(b).storeys(3).height(5).wall(STONE_BRICKS).corner(CHISELED).beam(POLISHED_ANDESITE).base(COBBLE)
                .floor("minecraft:bricks").upperFloor(SPRUCE_PLANKS).roof(Roof.BRICK).gable(STONE_BRICKS)
                .doorAt('e', Z(3444), 2, 3).build();
        Box in = b.inset(1);
        for (int s = 0; s < 3; s++) {
            int y = s * 5;
            k.set(in.x1(), y, in.z1(), RANGE).set(in.x1() + 1, y, in.z1(), RANGE);
            k.fill(in.x1() + 2, y, in.z1(), in.x1() + 6, y, in.z1(), "minecraft:spruce_planks");
            k.fill(in.x1() + 2, y + 1, in.z1(), in.x1() + 6, y + 1, in.z1(), "minecraft:spruce_slab");
            k.set(in.x1() + 3, y + 1, in.z1(), "minecraft:cauldron");
            k.set(in.x2(), y, in.z2(), BARREL).set(in.x2() - 1, y, in.z2(), "minecraft:chest[facing=north]");
            k.diningX(in.x1() + 4, in.x1() + 8, y, in.cz() + 2);
            k.hang(in.cx(), y + 3, in.cz());
        }
        k.ladder(in.x2(), in.z1(), "south", 0, 9);
        k.set(in.x1(), 5, in.z2(), "minecraft:smoker[facing=east]");
        k.set(in.x1(), 10, in.z2(), "runecraft:cooking_range");
        // chimney stack
        k.fill(b.x1() + 1, 15, b.z1() + 1, b.x1() + 2, 22, b.z1() + 2, "minecraft:bricks");
        k.set(b.x1() + 1, 23, b.z1() + 1, "minecraft:campfire[lit=true]");
        k.fill(b.x2() + 1, 0, Z(3445), b.x2() + 1, 2, Z(3445), "minecraft:white_wool");
        k.tPath(3149, 3444, 3165, 3444, 2);
        k.field(t(3136, 3436, 3141, 3437), "minecraft:wheat[age=7]");
    }

    private static void gertrude(MisthalinKit k) {
        // Gertrude's house (the lost kitten), garden fence and a cat-friendly crate pile
        k.tFlatten(3146, 3404, 3162, 3418);
        Box b = t(3149, 3407, 3156, 3413);
        MisthalinKit.House h = house(k, b, 1).doorAt('e', Z(3410), 2, 3);
        h.build();
        k.furnishHome(h, 'e', "pink");
        k.fenceRing(t(3147, 3405, 3161, 3416), "minecraft:oak_fence");
        k.clear(X(3161) + 1, 0, Z(3410), X(3161) + 1, 0, Z(3410) + 1);
        k.crates(X(3159), Z(3407));
        k.meadow(t(3157, 3406, 3160, 3415), 3);
        k.tPath(3162, 3410, 3172, 3410, 2);
    }

    private static void juliet(MisthalinKit k) {
        // Juliet's house with the famous balcony (Romeo & Juliet)
        k.tFlatten(3154, 3424, 3166, 3438);
        Box b = t(3155, 3426, 3163, 3436);
        MisthalinKit.House h = house(k, b, 2).doorAt('e', Z(3431), 2, 3);
        h.build();
        k.furnishHome(h, 'e', "red");
        // the balcony on the east side of the upper floor
        k.fill(b.x2() + 1, 4, Z(3433), b.x2() + 2, 4, Z(3429) + 1, "minecraft:spruce_planks");
        k.connect(b.x2() + 3, 5, Z(3433), b.x2() + 3, 5, Z(3429) + 1, "minecraft:spruce_fence");
        k.clear(b.x2(), 5, Z(3431), b.x2(), 6, Z(3431) + 1);
        k.fill(b.x2() + 1, 0, Z(3433), b.x2() + 1, 3, Z(3433), "minecraft:spruce_fence");
        k.fill(b.x2() + 1, 0, Z(3429) + 1, b.x2() + 1, 3, Z(3429) + 1, "minecraft:spruce_fence");
        k.set(b.x2() + 3, 6, Z(3431), "minecraft:potted_red_tulip");
        k.tPath(3166, 3430, 3166, 3431, 2);
    }

    /**
     * The Champions' Guild south of Varrock, traced from the OSRS map (mejrs layers_osrs, planes 0-1): a stone
     * hall on tiles 3188-3194, 3352-3362 with its door north (3191, 3362), a side door east (3194, 3357),
     * the dividing wall at y 3354 (door at 3192) with the staircase in the south-west corner (3188, 3354), a
     * walled rose garden to the east (3195-3198, 3352-3359) and the fenced grounds with the gap for the path
     * north. Its own structure so the champions_guild place builds no plaza over the door.
     */
    static StructureDef championsGuild() {
        MisthalinKit k = new MisthalinKit("champions_guild", "Champions' Guild", "champions_guild");
        k.tFlatten(3180, 3346, 3204, 3370);
        Box b = t(3188, 3352, 3194, 3362);
        k.house(b).storeys(2).height(5).wall(STONE_BRICKS).corner(CHISELED).beam(POLISHED_ANDESITE).base(COBBLE)
                .floor(POLISHED_ANDESITE).upperFloor(SPRUCE_PLANKS).roof(Roof.SLATE).gable(STONE_BRICKS)
                .doorAt('n', X(3191), 2, 3).door('e', 3357).build();
        Box in = b.inset(1);
        // the dividing wall at y 3354 with its door (3192), the pillar (3190, 3355)
        int wz = Z(3354);
        k.fill(in.x1(), 0, wz, in.x2(), 3, wz, STONE_BRICKS).clear(X(3192), 0, wz, X(3192) + 1, 2, wz);
        k.fill(X(3190), 0, Z(3355) + 1, X(3190) + 1, 3, Z(3355) + 1, CHISELED);
        // the staircase (3188, 3354-3355) as a ladder against the west wall
        k.ladder(in.x1(), Z(3355), "east", 0, 4).ladder(in.x1(), Z(3355) + 1, "east", 0, 4);
        // the hall: red carpet from the door, banners, the weapon counter and the grindstone
        k.carpet(new Box(X(3191), in.z1(), X(3191) + 1, Z(3356)), 0, "red");
        for (int z = in.z1() + 2; z < Z(3356); z += 5) {
            k.set(in.x1(), 2, z, "minecraft:yellow_wall_banner[facing=east]");
            k.set(in.x2(), 2, z, "minecraft:yellow_wall_banner[facing=west]");
        }
        k.counterX(in.x1() + 1, in.x1() + 4, 0, Z(3359), "runecraft:weapon_shop");
        k.set(in.x2(), 0, in.z1(), "minecraft:grindstone[face=floor,facing=west]");
        k.set(in.x2(), 0, Z(3353), "minecraft:chest[facing=west]").set(in.x2() - 1, 0, Z(3353), BARREL);
        k.hang(in.cx(), 3, Z(3359)).hang(in.cx(), 3, Z(3353));
        // first floor: the champions' trophies and the dining table
        k.diningZ(in.cx(), 5, Z(3361), Z(3356));
        k.shelvesX(in.x1() + 2, in.x2(), 5, in.z2(), "north");
        k.set(in.x2(), 5, in.z1(), "minecraft:skeleton_skull").set(in.x2(), 5, in.z1() + 2, "minecraft:skeleton_skull");
        k.hang(in.cx(), 8, Z(3359));
        // the walled rose garden east of the hall
        Box garden = t(3195, 3352, 3198, 3359);
        k.floor(garden, "minecraft:podzol");
        k.walls(garden, 0, 1, "minecraft:stone_brick_wall");
        k.clear(garden.x1(), 0, Z(3357), garden.x1(), 1, Z(3357) + 1);
        for (int z = garden.z1() + 2; z < garden.z2() - 1; z += 3) {
            k.set(garden.x1() + 2, 0, z, "minecraft:rose_bush[half=lower]").set(garden.x1() + 5, 0, z, "minecraft:rose_bush[half=lower]");
        }
        // the grounds' fence: west side, the angled corners and the north side with the path gap (3190-3192)
        String fence = "minecraft:spruce_fence";
        k.fence(X(3185), Z(3360), X(3185), Z(3352) + 1, fence);
        for (int i = 0; i < 4; i++) {
            k.fence(X(3185) + i, Z(3361) - i, X(3185) + i, Z(3361) - i, fence);
            k.fence(X(3198) + 1 - i, Z(3361) - i, X(3198) + 1 - i, Z(3361) - i, fence);
        }
        k.fence(X(3187), Z(3364) + 1, X(3189) + 1, Z(3364) + 1, fence);
        k.fence(X(3193), Z(3364) + 1, X(3195) + 1, Z(3364) + 1, fence);
        k.fence(X(3198) + 1, Z(3360), X(3198) + 1, Z(3360) + 1, fence);
        k.tPath(3191, 3363, 3191, 3383, 2);
        return k.build();
    }

    private static void eastMine(MisthalinKit k) {
        // Varrock east mine: a rocky hollow with copper, tin and iron
        k.tFlatten(3278, 3358, 3294, 3374);
        Box m = t(3280, 3360, 3292, 3372);
        k.floor(m, GRAVEL);
        k.scatter(m, -1, COBBLE, 3);
        k.scatter(m, -1, ANDESITE, 4);
        String[] rocks = {"runecraft:copper_rock", "runecraft:tin_rock", "runecraft:iron_rock"};
        int i = 0;
        for (int x = m.x1() + 2; x < m.x2() - 1; x += 4) {
            for (int z = m.z1() + 2; z < m.z2() - 1; z += 4) {
                k.set(x, 0, z, rocks[i % 3]);
                k.set(x + 1, 0, z, "minecraft:cobblestone");
                i++;
            }
        }
        // rocky outcrop around the edge
        k.walls(m.grow(1), 0, 1, STONE);
        k.walls(m.grow(2), 0, 3, STONE);
        k.walls(m.grow(3), 0, 2, "minecraft:andesite");
        k.clear(m.cx() - 1, 0, m.z1() - 3, m.cx() + 1, 3, m.z1() - 1);
        k.scatter(m.grow(2), 4, STONE, 20);
        k.set(m.x1(), 0, m.z2(), "minecraft:rail");
        k.tPath(3286, 3374, 3286, 3383, 2);
    }

    private static void lumberYard(MisthalinKit k) {
        // the lumber yard north-east of Varrock: fenced yard, log piles, the sawmill shed
        k.tFlatten(3292, 3480, 3318, 3504);
        Box y = t(3294, 3482, 3316, 3502);
        k.floor(y, COARSE_DIRT);
        k.scatter(y, -1, DIRT_PATH, 4);
        k.fenceRing(y, "minecraft:spruce_fence");
        k.clear(y.x1(), 0, Z(3492), y.x1(), 0, Z(3491) + 1);
        for (int z = y.z1() + 3; z < y.z2() - 8; z += 5) {
            k.fill(y.x1() + 3, 0, z, y.x1() + 12, 1, z + 2, "minecraft:oak_log[axis=x]");
            k.fill(y.x1() + 4, 2, z + 1, y.x1() + 11, 2, z + 1, "minecraft:oak_log[axis=x]");
        }
        k.fill(y.x2() - 12, 0, y.z2() - 5, y.x2() - 3, 0, y.z2() - 3, "minecraft:spruce_log[axis=x]");
        Box shed = t(3307, 3484, 3315, 3490);
        k.house(shed).storeys(1).height(5).wall(SPRUCE_PLANKS).corner(SPRUCE_LOG).floor(SPRUCE_PLANKS)
                .roof(Roof.SPRUCE).doorAt('n', shed.cx(), 3, 3).build();
        Box si = shed.inset(1);
        k.set(si.cx(), 0, si.cz(), WORKBENCH);
        k.set(si.x1(), 0, si.z2(), "minecraft:stonecutter").set(si.x2(), 0, si.z2(), CRAFTING);
        k.fill(si.x1(), 0, si.z1(), si.x1() + 2, 0, si.z1(), "minecraft:stripped_oak_log[axis=x]");
        k.hang(si.cx(), 3, si.cz());
        k.tOak(3300, 3478).tOak(3318, 3482);
        k.tPath(3284, 3491, 3293, 3491, 2);
    }

    // ------------------------------------------------------------------ small place structures

    static StructureDef teaStall() {
        MisthalinKit k = new MisthalinKit("varrock_tea_stall", "Varrock tea stall", "varrock_tea_stall");
        int x = X(3270);
        int z = Z(3410);
        k.street(new Box(x - 4, z - 4, x + 4, z + 3));
        k.stall(x, z, "runecraft:tea_stall", "light_blue");
        k.set(x + 3, 0, z + 2, "minecraft:spruce_stairs[facing=west]");
        return k.build();
    }

    static StructureDef sewers() {
        // the Varrock sewers manhole east of the palace
        MisthalinKit k = new MisthalinKit("varrock_sewers", "Varrock Sewers manhole", "varrock_sewers");
        int x = X(3237);
        int z = Z(3458);
        k.fill(x - 2, -1, z - 2, x + 2, -1, z + 2, COBBLE);
        k.fill(x - 4, -12, z - 3, x + 8, -2, z + 3, "minecraft:mossy_stone_bricks");
        k.clear(x - 3, -11, z - 2, x + 7, -8, z + 2);
        k.fill(x - 3, -12, z, x + 7, -12, z, WATER);
        k.fill(x - 3, -11, z, x + 7, -11, z, AIR);
        k.clear(x, -7, z, x, -2, z);
        k.ladder(x, z, "north", -11, -2);
        k.set(x, -1, z, "minecraft:iron_trapdoor[half=top]");
        k.set(x - 2, -8, z - 2, LANTERN_HANG).set(x + 5, -8, z + 2, LANTERN_HANG);
        k.set(x + 6, -11, z - 2, "minecraft:cobweb").set(x - 3, -11, z + 2, BARREL);
        return k.build();
    }

    static StructureDef treePatch() {
        MisthalinKit k = new MisthalinKit("varrock_tree_patch", "Varrock tree patch", "varrock_tree_patch");
        int x = X(3229);
        int z = Z(3459);
        k.fill(x - 2, -1, z - 2, x + 2, -1, z + 2, COARSE_DIRT);
        k.connect(x - 3, 0, z - 3, x + 3, 0, z - 3, "minecraft:oak_fence");
        k.connect(x - 3, 0, z - 3, x - 3, 0, z + 3, "minecraft:oak_fence");
        k.connect(x - 3, 0, z + 3, x + 3, 0, z + 3, "minecraft:oak_fence");
        k.set(x, 0, z, "runecraft:tree_patch");
        return k.build();
    }

    static StructureDef championsBush() {
        MisthalinKit k = new MisthalinKit("champions_guild_bush", "Champions' Guild bush patch", "champions_guild_bush");
        int x = X(3181);
        int z = Z(3357);
        k.fill(x - 1, -1, z - 1, x + 2, -1, z + 2, COARSE_DIRT);
        k.connect(x - 2, 0, z - 2, x + 3, 0, z - 2, "minecraft:spruce_fence");
        k.connect(x - 2, 0, z - 2, x - 2, 0, z + 3, "minecraft:spruce_fence");
        k.set(x, 0, z, "runecraft:bush_patch");
        return k.build();
    }

    static StructureDef stoneCircle() {
        // the stone circle south of Varrock
        MisthalinKit k = new MisthalinKit("stone_circle", "Stone circle", "STONE_CIRCLE");
        int cx = X(3227);
        int cz = Z(3369);
        k.tFlatten(3219, 3361, 3233, 3375);
        k.disc(cx, cz, 11, -1, -1, "minecraft:moss_block");
        k.scatter(new Box(cx - 11, cz - 11, cx + 11, cz + 11), -1, GRASS, 2);
        for (int a = 0; a < 10; a++) {
            double ang = a * Math.PI / 5;
            int px = cx + (int) Math.round(Math.cos(ang) * 9);
            int pz = cz + (int) Math.round(Math.sin(ang) * 9);
            k.fill(px, -1, pz, px, 2 + (a % 3 == 0 ? 1 : 0), pz, a % 2 == 0 ? STONE : "minecraft:mossy_cobblestone");
        }
        k.fill(cx - 1, 0, cz, cx + 1, 0, cz, "minecraft:stone_slab");
        k.set(cx, 1, cz, "minecraft:candle[candles=4,lit=true]");
        return k.build();
    }
}
