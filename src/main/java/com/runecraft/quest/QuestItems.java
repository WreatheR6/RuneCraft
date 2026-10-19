package com.runecraft.quest;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Quest items with a right-click use (reading notes, etc.). */
public final class QuestItems {
    private QuestItems() {
    }

    public static boolean hasUse(String id) {
        return id.equals("research_notes") || id.equals("message_from_juliet");
    }

    public static void use(ServerPlayer player, ItemStack stack, String id) {
        switch (id) {
            case "research_notes" -> com.runecraft.util.Msg.game(player,
                    "The notes are in a strange code. Something about \"teleporting to the altars\" and \"essence\".");
            case "message_from_juliet" -> com.runecraft.util.Msg.game(player,
                    "Dearest Romeo, come and see me, I have a plan... Love, Juliet.");
            default -> {
            }
        }
    }
}
