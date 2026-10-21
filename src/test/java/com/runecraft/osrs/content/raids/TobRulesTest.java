package com.runecraft.osrs.content.raids;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.systems.CombatAchievementRules;
import com.runecraft.osrs.content.systems.CombatAchievements;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Theatre of Blood room rules (round 33) against the OSRS wiki. */
class TobRulesTest {
    @Test
    void supplyChestStockAndPrices() {
        assertEquals(7, TobRules.SUPPLIES.size());
        assertEquals(1, TobRules.supply("stamina_potion").cost());
        assertEquals(1, TobRules.supply("stamina_potion").perChest());
        assertEquals(2, TobRules.supply("prayer_potion").cost());
        assertEquals(3, TobRules.supply("saradomin_brew").cost());
        assertEquals(3, TobRules.supply("super_restore").cost());
        assertEquals(1, TobRules.supply("shark").cost());
        assertEquals(2, TobRules.supply("sea_turtle").cost());
        assertEquals(2, TobRules.supply("manta_ray").cost());
        assertNull(TobRules.supply("dragon_claws"));
        for (TobRules.Supply s : TobRules.SUPPLIES) {
            assertTrue(ItemCatalog.exists(s.item()), "the supply chest sells unknown " + s.item());
        }
    }

    @Test
    void chestPointsFollowPerformance() {
        Random r = new Random(5);
        for (int i = 0; i < 500; i++) {
            int above = TobRules.chestPoints(TobRules.Performance.ABOVE_AVERAGE, r);
            int avg = TobRules.chestPoints(TobRules.Performance.AVERAGE, r);
            int below = TobRules.chestPoints(TobRules.Performance.BELOW_AVERAGE, r);
            assertTrue(above >= 10 && above <= 13, "above average " + above);
            assertTrue(avg >= 8 && avg <= 11, "average " + avg);
            assertTrue(below >= 6 && below <= 9, "below average " + below);
        }
        assertEquals(TobRules.Performance.ABOVE_AVERAGE, TobRules.performance(0, true));
        assertEquals(TobRules.Performance.AVERAGE, TobRules.performance(0, false));
        assertEquals(TobRules.Performance.BELOW_AVERAGE, TobRules.performance(1, true));
        assertTrue(TobRules.onionOnly(2));
        assertFalse(TobRules.onionOnly(1));
        // Hard mode chests give fewer points: at most 9 each, 18 over both (wiki "Theatre of Blood/Hard Mode").
        for (int i = 0; i < 500; i++) {
            int hard = TobRules.chestPoints(TobRules.Performance.ABOVE_AVERAGE, RaidMode.HARD, r);
            assertTrue(hard >= 6 && hard <= 9, "hard mode " + hard);
        }
    }

    @Test
    void entryModeBandages() {
        assertEquals(10, TobRules.ENTRY_BANDAGES);
        assertTrue(ItemCatalog.exists(TobRules.BANDAGES));
        assertTrue(RaidItems.RAID_ONLY_ITEMS.contains(TobRules.BANDAGES), "bandages never leave the Theatre");
        assertEquals(20, ItemCatalog.get(TobRules.BANDAGES).heal());
        // One level below a super potion (floor(15%) + 4) and a ranging potion's floor(10%) + 4 (wiki boost tables).
        assertEquals(18, TobRules.bandageMeleeBoost(99));
        assertEquals(4, TobRules.bandageMeleeBoost(6));
        assertEquals(5, TobRules.bandageMeleeBoost(7));
        assertEquals(13, TobRules.bandageRangedBoost(99));
        assertEquals(4, TobRules.bandageRangedBoost(9));
    }

    @Test
    void maidenNumbers() {
        assertEquals(36, TobRules.maidenBlackstormMax(0));
        assertEquals(40, TobRules.maidenBlackstormMax(1));
        assertEquals(71, TobRules.maidenBlackstormMax(10));
        assertEquals(10, TobRules.maidenSplat(0));
        assertEquals(16, TobRules.maidenSplat(3));
        assertEquals(8, TobRules.maidenDrain(39));
        assertEquals(0.1, TobRules.bloodSpawnChance(false));
        assertEquals(0.2, TobRules.bloodSpawnChance(true));
        // Wiki "Blood spawn": up to eight in a fight, one per throw when everyone dodged; trails 5-13 (2-5 Entry mode).
        assertEquals(8, TobRules.MAIDEN_BLOOD_SPAWN_CAP);
        assertEquals(1, TobRules.bloodSpawnsFromThrow(false, 4));
        assertEquals(4, TobRules.bloodSpawnsFromThrow(true, 4));
        assertEquals(5, TobRules.bloodTrailMin(RaidMode.NORMAL));
        assertEquals(13, TobRules.bloodTrailMax(RaidMode.HARD));
        assertEquals(2, TobRules.bloodTrailMin(RaidMode.ENTRY));
        assertEquals(5, TobRules.bloodTrailMax(RaidMode.ENTRY));
    }

