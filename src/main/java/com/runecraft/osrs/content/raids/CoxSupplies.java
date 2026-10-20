package com.runecraft.osrs.content.raids;

import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ItemSink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Chambers of Xeric preparation (OSRS wiki, July 2026 dump): the resource rooms' herbs, secondaries, fish and bats,
 * and raid herblore. Everything here is raid-only and wiped when a player leaves the Chambers.
 *
 * <ul>
 *   <li>Herbs: golpar (Farming 27, Herblore 47 to clean), buchu leaf (39, 52) and noxifer (55, 60), grown from seeds in
 *       the farming room's herb patches with a seed dibber (plant XP 4/6/12, harvest XP 10/15/30 per herb).</li>
 *   <li>Secondaries (stinkhorn mushroom, endarkened juice, cicely) drop together from scavenger beasts.</li>
 *   <li>Potions: herb + water-filled gourd vial + secondary; the strength made follows the Herblore level
 *       ((-) / normal / (+)). Overloads mix noxifer with an elder, twisted and kodai potion of the same strength;
 *       antipoisons are noxifer and cicely.</li>
 *   <li>Food: seven fish (Fishing) and seven bats (Hunter), tiers 0-6, healing 5 + 3 per tier.</li>
 * </ul>
 */
public final class CoxSupplies {
    private CoxSupplies() {
    }

    // ------------------------------------------------------------------ herbs, seeds and secondaries

    /** A CoX herb: seed, grimy and clean ids, Farming and Herblore levels and XP (OSRS wiki). */
    public enum Herb {
        GOLPAR("golpar", "Golpar", 27, 4, 10, 47, 1),
        BUCHU("buchu_leaf", "Buchu leaf", 39, 6, 15, 52, 2),
        NOXIFER("noxifer", "Noxifer", 55, 12, 30, 60, 3);

        public final String clean;
        public final String name;
        public final int farmingLevel;
        public final double plantXp;
        public final double harvestXp;
        public final int cleanLevel;
        public final double cleanXp;

        Herb(String clean, String name, int farmingLevel, double plantXp, double harvestXp, int cleanLevel, double cleanXp) {
            this.clean = clean;
            this.name = name;
            this.farmingLevel = farmingLevel;
            this.plantXp = plantXp;
            this.harvestXp = harvestXp;
            this.cleanLevel = cleanLevel;
            this.cleanXp = cleanXp;
        }

        public String grimy() {
            return "grimy_" + clean;
        }

        public String seed() {
            return (this == BUCHU ? "buchu" : clean) + "_seed";
        }

        public static Herb bySeed(String id) {
            for (Herb h : values()) {
                if (h.seed().equals(id)) {
                    return h;
                }
            }
            return null;
        }

        public static Herb byGrimy(String id) {
            for (Herb h : values()) {
                if (h.grimy().equals(id)) {
                    return h;
                }
            }
            return null;
        }

        public static Herb byClean(String id) {
            for (Herb h : values()) {
                if (h.clean.equals(id)) {
                    return h;
                }
            }
            return null;
        }
    }

    public static final String STINKHORN = "stinkhorn_mushroom";
    public static final String ENDARKENED = "endarkened_juice";
    public static final String CICELY = "cicely";
    public static final String EMPTY_GOURD = "empty_gourd_vial";
    public static final String WATER_GOURD = "water_filled_gourd_vial";
    public static final String CAVE_WORMS = "cave_worms";

    /** Game ticks a CoX herb takes to grow (OSRS wiki: fully grown in 30 seconds). */
    public static final int HERB_GROW_TICKS = 50;

    /** Herbs one patch yields. // approx: 3 plus one per 15 Farming levels above the herb's level, at most 7 */
    public static int herbYield(Herb herb, int farmingLevel) {
        return Math.min(7, 3 + Math.max(0, farmingLevel - herb.farmingLevel) / 15);
    }

