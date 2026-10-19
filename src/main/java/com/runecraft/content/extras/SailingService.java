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
import com.runecraft.platform.ServerTickEvents;
import com.runecraft.platform.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
            ItemStack stack = player.getItemInHand(hand);
            Seafaring.BoatTier tier = stack.isEmpty() ? null : Seafaring.byDeed(Inv.idOf(stack));
            if (tier == null) {
                return InteractionResultHolder.pass(stack);
            }
            if (!world.isClientSide() && player instanceof ServerPlayer sp) {
                launch(sp, stack, tier);
            }
            return InteractionResultHolder.success(stack);
        });
        BlockHooks.register(ExtrasContent.CHARTER_DOCK, (player, pos, state) -> dockOptions(player, portNear(pos)));
        for (Seafaring.Port p : Seafaring.PORTS) {
            LandmarkBuilder.registerCustom(p.place(), (world, centre) -> buildDock(world, p));
            if (p.shipwright()) {
                QuestScripts.onTalk("ext_shipwright_" + p.id(), (player, npc) -> shipwright(player));
            }
        }
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 10 == 0) {
                voyageTick(server);
            }
            if (server.getTickCount() % 200 == 50) {
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

    private static void buildDock(ServerLevel world, Seafaring.Port p) {
        BlockState dock = ExtrasUtil.block(ExtrasContent.CHARTER_DOCK);
        if (dock == null) {
            return;
        }
        BlockPos d = dockPos(p);
        int y = ExtrasUtil.surfaceY(world, d.getX(), d.getZ());
        BlockPos at = new BlockPos(d.getX(), y, d.getZ());
        for (BlockPos q : BlockPos.betweenClosed(at.offset(-3, -3, -3), at.offset(3, 3, 3))) {
            if (world.getBlockState(q).is(dock.getBlock())) {
                return;
            }
        }
        // A small jetty: planks under the dock post (over water too).
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos floor = at.offset(dx, -1, dz);
                if (world.getBlockState(floor).isAir() || !world.getFluidState(floor).isEmpty()) {
                    world.setBlockAndUpdate(floor, Blocks.SPRUCE_PLANKS.defaultBlockState());
                }
            }
        }
        world.setBlockAndUpdate(at, dock);
        world.setBlockAndUpdate(at.offset(1, 0, 0), Blocks.SPRUCE_FENCE.defaultBlockState());
        world.setBlockAndUpdate(at.offset(1, 1, 0), Blocks.LANTERN.defaultBlockState());
    }

    /** Re-places a dock a town structure flattened (structures are placed chunk by chunk). */
    private static void ensureDocks(MinecraftServer server) {
        ServerLevel world = Gielinor.world(server);
        if (world == null) {
            return;
        }
        for (ServerPlayer player : world.players()) {
            for (Seafaring.Port p : Seafaring.PORTS) {
                BlockPos d = dockPos(p);
                if (Math.abs(player.getX() - d.getX()) <= 48 && Math.abs(player.getZ() - d.getZ()) <= 48
                        && world.getChunkSource().hasChunk(d.getX() >> 4, d.getZ() >> 4)) {
                    buildDock(world, p);
                }
            }
        }
    }

    private static void dockOptions(ServerPlayer player, Seafaring.Port here) {
        if (here == null) {
            Msg.game(player, "This dock doesn't belong to any port.");
            return;
        }
        int level = ExtrasUtil.level(player, Skill.SAILING);
        CompoundTag voyage = ExtrasState.get(player.getServer()).entry("voyages", player.getStringUUID());
        if (voyage.contains("to")) {
            Seafaring.Port to = Seafaring.port(voyage.getString("to"));
            Msg.game(player, "Current voyage: sail your boat to " + (to == null ? "?" : to.name()) + ".");
        }
        List<MutableComponent> charters = new ArrayList<>();
        List<MutableComponent> voyages = new ArrayList<>();
        for (Seafaring.Port p : Seafaring.PORTS) {
            if (p == here) {
                continue;
            }
            int fare = Seafaring.charterFare(here, p);
            charters.add(ExtrasUtil.link(p.name() + " - " + ExtrasUtil.coins(fare), "/sailing charter " + here.id() + " " + p.id(),
                    "Charter a ship to " + p.name() + " (" + ExtrasUtil.coins(fare) + " coins)", ChatFormatting.GOLD));
            boolean ok = level >= p.level();
            voyages.add(ExtrasUtil.link(p.name() + " (" + p.level() + ")", "/sailing voyage " + here.id() + " " + p.id(),
                    "Sail your own boat to " + p.name() + "\nRequires level " + p.level() + " Sailing", ok ? ChatFormatting.GREEN
                            : ChatFormatting.DARK_GRAY));
        }
        Msg.action(player, here.name() + " charter dock.");
        ExtrasUtil.options(player, "Charter:", charters);
        ExtrasUtil.options(player, "Voyage:", voyages);
    }

    private static boolean nearDock(ServerPlayer player, Seafaring.Port p) {
        BlockPos d = dockPos(p);
        return Gielinor.is(player) && Math.hypot(player.getX() - d.getX(), player.getZ() - d.getZ()) < 24;
    }

    static void charter(ServerPlayer player, String fromId, String toId) {
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
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOAT_PADDLE_WATER, SoundSource.PLAYERS, 1f, 1f);
        Msg.game(player, "You board the charter ship and sail to " + to.name() + ".");
    }

    static void startVoyage(ServerPlayer player, String fromId, String toId) {
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
        CompoundTag voyage = ExtrasState.get(player.getServer()).entry("voyages", player.getStringUUID());
        voyage.putString("from", from.id());
        voyage.putString("to", to.id());
        voyage.putLong("start", ActionManager.gameTick());
        Msg.action(player, "You plot a course from " + from.name() + " to " + to.name() + ". Sail your boat there to complete the voyage.");
    }

    private static void voyageTick(MinecraftServer server) {
        CompoundTag all = ExtrasState.get(server).section("voyages");
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String key = player.getStringUUID();
            if (!all.contains(key) || !(player.getVehicle() instanceof SailBoatEntity boat)) {
                continue;
            }
            CompoundTag v = all.getCompound(key);
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
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
            Msg.action(player, "You complete your voyage to " + to.name() + " and are paid " + coins + " coins.");
        }
    }

    // ------------------------------------------------------------------ boats

    static boolean canBoard(ServerPlayer player, SailBoatEntity boat) {
        int level = ExtrasUtil.level(player, Skill.SAILING);
        if (level < boat.tier().level()) {
            Msg.game(player, "You need a Sailing level of " + boat.tier().level() + " to sail " + Msg.article(ExtrasUtil.lower(boat.tier()
                    .name())) + ".");
            return false;
        }
        return true;
    }

    /** Sailing your own boat needs the Pandemonium quest (creative players skip it). */
    static boolean pandemoniumDone(ServerPlayer player) {
        if (player.getAbilities().instabuild || com.runecraft.quest.QuestApi.isComplete(player, Seafaring.SAILING_QUEST)) {
            return true;
        }
        Msg.game(player, "You need to complete Pandemonium before you can sail your own boat.");
        return false;
    }

    private static void launch(ServerPlayer player, ItemStack stack, Seafaring.BoatTier tier) {
        if (!pandemoniumDone(player)) {
            return;
        }
        if (ExtrasUtil.level(player, Skill.SAILING) < tier.level()) {
            Msg.game(player, "You need a Sailing level of " + tier.level() + " to sail " + Msg.article(ExtrasUtil.lower(tier.name())) + ".");
            return;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(6.0));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.SOURCE_ONLY, player));
        if (hit.getType() != HitResult.Type.BLOCK || !player.level().getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
            Msg.game(player, "Use the deed while looking at water to launch your " + ExtrasUtil.lower(tier.name()) + ".");
            return;
        }
        EntityType<SailBoatEntity> type = ExtrasEntities.BOATS.get(tier.id());
        if (type == null) {
            return;
        }
        SailBoatEntity boat = type.create(player.level());
        if (boat == null) {
            return;
        }
        Vec3 p = hit.getLocation();
        boat.moveTo(p.x, p.y, p.z, player.getYRot(), 0);
        boat.setOwner(player.getUUID());
        boat.setCustomName(Component.literal(player.getName().getString() + "'s " + ExtrasUtil.lower(tier.name())));
        player.level().addFreshEntity(boat);
        stack.shrink(1);
        player.level().playSound(null, boat.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1f, 1f);
        Msg.action(player, "You launch your " + ExtrasUtil.lower(tier.name()) + ".");
    }

    /** Sneak + right-click: the owner docks the boat and gets the deed back. */
    static void dock(ServerPlayer player, SailBoatEntity boat) {
        if (boat.owner() != null && !boat.owner().equals(player.getUUID()) && !player.getAbilities().instabuild) {
            Msg.game(player, "This isn't your boat.");
            return;
        }
        if (boat.isVehicle()) {
            Msg.game(player, "Someone is still aboard.");
            return;
        }
        boat.discard();
        Inv.give(player, boat.tier().deed(), 1);
        Msg.game(player, "You dock your " + ExtrasUtil.lower(boat.tier().name()) + " and take its deed.");
    }

    // ------------------------------------------------------------------ shipwright

    private static void shipwright(ServerPlayer player) {
        Msg.game(player, "Shipwright: Ahoy! I can sell you a boat, or build one from your own materials.");
        List<MutableComponent> links = new ArrayList<>();
        for (Seafaring.BoatTier b : Seafaring.BOATS) {
            links.add(ExtrasUtil.link("Buy " + b.name() + " - " + ExtrasUtil.coins(b.price()), "/sailing buy " + b.id(),
                    b.name() + "\nRequires level " + b.level() + " Sailing to sail", ChatFormatting.GOLD));
            links.add(ExtrasUtil.link("Build " + b.name(), "/sailing build " + b.id(), b.name() + " from your materials\nLevel "
                    + b.buildLevel() + " Construction\n" + ExtrasUtil.materials(b.build()), ChatFormatting.YELLOW));
        }
        ExtrasUtil.options(player, "Boats:", links);
    }

    private static boolean nearShipwright(ServerPlayer player) {
        return !player.level().getEntitiesOfClass(QuestNpcEntity.class, player.getBoundingBox().inflate(8),
                e -> e.npcId() != null && e.npcId().startsWith("ext_shipwright_")).isEmpty();
    }

    static void buy(ServerPlayer player, String tierId, boolean build) {
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
    static void info(ServerPlayer player) {
        int level = ExtrasUtil.level(player, Skill.SAILING);
        Msg.game(player, "Sailing level " + level + ". Requirements:");
        for (Map.Entry<String, Integer> e : Seafaring.FEATURES.entrySet()) {
            player.displayClientMessage(Component.literal(" " + e.getKey() + ": " + (e.getValue() <= 0 ? "-" : e.getValue()))
                    .withStyle(level >= e.getValue() ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        }
    }
}
