package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.quests.QuestLamps.Grant;
import com.runecraft.osrs.content.quests.QuestLamps.Kind;
import com.runecraft.osrs.content.quests.QuestLamps.Lamp;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Quest lamps, tomes and skill choices against the OSRS Wiki snapshot {@code quest_lamps.tsv}: every quest whose OSRS reward
 * is XP the player places hands out the real lamp (or choice) instead of fixed XP in a skill the mod picked.
 */
class QuestLampsTest {
    /** Quests the reference marks with a lamp or choice that aren't quest-module lamps, and why. */
    private static final Map<String, String> NOT_QUEST_LAMPS = Map.ofEntries(
            Map.entry("making_history", "Varrock Museum: Historian Minas's lamp (MuseumService)"),
            Map.entry("merlins_crystal", "Varrock Museum: Historian Minas's lamp (MuseumService)"),
            Map.entry("shield_of_arrav", "Varrock Museum: Historian Minas's lamp (MuseumService)"),
            Map.entry("defender_of_varrock", "Varrock Museum: Historian Minas's lamp (MuseumService)"),
            Map.entry("curse_of_the_empty_lord", "Varrock Museum: Historian Minas's lamp (MuseumService)"),
            Map.entry("desert_treasure_i", "fixed Magic XP only (the reference's 'any' matched the ancient staff text)"),
            Map.entry("family_pest", "gauntlets, no XP"),
            Map.entry("garden_of_tranquillity", "fixed Farming XP"),
            Map.entry("horror_from_the_deep", "a choice of damaged book, not XP"),
            Map.entry("mage_arena_i", "a choice of god cape and staff, not XP"),
            Map.entry("mage_arena_ii", "imbued god cape, not XP"),
            Map.entry("recruitment_drive", "fixed XP"),
            Map.entry("RUNE_MYSTERIES", "unlocks Runecraft as a lamp skill, no lamp"),
            Map.entry("the_eyes_of_glouphrie", "fixed XP and the crystal saw seed"));

    record Row(String quest, String item, Kind kind, int count, int xp, int uses, String skills, int minLevel) {
    }

