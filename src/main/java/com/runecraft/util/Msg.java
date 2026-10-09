package com.runecraft.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Game-style chat messages. */
public final class Msg {
    private Msg() {
    }

    public static void game(PlayerEntity player, String message) {
        player.sendMessage(Text.literal(message), false);
    }

    public static void red(PlayerEntity player, String message) {
        player.sendMessage(Text.literal(message).formatted(Formatting.RED), false);
    }

    public static void action(PlayerEntity player, String message) {
        player.sendMessage(Text.literal(message), true);
    }

    public static String article(String noun) {
        String n = noun.toLowerCase(java.util.Locale.ROOT);
        return (n.startsWith("a") || n.startsWith("e") || n.startsWith("i") || n.startsWith("o") || n.startsWith("u") ? "an " : "a ") + noun;
    }
}
