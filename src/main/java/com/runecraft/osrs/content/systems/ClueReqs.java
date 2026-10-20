package com.runecraft.osrs.content.systems;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Item requirements of clue steps (emote clues, Falo the Bard, Charlie the Tramp, Sherlock), in {@link ClueStepData}'s
 * format: groups that are all needed, split by ";"; alternatives within a group split by "|"; items needed together
 * joined by "+"; "id*n" for a quantity; "!head,cape" for equipment slots that must be empty.
 */
public final class ClueReqs {
    /** One item (or empty-slot rule) of an alternative. */
    public record Token(String item, int qty, List<String> emptySlots) {
        public boolean isSlotRule() {
            return item == null;
        }
    }

    /** What a player has: worn (or carried, for Falo/Charlie/Sherlock) items and empty slots. */
    public interface Has {
        boolean has(String item, int qty);

        boolean slotEmpty(String slot);
    }

    private ClueReqs() {
    }

    /** groups -> alternatives -> tokens. */
    public static List<List<List<Token>>> groups(String req) {
        List<List<List<Token>>> out = new ArrayList<>();
        if (req == null || req.isBlank()) {
            return out;
        }
        for (String g : req.split(";")) {
            if (g.isBlank()) {
                continue;
            }
            List<List<Token>> alts = new ArrayList<>();
            for (String a : g.split("\\|")) {
                List<Token> tokens = new ArrayList<>();
                for (String t : a.split("\\+")) {
                    tokens.add(token(t.trim()));
                }
                alts.add(tokens);
            }
            out.add(alts);
        }
        return out;
    }

    static Token token(String t) {
        if (t.startsWith("!")) {
            return new Token(null, 0, List.of(t.substring(1).toLowerCase(Locale.ROOT).split(",")));
        }
        int star = t.indexOf('*');
        if (star > 0) {
            return new Token(t.substring(0, star), Integer.parseInt(t.substring(star + 1)), List.of());
        }
        return new Token(t, 1, List.of());
    }

    static boolean met(List<Token> alt, Has has) {
        for (Token t : alt) {
            if (t.isSlotRule()) {
                for (String slot : t.emptySlots()) {
                    if (!has.slotEmpty(slot)) {
                        return false;
                    }
                }
            } else if (!has.has(t.item(), t.qty())) {
                return false;
            }
        }
        return true;
    }

    /** The groups the player does not meet, each described by its first alternative. Empty when all are met. */
    public static List<String> missing(String req, Has has, Function<String, String> nameOf) {
        List<String> out = new ArrayList<>();
        for (List<List<Token>> g : groups(req)) {
            boolean ok = false;
            for (List<Token> alt : g) {
                if (met(alt, has)) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                out.add(describe(g, nameOf));
            }
        }
        return out;
    }

    /** "a steel platebody", "nothing in the head slot", "a team cape (or 3 others)". */
    public static String describe(List<List<Token>> group, Function<String, String> nameOf) {
        List<Token> first = group.get(0);
        StringBuilder b = new StringBuilder();
        for (Token t : first) {
            if (b.length() > 0) {
                b.append(" and ");
            }
            if (t.isSlotRule()) {
                b.append("nothing in the ").append(String.join("/", t.emptySlots())).append(" slot");
            } else {
                b.append(t.qty() > 1 ? t.qty() + " x " : "").append(nameOf.apply(t.item()));
            }
        }
        if (group.size() > 1) {
            b.append(" (or ").append(group.size() - 1).append(" other").append(group.size() > 2 ? "s" : "").append(")");
        }
        return b.toString();
    }

    /** A full description of every group (for the clue scroll screen). */
    public static List<String> describeAll(String req, Function<String, String> nameOf) {
        List<String> out = new ArrayList<>();
        for (List<List<Token>> g : groups(req)) {
            out.add(describe(g, nameOf));
        }
        return out;
    }

    /** The items of the first alternative of every group (what the self-test hands a player). */
    public static List<Token> firstChoice(String req) {
        List<Token> out = new ArrayList<>();
        for (List<List<Token>> g : groups(req)) {
            for (Token t : g.get(0)) {
                if (!t.isSlotRule()) {
                    out.add(t);
                }
            }
        }
        return out;
    }

    /** Every item id mentioned (for tests). */
    public static List<String> items(String req) {
        List<String> out = new ArrayList<>();
        for (List<List<Token>> g : groups(req)) {
            for (List<Token> alt : g) {
                for (Token t : alt) {
                    if (!t.isSlotRule()) {
                        out.add(t.item());
                    }
                }
            }
        }
        return out;
    }
}
