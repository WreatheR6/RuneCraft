package com.runecraft.entity;

import com.runecraft.osrs.QuestNpc;
import com.runecraft.quest.QuestManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** A stationary, invulnerable quest character. Right-click to talk. */
public class QuestNpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> NPC = SynchedEntityData.defineId(QuestNpcEntity.class, EntityDataSerializers.STRING);
    /** A built-in character loaded far from its OSRS tile (left at an old spot): removed on its first tick. */
    private boolean misplaced;

    public QuestNpcEntity(EntityType<? extends PathfinderMob> type, Level world) {
        super(type, world);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(NPC, QuestNpc.COOK.name());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0f, 1.0f));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    /** NPC id: a {@link QuestNpc} enum name for built-in characters or a module {@code NpcDef} id. */
    public String npcId() {
        return this.entityData.get(NPC);
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
        this.entityData.set(NPC, npc.name());
        setCustomName(Component.literal(npc.displayName()));
        setCustomNameVisible(true);
    }

    public void setNpc(com.runecraft.osrs.content.NpcDef npc) {
        this.entityData.set(NPC, npc.id());
        setCustomName(Component.literal(npc.name()));
        setCustomNameVisible(true);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putString("Npc", npcId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        String id = nbt.getString("Npc");
        com.runecraft.osrs.content.NpcDef def = com.runecraft.osrs.content.ContentRegistry.npc(id);
        if (def != null) {
            setNpc(def);
            // Left at the place it stood before it moved: the new place spawns its own copy.
            misplaced = com.runecraft.osrs.content.quests.QuestNpcMoves.stale(def, getBlockX(), getBlockZ());
        } else {
            try {
                QuestNpc q = QuestNpc.valueOf(id);
                setNpc(q);
                // Saved by an older version near the town centre: the character now spawns on its own OSRS tile.
                misplaced = q.remote() && Math.max(Math.abs(getBlockX() - q.blockX()), Math.abs(getBlockZ() - q.blockZ())) > QuestNpc.REMOTE;
            } catch (IllegalArgumentException ignored) {
                // An NPC that no longer exists (removed or merged into another module's): it would otherwise act as the Cook.
                misplaced = true;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (misplaced && !level().isClientSide()) {
            discard();
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide() && player instanceof ServerPlayer sp) {
            QuestManager.talk(sp, this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.isCreativePlayer();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }
}
