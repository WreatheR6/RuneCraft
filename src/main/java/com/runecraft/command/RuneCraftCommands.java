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
import com.runecraft.platform.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher, String name) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(name)
                .then(Commands.literal("stats").executes(ctx -> stats(ctx, ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> stats(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("quests").executes(RuneCraftCommands::quests))
                .then(Commands.literal("kc").executes(RuneCraftCommands::killCounts))
                .then(Commands.literal("gielinor").requires(s -> s.hasPermission(2))
                        .executes(ctx -> {
                            Gielinor.teleport(ctx.getSource().getPlayerOrException(), Landmark.LUMBRIDGE);
                            return 1;
                        }))
                .then(Commands.literal("leave").requires(s -> s.hasPermission(2))
                        .executes(ctx -> {
                            Gielinor.leave(ctx.getSource().getPlayerOrException());
                            return 1;
                        }))
                .then(Commands.literal("tele").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("landmark", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(Landmark.values()).map(l -> l.name().toLowerCase(Locale.ROOT)), b))
                                .executes(ctx -> {
                                    Landmark l = Landmark.byName(StringArgumentType.getString(ctx, "landmark"));
                                    if (l == null) {
                                        ctx.getSource().sendFailure(Component.literal("Unknown landmark"));
                                        return 0;
                                    }
                                    Gielinor.teleport(ctx.getSource().getPlayerOrException(), l);
                                    return 1;
                                })))
                .then(Commands.literal("setlevel").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("skill", StringArgumentType.word()).suggests(RuneCraftCommands::suggestSkills)
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 99))
                                                .executes(ctx -> setLevel(ctx))))))
                .then(Commands.literal("addxp").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("skill", StringArgumentType.word()).suggests(RuneCraftCommands::suggestSkills)
                                        .then(Commands.argument("xp", IntegerArgumentType.integer(1, Experience.MAX_XP))
                                                .executes(ctx -> addXp(ctx))))))
                .then(Commands.literal("completequest").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("quest", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                        com.runecraft.osrs.content.ContentRegistry.quests().stream().map(q -> q.questId().toLowerCase(Locale.ROOT)), b))
                                .executes(ctx -> {
                                    String wanted = StringArgumentType.getString(ctx, "quest");
                                    com.runecraft.osrs.QuestInfo q = com.runecraft.osrs.content.ContentRegistry.quests().stream()
                                            .filter(x -> x.questId().equalsIgnoreCase(wanted)).findFirst().orElse(null);
                                    if (q == null) {
                                        ctx.getSource().sendFailure(Component.literal("Unknown quest"));
                                        return 0;
                                    }
                                    QuestManager.complete(ctx.getSource().getPlayerOrException(), q);
                                    return 1;
                                })))
                .then(Commands.literal("coins").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    Players.data(p).setCoins(IntegerArgumentType.getInteger(ctx, "amount"));
                                    ServerNetworking.sync(p);
                                    return 1;
                                })));
        dispatcher.register(root);
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestSkills(
            CommandContext<CommandSourceStack> ctx, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(java.util.stream.Stream.concat(java.util.stream.Stream.of("all"),
                Arrays.stream(Skill.values()).map(Skill::id)), builder);
    }

    private static int stats(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        SkillSet s = Players.data(player).skills();
        CommandSourceStack src = ctx.getSource();
        src.sendSuccess(() -> Component.literal(player.getName().getString() + " - Combat level " + s.combatLevel()
                + ", Total level " + s.totalLevel()).withStyle(ChatFormatting.GOLD), false);
        StringBuilder line = new StringBuilder();
        for (Skill skill : Skill.values()) {
            line.append(skill.displayName()).append(' ').append(s.currentLevel(skill)).append('/').append(s.level(skill)).append("  ");
        }
        String text = line.toString();
        src.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int quests(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        PlayerData data = Players.data(p);
        for (com.runecraft.osrs.QuestInfo q : com.runecraft.osrs.content.ContentRegistry.quests()) {
            int stage = data.questStage(q);
            ChatFormatting color = stage == 0 ? ChatFormatting.RED : stage >= q.finalStage() ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
            p.displayClientMessage(Component.literal(q.displayName()).withStyle(color).append(Component.literal(" - " + q.journalFor(stage))
                    .withStyle(ChatFormatting.GRAY)), false);
        }
        p.displayClientMessage(Component.literal("Quest points: " + data.questPoints() + "/" + Quest.totalQuestPoints()), false);
        return 1;
    }

    private static int killCounts(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        PlayerData data = Players.data(p);
        if (data.killCounts().isEmpty()) {
            p.displayClientMessage(Component.literal("You haven't killed any bosses yet."), false);
        }
        data.killCounts().forEach((boss, kc) -> p.displayClientMessage(Component.literal(boss + ": " + kc), false));
        return 1;
    }

    private static int setLevel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        String name = StringArgumentType.getString(ctx, "skill");
        int level = IntegerArgumentType.getInteger(ctx, "level");
        if (name.equals("all")) {
            for (Skill s : Skill.values()) {
                // Hitpoints never goes below 10 in OSRS.
                XpService.setLevel(p, s, s == Skill.HITPOINTS ? Math.max(10, level) : level);
            }
            ctx.getSource().sendSuccess(() -> Component.literal("Set every skill to " + level), true);
            return 1;
        }
        Skill skill = Skill.byId(name);
        if (skill == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown skill"));
            return 0;
        }
        XpService.setLevel(p, skill, level);
        ctx.getSource().sendSuccess(() -> Component.literal("Set " + skill.displayName() + " to " + IntegerArgumentType.getInteger(ctx, "level")), true);
        return 1;
    }

    private static int addXp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        Skill skill = Skill.byId(StringArgumentType.getString(ctx, "skill"));
        if (skill == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown skill"));
            return 0;
        }
        XpService.add(p, skill, IntegerArgumentType.getInteger(ctx, "xp"));
        return 1;
    }
}
