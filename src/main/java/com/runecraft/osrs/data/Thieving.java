package com.runecraft.osrs.data;

import java.util.List;

/**
 * Thieving: pickpocket targets, market stalls and the shared pickpocketing rules used by every
 * pickpocket target in the game (these built-in ones, the npcs module's townsfolk and the skilling
 * module's targets):
 * <ul>
 *   <li>Coins come as a <b>coin pouch</b> per successful pickpocket (OSRS since 2017). Pouches stack to
 *       {@link #COIN_POUCH_LIMIT} (56/84/140 with the medium/hard/elite Ardougne diary); with a full
 *       stack you must open them before pickpocketing again. Opening one gives the NPC's coins.</li>
 *   <li>The <b>rogue equipment</b> gives a 20% chance per piece worn of double loot (100% for all five).</li>
 *   <li>A <b>dodgy necklace</b> has a 25% chance to stop the stun when you fail; it has 10 charges.</li>
 *   <li>The <b>Thieving cape</b>, <b>gloves of silence</b> and the <b>Ardougne diary</b> scale the target's low/high
 *       values ({@link #pickpocketChance}).</li>
 * </ul>
 */
public final class Thieving {
    private Thieving() {
    }

    /** Coin pouches a player may hold before having to open them (no Ardougne diary). */
    public static final int COIN_POUCH_LIMIT = 28;
    /** The rogue equipment pieces; each worn piece is a 20% chance of double pickpocket loot. */
    public static final List<String> ROGUE_OUTFIT = List.of("rogue_mask", "rogue_top", "rogue_trousers", "rogue_gloves", "rogue_boots");
    /** Dodgy necklace: charges and the chance a failed pickpocket's stun is avoided. */
    public static final int DODGY_NECKLACE_CHARGES = 10;
    public static final double DODGY_NECKLACE_CHANCE = 0.25;
    /** Thieving cape (and max cape) pickpocket success multiplier. */
    public static final double THIEVING_CAPE_BONUS = 1.10;
    /** Game ticks a failed pickpocket stuns for (about 5 seconds). */
    public static final int STUN_TICKS = 8;

    /** Coin pouch limit by Ardougne diary tier completed (0 none/easy, 2 medium, 3 hard, 4 elite). */
    public static int coinPouchLimit(int ardougneTier) {
        return switch (ardougneTier) {
            case 4 -> 140;
            case 3 -> 84;
            case 2 -> 56;
            default -> COIN_POUCH_LIMIT;
        };
    }

    /** Chance of double loot for {@code pieces} rogue equipment pieces worn. */
    public static double rogueDoubleChance(int pieces) {
        return Math.min(1.0, Math.max(0, pieces) * 0.2);
    }

    /**
     * Pickpocket success chance. Each bonus (gloves of silence 1.05, Ardougne diary 1.1, Thieving cape 1.1) scales
     * the target's low and high values, rounded down after each one, before the usual interpolation: the OSRS Wiki's
     * charts give a guard 50/240 normally, 52/252 with gloves of silence, 55/264 with the diary and 60/290 with the
     * diary and the cape.
     */
    public static double pickpocketChance(int level, int low, int high, double... multipliers) {
        return com.runecraft.osrs.Chance.lerp(level, scaled(low, multipliers), scaled(high, multipliers));
    }

    /** A low or high value after the pickpocket bonuses (each multiplied in and rounded down in turn). */
    public static int scaled(int value, double... multipliers) {
        int v = value;
        for (double m : multipliers) {
            v = (int) Math.floor(v * m + 1e-9);
        }
        return v;
    }

    /**
     * A coin pouch: one per successful pickpocket of an NPC that carries coins; opening it gives
     * {@code min..max} coins (OSRS amounts).
     */
    public record CoinPouch(String key, String npc, int min, int max) {
        public String itemId() {
            return pouchId(key);
        }
    }

