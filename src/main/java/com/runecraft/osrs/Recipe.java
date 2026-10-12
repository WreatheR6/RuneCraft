package com.runecraft.osrs;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A Make-X option: consumes {@link #inputs()}, produces {@link #outputs()} and grants XP after
 * {@link #ticks()} game ticks. Item ids without a namespace are RuneCraft items.
 */
public final class Recipe {
    private final String id;
    private final MenuType menu;
    private final String name;
    private final Skill skill;
    private final int level;
    private final int xpTenths;
    private final Map<String, Integer> inputs;
    private final Map<String, Integer> outputs;
    private final int ticks;
    private final double successChance;
    private final String failItem;
    private final int stopBurn;
    private final int gauntletStopBurn;
    private final String requiredTool;
    private final int rollLow;
    private final int rollHigh;
    private final int rollCertainAt;
    private final int failXpTenths;

    private Recipe(Builder b) {
        this.id = b.id;
        this.menu = b.menu;
        this.name = b.name;
        this.skill = b.skill;
        this.level = b.level;
        this.xpTenths = b.xpTenths;
        this.inputs = Collections.unmodifiableMap(b.inputs);
        this.outputs = Collections.unmodifiableMap(b.outputs);
        this.ticks = b.ticks;
        this.successChance = b.successChance;
        this.failItem = b.failItem;
        this.stopBurn = b.stopBurn;
        this.gauntletStopBurn = b.gauntletStopBurn;
        this.requiredTool = b.requiredTool;
        this.rollLow = b.rollLow;
        this.rollHigh = b.rollHigh;
        this.rollCertainAt = b.rollCertainAt;
        this.failXpTenths = b.failXpTenths;
    }

    public String id() {
        return id;
    }

    public MenuType menu() {
        return menu;
    }

    public String name() {
        return name;
    }

    /** Skill trained, or null for shop purchases. */
    public Skill skill() {
        return skill;
    }

    public int level() {
        return level;
    }

    public int xpTenths() {
        return xpTenths;
    }

    public Map<String, Integer> inputs() {
        return inputs;
    }

    public Map<String, Integer> outputs() {
        return outputs;
    }

    public int ticks() {
        return ticks;
    }

    /** Fixed success chance (e.g. 0.5 for iron bars); 1.0 for most recipes. */
    public double successChance() {
        return successChance;
    }

    /**
     * Success chance at a skill level: the level-based skilling roll when the recipe has one (e.g.
     * cutting opals, jades and red topaz can crush the gem), otherwise {@link #successChance()}.
     */
    public double successChance(int level) {
        if (rollHigh <= 0) {
            return successChance;
        }
        if (rollCertainAt > 0 && level >= rollCertainAt) {
            return 1.0;
        }
        return Chance.lerp(level, rollLow, rollHigh);
    }

    /** XP (in tenths) granted when the recipe fails (e.g. 3.8 Crafting XP for a crushed opal). */
    public int failXpTenths() {
        return failXpTenths;
    }

    /** Item given on failure (burnt food), or null. */
    public String failItem() {
        return failItem;
    }

    /** For cooking: level at which you stop burning on a fire; 0 if the recipe can't burn. */
    public int stopBurn() {
        return stopBurn;
    }

    /** Tool that must be in the inventory (not consumed), or null. */
    public String requiredTool() {
        return requiredTool;
    }

    /** Primary output item id (what the menu shows as the icon). */
    public String icon() {
        return outputs.isEmpty() ? inputs.keySet().iterator().next() : outputs.keySet().iterator().next();
    }

    /** Stop-burn level while wearing/carrying cooking gauntlets, or 0 if they don't help. */
    public int gauntletStopBurn() {
        return gauntletStopBurn;
    }

    /** Cooking success chance without cooking gauntlets. */
    public double cookingSuccess(int cookingLevel, boolean range) {
        return cookingSuccess(cookingLevel, range, false);
    }

    /**
     * Cooking success chance: the OSRS skilling roll with the food's low/high burn values (see
     * {@link BurnRates}); some foods burn less on a range, and cooking gauntlets help lobster,
     * swordfish, monkfish, shark and anglerfish. Sharks, sea turtles and manta rays (and other food
     * with a stop-burn of 100) can never stop burning without gauntlets.
     */
    public double cookingSuccess(int cookingLevel, boolean range, boolean gauntlets) {
        return cookingSuccess(cookingLevel, range, gauntlets, 0);
    }

    /**
     * Cooking success chance with a burn reduction on top, e.g. 0.05 for the Hosidius kitchen range
     * (5 percentage points less likely to burn, approx.). Food that can never stop burning keeps a
     * small burn chance.
     */
    public double cookingSuccess(int cookingLevel, boolean range, boolean gauntlets, double burnReduction) {
        double base = cookingSuccessBase(cookingLevel, range, gauntlets);
        if (burnReduction <= 0 || base >= 1.0) {
            return base;
        }
        int stop = gauntlets && gauntletStopBurn > 0 ? gauntletStopBurn : stopBurn;
        return Math.min(stop >= 100 ? 0.98 : 1.0, base + burnReduction);
    }

    private double cookingSuccessBase(int cookingLevel, boolean range, boolean gauntlets) {
        if (stopBurn <= 0) {
            return 1.0;
        }
        return BurnRates.success(icon(), stopBurn, gauntletStopBurn, cookingLevel, range, gauntlets);
    }

    public static Builder builder(String id, MenuType menu, String name) {
        return new Builder(id, menu, name);
    }

    public static final class Builder {
        private final String id;
        private final MenuType menu;
        private final String name;
        private Skill skill;
        private int level = 1;
        private int xpTenths;
        private final Map<String, Integer> inputs = new LinkedHashMap<>();
        private final Map<String, Integer> outputs = new LinkedHashMap<>();
        private int ticks = 3;
        private double successChance = 1.0;
        private String failItem;
        private int stopBurn;
        private int gauntletStopBurn;
        private String requiredTool;
        private int rollLow;
        private int rollHigh;
        private int rollCertainAt;
        private int failXpTenths;

        private Builder(String id, MenuType menu, String name) {
            this.id = id;
            this.menu = menu;
            this.name = name;
        }

        public Builder skill(Skill skill, int level, double xp) {
            this.skill = skill;
            this.level = level;
            this.xpTenths = Experience.tenths(xp);
            return this;
        }

        public Builder in(String item, int count) {
            inputs.merge(item, count, Integer::sum);
            return this;
        }

        public Builder out(String item, int count) {
            outputs.merge(item, count, Integer::sum);
            return this;
        }

        public Builder ticks(int ticks) {
            this.ticks = ticks;
            return this;
        }

        public Builder success(double chance) {
            this.successChance = chance;
            return this;
        }

        public Builder burn(int stopBurn, String failItem) {
            this.stopBurn = stopBurn;
            this.failItem = failItem;
            return this;
        }

        /** Stop-burn level with cooking gauntlets (lobster, swordfish, monkfish, shark, anglerfish). */
        public Builder gauntlets(int stopBurn) {
            this.gauntletStopBurn = stopBurn;
            return this;
        }

        /**
         * Level-based success: the OSRS skilling roll between {@code low} (level 1) and {@code high}
         * (level 99) out of 256, always succeeding from {@code certainAt} (0 = never). A failure
         * gives {@code failItem} and {@code failXp}.
         */
        public Builder roll(int low, int high, int certainAt, String failItem, double failXp) {
            this.rollLow = low;
            this.rollHigh = high;
            this.rollCertainAt = certainAt;
            this.failItem = failItem;
            this.failXpTenths = Experience.tenths(failXp);
            return this;
        }

        public Builder tool(String tool) {
            this.requiredTool = tool;
            return this;
        }

        public Recipe build() {
            return new Recipe(this);
        }
    }
}
