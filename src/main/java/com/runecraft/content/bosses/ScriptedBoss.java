package com.runecraft.content.bosses;

import com.runecraft.boss.BossScript;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.Mechanics;
import com.runecraft.combat.CombatHandler;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.world.ArenaLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

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
    /** Idle attacks handed to the engine in driven mode: movement only, the engine performs nothing for them. */
    static final AttackDef IDLE_RANGED = AttackDef.idle(AttackType.MAGIC);
    static final AttackDef IDLE_MELEE = AttackDef.idle(AttackType.CRUSH);
    static final int MAX_MC_HEALTH = 1024;

    /** A dodgeable ground strike. */
    private record Strike(ServerLevel world, Vec3 pos, long due, int maxDamage, double radius, ParticleOptions particle,
                          BiConsumer<List<ServerPlayer>, Vec3> onLand, String name) {
    }

    private final List<Strike> strikes = new ArrayList<>();
    protected BlockPos home;
    protected int cooldown;
    protected int attackCount;
    protected AttackDef last;
    protected long now;
    private ServerPlayer pendingDeathBy;
    private boolean pendingDeath;

    /** True when the script performs attacks itself. */
    protected boolean driven() {
        return false;
    }

    /** Next attack in driven mode (may have side effects; called once per attack). */
    protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
        return null;
    }

    /** Whether the boss should walk into melee range (driven mode). */
    protected boolean wantsMelee(DataBossEntity boss, ServerPlayer target) {
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
    protected void afterAttack(DataBossEntity boss, ServerPlayer target, AttackDef attack) {
    }

    /** Main boss id whose kill is credited when this form dies, or null if this is the main boss. */
    protected String creditAs() {
        return null;
    }

    /** Per-game-tick logic of the concrete script. */
    protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
    }

    /** Damage adjustments of the concrete script (before HP scaling). */
    protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
        return damage;
    }

    /**
     * The hit {@link #incoming} let through, as it goes on to the boss's health (after the HP scaling, which can round a
     * small hit down to 0: then nothing lands).
     */
    protected void incomingScaled(DataBossEntity boss, ServerPlayer attacker, int damage) {
    }

    // ------------------------------------------------------------------

    @Override
    public AttackDef chooseAttack(DataBossEntity boss, ServerPlayer target, double distance) {
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
            home = boss.blockPosition();
        }
        ServerPlayer target = target(boss);
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
        double dist = Math.sqrt(boss.distanceToSqr(target)) - boss.getBbWidth() / 2;
        AttackDef a = nextAttack(boss, target, dist);
        if (a == null || dist > a.range()) {
            return;
        }
        perform(boss, target, a);
        cooldown = Math.max(1, attackDelay(boss, a)) - 1;
        afterAttack(boss, target, a);
    }

    /** Performs an attack now (also usable outside the driven cadence, e.g. volleys and specials). */
    protected void perform(DataBossEntity boss, ServerPlayer target, AttackDef a) {
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
    public final int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
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
        incomingScaled(boss, attacker, d);
        return d;
    }

    /**
     * The on-hit hook sees the attack that landed as {@link #last}: hits land in real time (round 41), after the boss may have
     * started its next attack. An attack the hook itself starts stays the last one.
     */
    @Override
    public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type, AttackDef attack) {
        if (attack == null) {
            return onHitPlayer(boss, player, damage, type);
        }
        AttackDef started = last;
        int count = attackCount;
        last = attack;
        try {
            return onHitPlayer(boss, player, damage, type);
        } finally {
            if (attackCount == count) {
                last = started;
            }
        }
    }

    /** Scales a "true OSRS hitpoints" amount (heals, thresholds) into Minecraft health. */
    protected static float mcHp(DataBossEntity boss, double osrsHp) {
        int hp = boss.trueHitpoints();
        return hp > MAX_MC_HEALTH ? (float) (osrsHp * MAX_MC_HEALTH / hp) : (float) osrsHp;
    }

    protected static double percent(DataBossEntity boss) {
        return boss.getHealth() * 100.0 / boss.getMaxHealth();
    }

    protected ServerPlayer target(DataBossEntity boss) {
        if (boss.getTarget() instanceof ServerPlayer p && p.isAlive() && !p.isSpectator() && !p.isCreative()) {
            return p;
        }
        ServerPlayer nearest = null;
        for (ServerPlayer p : BossFx.players(boss, 24)) {
            if (nearest == null || p.distanceToSqr(boss) < nearest.distanceToSqr(boss)) {
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
    protected void strike(DataBossEntity boss, Vec3 pos, int delayTicks, int maxDamage, double radius, ParticleOptions particle) {
        strike(boss, pos, delayTicks, maxDamage, radius, particle, null);
    }

    protected void strike(DataBossEntity boss, Vec3 pos, int delayTicks, int maxDamage, double radius, ParticleOptions particle,
                          BiConsumer<List<ServerPlayer>, Vec3> onLand) {
        strikes.add(new Strike(BossFx.world(boss), pos, now + delayTicks, maxDamage, radius, particle, onLand, strikeName));
        owner = boss;
    }

    /** The attack name the next strikes' damage carries (Combat Achievement "no damage from ..." tasks); null: unnamed. */
    private String strikeName;

    /** A strike whose damage is reported under an attack name (e.g. "lightning"). */
    protected void namedStrike(String name, DataBossEntity boss, Vec3 pos, int delayTicks, int maxDamage, double radius,
                               ParticleOptions particle) {
        namedStrike(name, boss, pos, delayTicks, maxDamage, radius, particle, null);
    }

    protected void namedStrike(String name, DataBossEntity boss, Vec3 pos, int delayTicks, int maxDamage, double radius,
                               ParticleOptions particle, BiConsumer<List<ServerPlayer>, Vec3> onLand) {
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
            s.world().sendParticles(ParticleTypes.EXPLOSION, s.pos().x, s.pos().y + 0.3, s.pos().z, 2, 0.3, 0.2, 0.3, 0);
            s.world().sendParticles(s.particle(), s.pos().x, s.pos().y + 0.5, s.pos().z, 15, s.radius() / 2, 0.4, s.radius() / 2, 0.02);
            List<ServerPlayer> hit = s.world().getPlayers(p -> p.isAlive() && !p.isSpectator()
                    && p.position().distanceToSqr(s.pos()) <= s.radius() * s.radius());
            if (s.maxDamage() > 0) {
                for (ServerPlayer p : hit) {
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
    protected static Vec3 around(DataBossEntity boss, Vec3 centre, double radius) {
        double a = boss.getRandom().nextDouble() * Math.PI * 2;
        double r = Math.sqrt(boss.getRandom().nextDouble()) * radius;
        return BossFx.ground(BossFx.world(boss), centre.x + Math.cos(a) * r, centre.z + Math.sin(a) * r, centre.y);
    }

    protected Vec3 homeVec() {
        return home == null ? Vec3.ZERO : Vec3.atBottomCenterOf(home);
    }

    /**
     * The centre of the arena the boss was summoned in, or its spawn block when it was summoned away from its arena. The
     * summoning stone sits by the arena's north edge, so a stone-summoned boss's spawn block is not the centre.
     */
    protected static BlockPos arenaCentre(DataBossEntity boss) {
        BlockPos at = boss.blockPosition();
        if (!inOwnArena(boss)) {
            return at;
        }
        ArenaLayout.Arena a = ArenaLayout.of(boss.def().id);
        Vec3 floor = BossFx.ground(BossFx.world(boss), a.x() + 0.5, a.z() + 0.5, at.getY());
        return new BlockPos(a.x(), (int) Math.floor(floor.y), a.z());
    }

    /**
     * Whether the boss stands in its own arena (the one {@link ArenaLayout} built for it), rather than somewhere it was spawned
     * away from it (a quest version, a boss spawned by a command or a test).
     */
    protected static boolean inOwnArena(DataBossEntity boss) {
        BlockPos at = boss.blockPosition();
        ArenaLayout.Arena a = boss.def() == null ? null : ArenaLayout.of(boss.def().id);
        return a != null && Math.hypot(at.getX() - a.x(), at.getZ() - a.z()) <= a.radius() + 3;
    }
}
