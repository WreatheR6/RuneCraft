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

    private NpcStoreys() {
    }

    /** The storey this NPC stands on (0 = ground floor). */
    public static int of(String npcId) {
        return STOREYS.getOrDefault(npcId, 0);
    }
}
