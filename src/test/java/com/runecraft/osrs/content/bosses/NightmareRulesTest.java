package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.bosses.NightmareRules.PhosaniSet;
import com.runecraft.osrs.content.bosses.NightmareRules.Protection;
import com.runecraft.osrs.content.bosses.NightmareRules.Special;
import com.runecraft.osrs.data.Drop;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Nightmare's and Phosani's fight rules against the OSRS Wiki (July 2026). */
class NightmareRulesTest {
    @Test
    void shieldsAndTotemsScaleFromFivePlayers() {
        // Wiki: shield 2,000 a phase (+200 per player in a six-or-more fight, up to 19,600); totems 300 (+30, up to 2,640).
        assertEquals(2000, NightmareRules.shield(false, 1));
        assertEquals(2000, NightmareRules.shield(false, 5));
        assertEquals(2200, NightmareRules.shield(false, 6));
        assertEquals(17000, NightmareRules.shield(false, 80));
        assertEquals(19600, NightmareRules.shield(false, 200));
        assertEquals(300, NightmareRules.totemCharge(false, 3));
        assertEquals(330, NightmareRules.totemCharge(false, 6));
        assertEquals(2550, NightmareRules.totemCharge(false, 80));
        assertEquals(2640, NightmareRules.totemCharge(false, 200));
        // Phosani: 400 shields, 200 totems; 1,350 physical damage and 2,400 charge (1,200 with magic).
        assertEquals(400, NightmareRules.shield(true, 1));
        assertEquals(200, NightmareRules.totemCharge(true, 1));
        assertEquals(1350, NightmareRules.totalWork(true, 1));
        assertEquals(2400, NightmareRules.SHIELD_PHASES * NightmareRules.TOTEMS * NightmareRules.totemCharge(true, 1));
        assertEquals(20, NightmareRules.totemCharge(10, true));
        assertEquals(10, NightmareRules.totemCharge(10, false));
        // The original fight's 7,800 damage: three shields of 2,000 and 600 of magic charge a phase.
        assertEquals(7800, NightmareRules.totalWork(false, 1)
                + NightmareRules.SHIELD_PHASES * NightmareRules.TOTEMS * NightmareRules.totemCharge(false, 1) / 2);
        // Three blasts of 800 are her 2,400 health.
        assertEquals(2400, NightmareRules.SHIELD_PHASES * NightmareRules.TOTEM_BLAST);
        assertEquals(2400, ContentRegistry.boss("the_nightmare").hitpoints);
    }

    @Test
    void workLeftCountsDownThroughThePhases() {
        int total = NightmareRules.totalWork(false, 1);
        assertEquals(total, NightmareRules.workLeft(false, 1, 0, 2000, false, 0));
        assertEquals(4000, NightmareRules.workLeft(false, 1, 0, 0, false, 0)); // first shield broken
        assertEquals(2000, NightmareRules.workLeft(false, 1, 2, 2000, false, 0));
        assertEquals(0, NightmareRules.workLeft(false, 1, 2, 0, false, 0));
        assertEquals(150, NightmareRules.workLeft(true, 1, 2, 0, false, 0)); // Phosani's desperation health still to come
        assertEquals(40, NightmareRules.workLeft(true, 1, 3, 0, true, 40));
    }

