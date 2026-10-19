package com.runecraft.content.godwars;

import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.godwars.GwdPlaces;
import com.runecraft.osrs.content.godwars.GwdPlaces.Camp;
import com.runecraft.registry.ModEntities;
import com.runecraft.world.Gielinor;
import com.runecraft.platform.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
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
            ServerLevel world = Gielinor.world(server);
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

    private static void tickCamp(ServerLevel world, Camp camp) {
        int[] c = centre(camp);
        if (c == null) {
            return;
        }
        int cx = c[0];
        int cz = c[1];
        List<ServerPlayer> near = world.getPlayers(p -> p.isAlive() && !p.isSpectator()
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
        AABB area = new AABB(cx - camp.radius() - 16, y0 - 48, cz - camp.radius() - 16, cx + camp.radius() + 16, y0 + 48,
                cz + camp.radius() + 16);
        int alive = world.getEntitiesOfClass(Mob.class, area, e -> e.isAlive() && types.contains(e.getType())).size();
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
        for (ServerPlayer p : near) {
            if (p.distanceToSqr(x, p.getY(), z) < 6 * 6) {
                return; // never spawn on top of a player
            }
        }
        int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        Entity e = type.create(world);
        if (!(e instanceof Mob mob)) {
            return;
        }
        BlockPos pos = new BlockPos(x, y, z);
        mob.moveTo(x + 0.5, y, z + 0.5, world.getRandom().nextFloat() * 360f, 0);
        mob.finalizeSpawn(world, world.getCurrentDifficultyAt(pos), MobSpawnType.SPAWNER, null);
        world.addFreshEntity(mob);
    }
}
