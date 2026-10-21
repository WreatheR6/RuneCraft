package com.runecraft.osrs.data;

import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Monster aggressiveness and the built-in monsters' max hits against {@code src/test/resources/osrs/monster_aggression.tsv}, a
 * snapshot of the wiki monster infoboxes (Kiyogitpy/osrs-wiki-data, July 2026) for every row of {@code monster_traits.tsv}
 * (round 42).
 */
class MonsterAggressionTest {
    private static List<String[]> snapshot() throws IOException {
        String fixture = "osrs/monster_aggression.tsv";
        Path p = Path.of("src/test/resources", fixture);
        List<String> lines;
        if (Files.exists(p)) {
            lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        } else {
            try (InputStream in = MonsterAggressionTest.class.getClassLoader().getResourceAsStream(fixture)) {
                assertNotNull(in, "missing " + fixture);
                lines = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
            }
        }
        List<String[]> out = new ArrayList<>();
        for (String l : lines) {
            if (!l.isBlank() && !l.startsWith("#") && !l.startsWith("key\t")) {
                out.add(l.split("\t", -1));
            }
        }
        return out;
    }

    @Test
    void tableMatchesTheWikiSnapshot() throws IOException {
        Map<String, Boolean> want = new HashMap<>();
        for (String[] r : snapshot()) {
            if (r[4].equals("yes") || r[4].equals("no")) {
                want.put(r[0], r[4].equals("yes"));
            }
        }
        assertTrue(want.size() > 600, "definite rows: " + want.size());
        assertEquals(want, MonsterAggression.rows());
    }

    @Test
    void everySnapshotRowIsATraitsRow() throws IOException {
        for (String[] r : snapshot()) {
            assertNotNull(MonsterTraits.get(r[0]), r[0] + " has no monster_traits row");
        }
    }

    @Test
    void builtInMonstersMatchTheWiki() throws IOException {
        int checked = 0;
        for (String[] r : snapshot()) {
            if (!r[0].startsWith("Monster.")) {
                continue;
            }
            Monster m = Monster.valueOf(r[0].substring("Monster.".length()));
            if (!r[4].isEmpty()) {
                assertEquals(r[4].equals("yes"), m.aggressive(), r[0] + " aggressive (wiki: " + r[6] + ")");
            }
            if (!r[5].isEmpty()) {
                assertEquals(Integer.parseInt(r[5]), m.maxHit(), r[0] + " max hit");
                checked++;
            }
        }
        assertTrue(checked >= 18, "built-in max hits checked: " + checked);
        // the witch stands in for the level 20 dark wizard (no traits row): wiki max hit 6, aggressive
        assertEquals(6, Monster.WITCH.maxHit());
        assertTrue(Monster.WITCH.aggressive());
    }

    @Test
    void vanillaStandInsFollowTheWiki() {
        // round 43: the vanilla stand-ins read the wiki rows, the hand-read ones where the generator has none
        for (Monster m : Monster.values()) {
            if (m.isCustom()) {
                continue;
            }
            Boolean wiki = MonsterAggression.wiki(MonsterTraits.key(m));
            Boolean hand = MonsterAggression.standIns().get(MonsterTraits.key(m));
            assertFalse(wiki != null && hand != null, m + " has both a wiki row and a hand-read row");
            assertTrue(wiki != null || hand != null, m + " (a vanilla stand-in) has no wiki aggressiveness");
            assertEquals(wiki != null ? wiki : hand, MonsterAggression.aggressive(m), m.name());
            assertEquals(m.aggressive(), MonsterAggression.aggressive(m), m + "'s own flag disagrees with the wiki");
        }
        assertFalse(MonsterAggression.aggressive(Monster.COW));
        assertFalse(MonsterAggression.aggressive(Monster.CHICKEN));
        assertFalse(MonsterAggression.aggressive(Monster.VILLAGER), "the villager is a Man");
        assertTrue(MonsterAggression.aggressive(Monster.WITCH), "the witch is a level 20 dark wizard");
        assertTrue(MonsterAggression.aggressive(Monster.SKELETON));
        assertTrue(MonsterAggression.aggressive(Monster.GIANT_SPIDER));
        assertFalse(MonsterAggression.aggressive(Monster.SPIDER));
    }

    @Test
    void bossesTheWikiCallsPassive() {
        // round 43: these bosses only fight once disturbed (summoned at their stone) or attacked
        for (String id : List.of("kraken", "giant_mole", "abyssal_sire", "vorkath", "the_nightmare", "thermonuclear_smoke_devil",
                "dusk", "dawn", "vardorvis", "sotetseg", "delrith")) {
            assertEquals(Boolean.FALSE, MonsterAggression.wiki(id), id);
        }
        for (String id : List.of("zulrah", "cerberus", "alchemical_hydra", "general_graardor", "corporeal_beast", "callisto",
                "king_black_dragon", "sarachnis", "scurrius", "phosanis_nightmare")) {
            assertEquals(Boolean.TRUE, MonsterAggression.wiki(id), id);
        }
    }

    @Test
    void slayerCreaturesThatTheWikiCallsPassiveArePassive() {
        // the module defaults made these aggressive; the wiki infobox says No
        for (String id : List.of("abyssal_demon", "nechryael", "dust_devil", "smoke_devil", "bloodveld", "basilisk", "turoth")) {
            MonsterDef def = ContentRegistry.monster(id);
            assertNotNull(def, id);
            assertFalse(MonsterAggression.aggressive(id, def.aggressive), id);
        }
        for (String id : List.of("dark_beast", "lizardman_shaman")) {
            assertTrue(MonsterAggression.aggressive(id, false), id);
        }
        // conditional wiki text keeps the creature's own flag (God Wars followers, Vyrewatch, demonic gorillas)
        assertEquals(null, MonsterAggression.wiki("vyrewatch_sentinel"));
        assertTrue(MonsterAggression.aggressive("vyrewatch_sentinel", true));
    }
}
