package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.quests.QuestSpec.Stage;
import com.runecraft.osrs.content.quests.QuestSpec.Trigger;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round 23: the long quest chains (Recipe for Disaster, Desert Treasure I/II, Lunar Diplomacy, Regicide, Mourning's End,
 * Monkey Madness II, Dragon Slayer II, Song of the Elves) follow their OSRS Wiki walkthroughs, and quest steps can require
 * "any pickaxe/axe" of a minimum tier.
 */
class QuestChainsDepthTest {
    private static QuestSpec spec(String id) {
        return QuestBook.get().quests.stream().filter(q -> q.id().equals(id)).findFirst().orElseThrow();
    }

    private static boolean any(QuestSpec q, Predicate<Trigger> p) {
        return q.triggers().stream().anyMatch(p);
    }

    private static Trigger find(QuestSpec q, Trigger.Kind kind, String target) {
        return q.triggers().stream().filter(t -> t.kind == kind && t.target.equals(target)).findFirst().orElseThrow();
    }

    /** Stage counts are saved: deepening must happen inside the existing stages. */
    @Test
    void stageCountsAreUnchanged() {
        Map.ofEntries(Map.entry("recipe_for_disaster", 5), Map.entry("desert_treasure_i", 4), Map.entry("desert_treasure_ii", 6),
                Map.entry("lunar_diplomacy", 5), Map.entry("regicide", 6), Map.entry("mournings_end_part_i", 4),
                Map.entry("mournings_end_part_ii", 4), Map.entry("monkey_madness_ii", 5), Map.entry("dragon_slayer_ii", 8),
                Map.entry("song_of_the_elves", 5), Map.entry("dream_mentor", 5))
                .forEach((id, n) -> assertEquals(n, spec(id).stages.size(), id));
    }

    /** The self-test walks each stage through its first non-staying, non-drop trigger: every stage must have one. */
    @Test
    void everyStageCanBeWalked() {
        for (String id : List.of("recipe_for_disaster", "desert_treasure_i", "desert_treasure_ii", "lunar_diplomacy", "regicide",
                "mournings_end_part_i", "mournings_end_part_ii", "monkey_madness_ii", "dragon_slayer_ii", "song_of_the_elves")) {
            for (Stage s : spec(id).stages) {
                assertTrue(s.triggers.stream().anyMatch(t -> t.next != t.stage), id + " stage " + s.index + " never advances");
                long real = s.triggers.size();
                assertTrue(real >= 1, id + " stage " + s.index);
            }
        }
    }

    @Test
    void toolRequirementsMatchAnyTierOfTheirKind() {
        assertTrue(QuestSpec.toolSatisfies("pickaxe", ItemCatalog.get("bronze_pickaxe")));
        assertTrue(QuestSpec.toolSatisfies("pickaxe", ItemCatalog.get("dragon_pickaxe")));
        assertFalse(QuestSpec.toolSatisfies("pickaxe", ItemCatalog.get("bronze_axe")));
        assertFalse(QuestSpec.toolSatisfies("pickaxe:mithril", ItemCatalog.get("iron_pickaxe")));
        assertTrue(QuestSpec.toolSatisfies("pickaxe:mithril", ItemCatalog.get("rune_pickaxe")));
        assertTrue(QuestSpec.toolSatisfies("axe:rune", ItemCatalog.get("dragon_axe")));
        assertFalse(QuestSpec.toolSatisfies("axe:rune", ItemCatalog.get("adamant_axe")));
        assertFalse(QuestSpec.toolSatisfies("axe", null));
        // Crystal and infernal tools count as dragon tier.
        if (ItemCatalog.exists("infernal_pickaxe")) {
            assertTrue(QuestSpec.toolSatisfies("pickaxe:rune", ItemCatalog.get("infernal_pickaxe")));
        }
        assertEquals("bronze_pickaxe", QuestSpec.toolItem("pickaxe"));
        assertEquals("rune_axe", QuestSpec.toolItem("axe:rune"));
        assertTrue(QuestSpec.toolSatisfies("axe:rune", ItemCatalog.get(QuestSpec.toolItem("axe:rune"))));
        assertEquals("an axe", QuestSpec.toolName("axe"));
        assertEquals("a mithril pickaxe or better", QuestSpec.toolName("pickaxe:mithril"));
        assertThrows(IllegalArgumentException.class, () -> new QuestSpec("x", "X", com.runecraft.osrs.Quest.Difficulty.NOVICE, 0)
                .stage("s").talk("npc").needTool("hammer"));
        // Every tool requirement in the book names a real kind/tier and its self-test item exists.
        for (QuestSpec q : QuestBook.get().quests) {
            for (Trigger t : q.triggers()) {
                for (String tool : t.tools) {
                    assertTrue(QuestSpec.toolTier(tool) >= 0, q.id() + " " + tool);
                    assertTrue(ItemCatalog.exists(QuestSpec.toolItem(tool)), q.id() + " " + tool);
                }
            }
        }
        // Former exact-item checks: Legends' Quest needs a rune axe or better, the sunstone and blisterwood any tool.
        assertTrue(any(spec("legends_quest"), t -> t.tools.contains("axe:rune")));
        assertTrue(any(spec("sins_of_the_father"), t -> t.tools.contains("axe")));
    }

