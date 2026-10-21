package com.runecraft.combat;

import com.runecraft.action.ActionManager;
import com.runecraft.world.Gielinor;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * OSRS freezes on the server. A freeze is Slowness with amplifier {@value #FREEZE_AMPLIFIER} or more in Gielinor (ice
 * spells, Mechanics' FREEZE special, boss binds and stuns); no call site changes. After a freeze wears off the player is
 * immune to the next one for {@value #IMMUNITY_MC} Minecraft ticks (OSRS: 5 game ticks of freeze immunity). The client
 * side (no FOV zoom, no hopping out, the HUD icon) is {@code client.FreezeClient}.
 *
 * <p>Only a freeze that runs out starts the immunity: a bind broken early (husks killed, Vorkath's spawn killed, a cure)
 * doesn't, and a freeze refreshed while it is still on is left to vanilla's merge, so the scripts that renew a bind every
 * tick keep working. Run-energy Slowness II is never a freeze.
 */
public final class FreezeRules {
    /** Slowness at or above this amplifier is an OSRS freeze (speed drops to zero). */
    public static final int FREEZE_AMPLIFIER = 6;
    /** OSRS freeze immunity after a freeze ends: 5 game ticks. */
    public static final int IMMUNITY_MC = 5 * ActionManager.MC_TICKS_PER_GAME_TICK;

    /** Minecraft tick each player's last freeze ran out. */
    private static final Map<UUID, Long> ENDED = new ConcurrentHashMap<>();
    private static boolean registered;

    private FreezeRules() {
    }

    public static boolean isFreeze(MobEffectInstance effect) {
        return effect != null && effect.is(MobEffects.MOVEMENT_SLOWDOWN) && effect.getAmplifier() >= FREEZE_AMPLIFIER;
    }

    /** True while a freeze can't land on the player (the immunity after the last one ran out). */
    public static boolean immune(ServerPlayer player) {
        Long ended = ENDED.get(player.getUUID());
        long now = ActionManager.mcTick();
        return ended != null && now >= ended && now - ended < IMMUNITY_MC;
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
                ENDED.put(p.getUUID(), ActionManager.mcTick());
            }
        });
        NeoForge.EVENT_BUS.addListener(MobEffectEvent.Added.class, e -> {
            if (e.getEntity() instanceof ServerPlayer p && isFreeze(e.getEffectInstance())) {
                ENDED.remove(p.getUUID());
            }
        });
    }

    /** Self-test hook: forgets the player's last freeze. */
    public static void forget(ServerPlayer player) {
        ENDED.remove(player.getUUID());
    }
}
