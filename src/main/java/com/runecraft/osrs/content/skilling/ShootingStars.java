package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;

import java.util.ArrayList;
import java.util.List;

/**
 * Shooting Stars (OSRS Wiki): a star crashes roughly every 90 minutes (up to 15 minutes either way) at one of the
 * wiki's landing sites, never twice in a row at the same one (a repeat goes to the Daeyalt essence mine entrance
 * instead). A crashed star lands at size 6-9 (25/30/25/20%); each size needs 10 Mining per size, and a layer wears
 * away after 7 minutes of being mined, so a star shrinks by one size every 7 minutes while someone mines it and
 * crumbles after its size-1 layer. Every successful attempt (low/high 73/117 at any size) gives one stardust and
 * 32 Mining XP; the first player to mine a new star gets the discovery bonus (double stardust for the next 300).
 * Stardust buys Dusuri's Star Shop rewards by the Mining Guild in Falador.
 *
 * <p>Simplified (approx): there are no telescopes, so the crash is announced to every player with its site's area;
 * the star does not degrade while nobody mines it; it is gone after a restart (as after an OSRS system update).
 */
public final class ShootingStars extends SkillingPart {
    public static final String STAR = "crashed_star";
    public static final String STARDUST = "stardust";
    public static final String DUSURI = "dusuri";
    public static final String DUSURI_PLACE = "dusuri_star_shop";
    public static final int LOW = 73;
    public static final int HIGH = 117;
    public static final double XP = 32.0;
    /** Game ticks of mining that wear away one layer (7 minutes). */
    public static final int LAYER_TICKS = 700;
    /** Mean game ticks between crashes (90 minutes) and the spread either way (15 minutes). */
    public static final int CRASH_INTERVAL = 9000;
    public static final int CRASH_SPREAD = 1500;
    /** The first star after a (re)start lands 20-50 minutes in. */
    public static final int FIRST_CRASH_MIN = 2000;
    public static final int FIRST_CRASH_MAX = 5000;
    /** Extra stardust a star's discoverer receives (one per stardust mined, no XP). */
    public static final int DISCOVERY_BONUS = 300;
    /** Crash chance per landing size 6, 7, 8, 9 (percent). */
    public static final int[] CRASH_WEIGHTS = {25, 30, 25, 20};

    /** Mining level needed for a star of this size (size 1 = 10 ... size 9 = 90). */
    public static int level(int size) {
        return size * 10;
    }

    /** The landing size for a uniform roll in [0, 1). */
    public static int landingSize(double u) {
        int r = (int) Math.floor(u * 100);
        for (int i = 0; i < CRASH_WEIGHTS.length; i++) {
            if (r < CRASH_WEIGHTS[i]) {
                return 6 + i;
            }
            r -= CRASH_WEIGHTS[i];
        }
        return 9;
    }

    /** Game ticks until the next crash for a uniform roll in [0, 1). */
    public static int nextCrashTicks(double u) {
        return CRASH_INTERVAL - CRASH_SPREAD + (int) Math.floor(u * (2 * CRASH_SPREAD + 1));
    }

    /** A landing site: the wiki's area heading, the site and its OSRS tile. */
    public record Site(String region, String name, int x, int y) {
    }

    private static Site s(String region, String name, int x, int y) {
        return new Site(region, name, x, y);
    }

    /** The special site a star goes to when it rolls the site it last landed on. */
    public static final String REPEAT_SITE = "Daeyalt essence mine entrance";

