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
        public static final int VOID_KNIGHT_HP = 200;
        /** The squire won't let anyone holding 4,000 commendation points board (OSRS Wiki). */
        public static final int MAX_POINTS = 4000;
        /** Coins for a win: ten times the player's combat level. */
        public static final int COINS_PER_COMBAT_LEVEL = 10;
        /** A game lasts at most 20 minutes (2000 game ticks); a Void Knight still alive then wins it. */
        public static final int GAME_TICKS = 2000;
        public static final String[] PESTS = {"brawler", "defiler", "ravager", "shifter", "spinner", "splatter", "torcher"};
        /** West, east, south-east and south-west; their stats, shields and pests are in {@link PestControlRules}. */
        public static final String[] PORTALS = {"purple_portal", "blue_portal", "yellow_portal", "red_portal"};

        private PestControl() {
        }

        public static int tierFor(int combatLevel) {
            return combatLevel >= COMBAT[2] ? 2 : combatLevel >= COMBAT[1] ? 1 : 0;
        }

        /** Points actually added for a win, given the current balance (capped at {@link #MAX_POINTS}). */
        public static int pointsAwarded(int tier, int balance) {
            return Math.max(0, Math.min(POINTS[tier], MAX_POINTS - balance));
        }

        /** Whole XP bought with points: floor(level^2 / 600) * N per point, +1% for 10 points, +10% for 100. */
        public static int xpFor(int level, int multiplier, int points) {
            return PestControlRules.xpTenths(level, multiplier, points) / 10;
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
        /** OSRS Wiki (16 October 2024): healing a pyromancer is worth 75 points (was 30). */
        public static final int POINTS_HEAL_PYROMANCER = 75;
        public static final int MIN_POINTS = WintertodtRewards.MIN_POINTS;
        public static final int MAX_ROLLS = WintertodtRewards.MAX_ROLLS;
        public static final int BRAZIERS = 4;

        private Wintertodt() {
        }

        // ---- energy (OSRS Wiki "Wintertodt/Strategies", Wintertodt's energy) ----
        /** The Wintertodt's energy meter, in percent; it doesn't grow with the number of players. */
        public static final int MAX_ENERGY = 100;
        /** Each pyromancer channelling from a lit brazier drains 1% every 14 ticks; feeding the brazier doesn't speed it up. */
        public static final int PYROMANCER_DRAIN_TICKS = 14;
        /** With every brazier out the Wintertodt regains 1% every 35 ticks. */
        public static final int REGAIN_TICKS = 35;
        /** A relit brazier's (or healed pyromancer's) channelling resumes after a short random delay of about 1-5 seconds. */
        public static final int RESUME_MIN_TICKS = 2;
        public static final int RESUME_MAX_TICKS = 8;
        /** Pyromancers have 14 hitpoints: two of the Wintertodt's hits incapacitate one. */
        public static final int PYROMANCER_HP = 14;
        public static final int PYROMANCER_HIT = 7;

        /** Wintertodt energy at the start of a game: always a full meter (was an approximation growing with the players). */
        public static int energy(int players) {
            return MAX_ENERGY;
        }

        /**
         * Whether an attack attempt goes through (Mod Ash): a random number up to the maximum energy is compared with the
         * remaining energy. Standard (and area) attacks happen when it is lower, so mostly early in the fight; attacks on the
         * braziers and pyromancers when it is higher, so mostly late.
         */
        public static boolean attacksPlayers(int roll0toMax, int energy) {
            return roll0toMax < energy;
        }

        public static boolean attacksBraziers(int roll0toMax, int energy) {
            return roll0toMax > energy;
        }

        // ---- warmth (OSRS Wiki "Wintertodt", Warmth Meter, since 9 October 2024) ----
        /**
         * The warmth meter (0-100%) replaced Hitpoints inside the prison: the cold hits it, food that heals 4+ restores 35%, a
         * dose of rejuvenation potion 30%, it regenerates 8% a minute plus 1% per warm item, and at 0% the player dies.
         */
        public static final int WARMTH_MAX = 100;
        public static final int FOOD_WARMTH = 35;
        public static final int FOOD_MIN_HEAL = 4;
        public static final int POTION_WARMTH = 30;
        /** The standard attack's cooldown on each player. */
        public static final int STANDARD_COOLDOWN_TICKS = 10;

        /** Warmth regenerated each game tick: (8% + 1% per warm item, up to 4) a minute of 100 ticks. */
        public static double warmthRegenPerTick(int warmItems) {
            return (8 + Math.max(0, Math.min(4, warmItems))) / 100.0;
        }

        /**
         * The standard attack's warmth damage (Mod Ed's formulas, wiki): floor((16 - W - min(2B, 6)) x 100 / FM), with W the
         * warm items worn (max 4), B the braziers lit (max 3) and FM the base Firemaking level.
         */
        public static int standardDamage(int firemaking, int warmItems, int litBraziers) {
            int w = Math.max(0, Math.min(4, warmItems));
            int b = Math.max(0, Math.min(3, litBraziers));
            return Math.max(0, (16 - w - Math.min(2 * b, 6)) * 100 / Math.max(1, firemaking));
        }

        /** A shattering brazier's damage to the players next to it: 2 x floor((10 - W) x 100 / FM). */
        public static int brazierDamage(int firemaking, int warmItems) {
            return 2 * ((10 - Math.max(0, Math.min(4, warmItems))) * 100 / Math.max(1, firemaking));
        }

        /** The snowfall (3x3) area attack's damage: 3 x floor((10 - W) x 100 / FM). */
        public static int areaDamage(int firemaking, int warmItems) {
            return 3 * ((10 - Math.max(0, Math.min(4, warmItems))) * 100 / Math.max(1, firemaking));
        }

        /**
         * The warm clothing that counts at the Wintertodt (OSRS Wiki "Warmth", the items RuneCraft has; snapshot
         * {@code wintertodt_warmth.tsv}, diffed by {@code WintertodtWarmthTest}). Four worn count fully.
         */
        public static final java.util.Set<String> WARM_ITEMS = java.util.Set.of(
                "larupia_hat", "graahk_headdress", "kyatt_hat", "pyromancer_hood", "bearhead", "fire_tiara", "lumberjack_hat", "earmuffs",
                "wolf_mask", "slayer_helmet", "slayer_helmet_i", "gnome_scarf", "gloves_of_silence", "fremennik_gloves", "warm_gloves",
                "red_gloves", "yellow_gloves", "teal_gloves", "purple_gloves", "firemaking_cape", "max_cape", "fire_cape", "infernal_cape",
                "obsidian_cape", "wolf_cloak", "staff_of_fire", "fire_battlestaff", "lava_battlestaff", "steam_battlestaff",
                "smoke_battlestaff", "mystic_fire_staff", "mystic_lava_staff", "infernal_axe", "infernal_pickaxe", "infernal_harpoon",
                "ale_of_the_gods", "bruma_torch", "tome_of_fire", "bug_lantern", "polar_camo_top", "wood_camo_top", "jungle_camo_top",
                "desert_camo_top", "larupia_top", "graahk_top", "kyatt_top", "pyromancer_garb", "polar_camo_legs", "wood_camo_legs",
                "jungle_camo_legs", "desert_camo_legs", "larupia_legs", "graahk_legs", "kyatt_legs", "pyromancer_robe",
                "ring_of_the_elements", "pyromancer_boots", "mole_slippers", "bear_feet", "demon_feet", "frog_slippers", "cow_slippers",
                "brutus_slippers");

        /** Warm items among the worn item ids (counted up to four). */
        public static int warmItems(java.util.Collection<String> worn) {
            int n = 0;
            for (String id : worn) {
                if (id != null && WARM_ITEMS.contains(id)) {
                    n++;
                }
            }
            return Math.min(4, n);
        }

        /** Herblore XP for mixing a rejuvenation potion: 0.1 x the Herblore level (wiki). */
        public static double potionXp(int herblore) {
            return herblore * 0.1;
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

        /** Guaranteed reward rolls: 2 at 500 points, +1 per further 500 (max 28). See {@link WintertodtRewards}. */
        public static int rolls(int points) {
            return WintertodtRewards.rolls(points);
        }

        /** Reward rolls including the chance of an extra roll for points between 500-point milestones. */
        public static int rolls(int points, double random) {
            return WintertodtRewards.rolls(points, random);
        }

    }

    // ---------------------------------------------------------------- Tempoross
    public static final class Tempoross {
        /** Points (OSRS Wiki): 5 per harpoonfish caught, 10 per fish cooked, 20 per raw and 65 per cooked fish deposited. */
        public static final int POINTS_CATCH = 5;
        public static final int POINTS_COOK = 10;
        public static final int POINTS_LOAD_RAW = 20;
        public static final int POINTS_LOAD_COOKED = 65;
        /** Surviving a wave (tethered) gives 10 points; repairing a totem/mast or dousing a fire 40. */
        public static final int POINTS_TETHER = 10;
        public static final int POINTS_REPAIR = 40;
        public static final int POINTS_DOUSE = 40;
        /** Points per harpoon strike on the spirit pool. */
        public static final int POINTS_SPIRIT = 55;
        public static final int MIN_POINTS = 2000;
        /** Energy drained per raw / cooked harpoonfish loaded into a cannon (OSRS Wiki; the pools are {@link TemporossStorm}). */
        public static final int RAW_DAMAGE = TemporossStorm.RAW_ENERGY;
        public static final int COOKED_DAMAGE = TemporossStorm.COOKED_ENERGY;
        /** The essence phase: Tempoross stays under while its energy recharges (see {@link TemporossStorm}). */
        public static final int VULNERABLE_TICKS = TemporossStorm.ESSENCE_PHASE_TICKS;
        public static final int WAVE_INTERVAL = 40;
        public static final int WAVE_WARNING = 6;
        /**
         * Tempoross's attacks deal no damage (OSRS Wiki): a wave washes fish and tools (hammer, rope, buckets) away from anyone
         * not tethered, a lightning strike lights fires that spread unless doused with a bucket of water and destroy those
         * resources every 2 ticks while a player stands in them. Attacks stop at 10% energy. Interval, fires per strike,
         * spread time and cap are approx.
         */
        public static final int LIGHTNING_INTERVAL = 30;
        public static final int FIRES_PER_STRIKE = 2;
        public static final int FIRE_SPREAD_TICKS = 15;
        public static final int MAX_FIRES = 12;
        public static final int FIRE_BURN_INTERVAL = 2;
        public static final int ATTACKS_STOP_ENERGY = 10;
        /** Fires to douse in one fight for "Master of Buckets". */
        public static final int MASTER_OF_BUCKETS = 5;
        /**
         * The ordinary Bucket and Bucket of water (the ones shops sell and farming and recipes use): players bring their own,
         * fill them at the water pump and douse fires with them.
         */
        public static final String BUCKET = "minecraft:bucket";
        public static final String WATER_BUCKET = "minecraft:water_bucket";
        /** Items a wave or fire can destroy (OSRS Wiki); nothing else is ever lost. */
        public static final java.util.List<String> LOSABLE = java.util.List.of("raw_harpoonfish", "harpoonfish", "hammer", "rope",
                BUCKET, WATER_BUCKET);

        /** Chance (percent) that a wave breaks a mast or totem pole: 15% rising to 25% with the storm's intensity (0-1). */
        public static int tetherBreakChance(double intensity) {
            return 15 + (int) Math.round(10 * Math.max(0, Math.min(1, intensity)));
        }

        private Tempoross() {
        }

        /** Guaranteed reward permits: 1 at 2,000 points plus one per 700 above. See {@link TemporossRewards}. */
        public static int permits(int points) {
            return TemporossRewards.permits(points);
        }

        public static int permits(int points, double random) {
            return TemporossRewards.permits(points, random);
        }

        /** Fishing XP per harpoonfish caught (level-scaled modifier / 100, OSRS Wiki). */
        public static double catchXp(int fishing) {
            return TemporossRewards.catchXp(fishing);
        }

        public static double cookXp(int cooking) {
            return TemporossStorm.COOK_XP; // a flat 10 at any level (OSRS Wiki "Harpoonfish")
        }
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
        public static final int MIN_ENERGY = GuardiansRewards.MIN_ENERGY;

        private Guardians() {
        }

        /** Extra runes from the Raiments of the Eye: 10% per piece and +20% for the full set (60%). */
        public static double raimentBonus(int pieces) {
            return pieces >= 4 ? 0.60 : pieces * 0.10;
        }

        /** Rewards Guardian searches affordable with stored points (1 elemental + 1 catalytic each). */
        public static int searches(int elementalPoints, int catalyticPoints) {
            return GuardiansRewards.searches(elementalPoints, catalyticPoints);
        }
    }

    // ---------------------------------------------------------------- Barbarian Assault
    public static final class BarbarianAssault {
        public static final String[] ROLES = {"attacker", "defender", "collector", "healer"};
        public static final String[] PENANCE = {"penance_fighter", "penance_ranger", "penance_runner", "penance_healer"};
        public static final int WAVES = BaWaves.WAVES;
        public static final int QUEEN_HP = BaWaves.QUEEN.hitpoints();
        /** Eight omega egg hits kill the Penance Queen. */
        public static final int QUEEN_EGG_HITS = 8;
        public static final int PET_CHANCE = 1000;
        /** Wave 10 pays a flat 80 points in your role and 5 in each of the other three (OSRS Wiki). */
        public static final int WAVE_10_ROLE = 80;
        public static final int WAVE_10_OTHER = 5;
        /** Honour points are capped at 5,000 per role and queen kills at 40. */
        public static final int MAX_HONOUR = 5000;
        public static final int MAX_QUEEN_KILLS = 40;

        /** Role levels run from 1 to 5; each upgrade is bought with that role's honour points. */
        public static final int MAX_ROLE_LEVEL = 5;
        /** Honour points for reaching each level (index = level): 200, 300, 400, 500, 1,400 in all (OSRS Wiki). */
        private static final int[] LEVEL_COST = {0, 0, 200, 300, 400, 500};
        /** Healer: hitpoints restored by a heal at each level (10 / 15 / 20 / 25 / 35). */
        private static final int[] HEALER_HEAL = {0, 10, 15, 20, 25, 35};
        /** Collector: eggs the collection bag holds at each level (2 / 4 / 6 / 7 / 8). */
        private static final int[] COLLECTOR_BAG = {0, 2, 4, 6, 7, 8};

        private BarbarianAssault() {
        }

        private static int clampLevel(int level) {
            return Math.max(1, Math.min(MAX_ROLE_LEVEL, level));
        }

        /** Honour points needed to buy {@code level} (0 for level 1 or past 5). */
        public static int levelCost(int level) {
            return level < 2 || level > MAX_ROLE_LEVEL ? 0 : LEVEL_COST[level];
        }

        /** Honour points spent to reach {@code level} from level 1. */
        public static int totalCost(int level) {
            int sum = 0;
            for (int l = 2; l <= clampLevel(level); l++) {
                sum += LEVEL_COST[l];
            }
            return sum;
        }

        public static int healerHeal(int level) {
            return HEALER_HEAL[clampLevel(level)];
        }

        public static int collectorBag(int level) {
            return COLLECTOR_BAG[clampLevel(level)];
        }

        /** Attacker: +1 to +5 bonus damage. */
        public static int attackerBonus(int level) {
            return clampLevel(level);
        }

        /** Defender: luring range in spaces (4, then 5; the in-game upgrade text beyond level 2 is false). */
        public static int defenderLure(int level) {
            return clampLevel(level) == 1 ? 4 : 5;
        }

        /** Whether every role is at level 5 (the Kandarin elite blackboard task). */
        public static boolean allMaxed(int[] levels) {
            if (levels.length < ROLES.length) {
                return false;
            }
            for (int l : levels) {
                if (l < MAX_ROLE_LEVEL) {
                    return false;
                }
            }
            return true;
        }

        /** Every Penance of each type in a wave (initial + reserve): fighters, rangers, runners, healers (OSRS Wiki). */
        public static int[] wave(int wave) {
            int[] out = new int[BaWaves.TYPES];
            for (int t = 0; t < out.length; t++) {
                out[t] = BaWaves.total(wave, t);
            }
            return out;
        }

        /** Penance hitpoints for a type (0 fighter, 1 ranger, 2 runner, 3 healer) in a wave (OSRS Wiki). */
        public static int hitpoints(int type, int wave) {
            return BaWaves.stats(wave, type).hitpoints();
        }

        /** The queen's hitpoints split over {@link #QUEEN_EGG_HITS} eggs; real hits roll 30-39 ({@link BaRules#omegaEggDamage}). */
        public static int omegaEggDamage() {
            return (QUEEN_HP + QUEEN_EGG_HITS - 1) / QUEEN_EGG_HITS;
        }

        /** Base honour points for clearing a wave in a role with every Penance killed and no mistakes ({@link BaRules#wavePoints}). */
        public static int honour(int role, int wave) {
            return BaRules.wavePoints(role, wave, BaRules.WaveStats.clean(wave), false);
        }

        /**
         * Points lost to escaped runners: 3 per escape for everyone, capped at 10; defenders lose them again (at most 20).
         * There are no penalties on wave 10.
         */
        public static int runnerPenalty(int role, int escapes, int wave) {
            if (wave >= WAVES) {
                return 0;
            }
            int team = Math.min(10, 3 * escapes);
            return role == BaRules.DEFENDER ? 2 * team : team;
        }

        /** Honour for a clean wave except for escaped runners. */
        public static int waveHonour(int role, int wave, int escapes) {
            BaRules.WaveStats c = BaRules.WaveStats.clean(wave);
            return BaRules.wavePoints(role, wave, new BaRules.WaveStats(c.fightersKilled(), c.rangersKilled(), c.runnersKilled(),
                    c.healersKilled(), 0, 0, 0, 0, escapes, 0), false);
        }

        /** Total clean honour points in a role for a full game (all ten waves): 370 / 357 / 278 / 305. */
        public static int fullGameHonour(int role) {
            return BaRules.fullRound(role, false);
        }

        /** Adds honour to a balance, respecting the 5,000 cap. */
        public static int addHonour(int balance, int amount) {
            return Math.min(MAX_HONOUR, balance + amount) - balance;
        }
    }

    // ---------------------------------------------------------------- Castle Wars
    public static final class CastleWars {
        public static final int GAME_TICKS = 2000;
        /** Players must spend at least 15 minutes in a game to earn tickets. */
        public static final int MIN_TICKS_FOR_TICKETS = 1500;

        private CastleWars() {
        }

        /**
         * Tickets at the end of a game on a normal world (OSRS Wiki): a loss or a 0-0 draw pays 1, a
         * scoring draw or a win against a team that scored pays 2, and a shut-out win pays 3.
         */
        public static int tickets(int ownScore, int otherScore) {
            if (ownScore > otherScore) {
                return otherScore == 0 ? 3 : 2;
            }
            if (ownScore == otherScore) {
                return ownScore == 0 ? 1 : 2;
            }
            return 1;
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
        /** Magic needed for each room: Telekinetic Grab 33, Low Level Alchemy 21, Lvl-1 Enchant 7, Bones to Bananas 15. */
        public static final int[] ROOM_MAGIC = {33, 21, 7, 15};
        /** Telekinetic Theatre: 2 points per maze, plus 8 points (and 10 law runes) for every fifth maze in a row. */
        public static final int TELEKINETIC_MAZE = 2;
        public static final int TELEKINETIC_BONUS_EVERY = 5;
        public static final int TELEKINETIC_BONUS = 8;
        public static final int TELEKINETIC_BONUS_LAWS = 10;
        /** Alchemists' Playground: 1 point and 10 coins per 100 coins deposited, plus 2 Magic XP per coin. */
        public static final int ALCHEMY_COINS_PER_POINT = 100;
        public static final int ALCHEMY_COINS_REWARD = 10;
        public static final int ALCHEMY_XP_PER_COIN = 2;
        /** The five cupboard items' alchemy values, rotated every minute. */
        public static final int[] ALCHEMY_VALUES = {1, 5, 8, 15, 30};
        public static final int ALCHEMY_ROTATE_TICKS = 100;
        /** Enchanting Chamber: Lvl-1 to Lvl-6 Enchant (Magic level and XP per cast). */
        public static final int[] ENCHANT_LEVELS = {7, 27, 49, 57, 68, 87};
        public static final double[] ENCHANT_XP = {17.5, 37, 59, 67, 78, 97};
        public static final int ENCHANT_BONUS_SHAPE = 2;
        /** Creature Graveyard: 16 bananas or 8 peaches per point, at most 3 points per deposit. */
        public static final int GRAVEYARD_BANANAS_PER_POINT = 16;
        public static final int GRAVEYARD_PEACHES_PER_POINT = 8;
        public static final int GRAVEYARD_MAX_PER_DEPOSIT = 3;
        /** Falling bones hit for 2; dying in the graveyard costs 10 points. */
        public static final int GRAVEYARD_BONE_DAMAGE = 2;
        public static final int GRAVEYARD_DEATH_PENALTY = 10;
        /** Point caps per room. */
        public static final int[] MAX_POINTS = {8000, 16000, 32000, 8000};

        private MageArena() {
        }

        /** Best enchantment spell (1-6) for a Magic level, or 0. */
        public static int enchantLevel(int magic) {
            int best = 0;
            for (int i = 0; i < ENCHANT_LEVELS.length; i++) {
                if (magic >= ENCHANT_LEVELS[i]) {
                    best = i + 1;
                }
            }
            return best;
        }

        /**
         * Enchantment points for one cast: the spell's level (doubled for a dragonstone), doubled again
         * on every tenth shape, plus 2 for the indicated bonus shape (added after doubling).
         */
        public static int enchantPoints(int spellLevel, int nth, boolean bonusShape, boolean dragonstone) {
            int pts = spellLevel * (dragonstone ? 2 : 1);
            if (nth > 0 && nth % 10 == 0) {
                pts *= 2;
            }
            return pts + (bonusShape ? ENCHANT_BONUS_SHAPE : 0);
        }

        /** Points for depositing coins: one per 100, the remainder giving that percent chance of one more. */
        public static int alchemistPoints(int coins, double random) {
            int pts = coins / ALCHEMY_COINS_PER_POINT;
            return pts + (random < (coins % ALCHEMY_COINS_PER_POINT) / (double) ALCHEMY_COINS_PER_POINT ? 1 : 0);
        }

        /** Graveyard points for one deposit of fruit (fractions give that chance of an extra point; max 3). */
        public static int graveyardPoints(int fruit, boolean peaches, double random) {
            double exact = fruit / (double) (peaches ? GRAVEYARD_PEACHES_PER_POINT : GRAVEYARD_BANANAS_PER_POINT);
            int pts = (int) Math.floor(exact) + (random < exact - Math.floor(exact) ? 1 : 0);
            return Math.min(GRAVEYARD_MAX_PER_DEPOSIT, pts);
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
        /**
         * Recharging the pharaoh's sceptre at the guardian mummy (wiki): a full recharge costs 24 ivory/pottery, 12 stone
         * or 6 gold artefacts; shapes may be mixed within one material. The jewelled golden statuette isn't accepted.
         */
        public static final String[][] SCEPTRE_ARTEFACTS = {{"ivory_comb", "pottery_scarab", "pottery_statuette"},
                {"stone_seal", "stone_scarab", "stone_statuette"}, {"gold_seal", "golden_scarab", "golden_statuette"}};
        public static final int[] SCEPTRE_ARTEFACT_COUNT = {24, 12, 6};

        private PyramidPlunder() {
        }

        /**
         * The artefacts a full sceptre recharge takes from what the player carries (item -> count), the cheapest material
         * that suffices first and the cheapest shapes within it; null if no material has enough.
         */
        public static java.util.Map<String, Integer> sceptreRechargeCost(java.util.Map<String, Integer> carried) {
            for (int m = 0; m < SCEPTRE_ARTEFACTS.length; m++) {
                int need = SCEPTRE_ARTEFACT_COUNT[m];
                int have = 0;
                for (String id : SCEPTRE_ARTEFACTS[m]) {
                    have += carried.getOrDefault(id, 0);
                }
                if (have < need) {
                    continue;
                }
                java.util.Map<String, Integer> take = new java.util.LinkedHashMap<>();
                for (String id : SCEPTRE_ARTEFACTS[m]) {
                    int n = Math.min(need, carried.getOrDefault(id, 0));
                    if (n > 0) {
                        take.put(id, n);
                        need -= n;
                    }
                }
                return take;
            }
            return null;
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
    /**
     * Tithe Farm (OSRS Wiki "Tithe Farm", the seed, plant and fertiliser pages): planting gives no XP; a plant must be
     * watered in each of its three one-minute stages or it dies, then yields one fruit; Gricoller's fertiliser halves the
     * growth time. Fruit go into the sack in batches of 100.
     */
    public static final class TitheFarm {
        /** Farming XP per fruit harvested: golovanova 6, bologano 14, logavano 23 (planting gives none). */
        public static final double[] HARVEST_XP = {6, 14, 23};
        /** Farming XP per fruit deposited (10x harvest XP); fruit 75-100 of a batch pay double. */
        public static final double[] DEPOSIT_XP = {60, 140, 230};
        /** Fruit a sack batch holds; the 75th earns a 250x harvest XP bonus, the 100th 2 extra points. */
        public static final int SACK_SIZE = 100;
        public static final int BONUS_FRUIT = 75;
        public static final int MAX_POINTS = 16_000;
        /** Seeds and fruit a player may hold in one visit, and seeds taken from the table at a time. */
        public static final int MAX_SEEDS = 10_000;
        public static final int SEEDS_PER_TAKE = 100; // approx: the table asks for an amount
        public static final int WATERINGS = 3;
        /** Each growth stage lasts one minute (100 game ticks); a plant is fully grown after three. */
        public static final int GROW_TICKS = 100;
        /** Gricoller's fertiliser halves the growth time. */
        public static final int FERTILISED_GROW_TICKS = 50;
        /** Fruit a fully grown plant yields. */
        public static final int YIELD = 1;
        /** Doses of water: a watering can holds 8, Gricoller's can 1,000. */
        public static final int CAN_DOSES = 8;
        public static final int GRICOLLER_DOSES = 1000;

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

        /** Deposit XP for the n-th fruit (1-based) of a sack batch (the bonus isn't boosted by the farmer's outfit). */
        public static double depositXp(int type, int n) {
            double xp = n >= BONUS_FRUIT ? DEPOSIT_XP[type] * 2 : DEPOSIT_XP[type];
            if (n == BONUS_FRUIT) {
                xp += HARVEST_XP[type] * 250;
            }
            return xp;
        }

        /** The part of {@link #depositXp} that is the 75th fruit's bonus (250x the harvest XP; the farmer's outfit doesn't boost it). */
        public static double bonusXp(int type, int n) {
            return n == BONUS_FRUIT ? HARVEST_XP[type] * 250 : 0;
        }

        /** Points for the n-th fruit (1-based) of a sack batch: one per third fruit, plus 2 on the 100th. */
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
    /**
     * Mahogany Homes (OSRS Wiki "Mahogany Homes"): contracts by tier, the plank each tier uses, carpenter points and the
     * XP per material (the layouts are {@link MahoganyHomesLayouts}).
     */
    public static final class MahoganyHomes {
        public static final String[] TIERS = {"Beginner", "Novice", "Adept", "Expert"};
        public static final String[] TIER_IDS = {"BEGINNER", "NOVICE", "ADEPT", "EXPERT"};
        public static final String[] PLANKS = {"plank", "oak_plank", "teak_plank", "mahogany_plank"};
        public static final int[] LEVELS = {1, 20, 50, 70};
        public static final int[] POINTS = {2, 3, 4, 5};
        /** Construction XP per plank used to build new furniture and per plank used in a repair, by tier. */
        public static final double[] BUILD_XP = {22.5, 48, 72, 112};
        public static final double[] REPAIR_XP = {127.5, 160, 190, 240};
        /** Construction XP for a repair with a steel bar (any tier). */
        public static final double BAR_XP = 120;
        /** Construction XP for completing a contract (the carpenter's outfit boosts it by up to 2.5%). */
        public static final double[] COMPLETION_XP = {500, 1250, 2250, 2750};
        /** The contractors: Amy in Falador, Marlo in Varrock, Ellie in East Ardougne, Angelo in Hosidius. */
        public static final String[] CONTRACTORS = {"Amy", "Marlo", "Ellie", "Angelo"};

        /** Supply crate (25 carpenter points): one roll of the wiki's 144-weight table of noted materials. */
        public static final String SUPPLY_CRATE = "#mh_supply_crate";
        public static final List<Loot> SUPPLY_CRATE_TABLE = List.of(
                new Loot("bolt_of_cloth", 9, 9, 5), new Loot("bolt_of_cloth", 10, 10, 1),
                new Loot("limestone_brick", 9, 9, 5), new Loot("limestone_brick", 10, 10, 1),
                new Loot("mahogany_plank", 6, 6, 15), new Loot("mahogany_plank", 7, 7, 3),
                new Loot("oak_plank", 28, 28, 10), new Loot("oak_plank", 29, 29, 15), new Loot("oak_plank", 30, 30, 5),
                new Loot("soft_clay", 45, 45, 5), new Loot("soft_clay", 46, 46, 10), new Loot("soft_clay", 47, 47, 10),
                new Loot("soft_clay", 48, 48, 5),
                new Loot("steel_bar", 23, 23, 5), new Loot("steel_bar", 24, 24, 20), new Loot("steel_bar", 25, 25, 5),
                new Loot("teak_plank", 15, 15, 20), new Loot("teak_plank", 16, 16, 4));
        /** Planks a plank sack holds (any mix of types). */
        public static final int PLANK_SACK = 28;

        /** XP per material of a hotspot at a tier. */
        public static double hotspotXp(int tier, boolean steelBar, boolean repair) {
            return steelBar ? BAR_XP : repair ? REPAIR_XP[tier] : BUILD_XP[tier];
        }

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

    // ---------------------------------------------------------------- Fishing Trawler
    /**
     * Fishing Trawler (OSRS Wiki "Fishing Trawler"): a five-minute trawl; contribution points for bailing (2), patching a
     * leak (5, 5 Construction XP), mending the net with a rope (7, 5 Crafting XP), chopping the kraken's tentacle (10, 5
     * Woodcutting XP) and fixing a rail it broke (10, 5 Construction XP); 50 points earn a share of the catch, at most 255
     * count. Fish are rolled from the best the player's Fishing level allows down (each tier 150/256 at its level to
     * 200/256 at 99); 1/12 for a missing angler piece; the heron at 1/5,000 to 1/2,500 with a full meter.
     */
    public static final class Trawler {
        public static final int GAME_TICKS = 500;
        public static final int MAX_WATER = 100;
        public static final String[] OUTFIT = {"angler_hat", "angler_top", "angler_waders", "angler_boots"};
        public static final int MIN_CONTRIBUTION = 50;
        public static final int MAX_CONTRIBUTION = 255;
        public static final int BAIL_POINTS = 2;
        public static final int LEAK_POINTS = 5;
        public static final int NET_POINTS = 7;
        public static final int CHOP_POINTS = 10;
        public static final int RAIL_POINTS = 10;
        /** XP for a patched leak or fixed rail (Construction), a mended net (Crafting) and a tentacle chop (Woodcutting). */
        public static final double ACTION_XP = 5;
        /** Fishing level needed for any reward. */
        public static final int REWARD_LEVEL = 15;
        /** 1/12 for an angler piece the player doesn't have yet (a second set only after a full first one). */
        public static final int ANGLER_ROLL = 12;
        /** Game ticks between leaks for a lone player; more players spring more leaks (approx: busy worlds every 5 ticks). */
        public static final int LEAK_INTERVAL = 18;
        /** Game ticks between kraken tentacles ("about 45 seconds"), and successful chops that drive one off (approx). */
        public static final int TENTACLE_INTERVAL = 75;
        public static final int TENTACLE_HP = 2; // approx
        /** Game ticks between net tears (approx). */
        public static final int NET_TEAR_INTERVAL = 90; // approx

        /** A catch: item, Fishing level and Fishing XP per fish (wiki table). */
        public record Catch(String item, int level, double xp) {
        }

        /** Best first, as the net rolls them (the last always succeeds). */
        public static final List<Catch> CATCHES = List.of(new Catch("raw_manta_ray", 81, 69), new Catch("raw_sea_turtle", 79, 57),
                new Catch("raw_shark", 76, 66), new Catch("raw_swordfish", 50, 60), new Catch("raw_lobster", 40, 54),
                new Catch("raw_tuna", 35, 48), new Catch("oyster", 16, 51), new Catch("raw_anchovies", 15, 0.7),
                new Catch("raw_sardine", 15, 0.7), new Catch("raw_shrimps", 15, 0.7));
        /** Junk the net brings up: 0-2 items, each equally likely (wiki: about 37% one, 16% two). */
        public static final List<String> JUNK = List.of("broken_arrow", "broken_glass", "broken_staff", "buttons", "damaged_armour",
                "old_boot", "empty_pot", "rusty_sword");
        public static final double JUNK_XP = 0.5;

        private Trawler() {
        }

        /** The chance a fish tier is caught at a Fishing level: 150/256 at level 1 to 200/256 at 99 (OSRS skilling roll). */
        public static double tierChance(int level) {
            int l = Math.max(1, Math.min(99, level));
            return (1 + Math.floor(150 * (99 - l) / 98.0 + 200 * (l - 1) / 98.0 + 0.5)) / 256.0;
        }

        /** One fish for a Fishing level, given uniform random values in [0, 1) per tier tried. */
        public static Catch fish(int level, java.util.function.DoubleSupplier random) {
            double p = tierChance(level);
            for (int i = 0; i < CATCHES.size(); i++) {
                Catch c = CATCHES.get(i);
                if (c.level() > level) {
                    continue;
                }
                if (i == CATCHES.size() - 1 || random.getAsDouble() < p) {
                    return c;
                }
            }
            return CATCHES.get(CATCHES.size() - 1);
        }

        /** The best fish a Fishing level can get from the net. */
        public static String fishFor(int fishing) {
            for (Catch c : CATCHES) {
                if (fishing >= c.level()) {
                    return c.item();
                }
            }
            return CATCHES.get(CATCHES.size() - 1).item();
        }

        /**
         * A player's share of the catch: about 10.5 fish alone and 7 in a crowd at the minimum contribution, doubled by a
         * full meter (wiki: 20-22 solo, 13-15 on busy worlds), less the time the net was torn and half the time a tentacle
         * slowed the boat (approx).
         */
        public static int fishCount(int players, int contribution, double netIntact, double tentacleTime) {
            if (contribution < MIN_CONTRIBUTION) {
                return 0;
            }
            double base = 7 + 3.5 / Math.max(1, players); // approx
            double meter = 1 + (Math.min(MAX_CONTRIBUTION, contribution) - MIN_CONTRIBUTION) / (double) (MAX_CONTRIBUTION - MIN_CONTRIBUTION);
            double time = Math.max(0, Math.min(1, netIntact) - 0.5 * Math.max(0, Math.min(1, tentacleTime)));
            return (int) Math.floor(base * meter * time);
        }

        /** Junk items in the net for a uniform random value in [0, 1): none 47%, one 37%, two 16%. */
        public static int junkCount(double roll) {
            return roll < 0.47 ? 0 : roll < 0.84 ? 1 : 2;
        }

        /** The heron's chance: 1/5,000 at 50 points, scaling to 1/2,500 with a full meter. */
        public static double heronChance(int contribution) {
            if (contribution < MIN_CONTRIBUTION) {
                return 0;
            }
            double f = (Math.min(MAX_CONTRIBUTION, contribution) - MIN_CONTRIBUTION) / (double) (MAX_CONTRIBUTION - MIN_CONTRIBUTION);
            return 1.0 / (5000 - 2500 * f);
        }

        /** Game ticks between leaks with this many players aboard (approx). */
        public static int leakInterval(int players) {
            return Math.max(5, LEAK_INTERVAL - 2 * (Math.max(1, players) - 1));
        }
    }

    // ---------------------------------------------------------------- Rogues' Den
    public static final class RoguesDen {
        public static final String[] OBSTACLES = {"Contortion bars", "Pendulum", "Floor spikes", "Wall safe trap",
                "Grill", "Ledge", "Blade trap", "Tile maze"};
        public static final String[] OUTFIT = {"rogue_mask", "rogue_top", "rogue_trousers", "rogue_gloves", "rogue_boots"};
        /** Whether each obstacle is an Agility obstacle (else a Thieving trap). */
        public static final boolean[] AGILITY = {true, true, false, false, false, true, true, false};
        /**
         * Levels drained by each obstacle (OSRS Wiki: contortion bars 5, pendulum 3, ledge 4, floor
         * blades 3; the Thieving traps' drains are approx.).
         */
        public static final int[] DRAIN = {5, 3, 2, 2, 2, 4, 3, 2};
        /** Both 50 Agility and 50 Thieving (unboostable) are needed to enter the maze. */
        public static final int LEVEL = 50;
        /** On cracking the safe: 12 XP in Agility and Thieving for every level drained from that skill. */
        public static final int XP_PER_DRAINED_LEVEL = 12;

        private RoguesDen() {
        }

        /** Chance to disarm a Thieving trap (Agility obstacles never fail while levels remain): 60% at 50 to 98% at 99 (approx.). */
        public static double passChance(int level) {
            return Math.min(0.98, 0.60 + Math.max(0, level - 50) * 0.0078);
        }

        /**
         * Experience on leaving the maze for the levels drained from a skill: the full 12 per level
         * after cracking the safe, otherwise a share proportional to how far into the maze the
         * player got (approx. of the wiki's "fraction depending on distance").
         */
        public static double xp(int drained, int obstaclesPassed, boolean cracked) {
            double full = drained * (double) XP_PER_DRAINED_LEVEL;
            return cracked ? full : full * obstaclesPassed / (double) (OBSTACLES.length + 1);
        }
    }

    // ---------------------------------------------------------------- Trouble Brewing
    /**
     * Trouble Brewing (OSRS Wiki "Trouble Brewing", after the January 2025 XP rescale): a bottle of rum takes 1 bitternut,
     * 1 sweetgrubs, 1 scrapey bark, 5 buckets of water and 3 bowls of the team's coloured water from the hopper, and three
     * lit boilers with at least 3 logs each; it brews in 38 seconds and must be packed in the crate by someone whose
     * ingredients went into it. Each resource deposited is 1 contribution (pieces of eight: 10 per team bottle plus the
     * contribution, at most 100); each non-water ingredient also earns a rum reward of 100 Crafting, 250 Cooking and 150
     * Firemaking XP at the end (a third for each bowl of coloured water).
     */
    public static final class TroubleBrewing {
        /** A game lasts 20 minutes. */
        public static final int GAME_TICKS = 2000;
        /** 10 pieces of eight per bottle of rum the team makes, plus 1 per resource deposited (max 100). */
        public static final int EIGHT_PER_RUM = 10;
        public static final int MAX_CONTRIBUTION = 100;
        /** Cooking XP for depositing a bitternut (200), sweetgrubs, scrapey bark or a bowl of coloured water (100); water none. */
        public static final double BITTERNUT_XP = 200;
        public static final double DEPOSIT_XP = 100;
        /** Rum reward per credit: Crafting 100, Cooking 250, Firemaking 150. */
        public static final double RUM_CRAFTING_XP = 100;
        public static final double RUM_COOKING_XP = 250;
        public static final double RUM_FIREMAKING_XP = 150;
        /** A bottle takes 38 seconds to brew (64 game ticks); unpacked, it breaks about 34 seconds after it arrives. */
        public static final int BREW_TICKS = 64;
        public static final int BOTTLE_BREAK_TICKS = 57;
        /** One bottle's ingredients. */
        public static final int WATER_PER_RUM = 5;
        public static final int COLOURED_PER_RUM = 3;
        /** Boilers: three, each needs 3 logs (holds 10) and to be lit; the log store starts with 75 logs. */
        public static final int BOILERS = 3;
        public static final int BOILER_MIN_LOGS = 3;
        public static final int BOILER_MAX_LOGS = 10;
        public static final int LOG_STORE = 75;
        /** A lit boiler burns a log this often, rum or no rum (approx). */
        public static final int LOG_BURN_TICKS = 50; // approx
        /** XP for obtaining resources: a bitternut 50 Hunter, a caught monkey 15 Agility and 15 Hunter, sweetgrubs 40 Hunter,
         * scrapey bark 50 Fletching; lighting a boiler 100 Firemaking. */
        public static final double BITTERNUT_HUNTER_XP = 50;
        public static final double MONKEY_XP = 15;
        public static final double SWEETGRUB_HUNTER_XP = 40;
        public static final double BARK_FLETCHING_XP = 50;
        public static final double LIGHT_FIREMAKING_XP = 100;
        /** At most 2 bitternuts and 5 flowers carried; the kettle holds 10 flowers. */
        public static final int MAX_NUTS = 2;
        public static final int MAX_FLOWERS = 5;
        public static final int KETTLE_MAX = 10;
        /** Using raw meat on a sweetgrub mound: 1/5 it collapses, else 1/5 a swarm of sweetflies, else sweetgrubs. */
        public static final int GRUB_ROLL = 5;
        /** Fletching scrapey bark: 1/50 to catch a disease (4 damage). */
        public static final int DISEASE_ROLL = 50;
        public static final int DISEASE_DAMAGE = 4;

        /** Rum credits, in thirds, an ingredient earns its depositor: 3 for a solid ingredient, 1 per bowl of coloured water. */
        public static int rumThirds(String ingredient) {
            return switch (ingredient) {
                case "bitternut", "sweetgrubs", "scrapey_tree_bark" -> 3;
                case "bowl_of_red_water", "bowl_of_blue_water" -> 1;
                default -> 0;
            };
        }

        private TroubleBrewing() {
        }

        public static int piecesOfEight(int teamRum, int resourcesDeposited) {
            return EIGHT_PER_RUM * teamRum + Math.min(MAX_CONTRIBUTION, resourcesDeposited);
        }
    }

    // ---------------------------------------------------------------- Gnome Restaurant
    /**
     * Gnome Restaurant (OSRS Wiki "Gnome Restaurant"): Gianne jnr. hands out easy deliveries (around the Gnome Stronghold, 6
     * minutes, 1 point) and hard ones (anywhere, 11 minutes, 3 points; declining one means 5.5 minutes before the next).
     * Every 12 points make a reward token worth a random gnome dish (at most 10 stored). An easy customer tips 2-500 coins;
     * a hard customer tips an item from the hard table, plus the specials only some customers give (approx weights from the
     * Drop Log Project's 3,258 deliveries). Deliveries give no XP: the Cooking XP is in making the food.
     */
    public static final class GnomeRestaurant {
        public static final int EASY_TICKS = 600;
        public static final int HARD_TICKS = 1100;
        public static final int EASY_POINTS = 1;
        public static final int HARD_POINTS = 3;
        public static final int POINTS_PER_TOKEN = 12;
        /** A token stores up to 120 points: ten dishes. */
        public static final int MAX_POINTS = 120;
        /** Declining a hard job: no hard job for 5 minutes 30 seconds. */
        public static final int HARD_DECLINE_TICKS = 550;
        /** Easy tips: 2-500 coins. */
        public static final int TIP_MIN = 2;
        public static final int TIP_MAX = 500;
        /** Reward-shop id of a reward token's random dish. */
        public static final String RANDOM_DISH = "#gnome_food";

        /** Easy-order customers (in and around the Tree Gnome Stronghold). */
        public static final List<String> EASY_CUSTOMERS = List.of("Burkor", "Brimstail", "Captain Errdo", "Gnome Coach", "Dalila",
                "Damwin", "Eebel", "Ermin", "Femi", "Froono", "Guard Vemmeldo", "Gulluck", "Heckel Funch", "King Narnode Shareen",
                "Meegle", "Perrdur", "Rometti", "Sarble", "Trainer Nacklepen", "Wurbel");
        /** Hard-order customers. */
        public static final List<String> HARD_CUSTOMERS = List.of("Ambassador Ferrnook", "Ambassador Gimblewap", "Ambassador Spanfipple",
                "Brambickle", "Captain Bleemadge", "Captain Daerkin", "Captain Dalbur", "Captain Klemfoodle", "Captain Ninto",
                "G.L.O. Caranock", "Garkor", "Gnormadium Avlafrim", "Hazelmere", "King Bolren", "Lieutenant Schepbur", "Penwie",
                "Professor Imblewyn", "Professor Manglethorp", "Professor Onglewip", "Wingstone");

        /** A tip only some hard customers give: item, amount, chance out of 512, and who gives it. */
        public record Special(String item, int min, int max, double chance512, List<String> customers) {
        }

        private static final List<String> PILOTS_EXPLORERS_PROFESSORS = List.of("Captain Bleemadge", "Captain Daerkin", "Captain Dalbur",
                "Captain Klemfoodle", "Captain Ninto", "Gnormadium Avlafrim", "Lieutenant Schepbur", "Wingstone", "Penwie",
                "Professor Imblewyn", "Professor Manglethorp", "Professor Onglewip");
        private static final List<String> RUNE_PROFESSORS = List.of("Professor Imblewyn", "Professor Onglewip");

        public static final List<Special> SPECIALS = List.of(
                new Special("gnome_goggles", 1, 1, 23 * 512 / 384.0, List.of("Captain Ninto", "Captain Daerkin")),
                new Special("gnome_scarf", 1, 1, 23 * 512 / 384.0, List.of("Captain Ninto", "Captain Daerkin")),
                new Special("grand_seed_pod", 5, 5, 23, List.of("Wingstone", "Penwie", "Brambickle", "Professor Manglethorp")),
                new Special("mint_cake", 1, 1, 23, List.of("Wingstone", "Penwie", "Brambickle")),
                new Special("goutweed", 1, 1, 23, List.of("Brambickle")),
                new Special("raw_oomlie", 1, 1, 23, List.of("Penwie")),
                new Special("snake_charm", 1, 1, 23, List.of("Wingstone")),
                new Special("loop_half_of_key", 1, 1, 23 * 512 / 640.0, PILOTS_EXPLORERS_PROFESSORS),
                new Special("tooth_half_of_key", 1, 1, 23 * 512 / 640.0, PILOTS_EXPLORERS_PROFESSORS),
                new Special("gold_ore", 6, 11, 23, List.of("Professor Manglethorp")),
                new Special("coal", 12, 22, 23, List.of("Professor Manglethorp")),
                // "Rare" on the wiki: approx 8/512 each.
                new Special("law_rune", 7, 18, 8, RUNE_PROFESSORS), new Special("nature_rune", 9, 15, 8, RUNE_PROFESSORS),
                new Special("chaos_rune", 15, 27, 8, RUNE_PROFESSORS), new Special("cosmic_rune", 10, 19, 8, RUNE_PROFESSORS),
                new Special("death_rune", 7, 13, 8, RUNE_PROFESSORS), new Special("blood_rune", 6, 10, 8, RUNE_PROFESSORS),
                new Special("soul_rune", 5, 11, 8, RUNE_PROFESSORS), new Special("pure_essence", 51, 97, 8, RUNE_PROFESSORS));

        /** Every hard customer's table (the specials aside): x/128 from the wiki, bolts filling the rest (approx split). */
        public static final List<Loot> HARD_TIPS = List.of(
                new Loot("gnomeball", 1, 1, 40), new Loot("toadflax", 3, 3, 40), new Loot("snapdragon", 1, 1, 40),
                new Loot("uncut_sapphire", 6, 16, 40), new Loot("uncut_emerald", 3, 5, 40), new Loot("uncut_ruby", 2, 4, 40),
                new Loot("uncut_diamond", 1, 1, 40), new Loot("uncut_jade", 2, 3, 40), new Loot("uncut_opal", 3, 3, 40),
                new Loot("uncut_red_topaz", 1, 1, 40), new Loot("bird_nest_seeds", 1, 1, 20),
                new Loot("mithril_bolts", 16, 35, 60), new Loot("adamant_bolts", 12, 26, 24), new Loot("rune_bolts", 7, 15, 8));

        /** The specials a hard customer can give. */
        public static List<Special> specials(String customer) {
            return SPECIALS.stream().filter(sp -> sp.customers().contains(customer)).toList();
        }

        /**
         * A hard customer's tip for uniform random values: each of the customer's specials at its chance first, else the
         * base table.
         */
        public static Loot hardTip(String customer, java.util.function.DoubleSupplier random) {
            for (Special sp : specials(customer)) {
                if (random.getAsDouble() < sp.chance512() / 512.0) {
                    return new Loot(sp.item(), sp.min(), sp.max(), 1);
                }
            }
            return pick(HARD_TIPS, random.getAsDouble());
        }

        /** Cooking XP for making each restaurant dish from scratch (OSRS Wiki "Gnome cooking": baking plus toppings). */
        public static double dishXp(String dish) {
            return switch (dish) {
                case "fruit_batta" -> 150;
                case "toad_batta" -> 152;
                case "worm_hole" -> 170;
                case "veg_ball" -> 175;
                case "tangled_toads_legs" -> 185;
                case "chocolate_bomb" -> 190;
                default -> 0;
            };
        }

        /** Cooking level for each restaurant dish (orders don't check it; making the food does). */
        public static int dishLevel(String dish) {
            return switch (dish) {
                case "fruit_batta" -> 25;
                case "toad_batta" -> 26;
                case "worm_hole" -> 30;
                case "veg_ball" -> 35;
                case "tangled_toads_legs" -> 40;
                case "chocolate_bomb" -> 42;
                default -> 1;
            };
        }

        private GnomeRestaurant() {
        }

        /** Reward tokens earned when a delivery takes the point total from {@code before} to {@code after}. */
        public static int tokens(int before, int after) {
            return after / POINTS_PER_TOKEN - before / POINTS_PER_TOKEN;
        }
    }

    // ---------------------------------------------------------------- Imbues (Nightmare Zone / Soul Wars)
    public static final class Imbues {
        /**
         * Item, imbued item, Nightmare Zone points, Soul Wars zeal (OSRS Wiki). Every recoloured slayer helmet imbues for the
         * plain helmet's price; those rows come last ({@link #withSlayerHelmets}).
         */
        public static final Object[][] ITEMS = withSlayerHelmets(new Object[][]{
                {"black_mask", "black_mask_i", 1_250_000, 500},
                {"slayer_helmet", "slayer_helmet_i", 1_250_000, 500},
                {"salve_amulet", "salve_amulet_i", 800_000, 320},
                {"salve_amulet_e", "salve_amulet_ei", 800_000, 320},
                {"ring_of_the_gods", "ring_of_the_gods_i", 650_000, 260},
                {"ring_of_suffering", "ring_of_suffering_i", 725_000, 300},
                {"berserker_ring", "berserker_ring_i", 650_000, 260},
                {"warrior_ring", "warrior_ring_i", 650_000, 260},
                {"archers_ring", "archers_ring_i", 650_000, 260},
                {"seers_ring", "seers_ring_i", 650_000, 260},
                {"tyrannical_ring", "tyrannical_ring_i", 650_000, 260},
                {"treasonous_ring", "treasonous_ring_i", 650_000, 260},
                {"granite_ring", "granite_ring_i", 500_000, 200}});

        private Imbues() {
        }

        /** {@code base} plus one row per recoloured slayer helmet (black, green ... TzKal), priced like the plain helmet. */
        private static Object[][] withSlayerHelmets(Object[][] base) {
            List<String> plain = com.runecraft.osrs.content.slayer.SlayerRewards.slayerHelmets(false);
            List<String> imbued = com.runecraft.osrs.content.slayer.SlayerRewards.slayerHelmets(true);
            List<Object[]> rows = new java.util.ArrayList<>(List.of(base));
            for (int i = 0; i < plain.size(); i++) {
                if (!plain.get(i).equals("slayer_helmet")) {
                    rows.add(new Object[]{plain.get(i), imbued.get(i), 1_250_000, 500});
                }
            }
            return rows.toArray(new Object[0][]);
        }

        /** A tool that must be carried (not used up) to imbue {@code item}, or null: the ring of the gods needs a holy wrench. */
        public static String tool(String item) {
            return "ring_of_the_gods".equals(item) ? "holy_wrench" : null;
        }
    }

    // ---------------------------------------------------------------- Nightmare Zone
    public static final class NightmareZone {
        /** Scroll of redirection (775 points): turns a Teleport to house tablet into one for a house-portal town. */
        public static final String SCROLL_OF_REDIRECTION = "scroll_of_redirection";
        public static final String HOUSE_TABLET = "teleport_to_house";

        /** A redirection: the tablet made, its town, the Construction level to move a house there and any quest needed. */
        public record Redirect(String tablet, String town, int construction, String quest) {
        }

        /**
         * OSRS Wiki "Scroll of redirection": Rimmington, Taverley (10), Pollnivneach (20), Hosidius (25), Rellekka (30),
         * Brimhaven (40), Yanille (50), Trollheim (Eadgar's Ruse) and Prifddinas (Song of the Elves, 70). Aldarin (35, Children
         * of the Sun) is left out: no tablet tile in the transport snapshot. approx
         */
        public static final List<Redirect> REDIRECTS = List.of(
                new Redirect("rimmington_teleport_tablet", "Rimmington", 1, null),
                new Redirect("taverley_teleport_tablet", "Taverley", 10, null),
                new Redirect("pollnivneach_teleport_tablet", "Pollnivneach", 20, null),
                new Redirect("hosidius_teleport_tablet", "Hosidius", 25, null),
                new Redirect("rellekka_teleport_tablet", "Rellekka", 30, null),
                new Redirect("brimhaven_teleport_tablet", "Brimhaven", 40, null),
                new Redirect("yanille_teleport_tablet", "Yanille", 50, null),
                new Redirect("trollheim_teleport_tablet", "Trollheim", 1, "eadgars_ruse"),
                new Redirect("prifddinas_teleport_tablet", "Prifddinas", 70, "song_of_the_elves"));

        public static Redirect redirect(String town) {
            for (Redirect r : REDIRECTS) {
                if (r.town().equalsIgnoreCase(town) || r.tablet().equals(town)) {
                    return r;
                }
            }
            return null;
        }

        /** Coins per dream from Dominic's coffer: normal / hard rumble (OSRS Wiki). */
        public static final int RUMBLE_COST = 2000;
        public static final int HARD_RUMBLE_COST = 6000;
        /** Five of these boss quests must be complete to enter (the wiki's list, limited to quests RuneCraft has). */
        public static final List<String> QUESTS = List.of("the_ascent_of_arceuus", "VAMPYRE_SLAYER", "the_corsair_curse",
                "the_depths_of_despair", "tale_of_the_righteous", "what_lies_below", "witchs_house", "mountain_daughter",
                "lunar_diplomacy", "getting_ahead", "dragon_slayer_i", "the_grand_tree", "haunted_mine", "shilo_village",
                "in_search_of_the_myreque", "contact", "fairytale_i_growing_pains", "tree_gnome_village", "troll_romance",
                "holy_grail", "the_fremennik_isles", "lost_city", "fight_arena", "legends_quest", "dream_mentor",
                "recipe_for_disaster", "desert_treasure_i", "troll_stronghold", "grim_tales", "family_crest", "horror_from_the_deep",
                "monkey_madness_i", "my_arms_big_adventure", "one_small_favour", "roving_elves", "shadow_of_the_storm",
                "song_of_the_elves", "the_great_brain_robbery");
        public static final int QUESTS_NEEDED = 5;
        /** At most four bosses at once for a solo rumble. */
        public static final int MAX_BOSSES = 4;
        /**
         * The rumble's quest bosses: creature id, points per kill in a normal and in a hard rumble with every boss
         * enabled, and hitpoints in a hard rumble (OSRS Wiki boss table; hard bosses keep their Defence).
         */
        public static final Object[][] BOSSES = {
                {"nmz_count_draynor", 224, 7135, 210}, {"nmz_king_roald", 448, 8056, 150}, {"nmz_the_kendal", 998, 10127, 150},
                {"nmz_me", 1263, 9207, 135}, {"nmz_elvarg", 1385, 10357, 240}, {"nmz_tree_spirit", 2078, 8976, 187},
                {"nmz_dad", 2078, 9207, 240}, {"nmz_khazard_warlord", 2547, 8286, 256}, {"nmz_arrg", 2588, 10127, 255},
                {"nmz_black_knight_titan", 2934, 10127, 255}, {"nmz_bouncer", 3811, 13580, 232}, {"nmz_glod", 3872, 17493, 255}};

        /** The quest that unlocks each rumble boss (OSRS Wiki boss table). */
        public static final java.util.Map<String, String> BOSS_QUEST = java.util.Map.ofEntries(
                java.util.Map.entry("nmz_count_draynor", "VAMPYRE_SLAYER"), java.util.Map.entry("nmz_king_roald", "what_lies_below"),
                java.util.Map.entry("nmz_the_kendal", "mountain_daughter"), java.util.Map.entry("nmz_me", "lunar_diplomacy"),
                java.util.Map.entry("nmz_elvarg", "dragon_slayer_i"), java.util.Map.entry("nmz_tree_spirit", "lost_city"),
                java.util.Map.entry("nmz_dad", "troll_stronghold"), java.util.Map.entry("nmz_khazard_warlord", "tree_gnome_village"),
                java.util.Map.entry("nmz_arrg", "troll_romance"), java.util.Map.entry("nmz_black_knight_titan", "holy_grail"),
                java.util.Map.entry("nmz_bouncer", "fight_arena"), java.util.Map.entry("nmz_glod", "grim_tales"));

        /** Dream modes and their fee in coins, normal / hard (OSRS Wiki "Nightmare Zone", cost per dream). */
        public enum Mode {
            /** One chosen boss, 1-on-1, until one of you dies: no points (drops still fall). */
            PRACTICE("practice", "Practice", 0, 0),
            /** Every unlocked boss in turn, 1-on-1, until the player dies or beats them all. */
            ENDURANCE("endurance", "Endurance", 1000, 5000),
            /** Up to four bosses at once in a random order with duplicates, endlessly. */
            RUMBLE("rumble", "Rumble", RUMBLE_COST, HARD_RUMBLE_COST),
            /**
             * A rumble with chosen bosses switched off (fewer bosses pay fewer points). The 12,000 / 16,000 fee with maximum
             * quest points is not modelled (RuneCraft's quest point total differs).
             */
            CUSTOM_RUMBLE("custom", "Customisable rumble", 22000, 26000);

            public final String id;
            public final String displayName;
            private final int cost;
            private final int hardCost;

            Mode(String id, String displayName, int cost, int hardCost) {
                this.id = id;
                this.displayName = displayName;
                this.cost = cost;
                this.hardCost = hardCost;
            }

            public int cost(boolean hard) {
                return hard ? hardCost : cost;
            }

            public boolean rumble() {
                return this == RUMBLE || this == CUSTOM_RUMBLE;
            }

            public static Mode byId(String id) {
                for (Mode m : values()) {
                    if (m.id.equals(id)) {
                        return m;
                    }
                }
                return null;
            }
        }

        /** A dream as encoded in a session variant: {@code nmz:<mode>:<normal|hard>[:<practice boss>]}. */
        public record Dream(Mode mode, boolean hard, String boss) {
            public String variant() {
                return "nmz:" + mode.id + ":" + (hard ? "hard" : "normal") + (boss == null ? "" : ":" + boss);
            }

            /** Parses a variant; the entrance blocks ("nightmare_zone_entrance", "..._hard_entrance") start rumbles. */
            public static Dream parse(String variant) {
                String[] f = variant.split(":");
                if (f.length >= 3 && f[0].equals("nmz") && Mode.byId(f[1]) != null) {
                    return new Dream(Mode.byId(f[1]), f[2].equals("hard"), f.length > 3 ? f[3] : null);
                }
                return new Dream(Mode.RUMBLE, variant.contains("hard"), null);
            }
        }

        public static boolean isBoss(String id) {
            return BOSS_QUEST.containsKey(id);
        }

        /**
         * The bosses a player can meet: those whose quest is complete, in the wiki table's order. Empty when none is: dreams
         * only use bosses "they have previously defeated during completed quests" (OSRS Wiki).
         */
        public static List<String> unlocked(java.util.function.Predicate<String> questComplete) {
            List<String> out = new java.util.ArrayList<>();
            for (Object[] b : BOSSES) {
                if (questComplete.test(BOSS_QUEST.get((String) b[0]))) {
                    out.add((String) b[0]);
                }
            }
            return out;
        }

        /** Every boss, in the wiki table's order (the self-test's dreams). */
        public static List<String> allBosses() {
            return unlocked(q -> true);
        }

        /**
         * Points for a kill when only {@code included} of the rumble's bosses are in the dream: the wiki's values are the
         * maximum, with every boss enabled, and "point values increase based on the number of bosses included".
         * Linear in the share of bosses included. // approx: the exact scaling isn't documented
         */
        public static int points(String boss, boolean hard, int included) {
            int max = points(boss, hard);
            int all = BOSSES.length;
            return (int) ((long) max * Math.max(0, Math.min(included, all)) / all);
        }

        /**
         * Hard-mode offence per boss (OSRS Wiki infoboxes, normal / hard): attack level, max hit of the melee (or main)
         * attack, and ranged and magic levels. Defence and defensive bonuses stay the same in hard mode.
         */
        public record HardStats(String boss, int attack, int hardAttack, int maxHit, int hardMaxHit, int ranged, int hardRanged,
                                int magic, int hardMagic) {
        }

        public static final List<HardStats> HARD_STATS = List.of(
                new HardStats("nmz_count_draynor", 30, 198, 3, 17, 1, 1, 1, 1),
                new HardStats("nmz_king_roald", 35, 140, 3, 9, 1, 1, 1, 1),
                new HardStats("nmz_the_kendal", 65, 195, 9, 24, 1, 1, 1, 1),
                new HardStats("nmz_me", 75, 225, 9, 24, 1, 1, 1, 1),
                new HardStats("nmz_elvarg", 70, 210, 8, 22, 1, 1, 70, 210),
                new HardStats("nmz_tree_spirit", 90, 200, 10, 22, 1, 1, 1, 1),
                new HardStats("nmz_dad", 60, 132, 27, 57, 1, 1, 1, 1),
                new HardStats("nmz_khazard_warlord", 75, 165, 10, 18, 1, 1, 1, 1),
                new HardStats("nmz_arrg", 70, 140, 38, 74, 70, 140, 1, 1),
                new HardStats("nmz_black_knight_titan", 91, 182, 15, 28, 1, 1, 1, 1),
                new HardStats("nmz_bouncer", 120, 240, 13, 25, 1, 1, 1, 1),
                new HardStats("nmz_glod", 115, 230, 13, 25, 1, 1, 1, 1));

        public static HardStats hardStats(String boss) {
            for (HardStats h : HARD_STATS) {
                if (h.boss().equals(boss)) {
                    return h;
                }
            }
            return null;
        }

        /**
         * A hard boss's attack roll factor for a style: OSRS NPC attack rolls are (level + 9) * (bonus + 64), and hard mode
         * raises only the level, so the roll scales by (hard + 9) / (normal + 9).
         */
        public static double hardAccuracy(String boss, boolean melee, boolean ranged) {
            HardStats h = hardStats(boss);
            if (h == null) {
                return 1.0;
            }
            int from = melee ? h.attack() : ranged ? h.ranged() : h.magic();
            int to = melee ? h.hardAttack() : ranged ? h.hardRanged() : h.hardMagic();
            return (to + 9) / (double) (from + 9);
        }

        /** Extra max hit of a hard boss's melee attack (its ranged and dragonfire max hits don't change). */
        public static int hardMaxHitBonus(String boss) {
            HardStats h = hardStats(boss);
            return h == null ? 0 : Math.max(0, h.hardMaxHit() - h.maxHit());
        }

        /** Each potion barrel holds up to 255 doses of its potion (OSRS Wiki). */
        public static final int BARREL_CAPACITY = 255;

        /** Doses that fit in a barrel holding {@code stored}, of the {@code wanted} being bought. */
        public static int barrelRoom(int stored, int wanted) {
            return Math.max(0, Math.min(wanted, BARREL_CAPACITY - Math.max(0, stored)));
        }

        /** The zapper only refuses the killing blow on Count Draynor, and only while the player hasn't hit him. */
        public static boolean zapperMayKill(String boss, boolean playerHitIt) {
            return !boss.equals("nmz_count_draynor") || playerHitIt;
        }

        // ---- Benefits: potions sold by the dose at Dom Onion's Reward Shop, usable only inside a dream (OSRS Wiki).
        public static final String OVERLOAD = "overload";
        public static final String ABSORPTION = "absorption";
        public static final String SUPER_RANGING = "super_ranging";
        public static final String SUPER_MAGIC = "super_magic_potion";
        public static final List<String> POTIONS = List.of(OVERLOAD, ABSORPTION, SUPER_RANGING, SUPER_MAGIC);
        /** Overload: +5 + 15% to Attack, Strength, Defence, Ranged and Magic for 5 minutes, re-applied every 15 seconds. */
        public static final int OVERLOAD_TICKS = 500;
        public static final int OVERLOAD_REAPPLY_TICKS = 25;
        /** Overload damage: 50 in five hits of 10 (never absorbed); needs more than 50 Hitpoints; heals 50 when it ends. */
        public static final int OVERLOAD_HITS = 5;
        public static final int OVERLOAD_HIT = 10;
        public static final int OVERLOAD_HEAL = 50;
        /** Absorption: each dose absorbs the next 50 damage dealt by monsters, up to 1,000 stored. */
        public static final int ABSORPTION_PER_DOSE = 50;
        public static final int ABSORPTION_MAX = 1000;

        /** A power-up stays in the arena for one minute. */
        public static final int POWER_UP_LIFETIME = 100;
        /** Game ticks between power-ups appearing in a rumble (approx.: the wiki gives no rate). */
        public static final int POWER_UP_INTERVAL = 150;
        /** Zapper: max hit, and game ticks between zaps (interval approx.). */
        public static final int ZAPPER_MAX_HIT = 8;
        public static final int ZAPPER_INTERVAL = 3;
        /** Power surge: special attack energy restored per tick (20%). */
        public static final int POWER_SURGE_PER_TICK = 20;

        /** Power-ups (rumble only; OSRS Wiki). */
        public enum PowerUp {
            /** An extra hit of 3/4 of each hit's damage (no XP), 45 seconds. */
            RECURRENT_DAMAGE("Recurrent damage", 75),
            /** Zaps nearby bosses for up to 8 damage over time, 1 minute. */
            ZAPPER("Zapper", 100),
            /** Fills the special attack bar and regenerates it by 20% a tick, 45 seconds. */
            POWER_SURGE("Power surge", 75),
            /** Kills every boss in the arena at once, for no points. */
            ULTIMATE_FORCE("Ultimate force", 0);

            private final String displayName;
            private final int ticks;

            PowerUp(String displayName, int ticks) {
                this.displayName = displayName;
                this.ticks = ticks;
            }

            public String displayName() {
                return displayName;
            }

            /** How long the effect lasts in game ticks (0 = instant). */
            public int ticks() {
                return ticks;
            }
        }

        private NightmareZone() {
        }

        /**
         * Points per dose at Dom Onion's: overload 1,500, absorption 1,000, super ranging and super magic 250. Doses go into
         * the potion barrels in the lobby and are withdrawn from there, as many as wanted.
         */
        public static int doseCost(String potion) {
            return switch (potion) {
                case OVERLOAD -> 1500;
                case ABSORPTION -> 1000;
                case SUPER_RANGING, SUPER_MAGIC -> 250;
                default -> 0;
            };
        }

        /** Boost of the overload, super ranging and super magic potion: floor(level * 15 / 100) + 5. */
        public static int boost(int level) {
            return level * 15 / 100 + 5;
        }

        /** Absorption points after one more dose. */
        public static int absorb(int points) {
            return Math.min(ABSORPTION_MAX, Math.max(0, points) + ABSORPTION_PER_DOSE);
        }

        /** Recurrent damage's extra hit: 3/4 of the hit, rounded down (a 35 adds 26). */
        public static int recurrent(int damage) {
            return Math.max(0, damage) * 3 / 4;
        }

        public static int points(String boss, boolean hard) {
            for (Object[] b : BOSSES) {
                if (b[0].equals(boss)) {
                    return (Integer) b[hard ? 2 : 1];
                }
            }
            return 0;
        }

        /** Hitpoints of a boss in a hard rumble (0 if it isn't a rumble boss). */
        public static int hardHitpoints(String boss) {
            for (Object[] b : BOSSES) {
                if (b[0].equals(boss)) {
                    return (Integer) b[3];
                }
            }
            return 0;
        }

        /** Whether enough of the listed quests are complete, given a completion check. */
        public static boolean canEnter(java.util.function.Predicate<String> questComplete) {
            return QUESTS.stream().distinct().filter(questComplete).count() >= QUESTS_NEEDED;
        }
    }

    // ---------------------------------------------------------------- Soul Wars
    public static final class SoulWars {
        /** A game lasts up to 15 minutes; five Avatar kills end it early. */
        public static final int GAME_TICKS = 1500;
        public static final int AVATAR_KILLS_TO_WIN = 5;
        /** Entry: total level 500 and combat level 40. */
        public static final int TOTAL_LEVEL = 500;
        public static final int COMBAT_LEVEL = 40;
        /** Zeal for a full contribution: 60 for a win, 50 for a draw, 40 for a loss. */
        public static final int ZEAL_WIN = 60;
        public static final int ZEAL_DRAW = 50;
        public static final int ZEAL_LOSS = 40;
        /** Contribution limits (OSRS Wiki): 30 soul fragments, 6 captures, 260 Avatar damage. */
        public static final int FRAGMENT_LIMIT = 30;
        public static final int CAPTURE_LIMIT = 6;
        public static final int DAMAGE_LIMIT = 260;
        /** Avatars have 1,500 hitpoints and start with 100% damage reduction; each fragment removes 1%. */
        public static final int AVATAR_HP = 1500;

        private SoulWars() {
        }

        /**
         * Zeal at the end of a game: the outcome's maximum times the player's contribution, the mean of
         * their fragment, capture and Avatar-damage shares (approx.: the wiki gives only the limits).
         */
        public static int zeal(int outcome, int fragments, int captures, int avatarDamage) {
            int max = outcome > 0 ? ZEAL_WIN : outcome == 0 ? ZEAL_DRAW : ZEAL_LOSS;
            double share = (Math.min(fragments, FRAGMENT_LIMIT) / (double) FRAGMENT_LIMIT
                    + Math.min(captures, CAPTURE_LIMIT) / (double) CAPTURE_LIMIT
                    + Math.min(avatarDamage, DAMAGE_LIMIT) / (double) DAMAGE_LIMIT) / 3.0;
            return (int) Math.round(max * share);
        }

        /** Damage taken by an Avatar after its damage reduction (0-100%). */
        public static int reducedDamage(int damage, int reductionPercent) {
            return damage * (100 - Math.max(0, Math.min(100, reductionPercent))) / 100;
        }
    }

    // ---------------------------------------------------------------- Tears of Guthix
    public static final class TearsOfGuthix {
        /** Playable once every seven days, after 100,000 XP or a quest point since the last visit. */
        public static final int DAYS_BETWEEN = 7;
        public static final long XP_SINCE_LAST = 100_000;
        /** Tie-break order for the skill with the least experience (OSRS Wiki, since November 2025). */
        public static final List<com.runecraft.osrs.Skill> ORDER = List.of(com.runecraft.osrs.Skill.ATTACK,
                com.runecraft.osrs.Skill.STRENGTH, com.runecraft.osrs.Skill.RANGED, com.runecraft.osrs.Skill.MAGIC,
                com.runecraft.osrs.Skill.DEFENCE, com.runecraft.osrs.Skill.HITPOINTS, com.runecraft.osrs.Skill.PRAYER,
                com.runecraft.osrs.Skill.AGILITY, com.runecraft.osrs.Skill.HERBLORE, com.runecraft.osrs.Skill.THIEVING,
                com.runecraft.osrs.Skill.CRAFTING, com.runecraft.osrs.Skill.RUNECRAFT, com.runecraft.osrs.Skill.MINING,
                com.runecraft.osrs.Skill.SMITHING, com.runecraft.osrs.Skill.FISHING, com.runecraft.osrs.Skill.COOKING,
                com.runecraft.osrs.Skill.FIREMAKING, com.runecraft.osrs.Skill.WOODCUTTING, com.runecraft.osrs.Skill.FLETCHING,
                com.runecraft.osrs.Skill.SLAYER, com.runecraft.osrs.Skill.FARMING, com.runecraft.osrs.Skill.CONSTRUCTION,
                com.runecraft.osrs.Skill.HUNTER, com.runecraft.osrs.Skill.SAILING);
        /** Each stream stays put for 16 game ticks; three blue and three green streams run at once. */
        public static final int STREAM_TICKS = 16;

        private TearsOfGuthix() {
        }

        /** Game ticks in the cave: one per quest point. */
        public static int ticks(int questPoints) {
            return Math.max(0, questPoints);
        }

        /** XP per tear: min(60, 10 + floor(xp / 27) / 10), so 60 from level 30 (OSRS Wiki). */
        public static double xpPerTear(int currentXp) {
            return Math.min(60.0, 10 + Math.floor(currentXp / 27.0) / 10.0);
        }

        /** Total XP for the tears collected (never negative). */
        public static double xp(int tears, int currentXp) {
            return Math.max(0, tears) * xpPerTear(currentXp);
        }

        /**
         * The skill that receives the experience: the one with the least XP among the eligible skills,
         * ties going to the earlier skill in {@link #ORDER}.
         */
        public static com.runecraft.osrs.Skill skill(java.util.function.ToIntFunction<com.runecraft.osrs.Skill> xp,
                                                     java.util.function.Predicate<com.runecraft.osrs.Skill> eligible) {
            com.runecraft.osrs.Skill best = null;
            for (com.runecraft.osrs.Skill s : ORDER) {
                if (eligible.test(s) && (best == null || xp.applyAsInt(s) < xp.applyAsInt(best))) {
                    best = s;
                }
            }
            return best;
        }
    }

}
