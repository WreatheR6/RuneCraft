package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.Chance;
import com.runecraft.osrs.Skill;
import java.util.ArrayList;
import java.util.List;

/**
 * The Hallowed Sepulchre below Darkmeyer (OSRS Wiki, July 2026): five floors (Agility 52/62/72/82/92, unboostable; entry needs
 * Sins of the Father), each a route of trap corridors, coffins behind skill challenges, middle stairs and a platform jump,
 * ending at the stairs down and a magical obelisk (floor 5: the Grand Hallowed Coffin). The run starts with 2 minutes; each
 * floor descended adds 2 more and saved time carries over; a hallowed token adds a minute. Out of time, the floor can still
 * be finished for its XP, but nothing can be looted and the stairs are sealed.
 *
 * <p>Pure data: the wiki's numbers (floor XP, obstacle XP, coffin loot tables, marks, rates, pet chances) and the route of
 * each floor (from the wiki's floor guides) plus the trap timings, which are a deterministic function of the tick so the
 * session, the self-test and the tests agree on where it is safe to stand. Trap cycles, speeds and damage are approx (the
 * wiki only describes them).
 */
public final class HallowedSepulchre {
    public static final int FLOORS = 5;
    /** Agility needed for each floor (base level; boosts don't count). */
    public static final int[] FLOOR_LEVELS = {52, 62, 72, 82, 92};
    /** Agility XP for finishing a floor: the stairs down or a magical obelisk (wiki: 500/850/1,425/2,625/5,850). */
    public static final double[] FLOOR_XP = {500, 850, 1425, 2625, 5850};
    /** Climbing down the stairs in the middle of a floor (wiki "Stairs (Hallowed Sepulchre)"). */
    public static final double MIDDLE_STAIRS_XP = 25;
    /** The platform jump before each floor's obelisk (wiki "Platform": 50, 50, 50, none on floor 4, 100 to the Grand Coffin). */
    public static final double[] PLATFORM_XP = {50, 50, 50, 0, 100};
    /** Floor 2's north path starts with four consecutive platforms, 10 XP each. */
    public static final double UPPER_PLATFORM_XP = 10;
    public static final int UPPER_PLATFORMS = 4;
    /**
     * Agility XP for each crossing of a coffin's obstacle (pillar, bridge, holy barrier, portal), there and back, first time
     * only (wiki: 50 on floors 1-2, 75 on floors 3-4, 80 on floor 5).
     */
    public static final double[] CROSSING_XP = {50, 50, 75, 75, 80};

    /** Game ticks on the clock entering floor 1 (2 minutes); each floor descended adds 2 more minutes. */
    public static final int START_TICKS = 200;
    public static final int FLOOR_TICKS = 200;
    /** A hallowed token adds one minute. */
    public static final int TOKEN_TICKS = 100;
    /** Out of time, how long the session waits for the player to finish the floor before sending them out (approx). */
    public static final int OVERTIME_TICKS = 500;

    /** Coffins: 66 Thieving (boostable), 200 Thieving XP, poison on a failed search. */
    public static final int COFFIN_THIEVING = 66;
    public static final double COFFIN_XP = 200;
    /** Hallowed marks from an ordinary coffin per floor (wiki: 1, 2-3, 3-5, 3-6, 4-6) and the Grand Hallowed Coffin (8-10). */
    public static final int[][] MARKS = {{1, 1}, {2, 3}, {3, 5}, {3, 6}, {4, 6}};
    public static final int[] GRAND_MARKS = {8, 10};
    /** Chance (percent) of a coffin rolling the low, mid and high-tier table on each floor. */
    public static final int[][] TIER_PERCENT = {{60, 35, 5}, {30, 60, 10}, {15, 65, 20}, {0, 60, 40}, {0, 20, 80}};
    /** Strange old lockpick (full) from an ordinary coffin: 1/200, 1/120, 1/90, 1/60, 1/40 by floor. */
    public static final int[] LOCKPICK_RATE = {200, 120, 90, 60, 40};
    /** Each floor has its own mysterious page: 1/10 from that floor's coffins until the player has it. */
    public static final int PAGE_RATE = 10;
    public static final String[] PAGES = {"mysterious_page", "mysterious_page_2", "mysterious_page_3", "mysterious_page_4",
            "mysterious_page_5"};

