package com.runecraft.osrs.content.skilling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Sailing training methods as on the OSRS Wiki (July 2026): sea charting (358 tasks with sea and
 * ocean completion bonuses), courier tasks (439 wiki tasks with their XP, cargo and ports), core
 * boat parts (hull, mast and sails, helm, keel per boat type, built with Construction), boat
 * facilities, Barracuda Trials, sail trimming and deep sea trawling nets. Tables come from
 * {@link SailingTables}; snapshots and diff tests live under src/test/resources/osrs/sailing.
 */
public final class SailingTraining {
    private SailingTraining() {
    }

    private static List<String[]> rows(String table) {
        List<String[]> out = new ArrayList<>();
        for (String line : table.split("\n")) {
            if (!line.isBlank()) {
                out.add(line.split("\t", -1));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ sea charting

    /** Charting task types: unlock level, XP per task (wiki tool table) and the tool needed. */
    public enum ChartType {
        GENERIC(1, 35, null), SPYGLASS(1, 50, "spyglass"), CRATE(12, 75, "crowbar"), CURRENT(22, 125, "current_duck"),
        DIVING(38, 175, "diving_apparatus"), WEATHER(57, 250, "portable_weather_station");

        public final int level;
        public final double xp;
        public final String tool;

        ChartType(int level, double xp, String tool) {
            this.level = level;
            this.xp = xp;
            this.tool = tool;
        }
    }

    public record ChartTask(int id, int level, ChartType type, String hazard, String sea, String ocean, int x, int y, String task) {
        public String placeId() {
            return "chart_task_" + id;
        }

        /** Tasks in caves (OSRS y above 6400) have no surface spot. */
        public boolean underground() {
            return y >= 6400;
        }

        /** The completion group: bonus charts count towards "Miscellaneous". */
        public String group() {
            return ocean.equals("Bonus charts") ? "Miscellaneous" : ocean;
        }
    }

    public record Sea(String ocean, String name, int level, int tasks, double xp) {
    }

    public static final List<ChartTask> CHARTS;
    public static final List<Sea> SEAS;
    /** Ocean completion bonuses (claimed from Chartin' Charles in OSRS; awarded on completion here). */
    public static final Map<String, Double> OCEAN_BONUS;
    /** Bonus for charting all 358 tasks. */
    public static final double ALL_CHARTED_BONUS = 100_000;

    static {
        List<ChartTask> charts = new ArrayList<>();
        for (String[] r : rows(SailingTables.CHARTS)) {
            charts.add(new ChartTask(Integer.parseInt(r[0]), Integer.parseInt(r[1]), ChartType.valueOf(r[2].toUpperCase(Locale.ROOT)),
                    r[3].equals("-") ? "" : r[3], r[4], r[5], Integer.parseInt(r[6]), Integer.parseInt(r[7]), r[8]));
        }
        CHARTS = List.copyOf(charts);
        List<Sea> seas = new ArrayList<>();
        for (String[] r : rows(SailingTables.SEAS)) {
            seas.add(new Sea(r[0], r[1], Integer.parseInt(r[2]), Integer.parseInt(r[3]), Double.parseDouble(r[4])));
        }
        SEAS = List.copyOf(seas);
        Map<String, Double> ocean = new LinkedHashMap<>();
        ocean.put("Sunset Ocean", 2_000.0);
        ocean.put("Ardent Ocean", 5_000.0);
        ocean.put("Unquiet Ocean", 6_000.0);
        ocean.put("Western Ocean", 20_000.0);
        ocean.put("Shrouded Ocean", 30_000.0);
        ocean.put("Northern Ocean", 35_000.0);
        ocean.put("Miscellaneous", 15_000.0);
        OCEAN_BONUS = Collections.unmodifiableMap(ocean);
    }

    public static ChartTask chart(int id) {
        for (ChartTask c : CHARTS) {
            if (c.id() == id) {
                return c;
            }
        }
        return null;
    }

    public static ChartTask chartByPlace(String placeId) {
        return placeId.startsWith("chart_task_") ? chart(Integer.parseInt(placeId.substring(11))) : null;
    }

    /** The nearest surface charting task within {@code maxTiles} (Chebyshev) of an OSRS tile, or null. */
    public static ChartTask nearestChart(int x, int y, int maxTiles) {
        ChartTask best = null;
        int bestD = Integer.MAX_VALUE;
        for (ChartTask c : CHARTS) {
            int d = Math.max(Math.abs(c.x() - x), Math.abs(c.y() - y));
            if (!c.underground() && d <= maxTiles && d < bestD) {
                bestD = d;
                best = c;
            }
        }
        return best;
    }

    /** Why a player can't do a charting task, or null. */
    public static String canChart(ChartTask t, int sailingLevel, Collection<String> items) {
        int level = Math.max(t.level(), t.type().level);
        if (sailingLevel < level) {
            return "You need a Sailing level of " + level + " to chart here.";
        }
        if (t.type().tool != null && !items.contains(t.type().tool)) {
            return "You need " + article(title(t.type().tool)) + " to chart here.";
        }
        return null;
    }

    /** One XP reward for charting: what it is for and how much. */
    public record ChartReward(String reason, double xp) {
    }

    /**
     * XP for completing a charting task, given every task id done including this one: the task's XP,
     * plus its sea's completion XP, its ocean's bonus and the all-oceans bonus when those are now complete.
     */
    public static List<ChartReward> chartRewards(ChartTask t, Set<Integer> done) {
        List<ChartReward> out = new ArrayList<>();
        out.add(new ChartReward(t.type().name().toLowerCase(Locale.ROOT) + " charting", t.type().xp));
        if (!t.ocean().equals("Bonus charts")) {
            for (Sea s : SEAS) {
                if (s.name().equals(t.sea()) && s.ocean().equals(t.ocean()) && allDone(done, c -> c.ocean().equals(s.ocean())
                        && c.sea().equals(s.name()))) {
                    out.add(new ChartReward("charting the " + s.name(), s.xp()));
                }
            }
        }
        if (allDone(done, c -> c.group().equals(t.group())) && OCEAN_BONUS.containsKey(t.group())) {
            out.add(new ChartReward("charting the " + t.group(), OCEAN_BONUS.get(t.group())));
        }
        if (allDone(done, c -> true)) {
            out.add(new ChartReward("charting every ocean", ALL_CHARTED_BONUS));
        }
        return out;
    }

    private static boolean allDone(Set<Integer> done, java.util.function.Predicate<ChartTask> filter) {
        for (ChartTask c : CHARTS) {
            if (filter.test(c) && !done.contains(c.id())) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ courier tasks

    public record CourierTask(int id, int level, double xp, String board, String cargo, String destination, String cargoName, int qty) {
    }

    public static final List<CourierTask> COURIER;

    static {
        List<CourierTask> list = new ArrayList<>();
        for (String[] r : rows(SailingTables.COURIER)) {
            list.add(new CourierTask(Integer.parseInt(r[0]), Integer.parseInt(r[1]), r[2].isEmpty() ? Double.NaN : Double.parseDouble(r[2]),
                    r[3], r[4], r[5], r[6],
                    Integer.parseInt(r[7])));
        }
        COURIER = List.copyOf(list);
    }

    public static CourierTask courier(int id) {
        for (CourierTask c : COURIER) {
            if (c.id() == id) {
                return c;
            }
        }
        return null;
    }

    /** Port tasks a player can hold at once (wiki: 1, 2 at 7, 3 at 28, 4 at 56, 5 at 84). */
    public static int taskSlots(int sailingLevel) {
        return sailingLevel >= 84 ? 5 : sailingLevel >= 56 ? 4 : sailingLevel >= 28 ? 3 : sailingLevel >= 7 ? 2 : 1;
    }

    /** The courier tasks offered on a port's notice board to a player of this level. */
    public static List<CourierTask> boardTasks(String portId, int sailingLevel) {
        List<CourierTask> out = new ArrayList<>();
        for (CourierTask c : COURIER) {
            if (c.board().equals(portId) && c.level() <= sailingLevel) {
                out.add(c);
            }
        }
        return out;
    }

    /**
     * Sailing XP for a courier task: the wiki's value, or for the few tasks the wiki has none for, an estimate from the
     * distance sailed (board to cargo to destination) scaled by the task level (approx; fitted to the known tasks).
     */
    public static double courierXp(CourierTask t) {
        if (!Double.isNaN(t.xp())) {
            return t.xp();
        }
        SailingData.Port b = SailingData.port(t.board());
        SailingData.Port c = SailingData.port(t.cargo());
        SailingData.Port d = SailingData.port(t.destination());
        if (b == null || c == null || d == null) {
            return 0;
        }
        int tiles = Math.max(Math.abs(b.x() - c.x()), Math.abs(b.y() - c.y())) + Math.max(Math.abs(c.x() - d.x()), Math.abs(c.y() - d.y()));
        return Math.round(tiles * t.level() * 0.065);
    }

    /** Courier deliveries pay as many coins as they give XP (wiki). */
    public static int courierCoins(CourierTask t) {
        return (int) Math.round(courierXp(t));
    }

    // ------------------------------------------------------------------ boat parts

    public enum PartType { HULL, SAILS, HELM, KEEL }

    /** The three boats, by the boat type id used for parts ("raft", "skiff", "sloop"). */
    public static final List<String> BOATS = List.of("raft", "skiff", "sloop");

    /**
     * A core boat part for one boat type; tier 0 is what a new boat comes with. {@code stat} is the hull's speed,
     * the sails' trimming XP, the helm's rapids or the keel's armour (wiki columns).
     */
    public record Part(PartType type, String boat, int tier, int sailing, int construction, String name, Map<String, Integer> wikiMaterials,
                       double conXp, String stat) {
        public double trimXp() {
            return type == PartType.SAILS ? Double.parseDouble(stat) : 0;
        }

        /** Materials as RuneCraft items (see {@link #modMaterial}). */
        public Map<String, Integer> materials() {
            Map<String, Integer> m = new LinkedHashMap<>();
            wikiMaterials.forEach((k, v) -> {
                Object[] mm = modMaterial(k, v);
                m.merge((String) mm[0], (Integer) mm[1], Integer::sum);
            });
            return m;
        }
    }

    public static final List<Part> PARTS;

    static {
        List<Part> list = new ArrayList<>();
        Map<String, Integer> tiers = new LinkedHashMap<>();
        for (String[] r : rows(SailingTables.PARTS)) {
            PartType type = PartType.valueOf(r[0].toUpperCase(Locale.ROOT));
            Map<String, Integer> mats = new LinkedHashMap<>();
            for (String kv : r[5].split(";")) {
                String[] p = kv.split(":");
                mats.put(p[0], Integer.parseInt(p[1]));
            }
            int tier = tiers.merge(r[0] + "/" + r[1], 1, Integer::sum) - 1;
            list.add(new Part(type, r[1], tier, Integer.parseInt(r[2]), Integer.parseInt(r[3]), r[4], Map.copyOf(mats),
                    Double.parseDouble(r[6]), r[7]));
        }
        PARTS = List.copyOf(list);
    }

    public static List<Part> parts(String boat, PartType type) {
        List<Part> out = new ArrayList<>();
        for (Part p : PARTS) {
            if (p.boat().equals(boat) && p.type() == type) {
                out.add(p);
            }
        }
        return out;
    }

    public static Part part(String boat, PartType type, int tier) {
        List<Part> list = parts(boat, type);
        return tier >= 0 && tier < list.size() ? list.get(tier) : null;
    }

    private static final String[] WOODS = {"wooden", "oak", "teak", "mahogany", "camphor", "ironwood", "rosewood"};
    private static final String[] PLANKS = {"plank", "oak_plank", "teak_plank", "mahogany_plank", "camphor_plank", "ironwood_plank",
            "rosewood_plank"};
    private static final String[] METALS = {"bronze", "iron", "steel", "mithril", "adamant", "rune", "dragon"};
    private static final String[] BARS = {"bronze_bar", "iron_bar", "steel_bar", "mithril_bar", "adamantite_bar", "runite_bar",
            "dragon_metal_sheet"};

    /**
     * A wiki material as {id, qty} in RuneCraft items. Hull parts are planks (wiki: 5 planks each, 25 for large
     * parts); keel parts are bars (approx: 1 bar each, 2 for large); swamp tar is swamp paste (approx; swamp tar
     * belongs to another module).
     */
    public static Object[] modMaterial(String id, int qty) {
        for (int i = 0; i < WOODS.length; i++) {
            if (id.equals(WOODS[i] + "_hull_parts")) {
                return new Object[]{PLANKS[i], qty * 5};
            }
            if (id.equals("large_" + WOODS[i] + "_hull_parts")) {
                return new Object[]{PLANKS[i], qty * 25};
            }
        }
        for (int i = 0; i < METALS.length; i++) {
            if (id.equals(METALS[i] + "_keel_parts")) {
                return new Object[]{BARS[i], qty}; // approx
            }
            if (id.equals("large_" + METALS[i] + "_keel_parts")) {
                return new Object[]{BARS[i], qty * 2}; // approx
            }
        }
        return switch (id) {
            case "swamp_tar" -> new Object[]{"swamp_paste", qty}; // approx
            case "adamantite_nails" -> new Object[]{"adamant_nails", qty};
            default -> new Object[]{id, qty};
        };
    }

    /** Why a player can't build a part, or null (levels only; materials are checked by the caller). */
    public static String canBuild(Part p, int sailing, int construction, int currentTier) {
        if (p.tier() <= currentTier) {
            return "Your " + p.boat() + " already has " + article(p.name().toLowerCase(Locale.ROOT)) + " or better.";
        }
        if (p.tier() != currentTier + 1) {
            return "You need to build the previous tier first.";
        }
        if (sailing < p.sailing()) {
            return "You need a Sailing level of " + p.sailing() + " to build " + article(p.name().toLowerCase(Locale.ROOT)) + ".";
        }
        if (construction < p.construction()) {
            return "You need a Construction level of " + p.construction() + " to build " + article(p.name().toLowerCase(Locale.ROOT)) + ".";
        }
        return null;
    }

    // ------------------------------------------------------------------ facilities

    /** A boat facility: Sailing and Construction levels, Construction XP and materials (wiki; approx where noted). */
    public record Facility(String id, String name, int sailing, int construction, double conXp, Map<String, Integer> materials,
                           String use) {
    }

    private static Map<String, Integer> mats(Object... kv) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], (Integer) kv[i + 1]);
        }
        return Collections.unmodifiableMap(m);
    }

    public static final List<Facility> FACILITIES = List.of(
            new Facility("inoculation_station", "Inoculation station", 40, 37, 765, mats("teak_plank", 8, "steel_nails", 32,
                    "relicyms_balm", 6), "Protects against fetid waters; needed for the Jubbly Jive."),
            new Facility("salvaging_station", "Salvaging station", 42, 34, 366, mats("teak_plank", 4, "steel_nails", 16),
                    "Sort salvage at sea (no extra effect in RuneCraft yet)."),
            new Facility("rope_trawling_net", "Rope trawling net", 56, 45, 458, mats("rope", 7, "teak_plank", 4, "steel_bar", 4,
                    "lead_bar", 2), "Trawls shallow shoals."),
            // Yarns are bolts of the same fabric and ray barbs are left out (approx).
            new Facility("linen_trawling_net", "Linen trawling net", 65, 61, 697, mats("bolt_of_linen", 2, "rope", 1, "mahogany_plank", 4,
                    "mithril_bar", 4, "lead_bar", 2), "Trawls shallow and moderate shoals."),
            new Facility("hemp_trawling_net", "Hemp trawling net", 76, 65, 1003, mats("bolt_of_canvas", 2, "rope", 1, "camphor_plank", 4,
                    "adamantite_bar", 4, "cupronickel_bar", 2), "Trawls shoals of every depth."),
            new Facility("cotton_trawling_net", "Cotton trawling net", 84, 73, 1289, mats("bolt_of_cotton", 2, "rope", 1,
                    "ironwood_plank", 4, "runite_bar", 4, "cupronickel_bar", 2), "Trawls shoals of every depth."),
            new Facility("crystal_extractor", "Crystal extractor", 73, 67, 3066, mats("ironwood_plank", 6, "cupronickel_bar", 5,
                    "magic_stone", 2, "heart_of_ithell", 1), "Harvest a wind mote every 109 ticks for 250 Sailing XP."));

    public static Facility facility(String id) {
        for (Facility f : FACILITIES) {
            if (f.id().equals(id)) {
                return f;
            }
        }
        return null;
    }

    /** Crystal extractor: Sailing XP per harvest and ticks between harvests (wiki). */
    public static final double EXTRACTOR_XP = 250;
    public static final int EXTRACTOR_TICKS = 109;

    // ------------------------------------------------------------------ trawling

    /** Trawling nets, best last: facility id, deepest depth (0 shallow, 1 moderate, 2 deep), Sailing XP per roll. */
    public record Net(String facility, int depth, double xpPerRoll, int maxFish) {
    }

    public static final List<Net> NETS = List.of(new Net("rope_trawling_net", 0, 7, 2), new Net("linen_trawling_net", 1, 9, 3),
            new Net("hemp_trawling_net", 2, 11, 4), new Net("cotton_trawling_net", 2, 15, 5));

    /** A trawling roll every 3 ticks while the net is operated (wiki). */
    public static final int TRAWL_ROLL_TICKS = 3;

    /** Shoal depth of each trawled fish (wiki shoals table: the deepest depth the shoal reaches). */
    public static int shoalDepth(String fish) {
        return switch (fish) {
            case "yellowfin", "halibut" -> 1;
            case "bluefin", "marlin" -> 2;
            default -> 0;
        };
    }

    // ------------------------------------------------------------------ trimming

    /** Sails can be trimmed every 30 seconds while sailing (50 ticks). */
    public static final int TRIM_TICKS = 50;

    public static double trimXp(String boat, int sailsTier) {
        Part p = part(boat, PartType.SAILS, Math.max(0, sailsTier));
        return p == null ? 0 : p.trimXp();
    }

    // ------------------------------------------------------------------ Barracuda Trials

    /** One rank of a trial: base XP, lost supplies (crates) and XP each, objectives and XP each, target seconds. */
    public record Rank(String name, double base, int crates, double crateXp, int objectives, double objectiveXp, int targetSeconds) {
        public double xp() {
            return base + crates * crateXp + objectives * objectiveXp;
        }
    }

    /**
     * A Barracuda Trial: level, OSRS centre, the lost supply points of one lap, the two objective points
     * (e.g. Tempor Tantrum's rum pick-up and delivery) and the three ranks. Requirements on the boat are
     * checked by {@link #trialBlocker}.
     */
    public record Trial(String id, String name, int level, int x, int y, List<int[]> crates, int[] objectiveA, int[] objectiveB,
                        List<Rank> ranks, String quest) {
        public String placeId() {
            return "barracuda_" + id;
        }
    }

    private static List<int[]> ring(int cx, int cy, int r, int n) {
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            out.add(new int[]{cx + (int) Math.round(Math.cos(a) * r), cy + (int) Math.round(Math.sin(a) * r)});
        }
        return out;
    }

