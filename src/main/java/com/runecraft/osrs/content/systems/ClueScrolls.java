package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.QuestNpc;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Treasure trails: clue tiers, step generation (coordinate and map digs, hot/cold digs with the strange
 * device, emote clues, anagrams, ciphers, cryptic clues and Charlie-style item fetches) at real Gielinor
 * coordinates and for the NPCs that exist in the world. Step counts and the step types each tier can roll
 * follow the OSRS wiki; puzzle boxes, light boxes and Falo the Bard are not simulated.
 */
public final class ClueScrolls {
    public enum Tier {
        BEGINNER(1, 3, 1, 3, 0xC0B080), EASY(2, 4, 2, 4, 0x40A040), MEDIUM(3, 5, 3, 5, 0x40A0C0),
        HARD(4, 6, 4, 6, 0xA040C0), ELITE(5, 7, 4, 6, 0xE0C030), MASTER(6, 8, 5, 7, 0xC03030);

        private final int minSteps;
        private final int maxSteps;
        private final int minRolls;
        private final int maxRolls;
        private final int color;

        Tier(int minSteps, int maxSteps, int minRolls, int maxRolls, int color) {
            this.minSteps = minSteps;
            this.maxSteps = maxSteps;
            this.minRolls = minRolls;
            this.maxRolls = maxRolls;
            this.color = color;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public int minSteps() {
            return minSteps;
        }

        public int maxSteps() {
            return maxSteps;
        }

        public int minRolls() {
            return minRolls;
        }

        public int maxRolls() {
            return maxRolls;
        }

        public int color() {
            return color;
        }

        public String scrollItem() {
            return "clue_scroll_" + id();
        }

        public String casketItem() {
            return "reward_casket_" + id();
        }

        public static Tier byScroll(String itemId) {
            for (Tier t : values()) {
                if (t.scrollItem().equals(itemId)) {
                    return t;
                }
            }
            return null;
        }

        public static Tier byCasket(String itemId) {
            for (Tier t : values()) {
                if (t.casketItem().equals(itemId)) {
                    return t;
                }
            }
            return null;
        }
    }

    public enum StepType { COORDINATE, MAP, ANAGRAM, CRYPTIC, CHARLIE, EMOTE, CIPHER, HOT_COLD }

    /**
     * One clue step. Dig and emote steps use (x, y); talk steps use npcId (and item for Charlie steps).
     * Emote steps name the emote and may need an item carried or worn.
     */
    public record Step(StepType type, String text, int x, int y, String npcId, String npcName, String item, String emote) {
        public Step(StepType type, String text, int x, int y, String npcId, String npcName, String item) {
            this(type, text, x, y, npcId, npcName, item, null);
        }

        public boolean isDig() {
            return type == StepType.COORDINATE || type == StepType.MAP || type == StepType.HOT_COLD;
        }

        public boolean isTalk() {
            return npcId != null;
        }
    }

    /** OSRS emotes used by emote clues. */
    public static final List<String> EMOTES = List.of("Bow", "Wave", "Clap", "Cheer", "Dance", "Jig", "Spin", "Headbang", "Shrug",
            "Think", "Yawn", "Salute", "Panic", "Beckon", "Cry", "Laugh", "Jump for Joy", "Raspberry", "Blow Kiss", "Angry", "Yes",
            "No", "Goblin Salute", "Push up", "Idea", "Stamp", "Flap", "Slap Head");

    /** Step types each tier can roll (repeats weight them), after the OSRS wiki. */
    public static List<StepType> types(Tier tier) {
        return switch (tier) {
            case BEGINNER -> List.of(StepType.ANAGRAM, StepType.CRYPTIC, StepType.EMOTE, StepType.HOT_COLD, StepType.MAP, StepType.CHARLIE);
            case EASY -> List.of(StepType.ANAGRAM, StepType.CRYPTIC, StepType.CRYPTIC, StepType.EMOTE, StepType.EMOTE, StepType.MAP);
            case MEDIUM, HARD -> List.of(StepType.ANAGRAM, StepType.CIPHER, StepType.COORDINATE, StepType.COORDINATE, StepType.CRYPTIC,
                    StepType.EMOTE, StepType.MAP);
            case ELITE -> List.of(StepType.ANAGRAM, StepType.COORDINATE, StepType.COORDINATE, StepType.CRYPTIC, StepType.EMOTE,
                    StepType.MAP);
            case MASTER -> List.of(StepType.ANAGRAM, StepType.COORDINATE, StepType.COORDINATE, StepType.CRYPTIC, StepType.EMOTE,
                    StepType.HOT_COLD);
        };
    }

