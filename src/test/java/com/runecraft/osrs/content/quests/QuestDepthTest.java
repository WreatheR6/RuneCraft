package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.FreeQuestObjectives;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Quest;
import com.runecraft.osrs.QuestNpc;
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
        // Round 21: deepened inside their existing stages.
        Map.ofEntries(Map.entry("merlins_crystal", 7), Map.entry("holy_grail", 9), Map.entry("plague_city", 7), Map.entry("biohazard", 7),
                Map.entry("the_dig_site", 7), Map.entry("underground_pass", 4), Map.entry("recruitment_drive", 3),
                Map.entry("children_of_the_sun", 3), Map.entry("song_of_the_elves", 5), Map.entry("monkey_madness_i", 9),
                Map.entry("fairytale_i_growing_pains", 4), Map.entry("fairytale_ii_cure_a_queen", 6), Map.entry("horror_from_the_deep", 6),
                Map.entry("making_history", 3), Map.entry("dream_mentor", 5), Map.entry("creature_of_fenkenstrain", 5),
                Map.entry("ghosts_ahoy", 5), Map.entry("in_search_of_the_myreque", 5), Map.entry("prince_ali_rescue", 5),
                Map.entry("the_fremennik_isles", 5), Map.entry("the_golem", 6), Map.entry("learning_the_ropes", 2))
                .forEach((id, n) -> assertEquals(n, spec(id).stages.size(), id));
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

    /** Regression guard on the survey: no quest advances by talking alone (round 20: 3), none is shallow (round 20: 60, round 21: 41). */
    @Test
    void fewTalkOnlyQuests() {
        long talkOnly = QuestBook.get().quests.stream().filter(q -> q.stages.stream().noneMatch(QuestDepthTest::objective)).count();
        assertEquals(0, talkOnly, talkOnly + " talk-only quests");
        long shallow = QuestBook.get().quests.stream()
                .filter(q -> q.stages.stream().filter(QuestDepthTest::objective).count() * 2 < q.stages.size()).count();
        assertEquals(0, shallow, shallow + " shallow quests (fewer than half their stages have objectives)");
    }

    @Test
    void round21QuestsAreNoLongerShallow() {
        for (String id : List.of("merlins_crystal", "holy_grail", "plague_city", "biohazard", "the_dig_site", "underground_pass",
                "recruitment_drive", "children_of_the_sun", "learning_the_ropes", "song_of_the_elves", "monkey_madness_i",
                "fairytale_i_growing_pains", "fairytale_ii_cure_a_queen", "horror_from_the_deep", "making_history", "dream_mentor",
                "creature_of_fenkenstrain", "ghosts_ahoy", "in_search_of_the_myreque", "prince_ali_rescue", "the_fremennik_isles", "the_golem")) {
            QuestSpec q = spec(id);
            long real = q.stages.stream().filter(QuestDepthTest::objective).count();
            assertTrue(real * 2 >= q.stages.size(), id + ": only " + real + "/" + q.stages.size() + " stages have objectives");
        }
    }

    @Test
    void merlinsCrystalRitual() {
        QuestSpec q = spec("merlins_crystal");
        assertTrue(find(q, Trigger.Kind.REACH, "arheins_crate").needFlags.contains("mc_lancelot"));
        Trigger hive = find(q, Trigger.Kind.REACH, "catherby_beehives");
        assertTrue(hive.need.containsKey("insect_repellent") && hive.take.containsKey("minecraft:bucket") && hive.give.containsKey("bucket_of_wax"));
        assertTrue(find(q, Trigger.Kind.TALK, "candle_maker").take.containsKey("bucket_of_wax"));
        Trigger beggar = find(q, Trigger.Kind.REACH, "port_sarim_jewellers");
        assertTrue(beggar.take.containsKey("bread") && beggar.give.containsKey("excalibur"));
        Trigger star = find(q, Trigger.Kind.REACH, "camelot_star_symbol");
        assertEquals(Set.of("black_candle", "bat_bones"), star.take.keySet());
        assertTrue(star.need.keySet().containsAll(Set.of("excalibur", "tinderbox")) && star.needFlags.contains("mc_words"));
        assertTrue(find(q, Trigger.Kind.REACH, "merlins_crystal_tower").need.containsKey("excalibur"));
        assertPlace("camelot_star_symbol", 2780, 3515);
        assertPlace("port_sarim_jewellers", 3016, 3246);
    }

    @Test
    void holyGrailWhistlesBellAndFeather() {
        QuestSpec q = spec("holy_grail");
        assertEquals(2, find(q, Trigger.Kind.REACH, "draynor_manor_whistles").give.get("magic_whistle"));
        assertTrue(find(q, Trigger.Kind.REACH, "karamja_grail_tower").need.keySet().containsAll(Set.of("magic_whistle", "excalibur")));
        assertTrue(q.triggers().stream().anyMatch(t -> t.target.equals("fisher_king") && t.need.containsKey("grail_bell")));
        assertTrue(find(q, Trigger.Kind.REACH, "goblin_village_sacks").need.containsKey("magic_gold_feather"));
        assertPlace("karamja_grail_tower", 2742, 3236);
    }

    @Test
    void plagueCityAndBiohazardFollowTheWalkthrough() {
        QuestSpec pc = spec("plague_city");
        assertTrue(find(pc, Trigger.Kind.TALK, "alrena").take.containsKey("dwellberries"));
        assertTrue(pc.triggers().stream().anyMatch(t -> t.target.equals("edmond") && t.take.containsKey("rope")
                && t.take.getOrDefault("minecraft:water_bucket", 0) == 4 && t.need.containsKey("spade")));
        assertTrue(find(pc, Trigger.Kind.TALK, "jethick").need.containsKey("picture_of_elena"));
        assertTrue(find(pc, Trigger.Kind.REACH, "plague_house").give.containsKey("plague_house_key"));
        QuestSpec bh = spec("biohazard");
        assertTrue(find(bh, Trigger.Kind.REACH, "biohazard_watchtower").take.keySet().containsAll(Set.of("bird_feed", "pigeon_cage")));
        assertTrue(find(bh, Trigger.Kind.TALK, "omart").needFlags.contains("bh_pigeons"));
        assertTrue(find(bh, Trigger.Kind.KILL, "head_mourner").need.containsKey("medical_gown"));
        Trigger guidor = find(bh, Trigger.Kind.TALK, "guidor");
        assertTrue(guidor.need.keySet().containsAll(Set.of("priest_gown_top", "priest_gown_bottom")));
        assertTrue(guidor.take.keySet().containsAll(Set.of("plague_sample", "touch_paper", "ethenea", "liquid_honey", "sulphuric_broline")));
    }

    @Test
    void digSiteExamsNeedTheStudents() {
        QuestSpec q = spec("the_dig_site");
        List<Trigger> exams = q.triggers().stream().filter(t -> t.target.equals("examiner") && !t.needFlags.isEmpty()).toList();
        assertEquals(3, exams.size());
        assertTrue(exams.get(0).take.containsKey("sealed_letter"));
        assertTrue(q.triggers().stream().anyMatch(t -> t.target.equals("digsite_student_female") && t.take.containsKey("opal")));
        assertTrue(q.triggers().stream().anyMatch(t -> t.give.containsKey("explosive_compound")
                && t.take.keySet().containsAll(Set.of("chemical_powder", "unidentified_liquid", "charcoal", "arcenia_root"))));
        assertTrue(q.triggers().stream().anyMatch(t -> t.take.containsKey("explosive_compound") && t.need.containsKey("tinderbox")
                && t.give.containsKey("stone_tablet")));
    }

    @Test
    void recruitmentDriveAndChildrenOfTheSunHaveRealTests() {
        QuestSpec rd = spec("recruitment_drive");
        assertEquals(20, ContentRegistry.boss("sir_leye").combatLevel);
        assertTrue(find(rd, Trigger.Kind.KILL, "sir_leye").setFlags.contains("rd_leye"));
        assertTrue(rd.triggers().stream().anyMatch(t -> t.target.equals("sir_tiffy_cashien") && t.completes()
                && t.needFlags.containsAll(List.of("rd_tinley", "rd_ren", "rd_cheevers", "rd_hynn"))));
        QuestSpec cots = spec("children_of_the_sun");
        assertTrue(find(cots, Trigger.Kind.REACH, "cots_bandit_house").needFlags.containsAll(List.of("cots_hide1", "cots_hide2")));
        assertTrue(cots.triggers().stream().anyMatch(t -> t.target.equals("sergeant_tobyn") && t.completes()
                && t.needFlags.containsAll(List.of("cots_b1", "cots_b2", "cots_b3", "cots_b4"))));
        // Song of the Elves starts with Edmond in East Ardougne (OSRS), not Arianwyn.
        assertTrue(spec("song_of_the_elves").stages.get(0).triggers.get(0).target.equals("edmond"));
    }

    /** Round 22: the last 41 shallow quests, deepened inside their existing stages. */
    @Test
    void round22QuestsKeepTheirStagesAndHaveObjectives() {
        Map.ofEntries(
                Map.entry("bone_voyage", 3), Map.entry("temple_of_the_eye", 5), Map.entry("darkness_of_hallowvale", 3),
                Map.entry("sins_of_the_father", 5), Map.entry("the_slug_menace", 5), Map.entry("rum_deal", 3),
                Map.entry("client_of_kourend", 3), Map.entry("tears_of_guthix", 3), Map.entry("a_tail_of_two_cats", 4),
                Map.entry("defender_of_varrock", 3), Map.entry("garden_of_tranquillity", 3), Map.entry("the_giant_dwarf", 3),
                Map.entry("kings_ransom", 3), Map.entry("spirits_of_the_elid", 4), Map.entry("contact", 5),
                Map.entry("the_depths_of_despair", 3), Map.entry("the_queen_of_thieves", 3), Map.entry("tale_of_the_righteous", 3),
                Map.entry("the_forsaken_tower", 3), Map.entry("getting_ahead", 3), Map.entry("a_kingdom_divided", 4),
                Map.entry("death_to_the_dorgeshuun", 6), Map.entry("secrets_of_the_north", 6),
                Map.entry("my_arms_big_adventure", 5), Map.entry("one_small_favour", 7), Map.entry("a_souls_bane", 5),
                Map.entry("the_fremennik_exiles", 5), Map.entry("a_night_at_the_theatre", 5), Map.entry("perilous_moons", 4),
                Map.entry("death_on_the_isle", 4), Map.entry("ethically_acquired_antiquities", 3),
                Map.entry("shadows_of_custodia", 3), Map.entry("the_final_dawn", 4), Map.entry("architectural_alliance", 3),
                Map.entry("daddys_home", 3), Map.entry("into_the_tombs", 3), Map.entry("mage_arena_i", 3),
                Map.entry("the_ides_of_milk", 6), Map.entry("current_affairs", 6), Map.entry("the_blood_moon_rises", 8))
                .forEach((id, n) -> {
                    QuestSpec q = spec(id);
                    assertEquals(n, q.stages.size(), id);
                    long real = q.stages.stream().filter(QuestDepthTest::objective).count();
                    assertTrue(real * 2 >= q.stages.size(), id + ": only " + real + "/" + q.stages.size() + " stages have objectives");
                });
    }

    @Test
    void contactGiantDwarfAndKingsRansomFollowTheWalkthrough() {
        QuestSpec contact = spec("contact");
        Trigger kaleef = find(contact, Trigger.Kind.REACH, "sophanem_dungeon");
        assertTrue(kaleef.give.containsKey("contact_parchment") && kaleef.need.containsKey("tinderbox"));
        assertTrue(find(contact, Trigger.Kind.TALK, "maisa").needFlags.contains("ct_kaleef"));
        assertTrue(contact.triggers().stream().anyMatch(t -> t.target.equals("osman") && t.take.containsKey("contact_parchment")));
        assertTrue(contact.triggers().stream().anyMatch(t -> t.target.equals("high_priest_sophanem") && t.completes()
                && t.needFlags.contains("ct_scarab")));

        QuestSpec gd = spec("the_giant_dwarf");
        assertEquals(Set.of("exquisite_clothes", "exquisite_boots", "gd_dwarven_battleaxe"), find(gd, Trigger.Kind.TALK, "riki_the_sculptor").take.keySet());
        assertEquals(3, find(gd, Trigger.Kind.TALK, "santiri").take.get("sapphire"));
        assertTrue(find(gd, Trigger.Kind.TALK, "thurgo").take.containsKey("iron_bar"));
        assertTrue(find(gd, Trigger.Kind.TALK, "vermundi").take.keySet().containsAll(Set.of("coins", "coal", "logs")));
        assertTrue(gd.triggers().stream().anyMatch(t -> t.target.equals("commander_veldaban") && t.completes() && t.needFlags.contains("gd_joined")));

        QuestSpec kr = spec("kings_ransom");
        assertTrue(find(kr, Trigger.Kind.TALK, "kr_mansion_guard").take.keySet()
                .containsAll(Set.of("kr_scrap_paper", "kr_address_form", "kr_black_knight_helm")));
        Trigger arthur = find(kr, Trigger.Kind.REACH, "black_knights_fortress");
        assertTrue(arthur.take.keySet().containsAll(Set.of("quest_animate_rock_scroll", "granite_500g", "bronze_med_helm", "iron_chainbody")));
        assertTrue(arthur.need.keySet().containsAll(Set.of("black_full_helm", "black_platebody", "black_platelegs")));
        assertTrue(kr.triggers().stream().anyMatch(t -> t.target.equals("king_arthur") && t.completes() && t.needFlags.contains("kr_arthur")));
        // A Tail of Two Cats: Hild's 5 death runes before Bob can be understood.
        assertEquals(5, find(spec("a_tail_of_two_cats"), Trigger.Kind.TALK, "quest_hild").take.get("death_rune"));
        assertTrue(spec("tears_of_guthix").triggers().stream().anyMatch(t -> t.target.equals("juna") && t.need.containsKey("chisel")));
    }

    /** Hand-scripted F2P characters stand on their OSRS tiles; far ones spawn with their own chunk. */
    @Test
    void questNpcsStandOnTheirOsrsTiles() {
        assertTile(QuestNpc.COOK, 3209, 3215);
        assertTile(QuestNpc.FATHER_URHNEY, 3146, 3174);
        assertTile(QuestNpc.DR_HARLOW, 3222, 3397);
        assertTile(QuestNpc.DORIC, 2952, 3451);
        assertTrue(QuestNpc.FATHER_URHNEY.remote() && QuestNpc.DR_HARLOW.remote());
        assertTrue(!QuestNpc.ROMEO.remote() && !QuestNpc.SEDRIDOR.onOsrsTile());
    }

    private static void assertTile(QuestNpc n, int x, int y) {
        assertTrue(n.onOsrsTile(), n.name());
        assertEquals(GielinorMap.toBlockX(x), n.blockX(), n.name());
        assertEquals(GielinorMap.toBlockZ(y), n.blockZ(), n.name());
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