    /** One rake of the Chambers' weeds (OSRS wiki, "Weeds (Chambers of Xeric)"): 1-3 golpar, buchu or noxifer seeds, 1/3 each. */
    public static Map.Entry<String, Integer> rakeSeeds(Random random) {
        Herb herb = Herb.values()[random.nextInt(3)];
        return Map.entry(herb.seed(), 1 + random.nextInt(3));
    }

    /** Seeds the demi-bosses drop (OSRS wiki): 5-10 of each from mystics and shamans, 3-5 of each from a Guardian. */
    public static final int[] MYSTIC_SEEDS = {5, 10};
    public static final int[] GUARDIAN_SEEDS = {3, 5};

    // ------------------------------------------------------------------ potions

    /** The three strengths: (-), normal and (+). */
    public enum Strength {
        WEAK(" (-)", "_minus"), NORMAL("", ""), STRONG(" (+)", "_plus");

        public final String suffix;
        public final String idSuffix;

        Strength(String suffix, String idSuffix) {
            this.suffix = suffix;
            this.idSuffix = idSuffix;
        }
    }

    /** A raid potion family: herb, secondary (null for overloads), Herblore levels and XP per strength (OSRS wiki). */
    public enum Potion {
        ELDER("elder", "Elder", Herb.GOLPAR, STINKHORN, new int[]{47, 59, 70}, new double[]{6.5, 10, 13}, 0xC8A050),
        TWISTED("twisted", "Twisted", Herb.GOLPAR, CICELY, new int[]{47, 59, 70}, new double[]{6.5, 10, 13}, 0x3A8A3A),
        KODAI("kodai", "Kodai", Herb.GOLPAR, ENDARKENED, new int[]{47, 59, 70}, new double[]{6.5, 10, 13}, 0x3A6AC0),
        REVITALISATION("revitalisation", "Revitalisation", Herb.BUCHU, STINKHORN, new int[]{52, 65, 78}, new double[]{13.5, 20, 26.5},
                0xC060C0),
        PRAYER_ENHANCE("prayer_enhance", "Prayer enhance", Herb.BUCHU, CICELY, new int[]{52, 65, 78}, new double[]{13.5, 20, 26.5},
                0x40C0C0),
        XERICS_AID("xerics_aid", "Xeric's aid", Herb.BUCHU, ENDARKENED, new int[]{52, 65, 78}, new double[]{13.5, 20, 26.5}, 0xE0D080),
        OVERLOAD("overload", "Overload", Herb.NOXIFER, null, new int[]{60, 75, 90}, new double[]{33.5, 50, 66.5}, 0x2A2A2A),
        /** Round 36: noxifer and cicely (OSRS wiki "Chambers of Xeric/Strategies", Herblore tables). */
        ANTIPOISON("cox_antipoison", "Antipoison", Herb.NOXIFER, CICELY, new int[]{60, 75, 90}, new double[]{13.5, 20, 26.5},
                0x60C060);

        public final String base;
        public final String name;
        public final Herb herb;
        public final String secondary;
        private final int[] levels;
        private final double[] xp;
        public final int colour;

        Potion(String base, String name, Herb herb, String secondary, int[] levels, double[] xp, int colour) {
            this.base = base;
            this.name = name;
            this.herb = herb;
            this.secondary = secondary;
            this.levels = levels;
            this.xp = xp;
            this.colour = colour;
        }

        public int level(Strength s) {
            return levels[s.ordinal()];
        }

        public double xp(Strength s) {
            return xp[s.ordinal()];
        }

        /** Item id, e.g. {@code elder_plus}, {@code elder_potion}, {@code xerics_aid}, {@code cox_overload_minus}. */
        public String id(Strength s) {
            if (s == Strength.NORMAL) {
                return switch (this) {
                    case PRAYER_ENHANCE, XERICS_AID -> base;
                    case OVERLOAD -> "cox_overload";
                    default -> base + "_potion";
                };
            }
            return (this == OVERLOAD ? "cox_overload" : base) + s.idSuffix;
        }

