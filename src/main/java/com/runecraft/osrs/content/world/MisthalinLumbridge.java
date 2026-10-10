package com.runecraft.osrs.content.world;

import com.runecraft.osrs.content.world.MisthalinKit.Box;
import com.runecraft.osrs.content.world.MisthalinKit.Roof;
import com.runecraft.osrs.world.StructureDef;

import java.util.List;

import static com.runecraft.osrs.content.world.MisthalinKit.*;

/**
 * Lumbridge: the castle with its six towers and courtyard, the church and graveyard, the River Lum
 * and its bridge, Bob's Brilliant Axes, the general store, the furnace, the swamp shack, and the
 * farmland to the north (Mill Lane Mill, Fred's farm, the sheep, chicken and cow fields).
 * Lumbridge courtyard (3222, 3218) is block (0, 0).
 */
final class MisthalinLumbridge {
    private MisthalinLumbridge() {
    }

    static void addAll(List<StructureDef> out) {
        out.add(town());
        out.add(farmland());
        out.add(swamp());
        out.add(treePatch());
        out.add(hops());
    }

    // ------------------------------------------------------------------ town and castle

    static StructureDef town() {
        MisthalinKit k = new MisthalinKit("lumbridge", "Lumbridge", "LUMBRIDGE");
        k.tFlatten(3190, 3194, 3262, 3262);
        k.meadow(t(3190, 3194, 3262, 3262), 18);
        roads(k);
        river(k);
        castle(k);
        church(k);
        generalStore(k);
        bobsAxes(k);
        smithy(k);
        cottages(k);
        // trees around the castle grounds
        int[][] trees = {{-52, -40}, {-50, -10}, {-54, 12}, {-48, 36}, {-30, 40}, {-8, 42}, {-58, -60},
                {-44, -70}, {-34, -82}, {28, -60}, {22, -82}, {50, -40}, {58, -60}, {74, -30}, {-60, 40},
                {30, 46}, {-2, 36}, {-20, 44}, {-62, -20}, {-6, -84}};
        for (int[] tr : trees) {
            k.oak(tr[0], tr[1]);
        }
        return k.build();
    }

    private static void river(MisthalinKit k) {
        // River Lum: north-south past the castle, bending east below the bridge and south past the graveyard
        k.water(new Box(34, -80, 41, -1), 3);
        k.water(new Box(34, -10, 71, -1), 3);
        k.water(new Box(64, -10, 71, 43), 3);
        // reed and gravel banks
        k.floor(new Box(32, -80, 33, -18), "minecraft:sand").floor(new Box(42, -80, 43, -18), GRAVEL);
        k.floor(new Box(72, -10, 73, 43), GRAVEL).floor(new Box(42, 0, 63, 1), "minecraft:sand");
        k.clear(new Box(32, -80, 33, -18), 0, 1);
        k.scatter(new Box(33, -80, 33, -20), 0, "minecraft:sugar_cane", 3);
        k.set(36, -1, -60, "runecraft:lure_bait_fishing_spot").set(39, -1, -48, "runecraft:lure_bait_fishing_spot");
        k.set(68, -1, 20, "runecraft:lure_bait_fishing_spot");
        // the bridge east over the River Lum (road along tiles y 3225-3226)
        k.fill(33, -4, -17, 33, -1, -12, STONE_BRICKS).fill(42, -4, -17, 42, -1, -12, STONE_BRICKS);
        k.fill(37, -4, -16, 38, -1, -13, STONE_BRICKS);
        k.fill(31, 0, -16, 44, 0, -13, "minecraft:stone_brick_slab");
        k.fill(33, 0, -16, 42, 0, -13, "minecraft:spruce_planks");
        k.fill(32, 0, -16, 32, 0, -13, "minecraft:stone_brick_stairs[facing=east]");
        k.fill(43, 0, -16, 43, 0, -13, "minecraft:stone_brick_stairs[facing=west]");
        k.connect(32, 1, -17, 43, 1, -17, "minecraft:stone_brick_wall");
        k.connect(32, 1, -12, 43, 1, -12, "minecraft:stone_brick_wall");
        k.set(32, 2, -17, LANTERN).set(43, 2, -17, LANTERN).set(32, 2, -12, LANTERN).set(43, 2, -12, LANTERN);
    }

    private static void roads(MisthalinKit k) {
        // courtyard gate east to the bridge, then on towards Al Kharid
        k.dirtPath(new Box(12, -3, 22, 2));
        k.dirtPath(new Box(18, -16, 23, -3));
        k.dirtPath(new Box(18, -16, 31, -13));
        k.dirtPath(new Box(44, -16, 80, -13));
        // north road past the general store towards Mill Lane and Varrock
        k.dirtPath(new Box(-4, -88, 0, -28));
        k.dirtPath(new Box(1, -30, 18, -27));
        k.dirtPath(new Box(16, -27, 19, -16));
        k.dirtPath(new Box(-20, -48, -5, -46));
        // south road to Bob's, the church and the swamp
        k.dirtPath(new Box(18, 2, 21, 29));
        k.dirtPath(new Box(14, 27, 21, 29));
        k.dirtPath(new Box(22, 12, 35, 15));
        k.dirtPath(new Box(-56, 33, 13, 35));
        k.dirtPath(new Box(10, 30, 13, 35));
        // west path round the castle
        k.dirtPath(new Box(-56, -34, -52, 35));
        k.dirtPath(new Box(-56, -34, -5, -31));
        for (int z = -80; z <= -32; z += 12) {
            k.lampPost(1, z);
        }
        k.lampPost(24, -4).lampPost(24, 4).lampPost(29, -18).lampPost(46, -18).lampPost(22, 20);
    }

