package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;

import java.util.ArrayList;
import java.util.List;

/**
 * Thieving: more pickpocket targets (master farmer and his seed table, rogue, H.A.M. members,
 * knight of Ardougne, Fremennik citizen, Varlamore's wealthy citizens, watchman, menaphite thug,
 * paladin, gnome, hero, vyre, elf, TzHaar-Hur), thieving chests (including Dorgesh-Kaan's, which need
 * a lockpick, and the Civitas illa Fortis houses, which need a house key) and extra market stalls. Levels, XP, stun damage
 * and respawn times are OSRS values; success rates are the OSRS Wiki's skilling success chart low/high values
 * (snapshot: src/test/resources/osrs/skilling/success_rates.tsv).
 */
public final class ThievingData extends SkillingPart {

    /**
     * A pickpocket target (a module NPC). {@code always} is given on every success; one line of
     * {@code table} is rolled on top of it (an empty table gives only {@code always}).
     */
    public record Target(String npcId, String name, String place, int dx, int dz, int level, double xp, int low, int high,
                         int stunDamage, List<Loot> always, List<Loot> table, int[] colors) {
    }

    public static final List<Loot> SEED_TABLE = List.of(
            Loot.of("potato_seed", 1, 4, 300), Loot.of("onion_seed", 1, 3, 200), Loot.of("cabbage_seed", 1, 3, 150),
            Loot.of("tomato_seed", 1, 2, 100), Loot.of("sweetcorn_seed", 1, 2, 70), Loot.of("strawberry_seed", 1, 1, 40),
            Loot.of("watermelon_seed", 1, 1, 20), Loot.of("snape_grass_seed", 1, 1, 10), Loot.of("barley_seed", 1, 4, 120),
            Loot.of("hammerstone_seed", 1, 3, 100), Loot.of("asgarnian_seed", 1, 2, 80), Loot.of("jute_seed", 1, 3, 70),
            Loot.of("yanillian_seed", 1, 2, 50), Loot.of("krandorian_seed", 1, 2, 30), Loot.of("wildblood_seed", 1, 1, 20),
            Loot.of("redberry_seed", 60), Loot.of("cadavaberry_seed", 40), Loot.of("dwellberry_seed", 30),
            Loot.of("jangerberry_seed", 15), Loot.of("whiteberry_seed", 8), Loot.of("poison_ivy_seed", 5),
            Loot.of("guam_seed", 80), Loot.of("marrentill_seed", 60), Loot.of("tarromin_seed", 45), Loot.of("harralander_seed", 30),
            Loot.of("ranarr_seed", 9), Loot.of("toadflax_seed", 13), Loot.of("irit_seed", 9), Loot.of("avantoe_seed", 7),
            Loot.of("kwuarm_seed", 6), Loot.of("snapdragon_seed", 2), Loot.of("cadantine_seed", 2), Loot.of("lantadyme_seed", 2),
            Loot.of("dwarf_weed_seed", 1), Loot.of("torstol_seed", 1), Loot.of("mushroom_spore", 10), Loot.of("belladonna_seed", 4),
            Loot.of("cactus_seed", 4), Loot.of("potato_cactus_seed", 2));

    /**
     * The seed stall's table: allotment, flower and hop seeds (no herb or tree seeds, unlike the master
     * farmer). Weights approx.
     */
    public static final List<Loot> STALL_SEEDS = List.of(
            Loot.of("potato_seed", 1, 2, 300), Loot.of("onion_seed", 1, 2, 200), Loot.of("cabbage_seed", 1, 2, 150),
            Loot.of("tomato_seed", 1, 1, 100), Loot.of("sweetcorn_seed", 1, 1, 50), Loot.of("strawberry_seed", 1, 1, 25),
            Loot.of("watermelon_seed", 1, 1, 10), Loot.of("marigold_seed", 1, 1, 80), Loot.of("rosemary_seed", 1, 1, 40),
            Loot.of("nasturtium_seed", 1, 1, 40), Loot.of("woad_seed", 1, 1, 20), Loot.of("limpwurt_seed", 1, 1, 10),
            Loot.of("barley_seed", 1, 2, 100), Loot.of("hammerstone_seed", 1, 2, 80), Loot.of("asgarnian_seed", 1, 1, 60),
            Loot.of("jute_seed", 1, 2, 60), Loot.of("yanillian_seed", 1, 1, 40), Loot.of("krandorian_seed", 1, 1, 20),
            Loot.of("wildblood_seed", 1, 1, 10));

