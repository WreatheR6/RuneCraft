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
import com.runecraft.osrs.content.NpcDef;
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
        // Archeio works in the Arceuus Library (wiki); moved there from the town square (QuestNpcMoves)
        Cast.npcAt(b, "quest_archeio", "Archeio", 1624, 3808, Cast.PALE, 0x5A2A6A);
        b.item("quest_kourend_plans", "Building plans", "note", 0xD8D0B0, "Old plans of Kourend's architecture.");
        // The five houses' architects (wiki map pins); Hosa (Hosidius) waits by the statue of King Rada I.
        b.spot("aa_arcis", "Arcis's house", 1652, 3754);
        b.spot("aa_lovada", "Lovada's house", 1487, 3833);
        b.spot("aa_piliar", "Piliar's house", 1794, 3738);
        b.spot("aa_shayda", "Shayda's house", 1532, 3545);
        b.npc("quest_arcis", "Arcis", "aa_arcis", 0, 0, 0xE8C8B0, 0x4A2A6A, 0x2A1A4A, 0x9A7AC0);
        b.npc("quest_lovada", "Lovada", "aa_lovada", 0, 0, 0xC8946A, 0x8A5A2A, 0x5A3A1A, 0xD0A040);
        b.npc("quest_piliar", "Piliar", "aa_piliar", 0, 0, 0xE0B89A, 0x2A4A6A, 0x1A2A4A, 0x60A0C0);
        b.npc("quest_shayda", "Shayda", "aa_shayda", 0, 0, 0xA86A45, 0x7A1A1A, 0x4A0A0A, 0xC0C0C0);
        b.quest("architectural_alliance", "Architectural Alliance (miniquest)", Difficulty.EXPERIENCED, 0)
                .req(Skill.MINING, 42)
                .reqNote("100% favour with each of Kourend's five houses before its architect will help (favour isn't modelled)")
                // OSRS: a 10,000 XP antique lamp (any skill above 40) from Hosa. // approx
                .xp(Skill.MINING, 1000)
                .unlock("Xeric's talisman teleport to Xeric's Heart")
                .stage("I can start this miniquest by speaking to the ghost Hosa by the statue of King Rada I outside Kourend Castle. (Requires 42 Mining.)")
                .talk("quest_hosa").say("A storm wrecked King Rada's statue, and I can't rebuild it alone. I need the architects of all five houses.",
                        "Win over Arcis of Arceuus, Lovada of Lovakengj, Piliar of Port Piscarilius and Shayda of Shayzien. I speak for Hosidius.")
                .stage("I should win the support of the architects: Arcis in Arceuus, Lovada in Lovakengj, Piliar in Port Piscarilius and Shayda in Shayzien.")
                .talk("quest_arcis").stay().unlessFlag("aa_arcis").flag("aa_arcis").say("Arceuus will lend its architect. I'll meet you at the statue.")
                .talk("quest_lovada").stay().unlessFlag("aa_lovada").flag("aa_lovada").reqSkill(Skill.MINING, 42)
                .say("You've worked our mines, so Lovakengj will help. I'll bring the stone.")
                .talk("quest_piliar").stay().unlessFlag("aa_piliar").flag("aa_piliar").say("Piscarilius never forgets a friend. I'll be there.")
                .talk("quest_shayda").stay().unlessFlag("aa_shayda").flag("aa_shayda").say("Shayzien honours its allies. Count me in.")
                .talk("quest_hosa").needFlag("aa_arcis", "aa_lovada", "aa_piliar", "aa_shayda")
                .say("All five houses together! Meet us at the statue and we'll begin.")
                .missing("Not every architect has agreed yet: Arcis, Lovada, Piliar and Shayda must all come.")
                .hint("quest_archeio", "Arcis is the Arceuus architect. You'll find his house in the north of the city.")
                .stage("I should speak to an architect at the statue of King Rada I.")
                .reach("quest_kourend_castle", 8).say("* The five architects gather at the statue of King Rada I and rebuild it.",
                        "@Hosa: Splendid! As thanks, Xeric's talisman can now take you to Xeric's Heart.");
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
                .req(Skill.FISHING, 55).req(Skill.AGILITY, 15).req(Skill.STRENGTH, 35).req(Skill.FIREMAKING, 35)
                .req(Skill.CRAFTING, 11).req(Skill.SMITHING, 5).req(Skill.HERBLORE, 4)
                .reward("bronze_hasta")
                .unlock("Barbarian fishing, barbarian firemaking, barbarian herblore mixes and smithing hastae")
                .stage("I can start this miniquest by speaking to Otto Godblessed at his grotto north of the Barbarian Outpost. (Requires Druidic Ritual, Tai Bwo Wannai Trio and various skills.)")
                .talk("otto_godblessed").say("You wish to learn the barbarian ways? Then fish as we fish, burn as we burn, and forge as we forge.")
                .stage("I should complete Otto's three trials: fish with a barbarian rod, light a fire with a bow, and smith a hasta.")
                .talk("otto_godblessed").stay().unlessFlag("bt_fishing").take("raw_trout:3").flag("bt_fishing")
                .say("*You catch leaping trout with your bare hands and a heavy rod.* Good! Now you fish like a barbarian.")
                .missing("Bring me 3 trout caught the barbarian way.")
                .talk("otto_godblessed").stay().unlessFlag("bt_firemaking").take("oak_logs").need("shortbow").flag("bt_firemaking")
                .say("*You spin the bowstring against the log until it smokes.* Fire without a tinderbox! Excellent.")
                .missing("Light an oak log using a shortbow, as a barbarian would.")
                .talk("otto_godblessed").needFlag("bt_fishing", "bt_firemaking").take("bronze_bar", "logs").need("hammer") // OSRS: no fixed XP rewards, only XP from the activities
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
        // NPCs spawn when their place is first built, and glarials_tomb is already built in saved worlds: Valdez gets his own spot
        // on the same chunk (the old Valdez at quest_mage_arena keeps the id, so he still works where he stands).
        b.spot("cel_glarials_tomb", "Glarial's tomb (Valdez)", 2557, 3444);
        Cast.npc(b, "quest_valdez", "Valdez", "cel_glarials_tomb", 2, 3, Cast.GHOST, 0x6A6A8A); // wiki 2558, 3446
        // The other five ghosts, at their places in the wiki's first sequence. // approx: OSRS picks one of three routes
        b.spot("cel_shipwreck", "Wilderness shipwreck", 3022, 3946);
        b.spot("cel_chaos_temple", "Chaos Temple (Wilderness)", 2950, 3820);
        b.spot("cel_entrana_church", "Entrana church", 2850, 3348);
        b.spot("cel_wizards_tower", "Wizards' Tower ground floor", 3109, 3160);
        b.spot("cel_rogues_castle", "Rogues' Castle", 3294, 3934);
        Cast.npc(b, "quest_rennard", "Rennard", "cel_shipwreck", 0, 0, Cast.GHOST, 0x3A3A5A);
        b.npc("quest_kharrim", "Kharrim", "cel_chaos_temple", 0, 0, 0xD8E8F0, 0x5A3A2A, 0x3A2A1A, 0x8A8AB0);
        b.npc("quest_lennissa", "Lennissa", "cel_entrana_church", 0, 0, 0xD8E8F0, 0x6A2A4A, 0x4A1A2A, 0xB0A0C8);
        b.npc("quest_dhalak", "Dhalak", "cel_wizards_tower", 0, 0, 0xD8E8F0, 0x2A2A6A, 0x1A1A4A, 0xA0B0D8);
        b.npc("quest_viggora", "Viggora", "cel_rogues_castle", 0, 0, 0xD8E8F0, 0x1A1A1A, 0x0A0A0A, 0x8A1A1A);
        // The same five ghosts at the places of the wiki's second and third sequences (the upper-floor places stand on the
        // ground floor below them, approx). Viggora's third place is the Edgeville Dungeon's earth warrior cave (OSRS
        // 3119, 9996), here in the dungeon's earth warrior room (NpcStoreys). Same clothes as the first sequence's ghost
        // (the accent differs by one shade: every NPC has its own colours).
        int[][] cel2 = {{3024, 3691}, {3157, 3667}, {3044, 3204}, {3052, 3498}, {3448, 3550}};
        int[][] cel3 = {{3162, 2982}, {3068, 3858}, {2396, 3481}, {3046, 3379}, CEL_VIGGORA_3};
        for (int g = 0; g < CEL_GHOSTS.length; g++) {
            String firstId = "quest_" + CEL_GHOSTS[g];
            NpcDef first = b.npcs.stream().filter(n -> n.id().equals(firstId)).findFirst().orElseThrow();
            int[] c = first.colors();
            for (int route = 2; route <= 3; route++) {
                int[] at = (route == 2 ? cel2 : cel3)[g];
                b.npcAt(firstId + "_" + route, first.name(), at[0], at[1], c[0], c[1], c[2], c[3] ^ (route - 1));
            }
        }
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
        // Wiki: ghostly gloves and boots +2, cloak +5 magic attack and defence (the six pieces total +21).
        b.items(s -> s.add("ghostly_gloves", "Ghostly gloves", Kind.SIMPLE, "gloves", 0xD8E8F0).value(1).stack(1)
                .examine("The gloves of the Empty Lord's robes. Right-click to wear."));
        b.gear("ghostly_gloves", QuestBook.GearSlot.HANDS, new EquipmentStats(0, 0, 0, 2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0), null);
        b.items(s -> s.add("ghostly_boots", "Ghostly boots", Kind.ARMOUR, "boots", 0xD8E8F0).value(1).stack(1)
                .equipment(EquipmentDef.armour(EquipmentDef.Slot.FEET, new EquipmentStats(0, 0, 0, 2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0), 1))
                .examine("The boots of the Empty Lord's robes."));
        b.items(s -> s.add("ghostly_cloak", "Ghostly cloak", Kind.SIMPLE, "cape", 0xD8E8F0).value(1).stack(1)
                .examine("The cloak of the Empty Lord. Right-click to wear."));
        b.gear("ghostly_cloak", QuestBook.GearSlot.CAPE, new EquipmentStats(0, 0, 0, 5, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0), null);
        String[] sight = {"ring_of_visibility", "ghostspeak_amulet"};
        // Wiki: Valdez's hint picks one of three sequences for the whole miniquest ("pirates and a shipwreck": 1, "north-east":
        // 2, "south-east": 3); the ghosts stand at all three places but only talk at the player's own. Saves from before the
        // routes have no route flag and follow sequence 1 (the ghosts they already met).
        String[] routes = {"cel_route1", "cel_route2", "cel_route3"};
        QuestSpec q = b.quest("curse_of_the_empty_lord", "Curse of the Empty Lord (miniquest)", Difficulty.EXPERIENCED, 0)
                .reqQuest("desert_treasure_i", "RESTLESS_GHOST").req(Skill.PRAYER, 31)
                .stage("I can start this miniquest by speaking to the ghost Valdez at Glarial's Tomb, north-west of the Fishing Guild, while carrying "
                        + "a ring of visibility and an amulet of ghostspeak. (Requires Desert Treasure I, The Restless Ghost, 31 Prayer.)")
                .talk("quest_valdez").need(sight).flag("cel_valdez").flagOneOf(routes).give("ghostly_robe_top")
                .say("You can see me? Then hear my tale of woe. I am Valdez, an explorer. In a buried temple I found the Staff of Armadyl, "
                                + "and I was carrying it to Saradomin on Entrana when the thief Rennard waylaid and killed me.",
                        "Since that day I have been cursed to walk as a ghost. Take my robe, and find Rennard.")
                .say(VALDEZ_HINT)
                .missing("*Something is here, but you can't see or understand it. You need a ring of visibility and an amulet of ghostspeak.*")
                .stage("I should follow the tale of woe from one cursed ghost to the next: the thief Rennard, the messenger Kharrim, the spy "
                        + "Lennissa and the wizard Dhalak. Valdez's hint tells me where my sequence leads.")
                .talk("quest_valdez").need(sight).stay().say(VALDEZ_HINT);
        // Rennard (gloves), Kharrim (boots), Lennissa (robe bottoms) once each, then Dhalak (hood) moves the miniquest on; in
        // the order of the tale, at the places of the player's route.
        String[] pieces = {"ghostly_gloves", "ghostly_boots", "ghostly_robe_bottom", "ghostly_hood", "ghostly_cloak"};
        for (int g = 0; g < 4; g++) {
            String done = "cel_" + CEL_GHOSTS[g];
            for (int route = 1; route <= 3; route++) {
                q.talk(celGhost(g, route)).need(sight);
                celRoute(q, routes, route);
                if (g > 0) {
                    q.needFlag("cel_" + CEL_GHOSTS[g - 1]);
                }
                if (g < 3) {
                    q.unlessFlag(done).stay();
                }
                q.flag(done).give(pieces[g]).say(CEL_TALES[g]).say(CEL_NEXT[g][route - 1]).missing(GHOST_NOT_YET);
            }
        }
        for (int g = 0; g < 4; g++) {
            for (int route = 1; route <= 3; route++) {
                q.hint(celGhost(g, route), GHOST_ELSEWHERE);
            }
        }
        q.stage("I should find the last of the cursed: Viggora, once a general of Zaros.")
                // Saves from the old three-step version (whose completion gave the hood, top and bottoms) get the pieces they
                // missed: past Rennard (old stage 2) all five, past Valdez only (old stage 1) the robe top.
                .talk("quest_viggora").need(sight).stay().unlessFlag("cel_dhalak").flag("cel_dhalak", "cel_valdez")
                .give("ghostly_robe_top", "ghostly_gloves", "ghostly_boots", "ghostly_robe_bottom", "ghostly_hood")
                .say("The other ghosts' robes... they are bound to my curse. Take them, all of them, and hear me out.")
                .talk("quest_viggora").need(sight).stay().unlessFlag("cel_valdez").flag("cel_valdez").give("ghostly_robe_top")
                .say("Valdez's robe has drifted to me with the rest. Take it, and hear me out.");
        for (int route = 1; route <= 3; route++) {
            q.talk(celGhost(4, route)).need(sight);
            celRoute(q, routes, route);
            q.give(pieces[4]).say(CEL_TALES[4]).missing(GHOST_NOT_YET);
        }
        for (int route = 1; route <= 3; route++) {
            q.hint(celGhost(4, route), GHOST_ELSEWHERE);
        }
    }

    /** The five ghosts after Valdez, in the order of the tale. */
    static final String[] CEL_GHOSTS = {"rennard", "kharrim", "lennissa", "dhalak", "viggora"};

    /** A ghost's NPC at a route's place: the first sequence keeps the original ids. */
    static String celGhost(int ghost, int route) {
        return "quest_" + CEL_GHOSTS[ghost] + (route == 1 ? "" : "_" + route);
    }

    /**
     * Restricts the current trigger to a route: route 1 also serves saves from before the routes (no route flag), the
     * others need their flag. The other routes' flags skip it, so their players hear the ghost's "elsewhere" hint.
     */
    private static void celRoute(QuestSpec q, String[] routes, int route) {
        for (int r = 1; r <= 3; r++) {
            if (r != route) {
                q.unlessFlag(routes[r - 1]);
            }
        }
        if (route > 1) {
            q.needFlag(routes[route - 1]);
        }
    }

    /** Surface tile above Viggora's third place: the north-east of the Edgeville Dungeon's earth warrior room. */
    static final int[] CEL_VIGGORA_3 = {3105, 3545};

    /** Valdez's hint: the start of the player's sequence (wiki: pirates and a shipwreck, north-east, south-east). */
    private static final String[] VALDEZ_HINT = {
            // sequence 1, also for saves from before the routes (no route flag)
            "?!cel_route2|?!cel_route3|The thief fled into the Wilderness. Look for him among the pirates, by their shipwreck.",
            "?cel_route2|The thief fled north-east of here, into the Wilderness: the bandits' camp would hide him.",
            "?cel_route3|The thief fled south-east of here, to the desert bandits' camp. Look in their bar."};
    /** The ghosts' parts of the tale (Rennard, Kharrim, Lennissa, Dhalak, Viggora). */
    private static final String[][] CEL_TALES = {
            {"Yes, I killed Valdez for his package, and it was the Staff of Armadyl! In a drunken stupor I asked a messenger, Kharrim, "
                    + "to find me a buyer. That curse took me too. Take my gloves."},
            {"Rennard wanted a buyer for the staff, so I took it to Zamorak, a general of Zaros. With it he rose against his own lord.",
                    "Take my boots. Lucien told me of a spy for Saradomin hidden among the Mahjarrat: Lennissa."},
            {"I spied for Saradomin among Zaros's minions. When I learned the staff was being sold, I sent word to my superior, Dhalak, "
                    + "to warn Saradomin: Lucien watched me too closely to go myself. Take my robes, and ask Dhalak what he did."},
            {"I never told Saradomin. I cast a spell of concealment on the staff, so Zamorak could steal up on Zaros unseen.",
                    "Take my hood. The last of us is Viggora, the general who marched with Zamorak."},
            {"I am Viggora, once one of Zaros's generals, the only human among them. I joined Zamorak's assault on the Empty Lord's "
                    + "castle and watched Zamorak strike him down.",
                    "As Zaros fell he cursed all who had a hand in it, and being mortal I suffered the worst of it. Take my cloak: "
                            + "the robes are yours now."}};
    /** Where each ghost sends the player next, per route (wiki sequences 1-3). */
    private static final String[][] CEL_NEXT = {
            {"Kharrim haunts the Chaos Temple in the Wilderness.", "Kharrim haunts the Graveyard of Shadows.",
                    "Kharrim haunts the end of the Lava Maze, deep in the Wilderness."},
            {"Look for her in the church on Entrana.", "Look for her on the Port Sarim docks.",
                    "Look for her by the gnomeball field of the Tree Gnome Stronghold."},
            {"He haunts the Wizards' Tower.", "He haunts the Edgeville Monastery, by the altar upstairs.",
                    "He haunts the Falador Party Room, upstairs."},
            {"He lingers in the Rogues' Castle, deep in the Wilderness.", "He lingers in the Slayer Tower in Morytania.",
                    "He lingers in the Edgeville Dungeon, among the earth warriors."}};
    /** A ghost of the player's route before its turn in the tale. */
    private static final String[] GHOST_NOT_YET = {"* The ghost drifts past you, lost in its own sorrow. It isn't ready to tell you its part of the tale."};
    /** A ghost at another sequence's place. */
    private static final String[] GHOST_ELSEWHERE = {"* The ghost stares through you, lost in its own sorrow. Your tale doesn't lead here."};

    private static void daddysHome(QuestBook b) {
        Cast.npc(b, "marlo", "Marlo", "VARROCK", 8, 6, Cast.FAIR, 0x5A3A6A);
        Cast.npc(b, "old_man_yarlo", "Old Man Yarlo", "VARROCK", -8, -5, Cast.PALE, 0x6A5A3A);
        b.quest("daddys_home", "Daddy's Home (miniquest)", Difficulty.NOVICE, 0)
                .xp(Skill.CONSTRUCTION, 400).reward("plank:25", "steel_nails:50") // + 544 XP from building furniture during the quest
                .unlock("Construction contracts with Marlo")
                .stage("I can start this miniquest by speaking to Marlo in north-east Varrock.")
                .talk("marlo").say("My dad's house is falling apart and he won't let me fix it. Maybe he'll let you. Here's what you'll need: planks, nails, a hammer.")
                .stage("I should repair Old Man Yarlo's house (10 planks, 20 steel nails).")
                .talk("old_man_yarlo").take("plank:10", "steel_nails:20").need("hammer").flag("dh_repaired").say("*You mend the chairs, table and bed.* Hmph. Not bad. Not bad at all.")
                .missing("You'll need 10 planks and 20 steel nails to fix this place, and a hammer.")
                .stage("I should tell Marlo the house is repaired.")
                .talk("marlo").needFlag("dh_repaired").say("He actually said thank you? Here, take these supplies. Come back if you want more building work.");
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
                .reqQuest("desert_treasure_i")
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
        // NPCs spawn when their place is first built, and forthos_dungeon and quest_arceuus_library are already built in saved
        // worlds: the new NPCs get their own spots on the same chunks, at the same positions.
        b.spot("isk_forthos", "Forthos Dungeon entrance", 1702, 3574);
        b.spot("isk_arceuus_library", "Arceuus Library (Logosia)", 1632, 3808);
        b.npc("quest_brother_aimeri", "Brother Aimeri", "isk_forthos", 3, 3, 0xD8B898, 0x6A5A3A, 0x4A3A2A, 0x8A2A2A);
        b.npc("quest_logosia", "Logosia", "isk_arceuus_library", 3, 2, 0xE8D0B8, 0x3A4A7A, 0x2A2A5A, 0xD0B060);
        b.item("quest_lost_tome", "Lost tome", "book", 0x5A3A2A, "A tome missing from the Arceuus Library.");
        b.item("book_of_knowledge", "Book of knowledge", "book", 0xE0C060, "Reading this book grants experience in a skill of your choice.");
        b.item("tome_of_the_sun", "Tome of the sun", "book", 0xE0B030, "An old tome about the sun. Some pages are missing.");
        b.item("tome_of_the_moon", "Tome of the moon", "book", 0x8090C0, "An old tome about the moon. Some pages are missing.");
        b.item("tome_of_the_temple", "Tome of the temple", "book", 0x6A8A5A, "An old tome about the temple. Some pages are missing.");
        b.item("tattered_sun_page", "Tattered sun page", "note", 0xE8C870, "A tattered page from the tome of the sun.");
        b.item("tattered_moon_page", "Tattered moon page", "note", 0xB0B8D8, "A tattered page from the tome of the moon.");
        b.item("tattered_temple_page", "Tattered temple page", "note", 0xA0B890, "A tattered page from the tome of the temple.");
        // Wiki: the tomes are on the musty bookshelves in the corners of the temple library, the south-western room of the
        // Forthos Dungeon (sun 1804,9934; moon 1804,9943; temple 1796,9935; the north-western set holds a scroll), here the
        // west druid room, whose surface tiles (the dungeon is shifted by -128, -6398) are around (1672, 3540).
        b.spot("isk_temple_library", "Forthos temple library", 1672, 3540);
        b.quest("in_search_of_knowledge", "In Search of Knowledge (miniquest)", Difficulty.EXPERIENCED, 0)
                .reward("book_of_knowledge")
                .stage("I can start this miniquest by speaking to the starving Brother Aimeri in the Forthos Dungeon, by the entrance to Sarachnis' lair.")
                .talk("quest_brother_aimeri").say("*He wolfs down the food you give him.* Thank you, friend. I came for the temple's old tomes, "
                        + "but the druids and the spiders... Find the tomes of the sun, moon and temple, and their lost pages, and take them to Logosia.")
                .stage("I should search the musty bookshelves in the temple library of the Forthos Dungeon for the tomes of the sun, moon and "
                        + "temple, and collect four tattered sun, moon and temple pages from the dungeon's monsters.")
                .reach("isk_temple_library", 14).underground().unless("tome_of_the_sun").stay().give("tome_of_the_sun")
                .say("* You search the musty bookshelf in the library's south-east corner and find the tome of the sun.")
                .reach("isk_temple_library", 14).underground().unless("tome_of_the_moon").stay().give("tome_of_the_moon")
                .say("* You search the musty bookshelf in the north-east corner and find the tome of the moon.")
                .reach("isk_temple_library", 14).underground().unless("tome_of_the_temple").stay().give("tome_of_the_temple")
                .say("* You search the musty bookshelf in the south-west corner and find the tome of the temple.")
                // Wiki page drop rates (one page of any kind): Sarachnis 1/5, red dragon 1/10, undead druid 1/20, baby red dragon
                // 1/25, temple spider 1/30, only in the Forthos Dungeon. Each kind of page rolls on its own here at half the listed
                // chance, so the twelve pages take about as many kills as in OSRS (approx).
                .drop("sarachnis", "tattered_sun_page", 10).times(4).near("isk_forthos", 70)
                .drop("sarachnis", "tattered_moon_page", 10).times(4).near("isk_forthos", 70)
                .drop("sarachnis", "tattered_temple_page", 10).times(4).near("isk_forthos", 70)
                .drop("red_dragon", "tattered_sun_page", 20).times(4).near("isk_forthos", 70)
                .drop("red_dragon", "tattered_moon_page", 20).times(4).near("isk_forthos", 70)
                .drop("red_dragon", "tattered_temple_page", 20).times(4).near("isk_forthos", 70)
                .drop("undead_druid", "tattered_sun_page", 40).times(4).near("isk_forthos", 70)
                .drop("undead_druid", "tattered_moon_page", 40).times(4).near("isk_forthos", 70)
                .drop("undead_druid", "tattered_temple_page", 40).times(4).near("isk_forthos", 70)
                .drop("baby_red_dragon", "tattered_sun_page", 50).times(4).near("isk_forthos", 70)
                .drop("baby_red_dragon", "tattered_moon_page", 50).times(4).near("isk_forthos", 70)
                .drop("baby_red_dragon", "tattered_temple_page", 50).times(4).near("isk_forthos", 70)
                .drop("temple_spider", "tattered_sun_page", 60).times(4).near("isk_forthos", 70)
                .drop("temple_spider", "tattered_moon_page", 60).times(4).near("isk_forthos", 70)
                .drop("temple_spider", "tattered_temple_page", 60).times(4).near("isk_forthos", 70)
                .talk("quest_brother_aimeri").need("tome_of_the_sun", "tome_of_the_moon", "tome_of_the_temple")
                .take("tattered_sun_page:4", "tattered_moon_page:4", "tattered_temple_page:4")
                .say("*You put the pages back in their tomes.* Complete! Take them to Logosia in the Arceuus Library.")
                .missing("The three tomes, and four of each kind of page. The tomes are on the musty bookshelves of the temple library, "
                        + "where the undead druids are; the dungeon's monsters carry the pages.")
                .stage("I should take the tomes to Logosia in the Arceuus Library.")
                .talk("quest_logosia").take("tome_of_the_sun", "tome_of_the_moon", "tome_of_the_temple")
                .say("The lost tomes of the Forthos temple! The library is in your debt. Take this, and learn something new.")
                // Saves from the old version (a lost tome found on the Piscatoris shore) reach this stage carrying it.
                .talk("quest_logosia").take("quest_lost_tome")
                .say("Professor Gracklebone's lost tome! The library is in your debt. Take this, and learn something new.")
                .hint("quest_professor_gracklebone", "Lost tomes? Logosia looks after those. She's just over there.");
    }

    private static void intoTheTombs(QuestBook b) {
        // Wiki: Maisa at the Jaltevas Pyramid hands over Akila's journal; a full Tombs of Amascut run (the four lesser gods
        // freed, both Wardens beaten) follows, then Maisa in the tombs' lobby.
        b.quest("into_the_tombs", "Into the Tombs (miniquest)", Difficulty.MASTER, 0)
                .reqQuest("beneath_cursed_sands")
                .unlock("The Tombs of Amascut's full story")
                .stage("I can start this miniquest by speaking to Maisa. (Requires Beneath Cursed Sands.)")
                .talk("maisa").say("Amascut has seized the lesser gods - Apmeken, Crondis, Het and Scabaras - in the tombs beneath Jaltevas. "
                        + "Take Akila's journal, free them, and stop her.")
                .stage("I should complete a full raid of the Tombs of Amascut, freeing the lesser gods and defeating the Wardens.")
                .reach("toa_jaldraocht_entrance", 10).stay().unlessFlag("itt_tombs").flag("itt_tombs")
                .say("* In the tombs you hear Amascut's voice, promising the destruction of Tumeken's line.")
                .kill("tumekens_warden").flag("itt_tombs").say("* The Warden crumbles. In Osmumten's burial chamber the lesser gods stand free.")
                .kill("elidinis_warden").flag("itt_tombs").say("* The Warden crumbles. In Osmumten's burial chamber the lesser gods stand free.")
                .stage("I should talk to Maisa in the tombs' lobby.")
                .talk("maisa").needFlag("itt_tombs").say("Amascut fled... to Menaphos, perhaps, to think again. The gods are free because of you.")
                .done("tumeken_priest", "Amascut's followers are scattered. Tumeken's line endures, thanks to you.");
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
                .kill("quest_kolodion").flag("ma1_kolodion").say("* Kolodion reverts to his human form, laughing. 'Well fought! Now choose your god.'")
                .stage("I should pray at the statue of the god whose power I want: Saradomin, Guthix or Zamorak.")
                .talk("quest_statue_saradomin").needFlag("ma1_kolodion").give("saradomin_staff", "saradomin_cape").say("* A voice like a bell rings out. You receive a Saradomin staff and cape.")
                .talk("quest_statue_guthix").needFlag("ma1_kolodion").give("guthix_staff", "guthix_cape").say("* The ground hums with balance. You receive a Guthix staff and cape.")
                .talk("quest_statue_zamorak").needFlag("ma1_kolodion").give("zamorak_staff", "zamorak_cape").say("* Fire crackles around you. You receive a Zamorak staff and cape.");

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
        // Ranulph keeps the old id quest_totem_carver (saved worlds).
        Cast.npc(b, "quest_totem_carver", "Ranulph", "quest_auburn_vale", 0, 2, Cast.BROWN, 0x8A6A2A);
        b.quest("vale_totems", "Vale Totems (miniquest)", Difficulty.NOVICE, 0)
                .reqQuest("children_of_the_sun").req(Skill.FLETCHING, 20) // OSRS: no XP reward (Sacraments of ent folk, access to Vale Totems)
                .unlock("Carving and decorating totems in Auburnvale")
                .stage("I can start this miniquest by speaking to Ranulph in north-western Auburnvale. (Requires Children of the Sun, 20 Fletching.)")
                .talk("quest_totem_carver").say("The ents are restless. This old totem... *crack* Oops. Well, now we'll have to build a new one, won't we?")
                .stage("I should build a new totem from oak logs (or better), carve the nearby spirit animals into it and decorate it with four "
                        + "oak shortbows (u) fletched from the same wood.")
                .talk("quest_totem_carver").take("oak_logs:3").need("knife").stay().unlessFlag("vt_base").flag("vt_base")
                .say("*You build the totem's base at the site.* Good and solid. Now carve the spirits that watch over this place.")
                .missing("Logs for the base - oak or better - and a knife, friend.")
                .puzzle("quest_totem_carver", VALE_SPIRITS).needFlag("vt_base").unlessFlag("vt_carved").flag("vt_carved").stay()
                .say("*You carve the three spirits into the totem.* They'll like that. Now decorate it with bows of the same wood.")
                .talk("quest_totem_carver").needFlag("vt_carved").take("oak_shortbow_u:4")
                .say("*You hang the bows on the totem.* Look - the ents have left an offering! They approve. These sacraments are yours.")
                .missing("Four oak shortbows, unstrung, to decorate the totem.");
    }

    /**
     * Vale Totems' carving (wiki): each player's totem site is watched by three of the vale's spirit animals, which must
     * be carved (in any order). The five animals are approx.
     */
    static final QuestPuzzle VALE_SPIRITS = valeSpirits();

    private static QuestPuzzle valeSpirits() {
        String[] animals = {"Buffalo", "Jaguar", "Eagle", "Snake", "Scorpion"};
        QuestPuzzle.Builder b = QuestPuzzle.builder("vale_totems").room("carving");
        for (int a = 0; a < animals.length; a++) {
            for (int c = a + 1; c < animals.length; c++) {
                for (int d = c + 1; d < animals.length; d++) {
                    b.variant(null).prompt("* Around the totem site you spot the spirits of a " + animals[a].toLowerCase(java.util.Locale.ROOT) + ", a "
                                    + animals[c].toLowerCase(java.util.Locale.ROOT) + " and a " + animals[d].toLowerCase(java.util.Locale.ROOT) + ".",
                            "* Which spirits do you carve into the totem?")
                            .options(animals).anyOrder(a, c, d);
                }
            }
        }
        return b.failLines("Those aren't the spirits watching this place. Look around again.").build();
    }
}
