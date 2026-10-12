package com.runecraft.content.systems;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.Equipment;
import com.runecraft.combat.MonsterStats;
import com.runecraft.combat.PoisonService;
import com.runecraft.combat.PrayerService;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.CombatFormulas;
import com.runecraft.osrs.CombatStyle;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/** Combat maths shared by special attacks, spells, jewellery and thralls. */
public final class SysCombat {
    /** Stat drains applied to non-player targets (levels). */
    public static final class Drain {
        public int defence;
        public int attack;
        public int strength;
        public int magic;
        public int ranged;
    }

    private static final Map<UUID, Drain> DRAINS = new ConcurrentHashMap<>();
    private static final Set<LivingEntity> POISONED = Collections.newSetFromMap(new WeakHashMap<>());

    private SysCombat() {
    }

    public static SkillSet skills(ServerPlayerEntity p) {
        return Players.data(p).skills();
    }

    /** Bonuses from items worn in the systems module's extra slots (requirements must be met). */
    public static EquipmentStats wornStats(ServerPlayerEntity p) {
        SysState.SysPlayer sp = SysState.of(p);
        SkillSet skills = skills(p);
        EquipmentStats total = EquipmentStats.NONE;
        for (ItemStack stack : sp.worn.values()) {
            if (stack.isEmpty()) {
                continue;
            }
            String id = com.runecraft.util.Inv.idOf(stack);
            com.runecraft.osrs.ItemDef def = ModItems.def(stack);
            if (def != null && def.equipment() != null && def.equipment().unmetRequirement(skills) != null) {
                continue;
            }
            Jewellery.Worn w = Jewellery.worn(id);
            if (w != null) {
                total = total.plus(w.stats());
            } else if (def != null && def.equipment() != null) {
                total = total.plus(def.equipment().stats());
            }
        }
        return total;
    }

    public static EquipmentStats coreStats(ServerPlayerEntity p) {
        return Equipment.total(p, skills(p));
    }

    // ---------------- Attack rolls (without hooks) ----------------

    public static long meleeAttackRoll(ServerPlayerEntity p, AttackType type) {
        PlayerData d = Players.data(p);
        CombatStyle style = d.style().forMelee();
        int eff = CombatFormulas.effectiveAttack(d.skills().currentLevel(Skill.ATTACK), PrayerService.attack(d), style);
        return CombatFormulas.attackRoll(eff, coreStats(p).attack(type));
    }

    public static int meleeMaxHit(ServerPlayerEntity p) {
        PlayerData d = Players.data(p);
        CombatStyle style = d.style().forMelee();
        int eff = CombatFormulas.effectiveStrength(d.skills().currentLevel(Skill.STRENGTH), PrayerService.strength(d), style);
        return CombatFormulas.meleeMaxHit(eff, coreStats(p).meleeStrength());
    }

    public static long rangedAttackRoll(ServerPlayerEntity p) {
        PlayerData d = Players.data(p);
        int eff = CombatFormulas.effectiveRanged(d.skills().currentLevel(Skill.RANGED), PrayerService.rangedAttack(d), d.style().forRanged());
        return CombatFormulas.attackRoll(eff, coreStats(p).rangedAttack());
    }

    public static int rangedMaxHit(ServerPlayerEntity p, int ammoStrength) {
        PlayerData d = Players.data(p);
        int eff = CombatFormulas.effectiveRanged(d.skills().currentLevel(Skill.RANGED), PrayerService.rangedStrength(d), d.style().forRanged());
        return CombatFormulas.rangedMaxHit(eff, ammoStrength + coreStats(p).rangedStrength());
    }

    public static long magicAttackRoll(ServerPlayerEntity p) {
        PlayerData d = Players.data(p);
        int eff = CombatFormulas.effectiveMagic(d.skills().currentLevel(Skill.MAGIC), PrayerService.magic(d));
        return CombatFormulas.attackRoll(eff, coreStats(p).magicAttack());
    }

