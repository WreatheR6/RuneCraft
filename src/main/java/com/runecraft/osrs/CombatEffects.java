package com.runecraft.osrs;

/**
 * Status effects of potions and combat spells: Saradomin brew overhealing, the curse spells' stat drains
 * (Confuse, Weaken, Curse, Vulnerability, Enfeeble, Stun) and Tele Block.
 */
public final class CombatEffects {
    private CombatEffects() {
    }

    // ---------------------------------------------------------------- Saradomin brew

    /** Hitpoints a Saradomin brew dose heals: 15% of the Hitpoints level + 2. */
    public static int brewHeal(int hitpointsLevel) {
        return 2 + hitpointsLevel * 15 / 100;
    }

    /** Highest Hitpoints a brew can reach: the level plus one dose's heal (it can boost above the maximum). */
    public static int brewCap(int hitpointsLevel) {
        return hitpointsLevel + brewHeal(hitpointsLevel);
    }

    /** Hitpoints after one brew dose from {@code current} (never lowers an already higher boost). */
    public static int afterBrew(int current, int hitpointsLevel) {
        return Math.max(current, Math.min(brewCap(hitpointsLevel), current + brewHeal(hitpointsLevel)));
    }

    /** Defence boost of a brew dose: 20% + 2 of the Defence level. */
    public static int brewDefenceBoost(int defenceLevel) {
        return 2 + defenceLevel * 20 / 100;
    }

    /** Attack/Strength/Magic/Ranged drain of a brew dose: 10% + 2 of the current (boosted) level. */
    public static int brewDrain(int currentLevel) {
        return 2 + currentLevel * 10 / 100;
    }

    // ---------------------------------------------------------------- curse spells

    /** A curse spell's drain: which skill and by how many percent. */
    public record Curse(Skill skill, int percent) {
    }

    /** The stat drain of a standard-spellbook curse, or null if the spell isn't one. */
    public static Curse curse(String spellId) {
        return switch (spellId) {
            case "confuse" -> new Curse(Skill.ATTACK, 5);
            case "weaken" -> new Curse(Skill.STRENGTH, 5);
            case "curse" -> new Curse(Skill.DEFENCE, 5);
            case "vulnerability" -> new Curse(Skill.DEFENCE, 10);
            case "enfeeble" -> new Curse(Skill.STRENGTH, 10);
            case "stun" -> new Curse(Skill.ATTACK, 10);
            default -> null;
        };
    }

    /**
     * Levels a curse lowers: {@code percent} of the target's base level (at least 1). Curses don't stack: if the
     * stat is already lowered by that much or more the spell has no effect (0).
     */
    public static int curseDrain(int baseLevel, int currentLevel, int percent) {
        int amount = Math.max(1, baseLevel * percent / 100);
        if (baseLevel <= 0 || currentLevel <= baseLevel - amount) {
            return 0;
        }
        return Math.min(amount, currentLevel - (baseLevel - amount));
    }

    // ---------------------------------------------------------------- binding spells

    /** Bind / Snare / Entangle: base max hit and hold duration (game ticks), or null for other spells. */
    public static int[] binding(String spellId) {
        return switch (spellId) {
            case "bind" -> new int[]{0, 8}; // 4.8 s
            case "snare" -> new int[]{3, 16}; // 9.6 s
            case "entangle" -> new int[]{5, 24}; // 14.4 s
            default -> null;
        };
    }

    /** Binds last half as long on a player praying Protect from Magic. */
    public static int bindTicks(int ticks, boolean protectFromMagic) {
        return protectFromMagic ? ticks / 2 : ticks;
    }

    // ---------------------------------------------------------------- Tele Block

    /** Tele Block lasts 5 minutes, halved by Protect from Magic. */
    public static final int TELEBLOCK_TICKS = 500;
    /** After a tele block ends the player can't be tele blocked again for a minute. */
    public static final int TELEBLOCK_IMMUNITY_TICKS = 100; // approx

    public static int teleblockTicks(boolean protectFromMagic) {
        return protectFromMagic ? TELEBLOCK_TICKS / 2 : TELEBLOCK_TICKS;
    }

    /** Per-player Tele Block state. */
    public static final class Teleblock {
        private long until = Long.MIN_VALUE / 2;

        /** Applies a tele block; returns false if one is active or the player is still immune. */
        public boolean apply(long tick, boolean protectFromMagic) {
            if (tick < until + TELEBLOCK_IMMUNITY_TICKS) {
                return false;
            }
            until = tick + teleblockTicks(protectFromMagic);
            return true;
        }

        public boolean blocked(long tick) {
            return tick < until;
        }

        /** Game ticks of tele block left (0 when not blocked). */
        public long remaining(long tick) {
            return Math.max(0, until - tick);
        }

        public void clear() {
            until = Long.MIN_VALUE / 2;
        }
    }

    /** "m:ss" for a tick count, for tele block messages. */
    public static String minutes(long ticks) {
        long seconds = ticks * 6 / 10;
        return seconds / 60 + ":" + (seconds % 60 < 10 ? "0" : "") + seconds % 60;
    }
}