    /**
     * H.A.M. members' pickpocket table (15 Thieving, 22.2 XP): coins, ores, gems, tools and the
     * H.A.M. robes; an easy clue scroll is rolled separately at 1/50 ({@link #HAM_CLUE_CHANCE}).
     * Weights approx.; the rest of the H.A.M. outfit (cloak, logo, gloves, boots) and the junk items
     * (buttons, damaged armour) are not in the game; rusty swords go to Tindel Marchant.
     */
    public static final List<Loot> HAM_TABLE = List.of(
            Loot.of("coins", 1, 21, 28), Loot.of("iron_ore", 1, 1, 8), Loot.of("coal", 1, 1, 8), Loot.of("uncut_jade", 1, 1, 4),
            Loot.of("uncut_opal", 1, 1, 4), Loot.of("uncut_red_topaz", 1, 1, 2), Loot.of("needle", 1, 1, 6), Loot.of("thread", 1, 5, 6),
            Loot.of("tinderbox", 1, 1, 6), Loot.of("steel_arrow", 1, 13, 5), Loot.of("grimy_guam_leaf", 1, 1, 4),
            Loot.of("grimy_marrentill", 1, 1, 3), Loot.of("grimy_tarromin", 1, 1, 2), Loot.of("ham_hood", 1, 1, 3),
            Loot.of("ham_shirt", 1, 1, 3), Loot.of("ham_robe", 1, 1, 3), Loot.of("rusty_sword", 1, 1, 4)); // rusty sword 4/102 (wiki)
    public static final int HAM_CLUE_CHANCE = 50;
    public static final String HOUSE_KEY = "house_key";
    public static final String VALUABLES = "valuables";
    /** A house key opens a Civitas house for this many game ticks (3 minutes). */
    public static final int HOUSE_VISIT_TICKS = 300;

