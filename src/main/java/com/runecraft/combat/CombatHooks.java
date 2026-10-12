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

    /**
     * Can veto a player's attack before it happens (e.g. an uncharged trident or an inactive crystal bow).
     * Returns the message to show, or null to allow the attack.
     */
    public interface AttackGate {
        String deny(ServerPlayerEntity player, AttackType type);
    }

    private static final List<AttackGate> GATES = new ArrayList<>();
    private static final List<PlayerAttackModifier> MODIFIERS = new ArrayList<>();
    private static final List<PlayerHurtListener> HURT = new ArrayList<>();
    private static final List<DropTransformer> DROPS = new ArrayList<>();

    /** Changes a monster drop for its killer before it hits the ground (e.g. diary noted bones). */
    public interface DropTransformer {
        /** The stack to drop instead, or null to leave the drop alone. */
        net.minecraft.item.ItemStack transform(ServerPlayerEntity killer, LivingEntity mob, String item, int count);
    }

    public static void addDropTransformer(DropTransformer t) {
        DROPS.add(t);
    }

    /** The stack a killed monster drops for an item of its loot table. */
    public static net.minecraft.item.ItemStack dropStack(ServerPlayerEntity killer, LivingEntity mob, String item, int count) {
        for (DropTransformer t : DROPS) {
            net.minecraft.item.ItemStack s = t.transform(killer, mob, item, count);
            if (s != null && !s.isEmpty()) {
                return s;
            }
        }
        return com.runecraft.util.Inv.stack(item, count);
    }

    private CombatHooks() {
    }

    public static void addModifier(PlayerAttackModifier modifier) {
        MODIFIERS.add(modifier);
    }

    public static void addHurtListener(PlayerHurtListener listener) {
        HURT.add(listener);
    }

    public static void addAttackGate(AttackGate gate) {
        GATES.add(gate);
    }

    /** The first gate's refusal for this attack, or null when the attack may go ahead. */
    public static String deniedAttack(ServerPlayerEntity p, AttackType type) {
        for (AttackGate g : GATES) {
            String m = g.deny(p, type);
            if (m != null) {
                return m;
            }
        }
        return null;
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
