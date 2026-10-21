package com.runecraft.content.quests;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.FloorMarkers;
import com.runecraft.boss.HazardKind;
import com.runecraft.boss.Mechanics;
import com.runecraft.boss.StrikeMarkers;
import com.runecraft.combat.CombatHandler;
import com.runecraft.osrs.ArenaTiming;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Ground strikes a quest boss script lands itself, typeless: protection prayers don't touch them, only stepping out of the
 * marked tiles does (Vanstrom Klause's lightning and darkness, the tormented demons' fire bombs). Each is drawn as it is made
 * (a ring of particles and coloured floor blocks under its disc, {@link FloorMarkers}), named on the cast bar of the players
 * near it ({@code HudNet.groundCast}) and redrawn until it lands. // playability: OSRS marks the tiles a tick or two ahead; here
 * at least {@link ArenaTiming#MIN_LEAD_MC} Minecraft ticks
 */
final class QuestStrikes {
    private static final class Strike {
        final ServerLevel world;
        final Vec3 pos;
        final double radius;
        final int minHit;
        final int maxHit;
        final long dueMc;
        final ParticleOptions particle;
        final FloorMarkers.Layer floor;
        final String name;
        long lastDrawMc;

        Strike(ServerLevel world, Vec3 pos, double radius, int minHit, int maxHit, long dueMc, ParticleOptions particle,
               FloorMarkers.Layer floor, String name) {
            this.world = world;
            this.pos = pos;
            this.radius = radius;
            this.minHit = minHit;
            this.maxHit = maxHit;
            this.dueMc = dueMc;
            this.particle = particle;
            this.floor = floor;
            this.name = name;
        }
    }

    private final List<Strike> pending = new ArrayList<>();

    /**
     * Marks a strike on {@code pos} landing {@code delayTicks} game ticks from now (never sooner than the real-time floor): every
     * player horizontally within {@code radius} then takes {@code minHit}-{@code maxHit}, typeless.
     */
    void add(DataBossEntity boss, Vec3 pos, double radius, int minHit, int maxHit, int delayTicks, ParticleOptions particle,
             HazardKind kind, String name) {
        if (!(boss.level() instanceof ServerLevel world)) {
            return;
        }
        int lead = Math.max(ArenaTiming.MIN_LEAD_MC, delayTicks * ArenaTiming.MC_PER_GAME_TICK);
        long now = ActionManager.mcTick();
        FloorMarkers.Layer floor = kind == null ? null : FloorMarkers.show(new FloorMarkers.Shape(world).disc(pos, radius, kind), boss);
        com.runecraft.network.HudNet.groundCast(world, pos, radius, boss, null, lead, name);
        StrikeMarkers.ring(world, pos, radius, particle, false, ArenaTiming.ringPoints(radius, pending.size()));
        Strike s = new Strike(world, pos, radius, minHit, maxHit, now + lead, particle, floor, name);
        s.lastDrawMc = now;
        pending.add(s);
    }

    /** Every Minecraft tick: redraws the pending strikes and lands those that are due. */
    void tick(DataBossEntity boss) {
        if (pending.isEmpty()) {
            return;
        }
        long now = ActionManager.mcTick();
        List<Strike> landed = new ArrayList<>();
        Iterator<Strike> it = pending.iterator();
        while (it.hasNext()) {
            Strike s = it.next();
            if (now >= s.dueMc) {
                landed.add(s);
                it.remove();
            } else if (ArenaTiming.redrawDue(s.lastDrawMc, now)) {
                s.lastDrawMc = now;
                StrikeMarkers.ring(s.world, s.pos, s.radius, s.particle, ArenaTiming.finalCue(s.dueMc, now),
                        ArenaTiming.ringPoints(s.radius, pending.size()));
            }
        }
        for (Strike s : landed) {
            land(boss, s);
        }
    }

    private void land(DataBossEntity boss, Strike s) {
        if (s.floor != null) {
            s.floor.clear();
        }
        s.world.sendParticles(ParticleTypes.EXPLOSION, s.pos.x, s.pos.y + 0.3, s.pos.z, 1, 0.2, 0.2, 0.2, 0);
        s.world.sendParticles(s.particle, s.pos.x, s.pos.y + 0.5, s.pos.z, 12, s.radius / 2, 0.4, s.radius / 2, 0.02);
        s.world.playSound(null, s.pos.x, s.pos.y, s.pos.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 0.5f, 1.4f);
        for (ServerPlayer p : players(s.world)) {
            if (ArenaTiming.inStrike(p.getX() - s.pos.x, p.getY() - s.pos.y, p.getZ() - s.pos.z, s.radius)) {
                if (s.name != null) {
                    com.runecraft.combat.CombatHooks.nameAttack(s.name, null);
                }
                int dmg = s.minHit + (s.maxHit > s.minHit ? p.getRandom().nextInt(s.maxHit - s.minHit + 1) : 0);
                CombatHandler.applyDamage(boss.isAlive() ? boss : null, p, dmg);
            }
        }
    }

    /** Players a strike can reach: the level's own and the self-test's ({@link Mechanics#extraPlayers}). */
    static List<ServerPlayer> players(ServerLevel world) {
        List<ServerPlayer> out = new ArrayList<>(world.getPlayers(pl -> pl.isAlive() && !pl.isSpectator()));
        for (ServerPlayer p : Mechanics.extraPlayers.apply(world)) {
            if (p.isAlive() && p.level() == world && !out.contains(p)) {
                out.add(p);
            }
        }
        return out;
    }

    /** Strikes still to land. */
    int size() {
        return pending.size();
    }

    /** Takes every pending strike back without landing it (the boss died or left). */
    void clear() {
        for (Strike s : pending) {
            if (s.floor != null) {
                s.floor.clear();
            }
        }
        pending.clear();
    }
}
