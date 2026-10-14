package com.runecraft.osrs.world;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
}