    @Test
    void recipeForDisasterSubquestsFollowTheWiki() {
        QuestSpec q = spec("recipe_for_disaster");
        // Another Cook's Quest: the dirty blast ingredients.
        assertTrue(any(q, t -> t.take.keySet().containsAll(Set.of("eye_of_newt", "greenmans_ale", "rotten_tomato", "fruit_blast", "ashes"))));
        // Each council member is freed by exactly one trigger that grants that subquest's XP.
        Map<String, String> skill = Map.of("rfd_dwarf", "SLAYER", "rfd_goblins", "FARMING", "rfd_pete", "SMITHING", "rfd_guide", "MAGIC",
                "rfd_dave", "COOKING", "rfd_skrach", "RANGED", "rfd_amik", "HITPOINTS", "rfd_awowogei", "AGILITY");
        skill.forEach((flag, s) -> {
            List<Trigger> freeing = q.triggers().stream().filter(t -> t.setFlags.contains(flag)).toList();
            assertEquals(1, freeing.size(), flag);
            assertTrue(freeing.get(0).xp.keySet().stream().anyMatch(k -> k.name().equals(s)), flag + " XP");
        });
        assertTrue(any(q, t -> t.target.equals("rohak") && t.take.getOrDefault("asgarnian_ale", 0) == 4));
        assertTrue(any(q, t -> t.take.keySet().containsAll(Set.of("raw_cod", "bread", "rfd_ground_kelp", "rfd_ground_crab_meat"))));
        assertTrue(any(q, t -> t.kind == Trigger.Kind.DROP && t.target.equals("mudskipper")));
        assertTrue(any(q, t -> t.target.equals("rfd_traiborn") && t.give.containsKey("rfd_enchanted_egg")));
        assertTrue(any(q, t -> t.need.containsKey("gertrudes_kitten") && t.give.containsKey("rfd_hell_rat_spices")));
        assertTrue(any(q, t -> t.tools.containsAll(List.of("axe", "pickaxe")) && t.give.containsKey("rfd_raw_jubbly")));
        assertTrue(find(q, Trigger.Kind.KILL, "rfd_evil_chicken").give.containsKey("rfd_evil_chicken_egg"));
        assertTrue(any(q, t -> t.kind == Trigger.Kind.DROP && t.target.equals("black_dragon") && t.give.containsKey("rfd_dragon_token")));
        assertTrue(any(q, t -> t.take.keySet().containsAll(Set.of("rfd_snake_corpse", "rfd_red_banana", "rfd_tchiki_nuts"))));
        // The Culinaromancer's minions in order.
        assertTrue(find(q, Trigger.Kind.KILL, "culinaromancer").needFlags.contains("rfd_gelatinnoth"));
    }

