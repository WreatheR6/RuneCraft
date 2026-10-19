package com.runecraft.client.content.raids;

import com.runecraft.client.content.ClientModuleHooks;
import com.runecraft.content.raids.RaidNet;
import com.runecraft.platform.client.ClientPlayConnectionEvents;
import com.runecraft.platform.client.ClientPlayNetworking;
import com.runecraft.platform.client.HudRenderCallback;

import java.util.List;

/**
 * Client-side behaviour for the raids content module: the raid HUD and the completion screen.
 * Raid boss renderers are registered automatically from the module's ModelSpecs.
 */
public final class RaidsClientHooks implements ClientModuleHooks {
    @Override
    public void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(RaidNet.HUD, (client, handler, buf, sender) -> RaidHud.read(client, buf));
        ClientPlayNetworking.registerGlobalReceiver(RaidNet.STATS, (client, handler, buf, sender) -> {
            List<RaidsScreen.Row> rows = RaidsScreen.read(buf);
            client.execute(() -> client.setScreen(new RaidsScreen(rows)));
        });
        HudRenderCallback.EVENT.register(RaidHud::render);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(RaidHud::clear));
    }
}
