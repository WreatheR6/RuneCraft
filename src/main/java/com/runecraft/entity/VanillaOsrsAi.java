package com.runecraft.entity;

import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.MonsterStats;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.MonsterAi;
import com.runecraft.osrs.ThreatTiming;
import com.runecraft.osrs.data.Monster;
import com.runecraft.world.Gielinor;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

/**
 * Vanilla mobs that stand in for OSRS monsters in Gielinor (Giant rat = silverfish, Spider = cave spider, Giant spider = spider,
 * Zombie, Skeleton, Wolf, Dark wizard = witch) put on OSRS AI when they join the world (round 42): their vanilla attack, target,
 * leap, potion, stone-merging, begging and sun goals are stripped and they get the OSRS attack goal (their {@link Monster} stats at
 * the OSRS attack speed, delivered in real time through {@code Mechanics}: melee after a wind-up, the witch's spell and the skeleton's
 * arrow as projectiles) and the OSRS target goal (aggression by combat level, tolerance, hunt range, leash, sticky retaliation).
 * Beneficial potion effects (the witch's drinking, the hard-difficulty spider's speed or regeneration) never take on them, and a
 * zombie calls no reinforcements. Outside Gielinor they stay vanilla.
 */
public final class VanillaOsrsAi implements OsrsCombatant {
    /** The {@link Monster} constants with vanilla mobs that fight. */
    private static final Set<Monster> FIGHTERS = Set.of(Monster.GIANT_RAT, Monster.SPIDER, Monster.GIANT_SPIDER, Monster.ZOMBIE,
            Monster.SKELETON, Monster.WOLF, Monster.WITCH);
    /** The goals a stripped mob keeps (exact classes: the silverfish's stone-merging goal is a RandomStrollGoal subclass). */
    private static final Set<Class<?>> KEEP = Set.of(FloatGoal.class, WaterAvoidingRandomStrollGoal.class, RandomStrollGoal.class,
            LookAtPlayerGoal.class, RandomLookAroundGoal.class);
    private static final Map<Mob, VanillaOsrsAi> INSTALLED = new WeakHashMap<>();

    private final Mob mob;
    private final Monster def;
    private final MonsterStats stats;
    private OsrsTargetGoal targetGoal;

    private VanillaOsrsAi(Mob mob, Monster def) {
        this.mob = mob;
        this.def = def;
        this.stats = MonsterStats.from(def);
    }

