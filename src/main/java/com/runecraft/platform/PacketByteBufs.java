package com.runecraft.platform;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

/** Fabric's packet buffer factory. */
public final class PacketByteBufs {
    private PacketByteBufs() {
    }

    public static FriendlyByteBuf create() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }
}
