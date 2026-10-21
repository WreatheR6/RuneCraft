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

    // ---------------- Round 40: taskmasters, water sources ----------------

    /** An area's diary taskmaster (OSRS wiki "Achievement Diary"): NPC id, name and the tile mejrs' NPC list gives. */
    public record Taskmaster(String area, String npcId, String name, int x, int y) {
    }

    /**
     * The twelve taskmasters who hand out each area's rewards. Karamja's, Pirate Jackie the Fruit, is the minigames module's
     * Brimhaven Agility Arena NPC (talking to him offers the Karamja Diary); the others are added by this module.
     */
    public static final List<Taskmaster> TASKMASTERS = List.of(
            new Taskmaster("ardougne", "diary_two_pints", "Two-pints", 2574, 3325),
            new Taskmaster("desert", "diary_jarr", "Jarr", 3299, 3121),
            new Taskmaster("falador", "diary_sir_rebral", "Sir Rebral", 2977, 3346),
            new Taskmaster("fremennik", "diary_thorodin", "Thorodin", 2659, 3627),
            new Taskmaster("kandarin", "diary_the_wedge", "The 'Wedge'", 2760, 3476),
            new Taskmaster("karamja", "mg_pirate_jackie", "Pirate Jackie the Fruit", 2811, 3192),
            new Taskmaster("kourend", "diary_elise", "Elise", 1648, 3665),
            new Taskmaster("lumbridge", "diary_hatius_cosaintus", "Hatius Cosaintus", 3234, 3214),
            new Taskmaster("morytania", "diary_le_sabre", "Le-sabrè", 3465, 3478),
            new Taskmaster("varrock", "diary_toby", "Toby", 3225, 3415),
            new Taskmaster("western", "diary_elder_gnome_child", "Elder Gnome child", 2466, 3459),
            new Taskmaster("wilderness", "diary_lesser_fanatic", "Lesser Fanatic", 3121, 3517));

    /** The NPC id of an area's taskmaster, or null. */
    public static String taskmaster(String area) {
        for (Taskmaster t : TASKMASTERS) {
            if (t.area().equals(area)) {
                return t.npcId();
            }
        }
        return null;
    }

    /** The name of an area's taskmaster ("the diary master" if none). */
    public static String taskmasterName(String area) {
        for (Taskmaster t : TASKMASTERS) {
            if (t.area().equals(area)) {
                return t.name();
            }
        }
        return "the diary master";
    }

    /** The area whose taskmaster this NPC is, or null. */
    public static String taskmasterArea(String npcId) {
        for (Taskmaster t : TASKMASTERS) {
            if (t.npcId().equals(npcId)) {
                return t.area();
            }
        }
        return null;
    }

    /** The water pump north of Falador's west bank (data_osrs object "Waterpump", 2947, 3382). */
    public static final String WATER_PUMP = "falador_water_pump";
    public static final int FALADOR_PUMP_X = 2947;
    public static final int FALADOR_PUMP_Y = 3382;
    /** The well in Rellekka (data_osrs object "Well", 2668, 3660). */
    public static final String WELL = "rellekka_well";
    public static final int RELLEKKA_WELL_X = 2668;
    public static final int RELLEKKA_WELL_Y = 3660;

    /** Containers a pump or well fills: {empty, full} (OSRS: buckets, jugs, bowls and vials). */
    public static final List<String[]> WATER_FILLS = List.of(new String[]{"minecraft:bucket", "minecraft:water_bucket"},
            new String[]{"jug", "jug_of_water"}, new String[]{"bowl", "bowl_of_water"}, new String[]{"vial", "vial_of_water"});

    /**
     * An item spawn picked up by hand (OSRS ground item spawns): the block that stands for it, the item, its respawn in game
     * ticks and its spots: OSRS tiles on the surface ({@code tiles}) and absolute block positions inside carved dungeons
     * ({@code blocks}, feet level). Taking it gives one item; the block comes back after the respawn time.
     */
    public record GroundSpawn(String block, String item, String name, int respawnTicks, List<int[]> tiles, List<int[]> blocks) {
    }

    /** Seaweed all around Karamja's coasts (wiki "Seaweed" spawn table, 100-tick respawn; spots in this world's sea are skipped). */
    public static final GroundSpawn SEAWEED = new GroundSpawn("seaweed_spawn", "seaweed", "Seaweed", 100, List.of(
            new int[]{2753, 3119}, new int[]{2755, 2952}, new int[]{2755, 3017}, new int[]{2755, 3049}, new int[]{2755, 3071},
            new int[]{2756, 3008}, new int[]{2756, 3075}, new int[]{2756, 3125}, new int[]{2757, 2947}, new int[]{2757, 3109},
            new int[]{2762, 3001}, new int[]{2771, 2995}, new int[]{2782, 2987}, new int[]{2786, 2960}, new int[]{2807, 3129},
            new int[]{2840, 2974}, new int[]{2854, 2977}, new int[]{2864, 3195}, new int[]{2866, 2976}, new int[]{2896, 3119},
            new int[]{2913, 3111}, new int[]{2926, 3110}, new int[]{2939, 3073}, new int[]{2940, 3102}, new int[]{2947, 3067},
            new int[]{2952, 2951}, new int[]{2955, 3010}, new int[]{2960, 2959}, new int[]{2975, 3013}), List.of());

    /**
     * Potato cacti in the Kalphite Lair (wiki "Potato cactus": three in the soldiers' south-west room, the rest along the west
     * wall of the Queen's chamber; 100-tick respawn). The lair is carved more compactly than OSRS's (the dungeons module's
     * Kalphite Lair, floor block y 18), so the spots are those rooms' corners as built. // approx
     */
    public static final GroundSpawn POTATO_CACTUS = new GroundSpawn("potato_cactus_spawn", "potato_cactus", "Potato cactus", 100,
            List.of(), List.of(new int[]{29, 19, 263}, new int[]{31, 19, 264}, new int[]{33, 19, 262}, new int[]{73, 19, 196},
            new int[]{73, 19, 200}, new int[]{73, 19, 204}, new int[]{73, 19, 208}, new int[]{73, 19, 211}));

    /**
     * Swamp toads at the Tree Gnome Stronghold (wiki "Swamp toad (item)" spawn table: the swamp enclosure and the south-western
     * area; the respawn isn't listed, so the other spawns' 100 ticks). // approx respawn
     */
    public static final GroundSpawn SWAMP_TOAD = new GroundSpawn("swamp_toad_spawn", "swamp_toad", "Swamp toad", 100, List.of(
            new int[]{2407, 3516}, new int[]{2424, 3514}, new int[]{2409, 3514}, new int[]{2411, 3512}, new int[]{2412, 3519},
            new int[]{2413, 3511}, new int[]{2415, 3518}, new int[]{2417, 3508}, new int[]{2421, 3509}, new int[]{2428, 3510},
            new int[]{2416, 3512}, new int[]{2417, 3512}, new int[]{2417, 3515}, new int[]{2417, 3516}, new int[]{2418, 3511},
            new int[]{2418, 3517}, new int[]{2421, 3519}, new int[]{2424, 3517}, new int[]{2406, 3405}, new int[]{2408, 3408},
            new int[]{2381, 3414}, new int[]{2382, 3412}, new int[]{2388, 3410}, new int[]{2399, 3406}, new int[]{2401, 3404}),
            List.of());
    /** The Gnome Stronghold around both toad areas (the Western Provinces task's circle). */
    public static final int[] GNOME_TOADS = {2405, 3460, 65};

    public static final List<GroundSpawn> GROUND_SPAWNS = List.of(SEAWEED, POTATO_CACTUS, SWAMP_TOAD);

    /** The ground spawn a block id stands for, or null. */
    public static GroundSpawn groundSpawn(String block) {
        for (GroundSpawn g : GROUND_SPAWNS) {
            if (g.block().equals(block)) {
                return g;
            }
        }
        return null;
    }

    /** What a pump or well turns this held item into, or null. */
    public static String waterFill(String held) {
        for (String[] f : WATER_FILLS) {
            if (f[0].equals(held)) {
                return f[1];
            }
        }
        return null;
    }
}
