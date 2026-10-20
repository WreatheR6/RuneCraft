package com.runecraft.content.bosses;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.osrs.content.bosses.BossMechanics;
import com.runecraft.platform.ServerPlayConnectionEvents;
import com.runecraft.platform.ServerPlayerEvents;
import com.runecraft.platform.ServerTickEvents;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Damage over time that boss attacks leave on players: burns (Yama's fire specials; OSRS Wiki "Burn") and Vardorvis'
 * bleed. They tick every game tick for every online player, wherever the player is: a burn runs its course and the bleed
 * "will persist even through teleportation" (OSRS Wiki "Vardorvis"), so leaving the arena or the boss dying doesn't stop
 * them. Dying (or logging out) clears them. The boss is credited while it is still alive; afterwards the damage has no
 * attacker. approx: entering or leaving a player-owned house and the ornate rejuvenation pool don't clear burns here.
 */
final class BossDots {
    private static final Map<UUID, BossMechanics.Burn> BURNS = new HashMap<>();
    private static final Map<UUID, BossMechanics.Bleed> BLEEDS = new HashMap<>();
    private static final Map<UUID, Vec3> LAST_POS = new HashMap<>();
    /** Who inflicted each player's damage over time (credited while it is alive). */
    private static final Map<UUID, WeakReference<LivingEntity>> SOURCES = new HashMap<>();
    private static long lastGameTick = -1;

    private BossDots() {
    }

    /** Ticks every player's burns and bleeds once per game tick; clears them on death and logout. */
    static void register() {
        ServerTickEvents.END_SERVER_TICK.register(BossDots::serverTick);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) {
                clear(newPlayer.getUUID());
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> clear(handler.getPlayer().getUUID()));
    }

    private static void clear(UUID id) {
        BURNS.remove(id);
        BLEEDS.remove(id);
        LAST_POS.remove(id);
        SOURCES.remove(id);
    }

    /** Adds a burn of {@code hits} hits (1 damage per burn every 4 ticks; up to 5 burns stack). */
    static void burn(ServerPlayer p, int hits) {
        burn(null, p, hits);
    }

    /** Adds a burn inflicted by {@code source}. */
    static void burn(LivingEntity source, ServerPlayer p, int hits) {
        BURNS.computeIfAbsent(p.getUUID(), u -> new BossMechanics.Burn()).add(hits);
        source(source, p);
    }

    /** Starts (or restarts) a Vardorvis bleed. */
    static void bleed(ServerPlayer p) {
        bleed(null, p, BossMechanics.VARDORVIS_BLEED_DAMAGE);
    }

    /** Starts (or restarts) a bleed of {@code perHit} damage per bleed hit (awakened Vardorvis: 5). */
    static void bleed(ServerPlayer p, int perHit) {
        bleed(null, p, perHit);
    }

    /** Starts (or restarts) a bleed inflicted by {@code source}. */
    static void bleed(LivingEntity source, ServerPlayer p, int perHit) {
        BLEEDS.computeIfAbsent(p.getUUID(), u -> new BossMechanics.Bleed()).start(perHit);
        LAST_POS.put(p.getUUID(), p.position());
        source(source, p);
    }

    private static void source(LivingEntity source, ServerPlayer p) {
        if (source != null) {
            SOURCES.put(p.getUUID(), new WeakReference<>(source));
        }
    }

    static boolean burning(ServerPlayer p) {
        BossMechanics.Burn b = BURNS.get(p.getUUID());
        return b != null && b.stacks() > 0;
    }

    static boolean bleeding(ServerPlayer p) {
        BossMechanics.Bleed b = BLEEDS.get(p.getUUID());
        return b != null && b.active();
    }

    private static void serverTick(MinecraftServer server) {
        long gameTick = ActionManager.gameTick();
        if (gameTick == lastGameTick) {
            return;
        }
        lastGameTick = gameTick;
        if (BURNS.isEmpty() && BLEEDS.isEmpty()) {
            return;
        }
        java.util.Set<UUID> ids = new java.util.HashSet<>(BURNS.keySet());
        ids.addAll(BLEEDS.keySet());
        for (UUID id : new ArrayList<>(ids)) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) {
                clear(id);
                continue;
            }
            tick(p);
        }
    }

    /** The boss still credited with this player's damage over time, or null once it is gone. */
    private static LivingEntity source(ServerPlayer p) {
        WeakReference<LivingEntity> ref = SOURCES.get(p.getUUID());
        LivingEntity s = ref == null ? null : ref.get();
        return s != null && s.isAlive() && !s.isRemoved() && s.level() == p.level() ? s : null;
    }

    private static void hit(ServerPlayer p, String name, int damage) {
        LivingEntity source = source(p);
        CombatHooks.nameAttack(name, null);
        CombatHandler.applyDamage(source, p, damage);
        if (source == null) {
            // applyDamage only draws a hitsplat for an attacker; draw the burn/bleed splat here.
            com.runecraft.network.ServerNetworking.hitsplat(p, damage, com.runecraft.network.Net.SPLAT_DAMAGE);
        }
    }

    private static void tick(ServerPlayer p) {
        UUID id = p.getUUID();
        BossMechanics.Burn burn = BURNS.get(id);
        if (burn != null) {
            int d = burn.tick();
            if (d > 0 && p.isAlive()) {
                hit(p, "burn", d);
            }
            if (burn.stacks() == 0) {
                BURNS.remove(id);
            }
        }
        BossMechanics.Bleed bleed = BLEEDS.get(id);
        if (bleed != null) {
            Vec3 last = LAST_POS.put(id, p.position());
            // Running (sprinting, about a tile or more a tick) makes the bleed hit every tick.
            boolean running = last != null && p.isSprinting() && last.distanceToSqr(p.position()) > 1.0;
            int d = bleed.tick(running);
            if (d > 0 && p.isAlive()) {
                hit(p, "bleed", d);
            }
            if (!bleed.active()) {
                BLEEDS.remove(id);
                LAST_POS.remove(id);
            }
        }
        if (!BURNS.containsKey(id) && !BLEEDS.containsKey(id)) {
            SOURCES.remove(id);
        }
    }
}
