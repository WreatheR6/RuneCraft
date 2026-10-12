package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.PlaceDef.Style;

import java.util.List;

/**
 * Boss lairs at their real OSRS world-map coordinates. Underground and instanced lairs use the
 * coordinates of their surface entrance (the dungeon itself lives off the surface map in OSRS).
 */
public final class BossPlaces {
    private BossPlaces() {
    }

    public static final String FONT_OF_CONSUMPTION = "font_of_consumption";

    public static List<PlaceDef> all() {
        return List.of(
                PlaceDef.of("falador_mole_hills", "Falador Park mole hills", 2996, 3376, Style.CAMP, List.of()),
                PlaceDef.of("kalphite_lair", "Kalphite Lair", 3226, 3108, Style.CAVE, List.of()),
                PlaceDef.of("waterbirth_island", "Waterbirth Island", 2546, 3754, Style.CAVE, List.of()),
                PlaceDef.of("forthos_dungeon", "Forthos Dungeon", 1702, 3574, Style.RUINS, List.of()),
                PlaceDef.of("zul_andra", "Zul-Andra", 2200, 3056, Style.CAMP, List.of()),
                PlaceDef.of("ungael", "Ungael", 2272, 4047, Style.RUINS, List.of()),
                PlaceDef.of("kraken_cove", "Kraken Cove", 2278, 3611, Style.CAVE, List.of()),
                PlaceDef.of("smoke_devil_dungeon", "Smoke Devil Dungeon", 2412, 3060, Style.CAVE, List.of()),
                PlaceDef.of("cerberus_lair", "Cerberus' Lair", 2884, 3397, Style.CAVE, List.of()),
                // The Abyss is entered through the Mage of Zamorak's rift; we use his Varrock spot to stay out of the Wilderness.
                PlaceDef.of("abyssal_nexus", "Abyssal Nexus", 3258, 3383, Style.RUINS, List.of(FONT_OF_CONSUMPTION)),
                PlaceDef.of("karuulm_slayer_dungeon", "Karuulm Slayer Dungeon", 1311, 3807, Style.CAVE, List.of()),
                PlaceDef.of("slayer_tower_roof", "Slayer Tower rooftop", 3428, 3538, Style.RUINS, List.of()),
                PlaceDef.of("corporeal_beast_lair", "Corporeal Beast's lair", 3206, 3681, Style.CAVE, List.of()),
                PlaceDef.of("catacombs_of_kourend", "Catacombs of Kourend", 1636, 3673, Style.RUINS, List.of()),
                PlaceDef.of("farming_guild_hespori", "Farming Guild (Hespori)", 1248, 3726, Style.CAMP, List.of()),
                PlaceDef.of("the_gauntlet", "The Gauntlet", 3230, 6116, Style.ARENA, List.of()),
                PlaceDef.of("zalcano_chamber", "Zalcano's prison", 3034, 6068, Style.CAVE, List.of()),
                PlaceDef.of("sisterhood_sanctuary", "Sisterhood Sanctuary", 3728, 3302, Style.RUINS, List.of()),
                PlaceDef.of("ghorrock_dungeon", "Ghorrock Dungeon", 2911, 3950, Style.CAVE, List.of()),
                PlaceDef.of("ghorrock_prison", "Ghorrock Prison", 2938, 3965, Style.RUINS, List.of()),
                // The Scar is reached from the Ruins of Ullek; the Lassar Undercity through the Ancient Vault by the Digsite.
                PlaceDef.of("the_scar", "The Scar", 3418, 2790, Style.RUINS, List.of()),
                PlaceDef.of("lassar_undercity", "Lassar Undercity", 3360, 3432, Style.RUINS, List.of()),
                PlaceDef.of("stranglewood", "The Stranglewood", 1160, 3430, Style.RUINS, List.of()),
                PlaceDef.of("varrock_sewers", "Varrock Sewers (Scurrius)", 3237, 3458, Style.CAVE, List.of()),
                PlaceDef.of("araxyte_cave", "Morytania Spider Cave", 3657, 3405, Style.CAVE, List.of()),
                PlaceDef.of("hueycoatl_lair", "The Hueycoatl's lair", 1510, 3285, Style.RUINS, List.of()),
                PlaceDef.of("neypotzli", "Neypotzli", 1360, 3190, Style.CAVE, List.of()),
                // Approximate: the Royal Titans' cave beneath the Asgarnian Ice Dungeon.
                PlaceDef.of("royal_titans_lair", "Royal Titans' lair", 3008, 3150, Style.CAVE, List.of()),
                PlaceDef.of("watsons_house", "Watson's house", 1645, 3574, Style.TOWN, List.of()),
                PlaceDef.of("fortis_colosseum", "Fortis Colosseum", 1800, 3105, Style.ARENA, List.of()),
                // 2025-2026 bosses (ModernBosses). Yama's lair lies beneath the Chasm of Fire; the arena stands clear of the
                // chasm's own entrance (1433, 3671).
                PlaceDef.of("yamas_lair", "Yama's Lair", 1404, 3712, Style.RUINS, List.of()), // approx
                // Mokhaiotl's lair is entered from the Tlati jungle north of Tal Teklan.
                PlaceDef.of("mokhaiotl_lair", "Mokhaiotl's lair", 1262, 3130, Style.CAVE, List.of()), // approx
                // The Gemstone Crab roams between three Tlati Rainforest mines; this is the western one.
                PlaceDef.of("gemstone_crab_mine", "Tlati Rainforest mine (Gemstone Crab)", 1288, 3060, Style.CAMP, List.of()), // approx
                // The Shellbane Gryphon Cave sits at the centre of the Great Conch.
                PlaceDef.of("shellbane_gryphon_cave", "Shellbane Gryphon Cave", 3190, 2450, Style.CAVE, List.of()), // approx
                // Brutus' pen in the Lumbridge cow field east of the River Lum.
                PlaceDef.of("brutus_pen", "Brutus' pen", 3262, 3285, Style.CAMP, List.of()), // approx
                // The Maggot King's hive in the Vampyrium, reached through Darkmeyer.
                PlaceDef.of("maggot_kings_hive", "Maggot King's hive", 3660, 3310, Style.CAVE, List.of())); // approx
    }

    public static List<BlockDef> blocks() {
        return List.of(BlockDef.scripted(FONT_OF_CONSUMPTION, "Font of Consumption", 0x6A1A3A, "altar"));
    }
}
