package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.Quest.Difficulty;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.Drop;

/** Morytania quests, from Priest in Peril to the Myreque saga and Sins of the Father, plus Animal Magnetism. */
final class MorytaniaQuests {
    private MorytaniaQuests() {
    }

    static void register(QuestBook b) {
        b.place("paterdomus", "Paterdomus", 3415, 3487, PlaceDef.Style.RUINS);
        b.place("mort_myre", "Mort Myre Swamp", 3440, 3335, PlaceDef.Style.NONE);
        b.place("nature_grotto", "Nature Grotto", 3440, 3345, PlaceDef.Style.CAVE);
        b.spot("alices_farm", "Alice's farm", 3626, 3527);
        b.spot("rimmington_mine", "Rimmington mine", 2977, 3240);
        b.place("fenkenstrain_castle", "Fenkenstrain's Castle", 3550, 3550, PlaceDef.Style.RUINS);
        b.spot("canifis_graveyard", "Experiment graveyard", 3505, 3570);
        b.spot("old_crone", "Old crone's house", 3735, 3530);
        b.place("abandoned_mine", "Abandoned Mine", 3440, 3232, PlaceDef.Style.CAVE);
        b.place("mos_le_harmless", "Mos Le'Harmless", 3680, 2970, PlaceDef.Style.TOWN);
        b.place("myreque_hideout", "Myreque hideout", 3505, 3450, PlaceDef.Style.CAVE);
        b.place("meiyerditch", "Meiyerditch", 3626, 3218, PlaceDef.Style.RUINS);
        b.place("quest_ver_sinhaza", "Ver Sinhaza", 3650, 3205, PlaceDef.Style.RUINS);
        b.place("darkmeyer", "Darkmeyer", 3600, 3380, PlaceDef.Style.RUINS);
        priestInPeril(b);
        natureSpirit(b);
        animalMagnetism(b);
        fenkenstrain(b);
        ghostsAhoy(b);
        hauntedMine(b);
        cabinFever(b);
        searchOfTheMyreque(b);
        darknessOfHallowvale(b);
        tasteOfHope(b);
        sinsOfTheFather(b);
    }

    private static void priestInPeril(QuestBook b) {
        b.npc("drezel", "Drezel", "paterdomus", 0, 3, 0xE8D0B8, 0x3A5ABA, 0x2A3A8A, 0xE0E0E0);
        b.item("golden_key", "Golden key", "key", 0xE0C040, "A shiny golden key from the monument.");
        b.item("iron_key_pip", "Iron key", "key", 0x6A6A6A, "The key to Drezel's cell.");
        b.item("blessed_water", "Bucket of blessed water", "pot", 0x8AC0E0, "Water blessed by Drezel. Vampyres hate it.");
        b.item("murky_water", "Bucket of murky water", "pot", 0x5A6A4A, "Water from the well beneath Paterdomus.");
        b.boss(BossDef.of("temple_guardian", "Temple guardian").level(30).stats(45, 30, 30, 30, 1, 1).speed(4)
                .attack(AttackDef.of("bite", AttackType.STAB, 5, 2.5, 10))
                .drops(Drop.always("bones")).size(1.0f, 1.2f, 1.0f).at("paterdomus").arena(8, "stone"));
        b.quest("priest_in_peril", "Priest in Peril", Difficulty.NOVICE, 1)
                .xp(Skill.PRAYER, 1406).reward("wolfbane")
                .unlock("Access to Morytania")
                .stage("I can start this quest by speaking to King Roald in Varrock Palace.")
                .talk("king_roald").say("I've had no word from Drezel, the priest of Paterdomus temple on the River Salve. Would you check on him?")
                .stage("I should visit Paterdomus temple, east of Varrock, and check on Drezel.")
                .reach("paterdomus", 8).say("* You knock on the temple door. A voice says: 'Kill the dog in the mausoleum below, it's been causing trouble.'")
                .stage("The voice asked me to kill the dog in the temple's mausoleum.")
                .kill("temple_guardian").say("* The 'dog' howls and dies. It looked more like a holy guardian...")
                .stage("I should tell King Roald what happened.")
                .talk("king_roald").say("You killed the temple guardian?! You fool! Zamorakians have taken the temple. Go back and help Drezel!")
                .stage("I should return to Paterdomus and free Drezel. The monuments hide a key; Drezel can bless water to destroy the vampyre in the coffin.")
                // Quest-helper steps: a Monk of Zamorak (in the temple) drops the golden key; it is swapped for the iron key on
                // a monument under the temple, where the well gives murky water for Drezel to bless.
                .kill("monk_of_zamorak").stay().unless("golden_key", "iron_key_pip").give("golden_key")
                .say("* The monk drops a golden key.")
                .reach("paterdomus", 8).stay().unless("iron_key_pip").take("golden_key").give("iron_key_pip")
                .say("* Beneath the temple you study the monuments. One holds an iron key; you swap the golden key for it.")
                .reach("paterdomus", 8).stay().unless("murky_water", "blessed_water").take("minecraft:bucket").give("murky_water")
                .say("* You fill your bucket at the well beneath the temple. The water is murky.")
                .talk("drezel").stay().unless("blessed_water").take("murky_water").give("blessed_water")
                .say("Bless this water and pour it on the vampyre's coffin. It'll keep him in his grave!")
                .talk("drezel").take("iron_key_pip", "blessed_water").say("* You unlock the cell door and pour the blessed water over the vampyre's coffin.",
                        "Thank you! Now the barrier of Saradomin must be repaired. The Zamorakians defiled the Salve. I need 50 rune essence.")
                .missing("I'm trapped in this cell! The monks carry a golden key that fits the monuments below. And bring a bucket of water from the well for me to bless.")
                .stage("I need to bring Drezel 50 rune essence to restore the holy barrier.")
                .talk("drezel").take("rune_essence:50").say("* The essence seals the holy barrier. The River Salve is pure again.",
                        "Morytania lies beyond. Take this blessed dagger, Wolfbane; it stops werewolves changing form.")
                .missing("I need 50 rune essence to repair the barrier.")
                .done("drezel", "Saradomin protect you in Morytania.");
    }

