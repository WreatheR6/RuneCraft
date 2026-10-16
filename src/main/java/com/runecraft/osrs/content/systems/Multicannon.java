package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.systems.CombatAchievements.Tier;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The dwarf multicannon (OSRS Wiki, "Dwarf multicannon"): unlocked by Dwarf Cannon, bought from Nulodion (750,000 coins for
 * the set), set up on a free 3x3 by placing the base (base, stand, barrels, furnace), loaded with steel (max hit 30) or
 * granite (max hit 35) cannonballs, and turned one of eight directions every game tick, firing up to one ball per
 * direction at a monster in that direction's target area. Damage gives 2 Ranged XP per point and no Hitpoints XP. It holds
 * 30 balls, 35/45/60 with the medium/hard/elite Combat Achievement rewards; it breaks after 25 minutes (a click repairs
 * it) and decays 10 minutes later, after which Nulodion hands it back for free.
 */
public final class Multicannon {
    public static final String BASE = "cannon_base";
    public static final String STAND = "cannon_stand";
    public static final String BARRELS = "cannon_barrels";
    public static final String FURNACE = "cannon_furnace";
    /** Assembly order. */
    public static final List<String> PARTS = List.of(BASE, STAND, BARRELS, FURNACE);
    public static final String STEEL_BALL = "cannonball";
    public static final String GRANITE_BALL = "granite_cannonball";
    public static final String GRANITE_DUST = "granite_dust";
    public static final String AMMO_MOULD = "ammo_mould";
    /** The Dwarf Cannon quest's old mould id (Nulodion swaps it for {@link #AMMO_MOULD}). */
    public static final String OLD_QUEST_MOULD = "cannon_ammo_mould";
    /** The set-up cannon (a scripted block) and the stages before it is complete. */
    public static final String BLOCK = "dwarf_multicannon";
    public static final String QUEST = "dwarf_cannon";
    /** Nulodion, the Black Guard's cannon engineer (OSRS 3012, 3452: the western building of the base south of Ice Mountain). */
    public static final String NULODION = "nulodion";
    public static final int NULODION_X = 3012;
    public static final int NULODION_Y = 3452;

    /** Nulodion's set price (dialogue) and each part's store value (shop price is 107%: 200,625). */
    public static final int SET_PRICE = 750_000;
    public static final int PART_VALUE = 187_500;
    public static final int PART_PRICE = PART_VALUE * 107 / 100;

    public static final int BASE_CAPACITY = 30;
    public static final int MAX_HIT_STEEL = 30;
    public static final int MAX_HIT_GRANITE = 35;
    /** Ranged XP per point of cannon damage, in tenths (2 XP; no Hitpoints XP). */
    public static final int XP_TENTHS_PER_DAMAGE = 20;
    /** Granite cannonballs: 50 Smithing, steel cannonball + granite dust, no XP. */
    public static final int GRANITE_LEVEL = 50;
    /** Directions the barrel can face; it turns 45 degrees every game tick (a revolution in 8 ticks, 4.8 s). */
    public static final int DIRECTIONS = 8;
    /** Game ticks per assembly step (base, stand, barrels, furnace). // approx */
    public static final int SETUP_TICKS_PER_STEP = 2;
    /** The cannon breaks after 25 minutes set up (2,500 ticks); clicking it repairs it. */
    public static final int BREAK_TICKS = 2_500;
    /** ...and disappears 10 minutes after that (35 minutes), reclaimable from Nulodion. */
    public static final int DECAY_TICKS = 3_500;
    /** Farthest tile (Chebyshev distance from the cannon's centre tile) it fires at. // approx */
    public static final int RANGE = 8;
    /** Ammo mould from Nulodion's shop (store value 5 coins at his 107% rate). */
    public static final int MOULD_PRICE = 5;

    /**
     * The cannon's target tile offset: OSRS tiles east ({@code [0]}) and north ({@code [1]}) of the tile under the cannon's
     * centre block. Gielinor tiles are {@link GielinorMap#SCALE} blocks, and OSRS north is Minecraft -z.
     */
    public static int[] tileDelta(int cannonBlockX, int cannonBlockZ, int targetBlockX, int targetBlockZ) {
        return new int[]{GielinorMap.toOsrsX(targetBlockX) - GielinorMap.toOsrsX(cannonBlockX),
                GielinorMap.toOsrsY(targetBlockZ) - GielinorMap.toOsrsY(cannonBlockZ)};
    }

    /** Blocks around the cannon's centre block that can hold a target within {@link #RANGE} tiles. */
    public static int reachBlocks() {
        return (RANGE + 1) * GielinorMap.SCALE;
    }

