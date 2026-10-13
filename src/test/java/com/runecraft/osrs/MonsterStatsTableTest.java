package com.runecraft.osrs;

import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.CombatProfile;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Boss;
import com.runecraft.osrs.data.Monster;
import com.runecraft.osrs.data.MonsterStatsTable;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Monster and boss stats against the OSRS Wiki (DPS calculator data), via {@link MonsterStatsTable}. */
class MonsterStatsTableTest {

    private static CombatProfile<?> creature(String id) {
        MonsterDef m = ContentRegistry.monster(id);
        if (m != null) {
            return m;
        }
        BossDef b = ContentRegistry.boss(id);
        assertNotNull(b, id);
        return b;
    }

    /** level, hp, att, str, def, mage, range, speed, dStab, dSlash, dCrush, dMagic, dRanged, aMelee, aMagic, aRanged. */
    private static void assertLine(String id, int... want) {
        CombatProfile<?> p = creature(id);
        int[] got = {p.combatLevel, p.hitpoints, p.attack, p.strength, p.defence, p.magic, p.ranged, p.attackSpeed,
                p.defenceBonus(AttackType.STAB), p.defenceBonus(AttackType.SLASH), p.defenceBonus(AttackType.CRUSH),
                p.defenceBonus(AttackType.MAGIC), p.defenceBonus(AttackType.RANGED), p.meleeAttackBonus,
                p.magicAttackBonus, p.rangedAttackBonus};
        assertArrayEquals(want, got, id);
        assertTrue(p.referenceStats, id);
    }

    private static int max(String id, String attack) {
        for (AttackDef a : creature(id).attacks) {
            if (a.name().equals(attack)) {
                return a.maxHit();
            }
        }
        throw new AssertionError(id + " has no attack " + attack);
    }

    @Test
    void referenceStatLines() {
        assertLine("abyssal_demon", 124, 150, 97, 67, 135, 1, 1, 4, 20, 20, 20, 0, 20, 0, 0, 0);
        assertLine("rune_dragon", 380, 330, 284, 284, 276, 196, 246, 4, 20, 115, 90, 30, 95, 0, 0, 0);
        assertLine("vorkath", 732, 750, 560, 308, 214, 150, 308, 5, 26, 108, 108, 240, 26, 16, 150, 78);
        assertLine("zulrah", 725, 500, 1, 1, 300, 300, 300, 3, 0, 0, 0, -45, 50, 0, 50, 50);
        assertLine("zulrah_magma", 725, 500, 1, 1, 300, 300, 300, 3, 0, 0, 0, 0, 300, 0, 50, 50);
        assertLine("kalphite_queen", 333, 255, 300, 300, 300, 150, 1, 4, 50, 50, 10, 100, 100, 0, 0, 0);
        assertLine("kalphite_queen_airborne", 333, 255, 300, 300, 300, 150, 1, 4, 100, 100, 100, 10, 10, 0, 0, 0);
        assertLine("ice_troll", 82, 80, 80, 80, 40, 1, 1, 4, 30, 60, 30, 0, 0, 60, 0, 0);
        assertLine("ankou", 75, 60, 70, 70, 60, 1, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0);
        assertLine("dagannoth_lvl88", 88, 85, 10, 10, 75, 1, 100, 3, 10, 10, 90, 200, 150, 0, 0, 0);
        assertLine("giant_rock_crab", 137, 180, 50, 80, 200, 1, 1, 4, 225, 200, 175, 10, 250, 0, 0, 0);
        assertLine("prifddinas_guard", 108, 105, 95, 95, 80, 1, 1, 4, 50, 70, 70, 60, 50, 0, 0, 0);
        assertTrue(creature("monkey_zombie_lvl129").hasAttribute("undead"));
        assertTrue(creature("urium_shade").hasAttribute("shade"));
    }

    @Test
    void maxHitsCorrected() {
        assertEquals(29, max("rune_dragon", "melee"));
        assertEquals(17, max("ice_troll", "melee"));
        assertEquals(56, max("duke_sucellus", "fist_slam"));
        assertEquals(9, max("general_khazard", "slash"));
        // boss specials keep the module's numbers
        assertEquals(121, max("vorkath", "dragonfire_bomb"));
    }

    @Test
    void attributes() {
        assertTrue(creature("vorkath").hasAttribute("undead"));
        assertTrue(creature("vorkath").hasAttribute("dragon"));
        assertTrue(creature("abyssal_demon").hasAttribute("demon"));
        assertTrue(creature("kalphite_queen").hasAttribute("kalphite"));
        assertFalse(creature("rune_dragon").hasAttribute("undead"));
        // the name heuristic would call an impling an "imp" demon; the reference data does not
        assertFalse(creature("baby_impling").hasAttribute("demon"));
    }

    @Test
    void tableIsAppliedToEveryListedCreature() {
        assertTrue(MonsterStatsTable.rows().size() > 400, "rows " + MonsterStatsTable.rows().size());
        for (MonsterStatsTable.Row r : MonsterStatsTable.rows().values()) {
            CombatProfile<?> p = creature(r.id());
            assertTrue(p.referenceStats, r.id());
            assertEquals(r.combatLevel(), p.combatLevel, r.id());
            assertEquals(r.hitpoints(), p.hitpoints, r.id());
            assertEquals(r.attack(), p.attack, r.id());
            assertEquals(r.strength(), p.strength, r.id());
            assertEquals(r.defence(), p.defence, r.id());
            assertEquals(r.magic(), p.magic, r.id());
            assertEquals(r.ranged(), p.ranged, r.id());
            assertEquals(r.attackSpeed(), p.attackSpeed, r.id());
            assertEquals(r.rangedDefence(), p.defenceBonus(AttackType.RANGED), r.id());
            assertEquals(r.magicAttackBonus(), p.magicAttackBonus, r.id());
            assertEquals(r.attributes(), p.attributes, r.id());
            assertTrue(r.attackSpeed() > 0 && r.hitpoints() > 0, r.id());
            for (Map.Entry<String, Integer> e : MonsterStatsTable.maxHits(r.id()).entrySet()) {
                assertEquals(e.getValue(), max(r.id(), e.getKey()), r.id() + "." + e.getKey());
            }
        }
    }

    @Test
    void raidCreaturesAreLeftToRaidScaling() {
        for (String id : Set.of("tekton", "great_olm", "verzik_vitur", "akkha", "vespula")) {
            if (ContentRegistry.boss(id) != null) {
                assertNull(MonsterStatsTable.row(id), id);
                assertFalse(ContentRegistry.boss(id).referenceStats, id);
            }
        }
    }

    @Test
    void legacyEnums() {
        assertEquals(7, Boss.DELRITH.hitpoints());
        assertEquals(6, Boss.DELRITH.attackSpeed());
        assertEquals(22, Boss.OBOR.maxHit());
        assertEquals(130, Boss.BRYOPHYTA.attack());
        assertEquals(70, Monster.LESSER_DEMON.strength());
        assertEquals(5, Monster.ICE_GIANT.attackSpeed());
        assertEquals(30, Monster.GHOUL.attack());
    }
}
