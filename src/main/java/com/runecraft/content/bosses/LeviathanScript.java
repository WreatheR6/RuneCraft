package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.HitCalcBridge;
import com.runecraft.content.systems.CombatAchievementService;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.bosses.BossMechanics;
import com.runecraft.osrs.content.bosses.Dt2Bosses;
import com.runecraft.osrs.content.bosses.LeviathanRules;
import com.runecraft.osrs.content.systems.CombatAchievements;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The Leviathan (OSRS Wiki "The Leviathan/Strategies", {@link LeviathanRules}): volleys of orbs announced as they fly (blue
 * magic, green ranged, orange melee; switch protection prayers for each), faster every volley; after each a roar, debris
 * (one stays on the player's tile as a blocker, five in a + awakened) and a special, alternating Lightning Barrage (a current
 * chasing the player, orbs across the arena) and Smoke Blast (ten debris, then a blast for anyone it can see - hide behind
 * the debris). A shadow spell during a volley stuns it: hits are capped at 10 except on the gaping wound on its back, which
 * breaks the stun, starts a special and resets the volleys. Melee can't reach it. Below 20% it enrages with an abyssal
 * pathfinder circling the arena that the player must stay inside. Awakened: a tornado from 50%.
 */
final class LeviathanScript extends ScriptedBoss {
    private record Orb(AttackDef attack, long at) {
    }

    private enum Special { BARRAGE, SMOKE }

    private static final String NBT_DEBRIS = "LeviathanDebris";
    /** The scripts of Leviathans around, for the pathfinder's accuracy hook. */
    private static final Map<LivingEntity, LeviathanScript> LIVE = Collections.synchronizedMap(new WeakHashMap<>());
    private static boolean hooked;

    private final List<AttackDef> volley = new ArrayList<>();
    private final List<Orb> flying = new ArrayList<>();
    private int stage;
    private int stunResets;
    private int stunned;
    private int dealtWhileStunned;
    /** Where it faced when stunned (its wound is on the other side). */
    private Vec3 stunFacing;
    private Special special;
    private int specialTick;
    private boolean nextBarrage;
    private boolean enraged;
    private Vec3 pathfinder;
    private int pathfinderStep;
    private final List<BlockPos> debris = new ArrayList<>();
    private final List<BlockPos> smokeDebris = new ArrayList<>();
    private long[] savedDebris;
    private Vec3 tornado;
    private int tornadoPause;
    private final List<Vec3> current = new ArrayList<>();
    private int pause;

    /** Max hit scale: 1 for the boss, below 1 for the weaker Desert Treasure II quest version. */
    private final double scale;

    LeviathanScript() {
        this(1.0);
    }

    LeviathanScript(double scale) {
        this.scale = scale;
    }

    @Override
    protected double multiplier(DataBossEntity boss) {
        return super.multiplier(boss) * scale;
    }

    @Override
    protected boolean driven() {
        return true;
    }

    @Override
    protected boolean canAttack(DataBossEntity boss) {
        return stunned <= 0 && special == null;
    }

    private int scaled(DataBossEntity boss, AttackDef a) {
        return (int) Math.round(maxHit(boss, a) * scale);
    }

    @Override
    public void onSpawn(DataBossEntity boss) {
        nextBarrage = boss.getRandom().nextBoolean();
        LIVE.put(boss, this);
        hook();
    }

    // ---------------- the player's hits ----------------

    /** Whether a player stands in the abyssal pathfinder's area. */
    boolean inPathfinder(DataBossEntity boss, ServerPlayer p) {
        if (pathfinder == null) {
            return false;
        }
        double half = boss.awakened() ? LeviathanRules.PATHFINDER_HALF_AWAKENED : LeviathanRules.PATHFINDER_HALF;
        return Math.abs(p.getX() - pathfinder.x) <= half && Math.abs(p.getZ() - pathfinder.z) <= half;
    }

    /** Whether a player stands behind it (at the gaping wound) while it is stunned. */
    boolean atWound(DataBossEntity boss, ServerPlayer p) {
        if (stunned <= 0 || stunFacing == null) {
            return false;
        }
        Vec3 to = p.position().subtract(boss.position()).multiply(1, 0, 1);
        return to.lengthSqr() > 1e-4 && to.normalize().dot(stunFacing) < -0.3;
    }

    private static int estimatedMax(ServerPlayer p, DataBossEntity boss, AttackType type, int damage) {
        int max = HitCalcBridge.maxHit(p, boss, HitCalcBridge.input(p, boss, type, null, 0));
        return Math.max(damage, max);
    }

