package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.systems.ClueScrolls.Action;
import com.runecraft.osrs.content.systems.ClueScrolls.Clue;
import com.runecraft.osrs.content.systems.ClueScrolls.StepType;
import com.runecraft.osrs.content.systems.ClueScrolls.Tier;
import com.runecraft.osrs.world.SpawnTable;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Treasure trail tiers, the real OSRS steps, completability in this world and the guardians. */
class ClueScrollsAuditTest {
    @Test
    void stepAndRollCountsFollowOsrs() {
        int[][] steps = {{1, 3}, {2, 4}, {3, 5}, {4, 6}, {5, 7}, {6, 8}};
        int[][] rolls = {{1, 3}, {2, 4}, {3, 5}, {4, 6}, {4, 6}, {5, 7}};
        int[] stash = {12, 27, 42, 55, 77, 88};
        for (Tier t : Tier.values()) {
            assertEquals(steps[t.ordinal()][0], t.minSteps(), t + " min steps");
            assertEquals(steps[t.ordinal()][1], t.maxSteps(), t + " max steps");
            assertEquals(rolls[t.ordinal()][0], t.minRolls(), t + " min rolls");
            assertEquals(rolls[t.ordinal()][1], t.maxRolls(), t + " max rolls");
            assertEquals(stash[t.ordinal()], t.stashLevel(), t + " STASH Construction level");
            assertNotNull(ItemCatalog.get(t.stashPlank()), t.stashPlank());
        }
    }

    /** The step types each tier rolls in OSRS (Treasure Trails page, "Solving the Treasure Trails" table). */
    @Test
    void eachTierRollsItsOsrsStepTypes() {
        List<ClueScrolls.Npc> npcs = ClueScrolls.npcs();
        Map<Tier, Set<StepType>> want = new EnumMap<>(Tier.class);
        want.put(Tier.BEGINNER, EnumSet.of(StepType.ANAGRAM, StepType.CRYPTIC, StepType.EMOTE, StepType.HOT_COLD, StepType.MAP,
                StepType.CHARLIE));
        want.put(Tier.EASY, EnumSet.of(StepType.CRYPTIC, StepType.EMOTE, StepType.MAP, StepType.MUSIC));
        want.put(Tier.MEDIUM, EnumSet.of(StepType.ANAGRAM, StepType.CIPHER, StepType.COORDINATE, StepType.CRYPTIC, StepType.EMOTE,
                StepType.MAP, StepType.MUSIC));
        want.put(Tier.HARD, EnumSet.of(StepType.ANAGRAM, StepType.CIPHER, StepType.COORDINATE, StepType.CRYPTIC, StepType.EMOTE,
                StepType.MAP, StepType.MUSIC, StepType.FAIRY_RING));
        want.put(Tier.ELITE, EnumSet.of(StepType.ANAGRAM, StepType.COORDINATE, StepType.CRYPTIC, StepType.EMOTE, StepType.MAP,
                StepType.MUSIC, StepType.SHERLOCK));
        want.put(Tier.MASTER, EnumSet.of(StepType.ANAGRAM, StepType.COORDINATE, StepType.CRYPTIC, StepType.EMOTE, StepType.HOT_COLD,
                StepType.FALO, StepType.SHERLOCK));
        for (Tier t : Tier.values()) {
            Set<StepType> seen = EnumSet.noneOf(StepType.class);
            for (long seed = 0; seed < 600; seed++) {
                for (ClueScrolls.Step s : ClueScrolls.generate(t, seed, npcs)) {
                    seen.add(s.type());
                    assertEquals(t, s.tier());
                }
            }
            assertEquals(want.get(t), seen, t + " step types");
        }
    }

    @Test
    void everyTierHasAFullPoolOfRealSteps() {
        List<ClueScrolls.Npc> npcs = ClueScrolls.npcs();
        int[] min = {25, 120, 120, 120, 90, 150};
        for (Tier t : Tier.values()) {
            List<Clue> pool = ClueScrolls.usableClues(t, npcs);
            assertTrue(pool.size() >= min[t.ordinal()], t + " has " + pool.size() + " usable clues");
        }
        // Most OSRS clue steps can be done in this world (the rest lie in dungeons, instances and unbuilt areas).
        int usable = 0;
        for (Clue c : ClueScrolls.clues()) {
            if (ClueScrolls.unusable(c, npcs) == null) {
                usable++;
            }
        }
        assertTrue(usable >= 820, "usable steps " + usable + " of " + ClueScrolls.clues().size());
    }

