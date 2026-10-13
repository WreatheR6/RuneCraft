package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.PlaceDef;

/**
 * Every minigame in the module with its real OSRS location (where the entrance is built), how the
 * lobby works and its entry requirements. The playable arena itself is an instance generated far
 * from the main map.
 */
public enum Minigame {
    FIGHT_CAVES("fight_caves", "TzHaar Fight Cave", "mg_tzhaar_city", 2438, 5168, 2832, 3170, PlaceDef.Style.CAVE, 0, 1, null, 0),
    INFERNO("inferno", "The Inferno", "mg_tzhaar_city", 2495, 5112, 2832, 3170, PlaceDef.Style.CAVE, 0, 1, null, 0),
    PEST_CONTROL("pest_control", "Pest Control", "mg_void_knights_outpost", 2657, 2639, 2657, 2639, PlaceDef.Style.CAMP, 25, 25, null, 40),
    WINTERTODT("wintertodt", "Wintertodt", "mg_wintertodt_camp", 1630, 3982, 1630, 3982, PlaceDef.Style.CAMP, 20, 10, Skill.FIREMAKING, 50),
    TEMPOROSS("tempoross", "Tempoross", "mg_ruins_of_unkah", 3135, 2840, 3135, 2840, PlaceDef.Style.RUINS, 20, 10, Skill.FISHING, 35),
    GUARDIANS_OF_THE_RIFT("guardians_of_the_rift", "Guardians of the Rift", "mg_temple_of_the_eye", 3615, 9471, 3115, 3152,
            PlaceDef.Style.RUINS, 20, 10, Skill.RUNECRAFT, 27),
    BARBARIAN_ASSAULT("barbarian_assault", "Barbarian Assault", "mg_barbarian_outpost", 2531, 3570, 2531, 3570, PlaceDef.Style.CAMP, 15, 5, null, 0),
    CASTLE_WARS("castle_wars", "Castle Wars", "mg_castle_wars_lobby", 2440, 3090, 2440, 3090, PlaceDef.Style.ARENA, 30, 20, null, 0),
    WARRIORS_GUILD("warriors_guild", "Warriors' Guild", "mg_warriors_guild", 2877, 3546, 2877, 3546, PlaceDef.Style.TOWN, 0, 1, null, 0),
    MAGE_TRAINING_ARENA("mage_training_arena", "Mage Training Arena", "mg_mage_training_arena", 3363, 3318, 3363, 3318,
            PlaceDef.Style.TOWN, 0, 1, Skill.MAGIC, 7),
    PYRAMID_PLUNDER("pyramid_plunder", "Pyramid Plunder", "mg_sophanem_pyramid", 3288, 2800, 3288, 2800, PlaceDef.Style.ARENA, 0, 1,
            Skill.THIEVING, 21),
    HALLOWED_SEPULCHRE("hallowed_sepulchre", "Hallowed Sepulchre", "mg_darkmeyer_sepulchre", 3653, 3384, 3653, 3384, PlaceDef.Style.RUINS,
            0, 1, Skill.AGILITY, 52),
    BRIMHAVEN_AGILITY_ARENA("brimhaven_agility_arena", "Brimhaven Agility Arena", "mg_brimhaven_agility_arena", 2809, 3193, 2809, 3193,
            PlaceDef.Style.ARENA, 0, 1, null, 0),
    GNOME_RESTAURANT("gnome_restaurant", "Gnome Restaurant", "mg_grand_tree_restaurant", 2448, 3504, 2448, 3504, PlaceDef.Style.TOWN, 0, 1,
            Skill.COOKING, 29),
    TROUBLE_BREWING("trouble_brewing", "Trouble Brewing", "mg_mos_le_harmless_brewery", 3810, 3022, 3810, 3022, PlaceDef.Style.RUINS, 20, 10,
            Skill.COOKING, 40),
    /** Mort'ton (pyres, temple and Razmire's shops by the entrance); the Shade Catacombs are at 3493, 9725 (bronze chests). */
    SHADES_OF_MORTTON("shades_of_mortton", "Shades of Mort'ton", "mg_mortton", 3493, 9725, 3500, 3290, PlaceDef.Style.RUINS, 0, 1, null, 0),
    TITHE_FARM("tithe_farm", "Tithe Farm", "mg_tithe_farm", 1802, 3501, 1802, 3501, PlaceDef.Style.CAMP, 0, 1, Skill.FARMING, 34),
    GIANTS_FOUNDRY("giants_foundry", "Giants' Foundry", "mg_giants_foundry", 3361, 3149, 3361, 3149, PlaceDef.Style.CAVE, 0, 1, Skill.SMITHING, 15),
    MAHOGANY_HOMES("mahogany_homes", "Mahogany Homes", "mg_mahogany_homes_office", 2989, 3366, 2989, 3366, PlaceDef.Style.TOWN, 0, 1,
            Skill.CONSTRUCTION, 1),
    FISHING_TRAWLER("fishing_trawler", "Fishing Trawler", "mg_port_khazard_trawler", 2676, 3170, 2676, 3170, PlaceDef.Style.CAMP, 20, 10,
            Skill.FISHING, 15),
    ROGUES_DEN("rogues_den", "Rogues' Den", "mg_rogues_den", 2905, 3537, 2913, 3530, PlaceDef.Style.CAVE, 0, 1, Skill.THIEVING, 50),
    /** The arena is in the Champions' Guild basement; the entrance stands just north of the guild. */
    CHAMPIONS_CHALLENGE("champions_challenge", "Champions' Challenge", "mg_champions_guild", 3191, 9758, 3191, 3369,
            PlaceDef.Style.ARENA, 0, 1, null, 0),
    /**
     * Below Fossil Island's volcano (instanced; real coordinates approx). The entrance stands just south of
     * the islands module's Volcanic Mine lobby building and builds nothing else (style NONE).
     */
    VOLCANIC_MINE("volcanic_mine", "Volcanic Mine", "mg_volcanic_mine", 3812, 10240, 3820, 3800, PlaceDef.Style.NONE, 20, 30,
            Skill.MINING, 50),
    /**
     * Dominic Onion's dreams, entered just north-west of Yanille bank (the arena is an instance of the
     * King Black Dragon's lair). Needs five of the boss quests ({@link MinigameRules.NightmareZone#QUESTS}).
     */
    NIGHTMARE_ZONE("nightmare_zone", "Nightmare Zone", "mg_nightmare_zone", 2276, 4680, 2609, 3114, PlaceDef.Style.NONE, 0, 1, null, 0),
    /** Soul Wars on the Isle of Souls (total level 500 and combat level 40; checked by the manager). */
    SOUL_WARS("soul_wars", "Soul Wars", "mg_isle_of_souls", 2136, 2900, 2210, 2880, PlaceDef.Style.CAMP, 20, 30, null, 0),
    /** Juna's Chasm of Tears below Lumbridge Swamp (after the Tears of Guthix quest); the portal stands beside Juna. */
    TEARS_OF_GUTHIX("tears_of_guthix", "Tears of Guthix", "mg_chasm_of_tears", 3257, 9517, 3255, 3178, PlaceDef.Style.NONE, 0, 1,
            null, 0),
    /**
     * The Sorceress's Garden (an instance at 2912, 5472), entered through the sorceress's apprentice in the house north-east
     * of the Shantay Pass (3320, 3139); needs Prince Ali Rescue (checked by the manager). Appended last: arena slots use the
     * ordinal.
     */
    SORCERESS_GARDEN("sorceress_garden", "Sorceress's Garden", "mg_sorceress_house", SorceressGarden.GARDEN_X,
            SorceressGarden.GARDEN_Y, SorceressGarden.HOUSE_X, SorceressGarden.HOUSE_Y, PlaceDef.Style.NONE, 0, 1, null, 0),
    /** The TzHaar Fight Pit in Mor Ul Rek (2398, 5151): a safe last-man-standing PvP arena, entered beside the Fight Cave. */
    FIGHT_PITS("fight_pits", "TzHaar Fight Pit", "mg_tzhaar_city", 2398, 5151, 2832, 3170, PlaceDef.Style.CAVE, 30, 50, null, 0),
    /**
     * TzHaar-Ket-Rak's Challenges (see {@link KetRakChallenges}): TzHaar-Ket-Rak sits at 2545, 5110 in Mor Ul Rek; the
     * challenge entrances stand with the other TzHaar entrances on the volcano. Appended last: arena slots use the ordinal.
     */
    TZHAAR_KET_RAK("tzhaar_ket_rak", "TzHaar-Ket-Rak's Challenges", "mg_tzhaar_city", KetRakChallenges.KET_RAK_X,
            KetRakChallenges.KET_RAK_Y, 2832, 3170, PlaceDef.Style.CAVE, 0, 1, null, 0),
    /**
     * Temple Trekking (see {@link TempleTrekking}): escorts from Burgh de Rott to Paterdomus (and the Burgh de Rott Ramble back,
     * entered outside Paterdomus). Needs In Aid of the Myreque (checked by the manager).
     */
    TEMPLE_TREKKING("temple_trekking", "Temple Trekking", TempleTrekking.PLACE, TempleTrekking.MAP_X, TempleTrekking.MAP_Y,
            TempleTrekking.BURGH_X, TempleTrekking.BURGH_Y, PlaceDef.Style.NONE, 0, 1, null, 0),
    /**
     * Last Man Standing (see {@link LastManStanding}): Lisa's lobby in the Ferox Enclave (3142, 3636; style NONE, the enclave is
     * built by the world module) and the Deserted Island instance. Lisa waits 60 seconds for more players. Appended last: arena
     * slots use the ordinal.
     */
    LAST_MAN_STANDING("last_man_standing", "Last Man Standing", LastManStanding.PLACE, LastManStanding.ISLAND_X,
            LastManStanding.ISLAND_Y, LastManStanding.LOBBY_X, LastManStanding.LOBBY_Y, PlaceDef.Style.NONE, 60,
            LastManStanding.MAX_PLAYERS, null, 0),
    /**
     * Emir's Arena (see {@link EmirsArena}): the Al Kharid PvP arena that replaced the Duel Arena. The entrance block by the gate
     * is the ranked 1v1 queue (two players); legacy duels start when a challenge is accepted. Style NONE: the arena buildings are
     * built by the world module. Appended last: arena slots use the ordinal.
     */
    EMIRS_ARENA("emirs_arena", "Emir's Arena", EmirsArena.PLACE, EmirsArena.ARENA_X, EmirsArena.ARENA_Y, EmirsArena.ENTRANCE_X,
            EmirsArena.ENTRANCE_Y, PlaceDef.Style.NONE, 30, 2, null, 0);

