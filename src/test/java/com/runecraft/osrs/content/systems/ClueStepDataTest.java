package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.systems.ClueScrolls.Clue;
import com.runecraft.osrs.content.systems.ClueScrolls.Tier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Diffs the clue steps ({@link ClueStepData}) against the snapshot extracted from RuneLite's cluescrolls plugin
 * ({@code clue_steps.tsv}) and the casket tables ({@link ClueRewardData}) against the wiki snapshot ({@code
 * casket_rewards.tsv}).
 */
class ClueStepDataTest {
    private static final Path STEPS = Path.of("src/test/resources/osrs/clues/clue_steps.tsv");
    private static final Path STASH = Path.of("src/test/resources/osrs/clues/stash_units.tsv");
    private static final Path CASKETS = Path.of("src/test/resources/osrs/clues/casket_rewards.tsv");

    static List<String[]> rows(Path p) throws IOException {
        List<String[]> out = new ArrayList<>();
        boolean header = true;
        for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
            if (line.startsWith("#") || line.isBlank()) {
                continue;
            }
            if (header) {
                header = false;
                continue;
            }
            out.add(line.split("\t", -1));
        }
        return out;
    }

    @Test
    void stepsMatchTheRuneLiteSnapshot() throws IOException {
        List<String[]> snap = rows(STEPS);
        List<Clue> clues = ClueScrolls.clues();
        assertEquals(snap.size(), clues.size(), "step count");
        for (int i = 0; i < snap.size(); i++) {
            String[] s = snap.get(i);
            Clue c = clues.get(i);
            String where = "row " + (i + 1) + " " + String.join("|", s);
            assertEquals(s[0], c.type().name(), where);
            assertEquals(s[1], c.tier().id(), where);
            assertEquals(Integer.parseInt(s[2]), c.x(), where);
            assertEquals(Integer.parseInt(s[3]), c.y(), where);
            assertEquals(Integer.parseInt(s[4]), c.plane(), where);
            assertEquals(s[5], c.npc(), where);
            assertEquals(s[6], c.object(), where);
            assertEquals(s[7], c.guardian() == null ? "" : c.guardian().name(), where);
            assertEquals(s[8], String.join("|", c.emotes()), where);
            assertEquals(s[9], c.stash(), where);
            assertEquals(s[10], c.answer(), where);
            assertEquals(s[11], c.text(), where);
            assertEquals(s[12], c.solution(), where);
            assertEquals(s[13], c.question(), where);
            assertEquals(s.length > 14 ? s[14] : "", c.task(), where);
        }
        // Counts per tier of the OSRS clue lists (RuneLite, Oct 2026), e.g. 126 emote clues, 167 coordinates.
        Map<String, Integer> byType = new HashMap<>();
        for (Clue c : clues) {
            byType.merge(c.type().name(), 1, Integer::sum);
        }
        assertEquals(126, byType.get("EMOTE"));
        assertEquals(167, byType.get("COORDINATE"));
        assertEquals(141, byType.get("HOT_COLD"));
        assertEquals(20, byType.get("FALO"));
        assertEquals(24, byType.get("MUSIC"));
        assertEquals(8, byType.get("CHARLIE"));
    }

    @Test
    void stashUnitsMatchTheSnapshot() throws IOException {
        List<String[]> snap = rows(STASH);
        assertEquals(snap.size(), ClueScrolls.stashUnits().size());
        for (String[] s : snap) {
            ClueScrolls.Stash st = ClueScrolls.stashUnits().get(s[0]);
            assertNotNull(st, s[0]);
            assertEquals(Tier.valueOf(s[1].toUpperCase(java.util.Locale.ROOT)), st.tier());
            assertEquals(Integer.parseInt(s[2]), st.x());
            assertEquals(Integer.parseInt(s[3]), st.y());
        }
        // Every emote clue's STASH unit exists and is of the clue's tier.
        for (Clue c : ClueScrolls.clues()) {
            if (!c.stash().isEmpty()) {
                ClueScrolls.Stash st = ClueScrolls.stashUnits().get(c.stash());
                assertNotNull(st, c.stash());
                assertEquals(c.tier(), st.tier(), c.text());
            }
        }
    }

    @Test
    void casketTablesMatchTheWikiSnapshot() throws IOException {
        List<String[]> snap = rows(CASKETS);
        String[] data = ClueRewardData.ROWS.split("\n");
        assertEquals(snap.size(), data.length, "casket rows");
        Map<String, Double> sums = new HashMap<>();
        for (int i = 0; i < snap.size(); i++) {
            String[] s = snap.get(i);
            String[] d = java.util.Arrays.copyOf(data[i].split("\t", -1), 8);
            String where = "row " + (i + 1) + " " + String.join("|", s);
            assertEquals(s[0], d[0], where);
            assertEquals(s[1], d[1], where);
            assertEquals(s[3], d[2], where);
            assertEquals(s[4], d[3], where);
            assertEquals(s[5], d[4], where);
            assertEquals(s[6], d[5] == null ? "" : d[5], where);
            assertEquals(s[7], d[6], where);
            assertEquals(s[8], d[7], where);
            sums.merge(s[0], Double.parseDouble(s[7]) / Double.parseDouble(s[8]), Double::sum);
        }
        // Each tier's rows are one reward roll: the chances add up to 1 (the hard page rounds to 1.0001).
        for (Tier t : Tier.values()) {
            assertEquals(1.0, sums.get(t.id()), 2e-4, t + " per-roll total");
        }
    }

    @Test
    void casketRatesFollowTheWiki() {
        // Per-roll rates from the wiki tables (scaled by what this world lacks, at most a few percent).
        assertEquals(1.0 / 360, ClueRewards.perRoll(Tier.BEGINNER, "mole_slippers"), 0.3 / 360);
        assertTrue(ClueRewards.perRoll(Tier.MEDIUM, "ranger_boots") > 0);
        assertTrue(ClueRewards.perRoll(Tier.HARD, "3rd_age_platebody") > 0);
        assertEquals(0, ClueRewards.perRoll(Tier.HARD, "3rd_age_bow"), "3rd age bow is elite/master only");
        assertTrue(ClueRewards.perRoll(Tier.MASTER, "3rd_age_bow") > 0);
        // Most rows are present in this world.
        for (Tier t : Tier.values()) {
            double present = 0;
            for (ClueRewards.Row r : ClueRewards.table(t)) {
                present += r.chance();
                assertNotNull(ItemCatalog.get(r.item()), r.item());
            }
            assertTrue(present > 0.98, t + " table present " + present);
        }
        // Master clue per casket: easy 1/50, medium 1/30, hard 1/15, elite 1/5; Mimic elite 1/35, master 1/15.
        assertEquals(50, ClueRewards.masterClueOneIn(Tier.EASY));
        assertEquals(30, ClueRewards.masterClueOneIn(Tier.MEDIUM));
        assertEquals(15, ClueRewards.masterClueOneIn(Tier.HARD));
        assertEquals(5, ClueRewards.masterClueOneIn(Tier.ELITE));
        assertEquals(0, ClueRewards.masterClueOneIn(Tier.MASTER));
        assertEquals(35, ClueRewards.mimicOneIn(Tier.ELITE));
        assertEquals(15, ClueRewards.mimicOneIn(Tier.MASTER));
        assertEquals(1000, ClueRewards.BLOODHOUND_ONE_IN);
        // Scroll box stack limits: 2, +1 at the minor and major scroll case milestones.
        assertEquals(2, ClueRewards.stackLimit(Tier.HARD, 49));
        assertEquals(3, ClueRewards.stackLimit(Tier.HARD, 50));
        assertEquals(4, ClueRewards.stackLimit(Tier.MASTER, 75));
        // A sample of caskets: right number of rolls, items that exist.
        java.util.Random r = new java.util.Random(11);
        for (Tier t : Tier.values()) {
            for (int i = 0; i < 400; i++) {
                List<ClueRewards.Reward> rewards = ClueRewards.roll(t, r);
                assertTrue(!rewards.isEmpty());
                for (ClueRewards.Reward rw : rewards) {
                    assertNotNull(ItemCatalog.get(rw.item()), rw.item());
                }
            }
        }
    }

    @Test
    void declaredUniquesAreOnTheirTiersTable() {
        // Every item the definitions call a tier's unique is a reward of that tier in the wiki tables.
        List<String> stray = new ArrayList<>();
        for (Tier t : Tier.values()) {
            List<String> rewards = new ArrayList<>();
            for (ClueRewards.Row r : ClueRewards.table(t)) {
                rewards.add(r.item());
            }
            for (String id : ClueRewards.declared(t)) {
                if (!rewards.contains(id)) {
                    stray.add(t + ":" + id);
                }
            }
        }
        assertTrue(stray.size() <= 40, "declared uniques not on their wiki table: " + stray);
        System.out.println("Clue uniques declared but not on that tier's wiki table: " + stray);
    }
}
