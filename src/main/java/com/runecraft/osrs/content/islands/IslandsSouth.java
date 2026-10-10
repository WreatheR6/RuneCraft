package com.runecraft.osrs.content.islands;

import com.runecraft.osrs.world.StructureDef;
import com.runecraft.osrs.world.StructureDef.Builder;

import java.util.List;

import static com.runecraft.osrs.content.islands.IslandsKit.*;

/**
 * Crandor (the ruined city and its volcano; Elvarg's arena belongs to the quests module), Kharazi
 * Jungle (the Legends' Quest clearing and totem; Nezikchened's cave and arena and Shilo Village are
 * left alone), Cairn Isle, and the Void Knights' Outpost with the Pest Control landers.
 */
final class IslandsSouth {
    private IslandsSouth() {
    }

    static List<StructureDef> all() {
        return List.of(crandor(), kharaziJungle(), cairnIsle(), voidKnightsOutpost());
    }

    /** One layer of a round mound: a disc of {@code block} of radius r (blocks) centred on a tile. */
    static void disc(Builder b, int tx, int ty, int r, int y1, int y2, String block) {
        int cx = StructureDef.bx(tx) + 1;
        int cz = StructureDef.bz(ty) + 1;
        int g = b.ground();
        for (int dx = -r; dx <= r; dx++) {
            int half = (int) Math.floor(Math.sqrt(Math.max(0, r * r - dx * dx)));
            b.fill(cx + dx, g + y1, cz - half, cx + dx, g + y2, cz + half, block);
        }
    }

    /** Crandor: the ruins of the city in the south and the volcano above Elvarg's lair. */
    static StructureDef crandor() {
        Builder b = StructureDef.builder("isl_crandor", "Crandor").replaces(IslandsPlaces.CRANDOR);
        b.tFlatten(2830, 3226, 2851, 3249, "minecraft:coarse_dirt");
        b.tFlatten(2822, 3250, 2850, 3297, "minecraft:coarse_dirt");
        // The ruined city.
        ruins(b, 2832, 3230, 2840, 3238, 4, CRACKED, "minecraft:gravel");
        ruins(b, 2843, 3240, 2849, 3247, 3, MOSSY_STONE, "minecraft:gravel");
        ruins(b, 2826, 3282, 2834, 3292, 5, CRACKED, "minecraft:gravel");
        ruins(b, 2840, 3286, 2848, 3294, 4, MOSSY_STONE, "minecraft:gravel");
        b.tFloor(2836, 3240, 2838, 3262, "minecraft:gravel");
        // The volcano: stepped rings of stone around a lava crater.
        disc(b, 2836, 3270, 22, 0, 2, "minecraft:stone");
        disc(b, 2836, 3270, 17, 3, 5, "minecraft:stone");
        disc(b, 2836, 3270, 12, 6, 8, BASALT);
        disc(b, 2836, 3270, 8, 9, 10, "minecraft:blackstone");
        disc(b, 2836, 3270, 5, 9, 10, "minecraft:magma_block");
        disc(b, 2836, 3270, 3, 10, 10, "minecraft:lava");
        // The way into Elvarg's lair on the south slope.
        b.tClear(2836, 3260, 2837, 3262, 0, 3);
        b.tFill(2835, 3259, 2838, 3259, 4, 4, BASALT);
        // The wreck of the Lady Lumbridge on the west shore.
        b.tFill(2823, 3256, 2829, 3258, -1, 0, "minecraft:dark_oak_planks");
        b.tClear(2824, 3257, 2828, 3257, 0, 0);
        b.tFill(2826, 3257, 2826, 3257, 1, 5, "minecraft:dark_oak_fence");
        b.tSet(2845, 3233, 0, "minecraft:skeleton_skull");
        b.tSet(2828, 3252, 0, "minecraft:bone_block");
        return b.build();
    }

    /** Kharazi Jungle: Gujuo's clearing with the Legends' totem pole, jungle and ruined shrines. */
    static StructureDef kharaziJungle() {
        Builder b = StructureDef.builder("isl_kharazi_jungle", "Kharazi Jungle").replaces(IslandsPlaces.KHARAZI_JUNGLE);
        b.tFlatten(2826, 2904, 2850, 2931, "minecraft:podzol");
        b.tFloor(2830, 2914, 2840, 2922, "minecraft:coarse_dirt");
        // The totem pole of the Kharazi tribe.
        b.tFill(2835, 2918, 2835, 2918, 0, 6, "minecraft:stripped_jungle_log");
        b.tSet(2835, 2918, 7, "minecraft:carved_pumpkin[facing=south]");
        b.tSet(2834, 2918, 4, "minecraft:jungle_trapdoor[facing=west,half=top]");
        b.tSet(2836, 2918, 4, "minecraft:jungle_trapdoor[facing=east,half=top]");
        b.tSet(2832, 2916, 0, "minecraft:campfire");
        hut(b, 2844, 2926, 4, BAMBOO, "minecraft:hay_block", "minecraft:bamboo_planks", "south");
        ruins(b, 2828, 2924, 2833, 2929, 4, MOSSY, "minecraft:moss_block");
        ruins(b, 2842, 2906, 2848, 2911, 3, MOSSY, "minecraft:moss_block");
        String leaves = "minecraft:jungle_leaves[persistent=true]";
        for (int[] t : new int[][]{{2827, 2906}, {2838, 2906}, {2849, 2915}, {2827, 2915}, {2838, 2929}, {2849, 2930},
                {2845, 2919}, {2830, 2910}}) {
            tree(b, t[0], t[1], JUNGLE_LOG, leaves, 8);
        }
        for (int[] p : new int[][]{{2841, 2913}, {2829, 2920}}) {
            palm(b, p[0], p[1], 6);
        }
        return b.build();
    }

