package com.runecraft.world;

import com.runecraft.osrs.world.SpawnPoint;
import com.runecraft.osrs.world.SpawnTable;
import com.runecraft.registry.ModEntities;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spawns monsters only at their OSRS locations ({@link SpawnTable}). Each point keeps up to its
 * count of monsters alive within its radius while a player is within about 48 blocks and the chunk
 * is loaded: the first time a player arrives the point is fully stocked, after that a killed
 * monster comes back after its OSRS respawn time. Monsters are walked (or, if far, teleported) back
 * when they wander more than twice the radius away.
 *
 * <p>Spawned mobs are persistent and carry the point's key as a command tag, so they are counted
 * again after a reload instead of piling up; mobs of points that no longer exist are removed.
 *
 * <p>Also blocks all other vanilla mob spawning in Gielinor (the biomes have no spawners, and this
 * catches anything else: phantoms, patrols, traders, sieges, iron golems, breeding...).
 */
public final class LocationSpawner {
    /** Command tag on every mob a spawn point made. */
    public static final String TAG = "rc_sp";
    private static final String KEY_PREFIX = "rc_sp:";
    private static final int INTERVAL_MC_TICKS = 20;
    private static final int MC_TICKS_PER_GAME_TICK = 12;
    private static final int PLAYER_RANGE = 48;
    private static final int PLAYER_RANGE_Y = 16;
    /** Players below this height are in a dungeon: surface points above them stay idle. */
    private static final int SURFACE_PLAYER_MIN_Y = 40;
    private static final List<State> STATES = new ArrayList<>();
    private static final Map<String, State> BY_TAG = new HashMap<>();
    private static final List<Entity> DISCARD = new ArrayList<>();
    private static long ticks;

    /** Runtime state of one spawn point. */
    private static final class State {
        final SpawnPoint point;
        final String tag;
        final Deque<Long> pending = new ArrayDeque<>();
        EntityType<?> type;
        boolean resolved;
        boolean active;

        State(SpawnPoint point) {
            this.point = point;
            this.tag = KEY_PREFIX + point.key();
        }
    }

    private LocationSpawner() {
    }

