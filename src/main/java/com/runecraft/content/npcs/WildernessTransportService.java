package com.runecraft.content.npcs;

import com.runecraft.content.GameEvents;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.npcs.WildernessTransport;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import com.runecraft.world.LandmarkBuilder;
import com.runecraft.world.NpcFloor;
import com.runecraft.world.StructurePlacer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The Wilderness levers and teleport obelisks ({@link WildernessTransport}): lays them where their bare places are built,
 * pulls levers and runs the obelisks' countdown.
 */
public final class WildernessTransportService {
    private static final int MC_TICKS_PER_GAME_TICK = 12;
    /** Server tick at which each activated obelisk fires. */
    private static final Map<WildernessTransport.Obelisk, Long> ARMED = new HashMap<>();
    /** Server tick until which an obelisk that just fired stays inert. */
    private static final Map<WildernessTransport.Obelisk, Long> COOLING = new HashMap<>();
    private static final Random RANDOM = new Random();

    private WildernessTransportService() {
    }

    static void init() {
        for (WildernessTransport.Lever l : WildernessTransport.LEVERS) {
            LandmarkBuilder.registerCustom(l.placeId(), (world, centre) -> layLever(world, l));
        }
        for (WildernessTransport.Obelisk o : WildernessTransport.OBELISKS) {
            LandmarkBuilder.registerCustom(o.placeId(), (world, centre) -> layObelisk(world, o));
        }
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (hand != Hand.MAIN_HAND || !Gielinor.is(world)) {
                return ActionResult.PASS;
            }
            BlockPos pos = hit.getBlockPos();
            int tx = GielinorMap.toOsrsX(pos.getX());
            int ty = GielinorMap.toOsrsY(pos.getZ());
            BlockState state = world.getBlockState(pos);
            WildernessTransport.Lever lever = state.isOf(Blocks.LEVER) ? WildernessTransport.leverAt(tx, ty) : null;
            WildernessTransport.Obelisk obelisk = lever == null && pillar(state) ? WildernessTransport.pillarAt(tx, ty) : null;
            if (lever == null && obelisk == null) {
                return ActionResult.PASS;
            }
            if (world.isClient() || !(player instanceof ServerPlayerEntity sp)) {
                return ActionResult.SUCCESS;
            }
            if (lever != null) {
                pull(sp, lever);
            } else {
                activate(sp, obelisk);
            }
            return ActionResult.SUCCESS;
        });
        ServerTickEvents.END_SERVER_TICK.register(WildernessTransportService::tick);
    }

    private static boolean pillar(BlockState state) {
        return state.isOf(Blocks.POLISHED_BLACKSTONE) || state.isOf(Blocks.CHISELED_POLISHED_BLACKSTONE);
    }

    // ------------------------------------------------------------------ levers

    /** A lever on the floor of its tile (inside the building there, if any). */
    private static void layLever(ServerWorld world, WildernessTransport.Lever l) {
        int x = GielinorMap.toBlockX(l.x());
        int z = GielinorMap.toBlockZ(l.y());
        StructurePlacer.applyAround(world, x, z, 16);
        BlockPos feet = NpcFloor.spot(world, x, z, false);
        world.setBlockState(feet, StructurePlacer.parse("minecraft:lever[face=floor,facing=north]"));
    }

    /** Where a lever's traveller lands: the destination tile's other block, on its floor (the Mage Bank's cavern floor). */
    static BlockPos arrival(ServerWorld world, WildernessTransport.Lever l) {
        WildernessTransport.Lever there = WildernessTransport.leverAt(l.toX(), l.toY());
        if (there != null && there.id().equals("mage_bank")) {
            int[] a = WildernessTransport.mageBankArrival();
            StructurePlacer.applyAround(world, a[0], a[2], 24);
            return new BlockPos(a[0], a[1], a[2]);
        }
        int x = GielinorMap.toBlockX(l.toX()) + 1;
        int z = GielinorMap.toBlockZ(l.toY()) + 1;
        // The lever beside the landing spot (the Deserted Keep's, Ardougne's, the Mage Arena's) is laid first.
        for (WildernessTransport.Lever near : WildernessTransport.LEVERS) {
            PlaceDef place = ContentRegistry.place(near.placeId());
            if (place != null && Math.abs(near.x() - l.toX()) <= 2 && Math.abs(near.y() - l.toY()) <= 2) {
                LandmarkBuilder.buildNow(world, place);
            }
        }
        StructurePlacer.applyAround(world, x, z, 16);
        return NpcFloor.spot(world, x, z, false);
    }

    static void pull(ServerPlayerEntity p, WildernessTransport.Lever l) {
        if (com.runecraft.combat.Teleblock.blocked(p)) {
            return;
        }
        ServerWorld world = Gielinor.world(p.getServer());
        if (world == null) {
            return;
        }
        Msg.action(p, "You pull the lever...");
        BlockPos to = arrival(world, l);
        world.spawnParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 1, 0.4, 0.4);
        p.teleport(world, to.getX() + 0.5, NpcFloor.feetY(world, to), to.getZ() + 0.5, p.getYaw(), p.getPitch());
        world.playSound(null, to, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1f, 1f);
        Msg.game(p, "...and teleport to " + l.toName() + ".");
        GameEvents.fireActivity(p, "lever:" + l.id());
    }

    // ------------------------------------------------------------------ obelisks

    /** Levels the 5x5-tile square, with a stone platform and a pillar on each corner tile. */
    private static void layObelisk(ServerWorld world, WildernessTransport.Obelisk o) {
        int x1 = GielinorMap.toBlockX(o.x() - 2);
        int x2 = GielinorMap.toBlockX(o.x() + 2) + 1;
        int z1 = GielinorMap.toBlockZ(o.y() + 2);
        int z2 = GielinorMap.toBlockZ(o.y() - 2) + 1;
        int cx = GielinorMap.toBlockX(o.x());
        int cz = GielinorMap.toBlockZ(o.y());
        StructurePlacer.applyAround(world, cx, cz, 16);
        world.getChunk(cx >> 4, cz >> 4);
        int gy = Math.max(world.getTopY(Heightmap.Type.OCEAN_FLOOR, cx, cz) - 1, world.getSeaLevel());
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                world.setBlockState(new BlockPos(x, gy, z), (edge ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS).getDefaultState());
                for (int y = gy + 1; y <= gy + 5; y++) {
                    world.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState());
                }
            }
        }
        for (int[] c : new int[][]{{x1, z1}, {x2 - 1, z1}, {x1, z2 - 1}, {x2 - 1, z2 - 1}}) {
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    for (int y = gy + 1; y <= gy + 2; y++) {
                        world.setBlockState(new BlockPos(c[0] + dx, y, c[1] + dz), Blocks.POLISHED_BLACKSTONE.getDefaultState());
                    }
                    world.setBlockState(new BlockPos(c[0] + dx, gy + 3, c[1] + dz), Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState());
                }
            }
        }
    }

    static void activate(ServerPlayerEntity p, WildernessTransport.Obelisk o) {
        long now = p.getServer().getTicks();
        if (ARMED.containsKey(o)) {
            Msg.game(p, "The obelisk is already active.");
            return;
        }
        if (COOLING.getOrDefault(o, 0L) > now) {
            Msg.game(p, "The obelisk's power is still recovering.");
            return;
        }
        ARMED.put(o, now + (long) WildernessTransport.OBELISK_DELAY_TICKS * MC_TICKS_PER_GAME_TICK);
        Msg.action(p, "You activate the ancient obelisk...");
        GameEvents.fireActivity(p, "obelisk:" + o.level());
        if (p.getWorld() instanceof ServerWorld world) {
            world.playSound(null, p.getBlockPos(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1f, 1f);
        }
    }

    private static void tick(MinecraftServer server) {
        if (ARMED.isEmpty()) {
            return;
        }
        long now = server.getTicks();
        ServerWorld world = Gielinor.world(server);
        for (WildernessTransport.Obelisk o : new ArrayList<>(ARMED.keySet())) {
            long at = ARMED.get(o);
            if (world != null && at - now > 0 && (at - now) % MC_TICKS_PER_GAME_TICK == 0) {
                world.spawnParticles(ParticleTypes.ENCHANT, GielinorMap.toBlockX(o.x()) + 1, world.getTopY(Heightmap.Type.MOTION_BLOCKING,
                        GielinorMap.toBlockX(o.x()), GielinorMap.toBlockZ(o.y())) + 1, GielinorMap.toBlockZ(o.y()) + 1, 40, 2.5, 1, 2.5, 0.5);
            }
            if (at > now) {
                continue;
            }
            ARMED.remove(o);
            COOLING.put(o, now + (long) WildernessTransport.OBELISK_COOLDOWN_TICKS * MC_TICKS_PER_GAME_TICK);
            if (world != null) {
                fire(world, o, WildernessTransport.destination(o, RANDOM));
            }
        }
    }

    /**
     * Sends everyone on the obelisk's platform to the same spot of the destination obelisk's platform, except players under
     * a Tele Block (OSRS: "The obelisk will only teleport players who are not under the effects of a Tele Block").
     */
    static int fire(ServerWorld world, WildernessTransport.Obelisk from, WildernessTransport.Obelisk to) {
        List<ServerPlayerEntity> riders = new ArrayList<>();
        for (ServerPlayerEntity p : world.getPlayers()) {
            BlockPos at = p.getBlockPos();
            if (WildernessTransport.platformAt(GielinorMap.toOsrsX(at.getX()), GielinorMap.toOsrsY(at.getZ())) == from
                    && !com.runecraft.combat.Teleblock.blocked(p)) {
                riders.add(p);
            }
        }
        if (riders.isEmpty()) {
            return 0;
        }
        PlaceDef place = ContentRegistry.place(to.placeId());
        if (place != null) {
            LandmarkBuilder.buildNow(world, place);
        }
        int dx = GielinorMap.toBlockX(to.x()) - GielinorMap.toBlockX(from.x());
        int dz = GielinorMap.toBlockZ(to.y()) - GielinorMap.toBlockZ(from.y());
        StructurePlacer.applyAround(world, GielinorMap.toBlockX(to.x()), GielinorMap.toBlockZ(to.y()), 16);
        for (ServerPlayerEntity p : riders) {
            BlockPos spot = Gielinor.safeSpot(world, p.getBlockPos().getX() + dx, p.getBlockPos().getZ() + dz);
            p.teleport(world, p.getX() + dx, spot.getY(), p.getZ() + dz, p.getYaw(), p.getPitch());
            world.spawnParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 1, 0.4, 0.4);
            Msg.action(p, "Ancient magic teleports you somewhere in the Wilderness.");
            GameEvents.fireActivity(p, "obelisk_teleport:" + to.level());
        }
        world.playSound(null, new BlockPos(GielinorMap.toBlockX(to.x()), world.getSeaLevel(), GielinorMap.toBlockZ(to.y())),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1f, 1f);
        return riders.size();
    }
}
