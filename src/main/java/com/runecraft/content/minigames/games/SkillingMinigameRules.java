package com.runecraft.content.minigames.games;

import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.quest.QuestApi;
import net.minecraft.server.level.ServerPlayer;

/**
 * Entry requirements of the skilling minigames beyond their skill level (OSRS Wiki): Giants' Foundry needs Sleeping Giants,
 * Trouble Brewing Cabin Fever (and 40 Cooking, the minigame's own level).
 */
public final class SkillingMinigameRules {
    private SkillingMinigameRules() {
    }

    /** The first unmet requirement as a message, or null (also null for games this class doesn't cover). */
    public static String requirement(ServerPlayer player, Minigame game) {
        return switch (game) {
            case GIANTS_FOUNDRY -> GiantsFoundrySession.requirement(player);
            case TROUBLE_BREWING -> quest(player, "cabin_fever",
                    "Honest Jimmy: Only proper pirates brew rum here, matey. (Complete Cabin Fever.)");
            default -> null;
        };
    }

    private static String quest(ServerPlayer player, String quest, String message) {
        return ContentRegistry.quest(quest) != null && !QuestApi.isComplete(player, quest) ? message : null;
    }
}
