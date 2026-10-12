package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.content.ItemSink;

import java.util.List;

/**
 * Demonic ashes, scattered for Prayer XP (OSRS): fiendish 10, vile 25, malicious 65, abyssal 85.
 * Abyssal ashes are defined by the slayer module (abyssal demons drop them); the others here.
 */
public final class PrayerData extends SkillingPart {

    /** Ashes and the Prayer XP for scattering them. */
    public record Ashes(String id, String name, double xp, int color, int value) {
    }

    public static final List<Ashes> ASHES = List.of(
            new Ashes("fiendish_ashes", "Fiendish ashes", 10.0, 0x6A5A5A, 15),
            new Ashes("vile_ashes", "Vile ashes", 25.0, 0x5A4A4A, 60),
            new Ashes("malicious_ashes", "Malicious ashes", 65.0, 0x4A3A3A, 300),
            new Ashes("abyssal_ashes", "Abyssal ashes", 85.0, 0x4A2A4A, 1000));

    public static Ashes ashes(String id) {
        for (Ashes a : ASHES) {
            if (a.id().equals(id)) {
                return a;
            }
        }
        return null;
    }

    @Override
    public void items(ItemSink s) {
        for (Ashes a : ASHES) {
            if (!a.id().equals("abyssal_ashes")) {
                simple(s, a.id(), a.name(), "ashes", a.color(), a.value())
                        .examine("Demonic ashes. Scatter them for Prayer experience.");
            }
        }
    }
}
