package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.data.Drop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Delve levels of the Doom of Mokhaiotl (OSRS Wiki "Doom of Mokhaiotl" and its strategy page, osrs-dps-calc "Delve N").
 * Each defeat sends the Doom deeper: the player may follow it (keeping the loot so far at risk) or claim the loot and
 * leave. Hitpoints rise by 25 a level (delves 6 and 7 both 650), max hits and attack speed rise up to delve 8, and delves
 * beyond 8 ("deep delves") are delve 8 with 625 hitpoints. Every completed level rolls the loot table once with the
 * level's quantity multiplier, guaranteed demon tears from delve 3, and the unique table from delve 2.
 */
public final class DoomDelves {
    private DoomDelves() {
    }

    public static final String BOSS = "doom_of_mokhaiotl";
    /** Deepest level with its own stats; deeper levels repeat it ("deep delves"). */
    public static final int DEEPEST = 9;
    /** Kill-count style keys for the personal best and the number of deep delves (9+) completed. */
    public static final String DEEPEST_KEY = "doom_of_mokhaiotl_deepest_delve";
    public static final String DEEP_DELVES_KEY = "doom_of_mokhaiotl_deep_delves";

    //                                       1    2    3    4    5    6    7    8   9+
    private static final int[] HITPOINTS = {525, 550, 575, 600, 625, 650, 650, 675, 625};
    private static final int[] COMBAT = {558, 608, 658, 708, 758, 808, 858, 908, 908};
    private static final int[] SPEED = {6, 6, 5, 5, 5, 5, 4, 4, 4};
    /** Max hit of the standard orb/melee attacks: 47 at delve 1 rising to 65 at delve 8. */
    private static final int[] STANDARD_MAX = {47, 50, 50, 52, 53, 55, 57, 65, 65}; // delves 4 and 6 approx (interpolated)
    /**
     * The Special Beam Cannon ("charge"): 60, 80 at delves 4-5, 99 from delve 6 (the strategy page's table of attacks and the
     * shielded infobox; the main infobox's "80 (charge)" at delve 3 disagrees with both).
     */
    private static final int[] CHARGE_MAX = {60, 60, 60, 80, 80, 99, 99, 99, 99};
    private static final double[] QUANTITY = {-0.5, -0.35, 0, 0.05, 0.1, 0.12, 0.14, 0.17, 0.2};
    private static final int[] TEARS = {0, 0, 50, 60, 70, 80, 90, 100, 100};
    /** 1/n chance of a unique per completed level (0: none), shared equally by the uniques unlocked so far. */
    private static final int[] UNIQUE = {0, 2500, 1000, 450, 270, 255, 240, 210, 180};
    /** Dom: 1/1000 at delve 6, 1/750 at 7, 1/500 at 8 and 1/250 deeper. */
    private static final int[] PET = {0, 0, 0, 0, 0, 1000, 750, 500, 250};
    /** Uniques in the order they unlock: cloth from delve 2, the eye of ayak from 3, avernic treads from 4. */
    public static final List<String> UNIQUES = List.of("mokhaiotl_cloth", "eye_of_ayak", "avernic_treads");
    /** Table items added in round 39 (CommonItems: shark lure, sun-kissed bones; the bosses module: the waystone). */
    public static final String SHARK_LURE = "shark_lure";
    public static final String WAYSTONE = "mokhaiotl_waystone";
    public static final String SUN_KISSED_BONES = "sun_kissed_bones";
    /** Sun-kissed bones break down with a chisel into 45 blessed bone shards and 5 Crafting XP (OSRS Wiki). */
    public static final int SHARDS_PER_SUN_KISSED_BONES = 45;
    public static final int SUN_KISSED_CRAFTING_XP = 5;
    /** The Doom is fought after The Final Dawn (and the waystones only work after it). */
    public static final String QUEST = "the_final_dawn";

