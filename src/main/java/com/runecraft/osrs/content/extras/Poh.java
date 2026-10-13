package com.runecraft.osrs.content.extras;

import com.runecraft.osrs.Skill;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Player-owned house: rooms (OSRS level and cost), their furniture hotspots and the furniture that
 * can be built in each (Construction level, XP and materials consumed from the inventory), plus
 * the destinations of the functional furniture (portals, jewellery box, spirit tree, lectern
 * tablets). Rooms, levels and costs follow OSRS; furniture marked approx has estimated XP or
 * materials. Garden plants are bought from the garden centre, so their "material" is coins.
 */
public final class Poh {
    /** Size of a room in blocks (OSRS rooms are 8x8 tiles; interior 1..10, walls at 0 and 11). */
    public static final int ROOM = 12;

    public enum Effect {
        DECOR, SEAT, BED, EXIT_PORTAL, ALTAR, BURNER, POOL, PORTAL_FRAME, LECTERN, JEWELLERY_BOX, SPIRIT_TREE, LARDER,
        BOOKCASE, STOVE, WORKBENCH, PET_HOUSE, SPELLBOOK_ALTAR, CHEST, NEXUS, BELL_PULL,
        /** House fairy ring (power 1) or spirit tree & fairy ring (power 2: also a spirit tree). */
        FAIRY_RING
    }

    /**
     * A furniture piece. {@code power}: altar XP multiplier in percent, pool tier, lectern tier,
     * jewellery box tier or larder tier. {@code otherSkill}/{@code otherLevel}: an extra requirement
     * (spirit tree needs Farming).
     */
    public record Furniture(String id, String name, int level, double xp, Map<String, Integer> materials, Effect effect, int power,
                            int color, String look, Skill otherSkill, int otherLevel) {
        public String block() {
            return "ext_poh_" + id;
        }
    }

    /** A hotspot inside a room at local block offset (dx, dz) from the room's north-west corner. */
    public record Hotspot(String id, String name, int dx, int dz, List<Furniture> options) {
        public Furniture option(String furnitureId) {
            for (Furniture f : options) {
                if (f.id().equals(furnitureId)) {
                    return f;
                }
            }
            return null;
        }
    }

    public record Room(String id, String name, int level, int cost, boolean outdoor, int floorColor, List<Hotspot> hotspots) {
        public Hotspot hotspot(String hotspotId) {
            for (Hotspot h : hotspots) {
                if (h.id().equals(hotspotId)) {
                    return h;
                }
            }
            return null;
        }

        public Hotspot hotspotAt(int dx, int dz) {
            for (Hotspot h : hotspots) {
                if (h.dx() == dx && h.dz() == dz) {
                    return h;
                }
            }
            return null;
        }
    }

    /** A teleport destination at OSRS coordinates, with the Magic level and runes to direct a portal there. */
    public record Dest(String id, String name, int x, int y, int magic, Map<String, Integer> runes, int tier) {
    }

    /** A teleport tablet made at a lectern: tablet item, Magic level, XP and runes (plus one soft clay). */
    public record Tablet(String item, String name, int magic, double xp, int lecternTier, Map<String, Integer> runes) {
    }

    /** The house teleport tablet this module adds. */
    public static final String HOUSE_TABLET = "teleport_to_house";

    public static final List<Room> ROOMS = new ArrayList<>();
    public static final Map<String, Furniture> FURNITURE = new LinkedHashMap<>();

    // ------------------------------------------------------------------ builders

    private static final List<Hotspot> HOTSPOTS = new ArrayList<>();
    private static List<Furniture> options;

    private static Furniture f(String id, String name, int level, double xp, Effect effect, int power, int color, String look,
                               Object... mats) {
        return fx(id, name, level, xp, effect, power, color, look, null, 0, mats);
    }

