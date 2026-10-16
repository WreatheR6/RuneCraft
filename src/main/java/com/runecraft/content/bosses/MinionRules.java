package com.runecraft.content.bosses;

import com.runecraft.combat.CombatHooks;
import com.runecraft.osrs.AttackType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Special damage rules for boss minions and a hook for boss scripts that react to any player attack: protected
 * respiratory systems, the Judge of Yama's prayers, melee-only unstable ice, sleepwalkers dying to any hit, ur-maggot
 * larvae shot out of the air, the Royal Titans' walls doused by spells...
 */
final class MinionRules {
    /** How player attacks on one minion are resolved. */
    interface Rule {
        /** Accuracy multiplier (a large value: the attack always hits). */
        default double accuracy(ServerPlayerEntity player, LivingEntity minion, AttackType type) {
            return 1.0;
        }

        /** True when the attack always deals its max hit (the Judge of Yama, void flares); {@link #damage} then gets the max. */
        default boolean alwaysMaxHit(ServerPlayerEntity player, LivingEntity minion, AttackType type) {
            return false;
        }

        /** The damage the attack deals (after the roll). */
        int damage(ServerPlayerEntity player, LivingEntity minion, AttackType type, int damage);
    }

    /** Sees every player attack (hit or miss) on anything. */
    interface AttackWatcher {
        void onAttack(ServerPlayerEntity player, LivingEntity target, AttackType type, int damage);
    }

    /** Accuracy multiplier that makes an attack always hit. */
    static final double ALWAYS_HITS = 1000.0;

    private static final Map<LivingEntity, Rule> RULES = Collections.synchronizedMap(new WeakHashMap<>());
    private static final List<AttackWatcher> WATCHERS = Collections.synchronizedList(new ArrayList<>());
    private static boolean registered;

    private MinionRules() {
    }

    static void register() {
        if (registered) {
            return;
        }
        registered = true;
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            @Override
            public double accuracy(ServerPlayerEntity player, LivingEntity target, AttackType type) {
                Rule r = RULES.get(target);
                return r == null ? 1.0 : r.accuracy(player, target, type);
            }

            @Override
            public boolean alwaysMaxHit(ServerPlayerEntity player, LivingEntity target, AttackType type) {
                Rule r = RULES.get(target);
                return r != null && r.alwaysMaxHit(player, target, type);
            }

            @Override
            public int afterRoll(ServerPlayerEntity player, LivingEntity target, AttackType type, int damage) {
                Rule r = RULES.get(target);
                int d = r == null ? damage : Math.max(0, r.damage(player, target, type, damage));
                List<AttackWatcher> copy;
                synchronized (WATCHERS) {
                    copy = new ArrayList<>(WATCHERS);
                }
                for (AttackWatcher w : copy) {
                    w.onAttack(player, target, type, d);
                }
                return d;
            }
        });
    }

    static void set(LivingEntity minion, Rule rule) {
        if (minion != null) {
            RULES.put(minion, rule);
        }
    }

    static void watch(AttackWatcher watcher) {
        WATCHERS.add(watcher);
    }

    static void unwatch(AttackWatcher watcher) {
        WATCHERS.remove(watcher);
    }

    /** Damage that kills the minion outright. */
    static int lethal(LivingEntity minion) {
        return (int) Math.ceil(minion.getHealth()) + 1;
    }

    /** The spell id of a player's magic attack ("shadow_barrage", "water_blast"...), or null. */
    static String spell(ServerPlayerEntity p) {
        String auto = com.runecraft.content.systems.SysState.of(p).autocast;
        if (auto != null && !auto.isEmpty()) {
            return auto;
        }
        com.runecraft.osrs.Spell s = com.runecraft.player.Players.data(p).autocast();
        return s == null ? null : s.name().toLowerCase(java.util.Locale.ROOT);
    }
}