    private static void natureSpirit(QuestBook b) {
        b.npc("filliman_tarlock", "Filliman Tarlock", "mort_myre", 0, 0, 0xC8E8E0, 0x3A6A3A, 0x2A4A2A, 0xE8E8E8);
        b.npc("nature_spirit", "Nature spirit", "nature_grotto", 0, 2, 0xA0E0A0, 0x5AA05A, 0x3A8A3A, 0xD0F0A0);
        b.item("filliman_journal", "Journal", "book", 0x5A6A3A, "Filliman Tarlock's journal.");
        b.quest("nature_spirit", "Nature Spirit", Difficulty.INTERMEDIATE, 2)
                .xp(Skill.CRAFTING, 3000).xp(Skill.DEFENCE, 2000).xp(Skill.HITPOINTS, 2000) // OSRS (current quest XP rewards)
                .reqQuest("priest_in_peril", "RESTLESS_GHOST")
                .reward("silver_sickle_b")
                .stage("I can start this quest by speaking to Drezel beneath Paterdomus temple. (Requires Priest in Peril, Restless Ghost, 18 Crafting.)")
                .talk("drezel").say("Filliman Tarlock, a druid, went into the Mort Myre Swamp to fight its evil. I fear for him.")
                .stage("I should find Filliman Tarlock in the Mort Myre Swamp. I'll need an amulet of ghostspeak.")
                .talk("filliman_tarlock").need("ghostspeak_amulet").give("filliman_journal")
                .say("You can see me? I... I'm dead? Oh dear. Read my journal; I was trying to become a nature spirit!")
                .missing("* You see a faint shape but can't understand it. You need an amulet of ghostspeak.")
                .stage("I should read Filliman's journal and help him become the Nature spirit. A silver sickle blessed by Drezel will help.")
                .talk("drezel").take("silver_bar").give("silver_sickle").say("A silver sickle, shaped from your bar. Take it to Filliman's grotto to be blessed.")
                .missing("Bring me a silver bar and I'll have a sickle made.")
                .stage("I should take the journal and sickle to the grotto, help Filliman transform, and gather three Mort Myre fungi from the swamp's ghasts.")
                .drop("ghoul", "mort_myre_fungus", 2)
                .talk("nature_spirit").take("filliman_journal", "silver_sickle", "mort_myre_fungus:3")
                .say("* Filliman's spirit merges with the grotto and he becomes the Nature spirit!",
                        "I am reborn! I bless your sickle. The Mort Myre will heal in time. Drezel will want to know.")
                .missing("I need the journal, the silver sickle and three Mort Myre fungi to complete my transformation.")
                .stage("I should tell Drezel about Filliman's transformation.")
                .talk("drezel").say("A nature spirit in the Myre! Saradomin be praised. You've done well.")
                .done("nature_spirit", "Use the blessed sickle to cast Bloom in the swamp.");
    }

