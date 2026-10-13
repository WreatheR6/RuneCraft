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
 * teleport tablets. Destinations OSRS puts underground or on another plane go to the place this world builds
 * for them; those are listed in {@link #RELOCATED} with the reason.
 */
class TransportSnapshotTest {
    /** Destinations deliberately moved from their OSRS tile: name -> why. */
    private static final Map<String, String> RELOCATED = Map.ofEntries(
            Map.entry("Games necklace: Corporeal Beast", "lair built on the surface (corporeal_beast_lair)"),
            Map.entry("Games necklace: Tears of Guthix", "cave built on the surface (tears_of_guthix_cave)"),
            Map.entry("Skills necklace: Mining Guild", "OSRS lands underground; surface guild entrance"),
            Map.entry("Ring of wealth: Dondakan", "OSRS lands underground; mine entrance at White Wolf Mountain"),
            Map.entry("Digsite pendant: Lithkren", "OSRS lands in the vault; Lithkren's surface"),
            Map.entry("Drakan's medallion: Slepe", "OSRS lands in the sanctuary under Slepe; the village"),
            Map.entry("Paddewwa tablet", "Edgeville Dungeon is not built; surface above it"),
            Map.entry("Ape Atoll tablet", "OSRS lands in the Ape Atoll dungeon; the island above it"),
            Map.entry("Key master teleport", "Cerberus' lair is an instance; Taverley Dungeon entrance"));

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
        // Realms built elsewhere (CKP: the Cosmic Altar's plane, BJR: the Fisher Realm, DIR: Gorak's Plane).
        Set<String> otherRealms = Set.of("CKP", "BJR", "DIR");
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
        // Every surface ring exists (DIQ is the house ring; CJQ, the Great Conch, has no land in this world yet).
        for (Map.Entry<String, int[]> e : tiles.entrySet()) {
            int[] t = e.getValue();
            boolean surface = t[1] < 4100 && !Set.of("DIQ", "CJQ", "BKS").contains(e.getKey());
            if (surface) {
                assertTrue(seen.contains(e.getKey()), "missing fairy ring " + e.getKey() + " at " + xy(t));
            }
        }
        assertTrue(seen.size() >= 44, "rings " + seen.size());
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
        for (Map<String, String> r : rows("charter_ships.tsv")) {
            String from = r.get("section");
            String to = r.get("Display info");
            port.put(to, tile(r.get("Destination")));
            fare.put(from + ">" + to, coins(r.get("Items")));
            req.put(to, quests(r.get("Quests")));
        }
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
        // The House on the Hill's and Sticky Swamp's trees are part of hand-built structures, away from OSRS's (approx).
        Map<String, Integer> tolerance = Map.of("HOUSE_ON_THE_HILL", 20, "STICKY_SWAMP", 50);
        for (IslandsTravel.Stop s : IslandsTravel.MUSHTREES.stops()) {
            int[] src = dest.get(norm(s.name()));
            assertNotNull(src, s.name());
            int[] t = ringTile(s);
            int tol = tolerance.getOrDefault(s.code(), 10);
            assertTrue(Math.abs(t[0] - src[0]) <= tol && Math.abs(t[1] - src[1]) <= tol, s.name() + " " + xy(t) + " vs " + xy(src));
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
            Map.entry("Drakan's medallion", Teleports.DRAKAN), Map.entry("Ectophial", Teleports.ECTOPHIAL));
    private static final Map<String, String> ITEM_IDS = Map.ofEntries(
            Map.entry("Amulet of glory", "amulet_of_glory_4"), Map.entry("Ring of dueling", "ring_of_dueling_8"),
            Map.entry("Games necklace", "games_necklace_8"), Map.entry("Skills necklace", "skills_necklace_4"),
            Map.entry("Combat bracelet", "combat_bracelet_4"), Map.entry("Ring of wealth", "ring_of_wealth_5"),
            Map.entry("Digsite pendant", "digsite_pendant_5"), Map.entry("Necklace of passage", "necklace_of_passage_5"),
            Map.entry("Burning amulet", "burning_amulet_5"), Map.entry("Xeric's talisman", "xerics_talisman"),
            Map.entry("Drakan's medallion", "drakans_medallion"), Map.entry("Ectophial", "ectophial"));
    /** RuneCraft destination names that differ from the snapshot's. */
    private static final Map<String, String> ALIAS = Map.of("Cooks' Guild", "Cooking Guild", "Edgeville Monastery", "Monastery",
            "Dondakan's Rock", "Dondakan", "Wintertodt Camp", "Wintertodt", "Eagles' Eyrie", "Eagle's Eyrie",
            "Ectofuntus", "");
    /** OSRS destinations not offered (yet): Xeric's Honour needs an unlock this world doesn't model. */
    private static final Set<String> NOT_OFFERED = Set.of("Xeric's talisman: 5. Xeric's Honour");

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
                assertNotNull(r, item + ": " + d.name());
                offered.add(info);
                int[] t = tile(r.get("Destination"));
                if (!RELOCATED.containsKey(info)) {
                    assertTrue(same(t, d.x(), d.y()), info + " at " + d.x() + "," + d.y() + ", OSRS " + xy(t));
                }
                Set<String> q = quests(r.get("Quests"));
                assertEquals(q, d.quest() == null ? Set.of() : Set.of(d.quest()), info + " quest");
                assertEquals(Integer.parseInt(r.get("Wilderness level")), Teleports.wildernessLimit(ITEM_IDS.get(item)), info + " wilderness");
            }
            for (String k : byInfo.keySet()) {
                if (k.startsWith(item + ": ") && !NOT_OFFERED.contains(k)) {
                    assertTrue(offered.contains(k), "missing " + k);
                }
            }
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
