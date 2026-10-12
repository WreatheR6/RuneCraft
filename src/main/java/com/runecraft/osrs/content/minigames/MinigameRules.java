package com.runecraft.osrs.content.minigames;

import java.util.List;

/**
 * Numbers and formulas behind each minigame: points, experience, timers, monster tiers, drop
 * chances. Pure data so it can be unit tested. Values follow the OSRS wiki where known; the rest
 * are close approximations (marked).
 */
public final class MinigameRules {
    private MinigameRules() {
    }

    /** One reward-table line: item x[min,max] with weight. */
    public record Loot(String item, int min, int max, int weight) {
    }

    /** Rolls a weighted table with a random value in [0,1). */
    public static Loot pick(List<Loot> table, double random) {
        int total = 0;
        for (Loot l : table) {
            total += l.weight();
        }
        int r = (int) Math.floor(random * total);
        for (Loot l : table) {
            r -= l.weight();
            if (r < 0) {
                return l;
            }
        }
        return table.get(table.size() - 1);
    }

    // ---------------------------------------------------------------- Pest Control
    public static final class PestControl {
        /** Lander tiers: name, minimum combat level, commendation points per win, portal hitpoints. */
        public static final String[] TIERS = {"Novice", "Intermediate", "Veteran"};
        public static final int[] COMBAT = {40, 70, 100};
        public static final int[] POINTS = {3, 4, 5};
        public static final int[] PORTAL_HP = {200, 250, 250};
        /** Hitpoint scale applied to pests by lander tier (approx. of the per-level pest variants). */
        public static final double[] PEST_SCALE = {0.75, 1.0, 1.3};
        public static final int VOID_KNIGHT_HP = 200;
        /** A game lasts at most 20 minutes (2000 game ticks). */
        public static final int GAME_TICKS = 2000;
        /** Activity needed to earn points (OSRS: 50 damage dealt / contribution). */
        public static final int MIN_ACTIVITY = 50;
        public static final String[] PESTS = {"brawler", "defiler", "ravager", "shifter", "spinner", "splatter", "torcher"};
        public static final String[] PORTALS = {"purple_portal", "blue_portal", "yellow_portal", "red_portal"};
        /** Ticks between portal shields dropping (one shield at a time, in random order). */
        public static final int SHIELD_INTERVAL = 50;

        private PestControl() {
        }

        public static int tierFor(int combatLevel) {
            return combatLevel >= COMBAT[2] ? 2 : combatLevel >= COMBAT[1] ? 1 : 0;
        }

        /** XP bought with points: floor(level^2 / 600) * N per point, +1% for 10 points, +10% for 100. */
        public static int xpFor(int level, int multiplier, int points) {
            int per = RewardShop.pestControlXpPerPoint(level, multiplier);
            double bonus = points >= 100 ? 1.10 : points >= 10 ? 1.01 : 1.0;
            return (int) Math.floor(per * points * bonus);
        }

        /** Void set effects: accuracy and damage multipliers (style: 0 melee, 1 ranged, 2 magic). */
        public static double voidAccuracy(int style) {
            return style == 2 ? 1.45 : 1.10;
        }

        public static double voidDamage(int style, boolean elite) {
            return switch (style) {
                case 0 -> 1.10;
                case 1 -> elite ? 1.125 : 1.10;
                default -> elite ? 1.05 : 1.0; // elite void mage: +5% magic damage since May 2024
            };
        }
    }

    // ---------------------------------------------------------------- Wintertodt
    public static final class Wintertodt {
        public static final int POINTS_ROOT = 10;
        public static final int POINTS_KINDLING = 25;
        public static final int POINTS_LIGHT = 25;
        public static final int POINTS_FIX = 25;
        public static final int POINTS_HEAL_PYROMANCER = 30;
        public static final int MIN_POINTS = 500;
        public static final int MAX_ROLLS = 28;
        public static final int BRAZIERS = 4;
        public static final int PHOENIX_CHANCE = 5000;
        public static final int TOME_CHANCE = 1000;
        public static final int DRAGON_AXE_CHANCE = 10000;
        public static final int OUTFIT_CHANCE = 150;

        private Wintertodt() {
        }

        /** Wintertodt energy: about 600 units plus 400 per player (approx. so a solo game lasts minutes). */
        public static int energy(int players) {
            return 600 + 400 * Math.max(1, players);
        }

