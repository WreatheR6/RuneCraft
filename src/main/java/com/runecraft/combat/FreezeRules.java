package com.runecraft.combat;

import com.runecraft.action.ActionManager;
import com.runecraft.osrs.FreezeLock;
import com.runecraft.platform.ServerTickEvents;
import com.runecraft.world.Gielinor;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * OSRS freezes on the server. A freeze is Slowness with amplifier {@value #FREEZE_AMPLIFIER} or more in Gielinor (ice
 * spells, Mechanics' FREEZE special, boss binds and stuns); no call site changes. The client side (no FOV zoom, no hopping
 * out, the HUD icon) is {@code client.FreezeClient}; the rules are {@link FreezeLock}'s.
 *
 * <ul>
 *   <li>Immunity: after a freeze ends the player is immune to the next one for {@value #IMMUNITY_MC} Minecraft ticks (OSRS: 5
 *   game ticks). Round 42: a freeze broken early (a cure, the husks or Vorkath's spawn killed, a bind released) ends it there,
 *   so its immunity starts then too, as in OSRS; before, only a freeze that ran out did. A freeze refreshed while it is still on
 *   is left to vanilla's merge, so the scripts that renew a bind every tick keep working.</li>
 *   <li>Enforcement (round 42): Minecraft moves a player on their own client, so the server holds a frozen player at the spot
 *   the freeze caught them and puts them back when they walk, hop or step off it, however far ({@link FreezeLock#step}), at
 *   their own height when they fit there, else the held spot's ({@link FreezeLock#returnY}: never inside the ledge they
 *   slid off). Only a move the server makes moves the held spot: every server teleport (vanilla's
 *   {@code ServerGamePacketListenerImpl.teleport}, which every {@code teleportTo}, command, pearl and RuneCraft teleport goes
 *   through, reported by {@code mixin.ServerGamePacketListenerImplMixin}; OSRS allows teleporting while frozen) and a boss's
 *   pull ({@link #moved}). Vanilla's own snap-backs of a bad client move (moved too quickly / wrongly) don't count, so a
 *   client that ignores the freeze can't step out of it. // playability: OSRS can't move a frozen player at all; momentum
 *   from before the freeze may drift {@link FreezeLock#SLACK_BLOCKS} blocks.</li>
 * </ul>
 * Run-energy Slowness II is never a freeze.
 */
public final class FreezeRules {
    /** Slowness at or above this amplifier is an OSRS freeze (speed drops to zero). */
    public static final int FREEZE_AMPLIFIER = 6;
    /** OSRS freeze immunity after a freeze ends: 5 game ticks. */
    public static final int IMMUNITY_MC = FreezeLock.IMMUNITY_TICKS * ActionManager.MC_TICKS_PER_GAME_TICK;

    /** Minecraft tick each player's last freeze ended (ran out or was broken). */
    private static final Map<UUID, Long> ENDED = new ConcurrentHashMap<>();
    /** Where each frozen player is held. */
    private static final Map<UUID, Hold> HOLDS = new ConcurrentHashMap<>();
    /** Held players the server teleported since their last {@link #enforce}: held at the new spot. */
    private static final Set<UUID> TELEPORTED = ConcurrentHashMap.newKeySet();
    /** True while the server thread handles a client's move packet (its snap-backs aren't the server moving the player). */
    private static boolean clientMove;
    private static boolean registered;

    private record Hold(ResourceKey<Level> level, Vec3 anchor) {
    }

    private FreezeRules() {
    }

    public static boolean isFreeze(MobEffectInstance effect) {
        return effect != null && effect.is(MobEffects.MOVEMENT_SLOWDOWN) && effect.getAmplifier() >= FREEZE_AMPLIFIER;
    }

    /** True while the player is frozen in Gielinor. */
    public static boolean frozen(ServerPlayer player) {
        return isFreeze(player.getEffect(MobEffects.MOVEMENT_SLOWDOWN)) && Gielinor.is(player);
    }

    /** True while a freeze can't land on the player (the immunity after the last one ended). */
    public static boolean immune(ServerPlayer player) {
        Long ended = ENDED.get(player.getUUID());
        return FreezeLock.immune(ended == null ? Long.MIN_VALUE : ended, ActionManager.mcTick(), ActionManager.MC_TICKS_PER_GAME_TICK);
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener(MobEffectEvent.Applicable.class, e -> {
            if (e.getEntity() instanceof ServerPlayer p && isFreeze(e.getEffectInstance()) && Gielinor.is(p)
                    && !isFreeze(p.getEffect(MobEffects.MOVEMENT_SLOWDOWN)) && immune(p)) {
                e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            }
        });
        NeoForge.EVENT_BUS.addListener(MobEffectEvent.Expired.class, e -> {
            if (e.getEntity() instanceof ServerPlayer p && isFreeze(e.getEffectInstance())) {
                ended(p);
            }
        });
        // A freeze broken early (removeEffect: cures, released binds, milk) ends here too (OSRS: the immunity follows any
        // freeze's end). Cancelled removals never reach this listener.
        NeoForge.EVENT_BUS.addListener(MobEffectEvent.Remove.class, e -> {
            if (e.getEntity() instanceof ServerPlayer p && isFreeze(e.getEffectInstance())) {
                ended(p);
            }
        });
        NeoForge.EVENT_BUS.addListener(MobEffectEvent.Added.class, e -> {
            if (e.getEntity() instanceof ServerPlayer p && isFreeze(e.getEffectInstance())) {
                ENDED.remove(p.getUUID());
            }
        });
        // a move packet that threw on the server thread mustn't leave every later teleport looking like a snap-back
        ServerTickEvents.START_SERVER_TICK.register(server -> clientMove = false);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                enforce(p);
            }
        });
    }

    private static void ended(ServerPlayer p) {
        ENDED.put(p.getUUID(), ActionManager.mcTick());
        release(p.getUUID());
    }

    private static void release(UUID id) {
        HOLDS.remove(id);
        TELEPORTED.remove(id);
    }

    /**
     * One Minecraft tick of the server's hold on a frozen player (every listed player at the end of each server tick; the
     * self-tests call it for their fake player): puts them back on the held spot when they walked, hopped or stepped off it,
     * and moves the spot with a server teleport. Returns whether they were put back.
     */
    public static boolean enforce(ServerPlayer p) {
        UUID id = p.getUUID();
        if (!frozen(p) || p.isSpectator() || p.isCreative() || p.isPassenger() || !p.isAlive()) {
            release(id);
            return false;
        }
        Vec3 pos = p.position();
        Hold h = HOLDS.get(id);
        boolean same = h != null && h.level() == p.level().dimension();
        FreezeLock.Step step = FreezeLock.step(same, TELEPORTED.remove(id), same ? h.anchor().x : 0, same ? h.anchor().z : 0,
                pos.x, pos.z);
        switch (step) {
            case ANCHOR -> HOLDS.put(id, new Hold(p.level().dimension(), pos));
            case STAY -> {
            }
            case RETURN -> {
                Vec3 anchor = h.anchor();
                AABB over = p.getBoundingBox().move(anchor.x - pos.x, 0, anchor.z - pos.z);
                double y = FreezeLock.returnY(anchor.y, pos.y, p.level().noCollision(p, over));
                p.teleportTo(anchor.x, y, anchor.z);
                TELEPORTED.remove(id); // the server's own put-back, not a teleport that moves the held spot
                p.setDeltaMovement(0, Math.min(0, p.getDeltaMovement().y), 0);
                p.hurtMarked = true;
                return true;
            }
        }
        return false;
    }

    /** The server moved a frozen player on purpose (a boss's pull): the freeze holds them at the new spot. */
    public static void moved(ServerPlayer p) {
        if (frozen(p)) {
            HOLDS.put(p.getUUID(), new Hold(p.level().dimension(), p.position()));
            TELEPORTED.remove(p.getUUID());
        }
    }

    /**
     * {@code mixin.ServerGamePacketListenerImplMixin}: the server is about to teleport {@code p} (any {@code teleportTo},
     * command, pearl or RuneCraft teleport). Unless it is vanilla snapping back a bad client move, a held player's spot moves
     * with them at their next {@link #enforce}.
     */
    public static void teleporting(ServerPlayer p) {
        if (p != null && !clientMove && onServerThread(p) && HOLDS.containsKey(p.getUUID())) {
            TELEPORTED.add(p.getUUID());
        }
    }

    /** {@code mixin.ServerGamePacketListenerImplMixin}: the server thread starts ({@code true}) or ends a client's move packet. */
    public static void clientMove(ServerPlayer p, boolean handling) {
        if (p != null && onServerThread(p)) {
            clientMove = handling;
        }
    }

    private static boolean onServerThread(ServerPlayer p) {
        return p.getServer() != null && p.getServer().isSameThread();
    }

    /** Self-test hook: forgets the player's last freeze and held spot. */
    public static void forget(ServerPlayer player) {
        ENDED.remove(player.getUUID());
        release(player.getUUID());
    }
}