    private static void animalMagnetism(QuestBook b) {
        b.npc("ava", "Ava", "DRAYNOR_MANOR", 5, -4, 0xF0D0BA, 0x6A3A8A, 0x4A2A5A, 0x2A1A0A);
        b.npc("alice", "Alice", "alices_farm", 0, 0, 0xF0C8AD, 0xA06A3A, 0x5A3A2A, 0xC08A3A);
        b.npc("old_farmer_ghost", "Old farmer (ghost)", "alices_farm", 3, 2, 0xC8E8E1, 0x7A8A7A, 0x5A6A5A, 0x9A9A9A);
        b.item("undead_chicken", "Undead chicken", "creature", 0xB0C0A0, "A chicken from Alice's farm. It's... undead.");
        b.item("selected_iron", "Selected iron", "bar", 0x6A6A7A, "A magnetised iron bar.");
        b.item("undead_twigs", "Undead twigs", "log", 0x5A5A4A, "Twigs from the undead trees near Draynor Manor.");
        b.npc("am_witch", "Witch", "DRAYNOR_MANOR", -6, -6, 0xB8C898, 0x2A2A2A, 0x1A1A1A, 0x3A2A3A);
        b.item("bar_magnet", "Bar magnet", "bar", 0x8A3A3A, "A magnetised piece of selected iron.");
        b.item("polished_buttons", "Polished buttons", "gem", 0xD0C080, "Shiny buttons from the H.A.M. storeroom.");
        b.item("am_research_notes", "Research notes", "note", 0xE0D8C0, "Ava's garbled notes on the attractor, now deciphered.");
        b.quest("animal_magnetism", "Animal Magnetism", Difficulty.INTERMEDIATE, 1)
                .reqQuest("RESTLESS_GHOST", "ernest_the_chicken", "priest_in_peril")
                .req(Skill.SLAYER, 18).req(Skill.CRAFTING, 19).req(Skill.RANGED, 30).req(Skill.WOODCUTTING, 35)
                .xp(Skill.CRAFTING, 1000).xp(Skill.FLETCHING, 1000).xp(Skill.SLAYER, 1000).xp(Skill.WOODCUTTING, 2500)
                .reward("avas_attractor")
                .stage("I can start this quest by speaking to Ava in Draynor Manor. (Requires Restless Ghost, Ernest the Chicken, Priest in Peril.)")
                .talk("ava").say("I'm building a device to collect arrows. I need two undead chickens from Alice's farm near Port Phasmatys.")
                .stage("I should buy two undead chickens from Alice. Her late husband's ghost guards the farm.")
                .talk("old_farmer_ghost").stay().need("ghostspeak_amulet").unlessFlag("am_ghost").flag("am_ghost")
                .say("Alice wants to sell my chickens? ...Tell her I miss her. Very well, she may.")
                .talk("alice").needFlag("am_ghost").take("coins:10").give("undead_chicken:2")
                .say("He said that? Oh, my poor husband. Here, take two chickens.")
                .missing("My husband won't let me sell the chickens without his say. Talk to him; you'll need ghostspeak.")
                .stage("I should bring Ava the undead chickens.")
                .talk("ava").take("undead_chicken:2").say("Lovely, horrid chickens. Next I need a magnet. The witch in this manor can pick a suitable iron bar; then hammer it while facing north in the Rimmington mine.")
                .stage("I should get selected iron from the witch in Draynor Manor and magnetise it by hammering it at the Rimmington mine.")
                // Wiki walkthrough: bring 5 iron bars; the witch selects the right one.
                .talk("am_witch").stay().unless("selected_iron", "bar_magnet").take("iron_bar:5").give("selected_iron")
                .say("Iron for Ava? Hmm... this one. The others are rubbish, I'll keep them.")
                .missing("Bring me five iron bars and I'll select one fit for a magnet.")
                .reach("rimmington_mine", 8).need("hammer").take("selected_iron").give("bar_magnet").say("* You face north and hammer the selected iron. It becomes magnetic!")
                .stage("I should take the magnet to Ava.")
                .talk("ava").take("bar_magnet").say("Perfect. Now I need twigs from the undead trees outside this manor. Only a blessed mithril axe will cut them; Turael can bless one if you bring a holy symbol.")
                .stage("I need undead twigs (blessed mithril axe), then the solved research notes, hard leather and polished buttons for Ava's device.")
                // The blessed axe from Turael (mithril axe + holy symbol) is folded into the chop. // approx
                .reach("DRAYNOR_MANOR", 10).stay().need("mithril_axe", "holy_symbol").unless("undead_twigs", "am_research_notes").give("undead_twigs")
                .say("* Your blessed mithril axe bites into an undead tree. Some twigs fall off.")
                .missing("* The undead tree's bark turns your blade. (You need a mithril axe blessed with a holy symbol.)")
                .talk("ava").stay().unless("am_research_notes").take("undead_twigs").give("am_research_notes")
                .say("Splendid twigs. Now, my notes for the container are garbled... can you make sense of them?",
                        "* You puzzle over the notes and work out the pattern.")
                .reach("quest_ham_hideout", 8).stay().unless("polished_buttons").give("polished_buttons")
                .say("* In the H.A.M. storeroom you find some polished buttons.")
                .talk("ava").take("am_research_notes", "hard_leather", "polished_buttons")
                .say("* You stitch the hard leather and polished buttons to the pattern and hand Ava the container.",
                        "It works! Your very own Ava's attractor. With 50 Ranged, I'll sell you an improved accumulator for 999 coins.")
                .missing("I need the solved notes, hard leather and some polished buttons for the container.");
        // Shops are offered best-first (last declared first).
        b.shop("ava", "avas_attractor", 999, "animal_magnetism", null, null, 0, "Lost your attractor? I'll make another for 999 coins.");
        b.shop("ava", "avas_accumulator", 999, "animal_magnetism", null, Skill.RANGED, 50,
                "An accumulator collects even more of your ammunition. 999 coins.");
    }

