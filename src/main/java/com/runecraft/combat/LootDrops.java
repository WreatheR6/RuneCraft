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
 * Monster drops as OSRS lays them (round 42): on the monster's tile, not flung about (zero velocity), and the winner's alone for 60
 * seconds, in the Wilderness too (an untradeable item stays theirs); a tradeable drop is gone 2 minutes after the kill, an
 * untradeable one after 3 (wiki, Drops). // approx: OSRS hides a private drop from other players; here they see it but can't pick
 * it up.
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
        if (stack.isEmpty() || !(dead.level() instanceof ServerLevel level)) {
            return;
        }
        Inv.splitStacks(stack, s -> {
            ItemEntity e = new ItemEntity(level, dead.getX(), dead.getY() + 0.05, dead.getZ(), s, 0, 0, 0);
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
            if (!level.addFreshEntity(e) && isPrivate) {
                synchronized (LootDrops.class) {
                    PENDING.remove(e.getUUID());
                }
            }
        });
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
