package com.runecraft.osrs.world;

import java.util.Map;

/**
 * Module NPCs ({@code NpcDef}) that stand on an upper storey of the building at their spot, as in OSRS: the
 * storey counts floors above the ground floor (an open roof platform counts as the storey above the last
 * ceiling). NPCs not listed stand on the ground floor.
 */
public final class NpcStoreys {
    private static final Map<String, Integer> STOREYS = Map.of(
            "watchtower_wizard", 2, // the Watchtower's top platform (OSRS 2549, 3116, plane 2)
            "sir_lancelot", 1, // Camelot keep, by the Round Table (OSRS 2759, 3515, plane 1)
            "merlin", 2); // top of Camelot's south-east tower, where his crystal stood (approx)

    /**
     * Module NPCs that stand in a carved dungeon below their place: the block y of their feet (the place's dx/dz still
     * give the column). The dungeons module's tests check each spot is carved air: Zooknock, Bunkwicket and Waymottin
     * at the end of the Ape Atoll Dungeon, Bardur on Waterbirth sublevel 2, Vannaka in the Edgeville Dungeon.
     */
    private static final Map<String, Integer> UNDERGROUND_FEET = Map.of(
            "zooknock", 7, "bunkwicket", 7, "waymottin", 7, "bardur", 7,
            "quest_viggora_3", 27, // Curse of the Empty Lord, the Edgeville Dungeon's earth warrior room
            "quest_ashuelot_reis", 21, // before the God Wars Dungeon's frozen door (godwars GwdDungeon, level 2 floor 20)
            "rev_emblem_trader", 5, // the Revenant Caves (godwars RevenantCaves, floor 4)
            "vannaka", 27, // the Edgeville Dungeon, east of the Wilderness gate (slayer module, OSRS 3147, 9914)
            "movario", 23); // While Guthix Sleeps: by the Tears of Guthix pool, Lumbridge Swamp Caves (floor 22)

    private NpcStoreys() {
    }

    /** The feet block y of an NPC that stands in a dungeon below its place, or null for one on the surface. */
    public static Integer undergroundFeetY(String npcId) {
        Integer y = UNDERGROUND_FEET.get(npcId);
        return y != null ? y : com.runecraft.osrs.content.minigames.ColosseumLobby.feetY(npcId); // the Fortis Colosseum's lobby
    }

    /** The storey this NPC stands on (0 = ground floor). */
    public static int of(String npcId) {
        return STOREYS.getOrDefault(npcId, 0);
    }
}