    @Test
    void specialsByPhaseAndPhosanisSets() {
        assertEquals(List.of(Special.GRASPING_CLAWS, Special.HUSKS, Special.CORPSE_FLOWERS), NightmareRules.nightmareSpecials(0));
        assertEquals(List.of(Special.GRASPING_CLAWS, Special.CURSE, Special.PARASITES), NightmareRules.nightmareSpecials(1));
        assertEquals(List.of(Special.GRASPING_CLAWS, Special.SPORES, Special.SURGE), NightmareRules.nightmareSpecials(2));
        assertEquals(List.of(Special.SPORES, Special.HUSKS, Special.CURSE), PhosaniSet.A1.specials());
        assertEquals(List.of(Special.CORPSE_FLOWERS, Special.PARASITES, Special.SURGE), PhosaniSet.B2.specials());
        Set<PhosaniSet> seen = EnumSet.noneOf(PhosaniSet.class);
        Random r = new Random(4);
        for (int i = 0; i < 200; i++) {
            List<PhosaniSet> sets = NightmareRules.phosaniSets(r);
            assertEquals(3, sets.size());
            for (int p = 1; p < sets.size(); p++) {
                // A and B alternate, so neither the spawn nor the claw companion repeats in consecutive phases.
                assertNotEquals(sets.get(p).a(), sets.get(p - 1).a(), sets.toString());
                assertNotEquals(sets.get(p).specials().get(0), sets.get(p - 1).specials().get(0));
                assertNotEquals(sets.get(p).specials().get(1), sets.get(p - 1).specials().get(1));
            }
            seen.addAll(sets);
        }
        assertEquals(EnumSet.allOf(PhosaniSet.class), seen);
        // A round uses every special once.
        List<Special> round = NightmareRules.specialRound(NightmareRules.nightmareSpecials(1), new Random(1));
        assertEquals(new HashSet<>(NightmareRules.nightmareSpecials(1)), new HashSet<>(round));
    }

    @Test
    void standardAttacksAndPrayers() {
        // The Nightmare: 20% through the right prayer, 120% through a wrong one; Phosani: blocked by the right one.
        assertEquals(10, NightmareRules.standardDamage(false, 50, Protection.CORRECT));
        assertEquals(60, NightmareRules.standardDamage(false, 50, Protection.WRONG));
        assertEquals(50, NightmareRules.standardDamage(false, 50, Protection.NONE));
        assertEquals(0, NightmareRules.standardDamage(true, 73, Protection.CORRECT));
        assertEquals(73, NightmareRules.standardDamage(true, 73, Protection.WRONG));
        assertEquals(3, NightmareRules.RANGED_WIND_UP);
        assertEquals(2, NightmareRules.MELEE_WIND_UP);
        assertEquals(2, NightmareRules.meleeThirds(true, 2)); // Phosani's third phase: much more melee
        assertEquals(1, NightmareRules.meleeThirds(true, 1));
        assertEquals(1, NightmareRules.meleeThirds(false, 2));
        // Her attacks' max hits (the BossDef carries them).
        BossDef n = ContentRegistry.boss("the_nightmare");
        BossDef p = ContentRegistry.boss("phosanis_nightmare");
        assertEquals(50, attack(n, "grasping_claws").maxHit());
        assertEquals(60, attack(n, "surge").maxHit());
        assertEquals(65, attack(p, "grasping_claws").maxHit());
        assertEquals(85, attack(p, "surge").maxHit());
        assertEquals(3, attack(n, "dream_blast").telegraphTicks());
        assertEquals(2, attack(p, "claw_swipe").telegraphTicks());
        Random r = new Random(2);
        for (int i = 0; i < 500; i++) {
            int c = NightmareRules.clawDamage(true, r);
            assertTrue(c >= 50 && c <= 65, "Phosani's claws " + c);
            assertTrue(NightmareRules.clawDamage(false, r) <= 50);
        }
    }

    @Test
    void curseShufflesTheProtectionPrayers() {
        assertEquals(Prayer.PROTECT_FROM_MAGIC, NightmareRules.cursed(Prayer.PROTECT_FROM_MELEE));
        assertEquals(Prayer.PROTECT_FROM_MISSILES, NightmareRules.cursed(Prayer.PROTECT_FROM_MAGIC));
        assertEquals(Prayer.PROTECT_FROM_MELEE, NightmareRules.cursed(Prayer.PROTECT_FROM_MISSILES));
        assertEquals(Prayer.PIETY, NightmareRules.cursed(Prayer.PIETY));
        assertEquals(5, NightmareRules.CURSE_ATTACKS);
    }

