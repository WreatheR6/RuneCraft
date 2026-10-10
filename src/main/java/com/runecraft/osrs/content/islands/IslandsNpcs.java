package com.runecraft.osrs.content.islands;

import com.runecraft.osrs.content.NpcDef;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The islanders, with their dialogue. Quest characters that already exist (King Awowogei, Garkor,
 * the Oneiromancer, King Vargas, Advisor Ghrim, Mawnis Burowgar, Gjuki Sorvott, Captain Braindeath,
 * Brother Fintan, the Fairy Queen, Captain Tock...) and the npcs module's bankers and charter crews
 * are not duplicated; this module adds each area's other residents, shopkeepers and guides.
 * Offsets are blocks from the place centre.
 */
public final class IslandsNpcs {
    private IslandsNpcs() {
    }

    private static final Map<String, List<String>> DIALOGUE = new LinkedHashMap<>();

    private static void npc(List<NpcDef> out, String id, String name, String place, int dx, int dz, int skin, int top, int bottom,
                            int hat, String... lines) {
        out.add(NpcDef.of(id, name, place, dx, dz, skin, top, bottom, hat));
        DIALOGUE.put(id, List.of(lines));
    }

    /** Dialogue lines an NPC says when talked to (one per talk, cycling), or an empty list. */
    public static synchronized List<String> dialogue(String npcId) {
        if (DIALOGUE.isEmpty()) {
            all();
        }
        return DIALOGUE.getOrDefault(npcId, List.of());
    }

    private static final int HUMAN = 0xE0B898;
    private static final int TAN = 0xC89870;
    private static final int MONKEY = 0x8A6A44;
    private static final int OGRE = 0x7A8A4A;
    private static final int FAIRY = 0xE8D8F0;
    private static final int GHOST = 0xC8D8D8;
    private static final int SNAKE = 0x6A8A5A;

