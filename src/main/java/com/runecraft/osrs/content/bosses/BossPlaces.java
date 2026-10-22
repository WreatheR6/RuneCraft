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
    /** The Ancient Vault's chest: a Desert Treasure II quartz opens it (OSRS Wiki "Chest (Ancient Vault)"). */
    public static final String ANCIENT_VAULT_CHEST = "ancient_vault_chest";
    /** The growth in the Scar: pluck it for abyssal potatoes (after Desert Treasure II). */
    public static final String SCAR_GROWTH = "scar_growth";
    /** The Deranged archaeologist's clearing at the south end of Fossil Island's Tar Swamp. */
    public static final String DERANGED_SWAMP = "deranged_archaeologist_swamp";

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
                // Duke Sucellus' asylum: its mushrooms, salt piles and fermentation vats for the poison that wakes him.
                PlaceDef.of("ghorrock_prison", "Ghorrock Prison", 2938, 3965, Style.RUINS, List.of(DukeRules.ARDER_MUSHROOMS,
                        DukeRules.MUSCA_MUSHROOMS, DukeRules.SALT_PILE, DukeRules.VAT)),
                // The Scar is reached from the Ruins of Ullek; the Lassar Undercity through the Ancient Vault by the Digsite.
                PlaceDef.of("the_scar", "The Scar", 3418, 2790, Style.RUINS, List.of(SCAR_GROWTH)),
                PlaceDef.of("lassar_undercity", "Lassar Undercity", 3360, 3432, Style.RUINS, List.of(WhispererRules.WORKBENCH)),
                PlaceDef.of("stranglewood", "The Stranglewood", 1160, 3430, Style.RUINS, List.of()),
                // The Ancient Vault (an instance in OSRS): beside the Vault of Desert Treasure II, with its quartz chest. approx
                PlaceDef.of("ancient_vault", "Ancient Vault", 3180, 2850, Style.RUINS, List.of(ANCIENT_VAULT_CHEST)),
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
                PlaceDef.of("mokhaiotl_lair", "Mokhaiotl's lair", 1282, 3128, Style.CAVE, List.of()), // approx: the crater at the head of the stream
                // The Gemstone Crab moves between three Tlati Rainforest spawns (wiki: 1275,3160, 1351,3110 and 1237,3042); this is
                // the south-western one.
                PlaceDef.of("gemstone_crab_mine", "Tlati Rainforest mine (Gemstone Crab)", 1237, 3042, Style.CAMP, List.of()),
                // The Shellbane Gryphon Cave entrance in the centre of the Great Conch, north of fairy ring CJQ (wiki map pin).
                PlaceDef.of("shellbane_gryphon_cave", "Shellbane Gryphon Cave", 3176, 2477, Style.CAVE, List.of()),
                // Brutus' spawn in the Lumbridge cow field east of the River Lum (wiki location tile).
                PlaceDef.of("brutus_pen", "Brutus' pen", 3263, 3297, Style.CAMP, List.of()),
                // The Deranged archaeologist spawns at cache tile 3683, 3706 (mejrs NPCList_OSRS.json), the south end of the Tar
                // Swamp. Arenas are built 14 + radius blocks east of their place (ArenaLayout), so the place sits 11.5 tiles
                // west of that tile; with the snap to chunk centres his arena is centred within 5 tiles of it.
                PlaceDef.of(DERANGED_SWAMP, "Deranged archaeologist's clearing", 3672, 3706, Style.NONE, List.of()),
                // The Maggot King's hive in the Vampyrium, reached through Darkmeyer.
                PlaceDef.of("maggot_kings_hive", "Maggot King's hive", 3660, 3310, Style.CAVE, List.of())); // approx
    }

    public static List<BlockDef> blocks() {
        return List.of(BlockDef.scripted(FONT_OF_CONSUMPTION, "Font of Consumption", 0x6A1A3A, "altar"),
                BlockDef.scripted(SCAR_GROWTH, "Growth", 0x4A2A4A, "plant"),
                BlockDef.scripted(ANCIENT_VAULT_CHEST, "Chest", 0x6A4A6A, "chest"),
                BlockDef.scripted(WhispererRules.WORKBENCH, "Ketla's workbench", 0x5A4A6A, "table"),
                BlockDef.scripted(DukeRules.ARDER_MUSHROOMS, "Arder mushrooms", 0x8A6A3A, "mushroom"),
                BlockDef.scripted(DukeRules.MUSCA_MUSHROOMS, "Musca mushrooms", 0x6A8A4A, "mushroom"),
                BlockDef.scripted(DukeRules.SALT_PILE, "Salt pile", 0xE0E0E8, "rock"),
                BlockDef.scripted(DukeRules.VAT, "Fermentation vat", 0x6A5A4A, "vat"),
                BlockDef.scripted(ClassicBosses.SCURRIUS_FOOD_PILE, "Food Pile", 0x8A6A3A, "crate"));
    }
}
