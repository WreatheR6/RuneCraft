package com.runecraft.content.raids;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHandler;
import com.runecraft.osrs.ArenaTiming;
import com.runecraft.osrs.AttackType;
import com.runecraft.player.Players;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Telegraphed ground strikes and delayed tasks used by raid mechanics (falling crystals, lightning,
 * blood splats, death balls...). Everything runs on the server thread: strikes and tasks land once per game tick, while
 * markers and cues are redrawn every few Minecraft ticks ({@link #redraw}).
 */
public final class RaidEffects {
    /**
     * A marked area that explodes after a delay. {@code protectedShare}: the share of the roll a matching protection
     * prayer lets through (0 blocks it, the default unless piercing; Kephri's fireballs let a third through). The rest is
     * what the arena-cue recorder sees: when it was created and first drawn (raid-clock Minecraft ticks), how often it was
     * drawn, and whether it was created exact (logic that keeps its tick).
     */
    private static final class Strike {
        final ServerLevel world;
        final Vec3 pos;
        /** A lane's far end (a lightning bolt, a wall of fire), or null for a circle around {@link #pos}. */
        final Vec3 to;
        /** A circle's radius, or a lane's half width. */
        final double radius;
        final int minHit;
        final int maxHit;
        final AttackType style;
        final boolean piercing;
        final long due;
        final LivingEntity source;
        final ParticleOptions warn;
        final double protectedShare;
        final boolean exact;
        final long createdMc;
        /** What each player inside takes instead of the roll (a room's own rule), or null. */
        final Consumer<ServerPlayer> onHit;
        /** What happens once it lands, after the players inside were hit (a pool, a spawn), or null. */
        final Runnable onLand;
        /** Checked as it lands: false and it fizzles (its source died, the room moved on), or null. */
        final BooleanSupplier live;
        long firstDrawnMc = -1;
        int draws;
        /** Its area marked on the floor until it lands ({@link Marker#floor}), or null. */
        RaidFloor.Group floor;

        Strike(Marker m, long due, long createdMc) {
            this.world = m.world;
            this.pos = m.pos;
            this.to = m.to;
            this.radius = m.radius;
            this.minHit = m.minHit;
            this.maxHit = m.maxHit;
            this.style = m.style;
            this.piercing = m.piercing;
            this.due = due;
            this.source = m.source;
            this.warn = m.warn == null ? ParticleTypes.FLAME : m.warn;
            this.protectedShare = m.protectedShare;
            this.exact = m.exact;
            this.createdMc = createdMc;
            this.onHit = m.onHit;
            this.onLand = m.onLand;
            this.live = m.live;
        }

        boolean damaging() {
            return maxHit > 0 || onHit != null || onLand != null;
        }

        boolean contains(ServerPlayer p) {
            double dy = p.getY() - pos.y;
            if (to == null) {
                return ArenaTiming.inStrike(p.getX() - pos.x, dy, p.getZ() - pos.z, radius);
            }
            return Math.abs(dy) < ArenaTiming.Y_TOLERANCE
                    && com.runecraft.osrs.content.raids.RaidRealtime.inLane(p.getX(), p.getZ(), pos.x, pos.z, to.x, to.z, radius);
        }

        Vec3 centre() {
            return to == null ? pos : pos.lerp(to, 0.5);
        }
    }

    /**
     * A raid ground strike being set up (round 42): a circle ({@link #mark}) or a lane ({@link #lane}), what it deals, and
     * when. Like every raid strike it is drawn as it is made, redrawn until it lands, recorded, and (unless {@link #exact})
     * lands no sooner than {@link ArenaTiming#STRIKE_FLOOR_TICKS} game ticks after it was made. // playability
     */
    public static final class Marker {
        private final ServerLevel world;
        private final Vec3 pos;
        private Vec3 to;
        private final double radius;
        private int delay = ArenaTiming.STRIKE_FLOOR_TICKS;
        private LivingEntity source;
        private ParticleOptions warn;
        private int minHit;
        private int maxHit;
        private AttackType style;
        private boolean piercing = true;
        private double protectedShare = 1.0;
        private boolean exact;
        private Consumer<ServerPlayer> onHit;
        private Runnable onLand;
        private BooleanSupplier live;
        private com.runecraft.boss.HazardKind floor;

        private Marker(ServerLevel world, Vec3 pos, double radius) {
            this.world = world;
            this.pos = pos;
            this.radius = radius;
        }

        /** Game ticks until it lands (floored for a damaging strike unless {@link #exact}). */
        public Marker delay(int ticks) {
            this.delay = ticks;
            return this;
        }

        public Marker source(LivingEntity source) {
            this.source = source;
            return this;
        }

        /** The particle drawn as its fill. */
        public Marker warn(ParticleOptions warn) {
            this.warn = warn;
            return this;
        }

        /** Typeless damage, {@code min} to {@code max}, that no prayer stops. */
        public Marker damage(int min, int max) {
            this.minHit = Math.max(0, min);
            this.maxHit = Math.max(this.minHit, max);
            return this;
        }

        /** Styled damage: a matching protection prayer lets {@code protectedShare} of the roll through (0: blocks it). */
        public Marker style(AttackType style, double protectedShare) {
            this.style = style;
            this.piercing = false;
            this.protectedShare = Math.max(0, Math.min(1, protectedShare));
            return this;
        }

        /** What each player inside takes, instead of the damage roll. */
        public Marker onHit(Consumer<ServerPlayer> onHit) {
            this.onHit = onHit;
            return this;
        }

        /** What happens once it lands, after the players inside were hit. */
        public Marker onLand(Runnable onLand) {
            this.onLand = onLand;
            return this;
        }

        /** Checked as it lands: when false it fizzles (nothing is hit, nothing happens). */
        public Marker live(BooleanSupplier live) {
            this.live = live;
            return this;
        }

        /**
         * Marks its area on the floor in {@code kind}'s colour from now until it lands (round 43, {@link RaidFloor}). //
         * playability: OSRS tints the tiles about to be struck
         */
        public Marker floor(com.runecraft.boss.HazardKind kind) {
            this.floor = kind;
            return this;
        }

        /** Keeps the delay exact (at least a tick) even when it hurts: logic a room's timing depends on. */
        public Marker exact() {
            this.exact = true;
            return this;
        }

        /** Makes it: drawn now, landing on its (floored) game tick. Returns the game tick it lands on. */
        public long start() {
            return add(this);
        }
    }

    /** A circular raid strike of {@code radius} blocks at {@code pos}, to set up and {@link Marker#start}. */
    public static Marker mark(ServerLevel world, Vec3 pos, double radius) {
        return new Marker(world, pos, radius);
    }

    /**
     * A lane from {@code from} to {@code to}, {@code halfWidth} blocks either side of the line (a lightning bolt across the
     * chamber, a wall of fire), to set up and {@link Marker#start}. Drawn as its outline; it hits horizontally inside it.
     */
    public static Marker lane(ServerLevel world, Vec3 from, Vec3 to, double halfWidth) {
        Marker m = new Marker(world, from, halfWidth);
        m.to = to;
        return m;
    }

    /** A delayed task; {@code anchor}: where it belongs (a projectile's launch point), so a room reset drops it, or null. */
    private record Task(long due, Runnable run, Vec3 anchor) {
    }

    /**
     * A marker redrawn every few Minecraft ticks until the raid clock reaches {@code until} or it says to stop; {@code anchor} as
     * for a task.
     */
    private record Cue(long until, BooleanSupplier draw, Vec3 anchor) {
    }

    private static final List<Strike> STRIKES = new ArrayList<>();
    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Cue> CUES = new ArrayList<>();
    /** Game ticks the raid clock runs ahead of the server's (the self-test fast-forwards raids, see {@link #advance}). */
    private static long skew;
    /** Delayed tasks that threw (logged and skipped); the self-test fails on any. */
    private static int failures;
    private static String lastFailure;
    /** The Minecraft tick the last raid game tick ran on (a strike made between game ticks still gets its full lead). */
    private static long boundaryMc;

    /** Self-test hook: every strike as it lands, with its timing ({@link ArenaTiming.StrikeTrace}, raid-clock Minecraft ticks). */
    public static Consumer<ArenaTiming.StrikeTrace> strikeListener = t -> {
    };

    private RaidEffects() {
    }

    /**
     * The raid clock in game ticks: the server's game tick, plus whatever the self-test fast-forwarded. Every raid timer
     * (delayed tasks, strikes, room deadlines, the raid timer) reads it, so a raid can be driven tick by tick in one go.
     */
    public static long now() {
        return ActionManager.gameTick() + skew;
    }

    /** Self-test: moves the raid clock forward by {@code ticks} game ticks (it never goes back). */
    static void advance(int ticks) {
        skew += Math.max(0, ticks);
    }

    /** Delayed tasks that threw so far (self-test). */
    static int failures() {
        return failures;
    }

    /** The last failed delayed task's exception, or null (self-test). */
    static String lastFailure() {
        return lastFailure;
    }

    /** Runs {@code run} after {@code gameTicks} game ticks. */
    public static void later(int gameTicks, Runnable run) {
        TASKS.add(new Task(now() + Math.max(1, gameTicks), run, null));
    }

    /**
     * Minecraft ticks into the current raid game tick (0 on the game tick itself, and in the self-test's driven raids). On a
     * boundary Minecraft tick the server's game tick ({@link #now}) has already moved on before RaidManager runs this raid
     * game tick, so the count restarts there rather than reading 11 Minecraft ticks into the new game tick.
     */
    private static int mcIntoTick() {
        return ArenaTiming.mcIntoTick(ActionManager.mcTick(), boundaryMc);
    }

    /** The raid clock in Minecraft ticks: game ticks x 12, plus the Minecraft ticks into the current one. */
    public static long clockMc() {
        return now() * ArenaTiming.MC_PER_GAME_TICK + mcIntoTick();
    }

    /**
     * Marks {@code pos}; after {@code delay} game ticks every player within {@code radius} takes up to
     * {@code maxHit} (protection prayers block it unless piercing).
     */
    public static void strike(ServerLevel world, Vec3 pos, double radius, int maxHit, int delay, LivingEntity source,
                              ParticleOptions warn, AttackType style, boolean piercing) {
        Marker m = mark(world, pos, radius).delay(delay).source(source).warn(warn).damage(0, maxHit);
        if (!piercing) {
            m.style(style, 0.0);
        }
        m.style = style;
        m.start();
    }

    /**
     * {@link #strike} whose damage a matching protection prayer only reduces: {@code protectedShare} of the roll still
     * lands (at least Quiet Prayers' share in ToA).
     */
    public static void strike(ServerLevel world, Vec3 pos, double radius, int maxHit, int delay, LivingEntity source,
                              ParticleOptions warn, AttackType style, double protectedShare) {
        mark(world, pos, radius).delay(delay).source(source).warn(warn).damage(0, maxHit).style(style, protectedShare).start();
    }

    public static void strike(ServerLevel world, Vec3 pos, double radius, int maxHit, int delay, LivingEntity source,
                              ParticleOptions warn) {
        strike(world, pos, radius, maxHit, delay, source, warn, null, true);
    }

    /**
     * A strike that keeps its exact OSRS delay (at least a tick) even when it deals damage: for chained logic whose timing a
     * room depends on. Ordinary strikes are floored ({@link ArenaTiming#strikeDue}).
     */
    public static void strikeExact(ServerLevel world, Vec3 pos, double radius, int maxHit, int delay, LivingEntity source,
                                   ParticleOptions warn, AttackType style, boolean piercing) {
        Marker m = mark(world, pos, radius).delay(delay).source(source).warn(warn).damage(0, maxHit).exact();
        if (!piercing) {
            m.style(style, 0.0);
        }
        m.style = style;
        m.start();
    }

    /**
     * Every strike: a damaging one lands no sooner than {@link ArenaTiming#STRIKE_FLOOR_TICKS} game ticks (1.8 s) after it
     * is made, and its marker is drawn at once at its true radius, then redrawn every few Minecraft ticks ({@link #redraw}).
     * // playability: OSRS lands raid strikes from a tick after marking them, and the marker here was first drawn a game
     * tick late
     */
    private static long add(Marker m) {
        long now = now();
        int into = mcIntoTick();
        int d = Math.max(1, m.delay);
        boolean damaging = m.maxHit > 0 || m.onHit != null || m.onLand != null;
        long due = m.exact ? now + d : ArenaTiming.strikeDue(now, d, damaging, into);
        Strike s = new Strike(m, due, now * ArenaTiming.MC_PER_GAME_TICK + into);
        if (m.floor != null) {
            s.floor = s.to == null ? RaidFloor.disc(m.world, m.pos, m.radius, m.floor, 0, m.source)
                    : RaidFloor.lane(m.world, m.pos, m.to, m.radius, m.floor, 0, m.source);
        }
        STRIKES.add(s);
        draw(s, false);
        if (damaging) { // round 43: on the cast bar of the players near it (a lane: near its line), counting down to its landing
            com.runecraft.network.HudNet.groundCast(s.world, p -> s.to == null
                    ? com.runecraft.osrs.CastBar.nearStrike(p.getX() - s.pos.x, p.getY() - s.pos.y, p.getZ() - s.pos.z, s.radius)
                    : Math.abs(p.getY() - s.pos.y) < com.runecraft.osrs.CastBar.NEAR_DY && com.runecraft.osrs.content.raids.RaidRealtime.inLane(
                            p.getX(), p.getZ(), s.pos.x, s.pos.z, s.to.x, s.to.z, s.radius + com.runecraft.osrs.CastBar.NEAR_MARGIN),
                    s.source, s.style, com.runecraft.osrs.CastBar.landInMc(due, now, into), s.source == null ? "Strike" : null);
        }
        return due;
    }

    /** Draws a pending strike's marker: the outline at its true radius (a lane's two edges), its warn particle as a fill. */
    private static void draw(Strike s, boolean landing) {
        if (s.firstDrawnMc < 0) {
            s.firstDrawnMc = clockMc();
        }
        s.draws++;
        // the same outline as every other arena's strikes: typeless/AoE orange, red-orange in its last moments
        if (s.to == null) {
            com.runecraft.boss.StrikeMarkers.ring(s.world, s.pos, s.radius, s.warn, landing, ArenaTiming.ringPoints(s.radius, STRIKES.size()));
            return;
        }
        drawLane(s.world, s.pos, s.to, s.radius, landing ? com.runecraft.boss.StrikeMarkers.OUTLINE_LANDING
                : com.runecraft.boss.StrikeMarkers.OUTLINE, s.warn, STRIKES.size());
    }

    /**
     * A lane's outline: both edges {@code halfWidth} either side of the line from {@code a} to {@code b}, about two points a
     * block (fewer when many markers are up), and its fill particle sparsely along the middle.
     */
    static void drawLane(ServerLevel world, Vec3 a, Vec3 b, double halfWidth, ParticleOptions edge, ParticleOptions fill, int pending) {
        Vec3 d = b.subtract(a).multiply(1, 0, 1);
        double len = d.length();
        if (len < 1e-3) {
            com.runecraft.boss.StrikeMarkers.ring(world, a, halfWidth, fill, false, ArenaTiming.ringPoints(halfWidth, pending));
            return;
        }
        Vec3 dir = d.scale(1 / len);
        Vec3 side = new Vec3(-dir.z, 0, dir.x).scale(halfWidth);
        int points = (int) Math.max(4, Math.min(24, Math.ceil(len * 2)));
        if (pending > 6) {
            points = Math.max(4, points * 6 / pending);
        }
        for (int i = 0; i <= points; i++) {
            Vec3 at = a.lerp(b, i / (double) points);
            world.sendParticles(edge, at.x + side.x, at.y + 0.1, at.z + side.z, 1, 0, 0, 0, 0);
            world.sendParticles(edge, at.x - side.x, at.y + 0.1, at.z - side.z, 1, 0, 0, 0, 0);
            if (fill != null && i % 3 == 0) {
                world.sendParticles(fill, at.x, at.y + 0.15, at.z, 1, halfWidth * 0.4, 0.02, halfWidth * 0.4, 0);
            }
        }
    }

    // ------------------------------------------------------------------ projectiles and safe spots (round 42)

    /** What a raid projectile recorder sees (self-tests): when it left and landed (raid-clock MC), its style, if it landed. */
    public record ProjectileTrace(long createdMc, long landedMc, AttackType style, boolean landed) {
        public long flightMc() {
            return landedMc - createdMc;
        }
    }

    /** Self-test hook: every raid projectile as it lands or fizzles. */
    public static Consumer<ProjectileTrace> projectileListener = t -> {
    };

    /** Per player: the game tick the last raid projectile at them lands, and its style (spacing, see RaidRealtime). */
    private static final java.util.Map<java.util.UUID, long[]> LAST_PROJECTILE = new java.util.HashMap<>();

    /**
     * A raid projectile from {@code from} at {@code to} (a skull, an orb, a divine bolt): drawn as a style-coloured head and
     * tail homing on the target, redrawn every few Minecraft ticks, and landing after {@code flightTicks} game ticks (at
     * least {@link com.runecraft.osrs.content.raids.RaidRealtime#PROJECTILE_MIN_TICKS}; a projectile of another style already
     * on its way to the same player keeps {@link com.runecraft.osrs.content.raids.RaidRealtime#SEQUENCE_GAP_TICKS} ahead of
     * it), when {@code land} runs if {@code live} still holds and the target is alive. The protection prayer is read when it
     * lands, by {@code land}. Returns the game tick it lands on. // playability: OSRS draws its projectiles too, but rolls
     * some of them as they leave and flies them a tick or two
     */
    public static long projectile(ServerLevel world, Vec3 from, ServerPlayer to, AttackType style, int flightTicks, BooleanSupplier live,
                                  Runnable land) {
        long now = now();
        int into = mcIntoTick();
        int flight = Math.max(com.runecraft.osrs.content.raids.RaidRealtime.PROJECTILE_MIN_TICKS, flightTicks);
        long want = now + flight + (into > 0 ? 1 : 0);
        long[] last = LAST_PROJECTILE.get(to.getUUID());
        long due = last == null || last[0] < now ? want
                : com.runecraft.osrs.content.raids.RaidRealtime.spacedTick(want, last[0], last[1] != styleKey(style));
        if (last == null || due >= last[0]) {
            LAST_PROJECTILE.put(to.getUUID(), new long[]{due, styleKey(style)});
        }
        long createdMc = now * ArenaTiming.MC_PER_GAME_TICK + into;
        int rgb = style == null ? com.runecraft.util.Msg.TYPELESS_RGB : style.isMelee() ? com.runecraft.util.Msg.MELEE_RGB
                : style == AttackType.MAGIC ? com.runecraft.util.Msg.MAGIC_RGB : com.runecraft.util.Msg.RANGED_RGB;
        net.minecraft.core.particles.DustParticleOptions dust = new net.minecraft.core.particles.DustParticleOptions(
                new org.joml.Vector3f(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f), 1.6f);
        double flightMc = Math.max(1, due * ArenaTiming.MC_PER_GAME_TICK - createdMc);
        // round 43: on the target's cast bar in its style, with the key that blocks it as it lands
        com.runecraft.network.HudNet.cast(to, null, com.runecraft.osrs.CastBar.styleOf(style),
                style == null ? com.runecraft.osrs.CastBar.FLAG_UNPRAYABLE : 0, (int) flightMc, "Incoming");
        cue((int) (due - now) + 1, from, () -> {
            if (!to.isAlive() || (live != null && !live.getAsBoolean())) {
                return false;
            }
            double f = Math.min(1.0, (clockMc() - createdMc) / flightMc);
            Vec3 target = to.position().add(0, 1.0, 0);
            for (int k = 0; k < 3; k++) {
                Vec3 at = from.lerp(target, Math.max(0, f - k * 0.08));
                world.sendParticles(dust, at.x, at.y, at.z, 2, 0.05, 0.05, 0.05, 0.0);
            }
            return f < 1.0;
        });
        TASKS.add(new Task(due, () -> {
            boolean ok = to.isAlive() && (live == null || live.getAsBoolean());
            projectileListener.accept(new ProjectileTrace(createdMc, now() * ArenaTiming.MC_PER_GAME_TICK, style, ok));
            if (ok) {
                land.run();
            }
        }, from));
        return due;
    }

    private static long styleKey(AttackType style) {
        return style == null ? -1 : style.isMelee() ? 0 : style == AttackType.MAGIC ? 2 : 1;
    }

    /** Pale green: a safe tile or spot (the lightning skulls' cross, Zebak's shelter, a slam's safe rows). */
    public static final net.minecraft.core.particles.DustParticleOptions SAFE =
            new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.45f, 1.0f, 0.45f), 1.4f);

    /** Marks one block-sized tile (its standing position {@code at}) as safe: a green square outline. */
    public static void safeTile(ServerLevel world, Vec3 at) {
        double y = at.y + 0.1;
        for (int i = 0; i < 4; i++) {
            double f = -0.4 + 0.8 * i / 3.0;
            world.sendParticles(SAFE, at.x + f, y, at.z - 0.4, 1, 0, 0, 0, 0);
            world.sendParticles(SAFE, at.x + f, y, at.z + 0.4, 1, 0, 0, 0, 0);
            world.sendParticles(SAFE, at.x - 0.4, y, at.z + f, 1, 0, 0, 0, 0);
            world.sendParticles(SAFE, at.x + 0.4, y, at.z + f, 1, 0, 0, 0, 0);
        }
    }

    /**
     * A creature about to explode where it stands when its fuse runs out (a lizardman spawn, a grounded vespine soldier, an
     * old nylocas): the blast's ring of {@code radius} around it, following it, for its last {@code gameTicks} game ticks,
     * brightening at the end. // playability: OSRS shows the creature swelling, and the blast is tile-exact
     */
    public static void fuse(LivingEntity e, double radius, int gameTicks) {
        long until = now() + gameTicks;
        cue(gameTicks, () -> {
            if (!e.isAlive() || e.isRemoved()) {
                return false;
            }
            boolean landing = ArenaTiming.finalCue(until * ArenaTiming.MC_PER_GAME_TICK, clockMc());
            com.runecraft.boss.StrikeMarkers.ring((ServerLevel) e.level(), e.position(), radius, null, landing,
                    ArenaTiming.ringPoints(radius, 1));
            return true;
        });
    }

    /** Says {@code text} to each of {@code players} as a centre-screen warning (colour by the style it names). */
    public static void telegraph(Iterable<ServerPlayer> players, String text) {
        for (ServerPlayer p : players) {
            com.runecraft.util.Msg.telegraph(p, text, null);
        }
    }

    /**
     * Redraws every pending strike marker and cue. RaidManager calls it every {@link ArenaTiming#REDRAW_MC} Minecraft ticks,
     * between the game ticks that land them. In a strike's last {@link ArenaTiming#FINAL_CUE_MC} Minecraft ticks its outline
     * brightens and a bell ticks, rising as it nears. // playability: OSRS draws the marker once
     */
    public static void redraw() {
        long mc = clockMc();
        long now = now();
        CUES.removeIf(c -> now >= c.until());
        for (Cue c : new ArrayList<>(CUES)) {
            boolean keep;
            try {
                keep = c.draw().getAsBoolean();
            } catch (RuntimeException e) {
                keep = false;
                com.runecraft.RuneCraft.LOGGER.error("Raid cue failed", e);
            }
            if (!keep) {
                CUES.remove(c);
            }
        }
        Strike bell = null;
        for (Strike s : STRIKES) {
            boolean landing = ArenaTiming.finalCue(s.due * ArenaTiming.MC_PER_GAME_TICK, mc);
            draw(s, landing);
            if (landing && s.damaging() && (bell == null || s.due < bell.due)) {
                bell = s;
            }
        }
        if (bell != null) {
            com.runecraft.boss.StrikeMarkers.bell(bell.world, bell.centre(), bell.due * ArenaTiming.MC_PER_GAME_TICK - mc);
        }
    }

    /**
     * A marker (gap columns, safe edges, a projectile's trail) drawn now, then every {@link ArenaTiming#REDRAW_MC} Minecraft
     * ticks for {@code gameTicks} game ticks, or until {@code draw} returns false.
     */
    public static void cue(int gameTicks, BooleanSupplier draw) {
        cue(gameTicks, null, draw);
    }

    /** {@link #cue(int, BooleanSupplier)} belonging to a place ({@code anchor}): a reset of the room holding it drops it. */
    public static void cue(int gameTicks, Vec3 anchor, BooleanSupplier draw) {
        if (draw.getAsBoolean()) {
            CUES.add(new Cue(now() + Math.max(1, gameTicks), draw, anchor));
        }
    }

    /**
     * Drops everything pending inside a box (round 43: a room that resets after a ToA or ToB Entry wipe, a raid closing): its
     * strikes and their floor marks, the projectiles launched there and their trails, anchored cues, the projectile spacing of
     * the players there, and the raid floor marks. A strike or projectile from before a wipe never lands on the retry.
     */
    public static void clear(ServerLevel world, net.minecraft.world.phys.AABB box) {
        STRIKES.removeIf(s -> {
            if (s.world == world && box.contains(s.pos)) {
                if (s.floor != null) {
                    s.floor.release();
                }
                return true;
            }
            return false;
        });
        TASKS.removeIf(t -> t.anchor() != null && box.contains(t.anchor()));
        CUES.removeIf(c -> c.anchor() != null && box.contains(c.anchor()));
        for (ServerPlayer p : world.players()) {
            if (box.contains(p.position())) {
                LAST_PROJECTILE.remove(p.getUUID());
            }
        }
        RaidFloor.clear(world, box);
    }

    /** Self-test: strikes, projectiles, anchored cues and floor marks still pending inside a box. */
    static int pendingIn(ServerLevel world, net.minecraft.world.phys.AABB box) {
        int n = 0;
        for (Strike s : STRIKES) {
            n += s.world == world && box.contains(s.pos) ? 1 : 0;
        }
        for (Task t : TASKS) {
            n += t.anchor() != null && box.contains(t.anchor()) ? 1 : 0;
        }
        for (Cue c : CUES) {
            n += c.anchor() != null && box.contains(c.anchor()) ? 1 : 0;
        }
        return n + RaidFloor.liveIn(world, box);
    }

    /** Pending strikes whose area holds {@code p} right now (self-tests: what a player stands in). */
    static int strikesOn(ServerPlayer p) {
        int n = 0;
        for (Strike s : STRIKES) {
            if (s.world == p.level() && s.contains(p)) {
                n++;
            }
        }
        return n;
    }

    public static void gameTick() {
        long now = now();
        boundaryMc = ActionManager.mcTick();
        CUES.removeIf(c -> now >= c.until());
        RaidFloor.tick(now);
        if (!LAST_PROJECTILE.isEmpty()) {
            LAST_PROJECTILE.values().removeIf(v -> v[0] < now);
        }
        List<Task> dueTasks = new ArrayList<>();
        for (Iterator<Task> it = TASKS.iterator(); it.hasNext(); ) {
            Task t = it.next();
            if (now >= t.due()) {
                dueTasks.add(t);
                it.remove();
            }
        }
        for (Task t : dueTasks) {
            try {
                t.run().run();
            } catch (RuntimeException e) {
                failures++;
                lastFailure = e.toString();
                com.runecraft.RuneCraft.LOGGER.error("Raid task failed", e);
            }
        }
        List<Strike> due = new ArrayList<>();
        for (Iterator<Strike> it = STRIKES.iterator(); it.hasNext(); ) {
            Strike s = it.next();
            if (now >= s.due) {
                due.add(s);
                it.remove();
                if (s.floor != null) {
                    s.floor.release();
                }
            }
        }
        for (Strike s : due) {
            strikeListener.accept(new ArenaTiming.StrikeTrace(s.createdMc, s.firstDrawnMc, now * ArenaTiming.MC_PER_GAME_TICK,
                    s.draws, s.radius, s.maxHit, s.damaging(), s.exact));
            try {
                land(s);
            } catch (RuntimeException e) {
                failures++;
                lastFailure = e.toString();
                com.runecraft.RuneCraft.LOGGER.error("Raid strike failed", e);
            }
        }
    }

    /** A strike lands: unless it fizzled, everyone inside its outline takes its roll (or its own rule), then its aftermath. */
    private static void land(Strike s) {
        if (s.live != null && !s.live.getAsBoolean()) {
            return;
        }
        Vec3 c = s.centre();
        if (s.to == null) {
            s.world.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.5, c.z, 1, 0, 0, 0, 0);
        } else {
            drawLane(s.world, s.pos, s.to, s.radius * 0.5, ParticleTypes.ELECTRIC_SPARK, null, 0);
        }
        s.world.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.4f, 1.5f);
        // What is drawn is what hits: horizontally inside the outline, within its height tolerance. // playability
        boolean roll = s.onHit == null && (s.maxHit > 0 || s.onLand == null);
        for (ServerPlayer p : s.world.getPlayers(pl -> pl.isAlive() && !pl.isSpectator() && !pl.isCreative() && s.contains(pl))) {
            if (s.onHit != null) {
                s.onHit.accept(p);
            } else if (roll) {
                int dmg = s.minHit + p.getRandom().nextInt(Math.max(1, s.maxHit - s.minHit + 1));
                hit(s.source, p, dmg, s.style, s.piercing ? 1.0 : s.protectedShare);
            }
        }
        if (s.onLand != null) {
            s.onLand.run();
        }
    }

    public static double horizontalDistSq(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    /**
     * Deals damage from a raid mechanic. A matching protection prayer blocks it unless piercing.
     * Returns the damage dealt.
     */
    public static int hit(LivingEntity source, ServerPlayer player, int damage, AttackType style, boolean piercing) {
        return hit(source, player, damage, style, piercing ? 1.0 : 0.0);
    }

    /**
     * Deals damage from a raid mechanic; a matching protection prayer lets {@code protectedShare} of it through (0: blocks
     * it, 1: ignores the prayer). Returns the damage dealt.
     */
    public static int hit(LivingEntity source, ServerPlayer player, int damage, AttackType style, double protectedShare) {
        RaidInstance raid = RaidManager.of(player);
        if (raid != null) {
            if (raid.isDowned(player)) {
                return 0;
            }
            raid.noteAttack();
        }
        if (protectedShare < 1.0 && style != null && protectedFrom(player, style)) {
            // ToA Quiet Prayers (OSRS): protection prayers are 10% less effective.
            double leak = raid != null && raid.type == com.runecraft.osrs.content.raids.RaidType.TOA
                    ? com.runecraft.osrs.content.raids.Invocation.protectionLeak(raid.invocations()) : 0;
            damage = (int) Math.round(damage * Math.max(protectedShare, leak));
        }
        if (raid != null) {
            damage = (int) Math.round(damage * raid.damageMultiplier(source));
        }
        LivingEntity attacker = source != null && source.isAlive() ? source : null;
        int dealt = applyScaled(attacker, player, damage);
        if (dealt > 0) {
            RaidManager.hurt(player, dealt);
            RaidManager.deadlyPrayers(player, dealt);
        }
        return dealt;
    }

    /** True while a raid mechanic applies damage it has already scaled (see {@link RaidManager}'s minion scaling). */
    private static boolean applying;

    static boolean applyingScaled() {
        return applying;
    }

    private static int applyScaled(LivingEntity attacker, ServerPlayer player, int damage) {
        boolean was = applying;
        applying = true;
        try {
            return CombatHandler.applyDamage(attacker, player, damage);
        } finally {
            applying = was;
        }
    }

    /**
     * Like {@link #hit} but without the raid's damage multiplier (for damage that is already scaled or capped, such as
     * Akkha's unstable orbs).
     */
    public static int hitRaw(LivingEntity source, ServerPlayer player, int damage) {
        RaidInstance raid = RaidManager.of(player);
        if (raid != null) {
            if (raid.isDowned(player)) {
                return 0;
            }
            raid.noteAttack();
        }
        LivingEntity attacker = source != null && source.isAlive() ? source : null;
        int dealt = applyScaled(attacker, player, damage);
        if (dealt > 0) {
            RaidManager.hurt(player, dealt);
            RaidManager.deadlyPrayers(player, dealt);
        }
        return dealt;
    }

    /**
     * Whether the player prays against {@code style}, read through the one protection chokepoint
     * ({@link CombatHandler#isProtected}, which the prayer scope gives its switch grace), so raid checks agree with every
     * other hit.
     */
    public static boolean protectedFrom(ServerPlayer player, AttackType style) {
        return style != null && CombatHandler.isProtected(Players.data(player), style);
    }
}
