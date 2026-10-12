package com.runecraft.osrs;

import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.world.ArenaLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-way combat areas. Everywhere in Gielinor is single-way combat (one attacker per target) except the
 * areas listed here, which follow the OSRS multicombat map: the God Wars Dungeon, the Wilderness multi zones,
 * the Revenant Caves, the Catacombs of Kourend, the busy slayer dungeons and group boss lairs. Raids and
 * minigame instances are always multi; the Minecraft side checks those dynamically.
 *
 * <p>Areas are either OSRS tile boxes (real world-map coordinates) or a radius around a place (a module
 * {@link PlaceDef} id or a {@link Landmark} name), because many of this world's dungeons are laid out on the
 * surface around their entrance instead of in the real underground map squares.
 */
public final class MultiCombat {
    /** An inclusive OSRS tile box. */
    public record Box(String name, int x1, int y1, int x2, int y2) {
        public boolean contains(int x, int y) {
            return x >= Math.min(x1, x2) && x <= Math.max(x1, x2) && y >= Math.min(y1, y2) && y <= Math.max(y1, y2);
        }
    }

    /** A square of {@code radius} tiles around a place (module place id or Landmark name). */
    public record Around(String name, String place, int radius) {
    }

    /** OSRS world-map multicombat boxes. */
    public static final List<Box> BOXES = List.of(
            // ---- Wilderness (approx: traced from the OSRS multicombat map)
            new Box("Wilderness: north of Edgeville", 3134, 3525, 3148, 3535), // approx
            new Box("Wilderness: Dark Warriors' Fortress", 3008, 3600, 3071, 3711), // approx
            new Box("Wilderness: Chaos Temple (level 38)", 2946, 3816, 2959, 3831), // approx
            new Box("Wilderness: Lava Maze", 3008, 3856, 3047, 3903), // approx
            new Box("Wilderness: Frozen Waste Plateau", 2944, 3880, 3007, 3967), // approx
            new Box("Wilderness: eastern multi (Callisto, Vet'ion, Venenatis, Scorpia, Chaos Elemental)",
                    3200, 3672, 3392, 3967), // approx
            // ---- Elsewhere
            new Box("God Wars Dungeon", 2830, 3695, 2960, 3795), // the chambers around the GWD landmark (2917, 3745)
            new Box("Ape Atoll", 2690, 2690, 2815, 2815), // approx
            new Box("Death Plateau and Troll Country", 2835, 3580, 2945, 3640)); // approx

    /** Multicombat dungeons and lairs, by the place they are built around. */
    public static final List<Around> AROUND = List.of(
            new Around("Revenant Caves", "revenant_caves", 25),
            new Around("Scorpia's cave", "scorpias_cave", 15),
            new Around("Catacombs of Kourend", "catacombs_of_kourend", 20),
            new Around("Smoke Devil Dungeon", "smoke_devil_dungeon", 20),
            new Around("Kalphite Lair", "kalphite_lair", 20),
            new Around("Kraken Cove", "kraken_cove", 15),
            new Around("Abyssal Nexus", "abyssal_nexus", 15),
            new Around("Corporeal Beast's lair", "corporeal_beast_lair", 15),
            new Around("Waterbirth Island Dungeon", "waterbirth_island", 15),
            new Around("Grotesque Guardians' rooftop", "slayer_tower_roof", 10),
            new Around("Zalcano's prison", "zalcano_chamber", 20),
            new Around("The Hueycoatl's lair", "hueycoatl_lair", 20),
            new Around("Royal Titans' lair", "royal_titans_lair", 15),
            new Around("Lizardman Settlement", "zeah_lizardman_settlement", 20), // approx
            new Around("Lizardman Canyon", "lizardman_canyon", 15),
            new Around("Bandit Camp (desert)", "bandit_camp", 15)); // approx

    /** Bosses fought in multicombat lairs: their arenas ({@link ArenaLayout}) are multi-way. */
    public static final List<String> MULTI_BOSS_ARENAS = List.of("kreearra", "commander_zilyana", "nex", "callisto",
            "venenatis", "vetion", "scorpia", "chaos_elemental", "corporeal_beast", "kalphite_queen", "giant_mole",
            "sarachnis", "thermonuclear_smoke_devil", "abyssal_sire", "dusk", "zalcano", "the_hueycoatl",
            "branda_the_fire_queen", "scurrius"); // the Royal Titans share Branda's arena

    private static volatile List<Box> resolved;

    private MultiCombat() {
    }

    /** Every multi area as a tile box (place areas resolved to their coordinates; unknown places skipped). */
    public static List<Box> areas() {
        List<Box> r = resolved;
        if (r == null) {
            List<Box> all = new ArrayList<>(BOXES);
            for (Around a : AROUND) {
                int[] c = placeCentre(a.place());
                if (c != null) {
                    all.add(new Box(a.name(), c[0] - a.radius(), c[1] - a.radius(), c[0] + a.radius(), c[1] + a.radius()));
                }
            }
            for (String boss : MULTI_BOSS_ARENAS) {
                ArenaLayout.Arena arena = ArenaLayout.of(boss);
                if (arena != null) {
                    int e = arena.radius() + 4;
                    all.add(new Box(boss + " arena", GielinorMap.toOsrsX(arena.x() - e), GielinorMap.toOsrsY(arena.z() - e),
                            GielinorMap.toOsrsX(arena.x() + e), GielinorMap.toOsrsY(arena.z() + e)));
                }
            }
            r = List.copyOf(all);
            resolved = r;
        }
        return r;
    }

    /** OSRS coordinates of a module place or Landmark, or null if unknown. */
    public static int[] placeCentre(String place) {
        PlaceDef p = ContentRegistry.place(place);
        if (p != null) {
            return new int[]{p.osrsX(), p.osrsY()};
        }
        Landmark l = Landmark.byName(place);
        return l == null ? null : new int[]{l.osrsX(), l.osrsY()};
    }

    /** True if the OSRS tile is in a multicombat area. */
    public static boolean isMulti(int osrsX, int osrsY) {
        return area(osrsX, osrsY) != null;
    }

    /** The multicombat area containing the tile, or null in single-way combat. */
    public static Box area(int osrsX, int osrsY) {
        for (Box b : areas()) {
            if (b.contains(osrsX, osrsY)) {
                return b;
            }
        }
        return null;
    }

    /** Multicombat check at a Minecraft block position. */
    public static boolean isMultiAtBlock(int blockX, int blockZ) {
        return isMulti(GielinorMap.toOsrsX(blockX), GielinorMap.toOsrsY(blockZ));
    }
}
