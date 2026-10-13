package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.systems.CombatAchievementRules.Metrics;
import com.runecraft.osrs.content.systems.CombatAchievementRules;
import com.runecraft.osrs.content.systems.CombatAchievements;
import com.runecraft.osrs.content.systems.CombatAchievements.Task;
import com.runecraft.osrs.content.systems.CombatAchievements.Track;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KetRakChallengesTest {
    @Test
    void challengesFollowTheWiki() {
        // OSRS Wiki "TzHaar-Ket-Rak's Challenges": N JalTok-Jads in challenge N; rewards in tokkul.
        int[] tokkul = {80, 190, 480, 1190, 2970, 7430};
        for (int c = 1; c <= KetRakChallenges.CHALLENGES; c++) {
            assertEquals(c, KetRakChallenges.jads(c));
            assertEquals(tokkul[c - 1], KetRakChallenges.tokkul(c));
            assertEquals(c >= 3, KetRakChallenges.requiresInferno(c));
        }
        assertTrue(KetRakChallenges.requiresInferno(KetRakChallenges.FINAL));
        assertEquals(0, KetRakChallenges.tokkul(KetRakChallenges.FINAL));
        assertNotNull(ContentRegistry.monster(TzHaarWaves.JALTOK_JAD));
        assertEquals(900, ContentRegistry.monster(TzHaarWaves.JALTOK_JAD).combatLevel);
        assertTrue(ItemCatalog.exists("tokkul"));
        assertEquals(2545, Minigame.TZHAAR_KET_RAK.realX());
        assertEquals(5110, Minigame.TZHAAR_KET_RAK.realY());
    }

    @Test
    void everyChallengeHasItsOwnEntrance() {
        Set<String> seen = new HashSet<>();
        for (int c = 1; c <= KetRakChallenges.FINAL; c++) {
            String e = KetRakChallenges.entrance(c);
            assertTrue(seen.add(e), e);
            assertEquals(c, KetRakChallenges.challengeOf(e));
            assertTrue(MinigameWorld.blocks().stream().anyMatch(b -> b.id().equals(e)), e);
            assertTrue(MinigameWorld.placeBlocks().get(Minigame.TZHAAR_KET_RAK.place()).contains(e), e);
        }
        assertEquals(1, KetRakChallenges.challengeOf("something_else"));
        assertTrue(MinigameWorld.placeBlocks().get(Minigame.TZHAAR_KET_RAK.place()).contains(MinigameWorld.KET_RAK_SCOREBOARD));
    }

    @Test
    void newMinigamesAreAppendedLast() {
        Minigame[] all = Minigame.values();
        assertEquals(Minigame.FIGHT_PITS.ordinal() + 1, Minigame.TZHAAR_KET_RAK.ordinal());
        assertEquals(Minigame.TZHAAR_KET_RAK.ordinal() + 1, Minigame.TEMPLE_TREKKING.ordinal());
        assertEquals(Minigame.TEMPLE_TREKKING.ordinal() + 1, Minigame.LAST_MAN_STANDING.ordinal());
        assertEquals(Minigame.LAST_MAN_STANDING.ordinal() + 1, Minigame.EMIRS_ARENA.ordinal());
        assertEquals(Minigame.EMIRS_ARENA, all[all.length - 1]);
    }

    @Test
    void combatAchievementHelpers() {
        assertTrue(KetRakChallenges.newBest(0, 500));
        assertTrue(KetRakChallenges.newBest(600, 500));
        assertFalse(KetRakChallenges.newBest(400, 500));
        assertTrue(KetRakChallenges.fluke(5, 6));
        assertFalse(KetRakChallenges.fluke(-6, 6));
        assertFalse(KetRakChallenges.fluke(4, 6));
        assertFalse(KetRakChallenges.fluke(5, 5));
        assertTrue(KetRakChallenges.multiStyle(List.of(Set.of("melee"), Set.of("ranged"), Set.of("magic")), 3));
        assertFalse(KetRakChallenges.multiStyle(List.of(Set.of("melee"), Set.of("melee"), Set.of("magic")), 3));
        assertFalse(KetRakChallenges.multiStyle(List.of(Set.of("melee", "ranged"), Set.of("magic")), 2));
        assertFalse(KetRakChallenges.multiStyle(List.of(Set.of("melee"), Set.of("magic")), 3));
        assertEquals("melee", KetRakChallenges.styleClass("STAB"));
        assertEquals("ranged", KetRakChallenges.styleClass("RANGED"));
        assertEquals("magic", KetRakChallenges.styleClass("MAGIC"));
        assertTrue(KetRakChallenges.allComplete(c -> 1));
        assertFalse(KetRakChallenges.allComplete(c -> c == 6 ? 0 : 2));
    }

    private static Task task(int wikiId) {
        return CombatAchievements.task("ca_" + wikiId);
    }

    @Test
    void combatAchievementsTrackTheChallenges() {
        // Every TzHaar-Ket-Rak's Challenges task (wiki ids 361-371) is tracked as a run of the minigame.
        for (int id = 361; id <= 371; id++) {
            Task t = task(id);
            assertNotNull(t, "task " + id);
            assertEquals(Track.RUN, t.track(), t.name());
            assertEquals("mg:" + Minigame.TZHAAR_KET_RAK.id(), t.target(), t.name());
        }
        Metrics m = new Metrics();
        m.stage = 2;
        assertTrue(CombatAchievementRules.meets(task(361).rule(), m));
        assertFalse(CombatAchievementRules.meets(task(362).rule(), m));
        // Speed-Trialist: the first challenge in under 45 seconds.
        m.stage = 1;
        m.ticks = 74;
        assertTrue(CombatAchievementRules.meets(task(364).rule(), m));
        m.ticks = 75;
        assertFalse(CombatAchievementRules.meets(task(364).rule(), m));
        // Supplies? Who Needs 'em?, Multi-Style Specialist and It Wasn't a Fluke read the session's flags.
        m.stage = 3;
        assertFalse(CombatAchievementRules.meets(task(369).rule(), m));
        m.flags.add(KetRakChallenges.FLAG_EMPTY_INVENTORY);
        assertTrue(CombatAchievementRules.meets(task(369).rule(), m));
        assertFalse(CombatAchievementRules.meets(task(371).rule(), m));
        m.flags.add(KetRakChallenges.FLAG_MULTI_STYLE);
        assertTrue(CombatAchievementRules.meets(task(371).rule(), m));
        m.stage = 6;
        assertFalse(CombatAchievementRules.meets(task(370).rule(), m));
        m.flags.add(KetRakChallenges.FLAG_FLUKE);
        assertTrue(CombatAchievementRules.meets(task(370).rule(), m));
        // Facing Jad Head-on III: the second challenge with melee only.
        m.stage = 2;
        m.styles.add(com.runecraft.osrs.AttackType.SLASH);
        assertTrue(CombatAchievementRules.meets(task(367).rule(), m));
        m.styles.add(com.runecraft.osrs.AttackType.RANGED);
        assertFalse(CombatAchievementRules.meets(task(367).rule(), m));
    }
}
