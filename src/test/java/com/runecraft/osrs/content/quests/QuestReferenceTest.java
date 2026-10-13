package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.Quest;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.QuestDef;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every quest against the OSRS reference snapshot {@code src/test/resources/osrs/quests/quest_reference.tsv}
 * (generated from the OSRS Wiki dump and quest-helper by {@code build_quest_reference.py} next to it).
 *
 * <p>Checked per quest: quest points, difficulty, skill requirements, quest points needed to start, combat level
 * gate, prerequisites and fixed XP rewards. Stage-level gates and XP ({@code reqSkill}, {@code reqDone},
 * {@code reqQp}, {@code reqCombat}, {@code grantXp}) count towards a quest's effective data, so staged quests such as
 * Recipe for Disaster compare as a whole. Prerequisites compare transitively: every direct OSRS prerequisite must be
 * reachable through the mod's prerequisite chain, and every direct mod prerequisite must be in the OSRS chain (or a
 * quest OSRS only needs started, which the mod enforces as completed).
 */
class QuestReferenceTest {
    private static final String FIXTURE = "osrs/quests/quest_reference.tsv";

    record Ref(String id, String name, String type, String difficulty, int qp, Map<Skill, Integer> skills, int minQp,
               int combat, List<String> quests, List<String> started, Map<Skill, Double> xp, boolean xpChoice, String note) {
        boolean staged() {
            return note.contains("staged");
        }
    }

