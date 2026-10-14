package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.Chance;
import com.runecraft.osrs.content.extras.SkillingPets;
import com.runecraft.osrs.content.npcs.TownMonsters;
import com.runecraft.osrs.data.Fish;
import com.runecraft.osrs.data.Rock;
import com.runecraft.osrs.data.SpecialTool;
import com.runecraft.osrs.data.Thieving;
import com.runecraft.osrs.data.ToolTier;
import com.runecraft.osrs.data.Tree;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Diffs the mod's skilling success rates (pickpockets, stalls, fishing, woodcutting, mining) against snapshots of the
 * OSRS Wiki (Kiyogitpy/osrs-wiki-data, July 2026) under {@code src/test/resources/osrs/skilling/}:
 * <ul>
 *   <li>{@code success_rates.tsv}: every pickpocket, knock-out, fishing, woodcutting and mining "Skilling success
 *       chart" line (page, chart, tabber tab, line, low, high, level);</li>
 *   <li>{@code pickpocket_targets.tsv}: the Thieving page's NPC table (level, XP, stun damage);</li>
 *   <li>{@code stalls.tsv}: the Stall/Thievable table (level, XP, respawn in game ticks).</li>
 * </ul>
 */
class SuccessRateSnapshotTest {
    private record Line(String page, String chart, String tab, String line, double low, double high) {
    }

