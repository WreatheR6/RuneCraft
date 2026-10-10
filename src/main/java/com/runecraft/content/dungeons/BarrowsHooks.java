package com.runecraft.content.dungeons;

import com.runecraft.collection.CollectionLog;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.GameEvents;
import com.runecraft.entity.BossService;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.content.dungeons.Barrows;
import com.runecraft.osrs.content.dungeons.BarrowsRun;
import com.runecraft.osrs.data.Boss;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import com.runecraft.world.StructurePlacer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Random;

/**
 * Minecraft-side Barrows (see {@link Barrows}): digging on a mound with a spade drops the player
 * into its crypt, searching a sarcophagus summons its brother (or, for this run's tunnel brother,
 * offers the hidden tunnel), the tunnel doors open or stay locked per run, the chest summons the
 * hidden brother and then gives the reward and sends the player back to the surface. Prayer drains
 * slowly while underground, as in OSRS.
 */
final class BarrowsHooks {
    private static final String SPADE = "spade";
    /** OSRS drains prayer every 18 game ticks (12 Minecraft ticks each) in the crypts and tunnels. */
    private static final int DRAIN_INTERVAL = 18 * 12;
    private static long ticks;

    private BarrowsHooks() {
    }

    static void register() {
        BlockHooks.register(Barrows.SARCOPHAGUS, BarrowsHooks::sarcophagus);
        BlockHooks.register(Barrows.CRYPT_STAIRS, BarrowsHooks::stairs);
        BlockHooks.register(Barrows.TUNNEL_DOOR, BarrowsHooks::door);
        BlockHooks.register(Barrows.CHEST, BarrowsHooks::chest);

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (world.isClient() || stack.isEmpty() || !SPADE.equals(Inv.idOf(stack)) || !(player instanceof ServerPlayerEntity p)) {
                return TypedActionResult.pass(stack);
            }
            return dig(p) ? TypedActionResult.success(p.getStackInHand(hand), false) : TypedActionResult.pass(stack);
        });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity p)) {
                return ActionResult.PASS;
            }
            ItemStack stack = p.getMainHandStack();
            if (stack.isEmpty() || !SPADE.equals(Inv.idOf(stack))) {
                return ActionResult.PASS;
            }
            return dig(p) ? ActionResult.SUCCESS : ActionResult.PASS;
        });

        GameEvents.onKill((p, e, stats) -> {
            Boss boss = ModEntities.boss(e.getType());
            int i = Barrows.indexOf(boss);
            if (i < 0) {
                return;
            }
            BarrowsState state = BarrowsState.get(p.getServer());
            BarrowsRun run = state.run(p.getUuid());
            if (!run.kill(i)) {
                return;
            }
            state.markDirty();
            Msg.game(p, "You have defeated " + run.killedCount() + " of the six Barrows brothers this run.");
            if (run.killedCount() == 5 && !run.killed(run.tunnel())) {
                Msg.game(p, "Only one sarcophagus is left to search - it may hide something.");
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                CommandManager.literal("barrows")
                        .then(CommandManager.literal("tunnel")
                                .then(CommandManager.literal("yes").executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    if (p != null) {
                                        confirmTunnel(p);
                                    }
                                    return 1;
                                }))
                                .then(CommandManager.literal("no").executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    if (p != null) {
                                        Msg.game(p, "You leave the tunnel alone.");
                                    }
                                    return 1;
                                })))));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks % DRAIN_INTERVAL != 0) {
                return;
            }
            ServerWorld world = Gielinor.world(server);
            if (world == null) {
                return;
            }
            for (ServerPlayerEntity p : world.getPlayers()) {
                BlockPos at = p.getBlockPos();
                if (!p.isAlive() || p.isSpectator() || !Barrows.underground(at.getX(), at.getY(), at.getZ())) {
                    continue;
                }
                drainPrayer(p);
            }
        });
    }

    private static BarrowsState state(ServerPlayerEntity p) {
        return BarrowsState.get(p.getServer());
    }

    /** Teleports within Gielinor, building the structures there first so the player never lands in rock. */
    private static void moveTo(ServerPlayerEntity p, ServerWorld world, int x, int y, int z, float yaw) {
        StructurePlacer.applyAround(world, x, z, 12);
        p.stopRiding();
        p.teleport(world, x + 0.5, y, z + 0.5, yaw, p.getPitch());
    }

    // ------------------------------------------------------------------ mounds and crypts

    /** Digging with a spade: on top of a mound the player breaks into its crypt. */
    private static boolean dig(ServerPlayerEntity p) {
        if (!Gielinor.is(p) || !(p.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        BlockPos at = p.getBlockPos();
        Barrows.Crypt crypt = Barrows.moundAt(at.getX(), at.getY(), at.getZ());
        if (crypt == null) {
            return false;
        }
        world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.getX(), p.getY(), p.getZ(), 8, 0.3, 0.1, 0.3, 0.01);
        world.playSound(null, at, SoundEvents.ITEM_SHOVEL_FLATTEN, SoundCategory.PLAYERS, 1f, 1f);
        int[] a = crypt.arrival();
        moveTo(p, world, a[0], a[1], a[2], 180f);
        Msg.game(p, "You dig into the mound... and break into a crypt!");
        Msg.game(p, "This is " + crypt.brother().displayName() + "'s crypt. Search the sarcophagus.");
        return true;
    }

    private static void stairs(ServerPlayerEntity p, BlockPos pos, BlockState state) {
        Barrows.Crypt crypt = Barrows.cryptAt(pos.getX(), pos.getY(), pos.getZ());
        if (crypt == null || !(p.getWorld() instanceof ServerWorld world)) {
            Msg.game(p, "The stairs are blocked.");
            return;
        }
        StructurePlacer.applyAround(world, crypt.x(), crypt.z(), 12);
        BlockPos top = Gielinor.safeSpot(world, crypt.x(), crypt.z());
        p.stopRiding();
        p.teleport(world, top.getX() + 0.5, top.getY(), top.getZ() + 0.5, p.getYaw(), p.getPitch());
        Msg.game(p, "You climb the stairs to the top of the mound.");
    }

    private static void sarcophagus(ServerPlayerEntity p, BlockPos pos, BlockState blockState) {
        Barrows.Crypt crypt = Barrows.cryptAt(pos.getX(), pos.getY(), pos.getZ());
        if (crypt == null || !(p.getWorld() instanceof ServerWorld world)) {
            Msg.game(p, "The sarcophagus is sealed.");
            return;
        }
        BarrowsState state = state(p);
        BarrowsRun run = state.run(p.getUuid());
        switch (run.search(crypt.index())) {
            case EMPTY -> Msg.game(p, "You search the sarcophagus but find nothing.");
            case TUNNEL -> {
                if (p.isSneaking()) {
                    enterTunnels(p, world, run);
                    return;
                }
                MutableText line = Text.literal("You find a hidden tunnel, do you want to enter? ").formatted(Formatting.GRAY);
                line.append(link("Yes", "/barrows tunnel yes", "Enter the tunnels", Formatting.GREEN)).append(Text.literal(" "));
                line.append(link("No", "/barrows tunnel no", "Stay in the crypt", Formatting.RED));
                p.sendMessage(line, false);
                Msg.game(p, "(Or sneak while searching to enter at once.)");
            }
            case SUMMON -> {
                int[] f = crypt.spawnFloor();
                BossService.summon(p, world, new BlockPos(f[0], f[1], f[2]), crypt.brother());
                run.summon(crypt.index());
                state.markDirty();
            }
        }
    }

    private static MutableText link(String label, String command, String hover, Formatting color) {
        return Text.literal("[" + label + "]").styled(s -> s.withColor(color)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hover))));
    }

    /** [Yes] on the hidden-tunnel prompt: only works standing in this run's tunnel brother's crypt. */
    private static void confirmTunnel(ServerPlayerEntity p) {
        if (!Gielinor.is(p) || !(p.getWorld() instanceof ServerWorld world)) {
            return;
        }
        BlockPos at = p.getBlockPos();
        Barrows.Crypt crypt = Barrows.cryptAt(at.getX(), at.getY(), at.getZ());
        BarrowsRun run = state(p).run(p.getUuid());
        if (crypt == null || run.search(crypt.index()) != BarrowsRun.Search.TUNNEL) {
            Msg.game(p, "There is no tunnel here.");
            return;
        }
        enterTunnels(p, world, run);
    }

    private static void enterTunnels(ServerPlayerEntity p, ServerWorld world, BarrowsRun run) {
        int[] c = Barrows.roomCentre(run.startRoom());
        moveTo(p, world, c[0], Barrows.TUNNEL_FLOOR + 1, c[1], p.getYaw());
        Msg.game(p, "You climb down into the tunnels. Find your way to the chest in the centre.");
    }

    // ------------------------------------------------------------------ tunnels

    private static void door(ServerPlayerEntity p, BlockPos pos, BlockState state) {
        int edge = Barrows.edgeAtDoor(pos.getX(), pos.getZ());
        BarrowsRun run = state(p).run(p.getUuid());
        if (edge < 0 || !run.doorOpen(edge)) {
            Msg.game(p, "The door is locked. You'll have to find another way round.");
            return;
        }
        p.getWorld().playSound(null, pos, SoundEvents.BLOCK_WOODEN_DOOR_OPEN, SoundCategory.BLOCKS, 0.8f, 0.8f);
        DungeonsHooks.passThrough(p, pos);
    }

    private static void chest(ServerPlayerEntity p, BlockPos pos, BlockState blockState) {
        if (!(p.getWorld() instanceof ServerWorld world)) {
            return;
        }
        BarrowsState state = state(p);
        BarrowsRun run = state.run(p.getUuid());
        if (run.open() == BarrowsRun.Open.SUMMON_HIDDEN) {
            Boss hidden = run.tunnelBrother();
            Msg.game(p, "You open the chest... and something stirs in the dark!");
            BossService.summon(p, world, pos.add(2, -1, 0), hidden);
            run.summon(run.tunnel());
            state.markDirty();
            return;
        }
        List<BarrowsRun.Reward> rewards = run.rollRewards(new Random(p.getRandom().nextLong()));
        int killed = run.killedCount();
        Msg.game(p, "You open the chest (" + killed + " of 6 brothers killed) and find:");
        StringBuilder sb = new StringBuilder();
        for (BarrowsRun.Reward r : rewards) {
            Inv.give(p, r.item(), r.count());
            CollectionLog.obtain(p, r.item());
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(r.count()).append(" x ").append(r.item().replace('_', ' '));
        }
        Msg.game(p, sb.length() == 0 ? "Nothing." : sb.toString());
        PlayerData data = Players.data(p);
        int kc = data.incrementKillCount("barrows_chests");
        Msg.game(p, "Your Barrows chest count is: " + kc + ".");
        run.reset(new Random(p.getRandom().nextLong()));
        state.markDirty();
        world.playSound(null, pos, SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 1f, 0.8f);
        int x = GielinorMap.toBlockX(Barrows.SURFACE_TILE[0]);
        int z = GielinorMap.toBlockZ(Barrows.SURFACE_TILE[1]);
        StructurePlacer.applyAround(world, x, z, 12);
        BlockPos top = Gielinor.safeSpot(world, x, z);
        p.stopRiding();
        p.teleport(world, top.getX() + 0.5, top.getY(), top.getZ() + 0.5, p.getYaw(), p.getPitch());
        Msg.game(p, "The crypts rumble and you find yourself back on the surface.");
        ServerNetworking.sync(p);
    }

    // ------------------------------------------------------------------ prayer drain

    /** OSRS: prayer drains in the crypts and tunnels, one point more for each brother killed this run. */
    private static void drainPrayer(ServerPlayerEntity p) {
        PlayerData data = Players.data(p);
        if (data.prayerPoints() <= 0) {
            return;
        }
        int killed = state(p).run(p.getUuid()).killedCount();
        data.setPrayerPoints(data.prayerPoints() - (1 + killed));
        ServerNetworking.sync(p);
    }
}
