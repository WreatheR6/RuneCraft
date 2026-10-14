package com.runecraft.content.systems;

import com.runecraft.combat.CombatHandler;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.CombatFormulas;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.content.systems.SpecialAttacks;
import com.runecraft.osrs.content.systems.SpecialAttacks.Effect;
import com.runecraft.osrs.content.systems.SpecialAttacks.Spec;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Special attack energy and the special attacks themselves. A special is armed with the special attack
 * key; the next attack with a matching weapon consumes the energy and gets the weapon's effect. Effects are
 * applied through {@link SysModifier} (combat hooks) keyed by the wielded item id.
 */
public final class SpecialAttackService {
    /** A special attack in progress for one attack. */
    static final class Active {
        final Spec spec;
        final LivingEntity target;
        int[] clawHits;

        Active(Spec spec, LivingEntity target) {
            this.spec = spec;
            this.target = target;
        }
    }

    private static final Map<UUID, Active> ACTIVE = new ConcurrentHashMap<>();
    /** Set while this module rolls a follow-up hit, so hooks don't start or apply specials again. */
    static boolean secondary;

    private SpecialAttackService() {
    }

    public static Spec weaponSpec(ServerPlayerEntity p) {
        if (p.getMainHandStack().isEmpty()) {
            return null;
        }
        return SpecialAttacks.of(Inv.idOf(p.getMainHandStack()));
    }

    /** The special attack key / orb. */
    public static void toggle(ServerPlayerEntity p) {
        SysState.SysPlayer sp = SysState.of(p);
        Spec spec = weaponSpec(p);
        if (spec == null) {
            sp.specArmed = false;
            Msg.game(p, "Your weapon doesn't have a special attack.");
            SysNet.sendState(p);
            return;
        }
        if (sp.specArmed) {
            sp.specArmed = false;
            SysNet.sendState(p);
            return;
        }
        if (sp.specEnergy < spec.cost()) {
            Msg.game(p, "You do not have enough special attack energy left.");
            return;
        }
        if (spec.style() == SpecialAttacks.Style.INSTANT) {
            if (instant(p, spec)) {
                spend(p, spec);
            }
            SysNet.sendState(p);
            return;
        }
        sp.specArmed = true;
        Msg.action(p, spec.name() + ": " + spec.description());
        SysNet.sendState(p);
    }

