package com.runecraft.osrs.content.zeah;

import com.runecraft.osrs.content.NpcDef;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Kourend and Varlamore NPCs with their dialogue. The five house leaders Lord Kandur Hosidius,
 * Lady Shauna Piscarilius, Lord Shiro Shayzien and Lord Trobin Arceuus already stand in the
 * quests module's house squares (their talk handlers belong to the quest scripts); this module
 * adds Lady Vulcana Lovakengj, the real leader of House Lovakengj, plus each town's notable
 * residents, the travel NPCs and the guild masters.
 */
public final class ZeahNpcs {
    private ZeahNpcs() {
    }

    public static final String CABIN_BOY_HERBERT = "zeah_cabin_boy_herbert";
    public static final String VEOS_PISCARILIUS = "zeah_veos_piscarilius";
    public static final String CAPTAIN_MAGORO = "zeah_captain_magoro";
    public static final String PRIMIO = "zeah_primio";
    public static final String PRIMIO_CIVITAS = "zeah_primio_civitas";
    public static final String LADY_VULCANA = "zeah_lady_vulcana_lovakengj";
    public static final String THIRUS = "zeah_thirus";
    public static final String HUNTMASTER_GILMAN = "zeah_huntmaster_gilman";

    /** The quetzal (or Renu, at Civitas) waiting at a quetzal landing site. */
    public static String quetzal(String place) {
        return place.equals(ZeahPlaces.CIVITAS) ? "zeah_renu" : "zeah_quetzal_" + place.replace("zeah_", "");
    }

    private static final Map<String, List<String>> DIALOGUE = new LinkedHashMap<>();

    private static NpcDef npc(List<NpcDef> out, String id, String name, String place, int dx, int dz, int skin, int top, int bottom,
                              int hat, String... lines) {
        NpcDef n = NpcDef.of(id, name, place, dx, dz, skin, top, bottom, hat);
        out.add(n);
        if (lines.length > 0) {
            DIALOGUE.put(id, List.of(lines));
        }
        return n;
    }

    /** Dialogue lines an NPC says when talked to (one per talk, cycling), or an empty list. */
    public static List<String> dialogue(String npcId) {
        if (DIALOGUE.isEmpty()) {
            all();
        }
        return DIALOGUE.getOrDefault(npcId, List.of());
    }

