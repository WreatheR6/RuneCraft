package com.runecraft.osrs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * Crop disease, compost and farmer protection (OSRS rules, see the wiki's "Disease (Farming)"):
 * <ul>
 *   <li>Every growth cycle a crop may become diseased with a per-crop chance out of 128. It can't
 *   happen on the first growth cycle and a fully grown crop is safe (so a crop with {@code n}
 *   growth cycles gets {@code n - 1} rolls).</li>
 *   <li>The wiki knows the chance of only a few crops: herbs 27/128, fruit trees 18/128, maple trees
 *   13/128 and magic trees 9/128. The others are approximated by {@link #estimatedBase}, which keeps
 *   the total risk over a crop's growth close to those known crops (a crop with more growth cycles
 *   gets a smaller chance per cycle, as maple and magic trees do).</li>
 *   <li>Compost, supercompost and ultracompost cut that chance by 50%, 80% and 90%; the wiki's table
 *   gives herbs 27 -> 14 -> 6 -> 3, i.e. rounded up to whole 128ths. A living iasor anima plant cuts
 *   it by a further 80%, to at least 1/128.</li>
 *   <li>A diseased crop stops growing. Unless it is cured (plant cure, or pruning with secateurs
 *   for trees, fruit trees and bushes) before its next growth cycle, it dies.</li>
 *   <li>Paying the nearby gardener the crop's protection fee makes it immune to disease, and so does
 *   the right fully grown flower in the flower patch next to an allotment (or a scarecrow for
 *   sweetcorn; a white lily protects every allotment).</li>
 *   <li>Compost also adds harvest lives: +1 / +2 / +3.</li>
 * </ul>
 */
public final class FarmingDisease {
    /** Herb patches: 27/128 per growth cycle (OSRS wiki). */
    public static final int BASE_CHANCE = 27;
    /** Fruit trees: 18/128 per growth cycle (OSRS wiki). */
    public static final int FRUIT_TREE_CHANCE = 18;
    /**
     * Allotments, per visible growth step of the allotment block (approx.: the wiki has no figure for
     * allotments; with 3 rolls per crop this matches the herbs' total risk).
     */
    public static final int ALLOTMENT_CHANCE = estimatedBase(3);

    private FarmingDisease() {
    }

    /** Compost level 0 (none), 1 compost, 2 supercompost, 3 ultracompost -> disease chance out of 128. */
    public static int chance(int base128, int compost) {
        return chance(base128, compost, false);
    }

    /**
     * Disease chance out of 128 with compost level 0..3 and, if {@code iasor}, a living iasor anima
     * plant (a further 80% less, to a minimum of 1/128).
     */
    public static int chance(int base128, int compost, boolean iasor) {
        if (base128 <= 0) {
            return 0;
        }
        int c = switch (Math.max(0, Math.min(3, compost))) {
            case 1 -> (base128 * 50 + 99) / 100;
            case 2 -> (base128 * 20 + 99) / 100;
            case 3 -> (base128 * 10 + 99) / 100;
            default -> base128;
        };
        return iasor ? Math.max(1, c * 20 / 100) : c;
    }

    /**
     * Approximate disease chance per growth cycle for a crop with {@code riskyCycles} growth cycles
     * that can roll disease (its cycles minus one), for crops the wiki has no value for. Herbs (3
     * risky cycles) come out at 27, maple trees (7) at 13 and magic trees (11) at 8, close to the
     * wiki's 27, 13 and 9. Approx.
     */
    public static int estimatedBase(int riskyCycles) {
        if (riskyCycles <= 0) {
            return 0;
        }
        return Math.max(4, Math.min(27, (int) Math.round(88.0 / riskyCycles)));
    }

    /** Tree patch disease chances by tree: maple 13 and magic 9 are the wiki's, the rest approx. */
    public static int treeChance(String tree) {
        return switch (tree) {
            case "oak" -> 16;     // approx
            case "willow" -> 15;  // approx
            case "maple" -> 13;
            case "yew" -> 11;     // approx
            case "magic" -> 9;
            default -> estimatedBase(7);
        };
    }

    /** Patches that never get diseased in OSRS: the Troll Stronghold and Weiss herb patches. */
    public static boolean diseaseFreeArea(int osrsX, int osrsY) {
        return (osrsX >= 2816 && osrsX <= 2870 && osrsY >= 3662 && osrsY <= 3712)      // Troll Stronghold
                || (osrsX >= 2830 && osrsX <= 2870 && osrsY >= 3915 && osrsY <= 3950); // Weiss
    }

    /**
     * The Hosidius allotment, flower and herb patches, which are disease-free for players who have
     * completed the easy Kourend &amp; Kebos diary (OSRS).
     */
    public static boolean hosidiusFarm(int osrsX, int osrsY) {
        return osrsX >= 1725 && osrsX <= 1755 && osrsY >= 3545 && osrsY <= 3585;
    }

    /** Whether a crop planted at this tile can't get diseased, given the planter's Kourend &amp; Kebos easy diary. */
    public static boolean diseaseFree(int osrsX, int osrsY, boolean kourendEasyDiary) {
        return diseaseFreeArea(osrsX, osrsY) || (kourendEasyDiary && hosidiusFarm(osrsX, osrsY));
    }

    // ------------------------------------------------------------------ flowers and scarecrows

    /**
     * The flower (crop id) that protects an allotment crop (its produce id) from disease when it is
     * fully grown in the neighbouring flower patch, or null: marigolds protect potatoes, onions and
     * tomatoes, rosemary cabbages, nasturtiums watermelons and a scarecrow sweetcorn. Strawberries and
     * snape grass have no flower of their own.
     */
    public static String protectingFlower(String allotment) {
        return switch (allotment) {
            case "potato", "onion", "tomato" -> "marigold";
            case "cabbage" -> "rosemary";
            case "watermelon" -> "nasturtium";
            case "sweetcorn" -> "scarecrow";
            default -> null;
        };
    }

    /** Whether a fully grown flower (or scarecrow) protects this allotment crop; a white lily protects every allotment. */
    public static boolean flowerProtects(String allotment, String flower) {
        if (allotment == null || flower == null) {
            return false;
        }
        return flower.equals("white_lily") || flower.equals(protectingFlower(allotment));
    }

    // ------------------------------------------------------------------ anima plants

    /** Kronos: chance (percent) that a growth cycle grows an extra stage. Approx. (the wiki gives no figure). */
    public static final int KRONOS_SKIP_PERCENT = 10;

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

    private static final Map<String, List<Payment>> PAYMENTS = new HashMap<>();

    private static void pay(String crop, String item, int count) {
        PAYMENTS.computeIfAbsent(crop, k -> new ArrayList<>()).add(new Payment(item, count));
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
        pay("hammerstone", "marigolds", 1);
        pay("asgarnian", "onion", 10);         // a sack of onions
        pay("jute", "barley_malt", 6);
        pay("yanillian", "tomato", 5);
        pay("krandorian", "cabbage", 30);
        pay("wildblood", "nasturtiums", 1);
        // Sailing crops (OSRS Wiki): hemp 6 flax, cotton 6 hemp (flax wants 6 grain, which has no item here).
        pay("hemp", "flax", 6);
        pay("cotton", "hemp", 6);
        pay("camphor", "white_berries", 10);
        pay("ironwood", "curry_leaf", 10);
        pay("rosewood", "dragonfruit", 8);
        // Coral nurseries (Chet): 5 giant seaweed / elkhorn coral / pillar coral.
        pay("elkhorn", "giant_seaweed", 5);
        pay("pillar", "elkhorn_coral", 5);
        pay("umbral", "pillar_coral", 5);
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
        pay("spirit", "monkey_nuts", 5);
        pay("spirit", "monkey_bar", 1);
        pay("spirit", "ground_tooth", 1);
        pay("cactus", "cadava_berries", 6);
        pay("potato_cactus", "snape_grass", 8);
    }

    /** The protection fee for a crop id (allotment produce id or farming crop id), or null if it can't be protected. */
    public static Payment payment(String cropId) {
        List<Payment> list = PAYMENTS.get(cropId);
        return list == null ? null : list.get(0);
    }

    /** Every item of a crop's protection fee (empty if it can't be protected); the spirit tree's has three. */
    public static List<Payment> paymentsFor(String cropId) {
        return List.copyOf(PAYMENTS.getOrDefault(cropId, List.of()));
    }

    public static Map<String, List<Payment>> payments() {
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
