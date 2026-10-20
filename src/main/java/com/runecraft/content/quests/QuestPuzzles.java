package com.runecraft.content.quests;

import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.quests.QuestBook;
import com.runecraft.osrs.content.quests.QuestPuzzle;
import com.runecraft.osrs.content.quests.QuestSpec;
import com.runecraft.osrs.content.quests.QuestSpec.Trigger;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

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
    /** Place-presented puzzles whose unmet condition the player was already told about this visit. */
    private static final Set<String> TOLD = new HashSet<>();

    private QuestPuzzles() {
    }

    static String attemptsKey(QuestPuzzle puzzle) {
        return "puzzle:" + puzzle.key;
    }

    static long seed(ServerPlayer p, QuestPuzzle puzzle) {
        int attempt = QuestsState.get(p).kills(p.getUUID(), attemptsKey(puzzle));
        return QuestPuzzle.seed(p.getStringUUID(), puzzle.key, attempt);
    }

    static QuestPuzzle.Variant variant(ServerPlayer p, Trigger t) {
        return t.puzzle.variant(seed(p, t.puzzle), t.slot);
    }

    /** Who presents the trigger's puzzle to this player: the variant's NPC, else the trigger target. */
    static String presenter(ServerPlayer p, Trigger t) {
        String npc = variant(p, t).npc();
        return npc != null ? npc : t.target;
    }

    /** Talking to an NPC that presents a puzzle of an active quest: shows it. True if handled. */
    static boolean talk(ServerPlayer p, String npcId, List<QuestSpec> quests) {
        for (QuestSpec q : quests) {
            if (!QuestInterpreter.active(p, q)) {
                continue;
            }
            QuestSpec.Stage stage = q.stages.get(QuestInterpreter.stage(p, q));
            for (Trigger t : stage.triggers) {
                if (t.kind != Trigger.Kind.PUZZLE || QuestInterpreter.skipped(p, t) || !presenter(p, t).equals(npcId)) {
                    continue;
                }
                String reason = QuestInterpreter.unmet(p, t);
                if (reason != null) {
                    if (t.emptyHanded && !QuestInterpreter.emptyHanded(p)) {
                        if (t.banksItems) {
                            // Peer the Seer sends the player's belongings to the bank before the trial
                            QuestInterpreter.bankEverything(p);
                            Msg.game(p, QuestInterpreter.npcName(npcId) + " sends everything you carry and wear to your bank.");
                            reason = QuestInterpreter.unmet(p, t);
                            if (reason == null) {
                                show(p, q, t);
                                return true;
                            }
                        }
                        // the examiner tells the recruit why the test can't start
                        if (!reason.isEmpty()) {
                            Msg.game(p, "[" + q.def.displayName() + "] " + reason);
                        }
                        return true;
                    }
                    continue;
                }
                show(p, q, t);
                return true;
            }
        }
        return false;
    }

    /** Arriving at a place that presents a puzzle (called from the reach tick); shown once per visit. */
    static void reach(ServerPlayer p, QuestSpec q, Trigger t, boolean inside) {
        String key = p.getStringUUID() + "|" + q.id() + "|" + t.stage + "|" + q.stages.get(t.stage).triggers.indexOf(t);
        if (!inside) {
            SHOWN.remove(key);
            TOLD.remove(key);
            return;
        }
        if (SHOWN.add(key)) {
            String reason = QuestInterpreter.unmet(p, t);
            if (reason == null) {
                show(p, q, t);
            } else {
                SHOWN.remove(key);
                // say once per visit why the puzzle isn't shown (Recruitment Drive: items carried into the testing grounds)
                if (!reason.isEmpty() && TOLD.add(key)) {
                    Msg.game(p, "[" + q.def.displayName() + "] " + reason);
                }
            }
        }
    }

    static void show(ServerPlayer p, QuestSpec q, Trigger t) {
        int index = q.stages.get(t.stage).triggers.indexOf(t);
        Entry e = ACTIVE.get(p.getUUID());
        QuestPuzzle.Variant v = variant(p, t);
        String speaker = QuestInterpreter.npcName(presenter(p, t));
        if (e != null && e.awaitingKill && e.quest.equals(q.id()) && e.stage == t.stage && e.trigger == index) {
            com.runecraft.osrs.content.BossDef boss = ContentRegistry.boss(v.spawn());
            // The foe can die without this player's kill (another recruit's last hit, suffocation) or be left behind: raise it again.
            if (v.spawn() != null && !QuestInterpreter.bossNearby(p, v.spawn())) {
                QuestInterpreter.spawnNear(p, v.spawn());
            }
            Msg.game(p, "Defeat " + (boss == null ? v.kill().replace('_', ' ') : boss.name) + "!");
            return;
        }
        ACTIVE.put(p.getUUID(), new Entry(q.id(), t.stage, index));
        QuestInterpreter.speak(p, isNpc(presenter(p, t)) ? speaker : "", v.prompt());
        long seed = seed(p, t.puzzle);
        MutableComponent line = Component.literal("");
        for (int i : t.puzzle.displayOrder(seed, v)) {
            String cmd = "/questpuzzle " + q.id() + " " + t.stage + "." + index + " " + i;
            line.append(Component.literal("[" + v.options().get(i) + "] ").withStyle(st -> st.withColor(ChatFormatting.GREEN)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd))));
        }
        p.displayClientMessage(line, false);
        if (v.length() > 1) {
            p.displayClientMessage(Component.literal("(" + v.length() + " choices in order; click the options one at a time.)").withStyle(ChatFormatting.GRAY), false);
        }
    }

    /** Drops a player's puzzle progress (on disconnect): the room is shown afresh next time. */
    static void forget(UUID player) {
        ACTIVE.remove(player);
        String prefix = player + "|";
        SHOWN.removeIf(k -> k.startsWith(prefix));
        TOLD.removeIf(k -> k.startsWith(prefix));
    }

    /** {@code /questpuzzle <quest>}: shows the current puzzle of a quest again. */
    static void reshow(ServerPlayer p, String questId) {
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
    static void answer(ServerPlayer p, String questId, String ref, int option) {
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
        Entry e = ACTIVE.get(p.getUUID());
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
            p.displayClientMessage(Component.literal("> " + String.join(", ", picked) + " (" + e.input.size() + "/" + v.length() + ")")
                    .withStyle(ChatFormatting.GRAY), false);
        } else if (v.spawn() != null) {
            e.awaitingKill = true;
            QuestInterpreter.spawnNear(p, v.spawn());
        } else {
            ACTIVE.remove(p.getUUID());
            QuestInterpreter.fire(p, q, t, isNpc(presenter(p, t)) ? speaker : "");
        }
    }

    private static void fail(ServerPlayer p, QuestSpec q, Trigger t, String speaker) {
        ACTIVE.remove(p.getUUID());
        QuestPuzzle puzzle = t.puzzle;
        if (puzzle.failLines.isEmpty()) {
            Msg.red(p, "That isn't right.");
        } else {
            QuestInterpreter.speak(p, speaker, puzzle.failLines);
        }
        if (puzzle.failStage >= 0) {
            QuestsState state = QuestsState.get(p);
            puzzle.failFlags.forEach(f -> state.clearFlag(p.getUUID(), f));
            state.addKill(p.getUUID(), attemptsKey(puzzle));
            QuestApi.setStage(p, q.id(), puzzle.failStage);
            p.displayClientMessage(Component.literal("[" + q.def.displayName() + "] " + q.stages.get(puzzle.failStage).journal).withStyle(ChatFormatting.DARK_AQUA), false);
            com.runecraft.network.ServerNetworking.sync(p);
            // the re-drawn first room is shown again by the reach tick while the player stands at the place
            String prefix = p.getStringUUID() + "|" + q.id() + "|";
            SHOWN.removeIf(k -> k.startsWith(prefix));
        } else {
            Msg.game(p, "Try again.");
            show(p, q, t);
        }
    }

    /** A kill while a fight room's foe is up: solves the room. True if it fired a trigger. */
    static boolean onKill(ServerPlayer p, List<String> ids) {
        Entry e = ACTIVE.get(p.getUUID());
        if (e == null || !e.awaitingKill) {
            return false;
        }
        QuestSpec q = QuestBook.get().spec(e.quest);
        if (q == null || !QuestInterpreter.active(p, q) || QuestInterpreter.stage(p, q) != e.stage) {
            ACTIVE.remove(p.getUUID());
            return false;
        }
        Trigger t = q.stages.get(e.stage).triggers.get(e.trigger);
        QuestPuzzle.Variant v = variant(p, t);
        if (v.kill() == null || !ids.contains(v.kill())) {
            return false;
        }
        ACTIVE.remove(p.getUUID());
        QuestInterpreter.fire(p, q, t, "");
        return true;
    }

    /**
     * Self-test hook: shows the player's room and clicks a solution through {@link #answer}. Returns the creature to
     * kill when the room is a fight (its foe has been raised), else null.
     */
    public static String selfTestSolve(ServerPlayer p, QuestSpec q, Trigger t) {
        show(p, q, t);
        QuestPuzzle.Variant v = variant(p, t);
        String ref = t.stage + "." + q.stages.get(t.stage).triggers.indexOf(t);
        for (int option : v.solutions().get(0)) {
            answer(p, q.id(), ref, option);
        }
        Entry e = ACTIVE.get(p.getUUID());
        return e != null && e.awaitingKill ? v.kill() : null;
    }

    /** Self-test hook: clicks a wrong first option; returns whether the puzzle reacted (input reset). */
    public static boolean selfTestFail(ServerPlayer p, QuestSpec q, Trigger t) {
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
        return !ACTIVE.containsKey(p.getUUID());
    }

    /** Whether an id names an NPC (puzzles presented by places are narrated). */
    static boolean isNpc(String id) {
        return ContentRegistry.npc(id) != null;
    }
}
