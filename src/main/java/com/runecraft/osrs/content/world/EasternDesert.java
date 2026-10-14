package com.runecraft.osrs.content.world;

import com.runecraft.osrs.world.StructureDef;

import java.util.List;

import static com.runecraft.osrs.content.world.EasternKit.*;
import static com.runecraft.osrs.world.StructureDef.bx;
import static com.runecraft.osrs.world.StructureDef.bz;

/**
 * The Kharidian Desert at real OSRS tiles: Al Kharid (wall, toll gate, palace, bank, furnace and shops),
 * the PvP (Duel) Arena, Shantay Pass, Pollnivneach, Nardah, Sophanem, Menaphos, the Agility Pyramid, the
 * Jaldraocht pyramid, the Necropolis and Tombs of Amascut, the Bandit Camp, Uzer, the Kalphite Lair entrance
 * and the Desert Mining Camp.
 */
final class EasternDesert {
    private static final String WHITE_WOOL = "minecraft:white_wool";
    private static final String RED_WOOL = "minecraft:red_wool";
    private static final String GOLD = "minecraft:gold_block";
    private static final String SMOOTH_STAIRS = "minecraft:smooth_sandstone_stairs";
    private static final String SS_STAIRS = "minecraft:sandstone_stairs";

    private EasternDesert() {
    }

    static void addAll(List<StructureDef> out) {
        out.add(alKharid());
        out.add(alKharidPalace());
        out.add(alKharidCactusGarden());
        out.add(pvpArena());
        out.add(shantayPass());
        out.add(kalphiteLair());
        out.add(desertMiningCamp());
        out.add(pollnivneach());
        out.add(elidCave());
        out.add(nardah());
        out.add(sophanem());
        out.add(sophanemPyramid());
        out.add(sophanemTemple());
        out.add(necropolis());
        out.add(tombsOfAmascut());
        out.add(menaphos());
        out.add(agilityPyramid());
        out.add(jaldraocht());
        out.add(jaldraochtObelisks());
        out.add(banditCamp());
        out.add(uzer());
        out.add(uzerTemple());
    }

    // ------------------------------------------------------------------ Al Kharid

    /** Al Kharid town: west wall with the Lumbridge toll gate, bank, furnace, tanner, kebabs and shops. */
    static StructureDef alKharid() {
        EasternKit k = new EasternKit("al_kharid", "Al Kharid").replaces("AL_KHARID");
        int g = k.g;
        k.flat(3264, 3148, 3328, 3234, SAND);
        k.flat(3262, 3135, 3271, 3262, SAND);
        k.patches(3270, 3150, 3326, 3232, "minecraft:sandstone", 50, 11);
        k.patches(3270, 3150, 3326, 3232, "minecraft:red_sand", 25, 12);

        // The border fence along x=3267 and the toll gate to Lumbridge at y 3227-3228. On the OSRS map it
        // only runs north of about 3201 (it bends south-west to the river there), so the bank stands clear.
        k.wall(3267, 3201, 3267, 3262, 4, SS, CUT);
        k.tfill(3267, 3201, 3267, 3262, 1, 1, CUT);
        k.gate(3267, 3227, 3267, 3228, 4);
        int[] gq = r(3267, 3227, 3267, 3228);
        k.fill(gq[0], g, gq[1], gq[0], g, gq[3], "minecraft:oak_fence_gate[facing=east,open=true]");
        k.fill(gq[2], g + 3, gq[1], gq[2], g + 3, gq[3], CHIS);
        k.tower(3267, 3225, 2, 7, CUT, SMOOTH, "east", null);
        k.tower(3267, 3230, 2, 7, CUT, SMOOTH, "east", null);
        int[] toll = k.hall(3269, 3229, 3272, 3232, 1, DESERT, "south");
        k.set(toll[0] + 1, g, toll[1] + 1, "minecraft:chest[facing=south]");
        k.set(toll[2] - 1, g, toll[1] + 1, "minecraft:smooth_sandstone_slab[type=top]");
        k.awning(toll, "south", ORANGE_WOOL);

        // Roads: from the gate east, the main north-south road, the street north of the palace.
        k.floor(3268, 3227, 3279, 3228, SMOOTH);
        k.floor(3278, 3148, 3279, 3234, SMOOTH);
        k.floor(3268, 3180, 3310, 3181, SMOOTH);
        k.floor(3292, 3182, 3293, 3185, SMOOTH);
        k.floor(3311, 3156, 3311, 3190, CUT);

        // Al Kharid bank (OSRS map: 3265-3272 x 3161-3173, door east at 3166-3167): the booths in a north-south
        // row along x 3268 between the stub walls at its ends (3171-3173 and 3161-3163), bankers behind. The west
        // and south walls stand in (3266 and 3163, approx) so the rooftop course cable tile (3265, 3162) stays clear.
        int[] bank = k.hall(3266, 3163, 3272, 3173, 1, DESERT_RICH, "east");
        k.boothsZ(bx(3268), bz(3170), bz(3164) + 1, g, CUT);
        k.fill(bx(3268), g, bank[1] + 1, bx(3268), g + 3, bz(3171) + 1, CUT);
        k.fill(bx(3268), g, bz(3163), bx(3268), g + 3, bank[3] - 1, CUT);
        k.fill(bank[0] + 1, g - 1, bank[1] + 1, bx(3268) - 1, g - 1, bank[3] - 1, SMOOTH);
        int bmz = (bank[1] + bank[3]) / 2;
        k.fill(bx(3268) + 2, g, bmz, bank[2] - 1, g, bmz + 1, "minecraft:red_carpet");
        k.set(bank[0] + 1, g, bank[1] + 1, "minecraft:chest[facing=east]");
        k.set(bank[0] + 1, g, bank[3] - 1, "minecraft:chest[facing=east]");
        k.fill(bx(3269) + 1, g, bank[1] + 1, bank[2] - 1, g, bank[1] + 1, "minecraft:smooth_sandstone_stairs[facing=north]");
        k.fill(bx(3269) + 1, g, bank[3] - 1, bank[2] - 1, g, bank[3] - 1, "minecraft:smooth_sandstone_stairs[facing=south]");
        k.dome((bank[0] + bank[2]) / 2, bmz, g + 4, 4.5, 3, SMOOTH, SMOOTH);
        k.set((bank[0] + bank[2]) / 2, g + 7, bmz, GOLD);

        // Karim's kebabs (range), the furnace, Ellis the tanner.
        k.desertShop(3271, 3175, 3276, 3179, "east", RANGE, ORANGE_WOOL);
        int[] fur = k.hall(3270, 3183, 3276, 3189, 1, DESERT, "east");
        int fmz = (fur[1] + fur[3]) / 2;
        k.set(fur[0] + 1, g, fmz, FURNACE);
        k.set(fur[0] + 1, g, fmz + 1, FURNACE);
        k.fill(fur[0] + 1, g + 1, fmz, fur[0] + 1, g + 6, fmz + 1, "minecraft:bricks");
        k.set(fur[0] + 1, g + 7, fmz, "minecraft:campfire[lit=false]");
        k.set(fur[0] + 1, g, fur[1] + 1, "minecraft:barrel[facing=up]");
        k.set(fur[0] + 2, g, fur[1] + 1, "minecraft:coal_block");
        k.set(fur[0] + 1, g, fur[3] - 1, "minecraft:cauldron");
        k.set(fur[2] - 1, g, fur[1] + 1, "minecraft:chest[facing=west]");
        int[] tan = k.shop(3270, 3191, 3276, 3196, 1, DESERT, "east", TANNER);
        k.set(tan[0] + 1, g + 1, tan[1] + 2, "minecraft:brown_carpet");
        k.fill(tan[0] + 1, g, tan[3] - 2, tan[0] + 1, g, tan[3] - 1, "minecraft:water_cauldron[level=3]");

        // Shops: Zeke's scimitars, the gem trader, Louie's legs, Ranael's skirts, the general store, Dommik.
        k.desertShop(3284, 3186, 3289, 3191, "south", WEAPONS, RED_WOOL);
        k.desertShop(3284, 3195, 3290, 3201, "south", "runecraft:gem_stall", "minecraft:purple_wool");
        k.desertShop(3312, 3170, 3318, 3176, "west", WEAPONS, "minecraft:yellow_wool");
        k.desertShop(3312, 3160, 3318, 3166, "west", WEAPONS, "minecraft:pink_wool");
        k.desertShop(3312, 3179, 3318, 3185, "west", GENERAL, ORANGE_WOOL);
        k.desertShop(3318, 3189, 3324, 3194, "south", CRAFTING, "minecraft:lime_wool");
        k.stall(3300, 3182, "runecraft:silk_stall", "minecraft:white_wool");
        k.stall(3305, 3182, "runecraft:spice_stall", RED_WOOL);

        // Houses.
        k.desertHouse(3293, 3186, 3298, 3191, 1, "south", ORANGE_WOOL);
        k.desertHouse(3301, 3186, 3307, 3192, 2, "south", WHITE_WOOL);
        k.desertHouse(3294, 3196, 3300, 3202, 1, "south", null);
        k.desertHouse(3302, 3196, 3307, 3201, 1, "west", ORANGE_WOOL);
        k.desertHouse(3270, 3150, 3276, 3156, 1, "east", null);
        k.desertHouse(3310, 3149, 3316, 3154, 1, "north", WHITE_WOOL);
        k.desertHouse(3320, 3160, 3326, 3166, 1, "east", null);
        k.desertHouse(3320, 3170, 3326, 3176, 2, "east", ORANGE_WOOL);
        k.desertHouse(3282, 3215, 3288, 3221, 1, "west", ORANGE_WOOL);
        k.desertHouse(3292, 3212, 3298, 3218, 1, "south", null);
        k.desertHouse(3300, 3206, 3306, 3212, 1, "south", WHITE_WOOL);
        k.desertHouse(3270, 3203, 3276, 3209, 1, "east", ORANGE_WOOL);
        k.desertHouse(3270, 3212, 3276, 3218, 2, "east", null);

        for (int[] p : new int[][]{{3280, 3200}, {3282, 3207}, {3291, 3205}, {3305, 3220}, {3316, 3212}, {3324, 3205},
                {3277, 3150}, {3308, 3170}, {3320, 3185}, {3300, 3224}, {3288, 3226}, {3310, 3230}, {3281, 3183}}) {
            k.palm(p[0], p[1], 6);
        }
        k.scatter(3296, 3222, 3326, 3233, 0, "minecraft:cactus", 14, 13);
        k.scatter(3264, 3148, 3328, 3234, 0, "minecraft:dead_bush", 30, 14);
        // Street dressing: kerbs on the high street, a signpost at the toll gate, benches by the kebab shop,
        // lamps along the streets and props in the alleys (kept off the palace, which is its own structure).
        int[] hs = r(3278, 3148, 3279, 3234);
        k.d.kerb(hs[0], hs[1], hs[2], hs[3], CUT);
        k.d.signpost(bx(3270), bz(3225), "minecraft:jungle");
        k.d.bench(bx(3277) + 1, bz(3174), false, "west", "minecraft:jungle");
        k.d.bench(bx(3277) + 1, bz(3178), false, "west", "minecraft:jungle");
        k.take(bx(3278) - 4, bz(3181) - 4, bx(3305) + 4, bz(3154) + 4);
        k.take(bx(3312), bz(3201), bx(3317), bz(3195));
        k.dressTown(3266, 3148, 3328, 3234, "desert", 45, 15);
        return k.build();
    }