    static List<String> fixtureLines() throws IOException {
        Path p = Path.of("src/test/resources", FIXTURE);
        if (Files.exists(p)) {
            return Files.readAllLines(p, StandardCharsets.UTF_8);
        }
        try (InputStream in = QuestReferenceTest.class.getClassLoader().getResourceAsStream(FIXTURE)) {
            assertTrue(in != null, "missing " + FIXTURE);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }

    private static <V> Map<Skill, V> skillMap(String s, java.util.function.Function<String, V> parse) {
        Map<Skill, V> out = new EnumMap<>(Skill.class);
        if (!s.equals("-")) {
            for (String part : s.split(";")) {
                String[] kv = part.split(":");
                out.put(Skill.valueOf(kv[0]), parse.apply(kv[1]));
            }
        }
        return out;
    }

    private static List<String> list(String s) {
        return s.equals("-") ? List.of() : List.of(s.split(","));
    }

    static List<Ref> reference() throws IOException {
        List<Ref> out = new ArrayList<>();
        for (String line : fixtureLines()) {
            if (line.isBlank() || line.startsWith("#") || line.startsWith("id\t")) {
                continue;
            }
            String[] c = line.split("\t", -1);
            out.add(new Ref(c[0], c[1], c[2], c[3], Integer.parseInt(c[4]), skillMap(c[5], Integer::parseInt),
                    Integer.parseInt(c[6]), Integer.parseInt(c[7]), list(c[8]), list(c[9]), skillMap(c[10], Double::parseDouble),
                    c[11].equals("yes"), c[13]));
        }
        return out;
    }

    /** The mod's effective view of one quest: quest-level data plus every stage-level gate and XP grant. */
    record Mod(Map<Skill, Integer> skills, List<String> quests, int minQp, int combat, Map<Skill, Double> xp) {
    }

    static Mod mod(QuestInfo q) {
        Map<Skill, Integer> skills = new EnumMap<>(Skill.class);
        Map<Skill, Double> xp = new EnumMap<>(Skill.class);
        List<String> quests = new ArrayList<>();
        int minQp = 0;
        int combat = 0;
        skills.putAll(q.requirements());
        xp.putAll(q.xpRewards());
        if (q instanceof QuestDef d) {
            quests.addAll(d.questRequirements());
        }
        QuestSpec spec = QuestBook.get().spec(q.questId());
        if (spec != null) {
            minQp = spec.minQuestPoints;
            for (QuestSpec.Trigger t : spec.triggers()) {
                t.skills.forEach((s, lv) -> skills.merge(s, lv, Math::max));
                t.xp.forEach((s, v) -> xp.merge(s, v, Double::sum));
                for (String r : t.quests) {
                    if (!quests.contains(r)) {
                        quests.add(r);
                    }
                }
                minQp = Math.max(minQp, t.minQuestPoints);
                combat = Math.max(combat, t.minCombat);
            }
        }
        return new Mod(skills, quests, minQp, combat, xp);
    }

    private static Set<String> closure(Map<String, List<String>> graph, List<String> start) {
        Set<String> seen = new HashSet<>();
        List<String> todo = new ArrayList<>(start);
        while (!todo.isEmpty()) {
            String q = todo.remove(todo.size() - 1);
            if (seen.add(q)) {
                todo.addAll(graph.getOrDefault(q, List.of()));
            }
        }
        return seen;
    }

    @Test
    void everyQuestMatchesTheOsrsReference() throws IOException {
        List<Ref> refs = reference();
        Map<String, List<String>> refGraph = new LinkedHashMap<>();
        Map<String, List<String>> modGraph = new LinkedHashMap<>();
        Map<String, Mod> mods = new LinkedHashMap<>();
        for (QuestInfo q : ContentRegistry.quests()) {
            Mod m = mod(q);
            mods.put(q.questId(), m);
            modGraph.put(q.questId(), m.quests());
        }
        for (Ref r : refs) {
            List<String> all = new ArrayList<>(r.quests());
            all.addAll(r.started());
            refGraph.put(r.id(), all);
        }

        Map<String, List<String>> problems = new LinkedHashMap<>();
        java.util.function.BiConsumer<String, String> fail = (cat, msg) -> problems.computeIfAbsent(cat, k -> new ArrayList<>()).add(msg);
        int checked = 0;
        for (Ref r : refs) {
            if (r.id().equals("-")) {
                fail.accept("missing quest", r.name() + " (" + r.qp() + " QP)");
                continue;
            }
            QuestInfo q = ContentRegistry.quest(r.id());
            if (q == null) {
                fail.accept("missing quest", r.id());
                continue;
            }
            checked++;
            Mod m = mods.get(r.id());
            if (q.questPoints() != r.qp()) {
                fail.accept("quest points", r.id() + ": " + q.questPoints() + " != " + r.qp());
            }
            if (!r.type().equals("MINI") && !r.difficulty().equals("Special") && !r.difficulty().equals("-")
                    && !q.difficulty().name().equalsIgnoreCase(r.difficulty())) {
                fail.accept("difficulty", r.id() + ": " + q.difficulty() + " != " + r.difficulty());
            }
            if (r.staged()) {
                // Cumulative over subquests: no stage may ask for more than OSRS does.
                m.skills().forEach((s, lv) -> {
                    if (lv > r.skills().getOrDefault(s, 0)) {
                        fail.accept("skills", r.id() + ": " + s + " " + lv + " exceeds " + r.skills().getOrDefault(s, 0));
                    }
                });
            } else if (!m.skills().equals(r.skills())) {
                fail.accept("skills", r.id() + ": " + m.skills() + " != " + r.skills());
            }
            if (m.minQp() != r.minQp()) {
                fail.accept("quest points to start", r.id() + ": " + m.minQp() + " != " + r.minQp());
            }
            if (m.combat() != r.combat()) {
                fail.accept("combat level", r.id() + ": " + m.combat() + " != " + r.combat());
            }
            Set<String> modChain = closure(modGraph, m.quests());
            for (String need : r.quests()) {
                if (!modChain.contains(need)) {
                    fail.accept("prerequisites", r.id() + ": missing " + need);
                }
            }
            List<String> refStart = new ArrayList<>(r.quests());
            refStart.addAll(r.started());
            Set<String> refChain = closure(refGraph, refStart);
            for (String has : m.quests()) {
                if (!refChain.contains(has)) {
                    fail.accept("prerequisites", r.id() + ": not an OSRS prerequisite: " + has);
                }
            }
            r.xp().forEach((s, v) -> {
                Double got = m.xp().get(s);
                if (got == null || Math.abs(got - v) > 0.01) {
                    fail.accept("xp", r.id() + ": " + s + " " + got + " != " + v);
                }
            });
            for (Skill s : m.xp().keySet()) {
                if (!r.xp().containsKey(s) && !r.xpChoice()) {
                    fail.accept("xp", r.id() + ": " + s + " " + m.xp().get(s) + " is not an OSRS reward");
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        problems.forEach((cat, list) -> {
            sb.append("\n== ").append(cat).append(" (").append(list.size()).append(")");
            list.forEach(x -> sb.append("\n  ").append(x));
        });
        assertTrue(problems.isEmpty(), sb.toString());
        assertTrue(checked >= 200, "checked " + checked);
    }

    @Test
    void questPointTotalMatchesOsrs() throws IOException {
        int osrs = 0;
        Set<String> ids = new HashSet<>();
        for (Ref r : reference()) {
            osrs += r.qp();
            ids.add(r.id());
        }
        assertEquals(osrs, Quest.totalQuestPoints());
        // Quests outside the OSRS list (the legacy Barrows / Fight Caves entries) give no quest points.
        for (QuestInfo q : ContentRegistry.quests()) {
            assertTrue(ids.contains(q.questId()) || q.questPoints() == 0, q.questId() + " is not an OSRS quest");
        }
    }
}
