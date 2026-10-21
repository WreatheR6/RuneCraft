package com.runecraft.content.npcs;

import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.npcs.TownNpc;
import com.runecraft.osrs.content.npcs.Townsfolk;
import com.runecraft.osrs.content.npcs.Travel;
import com.runecraft.network.ServerNetworking;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Boats, charter ships, gliders, magic carpets and canoes: a clickable destination list, then a paid trip. */
final class TravelService {
    private TravelService() {
    }

    private static String fare(Travel.Dest d) {
        return fare(d.price());
    }

    private static String fare(int price) {
        return price > 0 ? String.format("%,d", price) + " coins" : "free";
    }

    private static String cost(Travel.Dest d) {
        if (d.item() == null) {
            return fare(d);
        }
        String item = d.itemCount() + " " + d.item().replace('_', ' ');
        return d.price() > 0 ? fare(d) + " and " + item : item;
    }

    /** Shows the destinations of an NPC's route as clickable chat options. */
    static void offer(ServerPlayer player, TownNpc npc) {
        Travel.Route route = Travel.get(npc.travel());
        if (route == null) {
            return;
        }
        MutableComponent line = Component.literal("Where to? ").withStyle(ChatFormatting.GRAY);
        List<Travel.Dest> dests = route.dests();
        for (int i = 0; i < dests.size(); i++) {
            Travel.Dest d = dests.get(i);
            String req = d.skill() != null ? "\nRequires level " + d.level() + " " + d.skill().displayName() : "";
            for (String quest : d.quests()) {
                QuestInfo q = ContentRegistry.quest(quest);
                req += "\nRequires " + (q == null ? quest : q.displayName());
            }
            String command = "/npcs travel " + npc.id() + " " + i;
            String hover = capital(route.verb()) + " to " + d.name() + " (" + cost(d) + ")" + req;
            line.append(Component.literal("[" + d.name() + (d.price() > 0 || d.item() != null ? " - " + cost(d) : "") + "]").withStyle(s -> s
                    .withColor(ChatFormatting.GOLD)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover)))));
            line.append(Component.literal(" "));
        }
        player.displayClientMessage(line, false);
    }

    /** The fare charged: the route's (Jarvald waives his for Fremennik Trials) after the achievement diary perks. */
    private static int fare(ServerPlayer player, Travel.Route route, Travel.Dest d) {
        int base = Travel.fareFor(route.id(), d, q -> QuestApi.isComplete(player, q));
        var sp = com.runecraft.content.systems.SysState.of(player);
        boolean gloves = sp.wornId(com.runecraft.osrs.content.systems.Jewellery.Slot.HANDS).startsWith("karamja_gloves_");
        return com.runecraft.osrs.content.systems.DiaryRewards.travelFare(route.id(), d.name(), base, gloves,
                (area, tier) -> com.runecraft.osrs.content.systems.Diaries.tierComplete(sp.diaryDone, area, tier));
    }

    private static String capital(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Runs a chosen trip if the player is next to the NPC and meets the fare and requirements. */
    static void travel(ServerPlayer player, String npcId, int index) {
        TownNpc npc = Townsfolk.get(npcId);
        if (npc == null || npc.travel() == null) {
            return;
        }
        Travel.Route route = Travel.get(npc.travel());
        if (route == null || index < 0 || index >= route.dests().size()) {
            return;
        }
        boolean near = !player.level().getEntitiesOfClass(QuestNpcEntity.class, player.getBoundingBox().inflate(10),
                e -> npcId.equals(e.npcId())).isEmpty();
        if (!near) {
            Msg.game(player, "You need to be next to " + npc.name() + " to travel.");
            return;
        }
        Travel.Dest d = route.dests().get(index);
        if (d.skill() != null && Players.data(player).skills().level(d.skill()) < d.level()) {
            Msg.game(player, "You need a " + d.skill().displayName() + " level of " + d.level() + " to " + route.verb() + " there.");
            return;
        }
        for (String quest : d.quests()) {
            if (ContentRegistry.quest(quest) != null && !QuestApi.isComplete(player, quest)) {
                Msg.game(player, "You need to complete " + ContentRegistry.quest(quest).displayName() + " first.");
                return;
            }
        }
        int price = fare(player, route, d);
        if (d.item() != null && !Inv.has(player, d.item(), d.itemCount())) {
            Msg.game(player, "You need " + cost(d) + " to " + route.verb() + " to " + d.name() + ".");
            return;
        }
        if (price > 0 && !Inv.has(player, Inv.COINS, price)) {
            Msg.game(player, "You need " + fare(price) + " to " + route.verb() + " to " + d.name() + ".");
            return;
        }
        if (d.item() != null) {
            Inv.remove(player, d.item(), d.itemCount());
        }
        if (price > 0) {
            Inv.remove(player, Inv.COINS, price);
        }
        if (price > 0 || d.item() != null) {
            ServerNetworking.sync(player);
        }
        ServerLevel world = Gielinor.world(player.getServer());
        if (world == null) {
            return;
        }
        BlockPos pos = Gielinor.safeSpot(world, GielinorMap.toBlockX(d.x()), GielinorMap.toBlockZ(d.y()));
        player.teleportTo(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot(), player.getXRot());
        world.playSound(null, pos, SoundEvents.BOAT_PADDLE_WATER, SoundSource.PLAYERS, 1f, 1f);
        // Achievement diaries: "travel:" + route + ":" + destination ("travel:carpet_shantay:pollnivneach").
        com.runecraft.content.GameEvents.fireActivity(player, "travel:" + route.id() + ":"
                + com.runecraft.osrs.content.systems.Diaries.slug(d.name()));
        Msg.game(player, switch (route.verb()) {
            case "sail" -> "You board the ship and sail to " + d.name() + ".";
            case "fly" -> "You climb into the glider and fly to " + d.name() + ".";
            case "ride" -> "You climb onto the magic carpet and ride to " + d.name() + ".";
            case "paddle" -> "You chop a canoe and paddle down the river to " + d.name() + ".";
            case "float" -> "You feed a log to the burner and the balloon floats you to " + d.name() + ".";
            case "cart" -> "You climb into the cart and rattle along the tracks to " + d.name() + ".";
            case "travel" -> "You place your hands on the dry tough bark of the spirit tree, and feel a surge of energy run through"
                    + " your veins. You arrive at " + d.name() + ".";
            default -> "You travel to " + d.name() + ".";
        });
    }
}
