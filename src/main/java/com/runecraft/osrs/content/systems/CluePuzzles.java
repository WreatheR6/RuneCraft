package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.content.systems.ClueScrolls.Tier;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Treasure trail puzzles: puzzle boxes (sliding tile puzzles), light boxes (eight buttons, each toggling a
 * fixed set of the 25 lights; light them all) and Falo the Bard's songs on master clues.
 */
public final class CluePuzzles {
    public static final String PUZZLE_BOX = "puzzle_box";
    public static final String LIGHT_BOX = "light_box";
    /** Falo the Bard's NPC id (systems module). */
    public static final String FALO = "falo_the_bard";

    private CluePuzzles() {
    }

    /** Side length of a tier's puzzle box: OSRS boxes are 5x5; medium clues use a 3x3 board (approx simplification). */
    public static int puzzleSize(Tier tier) {
        return tier == Tier.MEDIUM ? 3 : 5;
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

    // ------------------------------------------------------------------ Falo the Bard

    /** A Falo the Bard step: the verse he sings and the item it names (all exist in the core catalog). */
    public record Song(String item, String verse) {
    }

    /** Falo's songs (paraphrased) for master clues. */
    public static final List<Song> SONGS = List.of(
            new Song("abyssal_whip", "A lash of the deep, from a demon it fell; bring me the whip that the abyss knows well."),
            new Song("dragon_scimitar", "A curved blade of red, from a monkey king's land; bring the dragon's own scimitar to my hand."),
            new Song("dragon_defender", "Earned from the cyclops, a guard for the arm; the dragon defender keeps fighters from harm."),
            new Song("amulet_of_glory", "Dipped in the fountain where heroes have been; an amulet of glory is what I would see."),
            new Song("dragon_longsword", "Long is the blade and as red as the flame; a dragon longsword will add to your fame."),
            new Song("rune_platebody", "Blue as the sky and as strong as a wall; a rune platebody, the heaviest of all."),
            new Song("dragon_med_helm", "A helm made of dragon, the medium kind; bring it to Falo, and peace to my mind."),
            new Song("granite_maul", "A hammer of stone from the gargoyle's lair; a granite maul, carried with care."));
}
