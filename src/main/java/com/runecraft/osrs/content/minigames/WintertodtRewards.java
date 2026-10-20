package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.Skill;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * Wintertodt rewards, as on the OSRS Wiki (Wintertodt, Reward Cart, Module:Wintertodt supply crate; snapshot
 * {@code wintertodt_reward_cart.tsv}, diffed by {@code SkillingBossDataTest}).
 *
 * <p>500+ points earn rewards: 2 at 500 points, one more for every further 500 points and a chance of one extra for
 * the points between milestones (1% per 5 points). Since the 9 October 2024 rework they are owed at the reward cart in
 * the Wintertodt Camp (at most 8,000 stored; searched one at a time, or five with "Big-search") instead of coming in a
 * supply crate, and the old crate's cap of 28 rolls a game is gone with it. The rolls are made when the cart is
 * searched, with the player's levels at that time.
 *
 * <p>Each roll first tries the unique table in order (phoenix 1/5000 while not owned, dragon axe 1/10,000, Tome of
 * Fire 1/1000, warm gloves 1/150 (a magic seed once three are owned), bruma torch 1/150 (2-3 torstol seeds once three
 * are owned), the pyromancer piece the player has fewest of 1/150, burnt pages 1/45). Otherwise it is one of six skill
 * tables (3/25 each), coins (5/25), dynamite (1/25) or saltpetre (1/25). Within a skill table each item is tried in
 * turn, best first, with a level-scaled chance out of 256; the last item always hits.
 */
public final class WintertodtRewards {
    private WintertodtRewards() {
    }

    public static final int MIN_POINTS = 500;
    public static final int POINTS_PER_ROLL = 500;
    /** The discontinued supply crate's cap (28 rolls at 13,500 points); crates still held open with their own count. */
    public static final int MAX_ROLLS = 28;
    /** Rewards the reward cart holds for a player. */
    public static final int CART_CAPACITY = 8000;
    /** "Big-search" takes this many rewards at once. */
    public static final int BIG_SEARCH = 5;
    /** Game ticks per reward taken from the cart (one every 3 ticks). */
    public static final int SEARCH_TICKS = 3;
    /** Warm gloves and bruma torches are replaced (magic seed, torstol seeds) once the player owns this many. */
    public static final int REPLACE_AT = 3;

    public static final int PHOENIX_CHANCE = 5000;
    public static final int DRAGON_AXE_CHANCE = 10_000;
    public static final int TOME_CHANCE = 1000;
    public static final int WARM_GLOVES_CHANCE = 150;
    public static final int BRUMA_TORCH_CHANCE = 150;
    public static final int PYROMANCER_CHANCE = 150;
    public static final int BURNT_PAGE_CHANCE = 45;

    /** The pyromancer outfit in the order the wiki breaks ties in: garb, hood, robe, boots. */
    public static final String[] PYROMANCER = {"pyromancer_garb", "pyromancer_hood", "pyromancer_robe", "pyromancer_boots"};

    /** One skill-table line: item, quantity range, and the low/high values (out of 256) at levels 1 and 99. */
    public record Line(String item, int min, int max, int low, int high) {
    }

    /** A skill table, best item first. */
    public record Table(Skill skill, List<Line> lines) {
    }