    public static final List<Site> SITES = List.of(
            s("Asgarnia", "Dwarven Mine northern entrance", 3018, 3444),
            s("Asgarnia", "Mining Guild entrance", 3030, 3348),
            s("Asgarnia", "West Falador mine", 2906, 3355),
            s("Asgarnia", "Taverley (White Wolf Tunnel entrance)", 2882, 3474),
            s("Asgarnia", "Crafting Guild", 2940, 3280),
            s("Asgarnia", "Rimmington mine", 2974, 3241),
            s("Crandor and Karamja", "South Crandor mine", 2822, 3238),
            s("Crandor and Karamja", "North Crandor mine", 2835, 3296),
            s("Crandor and Karamja", "North Brimhaven mine", 2736, 3221),
            s("Crandor and Karamja", "South Brimhaven mine", 2742, 3143),
            s("Crandor and Karamja", "Karamja Jungle mine (Nature Altar)", 2845, 3037),
            s("Crandor and Karamja", "Shilo Village mine", 2827, 2999),
            s("Feldip Hills and the Isle of Souls", "Feldip Hunter area", 2571, 2964),
            s("Feldip Hills and the Isle of Souls", "Rantz's cave", 2630, 2993),
            s("Feldip Hills and the Isle of Souls", "Corsair Cove", 2567, 2858),
            s("Feldip Hills and the Isle of Souls", "Corsair Cove Resource Area", 2483, 2886),
            s("Feldip Hills and the Isle of Souls", "Myths' Guild", 2468, 2842),
            s("Feldip Hills and the Isle of Souls", "Isle of Souls mine", 2200, 2792),
            s("Fossil Island and Mos Le'Harmless", "Fossil Island mine", 3774, 3814),
            s("Fossil Island and Mos Le'Harmless", "Volcanic Mine entrance", 3818, 3801),
            s("Fossil Island and Mos Le'Harmless", "Mos Le'Harmless", 3686, 2969),
            s("Fremennik Lands and Lunar Isle", "Rellekka mine", 2683, 3699),
            s("Fremennik Lands and Lunar Isle", "Keldagrim entrance mine", 2727, 3683),
            s("Fremennik Lands and Lunar Isle", "Miscellania mine", 2528, 3887),
            s("Fremennik Lands and Lunar Isle", "Jatizso mine entrance", 2393, 3814),
            s("Fremennik Lands and Lunar Isle", "Central Fremennik Isles mine", 2375, 3832),
            s("Fremennik Lands and Lunar Isle", "Lunar Isle mine entrance", 2139, 3938),
            s("Great Kourend", "Hosidius mine", 1778, 3493),
            s("Great Kourend", "Shayzien mine", 1597, 3648),
            s("Great Kourend", "Port Piscarilius mine", 1769, 3709),
            s("Great Kourend", "Dense essence mine", 1760, 3853),
            s("Great Kourend", "Lovakite mine", 1437, 3840),
            s("Great Kourend", "Lovakengj (bank)", 1534, 3747),
            s("Kandarin", "Catherby bank", 2804, 3434),
            s("Kandarin", "Yanille bank", 2602, 3086),
            s("Kandarin", "Port Khazard mine", 2624, 3141),
            s("Kandarin", "Legends' Guild mine", 2705, 3333),
            s("Kandarin", "Coal Trucks", 2589, 3478),
            s("Kandarin", "South-east Ardougne mine (monastery)", 2608, 3233),
            s("Kebos Lowlands", "Kebos Lowlands mine (Kebos Swamp)", 1210, 3651),
            s("Kebos Lowlands", "Mount Karuulm mine", 1279, 3817),
            s("Kebos Lowlands", "Mount Karuulm (bank)", 1322, 3816),
            s("Kebos Lowlands", "Mount Quidamortem (bank)", 1258, 3564),
            s("Kharidian Desert", "Al Kharid mine", 3296, 3298),
            s("Kharidian Desert", "Al Kharid (bank)", 3276, 3164),
            s("Kharidian Desert", "Uzer mine", 3424, 3160),
            s("Kharidian Desert", "Desert quarry", 3171, 2910),
            s("Kharidian Desert", "Agility Pyramid mine", 3316, 2867),
            s("Kharidian Desert", "Nardah", 3434, 2889),
            s("Kharidian Desert", "Emir's Arena", 3351, 3281),
            s("Misthalin", "East Lumbridge Swamp mine", 3230, 3155),
            s("Misthalin", "West Lumbridge Swamp mine", 3153, 3150),
            s("Misthalin", "Draynor Village", 3094, 3235),
            s("Misthalin", "Varrock (East bank)", 3258, 3408),
            s("Misthalin", "South-east Varrock mine", 3290, 3353),
            s("Misthalin", "South-west Varrock mine", 3175, 3362),
            s("Morytania", "Canifis (bank)", 3505, 3485),
            s("Morytania", "Burgh de Rott (bank)", 3500, 3219),
            s("Morytania", "Abandoned Mine", 3451, 3233),
            s("Morytania", "Ver Sinhaza (bank)", 3650, 3214),
            s("Morytania", "Daeyalt essence mine entrance", 3635, 3340),
            s("Piscatoris and the Gnome Stronghold", "Piscatoris mine", 2341, 3635),
            s("Piscatoris and the Gnome Stronghold", "Grand Tree", 2444, 3490),
            s("Piscatoris and the Gnome Stronghold", "Tree Gnome Stronghold bank", 2448, 3436),
            s("Tirannwn", "Isafdar mine", 2269, 3158),
            s("Tirannwn", "Arandar mine", 2318, 3269),
            s("Tirannwn", "Lletya", 2329, 3163),
            s("Tirannwn", "Trahaearn mine entrance", 3274, 6055),
            s("Tirannwn", "Trahaearn mine entrance", 2253, 3299),
            s("Tirannwn", "Mynydd mine", 2173, 3409),
            s("Varlamore", "Civitas illa Fortis", 1771, 3102),
            s("Varlamore", "Stonecutter Outpost", 1742, 2954),
            s("Varlamore", "Ralos' Rise mining site", 1486, 3089),
            s("Varlamore", "Mistrock mine", 1422, 2873),
            s("Varlamore", "Salvager Overlook mine", 1625, 3275),
            s("Varlamore", "Custodia Mountains mine", 1287, 3413),
            s("Wilderness", "South Wilderness mine (Mage of Zamorak)", 3108, 3569),
            s("Wilderness", "South-west Wilderness mine (South of Dark Warriors' Fortress)", 3018, 3593),
            s("Wilderness", "Bandit Camp mine (Hobgoblins)", 3093, 3756),
            s("Wilderness", "Lava Maze runite mine", 3057, 3887),
            s("Wilderness", "Resource Area", 3188, 3932),
            s("Wilderness", "Mage Arena", 3091, 3962),
            s("Wilderness", "Pirates' Hideout mine", 3049, 3940));

