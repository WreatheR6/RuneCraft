package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.content.bosses.BossMechanics.YamaContract;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Yama's rewards (OSRS Wiki "Yama", Drops): each player who fought rolls on their own, scaled by their contribution (their
 * share of the damage dealt to Yama). The rolls go in order until one succeeds:
 * <ol>
 *   <li>less than 375 damage (15%): the junk table instead of everything else;</li>
 *   <li>1/120 for the unique table, scaled by contribution: an oathplate piece 3/5 (helm, chest or legs), the soulflame horn
 *       2/5;</li>
 *   <li>1/12 for a dossier (scaled by contribution);</li>
 *   <li>1/30 for the forgotten lockbox;</li>
 *   <li>1/15 for 12 oathplate shards (the quantity scales with contribution);</li>
 *   <li>the 78-slot standard table, whose quantities scale with contribution (supplies, the dragon plate and the tallow
 *       barrel don't); not rolled under any contract (the contract's own reward is given instead).</li>
 * </ol>
 * Tertiaries are rolled beside them: an elite clue 1/30 and Yami 1/2,500 (for anyone above the junk threshold).
 */
public final class YamaLoot {
    private YamaLoot() {
    }

    /** Damage below which a player only rolls junk: 375 of Yama's 2,500 hitpoints (15%). */
    public static final int JUNK_BELOW = 375;
    public static final double JUNK_SHARE = JUNK_BELOW / (double) BossMechanics.YAMA_HITPOINTS;
    public static final int UNIQUE = 120;
    public static final int DOSSIER = 12;
    public static final int LOCKBOX = 30;
    public static final int SHARDS_RATE = 15;
    public static final int SHARDS = 12;
    public static final int CLUE = 30;
    public static final int PET = 2500;
    public static final int MAIN_TABLE_WEIGHT = 78;
    /** The supply drop's share of the 78 slots: one of each pair below comes with it. */
    public static final int SUPPLY_WEIGHT = 15;

    /** An item and amount a player receives. */
    public record Loot(String item, int count) {
    }

    /** A standard-table line: its quantity at full contribution, its slots out of 78, and whether contribution scales it. */
    public record Line(String item, int quantity, int weight, boolean scales) {
    }

    /** The standard table besides the 15 supply slots (OSRS Wiki weights out of 78). */
    public static final List<Line> MAIN = List.of(
            new Line("rune_chainbody", 8, 5, true), new Line("battlestaff", 40, 4, true), new Line("rune_platebody", 8, 3, true),
            new Line("dragon_plateskirt", 1, 2, false), new Line("dragon_platelegs", 1, 2, false),
            new Line("blood_rune", 400, 3, true), new Line("law_rune", 150, 3, true), new Line("smoke_rune", 350, 2, true),
            new Line("soul_rune", 500, 2, true), new Line("soul_rune", 1000, 2, true), new Line("fire_rune", 40000, 1, true),
            new Line("wrath_rune", 800, 1, true),
            new Line("aether_catalyst", 850, 7, true), new Line("diabolic_worms", 90, 7, true),
            new Line("barrel_of_demonic_tallow", 1, 5, false), new Line("chasm_teleport_scroll", 6, 4, true),
            new Line("emerald", 40, 3, true), new Line("ruby", 40, 3, true), new Line("diamond", 40, 3, true),
            new Line("onyx_bolt_tips", 150, 1, true));

    /** The junk table (3/11 each, demon dung 2/11). */
    public static final List<Line> JUNK = List.of(new Line("cabbage", 1, 3, false), new Line("black_bead", 1, 3, false),
            new Line("spinach_roll", 1, 3, false), new Line("demon_dung", 1, 2, false));

    public static final List<String> OATHPLATE = List.of("oathplate_helm", "oathplate_chest", "oathplate_legs");

    /** A player's contribution: their share of Yama's hitpoints dealt, 0-1. */
    public static double contribution(int damage, int hitpoints) {
        return hitpoints <= 0 ? 0 : Math.max(0, Math.min(1.0, damage / (double) hitpoints));
    }

    /** A contribution-scaled quantity: the share of the full amount, at least 1. */
    public static int scaled(int quantity, double contribution) {
        return Math.max(1, (int) Math.floor(quantity * contribution));
    }

    /** Whether a player with this contribution only rolls junk (and no pet). */
    public static boolean junk(double contribution) {
        return contribution < JUNK_SHARE;
    }

    /**
     * Whether the standard table is rolled under this contract: with none, and under the contract of familiar acquisition
     * (OSRS Wiki: "If the player is fighting Yama under any contract other than the Contract of familiar acquisition, the
     * standard table will not be rolled").
     */
    public static boolean standardTable(YamaContract contract) {
        return contract == null || contract == YamaContract.FAMILIAR_ACQUISITION;
    }

