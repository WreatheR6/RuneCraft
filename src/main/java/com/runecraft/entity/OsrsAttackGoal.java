package com.runecraft.entity;

import com.runecraft.action.ActionManager;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ThreatTiming;
import java.util.EnumSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Chases (melee) or keeps range (ranged/magic) and attacks at the OSRS attack speed, stamped when each attack starts (the hit
 * itself is delivered later: a wind-up, a projectile or a telegraph, see {@code Mechanics}). Drives any monster on OSRS AI
 * ({@link OsrsCombatant}: the built-in and module creatures, and since round 42 the vanilla stand-ins, see {@link VanillaOsrsAi}).
 *
 * <p>Real-time adaptations (// playability, round 41): melee reach is the horizontal gap between the two bounding boxes (at most
 * 1.5 blocks, 1.5 in height; OSRS: an adjacent tile) instead of a 3D centre distance that grew with the monster's width, and a
 * melee monster closes in until the gap is 0.8. A newly engaged target, or one that just came into melee reach, is never hit
 * sooner than 10 MC later (OSRS can hit on the tick a monster arrives).
 */
public class OsrsAttackGoal extends Goal {
    private final OsrsCombatant c;
    private final Mob mob;
    private long lastAttack = -1000;
    /** The target the cadence was last set up for. */
    private LivingEntity engaged;
    private boolean wasInReach;
    private long arrivedAt = Long.MIN_VALUE / 4;
    /** The monster's target goal (looked up once it exists: it is registered after this goal). */
    private OsrsTargetGoal targets;

    public OsrsAttackGoal(OsrsMonsterEntity mob) {
        this((OsrsCombatant) mob);
    }

    public OsrsAttackGoal(OsrsCombatant c) {
        this.c = c;
        this.mob = c.self();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target instanceof ServerPlayer && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mob.getTarget();
        return target instanceof ServerPlayer p && p.isAlive() && !p.isCreative() && !p.isSpectator()
                && mob.distanceToSqr(p) < 48 * 48;
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (!(mob.getTarget() instanceof ServerPlayer target)) {
            return;
        }
        long now = ActionManager.mcTick();
        int speedTicks = c.stats().attackSpeed();
        int windup = ThreatTiming.meleeWindupMc(mob.getBbWidth());
        if (target != engaged) {
            engaged = target;
            lastAttack = ThreatTiming.engage(lastAttack, now, speedTicks, windup);
            wasInReach = false;
        }
        double distSq = mob.distanceToSqr(target);
        AttackType style = c.chooseStyle(target, distSq);
        boolean melee = style.isMelee() || c.meleeRangeOnly();
        double gap = ThreatTiming.gap(target.getX() - mob.getX(), target.getZ() - mob.getZ(), mob.getBbWidth(), target.getBbWidth());
        double ranged = c.rangedReach() > 0 ? c.rangedReach() : 10.0;
        boolean inRange = melee ? ThreatTiming.inMeleeReach(gap, target.getY() - mob.getY()) : distSq <= ranged * ranged;
        mob.getLookControl().setLookAt(target, 30.0f, 30.0f);
        boolean canSee = mob.getSensing().hasLineOfSight(target);
        if (!inRange || !canSee) {
            chase(target);
        } else if (!melee) {
            mob.getNavigation().stop();
        } else if (ThreatTiming.keepChasing(gap)) {
            chase(target);
        }
        boolean inReach = melee && inRange;
        if (inReach && !wasInReach) {
            arrivedAt = now;
        }
        wasInReach = inReach;
        long earliest = melee ? ThreatTiming.arrivalReadyMc(arrivedAt, windup) : Long.MIN_VALUE;
        tryAttack(target, style, inRange && canSee, now, speedTicks, earliest);
    }

    /**
     * Path-finds to the target, or walks straight at it for a creature with OSRS direct ("dumb") pathing; stays put while it holds
     * at the edge of its max range (its target attacking it from beyond, {@link OsrsTargetGoal#holding}).
     */
    private void chase(ServerPlayer target) {
        if (targets == null) {
            targets = OsrsTargetGoal.of(mob);
        }
        if (targets != null && targets.holding()) {
            mob.getNavigation().stop();
            return;
        }
        if (c.directPathing()) {
            mob.getNavigation().stop();
            mob.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
        } else {
            mob.getNavigation().moveTo(target, 1.0);
        }
    }

    private void tryAttack(ServerPlayer target, AttackType style, boolean ready, long now, int speedTicks, long earliest) {
        if (ready && ThreatTiming.cadenceReady(now, lastAttack, speedTicks) && now >= earliest
                && com.runecraft.combat.CombatZones.npcMayAttack(mob, target)) {
            lastAttack = now; // cadence stamped at launch: a later landing never adds or removes an attack
            c.performAttack(target, style);
        }
    }
}
