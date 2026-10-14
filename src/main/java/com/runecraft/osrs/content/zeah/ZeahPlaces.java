package com.runecraft.osrs.content.zeah;

import com.runecraft.osrs.Station;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.PlaceDef.Style;

import java.util.List;

/**
 * Great Kourend, the Kebos Lowlands and Varlamore at their real OSRS world-map coordinates.
 *
 * <p>Several Kourend locations already belong to other modules and are reused rather than
 * duplicated: the Catacombs of Kourend entrance, Forthos Dungeon and the Mimic's house (bosses),
 * Mount Karuulm (slayer), the Farming Guild, the Hosidius vinery and the Lovakengj ore stall
 * (skilling), the Wintertodt camp and Tithe Farm (minigames), the Dark Altar (systems), Lizardman
 * Canyon, the Kebos Lowlands and the house leaders' squares (quests), the Fortis Colosseum,
 * Neypotzli and the Hueycoatl's lair (bosses), and the Avium Savannah / tecu salamander hunting
 * grounds (skilling).
 *
 * <p>Coordinates marked "approx." are best estimates of the area's centre; underground places
 * (Cam Torum, the Hunter Guild caves, the Moons of Peril) sit at their surface entrance.
 */
public final class ZeahPlaces {
    private ZeahPlaces() {
    }

    // ---- Great Kourend ----
    public static final String KOUREND_CASTLE = "zeah_kourend_castle";
    public static final String KINGSTOWN = "zeah_kingstown";
    public static final String ARCEUUS = "zeah_arceuus";
    public static final String ARCEUUS_LIBRARY = "zeah_arceuus_library";
    public static final String BLOOD_ALTAR = "zeah_blood_altar";
    public static final String SOUL_ALTAR = "zeah_soul_altar";
    public static final String HOSIDIUS = "zeah_hosidius";
    public static final String PORT_PISCARILIUS = "zeah_port_piscarilius";
    public static final String SHAYZIEN = "zeah_shayzien";
    public static final String SHAYZIEN_ENCAMPMENT = "zeah_shayzien_encampment";
    public static final String LOVAKENGJ = "zeah_lovakengj";
    public static final String LOVAKITE_MINE = "zeah_lovakite_mine";
    public static final String BLAST_MINE = "zeah_blast_mine";
    public static final String WOODCUTTING_GUILD = "zeah_woodcutting_guild";
    public static final String LANDS_END = "zeah_lands_end";
    public static final String KOUREND_WOODLAND = "zeah_kourend_woodland";
    public static final String SAND_CRAB_BEACH = "zeah_sand_crab_beach";
    public static final String CRABCLAW_ISLE = "zeah_crabclaw_isle";
    // ---- Kebos Lowlands ----
    public static final String LIZARDMAN_SETTLEMENT = "zeah_lizardman_settlement";
    public static final String MOLCH = "zeah_molch";
    public static final String LAKE_MOLCH = "zeah_lake_molch";
    public static final String MOUNT_QUIDAMORTEM = "zeah_mount_quidamortem";
    public static final String MINECART_FARMING_GUILD = "zeah_minecart_farming_guild";
    public static final String MINECART_ARCEUUS = "zeah_minecart_arceuus";
    public static final String MINECART_HOSIDIUS_SOUTH = "zeah_minecart_hosidius_south";
    public static final String MINECART_HOSIDIUS_WEST = "zeah_minecart_hosidius_west";
    public static final String MINECART_KINGSTOWN = "zeah_minecart_kingstown";
    public static final String MINECART_KOUREND_WOODLAND = "zeah_minecart_kourend_woodland";
    public static final String MINECART_LOVAKENGJ = "zeah_minecart_lovakengj";
    public static final String MINECART_PORT_PISCARILIUS = "zeah_minecart_port_piscarilius";
    public static final String MINECART_SHAYZIEN_EAST = "zeah_minecart_shayzien_east";
    public static final String MINECART_SHAYZIEN_WEST = "zeah_minecart_shayzien_west";
    public static final String TEOMAT = "zeah_teomat";
    public static final String COLOSSAL_WYRM_REMAINS = "zeah_colossal_wyrm_remains";