    /**
     * The regular table (rolled once per level), weights out of 312 (OSRS Wiki: x/104 lines are 3x/312; raw sharks 10/312
     * and shark lures 5/312 split the old shark line 2:1), quantities at delve 3. Diffed against the wiki snapshot
     * {@code bosses/doom_delve_table.tsv} by {@code DoomDelveTableTest}. Shark lures only drop in even quantities at delve 3.
     */
    static final List<Drop> TABLE = List.of(
            Drop.of("dragon_med_helm", 1, 1, 15), Drop.of("dragon_platelegs", 2, 4, 3), Drop.of("mystic_earth_staff", 1, 1, 15),
            Drop.of("rune_pickaxe", 1, 3, 15), Drop.of("death_rune", 50, 70, 15), Drop.of("chaos_rune", 50, 70, 15),
            Drop.of("earth_rune", 500, 1000, 15), Drop.of("fire_rune", 500, 1000, 15), Drop.of("cannonball", 200, 600, 15),
            Drop.of("onyx_bolts", 5, 15, 15), Drop.of("coal", 15, 50, 15), Drop.of("gold_ore", 20, 60, 15),
            Drop.of("runite_ore", 3, 6, 15), Drop.of("celastrus_seed", 1, 1, 9), Drop.of("ranarr_seed", 1, 3, 6),
            Drop.of("spirit_seed", 1, 1, 9), Drop.of("aether_catalyst", 151, 400, 15), Drop.of("dragon_dart_tip", 30, 90, 15),
            Drop.of("raw_shark", 20, 35, 10), Drop.of(SHARK_LURE, 40, 70, 5), Drop.of("sun_kissed_bones", 18, 75, 15),
            Drop.of("tooth_half_of_key", 1, 1, 3), Drop.of("demon_tear", 100, 300, 21), Drop.of(WAYSTONE, 1, 2, 21));

    /** The table's total weight (the wiki's 312). */
    public static final int TABLE_WEIGHT = 312;

    /** The regular table (for the snapshot test and the collection log). */
    public static List<Drop> table() {
        return TABLE;
    }

    /**
     * A deep delve level's (9+) loot as 1/N lines, for the boss's listed drops: the guaranteed 100 demon tears, each unique
     * 1/540, the regular table's lines at their share of 312 (quantities at delve 3) and the elite clue 1/50.
     */
    public static List<Drop> deepDelveDrops() {
        List<Drop> out = new ArrayList<>();
        out.add(Drop.of("demon_tear", guaranteedTears(DEEPEST), guaranteedTears(DEEPEST), 1));
        for (String u : UNIQUES) {
            out.add(Drop.of(u, 1, 1, uniqueRate(DEEPEST) * UNIQUES.size()));
        }
        for (Drop d : TABLE) {
            out.add(Drop.of(d.item(), d.min(), d.max(), Math.max(1, Math.round(TABLE_WEIGHT / (float) d.rarity()))));
        }
        out.add(Drop.of("clue_scroll_elite", 1, 1, clueRate(DEEPEST)));
        return out;
    }

    private static int index(int level) {
        return Math.max(1, Math.min(DEEPEST, level)) - 1;
    }

    public static int hitpoints(int level) {
        return HITPOINTS[index(level)];
    }

    public static int combatLevel(int level) {
        return COMBAT[index(level)];
    }

    public static int attackSpeed(int level) {
        return SPEED[index(level)];
    }

    public static int standardMaxHit(int level) {
        return STANDARD_MAX[index(level)];
    }

    public static int chargeMaxHit(int level) {
        return CHARGE_MAX[index(level)];
    }

    public static double quantityMultiplier(int level) {
        return QUANTITY[index(level)];
    }

    /** Demon tears guaranteed for completing a level. */
    public static int guaranteedTears(int level) {
        return TEARS[index(level)];
    }

    /** 1/n chance of any unique on completing a level (0 when none can drop). */
    public static int uniqueRate(int level) {
        return UNIQUE[index(level)];
    }

    /** The uniques that can drop on completing a level. */
    public static List<String> eligibleUniques(int level) {
        return UNIQUES.subList(0, Math.max(0, Math.min(3, level - 1)));
    }

    /** 1/n chance of Dom on completing a level (0: none). */
    public static int petRate(int level) {
        return PET[index(level)];
    }

    /** Elite clue chance: 1/75 at delves 1 and 2, 1/50 deeper. */
    public static int clueRate(int level) {
        return level <= 2 ? 75 : 50;
    }

    /** OSRS: Q_n = Q_3 + trunc(Q_3 * M_n). */
    public static int quantity(int q3, int level) {
        return Math.max(1, q3 + (int) (q3 * quantityMultiplier(level)));
    }