    /** A coffin's extra roll: an item at 1 in {@code rate} from the coffins of {@code floor}. */
    public record Tertiary(String item, int rate, int floor) {
    }

    /** The five mysterious pages, one per floor (also in {@link #PAGES}). */
    public static final List<Tertiary> PAGE_DROPS = List.of(new Tertiary(PAGES[0], PAGE_RATE, 1), new Tertiary(PAGES[1], PAGE_RATE, 2),
            new Tertiary(PAGES[2], PAGE_RATE, 3), new Tertiary(PAGES[3], PAGE_RATE, 4), new Tertiary(PAGES[4], PAGE_RATE, 5));
    /** Clue scrolls from coffins (May 2022): easy 1/50, medium 1/50, medium 1/25, hard 1/50, elite 1/60; the Grand Coffin elite 1/30. */
    public static final String[] CLUES = {"clue_scroll_easy", "clue_scroll_medium", "clue_scroll_medium", "clue_scroll_hard",
            "clue_scroll_elite"};
    public static final int[] CLUE_RATE = {50, 50, 25, 50, 60};
    public static final int GRAND_CLUE_RATE = 30;
    /** Ring of endurance (uncharged), Grand Hallowed Coffin only. */
    public static final int RING_RATE = 200;
    /** Giant squirrel on leaving through a magical obelisk, by the floor completed (wiki: 1/35,000 ... 1/2,000). */
    public static final int[] PET_RATE = {35_000, 16_000, 8_000, 4_000, 2_000};
    public static final String PET = "pet_giant_squirrel";

    /** Coffin search success: the wiki's skilling chart (no lockpick -60..190, lockpick -40..210, strange old lockpick -20..230). */
    public enum Tool {
        NONE(-60, 190), LOCKPICK(-40, 210), STRANGE_OLD_LOCKPICK(-20, 230);

        public final int low;
        public final int high;

        Tool(int low, int high) {
            this.low = low;
            this.high = high;
        }
    }

    public static double coffinChance(int thieving, Tool tool) {
        return thieving < COFFIN_THIEVING ? 0 : Chance.lerp(thieving, tool.low, tool.high);
    }

    // ------------------------------------------------------------------ coffin loot

    /** One coffin-table line (each is 1/10): item, quantity range and potion doses (0 = not a potion). */
    public record Loot(String item, int min, int max, int doses) {
    }

    public enum Tier {
        LOW, MID, HIGH
    }

    public static final List<Loot> LOW = List.of(
            new Loot("monks_robe_top", 1, 1, 0), new Loot("monks_robe", 1, 1, 0), new Loot("holy_symbol", 1, 1, 0),
            new Loot("air_rune", 500, 750, 0), new Loot("fire_rune", 500, 750, 0), new Loot("chaos_rune", 25, 50, 0),
            new Loot("mithril_bolts", 50, 150, 0), new Loot("prayer_potion", 1, 1, 2), new Loot("white_lily", 1, 1, 0),
            new Loot("coins", 1500, 3000, 0));
    public static final List<Loot> MID = List.of(
            new Loot("adamant_2h_sword", 1, 1, 0), new Loot("adamant_platebody", 1, 1, 0), new Loot("cosmic_rune", 60, 100, 0),
            new Loot("death_rune", 60, 100, 0), new Loot("nature_rune", 60, 100, 0), new Loot("adamant_bolts", 50, 200, 0),
            new Loot("monkfish", 1, 3, 0), new Loot("prayer_potion", 1, 1, 4), new Loot("grimy_ranarr_weed", 1, 2, 0),
            new Loot("coins", 7500, 12500, 0));
    public static final List<Loot> HIGH = List.of(
            new Loot("rune_2h_sword", 1, 1, 0), new Loot("rune_platebody", 1, 1, 0), new Loot("law_rune", 150, 250, 0),
            new Loot("blood_rune", 150, 250, 0), new Loot("soul_rune", 150, 250, 0), new Loot("rune_bolts", 100, 300, 0),
            new Loot("monkfish", 2, 6, 0), new Loot("sanfew_serum", 1, 2, 4), new Loot("ranarr_seed", 1, 2, 0),
            new Loot("coins", 17500, 25000, 0));

