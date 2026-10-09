package com.runecraft.combat;

import com.runecraft.osrs.AttackType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Extension points into player combat (special attacks, slayer helmet, salve amulet, void, ring of
 * recoil...). Register from a module's {@code ModuleHooks.init()}.
 */
public final class CombatHooks {
    /** Modifies a player's attack. All methods are optional. */
    public interface PlayerAttackModifier {
        /** Multiplier applied to the attack roll. */
        default double accuracy(ServerPlayerEntity player, LivingEntity target, AttackType type) {
            return 1.0;
        }

        /** Multiplier applied to the max hit. */
        default double maxHit(ServerPlayerEntity player, LivingEntity target, AttackType type) {
            return 1.0;
        }

        /** Last chance to change the rolled damage (e.g. a special attack hitting twice). */
        default int afterRoll(ServerPlayerEntity player, LivingEntity target, AttackType type, int damage) {
            return damage;
        }

        /** Called after the damage was applied. */
        default void afterHit(ServerPlayerEntity player, LivingEntity target, AttackType type, int dealt) {
        }
    }

    /** Reacts to a player being hit (recoil, defence procs...). */
    public interface PlayerHurtListener {
        void onHurt(ServerPlayerEntity player, LivingEntity attacker, int damage);
    }

    private static final List<PlayerAttackModifier> MODIFIERS = new ArrayList<>();
    private static final List<PlayerHurtListener> HURT = new ArrayList<>();

    private CombatHooks() {
    }

    public static void addModifier(PlayerAttackModifier modifier) {
        MODIFIERS.add(modifier);
    }

    public static void addHurtListener(PlayerHurtListener listener) {
        HURT.add(listener);
    }

    static double accuracy(ServerPlayerEntity p, LivingEntity t, AttackType type) {
        double m = 1.0;
        for (PlayerAttackModifier mod : MODIFIERS) {
            m *= mod.accuracy(p, t, type);
        }
        return m;
    }

    static double maxHit(ServerPlayerEntity p, LivingEntity t, AttackType type) {
        double m = 1.0;
        for (PlayerAttackModifier mod : MODIFIERS) {
            m *= mod.maxHit(p, t, type);
        }
        return m;
    }

    static int afterRoll(ServerPlayerEntity p, LivingEntity t, AttackType type, int damage) {
        for (PlayerAttackModifier mod : MODIFIERS) {
            damage = mod.afterRoll(p, t, type, damage);
        }
        return damage;
    }

    static void afterHit(ServerPlayerEntity p, LivingEntity t, AttackType type, int dealt) {
        for (PlayerAttackModifier mod : MODIFIERS) {
            mod.afterHit(p, t, type, dealt);
        }
    }

    static void hurt(ServerPlayerEntity p, LivingEntity attacker, int damage) {
        for (PlayerHurtListener l : HURT) {
            l.onHurt(p, attacker, damage);
        }
    }
}