    public static final List<Target> TARGETS = List.of(
            new Target("ham_member", "H.A.M. Member", "quest_ham_hideout", 3, -2, 15, 22.2, 135, 239, 1,
                    List.of(), HAM_TABLE, new int[]{0xC8A080, 0xD8D0C0, 0xD8D0C0, 0xD8D0C0}),
            // Fremennik citizens of Rellekka: 45 Thieving, 65 XP, 40 coins (success rates and stun approx.).
            new Target("fremennik_citizen", "Fremennik citizen", "RELLEKKA", -4, 6, 45, 65.0, 65, 240, 2,
                    List.of(Loot.of("coins", 40, 40, 1)), List.of(), new int[]{0xE0B890, 0x7A5A3A, 0x5A4A3A, 0xC0A060}),
            // Varlamore's wealthy citizens: 50 Thieving, 96 XP, 35/200, coins and a house key 1/17.
            new Target("wealthy_citizen", "Wealthy citizen", "zeah_civitas_illa_fortis", 6, -6, 50, 96.0, 35, 200, 3,
                    List.of(Loot.of("coins", 85, 85, 1)), List.of(Loot.of(HOUSE_KEY, 1, 1, 1), Loot.of("coins", 1, 1, 16)),
                    new int[]{0xB08060, 0x7A3A8A, 0x4A2A5A, 0xE0C040}),
            new Target("rogue_pickpocket", "Rogue", "EDGEVILLE", 5, 4, 32, 36.5, 75, 240, 2,
                    List.of(), List.of(Loot.of("coins", 25, 120, 60), Loot.of("air_rune", 8, 8, 15), Loot.of("jug_of_wine", 10),
                    Loot.of("iron_dagger", 5), Loot.of("lockpick", 10)), new int[]{0xC8A080, 0x3A2A2A, 0x2A2A2A, 0x1A1A1A}),
            new Target("master_farmer", "Master Farmer", "DRAYNOR_VILLAGE", 4, -4, 38, 43.0, 90, 240, 3,
                    List.of(), SEED_TABLE, new int[]{0xC8A080, 0x4A7A2A, 0x6A4A2A, 0xC0A060}),
            new Target("knight_of_ardougne", "Knight of Ardougne", "ARDOUGNE", -5, 3, 55, 84.3, 50, 240, 3,
                    List.of(Loot.of("coins", 50, 50, 1)), List.of(), new int[]{0xC8A080, 0xA0A0A8, 0x6A6A70, 0x9A9AA0}),
            new Target("ardougne_paladin", "Paladin", "ARDOUGNE", 5, -3, 70, 131.8, 40, 170, 3,
                    List.of(Loot.of("coins", 80, 80, 1)), List.of(Loot.of("chaos_rune", 2, 2, 1), Loot.of("coins", 1, 1, 7)),
                    new int[]{0xC8A080, 0xE0E0E8, 0xC0C0C8, 0xE0C040}),
            new Target("watchman", "Watchman", "YANILLE", 4, 4, 65, 137.5, 15, 160, 3,
                    List.of(Loot.of("coins", 60, 60, 1)), List.of(Loot.of("bread", 1, 1, 1), Loot.of("coins", 1, 1, 3)),
                    new int[]{0xC8A080, 0x3A5A8A, 0x2A3A5A, 0x2A2A2A}),
            new Target("menaphite_thug", "Menaphite Thug", "POLLNIVNEACH", 3, -8, 65, 137.5, 50, 160, 5,
                    List.of(Loot.of("coins", 60, 60, 1)), List.of(), new int[]{0xA07850, 0x8A2A2A, 0x5A3A2A, 0x2A2A2A}),
            // Pollnivneach bandits (blackjacking targets in the north of the town; success rates approx, the wiki has no chart).
            new Target("bearded_pollnivneach_bandit", "Bearded Pollnivneach bandit", "POLLNIVNEACH", -4, 10, 45, 65.0, 60, 240, 5,
                    List.of(Loot.of("coins", 40, 40, 1)), List.of(), new int[]{0xA07850, 0x6A5A3A, 0x4A3A2A, 0x3A2A1A}),
            new Target("pollnivneach_bandit", "Pollnivneach bandit", "POLLNIVNEACH", 4, 12, 55, 84.3, 60, 240, 5,
                    List.of(Loot.of("coins", 50, 50, 1)), List.of(), new int[]{0xA07850, 0x7A6A4A, 0x4A3A2A, 0x2A2A2A}),
            new Target("gnome_pickpocket", "Gnome", "GNOME_STRONGHOLD", 4, 5, 75, 133.3, 43, 175, 1,
                    List.of(), List.of(Loot.of("coins", 300, 300, 40), Loot.of("earth_rune", 1, 1, 15), Loot.of("gold_ore", 10),
                    Loot.of("fire_orb", 5), Loot.of("uncut_sapphire", 10), Loot.of("mithril_ore", 10)),
                    new int[]{0xC8A080, 0x3A8A3A, 0x6A4A2A, 0x9A2A2A}),
            new Target("ardougne_hero", "Hero", "ARDOUGNE", 0, 6, 80, 163.3, 39, 160, 3,
                    List.of(), List.of(Loot.of("coins", 200, 300, 70), Loot.of("death_rune", 2, 2, 8), Loot.of("blood_rune", 1, 1, 6),
                    Loot.of("fire_orb", 4), Loot.of("diamond", 2), Loot.of("gold_ore", 4), Loot.of("jug_of_wine", 6)),
                    new int[]{0xC8A080, 0x7A2A2A, 0x3A3A3A, 0xE0C040}),
            new Target("vyre_pickpocket", "Vyre", "CANIFIS", -4, 5, 82, 306.9, 8, 128, 5,
                    List.of(), List.of(Loot.of("coins", 250, 250, 60), Loot.of("death_rune", 4, 4, 15), Loot.of("diamond", 4),
                    Loot.of("uncut_ruby", 6), Loot.of("vial_of_blood", 10)), new int[]{0xB0B0B8, 0x4A1A2A, 0x2A1A1A, 0x1A1A1A}),
            new Target("elf_pickpocket", "Elf", "LLETYA", 4, -4, 85, 353.3, 6, 100, 5,
                    List.of(), List.of(Loot.of("coins", 280, 350, 60), Loot.of("death_rune", 2, 2, 10), Loot.of("nature_rune", 3, 3, 10),
                    Loot.of("diamond", 3), Loot.of("gold_ore", 5), Loot.of("fire_orb", 4), Loot.of("crystal_dust", 3, 6, 4)),
                    new int[]{0xE0D0B0, 0x3A8A5A, 0x2A5A3A, 0xE0E0A0}),
            // Digsite workmen (25 Thieving, 10.4 XP, 1 damage): loose coins, not a pouch. The wiki's table also has a
            // specimen brush (3/11), a bucket and an animal skull (3/11, The Dig Site), which are other modules' items. The
            // wiki has no success chart: low/high approx.
            new Target("digsite_workman", "Digsite workman", "digsite_workmen", 0, 0, 25, 10.4, 120, 240, 1,
                    List.of(), List.of(Loot.of("coins", 10, 10, 4), Loot.of("rope", 1), Loot.of("leather_gloves", 1), Loot.of("spade", 1)),
                    new int[]{0xC8A080, 0x8A6A3A, 0x5A4A3A, 0xC0A060}),
            // Pollnivneach villagers (30 Thieving, 8 XP, 2 damage, 5 coins): blackjack targets after The Feud. Low/high approx.
            new Target("pollnivneach_villager", "Villager", "POLLNIVNEACH", 2, 3, 30, 8.0, 110, 240, 2,
                    List.of(Loot.of("coins", 5, 5, 1)), List.of(), new int[]{0xA07850, 0xC0A880, 0x8A7A5A, 0xE0D8C0}),
            // Pirates of the Sailing islands (60 Thieving, 72 XP, 3 damage; OSRS Wiki "Pirate (Thieving)", spawn tiles from its
            // location table): the Onyx Crest, Laguna Aurorae and the castle on Buccaneers' Haven. Loot: 20 coins (170/200)
            // or a steel cannonball (5/200); the bronze and iron cannonballs (24/200), elkhorn frag and the Onyx Crest's
            // medallion fragment are not in the game. The wiki has no success chart: low/high approx.
            pirate("onyx_crest_pirate", "onyx_crest_pirates", 0xA07850, 0x8A2A2A),
            pirate("laguna_aurorae_pirate", "laguna_aurorae_pirates", 0xC8A080, 0x2A4A8A),
            pirate("buccaneers_haven_pirate", "buccaneers_haven_pirates", 0x8A6A4A, 0x2A2A2A),
            new Target("tzhaar_hur", "TzHaar-Hur", "MOR_UL_REK", 5, 5, 90, 103.4, -200, 200, 4,
                    List.of(), List.of(Loot.of("tokkul", 3, 14, 70), Loot.of("uncut_sapphire", 6), Loot.of("uncut_emerald", 4),
                    Loot.of("uncut_ruby", 2), Loot.of("uncut_diamond", 1)), new int[]{0x8A3A1A, 0x5A2A1A, 0x4A2A1A, 0xE06010}));

