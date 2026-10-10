package com.runecraft.osrs.content.islands;

import com.runecraft.osrs.world.StructureDef;
import com.runecraft.osrs.world.StructureDef.Builder;

import java.util.List;

import static com.runecraft.osrs.content.islands.IslandsKit.*;

/**
 * Fossil Island: the Museum Camp where the barge from the Digsite lands, the House on the Hill and
 * its fossil displays, the Volcanic Mine lobby on the volcano, the Mushroom forest with the sticky
 * and tar swamps, Verdant Valley's stream, and the Wyvern Cave carved under its entrance. The four
 * Magic Mushtrees of the Mycelium transportation system stand at the House on the Hill, Verdant
 * Valley, the Sticky swamp and Mushroom Meadow. The skilling module's hardwood patches, sulliuscep
 * grove, ash piles, meadow and seaweed patches keep their own plazas.
 */
final class IslandsFossil {
    private IslandsFossil() {
    }

    static List<StructureDef> all() {
        return List.of(houseOnTheHill(), volcanicMine(), verdantValley(), mushroomForest(), wyvernCave(), museumCamp());
    }

    /** The Museum Camp: tents, the storage hut with its bank chest, the cleaning bench and the barge pier. */
    static StructureDef museumCamp() {
        Builder b = StructureDef.builder("isl_museum_camp", "Museum Camp").replaces(IslandsPlaces.FOSSIL_ISLAND);
        b.tFlatten(3712, 3796, 3750, 3822, GRASS);
        b.tFloor(3712, 3806, 3750, 3808, PATH);
        b.tFloor(3730, 3796, 3732, 3822, PATH);
        // The barge pier on the west shore.
        pier(b, 3700, 3806, 3711, 3808, SPRUCE);
        b.tFill(3702, 3809, 3708, 3811, -1, -1, "minecraft:dark_oak_planks");
        b.tFill(3702, 3809, 3708, 3811, 0, 0, "minecraft:dark_oak_fence");
        b.tClear(3703, 3810, 3707, 3810, 0, 0);
        // Tents.
        tent(b, 3716, 3812, 3722, 3816, "minecraft:white_wool");
        tent(b, 3736, 3812, 3742, 3816, "minecraft:white_wool");
        tent(b, 3716, 3798, 3722, 3802, "minecraft:light_gray_wool");
        // Storage hut with the bank chest and the camp kitchen.
        b.tBuilding(3736, 3797, 3748, 3804, 1, SPRUCE_LOG, SPRUCE, "minecraft:spruce", "north", true);
        b.tSet(3740, 3800, 0, rc("bank_booth"));
        b.tSet(3745, 3800, 0, rc("cooking_range"));
        b.tSet(3738, 3799, 0, "minecraft:barrel");
        b.tSet(3746, 3798, 0, "minecraft:barrel");
        // Fossil cleaning bench and the campfire.
        b.tSet(3726, 3811, 0, "minecraft:crafting_table");
        b.tSet(3727, 3811, 0, "minecraft:stonecutter");
        b.tSet(3734, 3811, 0, "minecraft:campfire");
        for (int t = 3714; t <= 3748; t += 4) {
            b.tSet(t, 3821, 0, "minecraft:spruce_fence");
            b.tSet(t, 3797, 0, "minecraft:spruce_fence");
        }
        lamp(b, 3729, 3805, "minecraft:spruce_fence");
        lamp(b, 3749, 3809, "minecraft:spruce_fence");
        tree(b, 3748, 3818, "minecraft:spruce_log", "minecraft:spruce_leaves[persistent=true]", 6);
        tree(b, 3713, 3819, "minecraft:spruce_log", "minecraft:spruce_leaves[persistent=true]", 6);
        return b.build();
    }

