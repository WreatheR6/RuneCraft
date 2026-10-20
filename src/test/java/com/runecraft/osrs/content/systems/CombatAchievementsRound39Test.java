package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.systems.CombatAchievementRules.Metrics;
import com.runecraft.osrs.content.systems.CombatAchievementRules.Raid;
import com.runecraft.osrs.content.systems.CombatAchievements.Task;
import com.runecraft.osrs.content.systems.CombatAchievements.Tier;
import com.runecraft.osrs.content.systems.CombatAchievements.Track;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round 39 Combat Achievements sweep: raid kill counts from the completion counts, the Tombs of Amascut mode levels,
 * checkpoint tasks (perfect rooms, Playing with Jads), held-weapon conditions, the thrall counter, the Giants' Den
 * collection, the slayer-task speed tasks and the reward helpers, each passing when its condition holds and failing
 * when it doesn't (OSRS Wiki task pages, July 2026).
 */
class CombatAchievementsRound39Test {
    private static Task task(int wikiId) {
        Task t = CombatAchievements.task("ca_" + wikiId);
        assertNotNull(t, "ca_" + wikiId);
        return t;
    }

    private static boolean meets(int wikiId, Metrics m) {
        return CombatAchievementRules.meets(task(wikiId).rule(), m);
    }

    private static Raid raid(String type, String mode, int size, int level) {
        return new Raid(type, mode, size, level, Set.of(), 0, 0, 0, false, 0, false);
    }

    @Test
    void killCountsCountTheRightThings() {
        // Phosani's Nightmare counts for the Nightmare, the Corrupted Gauntlet for the Gauntlet, KBD and brutal black dragons
        // for "Big, Black and Fiery".
        Map<String, Integer> kc = new HashMap<>(Map.of("the_nightmare", 10, "phosanis_nightmare", 15));
        assertEquals(25, CombatAchievements.kcValue(task(184).target(), k -> kc.getOrDefault(k, 0)));
        kc.put("corrupted_hunllef", 5);
        assertEquals(5, CombatAchievements.kcValue(task(109).target(), k -> kc.getOrDefault(k, 0)));
        assertTrue(CombatAchievements.killMatches(task(32).target(), "King Black Dragon"));
        assertTrue(CombatAchievements.killMatches(task(32).target(), "Brutal black dragon"));
        assertFalse(CombatAchievements.killMatches(task(32).target(), "Baby black dragon"));
        // Raid completions: ToB counts Normal and Hard but not Entry; CoX counts Challenge Mode; the ToA modes by level.
        Map<String, Integer> raids = new HashMap<>();
        raids.put(CombatAchievements.TOB_COMPLETIONS, 30); // every mode
        raids.put(CombatAchievements.TOB_ENTRY_COMPLETIONS, 10);
        raids.put(CombatAchievements.TOB_HARD_COMPLETIONS, 4);
        raids.put(CombatAchievements.COX_COMPLETIONS, 20);
        raids.put(CombatAchievements.COX_CM_COMPLETIONS, 6);
        raids.put(CombatAchievements.TOA_COMPLETIONS, 40);
        raids.put(CombatAchievements.TOA_ENTRY_COMPLETIONS, 15);
        raids.put(CombatAchievements.TOA_EXPERT_COMPLETIONS, 5);
        java.util.function.ToIntFunction<String> r = k -> raids.getOrDefault(k, 0);
        assertEquals(20, CombatAchievements.kcValue(task(237).target(), r)); // ToB Veteran: 30 - 10 Entry
        assertEquals(10, CombatAchievements.kcValue(task(397).target(), r)); // ToB: SM Adept
        assertEquals(4, CombatAchievements.kcValue(task(385).target(), r)); // HM Grandmaster
        assertEquals(26, CombatAchievements.kcValue(task(299).target(), r)); // CoX Veteran with CM
        assertEquals(6, CombatAchievements.kcValue(task(288).target(), r)); // CM Master
        assertEquals(40, CombatAchievements.kcValue(task(461).target(), r)); // Entry mode or above
        assertEquals(25, CombatAchievements.kcValue(task(468).target(), r)); // level 150+
        assertEquals(5, CombatAchievements.kcValue(task(470).target(), r)); // Expert
        for (int id : new int[]{237, 238, 239, 397, 385, 288, 289, 299, 300, 301, 461, 462, 463, 466, 467, 468, 469, 470, 471}) {
            assertEquals(Track.KC, task(id).track(), task(id).name());
        }
        // A subtraction never goes below zero.
        assertEquals(0, CombatAchievements.kcValue("a|-b", k -> k.equals("b") ? 3 : 1));
    }

