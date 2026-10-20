package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.data.Drop;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Moons of Peril (OSRS Wiki "Moons of Peril/Strategies", "Lunar Chest"). */
class MoonsRulesTest {
    @Test
    void standardAttackIsThreeHitsAndAMissEndsTheStack() {
        assertArrayEquals(new int[]{4, 8, 20}, MoonsRules.STANDARD_MAX);
        // every roll hits: three hits, each within its max
        int[] all = MoonsRules.standardHits(1.0, () -> 0.0, bound -> bound - 1);
        assertArrayEquals(new int[]{4, 8, 20}, all);
        // the second roll misses: the third misses with it
        Deque<Double> rolls = new ArrayDeque<>(List.of(0.1, 0.9, 0.1));
        int[] two = MoonsRules.standardHits(0.5, rolls::poll, bound -> 1);
        assertArrayEquals(new int[]{1, -1, -1}, two);
        int[] none = MoonsRules.standardHits(0.0, () -> 0.0, bound -> 1);
        assertArrayEquals(new int[]{-1, -1, -1}, none);
    }

    /** The glyph moves every two standard attacks: six attacks from the first glyph to the new moon, then six to the full. */
    @Test
    void glyphReachesASpecialEverySixAttacks() {
        int glyph = MoonsRules.FIRST_GLYPH;
        int attacks = 0;
        List<Integer> specials = new java.util.ArrayList<>();
        int since = 0;
        while (specials.size() < 4) {
            attacks++;
            if (++since == MoonsRules.STANDARD_PER_GLYPH) {
                since = 0;
                glyph = MoonsRules.nextGlyph(glyph);
                if (MoonsRules.specialGlyph(glyph)) {
                    specials.add(attacks);
                    glyph = MoonsRules.nextGlyph(glyph); // the special moves it on
                }
            }
        }
        assertEquals(List.of(6, 12, 18, 24), specials);
    }

    @Test
    void offTheGlyphDamageDoublesToSixteen() {
        assertEquals(0, MoonsRules.offGlyphDamage(0));
        assertEquals(List.of(1, 2, 4, 8, 16, 16, 16), java.util.stream.IntStream.rangeClosed(1, 7)
                .map(MoonsRules::offGlyphDamage).boxed().toList());
    }

    @Test
    void curseStacksDecayAndCap() {
        MoonsRules.Curse c = new MoonsRules.Curse();
        assertEquals(MoonsRules.BLUE_WARNINGS[0], c.add(3));
        assertEquals(0, c.add(1));
        assertEquals(MoonsRules.BLUE_WARNINGS[1], c.add(5));
        for (int t = 0; t < 10; t++) {
            c.tick();
        }
        assertEquals(5, c.stacks(), "2 every 5 ticks");
        c.add(500);
        assertEquals(127, c.stacks());
    }

    @Test
    void blueLocksAtEighteen() {
        MoonsRules.Curse c = new MoonsRules.Curse();
        c.add(17);
        assertFalse(c.blueLocks());
        c.add(3);
        assertTrue(c.blueLocks());
        assertEquals(2, c.stacks(), "the lock takes 18 stacks");
    }

    @Test
    void eclipseGlancesAndBloodHeals() {
        assertEquals(0.5, MoonsRules.glanceChance(50), 1e-9);
        assertEquals(1.0, MoonsRules.glanceChance(127), 1e-9);
        assertEquals(28, MoonsRules.glance(40, 40, false), "a max roll glances to max - 12");
        assertEquals(32, MoonsRules.glance(40, 40, true), "8 during the Clones");
        assertEquals(0, MoonsRules.glance(0, 40, false));
        assertEquals(5, MoonsRules.bloodHeal(0, 5));
        assertEquals(10, MoonsRules.bloodHeal(1, 5));
        assertEquals(30, MoonsRules.bloodHeal(2, 2), "the third heals at least 30");
        assertEquals(50, MoonsRules.bloodHeal(2, 20), "and at most 50");
        assertEquals(30, MoonsRules.bloodHeal(2, 0), "a landed third hit heals 30-50 regardless of the damage dealt");
        assertEquals(0, MoonsRules.bloodHeal(0, 0), "a 0 first hit heals nothing");
        assertEquals(0, MoonsRules.bloodHeal(1, 0), "nor a 0 second");
        assertEquals(20, MoonsRules.stormHeal(2));
        assertEquals(0, MoonsRules.stormHeal(0));
    }

    @Test
    void eachMoonHasItsSpecialsAndArmour() {
        assertEquals(MoonsRules.Special.WEAPON_FREEZE, MoonsRules.special(MoonsRules.BLUE, true));
        assertEquals(MoonsRules.Special.FROST_STORM, MoonsRules.special(MoonsRules.BLUE, false));
        assertEquals(MoonsRules.Special.CLONES, MoonsRules.special(MoonsRules.ECLIPSE, true));
        assertEquals(MoonsRules.Special.SEARING_RAYS, MoonsRules.special(MoonsRules.ECLIPSE, false));
        assertEquals(MoonsRules.Special.BLOOD_RAIN, MoonsRules.special(MoonsRules.BLOOD, true));
        assertEquals(MoonsRules.Special.BLOOD_JAGUARS, MoonsRules.special(MoonsRules.BLOOD, false));
        assertEquals(-5, MoonsRules.flatArmour(MoonsRules.BLUE, false));
        assertEquals(6, MoonsRules.flatArmour(MoonsRules.ECLIPSE, false));
        assertEquals(4, MoonsRules.flatArmour(MoonsRules.ECLIPSE, true));
        assertEquals(-2, MoonsRules.flatArmour(MoonsRules.BLOOD, false));
    }

