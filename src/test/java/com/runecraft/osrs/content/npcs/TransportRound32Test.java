package com.runecraft.osrs.content.npcs;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.islands.IslandsTravel;
import com.runecraft.osrs.content.systems.Charges;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.osrs.content.systems.Teleports;
import com.runecraft.osrs.world.StructureDef;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round 32 transport: the King Black Dragon's levers and lair, the realm fairy rings (BJR, DIR, DIP, DLS, BLQ) with a ring to
 * leave from, the Enchanted Valley (BKQ), the enchanted lyre, the sailors' amulet and the memoir books' charges.
 */
class TransportRound32Test {
    private static boolean covers(StructureDef.Op op, int x, int y, int z) {
        if (x < op.x1() || x > op.x2() || y < op.y1() || y > op.y2() || z < op.z1() || z > op.z2()) {
            return false;
        }
        return op.type() != StructureDef.Op.Type.WALLS || x == op.x1() || x == op.x2() || z == op.z1() || z == op.z2();
    }

    private static StructureDef.Op last(StructureDef def, int x, int y, int z) {
        StructureDef.Op hit = null;
        for (StructureDef.Op op : def.ops()) {
            if (op.type() != StructureDef.Op.Type.FLATTEN && covers(op, x, y, z)) {
                hit = op;
            }
        }
        return hit;
    }

    private static StructureDef structure(String id) {
        for (StructureDef s : ContentRegistry.structures()) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }

    /** Feet and head at (x, y, z) are carved air and the block below is a solid, dry floor. */
    private static void standable(StructureDef def, int x, int y, int z, String what) {
        for (int up = 0; up <= 1; up++) {
            StructureDef.Op op = last(def, x, y + up, z);
            assertNotNull(op, what + " outside " + def.id());
            assertEquals(StructureDef.Op.Type.CLEAR, op.type(), what + " blocked by " + op.block());
        }
        StructureDef.Op floor = last(def, x, y - 1, z);
        assertNotNull(floor, what + " floor");
        assertTrue(floor.type() != StructureDef.Op.Type.CLEAR && !floor.block().contains("lava") && !floor.block().contains("water"),
                what + " floor " + floor.block());
    }

    /** No other registered structure builds anything inside the box (a carved cavern stands alone). */
    private static void alone(StructureDef def, int x1, int y1, int z1, int x2, int y2, int z2) {
        for (StructureDef s : ContentRegistry.structures()) {
            if (s.id().equals(def.id())) {
                continue;
            }
            for (StructureDef.Op op : s.ops()) {
                assertFalse(op.x2() >= x1 && op.x1() <= x2 && op.y2() >= y1 && op.y1() <= y2 && op.z2() >= z1 && op.z1() <= z2,
                        def.id() + " overlaps " + s.id() + ": " + op);
            }
        }
    }

