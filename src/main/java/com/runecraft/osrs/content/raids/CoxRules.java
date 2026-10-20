package com.runecraft.osrs.content.raids;

import com.runecraft.osrs.AttackType;

import java.util.List;
import java.util.Random;

/**
 * Chambers of Xeric room rules (pure maths, unit tested; round 33). Sources: the OSRS wiki pages "Chambers of
 * Xeric/Strategies", "Great Olm", "Vanguard" and "Vespula" (July 2026 dump) and the Mod Ash quotes they cite. Values the
 * wiki doesn't give are marked approx.
 */
public final class CoxRules {
    private CoxRules() {
    }

    private static int clampParty(int partySize) {
        return Math.max(1, Math.min(100, partySize));
    }

    // ------------------------------------------------------------------ Tekton

    /** Tekton returns to his anvil after 10-14 attacks (OSRS wiki). */
    public static int tektonAttacksBeforeAnvil(Random random) {
        return 10 + random.nextInt(5);
    }

    /** Sets of sparks Tekton sends at the anvil before he gets up enraged (OSRS: up to five). */
    public static final int TEKTON_SPARK_SETS = 5;
    /** Sparks per player in the chamber, each a 3x3 area (OSRS). */
    public static final int TEKTON_SPARKS_PER_PLAYER = 2;
    public static final int TEKTON_SPARK_MIN = 10;
    public static final int TEKTON_SPARK_MAX = 20;
    /** Game ticks between spark sets. // approx: "every few ticks" */
    public static final int TEKTON_SPARK_INTERVAL = 3;
    /** Tekton repairs 1.33% of his maximum health at the anvil (per 10,000), once per spark set. */
    public static final int TEKTON_REPAIR_PER_10K = 133;
    /** Attacks the enraged state lasts (OSRS: it wears off after about five attacks). */
    public static final int TEKTON_ENRAGED_ATTACKS = 5;
    /** Tekton attacks every 3 game ticks. */
    public static final int TEKTON_ATTACK_SPEED = 3;

    // ------------------------------------------------------------------ Vasa Nistirio

    /** Vasa throws a boulder at every player within 9 tiles while he walks (OSRS). */
    public static final int VASA_BOULDER_RANGE = 9;
    /** Game ticks between Vasa's heals off a crystal: each heals 1% of his hitpoints and (10% + 1) Defence (Mod Ash). */
    public static final int VASA_HEAL_INTERVAL = 2;

    /** Vasa's per-heal Defence restoration (Mod Ash: 10% + 1 of the base level). */
    public static int vasaDefenceHeal(int baseDefence) {
        return baseDefence / 10 + 1;
    }

    /**
     * Game ticks Vasa takes to drain a crystal; each crystal the team disables makes the next drain faster (OSRS: "to the
     * point that it is generally impossible to disable every third crystal"). // approx: 34 ticks, x0.7 per disabled crystal
     */
    public static int vasaDrainTicks(int disabled) {
        double t = 34;
        for (int i = 0; i < disabled; i++) {
            t *= 0.7;
        }
        return Math.max(8, (int) Math.round(t));
    }

    /** Game ticks Vasa walks from the centre to a crystal (and back). // approx */
    public static final int VASA_WALK_TICKS = 6;

    // ------------------------------------------------------------------ Vanguards

    /**
     * The health spread (fraction of maximum) beyond which the Vanguards shuffle and heal to full (OSRS wiki): 40% in
     * groups of four or fewer, 33.3% in groups of five or more.
     */
    public static double vanguardResetSpread(int partySize) {
        return clampParty(partySize) >= 5 ? 1.0 / 3.0 : 0.4;
    }

    /** The Vanguards shuffle every 20-36 game ticks on their own (Mod Ash). */
    public static int vanguardShuffleTicks(Random random) {
        return 20 + random.nextInt(17);
    }

    /** Game ticks the Vanguards spend in their shells while they change places. // approx */
    public static final int VANGUARD_SHELL_TICKS = 4;

    // ------------------------------------------------------------------ Vespula

