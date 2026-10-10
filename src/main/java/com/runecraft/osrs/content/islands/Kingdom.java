package com.runecraft.osrs.content.islands;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Managing Miscellania (unlocked by Throne of Miscellania). The royal coffer pays the islanders:
 * each day up to 10% of the coffer is spent, capped at 50,000 coins (75,000 after Royal Trouble),
 * and the workers gather resources in proportion to the money spent and the kingdom's approval
 * rating. Approval (0-127, shown as a percentage) slowly falls every day and is raised by working
 * on the islands yourself (chopping, fishing, mining, raking...).
 *
 * <p>The per-day yields at full approval and the maximum daily spend are approximations of the OSRS
 * wiki's averages for ten workers in one category.
 */
public final class Kingdom {
    private Kingdom() {
    }

    public static final long MAX_COFFER = 7_500_000L;
    public static final int DAILY_CAP = 50_000;
    public static final int DAILY_CAP_ROYAL_TROUBLE = 75_000;
    public static final int MAX_APPROVAL = 127;
    /** Approval never decays below 25%. */
    public static final int MIN_APPROVAL = 32;
    /** Approval lost per day (about 1.6%). */
    public static final int DAILY_DECAY = 2;
    /** Days of production that can pile up uncollected. */
    public static final int MAX_DAYS = 60;

    /** What the islanders spend their days on, with the yield of a full-approval 75k day. */
    public enum Work {
        WOODCUTTING("Woodcutting (maple logs)", Map.of("maple_logs", 330)),
        MINING("Mining (coal)", Map.of("coal", 320)),
        FISHING("Fishing (raw tuna and swordfish)", Map.of("raw_tuna", 200, "raw_swordfish", 60)),
        HERBS("Herbs", Map.of("grimy_irit_leaf", 10, "grimy_avantoe", 10, "grimy_ranarr_weed", 8, "grimy_kwuarm", 8,
                "grimy_cadantine", 4)),
        FLAX("Flax", Map.of("flax", 1000)),
        HARDWOOD("Hardwood (teak and mahogany logs)", Map.of("teak_logs", 200, "mahogany_logs", 100));

        private final String label;
        private final Map<String, Integer> fullDay;

        Work(String label, Map<String, Integer> fullDay) {
            this.label = label;
            this.fullDay = fullDay;
        }

        public String label() {
            return label;
        }

        public Map<String, Integer> fullDay() {
            return fullDay;
        }

        public Work next() {
            Work[] all = values();
            return all[(ordinal() + 1) % all.length];
        }
    }

    /** A player's kingdom: coffer, approval, allocation and the resources waiting to be collected. */
    public record State(long coffer, int approval, Work work, Map<String, Integer> stock) {
        public State {
            stock = Map.copyOf(stock);
        }

        public static State fresh() {
            return new State(0, MAX_APPROVAL / 2, Work.WOODCUTTING, Map.of());
        }

        public int approvalPercent() {
            return Math.round(approval * 100f / MAX_APPROVAL);
        }
    }

    public static int dailySpend(long coffer, boolean royalTrouble) {
        long cap = royalTrouble ? DAILY_CAP_ROYAL_TROUBLE : DAILY_CAP;
        return (int) Math.min(coffer / 10, cap);
    }

    /** Resources produced in one day by spending {@code spend} coins at the given approval. */
    public static Map<String, Integer> dayYield(Work work, int spend, int approval) {
        Map<String, Integer> out = new LinkedHashMap<>();
        double factor = spend / (double) DAILY_CAP_ROYAL_TROUBLE * Math.max(0, Math.min(MAX_APPROVAL, approval)) / MAX_APPROVAL;
        for (Map.Entry<String, Integer> e : work.fullDay().entrySet()) {
            int n = (int) Math.floor(e.getValue() * factor);
            if (n > 0) {
                out.put(e.getKey(), n);
            }
        }
        return out;
    }

    /** Advances the kingdom by {@code days} days (at most {@link #MAX_DAYS}). */
    public static State advance(State s, int days, boolean royalTrouble) {
        long coffer = s.coffer();
        int approval = s.approval();
        Map<String, Integer> stock = new LinkedHashMap<>(s.stock());
        for (int d = 0; d < Math.min(days, MAX_DAYS); d++) {
            int spend = dailySpend(coffer, royalTrouble);
            coffer -= spend;
            dayYield(s.work(), spend, approval).forEach((item, n) -> stock.merge(item, n, Integer::sum));
            approval = Math.max(Math.min(approval, MIN_APPROVAL), approval - DAILY_DECAY);
        }
        return new State(coffer, approval, s.work(), stock);
    }

    /** Deposits coins (the coffer holds at most 7.5m); returns the new state. */
    public static State deposit(State s, long coins) {
        long room = Math.max(0, MAX_COFFER - s.coffer());
        return new State(s.coffer() + Math.min(room, Math.max(0, coins)), s.approval(), s.work(), s.stock());
    }

    /** How many of {@code coins} fit in the coffer. */
    public static long depositable(State s, long coins) {
        return Math.min(Math.max(0, MAX_COFFER - s.coffer()), Math.max(0, coins));
    }

    public static State work(State s, int approvalGain) {
        return new State(s.coffer(), Math.min(MAX_APPROVAL, s.approval() + approvalGain), s.work(), s.stock());
    }

    public static State reallocate(State s) {
        return new State(s.coffer(), s.approval(), s.work().next(), s.stock());
    }

    public static State collected(State s) {
        return new State(s.coffer(), s.approval(), s.work(), Map.of());
    }

    /** Skills whose actions on Miscellania and Etceteria raise approval. */
    public static final List<String> HELPFUL_SKILLS = List.of("WOODCUTTING", "FISHING", "MINING", "FARMING");
}
