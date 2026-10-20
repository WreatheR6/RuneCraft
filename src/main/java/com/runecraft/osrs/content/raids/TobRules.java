package com.runecraft.osrs.content.raids;

import java.util.List;
import java.util.Random;

/**
 * Theatre of Blood room rules (pure maths, unit tested; round 33). Sources: the OSRS wiki pages "Theatre of
 * Blood/Strategies", "Chest (Theatre of Blood)", "Xarpus" and "Verzik Vitur" (July 2026 dump) and the Mod Ash quotes they
 * cite. Values the wiki doesn't give are marked approx.
 */
public final class TobRules {
    private TobRules() {
    }

    // ------------------------------------------------------------------ the whole Theatre

    /** One item of the supply chest's stock (OSRS "Chest (Theatre of Blood)"): its cost in points, and a per-chest limit. */
    public record Supply(String item, String name, int cost, int perChest) {
    }

    /**
     * The supply chest after the Pestilent Bloat and Sotetseg (OSRS): stamina potion (one per chest) 1 point, prayer
     * potion 2, Saradomin brew 3, super restore 3, shark 1, sea turtle 2, manta ray 2 (the mushroom potato isn't in this
     * world). Potions are 4-dose.
     */
    public static final List<Supply> SUPPLIES = List.of(
            new Supply("stamina_potion", "Stamina potion(4)", 1, 1),
            new Supply("prayer_potion", "Prayer potion(4)", 2, 0),
            new Supply("saradomin_brew", "Saradomin brew(4)", 3, 0),
            new Supply("super_restore", "Super restore(4)", 3, 0),
            new Supply("shark", "Shark", 1, 0),
            new Supply("sea_turtle", "Sea turtle", 2, 0),
            new Supply("manta_ray", "Manta ray", 2, 0));

    public static Supply supply(String item) {
        for (Supply s : SUPPLIES) {
            if (s.item().equals(item)) {
                return s;
            }
        }
        return null;
    }

    /** A player's performance in the fights since the last chest. */
    public enum Performance {
        ABOVE_AVERAGE, AVERAGE, BELOW_AVERAGE
    }

    /**
     * How a player did since the last chest. // approx: OSRS doesn't publish the rule; here no deaths and the most damage
     * (MVP of a room) is above average, no deaths average, any death below average
     */
    public static Performance performance(int deaths, boolean mvp) {
        if (deaths > 0) {
            return Performance.BELOW_AVERAGE;
        }
        return mvp ? Performance.ABOVE_AVERAGE : Performance.AVERAGE;
    }

    /** Supply points a chest gives (OSRS wiki): above average 10-13, average 8-11, below average 6-9. */
    public static int chestPoints(Performance p, Random random) {
        return chestPoints(p, RaidMode.NORMAL, random);
    }

    /**
     * Supply points a chest gives in a mode. Hard mode chests give fewer (OSRS wiki "Theatre of Blood/Hard Mode": about
     * 15 points over both chests without deaths, 18 at most). // approx: 4 fewer than Normal mode (6-9 / 4-7 / 2-5), so two
     * chests hold at most 18
     */
    public static int chestPoints(Performance p, RaidMode mode, Random random) {
        int base = switch (p) {
            case ABOVE_AVERAGE -> 10;
            case AVERAGE -> 8;
            case BELOW_AVERAGE -> 6;
        };
        return base - (mode == RaidMode.HARD ? HARD_CHEST_POINTS_LESS : 0) + random.nextInt(4);
    }

    /** How many fewer points a Hard mode chest gives. // approx */
    public static final int HARD_CHEST_POINTS_LESS = 4;

    /**
     * Entry mode chests (OSRS wiki "Bandages (Theatre of Blood)"): ten bandages after the Bloat and Sotetseg, and again
     * after every wipe; uncollected ones don't carry over.
     */
    public static final int ENTRY_BANDAGES = 10;
    /** The Entry mode bandage (OSRS item "Bandages", Theatre of Blood). */
    public static final String BANDAGES = "tob_bandages";
    /** A bandage heals 20, restores 29 prayer and 20% run energy, cures poison and works as a stamina potion. */
    public static final int BANDAGE_HEAL = 20;
    public static final int BANDAGE_PRAYER = 29; // approx: the wiki suspects it scales with the Prayer level
    public static final int BANDAGE_RUN_PERCENT = 20;

