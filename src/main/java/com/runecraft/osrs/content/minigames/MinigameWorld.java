package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.CollectionLogData;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Places (real OSRS entrance locations), scripted blocks (entrances, reward stations and the
 * interactive objects inside the arenas), NPCs and collection log pages of the minigames.
 */
public final class MinigameWorld {
    private MinigameWorld() {
    }

    /** Extra entrances beyond each minigame's own "<id>_entrance": block id -> minigame. */
    public static final Map<String, Minigame> EXTRA_ENTRANCES = new LinkedHashMap<>();
    /** Reward-shop blocks: block id -> shop id. */
    public static final Map<String, String> SHOP_BLOCKS = new LinkedHashMap<>();

    static {
        EXTRA_ENTRANCES.put("pest_control_intermediate_entrance", Minigame.PEST_CONTROL);
        EXTRA_ENTRANCES.put("pest_control_veteran_entrance", Minigame.PEST_CONTROL);
        for (String role : MinigameRules.BarbarianAssault.ROLES) {
            EXTRA_ENTRANCES.put("barbarian_assault_" + role + "_entrance", Minigame.BARBARIAN_ASSAULT);
        }
        for (String room : MinigameRules.MageArena.ROOMS) {
            EXTRA_ENTRANCES.put("mta_" + room + "_entrance", Minigame.MAGE_TRAINING_ARENA);
        }
        EXTRA_ENTRANCES.put("warriors_guild_cyclops_entrance", Minigame.WARRIORS_GUILD);
        EXTRA_ENTRANCES.put("nightmare_zone_hard_entrance", Minigame.NIGHTMARE_ZONE);
        SHOP_BLOCKS.put("tzhaar_store", "tzhaar");
        SHOP_BLOCKS.put("pest_control_rewards", "pest_control");
        SHOP_BLOCKS.put("guardians_of_the_rift_rewards", "guardians_of_the_rift");
        SHOP_BLOCKS.put("barbarian_assault_rewards", "barbarian_assault");
        SHOP_BLOCKS.put("castle_wars_rewards", "castle_wars");
        SHOP_BLOCKS.put("mage_training_arena_rewards", "mage_training_arena");
        SHOP_BLOCKS.put("hallowed_sepulchre_rewards", "hallowed_sepulchre");
        SHOP_BLOCKS.put("brimhaven_agility_arena_rewards", "brimhaven_agility_arena");
        SHOP_BLOCKS.put("trouble_brewing_rewards", "trouble_brewing");
        SHOP_BLOCKS.put("tithe_farm_rewards", "tithe_farm");
        SHOP_BLOCKS.put("giants_foundry_rewards", "giants_foundry");
        SHOP_BLOCKS.put("mahogany_homes_rewards", "mahogany_homes");
        SHOP_BLOCKS.put("gnome_restaurant_rewards", "gnome_restaurant");
        SHOP_BLOCKS.put("nightmare_zone_rewards", "nightmare_zone");
        SHOP_BLOCKS.put("soul_wars_rewards", "soul_wars");
    }

