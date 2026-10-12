package com.runecraft.content.skilling;

import com.runecraft.action.Action;
import com.runecraft.action.ActionManager;
import com.runecraft.content.BlockHooks;
import com.runecraft.osrs.Chance;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.skilling.CookingData;
import com.runecraft.osrs.content.skilling.SailingData;
import com.runecraft.quest.QuestApi;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.LandmarkBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sailing: launching boats, boat tiers (level checks and bonus XP), salvaging shipwrecks, sea
 * charting, port courier tasks and trawling.
 */
final class SailingHooks {
    static final String BOAT_TAG = "runecraft_boat_";
    private static final Map<UUID, Double> DISTANCE = new HashMap<>();

    private SailingHooks() {
    }

    static void init() {
        for (SailingData.Wreck w : SailingData.WRECKS) {
            BlockHooks.register(w.blockId(), (player, pos, state) -> salvage(player, pos, w));
            LandmarkBuilder.registerCustom(w.blockId() + "_site", (world, centre) -> placeAtSea(world, centre, w.blockId(), 0));
        }
        BlockHooks.register("sea_chart_marker", (player, pos, state) -> chart(player, pos));
        for (SailingData.ChartPoint c : SailingData.CHART_POINTS) {
            LandmarkBuilder.registerCustom(c.placeId(), (world, centre) -> placeAtSea(world, centre, "sea_chart_marker", 0));
        }
        for (SailingData.Port p : SailingData.PORTS) {
            BlockHooks.register(p.board(), (player, pos, state) -> portBoard(player, p));
        }
        LandmarkBuilder.registerCustom("fishing_platform_deep_sea", (world, centre) -> {
            placeAtSea(world, centre, "deep_sea_fishing_spot", 0);
            placeAtSea(world, centre, "deep_sea_fishing_spot", 4);
        });
    }

    /** Puts a module block on the water surface (or on the ground if the spot turned out to be land). */
    private static void placeAtSea(ServerWorld world, BlockPos centre, String blockId, int dx) {
        BlockState block = SkillingUtil.moduleBlock(blockId);
        if (block == null) {
            return;
        }
        BlockPos at = centre.add(dx, 0, 0);
        BlockState here = world.getBlockState(at);
        if (here.getFluidState().isIn(FluidTags.WATER) || here.isReplaceable()) {
            world.setBlockState(at, block, Block.NOTIFY_LISTENERS);
        } else {
            world.setBlockState(at.up(), block, Block.NOTIFY_LISTENERS);
        }
    }

    // ------------------------------------------------------------------ boats

