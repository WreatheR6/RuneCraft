package com.runecraft.osrs;

/** Make-X style menus (production skills and shops). */
public enum MenuType {
    SMELTING("What would you like to smelt?"),
    SMITHING("What would you like to make?"),
    COOKING("How many would you like to cook?"),
    FLETCHING("What would you like to fletch?"),
    GEM_CUTTING("What would you like to cut?"),
    LEATHERWORKING("What would you like to make?"),
    SPINNING("What would you like to spin?"),
    TANNING("Tanning - 1 coin per hide"),
    HERBLORE("What would you like to make?"),
    CONSTRUCTION("What would you like to build?"),
    SHOP_GENERAL("General Store"),
    SHOP_RUNES("Aubury's Rune Shop"),
    SHOP_WEAPONS("Varrock Swordshop"),
    SHOP_TOOLS("Bob's Brilliant Axes"),
    SHOP_ARCHERY("Lowe's Archery Emporium");

    private final String title;

    MenuType(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }

    public boolean isShop() {
        return name().startsWith("SHOP_");
    }
}
