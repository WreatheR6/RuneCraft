package com.runecraft.osrs.content.raids;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Raid reward tables and the end-of-raid loot roll (pure Java, keyed by player id strings).
 * Unique weights, common tables and quantity formulas follow the OSRS wiki's loot calculators
 * (Module:Chambers of Xeric calculator, Module:Theatre of Blood calculator, Module:Tombs of Amascut loot).
 */
public final class RaidRewards {
    /** One weighted unique. */
    public record Unique(String item, int weight) {
    }

    /**
     * Points-based common (CoX and ToA): quantity = personal points / divisor. {@code weight} is the
     * item's share of the CoX resource table (out of 99); ToA's 27 commons are equally likely.
     */
    public record PointsCommon(String item, int divisor, int weight) {
        public PointsCommon(String item, int divisor) {
            this(item, divisor, 1);
        }
    }

    /**
     * ToB common: the quantity is {@code floor(min x mode multiplier x team points ratio)} up to 1.2x that
     * (see {@link RaidScaling#tobCommonRange}); {@code weight} is out of 30.
     */
    public record RangeCommon(String item, int min, int weight) {
        public int max() {
            return (int) Math.floor(min * 1.2);
        }
    }

    /** A mode reward rolled once per completion at 1/{@code rate}; some need the target time. */
    public record ExtraReward(String item, int rate, boolean needsTargetTime) {
    }

    public record Loot(String item, int count) {
    }

    /** The outcome of a raid's reward roll. */
    public record Result(Map<String, List<Loot>> loot, List<String> purpleRecipients, List<String> petRecipients, double purpleChance) {
        public List<Loot> lootFor(String player) {
            return loot.getOrDefault(player, List.of());
        }

        public boolean purple(String player) {
            return purpleRecipients.contains(player);
        }
    }

    /**
     * CoX uniques (OSRS, current weights out of 60: prayer scrolls 14 each, ancestral pieces 4 each,
     * twisted bow, kodai insignia and elder maul 2 each).
     */
    public static final List<Unique> COX_UNIQUES = List.of(
            new Unique("dexterous_prayer_scroll", 14),
            new Unique("arcane_prayer_scroll", 14),
            new Unique("twisted_buckler", 4),
            new Unique("dragon_hunter_crossbow", 4),
            new Unique("dinhs_bulwark", 3),
            new Unique("ancestral_hat", 4),
            new Unique("ancestral_robe_top", 4),
            new Unique("ancestral_robe_bottom", 4),
            new Unique("dragon_claws", 3),
            new Unique("elder_maul", 2),
            new Unique("kodai_insignia", 2),
            new Unique("twisted_bow", 2));
    /** CoX Challenge Mode uniques (OSRS: prayer scrolls drop to 12 each, total 56). */
    public static final List<Unique> COX_CM_UNIQUES = List.of(
            new Unique("dexterous_prayer_scroll", 12),
            new Unique("arcane_prayer_scroll", 12),
            new Unique("twisted_buckler", 4),
            new Unique("dragon_hunter_crossbow", 4),
            new Unique("dinhs_bulwark", 3),
            new Unique("ancestral_hat", 4),
            new Unique("ancestral_robe_top", 4),
            new Unique("ancestral_robe_bottom", 4),
            new Unique("dragon_claws", 3),
            new Unique("elder_maul", 2),
            new Unique("kodai_insignia", 2),
            new Unique("twisted_bow", 2));
    /** ToB uniques (OSRS normal mode, total 19). */
    public static final List<Unique> TOB_UNIQUES = List.of(
            new Unique("avernic_defender_hilt", 8),
            new Unique("ghrazi_rapier", 2),
            new Unique("sanguinesti_staff", 2),
            new Unique("justiciar_faceguard", 2),
            new Unique("justiciar_chestguard", 2),
            new Unique("justiciar_legguards", 2),
            new Unique("scythe_of_vitur", 1));
    /** ToB Hard mode uniques (OSRS: the avernic hilt drops to 7, total 18). */
    public static final List<Unique> TOB_HARD_UNIQUES = List.of(
            new Unique("avernic_defender_hilt", 7),
            new Unique("ghrazi_rapier", 2),
            new Unique("sanguinesti_staff", 2),
            new Unique("justiciar_faceguard", 2),
            new Unique("justiciar_chestguard", 2),
            new Unique("justiciar_legguards", 2),
            new Unique("scythe_of_vitur", 1));
    /** ToA uniques up to raid level 300 (OSRS, total 240). */
    public static final List<Unique> TOA_UNIQUES = List.of(
            new Unique("osmumtens_fang", 70),
            new Unique("lightbearer", 70),
            new Unique("elidinis_ward", 30),
            new Unique("masori_mask", 20),
            new Unique("masori_body", 20),
            new Unique("masori_chaps", 20),
            new Unique("tumekens_shadow", 10));

