package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.FreeQuestObjectives;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Quest;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.quests.QuestSpec.Stage;
import com.runecraft.osrs.content.quests.QuestSpec.Trigger;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Quest depth: the most-played quests progress through their real OSRS objectives (items, kills and
 * places from the wiki walkthroughs and Zoinkwiz/quest-helper steps), not just "talk to X".
 */
class QuestDepthTest {
    private static QuestSpec spec(String id) {
        return QuestBook.get().quests.stream().filter(q -> q.id().equals(id)).findFirst().orElseThrow();
    }

    /** Whether a stage has an objective beyond plain talking. */
    private static boolean objective(Stage s) {
        for (Trigger t : s.triggers) {
            if (t.kind != Trigger.Kind.TALK || !t.need.isEmpty() || !t.take.isEmpty() || !t.needFlags.isEmpty()
                    || !t.skills.isEmpty() || t.spawn != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean has(QuestSpec q, Trigger.Kind kind, String target) {
        return q.triggers().stream().anyMatch(t -> t.kind == kind && t.target.equals(target));
    }

    private static Trigger find(QuestSpec q, Trigger.Kind kind, String target) {
        return q.triggers().stream().filter(t -> t.kind == kind && t.target.equals(target)).findFirst().orElseThrow();
    }

    /** Stage counts are saved (stage == count means complete): deepening a quest must not change them. */
    @Test
    void deepenedQuestsKeepTheirStageCounts() {
        Map<String, Integer> counts = Map.of("ernest_the_chicken", 3, "witchs_house", 4, "fight_arena", 7,
                "priest_in_peril", 6, "tree_gnome_village", 5, "dragon_slayer_i", 6, "lost_city", 6, "waterfall_quest", 6);
        counts.forEach((id, n) -> assertEquals(n, spec(id).stages.size(), id));
        assertEquals(2, Quest.COOKS_ASSISTANT.finalStage());
        assertEquals(5, Quest.RESTLESS_GHOST.finalStage());
        assertEquals(3, Quest.WITCHS_POTION.finalStage());
        assertEquals(7, Quest.ROMEO_AND_JULIET.finalStage());
        assertEquals(4, Quest.DRUIDIC_RITUAL.finalStage());
        assertEquals(4, Quest.VAMPYRE_SLAYER.finalStage());
    }

    @Test
    void priorityQuestsHaveRealObjectives() {
        for (String id : List.of("dragon_slayer_i", "ernest_the_chicken", "waterfall_quest", "tree_gnome_village", "fight_arena",
                "witchs_house", "lost_city", "priest_in_peril", "animal_magnetism", "desert_treasure_i")) {
            QuestSpec q = spec(id);
            long real = q.stages.stream().filter(QuestDepthTest::objective).count();
            assertTrue(real * 3 >= q.stages.size(), id + ": only " + real + "/" + q.stages.size() + " stages have objectives");
        }
    }

    @Test
    void ernestTheChickenUsesTheManorPuzzles() {
        QuestSpec q = spec("ernest_the_chicken");
        assertTrue(q.triggers().stream().noneMatch(t -> t.kind == Trigger.Kind.DROP), "no random mob drops for the machine parts");
        Trigger fountain = find(q, Trigger.Kind.REACH, "draynor_manor_fountain");
        assertEquals(Set.of("fish_food", "poison"), fountain.take.keySet());
        assertTrue(fountain.give.containsKey("pressure_gauge"));
        assertTrue(find(q, Trigger.Kind.REACH, "draynor_manor_compost").need.containsKey("spade"));
        assertTrue(find(q, Trigger.Kind.REACH, "draynor_manor_tube_room").give.containsKey("rubber_tube"));
        assertTrue(find(q, Trigger.Kind.REACH, "draynor_manor_secret_room").give.containsKey("oil_can"));
        assertPlace("draynor_manor_fountain", 3088, 3335);
        assertPlace("draynor_manor_compost", 3085, 3361);
    }

    @Test
    void priestInPerilKeysAndWater() {
        QuestSpec q = spec("priest_in_peril");
        assertTrue(find(q, Trigger.Kind.KILL, "monk_of_zamorak").give.containsKey("golden_key"));
        assertTrue(q.triggers().stream().anyMatch(t -> t.take.containsKey("golden_key") && t.give.containsKey("iron_key_pip")));
        assertTrue(q.triggers().stream().anyMatch(t -> t.take.containsKey("murky_water") && t.give.containsKey("blessed_water")));
        // Drezel's cell opens with the iron key (it used to take the golden key).
        assertTrue(q.triggers().stream().anyMatch(t -> t.take.containsKey("iron_key_pip") && t.take.containsKey("blessed_water")));
        assertTrue(q.triggers().stream().anyMatch(t -> t.take.getOrDefault("rune_essence", 0) == 50));
    }

    @Test
    void fightArenaFightsInOrder() {
        QuestSpec q = spec("fight_arena");
        Trigger ogre = find(q, Trigger.Kind.KILL, "khazard_ogre");
        Trigger scorpion = find(q, Trigger.Kind.KILL, "khazard_scorpion");
        Trigger bouncer = find(q, Trigger.Kind.KILL, "bouncer");
        assertTrue(ogre.setFlags.contains("fa_ogre") && scorpion.needFlags.contains("fa_ogre"));
        assertTrue(scorpion.setFlags.contains("fa_scorpion") && bouncer.needFlags.contains("fa_scorpion"));
        assertEquals(63, ContentRegistry.boss("khazard_ogre").combatLevel);
        assertEquals(44, ContentRegistry.boss("khazard_scorpion").combatLevel);
        assertEquals(137, ContentRegistry.boss("bouncer").combatLevel);
        // Khali brew goes to the Khazard guard (Hengrad is a fellow prisoner).
        assertTrue(find(q, Trigger.Kind.REACH, "fight_arena_chest").give.containsKey("khazard_armour"));
        assertTrue(q.triggers().stream().anyMatch(t -> t.target.equals("arena_khazard_guard") && t.take.containsKey("khali_brew")));
    }

    @Test
    void witchsHouseNeedsGlovesCheeseAndMagnet() {
        QuestSpec q = spec("witchs_house");
        assertTrue(q.triggers().stream().anyMatch(t -> t.need.containsKey("leather_gloves") && t.give.containsKey("magnet")));
        Trigger mouse = find(q, Trigger.Kind.REACH, "witchs_mouse_hole");
        assertEquals(Set.of("cheese", "magnet"), mouse.take.keySet());
        assertTrue(find(q, Trigger.Kind.REACH, "witchs_fountain").needFlags.contains("wh_mouse"));
        assertTrue(find(q, Trigger.Kind.KILL, "witchs_experiment").need.containsKey("witchs_shed_key"));
    }

    @Test
    void treeGnomeVillageTrackersAndFirstOrb() {
        QuestSpec q = spec("tree_gnome_village");
        for (String npc : List.of("tracker_gnome", "tracker_gnome_2", "tracker_gnome_3")) {
            assertTrue(has(q, Trigger.Kind.TALK, npc), npc);
        }
        assertEquals(Set.of("tgv_t1", "tgv_t2", "tgv_t3"), new HashSet<>(find(q, Trigger.Kind.REACH, "tgv_ballista").needFlags));
        assertTrue(find(q, Trigger.Kind.KILL, "khazard_warlord").needFlags.contains("tgv_orb1"));
        assertPlace("tgv_tracker_1", 2501, 3261);
        assertPlace("tgv_ballista", 2509, 3211);
    }

    @Test
    void dragonSlayerShipNeedsItsHullPatched() {
        QuestSpec q = spec("dragon_slayer_i");
        assertTrue(q.triggers().stream().anyMatch(t -> t.target.equals("klarense") && t.take.getOrDefault("plank", 0) == 3
                && t.take.getOrDefault("steel_nails", 0) == 90 && t.need.containsKey("hammer")));
        assertTrue(q.triggers().stream().anyMatch(t -> t.target.equals("ned") && t.needFlags.contains("ds1_hull")));
        assertEquals(10000, find(q, Trigger.Kind.TALK, "wormbrain").take.get("coins"));
    }

    @Test
    void animalMagnetismFollowsTheWalkthrough() {
        QuestSpec q = spec("animal_magnetism");
        assertEquals(5, find(q, Trigger.Kind.TALK, "am_witch").take.get("iron_bar"));
        Trigger hammer = find(q, Trigger.Kind.REACH, "rimmington_mine");
        assertTrue(hammer.need.containsKey("hammer") && hammer.take.containsKey("selected_iron") && hammer.give.containsKey("bar_magnet"));
        assertTrue(q.triggers().stream().anyMatch(t -> t.need.containsKey("mithril_axe") && t.need.containsKey("holy_symbol")));
        assertTrue(q.triggers().stream().anyMatch(t -> t.completes() && t.take.containsKey("hard_leather") && t.take.containsKey("polished_buttons")));
    }

    @Test
    void monkeyMadnessGreegreeNeedsMonkeyBones() {
        QuestSpec q = spec("monkey_madness_i");
        assertTrue(q.triggers().stream().anyMatch(t -> t.give.containsKey("monkey_greegree") && t.take.containsKey("monkey_bones")
                && t.take.containsKey("monkey_talisman")));
        assertTrue(q.triggers().stream().noneMatch(t -> t.target.equals("tutab") && t.give.containsKey("monkey_dentures")));
    }

    /** Regression guard on the survey (round 20): at most 3 quests advance by talking alone. */
    @Test
    void fewTalkOnlyQuests() {
        long talkOnly = QuestBook.get().quests.stream().filter(q -> q.stages.stream().noneMatch(QuestDepthTest::objective)).count();
        assertTrue(talkOnly <= 3, talkOnly + " talk-only quests");
    }

    private static void assertPlace(String id, int x, int y) {
        var p = ContentRegistry.place(id);
        assertNotNull(p, id);
        assertEquals(x, p.osrsX(), id);
        assertEquals(y, p.osrsY(), id);
    }

    // ------------------------------------------------------------------ hand-scripted F2P quests

    @Test
    void freeQuestObjectivesReferenceRealThings() {
        for (FreeQuestObjectives.Spot s : FreeQuestObjectives.SPOTS) {
            assertTrue(s.fromStage() >= 1 && s.toStage() < s.quest().finalStage(), s.quest() + " stage range");
            if (s.item() != null) {
                assertNotNull(ItemCatalog.get(s.item()), s.item());
            }
        }
        for (String enchanted : FreeQuestObjectives.DRUIDIC_MEATS.values()) {
            assertNotNull(ItemCatalog.get(enchanted), enchanted);
        }
        for (FreeQuestObjectives.QuestDrop d : FreeQuestObjectives.DROPS) {
            assertNotNull(ItemCatalog.get(d.item()), d.item());
            for (String name : d.names()) {
                assertTrue(ContentRegistry.monsters().stream().anyMatch(m -> m.name.equals(name)), "no monster named " + name);
            }
        }
        for (Map<String, Integer> handIn : List.of(FreeQuestObjectives.WITCHS_POTION, FreeQuestObjectives.DRUIDIC_RITUAL,
                FreeQuestObjectives.DORICS_QUEST, FreeQuestObjectives.SHEEP_SHEARER)) {
            handIn.keySet().forEach(item -> assertNotNull(ItemCatalog.get(item), item));
        }
    }

    @Test
    void freeQuestObjectivesMatchTheWiki() {
        // Druidic Ritual: raw beef, rat, bear and chicken (not porkchop and mutton).
        assertEquals(Set.of("enchanted_beef", "enchanted_rat", "enchanted_bear", "enchanted_chicken"), FreeQuestObjectives.DRUIDIC_RITUAL.keySet());
        // Witch's Potion: standard rats drop the tail, giant rats don't.
        FreeQuestObjectives.QuestDrop tail = FreeQuestObjectives.DROPS.get(0);
        assertTrue(tail.matches(1, "Rat") && !tail.matches(1, "Giant rat"));
        assertEquals(Map.of("clay", 6, "copper_ore", 4, "iron_ore", 2), FreeQuestObjectives.DORICS_QUEST);
        assertEquals(20, FreeQuestObjectives.SHEEP_SHEARER.get("ball_of_wool"));
        // The ghost's skull is on the Wizards' Tower basement altar, the cadava bushes south-east of Varrock.
        FreeQuestObjectives.Spot skull = FreeQuestObjectives.spots(Quest.RESTLESS_GHOST, 3).get(0);
        assertTrue(skull.contains(3109, 3162) && "ghosts_skull".equals(skull.item()) && "minecraft:skeleton".equals(skull.spawn()));
        assertTrue(FreeQuestObjectives.spots(Quest.RESTLESS_GHOST, 2).isEmpty());
        FreeQuestObjectives.Spot cadava = FreeQuestObjectives.spots(Quest.ROMEO_AND_JULIET, 4).get(0);
        assertTrue(cadava.contains(3264, 3366) && cadava.contains(3277, 3374) && !cadava.contains(3211, 3422));
        FreeQuestObjectives.Spot cauldron = FreeQuestObjectives.spots(Quest.WITCHS_POTION, 2).get(0);
        assertTrue(cauldron.contains(2967, 3205) && cauldron.action() == FreeQuestObjectives.Action.COMPLETE);
        assertTrue(cadava.applies(i -> false) && !cadava.applies("cadava_potion"::equals));
    }
}
