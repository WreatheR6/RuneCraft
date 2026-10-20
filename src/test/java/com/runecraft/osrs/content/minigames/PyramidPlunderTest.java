package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.content.minigames.MinigameRules.PyramidPlunder;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pyramid Plunder against the OSRS Wiki (July 2026 dump): the Grand Gold Chest's and the sarcophagus's tables and sceptre rates
 * diffed against {@code src/test/resources/osrs/minigames/pyramid_plunder_loot.tsv}, the XP tables, urn counts and the success
 * charts.
 */
class PyramidPlunderTest {
    private static String id(String wiki) {
        return wiki.toLowerCase().replace("'", "").replace(' ', '_');
    }

    @Test
    void chestAndSarcophagusMatchTheWiki() throws Exception {
        List<String> lines = Files.readAllLines(Path.of("src/test/resources/osrs/minigames/pyramid_plunder_loot.tsv"), StandardCharsets.UTF_8);
        List<String> chest = new ArrayList<>();
        List<String> sarcophagus = new ArrayList<>();
        int[] chestSceptre = new int[8];
        int[] sarcSceptre = new int[8];
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] r = line.split("\t");
            int room = Integer.parseInt(r[1]);
            int rate = Integer.parseInt(r[3].split("/")[1]);
            if (r[2].equals("Pharaoh's sceptre")) {
                (r[0].equals("chest") ? chestSceptre : sarcSceptre)[room - 1] = rate;
                continue;
            }
            assertTrue(rate == 2 || rate == 4, line);
            (r[0].equals("chest") ? chest : sarcophagus).add(room + " " + id(r[2]) + " " + 4 / rate);
        }
        assertEquals(chest, flatten(PyramidPlunder.CHEST));
        assertEquals(sarcophagus, flatten(PyramidPlunder.SARCOPHAGUS));
        assertArrayEquals(chestSceptre, PyramidPlunder.SCEPTRE_CHANCE);
        assertArrayEquals(sarcSceptre, PyramidPlunder.SCEPTRE_CHANCE);
    }

    private static List<String> flatten(List<List<PyramidPlunder.Artefact>> table) {
        List<String> out = new ArrayList<>();
        for (int r = 0; r < table.size(); r++) {
            for (PyramidPlunder.Artefact a : table.get(r)) {
                out.add((r + 1) + " " + a.item() + " " + a.weight());
            }
        }
        return out;
    }

    @Test
    void experienceAndRooms() {
        assertArrayEquals(new int[]{21, 31, 41, 51, 61, 71, 81, 91}, PyramidPlunder.ROOM_LEVELS);
        assertArrayEquals(new double[]{60, 90, 150, 215, 300, 450, 675, 825}, PyramidPlunder.URN_XP);
        assertArrayEquals(new double[]{40, 60, 100, 140, 200, 300, 450, 550}, PyramidPlunder.CHEST_XP);
        assertArrayEquals(new double[]{20, 30, 50, 70, 100, 150, 225, 275}, PyramidPlunder.SARCOPHAGUS_XP);
        assertArrayEquals(new double[]{40, 60, 100, 140, 200, 300, 450, 0}, PyramidPlunder.DOOR_XP);
        assertEquals(10, PyramidPlunder.SPEAR_XP);
        assertArrayEquals(new int[]{13, 13, 12, 13, 13, 13, 13, 13}, PyramidPlunder.URNS);
        assertEquals(4, PyramidPlunder.DOORS);
        assertEquals(500, PyramidPlunder.GAME_TICKS, "five minutes");
        for (List<List<PyramidPlunder.Artefact>> t : List.of(PyramidPlunder.CHEST, PyramidPlunder.SARCOPHAGUS, PyramidPlunder.URN)) {
            assertEquals(8, t.size());
            for (List<PyramidPlunder.Artefact> room : t) {
                assertEquals(4, room.stream().mapToInt(PyramidPlunder.Artefact::weight).sum());
                for (PyramidPlunder.Artefact a : room) {
                    assertTrue(java.util.Arrays.stream(MinigameItems.ARTEFACTS).anyMatch(row -> row[0].equals(a.item())), a.item());
                }
            }
        }
        assertEquals("ivory_comb", PyramidPlunder.artefact(PyramidPlunder.URN, 1, 0.0), "ivory combs come from rooms 1-2");
    }

    @Test
    void successCharts() {
        // Urns: 100-180 in room 1, 1/256 less every room.
        assertEquals(101 / 256.0, PyramidPlunder.urnChance(1, 1), 1e-9);
        assertEquals(181 / 256.0, PyramidPlunder.urnChance(99, 1), 1e-9);
        assertEquals(174 / 256.0, PyramidPlunder.urnChance(99, 8), 1e-9);
        assertTrue(PyramidPlunder.doorChance(60, 3, true) > PyramidPlunder.doorChance(60, 3, false));
        // Sarcophagus: low 80 - room level, high 270 - room level; impossible at Strength 1 in room 7.
        assertEquals(250 / 256.0, PyramidPlunder.sarcophagusChance(99, 1), 1e-9);
        assertEquals(0.0, PyramidPlunder.sarcophagusChance(1, 7), 1e-9);
        assertEquals(1 / 9.0, PyramidPlunder.mummyChance(1), 1e-9);
        assertEquals(8 / 9.0, PyramidPlunder.mummyChance(8), 1e-9);
        assertEquals(221 / 256.0, PyramidPlunder.chestChance(99), 1e-9);
    }
}