    static void spend(ServerPlayerEntity p, Spec spec) {
        SysState.SysPlayer sp = SysState.of(p);
        sp.specEnergy = Math.max(0, sp.specEnergy - spec.cost());
        sp.specArmed = false;
        p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.8f, 0.7f);
        SysNet.sendState(p);
    }

    public static void restoreEnergy(ServerPlayerEntity p, int amount) {
        SysState.SysPlayer sp = SysState.of(p);
        sp.specEnergy = Math.min(SpecialAttacks.MAX_ENERGY, sp.specEnergy + amount);
        SysNet.sendState(p);
    }

    /** Called every game tick: energy regeneration (10% every 30 seconds). */
    public static void gameTick(ServerPlayerEntity p, long tick) {
        SysState.SysPlayer sp = SysState.of(p);
        if (sp.specEnergy < SpecialAttacks.MAX_ENERGY) {
            int before = sp.specEnergy;
            sp.specEnergy = SpecialAttacks.regen(sp.specEnergy, sp.wearing("lightbearer"), tick);
            if (sp.specEnergy != before) {
                SysNet.sendState(p);
            }
        }
        if (sp.specArmed) {
            Spec spec = weaponSpec(p);
            if (spec == null || spec.style() == SpecialAttacks.Style.INSTANT) {
                sp.specArmed = false;
                SysNet.sendState(p);
            }
        }
    }

    // ---------------- Hook plumbing ----------------

    /** Starts a special for this attack if one is armed and matches the attack style. */
    static Active begin(ServerPlayerEntity p, LivingEntity target, AttackType type) {
        if (secondary) {
            return null;
        }
        SysState.SysPlayer sp = SysState.of(p);
        if (!sp.specArmed) {
            return null;
        }
        Spec spec = weaponSpec(p);
        if (spec == null) {
            return null;
        }
        boolean matches = switch (spec.style()) {
            case MELEE -> type.isMelee();
            case RANGED -> type == AttackType.RANGED;
            case MAGIC -> type == AttackType.MAGIC;
            case INSTANT -> false;
        };
        if (!matches || sp.specEnergy < spec.cost()) {
            return null;
        }
        spend(p, spec);
        Active a = new Active(spec, target);
        ACTIVE.put(p.getUuid(), a);
        if (p.getWorld() instanceof ServerWorld w) {
            w.spawnParticles(net.minecraft.particle.ParticleTypes.CRIT, target.getX(), target.getY() + target.getHeight() / 2,
                    target.getZ(), 15, 0.3, 0.3, 0.3, 0.2);
        }
        return a;
    }

    static Active active(ServerPlayerEntity p) {
        return secondary ? null : ACTIVE.get(p.getUuid());
    }

    static double accuracy(ServerPlayerEntity p, LivingEntity target, AttackType type) {
        if (secondary) {
            return 1.0;
        }
        Active a = ACTIVE.get(p.getUuid());
        if (a == null || a.target != target) {
            a = begin(p, target, type);
        }
        if (a == null) {
            return 1.0;
        }
        double m = a.spec.accuracy();
        if (a.spec.effect() == Effect.GUARANTEED || a.spec.effect() == Effect.VOIDWAKER) {
            m *= 1000;
        }
        return m;
    }

    static double maxHit(ServerPlayerEntity p, LivingEntity target, AttackType type) {
        Active a = active(p);
        if (a == null) {
            return 1.0;
        }
        double m = a.spec.damage();
        if (a.spec.effect() == Effect.PENANCE) {
            PlayerData d = Players.data(p);
            m *= SpecialAttacks.penance(d.skills().level(Skill.PRAYER), d.prayerPoints());
        }
        if (a.spec.effect() == Effect.SHOVE) {
            return 0.0;
        }
        return m;
    }

    static int afterRoll(ServerPlayerEntity p, LivingEntity target, AttackType type, int damage) {
        Active a = active(p);
        if (a == null) {
            return damage;
        }
        Random r = new Random(p.getRandom().nextLong());
        switch (a.spec.effect()) {
            case CLAWS -> {
                int max = baseMax(p, target, type);
                long att = (long) baseAttack(p, target, type);
                long def = SysCombat.defenceRoll(target, type);
                boolean[] hits = new boolean[4];
                double chance = com.runecraft.osrs.Chance.hitChance(att, def);
                for (int i = 0; i < 4; i++) {
                    hits[i] = r.nextDouble() < chance;
                }
                a.clawHits = SpecialAttacks.clawHits(r, hits, max);
                return a.clawHits[0];
            }
            case VOIDWAKER -> {
                int max = baseMax(p, target, type);
                return max / 2 + r.nextInt(Math.max(1, max) + 1);
            }
            case DARK_BOW -> {
                int min = Inv.idOf(p.getOffHandStack()).contains("dragon_arrow") || hasDragonArrows(p) ? 8 : a.spec.param();
                return Math.max(min, damage);
            }
            case SHOVE -> {
                return 0;
            }
            default -> {
                return damage;
            }
        }
    }

    private static boolean hasDragonArrows(ServerPlayerEntity p) {
        return Inv.count(p, "dragon_arrow") > 0;
    }

    static void afterHit(ServerPlayerEntity p, LivingEntity target, AttackType type, int dealt) {
        Active a = secondary ? null : ACTIVE.remove(p.getUuid());
        if (a == null) {
            return;
        }
        Spec spec = a.spec;
        PlayerData data = Players.data(p);
        switch (spec.effect()) {
            case DOUBLE_HIT -> {
                if (!spec.itemId().startsWith("abyssal_dagger") || dealt > 0) {
                    followUp(p, target, type, spec.accuracy(), spec.damage(), 0);
                }
            }
            case CLAWS -> {
                int[] h = a.clawHits;
                if (h != null) {
                    fixedHit(p, target, type, h[1], 0);
                    fixedHit(p, target, type, h[2], 1);
                    fixedHit(p, target, type, h[3], 1);
                }
            }
            case DRAIN_DEFENCE_PERCENT -> {
                if (dealt > 0) {
                    int def = SysCombat.defenceLevel(target);
                    SysCombat.drainDefence(target, (int) Math.ceil(def * spec.param() / 100.0));
                    SysCombat.dust(target, new Vector3f(0.5f, 0.5f, 0.5f), 15);
                }
            }
            case DRAIN_BY_DAMAGE -> {
                int remaining = dealt;
                for (Skill s : new Skill[]{Skill.DEFENCE, Skill.STRENGTH, Skill.ATTACK, Skill.MAGIC, Skill.RANGED}) {
                    if (remaining <= 0) {
                        break;
                    }
                    int lvl = SysCombat.statLevel(target, s);
                    int d = Math.min(remaining, lvl);
                    SysCombat.drainStat(target, s, d);
                    remaining -= d;
                }
            }
            case HEAL_AND_PRAY -> {
                int[] hp = SpecialAttacks.sgsRestore(dealt);
                p.heal(hp[0]);
                data.setPrayerPoints(data.prayerPoints() + hp[1]);
                ServerNetworking.sync(p);
            }
            case FREEZE -> {
                if (dealt > 0) {
                    SysCombat.freeze(target, spec.param());
                    if (spec.itemId().equals("abyssal_tentacle") && p.getRandom().nextInt(4) == 0) {
                        SysCombat.poison(target, 4);
                    }
                }
            }
            case HEAL_PERCENT -> p.heal(dealt * spec.param() / 100f);
            case WEAKEN -> {
                if (dealt > 0) {
                    int pct = SysCombat.isDemon(target) ? spec.param() * 2 : spec.param();
                    for (Skill s : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE}) {
                        SysCombat.drainStat(target, s, Math.max(1, SysCombat.statLevel(target, s) * pct / 100));
                    }
                }
            }
            case AREA -> {
                for (LivingEntity other : p.getWorld().getEntitiesByClass(LivingEntity.class, target.getBoundingBox().expand(2.5),
                        e -> e != p && e != target && e.isAlive() && SysCombat.canAttack(p, e))) {
                    followUp(p, other, type, spec.accuracy(), spec.damage(), 0);
                }
            }
            case SHOVE -> {
                SysCombat.freeze(target, spec.param());
                Vec3d d = target.getPos().subtract(p.getPos()).normalize();
                target.takeKnockback(1.2, -d.x, -d.z);
            }
            case LIGHTNING -> {
                int extra = 1 + p.getRandom().nextInt(spec.param());
                SysTicks.later(0, () -> {
                    if (target.isAlive()) {
                        int done = SysCombat.hit(p, target, AttackType.MAGIC, extra);
                        CombatHandler.awardXp(p, data.style().forMelee(), done, false);
                    }
                });
                SysCombat.dust(target, new Vector3f(0.9f, 0.9f, 1.0f), 25);
            }
            case DOUBLE_SHOT, DARK_BOW -> SysTicks.later(1, () -> {
                if (target.isAlive()) {
                    secondaryRanged(p, target, spec);
                }
            });
            default -> {
            }
        }
        // Weapon-specific extras.
        String id = spec.itemId();
        if (id.startsWith("dragon_scimitar") && target instanceof ServerPlayerEntity victim && dealt > 0) {
            PlayerData vd = Players.data(victim);
            vd.activePrayers().removeIf(pr -> pr == Prayer.PROTECT_FROM_MELEE || pr == Prayer.PROTECT_FROM_MISSILES
                    || pr == Prayer.PROTECT_FROM_MAGIC);
            Msg.red(victim, "You have been injured and can't use protection prayers!");
            ServerNetworking.sync(victim);
        }
        if (id.contains("abyssal_whip") && target instanceof ServerPlayerEntity victim && dealt > 0) {
            SysState.SysPlayer vs = SysState.of(victim);
            int take = vs.runEnergy / 10;
            vs.runEnergy -= take;
            SysState.SysPlayer mine = SysState.of(p);
            mine.runEnergy = Math.min(10000, mine.runEnergy + take);
            SysNet.sendState(victim);
            SysNet.sendState(p);
        }
    }

    /** Attack roll with the player's gear effects (PlayerHitCalc) but without the other combat hooks. */
    private static double baseAttack(ServerPlayerEntity p, LivingEntity target, AttackType type) {
        return com.runecraft.osrs.PlayerHitCalc.attackRoll(hitInput(p, target, type));
    }

    private static int baseMax(ServerPlayerEntity p, LivingEntity target, AttackType type) {
        return com.runecraft.osrs.PlayerHitCalc.maxHit(hitInput(p, target, type));
    }

    private static com.runecraft.osrs.PlayerHitCalc.Input hitInput(ServerPlayerEntity p, LivingEntity target, AttackType type) {
        return com.runecraft.combat.HitCalcBridge.input(p, target, type, null, type == AttackType.RANGED ? SysCombat.ammoStrength(p) : 0);
    }

    /** Rolls and applies an additional hit with the given multipliers (through the other combat hooks). */
    static void followUp(ServerPlayerEntity p, LivingEntity target, AttackType type, double acc, double dmg, int delay) {
        Runnable run = () -> {
            if (!target.isAlive() || !p.isAlive()) {
                return;
            }
            secondary = true;
            try {
                com.runecraft.osrs.PlayerHitCalc.Input in = hitInput(p, target, type);
                long att = (long) (com.runecraft.osrs.PlayerHitCalc.attackRoll(in) * acc * CombatHandler.hookAccuracy(p, target, type));
                long def = com.runecraft.combat.HitCalcBridge.defenceRoll(target, in);
                int max = (int) Math.floor(com.runecraft.osrs.PlayerHitCalc.maxHit(in) * dmg * CombatHandler.hookMaxHit(p, target, type));
                int damage = SysCombat.roll(p, att, def, max);
                damage = CombatHandler.hookAfterRoll(p, target, type, damage);
                int done = SysCombat.hit(p, target, type, damage);
                CombatHandler.awardXp(p, type == AttackType.RANGED ? Players.data(p).style().forRanged() : Players.data(p).style().forMelee(),
                        done, false);
            } finally {
                secondary = false;
            }
        };
        if (delay <= 0) {
            run.run();
        } else {
            SysTicks.later(delay, run);
        }
    }

    private static void fixedHit(ServerPlayerEntity p, LivingEntity target, AttackType type, int damage, int delay) {
        Runnable run = () -> {
            if (!target.isAlive()) {
                return;
            }
            int done = SysCombat.hit(p, target, type, damage);
            CombatHandler.awardXp(p, Players.data(p).style().forMelee(), done, false);
        };
        if (delay <= 0) {
            run.run();
        } else {
            SysTicks.later(delay, run);
        }
    }

    private static void secondaryRanged(ServerPlayerEntity p, LivingEntity target, Spec spec) {
        if (p.getWorld() instanceof ServerWorld w) {
            SysCombat.trail(w, p.getEyePos(), target.getPos().add(0, target.getHeight() / 2, 0), new Vector3f(0.3f, 0.2f, 0.4f));
        }
        secondary = true;
        try {
            com.runecraft.osrs.PlayerHitCalc.Input in = hitInput(p, target, AttackType.RANGED);
            long att = (long) (com.runecraft.osrs.PlayerHitCalc.attackRoll(in) * spec.accuracy() * CombatHandler.hookAccuracy(p, target, AttackType.RANGED));
            long def = com.runecraft.combat.HitCalcBridge.defenceRoll(target, in);
            int max = (int) Math.floor(com.runecraft.osrs.PlayerHitCalc.maxHit(in) * spec.damage()
                    * CombatHandler.hookMaxHit(p, target, AttackType.RANGED));
            int damage = SysCombat.roll(p, att, def, Math.max(1, max));
            if (spec.effect() == Effect.DARK_BOW) {
                damage = Math.max(hasDragonArrows(p) ? 8 : spec.param(), damage);
            }
            int done = SysCombat.hit(p, target, AttackType.RANGED, damage);
            CombatHandler.awardXp(p, Players.data(p).style().forRanged(), done, false);
        } finally {
            secondary = false;
        }
    }

    // ---------------- Instant specials ----------------

    private static boolean instant(ServerPlayerEntity p, Spec spec) {
        PlayerData data = Players.data(p);
        SkillSet s = data.skills();
        switch (spec.effect()) {
            case RAMPAGE -> {
                int att = s.currentLevel(Skill.ATTACK);
                int def = s.currentLevel(Skill.DEFENCE);
                int mag = s.currentLevel(Skill.MAGIC);
                int rng = s.currentLevel(Skill.RANGED);
                s.setBoost(Skill.ATTACK, s.boost(Skill.ATTACK) - att / 10);
                s.setBoost(Skill.DEFENCE, s.boost(Skill.DEFENCE) - def / 10);
                s.setBoost(Skill.MAGIC, s.boost(Skill.MAGIC) - mag / 10);
                s.setBoost(Skill.RANGED, s.boost(Skill.RANGED) - rng / 10);
                int boost = SpecialAttacks.rampageBoost(att, def, mag, rng);
                s.setBoost(Skill.STRENGTH, Math.max(s.boost(Skill.STRENGTH), Math.min(boost, s.boost(Skill.STRENGTH) + boost)));
                p.swingHand(Hand.MAIN_HAND, true);
                Msg.action(p, "Raarrrrrgggggghhhhhhh!");
                ServerNetworking.sync(p);
                return true;
            }
            case SKILL_BOOST -> {
                Skill skill = Skill.valueOf(spec.skill());
                s.applyBoost(skill, spec.param(), 0);
                Msg.action(p, spec.name() + "! Your " + skill.displayName() + " level is boosted.");
                ServerNetworking.sync(p);
                return true;
            }
            case QUICK_SMASH -> {
                LivingEntity t = SysCombat.currentTarget(p, 5);
                if (t == null || !SysCombat.canAttack(p, t)) {
                    Msg.game(p, "You need a target in reach to use Quick Smash.");
                    return false;
                }
                p.swingHand(Hand.MAIN_HAND, true);
                followUp(p, t, com.runecraft.combat.Equipment.meleeType(p, s), 1.0, 1.0, 0);
                return true;
            }
            case POWER_OF_DEATH -> {
                SysState.of(p).powerOfDeathUntil = SysTicks.now() + spec.param();
                SysCombat.dust(p, new Vector3f(0.6f, 0.2f, 0.8f), 30);
                Msg.action(p, "Spirits of deceased evildoers offer you their protection.");
                return true;
            }
            case MAGIC_BLAST, BLAST_PRAYER -> {
                LivingEntity t = SysCombat.currentTarget(p, 16);
                if (t == null || !SysCombat.canAttack(p, t)) {
                    Msg.game(p, "You need a target to use this special attack.");
                    return false;
                }
                int magic = Math.min(99, s.currentLevel(Skill.MAGIC));
                int max = Math.max(1, spec.param() * magic / 99);
                // The blast is a spell with this base max hit: gear effects, magic damage and the target's defence come
                // from PlayerHitCalc through HitCalcBridge; the spec adds +50% accuracy.
                com.runecraft.osrs.PlayerHitCalc.Input in = com.runecraft.combat.HitCalcBridge.input(p, t, AttackType.MAGIC, null, 0);
                com.runecraft.combat.HitCalcBridge.spell(in, max, false);
                long att = (long) (com.runecraft.combat.HitCalcBridge.attackRoll(p, t, in) * 1.5);
                long def = com.runecraft.combat.HitCalcBridge.defenceRoll(t, in);
                int dmg = SysCombat.roll(p, att, def, com.runecraft.combat.HitCalcBridge.maxHit(p, t, in));
                if (p.getWorld() instanceof ServerWorld w) {
                    SysCombat.trail(w, p.getEyePos(), t.getPos().add(0, t.getHeight() / 2, 0), new Vector3f(0.6f, 0.1f, 0.6f));
                }
                p.swingHand(Hand.MAIN_HAND, true);
                int done = SysCombat.hit(p, t, AttackType.MAGIC, dmg);
                com.runecraft.skill.XpService.addTenths(p, Skill.MAGIC, done * 20);
                com.runecraft.skill.XpService.addTenths(p, Skill.HITPOINTS, CombatFormulas.hitpointsXpTenths(done));
                if (spec.effect() == Effect.BLAST_PRAYER) {
                    data.setPrayerPoints(data.prayerPoints() + done / 2);
                    ServerNetworking.sync(p);
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
