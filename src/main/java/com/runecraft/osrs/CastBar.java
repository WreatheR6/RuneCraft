package com.runecraft.osrs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The telegraph cast bar (round 42, {@code // playability}): one entry per telegraphed attack on its way to the player,
 * with the attacker, its style, a countdown in seconds and the key of the protection prayer that blocks it. OSRS shows a
 * telegraph as an animation the player reads on its 0.6 s ticks (Jad rearing up or stomping); in real time the same lead
 * (at least {@link ThreatTiming#TELEGRAPH_FLOOR_MC} MC) is shown as a bar that empties until the hit lands. Overlapping
 * telegraphs stack, soonest first.
 *
 * <p>The server sends one small packet per telegraph (source entity, style, flags, MC ticks until it lands); the client keeps
 * the list with {@link #add} / {@link #expire} and draws {@link #shown}. Style colours are the fixed round 41 ones.
 */
public final class CastBar {
    public static final int MELEE = 0;
    public static final int RANGED = 1;
    public static final int MAGIC = 2;
    /** An area or typeless attack: no prayer blocks it. */
    public static final int TYPELESS = 3;

    /** The hit lands on a marked spot: step out of it (sky strikes). */
    public static final int FLAG_DODGE = 1;
    /** No protection prayer helps at all (for a script's cast; no AttackDef special maps to it, see {@link #flagsOf}). */
    public static final int FLAG_UNPRAYABLE = 2;
    /**
     * The style's protection prayer lowers it without blocking it, so it is still the prayer to have up: dragonfire (Protect
     * from Magic caps it at two thirds without a shield or antifire, {@code Mechanics.hit}; OSRS Wiki, Dragonfire) and the
     * prayer-piercing hits, whose boss scripts take part off under the right prayer (the Hunllef's 12 / 16, Callisto's halved
     * swipe; the bears' knockback orb is blocked whole). The row shows the key with "part" and the usual right/wrong frame.
     */
    public static final int FLAG_REDUCED = 4;
    /** What the row shows after the key of a hit the prayer only lowers ({@link #FLAG_REDUCED}). */
    public static final String REDUCED_SUFFIX = " part";

    /** Colours (RGB) of the styles: melee red, ranged green, magic blue, typeless orange. */
    public static final int MELEE_RGB = 0xE03030;
    public static final int RANGED_RGB = 0x40C040;
    public static final int MAGIC_RGB = 0x4080FF;
    public static final int TYPELESS_RGB = 0xFFA020;

    /** Entries shown at once (the soonest). */
    public static final int MAX_SHOWN = 4;
    /** MC ticks an entry stays after its hit lands ("0.0s", so a hit landing on the frame it empties is still seen). */
    public static final int LINGER_MC = 6;
    /** A telegraph never shows longer than this (a lost packet or a cancelled attack can't leave a bar forever). */
    public static final int MAX_LEAD_MC = 20 * 20;
    /**
     * Blocks past a ground strike's or hazard's edge within which a player still gets its row (round 43): near enough to
     * walk into it before it lands at a run's 4.3 blocks a second.
     */
    public static final double NEAR_MARGIN = 2.5;
    /** Blocks above or below a ground strike within which a player gets its row (its hit test's 2-block tolerance and a step). */
    public static final double NEAR_DY = 3.0;

    private CastBar() {
    }

    /** One telegraph on its way: source entity id, style, flags, and the MC ticks it started and lands on (client clock). */
    public record Cast(int source, int style, int flags, long startMc, long landMc, String label) {
        public Cast {
            label = label == null ? "" : label;
        }

        /** Whether the style's protection prayer blocks it, or lowers it ({@link #reduced}). */
        public boolean prayable() {
            return style != TYPELESS && (flags & (FLAG_DODGE | FLAG_UNPRAYABLE)) == 0;
        }

        /** Whether the protection prayer only lowers it ({@link CastBar#FLAG_REDUCED}). */
        public boolean reduced() {
            return prayable() && (flags & FLAG_REDUCED) != 0;
        }

        /** Index of the protection prayer that blocks it (0 magic, 1 missiles, 2 melee, as {@link PrayerGrace#PROTECTIONS}), or -1. */
        public int protection() {
            return prayable() ? protectionOf(style) : -1;
        }

        /** MC ticks until it lands (never negative). */
        public long left(long nowMc) {
            return Math.max(0, landMc - nowMc);
        }

        /** How far the bar has run, 0 at the cue to 1 at the landing. */
        public float progress(float nowMc) {
            float span = Math.max(1, landMc - startMc);
            return Math.max(0f, Math.min(1f, (nowMc - startMc) / span));
        }

        public int rgb() {
            return CastBar.rgb(style);
        }
    }

    /**
     * The cast-bar flags of an attack special: a sky strike lands on a marked spot (step out: {@link #FLAG_DODGE}); dragonfire
     * and prayer-piercing hits are lowered, not blocked, by the style's protection prayer ({@link #FLAG_REDUCED}).
     */
    public static int flagsOf(com.runecraft.osrs.content.AttackDef.Special special) {
        if (special == null) {
            return 0;
        }
        return switch (special) {
            case SKY_STRIKE -> FLAG_DODGE;
            case DRAGONFIRE, PRAYER_PIERCING -> FLAG_REDUCED;
            default -> 0;
        };
    }

    /**
     * Whether a player at ({@code dx}, {@code dy}, {@code dz}) from a ground strike's centre gets its cast-bar row (round 43):
     * horizontally within its radius plus {@link #NEAR_MARGIN}, and within {@link #NEAR_DY} of its height. Players further off
     * can't be hit by it, so a boss's volley elsewhere in the arena doesn't fill their bar.
     */
    public static boolean nearStrike(double dx, double dy, double dz, double radius) {
        double r = Math.max(0, radius) + NEAR_MARGIN;
        return dx * dx + dz * dz <= r * r && Math.abs(dy) < NEAR_DY;
    }

    /**
     * The row label of a named ground strike or hazard ("fire_wall" to "Fire wall", "acid" to "Acid"), or null for no name
     * (the row then names its source, the boss).
     */
    public static String strikeLabel(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String s = name.replace('_', ' ').trim().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * MC ticks from now until a strike lands: its landing game tick {@code dueGameTick} (x 12) less the clock now in MC
     * ({@code nowGameTick} x 12 plus the MC ticks into it). Never negative.
     */
    public static int landInMc(long dueGameTick, long nowGameTick, int mcIntoTick) {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, dueGameTick * ThreatTiming.MC_PER_GAME_TICK
                - (nowGameTick * ThreatTiming.MC_PER_GAME_TICK + mcIntoTick)));
    }

    /** The cast-bar style of an attack style (null: typeless). */
    public static int styleOf(AttackType type) {
        if (type == null) {
            return TYPELESS;
        }
        return type == AttackType.MAGIC ? MAGIC : type == AttackType.RANGED ? RANGED : MELEE;
    }

    /** The protection index (0 magic, 1 missiles, 2 melee) of a cast-bar style, or -1 for typeless. */
    public static int protectionOf(int style) {
        return switch (style) {
            case MAGIC -> 0;
            case RANGED -> 1;
            case MELEE -> 2;
            default -> -1;
        };
    }

    public static int rgb(int style) {
        return switch (style) {
            case MAGIC -> MAGIC_RGB;
            case RANGED -> RANGED_RGB;
            case MELEE -> MELEE_RGB;
            default -> TYPELESS_RGB;
        };
    }

    /** The style's short tag on the bar: MAG, RNG, MEL, or "!" for an area / typeless hit. */
    public static String tag(int style) {
        return switch (style) {
            case MAGIC -> "MAG";
            case RANGED -> "RNG";
            case MELEE -> "MEL";
            default -> "!";
        };
    }

    /** A countdown in seconds with one decimal ("1.4s"), from MC ticks (20 a second). */
    public static String seconds(long mcLeft) {
        return String.format(Locale.ROOT, "%.1fs", Math.max(0, mcLeft) / 20.0);
    }

    /**
     * Adds a telegraph to the list: an exact repeat (same source, style and landing tick) is dropped, a lead longer than
     * {@link #MAX_LEAD_MC} is capped. Returns the entry kept.
     */
    public static Cast add(List<Cast> casts, int source, int style, int flags, long nowMc, int landInMc, String label) {
        long land = nowMc + Math.max(0, Math.min(MAX_LEAD_MC, landInMc));
        for (Cast c : casts) {
            if (c.source() == source && c.style() == style && Math.abs(c.landMc() - land) <= 1) {
                return c;
            }
        }
        Cast cast = new Cast(source, style, flags, nowMc, land, label);
        casts.add(cast);
        return cast;
    }

    /** Drops the entries whose hit landed more than {@link #LINGER_MC} MC ago. */
    public static void expire(List<Cast> casts, long nowMc) {
        casts.removeIf(c -> nowMc > c.landMc() + LINGER_MC);
    }

    /** The entries to draw, soonest landing first, at most {@code max}. */
    public static List<Cast> shown(List<Cast> casts, int max) {
        List<Cast> out = new ArrayList<>(casts);
        out.sort(Comparator.comparingLong(Cast::landMc).thenComparingLong(Cast::startMc));
        return out.size() > max ? new ArrayList<>(out.subList(0, max)) : out;
    }

    /**
     * Whether the player's overhead is the right one for a cast: true when the protection it needs ({@link Cast#protection})
     * is active ({@code activeProtection} = its index, -1 for none); null when no overhead matters (typeless, a dodge, a
     * script's unprayable cast). A hit the prayer only lowers ({@link #FLAG_REDUCED}) still wants it up.
     */
    public static Boolean rightOverhead(Cast cast, int activeProtection) {
        int need = cast.protection();
        return need < 0 ? null : need == activeProtection;
    }

    /**
     * The protection key to press for a needed protection under a prayer curse: {@code curse[i]} is the protection a press of
     * key {@code i} switches (identity when uncursed; the Nightmare's curse shuffles them). Returns the key index, or
     * {@code need} when no key reaches it.
     */
    public static int keyFor(int need, int[] curse) {
        if (need < 0 || curse == null) {
            return need;
        }
        for (int i = 0; i < curse.length; i++) {
            if (curse[i] == need) {
                return i;
            }
        }
        return need;
    }

    /** Whether a curse map shuffles anything (a key switches another protection than its own). */
    public static boolean cursed(int[] curse) {
        if (curse == null) {
            return false;
        }
        for (int i = 0; i < curse.length; i++) {
            if (curse[i] != i) {
                return true;
            }
        }
        return false;
    }
}
