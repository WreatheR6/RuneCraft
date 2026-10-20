package com.runecraft.osrs.content.raids;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.data.Drop;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Chambers of Xeric room rules and drops (round 33) against the OSRS wiki. */
class CoxRulesTest {
    @Test
    void bossDropsMatchTheWiki() throws Exception {
        Map<String, Set<String>> want = new HashMap<>();
        for (String line : Files.readAllLines(Path.of("src/test/resources/osrs/raids/cox_boss_drops.tsv"), StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\t");
            want.computeIfAbsent(f[0], k -> new TreeSet<>()).add(f[1] + " " + f[2] + "-" + f[3] + " 1/" + f[4]);
        }
        List<String> diffs = new ArrayList<>();
        for (Map.Entry<String, Set<String>> e : want.entrySet()) {
            BossDef b = ContentRegistry.boss(e.getKey());
            if (b == null) {
                diffs.add(e.getKey() + ": not registered");
                continue;
            }
            Set<String> have = new TreeSet<>();
            for (Drop d : b.drops) {
                have.add(d.item() + " " + d.min() + "-" + d.max() + " 1/" + d.rarity());
                assertTrue(ItemCatalog.exists(d.item()), e.getKey() + " drops unknown " + d.item());
            }
            if (!have.equals(e.getValue())) {
                diffs.add(e.getKey() + ": " + have + " != " + e.getValue());
            }
        }
        assertTrue(diffs.isEmpty(), String.join("\n", diffs));
        // Rare (1/24+) drops would make collection log pages; the rooms roll those themselves.
        for (BossDef b : CoxBosses.bosses()) {
            for (Drop d : b.drops) {
                assertTrue(d.rarity() < 24, b.id + " " + d.item());
            }
        }
    }

    @Test
    void tektonVasaAndVanguards() {
        Random r = new Random(1);
        for (int i = 0; i < 200; i++) {
            int a = CoxRules.tektonAttacksBeforeAnvil(r);
            assertTrue(a >= 10 && a <= 14, "attacks " + a);
            int s = CoxRules.vanguardShuffleTicks(r);
            assertTrue(s >= 20 && s <= 36, "shuffle " + s);
        }
        assertEquals(5, CoxRules.TEKTON_SPARK_SETS);
        assertEquals(0.4, CoxRules.vanguardResetSpread(4), 1e-9);
        assertEquals(1.0 / 3, CoxRules.vanguardResetSpread(5), 1e-9);
        assertEquals(21, CoxRules.vasaDefenceHeal(205));
        assertTrue(CoxRules.vasaDrainTicks(1) < CoxRules.vasaDrainTicks(0), "each disabled crystal speeds the drain up");
        assertEquals(125, CoxRules.LUX_GRUB_HP);
        assertEquals(30, CoxRules.BLOSSOM_HEAL);
    }

    @Test
    void muttadilesIceDemonThievingTightrope() {
        assertEquals(100, CoxRules.meatTreeHp(1));
        assertEquals(500, CoxRules.meatTreeHp(99));
        assertEquals(0.20, CoxRules.meatTreeAccuracy(1), 1e-9);
        assertEquals(0.78, CoxRules.meatTreeAccuracy(99), 1e-9);
        Random r = new Random(2);
        for (int i = 0; i < 300; i++) {
            int chop = CoxRules.meatTreeChop(98, r);
            assertTrue(chop >= 42 && chop <= 52, "98 Woodcutting chops 42-52, got " + chop);
            int stun = CoxRules.crabStunTicks(1, r);
            assertTrue(stun >= 50 && stun <= 60, "solo stun " + stun);
            assertTrue(CoxRules.crabStunTicks(7, r) <= 20);
            int lvl = CoxRules.tightropeLevel(90, r);
            assertTrue(lvl >= 72 && lvl <= 90, "tightrope level " + lvl);
            int grubs = CoxRules.grubs(80, r);
            assertTrue(grubs >= 1 && grubs <= 3, "grubs " + grubs);
        }
        assertEquals(54, CoxRules.iceDemonKindling(1));
        // Kindling per chop (wiki "Ice demon"): max = Woodcutting / 12, a roll of 0 counts as 1; averages from the wiki table.
        Map<Integer, Double> avg = Map.of(1, 1.0, 24, 1.3333, 36, 1.75, 48, 2.2, 72, 3.1429, 84, 3.625, 96, 4.1111);
        for (Map.Entry<Integer, Double> e : avg.entrySet()) {
            int max = Math.max(1, e.getKey() / 12);
            double exact = 0;
            for (int roll = 0; roll <= e.getKey() / 12; roll++) {
                exact += Math.max(1, roll);
            }
            exact /= e.getKey() / 12 + 1;
            assertEquals(e.getValue(), exact, 1e-3, "wiki average at Woodcutting " + e.getKey());
            Random kr = new Random(e.getKey());
            for (int i = 0; i < 200; i++) {
                int n = CoxRules.kindlingPerChop(e.getKey(), kr);
                assertTrue(n >= 1 && n <= max, "Woodcutting " + e.getKey() + " chopped " + n);
            }
        }
        Random kr = new Random(9);
        Set<Integer> seen = new TreeSet<>();
        for (int i = 0; i < 2000; i++) {
            seen.add(CoxRules.kindlingPerChop(96, kr));
        }
        assertEquals(Set.of(1, 2, 3, 4, 5, 6, 7, 8), seen);
        assertEquals(30, CoxRules.kindlingXp(1), 1e-9);
        assertEquals(81.5, CoxRules.kindlingXp(8), 1e-9);
        assertEquals(0.08, CoxRules.brazierLightChance(1), 1e-9);
        assertEquals(0.78, CoxRules.brazierLightChance(99), 1e-9);
        assertEquals(0.43, CoxRules.brazierLightChance(50), 1e-9);
        assertEquals(5, CoxRules.VESPULA_ENRAGED_STING_MIN);
        assertEquals(20, CoxRules.VESPULA_ENRAGED_STING_MAX);
        assertEquals(33, CoxRules.iceDemonDamage(100, false, false));
        assertEquals(115, CoxRules.iceDemonDamage(100, true, false));
        assertEquals(250, CoxRules.iceDemonDamage(100, false, true));
        assertEquals(1, CoxRules.grubMax(49));
        assertEquals(4, CoxRules.grubMax(100));
        assertEquals(5, CoxRules.batChest(1));
        assertEquals(25, CoxRules.batChest(40));
        assertEquals(12, CoxRules.batChest(12));
    }

    @Test
    void crabOrbsReflect() {
        assertEquals(CoxRules.Colour.BLUE, CoxRules.crabColour(AttackType.MAGIC));
        assertEquals(CoxRules.Colour.GREEN, CoxRules.crabColour(AttackType.RANGED));
        assertEquals(CoxRules.Colour.RED, CoxRules.crabColour(AttackType.CRUSH));
        // A white orb takes the crab's colour; a coloured orb only passes a crab of its own colour.
        assertEquals(CoxRules.Colour.BLUE, CoxRules.reflect(CoxRules.Colour.WHITE, CoxRules.Colour.BLUE));
        assertEquals(CoxRules.Colour.RED, CoxRules.reflect(CoxRules.Colour.RED, CoxRules.Colour.RED));
        assertNull(CoxRules.reflect(CoxRules.Colour.RED, CoxRules.Colour.GREEN));
        assertEquals(CoxRules.Colour.WHITE, CoxRules.reflect(CoxRules.Colour.WHITE, CoxRules.Colour.WHITE));
    }

    @Test
    void greatOlm() {
        assertEquals(27, CoxRules.olmMaxHit(false, false));
        assertEquals(28, CoxRules.olmMaxHit(true, false));
        assertEquals(29, CoxRules.olmMaxHit(true, true));
        assertEquals(100, CoxRules.olmMitigated(CoxRules.OlmPart.HEAD, AttackType.RANGED, 100));
        assertEquals(34, CoxRules.olmMitigated(CoxRules.OlmPart.HEAD, AttackType.MAGIC, 100));
        assertEquals(100, CoxRules.olmMitigated(CoxRules.OlmPart.RIGHT, AttackType.MAGIC, 100));
        assertEquals(34, CoxRules.olmMitigated(CoxRules.OlmPart.RIGHT, AttackType.SLASH, 100));
        assertEquals(100, CoxRules.olmMitigated(CoxRules.OlmPart.LEFT, AttackType.CRUSH, 100));
        assertEquals(34, CoxRules.olmMitigated(CoxRules.OlmPart.LEFT, AttackType.RANGED, 100));
        assertEquals(12, CoxRules.OLM_ROTATION.size());
        assertEquals(CoxRules.OlmStep.CRYSTAL_BURST, CoxRules.OLM_ROTATION.get(3));
        assertEquals(CoxRules.OlmStep.LIGHTNING, CoxRules.OLM_ROTATION.get(7));
        assertEquals(CoxRules.OlmStep.TELEPORT, CoxRules.OLM_ROTATION.get(11));
        assertEquals(75, CoxRules.olmCrystalBombDamage(0));
        assertEquals(15, CoxRules.olmCrystalBombDamage(4));
        assertEquals(0, CoxRules.olmCrystalBombDamage(6));
        assertEquals(0.5, CoxRules.olmPowerChance(true), 1e-9);
        assertEquals(1.0 / 3, CoxRules.olmPowerChance(false), 1e-9);
        assertEquals(50, CoxRules.olmSphereDamage(100));
        // At least four phases (three hand phases and the final stand), one more per 8 players.
        assertEquals(3, RaidScaling.olmPhases(1));
        assertEquals(4, RaidScaling.olmPhases(8));
    }

    @Test
    void foodBurnsAndRakes() {
        CoxSupplies.Food kyren = CoxSupplies.foodByRaw("raw_kyren_fish");
        assertTrue(CoxSupplies.burnChance(90, kyren) > 0.4);
        assertEquals(0.0, CoxSupplies.burnChance(99, CoxSupplies.foodByRaw("raw_pysk_fish")), 1e-9);
        assertEquals(CoxSupplies.BURNT_FISH, CoxSupplies.burnt(kyren));
        assertEquals(CoxSupplies.BURNT_BAT, CoxSupplies.burnt(CoxSupplies.foodByRaw("raw_psykk_bat")));
        assertTrue(RaidItems.RAID_ONLY_ITEMS.contains(CoxSupplies.KEYSTONE));
        assertTrue(RaidItems.RAID_ONLY_ITEMS.contains(CoxSupplies.BLOSSOM));
    }
}
