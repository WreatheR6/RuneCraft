package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.CollectionLogData;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.data.Drop;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Awakened Desert Treasure II bosses and the Doom of Mokhaiotl's delve levels against the OSRS Wiki. */
class BossVariantsTest {
    private static int rate(BossDef b, String item) {
        return b.drops.stream().filter(d -> d.item().equals(item)).findFirst().orElseThrow().rarity();
    }

    @Test
    void dt2DropRatesFollowTheWiki() {
        BossDef duke = ContentRegistry.boss("duke_sucellus");
        // Unique table 1/90: vestige 1/720, chromium 3/720, the Eye 1/720, each virtus piece 1/2160; orb 1/48.
        assertEquals(720, rate(duke, "magus_vestige"));
        assertEquals(240, rate(duke, "chromium_ingot"));
        assertEquals(720, rate(duke, "eye_of_the_duke"));
        assertEquals(2160, rate(duke, "virtus_mask"));
        assertEquals(48, rate(duke, AwakenedBosses.ORB));
        BossDef vard = ContentRegistry.boss("vardorvis");
        assertEquals(1088, rate(vard, "ultor_vestige"));
        assertEquals(80, rate(vard, AwakenedBosses.ORB));
        assertEquals(53, rate(ContentRegistry.boss("the_leviathan"), AwakenedBosses.ORB));
        assertEquals(512, rate(ContentRegistry.boss("the_whisperer"), "sirens_staff"));
        // The orb is a collection log item.
        assertTrue(CollectionLogData.pages().stream().anyMatch(p -> p.name().equals("Duke Sucellus") && p.items().contains(AwakenedBosses.ORB)));
    }

    @Test
    void awakenedVariants() {
        for (String id : AwakenedBosses.BOSSES) {
            BossDef b = ContentRegistry.boss(id);
            BossVariant v = AwakenedBosses.variant(b);
            assertTrue(v.awakened());
            assertEquals(0, v.delve());
            assertEquals(id + "_awakened", v.kcKey());
            assertTrue(v.hitpoints() > b.hitpoints && v.combatLevel() > 1000, id);
            // Three times the unique-table chance; everything else unchanged.
            for (Drop d : v.drops()) {
                int base = rate(b, d.item());
                if (AwakenedBosses.UNIQUE_TABLE.contains(d.item())) {
                    assertEquals(Math.round(base / 3f), d.rarity(), id + " " + d.item());
                } else {
                    assertEquals(base, d.rarity(), id + " " + d.item());
                }
            }
            for (String attack : AwakenedBosses.maxHits(id).keySet()) {
                // (the Whisperer's screech is scripted, not in her attack list)
                assertTrue(attack.equals("screech") || b.attacks.stream().anyMatch(a -> a.name().equals(attack)), id + " has no attack " + attack);
            }
        }
        BossVariant duke = AwakenedBosses.variant(ContentRegistry.boss("duke_sucellus"));
        assertEquals(1099, duke.combatLevel());
        assertEquals(1697, duke.hitpoints());
        assertEquals(81, duke.maxHit(Dt2Bosses.DUKE_SLAM));
        assertEquals(240, duke.drops().stream().filter(d -> d.item().equals("magus_vestige")).findFirst().orElseThrow().rarity());
        assertEquals(2700, AwakenedBosses.variant(ContentRegistry.boss("the_leviathan")).hitpoints());
        assertEquals(96, AwakenedBosses.variant(ContentRegistry.boss("vardorvis")).maxHit(Dt2Bosses.VARD_AXES));
        assertNotNull(ItemCatalog.get(AwakenedBosses.BLOOD_KIT));
    }

    @Test
    void bloodKitGoesToTheLastOfTheFour() {
        Map<String, Integer> kc = new HashMap<>();
        kc.put("duke_sucellus_awakened", 1);
        kc.put("the_leviathan_awakened", 1);
        kc.put("the_whisperer_awakened", 3);
        assertFalse(AwakenedBosses.earnsBloodKit("the_whisperer", kc));
        kc.put("vardorvis_awakened", 1);
        assertTrue(AwakenedBosses.earnsBloodKit("vardorvis", kc));
        kc.put("vardorvis_awakened", 2);
        assertFalse(AwakenedBosses.earnsBloodKit("vardorvis", kc));
    }