        public static double chopXp(int woodcutting) {
            return woodcutting * 0.3;
        }

        public static double fletchXp(int fletching) {
            return fletching * 0.6;
        }

        public static double burnRootXp(int firemaking) {
            return firemaking * 3.0;
        }

        public static double burnKindlingXp(int firemaking) {
            return firemaking * 3.8;
        }

        public static double lightXp(int firemaking) {
            return firemaking * 6.0;
        }

        public static double fixXp(int construction) {
            return construction * 4.0;
        }

        /** Bonus Firemaking XP for a successful game (500+ points). */
        public static double completionXp(int firemaking) {
            return firemaking * 100.0;
        }

        /** Supply crate rolls: one per 500 points (minimum 500), capped at 28. */
        public static int rolls(int points) {
            return points < MIN_POINTS ? 0 : Math.min(MAX_ROLLS, points / MIN_POINTS);
        }

        /**
         * Cold damage per hit (OSRS wiki): floor((16 - W - 2B) * (H + 1) / 99), where W is the number
         * of warm items worn (max 4), B is 1 when the player is beside a lit brazier and H is the
         * Hitpoints level. At least 1.
         */
        public static int coldDamage(int hitpointsLevel, int warmItems, int litBraziersNearby) {
            int w = Math.max(0, Math.min(4, warmItems));
            int b = litBraziersNearby > 0 ? 1 : 0;
            return Math.max(1, (16 - w - 2 * b) * (hitpointsLevel + 1) / 99);
        }

        public static final List<Loot> CRATE = List.of(
                new Loot("oak_logs", 15, 40, 8), new Loot("willow_logs", 15, 40, 8), new Loot("maple_logs", 12, 35, 6),
                new Loot("yew_logs", 10, 30, 5), new Loot("magic_logs", 6, 20, 3),
                new Loot("iron_ore", 15, 40, 6), new Loot("coal", 15, 40, 6), new Loot("mithril_ore", 10, 25, 5),
                new Loot("adamantite_ore", 5, 15, 3), new Loot("runite_ore", 1, 4, 1),
                new Loot("uncut_sapphire", 2, 6, 4), new Loot("uncut_emerald", 2, 5, 3), new Loot("uncut_ruby", 1, 4, 2),
                new Loot("uncut_diamond", 1, 3, 1),
                new Loot("grimy_ranarr_weed", 2, 5, 3), new Loot("grimy_irit_leaf", 2, 6, 3), new Loot("grimy_kwuarm", 2, 5, 2),
                new Loot("grimy_snapdragon", 1, 4, 1), new Loot("grimy_torstol", 1, 3, 1),
                new Loot("ranarr_seed", 1, 3, 2), new Loot("snapdragon_seed", 1, 2, 1), new Loot("torstol_seed", 1, 2, 1),
                new Loot("raw_tuna", 10, 25, 5), new Loot("raw_lobster", 10, 25, 5), new Loot("raw_swordfish", 8, 20, 4),
                new Loot("raw_shark", 5, 15, 2), new Loot("pure_essence", 50, 150, 5), new Loot("coins", 2000, 8000, 6),
                new Loot("burnt_page", 7, 30, 4), new Loot("dragon_bones", 2, 6, 1));
    }

    // ---------------------------------------------------------------- Tempoross
    public static final class Tempoross {
        public static final int POINTS_CATCH = 10;
        public static final int POINTS_COOK = 10;
        public static final int POINTS_LOAD = 10;
        public static final int POINTS_TETHER = 25;
        public static final int POINTS_SPIRIT = 25;
        public static final int MIN_POINTS = 2000;
        /** Energy damage per raw / cooked harpoonfish loaded into a cannon (approx.). */
        public static final int RAW_DAMAGE = 3;
        public static final int COOKED_DAMAGE = 5;
        /** Essence removed per harpoon strike on the spirit pool (approx.). */
        public static final int SPIRIT_DAMAGE = 4;
        public static final int VULNERABLE_TICKS = 25;
        public static final int WAVE_INTERVAL = 40;
        public static final int WAVE_WARNING = 6;

        private Tempoross() {
        }

