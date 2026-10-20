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

    @Test
    void dragonSlayerIIFollowsTheQuickGuide() {
        QuestSpec ds2 = spec("dragon_slayer_ii");
        assertEquals(8, ds2.stages.size(), "saved stage count");
        // Dallas Jones in the Musa Point pub, then the mural in Elvarg's lair and its spawn (wiki: level 100, 80 HP)
        com.runecraft.osrs.content.PlaceDef dallas = ContentRegistry.place("at_dallas_jones");
        assertEquals(List.of(2924, 3146), List.of(dallas.osrsX(), dallas.osrsY()));
        stats("ds2_spawn", 100, 80, 4);
        Trigger mural = find(ds2, t -> t.setFlags.contains("ds2_mural"), "the mural");
        assertTrue(mural.underground && mural.tools.contains("pickaxe") && "ds2_spawn".equals(mural.spawn));
        // 24 map pieces: 8 in the house, 4 fungi, 7 hook briar, 5 mushtree
        int pieces = ds2.triggers().stream().mapToInt(t -> t.give.getOrDefault("ds2_map_piece", 0)).sum();
        assertEquals(24, pieces);
        assertEquals(24, find(ds2, t -> t.setFlags.contains("ds2_map"), "the map").take.get("ds2_map_piece"));
        // the rowboat: 8 oak planks, 10 swamp paste, 12 nails
        Trigger boat = find(ds2, t -> t.setFlags.contains("ds2_boat"), "the rowboat");
        assertEquals(8, boat.take.get("oak_plank"));
        assertEquals(10, boat.take.get("swamp_paste"));
        // Robert the Strong in Bob's dream: level 224, 280 HP, melee only
        stats("ds2_robert_the_strong", 224, 280, 4);
        assertTrue(boss("ds2_robert_the_strong").immuneTo.containsAll(List.of(com.runecraft.osrs.AttackType.RANGED,
                com.runecraft.osrs.AttackType.MAGIC)));
        // the Kourend crypt: the plaque's clues, every arrangement of the four busts
        assertEquals(24, DS2_CRYPT_VARIANTS());
        QuestPuzzle.Variant first = GrandmasterQuests.DS2_CRYPT.rooms.get(0).variants().get(0);
        assertTrue(first.prompt().get(0).contains("Zartharim sat at the north"));
        assertEquals("Aivas", first.options().get(first.solutions().get(0).get(0)), "Zartharim's hero (Aivas) sits north");
        // the quest Vorkath (wiki: level 392, 460 HP), the Fire Waves on the mithril door's dragon heads, the four rulers
        stats("ds2_vorkath", 392, 460, 5);
        Trigger heads = find(ds2, t -> t.setFlags.contains("ds2_heads"), "the dragon heads");
        assertEquals(21, heads.take.get("fire_rune"));
        assertEquals(3, heads.take.get("blood_rune"));
        assertTrue(find(ds2, t -> t.setFlags.contains("ds2_council"), "the council").needFlags
                .containsAll(List.of("ds2_roald", "ds2_amik", "ds2_lathas", "ds2_brundt_army")));
        // Galvek (wiki): 1,200 HP over four forms in order fire, air (stat drain), water (tsunamis), earth (bind)
        BossDef galvek = boss("galvek");
        assertEquals(1200, galvek.hitpoints);
        assertEquals(6, galvek.attackSpeed);
        assertEquals(List.of(75, 50, 25), galvek.phases.stream().map(p -> p.belowPercent()).toList());
        assertTrue(galvek.phases.get(0).attacks().stream().anyMatch(a -> a.special() == com.runecraft.osrs.content.AttackDef.Special.DRAIN_STAT));
        assertTrue(galvek.phases.get(1).attacks().stream().anyMatch(a -> a.name().equals("tsunami")));
        assertTrue(galvek.phases.get(2).attacks().stream().anyMatch(a -> a.special() == com.runecraft.osrs.content.AttackDef.Special.FREEZE));
        // the assault: red, iron and brutal green dragons on the way, then the three waves
        for (String d : List.of("red_dragon", "iron_dragon", "brutal_green_dragon", "black_dragon", "rune_dragon")) {
            find(ds2, t -> t.kind == Trigger.Kind.KILL && t.target.equals(d), d);
        }
    }

    @Test
    void monkeyMadnessIIFollowsTheQuickGuide() {
        QuestSpec mm2 = spec("monkey_madness_ii");
        assertEquals(5, mm2.stages.size(), "saved stage count");
        // Kruk at his Monkey Madness II stats (wiki: combat 207, 210 HP, slash 33, ranged 37)
        stats("kruk", 207, 210, 4);
        assertEquals(37, boss("kruk").attacks.stream().filter(a -> a.style() == com.runecraft.osrs.AttackType.RANGED)
                .mapToInt(com.runecraft.osrs.content.AttackDef::maxHit).max().orElse(0));
        // chapter I: the handkerchief, Anita, the note, the translation, Assistant Lori, Auguste
        for (String npc : List.of("anita", "assistant_lori", "auguste", "awowogei", "assistant_le_smith", "mm2_monkey_guard")) {
            find(mm2, t -> t.kind == Trigger.Kind.TALK && t.target.equals(npc), npc);
        }
        // the troll generals fought until they give up (wiki: Kob 185 / 200 HP, Keef 178 / 180 HP)
        stats("mm2_kob", 185, 200, 5);
        stats("mm2_keef", 178, 180, 5);
        assertEquals("mm2_kob", find(mm2, t -> t.target.equals("kob"), "Kob").spawn);
        // Kruk's dungeon forks and the airship platform's six charges
        assertEquals(16, GrandmasterQuests.MM2_MAZE.rooms.get(0).variants().size());
        assertEquals(720, GrandmasterQuests.MM2_SABOTAGE.rooms.get(0).variants().get(0).solutions().size(), "six charges in any order");
        // the lab no longer asks for an onyx of the player's own (the charged onyx is in Glough's device)
        assertTrue(!find(mm2, t -> t.setFlags.contains("mm2_lab"), "the lab").take.containsKey("onyx"));
        // chapter IV: 4 tortured gorillas in the Stronghold, then 2 tortured and 2 demonic gorillas before Glough
        assertEquals(2, find(mm2, t -> t.kind == Trigger.Kind.KILL && t.target.equals("demonic_gorilla"), "demonic gorillas").count);
    }

    @Test
    void songOfTheElvesFollowsTheQuickGuide() {
        QuestSpec sote = spec("song_of_the_elves");
        assertEquals(5, sote.stages.size(), "saved stage count");
        // Elena's escape: the knight disguise, the thick clear liquid from her house's cabinet
        Trigger liquid = find(sote, t -> t.take.containsKey("sote_thick_liquid"), "the escape");
        assertEquals("elena", liquid.target);
        // the insurrection: three grain sacks burnt in mourner gear, then the market told of the new taxes
        assertEquals(3, sote.triggers().stream().filter(t -> t.target.startsWith("sote_grain_")).count());
        // the battles: 21 mourners (wiki: level 106, 100 HP), then the castle's knights and paladins (level 121)
        assertEquals(21, find(sote, t -> t.kind == Trigger.Kind.KILL && t.target.equals("sote_mourner"), "the mourners").count);
        stats("sote_mourner", 106, 100, 4);
        stats("sote_paladin", 121, 82, 5);
        // Baxtorian's tomb: six offerings by the pillars' numbers, in pillar order
        Trigger pillars = find(sote, t -> t.puzzle == ElfQuests.SOTE_PILLARS, "the pillars");
        assertTrue(pillars.take.keySet().containsAll(List.of("nature_rune", "irit_leaf", "black_knife", "wine_of_zamorak",
                "adamant_chainbody", "cabbage")));
        for (QuestPuzzle.Variant v : ElfQuests.SOTE_PILLARS.rooms.get(0).variants()) {
            // the south-west pillar states its number, which is its offering
            int sw = Integer.parseInt(v.prompt().get(1).replaceAll(".*South-west: 'I am (\\d)\\..*", "$1"));
            assertEquals(sw - 1, v.solutions().get(0).get(0));
        }
        // the Seal of Cadarn (wiki solution): east, down, south, west, south, red crystal, east, south
        QuestPuzzle.Variant cadarn = ElfQuests.SOTE_SEAL_CADARN.rooms.get(0).variants().get(0);
        List<String> beam = new ArrayList<>();
        cadarn.solutions().get(0).forEach(i -> beam.add(cadarn.options().get(i)));
        assertEquals(List.of("East", "Down", "South", "West", "South", "Red crystal", "East", "South"), beam);
        // Arianwyn the traitor (wiki: level 212, 300 HP), the orb, the elf lords, the clue digs and the spin with nothing equipped
        stats("sote_arianwyn", 212, 300, 5);
        assertTrue(find(sote, t -> t.setFlags.contains("sote_meilyr"), "Lady Meilyr").emptyHanded);
        // the dwarf camp: seven explosive potions, then Essyllt (stab, wiki defences) and the Fragment's ordered specials
        assertEquals(7, find(sote, t -> t.setFlags.contains("sote_holes"), "the holes").take.get("sote_explosive_potion"));
        assertTrue(boss("sote_essyllt").attacks.stream().anyMatch(a -> a.name().equals("sword") && a.style() == com.runecraft.osrs.AttackType.STAB));
        assertEquals(10, boss("fragment_of_seren").defenceBonus(com.runecraft.osrs.AttackType.MAGIC), "magic defence");
    }

    @Test
    void sinsOfTheFatherFollowsTheQuickGuide() {
        QuestSpec sotf = spec("sins_of_the_father");
        assertEquals(5, sotf.stages.size(), "saved stage count");
        // following Carl: four hiding spots in order, then Kroy (wiki: level 133, 209 HP) in his lab
        assertEquals(List.of(0, 1, 2, 3), MorytaniaQuests.SOTF_FOLLOW.rooms.get(0).variants().get(0).solutions().get(0));
        stats("sotf_kroy", 133, 209, 4);
        // the trek: two nail beasts, three logs for the bridge, two juvinates that only an Ivandis flail hurts
        assertEquals(2, find(sotf, t -> t.kind == Trigger.Kind.KILL && t.target.equals("sotf_nail_beast"), "the nail beasts").count);
        assertEquals(3, find(sotf, t -> t.setFlags.contains("sotf_bridge"), "the bridge").take.get("logs"));
        assertEquals("ivandis_flail", boss("sotf_juvinate").requiredWeapon);
        stats("sotf_damien", 204, 217, 5);
        // the arboretum's valves: 7 gallons a north turn, 4 a south turn, every amount with one setting
        List<String> amounts = new ArrayList<>();
        for (QuestPuzzle.Variant v : MorytaniaQuests.SOTF_VALVES.rooms.get(0).variants()) {
            List<Integer> turns = v.solutions().get(0);
            String gallons = v.prompt().get(0).replaceAll("\\D+", "");
            assertEquals(7 * turns.get(0) + 4 * turns.get(1), Integer.parseInt(gallons));
            assertTrue(!amounts.contains(gallons), "one setting for " + gallons + " gallons");
            amounts.add(gallons);
        }
        assertEquals(35, amounts.size());
        // the blisterwood flail: eight logs (seven kept by Vertida), a ruby, 5 fire and 1 cosmic, the Ivandis flail
        Trigger flail = find(sotf, t -> t.setFlags.contains("sotf_flail"), "the flail");
        assertEquals(8, flail.take.get("blisterwood_logs"));
        assertTrue(flail.take.keySet().containsAll(List.of("ruby", "fire_rune", "cosmic_rune", "ivandis_flail")));
        // Vanstrom Klause at his Sins of the Father stats (wiki: level 459, 750 HP, magic every 5 ticks, blisterwood only)
        stats("vanstrom_klause", 459, 750, 5);
        assertEquals("blisterwood_flail", boss("vanstrom_klause").requiredWeapon);
        assertTrue(boss("vanstrom_klause").attacks.stream().map(a -> a.name()).toList()
                .containsAll(List.of("pets", "blood_orb", "stare_into_darkness", "lightning")));
    }

    private static int DS2_CRYPT_VARIANTS() {
        return GrandmasterQuests.DS2_CRYPT.rooms.get(0).variants().size();
    }
}
