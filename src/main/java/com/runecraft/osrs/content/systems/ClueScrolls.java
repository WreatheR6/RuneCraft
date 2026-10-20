package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.QuestNpc;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.Monster;
import com.runecraft.osrs.world.Elevation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Treasure Trails: clue tiers and the real OSRS clue steps ({@link ClueStepData}, extracted from RuneLite's
 * cluescrolls plugin, which has every clue's text, tile, NPC, emotes and required items). A trail draws its steps
 * from the tier's step list the way OSRS does, one clue scroll id at a time (each coordinate, map, anagram, cipher,
 * cryptic, emote and music clue is one id; hot/cold, Charlie the Tramp, Falo the Bard and master Sherlock are one id
 * with a random location, item, song or challenge; elite Sherlock has 15 ids). Steps whose tile is not on the
 * surface of this world (dungeons, instances), or whose NPC does not stand here, are left out of the pool.
 *
 * <p>Talking to an anagram, cipher or cryptic NPC on medium and harder clues hands over a challenge scroll when OSRS
 * has a question for it, otherwise (hard and up) a puzzle box, or on master clues a light box or a puzzle box. Hard
 * and harder coordinate digs, master hot/cold digs and hard/master emote steps that say "Beware of double agents!"
 * summon the wiki's guardian first.
 */
public final class ClueScrolls {
    public enum Tier {
        BEGINNER(1, 3, 1, 3, 0xC0B080, 12, 150, "plank"), EASY(2, 4, 2, 4, 0x40A040, 27, 150, "plank"),
        MEDIUM(3, 5, 3, 5, 0x40A0C0, 42, 250, "oak_plank"), HARD(4, 6, 4, 6, 0xA040C0, 55, 400, "teak_plank"),
        ELITE(5, 7, 4, 6, 0xE0C030, 77, 600, "mahogany_plank"), MASTER(6, 8, 5, 7, 0xC03030, 88, 1500, "mahogany_plank");

        private final int minSteps;
        private final int maxSteps;
        private final int minRolls;
        private final int maxRolls;
        private final int color;
        private final int stashLevel;
        private final int stashXp;
        private final String stashPlank;

