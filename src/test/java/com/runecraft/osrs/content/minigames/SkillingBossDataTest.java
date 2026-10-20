package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Skill;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round 38: the skilling bosses against the OSRS Wiki snapshots in {@code src/test/resources/osrs/minigames/}
 * (wintertodt_reward_cart.tsv, tempoross_casket.tsv, gotr_intricate_pouch.tsv) and the wiki rules behind the Wintertodt
 * reward cart, the Tempoross reward pool and storm, Guardians of the Rift's cells, remains and portal, and the Volcanic Mine.
 */
class SkillingBossDataTest {
    private static List<String[]> rows(String file) throws Exception {
        List<String[]> out = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of("src/test/resources/osrs/minigames/" + file), StandardCharsets.UTF_8)) {
            if (!line.isBlank() && !line.startsWith("#")) {
                out.add(line.split("\\|", -1));
            }
        }
        return out;
    }

    /** A wiki item name as a RuneCraft id. */
    private static String id(String wikiName) {
        String id = wikiName.toLowerCase(java.util.Locale.ROOT).replace("(", "").replace(")", "").replace("'", "")
                .trim().replaceAll("[^a-z0-9]+", "_");
        return switch (id) {
            case "banana_tree_seed" -> "banana_seed";
            default -> id;
        };
    }

    private static int[] range(String q) {
        String[] p = q.split("-");
        return new int[]{Integer.parseInt(p[0].trim()), Integer.parseInt(p[p.length - 1].trim())};
    }

    // ------------------------------------------------------------------ Wintertodt

    @Test
    void wintertodtSkillTablesMatchTheWikiModule() throws Exception {
        Map<Skill, List<String[]>> bySkill = new HashMap<>();
        for (String[] r : rows("wintertodt_reward_cart.tsv")) {
            bySkill.computeIfAbsent(Skill.valueOf(r[0].toUpperCase(java.util.Locale.ROOT)), k -> new ArrayList<>()).add(r);
        }
        assertEquals(WintertodtRewards.TABLES.size(), bySkill.size());
        for (WintertodtRewards.Table t : WintertodtRewards.TABLES) {
            List<String[]> wiki = bySkill.get(t.skill());
            assertNotNull(wiki, t.skill().name());
            assertEquals(wiki.size(), t.lines().size(), t.skill().name());
            for (int i = 0; i < wiki.size(); i++) {
                String[] w = wiki.get(i);
                WintertodtRewards.Line l = t.lines().get(i);
                assertEquals(id(w[1]), l.item(), t.skill() + " line " + i);
                // Limestone: 3-7 on the Reward Cart and old supply crate drop tables, 2-7 in the calculator module.
                int min = l.item().equals("limestone") ? 2 : l.min();
                assertEquals(Integer.parseInt(w[2]), min, l.item());
                assertEquals(Integer.parseInt(w[3]), l.max(), l.item());
                assertEquals(Integer.parseInt(w[4]), l.low(), l.item());
                assertEquals(Integer.parseInt(w[5]), l.high(), l.item());
                assertTrue(ItemCatalog.exists(l.item()), l.item());
            }
        }
    }

    @Test
    void wintertodtCartRules() {
        // The cart holds 8,000 rewards; no per-game cap any more.
        assertEquals(8000, WintertodtRewards.addToCart(7990, 50));
        assertEquals(120, WintertodtRewards.addToCart(100, 20));
        assertEquals(81, WintertodtRewards.rolls(40_000));
        // The pyromancer piece is the one owned least, ties broken garb, hood, robe, boots.
        WintertodtRewards.Player none = new WintertodtRewards.Player(99, 99, 99, 99, 99, 99, false, 0, 0, null);
        assertEquals("pyromancer_garb", WintertodtRewards.pyromancerPiece(none));
        WintertodtRewards.Player p = none.after("pyromancer_garb");
        assertEquals("pyromancer_hood", WintertodtRewards.pyromancerPiece(p));
        p = p.after("pyromancer_hood").after("pyromancer_robe");
        assertEquals("pyromancer_boots", WintertodtRewards.pyromancerPiece(p));
        p = p.after("pyromancer_boots");
        assertEquals("pyromancer_garb", WintertodtRewards.pyromancerPiece(p));
        // Warm gloves and bruma torches are replaced only from the third one owned (Mod Ash).
        Random r = new Random(5);
        WintertodtRewards.Player twoEach = new WintertodtRewards.Player(99, 99, 99, 99, 99, 99, true, 2, 2, null);
        WintertodtRewards.Player threeEach = new WintertodtRewards.Player(99, 99, 99, 99, 99, 99, true, 3, 3, null);
        Map<String, Integer> two = new HashMap<>();
        Map<String, Integer> three = new HashMap<>();
        for (int i = 0; i < 150_000; i++) {
            two.merge(WintertodtRewards.roll(twoEach, r::nextDouble).item(), 1, Integer::sum);
            three.merge(WintertodtRewards.roll(threeEach, r::nextDouble).item(), 1, Integer::sum);
        }
        assertTrue(two.getOrDefault("warm_gloves", 0) > 0 && two.getOrDefault("bruma_torch", 0) > 0);
        assertFalse(two.containsKey("magic_seed"));
        assertFalse(three.containsKey("warm_gloves") || three.containsKey("bruma_torch"));
        assertTrue(three.getOrDefault("magic_seed", 0) > 0 && three.getOrDefault("torstol_seed", 0) > 0);
        // Resources come noted, seeds and uniques as they are.
        assertTrue(WintertodtRewards.noted("magic_logs") && WintertodtRewards.noted("dynamite") && WintertodtRewards.noted("raw_shark"));
        assertFalse(WintertodtRewards.noted("ranarr_seed") || WintertodtRewards.noted("pyromancer_garb")
                || WintertodtRewards.noted("burnt_page"));
        // Searching n rewards counts each unique for the next roll.
        assertEquals(5, WintertodtRewards.search(5, none, new Random(1)::nextDouble).size());
    }

    // ------------------------------------------------------------------ Tempoross

    @Test
    void temporossCasketMatchesTheWiki() throws Exception {
        List<String[]> wiki = rows("tempoross_casket.tsv");
        assertEquals(wiki.size(), TemporossRewards.CASKET_TABLE.size());
        int total = 0;
        for (int i = 0; i < wiki.size(); i++) {
            String[] w = wiki.get(i);
            TemporossRewards.CasketLine l = TemporossRewards.CASKET_TABLE.get(i);
            assertEquals(id(w[0]), l.item(), "line " + i);
            int[] q = range(w[1]);
            assertEquals(q[0], l.min(), l.item());
            assertEquals(q[1], l.max(), l.item());
            assertEquals(w[2], l.weight() + "/" + TemporossRewards.CASKET_TOTAL, l.item());
            assertTrue(ItemCatalog.exists(l.item()), l.item());
            total += l.weight();
        }
        assertEquals(TemporossRewards.CASKET_TOTAL, total);
        Map<String, Integer> clues = new HashMap<>();
        Random r = new Random(3);
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            List<MinigameReward> got = TemporossRewards.openCasket(r::nextDouble);
            assertTrue(got.size() == 1 || got.size() == 2);
            if (got.size() == 2) {
                clues.merge(got.get(1).item(), 1, Integer::sum);
            }
        }
        assertEquals(0.05, clues.getOrDefault("clue_scroll_easy", 0) / (double) n, 0.004);
        assertEquals(0.02, clues.getOrDefault("clue_scroll_hard", 0) / (double) n, 0.003);
    }

    @Test
    void temporossPoolFishFollowTheWikiBrackets() {
        // About 45/80 of the pool is fish; each bracket's five fish weigh 900/810/720/630/540 of 6,400.
        assertEquals(45 / 80.0, TemporossRewards.FISH_WEIGHT / 8000.0, 1e-9);
        assertEquals(900 / 6400.0, TemporossRewards.fishChance(81, "raw_bass"), 0.0001);
        assertEquals(540 / 6400.0, TemporossRewards.fishChance(81, "raw_manta_ray"), 0.0001);
        assertEquals(0, TemporossRewards.fishChance(80, "raw_manta_ray"), 0);
        assertEquals("raw_sea_turtle", TemporossRewards.fishFor(79).get(4).item());
        assertEquals("raw_shark", TemporossRewards.fishFor(76).get(4).item());
        assertEquals("raw_swordfish", TemporossRewards.fishFor(75).get(4).item());
        assertEquals("raw_bass", TemporossRewards.fishFor(49).get(4).item());
        assertEquals("raw_lobster", TemporossRewards.fishFor(45).get(4).item());
        assertEquals("raw_tuna", TemporossRewards.fishFor(35).get(4).item());
        Random r = new Random(9);
        int n = 200_000;
        int fish = 0;
        int other = 0;
        for (int i = 0; i < n; i++) {
            MinigameReward rw = TemporossRewards.roll(99, List.of(), r::nextDouble);
            assertTrue(ItemCatalog.exists(rw.item()), rw.item());
            if (rw.item().startsWith("raw_")) {
                fish++;
            } else if (TemporossRewards.OTHER.stream().anyMatch(l -> l.item().equals(rw.item()))) {
                other++;
            }
        }
        assertEquals(0.5625, fish / (double) n, 0.005);
        assertEquals(899 / 8000.0, other / (double) n, 0.003);
        assertEquals(10.0, TemporossRewards.POOL_XP, 0);
    }

    @Test
    void temporossEssencePhaseAndStorm() {
        // Energy recharges across the essence phase, then Tempoross rises; the storm builds only while he is up.
        assertEquals(0, TemporossStorm.recharged(260, 0));
        assertEquals(130, TemporossStorm.recharged(260, TemporossStorm.ESSENCE_PHASE_TICKS / 2));
        assertEquals(260, TemporossStorm.recharged(260, TemporossStorm.ESSENCE_PHASE_TICKS + 10));
        assertEquals(0, TemporossStorm.stormGain(true, true, 9));
        assertEquals(1, TemporossStorm.stormGain(false, false, 9));
        assertEquals(0, TemporossStorm.stormGain(false, false, 10));
        assertEquals(1, TemporossStorm.stormGain(false, true, 10));
        assertEquals(3, TemporossStorm.MAX_RECOVERIES);
        assertEquals(TemporossStorm.ESSENCE_PHASE_TICKS, MinigameRules.Tempoross.VULNERABLE_TICKS);
        // A mast or totem breaks 15% of the time in a calm storm, 25% at its height (Mod Ash).
        assertEquals(15, MinigameRules.Tempoross.tetherBreakChance(0));
        assertEquals(25, MinigameRules.Tempoross.tetherBreakChance(1));
    }

    // ------------------------------------------------------------------ Guardians of the Rift

    @Test
    void guardiansIntricatePouchMatchesTheWiki() throws Exception {
        List<String[]> wiki = rows("gotr_intricate_pouch.tsv");
        List<String[]> pre = new ArrayList<>();
        List<String[]> main = new ArrayList<>();
        String clue = null;
        for (String[] w : wiki) {
            switch (w[0]) {
                case "pre-roll" -> {
                    if (!w[1].equals("Lamp")) { // approx: the XP lamp isn't modelled
                        pre.add(w);
                    }
                }
                case "main drops" -> main.add(w);
                default -> clue = id(w[1]) + "|" + w[3];
            }
        }
        assertEquals(pre.size(), GuardiansRewards.POUCH_PRE_ROLL.length);
        for (String[] w : pre) {
            boolean found = false;
            for (Object[] p : GuardiansRewards.POUCH_PRE_ROLL) {
                if (p[0].equals(id(w[1]))) {
                    assertEquals(w[3], "1/" + p[1], w[1]);
                    found = true;
                }
            }
            assertTrue(found, w[1]);
        }
        assertEquals(main.size(), GuardiansRewards.POUCH_MAIN.size());
        for (int i = 0; i < main.size(); i++) {
            String[] w = main.get(i);
            GuardiansRewards.Line l = GuardiansRewards.POUCH_MAIN.get(i);
            assertEquals(id(w[1]), l.item());
            int[] q = range(w[2]);
            assertEquals(q[0], l.min(), l.item());
            assertEquals(q[1], l.max(), l.item());
            assertEquals(w[3], l.weight() + "/" + GuardiansRewards.POUCH_MAIN_TOTAL, l.item());
            assertTrue(ItemCatalog.exists(l.item()), l.item());
        }
        assertEquals("clue_scroll_hard|1/" + GuardiansRewards.POUCH_CLUE_CHANCE, clue);
        // Uniques only while never owned.
        Random r = new Random(4);
        int lockets = 0;
        for (int i = 0; i < 20_000; i++) {
            for (MinigameReward rw : GuardiansRewards.openIntricatePouch(r::nextDouble, GuardiansRewards.POUCH_UNIQUES)) {
                if (GuardiansRewards.POUCH_UNIQUES.contains(rw.item())) {
                    lockets++;
                }
            }
        }
        assertEquals(0, lockets);
        for (String id : GuardiansRewards.allItems()) {
            assertTrue(ItemCatalog.exists(id), id);
        }
    }

    @Test
    void guardiansCellsRemainsAndPortal() {
        // Placing a cell on an inactive tile: a weak barrier, 17/20/24/30 energy of each type by the cell (wiki table).
        int[] expected = {17, 20, 24, 30};
        for (GuardiansRift.Cell c : GuardiansRift.Cell.values()) {
            GuardiansRift rift = new GuardiansRift();
            GuardiansRift.Use use = rift.useCell(0, c, 0, 0);
            assertTrue(use.ok());
            assertEquals(expected[c.ordinal()], use.elemental(), c.label);
            assertEquals(expected[c.ordinal()], use.catalytic(), c.label);
            assertEquals(GuardiansRift.Cell.WEAK.ordinal(), rift.tier(0), c.label);
            if (c != GuardiansRift.Cell.WEAK) { // a guardian gives 2x+1, x the strengthening energy (weak: 6)
                assertEquals(2 * c.strengthenEnergy + 1, c.guardianEnergy, c.label);
            }
        }
        // Workbench Crafting XP: level / 10, clamped to 1-5.
        assertEquals(1.0, GuardiansRift.workbenchXp(5), 1e-9);
        assertEquals(2.7, GuardiansRift.workbenchXp(27), 1e-9);
        assertEquals(5.0, GuardiansRift.workbenchXp(99), 1e-9);
        assertEquals(5.0, GuardiansRift.MINING_XP, 0);
        assertEquals(56, GuardiansRift.LARGE_REMAINS_AGILITY);
        // The huge remains' portal: 160 s in, then every 140 s, open about 25 s.
        assertFalse(GuardiansRift.portalOpen(0));
        assertFalse(GuardiansRift.portalOpen(266));
        assertTrue(GuardiansRift.portalOpen(267));
        assertTrue(GuardiansRift.portalOpen(267 + 41));
        assertFalse(GuardiansRift.portalOpen(267 + 42));
        assertTrue(GuardiansRift.portalOpen(267 + 233));
        assertEquals(160, Math.round(GuardiansRift.PORTAL_FIRST_TICKS * 0.6));
        assertEquals(140, Math.round(GuardiansRift.PORTAL_INTERVAL_TICKS * 0.6));
    }

    // ------------------------------------------------------------------ Volcanic Mine

    @Test
    void volcanicMineRewardsAndByproducts() {
        // Pete pays 3.8 Mining XP per point at the end of a game left alive through the exit.
        assertEquals(380.0, VolcanicMine.endXp(100), 1e-9);
        // Lava beasts: a point per 10 damage of each attack, 3 more for the final blow.
        assertEquals(1, VolcanicMine.lavaBeastHitPoints(14));
        assertEquals(0, VolcanicMine.lavaBeastHitPoints(9));
        assertEquals(3, VolcanicMine.LAVA_BEAST_KILL_POINTS);
        // Calcite or pyrophosphite only come with an ore fragment: 1/10, one or the other.
        Random r = new Random(2);
        int n = 100_000;
        int minerals = 0;
        for (int i = 0; i < n; i++) {
            if (VolcanicMine.byproduct(r::nextDouble) != null) {
                minerals++;
            }
        }
        assertEquals(0.1, minerals / (double) n, 0.004);
        assertEquals(VolcanicMine.PREP_TICKS, 50);
        assertEquals(VolcanicMine.MAX_POINTS, (1 << 21) - 1);
        // Petrified Pete sells the large water container (doubling the vessel's throws) for 10,000 points.
        boolean found = false;
        for (RewardShop.Entry e : RewardShops.get("volcanic_mine").entries()) {
            if (e.reward() == RewardShop.Reward.UNLOCK && e.item().equals(VolcanicMine.LARGE_VESSEL_FLAG)) {
                assertEquals(10_000, e.costs().get(0).amount());
                found = true;
            }
        }
        assertTrue(found, "no large water container in Petrified Pete's Ore Shop");
        assertEquals(2 * VolcanicMine.vesselUses(false), VolcanicMine.vesselUses(true));
    }
}