    /** A lux grub's hitpoints, and what a medivaemia blossom heals (OSRS wiki). */
    public static final int LUX_GRUB_HP = 125;
    public static final int BLOSSOM_HEAL = 30;
    /** Enraged Vespula stings anyone in melee range for 5-20 typeless damage and stomps anyone under her for 2-8 (OSRS wiki). */
    public static final int VESPULA_ENRAGED_STING_MIN = 5;
    public static final int VESPULA_ENRAGED_STING_MAX = 20;
    public static final int VESPULA_STOMP_MIN = 2;
    public static final int VESPULA_STOMP_MAX = 8;
    /** Damage one of Vespula's stings does to a grub. // approx: five stings hatch a fresh grub */
    public static final int VESPULA_STING_DAMAGE = 25;
    /** Game ticks between Vespula's stings at the grubs. // approx */
    public static final int VESPULA_STING_INTERVAL = 8;
    /** A grounded vespine soldier explodes after 20 seconds (OSRS). */
    public static final int SOLDIER_FUSE_TICKS = 33;
    /** Game ticks after a soldier dies before its grub reappears. // approx: "soon after" */
    public static final int GRUB_RESPAWN_TICKS = 10;
    /** The abyssal portal hits players within 7 tiles, draining 3 prayer points (3 damage without prayer). */
    public static final int PORTAL_RANGE = 7;
    public static final int PORTAL_DRAIN = 3;
    /** Game ticks between the portal's pulses. // approx: its attack speed */
    public static final int PORTAL_INTERVAL = 4;

    // ------------------------------------------------------------------ Muttadiles

    /** A muttadile goes to feed below 50% health and restores up to 40% of its health at the tree (OSRS wiki). */
    public static final double MUTTADILE_FEED_BELOW = 0.5;
    public static final double MUTTADILE_FEED_HEAL = 0.4;
    /** Game ticks a muttadile feeds. // approx */
    public static final int MUTTADILE_FEED_TICKS = 8;

    /** The meat tree's health (OSRS wiki): 100 at Woodcutting 1 rising to 500 at 99 (team average). // approx: linear */
    public static int meatTreeHp(int averageWoodcutting) {
        int wc = Math.max(1, Math.min(99, averageWoodcutting));
        return 100 + (wc - 1) * 400 / 98;
    }

    /** A swing's chance to damage the meat tree (OSRS wiki): 20% at level 1 to 78% at 99. // approx: linear */
    public static double meatTreeAccuracy(int woodcutting) {
        int wc = Math.max(1, Math.min(99, woodcutting));
        return 0.20 + (wc - 1) * 0.58 / 98;
    }

    /** Damage of a successful chop (OSRS wiki): half the Woodcutting level, -7/+3, whatever the axe. */
    public static int meatTreeChop(int woodcutting, Random random) {
        return Math.max(1, Math.max(1, woodcutting) / 2 - 7 + random.nextInt(11));
    }

    /** Game ticks between the large muttadile's magic blasts from the water while the small one fights. // approx */
    public static final int MUTTADILE_WATER_BLAST_INTERVAL = 12;

    // ------------------------------------------------------------------ Ice demon

    /** Kindling to thaw the ice demon (OSRS wiki: 54 solo). // approx: +18 per extra player for the icefiends */
    public static int iceDemonKindling(int partySize) {
        return 54 + 18 * (clampParty(partySize) - 1);
    }

    /**
     * Kindling one successful chop of a sapling gives (OSRS wiki "Ice demon", Mod Ash): the max is the visible Woodcutting
     * level / 12 (boosts count), the roll is 0-max with equal chances and a 0 counts as 1 (1-8 at level 96).
     */
    public static int kindlingPerChop(int woodcutting, Random random) {
        int max = Math.max(0, woodcutting) / 12;
        return Math.max(1, random.nextInt(max + 1));
    }

    /** Woodcutting XP for a chop that gave {@code kindling} kindling (OSRS wiki "Ice demon", Mod Bruno's table). */
    private static final double[] KINDLING_XP = {30, 45, 55, 62.5, 68.5, 73.5, 77.7, 81.5};

