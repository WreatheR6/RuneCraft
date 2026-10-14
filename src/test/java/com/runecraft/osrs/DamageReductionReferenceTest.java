package com.runecraft.osrs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Diffs {@link NpcHitCalc#reduceIncoming} (Elysian spirit shield, Dinh's bulwark on Block, the Justiciar set) against the
 * OSRS Wiki DPS calculator's {@code NPCVsPlayerCalc}: {@code src/test/resources/osrs/combat/npc_reduction_reference.tsv}
 * holds 416 monster-vs-player scenarios (2 profiles x 8 gear sets x 26 monsters) with the calculator's max hit, hit
 * chance, expected damage per attack and largest hit after the reductions (generator {@code combat/generator/gen_npc_reference.ts}).
 */
class DamageReductionReferenceTest {
    private static final Path FIXTURE = Path.of("src/test/resources/osrs/combat/npc_reduction_reference.tsv");

    @Test
    void reductionsMatchTheDpsCalculator() throws IOException {
        List<String> bad = new ArrayList<>();
        String[] header = null;
        int rows = 0;
        int reduced = 0;
        for (String line : Files.readAllLines(FIXTURE, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\t", -1);
            if (header == null) {
                header = f;
                continue;
            }
            Map<String, String> r = new HashMap<>();
            for (int i = 0; i < header.length; i++) {
                r.put(header[i], f[i]);
            }
            List<String> gear = List.of(r.get("gear").split(","));
            boolean elysian = gear.contains("Elysian spirit shield");
            boolean dinhs = gear.contains("Dinh's bulwark") && r.get("styleName").equals("Block");
            boolean justiciar = gear.contains("Justiciar faceguard") && gear.contains("Justiciar chestguard") && gear.contains("Justiciar legguards");
            int bonus = Integer.parseInt(r.get("defBonus"));
            if (justiciar && bonus < 0) {
                continue; // RuneCraft never lets a negative bonus add damage (the calculator does)
            }
            rows++;
            int max = Integer.parseInt(r.get("npcMaxHit"));
            double hc = Double.parseDouble(r.get("hitChance"));
            double expected = 0;
            int distMax = 0;
            for (int d = 0; d <= max; d++) {
                double p = hc / (max + 1);
                int plain = NpcHitCalc.reduceIncoming(d, false, dinhs, justiciar, bonus);
                if (elysian) {
                    int proc = NpcHitCalc.reduceIncoming(d, true, dinhs, justiciar, bonus);
                    expected += p * (1 - NpcHitCalc.ELYSIAN_CHANCE) * plain + p * NpcHitCalc.ELYSIAN_CHANCE * proc;
                    distMax = Math.max(distMax, Math.max(plain, proc));
                } else {
                    expected += p * plain;
                    distMax = Math.max(distMax, plain);
                }
            }
            if (elysian || dinhs || justiciar) {
                reduced++;
            }
            List<String> diffs = new ArrayList<>();
            if (Math.abs(expected - Double.parseDouble(r.get("expected"))) > 2e-6) {
                diffs.add(String.format(Locale.ROOT, "expected %.6f != %s", expected, r.get("expected")));
            }
            if (distMax != Integer.parseInt(r.get("distMax"))) {
                diffs.add("max " + distMax + " != " + r.get("distMax"));
            }
            if (!diffs.isEmpty()) {
                bad.add(r.get("id") + ": " + String.join(", ", diffs));
            }
        }
        assertTrue(rows >= 350, "only " + rows + " rows");
        assertTrue(reduced >= 300, "only " + reduced + " rows with a reduction");
        assertTrue(bad.isEmpty(), bad.size() + " of " + rows + " differ:\n" + String.join("\n", bad.subList(0, Math.min(40, bad.size()))));
    }

    @Test
    void singleHitExamples() {
        assertEquals(30, NpcHitCalc.elysian(40)); // -25%
        assertEquals(0, NpcHitCalc.elysian(1)); // at least 1
        assertEquals(1, NpcHitCalc.elysian(2));
        assertEquals(32, NpcHitCalc.dinhsBlock(40));
        assertEquals(36, NpcHitCalc.justiciar(40, 300)); // 40 x (1 - 300/3000)
        assertEquals(40, NpcHitCalc.justiciar(40, -20));
    }
}
