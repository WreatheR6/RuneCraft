package com.runecraft.osrs.data;

/** Arrows: fletching level and XP per arrow, ranged strength, and the arrowtips smithing level. */
public enum ArrowTier {
    BRONZE(MetalTier.BRONZE, 1, 1.3, 7, 1, 5),
    IRON(MetalTier.IRON, 15, 2.5, 10, 1, 20),
    STEEL(MetalTier.STEEL, 30, 5.0, 16, 5, 35),
    MITHRIL(MetalTier.MITHRIL, 45, 7.5, 22, 20, 55),
    ADAMANT(MetalTier.ADAMANT, 60, 10.0, 31, 30, 75),
    RUNE(MetalTier.RUNE, 75, 12.5, 49, 40, 90);

    private final MetalTier metal;
    private final int fletchingLevel;
    private final double xpEach;
    private final int rangedStrength;
    private final int rangedLevel;
    private final int smithingLevel;

    ArrowTier(MetalTier metal, int fletchingLevel, double xpEach, int rangedStrength, int rangedLevel, int smithingLevel) {
        this.metal = metal;
        this.fletchingLevel = fletchingLevel;
        this.xpEach = xpEach;
        this.rangedStrength = rangedStrength;
        this.rangedLevel = rangedLevel;
        this.smithingLevel = smithingLevel;
    }

    public MetalTier metal() {
        return metal;
    }

    public String arrowId() {
        return metal.id() + "_arrow";
    }

    public String tipsId() {
        return metal.id() + "_arrowtips";
    }

    public int fletchingLevel() {
        return fletchingLevel;
    }

    public double xpEach() {
        return xpEach;
    }

    public int rangedStrength() {
        return rangedStrength;
    }

    public int rangedLevel() {
        return rangedLevel;
    }

    public int smithingLevel() {
        return smithingLevel;
    }
}
