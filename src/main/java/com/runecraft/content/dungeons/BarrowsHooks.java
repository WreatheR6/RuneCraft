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
import com.runecraft.platform.CommandRegistrationCallback;
import com.runecraft.platform.ServerTickEvents;
import com.runecraft.platform.UseBlockCallback;
import com.runecraft.platform.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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
 * every 18 seconds underground (8 points, one more per brother killed), as in OSRS. About half of the doors into the chest
 * room ask a "Which item comes next?" puzzle (answered by clicking in chat), every door opened may wake a brother still
 * alive, a skeleton, a bloodworm or a crypt rat, and tunnel monster kills add reward potential.
 */
public final class BarrowsHooks {
    private static final String SPADE = "spade";
    /** OSRS drains prayer every 18 seconds (30 game ticks of 12 Minecraft ticks) in the crypts and tunnels. */
    private static final int DRAIN_INTERVAL = BarrowsRun.PRAYER_DRAIN_TICKS * com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK;
    private static long ticks;

    /** A puzzle door waiting for the player's answer. */
    private record Pending(int edge, BlockPos door, BarrowsPuzzles.Asked asked) {
    }

    private static final Map<UUID, Pending> PUZZLES = new HashMap<>();

    private BarrowsHooks() {
    }

    static void register() {
        BlockHooks.register(Barrows.SARCOPHAGUS, BarrowsHooks::sarcophagus);
        BlockHooks.register(Barrows.CRYPT_STAIRS, BarrowsHooks::stairs);
        BlockHooks.register(Barrows.TUNNEL_DOOR, BarrowsHooks::door);
        BlockHooks.register(Barrows.CHEST, BarrowsHooks::chest);

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (world.isClientSide() || stack.isEmpty() || !SPADE.equals(Inv.idOf(stack)) || !(player instanceof ServerPlayer p)) {
                return InteractionResultHolder.pass(stack);
            }
            return dig(p) ? InteractionResultHolder.sidedSuccess(p.getItemInHand(hand), false) : InteractionResultHolder.pass(stack);
        });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClientSide() || hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer p)) {
                return InteractionResult.PASS;
            }
            ItemStack stack = p.getMainHandItem();
            if (stack.isEmpty() || !SPADE.equals(Inv.idOf(stack))) {
                return InteractionResult.PASS;
            }
            return dig(p) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        });

        GameEvents.onKill((p, e, stats) -> {
            Boss boss = ModEntities.boss(e.getType());
            int i = Barrows.indexOf(boss);
            if (i < 0) {
                BlockPos at = e.blockPosition();
                if (boss == null && Gielinor.is(p) && Barrows.inTunnels(at.getX(), at.getY(), at.getZ())) {
                    BarrowsState state = BarrowsState.get(p.getServer());
                    state.run(p.getUUID()).addMonsterKill(stats.combatLevel());
                    state.setDirty();
                }
                return;
            }
            BarrowsState state = BarrowsState.get(p.getServer());
            BarrowsRun run = state.run(p.getUUID());
            if (!run.kill(i)) {
                return;
            }
            state.setDirty();
            Msg.game(p, "You have defeated " + run.killedCount() + " of the six Barrows brothers this run.");
            if (run.killedCount() == 5 && !run.killed(run.tunnel())) {
                Msg.game(p, "Only one sarcophagus is left to search - it may hide something.");
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("barrows")
                        .then(Commands.literal("tunnel")
                                .then(Commands.literal("yes").executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p != null) {
                                        confirmTunnel(p);
                                    }
                                    return 1;
                                }))
                                .then(Commands.literal("no").executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p != null) {
                                        Msg.game(p, "You leave the tunnel alone.");
                                    }
                                    return 1;
                                })))
                        .then(Commands.literal("puzzle")
                                .then(Commands.literal("1").executes(ctx -> answerPuzzle(ctx.getSource().getPlayer(), 0)))
                                .then(Commands.literal("2").executes(ctx -> answerPuzzle(ctx.getSource().getPlayer(), 1)))
                                .then(Commands.literal("3").executes(ctx -> answerPuzzle(ctx.getSource().getPlayer(), 2))))));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ++ticks;
            if (ticks % DRAIN_INTERVAL != 0) {
                return;
            }
            ServerLevel world = Gielinor.world(server);
            if (world == null) {
                return;
            }
            for (ServerPlayer p : world.players()) {
                BlockPos at = p.blockPosition();
                boolean alive = p.isAlive() && !p.isSpectator();
                if (alive && Barrows.underground(at.getX(), at.getY(), at.getZ())
                        && !com.runecraft.content.systems.CombatAchievementService.barrowsDrainNullified(p)) {
                    drainPrayer(p);
                }
            }
        });
    }

    private static BarrowsState state(ServerPlayer p) {
        return BarrowsState.get(p.getServer());
    }

    /** Teleports within Gielinor, building the structures there first so the player never lands in rock. */
    private static void moveTo(ServerPlayer p, ServerLevel world, int x, int y, int z, float yaw) {
        StructurePlacer.applyAround(world, x, z, 12);
        p.stopRiding();
        p.teleportTo(world, x + 0.5, y, z + 0.5, yaw, p.getXRot());
    }

    // ------------------------------------------------------------------ mounds and crypts

    /** Digging with a spade: on top of a mound the player breaks into its crypt. */
    static boolean dig(ServerPlayer p) {
        if (!Gielinor.is(p) || !(p.level() instanceof ServerLevel world)) {
            return false;
        }
        BlockPos at = p.blockPosition();
        Barrows.Crypt crypt = Barrows.moundAt(at.getX(), at.getY(), at.getZ());
        if (crypt == null) {
            return false;
        }
        world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.getX(), p.getY(), p.getZ(), 8, 0.3, 0.1, 0.3, 0.01);
        world.playSound(null, at, SoundEvents.SHOVEL_FLATTEN, SoundSource.PLAYERS, 1f, 1f);
        int[] a = crypt.arrival();
        moveTo(p, world, a[0], a[1], a[2], 180f);
        Msg.game(p, "You dig into the mound... and break into a crypt!");
        Msg.game(p, "This is " + crypt.brother().displayName() + "'s crypt. Search the sarcophagus.");
        return true;
    }

    private static void stairs(ServerPlayer p, BlockPos pos, BlockState state) {
        Barrows.Crypt crypt = Barrows.cryptAt(pos.getX(), pos.getY(), pos.getZ());
        if (crypt == null || !(p.level() instanceof ServerLevel world)) {
            Msg.game(p, "The stairs are blocked.");
            return;
        }
        StructurePlacer.applyAround(world, crypt.x(), crypt.z(), 12);
        BlockPos top = Gielinor.safeSpot(world, crypt.x(), crypt.z());
        p.stopRiding();
        p.teleportTo(world, top.getX() + 0.5, top.getY(), top.getZ() + 0.5, p.getYRot(), p.getXRot());
        Msg.game(p, "You climb the stairs to the top of the mound.");
    }

    private static void sarcophagus(ServerPlayer p, BlockPos pos, BlockState blockState) {
        Barrows.Crypt crypt = Barrows.cryptAt(pos.getX(), pos.getY(), pos.getZ());
        if (crypt == null || !(p.level() instanceof ServerLevel world)) {
            Msg.game(p, "The sarcophagus is sealed.");
            return;
        }
        BarrowsState state = state(p);
        BarrowsRun run = state.run(p.getUUID());
        switch (run.search(crypt.index())) {
            case EMPTY -> Msg.game(p, "You search the sarcophagus but find nothing.");
            case TUNNEL -> {
                if (p.isShiftKeyDown()) {
                    enterTunnels(p, world, run);
                    return;
                }
                MutableComponent line = Component.literal("You find a hidden tunnel, do you want to enter? ").withStyle(ChatFormatting.GRAY);
                line.append(link("Yes", "/barrows tunnel yes", "Enter the tunnels", ChatFormatting.GREEN)).append(Component.literal(" "));
                line.append(link("No", "/barrows tunnel no", "Stay in the crypt", ChatFormatting.RED));
                p.displayClientMessage(line, false);
                Msg.game(p, "(Or sneak while searching to enter at once.)");
            }
            case SUMMON -> {
                int[] f = crypt.spawnFloor();
                BossService.summon(p, world, new BlockPos(f[0], f[1], f[2]), crypt.brother());
                run.summon(crypt.index());
                state.setDirty();
            }
        }
    }

    private static MutableComponent link(String label, String command, String hover, ChatFormatting color) {
        return Component.literal("[" + label + "]").withStyle(s -> s.withColor(color)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover))));
    }

    /** [Yes] on the hidden-tunnel prompt: only works standing in this run's tunnel brother's crypt. */
    private static void confirmTunnel(ServerPlayer p) {
        if (!Gielinor.is(p) || !(p.level() instanceof ServerLevel world)) {
            return;
        }
        BlockPos at = p.blockPosition();
        Barrows.Crypt crypt = Barrows.cryptAt(at.getX(), at.getY(), at.getZ());
        BarrowsRun run = state(p).run(p.getUUID());
        if (crypt == null || run.search(crypt.index()) != BarrowsRun.Search.TUNNEL) {
            Msg.game(p, "There is no tunnel here.");
            return;
        }
        enterTunnels(p, world, run);
    }

    private static void enterTunnels(ServerPlayer p, ServerLevel world, BarrowsRun run) {
        int[] c = Barrows.roomCentre(run.startRoom());
        moveTo(p, world, c[0], Barrows.TUNNEL_FLOOR + 1, c[1], p.getYRot());
        Msg.game(p, "You climb down into the tunnels. Find your way to the chest in the centre.");
    }

    // ------------------------------------------------------------------ tunnels

    private static void door(ServerPlayer p, BlockPos pos, BlockState state) {
        int edge = Barrows.edgeAtDoor(pos.getX(), pos.getZ());
        BarrowsRun run = state(p).run(p.getUUID());
        if (edge < 0 || !run.doorOpen(edge)) {
            Msg.game(p, "The door is locked. You'll have to find another way round.");
            return;
        }
        // OSRS: the doors into the chest room may pose a puzzle (only on the way in).
        if (run.needsPuzzle(edge) && Barrows.roomBeyondDoor(edge, p.getBlockX(), p.getBlockZ()) == Barrows.CHEST_ROOM) {
            Pending old = PUZZLES.get(p.getUUID());
            BarrowsPuzzles.Asked asked = old != null && old.edge() == edge ? old.asked()
                    : BarrowsPuzzles.roll(new Random(p.getRandom().nextLong()), null);
            PUZZLES.put(p.getUUID(), new Pending(edge, pos.immutable(), asked));
            askPuzzle(p, asked);
            return;
        }
        openDoor(p, pos, edge);
    }

    /** Opens a tunnel door: the player steps through, and the door may wake something in the room beyond. */
    private static void openDoor(ServerPlayer p, BlockPos pos, int edge) {
        int room = Barrows.roomBeyondDoor(edge, p.getBlockX(), p.getBlockZ());
        p.level().playSound(null, pos, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 0.8f, 0.8f);
        DungeonsHooks.passThrough(p, pos);
        if (p.level() instanceof ServerLevel world) {
            wakeFromDoor(p, world, room);
        }
    }

    /**
     * What a door wakes in the room the player steps into (OSRS Wiki): a brother still alive this run (12/128, the hidden
     * one too), a skeleton, a bloodworm or a crypt rat; nothing once the room holds {@value BarrowsRun#ROOM_ENEMY_CAP}
     * enemies. Returns what came, or null. Public for the self-test.
     */
    public static String wakeFromDoor(ServerPlayer p, ServerLevel world, int room) {
        if (room < 0) {
            return null;
        }
        int[] c = Barrows.roomCentre(room);
        AABB box = new AABB(c[0] - Barrows.ROOM_HALF, Barrows.TUNNEL_FLOOR, c[1] - Barrows.ROOM_HALF, c[0] + Barrows.ROOM_HALF + 1,
                Barrows.TUNNEL_FLOOR + Barrows.TUNNEL_HEIGHT + 1, c[1] + Barrows.ROOM_HALF + 1);
        int enemies = world.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, box,
                e -> e.isAlive() && e instanceof net.minecraft.world.entity.monster.Enemy).size();
        if (enemies >= BarrowsRun.ROOM_ENEMY_CAP) {
            return null;
        }
        BarrowsState state = state(p);
        BarrowsRun run = state.run(p.getUUID());
        BarrowsRun.DoorSpawn[] spawn = new BarrowsRun.DoorSpawn[1];
        int brother = run.doorEncounter(new Random(p.getRandom().nextLong()), brotherUp(world, p.blockPosition()), spawn);
        // A step further into the room than the player, so it doesn't land in the doorway.
        int sx = Integer.signum(c[0] - p.getBlockX()) * 2;
        int sz = Integer.signum(c[1] - p.getBlockZ()) * 2;
        BlockPos at = new BlockPos(p.getBlockX() + sx, Barrows.TUNNEL_FLOOR, p.getBlockZ() + sz);
        if (brother >= 0) {
            Msg.game(p, "You hear footsteps in the darkness...");
            Boss b = Barrows.BROTHERS.get(brother);
            BossService.summon(p, world, at, b);
            run.summon(brother);
            state.setDirty();
            return b.entityId();
        }
        if (spawn[0] == null) {
            return null;
        }
        String id = switch (spawn[0]) {
            case SKELETON -> BarrowsRun.SKELETON;
            case BLOODWORM -> BarrowsRun.BLOODWORM;
            default -> BarrowsRun.CRYPT_RAT;
        };
        EntityType<?> type = ModEntities.typeById(id);
        if (type == null || !(type.create(world) instanceof net.minecraft.world.entity.Mob mob)) {
            return null;
        }
        mob.moveTo(at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, p.getRandom().nextFloat() * 360f, 0);
        mob.finalizeSpawn(world, world.getCurrentDifficultyAt(at), net.minecraft.world.entity.MobSpawnType.EVENT, null);
        mob.setTarget(p);
        world.addFreshEntity(mob);
        return id;
    }

    private static void askPuzzle(ServerPlayer p, BarrowsPuzzles.Asked asked) {
        Msg.game(p, "The door is sealed by a puzzle. " + BarrowsPuzzles.PROMPT);
        p.displayClientMessage(Component.literal(asked.puzzle().question()).withStyle(ChatFormatting.YELLOW), false);
        MutableComponent line = Component.literal("Answer: ").withStyle(ChatFormatting.GRAY);
        List<String> options = asked.options();
        for (int i = 0; i < options.size(); i++) {
            line.append(link(options.get(i), "/barrows puzzle " + (i + 1), "Answer " + options.get(i), ChatFormatting.AQUA));
            line.append(Component.literal(" "));
        }
        p.displayClientMessage(line, false);
    }

    /** A click on one of the puzzle answers ({@code choice} 0..2). */
    static int answerPuzzle(ServerPlayer p, int choice) {
        if (p == null) {
            return 0;
        }
        UUID uuid = p.getUUID();
        Pending pending = PUZZLES.get(uuid);
        if (pending == null || !Gielinor.is(p)) {
            Msg.game(p, "No door is waiting for an answer.");
            return 1;
        }
        if (p.blockPosition().distSqr(pending.door()) > 6 * 6) {
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
            state.setDirty();
            Msg.game(p, "You hear the door's locking mechanism grind open.");
            openDoor(p, pending.door(), pending.edge());
            return 1;
        }
        // OSRS: a wrong answer resets the door - it stays shut and asks something else next time.
        BarrowsPuzzles.Asked next = BarrowsPuzzles.roll(new Random(p.getRandom().nextLong()), pending.asked().puzzle());
        PUZZLES.put(uuid, new Pending(pending.edge(), pending.door(), next));
        Msg.red(p, "Wrong answer! The door stays shut. Open it again for a new puzzle.");
        return 1;
    }

    // ------------------------------------------------------------------ brothers in the tunnels

    /** True if any Barrows brother is alive near {@code pos} (at most one brother at a time). */
    private static boolean brotherUp(ServerLevel world, BlockPos pos) {
        AABB box = new AABB(pos).inflate(48);
        for (Boss b : Barrows.BROTHERS) {
            EntityType<BossEntity> type = ModEntities.BOSS_TYPES.get(b);
            if (type != null && !world.getEntities(type, box, e -> e.isAlive()).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ self-test

    /**
     * Self-test (the fight of a Barrows brother): gives {@code p} a run of the test's own in which brother {@code brother} is
     * summoned, as searching his sarcophagus does (another brother holds the tunnel), so the fight's kill counts in it. Returns
     * the player's own run (null when he had none), to hand back to {@link #endSelfTestRun} so a real player's run is kept.
     */
    public static BarrowsRun startSelfTestRun(ServerPlayer p, int brother) {
        BarrowsRun run = BarrowsRun.of(0, 0, (brother + 1) % Barrows.BROTHERS.size(), 0L, false);
        run.summon(brother);
        return state(p).replace(p.getUUID(), run);
    }

    /** Self-test: the right answer (0..2) to the puzzle a door is asking the player, or -1 when none waits. */
    static int selfTestPuzzleChoice(ServerPlayer p) {
        Pending pending = PUZZLES.get(p.getUUID());
        if (pending == null) {
            return -1;
        }
        for (int i = 0; i < pending.asked().options().size(); i++) {
            if (pending.asked().isCorrect(i)) {
                return i;
            }
        }
        return -1;
    }

    /** The player's current Barrows run, as the chest would read it (self-test: what the fight's kill did to the run). */
    public static BarrowsRun selfTestRun(ServerPlayer p) {
        return state(p).run(p.getUUID());
    }

    /** Puts back the run {@link #startSelfTestRun} replaced (none when null). */
    public static void endSelfTestRun(ServerPlayer p, BarrowsRun saved) {
        state(p).replace(p.getUUID(), saved);
    }

    /** Rune quantity multiplier for a player's Barrows chest (Morytania hard diary perk; 1.0 = none). */
    public static java.util.function.ToDoubleFunction<ServerPlayer> runeBonus = p -> 1.0;

    private static void chest(ServerPlayer p, BlockPos pos, BlockState blockState) {
        if (!(p.level() instanceof ServerLevel world)) {
            return;
        }
        BarrowsState state = state(p);
        BarrowsRun run = state.run(p.getUUID());
        if (run.open() == BarrowsRun.Open.SUMMON_HIDDEN) {
            Boss hidden = run.tunnelBrother();
            Msg.game(p, "You open the chest... and something stirs in the dark!");
            BossService.summon(p, world, pos.offset(2, -1, 0), hidden);
            run.summon(run.tunnel());
            state.setDirty();
            return;
        }
        List<BarrowsRun.Reward> rewards = run.rollRewards(new Random(p.getRandom().nextLong()));
        int killed = run.killedCount();
        Msg.game(p, "You open the chest (" + killed + " of 6 brothers killed) and find:");
        Msg.game(p, "Reward potential: " + run.rewardPotential() + "/" + BarrowsRun.MAX_POTENTIAL
                + (run.monsterKills() > 0 ? " (" + run.monsterKills() + " tunnel kills)." : "."));
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
        PUZZLES.remove(p.getUUID());
        state.setDirty();
        world.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1f, 0.8f);
        int x = GielinorMap.toBlockX(Barrows.SURFACE_TILE[0]);
        int z = GielinorMap.toBlockZ(Barrows.SURFACE_TILE[1]);
        StructurePlacer.applyAround(world, x, z, 12);
        BlockPos top = Gielinor.safeSpot(world, x, z);
        p.stopRiding();
        p.teleportTo(world, top.getX() + 0.5, top.getY(), top.getZ() + 0.5, p.getYRot(), p.getXRot());
        Msg.game(p, "The crypts rumble and you find yourself back on the surface.");
        ServerNetworking.sync(p);
    }

    // ------------------------------------------------------------------ prayer drain

    /** OSRS Wiki: the brothers' faces drain 8 prayer points in the crypts and tunnels, one more for each brother killed this run. */
    private static void drainPrayer(ServerPlayer p) {
        PlayerData data = Players.data(p);
        if (data.prayerPoints() <= 0) {
            return;
        }
        int killed = state(p).run(p.getUUID()).killedCount();
        data.setPrayerPoints(data.prayerPoints() - BarrowsRun.prayerDrain(killed));
        ServerNetworking.sync(p);
    }
}
