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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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
    public final ServerLevel world;
    public final BlockPos origin;
    /** Entrance block used to join (selects difficulty, role or room). */
    public final String variant;
    protected final List<UUID> players = new ArrayList<>();
    private final Map<UUID, BlockPos> returnTo = new HashMap<>();
    protected final Random random = new Random();
    protected long ticks;
    private boolean ended;

    protected MinigameSession(Minigame game, int id, int slot, ServerLevel world, String variant) {
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
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return origin.offset(index % 3 - 1, 1, 0);
    }

    /** Called once after every player has been teleported in. */
    protected void start() {
    }

    /** Every OSRS game tick. */
    protected abstract void gameTick();

    /** A player killed a creature belonging to this session. */
    protected void onKill(ServerPlayer player, LivingEntity entity, String creatureId) {
    }

    /** A player clicked one of the module's arena blocks. Return true if handled. */
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        return false;
    }

    /** A player would have died. Default: they're removed from the game (items kept). */
    protected void onDeath(ServerPlayer player) {
        Msg.red(player, "Oh dear, you are dead! You have been removed from " + game.displayName() + ".");
        removePlayer(player, true);
    }

    /**
     * Dangerous minigames (Temple Trekking) return true: a death there is a real one. The player is sent back to where they
     * entered and dies there, keeping only the usual items kept on death.
     */
    protected boolean dangerous() {
        return false;
    }

    /** A death in a dangerous minigame: the player leaves the session at its entry point, then the normal death runs. */
    final void dieDangerously(ServerPlayer player) {
        removePlayer(player, true);
    }

    final boolean isDangerous() {
        return dangerous();
    }

    /** A player left (logout, exit portal, /minigame leave or death). */
    protected void onLeave(ServerPlayer player) {
    }

    /**
     * A player is logging out mid-game, just before they are removed (and {@link #onLeave} runs): games that pause instead
     * of ending (the Fight Cave, the Inferno) save where to pick up.
     */
    protected void onDisconnect(ServerPlayer player) {
    }

    /** Where a player in this session goes back to when they leave (the spot they entered from), or null. */
    protected final BlockPos returnPoint(ServerPlayer player) {
        return returnTo.get(player.getUUID());
    }

    /** Sends a player somewhere else than where they entered from when they leave (a run resumed after logging out). */
    protected final void setReturnPoint(ServerPlayer player, BlockPos pos) {
        if (players.contains(player.getUUID()) && pos != null) {
            returnTo.put(player.getUUID(), pos.immutable());
        }
    }

    /** Rewards and messages when the game finishes; players are still inside. */
    protected void onEnd(boolean success) {
    }

    /** The session is gone (after rewards and after every player left): release chunks or other resources. */
    protected void onClose() {
    }

    /** Multiplier on a session player's max hit (role bonuses). */
    protected double damageMultiplier(ServerPlayer player, LivingEntity target, com.runecraft.osrs.AttackType type) {
        return 1.0;
    }

    /** A player damaged one of this session's creatures (or another player in PvP games). */
    protected void onPlayerHit(ServerPlayer player, LivingEntity target, int dealt) {
    }

    /** {@link #onPlayerHit(ServerPlayer, LivingEntity, int)} with the attack's style (null when unknown, e.g. PvP melee). */
    protected void onPlayerHit(ServerPlayer player, LivingEntity target, com.runecraft.osrs.AttackType type, int dealt) {
        onPlayerHit(player, target, dealt);
    }

    /**
     * Whether players in this session gain experience (and Slayer task credit, which goes with the Slayer XP). Nightmare Zone
     * practice dreams return false.
     */
    public boolean awardsXp() {
        return true;
    }

    /** Whether two players in this session may fight each other (team PvP). */
    protected boolean canAttackPlayer(ServerPlayer attacker, ServerPlayer target) {
        return false;
    }

    /** Combat Achievement runs: the challenge or difficulty played (0 = none). Still read after the session ends. */
    public int caStage() {
        return 0;
    }

    /** Combat Achievement runs: conditions the session judged itself for this player. Still read after the session ends. */
    public java.util.Set<String> caFlags(ServerPlayer player) {
        return java.util.Set.of();
    }

    /** Overlay lines shown to a player. */
    protected List<String> hud(ServerPlayer player) {
        return List.of(game.displayName());
    }

    // ------------------------------------------------------------------ framework

    final void begin(List<ServerPlayer> joining) {
        build();
        int i = 0;
        for (ServerPlayer p : joining) {
            players.add(p.getUUID());
            returnTo.put(p.getUUID(), p.blockPosition());
            BlockPos at = spawnPoint(p, i++);
            p.teleportTo(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYRot(), p.getXRot());
            Msg.game(p, "Welcome to " + game.displayName() + ".");
        }
        start();
        for (ServerPlayer p : online()) {
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

    public final boolean has(ServerPlayer player) {
        return players.contains(player.getUUID());
    }

    public final List<ServerPlayer> online() {
        List<ServerPlayer> list = new ArrayList<>();
        for (UUID u : players) {
            ServerPlayer p = world.getServer().getPlayerList().getPlayer(u);
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
    public final void removePlayer(ServerPlayer player, boolean teleport) {
        if (!players.remove(player.getUUID())) {
            return;
        }
        leave(player);
        BlockPos back = returnTo.remove(player.getUUID());
        MinigameManager.forget(player);
        if (teleport && back != null) {
            player.teleportTo(world, back.getX() + 0.5, back.getY(), back.getZ() + 0.5, player.getYRot(), player.getXRot());
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

    /** {@link #onLeave}, with a failure logged so the player is still sent back and the session still released. */
    private void leave(ServerPlayer player) {
        try {
            onLeave(player);
        } catch (RuntimeException e) {
            com.runecraft.RuneCraft.LOGGER.error("Minigame {} failed to let {} leave", game.id(), player.getGameProfile().getName(), e);
        }
    }

    private void finish(boolean success, boolean rewards) {
        if (ended) {
            return;
        }
        ended = true;
        List<ServerPlayer> online = online();
        if (rewards) {
            try {
                onEnd(success);
            } catch (RuntimeException e) {
                com.runecraft.RuneCraft.LOGGER.error("Minigame {} failed to hand out rewards", game.id(), e);
            }
            if (success) {
                for (ServerPlayer p : online) {
                    com.runecraft.content.GameEvents.fireActivity(p, "minigame_win:" + game.id());
                }
            }
        }
        AABB box = box();
        for (Entity e : world.getEntitiesOfClass(Entity.class, box, e -> !(e instanceof Player))) {
            if (e instanceof ItemEntity item && !online.isEmpty()) {
                Inv.give(online.get(0), item.getItem().copy());
            }
            e.discard();
        }
        for (ServerPlayer p : online) {
            players.remove(p.getUUID());
            leave(p);
            BlockPos back = returnTo.remove(p.getUUID());
            MinigameManager.forget(p);
            if (back != null) {
                p.teleportTo(world, back.getX() + 0.5, back.getY(), back.getZ() + 0.5, p.getYRot(), p.getXRot());
            }
            MinigameNet.sendHud(p, List.of());
        }
        players.clear();
        try {
            onClose();
        } catch (RuntimeException e) {
            com.runecraft.RuneCraft.LOGGER.error("Minigame {} failed to close", game.id(), e);
        }
        MinigameManager.release(this);
    }

    public final AABB box() {
        return new AABB(Vec3.atLowerCornerOf(origin.offset(-halfX() - 2, -2, -halfZ() - 2)),
                Vec3.atLowerCornerOf(origin.offset(halfX() + 2, height() + 3, halfZ() + 2)));
    }

    private void removeStrays() {
        String tag = TAG + "_" + id;
        List<com.runecraft.boss.DataBossEntity> bosses = world.getEntitiesOfClass(com.runecraft.boss.DataBossEntity.class, box(),
                b -> b.getTags().contains(tag));
        for (Mob mob : world.getEntitiesOfClass(Mob.class, box(), m -> !m.getTags().contains(tag))) {
            if (mob instanceof com.runecraft.entity.QuestNpcEntity) {
                continue;
            }
            // Minions summoned by one of our bosses join the session; anything else is a stray spawn.
            boolean minion = bosses.stream().anyMatch(b -> b.distanceToSqr(mob) < 64);
            if (minion) {
                tag(mob);
            } else {
                mob.discard();
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Builds the sealed arena room with the given materials. */
    protected final void room(net.minecraft.world.level.block.Block floor, net.minecraft.world.level.block.Block wall, net.minecraft.world.level.block.Block accent) {
        Arena.room(world, origin, halfX(), halfZ(), height(), floor.defaultBlockState(), wall.defaultBlockState(), accent.defaultBlockState());
    }

    protected final BlockPos rel(int dx, int dy, int dz) {
        return origin.offset(dx, dy, dz);
    }

    protected final void set(int dx, int dy, int dz, BlockState state) {
        Arena.set(world, rel(dx, dy, dz), state);
    }

    protected final void place(int dx, int dz, String moduleBlock) {
        Arena.set(world, rel(dx, 1, dz), Arena.module(moduleBlock));
    }

    /** Spawns a creature (module monster, data boss or built-in) at an arena-relative position. */
    protected final Mob spawn(String creatureId, BlockPos at) {
        EntityType<?> type = ModEntities.typeById(creatureId);
        if (type == null) {
            com.runecraft.RuneCraft.LOGGER.warn("Unknown creature {}", creatureId);
            return null;
        }
        return spawnType(type, at);
    }

    protected final Mob spawnType(EntityType<?> type, BlockPos at) {
        Entity e = type.create(world);
        if (!(e instanceof Mob mob)) {
            return null;
        }
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360f, 0);
        mob.finalizeSpawn(world, world.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
        tag(mob);
        world.addFreshEntity(mob);
        if (mob instanceof com.runecraft.boss.DataBossEntity boss) {
            boss.instanceTag = TAG + "_" + id;
            boss.script().onSpawn(boss);
        }
        return mob;
    }

    protected final void tag(Mob mob) {
        mob.addTag(TAG);
        mob.addTag(TAG + "_" + id);
        mob.setPersistenceRequired();
    }

    /** Sets a creature's max and current hitpoints (wave scaling). */
    protected static void setHitpoints(LivingEntity mob, int hp) {
        AttributeInstance attr = mob.getAttribute(Attributes.MAX_HEALTH);
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
    protected final void aggro(Mob mob) {
        List<ServerPlayer> online = online();
        if (!online.isEmpty()) {
            mob.setTarget(online.get(random.nextInt(online.size())));
        }
    }

    /** Environmental damage (traps, cold, waves) with a hitsplat. */
    protected static void hurt(ServerPlayer p, int damage) {
        if (damage <= 0) {
            return;
        }
        com.runecraft.combat.CombatHandler.applyDamage(null, p, damage);
        com.runecraft.network.ServerNetworking.hitsplat(p, damage, com.runecraft.network.Net.SPLAT_DAMAGE);
    }

    /** Starts a repeating per-game-tick action for a player (stops when the step returns false). */
    protected static void repeat(ServerPlayer p, java.util.function.Predicate<ServerPlayer> step) {
        com.runecraft.action.ActionManager.start(p, (player, tick) -> step.test(player));
    }

    protected final void broadcast(String message) {
        for (ServerPlayer p : online()) {
            Msg.game(p, message);
        }
    }

    protected final void broadcastRed(String message) {
        for (ServerPlayer p : online()) {
            p.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.RED), false);
        }
    }

    protected final int level(ServerPlayer p, Skill skill) {
        return Players.data(p).skills().level(skill);
    }

    protected static void xp(ServerPlayer p, Skill skill, double amount) {
        if (amount > 0) {
            XpService.add(p, skill, amount);
        }
    }

    /** Gives an item reward, logs it and fires the obtain event. */
    protected static void reward(ServerPlayer p, String item, int count) {
        if (count <= 0) {
            return;
        }
        Inv.give(p, item, count);
        CollectionLog.obtain(p, item);
    }

    /** Rolls a loot table once and gives the result. */
    protected final void rollLoot(ServerPlayer p, List<MinigameRules.Loot> table) {
        MinigameRules.Loot l = MinigameRules.pick(table, random.nextDouble());
        reward(p, l.item(), l.min() + random.nextInt(l.max() - l.min() + 1));
    }

    protected final void addPoints(ServerPlayer p, String currency, int amount) {
        MinigameData.of(p).addPoints(p.getUUID(), com.runecraft.osrs.content.minigames.MinigameCurrencies.key(currency), amount);
    }

    protected final List<LivingEntity> living(String creatureId) {
        String tag = TAG + "_" + id;
        List<LivingEntity> list = new ArrayList<>();
        for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, box(), x -> x.isAlive() && x.getTags().contains(tag))) {
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
    protected final BlockPos relPos(ServerPlayer p) {
        return p.blockPosition().subtract(origin);
    }
}
