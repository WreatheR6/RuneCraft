package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.Skill;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Diffs the agility shortcuts against a snapshot of the wiki's "Shortcuts" page (July 2026 dump: the "List of shortcuts"
 * and "List of Agility Areas" tables, {@code agility_shortcuts.tsv}): every shortcut is a wiki row with the same Agility
 * level, the same other skill levels (grapples' Strength/Ranged) and the same success XP.
 */
class AgilityShortcutSnapshotTest {
    record Row(String kind, int agility, Map<Skill, Integer> other, String name, String location, double xp) {
    }

    static Map<String, Row> snapshot() throws Exception {
        Map<String, Row> rows = new HashMap<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                AgilityShortcutSnapshotTest.class.getResourceAsStream("/osrs/skilling/agility_shortcuts.tsv"), StandardCharsets.UTF_8))) {
            r.readLine();
            for (String line; (line = r.readLine()) != null; ) {
                String[] p = line.split("\t");
                Map<Skill, Integer> other = new HashMap<>();
                if (!p[2].equals("-")) {
                    for (String kv : p[2].split(",")) {
                        String[] e = kv.split("=");
                        other.put(Skill.valueOf(e[0].toUpperCase(Locale.ROOT)), Integer.parseInt(e[1]));
                    }
                }
                rows.putIfAbsent(p[3], new Row(p[0], Integer.parseInt(p[1]), other, p[3], p[4], xp(p[5])));
            }
        }
        return rows;
    }

    /** The success XP of a wiki XP cell ("3 (1 on failure)" -> 3, "5 × 2" -> 10, "-" -> 0). */
    static double xp(String cell) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([0-9.]+)(?:\\s*×\\s*([0-9]+))?").matcher(cell);
        if (!m.find()) {
            return 0;
        }
        return Double.parseDouble(m.group(1)) * (m.group(2) == null ? 1 : Integer.parseInt(m.group(2)));
    }

    private static List<AgilityShortcuts.Shortcut> all() {
        List<AgilityShortcuts.Shortcut> all = new ArrayList<>(AgilityShortcuts.SHORTCUTS);
        all.addAll(AgilityShortcuts.INDOOR);
        return all;
    }

    @Test
    void everyShortcutMatchesTheWikiList() throws Exception {
        Map<String, Row> wiki = snapshot();
        assertTrue(wiki.size() >= 160, "snapshot rows: " + wiki.size());
        List<String> problems = new ArrayList<>();
        for (AgilityShortcuts.Shortcut s : all()) {
            String name = AgilityShortcuts.WIKI.get(s.id());
            if (name == null) {
                if (!AgilityShortcuts.UNLISTED.contains(s.id())) {
                    problems.add(s.id() + ": no wiki shortcut mapped");
                }
                continue;
            }
            Row row = wiki.get(name);
            if (row == null) {
                problems.add(s.id() + ": wiki row '" + name + "' missing from the snapshot");
                continue;
            }
            if (row.agility() != s.level()) {
                problems.add(s.id() + ": Agility " + s.level() + " but the wiki says " + row.agility());
            }
            if (!row.other().equals(s.other())) {
                problems.add(s.id() + ": other levels " + s.other() + " but the wiki says " + row.other());
            }
            if (Math.abs(row.xp() - s.xp()) > 1e-9) {
                problems.add(s.id() + ": " + s.xp() + " XP but the wiki says " + row.xp());
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void mappingIsConsistent() {
        Map<String, AgilityShortcuts.Shortcut> ids = new HashMap<>();
        for (AgilityShortcuts.Shortcut s : all()) {
            assertFalse(ids.containsKey(s.id()), "duplicate shortcut " + s.id());
            ids.put(s.id(), s);
        }
        for (String id : AgilityShortcuts.WIKI.keySet()) {
            assertNotNull(ids.get(id), "WIKI maps unknown shortcut " + id);
            assertFalse(AgilityShortcuts.UNLISTED.contains(id), id + " is both mapped and unlisted");
        }
        // Round 30: the wiki's surface shortcuts in built areas (was 23 surface shortcuts).
        assertTrue(AgilityShortcuts.SHORTCUTS.size() >= 61, "surface shortcuts: " + AgilityShortcuts.SHORTCUTS.size());
        // Fixed levels: the Karamja (east of Shilo) stepping stones are 30, not 74; the Burgh de Rott fence 25.
        assertEquals(30, AgilityShortcuts.at(2925, 2952).level());
        assertEquals(25, AgilityShortcuts.at(3482, 3231).level());
        assertEquals(65, AgilityShortcuts.at(3423, 3476).level());
        assertEquals(49, AgilityShortcuts.at(1776, 3884).level());
    }

    /** Lists the wiki shortcuts the mod lacks (most are in dungeons or areas that aren't built). */
    @Test
    void reportMissing() throws Exception {
        Map<String, Row> wiki = snapshot();
        Map<String, Integer> missing = new TreeMap<>();
        java.util.Set<String> mapped = new java.util.HashSet<>(AgilityShortcuts.WIKI.values());
        for (Row row : wiki.values()) {
            if (!mapped.contains(row.name())) {
                missing.put(row.name(), row.agility());
            }
        }
        System.out.println("Agility shortcuts: " + mapped.size() + " of " + wiki.size() + " wiki shortcuts modelled; missing " + missing);
        assertTrue(mapped.size() >= 70, "modelled: " + mapped.size());
    }
}
