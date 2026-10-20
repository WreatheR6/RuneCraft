package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.QuestDef;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coin rewards against the items column of {@code quest_reference.tsv} (quest-helper's item rewards, checked against the wiki
 * dump's Quest rewards boxes in round 33): a quest pays exactly the coins OSRS does, counting the reward list and whatever its
 * completing trigger gives.
 */
class QuestCoinRewardsTest {
    /** Coin rewards the wiki lists but quest-helper's item rewards leave out. */
    private static final Map<String, Integer> WIKI_ONLY = Map.of(
            "black_knights_fortress", 2500, // wiki: 2,500 coins
            "shield_of_arrav", 600,         // wiki: 600 coins
            "learning_the_ropes", 25);      // the tutorial's starting coins

    /** A coins entry: "Coins:5000" or "(2,005 if siding with Ceril) Coins:2000", not "POH in Rimmington or 1,000 Coins:1". */
    private static final Pattern COINS = Pattern.compile("(?:^|;)(?:\\([^)]*\\)\\s*)?Coins:(\\d+)");

    static int modCoins(QuestDef d) {
        int total = 0;
        List<String> all = new ArrayList<>(d.itemRewards());
        QuestSpec spec = QuestBook.get().spec(d.questId());
        if (spec != null) {
            for (QuestSpec.Trigger t : spec.triggers()) {
                if (t.completes()) {
                    t.give.forEach((item, n) -> all.add(item + ":" + n));
                }
            }
        }
        for (String r : all) {
            String[] p = r.split(":");
            if (p[0].equals("coins")) {
                total += p.length > 1 ? Integer.parseInt(p[1]) : 1;
            }
        }
        return total;
    }

    @Test
    void coinRewardsMatchOsrs() throws IOException {
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (String line : QuestReferenceTest.fixtureLines()) {
            if (line.isBlank() || line.startsWith("#") || line.startsWith("id\t")) {
                continue;
            }
            String[] c = line.split("\t", -1);
            QuestInfo q = ContentRegistry.quest(c[0]);
            if (!(q instanceof QuestDef d)) {
                continue; // the hand-scripted free quests pay out in their own scripts
            }
            Matcher m = COINS.matcher(c[12]);
            int want = m.find() ? Integer.parseInt(m.group(1)) : WIKI_ONLY.getOrDefault(c[0], 0);
            int got = modCoins(d);
            if (got != want) {
                problems.add(c[0] + ": " + got + " coins != " + want);
            }
            checked++;
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
        assertTrue(checked > 150, "checked " + checked);
    }
}
