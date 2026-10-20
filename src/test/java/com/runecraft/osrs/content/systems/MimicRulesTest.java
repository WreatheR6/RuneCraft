package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.systems.ClueScrolls.Tier;
import com.runecraft.osrs.data.Drop;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Mimic against the wiki snapshot ({@code mimic_rewards.tsv}, from the OSRS Wiki "The Mimic" page): the pre-roll per
 * tier and attempt, the main drops' quantities after failed attempts, the attempts a casket gives, the one-time guaranteed
 * Mimic, the mimic casket's saved state and the Mimic scroll case.
 */
class MimicRulesTest {
    private static final Path SNAPSHOT = Path.of("src/test/resources/osrs/clues/mimic_rewards.tsv");

    private record Row(Tier tier, String kind, String item, double rarity, double altRarity, int[] quantities) {
    }

    private static List<Row> rows() throws IOException {
        List<Row> out = new ArrayList<>();
        for (String line : Files.readAllLines(SNAPSHOT, StandardCharsets.UTF_8)) {
            if (line.startsWith("#") || line.startsWith("tier\t") || line.isBlank()) {
                continue;
            }
            String[] f = line.split("\t", -1);
            int[] q = f[5].isEmpty() ? new int[0] : java.util.Arrays.stream(f[5].split(",")).mapToInt(Integer::parseInt).toArray();
            out.add(new Row(Tier.valueOf(f[0].toUpperCase(Locale.ROOT)), f[1], f[2], fraction(f[3]), fraction(f[4]), q));
        }
        return out;
    }

    private static double fraction(String s) {
        String[] p = s.split("/");
        return Double.parseDouble(p[0]) / Double.parseDouble(p[1]);
    }

