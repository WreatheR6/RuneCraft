package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.data.Drop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/**
 * How a boss kill rolls its drop list, as OSRS rolls a boss's drop table (OSRS Wiki "Drop table"): every 100% line drops,
 * tertiary lines are rolled each on its own, on top of the main drop, and the other lines are the main table, rolled once a
 * kill (or {@link #rolls} times: Zulrah, Vorkath, the Alchemical Hydra...), so a kill always lands on one of its lines. Rolling
 * every line on its own instead, as an ordinary monster's list is, left about a third of the kills of a boss without a 100%
 * drop (Kraken, Scorpia, Sarachnis, the Moons...) with nothing at all and gave others three or four drops.
 *
 * <p>The main lines keep their own 1/N rates (per roll): they are laid end to end, each as long as its rate times the rolls,
 * in a random order, and a kill takes the lines under one random point and every whole step after it (systematic sampling).
 * A kill therefore drops as many main lines as their rates add up to (one a roll for a table that adds up to one roll), never
 * none from a full table. The wiki rarities are rounded to whole 1/N and a few items aren't in RuneCraft, so a full table can
 * add up to a little less than a roll: from {@link #FULL_TABLE} of a roll on, the missing share is spread over the common lines
 * (rarer than {@link #COMMON_BELOW} lines, the uniques, keep their exact rates). A sparse list (Nex's generals' 1/1000 robes)
 * stays sparse: most of its kills drop nothing, as in OSRS.
 *
 * <p>A wiki pre-roll (Thermonuclear smoke devil's uniques, the Wilderness bosses' shards...) is rolled before the main table and
 * the main table only when it misses; for a one-roll boss that is what the main sampling does with its lines in the table
 * (landing on one leaves no other main line). A boss that rolls its main table more than once keeps its pre-roll, or a unique
 * table of its own, apart ({@link #once}): rolled once a kill, its rate per kill, a hit taking the place of as many main rolls
 * as the wiki says.
 */
public final class BossLoot {
    /** Main lines adding up to at least this share of a roll are a full main table (the rest is rounding). */
    public static final double FULL_TABLE = 0.9;
    /** Lines more common than 1/50 take a full table's missing share (the boss drop patcher's rare-line boundary too). */
    public static final int COMMON_BELOW = 50;
    /** Item id prefixes of tertiary drops on every monster (OSRS Wiki "Drop table#Tertiary drops"): clues, jars, ensouled heads. */
    private static final List<String> TERTIARY_PREFIXES = List.of("clue_scroll", "jar_of", "ensouled_");
    /** Tertiary on every monster that drops them: the long and curved bones (OSRS Wiki "Drop table#Tertiary drops"). */
    private static final Set<String> TERTIARY_ITEMS = Set.of("long_bone", "curved_bone");
    /** Each boss's other tertiary lines: the items of the Tertiary table of its OSRS Wiki page. */
    private static final Map<String, Set<String>> TERTIARIES = Map.ofEntries(
            Map.entry("vorkath", Set.of("scaly_blue_dragonhide", "vorkaths_head", "dragonbone_necklace", "draconic_visage",
                    "skeletal_visage")),
            Map.entry("king_black_dragon", Set.of("kbd_heads", "draconic_visage")),
            Map.entry("kalphite_queen", Set.of("kq_head", "dragon_chainbody", "dragon_2h_sword", "dragon_pickaxe")),
            Map.entry("alchemical_hydra", Set.of("alchemical_hydra_heads")),
            Map.entry("amoxliatl", Set.of("pendant_of_ates", "glacial_temotli")),
            Map.entry("vetion", Set.of("skeleton_champion_scroll")),
            Map.entry("calvarion", Set.of("skeleton_champion_scroll")),
            Map.entry("chaos_elemental", Set.of("weapon_poison_plus_plus")),
            Map.entry("phosanis_nightmare", Set.of("parasitic_egg", "slepey_tablet")),
            Map.entry("sarachnis", Set.of("giant_egg_sac_full")),
            Map.entry("scurrius", Set.of("scurrius_spine")),
            Map.entry("skotizo", Set.of("dark_claw", "dark_totem_base", "dark_totem_middle", "dark_totem_top", "dark_totem")));
    /** Bosses whose kill rolls the main table more than once (their main lines give the rate per roll, as the wiki lists them). */
    private static final Map<String, Integer> ROLLS = Map.of(
            // OSRS Wiki "Gemstone Crab": the top damagers mine three uncut gems from its shell (rolls=3 in its drop table).
            "gemstone_crab", 3,
            // OSRS Wiki drop tables: "two rolls on [its] drop table, excluding tertiary items, which have only one roll".
            "zulrah", 2, "vorkath", 2, "alchemical_hydra", 2, "shellbane_gryphon", 2, "phantom_muspah", 2);

    /**
     * Lines of a multi-roll boss that make a table of their own, rolled once a kill (their rarity is per kill): a hit takes the
     * place of {@code takes} of the kill's main rolls; one of the {@code alone} items comes with no main roll at all.
     */
    private record Once(Set<String> items, int takes, Set<String> alone) {
        Once(Set<String> items, int takes) {
            this(items, takes, Set.of());
        }
    }