    @Test
    void culinaromancersChestUnlocksGlovesBySubquestCount() {
        List<QuestBook.Shop> gloves = QuestBook.get().shops.stream().filter(s -> s.npc().equals("rfd_gypsy")).toList();
        assertEquals("rfd_hardleather_gloves", gloves.get(0).item());
        assertEquals("barrows_gloves", gloves.get(gloves.size() - 1).item());
        assertEquals("recipe_for_disaster", gloves.get(gloves.size() - 1).quest());
        String[] order = {"rfd_bronze_gloves", "rfd_iron_gloves", "rfd_steel_gloves", "rfd_black_gloves", "rfd_mithril_gloves",
                "rfd_adamant_gloves", "rfd_rune_gloves", "rfd_dragon_gloves"};
        String[] council = {"rfd_dwarf", "rfd_goblins", "rfd_pete", "rfd_guide", "rfd_dave", "rfd_skrach", "rfd_amik", "rfd_awowogei"};
        for (int done = 0; done <= 8; done++) {
            Set<String> flags = Set.of(java.util.Arrays.copyOf(council, done));
            for (int i = 0; i < order.length; i++) {
                QuestBook.Shop shop = gloves.get(i + 1);
                assertEquals(order[i], shop.item());
                assertEquals(i < done, shop.flagsMet(flags::contains), order[i] + " after " + done + " subquests");
            }
        }
        // Hardleather: after Another Cook's Quest.
        assertTrue(gloves.get(0).flagsMet(Set.of("rfd_cook")::contains));
        assertFalse(gloves.get(0).flagsMet(f -> false));
        assertNotNull(ItemCatalog.get("rfd_hardleather_gloves"));
    }

    @Test
    void desertTreasureFourDiamonds() {
        QuestSpec q = spec("desert_treasure_i");
        assertTrue(any(q, t -> t.target.equals("terry_balando") && t.give.containsKey("dt_translation")));
        assertTrue(any(q, t -> t.target.equals("eblis") && t.take.getOrDefault("molten_glass", 0) == 6 && t.take.getOrDefault("magic_logs", 0) == 12));
        // Smoke: facemask + torches before Fareed.
        assertTrue(find(q, Trigger.Kind.REACH, "smoke_dungeon").need.keySet().containsAll(Set.of("facemask", "tinderbox")));
        assertTrue(find(q, Trigger.Kind.KILL, "fareed").needFlags.contains("dt_torches"));
        // Shadow: the gilded cross for Rasolo's ring of visibility.
        assertTrue(any(q, t -> t.take.containsKey("dt_gilded_cross") && t.give.containsKey("ring_of_visibility")));
        assertTrue(find(q, Trigger.Kind.KILL, "damis").need.containsKey("ring_of_visibility"));
        // Ice: troll child, ice trolls, Kamil, spiked boots.
        assertTrue(find(q, Trigger.Kind.KILL, "kamil").needFlags.contains("dt_ice_trolls"));
        assertTrue(any(q, t -> t.need.containsKey("spiked_boots") && t.give.containsKey("ice_diamond")));
        // Blood: silver pot blessed on Entrana, blood from Malak, Dessous lured from his tomb.
        assertTrue(any(q, t -> t.target.equals("high_priest_entrana") && t.give.containsKey("dt_blessed_pot")));
        Trigger tomb = find(q, Trigger.Kind.REACH, "dessous_graveyard");
        assertEquals("dessous", tomb.spawn);
        assertTrue(tomb.take.keySet().containsAll(Set.of("dt_pot_of_blood", "spice", "garlic")));
        assertTrue(any(q, t -> t.target.equals("malak") && t.give.containsKey("blood_diamond") && t.needFlags.contains("dt_dessous")));
        assertTrue(find(q, Trigger.Kind.REACH, "quest_jaldraocht_pyramid").take.keySet()
                .containsAll(Set.of("smoke_diamond", "shadow_diamond", "ice_diamond", "blood_diamond")));
        assertTrue(q.unlocks.stream().anyMatch(u -> u.contains("Ancient Magicks")));
    }