    /** A bandage's Attack, Strength and Defence boost: one level below a super potion, floor(level x 15%) + 4 (wiki). */
    public static int bandageMeleeBoost(int level) {
        return level * 15 / 100 + 4;
    }

    /** A bandage's Ranged boost, as a ranging potion: floor(level / 10) + 4; Magic gets a flat +4 (wiki). */
    public static int bandageRangedBoost(int level) {
        return level / 10 + 4;
    }

    public static final int BANDAGE_MAGIC_BOOST = 4;

    /** A player who died in both of the last two fights only finds an onion in the chest (OSRS). */
    public static boolean onionOnly(int deathsInLastTwoFights) {
        return deathsInLastTwoFights >= 2;
    }

    // ------------------------------------------------------------------ The Maiden of Sugadinti

    /** Game ticks between the Maiden's attacks (OSRS: 10). */
    public static final int MAIDEN_ATTACK_SPEED = 10;

    /** Blackstorm's damage (OSRS wiki): 36.5 + 3.5 per Nylocas Matomenos that reached her; halved by Protect from Magic. */
    public static int maidenBlackstormMax(int fed) {
        return (int) Math.floor(36.5 + 3.5 * Math.max(0, fed));
    }

    /** Blackstorm drains the target's combat stats by (damage + 1) / 5, half the time (OSRS wiki). */
    public static int maidenDrain(int damage) {
        return (damage + 1) / 5;
    }

    /** A blood splat deals (and heals the Maiden) 10 + 2 per Matomenos that reached her (OSRS wiki). */
    public static int maidenSplat(int fed) {
        return 10 + 2 * Math.max(0, fed);
    }

    /**
     * The player standing furthest away gets two more blood splats in a 5x5 area around them, three in all (OSRS wiki "The
     * Maiden of Sugadinti": "three blood spots at the player standing furthest away"; Strategies: "two additional").
     */
    public static final int MAIDEN_EXTRA_SPLATS = 2;
    /** A blood splat turns into a blood spawn 10% of the time, 20% if someone stood on it (OSRS wiki). */
    public static double bloodSpawnChance(boolean steppedOn) {
        return steppedOn ? 0.2 : 0.1;
    }

    /** At most eight blood spawns appear in a fight (OSRS wiki "Blood spawn", January 2020 update). */
    public static final int MAIDEN_BLOOD_SPAWN_CAP = 8;

    /** Blood spawns a throw can make (OSRS, August 2021 update): at most one if everyone dodged the blood. */
    public static int bloodSpawnsFromThrow(boolean anyoneSteppedOn, int spots) {
        return anyoneSteppedOn ? spots : Math.min(1, spots);
    }

    /** Blood spawns' trails dry in about 20-30 seconds (OSRS wiki). // approx: 40 ticks */
    public static final int MAIDEN_TRAIL_TICKS = 40;

    /** A blood trail hits for 5-13 (2-5 in Entry mode) and heals the Maiden by the same (OSRS wiki "Blood spawn"). */
    public static int bloodTrailMin(RaidMode mode) {
        return mode == RaidMode.ENTRY ? 2 : 5;
    }

    public static int bloodTrailMax(RaidMode mode) {
        return mode == RaidMode.ENTRY ? 5 : 13;
    }
    /** After blood splats the Maiden can't throw them for her next two attacks (OSRS). */
    public static final int MAIDEN_SPLAT_COOLDOWN = 2;

    // ------------------------------------------------------------------ The Pestilent Bloat

    /** The Bloat's flies hit everyone in its line of sight for up to 20 every tick it walks (Protect from Missiles: -25%). */
    public static final int BLOAT_FLIES_MAX = 20;
    /** It stomps on the 30th tick of its 32-tick shutdown for 40-80 (OSRS wiki). */
    public static final int BLOAT_STOMP_TICK = 30;
    public static final int BLOAT_STOMP_MIN = 40;
    public static final int BLOAT_STOMP_MAX = 80;
    /** Below 90% health, hands fall from the ceiling while it walks: 30-50 and a short stun (OSRS wiki). */
    public static final double BLOAT_HANDS_BELOW = 0.9;
    public static final int BLOAT_HANDS_MIN = 30;
    public static final int BLOAT_HANDS_MAX = 50;
    /** A walk lasts 4 ticks longer when nobody attacked it during its last shutdown (OSRS wiki). */
    public static final int BLOAT_UNATTACKED_BONUS = 4;

