package com.runecraft.content.quests;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.runecraft.osrs.Quest;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.quests.QuestBook;
import com.runecraft.osrs.content.quests.QuestSpec;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.Map;

/**
 * {@code /questgear [remove <slot>]} - accessories in the virtual gear slots.
 * {@code /questguide <quest>} - requirements, rewards, unlocks and the current journal entry.
 * {@code /questcape} - progress towards the quest point cape.
 */
final class QuestCommands {
    private QuestCommands() {
    }

    static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("questgear")
                    .executes(QuestCommands::listGear)
                    .then(Commands.literal("remove")
                            .then(Commands.argument("slot", StringArgumentType.word())
                                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(new String[]{"hands", "cape", "neck"}, b))
                                    .executes(ctx -> {
                                        QuestGear.remove(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "slot"));
                                        return 1;
                                    }))));
            dispatcher.register(Commands.literal("questguide")
                    .then(Commands.argument("quest", StringArgumentType.word())
                            .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                    QuestBook.get().quests.stream().map(QuestSpec::id), b))
                            .executes(QuestCommands::guide)));
            dispatcher.register(Commands.literal("questcape").executes(QuestCommands::cape));
            // quest puzzles: the chat links QuestPuzzles prints run "/questpuzzle <quest> <stage>.<trigger> <option>"
            dispatcher.register(Commands.literal("questpuzzle")
                    .then(Commands.argument("quest", StringArgumentType.word())
                            .executes(ctx -> {
                                QuestPuzzles.reshow(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "quest"));
                                return 1;
                            })
                            .then(Commands.argument("ref", StringArgumentType.word())
                                    .then(Commands.argument("option", IntegerArgumentType.integer(0))
                                            .executes(ctx -> {
                                                QuestPuzzles.answer(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "quest"),
                                                        StringArgumentType.getString(ctx, "ref"), IntegerArgumentType.getInteger(ctx, "option"));
                                                return 1;
                                            })))));
        });
    }

    private static int listGear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        Map<String, String> worn = QuestsState.get(p).wornAll(p.getUUID());
        if (worn.isEmpty()) {
            p.displayClientMessage(Component.literal("You aren't wearing any gloves, capes or amulets. Right-click one to wear it."), false);
            return 1;
        }
        worn.forEach((slot, item) -> p.displayClientMessage(Component.literal(slot + ": " + QuestInterpreter.describe(item, 1)), false));
        var b = QuestGear.bonus(p);
        p.displayClientMessage(Component.literal("Bonuses: att " + b.stabAttack() + "/" + b.slashAttack() + "/" + b.crushAttack() + "/" + b.magicAttack()
                + "/" + b.rangedAttack() + ", str +" + b.meleeStrength()).withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int guide(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        QuestSpec q = QuestBook.get().spec(StringArgumentType.getString(ctx, "quest"));
        if (q == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown quest"));
            return 0;
        }
        PlayerData data = Players.data(p);
        p.displayClientMessage(Component.literal(q.def.displayName() + " (" + q.def.difficulty().name().toLowerCase() + ", "
                + q.def.questPoints() + " QP)").withStyle(ChatFormatting.GOLD), false);
        if (q.minQuestPoints > 0) {
            line(p, q.minQuestPoints + " quest points", data.questPoints() >= q.minQuestPoints);
        }
        for (Map.Entry<Skill, Integer> e : q.def.requirements().entrySet()) {
            line(p, e.getValue() + " " + e.getKey().displayName(), data.skills().level(e.getKey()) >= e.getValue());
        }
        for (String req : q.def.questRequirements()) {
            var info = com.runecraft.osrs.content.ContentRegistry.quest(req);
            line(p, info == null ? req : info.displayName(), info != null && data.questComplete(info));
        }
        q.missingRequirements.forEach(r -> p.displayClientMessage(Component.literal("  (" + r + " - not required here)").withStyle(ChatFormatting.DARK_GRAY), false));
        q.def.xpRewards().forEach((s, xp) -> p.displayClientMessage(Component.literal("  Reward: " + String.format("%,.0f", xp) + " " + s.displayName() + " XP")
                .withStyle(ChatFormatting.AQUA), false));
        q.def.itemRewards().forEach(r -> p.displayClientMessage(Component.literal("  Reward: " + r.replace('_', ' ')).withStyle(ChatFormatting.AQUA), false));
        q.unlocks.forEach(u -> p.displayClientMessage(Component.literal("  Unlocks: " + u).withStyle(ChatFormatting.AQUA), false));
        int stage = QuestInterpreter.stage(p, q);
        p.displayClientMessage(Component.literal(stage >= q.stages.size() ? "QUEST COMPLETE!" : q.stages.get(Math.max(0, stage)).journal)
                .withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static void line(ServerPlayer p, String text, boolean met) {
        p.displayClientMessage(Component.literal("  " + (met ? "[x] " : "[ ] ") + text).withStyle(met ? ChatFormatting.GREEN : ChatFormatting.RED), false);
    }

    private static int cape(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        int have = Players.data(p).questPoints();
        int total = Quest.totalQuestPoints();
        p.displayClientMessage(Component.literal("Quest points: " + have + " / " + total).withStyle(ChatFormatting.GOLD), false);
        if (have >= total) {
            p.displayClientMessage(Component.literal("You've completed every quest! Visit the Wise Old Man in Draynor Village for your quest point cape."), false);
        } else {
            long left = com.runecraft.osrs.content.ContentRegistry.quests().stream().filter(q -> q.questPoints() > 0 && !Players.data(p).questComplete(q)).count();
            p.displayClientMessage(Component.literal(left + " quests left before you can buy the quest point cape."), false);
        }
        return 1;
    }
}