    public static double kindlingXp(int kindling) {
        if (kindling <= 0) {
            return 0;
        }
        if (kindling <= KINDLING_XP.length) {
            return KINDLING_XP[kindling - 1];
        }
        return KINDLING_XP[KINDLING_XP.length - 1] + 3.5 * (kindling - KINDLING_XP.length); // approx: boosted past 107
    }

    /**
     * Chance one attempt lights an unlit brazier (OSRS wiki "Ice demon", Mod Ash): a normal Firemaking skill roll, 8% at
     * level 1 rising linearly to 78% at 99.
     */
    public static double brazierLightChance(int firemaking) {
        int fm = Math.max(1, Math.min(99, firemaking));
        return 0.08 + (fm - 1) * 0.70 / 98;
    }

    /**
     * Damage the thawed ice demon takes (OSRS wiki): 67% less unless demonbane (+15%) or a fire spell (+150%).
     */
    public static int iceDemonDamage(int damage, boolean demonbane, boolean fireSpell) {
        if (damage <= 0) {
            return 0;
        }
        if (fireSpell) {
            return damage * 250 / 100;
        }
        if (demonbane) {
            return damage * 115 / 100;
        }
        return damage * 33 / 100;
    }

    /** The secondaries the ice demon and the corrupted scavenger leave (OSRS: 7 stinkhorn, 7 endarkened juice, 5 cicely). */
    public static final int CLEAR_STINKHORN = 7;
    public static final int CLEAR_ENDARKENED = 7;
    public static final int CLEAR_CICELY = 5;

    // ------------------------------------------------------------------ Thieving

    /** Most grubs one chest gives (Mod Ash): 1, +1 each at Thieving 50, 75 and 100. */
    public static int grubMax(int thieving) {
        return 1 + (thieving >= 50 ? 1 : 0) + (thieving >= 75 ? 1 : 0) + (thieving >= 100 ? 1 : 0);
    }

    /** Grubs one opened chest gives: 1 up to {@link #grubMax}. */
    public static int grubs(int thieving, Random random) {
        return 1 + random.nextInt(grubMax(thieving));
    }

    /** The one-off chest of psykk bats: one per player in the raid, 5 to 25 (Mod Ash). */
    public static int batChest(int partySize) {
        return Math.max(5, Math.min(25, clampParty(partySize)));
    }

    /** Poisoned chests in the room (OSRS: three; 1-3 damage to anyone next to it, no poison). */
    public static final int POISON_CHESTS = 3;
    /** Game ticks before an opened chest locks itself again. // approx: "shortly after" */
    public static final int CHEST_RELOCK_TICKS = 25;
    /** Cavern grubs the corrupted scavenger eats before it falls asleep. // approx */
    public static int troughGrubs(int partySize) {
        return 12 + 6 * (clampParty(partySize) - 1);
    }

    // ------------------------------------------------------------------ Tightrope

    /** The tightrope's Agility requirement: 80-100% of the team's average Agility level, at random (Mod Ash). */
    public static int tightropeLevel(int averageAgility, Random random) {
        return Math.max(1, Math.max(1, averageAgility) * (80 + random.nextInt(21)) / 100);
    }

    // ------------------------------------------------------------------ Crystal crabs

    /** How long a hammer stuns a crystal crab, in game ticks (Mod Ash): solo 50-60, 2-3: 30-40, 4-5: 20-30, 6+: 10-20. */
    public static int crabStunTicks(int partySize, Random random) {
        int n = clampParty(partySize);
        int base = n == 1 ? 50 : n <= 3 ? 30 : n <= 5 ? 20 : 10;
        return base + random.nextInt(11);
    }

    /** A crab keeps the colour of the last style that hit it for about five seconds. */
    public static final int CRAB_COLOUR_TICKS = 8;
    /** Every layout needs three crabs; bigger parties get more. // approx: 4 + 1 per 2 players, up to 8 */
    public static int crabCount(int partySize) {
        return Math.min(8, 4 + clampParty(partySize) / 2);
    }

