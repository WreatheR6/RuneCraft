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
    private static final TrackedData<String> NPC = DataTracker.registerData(QuestNpcEntity.class, TrackedDataHandlerRegistry.STRING);

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
        this.dataTracker.startTracking(NPC, QuestNpc.COOK.name());
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f, 1.0f));
        this.goalSelector.add(2, new LookAroundGoal(this));
    }

    /** NPC id: a {@link QuestNpc} enum name for built-in characters or a module {@code NpcDef} id. */
    public String npcId() {
        return this.dataTracker.get(NPC);
    }

    /** The built-in quest character, or null for module NPCs. */
    public QuestNpc npc() {
        try {
            return QuestNpc.valueOf(npcId());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setNpc(QuestNpc npc) {
        this.dataTracker.set(NPC, npc.name());
        setCustomName(Text.literal(npc.displayName()));
        setCustomNameVisible(true);
    }

    public void setNpc(com.runecraft.osrs.content.NpcDef npc) {
        this.dataTracker.set(NPC, npc.id());
        setCustomName(Text.literal(npc.name()));
        setCustomNameVisible(true);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("Npc", npcId());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        String id = nbt.getString("Npc");
        com.runecraft.osrs.content.NpcDef def = com.runecraft.osrs.content.ContentRegistry.npc(id);
        if (def != null) {
            setNpc(def);
        } else {
            try {
                setNpc(QuestNpc.valueOf(id));
            } catch (IllegalArgumentException ignored) {
                // keep default
            }
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
