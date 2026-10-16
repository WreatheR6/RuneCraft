package com.runecraft.osrs.content.minigames;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * Guardians of the Rift energy, reward points and the Rewards Guardian, as on the OSRS Wiki
 * (Guardians of the Rift, Rewards Guardian, guardian stones).
 *
 * <p>Imbuing guardian essence at an elemental or catalytic altar gives one guardian stone of that
 * type per essence (combination runes give polyelemental stones). Each stone used on the Great
 * Guardian gives 2 energy of its type (3 for poly stones) and powers him; points and experience
 * depend on the essence crafted, not on the altar's tier. At the end of a closed rift, players
 * with at least 150 total energy get one point per 100 energy of each type, the remainder giving a
 * proportional chance of one more point. Points are kept until the player searches the Rewards
 * Guardian: one search costs one elemental and one catalytic point, or 25 abyssal pearls instead
 * (same table, pet included); "Big-search" does up to five at once.
 *
 * <p>Combination runes crafted at an elemental altar give polyelemental guardian stones (3 elemental
 * energy each). A portal talisman (1/250 per essence crafted at an altar, while the player doesn't
 * hold one for it) lets its holder enter that altar while it is inactive; it is consumed then.
 *
 * <p>Since November 2024 points and the end-of-game Runecraft XP need 300 total energy (150 before),
 * energy is capped at 1,000 per type and 1,200 in all per game, and pearls come 14-16 at a time.
 */
public final class GuardiansRewards {
    private GuardiansRewards() {
    }

    public static final int STONES_PER_ESSENCE = 1;
    public static final int ENERGY_PER_STONE = 2;
    public static final int ENERGY_PER_POLY_STONE = 3;
    /** Building a barrier gives 2 energy of each type. */
    public static final int BARRIER_ENERGY = 2;
    /** Minimum combined energy for points and the end-of-game XP (300 since November 2024). */
    public static final int MIN_ENERGY = 300;
    public static final int ENERGY_PER_POINT = 100;
    /** Energy cap per type per game. */
    public static final int ENERGY_CAP = 1000;
    /** Energy cap over both types per game. */
    public static final int TOTAL_ENERGY_CAP = 1200;
    /** The Great Guardian needs 250 guardian stones per player in the temple to reach 100% power. */
    public static final int STONES_PER_PLAYER = 250;
    /** Runecraft XP per guardian stone used on the Great Guardian (OSRS Wiki). */
    public static final double XP_PER_STONE = 5.0;
    /** Bonus Runecraft XP per level for a closed rift with 300+ energy (OSRS Wiki: level x 45). */
    public static final double COMPLETION_XP_PER_LEVEL = 45.0;

    /**
     * Adds energy to a player's {elemental, catalytic} totals within the caps (1,000 per type, 1,200 in
     * all). Returns false once the player is fully attuned and nothing more could be added.
     */
    public static boolean addEnergy(int[] energy, int elemental, int catalytic) {
        int before = energy[0] + energy[1];
        int el = Math.max(0, Math.min(elemental, ENERGY_CAP - energy[0]));
        el = Math.min(el, TOTAL_ENERGY_CAP - before);
        energy[0] += el;
        int cat = Math.max(0, Math.min(catalytic, ENERGY_CAP - energy[1]));
        cat = Math.min(cat, TOTAL_ENERGY_CAP - energy[0] - energy[1]);
        energy[1] += cat;
        return el + cat > 0 || elemental + catalytic == 0;
    }

    /** Energy from powering the Great Guardian with stones. */
    public static int stoneEnergy(int stones, int polyStones) {
        return stones * ENERGY_PER_STONE + polyStones * ENERGY_PER_POLY_STONE;
    }

    /** Guardian power (percent) added by one stone with {@code players} in the temple. */
    public static double powerPerStone(int players) {
        return 100.0 / (STONES_PER_PLAYER * Math.max(1, players));
    }

    /** True when a player's energy earns a reward at all. */
    public static boolean qualifies(int elemental, int catalytic) {
        return elemental + catalytic >= MIN_ENERGY;
    }

    /** Guaranteed points for one energy type (energy / 100, energy capped). */
    public static int points(int energy) {
        return Math.min(ENERGY_CAP, Math.max(0, energy)) / ENERGY_PER_POINT;
    }

