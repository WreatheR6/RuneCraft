package com.runecraft.osrs;

import com.runecraft.osrs.content.CommonEquipment;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.osrs.data.EquipmentBonuses;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every RuneCraft wearable whose name matches an item of the OSRS Wiki DPS calculator's {@code equipment.json}
 * (snapshot in {@code src/test/resources/osrs/equipment_reference.tsv}, with aliases for RuneCraft's names of
 * charged, trimmed and renamed variants) has the wiki's bonuses, slot, attack speed and two-handedness. Also checks
 * that every item of {@link CommonEquipment} can actually be obtained.
 */
class EquipmentWikiSyncTest {
    private static final String FIXTURE = "osrs/equipment_reference.tsv";

    /** One reference row: OSRS slot name, attack speed, two-handed flag and the 14 bonuses. */
    private record Ref(String slot, int speed, boolean twoHanded, int[] stats) {
    }

    private static List<String> fixtureLines() throws IOException {
        Path p = Path.of("src/test/resources", FIXTURE);
        if (Files.exists(p)) {
            return Files.readAllLines(p, StandardCharsets.UTF_8);
        }
        try (InputStream in = EquipmentWikiSyncTest.class.getClassLoader().getResourceAsStream(FIXTURE)) {
            assertTrue(in != null, "missing " + FIXTURE);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }

    private static Map<String, Ref> reference() throws IOException {
        Map<String, Ref> out = new HashMap<>();
        for (String line : fixtureLines()) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\\|");
            String[] v = f[4].split(",");
            int[] stats = new int[14];
            for (int i = 0; i < 14; i++) {
                stats[i] = Integer.parseInt(v[i]);
            }
            out.put(f[0], new Ref(f[1], Integer.parseInt(f[2]), f[3].equals("1"), stats));
        }
        return out;
    }

    private static int[] line(EquipmentStats s) {
        return new int[]{s.stabAttack(), s.slashAttack(), s.crushAttack(), s.magicAttack(), s.rangedAttack(),
                s.stabDefence(), s.slashDefence(), s.crushDefence(), s.magicDefence(), s.rangedDefence(),
                s.meleeStrength(), s.rangedStrength(), s.magicDamagePercent(), s.prayer()};
    }

    private static String osrsSlot(EquipmentDef.Slot s) {
        return s == EquipmentDef.Slot.CHEST ? "body" : s.name().toLowerCase(Locale.ROOT);
    }

    @Test
    void wearablesMatchTheWikiBonuses() throws IOException {
        Map<String, Ref> ref = reference();
        List<String> bad = new ArrayList<>();
        int compared = 0;
        for (ItemDef def : ItemCatalog.all()) {
            Ref r = ref.get(def.name().toLowerCase(Locale.ROOT));
            Jewellery.Worn worn = Jewellery.worn(def.id());
            if (r == null || (def.equipment() == null && worn == null)) {
                continue;
            }
            compared++;
            // Items worn in the systems module's extra slots carry HEAD-slot equipment only for tooltips.
            String slot = worn != null ? worn.slot().name().toLowerCase(Locale.ROOT) : osrsSlot(def.equipment().slot());
            int[] stats = line(worn != null ? worn.stats() : def.equipment().stats());
            List<String> why = new ArrayList<>();
            if (!slot.equals(r.slot())) {
                why.add("slot " + slot + " (wiki " + r.slot() + ")");
            }
            if (!java.util.Arrays.equals(stats, r.stats())) {
                why.add("bonuses " + java.util.Arrays.toString(stats) + " (wiki " + java.util.Arrays.toString(r.stats()) + ")");
            }
            if (worn == null && def.equipment().slot() == EquipmentDef.Slot.WEAPON) {
                if (r.speed() > 0 && def.equipment().attackSpeed() != r.speed()) {
                    why.add("speed " + def.equipment().attackSpeed() + " (wiki " + r.speed() + ")");
                }
                if (EquipmentBonuses.twoHanded(def.id()) != r.twoHanded()) {
                    why.add("two-handed " + EquipmentBonuses.twoHanded(def.id()) + " (wiki " + r.twoHanded() + ")");
                }
            }
            if (!why.isEmpty()) {
                bad.add(def.id() + ": " + String.join(", ", why));
            }
        }
        assertTrue(compared > 1000, "only " + compared + " wearables matched the wiki snapshot");
        assertTrue(bad.isEmpty(), bad.size() + " wearables differ from the OSRS Wiki:\n" + String.join("\n", bad));
    }

    /** Arrows, bolts, darts, knives, thrownaxes and javelins have the wiki's ranged strength (ammo or weapon row). */
    @Test
    void ammunitionMatchesTheWikiRangedStrength() throws IOException {
        Map<String, Ref> ref = reference();
        List<String> bad = new ArrayList<>();
        int compared = 0;
        for (ItemDef def : ItemCatalog.all()) {
            Ref r = ref.get(def.name().toLowerCase(Locale.ROOT));
            // The atlatl dart has no ranged strength in OSRS (the eclipse atlatl scales with Strength); RuneCraft gives it 40.
            if (def.kind() != ItemDef.Kind.ARROW || r == null || def.id().equals("atlatl_dart")) {
                continue;
            }
            compared++;
            if (def.rangedStrength() != r.stats()[11]) {
                bad.add(def.id() + ": ranged strength " + def.rangedStrength() + " (wiki " + r.stats()[11] + ")");
            }
        }
        assertTrue(compared > 40, "only " + compared + " ammunition items matched the wiki snapshot");
        assertTrue(bad.isEmpty(), bad.size() + " ammunition items differ from the OSRS Wiki:\n" + String.join("\n", bad));
    }

    @Test
    void commonEquipmentIsObtainable() {
        List<String> ids = new ArrayList<>();
        CommonEquipment.add((id, name, kind, shape, color) -> {
            ids.add(id);
            return new ItemDef(id, name, kind, shape, color);
        });
        ids.add("dragon_dart");
        assertTrue(ids.size() > 40, ids.toString());
        Set<String> sources = CollectionLogSources.all();
        List<String> orphans = ids.stream().filter(id -> !sources.contains(id)).toList();
        assertTrue(orphans.isEmpty(), "no drop, shop, recipe or reward gives " + orphans);
        for (String id : ids) {
            assertTrue(ItemCatalog.exists(id), id);
        }
    }
}