    @Test
    void kingBlackDragonLeversLeadToTheLairAndBack() {
        WildernessTransport.Lever entrance = WildernessTransport.lever("kbd_lair_entrance");
        WildernessTransport.Lever lair = WildernessTransport.lever("kbd_lair");
        assertNotNull(entrance);
        assertNotNull(lair);
        assertEquals(lair.x() + "," + lair.y(), entrance.toX() + "," + entrance.toY());
        assertEquals(2271, lair.x());
        assertEquals(4680, lair.y());
        assertTrue(WildernessTransport.inCavern(entrance) && WildernessTransport.inCavern(lair));
        for (PlaceDef p : WildernessTransport.places()) {
            assertFalse(p.id().startsWith("wild_lever_kbd"), "cavern levers get no surface place: " + p.id());
        }
        // The lair: arrival beside the lever, the lever and the dragon's summoning stone, sealed off from everything else.
        StructureDef cave = structure("wild_kbd_lair");
        assertNotNull(cave);
        int[] a = WildernessTransport.cavernArrival(entrance);
        assertTrue(java.util.Arrays.equals(WildernessTransport.kbdLairArrival(), a));
        standable(cave, a[0], a[1], a[2], "KBD lair arrival");
        assertEquals(lair, WildernessTransport.leverAt(GielinorMap.toOsrsX(a[0]), GielinorMap.toOsrsY(a[2])));
        int f = WildernessTransport.KBD_LAIR_FLOOR;
        assertTrue(last(cave, StructureDef.bx(lair.x()), f + 1, StructureDef.bz(lair.y())).block().startsWith("minecraft:lever"));
        int[] st = WildernessTransport.KBD_STONE_TILE;
        assertEquals("runecraft:king_black_dragon_summoning_stone", last(cave, StructureDef.bx(st[0]), f + 1, StructureDef.bz(st[1])).block());
        assertTrue(ContentRegistry.bosses().stream().anyMatch(b -> b.id.equals("king_black_dragon") && !b.raidOnly));
        int[] t = WildernessTransport.KBD_LAIR_TILES;
        alone(cave, StructureDef.bx(t[0]) - 1, f - 2, StructureDef.bz(t[3]) - 1, StructureDef.bx(t[2]) + 2, f + 10, StructureDef.bz(t[1]) + 2);
        assertTrue(f + 10 < 34, "the lair stays deep enough to leave the sea bed alone");
        // The entrance chamber: its lever, the arrival from the lair, and a doorway into the Lava Maze ladder's shaft.
        StructureDef room = structure("wild_kbd_lever_room");
        assertNotNull(room);
        int[] lb = WildernessTransport.kbdLeverBlock();
        int rf = WildernessTransport.KBD_ROOM_FLOOR;
        assertTrue(last(room, lb[0], rf + 1, lb[1]).block().startsWith("minecraft:lever"));
        assertEquals(entrance, WildernessTransport.leverAt(GielinorMap.toOsrsX(lb[0]), GielinorMap.toOsrsY(lb[1])));
        int[] back = WildernessTransport.cavernArrival(lair);
        standable(room, back[0], back[1], back[2], "KBD chamber arrival");
        assertEquals(lair.toX(), GielinorMap.toOsrsX(back[0]));
        assertEquals(lair.toY(), GielinorMap.toOsrsY(back[2]));
        // The Lava Maze's ladder shaft: the ladder on its south side, open air next to the chamber's doorway, a floor at the
        // chamber's floor height; the chamber never builds inside the shaft.
        StructureDef maze = structure("lava_maze");
        assertNotNull(maze);
        int sx = StructureDef.bx(WildernessTransport.LAVA_MAZE_LADDER_TILE[0]) + 1;
        int sz = StructureDef.bz(WildernessTransport.LAVA_MAZE_LADDER_TILE[1]) + 1;
        assertTrue(last(maze, sx, rf + 1, sz + 1).block().startsWith("minecraft:ladder"));
        assertEquals(StructureDef.Op.Type.CLEAR, last(maze, sx + 1, rf + 1, sz).type());
        assertEquals(StructureDef.Op.Type.CLEAR, last(maze, sx + 1, rf + 2, sz).type());
        assertNotNull(last(maze, sx, rf, sz));
        assertEquals(StructureDef.Op.Type.CLEAR, last(room, sx + 2, rf + 1, sz).type());
        for (StructureDef.Op op : room.ops()) {
            assertFalse(op.x1() <= sx + 1 && op.x2() >= sx - 1 && op.z1() <= sz + 1 && op.z2() >= sz - 1, "chamber cuts the shaft: " + op);
            assertTrue(op.y2() <= StructureDef.GROUND - 4, "chamber reaches the levelled ground: " + op);
        }
    }

    @Test
    void realmRingsStandBesideTheirPlaces() {
        assertEquals(5, IslandsTravel.REALM_RINGS.size());
        for (String code : IslandsTravel.REALM_RINGS) {
            IslandsTravel.Stop s = IslandsTravel.FAIRY_RINGS.stop(code);
            assertNotNull(s, code);
            assertTrue(s.hasBlock(), code + " has a ring to leave from");
            PlaceDef p = ContentRegistry.place(s.place());
            assertNotNull(p, code + " place " + s.place());
            int x = p.blockX() + s.dx();
            int z = p.blockZ() + s.dz();
            assertFalse(com.runecraft.osrs.world.Elevation.isWater(x, z), code + " ring in water");
            // Clear of the place's NPCs and of any structure above ground.
            for (com.runecraft.osrs.content.NpcDef n : ContentRegistry.npcs()) {
                if (n.place().equals(s.place())) {
                    assertFalse(Math.abs(n.dx() - s.dx()) <= 2 && Math.abs(n.dz() - s.dz()) <= 2, code + " ring on " + n.id());
                }
            }
            for (StructureDef d : ContentRegistry.structures()) {
                for (StructureDef.Op op : d.ops()) {
                    boolean builds = op.type() != StructureDef.Op.Type.FLATTEN && op.y2() >= StructureDef.GROUND - 1;
                    assertFalse(builds && op.x2() >= x - 1 && op.x1() <= x + 1 && op.z2() >= z - 1 && op.z1() <= z + 1,
                            code + " ring cuts into " + d.id() + ": " + op);
                }
            }
        }
        assertEquals("quest_yubiusk", IslandsTravel.FAIRY_RINGS.stop("BLQ").place());
        assertNotNull(ContentRegistry.quest(IslandsTravel.YUBIUSK_QUEST));
        assertNotNull(ContentRegistry.quest("troubled_tortugans"));
    }

