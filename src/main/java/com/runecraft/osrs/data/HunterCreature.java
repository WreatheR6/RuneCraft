package com.runecraft.osrs.data;

import com.runecraft.osrs.Region;

import java.util.List;
import java.util.Set;

/** Hunter creatures caught with bird snares or box traps, and where they live. */
public enum HunterCreature {
    CRIMSON_SWIFT("Crimson swift", Trap.BIRD_SNARE, 1, 34.0, Set.of(Region.FELDIP_HILLS, Region.KARAMJA, Region.MISTHALIN, Region.LUMBRIDGE_SWAMP, Region.UNCHARTED_ISLES),
            List.of("bones", "raw_bird_meat", "minecraft:feather")),
    GOLDEN_WARBLER("Golden warbler", Trap.BIRD_SNARE, 5, 47.0, Set.of(Region.KHARIDIAN_DESERT),
            List.of("bones", "raw_bird_meat", "minecraft:feather")),
    COPPER_LONGTAIL("Copper longtail", Trap.BIRD_SNARE, 9, 61.0, Set.of(Region.KANDARIN, Region.ASGARNIA),
            List.of("bones", "raw_bird_meat", "minecraft:feather")),
    CERULEAN_TWITCH("Cerulean twitch", Trap.BIRD_SNARE, 11, 64.7, Set.of(Region.FREMENNIK_PROVINCE, Region.TROLL_COUNTRY),
            List.of("bones", "raw_bird_meat", "minecraft:feather")),
    TROPICAL_WAGTAIL("Tropical wagtail", Trap.BIRD_SNARE, 19, 95.2, Set.of(Region.FELDIP_HILLS, Region.KARAMJA),
            List.of("bones", "raw_bird_meat", "minecraft:feather")),
    FERRET("Ferret", Trap.BOX_TRAP, 27, 115.0, Set.of(Region.KANDARIN, Region.FELDIP_HILLS, Region.ASGARNIA, Region.MISTHALIN),
            List.of("ferret")),
    GREY_CHINCHOMPA("Grey chinchompa", Trap.BOX_TRAP, 53, 198.4, Set.of(Region.KANDARIN, Region.TIRANNWN),
            List.of("grey_chinchompa")),
    RED_CHINCHOMPA("Red chinchompa", Trap.BOX_TRAP, 63, 265.0, Set.of(Region.FELDIP_HILLS, Region.TIRANNWN),
            List.of("red_chinchompa")),
    BLACK_CHINCHOMPA("Black chinchompa", Trap.BOX_TRAP, 73, 315.0, Set.of(Region.WILDERNESS),
            List.of("black_chinchompa"));

    public enum Trap { BIRD_SNARE, BOX_TRAP }

    private final String name;
    private final Trap trap;
    private final int level;
    private final double xp;
    private final Set<Region> regions;
    private final List<String> loot;

    HunterCreature(String name, Trap trap, int level, double xp, Set<Region> regions, List<String> loot) {
        this.name = name;
        this.trap = trap;
        this.level = level;
        this.xp = xp;
        this.regions = regions;
        this.loot = loot;
    }

    public String displayName() {
        return name;
    }

    public Trap trap() {
        return trap;
    }

    public int level() {
        return level;
    }

    public double xp() {
        return xp;
    }

    public Set<Region> regions() {
        return regions;
    }

    public List<String> loot() {
        return loot;
    }

    /** Best creature this trap can catch for a hunter level in a region, or null. */
    public static HunterCreature best(Trap trap, Region region, int level) {
        HunterCreature best = null;
        for (HunterCreature c : values()) {
            if (c.trap == trap && c.level <= level && c.regions.contains(region)) {
                if (best == null || c.level > best.level) {
                    best = c;
                }
            }
        }
        return best;
    }
}
