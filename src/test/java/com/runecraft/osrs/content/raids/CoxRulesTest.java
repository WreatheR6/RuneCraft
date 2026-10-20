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
        // OSRS wiki "Vasa Nistirio": one 40-second (66- or 67-tick) teleport timer shared by the crystals.
        Set<Integer> timers = new TreeSet<>();
        for (int i = 0; i < 100; i++) {
            timers.add(CoxRules.vasaTeleportTimer(r));
        }
        assertEquals(Set.of(66, 67), timers);
        assertEquals(125, CoxRules.LUX_GRUB_HP);
        assertEquals(35, CoxRules.BLOSSOM_HEAL, "blossoms heal 35 since November 2023");
        assertEquals(2, CoxRules.PORTAL_INTERVAL, "the portal drains every two ticks");
        assertEquals(20, CoxRules.VESPULA_POISON);
        assertEquals(15, CoxRules.SOLDIER_POISON);
        // Tekton (wiki infobox): enraged he hits 59 and his melee defences rise from 155/165/105 to 280/290/180.
        assertEquals(59, CoxRules.TEKTON_ENRAGED_MAX_HIT);
        assertEquals(List.of(280, 290, 180), List.of(CoxRules.TEKTON_ENRAGED_DEFENCE[0], CoxRules.TEKTON_ENRAGED_DEFENCE[1],
                CoxRules.TEKTON_ENRAGED_DEFENCE[2]));
        BossDef tekton = ContentRegistry.boss("tekton");
        assertEquals(CoxRules.TEKTON_DEFENCE[0], tekton.defenceBonus(AttackType.STAB));
        assertEquals(CoxRules.TEKTON_DEFENCE[1], tekton.defenceBonus(AttackType.SLASH));
        assertEquals(CoxRules.TEKTON_DEFENCE[2], tekton.defenceBonus(AttackType.CRUSH));
        // The onyx: 1/450 solo, better for bigger parties, three times worse in Challenge Mode for three or fewer.
        assertEquals(450, CoxRules.tektonOnyxRate(1, false));
        assertEquals(1350, CoxRules.tektonOnyxRate(1, true));
        assertEquals(CoxRules.tektonOnyxRate(4, false), CoxRules.tektonOnyxRate(4, true));
        assertTrue(CoxRules.tektonOnyxRate(5, false) < CoxRules.tektonOnyxRate(1, false));
    }

    @Test
    void maxHitsMatchTheReference() throws Exception {
        List<String> diffs = new ArrayList<>();
        int rows = 0;
        for (String line : Files.readAllLines(Path.of("src/test/resources/osrs/raids/cox_max_hits.tsv"), StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            rows++;
            String[] f = line.split("\t");
            com.runecraft.osrs.content.CombatProfile<?> p = ContentRegistry.boss(f[0]);
            if (p == null) {
                p = ContentRegistry.monster(f[0]);
            }
            if (p == null) {
                diffs.add(f[0] + ": not registered");
                continue;
            }
            com.runecraft.osrs.content.AttackDef found = null;
            for (com.runecraft.osrs.content.AttackDef a : p.attacks) {
                if (a.name().equals(f[1])) {
                    found = a;
                }
            }
            if (found == null) {
                diffs.add(f[0] + ": no attack " + f[1]);
            } else if (found.maxHit() != Integer.parseInt(f[2])) {
                diffs.add(f[0] + " " + f[1] + ": " + found.maxHit() + " != " + f[2]);
            }
        }
        assertTrue(rows >= 20, "rows " + rows);
        assertEquals(List.of(), diffs);
    }

    @Test
    void muttadilesIceDemonThievingTightrope() {
        // Mod Ash: 5 x the team's total Woodcutting, at least 100 (35 + 73 + 99 -> 1,035).
        assertEquals(100, CoxRules.meatTreeHp(1));
        assertEquals(495, CoxRules.meatTreeHp(99));
        assertEquals(1035, CoxRules.meatTreeHp(35 + 73 + 99));
        assertEquals(750, CoxRules.meatTreeHp(150));
        // The skilling roll 50/200: 51/256 at level 1, 201/256 at 99.
        assertEquals(51 / 256.0, CoxRules.meatTreeAccuracy(1), 1e-9);
        assertEquals(201 / 256.0, CoxRules.meatTreeAccuracy(99), 1e-9);
        assertEquals(45, CoxRules.MEAT_TREE_XP, 1e-9);
        assertEquals(0.5, CoxRules.MUTTADILE_FEED_HEAL, 1e-9);
        assertEquals(3, CoxRules.MUTTADILE_MAX_FEEDS);
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
        // Grubs (Mod Ash): a roll of 0..Thieving/25 raised to the minimum (2 from 95): at 100, 2 three times in five.
        Map<Integer, Integer> at100 = new HashMap<>();
        Random gr = new Random(5);
        for (int i = 0; i < 5000; i++) {
            at100.merge(CoxRules.grubs(100, gr), 1, Integer::sum);
        }
        assertEquals(Set.of(2, 3, 4), at100.keySet());
        assertTrue(at100.get(2) > 2700 && at100.get(2) < 3300, "grubs at 100 " + at100);
        assertEquals(1, CoxRules.grubs(1, gr));
        // Chests (Mod Ash): 39% to 61% bare-handed, 21 points more with a lockpick.
        assertEquals(0.39, CoxRules.chestOpenChance(1, false), 0.005);
        assertEquals(0.61, CoxRules.chestOpenChance(99, false), 0.005);
        assertEquals(0.21, CoxRules.chestOpenChance(50, true) - CoxRules.chestOpenChance(50, false), 0.005);
        // The trough: 30 grubs solo, 16 per player otherwise; 115 points a grub up to max(4,500, total / 6 x 150).
        assertEquals(30, CoxRules.troughGrubs(1));
        assertEquals(48, CoxRules.troughGrubs(3));
        assertEquals(40, CoxRules.pointGrubs(99));
        assertEquals(97, CoxRules.pointGrubs(447));
        assertEquals(5, CoxRules.THIEVING_STINKHORN);
        assertEquals(4, CoxRules.THIEVING_CICELY);
        assertEquals(54, CoxRules.iceDemonKindling(1));
        // Saplings (Mod Ash): 1/max(3, 2 x party), five times rarer in the ice demon room.
        assertEquals(3, CoxRules.saplingDepletion(1, false));
        assertEquals(15, CoxRules.saplingDepletion(1, true));
        assertEquals(50, CoxRules.saplingDepletion(5, true));
        assertEquals(1, CoxRules.icefiends(1));
        assertEquals(4, CoxRules.icefiends(9));
        for (int i = 0; i < 50; i++) {
            int d = CoxRules.icefiendDelay(r);
            assertTrue(d == 3 || d == 4, "icefiend delay " + d);
        }
        assertEquals(28, CoxRules.KINDLING_CAP);
        assertEquals(48, CoxRules.KINDLING_FIREMAKING_XP, 1e-9);
        assertEquals(36, CoxRules.KINDLING_POINTS);
        assertEquals(32, CoxRules.BRAZIER_LIGHT_POINTS);
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
        assertEquals(3, CoxRules.grubMax(95));
        assertEquals(2, CoxRules.grubMax(50));
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
        // A white orb takes the crab's colour; a coloured orb can't change colour again.
        assertEquals(CoxRules.Colour.BLUE, CoxRules.reflect(CoxRules.Colour.WHITE, CoxRules.Colour.BLUE));
        assertEquals(CoxRules.Colour.RED, CoxRules.reflect(CoxRules.Colour.RED, CoxRules.Colour.RED));
        assertNull(CoxRules.reflect(CoxRules.Colour.RED, CoxRules.Colour.GREEN));
        assertNull(CoxRules.reflect(CoxRules.Colour.RED, CoxRules.Colour.WHITE));
        assertEquals(CoxRules.Colour.WHITE, CoxRules.reflect(CoxRules.Colour.WHITE, CoxRules.Colour.WHITE));
        // The orb always turns right from its own point of view.
        assertEquals(List.of(-1, 0), List.of(CoxRules.turnRight(0, 1)[0], CoxRules.turnRight(0, 1)[1]));
        assertEquals(List.of(0, -1), List.of(CoxRules.turnRight(-1, 0)[0], CoxRules.turnRight(-1, 0)[1]));
        assertEquals(List.of(1, 0), List.of(CoxRules.turnRight(0, -1)[0], CoxRules.turnRight(0, -1)[1]));
        assertEquals(List.of(0, 1), List.of(CoxRules.turnRight(1, 0)[0], CoxRules.turnRight(1, 0)[1]));
        assertEquals(10, CoxRules.orbDamage(20));
        assertEquals(30, CoxRules.orbDamage(120));
        assertEquals(5, CoxRules.orbDamage(5));
    }

    @Test
    void everyCrabLayoutIsSolvableWithThreeCrabs() {
        for (int seed = 0; seed < 200; seed++) {
            CoxRules.CrabLayout l = CoxRules.crabLayout(new Random(seed));
            assertEquals(3, new TreeSet<>(List.of(l.rows()[0], l.rows()[1], l.rows()[2])).size(), "distinct rows");
            // No crabs: the white orb crosses the room into the black crystal.
            assertEquals(3, CoxRules.traceOrb(l, Map.of()));
            for (int i = 0; i < 3; i++) {
                int[] start = l.start();
                int key = start[0] * 1000 + l.rows()[i];
                // A crab in the lane at the crystal's row, struck the crystal's colour, bounces the orb into it.
                assertEquals(i, CoxRules.traceOrb(l, Map.of(key, CoxRules.crystalWants(i))), "seed " + seed + " crystal " + i);
                // Struck another colour, the orb lands on the right crystal with the wrong colour and does nothing.
                CoxRules.Colour wrong = CoxRules.crystalWants((i + 1) % 3);
                assertEquals(-1, CoxRules.traceOrb(l, Map.of(key, wrong)));
                // A second, differently coloured crab in the bounced orb's path swallows it.
                int[] c = l.crystal(i);
                int midX = c[0] / 2;
                assertEquals(-1, CoxRules.traceOrb(l, Map.of(key, CoxRules.crystalWants(i), midX * 1000 + l.rows()[i], wrong)));
            }
        }
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
        // Crystal bombs: 15 at the rim (4 tiles), +15 a tile closer, 60 on top.
        assertEquals(60, CoxRules.olmCrystalBombDamage(0));
        assertEquals(60, CoxRules.olmCrystalBombDamage(1));
        assertEquals(45, CoxRules.olmCrystalBombDamage(2));
        assertEquals(15, CoxRules.olmCrystalBombDamage(4));
        assertEquals(0, CoxRules.olmCrystalBombDamage(6));
        // The swap: nothing on top of each other, 5 adjacent, 10 two tiles apart, at most 50.
        assertEquals(0, CoxRules.olmTeleportDamage(0));
        assertEquals(5, CoxRules.olmTeleportDamage(1));
        assertEquals(10, CoxRules.olmTeleportDamage(2));
        assertEquals(50, CoxRules.olmTeleportDamage(30));
        assertEquals(50, CoxRules.OLM_CLENCH_TICKS, "the clench lasts about 30 seconds");
        assertEquals(38, CoxRules.olmMaxHit(false, false, true));
        assertEquals(39, CoxRules.olmMaxHit(true, false, true));
        assertEquals(41, CoxRules.olmMaxHit(true, true, true));
        assertEquals(27, CoxRules.olmMaxHit(false, false, false));
        assertEquals(0.5, CoxRules.olmPowerChance(true), 1e-9);
        assertEquals(1.0 / 3, CoxRules.olmPowerChance(false), 1e-9);
        assertEquals(50, CoxRules.olmSphereDamage(100));
        // At least four phases (three hand phases and the final stand), one more per 8 players.
        assertEquals(3, RaidScaling.olmPhases(1));
        assertEquals(4, RaidScaling.olmPhases(8));
    }

    @Test
    void roomCombatAchievementsAreTracked() {
        // No Time for Death (304) and Playing with Lasers (315): judged by the rooms, credited when the raid ends.
        for (int id : new int[]{304, 315}) {
            com.runecraft.osrs.content.systems.CombatAchievements.Task t = com.runecraft.osrs.content.systems.CombatAchievements.task("ca_" + id);
            assertTrue(t.track() != com.runecraft.osrs.content.systems.CombatAchievements.Track.UNTRACKED, t.name());
        }
        com.runecraft.osrs.content.systems.CombatAchievementRules.Metrics m = new com.runecraft.osrs.content.systems.CombatAchievementRules.Metrics();
        assertTrue(!com.runecraft.osrs.content.systems.CombatAchievementRules.meets("flag=cox_crabs_perfect", m));
        m.flags.add(com.runecraft.osrs.content.systems.CombatAchievements.FLAG_COX_CRABS_PERFECT);
        assertTrue(com.runecraft.osrs.content.systems.CombatAchievementRules.meets("flag=cox_crabs_perfect", m));
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
