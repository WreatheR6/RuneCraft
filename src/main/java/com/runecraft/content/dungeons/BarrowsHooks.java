package com.runecraft.content.dungeons;

import com.runecraft.collection.CollectionLog;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.GameEvents;
import com.runecraft.entity.BossEntity;
import com.runecraft.entity.BossService;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.content.dungeons.Barrows;
import com.runecraft.osrs.content.dungeons.BarrowsPuzzles;
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
import net.minecraft.entity.EntityType;
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
import net.minecraft.util.math.Box;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Minecraft-side Barrows (see {@link Barrows}): digging on a mound with a spade drops the player
 * into its crypt, searching a sarcophagus summons its brother (or, for this run's tunnel brother,
 * offers the hidden tunnel), the tunnel doors open or stay locked per run, the chest summons the
 * hidden brother and then gives the reward and sends the player back to the surface. Prayer drains
 * slowly while underground, as in OSRS. About half of the doors into the chest room ask a
 * "Which item comes next?" puzzle (answered by clicking in chat), living brothers may ambush the
 * player on entering a tunnel room, and tunnel monster kills add reward potential.
 */
public final class BarrowsHooks {
    private static final String SPADE = "spade";
    /** OSRS drains prayer every 18 game ticks (12 Minecraft ticks each) in the crypts and tunnels. */
    private static final int DRAIN_INTERVAL = 18 * 12;
    /** How often (Minecraft ticks) to check which tunnel room each player is in. */
    private static final int ROOM_CHECK_INTERVAL = 10;
    private static long ticks;

    /** A puzzle door waiting for the player's answer. */
    private record Pending(int edge, BlockPos door, BarrowsPuzzles.Asked asked) {
    }