    private static void fenkenstrain(QuestBook b) {
        b.npc("dr_fenkenstrain", "Dr Fenkenstrain", "fenkenstrain_castle", 0, 3, 0xD8C8B8, 0x3A3A3A, 0x2A2A2A, 0xC0C0C0);
        b.npc("fenkenstrains_monster", "Fenkenstrain's monster", "fenkenstrain_castle", 3, -3, 0x8AA08A, 0x4A3A2A, 0x3A2A1A, 0x2A2A1A);
        b.npc("rologarth", "Lord Rologarth", "fenkenstrain_castle", -3, -3, 0xC8E8E2, 0x6A1A3A, 0x4A1A2A, 0xE0C040);
        b.item("decapitated_head", "Decapitated head", "skull", 0xC0A090, "A head. Not pretty.");
        b.item("torso_fenk", "Torso", "meat_raw", 0xC09080, "A torso dug from a grave.");
        b.item("arms_fenk", "Arms", "meat_raw", 0xC09081, "A pair of arms.");
        b.item("legs_fenk", "Legs", "meat_raw", 0xC09082, "A pair of legs.");
        b.item("pickled_brain", "Pickled brain", "pot", 0xD0A0A0, "A brain in a jar, from Canifis.");
        b.items(s -> s.add("ring_of_charos", "Ring of charos", Kind.SIMPLE, "ring", 0x3A3A6A).value(1).stack(1)
                .examine("A ring that gives you a commanding presence."));
        b.quest("creature_of_fenkenstrain", "Creature of Fenkenstrain", Difficulty.INTERMEDIATE, 2)
                .reqQuest("priest_in_peril", "RESTLESS_GHOST").req(Skill.CRAFTING, 20).req(Skill.THIEVING, 25)
                .xp(Skill.THIEVING, 1000).reward("ring_of_charos")
                .stage("I can start this quest by speaking to Dr Fenkenstrain in his castle north of Canifis. (Requires Priest in Peril, Restless Ghost, 20 Crafting, 25 Thieving.)")
                .talk("dr_fenkenstrain").say("I am building... a man. I need body parts: a head, a torso, arms and legs. And a brain, of course.")
                .stage("I need a head, torso, arms and legs from the experiment graveyard, and a pickled brain from Canifis.")
                .reach("canifis_graveyard", 6).stay().unless("torso_fenk").give("torso_fenk", "arms_fenk", "legs_fenk", "decapitated_head")
                .say("* You dig up the graves. Ugh. You now have a torso, arms, legs and a head.")
                .talk("malak").stay().unless("pickled_brain").take("raw_shark").give("pickled_brain")
                .say("A brain? The fishmonger has one pickled... for a raw shark I'll fetch it for you.")
                .talk("dr_fenkenstrain").take("torso_fenk", "arms_fenk", "legs_fenk", "decapitated_head", "pickled_brain")
                .say("Wonderful parts! Now the lightning conductor on the roof must be repaired with silver to bring him to life.")
                .missing("I need a head, a torso, arms, legs and a brain.")
                .stage("I should repair the lightning conductor on the castle roof with a silver bar.")
                .talk("dr_fenkenstrain").take("silver_bar").say("* Lightning strikes the conductor! IT'S ALIVE!")
                .missing("Repair the conductor! Bring a silver bar.")
                .stage("The monster is alive, but miserable. I should speak to it, then steal the ring of charos from Fenkenstrain.")
                .talk("fenkenstrains_monster").stay().unlessFlag("cof_monster").flag("cof_monster")
                .say("Why did he make me? I want to be put out of my misery. He has a ring that commands the dead... take it from him.")
                .talk("dr_fenkenstrain").needFlag("cof_monster").reqSkill(Skill.THIEVING, 25)
                .say("* While Fenkenstrain rants about his creation, you slip the ring of charos from his finger.")
                .missing("What are you staring at? Get back to work! *He keeps a close eye on his ring.* (Requires 25 Thieving.)")
                .stage("I should steal Fenkenstrain's ring of charos... and let Lord Rologarth, the castle's rightful owner, know.")
                .talk("rologarth").say("You stole his ring and freed the monster? Then Fenkenstrain is finished. My castle is mine again. Keep the ring.")
                .done("dr_fenkenstrain", "Get out of my castle! Er... his castle.");
    }

