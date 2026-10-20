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

    /**
     * The standard OSRS skilling roll ("Skilling success chart"): {@code (1 + floor(low x (99 - L) / 98 + high x (L - 1) / 98
     * + 0.5)) / 256}, the level clamped to 1-99.
     */
    public static double skillRoll(int level, int low, int high) {
        int l = Math.max(1, Math.min(99, level));
        return (1 + Math.floor(low * (99 - l) / 98.0 + high * (l - 1) / 98.0 + 0.5)) / 256.0;
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
    /** Tekton's max hit (OSRS wiki infobox: 52, 59 enraged). */
    public static final int TEKTON_MAX_HIT = 52;
    public static final int TEKTON_ENRAGED_MAX_HIT = 59;
    /**
     * Tekton's stab/slash/crush defence bonuses (OSRS wiki infobox and DPS calculator: 155/165/105 normally, 280/290/180
     * enraged).
     */
    public static final int[] TEKTON_DEFENCE = {155, 165, 105};
    public static final int[] TEKTON_ENRAGED_DEFENCE = {280, 290, 180};
    /** Rapid stomp damage to a player left under Tekton as he walks back to the anvil. // approx: 0-8 a tick, like Vasa's */
    public static final int TEKTON_STOMP_MAX = 8;

    /**
     * The 1-in-N chance of Tekton's onyx (OSRS wiki "Tekton": 1/450 solo, better for a tougher Tekton in a bigger party,
     * and three times worse in Challenge Mode for parties of three or fewer, Mod Ash 2021/2026).
     * // approx: 450 solo falling by 38 per extra player to 70
     */
    public static int tektonOnyxRate(int partySize, boolean challenge) {
        int n = clampParty(partySize);
        int rate = Math.max(70, 450 - 38 * (n - 1));
        return challenge && n <= 3 ? rate * 3 : rate;
    }
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
     * Vasa's teleport timer (OSRS wiki "Vasa Nistirio"): when he first reaches a glowing crystal a 40-second (66- or
     * 67-tick) timer starts; it only runs while a crystal is active and pauses while none is. When it runs out he takes the
     * crystal's healing, leaves it and goes back to the centre to teleport the team, and the timer starts afresh. Every
     * crystal the team disables in time eats into the same timer, which is why the third one in a row is out of reach.
     */
    public static int vasaTeleportTimer(Random random) {
        return 66 + random.nextInt(2);
    }

    /** Rapid stomp damage to anyone standing under Vasa (OSRS wiki: up to 8 a tick). */
    public static final int VASA_STOMP_MAX = 8;

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
    /** Players in the shuffling Vanguards' path take 3-6 rapid stomp damage every few ticks (OSRS wiki). */
    public static final int VANGUARD_STOMP_MIN = 3;
    public static final int VANGUARD_STOMP_MAX = 6;
    /** Every Vanguard attack is three hits (OSRS wiki); the melee one lands all three on its target. */
    public static final int VANGUARD_HITS_PER_ATTACK = 3;

    // ------------------------------------------------------------------ Vespula

    /**
     * A lux grub's hitpoints, and what a medivaemia blossom heals (OSRS wiki: 125, and 35 since the 29 November 2023 update,
     * up from 20).
     */
    public static final int LUX_GRUB_HP = 125;
    public static final int BLOSSOM_HEAL = 35;
    /** Enraged Vespula stings anyone in melee range for 5-20 typeless damage and stomps anyone under her for 2-8 (OSRS wiki). */
    public static final int VESPULA_ENRAGED_STING_MIN = 5;
    public static final int VESPULA_ENRAGED_STING_MAX = 20;
    public static final int VESPULA_STOMP_MIN = 2;
    public static final int VESPULA_STOMP_MAX = 8;
    /**
     * Lux grubs (OSRS wiki): Vespula periodically stings a grub, injecting her eggs; its meter then slowly depletes until a
     * vespine soldier bursts out, unless blossoms keep it topped up. // approx: a stung grub loses 1 a tick (75 seconds
     * from full), and she stings the next healthy grub every 20 ticks while she flies
     */
    public static final int GRUB_DRAIN_PER_TICK = 1;
    public static final int VESPULA_STING_INTERVAL = 20;
    /** Vespula's sting poisons from 20, a vespine soldier's from 15 (OSRS wiki). */
    public static final int VESPULA_POISON = 20;
    public static final int SOLDIER_POISON = 15;
    /** A vespine soldier drops to the ground once its health is low (then only melee harms it). // approx: half health */
    public static final double SOLDIER_LAND_BELOW = 0.5;
    /** A grounded vespine soldier explodes after 20 seconds (OSRS). */
    public static final int SOLDIER_FUSE_TICKS = 33;
    /** A grounded soldier's explosion. // approx: "moderate damage" */
    public static final int SOLDIER_BLAST_MIN = 10;
    public static final int SOLDIER_BLAST_MAX = 25;
    /** Game ticks after a soldier dies before its grub reappears. // approx: "soon after" */
    public static final int GRUB_RESPAWN_TICKS = 10;
    /** The abyssal portal hits players within 7 tiles, draining 3 prayer points (3 damage without prayer). */
    public static final int PORTAL_RANGE = 7;
    public static final int PORTAL_DRAIN = 3;
    /**
     * The portal drains 3 prayer points every two ticks (its attack speed) from players in range while it is under attack
     * (OSRS wiki "Abyssal portal").
     */
    public static final int PORTAL_INTERVAL = 2;
    /** Ticks after the last attack on the portal that it still counts as under attack. // approx */
    public static final int PORTAL_UNDER_ATTACK_TICKS = 8;

    // ------------------------------------------------------------------ Muttadiles

    /**
     * A muttadile goes to feed at about half health and eats up to another 50% of its health (back to full) at the tree,
     * at most three times; one that can't reach the tree for about 15 seconds gives up (OSRS wiki "Muttadile").
     */
    public static final double MUTTADILE_FEED_BELOW = 0.5;
    public static final double MUTTADILE_FEED_HEAL = 0.5;
    public static final int MUTTADILE_MAX_FEEDS = 3;
    public static final int MUTTADILE_FEED_GIVE_UP_TICKS = 25;
    /** Game ticks a muttadile feeds. // approx */
    public static final int MUTTADILE_FEED_TICKS = 8;

    /** The meat tree's health (Mod Ash, OSRS wiki "Meat tree"): 5 x the team's total Woodcutting levels, at least 100. */
    public static int meatTreeHp(int totalWoodcutting) {
        return Math.max(100, 5 * Math.max(0, totalWoodcutting));
    }

    /**
     * A swing's chance to damage the meat tree (OSRS wiki "Meat tree": the skilling roll with low 50, high 200, i.e. 20% at
     * level 1 and 78% at 99).
     */
    public static double meatTreeAccuracy(int woodcutting) {
        return skillRoll(woodcutting, 50, 200);
    }

    /** Woodcutting XP for a successful swing at the meat tree (OSRS wiki: 45). */
    public static final double MEAT_TREE_XP = 45;

    /** Damage of a successful chop (OSRS wiki): half the Woodcutting level, -7/+3, whatever the axe. */
    public static int meatTreeChop(int woodcutting, Random random) {
        return Math.max(1, Math.max(1, woodcutting) / 2 - 7 + random.nextInt(11));
    }

    /**
     * While the small muttadile fights, its submerged mother attacks with magic (attack speed 4) and picks each player it
     * can see with a 1/3 chance (Mod Ash); its magic max hit is 23 (OSRS wiki).
     */
    public static final int MUTTADILE_WATER_BLAST_INTERVAL = 4;
    public static final double MUTTADILE_WATER_BLAST_CHANCE = 1.0 / 3;
    public static final int MUTTADILE_MAGIC_MAX = 23;

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

    /** Kindling stacks to 28; a player carrying 28 can't chop more (OSRS wiki "Kindling (Chambers of Xeric)"). */
    public static final int KINDLING_CAP = 28;
    /** Firemaking XP per kindling burnt (OSRS wiki: 48). */
    public static final double KINDLING_FIREMAKING_XP = 48;
    /** Points (OSRS wiki "Brazier (Chambers of Xeric)"): 36 for every kindling lit in a brazier, 32 for lighting one. */
    public static final int KINDLING_POINTS = 36;
    public static final int BRAZIER_LIGHT_POINTS = 32;

    /**
     * A sapling falls when chopped with a 1/X chance (Mod Ash): X is 3 or twice the party size at the start, whichever is
     * higher, and five times that in the ice demon room.
     */
    public static int saplingDepletion(int partySize, boolean iceDemonRoom) {
        int x = Math.max(3, 2 * clampParty(partySize));
        return iceDemonRoom ? x * 5 : x;
    }

    /** Game ticks a felled sapling's stump takes to grow back (OSRS wiki: "after a few seconds"). // approx */
    public static final int SAPLING_RESPAWN_TICKS = 8;

    /** A successful chop at a sapling. // approx: 40% plus 1% per 1.5 Woodcutting levels */
    public static double saplingChopChance(int woodcutting) {
        return Math.min(1, 0.4 + Math.max(1, woodcutting) / 150.0);
    }

    /** Icefiends at the braziers: one per player, at most four (OSRS wiki "Ice demon"). */
    public static int icefiends(int partySize) {
        return Math.min(4, clampParty(partySize));
    }

    /**
     * An icefiend tries to take a charge (a burning kindling) off its lit brazier every 3 or 4 ticks at random, succeeding
     * 1 time in 6 (Mod Ash, July 2026).
     */
    public static int icefiendDelay(Random random) {
        return 3 + random.nextInt(2);
    }

    public static final int ICEFIEND_SUCCESS = 6;
    /**
     * Game ticks a brazier takes to burn one kindling (the braziers burn on a timer set by their fuel, and the burning
     * kindling thaws the demon). // approx
     */
    public static final int BRAZIER_BURN_TICKS = 2;

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

    /** The secondaries the ice demon leaves (OSRS wiki: 7 stinkhorn, 7 endarkened juice, 5 cicely). */
    public static final int CLEAR_STINKHORN = 7;
    public static final int CLEAR_ENDARKENED = 7;
    public static final int CLEAR_CICELY = 5;
    /** The sleeping corrupted scavenger's batch (OSRS wiki "Chambers of Xeric/Strategies": 5 stinkhorn, 5 juice, 4 cicely). */
    public static final int THIEVING_STINKHORN = 5;
    public static final int THIEVING_ENDARKENED = 5;
    public static final int THIEVING_CICELY = 4;

    // ------------------------------------------------------------------ Lizardman shamans and Skeletal Mystics

    /** A shaman's jump lands on a 3x3 area for 20-25 typeless damage (OSRS wiki "Lizardman shaman"). */
    public static final int SHAMAN_JUMP_MIN = 20;
    public static final int SHAMAN_JUMP_MAX = 25;
    /**
     * The purple poison blob lands on the target's tile (3x3): 25-30 on the surface and about 25% stronger in the
     * Chambers, up to 39, poisoning from 12 (OSRS wiki). // approx: 31-39
     */
    public static final int SHAMAN_ACID_MIN = 31;
    public static final int SHAMAN_ACID_MAX = 39;
    public static final int SHAMAN_POISON = 12;
    /** Three spawns appear per summon and explode for 5-10 within two tiles (a 5x5 area) seconds later (OSRS wiki). */
    public static final int SHAMAN_SPAWNS = 3;
    public static final int SPAWN_BLAST_MIN = 5;
    public static final int SPAWN_BLAST_MAX = 10;
    /** Game ticks before a spawn self-destructs. // approx: "seconds later" */
    public static final int SPAWN_FUSE_TICKS = 6;
    /** Game ticks between a shaman's special moves (jump, blob, spawns). // approx: no cooldown except the spawns' */
    public static final int SHAMAN_SPECIAL_MIN = 8;
    public static final int SHAMAN_SPECIAL_SPREAD = 8;
    /** Game ticks before one shaman may summon spawns again (OSRS: only spawns have a cooldown). // approx */
    public static final int SHAMAN_SPAWN_COOLDOWN = 25;

    /**
     * A Skeletal Mystic melees a target praying Protect from Magic that it can reach; otherwise it casts (OSRS wiki).
     * Its magic is a standard bolt or a damaging Vulnerability.
     */
    public static boolean mysticMelees(boolean targetPraysMagic, boolean inReach) {
        return targetPraysMagic && inReach;
    }

    /** Teleport portals appear when the last mystic dies: at the start, the middle and the end of the room (OSRS wiki). */
    public static final int MYSTIC_PORTALS = 3;

    /** Guardians knock back anyone who tries to slip past them, for moderate damage (OSRS wiki). // approx: 10-15 */
    public static final int GUARDIAN_PUSH_MIN = 10;
    public static final int GUARDIAN_PUSH_MAX = 15;

    // ------------------------------------------------------------------ Thieving

    /**
     * Grubs one opened chest gives (Mod Ash, OSRS wiki "Chest (Chambers of Xeric)"): a random 0 to Thieving / 25 inclusive,
     * raised to at least 1 (2 from level 95).
     */
    public static int grubs(int thieving, Random random) {
        int max = Math.max(0, thieving) / 25;
        int min = thieving >= 95 ? 2 : 1;
        return Math.max(min, random.nextInt(max + 1));
    }

    /** Most grubs a chest can give. */
    public static int grubMax(int thieving) {
        return Math.max(thieving >= 95 ? 2 : 1, Math.max(0, thieving) / 25);
    }

    /**
     * Chance one attempt (one per tick) opens a chest: the skilling roll 99/155 bare-handed, 39% at level 1 to 61% at 99,
     * and 153/209 with a lockpick or hair clip, 21 points better (Mod Ash, January 2024).
     */
    public static double chestOpenChance(int thieving, boolean lockpick) {
        return lockpick ? skillRoll(thieving, 153, 209) : skillRoll(thieving, 99, 155);
    }

    /** Points per cavern grub fed to the trough (OSRS wiki: 115). */
    public static final int GRUB_POINTS = 115;

    /** Grubs that still earn points: max(4,500, floor(total Thieving / 6) x 150) / 115, rounded up (OSRS wiki). */
    public static int pointGrubs(int totalThievingLevels) {
        int max = Math.max(4_500, Math.max(0, totalThievingLevels) / 6 * 150);
        return (max + GRUB_POINTS - 1) / GRUB_POINTS;
    }

    /** The scavenger starts to get hungry again after 60 seconds without food (OSRS wiki "Corrupted scavenger"). */
    public static final int SCAVENGER_HUNGER_TICKS = 100;
    /** Game ticks the scavenger takes to eat a grub from the trough, and to lose one grub of fullness when hungry. // approx */
    public static final int SCAVENGER_EAT_TICKS = 1;
    public static final int SCAVENGER_HUNGER_RATE = 10;

    /** The one-off chest of psykk bats: one per player in the raid, 5 to 25 (Mod Ash). */
    public static int batChest(int partySize) {
        return Math.max(5, Math.min(25, clampParty(partySize)));
    }

    /** Poisoned chests in the room (OSRS: three; 1-3 damage to anyone next to it, no poison). */
    public static final int POISON_CHESTS = 3;
    /** Game ticks before an opened chest locks itself again. // approx: "shortly after" */
    public static final int CHEST_RELOCK_TICKS = 25;
    /**
     * Cavern grubs the corrupted scavenger eats before it falls asleep (OSRS wiki "Trough (Chambers of Xeric)": it scales
     * with the party's total Thieving, at least 16 per player or 30 solo). // approx: the minimum
     */
    public static int troughGrubs(int partySize) {
        int n = clampParty(partySize);
        return n == 1 ? 30 : 16 * n;
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

    /** How far from the room centre the crab room's walls (and so its crystals and sculpture) stand, in tiles. */
    public static final int CRAB_ROOM_REACH = 19;
    /** Rows the three coloured crystals can stand in along the side wall (relative to the centre, along the orb's lane). */
    static final int[] CRAB_CRYSTAL_ROWS = {-13, -9, -5, -1, 3, 7, 11};

    /**
     * A crab room layout (OSRS wiki "Crab puzzle": a carved sculpture sends the orb across the room; every layout needs
     * three crabs). The sculpture stands on the north wall ({@code lane} +1, the orb heading south) or the south wall
     * ({@code lane} -1, heading north); the black crystal faces it across the room. The yellow, cyan and magenta crystals
     * (0-2) stand on the wall to the orb's right, at {@code rows[i]} along the lane, so each needs one crab in the lane at
     * its row, struck the right colour, to bounce the orb into it.
     */
    public record CrabLayout(int lane, int[] rows) {
        /** The tile the orb leaves the sculpture from, and its heading {x, z}. */
        public int[] start() {
            return new int[]{0, -lane * (CRAB_ROOM_REACH - 1), 0, lane};
        }

        /** Crystal {@code i}'s tile {x, z}: 0-2 on the side wall to the orb's right, 3 (black) at the far end of the lane. */
        public int[] crystal(int i) {
            if (i == 3) {
                return new int[]{0, lane * CRAB_ROOM_REACH};
            }
            int[] right = turnRight(0, lane);
            return new int[]{right[0] * CRAB_ROOM_REACH, rows[i]};
        }
    }

    /** A random crab layout: either wall for the sculpture and three different crystal rows. */
    public static CrabLayout crabLayout(Random random) {
        List<Integer> rows = new java.util.ArrayList<>();
        for (int r : CRAB_CRYSTAL_ROWS) {
            rows.add(r);
        }
        java.util.Collections.shuffle(rows, random);
        return new CrabLayout(random.nextBoolean() ? 1 : -1, new int[]{rows.get(0), rows.get(1), rows.get(2)});
    }

    /**
     * Traces the orb through a crab room on the tile grid (pure model of the room logic): it moves one tile at a time from
     * the sculpture, bounces right off crabs (taking or keeping their colour, or vanishing), and stops at a wall, a player
     * or a crystal. Returns the crystal it reaches with the right colour (0-3), or -1.
     *
     * @param crabs the crab colour on each occupied tile, keyed {@code x * 1000 + z}
     */
    public static int traceOrb(CrabLayout layout, java.util.Map<Integer, Colour> crabs) {
        int[] s = layout.start();
        int x = s[0];
        int z = s[1];
        int dx = s[2];
        int dz = s[3];
        Colour colour = Colour.WHITE;
        for (int step = 0; step < 400; step++) {
            x += dx;
            z += dz;
            for (int i = 0; i < 4; i++) {
                int[] c = layout.crystal(i);
                if (c[0] == x && c[1] == z) {
                    return colour == crystalWants(i) ? i : -1;
                }
            }
            if (Math.abs(x) >= CRAB_ROOM_REACH || Math.abs(z) >= CRAB_ROOM_REACH) {
                return -1;
            }
            Colour crab = crabs.get(x * 1000 + z);
            if (crab != null) {
                colour = reflect(colour, crab);
                if (colour == null) {
                    return -1;
                }
                int[] t = turnRight(dx, dz);
                dx = t[0];
                dz = t[1];
            }
        }
        return -1;
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

    /**
     * The orb an orb-and-crab collision leaves (null: the orb is gone; OSRS wiki "Crab puzzle"): a white orb takes the
     * crab's colour (and stays white off an uncoloured crab); a coloured orb can't change colour again, so it only bounces
     * off a crab of its own colour and disappears against any other.
     */
    public static Colour reflect(Colour orb, Colour crab) {
        if (orb == Colour.WHITE) {
            return crab;
        }
        return orb == crab ? orb : null;
    }

    /**
     * The orb's new heading after it bounces off a crab: it always turns right from its own point of view (OSRS wiki). In
     * block coordinates (x east, z south), heading south turns west, west turns north, north turns east, east turns south.
     */
    public static int[] turnRight(int dx, int dz) {
        return new int[]{-dz, dx};
    }

    /** The colour a crystal wants: yellow (0) blue, cyan (1) red, magenta (2) green, black (3) white (OSRS wiki). */
    public static Colour crystalWants(int crystal) {
        return switch (crystal) {
            case 0 -> Colour.BLUE;
            case 1 -> Colour.RED;
            case 2 -> Colour.GREEN;
            default -> Colour.WHITE;
        };
    }

    /** Damage the orb does to a player who steps into it (OSRS wiki: 10-30, scaled to current health). // approx: 30% */
    public static int orbDamage(int currentHp) {
        int hp = Math.max(1, currentHp);
        return Math.min(hp, Math.max(10, Math.min(30, hp * 30 / 100)));
    }

    // ------------------------------------------------------------------ the Great Olm

    /** The head's standard attack max hit (OSRS DPS calculator): 27, 28 in the penultimate phase, 29 in the last. */
    public static int olmMaxHit(boolean penultimate, boolean headPhase) {
        return headPhase ? 29 : penultimate ? 28 : 27;
    }

    /** Challenge Mode max hits (OSRS wiki "Great Olm": 38 in the early phases, 39, 41 in the final stand). */
    public static int olmMaxHit(boolean penultimate, boolean headPhase, boolean challenge) {
        if (!challenge) {
            return olmMaxHit(penultimate, headPhase);
        }
        return headPhase ? 41 : penultimate ? 39 : 38;
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

    /**
     * Game ticks the left hand stays clenched (invulnerable, and no generic special attacks), unless the right hand goes
     * down first (OSRS wiki: "roughly 30 seconds").
     */
    public static final int OLM_CLENCH_TICKS = 50;
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

    /**
     * A crystal bomb hits a radius of 4 tiles: 15 at the rim, plus 15 for every tile closer, 60 directly on top (OSRS wiki
     * "Chambers of Xeric/Strategies").
     */
    public static int olmCrystalBombDamage(double distanceTiles) {
        if (distanceTiles > 4.5) {
            return 0;
        }
        int tiles = Math.max(1, (int) Math.round(Math.max(0, distanceTiles)));
        return 15 * (5 - Math.min(4, tiles));
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
    /**
     * Teleport (OSRS wiki "Chambers of Xeric/Strategies"): no damage when the swapped players stand on each other, 5 when
     * adjacent, 10 two tiles apart and so on, up to 50 as far apart as possible.
     */
    public static int olmTeleportDamage(double distanceTiles) {
        return (int) Math.min(50, 5 * Math.round(Math.max(0, distanceTiles)));
    }

    /** Up to six players are paired for the swap; an odd one out is left alone (OSRS wiki). */
    public static final int OLM_SWAP_MAX_PLAYERS = 6;
    /** A solo player is sent to a random tile up to ten tiles from where they stood (OSRS wiki). */
    public static final int OLM_SWAP_SOLO_RANGE = 10;
}
