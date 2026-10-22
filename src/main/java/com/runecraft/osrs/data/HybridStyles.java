package com.runecraft.osrs.data;

import com.runecraft.osrs.AttackType;

import java.util.Map;

/**
 * Monster attacks whose accuracy is rolled in another style than the one the protection prayers see (OSRS Wiki, "Magical
 * melee", "Ranged magic", "Magical ranged"): the attack keeps its own style for the prayer that blocks it, while the hit
 * roll uses the monster's {@link Hybrid#accuracy} level and bonus against the player's {@link Hybrid#defence} bonus.
 * Keyed by creature id and attack name.
 */
public final class HybridStyles {
    /** The style a hybrid attack rolls its accuracy with, and the player's defence bonus it rolls against. */
    public record Hybrid(AttackType accuracy, AttackType defence) {
    }

    private static final Map<String, Hybrid> HYBRIDS = Map.of(
            // Kree'arra (OSRS Wiki infobox: "Ranged, Ranged magic, Magical melee"): his talon swipe is magical melee
            // (Protect from Melee blocks it; his Magic against the player's magic defence), his wind blast ranged magic
            // (Protect from Magic blocks it; Mod Ash: "Kree's magic attack rolls its MAGIC accuracy against your RANGED
            // defence").
            key("kreearra", "talon swipe"), new Hybrid(AttackType.MAGIC, AttackType.MAGIC),
            key("kreearra", "wind blast"), new Hybrid(AttackType.MAGIC, AttackType.RANGED));

    private HybridStyles() {
    }

    private static String key(String creatureId, String attackName) {
        return creatureId + "/" + attackName;
    }

    /** The hybrid rule of a creature's attack, or null when it rolls in its own style. */
    public static Hybrid of(String creatureId, String attackName) {
        return creatureId == null || attackName == null ? null : HYBRIDS.get(key(creatureId, attackName));
    }
}
