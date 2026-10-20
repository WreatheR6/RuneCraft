package com.runecraft.combat;

import com.runecraft.osrs.CombatBonuses;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * OSRS aggression tolerance: after 10 minutes in the same area, ordinary monsters stop attacking the
 * player on sight until they leave the area (bosses ignore tolerance).
 */
public final class Aggression {
    private static final Map<UUID, CombatBonuses.Tolerance> TOLERANCE = new ConcurrentHashMap<>();

    private Aggression() {
    }

    /** Called every game tick for each player. One OSRS tile is two blocks. */
    public static void gameTick(ServerPlayer player, long gameTick) {
        TOLERANCE.computeIfAbsent(player.getUUID(), u -> new CombatBonuses.Tolerance())
                .update(player.getBlockX() >> 1, player.getBlockZ() >> 1, gameTick);
    }

    /** True once ordinary monsters have become tolerant of the player where they stand. */
    public static boolean tolerant(ServerPlayer player, long gameTick) {
        CombatBonuses.Tolerance t = TOLERANCE.get(player.getUUID());
        return t != null && t.tolerant(gameTick);
    }

    public static void forget(UUID player) {
        TOLERANCE.remove(player);
    }

    /** Rules under which a monster leaves a player alone on sight (God Wars followers and their gods' items). */
    private static final java.util.List<java.util.function.BiPredicate<net.minecraft.world.entity.Mob, ServerPlayer>> PEACEFUL =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void addPeaceful(java.util.function.BiPredicate<net.minecraft.world.entity.Mob, ServerPlayer> rule) {
        PEACEFUL.add(rule);
    }

    /** True if a module's rule keeps the monster from attacking the player on sight (it still fights back). */
    public static boolean peaceful(net.minecraft.world.entity.Mob mob, ServerPlayer player) {
        for (java.util.function.BiPredicate<net.minecraft.world.entity.Mob, ServerPlayer> rule : PEACEFUL) {
            if (rule.test(mob, player)) {
                return true;
            }
        }
        return false;
    }
}
