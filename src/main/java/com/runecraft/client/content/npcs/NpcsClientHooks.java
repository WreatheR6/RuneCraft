package com.runecraft.client.content.npcs;

import com.runecraft.client.content.ClientModuleHooks;
import com.runecraft.content.npcs.NpcsNet;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client side of the npcs module: the shop screen. */
public final class NpcsClientHooks implements ClientModuleHooks {
    @Override
    public void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(NpcsNet.SHOP, (client, handler, buf, sender) -> {
            ShopScreen.ShopData data = ShopScreen.ShopData.read(buf);
            client.execute(() -> {
                if (client.screen instanceof ShopScreen screen && screen.shopId().equals(data.id())) {
                    screen.update(data);
                } else if (data.open()) {
                    client.setScreen(new ShopScreen(data));
                }
            });
        });
    }
}
