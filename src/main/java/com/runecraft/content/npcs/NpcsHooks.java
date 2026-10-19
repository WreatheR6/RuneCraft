package com.runecraft.content.npcs;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.runecraft.combat.MonsterStats;
import com.runecraft.content.ModuleHooks;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.menu.MenuService;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.npcs.ShopDef;
import com.runecraft.osrs.content.npcs.Shops;
import com.runecraft.osrs.content.npcs.TownMonsters;
import com.runecraft.osrs.content.npcs.TownNpc;
import com.runecraft.osrs.content.npcs.Townsfolk;
import com.runecraft.quest.QuestApi;
import com.runecraft.quest.QuestScripts;
import com.runecraft.skill.Stations;
import com.runecraft.world.LandmarkBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import java.util.List;

/**
 * Minecraft side of the npcs module: talking to shopkeepers opens their shop, bankers open the bank,
 * travel NPCs offer their destinations, flavour NPCs chat; sneak + right-click trades directly
 * (including with existing characters such as Aubury); right-clicking an attackable townsperson
 * pickpockets them; town residents are kept populated; {@code /npcs travel} runs a chosen trip.
 */
public final class NpcsHooks implements ModuleHooks {
    @Override
    public void init() {
        NpcsNet.register();
        WildernessTransportService.init();
        for (TownNpc n : Townsfolk.all()) {
            QuestScripts.onTalk(n.id(), (player, entity) -> talk(player, entity, n));
        }
        QuestScripts.onTalk("npc_emblem_trader", (player, entity) ->
                com.runecraft.combat.SkullService.emblemTrader(player, entity.getName().getString()));
        for (String placeId : NpcPlacement.placeIds()) {
            LandmarkBuilder.registerCustom(placeId, (world, centre) -> NpcPlacement.settlePlace(world, centre, placeId));
        }
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            // Sneak + right-click: "Trade" with any shopkeeper, including characters from other modules.
            if (entity instanceof QuestNpcEntity npc && player.isShiftKeyDown()) {
                ShopDef shop = shopOf(npc.npcId());
                if (shop == null) {
                    return InteractionResult.PASS;
                }
                if (!world.isClientSide() && player instanceof ServerPlayer sp) {
                    ShopService.open(sp, shop.id(), npc.position());
                }
                return InteractionResult.SUCCESS;
            }
            // Right-click an attackable townsperson: "Pickpocket".
            if (entity instanceof OsrsMonsterEntity monster) {
                TownMonsters.Pickpocket target = pickpocketTarget(monster);
                if (target == null) {
                    return InteractionResult.PASS;
                }
                if (!world.isClientSide() && player instanceof ServerPlayer sp) {
                    pickpocket(sp, monster, target);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ShopService.tick(server);
            TownSpawner.tick(server);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ShopService.forget(handler.player.getUUID()));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("npcs")
                        .then(Commands.literal("travel")
                                .then(Commands.argument("npc", StringArgumentType.word())
                                        .then(Commands.argument("destination", IntegerArgumentType.integer(0))
                                                .executes(ctx -> {
                                                    ServerPlayer p = ctx.getSource().getPlayer();
                                                    if (p != null) {
                                                        TravelService.travel(p, StringArgumentType.getString(ctx, "npc"),
                                                                IntegerArgumentType.getInteger(ctx, "destination"));
                                                    }
                                                    return 1;
                                                }))))));
    }

    /** The shop an NPC runs: one of this module's shopkeepers or an existing character listed as a trader. */
    private static ShopDef shopOf(String npcId) {
        String extra = Townsfolk.EXTRA_TRADERS.get(npcId);
        if (extra != null) {
            return Shops.get(extra);
        }
        TownNpc n = Townsfolk.get(npcId);
        return n != null && n.role() == TownNpc.Role.SHOP ? Shops.get(n.shop()) : null;
    }

    private static void talk(ServerPlayer player, QuestNpcEntity entity, TownNpc n) {
        String name = entity.getName().getString();
        switch (n.role()) {
            case SHOP -> {
                ShopDef shop = Shops.get(n.shop());
                if (shop != null) {
                    QuestApi.say(player, name, shop.greeting());
                    ShopService.open(player, shop.id(), entity.position());
                }
            }
            case BANKER -> {
                QuestApi.say(player, name, n.lines().get(0));
                Stations.openBank(player);
            }
            case TRAVEL -> {
                QuestApi.say(player, name, n.lines().get(0));
                TravelService.offer(player, n);
            }
            case TANNER -> {
                QuestApi.say(player, name, n.lines().get(0));
                MenuService.open(player, MenuType.TANNING, entity.blockPosition(), false);
            }
            case AMBIENT -> {
                List<String> lines = n.lines();
                QuestApi.say(player, name, lines.get(player.getRandom().nextInt(lines.size())));
            }
        }
    }

    private static TownMonsters.Pickpocket pickpocketTarget(OsrsMonsterEntity monster) {
        MonsterStats stats = monster.stats();
        if (stats == null || !(stats.profile() instanceof MonsterDef def)) {
            return null;
        }
        return TownMonsters.pickpocket(def.id);
    }

    private static void pickpocket(ServerPlayer player, OsrsMonsterEntity target, TownMonsters.Pickpocket p) {
        // Shared Thieving rules (coin pouches, rogue equipment, dodgy necklace, Thieving cape, stun).
        com.runecraft.skill.ThievingService.attempt(player, target, new com.runecraft.skill.ThievingService.Target(p.name(), p.level(),
                p.xp(), p.low(), p.high(), p.stunDamage(), com.runecraft.osrs.data.Thieving.pouchForNpc(p.creature())), out -> {
            for (TownMonsters.Loot l : p.always()) {
                out.accept(l.item(), amount(player, l));
            }
            if (!p.table().isEmpty()) {
                int total = 0;
                for (TownMonsters.Loot l : p.table()) {
                    total += l.weight();
                }
                int roll = player.getRandom().nextInt(total);
                for (TownMonsters.Loot l : p.table()) {
                    roll -= l.weight();
                    if (roll < 0) {
                        out.accept(l.item(), amount(player, l));
                        break;
                    }
                }
            }
        });
    }

    private static int amount(ServerPlayer player, TownMonsters.Loot l) {
        return l.min() + (l.max() > l.min() ? player.getRandom().nextInt(l.max() - l.min() + 1) : 0);
    }

}
