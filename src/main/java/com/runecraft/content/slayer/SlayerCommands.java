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
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * {@code /slayer}: the slayer masters' dialogue options (rewards, equipment shop, task skips and
 * blocks, crafting) as clickable chat menus. Everything but checking the task needs a slayer
 * master nearby.
 */
final class SlayerCommands {
    private static final Identifier STATION = RuneCraft.id("slayer_master");

    private SlayerCommands() {
    }

    static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(CommandManager.literal("slayer")
                .executes(ctx -> run(ctx, p -> SlayerService.checkTask(p), false))
                .then(CommandManager.literal("task").executes(ctx -> run(ctx, SlayerService::checkTask, false)))
                .then(CommandManager.literal("skip").executes(ctx -> run(ctx, SlayerCommands::skip, true)))
                .then(CommandManager.literal("block").executes(ctx -> run(ctx, SlayerCommands::block, true)))
                .then(CommandManager.literal("unblock").then(CommandManager.argument("task", StringArgumentType.greedyString())
                        .executes(ctx -> run(ctx, p -> unblock(p, StringArgumentType.getString(ctx, "task")), true))))
                .then(CommandManager.literal("rewards").executes(ctx -> run(ctx, SlayerCommands::rewards, true)))
                .then(CommandManager.literal("unlock").then(CommandManager.argument("id", StringArgumentType.word())
                        .executes(ctx -> run(ctx, p -> unlock(p, StringArgumentType.getString(ctx, "id")), true))))
                .then(CommandManager.literal("buy").then(CommandManager.argument("item", StringArgumentType.word())
                        .executes(ctx -> run(ctx, p -> buyWithPoints(p, StringArgumentType.getString(ctx, "item")), true))))
                .then(CommandManager.literal("shop").executes(ctx -> run(ctx, SlayerCommands::shop, true))
                        .then(CommandManager.argument("item", StringArgumentType.word())
                                .executes(ctx -> run(ctx, p -> shopBuy(p, StringArgumentType.getString(ctx, "item"), 1), true))
                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 10000))
                                        .executes(ctx -> run(ctx, p -> shopBuy(p, StringArgumentType.getString(ctx, "item"),
                                                IntegerArgumentType.getInteger(ctx, "amount")), true)))))
                .then(CommandManager.literal("craft").executes(ctx -> run(ctx, SlayerCommands::crafts, true))
                        .then(CommandManager.argument("item", StringArgumentType.word())
                                .executes(ctx -> run(ctx, p -> craft(p, StringArgumentType.getString(ctx, "item")), true)))));
    }

    private interface Action {
        void run(ServerPlayerEntity player);
    }

    private static int run(CommandContext<ServerCommandSource> ctx, Action action, boolean needsMaster)
            throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        if (needsMaster && !nearMaster(player) && !ctx.getSource().hasPermissionLevel(2)) {
            Msg.game(player, "You need to speak to a Slayer master for that.");
            return 0;
        }
        action.run(player);
        return 1;
    }

    static boolean nearMaster(ServerPlayerEntity player) {
        boolean npc = !player.getWorld().getEntitiesByClass(QuestNpcEntity.class, player.getBoundingBox().expand(8),
                e -> e.npcId() != null && SlayerMasters.byNpc(e.npcId()) != null).isEmpty();
        if (npc) {
            return true;
        }
        BlockPos base = player.getBlockPos();
        for (BlockPos pos : BlockPos.iterate(base.add(-6, -3, -6), base.add(6, 3, 6))) {
            if (Registries.BLOCK.getId(player.getWorld().getBlockState(pos).getBlock()).equals(STATION)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ task management

    private static void skip(ServerPlayerEntity player) {
        SlayerState.Profile p = SlayerService.profile(player);
        if (!p.hasTask()) {
            Msg.game(player, "You don't have a task to cancel.");
        } else if (p.points < SlayerRewards.SKIP_COST) {
            Msg.game(player, "You need " + SlayerRewards.SKIP_COST + " slayer points to cancel a task.");
        } else {
            p.points -= SlayerRewards.SKIP_COST;
            Msg.game(player, "Your task (" + SlayerTasks.displayName(p.task) + ") has been cancelled. Your streak is kept.");
            p.task = null;
            p.remaining = 0;
            p.region = null;
        }
        SlayerNet.sync(player);
    }

    private static void block(ServerPlayerEntity player) {
        SlayerState.Profile p = SlayerService.profile(player);
        if (!p.hasTask()) {
            Msg.game(player, "You don't have a task to block.");
        } else if (SlayerRewards.blockedFor(p.blocked, p.master).size() >= SlayerRewards.MAX_BLOCKS) {
            Msg.game(player, "That master's block list is full. Unblock something first: "
                    + String.join(", ", SlayerRewards.blockedFor(p.blocked, p.master)));
        } else if (p.points < SlayerRewards.blockCost(p.master)) {
            Msg.game(player, "You need " + SlayerRewards.blockCost(p.master) + " slayer points to block a task.");
        } else {
            p.points -= SlayerRewards.blockCost(p.master);
            // Each master keeps their own block list (OSRS, August 2025); Turael and Spria share one.
            p.blocked.add(SlayerRewards.blockKey(p.master, p.task));
            Msg.game(player, "You will no longer be assigned " + SlayerTasks.displayName(p.task) + " by "
                    + masterName(p.master) + ".");
            p.task = null;
            p.remaining = 0;
            p.region = null;
        }
        SlayerNet.sync(player);
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

    private static void unblock(ServerPlayerEntity player, String task) {
        SlayerState.Profile p = SlayerService.profile(player);
        if (p.blocked.remove(task)) {
            Msg.game(player, "You may be assigned " + blockLabel(task) + " again.");
        } else {
            Msg.game(player, "That task isn't blocked. Blocked: " + (p.blocked.isEmpty() ? "nothing" : String.join(", ", p.blocked)));
        }
    }

    // ------------------------------------------------------------------ rewards

    private static void rewards(ServerPlayerEntity player) {
        SlayerState.Profile p = SlayerService.profile(player);
        player.sendMessage(Text.literal("--- Slayer rewards (" + p.points + " points) ---").formatted(Formatting.GOLD), false);
        for (SlayerRewards.Unlock u : SlayerRewards.UNLOCKS) {
            MutableText line = Text.literal(u.name() + " (" + u.cost() + ") ").formatted(
                    p.unlocks.contains(u.id()) ? Formatting.GREEN : Formatting.WHITE);
            if (!p.unlocks.contains(u.id())) {
                line.append(SlayerService.button("[Unlock]", "/slayer unlock " + u.id(), u.description()));
            } else {
                line.append(Text.literal("unlocked").formatted(Formatting.DARK_GREEN));
            }
            player.sendMessage(line, false);
        }
        for (SlayerRewards.PointBuy b : SlayerRewards.BUY) {
            player.sendMessage(Text.literal(b.amount() + " x " + name(b.item()) + " (" + b.cost() + ") ")
                    .append(SlayerService.button("[Buy]", "/slayer buy " + b.item(), "Costs " + b.cost() + " points")), false);
        }
        if (!p.blocked.isEmpty()) {
            MutableText line = Text.literal("Blocked: ").formatted(Formatting.GRAY);
            for (String b : p.blocked) {
                line.append(SlayerService.button("[" + blockLabel(b) + "]", "/slayer unblock " + b, "Unblock"));
                line.append(Text.literal(" "));
            }
            player.sendMessage(line, false);
        }
    }

    private static void unlock(ServerPlayerEntity player, String id) {
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

    private static void buyWithPoints(ServerPlayerEntity player, String item) {
        SlayerState.Profile p = SlayerService.profile(player);
        SlayerRewards.PointBuy b = SlayerRewards.pointBuy(item);
        if (b == null) {
            Msg.game(player, "That isn't sold for slayer points.");
        } else if (b.requiresUnlock() != null && !p.unlocks.contains(b.requiresUnlock())) {
            Msg.game(player, "You need to unlock " + SlayerRewards.unlock(b.requiresUnlock()).name() + " first.");
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

    private static void shop(ServerPlayerEntity player) {
        player.sendMessage(Text.literal("--- Slayer Equipment ---").formatted(Formatting.GOLD), false);
        for (SlayerRewards.ShopItem s : SlayerRewards.SHOP) {
            MutableText line = Text.literal(name(s.item()) + " - " + s.price() + " gp"
                    + (s.slayerLevel() > 1 ? " (Slayer " + s.slayerLevel() + ")" : "") + " ");
            line.append(SlayerService.button("[1]", "/slayer shop " + s.item() + " 1", "Buy 1"));
            line.append(Text.literal(" "));
            line.append(SlayerService.button("[10]", "/slayer shop " + s.item() + " 10", "Buy 10"));
            player.sendMessage(line, false);
        }
    }

    private static void shopBuy(ServerPlayerEntity player, String item, int amount) {
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

    private static void crafts(ServerPlayerEntity player) {
        player.sendMessage(Text.literal("--- Slayer crafting ---").formatted(Formatting.GOLD), false);
        for (SlayerRewards.Craft c : SlayerRewards.CRAFTS) {
            StringBuilder needs = new StringBuilder();
            for (SlayerRewards.Input in : c.inputs()) {
                needs.append(needs.length() == 0 ? "" : ", ").append(in.count()).append(" ").append(name(in.item()));
            }
            if (c.tool() != null) {
                needs.append(" (tool: ").append(name(c.tool())).append(")");
            }
            player.sendMessage(Text.literal(name(c.product()) + " ")
                    .append(SlayerService.button("[Make]", "/slayer craft " + c.product(),
                            "Needs " + needs + "; " + c.skill().displayName() + " " + c.level()
                                    + (c.unlock() != null ? "; unlock " + SlayerRewards.unlock(c.unlock()).name() : ""))), false);
        }
    }

    private static void craft(ServerPlayerEntity player, String product) {
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

    static String name(String item) {
        ItemDef def = ItemCatalog.get(item);
        if (def != null) {
            return def.name();
        }
        SlayerCategories.Category c = SlayerCategories.get(item);
        return c != null ? c.name() : item.replace('_', ' ');
    }
}