    private static Target pirate(String npcId, String place, int skin, int shirt) {
        return new Target(npcId, "Pirate", place, 0, 0, 60, 72.0, 40, 200, 3, List.of(),
                List.of(Loot.of("coins", 20, 20, 170), Loot.of("cannonball", 1, 1, 5)), new int[]{skin, shirt, 0x3A2A1A, 0x1A1A1A});
    }

    /** Targets that can be knocked out with a blackjack first (Pollnivneach, after The Feud). */
    public static final List<String> BLACKJACK_TARGETS = List.of("menaphite_thug", "bearded_pollnivneach_bandit", "pollnivneach_bandit",
            "pollnivneach_villager");
    /** Ordinary blackjacks (no Attack requirement), sold by the blackjack seller in Pollnivneach. */
    public static final List<String> BLACKJACKS = List.of("oak_blackjack", "willow_blackjack");
    public static final String BLACKJACK_SELLER = "pollnivneach_blackjack_seller";
    /** Game ticks a knocked-out target stays unconscious; pickpockets on it always succeed (approx.). */
    public static final int KNOCKOUT_TICKS = 4;

    /**
     * Knock-out low/high: the OSRS Wiki's Menaphite thug chart is 78/240; the bandits use the same values (approx).
     */
    public static final int KNOCKOUT_LOW = 78;
    public static final int KNOCKOUT_HIGH = 240;

    /** Chance a knock-out works; a failure hits the player like a failed pickpocket. */
    public static double knockoutChance(Target t, int thievingLevel) {
        return com.runecraft.osrs.Chance.lerp(thievingLevel, KNOCKOUT_LOW, KNOCKOUT_HIGH);
    }

    public static Target target(String npcId) {
        for (Target t : TARGETS) {
            if (t.npcId().equals(npcId)) {
                return t;
            }
        }
        return null;
    }

    /** A thieving chest or stall block: all of {@code always} plus one {@code table} roll; respawn in game ticks. */
    public record Container(String blockId, String name, boolean chest, int level, double xp, int respawnTicks, List<Loot> always,
                            List<Loot> table, String look, int color) {
    }

    /** The Isle of Souls Dungeon chest and the Dark Chest it gives keys for. */
    public static final String SOUL_CHEST = "isle_of_souls_chest";
    public static final String DARK_CHEST = "dark_chest";
    public static final String DARK_KEY = "dark_key";
    /** Containers that are opened with a key, which is used up: block -> key item. */
    public static final java.util.Map<String, String> KEY_CONTAINERS = java.util.Map.of(DARK_CHEST, DARK_KEY);

    /**
     * Picklock chances: block -> {low, high} on the 1-99 curve without and {low, high} with a lockpick carried
     * (Mod Ash: the Isle of Souls chest is -23/153, +26 with a lockpick). A failure has a 1/6 chance of the
     * chest's trap. Other containers always open.
     */
    public static final java.util.Map<String, int[]> PICKLOCK = java.util.Map.of(SOUL_CHEST, new int[]{-23, 153, 3, 179});
    public static final int TRAP_CHANCE = 6;

    /** Chance to open a container at a Thieving level (1 for containers that always open). */
    public static double openChance(String blockId, int thieving, boolean lockpick) {
        int[] lh = PICKLOCK.get(blockId);
        if (lh == null) {
            return 1.0;
        }
        return lockpick ? com.runecraft.osrs.Chance.lerp(thieving, lh[2], lh[3]) : com.runecraft.osrs.Chance.lerp(thieving, lh[0], lh[1]);
    }

    /** Containers that need a lockpick (Dorgesh-Kaan's chests). */
    public static final List<String> LOCKPICK_CONTAINERS = List.of("dorgesh_kaan_average_chest", "dorgesh_kaan_rich_chest");
    /** The Civitas illa Fortis house furniture (Stealing valuables): needs a house key to get in. */
    public static final List<String> HOUSE_CONTAINERS = List.of("civitas_house_chest", "civitas_house_wardrobe", "civitas_jewellery_case");