    /**
     * Monsters the cannon won't fire at: every RuneCraft creature whose wiki infobox says {@code immunecannon = Yes} (or
     * "Immune") for all its versions (snapshot {@code osrs/systems/cannon_immunity.tsv}, diffed by MulticannonTest). Sand,
     * ammonite and frost crabs are among them, as are demonic gorillas, tormented demons and the Iorwerth kurask/turoth.
     */
    public static final Set<String> IMMUNE = Set.of(
            "acidic_araxyte", "akkhas_shadow", "ammonite_crab", "amoxliatl", "araxxor", "arrav", "balance_elemental",
            "basilisk_knight", "blood_jaguar", "blood_moon", "blood_reaver", "blue_moon", "branda_the_fire_queen", "brutus",
            "callisto", "cave_kraken", "champion_of_scabaras", "cruor", "demonic_brutus", "demonic_gorilla", "doom_of_mokhaiotl",
            "ds2_vorkath", "dt2_forsaken_assassin", "dt2_kasonde", "dt2_kasonde_the_craven", "dt2_ketla_the_unworthy",
            "dt2_persten_the_deceitful", "dt2_quest_duke_sucellus", "dt2_quest_leviathan", "dt2_quest_vardorvis",
            "dt2_quest_whisperer", "duke_sucellus", "eclipse_moon", "eldric_the_ice_king", "elidinis_warden",
            "fremennik_warband_archer", "fremennik_warband_berserker", "fremennik_warband_seer", "frost_crab", "fumus",
            "gemstone_crab", "glacies", "gwd_spiritual_mage_armadyl", "gwd_spiritual_mage_bandos", "gwd_spiritual_mage_saradomin",
            "gwd_spiritual_mage_zamorak", "gwd_spiritual_ranger_armadyl", "gwd_spiritual_ranger_bandos",
            "gwd_spiritual_ranger_saradomin", "gwd_spiritual_ranger_zamorak", "gwd_spiritual_warrior_armadyl",
            "gwd_spiritual_warrior_bandos", "gwd_spiritual_warrior_saradomin", "gwd_spiritual_warrior_zamorak", "hespori",
            "jaguar_warrior", "javelin_colossus", "kraken", "kruk", "kurask", "mirrorback_araxyte", "nex", "phantom_muspah",
            "phosanis_nightmare", "quest_assassin", "quest_black_eye_bethel", "quest_blood_moon", "quest_blue_moon",
            "quest_eclipse_moon", "quest_evelot", "quest_giant_lobster", "quest_lowerniel_drakan", "quest_shellbane_gryphon",
            "quest_strange_creature", "ruptura_araxyte", "sand_crab", "scurrius", "serpent_shaman", "shellbane_gryphon",
            "spiritual_mage", "spiritual_ranger", "spiritual_warrior", "temple_guardian", "the_hueycoatl", "the_leviathan",
            "the_nightmare", "the_whisperer", "toa_arcane_scarab", "toa_baboon_brawler", "toa_baboon_mage", "toa_baboon_thrower",
            "toa_scarab_swarm", "toa_soldier_scarab", "toa_spitting_scarab", "tormented_demon", "tumekens_warden", "turoth",
            "umbra", "vardorvis", "vorkath", "vyrewatch_sentinel", "wyrm", "yama");

    /** Whether the cannon refuses to fire at this creature id. */
    public static boolean immune(String creatureId) {
        return creatureId != null && IMMUNE.contains(creatureId);
    }

    /**
     * Bosses a cannon may be set up near: most bosses destroy a cannon (it is refused near them), but the wiki's usage notes
     * name the Wilderness bosses and the Corporeal Beast as cannon targets; these are the ones whose infobox also says
     * {@code immunecannon = No} and whose lair isn't a no-cannon zone (Callisto is immune; Vet'ion's Rest is prohibited).
     */
    public static final Set<String> CANNON_BOSSES = Set.of("corporeal_beast", "chaos_elemental", "chaos_fanatic",
            "crazy_archaeologist", "scorpia", "venenatis", "artio", "spindel");

    private Multicannon() {
    }

    static void items(ItemSink items) {
        part(items, BASE, "Cannon base", "pot_lid", 10.0, "The cannon is built on this.");
        part(items, STAND, "Cannon stand", "clockwork", 0.907, "The mounting for the multicannon.");
        part(items, BARRELS, "Cannon barrels", "pipe", 15.0, "The barrels of the multicannon.");
        part(items, FURNACE, "Cannon furnace", "pot", 5.0, "This powers the multicannon.");
        items.add(GRANITE_BALL, "Granite cannonball", Kind.SIMPLE, "cannonball", 0x7A7A80).value(85)
                .examine("Ammo for the Dwarf Cannon, coated in granite.");
    }

