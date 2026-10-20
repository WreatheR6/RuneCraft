package com.runecraft.osrs.content.minigames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Barbarian Assault waves, Penance stats, role mechanics and honour points against the OSRS Wiki (round 37). */
class BaWavesTest {
    private static final List<String> TYPES = List.of("fighter", "ranger", "runner", "healer");

    private static List<String[]> rows() throws IOException {
        List<String[]> rows = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of("src/test/resources/osrs/minigames/ba_waves.tsv"), StandardCharsets.UTF_8)) {
            if (!line.isBlank() && !line.startsWith("#")) {
                rows.add(line.split("\\|"));
            }
        }
        return rows;
    }

    private static int[] stats(BaWaves.Stats s) {
        return new int[]{s.combat(), s.hitpoints(), s.attack(), s.strength(), s.defence(), s.ranged(), s.maxHit()};
    }

    @Test
    void wavesMatchTheWikiSnapshot() throws IOException {
        int compared = 0;
        for (String[] r : rows()) {
            int wave = Integer.parseInt(r[0]);
            int[] wiki = new int[7];
            for (int i = 0; i < 7; i++) {
                wiki[i] = Integer.parseInt(r[4 + i]);
            }
            int[] ours;
            if (r[1].equals("queen")) {
                ours = stats(BaWaves.QUEEN);
            } else if (r[1].equals("queen_spawn")) {
                ours = stats(BaWaves.QUEEN_SPAWN);
            } else {
                int type = TYPES.indexOf(r[1]);
                assertEquals(Integer.parseInt(r[2]), BaWaves.initial(wave, type), "wave " + wave + " " + r[1] + " initial");
                assertEquals(Integer.parseInt(r[3]), BaWaves.reserve(wave, type), "wave " + wave + " " + r[1] + " reserve");
                ours = stats(BaWaves.stats(wave, type));
            }
            assertEquals(java.util.Arrays.toString(wiki), java.util.Arrays.toString(ours), "wave " + wave + " " + r[1] + " stats");
            compared++;
        }
        assertEquals(42, compared);
        // The old MinigameRules API reads the same table.
        assertEquals(List.of(4, 4, 2, 2), java.util.Arrays.stream(MinigameRules.BarbarianAssault.wave(1)).boxed().toList());
        assertEquals(List.of(8, 8, 9, 8), java.util.Arrays.stream(MinigameRules.BarbarianAssault.wave(9)).boxed().toList());
        assertEquals(250, MinigameRules.BarbarianAssault.QUEEN_HP);
    }

    @Test
    void honourMatchesTheWikiRoundTotals() {
        // Strategy guide: a full round is 370 / 357 / 278 / 305 base points, 403 / 392 / 305 / 335 with the hard Kandarin diary.
        assertEquals(370, BaRules.fullRound(BaRules.ATTACKER, false));
        assertEquals(357, BaRules.fullRound(BaRules.DEFENDER, false));
        assertEquals(278, BaRules.fullRound(BaRules.COLLECTOR, false));
        assertEquals(305, BaRules.fullRound(BaRules.HEALER, false));
        for (int role = 0; role < 4; role++) {
            int withDiary = BaRules.fullRound(role, true);
            int base = BaRules.fullRound(role, false);
            // approx: per-wave rounding lands within a few points of the wiki's figures
            assertTrue(withDiary >= base * 11 / 10 - 9 && withDiary <= base * 11 / 10, role + ": " + withDiary);
        }
        // Wave 1 clean: attacker (3 + 3) twice + defender 3 + healer 1.
        assertEquals(16, BaRules.wavePoints(BaRules.ATTACKER, 1, BaRules.WaveStats.clean(1), false));
        assertEquals(80, BaRules.wavePoints(BaRules.COLLECTOR, 10, null, false));
        assertEquals(5, BaRules.wave10(BaRules.ATTACKER, BaRules.HEALER, false));
        assertEquals(88, BaRules.wave10(BaRules.HEALER, BaRules.HEALER, true));
    }

    @Test
    void honourAspectsFollowTheWikiFormulas() {
        BaRules.WaveStats clean = BaRules.WaveStats.clean(5);
        // 4.35 eggs give collectors 2 points and everyone else 1.
        BaRules.WaveStats eggs = new BaRules.WaveStats(clean.fightersKilled(), clean.rangersKilled(), clean.runnersKilled(),
                clean.healersKilled(), 9, 0, 0, 0, 0, 0);
        assertEquals(BaRules.wavePoints(BaRules.COLLECTOR, 5, clean, false) + 4, BaRules.wavePoints(BaRules.COLLECTOR, 5, eggs, false));
        assertEquals(BaRules.wavePoints(BaRules.ATTACKER, 5, clean, false) + 2, BaRules.wavePoints(BaRules.ATTACKER, 5, eggs, false));
        // Eggs count up to 60 (13 points).
        assertEquals(13, BaRules.rolePoints(new BaRules.WaveStats(0, 0, 0, 0, 500, 0, 0, 0, 0, 0))[BaRules.COLLECTOR]);
        // 18 hitpoints healed = 1 point, at most 28.
        assertEquals(28, BaRules.rolePoints(new BaRules.WaveStats(0, 0, 0, 0, 0, 0, 10_000, 0, 0, 0))[BaRules.HEALER]);
        // One free wrong hit, then 1 each (at most 10), and the attacker loses them twice.
        int[] pen = BaRules.rolePenalties(new BaRules.WaveStats(0, 0, 0, 0, 0, 0, 0, 4, 1, 9));
        assertEquals(3, pen[BaRules.ATTACKER]);
        assertEquals(3, pen[BaRules.DEFENDER]);
        assertEquals(2, pen[BaRules.HEALER]);
        assertEquals(10, BaRules.rolePenalties(new BaRules.WaveStats(0, 0, 0, 0, 0, 0, 0, 11, 4, 40))[BaRules.ATTACKER]);
        BaRules.WaveStats wrong = new BaRules.WaveStats(clean.fightersKilled(), clean.rangersKilled(), clean.runnersKilled(),
                clean.healersKilled(), 0, 0, 0, 3, 0, 0);
        assertEquals(BaRules.wavePoints(BaRules.ATTACKER, 5, clean, false) - 4, BaRules.wavePoints(BaRules.ATTACKER, 5, wrong, false));
        assertEquals(BaRules.wavePoints(BaRules.HEALER, 5, clean, false) - 2, BaRules.wavePoints(BaRules.HEALER, 5, wrong, false));
    }

    @Test
    void roleMechanics() {
        // Calls: who calls what to whom.
        assertEquals(BaRules.Call.EGG, BaRules.Call.madeBy(BaRules.ATTACKER));
        assertEquals(BaRules.Call.STYLE, BaRules.Call.heardBy(BaRules.ATTACKER));
        assertEquals(BaRules.Call.POISON, BaRules.Call.madeBy(BaRules.DEFENDER));
        assertEquals(BaRules.Call.BAIT, BaRules.Call.heardBy(BaRules.DEFENDER));
        assertEquals("Defensive/Barbed/Fire", BaRules.Call.STYLE.label(3));
        // Each style call is one melee style, one arrow and one spell element.
        assertEquals(0, BaRules.meleeStyle("Controlled"));
        assertEquals(0, BaRules.arrowStyle("bullet_arrow"));
        assertEquals(0, BaRules.spellStyle("air"));
        assertEquals(3, BaRules.meleeStyle("Defensive"));
        assertEquals(3, BaRules.arrowStyle("barbed_arrow"));
        assertEquals(3, BaRules.spellStyle("fire"));
        assertEquals(-1, BaRules.arrowStyle("rune_arrow"));
        // One poisoned food deals 54 to a Penance healer.
        assertEquals(54, BaRules.foodTotalDamage());
        // Converter: green -> red, red/blue -> green; 20% at level 2.
        assertEquals("red_egg", BaRules.converted("green_egg"));
        assertEquals("green_egg", BaRules.converted("blue_egg"));
        assertEquals(0, BaRules.convertPercent(1));
        assertEquals(20, BaRules.convertPercent(2));
        // Omega egg: healer, attacker, defender, then the collector loads it.
        assertEquals(BaRules.HEALER, BaRules.handTo(BaRules.YELLOW_EGG));
        assertEquals(BaRules.POISONED_EGG, BaRules.stepAt(BaRules.POISON_CRATER).result());
        assertEquals(BaRules.OMEGA_EGG, BaRules.stepAt(BaRules.LAVA_CRATER).result());
        assertEquals(BaRules.COLLECTOR, BaRules.handTo(BaRules.OMEGA_EGG));
        // Omega eggs hit 30-39 (wiki); the queen dies to 7 (14.9568%), 8 (85.01889%) or 9 (0.02431%) of them, as the wiki works out.
        assertEquals(30, BaRules.omegaEggDamage(0.0));
        assertEquals(39, BaRules.omegaEggDamage(0.9999));
        assertEquals(34, BaRules.omegaEggDamage(0.45));
        int queen = BaWaves.QUEEN.hitpoints();
        assertEquals(0.0, BaRules.queenDiesInExactly(6, queen), 1e-12);
        assertEquals(14.9568, 100 * BaRules.queenDiesInExactly(7, queen), 5e-5);
        assertEquals(85.01889, 100 * BaRules.queenDiesInExactly(8, queen), 5e-6);
        assertEquals(0.02431, 100 * BaRules.queenDiesInExactly(9, queen), 5e-6);
        assertEquals(0.0, BaRules.queenDiesInExactly(10, queen), 1e-12);
        // Green egg poison: every 3 s on the others, every 18 s on a Penance healer, five hits of 1.
        assertEquals(5, BaRules.greenPoisonInterval(false));
        assertEquals(30, BaRules.greenPoisonInterval(true));
        assertEquals(1, BaRules.greenPoisonHit(4));
        assertEquals(0, BaRules.greenPoisonHit(5));
        // The hopper holds five of each colour.
        assertEquals(5, BaRules.HOPPER_MAX_PER_COLOUR);
        // Runners walk south 4 times in 6.
        assertEquals(BaRules.RUNNER_STEP, BaRules.runnerWalk(0.1, false, 0)[1]);
        assertEquals(-BaRules.RUNNER_STEP, BaRules.runnerWalk(0.7, false, 0)[0]);
        assertEquals(-3, BaRules.runnerWalk(0.1, true, -3)[0]);
        // Healer drinks from the spring: level 5 with 99 hitpoints heals 5 + 49.
        assertEquals(54, BaRules.springSelfHeal(5, 99));
        // Commander Connad takes gear back for 80% (wiki "buy" column: 300 / 220 / 120 / 80 each role).
        assertEquals(List.of(300, 220, 120, 80), List.of(BaRules.refund(375), BaRules.refund(275), BaRules.refund(150),
                BaRules.refund(100)));
    }

    @Test
    void collectionLogPageMatchesTheWiki() throws IOException {
        java.util.Set<String> wiki = new java.util.TreeSet<>();
        for (String line : Files.readAllLines(Path.of("src/test/resources/osrs/bosses/collection_log_pages.tsv"), StandardCharsets.UTF_8)) {
            String[] f = line.split("\\|", -1);
            if (f.length == 4 && f[0].equals("Barbarian Assault") && !f[3].equals("-")) {
                wiki.add(f[3]);
            }
        }
        assertEquals(11, wiki.size());
        com.runecraft.osrs.CollectionLogData.Page page = com.runecraft.osrs.CollectionLogData.pages().stream()
                .filter(p -> p.name().equals("Barbarian Assault")).findFirst().orElseThrow();
        assertEquals(wiki, new java.util.TreeSet<>(page.items()));
    }

    @Test
    void roleItemsExist() {
        List<String> ids = new ArrayList<>();
        ids.addAll(BaRules.ARROWS);
        ids.addAll(BaRules.EGGS);
        ids.addAll(BaRules.BAIT);
        ids.addAll(BaRules.POISON_FOOD);
        ids.addAll(List.of(BaRules.YELLOW_EGG, BaRules.POISONED_EGG, BaRules.SPIKED_EGG, BaRules.OMEGA_EGG, BaRules.HEALING_VIAL,
                "collection_bag"));
        for (String role : MinigameRules.BarbarianAssault.ROLES) {
            ids.add(role + "_horn");
            ids.add(role + "_icon");
        }
        for (String id : ids) {
            ItemDef def = ItemCatalog.get(id);
            assertNotNull(def, id);
        }
        for (String arrow : BaRules.ARROWS) {
            assertEquals(ItemDef.Kind.ARROW, ItemCatalog.get(arrow).kind(), arrow);
            assertEquals(BaRules.ARROW_RANGED_STRENGTH, ItemCatalog.get(arrow).rangedStrength(), arrow);
            assertTrue(com.runecraft.osrs.data.AmmoRules.canFire("shortbow", arrow), arrow + " from a shortbow");
        }
    }
}
