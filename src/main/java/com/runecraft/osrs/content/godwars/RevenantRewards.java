package com.runecraft.osrs.content.godwars;

import com.runecraft.osrs.AttackType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * The revenants (OSRS Wiki, Revenants, the revenant pages and the "Killing revenants" money making guides): their
 * stats that the module table can't express, and their whole drop table.
 *
 * <p>A kill always drops 2 to {@code 2 * (floor(sqrt(level)) + 1)} revenant ether. Then either coins (1 to
 * {@code 25 * floor(sqrt(level)) + 1}; the chance falls with the revenant's level and is nil for the dragon) or one roll on
 * the main table (weapons, armour, resources, runes) together with one roll on the blighted supplies table. On top of that
 * a revenant rolls once down the rare chain (the first hit ends it): the unique table (amulet of avarice 2/5, each
 * Wilderness weapon 1/5), yew seeds, magic seeds, then the ancient artefacts and the ancient crystal; unskulled players
 * also have a level-scaled chance of a dragon med helm.
 *
 * <p>Every rare rate is a multiple of the unique table's {@code 1 / (176000 / (3 * floor(sqrt(level))))} (the wiki's rates
 * fit it to within 0.2%: imp 1/29,333.7, knight and dragon 1/5,333.4): yew and magic seeds and the totem 3/8 of it, the
 * crystal 1/2, the statuette 3/4, the medallion, effigy and relic 3/2 and the emblem 1/4. Being skulled multiplies every
 * one of these chances by 1/0.55 except the emblem's, which becomes 3/2 of the skulled unique chance (the wiki's knight:
 * emblem 1/1,333.3 unskulled, 1/4,400 skulled). A revenant Slayer task multiplies only the unique table by 5.
 */
public final class RevenantRewards {
    private RevenantRewards() {
    }

    // ---- the revenants ----

    /**
     * One revenant: combat level, hitpoints and max hit (wiki infobox / DPS calculator), its melee style, its respawn in
     * game ticks, how many the caves hold (wiki), and the coin weight that sets its chance of dropping coins instead of a
     * main table roll ({@code coinWeight / (coinWeight + 107)}: weights of the wiki template as oldschooljs copies them,
     * checked against the wiki's skulled revenant ork, 1/3.19).
     */
    public record Revenant(String id, int level, int hitpoints, int maxHit, AttackType melee, int respawnTicks, int count,
                           int coinWeight) {
        /** Chance a kill drops coins instead of rolling the main and supplies tables. */
        public double coinChance() {
            return coinWeight == 0 ? 0 : coinWeight / (double) (coinWeight + MAIN_ODDS_WEIGHT);
        }
    }

    /** The main table's weight against which a revenant's coin weight is set. */
    static final int MAIN_ODDS_WEIGHT = 107;

    public static final List<Revenant> REVENANTS = List.of(
            new Revenant("revenant_imp", 7, 10, 2, AttackType.STAB, 33, 4, 3145),
            new Revenant("revenant_goblin", 15, 14, 3, AttackType.CRUSH, 83, 2, 1745),
            new Revenant("revenant_pyrefiend", 52, 48, 5, AttackType.CRUSH, 60, 4, 332),
            new Revenant("revenant_hobgoblin", 60, 72, 8, AttackType.SLASH, 50, 2, 280),
            new Revenant("revenant_cyclops", 82, 110, 16, AttackType.CRUSH, 50, 2, 120),
            new Revenant("revenant_hellhound", 90, 80, 14, AttackType.STAB, 50, 2, 98),
            new Revenant("revenant_demon", 98, 80, 15, AttackType.CRUSH, 50, 4, 81),
            new Revenant("revenant_ork", 105, 105, 20, AttackType.SLASH, 27, 4, 49),
            new Revenant("revenant_dark_beast", 120, 140, 23, AttackType.CRUSH, 27, 2, 26),
            new Revenant("revenant_knight", 126, 143, 27, AttackType.SLASH, 20, 2, 8),
            new Revenant("revenant_dragon", 135, 155, 30, AttackType.SLASH, 20, 2, 0));

    /** The revenant with this creature id, or null. */
    public static Revenant revenant(String id) {
        for (Revenant r : REVENANTS) {
            if (r.id().equals(id)) {
                return r;
            }
        }
        return null;
    }