    private static void part(ItemSink items, String id, String name, String shape, double kg, String examine) {
        items.add(id, name, Kind.SIMPLE, shape, 0x3A3A40).value(PART_VALUE).stack(1).weight(kg).examine(examine);
    }

    /** Cannonballs the cannon holds: 30, 35 from the medium, 45 from the hard and 60 from the elite Combat Achievement tier. */
    public static int capacity(Tier unlocked) {
        if (unlocked == null) {
            return BASE_CAPACITY;
        }
        return switch (unlocked) {
            case EASY -> BASE_CAPACITY;
            case MEDIUM -> 35;
            case HARD -> 45;
            default -> 60;
        };
    }

    public static int maxHit(boolean granite) {
        return granite ? MAX_HIT_GRANITE : MAX_HIT_STEEL;
    }

    /** The direction after {@code facing}: N, NE, E, SE, S, SW, W, NW, then N again. */
    public static int next(int facing) {
        return (facing + 1) % DIRECTIONS;
    }

    /** Unit step (east, north) of a direction (0 = north, clockwise). */
    public static int[] step(int facing) {
        return switch (Math.floorMod(facing, DIRECTIONS)) {
            case 0 -> new int[]{0, 1};
            case 1 -> new int[]{1, 1};
            case 2 -> new int[]{1, 0};
            case 3 -> new int[]{1, -1};
            case 4 -> new int[]{0, -1};
            case 5 -> new int[]{-1, -1};
            case 6 -> new int[]{-1, 0};
            default -> new int[]{-1, 1};
        };
    }

    /**
     * Whether a target tile ({@code dx} east, {@code dy} north of the cannon's centre tile) is in the target area of the
     * barrel facing {@code facing}. A straight direction covers a three-tile-wide strip, a diagonal direction its whole
     * quadrant, so the areas overlap on the lines one tile off the cannon's centre: those tiles are shot twice a
     * revolution, the double-hit spots RuneLite marks around the 3x3 (one tile from the corner or adjacent to it).
     * Tiles under the cannon and beyond {@link #RANGE} are never shot.
     */
    public static boolean inArea(int facing, int dx, int dy) {
        if (Math.max(Math.abs(dx), Math.abs(dy)) > RANGE || (Math.abs(dx) <= 1 && Math.abs(dy) <= 1)) {
            return false;
        }
        int[] s = step(facing);
        if (s[0] == 0) {
            return Integer.signum(dy) == s[1] && Math.abs(dx) <= 1;
        }
        if (s[1] == 0) {
            return Integer.signum(dx) == s[0] && Math.abs(dy) <= 1;
        }
        return dx * s[0] >= 1 && dy * s[1] >= 1;
    }

    /** How many times a revolution a target on this tile is shot (0, 1 or 2). */
    public static int shotsPerRevolution(int dx, int dy) {
        int n = 0;
        for (int f = 0; f < DIRECTIONS; f++) {
            if (inArea(f, dx, dy)) {
                n++;
            }
        }
        return n;
    }

    /** What a game tick of a set-up cannon's life does at {@code age} ticks. */
    public enum Life { OK, BREAKS, DECAYS }

    public static Life life(long age) {
        if (age >= DECAY_TICKS) {
            return Life.DECAYS;
        }
        return age >= BREAK_TICKS ? Life.BREAKS : Life.OK;
    }

    // ------------------------------------------------------------------ where it can't be set up

    /** An OSRS tile box where a cannon can't be set up, with the game's message. */
    public record Zone(String name, int x1, int y1, int x2, int y2, String message) {
        public boolean contains(int x, int y) {
            return x >= Math.min(x1, x2) && x <= Math.max(x1, x2) && y >= Math.min(y1, y2) && y <= Math.max(y1, y2);
        }
    }

    public static final String DEFAULT_MESSAGE = "You can't set up a cannon here.";
    private static final String DWARVES = "The dwarves won't be happy if you set up a cannon here.";

