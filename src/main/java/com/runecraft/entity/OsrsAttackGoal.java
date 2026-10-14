package com.runecraft.entity;

import com.runecraft.action.ActionManager;
import com.runecraft.osrs.AttackType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.EnumSet;

/** Chases (melee) or keeps range (ranged/magic) and attacks at the OSRS attack speed. */
public class OsrsAttackGoal extends Goal {
    private final OsrsMonsterEntity mob;
    private long lastAttack = -1000;

    public OsrsAttackGoal(OsrsMonsterEntity mob) {
        this.mob = mob;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        LivingEntity target = mob.getTarget();
        return target instanceof ServerPlayerEntity && target.isAlive();
    }

    @Override
    public boolean shouldContinue() {
        LivingEntity target = mob.getTarget();
        return target instanceof ServerPlayerEntity p && p.isAlive() && !p.isCreative() && !p.isSpectator()
                && mob.squaredDistanceTo(p) < 48 * 48;
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (!(mob.getTarget() instanceof ServerPlayerEntity target)) {
            return;
        }
        double distSq = mob.squaredDistanceTo(target);
        AttackType style = mob.chooseStyle(target, distSq);
        boolean melee = style.isMelee();
        double reach = mob.getWidth() * 1.4 + target.getWidth() + 0.6;
        double range = melee ? reach * reach : 10.0 * 10.0;
        mob.getLookControl().lookAt(target, 30.0f, 30.0f);
        boolean canSee = mob.getVisibilityCache().canSee(target);
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
    private void chase(ServerPlayerEntity target) {
        if (mob.directPathing) {
            mob.getNavigation().stop();
            mob.getMoveControl().moveTo(target.getX(), target.getY(), target.getZ(), 1.0);
        } else {
            mob.getNavigation().startMovingTo(target, 1.0);
        }
    }

    private void tryAttack(ServerPlayerEntity target, AttackType style, double distSq, double range, boolean canSee, long now) {
        long speed = (long) mob.stats().attackSpeed() * ActionManager.MC_TICKS_PER_GAME_TICK;
        if (distSq <= range && canSee && now - lastAttack >= speed
                && com.runecraft.combat.CombatZones.npcMayAttack(mob, target)) {
            lastAttack = now;
            mob.performAttack(target, style);
        }
    }
}