    /** One level's Doom with its delve stats; its drops are empty because the loot is held until claimed. */
    public static BossVariant variant(BossDef doom, int level) {
        int lvl = Math.max(1, level);
        String name = lvl >= DEEPEST ? doom.name + " (deep delve " + lvl + ")" : doom.name + " (delve " + lvl + ")";
        Map<String, Integer> max = Map.of("crushing_claws", MELEE_MAX, "void_orb", standardMaxHit(lvl),
                "rock_spit", standardMaxHit(lvl), "melee_orb", standardMaxHit(lvl), "charge", chargeMaxHit(lvl));
        return new BossVariant(doom.id, BossVariant.DELVE, lvl, name, doom.id, combatLevel(lvl), hitpoints(lvl), doom.attack, doom.strength,
                doom.defence, doom.magic, doom.ranged, attackSpeed(lvl), max, standardMaxHit(lvl) / 65.0, List.of(), petRate(lvl));
    }

    // ------------------------------------------------------------------ fight mechanics (OSRS Wiki "Doom of Mokhaiotl",
    // "Demonic larva", "Volatile earth", "Earthen shield", "Acid blood"; timings the wiki doesn't give are approx)

    /** Delve level from which the Doom sprays acid blood, raises its shield (demonbane only) and burrows ("car" phase). */
    public static final int ACID_FROM = 3;
    public static final int SHIELD_FROM = 3;
    public static final int BURROW_FROM = 5;
    /** Delve level that wipes the acid blood carried over from earlier levels (once only). */
    public static final int ACID_WIPE_LEVEL = 6;
    /** The shield phase: 500 shield points, raised at 75% hitpoints or lower after at least two attacks. */
    public static final int SHIELD_POINTS = 500;
    public static final double SHIELD_BELOW = 0.75;
    public static final int SHIELD_MIN_ATTACKS = 2;
    /**
     * Game ticks the Doom charges during the shield; the full bar fires the beam and ends the phase. Every demonbane hit on the
     * shield resets the bar (OSRS Wiki "Doom of Mokhaiotl/Strategies", Special Beam Cannon); it charges faster deeper.
     */
    public static int shieldChargeTicks(int level) {
        return level >= 7 ? 24 : level >= 5 ? 30 : 36; // approx (the wiki gives no bar length)
    }

    /** Demonbane hits count this many times against the shield (they also skip the attack delay in OSRS). */
    public static final int SHIELD_DEMONBANE_FACTOR = 3; // approx
    /** Shield damage of a larva exploding next to the Doom (wiki: 100); outside the shield 5-10 hitpoints. */
    public static final int LARVA_SHIELD_DAMAGE = 100;
    /** A larva exploding by the Doom deals it 5-10 instead of hurting the player. */
    public static final int LARVA_DOOM_MIN = 5;
    public static final int LARVA_DOOM_MAX = 10;
    /** A larva exploding away from the Doom: up to 21 to players in a 3x3 area (a giant larva's is 4x4). */
    public static final int LARVA_EXPLOSION_MAX = 21;

    /** Tiles from a larva its explosion reaches: 1 (3x3), a giant larva's 4x4 about 1.5 (approx: OSRS centres it on a tile). */
    public static double larvaExplosionTiles(boolean giant) {
        return giant ? 1.5 : 1.0;
    }

    /** Game ticks the Special Beam Cannon charges before firing; a melee hit in that window punishes it. */
    public static final int BEAM_CHARGE_TICKS = 7; // approx (fired a tick later since 6 August 2025)
    /** Standard attacks between melee charges (each comes with a Rock Throw). */
    public static final int ATTACKS_PER_BEAM = 7; // approx
    /** Standard attacks between Rock Throws that come without a melee charge. */
    public static final int ATTACKS_PER_ROCK_THROW = 5; // approx
    /** The thrown rock explodes into falling debris 7 ticks after it is launched (OSRS Wiki "Rock Throw"). */
    public static final int ROCK_THROW_TICKS = 7;
    /** Debris landing on a player's shadow deals 15-20. */
    public static final int DEBRIS_MIN = 15;
    public static final int DEBRIS_MAX = 20;
    /** Projectiles launched once the rock explodes (protection prayers are switched off first): 1, 2 at delves 2-3, 3, two sets at 8+. */
    public static int rockThrowProjectiles(int level) {
        return level >= 8 ? 6 : level >= 4 ? 3 : level >= 2 ? 2 : 1;
    }

    /** Shockwaves ("slams") per volatile earth attack: 1 at delves 1-2, 2 at 3-4, 3 at 5-6, 4 at 7 and 5 from 8. */
    public static int shockwaves(int level) {
        return level >= 8 ? 5 : level == 7 ? 4 : level >= 5 ? 3 : level >= 3 ? 2 : 1;
    }

