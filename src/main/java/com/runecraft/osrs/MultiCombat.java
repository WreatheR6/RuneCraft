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

    /**
     * OSRS world-map multicombat boxes. The surface areas outside Zeah are the plane-0 polygons of RuneLite's
     * multi-lines map (MapLocations.java, as kept in SanLiteOSRS/SanLite), cut into tile boxes; areas Jagex changed
     * since (the 2023 Wilderness boss lairs, Kourend's rework) may differ (approx). Zeah's many small zones are not
     * listed; its lairs are in {@link #AROUND}.
     */
    public static final List<Box> BOXES = List.of(
            new Box("Wilderness: main multi area", 3136, 3520, 3327, 3583),
            new Box("Wilderness: main multi area", 3152, 3584, 3327, 3592),
            new Box("Wilderness: main multi area", 3151, 3593, 3327, 3593),
            new Box("Wilderness: main multi area", 3150, 3594, 3327, 3594),
            new Box("Wilderness: main multi area", 3149, 3595, 3327, 3595),
            new Box("Wilderness: main multi area", 3147, 3596, 3327, 3597),
            new Box("Wilderness: main multi area", 3146, 3598, 3327, 3615),
            new Box("Wilderness: main multi area", 3147, 3616, 3327, 3618),
            new Box("Wilderness: main multi area", 3151, 3619, 3327, 3619),
            new Box("Wilderness: main multi area", 3152, 3620, 3161, 3621),
            new Box("Wilderness: main multi area", 3153, 3622, 3161, 3622),
            new Box("Wilderness: main multi area", 3154, 3623, 3161, 3624),
            new Box("Wilderness: main multi area", 3155, 3625, 3161, 3625),
            new Box("Wilderness: main multi area", 3187, 3620, 3327, 3635),
            new Box("Wilderness: main multi area", 3183, 3636, 3327, 3636),
            new Box("Wilderness: main multi area", 3177, 3637, 3327, 3640),
            new Box("Wilderness: main multi area", 3176, 3641, 3327, 3644),
            new Box("Wilderness: main multi area", 3174, 3645, 3327, 3647),
            new Box("Wilderness: main multi area", 3192, 3648, 3327, 3751),
            new Box("Wilderness: main multi area", 3152, 3752, 3327, 3839),
            new Box("Wilderness: main multi area", 3008, 3856, 3047, 3863),
            new Box("Wilderness: main multi area", 3136, 3840, 3391, 3871),
            new Box("Wilderness: main multi area", 3008, 3864, 3055, 3871),
            new Box("Wilderness: main multi area", 3112, 3872, 3391, 3879),
            new Box("Wilderness: main multi area", 3008, 3872, 3047, 3895),
            new Box("Wilderness: main multi area", 3072, 3880, 3391, 3895),
            new Box("Wilderness: main multi area", 3008, 3896, 3391, 3903),
            new Box("Wilderness: main multi area", 3200, 3904, 3391, 3967),
            new Box("South of wilderness agility training arena", 2984, 3912, 3007, 3927),
            new Box("Wilderness Zamorak temple", 2944, 3816, 2959, 3831),
            new Box("Wilderness bandit camp", 3008, 3600, 3071, 3711),
            new Box("Chaos temple north of Falador", 2928, 3512, 2943, 3519),
            new Box("Burthorpe", 2880, 3520, 2903, 3543),
            new Box("White Wolf Mountain", 2816, 3456, 2879, 3519),
            new Box("Death Plateau", 2848, 3600, 2879, 3607),
            new Box("Trollheim/God Wars", 2896, 3688, 2919, 3695),
            new Box("Trollheim/God Wars", 2880, 3696, 2911, 3727),
            new Box("Trollheim/God Wars", 2888, 3728, 2911, 3743),
            new Box("Trollheim/God Wars", 2880, 3744, 2911, 3775),
            new Box("Northern Rellekka", 2656, 3712, 2735, 3727),
            new Box("Northern Rellekka", 2656, 3728, 2703, 3735),
            new Box("Northern Rellekka", 2712, 3728, 2735, 3735),
            new Box("Northern Fremennik Isles", 2352, 3816, 2367, 3823),
            new Box("Northern Fremennik Isles", 2304, 3824, 2367, 3839),
            new Box("Northern Fremennik Isles", 2304, 3840, 2431, 3903),
            new Box("Pirates Cove", 2176, 3776, 2239, 3839),
            new Box("Lunar Isle", 2048, 3840, 2175, 3967),
            new Box("Piscatoris Fishing Colony", 2304, 3648, 2367, 3711),
            new Box("Ranging Guild", 2656, 3408, 2679, 3415),
            new Box("Ranging Guild", 2648, 3416, 2687, 3439),
            new Box("Ranging Guild", 2656, 3440, 2679, 3447),
            new Box("Necromancer house, southeast of Ardougne", 2664, 3216, 2679, 3231),
            new Box("Necromancer house, southeast of Ardougne", 2656, 3232, 2679, 3255),
            new Box("Battlefield north of Tree Gnome Village", 2504, 3208, 2551, 3231),
            new Box("Battlefield north of Tree Gnome Village", 2504, 3232, 2543, 3247),
            new Box("Castle Wars", 2368, 3072, 2431, 3135),
            new Box("Jiggig", 2456, 3032, 2495, 3055),
            new Box("East Feldip hills, near Rantz", 2648, 2952, 2655, 2975),
            new Box("Ape Atoll", 2688, 2688, 2815, 2815),
            new Box("Pest Control", 2624, 2560, 2687, 2623),
            new Box("Desert Bandit Camp", 3152, 2960, 3191, 2999),
            new Box("Al Kharid", 3264, 3136, 3327, 3199),
            new Box("Wizards Tower", 3094, 3144, 3125, 3175),
            new Box("Draynor Village", 3104, 3232, 3135, 3255),
            new Box("Draynor Village", 3112, 3256, 3135, 3263),
            new Box("Falador", 2944, 3304, 3015, 3327),
            new Box("Falador", 2944, 3328, 3007, 3455),
            new Box("Barbarian Village", 3048, 3392, 3135, 3407),
            new Box("Barbarian Village", 3056, 3408, 3135, 3439),
            new Box("Barbarian Village", 3064, 3440, 3135, 3447),
            new Box("Barbarian Village", 3072, 3448, 3135, 3455),
            // ---- stand-ins for areas built away from their real map squares
            new Box("God Wars Dungeon", 2830, 3695, 2960, 3795)); // the chambers around the GWD landmark (2917, 3745)

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