        /** Reward permits for a subdued Tempoross: 1 at 2,000 points plus one per 700 above (approx.), max 8. */
        public static int permits(int points) {
            return points < MIN_POINTS ? 0 : Math.min(8, 1 + (points - MIN_POINTS) / 700);
        }

        public static double catchXp(int fishing) {
            return fishing * 1.0;
        }

        public static double cookXp(int cooking) {
            return cooking * 0.7;
        }

        public static final List<Loot> POOL = List.of(
                new Loot("raw_tuna", 15, 40, 12), new Loot("raw_lobster", 10, 30, 10), new Loot("raw_swordfish", 10, 25, 8),
                new Loot("raw_shark", 5, 15, 5), new Loot("spirit_flakes", 32, 64, 10), new Loot("soaked_page", 7, 30, 8),
                new Loot("oak_plank", 10, 30, 6), new Loot("teak_plank", 5, 15, 4), new Loot("mahogany_plank", 3, 10, 2),
                new Loot("coins", 1000, 5000, 6), new Loot("harpoon", 1, 1, 2),
                new Loot("uncut_sapphire", 1, 3, 3), new Loot("uncut_emerald", 1, 3, 2), new Loot("uncut_ruby", 1, 2, 1));
        /** Uniques per reward pool roll (1 in N). */
        public static final Object[][] UNIQUES = {
                {"fish_barrel", 400}, {"tackle_box", 400}, {"big_harpoonfish", 1600}, {"tome_of_water", 1600},
                {"dragon_harpoon", 8000}, {"tiny_tempor", 8000}};
    }

    // ---------------------------------------------------------------- Guardians of the Rift
    public static final class Guardians {
        /** Rune altars reachable through the rifts: id, rune, level, xp per essence, catalytic? */
        public static final Object[][] ALTARS = {
                {"air", "air_rune", 1, 5.0, false}, {"water", "water_rune", 5, 6.0, false},
                {"earth", "earth_rune", 9, 6.5, false}, {"fire", "fire_rune", 14, 7.0, false},
                {"mind", "mind_rune", 2, 5.5, true}, {"body", "body_rune", 20, 7.5, true},
                {"cosmic", "cosmic_rune", 27, 8.0, true}, {"chaos", "chaos_rune", 35, 8.5, true},
                {"nature", "nature_rune", 44, 9.0, true}, {"law", "law_rune", 54, 9.5, true},
                {"death", "death_rune", 65, 10.0, true}, {"blood", "blood_rune", 77, 10.5, true}};
        public static final int ALTAR_SWITCH_TICKS = 33;
        public static final int POWER_PER_STONE = 2;
        public static final int SEARCH_COST = 100;
        public static final int NEEDLE_CHANCE = 300;
        public static final int PET_CHANCE = 4000;
        public static final int MIN_ENERGY = 150;

        private Guardians() {
        }

        /** Extra runes from the Raiments of the Eye: 10% per piece and +20% for the full set (60%). */
        public static double raimentBonus(int pieces) {
            return pieces >= 4 ? 0.60 : pieces * 0.10;
        }

        /** Rift searches earned: (elemental + catalytic energy) / 100, at least 1 with 150+ energy. */
        public static int searches(int elemental, int catalytic) {
            int total = elemental + catalytic;
            return total < MIN_ENERGY ? 0 : Math.max(1, total / SEARCH_COST);
        }

        public static final List<Loot> POUCH = List.of(
                new Loot("abyssal_pearls", 3, 12, 30), new Loot("air_rune", 50, 150, 6), new Loot("water_rune", 50, 150, 6),
                new Loot("earth_rune", 50, 150, 6), new Loot("fire_rune", 50, 150, 6), new Loot("mind_rune", 40, 120, 5),
                new Loot("body_rune", 30, 90, 5), new Loot("cosmic_rune", 15, 45, 4), new Loot("chaos_rune", 15, 45, 4),
                new Loot("nature_rune", 10, 30, 4), new Loot("law_rune", 8, 25, 3), new Loot("death_rune", 6, 20, 3),
                new Loot("blood_rune", 4, 15, 2), new Loot("elemental_talisman", 1, 1, 1), new Loot("catalytic_talisman", 1, 1, 1));
    }

