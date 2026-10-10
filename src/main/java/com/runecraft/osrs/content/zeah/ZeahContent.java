package com.runecraft.osrs.content.zeah;

import com.runecraft.osrs.CollectionLogData;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentModule;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.world.StructureDef;

import java.util.List;
import java.util.function.Consumer;

/**
 * Great Kourend, the Kebos Lowlands and Varlamore (the continent of Zeah): towns and landmarks at
 * their real coordinates with hand-built town centres, Kourend Castle and the Catacombs of
 * Kourend, Civitas illa Fortis and the Fortis Colosseum; lizardmen, sand crabs, catacomb monsters,
 * naguas and the Moons of Peril; house NPCs; Veos' ship, the Lovakengj minecarts and the quetzal
 * transport system; lovakite, the Blast Mine and the Varlamore Hunter creatures.
 */
public final class ZeahContent implements ContentModule {
    @Override
    public String id() {
        return "zeah";
    }

    @Override
    public void items(ItemSink items) {
        ZeahItems.add(items);
    }

    @Override
    public void recipes(Consumer<Recipe> recipes) {
        ZeahItems.recipes(recipes);
    }

    @Override
    public List<MonsterDef> monsters() {
        return ZeahMonsters.all();
    }

    @Override
    public List<BossDef> bosses() {
        return ZeahBosses.all();
    }

    @Override
    public List<PlaceDef> places() {
        return ZeahPlaces.all();
    }

    @Override
    public List<NpcDef> npcs() {
        return ZeahNpcs.all();
    }

    @Override
    public List<BlockDef> blocks() {
        return ZeahBlocks.all();
    }

    @Override
    public List<ModelSpec> models() {
        return ZeahModels.all();
    }

    @Override
    public List<CollectionLogData.Page> collectionLog() {
        return ZeahItems.collectionLog();
    }

    @Override
    public List<StructureDef> structures() {
        return ZeahStructures.all();
    }
}