    @Test
    void bloatRunsBetweenFortyAndSixtyPercent() {
        assertFalse(TobRules.bloatRuns(0.9, 0));
        assertTrue(TobRules.bloatRuns(0.5, 0));
        assertTrue(TobRules.bloatRuns(0.4, 0));
        assertFalse(TobRules.bloatRuns(0.3, 0));
        assertTrue(TobRules.bloatRuns(0.3, 1));
        assertEquals(38, RaidScaling.TOB_BLOAT_FIRST_WALK);
        assertEquals(30, TobRules.BLOAT_STOMP_TICK);
    }

    @Test
    void sotetsegAndXarpus() {
        assertEquals(15, TobRules.mazeWrongTile(0));
        assertEquals(21, TobRules.mazeWrongTile(99));
        assertEquals(17, TobRules.mazeWrongTile(99, RaidMode.ENTRY), "+ 11 in Entry mode (wiki)");
        // The tornado comes once someone steps onto the fourth row (row 3).
        assertFalse(TobRules.mazeTornado(2));
        assertTrue(TobRules.mazeTornado(3));
        assertEquals(100, TobRules.SOTETSEG_DEFENCE_FLOOR);
        // Hard mode Xarpus: the two outer rings start poisoned except the six entrance tiles (west side, z = -1..1).
        int half = 8;
        assertTrue(TobRules.xarpusHardEdge(half, 0, half));
        assertTrue(TobRules.xarpusHardEdge(half - 1, 3, half));
        assertFalse(TobRules.xarpusHardEdge(half - 2, 0, half), "the third ring stays clear");
        assertFalse(TobRules.xarpusHardEdge(-half, 0, half), "entrance");
        assertFalse(TobRules.xarpusHardEdge(-half + 1, 1, half), "entrance");
        assertTrue(TobRules.xarpusHardEdge(-half, 2, half));
        assertFalse(TobRules.xarpusHardEdge(0, 0, half));
        assertEquals(2, TobRules.xarpusHardStare(1, 2, true));
        assertEquals(1, TobRules.xarpusHardStare(1, 2, false));
        assertEquals(12, TobRules.xarpusExhumeds(1));
        assertEquals(12, TobRules.xarpusExhumeds(3));
        assertEquals(15, TobRules.xarpusExhumeds(4));
        assertEquals(15, TobRules.xarpusExhumeds(5));
        assertEquals(8, TobRules.xarpusPoison(8, 0));
        assertTrue(TobRules.xarpusPoison(8, 10) > 8);
        assertEquals(75, TobRules.xarpusRetaliation(75, 0));
        Random r = new Random(9);
        Set<Integer> seen = new HashSet<>();
        int q = 0;
        for (int i = 0; i < 400; i++) {
            int next = TobRules.xarpusNextQuadrant(q, r);
            assertNotEquals(q, next, "Xarpus stared at the same quadrant twice");
            assertTrue(next >= 0 && next < 4);
            seen.add(next);
            q = next;
        }
        assertEquals(4, seen.size());
    }

    @Test
    void verzikNumbers() {
        assertEquals(10, TobRules.verzikP1Cap(50, true, false));
        assertEquals(3, TobRules.verzikP1Cap(50, false, false));
        assertEquals(2, TobRules.verzikP1Cap(2, false, false));
        assertEquals(80, TobRules.verzikP1Cap(80, false, true));
        assertEquals(74, TobRules.greenBall(99));
        assertEquals(137, TobRules.VERZIK_P1_BLAST_MAX);
        assertEquals(4, TobRules.P3_ROTATION.size());
        assertEquals(TobRules.P3Special.NYLOCAS, TobRules.P3_ROTATION.get(0));
        assertTrue(TobRules.VERZIK_ENRAGE_AT < TobRules.VERZIK_P25_AT);
        assertTrue(TobRules.VERZIK_HARD_LAST_STAND_AT < TobRules.VERZIK_ENRAGE_AT);
    }