    public static final List<Table> TABLES = List.of(
            new Table(Skill.CRAFTING, List.of(
                    new Line("uncut_diamond", 1, 3, 10, 120), new Line("uncut_ruby", 2, 4, 50, 140),
                    new Line("uncut_emerald", 1, 3, 90, 160), new Line("uncut_sapphire", 1, 3, 255, 255))),
            new Table(Skill.HERBLORE, List.of(
                    new Line("grimy_torstol", 1, 3, -70, 40), new Line("grimy_dwarf_weed", 2, 4, -50, 50),
                    new Line("grimy_lantadyme", 2, 4, -30, 60), new Line("grimy_cadantine", 2, 4, -10, 70),
                    new Line("grimy_kwuarm", 2, 4, 10, 85), new Line("grimy_avantoe", 3, 5, 20, 100),
                    new Line("grimy_irit_leaf", 3, 5, 30, 115), new Line("grimy_ranarr_weed", 1, 3, 10, 170),
                    new Line("grimy_tarromin", 3, 6, 70, -20), new Line("grimy_marrentill", 3, 6, 100, -30),
                    new Line("grimy_guam_leaf", 3, 6, 170, -40), new Line("grimy_harralander", 3, 6, 255, 255))),
            new Table(Skill.FARMING, List.of(
                    new Line("spirit_seed", 1, 1, -20, 5), new Line("dwarf_weed_seed", 1, 3, -60, 25),
                    new Line("lantadyme_seed", 1, 3, -60, 30), new Line("cadantine_seed", 1, 3, -40, 40),
                    new Line("snapdragon_seed", 1, 3, -15, 60), new Line("yew_seed", 1, 2, -10, 70),
                    new Line("snape_grass_seed", 3, 7, -8, 78), new Line("kwuarm_seed", 1, 3, -10, 80),
                    new Line("ranarr_seed", 1, 3, 0, 110), new Line("avantoe_seed", 1, 3, 0, 130),
                    new Line("watermelon_seed", 3, 7, -10, 180), new Line("irit_seed", 1, 3, 0, 170),
                    new Line("teak_seed", 1, 2, 10, 160), new Line("maple_seed", 1, 2, 15, 190),
                    new Line("mahogany_seed", 1, 2, 20, 190), new Line("toadflax_seed", 1, 3, 20, 190),
                    new Line("banana_seed", 1, 2, 30, 180), new Line("willow_seed", 1, 2, 60, 120),
                    new Line("harralander_seed", 1, 3, 80, -150), new Line("tarromin_seed", 1, 3, 150, -150),
                    new Line("acorn", 1, 1, 255, 255))),
            new Table(Skill.WOODCUTTING, List.of(
                    new Line("magic_logs", 10, 20, -60, 60), new Line("yew_logs", 10, 20, -50, 90),
                    new Line("mahogany_logs", 10, 20, -40, 130), new Line("maple_logs", 10, 20, 0, 160),
                    new Line("teak_logs", 10, 20, 30, 200), new Line("willow_logs", 10, 20, 120, 100),
                    new Line("oak_logs", 10, 20, 255, 255))),
            new Table(Skill.MINING, List.of(
                    new Line("runite_ore", 1, 2, -60, 40), new Line("adamantite_ore", 2, 3, -50, 80),
                    new Line("mithril_ore", 3, 5, -20, 140), new Line("gold_ore", 8, 11, 0, 160),
                    new Line("coal", 10, 14, 0, 180), new Line("pure_essence", 20, 70, 40, 190),
                    // Limestone 3-7 as on the Reward Cart drop table (the calculator module says 2-7).
                    new Line("silver_ore", 10, 12, 140, 10), new Line("limestone", 3, 7, 200, -20),
                    new Line("iron_ore", 5, 15, 255, 255))),
            new Table(Skill.FISHING, List.of(
                    new Line("raw_shark", 6, 11, -60, 80), new Line("raw_swordfish", 6, 11, -50, 100),
                    new Line("raw_lobster", 6, 11, -20, 130), new Line("raw_tuna", 6, 11, 10, 160),
                    new Line("raw_salmon", 6, 11, 40, 180), new Line("raw_anchovies", 6, 11, 160, 0),
                    new Line("raw_trout", 6, 11, 255, 255))));

    /** Category weights out of 25 for a non-unique roll: each skill table 3, coins 5, dynamite 1, saltpetre 1. */
    public static final int SKILL_WEIGHT = 3;
    public static final int COINS_WEIGHT = 5;
    public static final int DYNAMITE_WEIGHT = 1;
    public static final int SALTPETRE_WEIGHT = 1;
    public static final int CATEGORY_TOTAL = 25;

    /** Guaranteed rewards for a score: 0 below 500, else 1 + points / 500 (no cap since the reward cart). */
    public static int rolls(int points) {
        return points < MIN_POINTS ? 0 : 1 + points / POINTS_PER_ROLL;
    }

