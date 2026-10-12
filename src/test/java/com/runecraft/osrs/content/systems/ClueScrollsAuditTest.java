package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.QuestNpc;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Treasure trail tiers, step types, completability and casket tables against OSRS. */
class ClueScrollsAuditTest {
    @Test
    void stepAndRollCountsFollowOsrs() {
        int[][] steps = {{1, 3}, {2, 4}, {3, 5}, {4, 6}, {5, 7}, {6, 8}};
        int[][] rolls = {{1, 3}, {2, 4}, {3, 5}, {4, 6}, {4, 6}, {5, 7}};
        for (ClueScrolls.Tier t : ClueScrolls.Tier.values()) {
            assertEquals(steps[t.ordinal()][0], t.minSteps(), t + " min steps");
            assertEquals(steps[t.ordinal()][1], t.maxSteps(), t + " max steps");
            assertEquals(rolls[t.ordinal()][0], t.minRolls(), t + " min rolls");
            assertEquals(rolls[t.ordinal()][1], t.maxRolls(), t + " max rolls");
        }
    }

    @Test
    void eachTierRollsItsOsrsStepTypes() {
        List<ClueScrolls.Npc> npcs = ClueScrolls.npcs();
        Map<ClueScrolls.Tier, Set<ClueScrolls.StepType>> seen = new EnumMap<>(ClueScrolls.Tier.class);
        for (ClueScrolls.Tier t : ClueScrolls.Tier.values()) {
            seen.put(t, EnumSet.noneOf(ClueScrolls.StepType.class));
            for (long seed = 0; seed < 400; seed++) {
                for (ClueScrolls.Step s : ClueScrolls.generate(t, seed, npcs)) {
                    seen.get(t).add(s.type());
                    assertTrue(ClueScrolls.types(t).contains(s.type()), t + " rolled " + s.type());
                    if (s.type() == ClueScrolls.StepType.EMOTE) {
                        assertNotNull(s.emote());
                        assertTrue(ClueScrolls.EMOTES.contains(s.emote()));
                    }
                }
            }
        }
        // Coordinates from medium up, ciphers in medium/hard, hot/cold in beginner/master, emotes everywhere.
        assertFalse(seen.get(ClueScrolls.Tier.EASY).contains(ClueScrolls.StepType.COORDINATE));
        assertTrue(seen.get(ClueScrolls.Tier.MEDIUM).contains(ClueScrolls.StepType.COORDINATE));
        assertTrue(seen.get(ClueScrolls.Tier.HARD).contains(ClueScrolls.StepType.CIPHER));
        assertFalse(seen.get(ClueScrolls.Tier.ELITE).contains(ClueScrolls.StepType.CIPHER));
        assertTrue(seen.get(ClueScrolls.Tier.BEGINNER).contains(ClueScrolls.StepType.HOT_COLD));
        assertTrue(seen.get(ClueScrolls.Tier.MASTER).contains(ClueScrolls.StepType.HOT_COLD));
        assertTrue(seen.get(ClueScrolls.Tier.BEGINNER).contains(ClueScrolls.StepType.CHARLIE));
        for (ClueScrolls.Tier t : ClueScrolls.Tier.values()) {
            assertTrue(seen.get(t).contains(ClueScrolls.StepType.EMOTE), t + " emote");
            assertTrue(seen.get(t).contains(ClueScrolls.StepType.ANAGRAM), t + " anagram");
            assertTrue(seen.get(t).contains(ClueScrolls.StepType.CRYPTIC), t + " cryptic");
        }
    }

    @Test
    void everyStepIsCompletable() {
        for (int[] s : ClueScrolls.allSpots()) {
            assertTrue(DiaryCompletabilityTest.onLand(s[0], s[1], ClueScrolls.DIG_RADIUS), "spot at sea " + s[0] + "," + s[1]);
        }
        for (String id : ClueScrolls.crypticTargets()) {
            assertNotNull(QuestNpc.valueOf(id), "cryptic clue NPC " + id);
        }
        for (ClueScrolls.Tier t : ClueScrolls.Tier.values()) {
            for (String item : ClueScrolls.fetchItems(t)) {
                assertNotNull(ItemCatalog.get(item), t + " fetch item " + item);
            }
        }
        // Talk steps only target NPCs that exist (and so stand somewhere in the world).
        List<ClueScrolls.Npc> npcs = ClueScrolls.npcs();
        for (ClueScrolls.Tier t : ClueScrolls.Tier.values()) {
            for (long seed = 0; seed < 100; seed++) {
                for (ClueScrolls.Step s : ClueScrolls.generate(t, seed, npcs)) {
                    if (s.isTalk()) {
                        assertTrue(npcs.stream().anyMatch(n -> n.id().equals(s.npcId())), s.npcId());
                    }
                }
            }
        }
    }

    @Test
    void ciphersAndHotCold() {
        assertEquals("BMJ UIF LFCBC TFMMFS", ClueScrolls.cipher("Ali the Kebab seller", 1));
        assertEquals("incredibly hot", ClueScrolls.temperature(3));
        assertEquals("warm", ClueScrolls.temperature(50));
        assertEquals("ice cold", ClueScrolls.temperature(1000));
    }

    @Test
    void casketRatesFollowOsrs() {
        // 3rd age: per piece per casket (wiki).
        assertEquals(211_250, ClueRewards.megaOneIn(ClueScrolls.Tier.HARD, "3rd_age_platebody"));
        assertEquals(249_262, ClueRewards.megaOneIn(ClueScrolls.Tier.ELITE, "3rd_age_platebody"));
        assertEquals(313_168, ClueRewards.megaOneIn(ClueScrolls.Tier.MASTER, "3rd_age_platebody"));
        assertEquals(313_168, ClueRewards.megaOneIn(ClueScrolls.Tier.MASTER, "3rd_age_bow"));
        assertEquals(0, ClueRewards.megaOneIn(ClueScrolls.Tier.HARD, "3rd_age_bow"));
        assertEquals(1_000, ClueRewards.megaOneIn(ClueScrolls.Tier.MASTER, "bloodhound"));
        assertEquals(1_133, ClueRewards.megaOneIn(ClueScrolls.Tier.MEDIUM, "ranger_boots"));
        // Over many hard caskets a 3rd age piece is very rare (~1 in 17,600 caskets for any of the 12).
        Random r = new Random(7);
        int thirdAge = 0;
        for (int i = 0; i < 20_000; i++) {
            for (ClueRewards.Reward rw : ClueRewards.roll(ClueScrolls.Tier.HARD, r)) {
                if (rw.item().startsWith("3rd_age")) {
                    thirdAge++;
                }
            }
        }
        assertTrue(thirdAge <= 6, "3rd age drops " + thirdAge);
        // Master clue chance per casket: easy 1/50, medium 1/30, hard 1/15, elite 1/5.
        assertEquals(50, ClueRewards.masterClueOneIn(ClueScrolls.Tier.EASY));
        assertEquals(30, ClueRewards.masterClueOneIn(ClueScrolls.Tier.MEDIUM));
        assertEquals(15, ClueRewards.masterClueOneIn(ClueScrolls.Tier.HARD));
        assertTrue(ClueRewards.uniques(ClueScrolls.Tier.MEDIUM).contains("mithril_platebody_t"));
        assertTrue(ClueRewards.uniques(ClueScrolls.Tier.EASY).contains("black_pickaxe"));
    }
}