    @Test
    void verzikPhaseTwoFromHerPage() {
        // Wiki "Verzik Vitur" (and its infobox) settles the explosion numbers: nylocas up to 63, the Athanatos landing 78.
        assertEquals(63, TobRules.VERZIK_NYLO_MAX);
        assertEquals(78, TobRules.ATHANATOS_LANDING_MAX);
        assertEquals(44, TobRules.VERZIK_URN_MAX);
        assertEquals(50, TobRules.VERZIK_SLAM_MAX, "infobox: 50 (bounce)");
        assertEquals(48, TobRules.VERZIK_LIGHTNING_MAX);
        assertEquals(25, TobRules.VERZIK_LIGHTNING_INSULATED_MAX);
        // Mod Ash: the ball passing through her deals "15-20. No accuracy calculation."; it strikes the fourth player reached
        assertEquals(15, TobRules.VERZIK_LIGHTNING_SELF_MIN);
        assertEquals(20, TobRules.VERZIK_LIGHTNING_SELF_MAX);
        assertEquals(4, RaidRealtime.VERZIK_LIGHTNING_PLAYERS);
        assertEquals(5, TobRules.VERZIK_LIGHTNING_EVERY);
        assertEquals(7, TobRules.DAWNBRINGER_RANGE_TILES);
        // Raw damage barely hurts the Athanatos.
        assertEquals(10, TobRules.athanatosRawDamage(40));
        assertEquals(0, TobRules.athanatosRawDamage(-3));
        // Phase 2.5: 200-hitpoint Matomenos, one when a single player is left alive (May 2024 update).
        assertEquals(200, TobRules.VERZIK_MATOMENOS_HP);
        assertEquals(2, TobRules.verzikMatomenos(3));
        assertEquals(1, TobRules.verzikMatomenos(1));
        // Phase 3: tornadoes take half the current hitpoints, at least 5.
        assertEquals(5, TobRules.tornadoHit(6));
        assertEquals(57, TobRules.tornadoHit(115));
        // Hard mode green ball: lethal unbounced with company, 89 / 74 / 49 after one to three bounces.
        assertEquals(100, TobRules.greenBallHard(99, 0, 2, 99));
        assertEquals(74, TobRules.greenBallHard(99, 0, 1, 99), "a lone player takes the normal hit");
        assertEquals(89, TobRules.greenBallHard(99, 1, 3, 99));
        assertEquals(74, TobRules.greenBallHard(99, 2, 3, 99));
        assertEquals(49, TobRules.greenBallHard(99, 3, 3, 99));
        assertEquals(3, TobRules.yellowVolleys(RaidMode.HARD));
        assertEquals(1, TobRules.yellowVolleys(RaidMode.NORMAL));
    }

    @Test
    void round36CombatAchievementsAreTracked() {
        for (int id : new int[]{243, 373, 375, 377, 378, 379, 391, 394, 395}) {
            assertNotEquals(CombatAchievements.Track.UNTRACKED, CombatAchievements.task("ca_" + id).track(), "ca_" + id);
        }
        // Perfect Theatre: a deathless Normal mode raid with no room's perfect condition broken.
        String perfect = CombatAchievements.task("ca_243").rule();
        CombatAchievementRules.Metrics m = new CombatAchievementRules.Metrics();
        m.raid = new CombatAchievementRules.Raid("TOB", "NORMAL", 3, 0, Set.of(), 0, 0, 0, false, 54, false);
        assertTrue(CombatAchievementRules.meets(perfect, m));
        m.flags.add(CombatAchievements.FLAG_NYLO_OFF_PRAYER);
        assertFalse(CombatAchievementRules.meets(perfect, m));
        // Harder Mode III: no melee on her third form in Hard mode.
        CombatAchievementRules.Metrics hm = new CombatAchievementRules.Metrics();
        hm.raid = new CombatAchievementRules.Raid("TOB", "HARD", 3, 0, Set.of(), 0, 0, 0, false, 54, false);
        String harder3 = CombatAchievements.task("ca_379").rule();
        assertTrue(CombatAchievementRules.meets(harder3, hm));
        hm.flags.add(CombatAchievements.FLAG_VERZIK_P3_MELEE);
        assertFalse(CombatAchievementRules.meets(harder3, hm));
        // Just To Be Safe needs a team of at least two that split the ball together.
        CombatAchievementRules.Metrics entry = new CombatAchievementRules.Metrics();
        entry.raid = new CombatAchievementRules.Raid("TOB", "ENTRY", 1, 0, Set.of(), 0, 0, 0, false, 18, false);
        entry.flags.add(CombatAchievements.FLAG_SOTETSEG_TEAM_SPLIT);
        assertFalse(CombatAchievementRules.meets(CombatAchievements.task("ca_391").rule(), entry), "solo can't do it");
    }

    @Test
    void nylocasStyleRules() {
        assertEquals(0, TobNylocas.wrongStyleReflect(30, 11, false), "normal mode: immune to that player, nothing reflected");
        assertEquals(11, TobNylocas.wrongStyleReflect(30, 11, true), "hard mode: reflected, capped at the nylocas's hitpoints");
        assertEquals(7, TobNylocas.wrongStyleReflect(7, 11, true));
        assertEquals(17, TobNylocas.VASILIAS_PRAYED_MAX);
        assertEquals(20, TobNylocas.vasiliasHardBounce(40, true));
        assertEquals(60, TobNylocas.vasiliasHardBounce(40, false));
    }
}
