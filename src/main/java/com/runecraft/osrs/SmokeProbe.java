package com.runecraft.osrs;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Dev/CI helper for tools/smoke_server.sh: prints absolute positions of station blocks (bank booths,
 * furnaces, altars...) that the hand-built town structures are guaranteed to leave in place, so the
 * dedicated-server smoke test can check them with {@code execute if block}.
 *
 * <p>A position is only reported when replaying every structure operation that touches it (in the
 * order {@code StructurePlacer} applies them) deterministically ends on the station block, and when
 * it is clear of anything else that writes blocks near a place (plazas, boss arenas, lairs, the
 * missing-station slots of replaced towns). Terrain-dependent operations (the levelled ground and its
 * blend ring, fill-if-air over unknown terrain) make a position "unknown" and it is skipped.
 *
 * <p>Output lines: {@code STATION <town> <x> <y> <z> <block> <structure>} and, per town, one
 * {@code SPAWN <town> <x> <y> <z>} open spot on levelled ground for summoning creatures.
 */
public final class SmokeProbe {
    /** Station blocks worth checking. */
    static final List<String> STATION_BLOCKS = List.of("runecraft:bank_booth", "runecraft:furnace",
            "runecraft:prayer_altar", "runecraft:anvil", "runecraft:cooking_range");
    private static final Set<String> TALL_PLANTS = Set.of("tall_grass", "large_fern", "sunflower", "lilac",
            "rose_bush", "peony", "tall_seagrass", "pitcher_plant", "small_dripleaf");
    private static final String OTHER = "?other";

    public record Probe(String town, int x, int y, int z, String block, String structure) {
        public String line() {
            return "STATION " + town + " " + x + " " + y + " " + z + " " + block + " " + structure;
        }
    }

    private SmokeProbe() {
    }

    public static void main(String[] args) {
        List<String> towns = args.length > 0 ? List.of(args) : List.of("LUMBRIDGE", "VARROCK", "FALADOR");
        for (String town : towns) {
            for (Probe p : probes(town, 2)) {
                System.out.println(p.line());
            }
            int[] spot = spawnSpot(town);
            if (spot != null) {
                System.out.println("SPAWN " + town.toLowerCase(Locale.ROOT) + " " + spot[0] + " " + spot[1] + " " + spot[2]);
            }
        }
    }

    /** Base block id ("minecraft:" added when no namespace, block-state properties stripped). */
    static String base(String block) {
        if (block == null) {
            return "minecraft:air";
        }
        int b = block.indexOf('[');
        String id = (b < 0 ? block : block.substring(0, b)).trim();
        return id.contains(":") ? id : "minecraft:" + id;
    }

    static boolean multipart(String block) {
        if (block == null) {
            return false;
        }
        String id = base(block);
        String path = id.substring(id.indexOf(':') + 1);
        return (path.endsWith("_door") && !path.endsWith("trapdoor")) || path.endsWith("_bed")
                || TALL_PLANTS.contains(path) || block.contains("half=upper") || block.contains("half=lower");
    }

    /** Structures belonging to a town: the one replacing its landmark plus any sharing its id prefix. */
    static List<StructureDef> townStructures(String town) {
        List<StructureDef> out = new ArrayList<>();
        String prefix = town.toLowerCase(Locale.ROOT);
        for (StructureDef d : ContentRegistry.structures()) {
            if (town.equals(d.replacesLandmark()) || d.id().equals(prefix) || d.id().startsWith(prefix + "_")) {
                out.add(d);
            }
        }
        return out;
    }

    /**
     * Replays all structure operations touching (x, y, z) in placement order. Returns the base block id
     * left there, {@link #OTHER} for a non-station block of unknown kind, or null when it depends on the
     * natural terrain.
     */
    static String finalBlock(int x, int y, int z) {
        String cur = null;
        for (StructureDef d : ContentRegistry.structures()) {
            if (x < d.minX() - 1 || x > d.maxX() + 1 || z < d.minZ() - 1 || z > d.maxZ() + 1) {
                continue;
            }
            for (StructureDef.Op op : d.ops()) {
                cur = apply(op, x, y, z, cur);
            }
        }
        return cur;
    }

    private static String apply(StructureDef.Op op, int x, int y, int z, String cur) {
        int m = op.margin();
        boolean inXz = x >= op.x1() && x <= op.x2() && z >= op.z1() && z <= op.z2();
        if (op.type() == StructureDef.Op.Type.FLATTEN) {
            boolean inRing = x >= op.x1() - m && x <= op.x2() + m && z >= op.z1() - m && z <= op.z2() + m;
            if (!inRing) {
                return cur;
            }
            if (inXz) {
                int target = op.y1() - 1;
                if (y == target) {
                    return base(op.block());
                }
                if (y > target && y <= op.y2()) {
                    return "minecraft:air";
                }
                if (y > op.y2()) {
                    return cur;
                }
            }
            return null; // fill below the surface / blend ring: depends on terrain
        }
        boolean inBox = inXz && y >= op.y1() && y <= op.y2();
        if (multipart(op.block())) {
            boolean near = x >= op.x1() - 1 && x <= op.x2() + 1 && y >= op.y1() - 1 && y <= op.y2() + 1
                    && z >= op.z1() - 1 && z <= op.z2() + 1;
            return near ? OTHER : cur;
        }
        if (!inBox) {
            return cur;
        }
        switch (op.type()) {
            case FILL:
                return base(op.block());
            case CLEAR:
                return "minecraft:air";
            case WALLS:
                return x == op.x1() || x == op.x2() || z == op.z1() || z == op.z2() ? base(op.block()) : cur;
            case FILL_AIR:
                if (cur == null) {
                    return null;
                }
                return cur.equals("minecraft:air") ? base(op.block()) : cur;
            case REPLACE:
                if (cur == null) {
                    return null;
                }
                return cur.equals(base(op.extra())) ? base(op.block()) : cur;
            default:
                return null;
        }
    }

