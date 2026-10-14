package com.runecraft.osrs.content.systems;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * NPC services and objects behind achievement diary tasks (OSRS Wiki tables): Tindel Marchant's rusty sword repair,
 * the Lava Maze muddy chest, the Taverley crystal chest and the Stronghold of Security sceptre assembly.
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
}