    /** True for the eleven revenants (not the maledictus). */
    public static boolean isRevenant(String creatureId) {
        return revenant(creatureId) != null;
    }

    // ---- healing (Revenants/Strategies, Mod Ash) ----

    /** Hitpoints a revenant restores per heal ("20, irrespective of their base HP"; never above its maximum). */
    public static final int HEAL_AMOUNT = 20;
    /** Game ticks after a heal before it can heal again. */
    public static final int HEAL_COOLDOWN_TICKS = 15;
    /** Fewest and most heals a revenant gets (uniform). */
    public static final int MIN_HEALS = 8;
    public static final int MAX_HEALS = 25;

    /** Whether a revenant heals instead of attacking: below half its hitpoints, off cooldown, with heals left. */
    public static boolean shouldHeal(double health, double maxHealth, long now, long lastHeal, int healsLeft) {
        return healsLeft > 0 && health < maxHealth / 2 && (lastHeal < 0 || now - lastHeal >= HEAL_COOLDOWN_TICKS);
    }

    /** Heals a revenant gets for its life ({@code random.applyAsInt(n)} is 0..n-1). */
    public static int heals(IntUnaryOperator random) {
        return MIN_HEALS + random.applyAsInt(MAX_HEALS - MIN_HEALS + 1);
    }

    // ---- attack style (Revenants/Strategies: "they react to a player's overhead prayers and defensive bonuses") ----

    /**
     * The style a revenant attacks with: never the one the player is praying against, otherwise the one the player's
     * defence bonus is lowest against (melee only within reach), magic first on a tie ("by default, all revenants attack
     * with Magic"). {@code protect} is the protected style ({@code null} when none; any melee style stands for melee).
     */
    public static AttackType chooseStyle(AttackType melee, boolean inMeleeReach, int meleeDefence, int rangedDefence,
                                         int magicDefence, AttackType protect) {
        boolean noMagic = protect == AttackType.MAGIC;
        boolean noRanged = protect == AttackType.RANGED;
        boolean noMelee = !inMeleeReach || (protect != null && protect.isMelee());
        AttackType best = null;
        int bestDef = Integer.MAX_VALUE;
        if (!noMagic) {
            best = AttackType.MAGIC;
            bestDef = magicDefence;
        }
        if (!noRanged && rangedDefence < bestDef) {
            best = AttackType.RANGED;
            bestDef = rangedDefence;
        }
        if (!noMelee && meleeDefence < bestDef) {
            best = melee;
        }
        if (best == null) {
            // Every usable style is prayed against: magic regardless (approx).
            return AttackType.MAGIC;
        }
        return best;
    }

    // ---- the drop table ----

    /** One line of a weighted table. */
    public record Line(String item, int min, int max, int weight) {
    }

    /**
     * The main table (wiki, "Killing revenants (Craw's bow)": the skulled revenant ork's lines), in half-weights so the
     * dragon platelegs and plateskirt (half the dragon dagger's chance) stay whole. Total 198.
     */
    public static final List<Line> MAIN = List.of(
            new Line("bracelet_of_ethereum", 1, 1, 30),
            new Line("battlestaff", 4, 4, 10),
            new Line("rune_full_helm", 2, 2, 4),
            new Line("rune_platebody", 2, 2, 4),
            new Line("rune_platelegs", 2, 2, 4),
            new Line("rune_kiteshield", 2, 2, 4),
            new Line("rune_warhammer", 2, 2, 4),
            new Line("dragon_platelegs", 1, 2, 1),
            new Line("dragon_plateskirt", 1, 2, 1),
            new Line("dragon_dagger", 2, 2, 2),
            new Line("dragon_longsword", 2, 2, 2),
            new Line("onyx_bolt_tips", 3, 6, 6),
            new Line("coal", 30, 60, 12),
            new Line("adamantite_bar", 4, 6, 12),
            new Line("runite_ore", 2, 4, 8),
            new Line("black_dragonhide", 4, 4, 8),
            new Line("yew_logs", 20, 40, 8),
            new Line("runite_bar", 2, 3, 6),
            new Line("mahogany_plank", 8, 16, 6),
            new Line("magic_logs", 8, 16, 4),
            new Line("uncut_dragonstone", 2, 5, 2),
            new Line("blighted_manta_ray", 10, 15, 8),
            new Line("blighted_super_restore", 1, 3, 6),
            new Line("revenant_cave_teleport", 1, 5, 10),
            new Line("law_rune", 20, 45, 10),
            new Line("death_rune", 30, 60, 10),
            new Line("blood_rune", 50, 100, 10),
            new Line("dragonstone_bolt_tips", 20, 40, 6));