    /** The defs: 329, 500 hitpoints, attack speed 6, one standard attack named as Perilous Dancer allows, no health phases. */
    @Test
    void moonDefsMatchTheWiki() {
        for (String id : MoonsRules.MOONS) {
            BossDef def = ContentRegistry.boss(id);
            assertEquals(329, def.combatLevel, id);
            assertEquals(500, def.hitpoints, id);
            assertEquals(6, def.attackSpeed, id);
            assertTrue(def.phases.isEmpty(), id + " has no health phases");
            assertEquals(4, MoonsRules.setPieces(def.drops).size(), id + " set");
            assertTrue(def.minion == null, id + ": the jaguars are scripted");
        }
        assertEquals(20, ContentRegistry.boss(MoonsRules.BLUE).attacks.get(0).maxHit());
        assertEquals("spear thrust", ContentRegistry.boss(MoonsRules.BLUE).attacks.get(0).name());
        assertEquals("shield bash", ContentRegistry.boss(MoonsRules.ECLIPSE).attacks.get(0).name());
        assertEquals("macuahuitl flurry", ContentRegistry.boss(MoonsRules.BLOOD).attacks.get(0).name());
    }

    @Test
    void lunarChestRollsOneThreeOrSixTimesWithoutAUnique() {
        assertEquals(1, MoonsRules.standardRolls(1));
        assertEquals(3, MoonsRules.standardRolls(2));
        assertEquals(6, MoonsRules.standardRolls(3));
        Map<String, List<String>> sets = Map.of(MoonsRules.BLUE, List.of("a", "b", "c", "d"));
        // no unique (the 1/56 roll never 0): one standard roll
        MoonsRules.ChestRoll roll = MoonsRules.chest(List.of(MoonsRules.BLUE), sets, Set.of(), bound -> bound - 1);
        assertTrue(roll.uniques().isEmpty());
        assertEquals(1, roll.standardRolls());
        // a unique: no standard rolls at all; never a duplicate while a piece is missing
        MoonsRules.ChestRoll lucky = MoonsRules.chest(List.of(MoonsRules.BLUE), sets, Set.of("a", "b", "c"), bound -> 0);
        assertEquals(List.of("d"), lucky.uniques());
        assertEquals(0, lucky.standardRolls());
        MoonsRules.ChestRoll full = MoonsRules.chest(List.of(MoonsRules.BLUE), sets, Set.of("a", "b", "c", "d"), bound -> 0);
        assertEquals(List.of("a"), full.uniques(), "the whole set owned: any piece again");
    }

    /** About 1/56 per Moon, with every piece of the set coming up. */
    @Test
    void lunarChestUniqueRate() {
        Map<String, List<Drop>> tables = new HashMap<>();
        for (String id : MoonsRules.MOONS) {
            tables.put(id, ContentRegistry.boss(id).drops);
        }
        Map<String, List<String>> sets = MoonsRules.sets(tables);
        Random random = new Random(7);
        int claims = 200_000;
        int uniques = 0;
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < claims; i++) {
            MoonsRules.ChestRoll r = MoonsRules.chest(MoonsRules.MOONS, sets, Set.of(), random::nextInt);
            uniques += r.uniques().size();
            seen.addAll(r.uniques());
        }
        double rate = uniques / (double) claims;
        assertEquals(3.0 / 56, rate, 0.002);
        assertEquals(12, seen.size());
        assertTrue(MoonsRules.standardLines(tables.get(MoonsRules.BLUE)).stream().noneMatch(d -> d.rarity() >= 100));
    }

    /**
     * Perilous Dancer (CA 531): only the Moons' standard attacks may hurt, and the Blood Moon's jaguar summon ("The initial
     * damage taken from the jaguar special attack ... will not fail this task, however, taking a hit from the jaguars themselves
     * will", OSRS Wiki); her pools and being hit off the glyph (unnamed damage) fail it.
     */
    @Test
    void perilousDancerAllowsTheJaguarSummonOnly() {
        String rule = com.runecraft.osrs.content.systems.CombatAchievements.task("ca_531").rule();
        String onlydmg = java.util.Arrays.stream(rule.split("\\s+")).filter(t -> t.startsWith("onlydmg=")).findFirst().orElseThrow();
        com.runecraft.osrs.content.systems.CombatAchievementRules.Metrics ok = new com.runecraft.osrs.content.systems.CombatAchievementRules.Metrics();
        ok.took(new com.runecraft.osrs.content.systems.CombatAchievementRules.Hit(true, "blood moon", "macuahuitl flurry", null, 12, false));
        ok.took(new com.runecraft.osrs.content.systems.CombatAchievementRules.Hit(true, "blood moon", MoonsRules.Special.BLOOD_JAGUARS.key, null, 15, false));
        assertTrue(com.runecraft.osrs.content.systems.CombatAchievementRules.meets(onlydmg, ok), "the summon's hit is allowed");
        for (String failing : new String[]{"jaguar_cleave", "blood_pool", null}) {
            com.runecraft.osrs.content.systems.CombatAchievementRules.Metrics m = new com.runecraft.osrs.content.systems.CombatAchievementRules.Metrics();
            m.took(new com.runecraft.osrs.content.systems.CombatAchievementRules.Hit(true, "blood moon", failing, null, 5, false));
            assertFalse(com.runecraft.osrs.content.systems.CombatAchievementRules.meets(onlydmg, m), String.valueOf(failing));
        }
    }
}