    @Test
    void trailStepsAreCompletable() {
        List<ClueScrolls.Npc> npcs = ClueScrolls.npcs();
        for (Tier t : Tier.values()) {
            for (long seed = 0; seed < 200; seed++) {
                List<ClueScrolls.Step> a = ClueScrolls.generate(t, seed, npcs);
                assertEquals(a, ClueScrolls.generate(t, seed, npcs), "trails are deterministic");
                assertTrue(a.size() >= t.minSteps() && a.size() <= t.maxSteps(), t + " " + a.size());
                for (int i = 0; i < a.size(); i++) {
                    ClueScrolls.Step s = a.get(i);
                    assertFalse(s.text().isBlank(), s.toString());
                    if (i > 0) {
                        assertFalse(s.clue().equals(a.get(i - 1).clue()), "same clue twice in a row");
                    }
                    switch (s.action()) {
                        case TALK -> assertNotNull(s.npcId(), s.clue().npc());
                        case DIG, SEARCH, EMOTE -> assertTrue(ClueScrolls.onSurfaceLand(s.x(), s.y()), s.x() + "," + s.y());
                        case KILL -> assertTrue(ClueScrolls.monsterExists(s.clue().npc()), s.clue().npc());
                    }
                    for (String item : ClueReqs.items(s.req())) {
                        assertNotNull(ItemCatalog.get(item), "required item " + item);
                    }
                    if (s.type() == StepType.EMOTE) {
                        assertFalse(s.emotes().isEmpty());
                    }
                    if (s.box() != null) {
                        assertTrue(t.ordinal() >= Tier.HARD.ordinal(), "puzzle boxes are hard and up");
                        assertTrue(s.box().equals(CluePuzzles.PUZZLE_BOX) || t == Tier.MASTER, "light boxes are master only");
                    }
                }
            }
        }
    }

    @Test
    void stepSpecificsFollowTheWiki() {
        List<ClueScrolls.Npc> npcs = ClueScrolls.npcs();
        // Medium anagrams and ciphers set challenge questions; hard ones without a question hand over puzzle boxes.
        Clue baraek = find(StepType.ANAGRAM, "A BAKER");
        assertEquals("Baraek", baraek.npc());
        assertEquals("5", baraek.answer());
        ClueScrolls.Step s = ClueScrolls.step(baraek, new Random(1), npcs);
        assertTrue(s.hasChallenge());
        assertNull(s.box());
        Clue hard = ClueScrolls.clues().stream().filter(c -> c.type() == StepType.ANAGRAM && c.tier() == Tier.HARD
                && c.answer().isEmpty() && ClueScrolls.unusable(c, npcs) == null).findFirst().orElseThrow();
        assertEquals(CluePuzzles.PUZZLE_BOX, ClueScrolls.step(hard, new Random(1), npcs).box());
        // Cipher: the NPC's name Caesar-shifted.
        Clue ali = find(StepType.CIPHER, "BMJ UIF LFCBC TFMMFS");
        assertEquals(ClueScrolls.cipher(ali.npc(), 1), ali.text());
        assertEquals("399", ali.answer());
        // Coordinates: the guardian per tier and Wilderness.
        for (Clue c : ClueScrolls.clues()) {
            if (c.type() == StepType.COORDINATE) {
                switch (c.tier()) {
                    case MEDIUM -> assertNull(c.guardian(), "medium coordinates have no guardian");
                    case HARD -> assertTrue(c.guardian() == ClueScrolls.Guardian.SARADOMIN_WIZARD
                            || c.guardian() == ClueScrolls.Guardian.ZAMORAK_WIZARD || c.guardian() == null, c.toString());
                    case ELITE -> assertTrue(c.guardian() == null || c.guardian().name().contains("GUARD"), c.toString());
                    case MASTER -> assertTrue(c.guardian() == ClueScrolls.Guardian.BRASSICAN_MAGE
                            || c.guardian() == ClueScrolls.Guardian.ANCIENT_WIZARDS
                            || c.guardian() == ClueScrolls.Guardian.BRASSICAN_OR_WIZARDS, c.toString());
                    default -> throw new AssertionError("coordinates only from medium up: " + c);
                }
            }
            if (c.type() == StepType.EMOTE && c.text().contains("Beware of double agents")) {
                assertNotNull(c.guardian(), c.text());
                assertTrue(c.guardian().doubleAgent());
                assertTrue(c.tier() == Tier.HARD || c.tier() == Tier.MASTER, c.text());
            }
            if (c.type() == StepType.HOT_COLD) {
                assertTrue(c.tier() == Tier.BEGINNER || c.tier() == Tier.MASTER);
                assertEquals(c.tier() == Tier.MASTER, c.guardian() != null, "master hot/cold digs summon a guardian");
            }
        }
        // Emote clue: items and a STASH unit.
        Clue raspberry = find(StepType.EMOTE, "Blow a raspberry at Aris in her tent. Equip a gold ring and a gold necklace.");
        assertEquals(List.of("Raspberry"), raspberry.emotes());
        assertEquals("gold_ring;gold_necklace", raspberry.req());
        ClueScrolls.Stash stash = ClueScrolls.stashUnits().get(raspberry.stash());
        assertNotNull(stash);
        assertEquals(Tier.BEGINNER, stash.tier());
        assertEquals(119, ClueScrolls.stashUnits().size());
        // Two-emote medium steps.
        assertTrue(ClueScrolls.clues().stream().anyMatch(c -> c.type() == StepType.EMOTE && c.emotes().size() == 2));
        // Kill steps (elite cryptics) and key steps (medium).
        assertTrue(ClueScrolls.clues().stream().anyMatch(c -> c.action() == Action.KILL && c.npc().equals("Hellhound")));
        assertTrue(ClueScrolls.clues().stream().anyMatch(c -> c.task().equals("key") && c.npc().equals("Guard")));
    }

