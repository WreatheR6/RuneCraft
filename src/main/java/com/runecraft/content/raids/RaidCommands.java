package com.runecraft.content.raids;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.runecraft.osrs.content.raids.Invocation;
import com.runecraft.osrs.content.raids.RaidMode;
import com.runecraft.osrs.content.raids.RaidType;
import com.runecraft.osrs.content.raids.ToaLoot;
import com.runecraft.util.Msg;
import com.runecraft.platform.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /raids}: completion counts (chat + screen), {@code /raids leave}, {@code /raids level <n>}
 * (ToA party leader, before the raid begins), {@code /raids mode <mode>} (CoX Challenge Mode, ToB
 * Entry/Hard mode; party leader, before the raid begins), {@code /raids invocations} (the ToA invocation interface),
 * {@code /raids invocation <name>} and {@code /raids preset <n>} (its links), and {@code /raids supplies <bundle>}.
 */
final class RaidCommands {
    private RaidCommands() {
    }

    static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("raids")
                        .executes(ctx -> stats(ctx.getSource().getPlayerOrException()))
                        .then(Commands.literal("leave").executes(ctx -> leave(ctx.getSource().getPlayerOrException())))
                        .then(Commands.literal("invocations").executes(ctx -> invocations(ctx.getSource().getPlayerOrException())))
                        .then(Commands.literal("invocation")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> ToaInvocationMenu.toggle(ctx.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("preset")
                                .then(Commands.argument("index", IntegerArgumentType.integer(0, 20))
                                        .executes(ctx -> ToaInvocationMenu.preset(ctx.getSource().getPlayerOrException(),
                                                IntegerArgumentType.getInteger(ctx, "index")))))
                        .then(Commands.literal("supplies")
                                .then(Commands.argument("bundle", StringArgumentType.word())
                                        .executes(ctx -> ToaSupplyService.claim(ctx.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(ctx, "bundle")))))
                        .then(Commands.literal("buy")
                                .then(Commands.argument("item", StringArgumentType.word())
                                        .executes(ctx -> TobRooms.buy(ctx.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(ctx, "item")))))
                        .then(Commands.literal("mode")
                                .then(Commands.argument("mode", StringArgumentType.word())
                                        .executes(ctx -> mode(ctx.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(ctx, "mode")))))
                        .then(Commands.literal("level")
                                .then(Commands.argument("level", IntegerArgumentType.integer(0, 600))
                                        .executes(ctx -> level(ctx.getSource().getPlayerOrException(),
                                                IntegerArgumentType.getInteger(ctx, "level")))))));
    }

    private static int stats(ServerPlayer p) {
        RaidsState.Stats s = RaidsState.get(p.getServer()).stats(p.getUUID());
        p.displayClientMessage(Component.literal("Raid completions").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        for (RaidType t : RaidType.values()) {
            String best = t == RaidType.TOB ? "" : ", best points " + s.bestPoints.getOrDefault(t.id(), 0);
            p.displayClientMessage(Component.literal(t.displayName() + ": " + s.completions.getOrDefault(t.id(), 0) + " completions" + best
                    + ", " + s.purples.getOrDefault(t.id(), 0) + " purples").withStyle(ChatFormatting.YELLOW), false);
            if (t == RaidType.TOA) {
                p.displayClientMessage(Component.literal("  Entry " + s.completions.getOrDefault(ToaLoot.modeKey(0), 0) + ", Normal "
                        + s.completions.getOrDefault(ToaLoot.modeKey(150), 0) + ", Expert " + s.completions.getOrDefault(ToaLoot.modeKey(300), 0))
                        .withStyle(ChatFormatting.GRAY), false);
            }
        }
        RaidNet.sendStats(p);
        return 1;
    }

    private static int leave(ServerPlayer p) {
        RaidInstance raid = RaidManager.of(p);
        if (raid == null) {
            Msg.game(p, "You are not in a raid.");
            return 0;
        }
        raid.removePlayer(p.getUUID(), true);
        Msg.game(p, "You leave the " + raid.type.displayName() + ".");
        if (raid.party().isEmpty()) {
            RaidManager.close(raid);
        } else {
            raid.say(p.getName().getString() + " has left the raid.", ChatFormatting.GRAY);
        }
        return 1;
    }

    private static int invocations(ServerPlayer p) {
        RaidInstance raid = RaidManager.of(p);
        if (raid == null || raid.type != RaidType.TOA) {
            Msg.game(p, "You are not in the Tombs of Amascut.");
            return 0;
        }
        ToaInvocationMenu.show(p, raid);
        return 1;
    }

    private static int mode(ServerPlayer p, String name) {
        RaidInstance raid = RaidManager.of(p);
        if (raid == null) {
            Msg.game(p, "You must be in a raid lobby to choose the mode.");
            return 0;
        }
        if (raid.begun()) {
            Msg.game(p, "The raid has already begun.");
            return 0;
        }
        if (!raid.isLeader(p)) {
            Msg.game(p, "Only the party leader can choose the mode.");
            return 0;
        }
        RaidMode m = RaidMode.byName(name);
        if (m == null || !raid.setMode(m)) {
            Msg.game(p, raid.type.displayName() + " modes: " + (raid.type == RaidType.COX ? "normal, challenge"
                    : raid.type == RaidType.TOB ? "entry, normal, hard" : "normal (use the obelisk to set the raid level)") + ".");
            return 0;
        }
        raid.say("The raid mode is set to " + m.displayName() + ".", ChatFormatting.YELLOW);
        return 1;
    }

    private static int level(ServerPlayer p, int level) {
        RaidInstance raid = RaidManager.of(p);
        if (raid == null || raid.type != RaidType.TOA) {
            Msg.game(p, "You must be in the Tombs of Amascut lobby to set the raid level.");
            return 0;
        }
        if (raid.begun()) {
            Msg.game(p, "The raid has already begun.");
            return 0;
        }
        if (!raid.isLeader(p)) {
            Msg.game(p, "Only the party leader can set the raid level.");
            return 0;
        }
        raid.setCustomLevel(level);
        raid.say("The raid level is set to " + raid.raidLevel() + ".", ChatFormatting.YELLOW);
        return 1;
    }
}
