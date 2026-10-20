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

/** Boss kill loot ({@link BossLoot}): one main-table roll a kill (two for Zulrah, Vorkath...), each line at its own rate. */
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
                assertTrue(!BossLoot.pick(boss, drops, random::nextInt).isEmpty(), boss + ": a kill dropped nothing");
            }
        }
    }

    @Test
    void everyLineKeepsItsRate() {
        Random random = new Random(2);
        List<Drop> drops = table("araxxor"); // its main lines add up to more than a roll: no stretching
        Map<Drop, Integer> seen = new HashMap<>();
        int items = 0;
        for (int i = 0; i < KILLS; i++) {
            for (Drop d : BossLoot.pick("araxxor", drops, random::nextInt)) {
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
        // as many drops a kill as the lines' rates add up to (one main roll, a little more for its over-full table and clues)
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
        assertEquals(2, BossLoot.rolls("zulrah")); // OSRS Wiki: two rolls on Zulrah's and Vorkath's drop tables
        assertEquals(2, BossLoot.rolls("vorkath"));
        Random random = new Random(6);
        for (int i = 0; i < 10_000; i++) {
            long gems = BossLoot.pick(table("gemstone_crab"), 3, random::nextInt).stream()
                    .filter(d -> d.item().startsWith("uncut_")).count();
            assertEquals(3, gems);
        }
    }

    private static boolean any(List<Drop> got, String... items) {
        for (Drop d : got) {
            if (List.of(items).contains(d.item())) {
                return true;
            }
        }
        return false;
    }

    /** OSRS Wiki "Zulrah": two rolls on its table (rates per roll), the jar and the clue once. */
    @Test
    void zulrahRollsItsTableTwice() {
        Random random = new Random(7);
        List<Drop> drops = table("zulrah");
        int uniques = 0;
        int jars = 0;
        double main = 0;
        for (int i = 0; i < KILLS; i++) {
            List<Drop> got = BossLoot.pick("zulrah", drops, random::nextInt);
            uniques += any(got, "tanzanite_fang", "magic_fang", "serpentine_visage", "uncut_onyx") ? 1 : 0;
            jars += any(got, "jar_of_swamp") ? 1 : 0;
            for (Drop d : got) {
                main += d.rarity() > 1 && !BossLoot.tertiary("zulrah", d) ? 1 : 0;
            }
        }
        // any unique about 1/128 a kill (1/256 a roll, Mod Ash), each one 1/512
        assertEquals(1.0 / 128, uniques / (double) KILLS, 5 * Math.sqrt(1.0 / 128 / KILLS));
        assertEquals(1.0 / 3000, jars / (double) KILLS, 5 * Math.sqrt(1.0 / 3000 / KILLS) + 1e-4);
        assertEquals(2, main / KILLS, 0.05, "two main drops a kill");
    }

    /** OSRS Wiki "Vorkath": its head, visages and necklace are tertiaries, on top of the two main drops. */
    @Test
    void vorkathsTertiariesComeOnTopOfTheMainDrops() {
        Random random = new Random(8);
        List<Drop> drops = table("vorkath");
        int heads = 0;
        double mainWithHead = 0;
        for (int i = 0; i < KILLS; i++) {
            List<Drop> got = BossLoot.pick("vorkath", drops, random::nextInt);
            if (any(got, "vorkaths_head")) {
                heads++;
                for (Drop d : got) {
                    mainWithHead += d.rarity() > 1 && !BossLoot.tertiary("vorkath", d) ? 1 : 0;
                }
            }
        }
        assertEquals(1.0 / 50, heads / (double) KILLS, 5 * Math.sqrt(1.0 / 50 / KILLS));
        assertEquals(2, mainWithHead / heads, 0.1, "a head kill still gets its two main drops");
        assertTrue(BossLoot.tertiary("vorkath", new Drop("draconic_visage", 1, 1, 5000)));
        assertTrue(BossLoot.tertiary("kalphite_queen", new Drop("dragon_pickaxe", 1, 1, 400)));
        assertTrue(!BossLoot.tertiary("king_black_dragon", new Drop("dragon_pickaxe", 1, 1, 1000)), "KBD: pre-roll");
        assertTrue(!BossLoot.tertiary("kraken", new Drop("kraken_tentacle", 1, 1, 400)), "the Kraken's main table");
    }

    /** OSRS Wiki "Alchemical Hydra": "The first drop has a chance to be a unique, while the second is always standard." */
    @Test
    void theHydrasPreRollTakesOneOfItsTwoRolls() {
        Random random = new Random(9);
        List<Drop> drops = table("alchemical_hydra");
        double rate = 0;
        for (Drop d : drops) {
            rate += BossLoot.once("alchemical_hydra", d) ? 1.0 / d.rarity() : 0;
        }
        int uniqueKills = 0;
        double mainOnUnique = 0;
        double mainOtherwise = 0;
        for (int i = 0; i < KILLS; i++) {
            List<Drop> got = BossLoot.pick("alchemical_hydra", drops, random::nextInt);
            long uniques = got.stream().filter(d -> BossLoot.once("alchemical_hydra", d)).count();
            assertTrue(uniques <= 1, "one pre-roll a kill");
            long main = got.stream().filter(d -> d.rarity() > 1 && !BossLoot.tertiary("alchemical_hydra", d)
                    && !BossLoot.once("alchemical_hydra", d)).count();
            if (uniques > 0) {
                uniqueKills++;
                mainOnUnique += main;
            } else {
                mainOtherwise += main;
            }
        }
        assertEquals(rate, uniqueKills / (double) KILLS, 5 * Math.sqrt(rate / KILLS));
        assertTrue(mainOnUnique / uniqueKills < 1.3, "a unique takes the first roll: " + mainOnUnique / uniqueKills);
        assertTrue(mainOtherwise / (KILLS - uniqueKills) > 1.9, "two standard rolls otherwise");
    }

    /**
     * OSRS Wiki "Phantom Muspah": one roll on its unique table beside two on its secondary table; "If the player gets a
     * Venator shard, they will not get any other drop."
     */
    @Test
    void theMuspahsUniqueTableIsRolledOnce() {
        Random random = new Random(10);
        List<Drop> drops = table("phantom_muspah");
        int shards = 0;
        for (int i = 0; i < 20_000; i++) {
            List<Drop> got = BossLoot.pick("phantom_muspah", drops, random::nextInt);
            assertTrue(got.stream().filter(d -> BossLoot.once("phantom_muspah", d)).count() <= 1, "one unique-table line a kill");
            long secondary = got.stream().filter(d -> !BossLoot.once("phantom_muspah", d) && !BossLoot.tertiary("phantom_muspah", d)).count();
            if (got.stream().anyMatch(d -> d.item().equals("venator_shard"))) {
                shards++;
                assertEquals(0, secondary, "a Venator shard comes alone");
            } else {
                assertTrue(secondary >= 2, "two secondary rolls");
            }
        }
        assertEquals(1 / 100.0, shards / 20_000.0, 0.004);
    }

    /** Every item a boss's own tertiary or once-a-kill list names is a line of its table (no typo hides a tertiary). */
    @Test
    void namedLinesAreOnTheirBossTables() {
        for (String boss : BossLoot.bossesWithNamedLines()) {
            List<Drop> drops = table(boss);
            for (String item : BossLoot.namedLines(boss)) {
                assertTrue(drops.stream().anyMatch(d -> d.item().equals(item)), boss + ": no " + item + " line");
            }
        }
    }
}
