package com.runecraft.osrs.data;

/**
 * Bone-offering rules of the OSRS altars (wiki "Prayer training"):
 * <ul>
 *   <li>an ordinary altar only recharges Prayer points - bones can't be offered on it (they're buried for 100%);</li>
 *   <li>a player-owned house gilded altar gives 250%, +50% for each lit incense burner (300% / 350%);</li>
 *   <li>the Chaos Altar in the level-38 Wilderness gives 350% and has a 50% chance not to use up the bone;</li>
 *   <li>the Ectofuntus gives 400% for bonemeal worshipped with a bucket of slime.</li>
 * </ul>
 */
public final class PrayerAltars {
    public enum Kind {
        /** An ordinary church/temple altar: recharges Prayer only. */
        REGULAR(0.0),
        /** The Chaos Altar (Chaos Temple, level 38 Wilderness). */
        CHAOS(3.5),
        /** The Ectofuntus in Port Phasmatys. */
        ECTOFUNTUS(4.0);

        /** XP multiplier on a bone's burying XP (0 = bones can't be offered). */
        public final double multiplier;

        Kind(double multiplier) {
            this.multiplier = multiplier;
        }
    }

    /** Chance that the Chaos Altar keeps the bone (it is not used up). */
    public static final double CHAOS_SAVE_CHANCE = 0.5;

    private PrayerAltars() {
    }

    /** Which altar stands at an OSRS tile (the Chaos Temple and Ectofuntus footprints; everything else is ordinary). */
    public static Kind at(int osrsX, int osrsY) {
        if (osrsX >= 2944 && osrsX <= 2962 && osrsY >= 3810 && osrsY <= 3830) {
            return Kind.CHAOS;
        }
        if (osrsX >= 3654 && osrsX <= 3666 && osrsY >= 3513 && osrsY <= 3526) {
            return Kind.ECTOFUNTUS;
        }
        return Kind.REGULAR;
    }

    /** Gilded altar multiplier: 250% unlit, 300% with one lit incense burner, 350% with both. */
    public static double gilded(int litBurners) {
        return 2.5 + 0.5 * Math.max(0, Math.min(2, litBurners));
    }
}