    /** Hooks the world: called once from {@code LocationSpawner.register}. */
    public static void register() {
        com.runecraft.platform.ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof Mob mob && Gielinor.is(world)) {
                if (mob.getTags().contains(com.runecraft.world.LocationSpawner.TAG)) {
                    MonsterHome.setIfAbsent(mob, mob.blockPosition()); // a spawn point's monster from before round 42
                }
                install(mob);
            }
        });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable.class, e -> {
                    if (e.getEntity() instanceof Mob mob && isInstalled(mob)
                            && e.getEffectInstance().getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
                        e.setResult(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
                    }
                });
    }

    /** The {@link Monster} a vanilla mob fights as in Gielinor, or null for anything else (animals, villagers, pets, summons). */
    public static Monster fighter(Mob mob) {
        if (mob instanceof OsrsMonsterEntity || mob instanceof TamableAnimal tame && tame.isTame()) {
            return null;
        }
        for (String tag : mob.getTags()) {
            if (tag.startsWith("runecraft_owner:")) {
                return null; // a player's thrall
            }
        }
        Monster m = Monster.byEntity(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString());
        return m != null && !m.isCustom() && FIGHTERS.contains(m) ? m : null;
    }

    public static synchronized boolean isInstalled(Mob mob) {
        return INSTALLED.containsKey(mob);
    }

    /** The target goal of a vanilla mob on OSRS AI, or null. */
    public static synchronized OsrsTargetGoal targetGoal(Mob mob) {
        VanillaOsrsAi ai = INSTALLED.get(mob);
        return ai == null ? null : ai.targetGoal;
    }

    /** Puts a vanilla stand-in on OSRS AI (once); true if it is on OSRS AI now. */
    public static synchronized boolean install(Mob mob) {
        if (INSTALLED.containsKey(mob)) {
            return true;
        }
        Monster def = fighter(mob);
        if (def == null || !Gielinor.is(mob)) {
            return false;
        }
        GoalSelector goals = selector(mob, true);
        GoalSelector targets = selector(mob, false);
        if (goals == null || targets == null) {
            return false;
        }
        VanillaOsrsAi ai = new VanillaOsrsAi(mob, def);
        boolean strolls = false;
        for (WrappedGoal w : java.util.List.copyOf(goals.getAvailableGoals())) {
            Goal g = w.getGoal();
            if (!KEEP.contains(g.getClass())) {
                goals.removeGoal(g);
            } else if (g instanceof RandomStrollGoal) {
                strolls = true;
            }
        }
        targets.removeAllGoals(g -> true);
        goals.addGoal(2, new OsrsAttackGoal(ai));
        if (!strolls && mob instanceof net.minecraft.world.entity.PathfinderMob walker) {
            goals.addGoal(6, new WaterAvoidingRandomStrollGoal(walker, 0.8));
        }
        ai.targetGoal = new OsrsTargetGoal(ai, true);
        targets.addGoal(1, ai.targetGoal);
        mob.setTarget(null);
        // // playability: a chase at a player's walking pace, as the built-in monsters (OSRS: one tile a tick for everyone)
        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(com.runecraft.registry.ModEntities.DEFAULT_MOVE_SPEED);
        }
        AttributeInstance reinforcements = mob.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
        if (reinforcements != null) {
            reinforcements.setBaseValue(0); // OSRS zombies call no friends
        }
        mob.removeAllEffects();
        INSTALLED.put(mob, ai);
        return true;
    }

    /** The goals (or, with {@code targets}, the target goals) a mob has now (self-tests). */
    public static java.util.List<Goal> goals(Mob mob, boolean targets) {
        GoalSelector s = selector(mob, !targets);
        java.util.List<Goal> out = new java.util.ArrayList<>();
        if (s != null) {
            for (WrappedGoal w : s.getAvailableGoals()) {
                out.add(w.getGoal());
            }
        }
        return out;
    }

    /** A mob's goal or target selector (public through NeoForge's access transformer). */
    private static GoalSelector selector(Mob mob, boolean goals) {
        return goals ? mob.goalSelector : mob.targetSelector;
    }

    // ------------------------------------------------------------------ OsrsCombatant

    @Override
    public Mob self() {
        return mob;
    }

    @Override
    public MonsterStats stats() {
        return stats;
    }

    @Override
    public AttackType chooseStyle(ServerPlayer target, double distanceSq) {
        return def.style();
    }

    @Override
    public void performAttack(ServerPlayer target, AttackType type) {
        mob.swing(InteractionHand.MAIN_HAND);
        if (type == AttackType.MAGIC || type == AttackType.RANGED) {
            com.runecraft.boss.Mechanics.deliverProjectile(mob, target, type, false,
                    () -> CombatHandler.npcAttack(mob, target, type, true));
        } else {
            com.runecraft.boss.Mechanics.deliverMelee(mob, target, ThreatTiming.MELEE_REACH,
                    () -> CombatHandler.npcAttack(mob, target, type, true));
        }
    }

    @Override
    public boolean wantsToAttack(ServerPlayer player) {
        return OsrsTargetGoal.osrsAggression(mob, player, def.aggressive(), def.combatLevel(),
                com.runecraft.osrs.data.MonsterTraits.key(def));
    }

    @Override
    public double huntRange() {
        return MonsterHome.get(mob) != null ? MonsterAi.huntBlocks(def.name().toLowerCase(java.util.Locale.ROOT))
                : MonsterAi.ENCOUNTER_HUNT_BLOCKS;
    }
}