    /** The shockwave ("around a minute into the fight") and when it lands: 20 ticks after the volatile earth appears. */
    public static final int SHOCKWAVE_INTERVAL = 100; // approx (wiki: "around a minute")
    public static final int SHOCKWAVE_WINDUP = 20;
    /** Game ticks between the shockwaves of one attack (two ticks). */
    public static final int SHOCKWAVE_GAP = 2;
    /** Each shockwave deals 30-41 to a player outside the earthen shield. */
    public static final int SHOCKWAVE_MIN = 30;
    public static final int SHOCKWAVE_MAX = 41;
    /** Volatile earth raised for each shockwave attack (OSRS: "multitudes" across the arena). */
    public static final int VOLATILE_EARTH = 6; // approx

    /** Tiles a game tick the earthen shield moves: walking speed, half at delve 1. */
    public static double earthenShieldSpeed(int level) {
        return level <= 1 ? 0.5 : 1.0;
    }

    /** The burrowed ("car") phase: 2 zooms at delve 5, 3 (each followed by a car slam) from delve 6. */
    public static int zooms(int level) {
        return level >= 6 ? 3 : level >= BURROW_FROM ? 2 : 0;
    }

    public static boolean carSlams(int level) {
        return level >= 6;
    }

    /** Trample damage of a zoom: 10 at delve 5, 20 at 6, 30 at 7 and 40 from 8. */
    public static int trampleDamage(int level) {
        return level >= 8 ? 40 : level == 7 ? 30 : level == 6 ? 20 : level >= BURROW_FROM ? 10 : 0;
    }

    /** Game ticks a zoom takes to reach the eye: 1-2 from delve 8, slower before. */
    public static int zoomTicks(int level) {
        return level >= 8 ? 2 : 3; // approx below delve 8
    }

    /** Game ticks between the eye appearing and the zoom ("after a short duration"). */
    public static final int ZOOM_WINDUP = 3; // approx
    /** Rocks falling as the Doom burrows (fewer from delve 8). */
    public static int burrowRocks(int level) {
        return level >= 8 ? 3 : 5; // approx
    }

    /** A burrow beam charge (any hit resets it); the full bar fires the beam. */
    public static final int BURROW_CHARGE_TICKS = 16; // approx
    /** The Doom's tongue lash next to it: up to 40, halved by Protect from Melee. */
    public static final int MELEE_MAX = 40;
    /**
     * Holy water from a kill by a melee punish (delves 1-8): restores 32 Hitpoints, 24 prayer points and 25% special attack
     * energy (the 6 August 2025 update; the strategy page still says 28 and 14) and clears acid blood around it.
     */
    public static final int HOLY_WATER_HP = 32;
    public static final int HOLY_WATER_PRAYER = 24;
    public static final int HOLY_WATER_SPEC = 25;
    /** Holy water only scatters at delves 1-8. */
    public static final int HOLY_WATER_UNTIL = 8;
    /** Blocks from its centre tile the earthen shield protects (a 3x3 area, approx). */
    public static final double EARTHEN_SHIELD_BLOCKS = 2.5;

    /** Blocks around the Doom a car slam smashes unprotected rocks in (OSRS: 15 tiles in a much bigger cavern). */
    public static final double SLAM_SMASH_BLOCKS = 8; // approx

    /** A car slam's max hit: the burrowed form's 55 at delve 6, 56 at 7, 58 at 8 and 60 deeper (wiki burrowed infobox). */
    public static int slamMaxHit(int level) {
        return level >= 9 ? 60 : level == 8 ? 58 : level == 7 ? 56 : 55;
    }

    /** Combat Achievement flags the fight reports (see {@code CombatAchievementRules}, tasks 613-616, 620 and 624). */
    public static final String FLAG_EXPOSED = "exposed";
    public static final String FLAG_BURROWED = "burrowed";
    public static final String FLAG_GRUB_ABSORBED = "grub_absorbed";
    public static final String FLAG_NO_SHIELD = "noshield";
    public static final String FLAG_MOPPING_UP = "mopping_up";
    /** Acid splats one earthen shield must clear for the "Mopping up" task. */
    public static final int MOPPING_UP_SPLATS = 8;

    /** Game ticks the melee punish delays the Doom's next attack: 8 at delves 1-2, 7 at 3-6, 6 from 7 (OSRS Wiki strategies). */
    public static int punishDelay(int level) {
        return level <= 2 ? 8 : level <= 6 ? 7 : 6;
    }