    @Test
    void toaTasksNeedTheirPagesRaidLevel() {
        Metrics m = new Metrics();
        m.raid = raid("TOA", "NORMAL", 1, 100);
        assertFalse(meets(449, m), "Dropped the ball needs raid level 150");
        m.raid = raid("TOA", "NORMAL", 1, 150);
        assertTrue(meets(449, m));
        // Expert mode tasks (and Fancy feet) need 300.
        assertTrue(task(429).rule().startsWith("level>=300"), task(429).rule());
        assertTrue(task(450).rule().contains("level>=300"));
        // Entry mode tasks keep any level; rules naming a level keep theirs.
        assertEquals("level>=50", task(464).rule());
        assertTrue(task(422).rule().contains("level>=300") && !task(422).rule().contains("level>=150"));
    }

    @Test
    void perfectRoomsAndPlayingWithJadsAreCheckpoints() {
        for (int id : new int[]{432, 433, 434, 435, 436, 437, 438, 439, 440, 441, 442, 443, 344, 390}) {
            assertTrue(CombatAchievementRules.hasFlag(task(id).rule(), "checkpoint"), task(id).name());
        }
        Metrics m = new Metrics();
        m.raid = raid("TOA", "NORMAL", 2, 150);
        assertFalse(meets(432, m));
        m.flags.add(com.runecraft.osrs.content.raids.ToaLoot.FLAG_PERFECT_HET);
        assertTrue(meets(432, m));
        assertFalse(meets(434, m), "Perfection of Het needs Expert mode and Perfect Akkha too");
        m.flags.add(com.runecraft.osrs.content.raids.ToaLoot.FLAG_PERFECT_AKKHA);
        assertFalse(meets(434, m));
        m.raid = raid("TOA", "EXPERT", 2, 300);
        assertTrue(meets(434, m));
        // Perfect Akkha: the room's flag in a group with every Akkha invocation.
        assertFalse(meets(433, m));
        Metrics a = new Metrics();
        a.flags.add(com.runecraft.osrs.content.raids.ToaLoot.FLAG_PERFECT_AKKHA);
        a.raid = new Raid("TOA", "NORMAL", 2, 150, Set.of(CombatAchievementRules.AKKHA.split(",")), 0, 0, 0, false, 0, false);
        assertTrue(meets(433, a));
        // Playing with Jads.
        Metrics j = new Metrics();
        assertFalse(meets(344, j));
        j.flags.add(CombatAchievements.FLAG_JADS_QUICK);
        assertTrue(meets(344, j));
        // Nylocas, On the Rocks: Entry mode only.
        Metrics n = new Metrics();
        n.flags.add(CombatAchievements.FLAG_NYLO_BARRAGE);
        n.raid = raid("TOB", "NORMAL", 2, 0);
        assertFalse(meets(390, n));
        n.raid = raid("TOB", "ENTRY", 2, 0);
        assertTrue(meets(390, n));
    }

