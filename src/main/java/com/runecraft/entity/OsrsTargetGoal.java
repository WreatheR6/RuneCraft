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
 * fled beyond its aggression range, it drops the fight and walks home, keeping its damage (round 43: OSRS confines a monster to 7
 * tiles of its spawn point and it regenerates 1 hitpoint a minute, {@link MonsterAi#REGEN_TICKS}; round 42's full heal at home is
 * gone). A target that keeps attacking it from beyond its range is not given up: the monster stands at the edge of its max range
 * ({@link MonsterAi#holdAtEdge}, OSRS safespotting), so a player out-ranging it can still kill it.</li>
 * <li>Encounter arena (a boss's or quest's summon, a clue guardian, a superior, a Barrows tunnel monster: no spawn point; not a
 * boss, raid or minigame creature, whose rooms keep them): it drops a target further than {@link MonsterAi#ENCOUNTER_LEASH_BLOCKS}
 * from where it appeared (its boss's lair for a lair minion), picks none there and walks back when it strayed out with nobody to
 * fight (// playability: round 42 chased 48 blocks).</li>
 * <li>Single-way: a monster that may not attack its target (the player is fighting something else) drops it after
 * {@link MonsterAi#BLOCKED_DROP_MC} instead of hovering at the player's side (// playability). Every
 * {@link MonsterAi#REACH_CHECK_MC} it checks it can still get at its target ({@link #pathBlocked}): one walled off for
 * {@link MonsterAi#PATH_BLOCKED_MC} lets the single-way lock go even in sight (round 43).</li>
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
    /**
     * Who (credited: a thrall's owner) landed the last hit or miss, at {@link #lastHurtStamp}. Kept here because vanilla forgets
     * {@code getLastHurtByMob} 100 MC after the hit, shorter than the slowest player attack (the dark bow's 9 ticks).
     */
    private UUID lastAttacker;
    private int blockedFor;
    private UUID forgotten;
    private int forgottenUntil;
    private boolean returning;
    private int returningSince;
    /** Standing at the edge of its max range while its target attacks it from further out. */
    private boolean holding;
    /** Since when (MC, {@link ActionManager#mcTick}) it has had no way to {@link #reachTarget}; -1: it has one. */
    private long blockedSince = -1;
    /** The target {@link #blockedSince} is about. */
    private LivingEntity reachTarget;
    /** An encounter monster's arena centre (see {@link #anchor}), once looked up. */
    private BlockPos anchor;
    private boolean anchorKnown;
    /** Persistent-data key of an encounter monster's arena centre (where it first stood; a packed block position). */
    public static final String ANCHOR_KEY = "RcAnchor";

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

    /**
     * Whether this monster has had no way to {@code fighter} for {@link MonsterAi#PATH_BLOCKED_MC}: out of its attack range (or
     * sight, for one that attacks from range) and no path reaches them (a wall, a fence or water between them, even with a clear
     * line of sight). The single-way lock lets go then ({@code CombatZones}).
     */
    public boolean pathBlocked(LivingEntity fighter) {
        return fighter != null && fighter == reachTarget && MonsterAi.pathBlocked(blockedSince, ActionManager.mcTick());
    }

    /** An encounter monster's arena centre (where it first stood, or its boss's lair), or null for one without an arena. */
    public BlockPos arenaAnchor() {
        return anchor();
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
            lastAttacker = credited(hurtBy).getUUID();
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
        } else if (target == null) {
            walkBackToArena(level);
        }
        trackReach(target);
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
        if (mob.distanceToSqr(target) > max * max) {
            return false;
        }
        BlockPos arena = home == null ? anchor() : null;
        return arena == null || !MonsterAi.outsideArena(MonsterHome.fromHome(arena, target.getX(), target.getZ()));
    }

    /**
     * An encounter monster's arena centre ({@link MonsterAi#ENCOUNTER_LEASH_BLOCKS}): its boss's lair for a lair minion, else where
     * it first stood (kept in its persistent data). Null for open-world monsters (they have a spawn point and its leash), bosses
     * (their lair rules), raid and minigame creatures (their rooms and sessions keep them).
     */
    private BlockPos anchor() {
        if (anchorKnown) {
            return anchor;
        }
        anchorKnown = true;
        if (MonsterHome.get(mob) != null || mob instanceof BossEntity || mob instanceof com.runecraft.boss.DataBossEntity
                || ownedElsewhere(mob)) {
            anchor = null;
            return null;
        }
        String lair = com.runecraft.osrs.content.bosses.BossLair.MINION_TAG;
        for (String tag : mob.getTags()) {
            if (tag.startsWith(lair)) {
                try {
                    anchor = BlockPos.of(Long.parseLong(tag.substring(lair.length())));
                    return anchor;
                } catch (NumberFormatException ignored) {
                    break;
                }
            }
        }
        if (!mob.getPersistentData().contains(ANCHOR_KEY)) {
            mob.getPersistentData().putLong(ANCHOR_KEY, mob.blockPosition().asLong());
        }
        anchor = BlockPos.of(mob.getPersistentData().getLong(ANCHOR_KEY));
        return anchor;
    }

    /** A raid or minigame creature: its room or session keeps it in and picks its targets. */
    private static boolean ownedElsewhere(Mob mob) {
        return mob.getTags().contains(com.runecraft.content.minigames.MinigameSession.TAG)
                || com.runecraft.content.raids.RaidBuilder.slotAt(mob.getX(), mob.getZ()) >= 0;
    }

    /** An encounter monster out of its arena with nobody to fight walks back in (no heal: it keeps its damage). */
    private void walkBackToArena(ServerLevel level) {
        BlockPos arena = anchor();
        if (arena == null || mob.tickCount % 20 != 0
                || !MonsterAi.outsideArena(MonsterHome.fromHome(arena, mob.getX(), mob.getZ()) + MonsterAi.HOME_BLOCKS)
                || !level.hasChunkAt(arena)) {
            return;
        }
        mob.getNavigation().moveTo(arena.getX() + 0.5, arena.getY(), arena.getZ() + 0.5, 1.0);
    }

    /**
     * Every {@link MonsterAi#REACH_CHECK_MC}: whether this monster can still get at its player target (it can hit them from where it
     * stands, or a path reaches them); {@link #pathBlocked} reads the result. Only where single-way combat can lock a player: not
     * for bosses, raid or minigame creatures (exempt or multi) nor in a multicombat area.
     */
    private void trackReach(LivingEntity target) {
        if (!(target instanceof ServerPlayer) || mob instanceof BossEntity || mob instanceof com.runecraft.boss.DataBossEntity
                || ownedElsewhere(mob)) {
            reachTarget = null;
            blockedSince = -1;
            return;
        }
        if (target != reachTarget) {
            reachTarget = target;
            blockedSince = -1;
        }
        if (mob.tickCount % MonsterAi.REACH_CHECK_MC != 0) {
            return;
        }
        if (CombatZones.isMulti(target)) {
            blockedSince = -1; // no lock to release
        } else if (reachable(target)) {
            blockedSince = -1;
        } else if (blockedSince < 0) {
            blockedSince = ActionManager.mcTick();
        }
    }

    /**
     * Its target landed the last hit (or miss) on it within {@link MonsterAi#UNDER_FIRE_MC}: the fight is still on wherever the
     * target stands. Read from this goal's own record, not {@code getLastHurtByMob}, which vanilla clears 100 MC after the hit: a
     * dark bow shooting every 108 MC left the monster not under fire for the last 8 MC of every cycle, and it turned for home.
     */
    private boolean underFire(LivingEntity target, int now) {
        return lastAttacker != null && lastAttacker.equals(target.getUUID())
                && (long) now - lastHurtStamp < MonsterAi.UNDER_FIRE_MC;
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
            BlockPos arena = home == null ? anchor() : null;
            if (arena != null && MonsterAi.outsideArena(MonsterHome.fromHome(arena, p.getX(), p.getZ()))) {
                continue; // an encounter monster hunts only in its arena
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
     * Home again: the fight is over. It keeps its damage and its damage ledger (round 43, OSRS: no reset; it regenerates a hitpoint a
     * minute, {@link MonsterAi#REGEN_TICKS}, and forgets who hurt it once it is back to full health, see {@link #regenerate}).
     */
    private void arrive() {
        returning = false;
        mob.getNavigation().stop();
    }

    /**
     * One step of OSRS hitpoint regeneration for a monster on OSRS AI ({@link MonsterAi#REGEN_TICKS}: 1 hitpoint every 100 game
     * ticks, counted on its own age), called every Minecraft tick by its owner; back to full health, it forgets who damaged it
     * ({@link KillCredit}). Bosses, raid and minigame creatures are left to their own scripts.
     */
    public static void regenerate(Mob mob) {
        if (!mob.isAlive() || mob.getHealth() >= mob.getMaxHealth() || !MonsterAi.regenDue(mob.tickCount)
                || mob instanceof BossEntity || mob instanceof com.runecraft.boss.DataBossEntity || ownedElsewhere(mob)) {
            return;
        }
        mob.setHealth(MonsterAi.regenerated(mob.getHealth(), mob.getMaxHealth()));
        if (mob.getHealth() >= mob.getMaxHealth()) {
            KillCredit.forget(mob);
        }
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
