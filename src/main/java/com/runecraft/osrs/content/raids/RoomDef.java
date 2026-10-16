package com.runecraft.osrs.content.raids;

import java.util.List;

/**
 * One room of a raid. The Minecraft side picks the room's behaviour by {@link #id} (combat rooms
 * spawn {@link #bosses} and complete when every one of them is dead; puzzle and skilling rooms
 * have bespoke logic).
 *
 * @param style            arena style used for the room's floor and walls (stone, ice, crypt, jungle,
 *                         void, water, sand, lava)
 * @param completionPoints points every surviving party member receives when the room is cleared
 *                         (CoX/ToA personal points; ToB uses its own per-room points)
 * @param hint             shown to the party when they enter the room
 */
public record RoomDef(String id, String name, RaidType raid, Kind kind, String style, List<String> bosses,
                      int completionPoints, String hint) {

    public enum Kind {
        /** Party gathers, supplies and (ToA) invocations; the start barrier leads on. */
        LOBBY,
        /** Kill every listed boss to proceed. */
        COMBAT,
        /** Puzzle or skilling room with scripted completion. */
        PUZZLE,
        /** Final boss room. */
        FINAL,
        /** Reward chest and exit. */
        REWARD,
        /**
         * CoX resource room (scavengers, farming): its exit opens at once and its stations stay usable for the rest of
         * the raid.
         */
        RESOURCE
    }

    public boolean combat() {
        return kind == Kind.COMBAT || kind == Kind.FINAL;
    }
}
