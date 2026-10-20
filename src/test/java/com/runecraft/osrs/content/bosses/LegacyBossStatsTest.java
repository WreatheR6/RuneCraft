package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.data.Boss;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The built-in {@link Boss} bosses of the classic-bosses scope (the Barrows brothers, Obor and Bryophyta) diffed against a
 * snapshot of the wiki DPS calculator's {@code monsters.json} ({@code src/test/resources/osrs/bosses/legacy_boss_reference.tsv}):
 * levels, hitpoints, skills, attack speed, per-style defence bonuses (the brothers' heavy melee armour and weak magic defence),
 * the attack bonus of the style each uses, elemental weakness, max hit and attack style.
 */
class LegacyBossStatsTest {
    private static List<String[]> reference() throws Exception {
        String fixture = "osrs/bosses/legacy_boss_reference.tsv";
        Path p = Path.of("src/test/resources", fixture);
        List<String> lines;
        if (Files.exists(p)) {
            lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        } else {
            try (InputStream in = LegacyBossStatsTest.class.getClassLoader().getResourceAsStream(fixture)) {
                assertNotNull(in, "snapshot missing");
                lines = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines().toList();
            }
        }
        List<String[]> rows = new ArrayList<>();
        for (String line : lines) {
            if (!line.isBlank() && !line.startsWith("#")) {
                rows.add(line.split("\\|", -1));
            }
        }
        return rows;
    }

    private static int i(String s) {
        return Integer.parseInt(s.trim());
    }

    @Test
    void statsMatchTheDpsCalculator() throws Exception {
        List<String[]> rows = reference();
        assertEquals(8, rows.size());
        for (String[] r : rows) {
            Boss b = Boss.byEntity(r[0]);
            assertNotNull(b, r[0]);
            String id = r[0];
            assertEquals(r[1], b.displayName(), id + " name");
            assertEquals(i(r[2]), b.combatLevel(), id + " level");
            assertEquals(i(r[3]), b.hitpoints(), id + " hitpoints");
            assertEquals(i(r[4]), b.attack(), id + " attack");
            assertEquals(i(r[5]), b.strength(), id + " strength");
            assertEquals(i(r[6]), b.defence(), id + " defence");
            assertEquals(i(r[7]), b.magic(), id + " magic");
            assertEquals(i(r[8]), b.ranged(), id + " ranged");
            assertEquals(i(r[9]), b.attackSpeed(), id + " attack speed");
            AttackType[] types = AttackType.values();
            for (int t = 0; t < types.length; t++) {
                assertEquals(i(r[10 + t]), b.defenceBonus(types[t]), id + " " + types[t] + " defence bonus");
            }
            AttackType style = AttackType.valueOf(r[21]);
            assertEquals(style, b.style(), id + " attack style");
            int expectedAttack = style.isMelee() ? i(r[15]) : style == AttackType.MAGIC ? i(r[16]) : i(r[17]);
            assertEquals(expectedAttack, b.attackBonus(style), id + " attack bonus of its own style");
            assertEquals(r[18], b.weakness(), id + " weakness");
            assertEquals(i(r[19]), b.weaknessPercent(), id + " weakness percent");
            assertEquals(i(r[20]), b.maxHit(), id + " max hit");
        }
        // Obor also throws rocks (ranged +100).
        assertEquals(100, Boss.OBOR.attackBonus(AttackType.RANGED));
    }

    @Test
    void theMeleeBrothersAreWeakToMagic() {
        for (Boss b : List.of(Boss.DHAROK, Boss.GUTHAN, Boss.TORAG, Boss.VERAC)) {
            assertEquals(true, b.defenceBonus(AttackType.MAGIC) < 10 && b.defenceBonus(AttackType.SLASH) > 200, b.name());
        }
    }
}
