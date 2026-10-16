package com.runecraft.content.systems;

import com.runecraft.combat.CombatHandler;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.CombatFormulas;
import com.runecraft.osrs.PlayerHitCalc;
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
import net.minecraft.entity.EquipmentSlot;
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
        /** The first successful accuracy roll of a claws special (burning claws' burn chance), -1 for none. */
        int firstSuccess = -1;

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

    /** Rules that change a special attack's energy cost (e.g. ToA's Overly Draining); return -1 to keep it. */
    public interface CostRule {
        int cost(ServerPlayerEntity p, Spec spec);
    }

    private static final java.util.List<CostRule> COST_RULES = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void addCostRule(CostRule rule) {
        COST_RULES.add(rule);
    }

    /** The energy this player's special attack costs (the weapon's cost unless a {@link CostRule} changes it). */
    public static int cost(ServerPlayerEntity p, Spec spec) {
        for (CostRule r : COST_RULES) {
            int c = r.cost(p, spec);
            if (c >= 0) {
                return c;
            }
        }
        return spec.cost();
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
        if (sp.specEnergy < cost(p, spec)) {
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
        sp.specEnergy = Math.max(0, sp.specEnergy - cost(p, spec));
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
            sp.specEnergy = SpecialAttacks.regen(sp.specEnergy, sp.wearing("lightbearer") || sp.wearing("lms_lightbearer"), tick);
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

    /** Set while a follow-up hit of a special attack (second dagger stab, dark bow arrow, instant blast) builds its input. */
    static boolean forceSpecial;

    /** True when the player's {@code type} attack being rolled now is their special attack (HitCalcBridge.specialAttack). */
    public static boolean isSpecial(ServerPlayerEntity p, AttackType type) {
        if (forceSpecial) {
            return true;
        }
        if (secondary) {
            return false;
        }
        if (ACTIVE.containsKey(p.getUuid())) {
            return true;
        }
        return startable(p, type) != null;
    }

    /** The armed special that an attack of this style would start now (matching style, enough energy, its requirements), or null. */
    private static Spec startable(ServerPlayerEntity p, AttackType type) {
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
        if (!matches || sp.specEnergy < cost(p, spec)) {
            return null;
        }
        if (spec.effect() == Effect.BEHEAD && sp.soulStacks <= 0) {
            return null;
        }
        if (spec.effect() == Effect.BLOOD_INFUSION && !bloodMoonSet(p)) {
            return null;
        }
        return spec;
    }

    private static boolean bloodMoonSet(ServerPlayerEntity p) {
        return Inv.idOf(p.getEquippedStack(EquipmentSlot.HEAD)).equals("blood_moon_helm")
                && Inv.idOf(p.getEquippedStack(EquipmentSlot.CHEST)).equals("blood_moon_chestplate")
                && Inv.idOf(p.getEquippedStack(EquipmentSlot.LEGS)).equals("blood_moon_tassets");
    }

    /** Starts a special for this attack if one is armed and matches the attack style. */
    static Active begin(ServerPlayerEntity p, LivingEntity target, AttackType type) {
        if (secondary) {
            return null;
        }
        Spec spec = startable(p, type);
        if (spec == null) {
            return null;
        }
        if (spec.effect() == Effect.SHACKLES && !SysCombat.isDemon(target)) {
            Msg.game(p, "Scorching Shackles can only be used on demons.");
            return null;
        }
        spend(p, spec);
        Active a = new Active(spec, target);
        ACTIVE.put(p.getUuid(), a);
        if (spec.effect() == Effect.BLOOD_INFUSION) {
            // Blood Infusion: costs 25% of the current hitpoints (rounded down).
            p.setHealth(Math.max(1f, p.getHealth() - (float) Math.floor(p.getHealth() * spec.param() / 100.0)));
        }
        if (p.getWorld() instanceof ServerWorld w) {
            w.spawnParticles(net.minecraft.particle.ParticleTypes.CRIT, target.getX(), target.getY() + target.getHeight() / 2,
                    target.getZ(), 15, 0.3, 0.3, 0.3, 0.2);
        }
        return a;
    }

    static Active active(ServerPlayerEntity p) {
        return secondary ? null : ACTIVE.get(p.getUuid());
    }

    /** Whether the player's current attack is a special attack (boss scripts: "killed only with special attacks"). */
    public static boolean specialActive(ServerPlayerEntity p) {
        return active(p) != null;
    }

    /**
     * Starts the special on the attack's accuracy roll. Its accuracy and damage changes, minimum hits, the defence style it
     * rolls against and always-hitting specials come from PlayerHitCalc (HitCalcBridge marks the input special), so this
     * multiplier is 1.
     */
    static double accuracy(ServerPlayerEntity p, LivingEntity target, AttackType type) {
        if (secondary) {
            return 1.0;
        }
        Active a = ACTIVE.get(p.getUuid());
        if (a == null || a.target != target) {
            begin(p, target, type);
        }
        return 1.0;
    }

    static double maxHit(ServerPlayerEntity p, LivingEntity target, AttackType type) {
        Active a = active(p);
        if (a != null && a.spec.effect() == Effect.SHOVE) {
            return 0.0;
        }
        return 1.0;
    }

    static int afterRoll(ServerPlayerEntity p, LivingEntity target, AttackType type, int damage) {
        Active a = active(p);
        if (a == null) {
            return damage;
        }
        Random r = new Random(p.getRandom().nextLong());
        switch (a.spec.effect()) {
            case CLAWS, BURNING_CLAWS -> {
                boolean burning = a.spec.effect() == Effect.BURNING_CLAWS;
                PlayerHitCalc.Input in = hitInput(p, target, type);
                int max = PlayerHitCalc.maxHit(in);
                double chance = com.runecraft.combat.HitCalcBridge.hitChance(in, PlayerHitCalc.attackRoll(in),
                        com.runecraft.combat.HitCalcBridge.defenceRoll(target, in));
                boolean[] hits = new boolean[burning ? 3 : 4];
                for (int i = 0; i < hits.length; i++) {
                    hits[i] = r.nextDouble() < chance;
                }
                a.clawHits = burning ? SpecialAttacks.burningClawHits(r, hits, max) : SpecialAttacks.clawHits(r, hits, max);
                int first = -1;
                for (int i = 0; i < hits.length; i++) {
                    if (hits[i]) {
                        first = i;
                        break;
                    }
                }
                a.firstSuccess = first;
                return a.clawHits[0];
            }
            case DARK_BOW -> {
                // Each arrow deals between 5 (8 with dragon arrows) and 48 damage, even on a miss.
                int min = hasDragonArrows(p) ? 8 : a.spec.param();
                return Math.max(min, Math.min(48, damage));
            }
            case FLAT_BONUS -> {
                return damage + a.spec.param();
            }
            case BRUTAL_SWING -> {
                PlayerHitCalc.Input in = hitInput(p, target, type);
                int max = PlayerHitCalc.maxHit(in);
                double chance = com.runecraft.combat.HitCalcBridge.hitChance(in, PlayerHitCalc.attackRoll(in),
                        com.runecraft.combat.HitCalcBridge.defenceRoll(target, in));
                int successes = 0;
                for (int i = 0; i < 4; i++) {
                    if (r.nextDouble() < chance) {
                        successes++;
                    }
                }
                int[] range = SpecialAttacks.kistenRange(successes, max);
                return successes == 0 ? 0 : range[0] + r.nextInt(Math.max(0, range[1] - range[0]) + 1);
            }
            case MIN_DAMAGE -> {
                if (damage <= 0) {
                    return damage;
                }
                int max = PlayerHitCalc.maxHit(hitInput(p, target, type));
                return Math.max(damage, max * a.spec.param() / 100);
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
        return Inv.count(p, "dragon_arrow") > 0 || Inv.count(p, "lms_dragon_arrow") > 0;
    }

    static void afterHit(ServerPlayerEntity p, LivingEntity target, AttackType type, int dealt) {
        Active a = secondary ? null : ACTIVE.remove(p.getUuid());
        if (a == null) {
            soulreaperPassive(p, target, dealt);
            return;
        }
        Spec spec = a.spec;
        PlayerData data = Players.data(p);
        switch (spec.effect()) {
            case DOUBLE_HIT -> {
                String base = spec.itemId();
                if (base.equals("dragon_halberd") || base.startsWith("crystal_halberd")) {
                    // Sweep: a second hit with 25% less accuracy, only against targets larger than one tile.
                    if (hitInput(p, target, type).targetSize > 1) {
                        followUp(p, target, type, 0.75, false, 0);
                    }
                } else if (base.startsWith("abyssal_dagger")) {
                    // Abyssal Puncture: the second hit always lands when the first did.
                    if (dealt > 0) {
                        followUp(p, target, type, 1.0, true, 0);
                    }
                } else {
                    followUp(p, target, type, 1.0, false, 0);
                }
            }
            case CLAWS, BURNING_CLAWS -> {
                int[] h = a.clawHits;
                if (h != null) {
                    for (int i = 1; i < h.length; i++) {
                        fixedHit(p, target, type, h[i], i < 2 ? 0 : 1);
                    }
                    if (spec.effect() == Effect.BURNING_CLAWS && a.firstSuccess >= 0) {
                        // Each hitsplat may set the target alight (10 burn damage): 15% / 30% / 45% by the successful roll.
                        Random r = new Random(p.getRandom().nextLong());
                        for (int i = 0; i < 3; i++) {
                            if (r.nextDouble() < SpecialAttacks.burnChance(a.firstSuccess)) {
                                damageOverTime(p, target, 2, 5, 4); // approx: a burn as five 2-damage hits
                            }
                        }
                    }
                }
            }
            case DRAIN_DEFENCE_PERCENT -> {
                if (dealt > 0) {
                    // Relative to the target's current (already drained) Defence; stacks.
                    int def = SysCombat.defenceLevel(target);
                    SysCombat.drainDefence(target, (int) Math.ceil(def * spec.param() / 100.0));
                    SysCombat.dust(target, new Vector3f(0.5f, 0.5f, 0.5f), 15);
                }
            }
            case DRAIN_BY_DAMAGE -> drainByDamage(target, dealt, spec.itemId().contains("barrelchest") ? 10 : 100);
            case HEAL_AND_PRAY -> {
                int[] hp = SpecialAttacks.sgsRestore(dealt);
                p.heal(hp[0]);
                data.setPrayerPoints(data.prayerPoints() + hp[1]);
                ServerNetworking.sync(p);
            }
            case FREEZE -> {
                if (dealt > 0) {
                    SysCombat.freeze(target, spec.param());
                    if (spec.itemId().equals("abyssal_tentacle") && p.getRandom().nextInt(2) == 0) {
                        SysCombat.poison(target, 4);
                    }
                }
            }
            case HEAL_PERCENT -> p.heal(dealt * spec.param() / 100f);
            case BLOOD_SACRIFICE -> {
                if (dealt > 0) {
                    bloodSacrifice(p, target, spec.param());
                }
            }
            case WEAKEN -> {
                if (dealt > 0) {
                    int pct = SysCombat.isDemon(target) ? spec.param() * 2 : spec.param();
                    for (Skill s : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE}) {
                        SysCombat.drainStat(target, s, Math.max(1, SysCombat.baseStatLevel(target, s) * pct / 100));
                    }
                }
            }
            case AREA -> {
                for (LivingEntity other : p.getWorld().getEntitiesByClass(LivingEntity.class, target.getBoundingBox().expand(2.5),
                        e -> e != p && e != target && e.isAlive() && SysCombat.canAttack(p, e))) {
                    followUp(p, other, type, 1.0, false, 0);
                }
            }
            case SHOVE -> {
                SysCombat.freeze(target, spec.param());
                Vec3d d = target.getPos().subtract(p.getPos()).normalize();
                target.takeKnockback(1.2, -d.x, -d.z);
            }
            case LIGHTNING -> {
                if (dealt > 0) {
                    int extra = 1 + p.getRandom().nextInt(spec.param());
                    SysTicks.later(0, () -> {
                        if (target.isAlive()) {
                            int done = SysCombat.hit(p, target, AttackType.MAGIC, extra);
                            CombatHandler.awardXp(p, data.style().forMelee(), done, false);
                        }
                    });
                    SysCombat.dust(target, new Vector3f(0.9f, 0.9f, 1.0f), 25);
                }
            }
            case DOUBLE_SHOT, DARK_BOW -> {
                // Snapshot / Duality / Descent of Darkness fire a second projectile; Swarm fires four.
                int shots = spec.effect() == Effect.DOUBLE_SHOT && spec.param() > 1 ? spec.param() - 1 : 1;
                for (int i = 1; i <= shots; i++) {
                    SysTicks.later(i, () -> {
                        if (target.isAlive()) {
                            secondaryRanged(p, target, spec);
                        }
                    });
                }
            }
            case DIVISION -> {
                if (dealt > 0) {
                    drainDivision(target);
                }
                SysTicks.later(1, () -> {
                    if (target.isAlive()) {
                        int done = secondaryRanged(p, target, spec);
                        if (done > 0) {
                            drainDivision(target);
                        }
                    }
                });
            }
            case BEAR_DOWN -> {
                if (dealt > 0) {
                    damageOverTime(p, target, spec.param() / 4, 4, 2); // 20 damage over 6 seconds (approx: 4 x 5)
                }
            }
            case SHACKLES -> {
                if (SysCombat.isDemon(target)) {
                    SysCombat.freeze(target, spec.param());
                    damageOverTime(p, target, 1, 5, 4); // 1 burn damage now and every 4 ticks, 5 in total
                }
            }
            case PRAYER_SMASH -> {
                if (dealt > 0) {
                    if (target instanceof ServerPlayerEntity victim) {
                        PlayerData vd = Players.data(victim);
                        int drained = Math.min(vd.prayerPoints(), dealt);
                        vd.setPrayerPoints(vd.prayerPoints() - drained);
                        ServerNetworking.sync(victim);
                    }
                    data.setPrayerPoints(data.prayerPoints() + dealt);
                    ServerNetworking.sync(p);
                }
            }
            case BEHEAD -> SysState.of(p).soulStacks = 0;
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
        if (id.equals("statiuss_warhammer") && dealt > 0) {
            // Smash lowers the target's current Defence by 70% (wiki; approx, the tooltip says 75%).
            SysCombat.drainDefence(target, (int) Math.ceil(SysCombat.defenceLevel(target) * 0.70));
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

    /**
     * Bandos godsword Warstrike: drains the target's Defence by the damage dealt, then Strength, Prayer (players), Attack,
     * Magic and Ranged with what is left; the Barrelchest anchor drains {@code percent}% of the damage (Defence, Attack,
     * Ranged, Magic).
     */
    private static void drainByDamage(LivingEntity target, int dealt, int percent) {
        int remaining = dealt * percent / 100;
        Skill[] order = percent < 100 ? new Skill[]{Skill.DEFENCE, Skill.ATTACK, Skill.RANGED, Skill.MAGIC}
                : new Skill[]{Skill.DEFENCE, Skill.STRENGTH, Skill.PRAYER, Skill.ATTACK, Skill.MAGIC, Skill.RANGED};
        for (Skill s : order) {
            if (remaining <= 0) {
                break;
            }
            if (s == Skill.PRAYER) {
                if (target instanceof ServerPlayerEntity victim) {
                    PlayerData vd = Players.data(victim);
                    int d = Math.min(remaining, vd.prayerPoints());
                    vd.setPrayerPoints(vd.prayerPoints() - d);
                    remaining -= d;
                    ServerNetworking.sync(victim);
                }
                continue;
            }
            int lvl = SysCombat.drainedStatLevel(target, s);
            int d = Math.min(remaining, lvl);
            SysCombat.drainStat(target, s, d);
            remaining -= d;
        }
    }

    /** Tonalztics of Ralos: a damaging throw lowers the target's Defence by 12.5% of its Magic level. */
    private static void drainDivision(LivingEntity target) {
        SysCombat.drainDefence(target, Math.max(1, SysCombat.baseStatLevel(target, Skill.MAGIC) / 8));
    }

    /**
     * Ancient godsword Blood Sacrifice: unless the target is 5 or more tiles away 8 ticks later, it takes 25 damage and the
     * attacker heals 15% of its Hitpoints level (capped at 25, 15 against players).
     */
    private static void bloodSacrifice(ServerPlayerEntity p, LivingEntity target, int damage) {
        SysCombat.dust(target, new Vector3f(0.6f, 0.0f, 0.0f), 20);
        SysTicks.later(8, () -> {
            if (!target.isAlive() || !p.isAlive() || target.squaredDistanceTo(p) >= 25.0) {
                return;
            }
            int done = SysCombat.hit(p, target, AttackType.MAGIC, damage);
            int hpLevel = Math.round(target.getMaxHealth());
            if (!(target instanceof ServerPlayerEntity)) {
                hpLevel = Math.max(hpLevel, com.runecraft.combat.MonsterStats.of(target).hitpoints());
            }
            if (done > 0) {
                p.heal(SpecialAttacks.bloodSacrificeHeal(hpLevel, target instanceof ServerPlayerEntity));
            }
            SysCombat.dust(target, new Vector3f(0.8f, 0.0f, 0.0f), 30);
        });
    }

    /** {@code hits} hits of {@code damage} every {@code interval} game ticks (burns, Bear Down). */
    private static void damageOverTime(ServerPlayerEntity p, LivingEntity target, int damage, int hits, int interval) {
        for (int i = 0; i < hits; i++) {
            SysTicks.later(i * interval, () -> {
                if (target.isAlive() && p.isAlive()) {
                    SysCombat.hit(p, target, AttackType.MAGIC, Math.max(1, damage));
                }
            });
        }
    }

    /** Soulreaper axe: every damaging hit gains a soul stack (up to 5) at the cost of 8 hitpoints. */
    private static void soulreaperPassive(ServerPlayerEntity p, LivingEntity target, int dealt) {
        if (secondary || dealt <= 0
                || !"soulreaper_axe".equals(com.runecraft.osrs.data.EquipmentBonuses.reference(Inv.idOf(p.getMainHandStack())))) {
            return;
        }
        SysState.SysPlayer sp = SysState.of(p);
        if (sp.soulStacks < 5 && p.getHealth() > 8f) {
            sp.soulStacks++;
            p.setHealth(p.getHealth() - 8f);
        }
    }

    /** The player's input for a special attack's hit (special = true while a spec is active or being followed up). */
    private static PlayerHitCalc.Input hitInput(ServerPlayerEntity p, LivingEntity target, AttackType type) {
        return com.runecraft.combat.HitCalcBridge.input(p, target, type, null, type == AttackType.RANGED ? SysCombat.ammoStrength(p) : 0);
    }

    /** The input of a follow-up hit of the special (second stab, extra arrow, sweep), with the special's changes. */
    private static PlayerHitCalc.Input specialInput(ServerPlayerEntity p, LivingEntity target, AttackType type) {
        boolean before = forceSpecial;
        forceSpecial = true;
        try {
            return hitInput(p, target, type);
        } finally {
            forceSpecial = before;
        }
    }

    /**
     * Rolls and applies an additional hit of the special (through the other combat hooks): {@code accMult} scales the attack
     * roll (halberd sweep 0.75), {@code alwaysHits} skips the accuracy roll (abyssal dagger).
     */
    static void followUp(ServerPlayerEntity p, LivingEntity target, AttackType type, double accMult, boolean alwaysHits, int delay) {
        Runnable run = () -> {
            if (!target.isAlive() || !p.isAlive()) {
                return;
            }
            PlayerHitCalc.Input in = specialInput(p, target, type);
            secondary = true;
            try {
                long att = (long) (PlayerHitCalc.attackRoll(in) * accMult * CombatHandler.hookAccuracy(p, target, type));
                long def = com.runecraft.combat.HitCalcBridge.defenceRoll(target, in);
                int max = (int) Math.floor(PlayerHitCalc.maxHit(in) * CombatHandler.hookMaxHit(p, target, type) + 1e-9);
                Random r = new Random(p.getRandom().nextLong());
                int damage;
                if (alwaysHits) {
                    int min = Math.min(PlayerHitCalc.minMax(in)[0], Math.max(0, max));
                    damage = min + r.nextInt(Math.max(0, max - min) + 1);
                } else {
                    damage = com.runecraft.combat.HitCalcBridge.roll(r, in, att, def, max);
                }
                damage = com.runecraft.combat.HitCalcBridge.afterRoll(in, damage);
                damage = CombatHandler.hookAfterRoll(p, target, type, damage, max);
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

    /** An extra projectile of a ranged special (with the special's accuracy, damage and dark bow limits); returns the damage dealt. */
    private static int secondaryRanged(ServerPlayerEntity p, LivingEntity target, Spec spec) {
        if (p.getWorld() instanceof ServerWorld w) {
            SysCombat.trail(w, p.getEyePos(), target.getPos().add(0, target.getHeight() / 2, 0), new Vector3f(0.3f, 0.2f, 0.4f));
        }
        PlayerHitCalc.Input in = specialInput(p, target, AttackType.RANGED);
        secondary = true;
        try {
            long att = (long) (PlayerHitCalc.attackRoll(in) * CombatHandler.hookAccuracy(p, target, AttackType.RANGED));
            long def = com.runecraft.combat.HitCalcBridge.defenceRoll(target, in);
            int max = (int) Math.floor(PlayerHitCalc.maxHit(in) * CombatHandler.hookMaxHit(p, target, AttackType.RANGED) + 1e-9);
            int damage = com.runecraft.combat.HitCalcBridge.roll(new Random(p.getRandom().nextLong()), in, att, def, Math.max(1, max));
            if (spec.effect() == Effect.DARK_BOW) {
                damage = Math.max(hasDragonArrows(p) ? 8 : spec.param(), Math.min(48, damage));
            }
            damage = com.runecraft.combat.HitCalcBridge.afterRoll(in, damage);
            int done = SysCombat.hit(p, target, AttackType.RANGED, damage);
            CombatHandler.awardXp(p, Players.data(p).style().forRanged(), done, false);
            return done;
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
                followUp(p, t, com.runecraft.combat.Equipment.meleeType(p, s), 1.0, false, 0);
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
                boolean purging = spec.itemId().endsWith("purging_staff");
                if (purging && !SysCombat.isDemon(t)) {
                    Msg.game(p, "Scatter Ashes can only be used against demons.");
                    return false;
                }
                // The blast's base max hit (nightmare staves: scaled with Magic up to 44 / 58; sceptre / Eye of Ayak: their
                // built-in spell), accuracy and damage changes come from PlayerHitCalc with the input marked special.
                PlayerHitCalc.Input in = specialInput(p, t, AttackType.MAGIC);
                if (purging) {
                    int magic = s.currentLevel(Skill.MAGIC);
                    // The best demonbane spell the player can cast: Dark (82), Superior (62) or Inferior (44) Demonbane.
                    com.runecraft.combat.HitCalcBridge.spell(in, magic >= 82 ? 30 : magic >= 62 ? 23 : 16, false);
                }
                long att = com.runecraft.combat.HitCalcBridge.attackRoll(p, t, in);
                long def = com.runecraft.combat.HitCalcBridge.defenceRoll(t, in);
                int max = com.runecraft.combat.HitCalcBridge.maxHit(p, t, in);
                if (spec.itemId().endsWith("dawnbringer")) {
                    max = 150; // Pulsate: a guaranteed 75-150
                }
                int dmg = com.runecraft.combat.HitCalcBridge.roll(new Random(p.getRandom().nextLong()), in, att, def, max);
                if (spec.itemId().endsWith("dawnbringer")) {
                    dmg = 75 + p.getRandom().nextInt(76);
                }
                dmg = com.runecraft.combat.HitCalcBridge.afterRoll(in, dmg);
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
                if (done > 0 && spec.itemId().endsWith("accursed_sceptre")) {
                    // Condemn: lowers the target's Defence and Magic by 15%.
                    SysCombat.drainStat(t, Skill.DEFENCE, Math.max(1, SysCombat.drainedStatLevel(t, Skill.DEFENCE) * 15 / 100));
                    SysCombat.drainStat(t, Skill.MAGIC, Math.max(1, SysCombat.drainedStatLevel(t, Skill.MAGIC) * 15 / 100));
                }
                if (done > 0 && spec.itemId().endsWith("eye_of_ayak")) {
                    // Soul Rend lowers the target's Magic defence by the damage (approx: its Magic level).
                    SysCombat.drainStat(t, Skill.MAGIC, done);
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
