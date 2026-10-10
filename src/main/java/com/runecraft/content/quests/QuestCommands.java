package com.runecraft.content.quests;

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
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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
            dispatcher.register(CommandManager.literal("questgear")
                    .executes(QuestCommands::listGear)
                    .then(CommandManager.literal("remove")
                            .then(CommandManager.argument("slot", StringArgumentType.word())
                                    .suggests((ctx, b) -> CommandSource.suggestMatching(new String[]{"hands", "cape", "neck"}, b))
                                    .executes(ctx -> {
                                        QuestGear.remove(ctx.getSource().getPlayerOrThrow(), StringArgumentType.getString(ctx, "slot"));
                                        return 1;
                                    }))));
            dispatcher.register(CommandManager.literal("questguide")
                    .then(CommandManager.argument("quest", StringArgumentType.word())
                            .suggests((ctx, b) -> CommandSource.suggestMatching(
                                    QuestBook.get().quests.stream().map(QuestSpec::id), b))
                            .executes(QuestCommands::guide)));
            dispatcher.register(CommandManager.literal("questcape").executes(QuestCommands::cape));
        });
    }

    private static int listGear(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        Map<String, String> worn = QuestsState.get(p).wornAll(p.getUuid());
        if (worn.isEmpty()) {
            p.sendMessage(Text.literal("You aren't wearing any gloves, capes or amulets. Right-click one to wear it."), false);
            return 1;
        }
        worn.forEach((slot, item) -> p.sendMessage(Text.literal(slot + ": " + QuestInterpreter.describe(item, 1)), false));
        var b = QuestGear.bonus(p);
        p.sendMessage(Text.literal("Bonuses: att " + b.stabAttack() + "/" + b.slashAttack() + "/" + b.crushAttack() + "/" + b.magicAttack()
                + "/" + b.rangedAttack() + ", str +" + b.meleeStrength()).formatted(Formatting.GRAY), false);
        return 1;
    }

    private static int guide(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        QuestSpec q = QuestBook.get().spec(StringArgumentType.getString(ctx, "quest"));
        if (q == null) {
            ctx.getSource().sendError(Text.literal("Unknown quest"));
            return 0;
        }
        PlayerData data = Players.data(p);
        p.sendMessage(Text.literal(q.def.displayName() + " (" + q.def.difficulty().name().toLowerCase() + ", "
                + q.def.questPoints() + " QP)").formatted(Formatting.GOLD), false);
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
        q.missingRequirements.forEach(r -> p.sendMessage(Text.literal("  (" + r + " - not required here)").formatted(Formatting.DARK_GRAY), false));
        q.def.xpRewards().forEach((s, xp) -> p.sendMessage(Text.literal("  Reward: " + String.format("%,.0f", xp) + " " + s.displayName() + " XP")
                .formatted(Formatting.AQUA), false));
        q.def.itemRewards().forEach(r -> p.sendMessage(Text.literal("  Reward: " + r.replace('_', ' ')).formatted(Formatting.AQUA), false));
        q.unlocks.forEach(u -> p.sendMessage(Text.literal("  Unlocks: " + u).formatted(Formatting.AQUA), false));
        int stage = QuestInterpreter.stage(p, q);
        p.sendMessage(Text.literal(stage >= q.stages.size() ? "QUEST COMPLETE!" : q.stages.get(Math.max(0, stage)).journal)
                .formatted(Formatting.YELLOW), false);
        return 1;
    }

    private static void line(ServerPlayerEntity p, String text, boolean met) {
        p.sendMessage(Text.literal("  " + (met ? "[x] " : "[ ] ") + text).formatted(met ? Formatting.GREEN : Formatting.RED), false);
    }

    private static int cape(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        int have = Players.data(p).questPoints();
        int total = Quest.totalQuestPoints();
        p.sendMessage(Text.literal("Quest points: " + have + " / " + total).formatted(Formatting.GOLD), false);
        if (have >= total) {
            p.sendMessage(Text.literal("You've completed every quest! Visit the Wise Old Man in Draynor Village for your quest point cape."), false);
        } else {
            long left = com.runecraft.osrs.content.ContentRegistry.quests().stream().filter(q -> q.questPoints() > 0 && !Players.data(p).questComplete(q)).count();
            p.sendMessage(Text.literal(left + " quests left before you can buy the quest point cape."), false);
        }
        return 1;
    }
}
