package com.runecraft.osrs;

import java.util.Map;
import java.util.Set;

/**
 * OSRS vampyre tiers (the {@code vampyre1}, {@code vampyre2} and {@code vampyre3} attributes of the wiki's DPS calculator
 * data, weirdgloop/osrs-dps-calc {@code monsters.json}): tier 1 (juveniles, feral vampyres, Count Draynor, Dessous) take
 * damage from anything and +10% from silver weapons; tier 2 (juvinates) only take damage from silver weapons (capped at
 * 10 a hit), Ivandis / blisterwood weapons, or anything at half damage with Efaritay's aid worn; tier 3 (vyrewatch, vyres,
 * Vanstrom, Ranis) only from the Ivandis flail and blisterwood weapons. Keys are MonsterTraits keys (creature ids);
 * any other creature with the {@code vampyre} attribute counts as tier 1. Applied by {@link PlayerHitCalc}.
 */
public final class VampyreTiers {
    private VampyreTiers() {
    }

    private static final Map<String, Integer> TIERS = Map.ofEntries(
            Map.entry("vampyre_juvenile", 1), // Vampyre Juvenile (Meiyerditch / Darkmeyer / Burgh de Rott)
            Map.entry("gwd_vampire", 1), // Feral Vampyre (God Wars Dungeon)
            Map.entry("count_draynor", 1),
            Map.entry("nmz_count_draynor", 1),
            Map.entry("dessous", 1),
            Map.entry("nmz_dessous", 1),
            Map.entry("trek_vampyre_juvinate_1", 2), // Vampyre Juvinate (Level 59, Temple Trekking)
            Map.entry("trek_vampyre_juvinate_2", 2), // Level 90
            Map.entry("trek_vampyre_juvinate_3", 2), // Level 119
            Map.entry("vyrewatch_sentinel", 3),
            Map.entry("vyrewatch", 3),
            Map.entry("ranis_drakan", 3),
            Map.entry("vanstrom_klause", 3),
            Map.entry("lowerniel_drakan", 3),
            Map.entry("damien_leucurte", 3));

    /** The vampyre tier (1-3) of a creature, or 0 when it is not a vampyre. */
    public static int tier(String key, Set<String> attributes) {
        Integer t = key == null ? null : TIERS.get(key);
        if (t != null) {
            return t;
        }
        for (int i = 1; i <= 3; i++) {
            if (attributes.contains("vampyre" + i)) {
                return i;
            }
        }
        return attributes.contains("vampyre") ? 1 : 0;
    }

    /** Every listed creature key and its tier. */
    public static Map<String, Integer> all() {
        return TIERS;
    }
}
