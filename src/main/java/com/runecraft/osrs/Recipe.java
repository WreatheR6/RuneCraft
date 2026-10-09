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
    private final String requiredTool;

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
        this.requiredTool = b.requiredTool;
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

    /**
     * Cooking success chance. Rises linearly from 45% at the requirement to 100% at the stop-burn
     * level; ranges burn less than fires (treated as 3 levels higher), like in-game.
     */
    public double cookingSuccess(int cookingLevel, boolean range) {
        if (stopBurn <= 0) {
            return 1.0;
        }
        int effective = cookingLevel + (range ? 3 : 0);
        if (effective >= stopBurn) {
            return 1.0;
        }
        double span = Math.max(1, stopBurn - level);
        return Math.min(1.0, 0.45 + 0.55 * Math.max(0, effective - level) / span);
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
        private String requiredTool;

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

        public Builder tool(String tool) {
            this.requiredTool = tool;
            return this;
        }

        public Recipe build() {
            return new Recipe(this);
        }
    }
}
