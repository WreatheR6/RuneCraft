package com.runecraft.osrs.content.raids;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Party size, mode and raid level scaling (pure maths, unit tested). The CoX, ToB and ToA hitpoint
 * formulas are the OSRS ones (as used by the wiki's DPS calculator, assuming a maxed party); other
 * values are approximations and are marked as such.
 */
public final class RaidScaling {
    /** CoX personal points cap (OSRS: 131,071). */
    public static final int COX_POINTS_CAP = 131_071;
    /** CoX team points per unique roll: a roll with this many points has its maximum 65.7% chance. */
    public static final int COX_POINTS_PER_ROLL = 570_000;
    /** CoX team points per 1% unique chance (OSRS wiki: 8,675). */
    public static final int COX_POINTS_PER_PERCENT = 8_675;
    /** Most unique rolls a CoX raid gets: one per 570,000 team points, up to 6 (OSRS wiki loot calculator). */
    public static final int COX_MAX_UNIQUES = 6;
    /** CoX damage points: each creature holds a pool of 5 points per hitpoint (OSRS). */
    public static final int COX_DAMAGE_POOL_PER_HP = 5;
    /** CoX tightrope room: the crystal at the far end is worth 2,000 points (OSRS). */
    public static final int COX_TIGHTROPE_POINTS = 2_000;
    /** CoX crystal crabs room: 1,600 points for lighting every crystal (OSRS), split per crystal. */
    public static final int COX_CRABS_POINTS = 1_600;
    /** Game ticks after one Olm claw dies within which the other must die, or the dead one regrows. // approx */
    public static final int OLM_CLAW_REVIVE_TICKS = 20;
    /** Game ticks the Great Olm's left (melee) claw stays clenched unless the right claw dies first. // approx */
    public static final int OLM_CLENCH_TICKS = 16;
    /** ToA personal points cap (OSRS: 64,000). */
    public static final int TOA_POINTS_CAP = 64_000;
    /** ToB points a surviving player earns per room (6 rooms = 18 points, as in OSRS). */
    public static final int TOB_POINTS_PER_ROOM = 3;
    /** ToB bonus points for each room's MVP (OSRS: 14 MVP points per raid; 2 per room, 4 for Verzik). // approx split */
    public static final int TOB_MVP_POINTS = 2;
    public static final int TOB_VERZIK_MVP_POINTS = 4;
    /** ToB points lost per death (OSRS: 4). */
    public static final int TOB_DEATH_PENALTY = 4;
    /** ToA points every surviving player earns per room cleared (OSRS: 300 per room, 9 rooms). */
    public static final int TOA_ROOM_POINTS = 300;
    /** Highest ToA path level (OSRS: 6). */
    public static final int TOA_MAX_PATH_LEVEL = 6;

    /** ToB Entry mode base hitpoints (OSRS entry-mode variants; 5-player values). */
    public static final Map<String, Integer> TOB_ENTRY_HP = Map.of(
            "the_maiden_of_sugadinti", 2000,
            "pestilent_bloat", 1280,
            "nylocas_vasilias", 1440,
            "sotetseg", 2240,
            "xarpus", 3400,
            "verzik_vitur", 1200,
            "verzik_vitur_p2", 1600,
            "verzik_vitur_p3", 2400);

    /**
     * Challenge Mode target time for the twisted ancestral colour kit. OSRS scales it by team size;
     * this mod's rooms are smaller, so one generous target is used. // approx
     */
    public static final int COX_CM_TARGET_MINUTES = 50;
    /** ToB Hard mode challenge time for the ornament kits, sanguine dust and the larger chest. // approx */
    public static final int TOB_HM_TARGET_MINUTES = 35;

    private RaidScaling() {
    }

    /** The mode's target time in game ticks (0.6 s), or {@link Long#MAX_VALUE} when the mode has none. */
    public static long targetTicks(RaidType type, RaidMode mode, int partySize) {
        if (type == RaidType.COX && mode == RaidMode.CHALLENGE) {
            return COX_CM_TARGET_MINUTES * 100L;
        }
        if (type == RaidType.TOB && mode == RaidMode.HARD) {
            return TOB_HM_TARGET_MINUTES * 100L;
        }
        return Long.MAX_VALUE;
    }

    private static int clampParty(int partySize, int max) {
        return Math.max(1, Math.min(max, partySize));
    }

    /** Integer square root. */
    static int isqrt(int n) {
        int r = (int) Math.sqrt(Math.max(0, n));
        while (r * r > n) {
            r--;
        }
        while ((r + 1) * (r + 1) <= n) {
            r++;
        }
        return r;
    }

    // ------------------------------------------------------------------ Chambers of Xeric

    /**
     * CoX monster hitpoints multiplier (OSRS): {@code hp += hp * floor(partySize / 2)}, i.e. x1 solo,
     * x2 for 2-3 players, x3 for 4-5 and so on (party size capped at 100).
     */
    public static double coxHp(int partySize) {
        return 1 + clampParty(partySize, 100) / 2;
    }

    /** Party-size factor of the Great Olm's hitpoints: each extra phase (per 8 players) takes 3 players' worth. */
    public static int olmPartyFactor(int partySize) {
        int n = clampParty(partySize, 100);
        return Math.min(n - 1, 50) - 3 * (Math.min(n, 50) / 8);
    }

    /** Great Olm phases (OSRS): 3, plus one for every 8 players. */
    public static int olmPhases(int partySize) {
        return 3 + Math.min(clampParty(partySize, 100), 50) / 8;
    }

    /**
     * Scaled hitpoints of a CoX creature (OSRS formula for a maxed party). The Great Olm's head has
     * 800 + 400 per party factor and each claw 600 + 300, unaffected by Challenge Mode; everything
     * else is multiplied by {@link #coxHp(int)} and gets +50% in Challenge Mode (except Vasa's crystals).
     */
    public static int coxScaledHp(String id, int baseHp, int partySize, boolean challenge) {
        int n = clampParty(partySize, 100);
        if (id.equals("great_olm")) {
            return 800 + 400 * olmPartyFactor(n);
        }
        if (id.equals("olm_left_claw") || id.equals("olm_right_claw")) {
            return 600 + 300 * olmPartyFactor(n);
        }
        if (COX_SINGLES.contains(id)) {
            // Solo creatures (vespine soldiers) ignore party size; Challenge Mode raises the scaler by 50%.
            return Math.max(5, challenge ? baseHp * addPercent(126, 50) / 126 : baseHp);
        }
        int hp = baseHp + baseHp * (n * 50 / 100);
        if (challenge && !id.equals("vasa_crystal")) {
            hp += hp * 50 / 100;
        }
        return Math.max(1, Math.min(30_000, hp));
    }

    /** CoX creatures scaled as solo fights (OSRS: scavenger beasts and vespine soldiers). */
    static final Set<String> COX_SINGLES = Set.of("vespine_soldier");
    /** CoX creatures whose Magic level is defensive and scales like Defence (OSRS). */
    static final Set<String> COX_MAGIC_DEFENSIVE = Set.of("tekton", "vespula", "vespula_portal", "deathly_ranger",
            "vespine_soldier", "olm_left_claw", "olm_right_claw");

    private static int addPercent(int x, int pct) {
        return x + x * pct / 100;
    }

    /** Whether a CoX creature's Magic level scales with {@link #coxScaledDefence} (otherwise it is offensive). */
    public static boolean coxMagicIsDefensive(String id) {
        return COX_MAGIC_DEFENSIVE.contains(id);
    }

    /**
     * Scaled Defence level of a CoX creature for a maxed party (OSRS, as in the wiki DPS calculator): +1% per
     * integer square root of the extra players plus 0.7% per extra player, then +50% in Challenge Mode (Tekton
     * +20% below 4 players, +35% from 4; Vasa's crystals unchanged), at least 50. Solo creatures only get the
     * Challenge Mode bonus. Levels of 1 stay 1.
     */
    public static int coxScaledDefence(String id, int baseLevel, int partySize, boolean challenge) {
        if (baseLevel <= 1) {
            return baseLevel;
        }
        if (COX_SINGLES.contains(id)) {
            return Math.max(1, challenge ? baseLevel * addPercent(99, 50) / 99 : baseLevel);
        }
        int n = clampParty(partySize, 100);
        int m1 = n - 1;
        int def = baseLevel * (100 + isqrt(m1) + m1 * 7 / 10) / 100;
        if (challenge && !id.equals("vasa_crystal")) {
            def = addPercent(def, id.equals("tekton") ? (n < 4 ? 20 : 35) : 50);
        }
        return Math.max(50, Math.min(20_000, def));
    }

    /**
     * CoX offensive stat multiplier (OSRS): +7% per integer square root of the extra players plus 1%
     * per extra player, and +50% in Challenge Mode. Applied to the damage CoX creatures deal.
     */
    public static double coxOffence(int partySize, boolean challenge) {
        int m1 = clampParty(partySize, 100) - 1;
        double pct = (100 + isqrt(m1) * 7 + m1) / 100.0;
        return challenge ? pct * 1.5 : pct;
    }

    /** CoX purple chance of one roll: 1% per 8,675 points, capped at 570,000 points (65.7%). */
    public static double coxPurpleChance(int totalPoints) {
        return Math.min(Math.max(0, totalPoints), COX_POINTS_PER_ROLL) / (COX_POINTS_PER_PERCENT * 100.0);
    }

    /**
     * The chance of each CoX unique roll: team points are split into 570,000-point rolls (the last
     * one takes the remainder), up to {@link #COX_MAX_UNIQUES}.
     */
    public static List<Double> coxUniqueRolls(int totalPoints) {
        List<Double> rolls = new ArrayList<>();
        int left = Math.max(0, totalPoints);
        do {
            int chunk = Math.min(left, COX_POINTS_PER_ROLL);
            rolls.add(coxPurpleChance(chunk));
            left -= chunk;
        } while (left > 0 && rolls.size() < COX_MAX_UNIQUES);
        return rolls;
    }

    /**
     * CoX points for one hit (OSRS): every creature starts with a pool of 5 points per hitpoint; a hit
     * awards {@code max(1, min(5 * damage, pool * damage / (hp / 2)))}, so damage is worth full points
     * while the pool is fresh and less and less as it drains (about 4.15 points per hitpoint overall).
     * The caller subtracts the result from the creature's pool.
     */
    public static int coxDamagePoints(int pool, int maxHp, int damage) {
        if (damage <= 0 || pool <= 0) {
            return 0;
        }
        long half = Math.max(1, maxHp / 2);
        long decayed = (long) pool * damage / half;
        return (int) Math.min(pool, Math.max(1, Math.min(5L * damage, decayed)));
    }

    /** Skeletal Mystics in their room (OSRS): 3, plus one per 3 players, up to 12. */
    public static int coxMysticCount(int partySize) {
        return Math.min(12, clampParty(partySize, 100) / 3 + 3);
    }

    /** Lizardman shamans in their room (OSRS): 2, plus one per 5 players, up to 5. */
    public static int coxShamanCount(int partySize) {
        return Math.min(5, clampParty(partySize, 100) / 5 + 2);
    }

    /** Ice demon room: the braziers give up to max(6,000, 10 x the party's total Firemaking levels) points (OSRS). */
    public static int coxIceDemonPointsCap(int totalFiremakingLevels) {
        return Math.max(6_000, 10 * Math.max(0, totalFiremakingLevels));
    }

    /** Thieving room: the trough gives up to max(4,500, 150 per 6 total Thieving levels) points (OSRS). */
    public static int coxThievingPointsCap(int totalThievingLevels) {
        return Math.max(4_500, Math.max(0, totalThievingLevels) / 6 * 150);
    }

    /** Highest pickaxe level that counts against the Guardians (OSRS: the crystal pickaxe hits like a dragon one). */
    public static final int COX_GUARDIAN_PICKAXE_CAP = 61;

    /**
     * Damage a pickaxe hit deals to a CoX Guardian (OSRS wiki): damage x (50 + Mining level + pickaxe level) / 150,
     * the pickaxe's level requirement capped at {@link #COX_GUARDIAN_PICKAXE_CAP}.
     */
    public static int coxGuardianDamage(int damage, int miningLevel, int pickaxeLevel) {
        if (damage <= 0) {
            return 0;
        }
        int p = Math.max(1, Math.min(COX_GUARDIAN_PICKAXE_CAP, pickaxeLevel));
        return damage * (50 + Math.max(1, miningLevel) + p) / 150;
    }

    /** Mining level a pickaxe needs (OSRS), by item id; 1 for unknown pickaxes. */
    public static int pickaxeLevel(String itemId) {
        if (itemId == null) {
            return 1;
        }
        String id = itemId.replace("_pickaxe", "");
        return switch (id) {
            case "steel" -> 6;
            case "black" -> 11;
            case "mithril" -> 21;
            case "adamant" -> 31;
            case "rune", "gilded" -> 41;
            case "dragon", "dragon_or", "infernal", "3rd_age", "third_age" -> 61;
            case "crystal" -> 71;
            default -> id.startsWith("dragon") || id.startsWith("infernal") ? 61 : id.startsWith("crystal") ? 71 : 1;
        };
    }

    /**
     * CoX Guardian hitpoints (OSRS wiki): 151 x (1 + floor(team / 2)) + floor(average Mining level) x team, so 250
     * for a solo player with 99 Mining. Challenge Mode adds 50%.
     */
    public static int coxGuardianHp(int partySize, int averageMining) {
        int n = clampParty(partySize, 100);
        return 151 * (1 + n / 2) + Math.max(1, averageMining) * n;
    }

    /** The Great Olm's left claw clenches after a hit of at least 1/20 of its health (OSRS). */
    public static int olmClenchThreshold(int clawMaxHp) {
        return Math.max(1, clawMaxHp / 20);
    }

    /** CoX: a death costs 40% of your personal points. */
    public static int coxDeathPenalty(int points) {
        return points - points * 40 / 100;
    }

    // ------------------------------------------------------------------ Theatre of Blood

    /** ToB boss hitpoints (OSRS): (party + 3) / 8 with the party clamped to 3-5, i.e. 100% / 87.5% / 75%. */
    public static double tobHp(int partySize) {
        int n = Math.max(3, Math.min(5, partySize));
        return (n + 3) / 8.0;
    }

    /**
     * Scaled ToB hitpoints. Normal and Hard mode use {@link #tobHp(int)}; Entry mode swaps in the
     * entry-mode base hitpoints and scales them by 10/19/27/34/40 fortieths for 1-5 players (OSRS).
     */
    public static int tobScaledHp(String id, int baseHp, int partySize, RaidMode mode) {
        if (mode == RaidMode.ENTRY) {
            int[] f = {10, 19, 27, 34, 40};
            // Creatures without a known entry-mode variant get 60% of their normal hitpoints. // approx
            int base = TOB_ENTRY_HP.getOrDefault(id, baseHp * 6 / 10);
            return Math.max(1, base * f[clampParty(partySize, 5) - 1] / 40);
        }
        if (mode == RaidMode.HARD) {
            baseHp = TOB_HARD_HP.getOrDefault(id, baseHp);
        }
        return Math.max(1, (int) (baseHp * tobHp(partySize)));
    }

    /**
     * ToB Hard mode base hitpoints that differ from normal mode (OSRS wiki / DPS calculator 5-player values):
     * the Pestilent Bloat has 20% more (2,000 -> 2,400) and Xarpus 5,000 -> 6,000. The others are unchanged.
     */
    public static final Map<String, Integer> TOB_HARD_HP = Map.of(
            "pestilent_bloat", 2400,
            "xarpus", 6000);

    /**
     * ToB Hard mode combat level changes (OSRS wiki): Sotetseg's Attack 250 -> 350 and Xarpus's Defence
     * 250 -> 200. Returns {attack, strength, defence, magic, ranged}, -1 meaning unchanged, or null.
     */
    public static int[] tobHardLevels(String id) {
        return switch (id) {
            case "sotetseg" -> new int[]{350, -1, -1, -1, -1};
            case "xarpus" -> new int[]{-1, -1, 200, -1, -1};
            default -> null;
        };
    }

    /**
     * ToB Hard mode max hit multiplier (OSRS wiki): Nylocas Vasilias hits up to 105 instead of 70, and Verzik's
     * second form 44 instead of 47.
     */
    public static double tobHardMaxHit(String id) {
        return switch (id) {
            case "nylocas_vasilias" -> 105 / 70.0;
            case "verzik_vitur_p2" -> 44 / 47.0;
            default -> 1.0;
        };
    }

    /** Nylocas Matomenos the Maiden summons at 70%, 50% and 30% (OSRS): two per player up to 10; always 10 in Hard mode. */
    public static int tobMaidenNylocas(int partySize, boolean hard) {
        return hard ? 10 : Math.min(10, 2 * Math.max(1, partySize));
    }

    /** The Pestilent Bloat's first walk (OSRS: 39-47 game ticks; this is the minimum, plus 0-8). */
    public static final int TOB_BLOAT_FIRST_WALK = 39;

    /**
     * Players Vasa Nistirio's teleport pulls next to him (OSRS wiki: half the team, always the player when solo). The
     * rest land at the room's edges. // approx: rounding down for odd teams
     */
    public static int vasaTeleportTargets(int partySize) {
        return Math.max(1, partySize / 2);
    }

    /**
     * Damage of Vasa Nistirio's teleport barrage (OSRS wiki): the stunned players' current hitpoints added up, minus 5,
     * divided between everyone next to him when it lands (unstunned players there can pray Protect from Magic against
     * their share). // approx: the wiki states the minus 5 for one stunned player only
     */
    public static int vasaTeleportDamage(int[] stunnedHitpoints, int receivers) {
        int total = 0;
        for (int hp : stunnedHitpoints) {
            total += Math.max(0, hp);
        }
        return Math.max(0, total - 5) / Math.max(1, receivers);
    }

    /** Game ticks between Vasa's teleport and the barrage. // approx */
    public static final int VASA_TELEPORT_DELAY = 5;

    /** Verzik Vitur's phase 1 supporting pillars: blasts one absorbs before collapsing. // approx: "after several attacks" */
    public static final int VERZIK_PILLAR_BLASTS = 4;

    /** A collapsing Verzik pillar hits players next to it for up to 70 (50 in Entry mode) and stuns them (OSRS wiki). */
    public static int verzikPillarCollapse(RaidMode mode) {
        return mode == RaidMode.ENTRY ? 50 : 70;
    }
    /** The Bloat's later walks after each stomp (OSRS: 34-42 game ticks; minimum, plus 0-8). */
    public static final int TOB_BLOAT_WALK = 34;
    /** Game ticks the Bloat stays shut down before it stomps (OSRS: 19.2 seconds). */
    public static final int TOB_BLOAT_DOWN = 32;
    /** The Bloat can turn around once every 32 game ticks while walking (OSRS), every 16 in Hard mode. */
    public static final int TOB_BLOAT_TURN = 32;
    public static final int TOB_BLOAT_TURN_HARD = 16;
    /** Sotetseg's ball of death: up to 188 damage, split between the players it lands on (OSRS). */
    public static final int TOB_SOTETSEG_BALL = 188;

    /** ToB purple chance: 1/9.1 with every point, reduced proportionally by deaths (OSRS normal mode). */
    public static double tobPurpleChance(int teamPoints, int partySize) {
        return tobPurpleChance(teamPoints, partySize, RaidMode.NORMAL);
    }

    /** Most ToB points a party can hold (OSRS): 18 per player plus the 14 MVP points. */
    public static int tobMaxPoints(int partySize) {
        return Math.max(1, partySize) * 6 * TOB_POINTS_PER_ROOM + 5 * TOB_MVP_POINTS + TOB_VERZIK_MVP_POINTS;
    }

    /** ToB MVP bonus for a room (Verzik's is worth more). */
    public static int tobMvpPoints(String roomId) {
        return roomId.equals("verzik") ? TOB_VERZIK_MVP_POINTS : TOB_MVP_POINTS;
    }

    /** ToB purple chance by mode: 1/9.1 normal, 1/7.7 hard mode, none in entry mode (scaled by the share of points kept). */
    public static double tobPurpleChance(int teamPoints, int partySize, RaidMode mode) {
        if (mode == RaidMode.ENTRY) {
            return 0;
        }
        double base = mode == RaidMode.HARD ? 1 / 7.7 : 1 / 9.1;
        return tobPointsRatio(teamPoints, partySize) * base;
    }

    /** The share of the possible ToB points the party kept (deaths cost 4 each). */
    public static double tobPointsRatio(int teamPoints, int partySize) {
        return Math.max(0, Math.min(1.0, teamPoints / (double) tobMaxPoints(partySize)));
    }

    /**
     * ToB common quantity bounds (OSRS): {@code floor(min x modeMultiplier x pointsRatio)} up to 1.2 times
     * that. Hard mode chests hold 15% more, 30% more within the challenge time.
     */
    public static int[] tobCommonRange(int min, double ratio, RaidMode mode, boolean withinTarget) {
        double q = mode == RaidMode.HARD ? (withinTarget ? 1.30 : 1.15) : 1.0;
        int lo = (int) Math.floor(min * q * ratio);
        return new int[]{lo, (int) Math.floor(lo * 1.2)};
    }

    // ------------------------------------------------------------------ Tombs of Amascut

    /**
     * ToA hitpoints multiplier ignoring path level and rounding: +4% per 10 raid levels (truncated to
     * whole percent), x1.9 for 2 players, x2.8 for 3 and +0.6 for each further player (OSRS).
     */
    public static double toaHp(int raidLevel, int partySize) {
        int n = clampParty(partySize, 8);
        double party = 1.0 + 0.9 * Math.min(n - 1, 2) + 0.6 * Math.max(0, n - 3);
        return (1.0 + (4 * Math.max(0, raidLevel) / 10) / 100.0) * party;
    }

    /**
     * Exact ToA hitpoints (OSRS): raid level (+0.4% per level, truncated to whole percent), path level
     * for path monsters (+3% + 5% per level), party size, then rounded to 5 (over 100) or 10 (over 300).
     */
    public static int toaScaledHp(int baseHp, int raidLevel, int pathLevel, int partySize, boolean pathMonster) {
        int hp = baseHp;
        hp += hp * (4 * Math.max(0, raidLevel) / 10) / 100;
        int path = Math.max(0, Math.min(TOA_MAX_PATH_LEVEL, pathLevel));
        if (pathMonster && path >= 1) {
            hp = hp * (100 + 3 + 5 * path) / 100;
        }
        int n = clampParty(partySize, 8);
        if (n >= 2) {
            int factor = 9 * (n >= 3 ? 2 : 1);
            if (n >= 4) {
                factor += 6 * (n - 3);
            }
            hp = hp * (10 + factor) / 10;
        }
        if (hp > 100) {
            int roundTo = hp > 300 ? 10 : 5;
            hp = (hp + roundTo / 2) / roundTo * roundTo;
        }
        return Math.max(1, hp);
    }

    /**
     * Path of Apmeken's baboons have static hitpoints (OSRS, May 2024): neither the raid level, the path level nor
     * the party size scales them.
     */
    static final Set<String> TOA_STATIC_HP = Set.of("toa_baboon_brawler", "toa_baboon_thrower", "toa_baboon_mage");

    /** {@link #toaScaledHp(int, int, int, int, boolean)} for creature {@code id}, keeping static-hitpoint creatures. */
    public static int toaScaledHp(String id, int baseHp, int raidLevel, int pathLevel, int partySize, boolean pathMonster) {
        return TOA_STATIC_HP.contains(id) ? Math.max(1, baseHp) : toaScaledHp(baseHp, raidLevel, pathLevel, partySize, pathMonster);
    }

    /** Ba-Ba's baboons: 10 hitpoints, static (OSRS, June 2025; they were 35). */
    public static final int BABA_BABOON_HP = 10;

    /** Baboons Ba-Ba has out at once (OSRS: 1-4 by party size). // approx: one per player, up to four */
    public static int babaBaboons(int partySize) {
        return clampParty(partySize, 4);
    }

    /**
     * ToA creature damage multiplier (OSRS wiki): +2% per 5 raid levels, additively, capped at +150% (so x1.4 at
     * raid level 100, x2.2 at 300 and x2.5 from 375).
     */
    public static double toaDamage(int raidLevel) {
        int pct = Math.min(150, Math.max(0, raidLevel) / 5 * 2);
        return 1.0 + pct / 100.0;
    }

    /**
     * ToA accuracy and Defence multiplier (OSRS wiki; the DPS calculator scales the defence roll by (250 + raid level)
     * / 250): +0.4% per raid level, i.e. +2% per 5 levels, uncapped (x1.4 at 100, x3 at 500). Creatures' attack rolls
     * and their defence rolls are multiplied by it.
     */
    public static double toaAccuracy(int raidLevel) {
        return (250 + Math.max(0, raidLevel)) / 250.0;
    }

    /** ToA creatures whose Defence the raid level does not scale (OSRS DPS calculator: Kephri's overlords). */
    static final Set<String> TOA_UNSCALED_DEFENCE = Set.of("toa_soldier_scarab", "toa_spitting_scarab", "toa_arcane_scarab");

    /** Multiplier of a ToA creature's defence roll at this raid level. */
    public static double toaDefence(String id, int raidLevel) {
        return TOA_UNSCALED_DEFENCE.contains(id) ? 1.0 : toaAccuracy(raidLevel);
    }

    /** Path level damage bonus of path monsters (OSRS): +8% at path level 1 and +5% per further level. */
    public static double toaPathMultiplier(int pathLevel) {
        int path = Math.max(0, Math.min(TOA_MAX_PATH_LEVEL, pathLevel));
        return path == 0 ? 1.0 : 1.0 + (3 + 5 * path) / 100.0;
    }

    /**
     * Level of a path (OSRS): Pathseeker/Pathfinder/Pathmaster level every path 1/2/3 times on entry; with Walk the
     * Path, {@code walkLevelUps} is how many random level-ups that path has received (see {@link #walkThePathLevelUps}).
     */
    public static int toaPathLevel(Set<Invocation> invocations, int walkLevelUps) {
        int level = 0;
        if (invocations.contains(Invocation.PATHSEEKER)) {
            level = 1;
        } else if (invocations.contains(Invocation.PATHFINDER)) {
            level = 2;
        } else if (invocations.contains(Invocation.PATHMASTER)) {
            level = 3;
        }
        if (invocations.contains(Invocation.WALK_THE_PATH)) {
            level += Math.max(0, walkLevelUps);
        }
        return Math.min(TOA_MAX_PATH_LEVEL, level);
    }

    /**
     * Walk the Path (OSRS wiki): four level-ups per raid, given to random paths. Completing the first path levels two
     * of the other paths, the second one of the remaining two and the third the final path. {@code remaining} are the
     * paths not yet completed (excluding the one just finished), {@code completed} how many paths are done now (1-3).
     * Returns the paths that level up, each at most once.
     */
    public static List<Integer> walkThePathLevelUps(List<Integer> remaining, int completed, java.util.Random random) {
        List<Integer> pool = new ArrayList<>(remaining);
        int n = Math.min(pool.size(), completed == 1 ? 2 : completed <= 3 ? 1 : 0);
        java.util.Collections.shuffle(pool, random);
        return new ArrayList<>(pool.subList(0, Math.max(0, n)));
    }

    /**
     * ToA's effective raid level for the purple chance (OSRS): levels above 430 count half, then
     * levels above 310 count a third.
     */
    static int toaPurpleLevel(int raidLevel) {
        int level = Math.max(0, raidLevel);
        if (level > 310) {
            if (level > 430) {
                level = 430 + (level - 430) / 2;
            }
            level = 310 + (level - 310) / 3;
        }
        return level;
    }

    /** ToA's effective raid level for the pet chance (OSRS): levels above 400 count a third, 450 above 550. */
    static int toaPetLevel(int raidLevel) {
        int level = Math.max(0, raidLevel);
        if (level > 550) {
            return 450;
        }
        return level > 400 ? 400 + (level - 400) / 3 : level;
    }

    /**
     * ToA purple chance (OSRS): team points / (100 x (10,500 - 20 x effective level)), the divisor never
     * below 150,000, capped at 55%. Only one unique can be awarded per raid.
     */
    public static double toaPurpleChance(int totalPoints, int raidLevel) {
        double divisor = Math.max(150_000, 100.0 * (10_500 - 20 * toaPurpleLevel(raidLevel)));
        return Math.min(0.55, Math.max(0, totalPoints) / divisor);
    }

    /** Tumeken's guardian chance (OSRS): team points / (100 x (350,000 - 700 x effective pet level)). */
    public static double toaPetChance(int totalPoints, int raidLevel) {
        double divisor = 100.0 * (350_000 - 700 * toaPetLevel(raidLevel));
        return Math.min(1.0, Math.max(0, totalPoints) / divisor);
    }

    /** ToA elite clue chance (OSRS): team points / 200,000. */
    public static double toaEliteClueChance(int totalPoints) {
        return Math.min(1.0, Math.max(0, totalPoints) / 200_000.0);
    }

    /**
     * ToA common loot multiplier by raid level (OSRS): x0.75 below 150, and from 300 up +15% plus 1%
     * per 5 levels above 300.
     */
    public static double toaCommonScale(int raidLevel) {
        if (raidLevel < 150) {
            return 0.75;
        }
        if (raidLevel >= 300) {
            return 1 + ((raidLevel - 300) / 5 + 15) / 100.0;
        }
        return 1.0;
    }

    /** ToA common quantity (OSRS): floor(floor(personal points / divisor) x level scale), at least 1. */
    public static int toaCommonQuantity(int personalPoints, int divisor, int raidLevel) {
        int base = Math.max(0, Math.min(personalPoints, TOA_POINTS_CAP)) / Math.max(1, divisor);
        return Math.max(1, (int) Math.floor(base * toaCommonScale(raidLevel)));
    }

    /**
     * ToA points per damage by creature (OSRS wiki loot calculator): Ba-Ba x2, baboons x1.2, Zebak x1.5,
     * scarabs x0.5, the obelisk x1.5, the Wardens x2 (phase 2) and x2.5 (phase 3); everything else x1.
     */
    public static double toaPointsPerDamage(String id, boolean wardenPhaseThree) {
        return switch (id) {
            case "baba" -> 2.0;
            case "zebak" -> 1.5;
            case "wardens_obelisk" -> 1.5;
            case "elidinis_warden", "tumekens_warden" -> wardenPhaseThree ? 2.5 : 2.0;
            default -> id.contains("baboon") ? 1.2 : id.contains("scarab") ? 0.5 : 1.0;
        };
    }

    /** Damage a Warden's body must take in phase 2 before its core is ejected: a quarter of its hitpoints. // approx */
    public static int toaCoreChargeNeeded(int wardenMaxHp) {
        return Math.max(1, wardenMaxHp / 4);
    }

    /**
     * Phase 2 Wardens only lose hitpoints while their core is ejected, and one ejection can take at most
     * half of the Warden's hitpoints, so each needs at least two "downs" (OSRS). // approx: the exact cap
     */
    public static int toaCoreDamageCap(int wardenMaxHp) {
        return Math.max(1, wardenMaxHp / 2);
    }

    /** ToA: a death costs 20% of your personal points (approximation of the OSRS death penalty). */
    public static int toaDeathPenalty(int points) {
        return points - points * 20 / 100;
    }
}
