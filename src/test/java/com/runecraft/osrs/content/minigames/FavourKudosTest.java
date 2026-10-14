package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.minigames.MinigameRules.BarbarianAssault;
import com.runecraft.osrs.content.systems.Diaries;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Barbarian Assault role levels, Tai Bwo Wannai favour and Varrock Museum kudos against the OSRS wiki. */
class FavourKudosTest {
    private static final String QUIZ = "osrs/museum/natural_history_quiz.tsv";

    @Test
    void barbarianAssaultRoleLevels() {
        assertEquals(0, BarbarianAssault.levelCost(1));
        assertEquals(200, BarbarianAssault.levelCost(2));
        assertEquals(300, BarbarianAssault.levelCost(3));
        assertEquals(400, BarbarianAssault.levelCost(4));
        assertEquals(500, BarbarianAssault.levelCost(5));
        assertEquals(0, BarbarianAssault.levelCost(6));
        // Wiki: 1,400 points for a level-5 role, 5,600 for all four; points cap at 5,000 per role.
        assertEquals(1400, BarbarianAssault.totalCost(5));
        assertEquals(5600, BarbarianAssault.totalCost(5) * BarbarianAssault.ROLES.length);
        assertTrue(BarbarianAssault.totalCost(5) <= BarbarianAssault.MAX_HONOUR);
        assertEquals(List.of(10, 15, 20, 25, 35), List.of(BarbarianAssault.healerHeal(1), BarbarianAssault.healerHeal(2),
                BarbarianAssault.healerHeal(3), BarbarianAssault.healerHeal(4), BarbarianAssault.healerHeal(5)));
        assertEquals(List.of(2, 4, 6, 7, 8), List.of(BarbarianAssault.collectorBag(1), BarbarianAssault.collectorBag(2),
                BarbarianAssault.collectorBag(3), BarbarianAssault.collectorBag(4), BarbarianAssault.collectorBag(5)));
        assertEquals(5, BarbarianAssault.attackerBonus(5));
        assertEquals(4, BarbarianAssault.defenderLure(1));
        assertEquals(5, BarbarianAssault.defenderLure(5));
        assertTrue(BarbarianAssault.allMaxed(new int[]{5, 5, 5, 5}));
        assertTrue(!BarbarianAssault.allMaxed(new int[]{5, 5, 4, 5}));
        assertNotNull(ContentRegistry.blocks().stream().filter(b -> b.id().equals(MinigameWorld.BA_BLACKBOARD)).findFirst().orElse(null));
    }

    @Test
    void taiBwoWannaiFavour() {
        // Wiki: light / medium / dense jungle need Woodcutting 10 / 20 / 35, give 32 / 55 / 80 XP and 0.3 / 0.5 / 1% favour.
        assertEquals(10, TaiBwoWannai.Jungle.LIGHT.level);
        assertEquals(35, TaiBwoWannai.Jungle.DENSE.level);
        assertEquals(55, TaiBwoWannai.Jungle.MEDIUM.xp);
        assertEquals(3, TaiBwoWannai.Jungle.LIGHT.favour);
        assertEquals(10, TaiBwoWannai.Jungle.DENSE.favour);
        assertEquals(65.1, TaiBwoWannai.Spar.DENSE.xp);
        assertEquals(1, TaiBwoWannai.Spar.LIGHT.favour);
        assertEquals(0, TaiBwoWannai.hackChance(TaiBwoWannai.Jungle.DENSE, 34, 3));
        assertTrue(TaiBwoWannai.hackChance(TaiBwoWannai.Jungle.LIGHT, 60, 3) > TaiBwoWannai.hackChance(TaiBwoWannai.Jungle.LIGHT, 60, 0));
        // Mod Ash: gout tuber 1/100 at level 10 scaling to 1/33 at 99; the event table is out of 99.
        assertEquals(0.01, TaiBwoWannai.goutTuberChance(1), 1e-12);
        assertEquals(1 / 33.0, TaiBwoWannai.goutTuberChance(99), 1e-12);
        int total = 0;
        for (TaiBwoWannai.Event e : TaiBwoWannai.Event.values()) {
            total += e.weight;
        }
        assertEquals(99, total);
        assertEquals(TaiBwoWannai.Event.JUNGLE_SPIDER, TaiBwoWannai.Event.roll(0));
        assertEquals(TaiBwoWannai.Event.BROODOO_DISEASE, TaiBwoWannai.Event.roll(0.999));
        assertNotNull(ContentRegistry.monster("jungle_spider"));
        // Favour is capped at 100%; 100 sticks cost about 10% favour, more with villager clothing.
        assertEquals(1000, TaiBwoWannai.addFavour(995, 10));
        assertEquals(List.of(100, 100), List.of(TaiBwoWannai.trade(1000, 0)[0], TaiBwoWannai.trade(1000, 0)[1]));
        assertEquals(125, TaiBwoWannai.trade(1000, 5)[0]);
        assertEquals(40, TaiBwoWannai.trade(40, 0)[0]);
        // Gabooty: villager robe 300, hat 240, sandals 120, opal machete 600 trading sticks.
        assertEquals(300, TaiBwoWannai.price(250));
        assertEquals(240, TaiBwoWannai.price(200));
        assertEquals(120, TaiBwoWannai.price(100));
        assertEquals(600, TaiBwoWannai.price(500));
        RewardShop shop = RewardShops.get(TaiBwoWannai.SHOP);
        assertNotNull(shop);
        assertEquals(4 * 5 + 3 + 4, shop.entries().size());
        shop.entries().forEach(e -> assertTrue(ItemCatalog.exists(e.item()), e.item()));
        for (String npc : TaiBwoWannai.SUPPLIERS) {
            assertNotNull(ContentRegistry.npc(npc), npc);
        }
        assertNotNull(ContentRegistry.quest(TaiBwoWannai.QUEST));
        assertNotNull(ContentRegistry.quest("my_arms_big_adventure"));
        TaiBwoWannai.MACHETES.forEach(m -> assertTrue(ItemCatalog.exists(m), m));
    }

