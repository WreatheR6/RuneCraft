package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.bosses.BossMechanics.YamaContract;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The scripted boss mechanics' numbers against the OSRS Wiki. */
class BossMechanicsTest {
    @Test
    void yamaNumbers() {
        BossDef yama = ContentRegistry.boss("yama");
        assertNotNull(yama);
        assertEquals(BossMechanics.YAMA_HITPOINTS, yama.hitpoints);
        assertEquals(BossMechanics.YAMA_SPEED, yama.attackSpeed);
        // Meteor Strike: lethal within a tile of the centre, 80 at two tiles, 40 at three, nothing at four.
        assertEquals(121, BossMechanics.meteorDamage(0));
        assertEquals(121, BossMechanics.meteorDamage(1));
        assertEquals(80, BossMechanics.meteorDamage(2));
        assertEquals(40, BossMechanics.meteorDamage(3));
        assertEquals(0, BossMechanics.meteorDamage(4));
        // Glyph of Fire: 2 / 6 / 9 / 12 as Yama gets closer.
        assertEquals(2, BossMechanics.glyphCost(6));
        assertEquals(2, BossMechanics.glyphCost(4));
        assertEquals(6, BossMechanics.glyphCost(3));
        assertEquals(9, BossMechanics.glyphCost(2));
        assertEquals(12, BossMechanics.glyphCost(1));
        // Void flares: 44 at full health, 25 in phase 3, scaled by remaining health.
        assertEquals(44, BossMechanics.flareExplosion(1.0, false));
        assertEquals(22, BossMechanics.flareExplosion(0.5, false));
        assertEquals(25, BossMechanics.flareExplosion(1.0, true));
        assertEquals(0, BossMechanics.flareExplosion(0, false));
    }

    @Test
    void yamaContractsAreRealItems() {
        Set<String> items = new HashSet<>();
        for (YamaContract c : YamaContract.values()) {
            assertNotNull(ItemCatalog.get(c.item), c.item);
            assertTrue(items.add(c.item));
            assertEquals(c, YamaContract.byItem(c.item));
            assertTrue(c.hitpoints >= BossMechanics.YAMA_HITPOINTS);
        }
        assertNull(YamaContract.byItem("dossier"));
        // Wiki contract stat table: bloodied blows 3,000 HP, glyphic attenuation 2,875, the others 2,500.
        assertEquals(3000, YamaContract.BLOODIED_BLOWS.hitpoints);
        assertEquals(2875, YamaContract.GLYPHIC_ATTENUATION.hitpoints);
        assertEquals(2500, YamaContract.SENSORY_CLOUDING.hitpoints);
        assertEquals("Contract of divine severance", YamaContract.DIVINE_SEVERANCE.displayName());
        assertEquals(53, (int) Math.round(BossMechanics.YAMA_AUTO_MAX * BossMechanics.yamaHitScale(YamaContract.FORFEIT_BREATH)));
        assertEquals(1.0, BossMechanics.yamaHitScale(null));
    }

    @Test
    void sireStunChances() {
        assertEquals(25, BossMechanics.sireStunChance("shadow_rush"));
        assertEquals(50, BossMechanics.sireStunChance("shadow_burst"));
        assertEquals(75, BossMechanics.sireStunChance("shadow_blitz"));
        assertEquals(100, BossMechanics.sireStunChance("shadow_barrage"));
        assertEquals(0, BossMechanics.sireStunChance("smoke_barrage"));
        assertEquals(0, BossMechanics.sireStunChance(null));
    }

    @Test
    void vardorvisRamp() {
        assertEquals(0, BossMechanics.vardorvisDefenceReduction(0));
        assertEquals(10, BossMechanics.vardorvisDefenceReduction(100));
        assertEquals(71, BossMechanics.vardorvisDefenceReduction(700));
        assertEquals(32, BossMechanics.vardorvisMaxHit(1.0, false));
        assertEquals(43, BossMechanics.vardorvisMaxHit(0.0, false));
        assertEquals(46, BossMechanics.vardorvisMaxHit(1.0, true));
        assertEquals(61, BossMechanics.vardorvisMaxHit(0.0, true));
        assertEquals(1, BossMechanics.vardorvisAxes(700, false));
        assertEquals(2, BossMechanics.vardorvisAxes(500, false));
        assertEquals(3, BossMechanics.vardorvisAxes(200, false));
        assertEquals(4, BossMechanics.vardorvisAxes(200, true));
        assertEquals(1, BossMechanics.vardorvisDarts(600));
        assertEquals(2, BossMechanics.vardorvisDarts(300));
        assertEquals(3, BossMechanics.vardorvisDarts(100));
    }

    @Test
    void maggotKingAndTitans() {
        assertEquals(0, BossMechanics.maggotLarvae(1400, 0));
        assertEquals(1, BossMechanics.maggotLarvae(1000, 2));
        assertEquals(4, BossMechanics.maggotLarvae(200, 0));
        assertEquals(1, BossMechanics.maggotLarvae(200, 3));
        assertEquals(4, BossMechanics.maggotAttacksBeforeScreech(0));
        assertEquals(8, BossMechanics.maggotAttacksBeforeScreech(4));
        assertTrue(BossMechanics.dousesFire("water_blast"));
        assertTrue(BossMechanics.dousesFire("ice_barrage"));
        assertTrue(!BossMechanics.dousesFire("water_bolt"));
        assertTrue(BossMechanics.meltsIce("fire_surge"));
        assertTrue(!BossMechanics.meltsIce("fire_bolt"));
    }

    @Test
    void phosaniSleepwalkers() {
        assertEquals(2, BossMechanics.sleepwalkers(1));
        assertEquals(3, BossMechanics.sleepwalkers(2));
        assertEquals(4, BossMechanics.sleepwalkers(3));
        // Wiki: with 4 sleepwalkers each one killed takes 36 off; none killed always 149, all killed 5.
        assertEquals(149, BossMechanics.sleepwalkerBlast(4, 4));
        assertEquals(113, BossMechanics.sleepwalkerBlast(3, 4));
        assertEquals(5, BossMechanics.sleepwalkerBlast(0, 4));
        assertEquals(77, BossMechanics.sleepwalkerBlast(1, 2));
        assertEquals(53, BossMechanics.sleepwalkerBlast(1, 3));
    }

    @Test
    void minionsExist() {
        Set<String> ids = new HashSet<>();
        for (MonsterDef m : new BossesContent().monsters()) {
            ids.add(m.id);
        }
        for (String id : new String[]{BossMechanics.YAMA_JUDGE, BossMechanics.YAMA_VOID_FLARE, BossMechanics.UNSTABLE_ICE,
                BossMechanics.UR_MAGGOT_LARVA, BossMechanics.PHOSANI_SLEEPWALKER}) {
            assertTrue(ids.contains(id), id);
        }
    }
}