    /** An NPC that can be the target of a talk step. */
    public record Npc(String id, String name, int x, int y) {
    }

    record Spot(String description, int x, int y) {
    }

    private static final List<Spot> NEAR = List.of(
            new Spot("the courtyard of Lumbridge Castle", 3222, 3226), new Spot("the windmill north of Lumbridge", 3166, 3304),
            new Spot("Draynor Village market", 3081, 3251), new Spot("the Champions' Guild", 3191, 3360),
            new Spot("Varrock Palace garden", 3215, 3474), new Spot("Barbarian Village", 3082, 3420),
            new Spot("Falador Park", 3005, 3376), new Spot("Rimmington mine", 2977, 3240),
            new Spot("the Port Sarim docks", 3029, 3205), new Spot("the Al Kharid toll gate", 3268, 3227),
            new Spot("the Wizards' Tower", 3109, 3152), new Spot("the Lumbridge Swamp", 3166, 3172),
            new Spot("Edgeville Monastery", 3051, 3484));
    private static final List<Spot> MID = List.of(
            new Spot("the flax field of Seers' Village", 2741, 3443), new Spot("Catherby beach", 2853, 3428),
            new Spot("Camelot Castle", 2757, 3488), new Spot("East Ardougne market", 2662, 3305),
            new Spot("Yanille", 2606, 3093), new Spot("the Tree Gnome Stronghold", 2461, 3444),
            new Spot("Taverley", 2895, 3445), new Spot("Burthorpe", 2899, 3544), new Spot("Shilo Village", 2852, 2955),
            new Spot("Brimhaven", 2760, 3178), new Spot("Canifis", 3496, 3488), new Spot("Pollnivneach", 3359, 2970),
            new Spot("Rellekka", 2660, 3657));
    private static final List<Spot> FAR = List.of(
            new Spot("the Lava Maze", 3089, 3866), new Spot("the Mage Arena", 3105, 3934),
            new Spot("Port Phasmatys", 3687, 3502), new Spot("Lletya", 2340, 3170), new Spot("Nardah", 3428, 2892),
            new Spot("Trollheim", 2890, 3676), new Spot("the Barrows", 3565, 3288), new Spot("the TzHaar entrance", 2845, 3170),
            new Spot("the Wilderness Agility Course", 2998, 3913), new Spot("the Chaos Temple in the Wilderness", 3236, 3635),
            new Spot("Ape Atoll", 2755, 2780), new Spot("Mort'ton", 3489, 3288));
    private static final List<Spot> EXOTIC = List.of(
            new Spot("Lunar Isle", 2100, 3914), new Spot("Zul-Andra", 2197, 3055), new Spot("Fossil Island", 3724, 3808),
            new Spot("Kourend Castle", 1643, 3673), new Spot("the Wintertodt camp", 1630, 3940),
            new Spot("Piscatoris", 2340, 3650), new Spot("Iorwerth Camp", 2193, 3257),
            new Spot("Mos Le'Harmless", 3700, 2996), new Spot("the Fremennik Isles", 2335, 3800));

    /** Items a Charlie-style step can ask for (all exist in the core catalog). */
    private static final Map<Tier, List<String>> FETCH = Map.of(
            Tier.BEGINNER, List.of("raw_shrimps", "raw_anchovies", "bronze_dagger", "leather_boots", "logs", "copper_ore", "bones"),
            Tier.EASY, List.of("trout", "pike", "iron_dagger", "leather_body", "oak_logs", "iron_ore", "leather_cowl", "steel_full_helm"),
            Tier.MEDIUM, List.of("lobster", "steel_platebody", "maple_shortbow", "mithril_bar", "willow_logs", "sapphire"),
            Tier.HARD, List.of("swordfish", "rune_dagger", "yew_longbow", "adamantite_bar", "emerald", "magic_logs"),
            Tier.ELITE, List.of("shark", "rune_platebody", "magic_longbow", "runite_bar", "ruby"),
            Tier.MASTER, List.of("anglerfish", "dragon_dagger", "dragonstone", "dragon_bones"));

