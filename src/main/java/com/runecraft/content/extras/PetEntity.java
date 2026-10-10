package com.runecraft.content.extras;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * A pet following its owner (or roaming a house menagerie). Never saved with the world: the
 * follower is kept in {@link ExtrasState} and respawned by {@link PetService}, so the pet item can
 * never be lost. Right-click it to pick it up.
 */
public class PetEntity extends PathAwareEntity {
    private static final TrackedData<String> PET = DataTracker.registerData(PetEntity.class, TrackedDataHandlerRegistry.STRING);

    private UUID owner;
    /** Menagerie pets wander instead of following. */
    private boolean roaming;
    private int pathCooldown;

    public PetEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(PET, "");
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6.0f));
        this.goalSelector.add(7, new LookAroundGoal(this));
    }

    public String petId() {
        return this.dataTracker.get(PET);
    }

    public void setPetId(String id) {
        this.dataTracker.set(PET, id);
    }

    public UUID owner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public boolean roaming() {
        return roaming;
    }

    public void setRoaming(boolean roaming) {
        this.roaming = roaming;
        if (roaming) {
            this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient() || roaming) {
            return;
        }
        if (owner == null || getServer() == null) {
            discard();
            return;
        }
        ServerPlayerEntity player = getServer().getPlayerManager().getPlayer(owner);
        if (player == null || player.getWorld() != getWorld() || player.isSpectator()) {
            discard();
            return;
        }
        double d = squaredDistanceTo(player);
        if (d > 14 * 14) {
            refreshPositionAndAngles(player.getX() - Math.sin(Math.toRadians(-player.getYaw())) * 1.5, player.getY(),
                    player.getZ() - Math.cos(Math.toRadians(-player.getYaw())) * 1.5, player.getYaw(), 0);
            getNavigation().stop();
        } else if (d > 3.5 * 3.5) {
            if (--pathCooldown <= 0) {
                pathCooldown = 10;
                getNavigation().startMovingTo(player, d > 64 ? 1.6 : 1.2);
            }
        } else {
            getNavigation().stop();
        }
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }
        if (!getWorld().isClient() && player instanceof ServerPlayerEntity sp) {
            PetService.interact(sp, this);
        }
        return ActionResult.success(getWorld().isClient());
    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        return true;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public boolean canBeLeashedBy(PlayerEntity player) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
