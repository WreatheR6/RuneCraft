package com.runecraft.osrs;

import com.runecraft.osrs.content.quests.QuestBook;
import com.runecraft.osrs.content.systems.Jewellery;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The gear totals PlayerHitCalc sees in game (armour/weapon/shield, the extra jewellery / cape / gloves slots and quest
 * gear such as the imbued god capes, summed into one {@link EquipmentStats}) against the DPS calculator's raw sums in
 * {@code dps_reference.tsv}: attack bonus of the style, melee strength, ranged strength and magic damage, for every
 * loadout. Before round 15 the Ultor / Bellator / Magus / Venator rings and Zaryte vambraces had no stats in game, and
 * quest-slot items multiplied the max hit separately.
 */
class GearTotalsReferenceTest {
    /** In-game stats of one item: its equipment definition, else its extra-slot (Jewellery) stats, else quest gear. */
    static EquipmentStats stats(String id) {
        ItemDef d = ItemCatalog.get(id);
        if (d != null && d.equipment() != null) {
            return d.equipment().stats();
        }
        Jewellery.Worn w = Jewellery.worn(id);
        if (w != null) {
            return w.stats();
        }
        QuestBook.Gear g = QuestBook.get().gear.get(id);
        return g != null ? g.stats() : null;
    }

    private static int attack(EquipmentStats s, String type) {
        return switch (type) {
            case "stab" -> s.attack(AttackType.STAB);
            case "slash" -> s.attack(AttackType.SLASH);
            case "crush" -> s.attack(AttackType.CRUSH);
            case "ranged" -> s.attack(AttackType.RANGED);
            default -> s.attack(AttackType.MAGIC);
        };
    }

    @Test
    void gearTotalsMatchTheDpsCalculator() throws IOException {
        Map<String, String> ids = new HashMap<>();
        for (ItemDef d : ItemCatalog.all()) {
            ids.putIfAbsent(d.name().toLowerCase(Locale.ROOT), d.id());
        }
        Map<String, String> bad = new LinkedHashMap<>();
        String[] header = null;
        int loadouts = 0;
        Map<String, Boolean> seen = new HashMap<>();
        for (String line : CombatReferenceTest.lines()) {
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
            String gear = r.get("gear");
            if (seen.put(gear + "|" + r.get("type"), Boolean.TRUE) != null) {
                continue;
            }
            loadouts++;
            EquipmentStats total = EquipmentStats.NONE;
            List<String> missing = new ArrayList<>();
            // Ammunition and thrown weapons are fired from the inventory: ranged strength from the ammo table, a thrown
            // weapon's ranged attack from EquipmentBonuses.thrownRangedAttack (HitCalcBridge adds both).
            int ammo = 0;
            int thrownAttack = 0;
            for (String name : gear.split(",")) {
                String id = ids.get(name.toLowerCase(Locale.ROOT));
                EquipmentStats s = id == null ? null : stats(id);
                if (s == null && id != null) {
                    ItemDef d = ItemCatalog.get(id);
                    Integer a = com.runecraft.osrs.data.EquipmentBonuses.ammoStrength(id);
                    if (a != null || (d != null && d.rangedStrength() > 0)) {
                        ammo += a != null ? a : d.rangedStrength();
                        thrownAttack += com.runecraft.osrs.data.EquipmentBonuses.thrownRangedAttack(id);
                        continue;
                    }
                }
                if (s == null) {
                    missing.add(name);
                    continue;
                }
                total = total.plus(s);
            }
            List<String> diffs = new ArrayList<>();
            if (!missing.isEmpty()) {
                diffs.add("no in-game stats for " + missing);
            }
            String type = r.get("type");
            int attack = attack(total, type) + (type.equals("ranged") ? thrownAttack : 0);
            if (attack != Integer.parseInt(r.get("offBonus"))) {
                diffs.add(type + " attack " + attack + " != " + r.get("offBonus"));
            }
            if (total.meleeStrength() != Integer.parseInt(r.get("strBonus"))) {
                diffs.add("strength " + total.meleeStrength() + " != " + r.get("strBonus"));
            }
            if (type.equals("ranged") && total.rangedStrength() + ammo != Integer.parseInt(r.get("rangedStr"))) {
                diffs.add("ranged strength " + (total.rangedStrength() + ammo) + " != " + r.get("rangedStr"));
            }
            if (type.equals("magic") && total.magicDamagePercent() * 10 != Integer.parseInt(r.get("magicDmgTenths"))) {
                diffs.add("magic damage " + total.magicDamagePercent() * 10 + " != " + r.get("magicDmgTenths"));
            }
            if (!diffs.isEmpty()) {
                bad.put(r.get("loadout") + " (" + type + ")", String.join(", ", diffs));
            }
        }
        assertTrue(loadouts >= 70, "only " + loadouts + " loadouts");
        StringBuilder sb = new StringBuilder();
        bad.forEach((k, v) -> sb.append(k).append(": ").append(v).append('\n'));
        assertTrue(bad.isEmpty(), bad.size() + " of " + loadouts + " loadouts differ from the DPS calculator:\n" + sb);
    }
}
