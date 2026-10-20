package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.Quest;
import com.runecraft.osrs.content.quests.QuestSpec.Trigger;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Quests walked the way a real player walks them ({@link QuestStrictWalk}): every side step in order, quest items only from
 * the quest's own steps, flags only from the triggers that set them. Every quest must be completable that way; the master
 * and late experienced quests (the in-game {@code quest:walk:<id>} self-test walks these with real actions) must also have
 * no step that never fires, apart from the alternatives listed below.
 */
class QuestStrictWalkTest {
    private static final QuestBook BOOK = QuestBook.get();

    /** The quests the in-game strict walk covers ({@code com.runecraft.selftest.QuestWalkChecks} walks the same list). */
    static final List<String> MASTER = QuestStrictWalk.WALKED;

    /** Players to plan for: puzzle rooms, puzzle variants and per-player routes depend on the player's id. */
    private static final List<String> PLAYERS = List.of("tester", "0b9c2f4e-6a1d-4c4e-9f00-3f4a5b6c7d8e",
            "7d1e5a90-2b3c-4d5e-8f60-718293a4b5c6", "e3f1a2b4-c5d6-47e8-9a0b-1c2d3e4f5a6b");

    /**
     * Steps of the walked quests that a walk can legitimately leave out (another route, an optional fight), as
     * "quest stage kind target".
     */
    private static final Set<String> ALTERNATIVES = Set.of(
            // OSRS: ice gloves only matter for meleeing Fareed; water spells and ranged work without them
            "desert_treasure_i 2 kill ice_queen",
            // saves from before the royal seal hand Caranock Narnode's orders
            "monkey_madness_i 1 talk glo_caranock",
            // Tutab's Magical Market also sells the monkey talisman (the walk gets it from the monkey child)
            "monkey_madness_i 4 talk tutab",
            // Garkor replaces a lost 10th squad sigil
            "monkey_madness_i 7 talk garkor",
            // the Guildmaster replaces a lost maze key
            "dragon_slayer_i 3 talk guildmaster",
            // Radimus hands out more papyrus (and to saves from before the papyrus)
            "legends_quest 1 talk radimus_erkle",
            // crystal pieces lost after the guardians' kills are found again by the furnace (the walk keeps its pieces; the
            // stage's other cave steps are needed to get on, so a dead one would leave the walk stuck instead)
            "legends_quest 5 reach kharazi_cave",
            // saves from before carry Eluned's seed already: the moss guardian's kill just moves on
            "roving_elves 2 kill moss_guardian",
            // saves from before carry the Weiss salt: My Arm finishes the quest with it
            "making_friends_with_my_arm 5 talk my_arm",
            // Golrie sings a lost crystal chime again (and one for saves from before the chime)
            "the_path_of_glouphrie 2 talk golrie",
            // saves from before carry the old spider venom sample instead of the eggs and bark
            "a_night_at_the_theatre 2 talk quest_mysterious_stranger_tob",
            // the priest's lines when the player arrives without the urn (the walk brings it)
            "beneath_cursed_sands 3 talk tumeken_priest");

    @Test
    void everyQuestCompletesWhenWalkedStepByStep() {
        List<String> stuck = new ArrayList<>();
        for (QuestSpec q : BOOK.quests) {
            QuestStrictWalk.Plan plan = QuestStrictWalk.plan(q, PLAYERS.get(0));
            if (!plan.completes()) {
                stuck.add(q.id() + ": " + plan.stuck());
            }
        }
        assertTrue(stuck.isEmpty(), stuck.size() + " quests a real player gets stuck on:\n" + String.join("\n", stuck));
    }