    public static final List<Container> CONTAINERS = List.of(
            new Container("ten_coin_chest", "Chest (10 coins)", true, 13, 7.8, 7, List.of(Loot.of("coins", 10, 10, 1)), List.of(),
                    "chest", 0x8A6A3A),
            new Container("nature_rune_chest", "Chest (nature runes)", true, 28, 25.0, 13,
                    List.of(Loot.of("coins", 3, 3, 1), Loot.of("nature_rune", 1, 1, 1)), List.of(), "chest", 0x8A6A3A),
            new Container("fifty_coin_chest", "Chest (50 coins)", true, 43, 125.0, 83, List.of(Loot.of("coins", 50, 50, 1)), List.of(),
                    "chest", 0x8A6A3A),
            new Container("arrowtip_chest", "Chest (steel arrowtips)", true, 47, 150.0, 130,
                    List.of(Loot.of("coins", 20, 20, 1), Loot.of("steel_arrowtips", 5, 5, 1)), List.of(), "chest", 0x8A6A3A),
            new Container("blood_rune_chest", "Chest (blood runes)", true, 59, 250.0, 225,
                    List.of(Loot.of("coins", 500, 500, 1), Loot.of("blood_rune", 2, 2, 1)), List.of(), "chest", 0x6A2A2A),
            new Container("ardougne_castle_chest", "Chest (Ardougne castle)", true, 72, 500.0, 666,
                    List.of(Loot.of("coins", 1000, 1000, 1), Loot.of("raw_shark", 1, 1, 1), Loot.of("adamantite_ore", 1, 1, 1),
                            Loot.of("uncut_sapphire", 1, 1, 1)), List.of(), "chest", 0xA08040),
            new Container("rogues_castle_chest", "Chest (Rogues' Castle)", true, 84, 701.7, 15, List.of(),
                    List.of(Loot.of("coins", 100, 500, 50), Loot.of("raw_shark", 1, 3, 15), Loot.of("death_rune", 10, 30, 10),
                            Loot.of("uncut_diamond", 6), Loot.of("uncut_ruby", 8), Loot.of("dragonstone", 1), Loot.of("rune_dagger", 2),
                            Loot.of("adamantite_ore", 1, 4, 8)), "chest", 0x5A4A3A),
            // Dorgesh-Kaan chests (a lockpick needed; OSRS Wiki): average 52 Thieving, 200 XP, back after 90 s; rich 78, 650 XP,
            // 300 s. Loot: the average chest's main table is 3/7 coins (1-250) and 1/7 each of an oil lantern, bullseye
            // lantern, mining helmet and cave goblin wire; the rich chest's 15 equal lines are gems, lanterns, frog-leather,
            // an iron bar, wire and light orbs. Approx: lines whose items aren't in the game are left out.
            new Container("dorgesh_kaan_average_chest", "Chest (Dorgesh-Kaan, average)", true, 52, 200.0, 150,
                    List.of(), List.of(Loot.of("coins", 1, 250, 3)), "chest", 0x6A6A5A),
            new Container("dorgesh_kaan_rich_chest", "Chest (Dorgesh-Kaan, rich)", true, 78, 650.0, 500,
                    List.of(), List.of(Loot.of("uncut_sapphire", 1), Loot.of("uncut_emerald", 1), Loot.of("uncut_ruby", 1),
                    Loot.of("uncut_diamond", 1), Loot.of("uncut_opal", 1), Loot.of("uncut_jade", 1), Loot.of("uncut_red_topaz", 1),
                    Loot.of("iron_bar", 1), Loot.of("empty_light_orb", 1)), "chest", 0x8A7A4A),
            // Isle of Souls Dungeon chest (OSRS Wiki): 28 Thieving, 150 XP, loot back after 2 ticks; one of two sources of dark
            // keys. Weights out of 200 (OSRS gives nothing when the inventory is too full).
            new Container(SOUL_CHEST, "Chest (Isle of Souls)", true, 28, 150.0, 2, List.of(),
                    List.of(Loot.of("coins", 90, 190, 20), Loot.of("minecraft:feather", 30, 80, 20),
                            Loot.of("chocolate_bar", 6), Loot.of("bird_snare", 1, 2, 6), Loot.of("box_trap", 1, 2, 6),
                            Loot.of("grimy_guam_leaf", 1, 2, 20), Loot.of("grimy_lantadyme", 12), Loot.of("grimy_ranarr_weed", 10),
                            Loot.of("uncut_sapphire", 1, 2, 8), Loot.of("uncut_emerald", 1, 2, 8), Loot.of("uncut_ruby", 1, 2, 6),
                            Loot.of("uncut_diamond", 1, 2, 4), Loot.of("mithril_pickaxe", 6), Loot.of("mithril_axe", 6),
                            Loot.of("adamant_scimitar", 4), Loot.of("mind_rune", 1, 10, 15), Loot.of("nature_rune", 1, 2, 20),
                            Loot.of("death_rune", 1, 2, 20), Loot.of("clue_scroll_medium", 2), Loot.of(DARK_KEY, 1)), "chest", 0x5A4A6A),
            // The Dark Chest in the Crumbling Tower's basement (OSRS Wiki): a dark key and 28 Thieving, 1,500 XP. Weights "out of
            // 600" (the wiki's lines add up to 603); herbs, hides and bones come noted in OSRS.
            new Container(DARK_CHEST, "Dark Chest", true, 28, 1500.0, 2, List.of(),
                    List.of(Loot.of("shield_left_half", 1), Loot.of("dragon_plateskirt", 1), Loot.of("dragon_platelegs", 1),
                            Loot.of("rune_pickaxe", 15), Loot.of("rune_axe", 15), Loot.of("rune_longsword", 5), Loot.of("rune_kiteshield", 10),
                            Loot.of("rune_chainbody", 10), Loot.of("mind_rune", 400, 600, 25), Loot.of("chaos_rune", 300, 500, 25),
                            Loot.of("blood_rune", 100, 200, 25), Loot.of("nature_rune", 150, 350, 25), Loot.of("law_rune", 100, 300, 27),
                            Loot.of("death_rune", 200, 400, 27), Loot.of("adamant_arrow", 200, 400, 40), Loot.of("rune_arrow", 100, 300, 30),
                            Loot.of("grimy_dwarf_weed", 10, 15, 25), Loot.of("grimy_torstol", 10, 15, 25),
                            Loot.of("grimy_ranarr_weed", 10, 15, 25), Loot.of("grimy_snapdragon", 10, 15, 25),
                            Loot.of("green_dragon_leather", 5, 10, 20), Loot.of("blue_dragon_leather", 5, 10, 20),
                            Loot.of("red_dragon_leather", 5, 10, 20), Loot.of("black_dragon_leather", 5, 10, 20),
                            Loot.of("babydragon_bones", 10, 15, 35), Loot.of("dragon_bones", 10, 15, 20),
                            Loot.of("coins", 9500, 29500, 69), Loot.of("dragonstone", 10), Loot.of(DARK_KEY, 2, 2, 7)), "chest", 0x2A2A3A),
            // Stealing valuables (Civitas illa Fortis houses, 50 Thieving, 45 XP a theft): valuables, now and then a piece
            // of jewellery (amounts and weights approx.).
            new Container("civitas_house_chest", "Chest (Civitas house)", true, 50, 45.0, 2, List.of(Loot.of(VALUABLES, 1, 2, 1)),
                    List.of(Loot.of("coins", 1, 1, 30), Loot.of("gold_ring", 1, 1, 1)), "chest", 0x9A6A3A),
            new Container("civitas_house_wardrobe", "Wardrobe (Civitas house)", true, 50, 45.0, 2, List.of(Loot.of(VALUABLES, 1, 2, 1)),
                    List.of(Loot.of("coins", 1, 1, 30), Loot.of("sapphire_ring", 1, 1, 1)), "crate", 0x7A4A2A),
            new Container("civitas_jewellery_case", "Jewellery case (Civitas house)", true, 50, 45.0, 2,
                    List.of(Loot.of(VALUABLES, 1, 3, 1)), List.of(Loot.of("coins", 1, 1, 20), Loot.of("emerald_necklace", 1, 1, 1)),
                    "chest", 0xC0A050),
            new Container("vegetable_stall", "Vegetable stall", false, 2, 10.0, 2, List.of(),
                    List.of(Loot.of("potato", 1), Loot.of("onion", 1), Loot.of("cabbage", 1), Loot.of("tomato", 1), Loot.of("garlic", 1)),
                    "stall", 0x6A9A3A),
            new Container("tea_stall", "Tea stall", false, 5, 16.0, 4, List.of(), List.of(Loot.of("cup_of_tea", 1)), "stall", 0xA06030),
            new Container("wine_stall", "Wine stall", false, 22, 27.0, 8, List.of(),
                    List.of(Loot.of("jug_of_wine", 2), Loot.of("grapes", 3), Loot.of("jug_of_water", 3), Loot.of("jug_of_bad_wine", 2),
                            Loot.of("jug", 2)), "stall", 0x8A1A3A),
            // Hosidius fruit stall: 25 Thieving, 28.5 XP, respawns in 2.4 s (weights approx; lemons/limes/strange fruit
            // are not in the game yet).
            new Container("fruit_stall", "Fruit stall", false, 25, 28.5, 4, List.of(),
                    List.of(Loot.of("cooking_apple", 40), Loot.of("banana", 20), Loot.of("strawberry", 10), Loot.of("jangerberries", 8),
                            Loot.of("redberries", 8), Loot.of("pineapple", 6), Loot.of("papaya_fruit", 4), Loot.of("orange", 4)),
                    "stall", 0xC08030),
            new Container("seed_stall", "Seed stall", false, 27, 10.0, 4, List.of(), STALL_SEEDS, "stall", 0x4A7A2A),
            new Container("fish_stall", "Fish stall", false, 42, 42.0, 12, List.of(),
                    List.of(Loot.of("raw_salmon", 4), Loot.of("raw_tuna", 3), Loot.of("raw_lobster", 2)), "stall", 0x5A80A0),
            new Container("crossbow_stall", "Crossbow stall", false, 49, 52.0, 8, List.of(),
                    // Crossbow stall (wiki, out of 80): bronze bolts (3) 46, bronze limbs 18, wooden stock 12, mithril bolts (3) 3,
                    // mithril limbs 1 (the wiki lists 3 limbs/stocks a theft; 1 here, approx).
                    List.of(Loot.of("bronze_bolts", 3, 3, 46), Loot.of("bronze_limbs", 18), Loot.of("wooden_stock", 12),
                            Loot.of("mithril_bolts", 3, 3, 3), Loot.of("mithril_limbs", 1)), "stall", 0x8A6A3A),
            new Container("magic_stall", "Magic stall", false, 65, 90.0, 12, List.of(),
                    // Magic stall (wiki, out of 20): air, earth and fire runes 6 each, law and nature runes 1 each.
                    List.of(Loot.of("air_rune", 6), Loot.of("earth_rune", 6), Loot.of("fire_rune", 6), Loot.of("law_rune", 1),
                            Loot.of("nature_rune", 1)), "stall", 0x5A3A8A),
            new Container("scimitar_stall", "Scimitar stall", false, 65, 210.0, 32, List.of(),
                    // Scimitar stall (wiki, out of 40): iron 23, steel 13, mithril 3, adamant 1.
                    List.of(Loot.of("iron_scimitar", 23), Loot.of("steel_scimitar", 13), Loot.of("mithril_scimitar", 3),
                            Loot.of("adamant_scimitar", 1)), "stall", 0x8A8A9A),
            // Keldagrim's market (OSRS Wiki): the Carefree Crafting Stall (5 Thieving, 20 XP, restocks in 4.8 s; 54ths), Keldagrim's
            // Best Bread (a bakery stall: 5 Thieving, 16 XP, bread 5/20, cake 13/20, chocolate slice 2/20; restocks in 9.6 s)
            // and Green Gemstone Gems (75 Thieving, 408 XP, restocks in 60 s).
            new Container("keldagrim_crafting_stall", "Crafting stall", false, 5, 20.0, 8, List.of(),
                    List.of(Loot.of("amulet_mould", 12), Loot.of("bracelet_mould", 12), Loot.of("chisel", 6), Loot.of("gold_bar", 2),
                            Loot.of("necklace_mould", 11), Loot.of("ring_mould", 11)), "stall", 0x8A7A5A),
            new Container("keldagrim_bakery_stall", "Bakery stall", false, 5, 16.0, 16, List.of(),
                    List.of(Loot.of("bread", 5), Loot.of("cake", 13), Loot.of("chocolate_slice", 2)), "stall", 0xC09A5A),
            new Container("keldagrim_gem_stall", "Gem stall", false, 75, 408.0, 100, List.of(),
                    List.of(Loot.of("uncut_sapphire", 105), Loot.of("uncut_emerald", 17), Loot.of("uncut_ruby", 5),
                            Loot.of("uncut_diamond", 1)), "stall", 0x3A8A5A),
            // Ore stall: 82 Thieving, 350 XP, restocks in 30 s (OSRS: Mor Ul Rek's ore counter; weights approx).
            new Container("ore_stall", "Ore stall", false, 82, 350.0, 50, List.of(),
                    List.of(Loot.of("iron_ore", 30), Loot.of("silver_ore", 15), Loot.of("coal", 30), Loot.of("gold_ore", 12),
                            Loot.of("mithril_ore", 8), Loot.of("adamantite_ore", 4), Loot.of("runite_ore", 1)), "stall", 0x6A6A6A));