    private final String id;
    private final String displayName;
    private final String place;
    private final int realX;
    private final int realY;
    private final int entranceX;
    private final int entranceY;
    private final PlaceDef.Style style;
    private final int lobbySeconds;
    private final int maxPlayers;
    private final Skill skill;
    private final int level;

    Minigame(String id, String displayName, String place, int realX, int realY, int entranceX, int entranceY,
             PlaceDef.Style style, int lobbySeconds, int maxPlayers, Skill skill, int level) {
        this.id = id;
        this.displayName = displayName;
        this.place = place;
        this.realX = realX;
        this.realY = realY;
        this.entranceX = entranceX;
        this.entranceY = entranceY;
        this.style = style;
        this.lobbySeconds = lobbySeconds;
        this.maxPlayers = maxPlayers;
        this.skill = skill;
        this.level = level;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    /** PlaceDef id where the entrance is built. */
    public String place() {
        return place;
    }

    /** OSRS coordinates of the minigame itself (often underground or an instance). */
    public int realX() {
        return realX;
    }

    public int realY() {
        return realY;
    }

    /** OSRS coordinates of the surface entrance. */
    public int entranceX() {
        return entranceX;
    }

    public int entranceY() {
        return entranceY;
    }

    public PlaceDef.Style style() {
        return style;
    }

    /** Seconds the lobby waits for more players after the first joins; 0 = starts immediately (solo). */
    public int lobbySeconds() {
        return lobbySeconds;
    }

    public int maxPlayers() {
        return maxPlayers;
    }

    /** Skill requirement (null for none). For Pest Control the level is a combat level. */
    public Skill skill() {
        return skill;
    }

    public int level() {
        return level;
    }

    /** The scripted entrance block. */
    public String entranceBlock() {
        return id + "_entrance";
    }

    public static Minigame byId(String id) {
        for (Minigame m : values()) {
            if (m.id.equals(id)) {
                return m;
            }
        }
        return null;
    }
}
