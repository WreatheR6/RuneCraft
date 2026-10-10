package com.runecraft.osrs.content.islands;

import com.runecraft.osrs.Station;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.PlaceDef.Style;

import java.util.List;

/**
 * The islands and outlying areas of Gielinor at their real OSRS world-map coordinates.
 *
 * <p>Many of these areas already have a place owned by another module (the quests module's Ape
 * Atoll, Fossil Island, Lunar Isle, Miscellania, Neitiznot, Jatizso, Mos Le'Harmless, Crandor,
 * Kharazi Jungle, Gu'Tanoth, Corsair Cove, Braindeath Island and Harmony Island; the bosses module's
 * Waterbirth Island, Zul-Andra and Ungael; the minigames module's Castle Wars and Void Knights'
 * Outpost; the skilling module's Ape Atoll market, Verdant Valley, Corsair Cove docks and Feldip
 * Hunter area). This module never duplicates those: its hand-built structures {@code replaces(...)}
 * them instead (see {@link IslandsStructures}), keeping their NPCs, stations and boss arenas. The
 * places below are only the areas nobody had added yet.
 *
 * <p>Coordinates marked "approx." are best estimates of the area's centre; caves and dungeons sit at
 * their surface entrance.
 */
public final class IslandsPlaces {
    private IslandsPlaces() {
    }

    // ---- Fossil Island ----
    public static final String HOUSE_ON_THE_HILL = "isl_house_on_the_hill";
    public static final String VOLCANIC_MINE = "isl_volcanic_mine";
    public static final String MUSHROOM_FOREST = "isl_mushroom_forest";
    public static final String MUSHROOM_MEADOW = "isl_mushroom_meadow";
    public static final String FOSSIL_NORTH_COAST = "isl_fossil_north_coast";
    // ---- Ape Atoll ----
    public static final String APE_ATOLL_AGILITY = "isl_ape_atoll_agility";
    // ---- Fremennik isles and the northern seas ----
    public static final String ETCETERIA = "isl_etceteria";
    public static final String LUNAR_ISLE_MINE = "isl_lunar_isle_mine";
    public static final String LUNAR_SUQAH_HILLS = "isl_lunar_suqah_hills";
    public static final String PIRATES_COVE = "isl_pirates_cove";
    public static final String LITHKREN = "isl_lithkren";
    // ---- Morytania seas ----
    public static final String MOS_LE_HARMLESS_JUNGLE = "isl_mos_le_harmless_jungle";
    public static final String DRAGONTOOTH_ISLAND = "isl_dragontooth_island";
    // ---- Misthalin / Asgarnia coast ----
    public static final String TUTORIAL_ISLAND = "isl_tutorial_island";
    public static final String MUDSKIPPER_POINT = "isl_mudskipper_point";
    // ---- Karamja ----
    public static final String CAIRN_ISLE = "isl_cairn_isle";
    // ---- Tirannwn ----
    public static final String PORT_TYRAS = "isl_port_tyras";
    public static final String ISAFDAR = "isl_isafdar_forest";
    // ---- Other realms ----
    public static final String ZANARIS = "isl_zanaris";

    // Places owned by other modules that this module builds over (replaces) or adds NPCs / camps to.
    public static final String FOSSIL_ISLAND = "fossil_island";
    public static final String VERDANT_VALLEY = "fossil_island_verdant_valley";
    public static final String APE_ATOLL = "ape_atoll";
    public static final String APE_ATOLL_MARKET = "ape_atoll_stalls";
    public static final String APE_ATOLL_TEMPLE = "ape_atoll_temple";
    public static final String MISCELLANIA = "miscellania";
    public static final String LUNAR_ISLE = "quest_lunar_isle";
    public static final String NEITIZNOT = "neitiznot";
    public static final String JATIZSO = "jatizso";
    public static final String WATERBIRTH = "waterbirth_island";
    public static final String UNGAEL = "ungael";
    public static final String MOS_LE_HARMLESS = "mos_le_harmless";
    public static final String HARMONY_ISLAND = "quest_harmony_island";
    public static final String CRANDOR = "crandor";
    public static final String KHARAZI_JUNGLE = "kharazi_jungle";
    public static final String GU_TANOTH = "gu_tanoth";
    public static final String CASTLE_WARS = "mg_castle_wars_lobby";
    public static final String CORSAIR_COVE = "corsair_cove";
    public static final String CORSAIR_COVE_DOCKS = "corsair_cove_docks";
    public static final String FELDIP_HUNTER_AREA = "feldip_hunter_area";
    public static final String ZUL_ANDRA = "zul_andra";
    public static final String BRAINDEATH_ISLAND = "braindeath_island";
    public static final String VOID_KNIGHTS_OUTPOST = "mg_void_knights_outpost";

