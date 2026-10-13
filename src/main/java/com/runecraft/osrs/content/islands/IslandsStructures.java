package com.runecraft.osrs.content.islands;

import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentModule;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.Modules;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.runecraft.osrs.content.islands.IslandsKit.caveCentreX;
import static com.runecraft.osrs.content.islands.IslandsKit.caveCentreZ;
import static com.runecraft.osrs.content.islands.IslandsKit.centreX;
import static com.runecraft.osrs.content.islands.IslandsKit.centreZ;
import static com.runecraft.osrs.content.islands.IslandsKit.dx;
import static com.runecraft.osrs.content.islands.IslandsKit.dz;

/**
 * Hand-built islands at their real tile coordinates: Fossil Island (Museum Camp, House on the Hill,
 * Volcanic Mine, Mushroom forest, Verdant Valley, Mushroom Meadow), Ape Atoll (Marim's walls and gates,
 * King Awowogei's palace, the market, the temple and the agility course), Miscellania and Etceteria,
 * Lunar Isle's Moonclan village and mine, Neitiznot and Jatizso, Waterbirth Island, Pirates' Cove,
 * Ungael, Lithkren, Mos Le'Harmless, Harmony Island, Dragontooth Island, Tutorial Island, Mudskipper
 * Point, Crandor, Kharazi Jungle, Cairn Isle, Feldip Hills (Gu'Tanoth, Castle Wars with its two
 * castles, Corsair Cove and the Hunter area), Zul-Andra, Port Tyras, Isafdar, Braindeath Island, the
 * Void Knights' Outpost and Zanaris.
 *
 * <p>Places owned by other modules are replaced (each by exactly one structure) rather than
 * duplicated. Every other module's plaza, boss arena and structure is left alone: no flattening
 * (including its blend ring) and no building reaches into them. The layouts are drawn around them;
 * as a safety net for places added later, {@link #all()} also clips anything that would.
 */
public final class IslandsStructures {
    private IslandsStructures() {
    }

    // ---- travel blocks: offsets (blocks) of each ring / mushtree from its place centre ----
    static final int[] ZANARIS_RING = {2412, 4428};
    static final int[] MUDSKIPPER_RING = {2996, 3114};
    static final int[] MISCELLANIA_RING = {2513, 3884};
    static final int[] APE_RING = {2735, 2742};
    static final int[] FELDIP_RING = {2571, 2956};
    static final int[] CASTLE_WARS_RING = {2385, 3035};
    static final int[] ZUL_ANDRA_RING = {2213, 3099};
    // Magic Mushtrees on their OSRS tiles (skretzo/shortest-path magic_mushtrees.tsv).
    static final int[] HOUSE_TREE = {3764, 3879};
    static final int[] VALLEY_TREE = {3760, 3758};
    static final int[] FOREST_TREE = {3676, 3755};
    static final int[] MEADOW_TREE = {3676, 3870}; // OSRS 3676, 3871: a tile south, clear of the bird house camp

    public static final int ZANARIS_RING_DX = dx(ZANARIS_RING[0], 2412);
    public static final int ZANARIS_RING_DZ = dz(ZANARIS_RING[1], 4434);
    public static final int MUDSKIPPER_RING_DX = dx(MUDSKIPPER_RING[0], 2995);
    public static final int MUDSKIPPER_RING_DZ = dz(MUDSKIPPER_RING[1], 3112);
    public static final int MISCELLANIA_RING_DX = dx(MISCELLANIA_RING[0], 2500);
    public static final int MISCELLANIA_RING_DZ = dz(MISCELLANIA_RING[1], 3860);
    public static final int APE_RING_DX = dx(APE_RING[0], 2758);
    public static final int APE_RING_DZ = dz(APE_RING[1], 2744);
    public static final int FELDIP_RING_DX = dx(FELDIP_RING[0], 2550);
    public static final int FELDIP_RING_DZ = dz(FELDIP_RING[1], 2910);
    public static final int CASTLE_WARS_RING_DX = dx(CASTLE_WARS_RING[0], 2440);
    public static final int CASTLE_WARS_RING_DZ = dz(CASTLE_WARS_RING[1], 3090);
    public static final int ZUL_ANDRA_RING_DX = dx(ZUL_ANDRA_RING[0], 2200);
    public static final int ZUL_ANDRA_RING_DZ = dz(ZUL_ANDRA_RING[1], 3056);
    public static final int HOUSE_TREE_DX = dx(HOUSE_TREE[0], 3764);
    public static final int HOUSE_TREE_DZ = dz(HOUSE_TREE[1], 3869);
    public static final int VALLEY_TREE_DX = dx(VALLEY_TREE[0], 3764);
    public static final int VALLEY_TREE_DZ = dz(VALLEY_TREE[1], 3756);
    public static final int FOREST_TREE_DX = dx(FOREST_TREE[0], 3695);
    public static final int FOREST_TREE_DZ = dz(FOREST_TREE[1], 3792);
    public static final int MEADOW_TREE_DX = dx(MEADOW_TREE[0], 3672);
    public static final int MEADOW_TREE_DZ = dz(MEADOW_TREE[1], 3865);

    // ---- caves: the Corsair Cove Dungeon is carved here; the others belong to the dungeons module ----
    static final int[] CORSAIR_DUNGEON_ENTRANCE = {2523, 2861};
    static final int CORSAIR_DUNGEON_HALF = 14;

