package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.world.SpawnPoint;
import com.runecraft.osrs.world.SpawnTable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The round 17 slayer task monsters against the wiki snapshot {@code osrs/slayer/task_monsters_r17.tsv}. */
class SlayerTaskMonsters2Test {
    private static final String FIXTURE = "osrs/slayer/task_monsters_r17.tsv";

    private static List<String[]> snapshot() throws IOException {
        Path p = Path.of("src/test/resources", FIXTURE);
        List<String> lines;
        if (Files.exists(p)) {
            lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        } else {
            try (InputStream in = SlayerTaskMonsters2Test.class.getClassLoader().getResourceAsStream(FIXTURE)) {
                assertNotNull(in, "missing " + FIXTURE);
                lines = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
            }
        }
        List<String[]> out = new ArrayList<>();
        for (String line : lines) {
            if (!line.isBlank() && !line.startsWith("#") && !line.startsWith("id\t")) {
                out.add(line.split("\t", -1));
            }
        }
        return out;
    }

    @Test
    void statsMatchTheWikiSnapshot() throws IOException {
        List<String[]> rows = snapshot();
        assertEquals(SlayerTaskMonsters2.all().size(), rows.size(), "every round 17 monster is in the snapshot");
        List<String> bad = new ArrayList<>();
        for (String[] f : rows) {
            MonsterDef m = ContentRegistry.monster(f[0]);
            if (m == null) {
                bad.add(f[0] + " missing");
                continue;
            }
            int maxHit = m.attacks.isEmpty() ? 0 : m.attacks.get(0).maxHit(); // the main (first) attack
            String want = String.join(" ", f[3], f[4], f[5], f[6], f[7], f[8], f[9], f[10], f[11], f[12]);
            String got = m.combatLevel + " " + m.hitpoints + " " + m.attack + " " + m.strength + " " + m.defence + " " + m.magic
                    + " " + m.ranged + " " + m.attackSpeed + " " + maxHit + " " + m.slayerLevel;
            if (!want.equals(got)) {
                bad.add(f[0] + ": wiki " + want + ", RuneCraft " + got);
            }
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }

    @Test
    void everyNewMonsterSpawnsSomewhere() {
        Set<String> placed = new HashSet<>();
        for (SpawnPoint p : SpawnTable.all()) {
            placed.add(p.monster());
        }
        for (SpawnTable.CampRef c : SpawnTable.camps()) {
            placed.add(c.creature());
        }
        for (MonsterDef m : SlayerTaskMonsters2.all()) {
            assertTrue(placed.contains(m.id), m.id + " never spawns");
        }
        assertTrue(placed.contains("bloodworm"), "the Barrows tunnels hold bloodworms");
    }

    @Test
    void tasksThatHadNoCreaturesAreAssignableNow() {
        for (String task : List.of("custodian_stalkers", "araxytes", "lesser_nagua", "vampyres", "crabs", "mutated_zygomites")) {
            assertTrue(SlayerTasks.available(task), task);
        }
        assertTrue(SlayerCategories.counts("spiders", "araxyte", "araxytes"), "araxytes also count as spiders");
        assertTrue(SlayerCategories.counts("ogres", "skogre", "skeletons"), "skogres also count as ogres");
    }

    @Test
    void lizardsAndWyrmlingsNeedNoFinisherOrBoots() {
        assertNull(SlayerGear.finisher("sulphur_lizard", "lizards"));
        assertNull(SlayerGear.finisher("grimy_lizard", "lizards"));
        assertNotNull(SlayerGear.finisher("desert_lizard", "lizards"));
        assertNotNull(SlayerGear.finisher("ancient_zygomite", "mutated_zygomites"));
        assertNull(SlayerGear.protection("wyrmling", "wyrms"));
        assertNotNull(SlayerGear.protection("wyrm", "wyrms"));
    }
}
