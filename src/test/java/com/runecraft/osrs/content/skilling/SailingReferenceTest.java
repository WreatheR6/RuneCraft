package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.extras.Seafaring;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sailing diffed against the snapshots in src/test/resources/osrs/sailing (OSRS Wiki July 2026 dump and
 * RuneLite's SailingAction): courier tasks, sea charting tasks and seas, boat parts, the level up table and
 * the skill calculator's XP values.
 */
class SailingReferenceTest {

    private static List<String[]> tsv(String name) throws Exception {
        List<String[]> out = new ArrayList<>();
        Path p = Path.of("src/test/resources/osrs/sailing", name);
        assertTrue(Files.exists(p), p.toString());
        for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
            if (!line.isBlank() && !line.startsWith("#")) {
                out.add(line.split("\t", -1));
            }
        }
        return out;
    }

    private static String portName(String id) {
        SailingData.Port p = SailingData.port(id);
        assertNotNull(p, id);
        return p.name();
    }

    @Test
    void courierTasksMatchTheWiki() throws Exception {
        List<String[]> rows = tsv("courier_tasks.tsv");
        assertEquals(439, rows.size());
        assertEquals(rows.size(), SailingTraining.COURIER.size());
        for (String[] r : rows) {
            SailingTraining.CourierTask t = SailingTraining.courier(Integer.parseInt(r[0]));
            assertNotNull(t, r[0]);
            assertEquals(Integer.parseInt(r[1]), t.level(), r[0]);
            if (r[2].isEmpty()) {
                assertTrue(Double.isNaN(t.xp()) && SailingTraining.courierXp(t) > 0, r[0] + " estimated XP");
            } else {
                assertEquals(Double.parseDouble(r[2]), t.xp(), 1e-9, r[0]);
                assertEquals(t.xp(), SailingTraining.courierCoins(t), 0.5, r[0] + " coins equal XP");
            }
            assertEquals(r[3], portName(t.board()), r[0]);
            assertEquals(r[4], portName(t.cargo()), r[0]);
            assertEquals(r[5], portName(t.destination()), r[0]);
            assertEquals(r[6], t.cargoName(), r[0]);
            assertEquals(Integer.parseInt(r[7]), t.qty(), r[0]);
            // The task level is at least the docking level of every port it uses.
            for (String port : List.of(t.board(), t.cargo(), t.destination())) {
                assertTrue(SailingData.port(port).level() <= t.level() || port.equals("red_rock"), r[0] + " " + port);
            }
        }
        assertEquals(1, SailingTraining.taskSlots(6));
        assertEquals(2, SailingTraining.taskSlots(7));
        assertEquals(3, SailingTraining.taskSlots(28));
        assertEquals(4, SailingTraining.taskSlots(56));
        assertEquals(5, SailingTraining.taskSlots(84));
        assertFalse(SailingTraining.boardTasks("port_sarim", 1).isEmpty());
    }

    @Test
    void seaChartingMatchesTheWiki() throws Exception {
        List<String[]> rows = tsv("sea_charts.tsv");
        assertEquals(358, rows.size());
        assertEquals(rows.size(), SailingTraining.CHARTS.size());
        for (String[] r : rows) {
            SailingTraining.ChartTask t = SailingTraining.chart(Integer.parseInt(r[0]));
            assertNotNull(t, r[0]);
            assertEquals(Integer.parseInt(r[1]), t.level(), r[0]);
            assertEquals(r[2].toUpperCase(Locale.ROOT), t.type().name(), r[0]);
            assertEquals(r[3], t.hazard(), r[0]);
            assertEquals(r[4], t.sea(), r[0]);
            assertEquals(r[5], t.ocean(), r[0]);
            assertEquals(Integer.parseInt(r[6]), t.x(), r[0]);
            assertEquals(Integer.parseInt(r[7]), t.y(), r[0]);
            assertTrue(t.level() >= t.type().level, r[0] + " task level covers its tool");
        }
        // Tool XP (wiki tool table).
        assertEquals(35, SailingTraining.ChartType.GENERIC.xp);
        assertEquals(50, SailingTraining.ChartType.SPYGLASS.xp);
        assertEquals(75, SailingTraining.ChartType.CRATE.xp);
        assertEquals(125, SailingTraining.ChartType.CURRENT.xp);
        assertEquals(175, SailingTraining.ChartType.DIVING.xp);
        assertEquals(250, SailingTraining.ChartType.WEATHER.xp);
        for (SailingTraining.ChartType c : SailingTraining.ChartType.values()) {
            if (c.tool != null) {
                assertTrue(ItemCatalog.exists(c.tool), c.tool);
            }
        }

        List<String[]> seas = tsv("seas.tsv");
        int n = 0;
        Map<String, Double> oceans = new HashMap<>();
        for (String[] s : seas) {
            if (s[0].equals("OCEAN")) {
                oceans.put(s[1], Double.parseDouble(s[2]));
                continue;
            }
            SailingTraining.Sea sea = SailingTraining.SEAS.get(n++);
            assertEquals(s[0], sea.ocean());
            assertEquals(s[1], sea.name());
            assertEquals(Integer.parseInt(s[2]), sea.level(), s[1]);
            assertEquals(Double.parseDouble(s[4]), sea.xp(), 1e-9, s[1]);
            long tasks = SailingTraining.CHARTS.stream().filter(c -> c.ocean().equals(sea.ocean()) && c.sea().equals(sea.name())).count();
            assertTrue(tasks > 0, sea.name() + " has tasks");
        }
        assertEquals(n, SailingTraining.SEAS.size());
        assertEquals(70, n);
        assertEquals(SailingTraining.ALL_CHARTED_BONUS, oceans.remove("All"));
        assertEquals(oceans, SailingTraining.OCEAN_BONUS);
    }

    @Test
    void chartRewardsAddSeaAndOceanBonuses() {
        // Completing every Ardent-ocean task of the Kharidian Sea gives its sea bonus with the last task.
        Set<Integer> done = new HashSet<>();
        SailingTraining.ChartTask last = null;
        for (SailingTraining.ChartTask c : SailingTraining.CHARTS) {
            if (c.sea().equals("Kharidian Sea") && c.ocean().equals("Ardent Ocean")) {
                done.add(c.id());
                last = c;
            }
        }
        assertNotNull(last);
        List<SailingTraining.ChartReward> rewards = SailingTraining.chartRewards(last, done);
        assertEquals(2, rewards.size());
        assertEquals(190, rewards.get(1).xp());
        // Everything charted: task, sea, ocean and the 100,000 XP bonus.
        Set<Integer> all = new HashSet<>();
        SailingTraining.CHARTS.forEach(c -> all.add(c.id()));
        List<SailingTraining.ChartReward> end = SailingTraining.chartRewards(last, all);
        assertEquals(4, end.size());
        assertEquals(100_000, end.get(3).xp());
        assertNotNull(SailingTraining.canChart(SailingTraining.chart(11), 11, Set.of("crowbar")));
        assertNull(SailingTraining.canChart(SailingTraining.chart(11), 12, Set.of("crowbar")));
        assertNotNull(SailingTraining.canChart(SailingTraining.chart(11), 50, Set.of()));
    }

    @Test
    void boatPartsMatchTheWiki() throws Exception {
        List<String[]> rows = tsv("boat_parts.tsv");
        assertEquals(77, rows.size());
        assertEquals(rows.size(), SailingTraining.PARTS.size());
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            SailingTraining.Part p = SailingTraining.PARTS.get(i);
            String what = r[0] + " " + r[1] + " " + r[4];
            assertEquals(r[0].toUpperCase(Locale.ROOT), p.type().name(), what);
            assertEquals(r[1], p.boat(), what);
            assertEquals(Integer.parseInt(r[2]), p.sailing(), what);
            assertEquals(Integer.parseInt(r[3]), p.construction(), what);
            assertEquals(r[4], p.name(), what);
            assertEquals(Double.parseDouble(r[6]), p.conXp(), 1e-9, what);
            assertEquals(r[7], p.stat(), what);
            p.materials().forEach((item, qty) -> assertTrue(ItemCatalog.exists(item), what + " needs " + item));
        }
        // Tier 0 is the part a boat comes with; trimming XP follows the sails (wiki: 10.5 on a new raft).
        assertEquals(10.5, SailingTraining.trimXp("raft", 0));
        assertEquals(125, SailingTraining.trimXp("sloop", 6));
        assertEquals(7, SailingTraining.parts("skiff", SailingTraining.PartType.HULL).size());
        assertTrue(SailingTraining.parts("raft", SailingTraining.PartType.KEEL).isEmpty());
        SailingTraining.Part oak = SailingTraining.part("skiff", SailingTraining.PartType.HULL, 1);
        assertEquals(50, oak.materials().get("oak_plank"), "10 hull parts = 50 planks");
        assertNull(SailingTraining.canBuild(oak, 20, 8, 0));
        assertNotNull(SailingTraining.canBuild(oak, 19, 8, 0));
        assertNotNull(SailingTraining.canBuild(oak, 20, 7, 0));
        assertNotNull(SailingTraining.canBuild(oak, 99, 99, 1));
        for (SailingTraining.Facility f : SailingTraining.FACILITIES) {
            f.materials().keySet().forEach(item -> assertTrue(ItemCatalog.exists(item), f.id() + " needs " + item));
        }
    }

    private static final Pattern BUILD = Pattern.compile("Build (?:and use )?an? (.+?) \\(with Construction (\\d+)\\)");

    @Test
    void levelUpTableMatchesTheMod() throws Exception {
        int checked = 0;
        for (String[] r : tsv("level_unlocks.tsv")) {
            int level = Integer.parseInt(r[0]);
            String u = r[1];
            Matcher m;
            if ((m = Pattern.compile("^Dock at (?:the )?(.+?)(?: \\(with .*)?$").matcher(u)).matches()) {
                for (String name : m.group(1).split(" and ")) {
                    for (Seafaring.Port p : Seafaring.PORTS) {
                        if (p.name().equalsIgnoreCase(name) || p.name().equalsIgnoreCase("The " + name)) {
                            assertEquals(level, p.level(), u);
                            checked++;
                        }
                    }
                }
            } else if ((m = Pattern.compile("^Take port tasks to/from (?:the )?(.+)$").matcher(u)).matches()) {
                for (SailingData.Port p : SailingData.PORTS) {
                    if (p.name().equalsIgnoreCase(m.group(1)) || p.name().equalsIgnoreCase("The " + m.group(1))) {
                        assertEquals(level, p.level(), u);
                        checked++;
                    }
                }
            } else if ((m = Pattern.compile("^Salvage (.+) shipwrecks$").matcher(u)).matches()) {
                String name = m.group(1);
                SailingData.Wreck w = SailingData.WRECKS.stream().filter(x -> x.name().equalsIgnoreCase(name + " shipwreck")).findFirst()
                        .orElse(null);
                assertNotNull(w, u);
                assertEquals(level, w.level(), u);
                checked++;
            } else if ((m = Pattern.compile("^Sail a (\\w+)$").matcher(u)).matches()) {
                String name = m.group(1);
                assertEquals(level, SailingData.BOATS.stream().filter(b -> b.name().equalsIgnoreCase(name)).findFirst().orElseThrow().level());
                checked++;
            } else if ((m = Pattern.compile("^Complete Barracuda Trials at (.+?)(?: \\(with .*)?$").matcher(u)).matches()) {
                String name = m.group(1);
                assertEquals(level, SailingTraining.TRIALS.stream().filter(t -> t.name().equals(name)).findFirst().orElseThrow().level(), u);
                checked++;
            } else if ((m = Pattern.compile("^Have (?:up to )?(\\d) port tasks? at a time$").matcher(u)).matches()) {
                assertEquals(Integer.parseInt(m.group(1)), SailingTraining.taskSlots(level), u);
                if (level > 1) {
                    assertEquals(Integer.parseInt(m.group(1)) - 1, SailingTraining.taskSlots(level - 1), u);
                }
                checked++;
            } else if ((m = BUILD.matcher(u)).find()) {
                checked += checkBuild(level, m.group(1).toLowerCase(Locale.ROOT), Integer.parseInt(m.group(2)), u);
            }
        }
        assertTrue(checked >= 90, "checked " + checked);
    }

    private static int checkBuild(int level, String name, int con, String u) {
        String n = name.replace("innoculation", "inoculation");
        for (SailingTraining.Facility f : SailingTraining.FACILITIES) {
            if (f.name().equalsIgnoreCase(n)) {
                assertEquals(level, f.sailing(), u);
                assertEquals(con, f.construction(), u);
                return 1;
            }
        }
        int min = Integer.MAX_VALUE;
        int minCon = 0;
        for (SailingTraining.Part p : SailingTraining.PARTS) {
            String pn = p.name().toLowerCase(Locale.ROOT);
            String key = switch (p.type()) {
                case HULL -> pn.replace(" raft", " hull").replace(" skiff", " hull").replace(" sloop", " hull");
                case KEEL -> pn.replace(" skiff", "").replace(" sloop", "");
                default -> pn;
            };
            if (key.equals(n) && p.sailing() < min) {
                min = p.sailing();
                minCon = p.construction();
            }
        }
        if (min == Integer.MAX_VALUE) {
            return 0; // cannons, cargo holds, salvaging hooks, kegs... not built in RuneCraft
        }
        assertEquals(level, min, u);
        assertEquals(con, minCon, u);
        return 1;
    }

    @Test
    void runeliteCalculatorXpMatches() throws Exception {
        Map<String, Double> rl = new HashMap<>();
        for (String[] r : tsv("runelite_sailing_actions.tsv")) {
            rl.put(r[0], Double.parseDouble(r[3]));
            if (r[4].equals("SALVAGING") || r[4].equals("SORTING")) {
                SailingData.Wreck w = SailingData.WRECKS.stream().filter(x -> x.salvageName().startsWith(r[1].split(" ")[0]))
                        .findFirst().orElseThrow();
                assertEquals(Integer.parseInt(r[2]), w.level(), r[0]);
                assertEquals(Double.parseDouble(r[3]), r[4].equals("SALVAGING") ? w.xp() : w.sortXp(), 1e-9, r[0]);
            }
        }
        String[][] trials = {{"tempor_tantrum", "TEMPEST"}, {"jubbly_jive", "JIVE"}, {"gwenith_glide", "GLIDE"}};
        for (String[] t : trials) {
            SailingTraining.Trial trial = SailingTraining.trial(t[0]);
            for (SailingTraining.Rank rank : trial.ranks()) {
                double expected = rl.get(t[1] + "_TRIAL_" + rank.name().toUpperCase(Locale.ROOT));
                // RuneLite leaves out Tempor Tantrum's rum deliveries; Jubbly Jive's Marlin is 28 XP above the wiki formula.
                double ours = t[0].equals("tempor_tantrum") ? rank.base() + rank.crates() * rank.crateXp() : rank.xp();
                assertEquals(expected, ours, t[0].equals("jubbly_jive") && rank.name().equals("Marlin") ? 28 : 1e-9,
                        t[0] + " " + rank.name());
            }
        }
    }
}
