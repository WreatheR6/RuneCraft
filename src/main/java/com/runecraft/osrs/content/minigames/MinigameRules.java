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
        public static final int POINTS_HEAL_PYROMANCER = 30;
        public static final int MIN_POINTS = WintertodtRewards.MIN_POINTS;
        public static final int MAX_ROLLS = WintertodtRewards.MAX_ROLLS;
        public static final int BRAZIERS = 4;

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

        /** Guaranteed reward rolls: 2 at 500 points, +1 per further 500 (max 28). See {@link WintertodtRewards}. */
        public static int rolls(int points) {
            return WintertodtRewards.rolls(points);
        }

        /** Reward rolls including the chance of an extra roll for points between 500-point milestones. */
        public static int rolls(int points, double random) {
            return WintertodtRewards.rolls(points, random);
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
        public static final int VULNERABLE_TICKS = 25;
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
        public static final int WAVES = 10;
        public static final int QUEEN_HP = 250;
        /** Eight omega egg hits kill the Penance Queen. */
        public static final int QUEEN_EGG_HITS = 8;
        public static final int PET_CHANCE = 1000;
        /** Wave 10 pays a flat 80 points in your role and 5 in each of the other three (OSRS Wiki). */
        public static final int WAVE_10_ROLE = 80;
        public static final int WAVE_10_OTHER = 5;
        /** Honour points are capped at 5,000 per role and queen kills at 40. */
        public static final int MAX_HONOUR = 5000;
        public static final int MAX_QUEEN_KILLS = 40;
        /**
         * Base honour points for a full round (waves 1-10 including wave 10's 80) per role, from the
         * wiki strategy guide: attacker 370, defender 357, collector 278, healer 305.
         */
        public static final int[] FULL_ROUND = {370, 357, 278, 305};
        /** Penance hitpoints by wave (OSRS Wiki; wave 10 uses wave 7's). Runners always have 5. */
        private static final int[] FIGHTER_HP = {28, 29, 32, 37, 38, 49, 50, 55, 56, 50};
        private static final int[] RANGER_HP = {20, 28, 29, 34, 41, 50, 50, 54, 58, 50};
        private static final int[] HEALER_HP = {27, 32, 37, 43, 49, 55, 60, 67, 76, 60};
        public static final int RUNNER_HP = 5;

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

        /** Penance per wave: fighters, rangers, runners, healers (approx. of the wiki wave table). */
        public static int[] wave(int wave) {
            return new int[]{2 + (wave - 1) / 2, 2 + (wave - 1) / 2, 2 + wave / 3, 2 + (wave - 1) / 3};
        }

        /** Penance hitpoints for a type (0 fighter, 1 ranger, 2 runner, 3 healer) in a wave (OSRS Wiki). */
        public static int hitpoints(int type, int wave) {
            int w = Math.max(1, Math.min(WAVES, wave)) - 1;
            return switch (type) {
                case 0 -> FIGHTER_HP[w];
                case 1 -> RANGER_HP[w];
                case 2 -> RUNNER_HP;
                default -> HEALER_HP[w];
            };
        }

        /** Omega egg damage so that {@link #QUEEN_EGG_HITS} hits kill the queen. */
        public static int omegaEggDamage() {
            return (QUEEN_HP + QUEEN_EGG_HITS - 1) / QUEEN_EGG_HITS;
        }

        /**
         * Base honour points for clearing a wave (1-9) in a role with a clean performance. The wiki
         * gives only each role's round total, so it's spread over the waves in proportion to the
         * wave number (approx.; later waves pay more). Wave 10 pays {@link #WAVE_10_ROLE}.
         */
        public static int honour(int role, int wave) {
            if (wave >= WAVES) {
                return WAVE_10_ROLE;
            }
            return cumulative(role, wave) - cumulative(role, wave - 1);
        }

        private static int cumulative(int role, int wave) {
            int base = FULL_ROUND[role] - WAVE_10_ROLE;
            return (int) Math.round(base * (wave * (wave + 1) / 2.0) / 45.0);
        }

        /**
         * Points lost to escaped runners: 3 per escape for everyone, capped at 10; defenders lose
         * double (capped at 20). There are no penalties on wave 10.
         */
        public static int runnerPenalty(int role, int escapes, int wave) {
            if (wave >= WAVES) {
                return 0;
            }
            int team = Math.min(10, 3 * escapes);
            return role == 1 ? Math.min(20, 2 * 3 * escapes) : team;
        }

        /** Honour for a wave after penalties, never negative. */
        public static int waveHonour(int role, int wave, int escapes) {
            return Math.max(0, honour(role, wave) - runnerPenalty(role, escapes, wave));
        }

        /** Total clean honour points in a role for a full game (all ten waves). */
        public static int fullGameHonour(int role) {
            int total = 0;
            for (int w = 1; w <= WAVES; w++) {
                total += honour(role, w);
            }
            return total;
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