    @Test
    void enchantedValleyIsCarvedAtItsRing() {
        StructureDef v = structure(IslandsTravel.ENCHANTED_VALLEY);
        assertNotNull(v);
        IslandsTravel.DungeonRing r = IslandsTravel.dungeonRing("BKQ");
        assertEquals(3041, GielinorMap.toOsrsX(r.x()));
        assertEquals(4532, GielinorMap.toOsrsY(r.z()));
        int[] t = IslandsTravel.ENCHANTED_VALLEY_TILES;
        alone(v, StructureDef.bx(t[0]) - 1, r.floorY() - 2, StructureDef.bz(t[3]) - 1, StructureDef.bx(t[2]) + 2, r.floorY() + 11,
                StructureDef.bz(t[1]) + 2);
        assertTrue(r.floorY() + 11 < 34, "the valley stays deep enough to leave the sea bed alone");
        // The ring's 3x3 of mycelium and its arrival spot are on open grass (no pond or tree there).
        for (int dx = -1; dx <= 2; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                standable(v, r.x() + dx, r.floorY() + 1, r.z() + dz, "BKQ ring " + dx + "," + dz);
            }
        }
    }

    @Test
    void lyreSailorsAmuletAndMemoirs() {
        // Enchanted lyre: Rellekka; Waterbirth with the hard Fremennik Diary; Jatizso and Neitiznot with the elite one.
        assertEquals(Teleports.LYRE, Jewellery.held("enchanted_lyre").teleports());
        assertNull(Teleports.needsDiary(Teleports.LYRE.get(0)));
        assertEquals(List.of("fremennik", "HARD"), List.of(Teleports.needsDiary(Teleports.LYRE.get(1))));
        assertEquals(List.of("fremennik", "ELITE"), List.of(Teleports.needsDiary(Teleports.LYRE.get(2))));
        assertEquals(List.of("fremennik", "ELITE"), List.of(Teleports.needsDiary(Teleports.LYRE.get(3))));
        assertEquals(2, Charges.lyreCharges("raw_shark", false, false));
        assertEquals(3, Charges.lyreCharges("raw_manta_ray", false, false));
        assertEquals(4, Charges.lyreCharges("raw_sea_turtle", false, false));
        assertEquals(5, Charges.lyreCharges("raw_sea_turtle", false, true));
        assertEquals(0, Charges.lyreCharges("raw_bass", false, true));
        assertEquals(3, Charges.lyreCharges("raw_bass", true, true));
        assertEquals(0, Charges.lyreCharges("raw_lobster", true, true));
        assertEquals(Charges.ENCHANTED_LYRE_MAX, Charges.of("enchanted_lyre").max());
        assertTrue(Charges.of("enchanted_lyre").materials().isEmpty());
        assertNotNull(Charges.rechargeHint("enchanted_lyre"));
        // Kourend memoir books: law, body, mind and soul rune per charge; 100 in the memoirs, 250 in the book of the dead.
        assertEquals(100, Charges.of("kharedsts_memoirs").max());
        assertEquals(250, Charges.of("book_of_the_dead").max());
        assertEquals(Map.of("law_rune", 1, "body_rune", 1, "mind_rune", 1, "soul_rune", 1), Charges.of("book_of_the_dead").materials());
        // Sailors' amulet: ten charges per law rune and ten water runes, up to 10,000; three destinations need their marker.
        Charges.Spec sailors = Charges.of("sailors_amulet_inert");
        assertEquals(10_000, sailors.max());
        assertEquals(10, sailors.chargesPerUnit());
        assertEquals(Map.of("law_rune", 1, "water_rune", 10), sailors.materials());
        assertEquals(20, Charges.rechargeable(sailors, 0, Map.of("law_rune", 2, "water_rune", 25)));
        assertEquals(Teleports.SAILORS, Jewellery.worn("sailors_amulet_inert").teleports());
        assertFalse(Teleports.needsVisit(Teleports.SAILORS.get(0)));
        for (Teleports.Destination d : Teleports.SAILORS.subList(1, 4)) {
            assertTrue(Teleports.needsVisit(d), d.name());
        }
        for (String id : List.of("enchanted_lyre", "kharedsts_memoirs", "book_of_the_dead", "sailors_amulet_inert")) {
            assertTrue(com.runecraft.osrs.ItemCatalog.exists(id), id);
        }
    }

    @Test
    void obeliskDestinationsCanBeChosen() {
        WildernessTransport.Obelisk first = WildernessTransport.nextChoice(null);
        assertEquals(WildernessTransport.OBELISKS.get(0), first);
        WildernessTransport.Obelisk o = first;
        for (int i = 1; i < WildernessTransport.OBELISKS.size(); i++) {
            o = WildernessTransport.nextChoice(o);
            assertEquals(WildernessTransport.OBELISKS.get(i), o);
        }
        assertEquals(first, WildernessTransport.nextChoice(o));
    }
}
