package com.runecraft.osrs;

import java.util.Random;

/**
 * Player and NPC accuracy and max-hit formulas from Old School RuneScape (DPS calculator maths).
 * All multipliers are applied with the same flooring order as the game.
 */
public final class CombatFormulas {
    private CombatFormulas() {
    }

    // ----- Player melee -----

    public static int effectiveAttack(int attackLevel, double prayerMultiplier, CombatStyle style) {
        return (int) Math.floor(attackLevel * prayerMultiplier) + style.attackBonus() + 8;
    }

    public static int effectiveStrength(int strengthLevel, double prayerMultiplier, CombatStyle style) {
        return (int) Math.floor(strengthLevel * prayerMultiplier) + style.strengthBonus() + 8;
    }

    public static int effectiveDefence(int defenceLevel, double prayerMultiplier, CombatStyle style) {
        return (int) Math.floor(defenceLevel * prayerMultiplier) + style.defenceBonus() + 8;
    }

    /** Melee max hit: {@code floor((effStr * (strBonus + 64) + 320) / 640)}. */
    public static int meleeMaxHit(int effectiveStrength, int strengthBonus) {
        return (effectiveStrength * (strengthBonus + 64) + 320) / 640;
    }

    public static long attackRoll(int effectiveLevel, int equipmentBonus) {
        return (long) effectiveLevel * (equipmentBonus + 64);
    }

    // ----- Player ranged -----

    public static int effectiveRanged(int rangedLevel, double prayerMultiplier, CombatStyle style) {
        return (int) Math.floor(rangedLevel * prayerMultiplier) + style.rangedBonus() + 8;
    }

    /** Ranged max hit: {@code floor(0.5 + effRangedStr * (rangedStr + 64) / 640)}. */
    public static int rangedMaxHit(int effectiveRangedStrength, int rangedStrengthBonus) {
        return (int) Math.floor(0.5 + effectiveRangedStrength * (rangedStrengthBonus + 64) / 640.0);
    }

    // ----- Player magic -----

    public static int effectiveMagic(int magicLevel, double prayerMultiplier) {
        return (int) Math.floor(magicLevel * prayerMultiplier) + 8;
    }

    /** Spell max hit increased by magic damage % (e.g. occult necklace +10%). */
    public static int magicMaxHit(int spellBaseMaxHit, int magicDamagePercent) {
        return (int) Math.floor(spellBaseMaxHit * (1.0 + magicDamagePercent / 100.0));
    }

    /** Player magic defence uses 70% magic level and 30% defence level. */
    public static int effectiveMagicDefence(int magicLevel, double magicPrayer, int defenceLevel, double defencePrayer, CombatStyle style) {
        int magicPart = (int) Math.floor(Math.floor(magicLevel * magicPrayer) * 0.7);
        int defPart = (int) Math.floor(Math.floor(defenceLevel * defencePrayer) * 0.3);
        return magicPart + defPart + style.defenceBonus() + 8;
    }

    // ----- NPCs -----

    /** NPCs use {@code (level + 9) * (bonus + 64)} for both attack and defence rolls. */
    public static long npcRoll(int level, int bonus) {
        return (long) (level + 9) * (bonus + 64);
    }

    // ----- Rolling -----

    /** Rolls a hit: accuracy check, then uniform damage in [0, maxHit]. */
    public static int roll(Random random, long attackRoll, long defenceRoll, int maxHit) {
        double chance = Chance.hitChance(attackRoll, defenceRoll);
        if (random.nextDouble() >= chance) {
            return 0;
        }
        return random.nextInt(Math.max(0, maxHit) + 1);
    }

    // ----- Experience -----

    /** Melee XP per point of damage, in tenths: 4 XP to the trained skill, or 1.33 to each when controlled. */
    public static int meleeXpTenths(int damage) {
        return damage * 40;
    }

    public static int controlledXpTenths(int damage) {
        return (int) Math.round(damage * 13.3);
    }

    public static int hitpointsXpTenths(int damage) {
        return (int) Math.round(damage * 13.3);
    }
}
