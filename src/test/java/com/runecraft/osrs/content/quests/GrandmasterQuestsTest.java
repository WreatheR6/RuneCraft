package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.quests.QuestSpec.Trigger;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round 37: the grandmaster quests made playable step by step against the wiki quick guides and quest-helper (the order and
 * completeness of every step is planned by {@code QuestStepWalkTest}; this checks the content: NPCs, puzzles, foes at their
 * wiki stats, rewards).
 */
class GrandmasterQuestsTest {
    private static QuestSpec spec(String id) {
        QuestSpec q = QuestBook.get().spec(id);
        assertNotNull(q, id);
        return q;
    }

    private static Trigger find(QuestSpec q, Predicate<Trigger> p, String what) {
        return q.triggers().stream().filter(p).findFirst().orElseThrow(() -> new AssertionError(q.id() + " lacks " + what));
    }

    private static BossDef boss(String id) {
        BossDef b = ContentRegistry.boss(id);
        assertNotNull(b, id);
        return b;
    }

    /** Level, hitpoints and attack speed of a quest foe against its wiki infobox. */
    private static void stats(String id, int level, int hp, int speed) {
        BossDef b = boss(id);
        assertEquals(level, b.combatLevel, id + " level");
        assertEquals(hp, b.hitpoints, id + " hitpoints");
        assertEquals(speed, b.attackSpeed, id + " attack speed");
    }

    @Test
    void whileGuthixSleepsFollowsTheQuickGuide() {
        QuestSpec wgs = spec("while_guthix_sleeps");
        assertEquals(6, wgs.stages.size(), "saved stage count");
        // the rune door: the wiki's five stylised letters (O mind, H earth, E air, E fire, D water)
        QuestPuzzle door = GrandmasterQuests.WGS_DOOR;
        assertEquals(5, door.rooms.get(0).variants().size());
        List<String> answers = new ArrayList<>();
        for (QuestPuzzle.Variant v : door.rooms.get(0).variants()) {
            answers.add(v.options().get(v.solutions().get(0).get(0)));
        }
        assertEquals(List.of("Mind", "Earth", "Air", "Fire", "Water"), answers);
        // the temple: 8 of the 13 statues per player, Attack/Energy/Restoration/Strength with two accepted mixes
        assertEquals(13, GrandmasterQuests.WGS_STATUES.rooms.size());
        assertEquals(8, GrandmasterQuests.WGS_STATUES.picks);
        assertEquals(8, wgs.triggers().stream().filter(t -> t.puzzle == GrandmasterQuests.WGS_STATUES).count());
        assertEquals(2, GrandmasterQuests.WGS_STATUES.rooms.get(1).variants().get(0).solutions().size(), "Attack: guam or irit");
        // the broav: the Hunting expert, a pit trap baited with mort myre fungus, training
        Trigger trap = find(wgs, t -> t.target.equals("wgs_pit_trap"), "the pit trap");
        assertTrue(trap.take.containsKey("mort_myre_fungus") && trap.take.containsKey("logs") && trap.need.containsKey("knife"));
        // Idria held by two mercenary axemen and a mage north of McGrubor's Wood (wiki stats)
        stats("wgs_mercenary_axeman", 131, 110, 5);
        stats("wgs_mercenary_mage", 112, 150, 5);
        assertEquals(2, wgs.triggers().stream().filter(t -> t.kind == Trigger.Kind.KILL && t.target.equals("wgs_mercenary_axeman")).count());
        // NPC Contact with the four slayer masters (4 casts) and the Warriors' Guild recruits
        Trigger contact = find(wgs, t -> t.setFlags.contains("wgs_contacts"), "NPC Contact");
        assertEquals(4, contact.take.get("astral_rune"));
        assertEquals(8, contact.take.get("air_rune"));
        // three elite black knights for the armour, the Dark Squall's robes, Silif fed and freed, Surok, Lucien's camp
        assertEquals(3, wgs.triggers().stream().filter(t -> t.kind == Trigger.Kind.KILL && t.target.equals("elite_black_knight")).count());
        assertTrue(find(wgs, t -> t.setFlags.contains("wgs_elite"), "the third knight").give.keySet()
                .containsAll(List.of("elite_black_full_helm", "elite_black_platebody", "elite_black_platelegs")));
        assertTrue(find(wgs, t -> t.target.equals("silif") && t.setFlags.contains("wgs_silif"), "Silif").take.keySet()
                .containsAll(List.of("lobster", "restore_potion", "wgs_cell_key")));
        assertTrue(find(wgs, t -> t.setFlags.contains("wgs_orbed"), "the strange teleorb").need.keySet()
                .containsAll(List.of("strange_teleorb", "dark_squall_hood", "dark_squall_robe_top", "dark_squall_robe_bottom")));
        // the chasm: Movario fooled by the robes, the light creature and a lit sapphire lantern
        assertTrue(find(wgs, t -> t.target.equals("movario"), "Movario").need.containsKey("dark_squall_robe_top"));
        assertTrue(find(wgs, t -> t.target.equals("wgs_light_creature"), "the light creature").need.containsKey("sapphire_lantern"));
        // after the Balance Elemental, two tormented demons at the Stone (wiki: level 450, 600 HP)
        stats("wgs_tormented_demon", 450, 600, 6);
        QuestSpec.Stage last = wgs.stages.get(5);
        assertEquals(2, last.triggers.stream().filter(t -> t.kind == Trigger.Kind.KILL && t.target.equals("wgs_tormented_demon")).count());
        assertTrue(QuestWalk.advancing(last).needFlags.contains("wgs_demons"));
        // Movario's base: 100 death runes, fire runes, coal and magic logs (quest-helper item rewards)
        assertTrue(wgs.def.itemRewards().containsAll(List.of("death_rune:100", "fire_rune:100", "coal:100", "magic_logs:100")));
        // elite black armour bonuses (osrs-dps-calc equipment.json): platebody +46 stab, +8 prayer, 40 Defence
        com.runecraft.osrs.ItemDef body = com.runecraft.osrs.ItemCatalog.get("elite_black_platebody");
        assertNotNull(body);
        assertEquals(46, body.equipment().stats().stabDefence());
        assertEquals(40, body.equipment().requirements().get(com.runecraft.osrs.Skill.DEFENCE));
    }
}