    public static List<Loot> table(Tier tier) {
        return switch (tier) {
            case LOW -> LOW;
            case MID -> MID;
            case HIGH -> HIGH;
        };
    }

    /** The table an ordinary coffin on {@code floor} (1-based) rolls, given a random value in [0,1). */
    public static Tier tier(int floor, double random) {
        int[] p = TIER_PERCENT[floor - 1];
        double r = random * 100;
        if (r < p[0]) {
            return Tier.LOW;
        }
        return r < p[0] + p[1] ? Tier.MID : Tier.HIGH;
    }

    /** One line of a coffin table (each 1/10), given a random value in [0,1). */
    public static Loot roll(Tier tier, double random) {
        List<Loot> t = table(tier);
        return t.get(Math.min(t.size() - 1, (int) Math.floor(random * t.size())));
    }

    /** Hallowed marks from an ordinary coffin, given a random value in [0,1). */
    public static int marks(int floor, double random) {
        int[] m = MARKS[floor - 1];
        return m[0] + (int) Math.floor(random * (m[1] - m[0] + 1));
    }

    public static int grandMarks(double random) {
        return GRAND_MARKS[0] + (int) Math.floor(random * (GRAND_MARKS[1] - GRAND_MARKS[0] + 1));
    }

    /**
     * A hallowed sack (100 marks): five rolls on the low-tier table, one on the mid-tier and two on the high-tier (no pages or
     * lockpicks).
     */
    public static final int[] SACK_ROLLS = {5, 1, 2};

    // ------------------------------------------------------------------ coffin challenges

    /** The skill challenge in front of a coffin. */
    public enum Challenge {
        /** A broken bridge: hammer and saw, 2 planks and 5 nails (56 Construction, boostable). */
        BRIDGE("Broken bridge", Skill.CONSTRUCTION, 56),
        /** A Saradomin brazier lighting the holy barrier: 2 vampyre dust (1 with a hallowed symbol), 54 Prayer, 200 Prayer XP. */
        BRAZIER("Saradomin brazier", Skill.PRAYER, 54),
        /** A portal frame conjured with a standard-spellbook enchantment (7 Magic), 200 Magic XP; two failures in a row break it. */
        PORTAL("Portal frame", Skill.MAGIC, 7),
        /** A pillar grappled with a crossbow and a mith grapple (62 Ranged); the hallowed grapple never fails. */
        PILLAR("Pillar", Skill.RANGED, 62);

        public final String title;
        public final Skill skill;
        public final int level;

        Challenge(String title, Skill skill, int level) {
            this.title = title;
            this.skill = skill;
            this.level = level;
        }
    }

    /** Bridge materials (2 planks, 5 nails) and their Construction XP: plank/steel 150, oak/mithril 300, teak/adamant 450, mahogany/rune 700. */
    public record BridgeKit(String plank, String nails, double xp) {
    }

    public static final List<BridgeKit> BRIDGE_KITS = List.of(new BridgeKit("mahogany_plank", "rune_nails", 700),
            new BridgeKit("teak_plank", "adamant_nails", 450), new BridgeKit("oak_plank", "mithril_nails", 300),
            new BridgeKit("plank", "steel_nails", 150));
    public static final int BRIDGE_PLANKS = 2;
    public static final int BRIDGE_NAILS = 5;
    /** Chance of bending the nails on a bridge attempt without the hallowed hammer (approx: the wiki gives no rate). */
    public static final double BRIDGE_BEND_CHANCE = 0.15; // approx

