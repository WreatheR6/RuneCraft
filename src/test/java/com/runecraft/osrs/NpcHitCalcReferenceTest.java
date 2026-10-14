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
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Diffs {@link NpcHitCalc} against the OSRS Wiki DPS calculator's {@code NPCVsPlayerCalc}:
 * {@code src/test/resources/osrs/combat/npc_reference.tsv} holds 1,040 monster-vs-player scenarios (8 Defence / Magic /
 * prayer profiles x 12 gear sets and stances x 26 melee, ranged and magic monsters) with the NPC attack roll, the
 * player's defence roll, the hit chance and the NPC max hit formula (generator in {@code combat/generator/}).
 */
class NpcHitCalcReferenceTest {
    private static final Path FIXTURE = Path.of("src/test/resources/osrs/combat/npc_reference.tsv");

    private static AttackType type(String t) {
        return switch (t) {
            case "stab" -> AttackType.STAB;
            case "slash" -> AttackType.SLASH;
            case "crush" -> AttackType.CRUSH;
            case "ranged" -> AttackType.RANGED;
            default -> AttackType.MAGIC;
        };
    }

    private static CombatStyle stance(String s) {
        return switch (s) {
            case "Defensive" -> CombatStyle.DEFENSIVE;
            case "Controlled" -> CombatStyle.CONTROLLED;
            case "Longrange" -> CombatStyle.LONGRANGE;
            case "Aggressive" -> CombatStyle.AGGRESSIVE;
            case "Rapid" -> CombatStyle.RAPID;
            default -> CombatStyle.ACCURATE;
        };
    }

    @Test
    void monsterAttacksMatchTheDpsCalculator() throws IOException {
        List<String> bad = new ArrayList<>();
        Map<String, Integer> badByLoadout = new TreeMap<>();
        String[] header = null;
        int rows = 0;
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
            rows++;
            NpcHitCalc.Input in = new NpcHitCalc.Input();
            in.style = type(r.get("npcStyle"));
            in.npcLevel = Integer.parseInt(r.get("npcLevel"));
            in.npcAttackBonus = Integer.parseInt(r.get("npcBonus"));
            in.npcStrengthLevel = Integer.parseInt(r.get("npcStrLevel"));
            in.npcStrengthBonus = Integer.parseInt(r.get("npcStrBonus"));
            in.defence = Integer.parseInt(r.get("def"));
            in.magic = Integer.parseInt(r.get("magic"));
            in.hitpointsLevel = Integer.parseInt(r.get("hpMax"));
            in.hitpoints = Integer.parseInt(r.get("hpCur"));
            if (!r.get("prayers").equals("-")) {
                for (String p : r.get("prayers").split(",")) {
                    in.prayers.add(Prayer.valueOf(p.toUpperCase(Locale.ROOT)));
                }
            }
            in.stance = stance(r.get("stance"));
            in.defenceBonus = Integer.parseInt(r.get("defBonus"));
            List<String> gear = List.of(r.get("gear").split(","));
            in.torags = gear.containsAll(List.of("Torag's helm", "Torag's platebody", "Torag's platelegs", "Torag's hammers", "Amulet of the damned"));
            List<String> diffs = new ArrayList<>();
            if (NpcHitCalc.npcAttackRoll(in) != Long.parseLong(r.get("npcAttackRoll"))) {
                diffs.add("npc roll " + NpcHitCalc.npcAttackRoll(in) + " != " + r.get("npcAttackRoll"));
            }
            if (NpcHitCalc.playerDefenceRoll(in) != Long.parseLong(r.get("playerDefenceRoll"))) {
                diffs.add("defence roll " + NpcHitCalc.playerDefenceRoll(in) + " != " + r.get("playerDefenceRoll"));
            }
            double hc = NpcHitCalc.hitChance(in);
            if (Math.abs(hc - Double.parseDouble(r.get("hitChance"))) > 1e-5) {
                diffs.add(String.format(Locale.ROOT, "hit chance %.6f != %s", hc, r.get("hitChance")));
            }
            if (!r.get("npcMaxHit").equals("-") && NpcHitCalc.formulaMaxHit(in) != Integer.parseInt(r.get("npcMaxHit"))) {
                diffs.add("max " + NpcHitCalc.formulaMaxHit(in) + " != " + r.get("npcMaxHit"));
            }
            if (!diffs.isEmpty()) {
                bad.add(r.get("id") + ": " + String.join(", ", diffs));
                badByLoadout.merge(r.get("loadout"), 1, Integer::sum);
            }
        }
        assertTrue(rows >= 1000, "only " + rows + " reference rows");
        assertTrue(bad.isEmpty(), bad.size() + " of " + rows + " scenarios differ from the DPS calculator; by loadout "
                + badByLoadout + ":\n" + String.join("\n", bad.subList(0, Math.min(60, bad.size()))));
    }

    @Test
    void protectionPrayersBlockTheirStyle() {
        assertTrue(NpcHitCalc.protectedFrom(List.of(Prayer.PROTECT_FROM_MELEE), AttackType.SLASH));
        assertTrue(NpcHitCalc.protectedFrom(List.of(Prayer.PROTECT_FROM_MISSILES), AttackType.RANGED));
        assertTrue(NpcHitCalc.protectedFrom(List.of(Prayer.PROTECT_FROM_MAGIC, Prayer.AUGURY), AttackType.MAGIC));
        assertFalse(NpcHitCalc.protectedFrom(List.of(Prayer.PROTECT_FROM_MELEE), AttackType.MAGIC));
        assertFalse(NpcHitCalc.protectedFrom(List.of(), AttackType.CRUSH));
    }

    @Test
    void magicDefenceUsesSeventyPercentMagic() {
        NpcHitCalc.Input in = new NpcHitCalc.Input();
        in.style = AttackType.MAGIC;
        in.defence = 1;
        in.magic = 99;
        // floor(99 * 0.7) + floor(1 * 0.3) = 69, +8 = 77, x 64
        assertEquals(77L * 64, NpcHitCalc.playerDefenceRoll(in));
        in.style = AttackType.SLASH;
        assertEquals(9L * 64, NpcHitCalc.playerDefenceRoll(in));
    }
}