    private static final Map<String, Once> ONCE = Map.of(
            // Wiki pre-roll: "The first drop has a chance to be a unique, while the second is always standard."
            "alchemical_hydra", new Once(Set.of("hydras_eye", "hydras_fang", "hydras_heart", "hydra_tail", "hydra_leather",
                    "hydra_claw", "dragon_thrownaxe"), 1),
            // Wiki pre-roll, rolled once beside the two rolls; a hit taking a roll as the Hydra's does // approx
            "shellbane_gryphon", new Once(Set.of("belles_folly_tarnished"), 1),
            // Wiki: "In addition to the unique drops, the player will also have two rolls on the secondary drop table. If the
            // player gets a Venator shard, they will not get any other drop."
            "phantom_muspah", new Once(Set.of("ancient_essence", "ancient_icon", "venator_shard"), 0, Set.of("venator_shard")));

    private BossLoot() {
    }

    /** The items {@code bossId}'s own tertiary and once-a-kill lists name (each must be a line of its table). */
    static Set<String> namedLines(String bossId) {
        Set<String> out = new java.util.TreeSet<>(TERTIARIES.getOrDefault(bossId, Set.of()));
        Once o = ONCE.get(bossId);
        if (o != null) {
            out.addAll(o.items());
        }
        return out;
    }

    /** Bosses with their own tertiary or once-a-kill lists. */
    static Set<String> bossesWithNamedLines() {
        Set<String> out = new java.util.TreeSet<>(TERTIARIES.keySet());
        out.addAll(ONCE.keySet());
        return out;
    }

    /** Main-table rolls of a kill of {@code bossId}. */
    public static int rolls(String bossId) {
        return ROLLS.getOrDefault(bossId, 1);
    }

    /** Whether a line of {@code bossId}'s table (null: any boss) is a tertiary drop, rolled on its own. */
    public static boolean tertiary(String bossId, Drop d) {
        for (String p : TERTIARY_PREFIXES) {
            if (d.item().startsWith(p)) {
                return true;
            }
        }
        return TERTIARY_ITEMS.contains(d.item()) || bossId != null && TERTIARIES.getOrDefault(bossId, Set.of()).contains(d.item());
    }

    /** Whether a line of {@code bossId}'s table is rolled once a kill in a table of its own (a multi-roll boss's pre-roll). */
    public static boolean once(String bossId, Drop d) {
        Once o = bossId == null ? null : ONCE.get(bossId);
        return o != null && o.items().contains(d.item());
    }

    /**
     * The lines a kill of {@code bossId} drops (quantities and shared {@code #tables} are resolved by the caller). {@code nextInt}
     * returns a uniform int in [0, bound).
     */
    public static List<Drop> pick(String bossId, List<Drop> table, IntUnaryOperator nextInt) {
        return pick(bossId, table, rolls(bossId), nextInt);
    }

    /** The lines a kill drops with {@code rolls} main rolls and no boss's own tertiaries or pre-roll (any boss's rules only). */
    public static List<Drop> pick(List<Drop> table, int rolls, IntUnaryOperator nextInt) {
        return pick(null, table, rolls, nextInt);
    }

    private static List<Drop> pick(String bossId, List<Drop> table, int rolls, IntUnaryOperator nextInt) {
        List<Drop> out = new ArrayList<>();
        List<Drop> main = new ArrayList<>();
        List<Drop> own = new ArrayList<>();
        for (Drop d : table) {
            if (d.rarity() <= 1) {
                out.add(d);
            } else if (tertiary(bossId, d)) {
                if (nextInt.applyAsInt(d.rarity()) == 0) {
                    out.add(d);
                }
            } else if (once(bossId, d)) {
                own.add(d);
            } else {
                main.add(d);
            }
        }
        int r = Math.max(1, rolls);
        if (!own.isEmpty()) {
            int before = out.size();
            sample(own, 1, nextInt, out);
            if (out.size() > before) {
                Once o = ONCE.get(bossId);
                r -= o.takes();
                for (int i = before; i < out.size(); i++) {
                    if (o.alone().contains(out.get(i).item())) {
                        r = 0;
                    }
                }
            }
        }
        if (r > 0) {
            sample(main, r, nextInt, out);
        }
        return out;
    }

    /** {@code r} rolls on a table of lines (rarities per roll): the lines one systematic sample lands on go to {@code out}. */
    private static void sample(List<Drop> lines, int r, IntUnaryOperator nextInt, List<Drop> out) {
        if (lines.isEmpty()) {
            return;
        }
        List<Drop> order = new ArrayList<>(lines);
        double total = 0;
        double common = 0;
        for (Drop d : order) {
            total += (double) r / d.rarity();
            if (d.rarity() < COMMON_BELOW) {
                common += (double) r / d.rarity();
            }
        }
        // A full table short of a whole number of rolls only by rounding: the common lines take the missing share.
        double stretch = total < r && total >= FULL_TABLE * r && common > 0 ? 1 + (r - total) / common : 1;
        // A random order each kill, so which lines can drop together doesn't depend on the order of the list.
        for (int i = order.size() - 1; i > 0; i--) {
            Collections.swap(order, i, nextInt.applyAsInt(i + 1));
        }
        double u = nextInt.applyAsInt(1 << 24) / (double) (1 << 24);
        double at = 0;
        for (Drop d : order) {
            double w = Math.min(1, (double) r / d.rarity() * (d.rarity() < COMMON_BELOW ? stretch : 1));
            if (points(at + w, u) > points(at, u)) {
                out.add(d);
            }
            at += w;
        }
    }

    /** How many of the points u, u + 1, u + 2... lie below {@code x}. */
    private static long points(double x, double u) {
        return x <= u ? 0 : (long) Math.ceil(x - u);
    }
}