    /**
     * The Emir's palace as on the OSRS map (mejrs layers_osrs, planes 0-1): a two-storey block 3282-3303 x
     * 3159-3177 inside a low perimeter wall (3281-3304 x 3158-3178). The north half holds the west room
     * (3282-3286), the east room (3299-3303) and between them an arcaded corridor round the open sand
     * courtyard with its fountain (yard 3290-3295 x 3170-3176, fountain 3292-3293 x 3171-3172); the south half
     * is one long hall (3159-3166) with the chequered floor (3290-3303 x 3161-3164), the ladder up at 3284,3165
     * (shortest-path) and the two doors south at 3290 and 3295. The courtyard stays open to the sky so the
     * Al Kharid teleport (3293, 3174) lands in it.
     */
    static StructureDef alKharidPalace() {
        EasternKit k = new EasternKit("al_kharid_palace", "Al Kharid palace");
        int g = k.g;
        int[] q = r(3282, 3159, 3303, 3177);
        int[] yd = r(3290, 3170, 3295, 3176);
        int[] mid = r(3287, 3167, 3298, 3177);
        k.clear(q[0] - 2, g, q[1] - 2, q[2] + 2, g + 16, q[3] + 2);
        k.fill(q[0], g - 1, q[1], q[2], g - 1, q[3], SMOOTH);
        // the low perimeter wall with its gates south (3290, 3295) and east (3162-3163)
        k.b.walls(bx(3281), g, bz(3178), bx(3304) + 1, g, bz(3158) + 1, "minecraft:sandstone_wall");
        k.clear(bx(3290), g, bz(3158) + 1, bx(3290) + 1, g, bz(3158) + 1);
        k.clear(bx(3295), g, bz(3158) + 1, bx(3295) + 1, g, bz(3158) + 1);
        k.clear(bx(3304) + 1, g, bz(3163), bx(3304) + 1, g, bz(3162) + 1);
        k.fill(bx(3281) + 1, g - 1, bz(3178) + 1, bx(3304), g - 1, bz(3158), CUT);
        k.fill(q[0], g - 1, q[1], q[2], g - 1, q[3], SMOOTH);
        // shell: two storeys, a floor at g+4, the roof at g+9 and crenels
        k.b.walls(q[0], g, q[1], q[2], g + 9, q[3], CUT);
        k.b.walls(q[0], g, q[1], q[2], g, q[3], CHIS);
        k.b.walls(q[0], g + 4, q[1], q[2], g + 4, q[3], CHIS);
        k.fill(q[0] + 1, g + 4, q[1] + 1, q[2] - 1, g + 4, q[3] - 1, SMOOTH);
        k.fill(q[0] + 1, g + 9, q[1] + 1, q[2] - 1, g + 9, q[3] - 1, SMOOTH);
        k.crenels(q, g + 10, CUT);
        k.slits(q, g + 1, 4, AIR);
        k.slits(q, g + 6, 4, AIR);
        // the wall between the north rooms and the south hall (both storeys), doors at 3292-3293
        int wz = bz(3167) + 1;
        k.fill(q[0] + 1, g, wz, q[2] - 1, g + 8, wz, CUT);
        k.fill(q[0] + 1, g + 4, wz, q[2] - 1, g + 4, wz, CHIS);
        k.clear(bx(3292), g, wz, bx(3293) + 1, g + 2, wz);
        k.clear(bx(3292), g + 5, wz, bx(3293) + 1, g + 7, wz);
        // the west and east rooms' walls with their doors into the corridor at 3171-3172
        for (int x : new int[]{mid[0], mid[2]}) {
            k.fill(x, g, q[1] + 1, x, g + 8, wz - 1, CUT);
            k.fill(x, g + 4, q[1] + 1, x, g + 4, wz - 1, CHIS);
            k.clear(x, g, bz(3172), x, g + 2, bz(3171) + 1);
            k.clear(x, g + 5, bz(3172), x, g + 7, bz(3171) + 1);
        }
        // the courtyard: open to the sky, sand and the fountain, an arcade of columns round it and a
        // balustrade round the opening upstairs
        k.clear(yd[0], g + 4, yd[1], yd[2], g + 16, yd[3]);
        k.fill(yd[0], g - 1, yd[1], yd[2], g - 1, yd[3], SAND);
        for (int x = yd[0]; x <= yd[2]; x += 3) {
            k.pillar(x, yd[1], 4, CHIS, null);
            k.pillar(x, yd[3], 4, CHIS, null);
        }
        for (int z = yd[1] + 3; z < yd[3]; z += 3) {
            k.pillar(yd[0], z, 4, CHIS, null);
            k.pillar(yd[2], z, 4, CHIS, null);
        }
        k.b.walls(yd[0] - 1, g + 4, yd[1] - 1, yd[2] + 1, g + 4, yd[3] + 1, CHIS);
        k.b.walls(yd[0] - 1, g + 5, yd[1] - 1, yd[2] + 1, g + 5, yd[3] + 1, "minecraft:sandstone_wall");
        k.fountain(bx(3293), bz(3172), 1, CHIS, CHIS);
        // potted palms in the yard's corners (no trees: the Al Kharid teleport lands in this yard)
        for (int[] c : new int[][]{{yd[0] + 1, yd[1] + 1}, {yd[2] - 1, yd[1] + 1}, {yd[0] + 1, yd[3] - 1}, {yd[2] - 1, yd[3] - 1}}) {
            k.set(c[0], g, c[1], "minecraft:potted_bamboo");
        }
        // doors: the two south doors (3290, 3295) with a portico, the east door at 3162-3163
        for (int tx : new int[]{3290, 3295}) {
            k.clear(bx(tx), g, q[3], bx(tx) + 1, g + 2, q[3]);
            k.fill(bx(tx), g - 1, q[3], bx(tx) + 1, g - 1, bz(3158) + 1, "minecraft:red_terracotta");
        }
        k.clear(q[2], g, bz(3163), q[2], g + 2, bz(3162) + 1);
        // the south hall: chequered floor, the Emir's throne at its east end, banners and lanterns
        for (int tx = 3290; tx <= 3303; tx++) {
            for (int ty = 3161; ty <= 3164; ty++) {
                k.fill(bx(tx), g - 1, bz(ty), bx(tx) + 1, g - 1, bz(ty) + 1, ((tx + ty) & 1) == 0 ? CHIS : "minecraft:polished_blackstone");
            }
        }
        int tx0 = q[2] - 1;
        int tz = bz(3163);
        k.fill(tx0 - 1, g, tz - 1, tx0, g, tz + 2, CHIS);
        k.fill(tx0 - 1, g + 1, tz, tx0 - 1, g + 1, tz + 1, "minecraft:quartz_stairs[facing=west]");
        k.fill(tx0, g + 1, tz, tx0, g + 3, tz + 1, GOLD);
        k.fill(bx(3292), g, wz + 1, bx(3293) + 1, g, q[3] - 1, "minecraft:red_carpet");
        for (int x = q[0] + 3; x < q[2] - 2; x += 6) {
            k.set(x, g + 2, q[3] - 1, "minecraft:orange_wall_banner[facing=north]");
            k.set(x, g + 2, bz(3164), "minecraft:lantern[hanging=true]");
        }
        // the ladder up in the hall's west end (OSRS 3284, 3164-3165)
        k.fill(q[0] + 1, g, bz(3165), q[0] + 1, g + 4, bz(3165), "minecraft:ladder[facing=east]");
        k.clear(q[0] + 1, g + 5, bz(3165), q[0] + 1, g + 6, bz(3165));
        // west room: kitchen; east room: barracks
        int ww = q[0] + 1;
        k.set(ww, g, bz(3170), RANGE);
        k.set(ww, g, bz(3171), "minecraft:smoker[facing=east]");
        k.set(ww, g, bz(3173), "minecraft:barrel[facing=up]");
        k.set(ww, g, bz(3174), "minecraft:barrel[facing=up]");
        k.set(ww, g, bz(3176), "minecraft:crafting_table");
        k.fill(ww + 3, g, bz(3174), ww + 5, g, bz(3174), "minecraft:jungle_slab[type=top]");
        k.set(ww + 4, g + 1, bz(3174), "minecraft:cake");
        int ew = q[2] - 1;
        for (int z = bz(3176); z <= bz(3168); z += 3) {
            k.set(ew - 1, g, z, "minecraft:red_bed[facing=east,part=foot]");
            k.set(ew - 2, g, z, "minecraft:chest[facing=west]");
        }
        k.set(ew - 6, g, bz(3175), "minecraft:grindstone[face=floor,facing=east]");
        k.set(ew - 6, g, bz(3169), "minecraft:smithing_table");
        // upstairs: the Emir's chambers over the hall, a library over the west room
        k.fill(bx(3286), g + 5, bz(3164), bx(3300), g + 5, bz(3161), "minecraft:red_carpet");
        k.set(bx(3300), g + 5, bz(3163), "minecraft:red_bed[facing=west,part=foot]");
        k.fill(q[0] + 1, g + 5, bz(3176), q[0] + 1, g + 6, bz(3170), "minecraft:bookshelf");
        for (int x = q[0] + 4; x < q[2] - 2; x += 8) {
            k.set(x, g + 7, bz(3163), "minecraft:lantern[hanging=true]");
            k.set(x, g + 2, bz(3175), "minecraft:lantern[hanging=true]");
        }
        palaceFacade(k, q);
        k.palm(3291, 3182, 6);
        k.palm(3295, 3182, 6);
        return k.build();
    }

