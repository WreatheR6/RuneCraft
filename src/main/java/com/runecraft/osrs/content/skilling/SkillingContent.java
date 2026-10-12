package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ContentModule;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Extra skilling content: Cooking, Crafting, Smithing, Fletching, Herblore, Farming, Hunter, Mining,
 * Runecraft, Construction, Thieving, Woodcutting/Firemaking/Fishing extras and Sailing. Each skill's
 * data lives in its own {@link SkillingPart}; Minecraft behaviour is in
 * {@code com.runecraft.content.skilling.SkillingHooks}.
 */
public final class SkillingContent implements ContentModule {
    public static final List<SkillingPart> PARTS = List.of(new CookingData(), new CraftingData(), new SmithingData(),
            new FletchingData(), new HerbloreData(), new BrewingData(), new FarmingData(), new HunterData(), new MiningData(), new RunecraftData(),
            new ConstructionData(), new ThievingData(), new GatheringData(), new SailingData(), new AgilityData(),
            new PrayerData(), new BlastFurnaceData());

    @Override
    public String id() {
        return "skilling";
    }

    @Override
    public void items(ItemSink items) {
        PARTS.forEach(p -> p.items(items));
    }

    @Override
    public void recipes(Consumer<Recipe> recipes) {
        PARTS.forEach(p -> p.recipes(recipes));
    }

    @Override
    public List<BlockDef> blocks() {
        List<BlockDef> all = new ArrayList<>();
        PARTS.forEach(p -> all.addAll(p.blocks()));
        return all;
    }

    @Override
    public List<PlaceDef> places() {
        List<PlaceDef> all = new ArrayList<>();
        PARTS.forEach(p -> all.addAll(p.places()));
        return all;
    }

    @Override
    public List<NpcDef> npcs() {
        List<NpcDef> all = new ArrayList<>();
        PARTS.forEach(p -> all.addAll(p.npcs()));
        return all;
    }

    @Override
    public List<MonsterDef> monsters() {
        List<MonsterDef> all = new ArrayList<>();
        PARTS.forEach(p -> all.addAll(p.monsters()));
        return all;
    }
}