    private static void ghostsAhoy(QuestBook b) {
        b.npc("velorina", "Velorina", "PORT_PHASMATYS", 4, 4, 0xC8E8E3, 0x6A8A8A, 0x4A5A5A, 0xA0C0C0);
        b.npc("necrovarus", "Necrovarus", "PORT_PHASMATYS", -4, -4, 0xC8D8E8, 0x2A1A3A, 0x1A0A2A, 0x1A1A1A);
        b.npc("old_crone", "Old crone", "old_crone", 0, 0, 0xD8C0B0, 0x3A2A2A, 0x2A1A1A, 0xA0A0A0);
        b.npc("gravingas", "Gravingas", "PORT_PHASMATYS", 4, -4, 0xC8E8E4, 0x8A2A2A, 0x5A1A1A, 0x6A6A6A);
        b.item("petition_form", "Petition form", "note", 0xE8E0C8, "A petition against Necrovarus, with ghostly signatures.");
        b.item("ectophial", "Ectophial", "vial", 0x7AE0A0, "A vial of ectoplasm. Empty it to teleport to the Ectofuntus.");
        b.item("model_ship", "Model ship", "package", 0x8A6A3A, "A model of a ship, Necrovarus's childhood toy.");
        b.spot("ghosts_ahoy_wreck", "Shipwreck west of Port Phasmatys", 3605, 3545); // approx
        b.quest("ghosts_ahoy", "Ghosts Ahoy", Difficulty.INTERMEDIATE, 2)
                .reqQuest("priest_in_peril", "RESTLESS_GHOST").req(Skill.AGILITY, 25).req(Skill.COOKING, 20)
                .xp(Skill.PRAYER, 2400).reward("ectophial")
                .stage("I can start this quest by speaking to Velorina in Port Phasmatys. (Requires Priest in Peril, Restless Ghost, 25 Agility, 20 Cooking.)")
                .talk("velorina").need("ghostspeak_amulet").say("Necrovarus forbids us ghosts from passing on. Please, help us! The old crone east of here knows him.")
                .missing("* You need an amulet of ghostspeak to understand her.")
                .stage("I should speak to the old crone east of Port Phasmatys. She wants Necrovarus's old toy ship back from the shipwreck.")
                .reach("ghosts_ahoy_wreck", 4).stay().reqSkill(Skill.AGILITY, 25).unless("model_ship").give("model_ship")
                .say("* You jump across the rocks to the wrecked ship. Among the debris on deck is a little model ship.")
                .missing("* The wreck lies beyond some slippery rocks. (Requires 25 Agility.)")
                .talk("old_crone").take("model_ship")
                .say("Necrovarus? I was his nanny! And that's his old toy ship! Seeing it may soften his heart... First, get a petition signed by ten ghosts.")
                .missing("Necrovarus? I was his nanny. Bring me his old toy ship from the wreck west of the port and I'll help you.")
                .stage("I need a petition signed by ghosts. Gravingas is organising the protests.")
                .talk("gravingas").give("petition_form").say("A petition against Necrovarus? I'll sign! And so will nine others. *Ghosts flock to sign.*")
                .stage("With the signed petition and my enchanted amulet, I should command Necrovarus to let the ghosts pass on.")
                .talk("necrovarus").need("ghostspeak_amulet").take("petition_form").say("NO! You dare command me? ...The amulet... I... cannot resist. Very well. The ghosts may pass on.")
                .stage("I should tell Velorina the good news.")
                .talk("velorina").say("We are free! Take this ectophial. Empty it and you'll be teleported to the Ectofuntus.")
                .done("velorina", "Thank you for freeing my people.");
    }

    private static void hauntedMine(QuestBook b) {
        b.npc("zealot", "Zealot", "abandoned_mine", 3, 3, 0xD8B8A8, 0x6A6A2A, 0x4A4A1A, 0x3A3A1A);
        b.item("crystal_mine_key", "Crystal-mine key", "key", 0x9AE0E0, "A key to the crystal-filled cavern of the Abandoned Mine.");
        b.item("salve_shard", "Salve shard", "gem", 0x9AE0C0, "A shard of salve crystal. Crafted into an amulet, it harms the undead.");
        b.boss(BossDef.of("treus_dayth", "Treus Dayth").level(95).stats(100, 90, 90, 80, 1, 1).speed(4)
                .attack(AttackDef.of("ghostly_touch", AttackType.CRUSH, 13, 2.5, 8).special(Special.DRAIN_STAT, Skill.STRENGTH.ordinal()))
                .attack(AttackDef.of("soul_blast", AttackType.MAGIC, 12, 8, 3).particle("minecraft:soul"))
                .drops(Drop.always("bones")).skin(0xC8E8E5, 0x6A7A7A, 0x4A5A5A, 0x9AB0B0).at("abandoned_mine").arena(10, "crypt"));
        b.quest("haunted_mine", "Haunted Mine", Difficulty.EXPERIENCED, 2)
                .reqQuest("priest_in_peril").req(Skill.CRAFTING, 35)
                .xp(Skill.STRENGTH, 22000).reward("salve_shard")
                .stage("I can start this quest by speaking to the Zealot outside the Abandoned Mine in southern Morytania. (Requires Priest in Peril, 15 Agility, 35 Crafting.)")
                .talk("zealot").say("The mine is haunted by the ghost of Treus Dayth, the old owner. There are glowing crystals deep inside, if you dare.")
                .stage("I should explore the Abandoned Mine and find a way into its deepest level.")
                .reach("abandoned_mine", 10).give("crystal_mine_key").say("* Deep in the mine you find a key to a crystal-filled cavern... and a terrible chill.")
                .stage("The ghost of Treus Dayth attacks!")
                .kill("treus_dayth").say("* Treus Dayth's ghost is destroyed. The crystal cavern is open.")
                .stage("I should take a shard of the salve crystal from the cavern.")
                .reach("abandoned_mine", 10).take("crystal_mine_key").say("* You chip a shard from the glowing salve crystal.").finish()
                .done("zealot", "You survived the mine? Saradomin truly watches over you.");
    }

