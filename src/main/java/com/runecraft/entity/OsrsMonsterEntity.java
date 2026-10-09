package com.runecraft.entity;

import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.MonsterStats;
import com.runecraft.osrs.AttackType;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

/**
 * A Gielinor creature with OSRS combat stats. Attacks are performed by {@link OsrsAttackGoal}
 * at the monster's real attack speed; damage is rolled with OSRS formulas.
 */
public class OsrsMonsterEntity extends HostileEntity {
    private MonsterStats cachedStats;

    public OsrsMonsterEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 0;
    }

    public MonsterStats stats() {
        if (cachedStats == null) {
            cachedStats = ModEntities.stats(getType());
        }
        return cachedStats;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new OsrsAttackGoal(this));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, this::shouldAggro));
    }

    /** OSRS aggression: only attacks players whose combat level is at most double its own (+1). */
    protected boolean shouldAggro(net.minecraft.entity.LivingEntity entity) {
        MonsterStats s = stats();
        if (s == null || s.monster() == null || !(entity instanceof ServerPlayerEntity player) || player.isCreative()) {
            return false;
        }
        return s.monster().isAggressiveTo(Players.data(player).skills().combatLevel());
    }

    /** Style used for the next attack against a target at the given distance. */
    public AttackType chooseStyle(ServerPlayerEntity target, double distanceSq) {
        return stats().style();
    }

    /** Hook: adjust damage a player is about to deal to this monster. */
    public int modifyIncoming(ServerPlayerEntity attacker, int damage, AttackType type) {
        return damage;
    }

    /** Hook: max hit against a player for an attack style. */
    public int maxHitAgainst(ServerPlayerEntity player, AttackType type) {
        return stats().maxHit();
    }

    /** Hook: damage that still lands when the player uses the matching protection prayer. */
    public int damageThroughPrayer(int damage) {
        return 0;
    }

    /** Hook: called with the rolled damage just before it's applied to the player. */
    public int onHitPlayer(ServerPlayerEntity player, int damage, AttackType type) {
        return damage;
    }

    /** Hook: called when a player kills this monster. */
    public void onKilledBy(ServerPlayerEntity player) {
    }

    /** Performs an attack now (called by the attack goal when the attack speed allows). */
    public void performAttack(ServerPlayerEntity target, AttackType type) {
        if (type == AttackType.MAGIC) {
            com.runecraft.combat.MagicService.npcCast(this, target);
        } else {
            this.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            CombatHandler.npcAttack(this, target, type);
        }
    }

    @Override
    public boolean tryAttack(net.minecraft.entity.Entity target) {
        // Vanilla melee is replaced by OsrsAttackGoal.
        return false;
    }

    @Override
    protected boolean isDisallowedInPeaceful() {
        return stats() != null && stats().monster() != null && stats().monster().aggressive();
    }
}