    /** The House on the Hill, its fossil displays and the Magic Mushtree outside. */
    static StructureDef houseOnTheHill() {
        Builder b = StructureDef.builder("isl_house_on_the_hill", "House on the Hill").replaces(IslandsPlaces.HOUSE_ON_THE_HILL)
                .ground(68);
        b.tFlatten(3750, 3856, 3778, 3882, GRASS);
        b.tFloor(3762, 3856, 3764, 3865, PATH);
        b.tBuilding(3757, 3866, 3771, 3878, 2, MOSSY_STONE, "minecraft:polished_andesite", "minecraft:deepslate_tile", "south", true);
        // Fossil displays: skeletons of bone blocks on andesite plinths.
        for (int t = 3759; t <= 3769; t += 5) {
            b.tSet(t, 3876, 0, ANDESITE);
            b.tSet(t, 3876, 1, "minecraft:bone_block");
            b.tSet(t, 3876, 2, "minecraft:skeleton_skull");
        }
        b.tSet(3760, 3869, 0, "minecraft:bone_block[axis=x]");
        b.tSet(3761, 3869, 0, "minecraft:bone_block[axis=x]");
        b.tSet(3768, 3869, 0, "minecraft:bookshelf");
        b.tSet(3769, 3869, 0, "minecraft:bookshelf");
        b.tSet(3764, 3874, 4, "minecraft:lectern");
        // The Magic Mushtree.
        b.tSet(IslandsStructures.HOUSE_TREE[0], IslandsStructures.HOUSE_TREE[1], 0, rc(IslandsBlocks.MUSHTREE));
        mushroom(b, IslandsStructures.HOUSE_TREE[0] - 3, IslandsStructures.HOUSE_TREE[1] - 2, 6, 3, "minecraft:red_mushroom_block");
        mushroom(b, 3775, 3860, 5, 2, "minecraft:brown_mushroom_block");
        lamp(b, 3761, 3864, "minecraft:oak_fence");
        lamp(b, 3766, 3864, "minecraft:oak_fence");
        return b.build();
    }

    /** The Volcanic Mine lobby, dug into the volcano's basalt slope among lava vents. */
    static StructureDef volcanicMine() {
        Builder b = StructureDef.builder("isl_volcanic_mine", "Volcanic Mine").replaces(IslandsPlaces.VOLCANIC_MINE).ground(70);
        b.tFlatten(3804, 3798, 3828, 3818, "minecraft:basalt");
        b.tFloor(3804, 3798, 3828, 3818, BASALT);
        b.tBuilding(3809, 3805, 3821, 3813, 1, BLACKSTONE, "minecraft:polished_blackstone", "minecraft:polished_blackstone_brick_slab",
                "south", false);
        b.tSet(3815, 3811, 0, "minecraft:polished_blackstone_bricks");
        b.tSet(3815, 3811, 1, "minecraft:soul_lantern");
        b.tFill(3812, 3812, 3818, 3812, -1, -1, "minecraft:magma_block");
        // Lava vents around the lobby.
        for (int[] v : new int[][]{{3806, 3815}, {3826, 3815}, {3806, 3800}, {3826, 3801}, {3824, 3816}}) {
            b.tSetTile(v[0], v[1], -1, "minecraft:magma_block");
            b.tSet(v[0], v[1], -1, "minecraft:lava");
            b.tSet(v[0], v[1], 0, "minecraft:campfire[lit=true]");
        }
        rocks(b, 3805, 3808, BASALT);
        rocks(b, 3827, 3808, "minecraft:blackstone");
        torchPost(b, 3814, 3803, "minecraft:polished_blackstone_wall");
        torchPost(b, 3817, 3803, "minecraft:polished_blackstone_wall");
        return b.build();
    }

    /** Verdant Valley: a stream with a bridge, woodland, bird house spots and the Magic Mushtree. */
    static StructureDef verdantValley() {
        Builder b = StructureDef.builder("isl_verdant_valley", "Verdant Valley").replaces(IslandsPlaces.VERDANT_VALLEY);
        b.tFlatten(3748, 3746, 3782, 3768, GRASS);
        b.tFill(3770, 3746, 3771, 3768, -2, -1, "minecraft:water");
        b.tFill(3769, 3756, 3772, 3758, -1, -1, OAK);
        b.tFill(3769, 3756, 3769, 3758, 0, 0, "minecraft:oak_fence");
        b.tFill(3772, 3756, 3772, 3758, 0, 0, "minecraft:oak_fence");
        b.tClear(3769, 3757, 3772, 3757, 0, 0);
        b.tFloor(3750, 3757, 3768, 3757, PATH);
        b.tSet(3758, 3752, 0, rc("birdhouse_space"));
        b.tSet(3766, 3764, 0, rc("birdhouse_space"));
        b.tSet(IslandsStructures.VALLEY_TREE[0], IslandsStructures.VALLEY_TREE[1], 0, rc(IslandsBlocks.MUSHTREE));
        mushroom(b, IslandsStructures.VALLEY_TREE[0] - 3, IslandsStructures.VALLEY_TREE[1] + 2, 6, 3, "minecraft:red_mushroom_block");
        String leaves = "minecraft:oak_leaves[persistent=true]";
        for (int[] t : new int[][]{{3751, 3749}, {3753, 3766}, {3762, 3748}, {3776, 3750}, {3779, 3762}, {3775, 3766}, {3760, 3767}}) {
            tree(b, t[0], t[1], "minecraft:oak_log", leaves, 6);
        }
        b.tFill(3752, 3760, 3754, 3761, 0, 0, "minecraft:poppy");
        b.tFill(3774, 3756, 3776, 3757, 0, 0, "minecraft:dandelion");
        return b.build();
    }