    /** Rewards including the chance of one extra for the points between 500-point milestones. */
    public static int rolls(int points, double random) {
        int base = rolls(points);
        if (base == 0) {
            return base;
        }
        double extra = (points % POINTS_PER_ROLL) / (double) POINTS_PER_ROLL;
        return random < extra ? base + 1 : base;
    }

    /** Expected number of rewards (as on the wiki calculator: 1 + points / 500). */
    public static double expectedRolls(int points) {
        return points < MIN_POINTS ? 0 : 1 + points / (double) POINTS_PER_ROLL;
    }

    /** Rewards owed at the cart after adding a game's: the cart holds at most 8,000. */
    public static int addToCart(int owed, int won) {
        return (int) Math.min(CART_CAPACITY, Math.max(0L, (long) owed + Math.max(0, won)));
    }

    /** Chance (0-1) of one skill-table line succeeding at a level, as the wiki module interpolates it. */
    public static double lineChance(Line line, int level) {
        int l = Math.max(1, Math.min(99, level));
        int unclamped = (int) Math.floor(line.low() + (line.high() - line.low()) * (l - 1) / 98.0);
        return Math.min(Math.max(unclamped + 1, 0), 256) / 256.0;
    }

    /** Per-line probability of each item of a skill table being chosen at a level (sums to 1). */
    public static double[] tableChances(Table table, int level) {
        double[] out = new double[table.lines().size()];
        double remaining = 1.0;
        for (int i = 0; i < out.length; i++) {
            double c = remaining * lineChance(table.lines().get(i), level);
            out[i] = c;
            remaining -= c;
        }
        return out;
    }

    /**
     * The player's levels in the six crate skills, plus the uniques already owned: the phoenix, how many warm gloves and
     * bruma torches, and how many of each pyromancer piece (in {@link #PYROMANCER} order; null for none).
     */
    public record Player(int crafting, int herblore, int farming, int woodcutting, int mining, int fishing,
                         boolean hasPhoenix, int warmGloves, int brumaTorches, int[] pyromancer) {
        /** Booleans for the gloves and torch: true stands for owning three (the replacement threshold). */
        public Player(int crafting, int herblore, int farming, int woodcutting, int mining, int fishing, boolean hasPhoenix,
                      boolean hasWarmGloves, boolean hasBrumaTorch) {
            this(crafting, herblore, farming, woodcutting, mining, fishing, hasPhoenix, hasWarmGloves ? REPLACE_AT : 0,
                    hasBrumaTorch ? REPLACE_AT : 0, null);
        }

        int level(Skill s) {
            return switch (s) {
                case CRAFTING -> crafting;
                case HERBLORE -> herblore;
                case FARMING -> farming;
                case WOODCUTTING -> woodcutting;
                case MINING -> mining;
                default -> fishing;
            };
        }

        /** Pyromancer pieces of one kind owned (index into {@link #PYROMANCER}). */
        public int pieces(int i) {
            return pyromancer == null || i >= pyromancer.length ? 0 : pyromancer[i];
        }

        /** The same player after receiving {@code item} (counts for the replacement and pyromancer rules). */
        public Player after(String item) {
            int[] pyro = new int[PYROMANCER.length];
            for (int i = 0; i < pyro.length; i++) {
                pyro[i] = pieces(i) + (PYROMANCER[i].equals(item) ? 1 : 0);
            }
            return new Player(crafting, herblore, farming, woodcutting, mining, fishing, hasPhoenix || item.equals("phoenix"),
                    warmGloves + (item.equals("warm_gloves") ? 1 : 0), brumaTorches + (item.equals("bruma_torch") ? 1 : 0), pyro);
        }
    }

    /** The pyromancer piece a roll gives: the one the player has fewest of, ties broken garb, hood, robe, boots. */
    public static String pyromancerPiece(Player player) {
        int best = 0;
        for (int i = 1; i < PYROMANCER.length; i++) {
            if (player.pieces(i) < player.pieces(best)) {
                best = i;
            }
        }
        return PYROMANCER[best];
    }

