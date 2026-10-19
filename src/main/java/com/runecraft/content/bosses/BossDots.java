package com.runecraft.content.bosses;

import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.osrs.content.bosses.BossMechanics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Damage over time that boss attacks leave on players: burns (Yama's fire specials; OSRS Wiki "Burn") and Vardorvis'
 * bleed. Ticked by the boss script that causes them, for the players in its arena (approx: a burn or bleed stops when the
 * boss is gone, where OSRS keeps it running through a teleport).
 */
final class BossDots {
    private static final Map<UUID, BossMechanics.Burn> BURNS = new HashMap<>();
    private static final Map<UUID, BossMechanics.Bleed> BLEEDS = new HashMap<>();
    private static final Map<UUID, Vec3> LAST_POS = new HashMap<>();

    private BossDots() {
    }

    /** Adds a burn of {@code hits} hits (1 damage per burn every 4 ticks; up to 5 burns stack). */
    static void burn(ServerPlayer p, int hits) {
        BURNS.computeIfAbsent(p.getUUID(), u -> new BossMechanics.Burn()).add(hits);
    }

    /** Starts (or restarts) a Vardorvis bleed. */
    static void bleed(ServerPlayer p) {
        bleed(p, BossMechanics.VARDORVIS_BLEED_DAMAGE);
    }

    /** Starts (or restarts) a bleed of {@code perHit} damage per bleed hit (awakened Vardorvis: 5). */
    static void bleed(ServerPlayer p, int perHit) {
        BLEEDS.computeIfAbsent(p.getUUID(), u -> new BossMechanics.Bleed()).start(perHit);
        LAST_POS.put(p.getUUID(), p.position());
    }

    static boolean burning(ServerPlayer p) {
        BossMechanics.Burn b = BURNS.get(p.getUUID());
        return b != null && b.stacks() > 0;
    }

    static boolean bleeding(ServerPlayer p) {
        BossMechanics.Bleed b = BLEEDS.get(p.getUUID());
        return b != null && b.active();
    }

    /** One game tick for these players; {@code source} is credited with the damage. */
    static void tick(LivingEntity source, List<ServerPlayer> players) {
        for (ServerPlayer p : players) {
            UUID id = p.getUUID();
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
                Vec3 last = LAST_POS.put(id, p.position());
                // Running (sprinting, about a tile or more a tick) makes the bleed hit every tick.
                boolean running = last != null && p.isSprinting() && last.distanceToSqr(p.position()) > 1.0;
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
