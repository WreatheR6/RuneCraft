package com.runecraft.osrs.content.bosses;

/**
 * Waking Duke Sucellus (OSRS Wiki "Duke Sucellus/Strategies", "Arder-musca poison"): he sleeps until his poison tolerance
 * meter (200, awakened 370) is filled. Arder and musca mushrooms (from the asylum's far corridors) are ground with a pestle
 * and mortar into powder (5 a mushroom at 70 Herblore, 6 from 80), salax salt is mined from the salt piles, and six of each
 * in a fermentation vat make an arder-musca poison: two wake him (three awakened). Powder can also be fed to him directly,
 * 5 each. The salt deposit ("Salt Deposit", "Salax salt") gives 3-6 salt and 5 Mining XP a harvest, one harvest each time
 * the pickaxe swings (about 10,000 XP an hour with a rune pickaxe), and keeps going for five minutes after a click; a poison
 * gives 10 Herblore XP. approx: a poison's share of the meter (125, so that two fill 200 and three 370) and the powder below
 * 70 Herblore.
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
    /** Mining XP a salt harvest gives (whatever the salt count). */
    public static final double SALT_XP = 5.0;
    /** Herblore XP a fermented poison gives. */
    public static final double POISON_XP = 10.0;
    /** Game ticks a salt deposit keeps being mined after a click (five minutes; clicking again restarts it). */
    public static final int SALT_MINING_TICKS = 500;

    public static int tolerance(boolean awakened) {
        return awakened ? TOLERANCE_AWAKENED : TOLERANCE;
    }

    /** Powder from one mushroom at a Herblore level: 6 from 80, 5 from 70, else 4 (approx). */
    public static int powderPerMushroom(int herblore) {
        return herblore >= 80 ? 6 : herblore >= 70 ? 5 : 4;
    }

    /** Salax salt from one harvest of a salt deposit: 3 below 70 Mining, 4 from 70, 5 from 75, 6 from 80 (OSRS Wiki). */
    public static int saltPerHarvest(int mining) {
        return mining >= 80 ? 6 : mining >= 75 ? 5 : mining >= 70 ? 4 : 3;
    }

    /** How many poisons the inventory's ingredients make. */
    public static int poisons(int salt, int arder, int musca) {
        return Math.min(salt, Math.min(arder, musca)) / PER_POISON;
    }

    /**
     * Game ticks a filled vat ferments before its poison can be collected (OSRS Wiki "Arder-musca poison": a 6-tick recipe;
     * "Duke Sucellus/Strategies": "place them in both Fermentation Vats and let it ferment for several seconds before
     * collecting two finished poisons").
     */
    public static final int FERMENT_TICKS = 6;

    /**
     * One fermentation vat: it takes one batch (six of each ingredient) at a time, ferments it for {@link #FERMENT_TICKS}
     * and holds the finished poison until it is collected. The asylum has two, so two poisons brew at once.
     */
    public static final class Vat {
        /** Game tick the batch inside is ready, or -1 when the vat is empty. */
        private long readyAt = -1;

        public boolean empty() {
            return readyAt < 0;
        }

        /** Fills an empty vat at game tick {@code now}; false when it already holds a batch. */
        public boolean fill(long now) {
            if (!empty()) {
                return false;
            }
            readyAt = now + FERMENT_TICKS;
            return true;
        }

        public boolean ready(long now) {
            return !empty() && now >= readyAt;
        }

        /** Game ticks until the batch is ready (0 when ready or empty). */
        public long ticksLeft(long now) {
            return empty() ? 0 : Math.max(0, readyAt - now);
        }

        /** Takes the finished poison out: true when there was one ready. */
        public boolean collect(long now) {
            if (!ready(now)) {
                return false;
            }
            readyAt = -1;
            return true;
        }
    }

    /** The poison count that wakes him. */
    public static int poisonsToWake(boolean awakened) {
        int t = tolerance(awakened);
        return (t + POISON_VALUE - 1) / POISON_VALUE;
    }
}
