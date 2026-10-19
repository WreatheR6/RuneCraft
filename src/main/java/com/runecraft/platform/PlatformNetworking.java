package com.runecraft.platform;

import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.BiConsumer;

/**
 * Registers {@link RawPayload} both ways. Server-bound packets go to {@link ServerPlayNetworking}'s receivers;
 * client-bound ones go to the client dispatcher that {@code client.ClientPlayNetworking} installs (kept as a plain
 * field so the dedicated server never loads client classes).
 */
public final class PlatformNetworking {
    private PlatformNetworking() {
    }

    public static volatile BiConsumer<RawPayload, IPayloadContext> clientDispatch;

    public static void init(IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, PlatformNetworking::register);
    }

    private static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playBidirectional(RawPayload.TYPE, RawPayload.CODEC, (payload, context) -> {
            if (context.flow() == PacketFlow.SERVERBOUND) {
                ServerPlayNetworking.handle(payload, context);
            } else {
                BiConsumer<RawPayload, IPayloadContext> client = clientDispatch;
                if (client != null) {
                    client.accept(payload, context);
                }
            }
        });
    }
}