    /** Surface areas (OSRS Wiki's prohibited-areas list; boxes approx). */
    public static final List<Zone> ZONES = List.of(
            new Zone("Grand Exchange", 3140, 3465, 3190, 3515,
                    "The Grand Exchange staff prefer not to have heavy artillery operated around their premises."),
            new Zone("Black Knights' Fortress", 3005, 3498, 3035, 3530,
                    "It is not permitted to set up a cannon this close to the Dwarf Black Guard."),
            new Zone("Ice Mountain and the Black Guard base", 3000, 3440, 3030, 3497, DWARVES),
            new Zone("Warriors' Guild", 2837, 3530, 2877, 3556, DEFAULT_MESSAGE),
            new Zone("Entrana", 2800, 3320, 2880, 3395, DEFAULT_MESSAGE),
            new Zone("Mort'ton", 3478, 3268, 3510, 3310, DEFAULT_MESSAGE),
            new Zone("Jiggig", 2455, 3030, 2490, 3060, "The ogres won't be happy if you set up a cannon here."),
            new Zone("Feldip Hills jungle hunter area", 2525, 2880, 2620, 2940, DEFAULT_MESSAGE),
            new Zone("Fight Arena", 2580, 3140, 2610, 3175, DWARVES),
            new Zone("Mogre Camp", 2980, 3100, 3010, 3130, "It's just a little too damp for you to set up a cannon here."),
            new Zone("Wintertodt", 1600, 3960, 1660, 4030, "It's far too cold for you to set up a cannon here."),
            new Zone("Slayer Tower", 3403, 3529, 3453, 3580, "A strange magical force won't let you set up a cannon here."),
            new Zone("Gilbert's hut", 3100, 3400, 3115, 3410, "A colourful magical force won't let you set up a cannon here."));

    /** Areas around a module place or Landmark: {place, radius, message}. */
    public static final List<String[]> AROUND = List.of(
            new String[]{"catacombs_of_kourend", "20", "An ancient power won't let you set up a cannon here."},
            new String[]{"revenant_caves", "25", DEFAULT_MESSAGE},
            new String[]{"abyssal_nexus", "15", DEFAULT_MESSAGE},
            new String[]{"kraken_cove", "15", DEFAULT_MESSAGE});

    /** Carved dungeons (by dungeon id) where it can't be set up. */
    public static final Map<String, String> DUNGEONS = Map.ofEntries(
            Map.entry("dng_dwarven_mine", DWARVES),
            Map.entry("dng_ancient_cavern", "It's far too damp for you to set up a cannon here."),
            Map.entry("dng_fremennik_slayer_dungeon", "The air is too dank for you to set up a cannon here."),
            Map.entry("dng_slayer_tower_basement", "A strange magical force won't let you set up a cannon here."),
            Map.entry("dng_tarns_lair", "This temple is ancient and would probably collapse if you started firing a cannon."),
            Map.entry("dng_smoke_dungeon", "Dusty Aliv won't be happy if you set up a cannon here."),
            Map.entry("dng_tzhaar_city", "The TzHaar won't be happy if you set up a cannon here."),
            Map.entry("dng_jormungands_prison", "The ground is too soft for you to set up a cannon here."), // approx: basilisk areas only
            Map.entry("dng_karuulm_slayer_dungeon",
                    "The electricity bursting through this part of the cavern would render the cannon useless."), // approx: wyrm/hydra areas only
            Map.entry("dng_kraken_cove", DEFAULT_MESSAGE),
            Map.entry("dng_lizardman_caves", "The ground is too marshy here."),
            Map.entry("dng_forthos_dungeon", "A mysterious presence won't let you set up a cannon here."),
            Map.entry("dng_tonali_cavern", "An ancient magical force won't let you set up a cannon here."),
            Map.entry("dng_ruins_of_tapoyauik", "An ancient magical force won't let you set up a cannon here."),
            Map.entry("dng_wyvern_cave", "Fungi start eating into the legs of your cannon; you quickly decide not to set it up here."),
            Map.entry("dng_abyss_passage", DEFAULT_MESSAGE),
            Map.entry("dng_the_abyss", DEFAULT_MESSAGE));

    /** Boss lairs where it can't be set up (boss id to message); the other bosses destroy it (it is refused near any boss). */
    public static final Map<String, String> BOSS_LAIRS = Map.ofEntries(
            Map.entry("zulrah", "It's far too damp for you to set up a cannon here."),
            Map.entry("vorkath", "The intense cold threatens to crack the metal of your cannon. You probably shouldn't put it down here."),
            Map.entry("cerberus", "Your cannon would melt if you set it up here."),
            Map.entry("callisto", "It doesn't seem safe to set up a cannon here."),
            Map.entry("vetion", "It doesn't seem safe to set up a cannon here."),
            Map.entry("bryophyta", DEFAULT_MESSAGE),
            Map.entry("scurrius", "It's far too damp for you to set up a cannon here."),
            Map.entry("hespori", "It's far too damp down here to set up a cannon."),
            Map.entry("brutus", "It's far too muddy for you to set up a cannon here."),
            Map.entry("alchemical_hydra", "The electricity bursting through this part of the cavern would render the cannon useless."),
            Map.entry("doom_of_mokhaiotl", "An ancient magical force won't let you set up a cannon here."));

    /** The prohibited surface zone containing an OSRS tile, or null. */
    public static Zone zone(int osrsX, int osrsY) {
        for (Zone z : ZONES) {
            if (z.contains(osrsX, osrsY)) {
                return z;
            }
        }
        return null;
    }
}