    /** Points for one energy type, the remainder giving a chance of one more point. */
    public static int points(int energy, double random) {
        int e = Math.min(ENERGY_CAP, Math.max(0, energy));
        int base = e / ENERGY_PER_POINT;
        return random < (e % ENERGY_PER_POINT) / (double) ENERGY_PER_POINT ? base + 1 : base;
    }

    /** "Big-search": searches done per click. */
    public static final int BIG_SEARCH = 5;

    /** How a search is paid for. */
    public enum Payment { POINTS, PEARLS, NONE }

    /** Points first (one of each type), otherwise 25 abyssal pearls. */
    public static Payment payment(int elementalPoints, int catalyticPoints, int pearls) {
        if (elementalPoints >= 1 && catalyticPoints >= 1) {
            return Payment.POINTS;
        }
        return pearls >= PEARL_SEARCH_COST ? Payment.PEARLS : Payment.NONE;
    }

    // ------------------------------------------------------------------ polyelemental stones and portal talismans

    public static final String POLY_STONE = "polyelemental_guardian_stone";
    /** 1 in 250 per guardian essence crafted (OSRS Wiki, Mod Ash), only while none for that altar is held. */
    public static final int PORTAL_TALISMAN_CHANCE = 250;

    /** The portal talisman of a temple altar ("air" ... "blood"). */
    public static String portalTalisman(String altar) {
        return "portal_talisman_" + altar;
    }

    /**
     * Combination runes in the temple: altar, talisman, rune used per essence, result, Runecraft level, XP per
     * rune (OSRS Wiki, same as outside the temple). Approx: without the binding necklace modelled, each
     * essence has a 50% chance; a failed one is lost with its rune.
     */
    public static final Object[][] COMBINATIONS = {
            {"air", "water_talisman", "water_rune", "mist_rune", 6, 8.0},
            {"water", "air_talisman", "air_rune", "mist_rune", 6, 8.5},
            {"air", "earth_talisman", "earth_rune", "dust_rune", 10, 8.3},
            {"earth", "air_talisman", "air_rune", "dust_rune", 10, 9.0},
            {"water", "earth_talisman", "earth_rune", "mud_rune", 13, 9.3},
            {"earth", "water_talisman", "water_rune", "mud_rune", 13, 9.5},
            {"air", "fire_talisman", "fire_rune", "smoke_rune", 15, 8.5},
            {"fire", "air_talisman", "air_rune", "smoke_rune", 15, 9.5},
            {"water", "fire_talisman", "fire_rune", "steam_rune", 19, 9.5},
            {"fire", "water_talisman", "water_rune", "steam_rune", 19, 10.0},
            {"earth", "fire_talisman", "fire_rune", "lava_rune", 23, 10.0},
            {"fire", "earth_talisman", "earth_rune", "lava_rune", 23, 10.5}};
    /** Without a binding necklace each essence has a 50% chance; a worn necklace (one charge per altar click) makes it 100%. */
    public static final double COMBINATION_SUCCESS = 0.5;

    /** Success chance of a combination rune in the temple, with or without a binding necklace charge used. */
    public static double combinationSuccess(boolean binding) {
        return binding ? 1.0 : COMBINATION_SUCCESS;
    }

    /**
     * Aether runes in the temple (OSRS Wiki, Aether rune / Polycatalytic guardian stone): at the catalytic Cosmic altar, 90
     * Runecraft, one soul rune and one aether catalyst per rune, 20 XP each; a crafted rune gives a polycatalytic guardian
     * stone (3 catalytic energy). A failed rune keeps its catalyst.
     */
    public static final String AETHER_ALTAR = "cosmic";
    public static final int AETHER_LEVEL = 90;
    public static final double AETHER_XP = 20.0;
    public static final String AETHER_INPUT = "soul_rune";
    public static final String AETHER_CATALYST = "aether_catalyst";
    public static final String AETHER_RUNE = "aether_rune";
    public static final String POLYCATALYTIC_STONE = "polycatalytic_guardian_stone";
    public static final int ENERGY_PER_POLYCATALYTIC_STONE = 3;

    /** Catalytic energy from catalytic and polycatalytic stones. */
    public static int catalyticStoneEnergy(int stones, int polycatalytic) {
        return stones * ENERGY_PER_STONE + polycatalytic * ENERGY_PER_POLYCATALYTIC_STONE;
    }