    /** The Bloat's walking speed (OSRS wiki): runs between 40% and 60% health, alternates below 40% with every hit. */
    public static boolean bloatRuns(double healthFraction, int hitsBelow40) {
        if (healthFraction >= 0.4 && healthFraction <= 0.6) {
            return true;
        }
        return healthFraction < 0.4 && hitsBelow40 % 2 == 1;
    }

    // ------------------------------------------------------------------ Sotetseg

    /** Sotetseg's melee lunge (up to 45, 22 prayed) and magic orbs (up to 50, fully blocked by the right prayer). */
    public static final int SOTETSEG_MELEE_MAX = 45;
    public static final int SOTETSEG_ORB_MAX = 50;
    /** After 10 magic attacks Sotetseg launches the ball of death (OSRS wiki). */
    public static final int SOTETSEG_ATTACKS_PER_BALL = 10;
    /** Sotetseg attacks every 5 game ticks. */
    public static final int SOTETSEG_ATTACK_SPEED = 5;
    /** A wrong maze tile hurts everyone near it for 6.67% of current hitpoints + 15 every tick (OSRS wiki). */
    public static int mazeWrongTile(int currentHp) {
        return mazeWrongTile(currentHp, RaidMode.NORMAL);
    }

    /** {@link #mazeWrongTile(int)} by mode: + 11 instead of + 15 in Entry mode (OSRS wiki "Sotetseg"). */
    public static int mazeWrongTile(int currentHp, RaidMode mode) {
        return (mode == RaidMode.ENTRY ? 11 : 15) + (int) (currentHp * 0.0667);
    }

    /**
     * The maze's tornado (OSRS wiki "Theatre of Blood/Strategies"): it spawns once someone steps past the third row and
     * follows the path; anyone it catches is hurt (the whole party, per the "Sotetseg" page). It chases the maze runner
     * only when they are alone in the encounter. Rows here are 0-based, so the fourth row is row 3.
     */
    public static final int SOTETSEG_TORNADO_ROW = 3;
    /** The tornado's damage. // approx: "massive damage", no figure published */
    public static final int SOTETSEG_TORNADO_MIN = 30;
    public static final int SOTETSEG_TORNADO_MAX = 50;
    /** Game ticks the tornado takes per path tile (players run two tiles a tick). // approx */
    public static final int SOTETSEG_TORNADO_TICKS_PER_TILE = 1;
    /** Sotetseg's Defence can't be drained below 100, and walking the maze restores it (OSRS wiki). */
    public static final int SOTETSEG_DEFENCE_FLOOR = 100;

    /** Whether the maze's tornado is released: someone (or, solo, the runner) has passed the third row. */
    public static boolean mazeTornado(int furthestRow) {
        return furthestRow > SOTETSEG_TORNADO_ROW - 1;
    }

    // ------------------------------------------------------------------ Xarpus

    /** Exhumeds before Xarpus starts phase 2 (OSRS wiki: 12 in trios and smaller, 15 in four- and five-player teams). */
    public static int xarpusExhumeds(int partySize) {
        return partySize <= 3 ? 12 : 15;
    }

    /** Game ticks between exhumeds in phase 1. // approx */
    public static final int XARPUS_EXHUMED_INTERVAL = 3;
    /** An exhumed returns to the ground after this many ticks. // approx */
    public static final int XARPUS_EXHUMED_TICKS = 8;

    /**
     * Xarpus's poison (Mod Kieren): 4-8 a tick on a splattered tile, buffed by a percentage for every exhumed he
     * absorbed. // approx: +5% per exhumed
     */
    public static int xarpusPoison(int roll4to8, int absorbed) {
        return (int) Math.round(roll4to8 * (1 + 0.05 * Math.max(0, absorbed)));
    }

    /** Standing under Xarpus: pebbles for up to 9 a tick (OSRS wiki). */
    public static final int XARPUS_STOMP_MAX = 9;
    /** Phase 3 starts at 25% health; he stares at a new quadrant every 8 ticks, never the same twice (OSRS wiki). */
    public static final double XARPUS_SCREECH_AT = 0.25;
    public static final int XARPUS_TURN_TICKS = 8;

