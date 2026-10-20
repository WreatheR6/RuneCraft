package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.minigames.FortisColosseum.Modifier;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round 34: the Colosseum's monster mechanics after the wiki's strategy guide ("Fortis Colosseum/Strategies#Monsters"),
 * "Minotaur (Fortis Colosseum)" and "Fortis Colosseum/Modifiers".
 */
class ColosseumRound34Test {
    private static MonsterDef monster(String id) {
        return FortisColosseum.monsters().stream().filter(d -> d.id.equals(id)).findFirst().orElseThrow();
    }

    private static double range(String id) {
        return monster(id).attacks.stream().mapToDouble(AttackDef::range).max().orElse(0);
    }

    @Test
    void attackRangesFollowTheStrategyGuide() {
        // The whole warband attacks from melee range only, even the archer and seer.
        for (String w : FortisColosseum.WARBAND) {
            assertTrue(FortisColosseum.attacksFromMelee(w), w);
            assertEquals(FortisColosseum.WARBAND_REACH, range(w), 1e-9, w);
        }
        assertFalse(FortisColosseum.attacksFromMelee(FortisColosseum.SHAMAN));
        assertEquals(10, range(FortisColosseum.SHAMAN), 1e-9);
        assertEquals(15, range(FortisColosseum.JAVELIN), 1e-9);
        assertEquals(15, range(FortisColosseum.SHOCKWAVE), 1e-9);
        assertEquals(15, range(FortisColosseum.MANTICORE), 1e-9);
        // Sizes from the strategy tables.
        assertEquals(1, FortisColosseum.size(FortisColosseum.BERSERKER));
        assertEquals(1, FortisColosseum.size(FortisColosseum.SHAMAN));
        assertEquals(2, FortisColosseum.size(FortisColosseum.JAGUAR));
        assertEquals(2, FortisColosseum.size(FortisColosseum.BEE_SWARM));
        assertEquals(3, FortisColosseum.size(FortisColosseum.JAVELIN));
        assertEquals(3, FortisColosseum.size(FortisColosseum.MANTICORE));
        assertEquals(3, FortisColosseum.size(FortisColosseum.MINOTAUR));
        assertEquals(5, FortisColosseum.size(FortisColosseum.SOL_HEREDIT));
    }

    @Test
    void javelinColossusThrowsEveryFifthAttack() {
        // After every four auto-attacks the fifth is a javelin landing 6 ticks later for up to 40 typeless damage.
        List<Integer> throwsAt = new java.util.ArrayList<>();
        for (int n = 1; n <= 15; n++) {
            if (FortisColosseum.javelinThrow(n)) {
                throwsAt.add(n);
            }
        }
        assertEquals(List.of(5, 10, 15), throwsAt);
        assertEquals(6, FortisColosseum.JAVELIN_FLIGHT_TICKS);
        assertEquals(40, FortisColosseum.JAVELIN_MAX_HIT);
        // Reentry I melts the target tile; II also the tile south-west (permanent); III also the tile west of the target.
        assertEquals(0, FortisColosseum.reentryTiles(0).size());
        assertEquals(List.of("0,0"), tiles(FortisColosseum.reentryTiles(1)));
        assertEquals(List.of("0,0", "-1,1"), tiles(FortisColosseum.reentryTiles(2)));
        assertEquals(List.of("0,0", "-1,1", "-1,0"), tiles(FortisColosseum.reentryTiles(3)));
    }

    private static List<String> tiles(List<int[]> list) {
        return list.stream().map(t -> t[0] + "," + t[1]).toList();
    }

