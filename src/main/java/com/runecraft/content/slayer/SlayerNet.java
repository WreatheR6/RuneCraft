package com.runecraft.content.slayer;

import com.runecraft.RuneCraft;
import com.runecraft.osrs.content.slayer.SlayerTasks;
import com.runecraft.platform.PacketByteBufs;
import com.runecraft.platform.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

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
        // Krystilia's Wilderness tasks keep their own streak: show the one the current task adds to.
        com.runecraft.osrs.content.slayer.SlayerMasters.Master m = p.master == null ? null
                : com.runecraft.osrs.content.slayer.SlayerMasters.get(p.master);
        buf.writeVarInt(m != null && m.wilderness() ? p.wildyStreak : p.streak);
        buf.writeUtf(p.region == null ? "" : SlayerService.regionName(p.region));
        if (ServerPlayNetworking.canSend(player, TASK)) {
            ServerPlayNetworking.send(player, TASK, buf);
        }
    }
}