    private static void cabinFever(QuestBook b) {
        b.npc("bill_teach", "Bill Teach", "PORT_PHASMATYS", -6, 4, 0xD8A882, 0x1A1A1A, 0x2A2A2A, 0xC0C0C0);
        b.npc("mama_lehar", "Mama", "mos_le_harmless", 3, 3, 0xA0785A, 0x8A2A2A, 0x5A1A1A, 0x3A2A1A);
        b.item("plunder", "Plunder", "coins", 0xE0C040, "Pirate plunder from Mos Le'Harmless.");
        b.quest("cabin_fever", "Cabin Fever", Difficulty.EXPERIENCED, 2)
                .reqQuest("pirates_treasure", "priest_in_peril").reqQuest("rum_deal")
                .req(Skill.AGILITY, 42).req(Skill.CRAFTING, 45).req(Skill.SMITHING, 50).req(Skill.RANGED, 40)
                .xp(Skill.AGILITY, 7000).xp(Skill.CRAFTING, 7000).xp(Skill.SMITHING, 7000)
                .reward("coins:10000")
                .stage("I can start this quest by speaking to Bill Teach in Port Phasmatys. (Requires Pirate's Treasure, Priest in Peril and more.)")
                .talk("bill_teach").travel("mos_le_harmless")
                .say("Arr, I need a crew to sail to Mos Le'Harmless! *Halfway there, rival pirates board the Adventurous. You fight them off and limp into port.*")
                .stage("The ship is damaged. I should repair the hull (5 planks, 2 steel bars) and gather plunder in Mos Le'Harmless.")
                .talk("mama_lehar").stay().unless("plunder").take("coins:1000").give("plunder:10")
                .say("Want plunder? A thousand coins and Mama finds you plenty. No questions.")
                .talk("bill_teach").take("plank:5", "steel_bar:2", "plunder:10").travel("PORT_PHASMATYS")
                .say("* You patch the hull, load the cannons and stash the plunder. The Adventurous sails home in triumph!",
                        "Ye're a true pirate! Here be yer share.")
                .missing("We need 5 planks and 2 steel bars for the hull, and 10 plunder for the hold.")
                .done("bill_teach", "Sail with me again any time, matey!");
    }

    private static void searchOfTheMyreque(QuestBook b) {
        b.npc("vanstrom_disguised", "Vanstrom Klause", "CANIFIS", -6, -6, 0xD8D0D8, 0x3A2A4A, 0x2A1A3A, 0x1A1A1A);
        b.npc("veliaf_hurtz", "Veliaf Hurtz", "myreque_hideout", 0, 2, 0xE0B89A, 0x5A3A2A, 0x3A2A1A, 0x6A3A1A);
        b.npc("ivan_strom", "Ivan Strom", "myreque_hideout", 3, -2, 0xF0D0BB, 0x6A2A6A, 0x3A1A3A, 0xC08A3A);
        b.npc("sani_piliu", "Sani Piliu", "myreque_hideout", -3, -2, 0xE8C0A8, 0x3A6A3A, 0x2A4A2A, 0x2A1A0C);
        b.boss(BossDef.of("quest_skeleton_hellhound", "Skeleton hellhound").level(97).stats(80, 80, 80, 80, 1, 1).speed(4)
                .attack(AttackDef.of("bite", AttackType.STAB, 12, 2.5, 10))
                .drops(Drop.always("bones")).size(1.0f, 1.2f, 1.0f).at("myreque_hideout").arena(9, "crypt"));
        b.quest("in_search_of_the_myreque", "In Search of the Myreque", Difficulty.INTERMEDIATE, 2)
                .xp(Skill.ATTACK, 600).xp(Skill.STRENGTH, 600).xp(Skill.DEFENCE, 600).xp(Skill.HITPOINTS, 600).xp(Skill.CRAFTING, 600) // OSRS (current quest XP rewards)
                .reqQuest("nature_spirit").req(Skill.AGILITY, 25)
                .stage("I can start this quest by speaking to Vanstrom Klause in the Canifis pub. (Requires Nature Spirit, 25 Agility.)")
                .talk("vanstrom_disguised").say("The Myreque, a band of vampyre hunters, hide somewhere in Morytania. I have a message for them. Find them for me.")
                .stage("I should find the Myreque's hideout. The swamp is dangerous; a boat leaves from the Mort Myre.")
                .reach("myreque_hideout", 10).take("plank:6", "steel_nails:75").need("hammer").reqSkill(Skill.AGILITY, 25)
                .say("* You repair the rotten swamp bridge with planks and nails and cross it.",
                        "* You find a trapdoor beneath a tree. Below, armed men surround you.")
                .missing("* The bridge across the swamp is rotten. You'll need 6 planks, 75 steel nails and a hammer to repair it.")
                .stage("I should speak to the Myreque members.")
                .talk("ivan_strom").stay().unlessFlag("isotm_ivan").flag("isotm_ivan").say("Who are you? Are you here to join us?")
                .talk("sani_piliu").stay().unlessFlag("isotm_sani").flag("isotm_sani").say("Vanstrom sent you? I don't trust that man.")
                .talk("veliaf_hurtz").needFlag("isotm_ivan", "isotm_sani").missing("Talk to the others first; they'll want to know who you are.").say("Who sent you? Vanstrom Klause? Then you've been followed! A skeleton hellhound is in the tunnels!")
                .stage("I must defeat the skeleton hellhound that followed me.")
                .kill("quest_skeleton_hellhound").say("* The hellhound crumbles. The Myreque lower their weapons.")
                .stage("I should report to Veliaf Hurtz.")
                .talk("veliaf_hurtz").say("You fought for us. Vanstrom is a vampyre, one of the Drakans! We'll need your help again. Welcome, friend of the Myreque.")
                .done("ivan_strom", "I'll be a great vampyre hunter one day!")
                .done("sani_piliu", "Vanstrom Klause... I knew there was something wrong with him.");
    }

