package com.runecraft.content.npcs;

import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.content.npcs.TownMonsters;
import com.runecraft.registry.ModEntities;
import com.runecraft.world.Gielinor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

/**
 * Keeps each town's attackable residents (men, women, guards, knights, paladins, gnomes, TzHaar...)
 * populated around their OSRS spots while a player is nearby, respawning killed ones one at a time
 * like OSRS NPC respawns. Also re-settles this module's NPCs onto their floors (a building may have
 * been placed around them after they spawned).
 */
final class TownSpawner {
    private static final int CHECK_TICKS = 100;
    /** Delay before a missing resident is replaced (about 30 seconds, OSRS-like). */
    private static final int RESPAWN_TICKS = 600;
    private static final double PLAYER_RANGE = 64.0;
    private static final List<TownMonsters.TownSpawn> SPAWNS = TownMonsters.spawns();
    private static final long[] NEXT = new long[SPAWNS.size()];
    private static final boolean[] FILLED = new boolean[SPAWNS.size()];
    private static long ticks;

    private TownSpawner() {
    }

    static void tick(MinecraftServer server) {
        if (++ticks % CHECK_TICKS != 0) {
            return;
        }
        ServerWorld world = Gielinor.world(server);
        if (world == null || world.getPlayers().isEmpty()) {
            return;
        }
        for (int i = 0; i < SPAWNS.size(); i++) {
            try {
                check(world, i);
            } catch (RuntimeException e) {
                com.runecraft.RuneCraft.LOGGER.error("Town spawn {} failed", SPAWNS.get(i), e);
            }
        }
        for (ServerPlayerEntity p : world.getPlayers()) {
            for (QuestNpcEntity e : world.getEntitiesByClass(QuestNpcEntity.class, p.getBoundingBox().expand(48), NpcPlacement::isMine)) {
                NpcPlacement.settle(world, e);
            }
        }
    }

    private static boolean playerNear(ServerWorld world, int x, int z) {
        for (ServerPlayerEntity p : world.getPlayers()) {
            double dx = p.getX() - x;
            double dz = p.getZ() - z;
            if (dx * dx + dz * dz < PLAYER_RANGE * PLAYER_RANGE) {
                return true;
            }
        }
        return false;
    }

    private static void check(ServerWorld world, int index) {
        TownMonsters.TownSpawn s = SPAWNS.get(index);
        int cx = GielinorMap.toBlockX(s.x());
        int cz = GielinorMap.toBlockZ(s.y());
        if (!world.isChunkLoaded(cx >> 4, cz >> 4) || !playerNear(world, cx, cz)) {
            return;
        }
        EntityType<?> type = ModEntities.typeById(s.creature());
        if (type == null) {
            return;
        }
        int r = s.radius() * GielinorMap.SCALE;
        Box box = new Box(cx - r - 8, world.getBottomY(), cz - r - 8, cx + r + 9, world.getTopY(), cz + r + 9);
        int alive = world.getEntitiesByClass(MobEntity.class, box, e -> e.isAlive() && e.getType() == type).size();
        if (alive >= s.count()) {
            FILLED[index] = true;
            return;
        }
        if (FILLED[index] && ticks < NEXT[index]) {
            return;
        }
        int x = cx - r + world.getRandom().nextInt(2 * r + 1);
        int z = cz - r + world.getRandom().nextInt(2 * r + 1);
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return;
        }
        Entity entity = type.create(world);
        if (!(entity instanceof MobEntity mob)) {
            return;
        }
        int y = NpcPlacement.standY(world, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        mob.refreshPositionAndAngles(x + 0.5, y, z + 0.5, world.getRandom().nextFloat() * 360f, 0);
        mob.initialize(world, world.getLocalDifficulty(pos), SpawnReason.EVENT, null, null);
        mob.setPersistent();
        world.spawnEntity(mob);
        NEXT[index] = ticks + RESPAWN_TICKS;
        if (alive + 1 >= s.count()) {
            FILLED[index] = true;
        }
    }
}