    /**
     * The phase 3 retaliation (Mod Ash): 50-75, buffed by 40% of the stat uplift his absorbed exhumeds gave. // approx:
     * the uplift is 5% per exhumed
     */
    public static int xarpusRetaliation(int roll50to75, int absorbed) {
        return (int) Math.round(roll50to75 * (1 + 0.4 * 0.05 * Math.max(0, absorbed)));
    }

    /** Xarpus picks a new quadrant (0-3), never the one he stared at last. */
    public static int xarpusNextQuadrant(int last, Random random) {
        int q = random.nextInt(3);
        return q >= last && last >= 0 ? q + 1 : q;
    }

    /**
     * Hard mode (OSRS wiki "Xarpus"): the fight opens with poison on the two outermost rings of his arena, except the six
     * tiles (two rows of three) at the entrance.
     */
    public static final int XARPUS_HARD_EDGE_RINGS = 2;

    /**
     * Whether a tile of his arena (tile coordinates from its centre, {@code half} tiles to each side, the entrance on the
     * west side at z = 0) starts poisoned in Hard mode.
     */
    public static boolean xarpusHardEdge(int x, int z, int half) {
        int ring = half - Math.max(Math.abs(x), Math.abs(z));
        if (ring < 0 || ring >= XARPUS_HARD_EDGE_RINGS) {
            return false;
        }
        boolean entrance = x <= -half + 1 && Math.abs(z) <= 1;
        return !entrance;
    }

    /** Hard mode phase 3: a successful hit turns him to stare at the attacker's quadrant at once (OSRS wiki). */
    public static int xarpusHardStare(int current, int attackerQuadrant, boolean hit) {
        return hit ? attackerQuadrant : current;
    }

    // ------------------------------------------------------------------ Verzik Vitur

    /** Phase 1 caps each hit at 10 for melee and 3 for ranged and magic, except the Dawnbringer (Mod Ash). */
    public static int verzikP1Cap(int damage, boolean melee, boolean dawnbringer) {
        if (dawnbringer) {
            return damage;
        }
        return Math.min(damage, melee ? 10 : 3);
    }

    /** Phase 1's blast every 14 ticks: up to 137, halved by Protect from Magic (OSRS wiki). */
    public static final int VERZIK_P1_BLAST_MAX = 137;
    public static final int VERZIK_P1_BLAST_TICKS = 14;
    /**
     * The Dawnbringer (OSRS wiki): a powered staff with an attack range of 7 tiles; its special (Pulsate) always hits
     * Verzik's shield for 75-150. It drops to the floor when its holder dies or leaves, so a teammate can use it.
     */
    public static final int DAWNBRINGER_RANGE_TILES = 7;
    public static final int DAWNBRINGER_SPEC_MIN = 75;
    public static final int DAWNBRINGER_SPEC_MAX = 150;
    /** Hard mode phase 1 (OSRS wiki): a blast striking a pillar drops three pieces of debris within 3 tiles, up to 35 and a stun. */
    public static final int VERZIK_HARD_DEBRIS = 3;
    public static final int VERZIK_HARD_DEBRIS_MAX = 35;
    public static final int VERZIK_HARD_DEBRIS_TILES = 3;
    /**
     * Phase 2 (OSRS wiki "Verzik Vitur" and its infobox): attacks every 4 ticks; urnbombs up to 44 (22 prayed), the body
     * slam ("bounce") 50, the stomp under her ~80 (82 on the Strategies page).
     */
    public static final int VERZIK_P2_SPEED = 4;
    public static final int VERZIK_URN_MAX = 44;
    public static final int VERZIK_SLAM_MAX = 50;
    public static final int VERZIK_STOMP_MAX = 82;
    /** Hard mode urnbombs hit about 20% harder and leave a poison pool for 16 ticks: 9-10 a tick and poison (OSRS wiki). */
    public static final double VERZIK_HARD_URN_MULT = 1.2;
    public static final int VERZIK_HARD_POOL_TICKS = 16;
    public static final int VERZIK_HARD_POOL_MIN = 9;
    public static final int VERZIK_HARD_POOL_MAX = 10;

