package com.runecraft.osrs.content.godwars;

import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentModule;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.model.ModelSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * God Wars Dungeon generals, Nex and the Wilderness bosses: their followers and minions, uniques,
 * godsword / ward / voidwaker assembly, lairs and custom models.
 */
public final class GodWarsWildernessContent implements ContentModule {
    @Override
    public String id() {
        return "godwars";
    }

    @Override
    public void items(ItemSink items) {
        GwdItems.add(items);
    }

    @Override
    public void recipes(Consumer<Recipe> recipes) {
        GwdItems.recipes(recipes);
    }

    @Override
    public List<MonsterDef> monsters() {
        return GwdMonsters.all();
    }

    @Override
    public List<BossDef> bosses() {
        List<BossDef> list = new ArrayList<>(GwdBosses.all());
        list.addAll(WildernessBosses.all());
        list.add(WildernessBosses.maledictus());
        return list;
    }

    @Override
    public List<PlaceDef> places() {
        return GwdPlaces.all();
    }

    /** The Emblem Trader wandering the Revenant Caves (wiki: he buys ancient artefacts there). */
    @Override
    public List<com.runecraft.osrs.content.NpcDef> npcs() {
        return List.of(com.runecraft.osrs.content.NpcDef.of(RevenantCaves.EMBLEM_TRADER_NPC, "Emblem Trader",
                RevenantCaves.EMBLEM_TRADER_PLACE, 1, 1, 0xD8B08C, 0x2A2A2A, 0x1A1A1A, 0x3A2A4A));
    }

    /** The God Wars Dungeon's obstacles, doors and altars (scripted by the module hooks). */
    @Override
    public List<BlockDef> blocks() {
        return List.of(
                BlockDef.scripted(GwdDungeon.BLOCK_HOLE, "Hole (God Wars Dungeon)", 0x3A3A44, "pit"),
                BlockDef.scripted(GwdDungeon.BLOCK_ROPE, "Rope (God Wars Dungeon)", 0xA08A5A, "rope"),
                BlockDef.scripted(GwdDungeon.BLOCK_DOOR, "Big door", 0x6A6A72, "door"),
                BlockDef.scripted(GwdDungeon.BLOCK_PILLAR, "Pillar (God Wars Dungeon)", 0xC8C8D0, "pillar"),
                BlockDef.scripted(GwdDungeon.BLOCK_ROCK, "Rock (God Wars Dungeon)", 0x8A8A8A, "boulder"),
                BlockDef.scripted(GwdDungeon.BLOCK_ICE_BRIDGE, "Ice bridge", 0xB8E0F0, "ice"),
                BlockDef.scripted(GwdDungeon.BLOCK_FROZEN_DOOR, "Frozen Door", 0x9AD0F0, "door"),
                BlockDef.scripted(GwdDungeon.BLOCK_BARRIER, "Ancient Barrier", 0x7A2A9A, "portal_glow"),
                BlockDef.scripted("gwd_armadyl_altar", "Armadyl altar", 0xD8D0B0, "altar"),
                BlockDef.scripted("gwd_bandos_altar", "Bandos altar", 0x6A7A3A, "altar"),
                BlockDef.scripted("gwd_saradomin_altar", "Saradomin altar", 0x3A5AD0, "altar"),
                BlockDef.scripted("gwd_zamorak_altar", "Zamorak altar", 0xB02020, "altar"),
                BlockDef.scripted("gwd_ancient_altar", "Ancient altar", 0x5A2A6A, "altar"),
                // The Revenant Caves' ways in and out and their Agility pillars.
                BlockDef.scripted(RevenantCaves.BLOCK_CAVERN, "Cavern (Revenant Caves)", 0x1A1A22, "pit"),
                BlockDef.scripted(RevenantCaves.BLOCK_CREVICE, "Crevice (Revenant Caves)", 0x22222A, "pit"),
                BlockDef.scripted(RevenantCaves.BLOCK_STAIRS, "Stairs (Revenant Caves)", 0x5A5A66, "stairs"),
                BlockDef.scripted(RevenantCaves.BLOCK_PILLAR, "Pillar (Revenant Caves)", 0x6A6A78, "pillar"),
                BlockDef.scripted(RevenantCaves.BLOCK_TRAPDOOR, "Trapdoor (Revenant Caves)", 0x4A3A2A, "hatch"));
    }

    /** The God Wars Dungeon and the Revenant Caves, carved at their real tiles, and the caves' surface entrances. */
    @Override
    public List<com.runecraft.osrs.world.StructureDef> structures() {
        List<com.runecraft.osrs.world.StructureDef> list = new ArrayList<>();
        list.add(GwdDungeon.structure());
        list.add(RevenantCaves.structure());
        list.addAll(RevenantCaves.surface());
        return list;
    }

    @Override
    public List<ModelSpec> models() {
        List<ModelSpec> list = new ArrayList<>(GwdModels.all());
        list.addAll(WildernessModels.all());
        return list;
    }
}
