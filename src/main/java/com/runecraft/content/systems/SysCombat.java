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
import org.joml.Vector3f;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

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

    public static SkillSet skills(ServerPlayer p) {
        return Players.data(p).skills();
    }

    /** Bonuses from items worn in the systems module's extra slots (requirements must be met). */
    public static EquipmentStats wornStats(ServerPlayer p) {
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

    public static EquipmentStats coreStats(ServerPlayer p) {
        return Equipment.total(p, skills(p));
    }

    /** Armour, weapon and shield plus the extra slots: the totals every attack roll and max hit uses. */
    public static EquipmentStats allStats(ServerPlayer p) {
        return coreStats(p).plus(wornStats(p));
    }

    // ---------------- Attack rolls (without hooks; gear effects are in PlayerHitCalc) ----------------

    public static long meleeAttackRoll(ServerPlayer p, AttackType type) {
        PlayerData d = Players.data(p);
        CombatStyle style = d.style().forMelee();
        int eff = CombatFormulas.effectiveAttack(d.skills().currentLevel(Skill.ATTACK), PrayerService.attack(d), style);
        return CombatFormulas.attackRoll(eff, allStats(p).attack(type));
    }

    public static int meleeMaxHit(ServerPlayer p) {
        PlayerData d = Players.data(p);
        CombatStyle style = d.style().forMelee();
        int eff = CombatFormulas.effectiveStrength(d.skills().currentLevel(Skill.STRENGTH), PrayerService.strength(d), style);
        return CombatFormulas.meleeMaxHit(eff, allStats(p).meleeStrength());
    }

    public static long rangedAttackRoll(ServerPlayer p) {
        PlayerData d = Players.data(p);
        int eff = CombatFormulas.effectiveRanged(d.skills().currentLevel(Skill.RANGED), PrayerService.rangedAttack(d), d.style().forRanged());
        return CombatFormulas.attackRoll(eff, allStats(p).rangedAttack());
    }

    public static int rangedMaxHit(ServerPlayer p, int ammoStrength) {
        PlayerData d = Players.data(p);
        int eff = CombatFormulas.effectiveRanged(d.skills().currentLevel(Skill.RANGED), PrayerService.rangedStrength(d), d.style().forRanged());
        return CombatFormulas.rangedMaxHit(eff, ammoStrength + allStats(p).rangedStrength());
    }

    public static long magicAttackRoll(ServerPlayer p) {
        PlayerData d = Players.data(p);
        int eff = CombatFormulas.effectiveMagic(d.skills().currentLevel(Skill.MAGIC), PrayerService.magic(d));
        return CombatFormulas.attackRoll(eff, allStats(p).magicAttack());
    }

    /** Ranged strength of the first arrow in the inventory (what a bow would fire). */
    public static int ammoStrength(ServerPlayer p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            com.runecraft.osrs.ItemDef def = ModItems.def(p.getInventory().getItem(i));
            if (def != null && def.kind() == com.runecraft.osrs.ItemDef.Kind.ARROW) {
                return def.rangedStrength();
            }
        }
        return 0;
    }

    // ---------------- Defence rolls ----------------

    /** The core defence roll (no drains). */
    public static long baseDefenceRoll(LivingEntity target, AttackType type) {
        if (target instanceof ServerPlayer defender) {
            return com.runecraft.combat.HitCalcBridge.playerDefenceRoll(defender, type);
        }
        MonsterStats stats = MonsterStats.of(target);
        int level = type == AttackType.MAGIC ? stats.magic() : stats.defence();
        return CombatFormulas.npcRoll(level, stats.defenceBonus(type));
    }

    /** Defence roll after this module's stat drains. */
    public static long defenceRoll(LivingEntity target, AttackType type) {
        if (target instanceof ServerPlayer) {
            return baseDefenceRoll(target, type);
        }
        Drain drain = DRAINS.get(target.getUUID());
        if (drain == null) {
            return baseDefenceRoll(target, type);
        }
        MonsterStats stats = MonsterStats.of(target);
        int level = type == AttackType.MAGIC ? Math.max(0, stats.magic() - drain.magic) : Math.max(0, stats.defence() - drain.defence);
        return CombatFormulas.npcRoll(level, stats.defenceBonus(type));
    }

    /** Accuracy multiplier equivalent to the target's drained defence (used in the attack modifier). */
    public static double drainAccuracy(LivingEntity target, AttackType type) {
        if (target instanceof ServerPlayer || !DRAINS.containsKey(target.getUUID())) {
            return 1.0;
        }
        long base = baseDefenceRoll(target, type);
        long drained = defenceRoll(target, type);
        return drained <= 0 ? 2.0 : Math.min(4.0, base / (double) drained);
    }

    public static Drain drain(LivingEntity target) {
        return DRAINS.computeIfAbsent(target.getUUID(), u -> new Drain());
    }

    public static int defenceLevel(LivingEntity target) {
        if (target instanceof ServerPlayer p) {
            return skills(p).currentLevel(Skill.DEFENCE);
        }
        Drain d = DRAINS.get(target.getUUID());
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
        if (target instanceof ServerPlayer p) {
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
        if (mob instanceof ServerPlayer) {
            return level;
        }
        Drain d = DRAINS.get(mob.getUUID());
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
        if (target instanceof ServerPlayer p) {
            return skills(p).currentLevel(skill);
        }
        int base = statLevel(target, skill);
        Drain d = DRAINS.get(target.getUUID());
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
        if (target instanceof ServerPlayer p) {
            return skills(p).level(skill);
        }
        return statLevel(target, skill);
    }

    public static int statLevel(LivingEntity target, Skill skill) {
        if (target instanceof ServerPlayer p) {
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
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, mcTicks, 6, false, true));
        target.setDeltaMovement(Vec3.ZERO);
        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        dust(target, new Vector3f(0.6f, 0.85f, 1.0f), 20);
    }

    public static void poison(LivingEntity target, int damage) {
        PoisonService.poison(target, damage, ActionManager.gameTick());
        if (!(target instanceof ServerPlayer) && !(target instanceof OsrsMonsterEntity)) {
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
        DRAINS.remove(entity.getUUID());
    }

    public static void dust(Entity e, Vector3f color, int count) {
        if (e.level() instanceof ServerLevel w) {
            w.sendParticles(new DustParticleOptions(color, 1.2f), e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), count,
                    0.4, 0.5, 0.4, 0.01);
        }
    }

    public static void trail(ServerLevel world, Vec3 from, Vec3 to, Vector3f color) {
        DustParticleOptions dust = new DustParticleOptions(color, 1.2f);
        Vec3 delta = to.subtract(from);
        int steps = (int) Math.max(4, delta.length() * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(delta.scale(i / (double) steps));
            world.sendParticles(dust, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    // ---------------- Hits ----------------

    /** Rolls damage with the given rolls. */
    public static int roll(ServerPlayer p, long attackRoll, long defenceRoll, int maxHit) {
        return CombatFormulas.roll(new java.util.Random(p.getRandom().nextLong()), attackRoll, defenceRoll, maxHit);
    }

    /** Applies a player's hit to a target, passing it through the target's own damage modifiers. */
    public static int hit(ServerPlayer p, LivingEntity target, AttackType type, int damage) {
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
    public static LivingEntity lookTarget(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
                e -> e instanceof LivingEntity && e.isAlive() && e != player && !e.isSpectator(), range * range);
        return hit != null && hit.getEntity() instanceof LivingEntity l ? l : null;
    }

    /** The player's current combat target: what they're looking at, or the last thing they hit. */
    public static LivingEntity currentTarget(ServerPlayer player, double range) {
        LivingEntity look = lookTarget(player, range);
        if (look != null) {
            return look;
        }
        SysState.SysPlayer sp = SysState.of(player);
        if (sp.lastTargetId >= 0 && ActionManager.gameTick() - sp.lastCombatTick < 20) {
            Entity e = player.level().getEntity(sp.lastTargetId);
            if (e instanceof LivingEntity l && l.isAlive() && l.distanceToSqr(player) < range * range) {
                return l;
            }
        }
        LivingEntity attacker = player.getLastHurtByMob();
        if (attacker != null && attacker.isAlive() && attacker.distanceToSqr(player) < range * range) {
            return attacker;
        }
        return null;
    }

    /** Whether a target can be attacked by this player (Wilderness rules for players, single-way combat). */
    public static boolean canAttack(ServerPlayer p, LivingEntity target) {
        if (target instanceof ServerPlayer other && !CombatHandler.canAttackPlayer(p, other)) {
            return false;
        }
        if (target instanceof com.runecraft.entity.QuestNpcEntity) {
            return false;
        }
        return com.runecraft.combat.CombatZones.check(p, target) == com.runecraft.osrs.SingleCombat.Verdict.OK;
    }

    public static boolean isUndead(LivingEntity target) {
        if (target.isInvertedHealAndHarm()) {
            return true;
        }
        MonsterStats stats = MonsterStats.of(target);
        if (stats.knowsAttributes()) {
            return stats.attributes().contains("undead");
        }
        String n = stats.name().toLowerCase(java.util.Locale.ROOT);
        return n.contains("zombie") || n.contains("skeleton") || n.contains("ghost") || n.contains("ghoul") || n.contains("shade")
                || n.contains("revenant") || n.contains("vampyre") || n.contains("ankou") || n.contains("banshee");
    }

    public static boolean isDemon(LivingEntity target) {
        MonsterStats stats = MonsterStats.of(target);
        if (stats.knowsAttributes()) {
            return stats.attributes().contains("demon");
        }
        String n = stats.name().toLowerCase(java.util.Locale.ROOT);
        return n.contains("demon") || n.contains("imp") || n.contains("hellhound") || n.contains("k'ril") || n.contains("skotizo")
                || n.contains("nechryael") || n.contains("bloodveld") || n.contains("abyssal");
    }
}
