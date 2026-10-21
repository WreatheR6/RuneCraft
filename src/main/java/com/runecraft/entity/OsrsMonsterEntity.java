package com.runecraft.entity;

import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.MonsterStats;
import com.runecraft.osrs.AttackType;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A Gielinor creature with OSRS combat stats. Attacks are performed by {@link OsrsAttackGoal}
 * at the monster's real attack speed; damage is rolled with OSRS formulas. Who it fights is decided by {@link OsrsTargetGoal}
 * (round 42: OSRS aggression, hunt range, leash to its spawn point, sticky retaliation, single-way readability).
 */
public class OsrsMonsterEntity extends Monster implements OsrsCombatant {
    private MonsterStats cachedStats;
    private OsrsTargetGoal targetGoal;

    public OsrsMonsterEntity(EntityType<? extends Monster> type, Level world) {
        super(type, world);
        this.xpReward = 0;
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

    /**
     * Closes to melee reach before attacking whatever its attack style (the Fortis Colosseum's warband archer and seer only
     * attack from melee range). Not saved.
     */
    public boolean meleeRangeOnly;

    /** Distance (blocks) a ranged or magic attacker attacks from; 0 keeps the default 10 (the Colosseum's colossi reach 15). */
    public double rangedReach;

    /** Takes over an attack this creature is about to make; returns true when it handled the attack itself. */
    @FunctionalInterface
    public interface AttackInterceptor {
        boolean intercept(OsrsMonsterEntity mob, ServerPlayer target, com.runecraft.osrs.content.AttackDef attack);
    }

    /**
     * A minigame's hook on this creature's attacks (the Colosseum's Javelin Colossus replacing every fifth throw, manticores
     * launching their orb patterns). Not saved: set when the creature is spawned.
     */
    public AttackInterceptor attackInterceptor;

    public MonsterStats stats() {
        if (cachedStats == null) {
            cachedStats = ModEntities.stats(getType());
        }
        return cachedStats;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new OsrsAttackGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        addTargetGoal(true);
    }

    /** Registers this monster's OSRS target goal; {@code hunts}: it looks for players to attack unprovoked. */
    protected final void addTargetGoal(boolean hunts) {
        targetGoal = new OsrsTargetGoal(this, hunts);
        this.targetSelector.addGoal(1, targetGoal);
    }

    /** The goal that picks this monster's target (aggression, retaliation, leash). */
    public OsrsTargetGoal targetGoal() {
        return targetGoal;
    }

    /**
     * OSRS aggression: only attacks players whose combat level is at most double its own (any level in the
     * Wilderness), and stops once the player has been in the area for 10 minutes (tolerance; some monsters never tolerate).
     * An open-world creature takes its aggressiveness from the wiki ({@link com.runecraft.osrs.data.MonsterAggression}, round 42:
     * most Slayer creatures are passive); encounter creatures (bosses, raids, minigames, quest spawns) keep their own flag.
     */
    protected boolean shouldAggro(net.minecraft.world.entity.LivingEntity entity) {
        MonsterStats s = stats();
        if (s == null || (s.monster() == null && s.profile() == null) || !(entity instanceof ServerPlayer player)) {
            return false;
        }
        boolean own = s.monster() != null ? s.monster().aggressive() : s.profile().aggressive;
        String key = s.traitsKey();
        boolean aggressive = isOpenWorld() ? com.runecraft.osrs.data.MonsterAggression.aggressive(key, own) : own;
        return OsrsTargetGoal.osrsAggression(this, player, aggressive, s.combatLevel(), key);
    }

    /** True for a monster a world spawner placed (it has a spawn point to stay near), false for encounter monsters. */
    public boolean isOpenWorld() {
        return MonsterHome.get(this) != null;
    }

    /**
     * Remembers where a world spawner (spawn points, camps: SPAWNER; structures: STRUCTURE) placed an ordinary monster, so it hunts,
     * chases and returns around its spawn point (round 42). Bosses and encounter creatures never get one.
     */
    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty, net.minecraft.world.entity.MobSpawnType type,
            net.minecraft.world.entity.SpawnGroupData data) {
        net.minecraft.world.entity.SpawnGroupData out = super.finalizeSpawn(level, difficulty, type, data);
        if ((type == net.minecraft.world.entity.MobSpawnType.SPAWNER || type == net.minecraft.world.entity.MobSpawnType.STRUCTURE)
                && !(this instanceof BossEntity) && !(this instanceof com.runecraft.boss.DataBossEntity) && !encounterOnly()) {
            MonsterHome.setIfAbsent(this, blockPosition());
        }
        return out;
    }

    private boolean encounterOnly() {
        MonsterStats s = stats();
        return s != null && s.profile() != null && com.runecraft.osrs.world.SpawnTable.encounterOnly().containsKey(s.profile().id);
    }

    // ------------------------------------------------------------------ OsrsCombatant

    @Override
    public net.minecraft.world.entity.Mob self() {
        return this;
    }

    @Override
    public boolean wantsToAttack(ServerPlayer player) {
        return shouldAggro(player);
    }

    @Override
    public double huntRange() {
        if (!isOpenWorld()) {
            return com.runecraft.osrs.MonsterAi.ENCOUNTER_HUNT_BLOCKS;
        }
        MonsterStats s = stats();
        return com.runecraft.osrs.MonsterAi.huntBlocks(s != null && s.profile() != null ? s.profile().id : null);
    }