    @Test
    void heldWeaponConditions() {
        assertEquals("ranged", CombatAchievementRules.weaponStyle("twisted_bow"));
        assertEquals("melee", CombatAchievementRules.weaponStyle("abyssal_whip"));
        assertEquals("magic", CombatAchievementRules.weaponStyle("trident_of_the_seas"));
        assertEquals("", CombatAchievementRules.weaponStyle(""));
        assertEquals("", CombatAchievementRules.weaponStyle("bones"));
        // Facing Jad Head-on: only melee weapons held for the whole run.
        Metrics fc = new Metrics();
        fc.wield("", 0);
        fc.wield("abyssal_whip", 5);
        assertTrue(meets(153, fc));
        fc.wield("twisted_bow", 9);
        assertFalse(meets(153, fc));
        // Facing Jad Head-on II: no ranged or magic weapon before TzKal-Zuk is fought.
        Metrics zuk = new Metrics();
        zuk.wield("abyssal_whip", 10);
        zuk.engagedAt.put("tzkal_zuk", 500L);
        zuk.wield("twisted_bow", 600);
        assertTrue(meets(350, zuk));
        Metrics early = new Metrics();
        early.wield("twisted_bow", 10);
        early.engagedAt.put("tzkal_zuk", 500L);
        assertFalse(meets(350, early));
        Metrics never = new Metrics();
        never.wield("toxic_blowpipe", 10);
        assertFalse(meets(350, never), "held a ranged weapon and never reached Zuk");
        // Budget Setup and Back in My Day...: never held at all.
        Metrics budget = new Metrics();
        budget.wield("toxic_blowpipe", 1);
        assertTrue(meets(348, budget));
        budget.wield("twisted_bow", 2);
        assertFalse(meets(348, budget));
        Metrics tob = new Metrics();
        tob.raid = raid("TOB", "NORMAL", 3, 0);
        tob.wield("abyssal_tentacle", 1);
        assertTrue(meets(253, tob));
        tob.wield("holy_scythe_of_vitur", 2);
        assertFalse(meets(253, tob));
        // Morytania Only: Barrows weapons (and the Dawnbringer) only, and never in Entry mode.
        Metrics bar = new Metrics();
        bar.raid = raid("TOB", "HARD", 3, 0);
        bar.wield("dharoks_greataxe", 1);
        bar.wield("dawnbringer", 2);
        assertTrue(meets(252, bar));
        bar.raid = raid("TOB", "ENTRY", 3, 0);
        assertFalse(meets(252, bar));
        bar.raid = raid("TOB", "NORMAL", 3, 0);
        bar.wield("abyssal_whip", 3);
        assertFalse(meets(252, bar));
    }

    @Test
    void untrackedTasksNowTracked() {
        // A Slow Death: poison or venom dealt the last damage.
        Metrics bryo = new Metrics();
        assertFalse(meets(40, bryo));
        bryo.flags.add(CombatAchievements.FLAG_TOXIN_LAST);
        assertTrue(meets(40, bryo));
        // Sit Back and Relax: 100 thrall damage.
        assertEquals(Track.COUNTER, task(399).track());
        assertEquals(CombatAchievements.COUNTER_THRALL_DAMAGE, task(399).target());
        assertEquals(100, task(399).count());
        // Into the Den of Giants: a hill, a moss and a fire giant inside the Giants' Den.
        Task den = task(398);
        assertEquals(Track.COLLECT, den.track());
        int bits = CombatAchievements.collectBit(den, "Hill giant") | CombatAchievements.collectBit(den, "Moss giant");
        assertEquals(0, CombatAchievements.collectBit(den, "Ice giant"));
        assertFalse(CombatAchievements.collectedAll(den, bits));
        assertTrue(CombatAchievements.collectedAll(den, bits | CombatAchievements.collectBit(den, "Fire giant")));
        Metrics where = new Metrics();
        assertFalse(CombatAchievementRules.meets(den.rule(), where));
        where.at = place -> place.equals("dng_giants_den");
        assertTrue(CombatAchievementRules.meets(den.rule(), where));
        // Trying to fit in: Vampyrium weapons (or spells by hand) only.
        Metrics mk = new Metrics();
        mk.weapons.add("crimson_kisten");
        mk.weapons.add("");
        assertTrue(meets(641, mk));
        mk.weapons.add("dragon_scimitar");
        assertFalse(meets(641, mk));
        assertEquals(6, CombatAchievements.tasks().stream().filter(t -> t.track() == Track.UNTRACKED).count());
    }

