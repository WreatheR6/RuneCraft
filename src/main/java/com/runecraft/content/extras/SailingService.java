package com.runecraft.content.extras;

import com.runecraft.action.ActionManager;
import com.runecraft.content.BlockHooks;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.extras.ExtrasContent;
import com.runecraft.osrs.content.extras.Seafaring;
import com.runecraft.quest.QuestScripts;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import com.runecraft.world.LandmarkBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sailing: launching and docking boats from their deeds (level-checked by tier), shipwrights who
 * sell or build boats, charter docks at every port (paid travel to other ports), and voyages: take
 * a voyage at a dock, sail your own boat to the destination port and earn Sailing XP on arrival.
 */
final class SailingService {
    /** Blocks from a port's dock at which a voyage counts as arrived. */
    private static final int ARRIVAL_RADIUS = 28;

    private SailingService() {
    }

    static void init() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            Seafaring.BoatTier tier = stack.isEmpty() ? null : Seafaring.byDeed(Inv.idOf(stack));
            if (tier == null) {
                return TypedActionResult.pass(stack);
            }
            if (!world.isClient() && player instanceof ServerPlayerEntity sp) {
                launch(sp, stack, tier);
            }
            return TypedActionResult.success(stack);
        });
        BlockHooks.register(ExtrasContent.CHARTER_DOCK, (player, pos, state) -> dockOptions(player, portNear(pos)));
        for (Seafaring.Port p : Seafaring.PORTS) {
            LandmarkBuilder.registerCustom(p.place(), (world, centre) -> buildDock(world, p));
            if (p.shipwright()) {
                QuestScripts.onTalk("ext_shipwright_" + p.id(), (player, npc) -> shipwright(player));
            }
        }
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 10 == 0) {
                voyageTick(server);
            }
            if (server.getTicks() % 200 == 50) {
                ensureDocks(server);
            }
        });
    }

    // ------------------------------------------------------------------ ports

    static BlockPos dockPos(Seafaring.Port p) {
        return new BlockPos(GielinorMap.toBlockX(p.x()), 0, GielinorMap.toBlockZ(p.y()));
    }

    private static Seafaring.Port portNear(BlockPos pos) {
        Seafaring.Port best = null;
        double bestD = Double.MAX_VALUE;
        for (Seafaring.Port p : Seafaring.PORTS) {
            BlockPos d = dockPos(p);
            double dist = Math.hypot(d.getX() - pos.getX(), d.getZ() - pos.getZ());
            if (dist < bestD) {
                bestD = dist;
                best = p;
            }
        }
        return bestD < 64 ? best : null;
    }

    private static void buildDock(ServerWorld world, Seafaring.Port p) {
        BlockState dock = ExtrasUtil.block(ExtrasContent.CHARTER_DOCK);
        if (dock == null) {
            return;
        }
        BlockPos d = dockPos(p);
        int y = ExtrasUtil.surfaceY(world, d.getX(), d.getZ());
        BlockPos at = new BlockPos(d.getX(), y, d.getZ());
        for (BlockPos q : BlockPos.iterate(at.add(-3, -3, -3), at.add(3, 3, 3))) {
            if (world.getBlockState(q).isOf(dock.getBlock())) {
                return;
            }
        }
        // A small jetty: planks under the dock post (over water too).
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos floor = at.add(dx, -1, dz);
                if (world.getBlockState(floor).isAir() || !world.getFluidState(floor).isEmpty()) {
                    world.setBlockState(floor, Blocks.SPRUCE_PLANKS.getDefaultState());
                }
            }
        }
        world.setBlockState(at, dock);
        world.setBlockState(at.add(1, 0, 0), Blocks.SPRUCE_FENCE.getDefaultState());
        world.setBlockState(at.add(1, 1, 0), Blocks.LANTERN.getDefaultState());
    }

    /** Re-places a dock a town structure flattened (structures are placed chunk by chunk). */
    private static void ensureDocks(MinecraftServer server) {
        ServerWorld world = Gielinor.world(server);
        if (world == null) {
            return;
        }
        for (ServerPlayerEntity player : world.getPlayers()) {
            for (Seafaring.Port p : Seafaring.PORTS) {
                BlockPos d = dockPos(p);
                if (Math.abs(player.getX() - d.getX()) <= 48 && Math.abs(player.getZ() - d.getZ()) <= 48
                        && world.getChunkManager().isChunkLoaded(d.getX() >> 4, d.getZ() >> 4)) {
                    buildDock(world, p);
                }
            }
        }
    }

    private static void dockOptions(ServerPlayerEntity player, Seafaring.Port here) {
        if (here == null) {
            Msg.game(player, "This dock doesn't belong to any port.");
            return;
        }
        int level = ExtrasUtil.level(player, Skill.SAILING);
        NbtCompound voyage = ExtrasState.get(player.getServer()).entry("voyages", player.getUuidAsString());
        if (voyage.contains("to")) {
            Seafaring.Port to = Seafaring.port(voyage.getString("to"));
            Msg.game(player, "Current voyage: sail your boat to " + (to == null ? "?" : to.name()) + ".");
        }
        List<MutableText> charters = new ArrayList<>();
        List<MutableText> voyages = new ArrayList<>();
        for (Seafaring.Port p : Seafaring.PORTS) {
            if (p == here) {
                continue;
            }
            int fare = Seafaring.charterFare(here, p);
            charters.add(ExtrasUtil.link(p.name() + " - " + ExtrasUtil.coins(fare), "/sailing charter " + here.id() + " " + p.id(),
                    "Charter a ship to " + p.name() + " (" + ExtrasUtil.coins(fare) + " coins)", Formatting.GOLD));
            boolean ok = level >= p.level();
            voyages.add(ExtrasUtil.link(p.name() + " (" + p.level() + ")", "/sailing voyage " + here.id() + " " + p.id(),
                    "Sail your own boat to " + p.name() + "\nRequires level " + p.level() + " Sailing", ok ? Formatting.GREEN
                            : Formatting.DARK_GRAY));
        }
        Msg.action(player, here.name() + " charter dock.");
        ExtrasUtil.options(player, "Charter:", charters);
        ExtrasUtil.options(player, "Voyage:", voyages);
    }

    private static boolean nearDock(ServerPlayerEntity player, Seafaring.Port p) {
        BlockPos d = dockPos(p);
        return Gielinor.is(player) && Math.hypot(player.getX() - d.getX(), player.getZ() - d.getZ()) < 24;
    }

    static void charter(ServerPlayerEntity player, String fromId, String toId) {
        Seafaring.Port from = Seafaring.port(fromId);
        Seafaring.Port to = Seafaring.port(toId);
        if (from == null || to == null || from == to) {
            return;
        }
        if (!nearDock(player, from)) {
            Msg.game(player, "You need to be at the " + from.name() + " charter dock.");
            return;
        }
        int fare = Seafaring.charterFare(from, to);
        if (!Inv.has(player, Inv.COINS, fare)) {
            Msg.game(player, "You need " + ExtrasUtil.coins(fare) + " coins to charter a ship to " + to.name() + ".");
            return;
        }
        Inv.remove(player, Inv.COINS, fare);
        ServerNetworking.sync(player);
        ExtrasUtil.teleport(player, to.x(), to.y());
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_BOAT_PADDLE_WATER, SoundCategory.PLAYERS, 1f, 1f);
        Msg.game(player, "You board the charter ship and sail to " + to.name() + ".");
    }

    static void startVoyage(ServerPlayerEntity player, String fromId, String toId) {
        Seafaring.Port from = Seafaring.port(fromId);
        Seafaring.Port to = Seafaring.port(toId);
        if (from == null || to == null) {
            return;
        }
        if (!nearDock(player, from)) {
            Msg.game(player, "You need to be at the " + from.name() + " dock to plan a voyage from there.");
            return;
        }
        if (!pandemoniumDone(player)) {
            return;
        }
        int level = ExtrasUtil.level(player, Skill.SAILING);
        if (from == to) {
            Msg.game(player, "You're already at " + to.name() + ".");
            return;
        }
        if (level < to.level()) {
            Msg.game(player, "You need a Sailing level of " + to.level() + " to sail to " + to.name() + ".");
            return;
        }
        NbtCompound voyage = ExtrasState.get(player.getServer()).entry("voyages", player.getUuidAsString());
        voyage.putString("from", from.id());
        voyage.putString("to", to.id());
        voyage.putLong("start", ActionManager.gameTick());
        Msg.action(player, "You plot a course from " + from.name() + " to " + to.name() + ". Sail your boat there to complete the voyage.");
    }

    private static void voyageTick(MinecraftServer server) {
        NbtCompound all = ExtrasState.get(server).section("voyages");
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            String key = player.getUuidAsString();
            if (!all.contains(key) || !(player.getVehicle() instanceof SailBoatEntity boat)) {
                continue;
            }
            NbtCompound v = all.getCompound(key);
            Seafaring.Port from = Seafaring.port(v.getString("from"));
            Seafaring.Port to = Seafaring.port(v.getString("to"));
            if (from == null || to == null) {
                all.remove(key);
                continue;
            }
            BlockPos d = dockPos(to);
            if (Math.hypot(boat.getX() - d.getX(), boat.getZ() - d.getZ()) > ARRIVAL_RADIUS) {
                continue;
            }
            if (ActionManager.gameTick() - v.getLong("start") < Seafaring.VOYAGE_MIN_TICKS) {
                continue;
            }
            String why = Seafaring.canVoyage(ExtrasUtil.level(player, Skill.SAILING), boat.tier(), from, to);
            all.remove(key);
            if (why != null) {
                Msg.game(player, why);
                continue;
            }
            double xp = Seafaring.voyageXp(from, to, boat.tier());
            XpService.add(player, Skill.SAILING, xp);
            int coins = 20 + Seafaring.tiles(from, to) / 4; // approx: cargo payment
            Inv.give(player, Inv.COINS, coins);
            ServerNetworking.sync(player);
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.6f, 1.4f);
            Msg.action(player, "You complete your voyage to " + to.name() + " and are paid " + coins + " coins.");
        }
    }

    // ------------------------------------------------------------------ boats

    static boolean canBoard(ServerPlayerEntity player, SailBoatEntity boat) {
        int level = ExtrasUtil.level(player, Skill.SAILING);
        if (level < boat.tier().level()) {
            Msg.game(player, "You need a Sailing level of " + boat.tier().level() + " to sail " + Msg.article(ExtrasUtil.lower(boat.tier()
                    .name())) + ".");
            return false;
        }
        return true;
    }

    /** Sailing your own boat needs the Pandemonium quest (creative players skip it). */
    static boolean pandemoniumDone(ServerPlayerEntity player) {
        if (player.getAbilities().creativeMode || com.runecraft.quest.QuestApi.isComplete(player, Seafaring.SAILING_QUEST)) {
            return true;
        }
        Msg.game(player, "You need to complete Pandemonium before you can sail your own boat.");
        return false;
    }

    private static void launch(ServerPlayerEntity player, ItemStack stack, Seafaring.BoatTier tier) {
        if (!pandemoniumDone(player)) {
            return;
        }
        if (ExtrasUtil.level(player, Skill.SAILING) < tier.level()) {
            Msg.game(player, "You need a Sailing level of " + tier.level() + " to sail " + Msg.article(ExtrasUtil.lower(tier.name())) + ".");
            return;
        }
        Vec3d eye = player.getEyePos();
        Vec3d end = eye.add(player.getRotationVector().multiply(6.0));
        BlockHitResult hit = player.getWorld().raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.SOURCE_ONLY, player));
        if (hit.getType() != HitResult.Type.BLOCK || !player.getWorld().getFluidState(hit.getBlockPos()).isIn(FluidTags.WATER)) {
            Msg.game(player, "Use the deed while looking at water to launch your " + ExtrasUtil.lower(tier.name()) + ".");
            return;
        }
        EntityType<SailBoatEntity> type = ExtrasEntities.BOATS.get(tier.id());
        if (type == null) {
            return;
        }
        SailBoatEntity boat = type.create(player.getWorld());
        if (boat == null) {
            return;
        }
        Vec3d p = hit.getPos();
        boat.refreshPositionAndAngles(p.x, p.y, p.z, player.getYaw(), 0);
        boat.setOwner(player.getUuid());
        boat.setCustomName(Text.literal(player.getName().getString() + "'s " + ExtrasUtil.lower(tier.name())));
        player.getWorld().spawnEntity(boat);
        stack.decrement(1);
        player.getWorld().playSound(null, boat.getBlockPos(), SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.PLAYERS, 1f, 1f);
        Msg.action(player, "You launch your " + ExtrasUtil.lower(tier.name()) + ".");
    }

    /** Sneak + right-click: the owner docks the boat and gets the deed back. */
    static void dock(ServerPlayerEntity player, SailBoatEntity boat) {
        if (boat.owner() != null && !boat.owner().equals(player.getUuid()) && !player.getAbilities().creativeMode) {
            Msg.game(player, "This isn't your boat.");
            return;
        }
        if (boat.hasPassengers()) {
            Msg.game(player, "Someone is still aboard.");
            return;
        }
        boat.discard();
        Inv.give(player, boat.tier().deed(), 1);
        Msg.game(player, "You dock your " + ExtrasUtil.lower(boat.tier().name()) + " and take its deed.");
    }

    // ------------------------------------------------------------------ shipwright

    private static void shipwright(ServerPlayerEntity player) {
        Msg.game(player, "Shipwright: Ahoy! I can sell you a boat, or build one from your own materials.");
        List<MutableText> links = new ArrayList<>();
        for (Seafaring.BoatTier b : Seafaring.BOATS) {
            links.add(ExtrasUtil.link("Buy " + b.name() + " - " + ExtrasUtil.coins(b.price()), "/sailing buy " + b.id(),
                    b.name() + "\nRequires level " + b.level() + " Sailing to sail", Formatting.GOLD));
            links.add(ExtrasUtil.link("Build " + b.name(), "/sailing build " + b.id(), b.name() + " from your materials\nLevel "
                    + b.buildLevel() + " Construction\n" + ExtrasUtil.materials(b.build()), Formatting.YELLOW));
        }
        ExtrasUtil.options(player, "Boats:", links);
    }

    private static boolean nearShipwright(ServerPlayerEntity player) {
        return !player.getWorld().getEntitiesByClass(QuestNpcEntity.class, player.getBoundingBox().expand(8),
                e -> e.npcId() != null && e.npcId().startsWith("ext_shipwright_")).isEmpty();
    }

    static void buy(ServerPlayerEntity player, String tierId, boolean build) {
        Seafaring.BoatTier b = Seafaring.boat(tierId);
        if (b == null) {
            return;
        }
        if (!nearShipwright(player)) {
            Msg.game(player, "You need to be next to a shipwright.");
            return;
        }
        if (build) {
            if (ExtrasUtil.level(player, Skill.CONSTRUCTION) < b.buildLevel()) {
                Msg.game(player, "You need a Construction level of " + b.buildLevel() + " to build " + Msg.article(ExtrasUtil.lower(b.name()))
                        + ".");
                return;
            }
            if (!ExtrasUtil.hasAll(player, b.build())) {
                Msg.game(player, "You need " + ExtrasUtil.materials(b.build()) + ".");
                return;
            }
            ExtrasUtil.removeAll(player, b.build());
            // Building your own boat trains Construction (approx: like a large flatpack).
            XpService.add(player, Skill.CONSTRUCTION, b.buildLevel() * 20.0);
            XpService.add(player, Skill.SAILING, b.level() * 10.0 + 50);
        } else {
            if (!Inv.has(player, Inv.COINS, b.price())) {
                Msg.game(player, "You need " + ExtrasUtil.coins(b.price()) + " coins for " + Msg.article(ExtrasUtil.lower(b.name())) + ".");
                return;
            }
            Inv.remove(player, Inv.COINS, b.price());
        }
        Inv.give(player, b.deed(), 1);
        ServerNetworking.sync(player);
        Msg.action(player, "The shipwright hands you the deed to your new " + ExtrasUtil.lower(b.name()) + ".");
    }

    /** /sailing info: feature requirements. */
    static void info(ServerPlayerEntity player) {
        int level = ExtrasUtil.level(player, Skill.SAILING);
        Msg.game(player, "Sailing level " + level + ". Requirements:");
        for (Map.Entry<String, Integer> e : Seafaring.FEATURES.entrySet()) {
            player.sendMessage(Text.literal(" " + e.getKey() + ": " + (e.getValue() <= 0 ? "-" : e.getValue()))
                    .formatted(level >= e.getValue() ? Formatting.GREEN : Formatting.RED), false);
        }
    }
}
