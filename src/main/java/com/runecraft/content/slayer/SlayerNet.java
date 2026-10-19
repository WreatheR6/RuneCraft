package com.runecraft.content.slayer;

import com.runecraft.RuneCraft;
import com.runecraft.osrs.content.slayer.SlayerTasks;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.util.Locale;

/** Server to client sync of the slayer task (shown on the HUD by the client hooks). */
public final class SlayerNet {
    /** Payload: task display name (empty = none), remaining, points, streak, region restriction (empty = none). */
    public static final ResourceLocation TASK = RuneCraft.id("slayer/task");

    private SlayerNet() {
    }

    public static void sync(ServerPlayer player) {
        SlayerState.Profile p = SlayerService.profile(player);
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(p.hasTask() ? SlayerTasks.displayName(p.task) : "");
        buf.writeVarInt(Math.max(0, p.remaining));
        buf.writeVarInt(p.points);
        buf.writeVarInt(p.streak);
        buf.writeUtf(p.region == null ? "" : p.region.toLowerCase(Locale.ROOT).replace('_', ' '));
        if (ServerPlayNetworking.canSend(player, TASK)) {
            ServerPlayNetworking.send(player, TASK, buf);
        }
    }
}