    @Test
    void volatilityExplodesOneOrTwoTilesBeyondTheEnemy() {
        // Wiki: a 3x3 manticore explodes in a 5x5 at tier I and a 7x7 at tier II.
        int five = 0;
        int seven = 0;
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                five += FortisColosseum.volatilityHits(3, 1, dx, dz) ? 1 : 0;
                seven += FortisColosseum.volatilityHits(3, 2, dx, dz) ? 1 : 0;
            }
        }
        assertEquals(25, five);
        assertEquals(49, seven);
        assertEquals(seven, countHits(3, 3), "tier III keeps tier II's radius (and leaves molten sand)");
        assertEquals(9, countHits(1, 1)); // a warband member: 3x3
        assertEquals(49, countHits(5, 1)); // Sol Heredit (5x5) explodes too: 7x7
        assertFalse(FortisColosseum.volatilityHits(3, 0, 0, 0));
    }

    private static int countHits(int size, int tier) {
        int n = 0;
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                n += FortisColosseum.volatilityHits(size, tier, dx, dz) ? 1 : 0;
            }
        }
        return n;
    }

    @Test
    void minotaurHealsWoundedMonstersInSight() {
        // Below 75%, within 7 tiles and in line of sight (Mod Arcane).
        assertTrue(FortisColosseum.minotaurHeals(100, 200, 7, true));
        assertFalse(FortisColosseum.minotaurHeals(150, 200, 3, true), "75% is not wounded enough");
        assertFalse(FortisColosseum.minotaurHeals(100, 200, 7.5, true), "8 tiles is too far");
        assertFalse(FortisColosseum.minotaurHeals(100, 200, 3, false), "a pillar blocks the heal");
    }

    @Test
    void solarflareOrbitsThePillars() {
        // Waves 1-11: one orb on the square through the pillars, pausing at its four corners.
        List<int[]> regular = FortisColosseum.solarflarePath(false, 0);
        assertEquals(64, regular.size());
        assertEquals(1, FortisColosseum.solarflareOrbs(false));
        Set<String> corners = new HashSet<>();
        for (int i = 0; i < regular.size(); i++) {
            if (FortisColosseum.solarflareCorner(false, i)) {
                corners.add(regular.get(i)[0] + "," + regular.get(i)[1]);
            }
        }
        assertEquals(Set.of("-8,-8", "8,-8", "8,8", "-8,8"), corners);
        // Consecutive tiles are one step apart (the orb moves a tile at a time).
        for (int i = 0; i < regular.size(); i++) {
            int[] a = regular.get(i);
            int[] b = regular.get((i + 1) % regular.size());
            assertEquals(1, Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]));
        }
        // Wave 12: four orbs, each on the 5x5 ring around one pillar.
        assertEquals(4, FortisColosseum.solarflareOrbs(true));
        Set<String> pillars = new HashSet<>();
        for (int orb = 0; orb < 4; orb++) {
            List<int[]> ring = FortisColosseum.solarflarePath(true, orb);
            assertEquals(16, ring.size());
            int cx = 0;
            int cz = 0;
            for (int[] t : ring) {
                cx += t[0];
                cz += t[1];
            }
            pillars.add(cx / 16 + "," + cz / 16);
            for (int[] t : ring) {
                assertEquals(2, Math.max(Math.abs(t[0] - cx / 16), Math.abs(t[1] - cz / 16)), "a 5x5 ring round the pillar");
            }
        }
        assertEquals(Set.of("-8,-8", "8,-8", "-8,8", "8,8"), pillars);
        // Speeds: I every 2 ticks with a 7-tick corner pause, II every 2 without, III every tick with 2.
        assertEquals(List.of(2, 2, 1), List.of(FortisColosseum.solarflareMoveTicks(1), FortisColosseum.solarflareMoveTicks(2),
                FortisColosseum.solarflareMoveTicks(3)));
        assertEquals(List.of(7, 0, 2), List.of(FortisColosseum.solarflareCornerPause(1), FortisColosseum.solarflareCornerPause(2),
                FortisColosseum.solarflareCornerPause(3)));
    }

    @Test
    void quartetAddsAWarbanderBesideSolHeredit() {
        Map<Modifier, Integer> active = FortisColosseum.newModifiers();
        assertEquals(List.of(FortisColosseum.SOL_HEREDIT), FortisColosseum.startingMonsters(12, active, new Random(1)));
        active.put(Modifier.QUARTET, 1);
        List<String> sol = FortisColosseum.startingMonsters(12, active, new Random(1));
        assertEquals(2, sol.size());
        assertEquals(FortisColosseum.SOL_HEREDIT, sol.get(0));
        assertTrue(FortisColosseum.WARBAND.contains(sol.get(1)));
        // Dynamic Duo only pairs shockwave colossi, which never fight beside Sol.
        active.put(Modifier.DYNAMIC_DUO, 1);
        assertEquals(2, FortisColosseum.startingMonsters(12, active, new Random(1)).size());
    }

    @Test
    void undefendedGrappleTearsOffTheCalledArmour() {
        assertEquals("CHEST", SolHeredit.armourSlot(SolHeredit.Grapple.BODY));
        assertEquals("LEGS", SolHeredit.armourSlot(SolHeredit.Grapple.LEGS));
        assertEquals("FEET", SolHeredit.armourSlot(SolHeredit.Grapple.FEET));
        assertEquals(null, SolHeredit.armourSlot(SolHeredit.Grapple.BACK));
        assertEquals(null, SolHeredit.armourSlot(SolHeredit.Grapple.HANDS));
    }

    @Test
    void totemAndBeeTimings() {
        assertEquals(200, FortisColosseum.TOTEM_RESPAWN_TICKS);
        assertEquals(30, FortisColosseum.TOTEM_HEAL_PERCENT);
        assertTrue(FortisColosseum.TOTEM_FIRST_HEAL_TICKS > 0);
        assertEquals(12, FortisColosseum.BEE_MOVE_TICKS);
        assertEquals(1, FortisColosseum.BEE_STEP_TILES);
        assertEquals(50, FortisColosseum.BEE_RESPAWN_TICKS);
        assertEquals(AttackType.CRUSH, FortisColosseum.manticorePattern(0, null, new Random(5)).get(2));
    }
}
