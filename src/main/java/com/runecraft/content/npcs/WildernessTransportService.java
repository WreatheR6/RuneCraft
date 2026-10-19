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
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
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
    /** Where an armed obelisk sends its riders when its first activator had set a destination (hard Wilderness Diary). */
    private static final Map<WildernessTransport.Obelisk, WildernessTransport.Obelisk> TARGET = new HashMap<>();
    /** Each player's chosen obelisk (Set Destination, hard Wilderness Diary). */
    private static final Map<java.util.UUID, WildernessTransport.Obelisk> CHOSEN = new HashMap<>();
    private static final Random RANDOM = new Random();

    private WildernessTransportService() {
    }

    static void init() {
        for (WildernessTransport.Lever l : WildernessTransport.LEVERS) {
            if (!WildernessTransport.inCavern(l)) {
                LandmarkBuilder.registerCustom(l.placeId(), (world, centre) -> layLever(world, l));
            }
        }
        for (WildernessTransport.Obelisk o : WildernessTransport.OBELISKS) {
            LandmarkBuilder.registerCustom(o.placeId(), (world, centre) -> layObelisk(world, o));
        }
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || !Gielinor.is(world)) {
                return InteractionResult.PASS;
            }
            BlockPos pos = hit.getBlockPos();
            int tx = GielinorMap.toOsrsX(pos.getX());
            int ty = GielinorMap.toOsrsY(pos.getZ());
            BlockState state = world.getBlockState(pos);
            WildernessTransport.Lever lever = state.is(Blocks.LEVER) ? WildernessTransport.leverAt(tx, ty) : null;
            WildernessTransport.Obelisk obelisk = lever == null && pillar(state) ? WildernessTransport.pillarAt(tx, ty) : null;
            if (lever == null && obelisk == null) {
                return InteractionResult.PASS;
            }
            if (world.isClientSide() || !(player instanceof ServerPlayer sp)) {
                return InteractionResult.SUCCESS;
            }
            if (lever != null) {
                pull(sp, lever);
            } else {
                activate(sp, obelisk);
            }
            return InteractionResult.SUCCESS;
        });
        ServerTickEvents.END_SERVER_TICK.register(WildernessTransportService::tick);
    }

    private static boolean pillar(BlockState state) {
        return state.is(Blocks.POLISHED_BLACKSTONE) || state.is(Blocks.CHISELED_POLISHED_BLACKSTONE);
    }

    // ------------------------------------------------------------------ levers

    /** A lever on the floor of its tile (inside the building there, if any). */
    private static void layLever(ServerLevel world, WildernessTransport.Lever l) {
        int x = GielinorMap.toBlockX(l.x());
        int z = GielinorMap.toBlockZ(l.y());
        StructurePlacer.applyAround(world, x, z, 16);
        BlockPos feet = NpcFloor.spot(world, x, z, false);
        world.setBlockAndUpdate(feet, StructurePlacer.parse("minecraft:lever[face=floor,facing=north]"));
    }

    /** Where a lever's traveller lands: the destination tile's other block, on its floor (the Mage Bank's cavern floor). */
    static BlockPos arrival(ServerLevel world, WildernessTransport.Lever l) {
        // The Mage Bank, the King Black Dragon's lair and its entrance chamber are carved caverns: land on their floor.
        int[] a = WildernessTransport.cavernArrival(l);
        if (a != null) {
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

    /** Pulls a lever: sends the player to its paired lever (public for the self-test). */
    public static void pull(ServerPlayer p, WildernessTransport.Lever l) {
        if (com.runecraft.combat.Teleblock.blocked(p)) {
            return;
        }
        ServerLevel world = Gielinor.world(p.getServer());
        if (world == null) {
            return;
        }
        Msg.action(p, "You pull the lever...");
        BlockPos to = arrival(world, l);
        world.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 1, 0.4, 0.4);
        p.teleportTo(world, to.getX() + 0.5, NpcFloor.feetY(world, to), to.getZ() + 0.5, p.getYRot(), p.getXRot());
        world.playSound(null, to, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1f);
        Msg.game(p, "...and teleport to " + l.toName() + ".");
        GameEvents.fireActivity(p, "lever:" + l.id());
    }

    // ------------------------------------------------------------------ obelisks

    /** Levels the 5x5-tile square, with a stone platform and a pillar on each corner tile. */
    private static void layObelisk(ServerLevel world, WildernessTransport.Obelisk o) {
        int x1 = GielinorMap.toBlockX(o.x() - 2);
        int x2 = GielinorMap.toBlockX(o.x() + 2) + 1;
        int z1 = GielinorMap.toBlockZ(o.y() + 2);
        int z2 = GielinorMap.toBlockZ(o.y() - 2) + 1;
        int cx = GielinorMap.toBlockX(o.x());
        int cz = GielinorMap.toBlockZ(o.y());
        StructurePlacer.applyAround(world, cx, cz, 16);
        world.getChunk(cx >> 4, cz >> 4);
        int gy = Math.max(world.getHeight(Heightmap.Types.OCEAN_FLOOR, cx, cz) - 1, world.getSeaLevel());
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                world.setBlockAndUpdate(new BlockPos(x, gy, z), (edge ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS).defaultBlockState());
                for (int y = gy + 1; y <= gy + 5; y++) {
                    world.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        for (int[] c : new int[][]{{x1, z1}, {x2 - 1, z1}, {x1, z2 - 1}, {x2 - 1, z2 - 1}}) {
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    for (int y = gy + 1; y <= gy + 2; y++) {
                        world.setBlockAndUpdate(new BlockPos(c[0] + dx, y, c[1] + dz), Blocks.POLISHED_BLACKSTONE.defaultBlockState());
                    }
                    world.setBlockAndUpdate(new BlockPos(c[0] + dx, gy + 3, c[1] + dz), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
                }
            }
        }
    }

    /** Whether a player has completed the hard Wilderness Diary (Set Destination on the obelisks). */
    static boolean hardDiary(ServerPlayer p) {
        return com.runecraft.osrs.content.systems.Diaries.tierComplete(com.runecraft.content.systems.SysState.of(p).diaryDone,
                "wilderness", com.runecraft.osrs.content.systems.Diaries.Tier.HARD);
    }

    static void activate(ServerPlayer p, WildernessTransport.Obelisk o) {
        long now = p.getServer().getTickCount();
        // Set Destination (hard Wilderness Diary): sneak-activate to cycle through the six obelisks.
        if (p.isShiftKeyDown() && hardDiary(p)) {
            WildernessTransport.Obelisk next = WildernessTransport.nextChoice(CHOSEN.get(p.getUUID()));
            CHOSEN.put(p.getUUID(), next);
            Msg.game(p, "The obelisks will now take you to the level " + next.level() + " obelisk (" + next.name()
                    + "). Activate the obelisk to teleport there.");
            return;
        }
        if (ARMED.containsKey(o)) {
            Msg.game(p, "The obelisk is already active.");
            return;
        }
        if (COOLING.getOrDefault(o, 0L) > now) {
            Msg.game(p, "The obelisk's power is still recovering.");
            return;
        }
        ARMED.put(o, now + (long) WildernessTransport.OBELISK_DELAY_TICKS * MC_TICKS_PER_GAME_TICK);
        // The first activator's rules apply to everyone on the platform (OSRS Wiki).
        WildernessTransport.Obelisk chosen = hardDiary(p) ? CHOSEN.get(p.getUUID()) : null;
        if (chosen != null) {
            TARGET.put(o, chosen);
        } else {
            TARGET.remove(o);
        }
        Msg.action(p, "You activate the ancient obelisk...");
        GameEvents.fireActivity(p, "obelisk:" + o.level());
        if (p.level() instanceof ServerLevel world) {
            world.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1f, 1f);
        }
    }

    private static void tick(MinecraftServer server) {
        if (ARMED.isEmpty()) {
            return;
        }
        long now = server.getTickCount();
        ServerLevel world = Gielinor.world(server);
        for (WildernessTransport.Obelisk o : new ArrayList<>(ARMED.keySet())) {
            long at = ARMED.get(o);
            if (world != null && at - now > 0 && (at - now) % MC_TICKS_PER_GAME_TICK == 0) {
                world.sendParticles(ParticleTypes.ENCHANT, GielinorMap.toBlockX(o.x()) + 1, world.getHeight(Heightmap.Types.MOTION_BLOCKING,
                        GielinorMap.toBlockX(o.x()), GielinorMap.toBlockZ(o.y())) + 1, GielinorMap.toBlockZ(o.y()) + 1, 40, 2.5, 1, 2.5, 0.5);
            }
            if (at > now) {
                continue;
            }
            ARMED.remove(o);
            COOLING.put(o, now + (long) WildernessTransport.OBELISK_COOLDOWN_TICKS * MC_TICKS_PER_GAME_TICK);
            WildernessTransport.Obelisk target = TARGET.remove(o);
            if (world != null) {
                fire(world, o, target != null ? target : WildernessTransport.destination(o, RANDOM));
            }
        }
    }

    /**
     * Sends everyone on the obelisk's platform to the same spot of the destination obelisk's platform, except players under
     * a Tele Block (OSRS: "The obelisk will only teleport players who are not under the effects of a Tele Block").
     */
    static int fire(ServerLevel world, WildernessTransport.Obelisk from, WildernessTransport.Obelisk to) {
        List<ServerPlayer> riders = new ArrayList<>();
        for (ServerPlayer p : world.players()) {
            BlockPos at = p.blockPosition();
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
        for (ServerPlayer p : riders) {
            BlockPos spot = Gielinor.safeSpot(world, p.blockPosition().getX() + dx, p.blockPosition().getZ() + dz);
            p.teleportTo(world, p.getX() + dx, spot.getY(), p.getZ() + dz, p.getYRot(), p.getXRot());
            world.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 1, 0.4, 0.4);
            Msg.action(p, "Ancient magic teleports you somewhere in the Wilderness.");
            GameEvents.fireActivity(p, "obelisk_teleport:" + to.level());
        }
        world.playSound(null, new BlockPos(GielinorMap.toBlockX(to.x()), world.getSeaLevel(), GielinorMap.toBlockZ(to.y())),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1f);
        return riders.size();
    }
}
