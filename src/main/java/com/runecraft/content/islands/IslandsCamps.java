package com.runecraft.content.islands;

import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.islands.IslandsPlaces;
import com.runecraft.osrs.content.islands.IslandsPlaces.Camp;
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
 * Keeps the islands' creature camps stocked while a player is nearby: ammonite crabs, wyverns in the
 * Wyvern Cave, Marim's monkey guards and archers, the temple's monkey zombies, suqahs, yaks, ice
 * trolls, Waterbirth's dagannoth, wallasalki and rock lobsters, cave and jungle horrors, Crandor's
 * demons and giants, jogres, ogresses in the Corsair Cove Dungeon, mudskippers, the Lithkren Vault's
 * dragons, Braindeath Island's fever spiders and pirates and Zanaris' zygomites.
 */
final class IslandsCamps {
    private static final int INTERVAL_MC_TICKS = 100;
    private static long ticks;

    private IslandsCamps() {
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
            for (Camp camp : IslandsPlaces.camps()) {
                try {
                    tickCamp(world, camp);
                } catch (RuntimeException e) {
                    com.runecraft.RuneCraft.LOGGER.error("Islands camp {} failed", camp.place(), e);
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
        int reach = camp.radius() + 24;
        List<ServerPlayerEntity> near = world.getPlayers(p -> p.isAlive() && !p.isSpectator()
                && Math.abs(p.getX() - cx) < reach && Math.abs(p.getZ() - cz) < reach
                && (camp.surface() || Math.abs(p.getY() - camp.floorY()) < 12));
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
        int y0 = camp.surface() ? near.get(0).getBlockY() : camp.floorY();
        Box area = new Box(cx - camp.radius() - 16, y0 - 32, cz - camp.radius() - 16, cx + camp.radius() + 16, y0 + 32,
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
        int y;
        if (camp.surface()) {
            y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        } else {
            y = camp.floorY();
            BlockPos feet = new BlockPos(x, y, z);
            if (!world.getBlockState(feet).isAir() || !world.getBlockState(feet.up()).isAir()
                    || world.getBlockState(feet.down()).isAir()) {
                return; // the crypt hasn't been carved here (yet), or a pillar is in the way
            }
        }
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