    public static Container container(String blockId) {
        for (Container c : CONTAINERS) {
            if (c.blockId().equals(blockId)) {
                return c;
            }
        }
        return null;
    }

    @Override
    public void items(ItemSink s) {
        tool(s, "lockpick", "Lockpick", "key", 0x8A8A9A, 20);
        for (com.runecraft.osrs.data.Thieving.CoinPouch p : com.runecraft.osrs.data.Thieving.COIN_POUCHES) {
            simple(s, p.itemId(), "Coin pouch", "pouch", 0x9A7A3A, 0)
                    .examine("Coins stolen from " + p.npc() + ". Open it to take them out.");
        }
        tool(s, "oak_blackjack", "Oak blackjack", "club", 0x8A6A3A, 72).examine("A cosh for knocking people out.");
        simple(s, HOUSE_KEY, "House key", "key", 0xC0A050, 0).stack(Integer.MAX_VALUE)
                .examine("Opens a house in Civitas illa Fortis - when the owner is out.");
        simple(s, VALUABLES, "Valuables", "gem", 0xE0C060, 100).stack(Integer.MAX_VALUE) // value approx.
                .examine("Stolen from a house in Civitas illa Fortis.");
        tool(s, "willow_blackjack", "Willow blackjack", "club", 0x9A8A4A, 192).examine("A cosh for knocking people out.");
        simple(s, DARK_KEY, "Dark key", "key", 0x3A2A4A, 1).examine("Opens the Dark Chest in the Crumbling Tower.");
    }