    /** Hand-written cryptic clues for the core quest characters. */
    private static final Map<String, String> CRYPTIC = Map.ofEntries(
            Map.entry("COOK", "The Duke's birthday cake will not bake itself. Ask the one who stirs his pots."),
            Map.entry("DUKE_HORACIO", "His castle stands by the River Lum. Speak to the man who rules it."),
            Map.entry("FATHER_AERECK", "In Lumbridge church, a man of the cloth keeps your next clue."),
            Map.entry("RESTLESS_GHOST", "He cannot rest until his skull is found. Speak to the spirit by the church."),
            Map.entry("FRED_THE_FARMER", "His sheep need shearing and his chickens need feeding, north of the castle."),
            Map.entry("FATHER_URHNEY", "A grumpy hermit in the swamp would rather you went away."),
            Map.entry("SEDRIDOR", "Head of the tower, keeper of rune mysteries."),
            Map.entry("WIZARD_MIZGOG", "He has lost his beads to the imps. Find him at the tower."),
            Map.entry("WIZARD_TRAIBORN", "A forgetful wizard in the tower's halls could use a few bones."),
            Map.entry("AUBURY", "He sells runes in Varrock and knows the way to the essence mine."),
            Map.entry("ROMEO", "A lovestruck youth waits in Varrock square."),
            Map.entry("JULIET", "She waits on her balcony west of Varrock for a message."),
            Map.entry("FATHER_LAWRENCE", "Varrock's priest sleeps through his own sermons."),
            Map.entry("APOTHECARY", "He brews potions in south-west Varrock, some more deadly than others."),
            Map.entry("DR_HARLOW", "A former vampire hunter drowns his sorrows in a Varrock pub."),
            Map.entry("GYPSY_ARIS", "She sees the future in her crystal ball, for a silver coin."),
            Map.entry("SIR_PRYSIN", "Silverlight was entrusted to this knight of Varrock Palace."),
            Map.entry("DORIC", "A dwarf smith north of Falador wants clay, copper and iron."),
            Map.entry("HETTY", "Rimmington's witch brews in her cauldron."),
            Map.entry("KAQEMEEX", "A druid by the stone circle north of Taverley."),
            Map.entry("SANFEW", "Upstairs in Taverley, a druid awaits the ingredients of a ritual."),
            Map.entry("MORGAN", "A worried man of Draynor fears the count in the manor."));

    private ClueScrolls() {
    }

    // ---------------- NPCs ----------------

    /** NPCs that exist in the world: the core quest characters and every module NPC with a resolvable place. */
    public static List<Npc> npcs() {
        List<Npc> out = new ArrayList<>();
        for (QuestNpc q : QuestNpc.values()) {
            Landmark l = q.landmark();
            out.add(new Npc(q.name(), q.displayName(), l.osrsX() + q.dx() / 2, l.osrsY() - q.dz() / 2));
        }
        for (NpcDef n : ContentRegistry.npcs()) {
            int[] at = placeCoords(n.place());
            if (at != null) {
                out.add(new Npc(n.id(), n.name(), at[0] + n.dx() / 2, at[1] - n.dz() / 2));
            }
        }
        return out;
    }

    static int[] placeCoords(String place) {
        if (place == null) {
            return null;
        }
        Landmark l = Landmark.byName(place);
        if (l != null) {
            return new int[]{l.osrsX(), l.osrsY()};
        }
        PlaceDef p = ContentRegistry.place(place);
        return p == null ? null : new int[]{p.osrsX(), p.osrsY()};
    }

    // ---------------- Generation ----------------

    public static int stepCount(Tier tier, Random random) {
        return tier.minSteps() + random.nextInt(tier.maxSteps() - tier.minSteps() + 1);
    }

    /** Generates the full trail for a clue from its seed. */
    public static List<Step> generate(Tier tier, long seed, List<Npc> npcs) {
        Random random = new Random(seed);
        int n = stepCount(tier, random);
        List<Step> steps = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            steps.add(step(tier, random, npcs));
        }
        return steps;
    }

