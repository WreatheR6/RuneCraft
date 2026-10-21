package com.runecraft.content.raids;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.content.systems.SysCombat;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.raids.Invocation;
import com.runecraft.osrs.content.raids.RaidRealtime;
import com.runecraft.osrs.content.raids.RaidScaling;
import com.runecraft.osrs.content.raids.RoomDef;
import com.runecraft.osrs.content.raids.ToaBossRules;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The Wardens (OSRS wiki: Elidinis' Warden, Tumeken's Warden, Obelisk, Core, Energy Siphon and "Tombs of
 * Amascut/Strategies"). The room faces east: players come down at the west end, the obelisk stands at the east end
 * with Elidinis' Warden north of it and Tumeken's Warden south, each fed by an energy trough.
 * <ol>
 * <li>Phase 1: attack the obelisk while it sends orbs of energy along both troughs. Standing on a trough absorbs its
 * orbs (3 damage each) and slows that Warden's charge; a charged Warden alternates UFOs and a Charged Shot. When the
 * obelisk falls, the Warden whose charge was disrupted most is destroyed and the other one wakes.</li>
 * <li>Phase 2: the Warden prays against melee and one other style (Tumeken's magic first, Elidinis' ranged first,
 * swapping after each core), attacks with all three styles, a Divine Projectile above 55% and Imprisonment below. Hits
 * on its body fill a bar that ejects the core; core damage counts five times. The obelisk fires its Converging Beam,
 * Windmill Beam and Lightning Skull in turn. When the core's pool is empty the other Warden is restored.</li>
 * <li>Phase 3: the restored Warden slams the floor to its right, its left, then both sides; at 80/60/40/20% it launches
 * energy siphons (melee to reverse them before they detonate); phantoms of two path bosses join at the second and
 * third set; at 5% it heals 20% once and enrages: lightning strikes and the phantom rips the floor away row by row.</li>
 * </ol>
 */
final class ToaWardens extends RoomLogic {
    /** Live cores (melee always hits them for its maximum, OSRS). */
    static final Set<LivingEntity> CORES = Collections.newSetFromMap(new WeakHashMap<>());

    /** Room layout (room-relative tiles): the obelisk, the Wardens' plinths, the troughs and the fighting spots. */
    static final int OBELISK_X = 14;
    static final int WARDEN_Z = 12;
    static final int TROUGH_FROM = 2;
    static final int TROUGH_TO = 10;
    static final int P2_X = 8;
    /** Energy a Warden needs for a charged attack (an unblocked orb brings 4, an absorbed one 1). // approx */
    static final int FULL_CHARGE = 48;
    static final int ORB_ENERGY = 4;
    static final int BLOCKED_ORB_ENERGY = 1;
    /** Damage a player takes for each orb absorbed (OSRS). */
    static final int ORB_ABSORB_DAMAGE = 3;

    int phase;
    DataBossEntity obelisk;
    /** Elidinis' Warden (north) and Tumeken's Warden (south). */
    DataBossEntity elidinis;
    DataBossEntity tumeken;
    /** The phase 2 Warden, then the restored phase 3 Warden. */
    DataBossEntity warden;
    private long startTick;

    // phase 1
    /** Orbs in flight: side (0 Elidinis, 1 Tumeken), distance travelled along the trough (tiles), absorbed (0/1). */
    private final List<double[]> orbs = new ArrayList<>();
    final int[] charge = new int[2];
    final int[] absorbed = new int[2];
    private final int[] chargedAttacks = new int[2];
    int wardenAttacks;

    // phase 2
    private int p2Hp;
    private int p2Max;
    private int bar;
    private long lastBodyHit;
    private LivingEntity core;
    private int coreTicks;
    int coresEjected;
    /** The style the phase 2 Warden prays against besides melee. */
    AttackType prayed;
    private int p2Attacks;
    private long nextP2Attack;
    private int obeliskPattern;
    private long nextObeliskAttack;
    /** Windmill beam: remaining ticks and current angle (degrees). */
    private int windmillTicks;
    private double windmillAngle;
    private final Map<UUID, Long> beamHit = new HashMap<>();

    // phase 3
    private int slam;
    private long nextSlam;
    int siphonSets;
    private final List<LivingEntity> siphons = new ArrayList<>();
    private long siphonDeadline;
    boolean siphonsUp;
    int siphonsFailed;
    final List<String> phantoms = new ArrayList<>();
    /** The phantoms themselves (unattackable apparitions behind the Warden), in {@link #phantoms} order. */
    final List<LivingEntity> phantomMobs = new ArrayList<>();
    private final int[] phantomAttacks = new int[2];
    private final long[] phantomNext = new long[2];
    /** Attacks the phantoms made (self-test). */
    int phantomAttackCount;
    boolean enraged;
    private int rippedColumns;
    private long nextRip;

    ToaWardens(RaidInstance raid, RoomDef def, int index) {
        super(raid, def, index);
    }

    /** Players come down the stairs from the Nexus; the room has no west door. */
    @Override
    public boolean entersFromWest() {
        return false;
    }

    /** Whether the fight is in its final phase (Warden damage worth 2.5 points, OSRS). */
    boolean phaseThree() {
        return phase == 3;
    }

    /** Whether this creature is a phase 2 Warden's ejected core. */
    static boolean isCore(LivingEntity e) {
        return e != null && CORES.contains(e);
    }

    /** OSRS: attacks on the phase 2 Warden always hit (their damage still follows the attack's accuracy). */
    boolean alwaysAccurate(LivingEntity target) {
        return phase == 2 && target == warden;
    }

    private static String name(DataBossEntity w) {
        return w == null || w.def() == null ? "The Warden" : w.def().name;
    }

    private boolean isElidinis(DataBossEntity w) {
        return w != null && w == elidinis;
    }

    // ------------------------------------------------------------------ the room

    @Override
    public void decorate() {
        BlockPos c = center();
        int y = c.getY();
        // The obelisk's dais and the Wardens' plinths.
        RaidBuilder.fill(world(), c.getX() + OBELISK_X - 1, y, c.getZ() - 1, c.getX() + OBELISK_X + 1, y, c.getZ() + 1,
                Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
        RaidBuilder.fill(world(), c.getX() + OBELISK_X - 2, y, c.getZ() - WARDEN_Z - 2, c.getX() + OBELISK_X + 2, y, c.getZ() - WARDEN_Z + 1,
                Blocks.PRISMARINE_BRICKS.defaultBlockState());
        RaidBuilder.fill(world(), c.getX() + OBELISK_X - 2, y, c.getZ() + WARDEN_Z - 1, c.getX() + OBELISK_X + 2, y, c.getZ() + WARDEN_Z + 2,
                Blocks.CUT_RED_SANDSTONE.defaultBlockState());
        // The energy troughs: Elidinis' to the north, Tumeken's to the south.
        RaidBuilder.fill(world(), c.getX() + OBELISK_X, y, c.getZ() - TROUGH_TO, c.getX() + OBELISK_X, y, c.getZ() - TROUGH_FROM,
                Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA.defaultBlockState());
        RaidBuilder.fill(world(), c.getX() + OBELISK_X, y, c.getZ() + TROUGH_FROM, c.getX() + OBELISK_X, y, c.getZ() + TROUGH_TO,
                Blocks.ORANGE_GLAZED_TERRACOTTA.defaultBlockState());
        // The stairs down from the Nexus.
        RaidBuilder.fill(world(), c.getX() - RaidBuilder.HALF + 1, y, c.getZ() - 2, c.getX() - RaidBuilder.HALF + 3, y, c.getZ() + 2,
                Blocks.SMOOTH_SANDSTONE.defaultBlockState());
    }

    @Override
    public void start() {
        phase = 1;
        startTick = RaidEffects.now();
        orbs.clear();
        charge[0] = charge[1] = 0;
        absorbed[0] = absorbed[1] = 0;
        chargedAttacks[0] = chargedAttacks[1] = 0;
        wardenAttacks = 0;
        warden = null;
        core = null;
        coreTicks = 0;
        coresEjected = 0;
        bar = 0;
        obeliskPattern = 0;
        windmillTicks = 0;
        beamHit.clear();
        slam = 0;
        siphonSets = 0;
        siphons.clear();
        siphonsUp = false;
        siphonsFailed = 0;
        clearPhantoms();
        phantomAttacks[0] = phantomAttacks[1] = 0;
        phantomAttackCount = 0;
        enraged = false;
        rippedColumns = 0;
        ripWarnedFor = -1;
        obelisk = spawnBoss("wardens_obelisk", at(OBELISK_X, 0));
        elidinis = spawnBoss("elidinis_warden", at(OBELISK_X, -WARDEN_Z));
        tumeken = spawnBoss("tumekens_warden", at(OBELISK_X, WARDEN_Z));
        for (DataBossEntity b : new DataBossEntity[]{obelisk, elidinis, tumeken}) {
            if (b != null) {
                // Every attack in this fight is the room's.
                b.setNoAi(true);
                anchor(b);
            }
        }
        warn("The obelisk begins to channel energy into the Wardens! Attack it - and block a trough to slow a Warden's charge "
                + "(Elidinis' Warden north, Tumeken's south).");
    }

    @Override
    public DataBossEntity hudBoss() {
        DataBossEntity b = phase == 1 ? obelisk : warden;
        return alive(b) ? b : super.hudBoss();
    }

    private void clearPhantoms() {
        for (LivingEntity m : phantomMobs) {
            m.discard();
        }
        phantomMobs.clear();
        phantoms.clear();
    }

    @Override
    public void reset() {
        dropCore();
        clearSiphons();
        clearPhantoms();
        restoreFloor();
        super.reset();
        obelisk = elidinis = tumeken = warden = null;
    }

    @Override
    public void cleanup() {
        dropCore();
        clearSiphons();
        clearPhantoms();
        if (raid.world.getServer() != null) {
            restoreFloor();
        }
        super.cleanup();
    }

    private void dropCore() {
        if (core != null) {
            CORES.remove(core);
            core.discard();
            core = null;
        }
        coreTicks = 0;
    }

    private void clearSiphons() {
        for (LivingEntity s : siphons) {
            s.discard();
        }
        siphons.clear();
        siphonsUp = false;
    }

    // ------------------------------------------------------------------ boss hooks

    @Override
    public boolean bossVulnerable(DataBossEntity boss) {
        return switch (phase) {
            case 1 -> boss == obelisk;
            case 2 -> boss == warden;
            case 3 -> boss == warden && !siphonsUp;
            default -> false;
        };
    }

    @Override
    public int bossIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
        if (!bossVulnerable(boss)) {
            return 0;
        }
        if (phase != 2 || damage <= 0) {
            return damage;
        }
        // OSRS: always Protect from Melee, plus magic or ranged.
        if (type.isMelee() || type == prayed) {
            Msg.action(attacker, name(warden) + "'s prayer blocks your attack.");
            return 0;
        }
        if (core == null) {
            bar += damage;
            lastBodyHit = RaidEffects.now();
            // OSRS loot calculator: phase 2 Warden damage is worth 2 points each.
            raid.addPoints(attacker.getUUID(), damage * 2);
            if (bar >= RaidScaling.toaCoreChargeNeeded(raid.raidLevel(), raid.partySize())) {
                ejectCore();
            }
        }
        // The Warden's own hitpoints only follow its core's pool (see onCreatureHit).
        return 0;
    }

    /** The obelisk falling ends phase 1 (it stays for phase 2); the Warden enrages at 5% instead of dying. */
    @Override
    public boolean bossAllowDeath(DataBossEntity boss) {
        if (boss == obelisk && phase <= 2) {
            raid.setCurrentHitpoints(obelisk, 1);
            if (phase == 1) {
                later(1, this::beginPhaseTwo);
            }
            return false;
        }
        if (boss == warden && phase == 2) {
            syncBody();
            return false;
        }
        if (boss == warden && phase == 3 && !enraged) {
            raid.setCurrentHitpoints(warden, 1);
            enrage();
            return false;
        }
        if ((boss == elidinis || boss == tumeken) && !(boss == warden && phase == 3)) {
            // A dormant Warden can't be killed by chip damage that skipped bossIncoming.
            raid.setCurrentHitpoints(boss, raid.maxHitpoints(boss));
            return false;
        }
        return true;
    }

    @Override
    public void onCreatureHit(ServerPlayer player, LivingEntity target, int damage) {
        if (phase != 2 || target != core || core == null || damage <= 0) {
            return;
        }
        // OSRS: damage to the core counts five times against the Warden.
        p2Hp -= damage * RaidScaling.TOA_CORE_MULTIPLIER;
        core.setHealth(core.getMaxHealth());
        if (p2Hp <= 0) {
            beginPhaseThree();
            return;
        }
        syncBody();
    }

    /** The phase 2 Warden's visible health follows its core's pool. */
    private void syncBody() {
        if (alive(warden) && p2Max > 0) {
            raid.setCurrentHitpoints(warden, (int) Math.max(1, Math.ceil(raid.maxHitpoints(warden) * (p2Hp / (double) p2Max))));
        }
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void gameTick(long tick) {
        if (obelisk == null && warden == null) {
            return;
        }
        List<ServerPlayer> players = players();
        Set<Invocation> inv = raid.invocations();
        switch (phase) {
            case 1 -> phaseOneTick(tick, players, inv);
            case 2 -> phaseTwoTick(tick, players, inv);
            case 3 -> phaseThreeTick(tick, players, inv);
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ phase 1: the obelisk

    /** Where an orb is: the trough tile it has reached. */
    private Vec3 orbPos(double[] o) {
        double dz = (TROUGH_FROM + o[1]) * (o[0] == 0 ? -1 : 1);
        return vec(OBELISK_X, dz);
    }

    private void phaseOneTick(long tick, List<ServerPlayer> players, Set<Invocation> inv) {
        long t = RaidEffects.now() - startTick;
        // OSRS: the obelisk sends orbs along each trough (faster with Ancient Haste). // approx: one orb a side every 3 (2) ticks
        int every = Math.max(2, Invocation.wardensChargeEvery(inv) / 4);
        if (t >= 2 && t % every == 0) {
            orbs.add(new double[]{0, 0, 0});
            orbs.add(new double[]{1, 0, 0});
        }
        for (Iterator<double[]> it = orbs.iterator(); it.hasNext(); ) {
            double[] o = it.next();
            o[1] += 1.5;
            Vec3 at = orbPos(o);
            int side = (int) o[0];
            world().sendParticles(side == 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, at.x, at.y + 0.6, at.z, 3, 0.1, 0.1, 0.1, 0);
            ServerPlayer blocker = blocker(side, at);
            if (blocker != null) {
                // OSRS: absorbing an orb slows that Warden's charge a lot (not entirely) and costs 3 hitpoints.
                absorbed[side]++;
                charge[side] += BLOCKED_ORB_ENERGY;
                RaidEffects.hitRaw(obelisk, blocker, ORB_ABSORB_DAMAGE);
                it.remove();
            } else if (TROUGH_FROM + o[1] >= WARDEN_Z - 1) {
                charge[side] += ORB_ENERGY;
                it.remove();
            }
        }
        for (int side = 0; side < 2; side++) {
            DataBossEntity w = side == 0 ? elidinis : tumeken;
            if (alive(w) && charge[side] >= FULL_CHARGE) {
                charge[side] = 0;
                chargedAttack(side, w, players);
            } else if (alive(w) && tick % 4 == side * 2) {
                // A red circle fills up under a charging Warden.
                double fill = charge[side] / (double) FULL_CHARGE;
                world().sendParticles(new DustParticleOptions(new Vector3f(0.9f, 0.1f, 0.1f), 1.2f), w.getX(), w.getY() + 0.1, w.getZ(),
                        (int) (4 + 20 * fill), 1.0 + fill, 0.02, 1.0 + fill, 0);
            }
        }
    }

    /** The player standing on (or right beside) a trough tile the orb is crossing. */
    private ServerPlayer blocker(int side, Vec3 orb) {
        for (ServerPlayer p : players()) {
            double rz = p.getZ() - orb.z;
            if (Math.abs(p.getX() - orb.x) <= 1.5 && Math.abs(rz) < 1.0 && Math.abs(p.getY() - orb.y) < 2) {
                return p;
            }
        }
        return null;
    }

    /** A fully charged Warden alternates UFOs and a Charged Shot (OSRS), starting with UFOs. */
    private void chargedAttack(int side, DataBossEntity w, List<ServerPlayer> players) {
        boolean ufos = chargedAttacks[side]++ % 2 == 0;
        wardenAttacks++;
        if (players.isEmpty()) {
            return;
        }
        if (ufos) {
            warn(name(w) + " unleashes a swarm of floating pyramids!");
            ufos(w, players);
        } else {
            for (ServerPlayer p : players) {
                // OSRS: Elidinis' warning is blue, Tumeken's orange.
                p.displayClientMessage(net.minecraft.network.chat.Component.literal("A large ball of energy is shot your way...")
                        .withStyle(side == 0 ? net.minecraft.ChatFormatting.BLUE : net.minecraft.ChatFormatting.GOLD), false);
            }
            chargedShot(side, w);
        }
    }

    /**
     * UFOs (OSRS): small pyramids spread over the arena and discharge damaging light. The edge of the arena beside the
     * stairs is out of their reach. // approx: 3x3 bursts on each player and around the arena, 4 ticks' warning
     */
    private void ufos(DataBossEntity w, List<ServerPlayer> players) {
        List<Vec3> spots = new ArrayList<>();
        for (ServerPlayer p : players) {
            spots.add(p.position());
        }
        for (int i = 0; i < 6 + 2 * players.size(); i++) {
            spots.add(vec(-14 + rnd().nextInt(32), -18 + rnd().nextInt(37)));
        }
        for (Vec3 s : spots) {
            if (s.x - (center().getX() + 0.5) <= -RaidBuilder.HALF + 4) {
                continue; // the stairs' edge
            }
            // round 43: each pyramid's burst marked on the floor too. // playability: OSRS shows the pyramids over their tiles
            RaidEffects.mark(world(), s, 1.5).delay(4).source(w).warn(ParticleTypes.END_ROD).damage(0, 15).floor(RaidFloor.DANGER).start();
        }
    }

    /** Self-test: charged shots that landed, and the game ticks the last one flew. */
    int chargedShotsLanded;
    long lastChargedShotFlight = -1;

    /**
     * Charged Shot (OSRS): a slow ball at every player that can't be prayed against. Elidinis' small ball splashes onto
     * players next to its target (spread out; at most 45 solo, 30 in a group); Tumeken's big ball is shared by everyone
     * standing with its target (stack up; at most 30 solo). // approx: base damage and travel time
     */
    private void chargedShot(int side, DataBossEntity w) {
        List<ServerPlayer> targets = players();
        world().sendParticles(side == 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, w.getX(), w.getY() + 2, w.getZ(), 20, 0.4,
                0.4, 0.4, 0.05);
        // Round 43: a ball flies at each player (drawn all the way, five game ticks as before), with the reach of its splash (or
        // its share) drawn round its target: spread out from Elidinis' ball, stack up for Tumeken's. // playability: it was a
        // particle puff at the Warden and a hit five ticks later
        RaidEffects.telegraph(targets, side == 0 ? "Elidinis' charged shot - spread out!" : "Tumeken's charged shot - stack up!");
        long launched = RaidEffects.now();
        java.util.function.BooleanSupplier attempt = current();
        for (ServerPlayer target : targets) {
            RaidEffects.cue(5, center().getCenter(), () -> {
                if (!alive(w) || phase != 1 || !target.isAlive()) {
                    return false;
                }
                com.runecraft.boss.ThreatCues.ring(world(), target.position(), 1.6, side == 0 ? com.runecraft.util.Msg.TYPELESS_RGB : 0x73FF73,
                        null, false);
                return true;
            });
            RaidEffects.projectile(world(), w.getEyePosition(), target, null, 5, () -> attempt.getAsBoolean() && alive(w) && phase == 1,
                    () -> landChargedShot(side, w, target, launched));
        }
    }

    /** One charged shot landing on its target: Elidinis' splashes on anyone next to them, Tumeken's is shared by them. */
    private void landChargedShot(int side, DataBossEntity w, ServerPlayer target, long launched) {
        List<ServerPlayer> here = players();
        if (!here.contains(target)) {
            return;
        }
        chargedShotsLanded++;
        lastChargedShotFlight = RaidEffects.now() - launched;
        boolean solo = raid.partySize() <= 1;
        double mult = raid.damageMultiplier();
        if (side == 0) {
            int cap = ToaBossRules.chargedShotCap(true, solo);
            for (ServerPlayer p : here) {
                if (p == target || p.distanceToSqr(target) < 1.6 * 1.6) {
                    RaidEffects.hitRaw(w, p, Math.min(cap, (int) Math.round((18 + rnd().nextInt(10)) * mult)));
                }
            }
        } else {
            List<ServerPlayer> stack = new ArrayList<>();
            for (ServerPlayer p : here) {
                if (p.distanceToSqr(target) < 1.6 * 1.6) {
                    stack.add(p);
                }
            }
            double total = (12 + rnd().nextInt(8)) * mult * (solo ? 1 : 1 + 0.6 * (raid.partySize() - 1));
            int each = (int) Math.round(total / Math.max(1, stack.size()));
            for (ServerPlayer p : stack) {
                RaidEffects.hitRaw(w, p, Math.min(ToaBossRules.chargedShotCap(false, solo), each));
            }
        }
    }

    /**
     * The obelisk falls: the Warden whose energy was disrupted most (most orbs absorbed) is destroyed, the other one
     * wakes for phase 2 (OSRS).
     */
    private void beginPhaseTwo() {
        if (phase != 1) {
            return;
        }
        boolean fightElidinis = ToaBossRules.fightElidinisFirst(absorbed[0], absorbed[1], rnd().nextBoolean());
        DataBossEntity destroyed = fightElidinis ? tumeken : elidinis;
        warden = fightElidinis ? elidinis : tumeken;
        if (!alive(warden)) {
            warden = spawnBoss(fightElidinis ? "elidinis_warden" : "tumekens_warden", at(P2_X, 0));
        }
        orbs.clear();
        phase = 2;
        if (alive(destroyed)) {
            world().sendParticles(ParticleTypes.LARGE_SMOKE, destroyed.getX(), destroyed.getY() + 1.5, destroyed.getZ(), 40, 0.6, 1.2, 0.6, 0.02);
        }
        if (warden != null) {
            unanchor(warden);
            move(warden, vec(P2_X, 0));
            anchor(warden);
            warden.setNoAi(true);
        }
        p2Max = RaidScaling.toaWardenP2Hp(raid.raidLevel(), raid.partySize());
        p2Hp = p2Max;
        bar = 0;
        lastBodyHit = RaidEffects.now();
        // OSRS: Tumeken's Warden prays against magic first, Elidinis' against ranged (and both against melee).
        prayed = fightElidinis ? AttackType.RANGED : AttackType.MAGIC;
        p2Attacks = 0;
        nextP2Attack = RaidEffects.now() + 4;
        nextObeliskAttack = RaidEffects.now() + 6;
        syncBody();
        warn("The obelisk falls silent - " + name(destroyed) + " is destroyed, but " + name(warden) + " powers up! It prays against melee and "
                + (prayed == AttackType.MAGIC ? "magic" : "ranged") + ".");
    }

    // ------------------------------------------------------------------ phase 2: the Warden and its core

    private void ejectCore() {
        bar = 0;
        coreTicks = RaidScaling.toaCoreExposureTicks(p2Hp / (double) Math.max(1, p2Max));
        BlockPos at = BlockPos.containing(warden.getX() - 3, warden.getY(), warden.getZ());
        core = spawnMob("toa_warden_core", at, false);
        if (core != null) {
            CORES.add(core);
            core.setNoGravity(true);
        }
        coresEjected++;
        com.runecraft.content.systems.CombatAchievementService.countNear(warden,
                com.runecraft.osrs.content.systems.CombatAchievements.COUNT_WARDEN_CORE);
        warn(name(warden) + " is thrown off balance - its core is ejected to its " + RaidRealtime.compass(at.getX() + 0.5 - warden.getX(),
                at.getZ() + 0.5 - warden.getZ()) + "! Strike the core now (melee always hits it for its max)!");
    }

    private void phaseTwoTick(long tick, List<ServerPlayer> players, Set<Invocation> inv) {
        if (!alive(warden)) {
            beginPhaseThree();
            return;
        }
        // Undo chip damage that skipped bossIncoming (recoil, Vengeance, poison) so it can't build up.
        syncBody();
        long now = RaidEffects.now();
        if (core != null) {
            coreTicks--;
            world().sendParticles(ParticleTypes.END_ROD, core.getX(), core.getY() + 0.5, core.getZ(), 3, 0.3, 0.3, 0.3, 0.01);
            if (coreTicks <= 0 || !core.isAlive()) {
                dropCore();
                // OSRS: its prayers swap so neither ranged nor magic works every cycle.
                prayed = prayed == AttackType.MAGIC ? AttackType.RANGED : AttackType.MAGIC;
                warn(name(warden) + " pulls its core back in. It now prays against melee and "
                        + (prayed == AttackType.MAGIC ? "magic" : "ranged") + ".");
            }
        } else if (bar > 0 && now - lastBodyHit > 10 && tick % 2 == 0) {
            // OSRS: left alone, the Warden slowly restores its shield.
            bar--;
        }
        if (core == null && now >= nextP2Attack) {
            wardenAttack(players);
            nextP2Attack = now + Invocation.wardensAttackEvery(inv, isElidinis(warden));
        }
        obeliskAttacks(tick, players, inv);
    }

    /**
     * The phase 2 Warden's attacks: melee on anyone next to it (no time to react), else a magic (red skull) or ranged
     * skull that only hurts on impact; every fifth attack a special. // approx: special frequency
     */
    private void wardenAttack(List<ServerPlayer> players) {
        if (players.isEmpty()) {
            return;
        }
        wardenAttacks++;
        p2Attacks++;
        boolean high = raid.healthFraction(warden) * 100 > ToaBossRules.WARDEN_P2_SPECIAL_SPLIT;
        if (p2Attacks % 5 == 0) {
            if (high) {
                divineProjectile(players);
            } else {
                imprisonment(players.get(rnd().nextInt(players.size())));
            }
            return;
        }
        // OSRS infobox: Elidinis' Warden hits up to 20, Tumeken's up to 22.
        int max = isElidinis(warden) ? ToaBossRules.ELIDINIS_P2_MAX_HIT : ToaBossRules.TUMEKEN_P2_MAX_HIT;
        for (ServerPlayer p : players) {
            if (p.distanceToSqr(warden) < 3.2 * 3.2) {
                // OSRS: the melee lands on the swing ("cannot be reacted to"); here after the swing's wind-up, on a player still
                // in reach. // playability
                warden.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                com.runecraft.boss.Mechanics.deliverMelee(warden, p, com.runecraft.osrs.ThreatTiming.MELEE_REACH, () -> {
                    if (alive(warden) && phase == 2) {
                        RaidEffects.hit(warden, p, rnd().nextInt(max + 1), AttackType.CRUSH, false);
                    }
                });
                return;
            }
        }
        ServerPlayer target = players.get(rnd().nextInt(players.size()));
        AttackType style = rnd().nextBoolean() ? AttackType.MAGIC : AttackType.RANGED;
        // OSRS: a red skull (magic) or a plain skull (ranged); like Zebak's they only hurt on impact, so a late prayer still
        // works. Drawn here in the style's colour all the way to its target, the prayer read as it lands. // playability
        RaidEffects.projectile(world(), warden.position().add(0, 2.5, 0), target, style, 2, () -> alive(warden) && phase == 2,
                () -> RaidEffects.hit(warden, target, rnd().nextInt(max + 1), style, false));
    }

    /** Game ticks the Divine Projectile flies (from the overheads going off to the hit). */
    static final int DIVINE_FLIGHT_TICKS = 3;

    /**
     * Divine Projectile (above 55%): every overhead switched off, then a scimitar, arrow or spell at everyone. Its style is
     * named in the middle of the screen as it leaves, and each projectile is drawn in the style's colour all the way to its
     * player, landing {@value #DIVINE_FLIGHT_TICKS} game ticks later with the prayer read as it lands. // playability: in
     * OSRS the style is only told by the projectile's shape and colour (red scimitar, white arrow, blue spell)
     */
    private void divineProjectile(List<ServerPlayer> players) {
        AttackType style = switch (rnd().nextInt(3)) {
            case 0 -> AttackType.SLASH;
            case 1 -> AttackType.RANGED;
            default -> AttackType.MAGIC;
        };
        String what = style == AttackType.SLASH ? "a red scimitar (melee)" : style == AttackType.RANGED ? "a white arrow (ranged)"
                : "a blue spell (magic)";
        String pray = style == AttackType.SLASH ? "Melee" : style == AttackType.RANGED ? "Missiles" : "Magic";
        divineProjectiles++;
        divineStyle = style;
        long launched = RaidEffects.now();
        Vec3 from = warden.position().add(0, 2.5, 0);
        for (ServerPlayer p : players) {
            RoomLogic.dropOverheads(p, name(warden) + " disables your overhead prayers!");
            Msg.red(p, name(warden) + " fires " + what + " at you!");
            Msg.telegraph(p, "Divine projectile: " + what + " - pray " + pray + "!", null);
            RaidEffects.projectile(world(), from, p, style, DIVINE_FLIGHT_TICKS, () -> alive(warden) && phase == 2,
                    () -> {
                        if (players().contains(p)) {
                            divineFlight = RaidEffects.now() - launched;
                            divinePrayed = RaidEffects.protectedFrom(p, style);
                            RaidEffects.hit(warden, p, 25 + rnd().nextInt(16), style, false); // approx: "high damage"
                        }
                    });
        }
    }

    /** Divine Projectiles launched, the last one's style, and its last landing: game ticks in flight, prayed at impact (self-test). */
    int divineProjectiles;
    AttackType divineStyle;
    long divineFlight = -1;
    boolean divinePrayed;

    /**
     * Imprisonment (below 55%): a dark projectile at where the target stood; staying there encases them in stone. The spot
     * is marked as a raid strike (drawn at once, redrawn until it lands, the real-time floor).
     */
    private void imprisonment(ServerPlayer target) {
        Vec3 spot = target.position();
        world().sendParticles(ParticleTypes.SQUID_INK, spot.x, spot.y + 0.2, spot.z, 20, 0.4, 0.05, 0.4, 0.01);
        Msg.red(target, name(warden) + " launches a dark projectile at you!");
        Msg.telegraph(target, "Imprisonment - step off your spot!", null);
        RaidEffects.mark(world(), spot, 0.9).delay(3).source(warden).warn(ParticleTypes.SQUID_INK)
                .live(() -> alive(warden) && phase == 2)
                .onHit(p -> {
                    Msg.red(p, "You are encased in stone!");
                    SysCombat.freeze(p, 4); // approx: duration
                    RaidEffects.hit(warden, p, 5 + rnd().nextInt(6), null, true);
                }).start();
    }

    /**
     * The obelisk's set pattern in phase 2 (OSRS): Converging Beam (red lightning), Windmill Beam (orange), Lightning
     * Skull (yellow); stepping next to it gets you zapped. Penetration makes its hits switch off overheads.
     */
    private void obeliskAttacks(long tick, List<ServerPlayer> players, Set<Invocation> inv) {
        if (!alive(obelisk) || players.isEmpty()) {
            return;
        }
        int max = (int) Math.round(25 * Invocation.wardensObeliskDamage(inv));
        if (tick % 4 == 0) {
            for (ServerPlayer p : players) {
                if (p.distanceToSqr(obelisk) < 2.6 * 2.6) {
                    world().sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1, p.getZ(), 10, 0.3, 0.5, 0.3, 0.05);
                    obeliskHit(p, 8 + rnd().nextInt(5), inv); // approx
                }
            }
        }
        if (windmillTicks > 0) {
            windmill(max, inv);
            return;
        }
        long now = RaidEffects.now();
        if (now < nextObeliskAttack) {
            return;
        }
        int pattern = obeliskPattern++ % 3;
        if (pattern == 0 && core != null) {
            // OSRS: the Converging Beam isn't fired while the core is out.
            pattern = 1;
            obeliskPattern++;
        }
        switch (pattern) {
            case 0 -> convergingBeam(max, inv);
            case 1 -> {
                lightning(new Vector3f(1f, 0.55f, 0f));
                RaidEffects.telegraph(players(), "Windmill beam: four beams turn anticlockwise - the floor glows just ahead of them!");
                windmillTicks = 12;
                windmillAngle = rnd().nextInt(4) * 90 + 45;
                beamHit.clear();
            }
            default -> lightningSkulls(max, inv);
        }
        nextObeliskAttack = now + Invocation.wardensObeliskEvery(inv) + (pattern == 1 ? 12 : 0);
    }

    private void obeliskHit(ServerPlayer p, int damage, Set<Invocation> inv) {
        if (RaidEffects.hit(obelisk, p, damage, null, true) > 0 && Invocation.wardensPenetration(inv)) {
            RoomLogic.dropOverheads(p, "The obelisk's energy pierces your prayers!");
        }
    }

    private void lightning(Vector3f colour) {
        world().sendParticles(new DustParticleOptions(colour, 1.5f), obelisk.getX(), obelisk.getY() + 1.5, obelisk.getZ(), 40, 0.8, 1.4, 0.8, 0);
    }

    /**
     * Converging Beam: beams sweep two opposite quarters of the arena and converge on the obelisk; the other two quarters
     * are safe. // approx: the quarters around the arena's centre, 4 ticks' warning
     */
    private void convergingBeam(int max, Set<Invocation> inv) {
        lightning(new Vector3f(0.9f, 0.1f, 0.1f));
        boolean diagonal = rnd().nextBoolean(); // north-west + south-east, or north-east + south-west
        say("Red lightning crackles around the obelisk...");
        // The safe quarters named by compass, and the two struck quarters dusted red until the beams converge.
        // // playability: OSRS shows the beams irradiating their quarters; a real-time player gets the safe quarters named
        String safe = diagonal ? RaidRealtime.quarter(false, true) + " or " + RaidRealtime.quarter(true, false)
                : RaidRealtime.quarter(true, true) + " or " + RaidRealtime.quarter(false, false);
        RaidEffects.telegraph(players(), "Converging beam: stand in the " + safe + " quarter!");
        DustParticleOptions red = new DustParticleOptions(new Vector3f(0.9f, 0.1f, 0.1f), 1.0f);
        RaidEffects.cue(4, () -> {
            if (!alive(obelisk) || phase != 2 || core != null) {
                return false;
            }
            for (int i = 0; i < 24; i++) {
                double dx = rnd().nextInt(19) + 1;
                double dz = rnd().nextInt(19) + 1;
                boolean west = rnd().nextBoolean();
                boolean north = diagonal == west;
                Vec3 at = vec(west ? -dx : dx, north ? -dz : dz);
                world().sendParticles(red, at.x, at.y + 0.1, at.z, 2, 0.4, 0, 0.4, 0);
            }
            return true;
        });
        later(4, () -> {
            if (!alive(obelisk) || phase != 2 || core != null) {
                return;
            }
            for (ServerPlayer p : players()) {
                double rx = p.getX() - (center().getX() + 0.5);
                double rz = p.getZ() - (center().getZ() + 0.5);
                boolean west = rx < 0;
                boolean north = rz < 0;
                if ((north == west) == diagonal) {
                    obeliskHit(p, rnd().nextInt(max + 1), inv);
                }
            }
        });
    }

    /** Windmill Beam: four beams from the obelisk turn like a windmill's sails; the floor glows just ahead of them. */
    private void windmill(int max, Set<Invocation> inv) {
        windmillTicks--;
        windmillAngle -= 7.5;
        Vec3 o = obelisk.position();
        for (int k = 0; k < 4; k++) {
            double a = Math.toRadians(windmillAngle + 90 * k);
            double ahead = Math.toRadians(windmillAngle + 90 * k - 7.5);
            for (double r = 2; r < 40; r += 1.5) {
                Vec3 at = o.add(Math.cos(a) * r, 0, Math.sin(a) * r);
                if (!inside(at)) {
                    break;
                }
                world().sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.6, at.z, 1, 0.1, 0.1, 0.1, 0);
                Vec3 glow = o.add(Math.cos(ahead) * r, 0, Math.sin(ahead) * r);
                world().sendParticles(new DustParticleOptions(new Vector3f(1f, 0.55f, 0f), 0.8f), glow.x, glow.y + 0.05, glow.z, 1, 0.1, 0, 0.1, 0);
            }
            for (ServerPlayer p : players()) {
                Vec3 d = p.position().subtract(o);
                double along = d.x * Math.cos(a) + d.z * Math.sin(a);
                double across = Math.abs(-d.x * Math.sin(a) + d.z * Math.cos(a));
                Long last = beamHit.get(p.getUUID());
                if (along > 1.5 && across < 0.9 && (last == null || RaidEffects.now() - last >= 3)) {
                    beamHit.put(p.getUUID(), RaidEffects.now());
                    obeliskHit(p, rnd().nextInt(max + 1), inv);
                }
            }
        }
    }

    /** The last Lightning Skull volley's skulls (self-test). */
    List<BlockPos> lastSkulls = List.of();

    private static final DustParticleOptions SKULL_EDGE = new DustParticleOptions(new Vector3f(1f, 0.95f, 0.2f), 1.1f);

    /** One skull's cues: its shadow, the 7x7 burst's outline in yellow and the safe cross in green. */
    private void drawSkull(BlockPos s) {
        double cx = s.getX() + 0.5;
        double cz = s.getZ() + 0.5;
        double y = s.getY() + 0.1;
        world().sendParticles(ParticleTypes.SMOKE, cx, y, cz, 4, 0.3, 0.02, 0.3, 0);
        for (int i = 0; i <= 7; i++) {
            double f = -3.5 + i;
            world().sendParticles(SKULL_EDGE, cx + f, y, cz - 3.5, 1, 0, 0, 0, 0);
            world().sendParticles(SKULL_EDGE, cx + f, y, cz + 3.5, 1, 0, 0, 0, 0);
            world().sendParticles(SKULL_EDGE, cx - 3.5, y, cz + f, 1, 0, 0, 0, 0);
            world().sendParticles(SKULL_EDGE, cx + 3.5, y, cz + f, 1, 0, 0, 0, 0);
        }
        for (int[] d : SKULL_SAFE) {
            if (!ToaBossRules.lightningSkullHits(d[0], d[1])) {
                RaidEffects.safeTile(world(), new Vec3(cx + d[0], s.getY(), cz + d[1]));
            }
        }
    }

    /** The tiles of a skull's safe cross (OSRS: two tiles each way, straight). */
    static final int[][] SKULL_SAFE = {{1, 0}, {2, 0}, {-1, 0}, {-2, 0}, {0, 1}, {0, 2}, {0, -1}, {0, -2}};

    /**
     * Lightning Skull: skulls land around the arena and lightning bursts 7x7 around each, except the two tiles straight
     * north, south, east and west of the skull (OSRS).
     */
    private void lightningSkulls(int max, Set<Invocation> inv) {
        lightning(new Vector3f(1f, 0.95f, 0.2f));
        List<BlockPos> skulls = new ArrayList<>();
        for (ServerPlayer p : players()) {
            skulls.add(p.blockPosition().offset(rnd().nextInt(3) - 1, 0, rnd().nextInt(3) - 1));
        }
        for (int i = 0; i < 2; i++) {
            skulls.add(at(-14 + rnd().nextInt(26), -16 + rnd().nextInt(33)));
        }
        lastSkulls = skulls;
        // Until the lightning bursts: each skull's shadow, its 7x7 burst outlined in yellow, and the safe cross (the two tiles
        // straight north, south, east and west of it) in green. // playability: OSRS shows only the skulls' shadows, and the
        // safe tiles are read off the tile grid
        RaidEffects.telegraph(players(), "Lightning skulls: stand on a green tile beside a skull!");
        RaidEffects.cue(4, () -> {
            if (!alive(obelisk) || phase != 2) {
                return false;
            }
            for (BlockPos s : skulls) {
                drawSkull(s);
            }
            return true;
        });
        later(4, () -> {
            if (!alive(obelisk) || phase != 2) {
                return;
            }
            Set<UUID> hit = new HashSet<>();
            for (BlockPos s : skulls) {
                world().sendParticles(ParticleTypes.ELECTRIC_SPARK, s.getX() + 0.5, s.getY() + 0.5, s.getZ() + 0.5, 60, 2.5, 0.3, 2.5, 0.1);
                for (ServerPlayer p : players()) {
                    if (!hit.contains(p.getUUID()) && skullHits(p.position(), s)) {
                        hit.add(p.getUUID());
                        obeliskHit(p, rnd().nextInt(max + 1), inv);
                    }
                }
            }
        });
    }

    /**
     * Whether a skull's burst reaches a player standing at {@code pos}: inside its 7x7 and not on its safe cross. A player
     * straddling a tile edge with a foot ({@link RaidRealtime#MAZE_EDGE_LENIENCY}) on a safe tile, or outside the 7x7, is
     * spared. // playability: OSRS judges the one tile the player stands on
     */
    static boolean skullHits(Vec3 pos, BlockPos skull) {
        double m = RaidRealtime.MAZE_EDGE_LENIENCY;
        for (double ox : new double[]{0, -m, m}) {
            for (double oz : new double[]{0, -m, m}) {
                int dx = (int) Math.floor(pos.x + ox) - skull.getX();
                int dz = (int) Math.floor(pos.z + oz) - skull.getZ();
                if (!ToaBossRules.lightningSkullHits(dx, dz)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The core's pool is spent: the Warden collapses and restores the other one; the obelisk explodes (OSRS). */
    private void beginPhaseThree() {
        if (phase != 2) {
            return;
        }
        dropCore();
        DataBossEntity fallen = warden;
        warden = fallen == elidinis ? tumeken : elidinis;
        if (fallen != null) {
            unanchor(fallen);
            fallen.discard();
        }
        if (!alive(warden)) {
            warden = spawnBoss(fallen == elidinis ? "tumekens_warden" : "elidinis_warden", at(OBELISK_X, 0));
        }
        if (alive(obelisk)) {
            world().sendParticles(ParticleTypes.EXPLOSION_EMITTER, obelisk.getX(), obelisk.getY() + 1, obelisk.getZ(), 1, 0, 0, 0, 0);
            world().playSound(null, obelisk.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5f, 0.6f);
            unanchor(obelisk);
            obelisk.discard();
        }
        phase = 3;
        windmillTicks = 0;
        if (warden != null) {
            unanchor(warden);
            move(warden, vec(OBELISK_X, 0));
            anchor(warden);
            warden.setNoAi(true);
            raid.setCurrentHitpoints(warden, raid.maxHitpoints(warden));
        }
        slam = 0;
        nextSlam = RaidEffects.now() + 6;
        warn("The Warden's body collapses... it uses the last of its power to restore " + name(warden)
                + "! The obelisk explodes and the room warps into the void!");
    }

    // ------------------------------------------------------------------ phase 3: slams, siphons, phantoms, enrage

    private void phaseThreeTick(long tick, List<ServerPlayer> players, Set<Invocation> inv) {
        if (!alive(warden)) {
            complete();
            return;
        }
        double hp = raid.healthFraction(warden) * 100;
        long now = RaidEffects.now();
        if (siphonsUp) {
            siphonTick(now);
        } else if (!enraged && siphonSets < ToaBossRules.SIPHON_THRESHOLDS.length && hp <= ToaBossRules.SIPHON_THRESHOLDS[siphonSets]) {
            launchSiphons(players);
        } else if (!enraged && hp <= ToaBossRules.WARDEN_ENRAGE_AT) {
            enrage();
        } else if (!enraged && now >= nextSlam && !players.isEmpty()) {
            slam(players);
            nextSlam = now + ToaBossRules.wardenSlamEvery(inv);
        }
        if (enraged) {
            enrageTick(tick, now, players);
        }
        for (int i = 0; i < phantoms.size(); i++) {
            phantomTick(i, phantoms.get(i), tick, players, inv);
        }
    }

    /**
     * The floor slams (OSRS): to the Warden's right (north, as it faces west) first, then its left, then both sides.
     * Against a side slam stand at least a tile to the other side of its centre line; against both, stay in the three
     * centre tiles in front of it.
     */
    /**
     * Game ticks from the slam's warning to the floor rising. // playability: OSRS raises it about two ticks after the
     * Warden's animation (the slam travels, so a late step away still works 6-9 tiles out); here the full real-time floor
     */
    static final int SLAM_LEAD_TICKS = com.runecraft.osrs.ArenaTiming.STRIKE_FLOOR_TICKS;

    private static final DustParticleOptions SLAM_DANGER = new DustParticleOptions(new Vector3f(1.0f, 0.4f, 0.1f), 1.3f);

    /** Slams made and their warnings' lead in game ticks (self-test). */
    int slamsMade;
    long lastSlamLead;

    private void slam(List<ServerPlayer> players) {
        int kind = slam++ % 3;
        wardenAttacks++;
        slamsMade++;
        String where = kind == 0 ? "to its right" : kind == 1 ? "to its left" : "on both sides";
        say(name(warden) + " raises the floor " + where + "!");
        // The struck rows in orange, the safe ones in green, redrawn until the floor rises; the side named by compass.
        // // playability: OSRS shows the Warden's arm and the floor rising, read as "its right" / "its left"
        RaidEffects.telegraph(players(), RaidRealtime.wardenSlamCue(kind));
        int cz = center().getZ();
        long made = RaidEffects.now();
        // Round 43: the struck rows are marked on the floor too, until the floor rises. // playability: OSRS raises the floor
        List<int[]> struck = new ArrayList<>();
        for (int z = -18; z <= 18; z++) {
            if (ToaBossRules.slamHits(kind, z)) {
                for (int x = -16; x <= 10; x++) {
                    struck.add(new int[]{center().getX() + x, cz + z});
                }
            }
        }
        RaidFloor.columns(world(), struck, center().getY() + 1, RaidFloor.DANGER, SLAM_LEAD_TICKS, warden, center().getCenter());
        RaidEffects.cue(SLAM_LEAD_TICKS, () -> {
            if (!alive(warden) || phase != 3) {
                return false;
            }
            for (int x = -16; x <= 12; x += 4) {
                for (int z = -18; z <= 18; z++) {
                    boolean hits = ToaBossRules.slamHits(kind, z);
                    if (hits && Math.floorMod(z, 3) != 0) {
                        continue; // the struck rows sparser, the safe ones solid
                    }
                    world().sendParticles(hits ? SLAM_DANGER : RaidEffects.SAFE, center().getX() + 0.5 + x, center().getY() + 1.1,
                            cz + 0.5 + z, 1, 0.4, 0, 0.1, 0);
                }
            }
            return true;
        });
        later(SLAM_LEAD_TICKS, () -> {
            if (!alive(warden) || phase != 3) {
                return;
            }
            lastSlamLead = RaidEffects.now() - made;
            for (ServerPlayer p : players()) {
                if (ToaBossRules.slamHits(kind, p.getBlockZ() - cz)) {
                    world().sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 0.3, p.getZ(), 1, 0, 0, 0, 0);
                    // OSRS: "moderate or heavy" typeless damage; the phase 3 infobox max hit is 26. // approx: 13-26
                    int max = ToaBossRules.WARDEN_P3_MAX_HIT;
                    RaidEffects.hit(warden, p, max / 2 + rnd().nextInt(max / 2 + 1), null, true);
                }
            }
        });
    }

    /**
     * Energy siphons (OSRS): the Warden stops slamming, prays against everything and launches a set of siphons (a solo or
     * a group layout, more hitpoints in big groups). Melee reverses them; every one reversed in time hurts the Warden,
     * otherwise they detonate in a shockwave from the centre. Phantoms join at the second and third set.
     */
    private void launchSiphons(List<ServerPlayer> players) {
        int n = Math.max(1, players.size());
        int count = ToaBossRules.siphonCount(n);
        int hp = ToaBossRules.siphonHp(n);
        siphonSets++;
        siphonsUp = true;
        siphons.clear();
        warn(name(warden) + " focuses its energy - it prays against everything and launches energy siphons! Reverse them with melee!");
        double radius = n <= 1 ? 6 : 8;
        for (int i = 0; i < count; i++) {
            double a = 2 * Math.PI * i / count;
            BlockPos at = at((int) Math.round(Math.cos(a) * radius), (int) Math.round(Math.sin(a) * radius));
            for (ServerPlayer p : players) {
                if (p.blockPosition().equals(at)) {
                    // OSRS: a player where a siphon lands is knocked away, unhurt.
                    p.push(Math.cos(a), 0.3, Math.sin(a));
                    p.hurtMarked = true;
                }
            }
            LivingEntity s = spawnMob("toa_energy_siphon", at, false);
            if (s != null) {
                s.setNoGravity(true);
                if (s instanceof net.minecraft.world.entity.Mob m) {
                    m.setNoAi(true);
                }
                raid.setHitpoints(s, hp);
                siphons.add(s);
            }
        }
        siphonDeadline = RaidEffects.now() + ToaBossRules.siphonTicks(n, raid.invocations().contains(Invocation.INSANITY));
        if (siphonSets == 2 || siphonSets == 3) {
            boolean elid = isElidinis(warden);
            String phantom = elid ? (siphonSets == 2 ? "Akkha" : "Kephri") : (siphonSets == 2 ? "Zebak" : "Ba-Ba");
            int i = phantoms.size();
            phantoms.add(phantom);
            LivingEntity mob = spawnMob(PHANTOM_IDS.get(phantom), at(OBELISK_X + 3, i == 0 ? -7 : 7), false);
            if (mob instanceof net.minecraft.world.entity.Mob m) {
                m.setNoAi(true);
                m.setYRot(90);
                m.setYHeadRot(90);
                m.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, 20 * 3600, 0,
                        false, false));
                anchor(m);
            }
            if (mob != null) {
                phantomMobs.add(mob);
            }
            phantomNext[i] = RaidEffects.now() + 4;
            warn(phantom + "'s phantom rises to assist " + name(warden) + "!");
        }
    }

    /** The phantoms' creature ids by boss (OSRS: Akkha and Kephri with Elidinis' Warden, Zebak and Ba-Ba with Tumeken's). */
    static final Map<String, String> PHANTOM_IDS = Map.of("Akkha", "toa_akkhas_phantom", "Kephri", "toa_kephris_phantom", "Zebak",
            "toa_zebaks_phantom", "Ba-Ba", "toa_babas_phantom");

    /** The path index ({@link com.runecraft.osrs.content.raids.RaidLayouts#TOA_PATHS}) of each phantom's original. */
    private static int pathOf(String phantom) {
        return switch (phantom) {
            case "Kephri" -> 0;
            case "Akkha" -> 1;
            case "Ba-Ba" -> 2;
            default -> 3;
        };
    }

    private void siphonTick(long now) {
        int left = 0;
        for (LivingEntity s : siphons) {
            if (alive(s)) {
                left++;
                world().sendParticles(ParticleTypes.REVERSE_PORTAL, s.getX(), s.getY() + 0.8, s.getZ(), 3, 0.2, 0.3, 0.2, 0.01);
            }
        }
        if (left == 0) {
            // Reversed: they collide back into the Warden.
            int damage = (int) Math.round(raid.maxHitpoints(warden) * ToaBossRules.SIPHON_DAMAGE_EACH * siphons.size());
            raid.setCurrentHitpoints(warden, Math.max(1, raid.hitpoints(warden) - damage));
            world().sendParticles(ParticleTypes.EXPLOSION, warden.getX(), warden.getY() + 1.5, warden.getZ(), 4, 0.8, 0.8, 0.8, 0);
            warn("The reversed siphons slam into " + name(warden) + "!");
            clearSiphons();
            if (!raid.invocations().contains(Invocation.INSANITY)) {
                // OSRS: the slam rotation starts over (not with Insanity).
                slam = 0;
            }
            nextSlam = now + 3;
            return;
        }
        if (now >= siphonDeadline) {
            // OSRS: the shockwave from the centre scales with raid level and the siphons left. Far enough out is safe.
            warn(name(warden) + " detonates the siphons!");
            siphonsFailed++;
            Vec3 c = vec(0, 0);
            world().sendParticles(ParticleTypes.SONIC_BOOM, c.x, c.y + 1, c.z, 1, 0, 0, 0, 0);
            for (ServerPlayer p : players()) {
                if (RaidEffects.horizontalDistSq(p.position(), c) < 15 * 15) {
                    RaidEffects.hit(warden, p, 10 + 12 * left, null, true); // approx
                }
            }
            clearSiphons();
            nextSlam = now + 3;
        }
    }

    /** At 5%: a one-time 20% heal and the final enrage (OSRS). */
    private void enrage() {
        if (enraged || !alive(warden)) {
            return;
        }
        enraged = true;
        clearSiphons();
        raid.heal(warden, raid.maxHitpoints(warden) * ToaBossRules.WARDEN_ENRAGE_HEAL / 100);
        // OSRS: its Defence rises by 30 levels (150 -> 180) when it enrages.
        raid.raiseDefence(warden, ToaBossRules.WARDEN_ENRAGE_DEFENCE);
        nextRip = RaidEffects.now() + 8;
        warn(name(warden) + " shakes violently and draws in the last of its power! The phantom begins to devour the arena!");
    }

    /**
     * Enraged: no more slams; lightning strikes the arena (rate unaffected by invocations) and the phantom rips out the
     * furthest row of floor every so often, up to the row in front of the Warden. // approx: rates
     */
    private void enrageTick(long tick, long now, List<ServerPlayer> players) {
        boolean insanity = raid.invocations().contains(Invocation.INSANITY);
        // Round 43: every lightning strike marked on the floor (light blue) until it lands. // playability: OSRS shows the
        // lightning's shadow on its tiles
        if (tick % ToaBossRules.wardenLightningEvery(insanity) == 0) {
            int x = -18 + rippedColumns + rnd().nextInt(Math.max(1, 30 - rippedColumns));
            RaidEffects.mark(world(), vec(x, -18 + rnd().nextInt(37)), 1.0).delay(4).source(warden).warn(ParticleTypes.SMOKE).damage(0, 20)
                    .floor(RaidFloor.LIGHTNING).start();
        }
        if (tick % 6 == 3) {
            for (ServerPlayer p : players) {
                RaidEffects.mark(world(), p.position(), 1.0).delay(4).source(warden).warn(ParticleTypes.SMOKE).damage(0, 20)
                        .floor(RaidFloor.LIGHTNING).start();
            }
        }
        int lastColumn = OBELISK_X - 3 + RaidBuilder.HALF - 1; // columns counted from the west wall
        // The next row to be torn away is marked on the floor for the strike floor first (round 43: it vanished unannounced).
        // // playability: OSRS shows the phantom reaching for the row
        if (rippedColumns < lastColumn && now >= nextRip - RIP_WARN_TICKS && ripWarnedFor != rippedColumns) {
            ripWarnedFor = rippedColumns;
            int wx = center().getX() - RaidBuilder.HALF + 1 + rippedColumns;
            RaidFloor.rect(world(), wx, center().getZ() - RaidBuilder.HALF + 1, wx, center().getZ() + RaidBuilder.HALF - 1, center().getY() + 1,
                    RaidFloor.DANGER, (int) Math.max(1, nextRip - now), warden);
            ripsWarned++;
        }
        if (now >= nextRip && rippedColumns < lastColumn) {
            nextRip = now + ToaBossRules.wardenRipEvery(insanity);
            int x = -RaidBuilder.HALF + 1 + rippedColumns;
            rippedColumns++;
            BlockPos c = center();
            RaidBuilder.fill(world(), c.getX() + x, c.getY(), c.getZ() - RaidBuilder.HALF + 1, c.getX() + x, c.getY(), c.getZ() + RaidBuilder.HALF - 1,
                    Blocks.BLACK_CONCRETE.defaultBlockState());
            world().sendParticles(ParticleTypes.REVERSE_PORTAL, c.getX() + x + 0.5, c.getY() + 1, c.getZ() + 0.5, 60, 0.3, 0.5, 15, 0.02);
        }
        // Anyone on the torn-away floor is pulled back onto solid ground, hurt.
        int edge = center().getX() - RaidBuilder.HALF + 1 + rippedColumns;
        for (ServerPlayer p : players) {
            if (p.getBlockX() < edge) {
                Msg.red(p, "The void tears at you!");
                p.teleportTo(edge + 0.5, p.getY(), p.getZ());
                RaidEffects.hit(warden, p, 10, null, true); // approx
            }
        }
    }

    /** Game ticks the next row to be torn away is marked before it goes. */
    static final int RIP_WARN_TICKS = com.runecraft.osrs.ArenaTiming.STRIKE_FLOOR_TICKS;
    /** The ripped-column count the rip warning was last shown for (-1: none), and warnings shown (self-test). */
    private int ripWarnedFor = -1;
    int ripsWarned;

    private void restoreFloor() {
        if (rippedColumns <= 0) {
            return;
        }
        BlockPos c = center();
        for (int i = 0; i < rippedColumns; i++) {
            int x = -RaidBuilder.HALF + 1 + i;
            RaidBuilder.fill(world(), c.getX() + x, c.getY(), c.getZ() - RaidBuilder.HALF + 1, c.getX() + x, c.getY(), c.getZ() + RaidBuilder.HALF - 1,
                    RaidBuilder.palette(def.style()).floor());
        }
        rippedColumns = 0;
        decorate();
    }

    /**
     * The path bosses' phantoms attack with their originals' standard attacks and invocations (OSRS wiki): Akkha's ranged
     * and magic, switching every three attacks starting with ranged (at random with Stay Vigilant) and hitting everyone, only
     * partly blocked by prayer; Kephri's fireball (3x3 and harder with Aerial Assault); Zebak's rock and jug projectiles,
     * prayable until they land; Ba-Ba's boulder on a player's tile, dodged by stepping a tile away. Their original path's
     * level speeds Kephri and Zebak up as in their own fights; with Insanity they attack much faster once the Warden
     * enrages.
     */
    private void phantomTick(int i, String phantom, long tick, List<ServerPlayer> players, Set<Invocation> inv) {
        LivingEntity mob = i < phantomMobs.size() ? phantomMobs.get(i) : null;
        if (mob == null || !mob.isAlive() || players.isEmpty()) {
            return;
        }
        if (tick % 5 == 0) {
            world().sendParticles(ParticleTypes.SCULK_SOUL, mob.getX(), mob.getY() + 1.5, mob.getZ(), 6, 0.6, 1.0, 0.6, 0.01);
        }
        long now = RaidEffects.now();
        if (now < phantomNext[i]) {
            return;
        }
        int speed = ToaBossRules.phantomAttackSpeed(phantom, raid.pathLevelOf(pathOf(phantom)), enraged && inv.contains(Invocation.INSANITY));
        phantomNext[i] = now + speed;
        wardenAttacks++;
        phantomAttackCount++;
        ServerPlayer p = players.get(rnd().nextInt(players.size()));
        switch (phantom) {
            case "Kephri" -> {
                int radius = Invocation.kephriFireballRadius(inv);
                int max = (int) Math.round(ToaBossRooms.Kephri.KEPHRI_MAX_HIT * Invocation.kephriFireballDamage(inv));
                RaidEffects.mark(world(), p.position(), 0.8 + 1.5 * radius).delay(3).source(mob).warn(ParticleTypes.FLAME).damage(0, max)
                        .style(AttackType.MAGIC, ToaBossRooms.Kephri.KEPHRI_PROTECTED_SHARE).floor(RaidFloor.DANGER).start();
            }
            case "Akkha" -> {
                boolean magic = inv.contains(Invocation.STAY_VIGILANT) ? rnd().nextBoolean() : (phantomAttacks[i] / 3) % 2 == 1;
                phantomAttacks[i]++;
                AttackType style = magic ? AttackType.MAGIC : AttackType.RANGED;
                world().sendParticles(magic ? ParticleTypes.WITCH : ParticleTypes.CRIT, mob.getX(), mob.getY() + 2, mob.getZ(), 12, 0.3, 0.3, 0.3,
                        0.05);
                // Round 43: a projectile in its style's colour at each player, the prayer read as it lands (it was a hit two ticks
                // after a particle puff); its style named mid-screen. // playability: OSRS flies them too
                RaidEffects.telegraph(players, "Akkha's phantom: " + (magic ? "magic" : "ranged") + " - pray "
                        + (magic ? "Magic" : "Missiles") + "!");
                for (ServerPlayer q : players) {
                    RaidEffects.projectile(world(), mob.getEyePosition(), q, style, 2, () -> alive(warden) && phase == 3 && mob.isAlive(), () ->
                            // OSRS: Akkha's ranged and magic hit everyone; a protection prayer only takes 80-90% off.
                            RaidEffects.hit(mob, q, rnd().nextInt(ToaBossRules.AKKHA_PHANTOM_MAX_HIT + 1), style,
                                    ToaBossRules.AKKHA_PROTECTED_RANGED_MAGIC));
                }
            }
            case "Zebak" -> {
                AttackType style = rnd().nextBoolean() ? AttackType.MAGIC : AttackType.RANGED;
                world().sendParticles(style == AttackType.MAGIC ? ParticleTypes.DAMAGE_INDICATOR : ParticleTypes.CRIT, mob.getX(), mob.getY() + 2,
                        mob.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
                // round 43: his jug or rock flies in its style's colour, prayable until it lands
                com.runecraft.util.Msg.telegraph(p, "Zebak's phantom: " + (style == AttackType.MAGIC ? "jug - pray Magic" : "rock - pray Missiles")
                        + "!", null);
                RaidEffects.projectile(world(), mob.getEyePosition(), p, style, 2, () -> alive(warden) && phase == 3 && mob.isAlive(), () ->
                        RaidEffects.hit(mob, p, rnd().nextInt(ToaBossRules.ZEBAK_RANGED_MAX + 1), style, false));
            }
            default -> RaidEffects.mark(world(), p.position(), 0.9).delay(3).source(mob).warn(ParticleTypes.CAMPFIRE_COSY_SMOKE)
                    .damage(0, ToaBossRules.BABA_PHANTOM_BOULDER_MAX).floor(RaidFloor.DANGER).start();
        }
    }

    // ------------------------------------------------------------------ HUD and self-test

    @Override
    String hudNote() {
        return switch (phase) {
            case 1 -> String.format("Charge E %d%% T %d%%", 100 * charge[0] / FULL_CHARGE, 100 * charge[1] / FULL_CHARGE);
            case 2 -> core != null ? "Core exposed " + coreTicks + "t"
                    : "Shield " + 100 * bar / Math.max(1, RaidScaling.toaCoreChargeNeeded(raid.raidLevel(), raid.partySize())) + "%  Prays "
                    + (prayed == AttackType.MAGIC ? "Magic" : "Ranged");
            case 3 -> siphonsUp ? "Siphons " + Math.max(0, siphonDeadline - RaidEffects.now()) + "t" : enraged ? "Enraged" : "";
            default -> null;
        };
    }

    /**
     * Self-test: blocks Tumeken's trough so Elidinis' Warden is fought in phase 2 (and Tumeken's in phase 3), breaks the
     * obelisk once a Warden has fired a charged attack, fills the bar with unprayed styles and hammers each core with
     * melee, then reverses every siphon set until the enraged Warden falls.
     */
    @Override
    String selfTestSolve(RaidSelfTest t) {
        int guard = 0;
        int attacksBefore = raid.attacksOnParty();
        BlockPos trough = at(OBELISK_X, (TROUGH_FROM + TROUGH_TO) / 2);
        boolean triedPrayed = false;
        while (phase == 1 && guard++ < 1500) {
            t.moveTo(trough);
            if (absorbed[1] >= 3 && wardenAttacks >= 1 && alive(obelisk)) {
                t.hit(obelisk, 40, AttackType.MAGIC);
            }
            t.tick(1);
        }
        RaidSelfTest.expect(phase == 2, "Wardens: the obelisk phase did not end (" + guard + " ticks, " + absorbed[1] + " orbs absorbed)");
        RaidSelfTest.expect(warden == elidinis, "Wardens: blocking Tumeken's trough did not make Elidinis' Warden the phase 2 Warden");
        RaidSelfTest.expect(absorbed[0] == 0, "Wardens: orbs were absorbed on the trough nobody stood on");
        StringBuilder note = new StringBuilder("Wardens: obelisk down after " + guard + " ticks (" + absorbed[1] + " orbs absorbed)");
        t.moveTo(at(P2_X - 4, -3));
        // The Divine Projectile (round 42): its style named as it leaves and the overheads switched off; the player switches
        // the named prayer on a game tick (12 MC) later through the prayer keys' path, and it lands no sooner than three game
        // ticks after it left, read as prayed. // playability
        divineProjectile(players());
        AttackType divine = divineStyle;
        t.tick(1);
        t.pray(com.runecraft.osrs.PrayerGrace.protectionFor(divine), true);
        for (int g = 0; divineFlight < 0 && g < 8; g++) {
            t.tick(1);
        }
        RaidSelfTest.expect(divineFlight >= DIVINE_FLIGHT_TICKS, "Wardens: the Divine Projectile landed " + divineFlight
                + " game ticks after it left (at least " + DIVINE_FLIGHT_TICKS + ")");
        RaidSelfTest.expect(divinePrayed, "Wardens: the " + divine + " Divine Projectile did not read the prayer switched on after it left");
        t.pray(com.runecraft.osrs.PrayerGrace.protectionFor(divine), false);
        // Lightning Skulls: the safe cross spares a player on it (or with a foot on it), the 7x7 around it is struck.
        BlockPos skull = at(0, 0);
        Vec3 c = Vec3.atBottomCenterOf(skull);
        for (int[] d : SKULL_SAFE) {
            RaidSelfTest.expect(!skullHits(c.add(d[0], 0, d[1]), skull), "Wardens: a skull's safe tile " + d[0] + "," + d[1] + " was struck");
        }
        RaidSelfTest.expect(skullHits(c, skull) && skullHits(c.add(1, 0, 1), skull) && skullHits(c.add(3, 0, 0), skull),
                "Wardens: a skull spared its own tile, a diagonal or the cross's far end");
        RaidSelfTest.expect(!skullHits(c.add(1.0, 0, 0.7), skull), "Wardens: a foot on the safe cross did not spare the player");
        RaidSelfTest.expect(!skullHits(c.add(4, 0, 0), skull), "Wardens: a skull struck outside its 7x7");
        note.append(", divine ").append(divine).append(" prayed after ").append(divineFlight).append(" ticks");
        int p2Ticks = 0;
        while (phase == 2 && p2Ticks++ < 3000) {
            if (core != null && alive(core)) {
                t.hit(core, 60, AttackType.SLASH);
            } else if (alive(warden)) {
                if (!triedPrayed) {
                    triedPrayed = true;
                    int before = bar;
                    t.hit(warden, 40, AttackType.SLASH);
                    RaidSelfTest.expect(bar == before, "Wardens: the phase 2 Warden's Protect from Melee let a hit through");
                }
                t.hit(warden, 40, prayed == AttackType.MAGIC ? AttackType.RANGED : AttackType.MAGIC);
            }
            t.tick(1);
        }
        RaidSelfTest.expect(phase == 3, "Wardens: phase 2 did not end (" + coresEjected + " cores, " + p2Hp + "/" + p2Max + " left)");
        RaidSelfTest.expect(coresEjected >= 1, "Wardens: no core was ejected");
        RaidSelfTest.expect(warden == tumeken && alive(warden), "Wardens: Tumeken's Warden was not restored for phase 3");
        RaidSelfTest.expect(!alive(obelisk), "Wardens: the obelisk did not explode");
        note.append(", ").append(coresEjected).append(" cores");
        int p3Ticks = 0;
        int siphonHits = 0;
        List<String> phantomsSeen = new ArrayList<>();
        int phantomAttacksSeen = 0;
        while (!done && p3Ticks++ < 4000) {
            for (LivingEntity m : phantomMobs) {
                String id = RaidInstance.creatureId(m);
                if (!phantomsSeen.contains(id)) {
                    phantomsSeen.add(id);
                    // OSRS: the phantoms can't be attacked.
                    RaidSelfTest.expect(alive(m) && t.hit(m, 50, AttackType.SLASH) == 0 && t.hit(m, 50, AttackType.MAGIC) == 0,
                            "Wardens: " + id + " could be damaged");
                }
            }

            if (siphonsUp) {
                for (LivingEntity s : new ArrayList<>(siphons)) {
                    if (alive(s)) {
                        RaidSelfTest.expect(t.hit(s, 50, AttackType.MAGIC) == 0, "Wardens: magic hurt an energy siphon");
                        t.hit(s, ToaBossRules.siphonHp(1), AttackType.SLASH);
                        siphonHits++;
                    }
                }
            } else if (alive(warden)) {
                t.hit(warden, 30, AttackType.MAGIC);
            }
            t.tick(1);
        }
        phantomAttacksSeen = phantomAttackCount;
        RaidSelfTest.expect(done, "Wardens: the phase 3 Warden did not die (" + (alive(warden) ? raid.hitpoints(warden) + " hitpoints left" : "gone") + ")");
        RaidSelfTest.expect(siphonSets == 4 && siphonsFailed == 0, "Wardens: " + siphonSets + " siphon sets, " + siphonsFailed + " failed");
        RaidSelfTest.expect(phantomsSeen.equals(List.of("toa_zebaks_phantom", "toa_babas_phantom")),
                "Wardens: Tumeken's phantoms were " + phantomsSeen);
        RaidSelfTest.expect(phantomAttacksSeen > 0, "Wardens: the phantoms never attacked");
        RaidSelfTest.expect(enraged, "Wardens: the Warden died without enraging");
        RaidSelfTest.expect(slamsMade == 0 || lastSlamLead >= SLAM_LEAD_TICKS, "Wardens: a floor slam rose " + lastSlamLead
                + " game ticks after its warning (at least " + SLAM_LEAD_TICKS + ")");
        RaidSelfTest.expect(raid.attacksOnParty() > attacksBefore, "Wardens: nothing attacked the player");
        return note.append(", ").append(siphonHits).append(" siphon hits, phase 3 in ").append(p3Ticks).append(" ticks").toString();
    }
}
