package com.runecraft.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
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

    // ------------------------------------------------------------------ telegraphs

    /** Style colours shared by every combat cue (round 41 plan): melee red, ranged green, magic blue, typeless/AoE orange. */
    public static final int MELEE_RGB = 0xE03030;
    public static final int RANGED_RGB = 0x40C040;
    public static final int MAGIC_RGB = 0x4080FF;
    public static final int TYPELESS_RGB = 0xFFA020;
    private static final int GOLD_RGB = 0xFFC030;

    private static final Pattern MAGIC = Pattern.compile("\\b(magic|magical|mage)\\b");
    private static final Pattern RANGED = Pattern.compile("\\b(ranged|range|missiles?|arrows?)\\b");
    private static final Pattern MELEE = Pattern.compile("\\bmelee\\b");

    /** What a telegraph showed (self-tests): the player, the whole line now on screen, and whether it merged into one shown. */
    @FunctionalInterface
    public interface TelegraphListener {
        void shown(ServerPlayer player, String line, boolean merged);
    }

    /** Self-test hook: called for every telegraph shown. */
    public static TelegraphListener telegraphListener = (player, line, merged) -> {
    };

    /** The line a player is being shown: its parts and the Minecraft tick of the newest. */
    private static final class Line {
        long mc;
        final List<Component> parts = new ArrayList<>();
    }

    private static final Map<UUID, Line> LINES = new HashMap<>();

    /**
     * A boss or raid warning in the middle of the screen (the vanilla subtitle), where eating messages and status lines on the
     * action bar can't overwrite it. // playability: OSRS shows these as overhead text or a chat line the player reads while
     * acting on ticks; a real-time player needs it unmissable. Coloured by the style it names (magic blue, ranged green,
     * melee red, otherwise gold) unless {@code colourOrNull} is given. Warnings within {@link
     * com.runecraft.osrs.ArenaTiming#TELEGRAPH_MERGE_MC} Minecraft ticks merge into one line joined with " · ". A player with
     * no client connection (a fake player) gets it on the action bar.
     */
    public static void telegraph(ServerPlayer player, String text, ChatFormatting colourOrNull) {
        if (player == null || text == null || text.isEmpty()) {
            return;
        }
        MutableComponent part = Component.literal(text);
        if (colourOrNull != null) {
            part.withStyle(colourOrNull);
        } else {
            int rgb = colourFor(text);
            part.withStyle(s -> s.withColor(rgb));
        }
        telegraph(player, part);
    }

    /** A telegraph with its own colouring (e.g. one colour per orb of a volley). */
    public static void telegraph(ServerPlayer player, Component part) {
        if (player == null || part == null) {
            return;
        }
        long now = com.runecraft.action.ActionManager.mcTick();
        Line line = LINES.computeIfAbsent(player.getUUID(), u -> new Line());
        boolean merged = !line.parts.isEmpty() && com.runecraft.osrs.ArenaTiming.mergesTelegraph(line.mc, now);
        if (!merged) {
            line.parts.clear();
        }
        String text = part.getString();
        line.parts.removeIf(c -> c.getString().equals(text)); // the same warning twice shows once
        line.parts.add(part);
        while (line.parts.size() > com.runecraft.osrs.ArenaTiming.TELEGRAPH_MAX_PARTS) {
            line.parts.remove(0);
        }
        line.mc = now;
        MutableComponent shown = Component.empty();
        for (int i = 0; i < line.parts.size(); i++) {
            if (i > 0) {
                shown.append(Component.literal(" · ").withStyle(ChatFormatting.GRAY));
            }
            shown.append(line.parts.get(i));
        }
        if (player instanceof net.neoforged.neoforge.common.util.FakePlayer || player.connection == null) {
            player.displayClientMessage(shown, true);
        } else {
            // fade in 2, stay 30, fade out 6 Minecraft ticks; an empty title makes vanilla show the subtitle on its own
            player.connection.send(new ClientboundSetTitlesAnimationPacket(2, 30, 6));
            player.connection.send(new ClientboundSetSubtitleTextPacket(shown));
            player.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
        }
        telegraphListener.shown(player, shown.getString(), merged);
    }

    /** The colour of a warning by the combat style it names: magic blue, ranged green, melee red, otherwise gold. */
    public static int colourFor(String text) {
        String t = text.toLowerCase(Locale.ROOT);
        boolean magic = MAGIC.matcher(t).find();
        boolean ranged = RANGED.matcher(t).find();
        boolean melee = MELEE.matcher(t).find();
        int styles = (magic ? 1 : 0) + (ranged ? 1 : 0) + (melee ? 1 : 0);
        if (styles != 1) {
            return GOLD_RGB;
        }
        return magic ? MAGIC_RGB : ranged ? RANGED_RGB : MELEE_RGB;
    }
}