    public static void register() {
        for (SpawnPoint p : SpawnTable.all()) {
            State state = new State(p);
            STATES.add(state);
            BY_TAG.put(state.tag, state);
        }
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (Gielinor.is(world) && entity instanceof Mob mob && shouldRemove(mob)) {
                synchronized (DISCARD) {
                    DISCARD.add(mob); // removed on the next tick, not while the world is adding it
                }
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(LocationSpawner::tick);
    }

    // ------------------------------------------------------------------ blocking natural spawns

    /**
     * True for a mob that must not exist in Gielinor: one made by a spawn point that is no longer in
     * the table, or a vanilla mob that came from natural spawning (anything vanilla that isn't
     * persistent, named, tamed, leashed, already fighting, or made by one of our spawners).
     */
    private static boolean shouldRemove(Mob mob) {
        boolean ours = false;
        for (String tag : mob.getTags()) {
            if (tag.startsWith(KEY_PREFIX)) {
                if (BY_TAG.containsKey(tag)) {
                    return false;
                }
            } else if (tag.equals(TAG)) {
                ours = true;
            } else if (tag.startsWith("runecraft_owner:")) {
                return false;
            }
        }
        if (ours) {
            return true; // its spawn point was removed from the table
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (!"minecraft".equals(id.getNamespace())) {
            return false; // RuneCraft creatures only come from our own spawners and scripts
        }
        if (mob.isPersistenceRequired() || mob.hasCustomName() || mob.isLeashed() || mob.getTarget() != null) {
            return false; // a camp's, a script's or a player's mob
        }
        return !(mob instanceof TamableAnimal tame && tame.isTame());
    }

    // ------------------------------------------------------------------ spawn points

    private static void tick(MinecraftServer server) {
        List<Entity> remove;
        synchronized (DISCARD) {
            remove = DISCARD.isEmpty() ? List.of() : new ArrayList<>(DISCARD);
            DISCARD.clear();
        }
        for (Entity e : remove) {
            if (!e.isRemoved()) {
                e.discard();
            }
        }
        if (++ticks % INTERVAL_MC_TICKS != 0) {
            return;
        }
        ServerLevel world = Gielinor.world(server);
        if (world == null || world.players().isEmpty()) {
            return;
        }
        for (State state : STATES) {
            try {
                tickPoint(world, state);
            } catch (RuntimeException e) {
                com.runecraft.RuneCraft.LOGGER.error("Spawn point {} failed", state.point.key(), e);
            }
        }
    }

    private static boolean playerNear(ServerLevel world, SpawnPoint p) {
        double cx = p.blockX() + 0.5;
        double cz = p.blockZ() + 0.5;
        double range = PLAYER_RANGE + p.radiusBlocks();
        for (ServerPlayer player : world.players()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            double dx = player.getX() - cx;
            double dz = player.getZ() - cz;
            if (dx * dx + dz * dz >= range * range) {
                continue;
            }
            if (p.surface() ? player.getY() >= SURFACE_PLAYER_MIN_Y : Math.abs(player.getY() - p.floorY()) < PLAYER_RANGE_Y) {
                return true;
            }
        }
        return false;
    }

    private static void tickPoint(ServerLevel world, State state) {
        SpawnPoint p = state.point;
        int cx = p.blockX();
        int cz = p.blockZ();
        if (!playerNear(world, p)) {
            state.active = false;
            state.pending.clear();
            return;
        }
        if (!world.hasChunk(cx >> 4, cz >> 4)) {
            return;
        }
        if (!state.resolved) {
            state.type = ModEntities.typeById(p.monster());
            state.resolved = true;
            if (state.type == null) {
                com.runecraft.RuneCraft.LOGGER.warn("Spawn point {}: unknown creature", p.key());
            }
        }
        if (state.type == null) {
            return;
        }
        int r = p.radiusBlocks();
        int leash = Math.max(8, 2 * r);
        int reach = 2 * leash + 32;
        AABB area = p.surface()
                ? new AABB(cx - reach, world.getMinBuildHeight(), cz - reach, cx + reach + 1, world.getMaxBuildHeight(), cz + reach + 1)
                : new AABB(cx - reach, p.floorY() - PLAYER_RANGE_Y, cz - reach, cx + reach + 1, p.floorY() + PLAYER_RANGE_Y,
                cz + reach + 1);
        List<Mob> mine = new ArrayList<>(world.getEntitiesOfClass(Mob.class, area,
                e -> e.isAlive() && e.getTags().contains(state.tag)));
        if (mine.size() > p.count()) {
            // Some came back from an unloaded chunk after replacements were made: keep the nearest.
            mine.sort(Comparator.comparingDouble((Mob e) -> e.distanceToSqr(cx + 0.5, e.getY(), cz + 0.5)));
            for (int i = p.count(); i < mine.size(); i++) {
                mine.get(i).discard();
            }
            mine = mine.subList(0, p.count());
        }
        for (Mob mob : mine) {
            leash(world, p, mob, leash);
        }
        int missing = p.count() - mine.size();
        if (!state.active) {
            // A player just arrived: the spot is fully stocked, as it always is in OSRS.
            state.active = true;
            state.pending.clear();
            for (int i = 0; i < missing; i++) {
                spawn(world, state);
            }
            return;
        }
        long now = world.getGameTime();
        while (state.pending.size() > Math.max(0, missing)) {
            state.pending.pollLast();
        }
        while (state.pending.size() < missing) {
            state.pending.addLast(now + (long) p.respawnTicks() * MC_TICKS_PER_GAME_TICK);
        }
        while (!state.pending.isEmpty() && state.pending.peekFirst() <= now) {
            state.pending.pollFirst();
            spawn(world, state);
        }
    }

    /** Walks a wandering monster home, or teleports it back when it is far away (dropping its target). */
    private static void leash(ServerLevel world, SpawnPoint p, Mob mob, int leash) {
        double hx = p.blockX() + 0.5;
        double hz = p.blockZ() + 0.5;
        double dx = mob.getX() - hx;
        double dz = mob.getZ() - hz;
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d <= leash) {
            return;
        }
        if (d <= leash + 16 && mob.getTarget() == null) {
            mob.getNavigation().moveTo(hx, mob.getY(), hz, 1.0);
            return;
        }
        if (d > leash + 16) {
            BlockPos home = findSpot(world, p);
            if (home != null) {
                mob.setTarget(null);
                mob.getNavigation().stop();
                mob.teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
            }
        }
    }

    private static boolean open(BlockState state) {
        return state.isAir() || state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    /** A random free standing spot within the point's radius, or null after a few tries. */
    private static BlockPos findSpot(ServerLevel world, SpawnPoint p) {
        int r = p.radiusBlocks();
        for (int attempt = 0; attempt < 8; attempt++) {
            int x = p.blockX() - r + world.getRandom().nextInt(2 * r + 1);
            int z = p.blockZ() - r + world.getRandom().nextInt(2 * r + 1);
            if (!world.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            int y = p.surface() ? world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) : p.floorY();
            BlockPos feet = new BlockPos(x, y, z);
            BlockState below = world.getBlockState(feet.below());
            if (!open(world.getBlockState(feet)) || !open(world.getBlockState(feet.above())) || below.isAir()
                    || !below.getFluidState().isEmpty()) {
                continue; // water, lava, a wall, or the dungeon/building isn't there (yet)
            }
            boolean crowded = false;
            for (ServerPlayer player : world.players()) {
                if (player.distanceToSqr(x + 0.5, y, z + 0.5) < 5 * 5) {
                    crowded = true;
                    break;
                }
            }
            if (!crowded) {
                return feet; // never on top of a player
            }
        }
        return null;
    }

    private static void spawn(ServerLevel world, State state) {
        BlockPos pos = findSpot(world, state.point);
        if (pos == null) {
            return;
        }
        Entity e = state.type.create(world);
        if (!(e instanceof Mob mob)) {
            return;
        }
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, world.getRandom().nextFloat() * 360f, 0);
        mob.finalizeSpawn(world, world.getCurrentDifficultyAt(pos), MobSpawnType.SPAWNER, null, null);
        if (mob instanceof Zombie zombie) {
            zombie.setBaby(false); // OSRS zombies are all grown up
        }
        mob.setPersistenceRequired();
        mob.addTag(TAG);
        mob.addTag(state.tag);
        world.addFreshEntity(mob);
    }
}
