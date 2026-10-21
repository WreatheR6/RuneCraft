package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Sailing at sea, from the OSRS Wiki (July 2026): salvage sorting loot, salvaging hooks, boat combat (cannons,
 * cannonballs, sea creatures with their wiki HP, stats, keel max hits and drops, boat hitpoints and keel armour),
 * bounty tasks, ocean encounters and sailing hazards. Wiki tables live in {@link SailingSeaTables}.
 */
public final class SailingSea {
    private SailingSea() {
    }

    // ------------------------------------------------------------------ item names

    /** Wiki item names whose RuneCraft id is not the plain slug. */
    private static final Map<String, String> ALIAS = Map.ofEntries(
            Map.entry("Steel cannonball", "cannonball"), Map.entry("Adamantite nails", "adamant_nails"),
            Map.entry("Runite bolts", "rune_bolts"), Map.entry("Fishbowl#Water", "empty_fishbowl"),
            Map.entry("Rum", "bottle_of_rum"), Map.entry("Plundered salvage", "pirate_salvage"),
            Map.entry("Opulent salvage", "merchant_salvage"), Map.entry("Martial salvage", "mercenary_salvage"),
            Map.entry("Bird nest (egg)#Red egg", "bird_nest"), Map.entry("Bird nest (egg)#Green egg", "bird_nest"),
            Map.entry("Bird nest (egg)#Blue egg", "bird_nest"));

    /** Wiki loot that has no RuneCraft item (left out of the tables; a test checks none of them exists). */
    public static final Set<String> SKIPPED = Set.of("Feather", "Gnomeball", "Soup", "Platinum token", "Old boot",
            "Adamant halberd", "Dragon keel parts", "Large dragon keel parts", "Mudskipper hat");

    /** The RuneCraft item id of a wiki item name. */
    public static String itemId(String wikiName) {
        String a = ALIAS.get(wikiName);
        if (a != null) {
            return a;
        }
        return wikiName.toLowerCase(Locale.ROOT).replace("'", "").replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    }

    /** "1-5" or "3" (or "3-7,5-10": the first range) as {min, max}. */
    static int[] qty(String q) {
        String s = q.split(",")[0].trim();
        String[] p = s.split("-");
        int min = Integer.parseInt(p[0].trim());
        return new int[]{min, p.length > 1 ? Integer.parseInt(p[1].trim()) : min};
    }

    /** A wiki rarity as a probability ("Always" is 1; "Rare"/"Unknown" give -1). */
    static double rarity(String r) {
        if (r.equalsIgnoreCase("Always") || r.equals("100%")) {
            return 1;
        }
        String[] p = r.split("/");
        if (p.length != 2) {
            return -1;
        }
        return Double.parseDouble(p[0]) / Double.parseDouble(p[1]);
    }