    /** Interactive blocks placed inside the instanced arenas: id, name, colour, look. */
    public static final String[][] ARENA_BLOCKS = {
            // Wintertodt
            {"wintertodt_brazier", "Brazier", "C05020", "fire_brazier"},
            {"bruma_roots", "Bruma roots", "6A5A8A", "roots"},
            {"sprouting_roots", "Sprouting roots", "9AC0E0", "roots"},
            {"wintertodt_crate", "Supply crate", "8A6A3A", "crate"},
            {"wintertodt_doors", "Doors of Dinh", "6A6A7A", "portal_door"},
            // Tempoross
            {"tempoross_fishing_spot", "Harpoonfish spot", "4080C0", "water_spot"},
            {"tempoross_cannon", "Ammunition crate", "6A5A3A", "crate"},
            {"tempoross_shrine", "Shrine", "C06030", "fire_shrine"},
            {"tempoross_totem", "Totem pole", "8A6A3A", "totem"},
            {"tempoross_spirit_pool", "Spirit pool", "60E0E0", "portal_glow"},
            {"tempoross_reward_pool", "Reward pool", "4080C0", "portal_glow"},
            // Guardians of the Rift
            {"guardian_remains", "Huge guardian remains", "6A5AC0", "rock"},
            {"gotr_workbench", "Workbench", "8A6A4A", "machine"},
            {"elemental_rift", "Elemental guardian", "E07040", "portal_glow"},
            {"catalytic_rift", "Catalytic guardian", "8040E0", "portal_glow"},
            {"gotr_great_guardian", "The Great Guardian", "5A4AA0", "obelisk"},
            {"gotr_rift_search", "Rewards guardian", "9A8AE0", "chest"},
            // Barbarian Assault
            {"ba_egg_launcher", "Egg launcher", "8A7A5A", "machine"},
            {"ba_healer_spring", "Healing spring", "60A0E0", "portal_glow"},
            // Castle Wars
            {"cw_saradomin_stand", "Saradomin flag stand", "3060E0", "statue"},
            {"cw_zamorak_stand", "Zamorak flag stand", "C02020", "statue"},
            {"cw_table", "Supply table", "8A6A3A", "crate"},
            // Warriors' Guild
            {"wg_armour_animator", "Magical animator", "9A9AA0", "statue"},
            {"wg_cyclops_door", "Cyclops room door", "6A4A2A", "portal_door"},
            // Mage Training Arena
            {"mta_statue", "Guardian statue", "8A8AA0", "statue"},
            {"mta_cupboard", "Cupboard", "8A6A3A", "chest"},
            {"mta_coin_collector", "Coin collector", "E0C030", "machine"},
            {"mta_shape_pile", "Shape pile", "40C0C0", "crate"},
            {"mta_enchant_hole", "Hole", "202030", "portal_glow"},
            {"mta_bone_pile", "Bone pile", "E8E0D0", "crate"},
            {"mta_food_chute", "Food chute", "A0E060", "machine"},
            // Pyramid Plunder
            {"pp_urn", "Urn", "C0A080", "pot"},
            {"pp_chest", "Grand gold chest", "E0B030", "chest"},
            {"pp_sarcophagus", "Sarcophagus", "C0A060", "statue"},
            {"pp_door", "Tomb door", "8A7A5A", "portal_door"},
            // Hallowed Sepulchre
            {"sep_coffin", "Coffin", "6A6A7A", "chest"},
            {"sep_grand_coffin", "Grand Hallowed Coffin", "C0D0E0", "chest"},
            {"sep_stairs", "Stairs", "8A8AA0", "portal_door"},
            {"sep_statue", "Crossbow statue", "9A9AB0", "statue"},
            // Brimhaven Agility Arena
            {"agility_ticket_dispenser", "Ticket dispenser", "E0A040", "machine"},
            // Gnome Restaurant
            {"gnome_oven", "Gnome oven", "8A5A3A", "range"},
            {"gnome_customer", "Customer's door", "6A9A4A", "portal_door"},
            // Trouble Brewing
            {"tb_bitternut_tree", "Bitternut tree", "6A4A2A", "tree"},
            {"tb_scrapey_tree", "Scrapey tree", "8A6A4A", "tree"},
            {"tb_sweetgrub_mound", "Sweetgrub mound", "C0A060", "roots"},
            {"tb_brewing_vat", "Brewing vat", "8A6A3A", "machine"},
            // Shades of Mort'ton
            {"funeral_pyre", "Funeral pyre", "6A4A2A", "fire_pyre"},
            {"shade_chest", "Bronze chest", "8A5A2A", "chest"},
            {"shade_chest_steel", "Steel chest", "9A9AA0", "chest"},
            {"shade_chest_black", "Black chest", "2A2A2A", "chest"},
            {"shade_chest_silver", "Silver chest", "D0D0E0", "chest"},
            {"shade_chest_gold", "Gold chest", "E0B030", "chest"},
            {"shade_door_bronze", "Wooden doors (bronze lock)", "6A4A2A", "portal_door"},
            {"shade_door_steel", "Wooden doors (steel lock)", "6A4A2A", "portal_door"},
            {"shade_door_black", "Wooden doors (black lock)", "6A4A2A", "portal_door"},
            {"shade_door_silver", "Wooden doors (silver lock)", "6A4A2A", "portal_door"},
            {"shade_door_gold", "Wooden doors (gold lock)", "6A4A2A", "portal_door"},
            // Tithe Farm
            {"tithe_patch", "Tithe patch", "5A3A1A", "soil"},
            {"tithe_water_barrel", "Water barrel", "4080C0", "pot"},
            {"tithe_sack", "Sack", "C0A060", "crate"},
            {"tithe_seed_table", "Seed table", "8A6A3A", "crate"},
            // Giants' Foundry
            {"foundry_crucible", "Crucible", "E06020", "fire_crucible"},
            {"foundry_mould", "Mould jig", "8A8A8A", "machine"},
            {"foundry_trip_hammer", "Trip hammer", "6A6A70", "machine"},
            {"foundry_grindstone", "Grindstone", "9A9AA0", "machine"},
            {"foundry_polishing_wheel", "Polishing wheel", "C0C0C8", "machine"},
            {"foundry_lava_pool", "Lava pool", "FF6010", "fire_lava"},
            {"foundry_waterfall", "Waterfall", "4080E0", "water_spot"},
            {"foundry_handin", "Kovac's bench", "8A6A4A", "chest"},
            // Mahogany Homes
            {"mh_hotspot", "Furniture hotspot", "9A7A4A", "crate"},
            // Fishing Trawler
            {"trawler_leak", "Leak", "4080C0", "water_spot"},
            {"trawler_bucket", "Bailing bucket", "8A6A3A", "pot"},
            // Rogues' Den
            {"rd_obstacle", "Obstacle", "6A6A70", "machine"},
            {"rd_safe", "Wall safe", "5A5A60", "chest"},
            // Volcanic Mine
            {"vm_boulder", "Boulder", "5A4A40", "rock"},
            {"vm_vent", "Gas chamber", "C05020", "fire_lava"},
            // Soul Wars
            {"sw_soul_obelisk", "Soul Obelisk", "8A3AC0", "obelisk"},
            {"sw_graveyard", "Graveyard", "6A6A6A", "statue"},
            // Tears of Guthix
            {"tog_weeping_wall", "Weeping wall", "4A6A8A", "water_spot"},
            // Sorceress's Garden (gate, sq'irk tree and herb patch per season; see SorceressGarden.Season)
            {"sg_fountain", "Fountain", "6AA0E0", "statue"},
            {"sg_gate_winter", "Winter garden gate", "9AC8E8", "portal_door"},
            {"sg_gate_spring", "Spring garden gate", "6AC050", "portal_door"},
            {"sg_gate_autumn", "Autumn garden gate", "C07020", "portal_door"},
            {"sg_gate_summer", "Summer garden gate", "E04040", "portal_door"},
            {"sg_sqirk_tree_winter", "Sq'irk tree", "8AA0B0", "tree"},
            {"sg_sqirk_tree_spring", "Sq'irk tree", "4A9A3A", "tree"},
            {"sg_sqirk_tree_autumn", "Sq'irk tree", "A06020", "tree"},
            {"sg_sqirk_tree_summer", "Sq'irk tree", "3A8A2A", "tree"},
            {"sg_herbs_winter", "Herbs", "6A8A7A", "farming_patch"},
            {"sg_herbs_spring", "Herbs", "4A9A3A", "farming_patch"},
            {"sg_herbs_autumn", "Herbs", "8A7A2A", "farming_patch"},
            {"sg_herbs_summer", "Herbs", "3A8A2A", "farming_patch"},
            // Shared
            {"minigame_exit", "Exit portal", "C0C0FF", "portal_glow"}};

