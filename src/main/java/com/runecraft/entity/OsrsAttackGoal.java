package com.runecraft.entity;

import com.runecraft.action.ActionManager;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ThreatTiming;
import java.util.EnumSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Chases (melee) or keeps range (ranged/magic) and attacks at the OSRS attack speed, stamped when each attack starts (the hit
 * itself is delivered later: a wind-up, a projectile or a telegraph, see {@code Mechanics}).
 *
 * <p>Real-time adaptations (// playability, round 41): melee reach is the horizontal gap between the two bounding boxes (at most
 * 1.5 blocks, 1.5 in height; OSRS: an adjacent tile) instead of a 3D centre distance that grew with the monster's width, and a
 * melee monster closes in until the gap is 0.8. A newly engaged target, or one that just came into melee reach, is never hit
 * sooner than 10 MC later (OSRS can hit on the tick a monster arrives).
 */
public class OsrsAttackGoal extends Goal {
    private final OsrsMonsterEntity mob;
    private long lastAttack = -1000;
    /** The target the cadence was last set up for. */
    private LivingEntity engaged;
    private boolean wasInReach;
    private long arrivedAt = Long.MIN_VALUE / 4;

    public OsrsAttackGoal(OsrsMonsterEntity mob) {
        this.mob = mob;
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
        int speedTicks = mob.stats().attackSpeed();
        int windup = ThreatTiming.meleeWindupMc(mob.getBbWidth());
        if (target != engaged) {
            engaged = target;
            lastAttack = ThreatTiming.engage(lastAttack, now, speedTicks, windup);
            wasInReach = false;
        }
        double distSq = mob.distanceToSqr(target);
        AttackType style = mob.chooseStyle(target, distSq);
        boolean melee = style.isMelee() || mob.meleeRangeOnly;
        double gap = ThreatTiming.gap(target.getX() - mob.getX(), target.getZ() - mob.getZ(), mob.getBbWidth(), target.getBbWidth());
        double ranged = mob.rangedReach > 0 ? mob.rangedReach : 10.0;
        boolean inRange = melee ? mob.inMeleeReach(target) : distSq <= ranged * ranged; // the reach chooseStyle picks melee by
        mob.getLookControl().setLookAt(target, 30.0f, 30.0f);
        boolean canSee = mob.getSensing().hasLineOfSight(target);
        if (!inRange || !canSee) {
            chase(target, melee && canSee, gap);
        } else if (!melee) {
            mob.getNavigation().stop();
        } else if (ThreatTiming.keepChasing(gap)) {
            chase(target, true, gap);
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
     * Path-finds to the target, or walks straight at it for a creature with OSRS direct ("dumb") pathing, or a melee one
     * ({@code closeIn}: it sees its target) whose path reached the target's block but left it short ({@link #arrivedShort}).
     */
    private void chase(ServerPlayer target, boolean closeIn, double gap) {
        if (mob.directPathing) {
            mob.getNavigation().stop();
            mob.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
        } else if (closeIn && arrivedShort(target, gap)) {
            // the done path stays, so the navigation leaves this straight step alone until the target moves to another block
            mob.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
        } else {
            mob.getNavigation().moveTo(target, 1.0);
        }
    }

    /**
     * Whether the navigation reached {@code target}'s block (its path is done and got there) with the creature still short of
     * it ({@link ThreatTiming#closeInDirectly}: a wide creature's path ends up to 2 blocks out of melee reach, and a new path to
     * the same block ends there again, so Skotizo summoned east of a player stood 1.6 blocks away for good).
     */
    private boolean arrivedShort(ServerPlayer target, double gap) {
        net.minecraft.world.level.pathfinder.Path path = mob.getNavigation().getPath();
        return path != null && path.isDone() && path.canReach() && target.blockPosition().equals(path.getTarget())
                && ThreatTiming.closeInDirectly(gap, target.getY() - mob.getY());
    }

    private void tryAttack(ServerPlayer target, AttackType style, boolean ready, long now, int speedTicks, long earliest) {
        if (ready && ThreatTiming.cadenceReady(now, lastAttack, speedTicks) && now >= earliest
                && com.runecraft.combat.CombatZones.npcMayAttack(mob, target)) {
            lastAttack = now; // cadence stamped at launch: a later landing never adds or removes an attack
            mob.performAttack(target, style);
        }
    }
}
