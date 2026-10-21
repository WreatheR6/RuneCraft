package com.runecraft.osrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Ammo recovery against the OSRS Wiki: the device rates and the Ava's device metallic interference table (snapshot). */
class AmmoRecoveryTest {
    private static final Path SNAPSHOT = Path.of("src/test/resources/osrs/combat/ava_metallic_interference.tsv");

    @Test
    void deviceRatesMatchTheWikiTable() {
        // Ava's device, Effectiveness: recovery 60 / 72 / 80 %, dropped 20 / 8 / 0 %, 20 % always breaks
        assertEquals(0, AmmoRecovery.Device.NONE.recoverPercent);
        assertEquals(80, AmmoRecovery.Device.NONE.dropPercent());
        assertEquals(60, AmmoRecovery.Device.ATTRACTOR.recoverPercent);
        assertEquals(20, AmmoRecovery.Device.ATTRACTOR.dropPercent());
        assertEquals(72, AmmoRecovery.Device.ACCUMULATOR.recoverPercent);
        assertEquals(8, AmmoRecovery.Device.ACCUMULATOR.dropPercent());
        assertEquals(80, AmmoRecovery.Device.ASSEMBLER.recoverPercent);
        assertEquals(0, AmmoRecovery.Device.ASSEMBLER.dropPercent());
    }

    @Test
    void outcomesOverAllRolls() {
        for (AmmoRecovery.Device d : AmmoRecovery.Device.values()) {
            int[] n = new int[3];
            for (int roll = 0; roll < 100; roll++) {
                n[AmmoRecovery.outcome(d, roll).ordinal()]++;
            }
            assertEquals(AmmoRecovery.BREAK_PERCENT, n[AmmoRecovery.Outcome.BROKE.ordinal()], d.name());
            assertEquals(d.dropPercent(), n[AmmoRecovery.Outcome.DROPPED.ordinal()], d.name());
            assertEquals(d.recoverPercent, n[AmmoRecovery.Outcome.RECOVERED.ordinal()], d.name());
        }
    }

    @Test
    void wornDevices() {
        assertEquals(AmmoRecovery.Device.ACCUMULATOR, AmmoRecovery.device("avas_accumulator", "black_dhide_body"));
        assertEquals(AmmoRecovery.Device.ATTRACTOR, AmmoRecovery.device("avas_attractor", null));
        assertEquals(AmmoRecovery.Device.ACCUMULATOR, AmmoRecovery.device("ranged_cape", "bronze_platebody"), "bronze is compatible");
        assertEquals(AmmoRecovery.Device.ACCUMULATOR, AmmoRecovery.device("max_cape", null), "the max cape saves ammo like an accumulator");
        assertEquals(AmmoRecovery.Device.NONE, AmmoRecovery.device("avas_accumulator", "rune_platebody"), "metallic interference");
        assertEquals(AmmoRecovery.Device.NONE, AmmoRecovery.device("fire_cape", null));
        assertEquals(AmmoRecovery.Device.NONE, AmmoRecovery.device(null, null));
        assertTrue(AmmoRecovery.recoverable("rune_arrow"));
        assertFalse(AmmoRecovery.recoverable("red_chinchompa"));
        assertFalse(AmmoRecovery.recoverable(null));
    }

    /** "Rune platebody (g)" -> rune_platebody_g, "Ahrim's robetop" -> ahrims_robetop; the decorative bodies have their own ids. */
    private static String id(String name) {
        Map<String, String> aliases = Map.of(
                "Decorative armour (gold platebody)", "gold_decorative_body",
                "Decorative armour (red platebody)", "red_decorative_body",
                "Decorative armour (white platebody)", "white_decorative_body");
        String target = name.contains("#") ? name.substring(0, name.indexOf('#')) : name;
        if (aliases.containsKey(target)) {
            return aliases.get(target);
        }
        return target.toLowerCase(Locale.ROOT).replace("'", "").replace("(", "").replace(")", "").trim().replaceAll("[\\s-]+", "_");
    }

    @Test
    void metallicInterferenceMatchesTheWikiTable() throws Exception {
        List<String[]> rows = new ArrayList<>();
        for (String line : Files.readAllLines(SNAPSHOT, StandardCharsets.UTF_8)) {
            if (!line.isBlank() && !line.startsWith("#")) {
                rows.add(line.split("\t"));
            }
        }
        assertTrue(rows.size() > 100, "the wiki table has " + rows.size() + " rows");
        int checked = 0;
        List<String> wrong = new ArrayList<>();
        for (String[] row : rows) {
            String id = id(row[1]);
            boolean known = ItemCatalog.get(id) != null || AmmoRecovery.INTERFERING.contains(id);
            if (!known || row[2].equals("unknown")) {
                continue;
            }
            checked++;
            boolean interferes = row[2].equals("interferes");
            if (interferes != AmmoRecovery.INTERFERING.contains(id)) {
                wrong.add(id + " (wiki: " + row[2] + ")");
            }
        }
        assertTrue(wrong.isEmpty(), "metallic interference differs from the wiki for " + wrong);
        assertTrue(checked >= 60, "only " + checked + " wiki rows matched items");
    }
}
