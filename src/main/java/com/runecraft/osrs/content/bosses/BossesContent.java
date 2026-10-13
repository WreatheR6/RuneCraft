package com.runecraft.osrs.content.bosses;

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

/** Other bosses: Zulrah, Vorkath, Cerberus, Hydra, Corp, DKs, KQ, Mole, Nightmare, DT2 and more. */
public final class BossesContent implements ContentModule {
    @Override
    public String id() {
        return "bosses";
    }

    @Override
    public void items(ItemSink items) {
        BossItems.add(items);
    }

    @Override
    public void recipes(Consumer<Recipe> recipes) {
        BossItems.recipes(recipes);
    }

    @Override
    public List<MonsterDef> monsters() {
        List<MonsterDef> all = new ArrayList<>(BossMinions.all());
        all.addAll(ModernBosses.minions());
        return all;
    }

    @Override
    public List<BossDef> bosses() {
        List<BossDef> all = new ArrayList<>();
        all.addAll(ClassicBosses.all());
        all.addAll(ZulrahVorkath.all());
        all.addAll(SlayerBosses.all());
        all.addAll(MinigameBosses.all());
        all.addAll(Dt2Bosses.all());
        all.addAll(VarlamoreBosses.all());
        all.addAll(ModernBosses.all());
        all.addAll(IslandBosses.all());
        for (BossDef b : all) {
            if (b.model == null) {
                b.model(b.id);
            }
        }
        return all;
    }

    @Override
    public List<PlaceDef> places() {
        return BossPlaces.all();
    }

    @Override
    public List<BlockDef> blocks() {
        return BossPlaces.blocks();
    }

    @Override
    public List<ModelSpec> models() {
        return BossModels.all();
    }
}
