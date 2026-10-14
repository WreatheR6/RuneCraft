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

    /**
     * Replaces this creature's combat stats (raid party/mode scaling). The function receives the type's base
     * stats, so re-applying it never compounds. Not saved: raid creatures are rescaled when spawned.
     */
    public void overrideStats(java.util.function.UnaryOperator<MonsterStats> f) {
        MonsterStats base = ModEntities.stats(getType());
        if (base != null) {
            MonsterStats out = f.apply(base);
            cachedStats = out == null ? base : out;
        }
    }

    /**
     * OSRS "dumb" NPC pathing: the creature walks straight at its target and stops at obstacles instead of routing around
     * them, so it can be safespotted (e.g. the Fortis Colosseum's minotaurs without Red Flag). Not saved.
     */
    public boolean directPathing;

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

    /**
     * OSRS aggression: only attacks players whose combat level is at most double its own (any level in the
     * Wilderness), and stops once the player has been in the area for 10 minutes (tolerance).
     */
    protected boolean shouldAggro(net.minecraft.entity.LivingEntity entity) {
        MonsterStats s = stats();
        if (s == null || (s.monster() == null && s.profile() == null) || !(entity instanceof ServerPlayerEntity player) || player.isCreative()) {
            return false;
        }
        boolean aggressive = s.monster() != null ? s.monster().aggressive() : s.profile().aggressive;
        if (!aggressive || com.runecraft.combat.Aggression.tolerant(player, com.runecraft.action.ActionManager.gameTick())) {
            return false;
        }
        // Single-way combat: don't pick a fight with someone already fighting something else.
        if (!com.runecraft.combat.CombatZones.npcMayAttack(this, player)) {
            return false;
        }
        if (com.runecraft.world.Gielinor.wildernessLevel(player) > 0) {
            return true;
        }
        int combat = Players.data(player).skills().combatLevel();
        return s.monster() != null ? s.monster().isAggressiveTo(combat) : s.profile().isAggressiveTo(combat);
    }

    /** Attack chosen by {@link #chooseStyle} for module creatures with an attack list. */
    protected com.runecraft.osrs.content.AttackDef nextAttack;

    /** Attack list for module creatures (null for built-in ones). */
    protected java.util.List<com.runecraft.osrs.content.AttackDef> attackList() {
        MonsterStats s = stats();
        return s != null && s.profile() != null && !s.profile().attacks.isEmpty() ? s.profile().attacks : null;
    }

    /** Max hit multiplier (enrage phases). */
    protected double damageMultiplier() {
        return 1.0;
    }

    /** Style used for the next attack against a target at the given distance. */
    public AttackType chooseStyle(ServerPlayerEntity target, double distanceSq) {
        java.util.List<com.runecraft.osrs.content.AttackDef> attacks = attackList();
        if (attacks != null) {
            nextAttack = com.runecraft.boss.Mechanics.choose(attacks, Math.sqrt(distanceSq) - getWidth() / 2, getRandom());
            return nextAttack.style();
        }
        return stats().style();
    }

    /** Hook: adjust damage a player is about to deal to this monster. Applies slayer/immunity/weapon rules. */
    public int modifyIncoming(ServerPlayerEntity attacker, int damage, AttackType type) {
        MonsterStats s = stats();
        if (s == null || s.profile() == null) {
            return damage;
        }
        com.runecraft.osrs.content.CombatProfile<?> p = s.profile();
        if (Players.data(attacker).skills().level(com.runecraft.osrs.Skill.SLAYER) < p.slayerLevel) {
            com.runecraft.util.Msg.game(attacker, "You need a Slayer level of " + p.slayerLevel + " to know how to wound this monster.");
            return 0;
        }
        if (p.immuneTo.contains(type)) {
            com.runecraft.util.Msg.action(attacker, "The " + p.name + " is immune to that attack style.");
            return 0;
        }
        if (p.requiredWeapon != null && !p.requiredWeapon.equals(com.runecraft.util.Inv.idOf(attacker.getMainHandStack()))) {
            com.runecraft.util.Msg.action(attacker, "You need a special weapon to harm this creature.");
            return 0;
        }
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
        if (nextAttack != null && attackList() != null) {
            com.runecraft.boss.Mechanics.perform(this, target, nextAttack, damageMultiplier());
            return;
        }
        if (type == AttackType.MAGIC) {
            com.runecraft.combat.MagicService.npcCast(this, target);
        } else {
            this.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            CombatHandler.npcAttack(this, target, type);
        }
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (age % com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK == 0) {
            com.runecraft.combat.PoisonService.tick(this, com.runecraft.action.ActionManager.gameTick());
        }
    }

    @Override
    public boolean tryAttack(net.minecraft.entity.Entity target) {
        // Vanilla melee is replaced by OsrsAttackGoal.
        return false;
    }

    @Override
    public boolean isDisallowedInPeaceful() {
        MonsterStats s = stats();
        return s != null && (s.monster() != null ? s.monster().aggressive() : s.profile() != null && s.profile().aggressive);
    }
}