    /** The blighted supplies table, rolled alongside the main table (same guide; total 100). */
    public static final List<Line> SUPPLIES = List.of(
            new Line("blighted_entangle_sack", 1, 10, 20),
            new Line("blighted_anglerfish", 1, 2, 15),
            new Line("blighted_manta_ray", 1, 2, 15),
            new Line("blighted_ancient_ice_sack", 1, 10, 10),
            new Line("blighted_teleport_spell_sack", 1, 10, 10),
            new Line("blighted_vengeance_sack", 1, 10, 10),
            new Line("blighted_karambwan", 1, 2, 10),
            new Line("blighted_super_restore", 1, 1, 5),
            new Line("revenant_cave_teleport", 1, 1, 5));

    static int totalWeight(List<Line> table) {
        int t = 0;
        for (Line l : table) {
            t += l.weight();
        }
        return t;
    }

    /** {@code floor(sqrt(level))}, at least 1. */
    static int root(int level) {
        return Math.max(1, (int) Math.floor(Math.sqrt(Math.max(1, level))));
    }

    /** Fewest and most revenant ether a kill drops (wiki: imp 2-6, dragon 2-24). */
    public static int[] ether(int level) {
        return new int[]{2, 2 * (root(level) + 1)};
    }

    /** Most coins a coin drop gives (wiki: ork 1-251). */
    public static int coinsMax(int level) {
        return 25 * root(level) + 1;
    }

    /** Wilderness weapons from the unique table (the wiki's "(u)" items are this mod's only form). */
    public static final List<String> WEAPONS = List.of("craws_bow", "thammarons_sceptre", "viggoras_chainmace");

    /** Rare rolls after the unique table, in the order the game checks them, as multiples of the unique chance. */
    private record Rare(String item, double timesUnique, int min, int max) {
    }

    private static final List<Rare> RARES = List.of(
            new Rare("yew_seed", 8.0 / 3, 2, 6),
            new Rare("magic_seed", 8.0 / 3, 2, 6),
            new Rare("ancient_emblem", 4.0, 1, 1),
            new Rare("ancient_totem", 8.0 / 3, 1, 1),
            new Rare("ancient_crystal", 2.0, 1, 1),
            new Rare("ancient_statuette", 4.0 / 3, 1, 1),
            new Rare("ancient_medallion", 2.0 / 3, 1, 1),
            new Rare("ancient_effigy", 2.0 / 3, 1, 1),
            new Rare("ancient_relic", 2.0 / 3, 1, 1));

    /** The ancient artefacts (emblem to relic), in value order. */
    public static final List<String> ARTEFACTS = List.of("ancient_emblem", "ancient_totem", "ancient_statuette", "ancient_medallion",
            "ancient_effigy", "ancient_relic");

    /** 1-in-N chance of the unique table (amulet of avarice or a Wilderness weapon) per kill. */
    public static double uniqueDenominator(int level, boolean skulled, boolean onTask) {
        double n = 176000.0 / (3 * root(level));
        if (skulled) {
            n *= 0.55;
        }
        if (onTask) {
            n /= 5;
        }
        return n;
    }

    /** 1-in-N chance of a rare (seed, artefact or ancient crystal) line per kill; a Slayer task doesn't change it. */
    public static double rareDenominator(String item, int level, boolean skulled) {
        for (Rare r : RARES) {
            if (r.item().equals(item)) {
                if (skulled && item.equals("ancient_emblem")) {
                    return uniqueDenominator(level, true, false) * 1.5;
                }
                return uniqueDenominator(level, skulled, false) / r.timesUnique();
            }
        }
        throw new IllegalArgumentException(item);
    }

    /** 1-in-N chance of an ancient artefact per kill (the rare chain's artefact lines). */
    public static double artefactDenominator(String item, int level, boolean skulled, boolean onTask) {
        if (!ARTEFACTS.contains(item)) {
            throw new IllegalArgumentException(item);
        }
        return rareDenominator(item, level, skulled);
    }

