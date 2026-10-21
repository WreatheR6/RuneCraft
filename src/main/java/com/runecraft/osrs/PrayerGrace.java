package com.runecraft.osrs;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * Real-time leniency for protection prayer switches, plus the small prayer helpers the HUD and quick-prayers share.
 *
 * <p>// playability: OSRS reads the overhead prayer on the exact game tick a hit resolves (0.6 s precision). A Minecraft
 * player switching with a key press a moment too late would lose the hit, so a <em>voluntary</em> switch away from a
 * protection prayer keeps protecting from the old style for {@link #GRACE_MC} Minecraft ticks (0.4 s), provided the old
 * prayer had been on for at least {@link #MIN_ON_MC} ticks (0.6 s, one game tick), so flicking every prayer on and off
 * can't cover all three styles. Forced removals (drained to zero, boss prayer disables, the Nightmare's curse) never get
 * the grace. Block amounts are untouched: 100% against monsters, 40% against players.
 */
public final class PrayerGrace {
    /** Minecraft ticks an overhead keeps protecting after the player switches it off (0.4 s). */
    public static final int GRACE_MC = 8;
    /** Minecraft ticks the overhead must have been on before the switch for the grace to apply (0.6 s). */
    public static final int MIN_ON_MC = 12;
    /** "Never" for the stamps: far enough in the past that neither condition can hold. */
    public static final long NEVER = -1_000_000L;
    /**
     * Late side of the grace (round 42): Minecraft ticks a monster hit that lands while its protection is off is held and
     * judged again (0.2 s), so a switch pressed a hair late (key press, network, server tick) still blocks it.
     */
    public static final int LATE_MC = 4;
    /** A voluntary overhead switch at most this many Minecraft ticks ago marks the player as switching (two game ticks). */
    public static final int SWITCHING_MC = 24;

    /** The three protection overheads, in the order of {@link #index(Prayer)}. */
    public static final Prayer[] PROTECTIONS = {Prayer.PROTECT_FROM_MAGIC, Prayer.PROTECT_FROM_MISSILES, Prayer.PROTECT_FROM_MELEE};

    private PrayerGrace() {
    }

    /**
     * Whether a protection counts at {@code now}: it is on, or it was switched off voluntarily at {@code offAt} at most
     * {@link #GRACE_MC} ticks ago after having been on since {@code onAt} for at least {@link #MIN_ON_MC} ticks.
     */
    public static boolean protectedWithGrace(boolean activeNow, long onAt, long offAt, long now) {
        return activeNow || (now - offAt <= GRACE_MC && now >= offAt && offAt - onAt >= MIN_ON_MC);
    }

    /**
     * Whether a monster hit landing at {@code now} is held for the late window ({@link #LATE_MC}) instead of resolving at once.
     * // playability: OSRS reads the overhead on the exact tick a hit resolves; a real-time player who presses the right
     * protection a moment after the impact would lose the hit. Only a hit its protection doesn't cover yet, on a player who could
     * switch that protection on ({@code canSwitch}: the level, points and rules allow it) and is switching protections (one is
     * on now, or one was switched voluntarily at {@code lastSwitchMc}, at most {@link #SWITCHING_MC} ago), is held: a player
     * who prays nothing takes every hit on its tick. The roll, max hit and block amount are those of an on-time hit.
     */
    public static boolean holdLate(boolean protectedNow, boolean canSwitch, boolean anyProtectionOn, long lastSwitchMc, long now) {
        if (protectedNow || !canSwitch) {
            return false;
        }
        return anyProtectionOn || (now >= lastSwitchMc && now - lastSwitchMc <= SWITCHING_MC);
    }

    /**
     * Whether a hit held since {@code heldAt} resolves at {@code now}: the moment its protection counts (it is then blocked as
     * an on-time hit would be), or {@link #LATE_MC} ticks after the impact (it then lands unchanged).
     */
    public static boolean lateDue(boolean protectedNow, long heldAt, long now) {
        return protectedNow || now - heldAt >= LATE_MC;
    }

    /** The latest voluntary switch of any protection overhead in the stamps ({@link #NEVER} when none). */
    public static long lastSwitch(long[] onAt, long[] offAt) {
        long last = NEVER;
        for (int i = 0; i < PROTECTIONS.length; i++) {
            last = Math.max(last, Math.max(onAt[i], offAt[i]));
        }
        return last;
    }

    /** Index into the per-overhead stamps: 0 magic, 1 missiles, 2 melee; -1 for any other prayer. */
    public static int index(Prayer p) {
        if (p == null) {
            return -1;
        }
        switch (p) {
            case PROTECT_FROM_MAGIC:
                return 0;
            case PROTECT_FROM_MISSILES:
                return 1;
            case PROTECT_FROM_MELEE:
                return 2;
            default:
                return -1;
        }
    }

    /** Index of the protection that blocks an attack style: magic 0, ranged 1, every melee style 2. */
    public static int index(AttackType style) {
        if (style == AttackType.MAGIC) {
            return 0;
        }
        return style == AttackType.RANGED ? 1 : 2;
    }

    /** The protection prayer that blocks an attack style. */
    public static Prayer protectionFor(AttackType style) {
        return PROTECTIONS[index(style)];
    }

    /** A prayer set as a bit mask (bit = ordinal), the form quick-prayers are saved and sent in. */
    public static long mask(Collection<Prayer> prayers) {
        long m = 0;
        for (Prayer p : prayers) {
            m |= 1L << p.ordinal();
        }
        return m;
    }

    /** The prayers in a bit mask; unknown bits (ordinals past the enum) are ignored. */
    public static Set<Prayer> fromMask(long mask) {
        Set<Prayer> out = EnumSet.noneOf(Prayer.class);
        for (Prayer p : Prayer.values()) {
            if ((mask & (1L << p.ordinal())) != 0) {
                out.add(p);
            }
        }
        return out;
    }

    /**
     * Three-letter overhead tag for the side panel and HUD (OSRS draws a distinct icon for each): MAG, RNG, MEL, RET, RED,
     * SMI; null for a prayer that isn't an overhead.
     */
    public static String overheadTag(Prayer p) {
        switch (p) {
            case PROTECT_FROM_MAGIC:
                return "MAG";
            case PROTECT_FROM_MISSILES:
                return "RNG";
            case PROTECT_FROM_MELEE:
                return "MEL";
            case RETRIBUTION:
                return "RET";
            case REDEMPTION:
                return "RED";
            case SMITE:
                return "SMI";
            default:
                return null;
        }
    }

    /** The style colour of an overhead's tag (ARGB): magic blue, missiles green, melee red, others prayer gold. */
    public static int overheadColor(Prayer p) {
        switch (p) {
            case PROTECT_FROM_MAGIC:
                return 0xFF4080FF;
            case PROTECT_FROM_MISSILES:
                return 0xFF40C040;
            case PROTECT_FROM_MELEE:
                return 0xFFE03030;
            default:
                return 0xFFE0C060;
        }
    }

    /**
     * The prayer set after switching {@code p} on (true) or off from {@code active}: switching on removes every prayer it
     * conflicts with, as the server's toggle does. Used for the client's optimistic HUD.
     */
    public static long switched(long active, Prayer p, boolean on) {
        long bit = 1L << p.ordinal();
        if (!on) {
            return active & ~bit;
        }
        long out = active;
        for (Prayer other : Prayer.values()) {
            if (p.conflictsWith(other)) {
                out &= ~(1L << other.ordinal());
            }
        }
        return out | bit;
    }
}
