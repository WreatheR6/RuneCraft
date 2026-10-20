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
        /** Animated armour: tier, combat level, hitpoints, max hit, tokens awarded (OSRS Wiki "Animated ... Armour" infoboxes). */
        public static final Object[][] ARMOUR = {
                {"bronze", 11, 10, 2, 5}, {"iron", 23, 20, 3, 10}, {"steel", 46, 40, 5, 15}, {"black", 69, 60, 7, 20},
                {"mithril", 92, 80, 10, 25}, {"adamant", 113, 99, 12, 30}, {"rune", 138, 120, 14, 40}};
        /** Animated armour that isn't defeated within five minutes falls apart and its pieces go back to the owner. // approx */
        public static final int ANIMATED_TICKS = 500;
        /** Tokens taken on entering the cyclops room and then every minute (100 game ticks). */
        public static final int CYCLOPS_TOKENS = 10;
        /** Kamfreena (and Lorelai in the basement) only let players with at least 100 tokens in. */
        public static final int CYCLOPS_ENTRY_TOKENS = 100;
        public static final int CYCLOPS_TICKS_PER_CHARGE = 100;
        /** A player who runs out of tokens has a minute left before being sent out (OSRS Wiki "Warriors' Guild"). */
        public static final int CYCLOPS_GRACE_TICKS = 100;
        /** Top floor: every defender up to rune at 2/100 (OSRS Wiki "Cyclops" drop table); basement: the dragon defender at 1/100. */
        public static final int DEFENDER_CHANCE = 50;
        public static final int DRAGON_DEFENDER_CHANCE = 100;
        /** The top floor's level 76 cyclops (the level 56 one is "warriors_guild_cyclops", the basement's "cyclops_elite"). */
        public static final String CYCLOPS_76 = "warriors_guild_cyclops_76";
        /** Session variants (entrance blocks) of the guild's activities. */
        public static final String CYCLOPS_ENTRANCE = "warriors_guild_cyclops_entrance";
        public static final String BASEMENT_ENTRANCE = "warriors_guild_basement_entrance";
        public static final String DUMMY_ENTRANCE = "warriors_guild_dummy_entrance";
        public static final String CATAPULT_ENTRANCE = "warriors_guild_catapult_entrance";
        public static final String SHOT_PUT_ENTRANCE = "warriors_guild_shotput_entrance";
        public static final String KEG_ENTRANCE = "warriors_guild_keg_entrance";
        /** Flag set once a rune defender has been shown to Lorelai: the basement then stays open (OSRS Wiki). */
        public static final String BASEMENT_FLAG = "wg_basement_unlocked";
        /** Defender tier indices (into {@link MinigameItems#DEFENDER_TIERS}). */
        public static final int RUNE = 6;
        public static final int DRAGON = 7;

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

        /**
         * The tier a defender item counts as when shown to Kamfreena (0 bronze .. 7 dragon), or -1: the trimmed rune and dragon
         * defenders and the avernic defenders made from a dragon defender count as what they were made from.
         */
        public static int defenderTier(String item) {
            if (item == null) {
                return -1;
            }
            switch (item) {
                case "rune_defender_t" -> {
                    return RUNE;
                }
                case "dragon_defender_t", "avernic_defender", "ghommals_avernic_defender_5", "ghommals_avernic_defender_6" -> {
                    return DRAGON;
                }
                default -> {
                    for (int i = 0; i < MinigameItems.DEFENDER_TIERS.length; i++) {
                        if (item.equals(MinigameItems.DEFENDER_TIERS[i] + "_defender")) {
                            return i;
                        }
                    }
                    return -1;
                }
            }
        }

        /**
         * The defender the cyclopes drop for a player who showed {@code best} on entering (OSRS Wiki "Warriors' Guild"): on the
         * top floor the next one up to rune (rune again once shown), in the basement the dragon defender (players who never showed
         * a rune defender can't enter). The drop is fixed when entering: a new defender has to be shown by leaving and
         * re-entering.
         */
        public static int targetDefender(int best, boolean basement) {
            return basement ? DRAGON : Math.min(RUNE, best + 1);
        }

        /** 1 in N chance of a cyclops dropping the target defender. */
        public static int defenderChance(int target) {
            return target == DRAGON ? DRAGON_DEFENDER_CHANCE : DEFENDER_CHANCE;
        }

        /**
         * Chance in tenths that a defeated animated armour gives a piece back (OSRS Wiki drop tables): bronze 9/10 for each piece,
         * iron's legs and steel's helm 9/10, everything else always.
         */
        public static int returnTenths(String tier, String piece) {
            return switch (tier) {
                case "bronze" -> 9;
                case "iron" -> piece.equals("platelegs") ? 9 : 10;
                case "steel" -> piece.equals("full_helm") ? 9 : 10;
                default -> 10;
            };
        }

        // ---- The dummy room (Ajjat): seven dummies, each hit with the right style gives 15 Attack XP and 2 tokens.
        /** The seven dummies: the four melee combat styles and the three melee attack types (OSRS Wiki "Warriors' Guild"). */
        public static final List<String> DUMMIES = List.of("accurate", "aggressive", "controlled", "defensive", "stab", "slash", "crush");
        public static final int DUMMY_XP = 15;
        public static final int DUMMY_TOKENS = 2;
        /** A wrong style stuns the player for 3 ticks (OSRS Wiki "Dummy (Warriors' Guild)"). */
        public static final int DUMMY_STUN_TICKS = 3;
        /** A dummy rises every 8 ticks and stays up for 6. // approx: the wiki gives no timing */
        public static final int DUMMY_INTERVAL = 8;
        public static final int DUMMY_UP_TICKS = 6;

        /** Whether hitting {@code dummy} with this combat style name and melee attack type is right. */
        public static boolean dummyHit(String dummy, String combatStyle, String attackType) {
            return switch (dummy) {
                case "stab", "slash", "crush" -> dummy.equalsIgnoreCase(attackType);
                default -> dummy.equalsIgnoreCase(combatStyle);
            };
        }

        // ---- The catapult room (Gamfred): block the right attack type with the defensive shield.
        /** The catapult's four ammunition types, blocked with the matching defensive option (OSRS Wiki "Defensive shield"). */
        public static final List<String> CATAPULT_AMMO = List.of("stab", "blunt", "slash", "magic");
        /** It fires every 8 ticks; each successful block gives 10 Defence XP and 1 token (OSRS Wiki). */
        public static final int CATAPULT_INTERVAL = 8;
        public static final int CATAPULT_XP = 10;
        public static final int CATAPULT_TOKENS = 1;
        /** Damage of a shot that isn't blocked. // approx */
        public static final int CATAPULT_DAMAGE = 3;

        // ---- The shot put room: 50 Strength to pass the heavy door.
        public static final int SHOT_PUT_STRENGTH = 50;
        /** Throwing styles and their factor S: standing 20, step and throw 10, spin and throw 0 (OSRS Wiki "18lb shot"). */
        public static final List<String> SHOT_STYLES = List.of("standing", "step", "spin");
        /** The longest shot (99 Strength, full run energy, standing throw, dusted hands, 18lb): 14 yards. */
        public static final int SHOT_MAX_YARDS = 14;
        /** One in 50 throws fails (dropped on the toe for 1 damage, or off the lane). // approx: the wiki says "rarely" */
        public static final int SHOT_FAIL_CHANCE = 50;

        public static int shotStyleFactor(String style) {
            return switch (style) {
                case "standing" -> 20;
                case "step" -> 10;
                default -> 0;
            };
        }

        /** The wiki's shot variable X = L + R + S + D - W (base Strength, run energy %, style, dusted hands 5, weight in lb). */
        public static int shotX(int strength, int energyPercent, String style, boolean dusted, int weight) {
            return strength + energyPercent + shotStyleFactor(style) + (dusted ? 5 : 0) - weight;
        }

        /**
         * Yards a shot travels: proportional to X, 14 at the wiki's best throw (X = 206), at least 1. // approx: the wiki's
         * calculator formula isn't in the sources
         */
        public static int shotYards(int x) {
            return Math.max(1, Math.min(SHOT_MAX_YARDS, Math.round(x * SHOT_MAX_YARDS / 206f)));
        }

        /** Strength XP of a shot: yards * (X + 10) * 0.05, which gives the wiki's 151.2 for its 14-yard example. // approx */
        public static double shotXp(int yards, int x) {
            return yards * (x + 10) * 0.05;
        }

        /** Tokens for a shot: the distance plus one (18lb) or plus three (22lb) (OSRS Wiki "18lb shot", "22lb shot"). */
        public static int shotTokens(int yards, int weight) {
            return yards + (weight >= 22 ? 3 : 1);
        }

        // ---- Jimmy's challenge: balance up to five kegs on your head.
        public static final int MAX_KEGS = 5;
        /** Picking up a keg costs run energy (hundredths of a percent). // approx */
        public static final int KEG_ENERGY = 1000;
        /** Every 8 ticks of balancing gives Strength XP and tokens per keg. // approx: the wiki gives no rates */
        public static final int KEG_INTERVAL = 8;
        public static final double KEG_XP = 2.0;
        public static final int KEG_TOKENS = 1;
        /** Falling kegs deal up to 4 damage (OSRS Wiki "Keg (Warriors' Guild)"). */
        public static final int KEG_MAX_DAMAGE = 4;

        /** Chance per keg tick that the kegs fall: higher as run energy drops (0 at full energy). // approx */
        public static double kegFallChance(int energyPercent, int kegs) {
            return Math.max(0, 100 - energyPercent) / 100.0 * (0.05 + 0.02 * kegs);
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
        public static final double[] XP_PER_PLANK = {30, 48, 72, 112}; // plank value approx
        /** Construction XP for completing a contract (not boosted by the carpenter's outfit). */
        public static final double[] COMPLETION_XP = {500, 1250, 2250, 2750};
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
    public static final class TroubleBrewing {
        /** A game lasts 20 minutes. */
        public static final int GAME_TICKS = 2000;
        /** 10 pieces of eight per bottle of rum the team makes, plus 1 per resource deposited (max 100). */
        public static final int EIGHT_PER_RUM = 10;
        public static final int MAX_CONTRIBUTION = 100;
        /** Cooking XP for depositing a bitternut (200) or scrapey bark / sweetgrubs (100). */
        public static final double BITTERNUT_XP = 200;
        public static final double DEPOSIT_XP = 100;
        /** Rum contribution reward per bottle the player helped make: Crafting 100, Cooking 250, Firemaking 150. */
        public static final double RUM_CRAFTING_XP = 100;
        public static final double RUM_COOKING_XP = 250;
        public static final double RUM_FIREMAKING_XP = 150;
        /** A bottle takes 38 seconds to brew (64 game ticks). */
        public static final int BREW_TICKS = 64;

        private TroubleBrewing() {
        }

        public static int piecesOfEight(int teamRum, int resourcesDeposited) {
            return EIGHT_PER_RUM * teamRum + Math.min(MAX_CONTRIBUTION, resourcesDeposited);
        }
    }

    // ---------------------------------------------------------------- Gnome Restaurant
    public static final class GnomeRestaurant {
        /** Easy deliveries: 6 minutes, 1 point; hard deliveries: 11 minutes, 3 points. 12 points make a reward token. */
        public static final int EASY_TICKS = 600;
        public static final int HARD_TICKS = 1100;
        public static final int EASY_POINTS = 1;
        public static final int HARD_POINTS = 3;
        public static final int POINTS_PER_TOKEN = 12;
        /** Every delivery is tipped 2-500 coins plus a roll on the tip table (approx. weights from the wiki). */
        public static final int TIP_MIN = 2;
        public static final int TIP_MAX = 500;
        public static final List<Loot> TIPS = List.of(
                new Loot("gnome_scarf", 1, 1, 23), new Loot("gnome_goggles", 1, 1, 23), new Loot("mint_cake", 1, 1, 18),
                new Loot("toadflax", 3, 3, 31), new Loot("snapdragon", 1, 1, 31), new Loot("uncut_sapphire", 6, 16, 31),
                new Loot("uncut_emerald", 3, 5, 31), new Loot("uncut_ruby", 2, 4, 31), new Loot("uncut_diamond", 1, 1, 31),
                new Loot("uncut_jade", 2, 3, 31), new Loot("uncut_opal", 3, 3, 31), new Loot("uncut_red_topaz", 1, 1, 31),
                new Loot("mithril_bolts", 16, 35, 40), new Loot("adamant_bolts", 12, 26, 20), new Loot("bird_nest_seeds", 1, 1, 20),
                new Loot("coins", 2, 500, 200));
        /**
         * Hard deliveries roll this table instead: the same tips plus the grand seed pod, tipped 5 at a time at 23/512 by
         * Wingstone, Penwie, Brambickle and Professor Manglethorp (OSRS Wiki "Grand seed pod"). Customers aren't tracked, so the
         * weight is that rate averaged over all hard-delivery customers (about 4 in 15-20): roughly 1/90 per hard delivery
         * (approx).
         */
        public static final List<Loot> HARD_TIPS = hardTips();

        private static List<Loot> hardTips() {
            List<Loot> out = new java.util.ArrayList<>(TIPS);
            out.add(new Loot("grand_seed_pod", 5, 5, 7)); // 7/630; weight approx
            return List.copyOf(out);
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
         * The dream bosses: creature id (the boss's first form), points per kill in a normal and in a hard dream with every boss
         * enabled, and hitpoints of that form in a hard dream (OSRS Wiki boss table, {@link NmzBosses}; hard bosses keep their
         * Defence).
         */
        public static final Object[][] BOSSES = NmzBosses.ALL.stream()
                .map(b -> new Object[]{b.id(), b.points(), b.hardPoints(), b.first().hardHitpoints()}).toArray(Object[][]::new);

        /** The quest that unlocks each dream boss (OSRS Wiki boss table). */
        public static final java.util.Map<String, String> BOSS_QUEST;

        static {
            java.util.Map<String, String> quests = new java.util.LinkedHashMap<>();
            for (NmzBosses.Boss b : NmzBosses.ALL) {
                quests.put(b.id(), b.quest());
            }
            BOSS_QUEST = java.util.Collections.unmodifiableMap(quests);
        }

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

        /** Whether a creature is a dream boss (any of a boss's forms). */
        public static boolean isBoss(String id) {
            return NmzBosses.ofForm(id) != null;
        }

        /**
         * Whether killing this creature defeats its boss: the last form of Witch's experiment, Nazastarool and Damis, or a
         * one-form boss. Earlier forms turn into the next one instead (OSRS Wiki "Four forms must be defeated").
         */
        public static boolean finalForm(String id) {
            return isBoss(id) && NmzBosses.nextForm(id) == null;
        }

        /** The boss a creature (any form) belongs to, or {@code id} itself. */
        public static String bossOf(String id) {
            NmzBosses.Boss b = NmzBosses.ofForm(id);
            return b == null ? id : b.id();
        }

        /**
         * The bosses a player can meet: those whose quest is complete, in the wiki table's order. Empty when none is: dreams
         * only use bosses "they have previously defeated during completed quests" (OSRS Wiki).
         */
        public static List<String> unlocked(java.util.function.Predicate<String> questComplete) {
            List<String> out = new java.util.ArrayList<>();
            for (NmzBosses.Boss b : NmzBosses.ALL) {
                if (questComplete.test(b.quest())) {
                    out.add(b.id());
                }
            }
            return out;
        }

        /** Every boss, in the wiki table's order (the self-test's dreams). */
        public static List<String> allBosses() {
            return unlocked(q -> true);
        }

        /**
         * The bosses of the same quest as {@code boss}: a customisable rumble switches them on and off together (OSRS Wiki
         * "Nightmare Zone/Strategies": "Disabling a boss also disables every boss from that quest").
         */
        public static List<String> questGroup(String boss) {
            String quest = BOSS_QUEST.get(bossOf(boss));
            List<String> out = new java.util.ArrayList<>();
            for (NmzBosses.Boss b : NmzBosses.ALL) {
                if (b.quest().equals(quest)) {
                    out.add(b.id());
                }
            }
            return out;
        }

        /** The quest whose bosses trigger the points penalty when on without {@link #PENALTY_UNLESS}'s. */
        public static final String PENALTY_QUEST = "dream_mentor";
        public static final String PENALTY_UNLESS = "desert_treasure_i";
        /** "A large 83.33% point penalty": points divided by 6 (Mod Ash, Docter_Bogs; OSRS Wiki). */
        public static final int PENALTY_DIVISOR = 6;

        /**
         * Whether a dream's bosses carry the point penalty: the Dream Mentor bosses enabled while every Desert Treasure I boss
         * is off (OSRS Wiki "Rumble"; Recipe for Disaster has no effect).
         */
        public static boolean penalised(java.util.Collection<String> pool) {
            boolean dreamMentor = false;
            boolean desertTreasure = false;
            for (String id : pool) {
                String q = BOSS_QUEST.get(bossOf(id));
                dreamMentor |= PENALTY_QUEST.equals(q);
                desertTreasure |= PENALTY_UNLESS.equals(q);
            }
            return dreamMentor && !desertTreasure;
        }

        /**
         * Points for a kill when only {@code included} of the dream's bosses are in it: the wiki's values are the maximum,
         * with every boss enabled, and "point values increase based on the number of bosses included". Linear in the share of
         * the fifty bosses included. // approx: the exact per-boss multipliers aren't documented (Docter_Bogs found each boss
         * has a multiplier and the dream sums those of the enabled bosses; equal multipliers give this formula)
         */
        public static int points(String boss, boolean hard, int included) {
            int max = points(boss, hard);
            int all = BOSSES.length;
            return (int) ((long) max * Math.max(0, Math.min(included, all)) / all);
        }

        /** {@link #points(String, boolean, int)} for a dream holding {@code pool}, with the Dream Mentor penalty. */
        public static int points(String boss, boolean hard, java.util.Collection<String> pool) {
            java.util.Set<String> bosses = new java.util.HashSet<>();
            for (String id : pool) {
                bosses.add(bossOf(id));
            }
            int pts = points(boss, hard, bosses.size());
            return penalised(bosses) ? pts / PENALTY_DIVISOR : pts;
        }

        /**
         * Hard-mode offence per form (OSRS Wiki infoboxes, normal / hard): attack level, max hit of the main attack, and
         * ranged and magic levels. Defence and defensive bonuses stay the same in hard mode.
         */
        public record HardStats(String boss, int attack, int hardAttack, int maxHit, int hardMaxHit, int ranged, int hardRanged,
                                int magic, int hardMagic) {
        }

        public static final List<HardStats> HARD_STATS = NmzBosses.forms().stream()
                .map(f -> new HardStats(f.id(), f.attack(), f.hardAttack(), f.maxHit(), f.hardMaxHit(), f.ranged(), f.hardRanged(),
                        f.magic(), f.hardMagic())).toList();

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
            return (to + 9) / (double) (Math.max(0, from) + 9);
        }

        /**
         * Extra max hit of a hard boss's main attack (the one its infobox max hit is for: Elvarg's slash, Arrg's melee; its other
         * attacks and dragonfire keep their max hits): {@code melee} says whether the attack is a melee one.
         */
        public static int hardMaxHitBonus(String boss, boolean melee, boolean ranged) {
            NmzBosses.Form f = NmzBosses.form(boss);
            if (f == null) {
                return 0;
            }
            boolean main = f.meleeMain() ? melee : f.hitStyle().equals("Ranged") ? ranged : !melee && !ranged;
            return main ? Math.max(0, f.hardMaxHit() - f.maxHit()) : 0;
        }

        /** Extra max hit of a hard boss's main attack. */
        public static int hardMaxHitBonus(String boss) {
            HardStats h = hardStats(boss);
            return h == null ? 0 : Math.max(0, h.hardMaxHit() - h.maxHit());
        }

        /**
         * Slayer task category and Slayer XP (normal / hard dream) of the bosses that count for a task (OSRS Wiki "Nightmare
         * Zone", Slayer training: every other boss gives no task credit; practice dreams never do).
         */
        public record SlayerCredit(String category, double xp, double hardXp) {
        }

        public static final java.util.Map<String, SlayerCredit> SLAYER = java.util.Map.ofEntries(
                java.util.Map.entry("nmz_evil_chicken", new SlayerCredit("birds", 120, 240)),
                java.util.Map.entry("nmz_giant_roc", new SlayerCredit("birds", 250, 255)),
                java.util.Map.entry("nmz_black_demon", new SlayerCredit("black_demons", 157, 255)),
                java.util.Map.entry("nmz_dagannoth_mother", new SlayerCredit("dagannoth", 126, 252)),
                java.util.Map.entry("nmz_elvarg", new SlayerCredit("green_dragons", 80, 246)),
                java.util.Map.entry("nmz_kamil", new SlayerCredit("ice_warriors", 146, 312)),
                java.util.Map.entry("nmz_corrupt_lizardman", new SlayerCredit("lizardmen", 50, 150)),
                java.util.Map.entry("nmz_giant_scarab", new SlayerCredit("scabarites", 137, 280.5)),
                java.util.Map.entry("nmz_dad", new SlayerCredit("trolls", 126, 264)),
                java.util.Map.entry("nmz_arrg", new SlayerCredit("trolls", 150.5, 299.6)),
                java.util.Map.entry("nmz_ice_troll_king", new SlayerCredit("trolls", 161, 287)),
                java.util.Map.entry("nmz_count_draynor", new SlayerCredit("vampyres", 35, 210)),
                java.util.Map.entry("nmz_dessous", new SlayerCredit("vampyres", 220, 299.6)));

        /** Slayer XP of a dream boss kill on task, or -1 when it isn't one of {@link #SLAYER}. */
        public static double slayerXp(String boss, boolean hard) {
            SlayerCredit c = SLAYER.get(boss);
            return c == null ? -1 : hard ? c.hardXp() : c.xp();
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

        /** A power-up stays in the arena for one minute; another can't appear while one waits (OSRS Wiki). */
        public static final int POWER_UP_LIFETIME = 100;
        /** Game ticks between power-ups appearing in a rumble. // approx: the wiki gives no rate */
        public static final int POWER_UP_INTERVAL = 150;
        /**
         * Zapper: fires every two ticks at monsters near it, hitting each for about 3% of its maximum hitpoints, at most 8
         * (OSRS Wiki "Zapper", "Nightmare Zone"). The number of zaps per firing varies: each nearby boss is zapped with a 1/2
         * chance, approx; the reach (in blocks) is approx too.
         */
        public static final int ZAPPER_MAX_HIT = 8;
        public static final int ZAPPER_INTERVAL = 2;
        public static final double ZAPPER_SHARE = 0.03;
        public static final int ZAPPER_REACH = 12;
        /** Bosses the zapper doesn't damage: those immune to normal attacks, and The Everlasting (OSRS Wiki). */
        public static final java.util.Set<String> ZAPPER_IMMUNE = java.util.Set.of("nmz_dagannoth_mother", "nmz_gelatinnoth_mother",
                "nmz_tanglefoot", "nmz_the_everlasting");
        /** Power surge: special attack energy restored per tick (20%). */
        public static final int POWER_SURGE_PER_TICK = 20;

        /** One zap's damage against a boss with {@code maxHitpoints}: 3% of them, at least 1 and at most 8. */
        public static int zapDamage(int maxHitpoints) {
            return Math.max(1, Math.min(ZAPPER_MAX_HIT, (int) Math.round(maxHitpoints * ZAPPER_SHARE)));
        }

        /** Whether the zapper damages this boss at all. */
        public static boolean zappable(String boss) {
            return !ZAPPER_IMMUNE.contains(bossOf(boss));
        }

        /** Power-ups (rumble only; OSRS Wiki). */
        public enum PowerUp {
            /** An extra hit of 3/4 of each hit's damage (no XP), 45 seconds. */
            RECURRENT_DAMAGE("Recurrent damage", 75),
            /** Stays where it was picked up and zaps the bosses near it every two ticks (3% of their hitpoints, at most 8), 1 minute. */
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

        /** Points for a kill of {@code boss} (any form) with every boss enabled, normal or hard (0 for other creatures). */
        public static int points(String boss, boolean hard) {
            NmzBosses.Boss b = NmzBosses.ofForm(boss);
            return b == null ? 0 : hard ? b.hardPoints() : b.points();
        }

        /** Hitpoints of a boss form in a hard dream (0 if it isn't a dream boss). */
        public static int hardHitpoints(String boss) {
            NmzBosses.Form f = NmzBosses.form(boss);
            return f == null ? 0 : f.hardHitpoints();
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
