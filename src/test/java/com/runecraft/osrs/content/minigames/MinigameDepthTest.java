package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.minigames.TempleTrekking.Event;
import com.runecraft.osrs.content.minigames.TempleTrekking.Route;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Round 28 minigame depth: Temple Trekking's boat event and tomes, Mort'ton's serums, coffins and bag, garden hedges. */
class MinigameDepthTest {
    @Test
    void boatEventOnlyOnRouteThree() {
        assertEquals(0, Event.BOAT.weight(Route.ROUTE_1));
        assertEquals(0, Event.BOAT.weight(Route.ROUTE_2));
        assertTrue(Event.BOAT.weight(Route.ROUTE_3) > 0);
        Random rng = new Random(7);
        boolean sawBoat = false;
        for (int i = 0; i < 2000; i++) {
            for (Route r : Route.values()) {
                List<Event> events = TempleTrekking.rollEvents(r, rng::nextDouble);
                if (r != Route.ROUTE_3) {
                    assertFalse(events.contains(Event.BOAT), "boat on " + r);
                } else {
                    sawBoat |= events.contains(Event.BOAT);
                }
            }
        }
        assertTrue(sawBoat);
        assertEquals(TempleTrekking.SWAMP_HEAD, Event.BOAT.creature(Route.ROUTE_3));
        // Wiki: Head level 140, 150 HP, max hit 20; Tentacle level 99, 75 HP, max hit 12.
        assertEquals(150, ContentRegistry.monster(TempleTrekking.SWAMP_HEAD).hitpoints);
        assertEquals(140, ContentRegistry.monster(TempleTrekking.SWAMP_HEAD).combatLevel);
        assertEquals(20, ContentRegistry.monster(TempleTrekking.SWAMP_HEAD).attacks.get(0).maxHit());
        assertEquals(75, ContentRegistry.monster(TempleTrekking.SWAMP_TENTACLE).hitpoints);
        assertEquals(12, ContentRegistry.monster(TempleTrekking.SWAMP_TENTACLE).attacks.get(0).maxHit());
        assertTrue(TempleTrekking.headHeals(10, 150, 1));
        assertFalse(TempleTrekking.headHeals(10, 150, 0));
        assertFalse(TempleTrekking.headHeals(100, 150, 4));
    }

    @Test
    void tomesAreItems() {
        assertEquals(7, TempleTrekking.TOME_SKILLS.size());
        for (Skill s : TempleTrekking.TOME_SKILLS) {
            for (Route r : Route.values()) {
                String id = TempleTrekking.tomeId(s, r);
                assertNotNull(ItemCatalog.get(id), id);
                assertEquals(5000, ItemCatalog.get(id).value(), id);
                Object[] back = TempleTrekking.tome(id);
                assertEquals(s, back[0]);
                assertEquals(r, back[1]);
            }
        }
        assertEquals("agility_tome_blue", TempleTrekking.tomeId(Skill.AGILITY, Route.ROUTE_1));
        assertEquals("slayer_tome_red", TempleTrekking.tomeId(Skill.SLAYER, Route.ROUTE_3));
        assertNull(TempleTrekking.tome("reward_token_red"));
    }

