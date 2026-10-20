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

    @Test
    void challengeModeHasEveryRoomInTheWikiOrder() {
        RaidLayouts.CoxPlan plan = RaidLayouts.coxChallengePlan(new Random(4));
        List<String> ids = new ArrayList<>();
        for (RoomDef r : plan.rooms()) {
            ids.add(r.id().equals("cox_hunting") ? "cox_farming" : r.id());
        }
        // OSRS wiki "Chambers of Xeric/Challenge Mode": the same 18 rooms in the same order every run.
        assertEquals(List.of("cox_lobby",
                "tekton", "crystal_crabs", "cox_scavengers", "ice_demon", "shamans", "cox_farming",
                "vanguards", "thieving", "cox_scavengers", "vespula", "cox_farming", "tightrope",
                "guardians", "vasa", "cox_scavengers", "mystics", "muttadiles", "cox_farming",
                "olm", "cox_reward"), ids);
        assertEquals(RaidLayouts.MAX_ROOMS, plan.rooms().size());
        Set<String> all = new HashSet<>(ids);
        for (List<String> rot : RaidLayouts.COX_ROTATIONS) {
            assertTrue(all.containsAll(rot), "every boss room");
        }
        assertTrue(all.containsAll(RaidLayouts.COX_PUZZLES), "every puzzle room");
        // Deaths send a raider back to the start of their floor.
        assertEquals(0, plan.floorStart(3));
        assertEquals(7, plan.floorStart(9));
        assertEquals(13, plan.floorStart(18));
        assertEquals(19, plan.floorStart(19));
        assertTrue(plan.scout().startsWith("Floor 1: Tekton, Crystal crabs, Scavengers, Ice demon"), plan.scout());
        assertTrue(plan.scout().contains("Floor 3: Guardians"), plan.scout());
    }

    @Test
    void normalPlansSplitTheFloorsAtTheStairs() {
        RaidLayouts.CoxPlan plan = RaidLayouts.coxPlan("SCFCPC.CSPCSF", new Random(1));
        assertEquals(15, plan.rooms().size());
        // Lobby + 6 rooms on the first floor, so the second floor starts at room 7; the Olm at 13.
        assertEquals(0, plan.floorStart(6));
        assertEquals(7, plan.floorStart(7));
        assertEquals(7, plan.floorStart(12));
        assertEquals(13, plan.floorStart(13));
        assertEquals("olm", plan.rooms().get(13).id());
        assertEquals(2, plan.scout().split(" \\| ").length, plan.scout());
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