    /** Cairn Isle: rocky islet, the cairns and the entrance to Bervirius' tomb. */
    static StructureDef cairnIsle() {
        Builder b = StructureDef.builder("isl_cairn_isle", "Cairn Isle").replaces(IslandsPlaces.CAIRN_ISLE);
        b.tFlatten(2754, 2966, 2772, 2986, "minecraft:stone");
        b.tFloor(2756, 2968, 2770, 2984, "minecraft:gravel");
        disc(b, 2763, 2980, 6, 0, 3, "minecraft:stone");
        disc(b, 2763, 2980, 3, 4, 5, "minecraft:stone");
        // Bervirius' tomb entrance in the side of the rock.
        b.tFill(2761, 2975, 2765, 2977, 0, 3, "minecraft:chiseled_stone_bricks");
        b.tClear(2762, 2975, 2764, 2977, 0, 2);
        b.tFill(2762, 2977, 2764, 2977, -3, -1, "minecraft:stone_brick_stairs[facing=south]");
        // Cairns of piled rocks.
        for (int[] c : new int[][]{{2757, 2970}, {2769, 2969}, {2757, 2983}, {2770, 2984}}) {
            b.tSet(c[0], c[1], 0, COBBLE);
            b.tSet(c[0], c[1], 1, MOSSY);
            b.tSet(c[0], c[1], 2, "minecraft:cobblestone_wall");
        }
        return b.build();
    }

    /**
     * The Void Knights' Outpost: walls and towers, the bank, the Void Knights' reward hall, the north
     * dock (boats from Port Sarim) and the three Pest Control landers at the south pier.
     */
    static StructureDef voidKnightsOutpost() {
        Builder b = StructureDef.builder("isl_void_knights_outpost", "Void Knights' Outpost").replaces(IslandsPlaces.VOID_KNIGHTS_OUTPOST);
        b.tFlatten(2630, 2642, 2680, 2682, GRASS);
        b.tFloor(2632, 2642, 2678, 2648, "minecraft:polished_andesite");
        b.tFloor(2654, 2649, 2656, 2682, "minecraft:polished_andesite");
        // The south pier and the landers (novice, intermediate, veteran).
        b.tFill(2630, 2634, 2680, 2641, -1, -1, "minecraft:spruce_planks");
        b.tFill(2630, 2634, 2680, 2634, 0, 0, "minecraft:spruce_fence");
        ship(b, 2652, 2629, 8, "minecraft:spruce_planks", "minecraft:white_wool");
        ship(b, 2636, 2629, 7, "minecraft:spruce_planks", "minecraft:light_gray_wool");
        ship(b, 2666, 2629, 7, "minecraft:spruce_planks", "minecraft:gray_wool");
        b.tClear(2658, 2634, 2659, 2634, 0, 0);
        b.tClear(2642, 2634, 2643, 2634, 0, 0);
        b.tClear(2672, 2634, 2673, 2634, 0, 0);
        b.tSet(2658, 2636, 0, rc("pest_control_entrance"));
        b.tSet(2642, 2636, 0, rc("pest_control_intermediate_entrance"));
        b.tSet(2672, 2636, 0, rc("pest_control_veteran_entrance"));
        // Walls and towers.
        String w = STONE;
        wall(b, 2632, 2650, 2632, 2680, 5, w);
        wall(b, 2678, 2650, 2678, 2680, 5, w);
        wall(b, 2632, 2680, 2652, 2680, 5, w);
        wall(b, 2658, 2680, 2678, 2680, 5, w);
        b.tTower(2632, 2650, 3, 9, w, ANDESITE);
        b.tTower(2678, 2650, 3, 9, w, ANDESITE);
        b.tTower(2632, 2680, 3, 9, w, ANDESITE);
        b.tTower(2678, 2680, 3, 9, w, ANDESITE);
        // The bank and the Void Knights' hall with the rewards.
        b.tBuilding(2664, 2652, 2674, 2659, 1, w, "minecraft:polished_andesite", "minecraft:deepslate_tile", "west", true);
        b.tSet(2668, 2657, 0, rc("bank_booth"));
        b.tSet(2670, 2657, 0, rc("bank_booth"));
        b.tBuilding(2636, 2652, 2648, 2662, 2, w, "minecraft:polished_andesite", "minecraft:deepslate_tile", "east", true);
        b.tSet(2640, 2658, 0, rc("pest_control_rewards"));
        b.tFill(2641, 2654, 2643, 2660, -1, -1, "minecraft:purple_carpet");
        b.tBuilding(2660, 2666, 2672, 2676, 1, w, "minecraft:spruce_planks", "minecraft:deepslate_tile", "west", true);
        // The north dock for the Port Sarim boats.
        pier(b, 2654, 2683, 2657, 2690, SPRUCE);
        for (int[] l : new int[][]{{2653, 2650}, {2657, 2650}, {2653, 2670}, {2657, 2670}}) {
            lamp(b, l[0], l[1], "minecraft:stone_brick_wall");
        }
        return b.build();
    }
}