    public static final int BRAZIER_DUST = 2;
    public static final double BRAZIER_XP = 200;

    public static final double PORTAL_XP = 200;
    public static final int PORTAL_BREAK_FAILURES = 2;
    /** The standard-spellbook enchantments a portal frame takes (Spellbooks ids), lowest first. */
    public static final String[] PORTAL_SPELLS = {"enchant_1", "enchant_2", "enchant_3", "enchant_4", "enchant_5", "enchant_6",
            "enchant_7"};

    /** Chance a conjuring with the enchantment of tier 1-7 succeeds without the hallowed focus (approx: "higher level, higher chance"). */
    public static double portalChance(int spellTier) {
        return Math.min(0.97, 0.55 + 0.06 * spellTier); // approx
    }

    /** Chance a grapple across a pillar succeeds without the hallowed grapple (approx). */
    public static double pillarChance(int ranged) {
        return Math.max(0, Math.min(0.95, 0.70 + 0.008 * (ranged - 62))); // approx
    }

    // ------------------------------------------------------------------ routes

    /** Trap types: wizard statues (flames), knight statues (swords), crossbowman statues (bolts), strange tiles, priest statues. */
    public enum Trap {
        FLAME, SWORD, ARROW, TILES, LIGHTNING
    }

    public enum Kind {
        /** A corridor of traps. */
        TRAPS,
        /** A coffin behind its skill challenge, beside the corridor. */
        COFFIN,
        /** Stairs down to the next level of the floor (25 XP). */
        STAIRS,
        /** The four 10 XP platforms at the start of floor 2's north path. */
        UPPER_PLATFORMS,
        /** The platform jump to the obelisk (the Grand Coffin's pedestal on floor 5). */
        PLATFORM,
        /** Floor 5's Grand Hallowed Coffin. */
        GRAND_COFFIN
    }

    /** One stretch of a floor's route. {@code rolls} is the loot rolls of a coffin (floor 5's bridge coffin has two). */
    public record Section(Kind kind, List<Trap> traps, Challenge challenge, int rolls) {
        public boolean isTrap() {
            return kind == Kind.TRAPS;
        }
    }

    private static Section traps(Trap... traps) {
        return new Section(Kind.TRAPS, List.of(traps), null, 0);
    }

    private static Section coffin(Challenge c) {
        return new Section(Kind.COFFIN, List.of(), c, 1);
    }

    private static final Section STAIRS = new Section(Kind.STAIRS, List.of(), null, 0);
    private static final Section PLATFORM = new Section(Kind.PLATFORM, List.of(), null, 0);
    private static final Section UPPER = new Section(Kind.UPPER_PLATFORMS, List.of(), null, 0);
    private static final Section GRAND = new Section(Kind.GRAND_COFFIN, List.of(), null, 0);

    private static final Trap F = Trap.FLAME;
    private static final Trap S = Trap.SWORD;
    private static final Trap A = Trap.ARROW;
    private static final Trap T = Trap.TILES;
    private static final Trap L = Trap.LIGHTNING;

    /** One entrance of a floor: its name and route, in order. */
    public record Route(String entrance, List<Section> sections) {
    }

