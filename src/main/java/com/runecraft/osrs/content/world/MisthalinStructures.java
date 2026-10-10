package com.runecraft.osrs.content.world;

import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.List;

/**
 * Misthalin: Lumbridge, Draynor, Varrock, Edgeville, Barbarian Village and the Wizards' Tower, at
 * their real OSRS tile positions. Big settlements come first so the small place-replacing
 * structures (farming patches, stalls, manholes) are applied on top of them in each chunk.
 */
final class MisthalinStructures {
    private MisthalinStructures() {
    }

    static List<StructureDef> all() {
        List<StructureDef> out = new ArrayList<>();
        MisthalinLumbridge.addAll(out);
        MisthalinDraynor.addAll(out);
        MisthalinVarrock.addAll(out);
        MisthalinEdgeville.addAll(out);
        return out;
    }
}