    /** 1-in-N chance of a dragon med helm per kill; 0 when skulled ("being skulled removes the dragon med helm"). */
    public static double medHelmDenominator(int level, boolean skulled) {
        return skulled ? 0 : 6769.2 / root(level); // wiki: knight and dragon 1/615.38
    }

    /**
     * One rolled item: id and count; {@code notable} is false for the rolls an amulet of avarice leaves unnoted (the
     * secondary supplies roll and the rare chain's uniques).
     */
    public record Rolled(String item, int count, boolean notable) {
        public Rolled(String item, int count) {
            this(item, count, true);
        }
    }

    /** Wilderness Chaos Temple (level 38), where the amulet of avarice also notes drops (approx: the temple's footprint). */
    public static final int[] CHAOS_TEMPLE = {2944, 3810, 2962, 3830};
    public static final String WILDERNESS_SLAYER_CAVE = "dng_wilderness_slayer_cave";

    /**
     * Whether an amulet of avarice notes an NPC's drop at this block (wiki: drops "from NPCs within the Revenant Caves,
     * Wilderness Slayer Cave and Chaos Temple", except the secondary supply rolls and uniques: {@link Rolled#notable}).
     */
    public static boolean avariceNotesAt(int blockX, int blockY, int blockZ) {
        if (RevenantCaves.contains(blockX, blockY, blockZ)
                || com.runecraft.osrs.content.dungeons.Dungeons.inside(WILDERNESS_SLAYER_CAVE, blockX, blockY, blockZ)) {
            return true;
        }
        int x = com.runecraft.osrs.GielinorMap.toOsrsX(blockX);
        int y = com.runecraft.osrs.GielinorMap.toOsrsY(blockZ);
        return blockY > com.runecraft.osrs.content.dungeons.Dungeons.DEEP_TOP && x >= CHAOS_TEMPLE[0] && x <= CHAOS_TEMPLE[2]
                && y >= CHAOS_TEMPLE[1] && y <= CHAOS_TEMPLE[3];
    }

    /**
     * The rare items of one kill (the unique table, then the rest of the rare chain: at most one of them) and the dragon
     * med helm. {@code random.applyAsInt(bound)} returns 0..bound-1. Rates are rolled out of 1,000,000 so the fractional
     * wiki denominators keep their precision.
     */
    public static List<String> roll(int level, boolean skulled, boolean onTask, IntUnaryOperator random) {
        List<String> out = new ArrayList<>();
        for (Rolled r : rollRares(level, skulled, onTask, random)) {
            out.add(r.item());
        }
        return out;
    }

    /** {@link #roll} with the seed counts. */
    public static List<Rolled> rollRares(int level, boolean skulled, boolean onTask, IntUnaryOperator random) {
        List<Rolled> out = new ArrayList<>();
        if (hit(uniqueDenominator(level, skulled, onTask), random)) {
            int r = random.applyAsInt(5);
            out.add(new Rolled(r < 2 ? "amulet_of_avarice" : WEAPONS.get(r - 2), 1, false));
        } else {
            for (Rare rare : RARES) {
                if (hit(rareDenominator(rare.item(), level, skulled), random)) {
                    out.add(new Rolled(rare.item(), between(rare.min(), rare.max(), random), false));
                    break;
                }
            }
        }
        double helm = medHelmDenominator(level, skulled);
        if (helm > 0 && hit(helm, random)) {
            out.add(new Rolled("dragon_med_helm", 1, false));
        }
        return out;
    }

    /**
     * The common part of one kill: the ether, then coins or a main table and a supplies table roll. ({@link #rollRares}
     * adds the rare chain.)
     */
    public static List<Rolled> rollCommon(Revenant rev, IntUnaryOperator random) {
        List<Rolled> out = new ArrayList<>();
        int[] e = ether(rev.level());
        out.add(new Rolled("revenant_ether", between(e[0], e[1], random)));
        if (rev.coinWeight() > 0 && random.applyAsInt(rev.coinWeight() + MAIN_ODDS_WEIGHT) < rev.coinWeight()) {
            out.add(new Rolled("coins", between(1, coinsMax(rev.level()), random)));
            return out;
        }
        out.add(pick(MAIN, random));
        Rolled supply = pick(SUPPLIES, random);
        out.add(new Rolled(supply.item(), supply.count(), false)); // the avarice amulet leaves the supplies roll unnoted
        return out;
    }

