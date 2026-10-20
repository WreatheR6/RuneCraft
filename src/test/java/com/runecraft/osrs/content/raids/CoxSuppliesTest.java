package com.runecraft.osrs.content.raids;

import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.raids.CoxSupplies.Herb;
import com.runecraft.osrs.content.raids.CoxSupplies.Potion;
import com.runecraft.osrs.content.raids.CoxSupplies.Strength;
import com.runecraft.osrs.content.systems.CombatAchievementRules;
import com.runecraft.osrs.content.systems.CombatAchievements;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CoX preparation (herbs, raid herblore, food, scavengers) and the round-29 ToA changes against the OSRS wiki. */
class CoxSuppliesTest {

    @Test
    void herbsMatchTheWiki() {
        assertEquals(27, Herb.GOLPAR.farmingLevel);
        assertEquals(39, Herb.BUCHU.farmingLevel);
        assertEquals(55, Herb.NOXIFER.farmingLevel);
        assertEquals(47, Herb.GOLPAR.cleanLevel);
        assertEquals(52, Herb.BUCHU.cleanLevel);
        assertEquals(60, Herb.NOXIFER.cleanLevel);
        assertEquals(30.0, Herb.NOXIFER.harvestXp);
        assertEquals("buchu_seed", Herb.BUCHU.seed());
        assertEquals("grimy_buchu_leaf", Herb.BUCHU.grimy());
        assertEquals(Herb.NOXIFER, Herb.bySeed("noxifer_seed"));
        // Raking never yields a seed above the Farming level.
        Random r = new Random(3);
        for (int i = 0; i < 200; i++) {
            String seed = CoxSupplies.rakeSeed(30, r);
            assertEquals("golpar_seed", seed);
        }
        assertNull(CoxSupplies.rakeSeed(10, r));
    }

    @Test
    void potionsLevelsXpAndRecipes() {
        assertEquals(Potion.ELDER, CoxSupplies.potionOf(Herb.GOLPAR, CoxSupplies.STINKHORN));
        assertEquals(Potion.TWISTED, CoxSupplies.potionOf(Herb.GOLPAR, CoxSupplies.CICELY));
        assertEquals(Potion.KODAI, CoxSupplies.potionOf(Herb.GOLPAR, CoxSupplies.ENDARKENED));
        assertEquals(Potion.REVITALISATION, CoxSupplies.potionOf(Herb.BUCHU, CoxSupplies.STINKHORN));
        assertEquals(Potion.PRAYER_ENHANCE, CoxSupplies.potionOf(Herb.BUCHU, CoxSupplies.CICELY));
        assertEquals(Potion.XERICS_AID, CoxSupplies.potionOf(Herb.BUCHU, CoxSupplies.ENDARKENED));
        assertEquals(90, Potion.OVERLOAD.level(Strength.STRONG));
        assertEquals(66.5, Potion.OVERLOAD.xp(Strength.STRONG));
        assertEquals(78, Potion.XERICS_AID.level(Strength.STRONG));
        assertEquals(26.5, Potion.PRAYER_ENHANCE.xp(Strength.STRONG));
        assertEquals(59, Potion.KODAI.level(Strength.NORMAL));
        assertEquals(6.5, Potion.TWISTED.xp(Strength.WEAK));
        // The strength made follows the Herblore level.
        assertNull(Potion.ELDER.strengthFor(46));
        assertEquals(Strength.WEAK, Potion.ELDER.strengthFor(47));
        assertEquals(Strength.NORMAL, Potion.ELDER.strengthFor(69));
        assertEquals(Strength.STRONG, Potion.ELDER.strengthFor(70));
        assertEquals(Strength.NORMAL, Potion.OVERLOAD.strengthFor(89));
        assertEquals("Overload (+)", Potion.OVERLOAD.displayName(Strength.STRONG));
        assertEquals("Elder potion", Potion.ELDER.displayName(Strength.NORMAL));
        assertEquals("Xeric's aid", Potion.XERICS_AID.displayName(Strength.NORMAL));
        assertEquals("cox_overload_plus", Potion.OVERLOAD.id(Strength.STRONG));
        assertEquals(21, CoxSupplies.potionIds().size());
        assertEquals(new HashSet<>(CoxSupplies.potionIds()).size(), CoxSupplies.potionIds().size());
    }