    /** Each fighter's 1/n chance of Yami: 1/2,500, or 1/100 under the contract of familiar acquisition (both fighters). */
    public static int petRate(YamaContract contract) {
        return contract == YamaContract.FAMILIAR_ACQUISITION ? BossMechanics.FAMILIAR_PET_RATE : PET;
    }

    /**
     * The killer's extra pet roll under a contract: the boss engine rolls their usual 1/2,500 on the kill, so 1/n on top
     * brings the total to {@link #petRate} (0: none).
     */
    public static int killerExtraPetRate(YamaContract contract) {
        int rate = petRate(contract);
        if (rate >= PET) {
            return 0;
        }
        double p = (1.0 / rate - 1.0 / PET) / (1.0 - 1.0 / PET);
        return (int) Math.round(1.0 / p);
    }

    /**
     * One player's rewards for a kill: {@code contribution} is their share of Yama's hitpoints (0-1), {@code contract} the
     * fight's contract (null: none). The pet is left to the caller (1/{@link #PET} unless {@link #junk}).
     */
    public static List<Loot> roll(double contribution, YamaContract contract, Random random) {
        List<Loot> out = new ArrayList<>();
        if (junk(contribution)) {
            out.add(pick(JUNK, random, 1.0));
            return out;
        }
        if (random.nextDouble() < contribution / UNIQUE) {
            if (random.nextInt(5) < 3) {
                out.add(new Loot(OATHPLATE.get(random.nextInt(OATHPLATE.size())), 1));
            } else {
                out.add(new Loot("soulflame_horn", 1));
            }
        } else if (random.nextDouble() < contribution / DOSSIER) {
            out.add(new Loot("dossier", 1));
        } else if (random.nextInt(LOCKBOX) == 0) {
            out.add(new Loot("forgotten_lockbox", 1));
        } else if (random.nextInt(SHARDS_RATE) == 0) {
            out.add(new Loot("oathplate_shards", scaled(SHARDS, contribution)));
        } else if (standardTable(contract)) {
            out.addAll(standard(contribution, random));
        }
        if (random.nextInt(CLUE) == 0) {
            out.add(new Loot("clue_scroll_elite", 1));
        }
        return out;
    }

    /** One roll of the 78-slot standard table (the supply drop gives three items). */
    static List<Loot> standard(double contribution, Random random) {
        int r = random.nextInt(MAIN_TABLE_WEIGHT);
        if (r < SUPPLY_WEIGHT) {
            List<Loot> supplies = new ArrayList<>();
            supplies.add(new Loot(random.nextBoolean() ? "pineapple_pizza" : "wild_pie", 3 + random.nextInt(2)));
            supplies.add(random.nextBoolean() ? new Loot("prayer_potion(3)", 2) : new Loot("super_restore_mix", 2));
            supplies.add(random.nextBoolean() ? new Loot("super_combat_potion(1)", 1) : new Loot("zamorak_mix", 1));
            return supplies;
        }
        r -= SUPPLY_WEIGHT;
        for (Line l : MAIN) {
            r -= l.weight();
            if (r < 0) {
                return List.of(new Loot(l.item(), l.scales() ? scaled(l.quantity(), contribution) : l.quantity()));
            }
        }
        throw new IllegalStateException("standard table weights don't add up to " + MAIN_TABLE_WEIGHT);
    }

    private static Loot pick(List<Line> table, Random random, double contribution) {
        int total = 0;
        for (Line l : table) {
            total += l.weight();
        }
        int r = random.nextInt(total);
        for (Line l : table) {
            r -= l.weight();
            if (r < 0) {
                return new Loot(l.item(), l.scales() ? scaled(l.quantity(), contribution) : l.quantity());
            }
        }
        throw new IllegalStateException();
    }

    /**
     * Health shares at which Yama exiles his challengers to the Judge: 66.6% and 33.3%, or 75% and 50% under a contract
     * (the 29 May 2025 change). Percent of his health left.
     */
    public static double[] thresholds(YamaContract contract) {
        return contract == null ? new double[]{200 / 3.0, 100 / 3.0} : new double[]{75, 50};
    }

    /**
     * Whether a partner may join the fight under this contract (the catalyst, worm, shard and familiar acquisition
     * contracts).
     */
    public static boolean duoAllowed(YamaContract contract) {
        return contract == null || contract == YamaContract.CATALYST_ACQUISITION || contract == YamaContract.WORM_ACQUISITION
                || contract == YamaContract.SHARD_ACQUISITION || contract == YamaContract.FAMILIAR_ACQUISITION;
    }
}