        /** OSRS names: Elder (-), Elder potion, Elder (+); Prayer enhance, Xeric's aid and Overload have no "potion". */
        public String displayName(Strength s) {
            boolean plain = this == PRAYER_ENHANCE || this == XERICS_AID || this == OVERLOAD;
            return s == Strength.NORMAL ? name + (plain ? "" : " potion") : name + s.suffix;
        }

        /** The strength a player with this Herblore level brews (the strongest allowed), or null below the (-) level. */
        public Strength strengthFor(int herbloreLevel) {
            Strength best = null;
            for (Strength s : Strength.values()) {
                if (herbloreLevel >= level(s)) {
                    best = s;
                }
            }
            return best;
        }
    }

    /** One raid potion item: its family and strength. */
    public record Brew(Potion potion, Strength strength) {
        public String id() {
            return potion.id(strength);
        }
    }

    private static final Map<String, Brew> BREWS = new LinkedHashMap<>();

    static {
        for (Potion p : Potion.values()) {
            for (Strength s : Strength.values()) {
                BREWS.put(p.id(s), new Brew(p, s));
            }
        }
    }

    /** The raid potion an item id names, or null. */
    public static Brew brew(String id) {
        return BREWS.get(id);
    }

    public static List<String> potionIds() {
        return new ArrayList<>(BREWS.keySet());
    }

    /**
     * The potion a herb and secondary make (golpar + stinkhorn: elder, + cicely: twisted, + endarkened juice: kodai;
     * buchu + stinkhorn: revitalisation, + cicely: prayer enhance, + endarkened juice: Xeric's aid), or null.
     */
    public static Potion potionOf(Herb herb, String secondary) {
        for (Potion p : Potion.values()) {
            if (p.herb == herb && secondary != null && secondary.equals(p.secondary)) {
                return p;
            }
        }
        return null;
    }

    /** {@code floor(level x percent / 100) + flat}. */
    static int pct(int level, int flat, int percent) {
        return level * percent / 100 + flat;
    }

    /** Elder/twisted/kodai/overload boost (OSRS): 4 + 10%, 5 + 13%, 6 + 16% of the base level. */
    public static int combatBoost(Strength s, int level) {
        return switch (s) {
            case WEAK -> pct(level, 4, 10);
            case NORMAL -> pct(level, 5, 13);
            case STRONG -> pct(level, 6, 16);
        };
    }

    /** Skills a raid combat potion boosts: elder Attack/Strength/Defence, twisted Ranged and Defence, kodai Magic. */
    public static List<Skill> boosted(Potion p) {
        return switch (p) {
            case ELDER -> List.of(Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE);
            case TWISTED -> List.of(Skill.RANGED, Skill.DEFENCE);
            case KODAI -> List.of(Skill.MAGIC);
            case OVERLOAD -> List.of(Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC);
            default -> List.of();
        };
    }

    /**
     * Raid antipoison: cures poison and protects against it for 90 seconds (-), six minutes (normal) or nine minutes (+),
     * like an antipoison, a superantipoison and an antidote+ (OSRS wiki).
     */
    public static int antipoisonImmunityTicks(Strength s) {
        return switch (s) {
            case WEAK -> 150;
            case NORMAL -> 600;
            case STRONG -> 900;
        };
    }

    /** Overload: 50 hitpoints of damage in hits of 10, boosts re-applied every 15 s for 5 minutes, then 50 healed. */
    public static final int OVERLOAD_DAMAGE = 50;
    public static final int OVERLOAD_HIT = 10;
    public static final int OVERLOAD_REAPPLY_TICKS = 25;
    public static final int OVERLOAD_TICKS = 500;

    /** Xeric's aid hitpoints restored, above the maximum (OSRS): 7% + 1, 12% + 2, 15% + 5 of the base level. */
    public static int xericsAidHeal(Strength s, int hitpointsLevel) {
        return switch (s) {
            case WEAK -> pct(hitpointsLevel, 1, 7);
            case NORMAL -> pct(hitpointsLevel, 2, 12);
            case STRONG -> pct(hitpointsLevel, 5, 15);
        };
    }

