package com.runecraft.content.extras;

import com.runecraft.osrs.content.extras.Seafaring;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * A Sailing boat: a rideable vehicle on water (vanilla boat physics and controls) whose top speed
 * depends on its tier. It is still a {@link BoatEntity}, so trawling, salvaging and port tasks from
 * the skilling module work from it. It can't be broken; its owner sneak + right-clicks to dock it
 * (the deed goes back to the inventory).
 */
public class SailBoatEntity extends BoatEntity {
    private final Seafaring.BoatTier tier;
    private UUID owner;

    public SailBoatEntity(EntityType<? extends BoatEntity> type, World world, Seafaring.BoatTier tier) {
        super(type, world);
        this.tier = tier;
        setVariant(BoatEntity.Type.valueOf(tier.variant()));
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
        if (isLogicalSideForUpdatingMovement() && hasPassengers() && tier.speed() != 1.0) {
            Vec3d v = getVelocity();
            setVelocity(v.x * tier.speed(), v.y, v.z * tier.speed());
        }
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (player.isSneaking()) {
            if (!getWorld().isClient() && player instanceof ServerPlayerEntity sp) {
                SailingService.dock(sp, this);
            }
            return ActionResult.success(getWorld().isClient());
        }
        if (!getWorld().isClient() && player instanceof ServerPlayerEntity sp && !SailingService.canBoard(sp, this)) {
            return ActionResult.SUCCESS;
        }
        return super.interact(player, hand);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return false;
    }

    @Override
    public Item asItem() {
        return Items.AIR;
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (owner != null) {
            nbt.putUuid("RunecraftOwner", owner);
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.containsUuid("RunecraftOwner")) {
            owner = nbt.getUuid("RunecraftOwner");
        }
    }
}
