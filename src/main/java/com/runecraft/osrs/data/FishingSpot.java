package com.runecraft.osrs.data;

import java.util.List;
import java.util.Map;

/** Fishing spot types and what each method catches there (highest level fish is tried first). */
public enum FishingSpot {
    SEA_NET_BAIT("net_bait_fishing_spot", "Fishing spot (Net/Bait)", Map.of(
            FishingMethod.NET, List.of(Fish.ANCHOVIES, Fish.SHRIMPS),
            FishingMethod.BAIT, List.of(Fish.HERRING, Fish.SARDINE))),
    RIVER_LURE_BAIT("lure_bait_fishing_spot", "Rod fishing spot (Lure/Bait)", Map.of(
            FishingMethod.LURE, List.of(Fish.SALMON, Fish.TROUT),
            FishingMethod.BAIT, List.of(Fish.PIKE))),
    CAGE_HARPOON("cage_harpoon_fishing_spot", "Fishing spot (Cage/Harpoon)", Map.of(
            FishingMethod.CAGE, List.of(Fish.LOBSTER),
            FishingMethod.HARPOON, List.of(Fish.SWORDFISH, Fish.TUNA))),
    NET_HARPOON("net_harpoon_fishing_spot", "Fishing spot (Big Net/Harpoon)", Map.of(
            FishingMethod.NET, List.of(Fish.MONKFISH),
            FishingMethod.HARPOON, List.of(Fish.SHARK))),
    ANGLER("angler_fishing_spot", "Rod fishing spot (Anglerfish)", Map.of(
            FishingMethod.BAIT, List.of(Fish.ANGLERFISH))),
    DARK_CRAB("dark_crab_fishing_spot", "Fishing spot (Dark crabs)", Map.of(
            FishingMethod.DARK_BAIT, List.of(Fish.DARK_CRAB)));

    private final String blockId;
    private final String displayName;
    private final Map<FishingMethod, List<Fish>> catches;

    FishingSpot(String blockId, String displayName, Map<FishingMethod, List<Fish>> catches) {
        this.blockId = blockId;
        this.displayName = displayName;
        this.catches = catches;
    }

    public String blockId() {
        return blockId;
    }

    public String displayName() {
        return displayName;
    }

    public Map<FishingMethod, List<Fish>> catches() {
        return catches;
    }
}