    /**
     * Bare places (style NONE) at the OSRS tiles of Lovakengj minecart stations away from their town plazas;
     * the Minecraft side lays the minecart at each.
     */
    public static final List<String> MINECART_STATIONS = List.of(MINECART_ARCEUUS, MINECART_HOSIDIUS_SOUTH, MINECART_HOSIDIUS_WEST,
            MINECART_KINGSTOWN, MINECART_KOUREND_WOODLAND, MINECART_LOVAKENGJ, MINECART_PORT_PISCARILIUS, MINECART_SHAYZIEN_EAST,
            MINECART_SHAYZIEN_WEST);
    public static final String NORTHERN_TUNDRAS = "zeah_northern_tundras";
    // ---- Varlamore ----
    public static final String CIVITAS = "zeah_civitas_illa_fortis";
    public static final String OUTER_FORTIS = "zeah_outer_fortis";
    public static final String SUNSET_COAST = "zeah_sunset_coast";
    public static final String HUNTER_GUILD = "zeah_hunter_guild";
    public static final String HUNTER_GUILD_CAVES = "zeah_hunter_guild_caves";
    public static final String CAM_TORUM = "zeah_cam_torum";
    public static final String RALOS_RISE = "zeah_ralos_rise";
    public static final String MOONS_OF_PERIL = "zeah_moons_of_peril";
    public static final String QUETZACALLI_GORGE = "zeah_quetzacalli_gorge";
    public static final String ALDARIN = "zeah_aldarin";
    public static final String MISTROCK = "zeah_mistrock";
    public static final String TAL_TEKLAN = "zeah_tal_teklan";
    public static final String KASTORI = "zeah_kastori";
    public static final String AUBURNVALE = "zeah_auburnvale";
    public static final String SALVAGER_OVERLOOK = "zeah_salvager_overlook";
    public static final String SUNLIGHT_ANTELOPE_GROUNDS = "zeah_sunlight_antelope_grounds";
    public static final String SUNLIGHT_MOTH_GLADE = "zeah_sunlight_moth_glade";
    public static final String MOONLIGHT_MOTH_GROTTO = "zeah_moonlight_moth_grotto";
    public static final String LOCUS_OASIS = "zeah_locus_oasis";
    /** Entrances of unbuilt Varlamore dungeons, where their slayer creatures wait (stand-ins, like the frost nagua). */
    public static final String RUINS_OF_TAPOYAUIK = "zeah_ruins_of_tapoyauik";
    public static final String TONALI_CAVERN = "zeah_tonali_cavern";
    public static final String STALKER_DEN = "zeah_stalker_den";

    // Places owned by other modules that this module adds NPCs, camps or travel stops to.
    public static final String CATACOMBS = "catacombs_of_kourend";
    public static final String FORTIS_COLOSSEUM = "fortis_colosseum";
    public static final String FARMING_GUILD = "farming_guild";
    public static final String LIZARDMAN_CANYON = "lizardman_canyon";
    public static final String DARK_ALTAR = "arceuus_dark_altar";

