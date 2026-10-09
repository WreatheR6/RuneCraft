package com.runecraft.osrs.data;

import java.util.List;

/** Thieving: pickpocket targets and market stalls. */
public final class Thieving {
    private Thieving() {
    }

    /** Pickpocketing. Success uses the standard low/high interpolation; failing stuns and damages you. */
    public enum Pickpocket {
        MAN("Man", 1, 8.0, 180, 240, 3, 1, List.of(new Loot("coins", 3, 3, 1))),
        FARMER("Farmer", 10, 14.5, 150, 240, 9, 1, List.of(new Loot("coins", 9, 9, 1), new Loot("potato_seed", 1, 1, 4))),
        AL_KHARID_WARRIOR("Al-Kharid warrior", 25, 26.0, 100, 240, 18, 2, List.of(new Loot("coins", 18, 18, 1))),
        GUARD("Guard", 40, 46.8, 50, 240, 30, 2, List.of(new Loot("coins", 30, 30, 1))),
        KNIGHT("Knight of Ardougne", 55, 84.3, 50, 240, 50, 3, List.of(new Loot("coins", 50, 50, 1))),
        PALADIN("Paladin", 70, 151.8, 50, 230, 80, 3, List.of(new Loot("coins", 80, 80, 1), new Loot("chaos_rune", 2, 2, 8))),
        HERO("Hero", 80, 273.3, 40, 220, 200, 4, List.of(new Loot("coins", 200, 300, 1), new Loot("death_rune", 2, 2, 8)));

        private final String name;
        private final int level;
        private final double xp;
        private final int low;
        private final int high;
        private final int coins;
        private final int stunDamage;
        private final List<Loot> loot;

        Pickpocket(String name, int level, double xp, int low, int high, int coins, int stunDamage, List<Loot> loot) {
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
    }

    /** Market stalls. Respawn is in game ticks. */
    public enum Stall {
        CAKE("cake_stall", "Baker's stall", 5, 16.0, 4, List.of(new Loot("cake", 1, 1, 1), new Loot("minecraft:bread", 1, 1, 1), new Loot("chocolate_slice", 1, 1, 1))),
        SILK("silk_stall", "Silk stall", 20, 24.0, 8, List.of(new Loot("silk", 1, 1, 1))),
        FUR("fur_stall", "Fur stall", 35, 36.0, 17, List.of(new Loot("grey_wolf_fur", 1, 1, 1))),
        SILVER("silver_stall", "Silver stall", 50, 54.0, 50, List.of(new Loot("silver_ore", 1, 1, 1))),
        SPICE("spice_stall", "Spice stall", 65, 81.0, 134, List.of(new Loot("spice", 1, 1, 1))),
        GEM("gem_stall", "Gem stall", 75, 160.0, 300, List.of(new Loot("uncut_sapphire", 1, 1, 1),
                new Loot("uncut_emerald", 1, 1, 1), new Loot("uncut_ruby", 1, 1, 1), new Loot("uncut_diamond", 1, 1, 1)));

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

        /** One of these is picked uniformly per successful steal. */
        public List<Loot> loot() {
            return loot;
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
