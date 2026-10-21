package com.runecraft.osrs.content.minigames;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Giants' Foundry (Kovac, below the Giants' Plateau): commissions, moulds, the crucible's alloy, the refinement of the
 * preform and the hand-in rewards. Numbers are from the OSRS Wiki ("Giants' Foundry", "Giants' Foundry/Mould List",
 * "Giants' Foundry Reward Shop", "Ore pack (Giants' Foundry)") and the Easy Giants' Foundry RuneLite plugin, which reads
 * the game's varbits (heat and progress in tenths of a percent, the tools' tick cycles and the lava/waterfall rates).
 *
 * <ul>
 *     <li>A commission is two styles (Broad, Narrow, Heavy or Light; then Heavy, Light, Spiked or Flat): 12 in all.</li>
 *     <li>The mould score is the three moulds' scores for both styles; the metal score comes from the crucible's 28 bars
 *     (two most abundant metals); the sword starts at their sum and loses 10 quality per mistake.</li>
 *     <li>Hand-in: (floor(q^2/73) + floor(1.5q) + 1) x 30 Smithing XP, twice that in coins and q reputation.</li>
 * </ul>
 */
public final class GiantsFoundry {
    private GiantsFoundry() {
    }

    /** Bars' worth of metal the crucible must hold before it can be poured. */
    public static final int CRUCIBLE = 28;
    /** Heat and progress run from 0 to 1,000 (tenths of a percent), as in the game's varbits. */
    public static final int MAX = 1000;
    /** Quality lost per mistake (wrong tool, or the right tool at the wrong temperature). */
    public static final int MISTAKE = 10;
    /** A sweet spot (bonus window) stays open this many game ticks. */
    public static final int BONUS_TICKS = 10;
    /** Progress a caught sweet spot adds on the machine's next tick ("around 5%"). */
    public static final int BONUS_PROGRESS = 50;
    /** Reward for a sword handed in at 0 quality: 30 Smithing XP and 60 coins, whatever it was made of. */
    public static final int RUINED_XP = 30;

    // ------------------------------------------------------------------ commissions and moulds

    /** The six sword styles, in the Mould List's column order. */
    public enum Style {
        NARROW, BROAD, LIGHT, HEAVY, FLAT, SPIKED;

