package com.runecraft.osrs.content.world;

import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.world.StructureDef;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuresTest {
    /** namespace:path with an optional [prop=value,...] block, like command syntax. */
    private static final Pattern BLOCK = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_/.-]+(\\[[a-z0-9_]+=[a-z0-9_]+(,[a-z0-9_]+=[a-z0-9_]+)*])?");

    @Test
    void structuresAreWellFormed() {
        Set<String> places = new HashSet<>();
        for (Landmark l : Landmark.values()) {
            places.add(l.name());
        }
        for (PlaceDef p : ContentRegistry.places()) {
            places.add(p.id());
        }
        Set<String> replaced = new HashSet<>();
        for (StructureDef def : ContentRegistry.structures()) {
            assertTrue(def.id().matches("[a-z0-9_]+"), "bad structure id " + def.id());
            assertTrue(!def.ops().isEmpty(), def.id());
            if (def.replacesLandmark() != null) {
                assertTrue(places.contains(def.replacesLandmark()), def.id() + " replaces unknown place " + def.replacesLandmark());
                assertTrue(replaced.add(def.replacesLandmark()), "two structures replace " + def.replacesLandmark());
            }
            assertTrue(def.maxX() - def.minX() <= 1024 && def.maxZ() - def.minZ() <= 1024, def.id() + " is implausibly large");
            for (StructureDef.Op op : def.ops()) {
                assertTrue(op.y1() >= -64 && op.y2() < 320, def.id() + " op out of build height: " + op);
                assertTrue((long) (op.x2() - op.x1() + 1) * (op.z2() - op.z1() + 1) * (op.y2() - op.y1() + 1) <= 4_000_000L,
                        def.id() + " op too big: " + op);
                assertTrue(op.block() != null && BLOCK.matcher(op.block()).matches(), def.id() + " bad block '" + op.block() + "'");
                if (op.type() == StructureDef.Op.Type.REPLACE) {
                    assertTrue(op.extra() != null && BLOCK.matcher(op.extra()).matches(), def.id() + " bad replace source");
                }
            }
        }
    }

    @Test
    void tileHelpersMatchTheMap() {
        StructureDef def = StructureDef.builder("t", "T").tSetTile(3222, 3218, 0, "minecraft:stone").build();
        assertEquals(0, def.minX());
        assertEquals(0, def.minZ());
        assertEquals(1, def.maxX());
        assertEquals(1, def.maxZ());
        // Tile north (higher y) is at smaller block z.
        StructureDef north = StructureDef.builder("n", "N").tSetTile(3222, 3219, 0, "minecraft:stone").build();
        assertEquals(-2, north.minZ());
    }
}
