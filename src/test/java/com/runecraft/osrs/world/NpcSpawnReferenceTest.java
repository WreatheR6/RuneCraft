package com.runecraft.osrs.world;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spawn locations against the OSRS cache. {@code src/test/resources/osrs/world/npc_spawns.tsv} is a snapshot of the
 * surface spawns in mejrs/data_osrs NPCList_OSRS.json for every NPC name RuneCraft places; every surface spawn point
 * and surface camp must lie near a cache spawn of its NPC, so enemies only appear where they do in OSRS. Carved dungeons
 * are re-anchored under their entrances and are not compared.
 */
class NpcSpawnReferenceTest {
    private static final String FIXTURE = "osrs/world/npc_spawns.tsv";

    /** RuneCraft ids whose cache spawns carry another name (disguised crabs, renamed elves). */
    private static final Map<String, List<String>> ALIASES = Map.of(
            "rock_crab", List.of("Rocks"),
            "sand_crab", List.of("Sandy rocks"),
            "swamp_crab", List.of("Swampy log"),
            "king_sand_crab", List.of("Sandy boulder"),
            "ammonite_crab", List.of("Fossil Rock"),
            "elf_warrior", List.of("Elf Warrior", "Iorwerth Warrior"),
            // Jatizso's and the Fremennik Isles' trolls are runts, males, females and grunts in the cache
            "ice_troll", List.of("Ice troll", "Ice troll runt", "Ice troll male", "Ice troll female", "Ice troll grunt"));

    /**
     * Known stand-ins, by creature id (any spawn) or {@code id@campSource}: monsters whose OSRS home is a dungeon or
     * realm that isn't carved, placed at its surface entrance instead.
     */
    private static final Map<String, String> STAND_INS = Map.ofEntries(
            Map.entry("fever_spider@braindeath_island", "Braindeath Island's brewery lies off the main map"),
            Map.entry("pirate@braindeath_island", "Braindeath Island lies off the main map"),
            Map.entry("moss_giant@isl_laguna_aurorae", "the cache snapshot predates Sailing; Laguna Aurorae's tiles are the wiki's"),
            Map.entry("mudskipper@isl_mudskipper_point", "the cache has no Mudskipper Point spawns (they appear when fished up)"),
            Map.entry("fortis_guard", "the cache snapshot has no Varlamore spawns, and plain \"Guard\" would match guards elsewhere"));

    private static Map<String, List<int[]>> cache() throws IOException {
        Path p = Path.of("src/test/resources", FIXTURE);
        List<String> lines;
        if (Files.exists(p)) {
            lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        } else {
            try (InputStream in = NpcSpawnReferenceTest.class.getClassLoader().getResourceAsStream(FIXTURE)) {
                assertTrue(in != null, "missing " + FIXTURE);
                lines = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
            }
        }
        Map<String, List<int[]>> out = new HashMap<>();
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\t");
            out.computeIfAbsent(f[0].toLowerCase(Locale.ROOT), k -> new ArrayList<>())
                    .add(new int[]{Integer.parseInt(f[2]), Integer.parseInt(f[3])});
        }
        return out;
    }

    /** Distance to the nearest cache spawn of the creature, or -1 if the cache has none under its names. */
    private static double nearest(Map<String, List<int[]>> cache, String id, int x, int y) {
        List<String> names = ALIASES.getOrDefault(id, List.of(SpawnTable.info(id).name()));
        double best = -1;
        for (String n : names) {
            for (int[] c : cache.getOrDefault(n.toLowerCase(Locale.ROOT), List.of())) {
                double d = Math.hypot(c[0] - x, c[1] - y);
                if (best < 0 || d < best) {
                    best = d;
                }
            }
        }
        return best;
    }

    /** Implings wander in from Puro-Puro anywhere in OSRS (only crystal implings have fixed spots). */
    private static boolean roams(String id) {
        return id.endsWith("_impling");
    }

    @Test
    void spawnPointsLieNearACacheSpawn() throws IOException {
        Map<String, List<int[]>> cache = cache();
        List<String> far = new ArrayList<>();
        int compared = 0;
        int surface = 0;
        for (SpawnPoint p : SpawnTable.all()) {
            if (!p.surface() && p.floorY() < StructureDef.GROUND || roams(p.monster()) || STAND_INS.containsKey(p.monster())) {
                continue;
            }
            surface++;
            double d = nearest(cache, p.monster(), p.x(), p.y());
            if (d < 0) {
                continue;
            }
            compared++;
            if (d > 15 + p.radius() / 2.0) {
                far.add(p.key() + " (" + Math.round(d) + " tiles)");
            }
        }
        assertTrue(far.isEmpty(), "spawn points far from any OSRS cache spawn of their NPC: " + far);
        assertTrue(compared >= surface * 0.8, "only " + compared + " of " + surface + " points have cache spawns: stale snapshot?");
    }

    @Test
    void surfaceCampsLieNearACacheSpawn() throws IOException {
        Map<String, List<int[]>> cache = cache();
        List<String> far = new ArrayList<>();
        for (SpawnTable.CampRef c : SpawnTable.camps()) {
            // Carved dungeons and the God Wars Dungeon (gwd_ creatures) are re-anchored under their entrances.
            if (c.floorY() != SpawnPoint.SURFACE || c.source().startsWith("dng_") || c.creature().startsWith("gwd_") || roams(c.creature())
                    || STAND_INS.containsKey(c.creature()) || STAND_INS.containsKey(c.creature() + "@" + c.source())) {
                continue;
            }
            double d = nearest(cache, c.creature(), c.x(), c.y());
            if (d > 15 + c.radius() / 2.0) {
                far.add(c.creature() + "@" + c.source() + " " + c.x() + "," + c.y() + " (" + Math.round(d) + " tiles)");
            }
        }
        assertTrue(far.isEmpty(), "surface camps far from any OSRS cache spawn of their NPC: " + far);
    }

    @Test
    void standInsAreStillNeeded() throws IOException {
        Map<String, List<int[]>> cache = cache();
        for (SpawnTable.CampRef c : SpawnTable.camps()) {
            if (STAND_INS.containsKey(c.creature() + "@" + c.source())) {
                double d = nearest(cache, c.creature(), c.x(), c.y());
                assertTrue(d < 0 || d > 15 + c.radius() / 2.0, c.creature() + "@" + c.source() + " is near its cache spawns now");
            }
        }
    }
}