    public static List<PlaceDef> all() {
        return List.of(
                // Kourend Castle sits in the middle of Kingstown; the Catacombs entrance (bosses module) is in
                // its courtyard. This place is the keep at the north of the courtyard.
                PlaceDef.town(KOUREND_CASTLE, "Kourend Castle", 1639, 3698, Station.PRAYER_ALTAR, Station.RANGE),
                // Kingstown and the Kourend Kitchen (bank and the range with the 5% bonus) south-east of the castle.
                PlaceDef.town(KINGSTOWN, "Kingstown", 1688, 3626, Station.BANK_BOOTH, Station.RANGE, Station.GENERAL_STORE),
                new PlaceDef(ARCEUUS, "Arceuus", 1636, 3752, Style.TOWN,
                        List.of(Station.BANK_BOOTH, Station.RUNE_SHOP, Station.GENERAL_STORE),
                        List.of(ZeahBlocks.MINECART)),
                PlaceDef.town(ARCEUUS_LIBRARY, "Arceuus Library", 1632, 3812),
                PlaceDef.of(BLOOD_ALTAR, "Blood Altar", 1716, 3827, Style.RUINS, List.of("runecraft:blood_altar")),
                PlaceDef.of(SOUL_ALTAR, "Soul Altar", 1815, 3856, Style.RUINS, List.of("runecraft:soul_altar")),
                new PlaceDef(HOSIDIUS, "Hosidius", 1738, 3594, Style.TOWN,
                        List.of(Station.BANK_BOOTH, Station.GENERAL_STORE, Station.RANGE, Station.SPINNING_WHEEL,
                                Station.ALLOTMENT_PATCH, Station.HERB_PATCH),
                        List.of(ZeahBlocks.MINECART, "vegetable_stall")),
                new PlaceDef(PORT_PISCARILIUS, "Port Piscarilius", 1806, 3790, Style.TOWN,
                        List.of(Station.BANK_BOOTH, Station.GENERAL_STORE, Station.TOOL_SHOP, Station.RANGE),
                        List.of(ZeahBlocks.MINECART, "fish_stall")),
                new PlaceDef(SHAYZIEN, "Shayzien", 1522, 3624, Style.TOWN,
                        List.of(Station.BANK_BOOTH, Station.WEAPON_SHOP, Station.GENERAL_STORE, Station.PRAYER_ALTAR),
                        List.of(ZeahBlocks.MINECART)),
                PlaceDef.of(SHAYZIEN_ENCAMPMENT, "Shayzien Encampment", 1458, 3644, Style.CAMP, List.of()), // approx.
                new PlaceDef(LOVAKENGJ, "Lovakengj", 1532, 3756, Style.TOWN,
                        List.of(Station.BANK_BOOTH, Station.FURNACE, Station.ANVIL, Station.TOOL_SHOP),
                        List.of(ZeahBlocks.MINECART)),
                PlaceDef.of(LOVAKITE_MINE, "Lovakite mine", 1434, 3830, Style.CAMP,
                        List.of(ZeahBlocks.LOVAKITE_ROCK, ZeahBlocks.LOVAKITE_ROCK, ZeahBlocks.LOVAKITE_ROCK, "runecraft:coal_rock")),
                PlaceDef.of(BLAST_MINE, "Blast mine", 1490, 3864, Style.CAVE,
                        List.of(ZeahBlocks.HARD_ROCK, ZeahBlocks.HARD_ROCK, ZeahBlocks.HARD_ROCK, ZeahBlocks.HARD_ROCK)),
                PlaceDef.town(WOODCUTTING_GUILD, "Woodcutting Guild", 1620, 3496, Station.BANK_BOOTH, Station.TOOL_SHOP),
                PlaceDef.town(LANDS_END, "Land's End", 1508, 3421, Station.BANK_BOOTH, Station.RANGE),
                PlaceDef.of(KOUREND_WOODLAND, "Kourend Woodland", 1572, 3555, Style.CAMP, List.of(ZeahBlocks.MINECART)), // approx.
                PlaceDef.of(SAND_CRAB_BEACH, "Hosidius sand crab beach", 1754, 3470, Style.NONE, List.of()),
                PlaceDef.of(CRABCLAW_ISLE, "Crabclaw Isle", 1778, 3416, Style.CAMP, List.of()), // approx.
                // Kebos Lowlands.
                PlaceDef.of(LIZARDMAN_SETTLEMENT, "Lizardman Settlement", 1300, 3600, Style.CAMP, List.of()),
                PlaceDef.of(MOLCH, "Molch", 1366, 3634, Style.CAMP, List.of("runecraft:lure_bait_fishing_spot", "runecraft:lure_bait_fishing_spot")),
                PlaceDef.of(LAKE_MOLCH, "Lake Molch", 1350, 3612, Style.NONE, List.of()),
                PlaceDef.of(MOUNT_QUIDAMORTEM, "Mount Quidamortem", 1255, 3549, Style.CAMP, List.of(ZeahBlocks.MINECART)),
                PlaceDef.of(MINECART_FARMING_GUILD, "Farming Guild minecart", 1218, 3737, Style.CAMP, List.of(ZeahBlocks.MINECART)),
                PlaceDef.of(NORTHERN_TUNDRAS, "Northern Tundras", 1648, 3931, Style.CAMP, List.of(ZeahBlocks.MINECART)),
                // Lovakengj minecart stations (OSRS tiles).
                PlaceDef.of(MINECART_ARCEUUS, "Arceuus minecart station", 1670, 3833, Style.NONE, List.of(ZeahBlocks.MINECART)),
                PlaceDef.of(MINECART_HOSIDIUS_SOUTH, "Hosidius South minecart station", 1808, 3479, Style.NONE, List.of(ZeahBlocks.MINECART)),
                PlaceDef.of(MINECART_HOSIDIUS_WEST, "Hosidius West minecart station", 1655, 3543, Style.NONE, List.of(ZeahBlocks.MINECART)),
                PlaceDef.of(MINECART_KINGSTOWN, "Kingstown minecart station", 1699, 3660, Style.NONE, List.of(ZeahBlocks.MINECART)),
                PlaceDef.of(MINECART_KOUREND_WOODLAND, "Kourend Woodland minecart station", 1572, 3466, Style.NONE,
                        List.of(ZeahBlocks.MINECART)),
                PlaceDef.of(MINECART_LOVAKENGJ, "Lovakengj minecart station", 1518, 3733, Style.NONE, List.of(ZeahBlocks.MINECART)),
                PlaceDef.of(MINECART_PORT_PISCARILIUS, "Port Piscarilius minecart station", 1761, 3710, Style.NONE,
                        List.of(ZeahBlocks.MINECART)),
                PlaceDef.of(MINECART_SHAYZIEN_EAST, "Shayzien East minecart station", 1590, 3620, Style.NONE, List.of(ZeahBlocks.MINECART)),
                PlaceDef.of(MINECART_SHAYZIEN_WEST, "Shayzien West minecart station", 1415, 3577, Style.NONE, List.of(ZeahBlocks.MINECART)),
                // Quetzal landing sites without a town of their own.
                PlaceDef.of(TEOMAT, "The Teomat", 1437, 3171, Style.NONE, List.of()),
                PlaceDef.of(COLOSSAL_WYRM_REMAINS, "Colossal Wyrm Remains", 1670, 2933, Style.NONE, List.of()),
                // Varlamore.
                PlaceDef.town(CIVITAS, "Civitas illa Fortis", 1680, 3130, Station.BANK_BOOTH, Station.GENERAL_STORE, Station.RANGE,
                        Station.FURNACE, Station.ANVIL, Station.WEAPON_SHOP, Station.RUNE_SHOP, Station.ARCHERY_SHOP,
                        Station.HERBLORE_TABLE, Station.SPINNING_WHEEL),
                PlaceDef.of(OUTER_FORTIS, "Outer Fortis", 1700, 3036, Style.CAMP, List.of()), // approx.
                PlaceDef.town(SUNSET_COAST, "Sunset Coast", 1510, 2985, Station.BANK_BOOTH, Station.GENERAL_STORE, Station.RANGE),
                PlaceDef.town(HUNTER_GUILD, "Hunter Guild", 1558, 3046, Station.BANK_BOOTH, Station.TOOL_SHOP, Station.TANNER),
                PlaceDef.of(HUNTER_GUILD_CAVES, "Hunter Guild caves", 1532, 3076, Style.CAVE,
                        List.of(ZeahBlocks.MOONLIGHT_ANTELOPE_PIT, ZeahBlocks.MOONLIGHT_ANTELOPE_PIT)), // approx. (entrance)
                new PlaceDef(CAM_TORUM, "Cam Torum", 1436, 3129, Style.CAVE,
                        List.of(Station.BANK_BOOTH, Station.FURNACE, Station.ANVIL, Station.GENERAL_STORE), List.of()), // entrance
                new PlaceDef(RALOS_RISE, "Ralos' Rise", 1466, 3172, Style.RUINS, List.of(Station.PRAYER_ALTAR), List.of()), // approx.
                PlaceDef.of(MOONS_OF_PERIL, "Moons of Peril (Neypotzli)", 1420, 3212, Style.CAVE, List.of()), // approx. (entrance)
                PlaceDef.of(QUETZACALLI_GORGE, "Quetzacalli Gorge", 1510, 3222, Style.CAMP, List.of()),
                PlaceDef.town(ALDARIN, "Aldarin", 1390, 2938, Station.BANK_BOOTH, Station.GENERAL_STORE, Station.RANGE), // approx.
                PlaceDef.of(MISTROCK, "Mistrock", 1382, 2866, Style.CAMP, List.of("runecraft:coal_rock", "runecraft:mithril_rock")), // approx.
                PlaceDef.town(TAL_TEKLAN, "Tal Teklan", 1240, 3076, Station.BANK_BOOTH, Station.GENERAL_STORE), // approx.
                PlaceDef.town(KASTORI, "Kastori", 1344, 3030, Station.RANGE, Station.GENERAL_STORE), // approx.
                PlaceDef.town(AUBURNVALE, "Auburnvale", 1390, 3356, Station.BANK_BOOTH, Station.GENERAL_STORE), // approx.
                PlaceDef.of(SALVAGER_OVERLOOK, "Salvager Overlook", 1614, 3300, Style.CAMP, List.of()), // approx.
                PlaceDef.of(SUNLIGHT_ANTELOPE_GROUNDS, "Avium Savannah antelope pits", 1596, 2950, Style.CAMP,
                        List.of(ZeahBlocks.SUNLIGHT_ANTELOPE_PIT, ZeahBlocks.SUNLIGHT_ANTELOPE_PIT, ZeahBlocks.SUNLIGHT_ANTELOPE_PIT)),
                PlaceDef.of(SUNLIGHT_MOTH_GLADE, "Sunlight moth glade", 1556, 3212, Style.CAMP,
                        List.of(ZeahBlocks.SUNLIGHT_MOTH_BUSH, ZeahBlocks.SUNLIGHT_MOTH_BUSH)), // approx.
                PlaceDef.of(MOONLIGHT_MOTH_GROTTO, "Moonlight moth grotto", 1394, 3166, Style.CAVE,
                        List.of(ZeahBlocks.MOONLIGHT_MOTH_BUSH, ZeahBlocks.MOONLIGHT_MOTH_BUSH)), // approx.
                PlaceDef.of(LOCUS_OASIS, "Locus Oasis", 1690, 2996, Style.CAMP,
                        List.of(ZeahBlocks.JERBOA_BURROW, ZeahBlocks.JERBOA_BURROW, ZeahBlocks.JERBOA_BURROW)), // approx.
                PlaceDef.of(RUINS_OF_TAPOYAUIK, "Ruins of Tapoyauik", 1693, 3232, Style.NONE, List.of()), // entrance
                PlaceDef.of(TONALI_CAVERN, "Tonali Cavern", 1309, 3104, Style.NONE, List.of()), // Crypt of Tonali north entrance
                PlaceDef.of(STALKER_DEN, "Stalker Den", 1310, 3369, Style.NONE, List.of())); // between its two entrances
    }