    public static Site repeatSite() {
        for (Site s : SITES) {
            if (s.name().equals(REPEAT_SITE)) {
                return s;
            }
        }
        throw new IllegalStateException(REPEAT_SITE);
    }

    /**
     * The next landing site for a uniform roll in [0, 1): any site but the repeat site, and the repeat site when the
     * roll gives the site the previous star used ({@code previous} may be null).
     */
    public static Site nextSite(Site previous, double u) {
        List<Site> pool = new ArrayList<>();
        for (Site s : SITES) {
            if (!s.name().equals(REPEAT_SITE)) {
                pool.add(s);
            }
        }
        Site pick = pool.get(Math.min(pool.size() - 1, (int) Math.floor(u * pool.size())));
        return pick.equals(previous) ? repeatSite() : pick;
    }

    /** One of Dusuri's rewards: the item given ("bag_full_of_gems" rolls {@link #BAG_OF_GEMS}), how many, the stardust price. */
    public record Reward(String give, String label, int count, int stardust) {
    }

    /**
     * Dusuri's Star Shop. The bag full of gems (40 uncut gems) and the soft clay pack (100 soft clay) are handed out
     * opened. The celestial ring is sold uncharged and is not wearable yet. // approx
     */
    public static final List<Reward> SHOP = List.of(
            new Reward("celestial_ring_uncharged", "Celestial ring (uncharged)", 1, 2000),
            new Reward("star_fragment", "Star fragment", 1, 3000),
            new Reward("bag_full_of_gems", "Bag full of gems (40 uncut gems)", 40, 300),
            new Reward("soft_clay", "Soft clay pack (100 soft clay)", 100, 150));

