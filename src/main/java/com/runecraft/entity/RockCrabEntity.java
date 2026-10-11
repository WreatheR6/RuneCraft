package com.runecraft.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

/**
 * OSRS rock crab: lies on the shore as a plain rock ("Rocks") - passive and motionless, drawn as just
 * its boulder shell - until a player comes within a couple of blocks (or hits it); then it wakes,
 * shows its legs and claws and attacks. After a while without a target it settles down and
 * disguises itself again.
 */
public class RockCrabEntity extends OsrsMonsterEntity implements Disguisable {
    private static final TrackedData<Boolean> DISGUISED = DataTracker.registerData(RockCrabEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    /** A player this close wakes the crab (blocks). */
    private static final double WAKE_RANGE = 2.5;
    /** Ticks without a target before the crab disguises itself again (10 s). */
    private static final int REDISGUISE_TICKS = 200;
    private int idleTicks;

    public RockCrabEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(DISGUISED, true);
    }

    @Override
    public boolean isDisguised() {
        return this.dataTracker.get(DISGUISED);
    }

    private void setDisguised(boolean disguised) {
        this.dataTracker.set(DISGUISED, disguised);
        idleTicks = 0;
        if (disguised) {
            setTarget(null);
            getNavigation().stop();
        }
    }

    @Override
    protected void initGoals() {
        // The same goals as other monsters, but idle movement waits until the crab is awake, and it
        // picks targets by waking up (proximity or being hit) rather than by aggression range.
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new OsrsAttackGoal(this));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.8) {
            @Override
            public boolean canStart() {
                return !isDisguised() && super.canStart();
            }
        });
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f) {
            @Override
            public boolean canStart() {
                return !isDisguised() && super.canStart();
            }
        });
        this.goalSelector.add(8, new LookAroundGoal(this) {
            @Override
            public boolean canStart() {
                return !isDisguised() && super.canStart();
            }
        });
        this.targetSelector.add(1, new RevengeGoal(this));
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (isDisguised()) {
            getNavigation().stop();
            setVelocity(0, getVelocity().y, 0);
            PlayerEntity near = getWorld().getClosestPlayer(this, WAKE_RANGE);
            if (near instanceof ServerPlayerEntity player && !player.isCreative() && !player.isSpectator()) {
                setDisguised(false);
                setTarget(player);
            }
            return;
        }
        if (getTarget() != null && getTarget().isAlive()) {
            idleTicks = 0;
        } else if (++idleTicks >= REDISGUISE_TICKS) {
            setDisguised(true);
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (!getWorld().isClient() && isDisguised()) {
            setDisguised(false);
            if (source.getAttacker() instanceof ServerPlayerEntity player) {
                setTarget(player);
            }
        }
        return super.damage(source, amount);
    }
}