    private static void castle(MisthalinKit k) {
        String wall = STONE_BRICKS;
        String trim = POLISHED_ANDESITE;
        // ---- shells first: keep (3 storeys of 5 blocks), the two wings, then the six towers
        Box keep = new Box(-40, -26, -17, 25);
        k.house(keep).storeys(3).height(5).wall(wall).base(COBBLE).beam(trim).floor(POLISHED_ANDESITE)
                .upperFloor(SPRUCE_PLANKS).roof(Roof.STONE).crenel(wall)
                .doorAt('e', -2, 4, 4).build();
        Box northWing = new Box(-17, -26, 9, -15);
        Box southWing = new Box(-17, 14, 9, 25);
        k.house(northWing).storeys(2).height(5).wall(wall).base(COBBLE).beam(trim).floor(POLISHED_ANDESITE)
                .upperFloor(SPRUCE_PLANKS).roof(Roof.STONE).crenel(wall).doorAt('s', -6, 2, 3).doorAt('s', 2, 2, 3).build();
        k.house(southWing).storeys(2).height(5).wall(wall).base(COBBLE).beam(trim).floor(POLISHED_ANDESITE)
                .upperFloor(SPRUCE_PLANKS).roof(Roof.STONE).crenel(wall).doorAt('n', -6, 2, 3).doorAt('n', 2, 2, 3).build();
        // the keep's east wall is shared with the wings
        k.fill(-17, 0, -26, -17, 14, -15, wall).fill(-17, 0, 14, -17, 14, 25, wall);
        int[][] towers = {{-40, -26}, {-40, 25}, {-17, -26}, {-17, 25}, {10, -21}, {10, 20}};
        for (int[] tw : towers) {
            k.tower(tw[0], tw[1], 6, 20, 5, wall, SPRUCE_PLANKS, null, true);
            k.fill(tw[0], 20, tw[1], tw[0], 22, tw[1], "minecraft:spruce_fence");
            k.set(tw[0], 23, tw[1], "minecraft:blue_wool");
            k.set(tw[0] + 1, 23, tw[1], "minecraft:yellow_wool");
            k.set(tw[0] + 1, 22, tw[1], "minecraft:blue_wool");
        }
        // doorways from the keep roof (y 15) into the corner towers, and from the wing roofs (y 10)
        k.clear(-34, 15, -24, -34, 17, -24).clear(-23, 15, -24, -23, 17, -24);
        k.clear(-34, 15, 23, -34, 17, 23).clear(-23, 15, 23, -23, 17, 23);
        k.clear(-11, 10, -24, -11, 12, -24).clear(-11, 10, 23, -11, 12, 23);
        k.clear(4, 10, -21, 4, 12, -21).clear(4, 10, 20, 4, 12, 20);

        // grand entrance arch and lanterns
        k.fill(-17, 4, -3, -17, 4, 2, CHISELED);
        k.fill(-16, -1, -3, -15, -1, 2, POLISHED_ANDESITE);
        k.set(-16, 0, -3, "minecraft:stone_brick_wall").set(-16, 0, 2, "minecraft:stone_brick_wall");
        k.set(-16, 1, -3, LANTERN).set(-16, 1, 2, LANTERN);

        // ---- ground floor: north stair hall | great hall | kitchen (south)
        k.fill(-39, 0, -10, -18, 3, -10, wall).clear(-25, 0, -10, -24, 2, -10);
        k.fill(-39, 0, 11, -18, 3, 11, wall).clear(-25, 0, 11, -24, 2, 11);
        // great hall: long table, fireplace, carpet runner, banners
        k.carpet(new Box(-33, -8, -19, 9), 0, "red");
        k.carpet(new Box(-33, -2, -19, 1), 0, "yellow");
        k.diningZ(-30, 0, -6, 7);
        k.set(-30, 0, -8, "minecraft:dark_oak_stairs[facing=south]");
        k.fill(-39, 0, -2, -38, 3, 1, "minecraft:bricks");
        k.fill(-38, 0, -1, -38, 1, 0, AIR).set(-38, 0, -1, "minecraft:campfire[lit=true]").set(-38, 0, 0, "minecraft:campfire[lit=true]");
        k.fill(-38, -1, -2, -37, -1, 1, "minecraft:bricks");
        for (int z = -8; z <= 8; z += 4) {
            if (z != 0) {
                k.set(-39, 2, z, "minecraft:red_wall_banner[facing=east]");
            }
            k.hang(-26, 3, z);
        }
        k.set(-18, 2, -7, "minecraft:blue_wall_banner[facing=west]").set(-18, 2, 7, "minecraft:blue_wall_banner[facing=west]");
        k.shelvesZ(-18, 0, -9, -7, "west");
        k.shelvesZ(-18, 0, 8, 10, "west");

        // kitchen (the Cook's) with range, worktops, larder and cellar
        k.floor(new Box(-39, 12, -18, 24), "minecraft:bricks");
        k.set(-39, 0, 12, RANGE).set(-38, 0, 12, "minecraft:smoker[facing=south]");
        k.fill(-37, 0, 12, -31, 0, 12, "minecraft:spruce_planks").fill(-37, 1, 12, -31, 1, 12, "minecraft:spruce_slab");
        k.set(-35, 1, 12, "minecraft:cauldron").set(-33, 1, 12, "minecraft:flower_pot");
        k.fill(-39, 0, 13, -39, 0, 18, "minecraft:spruce_planks").fill(-39, 1, 13, -39, 1, 18, "minecraft:spruce_slab");
        k.diningX(-31, -26, 0, 18);
        for (int x = -28; x <= -24; x++) {
            k.set(x, 0, 24, x % 2 == 0 ? BARREL : "minecraft:chest[facing=north]");
            k.set(x, 1, 24, BARREL);
        }
        k.fill(-21, 0, 13, -19, 0, 13, HAY);
        // cellar below the kitchen
        k.fill(-30, -7, 19, -24, -2, 25, COBBLE);
        k.clear(-29, -6, 20, -25, -3, 24);
        k.ladder(-27, 24, "north", -6, -1);
        k.set(-27, -1, 24, "minecraft:spruce_trapdoor[half=top,facing=north]");
        k.set(-29, -6, 20, BARREL).set(-28, -6, 20, BARREL).set(-25, -6, 20, "minecraft:chest[facing=south]");
        k.set(-29, -6, 23, "minecraft:cobweb").set(-25, -3, 22, "minecraft:cobweb");
        k.set(-27, -3, 21, LANTERN_HANG);
        k.hang(-28, 3, 16).hang(-22, 3, 20).hang(-34, 3, 18);

        // north hall: the main staircase and stores
        k.carpet(new Box(-28, -20, -20, -11), 0, "gray");
        k.stairs2(-31, 0, -12, 'n', 5, "minecraft:stone_brick_stairs", 'w');
        k.stairs2(-36, 5, -16, 's', 5, "minecraft:spruce_stairs", 'w');
        k.connect(-33, 5, -15, -33, 5, -12, "minecraft:spruce_fence");
        k.connect(-30, 5, -15, -30, 5, -12, "minecraft:spruce_fence");
        k.connect(-32, 5, -12, -31, 5, -12, "minecraft:spruce_fence");
        k.connect(-38, 10, -16, -38, 10, -12, "minecraft:spruce_fence");
        k.connect(-35, 10, -16, -35, 10, -12, "minecraft:spruce_fence");
        k.connect(-37, 10, -16, -36, 10, -16, "minecraft:spruce_fence");
        k.crates(-27, -19);
        k.crates(-24, -12);
        k.hang(-30, 3, -18).hang(-24, 3, -16);

        // ---- first floor: Duke Horacio's chamber (north-east), spinning room (south), gallery
        k.fill(-39, 5, -10, -18, 8, -10, wall).clear(-25, 5, -10, -24, 7, -10);
        k.fill(-39, 5, 11, -18, 8, 11, wall).clear(-25, 5, 11, -24, 7, 11);
        k.fill(-29, 5, -20, -29, 8, -11, wall).clear(-29, 5, -14, -29, 7, -13);
        k.carpet(new Box(-28, -20, -18, -11), 5, "blue");
        k.fill(-25, 5, -19, -21, 5, -19, "minecraft:dark_oak_planks");
        k.darkChair(-23, 5, -18, "north");
        k.set(-23, 6, -19, "minecraft:gold_block");
        k.table(-23, 5, -15).chair(-23, 5, -14, "north").chair(-23, 5, -16, "south");
        k.shelvesZ(-28, 5, -19, -16, "east");
        k.set(-18, 6, -13, "minecraft:blue_wall_banner[facing=west]").set(-18, 6, -17, "minecraft:blue_wall_banner[facing=west]");
        k.hang(-23, 8, -15);
        // gallery and audience hall
        k.carpet(new Box(-36, -9, -19, 10), 5, "red");
        k.diningZ(-28, 5, -4, 3);
        for (int z = -8; z <= 8; z += 4) {
            k.hang(-28, 8, z).hang(-21, 8, z);
        }
        // spinning wheel room
        k.carpet(new Box(-34, 12, -19, 19), 5, "light_gray");
        k.set(-30, 5, 18, SPINNING).set(-26, 5, 18, SPINNING);
        k.fill(-30, 5, 14, -26, 5, 14, "minecraft:white_wool");
        k.set(-21, 5, 13, "minecraft:loom[facing=north]").set(-20, 5, 13, BARREL).set(-22, 5, 13, "minecraft:chest[facing=south]");
        k.hang(-28, 8, 16).hang(-21, 8, 18);

        // ---- second floor: Lumbridge bank (top of the castle) and the archive
        k.carpet(new Box(-39, -9, -18, 11), 10, "green");
        k.boothsX(-35, -23, 10, 4);
        k.fill(-36, 10, 4, -36, 11, 4, POLISHED_ANDESITE).fill(-22, 10, 4, -22, 11, 4, POLISHED_ANDESITE);
        k.fill(-35, 10, 5, -23, 10, 8, "minecraft:polished_andesite");
        k.set(-30, 10, 8, "minecraft:chest[facing=north]").set(-29, 10, 8, "minecraft:chest[facing=north]");
        k.set(-25, 10, 8, BARREL).set(-24, 10, 8, BARREL);
        k.fill(-39, 10, 12, -18, 13, 12, wall).clear(-25, 10, 12, -24, 12, 12);
        k.shelvesX(-33, -24, 10, 18, "north");
        k.diningX(-32, -26, 10, 15);
        k.set(-20, 10, -8, "minecraft:lectern[facing=west]");
        for (int x = -35; x <= -21; x += 7) {
            k.hang(x, 13, 0).hang(x, 13, -6).hang(x, 13, 17);
        }
        k.shelvesX(-30, -24, 10, -25, "south");
        k.ladder(-18, -8, "west", 10, 14);

        // ---- wing interiors
        // north wing: armoury below, guard quarters above
        for (int x = -10; x <= 2; x += 3) {
            k.set(x, 0, -25, "minecraft:grindstone[face=floor,facing=north]");
            k.set(x + 1, 0, -25, BARREL);
            k.set(x, 2, -25, "minecraft:white_wall_banner[facing=south]");
        }
        k.fill(-9, 0, -20, -3, 0, -20, "minecraft:spruce_planks").fill(-9, 1, -20, -3, 1, -20, "minecraft:spruce_slab");
        k.set(-6, 0, -20, "minecraft:smithing_table");
        k.ladder(-1, -25, "south", 0, 4);
        k.ladder(5, -16, "north", 5, 9);
        k.carpet(new Box(-10, -24, 2, -17), 5, "cyan");
        k.bed(-9, 5, -24, 's', "cyan").bed(-6, 5, -24, 's', "cyan").bed(-3, 5, -24, 's', "cyan");
        // south wing: barracks
        for (int x = -10; x <= 2; x += 3) {
            k.bed(x, 0, 23, 'n', "blue");
            k.set(x + 1, 0, 24, "minecraft:chest[facing=north]");
        }
        k.diningX(-9, -4, 0, 18);
        k.ladder(-1, 24, "north", 0, 4);
        k.ladder(5, 15, "south", 5, 9);
        k.carpet(new Box(-10, 16, 2, 23), 5, "brown");
        k.diningX(-8, -2, 5, 19);
        for (int x = -8; x <= 2; x += 10) {
            k.hang(x, 3, -20).hang(x, 8, -20).hang(x, 3, 19).hang(x, 8, 19);
        }

        // ---- courtyard: east curtain wall with gatehouse
        k.fill(10, -1, -14, 11, 5, 13, wall);
        k.clear(10, 0, -3, 11, 3, 2);
        k.fill(10, -1, -3, 11, -1, 2, POLISHED_ANDESITE);
        for (int z = -14; z <= 13; z += 2) {
            if (z < -3 || z > 2) {
                k.set(10, 6, z, wall).set(11, 6, z, wall);
            }
        }
        k.fill(10, 4, -3, 11, 5, 2, CHISELED);
        k.fill(12, 0, -4, 12, 6, -4, CHISELED).fill(12, 0, 3, 12, 6, 3, CHISELED);
        k.fill(12, 7, -4, 12, 7, 3, "minecraft:stone_brick_slab");
        k.set(13, 3, -4, "minecraft:wall_torch[facing=east]").set(13, 3, 3, "minecraft:wall_torch[facing=east]");
        k.set(9, 3, -4, "minecraft:wall_torch[facing=west]").set(9, 3, 3, "minecraft:wall_torch[facing=west]");
        // steps up onto the curtain wall walk
        k.stairsSolid(9, 0, 12, 'n', 6, "minecraft:stone_brick_stairs", wall);
        k.stairsSolid(9, 0, -13, 's', 6, "minecraft:stone_brick_stairs", wall);
        // courtyard paving and features
        Box yard = new Box(-16, -14, 8, 13);
        k.floor(yard, GRASS);
        k.floor(new Box(-16, -3, 9, 2), "minecraft:stone_bricks");
        k.floor(new Box(-6, -14, -3, 13), POLISHED_ANDESITE);
        k.scatter(new Box(-16, -3, 9, 2), -1, CRACKED_BRICKS, 10);
        k.scatter(new Box(-16, -3, 9, 2), -1, MOSSY_BRICKS, 12);
        k.meadow(new Box(-16, -14, -7, -4), 8);
        k.meadow(new Box(-2, 3, 7, 13), 8);
        k.oak(3, 8);
        k.oak(-12, -9);
        k.fill(-13, 0, 7, -11, 0, 7, "minecraft:spruce_stairs[facing=north]");
        k.fill(1, 0, -8, 3, 0, -8, "minecraft:spruce_stairs[facing=south]");
        k.lampPost(-8, -5).lampPost(-8, 4).lampPost(6, -5).lampPost(6, 4);
        // the Gielinor portal stands against the north wing
        k.fill(-1, -1, -14, 1, -1, -12, CHISELED);
        k.set(0, 0, -13, PORTAL);
        k.set(-1, 0, -13, "minecraft:stone_brick_wall").set(1, 0, -13, "minecraft:stone_brick_wall");
        k.set(-1, 1, -13, LANTERN).set(1, 1, -13, LANTERN);
    }

