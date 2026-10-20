package com.runecraft.content.quests;

import com.runecraft.boss.DataBossEntity;
import net.minecraft.server.level.ServerPlayer;

/**
 * Entry points of the in-game self-test ({@code boss:} checks in {@code selftest/GameplayChecks}) into the package-private
 * quest boss scripts.
 */
public final class QuestBossSelfTests {
    private QuestBossSelfTests() {
    }

    /**
     * The {@code boss:<id>} check kills a boss outright after letting it act. A quest boss whose death only counts at the end
     * of its scripted fight is first played there as a player would (OSRS: Vanstrom Klause heals 200 and starts his second
     * phase the first time he reaches 0 hitpoints, and only blisterwood hurts him). Returns what was played, or null for a
     * boss without such a fight.
     */
    public static String playToKill(DataBossEntity boss, ServerPlayer p) {
        return QuestBossScripts.Vanstrom.selfTestToKill(boss, p);
    }
}