    @Override
    public boolean meleeRangeOnly() {
        return meleeRangeOnly;
    }

    @Override
    public double rangedReach() {
        return rangedReach;
    }

    @Override
    public boolean directPathing() {
        return directPathing;
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

    /**
     * Style used for the next attack against a target at the given distance. A module creature's attack is rolled once and kept
     * until {@link #performAttack} uses it, or until it can't be used at the current distance (// playability: the goal asks every
     * tick, and a re-roll every tick made a mixed melee/magic creature stop and start walking): the choice is made on the
     * bounding-box gap to the target.
     */
    public AttackType chooseStyle(ServerPlayer target, double distanceSq) {
        java.util.List<com.runecraft.osrs.content.AttackDef> attacks = attackList();
        if (attacks != null) {
            double gap = Math.max(0, Math.sqrt(distanceSq) - getBbWidth() / 2 - target.getBbWidth() / 2);
            if (nextAttack == null || !containsSame(attacks, nextAttack) || gap > nextAttack.range()) {
                nextAttack = com.runecraft.boss.Mechanics.choose(attacks, gap, getRandom());
            }
            return nextAttack.style();
        }
        return stats().style();
    }

    private static boolean containsSame(java.util.List<com.runecraft.osrs.content.AttackDef> attacks, com.runecraft.osrs.content.AttackDef a) {
        for (com.runecraft.osrs.content.AttackDef x : attacks) {
            if (x == a) {
                return true;
            }
        }
        return false;
    }

    /** Hook: adjust damage a player is about to deal to this monster. Applies slayer/immunity/weapon rules. */
    public int modifyIncoming(ServerPlayer attacker, int damage, AttackType type) {
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
        if (p.requiredWeapon != null && !p.requiredWeapon.equals(com.runecraft.util.Inv.idOf(attacker.getMainHandItem()))) {
            com.runecraft.util.Msg.action(attacker, "You need a special weapon to harm this creature.");
            return 0;
        }
        return damage;
    }

    /** Hook: max hit against a player for an attack style. */
    public int maxHitAgainst(ServerPlayer player, AttackType type) {
        return stats().maxHit();
    }

    /** Hook: damage that still lands when the player uses the matching protection prayer. */
    public int damageThroughPrayer(int damage) {
        return 0;
    }

    /** Hook: called with the rolled damage just before it's applied to the player. */
    public int onHitPlayer(ServerPlayer player, int damage, AttackType type) {
        return damage;
    }

    /**
     * {@link #onHitPlayer(ServerPlayer, int, AttackType)} for a hit of {@code attack} landing now ({@code Mechanics}: the hit was
     * started earlier, and the monster may have started another attack since).
     */
    public int onHitPlayer(ServerPlayer player, int damage, AttackType type, com.runecraft.osrs.content.AttackDef attack) {
        return onHitPlayer(player, damage, type);
    }

    /** Hook: called when a player kills this monster. */
    public void onKilledBy(ServerPlayer player) {
    }

    /**
     * Performs an attack now (called by the attack goal when the attack speed allows; the goal stamps the cadence). The hit is
     * delivered in real time (// playability, round 41): a module creature's attack through {@code Mechanics.perform}, a built-in
     * ranged or magic attack as a projectile ({@code Mechanics.deliverProjectile}), a built-in melee attack after its swing's
     * wind-up ({@code Mechanics.deliverMelee}); each rolls when it lands, without the per-monster cooldown (the goal owns it).
     */
    public void performAttack(ServerPlayer target, AttackType type) {
        com.runecraft.osrs.content.AttackDef planned = attackList() != null ? nextAttack : null;
        if (attackInterceptor != null && attackInterceptor.intercept(this, target, planned)) {
            nextAttack = null; // used: the next attack is rolled afresh
            return;
        }
        if (nextAttack != null && attackList() != null) {
            double multiplier = damageMultiplier(); // read before the attack is used (data bosses scale by it)
            com.runecraft.osrs.content.AttackDef attack = nextAttack;
            nextAttack = null;
            com.runecraft.boss.Mechanics.perform(this, target, attack, multiplier);
            return;
        }
        boolean boss = this instanceof BossEntity || this instanceof com.runecraft.boss.DataBossEntity;
        if (type == AttackType.MAGIC || type == AttackType.RANGED) {
            com.runecraft.boss.Mechanics.deliverProjectile(this, target, type, boss,
                    () -> CombatHandler.npcAttack(this, target, type, true));
        } else {
            this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            com.runecraft.boss.Mechanics.deliverMelee(this, target, com.runecraft.osrs.ThreatTiming.MELEE_REACH,
                    () -> CombatHandler.npcAttack(this, target, type, true), takeMeleeAfter());
        }
    }

    /** What must run once a built-in melee swing has resolved (a boss script's after-attack hook), or null. */
    protected Runnable takeMeleeAfter() {
        return null;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK == 0) {
            com.runecraft.combat.PoisonService.tick(this, com.runecraft.action.ActionManager.gameTick());
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        // Vanilla melee is replaced by OsrsAttackGoal.
        return false;
    }

    @Override
    public boolean shouldDespawnInPeaceful() {
        MonsterStats s = stats();
        return s != null && (s.monster() != null ? s.monster().aggressive() : s.profile() != null && s.profile().aggressive);
    }
}