    /**
     * Each floor's routes, one per entrance (picked at random on arrival), after the wiki's "Hallowed Sepulchre/Strategies"
     * floor guides: the traps in order, the coffins and their challenges. Where the middle stairs and platforms sit along a
     * route is approx (the wiki's maps).
     */
    public static final List<List<Route>> ROUTES = List.of(
            List.of(
                    new Route("north-west", List.of(traps(A), traps(F), STAIRS, coffin(Challenge.PILLAR), PLATFORM)),
                    new Route("north-east", List.of(traps(F), traps(S), STAIRS, traps(F), coffin(Challenge.PILLAR), PLATFORM)),
                    new Route("south-east", List.of(traps(S), traps(F), STAIRS, traps(A), coffin(Challenge.BRIDGE), PLATFORM)),
                    new Route("south-west", List.of(traps(F), traps(S), STAIRS, traps(A), coffin(Challenge.BRIDGE), PLATFORM))),
            List.of(
                    new Route("north", List.of(UPPER, traps(A), traps(S), coffin(Challenge.PILLAR), STAIRS, traps(T), traps(F), PLATFORM)),
                    new Route("east", List.of(traps(F), traps(A), traps(T), STAIRS, coffin(Challenge.BRAZIER), traps(F), PLATFORM)),
                    new Route("south", List.of(traps(A), traps(T), traps(S), STAIRS, coffin(Challenge.BRAZIER), traps(A), PLATFORM)),
                    new Route("west", List.of(traps(S), coffin(Challenge.PILLAR), STAIRS, traps(T), traps(F), traps(S), PLATFORM))),
            List.of(
                    new Route("east", List.of(traps(F), coffin(Challenge.PILLAR), traps(S), traps(A), traps(S), traps(F),
                            coffin(Challenge.BRAZIER), STAIRS, traps(A, F), PLATFORM)),
                    new Route("west", List.of(traps(A), coffin(Challenge.PILLAR), traps(F), traps(S, T), traps(A), traps(S),
                            coffin(Challenge.PORTAL), STAIRS, traps(A, F), PLATFORM))),
            List.of(
                    new Route("north", List.of(traps(L), traps(S, T), traps(A, T), traps(F), traps(A, T, L), coffin(Challenge.PORTAL),
                            STAIRS, traps(S, A), traps(T), coffin(Challenge.BRAZIER))),
                    new Route("south", List.of(traps(F), traps(L), traps(S, F), traps(A), coffin(Challenge.BRIDGE), STAIRS,
                            traps(S, L, T), traps(A, T), traps(T), coffin(Challenge.BRAZIER)))),
            List.of(
                    new Route("south", List.of(traps(F), traps(S, T), STAIRS, coffin(Challenge.PORTAL), traps(A, T), traps(F, L),
                            traps(S, T), coffin(Challenge.PILLAR), STAIRS, traps(A, L, T), traps(F, A, L),
                            new Section(Kind.COFFIN, List.of(), Challenge.BRIDGE, 2), PLATFORM, GRAND))));

    public static List<Route> routes(int floor) {
        return ROUTES.get(floor - 1);
    }

    /**
     * The tier of a trap on a floor (1-3; wiki trap tables): swords and bolts 1 on floors 1-2, 2 on floor 3, 3 on floor 5 and
     * on floor 4 2 above the middle stairs and 3 below; flames 1 on floors 1-2, else 2; strange tiles 1 above floor 2's middle
     * stairs, 2 on floors 2-4 and 3 on floor 4 below the stairs and on floor 5; lightning has one tier.
     */
    public static int tier(Trap trap, int floor, boolean belowStairs) {
        return switch (trap) {
            case SWORD, ARROW -> floor <= 2 ? 1 : floor == 3 ? 2 : floor == 4 ? (belowStairs ? 3 : 2) : 3;
            case FLAME -> floor <= 2 ? 1 : 2;
            case TILES -> floor == 2 ? (belowStairs ? 2 : 1) : floor == 3 ? 2 : floor == 4 ? (belowStairs ? 3 : 2) : 3;
            case LIGHTNING -> 1;
        };
    }

    // ------------------------------------------------------------------ trap timing

    /** Length of a trap corridor in tiles (blocks), along the route. */
    public static final int LEN = 12;
    /** Lanes across a trap corridor: -1, 0, 1. */
    public static final int LANES = 3;
    /** Ticks a player who ran into a trap is held before carrying on: without and with the hallowed ring (approx). */
    public static final int RECOVER_TICKS = 4;
    public static final int RING_RECOVER_TICKS = 1;
    /** Ticks of invulnerability after a strange tile teleports the player. */
    public static final int TILE_GRACE_TICKS = 2;
    /** Tiles a yellow strange tile throws the player back, a blue one forward. */
    public static final int TILE_JUMP = 4;
    /** Ticks a lightning strike stuns. */
    public static final int STUN_TICKS = 2;