    /** Xeric's aid Defence boost (OSRS): 14% + 1, 18% + 2, 20% + 5. */
    public static int xericsAidDefence(Strength s, int defenceLevel) {
        return switch (s) {
            case WEAK -> pct(defenceLevel, 1, 14);
            case NORMAL -> pct(defenceLevel, 2, 18);
            case STRONG -> pct(defenceLevel, 5, 20);
        };
    }

    /** Xeric's aid drain of Attack, Strength, Ranged and Magic, from the current level (OSRS): 7% + 1, 9% + 2, 10% + 4. */
    public static int xericsAidDrain(Strength s, int currentLevel) {
        return switch (s) {
            case WEAK -> pct(currentLevel, 1, 7);
            case NORMAL -> pct(currentLevel, 2, 9);
            case STRONG -> pct(currentLevel, 4, 10);
        };
    }

    /**
     * Revitalisation restore of drained stats and prayer (OSRS: 11 + 30% of the base level for the (+)).
     * // approx: (-) 7 + 20%, normal 9 + 25%
     */
    public static int revitalisationRestore(Strength s, int level) {
        return switch (s) {
            case WEAK -> pct(level, 7, 20);
            case NORMAL -> pct(level, 9, 25);
            case STRONG -> pct(level, 11, 30);
        };
    }

    /**
     * Prayer enhance restorations per dose (Mod Ash, Feb 2023, for the (+)): floor(Prayer / 2 + 31) points, one at a
     * time, every floor(500 / that) ticks (6 ticks at Prayer 82+), over about 290 seconds. The wiki has no numbers for the
     * weaker two yet (they last 275 or 290 seconds). // approx: (-) and normal give 20 and 10 fewer
     */
    public static int prayerEnhanceTimes(Strength s, int prayerLevel) {
        int strong = prayerLevel / 2 + 31;
        return switch (s) {
            case WEAK -> strong - 20;
            case NORMAL -> strong - 10;
            case STRONG -> strong;
        };
    }

    /** Game ticks between prayer enhance restorations (Mod Ash): 500 / times, at most 63. */
    public static int prayerEnhanceInterval(Strength s, int prayerLevel) {
        return Math.min(63, 500 / Math.max(1, prayerEnhanceTimes(s, prayerLevel)));
    }

    // ------------------------------------------------------------------ food

    /** A CoX fish or bat: raw/cooked ids, gathering skill, levels and XP (OSRS wiki). */
    public record Food(String name, String raw, String cooked, Skill skill, int level, double gatherXp, double cookXp, int heal) {
    }

    private static final String[] FISH = {"pysk", "suphi", "leckish", "brawk", "mycil", "roqed", "kyren"};
    private static final String[] BATS = {"guanic", "prael", "giral", "phluxia", "kryket", "murng", "psykk"};
    public static final List<Food> FOODS;

    static {
        List<Food> foods = new ArrayList<>();
        int[] levels = {1, 15, 30, 45, 60, 75, 90};
        for (int t = 0; t < 7; t++) {
            String f = FISH[t];
            foods.add(new Food(cap(f) + " fish", "raw_" + f + "_fish", f + "_fish", Skill.FISHING, levels[t], 20 + 3 * t, 10 + 3 * t,
                    5 + 3 * t));
        }
        for (int t = 0; t < 7; t++) {
            String b = BATS[t];
            foods.add(new Food(cap(b) + " bat", "raw_" + b + "_bat", b + "_bat", Skill.HUNTER, levels[t], 5 + 4 * t, 10 + 3 * t,
                    5 + 3 * t));
        }
        FOODS = Collections.unmodifiableList(foods);
    }