    @Test
    void textAndHotCold() {
        assertEquals("BMJ UIF LFCBC TFMMFS", ClueScrolls.cipher("Ali the Kebab seller", 1));
        // Strange device bands (RuneLite HotColdTemperature / wiki): 500+ ice cold ... incredibly hot, visibly shaking.
        assertEquals("ice cold", ClueScrolls.temperature(Tier.MASTER, 500));
        assertEquals("very cold", ClueScrolls.temperature(Tier.MASTER, 499));
        assertEquals("cold", ClueScrolls.temperature(Tier.MASTER, 150));
        assertEquals("warm", ClueScrolls.temperature(Tier.MASTER, 100));
        assertEquals("hot", ClueScrolls.temperature(Tier.MASTER, 70));
        assertEquals("very hot", ClueScrolls.temperature(Tier.MASTER, 30));
        assertEquals("incredibly hot", ClueScrolls.temperature(Tier.MASTER, 5));
        assertEquals("visibly shaking", ClueScrolls.temperature(Tier.MASTER, 4));
        assertEquals("incredibly hot", ClueScrolls.temperature(Tier.BEGINNER, 4));
        assertEquals("visibly shaking", ClueScrolls.temperature(Tier.BEGINNER, 3));
        assertEquals(7, ClueScrolls.steps(0, 0, 7, -3));
    }

