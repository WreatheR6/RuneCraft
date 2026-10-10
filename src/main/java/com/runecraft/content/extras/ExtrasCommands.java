package com.runecraft.content.extras;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.runecraft.osrs.content.extras.Pets;
import com.runecraft.osrs.content.extras.Poh;
import com.runecraft.util.Msg;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.function.Consumer;

/** {@code /house ...} and {@code /sailing ...}: mostly run by the clickable chat options. */
final class ExtrasCommands {
    private ExtrasCommands() {
    }

    private static int run(CommandContext<ServerCommandSource> ctx, Consumer<ServerPlayerEntity> action) {
        ServerPlayerEntity p = ctx.getSource().getPlayer();
        if (p != null) {
            action.accept(p);
        }
        return 1;
    }

    private static String s(CommandContext<ServerCommandSource> ctx, String name) {
        return StringArgumentType.getString(ctx, name);
    }

    private static int i(CommandContext<ServerCommandSource> ctx, String name) {
        return IntegerArgumentType.getInteger(ctx, name);
    }

    private static void help(ServerPlayerEntity p) {
        Msg.game(p, "Player-owned house: click a house portal (Rimmington, Taverley, Pollnivneach, Rellekka, Brimhaven, Yanille, "
                + "Hosidius, Prifddinas) to enter. In your house: /house build toggles building mode, /house room adds rooms in the "
                + "direction of your choice, clicking a hotspot lists the furniture you can build (needs a hammer and the materials). "
                + "/house leave exits. A Teleport to house tablet takes you home from anywhere.");
    }