    @Override
    public List<NpcDef> npcs() {
        List<NpcDef> list = new ArrayList<>();
        list.add(NpcDef.of(BLACKJACK_SELLER, "Blackjack seller", "POLLNIVNEACH", -6, -4, 0xA07850, 0x5A4A6A, 0x3A2A2A, 0x2A2A2A));
        for (Target t : TARGETS) {
            int[] c = t.colors();
            list.add(NpcDef.of(t.npcId(), t.name(), t.place(), t.dx(), t.dz(), c[0], c[1], c[2], c[3]));
        }
        return list;
    }

    @Override
    public List<BlockDef> blocks() {
        List<BlockDef> list = new ArrayList<>();
        for (Container c : CONTAINERS) {
            list.add(BlockDef.scripted(c.blockId(), c.name(), c.color(), c.look()));
        }
        return list;
    }

    @Override
    public List<PlaceDef> places() {
        return List.of(
                PlaceDef.of("ardougne_chests", "Ardougne chests", 2671, 3299, PlaceDef.Style.TOWN,
                        List.of("ten_coin_chest", "nature_rune_chest", "fifty_coin_chest")),
                PlaceDef.of("ardougne_castle", "Ardougne Castle", 2588, 3291, PlaceDef.Style.TOWN, List.of("ardougne_castle_chest")),
                PlaceDef.of("rellekka_arrowtip_chest", "Rellekka chest", 2648, 3660, PlaceDef.Style.CAMP, List.of("arrowtip_chest", "fish_stall")),
                PlaceDef.of("morytania_blood_chest", "Sanguinesti chest", 3585, 3360, PlaceDef.Style.RUINS, List.of("blood_rune_chest")),
                PlaceDef.of("rogues_castle_chests", "Rogues' Castle", 3286, 3930, PlaceDef.Style.RUINS,
                        List.of("rogues_castle_chest", "rogues_castle_chest")),
                PlaceDef.of("draynor_market", "Draynor market", 3080, 3250, PlaceDef.Style.TOWN,
                        List.of("seed_stall", "wine_stall", "vegetable_stall")),
                PlaceDef.of("hosidius_fruit_stalls", "Hosidius fruit stalls", 1800, 3607, PlaceDef.Style.CAMP,
                        List.of("fruit_stall", "fruit_stall")),
                PlaceDef.of("varrock_tea_stall", "Varrock tea stall", 3270, 3410, PlaceDef.Style.TOWN, List.of("tea_stall")),
                PlaceDef.of("keldagrim_stalls", "Keldagrim market", 2765, 3712, PlaceDef.Style.CAVE,
                        List.of("crossbow_stall", "blast_furnace_stairs", "keldagrim_crafting_stall", "keldagrim_bakery_stall",
                                "keldagrim_gem_stall")),
                PlaceDef.of("ape_atoll_stalls", "Ape Atoll market", 2760, 2785, PlaceDef.Style.CAMP,
                        List.of("magic_stall", "scimitar_stall")),
                PlaceDef.of("lovakengj_ore_stall", "Lovakengj ore stall", 1490, 3770, PlaceDef.Style.TOWN, List.of("ore_stall")),
                // The houses burgled in Stealing valuables (Lavinia's, Victor's and Caius'), approx.: one spot just west of the city walls, as the city structure fills the town.
                // Isle of Souls: the dungeon chests by the dungeon entrance (2309, 2919 in OSRS) and the Dark Chest in the Crumbling
                // Tower basement (2131, 2994). Approx: both on the surface and pulled inside the approximate island outline.
                PlaceDef.of("isle_of_souls_chests", "Isle of Souls Dungeon chests", 2265, 2915, PlaceDef.Style.CAVE,
                        List.of(SOUL_CHEST, SOUL_CHEST, SOUL_CHEST)),
                PlaceDef.of("crumbling_tower_dark_chest", "Crumbling Tower", 2180, 2935, PlaceDef.Style.RUINS, List.of(DARK_CHEST)),
                PlaceDef.of("civitas_houses", "Civitas houses", 1612, 3112, PlaceDef.Style.TOWN,
                        List.of("civitas_house_chest", "civitas_house_wardrobe", "civitas_jewellery_case")),
                // The Digsite workmen's spawns (cache tiles 3350-3373, 3385-3429); one stands at their centre.
                PlaceDef.of("digsite_workmen", "Digsite", 3362, 3408, PlaceDef.Style.NONE, List.of()),
                // The Sailing islands' pirates: the centre of each island's pirate spawn tiles (wiki).
                PlaceDef.of("onyx_crest_pirates", "The Onyx Crest pirate camp", 2981, 2287, PlaceDef.Style.NONE, List.of()),
                PlaceDef.of("laguna_aurorae_pirates", "Laguna Aurorae pirates", 1164, 2781, PlaceDef.Style.NONE, List.of()),
                PlaceDef.of("buccaneers_haven_pirates", "Buccaneers' Haven castle", 2096, 3684, PlaceDef.Style.NONE, List.of()));
    }
}
