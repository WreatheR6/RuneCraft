package com.runecraft.content.godwars;

import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.godwars.GwdPlaces;
import com.runecraft.osrs.content.godwars.GwdPlaces.Camp;
import com.runecraft.registry.ModEntities;
import com.runecraft.world.Gielinor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Heightmap;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Keeps the God Wars followers, the built-in generals' bodyguards and the revenants stocked around
 * their camps while a player is nearby (the real dungeon respawns its monsters constantly).
 */
final class GwdCamps {
    private static final int INTERVAL_MC_TICKS = 100;
    private static long ticks;

    private GwdCamps() {
    }

    static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks % INTERVAL_MC_TICKS != 0) {
                return;
            }
            ServerWorld world = Gielinor.world(server);
            if (world == null) {
                return;
            }
            for (Camp camp : GwdPlaces.camps()) {
                try {
                    tickCamp(world, camp);
                } catch (RuntimeException e) {
                    com.runecraft.RuneCraft.LOGGER.error("God Wars camp {} failed", camp.place(), e);
                }
            }
        });
    }

    private static int[] centre(Camp camp) {
        PlaceDef place = ContentRegistry.place(camp.place());
        if (place != null) {
            return new int[]{place.blockX() + camp.dx(), place.blockZ() + camp.dz()};
        }
        Landmark landmark = Landmark.byName(camp.place());
        if (landmark != null) {
            return new int[]{landmark.blockX() + camp.dx(), landmark.blockZ() + camp.dz()};
        }
        return null;
    }

    private static void tickCamp(ServerWorld world, Camp camp) {
        int[] c = centre(camp);
        if (c == null) {
            return;
        }
        int cx = c[0];
        int cz = c[1];
        List<ServerPlayerEntity> near = world.getPlayers(p -> p.isAlive() && !p.isSpectator()
                && Math.abs(p.getX() - cx) < 48 && Math.abs(p.getZ() - cz) < 48);
        if (near.isEmpty()) {
            return;
        }
        Set<EntityType<?>> types = new HashSet<>();
        for (String id : camp.creatures()) {
            EntityType<?> type = ModEntities.typeById(id);
            if (type != null) {
                types.add(type);
            }
        }
        if (types.isEmpty()) {
            return;
        }
        int y0 = near.get(0).getBlockY();
        Box area = new Box(cx - camp.radius() - 16, y0 - 48, cz - camp.radius() - 16, cx + camp.radius() + 16, y0 + 48,
                cz + camp.radius() + 16);
        int alive = world.getEntitiesByClass(MobEntity.class, area, e -> e.isAlive() && types.contains(e.getType())).size();
        if (alive >= camp.cap()) {
            return;
        }
        String id = camp.creatures().get(world.getRandom().nextInt(camp.creatures().size()));
        EntityType<?> type = ModEntities.typeById(id);
        if (type == null) {
            return;
        }
        int x = cx + world.getRandom().nextInt(camp.radius() * 2 + 1) - camp.radius();
        int z = cz + world.getRandom().nextInt(camp.radius() * 2 + 1) - camp.radius();
        for (ServerPlayerEntity p : near) {
            if (p.squaredDistanceTo(x, p.getY(), z) < 6 * 6) {
                return; // never spawn on top of a player
            }
        }
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        Entity e = type.create(world);
        if (!(e instanceof MobEntity mob)) {
            return;
        }
        BlockPos pos = new BlockPos(x, y, z);
        mob.refreshPositionAndAngles(x + 0.5, y, z + 0.5, world.getRandom().nextFloat() * 360f, 0);
        mob.initialize(world, world.getLocalDifficulty(pos), SpawnReason.SPAWNER, null, null);
        world.spawnEntity(mob);
    }
}