    static List<Row> rows() throws IOException {
        List<Row> out = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of("src/test/resources/osrs/quests/quest_lamps.tsv"), StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#") || line.startsWith("quest\t")) {
                continue;
            }
            String[] c = line.split("\t", -1);
            out.add(new Row(c[0], c[1], Kind.valueOf(c[2]), Integer.parseInt(c[3]), Integer.parseInt(c[4]), Integer.parseInt(c[5]),
                    c[6], Integer.parseInt(c[7])));
        }
        return out;
    }

    private static Set<Skill> skills(String s) {
        return switch (s) {
            case "ANY" -> QuestLamps.ANY;
            case "COMBAT" -> QuestLamps.COMBAT;
            case "COMBAT_PRAYER" -> QuestLamps.COMBAT_PRAYER;
            case "MELEE", "PACKAGES" -> QuestLamps.MELEE;
            default -> {
                Set<Skill> set = EnumSet.noneOf(Skill.class);
                for (String part : s.split(",")) {
                    set.add(Skill.valueOf(part));
                }
                yield set;
            }
        };
    }

    @Test
    void everyGrantMatchesTheWiki() throws IOException {
        List<Row> rows = rows();
        List<String> problems = new ArrayList<>();
        Set<Grant> matched = new HashSet<>();
        for (Row r : rows) {
            Grant found = null;
            for (Grant g : QuestLamps.grants(r.quest())) {
                Lamp l = QuestLamps.get(g.lamp());
                if (!matched.contains(g) && l.kind() == r.kind() && l.xp() == r.xp() && l.skills().equals(skills(r.skills()))) {
                    found = g;
                    break;
                }
            }
            if (found == null) {
                problems.add(r.quest() + ": no " + r.kind() + " of " + r.xp() + " XP in " + r.skills() + " (" + r.item() + ")");
                continue;
            }
            matched.add(found);
            Lamp l = QuestLamps.get(found.lamp());
            if (found.count() != r.count() || l.uses() != r.uses() || l.minLevel() != r.minLevel()) {
                problems.add(r.quest() + " " + l.id() + ": count/uses/level " + found.count() + "/" + l.uses() + "/" + l.minLevel()
                        + " != " + r.count() + "/" + r.uses() + "/" + r.minLevel());
            }
            if (r.skills().equals("PACKAGES") != !l.packages().isEmpty()) {
                problems.add(r.quest() + " " + l.id() + ": packages");
            }
        }
        for (Grant g : QuestLamps.grants()) {
            if (!matched.contains(g)) {
                problems.add(g.quest() + " " + g.lamp() + ": not in the wiki snapshot");
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void daerosTrainingIsThirtyFiveAndTwentyThousand() {
        Lamp daero = QuestLamps.get("mm1_daero_training");
        assertEquals(2, daero.packages().size());
        for (QuestLamps.Package p : daero.packages()) {
            assertEquals(110_000, p.xp().values().stream().mapToInt(Integer::intValue).sum(), p.label());
            assertEquals(QuestLamps.MELEE, p.xp().keySet());
        }
        assertEquals(Map.of(Skill.ATTACK, 35_000, Skill.DEFENCE, 35_000, Skill.STRENGTH, 20_000, Skill.HITPOINTS, 20_000),
                QuestLamps.xpOf(daero, null, 0));
        assertEquals(Map.of(), QuestLamps.xpOf(daero, null, 2));
        // Duke trains only players Daero trained (wiki)
        Grant duke = QuestLamps.grants("monkey_madness_ii").get(0);
        assertEquals("mm1_daero_training", duke.needsChoice());
    }

    @Test
    void everyLampOrChoiceQuestIsCovered() throws IOException {
        List<String> problems = new ArrayList<>();
        for (QuestReferenceTest.Ref r : QuestReferenceTest.reference()) {
            if (!r.xpChoice() || r.id().equals("-")) {
                continue;
            }
            boolean lamps = !QuestLamps.grants(r.id()).isEmpty();
            if (lamps == NOT_QUEST_LAMPS.containsKey(r.id())) {
                problems.add(r.id() + (lamps ? ": has lamps but is listed as none" : ": lamp or choice reward not handed out"));
            }
        }
        for (String q : QuestLamps.quests()) {
            assertNotNull(ContentRegistry.quest(q), q);
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    /** A lamp quest's fixed XP is exactly OSRS's: the lamp's XP no longer also lands in a skill the mod picked. */
    @Test
    void lampQuestsGiveOnlyTheirFixedXp() throws IOException {
        List<String> problems = new ArrayList<>();
        for (QuestReferenceTest.Ref r : QuestReferenceTest.reference()) {
            if (QuestLamps.grants(r.id()).isEmpty()) {
                continue;
            }
            QuestInfo q = ContentRegistry.quest(r.id());
            Map<Skill, Double> xp = QuestReferenceTest.mod(q).xp();
            if (!xp.equals(r.xp())) {
                problems.add(r.id() + ": " + xp + " != " + r.xp());
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void lampItemsAreRegisteredAndReachTheirNpcs() {
        Set<String> npcIds = new HashSet<>();
        QuestBook.get().npcs.forEach(n -> npcIds.add(n.id()));
        Set<String> ids = new HashSet<>();
        for (Lamp l : QuestLamps.all()) {
            assertTrue(ids.add(l.id()), "duplicate " + l.id());
            // the diary and Combat Achievement lamps are routed by these prefixes (SystemsHooks)
            assertFalse(l.id().startsWith("antique_lamp_") || l.id().startsWith("ca_antique_lamp_"), l.id());
            assertTrue(l.uses() >= 1 && l.minLevel() >= 1, l.id());
            if (l.kind() == Kind.CHOICE) {
                assertFalse(ItemCatalog.exists(l.id()), l.id() + " is a choice, not an item");
                assertNotNull(l.npc(), l.id() + " needs an NPC to re-offer the choice");
            } else {
                assertTrue(ItemCatalog.exists(l.id()), l.id() + " not registered");
                assertEquals(l.kind() == Kind.TOME ? "Tome of experience" : ItemCatalog.get(l.id()).name(), ItemCatalog.get(l.id()).name());
            }
            if (l.npc() != null) {
                assertTrue(npcIds.contains(l.npc()), l.id() + ": unknown NPC " + l.npc());
            }
            assertFalse(l.describe().isBlank());
        }
        // the old In Search of Knowledge reward rubs as the lamp of knowledge
        assertEquals("lamp_of_knowledge", QuestLamps.get("book_of_knowledge").id());
        assertNull(QuestLamps.get("antique_lamp_easy"));
    }

    /**
     * The Varrock Museum's antique lamps (wiki, Antique lamp (Varrock Museum)) are real lamps the player rubs: Historian Minas's
     * for Shield of Arrav, Merlin's Crystal and Making History (1,000 XP, level 20), Defender of Varrock (5,000, 30) and Curse of
     * the Empty Lord (10,000, 50), and the specimen crate's (500, 10). None can be replaced.
     */
    @Test
    void museumLampsAreRubbedForAnySkill() {
        int lamps = 0;
        for (com.runecraft.osrs.content.minigames.VarrockMuseum.QuestKudos q : com.runecraft.osrs.content.minigames.VarrockMuseum.QUEST_KUDOS) {
            if (q.lampXp() == 0) {
                continue;
            }
            lamps++;
            Lamp l = QuestLamps.get(com.runecraft.osrs.content.minigames.VarrockMuseum.lampItem(q.lampXp()));
            assertNotNull(l, q.quest());
            assertEquals(q.lampXp(), l.xp(), q.quest());
            assertEquals(q.lampLevel(), l.minLevel(), q.quest());
            assertEquals(QuestLamps.ANY, l.skills(), q.quest());
            assertEquals(Kind.LAMP, l.kind());
            assertNull(l.npc(), "the museum's lamps can't be replaced");
            assertTrue(QuestLamps.commonItem(l.id()));
            assertTrue(ItemCatalog.exists(l.id()), l.id());
            assertEquals("Antique lamp", ItemCatalog.get(l.id()).name());
        }
        assertEquals(5, lamps);
        Lamp crate = QuestLamps.get(com.runecraft.osrs.content.minigames.VarrockMuseum.lampItem(
                com.runecraft.osrs.content.minigames.VarrockMuseum.CRATE_LAMP_XP));
        assertEquals(500, crate.xp());
        assertEquals(10, crate.minLevel());
    }

    @Test
    void kingdomDividedAndArchitecturalAllianceGiveTwoLampsBetweenThem() {
        String lamp = "quest_lamp_a_kingdom_divided";
        Grant akd = QuestLamps.grants("a_kingdom_divided").get(0);
        Grant aa = QuestLamps.grants("architectural_alliance").get(0);
        // A Kingdom Divided first: two lamps, and Hosa then has none left to give
        assertEquals(2, QuestLamps.count(akd, q -> false));
        assertEquals(0, QuestLamps.count(aa, q -> q.equals("a_kingdom_divided")));
        // Hosa first: one lamp, and Commander Fullore gives one more
        assertEquals(1, QuestLamps.count(aa, q -> false));
        assertEquals(1, QuestLamps.count(akd, q -> q.equals("architectural_alliance")));
        Map<Grant, Integer> given = new HashMap<>(Map.of(aa, 1, akd, 1));
        assertEquals(2, QuestLamps.owedUses(lamp, g -> given.getOrDefault(g, 0), 0));
        assertEquals(1, QuestLamps.owedUses(lamp, g -> given.getOrDefault(g, 0), 1));
        // one rubbed, one in the bank: nothing to reclaim; one rubbed and the other lost: Commander Fullore gives it back
        assertEquals(0, QuestLamps.reclaimable(lamp, g -> given.getOrDefault(g, 0), 1, 1));
        assertEquals(1, QuestLamps.reclaimable(lamp, g -> given.getOrDefault(g, 0), 1, 0));
    }

    @Test
    void tomesAndWishesCountByUse() {
        Lamp sins = QuestLamps.get("tome_of_experience_sins");
        Grant g = QuestLamps.grants("sins_of_the_father").get(0);
        assertEquals(Kind.TOME, sins.kind());
        assertEquals(6, QuestLamps.owedUses(sins.id(), x -> x == g ? 1 : 0, 0));
        assertEquals(1, QuestLamps.owedUses(sins.id(), x -> x == g ? 1 : 0, 5));
        // a tome with chapters left is one item to reclaim; a finished one is none
        assertEquals(1, QuestLamps.reclaimable(sins.id(), x -> x == g ? 1 : 0, 5, 0));
        assertEquals(0, QuestLamps.reclaimable(sins.id(), x -> x == g ? 1 : 0, 6, 0));
        // choices are never reclaimed as items
        Grant ds2 = QuestLamps.grants("dragon_slayer_ii").get(0);
        assertEquals(0, QuestLamps.reclaimable("ds2_ellen_training", x -> x == ds2 ? 1 : 0, 0, 0));
        assertEquals(4, QuestLamps.owedUses("ds2_ellen_training", x -> x == ds2 ? 1 : 0, 0));
        // a lamp with nobody to reclaim it from in OSRS
        Grant kr = QuestLamps.grants("kings_ransom").get(0);
        assertEquals(0, QuestLamps.reclaimable("quest_lamp_kings_ransom", x -> x == kr ? 1 : 0, 0, 0));
    }

    @Test
    void levelsAndSkillsGateTheChoice() {
        Lamp sins = QuestLamps.get("tome_of_experience_sins");
        assertTrue(sins.allows(Skill.SAILING, 60));
        assertFalse(sins.allows(Skill.SLAYER, 59));
        Lamp dreamy = QuestLamps.get("dreamy_lamp");
        assertFalse(dreamy.allows(Skill.ATTACK, 99), "no Attack from the dreamy lamp");
        assertFalse(dreamy.allows(Skill.PRAYER, 99), "no Prayer from the dreamy lamp");
        assertTrue(dreamy.allows(Skill.DEFENCE, 1));
        assertEquals(Skill.PRAYER, QuestLamps.get("dusty_lamp").fixedSkill());
        assertEquals(Map.of(Skill.STRENGTH, 30_000.0, Skill.SLAYER, 20_000.0, Skill.THIEVING, 5_000.0, Skill.MAGIC, 5_000.0),
                QuestLamps.fixedXp("the_path_of_glouphrie"));
        assertEquals("10,000 XP in any skill (level 40+)", QuestLamps.get("lamp_of_knowledge").describe());
    }
}