    @Test
    void delveLevelsFollowTheWiki() {
        BossDef doom = ContentRegistry.boss(DoomDelves.BOSS);
        int[] hp = {525, 550, 575, 600, 625, 650, 650, 675, 625, 625};
        for (int lvl = 1; lvl <= 10; lvl++) {
            BossVariant v = DoomDelves.variant(doom, lvl);
            assertEquals(lvl, v.delve());
            assertEquals(hp[lvl - 1], v.hitpoints(), "delve " + lvl);
            assertEquals(DoomDelves.BOSS, v.kcKey());
            assertTrue(v.drops().isEmpty());
        }
        assertEquals(558, DoomDelves.combatLevel(1));
        assertEquals(908, DoomDelves.combatLevel(8));
        assertEquals(60, DoomDelves.chargeMaxHit(1));
        assertEquals(99, DoomDelves.chargeMaxHit(6));
        assertEquals(47, DoomDelves.standardMaxHit(1));
        assertEquals(65, DoomDelves.standardMaxHit(12));
        assertEquals(6, DoomDelves.attackSpeed(1));
        assertEquals(4, DoomDelves.attackSpeed(7));
        // Uniques unlock at delves 2, 3 and 4; overall 1/2500 at delve 2 down to 1/180 from delve 9.
        assertEquals(List.of(), DoomDelves.eligibleUniques(1));
        assertEquals(List.of("mokhaiotl_cloth"), DoomDelves.eligibleUniques(2));
        assertEquals(3, DoomDelves.eligibleUniques(4).size());
        assertEquals(0, DoomDelves.uniqueRate(1));
        assertEquals(2500, DoomDelves.uniqueRate(2));
        assertEquals(450, DoomDelves.uniqueRate(4));
        assertEquals(180, DoomDelves.uniqueRate(9));
        assertEquals(180, DoomDelves.uniqueRate(20));
        assertEquals(0, DoomDelves.petRate(5));
        assertEquals(1000, DoomDelves.petRate(6));
        assertEquals(250, DoomDelves.petRate(9));
        // Quantity multiplier: 3 platelegs at delve 3 are 2 at delve 2 (3 - trunc(1.05)) and 3 at delve 8.
        assertEquals(2, DoomDelves.quantity(3, 2));
        assertEquals(3, DoomDelves.quantity(3, 8));
        assertEquals(1200, DoomDelves.quantity(1000, 9));
        assertEquals(0, DoomDelves.guaranteedTears(2));
        assertEquals(50, DoomDelves.guaranteedTears(3));
        assertEquals(100, DoomDelves.guaranteedTears(11));
    }

    @Test
    void delveLootUsesRealItems() {
        int total = 0;
        for (Drop d : DoomDelves.TABLE) {
            assertNotNull(ItemCatalog.get(d.item()), d.item());
            total += d.rarity();
        }
        assertTrue(total > 250 && total <= 312, "table weight " + total);
        for (String u : DoomDelves.UNIQUES) {
            assertNotNull(ItemCatalog.get(u), u);
        }
        Random r = new Random(1);
        int uniques = 0;
        for (int i = 0; i < 20000; i++) {
            List<DoomDelves.Loot> loot = DoomDelves.roll(9, r);
            assertTrue(loot.stream().anyMatch(l -> l.item().equals("demon_tear") && l.count() == 100));
            uniques += (int) loot.stream().filter(l -> DoomDelves.UNIQUES.contains(l.item())).count();
            assertTrue(DoomDelves.roll(1, r).stream().noneMatch(l -> DoomDelves.UNIQUES.contains(l.item())));
        }
        assertTrue(uniques > 60 && uniques < 170, "uniques " + uniques); // ~111 expected at 1/180
    }
}
