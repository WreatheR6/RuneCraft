package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Quest.Difficulty;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.Drop;

/** Quests that don't fit a regional group: The Slug Menace. */
final class MiscQuests {
    private MiscQuests() {
    }

    static void register(QuestBook b) {
        b.place("witchaven", "Witchaven", 2720, 3285, PlaceDef.Style.TOWN);
        b.place("witchaven_dungeon", "Witchaven dungeon", 2696, 3283, PlaceDef.Style.CAVE);
        slugMenace(b);
    }

    private static void slugMenace(QuestBook b) {
        b.npc("sir_tiffy_cashien", "Sir Tiffy Cashien", "falador_park", -3, -2, 0xF0C8B0, 0xE0E0F1, 0x6A3A8A, 0xD0B060);
        b.npc("mayor_hobb", "Mayor Hobb", "witchaven", 0, 3, 0xE0B8A1, 0x6A2A2A, 0x3A1A1A, 0xA0A0A1);
        b.npc("brother_maledict", "Brother Maledict", "witchaven", 3, -3, 0xE0B8A2, 0x6A4A3C, 0x6A4A3C, 0x2A2A2C);
        b.npc("holgart", "Holgart", "witchaven", -3, -3, 0xD8A883, 0x3A6A8A, 0x2A3A4A, 0x6A4A2B);
        b.item("slug_rune_fragments", "Fragments", "rune", 0x8A9A5A, "Fragments of an ancient tablet, with runic symbols.");
        b.boss(BossDef.of("slug_prince", "Slug Prince").level(62).stats(70, 50, 55, 60, 1, 1).speed(5)
                .attack(AttackDef.of("slime", AttackType.CRUSH, 9, 2.5, 7).special(Special.POISON, 5))
                .attack(AttackDef.of("acid_spit", AttackType.RANGED, 8, 8, 3).special(Special.HAZARD, 2))
                .drops(Drop.always("bones")).size(1.2f, 1.4f, 1.0f).at("witchaven_dungeon").arena(9, "water"));
        b.quest("the_slug_menace", "The Slug Menace", Difficulty.INTERMEDIATE, 1)
                .reqQuest("wanted", "sea_slug")
                .req(Skill.CRAFTING, 30).req(Skill.RUNECRAFT, 30).req(Skill.SLAYER, 30).req(Skill.THIEVING, 30)
                .xp(Skill.CRAFTING, 3500).xp(Skill.RUNECRAFT, 3500).xp(Skill.THIEVING, 3500)
                .unlock("Proselyte armour from the Temple Knights")
                .stage("I can start this quest by speaking to Sir Tiffy Cashien in Falador Park. (Requires 30 Crafting, Runecraft, Slayer and Thieving.)")
                .talk("sir_tiffy_cashien").say("What-ho! The sea slugs are back at Witchaven, and Mayor Hobb is behaving strangely. Investigate, there's a good chap.")
                .stage("I should investigate Witchaven. Mayor Hobb is acting oddly.")
                .reach("witchaven", 10).stay().unlessFlag("sm_notices").flag("sm_notices")
                .say("* The noticeboard announces that the Temple Knights' commorb is 'lost', and that nobody may go near the coast.")
                .talk("mayor_hobb").needFlag("sm_notices").missing("Nothing strange here! Read the notices if you have questions.").say("Nothing strange here! Everything's fine! *A slug twitches on the back of his neck.*")
                .stage("Brother Maledict might help. He has been studying strange runic fragments.")
                .talk("brother_maledict").take("pure_essence:5").give("slug_rune_fragments")
                .say("These fragments show five runes. Imbue essence with them and you can break the slugs' hold. *You craft the runes.*")
                .missing("I need 5 pure essence to inscribe the runes.")
                .stage("With the runic fragments, I should confront the Mother Mallum's prince in the dungeon beneath Witchaven.")
                .talk("holgart").stay().say("The slugs are in the old dungeon by the coast. I'll row you if you need. Go careful.")
                .kill("slug_prince").take("slug_rune_fragments").say("* The Slug Prince melts away, and the slugs release their hosts.")
                .stage("I should report to Sir Tiffy Cashien.")
                .talk("sir_tiffy_cashien").say("Splendid! Witchaven is saved. The Temple Knights can kit you out in proselyte armour now, old bean.")
                .done("mayor_hobb", "I feel... much better. Thank you.");
    }
}
