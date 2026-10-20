package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.content.systems.ClueScrolls.Tier;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Treasure trail puzzles: puzzle boxes (5x5 sliding tile puzzles handed over by hard, elite and master clue NPCs) and
 * light boxes (master clues: eight buttons, each toggling a fixed set of the 25 lights; light them all).
 */
public final class CluePuzzles {
    public static final String PUZZLE_BOX = "puzzle_box";
    public static final String LIGHT_BOX = "light_box";
    /** Falo the Bard's NPC id (systems module). */
    public static final String FALO = "falo_the_bard";

    private CluePuzzles() {
    }

    /** Side length of a tier's puzzle box: every OSRS puzzle box (hard, elite and master clues) is a 5x5 board. */
    public static int puzzleSize(Tier tier) {
        return 5;
    }

    // ------------------------------------------------------------------ sliding puzzle

    /** A sliding tile puzzle: tiles 1..n*n-1 and a gap (0); solved when they read in order with the gap last. */
    public static final class Slider {
        private final int n;
        private final int[] tiles;

        public Slider(int n, int[] tiles) {
            if (tiles.length != n * n) {
                throw new IllegalArgumentException("need " + n * n + " tiles");
            }
            this.n = n;
            this.tiles = tiles.clone();
        }

        public static Slider solvedBoard(int n) {
            int[] t = new int[n * n];
            for (int i = 0; i < t.length - 1; i++) {
                t[i] = i + 1;
            }
            return new Slider(n, t);
        }

        /** Saved form: {@code "<n>:t,t,..."}. */
        public String encode() {
            StringBuilder b = new StringBuilder().append(n).append(':');
            for (int i = 0; i < tiles.length; i++) {
                b.append(i == 0 ? "" : ",").append(tiles[i]);
            }
            return b.toString();
        }

        /** Reads {@link #encode()}; null unless it holds a valid, solvable board. */
        public static Slider decode(String s) {
            try {
                int colon = s.indexOf(':');
                int n = Integer.parseInt(s.substring(0, colon));
                String[] parts = s.substring(colon + 1).split(",");
                if (n < 2 || n > 8 || parts.length != n * n) {
                    return null;
                }
                int[] t = new int[n * n];
                boolean[] seen = new boolean[n * n];
                for (int i = 0; i < t.length; i++) {
                    t[i] = Integer.parseInt(parts[i]);
                    if (t[i] < 0 || t[i] >= t.length || seen[t[i]]) {
                        return null;
                    }
                    seen[t[i]] = true;
                }
                Slider out = new Slider(n, t);
                return out.solvable() ? out : null;
            } catch (RuntimeException e) {
                return null;
            }
        }

        /** A scrambled board, made by sliding tiles at random from the solved board (so always solvable). */
        public static Slider scrambled(int n, Random random) {
            Slider s = solvedBoard(n);
            int last = -1;
            for (int i = 0; i < n * n * 25 || s.solved(); i++) {
                int gap = s.gap();
                int[] options = s.neighbours(gap);
                int pick;
                do {
                    pick = options[random.nextInt(options.length)];
                } while (pick == last && options.length > 1);
                last = gap;
                s.move(pick);
            }
            return s;
        }

        public int size() {
            return n;
        }

        public int tile(int index) {
            return tiles[index];
        }

        public int[] tiles() {
            return tiles.clone();
        }

        public int gap() {
            for (int i = 0; i < tiles.length; i++) {
                if (tiles[i] == 0) {
                    return i;
                }
            }
            throw new IllegalStateException("no gap");
        }

        int[] neighbours(int index) {
            int r = index / n;
            int c = index % n;
            int[] buf = new int[4];
            int k = 0;
            if (r > 0) {
                buf[k++] = index - n;
            }
            if (r < n - 1) {
                buf[k++] = index + n;
            }
            if (c > 0) {
                buf[k++] = index - 1;
            }
            if (c < n - 1) {
                buf[k++] = index + 1;
            }
            return Arrays.copyOf(buf, k);
        }

        /** Slides the tile at {@code index} into the gap if they are side by side. Returns whether it moved. */
        public boolean move(int index) {
            if (index < 0 || index >= tiles.length || tiles[index] == 0) {
                return false;
            }
            int gap = gap();
            for (int nb : neighbours(index)) {
                if (nb == gap) {
                    tiles[gap] = tiles[index];
                    tiles[index] = 0;
                    return true;
                }
            }
            return false;
        }