    private static List<String> read(String name) throws IOException {
        String fixture = "osrs/skilling/" + name;
        Path p = Path.of("src/test/resources", fixture);
        if (Files.exists(p)) {
            return Files.readAllLines(p, StandardCharsets.UTF_8);
        }
        try (InputStream in = SuccessRateSnapshotTest.class.getClassLoader().getResourceAsStream(fixture)) {
            assertNotNull(in, "missing " + fixture);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }

    private static List<String[]> rows(String name) throws IOException {
        List<String[]> out = new ArrayList<>();
        for (String l : read(name)) {
            if (!l.isBlank() && !l.startsWith("#")) {
                out.add(l.split("\t", -1));
            }
        }
        return out;
    }

    private static List<Line> lines() throws IOException {
        List<Line> out = new ArrayList<>();
        for (String[] c : rows("success_rates.tsv")) {
            out.add(new Line(c[0], c[1], c[2], c[3], Double.parseDouble(c[4]), Double.parseDouble(c[5])));
        }
        return out;
    }

    /** The wiki line on {@code page} (chart label containing {@code chart}, in tab {@code tab}) called {@code line}. */
    private static Line ref(List<Line> all, String page, String chart, String tab, String line) {
        for (Line l : all) {
            if (l.page().equals(page) && l.chart().toLowerCase().contains(chart) && l.tab().equals(tab) && l.line().equals(line)) {
                return l;
            }
        }
        throw new AssertionError("no wiki line " + page + " / " + chart + " / " + tab + " / " + line);
    }

    private static void check(String what, Line ref, double low, double high) {
        assertEquals(ref.low(), low, 1e-9, what + " low (wiki " + ref + ")");
        assertEquals(ref.high(), high, 1e-9, what + " high (wiki " + ref + ")");
    }

    // ------------------------------------------------------------------ Thieving

    /** Mod pickpocket name -> wiki page with its pickpocket chart. */
    private static final Map<String, String> PICKPOCKET_PAGES = Map.ofEntries(
            Map.entry("Man", "Man"), Map.entry("Woman", "Woman"), Map.entry("Farmer", "Farmer"),
            Map.entry("Al-Kharid warrior", "Al Kharid warrior"), Map.entry("Guard", "Guard"),
            Map.entry("Knight of Ardougne", "Knight of Ardougne"), Map.entry("Paladin", "Paladin"), Map.entry("Hero", "Hero"),
            Map.entry("H.A.M. Member", "H.A.M. Member"), Map.entry("Rogue", "Rogue"), Map.entry("Master Farmer", "Master Farmer"),
            Map.entry("Watchman", "Watchman"), Map.entry("Menaphite Thug", "Menaphite Thug"), Map.entry("Gnome", "Gnome"),
            Map.entry("Vyre", "Vyre"), Map.entry("Elf", "Elf (Thieving)"), Map.entry("TzHaar-Hur", "TzHaar-Hur"),
            Map.entry("Wealthy citizen", "Wealthy citizen"), Map.entry("Bandit", "Bandit (Bandit Camp)"));

    /** Targets the wiki has no success chart for (low/high approx). */
    private static final Set<String> NO_CHART = Set.of("Fremennik citizen", "Bearded Pollnivneach bandit", "Pollnivneach bandit",
            "Cave goblin", "Digsite workman", "Villager");

    /** Mod pickpocket name -> row name of the Thieving page's NPC table, where they differ. */
    private static final Map<String, String> TABLE_NAMES = Map.of("Woman", "Man", "Al-Kharid warrior", "Warrior",
            "Bandit", "Desert Bandit", "Fremennik citizen", "Fremennik Citizen", "Bearded Pollnivneach bandit",
            "Bearded Pollnivnian Bandit", "Pollnivneach bandit", "Pollnivnian Bandit", "Watchman", "Yanille Watchman");

    private record Target(String name, int level, double xp, int low, int high, int stun) {
    }

    private static List<Target> targets() {
        List<Target> out = new ArrayList<>();
        for (Thieving.Pickpocket p : Thieving.Pickpocket.values()) {
            out.add(new Target(p.displayName(), p.level(), p.xp(), p.low(), p.high(), p.stunDamage()));
        }
        for (ThievingData.Target t : ThievingData.TARGETS) {
            out.add(new Target(t.name(), t.level(), t.xp(), t.low(), t.high(), t.stunDamage()));
        }
        for (TownMonsters.Pickpocket p : TownMonsters.pickpockets()) {
            out.add(new Target(p.name(), p.level(), p.xp(), p.low(), p.high(), p.stunDamage()));
        }
        return out;
    }

    @Test
    void pickpocketSuccessRatesMatchTheWikiCharts() throws IOException {
        List<Line> all = lines();
        for (Target t : targets()) {
            if (NO_CHART.contains(t.name())) {
                continue;
            }
            String page = PICKPOCKET_PAGES.get(t.name());
            assertNotNull(page, "pickpocket target with no wiki page mapping: " + t.name());
            check(t.name(), ref(all, page, "pickpocket chance", "", "Normal"), t.low(), t.high());
        }
        // Knock-outs (Menaphite thug chart).
        Line ko = ref(all, "Menaphite Thug", "knockout chance", "", "Menaphite Thug");
        check("knock-out", ko, ThievingData.KNOCKOUT_LOW, ThievingData.KNOCKOUT_HIGH);
    }

    @Test
    void pickpocketBonusesScaleLowAndHighLikeTheWikiCharts() throws IOException {
        List<Line> all = lines();
        int checked = 0;
        for (Map.Entry<String, String> e : PICKPOCKET_PAGES.entrySet()) {
            Line normal = ref(all, e.getValue(), "pickpocket chance", "", "Normal");
            Map<String, double[]> bonuses = new LinkedHashMap<>();
            bonuses.put("Gloves of silence", new double[]{com.runecraft.osrs.content.skilling.HunterData.GLOVES_OF_SILENCE_BONUS});
            bonuses.put("Ardougne Diary", new double[]{1.1});
            bonuses.put("Thieving cape + Ardougne Diary", new double[]{1.1, Thieving.THIEVING_CAPE_BONUS});
            for (Map.Entry<String, double[]> b : bonuses.entrySet()) {
                Line r = ref(all, e.getValue(), "pickpocket chance", "", b.getKey());
                check(e.getKey() + " " + b.getKey(), r, Thieving.scaled((int) normal.low(), b.getValue()),
                        Thieving.scaled((int) normal.high(), b.getValue()));
                checked++;
            }
        }
        assertTrue(checked >= 50, "bonus lines checked: " + checked);
        // The chance itself uses the scaled values: a guard at 60 with the diary is lerp(55, 264), not lerp(50, 240) * 1.1.
        assertEquals(Chance.lerp(60, 55, 264), Thieving.pickpocketChance(60, 50, 240, 1.1), 1e-12);
    }

    @Test
    void pickpocketLevelsXpAndStunsMatchTheThievingTable() throws IOException {
        Map<String, String[]> table = new HashMap<>();
        for (String[] r : rows("pickpocket_targets.tsv")) {
            table.put(r[0], r);
        }
        for (Target t : targets()) {
            String[] r = table.get(TABLE_NAMES.getOrDefault(t.name(), t.name()));
            assertNotNull(r, "no Thieving table row for " + t.name());
            assertEquals(Integer.parseInt(r[1]), t.level(), t.name() + " level");
            assertEquals(Double.parseDouble(r[2]), t.xp(), 1e-9, t.name() + " XP");
            if (r[3].matches("\\d+")) {
                assertEquals(Integer.parseInt(r[3]), t.stun(), t.name() + " stun damage");
            }
        }
    }

    /** Stall block id -> wiki stall name. */
    private static final Map<String, String> STALLS = Map.ofEntries(
            Map.entry("cake_stall", "Bakery stall"), Map.entry("silk_stall", "Silk stall"), Map.entry("fur_stall", "Fur stall"),
            Map.entry("silver_stall", "Silver stall"), Map.entry("spice_stall", "Spice stall"), Map.entry("gem_stall", "Gem stall"),
            Map.entry("vegetable_stall", "Vegetable stall"), Map.entry("tea_stall", "Tea stall"), Map.entry("wine_stall", "Wine stall"),
            Map.entry("fruit_stall", "Fruit Stall"), Map.entry("seed_stall", "Seed stall"), Map.entry("fish_stall", "Fish stall"),
            Map.entry("crossbow_stall", "Crossbow stall"), Map.entry("magic_stall", "Magic stall"),
            Map.entry("scimitar_stall", "Scimitar stall"), Map.entry("keldagrim_crafting_stall", "Crafting stall"),
            Map.entry("keldagrim_bakery_stall", "Bakery stall"), Map.entry("keldagrim_gem_stall", "Gem stall"),
            Map.entry("ore_stall", "Ore stall (Mor Ul Rek)"));

    /** Respawns that differ from the table's main row (the table notes Keldagrim's bakery restocks in 9.6 s). */
    private static final Map<String, Integer> RESPAWN_OVERRIDES = Map.of("keldagrim_bakery_stall", 16);

    @Test
    void stallsMatchTheStallTable() throws IOException {
        Map<String, String[]> table = new HashMap<>();
        for (String[] r : rows("stalls.tsv")) {
            table.put(r[0], r);
        }
        Map<String, int[]> mod = new LinkedHashMap<>();
        Map<String, Double> xp = new HashMap<>();
        for (Thieving.Stall s : Thieving.Stall.values()) {
            mod.put(s.blockId(), new int[]{s.level(), s.respawnTicks()});
            xp.put(s.blockId(), s.xp());
        }
        for (ThievingData.Container c : ThievingData.CONTAINERS) {
            if (!c.chest()) {
                mod.put(c.blockId(), new int[]{c.level(), c.respawnTicks()});
                xp.put(c.blockId(), c.xp());
            }
        }
        for (Map.Entry<String, int[]> e : mod.entrySet()) {
            String name = STALLS.get(e.getKey());
            assertNotNull(name, "stall with no wiki mapping: " + e.getKey());
            String[] r = table.get(name);
            assertNotNull(r, name);
            assertEquals(Integer.parseInt(r[1]), e.getValue()[0], e.getKey() + " level");
            assertEquals(Double.parseDouble(r[2]), xp.get(e.getKey()), 1e-9, e.getKey() + " XP");
            int respawn = RESPAWN_OVERRIDES.getOrDefault(e.getKey(), Integer.parseInt(r[3]));
            assertEquals(respawn, e.getValue()[1], e.getKey() + " respawn ticks");
        }
        // Rocky: the gem stall is 1/36,490 (keyed by stall, so equal-XP stalls and targets keep their own rates).
        assertEquals(36_490, SkillingPets.thievingBase(Thieving.Stall.GEM.blockId(), com.runecraft.osrs.Experience.tenths(408.0)));
        assertEquals(124_066, SkillingPets.thievingBase("cake_stall", com.runecraft.osrs.Experience.tenths(16.0)));
        assertEquals(68_926, SkillingPets.thievingBase("tea_stall", com.runecraft.osrs.Experience.tenths(16.0)));
    }

    // ------------------------------------------------------------------ Fishing

    @Test
    void fishCatchRatesMatchTheWikiCharts() throws IOException {
        List<Line> all = lines();
        Map<Fish, String[]> keys = new LinkedHashMap<>();
        keys.put(Fish.SHRIMPS, new String[]{"Raw shrimps", "", "Raw shrimps"});
        keys.put(Fish.SARDINE, new String[]{"Raw sardine", "", "Sardine"});
        keys.put(Fish.HERRING, new String[]{"Raw herring", "", "Herring"});
        keys.put(Fish.ANCHOVIES, new String[]{"Raw anchovies", "", "Raw anchovies"});
        keys.put(Fish.TROUT, new String[]{"Raw trout", "", "Raw trout"});
        keys.put(Fish.PIKE, new String[]{"Raw pike", "", "Raw pike"});
        keys.put(Fish.SALMON, new String[]{"Raw salmon", "", "Raw salmon"});
        keys.put(Fish.TUNA, new String[]{"Raw tuna", "Harpoon", "Tuna"});
        keys.put(Fish.LOBSTER, new String[]{"Raw lobster", "", "Raw lobster"});
        keys.put(Fish.SWORDFISH, new String[]{"Raw swordfish", "Harpoon", "Swordfish"});
        keys.put(Fish.MONKFISH, new String[]{"Raw monkfish", "", "Raw monkfish"});
        keys.put(Fish.SHARK, new String[]{"Raw shark", "", "Harpoon"});
        keys.put(Fish.ANGLERFISH, new String[]{"Raw anglerfish", "", "Sandworms"});
        keys.put(Fish.DARK_CRAB, new String[]{"Raw dark crab", "", "Normal"});
        assertEquals(Fish.values().length, keys.size());
        for (Map.Entry<Fish, String[]> e : keys.entrySet()) {
            String[] k = e.getValue();
            check(e.getKey().name(), ref(all, k[0], "fishing chance", k[1], k[2]), e.getKey().low(), e.getKey().high());
        }
        // Dragon and crystal harpoons: the wiki's tabs/lines are the harpoon values scaled by 1.2 / 1.35, rounded down.
        double dragon = SpecialTool.DRAGON_HARPOON.catchMultiplier();
        double crystal = SpecialTool.CRYSTAL_HARPOON.catchMultiplier();
        for (Fish f : new Fish[]{Fish.TUNA, Fish.SWORDFISH}) {
            String line = f == Fish.TUNA ? "Tuna" : "Swordfish";
            Line d = ref(all, "Raw tuna", "fishing chance", "Dragon harpoon", line);
            Line c = ref(all, "Raw tuna", "fishing chance", "Crystal harpoon", line);
            assertEquals(Chance.lerp(70, d.low(), d.high()), f.chance(70, dragon), 1e-12, f + " dragon");
            assertEquals(Chance.lerp(70, c.low(), c.high()), f.chance(70, crystal), 1e-12, f + " crystal");
        }
        Line d = ref(all, "Raw shark", "shark fishing chance", "", "Dragon");
        Line c = ref(all, "Raw shark", "shark fishing chance", "", "Crystal");
        assertEquals(Chance.lerp(85, d.low(), d.high()), Fish.SHARK.chance(85, dragon), 1e-12);
        assertEquals(Chance.lerp(85, c.low(), c.high()), Fish.SHARK.chance(85, crystal), 1e-12);
    }

    @Test
    void moduleFishingSpotsMatchTheWikiCharts() throws IOException {
        List<Line> all = lines();
        Map<String, String[]> keys = new HashMap<>();
        keys.put("raw_bass", new String[]{"Raw bass", "Raw bass"});
        keys.put("raw_cod", new String[]{"Raw cod", "Raw cod"});
        keys.put("raw_mackerel", new String[]{"Raw mackerel", "Raw mackerel"});
        keys.put("leaping_sturgeon", new String[]{"Leaping sturgeon", "Leaping sturgeon"});
        keys.put("leaping_salmon", new String[]{"Leaping salmon", "Leaping salmon"});
        keys.put("leaping_trout", new String[]{"Leaping trout", "Leaping trout"});
        keys.put("raw_karambwanji", new String[]{"Raw karambwanji", "Raw karambwanji"});
        keys.put("raw_karambwan", new String[]{"Raw karambwan", "Raw karambwan"});
        keys.put("raw_cave_eel", new String[]{"Raw cave eel", "Raw cave eel"});
        keys.put("raw_slimy_eel", new String[]{"Raw slimy eel", "Raw slimy eel"});
        keys.put("raw_lava_eel", new String[]{"Raw lava eel", "Oily fishing rod"});
        keys.put("sacred_eel", new String[]{"Sacred eel", "Sacred eel"});
        keys.put("infernal_eel", new String[]{"Infernal eel", "Infernal eel"});
        keys.put("raw_shark", new String[]{"Raw shark", "Harpoon"});
        // Deep sea fishing (manta rays, sea turtles) has no wiki chart: approx.
        Set<String> noChart = Set.of("raw_manta_ray", "raw_sea_turtle", GatheringData.MINNOW);
        for (GatheringData.Spot s : GatheringData.SPOTS) {
            for (GatheringData.Catch c : s.catches()) {
                if (noChart.contains(c.item())) {
                    continue;
                }
                String[] k = keys.get(c.item());
                assertNotNull(k, "catch with no wiki mapping: " + c.item());
                Line r = null;
                for (Line l : all) {
                    if (l.page().equals(k[0]) && l.tab().isEmpty() && l.line().equals(k[1])
                            && (l.chart().contains("fishing chance") || l.chart().contains("catch chance"))) {
                        r = l;
                        break;
                    }
                }
                assertNotNull(r, c.item());
                check(s.blockId() + " " + c.item(), r, c.low(), c.high());
            }
        }
    }

    // ------------------------------------------------------------------ Woodcutting and Mining

    private static final String[] AXES = {"Bronze", "Iron", "Steel", "Black", "Mithril", "Adamant", "Rune", "Dragon", "Crystal"};

    @Test
    void axeMultipliersMatchTheColumns() {
        ToolTier[] tiers = ToolTier.values();
        for (int i = 0; i < tiers.length; i++) {
            assertEquals(i, Tree.axeColumn(tiers[i].woodcuttingMultiplier()), tiers[i].name());
        }
        assertEquals(8, Tree.axeColumn(SpecialTool.woodcuttingMultiplier(ToolTier.DRAGON, "crystal_axe")));
        assertEquals(7, Tree.axeColumn(SpecialTool.woodcuttingMultiplier(ToolTier.DRAGON, "infernal_axe")));
    }

    @Test
    void treesMatchTheWikiChartsForEveryAxe() throws IOException {
        List<Line> all = lines();
        Map<Tree, String> pages = new LinkedHashMap<>();
        pages.put(Tree.TREE, "Tree");
        pages.put(Tree.OAK, "Oak tree");
        pages.put(Tree.WILLOW, "Willow tree");
        pages.put(Tree.TEAK, "Teak tree");
        pages.put(Tree.MAPLE, "Maple tree");
        pages.put(Tree.MAHOGANY, "Mahogany tree");
        pages.put(Tree.YEW, "Yew tree");
        pages.put(Tree.MAGIC, "Magic tree");
        pages.put(Tree.REDWOOD, "Redwood tree");
        assertEquals(Tree.values().length, pages.size());
        for (Map.Entry<Tree, String> e : pages.entrySet()) {
            Tree t = e.getKey();
            check(t + " bronze", ref(all, e.getValue(), "cut chance", "", "Bronze"), t.low(), t.high());
            for (int i = 0; i < AXES.length; i++) {
                double m = Tree.AXE_MULTIPLIERS[i];
                check(t + " " + AXES[i], ref(all, e.getValue(), "cut chance", "", AXES[i]), t.low(m), t.high(m));
            }
        }
        Map<String, String> modPages = Map.of("achey_tree", "Achey Tree", "arctic_pine_tree", "Arctic pine tree",
                "sulliuscep", "Sulliuscep", "hollow_tree", "Hollow tree", "camphor_tree", "Camphor tree",
                "ironwood_tree", "Ironwood tree", "rosewood_tree", "Rosewood tree");
        for (GatheringData.ModTree t : GatheringData.TREES) {
            String page = modPages.get(t.blockId());
            assertNotNull(page, "tree with no wiki mapping: " + t.blockId());
            check(t.blockId() + " bronze", ref(all, page, "cut chance", "", "Bronze"), t.low(), t.high());
            for (int i = 0; i < AXES.length; i++) {
                if (t.blockId().equals("hollow_tree") && AXES[i].equals("Crystal")) {
                    continue; // no crystal line on the wiki
                }
                Line r = ref(all, page, "cut chance", "", AXES[i]);
                double m = Tree.AXE_MULTIPLIERS[i];
                if (t.blockId().equals("camphor_tree") && AXES[i].equals("Black")) {
                    assertEquals(r.low(), t.low(m), 1e-9); // the wiki's black-axe high (71) repeats the adamant value
                    continue;
                }
                check(t.blockId() + " " + AXES[i], r, t.low(m), t.high(m));
            }
        }
    }

    @Test
    void rocksMatchTheWikiCharts() throws IOException {
        List<Line> all = lines();
        Map<Rock, String[]> keys = new LinkedHashMap<>();
        keys.put(Rock.COPPER, new String[]{"Copper rocks", "Copper rocks"});
        keys.put(Rock.TIN, new String[]{"Tin rocks", "Tin rocks"});
        keys.put(Rock.CLAY, new String[]{"Clay rocks", "Clay rocks"});
        keys.put(Rock.IRON, new String[]{"Iron rocks", "Iron rocks"});
        keys.put(Rock.SILVER, new String[]{"Silver rocks", "Silver rocks"});
        keys.put(Rock.COAL, new String[]{"Coal rocks", "Coal rocks"});
        keys.put(Rock.GOLD, new String[]{"Gold rocks", "Gold rocks"});
        keys.put(Rock.GEM, new String[]{"Gem rocks", "Normal"});
        keys.put(Rock.MITHRIL, new String[]{"Mithril rocks", "Mithril rocks"});
        keys.put(Rock.ADAMANTITE, new String[]{"Adamantite rocks", "Adamantite rocks"});
        keys.put(Rock.RUNITE, new String[]{"Runite rocks", "Runite rocks"});
        keys.put(Rock.AMETHYST, new String[]{"Amethyst crystals", "Amethyst crystal"});
        for (Rock r : Rock.values()) {
            if (r == Rock.RUNE_ESSENCE || r == Rock.PURE_ESSENCE) {
                assertEquals(256, r.low()); // essence always succeeds
                continue;
            }
            String[] k = keys.get(r);
            assertNotNull(k, "rock with no wiki mapping: " + r);
            check(r.name(), ref(all, k[0], "mining chance", "", k[1]), r.low(), r.high());
        }
        Map<String, String[]> mod = Map.of("blurite_rock", new String[]{"Blurite rocks", "Blurite rocks"},
                "limestone_rock", new String[]{"Limestone rock", "Limestone"},
                "pay_dirt_vein", new String[]{"Pay-dirt", "Pay-dirt"}, "inner_pay_dirt_vein", new String[]{"Pay-dirt", "Pay-dirt"},
                "upper_pay_dirt_vein", new String[]{"Pay-dirt", "Pay-dirt"}, MiningData.LEAD, new String[]{"Lead rocks", "Lead rocks"},
                MiningData.BARRONITE, new String[]{"Barronite rocks", "Barronite rock"},
                MiningData.SULPHUR, new String[]{"Volcanic sulphur", "Volcanic sulphur"});
        Map<String, String[]> sizes = Map.of("sandstone_10kg", new String[]{"Sandstone rocks", "10kg"},
                "sandstone_5kg", new String[]{"Sandstone rocks", "5kg"}, "sandstone_2kg", new String[]{"Sandstone rocks", "2kg"},
                "sandstone_1kg", new String[]{"Sandstone rocks", "1kg"}, "granite_5kg", new String[]{"Granite rocks", "5kg"},
                "granite_2kg", new String[]{"Granite rocks", "2kg"}, "granite_500g", new String[]{"Granite rocks", "500g"});
        for (MiningData.MineRock r : MiningData.ROCKS) {
            if (r.cascades()) {
                for (MiningData.Yield y : r.yields()) {
                    String[] k = sizes.get(y.item());
                    assertNotNull(k, y.item());
                    check(y.item(), ref(all, k[0], "mining chance", "", k[1]), y.low(), y.high());
                }
            } else if (mod.containsKey(r.blockId())) {
                String[] k = mod.get(r.blockId());
                check(r.blockId(), ref(all, k[0], "mining chance", "", k[1]), r.low(), r.high());
            }
        }
        for (String id : List.of(MiningData.LEAD, MiningData.BARRONITE, MiningData.SULPHUR)) {
            assertNotNull(MiningData.rock(id), id);
        }
        // Crashed stars: one chart for every size (Crashed Star and Shooting Stars pages agree).
        check("stardust", ref(all, "Crashed Star", "stardust mining chance", "", "Stardust"), ShootingStars.LOW, ShootingStars.HIGH);
        check("stardust", ref(all, "Shooting Stars", "stardust mining chance", "", "Stardust"), ShootingStars.LOW, ShootingStars.HIGH);
    }

    // ------------------------------------------------------------------ Shooting Stars, aerial fishing, minnows

    @Test
    void shootingStarLandingSitesMatchTheWikiList() throws IOException {
        List<String[]> rows = rows("shooting_stars.tsv");
        assertEquals(rows.size(), ShootingStars.SITES.size());
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            ShootingStars.Site s = ShootingStars.SITES.get(i);
            assertEquals(r[0], s.region(), r[1]);
            assertEquals(r[1], s.name());
            assertEquals(Integer.parseInt(r[2]), s.x(), r[1]);
            assertEquals(Integer.parseInt(r[3]), s.y(), r[1]);
        }
        // Size 6-9 at 25/30/25/20%; 10 Mining per size; 32 XP; 7-minute layers; a crash every 90 +/- 15 minutes.
        assertEquals(6, ShootingStars.landingSize(0.0));
        assertEquals(6, ShootingStars.landingSize(0.249));
        assertEquals(7, ShootingStars.landingSize(0.25));
        assertEquals(8, ShootingStars.landingSize(0.55));
        assertEquals(9, ShootingStars.landingSize(0.80));
        assertEquals(9, ShootingStars.landingSize(0.9999));
        assertEquals(90, ShootingStars.level(9));
        assertEquals(10, ShootingStars.level(1));
        assertEquals(32.0, ShootingStars.XP, 1e-9);
        assertEquals(700, ShootingStars.LAYER_TICKS);
        assertEquals(7500, ShootingStars.nextCrashTicks(0.0));
        assertEquals(10500, ShootingStars.nextCrashTicks(0.99999));
        // Never the same site twice: a repeat goes to the Daeyalt essence mine entrance, which is never rolled itself.
        ShootingStars.Site first = ShootingStars.nextSite(null, 0.0);
        assertEquals("Dwarven Mine northern entrance", first.name());
        assertEquals(ShootingStars.REPEAT_SITE, ShootingStars.nextSite(first, 0.0).name());
        for (int i = 0; i < 1000; i++) {
            assertTrue(!ShootingStars.nextSite(null, i / 1000.0).name().equals(ShootingStars.REPEAT_SITE));
        }
        // Dusuri's Star Shop prices.
        Map<String, Integer> prices = new HashMap<>();
        for (ShootingStars.Reward r : ShootingStars.SHOP) {
            prices.put(r.give(), r.stardust());
        }
        assertEquals(Map.of("celestial_ring_uncharged", 2000, "star_fragment", 3000, "bag_full_of_gems", 300, "soft_clay", 150), prices);
    }