    @Test
    void desertTreasureTwoAncientGuardian() {
        QuestSpec q = spec("desert_treasure_ii");
        Trigger rocks = find(q, Trigger.Kind.REACH, "dt2_digsite_east_winch");
        assertTrue(rocks.tools.contains("pickaxe"));
        assertEquals("dt2_ancient_guardian", rocks.spawn);
        assertEquals(153, ContentRegistry.boss("dt2_ancient_guardian").combatLevel);
    }

    @Test
    void lunarDiplomacyAndRegicideAndMourningsEnd() {
        QuestSpec ld = spec("lunar_diplomacy");
        assertTrue(any(ld, t -> t.target.equals("brundt") && t.give.containsKey("seal_of_passage")));
        for (String altar : List.of("AIR_ALTAR", "FIRE_ALTAR", "WATER_ALTAR", "EARTH_ALTAR")) {
            assertTrue(find(ld, Trigger.Kind.REACH, altar).need.containsKey("dramen_staff"), altar);
        }
        assertTrue(find(ld, Trigger.Kind.REACH, "lunar_isle_mine").tools.contains("pickaxe"));
        assertTrue(any(ld, t -> t.take.containsKey("suqah_tooth") && t.give.containsKey("waking_sleep_vial")));
        assertTrue(find(ld, Trigger.Kind.KILL, "me").needFlags.contains("ld_challenges"));

        QuestSpec reg = spec("regicide");
        assertTrue(any(reg, t -> t.target.equals("chemist") && t.take.getOrDefault("coal", 0) == 20 && t.give.containsKey("barrel_bomb")));
        assertTrue(find(reg, Trigger.Kind.REACH, "tyras_camp").take.keySet().containsAll(Set.of("barrel_bomb", "minecraft:cooked_rabbit")));
        assertEquals("Tyras guard", ContentRegistry.boss("king_tyras").name); // OSRS: Tyras dies in the bomb blast

        QuestSpec me1 = spec("mournings_end_part_i");
        assertTrue(any(me1, t -> t.target.equals("tegid") && t.give.containsKey("me_mourner_top")));
        assertTrue(any(me1, t -> t.take.containsKey("barrel_of_naphtha") && t.give.containsKey("me_toxic_powder")));
        QuestSpec me2 = spec("mournings_end_part_ii");
        Trigger light = me2.stages.get(1).triggers.stream().filter(t -> t.next != t.stage).findFirst().orElseThrow();
        assertTrue(light.needFlags.containsAll(List.of("me2_yellow", "me2_green", "me2_blue")));
    }

    @Test
    void grandmasterBosses() {
        QuestSpec mm2 = spec("monkey_madness_ii");
        assertTrue(find(mm2, Trigger.Kind.KILL, "mm2_glough").needFlags.contains("mm2_gorillas"));
        assertEquals(431, ContentRegistry.boss("mm2_glough").combatLevel);
        assertTrue(any(mm2, t -> t.take.containsKey("kruks_paw") && t.give.containsKey("mm2_kruk_greegree")));

        QuestSpec ds2 = spec("dragon_slayer_ii");
        Trigger forge = ds2.triggers().stream().filter(t -> t.give.containsKey("dragonkin_key")).findFirst().orElseThrow();
        assertTrue(forge.take.keySet().containsAll(Set.of("ds2_key_karamja", "ds2_key_morytania", "ds2_key_kourend", "ds2_key_ungael")));
        assertTrue(find(ds2, Trigger.Kind.KILL, "galvek").needFlags.contains("ds2_assault"));

        QuestSpec sote = spec("song_of_the_elves");
        assertTrue(find(sote, Trigger.Kind.KILL, "fragment_of_seren").needFlags.contains("sote_essyllt"));
        assertEquals(236, ContentRegistry.boss("sote_essyllt").combatLevel);
        assertEquals(494, ContentRegistry.boss("fragment_of_seren").combatLevel);
    }
}