    private static final Map<UUID, Pending> PUZZLES = new HashMap<>();
    /** The tunnel room each player was last seen in (so entering a room is noticed once). */
    private static final Map<UUID, Integer> ROOMS = new HashMap<>();

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
                BlockPos at = e.getBlockPos();
                if (boss == null && Gielinor.is(p) && Barrows.inTunnels(at.getX(), at.getY(), at.getZ())) {
                    BarrowsState state = BarrowsState.get(p.getServer());
                    state.run(p.getUuid()).addMonsterKill();
                    state.markDirty();
                }
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
                                })))
                        .then(CommandManager.literal("puzzle")
                                .then(CommandManager.literal("1").executes(ctx -> answerPuzzle(ctx.getSource().getPlayer(), 0)))
                                .then(CommandManager.literal("2").executes(ctx -> answerPuzzle(ctx.getSource().getPlayer(), 1)))
                                .then(CommandManager.literal("3").executes(ctx -> answerPuzzle(ctx.getSource().getPlayer(), 2))))));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ++ticks;
            boolean drain = ticks % DRAIN_INTERVAL == 0;
            boolean rooms = ticks % ROOM_CHECK_INTERVAL == 0;
            if (!drain && !rooms) {
                return;
            }
            ServerWorld world = Gielinor.world(server);
            if (world == null) {
                return;
            }
            for (ServerPlayerEntity p : world.getPlayers()) {
                BlockPos at = p.getBlockPos();
                boolean alive = p.isAlive() && !p.isSpectator();
                if (rooms) {
                    if (alive && Barrows.inTunnels(at.getX(), at.getY(), at.getZ())) {
                        checkRoom(p, world, Barrows.roomAt(at.getX(), at.getZ()));
                    } else {
                        ROOMS.remove(p.getUuid());
                    }
                }
                if (drain && alive && Barrows.underground(at.getX(), at.getY(), at.getZ())) {
                    drainPrayer(p);
                }
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
        // OSRS: the doors into the chest room may pose a puzzle (only on the way in).
        if (run.needsPuzzle(edge) && Barrows.roomBeyondDoor(edge, p.getBlockX(), p.getBlockZ()) == Barrows.CHEST_ROOM) {
            Pending old = PUZZLES.get(p.getUuid());
            BarrowsPuzzles.Asked asked = old != null && old.edge() == edge ? old.asked()
                    : BarrowsPuzzles.roll(new Random(p.getRandom().nextLong()), null);
            PUZZLES.put(p.getUuid(), new Pending(edge, pos.toImmutable(), asked));
            askPuzzle(p, asked);
            return;
        }
        openDoor(p, pos);
    }

    private static void openDoor(ServerPlayerEntity p, BlockPos pos) {
        p.getWorld().playSound(null, pos, SoundEvents.BLOCK_WOODEN_DOOR_OPEN, SoundCategory.BLOCKS, 0.8f, 0.8f);
        DungeonsHooks.passThrough(p, pos);
    }

    private static void askPuzzle(ServerPlayerEntity p, BarrowsPuzzles.Asked asked) {
        Msg.game(p, "The door is sealed by a puzzle. " + BarrowsPuzzles.PROMPT);
        p.sendMessage(Text.literal(asked.puzzle().question()).formatted(Formatting.YELLOW), false);
        MutableText line = Text.literal("Answer: ").formatted(Formatting.GRAY);
        List<String> options = asked.options();
        for (int i = 0; i < options.size(); i++) {
            line.append(link(options.get(i), "/barrows puzzle " + (i + 1), "Answer " + options.get(i), Formatting.AQUA));
            line.append(Text.literal(" "));
        }
        p.sendMessage(line, false);
    }

    /** A click on one of the puzzle answers ({@code choice} 0..2). */
    private static int answerPuzzle(ServerPlayerEntity p, int choice) {
        if (p == null) {
            return 0;
        }
        UUID uuid = p.getUuid();
        Pending pending = PUZZLES.get(uuid);
        if (pending == null || !Gielinor.is(p)) {
            Msg.game(p, "No door is waiting for an answer.");
            return 1;
        }
        if (p.getBlockPos().getSquaredDistance(pending.door()) > 6 * 6) {
            Msg.game(p, "You are too far away from the door.");
            return 1;
        }
        BarrowsState state = state(p);
        BarrowsRun run = state.run(uuid);
        if (!run.needsPuzzle(pending.edge())) {
            PUZZLES.remove(uuid);
            Msg.game(p, "That door no longer needs an answer.");
            return 1;
        }
        if (pending.asked().isCorrect(choice)) {
            PUZZLES.remove(uuid);
            run.solve(pending.edge());
            state.markDirty();
            Msg.game(p, "You hear the door's locking mechanism grind open.");
            openDoor(p, pending.door());
            return 1;
        }
        // OSRS: a wrong answer resets the door - it stays shut and asks something else next time.
        BarrowsPuzzles.Asked next = BarrowsPuzzles.roll(new Random(p.getRandom().nextLong()), pending.asked().puzzle());
        PUZZLES.put(uuid, new Pending(pending.edge(), pending.door(), next));
        Msg.red(p, "Wrong answer! The door stays shut. Open it again for a new puzzle.");
        return 1;
    }

    // ------------------------------------------------------------------ brothers in the tunnels

    /** Called while a player is in the tunnels with the room he is in (-1 in a corridor). */
    private static void checkRoom(ServerPlayerEntity p, ServerWorld world, int room) {
        if (room < 0) {
            return;
        }
        Integer last = ROOMS.put(p.getUuid(), room);
        if ((last != null && last == room) || room == Barrows.CHEST_ROOM) {
            return;
        }
        if (brotherUp(world, p.getBlockPos())) {
            return;
        }
        BarrowsState state = state(p);
        BarrowsRun run = state.run(p.getUuid());
        int brother = run.tunnelEncounter(new Random(p.getRandom().nextLong()));
        if (brother < 0) {
            return;
        }
        int[] c = Barrows.roomCentre(room);
        // Appear on the far side of the room centre from the player.
        int dx = c[0] - p.getBlockX();
        int dz = c[1] - p.getBlockZ();
        int sx = Math.abs(dx) >= Math.abs(dz) ? (dx >= 0 ? 2 : -2) : 0;
        int sz = sx == 0 ? (dz >= 0 ? 2 : -2) : 0;
        Msg.game(p, "You hear footsteps in the darkness...");
        BossService.summon(p, world, new BlockPos(c[0] + sx, Barrows.TUNNEL_FLOOR, c[1] + sz), Barrows.BROTHERS.get(brother));
        run.summon(brother);
        state.markDirty();
    }

    /** True if any Barrows brother is alive near {@code pos} (at most one brother at a time). */
    private static boolean brotherUp(ServerWorld world, BlockPos pos) {
        Box box = new Box(pos).expand(48);
        for (Boss b : Barrows.BROTHERS) {
            EntityType<BossEntity> type = ModEntities.BOSS_TYPES.get(b);
            if (type != null && !world.getEntitiesByType(type, box, e -> e.isAlive()).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Rune quantity multiplier for a player's Barrows chest (Morytania elite diary perk; 1.0 = none). */
    public static java.util.function.ToDoubleFunction<ServerPlayerEntity> runeBonus = p -> 1.0;

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
        if (run.monsterPotential() > 0) {
            Msg.game(p, "Your " + run.monsterKills() + " tunnel kills add reward potential (+"
                    + (run.quantityPercent() - 100) + "% coins and runes).");
        }
        StringBuilder sb = new StringBuilder();
        double runes = runeBonus.applyAsDouble(p);
        for (BarrowsRun.Reward r : rewards) {
            int count = r.item().endsWith("_rune") ? (int) Math.floor(r.count() * runes) : r.count();
            r = new BarrowsRun.Reward(r.item(), count);
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
        PUZZLES.remove(p.getUuid());
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