    /** The colour a crab (and the orb it reflects) takes from an attack style (OSRS: red melee, blue magic, green ranged). */
    public static Colour crabColour(AttackType type) {
        if (type == null) {
            return Colour.WHITE;
        }
        return switch (type) {
            case MAGIC -> Colour.BLUE;
            case RANGED -> Colour.GREEN;
            default -> Colour.RED;
        };
    }

    /** Orb and crab colours. The black crystal wants the white orb; yellow wants blue, cyan red and magenta green. */
    public enum Colour {
        WHITE, RED, BLUE, GREEN
    }

    /** The orb an orb-and-crab collision leaves (null: the orb is gone): a coloured orb only passes a crab of its colour. */
    public static Colour reflect(Colour orb, Colour crab) {
        if (crab == Colour.WHITE) {
            return orb;
        }
        if (orb == Colour.WHITE || orb == crab) {
            return crab;
        }
        return null;
    }

    /** Damage the orb does to a player who steps into it (OSRS: scaled to current health). // approx: 30% */
    public static int orbDamage(int currentHp) {
        return Math.max(1, currentHp * 30 / 100);
    }

    // ------------------------------------------------------------------ the Great Olm

    /** The head's standard attack max hit (OSRS DPS calculator): 27, 28 in the penultimate phase, 29 in the last. */
    public static int olmMaxHit(boolean penultimate, boolean headPhase) {
        return headPhase ? 29 : penultimate ? 28 : 27;
    }

    /** One of the three parts of the Olm. */
    public enum OlmPart {
        HEAD, LEFT, RIGHT
    }

    /**
     * Olm damage mitigation (OSRS wiki): the head takes 66% less from everything but ranged, the right (mage) hand from
     * everything but magic and the left (melee) hand from everything but melee.
     */
    public static int olmMitigated(OlmPart part, AttackType type, int damage) {
        if (damage <= 0 || type == null) {
            return damage;
        }
        boolean melee = type == AttackType.STAB || type == AttackType.SLASH || type == AttackType.CRUSH;
        boolean full = switch (part) {
            case HEAD -> type == AttackType.RANGED;
            case RIGHT -> type == AttackType.MAGIC;
            case LEFT -> melee;
        };
        return full ? damage : damage * 34 / 100;
    }

    /** One step of the Olm's attack rotation (every 4 game ticks). */
    public enum OlmStep {
        STANDARD, EMPTY, CRYSTAL_BURST, LIGHTNING, TELEPORT
    }

    /** The Olm's attack rotation (OSRS wiki), looped. */
    public static final List<OlmStep> OLM_ROTATION = List.of(OlmStep.STANDARD, OlmStep.EMPTY, OlmStep.STANDARD, OlmStep.CRYSTAL_BURST,
            OlmStep.STANDARD, OlmStep.EMPTY, OlmStep.STANDARD, OlmStep.LIGHTNING, OlmStep.STANDARD, OlmStep.EMPTY, OlmStep.STANDARD,
            OlmStep.TELEPORT);
    /** The Olm acts every 4 game ticks. */
    public static final int OLM_STEP_TICKS = 4;
    /** Chance a standard attack is replaced by a phase power's attack (Mod Ash): 1/3, 1/2 in the final phase. */
    public static double olmPowerChance(boolean finalPhase) {
        return finalPhase ? 0.5 : 1.0 / 3;
    }

    /** The head switches between magic and ranged with a 1/5 chance after each attack (Mod Ash). */
    public static final double OLM_STYLE_SWITCH = 0.2;
    /** Chance a standard attack is a prayer sphere instead. // approx */
    public static final double OLM_SPHERE_CHANCE = 0.2;
    /** An unblocked sphere takes about half the player's current hitpoints (OSRS wiki). */
    public static int olmSphereDamage(int currentHp) {
        return Math.max(1, currentHp / 2);
    }

    /** The Olm's powers: each power phase uses one, never the previous phase's. */
    public enum OlmPower {
        ACID, FLAME, CRYSTAL
    }

