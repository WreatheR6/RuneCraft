package com.runecraft.entity;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.Aggression;
import com.runecraft.combat.CombatZones;
import com.runecraft.combat.KillCredit;
import com.runecraft.osrs.MonsterAi;
import com.runecraft.osrs.ThreatTiming;
import com.runecraft.player.Players;
import com.runecraft.world.Gielinor;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;

/**
 * Who an OSRS monster fights (round 42), replacing vanilla's HurtByTargetGoal and NearestAttackableTargetGoal for every monster on
 * OSRS AI: the one target goal, always running, ticked every Minecraft tick.
 *
 * <ul>
 * <li>Aggression: an idle aggressive monster looks for a player within its hunt range ({@link OsrsCombatant#huntRange}; an
 * open-world monster also needs the player inside its aggression range around its spawn point) whom it
 * {@linkplain OsrsCombatant#wantsToAttack wants to attack} (combat level, tolerance, module rules, single-way).</li>
 * <li>Retaliation stickiness: hit by someone else, it switches only when its target is gone, dead, out of range or out of reach
 * while the new attacker isn't ({@link MonsterAi#switchTarget}; // playability: vanilla switched to every new attacker).</li>
 * <li>Leash (open-world monsters, {@link MonsterHome}): past {@link MonsterAi#LEASH_BLOCKS} from its spawn point, or with its target
 * fled beyond its aggression range, it drops the fight, walks home and is back to full health there once nothing has hit it for
 * 5 s (// playability: OSRS confines a monster to 7 tiles of its spawn point and it regenerates 1 hitpoint a minute). A target that
 * keeps attacking it from beyond its range is not given up: the monster stands at the edge of its max range
 * ({@link MonsterAi#holdAtEdge}, OSRS safespotting), so a player out-ranging it can still kill it.</li>
 * <li>Single-way: a monster that may not attack its target (the player is fighting something else) drops it after
 * {@link MonsterAi#BLOCKED_DROP_MC} instead of hovering at the player's side (// playability).</li>
 * </ul>
 */
public class OsrsTargetGoal extends Goal {
    /** Self-test hook: players the hunt also sees (the self-test's fake player is not in the level's player list). */
    public static java.util.function.Function<ServerLevel, List<ServerPlayer>> extraPlayers = w -> List.of();
    /** How often (MC) a monster after a player re-checks the single-way rules. */
    private static final int BLOCK_CHECK_MC = 4;

    private final OsrsCombatant c;
    private final Mob mob;
    private final boolean hunts;
    private int scanIn;
    private int lastHurtStamp = Integer.MIN_VALUE;
    private int blockedFor;
    private UUID forgotten;
    private int forgottenUntil;
    private boolean returning;
    private int returningSince;
    /** When something last hit (or missed) the monster, in its own ticks. */
    private int lastHitAt = Integer.MIN_VALUE;
    /** Standing at the edge of its max range while its target attacks it from further out. */
    private boolean holding;
    /** Came home while still being hit: it heals once the hits have stopped. */
    private boolean healPending;