    @Test
    void potionEffectsMatchTheWiki() {
        // Elder/twisted/kodai/overload: 4 + 10%, 5 + 13%, 6 + 16%.
        assertEquals(21, CoxSupplies.combatBoost(Strength.STRONG, 99));
        assertEquals(17, CoxSupplies.combatBoost(Strength.NORMAL, 99));
        assertEquals(13, CoxSupplies.combatBoost(Strength.WEAK, 99));
        assertEquals(List.of(Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE), CoxSupplies.boosted(Potion.ELDER));
        assertEquals(List.of(Skill.MAGIC), CoxSupplies.boosted(Potion.KODAI));
        assertEquals(5, CoxSupplies.boosted(Potion.OVERLOAD).size());
        // Xeric's aid (+): hitpoints 15% + 5, Defence 20% + 5, drains 10% + 4; Xeric's aid: 12% + 2, 18% + 2, 9% + 2.
        assertEquals(19, CoxSupplies.xericsAidHeal(Strength.STRONG, 99));
        assertEquals(24, CoxSupplies.xericsAidDefence(Strength.STRONG, 99));
        assertEquals(13, CoxSupplies.xericsAidDrain(Strength.STRONG, 99));
        assertEquals(13, CoxSupplies.xericsAidHeal(Strength.NORMAL, 99));
        assertEquals(7, CoxSupplies.xericsAidHeal(Strength.WEAK, 99));
        assertEquals(10, CoxSupplies.xericsAidDrain(Strength.NORMAL, 99));
        // Revitalisation (+): 11 + 30%.
        assertEquals(40, CoxSupplies.revitalisationRestore(Strength.STRONG, 99));
        // Prayer enhance (+): floor(Prayer / 2 + 31) restorations, every 500 / that ticks (at most 63).
        assertEquals(80, CoxSupplies.prayerEnhanceTimes(Strength.STRONG, 99));
        assertEquals(6, CoxSupplies.prayerEnhanceInterval(Strength.STRONG, 99));
        assertEquals(13, CoxSupplies.prayerEnhanceInterval(Strength.STRONG, 11));
        // Overload: 50 damage in hits of 10, re-applied every 15 s for 5 minutes.
        assertEquals(5, CoxSupplies.OVERLOAD_DAMAGE / CoxSupplies.OVERLOAD_HIT);
        assertEquals(20, CoxSupplies.OVERLOAD_TICKS / CoxSupplies.OVERLOAD_REAPPLY_TICKS);
    }

    @Test
    void fishAndBatsMatchTheWiki() {
        assertEquals(14, CoxSupplies.FOODS.size());
        CoxSupplies.Food brawk = CoxSupplies.foodByRaw("raw_brawk_fish");
        assertEquals(45, brawk.level());
        assertEquals(29.0, brawk.gatherXp());
        assertEquals(19.0, brawk.cookXp());
        assertEquals(14, brawk.heal());
        CoxSupplies.Food kryket = CoxSupplies.foodByRaw("raw_kryket_bat");
        assertEquals(Skill.HUNTER, kryket.skill());
        assertEquals(60, kryket.level());
        assertEquals(21.0, kryket.gatherXp());
        assertEquals(17, kryket.heal());
        assertEquals(23, CoxSupplies.foodByRaw("raw_kyren_fish").heal());
        assertEquals(38.0, CoxSupplies.foodByRaw("raw_kyren_fish").gatherXp());
        assertEquals("raw_mycil_fish", CoxSupplies.catchFor(Skill.FISHING, 74).raw());
        assertEquals("raw_psykk_bat", CoxSupplies.catchFor(Skill.HUNTER, 99).raw());
    }

    @Test
    void scavengerDropsFollowTheWikiTable() {
        Random r = new Random(11);
        int kills = 18_000;
        Map<String, Integer> seen = new HashMap<>();
        for (int i = 0; i < kills; i++) {
            Map<String, Integer> drop = CoxSupplies.scavengerDrop(r);
            assertEquals(1, drop.get("bones"));
            if (drop.containsKey(CoxSupplies.STINKHORN)) {
                assertTrue(drop.containsKey(CoxSupplies.CICELY) && drop.containsKey(CoxSupplies.ENDARKENED), "secondaries drop together");
            }
            for (String k : drop.keySet()) {
                seen.merge(k, 1, Integer::sum);
            }
        }
        // Two rolls of 1/18 for a fishing rod: about 2,000 kills in 18,000; secondaries 6/18 per roll (about 5.6/9).
        assertTrue(Math.abs(seen.get("fishing_rod") - 1945) < 200, "fishing rods " + seen.get("fishing_rod"));
        assertTrue(Math.abs(seen.get(CoxSupplies.CICELY) - 10_000) < 400, "secondaries " + seen.get(CoxSupplies.CICELY));
    }

