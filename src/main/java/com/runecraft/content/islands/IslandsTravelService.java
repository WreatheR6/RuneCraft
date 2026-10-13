package com.runecraft.content.islands;

import com.runecraft.RuneCraft;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.islands.IslandsBlocks;
import com.runecraft.osrs.content.islands.IslandsTravel;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import com.runecraft.world.LandmarkBuilder;
import com.runecraft.world.StructurePlacer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Fairy rings and Magic Mushtrees: sneak-click to turn the dial to the next destination, click to
 * travel to the chosen one.
 */
public final class IslandsTravelService {
    /** Chosen destination code per player and network. */
    private static final Map<String, String> DIAL = new HashMap<>();

    /** Whether a player may use fairy rings without a staff (set by the systems module: Lumbridge elite diary). */
    public static java.util.function.Predicate<ServerPlayerEntity> staffless = p -> false;

    private IslandsTravelService() {
    }

    /** Block position (x, z) of a stop's ring or tree, or null if its place is unknown. */
    static int[] position(IslandsTravel.Stop stop) {
        PlaceDef p = ContentRegistry.place(stop.place());
        if (p != null) {
            return new int[]{p.blockX() + stop.dx(), p.blockZ() + stop.dz()};
        }
        Landmark l = Landmark.byName(stop.place());
        return l == null ? null : new int[]{l.blockX() + stop.dx(), l.blockZ() + stop.dz()};
    }

    /** The stop whose block was clicked. */
    static IslandsTravel.Stop here(IslandsTravel.Network network, BlockPos pos) {
        for (IslandsTravel.Stop s : network.stops()) {
            int[] at = position(s);
            if (s.hasBlock() && at != null && Math.abs(at[0] - pos.getX()) <= 1 && Math.abs(at[1] - pos.getZ()) <= 1) {
                return s;
            }
        }
        return null;
    }

    static void use(ServerPlayerEntity player, BlockPos pos, IslandsTravel.Network network) {
        boolean fairy = network == IslandsTravel.FAIRY_RINGS;
        String quest = network.quest();
        if (quest != null && QuestApi.stage(player, quest) < 1 && !QuestApi.isComplete(player, quest)) {
            Msg.game(player, "The fairy ring only works for those who have started Fairytale II - Cure a Queen.");
            return;
        }
        // OSRS: fairy rings only work while wielding a Dramen or Lunar staff.
        String staff = com.runecraft.util.Inv.idOf(player.getMainHandStack());
        if (fairy && !"dramen_staff".equals(staff) && !"lunar_staff".equals(staff) && !staffless.test(player)) {
            Msg.game(player, "You need to wield a Dramen or Lunar staff to use the fairy rings.");
            return;
        }
        IslandsTravel.Stop here = here(network, pos);
        String hereCode = here == null ? "" : here.code();
        String key = player.getUuid() + "|" + network.id();
        String chosen = DIAL.get(key);
        if (chosen == null || chosen.equals(hereCode) || network.stop(chosen) == null) {
            chosen = IslandsTravel.next(network, hereCode, hereCode).code();
        }
        if (player.isSneaking()) {
            IslandsTravel.Stop next = IslandsTravel.next(network, chosen, hereCode);
            DIAL.put(key, next.code());
            Msg.game(player, (fairy ? "You turn the dial to " + next.code() + ": " : "The mushtree will take you to ") + next.name()
                    + ". (Use it again to travel.)");
            return;
        }
        DIAL.put(key, chosen);
        IslandsTravel.Stop to = network.stop(chosen);
        for (IslandsTravel.Stop end : new IslandsTravel.Stop[]{here, to}) {
            if (end != null && end.quest() != null && ContentRegistry.quest(end.quest()) != null
                    && !QuestApi.isComplete(player, end.quest())) {
                Msg.game(player, "You need to complete " + ContentRegistry.quest(end.quest()).displayName() + " to use the "
                        + end.code() + " fairy ring.");
                return;
            }
        }
        Msg.game(player, fairy ? "You step into the fairy ring... " + to.name() + "." : "The mushtree's spores carry you to " + to.name()
                + ". (Sneak and use it to choose another destination.)");
        travel(player, to);
    }

    /** Lays the stand-alone fairy rings (mycelium, the ring, mushrooms) where their bare places are built. */
    static void registerRingBuilders() {
        for (PlaceDef p : IslandsTravel.ringPlaces()) {
            // The place centre is its chunk's centre; the ring stands on its own OSRS tile in that chunk.
            int x = com.runecraft.osrs.GielinorMap.toBlockX(p.osrsX());
            int z = com.runecraft.osrs.GielinorMap.toBlockZ(p.osrsY());
            LandmarkBuilder.registerCustom(p.id(), (world, plaza) -> layRing(world, ground(world, x, z)));
        }
    }

    /** The top solid block at (x, z), never below the sea surface. */
    static BlockPos ground(ServerWorld world, int x, int z) {
        world.getChunk(x >> 4, z >> 4);
        int top = world.getTopY(net.minecraft.world.Heightmap.Type.OCEAN_FLOOR, x, z) - 1;
        return new BlockPos(x, Math.max(top, world.getSeaLevel() - 1), z);
    }

    private static void layRing(ServerWorld world, BlockPos centre) {
        net.minecraft.block.Block ring = net.minecraft.registry.Registries.BLOCK.get(RuneCraft.resolve(IslandsBlocks.FAIRY_RING));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos floor = centre.add(dx, 0, dz);
                world.setBlockState(floor, net.minecraft.block.Blocks.MYCELIUM.getDefaultState());
                for (int up = 1; up <= 3; up++) {
                    world.setBlockState(floor.up(up), net.minecraft.block.Blocks.AIR.getDefaultState());
                }
                if (dx != 0 || dz != 0) {
                    world.setBlockState(floor.up(), (dx == 0 || dz == 0 ? net.minecraft.block.Blocks.RED_MUSHROOM
                            : net.minecraft.block.Blocks.BROWN_MUSHROOM).getDefaultState());
                }
            }
        }
        world.setBlockState(centre.up(), ring.getDefaultState());
    }

    static void travel(ServerPlayerEntity player, IslandsTravel.Stop to) {
        Landmark landmark = Landmark.byName(to.place());
        if (landmark != null && ContentRegistry.place(to.place()) == null) {
            Gielinor.teleport(player, landmark);
            return;
        }
        ServerWorld world = Gielinor.world(player.getServer());
        PlaceDef p = ContentRegistry.place(to.place());
        if (p == null || world == null) {
            RuneCraft.LOGGER.warn("Islands travel: unknown destination {}", to.place());
            return;
        }
        LandmarkBuilder.buildNow(world, p);
        int x = p.blockX() + to.dx();
        int z = p.blockZ() + to.dz();
        StructurePlacer.applyAround(world, x, z, 16);
        BlockPos pos = Gielinor.safeSpot(world, x + 2, z);
        player.teleport(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYaw(), player.getPitch());
    }
}