    public static synchronized List<NpcDef> all() {
        List<NpcDef> out = new ArrayList<>();
        String P;
        // ---- Fossil Island ----
        P = IslandsPlaces.FOSSIL_ISLAND;
        npc(out, "isl_petrified_pete", "Petrified Pete", P, 6, 2, HUMAN, 0x8A7A5A, 0x5A4A3A, 0xA09060,
                "Welcome to the Museum Camp! The Varrock Museum sent us here to dig up the island's fossils.",
                "Bring me unidentified fossils and I'll clean them up. The rare ones go to the House on the Hill.",
                "Numulites are the island's currency - those little shell fossils. The ammonite crabs on the coast drop them.");
        npc(out, "isl_barge_guard", "Barge guard", P, -8, 0, HUMAN, 0x4A5A7A, 0x2A2A3A, 0x6A6A6A,
                "The barge runs between here and the Digsite, east of Varrock.",
                "Mind the wyverns in the caves to the north-west. Bring an elemental, mind or dragonfire shield.");
        P = IslandsPlaces.HOUSE_ON_THE_HILL;
        npc(out, "isl_fossil_curator", "Museum Camp archaeologist", P, 3, 4, HUMAN, 0x6A5A3A, 0x4A3A2A, 0xC0A060,
                "The House on the Hill holds the island's fossil displays. Every skeleton you complete earns Kudos for the museum.",
                "The Magic Mushtree outside links the House on the Hill to Verdant Valley, the Sticky swamp and Mushroom Meadow.");
        P = IslandsPlaces.VOLCANIC_MINE;
        npc(out, "isl_volcanic_mine_overseer", "Volcanic Mine overseer", P, 3, 3, TAN, 0x8A3A1A, 0x3A2A1A, 0xE0A020,
                "The Volcanic Mine needs level 50 Mining. Work as a team: mine the vents, keep them stable, and get out before the eruption.",
                "Watch the vent stability and the falling boulders. If the volcano erupts early, nobody gets paid.",
                "Points from the mine buy ash covered tomes, dragon pickaxe upgrades and more.");
        // ---- Ape Atoll ----
        P = IslandsPlaces.APE_ATOLL;
        npc(out, "isl_ifaba", "Ifaba", P, -14, 6, MONKEY, 0x6A4A2A, 0x5A3A1A, 0x6A4A2A,
                "Ifaba's general store. Bananas, mostly.",
                "Without the amulet of monkey speak you'd hear nothing but 'Ook'.");
        npc(out, "isl_hamab", "Hamab", P, 14, 6, MONKEY, 0x7A5A2A, 0x5A3A1A, 0x7A5A2A,
                "Hamab's crafting emporium. Monkeys need jewellery too.",
                "The monkey guards at the palace eat bananas when they're hurt - be quick.");
        npc(out, "isl_daga", "Daga", P, -14, -6, MONKEY, 0x5A3A1A, 0x3A2A1A, 0x5A3A1A,
                "Daga's scimitar smithy. The finest dragon scimitars on Ape Atoll - if you've earned the right to buy one.",
                "Only those who helped against Glough may wield a dragon scimitar.");
        npc(out, "isl_solihib", "Solihib", P, 6, 10, MONKEY, 0x8A6A3A, 0x6A4A2A, 0x8A6A3A,
                "Fruit! Fresh from the jungle! Bananas, papayas, coconuts!",
                "The temple to the east belongs to Marim's priests. Keep away from the jungle demon's lair beneath it.");
        npc(out, "isl_marim_gate_guard", "Monkey gate guard", P, -48, 0, MONKEY, 0x4A3A2A, 0x3A2A1A, 0x8A8A8A,
                "Halt! Only monkeys may pass the gates of Marim!",
                "Strangers to King Awowogei's city are thrown in the jail.");
        P = IslandsPlaces.APE_ATOLL_AGILITY;
        npc(out, "isl_ape_agility_monkey", "Agility monkey", P, 2, 3, MONKEY, 0x9A7A4A, 0x7A5A3A, 0x9A7A4A,
                "The Ape Atoll course is for level 48 Agility - and you'll need to be a ninja monkey to do it properly.",
                "Stepping stone, tropical tree, monkey bars, skull slope, rope swing, then down the tropical tree. Ook!");
        // ---- Miscellania and Etceteria ----
        P = IslandsPlaces.MISCELLANIA;
        npc(out, "isl_lumberjack_leif", "Lumberjack Leif", P, 84, -36, HUMAN, 0x7A3A2A, 0x3A3A2A, 0xA07040,
                "I chop the maples here for the kingdom. Put me on Woodcutting at the royal ledger and I'll fill your coffers with logs.",
                "The more the islanders like you, the harder we work. Chop a few trees yourself!");
        npc(out, "isl_miner_magnus", "Miner Magnus", P, 94, -8, HUMAN, 0x5A5A5A, 0x3A3A3A, 0x8A6A3A,
                "The mine under the island is full of coal. Allocate workers to Mining and I'll bring it up.",
                "Approval falls a little every day. Work on the islands to keep the people happy.");
        npc(out, "isl_fisherman_frodi", "Fisherman Frodi", P, 100, -22, HUMAN, 0x3A5A7A, 0x2A3A4A, 0x5A7A9A,
                "Tuna and swordfish from the waters around Miscellania - if the royal coffer pays us.",
                "Up to 10% of the coffer is spent each day, 75,000 coins at most once Royal Trouble is done.");
        npc(out, "isl_gardener_gunnhild", "Gardener Gunnhild", P, 48, -38, HUMAN, 0x5A8A3A, 0x3A5A2A, 0xE0C060,
                "I tend the herb and flax fields. Rake the weeds and the islanders will think better of you.",
                "Herbs, flax or hardwood - choose what we work on at the workforce board in the castle.");
        P = IslandsPlaces.ETCETERIA;
        npc(out, "isl_queen_sigrid", "Queen Sigrid", P, 0, -6, HUMAN, 0x8A2A6A, 0x5A1A4A, 0xE0C040,
                "Welcome to Etceteria. Miscellania's royal family and mine have long been rivals.",
                "The spirit tree and the bush patch in my kingdom are tended by my gardeners.");
        npc(out, "isl_etceteria_guard", "Etceteria guard", P, 4, 4, HUMAN, 0x6A6A7A, 0x4A4A5A, 0xA0A0A8,
                "Queen Sigrid's castle. Keep your weapons sheathed.",
                "The ferry from Rellekka docks at Miscellania, west of here.");
        // ---- Lunar Isle ----
        P = IslandsPlaces.LUNAR_ISLE;
        npc(out, "isl_baba_yaga", "Baba Yaga", P, -40, -26, HUMAN, 0x6A3A6A, 0x3A1A3A, 0x5A5A5A,
                "Welcome to my house. Yes, it does have chicken legs. Berty the house is very well behaved.",
                "Lunar magic is the Moonclan's gift. You'll need a seal of passage to speak with us at all.");
        npc(out, "isl_selene", "Selene", P, 12, 4, HUMAN, 0x3A3A8A, 0x2A2A5A, 0xC0C0E0,
                "The Moonclan have lived on Lunar Isle since we fled the Fremennik lands.",
                "Without a seal of passage the Fremennik spirits will not let you understand us.");
        npc(out, "isl_meteora", "Meteora", P, 16, -8, HUMAN, 0x5A3A8A, 0x3A2A5A, 0xE0E0F0,
                "The Astral Altar lies to the south-east, past the suqah hills. Astral runes power the Lunar spellbook.",
                "Beware the suqahs. They are not fond of visitors.");
        npc(out, "isl_rimae_sirsalis", "Rimae Sirsalis", P, 20, 8, HUMAN, 0x4A4A7A, 0x2A2A4A, 0xA0A0C0,
                "The Lunar Isle mine is north-east of the village. Its lunar ore made our staffs and armour.",
                "Pirate Pete and Lokar Searunner sail here from Pirates' Cove.");
        P = IslandsPlaces.PIRATES_COVE;
        npc(out, "isl_lokar_searunner", "Lokar Searunner", P, 4, -4, TAN, 0x6A2A2A, 0x3A2A1A, 0x2A2A2A,
                "I sail between Rellekka, Pirates' Cove and Lunar Isle - if you have business with the Moonclan.",
                "This cove's a safe harbour for honest pirates. Mostly honest.");
        npc(out, "isl_captain_bentley", "Captain Bentley", P, -4, 4, TAN, 0x3A3A6A, 0x2A2A2A, 0x1A1A1A,
                "Lunar Isle's waters are cursed - our ship needs a Lunar-protected wheel to get through.",
                "Fancy a drink? The cove's bar is the only one for leagues.");
        // ---- Fremennik Isles ----
        P = IslandsPlaces.NEITIZNOT;
        npc(out, "isl_thakkrad_sigmundson", "Thakkrad Sigmundson", P, 8, 6, HUMAN, 0x8A6A3A, 0x5A4A2A, 0xA08050,
                "Bring me yak hides and I'll cure them for yak-hide armour - five coins a hide.",
                "The yaks graze in the pen north-east of the longhall.");
        npc(out, "isl_neitiznot_guard", "Neitiznot guard", P, -6, 8, HUMAN, 0x7A5A3A, 0x4A3A2A, 0x8A8A8A,
                "The Burgher rules Neitiznot from the longhall. We trust in tradition, not in kings like Jatizso's.",
                "The rope bridge leads east to Jatizso. Keep your tax money in your pocket over there.");
        P = IslandsPlaces.JATIZSO;
        npc(out, "isl_hring_hring", "Hring Hring", P, 8, 8, HUMAN, 0x5A5A6A, 0x3A3A4A, 0x9A9AA0,
                "Ore and bars! Taxed, of course. King Gjuki taxes everything.",
                "The mine to the north is full of ice trolls since the troll king moved in.");
        npc(out, "isl_flosi_dalksson", "Flosi Dalksson", P, -8, 8, HUMAN, 0x5A7A9A, 0x3A4A5A, 0x7A9AB0,
                "Fresh fish from the waters around Jatizso.",
                "The longhall's beer is the only thing not taxed - for now.");
        npc(out, "isl_raum_urda_stein", "Raum Urda-Stein", P, 8, -8, HUMAN, 0x6A3A2A, 0x3A2A1A, 0x8A8A8A,
                "Weapons for the King's warriors - and anyone else with coin.",
                "Neitiznot's Burgher thinks he's better than our king. He isn't.");
        npc(out, "isl_skuli_myrka", "Skuli Myrka", P, -8, -8, HUMAN, 0x5A5A5A, 0x3A3A3A, 0xA0A0A0,
                "Armour, forged in Jatizso. Pay the tax and it's yours.");
        npc(out, "isl_keepa_kettilon", "Keepa Kettilon", P, 0, 12, HUMAN, 0xF0F0F0, 0x6A6A6A, 0xF0F0F0,
                "I cook for the King's longhall. Bring me fish and I'll show you how it's done.");
        // ---- Ungael and Lithkren ----
        P = IslandsPlaces.UNGAEL;
        npc(out, "isl_torfinn", "Torfinn", P, -44, -6, HUMAN, 0x8A5A3A, 0x4A3A2A, 0xC09050,
                "My longboat sails between Rellekka and Ungael. Vorkath sleeps in the ice to the east.",
                "Only those who finished Dragon Slayer II should go near that beast.");
        P = IslandsPlaces.LITHKREN;
        npc(out, "isl_dallas_jones", "Dallas Jones", P, 4, 4, HUMAN, 0x8A6A3A, 0x5A4A2A, 0xA07040,
                "Lithkren! The dragonkin built a research facility here. Robert the Strong and I found it together.",
                "The Lithkren Vault below is full of adamant and rune dragons. Bring an anti-dragon shield.");
        // ---- Mos Le'Harmless ----
        P = IslandsPlaces.MOS_LE_HARMLESS;
        npc(out, "isl_mike", "Mike", P, 8, 6, TAN, 0x6A2A2A, 0x3A2A1A, 0x2A2A2A,
                "Mike's general store. Don't ask where the stock comes from.",
                "Without a book o' piracy you won't understand a word anyone here says. Arr.");
        npc(out, "isl_charley", "Charley", P, -8, 6, TAN, 0x2A4A6A, 0x3A2A1A, 0x8A2A2A,
                "Hats, eyepatches, fake beards - everything a pirate needs.");
        npc(out, "isl_patchy", "Patchy", P, 6, -6, TAN, 0x5A5A3A, 0x3A3A2A, 0x1A1A1A,
                "I sew things together. Pirate hats onto eyepatches, mostly.",
                "The caves east of town are full of cave horrors. Wear a witchwood icon or they'll scream you to death.");
        P = IslandsPlaces.MOS_LE_HARMLESS_JUNGLE;
        npc(out, "isl_mos_cave_pirate", "Old pirate", P, 4, 4, TAN, 0x5A3A2A, 0x3A2A1A, 0x6A2A2A,
                "The caves east of here are full of cave horrors. Level 58 Slayer, and a witchwood icon round yer neck.",
                "They drop black masks now and then. Worth a fortune to a slayer.");
        // ---- Dragontooth Island ----
        P = IslandsPlaces.DRAGONTOOTH_ISLAND;
        npc(out, "isl_ghost_captain", "Ghost captain", P, -6, 10, GHOST, 0x5A6A6A, 0x3A4A4A, 0x2A3A3A,
                "Wooooo... I sail between Port Phasmatys and Dragontooth Island, for those who wear the Amulet of ghostspeak.",
                "The Ectophial was lost here long ago. Many ghosts wander the shore.");
        // ---- Tutorial Island ----
        P = IslandsPlaces.TUTORIAL_ISLAND;
        npc(out, "isl_gielinor_guide", "Gielinor Guide", P, -8, 6, HUMAN, 0x3A6A3A, 0x2A3A2A, 0x6A4A2A,
                "Greetings! I'm the Gielinor Guide. This island teaches the basics of life in Gielinor.",
                "Talk to every instructor on the island, then the Magic Instructor can send you to the mainland.",
                "Your skills, quests, prayers and spells all work just like in Old School RuneScape.");
        npc(out, "isl_survival_expert", "Survival Expert", P, 12, 30, TAN, 0x4A6A2A, 0x3A4A1A, 0x6A5A2A,
                "Cut down a tree for logs, light them with a tinderbox, then catch some shrimp with a net.",
                "Cook your shrimp on the fire. Burning food is part of learning!");
        npc(out, "isl_master_chef", "Master Chef", P, -44, 50, HUMAN, 0xF0F0F0, 0x6A6A6A, 0xF0F0F0,
                "Mix flour and water to make bread dough, then bake it on the range.",
                "Ranges burn food less often than fires do.");
        npc(out, "isl_quest_guide", "Quest Guide", P, -24, -22, HUMAN, 0x3A3A8A, 0x2A2A5A, 0x6A4A2A,
                "Quests are adventures with stories and rewards. Open your quest journal to see them all.",
                "Quest points unlock guilds, areas and even capes.");
        npc(out, "isl_mining_instructor", "Mining Instructor", P, -36, -2, TAN, 0x5A4A3A, 0x3A2A1A, 0x8A8A8A,
                "Mine tin and copper, then smelt them into a bronze bar at the furnace.",
                "Smith the bar on an anvil with a hammer to make your first dagger.");
        npc(out, "isl_combat_instructor", "Combat Instructor", P, 22, -8, HUMAN, 0x8A8A9A, 0x5A5A6A, 0xA0A0A8,
                "Equip your bronze dagger and sword, then train on the rats in the pit.",
                "Try the shortbow and arrows too - ranged combat keeps you out of harm's way.");
        npc(out, "isl_account_guide", "Account Guide", P, 52, -22, HUMAN, 0x2A2A4A, 0x1A1A2A, 0x3A3A3A,
                "This is a bank. Deposit your items and coins here to keep them safe.",
                "The Grand Exchange in Varrock lets you trade with everyone.");
        npc(out, "isl_brother_brace", "Brother Brace", P, 52, 12, HUMAN, 0x6A4A2A, 0x4A3A1A, 0x3A2A1A,
                "Prayer gives you protection from your foes. Bury bones to train it, and recharge at an altar.",
                "Your prayer points drain while a prayer is active.");
        npc(out, "isl_magic_instructor", "Magic Instructor", P, 84, 48, HUMAN, 0x3A3A9A, 0x2A2A6A, 0x3A3A9A,
                "Runes power spells. Try Wind Strike on the chickens - one air rune and one mind rune.",
                "When you're ready, I can teleport you to Lumbridge on the mainland.");
        // ---- Feldip Hills ----
        P = IslandsPlaces.GU_TANOTH;
        npc(out, "isl_ogre_merchant", "Ogre merchant", P, -10, 8, OGRE, 0x6A4A2A, 0x4A3A1A, 0x6A4A2A,
                "Me sell things. You buy. Good deal for ogre.");
        npc(out, "isl_ogre_trader", "Ogre trader", P, -10, -8, OGRE, 0x5A3A1A, 0x3A2A1A, 0x5A3A1A,
                "Ogre food! Rock cake best. Very crunchy.",
                "City of Gu'Tanoth. Only ogres welcome. Watchtower wizard friend maybe welcome.");
        npc(out, "isl_gu_tanoth_guard", "Ogre guard", P, -26, 0, OGRE, 0x4A3A2A, 0x3A2A1A, 0x8A8A8A,
                "Stop! Need gift for guard. Then maybe guard let you in.");
        P = IslandsPlaces.FELDIP_HUNTER_AREA;
        npc(out, "isl_feldip_hunting_expert", "Hunting expert", P, -10, 6, TAN, 0x5A6A3A, 0x3A4A2A, 0x6A5A3A,
                "Feldip Hills is rich hunting ground: barb-tailed kebbits in the deadfalls, spined larupia and horned graahk in pits.",
                "Lay a pit, dress it with logs, then scare the beast in from the far side.");
        // ---- Castle Wars, Corsair Cove ----
        P = IslandsPlaces.CASTLE_WARS;
        npc(out, "isl_castle_wars_saradomin_guard", "Saradomin guard", P, -40, 4, HUMAN, 0x3A5AB0, 0x2A3A8A, 0xE0E0F0,
                "For Saradomin! Capture Zamorak's standard and bring it back to our castle.",
                "Use the barricades and the underground tunnels. Rocks can collapse them!");
        npc(out, "isl_castle_wars_zamorak_guard", "Zamorak guard", P, -128, -78, HUMAN, 0xB03A3A, 0x6A1A1A, 0x2A2A2A,
                "Zamorak's castle will never fall. Bring Saradomin's standard here, or die trying.");
        // ---- Zul-Andra ----
        P = IslandsPlaces.ZUL_ANDRA;
        npc(out, "isl_priestess_zul_gwenwynig", "Priestess Zul-Gwenwynig", P, -4, 22, SNAKE, 0x3A6A3A, 0x2A4A2A, 0x9AC060,
                "Zul-Andra worships the great serpent Zulrah. Offer yourself at the sacrificial boat - if you dare.",
                "Zulrah changes colour and form. Green spits venom from range, red strikes in melee, blue casts magic.");
        npc(out, "isl_zul_cheray", "Zul-Cheray", P, 4, 26, SNAKE, 0x4A7A4A, 0x2A4A2A, 0x6A8A4A,
                "Zul-Andra teleport scrolls - made from Zulrah's scales. They bring you straight back to the village.");
        npc(out, "isl_zul_areth", "Zul-Areth", P, 0, 34, SNAKE, 0x5A6A3A, 0x3A4A2A, 0x8A9A5A,
                "Fallen to Zulrah? Your belongings wash up here. I'll return them - for a fee.");
        // ---- Tirannwn ----
        P = IslandsPlaces.PORT_TYRAS;
        npc(out, "isl_tyras_sailor", "Tyras sailor", P, 6, -4, HUMAN, 0x8A2A2A, 0x5A1A1A, 0x8A8A8A,
                "Port Tyras supplies King Tyras' camp. The elves of Isafdar ambush every wagon that leaves.",
                "Ships sail from here for Ardougne and the Western Sea.");
        npc(out, "isl_tyras_guard", "Tyras guard", P, -6, 4, HUMAN, 0x8A2A2A, 0x6A6A6A, 0xA0A0A8,
                "Keep moving. King Tyras' orders - no elves, no spies.");
        P = IslandsPlaces.ISAFDAR;
        npc(out, "isl_elf_tracker", "Elf tracker", P, 4, -4, 0xE8E0C8, 0x3A6A3A, 0x2A4A2A, 0x5A8A3A,
                "Isafdar's forest protects us. Watch for leaf traps and the dense undergrowth - only elves pass easily.",
                "Lord Iorwerth's men and the Cadarn clan both claim this land.");
        P = IslandsPlaces.BRAINDEATH_ISLAND;
        npc(out, "isl_pirate_pete", "Pirate Pete", P, -6, 6, TAN, 0x6A2A2A, 0x3A2A1A, 0x1A1A1A,
                "Braindeath Island! Captain Braindeath brews his rum here... with the help of zombie pirates.",
                "I'll row you back to Mos Le'Harmless when you've had enough.");
        // ---- Void Knights' Outpost ----
        P = IslandsPlaces.VOID_KNIGHTS_OUTPOST;
        npc(out, "isl_void_knight", "Void Knight", P, 4, -30, HUMAN, 0x2A2A2A, 0x3A3A3A, 0xE0E0E0,
                "Protect the Void Knight and destroy the portals to save Pest Control.",
                "Commendation points buy void knight equipment, experience and more.");
        // ---- Zanaris ----
        P = IslandsPlaces.ZANARIS;
        npc(out, "isl_jukat", "Jukat", P, 18, 12, 0x6A3A3A, 0x3A1A1A, 0x2A1A1A, 0x1A1A1A,
                "Dragon swords! Dragon daggers! Finest in all the realms.",
                "Come back when you have the coin - they aren't cheap.");
        npc(out, "isl_irksol", "Irksol", P, -14, 12, FAIRY, 0xE04A6A, 0xA02A4A, 0xE0E0E0,
                "Ruby rings, only the finest!");
        npc(out, "isl_fairy_fixit", "Fairy Fixit", P, 6, -10, FAIRY, 0x8A6AB0, 0x5A4A8A, 0xE0E0F0,
                "The fairy ring centre connects to rings all over Gielinor. Turn the dial, then step in!",
                "Rings need the fairies' blessing - start helping the Fairy Queen and the rings will open to you.",
                "CKP leads to the cosmic entity's plane, where the Cosmic Altar floats.");
        npc(out, "isl_fairy_shopkeeper", "Fairy shop keeper", P, -6, -10, FAIRY, 0x6AB08A, 0x4A8A5A, 0xE0E0F0,
                "Welcome to the Zanaris general store.");
        npc(out, "isl_zanaris_fairy", "Fairy", P, 0, 18, FAIRY, 0xC0A0E0, 0x8A6AB0, 0xE0E0F0,
                "Zanaris is the fairies' home. The Lost City, humans called it.",
                "Zygomites grow in the fungus fields to the south. You'll need fungicide to kill them properly.");
        return out;
    }
}