    /**
     * Mort'ton's town objects beside the catacombs entrance: a funeral pyre, the Flamtaer temple's wall (repairs) and
     * sacred fire (olive oil to sacred oil), and Razmire's general store (olive oil) and builders' merchant.
     */
    public static final List<String> MORTTON_BLOCKS = List.of("funeral_pyre", "flamtaer_temple", "flamtaer_altar",
            "razmire_general_store", "razmire_builders_merchant");

    /** Shelves in the sorceress's house: free beer glasses for the sq'irkjuice. */
    public static final String SORCERESS_SHELVES = "sg_shelves";

    public static List<BlockDef> blocks() {
        List<BlockDef> list = new ArrayList<>();
        for (Minigame m : Minigame.values()) {
            list.add(BlockDef.scripted(m.entranceBlock(), m.displayName() + " entrance", 0x8060C0, "portal_glow"));
        }
        for (String id : EXTRA_ENTRANCES.keySet()) {
            String name = id.replace("_entrance", "").replace('_', ' ');
            list.add(BlockDef.scripted(id, Character.toUpperCase(name.charAt(0)) + name.substring(1) + " entrance", 0x8060C0,
                    "portal_glow"));
        }
        for (Map.Entry<String, String> e : SHOP_BLOCKS.entrySet()) {
            list.add(BlockDef.scripted(e.getKey(), RewardShops.get(e.getValue()).title(), 0xC09040, "chest"));
        }
        list.add(BlockDef.scripted("pyramid_plunder_artefact_trader", "Simon Templeton's artefact crate", 0xC0A060, "chest"));
        list.add(BlockDef.scripted("rogues_den_jewel_trader", "Brian O'Richard's crate", 0x5A5A60, "chest"));
        list.add(BlockDef.scripted(SORCERESS_SHELVES, "Shelves", 0x8A6A3A, "crate"));
        list.add(BlockDef.scripted("flamtaer_temple", "Flamtaer temple wall", 0xB0A890, "rock"));
        list.add(BlockDef.scripted("flamtaer_altar", "Sacred fire", 0xE08030, "fire_brazier"));
        list.add(BlockDef.scripted("razmire_general_store", "Razmire's general store", 0x8A6A3A, "crate"));
        list.add(BlockDef.scripted("razmire_builders_merchant", "Razmire's builders' merchant", 0x9A7A4A, "crate"));
        for (String[] b : ARENA_BLOCKS) {
            list.add(BlockDef.scripted(b[0], b[1], Integer.parseInt(b[2], 16), b[3]));
        }
        return list;
    }

