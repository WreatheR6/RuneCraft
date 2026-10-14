package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Diffs the achievement diaries against a snapshot of the OSRS wiki task tables
 * ({@code src/test/resources/osrs/diaries/diary_tasks.tsv}, parsed from the "&lt;Area&gt; Diary" pages of the
 * Kiyogitpy/osrs-wiki-data dump and cross-checked with RuneLite's achievement diary requirement classes):
 * the task count of every tier, each task's skill requirements and its quest requirements.
 */
class DiaryReferenceTest {
    private static final String FIXTURE = "osrs/diaries/diary_tasks.tsv";

    /** Known deviations from the wiki task table (task id -> reason). */
    private static final Map<String, String> SKILL_EXCEPTIONS = Map.of(
            "ardougne_medium_5", "Crafting 49 for The Hand in the Sand (RuneLite lists it)",
            "ardougne_medium_10", "Magic 50 to wield Iban's staff (RuneLite lists it)",
            "falador_hard_9", "65 Attack and 65 Strength stand in for the 130 combined Warriors' Guild requirement",
            "fremennik_medium_5", "Hunter 35 with a butterfly net (the wiki's first line is 45 bare-handed; RuneLite lists 35)",
            "kandarin_hard_2", "Crafting 10 to spin the bow string (RuneLite lists it)",
            "karamja_hard_7", "Woodcutting 15 to chop palm leaves (RuneLite lists it)");

    record Ref(String area, Diaries.Tier tier, int n, String task, Map<Skill, Integer> skills, List<String> quests) {
    }

    private static List<String> fixtureLines() throws IOException {
        Path p = Path.of("src/test/resources", FIXTURE);
        if (Files.exists(p)) {
            return Files.readAllLines(p, StandardCharsets.UTF_8);
        }
        try (InputStream in = DiaryReferenceTest.class.getClassLoader().getResourceAsStream(FIXTURE)) {
            assertNotNull(in, "missing " + FIXTURE);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }

    static List<Ref> reference() throws IOException {
        List<Ref> out = new ArrayList<>();
        for (String line : fixtureLines()) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\t", -1);
            Map<Skill, Integer> skills = new EnumMap<>(Skill.class);
            for (String s : f[4].split(",")) {
                if (!s.isEmpty()) {
                    String[] kv = s.split(":");
                    skills.put(Skill.valueOf(kv[0]), Integer.parseInt(kv[1]));
                }
            }
            List<String> quests = f[5].isEmpty() ? List.of() : List.of(f[5].split(";"));
            out.add(new Ref(f[0], Diaries.Tier.valueOf(f[1]), Integer.parseInt(f[2]), f[3], skills, quests));
        }
        return out;
    }

    private static final Set<String> STOP = Set.of("the", "and", "from", "some", "your", "with", "you", "any", "for", "within", "inside");

    private static Set<String> tokens(String s) {
        Set<String> out = new HashSet<>();
        for (String w : s.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (w.length() > 2 && !STOP.contains(w)) {
                out.add(w);
            }
        }
        return out;
    }

    private static double similarity(String a, String b) {
        Set<String> x = tokens(a);
        Set<String> y = tokens(b);
        Set<String> union = new HashSet<>(x);
        union.addAll(y);
        x.retainAll(y);
        return union.isEmpty() ? 0 : (double) x.size() / union.size();
    }

    /** Pairs each wiki task with the most similar unused mod task of the same tier (by description words). */
    static Map<Ref, Diaries.Task> match(List<Ref> refs, List<String> unmatched) {
        Map<Ref, Diaries.Task> out = new LinkedHashMap<>();
        Map<String, Set<String>> used = new HashMap<>();
        for (Ref r : refs) {
            Set<String> u = used.computeIfAbsent(r.area() + r.tier(), k -> new HashSet<>());
            Diaries.Task best = null;
            double bestSim = 0;
            for (Diaries.Task t : Diaries.tasks(r.area(), r.tier())) {
                double sim = similarity(r.task(), t.description());
                if (!u.contains(t.id()) && sim > bestSim) {
                    best = t;
                    bestSim = sim;
                }
            }
            if (best == null || bestSim < 0.25) {
                unmatched.add(r.area() + " " + r.tier() + " " + r.n() + ": " + r.task());
                continue;
            }
            u.add(best.id());
            out.put(r, best);
        }
        return out;
    }

    private static String norm(String questName) {
        return questName.split("/")[0].replaceAll(" \\((mini)?quest\\)$", "").toLowerCase(Locale.ROOT);
    }

    @Test
    void taskCountsMatchTheWiki() throws IOException {
        Map<String, Integer> wiki = new LinkedHashMap<>();
        for (Ref r : reference()) {
            wiki.merge(r.area() + " " + r.tier(), 1, Integer::sum);
        }
        assertEquals(48, wiki.size(), "12 areas x 4 tiers in the snapshot");
        List<String> bad = new ArrayList<>();
        for (Diaries.Area a : Diaries.AREAS) {
            for (Diaries.Tier t : Diaries.Tier.values()) {
                int want = wiki.getOrDefault(a.id() + " " + t, 0);
                int have = Diaries.tasks(a.id(), t).size();
                if (want != have) {
                    bad.add(a.id() + " " + t + ": wiki " + want + ", mod " + have);
                }
            }
        }
        assertTrue(bad.isEmpty(), "task counts differ: " + bad);
    }

    @Test
    void everyWikiTaskHasAModTask() throws IOException {
        List<String> unmatched = new ArrayList<>();
        match(reference(), unmatched);
        assertTrue(unmatched.isEmpty(), "wiki tasks without a matching diary task: " + unmatched);
    }

    @Test
    void skillRequirementsMatchTheWiki() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Map.Entry<Ref, Diaries.Task> e : match(reference(), new ArrayList<>()).entrySet()) {
            Ref r = e.getKey();
            Diaries.Task t = e.getValue();
            if (SKILL_EXCEPTIONS.containsKey(t.id())) {
                continue;
            }
            Map<Skill, Integer> all = t.allReqs();
            r.skills().forEach((s, lvl) -> {
                if (!lvl.equals(all.get(s))) {
                    bad.add(t.id() + " " + s + ": wiki " + lvl + ", mod " + all.get(s));
                }
            });
            // Explicit requirements must all be on the wiki; a LEVEL/XP_IN trigger level may exceed it (e.g. a course's level).
            t.reqs().forEach((s, lvl) -> {
                if (!r.skills().containsKey(s)) {
                    bad.add(t.id() + " " + s + " " + lvl + " is not a wiki requirement");
                }
            });
        }
        assertTrue(bad.isEmpty(), "skill requirements differ from the wiki: " + bad);
    }

    @Test
    void questRequirementsMatchTheWiki() throws IOException {
        Map<String, String> byName = new HashMap<>();
        for (QuestInfo q : ContentRegistry.quests()) {
            byName.put(norm(q.displayName()), q.questId());
        }
        List<String> bad = new ArrayList<>();
        for (Map.Entry<Ref, Diaries.Task> e : match(reference(), new ArrayList<>()).entrySet()) {
            Diaries.Task t = e.getValue();
            Set<String> have = new HashSet<>(Diaries.questReqs(t));
            if (t.type() == Diaries.Type.QUEST) {
                have.add(t.target());
            }
            for (String name : e.getKey().quests()) {
                String id = byName.get(norm(name));
                if (id != null && !have.contains(id)) {
                    bad.add(t.id() + " needs " + id);
                }
            }
        }
        assertTrue(bad.isEmpty(), "quest requirements missing: " + bad);
        for (Diaries.Task t : Diaries.tasks()) {
            for (String q : Diaries.questReqs(t)) {
                assertNotNull(ContentRegistry.quest(q), t.id() + " needs unknown quest " + q);
            }
        }
    }

    @Test
    void mostTasksAreTrackedFaithfully() {
        int total = Diaries.tasks().size();
        long approx = Diaries.tasks().stream().filter(Diaries::isApprox).count();
        System.out.println("Diary tasks: " + total + ", tracked faithfully " + (total - approx) + ", approximated " + approx);
        assertEquals(492, total);
        assertTrue(total - approx >= 458, "faithfully tracked tasks: " + (total - approx));
    }

    @Test
    void mergedBossTasksNeedEveryBoss() {
        Diaries.Task t = Diaries.task("wilderness_elite_0");
        assertTrue(t.target().contains("&"));
        assertTrue(Diaries.kcMet(t.target(), 1, Map.of("artio", 1, "spindel", 1, "vetion", 1)));
        assertTrue(!Diaries.kcMet(t.target(), 1, Map.of("callisto", 1, "venenatis", 1)));
        // Removed tasks keep their ids retired, so the remaining ids don't shift.
        assertEquals("Teleport to Ghorrock", Diaries.task("wilderness_elite_4").description());
    }
}
