package com.runecraft.osrs;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * Crop disease, compost and farmer protection (OSRS rules, see the wiki's "Disease (Farming)"):
 * <ul>
 *   <li>Every growth cycle a crop may become diseased with a per-crop chance out of 128. It can't
 *   happen on the first growth cycle and a fully grown crop is safe (so a crop with {@code n}
 *   growth cycles gets {@code n - 1} rolls).</li>
 *   <li>Compost, supercompost and ultracompost cut that chance by 50%, 80% and 90%, rounded down
 *   to whole 128ths (herbs: 27 -> 13 -> 5 -> 2).</li>
 *   <li>A diseased crop stops growing. Unless it is cured (plant cure, or pruning with secateurs
 *   for trees, fruit trees and bushes) before its next growth cycle, it dies.</li>
 *   <li>Paying the nearby gardener the crop's protection fee makes it immune to disease.</li>
 *   <li>Compost also adds harvest lives: +1 / +2 / +3.</li>
 * </ul>
 */
public final class FarmingDisease {
    /** Herb patches (and, approx., the other patch types without a known value): 27/128 per cycle. */
    public static final int BASE_CHANCE = 27;

    private FarmingDisease() {
    }

    /** Compost level 0 (none), 1 compost, 2 supercompost, 3 ultracompost -> disease chance out of 128. */
    public static int chance(int base128, int compost) {
        if (base128 <= 0) {
            return 0;
        }
        return switch (Math.max(0, Math.min(3, compost))) {
            case 1 -> base128 * 50 / 100;
            case 2 -> base128 * 20 / 100;
            case 3 -> base128 * 10 / 100;
            default -> base128;
        };
    }

    /** Extra harvest lives (herbs/allotments/hops...) from compost level 0..3. */
    public static int compostLives(int compost) {
        return Math.max(0, Math.min(3, compost));
    }

    /**
     * Rolls one growth cycle: true if the crop becomes diseased. {@code random.applyAsInt(128)} must
     * return 0..127. Protected crops and disease-free crops never roll.
     */
    public static boolean rollDisease(int chance128, boolean protectedByFarmer, IntUnaryOperator random) {
        return !protectedByFarmer && chance128 > 0 && random.applyAsInt(128) < chance128;
    }

    // ------------------------------------------------------------------ protection payments

    /** What a gardener wants to look after a crop (OSRS payments; baskets/sacks paid as loose items). */
    public record Payment(String item, int count) {
    }

    private static final Map<String, Payment> PAYMENTS = new HashMap<>();

    private static void pay(String crop, String item, int count) {
        PAYMENTS.put(crop, new Payment(item, count));
    }

    static {
        // Allotments (a sack = 10, a basket = 5 of the produce).
        pay("potato", "compost", 2);
        pay("onion", "potato", 10);
        pay("cabbage", "onion", 10);
        pay("tomato", "cabbage", 20);
        pay("sweetcorn", "jute_fibre", 10);
        pay("strawberry", "cooking_apple", 5);
        pay("watermelon", "curry_leaf", 10);
        pay("snape_grass", "jangerberries", 5);
        // Hops
        pay("barley", "compost", 3);
        pay("hammerstone", "onion", 5);        // approx. (OSRS: 1 marigold; flowers aren't in the game)
        pay("asgarnian", "onion", 5);
        pay("jute", "barley", 6);              // approx. (OSRS: 6 barley malt)
        pay("yanillian", "tomato", 5);
        pay("krandorian", "cabbage", 30);
        pay("wildblood", "cadava_berries", 10); // approx. (OSRS: 1 nasturtium)
        // Trees
        pay("oak", "tomato", 5);
        pay("willow", "cooking_apple", 5);
        pay("maple", "orange", 5);
        pay("yew", "cactus_spine", 10);
        pay("magic", "coconut", 25);
        // Fruit trees
        pay("apple", "sweetcorn", 9);
        pay("banana", "cooking_apple", 20);
        pay("orange", "strawberry", 15);
        pay("curry", "banana", 25);
        pay("pineapple", "watermelon", 10);
        pay("papaya", "pineapple", 10);
        pay("palm", "papaya_fruit", 15);
        pay("dragonfruit", "coconut", 15);
        // Bushes
        pay("redberry", "cabbage", 4);
        pay("cadavaberry", "tomato", 15);
        pay("dwellberry", "strawberry", 15);
        pay("jangerberry", "watermelon", 6);
        pay("whiteberry", "mushroom", 8);
        // Special trees and plants
        pay("teak", "limpwurt_root", 15);
        pay("mahogany", "yanillian_hops", 25);
        pay("calquat", "poison_ivy_berries", 8);
        pay("celastrus", "potato_cactus", 8);
        pay("redwood", "dragonfruit", 6);
        pay("spirit", "banana", 25);           // approx. (OSRS: 5 monkey nuts, a monkey bar and a ground tooth)
        pay("cactus", "cadava_berries", 6);
        pay("potato_cactus", "snape_grass", 8);
    }

    /** The protection fee for a crop id (allotment produce id or farming crop id), or null if it can't be protected. */
    public static Payment payment(String cropId) {
        return PAYMENTS.get(cropId);
    }

    public static Map<String, Payment> payments() {
        return Map.copyOf(PAYMENTS);
    }

    // ------------------------------------------------------------------ time-based crops

    /**
     * Health of a crop that grows with world time (a patch whose growth is computed from the tick it
     * was planted). The fields map 1:1 onto the patch's saved data. Call {@link #update} whenever the
     * patch is looked at; it rolls every growth cycle that passed since the last update.
     */
    public static final class Crop {
        /** World tick the crop was planted (shifted forward when a disease is cured). */
        public long planted;
        /** Growth cycles already rolled for disease. */
        public int rolled;
        /** World tick the crop became diseased, or -1. */
        public long diseasedAt = -1;
        public boolean dead;

        public Crop(long planted) {
            this.planted = planted;
        }

        public Crop(long planted, int rolled, long diseasedAt, boolean dead) {
            this.planted = planted;
            this.rolled = rolled;
            this.diseasedAt = diseasedAt;
            this.dead = dead;
        }

        public boolean diseased() {
            return diseasedAt >= 0 && !dead;
        }

        /**
         * Brings the crop up to {@code now}. {@code stages} is the number of growth cycles,
         * {@code cycleTicks} the length of one; disease is rolled at the end of every cycle except
         * the last (the one that makes it fully grown).
         */
        public void update(long now, int stages, long cycleTicks, int chance128, boolean protectedByFarmer, IntUnaryOperator random) {
            if (dead || cycleTicks <= 0) {
                return;
            }
            if (diseasedAt >= 0) {
                if (now - diseasedAt >= cycleTicks) {
                    dead = true;
                }
                return;
            }
            long cycles = Math.min(stages, Math.max(0, (now - planted) / cycleTicks));
            while (rolled < cycles) {
                int next = rolled + 1;
                if (next < stages && rollDisease(chance128, protectedByFarmer, random)) {
                    diseasedAt = planted + (long) next * cycleTicks;
                    if (now - diseasedAt >= cycleTicks) {
                        dead = true;
                    }
                    return;
                }
                rolled = next;
            }
        }

        /** Growth cycles completed (a diseased crop is stuck at the stage it caught the disease in). */
        public int stage(long now, int stages, long cycleTicks) {
            if (diseasedAt >= 0) {
                return rolled;
            }
            return (int) Math.min(stages, Math.max(0, (now - planted) / Math.max(1, cycleTicks)));
        }

        public boolean grown(long now, int stages, long cycleTicks) {
            return !dead && diseasedAt < 0 && stage(now, stages, cycleTicks) >= stages;
        }

        /**
         * Cures the disease at {@code now}: the crop resumes from the stage it was on and needs a
         * full cycle (re-rolled for disease) to grow again.
         */
        public void cure(long now, long cycleTicks) {
            if (diseasedAt < 0 || dead) {
                return;
            }
            planted = now - (long) rolled * cycleTicks;
            diseasedAt = -1;
        }
    }
}