    // ---------------------------------------------------------------- Barbarian Assault
    public static final class BarbarianAssault {
        public static final String[] ROLES = {"attacker", "defender", "collector", "healer"};
        public static final String[] PENANCE = {"penance_fighter", "penance_ranger", "penance_runner", "penance_healer"};
        public static final int WAVES = 10;
        public static final int QUEEN_HP = 250;
        public static final int PET_CHANCE = 1000;

        private BarbarianAssault() {
        }

        /** Penance per wave: fighters, rangers, runners, healers (approx. of the wiki wave table). */
        public static int[] wave(int wave) {
            return new int[]{2 + (wave - 1) / 2, 2 + (wave - 1) / 2, 2 + wave / 3, 2 + (wave - 1) / 3};
        }

        /** Penance hitpoints for a type in a wave (approx.). */
        public static int hitpoints(int type, int wave) {
            int[] base = {15, 12, 10, 12};
            int[] step = {9, 8, 6, 7};
            return base[type] + step[type] * (wave - 1);
        }

        /** Honour points for clearing a wave in your role (approx.). */
        public static int honour(int wave) {
            return wave >= WAVES ? 20 : wave * 2;
        }

        /** Total honour points for a full game (all ten waves). */
        public static int fullGameHonour() {
            int total = 0;
            for (int w = 1; w <= WAVES; w++) {
                total += honour(w);
            }
            return total;
        }
    }

    // ---------------------------------------------------------------- Castle Wars
    public static final class CastleWars {
        public static final int GAME_TICKS = 2000;
        public static final int TICKETS_WIN = 2;
        public static final int TICKETS_DRAW = 1;
        public static final int TICKETS_LOSS = 1;

        private CastleWars() {
        }
    }

    // ---------------------------------------------------------------- Warriors' Guild
    public static final class WarriorsGuild {
        /** Animated armour: tier, combat level, hitpoints, max hit, tokens awarded. */
        public static final Object[][] ARMOUR = {
                {"bronze", 11, 10, 2, 5}, {"iron", 23, 20, 3, 10}, {"steel", 46, 40, 5, 15}, {"black", 69, 60, 7, 20},
                {"mithril", 92, 80, 9, 25}, {"adamant", 113, 99, 11, 30}, {"rune", 138, 118, 14, 40}};
        /** Tokens taken on entering the cyclops room and then every minute (100 game ticks). */
        public static final int CYCLOPS_TOKENS = 10;
        /** Kamfreena only lets players with at least 100 tokens into the cyclops room. */
        public static final int CYCLOPS_ENTRY_TOKENS = 100;
        public static final int CYCLOPS_TICKS_PER_CHARGE = 100;
        public static final int DEFENDER_CHANCE = 50;
        public static final int DRAGON_DEFENDER_CHANCE = 100;

        private WarriorsGuild() {
        }

        /** Entry: 130 combined Attack + Strength, or 99 in either. */
        public static boolean canEnter(int attack, int strength) {
            return attack + strength >= 130 || attack >= 99 || strength >= 99;
        }

        /** Index (0=bronze..7=dragon) of the defender a player can get next, given the best one they own (-1 none). */
        public static int nextDefender(int bestOwned) {
            return Math.min(MinigameItems.DEFENDER_TIERS.length - 1, bestOwned + 1);
        }

        public static int tokens(String tier) {
            for (Object[] a : ARMOUR) {
                if (a[0].equals(tier)) {
                    return (Integer) a[4];
                }
            }
            return 0;
        }
    }

    // ---------------------------------------------------------------- Mage Training Arena
    public static final class MageArena {
        public static final String[] ROOMS = {"telekinetic", "alchemist", "enchantment", "graveyard"};
        /** Points per completed action in each room (approx.). */
        public static final int TELEKINETIC_MAZE = 2;
        public static final int TELEKINETIC_BONUS_EVERY = 5;
        public static final int TELEKINETIC_BONUS = 8;
        public static final int ALCHEMY_COINS_PER_POINT = 100;
        public static final int ENCHANT_SHAPES_PER_POINT = 1;
        public static final int GRAVEYARD_BONES_PER_POINT = 4;

        private MageArena() {
        }
    }

