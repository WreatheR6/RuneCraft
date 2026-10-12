package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.Quest.Difficulty;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.Drop;

import java.util.Map;

/**
 * OSRS miniquests (0 quest points): Architectural Alliance, Barbarian Training, Bear Your Soul, Curse
 * of the Empty Lord, Daddy's Home, Enchanted Key, Family Pest, The Frozen Door, The General's Shadow,
 * His Faithful Servants, Hopespear's Will, In Search of Knowledge, Into the Tombs, Lair of Tarn
 * Razorlor, Mage Arena I and II, Skippy and the Mogres and Vale Totems. (Alfred Grimhand's Barcrawl
 * and Enter the Abyss live in {@link MisthalinMoreQuests}.)
 */
final class Miniquests {
    private Miniquests() {
    }

    static void register(QuestBook b) {
        b.place("quest_mage_arena", "Mage Arena", 3105, 3934, PlaceDef.Style.ARENA);
        b.spot("quest_kourend_castle", "Kourend Castle", 1615, 3675);
        b.spot("quest_soul_altar", "Soul Altar", 1815, 3855);
        b.place("quest_tarns_lair", "Tarn's Lair", 3424, 3240, PlaceDef.Style.CAVE);
        b.spot("quest_auburn_vale", "Auburnvale", 1385, 3330);
        b.spot("quest_arceuus_library", "Arceuus Library", 1632, 3808);
        architecturalAlliance(b);
        barbarianTraining(b);
        bearYourSoul(b);
        curseOfTheEmptyLord(b);
        daddysHome(b);
        enchantedKey(b);
        familyPest(b);
        frozenDoor(b);
        generalsShadow(b);
        hisFaithfulServants(b);
        hopespearsWill(b);
        inSearchOfKnowledge(b);
        intoTheTombs(b);
        lairOfTarnRazorlor(b);
        mageArena(b);
        skippyAndTheMogres(b);
        valeTotems(b);
    }

    private static void architecturalAlliance(QuestBook b) {
        Cast.npc(b, "quest_hosa", "Hosa", "quest_kourend_castle", 0, 2, Cast.GHOST, 0x8A8AA0);
        Cast.npc(b, "quest_archeio", "Archeio", "arceuus", 3, -3, Cast.PALE, 0x5A2A6A);
        b.item("quest_kourend_plans", "Building plans", "note", 0xD8D0B0, "Old plans of Kourend's architecture.");
        b.quest("architectural_alliance", "Architectural Alliance (miniquest)", Difficulty.EXPERIENCED, 0)
                .reqQuest("client_of_kourend").req(Skill.MINING, 42)
                .xp(Skill.MINING, 1000) // approximate
                .unlock("Xeric's talisman teleport to Xeric's Heart")
                .stage("I can start this miniquest by speaking to the ghost Hosa in Kourend Castle. (Requires Client of Kourend, 42 Mining.)")
                .talk("quest_hosa").say("I was Kourend's architect, long ago. My plans were lost when I died. Find them, and I'll show you a secret of Xeric's.")
                .stage("I should ask Archeio in Arceuus whether the library kept Hosa's plans.")
                .talk("quest_archeio").give("quest_kourend_plans").say("Ah, the architect's plans. Dusty, but intact. Take them to him.")
                .stage("I should bring the plans to Hosa.")
                .talk("quest_hosa").take("quest_kourend_plans").say("My plans! Xeric's talisman can take you to the heart of his works now. Use it well.");
    }

