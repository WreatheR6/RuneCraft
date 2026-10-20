package com.runecraft.osrs.content.raids;

import com.runecraft.osrs.content.CombatProfile;
import com.runecraft.osrs.content.ContentRegistry;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ToA raid level scaling against in-game numbers: the hitpoints snapshot
 * ({@code src/test/resources/osrs/raids/toa_hp_reference.tsv}, from the DPS calculator's ToA test data), the wiki's
 * "+2% per 5 raid levels" for hitpoints, defence and accuracy (damage capped at +150%), and the Helpful Spirit's
 * supplies.
 */
class ToaScalingReferenceTest {
    private static CombatProfile<?> profile(String id) {
        CombatProfile<?> p = ContentRegistry.boss(id);
        return p != null ? p : ContentRegistry.monster(id);
    }

    @Test
    void hitpointsMatchTheInGameValues() throws Exception {
        List<String> diffs = new ArrayList<>();
        int rows = 0;
        for (String line : Files.readAllLines(Path.of("src/test/resources/osrs/raids/toa_hp_reference.tsv"), StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] r = line.split("\t");
            int level = Integer.parseInt(r[0]);
            CombatProfile<?> p = profile(r[1]);
            if (p == null) {
                diffs.add(r[1] + ": not registered");
                continue;
            }
            int want = Integer.parseInt(r[4]);
            int got = RaidScaling.toaScaledHp(r[1], p.hitpoints, level, Integer.parseInt(r[3]), 1, r[2].equals("yes"));
            if (got != want) {
                diffs.add(r[1] + " at raid level " + level + " path " + r[3] + ": " + got + " != " + want);
            }
            rows++;
        }
        assertEquals(56, rows);
        assertTrue(diffs.isEmpty(), String.join("\n", diffs));
    }

    @Test
    void wikiMultipliersAtTheModeLevels() {
        // OSRS wiki: every 5 raid levels add 2% hitpoints, defence and accuracy (additively); damage caps at +150%.
        int[] levels = {0, 150, 300, 400, 500};
        double[] stat = {1.0, 1.6, 2.2, 2.6, 3.0};
        double[] damage = {1.0, 1.6, 2.2, 2.5, 2.5};
        for (int i = 0; i < levels.length; i++) {
            assertEquals(stat[i], RaidScaling.toaHp(levels[i], 1), 1e-9, "hp at " + levels[i]);
            assertEquals(stat[i], RaidScaling.toaAccuracy(levels[i]), 1e-9, "accuracy at " + levels[i]);
            assertEquals(stat[i], RaidScaling.toaDefence("zebak", levels[i]), 1e-9, "defence at " + levels[i]);
            assertEquals(damage[i], RaidScaling.toaDamage(levels[i]), 1e-9, "damage at " + levels[i]);
        }
        // Akkha (400 hp) solo at the mode levels, then in groups: x1.9 for two, x2.8 for three, +0.6 per further player.
        assertEquals(400, RaidScaling.toaScaledHp("akkha", 400, 0, 0, 1, true));
        assertEquals(640, RaidScaling.toaScaledHp("akkha", 400, 150, 0, 1, true));
        assertEquals(880, RaidScaling.toaScaledHp("akkha", 400, 300, 0, 1, true));
        assertEquals(1040, RaidScaling.toaScaledHp("akkha", 400, 400, 0, 1, true));
        assertEquals(1200, RaidScaling.toaScaledHp("akkha", 400, 500, 0, 1, true));
        assertEquals(1220, RaidScaling.toaScaledHp("akkha", 400, 150, 0, 2, true));
        assertEquals(1790, RaidScaling.toaScaledHp("akkha", 400, 150, 0, 3, true));
        assertEquals(2180, RaidScaling.toaScaledHp("akkha", 400, 150, 0, 4, true));
        assertEquals(RaidScaling.toaScaledHp("akkha", 400, 150, 0, 8, true), RaidScaling.toaScaledHp("akkha", 400, 150, 0, 12, true),
                "party scaling stops at eight");
    }

    @Test
    void apmekenBaboonsHaveStaticHitpoints() {
        // OSRS (May 2024): the Path of Apmeken's baboons no longer scale with raid level or party size.
        assertEquals(25, RaidScaling.toaScaledHp("toa_baboon_brawler", 25, 500, 4, 8, true));
        assertEquals(30, RaidScaling.toaScaledHp("toa_baboon_thrower", 30, 300, 2, 3, true));
        assertEquals(10, RaidScaling.BABA_BABOON_HP);
        // OSRS wiki: Ba-Ba calls 2-4 baboons depending on party size.
        assertEquals(2, RaidScaling.babaBaboons(1));
        assertEquals(4, RaidScaling.babaBaboons(8));
        java.util.Random r = new java.util.Random(3);
        for (int i = 0; i < 200; i++) {
            int first = ToaBossRules.crackedBoulder(-1, r);
            assertTrue(first >= 1 && first <= 3, "the first cracked boulder is in the middle three");
            int next = ToaBossRules.crackedBoulder(first, r);
            assertTrue(Math.abs(next - first) <= 2 && next >= 0 && next < ToaBossRules.BOULDERS_PER_SET);
        }
    }

    @Test
    void akkhaCyclesHisStyles() {
        assertEquals(1, Invocation.akkhaNextStyle(0));
        assertEquals(2, Invocation.akkhaNextStyle(1));
        assertEquals(0, Invocation.akkhaNextStyle(2));
        Set<Invocation> none = EnumSet.noneOf(Invocation.class);
        Set<Invocation> vigilant = EnumSet.of(Invocation.STAY_VIGILANT);
        assertEquals(1, Invocation.akkhaSwitchOneIn(none, true), "always switches after a special");
        assertEquals(0, Invocation.akkhaSwitchOneIn(none, false));
        assertEquals(5, Invocation.akkhaSwitchOneIn(vigilant, true));
        assertEquals(4, Invocation.akkhaSwitchOneIn(vigilant, false));
        assertEquals(0.5, Invocation.kephriMedicSwarmSpeed(EnumSet.of(Invocation.MEDIC)), 1e-9);
        assertEquals(1.0, Invocation.kephriMedicSwarmSpeed(none), 1e-9);
    }

    @Test
    void helpfulSpiritSupplies() {
        Set<Invocation> none = EnumSet.noneOf(Invocation.class);
        assertEquals(Map.of(ToaSupplies.SMELLING_SALTS, 2, ToaSupplies.ADRENALINE, 1),
                ToaSupplies.bundle(ToaSupplies.Bundle.POWER, none, new Random(1)));
        for (int seed = 0; seed < 50; seed++) {
            Map<String, Integer> life = ToaSupplies.bundle(ToaSupplies.Bundle.LIFE, none, new Random(seed));
            assertEquals(5, life.get(ToaSupplies.NECTAR));
            assertEquals(5, life.get(ToaSupplies.TEARS));
            assertTrue(life.get(ToaSupplies.SILK_DRESSING) >= 1 && life.get(ToaSupplies.SILK_DRESSING) <= 3);
            assertTrue(life.get(ToaSupplies.AMBROSIA) >= 1 && life.get(ToaSupplies.AMBROSIA) <= 2);
            Map<String, Integer> diet = ToaSupplies.bundle(ToaSupplies.Bundle.LIFE, EnumSet.of(Invocation.ON_A_DIET), new Random(seed));
            assertNull(diet.get(ToaSupplies.SILK_DRESSING), "On a Diet removes the dressings");
            assertEquals(2, diet.get(ToaSupplies.CRYSTAL_SCARAB));
            Map<String, Integer> chaos = ToaSupplies.bundle(ToaSupplies.Bundle.CHAOS, none, new Random(seed));
            assertTrue(chaos.get(ToaSupplies.NECTAR) >= 1 && chaos.get(ToaSupplies.NECTAR) <= 8);
            assertTrue(chaos.getOrDefault(ToaSupplies.TEARS, 0) <= 6);
            // The Helpful Spirit invocations cut every supply, but always leave at least one.
            Map<String, Integer> tenth = ToaSupplies.bundle(ToaSupplies.Bundle.LIFE, EnumSet.of(Invocation.NO_HELP_NEEDED), new Random(seed));
            tenth.values().forEach(n -> assertEquals(1, n));
        }
        assertEquals(1.0, ToaSupplies.supplyFactor(none), 1e-9);
        assertEquals(0.66, ToaSupplies.supplyFactor(EnumSet.of(Invocation.NEED_SOME_HELP)), 1e-9);
        assertEquals(0.33, ToaSupplies.supplyFactor(EnumSet.of(Invocation.NEED_LESS_HELP)), 1e-9);
        assertEquals(3, ToaSupplies.reduce(5, 0.66));
        assertEquals(1, ToaSupplies.reduce(5, 0.10));
        assertEquals(0, ToaSupplies.reduce(0, 0.66));
        assertTrue(ToaSupplies.offeredAfter(2) && ToaSupplies.offeredAfter(4));
        assertFalse(ToaSupplies.offeredAfter(1) || ToaSupplies.offeredAfter(3));
        assertEquals(0, ToaSupplies.honeyLocustsOnWipe(EnumSet.of(Invocation.ON_A_DIET)));
    }

    @Test
    void supplyEffectsFollowTheWikiFormulas() {
        assertEquals(17, ToaSupplies.nectarHeal(99)); // floor(99 x 0.15) + 3
        assertEquals(116, ToaSupplies.afterNectar(110, 99), "overheals up to level + 17");
        assertEquals(10, ToaSupplies.nectarDrain(118)); // floor(118 / 20 + 5)
        assertEquals(26, ToaSupplies.ambrosiaHitpointsBoost(99));
        assertEquals(24, ToaSupplies.ambrosiaPrayerBoost(99));
        assertEquals(26, ToaSupplies.saltsBoost(99)); // floor(99 x 0.16) + 11
        assertEquals(27, ToaSupplies.tearsRestore(99));
        assertEquals(34, ToaSupplies.tearsPrayer(99));
        assertEquals(19, ToaSupplies.tearsPrayerShared(99));
        assertEquals(31, ToaSupplies.honeyLocustPrayer(99));
        assertEquals(32, ToaSupplies.honeyLocustRestore(99));
        assertEquals(100, ToaSupplies.SILK_HEAL * ToaSupplies.SILK_TIMES, "silk dressing heals 100 over a minute");
        assertEquals(72, ToaSupplies.SCARAB_PRAYER * ToaSupplies.SCARAB_TIMES, "a crystal scarab restores 72 prayer");
        assertEquals(2, ToaSupplies.doses(ToaSupplies.ADRENALINE));
        assertEquals(4, ToaSupplies.doses(ToaSupplies.NECTAR));
        // Overly Draining makes every special cost 100%; liquid adrenaline halves it (50%), or halves the weapon's cost.
        assertEquals(-1, ToaSupplies.specialCost(50, false, false));
        assertEquals(100, ToaSupplies.specialCost(25, true, false));
        assertEquals(50, ToaSupplies.specialCost(25, true, true));
        assertEquals(25, ToaSupplies.specialCost(50, false, true));
        for (String id : ToaSupplies.ITEMS) {
            assertTrue(RaidItems.RAID_ONLY_ITEMS.contains(id), id + " is taken away when leaving the raid");
        }
    }
}
