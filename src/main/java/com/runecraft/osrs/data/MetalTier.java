package com.runecraft.osrs.data;

/** Metal tiers for weapons, armour and tools. */
public enum MetalTier {
    BRONZE("Bronze", 1, 0xB08D57, "bronze_bar", 0, 12.5, 1.0),
    IRON("Iron", 1, 0x8A8A8E, "iron_bar", 15, 25.0, 3.5),
    STEEL("Steel", 5, 0xB8B8C0, "steel_bar", 30, 37.5, 12.5),
    BLACK("Black", 10, 0x2E2E34, null, 0, 0, 24.0),
    MITHRIL("Mithril", 20, 0x4A5A9C, "mithril_bar", 50, 50.0, 32.5),
    ADAMANT("Adamant", 30, 0x4C7A4C, "adamantite_bar", 70, 62.5, 80.0),
    RUNE("Rune", 40, 0x4FB8C8, "runite_bar", 85, 75.0, 800.0),
    DRAGON("Dragon", 60, 0xB02020, null, 0, 0, 3000.0);

    private final String displayName;
    private final int requirement;
    private final int color;
    private final String bar;
    private final int smithingBase;
    private final double xpPerBar;
    private final double valueMultiplier;

    MetalTier(String displayName, int requirement, int color, String bar, int smithingBase, double xpPerBar, double valueMultiplier) {
        this.displayName = displayName;
        this.requirement = requirement;
        this.color = color;
        this.bar = bar;
        this.smithingBase = smithingBase;
        this.xpPerBar = xpPerBar;
        this.valueMultiplier = valueMultiplier;
    }

    public String displayName() {
        return displayName;
    }

    /** Attack level to wield weapons / Defence level to wear armour of this tier. */
    public int requirement() {
        return requirement;
    }

    public int color() {
        return color;
    }

    /** Bar used to smith this tier, or null when the tier can't be smithed (black, dragon). */
    public String bar() {
        return bar;
    }

    public boolean smithable() {
        return bar != null;
    }

    /**
     * Base Smithing level of the tier: an item's level is this plus its offset (dagger +0, axe +1,
     * mace +2, ... platebody +18). Bronze is 0 so a bronze dagger/axe needs level 1 and a bronze
     * platebody 18, like in-game; use {@link #smithingLevel(int)} to clamp to 1-99.
     */
    public int smithingBase() {
        return smithingBase;
    }

    /** Smithing level for an item with the given offset from the tier base, clamped to 1-99. */
    public int smithingLevel(int offset) {
        return Math.max(1, Math.min(99, smithingBase + offset));
    }

    public double xpPerBar() {
        return xpPerBar;
    }

    public double valueMultiplier() {
        return valueMultiplier;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /** Tier factor relative to rune, for scaling stats (from scimitar slash bonus ratios). */
    public double statFactor() {
        return switch (this) {
            case BRONZE -> 0.156;
            case IRON -> 0.222;
            case STEEL -> 0.333;
            case BLACK -> 0.422;
            case MITHRIL -> 0.467;
            case ADAMANT -> 0.644;
            case RUNE -> 1.0;
            case DRAGON -> 1.49;
        };
    }
}
