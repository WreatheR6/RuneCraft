package com.runecraft.osrs.content.raids;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Pure rules of the Tombs of Amascut's bosses that the invocations don't cover (OSRS wiki boss pages and
 * "Tombs of Amascut/Strategies"): Akkha's quadrants, Memory Blast, shadows and final stand; Ba-Ba's boulders; Zebak's
 * specials, attack speed and waves; the Wardens' phases. Values the wiki doesn't give are marked approx.
 */
public final class ToaBossRules {
    private ToaBossRules() {
    }

    // ------------------------------------------------------------------ Akkha

    /** Akkha's quadrants in clockwise order: north-west (fire), north-east (shadow), south-east (lightning), south-west (ice). */
    public static final String[] QUADRANTS = {"fire", "shadow", "lightning", "ice"};

    /** Health thresholds (percent) at which Akkha's shadows appear or are restored. */
    public static final int[] SHADOW_THRESHOLDS = {80, 60, 40, 20};

    /**
     * Memory Blast's safe quadrants in order (OSRS): each lit quadrant is next to the previous one (never the opposite
     * corner, never the same one twice).
     */
    public static List<Integer> memoryBlastOrder(int rounds, Random random) {
        List<Integer> out = new ArrayList<>();
        int q = random.nextInt(4);
        out.add(q);
        while (out.size() < rounds) {
            q = Math.floorMod(q + (random.nextBoolean() ? 1 : -1), 4);
            out.add(q);
        }
        return out;
    }

    /** Memory Blast iterations (OSRS: one more for every two path levels, up to four levels). // approx: base four */
    public static int memoryBlastRounds(int pathLevel) {
        return 4 + Math.min(4, Math.max(0, pathLevel)) / 2;
    }

    /** Successful hits that make Akkha swap corners in his final stand (OSRS: three, plus one per extra player). */
    public static int akkhaSwapHits(int partySize) {
        return 3 + Math.max(0, partySize - 1);
    }

    /** Share of Akkha's maximum health he restores for his final stand (OSRS: 20%). */
    public static final int AKKHA_FINAL_STAND_PERCENT = 20;

    /** Unstable orbs' damage (OSRS: grows with raid and path level, capped at 25). // approx: 12 at raid level 0 */
    public static int unstableOrbDamage(double raidDamageMultiplier, int pathLevel) {
        double d = 12 * raidDamageMultiplier * (1 + 0.1 * (Math.min(4, Math.max(0, pathLevel)) / 2));
        return Math.min(25, (int) Math.round(d));
    }

    /** Share of Akkha's ranged/magic damage protection prayers let through (OSRS: they block about 80-90%). */
    public static final double AKKHA_PROTECTED_RANGED_MAGIC = 0.15;
    /** ... and of his melee (OSRS: about 75% blocked). */
    public static final double AKKHA_PROTECTED_MELEE = 0.25;
    /** Game ticks without being able to attack after which Akkha changes style anyway (OSRS: a minute). */
    public static final int AKKHA_STUCK_TICKS = 100;

    // ------------------------------------------------------------------ Ba-Ba

    /** Rolling boulder hitpoints (OSRS, solo: 25, 27 at path level 2, 31 at path level 4). */
    public static int babaBoulderHp(int pathLevel) {
        int p = Math.max(0, pathLevel);
        return 25 + (p >= 2 ? 2 : 0) + (p >= 4 ? 4 : 0);
    }

    /** Sets of rolling boulders before Ba-Ba stops (OSRS: ten). */
    public static final int BOULDER_SETS = 10;
    /** Boulders in each set, one of them cracked (OSRS: five). */
    public static final int BOULDERS_PER_SET = 5;
    /** Debris hitpoints: 10 a player in the raid. // approx */
    public static int debrisHp(int partySize) {
        return 10 * Math.max(1, partySize);
    }

    /** Damage a rock throw does to what a player hides next to (OSRS): debris 10, an unopened sarcophagus 30, a baboon 12. */
    public static final int THROW_ON_DEBRIS = 10;
    public static final int THROW_ON_SARCOPHAGUS = 30;
    public static final int THROW_ON_BABOON = 12;
    /** Hitpoints of a sarcophagus a baboon hacks at (20 a hit, OSRS). // approx: 60 */
    public static final int SARCOPHAGUS_HP = 60;