    /**
     * Celestial ring (OSRS Wiki): charged or not, an invisible +4 Mining boost (not for level requirements). Each stardust
     * added is one charge (up to 10,000); charged, every ore mined from a rock up to adamantite uses a charge, whether or
     * not the 1/10 extra ore comes.
     */
    public static final String CELESTIAL_RING = "celestial_ring";
    public static final String CELESTIAL_RING_UNCHARGED = "celestial_ring_uncharged";
    public static final int CELESTIAL_BOOST = 4;
    public static final int CELESTIAL_MAX_CHARGES = 10_000;
    public static final int CELESTIAL_EXTRA_ORE_ONE_IN = 10;
    /** Ores the mining enhancers' extra-ore effect applies to (rocks up to adamantite). */
    public static final List<String> ENHANCER_ORES = List.of("clay", "copper_ore", "tin_ore", "iron_ore", "silver_ore", "coal",
            "gold_ore", "mithril_ore", "adamantite_ore");

    /** Mining level used for success rolls with a celestial ring worn (+4, invisible). */
    public static int celestialLevel(int mining, boolean ringWorn) {
        return ringWorn ? mining + CELESTIAL_BOOST : mining;
    }

    /** Whether mining this ore uses one of the ring's charges (every ore from a suitable rock while charged). */
    public static boolean celestialUsesCharge(String ore, int charges) {
        return charges > 0 && ENHANCER_ORES.contains(ore);
    }

    /** Whether the charged ring gives an extra ore this time ({@code roll} uniform in [0, 1)). */
    public static boolean celestialExtraOre(String ore, int charges, double roll) {
        return celestialUsesCharge(ore, charges) && roll < 1.0 / CELESTIAL_EXTRA_ORE_ONE_IN;
    }

    /** Stardust accepted when charging a ring holding {@code charges}. */
    public static int celestialCharge(int charges, int stardust) {
        return Math.max(0, Math.min(stardust, CELESTIAL_MAX_CHARGES - Math.max(0, charges)));
    }

    /** Uncut gems in a stardust bag full of gems, by weight. // approx */
    public static final List<Loot> BAG_OF_GEMS = List.of(Loot.of("uncut_sapphire", 500), Loot.of("uncut_emerald", 346),
            Loot.of("uncut_ruby", 136), Loot.of("uncut_diamond", 16), Loot.of("uncut_dragonstone", 2));

    @Override
    public void items(ItemSink s) {
        simple(s, STARDUST, "Stardust", "essence", 0xE8D060, 1).stack(Integer.MAX_VALUE)
                .examine("Some dust from a fallen star.");
        // Worn in the systems module's ring slot (Jewellery.wornExternal); charged by using stardust on it.
        simple(s, "celestial_ring_uncharged", "Celestial ring (uncharged)", "ring", 0xD8C890, 2000).stack(1)
                .examine("A ring made from the remnants of a fallen star.");
        simple(s, CELESTIAL_RING, "Celestial ring", "ring", 0xF0E0A0, 2000).stack(1)
                .examine("A ring made from the remnants of a fallen star. It glows with stardust.");
        simple(s, "star_fragment", "Star fragment", "gem", 0xE8E0A0, 3000).stack(1)
                .examine("A piece of a fallen star, still glowing faintly.");
    }

    @Override
    public List<BlockDef> blocks() {
        return List.of(BlockDef.scripted(STAR, "Crashed star", 0xC8B860, "rock"));
    }

    @Override
    public List<PlaceDef> places() {
        // Dusuri stands by the Mining Guild entrance in Falador (cache tile 3022, 3342).
        return List.of(PlaceDef.of(DUSURI_PLACE, "Dusuri's Star Shop", 3022, 3342, PlaceDef.Style.NONE, List.of()));
    }

    @Override
    public List<NpcDef> npcs() {
        return List.of(NpcDef.of(DUSURI, "Dusuri", DUSURI_PLACE, 0, 0, 0xC8A080, 0x3A3A6A, 0x2A2A4A, 0xE8D060));
    }
}
