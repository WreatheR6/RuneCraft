package com.runecraft.content.extras;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.runecraft.osrs.content.extras.Pets;
import com.runecraft.osrs.content.extras.Poh;
import com.runecraft.util.Msg;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.Consumer;

/** {@code /house ...} and {@code /sailing ...}: mostly run by the clickable chat options. */
final class ExtrasCommands {
    private ExtrasCommands() {
    }

    private static int run(CommandContext<CommandSourceStack> ctx, Consumer<ServerPlayer> action) {
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p != null) {
            action.accept(p);
        }
        return 1;
    }

    private static String s(CommandContext<CommandSourceStack> ctx, String name) {
        return StringArgumentType.getString(ctx, name);
    }

    private static int i(CommandContext<CommandSourceStack> ctx, String name) {
        return IntegerArgumentType.getInteger(ctx, name);
    }

    private static void help(ServerPlayer p) {
        Msg.game(p, "Player-owned house: click a house portal (Rimmington, Taverley, Pollnivneach, Rellekka, Brimhaven, Yanille, "
                + "Hosidius, Prifddinas) to enter. In your house: /house build toggles building mode, /house room adds rooms in the "
                + "direction of your choice, clicking a hotspot lists the furniture you can build (needs a hammer and the materials). "
                + "/house leave exits. A Teleport to house tablet takes you home from anywhere. Your house starts in Rimmington; "
                + "click another town's house portal to have it moved there (Construction level and fee). Ring a bell-pull to hire "
                + "a servant once your house has two beds.");
    }

    static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("house")
                    .executes(ctx -> run(ctx, ExtrasCommands::help))
                    .then(Commands.literal("help").executes(ctx -> run(ctx, ExtrasCommands::help)))
                    .then(Commands.literal("enter")
                            .executes(ctx -> run(ctx, p -> HouseService.enter(p, p.getUUID(), false, true)))
                            .then(Commands.literal("build").executes(ctx -> run(ctx, p -> HouseService.enter(p, p.getUUID(), true, true)))))
                    .then(Commands.literal("visit")
                            .then(Commands.argument("player", StringArgumentType.word()).executes(ctx -> run(ctx, p -> {
                                ServerPlayer owner = p.getServer().getPlayerList().getPlayerByName(s(ctx, "player"));
                                if (owner == null) {
                                    Msg.game(p, "That player is not online.");
                                } else {
                                    HouseService.enter(p, owner.getUUID(), false, true);
                                }
                            }))))
                    .then(Commands.literal("leave").executes(ctx -> run(ctx, HouseService::leave)))
                    .then(Commands.literal("build").executes(ctx -> run(ctx, HouseService::toggleBuilding)))
                    .then(Commands.literal("room")
                            .executes(ctx -> run(ctx, HouseService::roomInfo))
                            .then(Commands.argument("dir", StringArgumentType.word())
                                    .executes(ctx -> run(ctx, p -> HouseService.roomChoices(p, s(ctx, "dir"))))
                                    .then(Commands.argument("room", StringArgumentType.word())
                                            .executes(ctx -> run(ctx, p -> HouseService.buildRoom(p, s(ctx, "dir"), s(ctx, "room")))))))
                    .then(Commands.literal("removeroom").executes(ctx -> run(ctx, HouseService::removeRoom)))
                    .then(Commands.literal("furnish")
                            .then(Commands.argument("cx", IntegerArgumentType.integer(0, 8))
                                    .then(Commands.argument("cz", IntegerArgumentType.integer(0, 8))
                                            .then(Commands.argument("hotspot", StringArgumentType.word())
                                                    .then(Commands.argument("furniture", StringArgumentType.word())
                                                            .executes(ctx -> run(ctx, p -> HouseService.furnish(p, i(ctx, "cx"), i(ctx, "cz"),
                                                                    s(ctx, "hotspot"), s(ctx, "furniture")))))))))
                    .then(Commands.literal("unfurnish")
                            .then(Commands.argument("cx", IntegerArgumentType.integer(0, 8))
                                    .then(Commands.argument("cz", IntegerArgumentType.integer(0, 8))
                                            .then(Commands.argument("hotspot", StringArgumentType.word())
                                                    .executes(ctx -> run(ctx, p -> HouseService.unfurnish(p, i(ctx, "cx"), i(ctx, "cz"),
                                                            s(ctx, "hotspot"))))))))
                    .then(Commands.literal("direct")
                            .then(Commands.argument("cx", IntegerArgumentType.integer(0, 8))
                                    .then(Commands.argument("cz", IntegerArgumentType.integer(0, 8))
                                            .then(Commands.argument("hotspot", StringArgumentType.word())
                                                    .then(Commands.argument("dest", StringArgumentType.word())
                                                            .executes(ctx -> run(ctx, p -> HouseFurniture.direct(p, i(ctx, "cx"), i(ctx, "cz"),
                                                                    s(ctx, "hotspot"), s(ctx, "dest")))))))))
                    .then(Commands.literal("tablet")
                            .then(Commands.argument("item", StringArgumentType.word())
                                    .then(Commands.argument("count", IntegerArgumentType.integer(1, 28))
                                            .executes(ctx -> run(ctx, p -> HouseFurniture.makeTablets(p, s(ctx, "item"), i(ctx, "count")))))))
                    .then(Commands.literal("tele")
                            .then(Commands.argument("kind", StringArgumentType.word())
                                    .then(Commands.argument("dest", StringArgumentType.word())
                                            .executes(ctx -> run(ctx, p -> HouseFurniture.teleport(p, s(ctx, "kind"), s(ctx, "dest")))))))
                    .then(Commands.literal("pet")
                            .then(Commands.argument("pet", StringArgumentType.word()).executes(ctx -> run(ctx, p -> {
                                if (HouseFurniture.nearby(p, Poh.Effect.PET_HOUSE) == 0) {
                                    Msg.game(p, "You need to be next to your pet house.");
                                } else if (Pets.isPet(s(ctx, "pet"))) {
                                    PetService.take(p, s(ctx, "pet"));
                                }
                            }))))
                    .then(Commands.literal("altar")
                            .then(Commands.argument("book", StringArgumentType.word())
                                    .executes(ctx -> run(ctx, p -> HouseFurniture.occult(p, s(ctx, "book"))))))
                    .then(Commands.literal("move")
                            .then(Commands.argument("town", StringArgumentType.word())
                                    .executes(ctx -> run(ctx, p -> HouseService.move(p, s(ctx, "town"))))))
                    .then(Commands.literal("nexus")
                            .then(Commands.argument("dest", StringArgumentType.word())
                                    .executes(ctx -> run(ctx, p -> HouseFurniture.addToNexus(p, s(ctx, "dest"))))))
                    .then(Commands.literal("servant")
                            .executes(ctx -> run(ctx, HouseServants::ring))
                            .then(Commands.literal("hire")
                                    .then(Commands.argument("who", StringArgumentType.word())
                                            .executes(ctx -> run(ctx, p -> HouseServants.hire(p, s(ctx, "who"))))))
                            .then(Commands.literal("fetch")
                                    .then(Commands.argument("item", StringArgumentType.word())
                                            .executes(ctx -> run(ctx, p -> HouseServants.fetch(p, s(ctx, "item"))))))
                            .then(Commands.literal("sawmill").executes(ctx -> run(ctx, HouseServants::sawmill)))
                            .then(Commands.literal("dismiss").executes(ctx -> run(ctx, HouseServants::dismiss)))));
            dispatcher.register(Commands.literal("sailing")
                    .executes(ctx -> run(ctx, SailingService::info))
                    .then(Commands.literal("info").executes(ctx -> run(ctx, SailingService::info)))
                    .then(Commands.literal("charter")
                            .then(Commands.argument("from", StringArgumentType.word())
                                    .then(Commands.argument("to", StringArgumentType.word())
                                            .executes(ctx -> run(ctx, p -> SailingService.charter(p, s(ctx, "from"), s(ctx, "to")))))))
                    .then(Commands.literal("voyage")
                            .then(Commands.argument("from", StringArgumentType.word())
                                    .then(Commands.argument("to", StringArgumentType.word())
                                            .executes(ctx -> run(ctx, p -> SailingService.startVoyage(p, s(ctx, "from"), s(ctx, "to")))))))
                    .then(Commands.literal("buy")
                            .then(Commands.argument("boat", StringArgumentType.word())
                                    .executes(ctx -> run(ctx, p -> SailingService.buy(p, s(ctx, "boat"), false)))))
                    .then(Commands.literal("build")
                            .then(Commands.argument("boat", StringArgumentType.word())
                                    .executes(ctx -> run(ctx, p -> SailingService.buy(p, s(ctx, "boat"), true))))));
        });
    }
}
