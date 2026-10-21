package com.runecraft.entity;

import com.runecraft.action.ActionManager;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ThreatTiming;
import java.util.EnumSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;

/**
 * Chases (melee) or keeps range (ranged/magic) and attacks at the OSRS attack speed, stamped when each attack starts (the hit
 * itself is delivered later: a wind-up, a projectile or a telegraph, see {@code Mechanics}). Drives any monster on OSRS AI
 * ({@link OsrsCombatant}: the built-in and module creatures, and since round 42 the vanilla stand-ins, see {@link VanillaOsrsAi}).
 *
 * <p>Real-time adaptations (// playability, round 41): melee reach is the horizontal gap between the two bounding boxes (at most
 * 1.5 blocks, 1.5 in height; OSRS: an adjacent tile) instead of a 3D centre distance that grew with the monster's width, and a
 * melee monster closes in until the gap is 0.8. A newly engaged target, or one that just came into melee reach, is never hit
 * sooner than 10 MC later (OSRS can hit on the tick a monster arrives). A melee-styled special made to reach from range (BLINK,
 * SKY_STRIKE, PULL: {@code ThreatTiming.needsMeleeReach}) starts from range; its creature keeps walking in meanwhile.
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
        standStill(mob); // its target is gone: a spider doesn't walk on to the target's last block
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
        // started only in melee reach (chooseStyle rolls by the same rule and reach); a melee special made for range (BLINK,
        // SKY_STRIKE, PULL) starts from range like a spell, but its creature keeps walking in while it waits
        boolean melee = c.needsMeleeReach(style) || c.meleeRangeOnly();
        boolean closing = melee || style.isMelee();
        double gap = ThreatTiming.gap(target.getX() - mob.getX(), target.getZ() - mob.getZ(), mob.getBbWidth(), target.getBbWidth());
        double ranged = c.rangedReach() > 0 ? c.rangedReach() : 10.0;
        boolean inRange = melee ? c.inMeleeReach(target, distSq) : distSq <= ranged * ranged;
        mob.getLookControl().setLookAt(target, 30.0f, 30.0f);
        boolean canSee = mob.getSensing().hasLineOfSight(target);
        if (!inRange || !canSee) {
            chase(target, melee && canSee, gap);
        } else if (!closing) {
            standStill(mob);
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
     * ({@code closeIn}: it sees its target) whose path reached the target's block but left it short ({@link #arrivedShort}); stays
     * put while it holds at the edge of its max range (its target attacking it from beyond, {@link OsrsTargetGoal#holding}).
     */
    private void chase(ServerPlayer target, boolean closeIn, double gap) {
        if (targets == null) {
            targets = OsrsTargetGoal.of(mob);
        }
        if (targets != null && targets.holding()) {
            standStill(mob);
            return;
        }
        if (c.directPathing()) {
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
     * Stops walking. {@code PathNavigation.stop()} alone doesn't stop a spider (the giant spider and spider stand-ins): its
     * {@link WallClimberNavigation} remembers the block it last pathed to and, once the path is gone, steers straight at it every
     * tick, so a giant spider held at the edge of its max range walked on at the player shooting it until the 20-block leash sent
     * it home. Aimed at the block it stands on, it lets go on its next tick (a path to its own block: cheap). Called every tick of
     * the hold and of a ranged stand-off, after the target goal and before the navigation ticks, and when the goal stops (its
     * target gone, dropped or given up: it doesn't walk on to the target's last block).
     */
    static void standStill(Mob mob) {
        mob.getNavigation().stop();
        if (mob.getNavigation() instanceof WallClimberNavigation climber) {
            climber.createPath(mob.blockPosition(), 0);
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
            c.performAttack(target, style);
        }
    }
}