    /** Ranged strength of the first arrow in the inventory (what a bow would fire). */
    public static int ammoStrength(ServerPlayerEntity p) {
        for (int i = 0; i < p.getInventory().size(); i++) {
            com.runecraft.osrs.ItemDef def = ModItems.def(p.getInventory().getStack(i));
            if (def != null && def.kind() == com.runecraft.osrs.ItemDef.Kind.ARROW) {
                return def.rangedStrength();
            }
        }
        return 0;
    }

    // ---------------- Defence rolls ----------------

    /** The core defence roll (no drains). */
    public static long baseDefenceRoll(LivingEntity target, AttackType type) {
        if (target instanceof ServerPlayerEntity defender) {
            PlayerData d = Players.data(defender);
            SkillSet s = d.skills();
            EquipmentStats eq = Equipment.total(defender, s).plus(wornStats(defender));
            int eff = type == AttackType.MAGIC
                    ? CombatFormulas.effectiveMagicDefence(s.currentLevel(Skill.MAGIC), PrayerService.magic(d),
                    s.currentLevel(Skill.DEFENCE), PrayerService.defence(d), d.style())
                    : CombatFormulas.effectiveDefence(s.currentLevel(Skill.DEFENCE), PrayerService.defence(d), d.style());
            return CombatFormulas.attackRoll(eff, eq.defence(type));
        }
        MonsterStats stats = MonsterStats.of(target);
        int level = type == AttackType.MAGIC ? stats.magic() : stats.defence();
        return CombatFormulas.npcRoll(level, stats.defenceBonus(type));
    }

    /** Defence roll after this module's stat drains. */
    public static long defenceRoll(LivingEntity target, AttackType type) {
        if (target instanceof ServerPlayerEntity) {
            return baseDefenceRoll(target, type);
        }
        Drain drain = DRAINS.get(target.getUuid());
        if (drain == null) {
            return baseDefenceRoll(target, type);
        }
        MonsterStats stats = MonsterStats.of(target);
        int level = type == AttackType.MAGIC ? Math.max(0, stats.magic() - drain.magic) : Math.max(0, stats.defence() - drain.defence);
        return CombatFormulas.npcRoll(level, stats.defenceBonus(type));
    }

    /** Accuracy multiplier equivalent to the target's drained defence (used in the attack modifier). */
    public static double drainAccuracy(LivingEntity target, AttackType type) {
        if (target instanceof ServerPlayerEntity || !DRAINS.containsKey(target.getUuid())) {
            return 1.0;
        }
        long base = baseDefenceRoll(target, type);
        long drained = defenceRoll(target, type);
        return drained <= 0 ? 2.0 : Math.min(4.0, base / (double) drained);
    }

    public static Drain drain(LivingEntity target) {
        return DRAINS.computeIfAbsent(target.getUuid(), u -> new Drain());
    }

    public static int defenceLevel(LivingEntity target) {
        if (target instanceof ServerPlayerEntity p) {
            return skills(p).currentLevel(Skill.DEFENCE);
        }
        Drain d = DRAINS.get(target.getUuid());
        return Math.max(0, MonsterStats.of(target).defence() - (d == null ? 0 : d.defence));
    }

    /** Lowers a target's Defence by a number of levels (players: a real drain). */
    public static void drainDefence(LivingEntity target, int levels) {
        drainStat(target, Skill.DEFENCE, levels);
    }

    public static void drainStat(LivingEntity target, Skill skill, int levels) {
        if (levels <= 0) {
            return;
        }
        if (target instanceof ServerPlayerEntity p) {
            SkillSet s = skills(p);
            int floor = -s.level(skill);
            s.setBoost(skill, Math.max(floor, s.boost(skill) - levels));
            com.runecraft.network.ServerNetworking.sync(p);
            return;
        }
        MonsterStats stats = MonsterStats.of(target);
        Drain d = drain(target);
        switch (skill) {
            case DEFENCE -> d.defence = Math.min(stats.defence(), d.defence + levels);
            case ATTACK -> d.attack = Math.min(stats.attack(), d.attack + levels);
            case STRENGTH -> d.strength = Math.min(stats.strength(), d.strength + levels);
            case MAGIC -> d.magic = Math.min(stats.magic(), d.magic + levels);
            case RANGED -> d.ranged = Math.min(stats.ranged(), d.ranged + levels);
            default -> {
            }
        }
    }