    /** Damage of a flame, sword or bolt by floor (approx: "a small amount of damage"); none with the hallowed ring. */
    public static int trapDamage(int floor, double random) {
        return 1 + floor + (int) Math.floor(random * 3); // approx
    }

    /** The trap state one section of a run uses: the traps with their tiers and the section's random seed. */
    public record Layout(List<Trap> traps, int[] tiers, long seed, int floor) {
        public int tierOf(Trap t) {
            int i = traps.indexOf(t);
            return i < 0 ? 0 : tiers[i];
        }
    }

    public static Layout layout(Section s, int floor, boolean belowStairs, long seed) {
        int[] tiers = new int[s.traps().size()];
        for (int i = 0; i < tiers.length; i++) {
            tiers[i] = tier(s.traps().get(i), floor, belowStairs);
        }
        return new Layout(s.traps(), tiers, seed, floor);
    }

    private static long mix(long seed, long a, long b) {
        long h = seed * 0x9E3779B97F4A7C15L + a * 0xBF58476D1CE4E5B9L + b * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        h *= 0xD6E8FEB86659FD93L;
        h ^= h >>> 29;
        return h & Long.MAX_VALUE;
    }

    /** Flame rows (local z) and whether they burn at a tick. */
    public static boolean flameAt(int tier, int floor, int lz, long tick) {
        if (tier <= 1) {
            // Tier 1: every statue fires intermittently: rows 2, 5 and 8 burn for 2 ticks of every 6.
            long c = Math.floorMod(tick, 6);
            return (lz == 2 || lz == 5 || lz == 8) && (c == 3 || c == 4);
        }
        // Tier 2: two phases alternate (rows 2, 6, 10 then 4, 8); floor 5's statues change phase a tick faster.
        int cycle = floor >= 5 ? 5 : 6;
        long c = Math.floorMod(tick, cycle);
        boolean a = lz == 2 || lz == 6 || lz == 10;
        boolean b = lz == 4 || lz == 8;
        return a && (c == 0 || c == 1) || b && (floor >= 5 ? (c == 2 || c == 3) : (c == 3 || c == 4));
    }

    /** Sword speed (tiles per tick) and rest at the statue between throws, by tier. */
    public static int swordSpeed(int tier) {
        return tier + 1;
    }

    /** Ticks the knight statue holds the caught sword before throwing it again (wiki: five on floor 3's first traps). */
    private static int swordRest(int tier) {
        return 5; // approx
    }

    private static final int SWORD_START = 1;
    private static final int SWORD_STOP = LEN - 2;

    /**
     * The flying sword's centre (local z) at a tick, or {@link Integer#MIN_VALUE} while the statue holds it. The knight statue
     * at the corridor's start throws a 3x3 sword to the marked tile near its end and back, then rests before the next throw.
     */
    public static int swordAt(int tier, long tick) {
        int v = swordSpeed(tier);
        int dist = SWORD_STOP - SWORD_START;
        int out = (dist + v - 1) / v;
        int cycle = out + 1 + out + swordRest(tier);
        long c = Math.floorMod(tick, cycle);
        if (c < out) {
            return Math.min(SWORD_STOP, SWORD_START + (int) (c + 1) * v);
        }
        if (c == out) {
            return SWORD_STOP;
        }
        if (c < out + 1 + out) {
            long back = c - out;
            return Math.max(SWORD_START, SWORD_STOP - (int) back * v);
        }
        return Integer.MIN_VALUE;
    }

    /** Bolt speed (tiles per tick), volley interval and lanes firing per volley, by tier. */
    public static int boltSpeed(int tier) {
        return tier + 1;
    }

    private static int volleyInterval(int tier) {
        return tier >= 3 ? 3 : 4;
    }

