package com.runecraft.osrs;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.data.Boss;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boss collection log pages hold exactly the wiki's page contents (the items RuneCraft has), whatever the boss
 * drop tables say: {@code osrs/bosses/collection_log_pages.tsv} is the allowlist, so patching common wiki drops
 * into a boss table ({@link com.runecraft.osrs.data.BossDropsTable}) never adds log slots.
 */
class CollectionLogSnapshotTest {
    private static final Path SNAPSHOT = Path.of("src/test/resources/osrs/bosses/collection_log_pages.tsv");

    /** The OSRS Bosses tab, plus the boss-like pages of the Other tab handled here. */
    static final List<String> BOSS_PAGES = List.of("Abyssal Sire", "Alchemical Hydra", "Amoxliatl", "Araxxor", "Barrows Chests",
            "Brutus", "Bryophyta", "Callisto and Artio", "Cerberus", "Chaos Elemental", "Chaos Fanatic", "Commander Zilyana",
            "Corporeal Beast", "Crazy Archaeologist", "Dagannoth Kings", "Deranged archaeologist", "Doom of Mokhaiotl",
            "Duke Sucellus", "The Fight Caves", "Fortis Colosseum", "The Gauntlet", "General Graardor", "Giant Mole",
            "Grotesque Guardians", "Hespori", "The Hueycoatl", "The Inferno", "Kalphite Queen", "King Black Dragon", "Kraken",
            "Kree'arra", "K'ril Tsutsaroth", "The Leviathan", "Maggot King", "Moons of Peril", "Nex", "The Nightmare", "Obor",
            "Phantom Muspah", "Royal Titans", "Sarachnis", "Scorpia", "Scurrius", "Shellbane Gryphon", "Skotizo",
            "Thermonuclear Smoke Devil", "Vardorvis", "Venenatis and Spindel", "Vet'ion and Calvar'ion", "Vorkath",
            "The Whisperer", "Yama", "Zalcano", "Zulrah", "Revenants", "Tormented Demons");

    static Map<String, Set<String>> wikiPages() throws IOException {
        Map<String, Set<String>> out = new LinkedHashMap<>();
        for (String line : Files.readAllLines(SNAPSHOT)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\\|", -1);
            assertEquals(4, f.length, line);
            Set<String> items = out.computeIfAbsent(f[0], k -> new TreeSet<>());
            if (!f[3].equals("-")) {
                items.add(f[3]);
            }
        }
        return out;
    }

    @Test
    void bossPagesMatchTheWiki() throws IOException {
        Map<String, Set<String>> wiki = wikiPages();
        Map<String, CollectionLogData.Page> ours = new LinkedHashMap<>();
        for (CollectionLogData.Page p : CollectionLogData.pages()) {
            ours.put(p.name(), p);
        }
        List<String> wrong = new ArrayList<>();
        for (String name : BOSS_PAGES) {
            Set<String> want = wiki.get(name);
            assertTrue(want != null, "snapshot has no page " + name);
            CollectionLogData.Page page = ours.get(name);
            if (page == null) {
                wrong.add("no page " + name);
                continue;
            }
            Set<String> have = new TreeSet<>(page.items());
            if (!have.equals(want)) {
                Set<String> extra = new TreeSet<>(have);
                extra.removeAll(want);
                Set<String> lacking = new TreeSet<>(want);
                lacking.removeAll(have);
                wrong.add(name + ": extra " + extra + ", lacking " + lacking);
            }
        }
        assertTrue(wrong.isEmpty(), String.join("\n", wrong));
    }

    /** Every page built from a boss's drops is an OSRS page (merged or renamed to the wiki's name). */
    @Test
    void noGeneratedBossPagesOutsideTheWiki() throws IOException {
        Map<String, Set<String>> wiki = wikiPages();
        Set<String> bossNames = new TreeSet<>();
        for (Boss b : Boss.values()) {
            bossNames.add(b.displayName());
        }
        for (BossDef b : ContentRegistry.bosses()) {
            if (b.category.equals("Bosses") || b.category.equals("God Wars Dungeon") || b.category.equals("Wilderness")) {
                bossNames.add(b.name);
            }
        }
        List<String> stray = new ArrayList<>();
        for (CollectionLogData.Page p : CollectionLogData.pages()) {
            if (bossNames.contains(p.name()) && !wiki.containsKey(p.name())) {
                stray.add(p.name());
            }
        }
        assertTrue(stray.isEmpty(), "boss pages OSRS doesn't have: " + stray);
    }
}
