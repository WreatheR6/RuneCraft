package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Slayer data checked against machine-readable snapshots of its sources, committed under
 * {@code src/test/resources/osrs/slayer/}: the OSRS Wiki's master assignment tables, Slayer Rewards shop, reward
 * point table and superior slayer monster infoboxes (raw wikitext of July 2026, Kiyogitpy/osrs-wiki-data), and
 * RuneLite's slayer plugin task list.
 */
class SlayerWikiSnapshotTest {
    private static List<String> lines(String name) throws IOException {
        String fixture = "osrs/slayer/" + name;
        Path p = Path.of("src/test/resources", fixture);
        List<String> raw;
        if (Files.exists(p)) {
            raw = Files.readAllLines(p, StandardCharsets.UTF_8);
        } else {
            try (InputStream in = SlayerWikiSnapshotTest.class.getClassLoader().getResourceAsStream(fixture)) {
                assertNotNull(in, "missing " + fixture);
                raw = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
            }
        }
        List<String> out = new ArrayList<>();
        for (String l : raw) {
            if (!l.isBlank() && !l.startsWith("#")) {
                out.add(l);
            }
        }
        return out;
    }

    private static String norm(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /** RuneLite names some tasks in the plural where the wiki tables don't ("Kalphites"). */
    private static String plural(String s) {
        String n = norm(s);
        return n.endsWith("s") ? n : n + "s";
    }

    @Test
    void masterTablesMatchTheWiki() throws IOException {
        List<String> rows = lines("master_assignments.tsv");
        assertTrue(rows.get(0).startsWith("master\t"));
        Map<String, Map<String, String[]>> wiki = new LinkedHashMap<>();
        for (String row : rows.subList(1, rows.size())) {
            String[] f = row.split("\t", -1);
            wiki.computeIfAbsent(f[0], k -> new LinkedHashMap<>()).put(f[2], f);
        }
        assertEquals(9, wiki.size());
        List<String> bad = new ArrayList<>();
        for (Map.Entry<String, Map<String, String[]>> e : wiki.entrySet()) {
            SlayerMasters.Master m = SlayerMasters.get(e.getKey());
            assertNotNull(m, e.getKey());
            assertEquals(e.getValue().size(), m.assignments().size(), m.id() + " task count");
            for (String[] f : e.getValue().values()) {
                SlayerMasters.Assignment a = m.assignment(f[2]);
                if (a == null) {
                    bad.add(m.id() + " lacks " + f[2]);
                    continue;
                }
                String unlock = f[10].isEmpty() ? null : f[10];
                List<String> quests = f[11].isEmpty() ? List.of() : List.of(f[11].split(","));
                String want = f[3] + " " + f[4] + "-" + f[5] + " ext " + f[6] + "-" + f[7] + " cb " + f[9] + " " + unlock + " " + quests;
                String got = a.weight() + " " + a.min() + "-" + a.max() + " ext " + a.extMin() + "-" + a.extMax() + " cb "
                        + a.minCombat() + " " + a.unlock() + " " + a.quests();
                if (!want.equals(got)) {
                    bad.add(m.id() + " " + f[2] + ": wiki " + want + ", RuneCraft " + got);
                }
                // The category's Slayer level is the wiki's Slayer requirement for the task.
                int slayer = Integer.parseInt(f[8]);
                if (slayer > 0 && SlayerCategories.get(f[2]).level() != slayer) {
                    bad.add(f[2] + " Slayer level " + SlayerCategories.get(f[2]).level() + ", wiki " + slayer);
                }
                for (String q : quests) {
                    assertNotNull(ContentRegistry.quest(q.replace("~", "")), m.id() + " " + f[2] + " quest " + q);
                }
                // An extension range only exists where the shop sells the extension.
                if (a.extMin() > 0) {
                    assertNotNull(SlayerRewards.extensionFor(a.category()), "no extension unlock for " + a.category());
                }
            }
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
        assertEquals(SlayerMasters.get("nieve").assignments(), SlayerMasters.get("steve").assignments());
    }

    @Test
    void metalDragonsAreOneTask() {
        for (String id : List.of("bronze_dragon", "iron_dragon", "steel_dragon", "mithril_dragon", "adamant_dragon", "rune_dragon")) {
            assertEquals("metal_dragons", SlayerTasks.categoryOf(ContentRegistry.monster(id)), id);
        }
        for (String old : List.of("bronze_dragons", "mithril_dragons", "rune_dragons", "basilisk_knights")) {
            assertNull(SlayerCategories.get(old), old);
        }
        assertEquals("pedal_to_the_metals", SlayerRewards.extensionFor("metal_dragons").id());
        assertEquals(200, SlayerRewards.unlock("pedal_to_the_metals").cost());
        assertTrue(SlayerTasks.available("metal_dragons"));
        // Basilisk knights count as basilisks; Basilocked gates basilisks at Konar, Nieve and Duradel only.
        assertEquals("basilisks", SlayerTasks.categoryOf(ContentRegistry.monster("basilisk_knight")));
        assertEquals("basilocked", SlayerMasters.get("duradel").assignment("basilisks").unlock());
        assertNull(SlayerMasters.get("vannaka").assignment("basilisks").unlock());
        assertNotNull(SlayerGear.protection("basilisk_knight", "basilisks"));
        assertEquals(20, SlayerGear.protection("basilisk_knight", "basilisks").damage());
        assertEquals(12, SlayerGear.protection("basilisk", "basilisks").damage());
    }

    @Test
    void questAndCombatGates() {
        SlayerMasters.Master vannaka = SlayerMasters.get("vannaka");
        SlayerMasters.Assignment nechs = vannaka.assignment("nechryael");
        assertEquals(List.of("priest_in_peril"), nechs.quests());
        assertTrue(SlayerTasks.eligible(nechs, 99, 126, Set.of(), List.of(), null, q -> true));
        assertFalse(SlayerTasks.eligible(nechs, 99, 126, Set.of(), List.of(), null, q -> false));
        assertFalse(SlayerTasks.eligible(nechs, 99, 84, Set.of(), List.of(), null, q -> true)); // combat 85
        assertEquals(List.of("~dragon_slayer_i"), vannaka.assignment("blue_dragons").quests());
        // Krystilia's jellies, dust devils, nechryael and abyssal demons need the free "I Wildy More Slayer" toggle.
        SlayerMasters.Master krystilia = SlayerMasters.get("krystilia");
        for (String c : List.of("abyssal_demons", "dust_devils", "jellies", "nechryael")) {
            assertEquals("i_wildy_more_slayer", krystilia.assignment(c).unlock(), c);
        }
        assertEquals(0, SlayerRewards.unlock("i_wildy_more_slayer").cost());
        // Konar assigns hydras; Nieve and Duradel don't.
        assertNotNull(SlayerMasters.get("konar").assignment("hydras"));
        assertNull(SlayerMasters.get("duradel").assignment("hydras"));
        // Never assigned: blocked on that master's list only.
        List<String> entries = List.of(SlayerRewards.blockKey("duradel", "dark_beasts"), SlayerRewards.blockKey("spria", "bats"),
                "cows");
        assertEquals(List.of("dark_beasts", "cows"), SlayerRewards.blockedFor(entries, "duradel"));
        assertEquals(List.of("cows"), SlayerRewards.blockedFor(entries, "nieve"));
        assertEquals(List.of("bats", "cows"), SlayerRewards.blockedFor(entries, "turael"));
        assertEquals("nieve", SlayerRewards.blockList("steve"));
        Random random = new Random(3);
        SlayerMasters.Master duradel = SlayerMasters.get("duradel");
        List<String> blocked = SlayerRewards.blockedFor(entries, "duradel");
        for (int i = 0; i < 300; i++) {
            SlayerMasters.Assignment a = duradel.pick(random,
                    x -> SlayerTasks.eligible(x, 99, 126, Set.of(), blocked, null, q -> !q.contains("dragon_slayer")));
            assertFalse(a.category().equals("dark_beasts"));
            assertTrue(a.quests().stream().noneMatch(q -> q.contains("dragon_slayer")), a.category());
        }
        assertTrue(SlayerMasters.get("turael").changesTasks());
        assertFalse(SlayerMasters.get("spria").changesTasks());
    }

    @Test
    void pointsMatchTheWiki() throws IOException {
        int[] streaks = {1, 10, 50, 100, 250, 1000};
        for (String row : lines("points.txt")) {
            String[] f = row.split("\\|");
            SlayerMasters.Master m = SlayerMasters.get(f[0]);
            assertNotNull(m, f[0]);
            assertEquals(Integer.parseInt(f[1]), m.points(), f[0]);
            for (int i = 1; i < streaks.length; i++) {
                assertEquals(Integer.parseInt(f[i + 1]), m.pointsFor(streaks[i]), f[0] + " task " + streaks[i]);
            }
            assertEquals(0, m.pointsFor(4), f[0] + ": nothing for the first four tasks");
            int diary = Integer.parseInt(f[7]);
            assertEquals(diary > 0 ? diary : m.points(), m.pointsFor(7, true), f[0] + " elite diary");
            assertEquals(m.points(), m.pointsFor(7, false));
        }
        assertTrue(SlayerMasters.get("krystilia").wilderness());
    }

    @Test
    void rewardShopMatchesTheWiki() throws IOException {
        Map<String, SlayerRewards.Unlock> ours = new HashMap<>();
        for (SlayerRewards.Unlock u : SlayerRewards.UNLOCKS) {
            ours.put(norm(u.name()), u);
        }
        // Duly Noted and Task Storage aren't modelled; the twisted horns, slayer hood and demonic quill don't exist yet.
        Set<String> notOffered = Set.of(norm("Duly Noted"), norm("Task Storage"), norm("Twisted Vision"), norm("Absolutely Slayin'"),
                norm("Oath Breaker"));
        Set<String> seen = new HashSet<>();
        List<String> bad = new ArrayList<>();
        Map<String, Integer> buy = new HashMap<>();
        for (String row : lines("reward_shop.txt")) {
            String[] f = row.split("\\|");
            int cost = Integer.parseInt(f[2]);
            switch (f[0]) {
                case "unlock", "extend", "cosmetic" -> {
                    String key = norm(f[1]);
                    seen.add(key);
                    SlayerRewards.Unlock u = ours.get(key);
                    if (u == null) {
                        if (!notOffered.contains(key)) {
                            bad.add("missing " + f[1]);
                        }
                    } else if (u.cost() != cost) {
                        bad.add(f[1] + " costs " + u.cost() + ", wiki " + cost);
                    } else if (f[0].equals("extend") != (u.kind() == SlayerRewards.Kind.EXTEND)
                            || f[0].equals("cosmetic") != (u.kind() == SlayerRewards.Kind.COSMETIC)) {
                        bad.add(f[1] + " is in the wrong tab");
                    }
                }
                case "buy" -> buy.put(norm(f[1].replaceAll("\\s*\\(x\\d+\\)", "")), cost);
                case "task" -> {
                    if (f[1].equals("Cancel task")) {
                        assertEquals(cost, SlayerRewards.SKIP_COST);
                    }
                }
                default -> bad.add("unknown tab " + f[0]);
            }
        }
        for (String key : ours.keySet()) {
            if (!seen.contains(key)) {
                bad.add(ours.get(key).name() + " is not an OSRS reward");
            }
        }
        for (SlayerRewards.PointBuy b : SlayerRewards.BUY) {
            Integer cost = buy.get(norm(ItemCatalog.get(b.item()).name().replaceAll("\\s*\\(\\d+\\)$", "")));
            if (cost != null) {
                assertEquals((int) cost, b.cost(), b.item());
            }
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
        int[] block = {40, 40, 50, 60, 70, 80, 90, 90, 100, 100};
        String[] ids = {"turael", "spria", "mazchna", "vannaka", "chaeldar", "konar", "nieve", "steve", "duradel", "krystilia"};
        for (int i = 0; i < ids.length; i++) {
            assertEquals(block[i], SlayerRewards.blockCost(ids[i]), ids[i]);
        }
        assertEquals(35, SlayerRewards.pointBuy("broad_bolts").cost());
        assertEquals(250, SlayerRewards.pointBuy("broad_bolts").amount());
        assertNotNull(SlayerRewards.craft("slayer_ring_eternal"));
    }

    @Test
    void superiorsMatchTheWiki() throws IOException {
        Set<String> seen = new HashSet<>();
        for (String row : lines("superiors.txt")) {
            String[] f = row.split("\\|");
            Superiors.Superior s = Superiors.byId(f[0]);
            assertNotNull(s, f[0]);
            seen.add(f[0]);
            assertEquals(Integer.parseInt(f[3]), s.level(), f[0]);
            assertEquals(Integer.parseInt(f[4]), s.hp(), f[0]);
            assertEquals(Integer.parseInt(f[5]), s.slayer(), f[0]);
            assertEquals(Double.parseDouble(f[6]), s.xp(), 1e-9, f[0]);
            MonsterDef sup = ContentRegistry.monster(f[0]);
            assertNotNull(sup, f[0]);
            assertEquals(Integer.parseInt(f[3]), sup.combatLevel, f[0]);
            assertEquals(Math.min(Superiors.MAX_HP, Integer.parseInt(f[4])), sup.hitpoints, f[0]);
            MonsterDef base = ContentRegistry.monster(s.base());
            assertNotNull(base, s.base());
            assertEquals(SlayerTasks.categoryOf(base), sup.slayerCategory, f[0]);
            int[] r = Superiors.uniqueRarities(s.slayer());
            // (a choke devil also keeps the dust devil's own 1/4000 dust battlestaff line)
            assertTrue(has(sup, "imbued_heart", r[0]) && has(sup, "eternal_gem", r[1])
                    && has(sup, "dust_battlestaff", r[2]) && has(sup, "mist_battlestaff", r[3]), f[0]);
        }
        assertEquals(seen, Superiors.all().keySet());
        assertEquals(Superiors.byId("nechryarch"), Superiors.forBase("greater_nechryael"));
        // 1/(200 - (s+55)^2/125): the wiki's per-monster unique table rates.
        assertEquals(171.2, Superiors.uniqueTableDenominator(5), 1e-9);
        assertEquals(43.2, Superiors.uniqueTableDenominator(85), 1e-9);
        assertEquals(20.0, Superiors.uniqueTableDenominator(95), 1e-9);
        assertEquals(1370, rate(ContentRegistry.monster("crushing_hand"), "imbued_heart")); // 8 x 171.2
        assertEquals(46, rate(ContentRegistry.monster("colossal_hydra"), "dust_battlestaff")); // 16/7 x 20
        assertEquals(200, SlayerRewards.SUPERIOR_CHANCE);
    }

    @Test
    void runeliteTaskListCoversEveryCategory() throws IOException {
        Map<String, Set<String>> targets = new HashMap<>();
        Map<String, Integer> thresholds = new HashMap<>();
        for (String row : lines("runelite_tasks.txt")) {
            String[] f = row.split("\\|", -1);
            Set<String> names = new HashSet<>();
            for (String t : f[4].split(";")) {
                names.add(norm(t));
            }
            targets.put(plural(f[1]), names);
            if (!f[2].isEmpty() && !f[2].equals("0")) {
                thresholds.put(plural(f[1]), Integer.parseInt(f[2]));
            }
        }
        Set<String> categories = new HashSet<>();
        for (String row : lines("master_assignments.tsv").subList(1, lines("master_assignments.tsv").size())) {
            categories.add(row.split("\t")[2]);
        }
        List<String> missing = new ArrayList<>();
        for (String c : categories) {
            if (c.equals(SlayerBossTasks.CATEGORY)) {
                continue;
            }
            if (!targets.containsKey(plural(SlayerCategories.get(c).name()))) {
                missing.add(c);
            }
        }
        assertTrue(missing.isEmpty(), "categories RuneLite doesn't know: " + missing);
        // Superiors that RuneLite names count towards their base task.
        for (Superiors.Superior s : Superiors.all().values()) {
            Set<String> names = targets.get(plural(SlayerCategories.get(ContentRegistry.monster(s.id()).slayerCategory).name()));
            assertNotNull(names, s.id());
        }
        assertTrue(targets.get(plural("Metal dragons")).contains(norm("Rune dragon")));
        // Creatures that must be finished off with an item die at RuneLite's thresholds.
        for (Map.Entry<String, Integer> t : thresholds.entrySet()) {
            SlayerGear.Finisher f = SlayerGear.FINISHERS.stream()
                    .filter(x -> plural(SlayerCategories.get(x.category()).name()).equals(t.getKey())).findFirst().orElse(null);
            assertNotNull(f, t.getKey());
            assertEquals((int) t.getValue(), f.threshold(), t.getKey());
        }
    }

    private static boolean has(MonsterDef m, String item, int rarity) {
        return m.drops.stream().anyMatch(d -> d.item().equals(item) && d.rarity() == rarity);
    }

    private static int rate(MonsterDef m, String item) {
        for (Drop d : m.drops) {
            if (d.item().equals(item)) {
                return d.rarity();
            }
        }
        return -1;
    }
}
