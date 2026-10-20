package com.runecraft.content.minigames.games;

import com.runecraft.content.BlockHooks;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.osrs.content.minigames.SkillingBosses;

/** Block hooks of the round 38 skilling-boss blocks ({@link SkillingBosses}). */
public final class SkillingBossBlocks {
    private SkillingBossBlocks() {
    }

    public static void register() {
        // The Wintertodt's reward cart works wherever it stands (the camp and the prison's lobby).
        BlockHooks.register(SkillingBosses.WT_REWARD_CART, (player, pos, state) -> WintertodtSession.rewardCart(player));
        for (String id : SkillingBosses.ARENA_ONLY) {
            BlockHooks.register(id, (player, pos, state) -> MinigameManager.onBlock(player, id, pos));
        }
    }
}
