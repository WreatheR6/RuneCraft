package com.runecraft.content.quests;

import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.quests.QuestBook;
import com.runecraft.osrs.content.quests.QuestPuzzle;
import com.runecraft.osrs.content.quests.QuestSpec;
import com.runecraft.osrs.content.quests.QuestSpec.Trigger;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Msg;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Runs {@link QuestPuzzle} triggers: shows the player's room of the puzzle with clickable options
 * ({@code /questpuzzle <quest> <stage>.<trigger> <option>}), checks each click against the solutions, re-draws and
 * sends the player back on a wrong answer, and fires the trigger when solved (or, for a fight room, when its foe dies).
 */
public final class QuestPuzzles {
    /** A player's input towards the puzzle trigger they were last shown. */
    private static final class Entry {
        final String quest;
        final int stage;
        final int trigger;
        final List<Integer> input = new ArrayList<>();
        boolean awaitingKill;

        Entry(String quest, int stage, int trigger) {
            this.quest = quest;
            this.stage = stage;
            this.trigger = trigger;
        }
    }

    private static final Map<UUID, Entry> ACTIVE = new HashMap<>();
    /** Place-presented puzzles already shown to a player since they arrived ("uuid|quest|stage|trigger"). */
    private static final Set<String> SHOWN = new HashSet<>();

    private QuestPuzzles() {
    }

    static String attemptsKey(QuestPuzzle puzzle) {
        return "puzzle:" + puzzle.key;
    }

    static long seed(ServerPlayerEntity p, QuestPuzzle puzzle) {
        int attempt = QuestsState.get(p).kills(p.getUuid(), attemptsKey(puzzle));
        return QuestPuzzle.seed(p.getUuidAsString(), puzzle.key, attempt);
    }

    static QuestPuzzle.Variant variant(ServerPlayerEntity p, Trigger t) {
        return t.puzzle.variant(seed(p, t.puzzle), t.slot);
    }

    /** Who presents the trigger's puzzle to this player: the variant's NPC, else the trigger target. */
    static String presenter(ServerPlayerEntity p, Trigger t) {
        String npc = variant(p, t).npc();
        return npc != null ? npc : t.target;
    }

    /** Talking to an NPC that presents a puzzle of an active quest: shows it. True if handled. */
    static boolean talk(ServerPlayerEntity p, String npcId, List<QuestSpec> quests) {
        for (QuestSpec q : quests) {
            if (!QuestInterpreter.active(p, q)) {
                continue;
            }
            QuestSpec.Stage stage = q.stages.get(QuestInterpreter.stage(p, q));
            for (Trigger t : stage.triggers) {
                if (t.kind != Trigger.Kind.PUZZLE || QuestInterpreter.skipped(p, t) || !presenter(p, t).equals(npcId)) {
                    continue;
                }
                if (QuestInterpreter.unmet(p, t) != null) {
                    continue;
                }
                show(p, q, t);
                return true;
            }
        }
        return false;
    }

    /** Arriving at a place that presents a puzzle (called from the reach tick); shown once per visit. */
    static void reach(ServerPlayerEntity p, QuestSpec q, Trigger t, boolean inside) {
        String key = p.getUuidAsString() + "|" + q.id() + "|" + t.stage + "|" + q.stages.get(t.stage).triggers.indexOf(t);
        if (!inside) {
            SHOWN.remove(key);
            return;
        }
        if (SHOWN.add(key)) {
            if (QuestInterpreter.unmet(p, t) == null) {
                show(p, q, t);
            } else {
                SHOWN.remove(key);
            }
        }
    }

