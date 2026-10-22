package com.runecraft.osrs.content.slayer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The rune pouch (OSRS Wiki "Rune pouch"; 750 Slayer reward points): it holds up to 16,000 each of three kinds of rune,
 * and spells are cast from it directly. Pure rules over its contents (rune item id to count); the Minecraft side keeps
 * the contents on the pouch's stack.
 */
public final class RunePouch {
    public static final String ID = "rune_pouch";
    /** Kinds of rune one pouch holds (the divine rune pouch's fourth slot is not modelled). */
    public static final int TYPES = 3;
    public static final int MAX_PER_TYPE = 16_000;

    private RunePouch() {
    }

    /** Whether an item may go in the pouch: any rune, combination runes included (wiki), never Nightmare Zone runes. */
    public static boolean accepts(String itemId) {
        return itemId != null && itemId.endsWith("_rune") && !itemId.startsWith("nmz_") && !itemId.equals("pure_essence");
    }

    /** How many of {@code rune} may still go into a pouch holding {@code contents}. */
    public static int room(Map<String, Integer> contents, String rune) {
        if (!accepts(rune)) {
            return 0;
        }
        int have = contents.getOrDefault(rune, 0);
        if (have <= 0 && kinds(contents) >= TYPES) {
            return 0;
        }
        return Math.max(0, MAX_PER_TYPE - have);
    }

    /** Kinds of rune held (stacks of zero don't count). */
    public static int kinds(Map<String, Integer> contents) {
        int n = 0;
        for (int c : contents.values()) {
            if (c > 0) {
                n++;
            }
        }
        return n;
    }

    /** Adds up to {@code count} runes; returns how many went in. */
    public static int deposit(Map<String, Integer> contents, String rune, int count) {
        int n = Math.min(Math.max(0, count), room(contents, rune));
        if (n > 0) {
            contents.merge(rune, n, Integer::sum);
        }
        return n;
    }

    /** Takes up to {@code count} runes out; returns how many came out (an emptied kind frees its slot). */
    public static int withdraw(Map<String, Integer> contents, String rune, int count) {
        int have = contents.getOrDefault(rune, 0);
        int n = Math.min(have, Math.max(0, count));
        if (have - n <= 0) {
            contents.remove(rune);
        } else {
            contents.put(rune, have - n);
        }
        return n;
    }

    /** A copy without empty kinds, in insertion order. */
    public static Map<String, Integer> clean(Map<String, Integer> contents) {
        Map<String, Integer> out = new LinkedHashMap<>();
        contents.forEach((k, v) -> {
            if (v != null && v > 0) {
                out.put(k, v);
            }
        });
        return out;
    }
}
