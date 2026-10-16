package com.runecraft.osrs.content.quests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * An in-world quest puzzle (combination locks, river crossings, memory and riddle rooms, deductions), answered by
 * clicking options in chat ({@code /questpuzzle}). A {@link QuestSpec} puzzle trigger shows one {@linkplain Room room}
 * of a puzzle; rooms have one or more {@linkplain Variant variants} (Lady Table's missing statue, Ms Hynn Terprett's
 * riddles, Murder Mystery's murderer).
 *
 * <p>Each player gets a deterministic draw from {@link #seed}: which {@link #picks} rooms appear in which slots (rooms
 * listed in {@link #always} are always among them, in a random slot) and which variant of each. Puzzles that share a
 * {@link #key} share the draw, so two Murder Mystery puzzles agree on the murderer. A wrong answer re-draws (the attempt
 * counter is part of the seed) when the puzzle has a {@link #failStage}.
 *
 * <p>An answer is a sequence of option indices. Every option clicked must keep the input a prefix of one of the
 * variant's {@linkplain Variant#solutions solutions}: the first wrong click fails, and matching a whole solution solves
 * the puzzle (a variant with {@link Variant#spawn} then raises a foe whose {@link Variant#kill death} solves it).
 */
public final class QuestPuzzle {
    /** One version of a room: who presents it, what it says, its options and every accepted answer. */
    public record Variant(String npc, List<String> prompt, List<String> options, List<List<Integer>> solutions,
                          String spawn, String kill) {
        /** Clicks a solution takes. */
        public int length() {
            return solutions.get(0).size();
        }

        /** 1 when {@code input} is a whole solution, 0 while it is a prefix of one, -1 once it is wrong. */
        public int check(List<Integer> input) {
            boolean prefix = false;
            for (List<Integer> s : solutions) {
                if (input.size() <= s.size() && s.subList(0, input.size()).equals(input)) {
                    if (input.size() == s.size()) {
                        return 1;
                    }
                    prefix = true;
                }
            }
            return prefix ? 0 : -1;
        }
    }

    /** A room (a test, a lock) and its variants. */
    public record Room(String id, List<Variant> variants) {
    }

    /** Draw key: puzzles sharing it share the per-player draw and attempt counter. */
    public final String key;
    public final List<Room> rooms;
    /** Rooms each player gets (the number of slots). */
    public final int picks;
    /** Rooms always drawn. */
    public final List<String> always;
    /** Stage a wrong answer sends the player back to (re-drawing the rooms), or -1 to just let them retry. */
    public final int failStage;
    /** Quest flags cleared by a wrong answer (progress through the slots). */
    public final List<String> failFlags;
    /** Lines spoken on a wrong answer. */
    public final List<String> failLines;
    /** Whether the options are listed in a per-player order (so the listing doesn't give the answer away). */
    public final boolean shuffle;

    private QuestPuzzle(Builder b) {
        this.key = b.key;
        this.rooms = List.copyOf(b.rooms);
        this.picks = b.picks <= 0 ? b.rooms.size() : b.picks;
        this.always = List.copyOf(b.always);
        this.failStage = b.failStage;
        this.failFlags = List.copyOf(b.failFlags);
        this.failLines = List.copyOf(b.failLines);
        this.shuffle = b.shuffle;
    }

    // ------------------------------------------------------------------ the per-player draw

    /** The seed of a player's draw: their id, the puzzle key and how many times they have failed it. */
    public static long seed(String player, String key, int attempt) {
        long h = 1125899906842597L;
        String s = player + "|" + key + "|" + attempt;
        for (int i = 0; i < s.length(); i++) {
            h = 31 * h + s.charAt(i);
        }
        return mix(h);
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    private int roomIndex(String id) {
        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /** Room index in each slot for a seed: the {@link #always} rooms plus random others, in a random order. */
    public List<Integer> draw(long seed) {
        List<Integer> pool = new ArrayList<>();
        List<Integer> chosen = new ArrayList<>();
        for (int i = 0; i < rooms.size(); i++) {
            if (always.contains(rooms.get(i).id())) {
                chosen.add(i);
            } else {
                pool.add(i);
            }
        }
        Random r = new Random(seed);
        Collections.shuffle(pool, r);
        chosen.addAll(pool.subList(0, Math.max(0, picks - chosen.size())));
        Collections.shuffle(chosen, r);
        return chosen;
    }

    /** The room a seed puts in a slot. */
    public Room room(long seed, int slot) {
        return rooms.get(draw(seed).get(slot));
    }

    /** The variant a seed puts in a slot. */
    public Variant variant(long seed, int slot) {
        int room = draw(seed).get(slot);
        List<Variant> vs = rooms.get(room).variants();
        return vs.get((int) Math.floorMod(mix(seed + 31L * (room + 1)), (long) vs.size()));
    }

    /** Order to list a variant's options in (identity unless {@link #shuffle}). */
    public List<Integer> displayOrder(long seed, Variant v) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < v.options().size(); i++) {
            order.add(i);
        }
        if (shuffle) {
            Collections.shuffle(order, new Random(mix(seed ^ 0x5DEECE66DL)));
        }
        return order;
    }

    // ------------------------------------------------------------------ helpers for definitions

    /** The 26 letters, as options for combination-lock dials. */
    public static String[] letters() {
        String[] l = new String[26];
        for (int i = 0; i < 26; i++) {
            l[i] = String.valueOf((char) ('A' + i));
        }
        return l;
    }

    /** A word as letter option indices ({@link #letters}). */
    public static int[] word(String w) {
        int[] s = new int[w.length()];
        for (int i = 0; i < w.length(); i++) {
            s[i] = Character.toUpperCase(w.charAt(i)) - 'A';
        }
        return s;
    }

    public static Builder builder(String key) {
        return new Builder(key);
    }

    /** Fluent definition: {@code room(id)} then {@code variant(npc).prompt(..).options(..).solution(..)}. */
    public static final class Builder {
        private final String key;
        private final List<Room> rooms = new ArrayList<>();
        private final List<String> always = new ArrayList<>();
        private final List<String> failFlags = new ArrayList<>();
        private final List<String> failLines = new ArrayList<>();
        private int picks;
        private int failStage = -1;
        private boolean shuffle;
        private String roomId;
        private List<Variant> variants;
        private String npc;
        private List<String> prompt;
        private List<String> options;
        private List<List<Integer>> solutions;
        private String spawn;
        private String kill;

        private Builder(String key) {
            this.key = key;
        }

        public Builder picks(int n) {
            this.picks = n;
            return this;
        }

        public Builder always(String... roomIds) {
            always.addAll(List.of(roomIds));
            return this;
        }

        /** A wrong answer sends the player to {@code stage}, clears {@code flags} and re-draws. */
        public Builder failTo(int stage, String... flags) {
            this.failStage = stage;
            failFlags.addAll(List.of(flags));
            return this;
        }

        public Builder failLines(String... lines) {
            failLines.addAll(List.of(lines));
            return this;
        }

        public Builder shuffle() {
            this.shuffle = true;
            return this;
        }

        public Builder room(String id) {
            endRoom();
            roomId = id;
            variants = new ArrayList<>();
            return this;
        }

        /** A variant of the current room, presented by {@code npc} (null: the trigger's target). */
        public Builder variant(String npc) {
            if (roomId == null) {
                room(key);
            }
            endVariant();
            this.npc = npc;
            this.prompt = new ArrayList<>();
            this.options = new ArrayList<>();
            this.solutions = new ArrayList<>();
            this.spawn = null;
            this.kill = null;
            return this;
        }

        public Builder prompt(String... lines) {
            prompt.addAll(List.of(lines));
            return this;
        }

        public Builder options(String... opts) {
            options.addAll(List.of(opts));
            return this;
        }

        /** An accepted answer (option indices in order); call again for alternatives. */
        public Builder solution(int... seq) {
            List<Integer> s = new ArrayList<>();
            for (int i : seq) {
                s.add(i);
            }
            solutions.add(List.copyOf(s));
            return this;
        }

        /** Every ordering of these options is accepted (pick the three spirit animals, in any order). */
        public Builder anyOrder(int... picks) {
            permute(new ArrayList<>(), picks, new boolean[picks.length]);
            return this;
        }

        private void permute(List<Integer> acc, int[] picks, boolean[] used) {
            if (acc.size() == picks.length) {
                solutions.add(List.copyOf(acc));
                return;
            }
            for (int i = 0; i < picks.length; i++) {
                if (!used[i]) {
                    used[i] = true;
                    acc.add(picks[i]);
                    permute(acc, picks, used);
                    acc.remove(acc.size() - 1);
                    used[i] = false;
                }
            }
        }

        /** Solving raises {@code bossId}; killing {@code creature} then completes the room. */
        public Builder spawnKill(String bossId, String creature) {
            this.spawn = bossId;
            this.kill = creature;
            return this;
        }

        private void endVariant() {
            if (prompt == null) {
                return;
            }
            if (options.isEmpty() || solutions.isEmpty()) {
                throw new IllegalStateException(key + "/" + roomId + ": a variant needs options and a solution");
            }
            int len = solutions.get(0).size();
            for (List<Integer> s : solutions) {
                if (s.isEmpty() || s.size() != len) {
                    throw new IllegalStateException(key + "/" + roomId + ": solutions must be non-empty and equally long");
                }
                for (int i : s) {
                    if (i < 0 || i >= options.size()) {
                        throw new IllegalStateException(key + "/" + roomId + ": solution index " + i + " out of range");
                    }
                }
            }
            variants.add(new Variant(npc, List.copyOf(prompt), List.copyOf(options), List.copyOf(solutions), spawn, kill));
            prompt = null;
        }

        private void endRoom() {
            endVariant();
            if (roomId != null) {
                if (variants.isEmpty()) {
                    throw new IllegalStateException(key + "/" + roomId + ": a room needs a variant");
                }
                rooms.add(new Room(roomId, List.copyOf(variants)));
                roomId = null;
            }
        }

        public QuestPuzzle build() {
            endRoom();
            QuestPuzzle p = new QuestPuzzle(this);
            if (p.rooms.isEmpty() || p.picks > p.rooms.size() || p.always.size() > p.picks) {
                throw new IllegalStateException(key + ": bad room count");
            }
            for (String a : p.always) {
                if (p.roomIndex(a) < 0) {
                    throw new IllegalStateException(key + ": unknown always-room " + a);
                }
            }
            return p;
        }
    }
}
