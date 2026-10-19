package com.runecraft.content.extras;

import com.runecraft.osrs.content.extras.Seafaring;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A Sailing boat: a rideable vehicle on water (vanilla boat physics and controls) whose top speed
 * depends on its tier. It is still a {@link Boat}, so trawling, salvaging and port tasks from
 * the skilling module work from it. It can't be broken; its owner sneak + right-clicks to dock it
 * (the deed goes back to the inventory).
 */
public class SailBoatEntity extends Boat {
    private final Seafaring.BoatTier tier;
    private UUID owner;

    public SailBoatEntity(EntityType<? extends Boat> type, Level world, Seafaring.BoatTier tier) {
        super(type, world);
        this.tier = tier;
        setVariant(Boat.Type.valueOf(tier.variant()));
    }

    public Seafaring.BoatTier tier() {
        return tier;
    }

    public UUID owner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    @Override
    public void tick() {
        super.tick();
        // Movement is simulated by whoever controls the boat (the rider's client); scaling the velocity
        // each tick raises the boat's terminal speed by tier (see Seafaring.terminalSpeed).
        if (isControlledByLocalInstance() && isVehicle() && tier.speed() != 1.0) {
            Vec3 v = getDeltaMovement();
            setDeltaMovement(v.x * tier.speed(), v.y, v.z * tier.speed());
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (!level().isClientSide() && player instanceof ServerPlayer sp) {
                SailingService.dock(sp, this);
            }
            return InteractionResult.sidedSuccess(level().isClientSide());
        }
        if (!level().isClientSide() && player instanceof ServerPlayer sp && !SailingService.canBoard(sp, this)) {
            return InteractionResult.SUCCESS;
        }
        return super.interact(player, hand);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public Item getDropItem() {
        return Items.AIR;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if (owner != null) {
            nbt.putUUID("RunecraftOwner", owner);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.hasUUID("RunecraftOwner")) {
            owner = nbt.getUUID("RunecraftOwner");
        }
    }
}