    private static String cap(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * The fish or bat caught at this level: the best one allowed (OSRS: higher levels catch better fish and bats).
     * // approx: always the best allowed tier
     */
    public static Food catchFor(Skill skill, int level) {
        Food best = null;
        for (Food f : FOODS) {
            if (f.skill() == skill && level >= f.level()) {
                best = f;
            }
        }
        return best;
    }

    /**
     * The chance of burning raw CoX food on the fire. The top tier (kyren fish, psykk bat; level 90) follows the OSRS
     * wiki's kyren cooking chart: the skilling roll with low 163, high 255, so about 3% burn at 90 and none at 99.
     * // approx: the wiki charts only the kyren fish; the psykk bat shares it and the lower tiers burn 50% at their
     * level, falling to none 30 levels above it
     */
    public static double burnChance(int cookingLevel, Food food) {
        if (food.level() >= 90) {
            return Math.max(0, 1 - CoxRules.skillRoll(cookingLevel, 163, 255));
        }
        int over = cookingLevel - food.level();
        return over >= 30 ? 0 : Math.max(0, 0.5 - over * 0.5 / 30);
    }

    /** The burnt item a CoX fish or bat turns into. */
    public static String burnt(Food food) {
        return food.skill() == Skill.FISHING ? BURNT_FISH : BURNT_BAT;
    }

    public static final String BURNT_FISH = "burnt_cox_fish";
    public static final String BURNT_BAT = "burnt_bat";
    /** Vespula's room: medivaemia blossoms heal the lux grubs (OSRS). */
    public static final String BLOSSOM = "medivaemia_blossom";
    /** Tightrope room: the keystone crystal from the far side dispels the shimmering barrier (OSRS). */
    public static final String KEYSTONE = "keystone_crystal";

    /**
     * The CoX food tier a resource room yields (OSRS wiki: "the tier being determined by the team's overall Hunter/Fishing
     * level"): the best tier the team's average level allows. // approx: average rather than the hidden team formula
     */
    public static Food teamCatch(Skill skill, int averageLevel) {
        return catchFor(skill, averageLevel);
    }

    public static Food foodByRaw(String id) {
        for (Food f : FOODS) {
            if (f.raw().equals(id)) {
                return f;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ scavenger beasts

    /**
     * Two rolls on the scavenger beast's table (OSRS wiki): 1/18 each for a fishing rod, iron pickaxe, iron axe,
     * butterfly net, hammer and tinderbox, 2/18 lockpick, 2/18 30-50 cave worms, 6/18 for the secondaries (3-11
     * stinkhorn, 5-14 endarkened juice and 3-6 cicely, dropped together) and 2/18 for two mallignum root planks
     * (not modelled: the floor's bridges and chests need none). Bones always drop.
     */
    public static Map<String, Integer> scavengerDrop(Random random) {
        Map<String, Integer> out = new LinkedHashMap<>();
        out.put("bones", 1);
        for (int roll = 0; roll < 2; roll++) {
            int r = random.nextInt(18);
            switch (r) {
                case 0 -> out.merge("fishing_rod", 1, Integer::sum);
                case 1 -> out.merge("iron_pickaxe", 1, Integer::sum);
                case 2 -> out.merge("iron_axe", 1, Integer::sum);
                case 3 -> out.merge("butterfly_net", 1, Integer::sum);
                case 4 -> out.merge("hammer", 1, Integer::sum);
                case 5 -> out.merge("tinderbox", 1, Integer::sum);
                case 6, 7 -> out.merge("lockpick", 1, Integer::sum);
                case 8, 9 -> out.merge(CAVE_WORMS, 30 + random.nextInt(21), Integer::sum);
                case 10, 11, 12, 13, 14, 15 -> {
                    out.merge(STINKHORN, 3 + random.nextInt(9), Integer::sum);
                    out.merge(ENDARKENED, 5 + random.nextInt(10), Integer::sum);
                    out.merge(CICELY, 3 + random.nextInt(4), Integer::sum);
                }
                default -> {
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ items

    /** Every item id defined here (all raid-only). */
    public static final List<String> ITEMS;

    static {
        List<String> ids = new ArrayList<>();
        for (Herb h : Herb.values()) {
            ids.add(h.seed());
            ids.add(h.grimy());
            ids.add(h.clean);
        }
        ids.addAll(List.of(STINKHORN, ENDARKENED, CICELY, EMPTY_GOURD, WATER_GOURD, CAVE_WORMS, BURNT_FISH, BURNT_BAT, BLOSSOM,
                KEYSTONE));
        for (Food f : FOODS) {
            ids.add(f.raw());
            ids.add(f.cooked());
        }
        ids.addAll(BREWS.keySet());
        ITEMS = Collections.unmodifiableList(ids);
    }

    static void add(ItemSink items) {
        for (Herb h : Herb.values()) {
            items.add(h.seed(), h.name.replace(" leaf", "") + " seed", Kind.SIMPLE, "seed", 0x7A6A3A).value(1)
                    .examine("Plant it in a herb patch in the Chambers of Xeric.");
            items.add(h.grimy(), "Grimy " + h.name.toLowerCase(java.util.Locale.ROOT), Kind.SIMPLE, "herb_grimy", 0x4A5A2A).value(1)
                    .examine("I need to clean this herb before I can use it.");
            items.add(h.clean, h.name, Kind.SIMPLE, "herb", h == Herb.NOXIFER ? 0x5A3A6A : h == Herb.BUCHU ? 0x6A9A3A : 0x8A8A3A)
                    .value(1).examine("A herb that only grows in the Chambers of Xeric.");
        }
        items.add(STINKHORN, "Stinkhorn mushroom", Kind.SIMPLE, "secondary", 0xA08A6A).value(1).examine("It smells terrible.");
        items.add(ENDARKENED, "Endarkened juice", Kind.SIMPLE, "vial", 0x2A1A3A).value(1).examine("A dark, viscous liquid.");
        items.add(CICELY, "Cicely", Kind.SIMPLE, "secondary", 0xE0E0F0).value(1).examine("A delicate white flower.");
        items.add(EMPTY_GOURD, "Empty gourd vial", Kind.SIMPLE, "vial", 0xA08A50).value(1).examine("Fill it with water from a geyser.");
        items.add(WATER_GOURD, "Water-filled gourd vial", Kind.SIMPLE, "vial_water", 0x6A9AC0).value(1)
                .examine("Mix a herb into it to start a potion.");
        items.add(CAVE_WORMS, "Cave worms", Kind.SIMPLE, "bait", 0xC8A0A0).value(1).stack(Integer.MAX_VALUE)
                .examine("Bait for the fish in the Chambers of Xeric.");
        for (Food f : FOODS) {
            boolean fish = f.skill() == Skill.FISHING;
            items.add(f.raw(), "Raw " + f.name().toLowerCase(java.util.Locale.ROOT), Kind.SIMPLE, fish ? "fish_raw" : "meat_raw",
                    fish ? 0x7A8AA0 : 0x8A5A4A).value(1).examine("I should cook this on the fire.");
            items.add(f.cooked(), cap(f.name().toLowerCase(java.util.Locale.ROOT)), Kind.FOOD, fish ? "fish_cooked" : "meat_cooked",
                    fish ? 0xB08A5A : 0x9A6A3A).value(1).heal(f.heal()).examine("Heals " + f.heal() + " hitpoints.");
        }
        items.add(BURNT_FISH, "Burnt fish", Kind.SIMPLE, "fish_burnt", 0x2A2A2A).value(1).examine("Oops!");
        items.add(BURNT_BAT, "Burnt bat", Kind.SIMPLE, "meat_burnt", 0x2A2A2A).value(1).examine("Oops!");
        items.add(BLOSSOM, "Medivaemia blossom", Kind.SIMPLE, "secondary", 0xE070B0).value(1)
                .examine("Use it on a lux grub to heal it.");
        items.add(KEYSTONE, "Keystone crystal", Kind.SIMPLE, "shard", 0x9AE0F0).value(1).stack(1)
                .examine("Use it on the shimmering barrier.");
        for (Brew b : BREWS.values()) {
            items.add(b.id(), b.potion().displayName(b.strength()), Kind.POTION, "potion", b.potion().colour).value(1).stack(1)
                    .examine("A potion brewed in the Chambers of Xeric. It cannot leave them.");
        }
    }
}
