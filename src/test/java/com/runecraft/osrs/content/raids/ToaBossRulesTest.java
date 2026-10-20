package com.runecraft.osrs.content.raids;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Tombs of Amascut's boss rules (OSRS wiki boss pages). */
class ToaBossRulesTest {
    @Test
    void akkha() {
        Random r = new Random(5);
        for (int i = 0; i < 200; i++) {
            List<Integer> order = ToaBossRules.memoryBlastOrder(ToaBossRules.memoryBlastRounds(4), r);
            assertEquals(6, order.size());
            for (int k = 1; k < order.size(); k++) {
                int step = Math.floorMod(order.get(k) - order.get(k - 1), 4);
                assertTrue(step == 1 || step == 3, "each lit quadrant is next to the previous one: " + order);
            }
        }
        assertEquals(4, ToaBossRules.memoryBlastRounds(0));
        assertEquals(3, ToaBossRules.akkhaSwapHits(1));
        assertEquals(5, ToaBossRules.akkhaSwapHits(3));
        assertEquals(25, ToaBossRules.unstableOrbDamage(5.0, 4), "unstable orbs are capped at 25");
        assertEquals(12, ToaBossRules.unstableOrbDamage(1.0, 0));
        // Protect from Magic takes 25% off before the cap: it helps at low levels, not once the orbs hit far over 25.
        assertEquals(9, ToaBossRules.unstableOrbDamage(1.0, 0, true));
        assertEquals(25, ToaBossRules.unstableOrbDamage(3.5, 4, true));
        assertEquals(List.of("fire", "shadow", "lightning", "ice"), List.of(ToaBossRules.QUADRANTS));
    }

    @Test
    void babaAndZebak() {
        assertEquals(25, ToaBossRules.babaBoulderHp(0));
        assertEquals(27, ToaBossRules.babaBoulderHp(2));
        assertEquals(31, ToaBossRules.babaBoulderHp(4));
        assertEquals(10, ToaBossRules.debrisHp(1));
        assertEquals(30, ToaBossRules.debrisHp(3));
        assertEquals(10, ToaBossRules.BOULDER_SETS);
        assertEquals(5, ToaBossRules.BOULDERS_PER_SET);
        assertTrue(ToaBossRules.babaRockfallEvery(4) < ToaBossRules.babaRockfallEvery(0));
        assertEquals(7, ToaBossRules.zebakAttackSpeed(false, 0));
        assertEquals(4, ToaBossRules.zebakAttackSpeed(true, 0));
        assertEquals(5, ToaBossRules.zebakAttackSpeed(false, 4));
        assertEquals(38, ToaBossRules.ZEBAK_MELEE_MAX);
        assertEquals(3, ToaBossRules.ZEBAK_ROCK_HP / ToaBossRules.ZEBAK_ROAR_ON_ROCK, "a rock shelters from one whole roar");
    }

