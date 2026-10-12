package com.runecraft.combat;

import com.runecraft.osrs.CombatBonuses;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OSRS aggression tolerance: after 10 minutes in the same area, ordinary monsters stop attacking the
 * player on sight until they leave the area (bosses ignore tolerance).
 */
public final class Aggression {
    private static final Map<UUID, CombatBonuses.Tolerance> TOLERANCE = new ConcurrentHashMap<>();

    private Aggression() {
    }

    /** Called every game tick for each player. One OSRS tile is two blocks. */
    public static void gameTick(ServerPlayerEntity player, long gameTick) {
        TOLERANCE.computeIfAbsent(player.getUuid(), u -> new CombatBonuses.Tolerance())
                .update(player.getBlockX() >> 1, player.getBlockZ() >> 1, gameTick);
    }

    /** True once ordinary monsters have become tolerant of the player where they stand. */
    public static boolean tolerant(ServerPlayerEntity player, long gameTick) {
        CombatBonuses.Tolerance t = TOLERANCE.get(player.getUuid());
        return t != null && t.tolerant(gameTick);
    }

    public static void forget(UUID player) {
        TOLERANCE.remove(player);
    }
}
