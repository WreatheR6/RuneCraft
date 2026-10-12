package com.runecraft.osrs.data;

/** One line of a drop table: {@code item} x[min,max] with a 1/{@code rarity} chance. */
public record Drop(String item, int min, int max, int rarity) {
    public static Drop always(String item) {
        return new Drop(item, 1, 1, 1);
    }

    public static Drop of(String item, int min, int max, int rarity) {
        return new Drop(item, min, max, rarity);
    }

    /** Special marker that rolls the herb drop table ({@link DropTables#HERB}). */
    public static Drop herbs(int rarity) {
        return new Drop("#herbs", 1, 1, rarity);
    }

    /** Special marker that rolls the gem drop table ({@link DropTables#GEM}; about half its slots are empty). */
    public static Drop gems(int rarity) {
        return new Drop("#gems", 1, 1, rarity);
    }

    /** Special marker that rolls the rare drop table ({@link DropTables#RARE}), which includes the gem table. */
    public static Drop rare(int rarity) {
        return new Drop("#rare", 1, 1, rarity);
    }

    /** Special marker that rolls the generic herb seed table (the rare seed drop table). */
    public static Drop seeds(int rarity) {
        return new Drop("#seeds", 1, 1, rarity);
    }

    /** Special marker that rolls the allotment seed drop table ({@link DropTables#ALLOTMENT_SEEDS}). */
    public static Drop allotmentSeeds(int rarity) {
        return new Drop("#allotment_seeds", 1, 1, rarity);
    }

    /** Special marker that rolls the rare seed drop table ({@link DropTables#RARE_SEEDS}). */
    public static Drop rareSeeds(int rarity) {
        return new Drop("#rare_seeds", 1, 1, rarity);
    }

    /** Special marker that rolls the uncommon seed drop table ({@link DropTables#UNCOMMON_SEEDS}). */
    public static Drop uncommonSeeds(int rarity) {
        return new Drop("#uncommon_seeds", 1, 1, rarity);
    }

    /** Special marker that rolls the tree-herb seed drop table ({@link DropTables#TREE_HERB_SEEDS}). */
    public static Drop treeHerbSeeds(int rarity) {
        return new Drop("#tree_herb_seeds", 1, 1, rarity);
    }
}
