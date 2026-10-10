package com.runecraft.osrs.world;

import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Where the natural terrain has to make room for hand-built content, derived from the content
 * registry: towns and castles flatten their ground to a fixed level (see {@link StructureDef.Op.Type#FLATTEN}),
 * places and landmarks build plazas at their centre, and dungeons carve rooms deep underground.
 * {@link Elevation} uses these so structures sit naturally in the landscape.
 *
 * <p>All rectangles are inclusive Minecraft block coordinates.
 */
public final class TerrainAnchors {
    /** How an anchor shapes the terrain around it. */
    public enum Kind {
        /** Ground levelled exactly to {@code target} inside the rectangle, blending out over a few blocks, then clamped. */
        CORE,
        /** Terrain kept within a few blocks of {@code target} near the rectangle, rising no steeper than a slope beyond. */
        CLAMP,
        /** Solid rock up to at least {@code target} over the rectangle (dungeon rooms below). */
        FLOOR
    }

    /**
     * @param radius blocks around the rectangle over which the terrain stays within {@link #BAND} of the target
     * @param slope  how steeply (blocks per block) the terrain may depart from the target beyond that
     */
    public record Anchor(Kind kind, String source, int x1, int z1, int x2, int z2, double target, double radius, double slope) {
        /** Horizontal distance in blocks from (x, z) to the rectangle (0 inside). */
        public double distance(double x, double z) {
            double dx = Math.max(0, Math.max(x1 - x, x - x2));
            double dz = Math.max(0, Math.max(z1 - z, z - z2));
            return Math.sqrt(dx * dx + dz * dz);
        }
    }

    /** A hand-built body of water (harbour, moat, river channel) that the terrain keeps open. */
    public record WaterBox(String source, int x1, int z1, int x2, int z2, int bedTop) {
    }

    /** Cell size for grouping the many small block operations of a structure. */
    static final int CELL = 16;
    /** Underground operations whose top is below this y are dungeon rooms that need rock above them. */
    public static final int UNDERGROUND_BELOW = 56;
    /** Minimum rock cover above dungeon rooms, and the minimum surface height over them. */
    public static final int ROCK_COVER = 5;
    public static final int MIN_SURFACE_OVER_DUNGEONS = 56;
    /** Half-size of the levelled core at a place/landmark centre (bigger for boss arenas). */
    static final int PLACE_CORE = 6;
    /** Near an anchor the terrain stays within this many blocks of its level. */
    public static final double BAND = 6;
    /** Clamp radius/slope around levelled structure footprints... */
    public static final double STRUCTURE_RADIUS = 16;
    public static final double STRUCTURE_SLOPE = 1.5;
    /** ...around other surface parts of structures... */
    public static final double DETAIL_RADIUS = 12;
    /** ...and around place/landmark plazas. */
    public static final double PLACE_RADIUS = 8;
    public static final double PLACE_SLOPE = 1.75;

    /**
     * Places on mountain tops: their plazas are built on the natural ground (LandmarkBuilder follows
     * the heightmap), so the terrain is not pulled down to y 64 there.
     */
    public static final Set<String> MOUNTAIN_PLACES = Set.of("ice_mountain", "GOD_WARS_DUNGEON");

    /** Hand-built water that is later covered by the structure itself (not a terrain feature). */
    static final Set<String> COVERED_WATER = Set.of("ardougne_castle");

    private final List<Anchor> anchors;
    private final List<WaterBox> water;

    private TerrainAnchors(List<Anchor> anchors, List<WaterBox> water) {
        this.anchors = List.copyOf(anchors);
        this.water = List.copyOf(water);
    }

    public List<Anchor> anchors() {
        return anchors;
    }

    public List<WaterBox> water() {
        return water;
    }

    private static volatile TerrainAnchors instance;

    /** The anchors of all registered content (built once). */
    public static TerrainAnchors get() {
        TerrainAnchors a = instance;
        if (a == null) {
            synchronized (TerrainAnchors.class) {
                a = instance;
                if (a == null) {
                    a = fromRegistry();
                    instance = a;
                }
            }
        }
        return a;
    }

    /** Builds anchors from the structures, places and landmarks in {@link ContentRegistry}. */
    static TerrainAnchors fromRegistry() {
        List<Anchor> out = new ArrayList<>();
        List<WaterBox> water = new ArrayList<>();
        Map<Long, Double> clampCells = new HashMap<>();
        Map<Long, String> clampSource = new HashMap<>();
        Map<Long, Double> floorCells = new HashMap<>();
        Map<Long, String> floorSource = new HashMap<>();
        for (StructureDef def : ContentRegistry.structures()) {
            int ground = StructureDef.GROUND;
            List<StructureDef.Op> flats = new ArrayList<>();
            for (StructureDef.Op op : def.ops()) {
                if (op.type() == StructureDef.Op.Type.FLATTEN) {
                    if (flats.isEmpty()) {
                        ground = op.y1();
                    }
                    flats.add(op);
                    out.add(new Anchor(Kind.CORE, def.id(), op.x1(), op.z1(), op.x2(), op.z2(), op.y1(), STRUCTURE_RADIUS, STRUCTURE_SLOPE));
                }
            }
            for (StructureDef.Op op : def.ops()) {
                if (op.type() == StructureDef.Op.Type.FLATTEN) {
                    continue;
                }
                if (isWater(op) && op.y1() <= 61 && op.y2() >= 61 && area(op) >= 300 && !COVERED_WATER.contains(def.id())) {
                    water.add(new WaterBox(def.id(), op.x1(), op.z1(), op.x2(), op.z2(), op.y1()));
                }
                if (op.y2() < UNDERGROUND_BELOW) {
                    double need = Math.max(op.y2() + 1 + ROCK_COVER, MIN_SURFACE_OVER_DUNGEONS);
                    forCells(op, key -> {
                        if (floorCells.merge(key, need, Math::max) == need) {
                            floorSource.put(key, def.id());
                        }
                    });
                } else if (op.y2() >= 58 && op.y1() <= ground + 12 && !insideAny(op, flats)) {
                    final double g = ground;
                    forCells(op, key -> {
                        clampCells.putIfAbsent(key, g);
                        clampSource.putIfAbsent(key, def.id());
                    });
                }
            }
        }
        // Floor cells are exact op footprints merged per cell: use the cell rectangle clipped to nothing
        // finer than the cell (conservative: covers the whole cell).
        for (Map.Entry<Long, Double> e : floorCells.entrySet()) {
            int cx = (int) (e.getKey() >> 32);
            int cz = (int) (long) e.getKey();
            out.add(new Anchor(Kind.FLOOR, floorSource.get(e.getKey()), cx * CELL, cz * CELL, cx * CELL + CELL - 1, cz * CELL + CELL - 1, e.getValue(), 2, 1));
        }
        for (Map.Entry<Long, Double> e : clampCells.entrySet()) {
            int cx = (int) (e.getKey() >> 32);
            int cz = (int) (long) e.getKey();
            out.add(new Anchor(Kind.CLAMP, clampSource.get(e.getKey()), cx * CELL, cz * CELL, cx * CELL + CELL - 1, cz * CELL + CELL - 1,
                    e.getValue(), DETAIL_RADIUS, STRUCTURE_SLOPE));
        }
        // Places and landmarks: a levelled plaza at the centre, and boss arenas beside it (placed
        // exactly as LandmarkBuilder does). Each is levelled at its own natural height, so on a hillside
        // they become terraces. Places without a plaza (style NONE: NPC spots, sea chart markers...)
        // build nothing, so they keep the natural terrain.
        Map<String, List<BossDef>> arenas = new HashMap<>();
        for (BossDef b : ContentRegistry.bosses()) {
            if (b.place != null && !b.raidOnly) {
                arenas.computeIfAbsent(b.place, k -> new ArrayList<>()).add(b);
            }
        }
        Set<String> seen = new HashSet<>();
        for (PlaceDef p : ContentRegistry.places()) {
            if (!seen.add(p.id())) {
                continue;
            }
            if (hasPlaza(p)) {
                plaza(out, "place:" + p.id(), p.blockX(), p.blockZ(), PLACE_CORE);
            }
            arenas(out, p.id(), p.blockX(), p.blockZ(), arenas.get(p.id()));
        }
        for (Landmark l : Landmark.values()) {
            if (MOUNTAIN_PLACES.contains(l.name())) {
                continue;
            }
            plaza(out, "landmark:" + l.name(), l.blockX(), l.blockZ(), PLACE_CORE);
            arenas(out, l.name(), l.blockX(), l.blockZ(), arenas.get(l.name()));
        }
        return new TerrainAnchors(out, water);
    }

    private static void plaza(List<Anchor> out, String source, int x, int z, int r) {
        double level = plazaLevel(x, z);
        for (Anchor a : out) {
            // in or right next to a structure's levelled ground the plaza is built at that level
            if (a.kind() == Kind.CORE && !a.source().contains(":") && a.distance(x, z) <= a.radius() + r + TerrainAnchors.BAND) {
                level = a.target();
                break;
            }
        }
        out.add(new Anchor(Kind.CORE, source, x - r, z - r, x + r, z + r, level, PLACE_RADIUS, PLACE_SLOPE));
    }

    /** Boss arenas around a place centre, mirroring LandmarkBuilder's layout. */
    private static void arenas(List<Anchor> out, String placeId, int cx, int cz, List<BossDef> bosses) {
        if (bosses == null) {
            return;
        }
        int arena = 0;
        for (BossDef b : bosses) {
            int r = b.arenaRadius;
            int ax = cx + (arena % 2 == 0 ? 1 : -1) * (14 + r);
            int az = cz + (arena / 2) * (2 * r + 6);
            plaza(out, "arena:" + placeId + ":" + b.id, ax, az, r + 1);
            arena++;
        }
    }

    /**
     * Level of a plaza or arena. The landmark builder builds them on the natural ground, so the
     * terrain is levelled at the natural height of the centre (a terrace on a hillside, a clearing
     * on a mountain) - and never below town level, so a plaza in the sea gets an islet.
     */
    static double plazaLevel(int x, int z) {
        return Math.max(StructureDef.GROUND, Math.round(Elevation.naturalHeight(x, z)));
    }

    /** True if the landmark builder levels a plaza (or arena) at this place's centre. */
    public static boolean hasPlaza(PlaceDef p) {
        return !MOUNTAIN_PLACES.contains(p.id()) && p.style() != PlaceDef.Style.NONE;
    }

    private static boolean isWater(StructureDef.Op op) {
        return op.type() == StructureDef.Op.Type.FILL && op.block() != null && op.block().startsWith("minecraft:water");
    }

    private static int area(StructureDef.Op op) {
        return (op.x2() - op.x1() + 1) * (op.z2() - op.z1() + 1);
    }

    private static boolean insideAny(StructureDef.Op op, List<StructureDef.Op> flats) {
        for (StructureDef.Op f : flats) {
            if (op.x1() >= f.x1() && op.x2() <= f.x2() && op.z1() >= f.z1() && op.z2() <= f.z2()) {
                return true;
            }
        }
        return false;
    }

    private static void forCells(StructureDef.Op op, java.util.function.LongConsumer action) {
        for (int cx = Math.floorDiv(op.x1(), CELL); cx <= Math.floorDiv(op.x2(), CELL); cx++) {
            for (int cz = Math.floorDiv(op.z1(), CELL); cz <= Math.floorDiv(op.z2(), CELL); cz++) {
                action.accept(((long) cx << 32) | (cz & 0xFFFFFFFFL));
            }
        }
    }
}
