package com.runecraft.osrs;

import java.util.Set;

/**
 * Gear, prayer and rule modifiers to the core OSRS combat formulas (DPS calculator maths from the wiki):
 * salve amulets, Dharok's set, obsidian + berserker necklace, crystal armour, the twisted bow, powered
 * staves (tridents, sanguinesti staff, Tumeken's shadow), protection prayers in PvP, Wilderness level
 * ranges and monster aggression / tolerance.
 */
public final class CombatBonuses {
    private CombatBonuses() {
    }

    // ---------------------------------------------------------------- salve amulet

    /**
     * Salve amulet multiplier against undead (applied to both the attack roll and the max hit). Salve: melee
     * 7/6; salve(e): melee 1.2; salve(i): melee and ranged 7/6, magic 1.15; salve(ei): 1.2 for every style.
     * It does not stack with a black mask / slayer helmet: the salve wins.
     */
    public static double salve(String neckId, AttackType type) {
        if (neckId == null) {
            return 1.0;
        }
        boolean melee = type.isMelee();
        return switch (neckId) {
            case "salve_amulet" -> melee ? 7.0 / 6.0 : 1.0;
            case "salve_amulet_e" -> melee ? 1.2 : 1.0;
            case "salve_amulet_i" -> type == AttackType.MAGIC ? 1.15 : 7.0 / 6.0;
            case "salve_amulet_ei" -> 1.2;
            default -> 1.0;
        };
    }

    // ---------------------------------------------------------------- Dharok's

    public static final Set<String> DHAROKS = Set.of("dharoks_helm", "dharoks_platebody", "dharoks_platelegs", "dharoks_greataxe");

    /** Dharok's set effect: max hit {@code * (1 + lost/100 * max/100)}. */
    public static double dharok(int maxHp, int currentHp) {
        int lost = Math.max(0, maxHp - currentHp);
        return 1.0 + (lost / 100.0) * (maxHp / 100.0);
    }

    /** Dharok's max hit, floored the way the game does: {@code base + floor(base * lost * max / 10000)}. */
    public static int dharokMaxHit(int base, int maxHp, int currentHp) {
        int lost = Math.max(0, maxHp - currentHp);
        return base + base * lost * maxHp / 10000;
    }

    // ---------------------------------------------------------------- obsidian

    /** Obsidian melee weapons that benefit from the obsidian armour set and the berserker necklace. */
    public static final Set<String> TZHAAR_MELEE = Set.of("tzhaar_ket_om", "tzhaar_ket_em", "toktz_xil_ak", "toktz_xil_ek",
            "tzhaar_ket_om_t");

    /** Damage bonus (fraction) of obsidian armour with an obsidian weapon. */
    public static final double OBSIDIAN_SET = 0.10;
    /** Damage bonus (fraction) of a berserker necklace with an obsidian weapon; additive with the armour set. */
    public static final double BERSERKER_NECKLACE = 0.20;

    /**
     * Max-hit multiplier for the berserker necklace on top of an already-applied obsidian armour multiplier
     * (the two bonuses add: armour + necklace = +30%, not 1.1 * 1.2).
     */
    public static double berserkerOnTop(boolean obsidianArmour) {
        return obsidianArmour ? (1.0 + OBSIDIAN_SET + BERSERKER_NECKLACE) / (1.0 + OBSIDIAN_SET) : 1.0 + BERSERKER_NECKLACE;
    }

    // ---------------------------------------------------------------- crystal armour

    public static boolean isCrystalBow(String weaponId) {
        return weaponId != null && (weaponId.startsWith("bow_of_faerdhinen") || weaponId.startsWith("crystal_bow"));
    }

    /** Crystal armour with a crystal bow: helm +5% / body +15% / legs +10% accuracy (30% for the set). */
    public static double crystalAccuracy(boolean helm, boolean body, boolean legs) {
        return 1.0 + (helm ? 0.05 : 0) + (body ? 0.15 : 0) + (legs ? 0.10 : 0);
    }

    /** Crystal armour with a crystal bow: helm +2.5% / body +7.5% / legs +5% damage (15% for the set). */
    public static double crystalDamage(boolean helm, boolean body, boolean legs) {
        return 1.0 + (helm ? 0.025 : 0) + (body ? 0.075 : 0) + (legs ? 0.05 : 0);
    }

    // ---------------------------------------------------------------- twisted bow

    /** The Magic value the twisted bow scales with: max(magic level, magic attack bonus), capped at 250 (350 in CoX). */
    public static int twistedBowMagic(int magicLevel, int magicAttackBonus, boolean chambers) {
        return Math.max(0, Math.min(chambers ? 350 : 250, Math.max(magicLevel, magicAttackBonus)));
    }

