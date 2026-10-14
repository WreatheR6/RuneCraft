package com.runecraft.osrs.content.npcs;

import com.runecraft.osrs.content.npcs.TownNpc.Role;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shopkeepers, bankers, travel NPCs and flavour characters at their OSRS tiles. Coordinates are the
 * NPC's usual spot in OSRS (shop counter, bank booth row, dock); a few NPCs who live underground or
 * upstairs in OSRS stand at the nearest surface spot instead (noted inline).
 */
public final class Townsfolk {
    private static final int SKIN = 0xC8A080;
    private static final int TAN = 0xA07850;
    private static final int DARK = 0x7A5032;
    private static final int GNOME = 0xC09070;
    private static final int ELF = 0xE0D0B0;
    private static final int GHOST = 0xB8D0C8;
    private static final int WOLF = 0x8A7A6A;
    private static final int DWARF = 0xC08A68;

    private static final List<TownNpc> ALL = new ArrayList<>();

    /**
     * Characters from other modules (or built-in quest NPCs) who also run a shop: sneak + right-click
     * them to trade. NPC id -> shop id.
     */
    public static final Map<String, String> EXTRA_TRADERS = new LinkedHashMap<>();

    private Townsfolk() {
    }

    public static List<TownNpc> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static TownNpc get(String id) {
        for (TownNpc n : ALL) {
            if (n.id().equals(id)) {
                return n;
            }
        }
        return null;
    }

    private static int[] look(int skin, int top, int bottom, int hat) {
        return new int[]{skin, top, bottom, hat};
    }

    private static void keeper(String id, String name, String area, int x, int y, String shop, int[] look) {
        ALL.add(new TownNpc(id, name, area, x, y, Role.SHOP, shop, null, List.of(), look));
    }

    private static void banker(String id, String area, int x, int y, int[] look) {
        ALL.add(new TownNpc(id, "Banker", area, x, y, Role.BANKER, null, null,
                List.of("Good day, how may I help you?"), look));
    }

    private static void bankers(String key, String area, int x1, int y1, int x2, int y2) {
        banker("npc_banker_" + key + "_1", area, x1, y1, look(SKIN, 0x2A3A6A, 0x2A2A3A, 0x3A2A1A));
        banker("npc_banker_" + key + "_2", area, x2, y2, look(SKIN, 0x6A2A2A, 0x2A2A3A, 0x1A1A1A));
    }

    private static void ambient(String id, String name, String area, int x, int y, int[] look, String... lines) {
        ALL.add(new TownNpc(id, name, area, x, y, Role.AMBIENT, null, null, List.of(lines), look));
    }

    private static void travel(String id, String name, String area, int x, int y, String route, int[] look, String line) {
        ALL.add(new TownNpc(id, name, area, x, y, Role.TRAVEL, null, route, List.of(line), look));
    }

    private static void tanner(String id, String name, String area, int x, int y, int[] look, String line) {
        ALL.add(new TownNpc(id, name, area, x, y, Role.TANNER, null, null, List.of(line), look));
    }

