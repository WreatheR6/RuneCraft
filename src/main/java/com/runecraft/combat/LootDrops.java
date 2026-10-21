package com.runecraft.combat;

import com.runecraft.osrs.MonsterAi;
import com.runecraft.util.Inv;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Monster drops as OSRS lays them (round 42): on the monster's tile (a monster bigger than a tile: its south-westernmost one,
 * round 43), not flung about (zero velocity), and the winner's alone for 60 seconds, in the Wilderness too (an untradeable item
 * stays theirs); a tradeable drop is gone 2 minutes after the kill, an untradeable one after 3 (wiki, Drops). Every path that puts
 * a kill's loot on the ground goes through here (round 43: boss tables, revenants, clue scrolls and keys, quest drops, champion
 * scrolls, the looting bag, Slayer keys, God Wars key pieces). // approx: OSRS hides a private drop from other players; here they
 * see it but can't pick it up.
 */
public final class LootDrops {
    /** Tag on a drop that is still private to its winner. */
    public static final String PRIVATE_TAG = "rc_private_loot";
    /** Private tradeable drops: entity UUID -> (level, game time the drop goes public). */
    private static final Map<UUID, Release> PENDING = new HashMap<>();

    private record Release(ResourceKey<Level> level, long at) {
    }

    private LootDrops() {
    }

    /** Hooks the world: called once from {@code LocationSpawner.register}. */
    public static void register() {
        com.runecraft.platform.ServerTickEvents.END_SERVER_TICK.register(LootDrops::tick);
        com.runecraft.platform.ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            // a private drop reloaded after a restart (or its release time passed while its chunk was unloaded) goes public
            if (entity instanceof ItemEntity item && item.getTags().contains(PRIVATE_TAG) && !pending(item.getUUID())
                    && !com.runecraft.osrs.data.Untradeables.untradeable(Inv.idOf(item.getItem()))) {
                item.setTarget(null);
                item.removeTag(PRIVATE_TAG);
            }
        });
    }

    private static synchronized boolean pending(UUID id) {
        return PENDING.containsKey(id);
    }

    /** Lays {@code stack} where {@code dead} fell, private to {@code winner} (null: everyone's). */
    public static void spawn(LivingEntity dead, ItemStack stack, ServerPlayer winner) {
        spawn(dead, stack, winner, null);
    }

    /**
     * Lays {@code stack} where {@code at} stands (the monster that died, or the player a boss's loot goes under), private to
     * {@code winner} (null: everyone's); {@code spawned} (nullable) is handed each item entity that joined the world.
     */
    public static void spawn(Entity at, ItemStack stack, ServerPlayer winner, java.util.function.Consumer<ItemEntity> spawned) {
        if (at.level() instanceof ServerLevel level) {
            spawnAt(level, dropSpot(at), stack, winner, spawned);
        }
    }

    /** {@link #spawn(Entity, ItemStack, ServerPlayer, java.util.function.Consumer)} at a spot (a corpse harvested after the kill). */
    public static void spawnAt(ServerLevel level, net.minecraft.world.phys.Vec3 spot, ItemStack stack, ServerPlayer winner,
            java.util.function.Consumer<ItemEntity> spawned) {
        if (stack.isEmpty()) {
            return;
        }
        Inv.splitStacks(stack, s -> {
            ItemEntity e = new ItemEntity(level, spot.x, spot.y + 0.05, spot.z, s, 0, 0, 0);
            e.setDefaultPickUpDelay();
            boolean tradeable = !com.runecraft.osrs.data.Untradeables.untradeable(Inv.idOf(s));
            e.lifespan = MonsterAi.lootLifespanMc(tradeable);
            boolean isPrivate = winner != null && MonsterAi.lootPrivate(0, tradeable);
            if (isPrivate) {
                e.setTarget(winner.getUUID());
                e.addTag(PRIVATE_TAG);
                if (tradeable) {
                    // listed before it joins the world: the load hook below releases private drops it doesn't know
                    synchronized (LootDrops.class) {
                        PENDING.put(e.getUUID(), new Release(level.dimension(), level.getGameTime() + MonsterAi.PRIVATE_LOOT_MC));
                    }
                }
            }
            if (level.addFreshEntity(e)) {
                if (spawned != null) {
                    spawned.accept(e);
                }
            } else if (isPrivate) {
                synchronized (LootDrops.class) {
                    PENDING.remove(e.getUUID());
                }
            }
        });
    }

    /**
     * Where a drop lands: the monster's tile, or the south-westernmost tile of one bigger than a tile (wiki, Drops: "Large monsters
     * (that take up more than one square) will always drop their drops in the south-westernmost square"), unless a block fills
     * that spot. A player (loot under the killer) gets it at their feet.
     */
    static net.minecraft.world.phys.Vec3 dropSpot(Entity at) {
        if (at instanceof net.minecraft.world.entity.player.Player || !MonsterAi.dropsInSouthWestTile(at.getBbWidth())) {
            return at.position();
        }
        double[] xz = MonsterAi.southWestTileCentre(at.getBoundingBox().minX, at.getBoundingBox().maxZ);
        net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(xz[0], at.getY() + 0.05, xz[1]);
        if (!at.level().getBlockState(pos).getCollisionShape(at.level(), pos).isEmpty()) {
            return at.position();
        }
        return new net.minecraft.world.phys.Vec3(xz[0], at.getY(), xz[1]);
    }

    /** Releases private tradeable drops whose 60 seconds are up. */
    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 == 0) {
            releaseDue(server, 0);
        }
    }

    /** Releases the private drops due within {@code earlyMc} ticks from now. */
    private static void releaseDue(MinecraftServer server, long earlyMc) {
        java.util.List<Entity> due = new java.util.ArrayList<>();
        synchronized (LootDrops.class) {
            for (Iterator<Map.Entry<UUID, Release>> it = PENDING.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<UUID, Release> en = it.next();
                ServerLevel level = server.getLevel(en.getValue().level());
                if (level == null) {
                    it.remove();
                } else if (level.getGameTime() + earlyMc >= en.getValue().at()) {
                    it.remove();
                    Entity e = level.getEntity(en.getKey());
                    if (e != null) {
                        due.add(e);
                    }
                }
            }
        }
        due.forEach(LootDrops::release);
    }

    /** Makes a private drop everyone's now. */
    public static void release(Entity e) {
        if (e instanceof ItemEntity item && item.isAlive()) {
            item.setTarget(null);
            item.removeTag(PRIVATE_TAG);
        }
    }

    /** Self-test hook: releases the private drops due within {@code mc} ticks, as if that time had passed. */
    public static void advanceForSelfTest(MinecraftServer server, int mc) {
        releaseDue(server, mc);
    }
}
