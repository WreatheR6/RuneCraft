package com.runecraft.content.minigames;

import com.runecraft.collection.CollectionLog;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameRules;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * One running instance of a minigame. Subclasses build their arena around {@link #origin}, drive
 * the game every OSRS game tick and react to kills, block clicks and deaths. Players who die in a
 * minigame don't lose items (like OSRS's safe minigames) and are handled by {@link #onDeath}.
 */
public abstract class MinigameSession {
    public static final String TAG = "rcmg";

    public final Minigame game;
    public final int id;
    public final int slot;
    public final ServerWorld world;
    public final BlockPos origin;
    /** Entrance block used to join (selects difficulty, role or room). */
    public final String variant;
    protected final List<UUID> players = new ArrayList<>();
    private final Map<UUID, BlockPos> returnTo = new HashMap<>();
    protected final Random random = new Random();
    protected long ticks;
    private boolean ended;

    protected MinigameSession(Minigame game, int id, int slot, ServerWorld world, String variant) {
        this.game = game;
        this.id = id;
        this.slot = slot;
        this.world = world;
        this.origin = Arena.origin(game, slot);
        this.variant = variant;
    }

    // ------------------------------------------------------------------ lifecycle hooks

    /** Arena half width (x), used for cleanup and stray-mob checks. */
    protected int halfX() {
        return 24;
    }

    protected int halfZ() {
        return 24;
    }

    protected int height() {
        return 10;
    }

    /** Builds the arena blocks. */
    protected abstract void build();

    /** Where a player appears when entering. */
    protected BlockPos spawnPoint(ServerPlayerEntity player, int index) {
        return origin.add(index % 3 - 1, 1, 0);
    }

    /** Called once after every player has been teleported in. */
    protected void start() {
    }

    /** Every OSRS game tick. */
    protected abstract void gameTick();

    /** A player killed a creature belonging to this session. */
    protected void onKill(ServerPlayerEntity player, LivingEntity entity, String creatureId) {
    }

    /** A player clicked one of the module's arena blocks. Return true if handled. */
    protected boolean onBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
        return false;
    }

    /** A player would have died. Default: they're removed from the game (items kept). */
    protected void onDeath(ServerPlayerEntity player) {
        Msg.red(player, "Oh dear, you are dead! You have been removed from " + game.displayName() + ".");
        removePlayer(player, true);
    }

    /** A player left (logout, exit portal, /minigame leave or death). */
    protected void onLeave(ServerPlayerEntity player) {
    }

    /** Rewards and messages when the game finishes; players are still inside. */
    protected void onEnd(boolean success) {
    }

    /** Multiplier on a session player's max hit (role bonuses). */
    protected double damageMultiplier(ServerPlayerEntity player, LivingEntity target, com.runecraft.osrs.AttackType type) {
        return 1.0;
    }

    /** A player damaged one of this session's creatures (or another player in PvP games). */
    protected void onPlayerHit(ServerPlayerEntity player, LivingEntity target, int dealt) {
    }

    /** Whether two players in this session may fight each other (team PvP). */
    protected boolean canAttackPlayer(ServerPlayerEntity attacker, ServerPlayerEntity target) {
        return false;
    }

    /** Combat Achievement runs: the challenge or difficulty played (0 = none). Still read after the session ends. */
    public int caStage() {
        return 0;
    }

    /** Combat Achievement runs: conditions the session judged itself for this player. Still read after the session ends. */
    public java.util.Set<String> caFlags(ServerPlayerEntity player) {
        return java.util.Set.of();
    }

    /** Overlay lines shown to a player. */
    protected List<String> hud(ServerPlayerEntity player) {
        return List.of(game.displayName());
    }

    // ------------------------------------------------------------------ framework

    final void begin(List<ServerPlayerEntity> joining) {
        build();
        int i = 0;
        for (ServerPlayerEntity p : joining) {
            players.add(p.getUuid());
            returnTo.put(p.getUuid(), p.getBlockPos());
            BlockPos at = spawnPoint(p, i++);
            p.teleport(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYaw(), p.getPitch());
            Msg.game(p, "Welcome to " + game.displayName() + ".");
        }
        start();
        for (ServerPlayerEntity p : online()) {
            com.runecraft.content.GameEvents.fireActivity(p, "minigame_start:" + game.id());
        }
    }

    final void tick() {
        if (ended) {
            return;
        }
        ticks++;
        if (ticks % 5 == 0) {
            removeStrays();
        }
        gameTick();
    }

    public final boolean isEnded() {
        return ended;
    }

    public final boolean has(ServerPlayerEntity player) {
        return players.contains(player.getUuid());
    }

    public final List<ServerPlayerEntity> online() {
        List<ServerPlayerEntity> list = new ArrayList<>();
        for (UUID u : players) {
            ServerPlayerEntity p = world.getServer().getPlayerManager().getPlayer(u);
            if (p != null) {
                list.add(p);
            }
        }
        return list;
    }

    public final int playerCount() {
        return players.size();
    }

    /** Sends a player back to where they entered and drops them from the session. */
    public final void removePlayer(ServerPlayerEntity player, boolean teleport) {
        if (!players.remove(player.getUuid())) {
            return;
        }
        onLeave(player);
        BlockPos back = returnTo.remove(player.getUuid());
        MinigameManager.forget(player);
        if (teleport && back != null) {
            player.teleport(world, back.getX() + 0.5, back.getY(), back.getZ() + 0.5, player.getYaw(), player.getPitch());
        }
        MinigameNet.sendHud(player, List.of());
        if (players.isEmpty() && !ended) {
            finish(false, false);
        }
    }

    /** Ends the game: rewards ({@link #onEnd}), loot on the floor goes to a player, everyone is sent back. */
    public final void end(boolean success) {
        finish(success, true);
    }

    private void finish(boolean success, boolean rewards) {
        if (ended) {
            return;
        }
        ended = true;
        List<ServerPlayerEntity> online = online();
        if (rewards) {
            try {
                onEnd(success);
            } catch (RuntimeException e) {
                com.runecraft.RuneCraft.LOGGER.error("Minigame {} failed to hand out rewards", game.id(), e);
            }
            if (success) {
                for (ServerPlayerEntity p : online) {
                    com.runecraft.content.GameEvents.fireActivity(p, "minigame_win:" + game.id());
                }
            }
        }
        Box box = box();
        for (Entity e : world.getEntitiesByClass(Entity.class, box, e -> !(e instanceof PlayerEntity))) {
            if (e instanceof ItemEntity item && !online.isEmpty()) {
                Inv.give(online.get(0), item.getStack().copy());
            }
            e.discard();
        }
        for (ServerPlayerEntity p : online) {
            players.remove(p.getUuid());
            onLeave(p);
            BlockPos back = returnTo.remove(p.getUuid());
            MinigameManager.forget(p);
            if (back != null) {
                p.teleport(world, back.getX() + 0.5, back.getY(), back.getZ() + 0.5, p.getYaw(), p.getPitch());
            }
            MinigameNet.sendHud(p, List.of());
        }
        players.clear();
        MinigameManager.release(this);
    }

    public final Box box() {
        return new Box(origin.add(-halfX() - 2, -2, -halfZ() - 2), origin.add(halfX() + 2, height() + 3, halfZ() + 2));
    }

    private void removeStrays() {
        String tag = TAG + "_" + id;
        List<com.runecraft.boss.DataBossEntity> bosses = world.getEntitiesByClass(com.runecraft.boss.DataBossEntity.class, box(),
                b -> b.getCommandTags().contains(tag));
        for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, box(), m -> !m.getCommandTags().contains(tag))) {
            if (mob instanceof com.runecraft.entity.QuestNpcEntity) {
                continue;
            }
            // Minions summoned by one of our bosses join the session; anything else is a stray spawn.
            boolean minion = bosses.stream().anyMatch(b -> b.squaredDistanceTo(mob) < 64);
            if (minion) {
                tag(mob);
            } else {
                mob.discard();
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Builds the sealed arena room with the given materials. */
    protected final void room(net.minecraft.block.Block floor, net.minecraft.block.Block wall, net.minecraft.block.Block accent) {
        Arena.room(world, origin, halfX(), halfZ(), height(), floor.getDefaultState(), wall.getDefaultState(), accent.getDefaultState());
    }

    protected final BlockPos rel(int dx, int dy, int dz) {
        return origin.add(dx, dy, dz);
    }

    protected final void set(int dx, int dy, int dz, BlockState state) {
        Arena.set(world, rel(dx, dy, dz), state);
    }

    protected final void place(int dx, int dz, String moduleBlock) {
        Arena.set(world, rel(dx, 1, dz), Arena.module(moduleBlock));
    }

    /** Spawns a creature (module monster, data boss or built-in) at an arena-relative position. */
    protected final MobEntity spawn(String creatureId, BlockPos at) {
        EntityType<?> type = ModEntities.typeById(creatureId);
        if (type == null) {
            com.runecraft.RuneCraft.LOGGER.warn("Unknown creature {}", creatureId);
            return null;
        }
        return spawnType(type, at);
    }

    protected final MobEntity spawnType(EntityType<?> type, BlockPos at) {
        Entity e = type.create(world);
        if (!(e instanceof MobEntity mob)) {
            return null;
        }
        mob.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360f, 0);
        mob.initialize(world, world.getLocalDifficulty(at), SpawnReason.EVENT, null, null);
        tag(mob);
        world.spawnEntity(mob);
        if (mob instanceof com.runecraft.boss.DataBossEntity boss) {
            boss.instanceTag = TAG + "_" + id;
            boss.script().onSpawn(boss);
        }
        return mob;
    }

    protected final void tag(MobEntity mob) {
        mob.addCommandTag(TAG);
        mob.addCommandTag(TAG + "_" + id);
        mob.setPersistent();
    }

    /** Sets a creature's max and current hitpoints (wave scaling). */
    protected static void setHitpoints(LivingEntity mob, int hp) {
        EntityAttributeInstance attr = mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(hp);
            mob.setHealth(hp);
        }
    }

    /** A random free spot on the arena floor within the given radius of a centre offset. */
    protected final BlockPos randomFloor(int cx, int cz, int radius) {
        int x = cx + random.nextInt(radius * 2 + 1) - radius;
        int z = cz + random.nextInt(radius * 2 + 1) - radius;
        x = Math.max(-halfX() + 1, Math.min(halfX() - 1, x));
        z = Math.max(-halfZ() + 1, Math.min(halfZ() - 1, z));
        return rel(x, 1, z);
    }

    /** Sets every online player's target as the creature's target. */
    protected final void aggro(MobEntity mob) {
        List<ServerPlayerEntity> online = online();
        if (!online.isEmpty()) {
            mob.setTarget(online.get(random.nextInt(online.size())));
        }
    }

    /** Environmental damage (traps, cold, waves) with a hitsplat. */
    protected static void hurt(ServerPlayerEntity p, int damage) {
        if (damage <= 0) {
            return;
        }
        com.runecraft.combat.CombatHandler.applyDamage(null, p, damage);
        com.runecraft.network.ServerNetworking.hitsplat(p, damage, com.runecraft.network.Net.SPLAT_DAMAGE);
    }

    /** Starts a repeating per-game-tick action for a player (stops when the step returns false). */
    protected static void repeat(ServerPlayerEntity p, java.util.function.Predicate<ServerPlayerEntity> step) {
        com.runecraft.action.ActionManager.start(p, (player, tick) -> step.test(player));
    }

    protected final void broadcast(String message) {
        for (ServerPlayerEntity p : online()) {
            Msg.game(p, message);
        }
    }

    protected final void broadcastRed(String message) {
        for (ServerPlayerEntity p : online()) {
            p.sendMessage(Text.literal(message).formatted(Formatting.RED), false);
        }
    }

    protected final int level(ServerPlayerEntity p, Skill skill) {
        return Players.data(p).skills().level(skill);
    }

    protected static void xp(ServerPlayerEntity p, Skill skill, double amount) {
        if (amount > 0) {
            XpService.add(p, skill, amount);
        }
    }

    /** Gives an item reward, logs it and fires the obtain event. */
    protected static void reward(ServerPlayerEntity p, String item, int count) {
        if (count <= 0) {
            return;
        }
        Inv.give(p, item, count);
        CollectionLog.obtain(p, item);
    }

    /** Rolls a loot table once and gives the result. */
    protected final void rollLoot(ServerPlayerEntity p, List<MinigameRules.Loot> table) {
        MinigameRules.Loot l = MinigameRules.pick(table, random.nextDouble());
        reward(p, l.item(), l.min() + random.nextInt(l.max() - l.min() + 1));
    }

    protected final void addPoints(ServerPlayerEntity p, String currency, int amount) {
        MinigameData.of(p).addPoints(p.getUuid(), com.runecraft.osrs.content.minigames.MinigameCurrencies.key(currency), amount);
    }

    protected final List<LivingEntity> living(String creatureId) {
        String tag = TAG + "_" + id;
        List<LivingEntity> list = new ArrayList<>();
        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, box(), x -> x.isAlive() && x.getCommandTags().contains(tag))) {
            if (creatureId == null || creatureId.equals(MinigameManager.creatureId(e))) {
                list.add(e);
            }
        }
        return list;
    }

    protected final int mobsAlive() {
        return living(null).size();
    }

    protected static String time(long gameTicks) {
        long seconds = Math.max(0, gameTicks) * 6 / 10;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    /** The player's current position relative to the arena origin. */
    protected final BlockPos relPos(ServerPlayerEntity p) {
        return p.getBlockPos().subtract(origin);
    }
}
