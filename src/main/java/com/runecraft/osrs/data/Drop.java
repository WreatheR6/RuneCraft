package com.runecraft.osrs.data;

/** One line of a drop table: {@code item} x[min,max] with a 1/{@code rarity} chance. */
public record Drop(String item, int min, int max, int rarity) {
    public static Drop always(String item) {
        return new Drop(item, 1, 1, 1);
    }

    public static Drop of(String item, int min, int max, int rarity) {
        return new Drop(item, min, max, rarity);
    }

    /** Special marker that rolls the shared herb table. */
    public static Drop herbs(int rarity) {
        return new Drop("#herbs", 1, 1, rarity);
    }

    /** Special marker that rolls the gem table. */
    public static Drop gems(int rarity) {
        return new Drop("#gems", 1, 1, rarity);
    }

    /** Special marker that rolls the herb seed table. */
    public static Drop seeds(int rarity) {
        return new Drop("#seeds", 1, 1, rarity);
    }
}
