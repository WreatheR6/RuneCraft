package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.world.NpcStoreys;
import com.runecraft.osrs.world.StructureDef;
import com.runecraft.osrs.world.TerrainAnchors;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Fortis Colosseum's underground lobby: wiki map pins, carved air where people stand, rock cover above. */
class ColosseumLobbyTest {
    /** The lobby's blocks after its ops are applied in order (absent = untouched rock). */
    private static Map<Long, String> blocks() {
        StructureDef def = ContentRegistry.structures().stream().filter(s -> s.id().equals(ColosseumLobby.ID)).findFirst().orElse(null);
        assertNotNull(def, "the lobby structure is registered");
        Map<Long, String> out = new HashMap<>();
        for (StructureDef.Op op : def.ops()) {
            for (int x = op.x1(); x <= op.x2(); x++) {
                for (int z = op.z1(); z <= op.z2(); z++) {
                    for (int y = op.y1(); y <= op.y2(); y++) {
                        boolean edge = x == op.x1() || x == op.x2() || z == op.z1() || z == op.z2();
                        switch (op.type()) {
                            case FILL -> out.put(key(x, y, z), op.block());
                            case CLEAR -> out.put(key(x, y, z), "minecraft:air");
                            case WALLS -> {
                                if (edge) {
                                    out.put(key(x, y, z), op.block());
                                }
                            }
                            default -> throw new AssertionError("unexpected op " + op.type());
                        }
                    }
                }
            }
        }
        return out;
    }

    private static long key(int x, int y, int z) {
        return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | (y & 0xFFF);
    }

    private static boolean air(Map<Long, String> m, int x, int y, int z) {
        return "minecraft:air".equals(m.get(key(x, y, z)));
    }

    private static boolean standable(Map<Long, String> m, int[] s) {
        String floor = m.get(key(s[0], s[1] - 1, s[2]));
        return air(m, s[0], s[1], s[2]) && air(m, s[0], s[1] + 1, s[2]) && floor != null && !floor.equals("minecraft:air");
    }

    @Test
    void peopleStandOnCarvedFloorAtTheirWikiTiles() {
        Map<Long, String> m = blocks();
        // The stairs' arrival tile, Minimus, Gloria and the lobby's other NPCs.
        assertTrue(standable(m, ColosseumLobby.stand(FortisColosseum.LOBBY_X, FortisColosseum.LOBBY_Y)), "stairs arrival");
        PlaceDef lobby = ContentRegistry.place(FortisColosseum.LOBBY_PLACE);
        assertNotNull(lobby);
        assertEquals(PlaceDef.Style.NONE, lobby.style());
        for (String id : new String[]{FortisColosseum.MINIMUS, ColosseumLobby.GLORIA, ColosseumLobby.UEMAN, ColosseumLobby.SEIA,
                ColosseumLobby.GLADIATOR, ColosseumLobby.GLADIATOR_2, ColosseumLobby.GUARD, ColosseumLobby.GUARD_2}) {
            NpcDef npc = ContentRegistry.npc(id);
            assertNotNull(npc, id);
            assertEquals(FortisColosseum.LOBBY_PLACE, npc.place(), id);
            Integer feet = NpcStoreys.undergroundFeetY(id);
            assertEquals(ColosseumLobby.FEET, feet, id);
            int[] s = {lobby.blockX() + npc.dx(), feet, lobby.blockZ() + npc.dz()};
            assertTrue(standable(m, s), id + " at " + s[0] + "," + s[1] + "," + s[2]);
        }
        // Minimus and Gloria at their wiki pins.
        NpcDef minimus = ContentRegistry.npc(FortisColosseum.MINIMUS);
        int[] want = ColosseumLobby.offset(FortisColosseum.MINIMUS_X, FortisColosseum.MINIMUS_Y);
        assertEquals(want[0], minimus.dx());
        assertEquals(want[1], minimus.dz());
        NpcDef gloria = ContentRegistry.npc(ColosseumLobby.GLORIA);
        want = ColosseumLobby.offset(FortisColosseum.GLORIA_X, FortisColosseum.GLORIA_Y);
        assertEquals(want[0], gloria.dx());
        assertEquals(want[1], gloria.dz());
    }

    @Test
    void objectsStandAtTheirWikiTiles() {
        Map<Long, String> m = blocks();
        int[] s = ColosseumLobby.stand(FortisColosseum.SCOREBOARD_X, FortisColosseum.SCOREBOARD_Y);
        assertEquals("runecraft:" + FortisColosseum.SCOREBOARD, m.get(key(s[0], s[1], s[2])));
        s = ColosseumLobby.stand(FortisColosseum.BANK_CHEST_X, FortisColosseum.BANK_CHEST_Y);
        assertEquals("runecraft:" + FortisColosseum.BANK_CHEST, m.get(key(s[0], s[1], s[2])));
        s = ColosseumLobby.stand(FortisColosseum.ARENA_ENTRANCE_X, FortisColosseum.ARENA_ENTRANCE_Y);
        assertEquals("runecraft:" + FortisColosseum.ARENA_ENTRANCE, m.get(key(s[0], s[1], s[2])));
        s = ColosseumLobby.stand(1797, FortisColosseum.LOBBY_Y);
        assertEquals("runecraft:" + FortisColosseum.LOBBY_EXIT, m.get(key(s[0], s[1], s[2])));
        // Every block the lobby places exists in the minigames module (or is vanilla).
        java.util.Set<String> module = new java.util.HashSet<>();
        for (var b : new MinigamesContent().blocks()) {
            module.add("runecraft:" + b.id());
        }
        for (String b : m.values()) {
            if (b.startsWith("runecraft:")) {
                assertTrue(module.contains(b), b);
            }
        }
        // The scoreboard and bank chest moved off the surface entrance.
        assertTrue(!MinigameWorld.placeBlocks().get(Minigame.FORTIS_COLOSSEUM.place()).contains(FortisColosseum.SCOREBOARD));
    }

    @Test
    void theLobbyLiesUnderTheColosseumWithRockCover() {
        StructureDef def = ContentRegistry.structures().stream().filter(s -> s.id().equals(ColosseumLobby.ID)).findFirst().orElseThrow();
        int top = 0;
        for (StructureDef.Op op : def.ops()) {
            top = Math.max(top, op.y2());
        }
        assertTrue(top < TerrainAnchors.UNDERGROUND_BELOW, "the lobby is a dungeon room (top " + top + ")");
        // Under the Colosseum (surface tiles 1790-1833, 3085-3118): the main hall's centre.
        int[] h = ColosseumLobby.HALLS[0];
        int cx = (h[0] + h[2]) / 2;
        int cy = (h[1] + h[3]) / 2 - FortisColosseum.UNDERGROUND_OFFSET;
        assertTrue(cx >= 1790 && cx <= 1833 && cy >= 3085 && cy <= 3118, cx + "," + cy);
    }
}
