package com.runecraft.osrs.content.islands;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Managing Miscellania (unlocked by Throne of Miscellania), after the OSRS wiki:
 * <ul>
 *   <li>The coffer holds 5,000,000 coins (7,500,000 after Royal Trouble). Every day at midnight UTC
 *       10% of it is paid out as wages, at most 50,000 (75,000 after Royal Trouble).</li>
 *   <li>Approval runs from 0 to 127 points (shown as a percentage, never below 25%). It falls 2.5% a day,
 *       or 1% after Royal Trouble, and is raised by working on the islands yourself.</li>
 *   <li>Ten subjects (fifteen after Royal Trouble), at most ten per kind of labour. Hardwood and farming
 *       are Etceteria's and need Royal Trouble. Each day's wage, scaled by approval, becomes "resource
 *       points"; what they turn into is decided at collection from the current allocation, so moving
 *       workers before collecting applies the new split to the whole period.</li>
 *   <li>Yields are the wiki's maxima for ten workers at 100% approval with a 75,000 wage (892 maple logs,
 *       546 coal, 440 tuna + 132 swordfish, 61 herbs or 1,250 flax, 301 teak / 223 mahogany / 121 + 121,
 *       481 seeds), scaled linearly by wage, approval and workers; that reproduces the wiki's pre-Royal
 *       Trouble example (595 maple logs at 50,000). Secondaries: a bird nest per 100 maple logs or per
 *       350 hardwood logs, and an uncut gem for the first 100 coal plus one per 200 coal after.</li>
 * </ul>
 * Approximations: the split of herbs and seeds between types, the gem types, the linear approval
 * scaling (Jagex's resource-point formula isn't public) and the storage cap (the wiki: "about eight months").
 */
public final class Kingdom {
    private Kingdom() {
    }

    public static final long MAX_COFFER = 5_000_000L;
    public static final long MAX_COFFER_ROYAL_TROUBLE = 7_500_000L;
    public static final int DAILY_CAP = 50_000;
    public static final int DAILY_CAP_ROYAL_TROUBLE = 75_000;
    public static final int MAX_APPROVAL = 127;
    /** Approval never falls below 25% (32 of 127). */
    public static final int MIN_APPROVAL = 32;
    /** Daily approval loss in points: 2.5% (3.2 points) before Royal Trouble, 1% (1.3) after. // approx: rounded */
    public static final int DAILY_DECAY = 3;
    public static final int DAILY_DECAY_ROYAL_TROUBLE = 1;
    public static final int WORKERS = 10;
    public static final int WORKERS_ROYAL_TROUBLE = 15;
    public static final int MAX_PER_TASK = 10;
    /** Days of production that can pile up uncollected (wiki: about eight months). // approx */
    public static final int MAX_DAYS = 240;
    /** Resource points a full 75,000-coin day at 100% approval earns. */
    public static final long FULL_DAY_POINTS = DAILY_CAP_ROYAL_TROUBLE;

    /** What the islanders spend their days on, with the wiki's yield for ten workers on a full day. */
    public enum Work {
        WOODCUTTING("Woodcutting (maple logs)", "wood", false, Map.of("maple_logs", 892)),
        MINING("Mining (coal)", "mining", false, Map.of("coal", 546)),
        FISHING("Fishing (raw tuna and swordfish)", "fishing", false, Map.of("raw_tuna", 440, "raw_swordfish", 132)),
        HERBS("Herbs", "herbs", false, herbs()),
        FLAX("Herbs (flax)", "herbs", false, Map.of("flax", 1250)),
        HARDWOOD("Hardwood (teak and mahogany)", "hardwood", true, Map.of("teak_logs", 121, "mahogany_logs", 121)),
        TEAK("Hardwood (teak only)", "hardwood", true, Map.of("teak_logs", 301)),
        MAHOGANY("Hardwood (mahogany only)", "hardwood", true, Map.of("mahogany_logs", 223)),
        FARMING("Farming (seeds)", "farm", true, seeds());

        private final String label;
        private final String group;
        private final boolean royalTrouble;
        private final Map<String, Integer> fullDay;

        Work(String label, String group, boolean royalTrouble, Map<String, Integer> fullDay) {
            this.label = label;
            this.group = group;
            this.royalTrouble = royalTrouble;
            this.fullDay = fullDay;
        }

        public String label() {
            return label;
        }

        /** Labours sharing a group share its ten-worker cap (herbs/flax, the hardwood options). */
        public String group() {
            return group;
        }

        public boolean needsRoyalTrouble() {
            return royalTrouble;
        }

        public Map<String, Integer> fullDay() {
            return fullDay;
        }
    }

