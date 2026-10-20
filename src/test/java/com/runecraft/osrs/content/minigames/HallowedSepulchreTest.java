package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.content.minigames.HallowedSepulchre.Kind;
import com.runecraft.osrs.content.minigames.HallowedSepulchre.Layout;
import com.runecraft.osrs.content.minigames.HallowedSepulchre.Route;
import com.runecraft.osrs.content.minigames.HallowedSepulchre.Section;
import com.runecraft.osrs.content.minigames.HallowedSepulchre.Tier;
import com.runecraft.osrs.content.minigames.HallowedSepulchre.Tool;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Hallowed Sepulchre against the OSRS Wiki (July 2026 dump): floor levels and XP ("Magical Obelisk", "Stairs"), obstacle
 * XP, the timer, coffin marks and table odds, the three coffin loot tables diffed against
 * {@code src/test/resources/osrs/minigames/sepulchre_coffins.tsv}, the routes, and every trap corridor being crossable at
 * OSRS run speed whatever its random seed and whenever the player arrives.
 */
class HallowedSepulchreTest {
    /** Wiki item names to mod item ids (and potion doses). */
    private static final Map<String, String> IDS = Map.ofEntries(Map.entry("Monk's robe top", "monks_robe_top"),
            Map.entry("Monk's robe", "monks_robe"), Map.entry("Holy symbol", "holy_symbol"), Map.entry("Air rune", "air_rune"),
            Map.entry("Fire rune", "fire_rune"), Map.entry("Chaos rune", "chaos_rune"), Map.entry("Mithril bolts", "mithril_bolts"),
            Map.entry("Prayer potion(2)", "prayer_potion@2"), Map.entry("White lily", "white_lily"), Map.entry("Coins", "coins"),
            Map.entry("Adamant 2h sword", "adamant_2h_sword"), Map.entry("Adamant platebody", "adamant_platebody"),
            Map.entry("Cosmic rune", "cosmic_rune"), Map.entry("Death rune", "death_rune"), Map.entry("Nature rune", "nature_rune"),
            Map.entry("Adamant bolts", "adamant_bolts"), Map.entry("Monkfish", "monkfish"), Map.entry("Prayer potion(4)", "prayer_potion@4"),
            Map.entry("Grimy ranarr weed", "grimy_ranarr_weed"), Map.entry("Rune 2h sword", "rune_2h_sword"),
            Map.entry("Rune platebody", "rune_platebody"), Map.entry("Law rune", "law_rune"), Map.entry("Blood rune", "blood_rune"),
            Map.entry("Soul rune", "soul_rune"), Map.entry("Runite bolts", "rune_bolts"), Map.entry("Sanfew serum(4)", "sanfew_serum@4"),
            Map.entry("Ranarr seed", "ranarr_seed"));

    private static String key(HallowedSepulchre.Loot l) {
        return (l.doses() > 0 ? l.item() + "@" + l.doses() : l.item()) + " " + l.min() + "-" + l.max();
    }