    /** CoX resource table (OSRS): two different items, quantity personal points / divisor; weights total 99. */
    public static final List<PointsCommon> COX_COMMONS = List.of(
            new PointsCommon("death_rune", 36, 3), new PointsCommon("blood_rune", 32, 3), new PointsCommon("soul_rune", 20, 3),
            new PointsCommon("rune_arrow", 14, 3), new PointsCommon("dragon_arrow", 202, 3),
            new PointsCommon("grimy_ranarr_weed", 946, 2), new PointsCommon("grimy_toadflax", 624, 2),
            new PointsCommon("grimy_irit_leaf", 194, 2), new PointsCommon("grimy_avantoe", 389, 2),
            new PointsCommon("grimy_kwuarm", 454, 2), new PointsCommon("grimy_snapdragon", 1560, 2),
            new PointsCommon("grimy_cadantine", 396, 2), new PointsCommon("grimy_lantadyme", 297, 2),
            new PointsCommon("grimy_dwarf_weed", 240, 2), new PointsCommon("grimy_torstol", 972, 2),
            new PointsCommon("ranarr_seed", 6622, 1), new PointsCommon("toadflax_seed", 4992, 1), new PointsCommon("irit_seed", 1552, 1),
            new PointsCommon("avantoe_seed", 3112, 1), new PointsCommon("kwuarm_seed", 3632, 1),
            new PointsCommon("snapdragon_seed", 10920, 1), new PointsCommon("cadantine_seed", 3168, 1),
            new PointsCommon("lantadyme_seed", 2376, 1), new PointsCommon("dwarf_weed_seed", 1920, 1),
            new PointsCommon("torstol_seed", 6804, 1),
            new PointsCommon("silver_ore", 20, 3), new PointsCommon("coal", 20, 3), new PointsCommon("gold_ore", 44, 3),
            new PointsCommon("mithril_ore", 32, 3), new PointsCommon("adamantite_ore", 166, 3), new PointsCommon("runite_ore", 2000, 3),
            new PointsCommon("uncut_sapphire", 188, 3), new PointsCommon("uncut_emerald", 142, 3), new PointsCommon("uncut_ruby", 242, 3),
            new PointsCommon("uncut_diamond", 508, 3),
            new PointsCommon("lizardman_fang", 28, 3), new PointsCommon("pure_essence", 2, 3), new PointsCommon("saltpetre", 28, 3),
            new PointsCommon("teak_plank", 96, 3), new PointsCommon("mahogany_plank", 238, 3), new PointsCommon("dynamite", 54, 3),
            new PointsCommon("torn_prayer_scroll", 1, 3), new PointsCommon("dark_relic", 1, 3));

