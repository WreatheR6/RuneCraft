package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import java.util.Set;

/**
 * Quest NPCs moved to another place after worlds were already generated with them. The new place spawns its own copy when
 * it is built, so the entity a saved world kept at the old place is a stale duplicate: {@code QuestNpcEntity} removes a
 * listed NPC that loads more than {@link #TOLERANCE} blocks from where its NpcDef puts it. Add an id here whenever an
 * existing NPC's place or offset changes.
 */
public final class QuestNpcMoves {
    private QuestNpcMoves() {
    }

    /** Blocks an NPC may stand from its spot (NpcFloor looks for a free floor nearby) before it counts as stale. */
    public static final int TOLERANCE = 32;

    /** Round 33: start NPCs moved to their cache tiles (QuestStartNpcsTest), and Lady Vulcana to the Lovakengj Assembly. */
    public static final Set<String> RELOCATED = Set.of("dimintheis", "edmond", "king_lathas", "lokar_searunner", "mord_gunnars",
            "vestri", "gossip_kr", "ali_morrisane", "wanderer_sophanem", "akrisae", "ivy_sophista", "lord_kandur_lovakengj", "lazim");

    /** Block x/z where the NPC is spawned (its place's centre plus its offset), or null if its place is unknown. */
    public static int[] homeBlock(NpcDef def) {
        PlaceDef p = ContentRegistry.place(def.place());
        if (p != null) {
            return new int[]{p.blockX() + def.dx(), p.blockZ() + def.dz()};
        }
        Landmark l = Landmark.byName(def.place());
        return l == null ? null : new int[]{l.blockX() + def.dx(), l.blockZ() + def.dz()};
    }

    /** Whether an entity of this NPC standing at block x/z is a copy left behind at the NPC's old place. */
    public static boolean stale(NpcDef def, int blockX, int blockZ) {
        if (def == null || !RELOCATED.contains(def.id())) {
            return false;
        }
        int[] home = homeBlock(def);
        return home != null && Math.max(Math.abs(blockX - home[0]), Math.abs(blockZ - home[1])) > TOLERANCE;
    }
}