    private static String id(String name) {
        return name.toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    @Test
    void preRollMatchesTheWiki() throws IOException {
        int[] pieces = new int[Tier.values().length];
        for (Row r : rows()) {
            if (!r.kind().equals("preroll")) {
                continue;
            }
            double first = 1.0 / MimicRewards.table(r.tier(), 1);
            double later = 1.0 / MimicRewards.table(r.tier(), 2);
            if (id(r.item()).equals(MimicRewards.RING)) {
                assertEquals(r.rarity(), MimicRewards.RING_SLOTS * first, 1e-12, r.tier() + " ring");
                assertEquals(r.altRarity(), MimicRewards.RING_SLOTS * later, 1e-12, r.tier() + " ring, later attempts");
            } else {
                assertTrue(MimicRewards.THIRD_AGE.contains(id(r.item())), r.item() + " missing from the pre-roll");
                assertNotNull(ItemCatalog.get(id(r.item())), r.item());
                int n = MimicRewards.THIRD_AGE.size();
                assertEquals(r.rarity(), first / n, 1e-12, r.tier() + " " + r.item());
                assertEquals(r.altRarity(), later / n, 1e-12, r.tier() + " " + r.item() + ", later attempts");
                pieces[r.tier().ordinal()]++;
            }
        }
        assertEquals(MimicRewards.THIRD_AGE.size(), pieces[Tier.MASTER.ordinal()]);
        assertEquals(MimicRewards.THIRD_AGE.size(), pieces[Tier.ELITE.ordinal()]);
        // Sampled: an elite Mimic killed on its second attempt gives the ring 6/264 of the time.
        Random random = new Random(5);
        int rings = 0;
        for (int i = 0; i < 264_000; i++) {
            if (MimicRewards.RING.equals(MimicRewards.roll(Tier.ELITE, 2, random))) {
                rings++;
            }
        }
        assertTrue(rings > 5500 && rings < 6500, "6/264: " + rings);
    }

    @Test
    void mainDropsShrinkWithEachFailedAttempt() throws IOException {
        BossDef mimic = ContentRegistry.boss(MimicRewards.MIMIC);
        assertNotNull(mimic);
        int mains = 0;
        for (Row r : rows()) {
            if (!r.kind().equals("main")) {
                continue;
            }
            assertEquals(MimicRewards.attempts(r.tier()), r.quantities().length, r.tier() + " attempts");
            Drop line = null;
            for (Drop d : mimic.drops) {
                if (d.item().equals(id(r.item()))) {
                    line = d;
                }
            }
            assertNotNull(line, r.item() + " not on the Mimic's table");
            assertEquals(r.quantities()[0], line.max(), r.item() + " maximum");
            for (int a = 1; a <= r.quantities().length; a++) {
                assertEquals(r.quantities()[a - 1], MimicRewards.quantity(r.tier(), line.max(), a), r.tier() + " " + r.item()
                        + " on attempt " + a);
            }
            // The main table is what the pre-roll leaves: (table - 7) / table over its rows, evenly.
            int rows = 5;
            assertEquals(r.rarity(), (MimicRewards.table(r.tier(), 1) - 7.0) / MimicRewards.table(r.tier(), 1) / rows, 1e-12);
            assertEquals(r.altRarity(), (MimicRewards.table(r.tier(), 2) - 7.0) / MimicRewards.table(r.tier(), 2) / rows, 1e-12);
            mains++;
        }
        assertEquals(10, mains);
        assertNull(mimic.key, "the mimic casket is kept between attempts, not used up as a key");
    }

    @Test
    void mimicCasketState() {
        MimicRewards.Casket c = new MimicRewards.Casket(Tier.MASTER, 6, 0);
        assertEquals(c, MimicRewards.Casket.decode(c.encode()));
        assertEquals(6, c.nextAttempt().rollsNow(), "first attempt: every roll");
        assertEquals(5, c.nextAttempt().nextAttempt().rollsNow(), "one roll fewer per failed attempt");
        MimicRewards.Casket last = c;
        for (int i = 0; i < 6; i++) {
            assertFalse(last.exhausted());
            last = last.nextAttempt();
        }
        assertTrue(last.exhausted(), "six tries from a master casket");
        assertTrue(new MimicRewards.Casket(Tier.ELITE, 4, 5).exhausted(), "five from an elite one");
        assertEquals(0, new MimicRewards.Casket(Tier.ELITE, 4, 5).rollsNow());
        assertEquals(new MimicRewards.Casket(Tier.MASTER, Tier.MASTER.minRolls(), 0), MimicRewards.Casket.decode("MASTER"));
        assertNull(MimicRewards.Casket.decode(""));
        assertNull(MimicRewards.Casket.decode("HARD|4|0"), "only elite and master caskets are Mimics");
        assertNull(MimicRewards.Casket.decode("MASTER|x|1"));
    }

    @Test
    void guaranteedMimicAndScrollCase() {
        // OSRS Wiki: the 10th master or 25th elite casket, once, if no Mimic has been met.
        assertFalse(MimicRewards.guaranteed(Tier.MASTER, 9, false));
        assertTrue(MimicRewards.guaranteed(Tier.MASTER, 10, false));
        assertTrue(MimicRewards.guaranteed(Tier.MASTER, 40, false), "already past the threshold: the next casket");
        assertFalse(MimicRewards.guaranteed(Tier.MASTER, 10, true));
        assertFalse(MimicRewards.guaranteed(Tier.ELITE, 24, false));
        assertTrue(MimicRewards.guaranteed(Tier.ELITE, 25, false));
        assertFalse(MimicRewards.guaranteed(Tier.HARD, 500, false));
        // Scroll box stack limit: 2, +1 minor and major scroll cases, +1 Mimic scroll case for every tier (at most 5).
        for (Tier t : Tier.values()) {
            assertEquals(ClueRewards.stackLimit(t, 0) + 1, ClueRewards.stackLimit(t, 0, true));
            assertEquals(5, ClueRewards.stackLimit(t, 1000, true), t + " maximum");
        }
        // A casket's rolls: the tier's range; a Mimic's casket with no rolls left still rolls its tertiaries only.
        Random random = new Random(9);
        for (int i = 0; i < 200; i++) {
            int n = ClueRewards.rollCount(Tier.MASTER, random);
            assertTrue(n >= 5 && n <= 7, "master rolls " + n);
        }
        for (ClueRewards.Reward r : ClueRewards.roll(Tier.MASTER, 0, random)) {
            assertTrue(r.item().equals("bloodhound"), r.item());
        }
    }
}
