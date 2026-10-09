package com.runecraft.osrs;

import java.util.Arrays;

/**
 * A player's experience in every skill plus temporary boosts/drains (potions, prayers that
 * lower stats, etc). Pure data; the Minecraft layer persists and syncs it.
 */
public final class SkillSet {
    private final int[] xpTenths = new int[Skill.count()];
    /** Signed offset from the real level, e.g. +5 after a super attack potion. */
    private final int[] boosts = new int[Skill.count()];

    public SkillSet() {
        for (Skill s : Skill.values()) {
            xpTenths[s.ordinal()] = s.startingXpTenths();
        }
    }

    public int xpTenths(Skill skill) {
        return xpTenths[skill.ordinal()];
    }

    public int xp(Skill skill) {
        return xpTenths[skill.ordinal()] / 10;
    }

    public void setXpTenths(Skill skill, int tenths) {
        xpTenths[skill.ordinal()] = Math.max(0, Math.min(Experience.MAX_XP_TENTHS, tenths));
    }

    /** Real (unboosted) level, 1-99. */
    public int level(Skill skill) {
        return Experience.levelForTenths(xpTenths[skill.ordinal()]);
    }

    public int virtualLevel(Skill skill) {
        return Experience.virtualLevelForXp(xp(skill));
    }

    /** Level including boosts and drains, never below 0. */
    public int currentLevel(Skill skill) {
        return Math.max(0, level(skill) + boosts[skill.ordinal()]);
    }

    public int boost(Skill skill) {
        return boosts[skill.ordinal()];
    }

    public void setBoost(Skill skill, int boost) {
        boosts[skill.ordinal()] = boost;
    }

    /**
     * Applies a potion style boost: {@code flat + percent * level}, never stacking above what the
     * same potion would give from an unboosted level (matching the game's behaviour).
     */
    public void applyBoost(Skill skill, int flat, double percent) {
        int amount = flat + (int) Math.floor(level(skill) * percent);
        if (boosts[skill.ordinal()] < amount) {
            boosts[skill.ordinal()] = amount;
        }
    }

    /** Restores drained stats toward the real level by {@code flat + percent * level}. */
    public void restore(Skill skill, int flat, double percent) {
        int amount = flat + (int) Math.floor(level(skill) * percent);
        if (boosts[skill.ordinal()] < 0) {
            boosts[skill.ordinal()] = Math.min(0, boosts[skill.ordinal()] + amount);
        }
    }

    /** Called once a minute: boosts and drains move one level back toward normal. */
    public void decayBoosts(boolean keepHitpoints) {
        for (int i = 0; i < boosts.length; i++) {
            if (keepHitpoints && i == Skill.HITPOINTS.ordinal()) {
                continue;
            }
            if (boosts[i] > 0) {
                boosts[i]--;
            } else if (boosts[i] < 0) {
                boosts[i]++;
            }
        }
    }

    /** Adds XP; returns the number of real levels gained (0 if none). */
    public int addXp(Skill skill, int tenths) {
        int before = level(skill);
        xpTenths[skill.ordinal()] = Experience.addTenths(xpTenths[skill.ordinal()], tenths);
        return level(skill) - before;
    }

    public int totalLevel() {
        int total = 0;
        for (Skill s : Skill.values()) {
            total += level(s);
        }
        return total;
    }

    public long totalXp() {
        long total = 0;
        for (int t : xpTenths) {
            total += t / 10;
        }
        return total;
    }

    public int combatLevel() {
        return CombatLevel.of(this);
    }

    public int[] rawXp() {
        return Arrays.copyOf(xpTenths, xpTenths.length);
    }

    public int[] rawBoosts() {
        return Arrays.copyOf(boosts, boosts.length);
    }

    public void load(int[] xp, int[] boost) {
        for (int i = 0; i < Math.min(xp.length, xpTenths.length); i++) {
            xpTenths[i] = Math.max(0, Math.min(Experience.MAX_XP_TENTHS, xp[i]));
        }
        // Skills added after a save was written start at their default.
        for (int i = xp.length; i < xpTenths.length; i++) {
            xpTenths[i] = Skill.byIndex(i).startingXpTenths();
        }
        Arrays.fill(boosts, 0);
        if (boost != null) {
            System.arraycopy(boost, 0, boosts, 0, Math.min(boost.length, boosts.length));
        }
    }
}