    // ---------------------------------------------------------------- Pyramid Plunder
    public static final class PyramidPlunder {
        public static final int[] ROOM_LEVELS = {21, 31, 41, 51, 61, 71, 81, 91};
        /** Thieving XP for looting an urn (3x the snake-check XP), per room. */
        public static final double[] URN_XP = {60, 90, 150, 210, 300, 450, 675, 825};
        /** Thieving XP for opening the Grand Gold Chest, per room. */
        public static final double[] CHEST_XP = {40, 60, 100, 140, 200, 300, 450, 550};
        /** Strength XP for opening the sarcophagus, per room. */
        public static final double[] SARCOPHAGUS_XP = {20, 30, 50, 70, 100, 150, 225, 275};
        /** Sceptre chance (1 in N) from the Grand Gold Chest per room (approx.). */
        public static final int[] SCEPTRE_CHANCE = {3500, 2250, 1250, 750, 650, 650, 650, 650};
        public static final int GAME_TICKS = 500;

        private PyramidPlunder() {
        }

        /** Highest room a Thieving level allows (1-8), or 0. */
        public static int maxRoom(int thieving) {
            int room = 0;
            for (int i = 0; i < ROOM_LEVELS.length; i++) {
                if (thieving >= ROOM_LEVELS[i]) {
                    room = i + 1;
                }
            }
            return room;
        }

        /** Index into {@link MinigameItems#ARTEFACTS} for an urn in a room, given a random value in [0,1). */
        public static int artefact(int room, double random) {
            int max = Math.min(MinigameItems.ARTEFACTS.length - 1, room + 1);
            int min = Math.max(0, room - 3);
            return min + (int) Math.floor(random * (max - min + 1));
        }
    }

    // ---------------------------------------------------------------- Hallowed Sepulchre
    public static final class Sepulchre {
        public static final int[] FLOOR_LEVELS = {52, 62, 72, 82, 92};
        public static final double[] FLOOR_XP = {575, 925, 1500, 2700, 6000};
        public static final int RING_CHANCE = 1000;
        public static final int GAME_TICKS = 300;

        private Sepulchre() {
        }

        /** Hallowed marks from a coffin on a floor (1-based), given a random value in [0,1). */
        public static int coffinMarks(int floor, double random) {
            return floor + (int) Math.floor(random * (floor + 1));
        }
    }

    // ---------------------------------------------------------------- Tithe Farm
    public static final class TitheFarm {
        /** Planting XP per seed (approx.; OSRS gives most Tithe XP on harvest and deposit). */
        public static final double[] PLANT_XP = {3.5, 5.0, 7.0}; // approx
        /** Farming XP per fruit harvested: golovanova 6, bologano 14, logavano 23. */
        public static final double[] HARVEST_XP = {6, 14, 23};
        /** Farming XP per fruit deposited (10x harvest XP); fruit 75-100 of a run pay double. */
        public static final double[] DEPOSIT_XP = {60, 140, 230};
        /** Fruit a sack holds per run; the 75th earns a 250x harvest XP bonus. */
        public static final int SACK_SIZE = 100;
        public static final int BONUS_FRUIT = 75;
        public static final int MAX_POINTS = 16_000;
        public static final int WATERINGS = 3;
        public static final int GROW_TICKS = 25;
        public static final int GAME_TICKS = 1000;

        private TitheFarm() {
        }

        /** Best seed (index into MinigameItems.TITHE_PLANTS) for a Farming level, or -1. */
        public static int seedFor(int farming) {
            int best = -1;
            for (int i = 0; i < MinigameItems.TITHE_LEVELS.length; i++) {
                if (farming >= MinigameItems.TITHE_LEVELS[i]) {
                    best = i;
                }
            }
            return best;
        }

        /** Deposit XP for the n-th fruit (1-based) put in the sack this run. */
        public static double depositXp(int type, int n) {
            double xp = n >= BONUS_FRUIT ? DEPOSIT_XP[type] * 2 : DEPOSIT_XP[type];
            if (n == BONUS_FRUIT) {
                xp += HARVEST_XP[type] * 250;
            }
            return xp;
        }

        /** Points for the n-th fruit (1-based) of a run: one per third fruit, plus 2 on the 100th. */
        public static int depositPoints(int n) {
            int pts = n % 3 == 0 ? 1 : 0;
            return n == SACK_SIZE ? pts + 2 : pts;
        }

        /** Points for depositing {@code fruit} fruit in an empty sack (a full sack of 100 pays 35). */
        public static int points(int fruit) {
            int total = 0;
            for (int n = 1; n <= Math.min(SACK_SIZE, fruit); n++) {
                total += depositPoints(n);
            }
            return total;
        }
    }

