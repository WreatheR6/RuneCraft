package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.bosses.Dt2Bosses;
import com.runecraft.osrs.content.quests.QuestSpec.Trigger;
import com.runecraft.osrs.content.raids.RaidScaling;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round 25: Desert Treasure II's boss chapters (quest versions of the four bosses, the Mysterious Figure and Sliske's
 * wights), Garden of Tranquillity, Secrets of the North and One Small Favour follow their OSRS Wiki walkthroughs inside
 * their saved stage counts.
 */
class QuestFinalChaptersTest {
    private static QuestSpec spec(String id) {
        return QuestBook.get().quests.stream().filter(q -> q.id().equals(id)).findFirst().orElseThrow();
    }

    private static Trigger find(QuestSpec q, Trigger.Kind kind, String target) {
        return q.triggers().stream().filter(t -> t.kind == kind && t.target.equals(target)).findFirst().orElseThrow();
    }

    private static boolean any(QuestSpec q, java.util.function.Predicate<Trigger> p) {
        return q.triggers().stream().anyMatch(p);
    }

    @Test
    void desertTreasureTwoBossChapters() {
        QuestSpec q = spec("desert_treasure_ii");
        assertEquals(6, q.stages.size());
        // osrs-dps-calc "Quest" versions: combat level and hitpoints.
        Map.of("dt2_quest_vardorvis", new int[]{572, 500}, "dt2_quest_leviathan", new int[]{593, 720},
                "dt2_quest_duke_sucellus", new int[]{538, 330}, "dt2_quest_whisperer", new int[]{587, 660},
                "dt2_kasonde", new int[]{231, 240}, "dt2_mysterious_figure", new int[]{271, 450},
                "dt2_forsaken_assassin", new int[]{252, 200}, "dt2_ketla_the_unworthy", new int[]{236, 200},
                "dt2_kasonde_the_craven", new int[]{221, 200}, "dt2_persten_the_deceitful", new int[]{264, 200})
                .forEach((id, v) -> {
                    BossDef b = ContentRegistry.boss(id);
                    assertEquals(v[0], b.combatLevel, id);
                    assertEquals(v[1], b.hitpoints, id);
                });
        // The medallions come from the chapters, no longer from drops of the post-quest bosses.
        assertTrue(q.triggers().stream().noneMatch(t -> t.kind == Trigger.Kind.DROP));
        for (String boss : List.of("dt2_quest_vardorvis", "dt2_quest_leviathan", "dt2_quest_duke_sucellus", "dt2_quest_whisperer")) {
            assertTrue(any(q, t -> boss.equals(t.spawn)), boss + " is never spawned");
            assertEquals(3, find(q, Trigger.Kind.KILL, boss).stage, boss);
        }
        assertEquals(4, find(q, Trigger.Kind.KILL, "dt2_strangled").count);
        assertTrue(find(q, Trigger.Kind.TALK, "kasonde").setFlags.contains("dt2_v_kasonde"));
        assertTrue(find(q, Trigger.Kind.KILL, "dt2_kasonde").give.containsKey("vardorvis_medallion_piece"));
        assertTrue(find(q, Trigger.Kind.REACH, "dt2_scar_northwest").need.containsKey("tinderbox"));
        Trigger asylum = find(q, Trigger.Kind.REACH, "dt2_ghorrock_asylum");
        assertTrue(asylum.tools.contains("pickaxe") && asylum.need.containsKey("pestle_and_mortar"));
        assertTrue(find(q, Trigger.Kind.REACH, "dt2_camdozaal_sinkhole").take.containsKey("dt2_very_long_rope"));
        // The final chapter: the Mysterious Figure, then Sliske's four wights in order.
        Trigger last = q.stages.get(4).triggers.stream().filter(t -> t.next != t.stage).findFirst().orElseThrow();
        assertEquals("dt2_persten_the_deceitful", last.target);
        assertTrue(last.needFlags.contains("dt2_f_w3"));
        assertEquals("dt2_ketla_the_unworthy", find(q, Trigger.Kind.KILL, "dt2_forsaken_assassin").spawn);
        assertEquals("dt2_kasonde_the_craven", find(q, Trigger.Kind.KILL, "dt2_ketla_the_unworthy").spawn);
        assertEquals("dt2_persten_the_deceitful", find(q, Trigger.Kind.KILL, "dt2_kasonde_the_craven").spawn);
        assertEquals("Perseriya's medallion", com.runecraft.osrs.ItemCatalog.get("leviathan_medallion_piece").name());
        // The quest versions reuse the bosses' scripts with lower max hits.
        assertEquals(36.0 / 56, Dt2Bosses.QUEST_DUKE_SCALE, 1e-9);
        assertEquals(37, ContentRegistry.boss("dt2_quest_vardorvis").attacks.get(0).maxHit());
    }