    public static final List<Trial> TRIALS = List.of(
            // Swordfish crate tiles from the wiki route map; later ranks repeat the lap (approx).
            new Trial("tempor_tantrum", "The Tempor Tantrum", 30, 3035, 2850, List.of(new int[]{2994, 2891}, new int[]{2978, 2866},
                    new int[]{2981, 2848}, new int[]{2978, 2828}, new int[]{2990, 2808}, new int[]{3001, 2788}, new int[]{3012, 2768},
                    new int[]{3057, 2792}, new int[]{3065, 2811}, new int[]{3077, 2825}, new int[]{3078, 2863}, new int[]{3082, 2875},
                    new int[]{3084, 2896}, new int[]{3053, 2920}), new int[]{3037, 2766}, new int[]{3035, 2915},
                    List.of(new Rank("Swordfish", 385, 14, 15, 2, 19.5, 108), new Rank("Shark", 650, 25, 15, 4, 19.5, 171),
                            new Rank("Marlin", 1250, 36, 15, 6, 19.5, 270)), null),
            // Course layouts approx (a ring round the trial area).
            new Trial("jubbly_jive", "The Jubbly Jive", 55, 2400, 2970, ring(2400, 2970, 28, 20), new int[]{2400, 2998},
                    new int[]{2400, 2942}, List.of(new Rank("Swordfish", 1700, 20, 25, 3, 64, 123), new Rank("Shark", 3000, 38, 25, 5, 64, 180),
                    new Rank("Marlin", 6200, 56, 25, 9, 64, 321)), null),
            // Gwenith Glide crate counts approx (the wiki gives only the supply XP total: 1,050 / 2,065 / 3,360).
            new Trial("gwenith_glide", "The Gwenith Glide", 72, 2183, 3370, ring(2183, 3370, 30, 30), new int[]{2183, 3400},
                    new int[]{2183, 3340}, List.of(new Rank("Swordfish", 3050, 30, 35, 0, 0, 120), new Rank("Shark", 7250, 59, 35, 0, 0, 222),
                    new Rank("Marlin", 16050, 96, 35, 0, 0, 369)), "regicide"));

