package com.runecraft.osrs.content.systems;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * NPC services and objects behind achievement diary tasks (OSRS Wiki tables): Tindel Marchant's rusty sword repair,
 * the Lava Maze muddy chest, the Taverley crystal chest, the Stronghold of Security sceptre assembly, the Hosidius
 * saltpetre deposits, the Legends' Guild totem pole, the Varrock sawmill operator and fallen Ent trunks.
 */
public final class DiaryExtras {
    private DiaryExtras() {
    }

    /** One item of a reward roll. */
    public record Loot(String item, int count) {
    }

    // ---------------- NPCs and objects ----------------

    public static final String TINDEL = "tindel_marchant";
    /** Tindel Marchant on the southern dock of Port Khazard (wiki map 2673, 3150). */
    public static final int TINDEL_X = 2673;
    public static final int TINDEL_Y = 3150;
    public static final int TINDEL_FEE = 100;
    public static final String GENIE = "genie_elid";
    /** The genie's crevice north-east of Nardah (Spirits of the Elid); the cave itself is not carved, so he stands at its mouth. // approx */
    public static final int GENIE_X = 3372;
    public static final int GENIE_Y = 2906;
    public static final String MUDDY_CHEST = "muddy_chest";
    public static final int MUDDY_CHEST_X = 3089;
    public static final int MUDDY_CHEST_Y = 3859;
    public static final String CRYSTAL_CHEST = "crystal_chest";
    public static final int CRYSTAL_CHEST_X = 2914;
    public static final int CRYSTAL_CHEST_Y = 3452;

    // ---------------- Tindel Marchant ----------------

    /** Tindel's rusty sword results out of 100 (wiki "Possible rewards (Tindel Marchant)"); null is nothing (the sword breaks). */
    private static final Object[][] TINDEL_TABLE = {
            {"iron_sword", 20}, {"iron_longsword", 37}, {"bronze_sword", 17}, {"bronze_longsword", 7}, {"steel_sword", 6},
            {"steel_longsword", 4}, {"mithril_sword", 1}, {"mithril_longsword", 1}, {"black_sword", 3}, {"black_longsword", 2},
            {"coins", 1}, {null, 1}};

    /**
     * Tindel's result for one rusty sword: a sword, about 2,000 coins (an "ancient artefact" he keeps, which doesn't count
     * for the diary) or nothing.
     */
    public static Loot tindel(DoubleSupplier rng) {
        int roll = (int) (rng.getAsDouble() * 100);
        for (Object[] row : TINDEL_TABLE) {
            roll -= (Integer) row[1];
            if (roll < 0) {
                if (row[0] == null) {
                    return null;
                }
                return row[0].equals("coins") ? new Loot("coins", 1500 + (int) (rng.getAsDouble() * 500)) : new Loot((String) row[0], 1);
            }
        }
        return null;
    }

    /** Whether Tindel's result is a repaired weapon (what the easy Ardougne task needs). */
    public static boolean identified(Loot result) {
        return result != null && !result.item().equals("coins");
    }

    // ---------------- Chests ----------------

    /**
     * The muddy chest (wiki, since February 2024): always an uncut ruby, 2 mithril bars, 5 law, 5 death and 15 chaos runes,
     * plus a 3/40 roll on the rare table (blighted supplies and Larran's key; items RuneCraft lacks are skipped by the caller).
     */
    public static List<Loot> muddyChest(DoubleSupplier rng) {
        List<Loot> loot = new ArrayList<>(List.of(new Loot("uncut_ruby", 1), new Loot("mithril_bar", 2), new Loot("law_rune", 5),
                new Loot("death_rune", 5), new Loot("chaos_rune", 15)));
        if (rng.getAsDouble() < 3.0 / 40) {
            int r = (int) (rng.getAsDouble() * 21);
            loot.add(r < 3 ? new Loot("blighted_manta_ray", 25) : r < 6 ? new Loot("blighted_karambwan", 25)
                    : r < 9 ? new Loot("blighted_ancient_ice_sack", 25) : r < 12 ? new Loot("blighted_anglerfish", 15)
                    : r < 15 ? new Loot("blighted_super_restore", 3) : new Loot("larrans_key", 1));
        }
        return loot;
    }

    /** The crystal chest (wiki): always an uncut dragonstone, plus one of eleven tables out of 128. */
    public static List<Loot> crystalChest(DoubleSupplier rng) {
        List<Loot> loot = new ArrayList<>();
        loot.add(new Loot("uncut_dragonstone", 1));
        int roll = (int) (rng.getAsDouble() * 128);
        if ((roll -= 34) < 0) {
            loot.add(new Loot("spinach_roll", 1));
            loot.add(new Loot("coins", 2000));
        } else if ((roll -= 12) < 0) {
            for (String r : new String[]{"air", "water", "earth", "fire", "body", "mind"}) {
                loot.add(new Loot(r + "_rune", 50));
            }
            for (String r : new String[]{"chaos", "death", "cosmic", "nature", "law"}) {
                loot.add(new Loot(r + "_rune", 10));
            }
        } else if ((roll -= 12) < 0) {
            loot.add(new Loot("ruby", 2));
            loot.add(new Loot("diamond", 2));
        } else if ((roll -= 12) < 0) {
            loot.add(new Loot("runite_bar", 3));
        } else if ((roll -= 10) < 0) {
            loot.add(new Loot("coins", 750));
            loot.add(new Loot(rng.getAsDouble() < 0.5 ? "loop_half_of_key" : "tooth_half_of_key", 1));
        } else if ((roll -= 10) < 0) {
            loot.add(new Loot("iron_ore", 150));
        } else if ((roll -= 10) < 0) {
            loot.add(new Loot("coal", 100));
        } else if ((roll -= 8) < 0) {
            loot.add(new Loot("raw_swordfish", 5));
            loot.add(new Loot("coins", 1000));
        } else if ((roll -= 2) < 0) {
            loot.add(new Loot("adamant_sq_shield", 1));
        } else if ((roll -= 1) < 0) {
            loot.add(new Loot(rng.getAsDouble() < 0.5 ? "rune_platelegs" : "rune_plateskirt", 1));
        }
        // the remaining 17/128: nothing else
        return loot;
    }