    @Test
    void corpseFlowerQuadrants() {
        assertEquals(0, NightmareRules.quadrant(3, -3)); // north-east (north is -z)
        assertEquals(1, NightmareRules.quadrant(-3, -3));
        assertEquals(2, NightmareRules.quadrant(-3, 3));
        assertEquals(3, NightmareRules.quadrant(3, 3));
        for (int safe = 0; safe < 4; safe++) {
            assertEquals(0, NightmareRules.berrySides(safe, safe));
            assertEquals(1, NightmareRules.berrySides(safe, (safe + 1) % 4));
            assertEquals(1, NightmareRules.berrySides(safe, (safe + 3) % 4));
            assertEquals(2, NightmareRules.berrySides(safe, (safe + 2) % 4)); // berries on both sides: twice the damage
        }
        assertEquals(5, NightmareRules.FLOWER_BLOOM_TICKS);
        assertEquals(20, NightmareRules.FLOWER_MAX);
        assertEquals(2, NightmareRules.FLOWER_HEAL_MULTIPLIER);
    }

    @Test
    void sleepwalkersAndTheirBlast() {
        // The Nightmare: at the start of phases 2 and 3 (none after her third blast, which kills her), 1 for a solo fight.
        assertTrue(NightmareRules.sleepwalkersAfter(false, 1));
        assertTrue(NightmareRules.sleepwalkersAfter(false, 2));
        assertFalse(NightmareRules.sleepwalkersAfter(false, 3));
        assertEquals(1, NightmareRules.sleepwalkers(false, 1, 1));
        assertEquals(24, NightmareRules.sleepwalkers(false, 1, 80));
        // A solo player who lets the sleepwalker through dies to the blast (149).
        assertEquals(149, BossMechanics.sleepwalkerBlast(1, 1));
        // Phosani: after each of her three phases, 2, 3 then 4.
        assertTrue(NightmareRules.sleepwalkersAfter(true, 3));
        assertEquals(List.of(2, 3, 4), List.of(NightmareRules.sleepwalkers(true, 1, 1), NightmareRules.sleepwalkers(true, 2, 1),
                NightmareRules.sleepwalkers(true, 3, 1)));
    }

    @Test
    void parasitesAndHusksHaveTheirOwnNumbersInPhosanisFight() {
        Random r = new Random(3);
        // The Nightmare's: 40 hp weakened, 80 full; heals 31-50; attacks up to 10 / 12.
        assertEquals(40, NightmareRules.parasiteHitpoints(false, true, 7));
        assertEquals(80, NightmareRules.parasiteHitpoints(false, false, 40));
        assertEquals(10, NightmareRules.parasiteMaxHit(false, true));
        assertEquals(12, NightmareRules.parasiteMaxHit(false, false));
        // Phosani's: 55 hp (plus the burst's damage when not weakened), bursts 5-10 / ~90, heals ~51-100, attacks 18 / 22.
        assertEquals(55, NightmareRules.parasiteHitpoints(true, true, 8));
        assertEquals(145, NightmareRules.parasiteHitpoints(true, false, 90));
        assertEquals(18, NightmareRules.parasiteMaxHit(true, true));
        assertEquals(22, NightmareRules.parasiteMaxHit(true, false));
        for (int i = 0; i < 500; i++) {
            int weak = NightmareRules.parasiteBurst(true, true, r);
            int full = NightmareRules.parasiteBurst(true, false, r);
            assertTrue(weak >= 5 && weak <= 10, "weakened burst " + weak);
            assertTrue(full >= 80 && full <= 100, "full burst " + full);
            int heal = NightmareRules.parasiteHeal(true, r);
            assertTrue(heal >= 51 && heal <= 100, "Phosani's parasite heal " + heal);
            int nheal = NightmareRules.parasiteHeal(false, r);
            assertTrue(nheal >= 31 && nheal <= 50, "parasite heal " + nheal);
        }
        assertEquals(1, NightmareRules.parasites(1));
        assertEquals(2, NightmareRules.parasites(10));
        // Husks: 20 hitpoints; 14 (Phosani 26, accurate).
        assertEquals(14, NightmareRules.huskMaxHit(false));
        assertEquals(26, NightmareRules.huskMaxHit(true));
        assertEquals(168, NightmareRules.huskAttackBonus(true));
    }

