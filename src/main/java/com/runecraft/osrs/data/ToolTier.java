package com.runecraft.osrs.data;

/**
 * Axe and pickaxe tiers. Woodcutting multiplies the tree's success values by
 * {@link #woodcuttingMultiplier()} (e.g. a rune axe on a normal tree is 224-700/256 vs bronze
 * 64-200/256). Mining rolls every {@link #miningTicks()} game ticks.
 */
public enum ToolTier {
    BRONZE(MetalTier.BRONZE, 1, 1.0, 8, 16),
    IRON(MetalTier.IRON, 1, 1.5, 7, 56),
    STEEL(MetalTier.STEEL, 6, 2.0, 6, 200),
    BLACK(MetalTier.BLACK, 11, 2.25, 5, 384),
    MITHRIL(MetalTier.MITHRIL, 21, 2.5, 5, 520),
    ADAMANT(MetalTier.ADAMANT, 31, 3.0, 4, 1280),
    RUNE(MetalTier.RUNE, 41, 3.5, 3, 12800),
    DRAGON(MetalTier.DRAGON, 61, 3.75, 3, 56000);

    private final MetalTier metal;
    private final int level;
    private final double woodcuttingMultiplier;
    private final int miningTicks;
    private final int value;

    ToolTier(MetalTier metal, int level, double woodcuttingMultiplier, int miningTicks, int value) {
        this.metal = metal;
        this.level = level;
        this.woodcuttingMultiplier = woodcuttingMultiplier;
        this.miningTicks = miningTicks;
        this.value = value;
    }

    public MetalTier metal() {
        return metal;
    }

    /** Woodcutting / Mining level needed to use it (Attack level to wield it isn't required here). */
    public int level() {
        return level;
    }

    public double woodcuttingMultiplier() {
        return woodcuttingMultiplier;
    }

    public int miningTicks() {
        return miningTicks;
    }

    public int value() {
        return value;
    }

    public String axeId() {
        return metal.id() + "_axe";
    }

    public String pickaxeId() {
        return metal.id() + "_pickaxe";
    }
}