    static Step step(Tier tier, Random random, List<Npc> npcs) {
        List<StepType> types = types(tier);
        StepType type = types.get(random.nextInt(types.size()));
        if ((type == StepType.ANAGRAM || type == StepType.CRYPTIC || type == StepType.CHARLIE || type == StepType.CIPHER) && npcs.isEmpty()) {
            type = StepType.MAP;
        }
        switch (type) {
            case COORDINATE -> {
                Spot s = spot(tier, random);
                int x = s.x() + random.nextInt(9) - 4;
                int y = s.y() + random.nextInt(9) - 4;
                return new Step(type, coordinateText(x, y), x, y, null, null, null);
            }
            case MAP -> {
                Spot s = spot(tier, random);
                return new Step(type, "The map shows a cross marked near " + s.description() + ". Dig there with a spade.",
                        s.x(), s.y(), null, null, null);
            }
            case ANAGRAM -> {
                Npc npc = npcs.get(random.nextInt(npcs.size()));
                return new Step(type, "This anagram reveals who to speak to next: " + anagram(npc.name(), random), npc.x(), npc.y(),
                        npc.id(), npc.name(), null);
            }
            case CRYPTIC -> {
                Npc npc = npcs.get(random.nextInt(npcs.size()));
                return new Step(type, cryptic(npc), npc.x(), npc.y(), npc.id(), npc.name(), null);
            }
            case CIPHER -> {
                Npc npc = npcs.get(random.nextInt(npcs.size()));
                return new Step(type, "The cipher reads: " + cipher(npc.name(), 1 + random.nextInt(25)), npc.x(), npc.y(), npc.id(),
                        npc.name(), null);
            }
            case HOT_COLD -> {
                Spot s = spot(tier, random);
                int x = s.x() + random.nextInt(9) - 4;
                int y = s.y() + random.nextInt(9) - 4;
                return new Step(type, "Buried treasure awaits " + (tier == Tier.BEGINNER ? "somewhere in the lands around Lumbridge"
                        : "somewhere in Gielinor") + ". Feel the strange device to find out how close you are, then dig.", x, y, null,
                        null, null);
            }
            case EMOTE -> {
                Spot s = spot(tier, random);
                String emote = EMOTES.get(random.nextInt(EMOTES.size()));
                String item = null;
                if (tier != Tier.BEGINNER) {
                    List<String> equip = FETCH.get(tier);
                    item = equip.get(random.nextInt(equip.size()));
                }
                return new Step(type, emote + " at " + s.description() + "." + (item == null ? "" : " Equip or carry: "
                        + item.replace('_', ' ') + "."), s.x(), s.y(), null, null, item, emote);
            }
            default -> {
                Npc npc = npcs.get(random.nextInt(npcs.size()));
                List<String> fetch = FETCH.get(tier);
                String item = fetch.get(random.nextInt(fetch.size()));
                return new Step(StepType.CHARLIE, "Talk to " + npc.name() + " while carrying: " + item.replace('_', ' ') + ".",
                        npc.x(), npc.y(), npc.id(), npc.name(), item);
            }
        }
    }

    private static Spot spot(Tier tier, Random random) {
        List<Spot> pool = switch (tier) {
            case BEGINNER, EASY -> NEAR;
            case MEDIUM -> random.nextBoolean() ? NEAR : MID;
            case HARD -> random.nextBoolean() ? MID : FAR;
            case ELITE -> random.nextInt(3) == 0 ? MID : random.nextBoolean() ? FAR : EXOTIC;
            case MASTER -> random.nextBoolean() ? FAR : EXOTIC;
        };
        return pool.get(random.nextInt(pool.size()));
    }

    static String cryptic(Npc npc) {
        String known = CRYPTIC.get(npc.id());
        if (known != null) {
            return known;
        }
        String name = npc.name();
        int letters = name.replaceAll("[^A-Za-z]", "").length();
        return "Seek one whose name has " + letters + " letters and begins with '" + Character.toUpperCase(name.charAt(0))
                + "'. They wait " + direction(npc.x(), npc.y()) + ".";
    }

    /** A rough description of where coordinates are relative to Lumbridge. */
    static String direction(int x, int y) {
        int dx = x - 3222;
        int dy = y - 3218;
        if (Math.abs(dx) < 60 && Math.abs(dy) < 60) {
            return "not far from Lumbridge";
        }
        String ns = dy > 60 ? "north" : dy < -60 ? "south" : "";
        String ew = dx > 60 ? "east" : dx < -60 ? "west" : "";
        return "far to the " + ns + (ns.isEmpty() || ew.isEmpty() ? "" : "-") + ew + " of Lumbridge";
    }

