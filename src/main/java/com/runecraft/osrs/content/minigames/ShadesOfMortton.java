package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentDef.Slot;
import com.runecraft.osrs.EquipmentDef.WeaponClass;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ItemSink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleSupplier;
import java.util.function.ToIntFunction;

/**
 * Shades of Mort'ton (OSRS Wiki "Shades of Mort'ton (minigame)", the shade, remains, pyre log and
 * chest pages): shades drop only their remains; remains are cremated on a funeral pyre with pyre logs
 * (logs + sacred oil) for Firemaking and Prayer XP and a coloured shade key (79%) or coins (21%);
 * keys open the matching chests in the Shade Catacombs. Sacred oil is olive oil sanctified at the
 * Flamtaer temple's fire altar, which costs sanctity earned by repairing the temple or killing shades.
 */
public final class ShadesOfMortton {
    private ShadesOfMortton() {
    }

    public static final String QUEST = "shades_of_mortton";
    public static final String[] METALS = {"bronze", "steel", "black", "silver", "gold"};
    /** Key and lock colours, worst to best. */
    public static final String[] COLOURS = {"red", "brown", "crimson", "black", "purple"};

    /** A shade key: the red key keeps the old id ({@code bronze_shade_key}), the others add their colour. */
    public static String key(String metal, String colour) {
        return metal + "_shade_key" + (colour.equals("red") ? "" : "_" + colour);
    }

