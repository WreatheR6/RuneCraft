package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round 44's quest boss polish against the wiki: the Balance Elemental's draining special, the Stone of Jas fight, Vanstrom
 * Klause's specials and enraged lightning, and A Night at the Theatre's Hespori island.
 */
class QuestBossPolishTest {
    private static AttackDef attack(String boss, String name) {
        BossDef b = ContentRegistry.boss(boss);
        assertNotNull(b, boss);
        return b.attacks.stream().filter(a -> a.name().equals(name)).findFirst().orElseThrow(() -> new AssertionError(boss + " " + name));
    }

    @Test
    void balanceElementalOpensEachFormWithItsDrain() {
        for (int form = 0; form < 3; form++) {
            AttackDef drain = attack("balance_elemental", BalanceElemental.FORM_DRAINS[form]);
            AttackDef normal = attack("balance_elemental", BalanceElemental.FORM_ATTACKS[form]);
            // wiki infobox: 40 standard, 89 stat-draining, in the form's own style
            assertEquals(BalanceElemental.DRAIN_MAX, drain.maxHit());
            assertEquals(89, drain.maxHit());
            assertEquals(BalanceElemental.STANDARD_MAX, normal.maxHit());
            assertEquals(BalanceElemental.family(normal.style()), BalanceElemental.family(drain.style()));
            assertEquals(form, BalanceElemental.family(drain.style()));
            assertEquals(0, drain.weight(), "the script opens a form with it; never picked at random");
            assertTrue(drain.telegraphTicks() >= 2, "a visible wind-up to switch prayers (wiki: about 2 ticks)");
            assertTrue(BalanceElemental.isDrain(drain.name()));
            assertFalse(BalanceElemental.isDrain(normal.name()));
        }
        // a heavy drain: a fifth plus two of every combat stat, and a third of Prayer (at least 10)
        assertEquals(21, BalanceElemental.statDrain(99));
        assertEquals(0, BalanceElemental.statDrain(0));
        assertEquals(33, BalanceElemental.prayerDrain(99));
        assertEquals(10, BalanceElemental.prayerDrain(20));
        assertEquals(5, BalanceElemental.prayerDrain(5));
    }

    @Test
    void theStoneOfJasRaisesStatsTo255() {
        assertEquals(156, QuestBossRules.stoneOfJasBoost(99));
        assertEquals(255 - 70, QuestBossRules.stoneOfJasBoost(70));
        BossDef td = ContentRegistry.boss("wgs_tormented_demon");
        assertEquals(600, td.hitpoints);
        assertEquals(31, td.attacks.stream().mapToInt(AttackDef::maxHit).max().orElse(0));
        assertEquals(45, QuestBossRules.WGS_DEMON_BOMB_MAX); // DPS calculator: 45 special
        assertTrue(QuestBossRules.WGS_DEMON_BOMB_TICKS > QuestBossRules.WGS_DEMON_BIND_TICKS, "the bind ends before the fire lands");
    }

