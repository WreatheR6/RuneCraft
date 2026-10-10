package com.runecraft.content.raids;

import com.runecraft.RuneCraft;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.osrs.content.raids.RaidType;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Server-to-client packets of the raids module: the raid HUD (room, points, timer, boss health
 * including hitpoints above Minecraft's 1024 cap) and the completion-count screen.
 */
public final class RaidNet {
    public static final Identifier HUD = RuneCraft.id("raids/hud");
    public static final Identifier STATS = RuneCraft.id("raids/stats");

    private RaidNet() {
    }

    static void sendHud(RaidInstance raid) {
        RoomLogic logic = raid.logic();
        DataBossEntity boss = logic.started && !logic.done ? logic.hudBoss() : null;
        int alive = raid.activePlayers().size();
        for (ServerPlayerEntity p : raid.players()) {
            if (!ServerPlayNetworking.canSend(p, HUD)) {
                continue;
            }
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBoolean(true);
            buf.writeString(raid.type.displayName());
            buf.writeString(raid.rooms.get(raid.currentRoom()).name());
            buf.writeVarInt(raid.currentRoom());
            buf.writeVarInt(raid.rooms.size());
            buf.writeVarInt(raid.points(p.getUuid()));
            buf.writeVarInt(raid.totalPoints());
            buf.writeVarInt(raid.type == RaidType.TOA ? raid.raidLevel() : -1);
            buf.writeVarInt((int) (raid.elapsedTicks() * 6 / 10));
            buf.writeBoolean(raid.isDowned(p));
            buf.writeString(boss == null || boss.def() == null ? "" : boss.def().name);
            buf.writeVarInt(boss == null ? 0 : raid.hitpoints(boss));
            buf.writeVarInt(boss == null ? 0 : raid.maxHitpoints(boss));
            buf.writeVarInt(alive);
            buf.writeVarInt(raid.party().size());
            buf.writeBoolean(raid.type == RaidType.TOB);
            ServerPlayNetworking.send(p, HUD, buf);
        }
    }

    static void clearHud(ServerPlayerEntity p) {
        if (!ServerPlayNetworking.canSend(p, HUD)) {
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(false);
        ServerPlayNetworking.send(p, HUD, buf);
    }

    /** Opens the raid statistics screen. Returns false if the client lacks the module. */
    static boolean sendStats(ServerPlayerEntity p) {
        if (!ServerPlayNetworking.canSend(p, STATS)) {
            return false;
        }
        RaidsState.Stats stats = RaidsState.get(p.getServer()).stats(p.getUuid());
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(RaidType.values().length);
        for (RaidType t : RaidType.values()) {
            buf.writeString(t.displayName());
            buf.writeVarInt(stats.completions.getOrDefault(t.id(), 0));
            buf.writeVarInt(stats.bestPoints.getOrDefault(t.id(), 0));
            buf.writeVarInt(stats.purples.getOrDefault(t.id(), 0));
            buf.writeBoolean(t == RaidType.TOB);
        }
        ServerPlayNetworking.send(p, STATS, buf);
        return true;
    }
}
