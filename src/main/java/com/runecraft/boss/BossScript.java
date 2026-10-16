package com.runecraft.boss;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Optional per-boss behaviour layered on top of a data-driven {@link DataBossEntity}. Register with
 * {@link BossScripts#register(String, java.util.function.Supplier)} using the BossDef id; a fresh
 * instance is created per boss entity, so scripts may keep state.
 */
public interface BossScript {
    /**
     * Adjusts the defence a player's attack of {@code type} meets ({@code in.targetDefenceBonus} and, for ranged,
     * {@code in.targetRangedDefence}): e.g. Yama's magic defence follows his target's style and rises in phase 3.
     */
    default void adjustDefence(DataBossEntity boss, ServerPlayerEntity attacker, AttackType type, com.runecraft.osrs.PlayerHitCalc.Input in) {
    }

    /** Called when the boss spawns. */
    default void onSpawn(DataBossEntity boss) {
    }

    /** Called every Minecraft tick on the server. */
    default void tick(DataBossEntity boss) {
    }

    /** Called every game tick (0.6s). */
    default void gameTick(DataBossEntity boss, long gameTick) {
    }

    /**
     * Chooses the next attack. Return null to use the default weighted choice from the
     * BossDef / phase attack list.
     */
    default AttackDef chooseAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
        return null;
    }

    /** Adjusts damage dealt to the boss (immunities, damage caps, shields...). */
    default int modifyIncoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
        return damage;
    }

    /** Adjusts damage the boss deals to a player just before it's applied. */
    default int onHitPlayer(DataBossEntity boss, ServerPlayerEntity player, int damage, AttackType type) {
        return damage;
    }

    /** Called when the boss enters a new phase (index into BossDef.phases). */
    default void onPhase(DataBossEntity boss, int phase) {
    }

    /** Called when a player kills the boss (after drops). */
    default void onDeath(DataBossEntity boss, ServerPlayerEntity killer) {
    }

    /** Whether the boss can be damaged right now (e.g. Zulrah diving, Vorkath's spawn phase). */
    default boolean isVulnerable(DataBossEntity boss) {
        return true;
    }
}
