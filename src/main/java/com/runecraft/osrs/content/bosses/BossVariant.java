package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.data.Drop;

import java.util.List;
import java.util.Map;

/**
 * A harder (or level-scaled) version of a data-driven boss fought with the same entity: an awakened Desert Treasure II
 * boss or one delve level of the Doom of Mokhaiotl. The variant replaces the boss's combat levels, hitpoints, attack
 * speed, max hits and drop table, and counts its kills under its own kill-count key.
 *
 * @param kind        "awakened" or "delve"
 * @param level       the delve level (1 for awakened bosses)
 * @param kcKey       kill-count key the kill is credited to (the boss id for delves, "{@code <id>_awakened}" for awakened)
 * @param attackSpeed attack speed in ticks (0 keeps the boss's own)
 * @param maxHits     max hit per attack name; other attacks scale by {@code otherHitScale}
 * @param drops       the drop table rolled where the boss dies; empty when the loot is held for the player instead
 * @param petRate     1/n pet chance (0: none at this level, -1: the boss's usual rate)
 */
public record BossVariant(String bossId, String kind, int level, String name, String kcKey, int combatLevel, int hitpoints,
                          int attack, int strength, int defence, int magic, int ranged, int attackSpeed, Map<String, Integer> maxHits,
                          double otherHitScale, List<Drop> drops, int petRate) {
    public static final String AWAKENED = "awakened";
    public static final String DELVE = "delve";

    public boolean awakened() {
        return AWAKENED.equals(kind);
    }

    /** The delve level, or 0 for a variant that isn't a delve. */
    public int delve() {
        return DELVE.equals(kind) ? level : 0;
    }

    /** This variant's max hit with one of the boss's attacks. */
    public int maxHit(AttackDef attack) {
        Integer m = maxHits.get(attack.name());
        return m != null ? m : (int) Math.round(attack.maxHit() * otherHitScale);
    }

    /** Damage rolled against the boss's own max hit, rescaled to this variant's max hit. */
    public int scaleHit(AttackDef attack, int baseMax, int damage) {
        if (damage <= 0 || baseMax <= 0) {
            return damage;
        }
        return (int) Math.round(damage * (double) maxHit(attack) / baseMax);
    }
}
