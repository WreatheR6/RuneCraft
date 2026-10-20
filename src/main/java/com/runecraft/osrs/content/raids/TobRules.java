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
        int base = switch (p) {
            case ABOVE_AVERAGE -> 10;
            case AVERAGE -> 8;
            case BELOW_AVERAGE -> 6;
        };
        return base + random.nextInt(4);
    }

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

    /** One player gets two more blood splats in a 5x5 area around them (OSRS). */
    public static final int MAIDEN_EXTRA_SPLATS = 2;
    /** A blood splat turns into a blood spawn 10% of the time, 20% if someone stood on it (OSRS wiki). */
    public static double bloodSpawnChance(boolean steppedOn) {
        return steppedOn ? 0.2 : 0.1;
    }

    /** Blood spawns' trails dry in about 20-30 seconds (OSRS wiki). // approx: 40 ticks */
    public static final int MAIDEN_TRAIL_TICKS = 40;
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
        return 15 + (int) (currentHp * 0.0667);
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
    /** Phase 2 (OSRS wiki): attacks every 4 ticks; urnbombs up to 44 (22 prayed), body slam 45, stomp 82. */
    public static final int VERZIK_P2_SPEED = 4;
    public static final int VERZIK_URN_MAX = 44;
    public static final int VERZIK_SLAM_MAX = 45;
    public static final int VERZIK_STOMP_MAX = 82;
    /** Every fifth attack is a lightning ball: up to 48 to a player, 15-20 to Verzik when it passes through her. */
    public static final int VERZIK_LIGHTNING_EVERY = 5;
    public static final int VERZIK_LIGHTNING_MAX = 48;
    /** The phase 2 nylocas explode for up to 46 next to a player, up to 8 three tiles away, and die after ~15 s. */
    public static final int VERZIK_NYLO_MAX = 46;
    public static final int VERZIK_NYLO_FAR = 8;
    public static final int VERZIK_NYLO_TICKS = 25;
    /** The Nylocas Athanatos heals her 9-10 every few ticks; poison makes it deal up to 70 to her instead (OSRS wiki). */
    public static final int ATHANATOS_HEAL_MIN = 9;
    public static final int ATHANATOS_HEAL_MAX = 10;
    public static final int ATHANATOS_POISON_MAX = 70;
    /** Phase 2.5 below 35%: 75% of attacks are her healing spell, up to 45 (halved by prayer), healing half (Mod Ash). */
    public static final double VERZIK_P25_AT = 0.35;
    public static final double VERZIK_HEAL_SPELL_CHANCE = 0.75;
    public static final int VERZIK_HEAL_SPELL_MAX = 45;
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
    /** The green ball hits for 75% of the Hitpoints level (OSRS wiki). */
    public static int greenBall(int hitpointsLevel) {
        return hitpointsLevel * 75 / 100;
    }

    /** Webs: 10 hitpoints; a web that isn't broken in time snaps for heavy damage. // approx: after 8 ticks, 40-60 */
    public static final int WEB_SNAP_TICKS = 8;
    public static final int WEB_SNAP_MIN = 40;
    public static final int WEB_SNAP_MAX = 60;
    /** Enrage at 20%: projectiles hit up to 34, each player gets a purple tornado (50% of current hitpoints, healing 3x). */
    public static final double VERZIK_ENRAGE_AT = 0.2;
    public static final int VERZIK_ENRAGED_PROJECTILE_MAX = 34;
    public static final int TORNADO_RESPAWN_TICKS = 16;
}