    static void launch(ServerPlayerEntity player, ItemStack stack, SailingData.Boat boat) {
        String why = SailingData.canSail(pandemoniumDone(player), SkillingUtil.level(player, Skill.SAILING), boat);
        if (why != null) {
            Msg.game(player, why);
            return;
        }
        Vec3d eye = player.getEyePos();
        Vec3d end = eye.add(player.getRotationVector().multiply(5.0));
        BlockHitResult hit = player.getWorld().raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.SOURCE_ONLY, player));
        if (hit.getType() != HitResult.Type.BLOCK || !player.getWorld().getFluidState(hit.getBlockPos()).isIn(FluidTags.WATER)) {
            Msg.game(player, "Use the kit while looking at water to launch your " + SkillingUtil.lower(boat.name()) + ".");
            return;
        }
        Vec3d p = hit.getPos();
        BoatEntity entity = new BoatEntity(player.getWorld(), p.x, p.y, p.z);
        entity.setVariant(BoatEntity.Type.valueOf(boat.variant()));
        entity.setYaw(player.getYaw());
        entity.addCommandTag(BOAT_TAG + boat.kit());
        entity.setCustomName(net.minecraft.text.Text.literal(player.getName().getString() + "'s " + SkillingUtil.lower(boat.name())));
        player.getWorld().spawnEntity(entity);
        stack.decrement(1);
        player.getWorld().playSound(null, entity.getBlockPos(), SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.PLAYERS, 1f, 1f);
        Msg.action(player, "You launch your " + SkillingUtil.lower(boat.name()) + ".");
    }

    /** Sailing is taught by the Pandemonium quest (creative players skip it). */
    static boolean pandemoniumDone(ServerPlayerEntity player) {
        return player.getAbilities().creativeMode || QuestApi.isComplete(player, SailingData.SAILING_QUEST);
    }

    static SailingData.Boat boatOf(Entity vehicle) {
        if (!(vehicle instanceof BoatEntity)) {
            return null;
        }
        for (String tag : vehicle.getCommandTags()) {
            if (tag.startsWith(BOAT_TAG)) {
                return SailingData.boat(tag.substring(BOAT_TAG.length()));
            }
        }
        return null;
    }

    /** Called every game tick: boat level requirements and bonus Sailing XP for better boats. */
    static void boatTick(ServerPlayerEntity player) {
        Entity vehicle = player.getVehicle();
        SailingData.Boat boat = boatOf(vehicle);
        if (boat == null) {
            return;
        }
        if (SkillingUtil.level(player, Skill.SAILING) < boat.level()) {
            player.stopRiding();
            Msg.game(player, "You need a Sailing level of " + boat.level() + " to sail this " + SkillingUtil.lower(boat.name()) + ".");
            return;
        }
        if (boat.bonusXp() <= 0) {
            return;
        }
        Vec3d v = vehicle.getVelocity();
        double d = DISTANCE.getOrDefault(player.getUuid(), 0.0) + Math.sqrt(v.x * v.x + v.z * v.z) * 12;
        if (d >= 32) {
            d -= 32;
            XpService.add(player, Skill.SAILING, boat.bonusXp());
        }
        DISTANCE.put(player.getUuid(), d);
    }

    private static boolean nearBoat(ServerPlayerEntity player) {
        return player.getVehicle() instanceof BoatEntity
                || !player.getWorld().getEntitiesByClass(BoatEntity.class, player.getBoundingBox().expand(6), b -> true).isEmpty();
    }

    // ------------------------------------------------------------------ salvaging

    private static void salvage(ServerPlayerEntity player, BlockPos pos, SailingData.Wreck w) {
        if (!SkillingUtil.need(player, Skill.SAILING, w.level(), "salvage this shipwreck")) {
            return;
        }
        if (!SkillingUtil.needItem(player, "salvaging_hook", "You need a salvaging hook to salvage shipwrecks.")) {
            return;
        }
        ServerWorld world = (ServerWorld) player.getWorld();
        if (GatheringHooks.depleted(world, pos)) {
            Msg.game(player, "There's nothing left to salvage here for now.");
            return;
        }
        if (!nearBoat(player)) {
            Msg.game(player, "You need to be on (or next to) a boat to salvage shipwrecks.");
            return;
        }
        Msg.action(player, "You lower your salvaging hook into the wreck.");
        ActionManager.start(player, new SkillingUtil.BlockAction(pos, w.blockId(), 5) {
            @Override
            boolean roll(ServerPlayerEntity p, Random random) {
                if (!Inv.has(p, "salvaging_hook", 1) || GatheringHooks.depleted(world, pos)) {
                    return false;
                }
                world.playSound(null, pos, SoundEvents.ENTITY_FISHING_BOBBER_SPLASH, SoundCategory.PLAYERS, 0.5f, 0.8f);
                if (random.nextDouble() < Chance.lerp(SkillingUtil.level(p, Skill.SAILING), w.low(), w.high())) {
                    SkillingUtil.give(p, w.salvage(), 1);
                    XpService.add(p, Skill.SAILING, w.xp());
                    Msg.action(p, "You haul up some salvage.");
                    if (random.nextInt(8) == 0) {
                        GatheringHooks.deplete(world, pos, 60);
                        Msg.game(p, "The wreck has been picked clean for now.");
                        return false;
                    }
                }
                return true;
            }
        }, true);
    }

    // ------------------------------------------------------------------ sea charting

    private static void chart(ServerPlayerEntity player, BlockPos pos) {
        int x = GielinorMap.toOsrsX(pos.getX());
        int y = GielinorMap.toOsrsY(pos.getZ());
        SailingData.ChartPoint best = null;
        int bestDist = Integer.MAX_VALUE;
        for (SailingData.ChartPoint c : SailingData.CHART_POINTS) {
            int d = Math.abs(c.x() - x) + Math.abs(c.y() - y);
            if (d < bestDist) {
                bestDist = d;
                best = c;
            }
        }
        if (best == null || bestDist > 40) {
            Msg.game(player, "There's nothing to chart here.");
            return;
        }
        if (!SkillingUtil.needItem(player, "spyglass", "You need a spyglass to chart the sea here.")) {
            return;
        }
        if (!SkillingUtil.need(player, Skill.SAILING, best.level(), "chart this area")) {
            return;
        }
        NbtCompound charted = SkillingState.get(player.getServer()).entry("charted", player.getUuidAsString());
        if (charted.getBoolean(best.placeId())) {
            Msg.game(player, "You have already charted " + best.name() + ".");
            return;
        }
        charted.putBoolean(best.placeId(), true);
        XpService.add(player, Skill.SAILING, best.xp());
        int done = charted.getKeys().size();
        Msg.action(player, "You survey " + best.name() + " through your spyglass and add it to your sea chart (" + done + "/"
                + SailingData.CHART_POINTS.size() + ").");
    }

    // ------------------------------------------------------------------ port tasks

    private static SailingData.Port port(String id) {
        for (SailingData.Port p : SailingData.PORTS) {
            if (p.id().equals(id)) {
                return p;
            }
        }
        return null;
    }

    private static void portBoard(ServerPlayerEntity player, SailingData.Port here) {
        SkillingState state = SkillingState.get(player.getServer());
        String key = player.getUuidAsString();
        if (state.has("courier", key)) {
            NbtCompound task = state.entry("courier", key);
            SailingData.Port from = port(task.getString("from"));
            SailingData.Port dest = port(task.getString("to"));
            if (dest == null || from == null) {
                state.remove("courier", key);
                return;
            }
            if (dest == here) {
                if (!Inv.has(player, "cargo_crate", 1)) {
                    Msg.game(player, "You've lost the cargo! Sneak and use a task board to abandon this task.");
                    return;
                }
                Inv.remove(player, "cargo_crate", 1);
                int tiles = Math.abs(from.x() - dest.x()) + Math.abs(from.y() - dest.y());
                XpService.add(player, Skill.SAILING, SailingData.courierXp(tiles, dest.level()));
                Inv.give(player, "coins", SailingData.courierCoins(tiles));
                state.remove("courier", key);
                Msg.action(player, "You deliver the cargo to " + dest.name() + " and receive your payment.");
                return;
            }
            if (player.isSneaking()) {
                state.remove("courier", key);
                Inv.remove(player, "cargo_crate", 1);
                Msg.game(player, "You abandon your courier task.");
                return;
            }
            Msg.game(player, "Your current task: deliver a cargo crate from " + from.name() + " to " + dest.name()
                    + ". (Sneak to abandon it.)");
            return;
        }
        if (!pandemoniumDone(player)) {
            Msg.game(player, "You need to complete Pandemonium before you can take port tasks.");
            return;
        }
        int level = SkillingUtil.level(player, Skill.SAILING);
        if (level < here.level()) {
            Msg.game(player, "You need a Sailing level of " + here.level() + " to take tasks from " + here.name() + ".");
            return;
        }
        List<SailingData.Port> options = new ArrayList<>();
        for (SailingData.Port p : SailingData.PORTS) {
            if (p != here && p.level() <= level) {
                options.add(p);
            }
        }
        if (options.isEmpty()) {
            Msg.game(player, "There are no tasks you can take from here yet.");
            return;
        }
        SailingData.Port dest = options.get(player.getRandom().nextInt(options.size()));
        NbtCompound task = state.entry("courier", key);
        task.putString("from", here.id());
        task.putString("to", dest.id());
        Inv.give(player, "cargo_crate", 1);
        Msg.action(player, "You take a courier task: sail this cargo crate to " + dest.name() + ".");
    }

    // ------------------------------------------------------------------ trawling

    private static final List<CookingData.NewFish> TRAWL = new ArrayList<>();

    static {
        for (CookingData.NewFish f : CookingData.FISH) {
            if (List.of("giant_krill", "haddock", "yellowfin", "halibut", "bluefin", "marlin").contains(f.id())) {
                TRAWL.add(0, f);   // best first
            }
        }
    }

    static void trawl(ServerPlayerEntity player) {
        if (!(player.getVehicle() instanceof BoatEntity)) {
            Msg.game(player, "You need to be sailing a boat to cast a trawling net.");
            return;
        }
        if (SkillingUtil.level(player, Skill.FISHING) < TRAWL.get(TRAWL.size() - 1).fishLevel()) {
            Msg.game(player, "You need a Fishing level of " + TRAWL.get(TRAWL.size() - 1).fishLevel() + " to trawl.");
            return;
        }
        Msg.action(player, "You cast your trawling net behind the boat.");
        ActionManager.start(player, new Action() {
            int counter;

            @Override
            public boolean tick(ServerPlayerEntity p, long gameTick) {
                if (!(p.getVehicle() instanceof BoatEntity) || !Inv.has(p, "trawling_net", 1)) {
                    Msg.game(p, "You haul in your trawling net.");
                    return false;
                }
                if (++counter < 6) {
                    return true;
                }
                counter = 0;
                if (!p.getWorld().getFluidState(p.getVehicle().getBlockPos()).isIn(FluidTags.WATER)
                        && !p.getWorld().getFluidState(p.getVehicle().getBlockPos().down()).isIn(FluidTags.WATER)) {
                    return true;
                }
                int fishing = SkillingUtil.level(p, Skill.FISHING);
                Random random = p.getRandom();
                for (CookingData.NewFish f : TRAWL) {
                    if (fishing >= f.fishLevel() && random.nextDouble() < Chance.lerp(fishing, 24, 96)) {
                        SkillingUtil.give(p, f.raw(), 1);
                        XpService.add(p, Skill.FISHING, f.fishXp());
                        XpService.add(p, Skill.SAILING, SailingData.TRAWL_SAILING_XP);
                        Msg.action(p, "Your net brings up " + Msg.article(SkillingUtil.lower(f.name())) + ".");
                        return true;
                    }
                }
                return true;
            }
        }, true);
    }
}