    /** Pilasters, a terracotta string course, banners, window sills and roof-terrace kiosks for the palace. */
    private static void palaceFacade(EasternKit k, int[] q) {
        int g = k.g;
        k.d.ringReplace(q, g + 8, g + 8, CUT, ORANGE_TERRA);
        k.d.ringReplace(q, g + 5, g + 5, CUT, SMOOTH);
        for (int x = q[0] + 5; x < q[2] - 4; x += 6) {
            k.fill(x, g, q[1] - 1, x, g + 9, q[1] - 1, SMOOTH);
            k.set(x, g + 10, q[1] - 1, CHIS);
            if (Math.abs(x - bx(3290)) > 1 && Math.abs(x - bx(3295)) > 1) {
                k.fill(x, g, q[3] + 1, x, g + 9, q[3] + 1, SMOOTH);
                k.set(x, g + 10, q[3] + 1, CHIS);
            }
        }
        for (int z = q[1] + 5; z < q[3] - 4; z += 6) {
            k.fill(q[0] - 1, g, z, q[0] - 1, g + 9, z, SMOOTH);
            k.set(q[0] - 1, g + 10, z, CHIS);
            if (Math.abs(z - bz(3162)) > 2) {
                k.fill(q[2] + 1, g, z, q[2] + 1, g + 9, z, SMOOTH);
                k.set(q[2] + 1, g + 10, z, CHIS);
            }
        }
        // banners of the Emir either side of the south doors
        for (int x : new int[]{bx(3289), bx(3291) + 1, bx(3294), bx(3296) + 1}) {
            k.set(x, g + 3, q[3] + 1, "minecraft:orange_wall_banner[facing=south]");
        }
        // striped canopies over the two south doors
        for (int tx : new int[]{3290, 3295}) {
            k.d.awning(bx(tx) - 1, q[3] + 1, bx(tx) + 2, q[3] + 2, g + 3, "minecraft:orange_wool", "minecraft:white_wool", true);
        }
        // roof terraces: kiosks with little domes over the east and west rooms, potted plants
        int mz = bz(3172);
        for (int kx : new int[]{q[0] + 4, q[2] - 4}) {
            for (int[] p : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
                k.fill(kx + p[0], g + 10, mz + p[1], kx + p[0], g + 11, mz + p[1], "minecraft:sandstone_wall");
            }
            k.fill(kx - 1, g + 12, mz - 1, kx + 1, g + 12, mz + 1, CUT);
            k.d.stairDome(kx, mz, g + 13, 1.6, SMOOTH, SMOOTH_STAIRS, GOLD);
            k.set(kx, g + 10, mz, "minecraft:orange_carpet");
            k.set(kx, g + 11, mz, "minecraft:lantern[hanging=true]");
        }
        for (int x = q[0] + 4; x < q[2] - 3; x += 7) {
            k.set(x, g + 10, q[3] - 2, "minecraft:potted_dead_bush");
        }
    }

    /** The fenced sand bed of the Al Kharid cactus patch. */
    static StructureDef alKharidCactusGarden() {
        EasternKit k = new EasternKit("al_kharid_cactus_garden", "Al Kharid cactus patch").replaces("al_kharid_cactus");
        int g = k.g;
        int x = bx(3314);
        int z = bz(3198);
        k.fill(x - 2, g - 1, z - 2, x + 2, g - 1, z + 2, SAND);
        k.b.walls(x - 3, g, z - 3, x + 3, g, z + 3, "minecraft:oak_fence");
        k.set(x, g, z + 3, "minecraft:oak_fence_gate[facing=south]");
        k.set(x, g, z, "runecraft:cactus_patch");
        k.set(x - 2, g, z - 2, "minecraft:cactus");
        k.set(x + 2, g, z - 2, "minecraft:cactus");
        k.set(x + 4, g, z + 4, "runecraft:compost_bin");
        return k.build();
    }

    // ------------------------------------------------------------------ PvP Arena

    /** The Duel Arena / PvP Arena: six walled combat pits, a raised viewing gallery and the arena lobby. */
    static StructureDef pvpArena() {
        EasternKit k = new EasternKit("pvp_arena", "PvP Arena");
        int g = k.g;
        k.flat(3331, 3198, 3390, 3290, SAND);
        k.wall(3331, 3198, 3390, 3198, 3, CUT, null);
        k.wall(3331, 3290, 3390, 3290, 3, CUT, null);
        k.wall(3331, 3198, 3331, 3290, 3, CUT, null);
        k.wall(3390, 3198, 3390, 3290, 3, CUT, null);
        k.tfill(3331, 3198, 3390, 3198, 3, 3, SS_WALL);
        k.tfill(3331, 3290, 3390, 3290, 3, 3, SS_WALL);
        k.tfill(3331, 3198, 3331, 3290, 3, 3, SS_WALL);
        k.tfill(3390, 3198, 3390, 3290, 3, 3, SS_WALL);
        k.gate(3331, 3240, 3331, 3243, 4);
        k.tClearTop(3331, 3240, 3331, 3243, 3);
        k.gate(3358, 3290, 3361, 3290, 4);
        k.tClearTop(3358, 3290, 3361, 3290, 3);
        k.floor(3331, 3240, 3357, 3243, SMOOTH);
        k.floor(3358, 3255, 3361, 3289, SMOOTH);
        for (int[] t : new int[][]{{3331, 3198}, {3390, 3198}, {3331, 3290}, {3390, 3290}}) {
            k.tower(t[0], t[1], 3, 8, CUT, SMOOTH, null, null);
            k.dome(cx(t[0]), cz(t[1]), g + 8, 4.0, 3, SMOOTH, SMOOTH);
        }
        // Six arenas (three with obstacles).
        int n = 0;
        for (int[] col : new int[][]{{3333, 3355}, {3364, 3387}}) {
            for (int[] row : new int[][]{{3202, 3216}, {3220, 3234}, {3238, 3252}}) {
                arena(k, col[0], row[0], col[1], row[1], n++ % 2 == 1);
            }
        }
        // Raised viewing gallery between the two columns of arenas.
        k.tfill(3357, 3201, 3361, 3253, 0, 1, SMOOTH);
        k.tfill(3357, 3201, 3357, 3253, 2, 2, SS_WALL);
        k.tfill(3361, 3201, 3361, 3253, 2, 2, SS_WALL);
        int gz = bz(3253);
        int[] gr = r(3358, 3253, 3360, 3253);
        k.fill(gr[0], g, gz - 1, gr[2], g, gz - 1, SS);
        k.fill(gr[0], g + 1, gz - 1, gr[2], g + 1, gz - 1, SS_STAIRS + "[facing=south]");
        k.fill(gr[0], g, gz - 2, gr[2], g, gz - 2, SS_STAIRS + "[facing=south]");
        k.clear(gr[0], g + 2, gz, gr[2], g + 2, gz + 1);
        for (int z = bz(3250); z <= bz(3204); z += 6) {
            k.fill(gr[0], g + 2, z, gr[2], g + 2, z, "minecraft:smooth_sandstone_stairs[facing=north]");
        }
        k.stall(3348, 3259 - 4, "runecraft:spice_stall", ORANGE_WOOL);
        k.stall(3370, 3259 - 4, "runecraft:fish_stall", WHITE_WOOL);
        // Arena lobby: two storeys, bank booths, altar, hospital beds, scoreboard.
        int[] l = k.hall(3342, 3262, 3378, 3284, 2, DESERT_RICH, "north");
        int lmx = (l[0] + l[2]) / 2;
        k.boothsX(lmx - 9, lmx + 9, l[3] - 3, g, CUT);
        k.set(l[0] + 3, g, (l[1] + l[3]) / 2, ALTAR);
        k.fill(l[0] + 2, g, (l[1] + l[3]) / 2 - 1, l[0] + 4, g, (l[1] + l[3]) / 2 + 1, "minecraft:red_carpet");
        k.set(l[0] + 3, g, (l[1] + l[3]) / 2, ALTAR);
        for (int z = l[1] + 3; z <= l[3] - 3; z += 3) {
            k.set(l[2] - 1, g, z, "minecraft:white_carpet");
            k.set(l[2] - 2, g, z, "minecraft:red_carpet");
            k.set(l[2] - 3, g, z, "minecraft:red_carpet");
        }
        for (int x = l[0] + 8; x < l[2] - 6; x += 8) {
            for (int z = l[1] + 6; z < l[3] - 5; z += 8) {
                k.pillar(x, z, 3, CHIS, null);
            }
        }
        k.fill(lmx - 6, g + 4, l[1] - 1, lmx + 6, g + 6, l[1] - 1, "minecraft:black_concrete");
        k.fill(lmx - 5, g + 5, l[1] - 1, lmx + 5, g + 5, l[1] - 1, "minecraft:yellow_concrete");
        k.fill(lmx - 3, g + 4, l[1] + 2, lmx + 3, g + 4, l[1] + 6, "minecraft:orange_carpet");
        k.dome(lmx, (l[1] + l[3]) / 2, g + 8, 7.5, 5, SMOOTH, ORANGE_TERRA);
        k.fountain(bx(3352), bz(3258), 2, CHIS, CHIS);
        k.fountain(bx(3368), bz(3258), 2, CHIS, CHIS);
        k.palm(3346, 3288, 6);
        k.palm(3374, 3288, 6);
        k.palm(3334, 3260, 6);
        k.palm(3386, 3260, 6);
        return k.build();
    }

    private static void arena(EasternKit k, int x1, int y1, int x2, int y2, boolean obstacles) {
        int g = k.g;
        int[] q = r(x1, y1, x2, y2);
        k.b.walls(q[0], g - 1, q[1], q[2], g + 2, q[3], CUT);
        k.set(q[0], g + 3, q[1], CHIS);
        k.set(q[2], g + 3, q[1], CHIS);
        k.set(q[0], g + 3, q[3], CHIS);
        k.set(q[2], g + 3, q[3], CHIS);
        k.fill(q[0] + 1, g - 1, q[1] + 1, q[2] - 1, g - 1, q[3] - 1, SAND);
        k.b.walls(q[0] + 1, g - 1, q[1] + 1, q[2] - 1, g - 1, q[3] - 1, SMOOTH);
        int mz = (q[1] + q[3]) / 2;
        k.set(q[0], g, mz, "minecraft:oak_fence_gate[facing=east]");
        k.set(q[0], g + 1, mz, AIR);
        if (obstacles) {
            for (int x = q[0] + 6; x < q[2] - 4; x += 8) {
                for (int z = q[1] + 5; z < q[3] - 3; z += 8) {
                    k.fill(x, g, z, x + 1, g + 1, z + 1, SS);
                }
            }
            k.fill(q[0] + 10, g, mz - 3, q[0] + 10, g + 1, mz + 3, SS);
            k.fill(q[2] - 10, g, mz - 3, q[2] - 10, g + 1, mz + 3, SS);
        }
    }

