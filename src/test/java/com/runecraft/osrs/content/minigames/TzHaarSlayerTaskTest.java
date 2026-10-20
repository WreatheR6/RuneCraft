package com.runecraft.osrs.content.minigames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.runecraft.osrs.content.slayer.SlayerMasters;
import com.runecraft.osrs.content.systems.CombatAchievements;
import com.runecraft.osrs.content.systems.CombatAchievements.Tier;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * A TzHaar Slayer task taken as a TzTok-Jad / TzKal-Zuk task (OSRS Wiki "TzTok-Jad", Slayer; "Slayer task/TzHaar"; the
 * master and grandmaster rewards on "Combat Achievements"): its size, the player's choice and the count-down to completion.
 */
class TzHaarSlayerTaskTest {
    /** One kill without Combat Achievements and up to elite, 2 from the master tier, 3 from the grandmaster tier. */
    @Test
    void sizeFollowsTheCombatAchievementTier() {
        assertEquals(1, CombatAchievements.tzhaarTaskKills(null));
        for (Tier t : Tier.values()) {
            int expected = t == Tier.GRANDMASTER ? 3 : t == Tier.MASTER ? 2 : 1;
            assertEquals(expected, CombatAchievements.tzhaarTaskKills(t), t.name());
        }
        assertEquals(TzHaarWaves.SLAYER_BOSS_TASK_MAX_KILLS, CombatAchievements.tzhaarTaskKills(Tier.GRANDMASTER));
    }

    /** The tier reached by points: the master threshold gives 2, one point short of it 1, the grandmaster threshold 3. */
    @Test
    void sizeFromCombatAchievementPoints() {
        int master = CombatAchievements.threshold(Tier.MASTER);
        int grandmaster = CombatAchievements.threshold(Tier.GRANDMASTER);
        assertEquals(1, CombatAchievements.tzhaarTaskKills(CombatAchievements.unlocked(0)));
        assertEquals(1, CombatAchievements.tzhaarTaskKills(CombatAchievements.unlocked(master - 1)));
        assertEquals(2, CombatAchievements.tzhaarTaskKills(CombatAchievements.unlocked(master)));
        assertEquals(2, CombatAchievements.tzhaarTaskKills(CombatAchievements.unlocked(grandmaster - 1)));
        assertEquals(3, CombatAchievements.tzhaarTaskKills(CombatAchievements.unlocked(grandmaster)));
        // Every task up to a tier unlocks exactly that tier (how the minigame:tzhaar_task self-test makes a master-tier player).
        for (Tier tier : Tier.values()) {
            List<String> done = CombatAchievements.tasks().stream().filter(t -> t.tier().ordinal() <= tier.ordinal())
                    .map(CombatAchievements.Task::id).toList();
            assertEquals(tier, CombatAchievements.unlocked(CombatAchievements.points(done)), tier.name());
        }
    }

    /** The player is offered 1 up to the tier's number of kills, never 0 and never more. */
    @Test
    void playerPicksOneUpToTheAllowance() {
        for (int allowed = 1; allowed <= 3; allowed++) {
            assertFalse(TzHaarWaves.slayerBossTaskSize(0, allowed));
            for (int chosen = 1; chosen <= allowed; chosen++) {
                assertTrue(TzHaarWaves.slayerBossTaskSize(chosen, allowed), chosen + " of " + allowed);
            }
            assertFalse(TzHaarWaves.slayerBossTaskSize(allowed + 1, allowed), (allowed + 1) + " of " + allowed);
        }
        assertFalse(TzHaarWaves.slayerBossTaskSize(4, 4), "never more than three, whatever the allowance");
    }

    /** Each boss kill takes one off; the task is done only after the last (2 kills: 1 left, then done). */
    @Test
    void completesOnlyAfterTheLastKill() {
        for (int size = 1; size <= 3; size++) {
            int left = size;
            for (int kill = 1; kill < size; kill++) {
                left = TzHaarWaves.slayerBossTaskLeft(left);
                assertEquals(size - kill, left, "kill " + kill + " of " + size);
                assertTrue(left > 0, "a " + size + "-kill task completed after kill " + kill);
            }
            assertEquals(0, TzHaarWaves.slayerBossTaskLeft(left), "the last of " + size + " kills completes it");
        }
        assertEquals(0, TzHaarWaves.slayerBossTaskLeft(0));
    }

    /**
     * An ordinary TzHaar task (Chaeldar 90-150, Nieve 110-180, Duradel 130-199) is far bigger than a boss task, so a boss
     * task's flag (which holds its size) never matches an ordinary TzHaar task assigned later.
     */
    @Test
    void ordinaryTzHaarTasksAreBiggerThanABossTask() {
        int masters = 0;
        for (SlayerMasters.Master m : SlayerMasters.all()) {
            SlayerMasters.Assignment a = m.assignment("tzhaar");
            if (a == null) {
                continue;
            }
            masters++;
            assertTrue(a.min() > TzHaarWaves.SLAYER_BOSS_TASK_MAX_KILLS, m.id() + " assigns " + a.min() + " TzHaar");
            assertTrue(a.extMin() == 0 || a.extMin() > TzHaarWaves.SLAYER_BOSS_TASK_MAX_KILLS, m.id());
        }
        assertTrue(masters >= 3, "TzHaar comes from Chaeldar, Nieve (and Steve) and Duradel");
        for (String id : new String[]{"chaeldar", "nieve", "duradel"}) {
            assertNotNull(SlayerMasters.get(id).assignment("tzhaar"), id);
        }
    }
}