    /** The Mushroom forest: giant mushrooms, the sticky swamp, the tar swamp and a Magic Mushtree. */
    static StructureDef mushroomForest() {
        Builder b = StructureDef.builder("isl_mushroom_forest", "Mushroom forest").replaces(IslandsPlaces.MUSHROOM_FOREST);
        b.tFlatten(3680, 3772, 3705, 3800, "minecraft:mycelium");
        // Sticky swamp (mud and shallow water) and the tar swamp.
        b.tFloor(3684, 3776, 3692, 3782, MUD);
        b.tFill(3686, 3778, 3689, 3780, -1, -1, "minecraft:water");
        b.tFloor(3696, 3774, 3702, 3779, "minecraft:black_concrete_powder");
        b.tFill(3698, 3776, 3700, 3777, -1, -1, "minecraft:black_concrete");
        int[][] caps = {{3683, 3790, 7, 3}, {3690, 3796, 9, 4}, {3697, 3787, 6, 3}, {3684, 3797, 5, 2}, {3703, 3791, 8, 3},
                {3692, 3785, 5, 2}};
        for (int i = 0; i < caps.length; i++) {
            int[] c = caps[i];
            mushroom(b, c[0], c[1], c[2], c[3], i % 2 == 0 ? "minecraft:red_mushroom_block" : "minecraft:brown_mushroom_block");
        }
        b.tSet(IslandsStructures.FOREST_TREE[0], IslandsStructures.FOREST_TREE[1], 0, rc(IslandsBlocks.MUSHTREE));
        b.tFloor(3693, 3772, 3694, 3800, PATH);
        b.tSet(3688, 3792, 0, "minecraft:brown_mushroom");
        b.tSet(3700, 3783, 0, "minecraft:red_mushroom");
        b.tSet(3682, 3784, 0, "minecraft:brown_mushroom");
        return b.build();
    }

    /** The Wyvern Cave: a rocky entrance in the north of the island and the cave carved beneath it. */
    static StructureDef wyvernCave() {
        Builder b = StructureDef.builder("isl_wyvern_cave", "Wyvern Cave").replaces(IslandsPlaces.WYVERN_CAVE);
        b.tFlatten(3666, 3838, 3688, 3862, "minecraft:coarse_dirt");
        int[] e = IslandsStructures.WYVERN_CAVE_ENTRANCE;
        cave(b, e[0], e[1], IslandsStructures.WYVERN_CAVE_HALF, IslandsStructures.WYVERN_CAVE_HALF, "minecraft:tuff",
                "minecraft:packed_ice");
        // Frost on the cave floor (the wyverns' icy breath) and bones of their prey.
        for (int[] r : new int[][]{{3668, 3840}, {3686, 3842}, {3670, 3856}, {3684, 3858}, {3666, 3848}}) {
            rocks(b, r[0], r[1], "minecraft:tuff");
        }
        b.tSet(3680, 3841, 0, "minecraft:bone_block");
        b.tSet(3674, 3843, 0, "minecraft:skeleton_skull");
        b.tSet(IslandsStructures.MEADOW_TREE[0], IslandsStructures.MEADOW_TREE[1], 0, rc(IslandsBlocks.MUSHTREE));
        mushroom(b, IslandsStructures.MEADOW_TREE[0] + 3, IslandsStructures.MEADOW_TREE[1] - 1, 6, 3, "minecraft:red_mushroom_block");
        return b.build();
    }
}
