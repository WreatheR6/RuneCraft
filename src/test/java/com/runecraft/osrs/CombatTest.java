package com.runecraft.osrs;

import com.runecraft.osrs.data.MetalTier;
import com.runecraft.osrs.data.WeaponType;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatTest {
    @Test
    void combatLevels() {
        assertEquals(3, CombatLevel.of(1, 1, 1, 10, 1, 1, 1));
        assertEquals(126, CombatLevel.of(99, 99, 99, 99, 99, 99, 99));
        // Pure ranger 99 ranged, 1 def, 10 hp, 1 prayer
        assertEquals(50, CombatLevel.of(1, 1, 1, 10, 1, 99, 1));
    }

    @Test
    void meleeMaxHits() {
        // 99 strength, aggressive, no prayer, rune scimitar (+44): floor((110 * 108 + 320) / 640) = 19
        int eff = CombatFormulas.effectiveStrength(99, 1.0, CombatStyle.AGGRESSIVE);
        assertEquals(110, eff);
        assertEquals(19, CombatFormulas.meleeMaxHit(eff, 44));
        // +100 strength bonus at 110 effective: floor(18360 / 640) = 28
        assertEquals(28, CombatFormulas.meleeMaxHit(eff, 100));
        // Level 1, no gear: max hit 1
        assertEquals(1, CombatFormulas.meleeMaxHit(CombatFormulas.effectiveStrength(1, 1.0, CombatStyle.ACCURATE), 0));
        // 99 str with piety, aggressive, +0 bonus
        int piety = CombatFormulas.effectiveStrength(99, Prayer.PIETY.strengthMultiplier(), CombatStyle.AGGRESSIVE);
        assertEquals(132, piety);
    }

    @Test
    void rangedMaxHit() {
        // 99 ranged accurate, rune arrows (+49): floor(0.5 + 110*113/640) = 19
        int eff = CombatFormulas.effectiveRanged(99, 1.0, CombatStyle.RANGED_ACCURATE);
        assertEquals(19, CombatFormulas.rangedMaxHit(eff, 49));
    }

    @Test
    void hitChanceFormula() {
        assertEquals(0.5, Chance.hitChance(100, 99), 1e-9);
        assertTrue(Chance.hitChance(10_000, 100) > 0.98);
        assertTrue(Chance.hitChance(100, 10_000) < 0.01);
    }

    @Test
    void rollsWithinMaxHit() {
        Random r = new Random(1);
        for (int i = 0; i < 1000; i++) {
            int dmg = CombatFormulas.roll(r, 5000, 100, 13);
            assertTrue(dmg >= 0 && dmg <= 13);
        }
    }

    @Test
    void weaponStats() {
        assertEquals(44, WeaponType.SCIMITAR.stats(MetalTier.RUNE).meleeStrength());
        assertEquals(66, WeaponType.SCIMITAR.stats(MetalTier.DRAGON).meleeStrength());
        assertEquals(7, WeaponType.SCIMITAR.stats(MetalTier.BRONZE).slashAttack());
        assertEquals(85, WeaponType.SCIMITAR.smithingLevel(MetalTier.RUNE) - 5);
        assertEquals(99, com.runecraft.osrs.data.ArmourType.PLATEBODY.smithingLevel(MetalTier.RUNE));
    }

    @Test
    void npcMaxHit() {
        assertEquals(1, com.runecraft.osrs.data.Monster.GOBLIN.maxHit());
        assertTrue(com.runecraft.osrs.data.Monster.BLACK_DEMON.maxHit() >= 15);
    }

    @Test
    void prayerConflicts() {
        assertTrue(Prayer.PIETY.conflictsWith(Prayer.BURST_OF_STRENGTH));
        assertTrue(Prayer.PROTECT_FROM_MELEE.conflictsWith(Prayer.PROTECT_FROM_MAGIC));
        assertTrue(Prayer.EAGLE_EYE.conflictsWith(Prayer.ULTIMATE_STRENGTH));
        assertTrue(!Prayer.EAGLE_EYE.conflictsWith(Prayer.STEEL_SKIN));
        assertTrue(!Prayer.PROTECT_ITEM.conflictsWith(Prayer.PIETY));
    }
}
