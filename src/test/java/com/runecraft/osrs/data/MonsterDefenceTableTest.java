package com.runecraft.osrs.data;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.PlayerHitCalc;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.CombatProfile;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The light / heavy ranged defences and elemental weaknesses from monsters.json reach the module creatures. */
class MonsterDefenceTableTest {
    private static Map<String, CombatProfile<?>> profiles() {
        Map<String, CombatProfile<?>> all = new HashMap<>();
        for (MonsterDef m : ContentRegistry.monsters()) {
            all.put(m.id, m);
        }
        for (BossDef b : ContentRegistry.bosses()) {
            all.put(b.id, b);
        }
        return all;
    }

    @Test
    void everyRowIsAppliedToItsCreature() {
        Map<String, CombatProfile<?>> all = profiles();
        List<String> bad = new ArrayList<>();
        for (MonsterDefenceTable.Row r : MonsterDefenceTable.rows().values()) {
            CombatProfile<?> p = all.get(r.id());
            if (p == null) {
                bad.add(r.id() + ": no such creature");
                continue;
            }
            if (MonsterStatsTable.row(r.id()) == null) {
                bad.add(r.id() + ": no MonsterStatsTable row");
                continue;
            }
            if (p.rangedDefence("light") != r.lightRanged() || p.rangedDefence("heavy") != r.heavyRanged()
                    || !java.util.Objects.equals(p.weakness, r.weakness()) || p.weaknessPercent != r.weaknessPercent()) {
                bad.add(r.id() + ": not applied");
            }
            if (r.weakness() != null && !Set.of("air", "water", "earth", "fire").contains(r.weakness())) {
                bad.add(r.id() + ": unknown element " + r.weakness());
            }
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
        assertTrue(MonsterDefenceTable.rows().size() >= 300, "only " + MonsterDefenceTable.rows().size() + " rows");
    }

    @Test
    void knownValues() {
        Map<String, CombatProfile<?>> all = profiles();
        CombatProfile<?> black = all.get("black_dragon");
        assertEquals(50, black.rangedDefence("standard"));
        assertEquals(50, black.rangedDefence("light"));
        assertEquals(10, black.rangedDefence("heavy"));
        assertEquals("water", black.weakness);
        assertEquals(50, black.weaknessPercent);
        CombatProfile<?> corp = all.get("corporeal_beast");
        assertEquals(230, corp.defenceBonus(AttackType.RANGED));
        assertEquals(100, corp.rangedDefence("heavy"));
        assertEquals((230 + 230 + 100) / 3, corp.rangedDefence("mixed"));
    }

    @Test
    void weaponClasses() {
        for (String w : new String[]{"toxic_blowpipe", "dragon_dart", "rune_knife", "dragon_thrownaxe", "toktz_xil_ul", "morrigans_javelin"}) {
            assertEquals("light", PlayerHitCalc.rangedDefenceKind(w), w);
        }
        for (String w : new String[]{"rune_crossbow", "armadyl_crossbow", "zaryte_crossbow", "heavy_ballista", "light_ballista", "red_chinchompa", "lms_rune_crossbow"}) {
            assertEquals("heavy", PlayerHitCalc.rangedDefenceKind(w), w);
        }
        for (String w : new String[]{"twisted_bow", "magic_shortbow", "bow_of_faerdhinen", "eclipse_atlatl", "dark_bow"}) {
            assertEquals("standard", PlayerHitCalc.rangedDefenceKind(w), w);
        }
        assertEquals("mixed", PlayerHitCalc.rangedDefenceKind("black_salamander"));
        assertNull(MonsterDefenceTable.row("no_such_monster"));
    }
}