    /** Bonus damage of a melee punish: a fifth of the player's visible melee strength bonus. */
    public static int punishBonus(int meleeStrength) {
        return Math.max(0, meleeStrength / 5);
    }

    /** Game ticks between larvae dropping outside the shield (none before delve 5 except a lone larva now and then). */
    public static int larvaInterval(int level) {
        return level >= 5 ? 20 : 30; // approx
    }

    /** Larvae per drop outside the shield: one, two at delves 5-7, one giant larva from delve 8. */
    public static int larvaeOutsideShield(int level) {
        return level >= 5 && level <= 7 ? 2 : 1;
    }

    public static boolean giantLarvae(int level) {
        return level >= 8;
    }

    /** Game ticks between larvae during the shield phase; fastest from delve 7. */
    public static int shieldLarvaInterval(int level) {
        return level >= 7 ? 3 : level >= 5 ? 4 : 5; // approx
    }

    /**
     * A larva reaching the Doom: it heals 10 and deals 1 at no stacks of demonic charge, more with every stack (stacks
     * carry over between levels); a giant larva counts three times. Damage is capped at 12 before delve 8, then 20 (30 for
     * a giant larva).
     */
    public static int larvaHeal(int stacks, boolean giant) {
        return 10 * (stacks + 1) * (giant ? 3 : 1); // approx: linear growth per stack
    }

    public static int larvaDamage(int stacks, boolean giant, int level) {
        int cap = level < 8 ? 12 : giant ? 30 : 20;
        return Math.min(cap, (stacks + 1) * (giant ? 3 : 1));
    }

    /** Stacks of demonic charge a larva adds when it reaches the Doom. */
    public static int larvaStacks(boolean giant) {
        return giant ? 3 : 1;
    }

    /** Larva hitpoints: 2, a giant larva 4. Non-demonbane hits deal at most 1; a demonbane hit kills a small larva outright. */
    public static int larvaHitpoints(boolean giant) {
        return giant ? 4 : 2;
    }

    /** Damage a player's hit deals to a larva. */
    public static int larvaHit(int rolled, boolean demonbane) {
        if (demonbane) {
            return larvaHitpoints(false); // one demonbane hit kills a small larva, two a giant one
        }
        return Math.min(1, Math.max(0, rolled));
    }

    // ------------------------------------------------------------------ held loot (stored with the player)

    /** Held loot as "item:count;item:count" for storage. */
    public static String encode(List<Loot> loot) {
        StringBuilder b = new StringBuilder();
        for (Loot l : loot) {
            if (b.length() > 0) {
                b.append(';');
            }
            b.append(l.item()).append(':').append(l.count());
        }
        return b.toString();
    }

    public static List<Loot> decode(String s) {
        List<Loot> out = new ArrayList<>();
        if (s == null || s.isBlank()) {
            return out;
        }
        for (String part : s.split(";")) {
            int c = part.lastIndexOf(':');
            if (c <= 0) {
                continue;
            }
            try {
                out.add(new Loot(part.substring(0, c), Integer.parseInt(part.substring(c + 1))));
            } catch (NumberFormatException ignored) {
                // skip a damaged entry
            }
        }
        return out;
    }

    /** An item and amount awarded for completing a level. */
    public record Loot(String item, int count) {
    }

    /** Rolls the reward for completing one delve level: the regular table, guaranteed tears, a unique and a clue. */
    public static List<Loot> roll(int level, Random random) {
        List<Loot> out = new ArrayList<>();
        int total = 0;
        for (Drop d : TABLE) {
            total += d.rarity();
        }
        int r = random.nextInt(total);
        for (Drop d : TABLE) {
            r -= d.rarity();
            if (r < 0) {
                int q3 = d.item().equals(SHARK_LURE) ? d.min() + 2 * random.nextInt((d.max() - d.min()) / 2 + 1) // even only
                        : d.min() + random.nextInt(d.max() - d.min() + 1);
                out.add(new Loot(d.item(), quantity(q3, level)));
                break;
            }
        }
        if (guaranteedTears(level) > 0) {
            out.add(new Loot("demon_tear", guaranteedTears(level)));
        }
        int u = uniqueRate(level);
        List<String> eligible = eligibleUniques(level);
        if (u > 0 && !eligible.isEmpty() && random.nextInt(u) == 0) {
            out.add(new Loot(eligible.get(random.nextInt(eligible.size())), 1));
        }
        if (random.nextInt(clueRate(level)) == 0) {
            out.add(new Loot("clue_scroll_elite", 1));
        }
        return out;
    }
}
