package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.bosses.MuspahRules.Form;
import com.runecraft.osrs.content.bosses.MuspahRules.Special;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Phantom Muspah's fight rules against the OSRS Wiki (July 2026). */
class MuspahRulesTest {
    @Test
    void formsSwapByDamageAndLaterAlsoByHits() {
        assertFalse(MuspahRules.swaps(Form.RANGED, 99, 9, false));
        assertTrue(MuspahRules.swaps(Form.RANGED, 100, 1, false)); // the first swap: damage alone
        assertTrue(MuspahRules.swaps(Form.MELEE, 80, 1, false));
        assertFalse(MuspahRules.swaps(Form.MELEE, 200, 3, true)); // from then on also four separate hits (22 July 2026)
        assertTrue(MuspahRules.swaps(Form.MELEE, 80, 4, true));
    }

    @Test
    void specialsAtSeventyFiveAndFiftyPercent() {
        assertEquals(637, MuspahRules.FIRST_SPECIAL_HP);
        assertEquals(425, MuspahRules.SECOND_SPECIAL_HP);
        assertEquals(127, MuspahRules.SHIELD_BELOW);
        assertEquals(0, MuspahRules.specialsDue(638));
        assertEquals(1, MuspahRules.specialsDue(637));
        assertEquals(2, MuspahRules.specialsDue(425));
        // Ranged spawns start with Lightning Clouds, melee spawns with Homing Spikes; the other comes at 50%.
        assertEquals(Special.LIGHTNING_CLOUDS, MuspahRules.special(0, Form.RANGED));
        assertEquals(Special.HOMING_SPIKES, MuspahRules.special(1, Form.RANGED));
        assertEquals(Special.HOMING_SPIKES, MuspahRules.special(0, Form.MELEE));
        assertEquals(Special.LIGHTNING_CLOUDS, MuspahRules.special(1, Form.MELEE));
    }

    @Test
    void maxHitsAndSpeeds() {
        assertEquals(61, MuspahRules.rangedMax(false));
        assertEquals(72, MuspahRules.magicMax(false));
        assertEquals(72, MuspahRules.rangedMax(true));
        assertEquals(79, MuspahRules.magicMax(true));
        assertEquals(6, MuspahRules.RANGED_SPEED);
        assertEquals(5, MuspahRules.FAST_SPEED);
        BossDef m = ContentRegistry.boss("phantom_muspah");
        assertEquals(741, m.combatLevel);
        assertEquals(850, m.hitpoints);
        assertTrue(m.phases.isEmpty(), "its forms follow the damage taken, not BossDef phases");
        assertEquals(AttackType.STAB, m.attacks.stream().filter(a -> a.name().equals("stomp")).findFirst().orElseThrow().style());
        assertTrue(m.attacks.stream().noneMatch(a -> a.name().equals("darkness")), "no invented darkness attack");
        // Its melee: blocked when it had to chase, more through the prayer the longer it stands still.
        assertEquals(0.0, MuspahRules.meleeThroughPrayer(0));
        assertTrue(MuspahRules.meleeThroughPrayer(2) > MuspahRules.meleeThroughPrayer(1));
        assertEquals(1.0, MuspahRules.meleeThroughPrayer(10));
    }

    @Test
    void corruptionDrainsEighteenPrayerOverEighteenSeconds() {
        assertEquals(18, MuspahRules.corruptionDrain(1, false) + MuspahRules.corruptionDrain(2, false) + MuspahRules.corruptionDrain(3, false));
        assertEquals(10, MuspahRules.CORRUPTION_INTERVAL);
        assertTrue(MuspahRules.corruptionDrain(1, true) > MuspahRules.corruptionDrain(1, false));
    }

    @Test
    void homingSpikesStepWithoutDiagonalsAndStopAtSpikes() {
        Set<Long> none = new HashSet<>();
        assertArrayEquals(new int[]{1, 0}, MuspahRules.homingStep(0, 0, 5, 2, none)); // along the longer axis
        assertArrayEquals(new int[]{0, 1}, MuspahRules.homingStep(0, 0, 1, 5, none));
        Set<Long> blocked = new HashSet<>(Set.of(BossMechanics.tile(1, 0)));
        assertArrayEquals(new int[]{0, 1}, MuspahRules.homingStep(0, 0, 5, 2, blocked)); // the other axis around a spike
        blocked.add(BossMechanics.tile(0, 1));
        assertArrayEquals(new int[]{0, 0}, MuspahRules.homingStep(0, 0, 5, 2, blocked)); // boxed in: it can't pass spikes
        assertEquals(34, MuspahRules.HOMING_TICKS);
        assertEquals(2, MuspahRules.HOMING_STEP_TICKS);
    }

    @Test
    void onlyASpikeSheltersFromTheShockwave() {
        Set<Long> spikes = new HashSet<>();
        assertFalse(MuspahRules.sheltered(spikes, 0, 0, 8, 0, 2));
        spikes.add(BossMechanics.tile(5, 0));
        assertTrue(MuspahRules.sheltered(spikes, 0, 0, 8, 0, 2));
        assertFalse(MuspahRules.sheltered(spikes, 0, 0, 0, 8, 2)); // not between them
        spikes.clear();
        spikes.add(BossMechanics.tile(1, 0)); // under its own footprint: no shelter
        assertFalse(MuspahRules.sheltered(spikes, 0, 0, 8, 0, 2));
        spikes.add(BossMechanics.tile(8, 0)); // the player's own tile doesn't shelter them
        assertFalse(MuspahRules.sheltered(spikes, 0, 0, 8, 0, 2));
        List<Long> line = MuspahRules.line(0, 0, 3, 3);
        assertEquals(List.of(BossMechanics.tile(0, 0), BossMechanics.tile(1, 1), BossMechanics.tile(2, 2), BossMechanics.tile(3, 3)), line);
        assertEquals(80, MuspahRules.SHOCKWAVE);
        assertEquals(5, MuspahRules.SHOCKWAVE_WIND_UP);
    }

    @Test
    void dropsAfterSecretsOfTheNorth() {
        BossDef m = ContentRegistry.boss("phantom_muspah");
        assertEquals(30, m.drops.stream().filter(d -> d.item().equals("clue_scroll_hard")).findFirst().orElseThrow().rarity());
        assertEquals(45, m.drops.stream().filter(d -> d.item().equals("clue_scroll_elite")).findFirst().orElseThrow().rarity());
        assertEquals(100, m.drops.stream().filter(d -> d.item().equals("venator_shard")).findFirst().orElseThrow().rarity());
        assertEquals(50, m.drops.stream().filter(d -> d.item().equals("ancient_icon")).findFirst().orElseThrow().rarity());
        assertEquals(2500, com.runecraft.osrs.content.extras.Pets.engineRate("phantom_muspah"));
        assertTrue(m.drops.stream().noneMatch(d -> d.item().equals("coins")), "no coins on its table");
    }
}