    public static Trial trial(String id) {
        for (Trial t : TRIALS) {
            if (t.id().equals(id)) {
                return t;
            }
        }
        return null;
    }

    /**
     * Why a player can't start a trial, or null: the level, a skiff, and the boat requirements (Tempor
     * Tantrum: iron helm and oak sails; Jubbly Jive: inoculation station; Gwenith Glide: adamant keel and Regicide).
     */
    public static String trialBlocker(Trial t, int sailing, String boat, Map<PartType, Integer> tiers, Set<String> facilities,
                                      boolean questDone) {
        if (sailing < t.level()) {
            return "You need a Sailing level of " + t.level() + " to attempt " + t.name() + ".";
        }
        if (!"skiff".equals(boat)) {
            return "Only skiffs may be used in the Barracuda Trials.";
        }
        switch (t.id()) {
            case "tempor_tantrum" -> {
                if (tiers.getOrDefault(PartType.HELM, 0) < 1 || tiers.getOrDefault(PartType.SAILS, 0) < 1) {
                    return "You need an iron helm and an oak mast with linen sails to brave the Storm Tempor.";
                }
            }
            case "jubbly_jive" -> {
                if (!facilities.contains("inoculation_station")) {
                    return "You need an inoculation station on your skiff to survive the fetid waters.";
                }
            }
            case "gwenith_glide" -> {
                if (!questDone) {
                    return "You need to complete Regicide to attempt The Gwenith Glide.";
                }
                if (tiers.getOrDefault(PartType.KEEL, 0) < 4) {
                    return "You need an adamant keel to sail through the crystal-flecked waters.";
                }
            }
            default -> {
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ helpers

    static String title(String id) {
        String s = id.replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static String article(String s) {
        String l = s.toLowerCase(Locale.ROOT);
        return ("aeiou".indexOf(l.charAt(0)) >= 0 ? "an " : "a ") + l;
    }
}
