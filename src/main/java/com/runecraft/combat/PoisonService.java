package com.runecraft.combat;

import com.runecraft.network.Net;
import com.runecraft.network.ServerNetworking;
import com.runecraft.util.Msg;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OSRS poison and venom. Poison hits every 30 game ticks (18s), starting at its strength and
 * dropping by 1 every 5 hits. Venom starts at 6 and rises by 2 per hit up to 20.
 */
public final class PoisonService {
    private record State(int damage, boolean venom, int hits, long nextTick) {
    }

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> IMMUNE_UNTIL = new ConcurrentHashMap<>();

    private PoisonService() {
    }

    public static void poison(LivingEntity target, int damage, long gameTick) {
        if (IMMUNE_UNTIL.getOrDefault(target.getUuid(), 0L) > gameTick) {
            return;
        }
        State current = STATES.get(target.getUuid());
        if (current != null && (current.venom() || current.damage() >= damage)) {
            return;
        }
        STATES.put(target.getUuid(), new State(damage, false, 0, gameTick + 30));
        if (target instanceof ServerPlayerEntity p) {
            p.sendMessage(Text.literal("You have been poisoned!").formatted(Formatting.DARK_GREEN), false);
        }
    }

    public static void venom(LivingEntity target, long gameTick) {
        if (IMMUNE_UNTIL.getOrDefault(target.getUuid(), 0L) > gameTick) {
            return;
        }
        State current = STATES.get(target.getUuid());
        if (current != null && current.venom()) {
            return;
        }
        STATES.put(target.getUuid(), new State(6, true, 0, gameTick + 30));
        if (target instanceof ServerPlayerEntity p) {
            p.sendMessage(Text.literal("You have been envenomed!").formatted(Formatting.DARK_GREEN), false);
        }
    }

    /** Cures poison (and venom if {@code venomToo}) and grants immunity for the given game ticks. */
    public static void cure(LivingEntity target, boolean venomToo, int immunityTicks, long gameTick) {
        State current = STATES.get(target.getUuid());
        if (current != null && (!current.venom() || venomToo)) {
            STATES.remove(target.getUuid());
        }
        if (immunityTicks > 0) {
            IMMUNE_UNTIL.put(target.getUuid(), gameTick + immunityTicks);
        }
    }

    public static boolean isPoisoned(LivingEntity target) {
        return STATES.containsKey(target.getUuid());
    }

    public static boolean isEnvenomed(LivingEntity target) {
        State s = STATES.get(target.getUuid());
        return s != null && s.venom();
    }

    /** Called once per game tick for each player (and for module mobs from their tick). */
    public static void tick(LivingEntity entity, long gameTick) {
        State s = STATES.get(entity.getUuid());
        if (s == null || gameTick < s.nextTick()) {
            return;
        }
        if (!entity.isAlive()) {
            STATES.remove(entity.getUuid());
            return;
        }
        int damage = s.damage();
        CombatHandler.applyDamage(null, entity, damage);
        ServerNetworking.hitsplat(entity, damage, Net.SPLAT_POISON);
        int hits = s.hits() + 1;
        if (s.venom()) {
            STATES.put(entity.getUuid(), new State(Math.min(20, damage + 2), true, hits, gameTick + 30));
        } else {
            int next = hits % 5 == 0 ? damage - 1 : damage;
            if (next <= 0) {
                STATES.remove(entity.getUuid());
                if (entity instanceof ServerPlayerEntity p) {
                    Msg.game(p, "The poison has worn off.");
                }
            } else {
                STATES.put(entity.getUuid(), new State(next, false, hits, gameTick + 30));
            }
        }
    }
}