    public static final List<CoinPouch> COIN_POUCHES = List.of(
            new CoinPouch("man", "a man or woman", 3, 3),
            new CoinPouch("farmer", "a farmer", 9, 9),
            new CoinPouch("al_kharid_warrior", "an Al-Kharid warrior", 18, 18),
            new CoinPouch("rogue", "a rogue", 25, 120),
            new CoinPouch("guard", "a guard", 30, 30),
            new CoinPouch("fremennik_citizen", "a Fremennik citizen", 40, 40),
            new CoinPouch("bandit", "a bandit", 30, 30),
            new CoinPouch("bearded_pollnivneach_bandit", "a bearded Pollnivneach bandit", 40, 40), // approx.
            new CoinPouch("pollnivneach_bandit", "a Pollnivneach bandit", 50, 50),
            new CoinPouch("knight_of_ardougne", "a knight of Ardougne", 50, 50),
            new CoinPouch("watchman", "a watchman", 60, 60),
            new CoinPouch("menaphite_thug", "a Menaphite thug", 60, 60),
            new CoinPouch("paladin", "a paladin", 80, 80),
            new CoinPouch("gnome", "a gnome", 300, 300),
            new CoinPouch("hero", "a hero", 200, 300),
            new CoinPouch("vyre", "a vyre", 250, 250),
            new CoinPouch("elf", "an elf", 280, 350),
            // Varlamore's wealthy citizens (Civitas illa Fortis): 85 coins a pickpocket (approx.).
            new CoinPouch("wealthy_citizen", "a wealthy citizen", 85, 85));

    public static String pouchId(String key) {
        return "coin_pouch_" + key;
    }

    public static CoinPouch pouch(String key) {
        for (CoinPouch p : COIN_POUCHES) {
            if (p.key().equals(key)) {
                return p;
            }
        }
        return null;
    }

    /** The coin pouch each pickpocket target (any module's NPC or creature id) hands out. */
    private static final java.util.Map<String, String> NPC_POUCHES = java.util.Map.ofEntries(
            java.util.Map.entry("man", "man"), java.util.Map.entry("woman", "man"), java.util.Map.entry("farmer", "farmer"),
            java.util.Map.entry("al_kharid_warrior", "al_kharid_warrior"), java.util.Map.entry("rogue_pickpocket", "rogue"),
            java.util.Map.entry("guard", "guard"), java.util.Map.entry("desert_bandit", "bandit"),
            java.util.Map.entry("pollnivneach_bandit", "pollnivneach_bandit"),
            java.util.Map.entry("bearded_pollnivneach_bandit", "bearded_pollnivneach_bandit"),
            java.util.Map.entry("ardougne_knight", "knight_of_ardougne"), java.util.Map.entry("knight_of_ardougne", "knight_of_ardougne"),
            java.util.Map.entry("watchman", "watchman"), java.util.Map.entry("menaphite_thug", "menaphite_thug"),
            java.util.Map.entry("paladin", "paladin"), java.util.Map.entry("ardougne_paladin", "paladin"),
            java.util.Map.entry("gnome", "gnome"), java.util.Map.entry("gnome_pickpocket", "gnome"),
            java.util.Map.entry("hero", "hero"), java.util.Map.entry("ardougne_hero", "hero"),
            java.util.Map.entry("vyre_pickpocket", "vyre"), java.util.Map.entry("elf_pickpocket", "elf"),
            java.util.Map.entry("fremennik_citizen", "fremennik_citizen"),
            java.util.Map.entry("wealthy_citizen", "wealthy_citizen"));

    /**
     * Pickpocket targets whose (varying) coins are handed out loose, not in a coin pouch: H.A.M.
     * members, cave goblins and Digsite workmen, whose coins are one line of a mixed loot table, and Pollnivneach villagers.
     */
    public static final java.util.Set<String> LOOSE_COINS = java.util.Set.of("ham_member", "cave_goblin", "digsite_workman",
            "pollnivneach_villager");

    /** Pouch key for a pickpocket target id, or null if its coins (if any) are given loose. */
    public static String pouchForNpc(String npcId) {
        return NPC_POUCHES.get(npcId);
    }

    public static CoinPouch pouchByItem(String itemId) {
        for (CoinPouch p : COIN_POUCHES) {
            if (p.itemId().equals(itemId)) {
                return p;
            }
        }
        return null;
    }

    /** Pickpocketing. Success uses the standard low/high interpolation; failing stuns and damages you. */
    public enum Pickpocket {
        // Coins (the "coins" lines) are handed out as one coin pouch of the target's kind.
        MAN("Man", 1, 8.0, 180, 240, 3, 1, "man", List.of(new Loot("coins", 3, 3, 1))),
        FARMER("Farmer", 10, 14.5, 150, 240, 9, 1, "farmer", List.of(new Loot("coins", 9, 9, 1), new Loot("potato_seed", 1, 1, 4))),
        AL_KHARID_WARRIOR("Al-Kharid warrior", 25, 26.0, 100, 240, 18, 2, "al_kharid_warrior", List.of(new Loot("coins", 18, 18, 1))),
        GUARD("Guard", 40, 46.8, 50, 240, 30, 2, "guard", List.of(new Loot("coins", 30, 30, 1))),
        KNIGHT("Knight of Ardougne", 55, 84.3, 50, 240, 50, 3, "knight_of_ardougne", List.of(new Loot("coins", 50, 50, 1))),
        PALADIN("Paladin", 70, 131.8, 40, 170, 80, 3, "paladin", List.of(new Loot("coins", 80, 80, 1), new Loot("chaos_rune", 2, 2, 8))),
        HERO("Hero", 80, 163.3, 39, 160, 200, 3, "hero", List.of(new Loot("coins", 200, 300, 1), new Loot("death_rune", 2, 2, 8)));

