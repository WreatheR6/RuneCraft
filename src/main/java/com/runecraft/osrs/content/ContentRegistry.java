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
            all.forEach(com.runecraft.osrs.data.MonsterStatsTable::apply);
            all.forEach(com.runecraft.osrs.data.MonsterTraits::apply);
            all.forEach(com.runecraft.osrs.data.MonsterDropsTable::apply);
            monsters = unique(all, d -> d.id, "monster");
        }
        return monsters;
    }

    public static synchronized List<BossDef> bosses() {
        if (bosses == null) {
            List<BossDef> all = new ArrayList<>();
            Modules.ALL.forEach(m -> all.addAll(m.bosses()));
            all.forEach(com.runecraft.osrs.data.MonsterStatsTable::apply);
            all.forEach(com.runecraft.osrs.data.MonsterTraits::apply);
            all.forEach(com.runecraft.osrs.data.BossDropsTable::apply);
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
            // Re-sculpted versions of other modules' models replace them in place (same id, so every
            // creature that uses the model picks up the new look).
            for (ContentModule m : Modules.ALL) {
                for (ModelSpec spec : m.modelOverrides()) {
                    if (!map.containsKey(spec.id())) {
                        throw new IllegalStateException("Model override for unknown model " + spec.id());
                    }
                    map.put(spec.id(), spec);
                }
            }
            // Look-alikes copy a (possibly re-sculpted) registered model under the creature's own id. A variant
            // whose base is itself a fallback model is resolved after the fallbacks and replaces the fallback
            // generated for its own id.
            List<ModelSpec.Variant> pending = new ArrayList<>();
            for (ContentModule m : Modules.ALL) {
                for (ModelSpec.Variant v : m.modelVariants()) {
                    ModelSpec base = map.get(v.base());
                    if (base == null) {
                        pending.add(v);
                    } else {
                        map.putIfAbsent(v.id(), base.copy(v.id(), v.scale(), v.tint()));
                    }
                }
            }
            // Last, modules may supply models for creatures that still have none (so nothing falls back to
            // the plain biped); they only ever add ids that are not taken yet.
            java.util.Set<String> fromFallback = new java.util.HashSet<>();
            for (ContentModule m : Modules.ALL) {
                for (ModelSpec spec : m.fallbackModels(Collections.unmodifiableSet(map.keySet()))) {
                    if (map.putIfAbsent(spec.id(), spec) == null) {
                        fromFallback.add(spec.id());
                    }
                }
            }
            for (ModelSpec.Variant v : pending) {
                ModelSpec base = map.get(v.base());
                if (base == null) {
                    throw new IllegalStateException("Model variant " + v.id() + " of unknown model " + v.base());
                }
                if (!map.containsKey(v.id()) || fromFallback.contains(v.id())) {
                    map.put(v.id(), base.copy(v.id(), v.scale(), v.tint()));
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
