package com.runecraft.content.extras;

import java.util.UUID;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A pet following its owner (or roaming a house menagerie). Never saved with the world: the
 * follower is kept in {@link ExtrasState} and respawned by {@link PetService}, so the pet item can
 * never be lost. Right-click it to pick it up.
 */
public class PetEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> PET = SynchedEntityData.defineId(PetEntity.class, EntityDataSerializers.STRING);

    private UUID owner;
    /** Menagerie pets wander instead of following. */
    private boolean roaming;
    private int pathCooldown;

    public PetEntity(EntityType<? extends PathfinderMob> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PET, "");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public String petId() {
        return this.entityData.get(PET);
    }

    public void setPetId(String id) {
        this.entityData.set(PET, id);
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
            this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || roaming) {
            return;
        }
        if (owner == null || getServer() == null) {
            discard();
            return;
        }
        ServerPlayer player = getServer().getPlayerList().getPlayer(owner);
        if (player == null || player.level() != level() || player.isSpectator()) {
            discard();
            return;
        }
        double d = distanceToSqr(player);
        if (d > 14 * 14) {
            moveTo(player.getX() - Math.sin(Math.toRadians(-player.getYRot())) * 1.5, player.getY(),
                    player.getZ() - Math.cos(Math.toRadians(-player.getYRot())) * 1.5, player.getYRot(), 0);
            getNavigation().stop();
        } else if (d > 3.5 * 3.5) {
            if (--pathCooldown <= 0) {
                pathCooldown = 10;
                getNavigation().moveTo(player, d > 64 ? 1.6 : 1.2);
            }
        } else {
            getNavigation().stop();
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide() && player instanceof ServerPlayer sp) {
            PetService.interact(sp, this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
