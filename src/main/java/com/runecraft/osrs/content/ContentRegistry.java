package com.runecraft.osrs.content;

import com.runecraft.osrs.Quest;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.model.ModelSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Aggregated, de-duplicated view of all module content (built lazily, once). */
public final class ContentRegistry {
    private static List<MonsterDef> monsters;
    private static List<BossDef> bosses;
    private static List<QuestInfo> quests;
    private static List<PlaceDef> places;
    private static List<NpcDef> npcs;
    private static List<BlockDef> blocks;
    private static Map<String, ModelSpec> models;

    private ContentRegistry() {
    }

    private static <T> List<T> unique(List<T> list, java.util.function.Function<T, String> id, String what) {
        Map<String, T> map = new LinkedHashMap<>();
        for (T t : list) {
            if (map.put(id.apply(t), t) != null) {
                throw new IllegalStateException("Duplicate " + what + " id " + id.apply(t));
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(map.values()));
    }

    public static synchronized List<MonsterDef> monsters() {
        if (monsters == null) {
            List<MonsterDef> all = new ArrayList<>();
            Modules.ALL.forEach(m -> all.addAll(m.monsters()));
            monsters = unique(all, d -> d.id, "monster");
        }
        return monsters;
    }

    public static synchronized List<BossDef> bosses() {
        if (bosses == null) {
            List<BossDef> all = new ArrayList<>();
            Modules.ALL.forEach(m -> all.addAll(m.bosses()));
            bosses = unique(all, d -> d.id, "boss");
        }
        return bosses;
    }

    /** Built-in quests followed by module quests. */
    public static synchronized List<QuestInfo> quests() {
        if (quests == null) {
            List<QuestInfo> all = new ArrayList<>(List.of(Quest.values()));
            Modules.ALL.forEach(m -> all.addAll(m.quests()));
            quests = unique(all, QuestInfo::questId, "quest");
        }
        return quests;
    }

    public static QuestInfo quest(String id) {
        for (QuestInfo q : quests()) {
            if (q.questId().equals(id)) {
                return q;
            }
        }
        return null;
    }

    public static synchronized List<PlaceDef> places() {
        if (places == null) {
            List<PlaceDef> all = new ArrayList<>();
            Modules.ALL.forEach(m -> all.addAll(m.places()));
            places = unique(all, PlaceDef::id, "place");
        }
        return places;
    }

    public static PlaceDef place(String id) {
        for (PlaceDef p : places()) {
            if (p.id().equals(id)) {
                return p;
            }
        }
        return null;
    }

    public static synchronized List<NpcDef> npcs() {
        if (npcs == null) {
            List<NpcDef> all = new ArrayList<>();
            Modules.ALL.forEach(m -> all.addAll(m.npcs()));
            npcs = unique(all, NpcDef::id, "npc");
        }
        return npcs;
    }

    public static NpcDef npc(String id) {
        for (NpcDef n : npcs()) {
            if (n.id().equals(id)) {
                return n;
            }
        }
        return null;
    }

    public static synchronized List<BlockDef> blocks() {
        if (blocks == null) {
            List<BlockDef> all = new ArrayList<>();
            Modules.ALL.forEach(m -> all.addAll(m.blocks()));
            blocks = unique(all, BlockDef::id, "block");
        }
        return blocks;
    }

    public static synchronized Map<String, ModelSpec> models() {
        if (models == null) {
            Map<String, ModelSpec> map = new LinkedHashMap<>();
            for (ContentModule m : Modules.ALL) {
                for (ModelSpec spec : m.models()) {
                    if (map.put(spec.id(), spec) != null) {
                        throw new IllegalStateException("Duplicate model " + spec.id());
                    }
                }
            }
            models = Collections.unmodifiableMap(map);
        }
        return models;
    }

    private static List<com.runecraft.osrs.world.StructureDef> structures;

    public static synchronized List<com.runecraft.osrs.world.StructureDef> structures() {
        if (structures == null) {
            List<com.runecraft.osrs.world.StructureDef> all = new ArrayList<>();
            Modules.ALL.forEach(m -> all.addAll(m.structures()));
            structures = unique(all, com.runecraft.osrs.world.StructureDef::id, "structure");
        }
        return structures;
    }

    public static MonsterDef monster(String id) {
        for (MonsterDef m : monsters()) {
            if (m.id.equals(id)) {
                return m;
            }
        }
        return null;
    }

    public static BossDef boss(String id) {
        for (BossDef b : bosses()) {
            if (b.id.equals(id)) {
                return b;
            }
        }
        return null;
    }
}
