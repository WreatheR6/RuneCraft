package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.Skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Achievement diaries for every area with easy/medium/hard/elite tasks. Tasks are modelled on the real
 * OSRS tasks and expressed as things the game can observe (levels, XP gained in an area, kills, items
 * made or carried, quest completions, places visited, boss kill counts, clues and Grand Exchange trades).
 */
public final class Diaries {
    public enum Tier {
        EASY(2500, 30), MEDIUM(7500, 40), HARD(15000, 50), ELITE(50000, 70);

        /** XP from the antique lamp reward and the minimum level of the skill it can go into. */
        public final int lampXp;
        public final int lampMinLevel;

        Tier(int lampXp, int lampMinLevel) {
            this.lampXp = lampXp;
            this.lampMinLevel = lampMinLevel;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public String displayName() {
            return Character.toUpperCase(id().charAt(0)) + id().substring(1);
        }
    }

    /** A diary area: kingdoms (Region enum names) and/or a circle around OSRS coordinates. */
    public record Area(String id, String name, List<String> regions, int x, int y, int radius, String rewardBase, String rewardName) {
        public boolean contains(String region, int px, int py) {
            if (region != null && regions.contains(region)) {
                return true;
            }
            return radius > 0 && Teleports.within(px, py, x, y, radius);
        }

        public String reward(Tier tier) {
            return rewardBase + "_" + (tier.ordinal() + 1);
        }
    }

    public enum Type { LEVEL, XP_IN, KILL, MAKE, HAVE, QUEST, VISIT, CLUE, GE, KC, CAST, TOTAL_LEVEL }

    /**
     * A task. {@code target} is a skill name (LEVEL/XP_IN), monster keyword (KILL), item id (MAKE/HAVE,
     * a leading '*' matches any id containing the rest), quest id, place "x,y,r" (VISIT), clue tier, boss id
     * (KC) or spell id (CAST). {@code restrict} is the area the action must happen in (null = anywhere).
     */
    public record Task(String id, String area, Tier tier, String description, Type type, String target, int count,
                       String restrict, int level) {
    }

    public static final List<Area> AREAS = List.of(
            new Area("ardougne", "Ardougne", List.of(), 2620, 3260, 130, "ardougne_cloak", "Ardougne cloak"),
            new Area("desert", "Desert", List.of("KHARIDIAN_DESERT"), 3330, 2950, 0, "desert_amulet", "Desert amulet"),
            new Area("falador", "Falador", List.of("ASGARNIA"), 2965, 3380, 100, "falador_shield", "Falador shield"),
            new Area("fremennik", "Fremennik", List.of("FREMENNIK_PROVINCE", "TROLL_COUNTRY"), 2660, 3657, 0, "fremennik_sea_boots",
                    "Fremennik sea boots"),
            new Area("kandarin", "Kandarin", List.of("KANDARIN"), 2730, 3480, 0, "kandarin_headgear", "Kandarin headgear"),
            new Area("karamja", "Karamja", List.of("KARAMJA"), 2850, 3000, 0, "karamja_gloves", "Karamja gloves"),
            new Area("kourend", "Kourend & Kebos", List.of(), 1600, 3640, 380, "radas_blessing", "Rada's blessing"),
            new Area("lumbridge", "Lumbridge & Draynor", List.of("LUMBRIDGE_SWAMP"), 3160, 3240, 110, "explorers_ring", "Explorer's ring"),
            new Area("morytania", "Morytania", List.of("MORYTANIA"), 3496, 3400, 0, "morytania_legs", "Morytania legs"),
            new Area("varrock", "Varrock", List.of(), 3213, 3440, 110, "varrock_armour", "Varrock armour"),
            new Area("western", "Western Provinces", List.of("TIRANNWN", "FELDIP_HILLS"), 2461, 3444, 90, "western_banner",
                    "Western banner"),
            new Area("wilderness", "Wilderness", List.of("WILDERNESS"), 3100, 3700, 0, "wilderness_sword", "Wilderness sword"));