    /** The minigame blocks placed at each entrance place, keyed by place id. */
    public static Map<String, List<String>> placeBlocks() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (Minigame m : Minigame.values()) {
            map.computeIfAbsent(m.place(), k -> new ArrayList<>()).add(m.entranceBlock());
        }
        for (Map.Entry<String, Minigame> e : EXTRA_ENTRANCES.entrySet()) {
            map.get(e.getValue().place()).add(e.getKey());
        }
        for (Map.Entry<String, String> e : SHOP_BLOCKS.entrySet()) {
            String shop = e.getValue();
            Minigame m = shop.equals("tzhaar") ? Minigame.FIGHT_CAVES : Minigame.byId(shop);
            map.get(m.place()).add(e.getKey());
        }
        map.get(Minigame.PYRAMID_PLUNDER.place()).add("pyramid_plunder_artefact_trader");
        map.get(Minigame.ROGUES_DEN.place()).add("rogues_den_jewel_trader");
        map.get(Minigame.TEMPOROSS.place()).add("tempoross_reward_pool");
        map.get(Minigame.SORCERESS_GARDEN.place()).add(SORCERESS_SHELVES);
        map.get(Minigame.SHADES_OF_MORTTON.place()).addAll(MORTTON_BLOCKS);
        return map;
    }

    public static List<PlaceDef> places() {
        List<PlaceDef> list = new ArrayList<>();
        Map<String, List<String>> blocks = placeBlocks();
        for (Minigame m : Minigame.values()) {
            if (list.stream().anyMatch(p -> p.id().equals(m.place()))) {
                continue;
            }
            String name = switch (m) {
                case FIGHT_CAVES, INFERNO -> "Mor Ul Rek gate";
                case PEST_CONTROL -> "Void Knights' Outpost";
                case WINTERTODT -> "Wintertodt Camp";
                case TEMPOROSS -> "Ruins of Unkah";
                case GUARDIANS_OF_THE_RIFT -> "Temple of the Eye";
                case BARBARIAN_ASSAULT -> "Barbarian Outpost";
                case CASTLE_WARS -> "Castle Wars";
                case PYRAMID_PLUNDER -> "Jalsavrah (Sophanem)";
                case HALLOWED_SEPULCHRE -> "Darkmeyer";
                case GNOME_RESTAURANT -> "Grand Tree";
                case TROUBLE_BREWING -> "Mos Le'Harmless";
                case SHADES_OF_MORTTON -> "Mort'ton";
                case FISHING_TRAWLER -> "Port Khazard";
                case CHAMPIONS_CHALLENGE -> "Champions' Guild arena";
                case VOLCANIC_MINE -> "Volcanic Mine entrance";
                case NIGHTMARE_ZONE -> "Nightmare Zone";
                case SOUL_WARS -> "Isle of Souls";
                case TEARS_OF_GUTHIX -> "Chasm of Tears entrance";
                case SORCERESS_GARDEN -> "Sorceress's house";
                default -> m.displayName();
            };
            list.add(PlaceDef.of(m.place(), name, m.entranceX(), m.entranceY(), m.style(), blocks.get(m.place())));
        }
        return list;
    }

    public static List<NpcDef> npcs() {
        List<NpcDef> list = new ArrayList<>();
        int tz = 0x6A2A10;
        list.add(NpcDef.of("mg_tzhaar_mej_jal", "TzHaar-Mej-Jal", Minigame.FIGHT_CAVES.place(), 2, 2, tz, 0x4A1808, 0x3A1008, 0xFF8030));
        list.add(NpcDef.of("mg_tzhaar_hur_tel", "TzHaar-Hur-Tel", Minigame.FIGHT_CAVES.place(), -2, 2, tz, 0x5A2010, 0x3A1008, 0xFFA040));
        list.add(NpcDef.of("mg_tzhaar_mej_kah", "TzHaar-Mej-Kah", Minigame.FIGHT_PITS.place(), 4, -2, tz, 0x4A1808, 0x3A1008,
                0xFF9040));
        list.add(NpcDef.of("mg_tzhaar_ket_keh", "TzHaar-Ket-Keh", Minigame.FIGHT_CAVES.place(), 0, -2, tz, 0x3A1008, 0x2A0808, 0xFF6020));
        list.add(NpcDef.of("mg_void_knight_squire", "Squire", Minigame.PEST_CONTROL.place(), 2, 2, 0xC8A080, 0x6A6A70, 0x4A4A50, 0xA0A0A8));
        list.add(NpcDef.of("mg_ignisia", "Ignisia", Minigame.WINTERTODT.place(), 2, 2, 0xC8A080, 0xB04020, 0x6A2010, 0xE06020));
        list.add(NpcDef.of("mg_first_mate_deri", "First Mate Deri", Minigame.TEMPOROSS.place(), 2, 2, 0xC09070, 0x3A5A8A, 0x2A2A3A, 0x2A2A2A));
        list.add(NpcDef.of("mg_apprentice_cordelia", "Apprentice Cordelia", Minigame.GUARDIANS_OF_THE_RIFT.place(), 2, 2, 0xC8A080,
                0x6A2A8A, 0x4A1A6A, 0x8A3AAA));
        list.add(NpcDef.of("mg_commander_connad", "Commander Connad", Minigame.BARBARIAN_ASSAULT.place(), 2, 2, 0xC09070, 0x8A5A2A,
                0x5A3A1A, 0xB08040));
        list.add(NpcDef.of("mg_lanthus", "Lanthus", Minigame.CASTLE_WARS.place(), 2, 2, 0xC8A080, 0xE0C060, 0x8A6A3A, 0x6A4A2A));
        list.add(NpcDef.of("mg_harrallak_menarous", "Harrallak Menarous", Minigame.WARRIORS_GUILD.place(), 2, 2, 0xC8A080, 0x9A9AA0,
                0x6A6A70, 0x6A4A2A));
        list.add(NpcDef.of("mg_mta_rewards_guardian", "Rewards Guardian", Minigame.MAGE_TRAINING_ARENA.place(), 2, 2, 0x8A8AA0,
                0x4A3A8A, 0x3A2A6A, 0x8A6AC0));
        list.add(NpcDef.of("mg_simon_templeton", "Simon Templeton", Minigame.PYRAMID_PLUNDER.place(), 2, 2, 0xC8A080, 0xC0A060,
                0x8A6A3A, 0xE0D0A0));
        list.add(NpcDef.of("mg_guardian_mummy", "Guardian mummy", Minigame.PYRAMID_PLUNDER.place(), -2, 2, 0xD0C8A0, 0xD0C8A0,
                0xB0A880, 0xD0C8A0));
        list.add(NpcDef.of("mg_valindor", "Valindor", Minigame.HALLOWED_SEPULCHRE.place(), 2, 2, 0xC0C0D0, 0x3A3A4A, 0x2A2A3A, 0x9A9AB0));
        list.add(NpcDef.of("mg_pirate_jackie", "Pirate Jackie the Fruit", Minigame.BRIMHAVEN_AGILITY_ARENA.place(), 2, 2, 0x8A6A4A,
                0xE0A040, 0x3A3A6A, 0xC02020));
        list.add(NpcDef.of("mg_aluft_gianne", "Aluft Gianne snr.", Minigame.GNOME_RESTAURANT.place(), 2, 2, 0xC8A080, 0xF0F0F0,
                0x6A9A4A, 0xF0F0F0));
        list.add(NpcDef.of("mg_honest_jimmy", "Honest Jimmy", Minigame.TROUBLE_BREWING.place(), 2, 2, 0xC09070, 0x3060C0, 0x2A2A3A,
                0x2A2A2A));
        list.add(NpcDef.of("mg_farmer_gricoller", "Farmer Gricoller", Minigame.TITHE_FARM.place(), 2, 2, 0xC8A080, 0x6A8A3A, 0x5A4A2A,
                0xE0C060));
        list.add(NpcDef.of("mg_kovac", "Kovac", Minigame.GIANTS_FOUNDRY.place(), 2, 2, 0xA08060, 0x6A4A2A, 0x4A3A2A, 0x5A3A1A));
        list.add(NpcDef.of("mg_amy", "Amy", Minigame.MAHOGANY_HOMES.place(), 2, 2, 0xC8A080, 0x6A8AA0, 0x4A5A7A, 0xC0A040));
        list.add(NpcDef.of(ChampionsChallenge.LARXUS, "Larxus", Minigame.CHAMPIONS_CHALLENGE.place(), 2, 2, 0xC8A080, 0x3A4A8A,
                0x2A2A5A, 0xC0A040));
        list.add(NpcDef.of("mg_murphy", "Murphy", Minigame.FISHING_TRAWLER.place(), 2, 2, 0xC09070, 0x3A6A8A, 0x2A3A4A, 0x3A3A3A));
        list.add(NpcDef.of("mg_brian_orichard", "Brian O'Richard", Minigame.ROGUES_DEN.place(), 2, 2, 0xC8A080, 0x3A3A40, 0x2A2A30,
                0x2A2A30));
        list.add(NpcDef.of("mg_dominic_onion", "Dominic Onion", Minigame.NIGHTMARE_ZONE.place(), 2, 2, 0xC8A080, 0x4A6AA0, 0x3A4A7A,
                0xE0E0E0));
        list.add(NpcDef.of("mg_nomad", "Nomad", Minigame.SOUL_WARS.place(), 2, 2, 0xC09070, 0x5A3A8A, 0x3A2A5A, 0x2A2A2A));
        list.add(NpcDef.of("mg_sorceress_apprentice", "Apprentice", Minigame.SORCERESS_GARDEN.place(), 2, 2, 0xC09070, 0x6A3A8A,
                0x4A2A6A, 0x2A1A1A));
        list.add(NpcDef.of("mg_mortton_priest", "Ulsquire Shauncy", Minigame.SHADES_OF_MORTTON.place(), 2, 2, 0xB0B0A0, 0x3A3A3A,
                0x2A2A2A, 0x8A8A8A));
        return list;
    }

    /** NPC id -> shop id opened when talking to them. */
    public static final Map<String, String> NPC_SHOPS = Map.ofEntries(
            Map.entry("mg_tzhaar_hur_tel", "tzhaar"),
            Map.entry("mg_void_knight_squire", "pest_control"),
            Map.entry("mg_apprentice_cordelia", "guardians_of_the_rift"),
            Map.entry("mg_commander_connad", "barbarian_assault"),
            Map.entry("mg_lanthus", "castle_wars"),
            Map.entry("mg_mta_rewards_guardian", "mage_training_arena"),
            Map.entry("mg_valindor", "hallowed_sepulchre"),
            Map.entry("mg_pirate_jackie", "brimhaven_agility_arena"),
            Map.entry("mg_aluft_gianne", "gnome_restaurant"),
            Map.entry("mg_honest_jimmy", "trouble_brewing"),
            Map.entry("mg_farmer_gricoller", "tithe_farm"),
            Map.entry("mg_kovac", "giants_foundry"),
            Map.entry("mg_amy", "mahogany_homes"),
            Map.entry("mg_dominic_onion", "nightmare_zone"),
            Map.entry("mg_nomad", "soul_wars"));

    public static List<CollectionLogData.Page> collectionLog() {
        List<CollectionLogData.Page> pages = new ArrayList<>();
        String c = "Minigames";
        pages.add(new CollectionLogData.Page(c, "Fight Caves", List.of("fire_cape", "tzrek_jad")));
        pages.add(ChampionsChallenge.collectionPage(c));
        pages.add(new CollectionLogData.Page(c, "The Inferno", List.of("infernal_cape", "jal_nib_rek")));
        pages.add(new CollectionLogData.Page(c, "Pest Control", List.of("void_knight_top", "void_knight_robe", "void_knight_gloves",
                "void_mage_helm", "void_ranger_helm", "void_melee_helm", "void_knight_mace", "elite_void_top", "elite_void_robe",
                "void_seal")));
        pages.add(new CollectionLogData.Page(c, "Wintertodt", List.of("phoenix", "tome_of_fire", "burnt_page", "pyromancer_hood",
                "pyromancer_garb", "pyromancer_robe", "pyromancer_boots", "warm_gloves", "bruma_torch", "dragon_axe")));
        pages.add(new CollectionLogData.Page(c, "Tempoross", List.of("tiny_tempor", "fish_barrel", "tackle_box", "big_harpoonfish",
                "tome_of_water", "soaked_page", "dragon_harpoon", "spirit_flakes")));
        pages.add(new CollectionLogData.Page(c, "Guardians of the Rift", List.of("abyssal_protector", "abyssal_needle",
                "abyssal_lantern", "hat_of_the_eye", "robe_top_of_the_eye", "robe_bottoms_of_the_eye", "boots_of_the_eye",
                "catalytic_talisman", "elemental_talisman", "abyssal_green_dye", "abyssal_blue_dye", "abyssal_red_dye",
                "intricate_pouch", "abyssal_pearls")));
        pages.add(new CollectionLogData.Page(c, "Barbarian Assault", List.of("pet_penance_queen", "fighter_torso", "penance_gloves",
                "penance_skirt", "fighter_hat", "ranger_hat", "runner_hat", "healer_hat", "runner_boots", "granite_body")));
        pages.add(new CollectionLogData.Page(c, "Castle Wars", List.of("saradomin_halo", "zamorak_halo", "saradomin_hood",
                "zamorak_hood", "guthix_halo", "red_decorative_helm", "red_decorative_body", "white_decorative_helm",
                "white_decorative_body", "gold_decorative_helm", "gold_decorative_body")));
        List<String> defenders = new ArrayList<>();
        for (String t : MinigameItems.DEFENDER_TIERS) {
            defenders.add(t + "_defender");
        }
        pages.add(new CollectionLogData.Page(c, "Warriors' Guild", defenders));
        pages.add(new CollectionLogData.Page(c, "Mage Training Arena", List.of("infinity_hat", "infinity_top", "infinity_bottoms",
                "infinity_boots", "infinity_gloves", "beginner_wand", "apprentice_wand", "teacher_wand", "master_wand", "mages_book",
                "bones_to_peaches_tablet")));
        pages.add(new CollectionLogData.Page(c, "Pyramid Plunder", List.of("pharaohs_sceptre", "jewelled_golden_statuette")));
        pages.add(new CollectionLogData.Page(c, "Hallowed Sepulchre", List.of("ring_of_endurance", "mysterious_page", "dark_dye",
                "dark_acorn", "hallowed_crystal_shard", "hallowed_focus", "hallowed_symbol", "hallowed_hammer", "hallowed_grapple",
                "hallowed_ring", "strange_old_lockpick")));
        pages.add(new CollectionLogData.Page(c, "Brimhaven Agility Arena", List.of("pirates_hook")));
        pages.add(new CollectionLogData.Page(c, "Gnome Restaurant", List.of("gnome_scarf", "gnome_goggles", "mint_cake")));
        List<String> naval = new ArrayList<>();
        for (String colour : MinigameItems.NAVAL_COLOURS) {
            naval.addAll(List.of(colour + "_naval_shirt", colour + "_tricorn_hat", colour + "_navy_slacks"));
        }
        naval.add("the_stuff");
        pages.add(new CollectionLogData.Page(c, "Trouble Brewing", naval));
        pages.add(new CollectionLogData.Page(c, "Shades of Mort'ton", List.of("amulet_of_the_damned", "flamtaer_bag", "fine_cloth",
                "bronze_locks", "steel_locks", "black_locks", "silver_locks", "gold_locks", "runescroll_of_swampbark",
                "runescroll_of_bloodbark", "zealots_helm", "zealots_robe_top", "zealots_robe_bottom", "zealots_boots")));
        pages.add(new CollectionLogData.Page(c, "Tithe Farm", List.of("farmers_strawhat", "farmers_jacket", "farmers_boro_trousers",
                "farmers_boots", "gricollers_can", "seed_box", "herb_sack")));
        pages.add(new CollectionLogData.Page(c, "Giants' Foundry", List.of("smiths_tunic", "smiths_trousers", "smiths_boots",
                "smiths_gloves", "colossal_blade", "double_ammo_mould")));
        pages.add(new CollectionLogData.Page(c, "Mahogany Homes", List.of("carpenters_helmet", "carpenters_shirt", "carpenters_trousers",
                "carpenters_boots", "plank_sack", "amys_saw", "hosidius_blueprints")));
        pages.add(new CollectionLogData.Page(c, "Fishing Trawler", List.of("angler_hat", "angler_top", "angler_waders", "angler_boots")));
        pages.add(new CollectionLogData.Page(c, "Soul Wars", List.of("soul_cape")));
        pages.add(new CollectionLogData.Page(c, "Rogues' Den", List.of("rogue_mask", "rogue_top", "rogue_trousers", "rogue_gloves",
                "rogue_boots")));
        return pages;
    }
}
