package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentModule;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.QuestDef;
import com.runecraft.osrs.model.ModelSpec;

import java.util.List;

/**
 * Additional OSRS quests, their NPCs, places, quest items, quest bosses and boss models. All the
 * data is declared in {@link QuestBook} (one class per area); the Minecraft-side interpreter in
 * {@code com.runecraft.content.quests} runs the {@link QuestSpec} state machines.
 */
public final class QuestsContent implements ContentModule {
    @Override
    public String id() {
        return "quests";
    }

    @Override
    public void items(ItemSink items) {
        QuestBook.get().items.forEach(adder -> adder.accept(items));
    }

    @Override
    public List<MonsterDef> monsters() {
        return List.copyOf(QuestBook.get().monsters);
    }

    @Override
    public List<BossDef> bosses() {
        return List.copyOf(QuestBook.get().bosses);
    }

    @Override
    public List<QuestDef> quests() {
        return QuestBook.get().questDefs();
    }

    @Override
    public List<PlaceDef> places() {
        return List.copyOf(QuestBook.get().places);
    }

    @Override
    public List<NpcDef> npcs() {
        return List.copyOf(QuestBook.get().npcs);
    }

    @Override
    public List<BlockDef> blocks() {
        return List.copyOf(QuestBook.get().blocks);
    }

    @Override
    public List<ModelSpec> models() {
        return List.copyOf(QuestBook.get().models);
    }
}
