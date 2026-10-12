package com.runecraft.content.npcs;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.MonsterStats;
import com.runecraft.content.ModuleHooks;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.menu.MenuService;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Chance;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.npcs.ShopDef;
import com.runecraft.osrs.content.npcs.Shops;
import com.runecraft.osrs.content.npcs.TownMonsters;
import com.runecraft.osrs.content.npcs.TownNpc;
import com.runecraft.osrs.content.npcs.Townsfolk;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.quest.QuestScripts;
import com.runecraft.skill.Stations;
import com.runecraft.skill.Stun;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.LandmarkBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

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
        for (TownNpc n : Townsfolk.all()) {
            QuestScripts.onTalk(n.id(), (player, entity) -> talk(player, entity, n));
        }
        for (String placeId : NpcPlacement.placeIds()) {
            LandmarkBuilder.registerCustom(placeId, (world, centre) -> NpcPlacement.settlePlace(world, centre, placeId));
        }
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (hand != Hand.MAIN_HAND) {
                return ActionResult.PASS;
            }
            // Sneak + right-click: "Trade" with any shopkeeper, including characters from other modules.
            if (entity instanceof QuestNpcEntity npc && player.isSneaking()) {
                ShopDef shop = shopOf(npc.npcId());
                if (shop == null) {
                    return ActionResult.PASS;
                }
                if (!world.isClient() && player instanceof ServerPlayerEntity sp) {
                    ShopService.open(sp, shop.id(), npc.getPos());
                }
                return ActionResult.SUCCESS;
            }
            // Right-click an attackable townsperson: "Pickpocket".
            if (entity instanceof OsrsMonsterEntity monster) {
                TownMonsters.Pickpocket target = pickpocketTarget(monster);
                if (target == null) {
                    return ActionResult.PASS;
                }
                if (!world.isClient() && player instanceof ServerPlayerEntity sp) {
                    pickpocket(sp, monster, target);
                }
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ShopService.tick(server);
            TownSpawner.tick(server);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ShopService.forget(handler.player.getUuid()));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                CommandManager.literal("npcs")
                        .then(CommandManager.literal("travel")
                                .then(CommandManager.argument("npc", StringArgumentType.word())
                                        .then(CommandManager.argument("destination", IntegerArgumentType.integer(0))
                                                .executes(ctx -> {
                                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
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

    private static void talk(ServerPlayerEntity player, QuestNpcEntity entity, TownNpc n) {
        String name = entity.getName().getString();
        switch (n.role()) {
            case SHOP -> {
                ShopDef shop = Shops.get(n.shop());
                if (shop != null) {
                    QuestApi.say(player, name, shop.greeting());
                    ShopService.open(player, shop.id(), entity.getPos());
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
                MenuService.open(player, MenuType.TANNING, entity.getBlockPos(), false);
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

    private static void pickpocket(ServerPlayerEntity player, OsrsMonsterEntity target, TownMonsters.Pickpocket p) {
        if (!Stun.check(player)) {
            return;
        }
        int level = Players.data(player).skills().currentLevel(Skill.THIEVING);
        String who = p.name().startsWith("TzHaar") ? p.name() : p.name().toLowerCase(java.util.Locale.ROOT);
        if (level < p.level()) {
            Msg.game(player, "You need a Thieving level of " + p.level() + " to pickpocket the " + who + ".");
            return;
        }
        Msg.action(player, "You attempt to pick the " + who + "'s pocket.");
        player.swingHand(Hand.MAIN_HAND, true);
        if (player.getRandom().nextDouble() < com.runecraft.skill.ThievingService.pickpocketChance(player,
                Chance.lerp(level, p.low(), p.high()))) {
            for (TownMonsters.Loot l : p.always()) {
                give(player, l);
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
                        give(player, l);
                        break;
                    }
                }
            }
            XpService.add(player, Skill.THIEVING, p.xp());
            Msg.action(player, "You pick the " + who + "'s pocket.");
            ServerNetworking.sync(player);
        } else {
            Msg.game(player, "You fail to pick the " + who + "'s pocket.");
            Msg.red(player, "You have been stunned!");
            target.swingHand(Hand.MAIN_HAND);
            CombatHandler.applyDamage(target, player, 1 + player.getRandom().nextInt(p.stunDamage()));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 4, false, false));
            Stun.apply(player, 8);
        }
    }

    private static void give(ServerPlayerEntity player, TownMonsters.Loot l) {
        int n = l.min() + (l.max() > l.min() ? player.getRandom().nextInt(l.max() - l.min() + 1) : 0);
        Inv.give(player, l.item(), n);
    }
}
