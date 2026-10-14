package com.runecraft.osrs.data;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.CombatProfile;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.slayer.SlayerTasks;
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
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Monster attributes, size, poison/venom immunity and Slayer XP against {@code src/test/resources/osrs/monster_traits.tsv},
 * a snapshot of the wiki's DPS calculator data (weirdgloop/osrs-dps-calc monsters.json) and monster infoboxes
 * (Kiyogitpy/osrs-wiki-data, July 2026) for every RuneCraft creature with a wiki counterpart.
 */
class MonsterTraitsTest {
    private static List<String[]> snapshot() throws IOException {
        String fixture = "osrs/monster_traits.tsv";
        Path p = Path.of("src/test/resources", fixture);
        List<String> lines;
        if (Files.exists(p)) {
            lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        } else {
            try (InputStream in = MonsterTraitsTest.class.getClassLoader().getResourceAsStream(fixture)) {
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

    private static Set<String> attrs(String s) {
        Set<String> out = new TreeSet<>();
        for (String a : s.split(",")) {
            if (!a.isEmpty()) {
                out.add(a);
            }
        }
        return out;
    }

    @Test
    void tableMatchesTheWikiSnapshot() throws IOException {
        List<String[]> rows = snapshot();
        assertTrue(rows.size() >= 650, "only " + rows.size() + " snapshot rows");
        assertEquals(rows.size(), MonsterTraits.rows().size(), "every table row is in the snapshot");
        List<String> bad = new ArrayList<>();
        for (String[] f : rows) {
            MonsterTraits.Traits t = MonsterTraits.get(f[0]);
            if (t == null) {
                bad.add(f[0] + " missing");
                continue;
            }
            String want = f[5] + " " + attrs(f[6]) + " poison " + f[7].equals("yes") + " venom " + f[8].equals("yes") + " xp "
                    + (f[9].isEmpty() ? 0 : Double.parseDouble(f[9]));
            String got = t.size() + " " + new TreeSet<>(t.attributes()) + " poison " + t.poisonImmune() + " venom " + t.venomImmune()
                    + " xp " + t.slayerXp();
            if (!want.equals(got)) {
                bad.add(f[0] + " (" + f[1] + "): wiki " + want + ", RuneCraft " + got);
            }
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }

    @Test
    void creaturesCarryTheirTraits() throws IOException {
        Map<String, CombatProfile<?>> all = new HashMap<>();
        for (MonsterDef m : ContentRegistry.monsters()) {
            all.put(m.id, m);
        }
        for (BossDef b : ContentRegistry.bosses()) {
            all.put(b.id, b);
        }
        List<String> bad = new ArrayList<>();
        int checked = 0;
        for (String[] f : snapshot()) {
            CombatProfile<?> p = all.get(f[0]);
            if (p == null) {
                continue;
            }
            checked++;
            if (!new TreeSet<>(p.attributes).equals(attrs(f[6])) || p.tiles != Integer.parseInt(f[5])
                    || p.poisonImmune != f[7].equals("yes") || p.venomImmune != f[8].equals("yes")) {
                bad.add(f[0] + ": wiki " + attrs(f[6]) + " size " + f[5] + ", RuneCraft " + p.attributes + " size " + p.tiles);
            }
        }
        assertTrue(checked >= 600, "only " + checked + " creatures compared");
        assertTrue(bad.isEmpty(), String.join("\n", bad));
        // Built-in monsters and bosses are keyed by enum name / boss id.
        for (Monster m : Monster.values()) {
            if (m == Monster.VILLAGER || m == Monster.WITCH || m == Monster.SHEEP || m == Monster.PIG) {
                continue; // vanilla stand-ins, or no combat stats on the wiki calculator
            }
            assertNotNull(MonsterTraits.get(MonsterTraits.key(m)), m.name());
        }
        for (Boss b : Boss.values()) {
            assertNotNull(MonsterTraits.get(b.entityId()), b.name());
        }
    }

    @Test
    void gameplayExamples() {
        assertTrue(MonsterTraits.get("Monster.ZOMBIE").attributes().contains("undead"));
        assertTrue(MonsterTraits.get("Monster.LESSER_DEMON").attributes().contains("demon"));
        assertTrue(MonsterTraits.get("Monster.GIANT_RAT").attributes().contains("rat"));
        assertTrue(MonsterTraits.get("kril_tsutsaroth").attributes().contains("demon"));
        assertTrue(MonsterTraits.poisonImmune("kril_tsutsaroth"));
        assertTrue(MonsterTraits.venomImmune("zulrah"));
        assertFalse(MonsterTraits.poisonImmune("Monster.GOBLIN"));
        assertTrue(ContentRegistry.monster("vyrewatch_sentinel").hasAttribute("vampyre"));
        assertTrue(ContentRegistry.monster("tortured_soul").hasAttribute("spectral"));
        // Slayer XP differs from the hitpoints for dragons and bosses.
        assertEquals(363.0, MonsterTraits.slayerXp("rune_dragon", 330));
        assertEquals(350.5, MonsterTraits.slayerXp("kril_tsutsaroth", 255));
        assertEquals(2, MonsterTraits.size("black_bear"));
        // The vampyres task has creatures with spawn points now (Meiyerditch juveniles, Darkmeyer sentinels).
        assertTrue(SlayerTasks.available("vampyres"));
        assertEquals(List.of(com.runecraft.osrs.Region.MORYTANIA), SlayerTasks.regions("vampyres"));
    }
}
