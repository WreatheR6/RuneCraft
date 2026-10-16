package com.runecraft.content.raids;

import com.runecraft.RuneCraft;
import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHooks;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.content.raids.Invocation;
import com.runecraft.osrs.content.raids.RaidType;
import com.runecraft.osrs.content.raids.RaidsContent;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.item.BlockItem;
import net.minecraft.util.ActionResult;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** All running raids: party formation, instance slots, ticking, deaths and logouts. */
public final class RaidManager {
    /** At most this many raids run at once (each owns a 1024-block strip). */
    public static final int MAX_INSTANCES = 64;
    private static final Map<Integer, RaidInstance> ACTIVE = new HashMap<>();
    private static final Map<UUID, RaidInstance> BY_PLAYER = new HashMap<>();

    private RaidManager() {
    }

    public static RaidInstance of(ServerPlayerEntity player) {
        return BY_PLAYER.get(player.getUuid());
    }

    /** The raid whose instance contains this entity, or null. */
    public static RaidInstance at(Entity e) {
        int slot = RaidBuilder.slotAt(e.getX(), e.getZ());
        if (slot < 0) {
            return null;
        }
        RaidInstance raid = ACTIVE.get(slot);
        return raid != null && raid.world == e.getWorld() ? raid : null;
    }

    static void unbind(UUID u) {
        BY_PLAYER.remove(u);
    }

    public static List<RaidInstance> active() {
        return new ArrayList<>(ACTIVE.values());
    }

    static void close(RaidInstance raid) {
        if (ACTIVE.get(raid.slot) != raid) {
            return;
        }
        for (UUID u : new ArrayList<>(raid.party())) {
            raid.removePlayer(u, true);
        }
        ACTIVE.remove(raid.slot);
        raid.shutdown();
    }

    /** Self-test hook ({@code com.runecraft.selftest}): runs every room of a raid briefly; see RaidInstance. */
    public static int selfTestRun(RaidInstance raid, int ticks) {
        return raid.selfTestRun(ticks);
    }

    /** Self-test hook: closes a raid (party sent home, instance released). */
    public static void selfTestClose(RaidInstance raid) {
        close(raid);
    }