    /** An urnbomb's maximum by mode. */
    public static int verzikUrnMax(RaidMode mode) {
        return mode == RaidMode.HARD ? (int) Math.round(VERZIK_URN_MAX * VERZIK_HARD_URN_MULT) : VERZIK_URN_MAX;
    }

    /**
     * Every fifth attack is a lightning ball: up to 48 to a player (25 with insulated boots), 15-20 to Verzik when it
     * passes through her.
     */
    public static final int VERZIK_LIGHTNING_EVERY = 5;
    public static final int VERZIK_LIGHTNING_MAX = 48;
    public static final int VERZIK_LIGHTNING_INSULATED_MAX = 25;
    /**
     * The phase 2 and 3 nylocas home in on a player and explode for up to 63 when they reach their target (OSRS wiki
     * "Verzik Vitur", which settles the dispute with the Strategies page's 46), and die after ~15 s (25 ticks). Players up
     * to three tiles away take up to 8 (Strategies page; // approx: the only figure for the splash).
     */
    public static final int VERZIK_NYLO_MAX = 63;
    public static final int VERZIK_NYLO_FAR = 8;
    public static final int VERZIK_NYLO_TICKS = 25;
    /**
     * Hard mode (OSRS wiki): the nylocas never time out (they pop only when killed or on reaching their target) and change
     * colour "from green to blue to grey" (ranged, magic, melee) at set intervals. // approx: every 10 ticks, round again
     */
    public static final int VERZIK_HARD_NYLO_CYCLE_TICKS = 10;
    /**
     * The Nylocas Athanatos is thrown from Verzik at a random player: up to 78 on the tile it lands on, less one tile away
     * (OSRS wiki "Nylocas Athanatos"; // approx: half). It heals her 9-10 every few ticks; a poison or venom hit (or a
     * Smoke spell's poison) bursts it for up to 70 on her. Raw damage barely hurts it: it regenerates and comes back when
     * killed without poison (// approx: a quarter of the damage, 5 hitpoints a tick, back after 10 ticks).
     */
    public static final int ATHANATOS_LANDING_MAX = 78;
    public static final int ATHANATOS_LANDING_DELAY = 4;
    public static final int ATHANATOS_HEAL_MIN = 9;
    public static final int ATHANATOS_HEAL_MAX = 10;
    public static final int ATHANATOS_POISON_MAX = 70;
    public static final int ATHANATOS_RAW_DIVISOR = 4;
    public static final int ATHANATOS_REGEN = 5;
    public static final int ATHANATOS_RESPAWN_TICKS = 10;

    /** Raw (non-poison) damage the Athanatos takes. */
    public static int athanatosRawDamage(int damage) {
        return Math.max(0, damage) / ATHANATOS_RAW_DIVISOR;
    }

    /** Smoke spells poison on 1 in 8 successful hits (OSRS wiki "Smoke spells"); that poison bursts the Athanatos. */
    public static final int SMOKE_POISON_CHANCE = 8;

    /** Whether a combat spell id is one of the Ancient Smoke spells. */
    public static boolean smokeSpell(String spellId) {
        return spellId != null && spellId.startsWith("smoke_");
    }

    /** Phase 2.5 below 35%: 75% of attacks are her healing spell, up to 45 (halved by prayer), healing half (Mod Ash). */
    public static final double VERZIK_P25_AT = 0.35;
    public static final double VERZIK_HEAL_SPELL_CHANCE = 0.75;
    public static final int VERZIK_HEAL_SPELL_MAX = 45;
    /**
     * Phase 2.5 (OSRS wiki): she periodically summons two Nylocas Matomenos (one when only one player is left alive); damage
     * in the first three seconds (5 ticks) heals her; when they persist she kills them off and heals by the hitpoints
     * they have left. Hers have 200 hitpoints. In Hard mode the absorbed ones explode on nearby players for their health.
     * // approx: a new pair every 24 ticks, the old pair absorbed then
     */
    public static final int VERZIK_MATOMENOS_HP = 200;
    public static final int VERZIK_MATOMENOS_INTERVAL = 24;
    public static final int VERZIK_SUMMON_TICKS = 5;

    /** Matomenos per phase 2.5 summon: two, or one when only one player is left alive (OSRS, May 2024). */
    public static int verzikMatomenos(int playersAlive) {
        return playersAlive <= 1 ? 1 : 2;
    }

