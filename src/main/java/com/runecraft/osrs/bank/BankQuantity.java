package com.runecraft.osrs.bank;

import java.util.Locale;

/**
 * The OSRS bank's default quantity buttons (1, 5, 10, X, All) plus the right-click "All-but-1": how many of an
 * item a withdraw or deposit moves, given how many are available.
 */
public final class BankQuantity {
    /** OSRS stack cap (2,147,483,647). */
    public static final long MAX = Integer.MAX_VALUE;

    public enum Mode {
        ONE("1"), FIVE("5"), TEN("10"), X("X"), ALL("All"), ALL_BUT_ONE("All-but-1");

        public final String label;

        Mode(String label) {
            this.label = label;
        }

        public static Mode byOrdinal(int i) {
            Mode[] v = values();
            return i >= 0 && i < v.length ? v[i] : ONE;
        }
    }

    private BankQuantity() {
    }

    /**
     * Amount to move for a quantity mode.
     *
     * @param mode      the selected quantity button
     * @param x         the remembered X amount (used by {@link Mode#X})
     * @param available how many there are to move
     * @return 0..available
     */
    public static long amount(Mode mode, long x, long available) {
        if (available <= 0) {
            return 0;
        }
        long want = switch (mode) {
            case ONE -> 1;
            case FIVE -> 5;
            case TEN -> 10;
            case X -> Math.max(0, x);
            case ALL -> available;
            case ALL_BUT_ONE -> available - 1;
        };
        return Math.max(0, Math.min(want, available));
    }

    /**
     * Parses an OSRS "Enter amount" value: digits with optional commas and a k/m/b suffix ("10k", "1.5m").
     * Returns -1 for invalid input; results are capped at {@link #MAX}.
     */
    public static long parseX(String text) {
        if (text == null) {
            return -1;
        }
        String t = text.trim().toLowerCase(Locale.ROOT).replace(",", "");
        if (t.isEmpty()) {
            return -1;
        }
        double mult = 1;
        char last = t.charAt(t.length() - 1);
        if (last == 'k' || last == 'm' || last == 'b') {
            mult = last == 'k' ? 1e3 : last == 'm' ? 1e6 : 1e9;
            t = t.substring(0, t.length() - 1);
        }
        try {
            double v = Double.parseDouble(t) * mult;
            if (Double.isNaN(v) || v < 0) {
                return -1;
            }
            return (long) Math.min(MAX, Math.floor(v));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** OSRS stack-size text: up to 99,999 as is, then "100K" (to 9,999,999), then "10M". */
    public static String shortCount(long n) {
        if (n >= 10_000_000) {
            return (n / 1_000_000) + "M";
        }
        if (n >= 100_000) {
            return (n / 1000) + "K";
        }
        return Long.toString(n);
    }

    /** Colour of OSRS stack text: yellow below 100K, white below 10M, green above. */
    public static int countColor(long n) {
        if (n >= 10_000_000) {
            return 0x00FF80;
        }
        if (n >= 100_000) {
            return 0xFFFFFF;
        }
        return 0xFFFF00;
    }
}
