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

    /** Whether a player has built a fairy ring in their house (set by the extras module's house): DIQ needs one. */
    public static java.util.function.Predicate<ServerPlayerEntity> houseFairyRing = p -> false;

    /**
     * DIQ's arrival: into the player's house beside its fairy ring (set by the extras module's house; until then a plain
     * Teleport to House).
     */
    public static java.util.function.Consumer<ServerPlayerEntity> houseRingTeleport =
            p -> com.runecraft.content.systems.SysTeleport.houseTeleport.accept(p);

    private IslandsTravelService() {
    }

    /** Block position (x, z) of a stop's ring or tree, or null if its place is unknown. */
    static int[] position(IslandsTravel.Stop stop) {
        IslandsTravel.DungeonRing ring = dungeonRing(stop);
        if (ring != null) {
            return new int[]{ring.x(), ring.z()};
        }
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
            IslandsTravel.DungeonRing ring = dungeonRing(s);
            boolean level = ring == null ? pos.getY() > com.runecraft.osrs.content.dungeons.Dungeons.DEEP_TOP
                    : Math.abs(pos.getY() - (ring.floorY() + 1)) <= 2;
            if (s.hasBlock() && at != null && level && Math.abs(at[0] - pos.getX()) <= 1 && Math.abs(at[1] - pos.getZ()) <= 1) {
                return s;
            }
        }
        return null;
    }

    /** Uses a fairy ring or mushtree at {@code pos} (also the house fairy ring, which has no stop of its own). */
    public static void use(ServerPlayerEntity player, BlockPos pos, IslandsTravel.Network network) {
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
        // OSRS: DIQ "only works if the player has a fairy ring in their house".
        if (IslandsTravel.HOUSE_PLACE.equals(to.place()) && !houseFairyRing.test(player)) {
            Msg.game(player, "The " + to.code() + " fairy ring code only works if you have built a fairy ring in your house.");
            return;
        }
        // BLQ (Yu'biusk) is attuned during Land of the Goblins (OSRS: part-way through the quest). approx: once started
        for (IslandsTravel.Stop end : new IslandsTravel.Stop[]{here, to}) {
            if (end != null && IslandsTravel.YUBIUSK.equals(end.code())
                    && ContentRegistry.quest(IslandsTravel.YUBIUSK_QUEST) != null
                    && QuestApi.stage(player, IslandsTravel.YUBIUSK_QUEST) < 1 && !QuestApi.isComplete(player, IslandsTravel.YUBIUSK_QUEST)) {
                Msg.game(player, "The " + end.code() + " fairy ring only works for those who have started "
                        + ContentRegistry.quest(IslandsTravel.YUBIUSK_QUEST).displayName() + ".");
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
            IslandsTravel.DungeonRing ring = null;
            for (IslandsTravel.DungeonRing d : IslandsTravel.DUNGEON_RINGS) {
                if (IslandsTravel.ringPlace(d.code()).equals(p.id())) {
                    ring = d;
                }
            }
            IslandsTravel.DungeonRing underground = ring;
            LandmarkBuilder.registerCustom(p.id(), (world, plaza) -> {
                if (underground == null) {
                    layRing(world, ground(world, x, z));
                } else {
                    layDungeonRing(world, underground); // on the dungeon's floor, once the dungeon is carved
                }
            });
        }
        // The rings of the realms built as places elsewhere (BJR, DIR, DIP, DLS, BLQ): beside their place.
        for (IslandsTravel.Stop s : IslandsTravel.FAIRY_RINGS.stops()) {
            if (IslandsTravel.REALM_RINGS.contains(s.code())) {
                LandmarkBuilder.registerCustom(s.place(), (world, plaza) -> ensureRealmRing(world, s));
            }
        }
    }

    /**
     * Lays a {@link IslandsTravel#REALM_RINGS} ring beside its place unless it is already there (worlds whose place was built
     * before the ring existed get it when a traveller arrives).
     */
    static BlockPos ensureRealmRing(ServerWorld world, IslandsTravel.Stop stop) {
        int[] at = position(stop);
        if (at == null) {
            return null;
        }
        net.minecraft.block.Block ring = net.minecraft.registry.Registries.BLOCK.get(RuneCraft.resolve(IslandsBlocks.FAIRY_RING));
        BlockPos floor = ground(world, at[0], at[1]);
        for (int dy = -3; dy <= 2; dy++) {
            if (world.getBlockState(floor.up(dy + 1)).isOf(ring)) {
                return floor.up(dy);
            }
        }
        layRing(world, floor);
        return floor;
    }

    /** Lays a dungeon realm's ring on its dungeon's floor (carving the dungeon's chunks first), unless it is there. */
    static void layDungeonRing(ServerWorld world, IslandsTravel.DungeonRing ring) {
        StructurePlacer.applyAround(world, ring.x(), ring.z(), 16);
        net.minecraft.block.Block block = net.minecraft.registry.Registries.BLOCK.get(RuneCraft.resolve(IslandsBlocks.FAIRY_RING));
        if (!world.getBlockState(new BlockPos(ring.x(), ring.floorY() + 1, ring.z())).isOf(block)) {
            layRing(world, new BlockPos(ring.x(), ring.floorY(), ring.z()));
        }
    }

    /** The top solid block at (x, z), never below the sea surface. */
    static BlockPos ground(ServerWorld world, int x, int z) {
        world.getChunk(x >> 4, z >> 4);
        // The OCEAN_FLOOR heightmap stops on tree tops (leaves and logs block motion), which used to put rings in the canopy
        // out of reach of their travellers: walk down through trees, plants and air to the real ground.
        BlockPos.Mutable pos = new BlockPos.Mutable(x, world.getTopY(net.minecraft.world.Heightmap.Type.OCEAN_FLOOR, x, z) - 1, z);
        while (pos.getY() > world.getSeaLevel() - 1) {
            net.minecraft.block.BlockState here = world.getBlockState(pos);
            boolean through = here.isIn(net.minecraft.registry.tag.BlockTags.LOGS) || here.isIn(net.minecraft.registry.tag.BlockTags.LEAVES)
                    || here.isAir() || (here.isReplaceable() && here.getFluidState().isEmpty());
            if (!through) {
                break;
            }
            pos.move(0, -1, 0);
        }
        return new BlockPos(x, Math.max(pos.getY(), world.getSeaLevel() - 1), z);
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

    /** The dungeon ring a stop stands for (a ring carved into a dungeon realm), or null. */
    static IslandsTravel.DungeonRing dungeonRing(IslandsTravel.Stop stop) {
        IslandsTravel.DungeonRing ring = IslandsTravel.dungeonRing(stop.code());
        return ring != null && IslandsTravel.ringPlace(ring.code()).equals(stop.place()) ? ring : null;
    }

    /** Travels straight to a fairy ring code's destination, without a staff or dial (the self-test). */
    public static void travelTo(ServerPlayerEntity player, String code) {
        IslandsTravel.Stop to = IslandsTravel.FAIRY_RINGS.stop(code);
        if (to != null) {
            travel(player, to);
        }
    }

    static void travel(ServerPlayerEntity player, IslandsTravel.Stop to) {
        if (IslandsTravel.HOUSE_PLACE.equals(to.place())) {
            // DIQ: the player-owned house's ring (the extras module's house registers the teleport).
            houseRingTeleport.accept(player);
            return;
        }
        IslandsTravel.DungeonRing ring = dungeonRing(to);
        if (ring != null) {
            ServerWorld world = Gielinor.world(player.getServer());
            if (world == null) {
                return;
            }
            layDungeonRing(world, ring);
            int[] a = IslandsTravel.dungeonArrival(ring);
            player.teleport(world, a[0] + 0.5, a[1], a[2] + 0.5, player.getYaw(), player.getPitch());
            return;
        }
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
        if (IslandsTravel.REALM_RINGS.contains(to.code())) {
            // Step out beside the ring (on its mycelium, east of the ring block), wherever the ground there lies.
            BlockPos ring = ensureRealmRing(world, to);
            if (ring != null) {
                player.teleport(world, ring.getX() + 1.5, ring.getY() + 1, ring.getZ() + 0.5, player.getYaw(), player.getPitch());
                return;
            }
        }
        BlockPos pos = Gielinor.safeSpot(world, x + 2, z);
        player.teleport(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYaw(), player.getPitch());
    }
}
