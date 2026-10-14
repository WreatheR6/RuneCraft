package com.runecraft.boss;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.MonsterStats;
import com.runecraft.combat.PoisonService;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.CombatFormulas;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Executes {@link AttackDef}s for module monsters and data-driven bosses: accuracy rolls, protection
 * prayers, telegraphs, sky strikes, AoE, hazards, poison/venom, freezes, drains and more.
 */
public final class Mechanics {
    private record Pending(OsrsMonsterEntity mob, ServerPlayerEntity target, AttackDef attack, Vec3d marked, long dueMcTick,
                           double multiplier) {
    }

    private record Hazard(ServerWorld world, Vec3d pos, int damage, long untilGameTick, LivingEntity owner) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();
    private static final List<Hazard> HAZARDS = new ArrayList<>();

    private Mechanics() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = ActionManager.mcTick();
            Iterator<Pending> it = PENDING.iterator();
            List<Pending> due = new ArrayList<>();
            while (it.hasNext()) {
                Pending p = it.next();
                if (now >= p.dueMcTick()) {
                    due.add(p);
                    it.remove();
                }
            }
            for (Pending p : due) {
                if (p.mob().isAlive()) {
                    land(p.mob(), p.target(), p.attack(), p.marked(), p.multiplier());
                }
            }
            if (now % ActionManager.MC_TICKS_PER_GAME_TICK == 0) {
                tickHazards(ActionManager.gameTick());
            }
        });
    }

    /** Starts an attack; telegraphed attacks land later. {@code multiplier} scales the max hit (enrage phases). */
    /** Sees every attack a monster or boss performs (Combat Achievements count specials and repeats). */
    public interface AttackListener {
        void onAttack(OsrsMonsterEntity mob, ServerPlayerEntity target, AttackDef attack);
    }

    public static AttackListener attackListener = (mob, target, attack) -> {
    };

    public static void perform(OsrsMonsterEntity mob, ServerPlayerEntity target, AttackDef attack, double multiplier) {
        ServerWorld world = (ServerWorld) mob.getWorld();
        attackListener.onAttack(mob, target, attack);
        if (attack.message() != null) {
            for (ServerPlayerEntity p : world.getPlayers(pl -> pl.squaredDistanceTo(mob) < 40 * 40)) {
                Msg.action(p, attack.message());
            }
        }
        if (attack.melee()) {
            mob.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        }
        Vec3d marked = target.getPos();
        if (attack.special() == AttackDef.Special.SKY_STRIKE) {
            world.spawnParticles(ParticleTypes.FLAME, marked.x, marked.y + 0.1, marked.z, 30, 0.6, 0.05, 0.6, 0.01);
        }
        if (attack.telegraphTicks() > 0 || attack.special() == AttackDef.Special.SKY_STRIKE) {
            int ticks = Math.max(attack.telegraphTicks(), attack.special() == AttackDef.Special.SKY_STRIKE ? 3 : 0);
            world.spawnParticles(particle(attack, ParticleTypes.ENCHANTED_HIT), mob.getX(), mob.getY() + mob.getHeight(), mob.getZ(),
                    25, 0.5, 0.5, 0.5, 0.1);
            PENDING.add(new Pending(mob, target, attack, marked, ActionManager.mcTick() + (long) ticks * ActionManager.MC_TICKS_PER_GAME_TICK,
                    multiplier));
            return;
        }
        land(mob, target, attack, marked, multiplier);
    }

    private static ParticleEffect particle(AttackDef a, ParticleEffect fallback) {
        if (a.particle() == null) {
            return fallback;
        }
        var type = Registries.PARTICLE_TYPE.get(new Identifier(a.particle()));
        return type instanceof DefaultParticleType simple ? simple : fallback;
    }

    private static void trail(ServerWorld world, Vec3d from, Vec3d to, ParticleEffect effect) {
        Vec3d delta = to.subtract(from);
        int steps = (int) Math.max(4, delta.length() * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3d p = from.add(delta.multiply(i / (double) steps));
            world.spawnParticles(effect, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    private static void land(OsrsMonsterEntity mob, ServerPlayerEntity target, AttackDef a, Vec3d marked, double multiplier) {
        ServerWorld world = (ServerWorld) mob.getWorld();
        switch (a.special()) {
            case SUMMON -> {
                if (mob instanceof DataBossEntity boss) {
                    boss.summonMinions(Math.max(1, a.param()));
                }
                return;
            }
            case HEAL_SELF -> {
                mob.heal(a.param());
                world.spawnParticles(ParticleTypes.HEART, mob.getX(), mob.getY() + mob.getHeight(), mob.getZ(), 5, 0.5, 0.3, 0.5, 0);
                return;
            }
            case BLINK -> {
                mob.requestTeleport(target.getX() + 1, target.getY(), target.getZ() + 1);
                world.spawnParticles(ParticleTypes.PORTAL, mob.getX(), mob.getY() + 1, mob.getZ(), 40, 0.5, 1, 0.5, 0.3);
            }
            case AOE -> {
                world.spawnParticles(particle(a, ParticleTypes.EXPLOSION), mob.getX(), mob.getY() + 1, mob.getZ(), 6, a.param() / 2.0, 0.5,
                        a.param() / 2.0, 0);
                for (ServerPlayerEntity p : world.getPlayers(pl -> pl.isAlive() && !pl.isSpectator()
                        && pl.squaredDistanceTo(mob) <= a.param() * (double) a.param())) {
                    hit(mob, p, a, multiplier);
                }
                return;
            }
            case SKY_STRIKE -> {
                world.spawnParticles(particle(a, ParticleTypes.EXPLOSION), marked.x, marked.y + 0.5, marked.z, 3, 0.5, 0.5, 0.5, 0);
                world.playSound(null, marked.x, marked.y, marked.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.6f, 1.4f);
                for (ServerPlayerEntity p : world.getPlayers(pl -> pl.isAlive() && pl.getPos().squaredDistanceTo(marked) <= 2.25)) {
                    hit(mob, p, a, multiplier);
                }
                return;
            }
            default -> {
            }
        }
        if (!a.melee()) {
            trail(world, mob.getEyePos(), target.getPos().add(0, 1, 0), particle(a,
                    a.style() == AttackType.MAGIC ? ParticleTypes.WITCH : ParticleTypes.CRIT));
        }
        hit(mob, target, a, multiplier);
    }

    /** One accuracy-rolled hit with all on-hit effects. */
    private static void hit(OsrsMonsterEntity mob, ServerPlayerEntity target, AttackDef a, double multiplier) {
        MonsterStats stats = mob.stats();
        PlayerData data = Players.data(target);
        int attackLevel = switch (a.style()) {
            case MAGIC -> stats.magic();
            case RANGED -> stats.ranged();
            default -> stats.attack();
        };
        int maxHit = (int) Math.round(a.maxHit() * multiplier);
        if (a.special() == AttackDef.Special.DRAGONFIRE) {
            String off = Inv.idOf(target.getOffHandStack());
            boolean shield = off.equals("anti_dragon_shield") || off.startsWith("dragonfire_")
                    || off.equals("elemental_shield") || off.equals("mind_shield") || off.equals("ancient_wyvern_shield");
            boolean antifire = target.hasStatusEffect(StatusEffects.FIRE_RESISTANCE);
            boolean prayer = data.isActive(Prayer.PROTECT_FROM_MAGIC);
            if (shield && antifire) {
                maxHit = 0;
            } else if (shield || antifire) {
                maxHit = Math.min(maxHit, 10);
            } else if (prayer) {
                maxHit = Math.min(maxHit, maxHit * 2 / 3);
            } else {
                Msg.red(target, "You're horribly burnt by the dragonfire!");
            }
        }
        Random random = new Random(mob.getRandom().nextLong());
        long attackRoll = CombatHandler.npcAttackRoll(mob, stats, a.style(), attackLevel);
        long defenceRoll = CombatHandler.defenceRoll(target, a.style());
        boolean alwaysHits = a.special() == AttackDef.Special.DRAGONFIRE || a.special() == AttackDef.Special.AOE
                || a.special() == AttackDef.Special.SKY_STRIKE;
        int damage = alwaysHits ? random.nextInt(Math.max(0, maxHit) + 1) : CombatFormulas.roll(random, attackRoll, defenceRoll, maxHit);
        boolean protectedHit = com.runecraft.osrs.NpcHitCalc.protectedFrom(data.activePrayers(), a.style());
        if (protectedHit && a.special() != AttackDef.Special.PRAYER_PIERCING && a.special() != AttackDef.Special.DRAGONFIRE) {
            damage = 0;
        }
        damage = mob.onHitPlayer(target, damage, a.style());
        com.runecraft.combat.CombatHooks.nameAttack(a.name(), a.style());
        int dealt = CombatHandler.applyDamage(mob, target, damage);
        long tick = ActionManager.gameTick();
        switch (a.special()) {
            case POISON -> {
                if (dealt > 0) {
                    PoisonService.poison(target, Math.max(1, a.param()), tick);
                }
            }
            case VENOM -> {
                if (dealt > 0 || random.nextInt(4) == 0) {
                    PoisonService.venom(target, tick);
                }
            }
            case DRAIN_PRAYER -> {
                data.setPrayerPoints(data.prayerPoints() - Math.max(1, a.param()));
                ServerNetworking.sync(target);
            }
            case LIFESTEAL -> mob.heal(dealt * a.param() / 100f);
            case KNOCKBACK -> {
                Vec3d dir = target.getPos().subtract(mob.getPos()).normalize().multiply(Math.max(1, a.param()) * 0.5);
                target.addVelocity(dir.x, 0.4, dir.z);
                target.velocityModified = true;
            }
            case FREEZE -> {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, Math.max(1, a.param()) * 12, 10, false, true));
                Msg.action(target, "You have been frozen!");
            }
            case PULL -> target.requestTeleport(mob.getX() + 1, mob.getY(), mob.getZ());
            case HAZARD -> HAZARDS.add(new Hazard((ServerWorld) mob.getWorld(), target.getPos(), Math.max(1, a.param()), tick + 10, mob));
            case DRAIN_STAT -> {
                Skill skill = Skill.byIndex(Math.max(0, Math.min(Skill.count() - 1, a.param())));
                int drain = Math.max(1, data.skills().currentLevel(skill) / 10);
                data.skills().setBoost(skill, data.skills().boost(skill) - drain);
                ServerNetworking.sync(target);
            }
            default -> {
            }
        }
    }

    /** Places a damaging ground hazard (e.g. acid pool, lava splat) for a number of game ticks. */
    public static void addHazard(ServerWorld world, Vec3d pos, int damage, int gameTicks, LivingEntity owner) {
        HAZARDS.add(new Hazard(world, pos, damage, ActionManager.gameTick() + gameTicks, owner));
    }

    /** Removes every ground hazard inside a box (raid rooms and instances being cleared). */
    public static void clearHazards(ServerWorld world, net.minecraft.util.math.Box box) {
        HAZARDS.removeIf(h -> h.world() == world && box.contains(h.pos()));
    }

    private static void tickHazards(long gameTick) {
        Iterator<Hazard> it = HAZARDS.iterator();
        while (it.hasNext()) {
            Hazard h = it.next();
            if (gameTick > h.untilGameTick()) {
                it.remove();
                continue;
            }
            h.world().spawnParticles(ParticleTypes.SNEEZE, h.pos().x, h.pos().y + 0.1, h.pos().z, 6, 0.5, 0.02, 0.5, 0.0);
            for (ServerPlayerEntity p : h.world().getPlayers(pl -> pl.isAlive() && pl.getPos().squaredDistanceTo(h.pos()) < 1.0)) {
                com.runecraft.combat.CombatHooks.nameAttack("hazard", null);
                CombatHandler.applyDamage(h.owner() != null && h.owner().isAlive() ? h.owner() : null, p,
                        1 + p.getRandom().nextInt(h.damage()));
            }
        }
    }

    /** Picks a weighted attack usable at this distance (falls back to the closest-range attack). */
    public static AttackDef choose(List<AttackDef> attacks, double distance, net.minecraft.util.math.random.Random random) {
        int total = 0;
        for (AttackDef a : attacks) {
            if (distance <= a.range()) {
                total += a.weight();
            }
        }
        if (total <= 0) {
            AttackDef best = null;
            for (AttackDef a : attacks) {
                if (best == null || a.range() > best.range()) {
                    best = a;
                }
            }
            return best;
        }
        int r = random.nextInt(total);
        for (AttackDef a : attacks) {
            if (distance <= a.range()) {
                r -= a.weight();
                if (r < 0) {
                    return a;
                }
            }
        }
        return attacks.get(0);
    }
}
