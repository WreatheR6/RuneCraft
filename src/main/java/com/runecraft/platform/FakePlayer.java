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
 * ignores teleports, which would leave the self-test's player wherever it first stood. A second one per level
 * ({@link #partner}) stands in for the other player of the self-test's two-player checks.
 */
public final class FakePlayer extends net.neoforged.neoforge.common.util.FakePlayer {
    private static final GameProfile PROFILE = new GameProfile(UUID.fromString("41C82C87-7AfB-4024-BA57-13D2C99CAE77"), "[Minecraft]");
    /** The second fake player's profile (its own UUID, so its kill counts, loot and contribution are its own). */
    private static final GameProfile PARTNER_PROFILE = new GameProfile(UUID.fromString("5E1F7E57-0D00-4C2B-9A11-7A3B2C6D4E5F"), "[Partner]");
    private static final Map<ServerLevel, FakePlayer> BY_LEVEL = new WeakHashMap<>();
    private static final Map<ServerLevel, FakePlayer> PARTNERS = new WeakHashMap<>();

    private FakePlayer(ServerLevel level, GameProfile profile) {
        super(level, profile);
        this.connection = new PacketDroppingHandler(level.getServer(), this);
    }

    public static synchronized ServerPlayer get(ServerLevel world) {
        return BY_LEVEL.computeIfAbsent(world, w -> new FakePlayer(w, PROFILE));
    }

    /**
     * A second fake player, never the same as {@link #get}: the partner of the self-test's two-player checks (a duo's other
     * fighter). {@link #get} is the self-test's own player when no real player runs it, so a partner taken from there would
     * be the same player and every share, kill count and loot roll would land on one.
     */
    public static synchronized ServerPlayer partner(ServerLevel world) {
        return PARTNERS.computeIfAbsent(world, w -> new FakePlayer(w, PARTNER_PROFILE));
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