    /** {metal, colour} of a shade key id, or null. */
    public static String[] keyParts(String id) {
        for (String m : METALS) {
            for (String c : COLOURS) {
                if (key(m, c).equals(id)) {
                    return new String[]{m, c};
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ shades and remains

    /** A key reward from cremating remains: key id and weight (out of the remains' total; coins are separate). */
    public record KeyWeight(String key, int weight) {
    }

    /**
     * The six shades: combat level, hitpoints, attack, strength, defence, max hit (crush, 4-tick), the
     * key metal needed to reach them in the catacombs (null: Loar shades roam Mort'ton), the keys their
     * remains give, and the coins (21%) otherwise.
     */
    public enum Shade {
        LOAR("loar", "Loar", 40, 38, 45, 30, 26, 4, null, 200, 300,
                List.of(new KeyWeight(key("bronze", "red"), 35), new KeyWeight(key("bronze", "brown"), 30),
                        new KeyWeight(key("bronze", "crimson"), 21))),
        PHRIN("phrin", "Phrin", 60, 56, 64, 47, 42, 6, "bronze", 400, 500,
                List.of(new KeyWeight(key("steel", "red"), 35), new KeyWeight(key("steel", "brown"), 30),
                        new KeyWeight(key("steel", "crimson"), 20), new KeyWeight(key("bronze", "black"), 10),
                        new KeyWeight(key("bronze", "purple"), 6))),
        RIYL("riyl", "Riyl", 80, 76, 88, 55, 60, 6, "steel", 600, 700,
                List.of(new KeyWeight(key("black", "red"), 35), new KeyWeight(key("black", "brown"), 30),
                        new KeyWeight(key("black", "crimson"), 20), new KeyWeight(key("steel", "black"), 10),
                        new KeyWeight(key("steel", "purple"), 6))),
        ASYN("asyn", "Asyn", 100, 90, 102, 84, 70, 9, "black", 800, 900,
                List.of(new KeyWeight(key("silver", "red"), 35), new KeyWeight(key("silver", "brown"), 30),
                        new KeyWeight(key("black", "crimson"), 20), new KeyWeight(key("black", "black"), 10),
                        new KeyWeight(key("black", "purple"), 6))),
        FIYR("fiyr", "Fiyr", 120, 110, 120, 100, 85, 11, "silver", 2000, 4000,
                List.of(new KeyWeight(key("gold", "red"), 20), new KeyWeight(key("silver", "brown"), 20),
                        new KeyWeight(key("silver", "crimson"), 45), new KeyWeight(key("silver", "black"), 10),
                        new KeyWeight(key("silver", "purple"), 6))),
        /** Added with the 2021 rework; found behind the gold-key doors. */
        URIUM("urium", "Urium", 140, 130, 138, 116, 100, 13, "gold", 2000, 7000,
                List.of(new KeyWeight(key("gold", "crimson"), 45), new KeyWeight(key("gold", "red"), 20),
                        new KeyWeight(key("gold", "brown"), 20), new KeyWeight(key("gold", "black"), 10),
                        new KeyWeight(key("gold", "purple"), 6)));

        private final String id;
        private final String displayName;
        private final int level;
        private final int hitpoints;
        private final int attack;
        private final int strength;
        private final int defence;
        private final int maxHit;
        private final String area;
        private final int coinsMin;
        private final int coinsMax;
        private final List<KeyWeight> keys;

        Shade(String id, String displayName, int level, int hitpoints, int attack, int strength, int defence, int maxHit,
              String area, int coinsMin, int coinsMax, List<KeyWeight> keys) {
            this.id = id;
            this.displayName = displayName;
            this.level = level;
            this.hitpoints = hitpoints;
            this.attack = attack;
            this.strength = strength;
            this.defence = defence;
            this.maxHit = maxHit;
            this.area = area;
            this.coinsMin = coinsMin;
            this.coinsMax = coinsMax;
            this.keys = keys;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        public int level() {
            return level;
        }

        public int hitpoints() {
            return hitpoints;
        }

        public int attack() {
            return attack;
        }

        public int strength() {
            return strength;
        }

        public int defence() {
            return defence;
        }

        public int maxHit() {
            return maxHit;
        }

        /** Key metal of the catacomb doors in front of this shade (null for the Loar shades in town). */
        public String area() {
            return area;
        }

        public int coinsMin() {
            return coinsMin;
        }

        public int coinsMax() {
            return coinsMax;
        }

        public List<KeyWeight> keys() {
            return keys;
        }

        public String creature() {
            return id + "_shade";
        }

        public String remains() {
            return id + "_remains";
        }

        public static Shade byRemains(String item) {
            for (Shade s : values()) {
                if (s.remains().equals(item)) {
                    return s;
                }
            }
            return null;
        }
    }

    /** Chance that cremated remains leave coins instead of a key. */
    public static final double COINS_CHANCE = 0.21;

    /** The reward from cremating remains: a key, or coins (amount > 0). */
    public record Cremation(String key, int coins) {
    }

    /** Rolls the key or coins from cremating one set of remains. */
    public static Cremation cremate(Shade s, DoubleSupplier random) {
        if (random.getAsDouble() < COINS_CHANCE) {
            int coins = s.coinsMin() + (int) Math.floor(random.getAsDouble() * (s.coinsMax() - s.coinsMin() + 1));
            return new Cremation(null, coins);
        }
        int total = s.keys().stream().mapToInt(KeyWeight::weight).sum();
        int r = (int) Math.floor(random.getAsDouble() * total);
        for (KeyWeight k : s.keys()) {
            r -= k.weight();
            if (r < 0) {
                return new Cremation(k.key(), 0);
            }
        }
        return new Cremation(s.keys().get(0).key(), 0);
    }

    // ------------------------------------------------------------------ pyre logs

    /**
     * Pyre logs: the log they're made from, sacred oil doses (5 Firemaking XP per dose when made), the
     * Firemaking level and XP for cremating with them, and the Prayer XP per shade (Loar..Urium; 0 =
     * too weak to burn those remains). Arctic pine, camphor, ironwood and rosewood pyre logs are left
     * out until RuneCraft has their logs.
     */
    public enum PyreLog {
        NORMAL("pyre_logs", "Pyre logs", "logs", 2, 5, 50.0, 8, 25, 37.5, 0, 0, 0, 0),
        OAK("oak_pyre_logs", "Oak pyre logs", "oak_logs", 2, 20, 70.0, 40, 33, 45.5, 0, 0, 0, 0),
        WILLOW("willow_pyre_logs", "Willow pyre logs", "willow_logs", 3, 35, 100.0, 80, 33.5, 46, 61, 0, 0, 0),
        TEAK("teak_pyre_logs", "Teak pyre logs", "teak_logs", 3, 40, 120.0, 100, 33.7, 46.2, 61.2, 0, 0, 0),
        MAPLE("maple_pyre_logs", "Maple pyre logs", "maple_logs", 3, 50, 175.0, 160, 34, 46.5, 61.5, 0, 0, 0),
        MAHOGANY("mahogany_pyre_logs", "Mahogany pyre logs", "mahogany_logs", 3, 55, 210.0, 200, 34.3, 46.8, 61.8, 0, 0, 0),
        YEW("yew_pyre_logs", "Yew pyre logs", "yew_logs", 4, 65, 255.0, 320, 34.5, 47, 62, 79.5, 0, 0),
        MAGIC("magic_pyre_logs", "Magic pyre logs", "magic_logs", 4, 80, 404.5, 640, 35, 47.5, 62.5, 80, 100, 0),
        REDWOOD("redwood_pyre_logs", "Redwood pyre logs", "redwood_logs", 4, 95, 500.0, 895, 35.5, 48, 63, 80.5, 100.5, 120.5);

        private final String id;
        private final String displayName;
        private final String log;
        private final int doses;
        private final int level;
        private final double firemakingXp;
        private final int value;
        private final double[] prayerXp;

        PyreLog(String id, String displayName, String log, int doses, int level, double firemakingXp, int value, double... prayerXp) {
            this.id = id;
            this.displayName = displayName;
            this.log = log;
            this.doses = doses;
            this.level = level;
            this.firemakingXp = firemakingXp;
            this.value = value;
            this.prayerXp = prayerXp;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        public String log() {
            return log;
        }

        /** Sacred oil doses used to make one. */
        public int doses() {
            return doses;
        }

        /** Firemaking XP for making one: a flat 5 per dose of oil (since July 2024). */
        public double makeXp() {
            return 5.0 * doses;
        }

        /** Firemaking level to light a pyre built with these logs. */
        public int level() {
            return level;
        }

        /** Firemaking XP for cremating with these logs. */
        public double firemakingXp() {
            return firemakingXp;
        }

        public int value() {
            return value;
        }

        public boolean burns(Shade s) {
            return prayerXp[s.ordinal()] > 0;
        }

        /** Prayer XP for cremating a shade's remains on these logs (base, without the Morytania hard diary). */
        public double prayerXp(Shade s) {
            return prayerXp[s.ordinal()];
        }

        public static PyreLog byId(String id) {
            for (PyreLog p : values()) {
                if (p.id.equals(id)) {
                    return p;
                }
            }
            return null;
        }
    }

    // ------------------------------------------------------------------ sanctity and the temple

    /** Sanctity (percent) needed to use the fire altar, and the cost per dose of olive oil sanctified. */
    public static final double ALTAR_MIN_SANCTITY = 10.0;
    public static final double SANCTITY_PER_DOSE = 0.9;
    /** Sanctity per shade killed around the temple, and lost per minute. */
    public static final double SANCTITY_PER_KILL = 2.0;
    public static final double SANCTITY_DRAIN_PER_MINUTE = 1.0;
    /** One set (a timber beam or plank, a limestone brick and five swamp paste) adds 5% resources. */
    public static final double RESOURCES_PER_SET = 5.0;
    public static final int PASTE_PER_SET = 5;
    /** Each repair uses 1% resources and earns about 5% sanctity (wiki: "approximately 5% Sanctity for each 1%"). */
    public static final double RESOURCES_PER_REPAIR = 1.0;
    public static final double SANCTITY_PER_REPAIR = 5.0;
    /** Crafting XP per repair: approx, from the wiki's ~12,000 XP an hour with a regular hammer (3-tick repairs). */
    public static final double REPAIR_CRAFTING_XP = 6.0;
    /** Ticks per repair: a hammer, and a flamtaer hammer (about 75% faster). Approx. */
    public static final int REPAIR_TICKS = 3;
    public static final int REPAIR_TICKS_FLAMTAER = 2;
    /** Razmire's general store sells olive oil(4); the builders' merchant sells repair supplies (approx prices). */
    public static final int OLIVE_OIL_PRICE = 28;
    public static final int REPAIR_SET_PRICE = 60; // approx: one timber beam, one limestone brick and five swamp paste

    // ------------------------------------------------------------------ chests

    /** One main-table line of a chest: item, quantity range and weight (out of the table total). */
    public record Line(String item, int min, int max, double weight) {
    }

    /**
     * A chest (metal, lock colour): the zealot's robes (1/128, gold chests), locks (1/63 red .. 1/59
     * purple) and splitbark scroll (steel and up) pre-rolls, the main table, the coin roll bounds and
     * the swamp paste always given (or a flamtaer bag, 1/2, to players without one, steel and up).
     */
    public record Chest(String metal, String colour, int zealot, int locks, int splitbark, int pasteMin, int pasteMax,
                        List<Line> main) {
        public double total() {
            return main.stream().mapToDouble(Line::weight).sum();
        }

        public int coinRollMin() {
            return coinRolls(metal)[0];
        }

        public int coinRollMax() {
            return coinRolls(metal)[1];
        }
    }

    /** RandomRoll bounds of a chest's coins (wiki "Coin amounts"). */
    public static int[] coinRolls(String metal) {
        return switch (metal) {
            case "bronze" -> new int[]{400, 600};
            case "steel" -> new int[]{900, 1100};
            case "black" -> new int[]{1500, 2000};
            case "silver" -> new int[]{3600, 7000};
            default -> new int[]{4500, 9500};
        };
    }

    /** Coins from a chest: floor((roll - value of the main loot) / 3), none when the loot is worth more. */
    public static int coins(int roll, int mainLootValue) {
        return roll > mainLootValue ? (roll - mainLootValue) / 3 : 0;
    }

    public static final List<String> ZEALOT_ORDER = List.of("zealots_boots", "zealots_helm", "zealots_robe_bottom",
            "zealots_robe_top");

    /** A chest opening's loot. */
    public record Opening(List<MinigameReward> items) {
    }

    /**
     * Opens a chest: the pre-rolls in order (zealot's robes - the next piece not yet owned -, locks,
     * splitbark scrolls), otherwise the main table; then coins and swamp paste or a flamtaer bag.
     */
    public static Opening open(Chest c, Set<String> owned, ToIntFunction<String> value, DoubleSupplier random) {
        List<MinigameReward> out = new ArrayList<>();
        MinigameReward main = null;
        if (c.zealot() > 0 && random.getAsDouble() < 1.0 / c.zealot()) {
            for (String piece : ZEALOT_ORDER) {
                if (!owned.contains(piece)) {
                    main = new MinigameReward(piece, 1);
                    break;
                }
            }
        }
        if (main == null && random.getAsDouble() < 1.0 / c.locks()) {
            main = new MinigameReward(c.metal() + "_locks", 1);
        }
        if (main == null && c.splitbark() > 0 && random.getAsDouble() < 1.0 / c.splitbark()) {
            main = new MinigameReward(random.getAsDouble() < 0.5 ? "runescroll_of_swampbark" : "runescroll_of_bloodbark", 1);
        }
        if (main == null) {
            double r = random.getAsDouble() * c.total();
            Line pick = c.main().get(c.main().size() - 1);
            for (Line l : c.main()) {
                r -= l.weight();
                if (r < 0) {
                    pick = l;
                    break;
                }
            }
            int n = pick.min() + (int) Math.floor(random.getAsDouble() * (pick.max() - pick.min() + 1));
            main = new MinigameReward(pick.item(), n);
        }
        out.add(main);
        int roll = c.coinRollMin() + (int) Math.floor(random.getAsDouble() * (c.coinRollMax() - c.coinRollMin() + 1));
        int coins = coins(roll, value.applyAsInt(main.item()) * main.amount());
        if (coins > 0) {
            out.add(new MinigameReward("coins", coins));
        }
        boolean bag = !c.metal().equals("bronze") && !owned.contains("flamtaer_bag") && random.getAsDouble() < 0.5;
        if (bag) {
            out.add(new MinigameReward("flamtaer_bag", 1));
        } else {
            int paste = c.pasteMin() + (int) Math.floor(random.getAsDouble() * (c.pasteMax() - c.pasteMin() + 1));
            out.add(new MinigameReward("swamp_paste", paste));
        }
        return new Opening(out);
    }

    private static final Map<String, Chest> CHESTS = new LinkedHashMap<>();

    public static Chest chest(String metal, String colour) {
        return CHESTS.get(metal + " " + colour);
    }

    public static List<Chest> chests() {
        return Collections.unmodifiableList(new ArrayList<>(CHESTS.values()));
    }

    static {
        Map<String, List<Line>> main = new LinkedHashMap<>();
        for (String line : ShadeChestRewards.MAIN.strip().split("\n")) {
            String[] f = line.trim().split(" ");
            main.computeIfAbsent(f[0] + " " + f[1], k -> new ArrayList<>())
                    .add(new Line(f[2], Integer.parseInt(f[3]), Integer.parseInt(f[4]), Double.parseDouble(f[5])));
        }
        for (String line : ShadeChestRewards.META.strip().split("\n")) {
            String[] f = line.trim().split(" ");
            String[] paste = f[5].split("-");
            String k = f[0] + " " + f[1];
            CHESTS.put(k, new Chest(f[0], f[1], Integer.parseInt(f[2]), Integer.parseInt(f[3]), Integer.parseInt(f[4]),
                    Integer.parseInt(paste[0]), Integer.parseInt(paste[1]), List.copyOf(main.get(k))));
        }
    }

    // ------------------------------------------------------------------ items

    private static void single(ItemSink items, String id, String name, String shape, int color, int value, String examine) {
        items.add(id, name, Kind.SIMPLE, shape, color).value(value).stack(1).examine(examine);
    }

    private static final int[] METAL_RGB = {0x8A5A2A, 0x9A9AA0, 0x2A2A2A, 0xD0D0E0, 0xE0B030};
    private static final int[] REMAINS_RGB = {0x8A8A7A, 0x7A7A8A, 0x6A6A8A, 0x5A5A7A, 0x4A4A6A, 0x3A2A4A};

    static void items(ItemSink items) {
        for (Shade s : Shade.values()) {
            single(items, s.remains(), s.displayName() + " remains", "bones", REMAINS_RGB[s.ordinal()], 1,
                    "The remains of a deadly shade.");
        }
        for (int m = 0; m < METALS.length; m++) {
            String cap = Character.toUpperCase(METALS[m].charAt(0)) + METALS[m].substring(1);
            for (String c : COLOURS) {
                single(items, key(METALS[m], c), cap + " key " + c, "key", METAL_RGB[m], 81,
                        "A " + METALS[m] + " key with a " + c + " painted eyelet (opens chests found in the Mort'ton catacombs).");
            }
            int[] lockValues = {800, 1600, 3200, 19200, 32000};
            single(items, METALS[m] + "_locks", cap + " locks", "key", METAL_RGB[m], lockValues[m],
                    "Perhaps someone can do something with this.");
        }
        for (PyreLog p : PyreLog.values()) {
            items.add(p.id(), p.displayName(), Kind.SIMPLE, "log", 0x8A6A3A).value(p.value())
                    .examine("Logs prepared with sacred oil for a funeral pyre.");
        }
        items.add("olive_oil", "Olive oil", Kind.POTION, "potion", 0xC0C060).value(22).stack(1).examine("4 doses of olive oil.");
        items.add("timber_beam", "Timber beam", Kind.SIMPLE, "plank", 0x8A6A3A).value(1)
                .examine("A hefty beam of timber, perfect for building temples.");
        single(items, "flamtaer_hammer", "Flamtaer hammer", "hammer", 0x8A6A4A, 1, "Repairs the Mort'ton temple faster.");
        single(items, "fine_cloth", "Fine cloth", "cloth", 0xE0D8C0, 500, "Amazingly untouched by time.");
        single(items, "flamtaer_bag", "Flamtaer bag", "package", 0x6A5A3A, 150,
                "A handy sack of supplies for repairing the Flamtaer Temple at Mort'ton.");
        single(items, "runescroll_of_swampbark", "Runescroll of swampbark", "note", 0x3A6A3A, 400,
                "A rough scroll with faint writing, on a deep green parchment.");
        single(items, "runescroll_of_bloodbark", "Runescroll of bloodbark", "note", 0x8A1A1A, 600,
                "A rough scroll with faint writing, on a malevolent dark red parchment.");
        items.add("amulet_of_the_damned", "Amulet of the damned", Kind.SIMPLE, "amulet", 0x6A2A6A).value(58001).stack(1)
                .examine("Heirloom of an ancient family of Morytania.");
        zealot(items, "zealots_helm", "Zealot's helm", "full_helm", Slot.HEAD, 24400, 1, "helmet");
        zealot(items, "zealots_robe_top", "Zealot's robe top", "platebody", Slot.CHEST, 25000, 7, "robe");
        zealot(items, "zealots_robe_bottom", "Zealot's robe bottom", "platelegs", Slot.LEGS, 24500, 6, "robe");
        zealot(items, "zealots_boots", "Zealot's boots", "boots", Slot.FEET, 24400, 1, "pair of boots");
        items.add("black_spear", "Black spear", Kind.MELEE_WEAPON, "spear", 0x2A2A2A).value(312).stack(1).tier("black")
                .equipment(new EquipmentDef(Slot.WEAPON, new EquipmentStats(15, 15, 15, 0, 0, 1, 1, 0, 0, 0, 16, 0, 0, 0),
                        Map.of(Skill.ATTACK, 10), 4, AttackType.STAB, WeaponClass.MELEE));
    }

    private static void zealot(ItemSink items, String id, String name, String shape, Slot slot, int value, int prayer, String what) {
        items.add(id, name, Kind.ARMOUR, shape, 0xE0E0D0).value(value).stack(1)
                .equipment(new EquipmentDef(slot, new EquipmentStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, prayer), Map.of(), 0,
                        AttackType.CRUSH, WeaponClass.NONE))
                .examine("A well fashioned " + what + " with a strange aura emanating.");
    }
}
