package com.runecraft.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Game-style chat messages. */
public final class Msg {
    private Msg() {
    }

    public static void game(Player player, String message) {
        player.displayClientMessage(Component.literal(message), false);
    }

    public static void red(Player player, String message) {
        player.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.RED), false);
    }

    public static void action(Player player, String message) {
        player.displayClientMessage(Component.literal(message), true);
    }

    public static String article(String noun) {
        String n = noun.toLowerCase(java.util.Locale.ROOT);
        return (n.startsWith("a") || n.startsWith("e") || n.startsWith("i") || n.startsWith("o") || n.startsWith("u") ? "an " : "a ") + noun;
    }
}