    private static final List<Task> TASKS = new ArrayList<>();
    private static final Map<String, Task> BY_ID = new LinkedHashMap<>();

    private static String area;
    private static Tier tier;

    private static void in(String a, Tier t) {
        area = a;
        tier = t;
    }

    private static void add(String desc, Type type, String target, int count, String restrict, int level) {
        String id = area + "_" + tier.id() + "_" + (int) TASKS.stream().filter(x -> x.area().equals(area) && x.tier() == tier).count();
        Task task = new Task(id, area, tier, desc, type, target, count, restrict, level);
        TASKS.add(task);
        BY_ID.put(id, task);
    }

    private static void level(String desc, Skill s, int lvl) {
        add(desc, Type.LEVEL, s.name(), 1, null, lvl);
    }

    private static void xp(String desc, Skill s, int lvl) {
        add(desc, Type.XP_IN, s.name(), 1, area, lvl);
    }

    private static void xpAt(String desc, Skill s, int lvl, String place) {
        add(desc, Type.XP_IN, s.name(), 1, place, lvl);
    }

    private static void kill(String desc, String keyword, int n) {
        add(desc, Type.KILL, keyword, n, area, 0);
    }

    private static void killAnywhere(String desc, String keyword, int n) {
        add(desc, Type.KILL, keyword, n, null, 0);
    }

    /** The skill and level are documentation only: making the item already requires them. */
    private static void make(String desc, String item, int n, Skill s, int lvl) {
        if (item.endsWith("rune")) {
            // Runecrafting doesn't go through a make menu: carrying the runes counts.
            add(desc, Type.HAVE, item, n, null, 0);
            return;
        }
        add(desc, Type.MAKE, item, n, null, 0);
    }

    private static void have(String desc, String item) {
        add(desc, Type.HAVE, item, 1, area, 0);
    }

    private static void quest(String desc, String questId) {
        add(desc, Type.QUEST, questId, 1, null, 0);
    }

    private static void visit(String desc, int x, int y, int r) {
        add(desc, Type.VISIT, x + "," + y + "," + r, 1, null, 0);
    }

    private static void clue(String desc, String clueTier) {
        add(desc, Type.CLUE, clueTier, 1, null, 0);
    }

    private static void kc(String desc, String boss, int n) {
        add(desc, Type.KC, boss, n, null, 0);
    }

    private static void cast(String desc, String spell) {
        add(desc, Type.CAST, spell, 1, null, 0);
    }