    // ------------------------------------------------------------------ Shantay Pass & desert edge

    /** Shantay Pass: the wall across the desert with its gate building, Shantay's hut, and the jail. */
    static StructureDef shantayPass() {
        EasternKit k = new EasternKit("shantay_pass", "Shantay Pass").replaces("shantay_pass");
        int g = k.g;
        k.flat(3276, 3106, 3334, 3132, SAND);
        k.wall(3278, 3116, 3332, 3116, 5, SS, CUT);
        k.tfill(3278, 3116, 3332, 3116, 2, 2, CUT);
        int[] gate = k.hall(3300, 3113, 3308, 3119, 1, DESERT, null);
        k.b.tClear(3303, 3113, 3305, 3119, 0, 2);
        k.tfill(3302, 3114, 3302, 3118, 0, 3, CUT);
        k.tfill(3306, 3114, 3306, 3118, 0, 3, CUT);
        k.tfill(3303, 3113, 3305, 3113, 3, 3, CHIS);
        k.tfill(3303, 3119, 3305, 3119, 3, 3, CHIS);
        k.floor(3303, 3106, 3305, 3132, SMOOTH);
        k.set(gate[0] + 1, g, gate[1] + 2, "minecraft:chest[facing=east]");
        k.set(gate[2] - 1, g, gate[1] + 2, "minecraft:barrel[facing=up]");
        k.tower(3300, 3116, 2, 7, CUT, SMOOTH, null, null);
        k.tower(3308, 3116, 2, 7, CUT, SMOOTH, null, null);
        // Shantay's hut: shop counter and the bank chest.
        int[] hut = k.shop(3294, 3121, 3301, 3127, 1, DESERT, "south", GENERAL);
        k.set(hut[2] - 1, g, hut[1] + 2, BANK);
        k.awning(hut, "south", ORANGE_WOOL);
        // The jail.
        int[] jail = k.hall(3308, 3121, 3313, 3125, 1, DESERT, "south");
        int jmx = (jail[0] + jail[2]) / 2;
        k.fill(jmx, g, jail[3], jmx + 1, g + 1, jail[3], "minecraft:iron_bars");
        for (int y : new int[]{g + 1}) {
            k.b.replace(jail[0], y, jail[1], jail[2], y, jail[1], AIR, "minecraft:iron_bars");
            k.b.replace(jail[0], y, jail[3], jail[2], y, jail[3], AIR, "minecraft:iron_bars");
            k.b.replace(jail[0], y, jail[1], jail[0], y, jail[3], AIR, "minecraft:iron_bars");
            k.b.replace(jail[2], y, jail[1], jail[2], y, jail[3], AIR, "minecraft:iron_bars");
        }
        k.set(jail[0] + 1, g, jail[1] + 1, "minecraft:hay_block");
        k.set(jail[2] - 1, g, jail[1] + 1, "minecraft:cauldron");
        k.palm(3292, 3130, 6);
        k.palm(3316, 3128, 6);
        k.palm(3286, 3110, 5);
        k.palm(3322, 3109, 5);
        k.scatter(3278, 3106, 3332, 3114, 0, "minecraft:cactus", 10, 31);
        k.scatter(3278, 3106, 3332, 3132, 0, "minecraft:dead_bush", 14, 32);
        // Warning signs, a water trough for travellers and crates of desert gear outside the hut.
        k.d.signpost(bx(3302), bz(3110), "minecraft:jungle");
        k.d.signpost(bx(3306) + 1, bz(3121), "minecraft:jungle");
        k.fill(bx(3297), g, bz(3120), bx(3299), g, bz(3120), "minecraft:water_cauldron[level=3]");
        k.d.crates(bx(3292), bz(3124), 33);
        k.dressTown(3278, 3106, 3332, 3132, "desert", 10, 34);
        return k.build();
    }

    /** The Kalphite Lair entrance: a sand mound with a crevice and a rope down into the dark. */
    static StructureDef kalphiteLair() {
        EasternKit k = new EasternKit("kalphite_lair_entrance", "Kalphite Lair entrance").replaces("kalphite_lair");
        int g = k.g;
        k.flat(3220, 3101, 3234, 3115, SAND);
        int x = cx(3227);
        int z = cz(3108);
        k.dome(x, z, g, 7.5, 3, "minecraft:sandstone", SAND);
        k.ring(x, g - 1, g + 3, z, 3.5, 2.0, "minecraft:sandstone");
        for (int y = g - 8; y <= g + 4; y++) {
            k.disc(x, y, z, 2.0, AIR);
        }
        k.disc(x, g - 9, z, 2.5, "minecraft:soul_sand");
        k.fill(x - 3, g + 1, z, x - 3, g + 3, z, "minecraft:oak_fence");
        k.fill(x + 3, g + 1, z, x + 3, g + 3, z, "minecraft:oak_fence");
        k.fill(x - 3, g + 4, z, x + 3, g + 4, z, "minecraft:oak_log[axis=x]");
        k.fill(x, g - 7, z, x, g + 3, z, "minecraft:chain");
        k.set(x + 1, g - 8, z + 1, COBWEB);
        k.set(x - 1, g - 8, z - 1, "minecraft:bone_block");
        k.scatter(3221, 3102, 3233, 3114, 0, "minecraft:dead_bush", 6, 41);
        return k.build();
    }

    /** The Desert Mining Camp: palisade, guard tents, the mine entrance and the slaves' cell. */
    static StructureDef desertMiningCamp() {
        EasternKit k = new EasternKit("desert_mining_camp", "Desert Mining Camp").replaces("desert_mining_camp");
        int g = k.g;
        k.flat(3270, 3010, 3296, 3034, SAND);
        String log = "minecraft:spruce_log";
        k.wall(3272, 3012, 3294, 3012, 3, log, null);
        k.wall(3272, 3032, 3294, 3032, 3, log, null);
        k.wall(3272, 3012, 3272, 3032, 3, log, null);
        k.wall(3294, 3012, 3294, 3032, 3, log, null);
        k.tfill(3272, 3012, 3294, 3012, 3, 3, "minecraft:spruce_fence");
        k.tfill(3272, 3032, 3294, 3032, 3, 3, "minecraft:spruce_fence");
        k.gate(3282, 3012, 3284, 3012, 4);
        k.tClearTop(3282, 3012, 3284, 3012, 3);
        k.tower(3272, 3032, 2, 6, log, "minecraft:spruce_planks", "south", null);
        k.tower(3294, 3032, 2, 6, log, "minecraft:spruce_planks", "south", null);
        int[] gh = k.house(3274, 3014, 3280, 3020, 1, DESERT_MUD, "east");
        k.set(gh[0] + 1, g, gh[1] + 2, WEAPONS);
        k.tentT(3286, 3014, 3289, 3019, "minecraft:brown_wool");
        k.tentT(3274, 3024, 3277, 3029, WHITE_WOOL);
        // The mine: a rocky hill with an opening and rails leading in.
        int mx = cx(3288);
        int mz = cz(3027);
        k.dome(mx, mz, g, 8.5, 6, "minecraft:stone", "minecraft:sandstone");
        k.clear(mx - 1, g, mz, mx + 1, g + 2, mz + 9);
        k.clear(mx - 3, g, mz - 4, mx + 3, g + 3, mz + 1);
        k.fill(mx, g, mz - 3, mx, g, mz + 12, "minecraft:rail");
        k.fill(mx - 1, g + 3, mz + 3, mx + 1, g + 3, mz + 3, "minecraft:spruce_log[axis=x]");
        k.fill(mx - 1, g, mz + 3, mx - 1, g + 2, mz + 3, "minecraft:spruce_fence");
        k.fill(mx + 1, g, mz + 3, mx + 1, g + 2, mz + 3, "minecraft:spruce_fence");
        k.set(mx - 2, g, mz - 2, "runecraft:coal_rock");
        k.set(mx + 2, g, mz - 2, "runecraft:sandstone_rock");
        k.set(mx, g + 2, mz - 1, "minecraft:lantern[hanging=true]");
        // Slaves' cell.
        int[] cell = k.r(3279, 3024, 3283, 3029);
        k.b.walls(cell[0], g, cell[1], cell[2], g + 2, cell[3], "minecraft:iron_bars");
        k.fill(cell[0], g + 3, cell[1], cell[2], g + 3, cell[3], "minecraft:spruce_slab");
        k.set(cell[0] + 2, g, cell[1] + 2, "minecraft:hay_block");
        k.campfire(bx(3284), bz(3020));
        return k.build();
    }

    // ------------------------------------------------------------------ Pollnivneach

