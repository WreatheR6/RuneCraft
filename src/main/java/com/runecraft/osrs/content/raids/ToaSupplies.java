package com.runecraft.osrs.content.raids;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Tombs of Amascut supplies (OSRS wiki, "Supplies (Tombs of Amascut)" and "Helpful Spirit"): the Helpful Spirit
 * offers one of three bundles after the second and after the fourth path; the Helpful Spirit invocations cut the
 * quantities to 66%, 33% or 10% (at least one of each supply), On a Diet removes the silk dressings. The supplies only
 * work inside the raid and are taken away when you leave it. Honey locusts are handed out when the party wipes.
 */
public final class ToaSupplies {
    private ToaSupplies() {
    }

    public static final String NECTAR = "nectar";
    public static final String TEARS = "tears_of_elidinis";
    public static final String AMBROSIA = "ambrosia";
    public static final String SILK_DRESSING = "silk_dressing";
    public static final String CRYSTAL_SCARAB = "blessed_crystal_scarab";
    public static final String SMELLING_SALTS = "smelling_salts";
    public static final String ADRENALINE = "liquid_adrenaline";
    public static final String HONEY_LOCUST = "honey_locust";

    /** Every supply item (all raid-only). */
    public static final List<String> ITEMS = List.of(NECTAR, TEARS, AMBROSIA, SILK_DRESSING, CRYSTAL_SCARAB, SMELLING_SALTS,
            ADRENALINE, HONEY_LOCUST);

    /** Combat achievement flag set on the party when anyone eats or drinks something brought into the tomb. */
    public static final String OUTSIDE_SUPPLY_FLAG = com.runecraft.osrs.content.systems.CombatAchievements.FLAG_TOA_OUTSIDE_SUPPLY;

    /**
     * Whether a consumable was given inside the tomb (CA 424 "You are not prepared", OSRS: only the Helpful Spirit's
     * supplies and the honey locusts handed out after a wipe).
     */
    public static boolean givenInsideTomb(String id) {
        return ITEMS.contains(id);
    }

    /** The Helpful Spirit's three bundles. */
    public enum Bundle {
        LIFE("Life", "nectar, tears of Elidinis, silk dressings, blessed crystal scarabs and ambrosia"),
        CHAOS("Chaos", "a random mix of nectar, tears, smelling salts and, rarely, ambrosia or liquid adrenaline"),
        POWER("Power", "two smelling salts and a liquid adrenaline");

        private final String displayName;
        private final String contents;

        Bundle(String displayName, String contents) {
            this.displayName = displayName;
            this.contents = contents;
        }

        public String displayName() {
            return displayName;
        }

        public String contents() {
            return contents;
        }

        public static Bundle byName(String name) {
            for (Bundle b : values()) {
                if (b.name().equalsIgnoreCase(name) || b.displayName.equalsIgnoreCase(name)) {
                    return b;
                }
            }
            return null;
        }
    }

    /** Doses (uses) a fresh supply item has: nectar and tears 4, the others 2, honey locusts 0 (stackable food). */
    public static int doses(String id) {
        return switch (id) {
            case NECTAR, TEARS -> 4;
            case HONEY_LOCUST -> 0;
            default -> 2;
        };
    }

    /** Share of the supplies the Helpful Spirit still offers (OSRS: 66% / 33% / 10% with the Helpful Spirit group). */
    public static double supplyFactor(Set<Invocation> invocations) {
        if (invocations.contains(Invocation.NO_HELP_NEEDED)) {
            return 0.10;
        }
        if (invocations.contains(Invocation.NEED_LESS_HELP)) {
            return 0.33;
        }
        return invocations.contains(Invocation.NEED_SOME_HELP) ? 0.66 : 1.0;
    }

    /**
     * A reduced quantity: never below one of a supply that was offered (OSRS). The wiki notes that "the reduction may or
     * may not occur" (a Power bundle's two salts at 66% are sometimes still two), so the fraction is rounded up with its
     * own probability: 2 x 0.66 = 1.32 gives 2 a third of the time. // approx: the rounding odds
     */
    public static int reduce(int count, double factor, Random random) {
        if (count <= 0) {
            return 0;
        }
        double exact = count * factor;
        int whole = (int) Math.floor(exact + 1e-9);
        if (random.nextDouble() < exact - whole) {
            whole++;
        }
        return Math.max(1, Math.min(count, whole));
    }

    /** Paths completed when the Helpful Spirit appears: after two paths and after all four (OSRS). */
    public static boolean offeredAfter(int pathsCompleted) {
        return pathsCompleted == 2 || pathsCompleted == 4;
    }

    /** Chance of the rare ambrosia or liquid adrenaline in a Chaos bundle. // approx: "rare" */
    static final int CHAOS_RARE = 8;