    /** Searches affordable with stored points (one elemental + one catalytic point each). */
    public static int searches(int elementalPoints, int catalyticPoints) {
        return Math.max(0, Math.min(elementalPoints, catalyticPoints));
    }

    public static double completionXp(int runecraft) {
        return runecraft * COMPLETION_XP_PER_LEVEL;
    }

    // ------------------------------------------------------------------ Rewards Guardian

    /** A main-table line: item, quantity, weight out of 140 (out of 125 once every essence pouch is owned). */
    public record Line(String item, int min, int max, int weight) {
    }

    /** Main-table placeholder for the talisman sub-table (16/140). */
    public static final String TALISMANS = "#talismans";
    /** Main-table placeholder for an essence pouch the player doesn't own yet (15/140). */
    public static final String POUCH = "#pouch";

    /** The main table (OSRS Wiki, Rewards Guardian; rune weights 4 and 10, pearls 18, talismans 16, pouch 15, intricate 5). */
    public static final List<Line> MAIN = List.of(
            new Line("air_rune", 400, 500, 4), new Line("water_rune", 400, 500, 4), new Line("earth_rune", 400, 500, 4),
            new Line("fire_rune", 400, 500, 4), new Line("mind_rune", 250, 400, 4), new Line("body_rune", 80, 150, 4),
            new Line("chaos_rune", 61, 150, 10), new Line("cosmic_rune", 20, 30, 10), new Line("nature_rune", 28, 150, 10),
            new Line("law_rune", 5, 120, 10), new Line("death_rune", 5, 120, 10), new Line("blood_rune", 5, 120, 10),
            new Line("abyssal_pearls", 14, 16, 18), new Line(TALISMANS, 1, 1, 16), new Line(POUCH, 1, 1, 15),
            new Line("intricate_pouch", 1, 1, 5), new Line("abyssal_ashes", 1, 1, 1), new Line("needle", 1, 1, 1));
    /** Essence pouches the pouch line gives, smallest first; once all are owned the line is skipped (125 total). */
    public static final List<String> POUCHES = List.of("small_pouch", "medium_pouch", "large_pouch", "giant_pouch");

    /** The talisman sub-table, weights out of 35 (law and death need Troll Stronghold / Mourning's End Part II in OSRS). */
    public static final Object[][] TALISMAN_TABLE = {
            {"air_talisman", 3}, {"water_talisman", 3}, {"earth_talisman", 3}, {"fire_talisman", 3}, {"mind_talisman", 4},
            {"body_talisman", 4}, {"chaos_talisman", 4}, {"cosmic_talisman", 4}, {"nature_talisman", 4},
            {"elemental_talisman", 1}, {"law_talisman", 1}, {"death_talisman", 1}};

    /** The rare table, rolled before the main table on every search: item, 1 in N. */
    public static final Object[][] RARE = {
            {"atlaxs_diary", 20}, {"catalytic_talisman", 200}, {"abyssal_needle", 300}, {"abyssal_lantern", 700},
            {"abyssal_red_dye", 1200}, {"abyssal_green_dye", 1200}, {"abyssal_blue_dye", 1200}};
    /** Rare items rolled only while the player has never had one. */
    public static final List<String> ONCE_ONLY = List.of("atlaxs_diary", "abyssal_needle");
    /** Tertiary pet roll on every search. */
    public static final int PET_CHANCE = 4000;
    /** Abyssal pearls that buy one extra search. */
    public static final int PEARL_SEARCH_COST = 25;

    /** True when every essence pouch is owned (or the colossal pouch). */
    public static boolean allPouches(List<String> owned) {
        return owned.contains("colossal_pouch") || owned.containsAll(POUCHES);
    }

    /** Total main-table weight given the items already owned (140, or 125 with every pouch). */
    public static int mainTotal(List<String> owned) {
        int total = 0;
        for (Line l : MAIN) {
            if (!(l.item().equals(POUCH) && allPouches(owned))) {
                total += l.weight();
            }
        }
        return total;
    }

    /** Chance (0-1) of a main-table line per search (talismans and pouches by their placeholders). */
    public static double chance(String item, List<String> owned) {
        for (Line l : MAIN) {
            if (l.item().equals(item)) {
                return l.item().equals(POUCH) && allPouches(owned) ? 0 : l.weight() / (double) mainTotal(owned);
            }
        }
        return 0;
    }

