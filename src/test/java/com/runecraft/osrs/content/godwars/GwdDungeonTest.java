package com.runecraft.osrs.content.godwars;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.godwars.GodWarsFactions.God;
import com.runecraft.osrs.content.godwars.GwdDungeon.Area;
import com.runecraft.osrs.content.godwars.GwdDungeon.Passage;
import com.runecraft.osrs.content.godwars.GwdDungeon.Spot;
import com.runecraft.osrs.world.StructureDef;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The God Wars Dungeon at its OSRS tiles: the traced floor plan's areas, the obstacles between them, the generals'
 * chambers, altars and spawn tiles, the follower spawns against the cache snapshot and the god items against the wiki.
 */
class GwdDungeonTest {
    private static List<String[]> tsv(String name) throws IOException {
        List<String[]> out = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of("src/test/resources/osrs/godwars/" + name))) {
            if (!line.isBlank() && !line.startsWith("#")) {
                out.add(line.split("\t"));
            }
        }
        return out;
    }

    @Test
    void everyAreaIsTracedAndTheAreasDontTouch() {
        Map<Area, Integer> min = new HashMap<>();
        min.put(Area.MAIN, 2000);
        min.put(Area.ZAMORAK_FORTRESS, 800);
        min.put(Area.BANDOS_STRONGHOLD, 800);
        min.put(Area.ARMADYL_EYRIE, 800);
        min.put(Area.SARADOMIN_ENCAMPMENT, 150);
        min.put(Area.KREEARRA_CHAMBER, 200);
        min.put(Area.GRAARDOR_CHAMBER, 200);
        min.put(Area.ZILYANA_CHAMBER, 300);
        min.put(Area.KRIL_CHAMBER, 250);
        min.put(Area.ANCIENT_PRISON, 800);
        min.put(Area.NEX_CHAMBER, 400);
        min.put(Area.PRISON_ENTRANCE, 40);
        min.put(Area.PRISON_REFUGE, 40);
        for (Area a : Area.values()) {
            for (Spot s : a.seeds()) {
                assertEquals(a, GwdDungeon.areaOf(s.plane(), s.x(), s.y()), a + " seed " + s);
            }
            assertTrue(GwdDungeon.size(a) >= min.get(a), a + " has " + GwdDungeon.size(a) + " tiles");
        }
        // Every floor tile belongs to one area (no stray pockets), and no two areas touch without a wall between them.
        for (int p = 0; p < 3; p++) {
            for (char c : new char[]{'o', 'w', 'n', 'b'}) {
                for (Spot s : GwdDungeon.tilesOf(p, c)) {
                    Area a = GwdDungeon.areaOf(p, s.x(), s.y());
                    assertNotNull(a, "stray floor " + s);
                    int[][] n = {{1, 0}, {0, 1}};
                    for (int[] d : n) {
                        Area b = GwdDungeon.areaOf(p, s.x() + d[0], s.y() + d[1]);
                        if (b != null && b != a) {
                            assertTrue(GwdDungeon.blockedEdge(p, s.x(), s.y(), s.x() + d[0], s.y() + d[1]), a + " touches " + b + " at " + s);
                        }
                    }
                }
            }
        }
    }

    /** Each obstacle joins the two areas it does in OSRS, and everything is reachable from the bottom of the rope. */
    @Test
    void passagesJoinTheRightAreasAndReachEverything() {
        Map<String, Area[]> expected = new HashMap<>();
        expected.put("bandos_big_door", new Area[]{Area.MAIN, Area.BANDOS_STRONGHOLD});
        expected.put("armadyl_pillar", new Area[]{Area.MAIN, Area.ARMADYL_EYRIE});
        expected.put("saradomin_upper_rock", new Area[]{Area.MAIN, Area.SARADOMIN_ENCAMPMENT});
        expected.put("saradomin_lower_rock", new Area[]{Area.SARADOMIN_ENCAMPMENT, Area.SARADOMIN_ENCAMPMENT});
        expected.put("zamorak_ice_bridge", new Area[]{Area.MAIN, Area.ZAMORAK_FORTRESS});
        expected.put("kreearra_door", new Area[]{Area.ARMADYL_EYRIE, Area.KREEARRA_CHAMBER});
        expected.put("graardor_door", new Area[]{Area.BANDOS_STRONGHOLD, Area.GRAARDOR_CHAMBER});
        expected.put("zilyana_door", new Area[]{Area.SARADOMIN_ENCAMPMENT, Area.ZILYANA_CHAMBER});
        expected.put("kril_door", new Area[]{Area.ZAMORAK_FORTRESS, Area.KRIL_CHAMBER});
        expected.put("frozen_door", new Area[]{Area.MAIN, Area.PRISON_ENTRANCE});
        expected.put("prison_door", new Area[]{Area.PRISON_ENTRANCE, Area.ANCIENT_PRISON});
        expected.put("refuge_door", new Area[]{Area.ANCIENT_PRISON, Area.PRISON_REFUGE});
        expected.put("ancient_barrier", new Area[]{Area.PRISON_REFUGE, Area.NEX_CHAMBER});
        assertEquals(expected.keySet().size(), GwdDungeon.passages().size());
        Map<Area, List<Area>> graph = new HashMap<>();
        for (Passage p : GwdDungeon.passages()) {
            Area[] e = expected.get(p.id());
            assertNotNull(e, p.id());
            assertEquals(e[0], area(p.a()), p.id() + " outer side " + p.a());
            assertEquals(e[1], area(p.b()), p.id() + " inner side " + p.b());
            graph.computeIfAbsent(e[0], k -> new ArrayList<>()).add(e[1]);
            graph.computeIfAbsent(e[1], k -> new ArrayList<>()).add(e[0]);
            // Every block stands next to one of the passage's two landing spots, on the same level.
            for (Spot s : p.blocks()) {
                boolean near = false;
                for (Spot land : List.of(p.a(), p.b())) {
                    near |= land.plane() == s.plane() && Math.abs(land.x() - s.x()) <= 2 && Math.abs(land.y() - s.y()) <= 2;
                }
                assertTrue(near, p.id() + " block " + s + " is not by a landing spot");
                assertEquals(p, GwdDungeon.passageAt(s.blockX() + (p.westEdge() ? 0 : 1), GwdDungeon.floorY(s.plane()) + 1,
                        s.blockZ() + 1), p.id());
            }
            if (p.kind() == GwdDungeon.Kind.CHAMBER_DOOR) {
                assertEquals(p, GwdDungeon.chamberDoor(p.god()));
                assertEquals(p.god(), e[1].god(), p.id());
                assertTrue(e[1].chamber());
            }
        }
        Set<Area> seen = EnumSet.of(area(GwdDungeon.LANDING));
        ArrayDeque<Area> q = new ArrayDeque<>(seen);
        while (!q.isEmpty()) {
            for (Area n : graph.getOrDefault(q.poll(), List.of())) {
                if (seen.add(n)) {
                    q.add(n);
                }
            }
        }
        assertEquals(EnumSet.allOf(Area.class), seen);
        assertEquals(Area.MAIN, area(GwdDungeon.LANDING));
        assertEquals(Area.MAIN, area(new Spot(2, GwdDungeon.ROPE.x() + 1, GwdDungeon.ROPE.y())));
    }

    private static Area area(Spot s) {
        Area a = GwdDungeon.areaOf(s.plane(), s.x(), s.y());
        assertNotNull(a, "no area at " + s);
        assertTrue(GwdDungeon.walkable(s.plane(), s.x(), s.y()));
        return a;
    }

    /** The generals and bodyguards stand on their OSRS spawn tiles in their chambers; each chamber has its god's altar. */
    @Test
    void generalsBodyguardsAndAltars() {
        Map<Area, Integer> generals = new HashMap<>();
        Map<Area, Integer> guards = new HashMap<>();
        for (GwdDungeon.Lair l : GwdDungeon.lairs()) {
            assertEquals(l.area(), area(l.spot()), l.creature());
            if (l.general()) {
                generals.merge(l.area(), 1, Integer::sum);
                assertEquals(l.area().god(), GodWarsFactions.generalGod(l.creature()), l.creature());
            } else {
                guards.merge(l.area(), 1, Integer::sum);
                assertEquals(l.area().god(), GodWarsFactions.bodyguardGod(l.creature()), l.creature());
                assertNotNull(ContentRegistry.monster(l.creature()), l.creature());
            }
        }
        for (Area a : List.of(Area.KREEARRA_CHAMBER, Area.GRAARDOR_CHAMBER, Area.ZILYANA_CHAMBER, Area.KRIL_CHAMBER)) {
            assertEquals(1, generals.get(a), a + " general");
            assertEquals(3, guards.get(a), a + " bodyguards");
        }
        assertNotNull(ContentRegistry.boss("kreearra"));
        assertNotNull(ContentRegistry.boss("commander_zilyana"));
        assertNotNull(com.runecraft.osrs.data.Boss.byEntity("general_graardor"));
        assertNotNull(com.runecraft.osrs.data.Boss.byEntity("kril_tsutsaroth"));
        assertEquals(150, GwdDungeon.GENERAL_RESPAWN_TICKS);
        Set<God> altarGods = new HashSet<>();
        for (GwdDungeon.Altar a : GwdDungeon.altars()) {
            Area in = area(a.spot());
            assertTrue(in.chamber(), a.name() + " stands in " + in);
            assertEquals(a.god(), in.god(), a.name());
            assertFalse(area(a.outside()).chamber(), a.name() + " teleports out of the chamber");
            if (a.god() != null) {
                altarGods.add(a.god());
                assertEquals(area(GwdDungeon.chamberDoor(a.god()).a()), area(a.outside()), a.name());
            }
        }
        assertEquals(EnumSet.allOf(God.class), altarGods);
        assertEquals(1000, GwdDungeon.ALTAR_COOLDOWN_TICKS); // 10 minutes
        assertEquals(Area.NEX_CHAMBER, area(GwdDungeon.NEX_SPAWN));
        for (Spot s : GwdDungeon.NEX_MAGES) {
            assertEquals(Area.NEX_CHAMBER, area(s));
        }
    }

    /**
     * Room-wide attacks stay in their room: the spot each altar teleports to lies within a boss's splash range of its
     * chamber (and the refuge right next to Nex's) yet is never reached; inside the chamber everyone is.
     */
    @Test
    void roomWideAttacksStopAtTheChamberWalls() {
        for (GwdDungeon.Altar a : GwdDungeon.altars()) {
            Area chamber = area(a.spot());
            Spot out = a.outside();
            assertTrue(GwdDungeon.inRoom(chamber, a.spot().blockX(), a.spot().feetY(), a.spot().blockZ()), a.name());
            assertFalse(GwdDungeon.inRoom(chamber, out.blockX(), out.feetY(), out.blockZ()), a.name() + " outside spot");
            assertFalse(GwdDungeon.inRoom(chamber, out.blockX(), out.feetY() + 1, out.blockZ()), a.name() + " outside, jumping");
            assertTrue(GwdDungeon.inRoom(null, out.blockX(), out.feetY(), out.blockZ()), "no room: everywhere");
        }
        // Kree'arra's chamber ends a few tiles from the spot outside its door: a 20-block sphere alone would reach it.
        Spot outside = GwdDungeon.altar("gwd_armadyl_altar").outside();
        double nearest = Double.MAX_VALUE;
        for (int x = outside.x() - 15; x <= outside.x() + 15; x++) {
            for (int y = outside.y() - 15; y <= outside.y() + 15; y++) {
                if (GwdDungeon.areaOf(2, x, y) == Area.KREEARRA_CHAMBER) {
                    double dx = StructureDef.bx(x) - outside.blockX();
                    double dz = StructureDef.bz(y) - outside.blockZ();
                    nearest = Math.min(nearest, Math.sqrt(dx * dx + dz * dz));
                }
            }
        }
        assertTrue(nearest < 20, "Kree'arra's chamber is " + nearest + " blocks from its door's outside spot");
        // The refuge (bank) next to Nex's chamber is another room.
        Spot refuge = Area.PRISON_REFUGE.seeds().get(0);
        assertFalse(GwdDungeon.inRoom(Area.NEX_CHAMBER, refuge.blockX(), refuge.feetY(), refuge.blockZ()));
        Spot nex = GwdDungeon.NEX_SPAWN;
        assertTrue(GwdDungeon.inRoom(Area.NEX_CHAMBER, nex.blockX(), nex.feetY(), nex.blockZ()));
    }

    /** Ashuelot Reis (quests module) stands before the frozen door, where The Frozen Door starts. */
    @Test
    void ashuelotReisStandsByTheFrozenDoor() {
        PlaceDef place = ContentRegistry.place("gwd_ancient_prison");
        assertNotNull(place);
        com.runecraft.osrs.content.NpcDef npc = ContentRegistry.npc("quest_ashuelot_reis");
        assertNotNull(npc);
        int x = place.blockX() + npc.dx();
        int z = place.blockZ() + npc.dz();
        Integer feet = com.runecraft.osrs.world.NpcStoreys.undergroundFeetY(npc.id());
        assertNotNull(feet);
        assertEquals(Area.MAIN, GwdDungeon.areaAt(x, feet, z));
        assertEquals(GwdDungeon.floorY(2) + 1, (int) feet);
        Spot outside = GwdDungeon.passage("frozen_door").a();
        assertTrue(Math.abs(com.runecraft.osrs.GielinorMap.toOsrsX(x) - outside.x()) <= 3
                && Math.abs(com.runecraft.osrs.GielinorMap.toOsrsY(z) - outside.y()) <= 3, "by the frozen door");
    }

    /** The carving stays deep under the sea (no shelf on the sea bed) and inside the dungeon's box. */
    @Test
    void theCarvingStaysDeepAndInItsBox() {
        StructureDef s = GwdDungeon.structure();
        int[] box = GwdDungeon.blockBox();
        for (StructureDef.Op op : s.ops()) {
            assertTrue(op.y2() < com.runecraft.osrs.world.TerrainAnchors.DEEP_UNDERGROUND_BELOW, op.toString());
            assertTrue(op.y1() > -64, op.toString());
            assertTrue(op.x1() >= box[0] - 2 && op.x2() <= box[2] + 2 && op.z1() >= box[1] - 2 && op.z2() <= box[3] + 2, op.toString());
        }
        assertTrue(s.ops().size() > 500);
        // The real OSRS tiles, north of everything else in the world: no other structure shares the box.
        for (StructureDef other : ContentRegistry.structures()) {
            if (!other.id().equals(s.id())) {
                assertFalse(other.maxX() >= box[0] && other.minX() <= box[2] && other.maxZ() >= box[1] && other.minZ() <= box[3],
                        other.id() + " overlaps the God Wars Dungeon");
            }
        }
        assertTrue(ContentRegistry.structures().stream().anyMatch(d -> d.id().equals("gwd_dungeon")));
        assertTrue(GwdDungeon.contains(GwdDungeon.LANDING.blockX(), GwdDungeon.LANDING.feetY(), GwdDungeon.LANDING.blockZ()));
        assertFalse(GwdDungeon.contains(StructureDef.bx(GwdDungeon.HOLE_TILE[0]), 70, StructureDef.bz(GwdDungeon.HOLE_TILE[1])));
    }

    /** OSRS creature ids of the cache NPCs, as the module names them. */
    private static String creature(String name, int level, int npcId) {
        if (name.equals("Aviansie")) {
            return level >= 131 ? "gwd_aviansie_elite" : "gwd_aviansie";
        }
        String god = switch (npcId) {
            case 2210, 2211, 2212 -> "saradomin";
            case 2242, 2243, 2244 -> "bandos";
            case 3159, 3160, 3161 -> "zamorak";
            case 3166, 3167, 3168 -> "armadyl";
            default -> null;
        };
        if (god != null) {
            return "gwd_" + name.toLowerCase(Locale.ROOT).replace(' ', '_') + "_" + god;
        }
        return switch (name) {
            case "Feral Vampyre" -> "gwd_vampire";
            default -> "gwd_" + name.toLowerCase(Locale.ROOT).replace(' ', '_');
        };
    }

    /** Every cache follower spawn has its follower on its tile, and nothing else stands in the four gods' areas. */
    @Test
    void followerSpawnsMatchTheCache() throws IOException {
        List<String[]> rows = tsv("gwd_npc_spawns.tsv");
        assertEquals(346, rows.size());
        Map<String, Integer> want = new HashMap<>();
        for (String[] r : rows) {
            want.merge(creature(r[0], Integer.parseInt(r[1]), Integer.parseInt(r[2])) + "@" + r[3] + "," + r[4] + "," + r[5], 1,
                    Integer::sum);
        }
        Map<String, Integer> have = new HashMap<>();
        for (GwdSpawns.Spawn s : GwdSpawns.all()) {
            assertNotNull(ContentRegistry.monster(s.creature()), s.creature());
            Area a = GwdDungeon.areaOf(s.plane(), s.x(), s.y());
            assertNotNull(a, s + " is not on the dungeon's floor");
            assertFalse(a.chamber(), s + " stands in a general's chamber");
            assertEquals(GwdSpawns.respawnTicks(s.creature()), s.respawnTicks());
            if (s.creature().endsWith("_zaros") || s.creature().equals("gwd_blood_reaver")) {
                assertEquals(Area.ANCIENT_PRISON, a, s.toString());
                assertTrue(GodWarsFactions.zarosEssence(s.creature()) > 0);
                assertEquals("zaros", GodWarsFactions.allegiance(s.creature()));
                continue;
            }
            assertNotNull(GodWarsFactions.followerGod(s.creature()), s.creature());
            assertFalse(a == Area.ANCIENT_PRISON || a == Area.PRISON_ENTRANCE || a == Area.PRISON_REFUGE, s.toString());
            have.merge(s.creature() + "@" + s.plane() + "," + s.x() + "," + s.y(), 1, Integer::sum);
        }
        assertEquals(want, have);
        // OSRS respawn times (wiki infoboxes).
        assertEquals(60, GwdSpawns.respawnTicks("gwd_goblin"));
        assertEquals(50, GwdSpawns.respawnTicks("gwd_knight_of_saradomin"));
        assertEquals(25, GwdSpawns.respawnTicks("gwd_spiritual_mage_zamorak"));
        // Every follower stands somewhere.
        Set<String> spawned = new HashSet<>();
        GwdSpawns.all().forEach(s -> spawned.add(s.creature()));
        assertTrue(spawned.containsAll(GodWarsFactions.followers().keySet()));
    }

    private static String itemId(String name) {
        return name.toLowerCase(Locale.ROOT).replace("'", "").replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    }

    /** The items that calm each god's followers are the wiki's protection table (ids from the names). */
    @Test
    void godItemsAreTheWikiTable() throws IOException {
        Map<String, Set<String>> want = new HashMap<>();
        for (String[] r : tsv("god_items.tsv")) {
            want.computeIfAbsent(r[0], k -> new HashSet<>()).add(itemId(r[2]));
        }
        assertEquals(Set.of("armadyl", "bandos", "saradomin", "zamorak", "zaros"), want.keySet());
        for (Map.Entry<String, Set<String>> e : want.entrySet()) {
            assertEquals(e.getValue(), GodItems.items(e.getKey()), e.getKey());
            long existing = e.getValue().stream().filter(ItemCatalog::exists).count();
            assertTrue(existing >= 8, e.getKey() + " has only " + existing + " items in the game");
        }
        assertTrue(GodItems.wears("armadyl", List.of("rune_platebody", "armadyl_pendant")));
        assertFalse(GodItems.wears("bandos", List.of("armadyl_pendant")));
        assertEquals(2, GodItems.count("zaros", List.of("torva_full_helm", "zaryte_vambraces", "bandos_boots")));
        assertTrue(GodItems.items("saradomin").contains("staff_of_light") && GodItems.items("zamorak").contains("staff_of_light"));
    }

    /** OSRS Wiki "Kill count": every follower, bodyguard and general counts; the prison pays more and Zarosian gear adds. */
    @Test
    void essenceRules() {
        assertEquals(God.ARMADYL, GodWarsFactions.essenceGod("flight_kilisa"));
        assertEquals(God.BANDOS, GodWarsFactions.essenceGod("general_graardor"));
        assertEquals(God.ZAMORAK, GodWarsFactions.essenceGod("gwd_icefiend"));
        assertEquals(God.SARADOMIN, GodWarsFactions.essenceGod("growler"));
        assertNull(GodWarsFactions.essenceGod("gwd_spiritual_mage_zaros"));
        assertEquals(2, GodWarsFactions.zarosEssence("gwd_spiritual_warrior_zaros"));
        assertEquals(2, GodWarsFactions.zarosEssence("gwd_spiritual_ranger_zaros"));
        assertEquals(5, GodWarsFactions.zarosEssence("gwd_spiritual_mage_zaros"));
        assertEquals(3, GodWarsFactions.zarosEssence("gwd_blood_reaver"));
        assertEquals(2, GodWarsFactions.zarosEssence("fumus"));
        assertEquals(5, GodWarsFactions.zarosEssence("nex"));
        assertEquals(0, GodWarsFactions.zarosEssence("gwd_goblin"));
        assertEquals(0, GodWarsFactions.zarosBonus(0, 0.0));
        assertEquals(1, GodWarsFactions.zarosBonus(2, 0.39)); // 40% with two items
        assertEquals(0, GodWarsFactions.zarosBonus(2, 0.41));
        assertEquals(1, GodWarsFactions.zarosBonus(5, 0.99)); // five items guarantee one
        assertEquals(2, GodWarsFactions.zarosBonus(10, 0.99)); // all ten: always two (a spiritual mage then gives seven)
        assertEquals(4000, GodWarsFactions.addKill(3999, 5));
        assertEquals(41, GodWarsFactions.addKill(40, 1));
        assertEquals(40, GodWarsFactions.REQUIRED_ZAROS_ESSENCE);
        assertNull(GodWarsFactions.allegiance("kreearra"));
        assertNull(GodWarsFactions.allegiance("flight_kilisa"));
        assertNull(GodWarsFactions.allegiance("blood_reaver")); // Nex's own reavers are always aggressive
        assertEquals("armadyl", GodWarsFactions.allegiance("gwd_aviansie"));
    }
}
