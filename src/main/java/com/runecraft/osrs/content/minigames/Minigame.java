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
    SHADES_OF_MORTTON("shades_of_mortton", "Shades of Mort'ton", "mg_mortton", 3500, 3290, 3500, 3290, PlaceDef.Style.RUINS, 0, 1, null, 0),
    TITHE_FARM("tithe_farm", "Tithe Farm", "mg_tithe_farm", 1802, 3501, 1802, 3501, PlaceDef.Style.CAMP, 0, 1, Skill.FARMING, 34),
    GIANTS_FOUNDRY("giants_foundry", "Giants' Foundry", "mg_giants_foundry", 3361, 3149, 3361, 3149, PlaceDef.Style.CAVE, 0, 1, Skill.SMITHING, 15),
    MAHOGANY_HOMES("mahogany_homes", "Mahogany Homes", "mg_mahogany_homes_office", 2989, 3366, 2989, 3366, PlaceDef.Style.TOWN, 0, 1,
            Skill.CONSTRUCTION, 1),
    FISHING_TRAWLER("fishing_trawler", "Fishing Trawler", "mg_port_khazard_trawler", 2676, 3170, 2676, 3170, PlaceDef.Style.CAMP, 20, 10,
            Skill.FISHING, 15),
    ROGUES_DEN("rogues_den", "Rogues' Den", "mg_rogues_den", 2905, 3537, 2913, 3530, PlaceDef.Style.CAVE, 0, 1, Skill.THIEVING, 50);

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
