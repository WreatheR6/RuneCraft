package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.Skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.runecraft.osrs.Skill.AGILITY;
import static com.runecraft.osrs.Skill.ATTACK;
import static com.runecraft.osrs.Skill.CONSTRUCTION;
import static com.runecraft.osrs.Skill.COOKING;
import static com.runecraft.osrs.Skill.CRAFTING;
import static com.runecraft.osrs.Skill.DEFENCE;
import static com.runecraft.osrs.Skill.FARMING;
import static com.runecraft.osrs.Skill.FIREMAKING;
import static com.runecraft.osrs.Skill.FISHING;
import static com.runecraft.osrs.Skill.FLETCHING;
import static com.runecraft.osrs.Skill.HERBLORE;
import static com.runecraft.osrs.Skill.HITPOINTS;
import static com.runecraft.osrs.Skill.HUNTER;
import static com.runecraft.osrs.Skill.MAGIC;
import static com.runecraft.osrs.Skill.MINING;
import static com.runecraft.osrs.Skill.PRAYER;
import static com.runecraft.osrs.Skill.RANGED;
import static com.runecraft.osrs.Skill.RUNECRAFT;
import static com.runecraft.osrs.Skill.SLAYER;
import static com.runecraft.osrs.Skill.SMITHING;
import static com.runecraft.osrs.Skill.STRENGTH;
import static com.runecraft.osrs.Skill.THIEVING;
import static com.runecraft.osrs.Skill.WOODCUTTING;

