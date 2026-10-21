package com.runecraft.entity;

import com.runecraft.combat.MonsterStats;
import com.runecraft.osrs.MonsterAi;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

/**
 * Low-hitpoint retreat (round 43): a monster the wiki lists as retreating when its hitpoints run low ({@link MonsterAi#RETREATS}:
 * chickens, ghouls, giant rock crabs, imps, seagulls, wallasalki, the God Wars werewolf...) stops attacking at
 * {@link MonsterAi#retreatAtHp} and runs from the player it was fighting, staying inside its max range around its spawn point
 * (OSRS Wiki, Retreat: "the area in which the NPC is confined to when it is in retreat mode ... almost always seven tiles"; one
 * cornered against a wall or the edge of its range stops there). It keeps its target and turns on it again once it has
 * regenerated above the threshold ({@link MonsterAi#REGEN_TICKS}).
 *
 * <p>It takes the movement and look of the monster above its attack goal (priority 1), so the attack goal stands down while it runs.
 */
public class OsrsRetreatGoal extends Goal {
    /** How often (MC) a retreating monster picks a new spot away from the player while it still has a way to go. */
    private static final int REPICK_MC = 20;
    /** Tries at a spot away from the player inside its range before it stays where it is. */
    private static final int TRIES = 6;

    private final OsrsCombatant c;
    private final Mob mob;
    private int pickIn;

    public OsrsRetreatGoal(OsrsCombatant c) {
        this.c = c;
        this.mob = c.self();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /** Whether this monster retreats now: one that retreats, after a living player, with its hitpoints at or below the threshold. */
    public static boolean retreating(OsrsCombatant c) {
        Mob mob = c.self();
        MonsterStats s = c.stats();
        return s != null && MonsterAi.retreats(s.traitsKey()) && mob.getTarget() instanceof ServerPlayer p && p.isAlive()
                && MonsterAi.lowEnoughToRetreat(mob.getHealth(), mob.getMaxHealth());
    }

    @Override
    public boolean canUse() {
        return mob instanceof PathfinderMob && retreating(c);
    }

    @Override
    public boolean canContinueToUse() {
        return retreating(c);
    }

    @Override
    public void start() {
        pickIn = 0;
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (!(mob.getTarget() instanceof ServerPlayer from) || !(mob instanceof PathfinderMob walker)) {
            return;
        }
        if (--pickIn > 0 && !mob.getNavigation().isDone()) {
            return;
        }
        pickIn = REPICK_MC;
        Vec3 spot = spotAway(walker, from);
        if (spot != null) {
            mob.getNavigation().moveTo(spot.x, spot.y, spot.z, 1.0);
        } else {
            mob.getNavigation().stop(); // cornered: it stays put
        }
    }

    /**
     * A reachable spot up to {@link MonsterAi#RETREAT_STEP_BLOCKS} away from the player and further from them than the monster is
     * now, inside its max range around its spawn point (an encounter monster without one: anywhere), or null.
     */
    private Vec3 spotAway(PathfinderMob walker, ServerPlayer from) {
        BlockPos home = MonsterHome.get(mob);
        double now = mob.distanceToSqr(from);
        // out past its max range already (it chased the player there): any spot no further out than it stands
        double range = home == null ? 0 : Math.max(MonsterAi.MAX_RANGE_BLOCKS, MonsterHome.fromHome(home, mob.getX(), mob.getZ()));
        int step = (int) MonsterAi.RETREAT_STEP_BLOCKS;
        for (int i = 0; i < TRIES; i++) {
            Vec3 v = DefaultRandomPos.getPosAway(walker, step, 4, from.position());
            if (v == null) {
                continue;
            }
            boolean further = from.distanceToSqr(v) > now;
            boolean inRange = home == null || MonsterHome.fromHome(home, v.x, v.z) <= range;
            if (further && inRange) {
                return v;
            }
        }
        return null;
    }
}