    // ---------------- Item-on-item joins ----------------

    /** "Use A on B" joins: {first, second, result}. Key halves (slayer module) make a crystal key; the sceptre pieces a sceptre. */
    public static final List<String[]> JOINS = List.of(
            new String[]{"loop_half_of_key", "tooth_half_of_key", "crystal_key"},
            new String[]{"left_skull_half", "right_skull_half", "strange_skull"},
            new String[]{"top_of_sceptre", "bottom_of_sceptre", "runed_sceptre"},
            new String[]{"strange_skull", "runed_sceptre", "skull_sceptre"});

    /** The join a held item takes part in, or null. */
    public static String[] join(String held) {
        for (String[] j : JOINS) {
            if (j[0].equals(held) || j[1].equals(held)) {
                return j;
            }
        }
        return null;
    }

    // ---------------- Round 23: saltpetre, totem pole, sawmill, Ent trunks ----------------

    public static final String SALTPETRE_DEPOSIT = "saltpetre_deposit";
    /** The five saltpetre deposits north-east of the Woodcutting Guild (wiki map pins). */
    public static final int[][] SALTPETRE_TILES = {{1708, 3545}, {1716, 3518}, {1687, 3512}, {1686, 3529}, {1702, 3508}};

    /** Saltpetre per dig: two with a chance of roughly the Mining level out of 100 (wiki), otherwise one. */
    public static int saltpetreDug(int miningLevel, double roll) {
        return roll < Math.min(99, miningLevel) / 100.0 ? 2 : 1;
    }

    public static final String LEGENDS_TOTEM = "legends_totem_pole";
    /** The totem pole in the Legends' Guild main hall (wiki map 2729, 3378). */
    public static final int TOTEM_X = 2729;
    public static final int TOTEM_Y = 3378;
    /** Jewellery the totem pole recharges (to 4 charges, unlike the Fountain of Rune's 6). */
    public static final List<String> TOTEM_RECHARGES = List.of("skills_necklace", "combat_bracelet");
    public static final int TOTEM_CHARGES = 4;

    public static final String SAWMILL_OPERATOR = "sawmill_operator";
    /** The Varrock Lumber Yard's sawmill operator (wiki map 3302, 3492). */
    public static final int SAWMILL_X = 3302;
    public static final int SAWMILL_Y = 3492;

    /** A sawmill conversion: logs to planks and the fee per plank (wiki "Sawmill"). */
    public record Sawmill(String logs, String plank, int fee) {
    }

    /** Most valuable first: the operator converts every log of the best kind carried in one go. */
    public static final List<Sawmill> SAWMILL = List.of(
            new Sawmill("mahogany_logs", "mahogany_plank", 1500),
            new Sawmill("teak_logs", "teak_plank", 500),
            new Sawmill("oak_logs", "oak_plank", 250),
            new Sawmill("logs", "plank", 100));

    /** How many planks the operator makes from {@code logs} logs with {@code coins} coins at {@code fee} each. */
    public static int sawmillBatch(int logs, int coins, int fee) {
        return Math.max(0, Math.min(logs, fee <= 0 ? logs : coins / fee));
    }

    public static final String ENT_TRUNK = "ent_trunk";
    /** An Ent's trunk lasts 101 ticks after it dies (wiki); a chop roll every 4 ticks gives about 25 attempts. */
    public static final int ENT_TRUNK_TICKS = 101;
    public static final int ENT_CHOP_TICKS = 4;
    /** Woodcutting XP per set of logs from an Ent trunk, whatever the log type (wiki). */
    public static final double ENT_XP = 25;

    /**
     * The log type a trunk chop gives at a base Woodcutting level: magic from 75, yew from 61, maple from 45, willow from
     * 30, oak from 15 (wiki notes); the best available tier comes up half the time, else the tier below. // approx (axe-based split)
     */
    public static String entLog(int baseLevel, double roll) {
        String[] tiers = {"logs", "oak_logs", "willow_logs", "maple_logs", "yew_logs", "magic_logs"};
        int[] from = {1, 15, 30, 45, 61, 75};
        int best = 0;
        for (int i = 0; i < from.length; i++) {
            if (baseLevel >= from[i]) {
                best = i;
            }
        }
        return tiers[roll < 0.5 || best == 0 ? best : best - 1];
    }

    /** Chance a trunk chop succeeds at the current Woodcutting level. // approx (wiki gives no formula) */
    public static double entChopChance(int level, boolean wildernessDiaryMedium) {
        double p = Math.min(0.95, 0.25 + level / 200.0);
        return wildernessDiaryMedium ? Math.min(0.95, p * 1.15) : p;
    }

    /** Logs per successful chop: 2 in the Wilderness, 1 in the Woodcutting Guild (wiki). */
    public static int entLogsPerChop(boolean wilderness) {
        return wilderness ? 2 : 1;
    }
}