    /** OSRS cipher clues: the NPC's name Caesar-shifted by {@code shift} letters, in capitals. */
    public static String cipher(String name, int shift) {
        StringBuilder sb = new StringBuilder();
        for (char c : name.toUpperCase(Locale.ROOT).toCharArray()) {
            sb.append(c >= 'A' && c <= 'Z' ? (char) ('A' + (c - 'A' + shift) % 26) : c);
        }
        return sb.toString();
    }

    /** Hot/cold strange device reading for a distance in tiles (OSRS temperatures). */
    public static String temperature(int distance) {
        if (distance <= 4) {
            return "incredibly hot";
        }
        if (distance <= 10) {
            return "very hot";
        }
        if (distance <= 30) {
            return "hot";
        }
        if (distance <= 70) {
            return "warm";
        }
        if (distance <= 150) {
            return "cold";
        }
        if (distance <= 300) {
            return "very cold";
        }
        return "ice cold";
    }

    /** Emote steps are performed within this many tiles of the spot. */
    public static final int EMOTE_RADIUS = 6;

    /** Every dig/emote spot of every tier (for tests). */
    public static List<int[]> allSpots() {
        List<int[]> out = new ArrayList<>();
        for (List<Spot> pool : List.of(NEAR, MID, FAR, EXOTIC)) {
            for (Spot s : pool) {
                out.add(new int[]{s.x(), s.y()});
            }
        }
        return out;
    }

    /** Ids of the characters the hand-written cryptic clues point to (for tests). */
    public static java.util.Set<String> crypticTargets() {
        return CRYPTIC.keySet();
    }

    /** Items emote/Charlie steps can ask for (for tests). */
    public static List<String> fetchItems(Tier tier) {
        return FETCH.get(tier);
    }

    /** Scrambles a name into an upper-case anagram (never the name itself). */
    public static String anagram(String name, Random random) {
        char[] letters = name.toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "").toCharArray();
        String original = new String(letters);
        for (int attempt = 0; attempt < 10; attempt++) {
            for (int i = letters.length - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                char t = letters[i];
                letters[i] = letters[j];
                letters[j] = t;
            }
            if (!new String(letters).equals(original) || letters.length < 2) {
                break;
            }
        }
        StringBuilder sb = new StringBuilder(new String(letters));
        if (sb.length() > 5) {
            sb.insert(sb.length() / 2, ' ');
        }
        return sb.toString();
    }

    /** OSRS coordinate clue text: degrees and minutes from the Observatory (2440, 3161), 1.875 tiles per minute. */
    public static String coordinateText(int x, int y) {
        int[] ns = degMin(y - 3161);
        int[] ew = degMin(x - 2440);
        return String.format(Locale.ROOT, "%02d degrees %02d minutes %s, %02d degrees %02d minutes %s", ns[0], ns[1],
                y >= 3161 ? "north" : "south", ew[0], ew[1], x >= 2440 ? "east" : "west");
    }

    static int[] degMin(int tiles) {
        int minutes = (int) Math.round(Math.abs(tiles) / 1.875);
        return new int[]{minutes / 60, minutes % 60};
    }

    /** Inverse of {@link #coordinateText}: tiles from the Observatory for the given degrees/minutes. */
    public static int tilesFrom(int degrees, int minutes) {
        return (int) Math.round((degrees * 60 + minutes) * 1.875);
    }

    /** Dig steps succeed within this many tiles of the spot. */
    public static final int DIG_RADIUS = 4;

    /** Clue scroll drop rate (1 in n) for a monster of the given combat level, or 0. Based on OSRS drop tables. */
    public static int dropOneIn(Tier tier, int combatLevel) {
        return switch (tier) {
            case BEGINNER -> combatLevel <= 25 ? 64 : 0;
            case EASY -> combatLevel <= 60 ? 128 : 0;
            case MEDIUM -> combatLevel >= 15 && combatLevel <= 120 ? 128 : 0;
            case HARD -> combatLevel >= 60 ? 128 : 0;
            case ELITE -> combatLevel >= 100 ? 400 : 0;
            case MASTER -> 0;
        };
    }
}