    /**
     * Whether lane {@code lane} (-1..1) fires in volley {@code volley}: tier 1 one statue at random, tiers 2-3 two of the
     * three (one idle at random).
     */
    public static boolean laneFires(int tier, long seed, long volley, int lane) {
        int pick = (int) (mix(seed, volley, 7) % LANES) - 1;
        return tier <= 1 ? lane == pick : lane != pick;
    }

    /** Local z span [lo, hi] a bolt fired at {@code fireTick} sweeps during {@code tick} (from the far end toward the start). */
    private static int[] boltSpan(int tier, long fireTick, long tick) {
        int v = boltSpeed(tier);
        long dt = tick - fireTick;
        if (dt < 0) {
            return null;
        }
        int now = LEN - 1 - (int) (dt * v);
        int before = dt == 0 ? LEN - 1 : now + v;
        if (before < 0) {
            return null;
        }
        return new int[]{Math.max(0, now), Math.min(LEN - 1, before)};
    }

    /** Whether a bolt is in lane {@code lane} within local z span [z0, z1] during {@code tick}. */
    public static boolean boltHits(int tier, long seed, int lane, int z0, int z1, long tick) {
        int interval = volleyInterval(tier);
        int flight = (LEN + boltSpeed(tier) - 1) / boltSpeed(tier) + 1;
        long first = Math.floorDiv(tick - flight, interval);
        for (long volley = Math.max(0, first); volley * interval <= tick; volley++) {
            if (!laneFires(tier, seed, volley, lane)) {
                continue;
            }
            int[] span = boltSpan(tier, volley * interval, tick);
            if (span != null && span[0] <= Math.max(z0, z1) && span[1] >= Math.min(z0, z1)) {
                return true;
            }
        }
        return false;
    }

    /** Strange tiles: none, yellow (back) or blue (forward). */
    public enum TileState {
        NONE, YELLOW, BLUE
    }

    private static boolean tileRow(int lz) {
        return lz == 3 || lz == 4 || lz == 7 || lz == 8;
    }

    /** The strange tile at a spot at a tick: tier 1 yellow only, tier 2 adds blue, tier 3 lights up more often. */
    public static TileState tileAt(int tier, long seed, int lane, int lz, long tick) {
        if (!tileRow(lz)) {
            return TileState.NONE;
        }
        int period = tier >= 3 ? 2 : 3;
        long slot = Math.floorDiv(tick, period);
        long h = mix(seed, (lane + 2) * 31L + lz, slot);
        boolean active = tier >= 3 ? h % 2 == 0 : h % 3 == 0;
        if (!active) {
            return TileState.NONE;
        }
        boolean blue = tier >= 2 && Math.floorMod(lane + lz, 2) == 0;
        return blue ? TileState.BLUE : TileState.YELLOW;
    }

    /** Lightning: a chequered strike over rows 2-9 every 4 ticks, alternating the squares. */
    public static boolean lightningAt(int lane, int lz, long tick) {
        if (lz < 2 || lz > 9 || Math.floorMod(tick, 4) != 3) {
            return false;
        }
        long strike = Math.floorDiv(tick, 4);
        return Math.floorMod(lane + lz + strike, 2) == 0;
    }

    /** What happens to a player in a section. */
    public enum Hazard {
        NONE, HIT, YELLOW, BLUE, STUN
    }

