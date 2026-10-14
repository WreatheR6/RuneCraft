package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.CollectionLogData;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentModule;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.model.ModelSpec;

import java.util.List;

/**
 * Minigames: TzHaar Fight Cave and Inferno, Pest Control, Wintertodt, Tempoross, Guardians of the
 * Rift, Barbarian Assault, Castle Wars, Warriors' Guild, Mage Training Arena, Pyramid Plunder,
 * Hallowed Sepulchre, Brimhaven Agility Arena, Gnome Restaurant, Trouble Brewing, Shades of
 * Mort'ton, Tithe Farm, Giants' Foundry, Mahogany Homes, Fishing Trawler, Rogues' Den, TzHaar-Ket-Rak's Challenges,
 * Temple Trekking and Last Man Standing.
 */
public final class MinigamesContent implements ContentModule {
    @Override
    public String id() {
        return "minigames";
    }

    @Override
    public void items(ItemSink items) {
        MinigameItems.add(items);
        LastManStanding.items(items);
        EmirsArena.items(items);
        TaiBwoWannai.items(items);
        VarrockMuseum.items(items);
    }

    @Override
    public List<MonsterDef> monsters() {
        return MinigameMonsters.monsters();
    }

    @Override
    public List<BossDef> bosses() {
        List<BossDef> list = new java.util.ArrayList<>(MinigameMonsters.bosses());
        list.addAll(ChampionsChallenge.bosses());
        return list;
    }

    @Override
    public List<PlaceDef> places() {
        return MinigameWorld.places();
    }

    @Override
    public List<NpcDef> npcs() {
        return MinigameWorld.npcs();
    }

    @Override
    public List<BlockDef> blocks() {
        return MinigameWorld.blocks();
    }

    @Override
    public List<ModelSpec> models() {
        return MinigameModels.models();
    }

    @Override
    public List<CollectionLogData.Page> collectionLog() {
        return MinigameWorld.collectionLog();
    }
}