        public boolean solved() {
            for (int i = 0; i < tiles.length - 1; i++) {
                if (tiles[i] != i + 1) {
                    return false;
                }
            }
            return tiles[tiles.length - 1] == 0;
        }

        /** Whether the board can be solved at all (inversion parity rule). */
        public boolean solvable() {
            int inversions = 0;
            for (int i = 0; i < tiles.length; i++) {
                for (int j = i + 1; j < tiles.length; j++) {
                    if (tiles[i] != 0 && tiles[j] != 0 && tiles[i] > tiles[j]) {
                        inversions++;
                    }
                }
            }
            if (n % 2 == 1) {
                return inversions % 2 == 0;
            }
            int gapRowFromBottom = n - gap() / n;
            return (inversions + gapRowFromBottom) % 2 == 1;
        }
    }

    // ------------------------------------------------------------------ light box

    /** Number of lights (5x5) and buttons (A-H) on a light box. */
    public static final int LIGHTS = 25;
    public static final int BUTTONS = 8;

    /**
     * An OSRS light box: each of the eight buttons toggles its own fixed set of lights; the box opens when every
     * light is on. Boxes start from all-lit with a random set of buttons pressed, so pressing that set again
     * solves them.
     */
    public static final class LightBox {
        private final boolean[] lights;
        private final int[][] buttons;

        public LightBox(boolean[] lights, int[][] buttons) {
            this.lights = lights.clone();
            this.buttons = buttons;
        }

        public static LightBox scrambled(Random random) {
            int[][] buttons = new int[BUTTONS][];
            for (int b = 0; b < BUTTONS; b++) {
                int count = 5 + random.nextInt(4);
                int[] pick = new int[count];
                boolean[] used = new boolean[LIGHTS];
                for (int i = 0; i < count; i++) {
                    int l;
                    do {
                        l = random.nextInt(LIGHTS);
                    } while (used[l]);
                    used[l] = true;
                    pick[i] = l;
                }
                buttons[b] = pick;
            }
            boolean[] all = new boolean[LIGHTS];
            Arrays.fill(all, true);
            LightBox box = new LightBox(all, buttons);
            while (box.solved()) {
                for (int b = 0; b < BUTTONS; b++) {
                    if (random.nextBoolean()) {
                        box.press(b);
                    }
                }
            }
            return box;
        }

        /** Saved form: 25 lights as 1/0, then each button's lights: {@code "1101...:0,4,7;2,3,..."}. */
        public String encode() {
            StringBuilder b = new StringBuilder();
            for (boolean l : lights) {
                b.append(l ? '1' : '0');
            }
            b.append(':');
            for (int i = 0; i < buttons.length; i++) {
                b.append(i == 0 ? "" : ";");
                for (int j = 0; j < buttons[i].length; j++) {
                    b.append(j == 0 ? "" : ",").append(buttons[i][j]);
                }
            }
            return b.toString();
        }

        /** Reads {@link #encode()}; null if malformed. */
        public static LightBox decode(String s) {
            try {
                String[] halves = s.split(":", 2);
                if (halves.length != 2 || halves[0].length() != LIGHTS) {
                    return null;
                }
                boolean[] l = new boolean[LIGHTS];
                for (int i = 0; i < LIGHTS; i++) {
                    l[i] = halves[0].charAt(i) == '1';
                }
                String[] bs = halves[1].split(";");
                if (bs.length != BUTTONS) {
                    return null;
                }
                int[][] buttons = new int[BUTTONS][];
                for (int b = 0; b < BUTTONS; b++) {
                    String[] ls = bs[b].split(",");
                    buttons[b] = new int[ls.length];
                    for (int j = 0; j < ls.length; j++) {
                        buttons[b][j] = Integer.parseInt(ls[j]);
                        if (buttons[b][j] < 0 || buttons[b][j] >= LIGHTS) {
                            return null;
                        }
                    }
                }
                return new LightBox(l, buttons);
            } catch (RuntimeException e) {
                return null;
            }
        }

        public void press(int button) {
            if (button < 0 || button >= buttons.length) {
                return;
            }
            for (int l : buttons[button]) {
                lights[l] = !lights[l];
            }
        }

        public boolean lit(int light) {
            return lights[light];
        }

        public int litCount() {
            int n = 0;
            for (boolean b : lights) {
                if (b) {
                    n++;
                }
            }
            return n;
        }

        public boolean solved() {
            return litCount() == LIGHTS;
        }
    }
}
