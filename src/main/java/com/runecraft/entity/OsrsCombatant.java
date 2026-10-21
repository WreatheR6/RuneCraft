package com.runecraft.entity;

import com.runecraft.combat.MonsterStats;
import com.runecraft.osrs.AttackType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/**
 * A monster on OSRS AI (round 42): a built-in or module creature ({@link OsrsMonsterEntity}) or a vanilla mob standing in for one
 * in Gielinor ({@link VanillaOsrsAi}: Giant rat, Spider, Giant spider, Zombie, Skeleton, Wolf, the witch as a Dark wizard). The
 * attack goal ({@link OsrsAttackGoal}) and the target goal ({@link OsrsTargetGoal}) only talk to this.
 */
public interface OsrsCombatant {
    Mob self();

    MonsterStats stats();

    /** Style of the next attack against a target at this squared distance. */
    AttackType chooseStyle(ServerPlayer target, double distanceSq);

    /** Starts an attack now (the attack goal stamps the cadence). */
    void performAttack(ServerPlayer target, AttackType style);

    /** Whether this monster attacks this player unprovoked (aggressiveness, combat level, tolerance, module rules, single-way). */
    boolean wantsToAttack(ServerPlayer player);

    /** How far (blocks) it looks for a player to attack. */
    double huntRange();

    /** Closes to melee reach before attacking whatever its style. */
    default boolean meleeRangeOnly() {
        return false;
    }

    /** Distance (blocks) a ranged or magic attacker attacks from; 0 keeps the default 10. */
    default double rangedReach() {
        return 0;
    }

    /** OSRS "dumb" pathing: walks straight at its target. */
    default boolean directPathing() {
        return false;
    }

    /** How far (blocks) its attacks reach: its ranged reach for ranged or magic attackers, else one melee tile. */
    default double attackReach() {
        MonsterStats s = stats();
        if (s != null && !s.style().isMelee() && !meleeRangeOnly()) {
            return rangedReach() > 0 ? rangedReach() : 10.0;
        }
        return com.runecraft.osrs.ThreatTiming.MELEE_REACH;
    }
}