    /**
     * The dungeons module carves the Wyvern Cave (entrance tile 3677, 3854) with its spitting and taloned
     * wyverns; this module's long-tailed and ancient wyverns live in its great southern cavern (blocks
     * +20..+64 east, -30..+24 south of the entrance, floor y = 14).
     */
    public static final int WYVERN_CAVE_FLOOR_Y = 15;
    public static final int WYVERN_CAMP_DX = StructureDef.bx(3677) + 42 - centreX(3672);
    public static final int WYVERN_CAMP_DZ = StructureDef.bz(3854) - 3 - centreZ(3865);
    public static final int CORSAIR_DUNGEON_DX = caveCentreX(CORSAIR_DUNGEON_ENTRANCE[0]) - centreX(2570);
    public static final int CORSAIR_DUNGEON_DZ = caveCentreZ(CORSAIR_DUNGEON_ENTRANCE[1], CORSAIR_DUNGEON_HALF) - centreZ(2862);

    /** Every structure as drawn, before the safety-net clipping. Listed in build order. */
    public static List<StructureDef> raw() {
        List<StructureDef> out = new ArrayList<>();
        out.addAll(IslandsFossil.all());
        out.addAll(IslandsApeAtoll.all());
        out.addAll(IslandsNorth.all());
        out.addAll(IslandsEast.all());
        out.addAll(IslandsSouth.all());
        out.addAll(IslandsWest.all());
        return out;
    }

    private static List<StructureDef> cached;
    private static final ThreadLocal<Boolean> BUILDING = ThreadLocal.withInitial(() -> false);

    /** The module's structures, clipped clear of every other module's plazas, arenas and structures. */
    public static List<StructureDef> all() {
        synchronized (IslandsStructures.class) {
            if (cached != null) {
                return cached;
            }
            if (BUILDING.get()) {
                return raw(); // re-entered from another module's structures(): don't recurse
            }
            BUILDING.set(true);
            try {
                List<StructureDef> raw = raw();
                List<IslandsKit.Zone> zones = foreignZones(raw);
                List<StructureDef> out = new ArrayList<>();
                for (StructureDef s : raw) {
                    StructureDef c = IslandsKit.clip(s, zones);
                    if (c != null) {
                        out.add(c);
                    }
                }
                cached = Collections.unmodifiableList(out);
                return cached;
            } finally {
                BUILDING.set(false);
            }
        }
    }

    /**
     * Zones this module must never build in: the default plaza of every place and landmark it does not
     * replace, every boss arena, and the footprint of every other module's structures.
     */
    static List<IslandsKit.Zone> foreignZones(List<StructureDef> mine) {
        Set<String> replaced = new HashSet<>();
        for (StructureDef s : mine) {
            if (s.replacesLandmark() != null) {
                replaced.add(s.replacesLandmark());
            }
        }
        List<IslandsKit.Zone> zones = new ArrayList<>();
        for (PlaceDef p : ContentRegistry.places()) {
            if (!replaced.contains(p.id()) && p.style() != PlaceDef.Style.NONE) {
                int r = Math.max(5, 3 + (p.stations().size() + p.blocks().size()) / 2 + 2) + 2;
                zones.add(square(p.id(), p.blockX(), p.blockZ(), r));
            }
        }
        for (Landmark l : Landmark.values()) {
            if (!replaced.contains(l.name())) {
                zones.add(square(l.name(), l.blockX(), l.blockZ(), l.isAltar() ? 5 : 10));
            }
        }
        Map<String, Integer> arenaIndex = new HashMap<>();
        for (BossDef b : ContentRegistry.bosses()) {
            if (b.raidOnly || b.place == null) {
                continue;
            }
            int cx;
            int cz;
            PlaceDef p = ContentRegistry.place(b.place);
            if (p != null) {
                cx = p.blockX();
                cz = p.blockZ();
            } else {
                Landmark l = Landmark.byName(b.place);
                if (l == null) {
                    continue;
                }
                cx = l.blockX();
                cz = l.blockZ();
            }
            int i = arenaIndex.merge(b.place, 1, Integer::sum) - 1;
            int r = b.arenaRadius;
            zones.add(square(b.id + " arena", cx + (i % 2 == 0 ? 1 : -1) * (14 + r), cz + (i / 2) * (2 * r + 6), r + 2));
        }
        for (ContentModule m : Modules.ALL) {
            if (m instanceof IslandsContent) {
                continue;
            }
            for (StructureDef s : m.structures()) {
                IslandsKit.Zone z = surface(s);
                if (z != null) {
                    zones.add(z);
                }
            }
        }
        return zones;
    }

    /**
     * The footprint of a structure's surface parts (flattening with its blend ring, and everything that
     * reaches ground level), or null if it is entirely underground (a dungeon's caves).
     */
    static IslandsKit.Zone surface(StructureDef s) {
        int x1 = Integer.MAX_VALUE;
        int z1 = Integer.MAX_VALUE;
        int x2 = Integer.MIN_VALUE;
        int z2 = Integer.MIN_VALUE;
        for (StructureDef.Op op : s.ops()) {
            if (op.type() != StructureDef.Op.Type.FLATTEN && op.y2() < StructureDef.GROUND - 1) {
                continue;
            }
            int m = op.margin();
            x1 = Math.min(x1, op.x1() - m);
            z1 = Math.min(z1, op.z1() - m);
            x2 = Math.max(x2, op.x2() + m);
            z2 = Math.max(z2, op.z2() + m);
        }
        return x1 > x2 ? null : new IslandsKit.Zone(s.id() + " structure", x1, z1, x2, z2);
    }

    private static IslandsKit.Zone square(String owner, int cx, int cz, int r) {
        return new IslandsKit.Zone(owner, cx - r, cz - r, cx + r, cz + r);
    }
}
