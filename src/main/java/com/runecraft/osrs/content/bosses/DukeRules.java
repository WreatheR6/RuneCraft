package com.runecraft.osrs.content.bosses;

/**
 * Waking Duke Sucellus (OSRS Wiki "Duke Sucellus/Strategies", "Arder-musca poison"): he sleeps until his poison tolerance
 * meter (200, awakened 370) is filled. Arder and musca mushrooms (from the asylum's far corridors) are ground with a pestle
 * and mortar into powder (5 a mushroom at 70 Herblore, 6 from 80), salax salt is mined from the salt piles, and six of each
 * in a fermentation vat make an arder-musca poison: two wake him (three awakened). Powder can also be fed to him directly,
 * 5 each. approx: a poison's share of the meter (125, so that two fill 200 and three 370), the salt a harvest gives and the
 * powder below 70 Herblore.
 */
public final class DukeRules {
    private DukeRules() {
    }

    public static final String ARDER_MUSHROOM = "arder_mushroom";
    public static final String MUSCA_MUSHROOM = "musca_mushroom";
    public static final String ARDER_POWDER = "arder_powder";
    public static final String MUSCA_POWDER = "musca_powder";
    public static final String SALAX_SALT = "salax_salt";
    public static final String POISON = "arder_musca_poison";

    /** The asylum's resources (blocks in the Ghorrock Prison). */
    public static final String ARDER_MUSHROOMS = "duke_arder_mushrooms";
    public static final String MUSCA_MUSHROOMS = "duke_musca_mushrooms";
    public static final String SALT_PILE = "duke_salt_pile";
    public static final String VAT = "duke_fermentation_vat";

    public static final int TOLERANCE = 200;
    public static final int TOLERANCE_AWAKENED = 370;
    public static final int POISON_VALUE = 125;
    public static final int POWDER_VALUE = 5;
    /** Each ingredient a vat takes for one poison. */
    public static final int PER_POISON = 6;

    public static int tolerance(boolean awakened) {
        return awakened ? TOLERANCE_AWAKENED : TOLERANCE;
    }

    /** Powder from one mushroom at a Herblore level: 6 from 80, 5 from 70, else 4 (approx). */
    public static int powderPerMushroom(int herblore) {
        return herblore >= 80 ? 6 : herblore >= 70 ? 5 : 4;
    }

    /** Salax salt from one harvest of a salt pile (12 in two harvests at 80 Mining, OSRS Wiki; approx otherwise). */
    public static int saltPerHarvest(int mining) {
        return Math.max(2, Math.min(7, mining / 13));
    }

    /** How many poisons the inventory's ingredients make. */
    public static int poisons(int salt, int arder, int musca) {
        return Math.min(salt, Math.min(arder, musca)) / PER_POISON;
    }

    /** The poison count that wakes him. */
    public static int poisonsToWake(boolean awakened) {
        int t = tolerance(awakened);
        return (t + POISON_VALUE - 1) / POISON_VALUE;
    }
}
