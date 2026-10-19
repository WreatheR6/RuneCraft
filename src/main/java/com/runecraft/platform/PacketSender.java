package com.runecraft.platform;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Fabric's response sender: sends a packet back to the player a callback is about. */
public final class PacketSender {
    private final ServerPlayer player;

    public PacketSender(ServerPlayer player) {
        this.player = player;
    }

    public void sendPacket(ResourceLocation channel, FriendlyByteBuf buf) {
        ServerPlayNetworking.send(player, channel, buf);
    }
}