    /** True when blocks written by the landmark builder (plazas, arenas, lairs...) could reach (x, z). */
    static boolean nearOtherBuilders(int x, int z) {
        Set<String> replaced = new HashSet<>();
        for (StructureDef d : ContentRegistry.structures()) {
            if (d.replacesLandmark() != null) {
                replaced.add(d.replacesLandmark());
            }
        }
        // centre x, centre z, reach of the default build when the place is not replaced by a structure
        Map<String, int[]> centres = new LinkedHashMap<>();
        for (Landmark l : Landmark.values()) {
            // plaza (radius 6/8), lairs south of it and the agility course east of it
            centres.put(l.name(), new int[]{l.blockX(), l.blockZ(), 32});
        }
        for (PlaceDef p : ContentRegistry.places()) {
            int n = p.stations().size() + p.blocks().size();
            int reach = p.style() == PlaceDef.Style.NONE ? -1 : Math.max(5, 3 + n / 2 + 2) + 2;
            centres.put(p.id(), new int[]{p.blockX(), p.blockZ(), reach});
        }
        for (Map.Entry<String, int[]> e : centres.entrySet()) {
            int cx = e.getValue()[0];
            int cz = e.getValue()[1];
            int d = Math.max(Math.abs(x - cx), Math.abs(z - cz));
            if (replaced.contains(e.getKey())) {
                // missing-station slots (radius 4) and boss lairs (cx-7..cx+1, cz+11..cz+13)
                if (d <= 6 || (x >= cx - 9 && x <= cx + 3 && z >= cz + 9 && z <= cz + 15)) {
                    return true;
                }
            } else if (d <= e.getValue()[2]) {
                return true;
            }
            int arena = 0;
            for (BossDef b : ContentRegistry.bosses()) {
                if (b.raidOnly || !e.getKey().equals(b.place)) {
                    continue;
                }
                int r = b.arenaRadius;
                int ax = cx + (arena % 2 == 0 ? 1 : -1) * (14 + r);
                int az = cz + (arena / 2) * (2 * r + 6);
                if (Math.max(Math.abs(x - ax), Math.abs(z - az)) <= r + 4) {
                    return true;
                }
                arena++;
            }
        }
        return false;
    }

    /**
     * An open spot on a town's levelled ground (known solid floor, 8 blocks of known air above a 5x5
     * area, away from other builders) to summon creatures at, or null.
     */
    public static int[] spawnSpot(String town) {
        int tried = 0;
        for (StructureDef d : townStructures(town)) {
            for (StructureDef.Op op : d.ops()) {
                if (op.type() != StructureDef.Op.Type.FLATTEN) {
                    continue;
                }
                int y = op.y1();
                for (int x = op.x1() + 2; x <= op.x2() - 2; x += 3) {
                    for (int z = op.z1() + 2; z <= op.z2() - 2; z += 3) {
                        if (++tried > 4000) {
                            return null;
                        }
                        if (open(x, y, z) && !nearOtherBuilders(x, z)) {
                            return new int[]{x, y, z};
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean open(int x, int y, int z) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                String floor = finalBlock(x + dx, y - 1, z + dz);
                if (floor == null || floor.equals("minecraft:air") || floor.equals(OTHER)) {
                    return false;
                }
                for (int dy = 0; dy < 8; dy++) {
                    if (!"minecraft:air".equals(finalBlock(x + dx, y + dy, z + dz))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** Up to {@code perBlock} verified positions of each station block in a town's structures. */
    public static List<Probe> probes(String town, int perBlock) {
        List<Probe> out = new ArrayList<>();
        Map<String, Integer> found = new LinkedHashMap<>();
        Set<Long> seen = new HashSet<>();
        for (StructureDef d : townStructures(town)) {
            for (StructureDef.Op op : d.ops()) {
                if (op.type() != StructureDef.Op.Type.FILL && op.type() != StructureDef.Op.Type.WALLS) {
                    continue;
                }
                String block = base(op.block());
                if (!STATION_BLOCKS.contains(block) || found.getOrDefault(block, 0) >= perBlock) {
                    continue;
                }
                long volume = (long) (op.x2() - op.x1() + 1) * (op.y2() - op.y1() + 1) * (op.z2() - op.z1() + 1);
                if (volume > 64) {
                    continue;
                }
                search:
                for (int x = op.x1(); x <= op.x2(); x++) {
                    for (int y = op.y1(); y <= op.y2(); y++) {
                        for (int z = op.z1(); z <= op.z2(); z++) {
                            long key = ((long) x << 40) ^ ((long) (y & 0xFFF) << 28) ^ (z & 0xFFFFFFFL);
                            if (!seen.add(key) || !block.equals(finalBlock(x, y, z)) || nearOtherBuilders(x, z)) {
                                continue;
                            }
                            out.add(new Probe(town.toLowerCase(Locale.ROOT), x, y, z, block, d.id()));
                            found.merge(block, 1, Integer::sum);
                            break search;
                        }
                    }
                }
            }
        }
        return out;
    }
}
