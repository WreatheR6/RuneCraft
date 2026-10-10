package com.runecraft.osrs.content.islands;

import java.util.List;

/**
 * Block-based travel networks. Sneak-clicking a network block turns its dial to the next
 * destination; clicking travels to the chosen one. Each stop is a place (or Landmark) plus the block offset of the
 * network block from the place centre, where travellers arrive.
 *
 * <ul>
 *   <li>Fairy rings: the hub in Zanaris and the rings built by this module (codes as in OSRS), plus
 *   CKP, which leads to the plane of the Cosmic Altar (the existing landmark). Using them needs
 *   Fairytale II - Cure a Queen to have been started.</li>
 *   <li>The Mycelium transportation system: Fossil Island's four Magic Mushtrees.</li>
 * </ul>
 */
public final class IslandsTravel {
    private IslandsTravel() {
    }

    /** A destination: {@code place} is a PlaceDef id or Landmark name; dx/dz the block offset of the ring/tree. */
    public record Stop(String code, String name, String place, int dx, int dz, boolean hasBlock) {
    }

    public record Network(String id, String blockId, String quest, List<Stop> stops) {
        public Stop stop(String code) {
            for (Stop s : stops) {
                if (s.code().equals(code)) {
                    return s;
                }
            }
            return null;
        }

        public int indexOf(String code) {
            for (int i = 0; i < stops.size(); i++) {
                if (stops.get(i).code().equals(code)) {
                    return i;
                }
            }
            return -1;
        }
    }

    public static final Network FAIRY_RINGS = new Network("fairy_rings", IslandsBlocks.FAIRY_RING, "fairytale_ii_cure_a_queen", List.of(
            new Stop("ZANARIS", "Zanaris (fairy ring centre)", IslandsPlaces.ZANARIS, IslandsStructures.ZANARIS_RING_DX,
                    IslandsStructures.ZANARIS_RING_DZ, true),
            new Stop("AIQ", "Asgarnia: Mudskipper Point", IslandsPlaces.MUDSKIPPER_POINT, IslandsStructures.MUDSKIPPER_RING_DX,
                    IslandsStructures.MUDSKIPPER_RING_DZ, true),
            new Stop("CIP", "Islands: Miscellania", IslandsPlaces.MISCELLANIA, IslandsStructures.MISCELLANIA_RING_DX,
                    IslandsStructures.MISCELLANIA_RING_DZ, true),
            new Stop("CLR", "Islands: Ape Atoll", IslandsPlaces.APE_ATOLL_AGILITY, IslandsStructures.APE_RING_DX,
                    IslandsStructures.APE_RING_DZ, true),
            new Stop("AKS", "Feldip Hills: Feldip Hunter area", IslandsPlaces.FELDIP_HUNTER_AREA, IslandsStructures.FELDIP_RING_DX,
                    IslandsStructures.FELDIP_RING_DZ, true),
            new Stop("BKP", "Feldip Hills: south of Castle Wars", IslandsPlaces.CASTLE_WARS, IslandsStructures.CASTLE_WARS_RING_DX,
                    IslandsStructures.CASTLE_WARS_RING_DZ, true),
            new Stop("DLR", "Islands: Poison Waste, south of Isafdar", IslandsPlaces.ZUL_ANDRA, IslandsStructures.ZUL_ANDRA_RING_DX,
                    IslandsStructures.ZUL_ANDRA_RING_DZ, true),
            new Stop("CKP", "Other realms: Cosmic entity's plane", "COSMIC_ALTAR", 3, 3, false)));

    public static final Network MUSHTREES = new Network("mycelium", IslandsBlocks.MUSHTREE, null, List.of(
            new Stop("HOUSE_ON_THE_HILL", "House on the Hill", IslandsPlaces.HOUSE_ON_THE_HILL, IslandsStructures.HOUSE_TREE_DX,
                    IslandsStructures.HOUSE_TREE_DZ, true),
            new Stop("VERDANT_VALLEY", "Verdant Valley", IslandsPlaces.VERDANT_VALLEY, IslandsStructures.VALLEY_TREE_DX,
                    IslandsStructures.VALLEY_TREE_DZ, true),
            new Stop("STICKY_SWAMP", "Sticky swamp", IslandsPlaces.MUSHROOM_FOREST, IslandsStructures.FOREST_TREE_DX,
                    IslandsStructures.FOREST_TREE_DZ, true),
            new Stop("MUSHROOM_MEADOW", "Mushroom Meadow", IslandsPlaces.WYVERN_CAVE, IslandsStructures.MEADOW_TREE_DX,
                    IslandsStructures.MEADOW_TREE_DZ, true)));

    public static List<Network> networks() {
        return List.of(FAIRY_RINGS, MUSHTREES);
    }

    public static Network byBlock(String blockId) {
        for (Network n : networks()) {
            if (n.blockId().equals(blockId)) {
                return n;
            }
        }
        return null;
    }

    /** The destination after {@code code} on the dial (skipping {@code here}). */
    public static Stop next(Network n, String code, String here) {
        int i = Math.max(-1, n.indexOf(code));
        for (int k = 1; k <= n.stops().size(); k++) {
            Stop s = n.stops().get((i + k) % n.stops().size());
            if (!s.code().equals(here)) {
                return s;
            }
        }
        return n.stops().get(0);
    }
}
