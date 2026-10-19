package com.runecraft.osrs.content.npcs;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.islands.IslandsTravel;
import com.runecraft.osrs.content.systems.Spellbooks;
import com.runecraft.osrs.content.systems.Teleports;
import com.runecraft.osrs.content.zeah.ZeahTravel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every transport checked against a committed snapshot of the RuneLite Shortest Path plugin's transport
 * tables (skretzo/shortest-path, wiki-derived OSRS tiles, fares and requirements) in
 * {@code src/test/resources/osrs/transport/}: fairy rings, spirit trees, gnome gliders, charter ships, boats,
 * canoes, magic carpets, balloons, Magic Mushtrees, quetzals, the Lovakengj minecarts, teleport jewellery and
 * teleport tablets, the Wilderness levers and obelisks. OSRS dungeon tiles (y 9,000+) land in the dungeon carved under
 * the surface tile above them where one exists; other destinations OSRS puts underground, in an instance or on another
 * plane go to the place this world builds for them, listed in {@link #RELOCATED} with the reason.
 */
class TransportSnapshotTest {
    /** Destinations deliberately moved from their OSRS tile: name -> why. */
    private static final Map<String, String> RELOCATED = Map.ofEntries(
            Map.entry("Games necklace: Corporeal Beast", "lair built on the surface (corporeal_beast_lair)"),
            Map.entry("Games necklace: Tears of Guthix", "cave built on the surface (tears_of_guthix_cave)"),
            Map.entry("Skills necklace: Mining Guild",
                    "OSRS's underground entrance tile lies east of this world's carved Mining Guild; the hall outside its door"),
            Map.entry("Ring of wealth: Dondakan", "OSRS lands underground; mine entrance at White Wolf Mountain"),
            Map.entry("Digsite pendant: Lithkren", "OSRS lands in the vault; Lithkren's surface"),
            Map.entry("Drakan's medallion: Slepe", "OSRS lands in the sanctuary under Slepe; the village"),
            Map.entry("Slayer ring: Tarn's Lair", "an instance in OSRS; the Haunted Mine entrance to the lair as built here"),
            Map.entry("Slayer ring: Dark Beasts", "OSRS lands by the Temple of Light; this world's dark beasts are in the Iorwerth Dungeon"),
            Map.entry("Kharedst's memoirs: Lunch by the Lancalliums", "OSRS tile is in this world's river; 4 tiles west"),
            Map.entry("Book of the dead: Lunch by the Lancalliums", "OSRS tile is in this world's river; 4 tiles west"),
            Map.entry("Key master teleport", "Cerberus' lair is an instance; Taverley Dungeon entrance"),
            Map.entry("Pharaoh's sceptre: Jalsavrah", "the plane-3 Pyramid Plunder lobby; the minigame's pyramid"),
            Map.entry("Giantsoul amulet: 1. Bryophyta", "this world's Varrock Sewers don't reach under OSRS's tile; Bryophyta's summoning stone"),
            Map.entry("Giantsoul amulet: 2. Obor", "the Stronghold of Security, not Obor's lair, lies under OSRS's tile here; his summoning stone"),
            Map.entry("Giantsoul amulet: 3. Branda and Eldric", "this world's Asgarnian Ice Dungeon doesn't reach under OSRS's tile; its entrance (Titans' lair)"),
            Map.entry("Amulet of the eye", "the Temple of the Eye is an instance; the Guardians of the Rift place on the surface"),
            Map.entry("Camulet: Enakhra's Temple", "OSRS lands inside the temple (plane 2); its entrance"),
            Map.entry("Hallowed crystal shard", "the Sepulchre lobby is an instance; the minigame's place in Darkmeyer"));

    // ------------------------------------------------------------------ snapshot reading

    private static List<Map<String, String>> rows(String file) throws IOException {
        String fixture = "osrs/transport/" + file;
        Path p = Path.of("src/test/resources", fixture);
        List<String> raw;
        if (Files.exists(p)) {
            raw = Files.readAllLines(p, StandardCharsets.UTF_8);
        } else {
            try (InputStream in = TransportSnapshotTest.class.getClassLoader().getResourceAsStream(fixture)) {
                assertNotNull(in, "missing " + fixture);
                raw = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
            }
        }
        String[] header = null;
        String section = "";
        List<Map<String, String>> out = new ArrayList<>();
        for (String l : raw) {
            if (header == null && (l.startsWith("# Origin\t") || l.startsWith("# Destination\t") || l.startsWith("#Destination\t"))) {
                header = l.replaceFirst("^# ?", "").split("\t", -1);
                continue;
            }
            if (l.startsWith("#")) {
                section = l.replaceFirst("^# ?", "").trim();
                continue;
            }
            if (header == null || l.isBlank() || l.replace("\t", "").isBlank()) {
                continue;
            }
            String[] f = l.split("\t", -1);
            Map<String, String> row = new HashMap<>();
            for (int i = 0; i < header.length; i++) {
                row.put(header[i].trim(), i < f.length ? f[i].trim() : "");
            }
            row.put("section", section);
            out.add(row);
        }
        assertFalse(out.isEmpty(), file);
        return out;
    }

    private static int[] tile(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String[] p = s.trim().split("\\s+");
        return new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1])};
    }

    private static String norm(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("^[0-9a-z][.:] ", "").replaceAll("[^a-z0-9]", "");
    }

    /** RuneCraft quest id for an OSRS quest name. */
    private static String questId(String name) {
        String n = name.trim();
        if (n.equals("The Fremennik Trials")) {
            return "fremennik_trials";
        }
        return n.toLowerCase(Locale.ROOT).replace("'", "").replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    }

    private static Set<String> quests(String field) {
        Set<String> out = new HashSet<>();
        if (field != null && !field.isBlank()) {
            for (String q : field.split(";")) {
                out.add(questId(q));
            }
        }
        return out;
    }

    private static int coins(String items) {
        if (items == null) {
            return 0;
        }
        for (String part : items.split("[&|]+")) {
            if (part.startsWith("COINS=")) {
                return Integer.parseInt(part.substring(6));
            }
        }
        return 0;
    }

    private static String xy(int[] t) {
        return t[0] + "," + t[1];
    }

    private static boolean same(int[] a, int x, int y) {
        return a != null && a[0] == x && a[1] == y;
    }

    // ------------------------------------------------------------------ fairy rings

    /** OSRS tile of a fairy ring stop as this world builds it. */
    private static int[] ringTile(IslandsTravel.Stop s) {
        PlaceDef p = ContentRegistry.place(s.place());
        int bx;
        int bz;
        if (p != null) {
            bx = p.blockX() + s.dx();
            bz = p.blockZ() + s.dz();
        } else {
            Landmark l = Landmark.byName(s.place());
            assertNotNull(l, s.place());
            bx = l.blockX() + s.dx();
            bz = l.blockZ() + s.dz();
        }
        return new int[]{GielinorMap.toOsrsX(bx), GielinorMap.toOsrsY(bz)};
    }

    @Test
    void fairyRingsMatchTheirCodesTilesAndQuests() throws IOException {
        Map<String, int[]> tiles = new HashMap<>();
        Map<String, Set<String>> req = new HashMap<>();
        for (Map<String, String> r : rows("fairy_rings.tsv")) {
            String code = r.get("Display info").replace(" ", "");
            if (r.get("Origin").isEmpty() && code.length() == 3) {
                tiles.put(code, tile(r.get("Destination")));
                req.put(code, quests(r.get("Quests")));
            }
        }
        // Realms built elsewhere (CKP: the Cosmic Altar's plane, BJR: the Fisher Realm, DIR: Gorak's Plane, DIP: the Abyssal
        // Nexus, DLS: the Myreque hideout, BLQ: Yu'biusk), rings carved into dungeons (BLP, ALR, AJQ: checked below) and the
        // house ring. BKQ's Enchanted Valley is carved at its OSRS tile, so its ring is checked like a surface one.
        Set<String> otherRealms = Set.of("CKP", "BJR", "DIR", "DIP", "DLS", "BLQ", "BLP", "ALR", "AJQ", "DIQ");
        // CLR's ring is part of the hand-built Ape Atoll course, a few tiles from OSRS's.
        Map<String, Integer> tolerance = Map.of("CLR", 6);
        Set<String> seen = new HashSet<>();
        for (IslandsTravel.Stop s : IslandsTravel.FAIRY_RINGS.stops()) {
            if (s.code().equals("ZANARIS")) {
                continue;
            }
            assertTrue(seen.add(s.code()), s.code());
            int[] src = tiles.get(s.code());
            assertNotNull(src, "unknown fairy ring code " + s.code());
            Set<String> mine = s.quest() == null ? Set.of() : Set.of(s.quest());
            assertEquals(req.get(s.code()), mine, s.code() + " quest");
            if (otherRealms.contains(s.code())) {
                continue;
            }
            int[] t = ringTile(s);
            int tol = tolerance.getOrDefault(s.code(), 1);
            assertTrue(Math.abs(t[0] - src[0]) <= tol && Math.abs(t[1] - src[1]) <= tol,
                    s.code() + " at " + xy(t) + ", OSRS " + xy(src));
        }
        // Every surface ring exists (DIQ is the house ring), and every OSRS code is on the dial (BKS is the Zanaris hub).
        for (Map.Entry<String, int[]> e : tiles.entrySet()) {
            int[] t = e.getValue();
            boolean surface = t[1] < 4100 && !Set.of("DIQ", "BKS").contains(e.getKey());
            if (surface) {
                assertTrue(seen.contains(e.getKey()), "missing fairy ring " + e.getKey() + " at " + xy(t));
            }
        }
        assertTrue(seen.size() >= 50, "rings " + seen.size());
        Set<String> every = new HashSet<>(tiles.keySet());
        every.remove("BKS");
        assertEquals(every, seen);
        // The Great Conch's ring stands on dry land.
        int[] cjq = ringTile(IslandsTravel.FAIRY_RINGS.stop("CJQ"));
        assertFalse(com.runecraft.osrs.world.Elevation.isWater(GielinorMap.toBlockX(cjq[0]), GielinorMap.toBlockZ(cjq[1])), "CJQ in water");
        assertEquals(IslandsTravel.HOUSE_PLACE, IslandsTravel.FAIRY_RINGS.stop("DIQ").place());
    }

    /** Whether a structure op puts a block at (x, y, z) (WALLS only on its rim). */
    private static boolean covers(com.runecraft.osrs.world.StructureDef.Op op, int x, int y, int z) {
        if (x < op.x1() || x > op.x2() || y < op.y1() || y > op.y2() || z < op.z1() || z > op.z2()) {
            return false;
        }
        return op.type() != com.runecraft.osrs.world.StructureDef.Op.Type.WALLS
                || x == op.x1() || x == op.x2() || z == op.z1() || z == op.z2();
    }

    /** The last op of a structure that touches (x, y, z), or null. */
    private static com.runecraft.osrs.world.StructureDef.Op last(com.runecraft.osrs.world.StructureDef def, int x, int y, int z) {
        com.runecraft.osrs.world.StructureDef.Op hit = null;
        for (com.runecraft.osrs.world.StructureDef.Op op : def.ops()) {
            if (op.type() != com.runecraft.osrs.world.StructureDef.Op.Type.FLATTEN && covers(op, x, y, z)) {
                hit = op;
            }
        }
        return hit;
    }

    /** The registered structure with this id (a dungeon's, or a carved realm's such as the Enchanted Valley), or null. */
    private static com.runecraft.osrs.world.StructureDef structure(String id) {
        for (com.runecraft.osrs.world.StructureDef s : ContentRegistry.structures()) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }

    /** Each dungeon realm's ring and arrival spot stand in its dungeon's carved air, on a solid floor. */
    @Test
    void dungeonFairyRingsStandInCarvedAir() {
        assertEquals(4, IslandsTravel.DUNGEON_RINGS.size());
        for (IslandsTravel.DungeonRing r : IslandsTravel.DUNGEON_RINGS) {
            com.runecraft.osrs.world.StructureDef d = structure(r.dungeon());
            assertNotNull(d, r.code() + " dungeon " + r.dungeon());
            int[] a = IslandsTravel.dungeonArrival(r);
            for (int[] at : new int[][]{{r.x(), r.z()}, {a[0], a[2]}}) {
                for (int up = 1; up <= 2; up++) {
                    com.runecraft.osrs.world.StructureDef.Op op = last(d, at[0], r.floorY() + up, at[1]);
                    assertNotNull(op, r.code() + " not inside " + r.dungeon() + " at " + at[0] + "," + (r.floorY() + up) + "," + at[1]);
                    assertEquals(com.runecraft.osrs.world.StructureDef.Op.Type.CLEAR, op.type(),
                            r.code() + " blocked by " + op.block() + " at " + at[0] + "," + (r.floorY() + up) + "," + at[1]);
                }
                com.runecraft.osrs.world.StructureDef.Op floor = last(d, at[0], r.floorY(), at[1]);
                assertNotNull(floor, r.code() + " floor");
                assertTrue(floor.type() != com.runecraft.osrs.world.StructureDef.Op.Type.CLEAR && !floor.block().contains("lava")
                        && !floor.block().contains("water"), r.code() + " floor " + floor.block());
            }
            IslandsTravel.Stop stop = IslandsTravel.FAIRY_RINGS.stop(r.code());
            assertEquals(IslandsTravel.ringPlace(r.code()), stop.place(), r.code());
            assertTrue(stop.hasBlock(), r.code());
            // The stop's block offset from its bare place is the ring's own block column.
            PlaceDef p = ContentRegistry.place(stop.place());
            assertNotNull(p, stop.place());
            assertEquals(r.x(), p.blockX() + stop.dx(), r.code());
            assertEquals(r.z(), p.blockZ() + stop.dz(), r.code());
        }
    }

    /**
     * OSRS dungeon tiles (y 9,000 and up) land in the dungeon carved under the surface tile 6,400 tiles south (SysTeleport
     * searches 3 blocks around that column), so every such destination needs one there.
     */
    @Test
    void undergroundDestinationsHaveACarvedDungeon() {
        List<Teleports.Destination> all = new ArrayList<>();
        JEWELLERY.values().forEach(all::addAll);
        for (Spellbooks.Tablet t : Spellbooks.TABLETS) {
            all.add(t.destination());
        }
        int checked = 0;
        for (Teleports.Destination d : all) {
            if (!Teleports.underground(d)) {
                continue;
            }
            int bx = GielinorMap.toBlockX(d.x());
            int bz = GielinorMap.toBlockZ(Teleports.surfaceY(d));
            boolean carved = false;
            for (com.runecraft.osrs.content.dungeons.Dungeons.Dungeon g : com.runecraft.osrs.content.dungeons.Dungeons.all()) {
                for (com.runecraft.osrs.world.StructureDef.Op op : g.structure().ops()) {
                    carved |= op.type() == com.runecraft.osrs.world.StructureDef.Op.Type.CLEAR
                            && op.y2() <= com.runecraft.osrs.content.dungeons.Dungeons.DEEP_TOP
                            && bx + 3 >= op.x1() && bx - 3 <= op.x2() && bz + 3 >= op.z1() && bz - 3 <= op.z2();
                }
            }
            assertTrue(carved, d.name() + " at " + d.x() + "," + d.y() + " has no dungeon under " + d.x() + "," + Teleports.surfaceY(d));
            checked++;
        }
        assertTrue(checked >= 3, "underground destinations " + checked);
    }

    // ------------------------------------------------------------------ Wilderness levers and obelisks

    @Test
    void wildernessLeversMatch() throws IOException {
        Set<String> osrs = new HashSet<>();
        for (Map<String, String> r : rows("teleportation_levers.tsv")) {
            osrs.add(xy(tile(r.get("Origin"))) + ">" + xy(tile(r.get("Destination"))));
        }
        Set<String> mine = new HashSet<>();
        for (WildernessTransport.Lever l : WildernessTransport.LEVERS) {
            mine.add(l.x() + "," + l.y() + ">" + l.toX() + "," + l.toY());
            assertEquals(l, WildernessTransport.leverAt(l.x(), l.y()));
        }
        // The King Black Dragon's levers: the lair end is on its OSRS tile; the Lava Maze dungeon around the entrance lever
        // (3067, 10253) isn't built, so that lever stands in a chamber off the foot of this world's Lava Maze KBD ladder.
        Set<String> expected = new HashSet<>(osrs);
        assertTrue(expected.remove("3067,10253>2271,4680"));
        assertTrue(expected.remove("2271,4680>3067,10254"));
        expected.add("3037,3851>2271,4680");
        expected.add("2271,4680>3036,3851");
        assertEquals(expected, mine);
        assertEquals(7, WildernessTransport.LEVERS.size());
    }

    @Test
    void wildernessObelisksMatch() throws IOException {
        Map<String, Set<String>> platforms = new LinkedHashMap<>();
        for (Map<String, String> r : rows("wilderness_obelisks.tsv")) {
            if (r.get("Origin").isEmpty() || !r.get("section").startsWith("Level ")) {
                continue; // the arrival rows, and the player-owned house's obelisk (the extras module's house)
            }
            String level = r.get("section").replaceAll("^Level (\\d+) .*", "$1");
            platforms.computeIfAbsent(level, k -> new HashSet<>()).add(xy(tile(r.get("Origin"))));
        }
        assertEquals(6, platforms.size());
        assertEquals(6, WildernessTransport.OBELISKS.size());
        for (WildernessTransport.Obelisk o : WildernessTransport.OBELISKS) {
            Set<String> mine = new HashSet<>();
            int shift = o.level() == 13 ? WildernessTransport.OBELISK_13_EAST_SHIFT : 0; // clear of the Ferox Enclave
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    mine.add((o.x() - shift + dx) + "," + (o.y() + dy));
                    assertEquals(o, WildernessTransport.platformAt(o.x() + dx, o.y() + dy));
                }
            }
            assertEquals(platforms.get(String.valueOf(o.level())), mine, "obelisk " + o.level());
            for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
                assertEquals(o, WildernessTransport.pillarAt(o.x() + c[0], o.y() + c[1]));
            }
            assertEquals(null, WildernessTransport.pillarAt(o.x(), o.y()));
            // Any of the six can come up, the obelisk itself included ("players may even be teleported to the same location").
            Set<Integer> reached = new HashSet<>();
            java.util.Random random = new java.util.Random(o.level());
            for (int i = 0; i < 300; i++) {
                reached.add(WildernessTransport.destination(o, random).level());
            }
            assertEquals(6, reached.size());
            assertEquals(o, WildernessTransport.obelisk(o.level()));
            assertEquals(o.level(), GielinorMap.wildernessLevel(GielinorMap.toBlockX(o.x()), GielinorMap.toBlockZ(o.y())), "obelisk " + o.level() + " Wilderness level");
        }
        // Laying an obelisk's 5x5-tile square (floor, pillars, five blocks of air) cuts into no structure above ground.
        for (WildernessTransport.Obelisk o : WildernessTransport.OBELISKS) {
            int x1 = GielinorMap.toBlockX(o.x() - 2);
            int x2 = GielinorMap.toBlockX(o.x() + 2) + 1;
            int z1 = GielinorMap.toBlockZ(o.y() + 2);
            int z2 = GielinorMap.toBlockZ(o.y() - 2) + 1;
            for (com.runecraft.osrs.world.StructureDef s : ContentRegistry.structures()) {
                for (com.runecraft.osrs.world.StructureDef.Op op : s.ops()) {
                    boolean builds = op.type() != com.runecraft.osrs.world.StructureDef.Op.Type.FLATTEN
                            && op.type() != com.runecraft.osrs.world.StructureDef.Op.Type.CLEAR;
                    assertFalse(builds && op.y2() >= com.runecraft.osrs.world.StructureDef.GROUND && op.x2() >= x1 && op.x1() <= x2
                            && op.z2() >= z1 && op.z1() <= z2, "obelisk " + o.level() + " cuts into " + s.id() + ": " + op);
                }
            }
        }
        // Every lever and obelisk tile on the surface is dry land (the Mage Bank is its own cavern under the sea).
        for (WildernessTransport.Lever l : WildernessTransport.LEVERS) {
            if (!WildernessTransport.inCavern(l)) {
                assertFalse(com.runecraft.osrs.world.Elevation.isWater(GielinorMap.toBlockX(l.x()), GielinorMap.toBlockZ(l.y())), l.name());
            }
        }
        for (WildernessTransport.Obelisk o : WildernessTransport.OBELISKS) {
            assertFalse(com.runecraft.osrs.world.Elevation.isWater(GielinorMap.toBlockX(o.x()), GielinorMap.toBlockZ(o.y())), "obelisk " + o.level());
        }
        // The Mage Bank's arrival spot and lever lie inside its cavern's carved air.
        com.runecraft.osrs.world.StructureDef bank = WildernessTransport.mageBank();
        int[] a = WildernessTransport.mageBankArrival();
        assertEquals(com.runecraft.osrs.world.StructureDef.Op.Type.CLEAR, last(bank, a[0], a[1], a[2]).type());
        assertEquals(com.runecraft.osrs.world.StructureDef.Op.Type.CLEAR, last(bank, a[0], a[1] + 1, a[2]).type());
        WildernessTransport.Lever back = WildernessTransport.leverAt(2539, 4712);
        assertTrue(last(bank, GielinorMap.toBlockX(back.x()), a[1], GielinorMap.toBlockZ(back.y())).block().startsWith("minecraft:lever"));
    }

    // ------------------------------------------------------------------ NPC-run routes

    private static Travel.Agent agent(String route) {
        for (Travel.Agent a : Travel.agents()) {
            if (a.route().equals(route)) {
                return a;
            }
        }
        return null;
    }

    @Test
    void spiritTreesMatch() throws IOException {
        Map<String, Set<String>> dests = new HashMap<>();
        for (Map<String, String> r : rows("spirit_trees.tsv")) {
            if (r.get("Origin").isEmpty() && !r.get("Destination").isEmpty()) {
                dests.put(xy(tile(r.get("Destination"))), quests(r.get("Quests")));
            }
        }
        // Player-grown trees at farming patches, the house tree and the Sailing-era Pandemonium tree.
        Set<String> notNatural = Set.of("1251,3750", "1693,3540", "2613,3855", "2800,3203", "3058,3257", "1858,7051", "1202,2785");
        Set<String> mine = new HashSet<>();
        for (Travel.SpiritTree t : Travel.SPIRIT_TREES) {
            String k = t.x() + "," + t.y();
            assertTrue(dests.containsKey(k), t.name() + " at " + k + " is not a spirit tree");
            assertTrue(t.quests().containsAll(dests.get(k)), t.name() + " quests " + t.quests() + " vs " + dests.get(k));
            assertTrue(t.quests().contains("tree_gnome_village"), t.name());
            mine.add(k);
            assertNotNull(Travel.get(Travel.spiritTreeRoute(t.id())), t.id());
        }
        for (String k : dests.keySet()) {
            assertTrue(mine.contains(k) || notNatural.contains(k), "missing spirit tree at " + k);
        }
        // Travelling to or from the Gnome Stronghold's tree needs The Grand Tree.
        for (Travel.Dest d : Travel.get(Travel.spiritTreeRoute("tree_gnome_village")).dests()) {
            assertEquals(d.name().equals("Gnome Stronghold"), d.quests().contains("the_grand_tree"), d.name());
        }
    }

    @Test
    void glidersMatch() throws IOException {
        Map<String, Map<String, String>> byName = new HashMap<>();
        for (Map<String, String> r : rows("gnome_gliders.tsv")) {
            if (r.get("Origin").isEmpty()) {
                byName.put(norm(r.get("Display info")), r);
            }
        }
        for (int i = 0; i < Travel.GLIDER_NAMES.length; i++) {
            String station = Travel.GLIDER_NAMES[i].replaceAll(" \\(.*", "");
            Map<String, String> r = byName.get(norm(station));
            assertNotNull(r, station);
            int[] t = tile(r.get("Destination"));
            assertEquals(xy(t), xy(Travel.GLIDER_TILES[i]), station);
        }
        assertEquals(byName.size(), Travel.GLIDER_NAMES.length, "glider stations");
        for (Travel.Route route : Travel.all()) {
            if (!route.id().startsWith("glider_")) {
                continue;
            }
            for (Travel.Dest d : route.dests()) {
                Set<String> need = quests(byName.get(norm(d.name().replaceAll(" \\(.*", ""))).get("Quests"));
                assertTrue(d.quests().containsAll(need), route.id() + " -> " + d.name() + " " + d.quests() + " vs " + need);
            }
        }
    }

    @Test
    void charterShipsMatchTheOsrsFareTable() throws IOException {
        Map<String, int[]> port = new HashMap<>();
        Map<String, Integer> fare = new HashMap<>();
        Map<String, Set<String>> req = new HashMap<>();
        Map<String, String> skill = new HashMap<>();
        for (Map<String, String> r : rows("charter_ships.tsv")) {
            String from = r.get("section");
            String to = r.get("Display info");
            port.put(to, tile(r.get("Destination")));
            fare.put(from + ">" + to, coins(r.get("Items")));
            req.put(to, quests(r.get("Quests")));
            skill.put(to, r.get("Skills"));
        }
        // Every OSRS charter port has a crew here (the Sailing ports too).
        Set<String> sections = new HashSet<>();
        for (Map<String, String> r : rows("charter_ships.tsv")) {
            sections.add(r.get("section"));
        }
        assertEquals(sections.size(), Travel.CHARTER_PORTS.length, "charter ports " + sections);
        int routes = 0;
        for (int i = 0; i < Travel.CHARTER_PORTS.length; i++) {
            String from = i == 3 ? "Shipyard" : Travel.CHARTER_PORTS[i];
            assertNotNull(port.get(from), from);
            assertEquals(xy(port.get(from)), xy(Travel.CHARTER_TILES[i]), from + " dock");
            for (int j = 0; j < Travel.CHARTER_PORTS.length; j++) {
                String to = j == 3 ? "Shipyard" : Travel.CHARTER_PORTS[j];
                Integer osrs = fare.get(from + ">" + to);
                assertEquals(osrs == null ? 0 : osrs, Travel.charterFare(i, j), from + " -> " + to);
            }
            Travel.Route route = Travel.get(Travel.charterRoute(i));
            for (Travel.Dest d : route.dests()) {
                routes++;
                assertEquals(req.get(d.name()), new HashSet<>(d.quests()), "charter to " + d.name() + " quest");
                assertEquals(skill.get(d.name()), d.skill() == null ? "" : d.level() + " " + d.skill().displayName(),
                        "charter to " + d.name() + " skill");
                assertTrue(same(port.get(d.name()), d.x(), d.y()), d.name());
            }
            Travel.Agent crew = agent(route.id());
            assertNotNull(crew, route.id());
        }
        assertTrue(routes > 250, "charter routes " + routes);
    }

    @Test
    void canoesMatch() throws IOException {
        List<Map<String, String>> src = rows("canoes.tsv");
        int checked = 0;
        for (Travel.Route route : Travel.all()) {
            if (!route.id().startsWith("canoe_")) {
                continue;
            }
            Travel.Agent station = agent(route.id());
            assertNotNull(station, route.id());
            Map<String, Map<String, String>> fromHere = new HashMap<>();
            for (Map<String, String> r : src) {
                if (same(tile(r.get("Origin")), station.x(), station.y())) {
                    fromHere.put(norm(r.get("Display info")), r);
                }
            }
            assertFalse(fromHere.isEmpty(), route.id() + " station tile " + station.x() + "," + station.y());
            assertEquals(fromHere.size(), route.dests().size(), route.id() + " destinations");
            for (Travel.Dest d : route.dests()) {
                Map<String, String> r = fromHere.get(norm(d.name()));
                assertNotNull(r, route.id() + " -> " + d.name());
                assertTrue(same(tile(r.get("Destination")), d.x(), d.y()), route.id() + " -> " + d.name() + " tile");
                assertEquals(Skill.WOODCUTTING, d.skill());
                assertEquals(r.get("Skills"), d.level() + " Woodcutting", route.id() + " -> " + d.name() + " level");
                checked++;
            }
        }
        assertEquals(src.size(), checked, "every OSRS canoe trip");
    }

    @Test
    void magicCarpetsMatch() throws IOException {
        List<Map<String, String>> src = rows("magic_carpets.tsv");
        Map<String, String> varbitQuest = Map.of("346", "the_golem", "418", "icthlarins_little_helper");
        int checked = 0;
        for (Travel.Route route : Travel.all()) {
            if (!route.id().startsWith("carpet_")) {
                continue;
            }
            TownNpc merchant = null;
            for (TownNpc n : Townsfolk.all()) {
                if (route.id().equals(n.travel())) {
                    merchant = n;
                }
            }
            assertNotNull(merchant, route.id());
            for (Travel.Dest d : route.dests()) {
                List<Map<String, String>> hits = new ArrayList<>();
                for (Map<String, String> r : src) {
                    if (same(tile(r.get("Origin")), merchant.x(), merchant.y()) && same(tile(r.get("Destination")), d.x(), d.y())) {
                        hits.add(r);
                    }
                }
                assertFalse(hits.isEmpty(), route.id() + " -> " + d.name());
                assertEquals(coins(hits.get(0).get("Items")), d.price(), route.id() + " -> " + d.name());
                boolean gated = hits.stream().allMatch(r -> r.get("Varbits").contains("="));
                if (gated) {
                    String q = varbitQuest.get(hits.get(0).get("Varbits").split("[=<>]")[0]);
                    assertTrue(d.quests().contains(q), route.id() + " -> " + d.name() + " needs " + q);
                }
                checked++;
            }
        }
        assertTrue(checked >= 11, "carpet trips " + checked);
    }

    @Test
    void balloonsMatch() throws IOException {
        Map<String, int[]> dest = new HashMap<>();
        for (Map<String, String> r : rows("hot_air_balloons.tsv")) {
            if (r.get("Origin").isEmpty()) {
                dest.put(r.get("Display info"), tile(r.get("Destination")));
            }
        }
        assertEquals(dest.size(), Travel.BALLOON_STOPS.length);
        for (int i = 0; i < Travel.BALLOON_STOPS.length; i++) {
            assertEquals(xy(dest.get(Travel.BALLOON_STOPS[i])), xy(Travel.BALLOON_TILES[i]), Travel.BALLOON_STOPS[i]);
        }
    }

    @Test
    void fixedFareBoatsMatch() throws IOException {
        List<Map<String, String>> src = new ArrayList<>(rows("ships.tsv"));
        src.addAll(rows("boats.tsv"));
        for (Travel.Route route : Travel.all()) {
            if (!route.id().startsWith("boat_")) {
                continue;
            }
            for (Travel.Dest d : route.dests()) {
                boolean found = false;
                for (Map<String, String> r : src) {
                    if (same(tile(r.get("Destination")), d.x(), d.y()) && coins(r.get("Items")) == d.price()) {
                        found = true;
                    }
                }
                assertTrue(found, route.id() + " -> " + d.name() + " at " + d.x() + "," + d.y() + " for " + d.price());
            }
        }
    }

    // ------------------------------------------------------------------ islands / zeah networks

    @Test
    void mushtreesMatch() throws IOException {
        Map<String, int[]> dest = new HashMap<>();
        for (Map<String, String> r : rows("magic_mushtrees.tsv")) {
            if (r.get("Origin").isEmpty()) {
                dest.put(norm(r.get("Display info")), tile(r.get("Destination")));
            }
        }
        // Every tree stands on its OSRS tile (Verdant Valley's tile is the arrival tile beside its tree).
        for (IslandsTravel.Stop s : IslandsTravel.MUSHTREES.stops()) {
            int[] src = dest.get(norm(s.name()));
            assertNotNull(src, s.name());
            int[] t = ringTile(s);
            assertTrue(Math.abs(t[0] - src[0]) <= 1 && Math.abs(t[1] - src[1]) <= 1, s.name() + " " + xy(t) + " vs " + xy(src));
        }
        assertEquals(dest.size(), IslandsTravel.MUSHTREES.stops().size());
    }

    @Test
    void quetzalsMatch() throws IOException {
        Map<String, int[]> dest = new LinkedHashMap<>();
        Set<String> need = new HashSet<>();
        for (Map<String, String> r : rows("quetzals.tsv")) {
            if (r.get("Origin").isEmpty()) {
                dest.put(norm(r.get("Display info")), tile(r.get("Destination")));
                need.addAll(quests(r.get("Quests")));
            }
        }
        assertEquals(dest.size(), ZeahTravel.QUETZAL.stops().size());
        for (ZeahTravel.Stop s : ZeahTravel.QUETZAL.stops()) {
            int[] src = dest.get(norm(s.name().replaceFirst("^the ", "")));
            assertNotNull(src, s.name());
            assertTrue(same(src, s.x(), s.y()), s.name());
        }
        assertEquals(need, Set.of(ZeahTravel.QUETZAL.quest()));
    }

    @Test
    void lovakengjMinecartsMatch() throws IOException {
        Map<String, int[]> dest = new HashMap<>();
        for (Map<String, String> r : rows("minecarts.tsv")) {
            if (r.get("Origin").isEmpty() && r.get("Display info").matches("[0-9A-C]: .*")) {
                dest.put(norm(r.get("Display info")), tile(r.get("Destination")));
            }
        }
        assertEquals(12, dest.size());
        assertEquals(dest.size(), ZeahTravel.MINECART.stops().size());
        for (ZeahTravel.Stop s : ZeahTravel.MINECART.stops()) {
            int[] src = dest.get(norm(s.name()));
            assertNotNull(src, s.name());
            assertTrue(same(src, s.x(), s.y()), s.name());
        }
        assertEquals(20, ZeahTravel.MINECART.fare(false));
        assertEquals(0, ZeahTravel.MINECART.fare(true));
        assertEquals("the_forsaken_tower", ZeahTravel.MINECART.freeAfter());
    }

    // ------------------------------------------------------------------ teleport items

    private static final Map<String, List<Teleports.Destination>> JEWELLERY = Map.ofEntries(
            Map.entry("Amulet of glory", Teleports.GLORY), Map.entry("Ring of dueling", Teleports.DUELING),
            Map.entry("Games necklace", Teleports.GAMES), Map.entry("Skills necklace", Teleports.SKILLS),
            Map.entry("Combat bracelet", Teleports.COMBAT), Map.entry("Ring of wealth", Teleports.WEALTH),
            Map.entry("Digsite pendant", Teleports.DIGSITE), Map.entry("Necklace of passage", Teleports.PASSAGE),
            Map.entry("Burning amulet", Teleports.BURNING), Map.entry("Xeric's talisman", Teleports.XERIC),
            Map.entry("Drakan's medallion", Teleports.DRAKAN), Map.entry("Ectophial", Teleports.ECTOPHIAL),
            Map.entry("Slayer ring", Teleports.SLAYER), Map.entry("Pharaoh's sceptre", Teleports.PHARAOH),
            Map.entry("Ring of the elements", Teleports.ELEMENTS), Map.entry("Giantsoul amulet", Teleports.GIANTSOUL),
            Map.entry("Pendant of ates", Teleports.ATES), Map.entry("Teleport crystal", Teleports.TELEPORT_CRYSTAL),
            Map.entry("Eternal teleport crystal", Teleports.TELEPORT_CRYSTAL), Map.entry("Royal seed pod", Teleports.SEED_POD),
            Map.entry("Grand seed pod", Teleports.SEED_POD), Map.entry("Icy basalt", Teleports.ICY_BASALT),
            Map.entry("Stony basalt", Teleports.STONY_BASALT), Map.entry("Amulet of the eye", Teleports.AMULET_OF_THE_EYE),
            Map.entry("Chronicle", Teleports.CHRONICLE), Map.entry("Mythical cape", Teleports.MYTHICAL_CAPE),
            Map.entry("Music cape", Teleports.MUSIC_CAPE), Map.entry("Kharedst's memoirs", Teleports.MEMOIRS),
            Map.entry("Book of the dead", Teleports.MEMOIRS), Map.entry("Hallowed crystal shard", Teleports.HALLOWED_SHARD),
            Map.entry("Camulet", Teleports.CAMULET), Map.entry("Enchanted lyre", Teleports.LYRE),
            Map.entry("Sailors' amulet", Teleports.SAILORS));
    private static final Map<String, String> ITEM_IDS = Map.ofEntries(
            Map.entry("Amulet of glory", "amulet_of_glory_4"), Map.entry("Ring of dueling", "ring_of_dueling_8"),
            Map.entry("Games necklace", "games_necklace_8"), Map.entry("Skills necklace", "skills_necklace_4"),
            Map.entry("Combat bracelet", "combat_bracelet_4"), Map.entry("Ring of wealth", "ring_of_wealth_5"),
            Map.entry("Digsite pendant", "digsite_pendant_5"), Map.entry("Necklace of passage", "necklace_of_passage_5"),
            Map.entry("Burning amulet", "burning_amulet_5"), Map.entry("Xeric's talisman", "xerics_talisman"),
            Map.entry("Drakan's medallion", "drakans_medallion"), Map.entry("Ectophial", "ectophial"),
            Map.entry("Slayer ring", "slayer_ring_8"), Map.entry("Pharaoh's sceptre", "pharaohs_sceptre"),
            Map.entry("Ring of the elements", "ring_of_the_elements"), Map.entry("Giantsoul amulet", "giantsoul_amulet"),
            Map.entry("Pendant of ates", "pendant_of_ates"), Map.entry("Teleport crystal", "teleport_crystal"),
            Map.entry("Eternal teleport crystal", "eternal_teleport_crystal"), Map.entry("Royal seed pod", "royal_seed_pod"),
            Map.entry("Grand seed pod", "grand_seed_pod"), Map.entry("Icy basalt", "icy_basalt"), Map.entry("Stony basalt", "stony_basalt"),
            Map.entry("Amulet of the eye", "amulet_of_the_eye"), Map.entry("Chronicle", "chronicle"),
            Map.entry("Mythical cape", "mythical_cape"), Map.entry("Music cape", "music_cape"),
            Map.entry("Kharedst's memoirs", "kharedsts_memoirs"), Map.entry("Book of the dead", "book_of_the_dead"),
            Map.entry("Hallowed crystal shard", "hallowed_crystal_shard"), Map.entry("Camulet", "camulet"),
            Map.entry("Enchanted lyre", "enchanted_lyre"), Map.entry("Sailors' amulet", "sailors_amulet_inert"));
    /** RuneCraft destination names that differ from the snapshot's. */
    private static final Map<String, String> ALIAS = Map.ofEntries(Map.entry("Cooks' Guild", "Cooking Guild"),
            Map.entry("Edgeville Monastery", "Monastery"), Map.entry("Dondakan's Rock", "Dondakan"),
            Map.entry("Wintertodt Camp", "Wintertodt"), Map.entry("Eagles' Eyrie", "Eagle's Eyrie"), Map.entry("Ectofuntus", ""),
            Map.entry("Stronghold Slayer Cave", "Stronghold"), Map.entry("Fremennik Slayer Dungeon", "Fremennik"),
            Map.entry("Air Altar", "Air"), Map.entry("Water Altar", "Water"), Map.entry("Earth Altar", "Earth"),
            Map.entry("Fire Altar", "Fire"), Map.entry("Falo the Bard", "Teleport"));
    /** OSRS destinations not offered (yet). */
    private static final Set<String> NOT_OFFERED = Set.of();

    @Test
    void teleportJewelleryMatches() throws IOException {
        Map<String, Map<String, String>> byInfo = new HashMap<>();
        for (Map<String, String> r : rows("teleportation_items.tsv")) {
            byInfo.putIfAbsent(r.get("Display info"), r);
        }
        for (Map.Entry<String, List<Teleports.Destination>> e : JEWELLERY.entrySet()) {
            String item = e.getKey();
            Set<String> offered = new HashSet<>();
            for (Teleports.Destination d : e.getValue()) {
                String name = ALIAS.getOrDefault(d.name(), d.name());
                Map<String, String> r = null;
                String info = null;
                for (Map.Entry<String, Map<String, String>> s : byInfo.entrySet()) {
                    String k = s.getKey();
                    if (name.isEmpty() ? k.equals(item) : k.startsWith(item + ": ") && norm(k.substring(item.length() + 2)).equals(norm(name))) {
                        r = s.getValue();
                        info = k;
                    }
                }
                if (r == null && byInfo.containsKey(item)) { // the item's own (unnamed) teleport, e.g. "Stony basalt"
                    r = byInfo.get(item);
                    info = item;
                }
                assertNotNull(r, item + ": " + d.name());
                offered.add(info);
                int[] t = tile(r.get("Destination"));
                if (!RELOCATED.containsKey(info)) {
                    assertTrue(same(t, d.x(), d.y()), info + " at " + d.x() + "," + d.y() + ", OSRS " + xy(t));
                }
                Set<String> q = quests(r.get("Quests"));
                if (r.get("Varbits").contains("6528=207")) {
                    q.add("making_friends_with_my_arm"); // that quest's progress varbit at its completed value
                }
                assertEquals(q, d.quest() == null ? Set.of() : Set.of(d.quest()), info + " quest");
                assertEquals(Integer.parseInt(r.get("Wilderness level")), Teleports.wildernessLimit(ITEM_IDS.get(item)), info + " wilderness");
            }
            for (String k : byInfo.keySet()) {
                if ((k.startsWith(item + ": ") || k.equals(item)) && !NOT_OFFERED.contains(k)) {
                    assertTrue(offered.contains(k), "missing " + k);
                }
            }
        }
    }

    /**
     * Charges: the snapshot lists one OSRS item id per charge state (plus trimmed/imbued copies), and marks a teleport
     * "Consumable" when it uses a charge. RuneCraft's charge states must match, and unlimited items must not use any.
     */
    @Test
    void teleportJewelleryChargesMatch() throws IOException {
        // display name -> {RuneCraft charge base, OSRS variants per charge state (glory (t), ring of wealth (i))}
        Map<String, String[]> charged = Map.of("Games necklace", new String[]{"games_necklace", "1"},
                "Ring of dueling", new String[]{"ring_of_dueling", "1"}, "Combat bracelet", new String[]{"combat_bracelet", "1"},
                "Skills necklace", new String[]{"skills_necklace", "1"}, "Amulet of glory", new String[]{"amulet_of_glory", "2"},
                "Ring of wealth", new String[]{"ring_of_wealth", "2"}, "Slayer ring", new String[]{"slayer_ring", "1"},
                "Digsite pendant", new String[]{"digsite_pendant", "1"}, "Necklace of passage", new String[]{"necklace_of_passage", "1"},
                "Burning amulet", new String[]{"burning_amulet", "1"});
        Map<String, Integer> mine = new HashMap<>();
        for (com.runecraft.osrs.content.systems.Jewellery.Worn w : com.runecraft.osrs.content.systems.Jewellery.all()) {
            if (w.chargeBase() != null) {
                mine.merge(w.chargeBase(), w.charges(), Math::max);
            }
        }
        Set<String> checked = new HashSet<>();
        for (Map<String, String> r : rows("teleportation_items.tsv")) {
            String info = r.get("Display info");
            int colon = info.indexOf(": ");
            String item = colon < 0 ? info : info.substring(0, colon);
            String[] c = charged.get(item);
            if (c == null || !"T".equals(r.get("Consumable")) || !checked.add(item)) {
                continue;
            }
            int ids = r.get("Items").split("\\|\\|").length;
            assertEquals(ids / Integer.parseInt(c[1]), mine.getOrDefault(c[0], 0), item + " charges");
        }
        assertEquals(charged.keySet(), checked);
        // Unlimited versions: the eternal glory and eternal slayer ring use no charges ("Consumable" F).
        for (String id : List.of("amulet_of_eternal_glory", "slayer_ring_eternal", "drakans_medallion")) {
            com.runecraft.osrs.content.systems.Jewellery.Worn w = com.runecraft.osrs.content.systems.Jewellery.worn(id);
            assertNotNull(w, id);
            assertEquals(null, w.chargeBase(), id);
            assertFalse(w.teleports().isEmpty(), id);
        }
        assertEquals(Teleports.SLAYER, com.runecraft.osrs.content.systems.Jewellery.worn("slayer_ring_eternal").teleports());
        // Xeric's talisman: one charge per teleport ("Consumable" T), kept per stack: one per lizardman fang, at most 1,000.
        com.runecraft.osrs.content.systems.Charges.Spec xeric = com.runecraft.osrs.content.systems.Charges.of("xerics_talisman");
        assertNotNull(xeric);
        assertEquals(1000, xeric.max());
        assertEquals(Map.of("lizardman_fang", 1), xeric.materials());
        assertEquals(com.runecraft.osrs.content.systems.Charges.Drain.TELEPORT, xeric.drain());
        assertEquals("ancient_tablet", Teleports.unlockItem(Teleports.XERICS_HONOUR));
        assertTrue(com.runecraft.osrs.ItemCatalog.exists("ancient_tablet"));
        for (int n = 1; n <= 8; n++) {
            assertTrue(com.runecraft.osrs.ItemCatalog.exists("slayer_ring_" + n), "slayer_ring_" + n);
        }
        // Round 17 teleport items (wiki): per-stack charges, one per teleport ("Consumable" T in the snapshot).
        Map<String, Object[]> perStack = Map.of(
                "ring_of_the_elements", new Object[]{10_000, Map.of("air_rune", 1, "water_rune", 1, "earth_rune", 1, "fire_rune", 1,
                        "law_rune", 1)},
                "giantsoul_amulet", new Object[]{16_000, Map.of("big_bones", 1, "law_rune", 1)},
                "pendant_of_ates", new Object[]{1_000, Map.of("frozen_tear", 1)},
                "pharaohs_sceptre", new Object[]{100, Map.of()});
        for (Map.Entry<String, Object[]> e : perStack.entrySet()) {
            com.runecraft.osrs.content.systems.Charges.Spec spec = com.runecraft.osrs.content.systems.Charges.of(e.getKey());
            assertNotNull(spec, e.getKey());
            assertEquals(e.getValue()[0], spec.max(), e.getKey() + " max charges");
            assertEquals(e.getValue()[1], spec.materials(), e.getKey() + " charge cost");
            assertEquals(com.runecraft.osrs.content.systems.Charges.Drain.TELEPORT, spec.drain(), e.getKey());
            assertFalse(com.runecraft.osrs.content.systems.Charges.depleted(spec, 0), e.getKey() + " keeps its stats when empty");
        }
        for (Map<String, String> r : rows("teleportation_items.tsv")) {
            String info = r.get("Display info");
            if (info.startsWith("Pharaoh's sceptre") || info.startsWith("Ring of the elements") || info.startsWith("Giantsoul amulet")
                    || info.startsWith("Pendant of ates")) {
                assertEquals("T", r.get("Consumable"), info);
            }
        }
        // Pharaoh's sceptre: 3 charges, 10/25/50/100 with each Desert Diary tier; a hand-held item, not a worn one.
        assertEquals(List.of(3, 10, 25, 50, 100), java.util.stream.IntStream.rangeClosed(0, 4)
                .map(com.runecraft.osrs.content.systems.Charges::pharaohsSceptreCap).boxed().toList());
        assertEquals(null, com.runecraft.osrs.content.systems.Jewellery.worn("pharaohs_sceptre"));
        assertEquals(Teleports.PHARAOH, com.runecraft.osrs.content.systems.Jewellery.held("pharaohs_sceptre").teleports());
        // Ring of returning: 5 charges, crumbles; made from a jade ring with Lvl-2 Enchant.
        assertEquals(5, mine.get("ring_of_returning"));
        assertTrue(com.runecraft.osrs.content.systems.Jewellery.worn("ring_of_returning_1").crumbles());
        assertEquals("ring_of_returning_5", com.runecraft.osrs.content.systems.Jewellery.ENCHANT.get("jade_ring"));
        assertEquals(2, com.runecraft.osrs.content.systems.Jewellery.ENCHANT_LEVEL.get("jade_ring"));
        assertTrue(com.runecraft.osrs.ItemCatalog.exists("jade_ring"));
        // Round 27: a Pyramid Plunder sceptre starts uncharged; the skull sceptre holds 10 + 4 per Varrock Diary tier.
        assertEquals(0, com.runecraft.osrs.content.systems.Charges.of("pharaohs_sceptre").initial());
        assertEquals(List.of(10, 14, 18, 22, 26), java.util.stream.IntStream.rangeClosed(0, 4)
                .map(com.runecraft.osrs.content.systems.Charges::skullSceptreCap).boxed().toList());
        assertEquals(26, com.runecraft.osrs.content.systems.Charges.of("skull_sceptre").max());
        // Teleport crystal: one OSRS item per charge state, (1)-(5), "Consumable" T; the eternal crystal uses none.
        for (Map<String, String> r : rows("teleportation_items.tsv")) {
            String info = r.get("Display info");
            if (info.startsWith("Teleport crystal: ")) {
                assertEquals("T", r.get("Consumable"));
                assertEquals(r.get("Items").split("\\|\\|").length, com.runecraft.osrs.content.systems.Charges.of("teleport_crystal").max());
            }
            if (info.startsWith("Eternal teleport crystal") || info.startsWith("Royal seed pod") || info.startsWith("Mythical cape")
                    || info.startsWith("Music cape") || info.startsWith("Amulet of the eye")) {
                assertEquals("F", r.get("Consumable"), info);
            }
            if (info.startsWith("Grand seed pod") || info.startsWith("Icy basalt") || info.startsWith("Stony basalt")
                    || info.startsWith("Hallowed crystal shard")) {
                assertEquals("T", r.get("Consumable"), info);
                assertTrue(com.runecraft.osrs.content.systems.Jewellery.SINGLE_USE.contains(ITEM_IDS.get(info.replaceAll(":.*", ""))), info);
            }
        }
        assertEquals(1000, com.runecraft.osrs.content.systems.Charges.of("chronicle").max());
        assertEquals(Map.of("teleport_card", 1), com.runecraft.osrs.content.systems.Charges.of("chronicle").materials());
        // Charges that belong to the player: the ring of the elements and the camulet (refilled by one bucket of dung).
        assertTrue(com.runecraft.osrs.content.systems.Charges.perPlayer("ring_of_the_elements"));
        assertTrue(com.runecraft.osrs.content.systems.Charges.perPlayer("camulet"));
        assertFalse(com.runecraft.osrs.content.systems.Charges.perPlayer("pendant_of_ates")); // "the charges are not shared"
        // Charges a ring or camulet held on its stack before they moved to the player carry over (the larger count wins).
        com.runecraft.osrs.content.systems.Charges.Spec ring = com.runecraft.osrs.content.systems.Charges.of("ring_of_the_elements");
        com.runecraft.osrs.content.systems.Charges.Spec camulet = com.runecraft.osrs.content.systems.Charges.of("camulet");
        assertEquals(0, com.runecraft.osrs.content.systems.Charges.perPlayerCharges(ring, null, null));
        assertEquals(100, com.runecraft.osrs.content.systems.Charges.perPlayerCharges(ring, null, 100));
        assertEquals(100, com.runecraft.osrs.content.systems.Charges.perPlayerCharges(ring, 40, 100));
        assertEquals(120, com.runecraft.osrs.content.systems.Charges.perPlayerCharges(ring, 120, 100));
        assertEquals(40, com.runecraft.osrs.content.systems.Charges.perPlayerCharges(ring, 40, null));
        assertEquals(4, com.runecraft.osrs.content.systems.Charges.perPlayerCharges(camulet, null, null));
        assertEquals(1, com.runecraft.osrs.content.systems.Charges.perPlayerCharges(camulet, null, 1));
        assertEquals(0, com.runecraft.osrs.content.systems.Charges.perPlayerCharges(camulet, 0, null));
        assertEquals(4, com.runecraft.osrs.content.systems.Charges.camuletRefill(0, 1));
        assertEquals(3, com.runecraft.osrs.content.systems.Charges.camuletRefill(1, 1));
        assertEquals(0, com.runecraft.osrs.content.systems.Charges.camuletRefill(4, 1));
        assertEquals(0, com.runecraft.osrs.content.systems.Charges.camuletRefill(0, 0));
        assertTrue(com.runecraft.osrs.ItemCatalog.exists("ugthanki_dung"));
        assertTrue(com.runecraft.osrs.ItemCatalog.exists("teleport_card"));
        for (String id : ITEM_IDS.values()) {
            assertTrue(com.runecraft.osrs.ItemCatalog.exists(id), id);
            assertTrue(com.runecraft.osrs.content.systems.Jewellery.worn(id) != null || com.runecraft.osrs.content.systems.Jewellery.held(id) != null
                    || id.equals("slayer_ring_8") || id.startsWith("ring_of_dueling") || id.equals("pharaohs_sceptre"), id + " teleports");
        }
        // North Aldarin's statue of Ates needs Death on the Isle.
        assertEquals("death_on_the_isle", Teleports.activationQuest(Teleports.ATES.get(3)));
        assertEquals(null, Teleports.activationQuest(Teleports.ATES.get(0)));
        // Visit-unlocked destinations: the six statues of Ates and the Necropolis obelisk.
        for (Teleports.Destination d : Teleports.ATES) {
            assertTrue(Teleports.needsVisit(d), d.name());
        }
        assertTrue(Teleports.needsVisit(Teleports.PHARAOH.get(3)));
        assertFalse(Teleports.needsVisit(Teleports.PHARAOH.get(0)));
    }

    /** No teleport destination added for an item may land in water (the in-game self-test checks the rest). */
    @Test
    void newTeleportDestinationsAreOnLand() {
        List<Teleports.Destination> all = new ArrayList<>();
        for (List<Teleports.Destination> l : List.of(Teleports.PHARAOH, Teleports.ELEMENTS, Teleports.RETURNING, Teleports.GIANTSOUL,
                Teleports.ATES, Teleports.TELEPORT_CRYSTAL, Teleports.SEED_POD, Teleports.ICY_BASALT, Teleports.STONY_BASALT,
                Teleports.AMULET_OF_THE_EYE, Teleports.CHRONICLE, Teleports.MYTHICAL_CAPE, Teleports.MUSIC_CAPE, Teleports.MEMOIRS,
                Teleports.HALLOWED_SHARD, Teleports.CAMULET, Teleports.LYRE, Teleports.SAILORS)) {
            all.addAll(l);
        }
        for (Teleports.Destination d : all) {
            if (Teleports.underground(d)) {
                continue;
            }
            assertFalse(com.runecraft.osrs.world.Elevation.isWater(GielinorMap.toBlockX(d.x()), GielinorMap.toBlockZ(d.y())),
                    d.name() + " at " + d.x() + "," + d.y() + " is in water");
        }
    }

    @Test
    void skillcapeTeleportsMatch() throws IOException {
        Map<String, int[]> src = new HashMap<>();
        for (Map<String, String> r : rows("teleportation_items.tsv")) {
            src.putIfAbsent(r.get("Display info"), tile(r.get("Destination")));
        }
        Map<String, Skill> capes = Map.of("Crafting cape", Skill.CRAFTING, "Fishing cape", Skill.FISHING, "Farming cape",
                Skill.FARMING, "Strength cape", Skill.STRENGTH, "Hunter cape", Skill.HUNTER, "Construction cape", Skill.CONSTRUCTION);
        for (Map.Entry<String, Skill> e : capes.entrySet()) {
            for (Teleports.Destination d : com.runecraft.osrs.content.systems.Capes.capeTeleports(e.getValue())) {
                int[] t = null;
                for (Map.Entry<String, int[]> s : src.entrySet()) {
                    String k = s.getKey();
                    if (k.startsWith(e.getKey() + ": ") && (norm(k).endsWith(norm(d.name())) || k.endsWith(": Teleport"))) {
                        t = s.getValue();
                    }
                }
                assertNotNull(t, e.getKey() + ": " + d.name());
                assertTrue(same(t, d.x(), d.y()), e.getKey() + ": " + d.name() + " at " + d.x() + "," + d.y() + ", OSRS " + xy(t));
            }
        }
    }

    @Test
    void teleportTabletsMatch() throws IOException {
        Map<String, int[]> src = new HashMap<>();
        for (Map<String, String> r : rows("teleportation_items.tsv")) {
            src.putIfAbsent(norm(r.get("Display info")), tile(r.get("Destination")));
        }
        int checked = 0;
        for (Spellbooks.Tablet t : Spellbooks.TABLETS) {
            String base = t.name().replaceFirst(" teleport$", "");
            String key = src.containsKey(norm(base + " tablet")) ? base + " tablet" : t.name();
            int[] osrs = src.get(norm(key));
            if (osrs == null) {
                continue; // not in the snapshot (e.g. Kourend castle tablet)
            }
            String label = Character.toUpperCase(key.charAt(0)) + key.substring(1);
            if (RELOCATED.keySet().stream().anyMatch(k -> norm(k).equals(norm(key)))) {
                continue;
            }
            assertTrue(same(osrs, t.destination().x(), t.destination().y()),
                    label + " at " + t.destination().x() + "," + t.destination().y() + ", OSRS " + xy(osrs));
            checked++;
        }
        assertTrue(checked >= 40, "tablets checked " + checked);
    }
}