    @Test
    void secretsOfTheNorthFollowsTheWalkthrough() {
        QuestSpec q = spec("secrets_of_the_north");
        assertEquals("sotn_carnillean_guard", q.stages.get(0).triggers.get(0).target);
        assertTrue(find(q, Trigger.Kind.TALK, "khazard_barman").setFlags.contains("sotn_barman"));
        assertEquals("quest_evelot", find(q, Trigger.Kind.REACH, "sotn_evelot_trail").spawn);
        assertTrue(find(q, Trigger.Kind.TALK, "sotn_hazeel").setFlags.contains("sotn_hazeel"));
        assertTrue(any(q, t -> t.target.equals("sotn_hazeel") && t.take.containsKey("sotn_dusty_scroll")));
        assertTrue(any(q, t -> t.kind == Trigger.Kind.PUZZLE && t.target.equals("sotn_weiss_ritual_room") && t.need.containsKey("tinderbox")));
        assertTrue(any(q, t -> t.target.equals("sotn_carnillean_guard") && t.completes() && t.needFlags.contains("sotn_creature")));
        assertEquals(148, ContentRegistry.boss("quest_evelot").combatLevel);
        assertEquals(262, ContentRegistry.boss("quest_assassin").combatLevel);
        assertEquals(368, ContentRegistry.boss("quest_strange_creature").combatLevel);
        assertEquals(550, ContentRegistry.boss("quest_strange_creature").hitpoints);
    }

    @Test
    void gardenOfTranquillityGardenersAndStatues() {
        QuestSpec q = spec("garden_of_tranquillity");
        assertTrue(find(q, Trigger.Kind.TALK, "quest_wise_old_man").need.containsKey("ring_of_charos"));
        assertEquals(3, find(q, Trigger.Kind.REACH, "got_lyra").take.get("onion_seed"));
        assertEquals(3, find(q, Trigger.Kind.REACH, "got_kragen").take.get("cabbage_seed"));
        assertTrue(find(q, Trigger.Kind.REACH, "got_elstan").take.containsKey("marigold_seed"));
        assertTrue(find(q, Trigger.Kind.REACH, "got_white_tree").need.containsKey("secateurs"));
        assertTrue(find(q, Trigger.Kind.TALK, "got_brother_althric").need.containsKey("fishing_rod"));
        assertEquals(2, find(q, Trigger.Kind.TALK, "got_bernald").take.get("plant_cure"));
        Trigger plant = q.stages.get(1).triggers.stream().filter(t -> t.next != t.stage).findFirst().orElseThrow();
        assertTrue(plant.needFlags.containsAll(List.of("got_statue_lumbridge", "got_statue_falador")));
        assertTrue(plant.take.keySet().containsAll(List.of("got_orchid_seed", "got_snowdrop_seed", "got_delphinium_seed",
                "got_white_tree_shoot", "got_rose_seed", "got_vine_seed", "compost")));
        assertTrue(q.def.itemRewards().containsAll(List.of("apple_seed", "acorn", "guam_seed:5", "compost_potion")));
    }

    @Test
    void oneSmallFavourChain() {
        QuestSpec q = spec("one_small_favour");
        for (String npc : List.of("quest_brian_archery", "aggie", "osf_johanhus", "quest_seth_groats", "quest_horvik", "osf_tassie",
                "osf_hammerspike", "osf_bleemadge", "quest_ca_arhein", "osf_phantuwti", "wizard_cromperty", "rantz", "osf_gnormadium")) {
            assertTrue(any(q, t -> t.kind == Trigger.Kind.TALK && t.target.equals(npc)), npc);
        }
        assertEquals(3, find(q, Trigger.Kind.TALK, "quest_horvik").take.get("steel_bar"));
        assertTrue(find(q, Trigger.Kind.REACH, "osf_sanfew").take.keySet().containsAll(List.of("harralander", "guam_leaf", "marrentill")));
        assertEquals(3, find(q, Trigger.Kind.KILL, "osf_dwarf_gang_member").count);
        assertTrue(any(q, t -> t.target.equals("osf_goblin_cave") && "quest_slagilith".equals(t.spawn)
                && t.need.containsKey("quest_animate_rock_scroll")));
        BossDef slag = ContentRegistry.boss("quest_slagilith");
        assertEquals(92, slag.combatLevel);
        assertEquals(60, slag.hitpoints);
        assertEquals(13, slag.attacks.get(0).maxHit());
        assertTrue(any(q, t -> t.target.equals("yanni_salika") && t.completes() && t.take.containsKey("osf_red_mahogany")));
    }

    @Test
    void bloatTurnsMoreOftenInHardMode() {
        assertEquals(32, RaidScaling.TOB_BLOAT_TURN);
        assertEquals(16, RaidScaling.TOB_BLOAT_TURN_HARD);
    }
}