    @Test
    void mortonSerumsAndCures() {
        int total207 = ShadesOfMortton.CURE_207.stream().mapToInt(ShadesOfMortton.CureReward::weight).sum();
        int total208 = ShadesOfMortton.CURE_208.stream().mapToInt(ShadesOfMortton.CureReward::weight).sum();
        assertEquals(256, total207);
        assertEquals(128, total208);
        for (List<ShadesOfMortton.CureReward> table : List.of(ShadesOfMortton.CURE_207, ShadesOfMortton.CURE_208)) {
            for (ShadesOfMortton.CureReward l : table) {
                assertNotNull(ItemCatalog.get(l.item()), l.item());
                assertTrue(l.min() >= 1 && l.max() >= l.min(), l.item());
                assertTrue(l.doses() == 0 || l.item().equals("olive_oil") && l.doses() <= 4, l.item());
            }
        }
        // Wiki: serum 207's commonest reward is coins (59/256); serum 208 can give bronze keys.
        assertEquals("coins", ShadesOfMortton.cure(ShadesOfMortton.CURE_207, 0.0).item());
        assertTrue(ShadesOfMortton.CURE_208.stream().anyMatch(c -> c.item().startsWith("bronze_shade_key")));
        assertNotNull(ItemCatalog.get(ShadesOfMortton.SERUM_208));
        assertEquals(20.0, ShadesOfMortton.SERUM_MIN_SANCTITY);
        assertEquals(5.0, ShadesOfMortton.SANCTITY_PER_SERUM_DOSE);
        // Diary bonuses: +50% Prayer (hard) and +50% Firemaking (elite) when cremating.
        assertEquals(150.0, ShadesOfMortton.cremationPrayerXp(100, true));
        assertEquals(100.0, ShadesOfMortton.cremationPrayerXp(100, false));
        assertEquals(750.0, ShadesOfMortton.cremationFiremakingXp(500, true));
        // Flamtaer bracelet: about 75% more sanctity per repair.
        assertEquals(ShadesOfMortton.SANCTITY_PER_REPAIR * 1.75, ShadesOfMortton.repairSanctity(true), 1e-9);
        assertEquals(ShadesOfMortton.SANCTITY_PER_REPAIR, ShadesOfMortton.repairSanctity(false), 1e-9);
        assertEquals(500, ShadesOfMortton.BAG_CAPACITY[2]);
        for (String afflicted : ShadesOfMortton.AFFLICTED) {
            assertTrue(MinigameWorld.npcs().stream().anyMatch(n -> n.id().equals(afflicted)), afflicted);
        }
    }

    @Test
    void dampesCoffins() {
        int[] capacity = {3, 8, 14, 20, 28};
        for (ShadesOfMortton.Coffin c : ShadesOfMortton.Coffin.values()) {
            assertEquals(capacity[c.ordinal()], c.capacity);
            assertNotNull(ItemCatalog.get(c.id()), c.id());
            assertNotNull(ItemCatalog.get(c.locks()), c.locks());
        }
        assertNotNull(ItemCatalog.get(ShadesOfMortton.BROKEN_COFFIN));
        assertEquals(ShadesOfMortton.Coffin.STEEL, ShadesOfMortton.upgrade(ShadesOfMortton.BROKEN_COFFIN, Set.of("steel_locks")));
        assertEquals(ShadesOfMortton.Coffin.GOLD, ShadesOfMortton.upgrade("bronze_coffin", Set.of("steel_locks", "gold_locks")));
        assertNull(ShadesOfMortton.upgrade("silver_coffin", Set.of("steel_locks")));
        assertNull(ShadesOfMortton.upgrade("logs", Set.of("gold_locks")));
        assertTrue(MinigameWorld.placeBlocks().get(Minigame.SHADES_OF_MORTTON.place()).contains(MinigameWorld.DAMPE_COFFINS));
        assertTrue(MinigameWorld.placeBlocks().get(Minigame.NIGHTMARE_ZONE.place()).contains(MinigameWorld.NMZ_BARRELS));
    }

    @Test
    void elementalsCantSeeThroughHedges() {
        for (SorceressGarden.Season s : SorceressGarden.Season.values()) {
            // The open corridor never blocks a view along it.
            assertTrue(SorceressGarden.clearLine(s, 1, 0, 3, 0));
            int blocked = 0;
            for (int u = 1; u <= SorceressGarden.CORRIDOR; u++) {
                if (!SorceressGarden.walkable(s, u, 2)) {
                    // A hedge square two off the corridor hides the square beyond it from the corridor.
                    assertFalse(SorceressGarden.clearLine(s, u, 0, u, 4), s + " sees through the hedge at " + u);
                    blocked++;
                }
            }
            assertTrue(blocked > 0, s + " has no hedge beside the corridor");
        }
    }
}
