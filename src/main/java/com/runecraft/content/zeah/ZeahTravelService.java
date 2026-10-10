package com.runecraft.content.zeah;

import com.runecraft.RuneCraft;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.zeah.ZeahTravel;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import com.runecraft.world.LandmarkBuilder;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Travel between Zeah stops. Talking to a travel NPC (or using a minecart) while sneaking picks the
 * next destination; talking normally travels to the chosen one.
 */
final class ZeahTravelService {
    /** Chosen destination index per player and network. */
    private static final Map<String, Integer> CHOICE = new HashMap<>();

    private ZeahTravelService() {
    }

    static void talk(ServerPlayerEntity player, String npcName, ZeahTravel.Agent agent) {
        choose(player, npcName, agent.network(), agent.at());
    }

    /** A Lovakengj minecart: works out which station it belongs to from its position. */
    static void minecart(ServerPlayerEntity player, BlockPos pos) {
        int x = GielinorMap.toOsrsX(pos.getX());
        int y = GielinorMap.toOsrsY(pos.getZ());
        ZeahTravel.Stop here = ZeahTravel.nearestMinecart(x, y, id -> {
            PlaceDef p = ContentRegistry.place(id);
            return p == null ? null : new int[]{p.osrsX(), p.osrsY()};
        });
        if (here == null) {
            Msg.game(player, "The minecart doesn't seem to be connected to anything.");
            return;
        }
        choose(player, "Minecart conductor", ZeahTravel.MINECART, here.place());
    }

    private static void choose(ServerPlayerEntity player, String speaker, ZeahTravel.Network network, String from) {
        List<ZeahTravel.Stop> dests = network.destinations(from);
        if (dests.isEmpty()) {
            return;
        }
        String key = player.getUuid() + "|" + network.id() + "|" + from;
        int index = CHOICE.getOrDefault(key, 0) % dests.size();
        if (player.isSneaking()) {
            index = (index + 1) % dests.size();
            CHOICE.put(key, index);
            QuestApi.say(player, speaker, "Next stop: " + dests.get(index).name() + ". Talk to me again to go there.");
            return;
        }
        ZeahTravel.Stop to = dests.get(index);
        if (network.fare() > 0) {
            if (!Inv.has(player, Inv.COINS, network.fare())) {
                QuestApi.say(player, speaker, "The fare is " + network.fare() + " coins.");
                return;
            }
            Inv.remove(player, Inv.COINS, network.fare());
        }
        Msg.game(player, network.verb() + " " + to.name() + ". (Sneak and talk to choose another destination.)");
        travel(player, to.place());
    }

    /** Sends a player to a module place or landmark, building it first if needed. */
    static void travel(ServerPlayerEntity player, String place) {
        Landmark landmark = Landmark.byName(place);
        if (landmark != null) {
            Gielinor.teleport(player, landmark);
            return;
        }
        PlaceDef p = ContentRegistry.place(place);
        ServerWorld world = Gielinor.world(player.getServer());
        if (p == null || world == null) {
            RuneCraft.LOGGER.warn("Zeah travel: unknown destination {}", place);
            return;
        }
        LandmarkBuilder.buildNow(world, p);
        BlockPos pos = Gielinor.safeSpot(world, p.blockX() + 3, p.blockZ() + 3);
        player.teleport(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYaw(), player.getPitch());
    }
}
