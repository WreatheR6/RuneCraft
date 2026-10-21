package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.NpcDef;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Mortimer of Wyrmscraig (OSRS Wiki "Mortimer" and "Slayer Master", July 2026). */
class MortimerTest {
    @Test
    void masterAndTable() {
        SlayerMasters.Master m = SlayerMasters.get(Mortimer.ID);
        assertNotNull(m);
        assertEquals(100, m.combatLevel());
        assertEquals(70, m.slayerLevel());
        assertEquals(0, m.points(), "no points but through a modifier");
        assertEquals(Mortimer.QUEST, m.quest());
        assertNotNull(ContentRegistry.quest(Mortimer.QUEST));
        assertEquals(28, m.assignments().size(), "the wiki's 29 tasks without venators");
        assertNull(m.assignment("venators"));
        SlayerMasters.Assignment hydras = m.assignment("hydras");
        assertEquals(10, hydras.weight());
        assertEquals(150, hydras.min());
        assertEquals(200, hydras.max());
        SlayerMasters.Assignment nechs = m.assignment("nechryael");
        assertEquals(8, nechs.weight());
        assertEquals(200, nechs.extMin());
        assertEquals(List.of("priest_in_peril"), m.assignment("abyssal_demons").quests(), "as at Duradel");
        for (SlayerMasters.Assignment a : m.assignments()) {
            assertNotNull(SlayerCategories.get(a.category()), a.category());
            // every one of his tasks has a superior (gryphons and aquanites aren't in the game yet; the warped creatures'
            // mutated superiors are missing)
            if (SlayerTasks.available(a.category()) && !a.category().equals("warped_creatures")) {
                assertTrue(SlayerTasks.creatures(a.category()).stream().anyMatch(c -> Superiors.forBase(c.id) != null),
                        a.category() + " has no superior");
            }
            if (a.extMin() > 0) {
                assertNotNull(SlayerRewards.extensionFor(a.category()), a.category());
            }
        }
        NpcDef npc = ContentRegistry.npc(Mortimer.ID);
        assertNotNull(npc);
        assertNotNull(ContentRegistry.place(npc.place()), npc.place());
    }

    @Test
    void choicesModifiersAndCosts() {
        assertEquals(2, Mortimer.choices(0));
        assertEquals(2, Mortimer.choices(99));
        assertEquals(3, Mortimer.choices(100));
        assertEquals(List.of(Mortimer.Modifier.POINTS, Mortimer.Modifier.QUANTITY), Mortimer.unlocked(0));
        assertTrue(Mortimer.unlocked(50).contains(Mortimer.Modifier.XP));
        assertEquals(75, Mortimer.amount(100, Mortimer.Modifier.QUANTITY, 25));
        assertEquals(100, Mortimer.amount(100, Mortimer.Modifier.POINTS, 25));
        assertEquals(1, Mortimer.amount(1, Mortimer.Modifier.QUANTITY, 30));
        assertEquals(110.0, Mortimer.xp(100, Mortimer.Modifier.XP, 10), 1e-9);
        assertEquals(100.0, Mortimer.xp(100, Mortimer.Modifier.QUANTITY, 10), 1e-9);
        assertEquals(20, Mortimer.points(Mortimer.Modifier.POINTS, 20));
        assertEquals(0, Mortimer.points(Mortimer.Modifier.XP, 20));
        assertEquals(100, SlayerRewards.skipCost(Mortimer.ID));
        assertEquals(SlayerRewards.SKIP_COST, SlayerRewards.skipCost("duradel"));
        assertEquals(120, SlayerRewards.blockCost(Mortimer.ID));
        assertEquals(2, SlayerRewards.blockSlots(Mortimer.ID, 300, true));
        assertEquals(7, SlayerRewards.blockSlots("duradel", 300, true));
        assertEquals(1, SlayerRewards.blockSlots(Mortimer.ID, 50, false));
    }

    @Test
    void offersAreDistinctAndModified() {
        Random random = new Random(7);
        for (int i = 0; i < 200; i++) {
            int done = i % 2 == 0 ? 0 : 120;
            List<Mortimer.Offer> offers = Mortimer.offers(random, a -> true, Set.of(), done, a -> false);
            assertEquals(Mortimer.choices(done), offers.size());
            Set<String> seen = new HashSet<>();
            for (Mortimer.Offer o : offers) {
                assertTrue(seen.add(o.category()), "offered twice: " + o.category());
                assertTrue(Mortimer.unlocked(done).contains(o.modifier()));
                assertTrue(o.value() >= o.modifier().min() && o.value() <= o.modifier().max());
                SlayerMasters.Assignment a = SlayerMasters.get(Mortimer.ID).assignment(o.category());
                int lo = Mortimer.amount(a.min(), o.modifier(), o.value());
                assertTrue(o.amount() >= lo && o.amount() <= a.max(), o.toString());
            }
        }
        // only what the player may be given
        List<Mortimer.Offer> only = Mortimer.offers(random, a -> a.category().equals("hydras"), Set.of(), 120, a -> false);
        assertEquals(1, only.size());
        assertFalse(Mortimer.offers(random, a -> false, Set.of(), 0, a -> false).iterator().hasNext());
    }
}
