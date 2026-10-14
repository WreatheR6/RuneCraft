package com.runecraft.osrs;

import com.runecraft.osrs.content.systems.SpecialAttacks;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Diffs {@link PlayerHitCalc}'s special attacks against the OSRS Wiki DPS calculator:
 * {@code src/test/resources/osrs/combat/spec_reference.tsv} holds 1,000 scenarios (3 level / prayer profiles x 50 special attack
 * loadouts x up to 12 monsters) computed by weirdgloop/osrs-dps-calc's {@code PlayerVsNPCCalc} with {@code usingSpecialAttack}
 * (generator: {@code generator/gen_dps_reference.ts}): min / max hit, attack roll, the defence roll against the style the
 * special rolls against, hit chance, the number of hits and the largest total of the attack.
 */
class SpecialAttackReferenceTest {
    private static final String FIXTURE = "osrs/combat/spec_reference.tsv";
    /** Calculator names whose RuneCraft item has another name. */
    private static final Map<String, String> ALIASES = Map.of("crystal halberd", "crystal_halberd_perfected");

    static List<String> lines() throws IOException {
        Path p = Path.of("src/test/resources", FIXTURE);
        if (Files.exists(p)) {
            return Files.readAllLines(p, StandardCharsets.UTF_8);
        }
        try (InputStream in = SpecialAttackReferenceTest.class.getClassLoader().getResourceAsStream(FIXTURE)) {
            assertTrue(in != null, "missing " + FIXTURE);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }

    private static CombatStyle style(String type, String stance) {
        if (type.equals("ranged")) {
            return switch (stance) {
                case "Rapid" -> CombatStyle.RAPID;
                case "Longrange" -> CombatStyle.LONGRANGE;
                default -> CombatStyle.RANGED_ACCURATE;
            };
        }
        return switch (stance) {
            case "Aggressive" -> CombatStyle.AGGRESSIVE;
            case "Controlled" -> CombatStyle.CONTROLLED;
            case "Defensive", "Autocast" -> CombatStyle.DEFENSIVE;
            case "Longrange" -> CombatStyle.LONGRANGE;
            default -> CombatStyle.ACCURATE;
        };
    }

    private static AttackType type(String t) {
        return switch (t) {
            case "stab" -> AttackType.STAB;
            case "slash" -> AttackType.SLASH;
            case "crush" -> AttackType.CRUSH;
            case "ranged" -> AttackType.RANGED;
            default -> AttackType.MAGIC;
        };
    }

    @Test
    void specialAttacksMatchTheDpsCalculator() throws IOException {
        Map<String, String> ids = new HashMap<>();
        for (ItemDef d : ItemCatalog.all()) {
            ids.putIfAbsent(d.name().toLowerCase(Locale.ROOT), d.id());
        }
        ids.putAll(ALIASES);
        List<String> bad = new ArrayList<>();
        Map<String, Integer> badByLoadout = new TreeMap<>();
        int rows = 0;
        String[] header = null;
        for (String line : lines()) {
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
            PlayerHitCalc.Input in = new PlayerHitCalc.Input();
            in.special = true;
            in.type = type(r.get("type"));
            in.style = style(r.get("type"), r.get("stance"));
            in.attack = Integer.parseInt(r.get("atk"));
            in.strength = Integer.parseInt(r.get("str"));
            in.ranged = Integer.parseInt(r.get("ranged"));
            in.magic = Integer.parseInt(r.get("magic"));
            in.hitpointsLevel = Integer.parseInt(r.get("hpMax"));
            in.hitpoints = in.hitpointsLevel;
            if (!r.get("prayers").equals("-")) {
                for (String p : r.get("prayers").split(",")) {
                    in.prayers.add(Prayer.valueOf(p.toUpperCase(Locale.ROOT)));
                }
            }
            String missing = null;
            String[] gear = r.get("gear").split(",");
            for (int i = 0; i < gear.length; i++) {
                String id = ids.get(gear[i].toLowerCase(Locale.ROOT));
                if (id == null) {
                    missing = gear[i];
                    continue;
                }
                in.worn.add(id);
                if (i == 0) {
                    in.weapon = id;
                }
            }
            in.attackBonus = Integer.parseInt(r.get("offBonus"));
            in.strengthBonus = Integer.parseInt(r.get("strBonus"));
            in.rangedStrength = Integer.parseInt(r.get("rangedStr"));
            in.magicDamageTenths = Integer.parseInt(r.get("magicDmgTenths"));
            in.targetDefenceLevel = Integer.parseInt(r.get("npcDefLevel"));
            in.targetMagicLevel = in.targetDefenceLevel;
            in.targetDefenceBonus = Integer.parseInt(r.get("npcDefBonus"));
            in.targetRangedDefence = new int[]{Integer.parseInt(r.get("npcRangedLight")), Integer.parseInt(r.get("npcRangedStandard")),
                    Integer.parseInt(r.get("npcRangedHeavy"))};
            in.targetTbowMagic = Integer.parseInt(r.get("npcTbowMagic"));
            if (!r.get("attrs").equals("-")) {
                in.attributes.addAll(List.of(r.get("attrs").split(",")));
            }
            in.onTask = r.get("slayerMonster").equals("1");
            in.targetSize = Integer.parseInt(r.get("size"));
            if (!r.get("weakness").equals("-")) {
                String[] w = r.get("weakness").split(":");
                in.weaknessElement = w[0];
                in.weaknessPercent = Integer.parseInt(w[1]);
            }
            String tags = r.get("tags");
            if (tags.startsWith("soul-")) {
                in.soulreaperStacks = Integer.parseInt(tags.substring(5));
            } else if (tags.startsWith("prayer-")) {
                in.prayerMissing = Integer.parseInt(tags.substring(7));
            }
            String id = r.get("id");
            if (missing != null) {
                bad.add(id + ": no RuneCraft item named " + missing);
                badByLoadout.merge(r.get("loadout"), 1, Integer::sum);
                continue;
            }
            List<String> diffs = new ArrayList<>();
            if (SpecialAttacks.of(in.weapon) == null) {
                diffs.add("no special attack for " + in.weapon);
            }
            String specDef = r.get("specDef");
            AttackType against = PlayerHitCalc.specialDefenceType(in);
            String ours = against == null ? r.get("type") : against.name().toLowerCase(Locale.ROOT);
            if (!ours.equals(specDef)) {
                diffs.add("defence style " + ours + " != " + specDef);
            }
            int[] mm = PlayerHitCalc.minMax(in);
            long atk = PlayerHitCalc.attackRoll(in);
            long def = PlayerHitCalc.defenceRoll(in);
            double hc = PlayerHitCalc.hitChance(in);
            if (mm[1] != Integer.parseInt(r.get("maxHit"))) {
                diffs.add("max " + mm[1] + " != " + r.get("maxHit"));
            }
            if (mm[0] != Integer.parseInt(r.get("minHit"))) {
                diffs.add("min " + mm[0] + " != " + r.get("minHit"));
            }
            if (atk != Long.parseLong(r.get("attackRoll"))) {
                diffs.add("attack roll " + atk + " != " + r.get("attackRoll"));
            }
            if (def != Long.parseLong(r.get("defenceRoll"))) {
                diffs.add("defence roll " + def + " != " + r.get("defenceRoll"));
            }
            if (Math.abs(hc - Double.parseDouble(r.get("hitChance"))) > 1e-5) {
                diffs.add(String.format(Locale.ROOT, "hit chance %.6f != %s", hc, r.get("hitChance")));
            }
            if (PlayerHitCalc.specialHits(in) != Integer.parseInt(r.get("hits"))) {
                diffs.add("hits " + PlayerHitCalc.specialHits(in) + " != " + r.get("hits"));
            }
            // The calculator halves non-corpbane damage to the Corporeal Beast inside its attacker distribution; not compared.
            boolean corp = r.get("monster").startsWith("Corporeal Beast");
            if (!corp && PlayerHitCalc.specialMaxTotal(in) != Integer.parseInt(r.get("distMax"))) {
                diffs.add("max total " + PlayerHitCalc.specialMaxTotal(in) + " != " + r.get("distMax"));
            }
            if (!diffs.isEmpty()) {
                bad.add(id + ": " + String.join(", ", diffs));
                badByLoadout.merge(r.get("loadout"), 1, Integer::sum);
            }
        }
        assertTrue(rows >= 900, "only " + rows + " reference rows");
        assertTrue(bad.isEmpty(), bad.size() + " of " + rows + " special attack scenarios differ from the DPS calculator; by loadout "
                + badByLoadout + ":\n" + String.join("\n", bad.subList(0, Math.min(60, bad.size()))));
    }

    /** Wiki "Special attacks" energy costs (Kiyogitpy osrs-wiki-data dump, July 2026). */
    @Test
    void energyCostsMatchTheWiki() {
        Map<String, Integer> wiki = new java.util.LinkedHashMap<>();
        wiki.put("dragon_dagger", 25);
        wiki.put("dragon_longsword", 25);
        wiki.put("dragon_mace", 25);
        wiki.put("dragon_sword", 40);
        wiki.put("dragon_scimitar", 55);
        wiki.put("dragon_warhammer", 50);
        wiki.put("dragon_claws", 50);
        wiki.put("burning_claws", 35);
        wiki.put("dragon_halberd", 30);
        wiki.put("crystal_halberd_perfected", 30);
        wiki.put("armadyl_godsword", 50);
        wiki.put("ancient_godsword", 50);
        wiki.put("elder_maul", 50);
        wiki.put("abyssal_dagger", 25);
        wiki.put("voidwaker", 50);
        wiki.put("granite_maul", 60);
        wiki.put("granite_maul_or", 50);
        wiki.put("granite_hammer", 60);
        wiki.put("armadyl_crossbow", 50);
        wiki.put("zaryte_crossbow", 75);
        wiki.put("dragon_crossbow", 60);
        wiki.put("dark_bow", 55);
        wiki.put("magic_shortbow", 55);
        wiki.put("magic_shortbow_i", 50);
        wiki.put("toxic_blowpipe", 50);
        wiki.put("webweaver_bow", 50);
        wiki.put("heavy_ballista", 65);
        wiki.put("dragon_knife", 25);
        wiki.put("dragon_thrownaxe", 25);
        wiki.put("volatile_nightmare_staff", 55);
        wiki.put("eldritch_nightmare_staff", 55);
        wiki.put("accursed_sceptre", 50);
        wiki.put("eye_of_ayak", 50);
        wiki.put("tonalztics_of_ralos", 50);
        wiki.put("crimson_kisten", 50);
        wiki.put("ursine_chainmace", 50);
        wiki.put("dual_macuahuitl", 25);
        wiki.put("scorching_bow", 25);
        wiki.put("purging_staff", 25);
        wiki.put("dawnbringer", 35);
        wiki.put("saradomin_sword", 100);
        wiki.put("lms_dragon_claws", 50);
        wiki.put("lms_statiuss_warhammer", 35);
        wiki.put("lms_vestas_longsword", 25);
        wiki.put("lms_morrigans_javelin", 50);
        List<String> bad = new ArrayList<>();
        wiki.forEach((id, cost) -> {
            SpecialAttacks.Spec s = SpecialAttacks.of(id);
            if (s == null) {
                bad.add(id + ": no special attack");
            } else if (s.cost() != cost) {
                bad.add(id + ": " + s.cost() + " != " + cost);
            }
        });
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }

    @Test
    void energyRegeneratesTenPercentEveryThirtySeconds() {
        assertEquals(10, SpecialAttacks.regen(0, false, 50));
        assertEquals(0, SpecialAttacks.regen(0, false, 49));
        assertEquals(10, SpecialAttacks.regen(0, true, 25));
        assertEquals(100, SpecialAttacks.regen(95, false, 100));
    }

    @Test
    void clawCascadesFollowTheCalculator() {
        java.util.Random r = new java.util.Random(7);
        for (int i = 0; i < 2000; i++) {
            boolean[] hits = new boolean[4];
            for (int k = 0; k < 4; k++) {
                hits[k] = r.nextBoolean();
            }
            int[] h = SpecialAttacks.clawHits(r, hits, 48);
            assertNotNull(h);
            int sum = 0;
            for (int x : h) {
                sum += x;
            }
            assertTrue(sum <= PlayerHitCalc.clawsMaxTotal(48, false), "claws total " + sum);
        }
        assertEquals(PlayerHitCalc.clawsMaxTotal(48, false), 93);
    }
}