    @Test
    void vanstromsSpecialsFollowASetOrder() {
        Set<String> firstThree = new HashSet<>();
        for (int first = 0; first < 3; first++) {
            Set<String> seen = new HashSet<>();
            for (int n = 0; n < 3; n++) {
                seen.add(QuestBossRules.vanstromSpecial(first, n));
            }
            assertEquals(3, seen.size(), "none comes back until all three are used");
            // the order repeats
            assertEquals(QuestBossRules.vanstromSpecial(first, 0), QuestBossRules.vanstromSpecial(first, 3));
            firstThree.add(QuestBossRules.vanstromSpecial(first, 0));
        }
        assertEquals(Set.copyOf(QuestBossRules.VANSTROM_SPECIALS), firstThree, "any special can come first");
        // the special attacks only name them: the script makes them
        for (String s : QuestBossRules.VANSTROM_SPECIALS) {
            assertEquals(0, attack("vanstrom_klause", s).weight(), s);
        }
        // the blood blast pierces so the script can halve it under Protect from Magic (wiki: 50%)
        assertEquals(AttackDef.Special.PRAYER_PIERCING, attack("vanstrom_klause", "blood_blast").special());
        assertEquals(24, attack("vanstrom_klause", "blood_blast").maxHit());
        assertEquals(0.5, QuestBossRules.VANSTROM_BLAST_PRAYED_SHARE);
        // the acidic bloodveld (DPS calculator): level 56, 5 hitpoints, explodes for 35-50
        MonsterDef pet = ContentRegistry.monster("sotf_acidic_bloodveld");
        assertNotNull(pet);
        assertEquals(56, pet.combatLevel);
        assertEquals(5, pet.hitpoints);
        assertEquals(50, QuestBossRules.VANSTROM_PET_MAX);
        assertEquals(35, QuestBossRules.VANSTROM_PET_MIN);
        assertEquals("sotf_acidic_bloodveld", ContentRegistry.boss("vanstrom_klause").minion);
        // enraged lightning (wiki): 10-15 shadows every couple of seconds, up to 40
        assertEquals(10, QuestBossRules.vanstromBolts(0));
        assertEquals(15, QuestBossRules.vanstromBolts(5));
        assertEquals(15, QuestBossRules.vanstromBolts(9));
        assertEquals(40, QuestBossRules.VANSTROM_LIGHTNING_MAX);
        assertTrue(QuestBossRules.VANSTROM_LIGHTNING_EVERY_TICKS * 0.6 <= 2.5);
    }

    @Test
    void theWildHesporiStandsOnItsIsland() {
        // wiki: its four flowers at 3504/3512 x 3353/3361
        var spot = ContentRegistry.place("ant_hespori_island");
        assertNotNull(spot);
        assertEquals(List.of(3508, 3357), List.of(spot.osrsX(), spot.osrsY()));
        assertEquals(AttackType.RANGED, attack("ant_hespori", "spores").style());
    }

    /**
     * The cave steps happen in their caves: While Guthix Sleeps' light creatures, skull mechanisms, temple and statues down in the
     * Lumbridge Swamp Caves past the Tears of Guthix (Movario waiting by the pool), Dragon Slayer II's tomb down in the Shayzien
     * Crypts. Surok attacks like Vorkath: six regular attacks, a special every seventh.
     */
    @Test
    void caveStepsHappenUnderground() {
        QuestSpec wgs = QuestBook.get().spec("while_guthix_sleeps");
        Set<String> under = new HashSet<>();
        for (QuestSpec.Trigger t : wgs.triggers()) {
            if (t.target.equals("wgs_light_creature") || t.target.equals("wgs_temple")) {
                assertTrue(t.underground, t.kind + " at " + t.target);
                under.add(t.kind + ":" + (t.puzzle == null ? "" : t.puzzle.key));
            }
            assertFalse(t.kind == QuestSpec.Trigger.Kind.PUZZLE && t.target.equals("guthix_sanctum"), "a puzzle left on the surface");
        }
        assertTrue(under.containsAll(List.of("REACH:", "PUZZLE:" + GrandmasterQuests.WGS_SKULLS.key,
                "PUZZLE:" + GrandmasterQuests.WGS_STATUES.key)), under.toString());
        var movario = ContentRegistry.npc("movario");
        assertEquals("at_movario", movario.place());
        assertEquals(23, com.runecraft.osrs.world.NpcStoreys.undergroundFeetY("movario"));
        assertTrue(QuestNpcMoves.RELOCATED.contains("movario"));
        QuestSpec ds2 = QuestBook.get().spec("dragon_slayer_ii");
        assertTrue(ds2.triggers().stream().anyMatch(t -> t.kind == QuestSpec.Trigger.Kind.PUZZLE && t.target.equals("ds2_kourend_tomb")
                && t.underground && t.puzzle == GrandmasterQuests.DS2_CRYPT));
        var tomb = ContentRegistry.place("ds2_kourend_tomb");
        assertEquals(List.of(1504, 9939 - 6405), List.of(tomb.osrsX(), tomb.osrsY())); // quest-helper's tile in the shifted crypts
        assertEquals(7, QuestBossRules.SUROK_SPECIAL_EVERY);
    }
}