    private static void barbarianTraining(QuestBook b) {
        Cast.npc(b, "otto_godblessed", "Otto Godblessed", "otto_grotto", 0, 2, 0xD8A88F, 0x6A3A2A);
        b.items(s -> s.add("bronze_hasta", "Bronze hasta", Kind.MELEE_WEAPON, "spear", 0xB08040).value(1).stack(1)
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(5, 5, 5, 0, 0, 1, 1, 1, 0, 0, 6, 0, 0, 0),
                        Map.of(), 4, AttackType.STAB, EquipmentDef.WeaponClass.MELEE))
                .examine("A barbarian spear that can be wielded with a shield."));
        b.quest("barbarian_training", "Barbarian Training (miniquest)", Difficulty.INTERMEDIATE, 0)
                .req(Skill.FARMING, 15)
                .reqQuest("DRUIDIC_RITUAL", "tai_bwo_wannai_trio")
                .req(Skill.FISHING, 55).req(Skill.AGILITY, 15).req(Skill.STRENGTH, 15).req(Skill.FIREMAKING, 35)
                .req(Skill.CRAFTING, 11).req(Skill.SMITHING, 5).req(Skill.HERBLORE, 4)
                .reward("bronze_hasta")
                .unlock("Barbarian fishing, barbarian firemaking, barbarian herblore mixes and smithing hastae")
                .stage("I can start this miniquest by speaking to Otto Godblessed at his grotto north of the Barbarian Outpost. (Requires Druidic Ritual, Tai Bwo Wannai Trio and various skills.)")
                .talk("otto_godblessed").say("You wish to learn the barbarian ways? Then fish as we fish, burn as we burn, and forge as we forge.")
                .stage("I should complete Otto's three trials: fish with a barbarian rod, light a fire with a bow, and smith a hasta.")
                .talk("otto_godblessed").stay().unlessFlag("bt_fishing").take("raw_trout:3").flag("bt_fishing").grantXp(Skill.FISHING, 5000)
                .say("*You catch leaping trout with your bare hands and a heavy rod.* Good! Now you fish like a barbarian.")
                .missing("Bring me 3 trout caught the barbarian way.")
                .talk("otto_godblessed").stay().unlessFlag("bt_firemaking").take("oak_logs").need("shortbow").flag("bt_firemaking").grantXp(Skill.FIREMAKING, 2500)
                .say("*You spin the bowstring against the log until it smokes.* Fire without a tinderbox! Excellent.")
                .missing("Light an oak log using a shortbow, as a barbarian would.")
                .talk("otto_godblessed").needFlag("bt_fishing", "bt_firemaking").take("bronze_bar", "logs").need("hammer").grantXp(Skill.SMITHING, 1000)
                .say("*You forge a bronze hasta on the barbarian anvil.* You have learned well. The barbarian ways are yours.")
                .missing("Finish the fishing and firemaking trials, then bring a bronze bar and some logs to forge a hasta.");
    }

    private static void bearYourSoul(QuestBook b) {
        Cast.npc(b, "aretha", "Aretha", "quest_soul_altar", 0, 2, Cast.PALE, 0x4A2A5A);
        b.item("damaged_soul_bearer", "Damaged soul bearer", "talisman", 0x5A4A5A, "A damaged soul bearer.");
        b.item("soul_bearer", "Soul bearer", "talisman", 0x8A6A9A, "Transports ensouled heads to your bank.");
        b.quest("bear_your_soul", "Bear Your Soul (miniquest)", Difficulty.NOVICE, 0)
                .reward("soul_bearer")
                .stage("I can start this miniquest by speaking to Aretha at the Soul Altar in Arceuus.")
                .talk("aretha").say("Long ago a soul bearer was lost in the Arceuus crypts. It could carry ensouled heads safely. Find it.")
                .stage("I should search the area by the Dark Altar for the soul bearer.")
                .reach("arceuus_dark_altar", 8).need("spade").give("damaged_soul_bearer").say("* You dig in the grave mounds and unearth a cracked soul bearer.")
                .missing("* I'll need a spade to dig here.")
                .stage("I should take the damaged soul bearer to Aretha.")
                .talk("aretha").take("damaged_soul_bearer").say("Damaged, but I can mend it. *She whispers to it.* It will carry heads to your bank now.");
    }

    private static void curseOfTheEmptyLord(QuestBook b) {
        Cast.npc(b, "quest_valdez", "Valdez", "quest_mage_arena", -6, 4, Cast.GHOST, 0x6A6A8A);
        Cast.npc(b, "quest_rennard", "Rennard", "BRIMHAVEN", 7, -3, Cast.GHOST, 0x3A3A5A);
        b.items(s -> s.add("ring_of_visibility", "Ring of visibility", Kind.SIMPLE, "ring", 0xC0C0E0).value(1).stack(1)
                .examine("A ring that reveals what is hidden."));
        b.items(s -> s.add("ghostly_hood", "Ghostly hood", Kind.ARMOUR, "cowl", 0xD8E8F0).value(1).stack(1)
                .equipment(EquipmentDef.armour(EquipmentDef.Slot.HEAD, new EquipmentStats(0, 0, 0, 3, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0), 1))
                .examine("The hood of the Empty Lord's robes."));
        b.items(s -> s.add("ghostly_robe_top", "Ghostly robe", Kind.ARMOUR, "leather_body", 0xD8E8F0).value(1).stack(1)
                .equipment(EquipmentDef.armour(EquipmentDef.Slot.CHEST, new EquipmentStats(0, 0, 0, 10, 0, 0, 0, 0, 10, 0, 0, 0, 0, 0), 1))
                .examine("The robe top of the Empty Lord."));
        b.items(s -> s.add("ghostly_robe_bottom", "Ghostly robe bottoms", Kind.ARMOUR, "chaps", 0xD8E8F0).value(1).stack(1)
                .equipment(EquipmentDef.armour(EquipmentDef.Slot.LEGS, new EquipmentStats(0, 0, 0, 6, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0), 1))
                .examine("The robe bottoms of the Empty Lord."));
        b.quest("curse_of_the_empty_lord", "Curse of the Empty Lord (miniquest)", Difficulty.EXPERIENCED, 0)
                .reqQuest("desert_treasure_i", "RESTLESS_GHOST").req(Skill.PRAYER, 31)
                .reward("ghostly_hood", "ghostly_robe_top", "ghostly_robe_bottom")
                .stage("I can start this miniquest by speaking to the ghost Valdez in the Wilderness while wearing a ring of visibility and an amulet of ghostspeak. (Requires Desert Treasure I, The Restless Ghost, 31 Prayer.)")
                .talk("quest_valdez").need("ghostspeak_amulet").give("ring_of_visibility")
                .say("I am cursed to wander. The Empty Lord bound us. Find Rennard in Brimhaven; he knows his name. Wear this ring, or you'll never see him.")
                .missing("*The ghost's words are an eerie wail. You need an amulet of ghostspeak.*")
                .stage("I should find the ghost Rennard in Brimhaven.")
                .talk("quest_rennard").need("ring_of_visibility", "ghostspeak_amulet").say("The Empty Lord... his name was Viggora. Speak it at the Wilderness ruins and the curse breaks.")
                .missing("*You feel something watching you, but can't see it.*")
                .stage("I should return to Valdez with the Empty Lord's name.")
                .talk("quest_valdez").need("ghostspeak_amulet").say("Viggora! The name breaks his hold. Take his robes; they are ours to give now.");
    }

    private static void daddysHome(QuestBook b) {
        Cast.npc(b, "marlo", "Marlo", "VARROCK", 8, 6, Cast.FAIR, 0x5A3A6A);
        Cast.npc(b, "old_man_yarlo", "Old Man Yarlo", "VARROCK", -8, -5, Cast.PALE, 0x6A5A3A);
        b.quest("daddys_home", "Daddy's Home (miniquest)", Difficulty.NOVICE, 0)
                .xp(Skill.CONSTRUCTION, 944).reward("plank:25", "steel_nails:50")
                .unlock("Construction contracts with Marlo")
                .stage("I can start this miniquest by speaking to Marlo in north-east Varrock.")
                .talk("marlo").say("My dad's house is falling apart and he won't let me fix it. Maybe he'll let you. Here's what you'll need: planks, nails, a hammer.")
                .stage("I should repair Old Man Yarlo's house (10 planks, 20 steel nails).")
                .talk("old_man_yarlo").take("plank:10", "steel_nails:20").need("hammer").say("*You mend the chairs, table and bed.* Hmph. Not bad. Not bad at all.")
                .missing("You'll need 10 planks and 20 steel nails to fix this place, and a hammer.")
                .stage("I should tell Marlo the house is repaired.")
                .talk("marlo").say("He actually said thank you? Here, take these supplies. Come back if you want more building work.");
    }

    private static void enchantedKey(QuestBook b) {
        b.quest("enchanted_key", "Enchanted Key (miniquest)", Difficulty.INTERMEDIATE, 0)
                .reqQuest("making_history")
                .reward("law_rune:20", "nature_rune:20", "rune_arrow:20", "ring_of_forging") // approximate treasure
                .stage("I can start this miniquest by speaking to Jorral at the outpost with the enchanted key from Making History. (Requires Making History.)")
                .talk("jorral").need("enchanted_key").say("That key grows warm near buried treasure. The old hoards are spread across Gielinor. Dig where it burns hottest!")
                .missing("You'll need the enchanted key I gave you.")
                .stage("I should follow the enchanted key to buried treasure and dig it up.")
                .reach("RELLEKKA", 12).stay().unlessFlag("ek_rellekka").need("enchanted_key", "spade").flag("ek_rellekka").say("* The key is hot! You dig up a small chest.")
                .reach("YANILLE", 12).stay().unlessFlag("ek_yanille").need("enchanted_key", "spade").flag("ek_yanille").say("* The key is hot! Another chest.")
                .reach("LLETYA", 12).stay().unlessFlag("ek_lletya").need("enchanted_key", "spade").flag("ek_lletya").say("* The key is hot! The last chest is buried here.")
                .talk("jorral").needFlag("ek_rellekka", "ek_yanille", "ek_lletya").say("All the hoards found! Keep what you dug up, adventurer.")
                .missing("The key still has treasure to find: try Rellekka, Yanille and Lletya, with a spade.");
    }

    private static void familyPest(QuestBook b) {
        b.quest("family_pest", "Family Pest (miniquest)", Difficulty.NOVICE, 0)
                .reqQuest("family_crest")
                .unlock("Steel gauntlets can be re-enchanted by any of the three brothers")
                .stage("I can start this miniquest by speaking to Dimintheis in Varrock. (Requires Family Crest.)")
                .talk("dimintheis").say("My sons are bickering again. Each thinks his gauntlet enchantment is the best. Talk sense into them.")
                .stage("I should speak to Caleb, Avan and Johnathon.")
                .talk("caleb").stay().unlessFlag("fp_caleb").flag("fp_caleb").say("Fine, fine, I'll share my secret with my brothers. If they share theirs.")
                .talk("avan").stay().unlessFlag("fp_avan").flag("fp_avan").say("Very well. Goldsmithing for all.")
                .talk("johnathon").stay().unlessFlag("fp_johnathon").flag("fp_johnathon").say("Chaos gauntlets for everyone, then. *Hic.*")
                .talk("dimintheis").needFlag("fp_caleb", "fp_avan", "fp_johnathon").take("coins:500000")
                .say("They agree! For the trouble, 500,000 coins to the family, and any of my sons will now change your gauntlets' enchantment.")
                .missing("Talk to all three of my sons, and bring 500,000 coins for the family coffers.");
    }

    private static void frozenDoor(QuestBook b) {
        Cast.npc(b, "quest_ashuelot_reis", "Ashuelot Reis", "gwd_ancient_prison", 3, -3, Cast.PALE, 0x5A2A6A);
        b.quest("the_frozen_door", "The Frozen Door (miniquest)", Difficulty.MASTER, 0)
                .req(Skill.AGILITY, 70).req(Skill.STRENGTH, 70).req(Skill.HITPOINTS, 70).req(Skill.RANGED, 70)
                .reward("frozen_key")
                .unlock("Access to Nex's chamber in the Ancient Prison")
                .stage("I can start this miniquest by speaking to Ashuelot Reis before the frozen door in the God Wars Dungeon. (Requires 70 Agility, 70 Strength, 70 Hitpoints, 70 Ranged.)")
                .talk("quest_ashuelot_reis").say("Beyond this door lies Nex, the Zarosian general. The key was broken into four pieces, one taken into each god's stronghold.")
                .stage("I must recover the four frozen key pieces from the strongholds' generals.")
                .drop("kreearra", "frozen_key_piece_armadyl", 1).drop("general_graardor", "frozen_key_piece_bandos", 1)
                .drop("commander_zilyana", "frozen_key_piece_saradomin", 1).drop("kril_tsutsaroth", "frozen_key_piece_zamorak", 1)
                .talk("quest_ashuelot_reis").take("frozen_key_piece_armadyl", "frozen_key_piece_bandos", "frozen_key_piece_saradomin", "frozen_key_piece_zamorak")
                .say("*The four pieces fuse into one icy key.* The frozen door will open for you now. Zaros be with you.")
                .missing("The door needs all four pieces: Kree'arra, General Graardor, Commander Zilyana and K'ril Tsutsaroth each guard one.");
    }

    private static void generalsShadow(QuestBook b) {
        Cast.npc(b, "quest_gnome_scout", "Scout", "GNOME_STRONGHOLD", -6, 6, 0xE0B080, 0x3A6A2A);
        Cast.npc(b, "sin_seer", "Sin Seer", "DRAYNOR_VILLAGE", 2, 7, Cast.PALE, 0x6A2A6A);
        b.items(s -> s.add("shadow_sword", "Shadow sword", Kind.MELEE_WEAPON, "longsword", 0x2A2A3A).value(1).stack(1)
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(38, 32, -2, 0, 0, 0, 0, 0, 0, 0, 34, 0, 0, 0),
                        Map.of(Skill.ATTACK, 32), 5, AttackType.STAB, EquipmentDef.WeaponClass.MELEE))
                .examine("The sword of General Khazard's shadow. Hits may drain the target's stats."));
        b.quest("the_generals_shadow", "The General's Shadow (miniquest)", Difficulty.EXPERIENCED, 0)
                .xp(Skill.SLAYER, 2000) // OSRS (current quest XP rewards)
                .reqQuest("curse_of_the_empty_lord", "fight_arena")
                .reward("shadow_sword")
                .stage("I can start this miniquest by speaking to a gnome Scout in the Gnome Stronghold while wearing a ring of visibility. (Requires Curse of the Empty Lord, Fight Arena.)")
                .talk("quest_gnome_scout").need("ring_of_visibility").say("Shh! I'm scouting for General Khazard's shadow. Ask the Sin Seer in Draynor about him.")
                .missing("*You hear a voice but see nobody.*")
                .stage("I should visit the Sin Seer in Draynor Village.")
                .talk("sin_seer").say("Khazard's sins cling to him even in death. His beast Bouncer lingers as a ghost. Lay Bouncer to rest and the general's shadow will come.")
                .stage("I must defeat the ghost of Bouncer.")
                .kill("bouncer").need("ghostspeak_amulet").say("* Bouncer's spirit fades. General Khazard's shadow appears and, defeated, drops his sword.")
                .missing("* The ghost is too faint to strike without an amulet of ghostspeak.")
                .stage("I should report to the Scout.")
                .talk("quest_gnome_scout").say("The general's shadow is gone! Keep the shadow sword; you've earned it.");
    }

    private static void hisFaithfulServants(QuestBook b) {
        Cast.npc(b, "quest_strange_old_man_barrows", "Strange Old Man", "BARROWS", 5, -5, Cast.PALE, 0x5A5A3A);
        b.quest("his_faithful_servants", "His Faithful Servants (miniquest)", Difficulty.EXPERIENCED, 0)
                .xp(Skill.PRAYER, 20000) // OSRS (current quest XP rewards)
                .reqQuest("priest_in_peril")
                .unlock("Barrows crypt tunnels can be cleared more quickly")
                .stage("I can start this miniquest by speaking to the Strange Old Man at the Barrows. (Requires Priest in Peril.)")
                .talk("quest_strange_old_man_barrows").say("The brothers serve their master even in death. Defeat all six, and perhaps they'll tell you who that master was.")
                .stage("I must defeat all six Barrows brothers.")
                .kill("ahrim").stay().unlessFlag("hfs_ahrim").flag("hfs_ahrim")
                .kill("dharok").stay().unlessFlag("hfs_dharok").flag("hfs_dharok")
                .kill("guthan").stay().unlessFlag("hfs_guthan").flag("hfs_guthan")
                .kill("karil").stay().unlessFlag("hfs_karil").flag("hfs_karil")
                .kill("torag").stay().unlessFlag("hfs_torag").flag("hfs_torag")
                .kill("verac").stay().unlessFlag("hfs_verac").flag("hfs_verac")
                .talk("quest_strange_old_man_barrows").needFlag("hfs_ahrim", "hfs_dharok", "hfs_guthan", "hfs_karil", "hfs_torag", "hfs_verac")
                .say("All six! Their master was a Zarosian; the vault in the desert remembers. Off you go.")
                .missing("The brothers still stand. All six must fall.");
    }

    private static void hopespearsWill(QuestBook b) {
        Cast.npc(b, "quest_hopespear_ghost", "Hopespear", "goblin_village", -3, 3, Cast.GHOST, 0x6A8A5A);
        b.quest("hopespears_will", "Hopespear's Will (miniquest)", Difficulty.EXPERIENCED, 0)
                .reqQuest("desert_treasure_i", "fairytale_ii_cure_a_queen")
                .xp(Skill.PRAYER, 38750) // OSRS (current quest XP rewards)
                .reqQuest("land_of_the_goblins", "RESTLESS_GHOST").req(Skill.PRAYER, 50)
                .stage("I can start this miniquest by speaking to Hopespear's ghost in Goblin Village with an amulet of ghostspeak. (Requires Land of the Goblins, Desert Treasure I, Fairytale II, The Restless Ghost, 50 Prayer.)")
                .talk("quest_hopespear_ghost").need("ghostspeak_amulet").say("Hopespear sees goblins forget the old ways. Take Hopespear's bones to the goblin temple so Hopespear can rest.")
                .missing("*The ghost gibbers at you. You need an amulet of ghostspeak.*")
                .stage("I should lay bones to rest at the ancient goblin temple.")
                .reach("quest_ancient_goblin_temple", 6).take("bones:3").say("* You bury the bones before the goblin shrine. A green glow passes over you.")
                .missing("* Hopespear's resting place needs bones to bury.")
                .stage("I should tell Hopespear's ghost.")
                .talk("quest_hopespear_ghost").need("ghostspeak_amulet").say("Hopespear rests now. Hopespear gives you wisdom of the priests.");
    }

    private static void inSearchOfKnowledge(QuestBook b) {
        Cast.npc(b, "quest_professor_gracklebone", "Professor Gracklebone", "quest_arceuus_library", 0, 2, Cast.PALE, 0x3A2A5A);
        b.item("quest_lost_tome", "Lost tome", "book", 0x5A3A2A, "A tome missing from the Arceuus Library.");
        b.item("book_of_knowledge", "Book of knowledge", "book", 0xE0C060, "Reading this book grants experience in a skill of your choice.");
        b.quest("in_search_of_knowledge", "In Search of Knowledge (miniquest)", Difficulty.NOVICE, 0)
                .reward("book_of_knowledge")
                .stage("I can start this miniquest by speaking to Professor Gracklebone in the Arceuus Library.")
                .talk("quest_professor_gracklebone").say("A tome has gone missing from the library. My students say a sea troll took it to the Piscatoris shore! Absurd, I know.")
                .stage("I should search the shore near Piscatoris for the lost tome.")
                .reach("piscatoris", 10).give("quest_lost_tome").say("* Among the rocks you find a damp but legible book.")
                .stage("I should return the tome to Professor Gracklebone.")
                .talk("quest_professor_gracklebone").take("quest_lost_tome").say("Remarkable! The library thanks you. Here, a book of knowledge for your trouble.");
    }

    private static void intoTheTombs(QuestBook b) {
        b.quest("into_the_tombs", "Into the Tombs (miniquest)", Difficulty.MASTER, 0)
                .reqQuest("beneath_cursed_sands")
                .unlock("The Tombs of Amascut's full story")
                .stage("I can start this miniquest by speaking to the Priest of Tumeken at the Necropolis. (Requires Beneath Cursed Sands.)")
                .talk("tumeken_priest").say("Amascut's followers gather in the tombs beneath Jaldraocht. Go and see what they plan.")
                .stage("I should enter the Tombs of Amascut.")
                .reach("toa_jaldraocht_entrance", 10).say("* In the tombs you hear Amascut's voice, promising the destruction of Tumeken's line.")
                .stage("I should report to the Priest of Tumeken.")
                .talk("tumeken_priest").say("So Amascut's plan runs deeper than we feared. The tombs must be raided, again and again.");
    }

    private static void lairOfTarnRazorlor(QuestBook b) {
        b.item("tarns_diary", "Tarn's diary", "book", 0x3A2A1A, "Tarn Razorlor's diary. It describes enchanting a salve amulet.");
        b.boss(BossDef.of("quest_tarn_razorlor", "Mutant Tarn").level(69).stats(110, 60, 60, 70, 1, 1).speed(4)
                .attack(AttackDef.of("claw", AttackType.SLASH, 12, 2.5, 6))
                .attack(AttackDef.of("drain", AttackType.MAGIC, 10, 8, 2).special(Special.DRAIN_PRAYER, 3))
                .drops(Drop.always("bones"), Drop.always("tarns_diary")).skin(0x6A7A5A, 0x3A2A3A, 0x2A1A2A, 0x8A1A1A)
                .at("quest_tarns_lair").arena(9, "crypt"));
        b.quest("lair_of_tarn_razorlor", "Lair of Tarn Razorlor (miniquest)", Difficulty.EXPERIENCED, 0)
                .reqQuest("haunted_mine").req(Skill.SLAYER, 40)
                .xp(Skill.SLAYER, 5000)
                .unlock("Enchanting the salve amulet; Tarn's Lair")
                .stage("I can start this miniquest by speaking to the zealot near the Haunted Mine about Tarn Razorlor. (Requires Haunted Mine, 40 Slayer.)")
                .talk("zealot").say("Tarn Razorlor, the necromancer, hides in a lair in the Mort'ton marshes. He makes creatures of the mine's dead.")
                .stage("I should find Tarn's Lair and get past its traps.")
                .reach("quest_tarns_lair", 6).say("* Through trapped corridors and pits you reach Tarn's laboratory. He drinks a potion and transforms!")
                .stage("I must defeat Tarn Razorlor.")
                .kill("quest_tarn_razorlor").say("* Tarn is slain. Among his belongings is his diary.");
    }

    private static BossDef godAvatar(String id, String name, AttackType style, int skin, int top, String particle) {
        return BossDef.of(id, name).level(id.equals("quest_justiciar_zachariah") ? 348 : 235).stats(320, 200, 200, 200, 200, 200).speed(4)
                .defenceBonus(100, 100, 100, 100, 100)
                .attack(AttackDef.of("strike", style, 28, style == AttackType.MAGIC ? 9 : 3, 6).particle(particle))
                .attack(AttackDef.of("smite", AttackType.MAGIC, 22, 9, 3).special(Special.SKY_STRIKE, 0).telegraph(2, name + " calls down divine wrath!"))
                .drops(Drop.always("big_bones")).skin(skin, top, top, 0xE0E0E0).size(1.0f, 2.4f, 1.1f).wilderness()
                .at("quest_mage_arena").arena(10, "void");
    }

    private static void mageArena(QuestBook b) {
        Cast.npc(b, "kolodion", "Kolodion", "quest_mage_arena", 0, 4, Cast.PALE, 0x8A2A8A);
        Cast.npc(b, "quest_statue_saradomin", "Statue of Saradomin", "quest_mage_arena", -4, -4, 0xE0E0F0, 0x2A4AC0);
        Cast.npc(b, "quest_statue_guthix", "Statue of Guthix", "quest_mage_arena", 0, -5, 0xE0E0F1, 0x2A8A2A);
        Cast.npc(b, "quest_statue_zamorak", "Statue of Zamorak", "quest_mage_arena", 4, -4, 0xE0E0F2, 0xA02A2A);
        b.boss(BossDef.of("quest_kolodion", "Kolodion").level(112).stats(120, 1, 1, 60, 120, 1).speed(4)
                .attack(AttackDef.of("god_spell", AttackType.MAGIC, 20, 9, 6).particle("minecraft:enchant"))
                .attack(AttackDef.of("shapeshift", AttackType.MAGIC, 16, 9, 2).special(Special.BLINK, 0))
                .drops(Drop.always("bones")).skin(0xE8C8B0, 0x8A2A8A, 0x5A1A5A, 0xE0E0E0).wilderness()
                .at("quest_mage_arena").arena(10, "void"));
        b.boss(godAvatar("quest_justiciar_zachariah", "Justiciar Zachariah", AttackType.MAGIC, 0xE8C8B0, 0xE0E0F0, "minecraft:end_rod"));
        b.boss(godAvatar("quest_derwen", "Derwen", AttackType.CRUSH, 0x6A8A4A, 0x3A6A2A, "minecraft:happy_villager"));
        b.boss(godAvatar("quest_porazdir", "Porazdir", AttackType.SLASH, 0x8A2A2A, 0x3A0A0A, "minecraft:flame"));
        b.items(s -> s.add("imbued_saradomin_cape", "Imbued saradomin cape", Kind.SIMPLE, "cape", 0x2A4AC0).value(1).stack(1)
                .examine("A saradomin cape, imbued with the power of the Mage Arena."));
        b.items(s -> s.add("imbued_guthix_cape", "Imbued guthix cape", Kind.SIMPLE, "cape", 0x2A8A2A).value(1).stack(1)
                .examine("A guthix cape, imbued with the power of the Mage Arena."));
        b.items(s -> s.add("imbued_zamorak_cape", "Imbued zamorak cape", Kind.SIMPLE, "cape", 0xA02A2A).value(1).stack(1)
                .examine("A zamorak cape, imbued with the power of the Mage Arena."));
        EquipmentStats imbued = new EquipmentStats(0, 0, 0, 15, 0, 3, 3, 3, 15, 3, 0, 0, 2, 0);
        b.gear("imbued_saradomin_cape", QuestBook.GearSlot.CAPE, imbued, "mage_arena_ii");
        b.gear("imbued_guthix_cape", QuestBook.GearSlot.CAPE, imbued, "mage_arena_ii");
        b.gear("imbued_zamorak_cape", QuestBook.GearSlot.CAPE, imbued, "mage_arena_ii");
        b.item("quest_symbol_piece_saradomin", "Saradomin's symbol piece", "talisman", 0x2A4AC1, "A piece of a symbol, taken from Justiciar Zachariah.");
        b.item("quest_symbol_piece_guthix", "Guthix's symbol piece", "talisman", 0x2A8A2B, "A piece of a symbol, taken from Derwen.");
        b.item("quest_symbol_piece_zamorak", "Zamorak's symbol piece", "talisman", 0xA02A2B, "A piece of a symbol, taken from Porazdir.");

        b.quest("mage_arena_i", "Mage Arena I (miniquest)", Difficulty.INTERMEDIATE, 0)
                .req(Skill.MAGIC, 60)
                .unlock("God spells: Saradomin Strike, Claws of Guthix or Flames of Zamorak")
                .stage("I can start this miniquest by speaking to Kolodion in the Mage Arena, deep in the Wilderness. (Requires 60 Magic.)")
                .talk("kolodion").say("You wish to learn the god spells? First you must defeat me. And I don't fight fair: I change shape as I please.")
                .stage("I must defeat Kolodion in all of his forms, using only magic.")
                .kill("quest_kolodion").say("* Kolodion reverts to his human form, laughing. 'Well fought! Now choose your god.'")
                .stage("I should pray at the statue of the god whose power I want: Saradomin, Guthix or Zamorak.")
                .talk("quest_statue_saradomin").give("saradomin_staff", "saradomin_cape").say("* A voice like a bell rings out. You receive a Saradomin staff and cape.")
                .talk("quest_statue_guthix").give("guthix_staff", "guthix_cape").say("* The ground hums with balance. You receive a Guthix staff and cape.")
                .talk("quest_statue_zamorak").give("zamorak_staff", "zamorak_cape").say("* Fire crackles around you. You receive a Zamorak staff and cape.");

        b.quest("mage_arena_ii", "Mage Arena II (miniquest)", Difficulty.MASTER, 0)
                .reqQuest("mage_arena_i").req(Skill.MAGIC, 75)
                .unlock("Imbued god capes")
                .stage("I can start this miniquest by speaking to Kolodion in the Mage Arena. (Requires Mage Arena I, 75 Magic.)")
                .talk("kolodion").say("The gods' followers in the Wilderness hold the pieces of an old symbol. Bring me all three, and I'll imbue your cape.")
                .stage("I must defeat Justiciar Zachariah, Derwen and Porazdir in the Wilderness and recover their symbol pieces.")
                .kill("quest_justiciar_zachariah").stay().unless("quest_symbol_piece_saradomin").give("quest_symbol_piece_saradomin")
                .say("* Justiciar Zachariah falls, dropping a piece of a symbol.")
                .kill("quest_derwen").stay().unless("quest_symbol_piece_guthix").give("quest_symbol_piece_guthix")
                .say("* Derwen falls, dropping a piece of a symbol.")
                .kill("quest_porazdir").stay().unless("quest_symbol_piece_zamorak").give("quest_symbol_piece_zamorak")
                .say("* Porazdir falls, dropping a piece of a symbol.")
                .talk("kolodion").need("quest_symbol_piece_saradomin", "quest_symbol_piece_guthix", "quest_symbol_piece_zamorak")
                .say("All three pieces! Now bring me your god cape and I'll imbue it.")
                .missing("I need all three symbol pieces: from Justiciar Zachariah, Derwen and Porazdir.")
                .stage("I should give Kolodion my god cape to imbue.")
                .talk("kolodion").take("saradomin_cape", "quest_symbol_piece_saradomin", "quest_symbol_piece_guthix", "quest_symbol_piece_zamorak")
                .give("imbued_saradomin_cape").say("*Kolodion chants.* Your cape is imbued with Saradomin's power.")
                .talk("kolodion").take("guthix_cape", "quest_symbol_piece_saradomin", "quest_symbol_piece_guthix", "quest_symbol_piece_zamorak")
                .give("imbued_guthix_cape").say("*Kolodion chants.* Your cape is imbued with Guthix's power.")
                .talk("kolodion").take("zamorak_cape", "quest_symbol_piece_saradomin", "quest_symbol_piece_guthix", "quest_symbol_piece_zamorak")
                .give("imbued_zamorak_cape").say("*Kolodion chants.* Your cape is imbued with Zamorak's power.")
                .missing("Bring me your god cape: Saradomin, Guthix or Zamorak.");
    }

    private static void skippyAndTheMogres(QuestBook b) {
        Cast.npc(b, "skippy", "Skippy", "mudskipper_point", 3, 3, Cast.FAIR, 0x6A5A3A);
        b.quest("skippy_and_the_mogres", "Skippy and the Mogres (miniquest)", Difficulty.NOVICE, 0)
                .req(Skill.COOKING, 20)
                .unlock("Mogres can be lured out of the water with fishing explosives")
                .stage("I can start this miniquest by speaking to Skippy at Mudskipper Point. (Requires 20 Cooking.)")
                .talk("skippy").say("*Skippy is drunk.* Mogresh... in the water... *hic*. Throw some water on me. A jug'll do.")
                .stage("I should sober Skippy up with a jug of water.")
                .talk("skippy").take("jug_of_water").say("Brrr! Thanks. Now my head hurts. Make me a hangover cure: chocolate dust and snape grass, mixed in... something.")
                .missing("*Skippy sways.* Water... jug of water...")
                .stage("I should make Skippy a hangover cure (chocolate dust and snape grass).")
                .talk("skippy").take("chocolate_dust", "snape_grass").say("*He gulps it down.* Ahh. Right: the mogres. Ugly fish-ogres off the point. Throw fishing explosives in and they'll come up fighting.")
                .missing("Chocolate dust and snape grass. Please.")
                .stage("I should lure out a mogre and defeat it.")
                .kill("mogre").say("* The mogre falls. Skippy cheers from the shore.");
    }

    private static void valeTotems(QuestBook b) {
        Cast.npc(b, "quest_totem_carver", "Totem carver", "quest_auburn_vale", 0, 2, Cast.BROWN, 0x8A6A2A);
        b.quest("vale_totems", "Vale Totems (miniquest)", Difficulty.NOVICE, 0)
                .reqQuest("children_of_the_sun").req(Skill.FLETCHING, 20)
                .xp(Skill.FLETCHING, 1000) // approximate
                .unlock("Carving and decorating totems in Auburnvale")
                .stage("I can start this miniquest by speaking to the Totem carver in Auburnvale. (Requires Children of the Sun, 20 Fletching.)")
                .talk("quest_totem_carver").say("The vale's totems have rotted. Help me carve new ones and the spirits of the vale will be pleased.")
                .stage("I should carve a totem from 3 oak logs with a knife.")
                .talk("quest_totem_carver").take("oak_logs:3").need("knife").say("*You carve the totem's faces.* Beautiful work! The spirits are content.")
                .missing("Three oak logs and a knife, friend.");
    }
}
