package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.zeah.ZeahBosses;
import com.runecraft.osrs.data.Drop;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Boss kill loot ({@link BossLoot}): one main-table roll a kill, each line at its own rate. */
class BossLootTest {
    private static final int KILLS = 100_000;

    private static List<Drop> table(String boss) {
        BossDef def = ContentRegistry.boss(boss);
        assertTrue(def != null && !def.drops.isEmpty(), boss);
        return def.drops;
    }

    /** Bosses with no 100% drop (OSRS: a kill still always lands on a main-table line). */
    @Test
    void aFullTableNeverLeavesAKillEmpty() {
        Random random = new Random(1);
        for (String boss : List.of("kraken", "scorpia", "kalphite_queen", "sarachnis", "gemstone_crab", "blue_moon", "eclipse_moon",
                "blood_moon", "branda_the_fire_queen", "corporeal_beast", "araxxor", "maggot_king", "yama", "hespori", "amoxliatl",
                "chaos_elemental", "phantom_muspah")) {
            List<Drop> drops = table(boss);
            for (int i = 0; i < 20_000; i++) {
                assertTrue(!BossLoot.pick(drops, BossLoot.rolls(boss), random::nextInt).isEmpty(), boss + ": a kill dropped nothing");
            }
        }
    }

    @Test
    void everyLineKeepsItsRate() {
        Random random = new Random(2);
        List<Drop> drops = table("scorpia"); // its main lines add up to more than a roll: no stretching
        Map<Drop, Integer> seen = new HashMap<>();
        int items = 0;
        for (int i = 0; i < KILLS; i++) {
            for (Drop d : BossLoot.pick(drops, 1, random::nextInt)) {
                seen.merge(d, 1, Integer::sum);
                items++;
            }
        }
        double total = 0;
        for (Drop d : drops) {
            double expected = 1.0 / d.rarity();
            total += expected;
            double got = seen.getOrDefault(d, 0) / (double) KILLS;
            double sd = Math.sqrt(expected / KILLS);
            assertEquals(expected, got, 5 * sd + 1e-4, d.item() + " 1/" + d.rarity());
        }
        // as many drops a kill as the lines' rates add up to (one main roll, a little more for its 1/18 coins etc.)
        assertEquals(total, items / (double) KILLS, 0.01);
    }

    @Test
    void aTableJustShortOfARollGivesItsCommonLinesTheRest() {
        Random random = new Random(3);
        List<Drop> drops = table("blood_moon"); // the Lunar Chest's standard loot lacks sun-kissed bones (3/30)
        Map<String, Integer> seen = new HashMap<>();
        for (int i = 0; i < KILLS; i++) {
            List<Drop> got = BossLoot.pick(drops, 1, random::nextInt);
            assertEquals(1, got.size(), "one Lunar Chest roll a Moon");
            seen.merge(got.get(0).item(), 1, Integer::sum);
        }
        // uniques keep their exact 1/224
        double unique = seen.getOrDefault("dual_macuahuitl", 0) / (double) KILLS;
        int rate = ZeahBosses.MOON_UNIQUE_RARITY;
        assertEquals(1.0 / rate, unique, 5 * Math.sqrt(1.0 / rate / KILLS));
        // a common line takes a little more than its 1/6
        double darts = seen.getOrDefault("atlatl_dart", 0) / (double) KILLS;
        assertTrue(darts > 1.0 / 6 * 1.05 && darts < 1.0 / 6 * 1.15, "atlatl darts " + darts);
    }

    @Test
    void aSparseListStaysSparse() {
        Random random = new Random(4);
        List<Drop> robes = List.of(Drop.of("ancient_ceremonial_mask", 1, 1, 1000), Drop.of("ancient_ceremonial_top", 1, 1, 1000));
        int drops = 0;
        for (int i = 0; i < KILLS; i++) {
            drops += BossLoot.pick(robes, 1, random::nextInt).size();
        }
        assertEquals(2.0 / 1000, drops / (double) KILLS, 0.001);
    }

    @Test
    void alwaysAndTertiaryLinesStandApart() {
        Random random = new Random(5);
        List<Drop> table = List.of(Drop.always("big_bones"), Drop.of("clue_scroll_elite", 1, 1, 2), Drop.of("coins", 1, 1, 2),
                Drop.of("rune_kiteshield", 1, 1, 2));
        int clues = 0;
        for (int i = 0; i < KILLS; i++) {
            List<Drop> got = BossLoot.pick(table, 1, random::nextInt);
            assertEquals("big_bones", got.get(0).item());
            long main = got.stream().filter(d -> d.item().equals("coins") || d.item().equals("rune_kiteshield")).count();
            assertEquals(1, main, "one main-table line a kill");
            clues += got.stream().anyMatch(d -> d.item().startsWith("clue_scroll")) ? 1 : 0;
        }
        assertEquals(0.5, clues / (double) KILLS, 0.01, "the clue is rolled on its own");
    }

    @Test
    void theGemstoneCrabsShellGivesThreeGems() {
        assertEquals(3, BossLoot.rolls("gemstone_crab"));
        assertEquals(1, BossLoot.rolls("kraken"));
        Random random = new Random(6);
        for (int i = 0; i < 10_000; i++) {
            long gems = BossLoot.pick(table("gemstone_crab"), 3, random::nextInt).stream()
                    .filter(d -> d.item().startsWith("uncut_")).count();
            assertEquals(3, gems);
        }
    }
}
