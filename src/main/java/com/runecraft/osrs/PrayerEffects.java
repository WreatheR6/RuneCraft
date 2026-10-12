package com.runecraft.osrs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The formulas behind the prayers whose effect isn't a stat multiplier (OSRS wiki):
 * <ul>
 *   <li>Smite: each hit on a player drains a quarter of the damage dealt from their Prayer points.</li>
 *   <li>Redemption: when a hit leaves the player below 10% of their max Hitpoints (and alive), they heal 25% of their
 *       base Prayer level and lose all their remaining Prayer points.</li>
 *   <li>Retribution: on death, deals 0 to 25% of the user's base Prayer level to every enemy next to them
 *       (only the opponent in single-way combat).</li>
 *   <li>Protect Item: one extra item kept on death (3 kept normally, 0 when skulled).</li>
 *   <li>Rapid Heal / Rapid Restore / Preserve: regeneration intervals.</li>
 * </ul>
 */
public final class PrayerEffects {
    /** Normal ticks between natural Hitpoints regeneration (1 HP a minute) and stat restore/decay. */
    public static final int REGEN_TICKS = 100;

    /** Prayer books / prayers in OSRS that this mod doesn't have, with the reason (tracked by PrayerAuditTest). */
    public static final Map<String, String> UNIMPLEMENTED = new LinkedHashMap<>();

    static {
        UNIMPLEMENTED.put("Ruinous Powers", "Leagues / Deadman-only prayer book (never added to the main game)");
    }

    private PrayerEffects() {
    }

    /** Prayer points Smite drains from the player it hits. */
    public static int smiteDrain(int damage) {
        return Math.max(0, damage) / 4;
    }

    /** Whether Redemption triggers: alive and below 10% of max Hitpoints after a hit. */
    public static boolean redemptionTriggers(int hitpoints, int maxHitpoints) {
        return hitpoints > 0 && hitpoints * 10 < maxHitpoints;
    }

    /** Hitpoints Redemption heals: 25% of the base Prayer level. */
    public static int redemptionHeal(int prayerLevel) {
        return prayerLevel / 4;
    }

    /** Retribution's max hit on each adjacent enemy: 25% of the base Prayer level. */
    public static int retributionMaxHit(int prayerLevel) {
        return prayerLevel / 4;
    }

    /** Number of items kept on death (OSRS: 3, none while skulled, +1 with Protect Item). */
    public static int itemsKept(boolean skulled, boolean protectItem) {
        return (skulled ? 0 : 3) + (protectItem ? 1 : 0);
    }

    /** Ticks between natural Hitpoints regeneration (halved by Rapid Heal). */
    public static int hitpointsRegenTicks(boolean rapidHeal) {
        return rapidHeal ? REGEN_TICKS / 2 : REGEN_TICKS;
    }

    /** Ticks between drained stats restoring by one level (halved by Rapid Restore). */
    public static int statRestoreTicks(boolean rapidRestore) {
        return rapidRestore ? REGEN_TICKS / 2 : REGEN_TICKS;
    }

    /** Ticks between boosted stats falling by one level (50% longer with Preserve). */
    public static int boostDecayTicks(boolean preserve) {
        return preserve ? REGEN_TICKS * 3 / 2 : REGEN_TICKS;
    }

    /** One item (or a stack) a player carries, by inventory slot, with its value per item. */
    public record Carried(int slot, int count, long value) {
    }

    /** One item kept on death: {@code count} items taken from {@code slot}. */
    public record Kept(int slot, int count) {
    }

    /**
     * Picks the items kept on death: the {@code keep} most valuable single items (OSRS counts each item of a stack
     * separately, so a stack can supply several kept items). Ties keep the earlier slot.
     */
    public static List<Kept> keptOnDeath(List<Carried> carried, int keep) {
        List<Carried> sorted = new ArrayList<>(carried);
        sorted.sort(Comparator.comparingLong(Carried::value).reversed().thenComparingInt(Carried::slot));
        List<Kept> out = new ArrayList<>();
        int left = keep;
        for (Carried c : sorted) {
            if (left <= 0) {
                break;
            }
            if (c.count() <= 0) {
                continue;
            }
            int n = Math.min(left, c.count());
            out.add(new Kept(c.slot(), n));
            left -= n;
        }
        return out;
    }
}
