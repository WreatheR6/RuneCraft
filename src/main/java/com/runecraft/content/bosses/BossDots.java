package com.runecraft.content.bosses;

import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.osrs.content.bosses.BossMechanics;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Damage over time that boss attacks leave on players: burns (Yama's fire specials; OSRS Wiki "Burn") and Vardorvis'
 * bleed. Ticked by the boss script that causes them, for the players in its arena (approx: a burn or bleed stops when the
 * boss is gone, where OSRS keeps it running through a teleport).
 */
final class BossDots {
    private static final Map<UUID, BossMechanics.Burn> BURNS = new HashMap<>();
    private static final Map<UUID, BossMechanics.Bleed> BLEEDS = new HashMap<>();
    private static final Map<UUID, Vec3d> LAST_POS = new HashMap<>();

    private BossDots() {
    }

    /** Adds a burn of {@code hits} hits (1 damage per burn every 4 ticks; up to 5 burns stack). */
    static void burn(ServerPlayerEntity p, int hits) {
        BURNS.computeIfAbsent(p.getUuid(), u -> new BossMechanics.Burn()).add(hits);
    }

    /** Starts (or restarts) a Vardorvis bleed. */
    static void bleed(ServerPlayerEntity p) {
        bleed(p, BossMechanics.VARDORVIS_BLEED_DAMAGE);
    }

    /** Starts (or restarts) a bleed of {@code perHit} damage per bleed hit (awakened Vardorvis: 5). */
    static void bleed(ServerPlayerEntity p, int perHit) {
        BLEEDS.computeIfAbsent(p.getUuid(), u -> new BossMechanics.Bleed()).start(perHit);
        LAST_POS.put(p.getUuid(), p.getPos());
    }

    static boolean burning(ServerPlayerEntity p) {
        BossMechanics.Burn b = BURNS.get(p.getUuid());
        return b != null && b.stacks() > 0;
    }

    static boolean bleeding(ServerPlayerEntity p) {
        BossMechanics.Bleed b = BLEEDS.get(p.getUuid());
        return b != null && b.active();
    }

    /** One game tick for these players; {@code source} is credited with the damage. */
    static void tick(LivingEntity source, List<ServerPlayerEntity> players) {
        for (ServerPlayerEntity p : players) {
            UUID id = p.getUuid();
            BossMechanics.Burn burn = BURNS.get(id);
            if (burn != null) {
                int d = burn.tick();
                if (d > 0 && p.isAlive()) {
                    CombatHooks.nameAttack("burn", null);
                    CombatHandler.applyDamage(source, p, d);
                }
                if (burn.stacks() == 0) {
                    BURNS.remove(id);
                }
            }
            BossMechanics.Bleed bleed = BLEEDS.get(id);
            if (bleed != null) {
                Vec3d last = LAST_POS.put(id, p.getPos());
                // Running (sprinting, about a tile or more a tick) makes the bleed hit every tick.
                boolean running = last != null && p.isSprinting() && last.squaredDistanceTo(p.getPos()) > 1.0;
                int d = bleed.tick(running);
                if (d > 0 && p.isAlive()) {
                    CombatHooks.nameAttack("bleed", null);
                    CombatHandler.applyDamage(source, p, d);
                }
                if (!bleed.active()) {
                    BLEEDS.remove(id);
                    LAST_POS.remove(id);
                }
            }
        }
    }
}
