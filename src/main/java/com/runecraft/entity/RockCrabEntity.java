package com.runecraft.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * OSRS rock crab: lies on the shore as a plain rock ("Rocks") - passive and motionless, drawn as just
 * its boulder shell - until a player comes within a couple of blocks (or hits it); then it wakes,
 * shows its legs and claws and attacks. After a while without a target it settles down and
 * disguises itself again.
 */
public class RockCrabEntity extends OsrsMonsterEntity implements Disguisable {
    private static final EntityDataAccessor<Boolean> DISGUISED = SynchedEntityData.defineId(RockCrabEntity.class, EntityDataSerializers.BOOLEAN);
    /** A player this close wakes the crab (blocks). */
    private static final double WAKE_RANGE = 2.5;
    /** Ticks without a target before the crab disguises itself again (10 s). */
    private static final int REDISGUISE_TICKS = 200;
    private int idleTicks;

    public RockCrabEntity(EntityType<? extends Monster> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DISGUISED, true);
    }

    @Override
    public boolean isDisguised() {
        return this.entityData.get(DISGUISED);
    }

    private void setDisguised(boolean disguised) {
        this.entityData.set(DISGUISED, disguised);
        idleTicks = 0;
        if (disguised) {
            setTarget(null);
            getNavigation().stop();
        }
    }

    @Override
    protected void registerGoals() {
        // The same goals as other monsters, but idle movement waits until the crab is awake, and it
        // picks targets by waking up (proximity or being hit) rather than by aggression range.
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new OsrsAttackGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return !isDisguised() && super.canUse();
            }
        });
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0f) {
            @Override
            public boolean canUse() {
                return !isDisguised() && super.canUse();
            }
        });
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return !isDisguised() && super.canUse();
            }
        });
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (isDisguised()) {
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            Player near = level().getNearestPlayer(this, WAKE_RANGE);
            if (near instanceof ServerPlayer player && !player.isCreative() && !player.isSpectator()) {
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
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide() && isDisguised()) {
            setDisguised(false);
            if (source.getEntity() instanceof ServerPlayer player) {
                setTarget(player);
            }
        }
        return super.hurt(source, amount);
    }
}
