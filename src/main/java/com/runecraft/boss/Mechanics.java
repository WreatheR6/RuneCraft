package com.runecraft.boss;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.MonsterStats;
import com.runecraft.combat.OsrsKnockback;
import com.runecraft.combat.PoisonService;
import com.runecraft.entity.BossEntity;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.network.Net;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.CombatFormulas;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.ThreatTiming;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.platform.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Executes {@link AttackDef}s for module monsters and data-driven bosses: accuracy rolls, protection prayers, telegraphs, sky
 * strikes, AoE, hazards, poison/venom, freezes, drains and more; and delivers every monster hit on a player in real time
 * (round 41, CONTENT_GUIDE rule 5, // playability; the floors are {@link ThreatTiming}'s):
 * <ul>
 *   <li>a telegraphed attack lands at least 36 MC after its first cue, its halo (and a sky strike's or AoE's ring at its true
 *   radius) redrawn every 4 MC, a rising note in its last 10 MC (OSRS: 1-2 game ticks after the animation);</li>
 *   <li>an untelegraphed ranged or magic attack is a homing projectile, judged when it arrives: it fizzles on a wall, on a target that
 *   died, left the level or got out of range (OSRS rolls it at launch and applies it after the hit delay);</li>
 *   <li>a melee attack winds up for 6-10 MC and only lands on a target still within reach (OSRS: on the swing tick);</li>
 *   <li>hits that need different protection prayers land at least 20 MC apart on one target: a projectile, swing or single-target
 *   telegraph is pushed (by up to 20 MC) from those on their way or just landed (OSRS: a one-tick flick);</li>
 *   <li>ground hazards arm after 12 MC and hit a player standing in them once per 12 MC (OSRS: once a game tick).</li>
 * </ul>
 * Rolls, max hits, multipliers, prayer rules and every on-hit effect are unchanged and evaluated at impact, and cadence is stamped
 * when an attack starts, so these delays move a hit later but never add or remove one. An attack a monster starts while one of
 * its own hits is resolving (a multi-target splash, a chained follow-up) resolves with it, as before.
 */
public final class Mechanics {
    /** How a pending hit is delivered. */
    public enum Kind {
        /** A telegraphed attack: halo and markers until it lands. */
        TELEGRAPH,
        /** A melee swing (or an area burst) winding up. */
        WINDUP,
        /** A travelling projectile. */
        PROJECTILE
    }

    /** One pending hit as a player experiences it (self-test recorders). */
    public record Threat(long id, Kind kind, long createdMc, long firstCueMc, long dueMc, AttackType style, boolean prayerCheckable) {
    }

    /** Self-test recorder: sees every pending hit created, each cue drawn for it, and how it ended. */
    public interface PendingListener {
        default void created(Threat threat) {
        }

        default void cue(Threat threat, long mc) {
        }

        default void resolved(Threat threat, long mc, boolean landed) {
        }
    }

    public static PendingListener pendingListener = new PendingListener() {
    };

    /**
     * Self-test hook: players the area tests (AoE, sky strikes, hazards) see besides the level's own player list. The self-test's
     * fake player is not in that list.
     */
    public static java.util.function.Function<ServerLevel, List<ServerPlayer>> extraPlayers = w -> List.of();

    private static final class Pending {
        final long id;
        final Kind kind;
        final LivingEntity from;
        final OsrsMonsterEntity mob;
        final ServerPlayer target;
        final AttackDef attack;
        final AttackType style;
        final double multiplier;
        final Vec3 marked;
        final ServerLevel level;
        final long createdMc;
        final long dueMc;
        /** Melee: the reach (a gap); projectile and telegraph: the attack's range. */
        final double reach;
        final Runnable resolve;
        final Runnable after;
        final ParticleOptions particle;
        /** A melee hit judged by the reach rule at landing. */
        final boolean meleeRule;
        /** The boss already blinked next to its target at launch. */
        final boolean blinked;
        Vec3 launch;
        Vec3 head;
        long lastDrawMc = Long.MIN_VALUE;
        boolean swung;
        boolean impactShown;
        boolean done;
        Threat threat;

        Pending(Kind kind, LivingEntity from, OsrsMonsterEntity mob, ServerPlayer target, AttackDef attack, AttackType style,
                double multiplier, Vec3 marked, long createdMc, long dueMc, double reach, Runnable resolve, Runnable after,
                ParticleOptions particle, boolean meleeRule, boolean blinked) {
            this.id = ++nextId;
            this.kind = kind;
            this.from = from;
            this.mob = mob;
            this.target = target;
            this.attack = attack;
            this.style = style;
            this.multiplier = multiplier;
            this.marked = marked;
            this.level = (ServerLevel) from.level();
            this.createdMc = createdMc;
            this.dueMc = dueMc;
            this.reach = reach;
            this.resolve = resolve;
            this.after = after;
            this.particle = particle;
            this.meleeRule = meleeRule;
            this.blinked = blinked;
            boolean checkable = attack == null || (attack.special() != AttackDef.Special.PRAYER_PIERCING
                    && attack.special() != AttackDef.Special.DRAGONFIRE);
            this.threat = new Threat(id, kind, createdMc, createdMc, dueMc, style, checkable);
        }
    }

    private static final class Hazard {
        final ServerLevel world;
        final Vec3 pos;
        final int damage;
        final long untilGameTick;
        final LivingEntity owner;
        final long placedMc;
        final long armAtMc;
        final Map<UUID, Long> lastHit = new HashMap<>();

        Hazard(ServerLevel world, Vec3 pos, int damage, long untilGameTick, LivingEntity owner, long placedMc) {
            this.world = world;
            this.pos = pos;
            this.damage = Math.max(1, damage);
            this.untilGameTick = untilGameTick;
            this.owner = owner;
            this.placedMc = placedMc;
            this.armAtMc = placedMc + ThreatTiming.HAZARD_ARM_MC;
        }
    }

    /**
     * Attacks whose script already delivers them (its own telegraphed flight, prayed against while they fly) and performs them
     * when they arrive: they land at once instead of flying a second time. The Leviathan's volley orbs (LeviathanScript lands each
     * from its own list of orbs in flight).
     */
    private static final java.util.Set<AttackDef> SELF_DELIVERED = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    static {
        selfDelivered(com.runecraft.osrs.content.bosses.Dt2Bosses.LEVI_RANGED, com.runecraft.osrs.content.bosses.Dt2Bosses.LEVI_MAGIC,
                com.runecraft.osrs.content.bosses.Dt2Bosses.LEVI_MELEE);
    }

    /** Marks attacks a script flies itself and performs on impact (see {@link #SELF_DELIVERED}). */
    public static void selfDelivered(AttackDef... attacks) {
        java.util.Collections.addAll(SELF_DELIVERED, attacks);
    }

    /** A single-target hit that just landed on a player: new hits of another style are spaced from it as from one in flight. */
    private record Landed(ServerPlayer target, long mc, AttackType style) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();
    /** Single-target hits landed within the last {@link ThreatTiming#STYLE_SPACING_MC} MC. */
    private static final List<Landed> LANDED = new ArrayList<>();
    private static final List<Hazard> HAZARDS = new ArrayList<>();
    private static long nextId;
    /** The monster whose hit is resolving right now, and that hit's attack (null outside a resolution or for a built-in hit). */
    private static LivingEntity resolvingMob;
    private static AttackDef resolvingAttack;

    private Mechanics() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
        OsrsKnockback.register();
    }

    /**
     * Self-test hook ({@code com.runecraft.selftest}): moves every pending hit (telegraphs, wind-ups, projectiles) and the ground
     * hazards on to the current Minecraft tick, for a fight simulated inside one server tick (see
     * {@code ActionManager.advanceForSelfTest}).
     */
    public static void tickForSelfTest() {
        tick();
    }

    /** The attack whose hit is resolving right now (a script's on-hit hook can tell which of its attacks landed), else null. */
    public static AttackDef resolvingAttack() {
        return resolvingAttack;
    }

    /** Whether one of {@code mob}'s own hits is resolving right now. */
    public static boolean resolving(LivingEntity mob) {
        return mob != null && resolvingMob == mob;
    }

    /** Pending hits on a player (self-tests). */
    public static int pendingOn(ServerPlayer target) {
        int n = 0;
        for (Pending p : PENDING) {
            if (!p.done && p.target == target) {
                n++;
            }
        }
        return n;
    }

    private static void tick() {
        long now = ActionManager.mcTick();
        if (!LANDED.isEmpty()) {
            LANDED.removeIf(l -> now - l.mc() >= ThreatTiming.STYLE_SPACING_MC || l.target().isRemoved());
        }
        if (!PENDING.isEmpty()) {
            // a resolving hit may start new ones (scripts' on-hit hooks): those wait for the next tick
            for (Pending p : PENDING.toArray(new Pending[0])) {
                if (!p.done) {
                    step(p, now);
                }
            }
            PENDING.removeIf(p -> p.done);
        }
        tickHazards(now);
    }

    /** Sees every attack a monster or boss performs (Combat Achievements count specials and repeats). */
    public interface AttackListener {
        void onAttack(OsrsMonsterEntity mob, ServerPlayer target, AttackDef attack);
    }

    public static AttackListener attackListener = (mob, target, attack) -> {
    };

    /**
     * Starts an attack: the attack listener hears it now, the hit is delivered in real time (telegraph, wind-up or projectile) and
     * rolled when it lands. {@code multiplier} scales the max hit (enrage phases, variants).
     */
    public static void perform(OsrsMonsterEntity mob, ServerPlayer target, AttackDef attack, double multiplier) {
        if (attack.isIdle()) {
            // A boss script's movement-only placeholder (AttackDef.idle), also what a driven script spends a turn on while its
            // special is made of its own strikes (the Deranged archaeologist's books): not an attack, so nothing happens and the
            // attack listener (Combat Achievements' "used:", "repeat:" and same-tick checks) never sees it.
            return;
        }
        attackListener.onAttack(mob, target, attack);
        AttackDef.Special sp = attack.special();
        Vec3 marked = target.position();
        if (resolving(mob) || SELF_DELIVERED.contains(attack)) {
            // A splash or follow-up started by one of its own landing hits (GWD multi-target attacks, Nex's blood siphon): lands
            // with it, as before (the scripts' "spreading" flags rely on it). An attack its script already flew lands now.
            if (attack.melee()) {
                mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
            land(mob, target, attack, marked, multiplier, false, true);
            return;
        }
        long now = ActionManager.mcTick();
        ParticleOptions extra = particle(attack, null);
        if (attack.message() != null) {
            ThreatCues.announce(mob, target, attack.message(), attack.style());
        }
        if (ThreatTiming.telegraphed(attack.telegraphTicks(), sp)) {
            // playability: OSRS lands a telegraph 1-2 game ticks after its animation; here at least 36 MC after the first cue
            ThreatCues.launchSound(mob, attack.style(), sp);
            int lead = ThreatTiming.leadMc(attack.telegraphTicks(), sp);
            // a single-target hit is spread from the other styles already on their way to the target (area ones keep their tick)
            long due = singleTarget(sp) ? spacedDue(target, now + lead, attack.style()) : now + lead;
            double reach = attack.melee() ? Math.max(ThreatTiming.MELEE_REACH, attack.range()) : attack.range();
            Pending p = new Pending(Kind.TELEGRAPH, mob, mob, target, attack, attack.style(), multiplier, marked, now, due, reach,
                    null, null, extra, attack.melee(), false);
            add(p);
            drawTelegraph(p, now);
            return;
        }
        if (sp == AttackDef.Special.SUMMON || sp == AttackDef.Special.HEAL_SELF) {
            land(mob, target, attack, marked, multiplier, false, false); // not a hit on the player
            return;
        }
        ThreatCues.launchSound(mob, attack.style(), sp);
        boolean blinked = false;
        if (sp == AttackDef.Special.BLINK) {
            blinkNextTo(mob, target); // the boss appears beside its target now; its hit follows the wind-up
            blinked = true;
        }
        if (attack.melee() || sp == AttackDef.Special.AOE || sp == AttackDef.Special.BLINK) {
            // playability: OSRS lands a melee hit on the swing tick; here after a 6-10 MC wind-up, on a target still in reach, and
            // 20 MC from a hit of another style already on its way (the swing waits for it: one prayer switch at a time)
            long natural = now + ThreatTiming.meleeWindupMc(mob.getBbWidth());
            long due = singleTarget(sp) ? spacedDue(target, natural, attack.style()) : natural;
            boolean swingNow = attack.melee() && due == natural;
            if (swingNow) {
                mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
            double reach = sp == AttackDef.Special.BLINK ? ThreatTiming.MELEE_REACH
                    : attack.melee() ? Math.max(ThreatTiming.MELEE_REACH, attack.range()) : attack.range();
            Pending p = new Pending(Kind.WINDUP, mob, mob, target, attack, attack.style(), multiplier, marked, now, due, reach, null,
                    null, extra, attack.melee() && sp != AttackDef.Special.AOE, blinked);
            p.swung = !attack.melee() || swingNow;
            add(p);
            cue(p, now);
            if (sp == AttackDef.Special.AOE) {
                ThreatCues.ring(p.level, mob.position(), Math.max(1, attack.param()), ThreatCues.AREA_RGB, extra, false);
            }
            return;
        }
        // playability: OSRS applies a ranged/magic hit after its projectile's hit delay; here the projectile flies 10-24 MC and is
        // judged when it arrives
        projectile(mob, mob, target, attack, attack.style(), multiplier, isBoss(mob), attack.range(), null, extra);
    }

    /**
     * A built-in ranged or magic attack (or any scripted one) delivered as a travelling projectile: {@code resolve} runs when it
     * reaches {@code to} (alive, on the same level, within 10 blocks + 4 and with no wall in the way), else it fizzles.
     */
    public static void deliverProjectile(LivingEntity from, ServerPlayer to, AttackType style, boolean boss, Runnable resolve) {
        double range = from instanceof OsrsMonsterEntity m && m.rangedReach > 0 ? m.rangedReach : 10.0;
        deliverProjectile(from, to, style, boss, range, resolve);
    }

    /** {@link #deliverProjectile(LivingEntity, ServerPlayer, AttackType, boolean, Runnable)} with the attack's range. */
    public static void deliverProjectile(LivingEntity from, ServerPlayer to, AttackType style, boolean boss, double range,
                                         Runnable resolve) {
        if (resolving(from)) {
            resolve.run();
            return;
        }
        ThreatCues.launchSound(from, style, AttackDef.Special.NONE);
        projectile(from, from instanceof OsrsMonsterEntity m ? m : null, to, null, style, 1.0, boss, range, resolve, null);
    }

    /**
     * A built-in melee hit after its wind-up: {@code resolve} runs when it lands on {@code to} (alive, same level, within
     * {@code reach} + 1 blocks of gap and 2 of height), else the swing whooshes past. The caller swings.
     */
    public static void deliverMelee(LivingEntity from, ServerPlayer to, double reach, Runnable resolve) {
        deliverMelee(from, to, reach, resolve, null);
    }

    /** {@link #deliverMelee(LivingEntity, ServerPlayer, double, Runnable)}; {@code after} runs once the swing resolved, hit or miss. */
    public static void deliverMelee(LivingEntity from, ServerPlayer to, double reach, Runnable resolve, Runnable after) {
        if (resolving(from)) {
            resolve.run();
            if (after != null) {
                after.run();
            }
            return;
        }
        long now = ActionManager.mcTick();
        AttackType style = from instanceof OsrsMonsterEntity m && m.stats() != null && m.stats().style().isMelee()
                ? m.stats().style() : AttackType.CRUSH;
        long natural = now + ThreatTiming.meleeWindupMc(from.getBbWidth());
        long due = spacedDue(to, natural, style); // 20 MC from a hit of another style already on its way
        Pending p = new Pending(Kind.WINDUP, from, from instanceof OsrsMonsterEntity m ? m : null, to, null, style, 1.0, to.position(), now,
                due, reach, resolve, after, null, true, false);
        p.swung = due == natural; // the caller swung; a spaced swing is shown again when it is about to land
        add(p);
        cue(p, now);
    }

    private static void projectile(LivingEntity from, OsrsMonsterEntity mob, ServerPlayer target, AttackDef attack, AttackType style,
                                   double multiplier, boolean boss, double range, Runnable resolve, ParticleOptions extra) {
        long now = ActionManager.mcTick();
        Vec3 launch = launchPoint(from);
        double dist = launch.distanceTo(chest(target));
        // landing before the caster's next attack starts (a fast attacker's hits keep their order)
        long latest = from instanceof OsrsMonsterEntity m && m.stats() != null
                ? now + ThreatTiming.latestLandingMc(m.stats().attackSpeed() * ActionManager.MC_TICKS_PER_GAME_TICK) : Long.MAX_VALUE;
        long natural = Math.min(now + ThreatTiming.flightMc(dist, boss), latest);
        long due = Math.min(spacedDue(target, natural, style), Math.max(natural, latest));
        Pending p = new Pending(Kind.PROJECTILE, from, mob, target, attack, style, multiplier, target.position(), now, due, range,
                resolve, null, extra, false, false);
        p.launch = launch;
        p.head = launch;
        add(p);
        ThreatCues.head(p.level, launch, ThreatCues.rgb(style), extra);
        cue(p, now);
    }

    /** Whether an attack hits its one target (spread from other styles), not an area (judged by its ring) or nobody. */
    private static boolean singleTarget(AttackDef.Special sp) {
        return sp != AttackDef.Special.AOE && sp != AttackDef.Special.SKY_STRIKE && sp != AttackDef.Special.SUMMON
                && sp != AttackDef.Special.HEAL_SELF;
    }

    /**
     * The style spacing against every hit already on its way to this target, or landed on it in the last 20 MC (at most
     * {@link ThreatTiming#MAX_SPACING_PUSH_MC} past {@code natural}): projectiles, melee wind-ups and single-target telegraphs alike.
     */
    private static long spacedDue(ServerPlayer target, long natural, AttackType style) {
        List<Long> dues = new ArrayList<>();
        List<AttackType> styles = new ArrayList<>();
        for (Pending p : PENDING) {
            if (!p.done && p.target == target && (p.attack == null || p.attack.special() != AttackDef.Special.SUMMON
                    && p.attack.special() != AttackDef.Special.HEAL_SELF)) {
                dues.add(p.dueMc);
                styles.add(p.style);
            }
        }
        for (Landed l : LANDED) {
            if (l.target() == target) {
                dues.add(l.mc());
                styles.add(l.style());
            }
        }
        if (dues.isEmpty()) {
            return natural;
        }
        long[] at = new long[dues.size()];
        for (int i = 0; i < at.length; i++) {
            at[i] = dues.get(i);
        }
        return ThreatTiming.spacedDue(natural, style, at, styles.toArray(new AttackType[0]));
    }

    private static void add(Pending p) {
        PENDING.add(p);
        pendingListener.created(p.threat);
    }

    private static void cue(Pending p, long now) {
        p.lastDrawMc = now;
        pendingListener.cue(p.threat, now);
    }

    private static boolean isBoss(LivingEntity e) {
        return e instanceof DataBossEntity || e instanceof BossEntity;
    }

    // ------------------------------------------------------------------ one Minecraft tick of a pending hit

    private static void step(Pending p, long now) {
        switch (p.kind) {
            case TELEGRAPH -> stepTelegraph(p, now);
            case WINDUP -> stepWindup(p, now);
            case PROJECTILE -> stepProjectile(p, now);
        }
    }

    private static void finish(Pending p, long now, boolean landed) {
        p.done = true;
        if (landed && (p.attack == null || singleTarget(p.attack.special()))) {
            LANDED.add(new Landed(p.target, now, p.style));
        }
        pendingListener.resolved(p.threat, now, landed);
    }

    private static void stepTelegraph(Pending p, long now) {
        if (!p.from.isAlive()) {
            finish(p, now, false); // a telegraph is cancelled by its caster's death, as before
            return;
        }
        if (now < p.dueMc) {
            if (ThreatTiming.redraw(p.createdMc, now) && p.lastDrawMc != now) {
                drawTelegraph(p, now);
            }
            if (p.attack.melee() && !p.swung && now >= p.dueMc - ThreatTiming.MELEE_SWING_LEAD_MC) {
                p.swung = true;
                p.from.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
            ThreatCues.finalNote(p.from, ThreatTiming.finalNotePitch(now, p.dueMc));
            return;
        }
        if (!telegraphReaches(p)) {
            miss(p);
            finish(p, now, false);
            return;
        }
        finish(p, now, true);
        resolveWith(p, () -> land(p.mob, p.target, p.attack, p.marked, p.multiplier, false, true));
    }

    private static boolean telegraphReaches(Pending p) {
        switch (p.attack.special()) {
            case AOE, SKY_STRIKE, SUMMON, HEAL_SELF -> {
                return true; // area attacks are judged by their ring, the others don't hit the player
            }
            default -> {
            }
        }
        if (!valid(p)) {
            return false;
        }
        if (p.attack.special() == AttackDef.Special.BLINK) {
            return true; // the boss lands beside its target
        }
        if (p.meleeRule) {
            return meleeLands(p.from, p.target, p.reach);
        }
        return ThreatTiming.inRange(edgeDistance(p.from.position(), p.from.getBbWidth(), p.target), p.reach);
    }

    private static void stepWindup(Pending p, long now) {
        if (!p.from.isAlive()) {
            finish(p, now, false); // a wind-up is cancelled by its caster's death
            return;
        }
        if (now < p.dueMc) {
            if (!p.swung && now >= p.dueMc - ThreatTiming.meleeWindupMc(p.from.getBbWidth())) {
                p.swung = true; // a swing spaced from another style's hit: shown one wind-up before it lands
                p.from.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
            if (p.attack != null && p.attack.special() == AttackDef.Special.AOE && ThreatTiming.redraw(p.createdMc, now)
                    && p.lastDrawMc != now) {
                ThreatCues.ring(p.level, p.from.position(), Math.max(1, p.attack.param()), ThreatCues.AREA_RGB, p.particle, false);
                cue(p, now);
            }
            return;
        }
        boolean lands;
        if (p.attack != null && p.attack.special() == AttackDef.Special.AOE) {
            lands = true; // judged by its ring
        } else if (!valid(p)) {
            lands = false;
        } else if (p.meleeRule) {
            lands = meleeLands(p.from, p.target, p.reach);
        } else {
            lands = ThreatTiming.inRange(edgeDistance(p.from.position(), p.from.getBbWidth(), p.target), p.reach);
        }
        finish(p, now, lands);
        if (lands) {
            if (p.resolve != null) {
                resolveWith(p, p.resolve);
            } else {
                resolveWith(p, () -> land(p.mob, p.target, p.attack, p.marked, p.multiplier, p.blinked, true));
            }
        } else {
            miss(p);
        }
        if (p.after != null) {
            p.after.run();
        }
    }

    private static void stepProjectile(Pending p, long now) {
        if (p.from.isRemoved() && p.from.getRemovalReason() != Entity.RemovalReason.KILLED) {
            finish(p, now, false); // its caster is gone (despawned, cleared): a projectile already flying from one that died lands
            return;
        }
        boolean valid = valid(p);
        double span = Math.max(1, p.dueMc - p.createdMc);
        double progress = Math.max(0, Math.min(1, (now - p.createdMc) / span));
        Vec3 aim = valid ? chest(p.target) : p.head;
        Vec3 next = p.launch.add(aim.subtract(p.launch).scale(progress));
        if (valid && now > p.createdMc) {
            Vec3 wall = blockHit(p.level, p.head, next, p.from);
            if (wall != null) {
                ThreatCues.fizzle(p.level, wall);
                finish(p, now, false);
                return;
            }
        }
        p.head = next;
        if (now < p.dueMc) {
            if (p.lastDrawMc != now) {
                ThreatCues.head(p.level, next, ThreatCues.rgb(p.style), p.particle);
                cue(p, now);
            }
            if (!p.impactShown && valid && now >= p.dueMc - ThreatTiming.IMPACT_LEAD_MC) {
                p.impactShown = true;
                ThreatCues.impact(p.level, chest(p.target), p.style, p.particle);
            }
            return;
        }
        Vec3 anchor = p.from.isAlive() ? p.from.position() : p.launch;
        boolean lands = valid && ThreatTiming.inRange(edgeDistance(anchor, p.from.getBbWidth(), p.target), p.reach)
                && blockHit(p.level, p.head, p.target.getEyePosition(), p.from) == null;
        if (!lands) {
            ThreatCues.fizzle(p.level, p.head);
            finish(p, now, false);
            return;
        }
        if (!p.impactShown) {
            ThreatCues.impact(p.level, chest(p.target), p.style, p.particle);
        }
        finish(p, now, true);
        if (p.resolve != null) {
            resolveWith(p, p.resolve);
        } else {
            resolveWith(p, () -> land(p.mob, p.target, p.attack, p.marked, p.multiplier, false, false));
        }
    }

    /** Runs a landing hit with the resolving context set (nested attacks of the same monster land with it). */
    private static void resolveWith(Pending p, Runnable hit) {
        LivingEntity prevMob = resolvingMob;
        AttackDef prevAttack = resolvingAttack;
        resolvingMob = p.from;
        resolvingAttack = p.attack;
        try {
            hit.run();
        } finally {
            resolvingMob = prevMob;
            resolvingAttack = prevAttack;
        }
    }

    /** A hit that found nobody: a whoosh for a swing, a miss splat on the target. */
    private static void miss(Pending p) {
        if (p.kind == Kind.PROJECTILE || p.attack != null && !p.attack.melee()) {
            ThreatCues.fizzle(p.level, p.kind == Kind.PROJECTILE && p.head != null ? p.head : chest(p.target));
        } else {
            ThreatCues.whoosh(p.from);
        }
        if (p.target.isAlive() && p.target.level() == p.level) {
            ServerNetworking.hitsplat(p.target, 0, Net.SPLAT_MISS);
        }
    }

    private static boolean valid(Pending p) {
        return p.target.isAlive() && !p.target.isRemoved() && p.target.level() == p.level;
    }

    private static boolean meleeLands(LivingEntity from, ServerPlayer target, double reach) {
        double gap = ThreatTiming.gap(target.getX() - from.getX(), target.getZ() - from.getZ(), from.getBbWidth(), target.getBbWidth());
        return ThreatTiming.meleeLands(gap, target.getY() - from.getY(), reach);
    }

    /** Distance (blocks) from the edge of a body {@code width} wide at {@code at} to the edge of the target. */
    private static double edgeDistance(Vec3 at, double width, LivingEntity target) {
        return Math.max(0, at.distanceTo(target.position()) - width / 2 - target.getBbWidth() / 2);
    }

    private static Vec3 chest(LivingEntity e) {
        return e.position().add(0, e.getBbHeight() * 0.6, 0);
    }

    /** Where a projectile leaves its caster: its eyes, or lower when those are inside a block (a big boss under a ceiling). */
    private static Vec3 launchPoint(LivingEntity from) {
        Vec3 eye = from.getEyePosition();
        if (!solid((ServerLevel) from.level(), eye)) {
            return eye;
        }
        Vec3 low = from.position().add(0, Math.min(1.0, from.getBbHeight() * 0.5), 0);
        return solid((ServerLevel) from.level(), low) ? from.position().add(0, 0.5, 0) : low;
    }

    private static boolean solid(ServerLevel world, Vec3 at) {
        BlockPos pos = BlockPos.containing(at);
        return !world.getBlockState(pos).getCollisionShape(world, pos).isEmpty();
    }

    /** The point where a straight line first hits a block, or null when it is clear. */
    private static Vec3 blockHit(ServerLevel world, Vec3 from, Vec3 to, Entity context) {
        if (from.distanceToSqr(to) < 1.0E-6) {
            return null;
        }
        BlockHitResult r = world.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context));
        return r.getType() == HitResult.Type.MISS ? null : r.getLocation();
    }

    /** The telegraph's cues: its style halo, and a sky strike's or AoE's ring at its true radius. */
    private static void drawTelegraph(Pending p, long now) {
        AttackDef a = p.attack;
        boolean bright = p.dueMc - now <= ThreatTiming.FINAL_NOTE_MC;
        ThreatCues.halo(p.from, ThreatCues.rgb(a.style()), p.particle, bright);
        if (a.special() == AttackDef.Special.SKY_STRIKE) {
            ThreatCues.ring(p.level, p.marked, ThreatTiming.SKY_STRIKE_RADIUS, ThreatCues.AREA_RGB, p.particle, bright);
        } else if (a.special() == AttackDef.Special.AOE) {
            ThreatCues.ring(p.level, p.from.position(), Math.max(1, a.param()), ThreatCues.AREA_RGB, p.particle, bright);
        }
        cue(p, now);
    }

    private static ParticleOptions particle(AttackDef a, ParticleOptions fallback) {
        if (a.particle() == null) {
            return fallback;
        }
        var type = BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.parse(a.particle()));
        return type instanceof SimpleParticleType simple ? simple : fallback;
    }

    private static void trail(ServerLevel world, Vec3 from, Vec3 to, ParticleOptions effect) {
        Vec3 delta = to.subtract(from);
        int steps = (int) Math.max(4, delta.length() * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(delta.scale(i / (double) steps));
            world.sendParticles(effect, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    /** Players an area attack can reach: the level's own, plus the self-test's ({@link #extraPlayers}). */
    private static List<ServerPlayer> areaPlayers(ServerLevel world) {
        List<ServerPlayer> out = new ArrayList<>(world.getPlayers(pl -> pl.isAlive() && !pl.isSpectator()));
        for (ServerPlayer p : extraPlayers.apply(world)) {
            if (p.isAlive() && p.level() == world && !out.contains(p)) {
                out.add(p);
            }
        }
        return out;
    }

    private static double horizontal(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Lands an attack: its special, then the hit. {@code blinked}: the boss already appeared beside its target; {@code trail}: draw
     * the ranged/magic trail from the monster (a projectile that flew there already showed its way).
     */
    private static void land(OsrsMonsterEntity mob, ServerPlayer target, AttackDef a, Vec3 marked, double multiplier, boolean blinked,
                             boolean trail) {
        ServerLevel world = (ServerLevel) mob.level();
        switch (a.special()) {
            case SUMMON -> {
                if (mob instanceof DataBossEntity boss) {
                    boss.summonMinions(Math.max(1, a.param()));
                }
                return;
            }
            case HEAL_SELF -> {
                mob.heal(a.param());
                world.sendParticles(ParticleTypes.HEART, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 5, 0.5, 0.3, 0.5, 0);
                return;
            }
            case BLINK -> {
                if (!blinked) {
                    blinkNextTo(mob, target);
                }
            }
            case AOE -> {
                world.sendParticles(particle(a, ParticleTypes.EXPLOSION), mob.getX(), mob.getY() + 1, mob.getZ(), 6, a.param() / 2.0, 0.5,
                        a.param() / 2.0, 0);
                // playability: horizontal, from the monster where it stands at landing (the ring drawn around it)
                for (ServerPlayer p : areaPlayers(world)) {
                    if (ThreatTiming.inArea(horizontal(p.position(), mob.position()), p.getY() - mob.getY(), a.param())) {
                        hit(mob, p, a, multiplier);
                    }
                }
                return;
            }
            case SKY_STRIKE -> {
                world.sendParticles(particle(a, ParticleTypes.EXPLOSION), marked.x, marked.y + 0.5, marked.z, 3, 0.5, 0.5, 0.5, 0);
                world.playSound(null, marked.x, marked.y, marked.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.6f, 1.4f);
                // playability: horizontal within the 1.5-block ring drawn on the marked spot
                for (ServerPlayer p : areaPlayers(world)) {
                    if (ThreatTiming.inSkyStrike(horizontal(p.position(), marked), p.getY() - marked.y)) {
                        hit(mob, p, a, multiplier);
                    }
                }
                return;
            }
            default -> {
            }
        }
        if (!a.melee() && trail) {
            trail(world, mob.getEyePosition(), target.position().add(0, 1, 0), particle(a,
                    a.style() == AttackType.MAGIC ? ParticleTypes.WITCH : ParticleTypes.CRIT));
        }
        hit(mob, target, a, multiplier);
    }

    /** One accuracy-rolled hit with all on-hit effects. */
    private static void hit(OsrsMonsterEntity mob, ServerPlayer target, AttackDef a, double multiplier) {
        MonsterStats stats = mob.stats();
        PlayerData data = Players.data(target);
        int attackLevel = switch (a.style()) {
            case MAGIC -> stats.magic();
            case RANGED -> stats.ranged();
            default -> stats.attack();
        };
        int maxHit = (int) Math.round(a.maxHit() * multiplier);
        if (a.special() == AttackDef.Special.DRAGONFIRE) {
            String off = Inv.idOf(target.getOffhandItem());
            boolean shield = off.equals("anti_dragon_shield") || off.startsWith("dragonfire_")
                    || off.equals("elemental_shield") || off.equals("mind_shield") || off.equals("ancient_wyvern_shield");
            boolean antifire = target.hasEffect(MobEffects.FIRE_RESISTANCE);
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
        long attackRoll = com.runecraft.combat.CombatHooks.versusAttackRoll(mob, target, a.style(),
                CombatHandler.npcAttackRoll(mob, stats, a.style(), attackLevel));
        long defenceRoll = CombatHandler.defenceRoll(target, a.style());
        maxHit = com.runecraft.combat.CombatHooks.versusMaxHit(mob, target, a.style(), maxHit);
        boolean alwaysHits = a.special() == AttackDef.Special.DRAGONFIRE || a.special() == AttackDef.Special.AOE
                || a.special() == AttackDef.Special.SKY_STRIKE;
        int damage = alwaysHits ? random.nextInt(Math.max(0, maxHit) + 1) : CombatFormulas.roll(random, attackRoll, defenceRoll, maxHit);
        // the protection prayer is read at impact, through the shared chokepoint (the prayer scope adds its switch grace there)
        boolean protectedHit = CombatHandler.isProtected(data, a.style());
        if (protectedHit && a.special() != AttackDef.Special.PRAYER_PIERCING && a.special() != AttackDef.Special.DRAGONFIRE) {
            damage = 0;
        }
        // the attack that landed: the monster may have started others since (a script's on-hit hook must not read its latest one)
        damage = mob.onHitPlayer(target, damage, a.style(), a);
        com.runecraft.combat.CombatHooks.nameAttack(a.name(), a.style());
        int dealt = CombatHandler.applyDamage(mob, target, damage);
        if (dealt > 0) {
            // As for regular monster hits: Vengeance, rings of recoil and hit-taken charges react to scripted attacks too.
            com.runecraft.combat.CombatHooks.hurt(target, mob, dealt);
        }
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
                // one push straight away from the monster ({@code param} blocks' worth), outside applyDamage so OsrsKnockback lets it be
                double dx = target.getX() - mob.getX();
                double dz = target.getZ() - mob.getZ();
                if (dx * dx + dz * dz < 1.0E-4) {
                    dx = 1;
                    dz = 0;
                }
                target.knockback(Math.max(1, a.param()) * 0.5, -dx, -dz);
                target.hurtMarked = true;
            }
            case FREEZE -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.max(1, a.param()) * 12, 10, false, true));
                Msg.action(target, "You have been frozen!");
            }
            case PULL -> pullNextTo(mob, target);
            case HAZARD -> addHazard((ServerLevel) mob.level(), target.position(), Math.max(1, a.param()), 10, mob);
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

    /** Free spots beside a body: 8 directions around {@code centre}, the first one facing {@code toward}. */
    private static Vec3 freeSpot(ServerLevel world, Entity mover, Vec3 centre, double distance, Vec3 toward) {
        double base = Math.atan2(toward.z - centre.z, toward.x - centre.x);
        for (int i = 0; i < 8; i++) {
            double a = base + (i % 2 == 0 ? 1 : -1) * ((i + 1) / 2) * Math.PI / 4;
            Vec3 spot = new Vec3(centre.x + Math.cos(a) * distance, centre.y, centre.z + Math.sin(a) * distance);
            AABB moved = mover.getBoundingBox().move(spot.subtract(mover.position()));
            if (world.noCollision(mover, moved)) {
                return spot;
            }
        }
        return null;
    }

    /** PULL: the target lands in a free 1x2 space beside the monster, facing it; nowhere free, it stays put. */
    private static void pullNextTo(OsrsMonsterEntity mob, ServerPlayer target) {
        double d = mob.getBbWidth() / 2 + target.getBbWidth() / 2 + 0.5;
        Vec3 spot = freeSpot((ServerLevel) mob.level(), target, mob.position(), d, target.position());
        if (spot != null) {
            target.teleportTo(spot.x, spot.y, spot.z);
        }
    }

    /** BLINK: the monster appears in a free space beside its target, on its own side; nowhere free, it stays put. */
    private static void blinkNextTo(OsrsMonsterEntity mob, ServerPlayer target) {
        ServerLevel world = (ServerLevel) mob.level();
        double d = mob.getBbWidth() / 2 + target.getBbWidth() / 2 + 0.3;
        Vec3 spot = freeSpot(world, mob, target.position(), d, mob.position());
        if (spot == null) {
            return;
        }
        world.sendParticles(ParticleTypes.PORTAL, mob.getX(), mob.getY() + 1, mob.getZ(), 30, 0.5, 1, 0.5, 0.3);
        mob.teleportTo(spot.x, spot.y, spot.z);
        world.sendParticles(ParticleTypes.PORTAL, mob.getX(), mob.getY() + 1, mob.getZ(), 40, 0.5, 1, 0.5, 0.3);
    }

    // ------------------------------------------------------------------ ground hazards

    /** Places a damaging ground hazard (e.g. acid pool, lava splat) for a number of game ticks; it arms 12 MC later. */
    public static void addHazard(ServerLevel world, Vec3 pos, int damage, int gameTicks, LivingEntity owner) {
        HAZARDS.add(new Hazard(world, pos, damage, ActionManager.gameTick() + gameTicks, owner, ActionManager.mcTick()));
    }

    /** Removes every ground hazard inside a box (raid rooms and instances being cleared). */
    public static void clearHazards(ServerLevel world, AABB box) {
        HAZARDS.removeIf(h -> h.world == world && box.contains(h.pos));
    }

    /**
     * Self-test hook (between checks): forgets every hit still on its way to a player inside a box, and the hits just landed there
     * (style spacing), so a projectile from a boss killed at the end of one check doesn't land in the next.
     */
    public static void clearPendingForSelfTest(ServerLevel world, AABB box) {
        PENDING.removeIf(p -> p.level == world && box.contains(p.target.position()));
        LANDED.removeIf(l -> l.target().level() == world && box.contains(l.target().position()));
    }

    /**
     * Every Minecraft tick (playability: OSRS samples a hazard once a game tick under the player's tile): the ring is redrawn every
     * 4 MC (flashing while it arms), and a player standing in it (horizontally within 1 block) is hit once per 12 MC after it armed,
     * the same damage per second as before.
     */
    private static void tickHazards(long nowMc) {
        if (HAZARDS.isEmpty()) {
            return;
        }
        long gameTick = ActionManager.gameTick();
        Iterator<Hazard> it = HAZARDS.iterator();
        List<Runnable> hits = new ArrayList<>();
        while (it.hasNext()) {
            Hazard h = it.next();
            if (gameTick > h.untilGameTick) {
                it.remove();
                continue;
            }
            if (ThreatTiming.redraw(h.placedMc, nowMc)) {
                boolean arming = nowMc < h.armAtMc;
                int rgb = arming && ((nowMc - h.placedMc) / ThreatTiming.REDRAW_MC) % 2 == 1 ? 0xFFFFFF : ThreatCues.HAZARD_RGB;
                ThreatCues.hazardRing(h.world, h.pos, rgb, ParticleTypes.SNEEZE, !arming);
            }
            if (nowMc < h.armAtMc) {
                continue;
            }
            for (ServerPlayer p : areaPlayers(h.world)) {
                if (!ThreatTiming.hazardContact(horizontal(p.position(), h.pos), p.getY() - h.pos.y)) {
                    continue;
                }
                long last = h.lastHit.getOrDefault(p.getUUID(), ThreatTiming.NEVER);
                if (!ThreatTiming.hazardShouldHit(last, nowMc, h.armAtMc)) {
                    continue;
                }
                h.lastHit.put(p.getUUID(), nowMc);
                hits.add(() -> {
                    com.runecraft.combat.CombatHooks.nameAttack("hazard", null);
                    CombatHandler.applyDamage(h.owner != null && h.owner.isAlive() ? h.owner : null, p, 1 + p.getRandom().nextInt(h.damage));
                });
            }
        }
        // after the sweep: a hit's hooks may place or clear hazards
        hits.forEach(Runnable::run);
    }

    /** Picks a weighted attack usable at this distance (falls back to the closest-range attack). */
    public static AttackDef choose(List<AttackDef> attacks, double distance, net.minecraft.util.RandomSource random) {
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
