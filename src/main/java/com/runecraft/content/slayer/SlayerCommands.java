package com.runecraft.content.slayer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.runecraft.RuneCraft;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.slayer.SlayerCategories;
import com.runecraft.osrs.content.slayer.SlayerMasters;
import com.runecraft.osrs.content.slayer.SlayerRewards;
import com.runecraft.osrs.content.slayer.SlayerTasks;
import com.runecraft.player.Players;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.platform.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /slayer}: the slayer masters' dialogue options (rewards, equipment shop, task skips and
 * blocks, crafting) as clickable chat menus. Everything but checking the task needs a slayer
 * master nearby.
 */
final class SlayerCommands {
    private static final ResourceLocation STATION = RuneCraft.id("slayer_master");

    private SlayerCommands() {
    }

    static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("slayer")
                .executes(ctx -> run(ctx, p -> SlayerService.checkTask(p), false))
                .then(Commands.literal("task").executes(ctx -> run(ctx, SlayerService::checkTask, false)))
                .then(Commands.literal("skip").executes(ctx -> run(ctx, SlayerCommands::skip, true)))
                .then(Commands.literal("amount").then(Commands.argument("kills", IntegerArgumentType.integer(1, 100))
                        .executes(ctx -> run(ctx, p -> SlayerService.chooseBossAmount(p,
                                IntegerArgumentType.getInteger(ctx, "kills")), true))))
                .then(Commands.literal("block").executes(ctx -> run(ctx, SlayerCommands::block, true)))
                .then(Commands.literal("unblock").then(Commands.argument("task", StringArgumentType.greedyString())
                        .executes(ctx -> run(ctx, p -> unblock(p, StringArgumentType.getString(ctx, "task")), true))))
                .then(Commands.literal("rewards").executes(ctx -> run(ctx, SlayerCommands::rewards, true)))
                .then(Commands.literal("unlock").then(Commands.argument("id", StringArgumentType.word())
                        .executes(ctx -> run(ctx, p -> unlock(p, StringArgumentType.getString(ctx, "id")), true))))
                .then(Commands.literal("buy").then(Commands.argument("item", StringArgumentType.word())
                        .executes(ctx -> run(ctx, p -> buyWithPoints(p, StringArgumentType.getString(ctx, "item")), true))))
                .then(Commands.literal("shop").executes(ctx -> run(ctx, SlayerCommands::shop, true))
                        .then(Commands.argument("item", StringArgumentType.word())
                                .executes(ctx -> run(ctx, p -> shopBuy(p, StringArgumentType.getString(ctx, "item"), 1), true))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 10000))
                                        .executes(ctx -> run(ctx, p -> shopBuy(p, StringArgumentType.getString(ctx, "item"),
                                                IntegerArgumentType.getInteger(ctx, "amount")), true)))))
                .then(Commands.literal("craft").executes(ctx -> run(ctx, SlayerCommands::crafts, true))
                        .then(Commands.argument("item", StringArgumentType.word())
                                .executes(ctx -> run(ctx, p -> craft(p, StringArgumentType.getString(ctx, "item")), true))))
                .then(Commands.literal("store").executes(ctx -> run(ctx, SlayerService::storeTask, true)))
                .then(Commands.literal("choose").then(Commands.argument("n", IntegerArgumentType.integer(1, 3))
                        .executes(ctx -> run(ctx, p -> SlayerService.chooseMortimer(p, IntegerArgumentType.getInteger(ctx, "n")),
                                false))))
                .then(Commands.literal("unstore").executes(ctx -> run(ctx, SlayerService::unstoreTask, true)))
                // Disassembling a helmet and using the rune pouch need no master (OSRS: item options).
                .then(Commands.literal("disassemble").then(Commands.argument("helmet", StringArgumentType.word())
                        .executes(ctx -> run(ctx, p -> SlayerItemService.disassemble(p, StringArgumentType.getString(ctx, "helmet")),
                                false))))
                .then(Commands.literal("pouch").executes(ctx -> run(ctx, SlayerItemService::showPouch, false))
                        .then(Commands.literal("deposit").then(Commands.argument("rune", StringArgumentType.word())
                                .executes(ctx -> run(ctx, p -> {
                                    SlayerItemService.depositRunes(p, StringArgumentType.getString(ctx, "rune"));
                                    SlayerItemService.showPouch(p);
                                }, false))))
                        .then(Commands.literal("withdraw").then(Commands.argument("rune", StringArgumentType.word())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 16000))
                                        .executes(ctx -> run(ctx, p -> {
                                            SlayerItemService.withdrawRunes(p, StringArgumentType.getString(ctx, "rune"),
                                                    IntegerArgumentType.getInteger(ctx, "amount"));
                                            SlayerItemService.showPouch(p);
                                        }, false)))))));
    }

    private interface Action {
        void run(ServerPlayer player);
    }

    private static int run(CommandContext<CommandSourceStack> ctx, Action action, boolean needsMaster)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (needsMaster && !nearMaster(player) && !ctx.getSource().hasPermission(2)) {
            Msg.game(player, "You need to speak to a Slayer master for that.");
            return 0;
        }
        action.run(player);
        return 1;
    }

    static boolean nearMaster(ServerPlayer player) {
        boolean npc = !player.level().getEntitiesOfClass(QuestNpcEntity.class, player.getBoundingBox().inflate(8),
                e -> e.npcId() != null && SlayerMasters.byNpc(e.npcId()) != null).isEmpty();
        if (npc) {
            return true;
        }
        BlockPos base = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-6, -3, -6), base.offset(6, 3, 6))) {
            if (BuiltInRegistries.BLOCK.getKey(player.level().getBlockState(pos).getBlock()).equals(STATION)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ task management

    private static void skip(ServerPlayer player) {
        SlayerState.Profile p = SlayerService.profile(player);
        if (!p.hasTask()) {
            Msg.game(player, "You don't have a task to cancel.");
        } else if (p.points < SlayerRewards.skipCost(p.master)) {
            Msg.game(player, "You need " + SlayerRewards.skipCost(p.master) + " slayer points to cancel this task.");
        } else {
            p.points -= SlayerRewards.skipCost(p.master);
            p.modifier = null;
            p.modifierValue = 0;
            Msg.game(player, "Your task (" + SlayerTasks.displayName(p.task) + ") has been cancelled. Your streak is kept.");
            p.task = null;
            p.remaining = 0;
            p.region = null;
        }
        SlayerNet.sync(player);
    }

    private static void block(ServerPlayer player) {
        SlayerState.Profile p = SlayerService.profile(player);
        if (!p.hasTask()) {
            Msg.game(player, "You don't have a task to block.");
        } else if (SlayerRewards.blockedFor(p.blocked, p.master).size() >= blockSlots(player, p.master)) {
            Msg.game(player, blockSlots(player, p.master) == 0 ? "You need 50 quest points to block a task."
                    : "That master's block list is full (" + blockSlots(player, p.master) + " slots: one per 50 quest points, one for the"
                    + " elite Lumbridge & Draynor Diary). Unblock something first: "
                    + String.join(", ", SlayerRewards.blockedFor(p.blocked, p.master)));
        } else if (p.points < SlayerRewards.blockCost(p.master)) {
            Msg.game(player, "You need " + SlayerRewards.blockCost(p.master) + " slayer points to block a task.");
        } else {
            p.points -= SlayerRewards.blockCost(p.master);
            // Each master keeps their own block list (OSRS, August 2025); Turael and Spria share one.
            p.blocked.add(SlayerRewards.blockKey(p.master, p.task));
            p.modifier = null;
            p.modifierValue = 0;
            Msg.game(player, "You will no longer be assigned " + SlayerTasks.displayName(p.task) + " by "
                    + masterName(p.master) + ".");
            p.task = null;
            p.remaining = 0;
            p.region = null;
        }
        SlayerNet.sync(player);
    }

    /** Block slots on each master's list: one per 50 quest points (6 at 300) plus one for the elite Lumbridge diary. */
    static int blockSlots(ServerPlayer player) {
        return blockSlots(player, null);
    }

    /** Block slots on the list of the master {@code masterId} (Mortimer: two at most). */
    static int blockSlots(ServerPlayer player, String masterId) {
        boolean elite = com.runecraft.osrs.content.systems.Diaries.tierComplete(
                com.runecraft.content.systems.SysState.of(player).diaryDone, "lumbridge",
                com.runecraft.osrs.content.systems.Diaries.Tier.ELITE);
        return SlayerRewards.blockSlots(masterId, Players.data(player).questPoints(), elite);
    }

    private static String masterName(String masterId) {
        SlayerMasters.Master m = masterId == null ? null : SlayerMasters.get(masterId);
        return m == null ? "that master" : m.name();
    }

    private static String blockLabel(String entry) {
        int colon = entry.indexOf(':');
        return colon < 0 ? SlayerTasks.displayName(entry)
                : SlayerTasks.displayName(entry.substring(colon + 1)) + " (" + masterName(entry.substring(0, colon)) + ")";
    }

    private static void unblock(ServerPlayer player, String task) {
        SlayerState.Profile p = SlayerService.profile(player);
        if (p.blocked.remove(task)) {
            Msg.game(player, "You may be assigned " + blockLabel(task) + " again.");
        } else {
            Msg.game(player, "That task isn't blocked. Blocked: " + (p.blocked.isEmpty() ? "nothing" : String.join(", ", p.blocked)));
        }
    }

    // ------------------------------------------------------------------ rewards

    private static void rewards(ServerPlayer player) {
        SlayerState.Profile p = SlayerService.profile(player);
        player.displayClientMessage(Component.literal("--- Slayer rewards (" + p.points + " points) ---").withStyle(ChatFormatting.GOLD), false);
        for (SlayerRewards.Unlock u : SlayerRewards.UNLOCKS) {
            MutableComponent line = Component.literal(u.name() + " (" + u.cost() + ") ").withStyle(
                    p.unlocks.contains(u.id()) ? ChatFormatting.GREEN : ChatFormatting.WHITE);
            if (!p.unlocks.contains(u.id())) {
                line.append(SlayerService.button("[Unlock]", "/slayer unlock " + u.id(), u.description()));
            } else {
                line.append(Component.literal("unlocked").withStyle(ChatFormatting.DARK_GREEN));
            }
            player.displayClientMessage(line, false);
        }
        for (SlayerRewards.PointBuy b : SlayerRewards.BUY) {
            player.displayClientMessage(Component.literal(b.amount() + " x " + name(b.item()) + " (" + b.cost() + ") ")
                    .append(SlayerService.button("[Buy]", "/slayer buy " + b.item(), "Costs " + b.cost() + " points")), false);
        }
        if (!p.blocked.isEmpty()) {
            MutableComponent line = Component.literal("Blocked: ").withStyle(ChatFormatting.GRAY);
            for (String b : p.blocked) {
                line.append(SlayerService.button("[" + blockLabel(b) + "]", "/slayer unblock " + b, "Unblock"));
                line.append(Component.literal(" "));
            }
            player.displayClientMessage(line, false);
        }
    }

    private static void unlock(ServerPlayer player, String id) {
        SlayerState.Profile p = SlayerService.profile(player);
        SlayerRewards.Unlock u = SlayerRewards.unlock(id);
        if (u == null) {
            Msg.game(player, "There's no such reward.");
        } else if (p.unlocks.contains(id) && u.cost() == 0) {
            // Free toggles ("I Wildy More Slayer") can be switched off again.
            p.unlocks.remove(id);
            Msg.game(player, u.name() + " is now off.");
            SlayerNet.sync(player);
        } else if (p.unlocks.contains(id)) {
            Msg.game(player, "You've already unlocked " + u.name() + ".");
        } else if (p.points < u.cost()) {
            Msg.game(player, "You need " + u.cost() + " slayer points for " + u.name() + "; you have " + p.points + ".");
        } else {
            p.points -= u.cost();
            p.unlocks.add(id);
            Msg.red(player, "You unlock " + u.name() + ": " + u.description());
            SlayerNet.sync(player);
        }
    }

    private static void buyWithPoints(ServerPlayer player, String item) {
        SlayerState.Profile p = SlayerService.profile(player);
        SlayerRewards.PointBuy b = SlayerRewards.pointBuy(item);
        if (b == null) {
            Msg.game(player, "That isn't sold for slayer points.");
        } else if (b.requiresUnlock() != null && !p.unlocks.contains(b.requiresUnlock())) {
            Msg.game(player, "You need to unlock " + SlayerRewards.unlock(b.requiresUnlock()).name() + " first.");
        } else if (SlayerRewards.ONE_ONLY.contains(b.item()) && (Inv.has(player, b.item(), 1) || banked(player, b.item()))) {
            Msg.game(player, "You may only own one " + name(b.item()).toLowerCase(java.util.Locale.ROOT) + ".");
        } else if (p.points < b.cost()) {
            Msg.game(player, "You need " + b.cost() + " slayer points.");
        } else {
            p.points -= b.cost();
            Inv.give(player, b.item(), b.amount());
            Msg.game(player, "You buy " + b.amount() + " x " + name(b.item()) + ".");
            SlayerNet.sync(player);
        }
    }

    // ------------------------------------------------------------------ equipment shop

    private static void shop(ServerPlayer player) {
        player.displayClientMessage(Component.literal("--- Slayer Equipment ---").withStyle(ChatFormatting.GOLD), false);
        for (SlayerRewards.ShopItem s : SlayerRewards.SHOP) {
            MutableComponent line = Component.literal(name(s.item()) + " - " + s.price() + " gp"
                    + (s.slayerLevel() > 1 ? " (Slayer " + s.slayerLevel() + ")" : "") + " ");
            line.append(SlayerService.button("[1]", "/slayer shop " + s.item() + " 1", "Buy 1"));
            line.append(Component.literal(" "));
            line.append(SlayerService.button("[10]", "/slayer shop " + s.item() + " 10", "Buy 10"));
            player.displayClientMessage(line, false);
        }
    }

    private static void shopBuy(ServerPlayer player, String item, int amount) {
        SlayerRewards.ShopItem s = SlayerRewards.shopItem(item);
        if (s == null) {
            Msg.game(player, "The slayer master doesn't sell that.");
            return;
        }
        int slayer = Players.data(player).skills().level(Skill.SLAYER);
        if (slayer < s.slayerLevel()) {
            Msg.game(player, "You need a Slayer level of " + s.slayerLevel() + " to buy that.");
            return;
        }
        String quest = SlayerRewards.SHOP_QUESTS.get(item);
        if (quest != null && com.runecraft.osrs.content.ContentRegistry.quest(quest) != null
                && !com.runecraft.quest.QuestApi.isComplete(player, quest)) {
            Msg.game(player, "The slayer master only sells those to those who have completed "
                    + com.runecraft.osrs.content.ContentRegistry.quest(quest).displayName() + ".");
            return;
        }
        long cost = (long) s.price() * amount;
        if (Inv.count(player, Inv.COINS) < cost) {
            Msg.game(player, "You don't have enough coins.");
            return;
        }
        Inv.remove(player, Inv.COINS, (int) cost);
        Inv.give(player, item, amount);
        Msg.game(player, "You buy " + amount + " x " + name(item) + " for " + cost + " gp.");
    }

    // ------------------------------------------------------------------ crafting

    private static void crafts(ServerPlayer player) {
        player.displayClientMessage(Component.literal("--- Slayer crafting ---").withStyle(ChatFormatting.GOLD), false);
        // Helmets carried can be taken apart again (their recolour item comes back).
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            String id = Inv.idOf(player.getInventory().getItem(i));
            if (!SlayerRewards.disassembly(id).isEmpty() && seen.add(id)) {
                player.displayClientMessage(Component.literal(name(id) + " ").append(SlayerService.button("[Disassemble]",
                        "/slayer disassemble " + id, "Take it apart into its components")), false);
            }
        }
        for (SlayerRewards.Craft c : SlayerRewards.allCrafts()) {
            StringBuilder needs = new StringBuilder();
            for (SlayerRewards.Input in : c.inputs()) {
                needs.append(needs.length() == 0 ? "" : ", ").append(in.count()).append(" ").append(name(in.item()));
            }
            if (c.tool() != null) {
                needs.append(" (tool: ").append(name(c.tool())).append(")");
            }
            player.displayClientMessage(Component.literal(name(c.product()) + " ")
                    .append(SlayerService.button("[Make]", "/slayer craft " + c.product(),
                            "Needs " + needs + "; " + c.skill().displayName() + " " + c.level()
                                    + (c.unlock() != null ? "; unlock " + SlayerRewards.unlock(c.unlock()).name() : "")
                                    + (c.caTier() != null ? "; the " + c.caTier().toLowerCase(java.util.Locale.ROOT)
                                    + " Combat Achievement rewards" : ""))), false);
        }
    }

    private static void craft(ServerPlayer player, String product) {
        SlayerRewards.Craft c = SlayerRewards.craft(product);
        if (c == null) {
            Msg.game(player, "You can't make that here.");
            return;
        }
        SlayerState.Profile p = SlayerService.profile(player);
        if (c.unlock() != null && !p.unlocks.contains(c.unlock())) {
            Msg.game(player, "You need to learn " + SlayerRewards.unlock(c.unlock()).name() + " from a slayer master first.");
            return;
        }
        if (c.caTier() != null && !caTierClaimed(player, c.caTier())) {
            Msg.game(player, "Only those who have earned the " + c.caTier().toLowerCase(java.util.Locale.ROOT)
                    + " Combat Achievement rewards may style their helmet so.");
            return;
        }
        if (Players.data(player).skills().level(c.skill()) < c.level()) {
            Msg.game(player, "You need a " + c.skill().displayName() + " level of " + c.level() + " to make that.");
            return;
        }
        if (c.tool() != null && !Inv.has(player, c.tool(), 1)) {
            Msg.game(player, "You need " + Msg.article(name(c.tool())) + ".");
            return;
        }
        for (SlayerRewards.Input in : c.inputs()) {
            if (!Inv.has(player, in.item(), in.count())) {
                Msg.game(player, "You need " + in.count() + " x " + name(in.item()) + ".");
                return;
            }
        }
        for (SlayerRewards.Input in : c.inputs()) {
            Inv.remove(player, in.item(), in.count());
        }
        Inv.give(player, c.product(), c.count());
        if (c.xp() > 0) {
            XpService.add(player, c.skill(), c.xp());
        }
        Msg.game(player, "You make " + Msg.article(name(c.product())) + ".");
    }

    /** Whether the player's bank holds the item. */
    private static boolean banked(ServerPlayer player, String item) {
        try {
            return Players.data(player).bank().count(item, "") > 0;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Whether the player has unlocked the Combat Achievement tier {@code tier} (a {@code CombatAchievements.Tier} name). */
    private static boolean caTierClaimed(ServerPlayer player, String tier) {
        com.runecraft.osrs.content.systems.CombatAchievements.Tier have =
                com.runecraft.content.systems.CombatAchievementService.tier(player);
        return have != null && have.ordinal() >= com.runecraft.osrs.content.systems.CombatAchievements.Tier.valueOf(tier).ordinal();
    }

    static String name(String item) {
        ItemDef def = ItemCatalog.get(item);
        if (def != null) {
            return def.name();
        }
        SlayerCategories.Category c = SlayerCategories.get(item);
        return c != null ? c.name() : item.replace('_', ' ');
    }
}