    private static void darknessOfHallowvale(QuestBook b) {
        b.npc("safalaan", "Safalaan Hallow", "meiyerditch", 2, 2, 0xD8D8E0, 0x3A5A8A, 0x2A3A5A, 0xE0E0E0);
        b.item("sketch_of_hallowvale", "Sketches", "note", 0xD8D0B8, "Sketches of the old city of Hallowvale.");
        b.quest("darkness_of_hallowvale", "Darkness of Hallowvale", Difficulty.EXPERIENCED, 2)
                .reqQuest("in_search_of_the_myreque").reqQuest("in_aid_of_the_myreque")
                .req(Skill.CONSTRUCTION, 5).req(Skill.MINING, 20).req(Skill.THIEVING, 22).req(Skill.AGILITY, 26)
                .req(Skill.CRAFTING, 32).req(Skill.MAGIC, 33).req(Skill.STRENGTH, 40)
                .xp(Skill.AGILITY, 7000).xp(Skill.THIEVING, 6000).xp(Skill.CONSTRUCTION, 2000)
                .stage("I can start this quest by speaking to Veliaf Hurtz. (Requires In Search of the Myreque and more.)")
                .talk("veliaf_hurtz").say("We've lost contact with Safalaan, our leader, in Meiyerditch. Vanstrom's spies are everywhere. Find him.")
                // OSRS: repair the boat and chute at the Burgh de Rott boathouse, then sneak through Meiyerditch's floorboards and walls.
                .stage("I should repair the boat south-east of Burgh de Rott, sneak into Meiyerditch, the vampyre-ruled ghetto, and find Safalaan.")
                .reach("burgh_de_rott", 10).stay().unlessFlag("doh_boat").take("plank:2").need("hammer", "steel_nails:10").flag("doh_boat") // approx: nails
                .say("* You patch the boat with a plank and fix the boat chute with another, then push the boat down the chute and row north.")
                .missing("* The boat and its chute are broken. You'll need 2 planks, a hammer and some nails.")
                .reach("meiyerditch", 10).stay().needFlag("doh_boat").unlessFlag("doh_ghetto").reqSkill(Skill.AGILITY, 26).flag("doh_ghetto")
                .say("* You kick through the floor, climb over the rubble and creep across floorboards and through hidden walls, past the vyrewatch.")
                .talk("safalaan").needFlag("doh_ghetto").missing("*There's nobody here who'll admit to being Safalaan. Find a way into the ghetto first.*")
                .give("sketch_of_hallowvale").say("You found me! I need sketches of the old city of Hallowvale. *You sketch the ruined statues for him.*")
                .stage("I should take the sketches back to Veliaf.")
                .talk("veliaf_hurtz").take("sketch_of_hallowvale").say("Safalaan is a Hallow, of the old royal line? Then there is hope. Thank you, friend.");
    }

    private static void tasteOfHope(QuestBook b) {
        b.npc("vertida_sefalatis", "Vertida Sefalatis", "quest_ver_sinhaza", -2, 3, 0xE8C0A9, 0x5A6A2A, 0x3A4A1A, 0x2A1A0D);
        b.boss(BossDef.of("ranis_drakan", "Ranis Drakan").level(233).stats(500, 200, 200, 150, 150, 1).speed(4)
                .defenceBonus(80, 80, 80, 50, 80)
                .requiresWeapon("ivandis_flail")
                .attack(AttackDef.of("claw", AttackType.SLASH, 30, 2.5, 6).special(Special.LIFESTEAL, 30))
                .attack(AttackDef.of("blood_storm", AttackType.MAGIC, 25, 8, 3).special(Special.AOE, 4).particle("minecraft:crimson_spore"))
                .drops(Drop.always("bones")).size(0.8f, 2.4f, 1.2f).at("quest_ver_sinhaza").arena(12, "crypt"));
        b.quest("a_taste_of_hope", "A Taste of Hope", Difficulty.EXPERIENCED, 1)
                .reqQuest("darkness_of_hallowvale")
                .req(Skill.CRAFTING, 48).req(Skill.AGILITY, 45).req(Skill.ATTACK, 40).req(Skill.HERBLORE, 40).req(Skill.SLAYER, 38)
                .xp(Skill.ATTACK, 2500).xp(Skill.AGILITY, 2500).xp(Skill.HERBLORE, 2500).xp(Skill.SLAYER, 2500)
                .stage("I can start this quest by speaking to Safalaan Hallow in Meiyerditch. (Requires Darkness of Hallowvale and more.)")
                .talk("safalaan").say("A vampyre in the Drakans' court, Flaygian, may help us. We must get to Ver Sinhaza, the vampyre stronghold.")
                .stage("I should go to Ver Sinhaza with the Myreque. Vertida will guide me.")
                .talk("vertida_sefalatis").take("silver_bar").give("vial_of_blood", "ivandis_flail")
                .say("We craft an Ivandis flail from blessed silver to harm Ranis Drakan. *You forge the flail with the silver bar.* Take this vial too.")
                .missing("We need a silver bar for the flail.")
                .stage("Wielding the Ivandis flail, I must defeat Ranis Drakan at Ver Sinhaza.")
                .kill("ranis_drakan").take("vial_of_blood").say("* Ranis Drakan falls! But the Myreque have lost brave members in the battle.")
                .stage("I should report back to Safalaan.")
                .talk("safalaan").say("Ranis is dead... at a terrible price. But Drakan's grip has weakened. Keep the flail; you've earned it.");
    }

