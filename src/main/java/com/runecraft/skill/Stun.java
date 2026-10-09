package com.runecraft.skill;

import com.runecraft.action.ActionManager;
import com.runecraft.player.Players;
import com.runecraft.util.Msg;
import net.minecraft.server.network.ServerPlayerEntity;

/** Thieving stuns block other actions for a few seconds. */
public final class Stun {
    private Stun() {
    }

    public static boolean check(ServerPlayerEntity player) {
        if (Players.data(player).stunnedUntil > ActionManager.mcTick()) {
            Msg.game(player, "You're stunned!");
            return false;
        }
        return true;
    }

    public static void apply(ServerPlayerEntity player, int gameTicks) {
        Players.data(player).stunnedUntil = ActionManager.mcTick() + (long) gameTicks * ActionManager.MC_TICKS_PER_GAME_TICK;
        ActionManager.cancel(player);
    }
}