    /** A Hard mode Matomenos she absorbs explodes on players next to it for its remaining health. // approx: capped at 50 */
    public static int verzikMatomenosBlast(int hitpoints) {
        return Math.max(0, Math.min(50, hitpoints));
    }

    /** After every third lightning ball there is a 25% chance she summons nylocas again (OSRS wiki). */
    public static final double VERZIK_RESUMMON_CHANCE = 0.25;
    /** Phase 3 (OSRS wiki): attacks every 7 ticks (5 when enraged); projectiles up to 33 (16 prayed), melee 63. */
    public static final int VERZIK_P3_SPEED = 7;
    public static final int VERZIK_P3_ENRAGED_SPEED = 5;
    public static final int VERZIK_P3_PROJECTILE_MAX = 33;
    public static final int VERZIK_P3_MELEE_MAX = 63;
    /** Phase 3 specials, one after every four attacks, in this order (OSRS wiki). */
    public enum P3Special {
        NYLOCAS, WEBS, YELLOWS, GREEN_BALL
    }

    public static final List<P3Special> P3_ROTATION = List.of(P3Special.NYLOCAS, P3Special.WEBS, P3Special.YELLOWS, P3Special.GREEN_BALL);
    public static final int VERZIK_P3_ATTACKS_PER_SPECIAL = 4;
    /** Yellows: up to 80 off a pool; a pool shelters one player (OSRS wiki). */
    public static final int VERZIK_YELLOW_MAX = 80;
    /**
     * The green ball hits for 75% of the Hitpoints level, 74 at level 99 (OSRS wiki "Theatre of Blood/Strategies"; the
     * "Verzik Vitur" page's "up to 74%" is that 74 at 99: 74% would be 73).
     */
    public static int greenBall(int hitpointsLevel) {
        return hitpointsLevel * 75 / 100;
    }

    /**
     * Hard mode green ball (OSRS wiki): unbounced it kills outright while two or more players are in the arena (a lone
     * player takes the Normal mode hit); bounced it deals 89, 74 and 49 after one, two and three bounces.
     */
    public static int greenBallHard(int hitpointsLevel, int bounces, int playersInArena, int currentHp) {
        if (bounces <= 0) {
            return playersInArena >= 2 ? Math.max(currentHp, 1) + 1 : greenBall(hitpointsLevel);
        }
        return switch (bounces) {
            case 1 -> 89;
            case 2 -> 74;
            default -> 49;
        };
    }

    /**
     * Webs: 10 hitpoints; a web that isn't broken in time snaps for heavy damage; aranea boots negate it once every 8 ticks
     * (OSRS wiki). // approx: after 8 ticks, 40-60
     */
    public static final int WEB_SNAP_TICKS = 8;
    public static final int WEB_SNAP_MIN = 40;
    public static final int WEB_SNAP_MAX = 60;
    public static final int WEB_ARANEA_COOLDOWN = 8;
    /** Hard mode yellows (OSRS wiki): the charged blast strikes three times per player, with three pools to stand on. */
    public static int yellowVolleys(RaidMode mode) {
        return mode == RaidMode.HARD ? 3 : 1;
    }

    /** Ticks between the Hard mode yellow strikes. // approx */
    public static final int YELLOW_VOLLEY_GAP = 3;
    /**
     * Enrage at 20%: projectiles hit up to 34, each player gets a purple tornado (50% of current hitpoints, at least 5,
     * healing her three times that) that comes back 16 ticks after it hits (OSRS wiki).
     */
    public static final double VERZIK_ENRAGE_AT = 0.2;
    public static final int VERZIK_ENRAGED_PROJECTILE_MAX = 34;
    public static final int TORNADO_RESPAWN_TICKS = 16;
    public static final int TORNADO_MIN = 5;

    /** A tornado's hit: half the player's current hitpoints, at least 5. */
    public static int tornadoHit(int currentHp) {
        return Math.max(TORNADO_MIN, currentHp / 2);
    }

    /** Hard mode (OSRS wiki): the first time she falls to 5% or lower she heals 30% of her hitpoints ("You think this is over?"). */
    public static final double VERZIK_HARD_LAST_STAND_AT = 0.05;
    public static final double VERZIK_HARD_LAST_STAND_HEAL = 0.30;
}