    public static List<PlaceDef> all() {
        return List.of(
                // Fossil Island (Bone Voyage). The House on the Hill holds the Fossil Island museum displays.
                PlaceDef.of(HOUSE_ON_THE_HILL, "House on the Hill", 3764, 3869, Style.RUINS, List.of(IslandsBlocks.MUSHTREE)),
                // The Volcanic Mine lobby on the slopes of the volcano.
                PlaceDef.of(VOLCANIC_MINE, "Volcanic Mine", 3815, 3808, Style.CAVE, List.of()),
                // The Mushroom forest (sticky swamp, tar swamp and its Magic Mushtree), approx.
                PlaceDef.of(MUSHROOM_FOREST, "Mushroom forest", 3695, 3792, Style.CAMP, List.of(IslandsBlocks.MUSHTREE)),
                // Mushroom Meadow's Magic Mushtree, just north of the Wyvern Cave entrance (the cave itself is the
                // dungeons module's), approx.
                PlaceDef.of(MUSHROOM_MEADOW, "Mushroom Meadow", 3672, 3865, Style.CAMP, List.of(IslandsBlocks.MUSHTREE)),
                PlaceDef.of(FOSSIL_NORTH_COAST, "Fossil Island north coast", 3726, 3889, Style.NONE, List.of()),
                // Ape Atoll Agility Course (south of Marim).
                PlaceDef.of(APE_ATOLL_AGILITY, "Ape Atoll Agility Course", 2758, 2744, Style.CAMP, List.of(IslandsBlocks.FAIRY_RING)),
                // Etceteria: Queen Sigrid's castle and the village.
                PlaceDef.town(ETCETERIA, "Etceteria", 2614, 3878, Station.GENERAL_STORE, Station.RANGE),
                // Lunar Isle mine (pure essence and lunar ore) north-east of the Moonclan village, approx.
                PlaceDef.of(LUNAR_ISLE_MINE, "Lunar Isle mine", 2142, 3944, Style.CAVE, List.of("runecraft:coal_rock")),
                PlaceDef.of(LUNAR_SUQAH_HILLS, "Lunar Isle suqah hills", 2128, 3870, Style.NONE, List.of()),
                // Pirates' Cove: Lokar Searunner's ship and the pirate bank.
                PlaceDef.town(PIRATES_COVE, "Pirates' Cove", 2212, 3808, Station.BANK_BOOTH, Station.RANGE),
                // Lithkren: the dragonkin research facility ruins west and north of the Lithkren Vault's entrance
                // (the vault is the dungeons module's), approx.
                PlaceDef.of(LITHKREN, "Lithkren", 3540, 4004, Style.RUINS, List.of()),
                // The jungle of Mos Le'Harmless (the caves beneath it are the dungeons module's).
                PlaceDef.of(MOS_LE_HARMLESS_JUNGLE, "Mos Le'Harmless jungle", 3720, 2996, Style.NONE, List.of()),
                // Dragontooth Island (Ghosts Ahoy), east of Port Phasmatys.
                PlaceDef.of(DRAGONTOOTH_ISLAND, "Dragontooth Island", 3806, 3557, Style.RUINS, List.of()),
                // Tutorial Island: the Gielinor Guide's house is at 3094, 3107.
                PlaceDef.town(TUTORIAL_ISLAND, "Tutorial Island", 3094, 3107, Station.BANK_BOOTH, Station.RANGE, Station.FURNACE,
                        Station.ANVIL, Station.PRAYER_ALTAR),
                // Mudskipper Point, with fairy ring AIQ.
                PlaceDef.of(MUDSKIPPER_POINT, "Mudskipper Point", 2995, 3112, Style.CAMP, List.of(IslandsBlocks.FAIRY_RING)),
                // Cairn Isle off Karamja's west coast (Bervirius' tomb).
                PlaceDef.of(CAIRN_ISLE, "Cairn Isle", 2763, 2977, Style.RUINS, List.of()),
                // Port Tyras, King Tyras' harbour on the west coast of Tirannwn.
                PlaceDef.town(PORT_TYRAS, "Port Tyras", 2150, 3120, Station.GENERAL_STORE, Station.RANGE),
                // Isafdar's dense forest with its leaf traps and elven lookouts (approx.).
                PlaceDef.of(ISAFDAR, "Isafdar dense forest", 2240, 3176, Style.CAMP, List.of()),
                // Zanaris, the fairy realm: the fairy ring centre, bank, market and the Fairy Queen's grove.
                new PlaceDef(ZANARIS, "Zanaris", 2412, 4434, Style.TOWN, List.of(Station.BANK_BOOTH, Station.GENERAL_STORE),
                        List.of(IslandsBlocks.FAIRY_RING)));
    }