    /**
     * The hazard a player meets moving from local z {@code z0} to {@code z1} in lane {@code lane} during {@code tick} (z outside
     * 0..LEN-1 is outside the corridor and safe). Flames, swords and bolts hit; tiles and lightning act on the tile they end on.
     */
    public static Hazard hazard(Layout l, int lane, int z0, int z1, long tick) {
        boolean inside = z1 >= 0 && z1 < LEN;
        int lo = Math.max(0, Math.min(z0, z1));
        int hi = Math.min(LEN - 1, Math.max(z0, z1));
        boolean crosses = hi >= lo && Math.max(z0, z1) >= 0 && Math.min(z0, z1) < LEN;
        for (int i = 0; i < l.traps().size(); i++) {
            int tier = l.tiers()[i];
            switch (l.traps().get(i)) {
                case FLAME -> {
                    if (inside && flameAt(tier, l.floor(), z1, tick)) {
                        return Hazard.HIT;
                    }
                }
                case SWORD -> {
                    int now = swordAt(tier, tick);
                    if (now != Integer.MIN_VALUE && crosses) {
                        int prev = swordAt(tier, tick - 1);
                        int s0 = prev == Integer.MIN_VALUE ? SWORD_START : prev;
                        int sLo = Math.min(s0, now) - 1;
                        int sHi = Math.max(s0, now) + 1;
                        if (sLo <= hi && sHi >= lo) {
                            return Hazard.HIT;
                        }
                    }
                }
                case ARROW -> {
                    if (crosses && boltHits(tier, l.seed(), lane, lo, hi, tick)) {
                        return Hazard.HIT;
                    }
                }
                default -> {
                }
            }
        }
        if (!inside) {
            return Hazard.NONE;
        }
        for (int i = 0; i < l.traps().size(); i++) {
            if (l.traps().get(i) == Trap.LIGHTNING && lightningAt(lane, z1, tick)) {
                return Hazard.STUN;
            }
            if (l.traps().get(i) == Trap.TILES) {
                TileState t = tileAt(l.tiers()[i], l.seed(), lane, z1, tick);
                if (t != TileState.NONE) {
                    return t == TileState.BLUE ? Hazard.BLUE : Hazard.YELLOW;
                }
            }
        }
        return Hazard.NONE;
    }

    /** One step of a planned crossing: lane and local z at the end of a tick. */
    public record Step(int lane, int z) {
    }

    /**
     * A safe crossing of a trap corridor for a player moving at most {@code speed} tiles a tick (lanes change freely),
     * starting just before it ({@code z = -1}) at {@code tick}: the positions at the end of each following tick until past the
     * end ({@code z = LEN}), avoiding every hazard (blue tiles too, so the plan doesn't depend on them), or null if none
     * within {@code horizon} ticks. Waiting before the corridor is always allowed.
     */
    public static List<Step> plan(Layout l, int lane, int z, long tick, int speed, int horizon) {
        int zs = LEN + 2;
        // state: (t, lane, z+1) visited
        boolean[][][] seen = new boolean[horizon + 1][LANES][zs];
        int[][][] from = new int[horizon + 1][LANES][zs];
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        int sz = Math.max(-1, Math.min(LEN, z));
        queue.add(new int[]{0, lane + 1, sz + 1});
        seen[0][lane + 1][sz + 1] = true;
        while (!queue.isEmpty()) {
            int[] s = queue.poll();
            int t = s[0];
            int cz = s[2] - 1;
            if (cz >= LEN) {
                List<Step> path = new ArrayList<>();
                int[] cur = s;
                while (cur[0] > 0) {
                    path.add(0, new Step(cur[1] - 1, cur[2] - 1));
                    int f = from[cur[0]][cur[1]][cur[2]];
                    cur = new int[]{cur[0] - 1, f / zs, f % zs};
                }
                return path;
            }
            if (t >= horizon) {
                continue;
            }
            for (int dz = speed; dz >= -speed; dz--) {
                int nz = Math.max(-1, Math.min(LEN, cz + dz));
                for (int nl = 0; nl < LANES; nl++) {
                    if (seen[t + 1][nl][nz + 1]) {
                        continue;
                    }
                    if (hazard(l, nl - 1, cz, nz, tick + t + 1) != Hazard.NONE) {
                        continue;
                    }
                    seen[t + 1][nl][nz + 1] = true;
                    from[t + 1][nl][nz + 1] = s[1] * zs + s[2];
                    queue.add(new int[]{t + 1, nl, nz + 1});
                }
            }
        }
        return null;
    }

    private HallowedSepulchre() {
    }
}