    /** ToB commons (OSRS, three rolls, weights out of 30). */
    public static final List<RangeCommon> TOB_COMMONS = List.of(
            new RangeCommon("vial_of_blood", 50, 2), new RangeCommon("death_rune", 500, 1),
            new RangeCommon("blood_rune", 500, 1), new RangeCommon("swamp_tar", 500, 1), new RangeCommon("coal", 500, 1),
            new RangeCommon("gold_ore", 300, 1), new RangeCommon("molten_glass", 200, 1), new RangeCommon("adamantite_ore", 130, 1),
            new RangeCommon("runite_ore", 60, 1), new RangeCommon("wine_of_zamorak", 50, 1), new RangeCommon("potato_cactus", 50, 1),
            new RangeCommon("grimy_cadantine", 50, 1), new RangeCommon("grimy_avantoe", 40, 1),
            new RangeCommon("grimy_toadflax", 37, 1), new RangeCommon("grimy_kwuarm", 36, 1),
            new RangeCommon("grimy_irit_leaf", 34, 1), new RangeCommon("grimy_ranarr_weed", 30, 1),
            new RangeCommon("grimy_snapdragon", 27, 1), new RangeCommon("grimy_lantadyme", 26, 1),
            new RangeCommon("grimy_dwarf_weed", 24, 1), new RangeCommon("grimy_torstol", 20, 1),
            new RangeCommon("battlestaff", 15, 1), new RangeCommon("mahogany_seed", 10, 1),
            new RangeCommon("rune_battleaxe", 4, 1), new RangeCommon("rune_platebody", 4, 1), new RangeCommon("rune_chainbody", 4, 1),
            new RangeCommon("palm_seed", 3, 1), new RangeCommon("yew_seed", 3, 1), new RangeCommon("magic_seed", 3, 1));

    /** ToA commons (OSRS): three rolls, all 27 equally likely; quantity from {@link RaidScaling#toaCommonQuantity}. */
    public static final List<PointsCommon> TOA_COMMONS = List.of(
            new PointsCommon("coins", 1), new PointsCommon("death_rune", 20), new PointsCommon("soul_rune", 40),
            new PointsCommon("gold_ore", 90), new PointsCommon("dragon_dart_tip", 100), new PointsCommon("mahogany_logs", 180),
            new PointsCommon("sapphire", 200), new PointsCommon("emerald", 250), new PointsCommon("gold_bar", 250),
            new PointsCommon("potato_cactus", 250), new PointsCommon("raw_shark", 250), new PointsCommon("ruby", 300),
            new PointsCommon("diamond", 400), new PointsCommon("raw_manta_ray", 450), new PointsCommon("cactus_spine", 600),
            new PointsCommon("dragonstone", 600), new PointsCommon("battlestaff", 1100), new PointsCommon("coconut_milk", 1100),
            new PointsCommon("lily_of_the_sands", 1100), new PointsCommon("toadflax_seed", 2000), new PointsCommon("ranarr_seed", 2500),
            new PointsCommon("torstol_seed", 3200), new PointsCommon("snapdragon_seed", 3200), new PointsCommon("dragon_med_helm", 4000),
            new PointsCommon("magic_seed", 6500), new PointsCommon("blood_essence", 7500), new PointsCommon("cache_of_runes", 999_999));

    /** CoX Challenge Mode extras (OSRS): metamorphic dust 1/400; the twisted ancestral colour kit 1/75 within the target time. */
    public static final List<ExtraReward> COX_CM_EXTRAS = List.of(
            new ExtraReward("metamorphic_dust", 400, false),
            new ExtraReward("twisted_ancestral_colour_kit", 75, true));
    /** ToB Hard mode extras within the challenge time (OSRS): holy kit 1/100, sanguine kit 1/150, sanguine dust 1/275. */
    public static final List<ExtraReward> TOB_HARD_EXTRAS = List.of(
            new ExtraReward("holy_ornament_kit", 100, true),
            new ExtraReward("sanguine_ornament_kit", 150, true),
            new ExtraReward("sanguine_dust", 275, true));

    /** Olmlet chance per CoX unique (OSRS: 1/53). */
    public static final int OLMLET_RATE = 53;
    /** Lil' Zik chance per ToB completion (OSRS normal mode: 1/650). */
    public static final int LIL_ZIK_RATE = 650;
    /** Lil' Zik chance per ToB hard mode completion (OSRS: 1/500). */
    public static final int LIL_ZIK_HARD_RATE = 500;
    /** CoX resource table rolls per player. */
    public static final int COX_COMMON_ROLLS = 2;
    /** ToB and ToA common rolls per player. */
    public static final int TOB_COMMON_ROLLS = 3;
    public static final int TOA_COMMON_ROLLS = 3;
    /** ToA weights are kept as multiples of 1/50 so the 50x rarer low-level weights stay exact. */
    static final int TOA_WEIGHT_SCALE = 50;