    /** Pollnivneach: central square and well, bar, general store, kebab house, the mayor's house. */
    static StructureDef pollnivneach() {
        EasternKit k = new EasternKit("pollnivneach", "Pollnivneach").replaces("POLLNIVNEACH");
        int g = k.g;
        k.flat(3334, 2938, 3384, 3002, SAND);
        k.patches(3336, 2940, 3382, 3000, "minecraft:sandstone", 30, 51);
        k.floor(3358, 2938, 3360, 3002, SMOOTH);
        k.floor(3334, 2970, 3384, 2971, SMOOTH);
        k.floor(3355, 2965, 3363, 2977, CUT);
        k.well(cx(3359), bz(2971), SS, "minecraft:smooth_sandstone_slab");
        // East side.
        int[] bar = k.hall(3362, 2944, 3370, 2953, 1, DESERT, "north");
        k.counterX(bar[0] + 2, bar[2] - 6, bar[3] - 3, g, "minecraft:jungle_slab[type=top]", null);
        k.fill(bar[0] + 1, g, bar[3] - 1, bar[2] - 1, g, bar[3] - 1, "minecraft:barrel[facing=up]");
        for (int x = bar[0] + 3; x < bar[2] - 2; x += 5) {
            k.set(x, g, bar[1] + 4, "minecraft:jungle_slab[type=top]");
            k.set(x - 1, g, bar[1] + 4, "minecraft:jungle_stairs[facing=west]");
            k.set(x + 1, g, bar[1] + 4, "minecraft:jungle_stairs[facing=east]");
        }
        k.awning(bar, "north", "minecraft:brown_wool");
        int[] ali = k.house(3373, 2950, 3381, 2958, 2, DESERT_RICH, "west");
        k.set(ali[2] - 2, g + 4, ali[1] + 2, "minecraft:jungle_slab[type=top]");
        k.desertShop(3364, 2976, 3370, 2982, "west", RANGE, ORANGE_WOOL);
        k.desertHouse(3372, 2976, 3378, 2982, 1, "west", null);
        k.desertHouse(3364, 2986, 3370, 2992, 2, "south", WHITE_WOOL);
        k.desertHouse(3372, 2986, 3378, 2992, 1, "west", ORANGE_WOOL);
        k.desertHouse(3363, 2994, 3369, 2999, 1, "west", null);
        // West side.
        k.desertShop(3347, 2976, 3353, 2982, "east", GENERAL, ORANGE_WOOL);
        k.desertHouse(3339, 2976, 3345, 2982, 1, "north", null);
        k.desertHouse(3339, 2960, 3345, 2966, 2, "east", RED_WOOL);
        k.desertHouse(3347, 2960, 3353, 2966, 1, "east", ORANGE_WOOL);
        k.desertHouse(3339, 2948, 3345, 2954, 1, "east", null);
        k.desertHouse(3347, 2946, 3353, 2954, 1, "east", WHITE_WOOL);
        k.desertHouse(3340, 2986, 3346, 2992, 1, "south", null);
        k.desertHouse(3348, 2986, 3354, 2992, 2, "east", ORANGE_WOOL);
        k.desertHouse(3372, 2940, 3378, 2946, 1, "north", null);
        k.desertHouse(3340, 2940, 3346, 2945, 1, "north", ORANGE_WOOL);
        // Market stalls on the square.
        k.stall(3355, 2979, "runecraft:silk_stall", RED_WOOL);
        k.stall(3361, 2979, "runecraft:spice_stall", ORANGE_WOOL);
        k.stall(3355, 2961, "runecraft:fish_stall", WHITE_WOOL);
        for (int[] p : new int[][]{{3356, 2966}, {3363, 2966}, {3356, 2976}, {3363, 2975}, {3337, 2970}, {3382, 2972},
                {3359, 2940}, {3359, 3000}}) {
            k.palm(p[0], p[1], 6);
        }
        k.scatter(3334, 2938, 3384, 3002, 0, "minecraft:dead_bush", 20, 52);
        // Kerbed main street, benches round the well, the bar's outdoor tables and the market lamps
        // (kept off the Source of the Elid cave mouth, which is its own structure).
        int[] ms = r(3358, 2938, 3360, 3002);
        k.d.kerb(ms[0], ms[1], ms[2], ms[3], CUT);
        k.d.bench(cx(3359) - 4, bz(2969), false, "east", "minecraft:jungle");
        k.d.bench(cx(3359) + 4, bz(2969), false, "west", "minecraft:jungle");
        for (int x = bar[0] + 2; x < bar[2] - 2; x += 4) {
            k.set(x, g, bar[1] - 3, "minecraft:jungle_slab[type=top]");
            k.set(x, g + 1, bar[1] - 3, "minecraft:candle[candles=2,lit=true]");
            k.set(x - 1, g, bar[1] - 3, "minecraft:jungle_stairs[facing=east]");
        }
        k.take(bx(3366), bz(2972), bx(3375), bz(2961));
        k.take(bar[0], bar[1] - 4, bar[2], bar[1]);
        k.dressTown(3334, 2938, 3384, 3002, "desert", 30, 53);
        return k.build();
    }

    /** The rocky cave mouth leading down to the Source of the Elid, east of Pollnivneach's square. */
    static StructureDef elidCave() {
        EasternKit k = new EasternKit("elid_cave_entrance", "Source of the Elid entrance").replaces("elid_cave");
        int g = k.g;
        int x = cx(3370);
        int z = cz(2966);
        k.dome(x, z, g, 5.5, 4, "minecraft:sandstone", "minecraft:smooth_sandstone");
        k.clear(x - 1, g, z - 1, x + 1, g + 2, z + 6);
        k.clear(x - 1, g - 4, z - 1, x + 1, g - 1, z + 1);
        k.fill(x - 1, g - 5, z - 1, x + 1, g - 5, z + 1, "minecraft:sandstone");
        k.fill(x - 1, g - 4, z - 2, x + 1, g - 1, z - 2, "minecraft:sandstone");
        k.fill(x, g - 4, z - 1, x, g - 1, z - 1, "minecraft:ladder[facing=south]");
        k.set(x, g + 2, z + 1, "minecraft:lantern[hanging=true]");
        k.fill(x - 1, g - 1, z + 2, x + 1, g - 1, z + 6, CUT);
        return k.build();
    }

    // ------------------------------------------------------------------ Nardah

    /** Nardah: walled town with the Elidinis shrine, the fountain plaza, bank and shops. */
    static StructureDef nardah() {
        EasternKit k = new EasternKit("nardah", "Nardah").replaces("NARDAH");
        int g = k.g;
        k.flat(3396, 2876, 3450, 2926, SAND);
        k.wall(3398, 2878, 3448, 2878, 5, SS, CUT);
        k.wall(3398, 2924, 3448, 2924, 5, SS, CUT);
        k.wall(3398, 2878, 3398, 2924, 5, SS, CUT);
        k.wall(3448, 2878, 3448, 2924, 5, SS, CUT);
        k.gate(3398, 2896, 3398, 2899, 5);
        k.gate(3448, 2896, 3448, 2899, 5);
        for (int[] t : new int[][]{{3398, 2878}, {3448, 2878}, {3398, 2924}, {3448, 2924}, {3398, 2894}, {3398, 2901},
                {3448, 2894}, {3448, 2901}}) {
            k.tower(t[0], t[1], 2, 8, CUT, SMOOTH, null, null);
        }
        k.floor(3399, 2896, 3447, 2899, SMOOTH);
        k.floor(3416, 2889, 3436, 2905, CUT);
        k.floor(3418, 2891, 3434, 2903, SMOOTH);
        // The fountain of Elidinis.
        int fx = cx(3426);
        int fz = cz(2897);
        k.fountain(fx, fz, 4, CHIS, CHIS);
        for (int[] d : new int[][]{{-6, -6}, {6, -6}, {-6, 6}, {6, 6}}) {
            k.pillar(fx + d[0], fz + d[1], 4, CHIS, "minecraft:lantern");
        }
        // Shrine of Elidinis.
        int[] sh = k.hall(3418, 2909, 3434, 2920, 2, DESERT_RICH, "south");
        k.openUp(sh, 2);
        int smx = (sh[0] + sh[2]) / 2;
        for (int z = sh[1] + 3; z < sh[3] - 2; z += 4) {
            k.fill(sh[0] + 4, g, z, sh[0] + 4, g + 6, z, CHIS);
            k.fill(sh[2] - 4, g, z, sh[2] - 4, g + 6, z, CHIS);
        }
        k.fill(sh[0] + 2, g - 1, sh[1] + 2, sh[0] + 2, g - 1, sh[3] - 3, WATER);
        k.fill(sh[2] - 2, g - 1, sh[1] + 2, sh[2] - 2, g - 1, sh[3] - 3, WATER);
        k.fill(smx - 3, g, sh[1] + 1, smx + 3, g, sh[1] + 4, CHIS);
        k.set(smx, g + 1, sh[1] + 4, ALTAR);
        k.fill(smx, g + 1, sh[1] + 1, smx + 1, g + 5, sh[1] + 2, SMOOTH);
        k.fill(smx, g + 6, sh[1] + 1, smx + 1, g + 7, sh[1] + 2, CHIS);
        k.set(smx - 1, g + 4, sh[1] + 2, "minecraft:smooth_sandstone_slab[type=top]");
        k.set(smx + 2, g + 4, sh[1] + 2, "minecraft:smooth_sandstone_slab[type=top]");
        k.fill(smx, g + 1, sh[1] + 3, smx + 1, g + 1, sh[1] + 3, "minecraft:light_blue_stained_glass");
        k.fill(smx, g, sh[1] + 5, smx + 1, g, sh[3] - 1, "minecraft:light_blue_carpet");
        for (int x = sh[0] + 2; x <= sh[2]; x += 4) {
            k.fill(x, g, sh[3] + 2, x, g + 7, sh[3] + 2, CHIS);
        }
        k.fill(sh[0], g + 8, sh[3] + 1, sh[2], g + 8, sh[3] + 2, CUT);
        k.dome(smx, (sh[1] + sh[3]) / 2, g + 8, 6.5, 5, SMOOTH, "minecraft:light_blue_terracotta");
        k.set(smx, g + 13, (sh[1] + sh[3]) / 2, GOLD);
        // Bank, shops, houses.
        int[] bank = k.hall(3420, 2880, 3432, 2886, 1, DESERT_RICH, "north");
        k.boothsX(bank[0] + 3, bank[2] - 3, bank[3] - 3, g, CUT);
        k.awning(bank, "north", ORANGE_WOOL);
        k.desertShop(3404, 2901, 3411, 2907, "east", GENERAL, ORANGE_WOOL);
        k.desertShop(3404, 2910, 3411, 2916, "east", "runecraft:cake_stall", "minecraft:brown_wool");
        k.desertShop(3438, 2901, 3445, 2907, "west", WEAPONS, RED_WOOL);
        k.desertShop(3438, 2910, 3445, 2916, "west", "runecraft:apothecary_table", "minecraft:lime_wool");
        k.desertHouse(3404, 2887, 3411, 2893, 2, "east", WHITE_WOOL);
        k.desertHouse(3438, 2887, 3445, 2893, 1, "west", ORANGE_WOOL);
        k.desertHouse(3402, 2918, 3409, 2922, 1, "east", null);
        k.desertHouse(3438, 2918, 3445, 2922, 1, "west", null);
        k.desertHouse(3402, 2880, 3408, 2884, 1, "north", null);
        k.desertHouse(3438, 2880, 3445, 2884, 1, "north", ORANGE_WOOL);
        for (int[] p : new int[][]{{3416, 2889}, {3436, 2889}, {3416, 2905}, {3436, 2905}, {3413, 2897}, {3440, 2897}}) {
            k.palm(p[0], p[1], 6);
        }
        // Benches and planters round the fountain plaza, blue banners of Elidinis on the shrine portico.
        for (int[] bch : new int[][]{{-8, -3}, {-8, 2}, {8, -3}, {8, 2}}) {
            k.d.bench(fx + bch[0], fz + bch[1], false, bch[0] < 0 ? "east" : "west", "minecraft:jungle");
        }
        for (int x = sh[0] + 4; x <= sh[2] - 4; x += 8) {
            k.set(x, g + 6, sh[3] + 3, "minecraft:light_blue_wall_banner[facing=south]");
        }
        k.take(bx(3416), bz(2905), bx(3436) + 1, bz(2889) + 1);
        k.dressTown(3398, 2878, 3448, 2924, "desert", 22, 82);
        return k.build();
    }

    // ------------------------------------------------------------------ Sophanem & Necropolis

