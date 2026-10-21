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
 * Zombie, Skeleton, Wolf, Dark wizard = witch; since round 43 the passive Cow, Chicken, Sheep and Pig too) put on OSRS AI when they
 * join the world (round 42): their vanilla attack, target, leap, potion, stone-merging, begging, sun, panic, breeding and tempting
 * goals are stripped and they get the OSRS attack goal (their {@link Monster} stats at the OSRS attack speed, delivered in real
 * time through {@code Mechanics}: melee after a wind-up, the witch's spell and the skeleton's arrow as projectiles), the OSRS target
 * goal (aggression from the wiki by combat level, tolerance, hunt range, leash, sticky retaliation) and the low-hitpoint retreat
 * (the chicken, {@link OsrsRetreatGoal}). A hit cow, chicken, sheep or pig fights back with its OSRS stats instead of panicking (a
 * sheep keeps grazing to regrow its wool). Round 43 also gives them their OSRS hitpoints (a wolf has 34, not vanilla's 8), the
 * OSRS regeneration of a hitpoint a minute, and no burning in Gielinor's daylight (the zombie and the skeleton). A stray vanilla
 * villager becomes the Man or Woman it stands for ({@link #convertVillager}: its brain-driven AI can't be put on OSRS goals).
 * Beneficial potion effects (the witch's drinking, the hard-difficulty spider's speed or regeneration) never take on them, and a
 * zombie calls no reinforcements. Outside Gielinor they stay vanilla.
 *
 * <p>One a world spawner placed (a spawn point, a dungeon camp: SPAWNER or STRUCTURE) gets a spawn point ({@link MonsterHome}) and
 * the open-world rules. Any other is an encounter spawn (a boss's or quest's summon, a quest guard): it keeps the target it was
 * given and attacks any player whatever their combat level or tolerance, as its vanilla AI did.
 */
public final class VanillaOsrsAi implements OsrsCombatant {
    /** The {@link Monster} constants with vanilla mobs that fight. */
    private static final Set<Monster> FIGHTERS = Set.of(Monster.GIANT_RAT, Monster.SPIDER, Monster.GIANT_SPIDER, Monster.ZOMBIE,
            Monster.SKELETON, Monster.WOLF, Monster.WITCH, Monster.COW, Monster.CHICKEN, Monster.SHEEP, Monster.PIG);
    /**
     * The goals a stripped mob keeps (exact classes: the silverfish's stone-merging goal is a RandomStrollGoal subclass; the sheep's
     * grazing regrows its wool).
     */
    private static final Set<Class<?>> KEEP = Set.of(FloatGoal.class, WaterAvoidingRandomStrollGoal.class, RandomStrollGoal.class,
            LookAtPlayerGoal.class, RandomLookAroundGoal.class, net.minecraft.world.entity.ai.goal.EatBlockGoal.class);
    /** Stray villagers waiting to become a Man or Woman at the end of the tick (an entity can't be added while one joins). */
    private static final java.util.List<net.minecraft.world.entity.npc.Villager> VILLAGERS = new java.util.ArrayList<>();
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
            if (entity instanceof net.minecraft.world.entity.npc.Villager villager && Gielinor.is(world)) {
                synchronized (VILLAGERS) {
                    VILLAGERS.add(villager);
                }
                return;
            }
            if (entity instanceof Mob mob && Gielinor.is(world)) {
                if (mob.getTags().contains(com.runecraft.world.LocationSpawner.TAG) || worldSpawned(mob) && fighter(mob) != null) {
                    // a spawn point's monster from before round 42, or a camp's (DungeonCamps and the like place vanilla mobs too)
                    MonsterHome.setIfAbsent(mob, mob.blockPosition());
                } else if (mob instanceof OsrsMonsterEntity m && MonsterHome.get(m) == null && townResident(m)) {
                    // a town resident placed before round 43 (as an EVENT spawn): it gets the spawn point it has now
                    MonsterHome.set(m, m.blockPosition());
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
        com.runecraft.platform.ServerTickEvents.END_SERVER_TICK.register(server -> {
            java.util.List<net.minecraft.world.entity.npc.Villager> due;
            synchronized (VILLAGERS) {
                if (VILLAGERS.isEmpty()) {
                    return;
                }
                due = new java.util.ArrayList<>(VILLAGERS);
                VILLAGERS.clear();
            }
            for (net.minecraft.world.entity.npc.Villager v : due) {
                convertVillager(v);
            }
        });
    }

    /**
     * A vanilla villager in Gielinor becomes the Man or Woman it stands for ({@link Monster#VILLAGER} is the Man: level 2, 7
     * hitpoints, passive), on OSRS AI like every townsperson: it fights back when hit instead of panicking, its spot becoming its
     * spawn point. The villager's own AI is a brain of behaviours, not goals, so it can't be stripped like the other stand-ins.
     * Returns the new monster, or null when it wasn't converted.
     */
    public static Mob convertVillager(net.minecraft.world.entity.npc.Villager v) {
        if (!v.isAlive() || v.isRemoved() || !(v.level() instanceof net.minecraft.server.level.ServerLevel level) || !Gielinor.is(level)) {
            return null;
        }
        String id = (v.getUUID().getLeastSignificantBits() & 1) == 0 ? Monster.MAN.entity() : "woman";
        net.minecraft.world.entity.EntityType<?> type = com.runecraft.registry.ModEntities.typeById(id);
        if (type == null) {
            type = com.runecraft.registry.ModEntities.typeById(Monster.MAN.entity());
        }
        if (type == null || !(type.create(level) instanceof Mob man)) {
            return null;
        }
        net.minecraft.core.BlockPos home = MonsterHome.get(v) != null ? MonsterHome.get(v) : v.blockPosition();
        man.moveTo(v.getX(), v.getY(), v.getZ(), v.getYRot(), v.getXRot());
        if (v.hasCustomName()) {
            man.setCustomName(v.getCustomName());
        }
        man.finalizeSpawn(level, level.getCurrentDifficultyAt(v.blockPosition()), net.minecraft.world.entity.MobSpawnType.CONVERSION, null);
        man.setPersistenceRequired();
        MonsterHome.set(man, home);
        v.discard();
        level.addFreshEntity(man);
        return man;
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

    private static Set<String> townIds;

    /**
     * A town resident ({@code TownSpawner}'s creatures: men, guards, muggers, snails...) that town spawner placed as an EVENT spawn
     * before round 43, so it has no spawn point and would follow an encounter monster's rules.
     */
    private static boolean townResident(OsrsMonsterEntity m) {
        if (m.getSpawnType() != net.minecraft.world.entity.MobSpawnType.EVENT || m.stats() == null || m.stats().profile() == null) {
            return false;
        }
        if (townIds == null) {
            java.util.Set<String> ids = new java.util.HashSet<>();
            for (com.runecraft.osrs.content.npcs.TownMonsters.TownSpawn s : com.runecraft.osrs.content.npcs.TownMonsters.spawns()) {
                ids.add(s.creature());
            }
            townIds = ids;
        }
        return townIds.contains(m.stats().profile().id);
    }

    /** Placed by a world spawner (NeoForge keeps the spawn type finalizeSpawn was given, saved with the mob). */
    private static boolean worldSpawned(Mob mob) {
        net.minecraft.world.entity.MobSpawnType type = mob.getSpawnType();
        return type == net.minecraft.world.entity.MobSpawnType.SPAWNER || type == net.minecraft.world.entity.MobSpawnType.STRUCTURE;
    }

    /** An encounter spawn: no spawn point (a boss's or quest's summon, a quest guard). */
    private boolean encounter() {
        return MonsterHome.get(mob) == null;
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
        // a summon is handed its target before it joins the world (DataBossEntity.summonMinions): stripping the vanilla target
        // goals would drop it
        net.minecraft.world.entity.LivingEntity given = mob.getTarget();
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
        goals.addGoal(0, new Upkeep(mob));
        goals.addGoal(1, new OsrsRetreatGoal(ai));
        goals.addGoal(2, new OsrsAttackGoal(ai));
        if (!strolls && mob instanceof net.minecraft.world.entity.PathfinderMob walker) {
            goals.addGoal(6, new WaterAvoidingRandomStrollGoal(walker, 0.8));
        }
        ai.targetGoal = new OsrsTargetGoal(ai, true);
        targets.addGoal(1, ai.targetGoal);
        mob.setTarget(given instanceof ServerPlayer ? given : null); // OSRS monsters fight players only
        // // playability: a chase at a player's walking pace, as the built-in monsters (OSRS: one tile a tick for everyone)
        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(com.runecraft.registry.ModEntities.DEFAULT_MOVE_SPEED);
        }
        AttributeInstance reinforcements = mob.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
        if (reinforcements != null) {
            reinforcements.setBaseValue(0); // OSRS zombies call no friends
        }
        osrsHitpoints(mob, def.hitpoints());
        mob.removeAllEffects();
        INSTALLED.put(mob, ai);
        return true;
    }

    /**
     * Gives a stand-in its OSRS hitpoints (round 43: vanilla's 8 for a wolf against OSRS 34, 12 for a cave spider against 2),
     * keeping the share of health it has (a reloaded mob already has them: its attributes are saved).
     */
    private static void osrsHitpoints(Mob mob, int hitpoints) {
        AttributeInstance max = mob.getAttribute(Attributes.MAX_HEALTH);
        if (max == null || hitpoints <= 0 || max.getBaseValue() == hitpoints && max.getModifiers().isEmpty()) {
            return;
        }
        float share = mob.getMaxHealth() > 0 ? mob.getHealth() / mob.getMaxHealth() : 1f;
        max.removeModifiers(); // a zombie leader's bonus health and the like
        max.setBaseValue(hitpoints);
        mob.setHealth(Math.max(1f, Math.min(hitpoints, share * hitpoints)));
    }

    /**
     * What every stand-in on OSRS AI does each tick besides fighting: no burning in daylight (OSRS zombies and skeletons don't; the
     * fire a vanilla zombie or skeleton sets on itself under the sun is put out before it hurts; lava still burns) and the OSRS
     * regeneration of a hitpoint a minute ({@link OsrsTargetGoal#regenerate}). No flags: it runs beside every other goal.
     */
    private static final class Upkeep extends Goal {
        private final Mob mob;

        Upkeep(Mob mob) {
            this.mob = mob;
        }

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return true;
        }

        @Override
        public boolean isInterruptable() {
            return false;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (mob.getRemainingFireTicks() > 0 && !mob.isInLava()) {
                mob.clearFire();
            }
            OsrsTargetGoal.regenerate(mob);
        }
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

    /**
     * An open-world stand-in follows OSRS aggression as the wiki gives it ({@link com.runecraft.osrs.data.MonsterAggression#aggressive(
     * Monster)}, round 43); an encounter spawn of a fighting kind attacks any player, as its vanilla AI did. A passive animal (a cow,
     * a chicken) never starts a fight, encounter or not: it only fights back.
     */
    @Override
    public boolean wantsToAttack(ServerPlayer player) {
        boolean encounter = encounter();
        boolean wiki = com.runecraft.osrs.data.MonsterAggression.aggressive(def);
        boolean animal = def == Monster.COW || def == Monster.CHICKEN || def == Monster.SHEEP || def == Monster.PIG;
        return OsrsTargetGoal.osrsAggression(mob, player, wiki || encounter && !animal, def.combatLevel(),
                com.runecraft.osrs.data.MonsterTraits.key(def), encounter);
    }

    @Override
    public double huntRange() {
        return MonsterHome.get(mob) != null ? MonsterAi.huntBlocks(def.name().toLowerCase(java.util.Locale.ROOT))
                : MonsterAi.ENCOUNTER_HUNT_BLOCKS;
    }
}