    /** A monster's attack level for a style after Attack/Magic/Ranged drains (players: unchanged). */
    public static int drainedAttackLevel(LivingEntity mob, AttackType type, int level) {
        if (mob instanceof ServerPlayerEntity) {
            return level;
        }
        Drain d = DRAINS.get(mob.getUuid());
        if (d == null) {
            return level;
        }
        int drain = switch (type) {
            case MAGIC -> d.magic;
            case RANGED -> d.ranged;
            default -> d.attack;
        };
        return Math.max(0, level - drain);
    }

    /** A target's level in a skill after drains (monsters: base level minus this module's drains). */
    public static int drainedStatLevel(LivingEntity target, Skill skill) {
        if (target instanceof ServerPlayerEntity p) {
            return skills(p).currentLevel(skill);
        }
        int base = statLevel(target, skill);
        Drain d = DRAINS.get(target.getUuid());
        if (d == null) {
            return base;
        }
        int drain = switch (skill) {
            case ATTACK -> d.attack;
            case STRENGTH -> d.strength;
            case MAGIC -> d.magic;
            case RANGED -> d.ranged;
            case DEFENCE -> d.defence;
            default -> 0;
        };
        return Math.max(0, base - drain);
    }

    /** A target's base (undrained) level in a skill. */
    public static int baseStatLevel(LivingEntity target, Skill skill) {
        if (target instanceof ServerPlayerEntity p) {
            return skills(p).level(skill);
        }
        return statLevel(target, skill);
    }

    public static int statLevel(LivingEntity target, Skill skill) {
        if (target instanceof ServerPlayerEntity p) {
            return skills(p).currentLevel(skill);
        }
        MonsterStats s = MonsterStats.of(target);
        return switch (skill) {
            case ATTACK -> s.attack();
            case STRENGTH -> s.strength();
            case MAGIC -> s.magic();
            case RANGED -> s.ranged();
            default -> s.defence();
        };
    }

    // ---------------- Effects ----------------

