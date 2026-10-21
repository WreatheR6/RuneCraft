package com.runecraft.osrs;

/**
 * The attack timer's look after eating (round 43, {@code // playability}): food adds its delay to a running weapon delay
 * (OSRS Wiki, Food: 3 ticks, combo food 2, nothing when the weapon is ready), and the bar under the crosshair now shows that
 * wait as its own amber segment after the weapon's, instead of restarting as if the player had attacked. The bar's width is
 * shared in proportion: the weapon's period, then the food's added MC; both fill left to right as the clock runs.
 */
public final class FoodDelayBar {
    /** The food segment's colours (RGB): filled, and still to come. */
    public static final int FOOD_RGB = 0xE0A030;
    public static final int FOOD_EMPTY_RGB = 0x5A4010;

    private FoodDelayBar() {
    }

    /** The bar at one moment: where the weapon's part ends (0-1 of the width) and how far the clock has run (0-1). */
    public record Look(float split, float fill) {
        /** Whether the clock is still in the weapon's own delay. */
        public boolean inWeapon() {
            return fill < split;
        }
    }

    /**
     * The bar {@code leftMc} before the next attack may resolve, after food added {@code foodMc} to a weapon period of
     * {@code periodMc}: the weapon's part takes periodMc / (periodMc + foodMc) of the width.
     */
    public static Look look(int periodMc, int foodMc, float leftMc) {
        float total = Math.max(1, periodMc) + Math.max(0, foodMc);
        float split = Math.max(1, periodMc) / total;
        float fill = Math.max(0f, Math.min(1f, 1f - Math.max(0f, leftMc) / total));
        return new Look(split, fill);
    }

    /** MC of the food's delay still to run when {@code leftMc} remain in all: the food's part is the last of the wait. */
    public static int foodLeft(int foodMc, long leftMc) {
        return (int) Math.max(0, Math.min(Math.max(0, foodMc), leftMc));
    }
}