    private static void church(MisthalinKit k) {
        // Lumbridge church, tiles 3240-3247 x 3204-3214: tall nave, bell tower, organ and altar
        Box nave = new Box(36, 9, 51, 30);
        k.house(nave).storeys(1).height(8).wall(STONE_BRICKS).base(COBBLE).corner(CHISELED)
                .floor(POLISHED_ANDESITE).roof(Roof.SLATE).window(null).doorAt('w', 12, 2, 4).doorAt('n', 43, 2, 4).build();
        for (int z = 12; z <= 28; z += 4) {
            if (z != 12) {
                k.connect(36, 2, z, 36, 5, z, "minecraft:yellow_stained_glass_pane");
            }
            k.connect(51, 2, z, 51, 5, z, "minecraft:blue_stained_glass_pane");
        }
        k.connect(43, 3, 30, 44, 6, 30, "minecraft:yellow_stained_glass_pane");
        k.connect(43, 3, 30, 44, 6, 30, "minecraft:yellow_stained_glass_pane");
        k.carpet(new Box(43, 10, 44, 29), 0, "red");
        for (int z = 13; z <= 24; z += 2) {
            k.fill(38, 0, z, 41, 0, z, "minecraft:spruce_stairs[facing=north]");
            k.fill(46, 0, z, 49, 0, z, "minecraft:spruce_stairs[facing=north]");
        }
        // sanctuary
        k.fill(37, -1, 26, 50, -1, 29, "minecraft:quartz_block");
        k.fill(37, 0, 26, 50, 0, 26, "minecraft:quartz_slab");
        k.clear(42, 0, 26, 45, 0, 26);
        k.set(43, 0, 28, ALTAR).set(44, 0, 28, "minecraft:quartz_block");
        k.set(44, 1, 28, "minecraft:candle[candles=3,lit=true]");
        k.set(42, 0, 29, "minecraft:gold_block").set(45, 0, 29, "minecraft:gold_block");
        k.set(42, 1, 29, LANTERN).set(45, 1, 29, LANTERN);
        // organ against the east wall
        k.fill(50, 0, 18, 50, 0, 22, "minecraft:note_block");
        k.fill(50, 1, 18, 50, 4, 22, "minecraft:spruce_planks");
        for (int z = 18; z <= 22; z++) {
            k.fill(49, 1, z, 49, 2 + (z % 3), z, "minecraft:iron_bars");
        }
        k.chair(48, 0, 20, "east");
        for (int z = 12; z <= 28; z += 5) {
            k.hang(43, 6, z).hang(44, 6, z);
        }
        // bell tower over the north-west corner
        Box bell = new Box(36, 5, 41, 10);
        k.fill(bell, -2, -1, COBBLE);
        k.walls(bell, 0, 15, STONE_BRICKS);
        k.clear(37, 0, 6, 40, 15, 9);
        k.fill(37, -1, 6, 40, -1, 9, POLISHED_ANDESITE);
        k.fill(37, 11, 6, 40, 11, 9, SPRUCE_PLANKS);
        k.ladder(37, 6, "south", 0, 11);
        k.clear(36, 12, 7, 36, 14, 8).clear(41, 12, 7, 41, 14, 8).clear(38, 12, 5, 39, 14, 5).clear(38, 12, 10, 39, 14, 10);
        k.fill(36, 16, 5, 41, 16, 10, STONE_BRICKS);
        k.set(38, 15, 7, "minecraft:bell[attachment=ceiling,facing=north]");
        k.roof(bell, 17, Roof.SLATE, null);
        k.fill(38, 20, 7, 39, 21, 8, "minecraft:deepslate_tiles");
        k.set(38, 22, 7, "minecraft:gold_block");
        k.clear(41, 0, 7, 41, 2, 8);
        // graveyard to the east, walled
        Box yard = new Box(52, 9, 62, 30);
        k.floor(yard, COARSE_DIRT);
        k.scatter(yard, -1, GRASS, 2);
        k.connect(52, 0, 30, 62, 0, 30, "minecraft:cobblestone_wall");
        k.connect(62, 0, 9, 62, 0, 30, "minecraft:cobblestone_wall");
        k.connect(52, 0, 9, 62, 0, 9, "minecraft:cobblestone_wall");
        k.clear(56, 0, 9, 57, 0, 9);
        for (int x = 54; x <= 60; x += 3) {
            for (int z = 12; z <= 27; z += 4) {
                k.grave(x, z);
            }
        }
        k.deadTree(59, 28);
        k.set(55, 0, 28, "minecraft:mossy_stone_brick_wall").set(55, 1, 28, "minecraft:mossy_stone_brick_wall");
    }

