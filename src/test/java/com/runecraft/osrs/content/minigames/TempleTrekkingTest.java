package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.minigames.TempleTrekking.Difficulty;
import com.runecraft.osrs.content.minigames.TempleTrekking.Event;
import com.runecraft.osrs.content.minigames.TempleTrekking.EventType;
import com.runecraft.osrs.content.minigames.TempleTrekking.Follower;
import com.runecraft.osrs.content.minigames.TempleTrekking.Reward;
import com.runecraft.osrs.content.minigames.TempleTrekking.Route;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TempleTrekkingTest {
    @Test
    void rewardFormulaMatchesTheWikiExamples() {
        // 500 points on route 2: 150 + floor(100 * 175 / 7675) = 152 bow strings, 66 + 2 = 68 coal and iron ore.
        assertEquals(152, TempleTrekking.rewardCount(Reward.BOW_STRING.low(Route.ROUTE_2), Reward.BOW_STRING.high(Route.ROUTE_2), 500));
        List<MinigameReward> ore = TempleTrekking.reward(Reward.ORE, Route.ROUTE_2, 500, false);
        assertEquals(68, ore.stream().mapToInt(MinigameReward::amount).sum());
        assertEquals(new MinigameReward("iron_ore", 22), ore.get(1));
        // The maximum: 8,000 points on route 3 gives 660 pure essence, 144 coal and 72 iron, 20 + 10 herbs.
        assertEquals(List.of(new MinigameReward("pure_essence", 660)), TempleTrekking.reward(Reward.PURE_ESSENCE, Route.ROUTE_3, 8000, false));
        assertEquals(List.of(new MinigameReward("coal", 144), new MinigameReward("iron_ore", 72)),
                TempleTrekking.reward(Reward.ORE, Route.ROUTE_3, 9999, false));
        assertEquals(List.of(new MinigameReward("grimy_harralander", 20), new MinigameReward("grimy_toadflax", 10)),
                TempleTrekking.reward(Reward.HERBS, Route.ROUTE_3, 8000, true));
        // No points still counts as 325: the low end.
        assertEquals(50, TempleTrekking.rewardCount(50, 150, 0));
        assertEquals(325, TempleTrekking.clampPoints(0));
        assertEquals(8000, TempleTrekking.clampPoints(12_000));
        ContentRegistry.monsters();
        for (Reward r : Reward.values()) {
            for (MinigameReward m : TempleTrekking.reward(r, Route.ROUTE_1, 4000, false)) {
                assertTrue(ItemCatalog.exists(m.item()), m.item());
            }
        }
    }

    @Test
    void routesScoreAndPayByTheWiki() {
        assertEquals(325, Route.ROUTE_1.killPoints);
        assertEquals(625, Route.ROUTE_2.killPoints);
        assertEquals(1125, Route.ROUTE_3.killPoints);
        assertEquals(1100, Route.ROUTE_1.tomeMin);
        assertEquals(1650, Route.ROUTE_1.tomeMax);
        assertEquals(5005, Route.ROUTE_3.tomeMax);
        Random rng = new Random(7);
        for (Route r : Route.values()) {
            for (int i = 0; i < 200; i++) {
                int xp = TempleTrekking.tomeXp(r, rng::nextDouble);
                assertTrue(xp >= r.tomeMin && xp <= r.tomeMax, r + " " + xp);
                assertTrue(TempleTrekking.rollEvents(r, rng::nextDouble).size() <= r.slots);
            }
        }
        assertEquals(9, Route.ROUTE_1.slots);
        assertEquals(4, Route.ROUTE_2.slots);
        assertEquals(5, Route.ROUTE_3.slots);
        // Evading: always on route 1, after half the monsters on route 2, never on route 3.
        assertTrue(Route.ROUTE_1.canEvade(0, 4));
        assertFalse(Route.ROUTE_2.canEvade(1, 4));
        assertTrue(Route.ROUTE_2.canEvade(2, 4));
        assertFalse(Route.ROUTE_3.canEvade(5, 5));
        assertEquals(500, Event.BRIDGE.points());
        assertEquals(0, Event.LUMBERJACK_BRIDGE.points());
        assertEquals(0, Event.ABIDOR_CRANK.points());
    }

    @Test
    void followersSetTheNumberOfMonsters() {
        Random rng = new Random(3);
        assertEquals(3, Difficulty.EASY.monsters(rng::nextDouble));
        assertEquals(4, Difficulty.MEDIUM.monsters(rng::nextDouble));
        for (int i = 0; i < 100; i++) {
            int n = Difficulty.HARD.monsters(rng::nextDouble);
            assertTrue(n == 5 || n == 6, "" + n);
        }
        int trek = 0;
        int ramble = 0;
        for (Follower f : Follower.values()) {
            if (f.ramble) {
                ramble++;
            } else {
                trek++;
            }
            assertNotNull(ContentRegistry.npc(f.npc()), f.npc());
            assertEquals(f, Follower.byId(f.id()));
        }
        assertEquals(6, trek);
        assertEquals(6, ramble);
        assertEquals(12, TempleTrekking.abidorDefenceBoost(99)); // "up to 12"
    }

    @Test
    void eventCreaturesHaveTheWikiStats() {
        for (Event e : Event.values()) {
            for (Route r : Route.values()) {
                String c = e.creature(r);
                if (c != null) {
                    assertNotNull(ContentRegistry.monster(c), c);
                }
            }
            if (e.kind == EventType.COMBAT) {
                assertNotNull(e.creature);
            }
        }
        // Levels per route (osrs-dps-calc / wiki Temple Trekking variants).
        assertEquals(59, ContentRegistry.monster("trek_vampyre_juvinate_1").combatLevel);
        assertEquals(119, ContentRegistry.monster("trek_vampyre_juvinate_3").combatLevel);
        MonsterDef ghast = ContentRegistry.monster("trek_ghast_3");
        assertEquals(139, ghast.combatLevel);
        assertEquals(160, ghast.hitpoints);
        assertEquals(8, ghast.attackSpeed);
        assertEquals(12, ghast.attacks.get(0).maxHit());
        assertEquals(18, ContentRegistry.monster("trek_swamp_snake_3").attacks.get(0).maxHit());
        assertEquals(17, ContentRegistry.monster("trek_giant_snail_3").attacks.get(0).maxHit());
        assertEquals(140, ContentRegistry.monster("trek_shade_3").combatLevel);
        assertEquals("Riyl shadow", ContentRegistry.monster("trek_shade_1").name);
        for (Difficulty d : Difficulty.values()) {
            assertNotNull(ContentRegistry.monster(d.creature()), d.creature());
        }
    }

    @Test
    void placesItemsAndRequirements() {
        ContentRegistry.monsters();
        for (String item : TempleTrekking.LUMBERJACK) {
            assertTrue(ItemCatalog.exists(item), item);
        }
        for (Route r : Route.values()) {
            assertTrue(ItemCatalog.exists(r.token), r.token);
            assertEquals(r, TempleTrekking.tokenRoute(r.token));
        }
        assertNotNull(ContentRegistry.place(TempleTrekking.PLACE));
        assertNotNull(ContentRegistry.place(TempleTrekking.RAMBLE_PLACE));
        assertNotNull(ContentRegistry.quest(TempleTrekking.QUEST));
        assertNotNull(ContentRegistry.quest(TempleTrekking.RAMBLE_QUEST));
        assertTrue(MinigameWorld.placeBlocks().get(TempleTrekking.RAMBLE_PLACE).contains(TempleTrekking.RAMBLE_ENTRANCE));
    }
}
