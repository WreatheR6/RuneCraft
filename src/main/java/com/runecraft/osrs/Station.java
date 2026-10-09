package com.runecraft.osrs;

/** Interactive stations that landmarks can contain. Each maps to a block in the mod. */
public enum Station {
    BANK_BOOTH("bank_booth"),
    FURNACE("furnace"),
    ANVIL("anvil"),
    RANGE("cooking_range"),
    PRAYER_ALTAR("prayer_altar"),
    GENERAL_STORE("general_store"),
    RUNE_SHOP("rune_shop"),
    WEAPON_SHOP("weapon_shop"),
    TOOL_SHOP("tool_shop"),
    ARCHERY_SHOP("archery_shop"),
    SPINNING_WHEEL("spinning_wheel"),
    TANNER("tanning_rack"),
    WORKBENCH("construction_workbench"),
    HERB_PATCH("herb_patch"),
    ALLOTMENT_PATCH("allotment_patch"),
    CAKE_STALL("cake_stall"),
    SILK_STALL("silk_stall"),
    FUR_STALL("fur_stall"),
    SILVER_STALL("silver_stall"),
    SPICE_STALL("spice_stall"),
    GEM_STALL("gem_stall"),
    PORTAL("gielinor_portal"),
    SLAYER_MASTER("slayer_master"),
    HERBLORE_TABLE("apothecary_table");

    private final String blockId;

    Station(String blockId) {
        this.blockId = blockId;
    }

    public String blockId() {
        return blockId;
    }
}