    /** Sophanem: city walls, gates, avenue of obelisks and houses (pyramid and temple are separate). */
    static StructureDef sophanem() {
        EasternKit k = new EasternKit("sophanem", "Sophanem").replaces("sophanem");
        int g = k.g;
        k.flat(3266, 2744, 3324, 2814, SAND);
        k.wall(3268, 2746, 3322, 2746, 7, SS, CUT);
        k.wall(3268, 2812, 3322, 2812, 7, SS, CUT);
        k.wall(3268, 2746, 3268, 2812, 7, SS, CUT);
        k.wall(3322, 2746, 3322, 2812, 7, SS, CUT);
        k.tfill(3268, 2746, 3322, 2746, 3, 3, CHIS);
        k.tfill(3268, 2812, 3322, 2812, 3, 3, CHIS);
        k.tfill(3268, 2746, 3268, 2812, 3, 3, CHIS);
        k.tfill(3322, 2746, 3322, 2812, 3, 3, CHIS);
        k.gate(3322, 2780, 3322, 2783, 6);
        k.gate(3304, 2812, 3307, 2812, 6);
        for (int[] t : new int[][]{{3268, 2746}, {3322, 2746}, {3268, 2812}, {3322, 2812}, {3322, 2777}, {3322, 2786},
                {3301, 2812}, {3310, 2812}}) {
            k.tower(t[0], t[1], 3, 10, CUT, SMOOTH, null, null);
            k.set(cx(t[0]), g + 11, cz(t[1]), GOLD);
        }
        k.floor(3289, 2780, 3321, 2783, SMOOTH);
        k.floor(3304, 2784, 3307, 2811, SMOOTH);
        k.floor(3289, 2775, 3291, 2779, SMOOTH);
        k.floor(3287, 2784, 3289, 2789, SMOOTH);
        for (int tx = 3293; tx <= 3318; tx += 5) {
            k.pillar(bx(tx), bz(2785), 6, CHIS, GOLD);
            k.pillar(bx(tx), bz(2778), 6, CHIS, GOLD);
        }
        k.desertHouse(3309, 2786, 3315, 2791, 1, "west", ORANGE_WOOL);
        k.desertHouse(3309, 2794, 3315, 2799, 2, "west", null);
        k.desertHouse(3309, 2802, 3316, 2808, 1, "west", WHITE_WOOL);
        k.desertHouse(3302, 2770, 3308, 2776, 1, "north", null);
        k.desertHouse(3311, 2770, 3318, 2776, 2, "north", ORANGE_WOOL);
        k.desertHouse(3302, 2758, 3308, 2764, 1, "north", WHITE_WOOL);
        k.desertHouse(3311, 2758, 3318, 2764, 1, "north", null);
        k.desertHouse(3302, 2749, 3308, 2754, 1, "north", null);
        k.desertHouse(3311, 2749, 3318, 2754, 1, "north", ORANGE_WOOL);
        k.desertHouse(3270, 2760, 3278, 2766, 2, "east", ORANGE_WOOL);
        k.desertHouse(3270, 2770, 3278, 2776, 1, "east", null);
        k.desertHouse(3270, 2780, 3277, 2786, 1, "east", WHITE_WOOL);
        k.desertHouse(3270, 2750, 3278, 2755, 1, "east", null);
        k.palm(3300, 2790, 6);
        k.palm(3300, 2774, 6);
        k.palm(3319, 2780, 6);
        k.palm(3280, 2779, 6);
        // The pyramid and the temple are separate structures inside the walls: keep props off them.
        k.take(bx(3278) - 4, bz(2808) - 4, bx(3298) + 6, bz(2790) + 8);
        k.take(bx(3282) - 2, bz(2774) - 5, bx(3298) + 3, bz(2758) + 3);
        k.dressTown(3268, 2746, 3322, 2812, "desert", 24, 91);
        return k.build();
    }

    /** Jalsavrah, the Sophanem pyramid (Pyramid Plunder). */
    static StructureDef sophanemPyramid() {
        EasternKit k = new EasternKit("sophanem_pyramid", "Jalsavrah").replaces("mg_sophanem_pyramid");
        int g = k.g;
        int[] q = r(3278, 2790, 3298, 2808);
        // Jalsavrah towers over Sophanem: a steep pyramid with banded courses and a gilded capstone.
        int top = k.steepPyramid(q[0], q[1], q[2], q[3], g, SS, SS_STAIRS, CUT);
        int mx = (q[0] + q[2]) / 2;
        k.fill(mx - 1, top, (q[1] + q[3]) / 2 - 1, mx + 1, top, (q[1] + q[3]) / 2 + 1, GOLD);
        k.set(mx, top + 1, (q[1] + q[3]) / 2, GOLD);
        k.clear(mx - 1, g, q[3] - 8, mx + 1, g + 3, q[3]);
        k.clear(mx - 5, g, q[3] - 16, mx + 5, g + 4, q[3] - 8);
        k.b.replace(mx - 6, g, q[3] - 17, mx + 6, g + 5, q[3], SS, CHIS);
        k.set(mx, g, q[3] - 15, "runecraft:pyramid_plunder_entrance");
        k.set(mx - 4, g, q[3] - 15, "runecraft:pp_urn");
        k.set(mx + 4, g, q[3] - 15, "runecraft:pp_urn");
        k.set(mx - 3, g + 3, q[3] - 12, "minecraft:soul_lantern[hanging=true]");
        k.set(mx + 3, g + 3, q[3] - 12, "minecraft:soul_lantern[hanging=true]");
        k.fill(mx - 2, g, q[3] + 1, mx - 2, g + 4, q[3] + 1, CHIS);
        k.fill(mx + 2, g, q[3] + 1, mx + 2, g + 4, q[3] + 1, CHIS);
        k.set(mx + 4, g, q[3] + 3, "runecraft:pyramid_plunder_artefact_trader");
        sphinx(k, mx - 6, q[3] + 3, "south");
        sphinx(k, mx + 6, q[3] + 3, "south");
        return k.build();
    }

    /** A small sphinx statue facing a direction (north/south). */
    private static void sphinx(EasternKit k, int x, int z, String facing) {
        int g = k.g;
        int d = facing.equals("south") ? 1 : -1;
        k.fill(x - 1, g, z - 2 * d, x + 1, g, z + 2 * d, SMOOTH);
        k.fill(x - 1, g + 1, z - 2 * d, x + 1, g + 1, z - d, SS);
        k.set(x, g + 1, z + d, CHIS);
        k.set(x, g + 2, z + d, CHIS);
        k.set(x, g + 3, z + d, "minecraft:smooth_sandstone_slab");
    }

    /** The temple of Sophanem with its columned facade, altar and the bank. */
    static StructureDef sophanemTemple() {
        EasternKit k = new EasternKit("sophanem_temple", "Temple of Sophanem").replaces("sophanem_dungeon");
        int g = k.g;
        int[] t = k.hall(3282, 2758, 3298, 2774, 2, DESERT_RICH, "north");
        k.openUp(t, 2);
        int mx = (t[0] + t[2]) / 2;
        for (int z = t[1] + 4; z < t[3] - 2; z += 5) {
            k.fill(mx - 6, g, z, mx - 6, g + 6, z, CHIS);
            k.fill(mx + 7, g, z, mx + 7, g + 6, z, CHIS);
        }
        k.fill(mx - 2, g, t[3] - 4, mx + 3, g, t[3] - 1, CHIS);
        k.set(mx, g + 1, t[3] - 3, ALTAR);
        k.fill(mx, g + 1, t[3] - 1, mx + 1, g + 4, t[3] - 1, GOLD);
        k.fill(mx, g, t[1] + 1, mx + 1, g, t[3] - 5, "minecraft:red_carpet");
        k.boothsZ(t[0] + 2, t[1] + 4, t[3] - 4, g, CUT);
        for (int x = t[0]; x <= t[2]; x += 4) {
            k.fill(x, g, t[1] - 3, x, g + 7, t[1] - 3, CHIS);
        }
        k.fill(t[0], g + 8, t[1] - 3, t[2], g + 8, t[1] - 1, CUT);
        k.fill(t[0], g - 1, t[1] - 3, t[2], g - 1, t[1] - 1, SMOOTH);
        k.fill(t[0] - 1, g + 9, t[1] - 3, t[2] + 1, g + 9, t[3] + 1, "minecraft:cut_sandstone_slab");
        return k.build();
    }

    /** The Necropolis south of Sophanem: tombs, emissary houses and obelisks around a statue plaza. */
    static StructureDef necropolis() {
        EasternKit k = new EasternKit("necropolis", "Necropolis").replaces("necropolis");
        int g = k.g;
        k.flat(3330, 2710, 3368, 2760, SAND);
        k.floor(3332, 2720, 3344, 2732, CUT);
        k.floor(3334, 2722, 3342, 2730, SMOOTH);
        k.floor(3337, 2733, 3339, 2758, SMOOTH);
        int px = bx(3338);
        int pz = bz(2726);
        k.fill(px - 2, g, pz - 2, px + 2, g, pz + 2, CHIS);
        k.fill(px - 1, g + 1, pz - 1, px + 1, g + 5, pz + 1, SMOOTH);
        k.fill(px - 3, g + 4, pz, px + 3, g + 4, pz, SMOOTH);
        k.fill(px - 1, g + 6, pz - 1, px + 1, g + 7, pz + 1, GOLD);
        for (int[] o : new int[][]{{3332, 2720}, {3344, 2720}, {3332, 2732}, {3344, 2732}}) {
            k.pillar(bx(o[0]), bz(o[1]), 7, CHIS, GOLD);
        }
        int[][] tombs = {{3330, 2740, 3335, 2745}, {3330, 2748, 3335, 2753}, {3348, 2712, 3353, 2717}, {3356, 2712, 3361, 2717},
                {3348, 2722, 3353, 2727}, {3356, 2722, 3361, 2727}};
        for (int[] t : tombs) {
            int[] q = k.hall(t[0], t[1], t[2], t[3], 1, DESERT_RICH, t[0] < 3340 ? "east" : "west");
            int mx = (q[0] + q[2]) / 2;
            int mz = (q[1] + q[3]) / 2;
            k.fill(mx - 1, g, mz, mx + 1, g, mz + 1, "minecraft:polished_blackstone");
            k.set(mx, g + 1, mz, "minecraft:skeleton_skull");
            k.dome(mx, mz, g + 4, 3.5, 2, SMOOTH, SMOOTH);
        }
        k.desertHouse(3330, 2712, 3336, 2717, 1, "north", ORANGE_WOOL);
        k.desertHouse(3341, 2738, 3346, 2744, 1, "west", WHITE_WOOL);
        k.desertHouse(3341, 2748, 3346, 2754, 1, "west", null);
        k.palm(3346, 2733, 6);
        k.palm(3330, 2735, 6);
        // The Tombs of Amascut pyramid is its own structure in the east: keep props off it.
        k.take(bx(3346), bz(2760), bx(3368), bz(2740));
        k.dressTown(3330, 2710, 3368, 2760, "desert", 12, 93);
        return k.build();
    }