    static {
        int[] shopkeeper = look(SKIN, 0x5A6A3A, 0x4A3A2A, 0x3A2A1A);
        int[] assistant = look(SKIN, 0x3A5A7A, 0x4A3A2A, 0x6A4A2A);
        int[] smith = look(SKIN, 0x5A4A3A, 0x3A3A3A, 0x2A2A2A);
        int[] armoured = look(SKIN, 0x9A9AA0, 0x6A6A70, 0x5A3A2A);
        int[] desert = look(TAN, 0xE0D8C0, 0xC0B8A0, 0xE0D8C0);
        int[] karamja = look(DARK, 0xC0A040, 0x5A4A2A, 0x1A1A1A);
        int[] bartender = look(SKIN, 0xE0E0E0, 0x3A3A3A, 0x6A4A2A);
        int[] sailor = look(SKIN, 0x2A3A6A, 0x3A3A4A, 0x1A1A2A);
        int[] gnome = look(GNOME, 0x3A8A3A, 0x6A4A2A, 0x9A2A2A);
        int[] elf = look(ELF, 0x3A8A5A, 0x2A5A3A, 0xE0E0A0);
        int[] ghost = look(GHOST, 0x8AA0A0, 0x6A8080, 0x5A7070);
        int[] werewolf = look(WOLF, 0x5A4A3A, 0x4A3A2A, 0x3A2A1A);
        int[] fremennik = look(SKIN, 0x8A6A3A, 0x5A4A2A, 0xC0A060);
        int[] wizard = look(SKIN, 0x3A3A9A, 0x2A2A7A, 0x3A3A9A);
        int[] dwarf = look(DWARF, 0x6A3A1A, 0x4A3A2A, 0xA04020);

        // ------------------------------------------------------------- general stores
        keeper("npc_lumbridge_shopkeeper", "Shop keeper", "Lumbridge", 3211, 3247, "lumbridge_general_store", shopkeeper);
        keeper("npc_varrock_shopkeeper", "Shop keeper", "Varrock", 3217, 3415, "varrock_general_store", shopkeeper);
        keeper("npc_falador_shopkeeper", "Shop keeper", "Falador", 2958, 3388, "falador_general_store", shopkeeper);
        keeper("npc_edgeville_shopkeeper", "Shop keeper", "Edgeville", 3080, 3509, "edgeville_general_store", shopkeeper);
        keeper("npc_al_kharid_shopkeeper", "Shop keeper", "Al Kharid", 3315, 3180, "al_kharid_general_store", desert);
        keeper("npc_rimmington_shopkeeper", "Shop keeper", "Rimmington", 2948, 3214, "rimmington_general_store", shopkeeper);
        keeper("npc_karamja_shopkeeper", "Shop keeper", "Musa Point", 2905, 3147, "karamja_general_store", karamja);
        keeper("npc_chadwell", "Chadwell", "West Ardougne", 2465, 3285, "west_ardougne_general_store", look(SKIN, 0x5A5A4A, 0x3A3A2A, 0x2A2A2A));
        keeper("npc_port_khazard_shopkeeper", "Shop keeper", "Port Khazard", 2658, 3153, "port_khazard_general_store", shopkeeper);
        keeper("npc_fidelio", "Fidelio", "Canifis", 3508, 3496, "canifis_general_store", werewolf);
        keeper("npc_phasmatys_shopkeeper", "Ghost shopkeeper", "Port Phasmatys", 3681, 3501, "port_phasmatys_general_store", ghost);
        keeper("npc_kazemde", "Kazemde", "Nardah", 3418, 2905, "nardah_general_store", desert);
        keeper("npc_pollnivneach_shopkeeper", "Shop keeper", "Pollnivneach", 3358, 2984, "pollnivneach_general_store", desert);
        keeper("npc_lletya_shopkeeper", "Shop keeper", "Lletya", 2337, 3164, "lletya_general_store", elf);
        keeper("npc_gnome_shopkeeper", "Gnome shopkeeper", "Tree Gnome Stronghold", 2464, 3436, "gnome_general_store", gnome);
        keeper("npc_obli", "Obli", "Shilo Village", 2848, 2968, "shilo_general_store", karamja);
        keeper("npc_bandit_shopkeeper", "Bandit shopkeeper", "Bandit Camp", 3176, 2988, "bandit_camp_general_store", desert);
        keeper("npc_jiminua", "Jiminua", "Karamja", 2764, 3121, "jiminuas_jungle_store", karamja);
        keeper("npc_arhein", "Arhein", "Catherby", 2803, 3430, "arheins_store", sailor);

        // ------------------------------------------------------------- Lumbridge, Draynor, Varrock
        keeper("npc_bob", "Bob", "Lumbridge", 3230, 3203, "bobs_brilliant_axes", smith);
        keeper("npc_zaff", "Zaff", "Varrock", 3203, 3434, "zaffs_superior_staffs", wizard);
        keeper("npc_horvik", "Horvik", "Varrock", 3229, 3437, "horviks_armour_shop", smith);
        keeper("npc_lowe", "Lowe", "Varrock", 3233, 3421, "lowes_archery_emporium", look(SKIN, 0x3A6A2A, 0x5A4A2A, 0x3A2A1A));
        keeper("npc_varrock_sword_shopkeeper", "Shop keeper", "Varrock", 3204, 3399, "varrock_swordshop", smith);
        keeper("npc_thessalia", "Thessalia", "Varrock", 3206, 3417, "thessalias_fine_clothes", look(SKIN, 0xA03A6A, 0x6A2A4A, 0xC09040));
        keeper("npc_tea_seller", "Tea seller", "Varrock", 3271, 3411, "varrock_tea_stall", assistant);
        keeper("npc_peksa", "Peksa", "Barbarian Village", 3075, 3428, "peksas_helmet_shop", look(SKIN, 0x7A5A3A, 0x4A3A2A, 0xC0A060));
        keeper("npc_fortunato", "Fortunato", "Draynor Village", 3083, 3252, "fortunatos_fine_wine", look(SKIN, 0x6A1A3A, 0x3A2A2A, 0x1A1A1A));
        keeper("npc_olivia", "Olivia", "Draynor Village", 3075, 3254, "draynor_seed_market", look(SKIN, 0x4A7A2A, 0x5A4A2A, 0xC09040));

        // ------------------------------------------------------------- inns
        keeper("npc_blue_moon_bartender", "Bartender", "Varrock", 3226, 3399, "blue_moon_inn", bartender);
        keeper("npc_jolly_boar_bartender", "Bartender", "Varrock", 3279, 3488, "jolly_boar_inn", bartender);
        keeper("npc_rusty_anchor_bartender", "Bartender", "Port Sarim", 3045, 3257, "rusty_anchor_inn", bartender);
        keeper("npc_dragon_inn_bartender", "Bartender", "Yanille", 2555, 3080, "dragon_inn", bartender);
        keeper("npc_foresters_arms_bartender", "Bartender", "Seers' Village", 2691, 3493, "foresters_arms", bartender);
        keeper("npc_flying_horse_bartender", "Bartender", "East Ardougne", 2574, 3323, "flying_horse_inn", bartender);

        // ------------------------------------------------------------- Port Sarim, Rimmington
        keeper("npc_gerrant", "Gerrant", "Port Sarim", 3014, 3224, "gerrants_fishy_business", sailor);
        keeper("npc_betty", "Betty", "Port Sarim", 3013, 3258, "bettys_magic_emporium", look(SKIN, 0x8A3A9A, 0x5A2A6A, 0xC09040));
        keeper("npc_wydin", "Wydin", "Port Sarim", 3014, 3204, "wydins_food_store", look(SKIN, 0xE0E0E0, 0x4A3A2A, 0x6A4A2A));
        keeper("npc_brian", "Brian", "Port Sarim", 3027, 3249, "brians_battleaxe_bazaar", smith);
        keeper("npc_grum", "Grum", "Port Sarim", 3013, 3247, "grums_gold_exchange", look(SKIN, 0xC0A040, 0x4A3A2A, 0x2A2A2A));
        keeper("npc_brian_archery", "Brian", "Rimmington", 2957, 3204, "brians_archery_supplies", look(SKIN, 0x3A6A2A, 0x5A4A2A, 0x6A4A2A));

        // ------------------------------------------------------------- Falador, Taverley, Dwarven Mine
        keeper("npc_wayne", "Wayne", "Falador", 2972, 3312, "waynes_chains", smith);
        keeper("npc_cassie", "Cassie", "Falador", 2976, 3383, "cassies_shield_shop", look(SKIN, 0x8A6A4A, 0x5A4A3A, 0xC09040));
        keeper("npc_herquin", "Herquin", "Falador", 2945, 3335, "herquins_gems", assistant);
        keeper("npc_flynn", "Flynn", "Falador", 2950, 3387, "flynns_mace_market", smith);
        keeper("npc_gaius", "Gaius", "Taverley", 2884, 3450, "gaius_two_handed_shop", armoured);
        keeper("npc_jatix", "Jatix", "Taverley", 2899, 3428, "jatix_herblore_shop", look(SKIN, 0x3A6A3A, 0x2A4A2A, 0xE0E0E0));
        // Nurmof and Drogo trade in the Dwarven Mine; here they stand at the Falador mine entrance.
        keeper("npc_nurmof", "Nurmof", "Falador", 3058, 3377, "nurmofs_pickaxe_shop", dwarf);
        keeper("npc_drogo", "Drogo dwarf", "Falador", 3060, 3375, "drogos_mining_emporium", dwarf);

        // ------------------------------------------------------------- Al Kharid, desert
        keeper("npc_zeke", "Zeke", "Al Kharid", 3287, 3189, "zekes_superior_scimitars", desert);
        keeper("npc_louie_legs", "Louie Legs", "Al Kharid", 3316, 3175, "louies_armoured_legs_bazaar", desert);
        keeper("npc_ranael", "Ranael", "Al Kharid", 3315, 3163, "ranaels_super_skirt_store", look(TAN, 0xA03A6A, 0x6A2A4A, 0x1A1A1A));
        keeper("npc_dommik", "Dommik", "Al Kharid", 3319, 3193, "dommiks_crafting_store", desert);
        keeper("npc_gem_trader", "Gem trader", "Al Kharid", 3287, 3210, "al_kharid_gem_trader", desert);
        keeper("npc_karim", "Karim", "Al Kharid", 3273, 3180, "karims_kebabs", desert);
        keeper("npc_shantay", "Shantay", "Shantay Pass", 3304, 3123, "shantay_pass_shop", desert);
        keeper("npc_rokuh", "Rokuh", "Nardah", 3428, 2913, "rokuhs_choc_ice_emporium", desert);
        keeper("npc_seddu", "Seddu", "Nardah", 3436, 2911, "seddus_adventurers_store", desert);

        // ------------------------------------------------------------- Kandarin
        keeper("npc_harry", "Harry", "Catherby", 2834, 3443, "harrys_fishing_shop", sailor);
        keeper("npc_hickton", "Hickton", "Catherby", 2823, 3443, "hicktons_archery_emporium", look(SKIN, 0x3A6A2A, 0x5A4A2A, 0x3A2A1A));
        keeper("npc_vanessa", "Vanessa", "Catherby", 2819, 3463, "vanessas_farming_shop", look(SKIN, 0x4A7A2A, 0x6A4A2A, 0xC09040));
        keeper("npc_aemad", "Aemad", "East Ardougne", 2614, 3293, "aemads_adventuring_supplies", shopkeeper);
        keeper("npc_zenesha", "Zenesha", "East Ardougne", 2651, 3294, "zeneshas_plate_mail", armoured);
        keeper("npc_ardougne_baker", "Baker", "East Ardougne", 2667, 3311, "ardougne_baker", look(SKIN, 0xE0E0E0, 0x6A4A2A, 0xE0E0E0));
        keeper("npc_ardougne_silk_merchant", "Silk merchant", "East Ardougne", 2656, 3300, "ardougne_silk_trader", look(SKIN, 0xC03A3A, 0x5A2A4A, 0x1A1A1A));
        keeper("npc_ardougne_fur_trader", "Fur trader", "East Ardougne", 2663, 3295, "ardougne_fur_trader", look(SKIN, 0x7A6A5A, 0x4A3A2A, 0x6A5A4A));
        keeper("npc_ardougne_silver_merchant", "Silver merchant", "East Ardougne", 2658, 3316, "ardougne_silver_merchant", look(SKIN, 0xB0B0C0, 0x4A4A5A, 0x2A2A2A));
        keeper("npc_ardougne_spice_seller", "Spice seller", "East Ardougne", 2658, 3297, "ardougne_spice_seller", look(TAN, 0xC07A2A, 0x5A3A2A, 0xC03A3A));
        keeper("npc_ardougne_gem_merchant", "Gem merchant", "East Ardougne", 2667, 3303, "ardougne_gem_merchant", look(SKIN, 0x3A3A9A, 0x2A2A5A, 0x1A1A1A));
        keeper("npc_frenita", "Frenita", "Yanille", 2568, 3100, "frenitas_cookery_shop", look(SKIN, 0xE0E0E0, 0x6A4A2A, 0xC09040));
        keeper("npc_aleck", "Aleck", "Yanille", 2566, 3083, "alecks_hunter_emporium", look(SKIN, 0x5A6A3A, 0x4A3A2A, 0x6A5A3A));
        keeper("npc_wizard_sinina", "Wizard Sinina", "Yanille", 2595, 3089, "magic_guild_store_mystic", wizard);
        keeper("npc_davon", "Davon", "Brimhaven", 2779, 3152, "davons_amulet_store", look(SKIN, 0x3A3A3A, 0x2A2A2A, 0x1A1A1A));
        keeper("npc_gulluck", "Gulluck", "Tree Gnome Stronghold", 2477, 3505, "gulluck_and_sons", gnome);
        keeper("npc_heckel_funch", "Heckel Funch", "Tree Gnome Stronghold", 2490, 3489, "grand_tree_groceries", gnome);

        // ------------------------------------------------------------- Karamja
        keeper("npc_fernahei", "Fernahei", "Shilo Village", 2858, 2966, "fernaheis_fishing_hut", karamja);

        // ------------------------------------------------------------- Fremennik, Morytania, Lletya, Wilderness
        keeper("npc_fremennik_fishmonger", "Fish monger", "Rellekka", 2645, 3676, "fremennik_fish_monger", fremennik);
        keeper("npc_fremennik_fur_trader", "Fur trader", "Rellekka", 2642, 3678, "fremennik_fur_trader", fremennik);
        keeper("npc_yrsa", "Yrsa", "Rellekka", 2625, 3675, "yrsas_accoutrements", fremennik);
        keeper("npc_rufus", "Rufus", "Canifis", 3507, 3492, "rufus_meat_emporium", werewolf);
        keeper("npc_barker", "Barker", "Canifis", 3498, 3505, "barkers_haberdashery", werewolf);
        keeper("npc_lletya_food_seller", "Shop keeper", "Lletya", 2341, 3177, "lletya_food_store", elf);
        keeper("npc_lletya_fletcher", "Shop keeper", "Lletya", 2324, 3163, "lletya_archery_shop", elf);
        // Lundail and the Chamber Guardian are in the Mage Arena bank in OSRS; here they wait outside the arena.
        keeper("npc_lundail", "Lundail", "Mage Arena", 3103, 3937, "lundails_rune_shop", wizard);
        keeper("npc_chamber_guardian", "Chamber Guardian", "Mage Arena", 3107, 3937, "mage_arena_staffs", look(SKIN, 0x5A3A8A, 0x3A2A5A, 0x5A3A8A));

        // ------------------------------------------------------------- bankers
        bankers("lumbridge", "Lumbridge", 3208, 3222, 3208, 3218); // OSRS: top floor of Lumbridge Castle
        bankers("varrock_west", "Varrock", 3187, 3438, 3187, 3442);
        bankers("varrock_east", "Varrock", 3252, 3418, 3255, 3418);
        bankers("grand_exchange", "Grand Exchange", 3161, 3489, 3168, 3492);
        bankers("edgeville", "Edgeville", 3096, 3489, 3096, 3492);
        bankers("draynor", "Draynor Village", 3090, 3245, 3090, 3242);
        bankers("al_kharid", "Al Kharid", 3267, 3167, 3267, 3164);
        bankers("falador_west", "Falador", 2944, 3366, 2947, 3366);
        bankers("falador_east", "Falador", 3013, 3353, 3015, 3353);
        bankers("seers", "Seers' Village", 2724, 3494, 2727, 3494);
        bankers("catherby", "Catherby", 2809, 3442, 2811, 3442);
        bankers("ardougne_north", "East Ardougne", 2615, 3336, 2618, 3336);
        bankers("ardougne_south", "East Ardougne", 2652, 3280, 2655, 3280);
        bankers("yanille", "Yanille", 2613, 3097, 2615, 3097);
        bankers("gnome_stronghold_south", "Tree Gnome Stronghold", 2445, 3427, 2448, 3427);
        bankers("gnome_stronghold_tree", "Tree Gnome Stronghold", 2449, 3484, 2451, 3484); // OSRS: first floor of the Grand Tree
        bankers("canifis", "Canifis", 3512, 3478, 3512, 3481);
        bankers("port_phasmatys", "Port Phasmatys", 3689, 3466, 3691, 3466);
        bankers("shilo", "Shilo Village", 2852, 2953, 2855, 2953);
        bankers("nardah", "Nardah", 3428, 2889, 3430, 2889);
        bankers("lletya", "Lletya", 2352, 3166, 2354, 3166);
        bankers("castle_wars", "Castle Wars", 2443, 3085, 2445, 3085);
        bankers("fishing_guild", "Fishing Guild", 2586, 3418, 2588, 3418);
        bankers("ferox_enclave", "Ferox Enclave", 3130, 3631, 3132, 3631);
        bankers("emirs_arena", "Al Kharid", 3381, 3268, 3383, 3268);
        bankers("warriors_guild", "Warriors' Guild", 2843, 3543, 2845, 3543);
        bankers("crafting_guild", "Crafting Guild", 2936, 3281, 2938, 3281);
        bankers("burgh_de_rott", "Burgh de Rott", 3496, 3211, 3498, 3211);
        bankers("neitiznot", "Neitiznot", 2336, 3807, 2338, 3807);
        bankers("jatizso", "Jatizso", 2416, 3801, 2418, 3801);
        bankers("piscatoris", "Piscatoris", 2330, 3689, 2332, 3689);
        bankers("lunar_isle", "Lunar Isle", 2099, 3920, 2101, 3920);
        bankers("hosidius", "Hosidius", 1749, 3599, 1751, 3599);
        bankers("arceuus", "Arceuus", 1624, 3745, 1626, 3745);
        bankers("lovakengj", "Lovakengj", 1526, 3739, 1528, 3739);
        bankers("shayzien", "Shayzien", 1504, 3615, 1506, 3615);
        bankers("piscarilius", "Port Piscarilius", 1803, 3790, 1805, 3790);

        // ------------------------------------------------------------- tanners
        tanner("npc_ellis", "Ellis", "Al Kharid", 3274, 3192, desert, "Greetings friend. I am a manufacturer of leather.");
        tanner("npc_sbott", "Sbott", "Canifis", 3490, 3500, werewolf, "Hello stranger. Would you like me to tan any hides for you?");

        // ------------------------------------------------------------- flavour characters
        // Emblem Trader (Edgeville): his "Skull" option is wired up by the npcs module hooks (combat SkullService).
        ambient("npc_emblem_trader", "Emblem Trader", "Edgeville", 3096, 3503, look(SKIN, 0x2A2A2A, 0x1A1A1A, 0x3A2A4A),
                "Fancy a PK skull? Talk to me again and you'll have one.");
        ambient("npc_doomsayer", "Doomsayer", "Lumbridge", 3232, 3235, look(SKIN, 0x3A2A2A, 0x2A2A2A, 0x6A6A6A),
                "Dooooom!", "Where? All around us! I can feel it in the air, hear it on the wind, smell it... also in the air!",
                "I can warn you about dangers in Gielinor. Doom approaches, but forewarned is forearmed.");
        ambient("npc_gee", "Gee", "Lumbridge", 3226, 3226, look(SKIN, 0x8A6A3A, 0x4A3A2A, 0x5A3A1A),
                "Hello there, can I help you?", "I'm afraid I'm not much of a talker.", "Lovely day in Lumbridge, isn't it?");
        ambient("npc_donie", "Donie", "Lumbridge", 3218, 3212, look(SKIN, 0x3A5A8A, 0x3A3A2A, 0x6A4A2A),
                "Hello there, can I help you?", "Do you know where I can find the Duke? I've an urgent message for him.",
                "Have you heard? They say there's a dragon on Crandor!");
        ambient("npc_lumbridge_castle_guard_1", "Guard", "Lumbridge", 3216, 3219, armoured,
                "Move along, citizen.", "Duke Horacio is in his chamber upstairs, if you need him.");
        ambient("npc_lumbridge_castle_guard_2", "Guard", "Lumbridge", 3216, 3216, armoured,
                "Halt! Oh, it's you. Carry on.", "The Duke keeps a fine castle. Mind you behave yourself.");
        ambient("npc_benny", "Benny", "Varrock", 3219, 3432, look(SKIN, 0x8A7A5A, 0x4A3A2A, 0x3A2A1A),
                "Read all about it! Read all about it! Varrock Herald, now only 50 gold!",
                "Extra! Extra! Mysterious happenings at the Champions' Guild!");
        ambient("npc_varrock_town_crier", "Town crier", "Varrock", 3212, 3430, look(SKIN, 0x8A2A2A, 0x2A2A6A, 0x1A1A1A),
                "Oh yez! Oh yez! Visit the Grand Exchange, north-west of Varrock, to trade with other players!",
                "Oh yez! Oh yez! Never give your password to anyone, not even a town crier!",
                "Oh yez! Oh yez! Bankers can store your items safely at every bank in Gielinor!");
        ambient("npc_falador_town_crier", "Town crier", "Falador", 2964, 3386, look(SKIN, 0x8A2A2A, 0x2A2A6A, 0x1A1A1A),
                "Oh yez! Oh yez! The White Knights welcome all honest adventurers to Falador!",
                "Oh yez! Oh yez! Falador Park is a fine place to rest your feet.",
                "Oh yez! Oh yez! Visit the Mining Guild with 60 Mining for the finest ores!");
        ambient("npc_ardougne_town_crier", "Town crier", "East Ardougne", 2662, 3307, look(SKIN, 0x8A2A2A, 0x2A2A6A, 0x1A1A1A),
                "Oh yez! Oh yez! Beware of thieves in the Ardougne market!",
                "Oh yez! Oh yez! The Paladins of Ardougne protect our fair city!",
                "Oh yez! Oh yez! Captain Barnaby sails to Brimhaven for only 30 coins!");
        ambient("npc_seers_town_crier", "Town crier", "Seers' Village", 2730, 3486, look(SKIN, 0x8A2A2A, 0x2A2A6A, 0x1A1A1A),
                "Oh yez! Oh yez! King Arthur's knights welcome you to Camelot!",
                "Oh yez! Oh yez! The Flax fields south of Seers' Village are free to pick!");
        ambient("npc_seer", "Seer", "Seers' Village", 2708, 3484, wizard,
                "Many greetings. I can see you have travelled a long way.",
                "The spirits tell me... that you should be careful in the Wilderness.");
        ambient("npc_tramp", "Tramp", "Varrock", 3207, 3392, look(SKIN, 0x5A4A3A, 0x3A3A2A, 0x3A2A1A),
                "Spare some change, guv?", "Times are hard, mate. Times are hard.");
        ambient("npc_barbarian_village_drunk", "Drunken man", "Barbarian Village", 3080, 3440, look(SKIN, 0x8A5A3A, 0x4A3A2A, 0xB08060),
                "*hic* Who are you?", "Ish a lovely day, *hic*, a lovely day for a drink.");
        // Wizard Distentor, on the Wizards' Guild ground floor (tile approx): teleports to the Rune essence mine after
        // Rune Mysteries (the dungeons module's EssenceMine adds the Teleport option).
        ambient("npc_wizard_distentor", "Wizard Distentor", "Yanille", 2591, 3088, look(SKIN, 0x3A3A9A, 0x2A2A7A, 0x3A3A9A),
                "Welcome to the Magicians' Guild!", "I can teleport you to the Rune essence mine, if you know of it.");

        // ------------------------------------------------------------- travel
        travel("npc_captain_tobias", "Captain Tobias", "Port Sarim", 3028, 3216, "boat_port_sarim_karamja", sailor,
                "Do you want to go on a trip to Karamja? The trip will cost you 30 coins.");
        travel("npc_seaman_lorris", "Seaman Lorris", "Port Sarim", 3027, 3220, "boat_port_sarim_karamja", sailor,
                "Do you want to go on a trip to Karamja? The trip will cost you 30 coins.");
        travel("npc_seaman_thresnor", "Seaman Thresnor", "Port Sarim", 3031, 3214, "boat_port_sarim_karamja", sailor,
                "Do you want to go on a trip to Karamja? The trip will cost you 30 coins.");
        travel("npc_musa_customs_officer", "Customs officer", "Musa Point", 2954, 3147, "boat_karamja_port_sarim", sailor,
                "Can I help you? Would you like to journey back to Port Sarim? It'll cost 30 coins.");
        travel("npc_captain_barnaby", "Captain Barnaby", "East Ardougne", 2683, 3274, "boat_ardougne_brimhaven", sailor,
                "Do you want to go on a trip to Brimhaven? The trip will cost you 30 coins.");
        travel("npc_brimhaven_customs_officer", "Customs officer", "Brimhaven", 2775, 3228, "boat_brimhaven_ardougne", sailor,
                "Would you like to journey to Ardougne? It'll cost 30 coins.");
        travel("npc_port_sarim_squire", "Squire", "Port Sarim", 3041, 3204, "boat_void_outpost", armoured,
                "Hi, how can I help you? I can take you to the Void Knights' Outpost.");
        travel("npc_outpost_squire", "Squire", "Void Knights' Outpost", 2659, 2678, "boat_void_port_sarim", armoured,
                "Would you like to go back to Port Sarim?");
        travel("npc_rellekka_sailor", "Sailor", "Rellekka", 2629, 3693, "boat_rellekka_miscellania", fremennik,
                "Ahoy! Fancy a trip to Miscellania?");
        travel("npc_miscellania_sailor", "Sailor", "Miscellania", 2581, 3847, "boat_miscellania_rellekka", fremennik,
                "Ahoy! Back to Rellekka, is it?");
        travel("npc_jarvald", "Jarvald", "Rellekka", 2620, 3686, "boat_rellekka_waterbirth", fremennik,
                "Ho there, outerlander! I am hunting the dagannoth on Waterbirth Island. I can take you there for 1000 coins.");
        travel("npc_jarvald_waterbirth", "Jarvald", "Waterbirth Island", 2544, 3761, "boat_waterbirth_rellekka", fremennik,
                "Ready to head back to Rellekka?");
        // More fixed-fare boats (OSRS): Barnaby's Rimmington stop, the Pandemonium, Cabin Boy Colin, the Gunnars' boats
        // to the Fremennik Isles and Antonia's ferry between Sunset Coast and Aldarin.
        travel("npc_captain_barnaby_rimmington", "Captain Barnaby", "Rimmington", 2915, 3226, "boat_rimmington", sailor,
                "Do you want to go on a trip to Ardougne or Brimhaven? The trip will cost you 30 coins.");
        travel("npc_seaman_morris", "Seaman Morris", "The Pandemonium", 3064, 3004, "boat_pandemonium", sailor,
                "Back to the mainland? 30 coins.");
        travel("npc_cabin_boy_colin_rimmington", "Cabin Boy Colin", "Rimmington", 2909, 3227, "boat_rimmington_corsair_cove", sailor,
                "The captain's ship sails for Corsair Cove whenever you're ready.");
        travel("npc_cabin_boy_colin_corsair_cove", "Cabin Boy Colin", "Corsair Cove", 2578, 2841, "boat_corsair_cove_rimmington", sailor,
                "Back to Rimmington?");
        travel("npc_mord_gunnars_rellekka", "Mord Gunnars", "Rellekka", 2644, 3708, "boat_rellekka_jatizso", fremennik,
                "Off to Jatizso? Hop in.");
        travel("npc_mord_gunnars_jatizso", "Mord Gunnars", "Jatizso", 2421, 3782, "boat_jatizso_rellekka", fremennik,
                "Back to Rellekka?");
        travel("npc_maria_gunnars_rellekka", "Maria Gunnars", "Rellekka", 2645, 3711, "boat_rellekka_neitiznot", fremennik,
                "Want to go to Neitiznot?");
        travel("npc_maria_gunnars_neitiznot", "Maria Gunnars", "Neitiznot", 2311, 3782, "boat_neitiznot_rellekka", fremennik,
                "Back to Rellekka?");
        travel("npc_antonia_sunset_coast", "Antonia", "Sunset Coast", 1494, 2986, "boat_sunset_coast_aldarin", sailor,
                "A boat to Aldarin? 20 coins.");
        travel("npc_antonia_aldarin", "Antonia", "Aldarin", 1443, 2978, "boat_aldarin_sunset_coast", sailor,
                "A boat to the Sunset Coast? 20 coins.");
        // Charter crews, glider pilots, canoe stations, spirit trees, balloon assistants and cart conductors.
        Map<String, int[]> agentLooks = Map.of("sailor", sailor, "gnome", look(GNOME, 0x8A5A2A, 0x5A3A2A, 0x3A3A3A),
                "canoe", look(SKIN, 0x6A4A2A, 0x4A3A2A, 0x3A6A2A), "tree", look(0x6A5A3A, 0x4A8A3A, 0x5A4A2A, 0x3A7A2A),
                "balloon", look(SKIN, 0x8A2A2A, 0x3A3A6A, 0xC0A060), "dwarf", dwarf);
        for (Travel.Agent a : Travel.agents()) {
            travel(a.id(), a.name(), a.area(), a.x(), a.y(), a.route(), agentLooks.getOrDefault(a.look(), sailor), a.line());
        }
        int[] rug = look(TAN, 0x7A3A8A, 0x5A2A6A, 0xE0D8C0);
        travel("npc_rug_merchant_shantay", "Rug Merchant", "Shantay Pass", 3311, 3109, "carpet_shantay", rug,
                "Greetings, desert traveller. Do you require the services of my double-decker rugs? 200 coins a trip.");
        travel("npc_rug_merchant_pollnivneach_north", "Rug Merchant", "Pollnivneach", 3350, 3001, "carpet_pollnivneach_north", rug,
                "Greetings, desert traveller. Would you like a ride on my magic carpet?");
        travel("npc_rug_merchant_pollnivneach_south", "Rug Merchant", "Pollnivneach", 3348, 2942, "carpet_pollnivneach_south", rug,
                "Greetings, desert traveller. Would you like a ride on my magic carpet?");
        travel("npc_rug_merchant_nardah", "Rug Merchant", "Nardah", 3401, 2918, "carpet_nardah", rug,
                "Greetings, desert traveller. Would you like a ride on my magic carpet?");
        travel("npc_rug_merchant_sophanem", "Rug Merchant", "Sophanem", 3287, 2813, "carpet_sophanem", rug,
                "Greetings, desert traveller. Would you like a ride on my magic carpet?");
        travel("npc_rug_merchant_menaphos", "Rug Merchant", "Menaphos", 3243, 2813, "carpet_menaphos", rug,
                "Greetings, desert traveller. Would you like a ride on my magic carpet?");
        travel("npc_rug_merchant_bedabin", "Rug Merchant", "Bedabin Camp", 3182, 3043, "carpet_bedabin", rug,
                "Greetings, desert traveller. Would you like a ride on my magic carpet?");
        travel("npc_rug_merchant_uzer", "Rug Merchant", "Uzer", 3467, 3110, "carpet_uzer", rug,
                "Greetings, desert traveller. Would you like a ride on my magic carpet?");

        // ------------------------------------------------------------- characters from elsewhere who also trade
        EXTRA_TRADERS.put("AUBURY", "auburys_rune_shop");
        EXTRA_TRADERS.put("candle_maker", "candle_shop");
        EXTRA_TRADERS.put("zambo", "karamja_spirits_bar");
        EXTRA_TRADERS.put("solihib", "solihibs_food_stall");
        EXTRA_TRADERS.put("sir_vyvin", "white_knight_armoury");
        // The Warrens are under Port Piscarilius (not carved): the shopkeeper stands by the entrance. Keldagrim's stonemason
        // stands in RuneCraft's surface Keldagrim. // approx
        keeper("npc_warrens_shopkeeper", "Shop keeper", "Port Piscarilius", 1813, 3744, "warrens_general_store",
                look(SKIN, 0x5A6A3A, 0x4A3A2A, 0x3A2A1A));
        keeper("npc_keldagrim_stonemason", "Stonemason", "Keldagrim", 2740, 3712, "keldagrim_stonemason",
                look(SKIN, 0x6A4A2A, 0x4A3A2A, 0x8A5A2A));
        for (String[] m : Shops.CAPE_MERCHANTS) {
            keeper("npc_cape_merchant_" + m[0], m[1], "Wilderness", Integer.parseInt(m[3]), Integer.parseInt(m[4]), "cape_merchant_" + m[0],
                    look(SKIN, com.runecraft.osrs.content.CommonItems.TEAM_CAPE_RGB[Integer.parseInt(m[2]) % 5], 0x4A3A2A, 0x3A2A1A));
        }
    }
}
