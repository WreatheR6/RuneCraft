package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Boss attack max hits against the OSRS Wiki (snapshot {@code osrs/bosses/boss_max_hits.tsv}). */
class BossMaxHitsTest {
    @Test
    void maxHitsMatchTheWiki() throws Exception {
        List<String> wrong = new ArrayList<>();
        int checked = 0;
        for (String line : Files.readAllLines(Path.of("src/test/resources/osrs/bosses/boss_max_hits.tsv"))) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\\|");
            BossDef b = ContentRegistry.boss(f[0]);
            if (b == null) {
                wrong.add(f[0] + ": no such boss");
                continue;
            }
            List<AttackDef> all = new ArrayList<>(b.attacks);
            b.phases.forEach(p -> all.addAll(p.attacks()));
            AttackDef a = all.stream().filter(x -> x.name().equals(f[1])).findFirst().orElse(null);
            if (a == null) {
                wrong.add(f[0] + ": no attack " + f[1]);
            } else if (a.maxHit() != Integer.parseInt(f[2])) {
                wrong.add(f[0] + " " + f[1] + ": " + a.maxHit() + ", wiki " + f[2] + " (" + f[3] + ")");
            }
            checked++;
        }
        assertTrue(checked >= 70, "checked " + checked);
        assertEquals(List.of(), wrong);
    }

    /** Attacks that aren't in OSRS stay gone: the Royal Titans only stomp (their walls and blasts are scripted). */
    @Test
    void noInventedTitanAttacks() {
        for (String id : List.of("branda_the_fire_queen", "eldric_the_ice_king")) {
            assertEquals(1, ContentRegistry.boss(id).attacks.size(), id);
        }
        assertTrue(ContentRegistry.boss("dusk").attacks.stream().noneMatch(a -> a.name().equals("falling_rocks")));
        assertTrue(ContentRegistry.boss("vardorvis").attacks.stream().noneMatch(a -> a.name().equals("blood_splat")));
    }
}