        Tier(int minSteps, int maxSteps, int minRolls, int maxRolls, int color, int stashLevel, int stashXp, String stashPlank) {
            this.minSteps = minSteps;
            this.maxSteps = maxSteps;
            this.minRolls = minRolls;
            this.maxRolls = maxRolls;
            this.color = color;
            this.stashLevel = stashLevel;
            this.stashXp = stashXp;
            this.stashPlank = stashPlank;
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

        /** Construction level to build this tier's STASH units (OSRS Wiki "STASH"). */
        public int stashLevel() {
            return stashLevel;
        }

        /** Construction XP for building one of this tier's STASH units. */
        public int stashXp() {
            return stashXp;
        }

        /** The plank a STASH unit of this tier takes (two of them, ten nails; master units also a gold leaf). */
        public String stashPlank() {
            return stashPlank;
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

    /** OSRS step kinds. PUZZLE and LIGHT_BOX are no longer steps of their own (NPCs hand the boxes over); kept for ordinals. */
    public enum StepType { COORDINATE, MAP, ANAGRAM, CRYPTIC, CHARLIE, EMOTE, CIPHER, HOT_COLD, PUZZLE, LIGHT_BOX, FALO,
        MUSIC, SHERLOCK, FAIRY_RING }

    /** What the player does to finish a step. */
    public enum Action { DIG, SEARCH, TALK, EMOTE, KILL }

    /** The clue guardians (OSRS Wiki "Treasure Trails", Antagonists), as RuneLite tags each step. */
    public enum Guardian {
        DOUBLE_AGENT_65("Double agent", "double_agent_65"),
        DOUBLE_AGENT_108("Double agent", "double_agent_108"),
        DOUBLE_AGENT_141("Double agent", "double_agent_141"),
        ZAMORAK_WIZARD("Zamorak wizard", "zamorak_wizard"),
        SARADOMIN_WIZARD("Saradomin wizard", "saradomin_wizard"),
        ARMADYLEAN_OR_BANDOSIAN_GUARD("Armadylean or Bandosian guard", "armadylean_guard", "bandosian_guard"),
        ARMADYLEAN_GUARD("Armadylean guard", "armadylean_guard"),
        BANDOSIAN_GUARD("Bandosian guard", "bandosian_guard"),
        BRASSICAN_MAGE("Brassican Mage", "brassican_mage"),
        ANCIENT_WIZARDS("Ancient Wizards", ANCIENT_TRIO),
        BRASSICAN_OR_WIZARDS("Brassican Mage or Ancient Wizards", "brassican_mage", ANCIENT_TRIO);

        private final String display;
        private final List<String> options;

        Guardian(String display, String... options) {
            this.display = display;
            this.options = List.of(options);
        }

        public String display() {
            return display;
        }

        /** The monster ids that appear: one option at random (an option may be a group joined by "+"). */
        public List<String> spawn(Random random) {
            return List.of(options.get(random.nextInt(options.size())).split("\\+"));
        }

        /** Every monster id this guardian can be. */
        public Set<String> monsterIds() {
            Set<String> out = new java.util.LinkedHashSet<>();
            for (String o : options) {
                out.addAll(List.of(o.split("\\+")));
            }
            return out;
        }

        public boolean doubleAgent() {
            return name().startsWith("DOUBLE_AGENT");
        }

        static Guardian parse(String s) {
            return s == null || s.isEmpty() ? null : valueOf(s);
        }
    }

    private static final String ANCIENT_TRIO = "ancient_wizard_mage+ancient_wizard_ranger+ancient_wizard_melee";

    /** Every clue guardian monster id. */
    public static Set<String> guardianMonsters() {
        Set<String> out = new java.util.LinkedHashSet<>();
        for (Guardian g : Guardian.values()) {
            out.addAll(g.monsterIds());
        }
        return out;
    }

    /** One OSRS clue (a row of {@link ClueStepData}). */
    public record Clue(int index, StepType type, Tier tier, int x, int y, int plane, String npc, String object, Guardian guardian,
                       List<String> emotes, String stash, String answer, String text, String solution, String question,
                       String task, String req) {
        /** What the player has to do. */
        public Action action() {
            return switch (type) {
                case COORDINATE, MAP, FAIRY_RING, HOT_COLD -> object.isEmpty() ? Action.DIG : Action.SEARCH;
                case EMOTE -> Action.EMOTE;
                case CRYPTIC -> switch (task) {
                    case "dig" -> Action.DIG;
                    case "search", "key" -> Action.SEARCH;
                    case "kill" -> Action.KILL;
                    default -> Action.TALK;
                };
                default -> Action.TALK;
            };
        }
    }

    /** A STASH unit: where its emote clue's items can be stored. */
    public record Stash(String name, Tier tier, int x, int y, int plane) {
    }

    /** An NPC that can be the target of a talk step. */
    public record Npc(String id, String name, int x, int y) {
    }

    /**
     * One step of a player's trail: the OSRS clue, the NPC that stands for it here, what the NPC hands over ({@link
     * CluePuzzles#PUZZLE_BOX}, {@link CluePuzzles#LIGHT_BOX} or null) and the monster whose key a key step needs.
     */
    public record Step(Clue clue, String npcId, String npcName, String box, String keyMonster) {
        public StepType type() {
            return clue.type();
        }

        public Tier tier() {
            return clue.tier();
        }

        public Action action() {
            return clue.action();
        }

        public int x() {
            return clue.x();
        }

        public int y() {
            return clue.y();
        }

        public boolean isDig() {
            return action() == Action.DIG;
        }

        public boolean isTalk() {
            return action() == Action.TALK;
        }

        public Guardian guardian() {
            return clue.guardian();
        }

        public List<String> emotes() {
            return clue.emotes();
        }

        public String emote() {
            return clue.emotes().isEmpty() ? null : clue.emotes().get(0);
        }

        /** The item requirement ({@link ClueReqs} format), or "". */
        public String req() {
            return clue.req();
        }

        /** Whether this step's NPC sets a challenge question before giving the next clue. */
        public boolean hasChallenge() {
            return !clue.answer().isEmpty() && (clue.type() == StepType.ANAGRAM || clue.type() == StepType.CIPHER
                    || clue.type() == StepType.CRYPTIC);
        }

        /** The text written on the clue scroll. */
        public String text() {
            Clue c = clue;
            return switch (c.type()) {
                case ANAGRAM -> (c.tier() == Tier.BEGINNER ? "The anagram reveals who to speak to next: "
                        : "This anagram reveals who to speak to next: ") + c.text();
                case CIPHER -> "The cipher reveals who to speak to next: " + c.text();
                case COORDINATE -> coordinateText(c.x(), c.y());
                case MAP -> "The map shows: " + c.solution() + (c.object().isEmpty() ? " X marks the spot." : " A cross marks a "
                        + objectName(c.object()) + ".");
                case FAIRY_RING -> "The cryptic reveals where to dig: " + c.text();
                case HOT_COLD -> "Buried beneath the ground, who knows where it's found. Lucky for you, a man called "
                        + (c.tier() == Tier.BEGINNER ? "Reldo" : "Jorral") + " may have a clue.";
                case MUSIC -> "Cecilia, in Falador Park, would like to hear the song: \"" + c.text() + "\".";
                case FALO -> "Falo the bard wants to see you.";
                case CHARLIE -> "Talk to Charlie the Tramp in Varrock.";
                case SHERLOCK -> "Show this to Sherlock.";
                default -> c.text();
            };
        }
    }

    /** Clue NPC aliases: RuneLite's name -> the name the character has here. */
    private static final Map<String, String> ALIASES = Map.of(
            "archmage sedridor", "sedridor",
            "lumbridge guide", "lumbridge guide");

    /** OSRS dungeon tiles lie 6400 tiles north of the surface above them. */
    static final int DUNGEON_Y = 6400;

    /** Steps whose tile is this far from the NPC we stand them on are left out (OSRS NPCs wander about 5-10 tiles). */
    public static final int NPC_RANGE = 48;

    /** Dig, search and hot/cold steps succeed within this many tiles of the spot (a block world is coarser than OSRS). */
    public static final int DIG_RADIUS = 4;
    /** Emote steps are performed within this many tiles of the spot. */
    public static final int EMOTE_RADIUS = 6;

    private static List<Clue> clues;
    private static Map<String, Stash> stash;
    private static final Map<Tier, List<Entry>> POOLS = new EnumMap<>(Tier.class);
    private static List<Npc> poolNpcs;

    private ClueScrolls() {
    }

    // ------------------------------------------------------------------ data

    /** Every OSRS clue step (all tiers, also the ones this world cannot host). */
    public static synchronized List<Clue> clues() {
        if (clues == null) {
            List<Clue> out = new ArrayList<>();
            for (String chunk : ClueStepData.ROWS) {
                for (String line : chunk.split("\n")) {
                    if (line.isBlank()) {
                        continue;
                    }
                    String[] f = java.util.Arrays.copyOf(line.split("\t", -1), 16);
                    for (int i = 0; i < f.length; i++) {
                        f[i] = f[i] == null ? "" : f[i];
                    }
                    out.add(new Clue(out.size(), StepType.valueOf(f[0]), Tier.valueOf(f[1].toUpperCase(Locale.ROOT)),
                            Integer.parseInt(f[2]), Integer.parseInt(f[3]), Integer.parseInt(f[4]), f[5], f[6], Guardian.parse(f[7]),
                            f[8].isEmpty() ? List.of() : List.of(f[8].split("\\|")), f[9], f[10], f[11], f[12], f[13], f[14], f[15]));
                }
            }
            clues = Collections.unmodifiableList(out);
        }
        return clues;
    }

    /** STASH units by name (RuneLite STASHUnit). */
    public static synchronized Map<String, Stash> stashUnits() {
        if (stash == null) {
            Map<String, Stash> out = new LinkedHashMap<>();
            for (String line : ClueStepData.STASH.split("\n")) {
                String[] f = line.split("\t");
                if (f.length == 5) {
                    out.put(f[0], new Stash(f[0], Tier.valueOf(f[1].toUpperCase(Locale.ROOT)), Integer.parseInt(f[2]),
                            Integer.parseInt(f[3]), Integer.parseInt(f[4])));
                }
            }
            stash = Collections.unmodifiableMap(out);
        }
        return stash;
    }

    /** A readable name for a RuneLite object id ("CRATE2_OLD" -> "crate"). */
    public static String objectName(String object) {
        String o = object.toLowerCase(Locale.ROOT);
        for (String k : new String[]{"drawers", "crate", "chest", "bookcase", "boxes", "box", "barrel", "bucket", "cupboard",
                "wardrobe", "coffin", "haystack", "hay", "rack", "sack", "cabinet", "shelves", "stone", "bush", "rock", "pot", "table",
                "log", "boulder", "tree"}) {
            if (o.contains(k)) {
                return k.equals("hay") ? "haystack" : k;
            }
        }
        return "spot";
    }

    // ------------------------------------------------------------------ NPCs

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

    static String norm(String name) {
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT).replace('’', '\'');
        n = n.replaceAll("[^a-z0-9' ]", " ").replaceAll("\\s+", " ").trim();
        return ALIASES.getOrDefault(n, n);
    }

    /** The NPC of this world standing for a clue's NPC: same name, nearest to the clue's tile (dungeon tiles folded up). */
    public static Npc resolveNpc(String name, int x, int y, List<Npc> npcs) {
        String want = norm(name);
        int ty = y >= DUNGEON_Y ? y - DUNGEON_Y : y;
        Npc best = null;
        double bestD = Double.MAX_VALUE;
        for (Npc n : npcs) {
            if (!norm(n.name()).equals(want)) {
                continue;
            }
            int ny = n.y() >= DUNGEON_Y ? n.y() - DUNGEON_Y : n.y();
            double d = Math.hypot(n.x() - x, ny - ty);
            if (d < bestD) {
                bestD = d;
                best = n;
            }
        }
        return best != null && bestD <= NPC_RANGE ? best : null;
    }

    private static Set<String> monsterNames;

    /** Every monster name this world has (normalised), for kill steps and key steps. */
    public static synchronized Set<String> monsterNames() {
        if (monsterNames == null) {
            Set<String> out = new HashSet<>();
            for (Monster m : Monster.values()) {
                out.add(norm(m.displayName()));
            }
            for (MonsterDef m : ContentRegistry.monsters()) {
                out.add(norm(m.name));
            }
            for (BossDef b : ContentRegistry.bosses()) {
                out.add(norm(b.name));
            }
            monsterNames = out;
        }
        return monsterNames;
    }

    /** Whether a killed monster's name counts for a clue's monster (alternatives split by "|"). */
    public static boolean monsterMatches(String clueMonster, String killedName) {
        if (clueMonster == null || clueMonster.isEmpty()) {
            return false;
        }
        String k = norm(killedName);
        for (String alt : clueMonster.split("\\|")) {
            if (norm(alt).equals(k)) {
                return true;
            }
        }
        return false;
    }

    /** Whether any of a clue's monsters ("|" alternatives) lives in this world. */
    public static boolean monsterExists(String clueMonster) {
        for (String alt : clueMonster.split("\\|")) {
            if (!alt.isBlank() && monsterNames().contains(norm(alt))) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ the step pool

    /** One OSRS clue scroll id of a tier: its variants (one for most clues; every location/item/song for the shared ones). */
    record Entry(List<Clue> variants, int weight) {
    }

    /** Whether a tile is on this world's surface (no dungeons, instances or upper-plane-only areas) and on land. */
    public static boolean onSurfaceLand(int x, int y) {
        if (y >= 4160 || x < 1000 || x > 4000 || y < 2400) {
            return false;
        }
        for (int dx = -DIG_RADIUS; dx <= DIG_RADIUS; dx += 2) {
            for (int dy = -DIG_RADIUS; dy <= DIG_RADIUS; dy += 2) {
                if (!Elevation.isOcean(GielinorMap.toBlockX(x + dx), GielinorMap.toBlockZ(y + dy))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Why a clue can't be part of a trail in this world, or null if it can. */
    public static String unusable(Clue c, List<Npc> npcs) {
        boolean npcStep = c.type() == StepType.ANAGRAM || c.type() == StepType.CIPHER || c.type() == StepType.MUSIC
                || c.type() == StepType.FALO || c.type() == StepType.CHARLIE || c.type() == StepType.SHERLOCK
                || (c.type() == StepType.CRYPTIC && c.action() == Action.TALK);
        if (npcStep) {
            if (c.type() == StepType.CRYPTIC && (c.task().equals("sherlock") || c.task().equals("hotcold"))) {
                return "shared entry";
            }
            Npc n = resolveNpc(c.npc(), c.x(), c.y(), npcs);
            if (n == null) {
                return "no NPC " + c.npc();
            }
            int[] at = new int[]{n.x(), n.y() >= DUNGEON_Y ? n.y() - DUNGEON_Y : n.y()};
            if (!onSurfaceLand(at[0], at[1])) {
                return "NPC off the surface";
            }
            if ((c.type() == StepType.FALO || c.type() == StepType.CHARLIE) && ClueReqs.groups(c.req()).isEmpty()) {
                return "item missing";
            }
            if (c.type() == StepType.SHERLOCK && ClueChallenges.of(c.text()) == null) {
                return "challenge not modelled";
            }
            return null;
        }
        if (c.action() == Action.KILL) {
            return monsterExists(c.npc()) ? null : "no monster " + c.npc();
        }
        if (!onSurfaceLand(c.x(), c.y())) {
            return "not on the surface";
        }
        return null;
    }

    /** The tier's clue scroll ids that this world can host, with their OSRS weights. */
    static synchronized List<Entry> pool(Tier tier, List<Npc> npcs) {
        if (poolNpcs != npcs) {
            POOLS.clear();
            poolNpcs = npcs;
        }
        return POOLS.computeIfAbsent(tier, t -> {
            List<Entry> out = new ArrayList<>();
            Map<StepType, List<Clue>> shared = new EnumMap<>(StepType.class);
            for (Clue c : clues()) {
                if (c.tier() != t || unusable(c, npcs) != null) {
                    continue;
                }
                if (c.type() == StepType.HOT_COLD || c.type() == StepType.CHARLIE || c.type() == StepType.FALO
                        || c.type() == StepType.SHERLOCK) {
                    shared.computeIfAbsent(c.type(), k -> new ArrayList<>()).add(c);
                } else {
                    out.add(new Entry(List.of(c), 1));
                }
            }
            shared.forEach((type, list) -> out.add(new Entry(List.copyOf(list),
                    type == StepType.SHERLOCK && t == Tier.ELITE ? 15 : 1)));
            return List.copyOf(out);
        });
    }

    /** The clues a tier's trails can use in this world (for tests and the self-test). */
    public static List<Clue> usableClues(Tier tier, List<Npc> npcs) {
        List<Clue> out = new ArrayList<>();
        for (Entry e : pool(tier, npcs)) {
            out.addAll(e.variants());
        }
        return out;
    }

    // ------------------------------------------------------------------ generation

    public static int stepCount(Tier tier, Random random) {
        return tier.minSteps() + random.nextInt(tier.maxSteps() - tier.minSteps() + 1);
    }

    /** Generates the full trail for a clue from its seed. OSRS draws each step independently (repeats can happen). */
    public static List<Step> generate(Tier tier, long seed, List<Npc> npcs) {
        Random random = new Random(seed);
        int n = stepCount(tier, random);
        List<Entry> pool = pool(tier, npcs);
        int total = 0;
        for (Entry e : pool) {
            total += e.weight();
        }
        List<Step> steps = new ArrayList<>();
        Clue last = null;
        for (int i = 0; i < n && total > 0; i++) {
            Clue c;
            int guard = 0;
            do {
                int r = random.nextInt(total);
                Entry pick = pool.get(0);
                for (Entry e : pool) {
                    r -= e.weight();
                    if (r < 0) {
                        pick = e;
                        break;
                    }
                }
                c = pick.variants().get(random.nextInt(pick.variants().size()));
            } while (c.equals(last) && ++guard < 8); // never the same clue twice in a row
            last = c;
            steps.add(step(c, random, npcs));
        }
        return steps;
    }

    /** Builds a trail step for one clue: resolves its NPC, the box it hands over and the key monster. */
    public static Step step(Clue c, Random random, List<Npc> npcs) {
        String npcId = null;
        String npcName = null;
        if (c.action() == Action.TALK) {
            Npc n = resolveNpc(c.npc(), c.x(), c.y(), npcs);
            if (n != null) {
                npcId = n.id();
                npcName = n.name();
            }
        }
        String box = null;
        boolean boxable = c.type() == StepType.ANAGRAM || c.type() == StepType.CIPHER
                || (c.type() == StepType.CRYPTIC && c.action() == Action.TALK);
        if (c.type() == StepType.CRYPTIC && c.task().equals("puzzle")) {
            box = CluePuzzles.PUZZLE_BOX;
        } else if (boxable && c.answer().isEmpty() && c.type() != StepType.CRYPTIC && c.tier().ordinal() >= Tier.HARD.ordinal()) {
            box = c.tier() == Tier.MASTER && random.nextBoolean() ? CluePuzzles.LIGHT_BOX : CluePuzzles.PUZZLE_BOX;
        }
        String key = null;
        if (c.type() == StepType.CRYPTIC && c.task().equals("key") && !c.npc().isEmpty() && monsterExists(c.npc())) {
            key = c.npc();
        }
        return new Step(c, npcId, npcName, box, key);
    }

    // ------------------------------------------------------------------ text helpers

    /** OSRS cipher clues: the NPC's name Caesar-shifted by {@code shift} letters, in capitals. */
    public static String cipher(String name, int shift) {
        StringBuilder sb = new StringBuilder();
        for (char c : name.toUpperCase(Locale.ROOT).toCharArray()) {
            sb.append(c >= 'A' && c <= 'Z' ? (char) ('A' + (c - 'A' + shift) % 26) : c);
        }
        return sb.toString();
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

    // ------------------------------------------------------------------ hot/cold

    /**
     * The strange device's reading for a distance in steps (Chebyshev distance, as RuneLite's solver uses), per the
     * wiki: ice cold 500+, very cold 200+, cold 150+, warm 100+, hot 70+, very hot 30+, incredibly hot from 4 (beginner)
     * or 5 (master), visibly shaking below that.
     */
    public static String temperature(Tier tier, int distance) {
        if (distance >= 500) {
            return "ice cold";
        }
        if (distance >= 200) {
            return "very cold";
        }
        if (distance >= 150) {
            return "cold";
        }
        if (distance >= 100) {
            return "warm";
        }
        if (distance >= 70) {
            return "hot";
        }
        if (distance >= 30) {
            return "very hot";
        }
        return distance > (tier == Tier.MASTER ? 4 : 3) ? "incredibly hot" : "visibly shaking";
    }

    /** Chebyshev distance in tiles (the strange device counts steps, ignoring obstacles). */
    public static int steps(int x1, int y1, int x2, int y2) {
        return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
    }

    /** The strange device of a hot/cold tier (Reldo's beginner device, Jorral's master one). */
    public static String strangeDevice(Tier tier) {
        return tier == Tier.MASTER ? "strange_device_master" : "strange_device";
    }

    // ------------------------------------------------------------------ drops

    /**
     * Clue scroll drop rate (1 in n, 0 = none) for a monster: its OSRS wiki rate from {@link ClueDrops} when the
     * table knows it, otherwise the combat-level fallback.
     */
    public static int dropOneIn(Tier tier, String monsterName, int combatLevel) {
        int known = ClueDrops.oneIn(tier, monsterName);
        return known >= 0 ? known : dropOneIn(tier, combatLevel);
    }

    /** Clue tiers a monster's own drop table already rolls (so the generic clue roll skips them). */
    public static Set<Tier> ownClueTiers(java.util.Collection<com.runecraft.osrs.data.Drop> drops) {
        Set<Tier> out = java.util.EnumSet.noneOf(Tier.class);
        if (drops != null) {
            for (com.runecraft.osrs.data.Drop d : drops) {
                Tier t = Tier.byScroll(d.item());
                if (t != null) {
                    out.add(t);
                }
            }
        }
        return out;
    }

    /** Fallback clue scroll drop rate (1 in n) for a monster of the given combat level, or 0 (approx). */
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