    @Test
    void guardiansAndClueNpcsExist() {
        for (String id : ClueScrolls.guardianMonsters()) {
            MonsterDef m = ContentRegistry.monster(id);
            assertNotNull(m, id);
            assertTrue(m.drops.isEmpty(), id + " drops nothing");
            assertTrue(SpawnTable.encounterOnly().containsKey(id), id + " only appears at its clue step");
        }
        assertEquals(97, ContentRegistry.monster("armadylean_guard").combatLevel);
        assertEquals(125, ContentRegistry.monster("bandosian_guard").combatLevel);
        assertEquals(140, ContentRegistry.monster("brassican_mage").combatLevel);
        assertEquals(141, ContentRegistry.monster("double_agent_141").combatLevel);
        // Clue NPCs stand where the clue says; none duplicates a character of the same name close by.
        List<ClueScrolls.Npc> npcs = ClueScrolls.npcs();
        for (ClueNpcs.ClueNpc n : ClueNpcs.ALL) {
            NpcDef def = ContentRegistry.npc(n.id());
            assertNotNull(def, n.id());
            for (ClueScrolls.Npc other : npcs) {
                if (!other.id().equals(n.id()) && other.name().equalsIgnoreCase(n.name())) {
                    assertTrue(Math.hypot(other.x() - n.x(), other.y() - n.y()) > 8, n.name() + " twice at " + n.x() + "," + n.y());
                }
            }
        }
        assertNotNull(ClueScrolls.resolveNpc("Charlie the Tramp", 3209, 3392, npcs));
        assertNotNull(ClueScrolls.resolveNpc("Sherlock", 2733, 3415, npcs));
        assertNotNull(ClueScrolls.resolveNpc("Cecilia", 2990, 3384, npcs));
        assertNotNull(ClueScrolls.resolveNpc("Falo the Bard", 2689, 3550, npcs));
        assertNotNull(ClueScrolls.resolveNpc("Watson", SystemsContent.WATSON_X, SystemsContent.WATSON_Y, npcs));
        assertNotNull(ClueScrolls.resolveNpc("Reldo", 3211, 3494, npcs));
        assertNotNull(ClueScrolls.resolveNpc("Jorral", 2436, 3347, npcs));
        for (String id : List.of("sextant", "watch", "chart", "strange_device", "strange_device_master", "key_medium", "key_elite",
                ClueRewards.MIMIC_ITEM)) {
            assertNotNull(ItemCatalog.get(id), id);
        }
    }

    @Test
    void sherlockChallengesAreModelled() {
        int modelled = 0;
        for (Clue c : ClueScrolls.clues()) {
            if (c.type() == StepType.SHERLOCK) {
                assertTrue(c.tier() == Tier.ELITE || c.tier() == Tier.MASTER, c.text());
                ClueChallenges.Challenge ch = ClueChallenges.of(c.text());
                if (ch != null) {
                    modelled++;
                    if (ch.kind() == ClueChallenges.Kind.OBTAIN || ch.kind() == ClueChallenges.Kind.MAKE
                            || ch.kind() == ClueChallenges.Kind.EQUIP) {
                        for (String id : ch.targets()) {
                            assertNotNull(ItemCatalog.get(id), c.text() + " -> " + id);
                        }
                    }
                    if (ch.kind() == ClueChallenges.Kind.KILL) {
                        assertTrue(ClueScrolls.monsterExists(ch.targets().get(0)), ch.targets().get(0));
                    }
                }
            }
        }
        assertEquals(ClueChallenges.all().size(), modelled, "every modelled challenge is a real Sherlock task");
        assertTrue(modelled >= 35, "modelled " + modelled);
        assertTrue(ClueChallenges.of("Mine a mithril ore.").matches(ClueChallenges.Kind.OBTAIN, "mithril_ore", 1));
        assertFalse(ClueChallenges.of("Craft multiple cosmic runes from a single essence.").matches(ClueChallenges.Kind.OBTAIN,
                "cosmic_rune", 1));
        assertTrue(ClueChallenges.of("Kill a Spiritual Mage while wearing something from their god.").matches(
                ClueChallenges.Kind.KILL, "Spiritual mage", 1));
    }

    @Test
    void reqStrings() {
        ClueReqs.Has has = new ClueReqs.Has() {
            @Override
            public boolean has(String item, int qty) {
                return item.equals("a") || item.equals("c") && qty <= 2;
            }

            @Override
            public boolean slotEmpty(String slot) {
                return !slot.equals("head");
            }
        };
        assertTrue(ClueReqs.missing("a;b|c*2;!gloves,ring", has, id -> id).isEmpty());
        assertEquals(List.of("b"), ClueReqs.missing("a;b", has, id -> id));
        assertEquals(1, ClueReqs.missing("!head", has, id -> id).size());
        assertEquals(1, ClueReqs.missing("c*3", has, id -> id).size());
        assertEquals(List.of("a", "b"), ClueReqs.items("a+b"));
    }

    private static Clue find(StepType type, String text) {
        return ClueScrolls.clues().stream().filter(c -> c.type() == type && c.text().equals(text)).findFirst().orElseThrow();
    }
}