    @Test
    void uniquesPetAndTablet() {
        // Second unique roll: (party - 5)%, 0-75%.
        assertEquals(0.0, NightmareRules.secondUniqueRoll(1));
        assertEquals(0.05, NightmareRules.secondUniqueRoll(10), 1e-9);
        assertEquals(0.75, NightmareRules.secondUniqueRoll(80), 1e-9);
        assertEquals(0.75, NightmareRules.secondUniqueRoll(200), 1e-9);
        // Pet: 1/800 solo, 1/1,600 duo ... 1/4,000 from five; Phosani 1/1,400.
        assertEquals(800, NightmareRules.petRate(false, 1));
        assertEquals(1600, NightmareRules.petRate(false, 2));
        assertEquals(4000, NightmareRules.petRate(false, 5));
        assertEquals(4000, NightmareRules.petRate(false, 40));
        assertEquals(1400, NightmareRules.petRate(true, 1));
        assertEquals(4000, com.runecraft.osrs.content.extras.Pets.engineRate("the_nightmare"));
        // The two unique tables add up to the wiki's 1/83.33 and 1/320.
        BossDef n = ContentRegistry.boss("the_nightmare");
        double inquisitor = 0;
        double orbs = 0;
        for (Drop d : n.drops) {
            if (NightmareRules.INQUISITOR_TABLE.contains(d.item())) {
                inquisitor += 1.0 / d.rarity();
            } else if (NightmareRules.ORB_TABLE.contains(d.item())) {
                orbs += 1.0 / d.rarity();
            }
        }
        assertEquals(1 / 83.33, inquisitor, 0.0005);
        assertEquals(1 / 320.0, orbs, 1e-6);
        assertEquals(NightmareRules.CLUE_MVP, rate(n, "clue_scroll_elite"));
        assertTrue(n.drops.stream().anyMatch(d -> d.item().equals("bones") && d.rarity() == 1), "bones for the unique-eligible");
        // Phosani's Slepey tablet: 1/25 tertiary, always by the 25th kill.
        BossDef p = ContentRegistry.boss("phosanis_nightmare");
        assertEquals(25, rate(p, "slepey_tablet"));
        assertTrue(BossLoot.tertiary("phosanis_nightmare", p.drops.stream().filter(d -> d.item().equals("slepey_tablet")).findFirst().orElseThrow()));
        assertTrue(NightmareRules.slepeyTabletGuaranteed(25, false));
        assertFalse(NightmareRules.slepeyTabletGuaranteed(24, false));
        assertFalse(NightmareRules.slepeyTabletGuaranteed(30, true));
        assertEquals("slepey_tablet", com.runecraft.osrs.content.systems.Teleports.unlockItem(
                com.runecraft.osrs.content.systems.Teleports.DRAKAN.get(2)));
        assertTrue(NightmareRules.eligible(1, 1));
        assertFalse(NightmareRules.eligible(500, 0));
    }

    private static AttackDef attack(BossDef b, String name) {
        return b.attacks.stream().filter(a -> a.name().equals(name)).findFirst().orElseThrow(() -> new AssertionError(b.id + " " + name));
    }

    private static int rate(BossDef b, String item) {
        return b.drops.stream().filter(d -> d.item().equals(item)).findFirst().orElseThrow(() -> new AssertionError(item)).rarity();
    }
}