    static List<String[]> rows(String table) {
        List<String[]> out = new ArrayList<>();
        for (String line : table.split("\n")) {
            if (!line.isBlank()) {
                out.add(line.split("\t", -1));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ salvage

    /** One salvage loot line: section ("Pre-roll", "Tertiary" or a main-table section), item id, quantity and chance. */
    public record SalvageLine(String salvage, String section, String wikiName, String item, int min, int max, double chance) {
        public boolean preRoll() {
            return section.equals("Pre-roll");
        }

        public boolean tertiary() {
            return section.equals("Tertiary");
        }
    }

    /** Sorting loot by salvage item id (wiki tables; lines without a RuneCraft item are left out). */
    public static final Map<String, List<SalvageLine>> SALVAGE;

    static {
        Map<String, List<SalvageLine>> m = new LinkedHashMap<>();
        for (String[] r : rows(SailingSeaTables.SALVAGE)) {
            double chance = rarity(r[4]);
            if (SKIPPED.contains(r[2]) || chance <= 0) {
                continue;
            }
            int[] q = qty(r[3]);
            String salvage = itemId(r[0]);
            m.computeIfAbsent(salvage, k -> new ArrayList<>()).add(new SalvageLine(salvage, r[1], r[2], itemId(r[2]), q[0], q[1], chance));
        }
        m.replaceAll((k, v) -> List.copyOf(v));
        SALVAGE = Collections.unmodifiableMap(m);
    }

    /** An item and amount handed out. */
    public record Got(String item, int count) {
    }

    /**
     * Sorts one salvage: the pre-roll lines are rolled first (a hit replaces the main roll), then one main-table line
     * (the main sections' rarities add up to 1), then every tertiary line independently.
     */
    public static List<Got> sort(String salvage, Random random) {
        List<SalvageLine> lines = SALVAGE.get(salvage);
        List<Got> out = new ArrayList<>();
        if (lines == null) {
            return out;
        }
        boolean pre = false;
        for (SalvageLine l : lines) {
            if (l.preRoll() && random.nextDouble() < l.chance()) {
                out.add(got(l, random));
                pre = true;
                break;
            }
        }
        if (!pre) {
            double total = 0;
            for (SalvageLine l : lines) {
                if (!l.preRoll() && !l.tertiary()) {
                    total += l.chance();
                }
            }
            double roll = random.nextDouble() * total;
            for (SalvageLine l : lines) {
                if (!l.preRoll() && !l.tertiary()) {
                    roll -= l.chance();
                    if (roll < 0) {
                        out.add(got(l, random));
                        break;
                    }
                }
            }
        }
        for (SalvageLine l : lines) {
            if (l.tertiary() && random.nextDouble() < l.chance()) {
                out.add(got(l, random));
            }
        }
        return out;
    }

    private static Got got(SalvageLine l, Random random) {
        return new Got(l.item(), l.min() + (l.max() > l.min() ? random.nextInt(l.max() - l.min() + 1) : 0));
    }

    // ------------------------------------------------------------------ salvaging hooks

    /** A salvaging hook facility (wiki Salvaging hook table); tier 0 is bronze. */
    public record Hook(int tier, String metal) {
        public String id() {
            return metal + "_salvaging_hook";
        }
    }

    public static final List<String> METALS = List.of("bronze", "iron", "steel", "mithril", "adamant", "rune", "dragon");

    private static List<SailingTraining.Facility> buildFacilities() {
        List<SailingTraining.Facility> f = new ArrayList<>();
        String[] planks = {"plank", "oak_plank", "teak_plank", "mahogany_plank", "camphor_plank", "ironwood_plank", "rosewood_plank"};
        String[] nails = {"bronze_nails", "iron_nails", "steel_nails", "mithril_nails", "adamant_nails", "rune_nails", "dragon_nails"};
        String[] bars = {"bronze_bar", "iron_bar", "steel_bar", "mithril_bar", "adamantite_bar", "runite_bar", "dragon_metal_sheet"};
        int[][] hook = {{15, 1, 151}, {21, 9, 312}, {27, 18, 499}, {44, 30, 769}, {59, 52, 919}, {74, 66, 1233}, {86, 78, 1537}};
        for (int i = 0; i < 7; i++) {
            Map<String, Integer> m = new LinkedHashMap<>();
            m.put(planks[i], 4);
            m.put(nails[i], 16);
            m.put(bars[i], 6);
            if (i >= 2 && i <= 4) {
                m.put("lead_bar", 3);
            } else if (i == 5) {
                m.put("lead_bar", 4);
            }
            if (i == 6) {
                m.put("broken_dragon_hook", 1);
            }
            if (i >= 5) {
                m.put("cupronickel_bar", 4);
            }
            m.put("rope", 1);
            String metal = METALS.get(i);
            f.add(new SailingTraining.Facility(metal + "_salvaging_hook", title(metal) + " salvaging hook", hook[i][0], hook[i][1], hook[i][2],
                    Collections.unmodifiableMap(m), "Salvage shipwrecks from your boat."));
        }
        for (Cannon c : CANNONS) {
            int i = c.tier();
            Map<String, Integer> m = new LinkedHashMap<>();
            m.put(planks[i], 4);
            m.put(nails[i], 16);
            m.put(bars[i], 8);
            if (i == 6) {
                m.put("dragon_cannon_barrel", 1);
            }
            f.add(new SailingTraining.Facility(c.id(), title(c.metal()) + " cannon", c.sailing(), c.construction(), c.conXp(),
                    Collections.unmodifiableMap(m), "Fire cannonballs at sea creatures (Ranged " + c.ranged() + ")."));
        }
        // Eternal brazier (wiki: 78 Sailing, 72 Construction, 1,373 XP, with 250 each of te, efh and urt salt).
        f.add(new SailingTraining.Facility("eternal_brazier", "Eternal brazier", 78, 72, 1373, mapOf("ironwood_plank", 4, "rune_nails", 16,
                "runite_bar", 6, "cupronickel_bar", 6, "te_salt", BRAZIER_SALTS, "efh_salt", BRAZIER_SALTS, "urt_salt", BRAZIER_SALTS),
                "Keeps the crew warm in the icy seas of the Northern Ocean."));
        return List.copyOf(f);
    }

    /** Salts of each kind the eternal brazier takes (wiki). */
    public static final int BRAZIER_SALTS = 250;

    private static Map<String, Integer> mapOf(Object... kv) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], (Integer) kv[i + 1]);
        }
        return Collections.unmodifiableMap(m);
    }

    static String title(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** The best salvaging hook tier among a boat's facilities, or -1. */
    public static int hookTier(Set<String> facilities) {
        int best = -1;
        for (int i = 0; i < METALS.size(); i++) {
            if (facilities.contains(METALS.get(i) + "_salvaging_hook")) {
                best = i;
            }
        }
        return best;
    }

    /** Each hook's bonus to the wreck's low/high success values, in percent (bronze .. dragon; wiki salvaging calculator). */
    public static final int[] HOOK_BONUS_PERCENT = {0, 5, 10, 15, 20, 25, 35};

    /** The wreck's {low, high} out of 256 with a hook of this tier: each rounded down after the hook's bonus. */
    public static int[] hookLowHigh(SailingData.Wreck w, int hookTier) {
        int bonus = HOOK_BONUS_PERCENT[Math.max(0, Math.min(HOOK_BONUS_PERCENT.length - 1, hookTier))];
        return new int[]{w.low() * (100 + bonus) / 100, w.high() * (100 + bonus) / 100};
    }

    /**
     * Chance to haul up salvage per 4-tick roll (OSRS Wiki shipwreck salvaging calculator, Module:Sandbox/User:Coopermor/
     * ShipwreckSalvaging/Calc): the wreck's low/high raised by the hook's bonus, then the standard skilling curve
     * {@code (1 + floor(low (99 - L) / 98 + high (L - 1) / 98 + 0.5)) / 256} at the player's Sailing level.
     */
    public static double salvageChance(SailingData.Wreck w, int sailingLevel, int hookTier) {
        int[] lh = hookLowHigh(w, hookTier);
        return com.runecraft.osrs.Chance.lerp(sailingLevel, lh[0], lh[1]);
    }

    /**
     * A crewmate's chance per 5-tick roll (same calculator): the hook's low/high times Deckhandiness x 10%, each rounded
     * down, on the player's Sailing level (wiki example: about 30% of the player's chance for Deckhandiness 3).
     */
    public static double crewSalvageChance(SailingData.Wreck w, int sailingLevel, int hookTier, int deckhandiness) {
        int[] lh = hookLowHigh(w, hookTier);
        int d = Math.max(1, Math.min(4, deckhandiness));
        return com.runecraft.osrs.Chance.lerp(sailingLevel, lh[0] * d / 10, lh[1] * d / 10);
    }

    /** OSRS salvages on a 4-tick cycle. */
    public static final int SALVAGE_TICKS = 4;

    // ------------------------------------------------------------------ cannons

    /** A boat cannon (wiki Cannon (facility) table). */
    public record Cannon(int tier, String metal, int privateering, int sailing, int ranged, int construction, double conXp,
                         int strength, int accuracy, int playerAccuracy) {
        public String id() {
            return metal + "_cannon";
        }
    }

    public static final List<Cannon> CANNONS = List.of(
            new Cannon(0, "bronze", 1, 28, 1, 21, 157, 8, 30, 60),
            new Cannon(1, "iron", 2, 35, 1, 28, 328, 10, 42, 82),
            new Cannon(2, "steel", 2, 47, 5, 39, 526, 12, 60, 120),
            new Cannon(3, "mithril", 3, 57, 20, 50, 816, 15, 78, 148),
            new Cannon(4, "adamant", 3, 69, 30, 61, 986, 23, 98, 194),
            new Cannon(5, "rune", 4, 80, 40, 76, 1359, 26, 163, 273),
            new Cannon(6, "dragon", 4, 92, 60, 84, 1857, 32, 194, 344));

    /** Cannons fire every 7 ticks and reach 15 tiles. */
    public static final int CANNON_TICKS = 7;
    public static final int CANNON_RANGE = 15;

    /** A cannonball usable by boat cannons (wiki Cannonball table); a cannon fires balls up to its own tier. */
    public record Ball(String item, int tier, int accuracy, int strength, int heavy) {
    }

    public static final List<Ball> BALLS = List.of(
            new Ball("bronze_cannonball", 0, 12, 43, 15), new Ball("iron_cannonball", 1, 16, 56, 30),
            new Ball("cannonball", 2, 20, 74, 40), new Ball("mithril_cannonball", 3, 30, 105, 50),
            new Ball("granite_cannonball", 3, 30, 125, 65), new Ball("adamant_cannonball", 4, 50, 157, 80),
            new Ball("rune_cannonball", 5, 64, 201, 90), new Ball("dragon_cannonball", 6, 80, 270, 100));

    /** Boat facilities added for salvaging and boat combat: hooks and cannons (wiki levels, materials and Construction XP). */
    public static final List<SailingTraining.Facility> FACILITIES = buildFacilities();

    public static Ball ball(String item) {
        for (Ball b : BALLS) {
            if (b.item().equals(item)) {
                return b;
            }
        }
        return null;
    }

    /** The best cannon among a boat's facilities, or null. */
    public static Cannon bestCannon(Set<String> facilities) {
        Cannon best = null;
        for (Cannon c : CANNONS) {
            if (facilities.contains(c.id())) {
                best = c;
            }
        }
        return best;
    }

    /**
     * Cannon max hit (OSRS Wiki, Boat combat and Calculator:Sailing/Cannon max hit). Fired by a crewmate ({@code player}
     * false): floor((Sailing level x (cannon str + ball str + 64) + 320) / 640), before the crewmate's Privateering
     * scaling. Fired by a player with no prayer, Void or worn ranged strength: see {@link #playerCannonMaxHit}.
     */
    public static int cannonMaxHit(int level, Cannon cannon, Ball ball, boolean player) {
        if (player) {
            return playerCannonMaxHit(effectiveRanged(level, 1.0, 1.0), cannon, ball, 0);
        }
        return (level * (cannon.strength() + ball.strength() + 64) + 320) / 640;
    }

    /**
     * A player's effective Ranged level on a cannon: floor((floor(visible Ranged x prayer) + 8) x Void) (the wiki
     * calculator; since 1 April 2026 Ranged prayers and the Void set apply to cannon fire).
     */
    public static int effectiveRanged(int rangedLevel, double prayer, double voidBonus) {
        return (int) Math.floor((Math.floor(rangedLevel * prayer + 1e-9) + 8) * voidBonus + 1e-9);
    }

    /**
     * A player's cannon max hit: floor(0.5 + effective Ranged x (cannon str + ball str + the ball's heavy ranged bonus +
     * worn ranged strength + 64) / 640) (wiki calculator; the heavy bonus applies only when a player mans the cannon).
     */
    public static int playerCannonMaxHit(int effectiveRanged, Cannon cannon, Ball ball, int wornRangedStrength) {
        int str = cannon.strength() + ball.strength() + ball.heavy() + wornRangedStrength;
        return (int) Math.floor(0.5 + effectiveRanged * (str + 64) / 640.0);
    }

    /**
     * Hit chance of a cannon shot: the OSRS accuracy roll against the creature's heavy ranged defence roll, (Defence + 9)
     * x (heavy bonus + 64). A player's attack roll is effective Ranged (Ranged prayer, +8) x (the cannon's manned accuracy
     * + the ball's accuracy + worn ranged attack + 64) (wiki: "this type of accuracy is considered Heavy Ranged"). A
     * crewmate fires with the cannon's base accuracy on the player's Sailing level + 8 (approx: the wiki gives no crew
     * accuracy formula).
     */
    public static double cannonAccuracy(int effectiveLevel, Cannon cannon, Ball ball, boolean player, int wornRangedAttack,
                                        SeaCreature target) {
        int bonus = (player ? cannon.playerAccuracy() + wornRangedAttack : cannon.accuracy()) + ball.accuracy();
        long attRoll = com.runecraft.osrs.CombatFormulas.attackRoll(effectiveLevel, bonus);
        long defRoll = (long) (target.def() + 9) * (target.dheavy() + 64);
        return com.runecraft.osrs.Chance.hitChance(attRoll, defRoll);
    }

    /** A crewmate's effective level on a cannon: the player's Sailing level + 8 (approx). */
    public static int crewEffectiveLevel(int sailingLevel) {
        return sailingLevel + 8;
    }

    /** Defence level of a boat's hull (OSRS Wiki, Hull: raft 1-30, skiff 10-70, sloop 15-75 from wooden to rosewood). */
    private static final int[][] HULL_DEFENCE = {{1, 5, 10, 15, 20, 25, 30}, {10, 20, 30, 40, 50, 60, 70}, {15, 25, 35, 45, 55, 65, 75}};

    public static int hullDefence(String boat, int hullTier) {
        return HULL_DEFENCE[Math.max(0, SailingTraining.BOATS.indexOf(boat))][clamp(hullTier)];
    }

    /**
     * A sea creature's chance to hit a boat: its attack roll, (Attack + 9) x (attack bonus + 64), or Magic for the
     * krakens that cast, against the hull's defence roll, (hull Defence + 9) x 64 (wiki Boat combat: the hull sets the
     * ship's defence level and bonuses; the bonuses' values are not in the July 2026 dump, so 0 here, approx).
     */
    public static double creatureHitChance(SeaCreature c, String boat, int hullTier) {
        long attRoll = (long) (c.attackLevel() + 9) * (c.attackBonus() + 64);
        long defRoll = (long) (hullDefence(boat, hullTier) + 9) * 64;
        return com.runecraft.osrs.Chance.hitChance(attRoll, defRoll);
    }

    // ------------------------------------------------------------------ sea creatures

    /** A sea creature: wiki stats, its max hit against no keel and each keel (bronze..dragon), and one wiki location. */
    public record SeaCreature(String id, String name, int combat, int hp, int att, int str, int def, int[] keelMaxHits, int speed,
                              AttackType style, boolean aggressive, int dlight, int dstandard, int dheavy, int x, int y, String location) {
        /** The max hit against a boat: keel index 0 is no keel (raft), 1..7 bronze..dragon. */
        public int maxHitAgainst(int keelIndex) {
            return keelMaxHits[Math.max(0, Math.min(keelMaxHits.length - 1, keelIndex))];
        }

        /** The level its attack roll uses: Magic for the krakens that cast, else Attack (wiki infobox). */
        public int attackLevel() {
            int[] a = ACCURACY.get(id);
            return style == AttackType.MAGIC ? (a == null ? att : a[0]) : att;
        }

        /** Its attack bonus for that style (wiki infobox: 0 for every sea creature but the Mogre's +22 crush). */
        public int attackBonus() {
            int[] a = ACCURACY.get(id);
            return a == null ? 0 : a[1];
        }
    }

    /** Magic level and attack bonus of each sea creature (wiki infoboxes, July 2026: mage, attbns/amagic). */
    private static final Map<String, int[]> ACCURACY = accuracy();

    private static Map<String, int[]> accuracy() {
        Map<String, int[]> m = new LinkedHashMap<>();
        for (String[] r : rows(SailingSeaTables.CREATURE_ACCURACY)) {
            m.put(creatureId(r[0]), new int[]{Integer.parseInt(r[1]), Integer.parseInt(r[2])});
        }
        return m;
    }

    public static final List<SeaCreature> CREATURES;

    static {
        List<SeaCreature> list = new ArrayList<>();
        for (String[] r : rows(SailingSeaTables.CREATURES)) {
            String[] h = r[6].split(",");
            int[] hits = new int[h.length];
            for (int i = 0; i < h.length; i++) {
                hits[i] = Integer.parseInt(h[i]);
            }
            list.add(new SeaCreature(creatureId(r[0]), r[0].replace(" (sea)", "").replace(" (monster)", ""), Integer.parseInt(r[1]),
                    Integer.parseInt(r[2]), Integer.parseInt(r[3]), Integer.parseInt(r[4]), Integer.parseInt(r[5]), hits,
                    Integer.parseInt(r[7]), AttackType.valueOf(r[8]), r[9].equals("Yes"), Integer.parseInt(r[10]), Integer.parseInt(r[11]),
                    Integer.parseInt(r[12]), Integer.parseInt(r[13]), Integer.parseInt(r[14]), r[15]));
        }
        CREATURES = List.copyOf(list);
    }

    /** RuneCraft id of a sea creature: the slug, except where it would clash (Mogre, the manta ray fish). */
    public static String creatureId(String wikiName) {
        return switch (wikiName) {
            case "Mogre (sea)" -> "sea_mogre";
            case "Manta ray (monster)" -> "sea_manta_ray";
            default -> itemId(wikiName);
        };
    }

    public static SeaCreature creature(String id) {
        for (SeaCreature c : CREATURES) {
            if (c.id().equals(id)) {
                return c;
            }
        }
        return null;
    }

    /** Wiki drops of a creature as RuneCraft drop lines (bounty-only parts, gem table and unknown rarities left out). */
    public static List<Drop> creatureDrops(String creatureId) {
        Set<String> parts = bountyParts();
        List<Drop> out = new ArrayList<>();
        for (String[] r : rows(SailingSeaTables.CREATURE_DROPS)) {
            if (!creatureId(r[0]).equals(creatureId) || SKIPPED.contains(r[2]) || parts.contains(itemId(r[2]))) {
                continue;
            }
            double chance = rarity(r[4]);
            if (chance <= 0) {
                continue;
            }
            int[] q = qty(r[3]);
            out.add(Drop.of(itemId(r[2]), q[0], q[1], (int) Math.max(1, Math.round(1 / chance))));
        }
        return out;
    }

    /** The sea creatures as monsters (spawned at sea by the Sailing hooks; wiki stats and drops). */
    public static List<MonsterDef> monsters() {
        List<MonsterDef> out = new ArrayList<>();
        for (SeaCreature c : CREATURES) {
            boolean magic = c.style() == AttackType.MAGIC;
            MonsterDef m = MonsterDef.of(c.id(), c.name()).level(c.combat())
                    .stats(c.hp(), c.att(), c.str(), c.def(), magic ? Math.max(c.att(), 50) : 1, 1)
                    .speed(c.speed()).defenceBonus(0, 0, 0, 0, c.dstandard())
                    .attack(AttackDef.of(magic ? "ink" : "bite", c.style(), c.maxHitAgainst(0), magic ? 8 : 2.5, 10))
                    .rangedDefence(c.dlight(), c.dheavy())
                    .skin(c.id().contains("kraken") ? 0x7A3A5A : c.id().contains("shark") ? 0x6A7A8A : 0x5A6A7A, 0x3A4A5A, 0x2A3A4A,
                            0xE0E0E0)
                    .size(1.6f, 1.0f, 1.2f);
            for (Drop d : creatureDrops(c.id())) {
                m.drop(d);
            }
            out.add(m);
        }
        return out;
    }

    // ------------------------------------------------------------------ bounty tasks

    /** A bounty task: board port id, creature id, part item, quantity, part drop chance while on task, XP (and coins). */
    public record Bounty(int id, int level, double xp, String board, String creature, String item, String itemName, int qty, double chance,
                         boolean guaranteed) {
    }

    public static final List<Bounty> BOUNTIES;

    static {
        List<Bounty> list = new ArrayList<>();
        for (String[] r : rows(SailingSeaTables.BOUNTIES)) {
            list.add(new Bounty(Integer.parseInt(r[0]), Integer.parseInt(r[1]), Double.parseDouble(r[2]), portId(r[3]), creatureId(r[4]),
                    itemId(r[5]), r[5], Integer.parseInt(r[6]), rarity(r[7]), r.length > 8 && r[8].equals("yes")));
        }
        BOUNTIES = List.copyOf(list);
    }

    static String portId(String board) {
        for (SailingData.Port p : SailingData.PORTS) {
            if (p.name().equalsIgnoreCase(board)) {
                return p.id();
            }
        }
        return itemId(board);
    }

    /** Bounty tasks open at level 30 Sailing. */
    public static final int BOUNTY_LEVEL = 30;

    public static Bounty bounty(int id) {
        for (Bounty b : BOUNTIES) {
            if (b.id() == id) {
                return b;
            }
        }
        return null;
    }

    public static List<Bounty> boardBounties(String portId, int level) {
        List<Bounty> out = new ArrayList<>();
        for (Bounty b : BOUNTIES) {
            if (b.board().equals(portId) && b.level() <= level) {
                out.add(b);
            }
        }
        return out;
    }

    /** Every bounty part item id. */
    public static Set<String> bountyParts() {
        Set<String> s = new java.util.LinkedHashSet<>();
        for (String[] r : rows(SailingSeaTables.BOUNTIES)) {
            s.add(itemId(r[5]));
        }
        return s;
    }

    /** Bounty parts with their creature (one entry per part). */
    public static Map<String, String> partCreatures() {
        Map<String, String> m = new LinkedHashMap<>();
        for (Bounty b : BOUNTIES) {
            m.putIfAbsent(b.item(), b.creature());
        }
        return m;
    }

    // ------------------------------------------------------------------ boat hitpoints

    private static final int[][] HULL_HP = {{20, 30, 40, 50, 80, 100, 120}, {30, 45, 60, 75, 120, 150, 180}, {40, 60, 80, 100, 160, 200, 240}};
    private static final int[][] KEEL_HP = {{0, 0, 0, 0, 0, 0, 0}, {50, 50, 60, 60, 70, 80, 80}, {70, 70, 90, 90, 100, 120, 120}};
    private static final int[] KEEL_ARMOUR = {100, 200, 300, 400, 600, 800, 1000};

    /** Boat hitpoints: the hull's plus the keel's (wiki Hull and Keel tables); rafts have no keel. */
    public static int boatHp(String boat, int hullTier, int keelTier) {
        int b = Math.max(0, SailingTraining.BOATS.indexOf(boat));
        return HULL_HP[b][clamp(hullTier)] + KEEL_HP[b][clamp(keelTier)];
    }

    /** Index into a creature's keel max hits: 0 for a raft (no keel), else 1 + keel tier. */
    public static int keelIndex(String boat, int keelTier) {
        return boat.equals("raft") ? 0 : 1 + clamp(keelTier);
    }

    /** Flat damage reduction of a keel: 1 per 100 armour. */
    public static int keelReduction(String boat, int keelTier) {
        return boat.equals("raft") ? 0 : KEEL_ARMOUR[clamp(keelTier)] / 100;
    }

    private static int clamp(int t) {
        return Math.max(0, Math.min(6, t));
    }

    // ------------------------------------------------------------------ ocean encounters

    /** Wiki ocean encounters with their share of spawns (wiki Ocean encounters: ~60%, 12%, 12%, 6%, 6%, 2.5%, 1.2%, 0.1%). */
    public enum Encounter {
        STRONG_WINDS(600), MYSTERIOUS_GLOW(120), LOST_CRATE(120), CASTAWAY(60), GIANT_CLAM(60), CLUE_TURTLE(25), OCEAN_MAN(12),
        LOST_CASKET(1);

        public final int weight;

        Encounter(int weight) {
            this.weight = weight;
        }
    }

    /** Ticks under way between encounter checks (115 with a keg of kraken ink stout). */
    public static final int ENCOUNTER_TICKS = 120;

    /** The chance at the n-th check (1-based): n/6, so an encounter is certain after six intervals. */
    public static boolean encounterSpawns(int interval, Random random) {
        return random.nextInt(6) < Math.min(6, interval);
    }

    public static Encounter rollEncounter(Random random) {
        int total = 0;
        for (Encounter e : Encounter.values()) {
            total += e.weight;
        }
        int r = random.nextInt(total);
        for (Encounter e : Encounter.values()) {
            r -= e.weight;
            if (r < 0) {
                return e;
            }
        }
        return Encounter.STRONG_WINDS;
    }

    /** Clue turtle: 50 XP and a clue whose tier depends on the Sailing level (wiki rates). */
    public static final double CLUE_TURTLE_XP = 50;

    public static String clueTurtle(int level, Random random) {
        int r = random.nextInt(20);
        if (level >= 70) {
            return r < 9 ? "clue_scroll_easy" : r < 15 ? "clue_scroll_medium" : r < 18 ? "clue_scroll_hard" : "clue_scroll_elite";
        }
        if (level >= 50) {
            return r < 9 ? "clue_scroll_easy" : r < 15 ? "clue_scroll_medium" : "clue_scroll_hard";
        }
        if (level >= 30) {
            return r < 8 ? "clue_scroll_easy" : "clue_scroll_medium";
        }
        return "clue_scroll_easy";
    }

    /** Mysterious glow: total XP of the whole chain of glows (8 XP doubling each glow) at a Sailing level (wiki table). */
    public static int glowXp(int level) {
        return level >= 91 ? 4088 : level >= 81 ? 2040 : level >= 71 ? 1016 : level >= 61 ? 504 : level >= 41 ? 248 : 120;
    }

    /** Uncut gem from the last glow: better gems at higher levels (approx; the wiki rates are unknown). */
    public static String glowGem(int level, Random random) {
        String[] gems = {"uncut_opal", "uncut_red_topaz", "uncut_sapphire", "uncut_emerald", "uncut_ruby", "uncut_diamond",
                "uncut_dragonstone"};
        int top = Math.min(gems.length - 1, 1 + level / 15);
        return gems[random.nextInt(top + 1)];
    }

    /** Castaway: 5x Sailing level XP on pick-up and 10x more when dropped off at a port. */
    public static double castawayPickupXp(int level) {
        return 5.0 * level;
    }

    public static double castawayDropXp(int level) {
        return 10.0 * level;
    }

    /**
     * Lost crates (wiki Lost crate): wooden, oak, teak, mahogany, camphor and ironwood crates open at Sailing 5, 26, 51,
     * 71, 86 and 95 and give 25, 50, 150, 235, 335 and 450 Sailing XP. Each crate gives one of eight kinds of loot (1/8
     * each) at the crate's tier, in a fixed amount. The wiki (July 2026) gives no odds for which unlocked tier spawns
     * ("each crate has a chance to spawn after reaching a minimum Sailing level"): equal here (approx).
     */
    public static final int[] CRATE_LEVELS = {5, 26, 51, 71, 86, 95};
    public static final double[] CRATE_XP = {25, 50, 150, 235, 335, 450};
    public static final String[] CRATE_NAMES = {"wooden", "oak", "teak", "mahogany", "camphor", "ironwood"};
    /**
     * Loot per kind (ammunition, construction supplies, farming goods, potions, hides, weapons, metals, runes) and tier,
     * as "item:amount"; a potion with fewer than four doses is written {@code id(n)} (wiki: superantipoison(2), stamina
     * potion(2), super combat potion(2) and prayer regeneration potion(1)). The wooden and oak crates give the wiki's
     * sawmill coupons; the ironwood hides line (mixed hide base) is a black dragon leather here (approx).
     */
    private static final String[][] CRATE = {
            {"steel_arrow:40", "mithril_arrow:40", "adamant_arrow:40", "rune_arrow:30", "amethyst_arrow:25", "dragon_arrow:20"},
            {"sawmill_coupon_wood_plank:20", "sawmill_coupon_oak_plank:12", "teak_plank:1", "mahogany_plank:1", "camphor_plank:1",
                    "ironwood_plank:1"},
            {"sweetcorn_seed:6", "willow_seed:2", "ranarr_seed:2", "watermelon_seed:6", "yew_seed:1", "spirit_seed:1"},
            {"strength_potion:1", "superantipoison(2):1", "prayer_potion:1", "stamina_potion(2):1", "super_combat_potion(2):1",
                    "prayer_regeneration_potion(1):1"},
            {"leather:1", "snakeskin:1", "green_dragon_leather:1", "red_dragon_leather:1", "black_dragon_leather:1", "black_dragon_leather:1"},
            {"steel_scimitar:1", "black_warhammer:1", "adamant_scimitar:1", "rune_dagger:1", "rune_battleaxe:1", "dragon_sword:1"},
            {"silver_bar:1", "gold_bar:1", "mithril_bar:1", "adamantite_bar:1", "runite_bar:1", "dragon_metal_sheet:1"},
            {"mind_rune:80", "law_rune:10", "chaos_rune:100", "nature_rune:100", "blood_rune:85", "wrath_rune:20"}};

    /** The best crate tier a Sailing level opens (0 = wooden), or -1 below level 5. */
    public static int maxCrateTier(int level) {
        int t = -1;
        for (int i = 0; i < CRATE_LEVELS.length; i++) {
            if (level >= CRATE_LEVELS[i]) {
                t = i;
            }
        }
        return t;
    }

    /** The tier of a lost crate found at a Sailing level: any unlocked tier, equally likely (approx). */
    public static int lostCrateTier(int level, Random random) {
        return random.nextInt(Math.max(0, maxCrateTier(level)) + 1);
    }

    /** The loot of a crate of a tier: one of the eight kinds, 1/8 each, in the wiki amount. */
    public static Got lostCrateLoot(int tier, Random random) {
        String[] it = CRATE[random.nextInt(CRATE.length)][Math.max(0, Math.min(5, tier))].split(":");
        return new Got(it[0], Integer.parseInt(it[1]));
    }

    public static Got lostCrate(int level, Random random) {
        return lostCrateLoot(lostCrateTier(level, random), random);
    }

    /** Every item a lost crate can give (for tests). */
    public static List<String> lostCrateItems() {
        List<String> out = new ArrayList<>();
        for (String[] row : CRATE) {
            for (String it : row) {
                out.add(it.split(":")[0]);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ hazards

    /** A sailing hazard: what protects against it and its damage to the boat (or the player) every few seconds. */
    public enum Hazard {
        STORMY_SEAS("Stormy seas", 10, false, "The sails are not strong enough to withstand the lightning in these stormy seas!"),
        CRYSTAL_FLECKED("Crystal-flecked waters", 20, false,
                "The keel is not strong enough to withstand the sharp crystals floating in the water!"),
        FETID_WATERS("Fetid waters", 5, true, "Without an inoculation station, the acrid environment infects you with disease!"),
        TANGLED_KELP("Tangled kelp", 4, false, "The kelp tangles around your helm!"),
        ICY_SEAS("Icy seas", 5, true, "The bitter cold freezes you to the bone!");

        public final String wiki;
        public final int damage;
        public final boolean hurtsPlayer;
        public final String message;

        Hazard(String wiki, int damage, boolean hurtsPlayer, String message) {
            this.wiki = wiki;
            this.damage = damage;
            this.hurtsPlayer = hurtsPlayer;
            this.message = message;
        }

        public static Hazard byWiki(String name) {
            for (Hazard h : values()) {
                if (h.wiki.equalsIgnoreCase(name)) {
                    return h;
                }
            }
            return null;
        }
    }

    /**
     * Whether a boat is protected: stormy seas need oak sails (tier 1), crystal-flecked waters an adamant keel (tier 4),
     * tangled kelp an adamant helm (tier 4), fetid waters an inoculation station and icy seas an eternal brazier.
     */
    public static boolean protectedFrom(Hazard h, Map<SailingTraining.PartType, Integer> tiers, Set<String> facilities) {
        return switch (h) {
            case STORMY_SEAS -> tiers.getOrDefault(SailingTraining.PartType.SAILS, 0) >= 1;
            case CRYSTAL_FLECKED -> tiers.getOrDefault(SailingTraining.PartType.KEEL, 0) >= 4;
            case TANGLED_KELP -> tiers.getOrDefault(SailingTraining.PartType.HELM, 0) >= 4;
            case FETID_WATERS -> facilities.contains("inoculation_station");
            case ICY_SEAS -> facilities.contains("eternal_brazier");
        };
    }

    /** The hazard of the sea around a tile: that of the nearest sea charting task within 40 tiles, or null. */
    public static Hazard hazardAt(int x, int y) {
        SailingTraining.ChartTask t = SailingTraining.nearestChart(x, y, 40);
        return t == null ? null : Hazard.byWiki(t.hazard());
    }

    /** Ticks between hazard hits ("every few seconds"). */
    public static final int HAZARD_TICKS = 5;
}
