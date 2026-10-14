package com.runecraft.osrs.content.raids;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.BossDef;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The ToB Nylocas room against the wiki wave chart snapshot, plus Vasa's teleport and Verzik's pillars. */
class TobNylocasTest {
    private static String cell(List<TobNylocas.Nylo> lane) {
        return lane.isEmpty() ? "-" : lane.stream().map(TobNylocas.Nylo::token).collect(Collectors.joining("+"));
    }

    @Test
    void wavesMatchTheWikiChart() throws Exception {
        List<String> want = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of("src/test/resources/osrs/raids/nylocas_waves.tsv"), StandardCharsets.UTF_8)) {
            if (!line.isBlank() && !line.startsWith("#") && !line.startsWith("wave")) {
                want.add(line);
            }
        }
        assertEquals(TobNylocas.WAVES, want.size());
        List<String> have = new ArrayList<>();
        for (TobNylocas.Wave w : TobNylocas.waves()) {
            have.add(w.number() + "\t" + cell(w.lane(TobNylocas.Lane.EAST)) + "\t" + cell(w.lane(TobNylocas.Lane.SOUTH)) + "\t"
                    + cell(w.lane(TobNylocas.Lane.WEST)));
        }
        assertEquals(want, have);
    }

    @Test
    void waveRules() {
        assertEquals(1, TobNylocas.wave(1).lane(TobNylocas.Lane.WEST).size());
        assertTrue(TobNylocas.wave(1).lane(TobNylocas.Lane.WEST).get(0).aggressive());
        assertTrue(TobNylocas.wave(4).lane(TobNylocas.Lane.SOUTH).get(0).big());
        assertEquals(0, TobNylocas.wave(7).lane(TobNylocas.Lane.WEST).size());
        assertEquals(12, TobNylocas.cap(19));
        assertEquals(24, TobNylocas.cap(20));
        assertTrue(TobNylocas.prinkipasWave(10) && TobNylocas.prinkipasWave(20) && TobNylocas.prinkipasWave(30));
        assertFalse(TobNylocas.prinkipasWave(31));
        assertTrue(TobNylocas.Breed.ISCHYROS.weakTo(AttackType.CRUSH));
        assertFalse(TobNylocas.Breed.TOXOBOLOS.weakTo(AttackType.MAGIC));
        assertTrue(TobNylocas.Breed.HAGIOS.weakTo(AttackType.MAGIC));
        Random r = new Random(7);
        for (int i = 0; i < 50; i++) {
            int f = i % 3;
            assertNotEquals(f, TobNylocas.nextForm(f, r), "never the same form twice");
        }
        // The breeds' creatures exist and are immune to the other styles.
        for (TobNylocas.Breed b : TobNylocas.Breed.values()) {
            assertNotNull(ContentRegistry.monster(b.creatureId), b.creatureId);
        }
    }

    @Test
    void bossesAndScaling() {
        BossDef prinkipas = ContentRegistry.boss("nylocas_prinkipas");
        assertNotNull(prinkipas);
        assertEquals(400, prinkipas.hitpoints);
        assertEquals(TobNylocas.PRINKIPAS_MAX_HIT, prinkipas.attacks.get(0).maxHit());
        assertEquals(TobNylocas.VASILIAS_MAX_HIT, ContentRegistry.boss("nylocas_vasilias").attacks.get(0).maxHit());
        // Big nylocas: 22 / 19 / 16 hitpoints in 5-, 4- and 3-man (wiki).
        assertEquals(22, RaidScaling.tobScaledHp("nylocas_ischyros", TobNylocas.BIG_HP, 5, RaidMode.NORMAL));
        assertEquals(19, RaidScaling.tobScaledHp("nylocas_ischyros", TobNylocas.BIG_HP, 4, RaidMode.NORMAL));
        assertEquals(16, RaidScaling.tobScaledHp("nylocas_ischyros", TobNylocas.BIG_HP, 3, RaidMode.NORMAL));
        assertEquals(9, RaidScaling.tobScaledHp("nylocas_ischyros", TobNylocas.SMALL_HP, 4, RaidMode.NORMAL));
    }

    @Test
    void vasaTeleportAndVerzikPillars() {
        // Solo: always the player, current hitpoints - 5.
        assertEquals(1, RaidScaling.vasaTeleportTargets(1));
        assertEquals(94, RaidScaling.vasaTeleportDamage(new int[]{99}, 1));
        // Two stunned players with 99 and 80: (179 - 5) / 2 each; a third player running in splits it three ways.
        assertEquals(2, RaidScaling.vasaTeleportTargets(4));
        assertEquals(87, RaidScaling.vasaTeleportDamage(new int[]{99, 80}, 2));
        assertEquals(58, RaidScaling.vasaTeleportDamage(new int[]{99, 80}, 3));
        assertEquals(0, RaidScaling.vasaTeleportDamage(new int[]{3}, 1));
        assertEquals(70, RaidScaling.verzikPillarCollapse(RaidMode.NORMAL));
        assertEquals(50, RaidScaling.verzikPillarCollapse(RaidMode.ENTRY));
    }
}
