package com.runecraft.osrs.content.godwars;

import com.runecraft.osrs.Recipe;
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

    @Override
    public List<ModelSpec> models() {
        List<ModelSpec> list = new ArrayList<>(GwdModels.all());
        list.addAll(WildernessModels.all());
        return list;
    }
}
