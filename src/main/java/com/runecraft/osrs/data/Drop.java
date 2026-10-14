package com.runecraft.osrs.data;

/**
 * One line of a drop table: {@code item} x[min,max] with a 1/{@code rarity} chance. A potion dropped with fewer than
 * four doses is written {@code id(n)} (e.g. {@code prayer_potion(2)}, the wiki's "Prayer potion(2)"); see
 * {@link #doses()} and {@link #baseItem()}.
 */
public record Drop(String item, int min, int max, int rarity) {
    /** The item id without a dose suffix ({@code prayer_potion(2)} gives {@code prayer_potion}). */
    public String baseItem() {
        return baseOf(item);
    }

    /** Doses (1-3) of a potion line written {@code id(n)}, or 0 for a full potion and every other item. */
    public int doses() {
        return dosesOf(item);
    }

    /** Doses (1-3) encoded in a drop item id, or 0 when it has no dose suffix. */
    public static int dosesOf(String item) {
        int n = item == null ? 0 : item.length();
        if (n > 3 && item.charAt(n - 3) == '(' && item.charAt(n - 1) == ')') {
            char c = item.charAt(n - 2);
            if (c >= '1' && c <= '3') {
                return c - '0';
            }
        }
        return 0;
    }

    /** A drop item id without its dose suffix. */
    public static String baseOf(String item) {
        return dosesOf(item) > 0 ? item.substring(0, item.length() - 3) : item;
    }

    /** A potion with {@code doses} doses (1-3), as a drop item id. */
    public static String dosed(String potion, int doses) {
        if (doses < 1 || doses > 3) {
            throw new IllegalArgumentException("doses " + doses);
        }
        return potion + "(" + doses + ")";
    }

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
