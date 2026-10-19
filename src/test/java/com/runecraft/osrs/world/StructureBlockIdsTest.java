package com.runecraft.osrs.world;

import com.runecraft.osrs.content.ContentRegistry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structure block strings are parsed as block states at build time; an entity or item id there parses to air and the
 * smoke server fails the build ("could not be parsed"). Catches the common entity-only ids offline.
 */
class StructureBlockIdsTest {
    private static final List<String> NOT_BLOCKS = List.of("armor_stand", "item_frame", "glow_item_frame", "painting",
            "minecart", "chest_minecart", "oak_boat", "spruce_boat", "end_crystal", "leash_knot");

    @Test
    void structureCodeUsesNoEntityIdsAsBlocks() throws IOException {
        Pattern p = Pattern.compile("\"minecraft:([a-z_]+)[\\[\"]");
        List<String> bad = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java/com/runecraft/osrs"))) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".java"))::iterator) {
                List<String> lines = Files.readAllLines(f);
                for (int i = 0; i < lines.size(); i++) {
                    Matcher m = p.matcher(lines.get(i));
                    while (m.find()) {
                        if (NOT_BLOCKS.contains(m.group(1))) {
                            bad.add(f.getFileName() + ":" + (i + 1) + " minecraft:" + m.group(1));
                        }
                    }
                }
            }
        }
        assertTrue(bad.isEmpty(), "entity ids used as structure blocks: " + bad);
    }

    /** Structures rebuilt from the OSRS map since round 26: every block state must exist in 1.20.1. */
    private static final Set<String> CHECKED = Set.of("watchtower", "rellekka", "rellekka_market", "burthorpe_castle",
            "burthorpe_games_room", "catherby", "draynor_village", "draynor_market", "draynor_manor",
            "taverley", "camelot_castle", "canifis", "pollnivneach", "nardah", "shilo_village",
            "dng_ape_atoll_dungeon", "dng_temple_of_marimbo_dungeon", "dng_waterbirth_dungeon");

    @Test
    void rebuiltStructuresUseValidBlockStates() throws IOException {
        // name -> property -> allowed values, from the PrismarineJS minecraft-data 1.20 snapshot
        Map<String, Map<String, Set<String>>> states = new HashMap<>();
        for (String line : Files.readAllLines(Path.of("src/test/resources/osrs/world/blocks_1_20_1.tsv"))) {
            if (line.startsWith("#") || line.isBlank()) {
                continue;
            }
            String[] c = line.split("\t", -1);
            Map<String, Set<String>> props = new HashMap<>();
            if (c.length > 1 && !c[1].isEmpty()) {
                for (String pv : c[1].split(";")) {
                    String[] kv = pv.split("=", 2);
                    props.put(kv[0], Set.of(kv[1].split(",")));
                }
            }
            states.put(c[0], props);
        }
        List<String> bad = new ArrayList<>();
        int seen = 0;
        for (StructureDef def : ContentRegistry.structures()) {
            if (!CHECKED.contains(def.id())) {
                continue;
            }
            seen++;
            for (StructureDef.Op op : def.ops()) {
                for (String block : new String[]{op.block(), op.type() == StructureDef.Op.Type.REPLACE ? op.extra() : null}) {
                    String err = check(states, block);
                    if (err != null) {
                        bad.add(def.id() + " " + op.type() + ": " + err);
                    }
                }
            }
        }
        assertTrue(seen == CHECKED.size(), "missing structures: found " + seen + " of " + CHECKED);
        assertTrue(bad.isEmpty(), "invalid block states: " + bad.stream().distinct().toList());
    }

    private static String check(Map<String, Map<String, Set<String>>> states, String block) {
        if (block == null || block.isEmpty() || block.equals("null")) {
            return null;
        }
        int br = block.indexOf('[');
        String id = br < 0 ? block : block.substring(0, br);
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        if (!id.startsWith("minecraft:")) {
            return null; // mod blocks are checked by the smoke server
        }
        Map<String, Set<String>> props = states.get(id.substring("minecraft:".length()));
        if (props == null) {
            return "unknown block " + block;
        }
        if (br >= 0) {
            if (!block.endsWith("]")) {
                return "malformed " + block;
            }
            for (String pv : block.substring(br + 1, block.length() - 1).split(",")) {
                String[] kv = pv.split("=", 2);
                Set<String> vals = props.get(kv[0].trim());
                if (kv.length < 2 || vals == null || !vals.contains(kv[1].trim())) {
                    return "bad property " + pv + " in " + block;
                }
            }
        }
        return null;
    }
}
