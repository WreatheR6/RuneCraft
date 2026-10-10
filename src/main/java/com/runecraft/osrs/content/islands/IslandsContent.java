package com.runecraft.osrs.content.islands;

import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ContentModule;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.world.StructureDef;

import java.util.List;

/**
 * The islands and outlying areas of Gielinor that had no hand-built structures yet: Fossil Island,
 * Ape Atoll, Miscellania and Etceteria, Lunar Isle, Neitiznot and Jatizso, Waterbirth Island, Pirates'
 * Cove, Ungael, Lithkren, Mos Le'Harmless, Harmony Island, Dragontooth Island, Tutorial Island,
 * Mudskipper Point, Crandor, Kharazi Jungle, Cairn Isle, Feldip Hills (Gu'Tanoth, Castle Wars, Corsair
 * Cove, the Hunter area), Zul-Andra, Port Tyras, Isafdar, Braindeath Island, the Void Knights' Outpost
 * and Zanaris - at their real tile coordinates, with their residents and dialogue, their monsters,
 * the fairy ring network, Fossil Island's mushtrees, the Ape Atoll Agility Course and the management
 * of the Kingdom of Miscellania.
 */
public final class IslandsContent implements ContentModule {
    @Override
    public String id() {
        return "islands";
    }

    @Override
    public void items(ItemSink items) {
        IslandsItems.add(items);
    }

    @Override
    public List<MonsterDef> monsters() {
        return IslandsMonsters.all();
    }

    @Override
    public List<PlaceDef> places() {
        return IslandsPlaces.all();
    }

    @Override
    public List<NpcDef> npcs() {
        return IslandsNpcs.all();
    }

    @Override
    public List<BlockDef> blocks() {
        return IslandsBlocks.all();
    }

    @Override
    public List<StructureDef> structures() {
        return IslandsStructures.all();
    }
}
