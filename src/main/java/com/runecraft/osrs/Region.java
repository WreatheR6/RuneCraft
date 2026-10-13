package com.runecraft.osrs;

/** Kingdoms of Gielinor. Each maps to a biome in the Gielinor dimension. */
public enum Region {
    MISTHALIN("Misthalin"),
    LUMBRIDGE_SWAMP("Lumbridge Swamp"),
    ASGARNIA("Asgarnia"),
    WILDERNESS("Wilderness"),
    KHARIDIAN_DESERT("Kharidian Desert"),
    MORYTANIA("Morytania"),
    KANDARIN("Kandarin"),
    FELDIP_HILLS("Feldip Hills"),
    FREMENNIK_PROVINCE("Fremennik Province"),
    TROLL_COUNTRY("Troll Country"),
    KARAMJA("Karamja"),
    TIRANNWN("Tirannwn"),
    UNCHARTED_ISLES("Uncharted Isles"),
    // Appended (ordinals are stable): the regions off the main continent that Konar quo Maten names.
    KOUREND("Kourend & Kebos"),
    VARLAMORE("Varlamore"),
    FOSSIL_ISLAND("Fossil Island"),
    LITHKREN("Lithkren");

    private final String displayName;

    Region(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /** Whether this is one of the kingdoms of the main continent (not Zeah, Varlamore or an island). */
    public boolean mainland() {
        return ordinal() < UNCHARTED_ISLES.ordinal();
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