    @Test
    void aerialFishingFollowsModAshsFormulas() {
        // X = (2 x Fishing + Hunter) / 3; pearls 1 / (100 - (X - 40) x 25 / 59): X = 80 gives 1/83.05 (Mod Ash rounds it to 1/84).
        assertEquals(80, AerialFishing.x(80, 80));
        assertEquals(1.0 / (100.0 - 40 * 25.0 / 59.0), AerialFishing.pearlChance(80, 80), 1e-12);
        assertEquals(1.0 / 100.0, AerialFishing.pearlChance(40, 40), 1e-12);
        // Wiki table: levels and XP.
        Map<String, double[]> wiki = Map.of("bluegill", new double[]{43, 11.5, 35, 16.5, 3.5},
                "common_tench", new double[]{56, 40, 51, 45, 10}, "mottled_eel", new double[]{73, 65, 68, 90, 20},
                "greater_siren", new double[]{91, 100, 87, 130, 25});
        assertEquals(wiki.size(), AerialFishing.FISH.size());
        for (AerialFishing.Fish f : AerialFishing.FISH) {
            double[] w = wiki.get(f.item());
            assertEquals(w[0], f.fishing(), f.item());
            assertEquals(w[1], f.fishingXp(), 1e-9, f.item());
            assertEquals(w[2], f.hunter(), f.item());
            assertEquals(w[3], f.hunterXp(), 1e-9, f.item());
            assertEquals(w[4], f.cookingXp(), 1e-9, f.item());
        }
        // 82+ siren, 67+ eel, 52+ tench, else bluegill, each only with the stats.
        assertEquals("greater_siren", AerialFishing.catchFor(99, 99, 98).item());
        assertEquals("mottled_eel", AerialFishing.catchFor(99, 86, 98).item());
        assertEquals("mottled_eel", AerialFishing.catchFor(99, 99, 81).item());
        assertEquals("common_tench", AerialFishing.catchFor(99, 99, 52).item());
        assertEquals("bluegill", AerialFishing.catchFor(99, 99, 51).item());
        assertEquals("bluegill", AerialFishing.catchFor(55, 99, 70).item());
    }

    @Test
    void minnowsComeTenToFourteenPerCatch() {
        int[][] wiki = {{82, 10}, {85, 11}, {90, 12}, {95, 13}, {99, 14}};
        for (int[] w : wiki) {
            assertEquals(w[1], GatheringData.minnowsPerCatch(w[0]), "level " + w[0]);
        }
        GatheringData.Spot spot = GatheringData.spot(GatheringData.MINNOW_SPOT);
        assertEquals("small_fishing_net", spot.tool());
        assertEquals(82, spot.catches().get(0).level());
        assertEquals(26.1, spot.catches().get(0).xp(), 1e-9);
        assertEquals(40, GatheringData.MINNOWS_PER_SHARK);
    }
}