    /** Game ticks between Ba-Ba's rockfalls: much quicker every two path levels (OSRS). // approx: 18 / 14 / 10 */
    public static int babaRockfallEvery(int pathLevel) {
        return 18 - 4 * (Math.min(4, Math.max(0, pathLevel)) / 2);
    }

    // ------------------------------------------------------------------ Zebak

    /** Health thresholds (percent) at which Zebak queues a special: Great Roar and Tidal Waves alternately (OSRS). */
    public static final int[] ZEBAK_SPECIALS = {85, 70, 55, 40};
    /** Zebak enrages below 25%: faster attacks and no more specials (OSRS). */
    public static final int ZEBAK_ENRAGE = 25;

    /** Zebak's attack interval in game ticks: 7, or 4 enraged, one tick faster every two path levels (OSRS). */
    public static int zebakAttackSpeed(boolean enraged, int pathLevel) {
        int base = enraged ? 4 : 7;
        return Math.max(2, base - Math.min(4, Math.max(0, pathLevel)) / 2);
    }

    /** Tidal Waves' safe gap in tiles: one tile tighter every two path levels (OSRS). // approx: 5 at path level 0 */
    public static int zebakWaveGap(int pathLevel) {
        return Math.max(2, 5 - Math.min(4, Math.max(0, pathLevel)) / 2);
    }

    /** Hitpoints of a rock Zebak throws for his Great Roar; each roar hit takes 50 off it (OSRS). */
    public static final int ZEBAK_ROCK_HP = 150;
    public static final int ZEBAK_ROAR_ON_ROCK = 50;
    /** Zebak's melee max hit and his ranged/magic max hits (OSRS infobox: 38, 16, 16). */
    public static final int ZEBAK_MELEE_MAX = 38;
    public static final int ZEBAK_RANGED_MAX = 16;

    // ------------------------------------------------------------------ the Wardens

    /** Phase 3 slam interval: 7 ticks, 6/4/3 with Overclocked, Overclocked 2 and Insanity (OSRS). */
    public static int wardenSlamEvery(java.util.Set<Invocation> invocations) {
        if (invocations.contains(Invocation.INSANITY)) {
            return 3;
        }
        if (invocations.contains(Invocation.OVERCLOCKED_2)) {
            return 4;
        }
        return invocations.contains(Invocation.OVERCLOCKED) ? 6 : 7;
    }

    /** Phase 3 health thresholds (percent) at which the Warden launches energy siphons (OSRS). */
    public static final int[] SIPHON_THRESHOLDS = {80, 60, 40, 20};

    /** Energy siphon hitpoints (OSRS): 1 up to three players, 2 with four or five, 3 with six or seven, 4 with eight. */
    public static int siphonHp(int playersInArena) {
        int n = Math.max(1, playersInArena);
        return n <= 3 ? 1 : n <= 5 ? 2 : n <= 7 ? 3 : 4;
    }

    /** Siphons in a set (OSRS: a solo and a group layout). // approx: six solo, nine in groups */
    public static int siphonCount(int playersInArena) {
        return playersInArena <= 1 ? 6 : 9;
    }

    /** Game ticks to reverse every siphon before they detonate (OSRS: less with Insanity, more lenient in groups). // approx */
    public static int siphonTicks(int playersInArena, boolean insanity) {
        int base = playersInArena <= 1 ? 16 : 20;
        return insanity ? base - 4 : base;
    }

    /** Share of the Warden's maximum health that reversed siphons deal back to it. // approx: 2% each */
    public static final double SIPHON_DAMAGE_EACH = 0.02;
    /** The Warden's one-time heal at 5% before its final enrage (OSRS: 20% of its maximum). */
    public static final int WARDEN_ENRAGE_AT = 5;
    public static final int WARDEN_ENRAGE_HEAL = 20;

    /** Health (percent) above which the phase 2 Warden fires its prayer-disabling Divine Projectile, below it Imprisonment. */
    public static final int WARDEN_P2_SPECIAL_SPLIT = 55;
}
