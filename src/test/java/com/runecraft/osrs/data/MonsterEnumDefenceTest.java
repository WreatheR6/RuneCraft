package com.runecraft.osrs.data;

import com.runecraft.osrs.AttackType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Diffs {@link MonsterEnumDefence} and the Monster enum's Defence / Magic levels against the DPS calculator snapshot. */
class MonsterEnumDefenceTest {
    @Test
    void matchesTheSnapshot() throws IOException {
        List<String> bad = new ArrayList<>();
        int rows = 0;
        for (String line : Files.readAllLines(Path.of("src/test/resources/osrs/combat/monster_enum_defence.tsv"), StandardCharsets.UTF_8)) {
            if (line.startsWith("#") || line.startsWith("monster\t") || line.isBlank()) {
                continue;
            }
            String[] f = line.split("\t", -1);
            Monster m = Monster.valueOf(f[0]);
            MonsterEnumDefence.Row r = MonsterEnumDefence.row(m);
            rows++;
            if (r == null) {
                bad.add(f[0] + ": no row");
                continue;
            }
            int[] want = new int[9];
            for (int i = 0; i < 9; i++) {
                want[i] = Integer.parseInt(f[2 + i]);
            }
            int[] have = {m.defence(), m.magic(), r.stab(), r.slash(), r.crush(), r.magic(), r.light(), r.standard(), r.heavy()};
            for (int i = 0; i < 9; i++) {
                if (want[i] != have[i]) {
                    bad.add(f[0] + " column " + i + ": " + have[i] + " != " + want[i]);
                }
            }
            String weak = r.weakness() == null ? "-" : r.weakness() + ":" + r.weaknessPercent();
            if (!weak.equals(f[11])) {
                bad.add(f[0] + " weakness " + weak + " != " + f[11]);
            }
        }
        assertEquals(MonsterEnumDefence.all().size(), rows);
        assertTrue(bad.isEmpty(), String.join("\n", bad));
        assertEquals(25, MonsterEnumDefence.bonus(Monster.GUARD, AttackType.SLASH));
        assertEquals(Monster.SHEEP.defenceBonus(), MonsterEnumDefence.bonus(Monster.SHEEP, AttackType.STAB));
    }
}