    private static List<String> quizLines() throws IOException {
        Path p = Path.of("src/test/resources", QUIZ);
        if (Files.exists(p)) {
            return Files.readAllLines(p, StandardCharsets.UTF_8);
        }
        try (InputStream in = FavourKudosTest.class.getClassLoader().getResourceAsStream(QUIZ)) {
            assertNotNull(in, "missing " + QUIZ);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }

    @Test
    void naturalHistoryQuizMatchesTheWiki() throws IOException {
        List<String> mod = new ArrayList<>();
        for (VarrockMuseum.Question q : VarrockMuseum.QUIZ) {
            mod.add(q.display() + "|" + q.question() + "|" + String.join("|", q.answers()) + "|" + q.correct());
        }
        assertEquals(quizLines().stream().filter(l -> !l.isBlank()).toList(), mod);
        assertEquals(14, VarrockMuseum.DISPLAYS.size());
        for (int d = 0; d < VarrockMuseum.DISPLAYS.size(); d++) {
            assertEquals(6, VarrockMuseum.questions(d).size(), VarrockMuseum.DISPLAYS.get(d));
        }
        assertEquals(28, VarrockMuseum.DISPLAYS.size() * VarrockMuseum.KUDOS_PER_DISPLAY);
    }

    @Test
    void museumKudos() {
        // Historian Minas: 80 kudos, 18,000 lamp XP.
        assertEquals(80, VarrockMuseum.QUEST_KUDOS.stream().mapToInt(VarrockMuseum.QuestKudos::kudos).sum());
        assertEquals(18_000, VarrockMuseum.QUEST_KUDOS.stream().mapToInt(VarrockMuseum.QuestKudos::lampXp).sum());
        for (VarrockMuseum.QuestKudos q : VarrockMuseum.QUEST_KUDOS) {
            assertNotNull(ContentRegistry.quest(q.quest()), q.quest());
        }
        assertNotNull(ContentRegistry.quest(VarrockMuseum.DIG_SITE));
        // 28 + 50 + 80 = 158: enough for Orlando's 153 without fossils or relics.
        assertEquals(158, VarrockMuseum.modelledKudos());
        assertTrue(VarrockMuseum.modelledKudos() >= VarrockMuseum.ORLANDO_KUDOS);
        // Cleaning: uniques 3/51 each (necklace 1/51), common 38/51 split four ways; owned uniques reroll unremarkable.
        Map<String, Integer> counts = new HashMap<>();
        int n = 51 * 40;
        for (int i = 0; i < n; i++) {
            counts.merge(VarrockMuseum.cleanedFind((i + 0.5) / n, (i % 4 + 0.5) / 4, Set.of()), 1, Integer::sum);
        }
        assertEquals(3 * 40, counts.get("old_symbol"));
        assertEquals(40, counts.get("clean_necklace"));
        assertEquals(38 * 40, counts.get("pottery") + counts.get("museum_jewellery") + counts.get("old_chipped_vase")
                + counts.get("museum_arrowheads"));
        assertTrue(!VarrockMuseum.cleanedFind(0.01, 0.5, Set.of("old_symbol")).equals("old_symbol"));
        for (double r = 0; r < 1; r += 0.01) {
            String item = VarrockMuseum.cleanedFind(r, r, Set.of("old_symbol", "ancient_symbol", "old_coin", "ancient_coin",
                    "clean_necklace"));
            assertTrue(ItemCatalog.exists(item), item);
        }
        assertEquals(195, VarrockMuseum.CRATE.stream().mapToInt(VarrockMuseum.CrateLoot::weight).sum() + VarrockMuseum.CRATE_LAMP_WEIGHT);
        assertEquals(null, VarrockMuseum.crateRoll(0.999));
        assertEquals("coins", VarrockMuseum.crateRoll(0).item());
        VarrockMuseum.ARTEFACTS.forEach(a -> assertTrue(ItemCatalog.exists(a), a));
        assertNotNull(ContentRegistry.npc("historian_minas"));
        assertNotNull(ContentRegistry.npc("curator_haig_halen"));
    }

    @Test
    void diaryTasksUseTheNewActivities() {
        for (String key : List.of("kingdom_approval:100", "ba_blackboard:level5", "tbw_favour:100", "kudos:haig_halen_50",
                "kudos:orlando_153", "ba_wave")) {
            List<Diaries.Task> tasks = Diaries.tasks().stream()
                    .filter(t -> t.type() == Diaries.Type.EVENT && Diaries.eventMatches(t.target(), key)).toList();
            assertEquals(1, tasks.size(), key);
            assertTrue(!Diaries.isApprox(tasks.get(0)), key);
        }
    }
}
