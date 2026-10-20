package com.runecraft.content.raids;

import com.runecraft.RuneCraft;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.osrs.content.raids.RaidType;
import com.runecraft.platform.PacketByteBufs;
import com.runecraft.platform.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-to-client packets of the raids module: the raid HUD (room, points, timer, boss health
 * including hitpoints above Minecraft's 1024 cap) and the completion-count screen.
 */
public final class RaidNet {
    public static final ResourceLocation HUD = RuneCraft.id("raids/hud");
    public static final ResourceLocation STATS = RuneCraft.id("raids/stats");

    private RaidNet() {
    }

    static void sendHud(RaidInstance raid) {
        RoomLogic logic = raid.logic();
        DataBossEntity boss = logic.started && !logic.done ? logic.hudBoss() : null;
        int alive = raid.activePlayers().size();
        String status = raid.hudStatus();
        for (ServerPlayer p : raid.players()) {
            if (!ServerPlayNetworking.canSend(p, HUD)) {
                continue;
            }
            FriendlyByteBuf buf = PacketByteBufs.create();
            buf.writeBoolean(true);
            buf.writeUtf(raid.type.displayName());
            buf.writeUtf(raid.rooms.get(raid.currentRoom()).name());
            buf.writeVarInt(raid.currentRoom());
            buf.writeVarInt(raid.rooms.size());
            buf.writeVarInt(raid.points(p.getUUID()));
            buf.writeVarInt(raid.totalPoints());
            buf.writeVarInt(raid.type == RaidType.TOA ? raid.raidLevel() : -1);
            buf.writeVarInt((int) (raid.elapsedTicks() * 6 / 10));
            buf.writeBoolean(raid.isDowned(p));
            buf.writeUtf(boss == null || boss.def() == null ? "" : boss.def().name);
            buf.writeVarInt(boss == null ? 0 : raid.hitpoints(boss));
            buf.writeVarInt(boss == null ? 0 : raid.maxHitpoints(boss));
            buf.writeVarInt(alive);
            buf.writeVarInt(raid.party().size());
            buf.writeBoolean(raid.type == RaidType.TOB);
            buf.writeUtf(status);
            ServerPlayNetworking.send(p, HUD, buf);
        }
    }

    static void clearHud(ServerPlayer p) {
        if (!ServerPlayNetworking.canSend(p, HUD)) {
            return;
        }
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(false);
        ServerPlayNetworking.send(p, HUD, buf);
    }

    /** Opens the raid statistics screen. Returns false if the client lacks the module. */
    static boolean sendStats(ServerPlayer p) {
        if (!ServerPlayNetworking.canSend(p, STATS)) {
            return false;
        }
        RaidsState.Stats stats = RaidsState.get(p.getServer()).stats(p.getUUID());
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(RaidType.values().length);
        for (RaidType t : RaidType.values()) {
            buf.writeUtf(t.displayName());
            buf.writeVarInt(stats.completions.getOrDefault(t.id(), 0));
            buf.writeVarInt(stats.bestPoints.getOrDefault(t.id(), 0));
            buf.writeVarInt(stats.purples.getOrDefault(t.id(), 0));
            buf.writeBoolean(t == RaidType.TOB);
        }
        ServerPlayNetworking.send(p, STATS, buf);
        return true;
    }
}
