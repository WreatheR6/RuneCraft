package com.runecraft.entity;

import com.runecraft.osrs.QuestNpc;
import com.runecraft.quest.QuestManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** A stationary, invulnerable quest character. Right-click to talk. */
public class QuestNpcEntity extends PathAwareEntity {
    private static final TrackedData<Integer> NPC = DataTracker.registerData(QuestNpcEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public QuestNpcEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        setPersistent();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(NPC, 0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f, 1.0f));
        this.goalSelector.add(2, new LookAroundGoal(this));
    }

    public QuestNpc npc() {
        int i = this.dataTracker.get(NPC);
        QuestNpc[] all = QuestNpc.values();
        return all[Math.max(0, Math.min(all.length - 1, i))];
    }

    public void setNpc(QuestNpc npc) {
        this.dataTracker.set(NPC, npc.ordinal());
        setCustomName(Text.literal(npc.displayName()));
        setCustomNameVisible(true);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("Npc", npc().name());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        try {
            setNpc(QuestNpc.valueOf(nbt.getString("Npc")));
        } catch (IllegalArgumentException ignored) {
            // keep default
        }
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }
        if (!getWorld().isClient() && player instanceof ServerPlayerEntity sp) {
            QuestManager.talk(sp, this);
        }
        return ActionResult.success(getWorld().isClient());
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.isSourceCreativePlayer();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }
}
