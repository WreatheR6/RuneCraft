package com.runecraft.osrs.content.dungeons;

import com.runecraft.osrs.data.Boss;
import com.runecraft.osrs.data.Drop;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * One player's current Barrows run (OSRS rules): which brothers he has killed and summoned, which
 * brother hides the tunnel this run, the tunnel layout (which doors are locked, where the player
 * comes in) and whether the chest has already summoned the hidden brother. Looting the chest ends
 * the run and a fresh one begins.
 *
 * <p>Brother indices follow {@link Barrows#BROTHERS}. The tunnels are a 3x3 grid of rooms (row-major
 * 0..8, the chest in room 4) joined by {@link #EDGES} corridors: edges 0..5 run east-west
 * (row * 2 + column), edges 6..11 north-south (6 + row * 3 + column). Each run opens a random
 * spanning tree of the corridors plus one extra, so every room can be reached but the route changes.
 */
public final class BarrowsRun {
    public static final int EDGES = 12;
    private static final int ALL = (1 << 6) - 1;

    private int killed;
    private int summoned;
    private int tunnel;
    private long seed;
    private boolean hiddenSummoned;

    private BarrowsRun(int killed, int summoned, int tunnel, long seed, boolean hiddenSummoned) {
        this.killed = killed & ALL;
        this.summoned = summoned & ALL;
        this.tunnel = Math.floorMod(tunnel, 6);
        this.seed = seed;
        this.hiddenSummoned = hiddenSummoned;
    }

    /** A new run with a random tunnel brother and tunnel layout. */
    public static BarrowsRun fresh(Random random) {
        return new BarrowsRun(0, 0, random.nextInt(6), random.nextLong(), false);
    }

    /** Restores a saved run (see the getters for the fields). */
    public static BarrowsRun of(int killed, int summoned, int tunnel, long seed, boolean hiddenSummoned) {
        return new BarrowsRun(killed, summoned, tunnel, seed, hiddenSummoned);
    }

    /** Ends the run (after the chest is looted): everything is reset and a new tunnel brother picked. */
    public void reset(Random random) {
        killed = 0;
        summoned = 0;
        tunnel = random.nextInt(6);
        seed = random.nextLong();
        hiddenSummoned = false;
    }

    public int killedMask() {
        return killed;
    }

    public int summonedMask() {
        return summoned;
    }

    public int tunnel() {
        return tunnel;
    }

    public Boss tunnelBrother() {
        return Barrows.BROTHERS.get(tunnel);
    }

    public long seed() {
        return seed;
    }

    public boolean hiddenSummoned() {
        return hiddenSummoned;
    }

    public boolean killed(int brother) {
        return (killed & (1 << brother)) != 0;
    }

    public boolean summoned(int brother) {
        return (summoned & (1 << brother)) != 0;
    }

    public boolean isTunnel(int brother) {
        return brother == tunnel;
    }

    public int killedCount() {
        return Integer.bitCount(killed);
    }

    /** What searching brother {@code b}'s sarcophagus does. */
    public enum Search { EMPTY, TUNNEL, SUMMON }

    public Search search(int brother) {
        if (killed(brother)) {
            return Search.EMPTY;
        }
        return isTunnel(brother) ? Search.TUNNEL : Search.SUMMON;
    }

    /** The brother was summoned for this run (from his sarcophagus or the chest). */
    public void summon(int brother) {
        summoned |= 1 << brother;
        if (isTunnel(brother)) {
            hiddenSummoned = true;
        }
    }

    /** A brother died: counts toward the run only if he was summoned in it. Returns whether it counted. */
    public boolean kill(int brother) {
        if (brother < 0 || brother >= 6 || !summoned(brother) || killed(brother)) {
            return false;
        }
        killed |= 1 << brother;
        return true;
    }

    /** What opening the chest does: summon the hidden brother first (once), then give the loot. */
    public enum Open { SUMMON_HIDDEN, LOOT }

    public Open open() {
        return !killed(tunnel) && !hiddenSummoned ? Open.SUMMON_HIDDEN : Open.LOOT;
    }

    // ------------------------------------------------------------------ tunnel layout

    /** The two rooms {a, b} joined by edge {@code e} (a is north/west of b). */
    public static int[] edgeRooms(int e) {
        if (e < 6) {
            int r = e / 2;
            int c = e % 2;
            return new int[]{r * 3 + c, r * 3 + c + 1};
        }
        int r = (e - 6) / 3;
        int c = (e - 6) % 3;
        return new int[]{r * 3 + c, (r + 1) * 3 + c};
    }

    /** Which corridors' doors open this run (index = edge). */
    public boolean[] openDoors() {
        Random random = new Random(seed);
        boolean[] open = new boolean[EDGES];
        int[] parent = new int[9];
        for (int i = 0; i < 9; i++) {
            parent[i] = i;
        }
        List<Integer> order = new ArrayList<>();
        for (int e = 0; e < EDGES; e++) {
            order.add(e);
        }
        java.util.Collections.shuffle(order, random);
        List<Integer> spare = new ArrayList<>();
        for (int e : order) {
            int[] r = edgeRooms(e);
            int a = find(parent, r[0]);
            int b = find(parent, r[1]);
            if (a != b) {
                parent[a] = b;
                open[e] = true;
            } else {
                spare.add(e);
            }
        }
        // One extra open door so the route sometimes has a choice.
        open[spare.get(random.nextInt(spare.size()))] = true;
        return open;
    }

    private static int find(int[] parent, int i) {
        while (parent[i] != i) {
            i = parent[i];
        }
        return i;
    }

    public boolean doorOpen(int edge) {
        return edge >= 0 && edge < EDGES && openDoors()[edge];
    }

    /** The room the tunnel entrance leads into this run: one of the four corner rooms. */
    public int startRoom() {
        int[] corners = {0, 2, 6, 8};
        return corners[(int) Math.floorMod(seed >>> 7, 4L)];
    }

    // ------------------------------------------------------------------ rewards

    /** One stack from the chest. */
    public record Reward(String item, int count) {
    }

    /** OSRS: each roll is a Barrows item with chance 1 / (450 - 58 x brothers killed). */
    public static int barrowsItemChance(int killed) {
        return 450 - 58 * Math.max(0, Math.min(6, killed));
    }

    /** OSRS: one reward roll per brother killed, plus one. */
    public static int rolls(int killed) {
        return Math.max(0, Math.min(6, killed)) + 1;
    }

    static boolean isPiece(Drop d) {
        return !d.item().equals("coins") && !d.item().endsWith("_rune") && !d.item().startsWith("#");
    }

    /**
     * Rolls the chest using the brothers' own drop tables ({@link Boss#drops()}): every roll is either
     * one of the killed brothers' equipment pieces (OSRS chance) or one of the common lines (coins and
     * runes) of the brothers' tables. Nothing can be won from a brother who wasn't killed.
     */
    public List<Reward> rollRewards(Random random) {
        int n = killedCount();
        List<String> pieces = new ArrayList<>();
        List<Drop> common = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            for (Drop d : Barrows.BROTHERS.get(i).drops()) {
                if (isPiece(d)) {
                    if (killed(i)) {
                        pieces.add(d.item());
                    }
                } else if (!common.contains(d)) {
                    common.add(d);
                }
            }
        }
        Map<String, Integer> out = new LinkedHashMap<>();
        int chance = barrowsItemChance(n);
        for (int roll = 0; roll < rolls(n); roll++) {
            if (!pieces.isEmpty() && random.nextInt(chance) == 0) {
                String piece = pieces.get(random.nextInt(pieces.size()));
                if (!out.containsKey(piece)) {
                    out.put(piece, 1);
                    continue;
                }
            }
            Drop d = common.get(random.nextInt(common.size()));
            int count = d.min() + (d.max() > d.min() ? random.nextInt(d.max() - d.min() + 1) : 0);
            out.merge(d.item(), count, Integer::sum);
        }
        List<Reward> list = new ArrayList<>();
        out.forEach((item, count) -> list.add(new Reward(item, count)));
        return list;
    }
}
