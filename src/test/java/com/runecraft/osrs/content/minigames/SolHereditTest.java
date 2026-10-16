package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.minigames.SolHeredit.Attack;
import com.runecraft.osrs.content.minigames.SolHeredit.Grapple;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sol Heredit against the OSRS Wiki ("Sol Heredit", "Fortis Colosseum/Strategies#Sol Heredit"). */
class SolHereditTest {
    @Test
    void phasesBeamsAndSpecialUnlocks() {
        assertEquals(1, SolHeredit.phase(100));
        assertEquals(1, SolHeredit.phase(90.5));
        assertEquals(2, SolHeredit.phase(90));
        assertEquals(3, SolHeredit.phase(75));
        assertEquals(4, SolHeredit.phase(50));
        assertEquals(5, SolHeredit.phase(25));
        assertEquals(6, SolHeredit.phase(10));
        assertEquals(6, SolHeredit.phase(1));
        // One light beam per phase from phase 2, four in all ("No more Light Beams are added from this point").
        assertEquals(List.of(0, 1, 2, 3, 4, 4), List.of(SolHeredit.beams(1), SolHeredit.beams(2), SolHeredit.beams(3),
                SolHeredit.beams(4), SolHeredit.beams(5), SolHeredit.beams(6)));
        assertFalse(SolHeredit.tripleUnlocked(1));
        assertTrue(SolHeredit.tripleUnlocked(2));      // below 90%
        assertFalse(SolHeredit.grappleUnlocked(2));
        assertTrue(SolHeredit.grappleUnlocked(3));     // below 75%
    }

    @Test
    void attackPatternAlternation() {
        Random r = new Random(7);
        assertEquals(Attack.SPEAR_1, SolHeredit.next(null, 1, false, r)); // always opens with Spear 1
        assertEquals(Attack.SPEAR_2, SolHeredit.regular(Attack.SPEAR_1, true));
        assertEquals(Attack.SPEAR_1, SolHeredit.regular(Attack.SPEAR_2, true));
        assertEquals(Attack.SHIELD_1, SolHeredit.regular(Attack.SPEAR_1, false)); // switching style resets
        assertEquals(Attack.SHIELD_2, SolHeredit.regular(Attack.SHIELD_1, false));
        assertEquals(Attack.SPEAR_1, SolHeredit.regular(Attack.SHIELD_2, true));
        // After a phase transition: always a spear, remembering Spear 1; a special resets the pattern.
        assertEquals(Attack.SPEAR_2, SolHeredit.next(Attack.SPEAR_1, 3, true, r));
        assertEquals(Attack.SPEAR_1, SolHeredit.next(Attack.SHIELD_1, 3, true, r));
        assertEquals(Attack.SPEAR_1, SolHeredit.next(Attack.TRIPLE, 3, true, r));
        Set<Attack> seen = EnumSet.noneOf(Attack.class);
        Attack prev = Attack.SPEAR_1;
        for (int i = 0; i < 2000; i++) {
            Attack a = SolHeredit.next(prev, 1, false, r);
            seen.add(a);
            prev = a;
        }
        assertFalse(seen.contains(Attack.TRIPLE) || seen.contains(Attack.GRAPPLE), "no specials in phase 1");
        prev = Attack.SPEAR_1;
        for (int i = 0; i < 2000; i++) {
            Attack a = SolHeredit.next(prev, 3, false, r);
            assertFalse(a.special() && prev.special(), "two specials in a row");
            if (prev.special()) {
                assertTrue(a == Attack.SPEAR_1 || a == Attack.SHIELD_1, "a special resets the pattern: " + a);
            }
            seen.add(a);
            prev = a;
        }
        assertEquals(EnumSet.allOf(Attack.class), seen);
    }

    @Test
    void attackSpeedsAndTriple() {
        assertEquals(7, SolHeredit.attackDelay(Attack.SPEAR_1, 1));
        assertEquals(6, SolHeredit.attackDelay(Attack.SHIELD_2, 2));
        assertEquals(6, SolHeredit.attackDelay(Attack.SPEAR_2, 3)); // one tick faster below 75%
        assertEquals(5, SolHeredit.attackDelay(Attack.SHIELD_1, 3));
        assertEquals(List.of(15, 25, 35), List.of(SolHeredit.tripleDamage(0, 2), SolHeredit.tripleDamage(1, 2), SolHeredit.tripleDamage(2, 3)));
        assertEquals(List.of(15, 30, 45), List.of(SolHeredit.tripleDamage(0, 4), SolHeredit.tripleDamage(1, 5), SolHeredit.tripleDamage(2, 6)));
        assertEquals(3, SolHeredit.tripleGap(0, 4));
        assertEquals(3, SolHeredit.tripleGap(2, 3));
        assertEquals(4, SolHeredit.tripleGap(2, 4)); // the last hit is delayed a tick from 50%
        // Protect from Melee must be switched on just before the hit; left on early, it doesn't block.
        assertTrue(SolHeredit.parried(true, 99, 100));
        assertTrue(SolHeredit.parried(true, 100, 100));
        assertFalse(SolHeredit.parried(true, 90, 100));
        assertFalse(SolHeredit.parried(false, 99, 100));
        assertFalse(SolHeredit.perfectParry(0));
        assertFalse(SolHeredit.perfectParry(2));
        assertTrue(SolHeredit.perfectParry(SolHeredit.GRAPPLE_WINDOW_TICKS - 1)); // the last tick before it lands
        assertEquals(5, Grapple.values().length);
        assertEquals(Grapple.BACK, Grapple.bySlot("back"));
        assertEquals("I'LL CUT YOUR FEET OFF!", Grapple.FEET.shout);
    }

