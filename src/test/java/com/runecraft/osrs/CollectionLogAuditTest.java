package com.runecraft.osrs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Collection log pages follow OSRS, every item exists and every item can actually be obtained. */
class CollectionLogAuditTest {
    /**
     * Files that only define or describe items (definitions, stats, trade rules, the log pages themselves):
     * a mention there is not a way to obtain the item.
     */
    private static boolean definitionFile(String name) {
        return name.endsWith("Items.java") || name.equals("ItemCatalog.java") || name.equals("EquipmentBonuses.java")
                || name.equals("Untradeables.java") || name.equals("CollectionLogData.java") || name.equals("OsrsPages.java")
                || name.equals("MinigameWorld.java") || name.equals("AmmoRules.java") || name.equals("Pets.java")
                || name.equals("SetEffects.java") || name.startsWith("Bestiary") || name.equals("Fallbacks.java");
    }

    /** Item ids that game code (reward sessions, skilling actions, scripts) hands out by literal id. */
    private static Set<String> codeSources() throws IOException {
        Set<String> out = new HashSet<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java/com/runecraft"))) {
            for (Path f : (Iterable<Path>) files::iterator) {
                String name = f.getFileName().toString();
                // Minecraft-side game code, plus the pure reward rules it rolls (e.g. MinigameRules' reward pools, ToaLoot's chest).
                boolean core = f.toString().replace('\\', '/').contains("/com/runecraft/osrs/");
                boolean rewardRules = name.endsWith("Rules.java") || name.endsWith("Rewards.java") || name.endsWith("Loot.java");
                if (!name.endsWith(".java") || definitionFile(name) || (core && !rewardRules)) {
                    continue;
                }
                String text = Files.readString(f);
                int i = 0;
                while ((i = text.indexOf('"', i)) >= 0) {
                    int j = text.indexOf('"', i + 1);
                    if (j < 0) {
                        break;
                    }
                    out.add(text.substring(i + 1, j));
                    i = j + 1;
                }
            }
        }
        return out;
    }

    /** Items whose game mechanic doesn't exist yet (none left). */
    static final Set<String> KNOWN_GAPS = Set.of();

    @Test
    void everyItemExistsAndHasASource() throws IOException {
        Set<String> sources = CollectionLogSources.all();
        Set<String> code = codeSources();
        List<String> missing = new ArrayList<>();
        Set<String> gapsSeen = new HashSet<>();
        for (CollectionLogData.Page p : CollectionLogData.pages()) {
            assertFalse(p.items().isEmpty(), p.name());
            for (String item : p.items()) {
                assertNotNull(ItemCatalog.get(item), p.name() + ": no item " + item);
                if (sources.contains(item) || code.contains(item)) {
                    continue;
                }
                if (KNOWN_GAPS.contains(item)) {
                    gapsSeen.add(item);
                } else {
                    missing.add(p.name() + ": " + item);
                }
            }
        }
        assertTrue(missing.isEmpty(), "collection log items nothing gives out:\n" + String.join("\n", missing));
        assertEquals(KNOWN_GAPS, gapsSeen, "a known gap now has a source (or left the log): update KNOWN_GAPS");
    }

    @Test
    void pagesFollowOsrs() {
        Map<String, CollectionLogData.Page> byName = new HashMap<>();
        Set<String> names = new HashSet<>();
        for (CollectionLogData.Page p : CollectionLogData.pages()) {
            assertTrue(names.add(p.category() + "/" + p.name()), "duplicate page " + p.name());
            byName.put(p.name(), p);
            assertTrue(List.of("Bosses", "Raids", "Clues", "Minigames", "Other", "Skilling").contains(p.category())
                    || p.category().equals("God Wars Dungeon") || p.category().equals("Wilderness"), p.category());
        }
        // Merged OSRS boss pages replace the per-form / per-brother pages.
        for (String gone : List.of("Ahrim the Blighted", "Dagannoth Rex", "Callisto", "Artio", "Dusk", "Crystalline Hunllef",
                "Phosani's Nightmare", "TzTok-Jad", "Herbs", "Gems", "Rune altars")) {
            assertFalse(byName.containsKey(gone), gone);
        }
        assertTrue(byName.get("Barrows Chests").items().size() >= 24);
        assertTrue(byName.get("Dagannoth Kings").items().containsAll(List.of("pet_dagannoth_rex", "berserker_ring", "seercull")));
        assertTrue(byName.get("General Graardor").items().containsAll(List.of("pet_general_graardor", "bandos_hilt", "godsword_shard_1")));
        assertTrue(byName.get("K'ril Tsutsaroth").items().containsAll(List.of("pet_kril_tsutsaroth", "zamorak_hilt")));
        assertFalse(byName.get("Kree'arra").items().contains("runite_ore"), "commodities are not log items");
        assertFalse(byName.get("Commander Zilyana").items().contains("diamond"));
        assertTrue(byName.get("The Fight Caves").items().containsAll(List.of("fire_cape", "tzrek_jad")));
        assertTrue(byName.get("Slayer").items().contains("imbued_heart"));
        // Every boss/raid/clue page has a kill count key.
        for (CollectionLogData.Page p : CollectionLogData.pages()) {
            if (p.category().equals("Bosses") || p.category().equals("Raids") || p.category().equals("Clues")
                    || p.category().equals("God Wars Dungeon") || p.category().equals("Wilderness")) {
                assertFalse(CollectionLogData.kcKeys(p).isEmpty(), "no kill count for " + p.name());
            }
        }
        CollectionLogData.Page dks = byName.get("Dagannoth Kings");
        assertEquals(3, CollectionLogData.killCount(dks, Map.of("dagannoth_rex", 1, "dagannoth_prime", 2, "kalphite_queen", 5)));
        assertEquals(4, CollectionLogData.killCount(byName.get("Hard Treasure Trails"), Map.of("clue_hard", 4)));
        assertEquals(7, CollectionLogData.killCount(byName.get("Barrows Chests"), Map.of("barrows_chests", 7, "ahrim", 3)));
        int total = 0;
        Set<String> unique = new HashSet<>();
        for (CollectionLogData.Page p : CollectionLogData.pages()) {
            unique.addAll(p.items());
            total++;
        }
        assertEquals(unique.size(), CollectionLogData.totalSlots());
        assertTrue(total > 50);
    }
}
