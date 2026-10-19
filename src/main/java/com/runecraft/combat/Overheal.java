package com.runecraft.combat;

import com.runecraft.osrs.CombatEffects;
import net.minecraft.server.level.ServerPlayer;

/**
 * Hitpoints boosted above the maximum (Saradomin brews). Minecraft health can't exceed max health, so the
 * boost is held as absorption: it is lost first when hit and decays by 1 every minute like other boosts.
 */
public final class Overheal {
    private Overheal() {
    }

    /** Total hitpoints including the boost. */
    public static int hitpoints(ServerPlayer player) {
        return (int) Math.ceil(player.getHealth() + player.getAbsorptionAmount());
    }

    /** One Saradomin brew dose: heals 15% + 2, up to that much above the Hitpoints level. */
    public static void brew(ServerPlayer player, int hitpointsLevel) {
        int current = hitpoints(player);
        int after = CombatEffects.afterBrew(current, hitpointsLevel);
        float max = player.getMaxHealth();
        player.setHealth(Math.min(max, after));
        player.setAbsorptionAmount(Math.max(0, after - max));
    }

    /** Called when boosts decay (every minute): boosted hitpoints fall by 1. */
    public static void decay(ServerPlayer player) {
        float boost = player.getAbsorptionAmount();
        if (boost > 0) {
            player.setAbsorptionAmount(Math.max(0, boost - 1));
        }
    }
}
