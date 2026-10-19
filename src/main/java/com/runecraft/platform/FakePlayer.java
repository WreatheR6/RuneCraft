package com.runecraft.platform;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Fabric's fake player: one per level, a NeoForge {@code FakePlayer} (so {@code isFakePlayer()} and the fake player
 * checks hold) whose connection only drops outgoing packets, as Fabric's does. NeoForge's own fake connection also
 * ignores teleports, which would leave the self-test's player wherever it first stood.
 */
public final class FakePlayer extends net.neoforged.neoforge.common.util.FakePlayer {
    private static final GameProfile PROFILE = new GameProfile(UUID.fromString("41C82C87-7AfB-4024-BA57-13D2C99CAE77"), "[Minecraft]");
    private static final Map<ServerLevel, FakePlayer> BY_LEVEL = new WeakHashMap<>();

    private FakePlayer(ServerLevel level) {
        super(level, PROFILE);
        this.connection = new PacketDroppingHandler(level.getServer(), this);
    }

    public static synchronized ServerPlayer get(ServerLevel world) {
        return BY_LEVEL.computeIfAbsent(world, FakePlayer::new);
    }

    private static final class PacketDroppingHandler extends ServerGamePacketListenerImpl {
        PacketDroppingHandler(MinecraftServer server, ServerPlayer player) {
            super(server, new DummyConnection(), player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
        }

        @Override
        public void tick() {
        }

        @Override
        public void send(Packet<?> packet) {
        }

        @Override
        public void send(Packet<?> packet, PacketSendListener listener) {
        }

        @Override
        public void disconnect(Component reason) {
        }

        @Override
        public void onDisconnect(DisconnectionDetails details) {
        }
    }

    private static final class DummyConnection extends Connection {
        DummyConnection() {
            super(PacketFlow.SERVERBOUND);
        }

        @Override
        public void setListenerForServerboundHandshake(PacketListener listener) {
        }
    }
}