    @Test
    void speedTasksWithoutASlayerTaskAndInclusiveTimes() {
        // Vardorvis Speed-Trialist: under 1:15, not on a slayer task.
        Metrics v = new Metrics();
        v.ticks = 124;
        assertTrue(meets(485, v));
        v.onTask = true;
        assertFalse(meets(485, v));
        v.onTask = false;
        v.ticks = 125;
        assertFalse(meets(485, v));
        for (int id : new int[]{485, 486, 487, 494, 495, 496, 503, 504, 505, 512, 513, 514}) {
            assertEquals(Track.RULE, task(id).track(), task(id).name());
            assertTrue(CombatAchievementRules.hasFlag(task(id).rule(), "notask"), task(id).name());
        }
        // Grotesque Guardians and Hespori: exactly the time counts (wiki task pages).
        Metrics gg = new Metrics();
        gg.ticks = 200;
        assertTrue(meets(92, gg));
        gg.ticks = 201;
        assertFalse(meets(92, gg));
        Metrics h = new Metrics();
        h.ticks = 80;
        assertTrue(meets(127, h));
    }

    @Test
    void raidModesOnNormalTasks() {
        // Putting It Olm on the Line: no Challenge Mode; Can't Drain This: not Entry mode.
        Metrics cox = new Metrics();
        cox.raid = new Raid("COX", "CHALLENGE", 1, 0, Set.of(), 0, 0, 0, false, 45_000, false);
        assertFalse(meets(305, cox));
        cox.raid = new Raid("COX", "NORMAL", 1, 0, Set.of(), 0, 0, 0, false, 45_000, false);
        assertTrue(meets(305, cox));
        Metrics maiden = new Metrics();
        maiden.raid = raid("TOB", "ENTRY", 2, 0);
        assertFalse(meets(250, maiden));
        maiden.raid = raid("TOB", "HARD", 2, 0);
        assertTrue(meets(250, maiden));
        assertTrue(CombatAchievementRules.meets("mode=normal/hard", maiden));
        assertFalse(CombatAchievementRules.meets("mode=entry/challenge", maiden));
    }

    @Test
    void rewardHelpers() {
        assertFalse(CombatAchievements.braceletRecharges(Tier.HARD, "bracelet_of_slaughter", 0.0));
        assertTrue(CombatAchievements.braceletRecharges(Tier.ELITE, "bracelet_of_slaughter", 0.09));
        assertTrue(CombatAchievements.braceletRecharges(Tier.GRANDMASTER, "expeditious_bracelet", 0.0));
        assertFalse(CombatAchievements.braceletRecharges(Tier.ELITE, "expeditious_bracelet", 0.10));
        assertFalse(CombatAchievements.braceletRecharges(Tier.ELITE, "ring_of_recoil", 0.0));
        assertEquals(-1, CombatAchievements.instanceFee("general_graardor", Tier.MEDIUM));
        assertEquals(150_000, CombatAchievements.instanceFee("general_graardor", Tier.HARD));
        assertEquals(125_000, CombatAchievements.instanceFee("kreearra", Tier.ELITE));
        assertEquals(100_000, CombatAchievements.instanceFee("kril_tsutsaroth", Tier.MASTER));
        assertEquals(75_000, CombatAchievements.instanceFee("commander_zilyana", Tier.GRANDMASTER));
        assertEquals(50_000, CombatAchievements.instanceFee("the_hueycoatl", null));
        assertEquals(25_000, CombatAchievements.instanceFee("the_hueycoatl", Tier.HARD));
        assertEquals(12_500, CombatAchievements.instanceFee("giant_mole", Tier.ELITE));
        assertTrue(CombatAchievements.rewards(Tier.ELITE).stream().anyMatch(r -> r.contains("TzTok slayer helmet")));
        assertTrue(CombatAchievements.rewards(Tier.MASTER).stream().anyMatch(r -> r.contains("vampyric slayer helmet")));
        assertTrue(CombatAchievements.rewards(Tier.GRANDMASTER).stream().anyMatch(r -> r.contains("TzKal slayer helmet")));
        assertTrue(CombatAchievements.rewards(Tier.ELITE).stream().anyMatch(r -> r.contains("Bracelets of slaughter")));
        // The bracelets exist with charges to save.
        ContentRegistry.bosses();
        assertNotNull(ItemCatalog.get("bracelet_of_slaughter"));
        assertNotNull(ItemCatalog.get("expeditious_bracelet"));
        assertTrue(Jewellery.chargePool("bracelet_of_slaughter") > 0);
    }
}
