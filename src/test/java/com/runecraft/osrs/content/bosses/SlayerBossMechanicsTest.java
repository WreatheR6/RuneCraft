package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.bosses.SlayerBosses.CerberusMove;
import com.runecraft.osrs.content.bosses.SlayerBosses.HydraPhase;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Slayer bosses' mechanics against the OSRS Wiki strategy pages. */
class SlayerBossMechanicsTest {
    @Test
    void krakenAndThermyAreTypeless() {
        assertEquals(AttackDef.Special.PRAYER_PIERCING, SlayerBosses.KRAKEN_BLAST.special());
        assertEquals(28, SlayerBosses.KRAKEN_BLAST.maxHit());
        assertEquals(AttackDef.Special.PRAYER_PIERCING, SlayerBosses.THERMY_BLAST.special());
        assertEquals(8, SlayerBosses.THERMY_BLAST.maxHit());
        // Mod Ash: "If you rolled 0 damage, it stays as 0. If you rolled >=1 damage, it's divided by 7, with a floor of 1."
        assertEquals(0, SlayerBosses.krakenRangedDamage(0));
        assertEquals(1, SlayerBosses.krakenRangedDamage(1));
        assertEquals(1, SlayerBosses.krakenRangedDamage(13));
        assertEquals(2, SlayerBosses.krakenRangedDamage(14));
        assertEquals(7, SlayerBosses.krakenRangedDamage(50));
    }

    @Test
    void cerberusPattern() {
        // Full health: the triple attack on 1, 11, 21... and nothing else special.
        List<Integer> triples = new ArrayList<>();
        for (int n = 1; n <= 30; n++) {
            CerberusMove m = SlayerBosses.cerberusMove(n, 600, false);
            if (m == CerberusMove.TRIPLE) {
                triples.add(n);
            } else {
                assertEquals(CerberusMove.AUTO, m, "attack " + n + " at full health");
            }
        }
        assertEquals(List.of(1, 11, 21), triples);
        // Under 400: souls every seventh; under 200 lava every fifth; "attacks 14 and 15: souls first, then lava".
        assertEquals(CerberusMove.AUTO, SlayerBosses.cerberusMove(7, 400, false));
        assertEquals(CerberusMove.SOULS, SlayerBosses.cerberusMove(7, 399, false));
        assertEquals(CerberusMove.AUTO, SlayerBosses.cerberusMove(5, 399, false));
        assertEquals(CerberusMove.SOULS, SlayerBosses.cerberusMove(14, 150, false));
        assertEquals(CerberusMove.LAVA, SlayerBosses.cerberusMove(15, 150, false));
        // Overlaps: triple > souls > lava (attack 21 is a triple; 35 is souls rather than lava).
        assertEquals(CerberusMove.TRIPLE, SlayerBosses.cerberusMove(21, 100, false));
        assertEquals(CerberusMove.SOULS, SlayerBosses.cerberusMove(35, 100, false));
        // The 10% "just a normal attack" roll skips specials but never the triple attack.
        assertEquals(CerberusMove.AUTO, SlayerBosses.cerberusMove(14, 150, true));
        assertEquals(CerberusMove.TRIPLE, SlayerBosses.cerberusMove(11, 150, true));
        assertEquals(30, SlayerBosses.CERB_SOUL_HIT);
        assertEquals(22, SlayerBosses.CERB_SOUL_HIT_ELYSIAN);
        assertEquals(15, SlayerBosses.CERB_SOUL_DRAIN_SPECTRAL);
        assertTrue(SlayerBosses.CERB_TRIPLE_MELEE.range() > 20, "the triple attack's bite lands at any distance");
    }

    @Test
    void sirePhasesAndPrayer() {
        // "Using Protect from Melee will reduce the damage from these attacks to 6, 6 and 26 respectively."
        assertEquals(26, SlayerBosses.sireThroughPrayer(SlayerBosses.SIRE_SLAM, 66));
        assertEquals(6, SlayerBosses.sireThroughPrayer(SlayerBosses.SIRE_TENDRIL, SlayerBosses.SIRE_TENDRIL.maxHit()));
        assertEquals(0, SlayerBosses.sireThroughPrayer(SlayerBosses.SIRE_SLAM, 0));
        assertEquals(AttackDef.Special.PRAYER_PIERCING, SlayerBosses.SIRE_SLAM.special());
        assertEquals(96, SlayerBosses.SIRE_EXPLOSION.maxHit());
        assertEquals(60, SlayerBosses.SIRE_PORTAL_BLAST.maxHit());
        assertEquals(210, SlayerBosses.SIRE_PHASE3_BELOW);
        assertEquals(140, SlayerBosses.SIRE_EXPLODE_BELOW);
        assertEquals(25, SlayerBosses.SIRE_SPAWN_MATURES, "15 seconds");
        assertEquals(15, SlayerBosses.SIRE_MAX_MINIONS);
        // Respiratory systems give Slayer XP from the script, not as task kills.
        assertEquals(null, com.runecraft.osrs.content.ContentRegistry.monster("respiratory_system").slayerCategory);
    }

    @Test
    void hydraSpecialsAndMaxHits() {
        // Three basic attacks open each phase, then the special, then one after every nine.
        assertFalse(SlayerBosses.hydraSpecialDue(2, 0));
        assertTrue(SlayerBosses.hydraSpecialDue(3, 0));
        assertFalse(SlayerBosses.hydraSpecialDue(8, 1));
        assertTrue(SlayerBosses.hydraSpecialDue(9, 1));
        assertEquals(17, SlayerBosses.hydraMaxHit(HydraPhase.GREEN, false));
        assertEquals(26, SlayerBosses.hydraMaxHit(HydraPhase.GREEN, true));
        assertEquals(35, SlayerBosses.hydraMaxHit(HydraPhase.BLUE, false));
        assertEquals(26, SlayerBosses.hydraMaxHit(HydraPhase.BLUE, true));
        assertEquals(52, SlayerBosses.hydraMaxHit(HydraPhase.RED, true));
        assertEquals(55, SlayerBosses.hydraMaxHit(HydraPhase.ENRAGED, false));
        assertTrue(SlayerBosses.hydraHitsTwice(HydraPhase.GREEN, false));
        assertTrue(SlayerBosses.hydraHitsTwice(HydraPhase.BLUE, true));
        assertFalse(SlayerBosses.hydraHitsTwice(HydraPhase.RED, true));
        assertEquals(22, SlayerBosses.HYDRA_LIGHTNING.maxHit());
        assertEquals(25, SlayerBosses.HYDRA_FIRE.maxHit());
        // Each carapace is weakened by the vent of the colour that beats it.
        assertEquals("red", HydraPhase.GREEN.vent);
        assertEquals("green", HydraPhase.BLUE.vent);
        assertEquals("blue", HydraPhase.RED.vent);
    }
}
