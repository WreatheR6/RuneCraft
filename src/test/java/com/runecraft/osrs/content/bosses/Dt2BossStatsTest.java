package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.data.MonsterStatsTable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** The DT2 bosses' literal stats equal the osrs-dps-calc rows MonsterStatsTable applies (no dead, misleading literals). */
class Dt2BossStatsTest {
    @Test
    void literalsMatchTheStatsTable() {
        for (BossDef b : Dt2Bosses.all()) {
            MonsterStatsTable.Row r = MonsterStatsTable.row(b.id);
            assertNotNull(r, b.id);
            assertEquals(r.combatLevel(), b.combatLevel, b.id + " level");
            assertEquals(r.hitpoints(), b.hitpoints, b.id + " hitpoints");
            assertEquals(r.attack(), b.attack, b.id + " attack");
            assertEquals(r.strength(), b.strength, b.id + " strength");
            assertEquals(r.defence(), b.defence, b.id + " defence");
            assertEquals(r.magic(), b.magic, b.id + " magic");
            assertEquals(r.ranged(), b.ranged, b.id + " ranged");
            assertEquals(r.stabDefence(), b.defenceBonus(AttackType.STAB), b.id + " stab defence");
            assertEquals(r.slashDefence(), b.defenceBonus(AttackType.SLASH), b.id + " slash defence");
            assertEquals(r.crushDefence(), b.defenceBonus(AttackType.CRUSH), b.id + " crush defence");
            assertEquals(r.magicDefence(), b.defenceBonus(AttackType.MAGIC), b.id + " magic defence");
        }
    }
}