    private RaidRewards() {
    }

    public static int totalWeight(List<Unique> table) {
        int t = 0;
        for (Unique u : table) {
            t += u.weight();
        }
        return t;
    }

    /** Uniques for a raid (normal mode). */
    public static List<Unique> uniques(RaidType type) {
        return uniques(type, RaidMode.NORMAL);
    }

    /** Uniques for a raid and mode. */
    public static List<Unique> uniques(RaidType type, RaidMode mode) {
        return switch (type) {
            case COX -> mode == RaidMode.CHALLENGE ? COX_CM_UNIQUES : COX_UNIQUES;
            case TOB -> mode == RaidMode.HARD ? TOB_HARD_UNIQUES : TOB_UNIQUES;
            case TOA -> TOA_UNIQUES;
        };
    }

    /** Mode-only extra rewards (CoX Challenge Mode, ToB Hard mode). */
    public static List<ExtraReward> extras(RaidType type, RaidMode mode) {
        if (type == RaidType.COX && mode == RaidMode.CHALLENGE) {
            return COX_CM_EXTRAS;
        }
        if (type == RaidType.TOB && mode == RaidMode.HARD) {
            return TOB_HARD_EXTRAS;
        }
        return List.of();
    }

    public static String pet(RaidType type) {
        return switch (type) {
            case COX -> "olmlet";
            case TOB -> "lil_zik";
            case TOA -> "tumekens_guardian";
        };
    }

    /**
     * ToA unique weight in fiftieths at a raid level (OSRS). Every unique is 50x rarer below its minimum
     * level (150 for the shadow, ward and Masori armour, 50 for the fang and Lightbearer); above raid
     * level 300 the fang and Lightbearer weights fall (summer 2025 reweight), down to 30 and 35 at 500+.
     */
    public static int toaWeight(Unique u, int raidLevel) {
        int weight = u.weight();
        boolean fang = u.item().equals("osmumtens_fang");
        boolean lightbearer = u.item().equals("lightbearer");
        if (fang || lightbearer) {
            weight = toaReweight(fang, raidLevel);
        }
        int w = weight * TOA_WEIGHT_SCALE;
        int minLevel = fang || lightbearer ? 50 : 150;
        return raidLevel < minLevel ? w / 50 : w;
    }

    private static int toaReweight(boolean fang, int level) {
        if (level <= 300) {
            return 70;
        }
        if (level >= 500) {
            return fang ? 30 : 35;
        }
        if (level >= 450) {
            return 40 - (int) Math.floor((level - 450) * (fang ? 0.2 : 0.1));
        }
        if (level >= 400) {
            return fang ? 40 : 50 - (int) Math.floor((level - 400) * 0.2);
        }
        if (level >= 350) {
            return 60 - (int) Math.floor((level - 350) * (fang ? 0.4 : 0.2));
        }
        return 70 - (int) Math.floor((level - 300) * 0.2);
    }

    private static int weightOf(Unique u, RaidType type, int raidLevel) {
        return type == RaidType.TOA ? toaWeight(u, raidLevel) : u.weight();
    }

    static String rollUnique(List<Unique> table, Random random, RaidType type, int raidLevel) {
        int total = 0;
        for (Unique u : table) {
            total += weightOf(u, type, raidLevel);
        }
        int r = random.nextInt(total);
        for (Unique u : table) {
            r -= weightOf(u, type, raidLevel);
            if (r < 0) {
                return u.item();
            }
        }
        return table.get(0).item();
    }

    /** Picks a player weighted by points (everyone equally if nobody has points). */
    static String weightedPlayer(Map<String, Integer> points, Random random) {
        return weightedPlayer(points, List.of(), random);
    }