    static {
        // ======================= Ardougne =======================
        in("ardougne", Tier.EASY);
        visit("Visit East Ardougne market", 2662, 3305, 12);
        xp("Steal a cake from the East Ardougne market stalls", Skill.THIEVING, 5);
        visit("Enter the Combat Training Camp north of West Ardougne", 2518, 3356, 12);
        visit("Use the altar in East Ardougne's church", 2616, 3307, 10);
        visit("Go out fishing on the Fishing Trawler (Port Khazard)", 2675, 3170, 15);
        visit("Visit the Ardougne Zoo", 2610, 3280, 15);
        in("ardougne", Tier.MEDIUM);
        visit("Enter the unicorn pen in Ardougne Zoo", 2610, 3270, 8);
        xp("Steal from the silk stall in East Ardougne", Skill.THIEVING, 20);
        xp("Pickpocket the master farmer north of East Ardougne", Skill.THIEVING, 38);
        visit("Visit the Ardougne Castle", 2580, 3290, 15);
        xp("Craft some water runes at the nearby altar", Skill.RUNECRAFT, 5);
        level("Reach 38 Thieving", Skill.THIEVING, 38);
        in("ardougne", Tier.HARD);
        xp("Steal from the gem stall in East Ardougne", Skill.THIEVING, 75);
        xp("Pickpocket a hero (or other high-level target) in Ardougne", Skill.THIEVING, 80);
        visit("Enter the Magic Guild in Yanille", 2590, 3087, 10);
        make("Craft any death runes", "death_rune", 1, Skill.RUNECRAFT, 65);
        level("Reach 70 Smithing", Skill.SMITHING, 68);
        visit("Teleport to the Watchtower", 2549, 3112, 12);
        in("ardougne", Tier.ELITE);
        xp("Steal from Ardougne's gem stall with 82 Thieving", Skill.THIEVING, 82);
        make("Smith a runite platebody", "rune_platebody", 1, Skill.SMITHING, 99);
        level("Reach 91 Runecraft", Skill.RUNECRAFT, 91);
        level("Reach 94 Magic (Ice Barrage)", Skill.MAGIC, 94);
        kill("Defeat 50 monsters around Ardougne", "", 50);
        clue("Complete an elite clue scroll", "elite");

        // ======================= Desert =======================
        in("desert", Tier.EASY);
        visit("Visit the Pyramid Plunder entrance in Sophanem", 3289, 2800, 20);
        kill("Kill a desert creature (e.g. a lizard or scorpion)", "", 1);
        visit("Enter Pollnivneach", 3359, 2970, 15);
        visit("Visit the Kalphite Lair entrance", 3226, 3108, 15);
        xp("Catch something with Hunter in the desert", Skill.HUNTER, 5);
        have("Carry a waterskin or any vial of water in the desert", "vial_of_water");
        in("desert", Tier.MEDIUM);
        visit("Visit Nardah", 3428, 2892, 15);
        visit("Climb to the top of the Agility Pyramid", 3354, 2830, 15);
        xp("Pickpocket a villager in Pollnivneach", Skill.THIEVING, 25);
        visit("Visit the Desert Mining Camp", 3285, 3030, 15);
        level("Reach 50 Agility", Skill.AGILITY, 30);
        quest("Complete Icthlarin's Little Helper", "icthlarins_little_helper");
        in("desert", Tier.HARD);
        visit("Visit the Jaldraocht Pyramid (Ancient altar)", 3233, 2898, 10);
        kill("Kill a dust devil in the desert", "dust devil", 1);
        level("Reach 65 Slayer", Skill.SLAYER, 65);
        quest("Complete Desert Treasure I", "desert_treasure_i");
        level("Reach 70 Firemaking", Skill.FIREMAKING, 60);
        cast("Cast an Ancient Magicks combat spell", "smoke_rush");
        in("desert", Tier.ELITE);
        cast("Cast Ice Barrage", "ice_barrage");
        kill("Defeat 50 desert monsters", "", 50);
        level("Reach 91 Prayer", Skill.PRAYER, 85);
        level("Reach 80 Fletching", Skill.FLETCHING, 95);
        level("Reach 91 Thieving", Skill.THIEVING, 91);
        visit("Teleport to the Pharaoh's sceptre location (Jalsavrah)", 3289, 2800, 20);

        // ======================= Falador =======================
        in("falador", Tier.EASY);
        visit("Find out what your family crest is (visit Falador)", 2965, 3380, 15);
        make("Smith a blurite or bronze item", "*bronze", 1, Skill.SMITHING, 1);
        visit("Visit the Falador Park", 3005, 3376, 12);
        kill("Kill a duck or chicken in Falador Park", "chicken", 1);
        quest("Complete Doric's Quest", "DORICS_QUEST");
        visit("Climb to the top of the White Knights' Castle", 2970, 3340, 15);
        in("falador", Tier.MEDIUM);
        xp("Mine some coal in the Falador area", Skill.MINING, 30);
        kill("Kill a mogre or other monster near Port Sarim", "", 3);
        visit("Visit the Crafting Guild", 2933, 3290, 10);
        visit("Visit Port Sarim docks", 3036, 3194, 15);
        xp("Pickpocket a Falador guard", Skill.THIEVING, 40);
        level("Reach 37 Prayer", Skill.PRAYER, 37);
        in("falador", Tier.HARD);
        level("Reach 70 Prayer (Piety)", Skill.PRAYER, 70);
        visit("Enter the Mining Guild", 3016, 3339, 10);
        kill("Kill the Giant Mole or a dark wizard nearby", "dark wizard", 1);
        level("Reach 70 Slayer", Skill.SLAYER, 72);
        visit("Visit the Warriors' Guild", 2882, 3548, 10);
        make("Craft a body rune", "body_rune", 1, Skill.RUNECRAFT, 20);
        in("falador", Tier.ELITE);
        make("Craft 252 air runes in one trip (craft any air runes)", "air_rune", 252, Skill.RUNECRAFT, 88);
        level("Reach 99 Agility (or 91 with boosts)", Skill.AGILITY, 80);
        level("Reach 91 Farming", Skill.FARMING, 91);
        kill("Defeat 100 monsters in Asgarnia", "", 100);
        level("Reach 94 Herblore", Skill.HERBLORE, 81);

        // ======================= Fremennik =======================
        in("fremennik", Tier.EASY);
        visit("Visit Rellekka", 2660, 3657, 15);
        kill("Kill 5 rock crabs or other creatures near Rellekka", "", 5);
        xp("Catch a cerulean twitch or other creature in the province", Skill.HUNTER, 11);
        xp("Chop and burn some oak logs in the Fremennik Province", Skill.WOODCUTTING, 15);
        visit("Visit Trollheim", 2890, 3676, 15);
        xp("Mine some iron in the Fremennik area", Skill.MINING, 15);
        in("fremennik", Tier.MEDIUM);
        visit("Visit the Lighthouse", 2509, 3634, 15);
        visit("Travel to Miscellania", 2535, 3862, 20);
        kill("Kill a troll", "troll", 1);
        xp("Steal from the Rellekka fish stall", Skill.THIEVING, 42);
        level("Reach 40 Slayer", Skill.SLAYER, 40);
        visit("Visit the Fremennik Slayer Dungeon entrance", 2797, 3615, 12);
        in("fremennik", Tier.HARD);
        visit("Teleport to Trollheim", 2890, 3676, 15);
        cast("Cast a Lunar teleport (Waterbirth)", "waterbirth_teleport");
        kill("Kill 10 trolls", "troll", 10);
        visit("Visit Waterbirth Island", 2546, 3757, 15);
        level("Reach 70 Crafting", Skill.CRAFTING, 61);
        cast("Cast Vengeance", "vengeance");
        in("fremennik", Tier.ELITE);
        kc("Kill each Dagannoth King (any boss) 1 time", "dagannoth_rex", 1);
        level("Reach 85 Slayer", Skill.SLAYER, 83);
        level("Reach 82 Runecraft (astral)", Skill.RUNECRAFT, 82);
        cast("Cast Heal Group or Spellbook Swap", "spellbook_swap");
        visit("Visit the God Wars Dungeon", 2917, 3745, 12);

        // ======================= Kandarin =======================
        in("kandarin", Tier.EASY);
        xp("Catch a mackerel or other fish at Catherby", Skill.FISHING, 16);
        visit("Visit Sherlock's house in Seers' Village", 2733, 3415, 12);
        xp("Pick flax in Seers' Village (any Crafting)", Skill.CRAFTING, 1);
        visit("Visit the Tree Gnome Stronghold", 2461, 3444, 15);
        visit("Visit Camelot castle", 2757, 3477, 15);
        kill("Kill a monster near Camelot", "", 1);
        in("kandarin", Tier.MEDIUM);
        visit("Visit the Ranging Guild", 2655, 3441, 10);
        xp("Complete a lap of the Seers' Village rooftop course", Skill.AGILITY, 60);
        xp("Catch and cook a bass in Catherby", Skill.COOKING, 43);
        visit("Teleport to Camelot", 2757, 3477, 12);
        xp("String a maple shortbow in Seers' Village bank", Skill.FLETCHING, 50);
        make("Make a superantipoison or any potion", "*potion", 1, Skill.HERBLORE, 3);
        in("kandarin", Tier.HARD);
        xp("Catch a leaping sturgeon or shark in Kandarin", Skill.FISHING, 76);
        xp("Chop some yew logs in Seers' Village", Skill.WOODCUTTING, 60);
        visit("Enter the Seers' Village courthouse", 2736, 3469, 10);
        level("Reach 70 Defence", Skill.DEFENCE, 70);
        make("Smith an adamant spear or any adamant item", "*adamant", 1, Skill.SMITHING, 75);
        visit("Visit Barbarian Outpost", 2520, 3571, 15);
        in("kandarin", Tier.ELITE);
        xp("Catch and cook a shark in Catherby", Skill.COOKING, 80);
        make("Craft a magic shortbow or any magic bow", "*magic", 1, Skill.FLETCHING, 85);
        level("Reach 90 Farming", Skill.FARMING, 79);
        level("Reach 99 Firemaking", Skill.FIREMAKING, 85);
        make("Make a stamina mix or any potion with 86 Herblore", "*potion", 1, Skill.HERBLORE, 86);

        // ======================= Karamja =======================
        in("karamja", Tier.EASY);
        visit("Pick a banana in Musa Point plantation", 2914, 3160, 15);
        xp("Use the rocks at Karamja volcano (Mining)", Skill.MINING, 1);
        xp("Catch a fish at Musa Point", Skill.FISHING, 1);
        visit("Visit Brimhaven", 2760, 3178, 15);
        visit("Visit the Karamja Volcano", 2856, 3167, 15);
        visit("Claim a ticket from the Brimhaven Agility Arena", 2809, 3191, 12);
        in("karamja", Tier.MEDIUM);
        visit("Visit Shilo Village", 2852, 2955, 15);
        kill("Kill a jogre", "jogre", 1);
        visit("Use the Brimhaven dungeon entrance", 2744, 3152, 12);
        xp("Catch a karambwan", Skill.FISHING, 65);
        visit("Visit Tai Bwo Wannai", 2789, 3065, 15);
        level("Reach 40 Agility", Skill.AGILITY, 40);
        in("karamja", Tier.HARD);
        visit("Enter the Fight Caves (Mor Ul Rek)", 2845, 3170, 12);
        kill("Kill a metal dragon in Brimhaven Dungeon", "dragon", 1);
        xp("Craft nature runes", Skill.RUNECRAFT, 44);
        kill("Kill a TzHaar or jungle monster", "", 10);
        level("Reach 53 Agility", Skill.AGILITY, 53);
        quest("Complete the Fight Caves", "FIGHT_CAVES");
        in("karamja", Tier.ELITE);
        kc("Defeat TzTok-Jad", "tztok_jad", 1);
        level("Reach 91 Runecraft", Skill.RUNECRAFT, 91);
        level("Reach 87 Herblore", Skill.HERBLORE, 87);
        level("Reach 72 Farming", Skill.FARMING, 72);
        have("Carry a fire cape in Karamja", "fire_cape");

        // ======================= Kourend & Kebos =======================
        in("kourend", Tier.EASY);
        visit("Visit Kourend Castle", 1643, 3673, 20);
        visit("Visit the Arceuus Library", 1632, 3808, 20);
        xp("Mine some iron in Kourend", Skill.MINING, 15);
        kill("Kill a sand crab or creature in Kourend", "", 1);
        visit("Visit the Wintertodt camp", 1630, 3940, 20);
        visit("Visit Shayzien", 1504, 3620, 25);
        in("kourend", Tier.MEDIUM);
        visit("Visit the Woodcutting Guild", 1662, 3505, 15);
        visit("Visit the Farming Guild", 1249, 3718, 15);
        cast("Cast an Arceuus teleport", "arceuus_library_teleport");
        xp("Chop logs in the Woodcutting Guild area", Skill.WOODCUTTING, 60);
        xp("Subdue the Wintertodt (Firemaking in Kourend)", Skill.FIREMAKING, 50);
        level("Reach 49 Agility", Skill.AGILITY, 49);
        in("kourend", Tier.HARD);
        visit("Visit Mount Karuulm", 1311, 3795, 20);
        cast("Reanimate an ensouled head", "expert_reanimation");
        kill("Kill a lizardman shaman or other Kourend creature", "lizardman", 1);
        level("Reach 62 Mining", Skill.MINING, 65);
        cast("Cast Mark of Darkness", "mark_of_darkness");
        level("Reach 74 Woodcutting", Skill.WOODCUTTING, 60);
        in("kourend", Tier.ELITE);
        cast("Cast Resurrect Greater Ghost", "resurrect_greater_ghost");
        cast("Reanimate a master ensouled head", "master_reanimation");
        level("Reach 95 Slayer", Skill.SLAYER, 95);
        level("Reach 90 Woodcutting", Skill.WOODCUTTING, 90);
        clue("Complete a master clue scroll", "master");

        // ======================= Lumbridge & Draynor =======================
        in("lumbridge", Tier.EASY);
        visit("Visit Lumbridge Castle", 3222, 3218, 12);
        kill("Kill a cow in Lumbridge", "cow", 1);
        xp("Fish some shrimp in Lumbridge Swamp", Skill.FISHING, 1);
        xp("Chop and burn some logs near Lumbridge", Skill.FIREMAKING, 1);
        quest("Complete Cook's Assistant", "COOKS_ASSISTANT");
        quest("Complete The Restless Ghost", "RESTLESS_GHOST");
        visit("Visit Draynor Village", 3093, 3244, 15);
        make("Craft some water runes", "water_rune", 1, Skill.RUNECRAFT, 5);
        in("lumbridge", Tier.MEDIUM);
        visit("Visit the Wizards' Tower", 3109, 3162, 15);
        visit("Teleport to Lumbridge", 3222, 3218, 12);
        kill("Kill a zombie in the Draynor Sewers", "zombie", 1);
        quest("Complete Rune Mysteries", "RUNE_MYSTERIES");
        xp("Catch some salmon in Lumbridge", Skill.FISHING, 30);
        level("Reach 40 Ranged", Skill.RANGED, 30);
        in("lumbridge", Tier.HARD);
        visit("Visit Draynor Manor", 3109, 3353, 12);
        cast("Cast Bones to Peaches", "bones_to_peaches");
        level("Reach 70 Agility", Skill.AGILITY, 46);
        make("Craft a cosmic rune or any rune", "*rune", 1, Skill.RUNECRAFT, 59);
        level("Reach 52 Prayer", Skill.PRAYER, 52);
        kill("Kill a monster in Lumbridge Swamp", "", 10);
        in("lumbridge", Tier.ELITE);
        make("Craft 140 water runes", "water_rune", 140, Skill.RUNECRAFT, 76);
        level("Reach 70 Ranged", Skill.RANGED, 70);
        level("Reach 88 Smithing", Skill.SMITHING, 88);
        level("Reach 76 Woodcutting", Skill.WOODCUTTING, 75);
        quest("Complete Demon Slayer", "DEMON_SLAYER");

        // ======================= Morytania =======================
        in("morytania", Tier.EASY);
        visit("Visit Canifis", 3496, 3488, 15);
        kill("Kill a ghoul", "ghoul", 1);
        visit("Visit the Slayer Tower", 3422, 3537, 12);
        visit("Visit Port Phasmatys", 3687, 3502, 15);
        xp("Craft or fletch something in Morytania", Skill.CRAFTING, 1);
        quest("Complete Priest in Peril", "priest_in_peril");
        in("morytania", Tier.MEDIUM);
        xp("Complete a lap of the Canifis rooftop course", Skill.AGILITY, 40);
        visit("Visit the Ectofuntus", 3660, 3522, 12);
        visit("Visit Mort'ton", 3489, 3288, 15);
        kill("Kill a werewolf", "werewolf", 1);
        cast("Cast Salve Graveyard Teleport", "salve_graveyard_teleport");
        level("Reach 42 Slayer", Skill.SLAYER, 42);
        in("morytania", Tier.HARD);
        visit("Enter the Barrows", 3565, 3288, 15);
        kill("Kill a Barrows brother or monster at Barrows", "", 5);
        cast("Cast Barrows Teleport", "barrows_teleport");
        level("Reach 71 Firemaking", Skill.FIREMAKING, 50);
        visit("Visit Darkmeyer", 3592, 3337, 15);
        level("Reach 70 Slayer", Skill.SLAYER, 66);
        in("morytania", Tier.ELITE);
        kill("Kill an abyssal demon in the Slayer Tower", "abyssal demon", 1);
        level("Reach 85 Slayer", Skill.SLAYER, 85);
        level("Reach 90 Prayer", Skill.PRAYER, 70);
        level("Reach 91 Fishing", Skill.FISHING, 96);
        kc("Kill Barrows brothers 25 times", "barrows", 25);

        // ======================= Varrock =======================
        in("varrock", Tier.EASY);
        visit("Enter the Varrock Palace", 3213, 3474, 12);
        xp("Mine some iron in the south-east Varrock mine", Skill.MINING, 15);
        visit("Visit the Grand Exchange", 3164, 3487, 12);
        kill("Kill a guard or rat in Varrock", "", 1);
        quest("Complete Romeo & Juliet", "ROMEO_AND_JULIET");
        make("Spin a bowl or craft anything (any Crafting)", "*", 1, Skill.CRAFTING, 1);
        visit("Teleport to the Lumberyard or visit it", 3302, 3487, 15);
        in("varrock", Tier.MEDIUM);
        add("Make a trade on the Grand Exchange", Type.GE, "trade", 1, null, 0);
        visit("Teleport to Varrock", 3213, 3424, 12);
        visit("Visit the Champions' Guild", 3191, 3360, 10);
        xp("Pickpocket a guard in Varrock", Skill.THIEVING, 40);
        kill("Kill 5 monsters in Varrock", "", 5);
        quest("Complete Demon Slayer", "DEMON_SLAYER");
        in("varrock", Tier.HARD);
        xp("Chop yew logs near Varrock Palace", Skill.WOODCUTTING, 60);
        cast("Cast Paddewwa Teleport", "paddewwa_teleport");
        visit("Visit Edgeville Monastery", 3052, 3488, 12);
        level("Reach 64 Prayer", Skill.PRAYER, 52);
        make("Smith an adamant platebody", "adamant_platebody", 1, Skill.SMITHING, 88);
        level("Reach 81 Farming", Skill.FARMING, 68);
        in("varrock", Tier.ELITE);
        make("Smith a runite platebody", "rune_platebody", 1, Skill.SMITHING, 99);
        level("Reach 86 Herblore", Skill.HERBLORE, 90);
        level("Reach 78 Runecraft", Skill.RUNECRAFT, 78);
        level("Reach 81 Fletching", Skill.FLETCHING, 81);
        add("Make 10 trades on the Grand Exchange", Type.GE, "trade", 10, null, 0);

        // ======================= Western Provinces =======================
        in("western", Tier.EASY);
        visit("Visit the Tree Gnome Stronghold", 2461, 3444, 15);
        xp("Complete a lap of the Gnome Agility Course", Skill.AGILITY, 1);
        visit("Visit the Feldip Hills", 2540, 2925, 30);
        xp("Catch a bird in the Feldip Hills", Skill.HUNTER, 9);
        kill("Kill a monster in the Western Provinces", "", 1);
        visit("Visit the Grand Tree", 2465, 3495, 15);
        in("western", Tier.MEDIUM);
        visit("Visit the Outpost", 2430, 3348, 15);
        visit("Travel to Ape Atoll", 2770, 2703, 25);
        xp("Chop some teak or other logs in the west", Skill.WOODCUTTING, 35);
        xp("Catch a chinchompa in the Feldip Hills", Skill.HUNTER, 53);
        level("Reach 37 Agility", Skill.AGILITY, 37);
        visit("Visit Piscatoris Fishing Colony", 2340, 3650, 20);
        in("western", Tier.HARD);
        visit("Visit Lletya", 2340, 3170, 15);
        visit("Visit Zul-Andra", 2197, 3055, 20);
        kill("Kill an elf or other Tirannwn creature", "elf", 1);
        level("Reach 70 Ranged", Skill.RANGED, 70);
        xp("Catch a red chinchompa", Skill.HUNTER, 63);
        level("Reach 75 Construction", Skill.CONSTRUCTION, 65);
        in("western", Tier.ELITE);
        visit("Visit Prifddinas (Elven city)", 2240, 3320, 30);
        level("Reach 85 Thieving", Skill.THIEVING, 85);
        level("Reach 75 Fletching", Skill.FLETCHING, 75);
        level("Reach 93 Magic", Skill.MAGIC, 93);
        kc("Defeat Zulrah", "zulrah", 1);

        // ======================= Wilderness =======================
        in("wilderness", Tier.EASY);
        visit("Enter the Wilderness", 3090, 3525, 20);
        visit("Visit the Chaos Temple", 3236, 3635, 12);
        kill("Kill a monster in the Wilderness", "", 1);
        xp("Mine some iron in the Wilderness", Skill.MINING, 15);
        visit("Visit the Ferox Enclave", 3151, 3635, 15);
        visit("Enter the Wilderness Mind Altar area", 2982, 3514, 12);
        in("wilderness", Tier.MEDIUM);
        visit("Visit the Bandit Camp", 3038, 3651, 15);
        kill("Kill a green dragon", "green dragon", 1);
        xp("Mine some mithril in the Wilderness", Skill.MINING, 55);
        visit("Enter the Wilderness God Wars Dungeon", 3017, 3738, 12);
        cast("Cast Carrallanger Teleport", "carrallanger_teleport");
        level("Reach 60 Woodcutting", Skill.WOODCUTTING, 60);
        in("wilderness", Tier.HARD);
        visit("Visit the Lava Maze", 3028, 3842, 15);
        visit("Visit the Mage Arena", 3105, 3934, 15);
        kill("Kill a lava dragon or other deep Wilderness monster", "dragon", 3);
        xp("Catch a black salamander in the Wilderness", Skill.HUNTER, 67);
        cast("Cast Ghorrock Teleport", "ghorrock_teleport");
        level("Reach 75 Slayer", Skill.SLAYER, 68);
        in("wilderness", Tier.ELITE);
        visit("Visit the Fountain of Rune", 3375, 3893, 10);
        kc("Kill Callisto, Venenatis or Vet'ion", "callisto", 1);
        xp("Fish and cook a dark crab in the Wilderness Resource Area", Skill.COOKING, 90);
        level("Reach 96 Thieving", Skill.THIEVING, 84);
        kill("Kill a spiritual mage in the Wilderness God Wars Dungeon", "spiritual mage", 1);
    }