    public static List<NpcDef> all() {
        List<NpcDef> out = new ArrayList<>();
        // ---- Kourend Castle & Kingstown ----
        npc(out, "zeah_kourend_castle_guard", "Kourend Castle guard", ZeahPlaces.KOUREND_CASTLE, 3, -6, 0xD8B090, 0x6A2A7A,
                0x3A2A3A, 0xA0A0A8,
                "Welcome to Kourend Castle, seat of the Kourend Council.",
                "The five houses rule Great Kourend together since the death of King Kharedst IV.",
                "The Catacombs of Kourend lie beneath the castle. Mind the dark totems - Skotizo waits below.");
        npc(out, "zeah_kingstown_cook", "Kourend Kitchen cook", ZeahPlaces.KINGSTOWN, -3, 2, 0xE0C0A0, 0xF0F0F0, 0x6A6A6A, 0xF0F0F0,
                "The range in the Kourend Kitchen is the best in Kourend - you'll burn less food here.",
                "Hosidius farms supply most of what we cook.");
        npc(out, "zeah_kourend_historian", "Historian Duffy", ZeahPlaces.KINGSTOWN, 4, -3, 0xD8A880, 0x5A4A3A, 0x3A2A1A, 0x4A3A2A,
                "Great Kourend was founded by King Rada I after the Kebos refugees settled the land.",
                "Xeric ruled from the old temple in the Kebos Lowlands before Kourend was united.",
                "Rada's blessing is still granted to those who serve all five houses well.");
        // ---- Arceuus ----
        npc(out, "zeah_aretha", "Aretha", ZeahPlaces.SOUL_ALTAR, 3, 2, 0xE8D0C3, 0x5A2A8D, 0x3A1A5D, 0xE0E0EF,
                "The soul altar draws on the essence of the dead. Bind dark essence fragments here to make soul runes.",
                "You need level 90 Runecraft to use this altar.");
        npc(out, "zeah_tyss", "Tyss", ZeahPlaces.DARK_ALTAR, -3, 3, 0xC8C0D8, 0x2A1A3A, 0x1A1A2A, 0x6A3A8A,
                "The Dark Altar is sacred to the Arceuus. Pray here to learn the Arceuus spellbook.",
                "Venerate dense essence here and it becomes dark essence.");
        npc(out, "zeah_biblia", "Biblia", ZeahPlaces.ARCEUUS_LIBRARY, 2, 2, 0xE8C8B0, 0x6A3A8A, 0x3A1A5A, 0xC0C0C0,
                "Welcome to the Arceuus Library. Professor Gracklebone and Villia can always use help finding books.",
                "Help the customers and they will reward you with experience.");
        npc(out, "zeah_professor_gracklebone", "Professor Gracklebone", ZeahPlaces.ARCEUUS_LIBRARY, -3, 1, 0xD8B8A0, 0x4A2A6A,
                0x2A1A3A, 0xE0E0E0,
                "Have you seen my copy of 'The Treachery of Royalty'? I'm sure I left it on a shelf somewhere.",
                "The Library holds the history of Great Kourend - and of Xeric.");
        npc(out, "zeah_horphis", "Horphis", ZeahPlaces.ARCEUUS_LIBRARY, 0, -4, 0xD0B090, 0x3A2A5A, 0x2A1A3A, 0x8A8A9A,
                "Bring me the Book of the dead and I'll tell you its secrets. The Kharedst memoirs too, if you find them.");
        npc(out, "zeah_arceuus_mage", "Arceuus mage", ZeahPlaces.ARCEUUS, 3, 3, 0xE0C8B0, 0x5A2A8D, 0x3A1A5D, 0x5A2A8D,
                "House Arceuus studies the magic of souls and the dead.",
                "Lord Trobin Arceuus leads our house from his tower.");
        // ---- Hosidius ----
        npc(out, "zeah_marisi", "Marisi", ZeahPlaces.HOSIDIUS, -6, 6, 0xC89870, 0x5A7A3A, 0x4A3A2A, 0xD8C080,
                "Hosidius is the breadbasket of Kourend. I'll look after your allotment for a small fee.",
                "The soil here is so fertile that compost works wonders.");
        npc(out, "zeah_hosidius_ploughman", "Hosidius ploughman", ZeahPlaces.HOSIDIUS, 6, -4, 0xD8A880, 0x3A6A3A, 0x2A4A2A, 0xC0A060,
                "Lord Kandur keeps the farms running. Without Hosidius, Kourend would starve.",
                "The Tithe Farm to the south needs hard workers too.");
        // ---- Port Piscarilius ----
        npc(out, "zeah_tynan", "Tynan", ZeahPlaces.PORT_PISCARILIUS, -4, 3, 0xD0A080, 0x3A5A9B, 0x2A3A6B, 0x4A4A4A,
                "Tynan's Fishing Supplies! Nets, harpoons, bait - all you need for the Piscarilius waters.",
                "Anglerfish bite off the Piscarilius piers if you're good enough.");
        npc(out, VEOS_PISCARILIUS, "Veos", ZeahPlaces.PORT_PISCARILIUS, 6, -6, 0xE0C0A0, 0x6A3A1A, 0x3A2A1A, 0xA08040);
        npc(out, "zeah_queen_of_thieves", "Queen of Thieves", ZeahPlaces.PORT_PISCARILIUS, -6, -6, 0xE8C8B0, 0x2A2A2A, 0x1A1A1A,
                0x6A1A1A,
                "Lady Shauna thinks she runs this port. My people know better.",
                "Piscarilius has always been the house of trade - and of those who trade in secrets.");
        // ---- Shayzien ----
        npc(out, "zeah_captain_ginea", "Captain Ginea", ZeahPlaces.SHAYZIEN, 4, 4, 0xC8986D, 0xA02A2C, 0x5A1A1C, 0x8A8A8A,
                "Shayzien soldiers train in the Combat Ring. Prove yourself against the lizardmen of the canyon.",
                "Shayzien armour protects you from the lizardman shamans' poison.");
        npc(out, "zeah_shayzien_quartermaster", "Shayzien quartermaster", ZeahPlaces.SHAYZIEN_ENCAMPMENT, 2, 2, 0xD0A070, 0xA02A2C,
                0x5A1A1C, 0x6A6A6A,
                "Every soldier needs proper armour. The Shayzien sets come in five grades; the fifth is the best.",
                "Wear full Shayzien (5) and the shamans' acid won't poison you.");
        // ---- Lovakengj ----
        npc(out, LADY_VULCANA, "Lady Vulcana Lovakengj", ZeahPlaces.LOVAKENGJ, -3, 3, 0xD8A88D, 0x6A6A6C, 0x3A3A3F, 0xC07A30,
                "House Lovakengj mines and smiths for all of Kourend. Our forges never cool.",
                "The Lovakite mine and the Blast Mine feed our furnaces. Dynamite is cheap - mistakes are not.",
                "The minecart network was built by my house. It will carry you anywhere in the kingdom for a few coins.");
        npc(out, THIRUS, "Thirus", ZeahPlaces.BLAST_MINE, 3, 3, 0xC89870, 0x5A5A5A, 0x3A3A3A, 0xE0A020,
                "The hard rock here needs dynamite. Bring a tinderbox and Mining level 43.",
                "I sell dynamite for 40 coins a stick. Talk to me again with coins to buy a stick.");
        npc(out, "zeah_lovakengj_smith", "Lovakengj smith", ZeahPlaces.LOVAKENGJ, 4, -3, 0xC88860, 0x4A4A4A, 0x2A2A2A, 0x6A4A2A,
                "Lovakite ore needs level 65 Mining. Smelt it with two coal to make a lovakite bar.");
        // ---- Kebos, guilds & the countryside ----
        npc(out, "zeah_perry", "Perry", ZeahPlaces.WOODCUTTING_GUILD, 3, 3, 0xD8B090, 0x4A6A2A, 0x3A2A1A, 0x6A4A2A,
                "Perry's Chop-chop Shop! The guild has the only redwood trees in Gielinor.",
                "You need level 60 Woodcutting to use the guild's trees.");
        npc(out, "zeah_guildmaster_jane", "Guildmaster Jane", ZeahPlaces.FARMING_GUILD, -4, 4, 0xE0C0A0, 0x3A7A3A, 0x2A4A2A, 0xD8C080,
                "Welcome to the Farming Guild. The higher your Farming level, the further in you may go.",
                "Help the Farming Guild with contracts and you'll be rewarded with seed packs.");
        npc(out, "zeah_alry_the_angler", "Alry the Angler", ZeahPlaces.MOLCH, 2, 2, 0xD0A880, 0x3A5A3A, 0x2A3A2A, 0x6A5A3A,
                "My cormorants fish Lake Molch for me. Bluegill, common tench, mottled eel, greater siren...",
                "Aerial fishing needs level 43 Fishing and 35 Hunter.");
        npc(out, CAPTAIN_MAGORO, "Captain Magoro", ZeahPlaces.LANDS_END, 4, -4, 0xC89870, 0x2A3A6A, 0x1A2A4A, 0x2A2A2A);
        npc(out, CABIN_BOY_HERBERT, "Cabin Boy Herbert", "PORT_SARIM", -6, 7, 0xE0C0A0, 0x6A3A1A, 0x3A2A1A, 0x6A3A1A);
        // ---- Varlamore ----
        npc(out, "zeah_prince_itzla_arkan", "Prince Itzla Arkan", ZeahPlaces.CIVITAS, 4, -8, 0xA87850, 0xD0A030, 0x6A3A1A, 0xF0D040,
                "Welcome to Civitas illa Fortis, capital of Varlamore.",
                "My family rules from the Tower of Ascension. Ralos' sun shines on us all.",
                "Test yourself in the Fortis Colosseum if you dare - Sol Heredit has never been defeated.");
        npc(out, PRIMIO_CIVITAS, "Primio", ZeahPlaces.CIVITAS, -4, 8, 0xB88860, 0x8A6A3A, 0x5A3A1A, 0x3A2A1A);
        npc(out, PRIMIO, "Primio", "VARROCK", 6, -7, 0xB88860, 0x8A6A3A, 0x5A3A1A, 0x3A2A1B);
        npc(out, "zeah_minimus", "Minimus", ZeahPlaces.FORTIS_COLOSSEUM, -4, -4, 0xB88860, 0xC0A060, 0x6A4A2A, 0xE0C040,
                "Welcome to the Fortis Colosseum! Twelve waves stand between you and Sol Heredit.",
                "Glory awaits - and sunfire splinters for the fanatics among you.");
        npc(out, HUNTMASTER_GILMAN, "Huntmaster Gilman", ZeahPlaces.HUNTER_GUILD, -3, 3, 0xC89870, 0x5A6A2A, 0x3A4A1A, 0x8A6A3A,
                "Welcome to the Hunter Guild. Speak to the guild hunters for rumours - and rewards.",
                "Sunlight antelopes graze the Avium Savannah; their moonlit cousins hide in the caves below the guild.");
        npc(out, "zeah_guild_hunter_wolf", "Guild Hunter Wolf", ZeahPlaces.HUNTER_GUILD, 4, 2, 0xB88860, 0x4A4A3A, 0x2A2A1A, 0x8A8A8A,
                "I give master rumours. Moonlight antelopes need level 91 Hunter.",
                "Bring me back proof of your catch and you'll earn a hunters' loot sack.");
        npc(out, "zeah_guild_hunter_aco", "Guild Hunter Aco", ZeahPlaces.HUNTER_GUILD, 2, -4, 0xA87850, 0x6A5A2A, 0x3A3A1A, 0x6A5A3A,
                "Expert rumours: sunlight antelopes, tecu salamanders and pyre foxes.",
                "Wear the guild hunter outfit with pride.");
        npc(out, "zeah_cam_torum_miner", "Cam Torum miner", ZeahPlaces.CAM_TORUM, 3, 3, 0xA87850, 0x6A5A4A, 0x3A3A2A, 0xE0A020,
                "Cam Torum was built inside the mountain. The sulphur naguas guard the old ruins nearby.");
        npc(out, "zeah_aldarin_vintner", "Aldarin vintner", ZeahPlaces.ALDARIN, 2, 3, 0xB88860, 0x8A3A5A, 0x4A2A3A, 0xC09040,
                "Aldarin wine is the finest in Varlamore. The villa on the hill makes the best.");
        for (ZeahTravel.Stop s : ZeahTravel.QUETZAL.stops()) {
            String id = quetzal(s.place());
            boolean renu = id.equals("zeah_renu");
            npc(out, id, renu ? "Renu" : "Quetzal", s.place(), renu ? 8 : 5, renu ? 6 : 5, 0x3AA05A, 0x2A8A6A, 0xD03A2A, 0xE0C040);
        }
        return out;
    }
}
