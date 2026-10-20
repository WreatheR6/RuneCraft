package com.runecraft.entity;

import com.runecraft.action.ActionManager;
import com.runecraft.osrs.AttackType;
import java.util.EnumSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/** Chases (melee) or keeps range (ranged/magic) and attacks at the OSRS attack speed. */
public class OsrsAttackGoal extends Goal {
    private final OsrsMonsterEntity mob;
    private long lastAttack = -1000;

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
        double distSq = mob.distanceToSqr(target);
        AttackType style = mob.chooseStyle(target, distSq);
        boolean melee = style.isMelee() || mob.meleeRangeOnly;
        double reach = mob.getBbWidth() * 1.4 + target.getBbWidth() + 0.6;
        double ranged = mob.rangedReach > 0 ? mob.rangedReach : 10.0;
        double range = melee ? reach * reach : ranged * ranged;
        mob.getLookControl().setLookAt(target, 30.0f, 30.0f);
        boolean canSee = mob.getSensing().hasLineOfSight(target);
        if (distSq > range || !canSee) {
            chase(target);
        } else if (!melee) {
            mob.getNavigation().stop();
        } else if (distSq > reach * reach * 0.6) {
            chase(target);
        }
        long now = ActionManager.mcTick();
        tryAttack(target, style, distSq, range, canSee, now);
    }

    /** Path-finds to the target, or walks straight at it for a creature with OSRS direct ("dumb") pathing. */
    private void chase(ServerPlayer target) {
        if (mob.directPathing) {
            mob.getNavigation().stop();
            mob.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
        } else {
            mob.getNavigation().moveTo(target, 1.0);
        }
    }

    private void tryAttack(ServerPlayer target, AttackType style, double distSq, double range, boolean canSee, long now) {
        long speed = (long) mob.stats().attackSpeed() * ActionManager.MC_TICKS_PER_GAME_TICK;
        if (distSq <= range && canSee && now - lastAttack >= speed
                && com.runecraft.combat.CombatZones.npcMayAttack(mob, target)) {
            lastAttack = now;
            mob.performAttack(target, style);
        }
    }
}