    private Diaries() {
    }

    public static List<Task> tasks() {
        return Collections.unmodifiableList(TASKS);
    }

    public static Task task(String id) {
        return BY_ID.get(id);
    }

    public static List<Task> tasks(String areaId, Tier t) {
        List<Task> out = new ArrayList<>();
        for (Task task : TASKS) {
            if (task.area().equals(areaId) && task.tier() == t) {
                out.add(task);
            }
        }
        return out;
    }

    public static Area area(String id) {
        for (Area a : AREAS) {
            if (a.id().equals(id)) {
                return a;
            }
        }
        return null;
    }

    /** Whether a set of completed task ids completes an area's tier. */
    public static boolean tierComplete(java.util.Set<String> done, String areaId, Tier t) {
        for (Task task : tasks(areaId, t)) {
            if (!done.contains(task.id())) {
                return false;
            }
        }
        return true;
    }

    /** Whether every elite diary is complete (achievement diary cape). */
    public static boolean allElite(java.util.Set<String> done) {
        for (Area a : AREAS) {
            for (Tier t : Tier.values()) {
                if (!tierComplete(done, a.id(), t)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Item id match for MAKE/HAVE tasks: exact, '*x' (contains x) or '*' (anything). */
    public static boolean itemMatches(String target, String item) {
        if (target.equals("*")) {
            return true;
        }
        if (target.startsWith("*")) {
            return item.contains(target.substring(1));
        }
        return target.equals(item);
    }

    /** Parses a VISIT target "x,y,r". */
    public static int[] visitTarget(String target) {
        String[] p = target.split(",");
        return new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])};
    }
}