    /** The Tombs of Amascut: the golden stepped pyramid in the Necropolis with its entrance and bank. */
    static StructureDef tombsOfAmascut() {
        EasternKit k = new EasternKit("tombs_of_amascut", "Tombs of Amascut").replaces("toa_jaldraocht_entrance");
        int g = k.g;
        int[] q = r(3348, 2742, 3366, 2758);
        int top = k.steppedPyramid(q[0], q[1], q[2], q[3], g, 2, 2, SMOOTH, "minecraft:cut_sandstone");
        int mz = (q[1] + q[3]) / 2;
        int mx = (q[0] + q[2]) / 2;
        k.fill(mx - 1, top, mz - 1, mx + 1, top + 1, mz + 1, GOLD);
        k.clear(q[0], g, mz - 1, q[0] + 9, g + 3, mz + 1);
        k.b.replace(q[0], g, mz - 2, q[0] + 10, g + 4, mz + 2, SMOOTH, CHIS);
        k.set(q[0] + 8, g, mz, "runecraft:tombs_of_amascut_entrance");
        k.set(q[0] + 5, g + 3, mz, "minecraft:soul_lantern[hanging=true]");
        k.fill(q[0] - 1, g, mz - 2, q[0] - 1, g + 5, mz - 2, CHIS);
        k.fill(q[0] - 1, g, mz + 2, q[0] - 1, g + 5, mz + 2, CHIS);
        k.set(q[0] - 3, g, mz - 4, BANK);
        k.set(q[0] - 3, g, mz + 4, "runecraft:toa_invocation_obelisk");
        k.fill(q[0] - 8, g - 1, mz - 1, q[0] - 1, g - 1, mz + 1, CHIS);
        sphinx(k, q[0] - 5, mz - 6, "north");
        sphinx(k, q[0] - 5, mz + 6, "south");
        return k.build();
    }

    // ------------------------------------------------------------------ Menaphos

    /** Menaphos: the great walled city with its ziggurat, Temple of Tumeken, bank and districts. */
    static StructureDef menaphos() {
        EasternKit k = new EasternKit("menaphos", "Menaphos");
        int g = k.g;
        k.flat(3192, 2742, 3260, 2810, SAND);
        k.wall(3194, 2744, 3258, 2744, 9, SMOOTH, CUT);
        k.wall(3194, 2808, 3258, 2808, 9, SMOOTH, CUT);
        k.wall(3194, 2744, 3194, 2808, 9, SMOOTH, CUT);
        k.wall(3258, 2744, 3258, 2808, 9, SMOOTH, CUT);
        for (int[] l : new int[][]{{3194, 2744, 3258, 2744}, {3194, 2808, 3258, 2808}, {3194, 2744, 3194, 2808}, {3258, 2744, 3258, 2808}}) {
            k.tfill(l[0], l[1], l[2], l[3], 4, 4, RED_SS);
        }
        k.gate(3258, 2774, 3258, 2778, 7);
        k.gate(3224, 2808, 3228, 2808, 7);
        k.gate(3224, 2744, 3228, 2744, 7);
        for (int[] t : new int[][]{{3194, 2744}, {3258, 2744}, {3194, 2808}, {3258, 2808}, {3258, 2771}, {3258, 2781},
                {3221, 2808}, {3231, 2808}, {3221, 2744}, {3231, 2744}}) {
            k.tower(t[0], t[1], 4, 13, SMOOTH, CUT, null, null);
            k.dome(cx(t[0]), cz(t[1]), g + 13, 5.0, 4, SMOOTH, ORANGE_TERRA);
            k.set(cx(t[0]), g + 17, cz(t[1]), GOLD);
        }
        k.floor(3195, 2774, 3257, 2778, SMOOTH);
        k.floor(3224, 2745, 3228, 2807, SMOOTH);
        // Imperial ziggurat at the heart of the city.
        int[] z = r(3212, 2784, 3236, 2802);
        int top = k.steppedPyramid(z[0], z[1], z[2], z[3], g, 3, 3, SMOOTH, RED_SS);
        int zx = (z[0] + z[2]) / 2;
        int zz = (z[1] + z[3]) / 2;
        k.fill(zx - 2, top, zz - 2, zx + 2, top + 3, zz + 2, CHIS);
        k.clear(zx - 1, top, zz - 1, zx + 1, top + 2, zz + 1);
        k.set(zx, top, zz, ALTAR);
        k.fill(zx - 2, top + 4, zz - 2, zx + 2, top + 4, zz + 2, GOLD);
        for (int i = 0; i < (top - g); i++) {
            k.fill(zx - 1, g + i, z[3] - i, zx + 1, g + i, z[3] - i, SMOOTH_STAIRS + "[facing=north]");
            k.clear(zx - 1, g + i + 1, z[3] - i, zx + 1, top, z[3] - i);
        }
        // Temple of Tumeken.
        int[] t = k.hall(3238, 2790, 3252, 2802, 2, DESERT_RICH, "south");
        k.openUp(t, 2);
        int tmx = (t[0] + t[2]) / 2;
        k.fill(tmx - 2, g, t[1] + 1, tmx + 2, g, t[1] + 3, CHIS);
        k.set(tmx, g + 1, t[1] + 2, ALTAR);
        k.disc(tmx, g + 5, t[1] + 1, 1.5, GOLD);
        k.fill(tmx, g, t[1] + 4, tmx + 1, g, t[3] - 1, "minecraft:yellow_carpet");
        k.dome(tmx, (t[1] + t[3]) / 2, g + 8, 6.0, 4, SMOOTH, GOLD);
        // Bank and districts.
        int[] bank = k.hall(3200, 2788, 3210, 2795, 1, DESERT_RICH, "south");
        k.boothsX(bank[0] + 3, bank[2] - 3, bank[1] + 3, g, CUT);
        k.desertShop(3200, 2798, 3207, 2804, "south", GENERAL, ORANGE_WOOL);
        k.desertShop(3240, 2780, 3247, 2786, "south", WEAPONS, RED_WOOL);
        k.desertShop(3249, 2780, 3255, 2786, "south", RUNES, "minecraft:blue_wool");
        int[][] houses = {{3198, 2748, 3205, 2754}, {3208, 2748, 3215, 2754}, {3198, 2758, 3205, 2764}, {3208, 2758, 3215, 2764},
                {3198, 2766, 3205, 2771}, {3208, 2766, 3215, 2771}, {3232, 2748, 3239, 2754}, {3242, 2748, 3249, 2754},
                {3232, 2758, 3239, 2764}, {3242, 2758, 3249, 2764}, {3232, 2766, 3239, 2771}, {3242, 2766, 3249, 2771},
                {3212, 2748, 3220, 2752}, {3238, 2804, 3244, 2806}};
        int i = 0;
        for (int[] h : houses) {
            k.desertHouse(h[0], h[1], h[2], h[3], i % 3 == 0 ? 2 : 1, "north", i % 2 == 0 ? ORANGE_WOOL : WHITE_WOOL);
            i++;
        }
        for (int tx = 3232; tx <= 3254; tx += 6) {
            k.stall(tx, 2773, "runecraft:spice_stall", i++ % 2 == 0 ? RED_WOOL : ORANGE_WOOL);
        }
        for (int[] p : new int[][]{{3218, 2780}, {3232, 2780}, {3210, 2776}, {3240, 2776}, {3222, 2760}, {3230, 2760}}) {
            k.palm(p[0], p[1], 7);
        }
        // Gilded obelisks flank the avenue to the ziggurat; braziers on its terraces; props in the districts.
        for (int ty = 2758; ty <= 2770; ty += 6) {
            k.pillar(bx(3222), bz(ty), 7, CHIS, GOLD);
            k.pillar(bx(3230) + 1, bz(ty), 7, CHIS, GOLD);
        }
        for (int tier = 1; tier <= 3; tier++) {
            int ins = 3 * tier;
            k.set(z[0] + ins, g + 3 * tier, z[1] + ins, "minecraft:campfire");
            k.set(z[2] - ins, g + 3 * tier, z[1] + ins, "minecraft:campfire");
            k.set(z[0] + ins, g + 3 * tier, z[3] - ins, "minecraft:campfire");
            k.set(z[2] - ins, g + 3 * tier, z[3] - ins, "minecraft:campfire");
        }
        k.take(z[0] - 2, z[1] - 2, z[2] + 2, z[3] + 2);
        k.dressTown(3194, 2744, 3258, 2808, "desert", 34, 92);
        return k.build();
    }

    // ------------------------------------------------------------------ Pyramids

    /** The Agility Pyramid: five stepped tiers with obstacles on every ledge and a golden capstone. */
    static StructureDef agilityPyramid() {
        EasternKit k = new EasternKit("agility_pyramid", "Agility Pyramid");
        int g = k.g;
        k.flat(3330, 2810, 3380, 2870, SAND);
        int[] q = r(3336, 2828, 3374, 2864);
        int top = k.steppedPyramid(q[0], q[1], q[2], q[3], g, 8, 6, SS, CUT);
        int cxp = (q[0] + q[2]) / 2;
        int czp = (q[1] + q[3]) / 2;
        k.fill(cxp - 1, top, czp - 1, cxp + 2, top + 1, czp + 2, GOLD);
        k.set(cxp, top + 2, czp, GOLD);
        // Entrance stairs from the ground onto the first tier (north face).
        for (int j = 0; j < 6; j++) {
            k.fill(cxp - 1, g + j, q[1] - 6 + j, cxp + 1, g + j, q[1] - 6 + j, SS_STAIRS + "[facing=south]");
            if (j > 0) {
                k.fill(cxp - 1, g, q[1] - 6 + j, cxp + 1, g + j - 1, q[1] - 6 + j, SS);
            }
        }
        for (int i = 0; i < 4; i++) {
            int ox1 = q[0] + 8 * i;
            int oz1 = q[1] + 8 * i;
            int ox2 = q[2] - 8 * i;
            int oz2 = q[3] - 8 * i;
            int ys = g + 6 * (i + 1);
            // Low wall across the north ledge.
            k.fill(ox1 + 14, ys, oz1, ox1 + 14, ys, oz1 + 7, SS_WALL);
            // Gap in the east ledge.
            k.clear(ox2 - 7, ys - 2, oz1 + 16, ox2, ys - 1, oz1 + 17);
            // Plank over a gap on the south ledge.
            k.clear(ox1 + 14, ys - 2, oz2 - 7, ox1 + 17, ys - 1, oz2);
            k.fill(ox1 + 14, ys - 1, oz2 - 4, ox1 + 17, ys - 1, oz2 - 4, "minecraft:oak_slab[type=top]");
            // Narrow ledge on the west side.
            k.clear(ox1 + 1, ys - 1, oz1 + 14, ox1 + 7, ys - 1, oz1 + 22);
            // Stairs up to the next tier (alternating sides of the north face).
            int sx = i % 2 == 0 ? cxp + 6 : cxp - 8;
            for (int j = 0; j < 6; j++) {
                k.fill(sx, ys + j, oz1 + 2 + j, sx + 1, ys + j, oz1 + 2 + j, SS_STAIRS + "[facing=south]");
                if (j > 0) {
                    k.fill(sx, ys, oz1 + 2 + j, sx + 1, ys + j - 1, oz1 + 2 + j, SS);
                }
            }
        }
        int[] hut = k.house(3343, 2813, 3349, 2819, 1, DESERT, "north");
        k.awning(hut, "north", ORANGE_WOOL);
        k.palm(3338, 2818, 6);
        k.palm(3368, 2816, 6);
        return k.build();
    }