    /** 61 herbs a full day, split between the nine herb types. // approx: the wiki gives no split */
    private static Map<String, Integer> herbs() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("grimy_tarromin", 11);
        m.put("grimy_harralander", 10);
        m.put("grimy_ranarr_weed", 5);
        m.put("grimy_irit_leaf", 9);
        m.put("grimy_avantoe", 8);
        m.put("grimy_kwuarm", 6);
        m.put("grimy_cadantine", 5);
        m.put("grimy_lantadyme", 4);
        m.put("grimy_dwarf_weed", 3);
        return m;
    }

    /** 481 seeds a full day, of many kinds. // approx: the wiki gives no split */
    private static Map<String, Integer> seeds() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("potato_seed", 80);
        m.put("onion_seed", 70);
        m.put("cabbage_seed", 60);
        m.put("tomato_seed", 50);
        m.put("barley_seed", 50);
        m.put("hammerstone_seed", 40);
        m.put("marigold_seed", 30);
        m.put("sweetcorn_seed", 30);
        m.put("redberry_seed", 25);
        m.put("strawberry_seed", 20);
        m.put("limpwurt_seed", 12);
        m.put("watermelon_seed", 8);
        m.put("acorn", 4);
        m.put("willow_seed", 2);
        return m;
    }

    /**
     * A player's kingdom: coffer, approval, workers per labour, the resource points banked since the
     * last collection and any items left over from before the allocation rework.
     */
    public record State(long coffer, int approval, Map<Work, Integer> workers, long points, Map<String, Integer> stock) {
        public State {
            EnumMap<Work, Integer> w = new EnumMap<>(Work.class);
            workers.forEach((k, v) -> {
                if (v > 0) {
                    w.put(k, v);
                }
            });
            workers = java.util.Collections.unmodifiableMap(w);
            stock = Map.copyOf(stock);
        }

        /** After the quest: approval starts at 25% and all ten subjects cut maples. // approx start */
        public static State fresh() {
            return new State(0, MIN_APPROVAL, Map.of(Work.WOODCUTTING, WORKERS), 0, Map.of());
        }

        public int approvalPercent() {
            return Math.round(approval * 100f / MAX_APPROVAL);
        }

        public int assigned() {
            return workers.values().stream().mapToInt(Integer::intValue).sum();
        }

        public int workers(Work w) {
            return workers.getOrDefault(w, 0);
        }
    }

    public static long maxCoffer(boolean royalTrouble) {
        return royalTrouble ? MAX_COFFER_ROYAL_TROUBLE : MAX_COFFER;
    }

    public static int maxWorkers(boolean royalTrouble) {
        return royalTrouble ? WORKERS_ROYAL_TROUBLE : WORKERS;
    }

    /** The day's wage: 10% of the coffer, at most 50,000 (75,000 after Royal Trouble). */
    public static int dailySpend(long coffer, boolean royalTrouble) {
        long cap = royalTrouble ? DAILY_CAP_ROYAL_TROUBLE : DAILY_CAP;
        return (int) Math.min(coffer / 10, cap);
    }

    /** Resource points a day's wage earns at an approval rating. */
    public static long dayPoints(int spend, int approval) {
        return (long) spend * Math.max(0, Math.min(MAX_APPROVAL, approval)) / MAX_APPROVAL;
    }

    /** Advances the kingdom by {@code days} days: wages paid, points banked, approval decayed. */
    public static State advance(State s, int days, boolean royalTrouble) {
        long coffer = s.coffer();
        int approval = s.approval();
        long points = s.points();
        int decay = royalTrouble ? DAILY_DECAY_ROYAL_TROUBLE : DAILY_DECAY;
        for (int d = 0; d < Math.min(days, MAX_DAYS); d++) {
            int spend = dailySpend(coffer, royalTrouble);
            coffer -= spend;
            points = Math.min(points + dayPoints(spend, approval), MAX_DAYS * FULL_DAY_POINTS);
            approval = Math.max(Math.min(approval, MIN_APPROVAL), approval - decay);
        }
        return new State(coffer, approval, s.workers(), points, s.stock());
    }

    /** What collecting now gives: the banked points spread over the current allocation, plus secondaries. */
    public static Map<String, Integer> collect(State s, boolean royalTrouble) {
        Map<String, Integer> out = new LinkedHashMap<>(s.stock());
        for (Map.Entry<Work, Integer> e : s.workers().entrySet()) {
            Work w = e.getKey();
            if (w.needsRoyalTrouble() && !royalTrouble) {
                continue;
            }
            double factor = s.points() / (double) FULL_DAY_POINTS * Math.min(MAX_PER_TASK, e.getValue()) / 10.0;
            int logsForNests = 0;
            for (Map.Entry<String, Integer> item : w.fullDay().entrySet()) {
                int n = (int) Math.floor(item.getValue() * factor + 1e-9);
                if (n > 0) {
                    out.merge(item.getKey(), n, Integer::sum);
                    logsForNests += n;
                }
            }
            switch (w) {
                case WOODCUTTING -> add(out, "bird_nest", logsForNests / 100);
                case HARDWOOD, TEAK, MAHOGANY -> add(out, "bird_nest", logsForNests / 350);
                case MINING -> gems(out, out.getOrDefault("coal", 0) - s.stock().getOrDefault("coal", 0));
                default -> {
                }
            }
        }
        return out;
    }

    /** Uncut gems for {@code coal} coal: one for the first 100, one more per 200 after (rounded down). */
    public static int gemsFor(int coal) {
        return coal < 100 ? 0 : 1 + (coal - 100) / 200;
    }

    private static final String[] GEMS = {"uncut_sapphire", "uncut_emerald", "uncut_ruby", "uncut_diamond"};
    /** Gem split. // approx */
    private static final int[] GEM_WEIGHTS = {4, 3, 2, 1};

    private static void gems(Map<String, Integer> out, int coal) {
        int n = gemsFor(coal);
        for (int i = 0; i < n; i++) {
            int slot = i % 10;
            int g = slot < GEM_WEIGHTS[0] ? 0 : slot < 7 ? 1 : slot < 9 ? 2 : 3;
            add(out, GEMS[g], 1);
        }
    }

    private static void add(Map<String, Integer> out, String item, int n) {
        if (n > 0) {
            out.merge(item, n, Integer::sum);
        }
    }

    /** Deposits coins (up to the coffer's limit); returns the new state. */
    public static State deposit(State s, long coins, boolean royalTrouble) {
        return new State(s.coffer() + depositable(s, coins, royalTrouble), s.approval(), s.workers(), s.points(), s.stock());
    }

    /** How many of {@code coins} fit in the coffer. */
    public static long depositable(State s, long coins, boolean royalTrouble) {
        return Math.min(Math.max(0, maxCoffer(royalTrouble) - s.coffer()), Math.max(0, coins));
    }

    /** Withdraws up to {@code coins}; returns the new state. */
    public static State withdraw(State s, long coins) {
        long n = Math.min(s.coffer(), Math.max(0, coins));
        return new State(s.coffer() - n, s.approval(), s.workers(), s.points(), s.stock());
    }

    /** Approval after gaining {@code points} (capped at 100%). */
    public static State work(State s, int points) {
        return new State(s.coffer(), Math.max(0, Math.min(MAX_APPROVAL, s.approval() + points)), s.workers(), s.points(), s.stock());
    }

    /**
     * Moves one worker on or off a labour; returns the state unchanged if the move isn't allowed (no idle
     * subject, ten already on that group, or Etceteria labour before Royal Trouble).
     */
    public static State assign(State s, Work w, int delta, boolean royalTrouble) {
        int now = s.workers(w);
        int next = now + delta;
        if (next < 0 || (delta > 0 && (w.needsRoyalTrouble() && !royalTrouble || s.assigned() + delta > maxWorkers(royalTrouble)
                || groupCount(s, w.group()) + delta > MAX_PER_TASK))) {
            return s;
        }
        Map<Work, Integer> m = new EnumMap<>(Work.class);
        m.putAll(s.workers());
        m.put(w, next);
        return new State(s.coffer(), s.approval(), m, s.points(), s.stock());
    }

    static int groupCount(State s, String group) {
        int n = 0;
        for (Map.Entry<Work, Integer> e : s.workers().entrySet()) {
            if (e.getKey().group().equals(group)) {
                n += e.getValue();
            }
        }
        return n;
    }

    public static State collected(State s) {
        return new State(s.coffer(), s.approval(), s.workers(), 0, Map.of());
    }

    /**
     * Approval points earned by obtaining {@code item} while working on the islands (OSRS wiki table):
     * +1 per raked weed or mined coal, +2 per gem found mining, a 50% chance of +1 per maple log or fish
     * (60% / 65% with the medium / elite Fremennik diary), and on Etceteria after Royal Trouble +1 per teak
     * or mahogany log. {@code roll} is a uniform random number in [0, 1).
     */
    public static int approvalFor(String item, double roll, boolean royalTrouble, double diaryBonus) {
        return switch (item) {
            case "weeds", "coal" -> 1;
            case "uncut_sapphire", "uncut_emerald", "uncut_ruby", "uncut_diamond" -> 2;
            case "maple_logs", "raw_tuna", "raw_lobster", "raw_swordfish" -> roll < 0.5 + diaryBonus ? 1 : 0;
            case "teak_logs", "mahogany_logs" -> royalTrouble ? 1 : 0;
            default -> 0;
        };
    }

    /** Miscellania and Etceteria, in OSRS tiles (x1, y1, x2, y2). */
    public static final List<int[]> ISLANDS = List.of(new int[]{2490, 3835, 2640, 3900});

    public static boolean onIslands(int x, int y) {
        for (int[] b : ISLANDS) {
            if (x >= b[0] && x <= b[2] && y >= b[1] && y <= b[3]) {
                return true;
            }
        }
        return false;
    }
}