    private static Furniture fx(String id, String name, int level, double xp, Effect effect, int power, int color, String look,
                                Skill other, int otherLevel, Object... mats) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < mats.length; i += 2) {
            m.put((String) mats[i], (Integer) mats[i + 1]);
        }
        Furniture furniture = FURNITURE.get(id);
        if (furniture == null) {
            furniture = new Furniture(id, name, level, xp, Map.copyOf(m), effect, power, color, look, other, otherLevel);
            FURNITURE.put(id, furniture);
        }
        options.add(furniture);
        return furniture;
    }

    private static void hotspot(String id, String name, int dx, int dz) {
        options = new ArrayList<>();
        HOTSPOTS.add(new Hotspot(id, name, dx, dz, options));
    }

    private static void room(String id, String name, int level, int cost, boolean outdoor, int floor) {
        List<Hotspot> list = new ArrayList<>(HOTSPOTS);
        HOTSPOTS.clear();
        ROOMS.add(new Room(id, name, level, cost, outdoor, floor, List.copyOf(list)));
    }

    private static final int WOOD = 0x8A6A3A;
    private static final int OAK = 0x9A7A4A;
    private static final int TEAK = 0x9C7A4A;
    private static final int MAHOG = 0x6A2A1A;
    private static final int STONE = 0xA0A0A0;
    private static final int MARBLE = 0xF0F0F0;
    private static final int GOLD = 0xE0C060;
    private static final int GREEN = 0x3A8A2A;

    private static void chairs() {
        f("crude_wooden_chair", "Crude wooden chair", 1, 58, Effect.SEAT, 0, WOOD, "chair", "plank", 2, "iron_nails", 2);
        f("wooden_chair", "Wooden chair", 8, 87, Effect.SEAT, 0, WOOD, "chair", "plank", 3, "iron_nails", 2);
        f("rocking_chair", "Rocking chair", 14, 87, Effect.SEAT, 0, WOOD, "chair", "plank", 3, "iron_nails", 3);
        f("oak_chair", "Oak chair", 19, 120, Effect.SEAT, 0, OAK, "chair", "oak_plank", 2);
        f("oak_armchair", "Oak armchair", 26, 180, Effect.SEAT, 0, OAK, "chair", "oak_plank", 3);
        f("teak_armchair", "Teak armchair", 35, 180, Effect.SEAT, 0, TEAK, "chair", "teak_plank", 2);
        f("mahogany_armchair", "Mahogany armchair", 50, 280, Effect.SEAT, 0, MAHOG, "chair", "mahogany_plank", 2);
    }

    private static void bookcases() {
        f("wooden_bookcase", "Wooden bookcase", 4, 115, Effect.BOOKCASE, 0, WOOD, "bookcase", "plank", 4, "iron_nails", 4);
        f("oak_bookcase", "Oak bookcase", 29, 180, Effect.BOOKCASE, 0, OAK, "bookcase", "oak_plank", 3);
        f("mahogany_bookcase", "Mahogany bookcase", 40, 420, Effect.BOOKCASE, 0, MAHOG, "bookcase", "mahogany_plank", 3);
    }

    private static void plants(boolean big) {
        if (big) {
            f("dead_tree", "Dead tree", 5, 31, Effect.DECOR, 0, 0x6A5A3A, "tree", "coins", 1000);
            f("nice_tree", "Nice tree", 10, 44, Effect.DECOR, 0, GREEN, "tree", "coins", 2000);
            f("oak_tree", "Oak tree", 15, 70, Effect.DECOR, 0, GREEN, "tree", "coins", 5000);
            f("willow_tree", "Willow tree", 30, 100, Effect.DECOR, 0, 0x5A8A3A, "tree", "coins", 10000);
            f("maple_tree", "Maple tree", 45, 122, Effect.DECOR, 0, 0xA0602A, "tree", "coins", 15000);
            f("yew_tree", "Yew tree", 60, 141, Effect.DECOR, 0, 0x2A5A2A, "tree", "coins", 20000);
            f("magic_tree", "Magic tree", 75, 223, Effect.DECOR, 0, 0x3A7AA0, "tree", "coins", 50000);
        } else {
            f("garden_plant", "Plant", 1, 31, Effect.DECOR, 0, GREEN, "plant", "coins", 1000);
            f("small_fern", "Small fern", 6, 70, Effect.DECOR, 0, GREEN, "plant", "coins", 5000);
            f("fern", "Fern", 12, 100, Effect.DECOR, 0, GREEN, "plant", "coins", 10000);
        }
    }

    static {
        // ---------------------------------------------------------------- Parlour (1, 1,000)
        hotspot("chair", "Chair", 3, 4);
        chairs();
        hotspot("chair_2", "Chair", 8, 4);
        chairs();
        hotspot("bookcase", "Bookcase", 1, 7);
        bookcases();
        hotspot("fireplace", "Fireplace", 3, 1);
        f("clay_fireplace", "Clay fireplace", 3, 30, Effect.DECOR, 0, 0xB07A50, "fireplace", "soft_clay", 3);
        f("stone_fireplace", "Stone fireplace", 33, 40, Effect.DECOR, 0, STONE, "fireplace", "limestone_brick", 2);
        f("marble_fireplace", "Marble fireplace", 63, 500, Effect.DECOR, 0, MARBLE, "fireplace", "marble_block", 1);
        room("parlour", "Parlour", 1, 1000, false, 0x8A6A3A);

        // ---------------------------------------------------------------- Garden (1, 1,000)
        hotspot("centrepiece", "Centrepiece", 5, 5);
        f("exit_portal", "Exit portal", 1, 100, Effect.EXIT_PORTAL, 0, 0x9070E0, "portal_glow", "iron_bar", 10);
        f("decorative_rock", "Decorative rock", 5, 100, Effect.DECOR, 0, STONE, "rock", "limestone_brick", 5);
        f("pond", "Pond", 10, 100, Effect.DECOR, 0, 0x3A6AC0, "pool", "soft_clay", 10);
        f("imp_statue", "Imp statue", 15, 150, Effect.DECOR, 0, 0xA03030, "statue", "limestone_brick", 5, "soft_clay", 5);
        f("dungeon_entrance", "Dungeon entrance", 70, 500, Effect.DECOR, 0, 0x2A2A2A, "trapdoor", "marble_block", 1);
        hotspot("big_tree", "Big tree", 2, 2);
        plants(true);
        hotspot("tree", "Tree", 9, 9);
        plants(true);
        hotspot("small_plant", "Small plant", 2, 9);
        plants(false);
        room("garden", "Garden", 1, 1000, true, 0x5A9A3A);

        // ---------------------------------------------------------------- Kitchen (5, 5,000)
        hotspot("larder", "Larder", 1, 3);
        f("wooden_larder", "Wooden larder", 9, 228, Effect.LARDER, 1, WOOD, "larder", "plank", 8, "iron_nails", 8);
        f("oak_larder", "Oak larder", 33, 480, Effect.LARDER, 2, OAK, "larder", "oak_plank", 8);
        f("teak_larder", "Teak larder", 43, 750, Effect.LARDER, 3, TEAK, "larder", "teak_plank", 8, "bolt_of_cloth", 2);
        hotspot("stove", "Stove", 3, 1);
        f("firepit", "Firepit", 5, 40, Effect.STOVE, 1, 0x5A5A5A, "range", "steel_bar", 1, "soft_clay", 2);
        f("firepit_with_hook", "Firepit with hook", 11, 60, Effect.STOVE, 1, 0x5A5A5A, "range", "steel_bar", 2, "soft_clay", 2);
        f("firepit_with_pot", "Firepit with pot", 17, 80, Effect.STOVE, 1, 0x5A5A5A, "range", "steel_bar", 3, "soft_clay", 2);
        f("small_oven", "Small oven", 24, 80, Effect.STOVE, 2, 0x4A4A4A, "range", "steel_bar", 4);
        f("large_oven", "Large oven", 29, 100, Effect.STOVE, 2, 0x4A4A4A, "range", "steel_bar", 5);
        f("steel_range", "Steel range", 34, 120, Effect.STOVE, 3, 0x6A6A6A, "range", "steel_bar", 6);
        f("fancy_range", "Fancy range", 42, 160, Effect.STOVE, 3, 0x8A8A8A, "range", "steel_bar", 8);
        hotspot("kitchen_table", "Table", 5, 6);
        f("wood_kitchen_table", "Wood kitchen table", 12, 87, Effect.DECOR, 0, WOOD, "table", "plank", 3, "iron_nails", 3);
        f("oak_kitchen_table", "Oak kitchen table", 32, 180, Effect.DECOR, 0, OAK, "table", "oak_plank", 3);
        f("teak_kitchen_table", "Teak kitchen table", 52, 270, Effect.DECOR, 0, TEAK, "table", "teak_plank", 3);
        hotspot("sink", "Sink", 10, 3);
        f("pump_and_drain", "Pump and drain", 7, 100, Effect.DECOR, 0, STONE, "sink", "steel_bar", 5);
        f("pump_and_tub", "Pump and tub", 27, 200, Effect.DECOR, 0, STONE, "sink", "steel_bar", 10);
        f("sink", "Sink", 47, 300, Effect.DECOR, 0, STONE, "sink", "steel_bar", 15);
        room("kitchen", "Kitchen", 5, 5000, false, 0x9A8A6A);

        // ---------------------------------------------------------------- Dining room (10, 5,000)
        hotspot("dining_table", "Table", 5, 5);
        f("wood_dining_table", "Wood dining table", 10, 115, Effect.DECOR, 0, WOOD, "table", "plank", 4, "iron_nails", 4);
        f("oak_dining_table", "Oak dining table", 22, 240, Effect.DECOR, 0, OAK, "table", "oak_plank", 4);
        f("carved_oak_table", "Carved oak table", 31, 360, Effect.DECOR, 0, OAK, "table", "oak_plank", 6);
        f("teak_table", "Teak table", 38, 360, Effect.DECOR, 0, TEAK, "table", "teak_plank", 4);
        f("carved_teak_table", "Carved teak table", 45, 600, Effect.DECOR, 0, TEAK, "table", "teak_plank", 6, "bolt_of_cloth", 4);
        f("mahogany_table", "Mahogany table", 52, 840, Effect.DECOR, 0, MAHOG, "table", "mahogany_plank", 6);
        f("opulent_table", "Opulent table", 72, 3100, Effect.DECOR, 0, GOLD, "table", "mahogany_plank", 6, "bolt_of_cloth", 4,
                "gold_leaf", 4, "marble_block", 2);
        hotspot("bench", "Bench", 5, 3);
        f("wooden_bench", "Wooden bench", 10, 115, Effect.SEAT, 0, WOOD, "bench", "plank", 4, "iron_nails", 4);
        f("oak_bench", "Oak bench", 22, 240, Effect.SEAT, 0, OAK, "bench", "oak_plank", 4);
        f("carved_oak_bench", "Carved oak bench", 31, 240, Effect.SEAT, 0, OAK, "bench", "oak_plank", 4);
        f("teak_dining_bench", "Teak dining bench", 38, 360, Effect.SEAT, 0, TEAK, "bench", "teak_plank", 4);
        f("carved_teak_bench", "Carved teak bench", 44, 360, Effect.SEAT, 0, TEAK, "bench", "teak_plank", 4);
        f("mahogany_bench", "Mahogany bench", 52, 560, Effect.SEAT, 0, MAHOG, "bench", "mahogany_plank", 4);
        f("gilded_bench", "Gilded bench", 61, 1760, Effect.SEAT, 0, GOLD, "bench", "mahogany_plank", 4, "gold_leaf", 4);
        hotspot("bell_pull", "Bell-pull", 1, 1);
        f("rope_bell_pull", "Rope bell-pull", 26, 64, Effect.BELL_PULL, 0, OAK, "bell", "oak_plank", 1, "bolt_of_cloth", 2);
        f("bell_pull", "Bell-pull", 37, 120, Effect.BELL_PULL, 0, TEAK, "bell", "teak_plank", 1, "bolt_of_cloth", 2);
        f("posh_bell_pull", "Posh bell-pull", 60, 420, Effect.BELL_PULL, 0, GOLD, "bell", "teak_plank", 1, "bolt_of_cloth", 2,
                "gold_leaf", 1);
        room("dining_room", "Dining room", 10, 5000, false, 0x7A4A2A);

        // ---------------------------------------------------------------- Workshop (15, 10,000)
        hotspot("workbench", "Workbench", 5, 5);
        f("wooden_workbench", "Wooden workbench", 17, 143, Effect.WORKBENCH, 1, WOOD, "workbench", "plank", 5, "iron_nails", 5);
        f("oak_workbench", "Oak workbench", 32, 300, Effect.WORKBENCH, 2, OAK, "workbench", "oak_plank", 5);
        f("steel_framed_bench", "Steel framed bench", 46, 440, Effect.WORKBENCH, 3, 0x8A8A8A, "workbench", "oak_plank", 6,
                "steel_bar", 4);
        f("bench_with_vice", "Bench with vice", 62, 750, Effect.WORKBENCH, 4, 0x8A8A8A, "workbench", "oak_plank", 2, "steel_bar", 1,
                "iron_bar", 1); // approx
        f("bench_with_lathe", "Bench with lathe", 77, 1000, Effect.WORKBENCH, 5, 0x8A8A8A, "workbench", "oak_plank", 2,
                "steel_bar", 1, "iron_bar", 1); // approx
        hotspot("repair", "Repair space", 9, 5);
        f("repair_bench", "Repair bench", 15, 120, Effect.DECOR, 0, OAK, "workbench", "oak_plank", 2);
        f("whetstone", "Whetstone", 35, 260, Effect.DECOR, 0, STONE, "grindstone", "oak_plank", 4, "limestone_brick", 1);
        f("armour_stand", "Armour stand", 55, 500, Effect.DECOR, 0, 0x8A8A8A, "armour_stand", "oak_plank", 8, "limestone_brick", 1);
        hotspot("tool_store", "Tool store", 1, 3);
        f("tool_store_1", "Tool store 1", 15, 121, Effect.DECOR, 0, OAK, "crate", "oak_plank", 2);
        f("tool_store_2", "Tool store 2", 25, 121, Effect.DECOR, 0, OAK, "crate", "oak_plank", 2);
        f("tool_store_3", "Tool store 3", 35, 121, Effect.DECOR, 0, OAK, "crate", "oak_plank", 2);
        room("workshop", "Workshop", 15, 10000, false, 0x6A6A6A);

        // ---------------------------------------------------------------- Bedroom (20, 10,000)
        hotspot("bed", "Bed", 5, 5);
        f("wooden_bed", "Wooden bed", 20, 117, Effect.BED, 0, WOOD, "bed", "plank", 3, "iron_nails", 3, "bolt_of_cloth", 2);
        f("oak_bed", "Oak bed", 30, 210, Effect.BED, 0, OAK, "bed", "oak_plank", 3, "bolt_of_cloth", 2);
        f("large_oak_bed", "Large oak bed", 34, 330, Effect.BED, 0, OAK, "bed", "oak_plank", 5, "bolt_of_cloth", 2);
        f("teak_bed", "Teak bed", 40, 300, Effect.BED, 0, TEAK, "bed", "teak_plank", 3, "bolt_of_cloth", 2);
        f("large_teak_bed", "Large teak bed", 45, 480, Effect.BED, 0, TEAK, "bed", "teak_plank", 5, "bolt_of_cloth", 2);
        f("four_poster_bed", "4-poster bed", 53, 450, Effect.BED, 0, MAHOG, "bed", "mahogany_plank", 3, "bolt_of_cloth", 2);
        f("gilded_four_poster", "Gilded 4-poster", 60, 1330, Effect.BED, 0, GOLD, "bed", "mahogany_plank", 5, "bolt_of_cloth", 2,
                "gold_leaf", 2);
        hotspot("clock", "Clock", 10, 1);
        f("oak_clock", "Oak clock", 25, 142, Effect.DECOR, 0, OAK, "clock", "oak_plank", 2, "clockwork", 1);
        f("teak_clock", "Teak clock", 55, 202, Effect.DECOR, 0, TEAK, "clock", "teak_plank", 2, "clockwork", 1);
        f("gilded_clock", "Gilded clock", 85, 602, Effect.DECOR, 0, GOLD, "clock", "mahogany_plank", 2, "clockwork", 1, "gold_leaf", 1);
        hotspot("wardrobe", "Wardrobe", 1, 1);
        f("shoe_box", "Shoe box", 20, 58, Effect.DECOR, 0, WOOD, "wardrobe", "plank", 2, "iron_nails", 2);
        f("oak_drawers", "Oak drawers", 27, 120, Effect.DECOR, 0, OAK, "wardrobe", "oak_plank", 2);
        f("oak_wardrobe", "Oak wardrobe", 39, 180, Effect.DECOR, 0, OAK, "wardrobe", "oak_plank", 3);
        f("teak_drawers", "Teak drawers", 51, 180, Effect.DECOR, 0, TEAK, "wardrobe", "teak_plank", 2);
        f("teak_wardrobe", "Teak wardrobe", 63, 270, Effect.DECOR, 0, TEAK, "wardrobe", "teak_plank", 3);
        f("mahogany_wardrobe", "Mahogany wardrobe", 75, 420, Effect.DECOR, 0, MAHOG, "wardrobe", "mahogany_plank", 3);
        f("gilded_wardrobe", "Gilded wardrobe", 87, 720, Effect.DECOR, 0, GOLD, "wardrobe", "mahogany_plank", 3, "gold_leaf", 1);
        room("bedroom", "Bedroom", 20, 10000, false, 0x8A3A3A);

        // ---------------------------------------------------------------- Combat room (32, 25,000)
        hotspot("ring", "Combat ring", 5, 5);
        f("boxing_ring", "Boxing ring", 32, 420, Effect.DECOR, 0, OAK, "ring", "oak_plank", 6, "bolt_of_cloth", 4);
        f("fencing_ring", "Fencing ring", 41, 570, Effect.DECOR, 0, OAK, "ring", "oak_plank", 8, "bolt_of_cloth", 6);
        f("combat_ring", "Combat ring", 51, 630, Effect.DECOR, 0, TEAK, "ring", "teak_plank", 6, "bolt_of_cloth", 6);
        f("ranging_pedestals", "Ranging pedestals", 71, 720, Effect.DECOR, 0, TEAK, "pedestal", "teak_plank", 8);
        f("balance_beam", "Balance beam", 81, 1000, Effect.DECOR, 0, TEAK, "beam", "teak_plank", 10, "steel_bar", 4); // approx
        hotspot("storage", "Storage", 1, 1);
        f("glove_rack", "Glove rack", 34, 120, Effect.DECOR, 0, OAK, "rack", "oak_plank", 2);
        f("weapons_rack", "Weapons rack", 44, 180, Effect.DECOR, 0, TEAK, "rack", "teak_plank", 2);
        f("extra_weapons_rack", "Extra weapons rack", 54, 440, Effect.DECOR, 0, TEAK, "rack", "teak_plank", 4, "steel_bar", 4);
        room("combat_room", "Combat room", 32, 25000, false, 0x7A6A5A);

        // ---------------------------------------------------------------- Menagerie (37, 30,000)
        hotspot("pet_house", "Pet house", 5, 5);
        f("oak_pet_house", "Oak pet house", 37, 200, Effect.PET_HOUSE, 1, OAK, "kennel", "oak_plank", 4);
        f("teak_pet_house", "Teak pet house", 44, 278, Effect.PET_HOUSE, 2, TEAK, "kennel", "teak_plank", 4);
        f("mahogany_pet_house", "Mahogany pet house", 50, 371, Effect.PET_HOUSE, 3, MAHOG, "kennel", "mahogany_plank", 4);
        f("consecrated_pet_house", "Consecrated pet house", 56, 1222, Effect.PET_HOUSE, 4, 0xE0E0F0, "kennel", "mahogany_plank", 4,
                "magic_stone", 1); // approx
        f("natural_pet_house", "Natural pet house", 65, 1222, Effect.PET_HOUSE, 5, GREEN, "kennel", "mahogany_plank", 4,
                "magic_stone", 1); // approx
        hotspot("pet_feeder", "Pet feeder", 2, 2);
        f("oak_pet_feeder", "Oak pet feeder", 37, 240, Effect.DECOR, 0, OAK, "trough", "oak_plank", 4);
        f("teak_pet_feeder", "Teak pet feeder", 52, 380, Effect.DECOR, 0, TEAK, "trough", "teak_plank", 4);
        f("mahogany_pet_feeder", "Mahogany pet feeder", 67, 880, Effect.DECOR, 0, MAHOG, "trough", "mahogany_plank", 4);
        hotspot("obelisk", "Obelisk", 9, 9);
        f("mini_obelisk", "Mini obelisk", 41, 676, Effect.DECOR, 0, 0x5A4A6A, "obelisk", "marble_block", 1); // approx
        room("menagerie", "Menagerie", 37, 30000, true, 0x6A9A4A);

        // ---------------------------------------------------------------- Study (40, 50,000)
        hotspot("lectern", "Lectern", 5, 5);
        f("oak_lectern", "Oak lectern", 40, 60, Effect.LECTERN, 1, OAK, "lectern", "oak_plank", 1);
        f("eagle_lectern", "Eagle lectern", 47, 120, Effect.LECTERN, 2, OAK, "lectern", "oak_plank", 2);
        f("demon_lectern", "Demon lectern", 47, 120, Effect.LECTERN, 2, OAK, "lectern", "oak_plank", 2);
        f("teak_eagle_lectern", "Teak eagle lectern", 57, 180, Effect.LECTERN, 3, TEAK, "lectern", "teak_plank", 2);
        f("teak_demon_lectern", "Teak demon lectern", 57, 180, Effect.LECTERN, 3, TEAK, "lectern", "teak_plank", 2);
        f("mahogany_eagle_lectern", "Mahogany eagle lectern", 67, 580, Effect.LECTERN, 4, MAHOG, "lectern", "mahogany_plank", 2,
                "gold_leaf", 1);
        f("mahogany_demon_lectern", "Mahogany demon lectern", 67, 580, Effect.LECTERN, 4, MAHOG, "lectern", "mahogany_plank", 2,
                "gold_leaf", 1);
        hotspot("crystal_ball", "Crystal ball", 2, 8);
        f("crystal_ball", "Crystal ball", 42, 280, Effect.DECOR, 0, 0xC0E0F0, "crystal_ball", "teak_plank", 3, "molten_glass", 1);
        f("elemental_sphere", "Elemental sphere", 54, 580, Effect.DECOR, 0, 0x80C0F0, "crystal_ball", "teak_plank", 3,
                "molten_glass", 1, "gold_leaf", 1);
        f("crystal_of_power", "Crystal of power", 66, 890, Effect.DECOR, 0, 0xA080F0, "crystal_ball", "mahogany_plank", 2,
                "molten_glass", 1, "magic_stone", 1);
        hotspot("telescope", "Telescope", 9, 2);
        f("wooden_telescope", "Wooden telescope", 44, 121, Effect.DECOR, 0, OAK, "telescope", "oak_plank", 2, "molten_glass", 1);
        f("teak_telescope", "Teak telescope", 64, 181, Effect.DECOR, 0, TEAK, "telescope", "teak_plank", 2, "molten_glass", 1);
        f("mahogany_telescope", "Mahogany telescope", 84, 580, Effect.DECOR, 0, MAHOG, "telescope", "mahogany_plank", 2,
                "molten_glass", 1);
        hotspot("study_bookcase", "Bookcase", 1, 3);
        bookcases();
        room("study", "Study", 40, 50000, false, 0x5A3A2A);

        // ---------------------------------------------------------------- Costume room (42, 50,000)
        hotspot("treasure_chest", "Treasure chest", 2, 2);
        f("oak_treasure_chest", "Oak treasure chest", 48, 120, Effect.CHEST, 0, OAK, "chest", "oak_plank", 2);
        f("teak_treasure_chest", "Teak treasure chest", 66, 180, Effect.CHEST, 0, TEAK, "chest", "teak_plank", 2);
        f("mahogany_treasure_chest", "Mahogany treasure chest", 84, 280, Effect.CHEST, 0, MAHOG, "chest", "mahogany_plank", 2);
        hotspot("armour_case", "Armour case", 9, 2);
        f("oak_armour_case", "Oak armour case", 46, 180, Effect.DECOR, 0, OAK, "armour_stand", "oak_plank", 3);
        f("teak_armour_case", "Teak armour case", 64, 270, Effect.DECOR, 0, TEAK, "armour_stand", "teak_plank", 3);
        f("mahogany_armour_case", "Mahogany armour case", 82, 420, Effect.DECOR, 0, MAHOG, "armour_stand", "mahogany_plank", 3);
        hotspot("magic_wardrobe", "Magic wardrobe", 5, 9);
        f("oak_magic_wardrobe", "Oak magic wardrobe", 42, 240, Effect.DECOR, 0, OAK, "wardrobe", "oak_plank", 4);
        f("teak_magic_wardrobe", "Teak magic wardrobe", 60, 360, Effect.DECOR, 0, TEAK, "wardrobe", "teak_plank", 4);
        f("mahogany_magic_wardrobe", "Mahogany magic wardrobe", 78, 560, Effect.DECOR, 0, MAHOG, "wardrobe", "mahogany_plank", 4);
        room("costume_room", "Costume room", 42, 50000, false, 0x6A3A6A);

        // ---------------------------------------------------------------- Chapel (45, 50,000)
        hotspot("altar", "Altar", 5, 3);
        f("oak_altar", "Oak altar", 45, 240, Effect.ALTAR, 110, OAK, "altar", "oak_plank", 4);
        f("teak_altar", "Teak altar", 50, 360, Effect.ALTAR, 120, TEAK, "altar", "teak_plank", 4);
        f("cloth_covered_altar", "Cloth-covered altar", 56, 390, Effect.ALTAR, 130, 0xE0E0F0, "altar", "teak_plank", 4,
                "bolt_of_cloth", 2);
        f("mahogany_altar", "Mahogany altar", 60, 590, Effect.ALTAR, 140, MAHOG, "altar", "mahogany_plank", 4, "bolt_of_cloth", 2);
        f("limestone_altar", "Limestone altar", 64, 910, Effect.ALTAR, 150, 0xD8D0B0, "altar", "mahogany_plank", 6, "bolt_of_cloth", 2,
                "limestone_brick", 2);
        f("marble_altar", "Marble altar", 70, 1030, Effect.ALTAR, 200, MARBLE, "altar", "marble_block", 2, "bolt_of_cloth", 2);
        f("gilded_altar", "Gilded altar", 75, 2230, Effect.ALTAR, 250, GOLD, "altar", "marble_block", 2, "bolt_of_cloth", 2,
                "gold_leaf", 4);
        for (String side : new String[]{"burner_left", "burner_right"}) {
            hotspot(side, "Lamp", side.endsWith("left") ? 2 : 8, 3);
            f("oak_burner", "Oak incense burner", 55, 280, Effect.BURNER, 0, OAK, "burner", "oak_plank", 4, "steel_bar", 2);
            f("teak_burner", "Teak incense burner", 64, 300, Effect.BURNER, 0, TEAK, "burner", "teak_plank", 4, "steel_bar", 2);
            f("mahogany_burner", "Mahogany incense burner", 72, 300, Effect.BURNER, 0, MAHOG, "burner", "mahogany_plank", 4,
                    "steel_bar", 2);
        }
        room("chapel", "Chapel", 45, 50000, false, 0xB0A890);

        // ---------------------------------------------------------------- Portal chamber (50, 100,000)
        int[][] frames = {{2, 5}, {5, 9}, {9, 5}};
        for (int i = 0; i < frames.length; i++) {
            hotspot("portal_" + (i + 1), "Portal frame", frames[i][0], frames[i][1]);
            f("teak_portal", "Teak portal", 50, 270, Effect.PORTAL_FRAME, 1, TEAK, "portal_glow", "teak_plank", 3);
            f("mahogany_portal", "Mahogany portal", 65, 420, Effect.PORTAL_FRAME, 2, MAHOG, "portal_glow", "mahogany_plank", 3);
            f("marble_portal", "Marble portal", 80, 1500, Effect.PORTAL_FRAME, 3, MARBLE, "portal_glow", "marble_block", 3);
        }
        hotspot("portal_centrepiece", "Centrepiece", 5, 5);
        f("teleport_focus", "Teleport focus", 50, 40, Effect.DECOR, 0, 0xA080F0, "orb", "limestone_brick", 2);
        f("greater_teleport_focus", "Greater teleport focus", 65, 500, Effect.DECOR, 0, 0xA080F0, "orb", "marble_block", 1);
        f("scrying_pool", "Scrying pool", 80, 2000, Effect.DECOR, 0, 0x60A0E0, "pool", "marble_block", 4); // approx
        room("portal_chamber", "Portal chamber", 50, 100000, false, 0x4A3A6A);

        // ---------------------------------------------------------------- Throne room (60, 150,000)
        hotspot("throne", "Throne", 5, 2);
        f("oak_throne", "Oak throne", 60, 800, Effect.SEAT, 0, OAK, "throne", "oak_plank", 5, "marble_block", 1);
        f("teak_throne", "Teak throne", 67, 1450, Effect.SEAT, 0, TEAK, "throne", "teak_plank", 5, "marble_block", 2);
        f("mahogany_throne", "Mahogany throne", 74, 2200, Effect.SEAT, 0, MAHOG, "throne", "mahogany_plank", 5, "marble_block", 3);
        f("gilded_throne", "Gilded throne", 81, 1700, Effect.SEAT, 0, GOLD, "throne", "mahogany_plank", 5, "marble_block", 2,
                "gold_leaf", 3);
        f("crystal_throne", "Crystal throne", 95, 15000, Effect.SEAT, 0, 0xC0E0F0, "throne", "magic_stone", 15); // approx
        hotspot("lever", "Lever", 8, 2);
        f("oak_lever", "Oak lever", 68, 300, Effect.DECOR, 0, OAK, "lever", "oak_plank", 5);
        f("teak_lever", "Teak lever", 78, 450, Effect.DECOR, 0, TEAK, "lever", "teak_plank", 5);
        f("mahogany_lever", "Mahogany lever", 88, 700, Effect.DECOR, 0, MAHOG, "lever", "mahogany_plank", 5);
        room("throne_room", "Throne room", 60, 150000, false, 0x8A2A2A);

        // ---------------------------------------------------------------- Oubliette (65, 150,000)
        hotspot("oubliette_floor", "Floor", 5, 5);
        f("spikes", "Spikes", 65, 623, Effect.DECOR, 0, 0x8A8A8A, "spikes", "steel_bar", 20, "coins", 50000); // approx
        f("tentacle_pool", "Tentacle pool", 71, 326, Effect.DECOR, 0, 0x3A6A4A, "pool", "coins", 100000); // approx
        f("flame_pit", "Flame pit", 77, 357, Effect.DECOR, 0, 0xE06020, "fire_pit", "coins", 125000); // approx
        f("rocnar", "Rocnar", 83, 387, Effect.DECOR, 0, 0x5A5A5A, "statue", "coins", 150000); // approx
        hotspot("prison", "Prison space", 1, 1);
        f("oak_cage", "Oak cage", 65, 640, Effect.DECOR, 0, OAK, "cage", "oak_plank", 10, "steel_bar", 2);
        f("steel_cage", "Steel cage", 79, 1100, Effect.DECOR, 0, 0x8A8A8A, "cage", "oak_plank", 10, "steel_bar", 20); // approx
        room("oubliette", "Oubliette", 65, 150000, false, 0x3A3A3A);

        // ---------------------------------------------------------------- Superior garden (65, 75,000)
        hotspot("pool", "Pool", 5, 5);
        // Each pool is an upgrade of the one before it (OSRS); materials are the upgrade's own.
        f("restoration_pool", "Restoration pool", 65, 706, Effect.POOL, 1, 0x6080C0, "pool", "limestone_brick", 5,
                "minecraft:water_bucket", 5, "soul_rune", 1000, "body_rune", 1000);
        f("revitalisation_pool", "Revitalisation pool", 70, 850, Effect.POOL, 2, 0x5090C0, "pool", "stamina_potion", 10);
        f("rejuvenation_pool", "Rejuvenation pool", 80, 900, Effect.POOL, 3, 0x40A0C0, "pool", "prayer_potion", 10);
        f("fancy_rejuvenation_pool", "Fancy rejuvenation pool", 85, 1950, Effect.POOL, 4, 0x40B0D0, "pool", "marble_block", 2,
                "super_restore", 10);
        f("ornate_rejuvenation_pool", "Ornate rejuvenation pool", 90, 3107, Effect.POOL, 5, 0x40C0E0, "pool", "gold_leaf", 2,
                "anti_venom_plus", 10); // approx materials
        hotspot("teleport", "Teleport", 2, 2);
        // Wiki: spirit tree 75 (83 Farming), 350 XP in both; fairy ring 85, 535 XP (10 mushrooms + a fairy enchantment,
        // 100 coins from Fairy Fixit); spirit tree & fairy ring 95 (83 Farming), 885 XP in both. The fairy ring's code is DIQ.
        fx("spirit_tree", "Spirit tree", 75, 350, Effect.SPIRIT_TREE, 1, 0x7A9A4A, "spirit_tree", Skill.FARMING, 83,
                "coins", 250000); // approx: bagged spirit tree
        f("fairy_ring", "Fairy ring", 85, 535, Effect.FAIRY_RING, 1, 0x9A6AC0, "mushroom", "coins", 100); // approx: the enchantment only
        fx("spirit_tree_fairy_ring", "Spirit tree & fairy ring", 95, 885, Effect.FAIRY_RING, 2, 0x7A9A6A, "spirit_tree",
                Skill.FARMING, 83, "coins", 250100); // approx: sapling + enchantment as coins
        hotspot("superior_tree", "Tree", 9, 9);
        plants(true);
        room("superior_garden", "Superior garden", 65, 75000, true, 0x4A8A3A);

        // ---------------------------------------------------------------- Dungeon (70, 7,500)
        hotspot("guard", "Guard", 5, 5);
        f("skeleton_guard", "Skeleton guard", 70, 223, Effect.DECOR, 0, 0xE0E0D0, "statue", "coins", 50000);
        f("guard_dog", "Guard dog", 74, 273, Effect.DECOR, 0, 0x6A4A2A, "statue", "coins", 75000);
        f("hobgoblin_guard", "Hobgoblin guard", 78, 316, Effect.DECOR, 0, 0x6A8A3A, "statue", "coins", 100000);
        f("baby_red_dragon", "Baby red dragon", 82, 387, Effect.DECOR, 0, 0xB03030, "statue", "coins", 150000);
        hotspot("dungeon_light", "Lighting", 1, 1);
        f("candle", "Candle", 72, 243, Effect.DECOR, 0, 0xE0D080, "candle", "oak_plank", 4); // approx
        f("torch", "Torch", 80, 244, Effect.DECOR, 0, 0xE08020, "torch", "oak_plank", 4); // approx
        room("dungeon", "Dungeon", 70, 7500, false, 0x4A4A4A);

        // ---------------------------------------------------------------- Treasure room (75, 250,000)
        hotspot("treasure", "Treasure", 5, 7);
        f("wooden_crate", "Wooden crate", 75, 143, Effect.CHEST, 0, WOOD, "crate", "plank", 5, "iron_nails", 5);
        f("oak_chest", "Oak chest", 79, 340, Effect.CHEST, 0, OAK, "chest", "oak_plank", 5, "steel_bar", 2);
        f("teak_chest", "Teak chest", 83, 530, Effect.CHEST, 0, TEAK, "chest", "teak_plank", 5, "steel_bar", 4);
        f("mahogany_chest", "Mahogany chest", 87, 1000, Effect.CHEST, 0, MAHOG, "chest", "mahogany_plank", 5, "gold_leaf", 1);
        f("magic_chest", "Magic chest", 91, 1000, Effect.CHEST, 0, 0x7A50C0, "chest", "magic_stone", 1);
        hotspot("treasure_guardian", "Monster", 5, 3);
        f("demon_guardian", "Demon", 75, 707, Effect.DECOR, 0, 0xA02020, "statue", "coins", 150000);
        f("kalphite_soldier", "Kalphite soldier", 80, 1208, Effect.DECOR, 0, 0x8A7A3A, "statue", "coins", 200000);
        f("tok_xil", "Tok-Xil", 85, 1823, Effect.DECOR, 0, 0x6A3A2A, "statue", "coins", 250000);
        f("dagannoth_guardian", "Dagannoth", 90, 2509, Effect.DECOR, 0, 0x4A6A4A, "statue", "coins", 300000);
        f("steel_dragon_guardian", "Steel dragon", 95, 3240, Effect.DECOR, 0, 0x8A8A9A, "statue", "coins", 350000);
        room("treasure_room", "Treasure room", 75, 250000, false, 0x6A5A2A);

        // ---------------------------------------------------------------- Achievement gallery (80, 200,000)
        hotspot("jewellery_box", "Jewellery box", 5, 5);
        // Fancy and ornate boxes upgrade the box below them (OSRS); materials are the upgrade's own.
        f("basic_jewellery_box", "Basic jewellery box", 81, 605, Effect.JEWELLERY_BOX, 1, OAK, "chest", "teak_plank", 2,
                "ring_of_dueling_8", 8, "games_necklace_8", 8); // approx materials
        f("fancy_jewellery_box", "Fancy jewellery box", 86, 1215, Effect.JEWELLERY_BOX, 2, TEAK, "chest", "mahogany_plank", 2,
                "skills_necklace_4", 8, "combat_bracelet_4", 8); // approx xp/materials
        f("ornate_jewellery_box", "Ornate jewellery box", 91, 2680, Effect.JEWELLERY_BOX, 3, GOLD, "chest", "gold_leaf", 2,
                "ring_of_wealth_5", 8, "amulet_of_glory_4", 8);
        hotspot("gallery_altar", "Altar space", 2, 5);
        f("ancient_altar", "Ancient altar", 80, 1000, Effect.SPELLBOOK_ALTAR, 1, 0x7A3A9A, "altar", "magic_stone", 1, "marble_block", 1);
        f("lunar_altar", "Lunar altar", 80, 1000, Effect.SPELLBOOK_ALTAR, 2, 0x9AA0C0, "altar", "magic_stone", 1, "marble_block", 1);
        f("dark_altar", "Dark altar", 80, 1000, Effect.SPELLBOOK_ALTAR, 3, 0x3A2A3A, "altar", "magic_stone", 1, "marble_block", 1);
        f("occult_altar", "Occult altar", 90, 3000, Effect.SPELLBOOK_ALTAR, 4, 0x5A3A7A, "altar", "magic_stone", 4, "marble_block", 3);
        // approx: spellbook altar XP (the wiki values could not be confirmed)
        room("achievement_gallery", "Achievement gallery", 80, 200000, false, 0x8A7A5A);

        // ---------------------------------------------------------------- Portal nexus (72, 200,000)
        hotspot("nexus", "Portal nexus", 5, 5);
        f("marble_portal_nexus", "Portal nexus", 72, 3000, Effect.NEXUS, 1, MARBLE, "portal_glow", "marble_block", 4,
                "magic_stone", 1); // approx xp/materials
        f("gilded_portal_nexus", "Gilded portal nexus", 82, 1500, Effect.NEXUS, 2, GOLD, "portal_glow", "gold_leaf", 3); // approx
        f("crystalline_portal_nexus", "Crystalline portal nexus", 92, 3000, Effect.NEXUS, 3, 0xC0E0F0, "portal_glow",
                "magic_stone", 2); // approx
        room("portal_nexus", "Portal nexus", 72, 200000, false, 0x3A2A5A);
    }

    // ------------------------------------------------------------------ teleports

    private static Map<String, Integer> runes(Object... kv) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], (Integer) kv[i + 1]);
        }
        return Map.copyOf(m);
    }

    /**
     * Portal chamber destinations: Magic level and runes per cast; directing a portal costs 100 casts
     * of the spell (like OSRS).
     */
    public static final List<Dest> PORTAL_DESTS = List.of(
            new Dest("varrock", "Varrock", 3213, 3424, 25, runes("law_rune", 1, "air_rune", 3, "fire_rune", 1), 1),
            new Dest("lumbridge", "Lumbridge", 3222, 3218, 31, runes("law_rune", 1, "air_rune", 3, "earth_rune", 1), 1),
            new Dest("falador", "Falador", 2965, 3378, 37, runes("law_rune", 1, "air_rune", 3, "water_rune", 1), 1),
            new Dest("camelot", "Camelot", 2757, 3477, 45, runes("law_rune", 1, "air_rune", 5), 1),
            new Dest("ardougne", "Ardougne", 2662, 3305, 51, runes("law_rune", 2, "water_rune", 2), 1),
            new Dest("yanille", "Yanille (Watchtower)", 2549, 3112, 58, runes("law_rune", 2, "earth_rune", 2), 1),
            new Dest("senntisten", "Senntisten", 3322, 3336, 60, runes("law_rune", 2, "soul_rune", 1), 1),
            new Dest("trollheim", "Trollheim", 2890, 3676, 61, runes("law_rune", 2, "fire_rune", 2), 1),
            new Dest("kharyrll", "Kharyrll", 3492, 3471, 66, runes("law_rune", 2, "blood_rune", 1), 1),
            new Dest("kourend", "Kourend Castle", 1643, 3673, 69, runes("law_rune", 2, "soul_rune", 2, "water_rune", 4, "fire_rune", 5), 1),
            new Dest("waterbirth", "Waterbirth Island", 2546, 3757, 72, runes("law_rune", 1, "astral_rune", 2, "water_rune", 1), 1),
            new Dest("carrallanger", "Carrallanger", 3157, 3666, 84, runes("law_rune", 2, "soul_rune", 2), 1),
            new Dest("annakarl", "Annakarl", 3288, 3886, 90, runes("law_rune", 2, "blood_rune", 2), 1));

    /** Casts of the teleport spell needed to direct a portal. */
    public static final int PORTAL_CASTS = 100;

    /** Jewellery box teleports by box tier (basic 1, fancy 2, ornate 3). */
    public static final List<Dest> JEWELLERY_DESTS = List.of(
            // basic: ring of dueling + games necklace
            new Dest("pvp_arena", "Emir's Arena", 3315, 3235, 0, Map.of(), 1),
            new Dest("castle_wars", "Castle Wars", 2440, 3090, 0, Map.of(), 1),
            new Dest("ferox_enclave", "Ferox Enclave", 3151, 3635, 0, Map.of(), 1),
            new Dest("burthorpe", "Burthorpe", 2899, 3553, 0, Map.of(), 1),
            new Dest("barbarian_outpost", "Barbarian Outpost", 2520, 3571, 0, Map.of(), 1),
            new Dest("corporeal_beast", "Corporeal Beast", 3206, 3681, 0, Map.of(), 1), // approx (cave entrance)
            new Dest("tears_of_guthix", "Tears of Guthix", 3252, 3175, 0, Map.of(), 1), // the Tears of Guthix cave as built here
            new Dest("wintertodt", "Wintertodt Camp", 1631, 3940, 0, Map.of(), 1),
            // fancy: combat bracelet + skills necklace
            new Dest("warriors_guild", "Warriors' Guild", 2882, 3547, 0, Map.of(), 2),
            new Dest("champions_guild", "Champions' Guild", 3192, 3368, 0, Map.of(), 2),
            new Dest("monastery", "Edgeville Monastery", 3052, 3490, 0, Map.of(), 2),
            new Dest("ranging_guild", "Ranging Guild", 2653, 3439, 0, Map.of(), 2),
            new Dest("fishing_guild", "Fishing Guild", 2611, 3390, 0, Map.of(), 2),
            new Dest("mining_guild", "Mining Guild", 3016, 3339, 0, Map.of(), 2), // approx (Falador entrance)
            new Dest("crafting_guild", "Crafting Guild", 2933, 3295, 0, Map.of(), 2),
            new Dest("cooking_guild", "Cooks' Guild", 3144, 3438, 0, Map.of(), 2),
            new Dest("woodcutting_guild", "Woodcutting Guild", 1662, 3505, 0, Map.of(), 2),
            new Dest("farming_guild", "Farming Guild", 1248, 3719, 0, Map.of(), 2),
            // ornate: amulet of glory + ring of wealth
            new Dest("edgeville", "Edgeville", 3087, 3496, 0, Map.of(), 3),
            new Dest("karamja", "Karamja", 2918, 3176, 0, Map.of(), 3),
            new Dest("draynor_village", "Draynor Village", 3105, 3251, 0, Map.of(), 3),
            new Dest("al_kharid", "Al Kharid", 3293, 3163, 0, Map.of(), 3),
            new Dest("miscellania", "Miscellania", 2534, 3862, 0, Map.of(), 3),
            new Dest("grand_exchange", "Grand Exchange", 3163, 3478, 0, Map.of(), 3),
            new Dest("falador_park", "Falador Park", 2995, 3375, 0, Map.of(), 3),
            new Dest("dondakans_rock", "Dondakan's Rock", 2826, 3504, 0, Map.of(), 3)); // approx (Keldagrim entrance)

    /** Spirit tree network (house spirit tree can travel to every other tree). */
    public static final List<Dest> SPIRIT_TREES = List.of(
            new Dest("tree_gnome_village", "Tree Gnome Village", 2542, 3170, 0, Map.of(), 1),
            new Dest("gnome_stronghold", "Gnome Stronghold", 2461, 3444, 0, Map.of(), 1),
            new Dest("battlefield_of_khazard", "Battlefield of Khazard", 2555, 3259, 0, Map.of(), 1),
            new Dest("grand_exchange", "Grand Exchange", 3185, 3508, 0, Map.of(), 1),
            new Dest("feldip_hills", "Feldip Hills", 2488, 2850, 0, Map.of(), 1),
            new Dest("prifddinas", "Prifddinas", 3274, 6123, 0, Map.of(), 1),
            new Dest("poison_waste", "Poison Waste", 2339, 3109, 0, Map.of(), 1),
            new Dest("port_sarim", "Port Sarim", 3059, 3257, 0, Map.of(), 1),
            new Dest("etceteria", "Etceteria", 2613, 3855, 0, Map.of(), 1),
            new Dest("brimhaven", "Brimhaven", 2800, 3203, 0, Map.of(), 1),
            new Dest("hosidius", "Hosidius", 1693, 3542, 0, Map.of(), 1),
            new Dest("farming_guild", "Farming Guild", 1251, 3750, 0, Map.of(), 1));

    /**
     * Lectern teleport tablets (soft clay + the spell's runes). Lectern tiers: oak 1, eagle 2, teak 3, mahogany 4. OSRS: Camelot
     * and Ardougne need a teak eagle lectern, Watchtower and Teleport to house a mahogany eagle lectern; Lumbridge/Falador approx.
     */
    public static final List<Tablet> TABLETS = List.of(
            new Tablet("varrock_teleport_tablet", "Varrock teleport", 25, 35, 1, runes("law_rune", 1, "air_rune", 3, "fire_rune", 1)),
            new Tablet("lumbridge_teleport_tablet", "Lumbridge teleport", 31, 41, 2, runes("law_rune", 1, "air_rune", 3, "earth_rune", 1)),
            new Tablet("falador_teleport_tablet", "Falador teleport", 37, 48, 2, runes("law_rune", 1, "air_rune", 3, "water_rune", 1)),
            new Tablet(HOUSE_TABLET, "Teleport to house", 40, 30, 4, runes("law_rune", 1, "air_rune", 1, "earth_rune", 1)),
            new Tablet("camelot_teleport_tablet", "Camelot teleport", 45, 55.5, 3, runes("law_rune", 1, "air_rune", 5)),
            new Tablet("ardougne_teleport_tablet", "Ardougne teleport", 51, 61, 3, runes("law_rune", 2, "water_rune", 2)),
            new Tablet("watchtower_teleport_tablet", "Watchtower teleport", 58, 68, 4, runes("law_rune", 2, "earth_rune", 2)));

    // ------------------------------------------------------------------ upgrades, nexus, servants

    /** Furniture that is built as an upgrade of what already stands in the hotspot (OSRS pools and jewellery boxes). */
    public static final Map<String, String> UPGRADE_OF = Map.of(
            "revitalisation_pool", "restoration_pool",
            "rejuvenation_pool", "revitalisation_pool",
            "fancy_rejuvenation_pool", "rejuvenation_pool",
            "ornate_rejuvenation_pool", "fancy_rejuvenation_pool",
            "fancy_jewellery_box", "basic_jewellery_box",
            "ornate_jewellery_box", "fancy_jewellery_box",
            "gilded_portal_nexus", "marble_portal_nexus",
            "crystalline_portal_nexus", "gilded_portal_nexus");

    /** The furniture {@code id} upgrades, or null if it is built on an empty hotspot. */
    public static String upgradeOf(String id) {
        return UPGRADE_OF.get(id);
    }

    /** Portal nexus: adding a teleport costs this many casts of its spell (OSRS: 1,000x the rune cost). */
    public static final int NEXUS_CASTS = 1000;

    /** Teleports a portal nexus can hold: every portal chamber destination plus a few Lunar/Arceuus/standard ones. */
    public static final List<Dest> NEXUS_DESTS;

    static {
        List<Dest> d = new ArrayList<>(PORTAL_DESTS);
        d.add(new Dest("ape_atoll", "Ape Atoll", 2796, 2791, 64, runes("law_rune", 2, "fire_rune", 2, "water_rune", 2), 1));
        d.add(new Dest("barrows", "Barrows", 3565, 3315, 83, runes("law_rune", 2, "soul_rune", 2, "blood_rune", 1), 1));
        d.add(new Dest("fishing_guild", "Fishing Guild", 2611, 3390, 85, runes("law_rune", 3, "astral_rune", 3, "water_rune", 10), 1));
        d.add(new Dest("catherby", "Catherby", 2802, 3449, 87, runes("law_rune", 3, "astral_rune", 3, "water_rune", 10), 1));
        NEXUS_DESTS = List.copyOf(d);
    }

    public static Dest nexusDest(String id) {
        for (Dest d : NEXUS_DESTS) {
            if (d.id().equals(id)) {
                return d;
            }
        }
        return null;
    }

    /** Teleports a nexus of the given tier can store (marble, gilded, crystalline). approx */
    public static int nexusCapacity(int tier) {
        return switch (tier) {
            case 1 -> 7;
            case 2 -> 15;
            case 3 -> NEXUS_DESTS.size();
            default -> 0;
        };
    }

    /** Destinations stored in a nexus ("id,id,..."), unknown ids dropped. */
    public static List<String> nexusList(String extra) {
        List<String> out = new ArrayList<>();
        if (extra == null || extra.isEmpty()) {
            return out;
        }
        for (String id : extra.split(",")) {
            if (nexusDest(id) != null && !out.contains(id)) {
                out.add(id);
            }
        }
        return out;
    }

    /** Why a teleport can't be added to a nexus of {@code tier} that already stores {@code stored}, or null. */
    public static String canAddToNexus(int tier, List<String> stored, String destId, int magicLevel) {
        Dest d = nexusDest(destId);
        if (d == null) {
            return "There's no such teleport.";
        }
        if (stored.contains(destId)) {
            return "Your portal nexus already has a teleport to " + d.name() + ".";
        }
        if (stored.size() >= nexusCapacity(tier)) {
            return "Your portal nexus can't hold any more teleports.";
        }
        if (magicLevel < d.magic()) {
            return "You need a Magic level of " + d.magic() + " to add a teleport to " + d.name() + ".";
        }
        return null;
    }

    /**
     * A servant: Construction level to hire, wage (paid every {@link #WAGE_TRIPS} errands), items carried per trip, ticks per
     * bank or sawmill trip, and whether they will go to the sawmill.
     */
    public record Servant(String id, String name, int level, int wage, int capacity, int tripTicks, boolean sawmill) {
    }

    public static final List<Servant> SERVANTS = List.of(
            new Servant("rick", "Rick", 20, 500, 6, 100, false),
            new Servant("maid", "Maid", 25, 1000, 10, 50, false),
            new Servant("cook", "Cook", 30, 3000, 16, 29, true),
            new Servant("butler", "Butler", 40, 5000, 20, 20, true),
            new Servant("demon_butler", "Demon butler", 50, 10000, 26, 12, true));

    /** Servants ask for their wage after this many errands. */
    public static final int WAGE_TRIPS = 8;

    /** Beds a servant needs in the house before moving in. */
    public static final int SERVANT_BEDS = 2;

    public static Servant servant(String id) {
        for (Servant s : SERVANTS) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }

    /** Why a servant can't be hired, or null. */
    public static String canHire(Servant s, int constructionLevel, int beds) {
        if (s == null) {
            return "There's no such servant.";
        }
        if (constructionLevel < s.level()) {
            return "You need a Construction level of " + s.level() + " to hire " + (s.id().equals("rick") ? "Rick"
                    : article(s.name().toLowerCase(java.util.Locale.ROOT))) + ".";
        }
        if (beds < SERVANT_BEDS) {
            return "Your servant needs two beds in your house to move in.";
        }
        return null;
    }

    /** A sawmill conversion: logs to planks and the fee per plank (OSRS sawmill prices). */
    public record Sawmill(String logs, String plank, int fee) {
    }

    public static final List<Sawmill> SAWMILL = List.of(
            new Sawmill("logs", "plank", 100),
            new Sawmill("oak_logs", "oak_plank", 250),
            new Sawmill("teak_logs", "teak_plank", 500),
            new Sawmill("mahogany_logs", "mahogany_plank", 1500));

    /** Building materials a servant can fetch from the bank. */
    public static final List<String> FETCHABLE = List.of("plank", "oak_plank", "teak_plank", "mahogany_plank", "iron_nails",
            "steel_bar", "bolt_of_cloth", "limestone_brick", "marble_block", "gold_leaf", "soft_clay");

    // ------------------------------------------------------------------ lookups

    private Poh() {
    }

    /** "a"/"an" + noun. */
    public static String article(String noun) {
        if (noun == null || noun.isEmpty()) {
            return "";
        }
        return ("aeiouAEIOU".indexOf(noun.charAt(0)) >= 0 ? "an " : "a ") + noun;
    }

    public static Room room(String id) {
        for (Room r : ROOMS) {
            if (r.id().equals(id)) {
                return r;
            }
        }
        return null;
    }

    public static Furniture furniture(String id) {
        return FURNITURE.get(id);
    }

    public static Furniture byBlock(String blockId) {
        if (!blockId.startsWith("ext_poh_")) {
            return null;
        }
        return FURNITURE.get(blockId.substring("ext_poh_".length()));
    }

    public static Dest portalDest(String id) {
        for (Dest d : PORTAL_DESTS) {
            if (d.id().equals(id)) {
                return d;
            }
        }
        return null;
    }

    /** Altar bone XP multiplier: altar percentage plus 50% per lit incense burner (at most two). */
    public static double altarMultiplier(Furniture altar, int litBurners) {
        if (altar == null || altar.effect() != Effect.ALTAR) {
            return 1.0;
        }
        return (altar.power() + Math.min(2, Math.max(0, litBurners)) * 50) / 100.0;
    }

    /** What drinking from a superior garden pool restores. */
    public record PoolEffects(boolean special, boolean run, boolean prayer, boolean stats, boolean hitpoints) {
    }

    /**
     * OSRS pool tiers: restoration (special attack), revitalisation (+ run energy), rejuvenation (+ prayer), fancy rejuvenation
     * (+ lowered stats) and ornate rejuvenation (+ hitpoints, curing poison and venom).
     */
    public static PoolEffects poolEffects(int tier) {
        return new PoolEffects(tier >= 1, tier >= 2, tier >= 3, tier >= 4, tier >= 5);
    }

    /** Tablets a lectern of the given tier can make. */
    public static List<Tablet> tablets(int lecternTier) {
        return TABLETS.stream().filter(t -> t.lecternTier() <= lecternTier).toList();
    }

    /** Jewellery box teleports available for a box tier. */
    public static List<Dest> jewelleryDests(int tier) {
        return JEWELLERY_DESTS.stream().filter(d -> d.tier() <= tier).toList();
    }

    /** OSRS room limit by Construction level (20 rooms up to level 49, rising to 30 at 99; approx between). */
    public static int maxRooms(int constructionLevel) {
        if (constructionLevel < 50) {
            return 20;
        }
        return Math.min(32, 21 + (constructionLevel - 50) / 5);
    }

    /** Items a larder of the given tier hands out (vanilla food stands in for tea, flour and so on). */
    public static List<String> larderItems(int tier) {
        List<String> items = new ArrayList<>(List.of("minecraft:egg", "minecraft:milk_bucket", "minecraft:bread"));
        if (tier >= 2) {
            items.addAll(List.of("minecraft:potato", "minecraft:carrot", "minecraft:beetroot"));
        }
        if (tier >= 3) {
            items.addAll(List.of("minecraft:cookie", "minecraft:pumpkin_pie", "minecraft:cooked_beef"));
        }
        return items;
    }
}