    // ---------------------------------------------------------------- Mahogany Homes
    public static final class MahoganyHomes {
        public static final String[] TIERS = {"Beginner", "Novice", "Adept", "Expert"};
        public static final String[] PLANKS = {"plank", "oak_plank", "teak_plank", "mahogany_plank"};
        public static final int[] LEVELS = {1, 20, 50, 70};
        public static final int[] POINTS = {2, 3, 4, 5};
        public static final double[] XP_PER_PLANK = {30, 48, 72, 112};
        public static final int HOTSPOTS = 4;

        private MahoganyHomes() {
        }

        public static int tierFor(int construction) {
            int tier = 0;
            for (int i = 0; i < LEVELS.length; i++) {
                if (construction >= LEVELS[i]) {
                    tier = i;
                }
            }
            return tier;
        }
    }

    // ---------------------------------------------------------------- Giants' Foundry
    public static final class GiantsFoundry {
        public static final String[] BARS = {"bronze_bar", "iron_bar", "steel_bar", "mithril_bar", "adamantite_bar", "runite_bar"};
        public static final int[] BAR_VALUE = {1, 2, 3, 5, 7, 10};
        public static final int BARS_NEEDED = 14;
        public static final int STAGES = 6;
        public static final int GAME_TICKS = 600;

        private GiantsFoundry() {
        }

        /** Reputation for a finished sword: quality (0-100) times metal value / 2 (approx.). */
        public static int reputation(int quality, int metalScore) {
            return Math.max(1, quality * metalScore / 20);
        }

        /** Smithing XP for a sword: proportional to quality and metal value (approx.). */
        public static double xp(int quality, int metalScore) {
            return quality * metalScore * 1.5;
        }
    }

    // ---------------------------------------------------------------- Fishing Trawler
    public static final class Trawler {
        public static final int GAME_TICKS = 500;
        public static final int LEAK_INTERVAL = 18;
        public static final int MAX_WATER = 100;
        public static final String[] OUTFIT = {"angler_hat", "angler_top", "angler_waders", "angler_boots"};

        private Trawler() {
        }

        /** Fish caught in the net for a Fishing level and contribution (approx.). */
        public static String fishFor(int fishing) {
            if (fishing >= 76) {
                return "raw_shark";
            }
            if (fishing >= 50) {
                return "raw_swordfish";
            }
            if (fishing >= 40) {
                return "raw_lobster";
            }
            if (fishing >= 35) {
                return "raw_tuna";
            }
            return "raw_anchovies";
        }
    }

    // ---------------------------------------------------------------- Rogues' Den
    public static final class RoguesDen {
        public static final String[] OBSTACLES = {"Contortion bars", "Pendulum", "Floor spikes", "Wall safe trap",
                "Grill", "Ledge", "Blade trap", "Tile maze"};
        public static final String[] OUTFIT = {"rogue_mask", "rogue_top", "rogue_trousers", "rogue_gloves", "rogue_boots"};

        private RoguesDen() {
        }

        /** Chance to pass an obstacle: 60% at level 50 rising to 98% at 99 (approx.). */
        public static double passChance(int level) {
            return Math.min(0.98, 0.60 + Math.max(0, level - 50) * 0.0078);
        }
    }

    // ---------------------------------------------------------------- Shades of Mort'ton
    public static final class Shades {
        /** Shade key dropped by each shade tier (1 in 4 chance, approx.). */
        public static final String[] KEYS = {"bronze_shade_key", "steel_shade_key", "black_shade_key", "silver_shade_key",
                "gold_shade_key"};
        public static final List<Loot> CHEST = List.of(
                new Loot("coins", 200, 3000, 10), new Loot("uncut_sapphire", 1, 2, 4), new Loot("uncut_emerald", 1, 2, 3),
                new Loot("uncut_ruby", 1, 1, 2), new Loot("uncut_diamond", 1, 1, 1), new Loot("rune_full_helm", 1, 1, 1),
                new Loot("adamant_platelegs", 1, 1, 1), new Loot("law_rune", 5, 20, 3), new Loot("death_rune", 5, 15, 2),
                new Loot("flamtaer_hammer", 1, 1, 1));

        private Shades() {
        }
    }
}