    static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("house")
                    .executes(ctx -> run(ctx, ExtrasCommands::help))
                    .then(CommandManager.literal("help").executes(ctx -> run(ctx, ExtrasCommands::help)))
                    .then(CommandManager.literal("enter")
                            .executes(ctx -> run(ctx, p -> HouseService.enter(p, p.getUuid(), false, true)))
                            .then(CommandManager.literal("build").executes(ctx -> run(ctx, p -> HouseService.enter(p, p.getUuid(), true, true)))))
                    .then(CommandManager.literal("visit")
                            .then(CommandManager.argument("player", StringArgumentType.word()).executes(ctx -> run(ctx, p -> {
                                ServerPlayerEntity owner = p.getServer().getPlayerManager().getPlayer(s(ctx, "player"));
                                if (owner == null) {
                                    Msg.game(p, "That player is not online.");
                                } else {
                                    HouseService.enter(p, owner.getUuid(), false, true);
                                }
                            }))))
                    .then(CommandManager.literal("leave").executes(ctx -> run(ctx, HouseService::leave)))
                    .then(CommandManager.literal("build").executes(ctx -> run(ctx, HouseService::toggleBuilding)))
                    .then(CommandManager.literal("room")
                            .executes(ctx -> run(ctx, HouseService::roomInfo))
                            .then(CommandManager.argument("dir", StringArgumentType.word())
                                    .executes(ctx -> run(ctx, p -> HouseService.roomChoices(p, s(ctx, "dir"))))
                                    .then(CommandManager.argument("room", StringArgumentType.word())
                                            .executes(ctx -> run(ctx, p -> HouseService.buildRoom(p, s(ctx, "dir"), s(ctx, "room")))))))
                    .then(CommandManager.literal("removeroom").executes(ctx -> run(ctx, HouseService::removeRoom)))
                    .then(CommandManager.literal("furnish")
                            .then(CommandManager.argument("cx", IntegerArgumentType.integer(0, 8))
                                    .then(CommandManager.argument("cz", IntegerArgumentType.integer(0, 8))
                                            .then(CommandManager.argument("hotspot", StringArgumentType.word())
                                                    .then(CommandManager.argument("furniture", StringArgumentType.word())
                                                            .executes(ctx -> run(ctx, p -> HouseService.furnish(p, i(ctx, "cx"), i(ctx, "cz"),
                                                                    s(ctx, "hotspot"), s(ctx, "furniture")))))))))
                    .then(CommandManager.literal("unfurnish")
                            .then(CommandManager.argument("cx", IntegerArgumentType.integer(0, 8))
                                    .then(CommandManager.argument("cz", IntegerArgumentType.integer(0, 8))
                                            .then(CommandManager.argument("hotspot", StringArgumentType.word())
                                                    .executes(ctx -> run(ctx, p -> HouseService.unfurnish(p, i(ctx, "cx"), i(ctx, "cz"),
                                                            s(ctx, "hotspot"))))))))
                    .then(CommandManager.literal("direct")
                            .then(CommandManager.argument("cx", IntegerArgumentType.integer(0, 8))
                                    .then(CommandManager.argument("cz", IntegerArgumentType.integer(0, 8))
                                            .then(CommandManager.argument("hotspot", StringArgumentType.word())
                                                    .then(CommandManager.argument("dest", StringArgumentType.word())
                                                            .executes(ctx -> run(ctx, p -> HouseFurniture.direct(p, i(ctx, "cx"), i(ctx, "cz"),
                                                                    s(ctx, "hotspot"), s(ctx, "dest")))))))))
                    .then(CommandManager.literal("tablet")
                            .then(CommandManager.argument("item", StringArgumentType.word())
                                    .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 28))
                                            .executes(ctx -> run(ctx, p -> HouseFurniture.makeTablets(p, s(ctx, "item"), i(ctx, "count")))))))
                    .then(CommandManager.literal("tele")
                            .then(CommandManager.argument("kind", StringArgumentType.word())
                                    .then(CommandManager.argument("dest", StringArgumentType.word())
                                            .executes(ctx -> run(ctx, p -> HouseFurniture.teleport(p, s(ctx, "kind"), s(ctx, "dest")))))))
                    .then(CommandManager.literal("pet")
                            .then(CommandManager.argument("pet", StringArgumentType.word()).executes(ctx -> run(ctx, p -> {
                                if (HouseFurniture.nearby(p, Poh.Effect.PET_HOUSE) == 0) {
                                    Msg.game(p, "You need to be next to your pet house.");
                                } else if (Pets.isPet(s(ctx, "pet"))) {
                                    PetService.take(p, s(ctx, "pet"));
                                }
                            }))))
                    .then(CommandManager.literal("altar")
                            .then(CommandManager.argument("book", StringArgumentType.word())
                                    .executes(ctx -> run(ctx, p -> HouseFurniture.occult(p, s(ctx, "book")))))));
            dispatcher.register(CommandManager.literal("sailing")
                    .executes(ctx -> run(ctx, SailingService::info))
                    .then(CommandManager.literal("info").executes(ctx -> run(ctx, SailingService::info)))
                    .then(CommandManager.literal("charter")
                            .then(CommandManager.argument("from", StringArgumentType.word())
                                    .then(CommandManager.argument("to", StringArgumentType.word())
                                            .executes(ctx -> run(ctx, p -> SailingService.charter(p, s(ctx, "from"), s(ctx, "to")))))))
                    .then(CommandManager.literal("voyage")
                            .then(CommandManager.argument("from", StringArgumentType.word())
                                    .then(CommandManager.argument("to", StringArgumentType.word())
                                            .executes(ctx -> run(ctx, p -> SailingService.startVoyage(p, s(ctx, "from"), s(ctx, "to")))))))
                    .then(CommandManager.literal("buy")
                            .then(CommandManager.argument("boat", StringArgumentType.word())
                                    .executes(ctx -> run(ctx, p -> SailingService.buy(p, s(ctx, "boat"), false)))))
                    .then(CommandManager.literal("build")
                            .then(CommandManager.argument("boat", StringArgumentType.word())
                                    .executes(ctx -> run(ctx, p -> SailingService.buy(p, s(ctx, "boat"), true))))));
        });
    }
}
