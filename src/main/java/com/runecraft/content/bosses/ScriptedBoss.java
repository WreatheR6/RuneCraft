package com.runecraft.content.bosses;

import com.runecraft.boss.BossScript;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.Mechanics;
import com.runecraft.combat.CombatHandler;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Base class for this module's boss scripts.
 *
 * <ul>
 *   <li><b>Driven mode</b> ({@link #driven()}): the script picks and performs every attack itself on its
 *   own cadence ({@link #nextAttack}), so it can count attacks, run rotations and queue volleys. The
 *   engine's attack goal is handed a harmless idle attack that only controls movement.</li>
 *   <li><b>Strikes</b>: dodgeable ground markers that land after a delay ({@link #strike}).</li>
 *   <li><b>Hitpoint scaling</b>: Minecraft caps max health at 1024, so bosses with more OSRS hitpoints
 *   take proportionally less damage to keep their real effective HP.</li>
 *   <li><b>Secondary forms</b> ({@link #creditAs()}): a lethal hit credits the main boss's kill instead.</li>
 * </ul>
 */
abstract class ScriptedBoss implements BossScript, DataBossEntity.HpScaled {
    /** Idle attacks handed to the engine in driven mode (SUMMON with no minion does nothing). */
    static final AttackDef IDLE_RANGED = AttackDef.of("idle", AttackType.MAGIC, 0, 64, 1).special(AttackDef.Special.SUMMON, 0);
    static final AttackDef IDLE_MELEE = AttackDef.of("idle", AttackType.CRUSH, 0, 64, 1).special(AttackDef.Special.SUMMON, 0);
    static final int MAX_MC_HEALTH = 1024;

    /** A dodgeable ground strike. */
    private record Strike(ServerWorld world, Vec3d pos, long due, int maxDamage, double radius, ParticleEffect particle,
                          BiConsumer<List<ServerPlayerEntity>, Vec3d> onLand, String name) {
    }

    private final List<Strike> strikes = new ArrayList<>();
    protected BlockPos home;
    protected int cooldown;
    protected int attackCount;
    protected AttackDef last;
    protected long now;
    private ServerPlayerEntity pendingDeathBy;
    private boolean pendingDeath;

    /** True when the script performs attacks itself. */
    protected boolean driven() {
        return false;
    }

    /** Next attack in driven mode (may have side effects; called once per attack). */
    protected AttackDef nextAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
        return null;
    }

    /** Whether the boss should walk into melee range (driven mode). */
    protected boolean wantsMelee(DataBossEntity boss, ServerPlayerEntity target) {
        return false;
    }

    /** Game ticks until the next attack after {@code attack}. */
    protected int attackDelay(DataBossEntity boss, AttackDef attack) {
        BossDef def = boss.def();
        return def == null ? 4 : def.attackSpeed;
    }

    /** False while the script is busy with a special (no auto attacks). */
    protected boolean canAttack(DataBossEntity boss) {
        return true;
    }

    /** Called after a driven attack has been started. */
    protected void afterAttack(DataBossEntity boss, ServerPlayerEntity target, AttackDef attack) {
    }

    /** Main boss id whose kill is credited when this form dies, or null if this is the main boss. */
    protected String creditAs() {
        return null;
    }

    /** Per-game-tick logic of the concrete script. */
    protected void onGameTick(DataBossEntity boss, ServerPlayerEntity target) {
    }

    /** Damage adjustments of the concrete script (before HP scaling). */
    protected int incoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
        return damage;
    }

    // ------------------------------------------------------------------

    @Override
    public AttackDef chooseAttack(DataBossEntity boss, ServerPlayerEntity target, double distance) {
        if (!driven()) {
            return null;
        }
        return wantsMelee(boss, target) ? IDLE_MELEE : IDLE_RANGED;
    }

    @Override
    public void tick(DataBossEntity boss) {
        if (pendingDeath) {
            pendingDeath = false;
            BossFx.creditKill(boss, pendingDeathBy, creditAs());
        }
    }

    @Override
    public final void gameTick(DataBossEntity boss, long gameTick) {
        now = gameTick;
        if (home == null) {
            home = boss.getBlockPos();
        }
        ServerPlayerEntity target = target(boss);
        tickStrikes();
        onGameTick(boss, target);
        if (!driven() || !boss.isAlive()) {
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (target == null || !canAttack(boss)) {
            return;
        }
        double dist = Math.sqrt(boss.squaredDistanceTo(target)) - boss.getWidth() / 2;
        AttackDef a = nextAttack(boss, target, dist);
        if (a == null || dist > a.range()) {
            return;
        }
        perform(boss, target, a);
        cooldown = Math.max(1, attackDelay(boss, a)) - 1;
        afterAttack(boss, target, a);
    }

    /** Performs an attack now (also usable outside the driven cadence, e.g. volleys and specials). */
    protected void perform(DataBossEntity boss, ServerPlayerEntity target, AttackDef a) {
        last = a;
        attackCount++;
        Mechanics.perform(boss, target, a, multiplier(boss) * boss.variantHitScale(a));
    }

    /** An attack's max hit for this boss (awakened variants hit harder). */
    protected static int maxHit(DataBossEntity boss, AttackDef a) {
        return (int) Math.round(a.maxHit() * boss.variantHitScale(a));
    }

    protected double multiplier(DataBossEntity boss) {
        BossDef def = boss.def();
        return def != null && boss.phase() >= 0 ? def.phases.get(boss.phase()).damageMultiplier() : 1.0;
    }

    @Override
    public final int modifyIncoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
        int d = incoming(boss, attacker, damage, type);
        int hp = boss.trueHitpoints();
        if (hp > MAX_MC_HEALTH && d > 0) {
            double scaled = d * (double) MAX_MC_HEALTH / hp;
            int whole = (int) scaled;
            d = whole + (boss.getRandom().nextDouble() < scaled - whole ? 1 : 0);
        }
        if (creditAs() != null && d >= boss.getHealth()) {
            d = Math.max(0, (int) Math.ceil(boss.getHealth()) - 1);
            pendingDeath = true;
            pendingDeathBy = attacker;
        }
        return d;
    }

    /** Scales a "true OSRS hitpoints" amount (heals, thresholds) into Minecraft health. */
    protected static float mcHp(DataBossEntity boss, double osrsHp) {
        int hp = boss.trueHitpoints();
        return hp > MAX_MC_HEALTH ? (float) (osrsHp * MAX_MC_HEALTH / hp) : (float) osrsHp;
    }

    protected static double percent(DataBossEntity boss) {
        return boss.getHealth() * 100.0 / boss.getMaxHealth();
    }

    protected ServerPlayerEntity target(DataBossEntity boss) {
        if (boss.getTarget() instanceof ServerPlayerEntity p && p.isAlive() && !p.isSpectator() && !p.isCreative()) {
            return p;
        }
        ServerPlayerEntity nearest = null;
        for (ServerPlayerEntity p : BossFx.players(boss, 24)) {
            if (nearest == null || p.squaredDistanceTo(boss) < nearest.squaredDistanceTo(boss)) {
                nearest = p;
            }
        }
        if (nearest != null) {
            boss.setTarget(nearest);
        }
        return nearest;
    }

    // ---------------- Strikes ----------------

    /** Marks a ground position; after {@code delayTicks} everyone within {@code radius} takes up to {@code maxDamage}. */
    protected void strike(DataBossEntity boss, Vec3d pos, int delayTicks, int maxDamage, double radius, ParticleEffect particle) {
        strike(boss, pos, delayTicks, maxDamage, radius, particle, null);
    }

    protected void strike(DataBossEntity boss, Vec3d pos, int delayTicks, int maxDamage, double radius, ParticleEffect particle,
                          BiConsumer<List<ServerPlayerEntity>, Vec3d> onLand) {
        strikes.add(new Strike(BossFx.world(boss), pos, now + delayTicks, maxDamage, radius, particle, onLand, strikeName));
        owner = boss;
    }

    /** The attack name the next strikes' damage carries (Combat Achievement "no damage from ..." tasks); null: unnamed. */
    private String strikeName;

    /** A strike whose damage is reported under an attack name (e.g. "lightning"). */
    protected void namedStrike(String name, DataBossEntity boss, Vec3d pos, int delayTicks, int maxDamage, double radius,
                               ParticleEffect particle) {
        namedStrike(name, boss, pos, delayTicks, maxDamage, radius, particle, null);
    }

    protected void namedStrike(String name, DataBossEntity boss, Vec3d pos, int delayTicks, int maxDamage, double radius,
                               ParticleEffect particle, BiConsumer<List<ServerPlayerEntity>, Vec3d> onLand) {
        strikeName = name;
        strike(boss, pos, delayTicks, maxDamage, radius, particle, onLand);
        strikeName = null;
    }

    private DataBossEntity owner;

    protected boolean strikesPending() {
        return !strikes.isEmpty();
    }

    private void tickStrikes() {
        Iterator<Strike> it = strikes.iterator();
        List<Strike> landed = new ArrayList<>();
        while (it.hasNext()) {
            Strike s = it.next();
            if (now >= s.due()) {
                landed.add(s);
                it.remove();
            } else {
                BossFx.ring(s.world(), s.pos(), s.radius(), s.particle());
            }
        }
        for (Strike s : landed) {
            s.world().spawnParticles(ParticleTypes.EXPLOSION, s.pos().x, s.pos().y + 0.3, s.pos().z, 2, 0.3, 0.2, 0.3, 0);
            s.world().spawnParticles(s.particle(), s.pos().x, s.pos().y + 0.5, s.pos().z, 15, s.radius() / 2, 0.4, s.radius() / 2, 0.02);
            List<ServerPlayerEntity> hit = s.world().getPlayers(p -> p.isAlive() && !p.isSpectator()
                    && p.getPos().squaredDistanceTo(s.pos()) <= s.radius() * s.radius());
            if (s.maxDamage() > 0) {
                for (ServerPlayerEntity p : hit) {
                    if (s.name() != null) {
                        com.runecraft.combat.CombatHooks.nameAttack(s.name(), null);
                    }
                    CombatHandler.applyDamage(owner != null && owner.isAlive() ? owner : null, p,
                            p.getRandom().nextInt(s.maxDamage() + 1));
                }
            }
            if (s.onLand() != null) {
                s.onLand().accept(hit, s.pos());
            }
        }
    }

    /** A random ground point within {@code radius} blocks of {@code centre}. */
    protected static Vec3d around(DataBossEntity boss, Vec3d centre, double radius) {
        double a = boss.getRandom().nextDouble() * Math.PI * 2;
        double r = Math.sqrt(boss.getRandom().nextDouble()) * radius;
        return BossFx.ground(BossFx.world(boss), centre.x + Math.cos(a) * r, centre.z + Math.sin(a) * r, centre.y);
    }

    protected Vec3d homeVec() {
        return home == null ? Vec3d.ZERO : Vec3d.ofBottomCenter(home);
    }
}
