package com.runecraft.osrs.content.raids;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Raid reward tables and the end-of-raid loot roll (pure Java, keyed by player id strings).
 * Unique weights and common tables follow the OSRS wiki; minor approximations are noted.
 */
public final class RaidRewards {
    /** One weighted unique. */
    public record Unique(String item, int weight) {
    }

    /** CoX common: quantity = personal points / divisor (OSRS caps follow from the 131,071 points cap). */
    public record PointsCommon(String item, int divisor) {
    }

    /** ToB common: a fixed quantity range. */
    public record RangeCommon(String item, int min, int max) {
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

    /** CoX uniques (OSRS weights out of 69: twisted bow, kodai insignia and elder maul are each 2/69). */
    public static final List<Unique> COX_UNIQUES = List.of(
            new Unique("dexterous_prayer_scroll", 20),
            new Unique("arcane_prayer_scroll", 20),
            new Unique("twisted_buckler", 4),
            new Unique("dragon_hunter_crossbow", 4),
            new Unique("dinhs_bulwark", 3),
            new Unique("ancestral_hat", 3),
            new Unique("ancestral_robe_top", 3),
            new Unique("ancestral_robe_bottom", 3),
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
    /** ToA uniques at raid level 150+ (OSRS, total 240). */
    public static final List<Unique> TOA_UNIQUES = List.of(
            new Unique("osmumtens_fang", 70),
            new Unique("lightbearer", 70),
            new Unique("elidinis_ward", 30),
            new Unique("masori_mask", 20),
            new Unique("masori_body", 20),
            new Unique("masori_chaps", 20),
            new Unique("tumekens_shadow", 10));

    public static final List<PointsCommon> COX_COMMONS = List.of(
            new PointsCommon("death_rune", 36), new PointsCommon("blood_rune", 32), new PointsCommon("soul_rune", 20),
            new PointsCommon("rune_arrow", 14), new PointsCommon("grimy_toadflax", 189), new PointsCommon("grimy_ranarr_weed", 489),
            new PointsCommon("grimy_irit_leaf", 98), new PointsCommon("grimy_avantoe", 151), new PointsCommon("grimy_kwuarm", 166),
            new PointsCommon("grimy_snapdragon", 518), new PointsCommon("grimy_cadantine", 155),
            new PointsCommon("grimy_lantadyme", 128), new PointsCommon("grimy_dwarf_weed", 111), new PointsCommon("grimy_torstol", 520),
            new PointsCommon("silver_ore", 20), new PointsCommon("coal", 20), new PointsCommon("gold_ore", 44),
            new PointsCommon("mithril_ore", 32), new PointsCommon("adamantite_ore", 168), new PointsCommon("runite_ore", 2010),
            new PointsCommon("uncut_sapphire", 189), new PointsCommon("uncut_emerald", 251), new PointsCommon("uncut_ruby", 307),
            new PointsCommon("uncut_diamond", 1003), new PointsCommon("pure_essence", 2), new PointsCommon("teak_plank", 100),
            new PointsCommon("mahogany_plank", 240));

    public static final List<RangeCommon> TOB_COMMONS = List.of(
            new RangeCommon("vial_of_blood", 50, 60), new RangeCommon("death_rune", 500, 600),
            new RangeCommon("blood_rune", 500, 600), new RangeCommon("coal", 500, 600), new RangeCommon("gold_ore", 300, 360),
            new RangeCommon("adamantite_ore", 130, 156), new RangeCommon("runite_ore", 60, 72),
            new RangeCommon("wine_of_zamorak", 50, 60), new RangeCommon("potato_cactus", 50, 60),
            new RangeCommon("grimy_cadantine", 50, 60), new RangeCommon("grimy_avantoe", 40, 48),
            new RangeCommon("grimy_toadflax", 37, 44), new RangeCommon("grimy_kwuarm", 36, 43),
            new RangeCommon("grimy_irit_leaf", 34, 40), new RangeCommon("grimy_ranarr_weed", 30, 36),
            new RangeCommon("grimy_snapdragon", 27, 32), new RangeCommon("grimy_lantadyme", 26, 31),
            new RangeCommon("grimy_dwarf_weed", 24, 28), new RangeCommon("grimy_torstol", 20, 24),
            new RangeCommon("rune_battleaxe", 4, 4), new RangeCommon("rune_platebody", 4, 4));

    /** ToA commons: quantity = points * (1 + level / 300) / divisor (approximation of OSRS). */
    public static final List<PointsCommon> TOA_COMMONS = List.of(
            new PointsCommon("death_rune", 20), new PointsCommon("soul_rune", 40), new PointsCommon("gold_ore", 90),
            new PointsCommon("dragonstone", 1500), new PointsCommon("grimy_ranarr_weed", 300), new PointsCommon("grimy_toadflax", 180),
            new PointsCommon("grimy_irit_leaf", 100), new PointsCommon("grimy_kwuarm", 160), new PointsCommon("grimy_snapdragon", 400),
            new PointsCommon("grimy_lantadyme", 140), new PointsCommon("grimy_dwarf_weed", 120), new PointsCommon("grimy_torstol", 450),
            new PointsCommon("potato_cactus", 80), new PointsCommon("magic_logs", 70), new PointsCommon("runite_ore", 1600),
            new PointsCommon("coal", 25));

    /** Olmlet chance per CoX unique (OSRS: 1/53). */
    public static final int OLMLET_RATE = 53;
    /** Lil' Zik chance per ToB completion (OSRS normal mode: 1/650). */
    public static final int LIL_ZIK_RATE = 650;
    /** Lil' Zik chance per ToB hard mode completion (OSRS: 1/500). */
    public static final int LIL_ZIK_HARD_RATE = 500;
    /** ToA weights are kept as multiples of 1/50 so the rarer low-level weights stay exact. */
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

    /** Uniques for a raid. */
    public static List<Unique> uniques(RaidType type) {
        return switch (type) {
            case COX -> COX_UNIQUES;
            case TOB -> TOB_UNIQUES;
            case TOA -> TOA_UNIQUES;
        };
    }

    public static String pet(RaidType type) {
        return switch (type) {
            case COX -> "olmlet";
            case TOB -> "lil_zik";
            case TOA -> "tumekens_guardian";
        };
    }

    /**
     * ToA unique weight at a raid level, in fiftieths: below raid level 150 every unique but the fang
     * and Lightbearer is rarer, 1/50 as likely below level 50 and half as likely from 50 to 149. // approx
     */
    public static int toaWeight(Unique u, int raidLevel) {
        int w = u.weight() * TOA_WEIGHT_SCALE;
        if (raidLevel >= 150 || u.item().equals("osmumtens_fang") || u.item().equals("lightbearer")) {
            return w;
        }
        return raidLevel < 50 ? w / 50 : w / 2;
    }

    static String rollUnique(List<Unique> table, Random random, RaidType type, int raidLevel) {
        int total = 0;
        for (Unique u : table) {
            total += type == RaidType.TOA ? toaWeight(u, raidLevel) : u.weight();
        }
        int r = random.nextInt(total);
        for (Unique u : table) {
            r -= type == RaidType.TOA ? toaWeight(u, raidLevel) : u.weight();
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

    /**
     * Rolls a completed raid's rewards.
     *
     * @param points    personal points per player (CoX/ToA points; ToB per-room points)
     * @param raidLevel ToA raid level (ignored for CoX/ToB)
     */
    public static Result roll(RaidType type, Map<String, Integer> points, int raidLevel, Random random) {
        return roll(type, RaidMode.NORMAL, points, raidLevel, random);
    }

    /**
     * Rolls a completed raid's rewards for a mode: CoX gets one unique roll per 570,000 team points
     * (each player can receive one unique), ToB's chance and pet rate depend on the mode.
     */
    public static Result roll(RaidType type, RaidMode mode, Map<String, Integer> points, int raidLevel, Random random) {
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
        for (double rollChance : rolls) {
            if (random.nextDouble() >= rollChance) {
                continue;
            }
            String who = weightedPlayer(points, purples, random);
            if (who == null) {
                break;
            }
            String item = rollUnique(uniques(type), random, type, raidLevel);
            loot.get(who).add(new Loot(item, 1));
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
        for (Map.Entry<String, Integer> e : points.entrySet()) {
            if (purples.contains(e.getKey())) {
                continue;
            }
            List<Loot> mine = loot.get(e.getKey());
            int pts = Math.max(0, e.getValue());
            switch (type) {
                case COX -> {
                    for (PointsCommon c : pick(COX_COMMONS, 2, random)) {
                        mine.add(new Loot(c.item(), Math.max(1, Math.min(pts, RaidScaling.COX_POINTS_CAP) / c.divisor())));
                    }
                    if (random.nextInt(12) == 0) {
                        mine.add(new Loot("clue_scroll_elite", 1));
                    }
                }
                case TOB -> {
                    if (pts <= 0) {
                        // Players who died in every room receive nothing (OSRS: "Your share was forfeit").
                        continue;
                    }
                    for (RangeCommon c : pick(TOB_COMMONS, 3, random)) {
                        int n = c.min() + random.nextInt(c.max() - c.min() + 1);
                        // Entry mode chests hold far less. // approx: a fifth
                        mine.add(new Loot(c.item(), mode == RaidMode.ENTRY ? Math.max(1, n / 5) : n));
                    }
                    if (mode != RaidMode.ENTRY && random.nextInt(25) == 0) {
                        mine.add(new Loot("clue_scroll_elite", 1));
                    }
                }
                case TOA -> {
                    double scaled = Math.min(pts, RaidScaling.TOA_POINTS_CAP) * (1.0 + raidLevel / 300.0);
                    for (PointsCommon c : pick(TOA_COMMONS, 3, random)) {
                        mine.add(new Loot(c.item(), Math.max(1, (int) (scaled / c.divisor()))));
                    }
                    if (random.nextInt(Math.max(5, 25 - raidLevel / 25)) == 0) {
                        mine.add(new Loot("clue_scroll_elite", 1));
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
}
