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
 * About half of the doors into the chest room pose a puzzle ({@link BarrowsPuzzles}) that must be
 * solved once per run, each door opened may wake a brother still alive, a skeleton, a bloodworm or a crypt rat
 * ({@link #doorEncounter}), and killing tunnel monsters adds their combat levels to the reward potential.
 */
public final class BarrowsRun {
    public static final int EDGES = 12;
    private static final int ALL = (1 << 6) - 1;

    private int killed;
    private int summoned;
    private int tunnel;
    private long seed;
    private boolean hiddenSummoned;
    private int solved;
    private int monsterKills;
    private int monsterLevels;

    private BarrowsRun(int killed, int summoned, int tunnel, long seed, boolean hiddenSummoned, int solved, int monsterKills) {
        this.killed = killed & ALL;
        this.summoned = summoned & ALL;
        this.tunnel = Math.floorMod(tunnel, 6);
        this.seed = seed;
        this.hiddenSummoned = hiddenSummoned;
        this.solved = solved & ((1 << EDGES) - 1);
        this.monsterKills = Math.max(0, monsterKills);
    }

    /** A new run with a random tunnel brother and tunnel layout. */
    public static BarrowsRun fresh(Random random) {
        return new BarrowsRun(0, 0, random.nextInt(6), random.nextLong(), false, 0, 0);
    }

    /** Restores a saved run (see the getters for the fields). */
    public static BarrowsRun of(int killed, int summoned, int tunnel, long seed, boolean hiddenSummoned) {
        return of(killed, summoned, tunnel, seed, hiddenSummoned, 0, 0);
    }

    /** Restores a saved run including its solved puzzle doors and tunnel monster kills. */
    public static BarrowsRun of(int killed, int summoned, int tunnel, long seed, boolean hiddenSummoned, int solved, int monsterKills) {
        return new BarrowsRun(killed, summoned, tunnel, seed, hiddenSummoned, solved, monsterKills);
    }

    /** Restores a saved run including the combat levels of the tunnel monsters killed (reward potential). */
    public static BarrowsRun of(int killed, int summoned, int tunnel, long seed, boolean hiddenSummoned, int solved, int monsterKills,
                                int monsterLevels) {
        BarrowsRun run = new BarrowsRun(killed, summoned, tunnel, seed, hiddenSummoned, solved, monsterKills);
        run.monsterLevels = Math.max(0, Math.min(LEVEL_SUM_CAP, monsterLevels));
        return run;
    }

    /** Ends the run (after the chest is looted): everything is reset and a new tunnel brother picked. */
    public void reset(Random random) {
        killed = 0;
        summoned = 0;
        tunnel = random.nextInt(6);
        seed = random.nextLong();
        hiddenSummoned = false;
        solved = 0;
        monsterKills = 0;
        monsterLevels = 0;
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

    /** Bit mask (by edge) of the puzzle doors already solved this run. */
    public int solvedMask() {
        return solved;
    }

    /** Tunnel monsters killed this run. */
    public int monsterKills() {
        return monsterKills;
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

    /** True if edge {@code e} joins the chest room to one of the four rooms around it. */
    public static boolean chestDoor(int e) {
        int[] r = edgeRooms(e);
        return r[0] == Barrows.CHEST_ROOM || r[1] == Barrows.CHEST_ROOM;
    }

    /**
     * True if the door of edge {@code e} poses a puzzle this run: about half of the doors into the
     * chest room, fixed for the run (so walking away and back doesn't skip it).
     */
    public boolean puzzleDoor(int e) {
        if (e < 0 || e >= EDGES || !chestDoor(e)) {
            return false;
        }
        long z = seed + (e + 1) * 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 33)) * 0xFF51AFD7ED558CCDL;
        z = (z ^ (z >>> 33)) * 0xC4CEB9FE1A85EC53L;
        return ((z ^ (z >>> 33)) & 1L) == 0;
    }

    public boolean solved(int e) {
        return e >= 0 && e < EDGES && (solved & (1 << e)) != 0;
    }

    /** The puzzle on door {@code e} was answered correctly: it stays open for the rest of the run. */
    public void solve(int e) {
        if (e >= 0 && e < EDGES) {
            solved |= 1 << e;
        }
    }

    /** True if opening door {@code e} (an open one) needs its puzzle answered first. */
    public boolean needsPuzzle(int e) {
        return doorOpen(e) && puzzleDoor(e) && !solved(e);
    }

    /** The room the tunnel entrance leads into this run: one of the four corner rooms. */
    public int startRoom() {
        int[] corners = {0, 2, 6, 8};
        return corners[(int) Math.floorMod(seed >>> 7, 4L)];
    }

    // ------------------------------------------------------------------ tunnel brothers

    /**
     * What opening a tunnel door wakes (OSRS Wiki, Barrows/Strategies "Navigating the tunnels"): 12/128 one of the brothers
     * still alive this run, 52/128 a skeleton, 32/128 a bloodworm, 32/128 a crypt rat.
     */
    public enum DoorSpawn { BROTHER, SKELETON, BLOODWORM, CRYPT_RAT }

    /** Rolls out of 128 for each {@link DoorSpawn} (same order). */
    public static final int[] DOOR_SPAWN_WEIGHTS = {12, 52, 32, 32};
    /** A door opened into a room already holding this many enemies wakes nothing (OSRS Wiki). */
    public static final int ROOM_ENEMY_CAP = 11;
    /** Monster ids of the door spawns other than a brother (the dungeons module's Barrows skeleton is level 77). */
    public static final String SKELETON = "barrows_skeleton";
    public static final String BLOODWORM = "bloodworm";
    public static final String CRYPT_RAT = "crypt_rat";

    /** What a door wakes for a roll of 0..127. */
    public static DoorSpawn doorSpawn(int roll128) {
        int r = Math.floorMod(roll128, 128);
        for (int i = 0; i < DOOR_SPAWN_WEIGHTS.length; i++) {
            r -= DOOR_SPAWN_WEIGHTS[i];
            if (r < 0) {
                return DoorSpawn.values()[i];
            }
        }
        return DoorSpawn.CRYPT_RAT;
    }

    /**
     * The brothers who may still appear in the tunnels: every brother not killed this run, the hidden one too (if he comes out
     * of a door he no longer waits in the chest, see {@link #summon(int)}).
     */
    public List<Integer> tunnelCandidates() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            if (!killed(i)) {
                list.add(i);
            }
        }
        return list;
    }

    /**
     * Rolls what opening a tunnel door wakes: a brother's index (0..5) for {@link DoorSpawn#BROTHER}, else -1 and the caller
     * reads {@code spawn[0]}. A brother roll with every brother dead (or one already up, {@code brotherUp}) wakes nothing.
     * The caller summons the brother and calls {@link #summon(int)}, so his kill counts toward the run as usual.
     */
    public int doorEncounter(Random random, boolean brotherUp, DoorSpawn[] spawn) {
        DoorSpawn s = doorSpawn(random.nextInt(128));
        spawn[0] = s;
        if (s != DoorSpawn.BROTHER) {
            return -1;
        }
        List<Integer> candidates = tunnelCandidates();
        if (candidates.isEmpty() || brotherUp) {
            spawn[0] = null;
            return -1;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    // ------------------------------------------------------------------ prayer drain

    /** OSRS Wiki (Barrows/Strategies): the brothers' faces drain prayer every 18 seconds underground. */
    public static final int PRAYER_DRAIN_TICKS = 30;

    /** Prayer points each drain takes: 8, one more for each brother killed this run (14 with all six). */
    public static int prayerDrain(int killed) {
        return 8 + Math.max(0, Math.min(6, killed));
    }

    // ------------------------------------------------------------------ rewards

    /** OSRS Wiki: the combat levels counted toward reward potential are capped at this sum. */
    public static final int LEVEL_SUM_CAP = 1000;
    /** Full reward potential: 1000 + 2 x 6 brothers. */
    public static final int MAX_POTENTIAL = 1012;
    /** Elite clue scroll chance per roll (1/200; the first success skips the clue roll on later rolls). */
    public static final int CLUE_CHANCE = 200;

    /**
     * The main table (OSRS Wiki "Chest (Barrows)", reward mechanics): a roll of 1..potential lands in a band; the band's
     * item comes in a quantity that grows across the band from the wiki's minimum to its maximum (the wiki lists the range
     * at full potential; the even spread within a band is approx). Key halves and the dragon med helm are always 1.
     */
    public record Band(String item, int from, int to, int minQty, int maxQty) {
        public int quantity(int roll) {
            if (to <= from || maxQty <= minQty) {
                return minQty;
            }
            return minQty + (int) ((long) (roll - from) * (maxQty - minQty) / (to - from));
        }
    }

    public static final List<Band> BANDS = List.of(
            new Band("coins", 1, 380, 2, 774),
            new Band("mind_rune", 381, 505, 253, 336),
            new Band("chaos_rune", 506, 630, 112, 139),
            new Band("death_rune", 631, 755, 70, 83),
            new Band("blood_rune", 756, 880, 37, 43),
            new Band("bolt_rack", 881, 1005, 35, 40),
            new Band("loop_half_of_key", 1006, 1008, 1, 1),
            new Band("tooth_half_of_key", 1009, 1011, 1, 1),
            new Band("dragon_med_helm", 1012, 1012, 1, 1));

    /** A monster was killed in the tunnels this run (its combat level adds reward potential). */
    public void addMonsterKill(int combatLevel) {
        if (monsterKills < 1000) {
            monsterKills++;
        }
        monsterLevels = (int) Math.min(LEVEL_SUM_CAP, (long) monsterLevels + Math.max(0, combatLevel));
    }

    /** Combat levels of the tunnel monsters killed this run (capped at {@link #LEVEL_SUM_CAP}). */
    public int monsterLevels() {
        return monsterLevels;
    }

    /**
     * OSRS Wiki reward potential R = min(sum of combat levels killed, 1000) + 2 x brothers killed: the brothers count with
     * their combat levels (656 for all six) plus the tunnel monsters.
     */
    public int rewardPotential() {
        int sum = monsterLevels;
        for (int i = 0; i < 6; i++) {
            if (killed(i)) {
                sum += Barrows.BROTHERS.get(i).combatLevel();
            }
        }
        return Math.min(LEVEL_SUM_CAP, sum) + 2 * killedCount();
    }

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

    /** The main-table band a roll (1..potential) lands in, or null. */
    public static Band band(int roll) {
        for (Band b : BANDS) {
            if (roll >= b.from() && roll <= b.to()) {
                return b;
            }
        }
        return null;
    }

    /**
     * Rolls the chest (OSRS Wiki): one roll per brother killed plus one. Each roll first tries for a Barrows piece of a killed
     * brother (1 / (450 - 58N); a piece already won this chest is never picked again); otherwise it rolls 1..reward potential
     * on the main table ({@link #BANDS}). Each roll also has a 1/200 elite clue chance until one is won.
     */
    public List<Reward> rollRewards(Random random) {
        int n = killedCount();
        List<String> pieces = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            if (!killed(i)) {
                continue;
            }
            for (Drop d : Barrows.BROTHERS.get(i).drops()) {
                if (isPiece(d) && !pieces.contains(d.item())) {
                    pieces.add(d.item());
                }
            }
        }
        int potential = rewardPotential();
        Map<String, Integer> out = new LinkedHashMap<>();
        int chance = barrowsItemChance(n);
        boolean clue = false;
        for (int roll = 0; roll < rolls(n); roll++) {
            if (!clue && random.nextInt(CLUE_CHANCE) == 0) {
                clue = true;
                out.put("clue_scroll_elite", 1);
            }
            if (!pieces.isEmpty() && random.nextInt(chance) == 0) {
                out.put(pieces.remove(random.nextInt(pieces.size())), 1);
                continue;
            }
            if (potential < 1) {
                continue;
            }
            int r = 1 + random.nextInt(Math.min(potential, MAX_POTENTIAL));
            Band b = band(r);
            if (b != null) {
                out.merge(b.item(), b.quantity(r), Integer::sum);
            }
        }
        List<Reward> list = new ArrayList<>();
        out.forEach((item, count) -> list.add(new Reward(item, count)));
        return list;
    }
}
