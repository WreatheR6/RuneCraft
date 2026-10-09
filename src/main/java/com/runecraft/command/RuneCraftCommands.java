package com.runecraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Experience;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.Quest;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestManager;
import com.runecraft.skill.XpService;
import com.runecraft.world.Gielinor;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.Locale;

/** /runecraft (alias /rc): stats, travel and admin tools. */
public final class RuneCraftCommands {
    private RuneCraftCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            register(dispatcher, "runecraft");
            register(dispatcher, "rc");
        });
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher, String name) {
        LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal(name)
                .then(CommandManager.literal("stats").executes(ctx -> stats(ctx, ctx.getSource().getPlayerOrThrow()))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(ctx -> stats(ctx, EntityArgumentType.getPlayer(ctx, "player")))))
                .then(CommandManager.literal("quests").executes(RuneCraftCommands::quests))
                .then(CommandManager.literal("kc").executes(RuneCraftCommands::killCounts))
                .then(CommandManager.literal("gielinor").requires(s -> s.hasPermissionLevel(2))
                        .executes(ctx -> {
                            Gielinor.teleport(ctx.getSource().getPlayerOrThrow(), Landmark.LUMBRIDGE);
                            return 1;
                        }))
                .then(CommandManager.literal("leave").requires(s -> s.hasPermissionLevel(2))
                        .executes(ctx -> {
                            Gielinor.leave(ctx.getSource().getPlayerOrThrow());
                            return 1;
                        }))
                .then(CommandManager.literal("tele").requires(s -> s.hasPermissionLevel(2))
                        .then(CommandManager.argument("landmark", StringArgumentType.word())
                                .suggests((ctx, b) -> CommandSource.suggestMatching(
                                        Arrays.stream(Landmark.values()).map(l -> l.name().toLowerCase(Locale.ROOT)), b))
                                .executes(ctx -> {
                                    Landmark l = Landmark.byName(StringArgumentType.getString(ctx, "landmark"));
                                    if (l == null) {
                                        ctx.getSource().sendError(Text.literal("Unknown landmark"));
                                        return 0;
                                    }
                                    Gielinor.teleport(ctx.getSource().getPlayerOrThrow(), l);
                                    return 1;
                                })))
                .then(CommandManager.literal("setlevel").requires(s -> s.hasPermissionLevel(2))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("skill", StringArgumentType.word()).suggests(RuneCraftCommands::suggestSkills)
                                        .then(CommandManager.argument("level", IntegerArgumentType.integer(1, 99))
                                                .executes(ctx -> setLevel(ctx))))))
                .then(CommandManager.literal("addxp").requires(s -> s.hasPermissionLevel(2))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("skill", StringArgumentType.word()).suggests(RuneCraftCommands::suggestSkills)
                                        .then(CommandManager.argument("xp", IntegerArgumentType.integer(1, Experience.MAX_XP))
                                                .executes(ctx -> addXp(ctx))))))
                .then(CommandManager.literal("completequest").requires(s -> s.hasPermissionLevel(2))
                        .then(CommandManager.argument("quest", StringArgumentType.word())
                                .suggests((ctx, b) -> CommandSource.suggestMatching(
                                        Arrays.stream(Quest.values()).map(q -> q.name().toLowerCase(Locale.ROOT)), b))
                                .executes(ctx -> {
                                    Quest q = Quest.valueOf(StringArgumentType.getString(ctx, "quest").toUpperCase(Locale.ROOT));
                                    QuestManager.complete(ctx.getSource().getPlayerOrThrow(), q);
                                    return 1;
                                })))
                .then(CommandManager.literal("coins").requires(s -> s.hasPermissionLevel(2))
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                                    Players.data(p).setCoins(IntegerArgumentType.getInteger(ctx, "amount"));
                                    ServerNetworking.sync(p);
                                    return 1;
                                })));
        dispatcher.register(root);
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestSkills(
            CommandContext<ServerCommandSource> ctx, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(Arrays.stream(Skill.values()).map(Skill::id), builder);
    }

    private static int stats(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player) {
        SkillSet s = Players.data(player).skills();
        ServerCommandSource src = ctx.getSource();
        src.sendFeedback(() -> Text.literal(player.getName().getString() + " - Combat level " + s.combatLevel()
                + ", Total level " + s.totalLevel()).formatted(Formatting.GOLD), false);
        StringBuilder line = new StringBuilder();
        for (Skill skill : Skill.values()) {
            line.append(skill.displayName()).append(' ').append(s.currentLevel(skill)).append('/').append(s.level(skill)).append("  ");
        }
        String text = line.toString();
        src.sendFeedback(() -> Text.literal(text), false);
        return 1;
    }

    private static int quests(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        PlayerData data = Players.data(p);
        for (Quest q : Quest.values()) {
            int stage = data.questStage(q);
            Formatting color = stage == 0 ? Formatting.RED : stage >= q.finalStage() ? Formatting.GREEN : Formatting.YELLOW;
            p.sendMessage(Text.literal(q.displayName()).formatted(color).append(Text.literal(" - " + q.journalFor(stage))
                    .formatted(Formatting.GRAY)), false);
        }
        p.sendMessage(Text.literal("Quest points: " + data.questPoints() + "/" + Quest.totalQuestPoints()), false);
        return 1;
    }

    private static int killCounts(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        PlayerData data = Players.data(p);
        if (data.killCounts().isEmpty()) {
            p.sendMessage(Text.literal("You haven't killed any bosses yet."), false);
        }
        data.killCounts().forEach((boss, kc) -> p.sendMessage(Text.literal(boss + ": " + kc), false));
        return 1;
    }

    private static int setLevel(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = EntityArgumentType.getPlayer(ctx, "player");
        Skill skill = Skill.byId(StringArgumentType.getString(ctx, "skill"));
        if (skill == null) {
            ctx.getSource().sendError(Text.literal("Unknown skill"));
            return 0;
        }
        XpService.setLevel(p, skill, IntegerArgumentType.getInteger(ctx, "level"));
        ctx.getSource().sendFeedback(() -> Text.literal("Set " + skill.displayName() + " to " + IntegerArgumentType.getInteger(ctx, "level")), true);
        return 1;
    }

    private static int addXp(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = EntityArgumentType.getPlayer(ctx, "player");
        Skill skill = Skill.byId(StringArgumentType.getString(ctx, "skill"));
        if (skill == null) {
            ctx.getSource().sendError(Text.literal("Unknown skill"));
            return 0;
        }
        XpService.add(p, skill, IntegerArgumentType.getInteger(ctx, "xp"));
        return 1;
    }
}
