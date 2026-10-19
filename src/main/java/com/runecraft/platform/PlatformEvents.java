package com.runecraft.platform;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fires the Fabric-shaped events in this package from NeoForge's events. Called once from the mod constructor;
 * registration order inside each event is the order the mod's hooks registered, as on Fabric.
 */
public final class PlatformEvents {
    private PlatformEvents() {
    }

    /** Old player objects between {@code PlayerEvent.Clone} and {@code PlayerRespawnEvent}, by UUID. */
    private static final Map<UUID, Respawn> RESPAWNS = new ConcurrentHashMap<>();

    private record Respawn(ServerPlayer oldPlayer, boolean alive) {
    }

    public static void init(IEventBus modBus) {
        IEventBus bus = NeoForge.EVENT_BUS;
        PlatformNetworking.init(modBus);

        // ticks and lifecycle
        bus.addListener(ServerTickEvent.Pre.class, e -> ServerTickEvents.START_SERVER_TICK.invoker().onStartTick(e.getServer()));
        bus.addListener(ServerTickEvent.Post.class, e -> ServerTickEvents.END_SERVER_TICK.invoker().onEndTick(e.getServer()));
        bus.addListener(LevelTickEvent.Post.class, e -> {
            if (e.getLevel() instanceof ServerLevel world) {
                ServerTickEvents.END_WORLD_TICK.invoker().onEndTick(world);
            }
        });
        bus.addListener(ServerStartedEvent.class, e -> ServerLifecycleEvents.SERVER_STARTED.invoker().onServerStarted(e.getServer()));
        bus.addListener(ServerStoppingEvent.class, e -> ServerLifecycleEvents.SERVER_STOPPING.invoker().onServerStopping(e.getServer()));
        bus.addListener(RegisterCommandsEvent.class, e ->
                CommandRegistrationCallback.EVENT.invoker().register(e.getDispatcher(), e.getBuildContext(), e.getCommandSelection()));

        // players joining, leaving, respawning and changing dimension
        bus.addListener(PlayerEvent.PlayerLoggedInEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                ServerPlayConnectionEvents.JOIN.invoker().onPlayReady(player.connection, new PacketSender(player), player.server);
            }
        });
        bus.addListener(PlayerEvent.PlayerLoggedOutEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                ServerPlayConnectionEvents.DISCONNECT.invoker().onPlayDisconnect(player.connection, player.server);
            }
        });
        bus.addListener(PlayerEvent.Clone.class, e -> {
            if (e.getOriginal() instanceof ServerPlayer old && e.getEntity() instanceof ServerPlayer now) {
                RESPAWNS.put(now.getUUID(), new Respawn(old, !e.isWasDeath()));
            }
        });
        bus.addListener(PlayerEvent.PlayerRespawnEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer now) {
                Respawn r = RESPAWNS.remove(now.getUUID());
                ServerPlayerEvents.AFTER_RESPAWN.invoker().afterRespawn(r == null ? now : r.oldPlayer(), now,
                        r == null ? e.isEndConquered() : r.alive());
            }
        });
        bus.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                ServerLevel from = player.server.getLevel(e.getFrom());
                ServerLevel to = player.server.getLevel(e.getTo());
                if (from != null && to != null) {
                    ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.invoker().afterChangeWorld(player, from, to);
                }
            }
        });

        // damage and death
        bus.addListener(LivingIncomingDamageEvent.class, e -> {
            if (!e.getEntity().level().isClientSide()
                    && !ServerLivingEntityEvents.ALLOW_DAMAGE.invoker().allowDamage(e.getEntity(), e.getSource(), e.getAmount())) {
                e.setCanceled(true);
            }
        });
        bus.addListener(EventPriority.HIGHEST, false, LivingDeathEvent.class, e -> {
            if (!e.getEntity().level().isClientSide()
                    && !ServerLivingEntityEvents.ALLOW_DEATH.invoker().allowDeath(e.getEntity(), e.getSource(), 0f)) {
                e.setCanceled(true);
            }
        });
        bus.addListener(EventPriority.LOWEST, false, LivingDeathEvent.class, e -> {
            if (!e.getEntity().level().isClientSide()) {
                ServerLivingEntityEvents.AFTER_DEATH.invoker().afterDeath(e.getEntity(), e.getSource());
            }
        });

        // chunks and entities
        bus.addListener(ChunkEvent.Load.class, e -> {
            if (e.getLevel() instanceof ServerLevel world && e.getChunk() instanceof LevelChunk chunk) {
                ServerChunkEvents.CHUNK_LOAD.invoker().onChunkLoad(world, chunk);
            }
        });
        bus.addListener(ChunkEvent.Unload.class, e -> {
            if (e.getLevel() instanceof ServerLevel world && e.getChunk() instanceof LevelChunk chunk) {
                ServerChunkEvents.CHUNK_UNLOAD.invoker().onChunkUnload(world, chunk);
            }
        });
        bus.addListener(EntityJoinLevelEvent.class, e -> {
            if (e.getLevel() instanceof ServerLevel world) {
                ServerEntityEvents.ENTITY_LOAD.invoker().onLoad(e.getEntity(), world);
            }
        });

        // block breaking and right/left clicks
        bus.addListener(BlockEvent.BreakEvent.class, e -> {
            if (e.getLevel() instanceof Level world && !PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(world, e.getPlayer(),
                    e.getPos(), e.getState(), world.getBlockEntity(e.getPos()))) {
                e.setCanceled(true);
            }
        });
        bus.addListener(PlayerInteractEvent.RightClickItem.class, e -> {
            InteractionResultHolder<ItemStack> result = UseItemCallback.EVENT.invoker().interact(e.getEntity(), e.getLevel(), e.getHand());
            if (result.getResult() != InteractionResult.PASS) {
                e.setCanceled(true);
                e.setCancellationResult(result.getResult());
            }
        });
        bus.addListener(PlayerInteractEvent.RightClickBlock.class, e -> {
            InteractionResult result = UseBlockCallback.EVENT.invoker().interact(e.getEntity(), e.getLevel(), e.getHand(), e.getHitVec());
            if (result != InteractionResult.PASS) {
                e.setCanceled(true);
                e.setCancellationResult(result);
            }
        });
        bus.addListener(PlayerInteractEvent.EntityInteractSpecific.class, e -> {
            EntityHitResult hit = new EntityHitResult(e.getTarget(), e.getLocalPos().add(e.getTarget().position()));
            InteractionResult result = UseEntityCallback.EVENT.invoker().interact(e.getEntity(), e.getLevel(), e.getHand(), e.getTarget(), hit);
            if (result != InteractionResult.PASS) {
                e.setCanceled(true);
                e.setCancellationResult(result);
            }
        });
        bus.addListener(PlayerInteractEvent.EntityInteract.class, e -> {
            InteractionResult result = UseEntityCallback.EVENT.invoker().interact(e.getEntity(), e.getLevel(), e.getHand(), e.getTarget(), null);
            if (result != InteractionResult.PASS) {
                e.setCanceled(true);
                e.setCancellationResult(result);
            }
        });
        bus.addListener(AttackEntityEvent.class, e -> {
            Player player = e.getEntity();
            InteractionResult result = AttackEntityCallback.EVENT.invoker().interact(player, player.level(),
                    net.minecraft.world.InteractionHand.MAIN_HAND, e.getTarget(), null);
            if (result != InteractionResult.PASS) {
                e.setCanceled(true);
            }
        });
    }
}
