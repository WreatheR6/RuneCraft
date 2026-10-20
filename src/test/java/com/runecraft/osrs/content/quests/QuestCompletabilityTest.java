package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.data.Boss;
import com.runecraft.osrs.data.Monster;
import com.runecraft.osrs.world.SpawnTable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Walks every quest's stages in order and checks the player can actually do each step in this world:
 * every NPC to talk to stands at a real place, every place to reach exists, every creature to kill
 * spawns somewhere (a spawn point, a camp, a quest boss arena or a boss summoned by the quest itself),
 * and every quest-only item a step needs is handed out earlier (by the same quest, a quest it
 * requires, a quest drop or a quest shop).
 */
class QuestCompletabilityTest {
    private static final QuestBook BOOK = QuestBook.get();
    /**
     * "quest:item" pairs only saves from an older version of a quest carry: the step that handed the item out was
     * replaced, and a later step still accepts it so those players can finish.
     */
    private static final Set<String> LEGACY_SAVE_ITEMS = Set.of("in_search_of_knowledge:quest_lost_tome",
            "monkey_madness_i:narnodes_orders", "making_friends_with_my_arm:bucket_of_salt",
            "a_night_at_the_theatre:quest_spider_venom_sample");

    private static String snake(String name) {
        return name.toLowerCase(Locale.ROOT).replace(' ', '_').replace("'", "");
    }

    /** Every identifier a creature that really spawns can be matched by when killed (see QuestInterpreter.ids). */
    private static Set<String> killableKeys() {
        Set<String> placed = new HashSet<>();
        SpawnTable.all().forEach(p -> placed.add(p.monster()));
        SpawnTable.camps().forEach(c -> placed.add(c.creature()));
        Set<String> keys = new HashSet<>();
        for (String id : placed) {
            keys.add(id);
            Monster m = Monster.byEntity(id);
            if (m != null) {
                keys.add(m.entity());
                keys.add(snake(m.displayName()));
            }
            MonsterDef d = ContentRegistry.monster(id);
            if (d != null) {
                keys.add(snake(d.name));
            }
        }
        return keys;
    }

    /** Items only the quests module defines: these can't be bought or gathered elsewhere. */
    private static Set<String> questOnlyItems() {
        Set<String> ids = new HashSet<>();
        BOOK.items.forEach(adder -> adder.accept((id, name, kind, shape, color) -> {
            ids.add(id);
            return new com.runecraft.osrs.ItemDef(id, name, kind, shape, color);
        }));
        return ids;
    }

    private static boolean placeExists(String place) {
        return Landmark.byName(place) != null || ContentRegistry.place(place) != null;
    }