    @Test
    void raidOnlyItemsAreWiped() {
        for (String id : CoxSupplies.ITEMS) {
            assertTrue(RaidItems.RAID_ONLY_ITEMS.contains(id), id);
        }
        assertTrue(RaidItems.RAID_ONLY_ITEMS.contains(ToaSupplies.NECTAR));
        assertEquals(new HashSet<>(CoxSupplies.ITEMS).size(), CoxSupplies.ITEMS.size());
    }

    @Test
    void coxLayoutHasResourceRooms() {
        for (int seed = 0; seed < 20; seed++) {
            List<RoomDef> cox = RaidLayouts.cox(new Random(seed));
            assertEquals(2, cox.stream().filter(x -> x.kind() == RoomDef.Kind.RESOURCE).count());
            assertTrue(cox.stream().anyMatch(x -> x.id().equals("cox_scavengers")));
            assertTrue(cox.stream().anyMatch(x -> x.id().equals("cox_farming")));
        }
    }

    // ------------------------------------------------------------------ Tombs of Amascut (round 29)

    @Test
    void kephriOverlordsSwarmsAndEggs() {
        Set<Invocation> none = EnumSet.noneOf(Invocation.class);
        Set<Invocation> more = EnumSet.of(Invocation.MORE_OVERLORDS, Invocation.LIVELY_LARVAE);
        assertEquals(List.of("toa_spitting_scarab"), Invocation.kephriOverlords(none, 1));
        assertEquals(List.of("toa_soldier_scarab", "toa_arcane_scarab"), Invocation.kephriOverlords(none, 2));
        assertEquals(List.of("toa_spitting_scarab", "toa_soldier_scarab"), Invocation.kephriOverlords(more, 1));
        assertEquals(List.of("toa_soldier_scarab", "toa_arcane_scarab", "toa_spitting_scarab"), Invocation.kephriOverlords(more, 2));
        assertEquals(28, Invocation.kephriSwarms(1));
        assertEquals(24, Invocation.kephriSwarms(2));
        int total = 0;
        for (int i = 0; i < 28; i++) {
            total += Invocation.kephriSwarmDelay(i);
        }
        assertEquals(4 * 4 + 4 * 3 + 4 * 2 + 16, total);
        Random r = new Random(5);
        assertEquals(2, Invocation.kephriDarkEggs(none, r));
        for (int i = 0; i < 50; i++) {
            int n = Invocation.kephriDarkEggs(more, r);
            assertTrue(n >= 3 && n <= 5, "Lively Larvae dark eggs " + n);
        }
        assertEquals(3, Invocation.arcaneScarabRelocateHits(1));
        assertEquals(5, Invocation.arcaneScarabRelocateHits(3));
    }

    @Test
    void wardenCorePointsAndSupplies() {
        // OSRS wiki "Chest (Tombs of Amascut)": the Warden's core gives no points (the phase 2 body gives 2 a hitpoint).
        assertEquals(0.0, RaidScaling.toaPointsPerDamage("toa_warden_core", false));
        assertTrue(ToaSupplies.givenInsideTomb(ToaSupplies.HONEY_LOCUST));
        assertTrue(ToaSupplies.givenInsideTomb(ToaSupplies.AMBROSIA));
        assertFalse(ToaSupplies.givenInsideTomb("saradomin_brew4"));
        assertFalse(ToaSupplies.givenInsideTomb("shark"));
    }

    @Test
    void youAreNotPreparedIsTracked() {
        CombatAchievements.Task t = CombatAchievements.task("ca_424");
        assertTrue(t.track() != CombatAchievements.Track.UNTRACKED, "CA 424 tracked");
        CombatAchievementRules.Metrics m = new CombatAchievementRules.Metrics();
        m.raid = new CombatAchievementRules.Raid("TOA", "NORMAL", 1, 150, Set.of(), 0, 0, 0, true, 30_000, true);
        assertTrue(CombatAchievementRules.meets(t.rule(), m), "Helpful Spirit supplies are allowed");
        m.flags.add(ToaSupplies.OUTSIDE_SUPPLY_FLAG);
        assertFalse(CombatAchievementRules.meets(t.rule(), m), "outside supplies fail it");
        m.flags.clear();
        m.raid = new CombatAchievementRules.Raid("TOA", "NORMAL", 1, 145, Set.of(), 0, 0, 0, true, 30_000, true);
        assertFalse(CombatAchievementRules.meets(t.rule(), m), "needs raid level 150");
    }
}
