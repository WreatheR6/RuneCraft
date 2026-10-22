package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.dungeons.Dungeons;
import com.runecraft.osrs.world.SpawnPoint;
import com.runecraft.osrs.world.SpawnTable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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
 * Konar quo Maten names a location for each task, not a kingdom (OSRS Wiki "Konar quo Maten", snapshot
 * {@code konar_locations.txt}): her list per task, the built locations it resolves to, and kills counting only there.
 */
class KonarLocationsTest {
    private static List<String> snapshot() throws IOException {
        String fixture = "osrs/slayer/konar_locations.txt";
        Path p = Path.of("src/test/resources", fixture);
        List<String> raw;
        if (Files.exists(p)) {
            raw = Files.readAllLines(p, StandardCharsets.UTF_8);
        } else {
            try (InputStream in = KonarLocationsTest.class.getClassLoader().getResourceAsStream(fixture)) {
                assertNotNull(in, "missing " + fixture);
                raw = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
            }
        }
        List<String> out = new ArrayList<>();
        for (String l : raw) {
            if (!l.isBlank() && !l.startsWith("#")) {
                out.add(l);
            }
        }
        return out;
    }

    @Test
    void locationListsMatchTheWiki() throws IOException {
        Map<String, List<String>> wiki = new LinkedHashMap<>();
        for (String row : snapshot()) {
            String[] f = row.split("\t", -1);
            String category = f[0].toLowerCase(Locale.ROOT).replace(' ', '_');
            wiki.put(category.equals("aviansie") ? "aviansies" : category, Arrays.asList(f[1].split(";")));
        }
        assertEquals(wiki, KonarLocations.WIKI);
        // every one of her tasks but the boss tasks has its list
        for (SlayerMasters.Assignment a : SlayerMasters.get("konar").assignments()) {
            if (!a.category().equals(SlayerBossTasks.CATEGORY)) {
                assertTrue(KonarLocations.WIKI.containsKey(a.category()), a.category());
            }
        }
    }

    @Test
    void everyTaskHasABuiltLocationHoldingItsMonsters() {
        for (Map.Entry<String, List<String>> e : KonarLocations.WIKI.entrySet()) {
            List<KonarLocations.Location> locations = KonarLocations.candidates(e.getKey());
            assertFalse(locations.isEmpty(), e.getKey());
            for (KonarLocations.Location l : locations) {
                assertTrue(e.getValue().stream().anyMatch(n -> KonarLocations.byName(n) == l), e.getKey() + ": " + l.name());
            }
        }
        // a few she names in OSRS that are built here
        assertEquals(List.of("Catacombs of Kourend", "Slayer Tower", "Stronghold Slayer Cave"),
                KonarLocations.candidates("aberrant_spectres").stream().map(KonarLocations.Location::name).toList());
        assertEquals(List.of("Karuulm Slayer Dungeon"),
                KonarLocations.candidates("hydras").stream().map(KonarLocations.Location::name).toList());
        assertTrue(KonarLocations.candidates("trolls").contains(KonarLocations.byName("Death Plateau")));
        assertTrue(KonarLocations.candidates("lizardmen").contains(KonarLocations.byName("Lizardman Canyon")));
    }

    @Test
    void namesQuestsAndBossesExist() {
        for (KonarLocations.Location l : KonarLocations.all()) {
            assertEquals(KonarLocations.id(l.name()), l.id());
            assertTrue(KonarLocations.WIKI.values().stream().anyMatch(v -> v.contains(l.name())), l.name() + " is on her list");
            if (l.quest() != null) {
                assertNotNull(ContentRegistry.quest(l.quest()), l.quest());
            }
            for (String b : l.bosses()) {
                assertNotNull(ContentRegistry.boss(b), b);
            }
            for (KonarLocations.Area a : l.areas()) {
                if (a.dungeon() != null) {
                    assertTrue(Dungeons.all().stream().anyMatch(d -> d.id().equals(a.dungeon())), a.dungeon());
                }
            }
        }
        assertEquals("song_of_the_elves", KonarLocations.byName("Iorwerth Dungeon").quest());
        assertEquals(KonarLocations.byName("Stronghold Slayer Cave"), KonarLocations.byName("Stronghold Slayer Dungeon"));
    }

    @Test
    void questLockedLocationsAreNotNamed() {
        List<KonarLocations.Location> none = KonarLocations.candidates("kurask", q -> false);
        assertEquals(List.of(KonarLocations.byName("Fremennik Slayer Dungeon")), none);
        assertEquals(2, KonarLocations.candidates("kurask", q -> true).size());
        assertTrue(KonarLocations.candidates("mutated_zygomites", q -> false).isEmpty(),
                "both their places need a quest: the kingdom rule applies");
    }

    /** A kill counts where the task's monsters spawn in the location, and not in another of her locations for it. */
    @Test
    void killsCountOnlyInTheNamedLocation() {
        for (String category : KonarLocations.WIKI.keySet()) {
            Set<String> ids = KonarLocations.creatures(category);
            List<KonarLocations.Location> locations = KonarLocations.candidates(category);
            for (KonarLocations.Location l : locations) {
                int[] spot = spawnIn(l, ids);
                assertNotNull(spot, category + " in " + l.name());
                assertTrue(KonarLocations.counts(l.id(), spot[0], spot[1], spot[2], null), category + " in " + l.name());
                for (KonarLocations.Location other : locations) {
                    if (other != l) {
                        assertFalse(KonarLocations.counts(other.id(), spot[0], spot[1], spot[2], null),
                                category + ": a kill in " + l.name() + " counted for " + other.name());
                    }
                }
            }
        }
        // Cerberus counts for hellhounds in Taverley Dungeon wherever her lair is
        assertTrue(KonarLocations.counts("taverley_dungeon", 0, 300, 0, "cerberus"));
        assertFalse(KonarLocations.counts("taverley_dungeon", 0, 300, 0, "kraken"));
    }

    /** A block spot (x, feet y, z) where one of the monsters spawns inside the location. */
    private static int[] spawnIn(KonarLocations.Location l, Set<String> ids) {
        for (Dungeons.Camp c : Dungeons.camps()) {
            if (c.creatures().stream().anyMatch(ids::contains) && l.contains(c.centreX(), c.feetY(), c.centreZ())) {
                return new int[]{c.centreX(), c.feetY(), c.centreZ()};
            }
        }
        for (SpawnTable.CampRef c : SpawnTable.camps()) {
            int y = c.floorY() == SpawnPoint.SURFACE ? 70 : c.floorY();
            int bx = GielinorMap.toBlockX(c.x());
            int bz = GielinorMap.toBlockZ(c.y());
            if (ids.contains(c.creature()) && l.contains(bx, y, bz)) {
                return new int[]{bx, y, bz};
            }
        }
        for (SpawnPoint p : SpawnTable.all()) {
            int y = p.surface() ? 70 : p.floorY();
            if (ids.contains(p.monster()) && l.contains(p.blockX(), y, p.blockZ())) {
                return new int[]{p.blockX(), y, p.blockZ()};
            }
        }
        return null;
    }
}