    static void show(ServerPlayerEntity p, QuestSpec q, Trigger t) {
        int index = q.stages.get(t.stage).triggers.indexOf(t);
        Entry e = ACTIVE.get(p.getUuid());
        QuestPuzzle.Variant v = variant(p, t);
        String speaker = QuestInterpreter.npcName(presenter(p, t));
        if (e != null && e.awaitingKill && e.quest.equals(q.id()) && e.stage == t.stage && e.trigger == index) {
            com.runecraft.osrs.content.BossDef boss = ContentRegistry.boss(v.spawn());
            Msg.game(p, "Defeat " + (boss == null ? v.kill().replace('_', ' ') : boss.name) + "!");
            return;
        }
        ACTIVE.put(p.getUuid(), new Entry(q.id(), t.stage, index));
        QuestInterpreter.speak(p, isNpc(presenter(p, t)) ? speaker : "", v.prompt());
        long seed = seed(p, t.puzzle);
        MutableText line = Text.literal("");
        for (int i : t.puzzle.displayOrder(seed, v)) {
            String cmd = "/questpuzzle " + q.id() + " " + t.stage + "." + index + " " + i;
            line.append(Text.literal("[" + v.options().get(i) + "] ").styled(st -> st.withColor(Formatting.GREEN)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd))));
        }
        p.sendMessage(line, false);
        if (v.length() > 1) {
            p.sendMessage(Text.literal("(" + v.length() + " choices in order; click the options one at a time.)").formatted(Formatting.GRAY), false);
        }
    }

    /** {@code /questpuzzle <quest>}: shows the current puzzle of a quest again. */
    static void reshow(ServerPlayerEntity p, String questId) {
        QuestSpec q = QuestBook.get().spec(questId);
        if (q == null || !QuestInterpreter.active(p, q)) {
            Msg.game(p, "You have no puzzle to solve for that quest.");
            return;
        }
        for (Trigger t : q.stages.get(QuestInterpreter.stage(p, q)).triggers) {
            if (t.kind == Trigger.Kind.PUZZLE && !QuestInterpreter.skipped(p, t) && QuestInterpreter.unmet(p, t) == null) {
                show(p, q, t);
                return;
            }
        }
        Msg.game(p, "You have no puzzle to solve for that quest right now.");
    }

    /** One clicked option ({@code ref} = "stage.trigger"). */
    static void answer(ServerPlayerEntity p, String questId, String ref, int option) {
        QuestSpec q = QuestBook.get().spec(questId);
        String[] parts = ref.split("\\.");
        if (q == null || parts.length != 2 || !QuestInterpreter.active(p, q)) {
            return;
        }
        int stageIndex;
        int index;
        try {
            stageIndex = Integer.parseInt(parts[0]);
            index = Integer.parseInt(parts[1]);
        } catch (NumberFormatException ex) {
            return;
        }
        Entry e = ACTIVE.get(p.getUuid());
        if (stageIndex != QuestInterpreter.stage(p, q) || index < 0 || index >= q.stages.get(stageIndex).triggers.size()
                || e == null || !e.quest.equals(questId) || e.stage != stageIndex || e.trigger != index) {
            Msg.game(p, "That puzzle has moved on. Look at it again.");
            return;
        }
        Trigger t = q.stages.get(stageIndex).triggers.get(index);
        if (t.kind != Trigger.Kind.PUZZLE || QuestInterpreter.skipped(p, t) || e.awaitingKill) {
            return;
        }
        String reason = QuestInterpreter.unmet(p, t);
        if (reason != null) {
            if (!reason.isEmpty()) {
                Msg.game(p, reason);
            }
            return;
        }
        QuestPuzzle.Variant v = variant(p, t);
        if (option < 0 || option >= v.options().size()) {
            return;
        }
        e.input.add(option);
        int result = v.check(e.input);
        String speaker = QuestInterpreter.npcName(presenter(p, t));
        if (result < 0) {
            fail(p, q, t, speaker);
        } else if (result == 0) {
            List<String> picked = new ArrayList<>();
            e.input.forEach(i -> picked.add(v.options().get(i)));
            p.sendMessage(Text.literal("> " + String.join(", ", picked) + " (" + e.input.size() + "/" + v.length() + ")")
                    .formatted(Formatting.GRAY), false);
        } else if (v.spawn() != null) {
            e.awaitingKill = true;
            QuestInterpreter.spawnNear(p, v.spawn());
        } else {
            ACTIVE.remove(p.getUuid());
            QuestInterpreter.fire(p, q, t, isNpc(presenter(p, t)) ? speaker : "");
        }
    }

    private static void fail(ServerPlayerEntity p, QuestSpec q, Trigger t, String speaker) {
        ACTIVE.remove(p.getUuid());
        QuestPuzzle puzzle = t.puzzle;
        if (puzzle.failLines.isEmpty()) {
            Msg.red(p, "That isn't right.");
        } else {
            QuestInterpreter.speak(p, speaker, puzzle.failLines);
        }
        if (puzzle.failStage >= 0) {
            QuestsState state = QuestsState.get(p);
            puzzle.failFlags.forEach(f -> state.clearFlag(p.getUuid(), f));
            state.addKill(p.getUuid(), attemptsKey(puzzle));
            QuestApi.setStage(p, q.id(), puzzle.failStage);
            p.sendMessage(Text.literal("[" + q.def.displayName() + "] " + q.stages.get(puzzle.failStage).journal).formatted(Formatting.DARK_AQUA), false);
            com.runecraft.network.ServerNetworking.sync(p);
        } else {
            Msg.game(p, "Try again.");
        }
    }

    /** A kill while a fight room's foe is up: solves the room. True if it fired a trigger. */
    static boolean onKill(ServerPlayerEntity p, List<String> ids) {
        Entry e = ACTIVE.get(p.getUuid());
        if (e == null || !e.awaitingKill) {
            return false;
        }
        QuestSpec q = QuestBook.get().spec(e.quest);
        if (q == null || !QuestInterpreter.active(p, q) || QuestInterpreter.stage(p, q) != e.stage) {
            ACTIVE.remove(p.getUuid());
            return false;
        }
        Trigger t = q.stages.get(e.stage).triggers.get(e.trigger);
        QuestPuzzle.Variant v = variant(p, t);
        if (v.kill() == null || !ids.contains(v.kill())) {
            return false;
        }
        ACTIVE.remove(p.getUuid());
        QuestInterpreter.fire(p, q, t, "");
        return true;
    }

    /**
     * Self-test hook: shows the player's room and clicks a solution through {@link #answer}. Returns the creature to
     * kill when the room is a fight (its foe has been raised), else null.
     */
    public static String selfTestSolve(ServerPlayerEntity p, QuestSpec q, Trigger t) {
        show(p, q, t);
        QuestPuzzle.Variant v = variant(p, t);
        String ref = t.stage + "." + q.stages.get(t.stage).triggers.indexOf(t);
        for (int option : v.solutions().get(0)) {
            answer(p, q.id(), ref, option);
        }
        Entry e = ACTIVE.get(p.getUuid());
        return e != null && e.awaitingKill ? v.kill() : null;
    }

    /** Self-test hook: clicks a wrong first option; returns whether the puzzle reacted (input reset). */
    public static boolean selfTestFail(ServerPlayerEntity p, QuestSpec q, Trigger t) {
        show(p, q, t);
        QuestPuzzle.Variant v = variant(p, t);
        int wrong = -1;
        for (int i = 0; i < v.options().size() && wrong < 0; i++) {
            if (v.check(List.of(i)) < 0) {
                wrong = i;
            }
        }
        if (wrong < 0) {
            return false;
        }
        answer(p, q.id(), t.stage + "." + q.stages.get(t.stage).triggers.indexOf(t), wrong);
        return !ACTIVE.containsKey(p.getUuid());
    }

    /** Whether an id names an NPC (puzzles presented by places are narrated). */
    static boolean isNpc(String id) {
        return ContentRegistry.npc(id) != null;
    }
}