    /** {@code hunts}: looks for players to attack unprovoked (a rock crab wakes by proximity instead). */
    public OsrsTargetGoal(OsrsCombatant c, boolean hunts) {
        this.c = c;
        this.mob = c.self();
        this.hunts = hunts;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    /** The target goal of a monster, or null. */
    public static OsrsTargetGoal of(Mob mob) {
        return mob instanceof OsrsMonsterEntity m ? m.targetGoal() : VanillaOsrsAi.targetGoal(mob);
    }

    /** True while the monster walks back to its spawn point after giving up a chase. */
    public boolean returning() {
        return returning;
    }

    /** True while the monster stands at the edge of its max range, its target attacking it from beyond (the attack goal stays put). */
    public boolean holding() {
        return holding;
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return true;
    }

    @Override
    public boolean isInterruptable() {
        return false;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (!(mob.level() instanceof ServerLevel level) || !mob.isAlive()) {
            return;
        }
        int now = mob.tickCount;
        BlockPos home = MonsterHome.get(mob);
        LivingEntity hurtBy = mob.getLastHurtByMob();
        boolean newHurt = hurtBy != null && mob.getLastHurtByMobTimestamp() != lastHurtStamp;
        if (newHurt) {
            lastHurtStamp = mob.getLastHurtByMobTimestamp();
            lastHitAt = now;
        }
        boolean wasHolding = holding;
        holding = false;
        if (returning) {
            tickReturn(level, home, now);
            return;
        }
        LivingEntity target = mob.getTarget();
        if (target != null && !valid(target, home)) {
            mob.setTarget(null);
            target = null;
        }
        if (newHurt) {
            retaliate(credited(hurtBy), home, target);
            target = mob.getTarget();
        }
        if (home != null) {
            double mobFromHome = MonsterHome.fromHome(home, mob.getX(), mob.getZ());
            boolean giveUp;
            if (target != null) {
                double targetFromHome = MonsterHome.fromHome(home, target.getX(), target.getZ());
                boolean underFire = underFire(target, now);
                giveUp = MonsterAi.giveUp(mobFromHome, targetFromHome, c.attackReach(), underFire);
                // the way to the target leads further from home: (target - mob) . (mob - home) > 0
                double outward = (target.getX() - mob.getX()) * (mob.getX() - (home.getX() + 0.5))
                        + (target.getZ() - mob.getZ()) * (mob.getZ() - (home.getZ() + 0.5));
                holding = !giveUp && MonsterAi.holdAtEdge(mobFromHome, targetFromHome, c.attackReach(), underFire, outward > 0);
                if (holding && !wasHolding) {
                    mob.getNavigation().stop();
                }
            } else {
                giveUp = mobFromHome > MonsterAi.LEASH_BLOCKS;
            }
            if (giveUp) {
                holding = false;
                startReturn(now);
                return;
            }
            if (target != null) {
                healPending = false; // fighting again: the next give-up decides
            } else if (healPending && MonsterAi.healsAtHome((long) now - lastHitAt)) {
                heal();
            }
        }
        if (target instanceof ServerPlayer p) {
            // the single-way test every few ticks (it walks the multicombat areas)
            if (now % BLOCK_CHECK_MC == 0) {
                if (CombatZones.npcMayAttack(mob, p)) {
                    blockedFor = 0;
                } else if ((blockedFor += BLOCK_CHECK_MC) >= MonsterAi.BLOCKED_DROP_MC) {
                    blockedFor = 0;
                    forgotten = p.getUUID();
                    forgottenUntil = now + MonsterAi.BLOCKED_FORGET_MC;
                    mob.setTarget(null);
                }
            }
        } else {
            blockedFor = 0;
        }
        if (mob.getTarget() == null && hunts && --scanIn <= 0) {
            scanIn = MonsterAi.HUNT_SCAN_MC;
            ServerPlayer prey = findPrey(level, home, now);
            if (prey != null) {
                mob.setTarget(prey);
            }
        }
    }

    /** Still worth fighting: alive, here, a player in survival or adventure, not absurdly far (the leash decides for open-world ones). */
    private boolean valid(LivingEntity target, BlockPos home) {
        if (!target.isAlive() || target.isRemoved() || target.level() != mob.level()) {
            return false;
        }
        if (target instanceof Player p && (p.isCreative() || p.isSpectator())) {
            return false;
        }
        double max = home != null ? 64 : 48;
        return mob.distanceToSqr(target) <= max * max;
    }

    /** Its target hit (or missed) it within {@link MonsterAi#UNDER_FIRE_MC}: the fight is still on wherever the target stands. */
    private boolean underFire(LivingEntity target, int now) {
        LivingEntity by = mob.getLastHurtByMob();
        return by != null && credited(by) == target && (long) now - mob.getLastHurtByMobTimestamp() < MonsterAi.UNDER_FIRE_MC;
    }

    /** A summon (a thrall) answers for its owner: the monster goes for the player behind it. */
    private LivingEntity credited(LivingEntity attacker) {
        ServerPlayer owner = KillCredit.owner(attacker);
        return owner != null && owner.level() == mob.level() ? owner : attacker;
    }

    private void retaliate(LivingEntity attacker, BlockPos home, LivingEntity target) {
        if (attacker == null || attacker == mob || attacker == target || !valid(attacker, home)) {
            return;
        }
        boolean targetValid = target != null && valid(target, home);
        if (MonsterAi.switchTarget(target != null, targetValid, targetValid && reachable(target), reachable(attacker))) {
            mob.setTarget(attacker);
        }
    }

    /** It can hit {@code e} from where it stands, or walk to it. */
    private boolean reachable(LivingEntity e) {
        double reach = c.attackReach();
        if (reach > ThreatTiming.MELEE_REACH + 0.01) {
            if (mob.distanceTo(e) <= reach && mob.getSensing().hasLineOfSight(e)) {
                return true;
            }
        } else {
            double gap = ThreatTiming.gap(e.getX() - mob.getX(), e.getZ() - mob.getZ(), mob.getBbWidth(), e.getBbWidth());
            if (ThreatTiming.inMeleeReach(gap, e.getY() - mob.getY())) {
                return true;
            }
        }
        Path path = probe(e, 1);
        return path != null && path.canReach();
    }

    /**
     * A path to {@code e} for a reachability test that doesn't steer the monster. A spider's {@link WallClimberNavigation} remembers
     * the block of every path it is asked for ({@code createPath} of a block or an entity) and, once its own path is done or gone,
     * steers straight at it: probing a second attacker sent a giant spider standing in melee reach of its target drifting toward
     * the attacker's block (its close-in step overridden too) until the gap reopened. Its {@code Set} form, which the climber doesn't
     * override, finds the same path once the target block is put where {@link GroundPathNavigation#createPath(BlockPos, int)} puts
     * it (an airborne target onto the ground below it, a solid block's up to the first non-solid one above).
     */
    private Path probe(LivingEntity e, int accuracy) {
        PathNavigation nav = mob.getNavigation();
        if (!(nav instanceof WallClimberNavigation)) {
            return nav.createPath(e, accuracy);
        }
        BlockPos pos = e.blockPosition();
        net.minecraft.world.level.chunk.LevelChunk chunk = mob.level().getChunkSource().getChunkNow(
                SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
        if (chunk == null) {
            return null;
        }
        int min = mob.level().getMinBuildHeight();
        int max = mob.level().getMaxBuildHeight();
        if (chunk.getBlockState(pos).isAir()) {
            BlockPos below = pos.below();
            while (below.getY() > min && chunk.getBlockState(below).isAir()) {
                below = below.below();
            }
            if (below.getY() > min) {
                return nav.createPath(java.util.Collections.singleton(below.above()), accuracy);
            }
            while (below.getY() < max && chunk.getBlockState(below).isAir()) {
                below = below.above();
            }
            pos = below;
        }
        if (chunk.getBlockState(pos).isSolid()) {
            BlockPos above = pos.above();
            while (above.getY() < max && chunk.getBlockState(above).isSolid()) {
                above = above.above();
            }
            pos = above;
        }
        return nav.createPath(java.util.Collections.singleton(pos), accuracy);
    }

    private ServerPlayer findPrey(ServerLevel level, BlockPos home, int now) {
        double range = c.huntRange();
        double reach = c.attackReach();
        List<ServerPlayer> candidates = new ArrayList<>(level.players());
        for (ServerPlayer extra : extraPlayers.apply(level)) {
            if (!candidates.contains(extra)) {
                candidates.add(extra);
            }
        }
        ServerPlayer best = null;
        double bestSq = range * range;
        for (ServerPlayer p : candidates) {
            if (!p.isAlive() || !p.canBeSeenAsEnemy() || p.level() != level) {
                continue; // creative, spectator or invulnerable players are never hunted (as vanilla's targeting)
            }
            if (p.getUUID().equals(forgotten) && now < forgottenUntil) {
                continue;
            }
            double dx = p.getX() - mob.getX();
            double dz = p.getZ() - mob.getZ();
            double sq = dx * dx + dz * dz;
            if (sq > bestSq || Math.abs(p.getY() - mob.getY()) > 6) {
                continue;
            }
            if (home != null && !MonsterAi.inAggressionRange(MonsterHome.fromHome(home, p.getX(), p.getZ()), reach)) {
                continue;
            }
            if (!mob.getSensing().hasLineOfSight(p) || !c.wantsToAttack(p)) {
                continue;
            }
            best = p;
            bestSq = sq;
        }
        return best;
    }

    private void startReturn(int now) {
        returning = true;
        returningSince = now;
        blockedFor = 0;
        mob.setTarget(null);
        mob.getNavigation().stop();
    }

    private void tickReturn(ServerLevel level, BlockPos home, int now) {
        if (home == null) {
            returning = false;
            return;
        }
        double fromHome = MonsterHome.fromHome(home, mob.getX(), mob.getZ());
        LivingEntity t = mob.getTarget();
        if (t != null) {
            // hit on its way home by someone it can fight from inside its range, or who keeps attacking it: back to the fight
            if (valid(t, home) && fromHome <= MonsterAi.LEASH_BLOCKS
                    && (MonsterAi.inAggressionRange(MonsterHome.fromHome(home, t.getX(), t.getZ()), c.attackReach())
                    || underFire(t, now))) {
                returning = false;
                return;
            }
            mob.setTarget(null);
        }
        if (fromHome <= MonsterAi.HOME_BLOCKS) {
            arrive();
            return;
        }
        if (now - returningSince > MonsterAi.RETURN_TIMEOUT_MC) {
            if (level.hasChunkAt(home)) {
                mob.teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
            }
            arrive();
            return;
        }
        BlockPos going = mob.getNavigation().getTargetPos();
        if (mob.getNavigation().isDone() || going == null || going.distManhattan(home) > 1 || (now - returningSince) % 20 == 0) {
            mob.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.0);
        }
    }

    /**
     * Home again: the fight is over, its health back to full and its damage ledger cleared, unless something hit it in the last
     * 5 s (it heals once the hits stop: a reset must not erase hits that are still coming).
     */
    private void arrive() {
        returning = false;
        mob.getNavigation().stop();
        if (MonsterAi.healsAtHome((long) mob.tickCount - lastHitAt)) {
            heal();
        } else {
            healPending = true;
        }
    }

    private void heal() {
        healPending = false;
        mob.setHealth(mob.getMaxHealth());
        KillCredit.forget(mob);
    }

    /**
     * The OSRS aggression test shared by every monster on OSRS AI: an aggressive monster attacks a player whose combat level is at
     * most twice its own (any level in the Wilderness), unless a module rule calms it (God Wars god items, revenants and a charged
     * bracelet, Mage Arena capes), the player is tolerated after 10 minutes in the area, or single-way combat forbids it.
     */
    public static boolean osrsAggression(Mob mob, ServerPlayer player, boolean aggressive, int level, String key) {
        return osrsAggression(mob, player, aggressive, level, key, false);
    }

    /**
     * {@link #osrsAggression(Mob, ServerPlayer, boolean, int, String)}; {@code regardless}: an encounter spawn (a boss's or quest's
     * summon) that attacks whatever the player's combat level and however long they have stayed (module rules and single-way
     * still apply).
     */
    public static boolean osrsAggression(Mob mob, ServerPlayer player, boolean aggressive, int level, String key, boolean regardless) {
        if (!aggressive || player.isCreative() || player.isSpectator() || Aggression.peaceful(mob, player)) {
            return false;
        }
        if (!regardless && !neverTolerant(key) && Aggression.tolerant(player, ActionManager.gameTick())) {
            return false;
        }
        if (!CombatZones.npcMayAttack(mob, player)) {
            return false;
        }
        return MonsterAi.aggressiveTo(level, Players.data(player).skills().combatLevel(),
                regardless || Gielinor.wildernessLevel(player) > 0);
    }

    /** Monsters that stay aggressive however long the player stays (wiki, Tolerance: Exceptions). */
    public static boolean neverTolerant(String key) {
        return key != null && (MonsterAi.NEVER_TOLERANT.contains(key)
                || com.runecraft.osrs.content.godwars.GodWarsFactions.allegiance(key) != null);
    }
}
