package com.runecraft.osrs.content.dungeons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The OSRS Barrows door puzzles: "Which item comes next?" - three items of a sequence and three
 * possible answers. The doors in the ring around the chest room pose them (see
 * {@link BarrowsRun#puzzleDoor(int)}); the right answer opens the door for the rest of the run, a
 * wrong one keeps it shut and the door asks a different question.
 */
public final class BarrowsPuzzles {
    /** One sequence: the three items shown, the item that comes next and two wrong answers. */
    public record Puzzle(List<String> sequence, String answer, List<String> wrong) {
        public Puzzle {
            sequence = List.copyOf(sequence);
            wrong = List.copyOf(wrong);
        }

        /** The sequence as the door shows it: "Bronze bar, Iron bar, Steel bar, ?". */
        public String question() {
            return String.join(", ", sequence) + ", ?";
        }
    }

    /** A puzzle as asked: its options in the order shown, and which of them is right. */
    public record Asked(Puzzle puzzle, List<String> options, int correct) {
        public Asked {
            options = List.copyOf(options);
        }

        public boolean isCorrect(int choice) {
            return choice == correct;
        }
    }

    public static final String PROMPT = "Which item comes next?";

    private static Puzzle p(String a, String b, String c, String answer, String wrong1, String wrong2) {
        return new Puzzle(List.of(a, b, c), answer, List.of(wrong1, wrong2));
    }

    /** The bank, in the spirit of the OSRS shapes/numbers/weapons/arrows/bars/pickaxes puzzles. */
    public static final List<Puzzle> BANK = List.of(
            p("Bronze bar", "Iron bar", "Steel bar", "Mithril bar", "Gold bar", "Silver bar"),
            p("Iron pickaxe", "Steel pickaxe", "Mithril pickaxe", "Adamant pickaxe", "Bronze pickaxe", "Dragon pickaxe"),
            p("Bronze arrow", "Iron arrow", "Steel arrow", "Mithril arrow", "Bronze bolts", "Rune arrow"),
            p("Air rune", "Water rune", "Earth rune", "Fire rune", "Mind rune", "Body rune"),
            p("Triangle", "Square", "Pentagon", "Hexagon", "Circle", "Octagon"),
            p("1", "2", "4", "8", "6", "5"),
            p("1", "3", "5", "7", "6", "9"),
            p("Bronze dagger", "Bronze sword", "Bronze longsword", "Bronze 2h sword", "Bronze mace", "Bronze dart"),
            p("Shrimps", "Sardine", "Herring", "Anchovies", "Lobster", "Shark"),
            p("Coal", "Mithril ore", "Adamantite ore", "Runite ore", "Gold ore", "Iron ore"),
            p("Logs", "Oak logs", "Willow logs", "Maple logs", "Magic logs", "Yew logs"),
            p("Mithril scimitar", "Adamant scimitar", "Rune scimitar", "Dragon scimitar", "Bronze scimitar", "Steel scimitar"),
            p("Bronze full helm", "Bronze platebody", "Bronze platelegs", "Bronze boots", "Bronze kiteshield", "Bronze med helm"));

    private BarrowsPuzzles() {
    }

    /** Asks a random puzzle (never {@code previous} again, so a wrong answer always gets a new question). */
    public static Asked roll(Random random, Puzzle previous) {
        Puzzle puzzle;
        do {
            puzzle = BANK.get(random.nextInt(BANK.size()));
        } while (puzzle.equals(previous) && BANK.size() > 1);
        List<String> options = new ArrayList<>(puzzle.wrong());
        options.add(puzzle.answer());
        Collections.shuffle(options, random);
        return new Asked(puzzle, options, options.indexOf(puzzle.answer()));
    }
}
