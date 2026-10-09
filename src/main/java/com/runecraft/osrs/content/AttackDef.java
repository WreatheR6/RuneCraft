package com.runecraft.osrs.content;

import com.runecraft.osrs.AttackType;

/**
 * One attack in a monster's or boss's rotation. Data-driven bosses pick attacks by weight among
 * those usable at the current distance. {@link Special} adds the signature mechanic.
 */
public record AttackDef(String name, AttackType style, int maxHit, double range, int weight, Special special,
                        int param, int telegraphTicks, String particle, String message) {

    public enum Special {
        /** A normal hit. */
        NONE,
        /** Hits every player within {@code param} blocks of the boss. */
        AOE,
        /** Marks the target's tile; after the telegraph anyone still within 1.5 blocks is hit. Move to dodge. */
        SKY_STRIKE,
        /** Ignores protection prayers. */
        PRAYER_PIERCING,
        /** Also drains {@code param} prayer points on hit. */
        DRAIN_PRAYER,
        /** Poisons the target (starting damage {@code param}). */
        POISON,
        /** Envenoms the target (venom starts at 6 and ramps up). */
        VENOM,
        /** Heals the boss by {@code param}% of the damage dealt. */
        LIFESTEAL,
        /** Spawns {@code param} minions (BossDef.minion) instead of hitting. */
        SUMMON,
        /** Knocks the target back {@code param} blocks. */
        KNOCKBACK,
        /** Freezes the target in place for {@code param} game ticks. */
        FREEZE,
        /** Pulls the target next to the boss. */
        PULL,
        /** Dragonfire: max hit is reduced to 10 with an anti-dragon shield and to 0 with antifire too. */
        DRAGONFIRE,
        /** Leaves a damaging hazard ({@code param} damage per tick) on the ground under the target for 10 ticks. */
        HAZARD,
        /** Drains the given skill: param = skill ordinal; drain is 10% of the level. */
        DRAIN_STAT,
        /** Teleports the boss next to the target. */
        BLINK,
        /** Heals the boss by {@code param} hitpoints instead of attacking. */
        HEAL_SELF
    }

    public static AttackDef melee(int maxHit) {
        return new AttackDef("melee", AttackType.CRUSH, maxHit, 2.5, 10, Special.NONE, 0, 0, null, null);
    }

    public static AttackDef of(String name, AttackType style, int maxHit, double range, int weight) {
        return new AttackDef(name, style, maxHit, range, weight, Special.NONE, 0, 0, null, null);
    }

    public AttackDef special(Special special, int param) {
        return new AttackDef(name, style, maxHit, range, weight, special, param, telegraphTicks, particle, message);
    }

    /** Delay (game ticks) between the wind-up message/particles and the hit landing. */
    public AttackDef telegraph(int ticks, String message) {
        return new AttackDef(name, style, maxHit, range, weight, special, param, ticks, particle, message);
    }

    /** Vanilla particle id used for the projectile / effect, e.g. "minecraft:flame". */
    public AttackDef particle(String particle) {
        return new AttackDef(name, style, maxHit, range, weight, special, param, telegraphTicks, particle, message);
    }

    public AttackDef message(String message) {
        return new AttackDef(name, style, maxHit, range, weight, special, param, telegraphTicks, particle, message);
    }

    public boolean melee() {
        return style.isMelee();
    }
}