    /** Twisted bow accuracy in percent: {@code 140 + (3M - 10)/100 - (3M/10 - 100)^2/100}, capped at 140. */
    public static int twistedBowAccuracy(int magic) {
        int t = 3 * magic / 10;
        int acc = 140 + Math.floorDiv(10 * t - 10, 100) - (t - 100) * (t - 100) / 100;
        return Math.max(0, Math.min(140, acc));
    }

    /** Twisted bow damage in percent: {@code 250 + (3M - 14)/100 - (3M/10 - 140)^2/100}, capped at 250. */
    public static int twistedBowDamage(int magic) {
        int t = 3 * magic / 10;
        int dmg = 250 + Math.floorDiv(10 * t - 14, 100) - (t - 140) * (t - 140) / 100;
        return Math.max(0, Math.min(250, dmg));
    }

    // ---------------------------------------------------------------- powered staves

    /** Built-in spell base max hit of a powered staff at a (boosted) Magic level, or -1 if not a powered staff. */
    public static int poweredStaffMaxHit(String weaponId, int magicLevel) {
        if (weaponId == null) {
            return -1;
        }
        int third = magicLevel / 3;
        return switch (weaponId) {
            case "trident_of_the_seas", "trident_of_the_seas_e" -> Math.max(1, third - 5);
            case "trident_of_the_swamp", "trident_of_the_swamp_e" -> Math.max(1, third - 2);
            case "sanguinesti_staff", "holy_sanguinesti_staff" -> Math.max(1, third - 1);
            case "tumekens_shadow" -> Math.max(1, third + 1);
            case "thammarons_sceptre" -> Math.max(1, third - 8);
            case "accursed_sceptre" -> Math.max(1, third - 6);
            default -> -1;
        };
    }

    /** Tumeken's shadow multiplies equipment magic attack and magic damage by 3 (4 inside the Tombs of Amascut). */
    public static int shadowMultiplier(boolean tombs) {
        return tombs ? 4 : 3;
    }

    /** Tumeken's shadow magic damage percent: tripled (quadrupled in ToA), capped at 100%. */
    public static int shadowMagicDamage(int magicDamagePercent, boolean tombs) {
        return Math.min(100, magicDamagePercent * shadowMultiplier(tombs));
    }

    // ---------------------------------------------------------------- PvP / Wilderness

    /** Overhead protection prayers block 100% of NPC damage but only 40% of damage from other players. */
    public static int protectedFromPlayer(int damage) {
        return (int) Math.floor(damage * 0.6);
    }

    /**
     * Wilderness level range: both players must be in the Wilderness and their combat levels may differ by
     * at most the Wilderness level (each player's own level applies, so the lower of the two is used).
     */
    public static boolean canAttackInWilderness(int attackerCombat, int targetCombat, int attackerWild, int targetWild) {
        if (attackerWild <= 0 || targetWild <= 0) {
            return false;
        }
        return Math.abs(attackerCombat - targetCombat) <= Math.min(attackerWild, targetWild);
    }

    // ---------------------------------------------------------------- aggression

    /** Level-based aggression: a monster attacks players whose combat level is at most double its own. */
    public static boolean aggressiveTo(int monsterLevel, int playerCombat) {
        return playerCombat <= monsterLevel * 2;
    }

    /** Monsters stop being aggressive after a player has spent 10 minutes (1000 game ticks) in the same area. */
    public static final int TOLERANCE_TICKS = 1000;
    /** The tolerance region: a 21x21 tile area around where the player settled (10 tiles each way). */
    public static final int TOLERANCE_RADIUS = 10;

    /** Per-player tolerance timer. Leaving the 21x21 area around the anchor tile restarts the 10 minutes. */
    public static final class Tolerance {
        private boolean set;
        private int anchorX;
        private int anchorZ;
        private long since;

        /** Updates with the player's current tile; returns true once the player is tolerated here. */
        public boolean update(int tileX, int tileZ, long gameTick) {
            if (!set || Math.abs(tileX - anchorX) > TOLERANCE_RADIUS || Math.abs(tileZ - anchorZ) > TOLERANCE_RADIUS) {
                set = true;
                anchorX = tileX;
                anchorZ = tileZ;
                since = gameTick;
            }
            return tolerant(gameTick);
        }

        public boolean tolerant(long gameTick) {
            return set && gameTick - since >= TOLERANCE_TICKS;
        }

        public void reset() {
            set = false;
        }
    }

    // ---------------------------------------------------------------- experience

    /** Combat spell XP in tenths: the spell's base XP plus 2 XP per damage dealt. */
    public static int magicXpTenths(int baseXpTenths, int damage) {
        return baseXpTenths + Math.max(0, damage) * 20;
    }

    /** Powered staves give no base XP: 2 Magic XP per damage on accurate (1.33 + 1 Defence on longrange). */
    public static int poweredStaffXpTenths(int damage) {
        return Math.max(0, damage) * 20;
    }
}
