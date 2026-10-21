package com.runecraft.osrs;

import java.util.ArrayList;
import java.util.List;

/**
 * The boss bar's status line and colour (round 42, {@code // playability}): the bar shows the fight's state that OSRS players
 * read from the boss's look and their own counting, "Vorkath · Phase 2/2", "The Hunllef · switches in 2", "The Nightmare ·
 * Cursed: MAG&gt;RNG RNG&gt;MEL MEL&gt;MAG", coloured by the style the boss attacks with (melee red, ranged green, magic
 * blue, typeless yellow, mixed white), so Zulrah's green, red and blue forms show as such.
 *
 * <p>{@code boss.BossScript#bossBarStatus} returns a {@link Status}; its default builds one from the boss's phase, its
 * current attack list and its target's curse map. A script that knows more (a style switch counting down) adds to it with
 * {@link Status#with}.
 */
public final class BossBarStatus {
    /** Boss bar colours, named as Minecraft's {@code BossEvent.BossBarColor}. */
    public enum Colour { PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE }

    /** A status line (parts joined with " · " after the boss's name) and a colour (null: leave the bar's colour). */
    public record Status(List<String> parts, Colour colour) {
        public Status {
            parts = parts == null ? List.of() : List.copyOf(parts);
        }

        /** This status with one more part (blank parts are skipped). */
        public Status with(String part) {
            if (part == null || part.isBlank()) {
                return this;
            }
            List<String> more = new ArrayList<>(parts);
            more.add(part);
            return new Status(more, colour);
        }

        /** This status in another colour. */
        public Status colour(Colour c) {
            return new Status(parts, c);
        }
    }

    /** One attack the boss may use: its style (null: typeless) and choice weight. */
    public record Attack(AttackType style, int weight) {
    }

    private BossBarStatus() {
    }

    /**
     * The colour of the style a boss attacks with: the style holding at least half of the attacks' weight (magic before
     * ranged before melee on a tie), yellow when it only has typeless attacks, white when no style holds half, null when it
     * has no attacks listed.
     */
    public static Colour styleColour(List<Attack> attacks) {
        int melee = 0;
        int ranged = 0;
        int magic = 0;
        int typeless = 0;
        for (Attack a : attacks) {
            int w = Math.max(1, a.weight());
            if (a.style() == null) {
                typeless += w;
            } else if (a.style() == AttackType.MAGIC) {
                magic += w;
            } else if (a.style() == AttackType.RANGED) {
                ranged += w;
            } else {
                melee += w;
            }
        }
        int styled = melee + ranged + magic;
        if (styled == 0) {
            return typeless > 0 ? Colour.YELLOW : null;
        }
        int top = Math.max(magic, Math.max(ranged, melee));
        if (top * 2 < styled) {
            return Colour.WHITE;
        }
        return magic == top ? Colour.BLUE : ranged == top ? Colour.GREEN : Colour.RED;
    }

    /** "Phase n/m" for a boss with phase thresholds ({@code phase} -1 before the first), else empty. */
    public static String phaseText(int phase, int thresholds) {
        if (thresholds <= 0) {
            return "";
        }
        return "Phase " + (Math.max(-1, Math.min(phase, thresholds - 1)) + 2) + "/" + (thresholds + 1);
    }

    /** "switches in n" for a script that counts the attacks to its next style switch (n &lt;= 0: "switching!"). */
    public static String switchText(int attacksLeft) {
        return attacksLeft <= 0 ? "switching!" : "switches in " + attacksLeft;
    }

    /**
     * The curse on the protection prayers, "Cursed: MAG&gt;RNG RNG&gt;MEL MEL&gt;MAG" ({@code curse[i]}: the protection a
     * click on protection {@code i} switches, indices as {@link PrayerGrace#PROTECTIONS}); empty when nothing is shuffled.
     */
    public static String curseText(int[] curse) {
        if (curse == null || !CastBar.cursed(curse)) {
            return "";
        }
        StringBuilder sb = new StringBuilder("Cursed:");
        for (int i = 0; i < curse.length && i < PrayerGrace.PROTECTIONS.length; i++) {
            int to = curse[i] >= 0 && curse[i] < PrayerGrace.PROTECTIONS.length ? curse[i] : i;
            sb.append(' ').append(PrayerGrace.overheadTag(PrayerGrace.PROTECTIONS[i])).append('>')
                    .append(PrayerGrace.overheadTag(PrayerGrace.PROTECTIONS[to]));
        }
        return sb.toString();
    }

    /**
     * The attacks a built-in boss ({@code entity.BossEntity}, round 43) chooses between, weighted as its own style roll: Graardor
     * 2/3 melee and 1/3 his ranged slam, K'ril 2/3 melee and 1/3 magic, TzTok-Jad magic or ranged (half each) and a third of
     * the time melee in reach, Bryophyta magic at range and half melee in reach; the others their one style. So the bar is red
     * for Graardor and K'ril, white for Jad (blue or green while a telegraph of his is on its way) and blue for Bryophyta.
     */
    public static List<Attack> attacksOf(com.runecraft.osrs.data.Boss boss) {
        return switch (boss.mechanic()) {
            case GRAARDOR -> List.of(new Attack(AttackType.CRUSH, 2), new Attack(AttackType.RANGED, 1));
            case KRIL -> List.of(new Attack(AttackType.SLASH, 2), new Attack(AttackType.MAGIC, 1));
            case JAD -> boss.meleeWhenAdjacent()
                    ? List.of(new Attack(AttackType.MAGIC, 2), new Attack(AttackType.RANGED, 2), new Attack(AttackType.CRUSH, 1))
                    : List.of(new Attack(AttackType.MAGIC, 1), new Attack(AttackType.RANGED, 1));
            case MIXED_MAGIC -> List.of(new Attack(AttackType.MAGIC, 3), new Attack(AttackType.CRUSH, 1));
            default -> List.of(new Attack(boss.style(), 1));
        };
    }

    /** The default status: the phase, the curse on the boss's target, the style colour. */
    public static Status of(int phase, int thresholds, List<Attack> attacks, int[] targetCurse) {
        return new Status(List.of(), styleColour(attacks)).with(phaseText(phase, thresholds)).with(curseText(targetCurse));
    }

    /** The bar's title: the boss's name, then the status parts joined with " · ". */
    public static String title(String name, Status status) {
        if (status == null || status.parts().isEmpty()) {
            return name;
        }
        return name + " · " + String.join(" · ", status.parts());
    }
}