        public String display() {
            return name().charAt(0) + name().substring(1).toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** A commission: Kovac's two descriptors. */
    public record Commission(Style first, Style second) {
        public String display() {
            return first.display() + " " + second.display();
        }
    }

    private static final Style[] FIRST = {Style.BROAD, Style.NARROW, Style.HEAVY, Style.LIGHT};
    private static final Style[] SECOND = {Style.HEAVY, Style.LIGHT, Style.SPIKED, Style.FLAT};

    /** The 12 commissions: never two of Heavy and Light together (OSRS Wiki, best-mould tables). */
    public static final List<Commission> COMMISSIONS = commissions();

    private static List<Commission> commissions() {
        List<Commission> list = new ArrayList<>();
        for (Style a : FIRST) {
            for (Style b : SECOND) {
                boolean weight = (a == Style.HEAVY || a == Style.LIGHT) && (b == Style.HEAVY || b == Style.LIGHT);
                if (!weight) {
                    list.add(new Commission(a, b));
                }
            }
        }
        return List.copyOf(list);
    }

    public enum Part { FORTE, BLADE, TIP }

    /**
     * A mould: Smithing level to use it (15 for the default ones), reputation cost (0 = unlocked by default) and its score
     * for each {@link Style} (Narrow, Broad, Light, Heavy, Flat, Spiked).
     */
    public record Mould(String id, String name, Part part, int level, int cost, int[] scores) {
        public int score(Style s) {
            return scores[s.ordinal()];
        }

        public int score(Commission c) {
            return score(c.first()) + score(c.second());
        }

        public boolean purchasable() {
            return cost > 0;
        }

        /** MinigameData flag a bought mould sets ({@link RewardShops}: Kovac sells the unlocks). */
        public String flag() {
            return "gf_mould_" + id;
        }
    }

    private static Mould m(String id, String name, Part part, int level, int cost, int narrow, int broad, int light, int heavy,
                           int flat, int spiked) {
        return new Mould(id, name, part, level, cost, new int[]{narrow, broad, light, heavy, flat, spiked});
    }

    /** Every mould (OSRS Wiki "Giants' Foundry/Mould List"; the snapshot test diffs this against the wiki table). */
    public static final List<Mould> MOULDS = List.of(
            m("saw_tip", "Saw Tip", Part.TIP, 15, 0, 0, 6, 0, 8, 0, 8),
            m("gladius_point", "Gladius Point", Part.TIP, 15, 0, 8, 0, 0, 8, 6, 0),
            m("serpents_fang", "Serpent's Fang", Part.TIP, 15, 0, 8, 0, 6, 0, 0, 8),
            m("medusas_head", "Medusa's Head", Part.TIP, 15, 0, 0, 4, 0, 4, 0, 4),
            m("chopper_tip", "Chopper Tip", Part.TIP, 15, 0, 0, 4, 4, 0, 0, 4),
            m("people_poker_point", "People Poker Point", Part.TIP, 15, 0, 4, 0, 0, 4, 4, 0),
            m("corrupted_point", "Corrupted Point", Part.TIP, 49, 300, 8, 0, 10, 0, 0, 8),
            m("defenders_tip", "Defenders Tip", Part.TIP, 52, 300, 0, 10, 0, 8, 0, 8),
            m("serrated_tip", "Serrated Tip", Part.TIP, 60, 350, 4, 0, 16, 0, 0, 4),
            m("needle_point", "Needle Point", Part.TIP, 69, 400, 18, 0, 3, 0, 4, 0),
            m("the_point", "The Point!", Part.TIP, 81, 450, 0, 2, 1, 0, 22, 0),
            m("gladius_edge", "Gladius Edge", Part.BLADE, 15, 0, 6, 0, 0, 8, 8, 0),
            m("stiletto_blade", "Stiletto Blade", Part.BLADE, 15, 0, 8, 0, 6, 0, 8, 0),
            m("medusa_blade", "Medusa Blade", Part.BLADE, 15, 0, 0, 8, 0, 8, 6, 0),
            m("fish_blade", "Fish Blade", Part.BLADE, 15, 0, 4, 0, 4, 0, 4, 0),
            m("defenders_edge", "Defenders Edge", Part.BLADE, 15, 0, 0, 4, 0, 4, 0, 4),
            m("saw_blade", "Saw Blade", Part.BLADE, 15, 0, 0, 4, 4, 0, 0, 4),
            m("flamberge_blade", "Flamberge Blade", Part.BLADE, 48, 300, 8, 0, 8, 0, 0, 10),
            m("serpent_blade", "Serpent Blade", Part.BLADE, 50, 300, 10, 0, 8, 0, 8, 0),
            m("claymore_blade", "Claymore Blade", Part.BLADE, 59, 350, 0, 16, 0, 4, 4, 0),
            m("fleur_de_blade", "Fleur de Blade", Part.BLADE, 71, 400, 0, 4, 0, 18, 0, 1),
            m("choppa", "Choppa!", Part.BLADE, 80, 450, 0, 1, 22, 0, 2, 0),
            m("serrated_forte", "Serrated Forte", Part.FORTE, 15, 0, 8, 0, 0, 8, 0, 6),
            m("serpent_ricasso", "Serpent Ricasso", Part.FORTE, 15, 0, 6, 0, 8, 0, 8, 0),
            m("medusa_ricasso", "Medusa Ricasso", Part.FORTE, 15, 0, 0, 8, 0, 6, 8, 0),
            m("disarming_forte", "Disarming Forte", Part.FORTE, 15, 0, 4, 0, 4, 0, 0, 4),
            m("gladius_ricasso", "Gladius Ricasso", Part.FORTE, 15, 0, 0, 4, 0, 4, 4, 0),
            m("chopper_forte", "Chopper Forte", Part.FORTE, 15, 0, 0, 4, 4, 0, 4, 0),
            m("stiletto_forte", "Stiletto Forte", Part.FORTE, 50, 300, 8, 0, 10, 0, 8, 0),
            m("defender_base", "Defender Base", Part.FORTE, 51, 300, 0, 8, 0, 10, 8, 0),
            m("juggernaut_forte", "Juggernaut Forte", Part.FORTE, 61, 350, 0, 4, 0, 4, 0, 16),
            m("chopper_forte_plus", "Chopper Forte +1", Part.FORTE, 70, 400, 0, 3, 4, 0, 18, 0),
            m("spiker", "Spiker!", Part.FORTE, 79, 450, 1, 0, 0, 2, 0, 22));

    public static Mould mould(String id) {
        for (Mould m : MOULDS) {
            if (m.id().equals(id)) {
                return m;
            }
        }
        return null;
    }

    /**
     * The best mould of each part for a commission among those {@code usable} (the jig lists the best first): forte, blade,
     * tip. Ties keep the cheaper (default) mould.
     */
    public static Mould[] bestMoulds(Commission c, Predicate<Mould> usable) {
        Mould[] best = new Mould[Part.values().length];
        for (Mould m : MOULDS) {
            if (!usable.test(m)) {
                continue;
            }
            Mould cur = best[m.part().ordinal()];
            if (cur == null || m.score(c) > cur.score(c) || (m.score(c) == cur.score(c) && m.cost() < cur.cost())) {
                best[m.part().ordinal()] = m;
            }
        }
        return best;
    }

    public static int mouldScore(Commission c, Mould... moulds) {
        int sum = 0;
        for (Mould m : moulds) {
            if (m != null) {
                sum += m.score(c);
            }
        }
        return sum;
    }

    // ------------------------------------------------------------------ metal

    /** The six metals (tier 1-6), their bar, item prefix and the Smithing level to add them (the smelting level). */
    public enum Metal {
        BRONZE("bronze", "bronze_bar", 1), IRON("iron", "iron_bar", 15), STEEL("steel", "steel_bar", 30),
        MITHRIL("mithril", "mithril_bar", 50), ADAMANT("adamant", "adamantite_bar", 70), RUNE("rune", "runite_bar", 85);

        private final String prefix;
        private final String bar;
        private final int level;

        Metal(String prefix, String bar, int level) {
            this.prefix = prefix;
            this.bar = bar;
            this.level = level;
        }

        public int tier() {
            return ordinal() + 1;
        }

        public String prefix() {
            return prefix;
        }

        public String bar() {
            return bar;
        }

        public int level() {
            return level;
        }
    }

    /**
     * Metal equipment the crucible takes and how many bars each is worth (the bars to smith it minus one; OSRS Wiki).
     * Item ids are {@code <metal prefix>_<suffix>}.
     */
    public static final List<String> EQUIPMENT_1 = List.of("scimitar", "longsword", "full_helm", "sq_shield", "claws");
    public static final List<String> EQUIPMENT_2 = List.of("warhammer", "battleaxe", "chainbody", "kiteshield", "2h_sword",
            "platelegs", "plateskirt");
    public static final List<String> EQUIPMENT_4 = List.of("platebody");

    /** Bars' worth of metal an item adds to the crucible (0 if it can't go in), and its metal via {@link #metalOf}. */
    public static int barValue(String item) {
        Metal metal = metalOf(item);
        if (metal == null) {
            return 0;
        }
        if (item.equals(metal.bar())) {
            return 1;
        }
        String suffix = item.substring(metal.prefix().length() + 1);
        if (EQUIPMENT_1.contains(suffix)) {
            return 1;
        }
        if (EQUIPMENT_2.contains(suffix)) {
            return 2;
        }
        return EQUIPMENT_4.contains(suffix) ? 4 : 0;
    }

    public static Metal metalOf(String item) {
        for (Metal m : Metal.values()) {
            if (item.equals(m.bar())) {
                return m;
            }
            if (item.startsWith(m.prefix() + "_")) {
                String suffix = item.substring(m.prefix().length() + 1);
                if (EQUIPMENT_1.contains(suffix) || EQUIPMENT_2.contains(suffix) || EQUIPMENT_4.contains(suffix)) {
                    return m;
                }
            }
        }
        return null;
    }

    /**
     * The metal score of a crucible holding {@code amounts[tier - 1]} bars' worth of each metal (OSRS Wiki): only the two
     * most abundant metals count (ties prefer the more valuable), V = 10 x tier x amount / 28, and the score is
     * floor((floor(10 V1) + floor(10 V2) + floor(V1 V2)) / 10), in exact integer arithmetic.
     */
    public static int metalScore(int[] amounts) {
        int first = -1;
        int second = -1;
        for (int t = amounts.length - 1; t >= 0; t--) {
            if (amounts[t] <= 0) {
                continue;
            }
            if (first < 0 || amounts[t] > amounts[first]) {
                second = first;
                first = t;
            } else if (second < 0 || amounts[t] > amounts[second]) {
                second = t;
            }
        }
        if (first < 0) {
            return 0;
        }
        long t1 = first + 1;
        long a1 = amounts[first];
        long t2 = second < 0 ? 0 : second + 1;
        long a2 = second < 0 ? 0 : amounts[second];
        long tenV1 = 100 * t1 * a1 / CRUCIBLE;
        long tenV2 = 100 * t2 * a2 / CRUCIBLE;
        long v1v2 = 100 * t1 * a1 * t2 * a2 / ((long) CRUCIBLE * CRUCIBLE);
        return (int) ((tenV1 + tenV2 + v1v2) / 10);
    }

    /** Hammer/grind/polish sections for a metal score: 10-19 three, 20-59 four, 60-89 five, 90-119 six, 120+ seven. */
    public static int sections(int metalScore) {
        if (metalScore < 20) {
            return 3;
        }
        if (metalScore < 60) {
            return 4;
        }
        if (metalScore < 90) {
            return 5;
        }
        return metalScore < 120 ? 6 : 7;
    }

    /** Sweet spots per sword: three on six- and seven-section swords, otherwise two. */
    public static int bonuses(int sections) {
        return sections >= 6 ? 3 : 2;
    }

    // ------------------------------------------------------------------ refinement

    /**
     * The machines: the heat range they need, the game ticks between their actions, the progress and heat change per
     * action (tenths of a percent; Easy Giants' Foundry's {@code Stage}) and the Smithing XP per action (OSRS Wiki).
     */
    public enum Tool {
        TRIP_HAMMER("trip hammer", "foundry_trip_hammer", Heat.HIGH, 5, 20, -25, 4.5),
        GRINDSTONE("grindstone", "foundry_grindstone", Heat.MEDIUM, 2, 10, 15, 3.2),
        POLISHING_WHEEL("polishing wheel", "foundry_polishing_wheel", Heat.LOW, 2, 10, -17, 3.3);

        private final String display;
        private final String block;
        private final Heat heat;
        private final int cycle;
        private final int progress;
        private final int heatChange;
        private final double xp;

        Tool(String display, String block, Heat heat, int cycle, int progress, int heatChange, double xp) {
            this.display = display;
            this.block = block;
            this.heat = heat;
            this.cycle = cycle;
            this.progress = progress;
            this.heatChange = heatChange;
            this.xp = xp;
        }

        public String display() {
            return display;
        }

        public String block() {
            return block;
        }

        public Heat heat() {
            return heat;
        }

        public int cycle() {
            return cycle;
        }

        public int progress() {
            return progress;
        }

        public int heatChange() {
            return heatChange;
        }

        public double xp() {
            return xp;
        }

        public static Tool byBlock(String block) {
            for (Tool t : values()) {
                if (t.block.equals(block)) {
                    return t;
                }
            }
            return null;
        }
    }

    /** The three heat ranges, centred at 1/6, 3/6 and 5/6 of the bar (Easy Giants' Foundry). */
    public enum Heat {
        LOW(1), MEDIUM(3), HIGH(5);

        private final int sixths;

        Heat(int sixths) {
            this.sixths = sixths;
        }

        /** The range [low, high] for a sword of this metal score. */
        public int[] range(int metalScore) {
            int half = rangeWidth(metalScore) / 2;
            int centre = MAX * sixths / 6;
            return new int[]{centre - half, centre + half};
        }

        public boolean contains(int heat, int metalScore) {
            int[] r = range(metalScore);
            return heat >= r[0] && heat <= r[1];
        }
    }

    /** Width of each heat range at the easiest swords (tenths of a percent). */
    public static final int FULL_RANGE = 300; // approx: the HUD's ranges are read from widgets, the wiki gives no size

    /** The ranges shrink linearly with the metal score, to half their full size at 130 (OSRS Wiki). */
    public static int rangeWidth(int metalScore) {
        int s = Math.max(0, Math.min(130, metalScore));
        return FULL_RANGE - FULL_RANGE * s / 260;
    }

    /**
     * Heat added (lava pool) or removed (waterfall) on each tick of a heating or cooling action; the rate grows the longer
     * it goes on. "Heat"/"Cool" start at the beginning, "Dunk"/"Quench" at {@link #FAST_START} (Easy Giants' Foundry,
     * measured in game). The last value repeats.
     */
    public static final int[] HEAT_STEPS = {7, 8, 9, 11, 13, 15, 17, 19, 21, 24, 27, 30, 33, 37, 41, 45, 49, 53, 57, 62, 67, 72, 77,
            83, 89, 95};
    public static final int FAST_START = 10;
    /** The preform cools by this much every game tick on its own. */
    public static final int PASSIVE_COOLING = 1;

    public static int heatStep(int index) {
        return HEAT_STEPS[Math.max(0, Math.min(HEAT_STEPS.length - 1, index))];
    }

    /** Each piece of the Smiths' Uniform gives a 20% chance of 1 extra progress per action; the full set always does. */
    public static double uniformChance(int pieces) {
        return pieces >= 4 ? 1.0 : 0.2 * Math.max(0, pieces);
    }

    public static final List<String> UNIFORM = List.of("smiths_tunic", "smiths_trousers", "smiths_boots", "smiths_gloves");

    /** Sections' tools: a random plan where no two neighbouring sections use the same machine. */
    public static Tool[] plan(int sections, java.util.Random random) {
        Tool[] plan = new Tool[sections];
        for (int i = 0; i < sections; i++) {
            Tool t;
            do {
                t = Tool.values()[random.nextInt(Tool.values().length)];
            } while (i > 0 && t == plan[i - 1]); // approx: the game's section order isn't documented
            plan[i] = t;
        }
        return plan;
    }

    /** The section (0-based) a progress value lies in. */
    public static int section(int progress, int sections) {
        return Math.min(sections - 1, Math.max(0, progress) * sections / MAX);
    }

    // ------------------------------------------------------------------ rewards

    /** Smithing XP for handing in a sword of quality q: (floor(q^2/73) + floor(1.5q) + 1) x 30 (30 to 25,230). */
    public static int xp(int quality) {
        int q = Math.max(0, quality);
        return (q * q / 73 + q * 3 / 2 + 1) * 30;
    }

    /** Coins for a sword: twice the experience (60 for a ruined sword). */
    public static int coins(int quality) {
        return 2 * xp(quality);
    }

    /** Foundry reputation for a sword: its final quality. */
    public static int reputation(int quality) {
        return Math.max(0, quality);
    }

    /** Ore pack (200 reputation): 30 noted ores rolled from the wiki's crowd-sourced distribution (mostly coal). */
    public static final int ORE_PACK_ROLLS = 30;
    public static final List<MinigameRules.Loot> ORE_PACK = List.of(
            new MinigameRules.Loot("coal", 1, 1, 46854), new MinigameRules.Loot("iron_ore", 1, 1, 5803),
            new MinigameRules.Loot("mithril_ore", 1, 1, 3701), new MinigameRules.Loot("adamantite_ore", 1, 1, 870),
            new MinigameRules.Loot("runite_ore", 1, 1, 162));
    /** Reward-shop id of the ore pack (rolled when bought). */
    public static final String ORE_PACK_ITEM = "#gf_ores";

    /** Sleeping Giants unlocks the foundry. */
    public static final String QUEST = "sleeping_giants";

    // ------------------------------------------------------------------ preform storage

    /** Prefix of the per-player integer stats a {@link Stored} commission is saved under. */
    public static final String STORE = "gf_store:";
    /** The stat that says a commission is waiting in storage (1) or not (0). */
    public static final String STORE_KEPT = STORE + "kept";

    /**
     * A commission kept while the player is away (OSRS Wiki "Giants' Foundry": "The player may leave the Foundry and come
     * back anytime ... your current commission is being kept warm in the preform storage"): the commission, the moulds set,
     * the crucible's metal (per {@link Metal}), and the poured preform's metal score, plan, progress, heat, quality,
     * mistakes and sweet spots. {@link #encode} and {@link #decode} turn it into integer stats under {@link #STORE}.
     */
    public record Stored(Commission commission, Mould[] moulds, int[] metal, int barsIn, boolean poured, int metalScore, Tool[] plan,
                         int progress, int heat, int quality, int startQuality, int mistakes, int bonusesLeft, int bonusesTotal) {
        /** Whether there is anything to keep: a commission, or metal in the crucible. */
        public boolean worthKeeping() {
            return commission != null || barsIn > 0;
        }

        /** The stats to save (every key under {@link #STORE}, {@link #STORE_KEPT} = 1). */
        public java.util.Map<String, Integer> encode() {
            java.util.Map<String, Integer> out = new java.util.LinkedHashMap<>();
            out.put(STORE_KEPT, 1);
            out.put(STORE + "commission", commission == null ? 0 : COMMISSIONS.indexOf(commission) + 1);
            int mouldBits = 0;
            if (moulds != null) {
                for (int i = 0; i < moulds.length && i < Part.values().length; i++) {
                    mouldBits |= (moulds[i] == null ? 0 : MOULDS.indexOf(moulds[i]) + 1) << (8 * i);
                }
            }
            out.put(STORE + "moulds", mouldBits);
            for (Metal m : Metal.values()) {
                out.put(STORE + "metal_" + m.prefix(), metal == null || m.ordinal() >= metal.length ? 0 : metal[m.ordinal()]);
            }
            out.put(STORE + "bars", barsIn);
            out.put(STORE + "poured", poured ? 1 : 0);
            out.put(STORE + "metal_score", metalScore);
            int planDigits = 0;
            for (int i = (plan == null ? 0 : plan.length) - 1; i >= 0; i--) {
                planDigits = planDigits * 4 + plan[i].ordinal() + 1;
            }
            out.put(STORE + "plan", planDigits);
            out.put(STORE + "progress", progress);
            out.put(STORE + "heat", heat);
            out.put(STORE + "quality", quality);
            out.put(STORE + "start_quality", startQuality);
            out.put(STORE + "mistakes", mistakes);
            out.put(STORE + "bonuses_left", bonusesLeft);
            out.put(STORE + "bonuses_total", bonusesTotal);
            return out;
        }

        /** The commission kept in storage, read from the player's stats; null when none is kept. */
        public static Stored decode(java.util.function.ToIntFunction<String> stat) {
            if (stat.applyAsInt(STORE_KEPT) != 1) {
                return null;
            }
            int c = stat.applyAsInt(STORE + "commission");
            Commission commission = c >= 1 && c <= COMMISSIONS.size() ? COMMISSIONS.get(c - 1) : null;
            int mouldBits = stat.applyAsInt(STORE + "moulds");
            Mould[] moulds = null;
            if (mouldBits != 0) {
                moulds = new Mould[Part.values().length];
                for (int i = 0; i < moulds.length; i++) {
                    int idx = (mouldBits >> (8 * i)) & 0xFF;
                    moulds[i] = idx >= 1 && idx <= MOULDS.size() ? MOULDS.get(idx - 1) : null;
                }
                if (java.util.Arrays.asList(moulds).contains(null)) {
                    moulds = null; // set the jig again
                }
            }
            int[] metal = new int[Metal.values().length];
            for (Metal m : Metal.values()) {
                metal[m.ordinal()] = Math.max(0, stat.applyAsInt(STORE + "metal_" + m.prefix()));
            }
            List<Tool> plan = new ArrayList<>();
            for (int digits = stat.applyAsInt(STORE + "plan"); digits > 0; digits /= 4) {
                int t = digits % 4 - 1;
                if (t < 0) {
                    break;
                }
                plan.add(Tool.values()[t]);
            }
            boolean poured = stat.applyAsInt(STORE + "poured") == 1 && !plan.isEmpty();
            return new Stored(commission, moulds, metal, Math.max(0, Math.min(CRUCIBLE, stat.applyAsInt(STORE + "bars"))), poured,
                    stat.applyAsInt(STORE + "metal_score"), plan.toArray(new Tool[0]), stat.applyAsInt(STORE + "progress"),
                    stat.applyAsInt(STORE + "heat"), stat.applyAsInt(STORE + "quality"), stat.applyAsInt(STORE + "start_quality"),
                    stat.applyAsInt(STORE + "mistakes"), stat.applyAsInt(STORE + "bonuses_left"), stat.applyAsInt(STORE + "bonuses_total"));
        }
    }
}
