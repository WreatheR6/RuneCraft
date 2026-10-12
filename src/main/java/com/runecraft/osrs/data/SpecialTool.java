package com.runecraft.osrs.data;

import com.runecraft.osrs.Skill;

/**
 * Gathering tools with bonuses beyond their metal tier (OSRS):
 * <ul>
 *   <li>Dragon and infernal pickaxes roll every 3 ticks with a 1/6 chance of a 2-tick roll (about
 *   2.83 ticks on average); the crystal pickaxe has a 1/4 chance (2.75).</li>
 *   <li>The crystal axe is a little better than the dragon axe; the infernal axe chops like a dragon
 *   axe and burns a third of the logs it cuts for half their Firemaking XP.</li>
 *   <li>The infernal pickaxe smelts a third of the ore it mines for half the bar's Smithing XP.</li>
 *   <li>The dragon and infernal harpoons catch fish 20% faster than a harpoon and the crystal harpoon
 *   35% faster; the infernal harpoon cooks a third of its catches for half the Cooking XP.</li>
 * </ul>
 */
public enum SpecialTool {
    INFERNAL_AXE("infernal_axe", Skill.WOODCUTTING, 61, 3.75, 0, 1.0, 1),
    CRYSTAL_AXE("crystal_axe", Skill.WOODCUTTING, 71, 4.125, 0, 1.0, 2),           // multiplier approx.
    INFERNAL_PICKAXE("infernal_pickaxe", Skill.MINING, 61, 0, 1.0 / 6, 1.0, 1),
    CRYSTAL_PICKAXE("crystal_pickaxe", Skill.MINING, 71, 0, 1.0 / 4, 1.0, 2),
    DRAGON_HARPOON("dragon_harpoon", Skill.FISHING, 61, 0, 0, 1.20, 1),
    INFERNAL_HARPOON("infernal_harpoon", Skill.FISHING, 75, 0, 0, 1.20, 2),
    CRYSTAL_HARPOON("crystal_harpoon", Skill.FISHING, 71, 0, 0, 1.35, 3);

    /** Chance the infernal tools' passive effect triggers on a successful gather. */
    public static final double INFERNAL_CHANCE = 1.0 / 3.0;
    /** Two-tick roll chance of a dragon pickaxe (and the 3rd age pickaxe, which mines like one). */
    public static final double DRAGON_PICKAXE_TWO_TICK = 1.0 / 6;

    private final String itemId;
    private final Skill skill;
    private final int level;
    private final double woodcuttingMultiplier;
    private final double twoTickChance;
    private final double catchMultiplier;
    private final int rank;

    SpecialTool(String itemId, Skill skill, int level, double woodcuttingMultiplier, double twoTickChance, double catchMultiplier,
                int rank) {
        this.itemId = itemId;
        this.skill = skill;
        this.level = level;
        this.woodcuttingMultiplier = woodcuttingMultiplier;
        this.twoTickChance = twoTickChance;
        this.catchMultiplier = catchMultiplier;
        this.rank = rank;
    }

    public String itemId() {
        return itemId;
    }

    public Skill skill() {
        return skill;
    }

    /** Skill level needed to use it. */
    public int level() {
        return level;
    }

    /** Preference among tools of the same metal tier (higher is better). */
    public int rank() {
        return rank;
    }

    public double catchMultiplier() {
        return catchMultiplier;
    }

    public static SpecialTool of(String itemId) {
        for (SpecialTool t : values()) {
            if (t.itemId.equals(itemId)) {
                return t;
            }
        }
        return null;
    }

    /** Level needed to use an axe/pickaxe item of the given tier. */
    public static int levelFor(ToolTier tier, String itemId) {
        SpecialTool t = of(itemId);
        return t != null ? t.level : tier.level();
    }

    /** Woodcutting success multiplier for an axe item of the given tier. */
    public static double woodcuttingMultiplier(ToolTier tier, String itemId) {
        SpecialTool t = of(itemId);
        return t != null && t.woodcuttingMultiplier > 0 ? t.woodcuttingMultiplier : tier.woodcuttingMultiplier();
    }

    /** Chance of a 2-tick roll for a pickaxe item of the given tier (0 below dragon). */
    public static double twoTickChance(ToolTier tier, String itemId) {
        SpecialTool t = of(itemId);
        if (t != null && t.twoTickChance > 0) {
            return t.twoTickChance;
        }
        return tier == ToolTier.DRAGON ? DRAGON_PICKAXE_TWO_TICK : 0;
    }

    /** Game ticks until the next mining roll; {@code roll} is uniform in [0, 1). */
    public static int miningTicks(ToolTier tier, String itemId, double roll) {
        return roll < twoTickChance(tier, itemId) ? 2 : tier.miningTicks();
    }

    /** Harpoon fishing catch-rate multiplier for a harpoon item (1.0 for a plain harpoon). */
    public static double harpoonMultiplier(String itemId) {
        SpecialTool t = of(itemId);
        return t != null && t.skill == Skill.FISHING ? t.catchMultiplier : 1.0;
    }

    /** True for items that work as a harpoon. */
    public static boolean isHarpoon(String itemId) {
        return itemId.equals("harpoon") || itemId.equals("barb_tail_harpoon")
                || (of(itemId) != null && of(itemId).skill == Skill.FISHING);
    }

    /** Bar the infernal pickaxe smelts an ore into, or null (coal, essence and gems aren't smelted). */
    public static Bar infernalBar(String ore) {
        return switch (ore) {
            case "copper_ore", "tin_ore" -> Bar.BRONZE;   // approx.: OSRS gives a bronze bar's worth of XP
            case "iron_ore" -> Bar.IRON;
            case "silver_ore" -> Bar.SILVER;
            case "gold_ore" -> Bar.GOLD;
            case "mithril_ore" -> Bar.MITHRIL;
            case "adamantite_ore" -> Bar.ADAMANTITE;
            case "runite_ore" -> Bar.RUNITE;
            default -> null;
        };
    }

    /** Smithing XP the infernal pickaxe gives for smelting an ore (half the bar's XP). */
    public static double infernalSmithingXp(String ore) {
        Bar bar = infernalBar(ore);
        return bar == null ? 0 : bar.xp() / 2;
    }

    /** Firemaking XP the infernal axe gives for burning a log of this tree (half the normal XP). */
    public static double infernalFiremakingXp(Tree tree) {
        return tree.firemakingXp() / 2;
    }

    /** Cooking XP the infernal harpoon gives for cooking a fish (half the normal XP). */
    public static double infernalCookingXp(Fish fish) {
        return fish.cookXp() / 2;
    }
}