    /**
     * A creature camp: the module keeps up to {@code cap} of these creatures alive within
     * {@code radius} blocks of the place centre (+dx/dz blocks) while a player is nearby. When
     * {@code floorY} is not {@link #SURFACE} the creatures spawn on that absolute block height
     * (e.g. in the Catacombs of Kourend carved beneath the castle).
     */
    public record Camp(String place, int dx, int dz, int radius, int cap, int floorY, List<String> creatures) {
        public boolean surface() {
            return floorY == SURFACE;
        }
    }

    public static final int SURFACE = Integer.MIN_VALUE;

    public static List<Camp> camps() {
        return List.of(
                new Camp(SAND_CRAB_BEACH, 0, 0, 14, 7, SURFACE, List.of("sand_crab")),
                new Camp(CRABCLAW_ISLE, 0, 0, 10, 4, SURFACE, List.of("sand_crab", "sand_crab", "king_sand_crab")),
                // No shamans on the settlement's surface in the cache; theirs are in the Lizardman Caves below (dungeons).
                new Camp(LIZARDMAN_SETTLEMENT, 0, 0, 16, 7, SURFACE, List.of("lizardman", "lizardman", "lizardman_brute",
                        "lizardman_brute")),
                // Cache spawns: lizardmen and brutes fill the canyon's east end (35 and 21 at 1480-1530, 3680-3720); its
                // three shamans stand at the west end (1424-1434, 3700-3711).
                new Camp(LIZARDMAN_CANYON, 0, 0, 14, 5, SURFACE, List.of("lizardman", "lizardman", "lizardman_brute")),
                new Camp(LIZARDMAN_CANYON, -172, -10, 8, 3, SURFACE, List.of("lizardman_shaman")),
                new Camp(KOUREND_CASTLE, 0, 10, 40, 12, ZeahStructures.CATACOMBS_FLOOR_Y + 1, ZeahMonsters.CATACOMB_CREATURES),
                new Camp(CAM_TORUM, 0, 20, 12, 4, SURFACE, List.of("sulphur_nagua")),
                new Camp(RALOS_RISE, 0, 0, 12, 3, SURFACE, List.of("sulphur_nagua")),
                new Camp(MOONS_OF_PERIL, 0, -28, 12, 4, SURFACE, List.of("frost_nagua")),
                // Round 17 slayer creatures of unbuilt Varlamore dungeons, at their entrances (approx.): Neypotzli's wyrmlings
                // and grimy lizards, the Ruins of Tapoyauik's frost crabs and chilled jellies, the Tonali Cavern's earthen
                // nagua and the Stalker Den's custodian stalkers.
                new Camp(MOONS_OF_PERIL, 26, -20, 10, 3, SURFACE, List.of("wyrmling")),
                new Camp(MOONS_OF_PERIL, 10, 24, 10, 4, SURFACE, List.of("grimy_lizard")),
                new Camp(RUINS_OF_TAPOYAUIK, 0, 0, 12, 6, SURFACE, List.of("frost_crab")),
                new Camp(RUINS_OF_TAPOYAUIK, 0, -26, 10, 3, SURFACE, List.of("chilled_jelly")),
                new Camp(TONALI_CAVERN, 0, -10, 12, 4, SURFACE, List.of("earthen_nagua")),
                new Camp(STALKER_DEN, -20, 0, 10, 4, SURFACE, List.of("juvenile_custodian_stalker")),
                new Camp(STALKER_DEN, 6, 10, 10, 3, SURFACE, List.of("mature_custodian_stalker")),
                new Camp(STALKER_DEN, 26, 16, 10, 2, SURFACE, List.of("elder_custodian_stalker")),
                new Camp(CIVITAS, 0, 10, 40, 4, SURFACE, List.of("fortis_guard")),
                new Camp(OUTER_FORTIS, 0, 0, 12, 2, SURFACE, List.of("fortis_guard")));
    }
}