    private static Rolled pick(List<Line> table, IntUnaryOperator random) {
        int r = random.applyAsInt(totalWeight(table));
        for (Line l : table) {
            r -= l.weight();
            if (r < 0) {
                return new Rolled(l.item(), between(l.min(), l.max(), random));
            }
        }
        Line last = table.get(table.size() - 1);
        return new Rolled(last.item(), last.min());
    }

    private static int between(int min, int max, IntUnaryOperator random) {
        return max > min ? min + random.applyAsInt(max - min + 1) : min;
    }

    /** Chance (0-1) of a main/supplies table line per kill, for tests and the bestiary. */
    public static double lineChance(Revenant rev, List<Line> table, String item) {
        double sum = 0;
        for (Line l : table) {
            if (l.item().equals(item)) {
                sum += l.weight();
            }
        }
        return (1 - rev.coinChance()) * sum / totalWeight(table);
    }

    private static boolean hit(double denominator, IntUnaryOperator random) {
        return random.applyAsInt(1_000_000) < Math.round(1_000_000 / denominator);
    }

    // ---- ancient artefacts and Wilderness weapons ----

    /** Coins the Emblem Trader pays for an artefact (wiki: its value; 1 revenant ether per 500 coins when chiselled). */
    public static int artefactValue(String item) {
        return switch (item) {
            case "ancient_emblem" -> 500_000;
            case "ancient_totem" -> 1_000_000;
            case "ancient_statuette" -> 2_000_000;
            case "ancient_medallion" -> 4_000_000;
            case "ancient_effigy" -> 8_000_000;
            case "ancient_relic" -> 16_000_000;
            default -> 0;
        };
    }

    /** Revenant ether a chisel breaks an artefact into (wiki: one per 500 coins of its value). */
    public static int artefactEther(String item) {
        return artefactValue(item) / 500;
    }

    /** Revenant ether that activates a Wilderness weapon (wiki); it is lost, not fired, and gone on a Wilderness death. */
    public static final int ACTIVATION_ETHER = 1_000;
    /** Revenant ether an uncharged Wilderness weapon dismantles into (wiki, Craw's bow: 7,500). */
    public static final int DISMANTLE_ETHER = 7_500;

    // ---- Revenant maledictus (OSRS wiki): a demi-boss that may appear when any revenant is slain ----

    public static final String MALEDICTUS = "revenant_maledictus";
    /** Divisor of the accumulated revenant combat levels. // approx: Jagex's flat rate is not published */
    public static final double MALEDICTUS_RATE = 1_000_000;
    /** After it appears the chance resets and it can't appear again for 45 minutes. */
    public static final int MALEDICTUS_COOLDOWN_TICKS = 4500;
    /** It leaves after about 10 minutes if not killed (wiki: citation needed). */
    public static final int MALEDICTUS_DESPAWN_TICKS = 1000;
    /** Failsafe: it is guaranteed if it hasn't appeared for this long. // approx: "a decent length of time" */
    public static final int MALEDICTUS_FAILSAFE_TICKS = 18000;
    /** The top damager rolls the revenant dragon's table this many times, besides an ancient emblem or totem. */
    public static final int MALEDICTUS_DRAGON_ROLLS = 2;
    public static final int REVENANT_DRAGON_LEVEL = 135;
    /** Forinthry Surge (wiki): the maledictus's top damager wearing an avarice gets +15% more against revenants for 30 minutes. */
    public static final int FORINTHRY_SURGE_TICKS = 3000;

    /** Chance of the maledictus appearing on a kill, from the revenant combat levels slain since it last appeared. */
    public static double maledictusChance(long accumulatedLevels) {
        return Math.min(1.0, accumulatedLevels / MALEDICTUS_RATE);
    }

    /** Whether a slain creature counts towards the maledictus (any revenant but the maledictus itself). */
    public static boolean countsForMaledictus(String creatureId) {
        return creatureId != null && creatureId.startsWith("revenant_") && !creatureId.equals(MALEDICTUS);
    }
}