    private static void generalStore(MisthalinKit k) {
        // Lumbridge General Store, tiles 3209-3216 x 3243-3250
        Box b = new Box(-26, -64, -11, -49);
        k.house(b).storeys(1).height(5).wall(STONE_BRICKS).base(COBBLE).corner(STRIPPED_SPRUCE)
                .floor(SPRUCE_PLANKS).roof(Roof.SLATE).doorAt('s', -19, 2, 3).doorAt('e', -57, 2, 3).build();
        k.counterX(-24, -15, 0, -57, GENERAL);
        k.shelvesX(-25, -12, 0, -63, "south");
        k.shelvesZ(-25, 0, -62, -58, "east");
        k.set(-13, 0, -52, BARREL).set(-13, 1, -52, BARREL).set(-14, 0, -50, "minecraft:chest[facing=north]");
        k.set(-24, 0, -51, "minecraft:cauldron");
        k.hang(-20, 3, -60).hang(-15, 3, -53).hang(-22, 3, -53);
        k.set(-20, 2, -48, "minecraft:wall_torch[facing=south]").set(-16, 2, -48, "minecraft:wall_torch[facing=south]");
        k.crates(-10, -52);
    }

    private static void bobsAxes(MisthalinKit k) {
        // Bob's Brilliant Axes, tiles 3229-3234 x 3198-3203
        Box b = new Box(14, 30, 25, 41);
        k.house(b).storeys(1).wall(STONE_BRICKS).base(COBBLE).corner(STRIPPED_SPRUCE)
                .floor(SPRUCE_PLANKS).roof(Roof.SLATE).doorAt('n', 16, 2, 3).build();
        k.counterX(17, 23, 0, 35, TOOL_SHOP);
        k.set(24, 0, 40, "minecraft:grindstone[face=floor,facing=north]");
        k.set(15, 0, 40, "minecraft:smithing_table").set(16, 0, 40, BARREL);
        k.set(24, 2, 31, "minecraft:wall_torch[facing=south]");
        k.hang(19, 2, 37);
        k.set(15, 0, 36, "minecraft:chest[facing=east]");
        k.crates(26, 36);
        // wood pile behind
        k.fill(15, 0, 43, 22, 1, 44, "minecraft:oak_log[axis=x]");
    }