    @Test
    void aoePatternsHaveTheWikiSafeSpots() {
        int fx = 0;
        int fz = 1; // player to the south (+z)
        for (Attack a : List.of(Attack.SPEAR_1, Attack.SPEAR_2, Attack.SHIELD_1, Attack.SHIELD_2)) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    assertTrue(SolHeredit.hits(a, dx, dz, fx, fz), a + " covers the 5x5 under him");
                }
            }
        }
        // Spear 1: one tile back from his centre or corner tiles is safe; the off-centre lines are not.
        assertTrue(SolHeredit.hits(Attack.SPEAR_1, 0, 3, fx, fz));
        assertFalse(SolHeredit.hits(Attack.SPEAR_1, 0, 4, fx, fz));
        assertFalse(SolHeredit.hits(Attack.SPEAR_1, 2, 4, fx, fz));
        assertTrue(SolHeredit.hits(Attack.SPEAR_1, 1, 4, fx, fz));
        assertTrue(SolHeredit.hits(Attack.SPEAR_1, -1, 7, fx, fz));
        assertFalse(SolHeredit.hits(Attack.SPEAR_1, -1, 8, fx, fz));
        // Spear 2: the off-centre tiles next to him are safe; the centre and edge lines are not.
        assertFalse(SolHeredit.hits(Attack.SPEAR_2, 1, 3, fx, fz));
        assertFalse(SolHeredit.hits(Attack.SPEAR_2, -1, 3, fx, fz));
        assertTrue(SolHeredit.hits(Attack.SPEAR_2, 0, 3, fx, fz));
        assertTrue(SolHeredit.hits(Attack.SPEAR_2, 2, 6, fx, fz));
        // Shields: one (Shield 1) or two (Shield 2) tiles back from melee range is safe, everything else within 7 is hit.
        assertTrue(SolHeredit.hits(Attack.SHIELD_1, 0, 3, fx, fz));
        assertFalse(SolHeredit.hits(Attack.SHIELD_1, 4, -4, fx, fz));
        assertTrue(SolHeredit.hits(Attack.SHIELD_1, 5, 0, fx, fz));
        assertTrue(SolHeredit.hits(Attack.SHIELD_2, 4, 0, fx, fz));
        assertFalse(SolHeredit.hits(Attack.SHIELD_2, 0, -5, fx, fz));
        assertTrue(SolHeredit.hits(Attack.SHIELD_2, 7, 7, fx, fz));
        assertFalse(SolHeredit.hits(Attack.SHIELD_2, 8, 0, fx, fz));
        // The patterns turn with the player: Spear 1 aimed east.
        assertTrue(SolHeredit.hits(Attack.SPEAR_1, 5, 1, 1, 0));
        assertFalse(SolHeredit.hits(Attack.SPEAR_1, 5, 0, 1, 0));
        assertEquals(List.of(1, 0), List.of(SolHeredit.facing(5, 2)[0], SolHeredit.facing(5, 2)[1]));
        assertEquals(List.of(0, -1), List.of(SolHeredit.facing(1, -4)[0], SolHeredit.facing(1, -4)[1]));
    }

    @Test
    void bossDefinitionFollowsTheWiki() {
        BossDef sol = ContentRegistry.boss(FortisColosseum.SOL_HEREDIT);
        assertNotNull(sol);
        assertEquals(SolHeredit.PHASES, sol.phases.stream().map(BossDef.Phase::belowPercent).toList());
        assertEquals(SolHeredit.PHASE_LINES, sol.phases.stream().map(BossDef.Phase::message).toList());
        for (BossDef.Phase p : sol.phases) {
            assertEquals(1.0, p.damageMultiplier(), 1e-9);
        }
        int max = 0;
        for (AttackDef a : sol.attacks) {
            max = Math.max(max, a.maxHit());
        }
        assertEquals(SolHeredit.LASER_MAX_HIT, max);
        assertEquals(44, SolHeredit.AOE_MAX_HIT);
        assertEquals(75, SolHeredit.TOTEM_HEAL);
        assertEquals(7, SolHeredit.TOTEM_HEAL_TICKS);
        assertEquals(FortisColosseum.TOTEM_HEAL_TICKS, SolHeredit.TOTEM_HEAL_TICKS);
    }
}
