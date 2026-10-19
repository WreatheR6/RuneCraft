package com.runecraft.boss;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

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
    default void adjustDefence(DataBossEntity boss, ServerPlayer attacker, AttackType type, com.runecraft.osrs.PlayerHitCalc.Input in) {
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
    default AttackDef chooseAttack(DataBossEntity boss, ServerPlayer target, double distance) {
        return null;
    }

    /**
     * Called just before the engine's attack goal performs {@code attack} (the weighted choice from the BossDef / phase
     * attack list); returns the attack to perform instead. Lets a script act on the boss's real next attack, e.g.
     * Phosani's Prayer Deactivation.
     */
    default AttackDef beforeAttack(DataBossEntity boss, ServerPlayer target, AttackDef attack) {
        return attack;
    }

    /** Adjusts damage dealt to the boss (immunities, damage caps, shields...). */
    default int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
        return damage;
    }

    /** Adjusts damage the boss deals to a player just before it's applied. */
    default int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
        return damage;
    }

    /** Called when the boss enters a new phase (index into BossDef.phases). */
    default void onPhase(DataBossEntity boss, int phase) {
    }

    /** Called when a player kills the boss (after drops). */
    default void onDeath(DataBossEntity boss, ServerPlayer killer) {
    }

    /**
     * Saves what has to outlive the entity being unloaded (chunk unload, logout, restart): a reloaded boss gets a fresh
     * script, so anything it put in the world (e.g. the Royal Titans' wall blocks) must be recorded here to be cleaned up.
     */
    default void writeNbt(DataBossEntity boss, CompoundTag nbt) {
    }

    /** Reads what {@link #writeNbt} saved, when the boss is loaded again. */
    default void readNbt(DataBossEntity boss, CompoundTag nbt) {
    }

    /**
     * Called (server side) when the boss leaves the world for good, also when {@link #onDeath} never ran: discarded,
     * killed by something other than a player, or moved to another dimension.
     */
    default void onRemoved(DataBossEntity boss) {
    }

    /** Whether the boss can be damaged right now (e.g. Zulrah diving, Vorkath's spawn phase). */
    default boolean isVulnerable(DataBossEntity boss) {
        return true;
    }
}