    private static void smithy(MisthalinKit k) {
        // Lumbridge furnace house, tiles 3222-3229 x 3251-3257
        Box b = new Box(4, -78, 15, -67);
        k.house(b).storeys(1).height(5).wall(COBBLE).corner(STRIPPED_SPRUCE).floor(STONE_BRICKS)
                .roof(Roof.SLATE).doorAt('w', -73, 2, 3).build();
        k.fill(11, 0, -77, 14, 4, -77, "minecraft:bricks");
        k.set(12, 0, -77, FURNACE).set(13, 0, -77, "minecraft:blast_furnace[facing=south]");
        k.fill(13, 5, -77, 14, 8, -76, "minecraft:bricks");
        k.set(9, 0, -72, ANVIL);
        k.set(6, 0, -68, BARREL).set(7, 0, -68, "minecraft:chest[facing=north]").set(14, 0, -68, "minecraft:cauldron");
        k.hang(9, 3, -72);
        k.set(5, 0, -77, COBBLE).set(5, 1, -77, "minecraft:coal_block");
    }

    private static void cottages(MisthalinKit k) {
        // small houses along the north road (Lumbridge Guide's area, the Doomsayer, tutors)
        int[][] spots = {{6, -42, 15, -33}, {-24, -44, -15, -36}, {8, -60, 17, -51}};
        int i = 0;
        for (int[] s : spots) {
            Box b = new Box(s[0], s[1], s[2], s[3]);
            k.house(b).storeys(1).wall(STONE_BRICKS).base(COBBLE).corner(STRIPPED_SPRUCE).floor(SPRUCE_PLANKS)
                    .roof(Roof.SLATE).doorAt(i == 1 ? 'e' : 'w', (s[1] + s[3]) / 2, 2, 3).build();
            Box in = b.inset(1);
            k.table(in.cx(), 0, in.cz()).chair(in.cx() - 1, 0, in.cz(), "east").chair(in.cx() + 1, 0, in.cz(), "west");
            k.bed(in.x1(), 0, in.z1(), 's', i == 0 ? "red" : "green");
            k.set(in.x2(), 0, in.z1(), BARREL).set(in.x2(), 0, in.z2(), "minecraft:chest[facing=north]");
            k.set(in.x1(), 0, in.z2(), "minecraft:crafting_table");
            k.hang(in.cx(), 2, in.cz());
            i++;
        }
        // the Doomsayer's sign post
        k.set(-2, 0, -20, "minecraft:spruce_fence").set(-2, 1, -20, "minecraft:spruce_fence");
        k.set(-2, 2, -20, "minecraft:red_wool");
    }

