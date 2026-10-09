package com.runecraft.osrs.content;

import com.runecraft.osrs.CollectionLogData;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.model.ModelSpec;

import java.util.List;
import java.util.function.Consumer;

/**
 * A bundle of OSRS content (pure data). Modules are listed in {@link Modules}; everything they
 * return is merged into the item catalog, recipes, monsters, bosses, quests, places, NPCs, blocks,
 * models and collection log. Minecraft-side behaviour for a module lives in
 * {@code com.runecraft.content.<module>} and is hooked up via {@code com.runecraft.content.ModuleHooks}.
 */
public interface ContentModule {
    String id();

    default void items(ItemSink items) {
    }

    default void recipes(Consumer<Recipe> recipes) {
    }

    default List<MonsterDef> monsters() {
        return List.of();
    }

    default List<BossDef> bosses() {
        return List.of();
    }

    default List<QuestDef> quests() {
        return List.of();
    }

    default List<PlaceDef> places() {
        return List.of();
    }

    default List<NpcDef> npcs() {
        return List.of();
    }

    default List<BlockDef> blocks() {
        return List.of();
    }

    default List<ModelSpec> models() {
        return List.of();
    }

    default List<CollectionLogData.Page> collectionLog() {
        return List.of();
    }
}
