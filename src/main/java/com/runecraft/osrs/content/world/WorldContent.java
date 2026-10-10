package com.runecraft.osrs.content.world;

import com.runecraft.osrs.content.ContentModule;
import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.List;

/**
 * Hand-built OSRS structures placed at their real map positions: castles, towns, walls, temples,
 * guilds and ruins across Misthalin, Asgarnia, Karamja, Kandarin, the Fremennik Province,
 * Tirannwn, Morytania, the Kharidian Desert and the Wilderness.
 */
public final class WorldContent implements ContentModule {
    @Override
    public String id() {
        return "world";
    }

    @Override
    public List<StructureDef> structures() {
        List<StructureDef> all = new ArrayList<>();
        all.addAll(MisthalinStructures.all());
        all.addAll(AsgarniaStructures.all());
        all.addAll(KandarinStructures.all());
        all.addAll(EasternStructures.all());
        return all;
    }
}
