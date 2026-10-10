package com.runecraft.content.raids;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.runecraft.osrs.content.raids.Invocation;
import com.runecraft.osrs.content.raids.RaidType;
import com.runecraft.util.Msg;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * {@code /raids}: completion counts (chat + screen), {@code /raids leave}, {@code /raids level <n>}
 * (ToA party leader, before the raid begins) and {@code /raids invocations}.
 */
final class RaidCommands {
    private RaidCommands() {
    }

    static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                CommandManager.literal("raids")
                        .executes(ctx -> stats(ctx.getSource().getPlayerOrThrow()))
                        .then(CommandManager.literal("leave").executes(ctx -> leave(ctx.getSource().getPlayerOrThrow())))
                        .then(CommandManager.literal("invocations").executes(ctx -> invocations(ctx.getSource().getPlayerOrThrow())))
                        .then(CommandManager.literal("level")
                                .then(CommandManager.argument("level", IntegerArgumentType.integer(0, 600))
                                        .executes(ctx -> level(ctx.getSource().getPlayerOrThrow(),
                                                IntegerArgumentType.getInteger(ctx, "level")))))));
    }

    private static int stats(ServerPlayerEntity p) {
        RaidsState.Stats s = RaidsState.get(p.getServer()).stats(p.getUuid());
        p.sendMessage(Text.literal("Raid completions").formatted(Formatting.GOLD, Formatting.BOLD), false);
        for (RaidType t : RaidType.values()) {
            String best = t == RaidType.TOB ? "" : ", best points " + s.bestPoints.getOrDefault(t.id(), 0);
            p.sendMessage(Text.literal(t.displayName() + ": " + s.completions.getOrDefault(t.id(), 0) + " completions" + best
                    + ", " + s.purples.getOrDefault(t.id(), 0) + " purples").formatted(Formatting.YELLOW), false);
        }
        RaidNet.sendStats(p);
        return 1;
    }

    private static int leave(ServerPlayerEntity p) {
        RaidInstance raid = RaidManager.of(p);
        if (raid == null) {
            Msg.game(p, "You are not in a raid.");
            return 0;
        }
        raid.removePlayer(p.getUuid(), true);
        Msg.game(p, "You leave the " + raid.type.displayName() + ".");
        if (raid.party().isEmpty()) {
            RaidManager.close(raid);
        } else {
            raid.say(p.getName().getString() + " has left the raid.", Formatting.GRAY);
        }
        return 1;
    }

    private static int invocations(ServerPlayerEntity p) {
        RaidInstance raid = RaidManager.of(p);
        if (raid == null || raid.type != RaidType.TOA) {
            Msg.game(p, "You are not in the Tombs of Amascut.");
            return 0;
        }
        StringBuilder sb = new StringBuilder();
        for (Invocation i : raid.invocations()) {
            sb.append(sb.length() == 0 ? "" : ", ").append(i.displayName()).append(" (").append(i.level()).append(")");
        }
        Msg.game(p, "Raid level " + raid.raidLevel() + ". " + (sb.length() == 0 ? "No invocations." : sb.toString()));
        return 1;
    }

    private static int level(ServerPlayerEntity p, int level) {
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
        raid.say("The raid level is set to " + raid.raidLevel() + ".", Formatting.YELLOW);
        return 1;
    }
}