    /** Freezes (or binds) a target for a number of game ticks. */
    public static void freeze(LivingEntity target, int gameTicks) {
        int mcTicks = gameTicks * ActionManager.MC_TICKS_PER_GAME_TICK;
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, mcTicks, 6, false, true));
        target.setVelocity(Vec3d.ZERO);
        if (target instanceof MobEntity mob) {
            mob.getNavigation().stop();
        }
        dust(target, new Vector3f(0.6f, 0.85f, 1.0f), 20);
    }

    public static void poison(LivingEntity target, int damage) {
        PoisonService.poison(target, damage, ActionManager.gameTick());
        if (!(target instanceof ServerPlayerEntity) && !(target instanceof OsrsMonsterEntity)) {
            POISONED.add(target);
        }
    }

    /** Ticks poison on vanilla mobs this module poisoned (players and module mobs tick it themselves). */
    public static void gameTick(long gameTick) {
        for (LivingEntity e : new java.util.ArrayList<>(POISONED)) {
            if (!e.isAlive() || !PoisonService.isPoisoned(e)) {
                POISONED.remove(e);
                continue;
            }
            PoisonService.tick(e, gameTick);
        }
        if (gameTick % 100 == 0) {
            // Drains recover over time, like stats do (roughly one level a minute; simplified to a reset every minute).
            DRAINS.values().forEach(d -> {
                d.defence = Math.max(0, d.defence - 1);
                d.attack = Math.max(0, d.attack - 1);
                d.strength = Math.max(0, d.strength - 1);
                d.magic = Math.max(0, d.magic - 1);
                d.ranged = Math.max(0, d.ranged - 1);
            });
            DRAINS.values().removeIf(d -> d.defence == 0 && d.attack == 0 && d.strength == 0 && d.magic == 0 && d.ranged == 0);
        }
    }

    public static void forget(Entity entity) {
        DRAINS.remove(entity.getUuid());
    }

    public static void dust(Entity e, Vector3f color, int count) {
        if (e.getWorld() instanceof ServerWorld w) {
            w.spawnParticles(new DustParticleEffect(color, 1.2f), e.getX(), e.getY() + e.getHeight() / 2, e.getZ(), count,
                    0.4, 0.5, 0.4, 0.01);
        }
    }

    public static void trail(ServerWorld world, Vec3d from, Vec3d to, Vector3f color) {
        DustParticleEffect dust = new DustParticleEffect(color, 1.2f);
        Vec3d delta = to.subtract(from);
        int steps = (int) Math.max(4, delta.length() * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3d p = from.add(delta.multiply(i / (double) steps));
            world.spawnParticles(dust, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    // ---------------- Hits ----------------

    /** Rolls damage with the given rolls. */
    public static int roll(ServerPlayerEntity p, long attackRoll, long defenceRoll, int maxHit) {
        return CombatFormulas.roll(new java.util.Random(p.getRandom().nextLong()), attackRoll, defenceRoll, maxHit);
    }

    /** Applies a player's hit to a target, passing it through the target's own damage modifiers. */
    public static int hit(ServerPlayerEntity p, LivingEntity target, AttackType type, int damage) {
        if (target instanceof OsrsMonsterEntity monster) {
            damage = monster.modifyIncoming(p, damage, type);
        }
        SysState.SysPlayer sp = SysState.of(p);
        sp.lastCombatTick = ActionManager.gameTick();
        sp.lastTargetId = target.getId();
        damage = CombatHandler.protectionAgainstPlayer(target, type, damage);
        return CombatHandler.applyDamage(p, target, damage);
    }

    /** The living entity the player is looking at, within range. */
    public static LivingEntity lookTarget(ServerPlayerEntity player, double range) {
        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVector();
        Vec3d end = eye.add(look.multiply(range));
        Box box = player.getBoundingBox().stretch(look.multiply(range)).expand(1.0);
        EntityHitResult hit = ProjectileUtil.raycast(player, eye, end, box,
                e -> e instanceof LivingEntity && e.isAlive() && e != player && !e.isSpectator(), range * range);
        return hit != null && hit.getEntity() instanceof LivingEntity l ? l : null;
    }

    /** The player's current combat target: what they're looking at, or the last thing they hit. */
    public static LivingEntity currentTarget(ServerPlayerEntity player, double range) {
        LivingEntity look = lookTarget(player, range);
        if (look != null) {
            return look;
        }
        SysState.SysPlayer sp = SysState.of(player);
        if (sp.lastTargetId >= 0 && ActionManager.gameTick() - sp.lastCombatTick < 20) {
            Entity e = player.getWorld().getEntityById(sp.lastTargetId);
            if (e instanceof LivingEntity l && l.isAlive() && l.squaredDistanceTo(player) < range * range) {
                return l;
            }
        }
        LivingEntity attacker = player.getAttacker();
        if (attacker != null && attacker.isAlive() && attacker.squaredDistanceTo(player) < range * range) {
            return attacker;
        }
        return null;
    }

    /** Whether a target can be attacked by this player (Wilderness rules for players, single-way combat). */
    public static boolean canAttack(ServerPlayerEntity p, LivingEntity target) {
        if (target instanceof ServerPlayerEntity other && !CombatHandler.canAttackPlayer(p, other)) {
            return false;
        }
        if (target instanceof com.runecraft.entity.QuestNpcEntity) {
            return false;
        }
        return com.runecraft.combat.CombatZones.check(p, target) == com.runecraft.osrs.SingleCombat.Verdict.OK;
    }

    public static boolean isUndead(LivingEntity target) {
        if (target.isUndead()) {
            return true;
        }
        String n = MonsterStats.of(target).name().toLowerCase(java.util.Locale.ROOT);
        return n.contains("zombie") || n.contains("skeleton") || n.contains("ghost") || n.contains("ghoul") || n.contains("shade")
                || n.contains("revenant") || n.contains("vampyre") || n.contains("ankou") || n.contains("banshee");
    }

    public static boolean isDemon(LivingEntity target) {
        String n = MonsterStats.of(target).name().toLowerCase(java.util.Locale.ROOT);
        return n.contains("demon") || n.contains("imp") || n.contains("hellhound") || n.contains("k'ril") || n.contains("skotizo")
                || n.contains("nechryael") || n.contains("bloodveld") || n.contains("abyssal");
    }
}
