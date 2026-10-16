package com.runecraft.osrs.world;

import java.util.Map;

/**
 * Module NPCs ({@code NpcDef}) that stand on an upper storey of the building at their spot, as in OSRS: the
 * storey counts floors above the ground floor (an open roof platform counts as the storey above the last
 * ceiling). NPCs not listed stand on the ground floor.
 */
public final class NpcStoreys {
    private static final Map<String, Integer> STOREYS = Map.of(
            "watchtower_wizard", 2); // the Watchtower's top platform (OSRS 2549, 3116, plane 2)

    private NpcStoreys() {
    }

    /** The storey this NPC stands on (0 = ground floor). */
    public static int of(String npcId) {
        return STOREYS.getOrDefault(npcId, 0);
    }
}