/**
 * Achievement diaries: the twelve OSRS areas with their easy/medium/hard/elite task lists. Task names and
 * skill/quest requirements follow the OSRS wiki; each task is mapped to something the game can observe
 * (levels, XP gained somewhere, kills, items made/obtained/carried, quests, places visited, boss kill
 * counts, spells cast, clues, Grand Exchange trades). Tasks whose OSRS mechanic doesn't exist here
 * (minigame internals, emotes, NPC services, PvP) are mapped to the nearest observable action and
 * marked {@code // approx}.
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

    /** Types completed by periodic state checks (levels, items held, quests, places, kill counts...). */
    public static final List<Type> PERIODIC_TYPES = List.of(Type.LEVEL, Type.HAVE, Type.QUEST, Type.VISIT, Type.KC, Type.GE, Type.CLUE,
            Type.CAST, Type.TOTAL_LEVEL, Type.COMBAT_LEVEL, Type.QUEST_POINTS, Type.SKILLCAPE);

    /** Types completed by game events (kills, XP drops, makes, obtains, casts, clues, GE trades). */
    public static final List<Type> EVENT_TYPES = List.of(Type.XP_IN, Type.KILL, Type.MAKE, Type.OBTAIN, Type.CAST, Type.CLUE, Type.GE);

    public enum Type {
        LEVEL, XP_IN, KILL, MAKE, HAVE, QUEST, VISIT, CLUE, GE, KC, CAST, TOTAL_LEVEL,
        /** Receive an item from a skilling action, drop or reward. */
        OBTAIN,
        /** Stand at a place (VISIT target) having 99 in any skill (skillcape emotes). */
        SKILLCAPE,
        COMBAT_LEVEL,
        /** Quest points; level 0 means "every quest complete" (quest point cape). */
        QUEST_POINTS
    }

    /**
     * A task. {@code target} is a skill name (LEVEL/XP_IN), monster keyword(s) (KILL, '|' separates
     * alternatives), item id(s) (MAKE/HAVE/OBTAIN, a leading '*' matches any id containing the rest, '|'
     * separates alternatives), quest id, place "x,y,r" (VISIT/SKILLCAPE), clue tier, boss id(s) (KC: '|' sums
     * alternatives, '&' needs each) or spell id(s) (CAST, '|' alternatives). {@code restrict} is where the
     * action must happen: an area id, an "x,y,r" circle or null (anywhere). {@code reqs} are the skill levels
     * OSRS requires for the task (checked when it completes).
     */
    public record Task(String id, String area, Tier tier, String description, Type type, String target, int count,
                       String restrict, int level, Map<Skill, Integer> reqs) {
        /** Every skill level this task needs, including a LEVEL/XP_IN target. */
        public Map<Skill, Integer> allReqs() {
            Map<Skill, Integer> all = new EnumMap<>(Skill.class);
            all.putAll(reqs);
            if ((type == Type.LEVEL || type == Type.XP_IN) && level > 0) {
                all.merge(Skill.valueOf(target), level, Math::max);
            }
            return all;
        }
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

    private static Map<Skill, Integer> reqs(Object... pairs) {
        Map<Skill, Integer> m = new EnumMap<>(Skill.class);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            m.put((Skill) pairs[i], (Integer) pairs[i + 1]);
        }
        return Collections.unmodifiableMap(m);
    }

    /** Next task index per "area_tier" (ids are saved in player progress, so they never shift). */
    private static final Map<String, Integer> NEXT_INDEX = new HashMap<>();
    /** Tasks whose trigger is a stand-in for an OSRS mechanic the game doesn't have. */
    private static final Set<String> APPROX = new HashSet<>();
    /** Quests (ids) a task needs completed, from the OSRS wiki task tables. */
    private static final Map<String, List<String>> QUEST_REQS = new HashMap<>();
    private static String lastId;

    private static void add(String desc, Type type, String target, int count, String restrict, int level, Object... req) {
        String key = area + "_" + tier.id();
        String id = key + "_" + (NEXT_INDEX.merge(key, 1, Integer::sum) - 1);
        Task task = new Task(id, area, tier, desc, type, target, count, restrict, level, reqs(req));
        TASKS.add(task);
        BY_ID.put(id, task);
        lastId = id;
    }

    /** Skips ids of tasks OSRS removed from a tier, so the following ids keep their saved meaning. */
    private static void retire(int n) {
        NEXT_INDEX.merge(area + "_" + tier.id(), n, Integer::sum);
    }

    /** Marks the task just added as a stand-in (its trigger only approximates the OSRS task). */
    private static void approx() {
        APPROX.add(lastId);
    }

    private static void needs(String taskId, String... quests) {
        QUEST_REQS.put(taskId, List.of(quests));
    }

    private static void level(String desc, Skill s, int lvl) {
        add(desc, Type.LEVEL, s.name(), 1, null, lvl);
    }

    /** Gain XP in a skill inside the diary area with at least the given level. */
    private static void xp(String desc, Skill s, int lvl, Object... req) {
        add(desc, Type.XP_IN, s.name(), 1, area, lvl, req);
    }

    /** Gain XP in a skill inside an "x,y,r" circle (or anywhere if null). */
    private static void xpAt(String desc, Skill s, int lvl, String place, Object... req) {
        add(desc, Type.XP_IN, s.name(), 1, place, lvl, req);
    }

    private static void kill(String desc, String keyword, int n, Object... req) {
        add(desc, Type.KILL, keyword, n, area, 0, req);
    }

    /** Kill inside an "x,y,r" circle. */
    private static void killAt(String desc, String keyword, int n, String place, Object... req) {
        add(desc, Type.KILL, keyword, n, place, 0, req);
    }

    private static void killAny(String desc, String keyword, int n, Object... req) {
        add(desc, Type.KILL, keyword, n, null, 0, req);
    }

    /** Make an item through a make menu (anywhere). */
    private static void make(String desc, String item, int n, Object... req) {
        add(desc, Type.MAKE, item, n, null, 0, req);
    }

    /** Make an item inside the diary area. */
    private static void makeHere(String desc, String item, int n, Object... req) {
        add(desc, Type.MAKE, item, n, area, 0, req);
    }

    private static void makeAt(String desc, String item, int n, String place, Object... req) {
        add(desc, Type.MAKE, item, n, place, 0, req);
    }

    /** Obtain an item (gathered, stolen, dropped, crafted...) inside the diary area. */
    private static void get(String desc, String item, int n, Object... req) {
        add(desc, Type.OBTAIN, item, n, area, 0, req);
    }

    private static void getAt(String desc, String item, int n, String place, Object... req) {
        add(desc, Type.OBTAIN, item, n, place, 0, req);
    }

    private static void have(String desc, String item, Object... req) {
        add(desc, Type.HAVE, item, 1, area, 0, req);
    }

    private static void haveAny(String desc, String item, Object... req) {
        add(desc, Type.HAVE, item, 1, null, 0, req);
    }

    private static void quest(String desc, String questId, Object... req) {
        add(desc, Type.QUEST, questId, 1, null, 0, req);
    }

    private static void visit(String desc, int x, int y, int r, Object... req) {
        add(desc, Type.VISIT, x + "," + y + "," + r, 1, null, 0, req);
    }

    private static void clue(String desc, String clueTier) {
        add(desc, Type.CLUE, clueTier, 1, null, 0);
    }

    private static void kc(String desc, String boss, int n, Object... req) {
        add(desc, Type.KC, boss, n, null, 0, req);
    }

    private static void cast(String desc, String spell, Object... req) {
        add(desc, Type.CAST, spell, 1, null, 0, req);
    }

    /** Cast a spell inside a restriction (an area id or an "x,y,r" circle). */
    private static void castAt(String desc, String spell, String place, Object... req) {
        add(desc, Type.CAST, spell, 1, place, 0, req);
    }

    private static void combat(String desc, int lvl, Object... req) {
        add(desc, Type.COMBAT_LEVEL, "combat", 1, null, lvl, req);
    }

    private static void skillcape(String desc, int x, int y, int r) {
        add(desc, Type.SKILLCAPE, x + "," + y + "," + r, 1, null, 0);
    }

    private static String at(int x, int y, int r) {
        return x + "," + y + "," + r;
    }

    static {
        // ======================= Ardougne (10 / 12 / 12 / 8) =======================
        in("ardougne", Tier.EASY);
        visit("Have Wizard Cromperty teleport you to the Rune essence mine", 2683, 3325, 8); approx(); // approx
        get("Steal a cake from the East Ardougne market stalls", "cake", 1, THIEVING, 5);
        get("Sell silk to the Silk trader in East Ardougne for 60 coins each", "silk", 1); approx(); // approx: steal silk
        visit("Use the altar in East Ardougne's church", 2616, 3307, 8);
        visit("Go out fishing on the Fishing Trawler", 2675, 3170, 12); approx(); // approx
        visit("Enter the Combat Training Camp north of West Ardougne", 2518, 3356, 12);
        visit("Have Tindel Marchant identify a rusty sword for you", 2678, 3152, 10); approx(); // approx
        visit("Use the Ardougne lever to teleport to the Wilderness", 3153, 3923, 12);
        visit("View Aleck's Hunter Emporium in Yanille", 2566, 3083, 8);
        visit("Check what pets you have insured with Probita in East Ardougne", 2621, 3294, 8);
        in("ardougne", Tier.MEDIUM);
        visit("Enter the unicorn pen in Ardougne Zoo using Fairy rings", 2610, 3270, 8);
        visit("Grapple over Yanille's south wall", 2556, 3072, 10, AGILITY, 39, STRENGTH, 38, RANGED, 21);
        get("Harvest some strawberries from the Ardougne farming patch", "strawberry", 1, FARMING, 31);
        cast("Cast the Ardougne Teleport spell", "ardougne_teleport", MAGIC, 51);
        visit("Travel to Castle Wars by Hot Air Balloon", 2442, 3090, 15, FIREMAKING, 50);
        quest("Claim buckets of sand from Bert in Yanille", "the_hand_in_the_sand", CRAFTING, 49); approx(); // approx
        visit("Catch any fish on the Fishing Platform", 2725, 3285, 15); approx(); // approx: Witchaven shore
        xp("Pickpocket the master farmer north of East Ardougne", THIEVING, 38);
        visit("Collect some Cave nightshade from the Skavid caves", 2528, 3034, 15); approx(); // approx
        quest("Kill a swordchick in the Tower of Life", "tower_of_life"); approx(); // approx
        haveAny("Equip an Iban's upgraded staff or upgrade an Iban's staff", "ibans_staff", MAGIC, 50);
        visit("Visit the Island east of the Necromancer's tower", 2700, 3247, 10);
        in("ardougne", Tier.HARD);
        visit("Recharge some jewellery at the Totem pole in the Legends' Guild", 2729, 3378, 12); approx(); // approx
        visit("Enter the Magic Guild", 2590, 3087, 8, MAGIC, 66);
        xp("Attempt to steal from a chest in Ardougne Castle", THIEVING, 72);
        quest("Have a zookeeper put you in Ardougne Zoo's monkey cage", "monkey_madness_i"); approx(); // approx
        cast("Teleport to the Watchtower", "watchtower_teleport", MAGIC, 58);
        xp("Catch a Red Salamander", HUNTER, 59);
        xpAt("Check the health of a palm tree near Tree Gnome Village", FARMING, 68, null); approx(); // approx
        get("Pick some poison ivy berries from the patch south of Ardougne", "poison_ivy_berries", 1, FARMING, 70);
        makeHere("Smith a Mithril platebody near Ardougne", "mithril_platebody", 1, SMITHING, 68);
        visit("Enter your POH from Yanille", 2544, 3095, 10, CONSTRUCTION, 50);
        makeHere("Smith a Dragon sq shield in West Ardougne", "dragon_sq_shield", 1, SMITHING, 60);
        getAt("Craft some Death runes", "death_rune", 1, null, RUNECRAFT, 65);
        in("ardougne", Tier.ELITE);
        makeHere("Catch a manta ray in the Fishing Trawler and cook it in Port Khazard", "manta_ray", 1, FISHING, 81, COOKING, 91);
        visit("Attempt to picklock the door to the basement of Yanille Agility Dungeon", 2569, 3122, 10, THIEVING, 82);
        xp("Pickpocket a Hero", THIEVING, 80);
        makeHere("Make a rune crossbow yourself from scratch within Witchaven or Yanille", "rune_crossbow", 1, CRAFTING, 10,
                SMITHING, 91, FLETCHING, 69);
        haveAny("Imbue a salve amulet at Nightmare Zone", "salve_amulet_i|salve_amulet_ei");
        get("Pick some torstol from the patch north of East Ardougne", "grimy_torstol", 1, FARMING, 85);
        xp("Complete a lap of Ardougne's rooftop agility course", AGILITY, 90);
        // Castle Wars has player-vs-player magic and no monsters, so a barrage cast there is cast on a player.
        castAt("Cast Ice Barrage on another player within Castle Wars", "ice_barrage", at(2440, 3090, 30), MAGIC, 94);

        // ======================= Desert (11 / 12 / 10 / 6) =======================
        in("desert", Tier.EASY);
        xp("Catch a Golden Warbler", HUNTER, 5);
        get("Mine 5 clay in the north-eastern desert", "clay", 5);
        visit("Enter the Kalphite Lair", 3226, 3108, 12);
        visit("Enter the Desert with a set of desert robes equipped", 3304, 3115, 15); approx(); // approx: Shantay Pass
        kill("Kill a Vulture", "vulture", 1);
        visit("Have the Nardah herbalist (Zahur) clean a herb for you", 3424, 2908, 8);
        kill("Collect 5 potato cacti from the Kalphite Hive", "kalphite", 5); approx(); // approx
        visit("Sell some artefacts to Simon Templeton", 3346, 2827, 10);
        visit("Open the sarcophagus in the first room of Pyramid Plunder", 3289, 2800, 15, THIEVING, 21);
        have("Cut a desert cactus open to fill a waterskin", "*waterskin|vial_of_water");
        visit("Travel from the Shantay Pass to Pollnivneach by magic carpet", 3349, 3003, 12);
        in("desert", Tier.MEDIUM);
        xpAt("Climb to the summit of the Agility Pyramid", AGILITY, 30, at(3354, 2830, 30));
        kill("Slay a desert lizard", "desert lizard", 1, SLAYER, 22);
        xp("Catch an Orange Salamander", HUNTER, 47);
        xp("Steal a feather from the Desert Phoenix", THIEVING, 25); approx(); // approx
        visit("Travel to Uzer via magic carpet", 3468, 3110, 15);
        quest("Travel to the Desert via the Eagle transport system", "eagles_peak");
        visit("Pray at the Elidinis statuette in Nardah", 3427, 2930, 8);
        makeHere("Create a combat potion in the desert", "combat_potion", 1, HERBLORE, 36);
        quest("Teleport to Enakhra's Temple with the Camulet", "enakhras_lament"); approx(); // approx
        quest("Visit the genie", "spirits_of_the_elid"); approx(); // approx
        visit("Teleport to Pollnivneach with a redirected teleport to house tablet", 3359, 2970, 15, CONSTRUCTION, 20);
        get("Chop some Teak logs near Uzer", "teak_logs", 1, WOODCUTTING, 35);
        in("desert", Tier.HARD);
        xp("Knock out and pickpocket a Menaphite Thug", THIEVING, 65);
        get("Mine some granite", "*granite", 1, MINING, 45);
        castAt("Refill your waterskins in the Desert using Lunar magic", "humidify", "desert", MAGIC, 68);
        kc("Kill the Kalphite Queen", "kalphite_queen", 1);
        xpAt("Complete a lap of the Pollnivneach agility course", AGILITY, 70, at(3359, 2970, 40));
        killAny("Slay a Dust Devil in the desert cave with a Slayer helmet equipped", "dust devil", 1, SLAYER, 65, DEFENCE, 10);
        visit("Activate Ancient Magicks at the altar in the Jaldraocht Pyramid", 3233, 2898, 10); approx(); // approx
        quest("Defeat a Locust Rider with Keris", "contact", ATTACK, 50); approx(); // approx
        xpAt("Burn some yew logs on the Nardah Mayor's balcony", FIREMAKING, 60, at(3428, 2892, 25));
        makeAt("Create a Mithril Platebody in Nardah", "mithril_platebody", 1, at(3428, 2892, 25), SMITHING, 68);
        in("desert", Tier.ELITE);
        makeAt("Bake a wild pie at the Nardah Clay Oven", "wild_pie", 1, at(3428, 2892, 25), COOKING, 85);
        castAt("Cast Ice Barrage against a foe in the Desert", "ice_barrage", "desert", MAGIC, 94);
        makeAt("Fletch some Dragon darts at the Bedabin Camp", "dragon_dart", 10, at(3171, 3034, 20), FLETCHING, 95);
        haveAny("Speak to the KQ head in your POH", "kq_head", CONSTRUCTION, 78); approx(); // approx
        xpAt("Steal from the Grand Gold Chest in the final room of Pyramid Plunder", THIEVING, 91, at(3289, 2800, 30));
        visit("Restore at least 85 Prayer points when praying at the altar in Sophanem", 3281, 2774, 10, PRAYER, 85);

        // ======================= Falador (11 / 14 / 11 / 6) =======================
        in("falador", Tier.EASY);
        visit("Find out what your family crest is from Sir Renitee", 2982, 3341, 12, CONSTRUCTION, 16);
        visit("Climb over the western Falador wall", 2936, 3355, 8, AGILITY, 5);
        visit("Browse Sarah's Farming Shop", 3039, 3292, 8);
        visit("Get a Haircut from the Falador hairdresser", 2944, 3380, 8);
        have("Fill a bucket from the pump north of Falador West Bank", "*vial_of_water|bucket_of_water"); approx(); // approx
        killAt("Kill a duck in Falador Park", "duck", 1, at(2986, 3370, 20));
        // Tiaras are bound at the altar (an obtain event), not made through a make menu.
        getAt("Make a Mind Tiara", "mind_tiara", 1, null);
        visit("Take the boat to Entrana", 2834, 3335, 15);
        xp("Repair a broken strut in the Motherlode Mine", MINING, 30); approx(); // approx
        visit("Claim a security book from the Security Guard upstairs at Port Sarim jail", 3013, 3192, 8);
        makeHere("Smith some Blurite Limbs on Doric's Anvil", "blurite_limbs", 1, MINING, 10, SMITHING, 13);
        in("falador", Tier.MEDIUM);
        xpAt("Light a Bullseye lantern at the Chemist's in Rimmington", FIREMAKING, 49, at(2932, 3210, 20));
        castAt("Telegrab some Wine of Zamorak at the Chaos Temple by the Wilderness", "telekinetic_grab", at(2935, 3516, 12), MAGIC, 33);
        visit("Unlock the crystal chest in Taverley", 2914, 3452, 8); approx(); // approx
        xp("Place a Scarecrow in the Falador farm flower patch", FARMING, 23);
        killAny("Kill a Mogre at Mudskipper Point", "mogre", 1, SLAYER, 32);
        visit("Visit the Port Sarim Rat Pits", 3017, 3233, 8);
        visit("Grapple up and then jump off the north Falador wall", 3033, 3389, 8, AGILITY, 11, STRENGTH, 37, RANGED, 19);
        xp("Pickpocket a Falador guard", THIEVING, 40);
        visit("Pray at the Altar of Guthix in Taverley whilst wearing full Initiate", 2925, 3483, 10, PRAYER, 10, DEFENCE, 20);
        get("Mine some Gold ore at the Crafting Guild", "gold_ore", 1, MINING, 40, CRAFTING, 40);
        visit("Squeeze through the crevice in the Dwarven mines", 3016, 3450, 12, AGILITY, 42);
        getAt("Chop and burn some Willow logs in Taverley", "willow_logs", 1, at(2895, 3445, 50), WOODCUTTING, 30, FIREMAKING, 30);
        makeHere("Craft a basket on the Falador farm loom", "basket", 1, CRAFTING, 36);
        cast("Teleport to the Falador Party Room", "falador_teleport", MAGIC, 37);
        in("falador", Tier.HARD);
        getAt("Craft 140 Mind runes simultaneously", "mind_rune", 1, null, RUNECRAFT, 56); approx(); // approx count
        add("Change your family crest to the Saradomin symbol", Type.LEVEL, "PRAYER", 1, null, 70, CONSTRUCTION, 16);
        kc("Kill the Giant Mole beneath Falador Park", "giant_mole", 1);
        killAny("Kill a Skeletal Wyvern in the Asgarnia Ice Dungeon", "skeletal wyvern", 1, SLAYER, 72);
        xp("Complete a lap of the Falador rooftop agility course", AGILITY, 50);
        visit("Enter the Mining Guild wearing full Prospector", 3016, 3339, 8, MINING, 60);
        killAny("Kill the Blue Dragon under the Heroes' Guild", "blue dragon", 1);
        visit("Crack a wall safe within Rogues' Den", 2906, 3537, 8, THIEVING, 50);
        visit("Recharge your prayer in the Port Sarim church while wearing full Proselyte", 3023, 3208, 15, DEFENCE, 30, PRAYER, 20);
        visit("Enter the Warriors' Guild", 2877, 3546, 8, ATTACK, 65, STRENGTH, 65); // approx reqs: 130 combined
        quest("Equip a dwarven helmet within the Dwarven mines", "grim_tales", DEFENCE, 50); approx(); // approx
        in("falador", Tier.ELITE);
        getAt("Craft 252 Air Runes simultaneously from Essence without the use of Extracts", "air_rune", 1, null, RUNECRAFT, 88);
        quest("Purchase a White 2h Sword from Sir Vyvin", "wanted"); approx(); // approx
        get("Find at least 3 magic roots at once when digging up your magic tree in Falador", "magic_roots", 1, FARMING, 91, WOODCUTTING, 75);
        skillcape("Perform a skillcape or quest cape emote at the top of Falador Castle", 2965, 3340, 12);
        visit("Jump over the strange floor in Taverley dungeon", 2884, 3397, 12, AGILITY, 80);
        makeHere("Mix a Saradomin brew in Falador east bank", "saradomin_brew", 1, HERBLORE, 81);

        // ======================= Fremennik (10 / 9 / 9 / 6) =======================
        in("fremennik", Tier.EASY);
        xp("Catch a Cerulean twitch", HUNTER, 11);
        visit("Change your boots at Yrsa's Shoe Store", 2625, 3675, 8);
        kill("Kill 5 Rock crabs", "rock crab", 5);
        makeHere("Craft a tiara from scratch in Rellekka", "tiara", 1, CRAFTING, 23, MINING, 20, SMITHING, 20);
        quest("Browse the Stonemasons shop", "the_giant_dwarf"); approx(); // approx: Keldagrim
        visit("Collect a Snape grass on Waterbirth Island", 2546, 3757, 15);
        quest("Steal from the Keldagrim crafting or baker's stall", "the_giant_dwarf", THIEVING, 5); approx(); // approx
        have("Fill a bucket with water at the Rellekka well", "*vial_of_water|bucket_of_water"); approx(); // approx
        visit("Enter the Troll Stronghold", 2839, 3690, 12);
        get("Chop and burn some oak logs in the Fremennik Province", "oak_logs", 1, WOODCUTTING, 15, FIREMAKING, 15);
        in("fremennik", Tier.MEDIUM);
        killAny("Slay a Brine rat", "brine rat", 1, SLAYER, 47);
        quest("Travel to the Snowy Hunter Area via Eagle", "eagles_peak");
        get("Mine some coal in Rellekka", "coal", 1, MINING, 30);
        xp("Steal from the Rellekka Fish stall", THIEVING, 42);
        visit("Travel to Miscellania by Fairy ring", 2513, 3884, 15);
        xp("Catch a Snowy knight", HUNTER, 35);
        level("Pick up your Pet Rock from your POH Menagerie", CONSTRUCTION, 37); approx(); // approx
        visit("Visit the Lighthouse from Waterbirth island", 2509, 3634, 12);
        quest("Mine some gold at the Arzinian mine", "between_a_rock", MINING, 40, DEFENCE, 30); approx(); // approx
        in("fremennik", Tier.HARD);
        cast("Teleport to Trollheim", "trollheim_teleport", MAGIC, 61);
        xp("Catch a Sabre-toothed Kyatt", HUNTER, 55);
        makeHere("Mix a Super defence potion in the Fremennik province", "super_defence", 1, HERBLORE, 66);
        quest("Steal from the Keldagrim Gem Stall", "the_giant_dwarf", THIEVING, 75); approx(); // approx
        quest("Craft a Fremennik shield on Neitiznot", "the_fremennik_isles", WOODCUTTING, 56); approx(); // approx
        getAt("Mine 5 adamantite ores on Jatizso", "adamantite_ore", 5, null, MINING, 70); approx(); // approx: anywhere
        level("Obtain the Blast Furnace Foreman's permission to use the Blast Furnace for free", SMITHING, 60);
        cast("Teleport to Waterbirth Island", "waterbirth_teleport", MAGIC, 72);
        visit("Obtain 100% support from your kingdom subjects", 2502, 3860, 15); approx(); // approx: visit Miscellania castle
        in("fremennik", Tier.ELITE);
        kc("Kill each of the Dagannoth Kings", "dagannoth_rex&dagannoth_prime&dagannoth_supreme", 1);
        getAt("Craft 56 astral runes simultaneously from Essence", "astral_rune", 1, null, RUNECRAFT, 82);
        makeHere("Create a dragonstone amulet in the Neitiznot furnace", "dragonstone_amulet_u|dragonstone_amulet", 1, CRAFTING, 80);
        xpAt("Complete a lap of the Rellekka agility course", AGILITY, 80, at(2640, 3670, 50));
        kc("Kill each of the God Wars Dungeon generals", "general_graardor&kril_tsutsaroth&kreearra&commander_zilyana", 1,
                AGILITY, 70, STRENGTH, 70, HITPOINTS, 70, RANGED, 70);
        killAny("Slay a Spiritual mage within the God Wars Dungeon", "spiritual mage", 1, SLAYER, 83);

        // ======================= Kandarin (11 / 14 / 11 / 7) =======================
        in("kandarin", Tier.EASY);
        get("Catch a mackerel at Catherby", "raw_mackerel", 1, FISHING, 16);
        visit("Buy a candle from the candle maker in Catherby", 2799, 3439, 8);
        get("Collect 5 flax from the Seers' Village flax field", "flax", 5);
        visit("Play the church organ in the Seers' Village church", 2692, 3463, 8);
        xp("Plant jute seeds in the farming patch north of McGrubor's Wood", FARMING, 13);
        visit("Have Galahad make you a cup of tea", 2612, 3474, 8);
        quest("Defeat one of each elemental in the Elemental Workshop", "elemental_workshop_i");
        visit("Get a pet fish from Harry in Catherby", 2833, 3443, 8);
        visit("Buy a stew from the Seers' Village pub", 2691, 3493, 8);
        visit("Speak to Sherlock between Seers' Village and McGrubor's Wood", 2733, 3415, 8);
        visit("Cross the Coal truck log shortcut", 2602, 3477, 8, AGILITY, 20);
        in("kandarin", Tier.MEDIUM);
        xpAt("Complete a lap of the Barbarian agility course", AGILITY, 35, at(2551, 3554, 25));
        makeHere("Create a Super Antipoison potion from scratch in the Seers/Catherby Area", "superantipoison", 1, HERBLORE, 48);
        visit("Enter the Ranging Guild", 2657, 3439, 8, RANGED, 40);
        visit("Use the grapple shortcut to get from the water obelisk to Catherby shore", 2841, 3424, 10, AGILITY, 36, STRENGTH, 22,
                RANGED, 39);
        makeHere("Catch and cook a bass in Catherby", "bass", 1, FISHING, 46, COOKING, 43);
        cast("Teleport to Camelot", "camelot_teleport", MAGIC, 45);
        makeHere("String a maple shortbow in Seers' Village bank", "maple_shortbow", 1, FLETCHING, 50);
        get("Pick some limpwurt root from the farming patch in Catherby", "limpwurt_root", 1, FARMING, 26);
        quest("Create a Mind helmet", "elemental_workshop_ii");
        killAny("Kill a Fire Giant inside Baxtorian Waterfall", "fire giant", 1);
        visit("Complete a wave of Barbarian Assault", 2531, 3577, 12);
        xp("Steal from the chest in Hemenster", THIEVING, 47);
        visit("Travel to McGrubor's Wood by Fairy ring", 2644, 3495, 10);
        get("Mine some coal near the coal trucks", "coal", 1, MINING, 30);
        in("kandarin", Tier.HARD);
        getAt("Catch a leaping sturgeon", "leaping_sturgeon", 1, null, FISHING, 70, AGILITY, 45, STRENGTH, 45);
        xpAt("Complete a lap of the Seers' Village agility course", AGILITY, 60, at(2729, 3488, 30));
        makeHere("Create a Yew Longbow from scratch around Seers' Village", "yew_longbow", 1, FLETCHING, 70, WOODCUTTING, 60, CRAFTING, 10);
        visit("Enter the Seers' Village courthouse with piety turned on", 2735, 3469, 8, PRAYER, 70, DEFENCE, 70);
        cast("Charge a water orb", "charge_water_orb", MAGIC, 56);
        xp("Burn some maple logs with a bow in Seers' Village", FIREMAKING, 65);
        killAny("Kill a shadow hound in the Shadow dungeon", "hound", 1, THIEVING, 53); approx(); // approx: no shadow hounds
        killAny("Kill a Mithril Dragon", "mithril dragon", 1);
        haveAny("Purchase and equip a granite body from Penance Queen", "granite_body", STRENGTH, 50, DEFENCE, 50);
        level("Have the Seers' estate agent decorate your house with Fancy Stone", CONSTRUCTION, 50);
        makeAt("Smith an Adamant spear at Otto's Grotto", "adamant_spear", 1, at(2500, 3493, 16), SMITHING, 75);
        in("kandarin", Tier.ELITE);
        get("Pick some dwarf weed from the herb patch at Catherby", "grimy_dwarf_weed", 1, FARMING, 79);
        makeHere("Fish and cook 5 sharks in Catherby using the Cooking gauntlets", "shark", 5, FISHING, 76, COOKING, 80);
        // The bank roof is part of the Seers' rooftop course; the trigger is the bank's footprint (the roof height isn't checked).
        makeAt("Mix a Stamina mix on top of the Seers' Village bank", "stamina_mix", 1, at(2725, 3493, 6), HERBLORE, 86, AGILITY, 60);
        makeAt("Smith a rune hasta at Otto's Grotto", "rune_hasta", 1, at(2500, 3493, 16), SMITHING, 90);
        xp("Construct a pyre ship from magic logs", FIREMAKING, 85, CRAFTING, 85); approx(); // approx
        cast("Teleport to Catherby", "catherby_teleport", MAGIC, 87);
        visit("Read the Blackboard at Barbarian Assault after reaching level 5 in every role", 2531, 3577, 12); approx(); // approx: no BA roles

        // ======================= Karamja (10 / 19 / 10 / 5) =======================
        in("karamja", Tier.EASY);
        get("Pick 5 bananas from the plantation located east of the volcano", "banana", 5);
        visit("Use the rope swing to travel to the small island north-west of Karamja", 2705, 3209, 10, AGILITY, 10);
        get("Mine some gold from the rocks on the north-west peninsula of Karamja", "gold_ore", 1, MINING, 40);
        visit("Travel to Port Sarim via the dock, east of Musa Point", 3029, 3217, 10);
        visit("Travel to Ardougne via the port near Brimhaven", 2683, 3271, 10);
        visit("Explore Cairn Island to the west of Karamja", 2763, 2975, 12, AGILITY, 15);
        xp("Use the Fishing spots north of the banana plantation", FISHING, 1);
        get("Collect 5 seaweed from anywhere on Karamja", "seaweed", 5); approx(); // approx
        visit("Attempt the TzHaar Fight Pits or Fight Cave", 2845, 3170, 12);
        killAny("Kill a Jogre in the Pothole dungeon", "jogre", 1);
        in("karamja", Tier.MEDIUM);
        visit("Claim a ticket from the Agility Arena in Brimhaven", 2809, 3191, 10);
        quest("Discover hidden wall in the dungeon below the volcano", "dragon_slayer_i");
        quest("Visit the Isle of Crandor via the dungeon below the volcano", "dragon_slayer_i");
        quest("Use Vigroy and Hajedy's cart service", "shilo_village");
        quest("Earn 100% favour in the village of Tai Bwo Wannai", "jungle_potion", WOODCUTTING, 10); approx(); // approx
        xp("Cook a spider on a stick", COOKING, 16); approx(); // approx: no spider on a stick; cook something in Karamja
        visit("Charter the Lady of the Waves from Cairn Isle to Port Khazard", 2675, 3150, 12);
        get("Cut a log from a teak tree", "teak_logs", 1, WOODCUTTING, 35);
        get("Cut a log from a mahogany tree", "mahogany_logs", 1, WOODCUTTING, 50);
        getAt("Catch a karambwan", "raw_karambwan", 1, null, FISHING, 65);
        quest("Exchange gems for a machete", "jungle_potion");
        quest("Use the gnome glider to travel to Karamja", "the_grand_tree");
        xp("Grow a healthy fruit tree in the patch near Brimhaven", FARMING, 27);
        xp("Trap a horned graahk", HUNTER, 41);
        visit("Chop the vines to gain deeper access to Brimhaven Dungeon", 2744, 3152, 10, WOODCUTTING, 10);
        visit("Cross the lava using the stepping stones within Brimhaven Dungeon", 2744, 3152, 10, AGILITY, 12, WOODCUTTING, 10);
        kill("Climb the stairs within Brimhaven Dungeon (slay a dungeon creature)", "dragon|moss giant|fire giant", 1, WOODCUTTING, 10); approx(); // approx
        visit("Charter a ship from the shipyard in the far east of Karamja", 2998, 3032, 15);
        get("Mine a red topaz from a gem rock", "uncut_red_topaz", 1, MINING, 40);
        in("karamja", Tier.HARD);
        visit("Become the Champion of the Fight Pits", 2845, 3170, 12); approx(); // approx
        killAny("Kill a Ket-Zek in the Fight Caves", "ket-zek", 1);
        xp("Eat an oomlie wrap", COOKING, 50); approx(); // approx: no oomlie wrap; cook something in Karamja
        getAt("Craft some nature runes", "nature_rune", 1, null, RUNECRAFT, 44);
        make("Cook a karambwan thoroughly", "cooked_karambwan", 1, COOKING, 30);
        quest("Kill a deathwing in the dungeon under the Kharazi Jungle", "legends_quest"); approx(); // approx
        visit("Use the crossbow shortcut south of the volcano", 2869, 3125, 10, AGILITY, 53, STRENGTH, 21, RANGED, 42);
        quest("Collect 5 palm leaves", "legends_quest", WOODCUTTING, 15); approx(); // approx
        combat("Be assigned a Slayer task by Duradel north of Shilo Village", 100, SLAYER, 50);
        killAny("Kill a metal dragon in Brimhaven Dungeon", "bronze dragon|iron dragon|steel dragon", 1, WOODCUTTING, 10);
        in("karamja", Tier.ELITE);
        getAt("Craft 56 Nature runes at once", "nature_rune", 1, null, RUNECRAFT, 91);
        xp("Check the health of a palm tree in Brimhaven", FARMING, 68);
        makeHere("Create an antivenom potion whilst standing in the horse shoe mine", "anti_venom", 1, HERBLORE, 87);
        xp("Check the health of your Calquat tree patch", FARMING, 72);
        have("Equip a Fire cape or Infernal cape in the TzHaar City", "fire_cape|infernal_cape");

        // ======================= Kourend & Kebos (12 / 13 / 10 / 8) =======================
        in("kourend", Tier.EASY);
        get("Mine some iron at the Mount Karuulm mine", "iron_ore", 1, MINING, 15);
        killAny("Kill a sand crab", "sand crab", 1);
        visit("Hand in a book in the Arceuus Library", 1632, 3808, 15);
        xp("Steal from a Hosidius food stall", THIEVING, 25);
        visit("Browse the Warrens General Store", 1640, 3673, 20); approx(); // approx
        visit("Take a boat to Land's End", 1504, 3399, 15);
        visit("Pray at the altar in Kourend Castle", 1616, 3673, 10);
        xp("Dig up some saltpetre", FARMING, 1); approx(); // approx
        visit("Enter your POH from Hosidius", 1742, 3517, 10, CONSTRUCTION, 25);
        xpAt("Do a lap of the Shayzien Agility Course", AGILITY, 1, at(1550, 3630, 40));
        makeHere("Create a strength potion in the Lovakengj pub", "strength_potion", 1, HERBLORE, 12);
        get("Fish a trout from the River Molch", "raw_trout", 1, FISHING, 20);
        in("kourend", Tier.MEDIUM);
        visit("Travel to the fairy ring south of Mount Karuulm", 1302, 3762, 10);
        killAny("Kill a lizardman", "lizardman", 1);
        quest("Use Kharedst's memoirs to teleport to all five cities", "client_of_kourend");
        xp("Mine some volcanic sulphur", MINING, 42); approx(); // approx
        visit("Enter the Farming Guild", 1249, 3718, 10, FARMING, 45);
        visit("Switch to the Necromancy spellbook at Tyss", 1712, 3883, 10);
        visit("Repair a Piscarilius crane", 1820, 3740, 15, CRAFTING, 30, CONSTRUCTION, 30);
        visit("Deliver some intelligence to Captain Ginea", 1504, 3632, 12);
        xp("Catch a Bluegill on Molch Island", FISHING, 43, HUNTER, 35);
        visit("Use the boulder leap in the Arceuus essence mine", 1760, 3880, 15, AGILITY, 49);
        xpAt("Subdue the Wintertodt", FIREMAKING, 50, at(1630, 3980, 50));
        xp("Catch a chinchompa in the Kourend Woodland", HUNTER, 53);
        get("Chop some mahogany logs north of the Farming Guild", "mahogany_logs", 1, WOODCUTTING, 50);
        in("kourend", Tier.HARD);
        visit("Enter the Woodcutting Guild", 1657, 3505, 10, WOODCUTTING, 60);
        makeHere("Smelt an adamantite bar in The Forsaken Tower", "adamantite_bar", 1, SMITHING, 70);
        killAny("Kill a lizardman shaman in Molch", "lizardman shaman", 1);
        get("Mine some Lovakite", "lovakite_ore", 1, MINING, 65);
        xp("Plant some Logavano seeds at the Tithe Farm", FARMING, 74);
        haveAny("Teleport to Xeric's Heart using Xeric's Talisman", "xerics_talisman"); approx(); // approx
        xp("Deliver an artefact to Captain Khaled", THIEVING, 49);
        killAny("Kill a wyrm in the Karuulm Slayer Dungeon", "wyrm", 1, SLAYER, 62);
        killAny("Cast Monster Examine on a mountain troll south of Mount Quidamortem", "mountain troll", 1, MAGIC, 66); approx(); // approx
        killAny("Kill a Zombie in the Shayzien Crypts", "zombie", 1); approx(); // approx: the crypts are not built
        in("kourend", Tier.ELITE);
        getAt("Craft one or more Blood runes from Dark essence fragments", "blood_rune", 1, null, RUNECRAFT, 77, MINING, 38, CRAFTING, 38);
        get("Chop some redwood logs", "redwood_logs", 1, WOODCUTTING, 90);
        kc("Kill Skotizo in the Catacombs of Kourend", "skotizo", 1);
        makeHere("Catch an Anglerfish and cook it whilst in Great Kourend", "anglerfish", 1, FISHING, 82, COOKING, 84);
        killAny("Kill a hydra in the Karuulm Slayer Dungeon", "hydra", 1, SLAYER, 95);
        cast("Create an Ape Atoll teleport tablet", "ape_atoll_teleport", MAGIC, 90); approx(); // approx
        kc("Complete a raid in the Chambers of Xeric", "great_olm", 1);
        make("Create your own Battlestaff from scratch within the Farming Guild", "*battlestaff", 1, FLETCHING, 40, FARMING, 85);

        // ======================= Lumbridge & Draynor (12 / 12 / 11 / 6) =======================
        in("lumbridge", Tier.EASY);
        xpAt("Complete a lap of the Draynor Village rooftop course", AGILITY, 10, at(3093, 3255, 30));
        killAny("Slay a cave bug beneath Lumbridge Swamp", "cave bug", 1, SLAYER, 7);
        quest("Have Sedridor teleport you to the Essence mine", "RUNE_MYSTERIES");
        getAt("Craft some water runes", "water_rune", 1, null, RUNECRAFT, 5);
        visit("Learn your age from Hans in Lumbridge", 3222, 3218, 12);
        xp("Pickpocket a man or woman in Lumbridge", THIEVING, 1);
        get("Chop and burn some oak logs in Lumbridge", "oak_logs", 1, WOODCUTTING, 15, FIREMAKING, 15);
        kill("Kill a zombie in the Draynor Sewers", "zombie", 1);
        getAt("Catch some anchovies in Al Kharid", "raw_anchovies", 1, null, FISHING, 15);
        make("Bake some bread on the Lumbridge castle kitchen range", "bread", 1);
        getAt("Mine some iron ore at the Al Kharid mine", "iron_ore", 1, null, MINING, 15);
        visit("Enter the H.A.M. Hideout", 3166, 3252, 8);
        in("lumbridge", Tier.MEDIUM);
        xpAt("Complete a lap of the Al Kharid rooftop course", AGILITY, 20, at(3273, 3195, 40));
        visit("Grapple across the River Lum", 3252, 3179, 10, AGILITY, 8, STRENGTH, 19, RANGED, 37);
        quest("Purchase an upgraded device from Ava", "animal_magnetism", RANGED, 50);
        visit("Travel to the Wizards' Tower by Fairy ring", 3109, 3162, 12);
        cast("Cast the teleport to Lumbridge spell", "lumbridge_teleport", MAGIC, 31);
        get("Catch some salmon in Lumbridge", "raw_salmon", 1, FISHING, 30);
        make("Craft a coif in the Lumbridge cow pen", "coif", 1, CRAFTING, 38);
        get("Chop some willow logs in Draynor Village", "willow_logs", 1, WOODCUTTING, 30);
        xp("Pickpocket Martin the Master Gardener", THIEVING, 38);
        combat("Get a slayer task from Chaeldar", 70);
        xpAt("Catch an essence or eclectic impling in Puro-Puro", HUNTER, 42, null);
        getAt("Craft some lava runes at the fire altar in Al Kharid", "lava_rune|fire_rune", 1, null, RUNECRAFT, 23);
        in("lumbridge", Tier.HARD);
        castAt("Cast Bones to Peaches in Al Kharid palace", "bones_to_peaches", at(3293, 3170, 15), MAGIC, 60);
        visit("Squeeze past the jutting wall on your way to the cosmic altar", 2405, 3380, 15, AGILITY, 46);
        getAt("Craft 56 cosmic runes simultaneously from Essence", "cosmic_rune", 1, null, RUNECRAFT, 59);
        visit("Travel from Lumbridge to Edgeville on a Waka Canoe", 3130, 3508, 12, WOODCUTTING, 57);
        quest("Collect at least 100 Tears of Guthix in one visit", "tears_of_guthix");
        quest("Take the train from Dorgesh-Kaan to Keldagrim", "another_slice_of_ham");
        haveAny("Purchase some Barrows gloves from the Lumbridge bank chest", "barrows_gloves");
        get("Pick some Belladonna from the farming patch at Draynor Manor", "*belladonna", 1, FARMING, 63); approx(); // approx
        xp("Light your mining helmet in the Lumbridge castle basement", FIREMAKING, 65);
        visit("Recharge your prayer at the Emir's Arena with Smite activated", 3369, 3170, 15, PRAYER, 52);
        // Enchanting is a spell (an obtain event), not a make-menu product.
        get("Craft, string and enchant an Amulet of Power in Lumbridge", "amulet_of_power", 1, CRAFTING, 70, MAGIC, 57);
        in("lumbridge", Tier.ELITE);
        quest("Steal from a Dorgesh-Kaan rich chest", "death_to_the_dorgeshuun", THIEVING, 78); approx(); // approx
        quest("Grapple across a pylon on the Dorgesh-Kaan Agility Course", "death_to_the_dorgeshuun", AGILITY, 70, RANGED, 70,
                STRENGTH, 70); approx(); // approx
        getAt("Chop some magic logs at the Mage Training Arena", "magic_logs", 1, null, WOODCUTTING, 75);
        make("Smith an adamant platebody down Draynor sewer", "adamant_platebody", 1, SMITHING, 88);
        getAt("Craft 140 or more Water runes at once from Essence", "water_rune", 1, null, RUNECRAFT, 76);
        add("Perform the Quest point cape emote in the Wise Old Man's house", Type.QUEST_POINTS, "all", 1, null, 0);

        // ======================= Morytania (11 / 11 / 10 / 6) =======================
        in("morytania", Tier.EASY);
        makeHere("Craft any Snelm from scratch in Morytania", "*snelm", 1, CRAFTING, 15);
        makeAt("Cook a thin Snail on the Port Phasmatys range", "thin_snail_meat", 1, at(3680, 3485, 25), COOKING, 12);
        approx(); // approx: Port Phasmatys has no range built, so any fire in the town counts
        combat("Get a slayer task from Mazchna", 20);
        killAny("Kill a Banshee in the Slayer Tower", "banshee", 1, SLAYER, 15);
        visit("Have Sbott in Canifis tan something for you", 3490, 3502, 8);
        visit("Enter Mort Myre Swamp", 3437, 3437, 15);
        kill("Kill a Ghoul", "ghoul", 1);
        xp("Place a Scarecrow in the Morytania flower patch", FARMING, 23);
        visit("Offer some Bonemeal at the Ectofuntus", 3660, 3522, 10);
        kill("Kill a Werewolf in its human form using the Wolfbane Dagger", "werewolf", 1);
        quest("Restore your prayer points at the nature altar", "nature_spirit");
        in("morytania", Tier.MEDIUM);
        xp("Catch a swamp lizard", HUNTER, 29);
        xpAt("Complete a lap of the Canifis agility course", AGILITY, 40, at(3507, 3488, 30));
        get("Obtain some Bark from a Hollow tree", "bark", 1, WOODCUTTING, 45);
        killAny("Kill a Terror Dog", "terror dog", 1, SLAYER, 40);
        quest("Complete a game of Trouble Brewing", "cabin_fever", COOKING, 40); approx(); // approx
        makeHere("Make a batch of cannonballs at the Port Phasmatys furnace", "cannonball", 1, SMITHING, 35);
        killAny("Kill a Fever Spider on Braindeath Island", "fever spider", 1, SLAYER, 42);
        haveAny("Use an ectophial to return to Port Phasmatys", "ectophial");
        makeHere("Mix a Guthix Balance potion while in Morytania", "guthix_balance", 1, HERBLORE, 22);
        visit("Travel to Dragontooth Isle", 3815, 3560, 20);
        visit("Board the Swamp boat at the Hollows", 3500, 3380, 12);
        in("morytania", Tier.HARD);
        cast("Enter the Kharyrll portal in your POH", "kharyrll_teleport", MAGIC, 66, CONSTRUCTION, 50); approx(); // approx
        visit("Climb the advanced spike chain within Slayer Tower", 3422, 3537, 12, AGILITY, 71);
        getAt("Harvest some Watermelon from the Allotment patch on Harmony Island", "watermelon", 1, null, FARMING, 47);
        getAt("Chop and burn some mahogany logs on Mos Le'Harmless", "mahogany_logs", 1, null, WOODCUTTING, 50, FIREMAKING, 50);
        quest("Complete a temple trek with a hard companion", "in_aid_of_the_myreque"); approx(); // approx
        killAny("Kill a Cave Horror", "cave horror", 1, SLAYER, 58);
        xp("Harvest some Bittercap Mushrooms", FARMING, 53);
        visit("Pray at the Altar of Nature with Piety activated", 3440, 3335, 15, PRAYER, 70, DEFENCE, 70);
        visit("Use the shortcut to get to the bridge over the Salve", 3424, 3476, 15, AGILITY, 65); approx(); // approx tile
        get("Mine some Mithril ore in the Abandoned Mine", "mithril_ore", 1, MINING, 55);
        in("morytania", Tier.ELITE);
        get("Catch a shark in Burgh de Rott with your bare hands", "raw_shark", 1, FISHING, 96, STRENGTH, 76);
        xp("Cremate any Shade remains on a Magic or Redwood pyre", FIREMAKING, 80);
        xp("Fertilize the Morytania herb patch using Lunar Magic", FARMING, 1, MAGIC, 83); approx(); // approx
        makeHere("Craft a Black dragonhide body in Canifis bank", "black_dhide_body", 1, CRAFTING, 84);
        kill("Kill an Abyssal demon in the Slayer Tower", "abyssal demon", 1, SLAYER, 85);
        kc("Loot the Barrows chest while wearing any complete Barrows set", "barrows_chests", 1, DEFENCE, 70);

        // ======================= Varrock (14 / 13 / 10 / 5) =======================
        in("varrock", Tier.EASY);
        visit("Browse Thessalia's store", 3204, 3417, 8);
        quest("Have Aubury teleport you to the Essence mine", "RUNE_MYSTERIES");
        get("Mine some Iron in the south east Varrock mine", "iron_ore", 1, MINING, 15);
        visit("Make a normal plank at the Sawmill", 3302, 3491, 10);
        visit("Enter the second level of the Stronghold of Security", 3081, 3421, 10);
        visit("Jump over the fence south of Varrock", 3240, 3335, 10, AGILITY, 13);
        get("Chop down a dying tree in the Lumber Yard", "logs", 1);
        visit("Buy a newspaper", 3218, 3432, 10);
        visit("Give a dog a bone!", 3213, 3428, 20); approx(); // approx
        makeAt("Spin a bowl on the pottery wheel and fire it in Barbarian Village", "bowl", 1, at(3086, 3409, 25), CRAFTING, 8);
        visit("Speak to Haig Halen after obtaining at least 50 kudos", 3256, 3448, 10); approx(); // approx
        getAt("Craft some Earth runes from Essence", "earth_rune", 1, null, RUNECRAFT, 9);
        getAt("Catch some trout in the River Lum at Barbarian Village", "raw_trout", 1, at(3104, 3430, 30), FISHING, 20);
        xp("Steal from the tea stall in Varrock", THIEVING, 5);
        in("varrock", Tier.MEDIUM);
        visit("Have the Apothecary in Varrock make you a strength potion", 3195, 3404, 8);
        visit("Enter the Champions' Guild", 3191, 3362, 8);
        quest("Select a colour for your kitten", "gertrudes_cat");
        visit("Use the spirit tree in the north eastern corner of Grand Exchange", 3183, 3508, 8);
        visit("Perform the 4 emotes from the Stronghold of Security", 3081, 3421, 10); approx(); // approx
        quest("Enter the Tolna dungeon after completing A Soul's Bane", "a_souls_bane");
        quest("Teleport to the digsite using a Digsite pendant", "the_dig_site");
        cast("Cast the teleport to Varrock spell", "varrock_teleport", MAGIC, 25);
        combat("Get a Slayer task from Vannaka", 40);
        haveAny("Make 20 Mahogany Planks in one go", "mahogany_plank"); approx(); // approx
        quest("Pick a White tree fruit", "garden_of_tranquillity", FARMING, 25);
        visit("Use the balloon to travel from Varrock", 3297, 3480, 10, FIREMAKING, 40, FARMING, 30);
        xp("Complete a lap of the Varrock Agility course", AGILITY, 30);
        in("varrock", Tier.HARD);
        haveAny("Trade furs with the Fancy Dress Seller for a spottier cape and equip it", "spottier_cape", HUNTER, 66);
        visit("Speak to Orlando Smith when you have achieved 153 kudos", 3256, 3448, 10); approx(); // approx
        xpAt("Make a Waka Canoe near Edgeville", WOODCUTTING, 57, at(3130, 3508, 20));
        cast("Teleport to Paddewwa", "paddewwa_teleport", MAGIC, 54);
        visit("Teleport to Barbarian Village with a Skull sceptre", 3082, 3420, 12); approx(); // approx
        get("Chop some yew logs in Varrock and burn them at the top of the Varrock church", "yew_logs", 1, WOODCUTTING, 60, FIREMAKING, 60);
        level("Have the Varrock estate agent decorate your house with Fancy Stone", CONSTRUCTION, 50);
        xp("Collect at least 2 yew roots from the Tree patch in Varrock Palace", FARMING, 68, WOODCUTTING, 60);
        visit("Pray at the altar in Varrock palace with Smite active", 3208, 3495, 8, PRAYER, 52);
        visit("Squeeze through the obstacle pipe in Edgeville dungeon", 3097, 3468, 10, AGILITY, 51);
        in("varrock", Tier.ELITE);
        makeHere("Create a super combat potion in Varrock west bank", "super_combat_potion", 1, HERBLORE, 90);
        castAt("Use Lunar magic to make 20 mahogany planks in the Varrock Lumber Yard", "plank_make", at(3302, 3491, 15), MAGIC, 86);
        make("Bake a summer pie in the Cooking Guild", "summer_pie", 1, COOKING, 95);
        makeHere("Smith and fletch ten rune darts within Varrock", "rune_dart", 10, SMITHING, 89, FLETCHING, 81);
        getAt("Craft 100 or more earth runes simultaneously from Essence", "earth_rune", 1, null, RUNECRAFT, 78);

        // ======================= Western Provinces (11 / 13 / 13 / 7) =======================
        in("western", Tier.EASY);
        xp("Catch a Copper Longtail", HUNTER, 9);
        combat("Complete a novice game of Pest Control", 40);
        get("Mine some Iron Ore near Piscatoris", "iron_ore", 1, MINING, 15);
        xpAt("Complete a lap of the Gnome Agility Course", AGILITY, 1, at(2474, 3436, 25));
        visit("Score a goal in a Gnome Ball match", 2400, 3487, 12);
        quest("Claim any Chompy bird hat from Rantz", "big_chompy_bird_hunting");
        visit("Teleport to Pest Control using the Minigame teleports", 2662, 2653, 15);
        visit("Collect a swamp toad at the Gnome Stronghold", 2420, 3510, 15);
        visit("Have Brimstail teleport you to the Essence mine", 2410, 3415, 12);
        makeHere("Fletch an Oak shortbow in the Gnome Stronghold", "oak_shortbow", 1, FLETCHING, 20);
        killAt("Kill a Terrorbird in the Terrorbird enclosure", "terrorbird", 1, at(2378, 3434, 12));
        in("western", Tier.MEDIUM);
        visit("Take the agility shortcut from the Grand Tree to Otto's Grotto", 2486, 3515, 10, AGILITY, 37);
        quest("Travel to the Gnome Stronghold by Spirit Tree", "tree_gnome_village");
        xp("Trap a Spined Larupia", HUNTER, 31);
        getAt("Fish some Bass on Ape Atoll", "raw_bass", 1, at(2755, 2775, 50), FISHING, 46);
        getAt("Chop and burn some teak logs on Ape Atoll", "teak_logs", 1, at(2755, 2775, 50), WOODCUTTING, 35, FIREMAKING, 35);
        combat("Complete an intermediate game of Pest Control", 70);
        quest("Travel to the Feldip Hills by Gnome Glider", "one_small_favour");
        quest("Claim a Chompy bird hat from Rantz after registering at least 30 kills", "big_chompy_bird_hunting");
        quest("Travel from Eagles' Peak to the Feldip Hills by Eagle", "eagles_peak");
        make("Make a Chocolate Bomb at the Grand Tree", "*chocolate", 1, COOKING, 42); approx(); // approx
        visit("Complete a delivery for the Gnome Restaurant", 2449, 3500, 12, COOKING, 29);
        quest("Turn your small crystal seed into a Crystal saw", "the_eyes_of_glouphrie");
        get("Mine some Gold ore underneath the Grand Tree", "gold_ore", 1, MINING, 40);
        in("western", Tier.HARD);
        kill("Kill an Elf with a Crystal bow", "elf", 1, RANGED, 70);
        makeHere("Catch and cook a Monkfish in Piscatoris", "monkfish", 1, FISHING, 62, COOKING, 62);
        combat("Complete a Veteran game of Pest Control", 100);
        xpAt("Catch a Dashing Kebbit", HUNTER, 69, null);
        xpAt("Complete a lap of the Ape Atoll agility course", AGILITY, 48, at(2755, 2775, 50));
        getAt("Chop and burn some Mahogany logs on Ape Atoll", "mahogany_logs", 1, at(2755, 2775, 50), WOODCUTTING, 50, FIREMAKING, 50);
        get("Mine some Adamantite ore in Tirannwn", "adamantite_ore", 1, MINING, 70);
        xpAt("Check the health of your Palm tree in Lletya", FARMING, 68, at(2340, 3170, 40));
        quest("Claim a Chompy bird hat from Rantz after registering at least 125 kills", "big_chompy_bird_hunting");
        quest("Build an Isafdar painting in your POH Quest hall", "roving_elves", CONSTRUCTION, 65);
        kc("Kill Zulrah", "zulrah", 1);
        cast("Teleport to Ape Atoll", "ape_atoll_teleport", MAGIC, 64);
        xp("Pickpocket a Gnome", THIEVING, 75);
        in("western", Tier.ELITE);
        makeHere("Fletch a Magic Longbow in Tirannwn", "magic_longbow", 1, FLETCHING, 85);
        kc("Kill the Thermonuclear Smoke devil", "thermonuclear_smoke_devil", 1, SLAYER, 93);
        xp("Have Prissy Scilla protect your Magic tree", FARMING, 75);
        visit("Use the Elven overpass advanced cliffside shortcut", 2340, 3260, 15, AGILITY, 85);
        haveAny("Equip any complete void set", "void_knight_top", ATTACK, 42, STRENGTH, 42, DEFENCE, 42, HITPOINTS, 42, RANGED, 42,
                MAGIC, 42, PRAYER, 22); approx(); // approx: carrying the top
        quest("Claim a Chompy bird hat from Rantz after registering at least 1000 kills", "big_chompy_bird_hunting");
        xp("Pickpocket an Elf", THIEVING, 85);

        // ======================= Wilderness (12 / 11 / 10 / 7) =======================
        in("wilderness", Tier.EASY);
        visit("Cast Low Alchemy at the Fountain of Rune", 3375, 3893, 10, MAGIC, 21); approx(); // approx
        visit("Enter the Wilderness from the Ardougne or Edgeville lever", 3153, 3923, 12);
        visit("Pray at the Chaos Altar in Western Wilderness", 2949, 3820, 10);
        visit("Enter the Chaos Runecrafting temple", 3060, 3591, 10);
        kill("Kill a Mammoth in the Wilderness", "mammoth", 1);
        kill("Kill an Earth Warrior in the Wilderness beneath Edgeville", "earth warrior", 1, AGILITY, 15);
        visit("Restore some Prayer points at the demonic ruins", 3287, 3884, 10);
        visit("Enter the King Black Dragon Lair", 3016, 3849, 12);
        get("Collect 5 Red spider's eggs from the Wilderness", "red_spiders_eggs", 5);
        get("Mine some Iron ore in the Wilderness", "iron_ore", 1, MINING, 15);
        quest("Have the Mage of Zamorak teleport you to the Abyss", "enter_the_abyss");
        visit("Equip any team cape in the Wilderness", 3090, 3525, 20); approx(); // approx
        in("wilderness", Tier.MEDIUM);
        get("Mine some Mithril ore in the wilderness", "mithril_ore", 1, MINING, 55);
        get("Chop some yew logs from a fallen Ent", "yew_logs", 1, WOODCUTTING, 61); approx(); // approx
        visit("Enter the Wilderness God Wars Dungeon", 3017, 3738, 12, AGILITY, 60);
        xpAt("Complete a lap of the Wilderness Agility course", AGILITY, 52, at(3000, 3940, 30));
        killAny("Kill a Green Dragon", "green dragon", 1);
        kill("Kill an Ankou in the Wilderness", "ankou", 1);
        cast("Charge an Earth Orb", "charge_earth_orb", MAGIC, 60);
        kill("Kill a Bloodveld in the Wilderness God Wars Dungeon", "bloodveld", 1, SLAYER, 50, AGILITY, 60);
        quest("Smith a Golden helmet in the Resource Area", "between_a_rock", SMITHING, 50); approx(); // approx
        visit("Talk to the Emblem Trader in Edgeville about emblems", 3097, 3504, 10);
        visit("Open the Muddy Chest in the lava maze", 3089, 3859, 8); approx(); // approx: no muddy key
        in("wilderness", Tier.HARD);
        castAt("Cast one of the 3 God spells against another player in the Wilderness",
                "saradomin_strike|claws_of_guthix|flames_of_zamorak", "wilderness", MAGIC, 60); approx(); // approx: any target
        cast("Charge an Air Orb", "charge_air_orb", MAGIC, 66);
        xp("Catch a Black Salamander", HUNTER, 67);
        makeHere("Smith an Adamant scimitar in the Resource Area", "adamant_scimitar", 1, SMITHING, 75);
        killAny("Kill a Lava dragon on Lava Dragon Isle", "lava dragon", 1);
        kc("Kill the Chaos Elemental", "chaos_elemental", 1);
        kc("Kill the Crazy Archaeologist, Chaos Fanatic and Scorpia", "crazy_archaeologist&chaos_fanatic&scorpia", 1);
        visit("Take the agility shortcut from Trollheim into the Wilderness", 2949, 3768, 12, AGILITY, 64);
        kill("Kill a Spiritual warrior in the Wilderness God Wars Dungeon", "spiritual warrior", 1, SLAYER, 68, AGILITY, 60);
        get("Fish some raw lava eel in the Wilderness", "*lava_eel", 1, FISHING, 53, HERBLORE, 25); // the Lava Maze spot
        in("wilderness", Tier.ELITE);
        kc("Kill Callisto, Venenatis and Vet'ion", "callisto|artio&venenatis|spindel&vetion|calvarion", 1);
        retire(3); // Venenatis, Vet'ion and Scorpia were merged into the boss tasks above
        cast("Teleport to Ghorrock", "ghorrock_teleport", MAGIC, 96);
        makeHere("Fish and Cook a Dark Crab in the Resource Area", "dark_crab", 1, FISHING, 85, COOKING, 90);
        makeHere("Smith a rune scimitar from scratch in the Resource Area", "rune_scimitar", 1, SMITHING, 90, MINING, 85);
        xp("Steal from the Rogues' chest", THIEVING, 84);
        kill("Slay a spiritual mage inside the wilderness God Wars Dungeon", "spiritual mage", 1, SLAYER, 83, AGILITY, 60);
        get("Cut and burn some magic logs in the Resource Area", "magic_logs", 1, WOODCUTTING, 75, FIREMAKING, 75);

        // ---- Quest requirements per task (OSRS wiki task tables: "Completion of ..."; quests only started are not
        // listed). Checked when the task completes, like the skill requirements. ----
        needs("ardougne_easy_0", "RUNE_MYSTERIES");
        needs("ardougne_easy_5", "biohazard");
        needs("ardougne_medium_3", "plague_city");
        needs("ardougne_medium_4", "enlightened_journey");
        needs("ardougne_medium_10", "underground_pass");
        needs("ardougne_hard_0", "legends_quest");
        needs("ardougne_hard_4", "watchtower");
        needs("ardougne_hard_11", "mournings_end_part_ii");
        needs("ardougne_elite_4", "haunted_mine");
        needs("ardougne_elite_7", "desert_treasure_i");
        needs("desert_medium_4", "the_golem");
        needs("desert_medium_6", "spirits_of_the_elid");
        needs("desert_hard_2", "dream_mentor");
        needs("desert_hard_6", "desert_treasure_i");
        needs("desert_elite_1", "desert_treasure_i");
        needs("desert_elite_3", "priest_in_peril");
        needs("falador_easy_10", "the_knights_sword", "DORICS_QUEST");
        needs("falador_medium_4", "skippy_and_the_mogres");
        needs("falador_medium_8", "recruitment_drive");
        needs("falador_hard_6", "heroes_quest");
        needs("falador_hard_8", "the_slug_menace");
        needs("fremennik_easy_1", "fremennik_trials");
        needs("fremennik_easy_3", "fremennik_trials");
        needs("fremennik_medium_2", "fremennik_trials");
        needs("fremennik_medium_3", "fremennik_trials");
        needs("fremennik_medium_4", "fremennik_trials", "fairytale_ii_cure_a_queen");
        needs("fremennik_medium_7", "horror_from_the_deep");
        needs("fremennik_hard_0", "eadgars_ruse");
        needs("fremennik_hard_7", "lunar_diplomacy");
        needs("fremennik_elite_1", "lunar_diplomacy");
        needs("kandarin_medium_0", "alfred_grimhands_barcrawl");
        needs("kandarin_hard_3", "kings_ransom");
        needs("kandarin_elite_1", "family_crest");
        needs("kandarin_elite_5", "lunar_diplomacy");
        needs("karamja_medium_6", "shilo_village");
        needs("karamja_medium_7", "jungle_potion");
        needs("karamja_medium_8", "jungle_potion");
        needs("karamja_hard_4", "tai_bwo_wannai_trio");
        needs("karamja_hard_8", "shilo_village");
        needs("kourend_easy_10", "DRUIDIC_RITUAL");
        needs("kourend_medium_2", "the_depths_of_despair", "the_queen_of_thieves", "tale_of_the_righteous", "the_forsaken_tower", "the_ascent_of_arceuus");
        needs("kourend_hard_1", "the_forsaken_tower");
        needs("kourend_hard_8", "dream_mentor");
        needs("lumbridge_easy_9", "COOKS_ASSISTANT");
        needs("lumbridge_medium_9", "lost_city");
        needs("lumbridge_hard_1", "lost_city");
        needs("lumbridge_hard_2", "lost_city");
        needs("lumbridge_hard_6", "recipe_for_disaster"); // the wiki row words it as "all subquests of Recipe for Disaster"
        needs("morytania_medium_3", "lair_of_tarn_razorlor");
        needs("morytania_medium_5", "dwarf_cannon");
        needs("morytania_medium_7", "ghosts_ahoy");
        needs("morytania_hard_0", "desert_treasure_i");
        needs("morytania_hard_3", "cabin_fever");
        needs("morytania_hard_5", "cabin_fever");
        needs("morytania_hard_7", "nature_spirit", "kings_ransom");
        needs("morytania_hard_9", "haunted_mine");
        needs("morytania_elite_0", "in_aid_of_the_myreque");
        needs("morytania_elite_1", "shades_of_mortton");
        needs("morytania_elite_2", "lunar_diplomacy");
        needs("varrock_medium_2", "garden_of_tranquillity");
        needs("varrock_medium_3", "tree_gnome_village");
        needs("varrock_medium_11", "enlightened_journey");
        needs("varrock_hard_3", "desert_treasure_i");
        needs("varrock_elite_0", "DRUIDIC_RITUAL");
        needs("varrock_elite_1", "dream_mentor");
        needs("varrock_elite_3", "the_tourist_trap");
        needs("western_easy_8", "RUNE_MYSTERIES");
        needs("western_medium_0", "tree_gnome_village", "the_grand_tree");
        needs("western_medium_6", "the_grand_tree");
        needs("western_medium_12", "the_grand_tree");
        needs("western_hard_0", "roving_elves");
        needs("western_hard_1", "swan_song");
        needs("western_hard_11", "recipe_for_disaster", "recipe_for_disaster");
        needs("western_elite_3", "underground_pass");
        needs("wilderness_hard_0", "mage_arena_i");
        needs("wilderness_elite_4", "desert_treasure_i");
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

    /** Highest level of each skill an area's tier needs (shown like the OSRS diary requirement list). */
    public static Map<Skill, Integer> requirements(String areaId, Tier t) {
        Map<Skill, Integer> out = new EnumMap<>(Skill.class);
        for (Task task : tasks(areaId, t)) {
            task.allReqs().forEach((s, l) -> out.merge(s, l, Math::max));
        }
        return out;
    }

    /** Quests a tier's tasks need. */
    public static Set<String> questRequirements(String areaId, Tier t) {
        Set<String> out = new LinkedHashSet<>();
        for (Task task : tasks(areaId, t)) {
            if (task.type() == Type.QUEST) {
                out.add(task.target());
            }
            out.addAll(questReqs(task));
        }
        return out;
    }

    /** Quests (ids) that must be complete before the task counts, as listed on the OSRS wiki. */
    public static List<String> questReqs(Task task) {
        return QUEST_REQS.getOrDefault(task.id(), List.of());
    }

    /** Whether the task's trigger only approximates the OSRS task (a mechanic the game doesn't have). */
    public static boolean isApprox(Task task) {
        return APPROX.contains(task.id());
    }

    /** Whether a set of completed task ids completes an area's tier. */
    public static boolean tierComplete(Set<String> done, String areaId, Tier t) {
        for (Task task : tasks(areaId, t)) {
            if (!done.contains(task.id())) {
                return false;
            }
        }
        return true;
    }

    /** Whether every elite diary is complete (achievement diary cape). */
    public static boolean allElite(Set<String> done) {
        for (Area a : AREAS) {
            for (Tier t : Tier.values()) {
                if (!tierComplete(done, a.id(), t)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Item id match for MAKE/HAVE/OBTAIN tasks: exact, '*x' (contains x), '*' (anything); '|' separates alternatives. */
    public static boolean itemMatches(String target, String item) {
        for (String alt : target.split("\\|")) {
            if (alt.equals("*") || (alt.startsWith("*") ? item.contains(alt.substring(1)) : alt.equals(item))) {
                return true;
            }
        }
        return false;
    }

    /** Monster keyword match for KILL tasks ('|' separates alternatives, matched against the lower-case name). */
    public static boolean killMatches(String target, String monsterName) {
        String name = monsterName.toLowerCase(Locale.ROOT);
        for (String alt : target.split("\\|")) {
            if (name.contains(alt)) {
                return true;
            }
        }
        return false;
    }

    /** Spell match for CAST tasks ('|' separates alternatives). */
    public static boolean spellMatches(String target, String spell) {
        for (String alt : target.split("\\|")) {
            if (alt.equals(spell)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether boss kill counts satisfy a KC target: '&' needs every boss at {@code count}, '|' sums the
     * alternatives. A boss id matches kill-count keys equal to it or starting with it (e.g. barrows_chests).
     */
    public static boolean kcMet(String target, int count, Map<String, Integer> killCounts) {
        for (String part : target.split("&")) {
            int sum = 0;
            for (String b : part.split("\\|")) {
                sum += kcOf(b, killCounts);
            }
            if (sum < count) {
                return false;
            }
        }
        return true;
    }

    private static int kcOf(String boss, Map<String, Integer> killCounts) {
        int kc = 0;
        for (Map.Entry<String, Integer> e : killCounts.entrySet()) {
            if (e.getKey().equals(boss) || e.getKey().startsWith(boss + "_")) {
                kc += e.getValue();
            }
        }
        return kc;
    }

    /** Parses a VISIT target "x,y,r". */
    public static int[] visitTarget(String target) {
        String[] p = target.split(",");
        return new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])};
    }

    /** Whether a restriction (null, an area id or an "x,y,r" circle) contains the player's region/position. */
    public static boolean restrictContains(String restrict, String region, int x, int y) {
        if (restrict == null) {
            return true;
        }
        if (restrict.indexOf(',') > 0) {
            int[] c = visitTarget(restrict);
            return Teleports.within(x, y, c[0], c[1], c[2]);
        }
        Area a = area(restrict);
        return a == null || a.contains(region, x, y);
    }
}
