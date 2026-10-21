package com.runecraft.combat;

import com.runecraft.action.ActionManager;
import com.runecraft.osrs.FreezeLock;
import com.runecraft.platform.ServerTickEvents;
import com.runecraft.world.Gielinor;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
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
 *   the freeze caught them and puts them back when they walk or hop off it ({@link FreezeLock#step}); a teleport while frozen
 *   (allowed in OSRS) moves the held spot with them. // playability: OSRS can't move a frozen player at all; momentum from
 *   before the freeze may drift {@link FreezeLock#SLACK_BLOCKS} blocks.</li>
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
    /** Where each frozen player is held, and where they stood last tick. */
    private static final Map<UUID, Hold> HOLDS = new ConcurrentHashMap<>();
    private static boolean registered;

    private record Hold(ResourceKey<Level> level, Vec3 anchor, Vec3 last) {
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
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                enforce(p);
            }
        });
    }

    private static void ended(ServerPlayer p) {
        ENDED.put(p.getUUID(), ActionManager.mcTick());
        HOLDS.remove(p.getUUID());
    }

    /**
     * One Minecraft tick of the server's hold on a frozen player (every listed player at the end of each server tick; the
     * self-tests call it for their fake player): puts them back on the held spot when they walked or hopped off it. Returns
     * whether they were put back.
     */
    public static boolean enforce(ServerPlayer p) {
        UUID id = p.getUUID();
        if (!frozen(p) || p.isSpectator() || p.isCreative() || p.isPassenger() || !p.isAlive()) {
            HOLDS.remove(id);
            return false;
        }
        Vec3 pos = p.position();
        Hold h = HOLDS.get(id);
        boolean same = h != null && h.level() == p.level().dimension();
        FreezeLock.Step step = FreezeLock.step(same, same ? h.anchor().x : 0, same ? h.anchor().z : 0, same ? h.last().x : 0,
                same ? h.last().z : 0, pos.x, pos.z);
        switch (step) {
            case ANCHOR -> HOLDS.put(id, new Hold(p.level().dimension(), pos, pos));
            case STAY -> HOLDS.put(id, new Hold(h.level(), h.anchor(), pos));
            case RETURN -> {
                Vec3 back = new Vec3(h.anchor().x, pos.y, h.anchor().z);
                p.teleportTo(back.x, back.y, back.z);
                p.setDeltaMovement(0, Math.min(0, p.getDeltaMovement().y), 0);
                p.hurtMarked = true;
                HOLDS.put(id, new Hold(h.level(), h.anchor(), back));
                return true;
            }
        }
        return false;
    }

    /** The server moved a frozen player on purpose (a boss's pull): the freeze holds them at the new spot. */
    public static void moved(ServerPlayer p) {
        if (frozen(p)) {
            HOLDS.put(p.getUUID(), new Hold(p.level().dimension(), p.position(), p.position()));
        }
    }

    /** Self-test hook: forgets the player's last freeze and held spot. */
    public static void forget(ServerPlayer player) {
        ENDED.remove(player.getUUID());
        HOLDS.remove(player.getUUID());
    }
}