    /** Game ticks the left hand stays clenched, unless the right hand goes down first. // approx */
    public static final int OLM_CLENCH_TICKS = 16;
    /** Penultimate phase: a disabled hand recovers at full health if the other lives for roughly 30 seconds (OSRS wiki). */
    public static final int OLM_HAND_RECOVER_TICKS = 50;
    /** Falling crystals before the Olm re-emerges on the other side (OSRS: 7-8 targeting crystals). */
    public static int olmTransitionCrystals(Random random) {
        return 7 + random.nextInt(2);
    }

    /** Game ticks between falling crystals during a phase change and in the final stand. // approx */
    public static final int OLM_FALLING_INTERVAL = 2;
    /** A falling crystal hits 3x3: 16-25 directly under it, 10-16 next to it (OSRS wiki). */
    public static final int OLM_CRYSTAL_DIRECT_MIN = 16;
    public static final int OLM_CRYSTAL_DIRECT_MAX = 25;
    public static final int OLM_CRYSTAL_NEAR_MIN = 10;
    public static final int OLM_CRYSTAL_NEAR_MAX = 16;
    /** Ticks after a head hit before an unfinished phase heals it back (OSRS: "after several ticks"). // approx */
    public static final int OLM_HEAD_AUTOHEAL_TICKS = 6;

    /** Crystal bombs (OSRS: up to three, two for smaller groups). // approx: three from five players */
    public static int olmCrystalBombs(int partySize) {
        return clampParty(partySize) >= 5 ? 3 : 2;
    }

    /** A crystal bomb hits a radius of 4 tiles: 15, plus 15 for every tile closer (OSRS wiki). */
    public static int olmCrystalBombDamage(double distanceTiles) {
        if (distanceTiles > 4.5) {
            return 0;
        }
        int tiles = (int) Math.round(Math.max(0, distanceTiles));
        return 15 + 15 * (4 - Math.min(4, tiles));
    }

    /** Game ticks before a crystal bomb explodes (OSRS: "several seconds"). // approx */
    public static final int OLM_BOMB_FUSE = 8;
    /** Falling Crystals power: 11 crystals at the targeted player, 15-20 each (OSRS wiki). */
    public static final int OLM_FALLING_POWER_CRYSTALS = 11;
    public static final int OLM_FALLING_POWER_MIN = 15;
    public static final int OLM_FALLING_POWER_MAX = 20;
    /** Acid pools: 3-6 a tick and poison starting at 2 (OSRS wiki). */
    public static final int OLM_ACID_MIN = 3;
    public static final int OLM_ACID_MAX = 6;
    public static final int OLM_ACID_POISON = 2;
    /** Game ticks the acid pools and an acid drip last. // approx */
    public static final int OLM_ACID_TICKS = 20;
    /** Deep Burn: 5 damage and -2 to stats, six times, every few ticks (OSRS wiki). // approx: every 4 ticks */
    public static final int OLM_BURN_DAMAGE = 5;
    public static final int OLM_BURN_DRAIN = 2;
    public static final int OLM_BURN_HITS = 6;
    public static final int OLM_BURN_INTERVAL = 4;
    /** Fire Wall: 8-10 to leap out of a wall tile, then 50-65 to anyone still trapped after 5 seconds (OSRS wiki). */
    public static final int OLM_WALL_LEAP_MIN = 8;
    public static final int OLM_WALL_LEAP_MAX = 10;
    public static final int OLM_WALL_MIN = 50;
    public static final int OLM_WALL_MAX = 65;
    public static final int OLM_WALL_TICKS = 8;
    /** Final stand Life Siphon: stand on a pool within 6 seconds or lose up to 18; the Olm heals 5x (OSRS wiki). */
    public static final int OLM_SIPHON_TICKS = 10;
    public static final int OLM_SIPHON_MAX = 18;
    public static final int OLM_SIPHON_HEAL = 5;
    /** Teleport: the further a swapped player lands from where they stood, the more damage. // approx: 2 per tile, up to 30 */
    public static int olmTeleportDamage(double distanceTiles) {
        return (int) Math.min(30, Math.round(distanceTiles * 2));
    }
}