    // ------------------------------------------------------------------ farmland

    static StructureDef farmland() {
        MisthalinKit k = new MisthalinKit("lumbridge_farmland", "Lumbridge farmland");
        mill(k);
        fredsFarm(k);
        chickens(k);
        cows(k);
        return k.build();
    }

    private static void mill(MisthalinKit k) {
        // Mill Lane Mill, tiles 3164-3169 x 3303-3308 (3 storeys: hopper on top, flour bin below)
        k.tFlatten(3150, 3288, 3180, 3316);
        Box mill = t(3164, 3303, 3169, 3308);
        k.house(mill).storeys(3).height(5).wall(STONE_BRICKS).upperWall(SPRUCE_PLANKS).corner(SPRUCE_LOG)
                .base(COBBLE).beam(STRIPPED_SPRUCE).floor(SPRUCE_PLANKS).roof(Roof.DARK_OAK).door('s', 3166).build();
        Box in = mill.inset(1);
        // ladders
        k.ladder(in.x1(), in.z1(), "south", 0, 9);
        // flour bin on the ground floor, hopper on the top floor
        k.fill(in.x2() - 1, 0, in.z2() - 1, in.x2(), 1, in.z2(), "minecraft:smooth_quartz");
        k.set(in.x2() - 1, 2, in.z2() - 1, "minecraft:hopper[facing=down]");
        k.set(in.cx(), 10, in.cz(), "minecraft:hopper[facing=down]");
        k.set(in.cx(), 11, in.cz(), "minecraft:hopper[facing=down]");
        k.fill(in.cx(), 5, in.cz(), in.cx(), 9, in.cz(), "minecraft:stripped_oak_log");
        k.set(in.cx() + 1, 5, in.cz(), "minecraft:grindstone[face=floor,facing=east]");
        k.set(in.x1() + 1, 0, in.z2(), BARREL).set(in.x1() + 2, 0, in.z2(), BARREL).set(in.x1() + 1, 1, in.z2(), BARREL);
        k.hang(in.cx(), 3, in.cz()).hang(in.cx() + 2, 8, in.cz() + 2).hang(in.cx() - 2, 13, in.cz());
        // sails on the east face
        int sx = mill.x2() + 1;
        int sy = 10;
        int sz = mill.cz();
        k.set(sx, sy, sz, "minecraft:spruce_log[axis=x]");
        for (int i = 1; i <= 7; i++) {
            k.set(sx + 1, sy + i, sz, "minecraft:spruce_fence").set(sx + 1, sy - i, sz, "minecraft:spruce_fence");
            k.set(sx + 1, sy, sz + i, "minecraft:spruce_fence").set(sx + 1, sy, sz - i, "minecraft:spruce_fence");
            if (i >= 2) {
                k.set(sx + 1, sy + i, sz + 1, "minecraft:white_wool").set(sx + 1, sy - i, sz - 1, "minecraft:white_wool");
                k.set(sx + 1, sy - 1, sz + i, "minecraft:white_wool").set(sx + 1, sy + 1, sz - i, "minecraft:white_wool");
            }
        }
        k.set(sx + 1, sy, sz, "minecraft:spruce_planks");
        // wheat fields west of the mill (Mill Lane)
        k.field(t(3152, 3296, 3160, 3302), "minecraft:wheat[age=7]");
        k.field(t(3152, 3305, 3160, 3311), "minecraft:wheat[age=6]");
        k.fenceRing(t(3151, 3295, 3161, 3312), "minecraft:oak_fence");
        k.gate(X(3161) + 1, Z(3304), "minecraft:oak_fence_gate", "east");
        k.tPath(3162, 3300, 3178, 3300, 2);
        k.tPath(3166, 3302, 3166, 3289, 2);
        k.fill(X(3163), 0, Z(3302), X(3163) + 1, 0, Z(3302), HAY);
        k.set(X(3163), 1, Z(3302), HAY);
        k.tOak(3172, 3312).tOak(3176, 3292).tOak(3155, 3290);
    }

