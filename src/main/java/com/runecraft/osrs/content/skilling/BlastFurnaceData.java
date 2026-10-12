package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.Station;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.Bar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Blast Furnace (Keldagrim). Ore put on the conveyor belt is smelted by the dwarves and the
 * bars wait in the bar dispenser. OSRS rules: only half the usual coal is needed (steel 1, mithril 2,
 * adamantite 3, runite 4), iron never fails, the furnace holds 28 of each ore and 254 coal, smelting
 * XP is the same as at a furnace, players below 60 Smithing pay the Foreman 2,500 coins per 10
 * minutes, and the coffer pays the workers 72,000 coins an hour. The furnace room is reached by the
 * stairs in Keldagrim and sits at its real OSRS coordinates (1940, 4958). Hot bars (ice gloves or a
 * bucket of water) and the stove/pump/belt chores of the dwarves are not modelled.
 */
public final class BlastFurnaceData extends SkillingPart {
    public static final int FURNACE_X = 1940;
    public static final int FURNACE_Y = 4958;
    /** Smithing level from which the Foreman lets you in for free. */
    public static final int FREE_LEVEL = 60;
    public static final int FOREMAN_FEE = 2_500;
    /** The Foreman's fee buys ten minutes (1,000 game ticks). */
    public static final int FOREMAN_TICKS = 1_000;
    /** The coffer pays 72,000 coins per hour: 12 coins per game tick. */
    public static final int COFFER_PER_HOUR = 72_000;
    public static final int COFFER_PER_TICK = COFFER_PER_HOUR / 6_000;
    public static final int MAX_ORE = 28;
    public static final int MAX_COAL = 254;
    /** Bars of each type the dispenser holds. */
    public static final int MAX_BARS = 28;

    /** One player's furnace: ore in the melting pot, bars in the dispenser, coffer and Foreman permit. */
    public static final class Furnace {
        public final Map<String, Integer> ore = new LinkedHashMap<>();
        public final Map<String, Integer> bars = new LinkedHashMap<>();
        public int coffer;
        /** Game tick until which the Foreman's permission lasts. */
        public long permitUntil;

        public int ore(String id) {
            return ore.getOrDefault(id, 0);
        }

        public int bars(String id) {
            return bars.getOrDefault(id, 0);
        }
    }

    /** Bars made by one smelting pass. */
    public record Smelted(Bar bar, int count) {
        public double xp() {
            return bar.xp() * count;
        }
    }

    /** True for ores the conveyor belt takes. */
    public static boolean accepts(String item) {
        for (Bar b : Bar.values()) {
            if (b.ores().containsKey(item)) {
                return true;
            }
        }
        return false;
    }

    public static int capacity(String ore) {
        return ore.equals("coal") ? MAX_COAL : MAX_ORE;
    }

    /** Coal per bar at the Blast Furnace: half the normal amount. */
    public static int coalPerBar(Bar bar) {
        return bar.ores().getOrDefault("coal", 0) / 2;
    }

    /** Puts up to {@code n} of an ore in the melting pot; returns how many went in. */
    public static int deposit(Furnace f, String ore, int n) {
        if (!accepts(ore) || n <= 0) {
            return 0;
        }
        int in = Math.max(0, Math.min(n, capacity(ore) - f.ore(ore)));
        if (in > 0) {
            f.ore.merge(ore, in, Integer::sum);
        }
        return in;
    }

    /** Whether the Foreman must be paid: below 60 Smithing without a running permit. */
    public static boolean needsFee(int smithing, Furnace f, long gameTick) {
        return smithing < FREE_LEVEL && gameTick >= f.permitUntil;
    }

    /**
     * Smelts everything that can be smelted for a Smithing level: runite, adamantite and mithril take
     * their (halved) coal first, then iron becomes steel while coal lasts and plain iron after that
     * (always successfully), gold and silver need no coal, and copper pairs with tin. Ore that cannot
     * be smelted yet stays in the pot; the dispenser never holds more than 28 of a bar.
     */
    public static List<Smelted> smelt(Furnace f, int smithing) {
        List<Smelted> out = new ArrayList<>();
        Bar[] order = {Bar.RUNITE, Bar.ADAMANTITE, Bar.MITHRIL, Bar.STEEL, Bar.IRON, Bar.GOLD, Bar.SILVER, Bar.BRONZE};
        for (Bar bar : order) {
            if (smithing < bar.level()) {
                continue;
            }
            int room = MAX_BARS - f.bars(bar.itemId());
            int n = room;
            int coal = coalPerBar(bar);
            for (Map.Entry<String, Integer> in : bar.ores().entrySet()) {
                int per = in.getKey().equals("coal") ? coal : in.getValue();
                if (per > 0) {
                    n = Math.min(n, f.ore(in.getKey()) / per);
                }
            }
            if (n <= 0) {
                continue;
            }
            for (Map.Entry<String, Integer> in : bar.ores().entrySet()) {
                int per = in.getKey().equals("coal") ? coal : in.getValue();
                if (per > 0) {
                    f.ore.merge(in.getKey(), -n * per, Integer::sum);
                    if (f.ore(in.getKey()) <= 0) {
                        f.ore.remove(in.getKey());
                    }
                }
            }
            f.bars.merge(bar.itemId(), n, Integer::sum);
            out.add(new Smelted(bar, n));
        }
        return out;
    }

    /** Takes up to {@code n} bars of a type from the dispenser; returns how many were taken. */
    public static int collect(Furnace f, String bar, int n) {
        int got = Math.min(n, f.bars(bar));
        if (got > 0) {
            f.bars.merge(bar, -got, Integer::sum);
            if (f.bars(bar) <= 0) {
                f.bars.remove(bar);
            }
        }
        return got;
    }

    /** Pays the workers for {@code ticks} game ticks; false when the coffer ran dry. */
    public static boolean drain(Furnace f, int ticks) {
        int cost = ticks * COFFER_PER_TICK;
        if (f.coffer < cost) {
            f.coffer = 0;
            return false;
        }
        f.coffer -= cost;
        return true;
    }

    @Override
    public List<BlockDef> blocks() {
        return List.of(
                BlockDef.scripted("blast_furnace_conveyor", "Conveyor belt", 0x5A4A3A, "machine"),
                BlockDef.scripted("blast_furnace_dispenser", "Bar dispenser", 0x8A6A3A, "machine"),
                BlockDef.scripted("blast_furnace_coffer", "Coffer", 0xA08040, "chest"),
                BlockDef.scripted("blast_furnace_stairs", "Blast Furnace stairs", 0x6A6A70, "portal_glow"),
                BlockDef.scripted("blast_furnace_exit", "Stairs", 0x6A6A70, "portal_glow"));
    }

    @Override
    public List<PlaceDef> places() {
        return List.of(new PlaceDef("blast_furnace", "Blast Furnace", FURNACE_X, FURNACE_Y, PlaceDef.Style.CAVE,
                List.of(Station.BANK_BOOTH),
                List.of("blast_furnace_conveyor", "blast_furnace_dispenser", "blast_furnace_coffer", "blast_furnace_exit")));
    }
}