    // ------------------------------------------------------------------ registration

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (ActionManager.mcTick() % ActionManager.MC_TICKS_PER_GAME_TICK != 0) {
                return;
            }
            long tick = ActionManager.gameTick();
            RaidEffects.gameTick();
            for (RaidInstance raid : active()) {
                try {
                    raid.gameTick(tick);
                } catch (RuntimeException e) {
                    RuneCraft.LOGGER.error("Raid {} crashed; closing it", raid.slot, e);
                    close(raid);
                }
            }
        });
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity p) {
                RaidInstance raid = of(p);
                if (raid != null && raid.contains(p)) {
                    raid.onDeath(p);
                    return false;
                }
            }
            if (entity instanceof com.runecraft.boss.DataBossEntity boss && at(boss) != null) {
                RoomLogic r = RaidScript.room(boss);
                return r == null || r.bossAllowDeath(boss);
            }
            return true;
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity p) {
                RaidInstance raid = of(p);
                return raid == null || !raid.isDowned(p);
            }
            return true;
        });
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            /** OSRS: melee always hits a Warden's ejected core for its maximum. */
            @Override
            public boolean alwaysMaxHit(ServerPlayerEntity player, LivingEntity target, AttackType type) {
                return ToaRooms.Wardens.isCore(target) && (type == AttackType.STAB || type == AttackType.SLASH || type == AttackType.CRUSH);
            }

            @Override
            public void afterHit(ServerPlayerEntity player, LivingEntity target, AttackType type, int dealt) {
                RaidInstance raid = of(player);
                if (raid != null && raid.contains(target)) {
                    raid.onDamage(player, target, dealt);
                }
            }
        });
        CombatHooks.addHurtListener((player, attacker, damage) -> {
            hurt(player, damage);
            if (!(attacker instanceof com.runecraft.boss.DataBossEntity)) {
                deadlyPrayers(player, damage);
            }
        });
        // ToA restoration invocations: On a Diet forbids food, Dehydration forbids health-restoring potions (OSRS).
        com.runecraft.skill.ItemUses.addConsumeGuard((player, def) -> {
            RaidInstance raid = of(player);
            if (raid == null || raid.type != RaidType.TOA) {
                return null;
            }
            if (def.kind() == com.runecraft.osrs.ItemDef.Kind.FOOD && !Invocation.foodAllowed(raid.invocations())) {
                return "The 'On a Diet' invocation stops you from eating.";
            }
            boolean heals = def.id().contains("saradomin_brew") || def.id().contains("xerics_aid") || def.id().contains("nectar")
                    || def.id().contains("ambrosia");
            if (def.kind() == com.runecraft.osrs.ItemDef.Kind.POTION && heals && !Invocation.healingPotionsAllowed(raid.invocations())) {
                return "The 'Dehydration' invocation stops you from drinking that.";
            }
            if (!com.runecraft.osrs.content.raids.ToaSupplies.givenInsideTomb(def.id())) {
                // CA 424 "You are not prepared": anyone using supplies brought from outside fails it for the party.
                for (ServerPlayerEntity member : raid.players()) {
                    com.runecraft.content.systems.CombatAchievementService.flag(member,
                            com.runecraft.osrs.content.systems.CombatAchievements.FLAG_TOA_OUTSIDE_SUPPLY);
                }
            }
            return null;
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity p = handler.player;
            RaidInstance raid = of(p);
            if (raid != null) {
                raid.removePlayer(p.getUuid(), false);
                raid.say(p.getName().getString() + " has left the raid.", Formatting.GRAY);
                if (raid.party().isEmpty()) {
                    close(raid);
                }
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.player;
            if (of(p) == null && RaidBuilder.inInstanceArea(p.getX(), p.getZ()) && Gielinor.is(p)) {
                RaidEffects.later(1, () -> sendHome(p));
            }
        });
        // Raid instances cannot be dug out of or built in (outside creative mode).
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
                player.isCreative() || !RaidBuilder.inInstanceArea(pos.getX(), pos.getZ()) || !Gielinor.is(world));
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!world.isClient() && !player.isCreative() && player.getStackInHand(hand).getItem() instanceof BlockItem
                    && RaidBuilder.inInstanceArea(hit.getBlockPos().getX(), hit.getBlockPos().getZ()) && Gielinor.is(world)) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
        ServerLifecycleEvents.SERVER_STARTED.register(RaidManager::releaseStaleChunks);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (RaidInstance raid : active()) {
                raid.shutdown();
            }
            ACTIVE.clear();
            BY_PLAYER.clear();
        });
    }

    /** ToA Deadly Prayers (OSRS): prayer is drained by 20% of the damage taken. */
    static void deadlyPrayers(ServerPlayerEntity player, int damage) {
        RaidInstance raid = of(player);
        if (raid == null || raid.type != RaidType.TOA || damage <= 0) {
            return;
        }
        int drain = (int) Math.round(damage * Invocation.prayerDrainPerDamage(raid.invocations()));
        if (drain > 0) {
            var data = com.runecraft.player.Players.data(player);
            data.setPrayerPoints(Math.max(0, data.prayerPoints() - drain));
            com.runecraft.network.ServerNetworking.sync(player);
        }
    }

    static void hurt(ServerPlayerEntity player, int damage) {
        RaidInstance raid = of(player);
        if (raid == null) {
            return;
        }
        int room = raid.roomAt(player.getPos());
        if (room >= 0) {
            raid.logic(room).onPlayerHurt(player, damage);
        }
    }

    /** Sends a player stranded in an instance (e.g. after a restart) back to the entrance they used. */
    static void sendHome(ServerPlayerEntity p) {
        if (!p.isAlive() || of(p) != null) {
            return;
        }
        RaidsState.Stats stats = RaidsState.get(p.getServer()).stats(p.getUuid());
        ServerWorld world = Gielinor.world(p.getServer());
        if (stats.lastEntrance != null && world != null) {
            BlockPos b = stats.lastEntrance;
            p.teleport(world, b.getX() + 0.5, b.getY(), b.getZ() + 0.5, p.getYaw(), 0);
        } else {
            Gielinor.teleport(p, Landmark.LUMBRIDGE);
        }
        Msg.game(p, "You find yourself back outside the raid.");
    }

    private static void releaseStaleChunks(MinecraftServer server) {
        RaidsState state = RaidsState.get(server);
        ServerWorld world = Gielinor.world(server);
        if (world == null) {
            return;
        }
        for (long c : new ArrayList<>(state.forcedChunks())) {
            world.setChunkForced(ChunkPos.getPackedX(c), ChunkPos.getPackedZ(c), false);
            state.removeForced(c);
        }
    }

    // ------------------------------------------------------------------ entrance

    /** The entrance block: forms a party from players within 8 blocks and starts a new instance. */
    public static void enter(ServerPlayerEntity player, RaidType type, BlockPos pos) {
        if (of(player) != null) {
            Msg.game(player, "You are already in a raid.");
            return;
        }
        ServerWorld world = Gielinor.world(player.getServer());
        if (world == null) {
            world = player.getServerWorld();
        }
        List<ServerPlayerEntity> members = new ArrayList<>();
        members.add(player);
        for (ServerPlayerEntity p : player.getServerWorld().getPlayers()) {
            if (p != player && members.size() < type.maxParty() && of(p) == null && p.isAlive() && !p.isSpectator()
                    && p.squaredDistanceTo(Vec3d.ofCenter(pos)) <= 8 * 8) {
                members.add(p);
            }
        }
        int slot = -1;
        for (int i = 0; i < MAX_INSTANCES; i++) {
            if (!ACTIVE.containsKey(i)) {
                slot = i;
                break;
            }
        }
        if (slot < 0) {
            Msg.game(player, "Every raid instance is in use. Please try again shortly.");
            return;
        }
        RaidInstance raid;
        try {
            raid = new RaidInstance(slot, type, world, members, pos);
            ACTIVE.put(slot, raid);
            for (ServerPlayerEntity p : members) {
                BY_PLAYER.put(p.getUuid(), raid);
            }
            raid.open(members);
        } catch (RuntimeException e) {
            RuneCraft.LOGGER.error("Failed to start a raid", e);
            Msg.red(player, "The raid could not be started.");
            ACTIVE.remove(slot);
            for (ServerPlayerEntity p : members) {
                BY_PLAYER.remove(p.getUuid());
            }
            return;
        }
        StringBuilder names = new StringBuilder();
        for (ServerPlayerEntity p : members) {
            names.append(names.length() == 0 ? "" : ", ").append(p.getName().getString());
        }
        raid.say("Party (" + members.size() + "): " + names, Formatting.YELLOW);
    }

    // ------------------------------------------------------------------ scripted blocks

    public static void useBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
        RaidInstance raid = of(player);
        if (raid == null || RaidBuilder.slotAt(pos.getX(), pos.getZ()) != raid.slot) {
            Msg.game(player, "Nothing interesting happens.");
            return;
        }
        switch (blockId) {
            case RaidsContent.START_BARRIER -> raid.begin(player);
            case RaidsContent.SUPPLIES -> raid.takeSupplies(player);
            case RaidsContent.REWARD_CHEST -> raid.claim(player);
            case RaidsContent.EXIT -> {
                if (raid.finished() && !raid.hasClaimed(player)) {
                    raid.claim(player);
                }
                Msg.game(player, raid.finished() ? "You leave with your reward." : "You leave the " + raid.type.displayName() + ".");
                raid.removePlayer(player.getUuid(), true);
                if (raid.party().isEmpty()) {
                    close(raid);
                } else {
                    raid.say(player.getName().getString() + " has left the raid.", Formatting.GRAY);
                }
            }
            case RaidsContent.INVOCATION_OBELISK -> invocations(player, raid, player.isSneaking() ? -1 : 1);
            default -> {
                int room = raid.roomAt(Vec3d.ofCenter(pos));
                RoomLogic logic = room >= 0 ? raid.logic(room) : null;
                // CoX resource rooms open their exit at once but their stations stay usable for the rest of the raid.
                boolean usable = logic != null && logic.started && (!logic.done || logic.def.kind() == com.runecraft.osrs.content.raids.RoomDef.Kind.RESOURCE);
                if (!usable || !logic.useBlock(player, blockId, pos)) {
                    Msg.game(player, "Nothing interesting happens.");
                }
            }
        }
    }

    /** The invocation obelisk cycles the ToA presets (sneak to go back). */
    static void invocations(ServerPlayerEntity player, RaidInstance raid, int step) {
        if (raid.type != RaidType.TOA) {
            return;
        }
        if (raid.begun()) {
            Msg.game(player, "The invocations are locked in: raid level " + raid.raidLevel() + ".");
            return;
        }
        raid.applyPreset(raid.presetIndex() + step);
        Invocation.Preset preset = Invocation.presets().get(raid.presetIndex());
        raid.say(player.getName().getString() + " sets the invocations to " + preset.name() + " (raid level " + raid.raidLevel() + ").",
                Formatting.YELLOW);
        StringBuilder list = new StringBuilder();
        for (Invocation i : raid.invocations()) {
            list.append(list.length() == 0 ? "" : ", ").append(i.displayName());
        }
        player.sendMessage(Text.literal(list.length() == 0 ? "No invocations active." : "Active: " + list).formatted(Formatting.GRAY),
                false);
    }
}
