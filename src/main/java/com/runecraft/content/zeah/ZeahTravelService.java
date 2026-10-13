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
        if (network.quest() != null && com.runecraft.osrs.content.ContentRegistry.quest(network.quest()) != null
                && !QuestApi.isComplete(player, network.quest())) {
            QuestApi.say(player, speaker, "You need to complete "
                    + com.runecraft.osrs.content.ContentRegistry.quest(network.quest()).displayName() + " first.");
            return;
        }
        int fare = network.fare(network.freeAfter() != null && QuestApi.isComplete(player, network.freeAfter()));
        if (fare > 0) {
            if (!Inv.has(player, Inv.COINS, fare)) {
                QuestApi.say(player, speaker, "The fare is " + fare + " coins.");
                return;
            }
            Inv.remove(player, Inv.COINS, fare);
        }
        Msg.game(player, network.verb() + " " + to.name() + ". (Sneak and talk to choose another destination.)");
        travel(player, to);
    }

    /** Sends a player to a stop: its OSRS tile if it has one, else next to its place. */
    static void travel(ServerPlayerEntity player, ZeahTravel.Stop to) {
        if (to.x() == 0) {
            travel(player, to.place());
            return;
        }
        ServerWorld world = Gielinor.world(player.getServer());
        if (world == null) {
            return;
        }
        PlaceDef p = ContentRegistry.place(to.place());
        if (p != null) {
            LandmarkBuilder.buildNow(world, p);
        }
        BlockPos pos = Gielinor.safeSpot(world, GielinorMap.toBlockX(to.x()), GielinorMap.toBlockZ(to.y()));
        player.teleport(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYaw(), player.getPitch());
    }

    /** Lays a minecart at each stand-alone Lovakengj station when its place is built. */
    static void registerMinecartStations() {
        for (String id : com.runecraft.osrs.content.zeah.ZeahPlaces.MINECART_STATIONS) {
            PlaceDef p = ContentRegistry.place(id);
            if (p == null) {
                continue;
            }
            // Place centres snap to their chunk's centre: the cart goes beside the station's own tile.
            int x = GielinorMap.toBlockX(p.osrsX()) + 2;
            int z = GielinorMap.toBlockZ(p.osrsY());
            LandmarkBuilder.registerCustom(id, (world, centre) -> {
                world.getChunk(x >> 4, z >> 4);
                int y = Math.max(world.getTopY(net.minecraft.world.Heightmap.Type.OCEAN_FLOOR, x, z), world.getSeaLevel());
                net.minecraft.block.Block cart = net.minecraft.registry.Registries.BLOCK.get(
                        RuneCraft.resolve(com.runecraft.osrs.content.zeah.ZeahBlocks.MINECART));
                world.setBlockState(new BlockPos(x, y, z), cart.getDefaultState());
                world.setBlockState(new BlockPos(x, y + 1, z), net.minecraft.block.Blocks.AIR.getDefaultState());
            });
        }
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