    private static void sinsOfTheFather(QuestBook b) {
        b.npc("vanescula_drakan", "Vanescula Drakan", "darkmeyer", 2, 3, 0xE0D8D8, 0x6A1A2A, 0x3A0A1A, 0x1A1A1A);
        b.item("blisterwood_logs", "Blisterwood logs", "log", 0x6A4A6A, "Logs from the Blisterwood tree. Deadly to vampyres.");
        b.boss(BossDef.of("vanstrom_klause", "Vanstrom Klause").level(169).stats(155, 150, 150, 120, 120, 1).speed(4)
                .defenceBonus(70, 70, 70, 50, 70)
                .requiresWeapon("blisterwood_flail")
                .attack(AttackDef.of("vampyric_claw", AttackType.SLASH, 25, 2.5, 6).special(Special.LIFESTEAL, 25))
                .attack(AttackDef.of("blood_pool", AttackType.MAGIC, 15, 8, 3).special(Special.HAZARD, 4)
                        .telegraph(2, "Vanstrom summons a pool of blood beneath you!").particle("minecraft:crimson_spore"))
                .attack(AttackDef.of("vanish", AttackType.MAGIC, 10, 12, 2).special(Special.BLINK, 0).message("Vanstrom vanishes into mist!"))
                .phase(50, "Vanstrom: You cannot defeat a Drakan!", 1.2, false)
                .drops(Drop.always("bones")).size(0.8f, 2.4f, 1.0f).at("darkmeyer").arena(12, "crypt"));
        b.quest("sins_of_the_father", "Sins of the Father", Difficulty.MASTER, 2)
                .reqQuest("a_taste_of_hope", "VAMPYRE_SLAYER")
                .req(Skill.WOODCUTTING, 62).req(Skill.FLETCHING, 60).req(Skill.CRAFTING, 56).req(Skill.AGILITY, 52)
                .req(Skill.ATTACK, 50).req(Skill.SLAYER, 50).req(Skill.MAGIC, 49)
                // OSRS: a tome of experience, 6 x 15,000 XP in skills of the player's choice (level 60+). // approx: fixed skills
                .xp(Skill.WOODCUTTING, 15000).xp(Skill.FLETCHING, 15000).xp(Skill.CRAFTING, 15000)
                .xp(Skill.AGILITY, 15000).xp(Skill.ATTACK, 15000).xp(Skill.SLAYER, 15000)
                .unlock("Access to Darkmeyer").unlock("Access to the Hallowed Sepulchre")
                .stage("I can start this quest by speaking to Veliaf Hurtz in the Myreque hideout. (Requires A Taste of Hope, Vampyre Slayer and more.)")
                .talk("veliaf_hurtz").say("Vanescula Drakan, Lord Drakan's own sister, wants to meet. It could be a trap... but we must hear her out.")
                // OSRS: Kroy's herb lab in Slepe, the trek with Ivan Strom and the Icyene graveyard, then Vanescula.
                .stage("I should help Veliaf deal with the herb dealer in Slepe, then join the trek to the Icyene graveyard and meet Vanescula Drakan.")
                .reach("quest_slepe_pub", 8).stay().unlessFlag("sotf_kroy").flag("sotf_kroy")
                .say("* You follow Carl from the pub to his supplier. In the lab you kill Kroy and destroy both lab tables.")
                .reach("quest_icyene_graveyard", 8).stay().needFlag("sotf_kroy").unlessFlag("sotf_trek").flag("sotf_trek")
                .say("* With Ivan Strom you trek across Morytania and take the boat to the Icyene graveyard. Veliaf is waiting.")
                .talk("vanescula_drakan").needFlag("sotf_trek")
                .missing("Not yet. Deal with the herb dealer in Slepe and come with the Myreque to the Icyene graveyard.").say("I want my brother overthrown. The Blisterwood tree can kill any vampyre. Cut its logs and craft a weapon.",
                        "But Vanstrom Klause hunts you. Be ready.")
                .stage("I should cut blisterwood logs (rune axe) and craft a blisterwood flail.")
                .talk("vanescula_drakan").need("rune_axe").reqSkill(Skill.WOODCUTTING, 62).reqSkill(Skill.FLETCHING, 60).give("blisterwood_logs", "blisterwood_flail")
                .say("* You cut logs from the Blisterwood tree and fletch them into a flail with silver from Vanescula's smiths.")
                .missing("The Blisterwood tree needs a strong axe. Bring a rune axe.")
                .stage("Vanstrom Klause has found me! I must defeat him with the blisterwood flail.")
                .kill("vanstrom_klause").take("blisterwood_logs").say("* Vanstrom Klause falls, slain by blisterwood! Darkmeyer trembles.")
                .stage("I should report to Vanescula Drakan.")
                .talk("vanescula_drakan").say("Vanstrom is dead. My brother will be next. Keep the flail, and enjoy the... hospitality of Darkmeyer.")
                .done("vanescula_drakan", "Lord Drakan's days are numbered.");
    }
}
