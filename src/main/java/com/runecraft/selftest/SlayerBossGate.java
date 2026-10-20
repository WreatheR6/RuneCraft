package com.runecraft.selftest;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.content.bosses.SlayerBossAccess;
import com.runecraft.content.slayer.SlayerService;
import com.runecraft.content.slayer.SlayerState;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.bosses.SlayerBossRules;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Inv;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

/**
 * The Slayer-task gate of a Slayer boss's fight check ({@code boss:fight:<id>}, {@link SlayerBossRules}): before the fight the
 * summon must be refused with no task (and for the Grotesque Guardians with the roof still locked, then opened by a brittle
 * key), then the boss's task is assigned so the fight is the one a player on task gets; after the kill the task must have
 * gone down and the kill must have given the boss's Slayer XP. The Slayer profile is the test player's own (TestContext
 * resets it before and restores a real player's after every check); the roof unlock is put back here.
 */
final class SlayerBossGate {
    private static final int TASK_AMOUNT = 10;

    private final TestContext c;
    private final BossDef def;
    private final SlayerBossRules.Access access;
    private final boolean roofBefore;
    private final StringBuilder refusals = new StringBuilder();
    private int remainingBefore;
    private long slayerXpBefore;

    private SlayerBossGate(TestContext c, BossDef def, SlayerBossRules.Access access) {
        this.c = c;
        this.def = def;
        this.access = access;
        this.roofBefore = SlayerBossAccess.roofUnlocked(c.player);
    }

    /** The gate of a Slayer boss (refusals checked, its task assigned), or null for a boss with no Slayer rule. */
    static SlayerBossGate open(TestContext c, BossDef def) {
        SlayerBossRules.Access access = SlayerBossRules.of(def.id);
        if (access == null) {
            return null;
        }
        SlayerBossGate g = new SlayerBossGate(c, def, access);
        g.refuseThenAssign();
        return g;
    }

    private SlayerState.Profile profile() {
        return SlayerService.profile(c.player);
    }

    private void refuseThenAssign() {
        SlayerState.Profile p = profile();
        p.task = null;
        p.remaining = 0;
        p.region = null;
        boolean guardians = "dusk".equals(def.id);
        if (guardians) {
            SlayerBossAccess.setRoofUnlocked(c.player, false);
            Inv.remove(c.player, SlayerBossRules.BRITTLE_KEY, Inv.count(c.player, SlayerBossRules.BRITTLE_KEY));
        }
        refused("no task");
        // a task the boss doesn't count towards is refused too
        assign("cows");
        refused("a cow task");
        String task = access.tasks().get(0);
        assign(task);
        if (guardians) {
            refused("a gargoyle task but the roof locked");
            Inv.give(c.player, SlayerBossRules.BRITTLE_KEY, 1);
        }
        remainingBefore = profile().remaining;
        slayerXpBefore = Players.data(c.player).skills().xp(Skill.SLAYER);
    }

    private void assign(String task) {
        SlayerState.Profile p = profile();
        p.task = task;
        p.remaining = TASK_AMOUNT;
        p.amount = TASK_AMOUNT;
        p.master = "duradel";
        p.region = null;
    }

    /** Summons the boss at the platform and checks nothing appeared. */
    private void refused(String why) {
        BlockPos at = c.near(0, 0);
        DataBossService.summon(c.player, c.world, at, def);
        List<DataBossEntity> found = c.world.getEntities(ModEntities.DATA_BOSS_TYPES.get(def.id), new AABB(at).inflate(48),
                e -> e.isAlive());
        if (!found.isEmpty()) {
            found.forEach(e -> e.discard());
            throw new AssertionError("summoned with " + why + ": " + access.bossName() + " needs a task of "
                    + String.join("/", access.tasks()));
        }
        refusals.append(refusals.length() == 0 ? "refused with " : ", ").append(why);
    }

    /** After a summon on task: the roof key used up (Guardians). */
    String afterSummon() {
        if (!"dusk".equals(def.id)) {
            return "";
        }
        if (!SlayerBossAccess.roofUnlocked(c.player) || Inv.has(c.player, SlayerBossRules.BRITTLE_KEY, 1)) {
            throw new AssertionError("the brittle key didn't open the roof (unlocked " + SlayerBossAccess.roofUnlocked(c.player)
                    + ", key still carried " + Inv.has(c.player, SlayerBossRules.BRITTLE_KEY, 1) + ")");
        }
        return ", brittle key opened the roof";
    }

    /** After the kill: the task went down and the Slayer XP is the boss's (its hitpoints or the wiki's figure). */
    String credit() {
        SlayerState.Profile p = profile();
        long xp = Players.data(c.player).skills().xp(Skill.SLAYER) - slayerXpBefore;
        double expected = com.runecraft.osrs.data.MonsterTraits.slayerXp(def.id, def.hitpoints);
        int done = remainingBefore - p.remaining;
        if (done <= 0) {
            throw new AssertionError("killed on a " + access.tasks().get(0) + " task, but the task didn't go down (" + p.remaining
                    + " left of " + remainingBefore + ")");
        }
        if (xp < Math.floor(expected)) {
            throw new AssertionError("the kill gave " + xp + " Slayer XP, expected " + expected);
        }
        return String.format(Locale.ROOT, ", task %s -%d, slayer xp +%d", access.tasks().get(0), done, xp);
    }

    String refusals() {
        return refusals.toString();
    }

    /** Puts the roof unlock back as it was (the profile is TestContext's). */
    void close() {
        SlayerBossAccess.setRoofUnlocked(c.player, roofBefore);
        Inv.remove(c.player, SlayerBossRules.BRITTLE_KEY, Inv.count(c.player, SlayerBossRules.BRITTLE_KEY));
    }
}