    @Test
    void walkedQuestsHaveNoDeadSteps() {
        List<String> problems = new ArrayList<>();
        Set<String> used = new TreeSet<>();
        for (String id : MASTER) {
            QuestSpec q = BOOK.spec(id);
            if (q == null) {
                problems.add(id + ": no such quest");
                continue;
            }
            for (String player : PLAYERS) {
                QuestStrictWalk.Plan plan = QuestStrictWalk.plan(q, player);
                if (!plan.completes()) {
                    problems.add(id + " (" + player + "): " + plan.stuck());
                    continue;
                }
                for (Trigger t : plan.dead()) {
                    String key = id + " " + t.stage + " " + t.kind.name().toLowerCase(java.util.Locale.ROOT) + " " + t.target;
                    if (ALTERNATIVES.contains(key)) {
                        used.add(key);
                    } else {
                        problems.add(id + " (" + player + "): stage " + t.stage + " " + QuestWalk.describe(t) + " never fires");
                    }
                }
            }
        }
        assertTrue(problems.isEmpty(), problems.size() + " problems:\n" + String.join("\n", new TreeSet<>(problems)));
        Set<String> stale = new TreeSet<>(ALTERNATIVES);
        stale.removeAll(used);
        assertTrue(stale.isEmpty(), "listed alternatives that fire now (remove them): " + stale);
    }

    @Test
    void walksTheSideStepsBeforeMovingOn() {
        QuestSpec q = new QuestSpec("strict_side", "Strict side", Quest.Difficulty.NOVICE, 1)
                .stage("Talk to Bob.").talk("bob")
                .stage("Get the key and open the door.")
                .talk("ann").stay().unlessFlag("got_key").flag("got_key")
                .reach("door", 4).needFlag("got_key")
                .stage("Done.").talk("bob");
        q.seal();
        QuestStrictWalk.Plan plan = QuestStrictWalk.plan(q, "p");
        assertTrue(plan.completes(), plan.stuck());
        assertEquals(List.of("talk bob", "talk ann", "reach door", "talk bob"), plan.steps().stream()
                .map(s -> s.kind().name().toLowerCase(java.util.Locale.ROOT) + " " + s.target()).toList());
        assertTrue(plan.dead().isEmpty());
    }

    @Test
    void reportsStepsARealPlayerCantReach() {
        // a flag nothing sets in this quest is another quest's progress (taken as set), but a quest item nobody hands out
        // and a talk hidden behind an earlier, endlessly repeating one get a player stuck
        QuestSpec missing = new QuestSpec("strict_missing", "Strict missing", Quest.Difficulty.NOVICE, 1)
                .stage("Start.").talk("bob").give("strict_token")
                .stage("Hand over the token twice.").talk("bob").take("strict_token:2");
        missing.seal();
        QuestStrictWalk.Plan plan = QuestStrictWalk.plan(missing, "p");
        assertTrue(plan.completes(), "strict_token is no quest-book item, so it can be gathered: " + plan.stuck());

        QuestSpec hidden = new QuestSpec("strict_hidden", "Strict hidden", Quest.Difficulty.NOVICE, 1)
                .stage("Start.").talk("bob")
                .stage("Talk to Ann twice.")
                .talk("ann").stay().say("Hello again.")
                .talk("ann").flag("x")
                .stage("Done.").talk("bob");
        hidden.seal();
        QuestStrictWalk.Plan h = QuestStrictWalk.plan(hidden, "p");
        assertTrue(!h.completes() && h.stuck().contains("fires an earlier trigger that makes no progress"), String.valueOf(h.stuck()));

        QuestSpec reach = new QuestSpec("strict_reach", "Strict reach", Quest.Difficulty.NOVICE, 1)
                .stage("Arrive somewhere.").reach("somewhere", 4)
                .stage("Done.").talk("bob");
        reach.seal();
        assertTrue(QuestStrictWalk.plan(reach, "p").stuck().contains("only starts by talking"));

        QuestSpec outside = new QuestSpec("strict_outside", "Strict outside", Quest.Difficulty.NOVICE, 1)
                .stage("Start.").talk("bob").needFlag("other_quest_flag")
                .stage("Done.").talk("bob");
        outside.seal();
        QuestStrictWalk.Plan o = QuestStrictWalk.plan(outside, "p");
        assertTrue(o.completes(), o.stuck());
        assertEquals(List.of("other_quest_flag"), o.steps().get(0).outsideFlags());
    }

    @Test
    void walkedQuestsExist() {
        for (String id : MASTER) {
            assertTrue(BOOK.spec(id) != null, id);
        }
    }
}