    @Test
    void everyQuestCanBeWalkedToCompletion() {
        Set<String> killable = killableKeys();
        Set<String> questItems = questOnlyItems();
        Set<String> npcs = new HashSet<>();
        BOOK.npcs.forEach(n -> npcs.add(n.id()));
        // Items any quest, quest drop or quest shop hands out, by quest.
        Set<String> shopItems = new HashSet<>();
        BOOK.shops.forEach(s -> shopItems.add(s.item()));
        // Quest monsters and bosses drop their quest items on every kill.
        BOOK.monsters.forEach(m -> m.drops.forEach(d -> shopItems.add(d.item())));
        BOOK.bosses.forEach(m -> m.drops.forEach(d -> shopItems.add(d.item())));
        List<String> problems = new ArrayList<>();
        for (QuestSpec q : BOOK.quests) {
            // Items from shops, drops and prerequisite quests, plus whatever this quest has handed out so far.
            Set<String> obtainable = new HashSet<>(shopItems);
            Set<String> seen = new HashSet<>();
            seen.add(q.id());
            q.def.questRequirements().forEach(r -> collectGiven(r, obtainable, seen));
            q.triggers().forEach(t -> t.quests.forEach(r -> collectGiven(r, obtainable, seen)));
            Set<String> spawned = new HashSet<>();
            for (QuestSpec.Stage s : q.stages) {
                s.triggers.forEach(t -> obtainable.addAll(t.give.keySet()));
                // Bosses summoned at this stage count for the whole stage.
                s.triggers.forEach(t -> {
                    if (t.spawn != null) {
                        spawned.add(t.spawn);
                    }
                });
                for (QuestSpec.Trigger t : s.triggers) {
                    String where = q.id() + " stage " + s.index + " " + t.kind + " " + t.target;
                    switch (t.kind) {
                        case TALK -> {
                            NpcDef n = ContentRegistry.npc(t.target);
                            if (!npcs.contains(t.target) || n == null || !placeExists(n.place())) {
                                problems.add(where + ": NPC missing or not placed");
                            }
                        }
                        case REACH -> {
                            if (!placeExists(t.target)) {
                                problems.add(where + ": unknown place");
                            }
                        }
                        case KILL, DROP -> {
                            if (!reachableCreature(t.target, killable, spawned)) {
                                problems.add(where + ": creature never spawns");
                            }
                        }
                    }
                    if (t.travel != null && !placeExists(t.travel)) {
                        problems.add(where + ": unknown travel destination " + t.travel);
                    }
                    List<String> needed = new ArrayList<>(t.need.keySet());
                    needed.addAll(t.take.keySet());
                    for (String item : needed) {
                        if (!item.startsWith("minecraft:") && !ItemCatalog.exists(item)) {
                            problems.add(where + ": unknown item " + item);
                        } else if (questItems.contains(item) && !obtainable.contains(item) && !LEGACY_SAVE_ITEMS.contains(q.id() + ":" + item)) {
                            problems.add(where + ": quest item " + item + " is not handed out by this stage");
                        }
                    }
                    for (String item : t.give.keySet()) {
                        if (!item.startsWith("minecraft:") && !ItemCatalog.exists(item)) {
                            problems.add(where + ": gives unknown item " + item);
                        }
                    }
                }
            }
        }
        assertTrue(problems.isEmpty(), problems.size() + " problems:\n" + String.join("\n", problems));
    }

    /** Items handed out by a quest and (recursively) the quests it requires. */
    private static void collectGiven(String questId, Set<String> into, Set<String> seen) {
        if (!seen.add(questId)) {
            return;
        }
        QuestSpec q = BOOK.spec(questId);
        if (q == null) {
            return;
        }
        q.triggers().forEach(t -> into.addAll(t.give.keySet()));
        q.def.itemRewards().forEach(r -> into.add(r.split(":")[0]));
        for (String req : q.def.questRequirements()) {
            collectGiven(req, into, seen);
        }
        q.triggers().forEach(t -> t.quests.forEach(r -> collectGiven(r, into, seen)));
    }

    private static boolean reachableCreature(String target, Set<String> killable, Set<String> spawned) {
        if (killable.contains(target) || spawned.contains(target)) {
            return true;
        }
        if (com.runecraft.osrs.content.godwars.GwdDungeon.lair(target) != null) {
            return true; // the God Wars generals stand in their chambers of the dungeon
        }
        BossDef boss = ContentRegistry.boss(target);
        if (boss != null) {
            // A boss with a place gets an arena and a summoning stone there; a raid boss is fought in its raid.
            return boss.place != null || boss.raidOnly;
        }
        // Built-in bosses have their own lairs.
        return Boss.byEntity(target) != null;
    }

    @Test
    void legacyQuestsHaveRealRewards() {
        for (com.runecraft.osrs.Quest q : com.runecraft.osrs.Quest.values()) {
            for (String reward : q.itemRewards()) {
                assertTrue(ItemCatalog.exists(reward.split(":")[0]), q + " reward " + reward);
            }
            assertTrue(q.finalStage() >= 1, q.name());
        }
        // Items the legacy QuestManager scripts hand out or take.
        for (String item : new String[]{"pot_of_flour", "ball_of_wool", "clay", "copper_ore", "iron_ore", "air_talisman",
                "research_package", "research_notes", "ghostspeak_amulet", "ghosts_skull", "black_bead", "red_bead",
                "yellow_bead", "white_bead", "rats_tail", "eye_of_newt", "burnt_meat", "onion", "message_from_juliet",
                "cadava_potion", "cadava_berries", "beer", "stake", "silverlights_key", "silverlight", "bones", "hammer"}) {
            assertTrue(ItemCatalog.exists(item), item);
        }
    }
}