    /** The items (item id to count) a bundle holds for this raid's invocations. */
    public static Map<String, Integer> bundle(Bundle bundle, Set<Invocation> invocations, Random random) {
        Map<String, Integer> raw = new LinkedHashMap<>();
        switch (bundle) {
            case LIFE -> {
                raw.put(NECTAR, 5);
                raw.put(TEARS, 5);
                if (invocations.contains(Invocation.ON_A_DIET)) {
                    // OSRS: the dressings are replaced by two scarabs and ambrosia.
                    raw.put(CRYSTAL_SCARAB, 2);
                } else {
                    raw.put(SILK_DRESSING, 1 + random.nextInt(3));
                    raw.put(CRYSTAL_SCARAB, 1 + random.nextInt(3));
                }
                raw.put(AMBROSIA, 1 + random.nextInt(2));
            }
            case CHAOS -> {
                raw.put(NECTAR, 1 + random.nextInt(8));
                raw.put(TEARS, random.nextInt(7));
                raw.put(SMELLING_SALTS, random.nextInt(3));
                raw.put(AMBROSIA, random.nextInt(CHAOS_RARE) == 0 ? 1 : 0);
                raw.put(ADRENALINE, random.nextInt(CHAOS_RARE) == 0 ? 1 : 0);
            }
            case POWER -> {
                raw.put(SMELLING_SALTS, 2);
                raw.put(ADRENALINE, 1);
            }
        }
        double factor = supplyFactor(invocations);
        Map<String, Integer> out = new LinkedHashMap<>();
        raw.forEach((id, n) -> {
            int r = reduce(n, factor, random);
            if (r > 0) {
                out.put(id, r);
            }
        });
        return out;
    }

    /** Honey locusts each player is given when the party wipes (none with On a Diet). // approx: 2 */
    public static int honeyLocustsOnWipe(Set<Invocation> invocations) {
        return invocations.contains(Invocation.ON_A_DIET) ? 0 : 2;
    }

    // ------------------------------------------------------------------ effects (OSRS wiki formulas)

    /** Nectar heals floor(level x 15 / 100) + 3 and may overheal by that much. */
    public static int nectarHeal(int hitpointsLevel) {
        return hitpointsLevel * 15 / 100 + 3;
    }

    /** Hitpoints after a nectar dose: + heal, capped at level + heal. */
    public static int afterNectar(int current, int hitpointsLevel) {
        int heal = nectarHeal(hitpointsLevel);
        return Math.max(current, Math.min(current + heal, hitpointsLevel + heal));
    }

    /** Nectar lowers Attack, Strength, Defence, Ranged and Magic by floor(current / 20 + 5). */
    public static int nectarDrain(int currentLevel) {
        return currentLevel / 20 + 5;
    }

    /** Ambrosia restores Hitpoints and boosts them by floor(level / 4 + 2). */
    public static int ambrosiaHitpointsBoost(int hitpointsLevel) {
        return hitpointsLevel / 4 + 2;
    }

    /** Ambrosia restores Prayer and boosts it by floor(level / 5 + 5). */
    public static int ambrosiaPrayerBoost(int prayerLevel) {
        return prayerLevel / 5 + 5;
    }

    /** Smelling salts boost Attack, Strength, Defence, Ranged and Magic by floor(level x 16 / 100) + 11. */
    public static int saltsBoost(int level) {
        return level * 16 / 100 + 11;
    }

    /** Smelling salts re-apply their boost every 25 game ticks (15 s) for 800 ticks (8 minutes). */
    public static final int SALTS_EVERY = 25;
    public static final int SALTS_TICKS = 800;
    /** Run energy (hundredths of a percent) each smelling salts pulse restores: 25%. */
    public static final int SALTS_RUN_ENERGY = 2500;

    /** Tears of Elidinis restore combat stats by floor(level / 4 + 3). */
    public static int tearsRestore(int level) {
        return level / 4 + 3;
    }

    /** Tears of Elidinis restore floor(level x 25 / 100) + 10 Prayer to the drinker. */
    public static int tearsPrayer(int prayerLevel) {
        return prayerLevel * 25 / 100 + 10;
    }

    /** ... and floor(level x 10 / 100) + 10 to party members within one tile. */
    public static int tearsPrayerShared(int prayerLevel) {
        return prayerLevel * 10 / 100 + 10;
    }

    /** A honey locust heals 20 (overhealing) and restores a prayer potion dose and a super restore dose. */
    public static final int HONEY_LOCUST_HEAL = 20;

    public static int honeyLocustPrayer(int prayerLevel) {
        return prayerLevel / 4 + 7;
    }

    public static int honeyLocustRestore(int level) {
        return level / 4 + 8;
    }

    /** Silk dressing: 5 hitpoints every 5 game ticks, 20 times, up to 5 above the Hitpoints level. */
    public static final int SILK_HEAL = 5;
    public static final int SILK_EVERY = 5;
    public static final int SILK_TIMES = 20;
    public static final int SILK_OVERHEAL = 5;

    /** Blessed crystal scarab: 8 Prayer points every 4 game ticks, 9 times. */
    public static final int SCARAB_PRAYER = 8;
    public static final int SCARAB_EVERY = 4;
    public static final int SCARAB_TIMES = 9;

    /** Liquid adrenaline halves special attack costs for 150 seconds (250 game ticks). */
    public static final int ADRENALINE_TICKS = 250;

    /**
     * Special attack energy a special costs in ToA (OSRS): 100% with Overly Draining, halved while liquid adrenaline
     * is active (so 50% with Overly Draining), or -1 to keep the weapon's own cost. // approx: OSRS keeps half
     * percents (energy is out of 1000); rounded up here
     */
    public static int specialCost(int baseCost, boolean overlyDraining, boolean adrenaline) {
        if (!overlyDraining && !adrenaline) {
            return -1;
        }
        int cost = overlyDraining ? 100 : baseCost;
        return adrenaline ? (cost + 1) / 2 : cost;
    }
}
