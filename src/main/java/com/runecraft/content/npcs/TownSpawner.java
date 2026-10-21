package com.runecraft.content.npcs;

import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.content.npcs.TownMonsters;
import com.runecraft.registry.ModEntities;
import com.runecraft.world.Gielinor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
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
        ServerLevel world = Gielinor.world(server);
        if (world == null || world.players().isEmpty()) {
            return;
        }
        for (int i = 0; i < SPAWNS.size(); i++) {
            try {
                check(world, i);
            } catch (RuntimeException e) {
                com.runecraft.RuneCraft.LOGGER.error("Town spawn {} failed", SPAWNS.get(i), e);
            }
        }
        for (ServerPlayer p : world.players()) {
            for (QuestNpcEntity e : world.getEntitiesOfClass(QuestNpcEntity.class, p.getBoundingBox().inflate(48), NpcPlacement::isMine)) {
                NpcPlacement.settle(world, e);
            }
        }
    }

    private static boolean playerNear(ServerLevel world, int x, int z) {
        for (ServerPlayer p : world.players()) {
            double dx = p.getX() - x;
            double dz = p.getZ() - z;
            if (dx * dx + dz * dz < PLAYER_RANGE * PLAYER_RANGE) {
                return true;
            }
        }
        return false;
    }

    private static void check(ServerLevel world, int index) {
        TownMonsters.TownSpawn s = SPAWNS.get(index);
        int cx = GielinorMap.toBlockX(s.x());
        int cz = GielinorMap.toBlockZ(s.y());
        if (!world.hasChunk(cx >> 4, cz >> 4) || !playerNear(world, cx, cz)) {
            return;
        }
        EntityType<?> type = ModEntities.typeById(s.creature());
        if (type == null) {
            return;
        }
        int r = s.radius() * GielinorMap.SCALE;
        AABB box = new AABB(cx - r - 8, world.getMinBuildHeight(), cz - r - 8, cx + r + 9, world.getMaxBuildHeight(), cz + r + 9);
        int alive = world.getEntitiesOfClass(Mob.class, box, e -> e.isAlive() && e.getType() == type).size();
        if (alive >= s.count()) {
            FILLED[index] = true;
            return;
        }
        if (FILLED[index] && ticks < NEXT[index]) {
            return;
        }
        int x = cx - r + world.getRandom().nextInt(2 * r + 1);
        int z = cz - r + world.getRandom().nextInt(2 * r + 1);
        if (!world.hasChunk(x >> 4, z >> 4)) {
            return;
        }
        Entity entity = type.create(world);
        if (!(entity instanceof Mob mob)) {
            return;
        }
        int y = NpcPlacement.standY(world, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        mob.moveTo(x + 0.5, y, z + 0.5, world.getRandom().nextFloat() * 360f, 0);
        // a world spawn (round 43): the resident gets its spawn point and the open-world rules (leash, 10-block hunt, combat-level
        // aggression), not an encounter monster's
        mob.finalizeSpawn(world, world.getCurrentDifficultyAt(pos), MobSpawnType.SPAWNER, null);
        mob.setPersistenceRequired();
        world.addFreshEntity(mob);
        NEXT[index] = ticks + RESPAWN_TICKS;
        if (alive + 1 >= s.count()) {
            FILLED[index] = true;
        }
    }
}