    /** Picks a player weighted by points, skipping {@code exclude}; null if nobody is left. */
    static String weightedPlayer(Map<String, Integer> points, List<String> exclude, Random random) {
        List<String> players = new ArrayList<>(points.keySet());
        players.removeAll(exclude);
        if (players.isEmpty()) {
            return null;
        }
        Collections.sort(players);
        long total = 0;
        for (String p : players) {
            total += Math.max(0, points.get(p));
        }
        if (total <= 0) {
            return players.get(random.nextInt(players.size()));
        }
        long r = (long) (random.nextDouble() * total);
        for (String p : players) {
            r -= Math.max(0, points.get(p));
            if (r < 0) {
                return p;
            }
        }
        return players.get(players.size() - 1);
    }

    /** CoX resource quantity (OSRS): personal points (capped at 131,071) / divisor; torn prayer scrolls and dark relics come singly. */
    public static int coxCommonQuantity(PointsCommon c, int personalPoints) {
        if (c.divisor() <= 1 && !c.item().equals("pure_essence")) {
            return 1;
        }
        return Math.max(1, Math.max(0, Math.min(personalPoints, RaidScaling.COX_POINTS_CAP)) / c.divisor());
    }

    /**
     * Rolls a completed raid's rewards.
     *
     * @param points    personal points per player (CoX/ToA points; ToB per-room points)
     * @param raidLevel ToA raid level (ignored for CoX/ToB)
     */
    public static Result roll(RaidType type, Map<String, Integer> points, int raidLevel, Random random) {
        return roll(type, RaidMode.NORMAL, points, raidLevel, false, random);
    }

    public static Result roll(RaidType type, RaidMode mode, Map<String, Integer> points, int raidLevel, Random random) {
        return roll(type, mode, points, raidLevel, false, random);
    }

