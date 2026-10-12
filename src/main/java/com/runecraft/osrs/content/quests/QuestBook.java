package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Quest;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.QuestDef;
import com.runecraft.osrs.model.ModelSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Everything the quests module contributes, collected from the quest group classes (F2P, Kandarin,
 * Fremennik, Morytania...). Built once; both the data module and the Minecraft interpreter read it.
 */
public final class QuestBook {
    /** A virtual equipment slot the base game doesn't have yet (gloves, cape, ammo-saving device). */
    public enum GearSlot { HANDS, CAPE, NECK }

    /** Stats of an accessory worn in a {@link GearSlot}. */
    public record Gear(String item, GearSlot slot, EquipmentStats stats, String requiredQuest) {
    }

    /**
     * Something an NPC sells once its requirements are met (Ava's accumulator, quest cape, RFD gloves).
     * {@code quest} must be complete and {@code flag} set (each optional); entries of the same NPC are
     * offered best-first (the last-declared qualifying entry the player doesn't already own).
     */
    public record Shop(String npc, String item, int price, String quest, String flag, com.runecraft.osrs.Skill skill, int level,
                       String pitch) {
        /**
         * Whether the shop's flag condition holds. {@code flag} is a single flag, or
         * {@code "count:N:a,b,c"} meaning at least N of the listed flags (RFD gloves unlock by the
         * number of subquests done, in any order).
         */
        public boolean flagsMet(java.util.function.Predicate<String> hasFlag) {
            if (flag == null) {
                return true;
            }
            if (flag.startsWith("count:")) {
                String[] parts = flag.split(":", 3);
                int need = Integer.parseInt(parts[1]);
                int have = 0;
                for (String f : parts[2].split(",")) {
                    if (hasFlag.test(f)) {
                        have++;
                    }
                }
                return have >= need;
            }
            return hasFlag.test(flag);
        }
    }

    private static QuestBook instance;

    public final List<QuestSpec> quests = new ArrayList<>();
    public final List<NpcDef> npcs = new ArrayList<>();
    public final List<PlaceDef> places = new ArrayList<>();
    public final List<BossDef> bosses = new ArrayList<>();
    public final List<MonsterDef> monsters = new ArrayList<>();
    public final List<ModelSpec> models = new ArrayList<>();
    public final List<BlockDef> blocks = new ArrayList<>();
    public final List<Consumer<ItemSink>> items = new ArrayList<>();
    public final Map<String, Gear> gear = new LinkedHashMap<>();
    public final List<Shop> shops = new ArrayList<>();
    /** Weapon item id -> quest id that must be complete to wield it effectively. */
    public final Map<String, String> weaponGates = new LinkedHashMap<>();

    private QuestBook() {
    }

    public static synchronized QuestBook get() {
        if (instance == null) {
            QuestBook b = new QuestBook();
            QuestModels.register(b);
            QuestItemsData.register(b);
            F2pQuests.register(b);
            AsgarniaKandarinQuests.register(b);
            KandarinGnomeQuests.register(b);
            KaramjaQuests.register(b);
            FremennikQuests.register(b);
            DesertQuests.register(b);
            MorytaniaQuests.register(b);
            TrollQuests.register(b);
            ElfQuests.register(b);
            GrandmasterQuests.register(b);
            MiscQuests.register(b);
            KandarinMoreQuests.register(b);
            MisthalinMoreQuests.register(b);
            LateQuestsA.register(b);
            LateQuestsB.register(b);
            RecipeForDisaster.register(b);
            DwarfGoblinQuests.register(b);
            MahjarratQuests.register(b);
            LateQuestsC.register(b);
            VarlamoreQuests.register(b);
            Miniquests.register(b);
            WiseOldMan.register(b);
            b.quests.forEach(QuestSpec::seal);
            instance = b;
        }
        return instance;
    }

    // ---------------- Builders ----------------

    public QuestSpec quest(String id, String name, Quest.Difficulty difficulty, int qp) {
        QuestSpec q = new QuestSpec(id, name, difficulty, qp);
        quests.add(q);
        return q;
    }

    public void npc(String id, String name, String place, int dx, int dz, int skin, int top, int bottom, int hat) {
        npcs.add(NpcDef.of(id, name, place, dx, dz, skin, top, bottom, hat));
    }

    /** A spot in the world with no generated structure (NPCs stand on the terrain). */
    public void spot(String id, String name, int osrsX, int osrsY) {
        places.add(PlaceDef.of(id, name, osrsX, osrsY, PlaceDef.Style.NONE, List.of()));
    }

    public void place(String id, String name, int osrsX, int osrsY, PlaceDef.Style style, String... blocks) {
        places.add(PlaceDef.of(id, name, osrsX, osrsY, style, List.of(blocks)));
    }

    /** Adds a quest boss (category "Quest bosses"). */
    public BossDef boss(BossDef boss) {
        boss.category("Quest bosses");
        bosses.add(boss);
        return boss;
    }

    public MonsterDef monster(MonsterDef monster) {
        monsters.add(monster);
        return monster;
    }

    public void block(BlockDef block) {
        blocks.add(block);
    }

    public void model(ModelSpec spec) {
        models.add(spec);
    }

    /** A plain untradeable quest item. */
    public void item(String id, String name, String shape, int color, String examine) {
        items.add(s -> s.add(id, name, ItemDef.Kind.SIMPLE, shape, color).value(0).examine(examine));
    }

    /** An item with custom setup. */
    public void items(Consumer<ItemSink> adder) {
        items.add(adder);
    }

    public void gear(String item, GearSlot slot, EquipmentStats stats, String requiredQuest) {
        gear.put(item, new Gear(item, slot, stats, requiredQuest));
    }

    public void shop(String npc, String item, int price, String quest, String flag, com.runecraft.osrs.Skill skill, int level, String pitch) {
        shops.add(new Shop(npc, item, price, quest, flag, skill, level, pitch));
    }

    public void gate(String weapon, String questId) {
        weaponGates.put(weapon, questId);
    }

    // ---------------- Lookups ----------------

    public QuestSpec spec(String id) {
        for (QuestSpec q : quests) {
            if (q.id().equals(id)) {
                return q;
            }
        }
        return null;
    }

    public List<QuestDef> questDefs() {
        List<QuestDef> defs = new ArrayList<>();
        quests.forEach(q -> defs.add(q.def));
        return Collections.unmodifiableList(defs);
    }

    /** Total quest points of the module quests. */
    public int questPoints() {
        int total = 0;
        for (QuestSpec q : quests) {
            total += q.def.questPoints();
        }
        return total;
    }
}