    @Test
    void coffinTablesMatchTheWiki() throws Exception {
        List<String> lines = Files.readAllLines(Path.of("src/test/resources/osrs/minigames/sepulchre_coffins.tsv"), StandardCharsets.UTF_8);
        Map<String, List<String>> wiki = Map.of("low", new ArrayList<>(), "mid", new ArrayList<>(), "high", new ArrayList<>());
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] r = line.split("\t");
            assertEquals("1/10", r[4], line);
            String id = IDS.get(r[1]);
            assertNotNull(id, "unmapped wiki item " + r[1]);
            wiki.get(r[0]).add(id + " " + r[2] + "-" + r[3]);
        }
        for (Tier t : Tier.values()) {
            List<String> mod = new ArrayList<>();
            HallowedSepulchre.table(t).forEach(l -> mod.add(key(l)));
            assertEquals(wiki.get(t.name().toLowerCase()), mod, t + " table");
        }
    }

    /** The agility minigames' collection log pages hold the wiki's items (osrs/bosses/collection_log_pages.tsv). */
    @Test
    void collectionLogPagesMatchTheWiki() throws Exception {
        List<String> lines = Files.readAllLines(Path.of("src/test/resources/osrs/bosses/collection_log_pages.tsv"), StandardCharsets.UTF_8);
        for (String page : List.of("Hallowed Sepulchre", "Rogues' Den", "Brimhaven Agility Arena")) {
            java.util.Set<String> wiki = new java.util.TreeSet<>();
            for (String line : lines) {
                String[] r = line.split("\\|");
                if (r.length == 4 && r[0].equals(page) && !r[3].equals("-")) {
                    wiki.add(r[3]);
                }
            }
            com.runecraft.osrs.CollectionLogData.Page ours = MinigameWorld.collectionLog().stream()
                    .filter(p -> p.name().equals(page)).findFirst().orElseThrow();
            assertEquals(wiki, new java.util.TreeSet<>(ours.items()), page);
        }
    }

    @Test
    void floorsAndExperience() {
        assertArrayEquals(new int[]{52, 62, 72, 82, 92}, HallowedSepulchre.FLOOR_LEVELS);
        assertArrayEquals(new double[]{500, 850, 1425, 2625, 5850}, HallowedSepulchre.FLOOR_XP);
        assertEquals(25, HallowedSepulchre.MIDDLE_STAIRS_XP);
        assertArrayEquals(new double[]{50, 50, 75, 75, 80}, HallowedSepulchre.CROSSING_XP);
        assertEquals(200, HallowedSepulchre.START_TICKS, "2 minutes on floor 1");
        assertEquals(200, HallowedSepulchre.FLOOR_TICKS, "2 more minutes per floor");
        assertEquals(100, HallowedSepulchre.TOKEN_TICKS, "a hallowed token adds a minute");
        assertEquals(66, HallowedSepulchre.COFFIN_THIEVING);
        assertEquals(200, HallowedSepulchre.COFFIN_XP);
    }

    @Test
    void marksTiersAndRates() {
        int[][] expected = {{1, 1}, {2, 3}, {3, 5}, {3, 6}, {4, 6}};
        for (int f = 1; f <= 5; f++) {
            assertEquals(expected[f - 1][0], HallowedSepulchre.marks(f, 0.0));
            assertEquals(expected[f - 1][1], HallowedSepulchre.marks(f, 0.9999));
            int[] p = HallowedSepulchre.TIER_PERCENT[f - 1];
            assertEquals(100, p[0] + p[1] + p[2], "floor " + f);
        }
        assertEquals(8, HallowedSepulchre.grandMarks(0.0));
        assertEquals(10, HallowedSepulchre.grandMarks(0.9999));
        assertEquals(Tier.LOW, HallowedSepulchre.tier(1, 0.59));
        assertEquals(Tier.MID, HallowedSepulchre.tier(1, 0.60));
        assertEquals(Tier.HIGH, HallowedSepulchre.tier(1, 0.96));
        assertEquals(Tier.MID, HallowedSepulchre.tier(4, 0.0), "no low-tier table from floor 4");
        assertEquals(Tier.HIGH, HallowedSepulchre.tier(5, 0.2));
        assertArrayEquals(new int[]{200, 120, 90, 60, 40}, HallowedSepulchre.LOCKPICK_RATE);
        assertArrayEquals(new int[]{50, 50, 25, 50, 60}, HallowedSepulchre.CLUE_RATE);
        assertEquals("clue_scroll_easy", HallowedSepulchre.CLUES[0]);
        assertEquals("clue_scroll_elite", HallowedSepulchre.CLUES[4]);
        assertEquals(30, HallowedSepulchre.GRAND_CLUE_RATE);
        assertEquals(200, HallowedSepulchre.RING_RATE);
        assertArrayEquals(new int[]{35_000, 16_000, 8_000, 4_000, 2_000}, HallowedSepulchre.PET_RATE);
        assertArrayEquals(new int[]{5, 1, 2}, HallowedSepulchre.SACK_ROLLS);
        assertEquals(5, HallowedSepulchre.PAGES.length);
    }

    @Test
    void coffinSuccessFollowsTheSkillingChart() {
        assertEquals(0, HallowedSepulchre.coffinChance(65, Tool.STRANGE_OLD_LOCKPICK));
        double none = HallowedSepulchre.coffinChance(66, Tool.NONE);
        double pick = HallowedSepulchre.coffinChance(66, Tool.LOCKPICK);
        double old = HallowedSepulchre.coffinChance(66, Tool.STRANGE_OLD_LOCKPICK);
        assertEquals(107 / 256.0, none, 1e-9);
        assertTrue(none < pick && pick < old);
        assertEquals(191 / 256.0, HallowedSepulchre.coffinChance(99, Tool.NONE), 1e-9);
    }

    @Test
    void routesFollowTheFloorGuides() {
        assertEquals(4, HallowedSepulchre.routes(1).size());
        assertEquals(4, HallowedSepulchre.routes(2).size());
        assertEquals(2, HallowedSepulchre.routes(3).size());
        assertEquals(2, HallowedSepulchre.routes(4).size());
        assertEquals(1, HallowedSepulchre.routes(5).size());
        for (int f = 1; f <= 5; f++) {
            for (Route r : HallowedSepulchre.routes(f)) {
                long coffins = r.sections().stream().filter(s -> s.kind() == Kind.COFFIN).count();
                long platforms = r.sections().stream().filter(s -> s.kind() == Kind.PLATFORM).count();
                long stairs = r.sections().stream().filter(s -> s.kind() == Kind.STAIRS).count();
                assertTrue(coffins >= 1, "floor " + f + " " + r.entrance());
                assertEquals(f == 4 ? 0 : 1, platforms, "no platforms on floor 4");
                assertEquals(f == 5 ? 2 : 1, stairs, "floor " + f + " " + r.entrance());
                for (Section s : r.sections()) {
                    if (s.kind() == Kind.COFFIN) {
                        assertNotNull(s.challenge());
                    }
                    if (s.isTrap()) {
                        assertTrue(!s.traps().isEmpty());
                    }
                }
            }
        }
        List<Section> five = HallowedSepulchre.routes(5).get(0).sections();
        assertEquals(Kind.GRAND_COFFIN, five.get(five.size() - 1).kind());
        assertTrue(five.stream().anyMatch(s -> s.kind() == Kind.COFFIN && s.rolls() == 2), "floor 5's bridge coffin rolls twice");
        assertTrue(HallowedSepulchre.routes(1).stream().allMatch(r -> r.sections().stream()
                .noneMatch(s -> s.traps().contains(HallowedSepulchre.Trap.LIGHTNING))), "priest statues from floor 4");
        assertEquals(1, HallowedSepulchre.tier(HallowedSepulchre.Trap.ARROW, 2, true));
        assertEquals(2, HallowedSepulchre.tier(HallowedSepulchre.Trap.ARROW, 3, false));
        assertEquals(3, HallowedSepulchre.tier(HallowedSepulchre.Trap.SWORD, 4, true));
        assertEquals(2, HallowedSepulchre.tier(HallowedSepulchre.Trap.FLAME, 5, false));
    }

    @Test
    void everyTrapCorridorCanBeCrossedAtRunSpeed() {
        int checked = 0;
        for (int f = 1; f <= 5; f++) {
            for (Route r : HallowedSepulchre.routes(f)) {
                boolean below = false;
                for (Section s : r.sections()) {
                    if (s.kind() == Kind.STAIRS) {
                        below = true;
                    }
                    if (!s.isTrap()) {
                        continue;
                    }
                    for (long seed = 0; seed < 12; seed++) {
                        Layout l = HallowedSepulchre.layout(s, f, below, seed * 7919 + f);
                        for (long tick = 0; tick < 24; tick += 3) {
                            List<HallowedSepulchre.Step> plan = HallowedSepulchre.plan(l, 0, -1, tick, 2, 80);
                            assertNotNull(plan, "floor " + f + " " + r.entrance() + " " + s.traps() + " seed " + seed + " tick " + tick);
                            assertEquals(HallowedSepulchre.LEN, plan.get(plan.size() - 1).z());
                            checked++;
                        }
                    }
                }
            }
        }
        assertTrue(checked > 1000, checked + " crossings");
    }

    @Test
    void trapsActuallyBite() {
        Section flames = new Section(Kind.TRAPS, List.of(HallowedSepulchre.Trap.FLAME), null, 0);
        Layout l = HallowedSepulchre.layout(flames, 1, false, 1);
        int burning = 0;
        for (long t = 0; t < 6; t++) {
            if (HallowedSepulchre.hazard(l, 0, 5, 5, t) == HallowedSepulchre.Hazard.HIT) {
                burning++;
            }
        }
        assertEquals(2, burning, "a tier-1 flame row burns 2 ticks in 6");
        assertEquals(HallowedSepulchre.Hazard.NONE, HallowedSepulchre.hazard(l, 0, 4, 4, 3), "between the flames is safe");
        Section sword = new Section(Kind.TRAPS, List.of(HallowedSepulchre.Trap.SWORD), null, 0);
        Layout s = HallowedSepulchre.layout(sword, 1, false, 1);
        boolean hit = false;
        for (long t = 0; t < 20; t++) {
            hit |= HallowedSepulchre.hazard(s, 0, 6, 6, t) == HallowedSepulchre.Hazard.HIT;
        }
        assertTrue(hit, "standing in a sword corridor gets you hit");
        Section bolts = new Section(Kind.TRAPS, List.of(HallowedSepulchre.Trap.ARROW), null, 0);
        Layout b = HallowedSepulchre.layout(bolts, 5, true, 3);
        int lanesHit = 0;
        for (int lane = -1; lane <= 1; lane++) {
            for (long t = 0; t < 12; t++) {
                if (HallowedSepulchre.hazard(b, lane, 5, 5, t) == HallowedSepulchre.Hazard.HIT) {
                    lanesHit++;
                    break;
                }
            }
        }
        assertTrue(lanesHit >= 2, "bolts fly down the lanes");
    }
}