    @Override
    protected int incoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
        if (attacker != null && type.isMelee()) {
            Msg.action(attacker, "Your melee attacks can't reach the lure!");
            return 0;
        }
        boolean magic = type == AttackType.MAGIC;
        if (attacker != null && magic && stunned <= 0 && (!volley.isEmpty() || !flying.isEmpty())
                && String.valueOf(com.runecraft.content.systems.SysState.of(attacker).autocast).startsWith("shadow_")) {
            stun(boss, attacker);
            return damage;
        }
        if (stunned > 0 && attacker != null) {
            boolean wound = atWound(boss, attacker);
            int d = LeviathanRules.stunnedHit(damage, wound, magic, estimatedMax(attacker, boss, type, damage));
            dealtWhileStunned += d;
            if (wound) {
                endStun(boss, true, "The Leviathan's wound is struck! It breaks free in a rage!");
            } else if (LeviathanRules.quarterDealt(dealtWhileStunned, boss.trueHitpoints())) {
                stage = LeviathanRules.afterStun(++stunResets);
                dealtWhileStunned = Integer.MIN_VALUE / 2; // once per stun
            }
            return d;
        }
        if (enraged && attacker != null) {
            damage = LeviathanRules.enragedHit(damage, inPathfinder(boss, attacker), magic, estimatedMax(attacker, boss, type, damage));
        }
        if (special != null) {
            damage = (int) Math.floor(damage * LeviathanRules.SPECIAL_DAMAGE);
        }
        return damage;
    }

    private void stun(DataBossEntity boss, ServerPlayer attacker) {
        stunned = boss.awakened() ? LeviathanRules.STUN_TICKS_AWAKENED : LeviathanRules.STUN_TICKS;
        dealtWhileStunned = 0;
        volley.clear();
        flying.clear();
        Vec3 facing = attacker.position().subtract(boss.position()).multiply(1, 0, 1);
        stunFacing = facing.lengthSqr() > 1e-4 ? facing.normalize() : new Vec3(0, 0, 1);
        CombatAchievementService.count(attacker, CombatAchievements.COUNT_LEVIATHAN_STUN, 1);
        BossFx.say(boss, "The shadow spell stuns the Leviathan! Strike the wound on its back!");
    }

    private void endStun(DataBossEntity boss, boolean wound, String message) {
        stunned = 0;
        stunFacing = null;
        if (wound) {
            stage = LeviathanRules.afterStun(++stunResets);
            BossFx.say(boss, message);
            startSpecial(boss);
        }
    }

    // ---------------- its attacks ----------------

    @Override
    protected AttackDef nextAttack(DataBossEntity boss, ServerPlayer target, double distance) {
        if (distance < 1.5) {
            volley.clear();
            return Dt2Bosses.LEVI_BITE; // the bite next to its pool (no warning)
        }
        if (volley.isEmpty() && flying.isEmpty()) {
            if (pause > 0) {
                return null;
            }
            int n = enraged ? 4 : (boss.awakened() ? 5 : 3) + boss.getRandom().nextInt(4); // approx: volley lengths
            boolean melee = LeviathanRules.melee(stage, enraged);
            for (int i = 0; i < n; i++) {
                int s = boss.getRandom().nextInt(melee ? 3 : 2);
                volley.add(s == 0 ? Dt2Bosses.LEVI_RANGED : s == 1 ? Dt2Bosses.LEVI_MAGIC : Dt2Bosses.LEVI_MELEE);
            }
            announceVolley(boss);
        }
        if (volley.isEmpty()) {
            return null;
        }
        // an orb flies: shown now, it lands after the reaction time (switch prayers while it flies)
        AttackDef a = volley.remove(0);
        int react = LeviathanRules.react(stage, enraged);
        flying.add(new Orb(a, now + react));
        BossFx.burst(boss, a == Dt2Bosses.LEVI_RANGED ? ParticleTypes.COMPOSTER : a == Dt2Bosses.LEVI_MAGIC ? ParticleTypes.SOUL_FIRE_FLAME
                : ParticleTypes.FLAME, 8, 0.8);
        cooldown = LeviathanRules.spacing(stage, enraged, boss.awakened()) - 1;
        return null;
    }

    /**
     * The whole volley in one centre-screen line, in firing order and style colours ("Orbs: MAG RNG MEL"), shown as it
     * begins. // playability: OSRS shows each orb only as it flies; one action-bar line per orb overwrote the last
     */
    private void announceVolley(DataBossEntity boss) {
        net.minecraft.network.chat.MutableComponent line = net.minecraft.network.chat.Component.literal("Orbs:")
                .withStyle(s -> s.withColor(0xFFC030));
        for (AttackDef a : volley) {
            final boolean ranged = a == Dt2Bosses.LEVI_RANGED;
            final boolean magic = a == Dt2Bosses.LEVI_MAGIC;
            line.append(net.minecraft.network.chat.Component.literal(ranged ? " RNG" : magic ? " MAG" : " MEL")
                    .withStyle(s -> s.withColor(ranged ? Msg.RANGED_RGB : magic ? Msg.MAGIC_RGB : Msg.MELEE_RGB)));
        }
        for (ServerPlayer p : BossFx.world(boss).getPlayers(p -> p.distanceToSqr(boss) < 48 * 48)) {
            Msg.telegraph(p, line);
        }
    }

    @Override
    public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
        if (!enraged || inPathfinder(boss, player) || damage > 0 || last == Dt2Bosses.LEVI_BITE) {
            return damage;
        }
        // outside the pathfinder its orbs pierce prayer: the hit lands as unprayed damage
        Prayer pr = type == AttackType.MAGIC ? Prayer.PROTECT_FROM_MAGIC : type == AttackType.RANGED ? Prayer.PROTECT_FROM_MISSILES
                : Prayer.PROTECT_FROM_MELEE;
        if (BossFx.protects(player, pr) && last != null) {
            int pierce = boss.getRandom().nextInt(scaled(boss, last) + 1);
            CombatHooks.nameAttack(last.name(), null);
            CombatHandler.applyDamage(boss, player, pierce);
            Msg.red(player, "Outside the pathfinder's light, the orb pierces your prayer!");
        }
        return 0;
    }

    /** After a volley: the roar, debris (one staying as a blocker on the player), then a special. */
    private void roar(DataBossEntity boss, ServerPlayer target) {
        BossFx.say(boss, "The Leviathan roars!");
        BossFx.sound(boss, net.minecraft.sounds.SoundEvents.RAVAGER_ROAR, 0.7f);
        for (ServerPlayer p : fighters(boss, target)) {
            CombatHooks.nameAttack("roar", null);
            CombatHandler.applyDamage(boss, p, boss.getRandom().nextInt(LeviathanRules.ROAR_MAX + 1));
        }
        dropDebris(boss, target, true);
        if (!enraged) {
            startSpecial(boss);
        }
    }

    private List<ServerPlayer> fighters(DataBossEntity boss, ServerPlayer target) {
        List<ServerPlayer> list = new ArrayList<>(BossFx.players(boss, 24));
        if (target != null && target.isAlive() && !list.contains(target)) {
            list.add(target);
        }
        return list;
    }

    /** Debris around the arena and on the player (that one, or five in a + awakened, staying as blockers when asked). */
    private void dropDebris(DataBossEntity boss, ServerPlayer target, boolean blockers) {
        int max = scaled(boss, Dt2Bosses.LEVI_DEBRIS);
        for (int i = 0; i < 3; i++) {
            namedStrike("falling_debris", boss, around(boss, homeVec(), 10), 3, max, 1.2, ParticleTypes.ASH);
        }
        if (target == null) {
            return;
        }
        Vec3 at = target.position();
        List<Vec3> spots = new ArrayList<>(List.of(at));
        if (boss.awakened()) {
            spots.addAll(List.of(at.add(2, 0, 0), at.add(-2, 0, 0), at.add(0, 0, 2), at.add(0, 0, -2)));
        }
        for (Vec3 s : spots) {
            namedStrike("falling_debris", boss, s, 3, max, 1.0, ParticleTypes.ASH, (hit, pos) -> {
                if (blockers) {
                    placeDebris(boss, pos, debris);
                }
            });
        }
    }

    /** A 2-high pile of debris (never on a player's spot or the Leviathan's pool). */
    private void placeDebris(DataBossEntity boss, Vec3 pos, List<BlockPos> into) {
        ServerLevel w = BossFx.world(boss);
        BlockPos base = BlockPos.containing(pos);
        if (pos.distanceToSqr(boss.position()) < 3.5 * 3.5) {
            return;
        }
        for (ServerPlayer p : w.players()) {
            if (p.blockPosition().equals(base) || p.blockPosition().equals(base.above())) {
                return;
            }
        }
        for (int y = 0; y < 2; y++) {
            BlockPos b = base.above(y);
            if (w.getBlockState(b).isAir()) {
                w.setBlock(b, Blocks.COBBLED_DEEPSLATE.defaultBlockState(), Block.UPDATE_ALL);
                into.add(b.immutable());
            }
        }
    }

    private static void clearDebris(ServerLevel w, List<BlockPos> list) {
        for (BlockPos b : list) {
            if (w.getBlockState(b).is(Blocks.COBBLED_DEEPSLATE)) {
                w.setBlock(b, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        list.clear();
    }

    private void startSpecial(DataBossEntity boss) {
        special = nextBarrage ? Special.BARRAGE : Special.SMOKE;
        nextBarrage = !nextBarrage;
        specialTick = 0;
        current.clear();
        CombatAchievementService.countNear(boss, "used:" + (special == Special.BARRAGE ? "lightning" : "smoke_blast"));
        BossFx.say(boss, special == Special.BARRAGE ? "The Leviathan unleashes a lightning barrage! Keep moving!"
                : "The Leviathan spits debris... a smoke blast is coming! Hide behind the debris!");
    }

    private void tickSpecial(DataBossEntity boss, ServerPlayer target) {
        specialTick++;
        ServerLevel w = BossFx.world(boss);
        if (target == null) {
            endSpecial(boss);
            return;
        }
        if (special == Special.BARRAGE) {
            // the current strikes where the player stood two ticks ago; orbs land across the arena
            current.add(target.position());
            if (current.size() > 2) {
                Vec3 at = current.remove(0);
                namedStrike("lightning", boss, at, 0, scaled(boss, Dt2Bosses.LEVI_LIGHTNING), 0.9, ParticleTypes.ELECTRIC_SPARK);
            }
            if (specialTick % 2 == 0) {
                namedStrike("lightning", boss, around(boss, homeVec(), 11), 2, scaled(boss, Dt2Bosses.LEVI_LIGHTNING) / 2, 0.7,
                        ParticleTypes.ELECTRIC_SPARK);
            }
            if (specialTick >= LeviathanRules.BARRAGE_TICKS) {
                endSpecial(boss);
            }
            return;
        }
        // Smoke Blast: ten debris on the player, then the blast
        if (specialTick <= LeviathanRules.SMOKE_DEBRIS) {
            namedStrike("falling_debris", boss, target.position(), 2, scaled(boss, Dt2Bosses.LEVI_DEBRIS), 1.0, ParticleTypes.ASH,
                    (hit, pos) -> placeDebris(boss, pos, smokeDebris));
            return;
        }
        if (specialTick == LeviathanRules.SMOKE_DEBRIS + 3) {
            int max = boss.awakened() ? (int) Math.round(LeviathanRules.SMOKE_BLAST_MAX_AWAKENED * scale) : scaled(boss, Dt2Bosses.LEVI_SMOKE);
            BossFx.burst(boss, ParticleTypes.LARGE_SMOKE, 60, 6.0);
            for (ServerPlayer p : fighters(boss, target)) {
                if (boss.hasLineOfSight(p)) {
                    CombatHooks.nameAttack(Dt2Bosses.LEVI_SMOKE.name(), null);
                    CombatHandler.applyDamage(boss, p, boss.getRandom().nextInt(max + 1));
                    Msg.red(p, "The smoke blast engulfs you!");
                } else {
                    Msg.game(p, "You shelter behind the debris.");
                }
            }
            clearDebris(w, smokeDebris);
            endSpecial(boss);
        }
    }

    private void endSpecial(DataBossEntity boss) {
        special = null;
        current.clear();
        stage = LeviathanRules.next(stage); // the next volley is faster
        pause = 2;
    }

    // ---------------- the enrage ----------------

    private void enrage(DataBossEntity boss, ServerPlayer target) {
        enraged = true;
        volley.clear();
        flying.clear();
        BossFx.say(boss, "The Leviathan focuses on you intensely! Stay in the abyssal pathfinder's light!");
        dropDebris(boss, target, true);
        double r = boss.def() == null ? 10 : Math.max(6, boss.def().arenaRadius - 4);
        Vec3 c = homeVec();
        pathfinder = new Vec3(c.x - r, c.y, c.z - r); // the north-west corner, then clockwise
        pathfinderStep = 0;
    }

    /** The pathfinder walks clockwise round the arena (north edge east, east edge south...). approx: half a block a tick */
    private void tickPathfinder(DataBossEntity boss) {
        double r = boss.def() == null ? 10 : Math.max(6, boss.def().arenaRadius - 4);
        Vec3 c = homeVec();
        double perimeter = 8 * r;
        double s = (pathfinderStep++ * 0.5) % perimeter;
        double x;
        double z;
        if (s < 2 * r) {
            x = c.x - r + s;
            z = c.z - r;
        } else if (s < 4 * r) {
            x = c.x + r;
            z = c.z - r + (s - 2 * r);
        } else if (s < 6 * r) {
            x = c.x + r - (s - 4 * r);
            z = c.z + r;
        } else {
            x = c.x - r;
            z = c.z + r - (s - 6 * r);
        }
        pathfinder = BossFx.ground(BossFx.world(boss), x, z, c.y);
        double half = boss.awakened() ? LeviathanRules.PATHFINDER_HALF_AWAKENED : LeviathanRules.PATHFINDER_HALF;
        ServerLevel w = BossFx.world(boss);
        for (int i = -1; i <= 1; i += 2) {
            for (int j = -1; j <= 1; j += 2) {
                w.sendParticles(ParticleTypes.END_ROD, pathfinder.x + i * half, pathfinder.y + 0.2, pathfinder.z + j * half, 1, 0, 0, 0, 0);
            }
        }
        w.sendParticles(ParticleTypes.GLOW, pathfinder.x, pathfinder.y + 0.6, pathfinder.z, 4, 0.4, 0.3, 0.4, 0);
    }

    /** Awakened, from 50%: a tornado drifts after the player and hits for 40+ on contact (approx: speed and pause). */
    private void tickTornado(DataBossEntity boss, ServerPlayer target) {
        if (!boss.awakened() || target == null || percent(boss) > 50) {
            return;
        }
        ServerLevel w = BossFx.world(boss);
        if (tornado == null) {
            if (--tornadoPause > 0) {
                return;
            }
            double a = boss.getRandom().nextDouble() * Math.PI * 2;
            tornado = BossFx.ground(w, homeVec().x + Math.cos(a) * 10, homeVec().z + Math.sin(a) * 10, homeVec().y);
            BossFx.warn(boss, "A tornado forms in the cavern!");
        }
        Vec3 d = target.position().subtract(tornado);
        double len = d.horizontalDistance();
        if (len <= 0.9) {
            CombatHooks.nameAttack(Dt2Bosses.LEVI_TORNADO.name(), null);
            CombatHandler.applyDamage(boss, target, (int) Math.round((BossMechanics.LEVIATHAN_TORNADO_MIN
                    + boss.getRandom().nextInt(BossMechanics.LEVIATHAN_TORNADO_MAX - BossMechanics.LEVIATHAN_TORNADO_MIN + 1)) * scale));
            Msg.red(target, "The tornado tears into you!");
            tornado = null;
            tornadoPause = 5;
            return;
        }
        double step = Math.min(0.5, len);
        tornado = BossFx.ground(w, tornado.x + d.x / len * step, tornado.z + d.z / len * step, tornado.y);
        w.sendParticles(ParticleTypes.CLOUD, tornado.x, tornado.y + 0.5, tornado.z, 8, 0.3, 0.8, 0.3, 0.02);
    }

    @Override
    protected void onGameTick(DataBossEntity boss, ServerPlayer target) {
        LIVE.put(boss, this);
        hook();
        if (savedDebris != null) {
            ServerLevel w = BossFx.world(boss);
            for (long p : savedDebris) {
                if (w.getBlockState(BlockPos.of(p)).is(Blocks.COBBLED_DEEPSLATE)) {
                    w.setBlock(BlockPos.of(p), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
            savedDebris = null;
        }
        if (pause > 0) {
            pause--;
        }
        tickTornado(boss, target);
        if (!enraged && percent(boss) <= LeviathanRules.ENRAGE_PERCENT && target != null && special != Special.SMOKE) {
            // (the Smoke Blast isn't interrupted by the enrage: OSRS Wiki)
            enrage(boss, target);
        }
        if (enraged) {
            tickPathfinder(boss);
            if (target != null && now % 6 == 0) {
                dropDebris(boss, target, now % 12 == 0); // debris keeps falling, blockers too
            }
        }
        // orbs in flight land
        for (java.util.Iterator<Orb> it = flying.iterator(); it.hasNext(); ) {
            Orb o = it.next();
            if (now >= o.at()) {
                it.remove();
                if (target != null && stunned <= 0) {
                    perform(boss, target, o.attack());
                }
            }
        }
        if (stunned > 0) {
            BossFx.burst(boss, ParticleTypes.SMOKE, 6, 1.5);
            if (--stunned == 0) {
                endStun(boss, false, null);
            }
            return;
        }
        if (special != null) {
            tickSpecial(boss, target);
            return;
        }
        if (!enraged && volley.isEmpty() && flying.isEmpty() && last != null && last != Dt2Bosses.LEVI_BITE && cooldown <= 0
                && attackCount > 0 && !roared) {
            roared = true;
            roar(boss, target);
        }
        if (!volley.isEmpty()) {
            roared = false;
        }
    }

    /** Whether the roar after the current volley has come (it comes once per volley). */
    private boolean roared;

    // ---------------- clean-up ----------------

    @Override
    public void writeNbt(DataBossEntity boss, CompoundTag nbt) {
        List<Long> all = new ArrayList<>();
        debris.forEach(p -> all.add(p.asLong()));
        smokeDebris.forEach(p -> all.add(p.asLong()));
        if (savedDebris != null) {
            for (long p : savedDebris) {
                all.add(p);
            }
        }
        if (!all.isEmpty()) {
            nbt.putLongArray(NBT_DEBRIS, all.stream().mapToLong(Long::longValue).toArray());
        }
    }

    @Override
    public void readNbt(DataBossEntity boss, CompoundTag nbt) {
        if (nbt.contains(NBT_DEBRIS)) {
            savedDebris = nbt.getLongArray(NBT_DEBRIS);
        }
    }

    @Override
    public void onDeath(DataBossEntity boss, ServerPlayer killer) {
        onRemoved(boss);
    }

    @Override
    public void onRemoved(DataBossEntity boss) {
        ServerLevel w = BossFx.world(boss);
        clearDebris(w, debris); // once it is slain all debris is removed
        clearDebris(w, smokeDebris);
        LIVE.remove(boss);
    }

    /** Inside the pathfinder, the player's ranged attacks on an enraged Leviathan never miss. */
    private static void hook() {
        if (hooked) {
            return;
        }
        hooked = true;
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            @Override
            public double accuracy(ServerPlayer player, LivingEntity target, AttackType type) {
                LeviathanScript s = LIVE.get(target);
                if (s != null && s.enraged && type == AttackType.RANGED && target instanceof DataBossEntity boss
                        && s.inPathfinder(boss, player)) {
                    return MinionRules.ALWAYS_HITS;
                }
                return 1.0;
            }
        });
    }

    // ---------------- self-test ----------------

    int selfTestStage() {
        return stage;
    }

    boolean selfTestStunned() {
        return stunned > 0;
    }

    String selfTestSpecial() {
        return special == null ? null : special.name().toLowerCase(java.util.Locale.ROOT);
    }

    int selfTestSpecialTick() {
        return specialTick;
    }

    boolean selfTestEnraged() {
        return enraged;
    }

    Vec3 selfTestPathfinder() {
        return pathfinder;
    }

    int selfTestFlying() {
        return flying.size() + volley.size();
    }

    /** The next orb to land (the one to pray against), or null. */
    AttackDef selfTestNextOrb() {
        Orb next = null;
        for (Orb o : flying) {
            if (next == null || o.at() < next.at()) {
                next = o;
            }
        }
        return next == null ? null : next.attack();
    }

    /** The debris of the running Smoke Blast (to hide behind). */
    List<BlockPos> selfTestSmokeDebris() {
        return smokeDebris;
    }

    boolean selfTestInPathfinder(DataBossEntity boss, ServerPlayer p) {
        return inPathfinder(boss, p);
    }

    int selfTestDebris() {
        return debris.size() + smokeDebris.size();
    }

    /** Stuns it as a shadow spell during a volley would (the walk has no shadow spell autocast to cast). */
    void selfTestStun(DataBossEntity boss, ServerPlayer p) {
        if (volley.isEmpty() && flying.isEmpty()) {
            volley.add(Dt2Bosses.LEVI_MAGIC);
        }
        stun(boss, p);
    }

    /** A spot behind it (at its wound) while stunned. */
    Vec3 selfTestWound(DataBossEntity boss) {
        return stunFacing == null ? null : boss.position().subtract(stunFacing.scale(3.5));
    }
}
