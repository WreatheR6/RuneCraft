package com.runecraft.osrs.content.raids;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Chambers of Xeric layouts (round 33): the mod's layout templates must be RuneLite's list of the OSRS layouts
 * ({@code src/test/resources/osrs/raids/cox_layouts.txt}), and every generated raid must follow one of them, with the
 * combat rooms a window of one rotation.
 */
class CoxLayoutsTest {
    private static List<String> snapshot() throws Exception {
        List<String> codes = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of("src/test/resources/osrs/raids/cox_layouts.txt"), StandardCharsets.UTF_8)) {
            if (!line.isBlank() && !line.startsWith("#")) {
                codes.add(line.split(" - ")[0].trim());
            }
        }
        return codes;
    }

    @Test
    void layoutsMatchRuneLite() throws Exception {
        assertEquals(snapshot(), RaidLayouts.COX_LAYOUTS);
        for (String code : RaidLayouts.COX_LAYOUTS) {
            String[] floors = code.split("\\.");
            assertEquals(2, floors.length, code);
            for (String floor : floors) {
                // OSRS wiki: each floor has one resource room, one or two scavenger rooms and two to four combat/puzzle rooms.
                assertEquals(1, floor.chars().filter(c -> c == 'F').count(), code);
                long scavengers = floor.chars().filter(c -> c == 'S').count();
                assertTrue(scavengers >= 1 && scavengers <= 2, code);
                long fights = floor.chars().filter(c -> c == 'C' || c == 'P').count();
                assertTrue(fights >= 2 && fights <= 4, code);
            }
            // A scavenger room always comes before the first puzzle room.
            assertTrue(code.indexOf('S') < code.indexOf('P'), code);
        }
    }

    @Test
    void generatedRaidsFollowALayout() {
        Set<String> seen = new HashSet<>();
        for (int seed = 0; seed < 400; seed++) {
            List<RoomDef> rooms = RaidLayouts.cox(new Random(seed));
            assertEquals("cox_lobby", rooms.get(0).id());
            assertEquals("olm", rooms.get(rooms.size() - 2).id());
            assertEquals("cox_reward", rooms.get(rooms.size() - 1).id());
            StringBuilder code = new StringBuilder();
            List<String> combats = new ArrayList<>();
            for (RoomDef r : rooms.subList(1, rooms.size() - 2)) {
                code.append(switch (r.kind()) {
                    case COMBAT -> 'C';
                    case PUZZLE -> 'P';
                    case RESOURCE -> r.id().equals("cox_scavengers") ? 'S' : 'F';
                    default -> '?';
                });
                if (r.kind() == RoomDef.Kind.COMBAT) {
                    combats.add(r.id());
                }
            }
            boolean matches = false;
            for (String layout : RaidLayouts.COX_LAYOUTS) {
                matches |= layout.replace(".", "").equals(code.toString());
            }
            assertTrue(matches, "seed " + seed + " made " + code);
            seen.add(code.toString());
            assertTrue(isWindow(combats), "seed " + seed + " combat rooms " + combats);
            assertEquals(combats.size(), new HashSet<>(combats).size(), "boss rooms never repeat");
        }
        assertTrue(seen.size() > 15, "layouts used: " + seen.size());
    }

    /** Whether the rooms are consecutive in one of the two rotations (either direction). */
    private static boolean isWindow(List<String> combats) {
        for (List<String> rot : RaidLayouts.COX_ROTATIONS) {
            for (int start = 0; start < rot.size(); start++) {
                for (int dir = -1; dir <= 1; dir += 2) {
                    boolean ok = true;
                    for (int i = 0; i < combats.size() && ok; i++) {
                        ok = rot.get(Math.floorMod(start + dir * i, rot.size())).equals(combats.get(i));
                    }
                    if (ok) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
