package com.runecraft.osrs;

import com.runecraft.osrs.content.systems.Spellbooks;
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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Diffs {@link PlayerHitCalc} (and the spell / powered staff max hits it uses) against the OSRS Wiki DPS
 * calculator: {@code src/test/resources/osrs/combat/dps_reference.tsv} holds 3,298 scenarios (5 level/prayer
 * profiles x 82 melee, ranged and magic loadouts x 20 monsters, including thrown weapons and crossbows against the
 * light / heavy ranged defences and spells of every element against elemental weaknesses; since round 17 also vampyres
 * of all three tiers against vampyrebane / silver weapons and Efaritay's aid, and revenants against the amulet of avarice
 * and the revenant weapons in and out of the Wilderness) computed by weirdgloop/osrs-dps-calc's
 * {@code PlayerVsNPCCalc}: max hit, min hit, attack roll, NPC defence roll, hit chance and the largest hit after the
 * calculator's hit transforms.
 * Gear names are mapped to RuneCraft item ids through the item catalogue; gear bonuses are the calculator's sums.
 */
class CombatReferenceTest {
    private static final String FIXTURE = "osrs/combat/dps_reference.tsv";

    static List<String> lines() throws IOException {
        Path p = Path.of("src/test/resources", FIXTURE);
        if (Files.exists(p)) {
            return Files.readAllLines(p, StandardCharsets.UTF_8);
        }
        try (InputStream in = CombatReferenceTest.class.getClassLoader().getResourceAsStream(FIXTURE)) {
            assertTrue(in != null, "missing " + FIXTURE);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }

    private static Map<String, String> idsByName() {
        Map<String, String> ids = new HashMap<>();
        for (ItemDef d : ItemCatalog.all()) {
            ids.putIfAbsent(d.name().toLowerCase(Locale.ROOT), d.id());
        }
        return ids;
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
            case "Defensive" -> CombatStyle.DEFENSIVE;
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

    /** Sets the spell's level-scaled base max hit, element and spellbook from RuneCraft's spell data. */
    static String spell(PlayerHitCalc.Input in, String name) {
        if (name.equals("-")) {
            return null;
        }
        for (Spell s : Spell.values()) {
            if (s.displayName().equalsIgnoreCase(name)) {
                in.spellMaxHit = s.maxHitAt(in.magic);
                in.spellElement = s.element();
                in.standardSpell = true;
                in.boltSpell = name.contains("Bolt");
                return null;
            }
        }
        for (Spellbooks.Spell s : Spellbooks.all()) {
            if (s.name().equalsIgnoreCase(name)) {
                in.spellMaxHit = s.id().equals("magic_dart") ? Spellbooks.magicDartMax(in.magic) : s.maxHit();
                in.standardSpell = s.book() == Spellbooks.Book.STANDARD;
                return null;
            }
        }
        return "unknown spell " + name;
    }

    @Test
    void playerHitsMatchTheDpsCalculator() throws IOException {
        Map<String, String> ids = idsByName();
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
            in.type = type(r.get("type"));
            in.style = style(r.get("type"), r.get("stance"));
            in.attack = Integer.parseInt(r.get("atk"));
            in.strength = Integer.parseInt(r.get("str"));
            in.ranged = Integer.parseInt(r.get("ranged"));
            in.magic = Integer.parseInt(r.get("magic"));
            in.hitpointsLevel = Integer.parseInt(r.get("hpMax"));
            in.hitpoints = Integer.parseInt(r.get("hpCur"));
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
            String spellErr = spell(in, r.get("spell"));
            in.targetDefenceLevel = Integer.parseInt(r.get("npcDefLevel"));
            in.targetMagicLevel = in.targetDefenceLevel;
            in.targetDefenceBonus = Integer.parseInt(r.get("npcDefBonus"));
            // The three ranged defences: PlayerHitCalc picks light / standard / heavy from the weapon (thrown, bow, crossbow).
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
            // Round 17 rows: the calculator's buffs (in the Wilderness, Forinthry surge) and revenants by name.
            in.revenant = r.get("monster").startsWith("Revenant");
            String buffs = r.getOrDefault("buffs", "-");
            in.inWilderness = buffs.contains("wildy");
            in.forinthrySurge = buffs.contains("surge");
            String id = r.get("id");
            if (missing != null || spellErr != null) {
                bad.add(id + ": " + (missing != null ? "no RuneCraft item named " + missing : spellErr));
                badByLoadout.merge(r.get("loadout"), 1, Integer::sum);
                continue;
            }
            List<String> diffs = new ArrayList<>();
            int[] mm = PlayerHitCalc.minMax(in);
            int max = PlayerHitCalc.maxHit(in);
            long atk = PlayerHitCalc.attackRoll(in);
            long def = PlayerHitCalc.defenceRoll(in);
            double hc = PlayerHitCalc.hitChance(in);
            if (max != Integer.parseInt(r.get("effMax"))) {
                diffs.add("max " + max + " != " + r.get("effMax"));
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
            // The largest hitsplat after the calculator's hit transforms (vampyre tiers; 0 when the target is immune).
            String distMax = r.getOrDefault("distMax", "-");
            if (!distMax.equals("-") && PlayerHitCalc.afterRoll(in, max) != Integer.parseInt(distMax)) {
                diffs.add("after-roll max " + PlayerHitCalc.afterRoll(in, max) + " != " + distMax);
            }
            if (!diffs.isEmpty()) {
                bad.add(id + ": " + String.join(", ", diffs));
                badByLoadout.merge(r.get("loadout"), 1, Integer::sum);
            }
        }
        assertTrue(rows >= 2000, "only " + rows + " reference rows");
        assertTrue(bad.isEmpty(), bad.size() + " of " + rows + " scenarios differ from the DPS calculator; by loadout "
                + badByLoadout + ":\n" + String.join("\n", bad.subList(0, Math.min(60, bad.size()))));
    }
}
