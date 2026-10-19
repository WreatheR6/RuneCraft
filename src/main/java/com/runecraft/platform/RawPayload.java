package com.runecraft.platform;

import com.runecraft.RuneCraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The one payload type all of RuneCraft's packets travel in: the Fabric-era channel id plus the bytes its handler
 * writes and reads. Registered both ways in {@link PlatformNetworking}; handlers are looked up by channel.
 */
public record RawPayload(ResourceLocation channel, byte[] data) implements CustomPacketPayload {
    public static final Type<RawPayload> TYPE = new Type<>(RuneCraft.id("raw"));

    public static final StreamCodec<FriendlyByteBuf, RawPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeResourceLocation(payload.channel());
                buf.writeByteArray(payload.data());
            },
            buf -> new RawPayload(buf.readResourceLocation(), buf.readByteArray()));

    public static RawPayload of(ResourceLocation channel, FriendlyByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), bytes);
        return new RawPayload(channel, bytes);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