    /**
     * One search. The rare table rolls first (the first hit is the reward, once-only items skipped when
     * owned); otherwise one main-table line. The pet is a separate 1/4,000 roll that comes on top. Runes
     * above the player's Runecraft level come in smaller stacks: amount x (level / required)^4, at least a
     * tenth of the rolled amount.
     */
    public static List<MinigameReward> search(int runecraft, List<String> owned, DoubleSupplier random) {
        List<MinigameReward> out = new ArrayList<>();
        if (random.getAsDouble() < 1.0 / PET_CHANCE) {
            out.add(new MinigameReward("abyssal_protector", 1));
        }
        for (Object[] r : RARE) {
            String item = (String) r[0];
            if (random.getAsDouble() < 1.0 / (Integer) r[1] && !(ONCE_ONLY.contains(item) && owned.contains(item))) {
                out.add(new MinigameReward(item, 1));
                return out;
            }
        }
        int roll = (int) (random.getAsDouble() * mainTotal(owned));
        Line hit = null;
        for (Line l : MAIN) {
            if (l.item().equals(POUCH) && allPouches(owned)) {
                continue;
            }
            roll -= l.weight();
            if (roll < 0) {
                hit = l;
                break;
            }
        }
        if (hit == null) {
            hit = MAIN.get(0);
        }
        if (hit.item().equals(TALISMANS)) {
            int t = (int) (random.getAsDouble() * 35);
            for (Object[] row : TALISMAN_TABLE) {
                t -= (Integer) row[1];
                if (t < 0) {
                    out.add(new MinigameReward((String) row[0], 1));
                    return out;
                }
            }
            out.add(new MinigameReward("air_talisman", 1));
            return out;
        }
        if (hit.item().equals(POUCH)) {
            for (String pouch : POUCHES) {
                if (!owned.contains(pouch)) {
                    out.add(new MinigameReward(pouch, 1));
                    return out;
                }
            }
        }
        int qty = hit.min() + (int) (random.getAsDouble() * (hit.max() - hit.min() + 1));
        Integer level = runeLevel(hit.item());
        if (level != null && runecraft < level) {
            qty = Math.max(Math.max(1, qty / 10), (int) Math.floor(qty * Math.pow(runecraft / (double) level, 4)));
        }
        out.add(new MinigameReward(hit.item(), qty));
        return out;
    }

    /** Runecraft level to craft a rune in the temple, or null when the item isn't a temple rune. */
    public static Integer runeLevel(String item) {
        for (Object[] a : MinigameRules.Guardians.ALTARS) {
            if (a[1].equals(item)) {
                return (Integer) a[2];
            }
        }
        return null;
    }

    /** Contents of an intricate pouch (approx.: a bigger rune, pearl or essence roll). */
    public static List<MinigameReward> openIntricatePouch(DoubleSupplier random) {
        List<MinigameReward> out = new ArrayList<>();
        if (random.getAsDouble() < 1.0 / 200) {
            out.add(new MinigameReward("blood_essence", 1));
        }
        String[] runes = {"nature_rune", "law_rune", "death_rune", "blood_rune", "cosmic_rune", "chaos_rune"};
        String rune = runes[(int) (random.getAsDouble() * runes.length)];
        out.add(new MinigameReward(rune, 50 + (int) (random.getAsDouble() * 101)));
        out.add(new MinigameReward("abyssal_pearls", 5 + (int) (random.getAsDouble() * 11)));
        out.add(new MinigameReward("pure_essence", 50 + (int) (random.getAsDouble() * 101)));
        return out;
    }

    /** Every item a search or intricate pouch can give. */
    public static List<String> allItems() {
        List<String> ids = new ArrayList<>();
        for (Line l : MAIN) {
            if (!l.item().startsWith("#")) {
                ids.add(l.item());
            }
        }
        for (Object[] r : RARE) {
            ids.add((String) r[0]);
        }
        for (Object[] r : TALISMAN_TABLE) {
            ids.add((String) r[0]);
        }
        ids.addAll(POUCHES);
        ids.addAll(List.of("abyssal_protector", "blood_essence", "pure_essence"));
        return ids;
    }
}
