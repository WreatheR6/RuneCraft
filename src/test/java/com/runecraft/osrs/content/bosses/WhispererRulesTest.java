package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.data.Untradeables;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Whisperer's Shadow Realm and sanity (OSRS Wiki "Sanity", "The Whisperer/Strategies", "Blackstone fragment"). */
class WhispererRulesTest {
    @Test
    void sanityRestoresOutsideOnePercentEveryFourTicks() {
        WhispererRules.Sanity s = new WhispererRules.Sanity();
        s.drain(20);
        for (int t = 0; t < 8; t++) {
            assertEquals(0, s.tick(false, false, false));
        }
        assertEquals(82, s.value());
        // awakened: sanity never comes back outside the realm
        WhispererRules.Sanity a = new WhispererRules.Sanity();
        a.drain(20);
        for (int t = 0; t < 40; t++) {
            a.tick(false, false, true);
        }
        assertEquals(80, a.value());
    }

    @Test
    void theRealmDrainsThreeAwakenedFourButNotInTheEnrage() {
        WhispererRules.Sanity s = new WhispererRules.Sanity();
        s.tick(true, false, false);
        assertEquals(97, s.value());
        WhispererRules.Sanity a = new WhispererRules.Sanity();
        a.tick(true, false, true);
        assertEquals(96, a.value());
        WhispererRules.Sanity e = new WhispererRules.Sanity();
        for (int t = 0; t < 50; t++) {
            e.tick(true, true, false);
        }
        assertEquals(100, e.value(), "only her tentacles drain sanity during the enrage");
        assertEquals(0, e.realmTicks());
    }

    /** At 0%: 5, 10, 20, 40, 80, 160 every 2 ticks (at 2, 4, 6... ticks): the sixth kills. */
    @Test
    void insanityDoublesEveryTwoTicks() {
        WhispererRules.Sanity s = new WhispererRules.Sanity();
        s.drain(100);
        assertTrue(s.insane());
        int[] dealt = new int[12];
        for (int t = 0; t < 12; t++) {
            dealt[t] = s.tick(false, true, true);
        }
        assertEquals(List.of(0, 5, 0, 10, 0, 20, 0, 40, 0, 80, 0, 160),
                java.util.Arrays.stream(dealt).boxed().toList());
        s.restore(WhispererRules.SANITAS_RESTORE);
        assertFalse(s.insane());
        s.drain(100);
        assertEquals(0, s.tick(false, true, true));
        assertEquals(5, s.tick(false, true, true), "the doubling starts over");
    }

    @Test
    void realmOnlyRules() {
        assertTrue(WhispererRules.canHitSoul(true));
        assertFalse(WhispererRules.canHitSoul(false), "souls can only be damaged in the Shadow Realm");
        assertTrue(WhispererRules.fragmentGlows(true, false));
        assertFalse(WhispererRules.fragmentGlows(false, false));
        assertFalse(WhispererRules.fragmentGlows(true, true), "the enrage pulls everyone in");
        assertEquals(0.3, WhispererRules.specialDamage(false), 1e-9);
        assertEquals(0.4, WhispererRules.specialDamage(true), 1e-9);
    }

    /** Two layouts, two light patterns each; every pattern symmetric across both axes; light and dark seeds in each. */
    @Test
    void seedLayoutsAreSymmetric() {
        assertEquals(2, WhispererRules.seedLayouts());
        for (int layout = 0; layout < 2; layout++) {
            Set<String> tiles = new HashSet<>();
            for (int pattern = 0; pattern < 2; pattern++) {
                List<WhispererRules.Seed> seeds = WhispererRules.seeds(layout, pattern);
                Set<String> light = new HashSet<>();
                Set<String> all = new HashSet<>();
                for (WhispererRules.Seed s : seeds) {
                    all.add(s.dx() + "," + s.dz());
                    if (s.light()) {
                        light.add(s.dx() + "," + s.dz());
                    }
                }
                assertEquals(seeds.size(), all.size(), "no tile twice");
                assertTrue(!light.isEmpty() && light.size() < all.size());
                for (WhispererRules.Seed s : seeds) {
                    if (s.light()) {
                        assertTrue(light.contains(-s.dx() + "," + s.dz()) && light.contains(s.dx() + "," + -s.dz()));
                    }
                }
                if (pattern == 0) {
                    tiles.addAll(all);
                } else {
                    assertEquals(tiles, all, "a layout's patterns share its tiles");
                }
            }
        }
    }

    @Test
    void theFragmentExistsAndIsUntradeable() {
        assertNotNull(ItemCatalog.get(WhispererRules.FRAGMENT));
        assertTrue(Untradeables.untradeable(WhispererRules.FRAGMENT));
        assertTrue(ContentRegistry.places().stream().anyMatch(p -> p.id().equals("lassar_undercity")
                && p.blocks().contains(WhispererRules.WORKBENCH)), "Ketla's workbench stands in the Lassar Undercity");
    }
}