    private static void fredsFarm(MisthalinKit k) {
        // Fred the Farmer's house (3188-3192 x 3271-3276) and sheep field to the east
        k.tFlatten(3182, 3258, 3216, 3282);
        Box house = t(3186, 3271, 3192, 3277);
        k.house(house).storeys(1).height(5).wall(COBBLE).corner(OAK_LOG).floor(OAK_PLANKS)
                .roof(Roof.SPRUCE).gable(OAK_PLANKS).doorAt('e', Z(3274), 2, 3).build();
        Box in = house.inset(1);
        k.kitchen(in, 0);
        k.bed(in.x1(), 0, in.z2() - 2, 'e', "brown");
        k.set(in.x1(), 0, in.z2(), "minecraft:chest[facing=east]");
        k.set(in.x2(), 0, in.z1() + 2, "minecraft:chest[facing=west]");
        k.hang(in.cx(), 3, in.cz());
        // sheep field
        Box sheep = t(3194, 3257, 3212, 3272);
        k.fenceRing(sheep, "minecraft:oak_fence");
        k.gate(sheep.x1(), Z(3268), "minecraft:oak_fence_gate", "east");
        k.meadow(sheep.inset(1), 8);
        k.fill(sheep.x1() + 3, 0, sheep.z1() + 3, sheep.x1() + 5, 0, sheep.z1() + 3, HAY);
        k.clear(sheep.x2() - 4, 0, sheep.z2() - 4, sheep.x2() - 3, 0, sheep.z2() - 4);
        k.set(sheep.x2() - 3, -1, sheep.z2() - 4, WATER).set(sheep.x2() - 4, -1, sheep.z2() - 4, WATER);
        k.set(sheep.x2() - 4, 0, sheep.z2() - 8, "minecraft:composter");
        k.connect(sheep.x2() - 6, 0, sheep.z2() - 6, sheep.x2() - 1, 0, sheep.z2() - 6, "minecraft:oak_fence");
        // farm garden and well
        k.field(t(3183, 3262, 3190, 3267), "minecraft:carrots[age=7]");
        k.fenceRing(t(3182, 3261, 3191, 3268), "minecraft:spruce_fence");
        k.gate(X(3186), Z(3268), "minecraft:spruce_fence_gate", "north");
        k.well(X(3195), Z(3277));
        k.tPath(3193, 3274, 3214, 3274, 2);
        k.tOak(3184, 3280).tOak(3214, 3279);
    }

    private static void chickens(MisthalinKit k) {
        // Lumbridge chicken farm east of the road (3225-3236 x 3289-3301) with its coop
        k.tFlatten(3220, 3284, 3240, 3306);
        Box pen = t(3225, 3289, 3236, 3301);
        k.floor(pen.inset(1), COARSE_DIRT);
        k.scatter(pen.inset(1), -1, GRASS, 3);
        k.fenceRing(pen, "minecraft:spruce_fence");
        k.gate(pen.x1(), Z(3294), "minecraft:spruce_fence_gate", "east");
        Box coop = t(3231, 3297, 3235, 3300);
        k.house(coop).storeys(1).height(3).wall(SPRUCE_PLANKS).corner(SPRUCE_LOG).floor(HAY)
                .roof(Roof.SPRUCE).window(null).doorAt('s', coop.cx(), 2, 2).build();
        k.fill(coop.x1() + 1, 0, coop.z1() + 1, coop.x2() - 1, 0, coop.z1() + 1, HAY);
        k.scatter(pen.inset(2), 0, "minecraft:wheat[age=7]", 25);
        k.set(pen.x1() + 3, 0, pen.z2() - 2, "minecraft:water_cauldron[level=3]");
        // farmhouse beside the pen
        Box farm = t(3226, 3285, 3232, 3288);
        k.house(farm).storeys(1).wall(COBBLE).corner(OAK_LOG).floor(OAK_PLANKS).roof(Roof.SPRUCE)
                .gable(OAK_PLANKS).doorAt('w', farm.cz(), 2, 3).build();
        k.kitchen(farm.inset(1), 0);
        k.tPath(3222, 3284, 3222, 3306, 2);
    }