    /**
     * One roll of the reward table. Warm gloves and the bruma torch are replaced by a magic seed and 2-3 torstol seeds
     * once the player owns three (Mod Ash, wiki).
     */
    public static MinigameReward roll(Player player, DoubleSupplier random) {
        if (!player.hasPhoenix() && random.getAsDouble() < 1.0 / PHOENIX_CHANCE) {
            return new MinigameReward("phoenix", 1);
        }
        if (random.getAsDouble() < 1.0 / DRAGON_AXE_CHANCE) {
            return new MinigameReward("dragon_axe", 1);
        }
        if (random.getAsDouble() < 1.0 / TOME_CHANCE) {
            return new MinigameReward("tome_of_fire", 1);
        }
        if (random.getAsDouble() < 1.0 / WARM_GLOVES_CHANCE) {
            return player.warmGloves() >= REPLACE_AT ? new MinigameReward("magic_seed", 1) : new MinigameReward("warm_gloves", 1);
        }
        if (random.getAsDouble() < 1.0 / BRUMA_TORCH_CHANCE) {
            return player.brumaTorches() >= REPLACE_AT ? new MinigameReward("torstol_seed", 2 + (int) (random.getAsDouble() * 2))
                    : new MinigameReward("bruma_torch", 1);
        }
        if (random.getAsDouble() < 1.0 / PYROMANCER_CHANCE) {
            return new MinigameReward(pyromancerPiece(player), 1);
        }
        if (random.getAsDouble() < 1.0 / BURNT_PAGE_CHANCE) {
            return new MinigameReward("burnt_page", 7 + (int) (random.getAsDouble() * 23));
        }
        int cat = (int) (random.getAsDouble() * CATEGORY_TOTAL);
        if (cat < TABLES.size() * SKILL_WEIGHT) {
            Table t = TABLES.get(cat / SKILL_WEIGHT);
            int level = player.level(t.skill());
            for (Line line : t.lines()) {
                if (random.getAsDouble() < lineChance(line, level) || line == t.lines().get(t.lines().size() - 1)) {
                    return new MinigameReward(line.item(), line.min() + (int) (random.getAsDouble() * (line.max() - line.min() + 1)));
                }
            }
        }
        cat -= TABLES.size() * SKILL_WEIGHT;
        if (cat < COINS_WEIGHT) {
            return new MinigameReward("coins", 2000 + (int) (random.getAsDouble() * 3000));
        }
        if (cat < COINS_WEIGHT + DYNAMITE_WEIGHT) {
            return new MinigameReward("dynamite", 3 + (int) (random.getAsDouble() * 3));
        }
        return new MinigameReward("saltpetre", 3 + (int) (random.getAsDouble() * 3));
    }

    /** All the rewards for a game's score, rolled in turn (each unique counted for the next roll). */
    public static List<MinigameReward> rewards(int points, Player player, DoubleSupplier random) {
        return search(rolls(points, random.getAsDouble()), player, random);
    }

    /** {@code n} rewards taken from the cart, rolled in turn (each unique counted for the next roll). */
    public static List<MinigameReward> search(int n, Player player, DoubleSupplier random) {
        List<MinigameReward> out = new ArrayList<>();
        Player p = player;
        for (int i = 0; i < n; i++) {
            MinigameReward r = roll(p, random);
            out.add(r);
            p = p.after(r.item());
        }
        return out;
    }

    /** Whether the cart hands a reward out as bank notes: the skill-table materials, dynamite and saltpetre (wiki). */
    public static boolean noted(String item) {
        if (item.equals("dynamite") || item.equals("saltpetre")) {
            return true;
        }
        for (Table t : TABLES) {
            if (t.skill() == Skill.FARMING) {
                continue; // seeds come as they are
            }
            for (Line l : t.lines()) {
                if (l.item().equals(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Every item a roll can give (for catalogue checks). */
    public static List<String> allItems() {
        List<String> ids = new ArrayList<>(List.of("phoenix", "dragon_axe", "tome_of_fire", "warm_gloves", "magic_seed", "bruma_torch",
                "torstol_seed", "burnt_page", "coins", "dynamite", "saltpetre"));
        ids.addAll(List.of(PYROMANCER));
        for (Table t : TABLES) {
            for (Line l : t.lines()) {
                ids.add(l.item());
            }
        }
        return ids;
    }
}
