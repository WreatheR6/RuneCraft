package com.runecraft.osrs;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * A monster's attack against a player, with the integer maths of the OSRS Wiki DPS calculator's
 * {@code NPCVsPlayerCalc} (weirdgloop/osrs-dps-calc): the NPC attack roll {@code (level + 9) * (bonus + 64)} with the
 * level and bonus of its style, the player's defence roll {@code (8 + effective level) * (bonus + 64)} where the
 * effective level is the boosted Defence after defence prayers (Torag's set scales it with missing hitpoints) and, against
 * magic, 70% of the boosted Magic level (after a magic prayer) plus 30% of that Defence, plus the stance bonus
 * (defensive and longrange +3, controlled +1). Protection prayers block all of a monster's damage of their style.
 * Snapshot-tested by {@code NpcHitCalcReferenceTest}; the Minecraft side fills an {@link Input} in
 * {@code combat.HitCalcBridge.npcInput}.
 */
public final class NpcHitCalc {
    private NpcHitCalc() {
    }

    /** One monster attack against one player. Player levels are current (boosted) levels. */
    public static final class Input {
        public AttackType style = AttackType.CRUSH;
        /** The monster's level for the style: Attack (melee), Ranged or Magic. */
        public int npcLevel = 1;
        /** The monster's attack bonus for the style (melee attack, ranged or magic attack bonus). */
        public int npcAttackBonus;
        /** The monster's level and strength bonus behind the max hit formula (Strength, Ranged or Magic). */
        public int npcStrengthLevel = 1;
        public int npcStrengthBonus;
        // ---- player
        public int defence = 1;
        public int magic = 1;
        public int hitpointsLevel = 10;
        public int hitpoints = 10;
        public final Set<Prayer> prayers = EnumSet.noneOf(Prayer.class);
        public CombatStyle stance = CombatStyle.ACCURATE;
        /** The player's total defence bonus against {@link #style}. */
        public int defenceBonus;
        /** Torag's helm, platebody, platelegs, hammers and the amulet of the damned. */
        public boolean torags;

        public Input pray(Collection<Prayer> list) {
            prayers.addAll(list);
            return this;
        }
    }

    public static long npcAttackRoll(Input in) {
        return CombatFormulas.npcRoll(in.npcLevel, in.npcAttackBonus);
    }

    /** The monster's max hit by the NPC formula {@code ((level + 9) * (bonus + 64) + 320) / 640}. */
    public static int formulaMaxHit(Input in) {
        return (int) (((long) (in.npcStrengthLevel + 9) * (in.npcStrengthBonus + 64) + 320) / 640);
    }

    /** Defence prayers add up: the best skin/Chivalry/Piety/Rigour/Augury prayer plus Deadeye's or Mystic Vigour's +5%. */
    static int defencePrayerPercent(Collection<Prayer> prayers) {
        int pct = 100;
        for (Prayer p : prayers) {
            pct += (int) Math.round((p.defenceMultiplier() - 1.0 + p.stackingDefenceBonus()) * 100);
        }
        return pct;
    }

    /** The player's effective defence level for the roll (stance bonus included, the +8 not). */
    public static int effectiveDefence(Input in) {
        int eff = in.defence * defencePrayerPercent(in.prayers) / 100;
        if (in.torags && in.hitpointsLevel > 0) {
            // The calculator rounds the missing fraction to two decimals before scaling.
            double missing = Math.round((double) (in.hitpointsLevel - in.hitpoints) / in.hitpointsLevel * 100) / 100.0 * 100;
            eff = (int) (eff * (1 + missing) / 100);
        }
        if (in.style == AttackType.MAGIC) {
            int magicPct = 100;
            for (Prayer p : in.prayers) {
                magicPct = Math.max(magicPct, (int) Math.round(p.magicMultiplier() * 100));
            }
            int effMagic = in.magic * magicPct / 100;
            eff = effMagic * 7 / 10 + eff * 3 / 10;
        }
        return eff + in.stance.defenceBonus();
    }

    public static long playerDefenceRoll(Input in) {
        return (long) (8 + effectiveDefence(in)) * (in.defenceBonus + 64);
    }

    public static double hitChance(Input in) {
        return Chance.hitChance(npcAttackRoll(in), playerDefenceRoll(in));
    }

    // ---------------------------------------------------------------- damage reduction on the defending player

    /** Chance for the Elysian spirit shield's effect to trigger on a hit. */
    public static final double ELYSIAN_CHANCE = 0.7;

    /** Elysian spirit shield (when it triggers): the hit loses a quarter, at least 1. */
    public static int elysian(int damage) {
        return Math.max(0, damage - Math.max(1, damage / 4));
    }

    /** Dinh's bulwark in its Block stance: hits are reduced by 20% (rounded down). */
    public static int dinhsBlock(int damage) {
        return (int) (damage * 0.8);
    }

    /**
     * The Justiciar set: a hit is reduced by (defence bonus against its style / 3000), rounded down. A negative bonus doesn't
     * add damage. // approx: the calculator would scale it up
     */
    public static int justiciar(int damage, int defenceBonus) {
        if (defenceBonus <= 0) {
            return damage;
        }
        return (int) (damage * (1 - defenceBonus / 3000.0));
    }

    /**
     * Every defensive reduction on one hit, in the calculator's order ({@code NPCVsPlayerCalc}): the Elysian spirit shield
     * (if it triggered), Dinh's bulwark on Block, then the Justiciar set with the defence bonus against the hit's style.
     */
    public static int reduceIncoming(int damage, boolean elysianTriggered, boolean dinhsBlock, boolean justiciar, int defenceBonus) {
        if (damage <= 0) {
            return Math.max(0, damage);
        }
        if (elysianTriggered) {
            damage = elysian(damage);
        }
        if (dinhsBlock) {
            damage = dinhsBlock(damage);
        }
        if (justiciar) {
            damage = justiciar(damage, defenceBonus);
        }
        return damage;
    }

    /** True for the whole Justiciar set (faceguard, chestguard and legguards). */
    public static boolean justiciarSet(String head, String body, String legs) {
        return "justiciar_faceguard".equals(head) && "justiciar_chestguard".equals(body) && "justiciar_legguards".equals(legs);
    }

    /** Dinh's bulwark (or the blazing one) held in its Block stance (the defensive style), which can't attack. */
    public static boolean dinhsBlocking(String weapon, CombatStyle style) {
        return ("dinhs_bulwark".equals(weapon) || "dinhs_blazing_bulwark".equals(weapon)) && style == CombatStyle.DEFENSIVE;
    }

    /** True when the player's overhead prayer protects from this style (monsters then deal no damage). */
    public static boolean protectedFrom(Collection<Prayer> prayers, AttackType style) {
        return switch (style) {
            case MAGIC -> prayers.contains(Prayer.PROTECT_FROM_MAGIC);
            case RANGED -> prayers.contains(Prayer.PROTECT_FROM_MISSILES);
            default -> prayers.contains(Prayer.PROTECT_FROM_MELEE);
        };
    }
}
