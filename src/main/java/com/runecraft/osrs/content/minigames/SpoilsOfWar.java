package com.runecraft.osrs.content.minigames;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * Soul Wars spoils of war (30 Zeal Tokens at Nomad's shop; OSRS Wiki, since the July 2025 rebalance):
 * three rolls on a nine-line table (1/9 each), plus a 1/400 tertiary roll for Lil' creator.
 */
public final class SpoilsOfWar {
    public static final String ITEM = "spoils_of_war";
    public static final String PET = "lil_creator";
    public static final int ROLLS = 3;
    public static final int PET_CHANCE = 400;
    public static final int ZEAL_COST = 30;

    public record Line(String item, int min, int max, boolean noted) {
    }

    public static final List<Line> TABLE = List.of(
            new Line("pure_essence", 1000, 1000, true),
            new Line("adamant_bolts", 150, 170, false),
            new Line("rune_bolts", 150, 170, false),
            new Line("blighted_vengeance_sack", 20, 30, false),
            new Line("blighted_ancient_ice_sack", 10, 15, false),
            new Line("blighted_karambwan", 35, 45, true),
            new Line("blighted_anglerfish", 25, 35, true),
            new Line("blighted_manta_ray", 15, 25, true),
            new Line("blighted_super_restore", 4, 4, true));

    /** One opened prize. */
    public record Prize(String item, int amount, boolean noted) {
    }

    private SpoilsOfWar() {
    }

    public static List<Prize> open(DoubleSupplier random) {
        List<Prize> out = new ArrayList<>();
        for (int i = 0; i < ROLLS; i++) {
            Line line = TABLE.get(Math.min(TABLE.size() - 1, (int) (random.getAsDouble() * TABLE.size())));
            int n = line.min() + (int) (random.getAsDouble() * (line.max() - line.min() + 1));
            out.add(new Prize(line.item(), Math.min(line.max(), n), line.noted()));
        }
        if (random.getAsDouble() * PET_CHANCE < 1) {
            out.add(new Prize(PET, 1, false));
        }
        return out;
    }
}