    /**
     * Rolls a completed raid's rewards for a mode: CoX gets one unique roll per 570,000 team points
     * (each player can receive one unique), ToB and ToA one roll (only one unique per raid). Challenge
     * and Hard mode add their extra rewards; {@code withinTarget} says the raid beat the mode's target time.
     */
    public static Result roll(RaidType type, RaidMode mode, Map<String, Integer> points, int raidLevel, boolean withinTarget,
                              Random random) {
        Map<String, List<Loot>> loot = new LinkedHashMap<>();
        List<String> purples = new ArrayList<>();
        List<String> pets = new ArrayList<>();
        if (points.isEmpty()) {
            return new Result(loot, purples, pets, 0);
        }
        int total = 0;
        for (int p : points.values()) {
            total += Math.max(0, p);
        }
        List<Double> rolls = switch (type) {
            case COX -> RaidScaling.coxUniqueRolls(total);
            case TOB -> List.of(RaidScaling.tobPurpleChance(total, points.size(), mode));
            case TOA -> List.of(RaidScaling.toaPurpleChance(total, raidLevel));
        };
        double chance = rolls.get(0);
        for (String p : points.keySet()) {
            loot.put(p, new ArrayList<>());
        }
        List<Unique> table = uniques(type, mode);
        for (double rollChance : rolls) {
            if (random.nextDouble() >= rollChance) {
                continue;
            }
            String who = weightedPlayer(points, purples, random);
            if (who == null) {
                break;
            }
            loot.get(who).add(new Loot(rollUnique(table, random, type, raidLevel), 1));
            purples.add(who);
            if (type == RaidType.COX && random.nextInt(OLMLET_RATE) == 0) {
                loot.get(who).add(new Loot("olmlet", 1));
                pets.add(who);
            }
        }
        if (type == RaidType.TOB && mode != RaidMode.ENTRY
                && random.nextInt(mode == RaidMode.HARD ? LIL_ZIK_HARD_RATE : LIL_ZIK_RATE) == 0) {
            String who = weightedPlayer(points, random);
            loot.get(who).add(new Loot("lil_zik", 1));
            pets.add(who);
        }
        if (type == RaidType.TOA && random.nextDouble() < RaidScaling.toaPetChance(total, raidLevel)) {
            String who = weightedPlayer(points, random);
            loot.get(who).add(new Loot("tumekens_guardian", 1));
            pets.add(who);
        }
        for (ExtraReward extra : extras(type, mode)) {
            if ((!extra.needsTargetTime() || withinTarget) && random.nextInt(extra.rate()) == 0) {
                loot.get(weightedPlayer(points, random)).add(new Loot(extra.item(), 1));
            }
        }
        if (type == RaidType.TOA && random.nextDouble() < RaidScaling.toaEliteClueChance(total)) {
            loot.get(weightedPlayer(points, random)).add(new Loot("clue_scroll_elite", 1));
        }
        double tobRatio = RaidScaling.tobPointsRatio(total, points.size());
        for (Map.Entry<String, Integer> e : points.entrySet()) {
            if (purples.contains(e.getKey())) {
                continue;
            }
            List<Loot> mine = loot.get(e.getKey());
            int pts = Math.max(0, e.getValue());
            switch (type) {
                case COX -> {
                    for (PointsCommon c : pickWeighted(COX_COMMONS, COX_COMMON_ROLLS, random)) {
                        mine.add(new Loot(c.item(), coxCommonQuantity(c, pts)));
                    }
                    if (random.nextInt(12) == 0) { // approx
                        mine.add(new Loot("clue_scroll_elite", 1));
                    }
                }
                case TOB -> {
                    if (pts <= 0) {
                        // OSRS: a player who kept no points finds only a cabbage.
                        mine.add(new Loot("cabbage", 1));
                        continue;
                    }
                    for (RangeCommon c : pickWeightedRange(TOB_COMMONS, TOB_COMMON_ROLLS, random)) {
                        int[] range = RaidScaling.tobCommonRange(c.min(), tobRatio, mode, withinTarget);
                        int n = range[0] + random.nextInt(range[1] - range[0] + 1);
                        // Entry mode chests hold far less. // approx: a fifth
                        mine.add(new Loot(c.item(), Math.max(1, mode == RaidMode.ENTRY ? n / 5 : n)));
                    }
                    if (mode != RaidMode.ENTRY && random.nextInt(25) == 0) { // approx
                        mine.add(new Loot("clue_scroll_elite", 1));
                    }
                }
                case TOA -> {
                    for (PointsCommon c : pick(TOA_COMMONS, TOA_COMMON_ROLLS, random)) {
                        mine.add(new Loot(c.item(), RaidScaling.toaCommonQuantity(pts, c.divisor(), raidLevel)));
                    }
                }
            }
        }
        return new Result(loot, purples, pets, chance);
    }

    private static <T> List<T> pick(List<T> table, int n, Random random) {
        List<T> copy = new ArrayList<>(table);
        Collections.shuffle(copy, random);
        return copy.subList(0, Math.min(n, copy.size()));
    }

    /** {@code n} different CoX commons, each chosen by weight. */
    static List<PointsCommon> pickWeighted(List<PointsCommon> table, int n, Random random) {
        List<PointsCommon> left = new ArrayList<>(table);
        List<PointsCommon> out = new ArrayList<>();
        while (out.size() < n && !left.isEmpty()) {
            int total = 0;
            for (PointsCommon c : left) {
                total += c.weight();
            }
            int r = random.nextInt(total);
            for (PointsCommon c : left) {
                r -= c.weight();
                if (r < 0) {
                    out.add(c);
                    left.remove(c);
                    break;
                }
            }
        }
        return out;
    }

    /** {@code n} different ToB commons, each chosen by weight. */
    static List<RangeCommon> pickWeightedRange(List<RangeCommon> table, int n, Random random) {
        List<RangeCommon> left = new ArrayList<>(table);
        List<RangeCommon> out = new ArrayList<>();
        while (out.size() < n && !left.isEmpty()) {
            int total = 0;
            for (RangeCommon c : left) {
                total += c.weight();
            }
            int r = random.nextInt(total);
            for (RangeCommon c : left) {
                r -= c.weight();
                if (r < 0) {
                    out.add(c);
                    left.remove(c);
                    break;
                }
            }
        }
        return out;
    }
}
