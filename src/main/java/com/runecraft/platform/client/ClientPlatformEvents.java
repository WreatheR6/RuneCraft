package com.runecraft.platform.client;

import com.runecraft.RuneCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.Map;

/**
 * Fires the Fabric-shaped client events in this package from NeoForge's events, and runs the mod's client init.
 * <p>
 * The client init (the old {@code onInitializeClient}) creates entity renderers for registered entity types, so it
 * cannot run in the mod constructor, before registration. It runs once, on the first of NeoForge's client
 * registration events (layer definitions, renderers, key mappings, client setup), all of which come after the
 * registries are filled; the common init runs first, as on Fabric.
 */
public final class ClientPlatformEvents {
    private ClientPlatformEvents() {
    }

    private static Runnable clientInit;
    private static boolean initialized;

    public static void init(IEventBus modBus, Runnable onInitializeClient) {
        clientInit = onInitializeClient;
        ClientPlayNetworking.install();

        // registration (mod bus)
        modBus.addListener(EntityRenderersEvent.RegisterLayerDefinitions.class, e -> {
            ensureInitialized();
            EntityModelLayerRegistry.PENDING.forEach(e::registerLayerDefinition);
        });
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, e -> {
            ensureInitialized();
            EntityRendererRegistry.PENDING.forEach(r -> r.accept(e));
        });
        modBus.addListener(RegisterKeyMappingsEvent.class, e -> {
            ensureInitialized();
            KeyBindingHelper.PENDING.forEach(e::register);
        });
        modBus.addListener(FMLClientSetupEvent.class, e -> e.enqueueWork(ClientPlatformEvents::ensureInitialized));
        modBus.addListener(ModelEvent.ModifyBakingResult.class, e -> {
            if (ModelLoadingPlugin.AFTER_BAKE.isEmpty()) {
                return;
            }
            for (Map.Entry<ModelResourceLocation, BakedModel> entry : e.getModels().entrySet()) {
                BakedModel model = ModelLoadingPlugin.AFTER_BAKE.invoker().modifyModelAfterBake(entry.getValue(),
                        new ModelLoadingPlugin.AfterBakeContext(entry.getKey()));
                if (model != entry.getValue()) {
                    entry.setValue(model);
                }
            }
        });
        // Above every vanilla layer, so vanilla's HUD (effects, hotbar, chat) never covers ours. Not hidden by F1:
        // the listeners check hideGui themselves.
        modBus.addListener(RegisterGuiLayersEvent.class, e -> e.registerAboveAll(RuneCraft.id("hud"), (g, delta) ->
                HudRenderCallback.EVENT.invoker().onHudRender(g, delta.getGameTimeDeltaPartialTick(false))));

        // game events
        IEventBus bus = NeoForge.EVENT_BUS;
        bus.addListener(ClientTickEvent.Post.class, e -> ClientTickEvents.END_CLIENT_TICK.invoker().onEndTick(Minecraft.getInstance()));
        bus.addListener(RenderLevelStageEvent.class, e -> {
            if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES && !WorldRenderEvents.AFTER_ENTITIES.isEmpty()) {
                WorldRenderEvents.AFTER_ENTITIES.invoker().afterEntities(new WorldRenderContext(e.getPoseStack(), e.getCamera(),
                        Minecraft.getInstance().renderBuffers().bufferSource(), e.getPartialTick().getGameTimeDeltaPartialTick(false)));
            }
        });
        bus.addListener(ItemTooltipEvent.class, e -> ItemTooltipCallback.EVENT.invoker().getTooltip(e.getItemStack(), e.getFlags(), e.getToolTip()));
        bus.addListener(ClientPlayerNetworkEvent.LoggingOut.class, e ->
                ClientPlayConnectionEvents.DISCONNECT.invoker().onPlayDisconnect(Minecraft.getInstance().getConnection(), Minecraft.getInstance()));
    }

    /** Runs the common init and then the client init, once. */
    private static synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        RuneCraft.initialize();
        clientInit.run();
    }
}