    private static void cows(MisthalinKit k) {
        // east cow field across the River Lum (3247-3265 x 3255-3298)
        k.tFlatten(3247, 3255, 3265, 3298);
        Box field = t(3247, 3256, 3265, 3297);
        k.fenceRing(field, "minecraft:oak_fence");
        k.meadow(field.inset(1), 6);
        // dairy shed in the corner
        Box shed = t(3259, 3292, 3264, 3296);
        k.house(shed).storeys(1).wall(SPRUCE_PLANKS).corner(SPRUCE_LOG).floor(COARSE_DIRT)
                .roof(Roof.SPRUCE).doorAt('s', shed.cx(), 3, 3).build();
        k.fill(shed.x1() + 1, 0, shed.z1() + 1, shed.x2() - 1, 1, shed.z1() + 1, HAY);
        k.set(shed.x1() + 1, 0, shed.z2() - 1, "minecraft:water_cauldron[level=3]");
        k.clear(X(3250), 0, Z(3262), X(3252) + 1, 0, Z(3262) + 2);
        k.fill(X(3250), -1, Z(3262), X(3252) + 1, -1, Z(3262) + 2, WATER);
        k.tOak(3262, 3260).tOak(3246, 3290);
        k.tPath(3250, 3255, 3250, 3227, 2);
        k.clear(X(3250), 0, field.z2(), X(3251) + 1, 0, field.z2());
        k.gate(X(3250), field.z2(), "minecraft:oak_fence_gate", "south");
        k.gate(X(3250) + 1, field.z2(), "minecraft:oak_fence_gate", "south");
    }

    // ------------------------------------------------------------------ swamp shack

    static StructureDef swamp() {
        // Father Urhney's shack in Lumbridge Swamp (~3147, 3175)
        MisthalinKit k = new MisthalinKit("lumbridge_swamp_shack", "Lumbridge Swamp shack");
        k.tFlatten(3136, 3164, 3160, 3186);
        Box area = t(3136, 3164, 3160, 3186);
        k.floor(area, MUD);
        k.scatter(area, -1, GRASS, 3);
        k.scatter(area, -1, "minecraft:moss_block", 6);
        for (int[] p : new int[][]{{3138, 3166}, {3156, 3183}, {3140, 3182}, {3155, 3167}, {3150, 3184}}) {
            k.fill(X(p[0]), -2, Z(p[1]), X(p[0]) + 3, -1, Z(p[1]) + 2, WATER);
            k.set(X(p[0]) + 1, 0, Z(p[1]) + 1, "minecraft:lily_pad");
        }
        Box shack = t(3144, 3172, 3150, 3178);
        k.fill(shack.grow(1), -1, -1, "minecraft:spruce_planks");
        k.house(shack).storeys(1).wall(SPRUCE_PLANKS).corner(SPRUCE_LOG).floor(SPRUCE_PLANKS)
                .base("minecraft:spruce_planks").roof(Roof.SPRUCE).doorAt('e', Z(3175), 2, 3).build();
        Box in = shack.inset(1);
        k.bed(in.x1(), 0, in.z1(), 's', "brown");
        k.set(in.x2(), 0, in.z1(), BARREL).set(in.x2() - 1, 0, in.z1(), "minecraft:cauldron");
        k.table(in.cx(), 0, in.cz() + 2).chair(in.cx() - 1, 0, in.cz() + 2, "east");
        k.set(in.x1(), 0, in.z2(), BOOKSHELF).set(in.x1() + 1, 0, in.z2(), "minecraft:lectern[facing=north]");
        k.hang(in.cx(), 2, in.cz());
        k.deadTree(X(3141), Z(3170)).deadTree(X(3154), Z(3176)).deadTree(X(3139), Z(3178));
        k.tree(X(3157), Z(3171), 5, DARK_LOG, DARK_LEAVES);
        return k.build();
    }

    // ------------------------------------------------------------------ farming patches

    static StructureDef treePatch() {
        MisthalinKit k = new MisthalinKit("lumbridge_tree_patch", "Lumbridge tree patch", "lumbridge_tree_patch");
        int x = X(3193);
        int z = Z(3231);
        k.fill(x - 2, -1, z - 2, x + 2, -1, z + 2, COARSE_DIRT);
        k.connect(x - 3, 0, z - 3, x + 3, 0, z - 3, "minecraft:oak_fence");
        k.connect(x - 3, 0, z + 3, x + 3, 0, z + 3, "minecraft:oak_fence");
        k.connect(x - 3, 0, z - 3, x - 3, 0, z + 3, "minecraft:oak_fence");
        k.connect(x + 3, 0, z - 3, x + 3, 0, z + 3, "minecraft:oak_fence");
        k.clear(x + 3, 0, z, x + 3, 0, z);
        k.set(x, 0, z, "runecraft:tree_patch");
        k.set(x + 4, 0, z + 2, "runecraft:compost_bin");
        return k.build();
    }

    static StructureDef hops() {
        MisthalinKit k = new MisthalinKit("lumbridge_hops", "Lumbridge hops patch", "lumbridge_hops");
        int x = X(3229);
        int z = Z(3315);
        k.tFlatten(3225, 3311, 3233, 3318);
        k.fill(x - 3, -1, z - 2, x + 3, -1, z + 2, COARSE_DIRT);
        k.connect(x - 4, 0, z - 3, x + 4, 0, z - 3, "minecraft:spruce_fence");
        k.connect(x - 4, 0, z - 3, x - 4, 0, z + 3, "minecraft:spruce_fence");
        k.connect(x + 4, 0, z - 3, x + 4, 0, z + 3, "minecraft:spruce_fence");
        for (int dx = -3; dx <= 3; dx += 2) {
            k.fill(x + dx, 0, z - 2, x + dx, 2, z - 2, "minecraft:spruce_fence");
        }
        k.set(x, 0, z, "runecraft:hops_patch");
        k.set(x + 5, 0, z + 2, "runecraft:compost_bin");
        return k.build();
    }
}
