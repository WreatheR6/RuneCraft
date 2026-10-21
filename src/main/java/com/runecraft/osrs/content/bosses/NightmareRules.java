package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.Prayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The fight rules of The Nightmare and Phosani's Nightmare (OSRS Wiki "The Nightmare", "The Nightmare/Strategies",
 * "Phosani's Nightmare", "Phosani's Nightmare/Strategies", "Parasite", "Parasite (Phosani's Nightmare)", "Husk", "Husk
 * (Phosani's Nightmare)", "Sleepwalker", "Totem (The Nightmare)", "Nightmare Berries", July 2026), played out by the bosses
 * module's Nightmare script.
 *
 * <p>Players never damage her physical health directly. Each phase they break her shield (The Nightmare: 2,000, plus 200 for
 * every player beyond five, up to 19,600; Phosani: 400), then charge the four totems in the corners (300 each, plus 30 for
 * every player beyond five, up to 2,640; Phosani: 200; magic charges twice as fast). Charged, the totems blast her for 800 of
 * her 2,400 health: The Nightmare dies to the third blast. Phosani survives hers and fights on in a desperation phase of 150
 * health. Phosani's first phase was removed on 25 June 2025, so she has three shield phases, as the current wiki says.
 */
public final class NightmareRules {
    private NightmareRules() {
    }

    // ------------------------------------------------------------ shields, totems, phases
    /** Shield and totem values scale from a five-player fight; smaller teams fight the five-player values. */
    public static final int SCALE_FROM = 5;
    public static final int SHIELD_BASE = 2000;
    public static final int SHIELD_PER_PLAYER = 200;
    public static final int SHIELD_CAP = 19600;
    public static final int TOTEM_BASE = 300;
    public static final int TOTEM_PER_PLAYER = 30;
    public static final int TOTEM_CAP = 2640;
    public static final int PHOSANI_SHIELD = 400;
    public static final int PHOSANI_TOTEM = 200;
    /** Party sizes beyond this don't raise the chance of the second unique roll (the shield and totems scale on to their caps). */
    public static final int MAX_PARTY = 80;
    public static final int TOTEMS = 4;
    /** Magic charges the totems twice as fast ("they will take two times as much damage if struck with magic"). */
    public static final int TOTEM_MAGIC_MULTIPLIER = 2;
    /** Each blast of the charged totems: 800 of The Nightmare's 2,400 health. */
    public static final int TOTEM_BLAST = 800;
    /** Shield phases (both versions); The Nightmare dies to the blast that ends the last one. */
    public static final int SHIELD_PHASES = 3;
    /** Phosani's desperation phase: 150 health, after her third set of totems. */
    public static final int PHOSANI_DESPERATION_HEALTH = BossMechanics.PHOSANI_DESPERATION_HEALTH;

    /** Shield durability of one phase. */
    public static int shield(boolean phosani, int players) {
        if (phosani) {
            return PHOSANI_SHIELD;
        }
        return Math.min(SHIELD_CAP, SHIELD_BASE + SHIELD_PER_PLAYER * extraPlayers(players));
    }

    /** Charge one totem needs. */
    public static int totemCharge(boolean phosani, int players) {
        if (phosani) {
            return PHOSANI_TOTEM;
        }
        return Math.min(TOTEM_CAP, TOTEM_BASE + TOTEM_PER_PLAYER * extraPlayers(players));
    }

    private static int extraPlayers(int players) {
        return Math.max(0, players - SCALE_FROM);
    }

    /** Charge a hit of {@code damage} puts into a totem. */
    public static int totemCharge(int damage, boolean magic) {
        return Math.max(0, damage) * (magic ? TOTEM_MAGIC_MULTIPLIER : 1);
    }

    /** Everything players must deal to her (not to the totems): three shields, plus Phosani's desperation health. */
    public static int totalWork(boolean phosani, int players) {
        return SHIELD_PHASES * shield(phosani, players) + (phosani ? PHOSANI_DESPERATION_HEALTH : 0);
    }

    /**
     * What is still to be dealt to her: the rest of this phase's shield, the later phases' shields and Phosani's desperation
     * health ({@code desperationLeft} once that phase has begun, else all of it).
     */
    public static int workLeft(boolean phosani, int players, int phase, int shieldLeft, boolean desperate, int desperationLeft) {
        if (desperate) {
            return Math.max(0, desperationLeft);
        }
        int later = Math.max(0, SHIELD_PHASES - 1 - phase) * shield(phosani, players);
        return Math.max(0, shieldLeft) + later + (phosani ? PHOSANI_DESPERATION_HEALTH : 0);
    }

    /** Game ticks between disturbing her and her waking: 30 s for The Nightmare (players may join), 10 s for Phosani. */
    public static final int NIGHTMARE_WAKE_TICKS = 50;
    public static final int PHOSANI_WAKE_TICKS = 17;

    // ------------------------------------------------------------ special attacks
    /** Her special attacks (Grasping Claws is the "rifts" of Perfect Nightmare). */
    public enum Special { GRASPING_CLAWS, HUSKS, CORPSE_FLOWERS, CURSE, PARASITES, SURGE, SPORES }

    /**
     * The Nightmare's specials by phase (0-2): Grasping Claws, Husks and Corpse Flowers; Grasping Claws, Curse and Parasites;
     * Grasping Claws, Spores and Surge.
     */
    public static List<Special> nightmareSpecials(int phase) {
        return switch (Math.max(0, Math.min(2, phase))) {
            case 0 -> List.of(Special.GRASPING_CLAWS, Special.HUSKS, Special.CORPSE_FLOWERS);
            case 1 -> List.of(Special.GRASPING_CLAWS, Special.CURSE, Special.PARASITES);
            default -> List.of(Special.GRASPING_CLAWS, Special.SPORES, Special.SURGE);
        };
    }

    /**
     * Phosani's special sets: one spawn (husks or a parasite), one Grasping Claws companion (spores or corpse flowers) and
     * one other (curse or surge).
     */
    public enum PhosaniSet {
        A1(Special.SPORES, Special.HUSKS, Special.CURSE),
        A2(Special.SPORES, Special.HUSKS, Special.SURGE),
        B1(Special.CORPSE_FLOWERS, Special.PARASITES, Special.CURSE),
        B2(Special.CORPSE_FLOWERS, Special.PARASITES, Special.SURGE);

        private final List<Special> specials;

        PhosaniSet(Special... specials) {
            this.specials = List.of(specials);
        }

        public List<Special> specials() {
            return specials;
        }

        public boolean a() {
            return this == A1 || this == A2;
        }
    }

    /**
     * Phosani's three phases' sets: the first phase picks A or B at random, every later phase takes the other letter, and
     * each picks 1 or 2 at random (e.g. A1, B2, A2), so neither the spawn nor the claw companion repeats in a row.
     */
    public static List<PhosaniSet> phosaniSets(Random r) {
        List<PhosaniSet> out = new ArrayList<>();
        boolean a = r.nextBoolean();
        for (int i = 0; i < SHIELD_PHASES; i++) {
            boolean one = r.nextBoolean();
            out.add(a ? (one ? PhosaniSet.A1 : PhosaniSet.A2) : (one ? PhosaniSet.B1 : PhosaniSet.B2));
            a = !a;
        }
        return out;
    }

    /**
     * One round of a phase's specials in a random order: she uses each before any repeats ("will expend all special
     * attacks first before she may use them again").
     */
    public static List<Special> specialRound(List<Special> specials, Random r) {
        List<Special> round = new ArrayList<>(specials);
        Collections.shuffle(round, r);
        return round;
    }

    /** Standard attacks between two specials (approx: the wiki gives no cadence). */
    public static final int ATTACKS_PER_SPECIAL = 4;
    /** Phosani's extra Grasping Claws between her set's specials: one standard attack in this many (approx). */
    public static final int PHOSANI_RANDOM_CLAWS = 6;
    /** Grasping Claws in a row with Phosani's spores or corpse flowers ("spores are always summoned before six consecutive claw attacks"). */
    public static final int PHOSANI_CLAW_CHAIN = 6;
    /** The Nightmare's Grasping Claws after her corpse flowers (approx). */
    public static final int NIGHTMARE_CLAW_CHAIN = 2;

    // ------------------------------------------------------------ standard attacks
    /** Game ticks to react: 3 for her magic and ranged attacks, 2 for her melee. */
    public static final int RANGED_WIND_UP = 3;
    public static final int MELEE_WIND_UP = 2;

    /** Which protection prayer a player has against one of her standard attacks. */
    public enum Protection { CORRECT, WRONG, NONE }

    /**
     * Damage of a standard attack against a player's protection: The Nightmare deals 20% through the right protection
     * prayer and 120% through a wrong one (her melee per the RuneScript Jagex shared; her magic and ranged alike, approx);
     * Phosani's attacks are blocked completely by the right prayer.
     */
    public static int standardDamage(boolean phosani, int rolled, Protection p) {
        int d = Math.max(0, rolled);
        if (phosani) {
            return p == Protection.CORRECT ? 0 : d;
        }
        return switch (p) {
            case CORRECT -> d / 5;
            case WRONG -> d * 6 / 5;
            case NONE -> d;
        };
    }

    /** Chance (in 1/3s) that a standard attack in melee reach is her melee: one in three, two in three in Phosani's third phase. */
    public static int meleeThirds(boolean phosani, int phase) {
        return phosani && phase >= 2 ? 2 : 1;
    }

    // ------------------------------------------------------------ Curse
    /** The curse shuffles the protection prayers for her next five attacks. */
    public static final int CURSE_ATTACKS = 5;

    /** The prayer a cursed player's click turns on: Melee gives Magic, Magic gives Missiles, Missiles gives Melee. */
    public static Prayer cursed(Prayer clicked) {
        if (clicked == null) {
            return null;
        }
        return switch (clicked) {
            case PROTECT_FROM_MELEE -> Prayer.PROTECT_FROM_MAGIC;
            case PROTECT_FROM_MAGIC -> Prayer.PROTECT_FROM_MISSILES;
            case PROTECT_FROM_MISSILES -> Prayer.PROTECT_FROM_MELEE;
            default -> clicked;
        };
    }

    // ------------------------------------------------------------ Grasping Claws, Surge
    /** Grasping Claws: up to 50 on The Nightmare's portals; 50-65 on Phosani's, which also clump around the player. */
    public static final int CLAWS_MAX = 50;
    public static final int PHOSANI_CLAWS_MIN = 50;
    public static final int PHOSANI_CLAWS_MAX = 65;
    /** Ticks between the portals opening and the claws coming up (approx). */
    public static final int CLAWS_DELAY = 3;

    public static int clawDamage(boolean phosani, Random r) {
        return phosani ? PHOSANI_CLAWS_MIN + r.nextInt(PHOSANI_CLAWS_MAX - PHOSANI_CLAWS_MIN + 1) : r.nextInt(CLAWS_MAX + 1);
    }

    /** Surge: up to 60 (Phosani ~85) to anyone in her path; she charges up for this many ticks first (approx). */
    public static final int SURGE_MAX = 60;
    public static final int PHOSANI_SURGE_MAX = 85;
    public static final int SURGE_WIND_UP = 3;

    // ------------------------------------------------------------ Corpse Flowers
    /** The berries bloom five ticks after they appear, then hit up to 20 a tick on each side, healing her twice that. */
    public static final int FLOWER_BLOOM_TICKS = 5;
    public static final int FLOWER_MAX = 20;
    public static final int FLOWER_HEAL_MULTIPLIER = 2;
    /** How long the flowers stay in bloom (approx). */
    public static final int FLOWER_TICKS = 20;

    /** Quadrant of a point relative to her centre: 0 north-east, 1 north-west, 2 south-west, 3 south-east (north is -z). */
    public static int quadrant(double dx, double dz) {
        boolean east = dx >= 0;
        boolean north = dz < 0;
        return north ? (east ? 0 : 1) : (east ? 3 : 2);
    }

    /**
     * Rows of berries along a quadrant's two sides when {@code safe} is the safe one: the safe quadrant has blossoms on both
     * sides, its two neighbours one row of berries, the opposite quadrant berries on both sides (twice the damage).
     */
    public static int berrySides(int safe, int quadrant) {
        int d = Math.floorMod(quadrant - safe, 4);
        return d == 0 ? 0 : d == 2 ? 2 : 1;
    }

    /** Whether half-line {@code line} (0 north, 1 west, 2 south, 3 east of her) is a side of quadrant {@code q} (0 NE, 1 NW, 2 SW, 3 SE). */
    public static boolean rowBorders(int line, int q) {
        return switch (q) {
            case 0 -> line == 0 || line == 3;
            case 1 -> line == 0 || line == 1;
            case 2 -> line == 2 || line == 1;
            default -> line == 2 || line == 3;
        };
    }

    /** The corpse flower rows' reach from her centre (blocks): they sprout from 2 to 13 out. // approx: drawn length */
    public static final int FLOWER_ROW_FROM = 2;
    public static final int FLOWER_ROW_TO = 13;

    /**
     * The floor columns {@code {x, z}} shown safe beside blossom row {@code line} (0 north, 1 west, 2 south, 3 east of her
     * centre ({@code cx}, {@code cz})) when quadrant {@code safe} is the safe one (round 43 floor markers, // playability):
     * the row itself runs along the quadrant border, half of its blocks in the quadrant next door, so the safe row is one
     * block into the safe quadrant and keeps only blocks lying wholly in it ({@link #quadrant} at all four corners, the test
     * the bloom uses). Empty for a berry row (one that doesn't border the safe quadrant).
     */
    public static List<int[]> blossomFloor(int safe, int line, double cx, double cz) {
        if (!rowBorders(line, safe)) {
            return new ArrayList<>();
        }
        int[][] dirs = {{0, -1}, {-1, 0}, {0, 1}, {1, 0}};
        int sx = safe == 0 || safe == 3 ? 1 : -1; // the safe quadrant's side: east for NE/SE
        int sz = safe == 2 || safe == 3 ? 1 : -1; // south (+z) for SW/SE
        double ox = dirs[line][0] == 0 ? sx : 0;
        double oz = dirs[line][1] == 0 ? sz : 0;
        List<int[]> row = com.runecraft.osrs.FloorShapes.line(cx + dirs[line][0] * FLOWER_ROW_FROM + ox,
                cz + dirs[line][1] * FLOWER_ROW_FROM + oz, cx + dirs[line][0] * FLOWER_ROW_TO + ox,
                cz + dirs[line][1] * FLOWER_ROW_TO + oz, 0.3);
        return com.runecraft.osrs.FloorShapes.inside(row, (x, z) -> quadrant(x - cx, z - cz) == safe);
    }

    // ------------------------------------------------------------ Sleepwalkers
    /** Sleepwalkers of The Nightmare (start of phases 2 and 3): 1 to 24 with the party size (approx: one per player). */
    public static int nightmareSleepwalkers(int players) {
        return Math.max(1, Math.min(24, players));
    }

    /** Sleepwalkers after a phase: Phosani 2, 3 and 4 ({@link BossMechanics#sleepwalkers}); The Nightmare by party size. */
    public static int sleepwalkers(boolean phosani, int phaseEnded, int players) {
        return phosani ? BossMechanics.sleepwalkers(phaseEnded) : nightmareSleepwalkers(players);
    }

    /** Whether sleepwalkers come after the blast ending {@code phaseEnded} (1-3): The Nightmare's third blast kills her. */
    public static boolean sleepwalkersAfter(boolean phosani, int phaseEnded) {
        return phosani ? phaseEnded >= 1 && phaseEnded <= SHIELD_PHASES : phaseEnded >= 1 && phaseEnded < SHIELD_PHASES;
    }

    // ------------------------------------------------------------ Parasites
    /** Parasites per Parasites special: one per five players, at least one (approx: "random player(s) based on group size"). */
    public static int parasites(int players) {
        return Math.max(1, (Math.max(1, players) + 4) / 5);
    }

    /**
     * A parasite's hitpoints: The Nightmare's 40 weakened, 80 full; Phosani's 55, plus the damage of its burst when it wasn't
     * weakened ("deals ~90 damage when bursting out which is added to its health").
     */
    public static int parasiteHitpoints(boolean phosani, boolean weakened, int burst) {
        if (phosani) {
            return 55 + (weakened ? 0 : Math.max(0, burst));
        }
        return BossMechanics.parasiteHitpoints(weakened);
    }

    /** Damage of the burst: Phosani's 5-10 weakened, ~90 (approx 80-100) full; The Nightmare's as {@link BossMechanics#parasiteBurst}. */
    public static int parasiteBurst(boolean phosani, boolean weakened, Random r) {
        if (phosani) {
            return weakened ? 5 + r.nextInt(6) : 80 + r.nextInt(21);
        }
        return BossMechanics.parasiteBurst(r, weakened);
    }

    /** One support blast: 31-50 (The Nightmare), ~51-100 (Phosani). */
    public static int parasiteHeal(boolean phosani, Random r) {
        return phosani ? 51 + r.nextInt(50) : BossMechanics.parasiteHeal(r);
    }

    /** A parasite's own attacks (ranged then magic, alternating) when it has nothing to heal: 10/12, Phosani's 18/22. */
    public static int parasiteMaxHit(boolean phosani, boolean weakened) {
        return phosani ? (weakened ? 18 : 22) : (weakened ? 10 : 12);
    }

    /** Magic and ranged attack bonus of a parasite's attacks (wiki infoboxes). */
    public static int parasiteAttackBonus(boolean phosani, boolean weakened) {
        return phosani ? (weakened ? 44 : 84) : (weakened ? 12 : 20);
    }

    // ------------------------------------------------------------ Husks
    /** Two husks (magic and ranged), 20 hitpoints each; up to 14 a hit (Phosani's 26, and far more accurate). */
    public static final int HUSKS = 2;
    public static final int HUSK_HITPOINTS = 20;
    public static final int HUSK_SPEED = 4;
    public static final int HUSK_LEVEL = 80;

    public static int huskMaxHit(boolean phosani) {
        return phosani ? 26 : 14;
    }

    public static int huskAttackBonus(boolean phosani) {
        return phosani ? 168 : 0;
    }

    // ------------------------------------------------------------ drops
    /** Unique rolls: armour/mace/staff 1/83.33 and orbs 1/320 (The Nightmare); 1/140 and ~1/533 (Phosani). */
    public static final List<String> INQUISITOR_TABLE = List.of("nightmare_staff", "inquisitors_great_helm", "inquisitors_hauberk",
            "inquisitors_plateskirt", "inquisitors_mace");
    public static final List<String> ORB_TABLE = List.of("eldritch_orb", "harmonised_orb", "volatile_orb");

    /** Chance of both unique tables being rolled a second time: (party size - 5)%, clamped to 0-75%, party capped at 80. */
    public static double secondUniqueRoll(int players) {
        return Math.max(0, Math.min(75, Math.min(MAX_PARTY, players) - SCALE_FROM)) / 100.0;
    }

    /** Little nightmare: 1/800 a player in the fight, 1/4,000 from five on (counted at the start); Phosani 1/1,400. */
    public static int petRate(boolean phosani, int players) {
        return phosani ? 1400 : 800 * Math.max(1, Math.min(SCALE_FROM, players));
    }

    /** Elite clue (1/190 for the MVP, 1/200 for the others) and jar of dreams (1/1,900 and 1/2,000). */
    public static final int CLUE_MVP = 190;
    public static final int CLUE = 200;
    public static final int JAR_MVP = 1900;
    public static final int JAR = 2000;

    /** Phosani's Slepey tablet: 1/25, and always on the 25th kill (or the next one) while the player has none. */
    public static final int SLEPEY_TABLET_RATE = 25;
    public static final int SLEPEY_TABLET_KC = 25;

    public static boolean slepeyTabletGuaranteed(int killCount, boolean hasOne) {
        return !hasOne && killCount >= SLEPEY_TABLET_KC;
    }

    /** A player's share of the loot needs damage on both her shield and the totems. */
    public static boolean eligible(int shieldDamage, int totemCharge) {
        return shieldDamage > 0 && totemCharge > 0;
    }
}
