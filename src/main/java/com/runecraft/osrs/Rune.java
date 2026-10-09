package com.runecraft.osrs;

/** Runes, with the item id used for each in the Minecraft registry. */
public enum Rune {
    AIR, WATER, EARTH, FIRE, MIND, BODY, COSMIC, CHAOS, ASTRAL, NATURE, LAW, DEATH, BLOOD, SOUL, WRATH;

    public String itemId() {
        return name().toLowerCase(java.util.Locale.ROOT) + "_rune";
    }

    public String displayName() {
        String n = name().toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(n.charAt(0)) + n.substring(1) + " rune";
    }

    public boolean isElemental() {
        return this == AIR || this == WATER || this == EARTH || this == FIRE;
    }
}