    /**
     * A creature camp: the module keeps up to {@code cap} of these creatures alive within
     * {@code radius} blocks of the place centre (+dx/dz blocks) while a player is nearby. When
     * {@code floorY} is not {@link #SURFACE} the creatures spawn on that absolute block height
     * (in the caves this module carves beneath their entrances).
     */
    public record Camp(String place, int dx, int dz, int radius, int cap, int floorY, List<String> creatures) {
        public boolean surface() {
            return floorY == SURFACE;
        }
    }

    public static final int SURFACE = Integer.MIN_VALUE;

    public static List<Camp> camps() {
        int cave = IslandsKit.CAVE_FLOOR_Y + 1;
        return List.of(
                new Camp(FOSSIL_NORTH_COAST, 0, 0, 12, 5, SURFACE, List.of("ammonite_crab")),
                // The long-tailed and ancient wyverns, in the dungeons module's Wyvern Cave (which keeps its own
                // spitting and taloned wyverns).
                new Camp(MUSHROOM_MEADOW, IslandsStructures.WYVERN_CAMP_DX, IslandsStructures.WYVERN_CAMP_DZ, 16, 4,
                        IslandsStructures.WYVERN_CAVE_FLOOR_Y, List.of("long_tailed_wyvern", "ancient_wyvern")),
                new Camp(APE_ATOLL, -54, -8, 8, 4, SURFACE, List.of("monkey_guard", "monkey_guard", "monkey_archer")),
                new Camp(APE_ATOLL_TEMPLE, -8, 0, 8, 3, SURFACE, List.of("monkey_zombie")),
                new Camp(LUNAR_SUQAH_HILLS, 0, 0, 14, 5, SURFACE, List.of("suqah")),
                new Camp(NEITIZNOT, 30, -40, 12, 6, SURFACE, List.of("yak")),
                new Camp(JATIZSO, -10, -64, 10, 4, SURFACE, List.of("ice_troll")),
                new Camp(WATERBIRTH, -40, -10, 14, 6, SURFACE, List.of("dagannoth", "wallasalki", "rock_lobster")),
                new Camp(MOS_LE_HARMLESS_JUNGLE, 0, 0, 14, 4, SURFACE, List.of("jungle_horror")),
                new Camp(DRAGONTOOTH_ISLAND, 0, 0, 12, 3, SURFACE, List.of("ghost")),
                new Camp(CRANDOR, -30, -60, 16, 5, SURFACE, List.of("lesser_demon", "moss_giant", "moss_giant")),
                new Camp(KHARAZI_JUNGLE, 0, 30, 14, 4, SURFACE, List.of("jogre")),
                new Camp(FELDIP_HUNTER_AREA, 0, -30, 14, 4, SURFACE, List.of("jogre", "ogre")),
                new Camp(CORSAIR_COVE, IslandsStructures.CORSAIR_DUNGEON_DX, IslandsStructures.CORSAIR_DUNGEON_DZ, 12, 5, cave,
                        List.of("ogress_warrior", "ogress_shaman")),
                new Camp(MUDSKIPPER_POINT, 0, 6, 10, 4, SURFACE, List.of("mudskipper")),
                new Camp(BRAINDEATH_ISLAND, 20, -6, 6, 4, SURFACE, List.of("fever_spider", "pirate")),
                new Camp(ZANARIS, 0, 40, 12, 3, SURFACE, List.of("mutated_zygomite")));
    }
}