        private final String name;
        private final int level;
        private final double xp;
        private final int low;
        private final int high;
        private final int coins;
        private final int stunDamage;
        private final String pouch;
        private final List<Loot> loot;

        Pickpocket(String name, int level, double xp, int low, int high, int coins, int stunDamage, String pouch, List<Loot> loot) {
            this.pouch = pouch;
            this.name = name;
            this.level = level;
            this.xp = xp;
            this.low = low;
            this.high = high;
            this.coins = coins;
            this.stunDamage = stunDamage;
            this.loot = loot;
        }

        public String displayName() {
            return name;
        }

        public int level() {
            return level;
        }

        public double xp() {
            return xp;
        }

        public int low() {
            return low;
        }

        public int high() {
            return high;
        }

        public int stunDamage() {
            return stunDamage;
        }

        public List<Loot> loot() {
            return loot;
        }

        /** Key of the coin pouch this target's coins come in ({@link #COIN_POUCHES}). */
        public String pouch() {
            return pouch;
        }
    }

    /** Market stalls (OSRS Wiki Stall/Thievable table, after the 2024 stall rebalance). Respawn is in game ticks. */
    public enum Stall {
        CAKE("cake_stall", "Baker's stall", 5, 16.0, 4, List.of(new Loot("cake", 1, 1, 1), new Loot("bread", 1, 1, 1), new Loot("chocolate_slice", 1, 1, 1))),
        SILK("silk_stall", "Silk stall", 20, 24.0, 8, List.of(new Loot("silk", 1, 1, 1))),
        FUR("fur_stall", "Fur stall", 35, 45.0, 12, List.of(new Loot("grey_wolf_fur", 1, 1, 1))),
        // Silver stall (wiki): silver ore 4/5, silver bar 3/20, tiara 1/20.
        SILVER("silver_stall", "Silver stall", 50, 205.0, 32, List.of(new Loot("silver_ore", 1, 1, 16), new Loot("silver_bar", 1, 1, 3),
                new Loot("tiara", 1, 1, 1))),
        SPICE("spice_stall", "Spice stall", 65, 92.0, 10, List.of(new Loot("spice", 1, 1, 1))),
        // Gem stall: sapphire 105/128, emerald 17/128, ruby 5/128, diamond 1/128 (wiki).
        GEM("gem_stall", "Gem stall", 75, 408.0, 100, List.of(new Loot("uncut_sapphire", 1, 1, 105),
                new Loot("uncut_emerald", 1, 1, 17), new Loot("uncut_ruby", 1, 1, 5), new Loot("uncut_diamond", 1, 1, 1)));

        private final String blockId;
        private final String name;
        private final int level;
        private final double xp;
        private final int respawnTicks;
        private final List<Loot> loot;

        Stall(String blockId, String name, int level, double xp, int respawnTicks, List<Loot> loot) {
            this.blockId = blockId;
            this.name = name;
            this.level = level;
            this.xp = xp;
            this.respawnTicks = respawnTicks;
            this.loot = loot;
        }

        public String blockId() {
            return blockId;
        }

        public String displayName() {
            return name;
        }

        public int level() {
            return level;
        }

        public double xp() {
            return xp;
        }

        public int respawnTicks() {
            return respawnTicks;
        }

        /** One of these is picked per successful steal, by weight. */
        public List<Loot> loot() {
            return loot;
        }

        /** The loot line for a roll in [0, total weight). */
        public Loot pick(int roll) {
            int r = roll;
            for (Loot l : loot) {
                if (r < l.weight()) {
                    return l;
                }
                r -= l.weight();
            }
            return loot.get(loot.size() - 1);
        }

        public int totalWeight() {
            int t = 0;
            for (Loot l : loot) {
                t += l.weight();
            }
            return t;
        }

        public static Stall byBlock(String blockId) {
            for (Stall s : values()) {
                if (s.blockId.equals(blockId)) {
                    return s;
                }
            }
            return null;
        }
    }

    /** A simple loot line; {@code weight} is relative for stalls and 1/weight chance for pickpocket extras. */
    public record Loot(String item, int min, int max, int weight) {
    }
}