    /** The Jaldraocht pyramid with its passage to the ancient altar chamber. */
    static StructureDef jaldraocht() {
        EasternKit k = new EasternKit("jaldraocht_pyramid", "Jaldraocht Pyramid").replaces("jaldraocht_pyramid");
        int g = k.g;
        k.flat(3218, 2883, 3248, 2913, SAND);
        int[] q = r(3223, 2888, 3243, 2908);
        int top = k.steepPyramid(q[0], q[1], q[2], q[3], g, SMOOTH, SMOOTH_STAIRS, CUT);
        int mx = (q[0] + q[2]) / 2;
        int mz = (q[1] + q[3]) / 2;
        k.fill(mx - 1, top, mz - 1, mx + 1, top, mz + 1, GOLD);
        k.fill(mx, top + 1, mz, mx, top + 2, mz, GOLD);
        k.clear(mx - 1, g, q[1], mx + 1, g + 2, q[1] + 12);
        k.clear(mx - 5, g, q[1] + 12, mx + 5, g + 5, q[1] + 24);
        k.b.replace(mx - 6, g - 1, q[1], mx + 6, g + 6, q[1] + 25, SMOOTH, CHIS);
        k.fill(mx - 5, g - 1, q[1] + 12, mx + 5, g - 1, q[1] + 24, "minecraft:polished_blackstone");
        k.fill(mx - 1, g, q[1] + 20, mx + 1, g, q[1] + 23, "minecraft:polished_blackstone_bricks");
        k.set(mx, g + 1, q[1] + 22, "runecraft:ancient_altar");
        k.set(mx - 4, g, q[1] + 23, "minecraft:skeleton_skull");
        k.set(mx + 4, g, q[1] + 23, "minecraft:skeleton_skull");
        for (int z = q[1] + 14; z <= q[1] + 22; z += 4) {
            k.set(mx - 4, g + 4, z, "minecraft:soul_lantern[hanging=true]");
            k.set(mx + 4, g + 4, z, "minecraft:soul_lantern[hanging=true]");
        }
        k.fill(mx - 2, g, q[1] - 1, mx - 2, g + 3, q[1] - 1, CHIS);
        k.fill(mx + 2, g, q[1] - 1, mx + 2, g + 3, q[1] - 1, CHIS);
        return k.build();
    }

    /** Paved precinct, obelisks and guardian statues around the Jaldraocht pyramid. */
    static StructureDef jaldraochtObelisks() {
        EasternKit k = new EasternKit("jaldraocht_precinct", "Jaldraocht precinct").replaces("quest_jaldraocht_pyramid");
        int g = k.g;
        int[] p = r(3220, 2885, 3246, 2911);
        k.fill(p[0], g - 1, p[1], p[2], g - 1, p[3], CUT);
        k.b.walls(p[0], g - 1, p[1], p[2], g - 1, p[3], CHIS);
        for (int[] o : new int[][]{{3220, 2885}, {3246, 2885}, {3220, 2911}, {3246, 2911}}) {
            int x = cx(o[0]);
            int z = cz(o[1]);
            k.fill(x - 1, g, z - 1, x + 1, g, z + 1, CHIS);
            k.pillar(x, z, 9, SMOOTH, GOLD);
        }
        int mx = (p[0] + p[2]) / 2;
        sphinx(k, mx - 5, p[1] - 2, "north");
        sphinx(k, mx + 5, p[1] - 2, "north");
        k.fill(mx - 1, g - 1, p[1] - 8, mx + 1, g - 1, p[1] - 1, CUT);
        return k.build();
    }

    // ------------------------------------------------------------------ Bandit Camp & Uzer

    /** The desert Bandit Camp: bar, Bandit Duty Free, mud huts and tents around a campfire. */
    static StructureDef banditCamp() {
        EasternKit k = new EasternKit("bandit_camp", "Bandit Camp").replaces("bandit_camp");
        int g = k.g;
        k.flat(3152, 2958, 3192, 2998, SAND);
        k.patches(3154, 2960, 3190, 2996, "minecraft:coarse_dirt", 20, 61);
        int[] bar = k.hall(3172, 2982, 3181, 2989, 1, DESERT_MUD, "south");
        k.counterX(bar[0] + 2, bar[2] - 2, bar[1] + 3, g, "minecraft:spruce_slab[type=top]", null);
        k.fill(bar[0] + 1, g, bar[1] + 1, bar[2] - 1, g, bar[1] + 1, "minecraft:barrel[facing=up]");
        k.set(bar[0] + 3, g, bar[3] - 3, "minecraft:spruce_slab[type=top]");
        k.set(bar[2] - 3, g, bar[3] - 3, "minecraft:spruce_slab[type=top]");
        k.desertShop(3159, 2982, 3166, 2988, "south", GENERAL, "minecraft:brown_wool");
        k.house(3158, 2962, 3164, 2968, 1, DESERT_MUD, "north");
        k.house(3177, 2962, 3183, 2968, 1, DESERT_MUD, "north");
        k.house(3184, 2982, 3189, 2988, 1, DESERT_MUD, "west");
        k.tentT(3155, 2972, 3158, 2978, "minecraft:brown_wool");
        k.tentT(3184, 2970, 3187, 2976, WHITE_WOOL);
        k.tentT(3166, 2962, 3169, 2967, "minecraft:brown_wool");
        k.tentT(3152 + 16, 2991, 3171, 2996, WHITE_WOOL);
        k.campfire(bx(3170), bz(2975));
        k.floor(3167, 2972, 3173, 2978, "minecraft:coarse_dirt");
        k.palm(3162, 2994, 6);
        k.palm(3188, 2994, 6);
        k.scatter(3152, 2958, 3192, 2998, 0, "minecraft:cobblestone", 16, 62);
        k.scatter(3152, 2958, 3192, 2998, 0, "minecraft:dead_bush", 16, 63);
        k.dressTown(3152, 2958, 3192, 2998, "desert", 14, 64);
        return k.build();
    }

    /** The ruined temple annex of Uzer where the storm demon was bound: a sunken court with black pillars. */
    static StructureDef uzerTemple() {
        EasternKit k = new EasternKit("uzer_temple", "Ruins of Uzer temple").replaces("demon_temple_storm");
        int g = k.g;
        int x = bx(3482);
        int z = bz(3086);
        k.clear(x - 4, g - 2, z - 4, x + 4, g - 1, z + 4);
        k.fill(x - 4, g - 3, z - 4, x + 4, g - 3, z + 4, "minecraft:polished_blackstone");
        for (int i = 0; i < 2; i++) {
            k.fill(x - 4 + i, g - 2 + i, z + 5 - i, x + 4 - i, g - 2 + i, z + 5 - i, "minecraft:sandstone_stairs[facing=south]");
        }
        for (int[] d : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
            k.fill(x + d[0], g - 2, z + d[1], x + d[0], g + 2 + Math.abs(d[0] + d[1]) / 4, z + d[1], "minecraft:blackstone");
        }
        k.fill(x - 1, g - 2, z - 1, x + 1, g - 2, z + 1, "minecraft:crying_obsidian");
        k.set(x, g - 1, z, "minecraft:soul_campfire");
        return k.build();
    }

    /** The ruins of Uzer: a broken palace with columns, scattered walls, obelisk stumps and a clay golem. */
    static StructureDef uzer() {
        EasternKit k = new EasternKit("uzer_ruins", "Ruins of Uzer").replaces("uzer");
        int g = k.g;
        k.flat(3466, 3076, 3504, 3108, SAND);
        String[] mats = {SS, CUT, "minecraft:smooth_sandstone", "minecraft:sandstone_slab", CHIS};
        k.ruin(3478, 3086, 3494, 3100, 7, mats, SMOOTH, 71);
        int[] p = r(3478, 3086, 3494, 3100);
        for (int x = p[0] + 4; x < p[2] - 2; x += 5) {
            k.pillar(x, p[1] + 5, 3 + (x % 4), CHIS, null);
            k.pillar(x, p[3] - 5, 2 + (x % 3), CHIS, null);
        }
        k.ruin(3468, 3080, 3474, 3086, 4, mats, null, 72);
        k.ruin(3497, 3078, 3502, 3084, 3, mats, null, 73);
        k.ruin(3468, 3096, 3474, 3104, 5, mats, CUT, 74);
        k.ruin(3497, 3094, 3502, 3104, 4, mats, null, 75);
        for (int[] o : new int[][]{{3476, 3092}, {3496, 3092}, {3486, 3104}, {3486, 3080}}) {
            k.pillar(cx(o[0]), cz(o[1]), 3, CHIS, "minecraft:sandstone_slab");
        }
        int gx = cx(3486);
        int gz = cz(3093);
        k.fill(gx - 1, g, gz, gx - 1, g + 1, gz, TERRA);
        k.fill(gx + 1, g, gz, gx + 1, g + 1, gz, TERRA);
        k.fill(gx - 1, g + 2, gz, gx + 1, g + 3, gz, TERRA);
        k.set(gx - 2, g + 3, gz, TERRA);
        k.set(gx + 2, g + 3, gz, TERRA);
        k.set(gx, g + 4, gz, "minecraft:carved_pumpkin[facing=south]");
        k.scatter(3466, 3076, 3504, 3108, 0, "minecraft:dead_bush", 14, 76);
        return k.build();
    }
}
