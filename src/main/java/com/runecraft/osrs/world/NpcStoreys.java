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
     * at the end of the Ape Atoll Dungeon, Bardur on Waterbirth sublevel 2.
     */
    private static final Map<String, Integer> UNDERGROUND_FEET = Map.of(
            "zooknock", 7, "bunkwicket", 7, "waymottin", 7, "bardur", 7);

    private NpcStoreys() {
    }

    /** The feet block y of an NPC that stands in a dungeon below its place, or null for one on the surface. */
    public static Integer undergroundFeetY(String npcId) {
        return UNDERGROUND_FEET.get(npcId);
    }

    /** The storey this NPC stands on (0 = ground floor). */
    public static int of(String npcId) {
        return STOREYS.getOrDefault(npcId, 0);
    }
}
