package com.runecraft.osrs;

import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;
import com.runecraft.osrs.world.SpawnTable;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImpCatcherBeadsTest {
    private static MonsterDef imp() {
        for (MonsterDef m : ContentRegistry.monsters()) {
            if (m.id.equals(ImpCatcherBeads.IMP)) {
                return m;
            }
        }
        return null;
    }

    @Test
    void impsAreTheBeadCarriers() {
        MonsterDef imp = imp();
        assertNotNull(imp, "imp monster must exist");
        assertTrue(ImpCatcherBeads.carriesBeads(imp.id, imp.name));
        assertFalse(ImpCatcherBeads.carriesBeads("goblin", "Goblin"));
        assertFalse(ImpCatcherBeads.carriesBeads("dark_wizard", "Dark wizard"));
        Set<String> dropped = new HashSet<>();
        for (Drop d : imp.drops) {
            dropped.add(d.item());
        }
        assertTrue(dropped.containsAll(ImpCatcherBeads.BEADS), "imps drop all four beads like OSRS: " + dropped);
        for (String bead : ImpCatcherBeads.BEADS) {
            assertTrue(ItemCatalog.get(bead) != null, bead + " must be an item");
        }
    }

    @Test
    void impsCanBeFoundInTheWorld() {
        assertTrue(SpawnTable.all().stream().anyMatch(p -> p.monster().equals(ImpCatcherBeads.IMP)),
                "imps need spawn points so the quest can be completed");
    }

    @Test
    void questDropIsAlwaysAMissingBead() {
        Set<String> held = new HashSet<>(List.of("black_bead", "white_bead"));
        for (int pick = -5; pick < 10; pick++) {
            String bead = ImpCatcherBeads.questDrop(held::contains, 0, pick);
            assertNotNull(bead);
            assertFalse(held.contains(bead), "dropped a bead the player already has: " + bead);
        }
        assertNull(ImpCatcherBeads.questDrop(held::contains, 1, 0), "only on a successful roll");
        assertEquals(List.of("red_bead", "yellow_bead"), ImpCatcherBeads.missing(held::contains));
    }

    @Test
    void questCanBeCompletedByKillingImps() {
        Set<String> held = new HashSet<>();
        java.util.Random random = new java.util.Random(7);
        int kills = 0;
        while (held.size() < 4 && kills < 1000) {
            kills++;
            String bead = ImpCatcherBeads.questDrop(held::contains, random.nextInt(ImpCatcherBeads.QUEST_DROP_ONE_IN), random.nextInt(4));
            if (bead != null) {
                held.add(bead);
            }
        }
        assertEquals(new HashSet<>(ImpCatcherBeads.BEADS), held);
        assertTrue(kills < 40, "took " + kills + " kills");
        assertNull(ImpCatcherBeads.questDrop(held::contains, 0, 0), "no more quest beads once all four are held");
    }

    @Test
    void journalPointsAtImps() {
        assertTrue(Quest.IMP_CATCHER.journal().stream().anyMatch(s -> s.toLowerCase(java.util.Locale.ROOT).contains("imp")));
        assertFalse(Quest.IMP_CATCHER.journal().stream().anyMatch(s -> s.contains("goblins may have")));
    }
}
