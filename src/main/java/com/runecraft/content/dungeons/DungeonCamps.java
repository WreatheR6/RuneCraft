package com.runecraft.content.dungeons;

import com.runecraft.osrs.content.dungeons.Dungeons;
import com.runecraft.osrs.content.dungeons.Dungeons.Camp;
import com.runecraft.registry.ModEntities;
import com.runecraft.world.Gielinor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Keeps each dungeon camp stocked while a player is close (within 32 blocks of the camp's box and
 * near its floor, so nothing ever appears on the surface above). When a player arrives the camp is
 * filled; after that every monster that dies comes back after the camp's OSRS respawn time.
 */
final class DungeonCamps {
    private static final int INTERVAL_MC_TICKS = 20;
    private static final int MC_TICKS_PER_GAME_TICK = 12;
    private static final int NEAR = 32;
    private static final int NEAR_Y = 16;
    private static long ticks;
    private static final List<State> STATES = new ArrayList<>();

    /** Runtime state of one camp. */
    private static final class State {
        final Camp camp;
        final Set<EntityType<?>> types = new HashSet<>();
        final Deque<Long> pending = new ArrayDeque<>();
        boolean active;

        State(Camp camp) {
            this.camp = camp;
        }
    }

    private DungeonCamps() {
    }

    static void register() {
        for (Camp camp : Dungeons.camps()) {
            STATES.add(new State(camp));
        }
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks % INTERVAL_MC_TICKS != 0) {
                return;
            }
            ServerWorld world = Gielinor.world(server);
            if (world == null) {
                return;
            }
            for (State state : STATES) {
                try {
                    tick(world, state);
                } catch (RuntimeException e) {
                    com.runecraft.RuneCraft.LOGGER.error("Dungeon camp {} failed", state.camp.dungeon(), e);
                }
            }
        });
    }

    private static boolean playerNear(ServerWorld world, Camp camp) {
        for (ServerPlayerEntity p : world.getPlayers()) {
            if (!p.isAlive() || p.isSpectator()) {
                continue;
            }
            double dx = Math.max(0, Math.max(camp.x1() - p.getX(), p.getX() - camp.x2()));
            double dz = Math.max(0, Math.max(camp.z1() - p.getZ(), p.getZ() - camp.z2()));
            if (dx < NEAR && dz < NEAR && Math.abs(p.getY() - camp.feetY()) < NEAR_Y) {
                return true;
            }
        }
        return false;
    }

    private static void tick(ServerWorld world, State state) {
        Camp camp = state.camp;
        if (!playerNear(world, camp)) {
            state.active = false;
            state.pending.clear();
            return;
        }
        if (state.types.isEmpty()) {
            for (String id : camp.creatures()) {
                EntityType<?> type = ModEntities.typeById(id);
                if (type != null) {
                    state.types.add(type);
                }
            }
            if (state.types.isEmpty()) {
                return;
            }
        }
        Box area = new Box(camp.x1() - 6, camp.feetY() - 3, camp.z1() - 6, camp.x2() + 7, camp.feetY() + 12, camp.z2() + 7);
        int alive = world.getEntitiesByClass(MobEntity.class, area, e -> e.isAlive() && state.types.contains(e.getType())).size();
        int missing = camp.cap() - alive;
        if (!state.active) {
            // A player just arrived: the camp is fully stocked, as the dungeon always is in OSRS.
            state.active = true;
            for (int i = 0; i < missing; i++) {
                spawn(world, camp);
            }
            return;
        }
        long now = world.getTime();
        while (state.pending.size() > Math.max(0, missing)) {
            state.pending.pollLast();
        }
        while (state.pending.size() < missing) {
            state.pending.addLast(now + (long) camp.respawnTicks() * MC_TICKS_PER_GAME_TICK);
        }
        while (!state.pending.isEmpty() && state.pending.peekFirst() <= now) {
            state.pending.pollFirst();
            spawn(world, camp);
        }
    }

    private static boolean open(BlockState state) {
        return state.isAir() || state.isReplaceable() && state.getFluidState().isEmpty();
    }

    private static void spawn(ServerWorld world, Camp camp) {
        for (int attempt = 0; attempt < 8; attempt++) {
            int x = camp.x1() + world.getRandom().nextInt(camp.x2() - camp.x1() + 1);
            int z = camp.z1() + world.getRandom().nextInt(camp.z2() - camp.z1() + 1);
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
            int y = camp.feetY();
            BlockPos feet = new BlockPos(x, y, z);
            BlockState below = world.getBlockState(feet.down());
            if (!open(world.getBlockState(feet)) || !open(world.getBlockState(feet.up())) || below.isAir()
                    || below.getFluidState().isOf(Fluids.LAVA) || below.getFluidState().isOf(Fluids.FLOWING_LAVA)) {
                continue; // a pillar, a pool of lava, or the dungeon hasn't been carved here yet
            }
            boolean crowded = false;
            for (ServerPlayerEntity p : world.getPlayers()) {
                if (p.squaredDistanceTo(x + 0.5, y, z + 0.5) < 5 * 5) {
                    crowded = true;
                    break;
                }
            }
            if (crowded) {
                continue; // never spawn on top of a player
            }
            String id = camp.creatures().get(world.getRandom().nextInt(camp.creatures().size()));
            EntityType<?> type = ModEntities.typeById(id);
            if (type == null) {
                return;
            }
            Entity e = type.create(world);
            if (!(e instanceof MobEntity mob)) {
                return;
            }
            mob.refreshPositionAndAngles(x + 0.5, y, z + 0.5, world.getRandom().nextFloat() * 360f, 0);
            mob.initialize(world, world.getLocalDifficulty(feet), SpawnReason.SPAWNER, null, null);
            mob.setPersistent();
            world.spawnEntity(mob);
            return;
        }
    }
}