    /** Boulder hitpoints match the wiki's measured table (src/test/resources/osrs/raids/toa_boulder_hp.tsv). */
    @Test
    void babaBouldersMatchTheWikiTable() throws Exception {
        int rows = 0;
        for (String line : java.nio.file.Files.readAllLines(java.nio.file.Path.of("src/test/resources/osrs/raids/toa_boulder_hp.tsv"))) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] r = line.split("\t");
            int team = Integer.parseInt(r[0]);
            for (int path = 0; path <= 4; path++) {
                assertEquals(Integer.parseInt(r[path + 1]), ToaBossRules.babaBoulderHp(team, path), "team " + team + " path " + path);
            }
            rows++;
        }
        assertEquals(8, rows);
        assertEquals(31, ToaBossRules.babaBoulderHp(1, 6), "path levels above 4 add nothing more");
    }

    @Test
    void babaShockwaveAndSarcophagi() {
        // A 3x3 plus one tile straight out on each side (13 tiles); 5x5 plus one out with Shaking Things Up (29 tiles).
        int normal = 0;
        int shaking = 0;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                normal += ToaBossRules.babaShockwaveHits(dx, dz, false) ? 1 : 0;
                shaking += ToaBossRules.babaShockwaveHits(dx, dz, true) ? 1 : 0;
            }
        }
        assertEquals(13, normal);
        assertEquals(29, shaking);
        assertTrue(ToaBossRules.babaShockwaveHits(0, 2, false) && !ToaBossRules.babaShockwaveHits(1, 2, false));
        assertTrue(ToaBossRules.babaShockwaveHits(0, 3, true) && !ToaBossRules.babaShockwaveHits(0, 3, false));
        // Opened sarcophagi hit a single tile (since September 2022), more from path level 2.
        assertEquals(0, ToaBossRules.babaSarcophagusBlastRadius(0));
        assertEquals(0, ToaBossRules.babaSarcophagusBlastRadius(1));
        assertEquals(1, ToaBossRules.babaSarcophagusBlastRadius(2));
        assertEquals(2, ToaBossRules.babaSarcophagusBlastRadius(5));
    }

    @Test
    void wardens() {
        assertEquals(7, ToaBossRules.wardenSlamEvery(Set.of()));
        assertEquals(6, ToaBossRules.wardenSlamEvery(EnumSet.of(Invocation.OVERCLOCKED)));
        assertEquals(4, ToaBossRules.wardenSlamEvery(EnumSet.of(Invocation.OVERCLOCKED, Invocation.OVERCLOCKED_2)));
        assertEquals(3, ToaBossRules.wardenSlamEvery(EnumSet.of(Invocation.OVERCLOCKED, Invocation.OVERCLOCKED_2, Invocation.INSANITY)));
        assertEquals(1, ToaBossRules.siphonHp(1));
        assertEquals(1, ToaBossRules.siphonHp(3));
        assertEquals(2, ToaBossRules.siphonHp(5));
        assertEquals(3, ToaBossRules.siphonHp(7));
        assertEquals(4, ToaBossRules.siphonHp(8));
        assertTrue(ToaBossRules.siphonTicks(1, true) < ToaBossRules.siphonTicks(1, false));
        assertEquals(List.of(80, 60, 40, 20), List.of(ToaBossRules.SIPHON_THRESHOLDS[0], ToaBossRules.SIPHON_THRESHOLDS[1],
                ToaBossRules.SIPHON_THRESHOLDS[2], ToaBossRules.SIPHON_THRESHOLDS[3]));
        assertEquals(5, ToaBossRules.WARDEN_ENRAGE_AT);
        assertEquals(20, ToaBossRules.WARDEN_ENRAGE_HEAL);
        assertNotNull(Invocation.byName("Insanity"));
        // Phase 1: blocking Tumeken's trough destroys Tumeken's Warden, so Elidinis' is fought in phase 2.
        assertTrue(ToaBossRules.fightElidinisFirst(0, 5, false));
        assertTrue(!ToaBossRules.fightElidinisFirst(5, 0, true));
        assertTrue(ToaBossRules.fightElidinisFirst(2, 2, true));
        assertEquals(45, ToaBossRules.chargedShotCap(true, true));
        assertEquals(30, ToaBossRules.chargedShotCap(true, false));
        assertEquals(30, ToaBossRules.chargedShotCap(false, true));
        assertEquals("ranged", ToaBossRules.firstPrayedStyle(true));
        assertEquals("magic", ToaBossRules.firstPrayedStyle(false));
        // Lightning Skull: 7x7, the two tiles straight out from the skull safe (the skull's own tile is not).
        int struck = 0;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                struck += ToaBossRules.lightningSkullHits(dx, dz) ? 1 : 0;
            }
        }
        assertEquals(49 - 8, struck);
        assertTrue(ToaBossRules.lightningSkullHits(0, 0));
        assertTrue(!ToaBossRules.lightningSkullHits(0, 2));
        assertTrue(!ToaBossRules.lightningSkullHits(-1, 0));
        assertTrue(ToaBossRules.lightningSkullHits(1, 1));
        assertTrue(ToaBossRules.lightningSkullHits(0, 3));
        // Slams: right, left, both; one tile to the other side dodges a side slam, the centre three dodge both.
        assertTrue(ToaBossRules.slamHits(0, 0) && ToaBossRules.slamHits(0, -5) && !ToaBossRules.slamHits(0, 1));
        assertTrue(ToaBossRules.slamHits(1, 0) && ToaBossRules.slamHits(1, 5) && !ToaBossRules.slamHits(1, -1));
        assertTrue(!ToaBossRules.slamHits(2, 0) && !ToaBossRules.slamHits(2, 1) && !ToaBossRules.slamHits(2, -1)
                && ToaBossRules.slamHits(2, 2) && ToaBossRules.slamHits(2, -2));
        for (int side = -3; side <= 3; side++) {
            boolean dodgesSome = false;
            for (int k = 0; k < 3; k++) {
                dodgesSome |= !ToaBossRules.slamHits(k, side);
            }
            assertTrue(dodgesSome, "every tile dodges at least one slam");
        }
        assertEquals("right", ToaBossRules.SLAMS[0]);
        assertNotEquals(ToaBossRules.wardenSlamEvery(Set.of()), ToaBossRules.wardenSlamEvery(EnumSet.of(Invocation.INSANITY)));
    }

    /** Every ToA creature's own attacks top out at its DPS calculator max hit (src/test/resources/osrs/raids/toa_max_hits.tsv). */
    @Test
    void maxHitsMatchTheDpsCalculator() throws Exception {
        List<String> diffs = new java.util.ArrayList<>();
        int rows = 0;
        for (String line : java.nio.file.Files.readAllLines(java.nio.file.Path.of("src/test/resources/osrs/raids/toa_max_hits.tsv"))) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] r = line.split("\t", -1);
            com.runecraft.osrs.content.CombatProfile<?> p = com.runecraft.osrs.content.ContentRegistry.boss(r[0]);
            if (p == null) {
                p = com.runecraft.osrs.content.ContentRegistry.monster(r[0]);
            }
            assertNotNull(p, r[0]);
            int max = 0;
            for (com.runecraft.osrs.content.AttackDef a : p.attacks) {
                max = Math.max(max, a.maxHit());
            }
            if (max != Integer.parseInt(r[3])) {
                diffs.add(r[0] + " " + max + " != " + r[3]);
            }
            rows++;
        }
        assertTrue(rows >= 14, "rows " + rows);
        assertTrue(diffs.isEmpty(), String.join("\n", diffs));
        // Scripted attacks (OSRS infoboxes): Zebak 38 melee / 16 projectiles, the Wardens 20 / 22 (phase 2) and 26 (phase 3).
        assertEquals(38, ToaBossRules.ZEBAK_MELEE_MAX);
        assertEquals(16, ToaBossRules.ZEBAK_RANGED_MAX);
        assertEquals(20, ToaBossRules.ELIDINIS_P2_MAX_HIT);
        assertEquals(22, ToaBossRules.TUMEKEN_P2_MAX_HIT);
        assertEquals(26, ToaBossRules.WARDEN_P3_MAX_HIT);
        // The raid level scales every creature's damage except the crocodiles and Agile Scarabs (OSRS wiki).
        assertTrue(RaidScaling.toaUnscaledDamage("toa_crocodile") && RaidScaling.toaUnscaledDamage("toa_agile_scarab"));
        assertTrue(!RaidScaling.toaUnscaledDamage("toa_soldier_scarab") && !RaidScaling.toaUnscaledDamage("toa_baboon_brawler"));
    }
}
