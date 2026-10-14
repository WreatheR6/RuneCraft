package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentDef.Slot;
import com.runecraft.osrs.EquipmentDef.WeaponClass;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tai Bwo Wannai Cleanup (needs Jungle Potion), after the OSRS wiki. Hacking light / medium / dense
 * jungle with a machete (Woodcutting 10 / 20 / 35, 32 / 55 / 80 XP) gives thatching spars and 0.3% /
 * 0.5% / 1% favour; repairing the rotten village fence with a spar gives 0.1% / 0.3% / 0.5% favour and
 * 28.1 / 42.1 / 65.1 XP, and a repaired fence can be reinforced once before it decays back. Monsters
 * that turn up while hacking (and, once you've had your first trading sticks, the jungle spiders north
 * of the village) give 1-3% favour. Villagers trade favour for trading sticks, with a cooldown of 1,500 to
 * 1,800 ticks per villager; Murcaily must hand out the first sticks, after a fence repair. Gabooty's
 * Cooperative sells villager clothing, gem machetes, gems and gout tubers for sticks at 1.2 times their
 * value. Reaching 100% favour fires the {@code tbw_favour:100} diary activity.
 *
 * <p>Favour is kept in tenths of a percent (0-1,000). The wiki doesn't give a favour decay (only the
 * fence decays), so favour is lost only by trading it for sticks.
 */
public final class TaiBwoWannai {
    private TaiBwoWannai() {
    }

    public static final String QUEST = "jungle_potion";
    public static final String PLACE = "mg_tai_bwo_wannai_cleanup";
    /** Murcaily's spot by the Hardwood Grove gate (OSRS tile). */
    public static final int X = 2816;
    public static final int Y = 3084;
    public static final int MAX_FAVOUR = 1000;
    public static final String STICKS = "trading_sticks";
    public static final String SHOP = "tai_bwo_wannai";
    public static final String FENCE = "tbw_village_fence";
    /** Ticks a villager waits before giving sticks again: six steps of 300 ticks (wiki: 1,500 to 1,800). */
    public static final int STICK_COOLDOWN_TICKS = 1800;
    /** Fence levels: 0 rotten, 1 repaired, 2 reinforced. */
    public static final int FENCE_MAX = 2;
    /** Game ticks before a repaired fence decays one level. // approx: the wiki only says "eventually" */
    public static final int FENCE_DECAY_TICKS = 6000;
    /** Chance a successful hack turns up an event (wiki: "between 10% and 20%"). // approx */
    public static final double EVENT_CHANCE = 0.15;

    /** The three jungle types: Woodcutting level, XP, favour (tenths of %) and the spar they give. */
    public enum Jungle {
        LIGHT("tbw_light_jungle", "Light jungle", 10, 32, 3, "thatch_spar_light"),
        MEDIUM("tbw_medium_jungle", "Medium jungle", 20, 55, 5, "thatch_spar_med"),
        DENSE("tbw_dense_jungle", "Dense jungle", 35, 80, 10, "thatch_spar_dense");

        public final String block;
        public final String name;
        public final int level;
        public final double xp;
        public final int favour;
        public final String spar;

        Jungle(String block, String name, int level, double xp, int favour, String spar) {
            this.block = block;
            this.name = name;
            this.level = level;
            this.xp = xp;
            this.favour = favour;
            this.spar = spar;
        }

        public static Jungle byBlock(String block) {
            for (Jungle j : values()) {
                if (j.block.equals(block)) {
                    return j;
                }
            }
            return null;
        }
    }

    /** Thatching spars used on the fence: favour (tenths of %) and Woodcutting XP. */
    public enum Spar {
        LIGHT("thatch_spar_light", 1, 28.1),
        MEDIUM("thatch_spar_med", 3, 42.1),
        DENSE("thatch_spar_dense", 5, 65.1);

        public final String item;
        public final int favour;
        public final double xp;

        Spar(String item, int favour, double xp) {
            this.item = item;
            this.favour = favour;
            this.xp = xp;
        }
    }

    /** Machetes, worst to best; better ones hack faster. */
    public static final List<String> MACHETES = List.of("machete", "opal_machete", "jade_machete", "red_topaz_machete");

    /**
     * Chance to hack a jungle in one attempt: 30% at its level plus 1% a level above it, +10% per
     * machete tier, at most 95%. // approx: the wiki gives only "progressively higher speed"
     */
    public static double hackChance(Jungle j, int woodcutting, int macheteTier) {
        if (woodcutting < j.level || macheteTier < 0) {
            return 0;
        }
        return Math.min(0.95, 0.30 + 0.01 * (woodcutting - j.level) + 0.10 * macheteTier);
    }

    /** Gout tuber event chance (Mod Ash): 1/100 at Woodcutting 10, interpolated to 1/33 at 99. */
    public static double goutTuberChance(int woodcutting) {
        int lvl = Math.max(10, Math.min(99, woodcutting));
        return 1 / 100.0 + (lvl - 10) / 89.0 * (1 / 33.0 - 1 / 100.0);
    }

    /** The event table when the gout tuber roll fails (Mod Ash, out of 99). */
    public enum Event {
        JUNGLE_SPIDER(22, "jungle_spider", 15), TRIBESMAN(21, null, 15), BUSH_SNAKE(21, null, 10),
        LARGE_MOSQUITO(8, null, 10), MOSQUITO_SWARM(7, null, 10), MOSQUITO_SWARM_20(6, null, 10),
        GEM_ROCK(5, null, 0), BROODOO_FOOD(4, null, 30), BROODOO_POISON(3, null, 30), BROODOO_DISEASE(2, null, 30);

        public final int weight;
        /** The creature spawned, or null where the monster isn't in the world (its favour is given instead). */
        public final String creature;
        /** Favour for killing it (tenths of %): 1% for mosquitoes and snakes, 1-2% (1.5) tribesmen and spiders, ~3% broodoo. */
        public final int favour;

        Event(int weight, String creature, int favour) {
            this.weight = weight;
            this.creature = creature;
            this.favour = favour;
        }

        public static Event roll(double r) {
            int total = 0;
            for (Event e : values()) {
                total += e.weight;
            }
            int x = (int) Math.floor(r * total);
            for (Event e : values()) {
                x -= e.weight;
                if (x < 0) {
                    return e;
                }
            }
            return BROODOO_DISEASE;
        }
    }

    /** Favour after gaining {@code gain} tenths (capped at 100%). */
    public static int addFavour(int favour, int gain) {
        return Math.max(0, Math.min(MAX_FAVOUR, favour + gain));
    }

    /**
     * Trading sticks a villager hands over: up to 10% of favour (100 tenths) is exchanged for one stick
     * per tenth, plus 5% per piece of villager clothing worn. Returns {sticks, favour spent}.
     * // approx: wiki "100 sticks ... favour reduced by around 10%"; the clothing bonus isn't documented
     */
    public static int[] trade(int favour, int clothingPieces) {
        int take = Math.min(100, Math.max(0, favour));
        int sticks = take * (100 + 5 * Math.max(0, Math.min(5, clothingPieces))) / 100;
        return new int[]{sticks, take};
    }

    /** Gabooty's price in trading sticks: 1.2 times the item's value (its value with Karamja gloves, not modelled). */
    public static int price(int value) {
        return value * 6 / 5;
    }

    /** Villagers that give trading sticks (Murcaily first; his NPC is the quests module's). */
    public static final List<String> SUPPLIERS = List.of("murcaily", "mg_gabooty", "mg_sharimika", "mg_layleen", "mg_mamma_bufetta",
            "mg_karaday", "mg_rionasta", "mg_safta_doc", "mg_fanellaman", "mg_jagbakoba");

    public static final String[] COLOURS = {"brown", "blue", "yellow", "pink"};
    private static final int[] COLOUR_RGB = {0x8A5A2A, 0x3A5AB0, 0xD0B030, 0xD07AA0};

    /** Villager clothing piece ids for one colour. */
    public static List<String> clothing(String colour) {
        return List.of("villager_hat_" + colour, "tribal_top_" + colour, "villager_armband_" + colour, "villager_robe_" + colour,
                "villager_sandals_" + colour);
    }

    /** Item value of each clothing piece (hat, top, armband, robe, sandals): Gabooty sells at 240 / 360 / 180 / 300 / 120. */
    private static final int[] CLOTHING_VALUE = {200, 300, 150, 250, 100};

    public static void items(ItemSink items) {
        items.add(STICKS, "Trading sticks", Kind.SIMPLE, "shaft", 0xB08A50).value(1).examine("Karamja currency.");
        items.add("thatch_spar_light", "Thatch spar light", Kind.SIMPLE, "shaft", 0xC8B070).value(1)
                .examine("A light wooden spar for thatching.");
        items.add("thatch_spar_med", "Thatch spar med", Kind.SIMPLE, "shaft", 0xA88A50).value(1)
                .examine("A medium wooden spar for thatching.");
        items.add("thatch_spar_dense", "Thatch spar dense", Kind.SIMPLE, "shaft", 0x8A6A3A).value(1)
                .examine("A dense wooden spar for thatching.");
        items.add("gout_tuber", "Gout tuber", Kind.SIMPLE, "produce", 0x9A7A4A).value(200).stack(1)
                .examine("A rare and valuable tuber.");
        items.add("opal_machete", "Opal machete", Kind.TOOL, "scimitar", 0xC0D0D0).value(500).stack(1)
                .examine("A machete with an opal set in the blade.");
        items.add("jade_machete", "Jade machete", Kind.TOOL, "scimitar", 0x6AB06A).value(1000).stack(1)
                .examine("A machete with a jade set in the blade.");
        items.add("red_topaz_machete", "Red topaz machete", Kind.TOOL, "scimitar", 0xC05A3A).value(2000).stack(1)
                .examine("A machete with a red topaz set in the blade.");
        String[] names = {"Villager hat", "Tribal top", "Villager armband", "Villager robe", "Villager sandals"};
        String[] shapes = {"cowl", "leather_body", "gloves", "chaps", "boots"};
        Slot[] slots = {Slot.HEAD, Slot.CHEST, null, Slot.LEGS, Slot.FEET};
        EquipmentStats none = new EquipmentStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        for (int c = 0; c < COLOURS.length; c++) {
            List<String> ids = clothing(COLOURS[c]);
            for (int i = 0; i < ids.size(); i++) {
                String name = names[i] + " (" + COLOURS[c] + ")";
                if (slots[i] == null) {
                    // No hands slot: the armband counts while carried.
                    items.add(ids.get(i), name, Kind.SIMPLE, shapes[i], COLOUR_RGB[c]).value(CLOTHING_VALUE[i]).stack(1)
                            .examine("Tai Bwo Wannai villager clothing.");
                } else {
                    items.add(ids.get(i), name, Kind.ARMOUR, shapes[i], COLOUR_RGB[c]).value(CLOTHING_VALUE[i]).stack(1)
                            .equipment(new EquipmentDef(slots[i], none, Map.of(), 0, AttackType.CRUSH, WeaponClass.NONE))
                            .examine("Tai Bwo Wannai villager clothing.");
                }
            }
        }
    }

    /** Gabooty's Tai Bwo Wannai Cooperative (prices: 1.2 x value, OSRS wiki). */
    static void shop(RewardShop shop) {
        String cur = "item:" + STICKS;
        for (String colour : COLOURS) {
            List<String> ids = clothing(colour);
            for (int i = 0; i < ids.size(); i++) {
                shop.item(ids.get(i), cur, price(CLOTHING_VALUE[i]));
            }
        }
        shop.item("opal_machete", cur, price(500)).item("jade_machete", cur, price(1000)).item("red_topaz_machete", cur, price(2000));
        // Gems and gout tubers: the store starts with none in stock; item values 15 / 30 / 60 (uncut 15 / 20 / 40). // approx stock
        shop.item("uncut_opal", cur, price(15)).item("uncut_jade", cur, price(20)).item("uncut_red_topaz", cur, price(40))
                .item("gout_tuber", cur, price(200));
    }

    public static List<BlockDef> blocks() {
        List<BlockDef> list = new ArrayList<>();
        for (Jungle j : Jungle.values()) {
            list.add(BlockDef.scripted(j.block, j.name, j == Jungle.LIGHT ? 0x5A8A3A : j == Jungle.MEDIUM ? 0x3A6A2A : 0x2A4A1A, "bush"));
        }
        list.add(BlockDef.scripted(FENCE, "Village fence", 0x8A6A3A, "crate"));
        return list;
    }

    public static List<PlaceDef> places() {
        return List.of(PlaceDef.of(PLACE, "Tai Bwo Wannai Cleanup", X, Y, PlaceDef.Style.NONE, blocks().stream().map(BlockDef::id).toList()));
    }

    /** The trading-stick villagers, around Murcaily (offsets in blocks from his spot). */
    public static List<NpcDef> npcs() {
        List<NpcDef> list = new ArrayList<>();
        int dark = 0x6A4A2A;
        list.add(NpcDef.of("mg_gabooty", "Gabooty", PLACE, -44, 38, dark, 0xC08030, 0x6A4A2A, 0x3A2A1A));
        list.add(NpcDef.of("mg_sharimika", "Sharimika", PLACE, -44, 16, dark, 0xB04A3A, 0x6A4A2A, 0x2A1A1A));
        list.add(NpcDef.of("mg_layleen", "Layleen", PLACE, -60, 20, dark, 0x3A8A5A, 0x6A4A2A, 0x2A1A1A));
        list.add(NpcDef.of("mg_mamma_bufetta", "Mamma Bufetta", PLACE, -44, 16 + 4, dark, 0xD0A040, 0x6A4A2A, 0x2A1A1A));
        list.add(NpcDef.of("mg_karaday", "Karaday", PLACE, -34, 0, dark, 0x8A6A3A, 0x6A4A2A, 0x2A1A1A));
        list.add(NpcDef.of("mg_rionasta", "Rionasta", PLACE, -68, -20, dark, 0x5A6AA0, 0x6A4A2A, 0x2A1A1A));
        list.add(NpcDef.of("mg_safta_doc", "Safta Doc", PLACE, -52, -32, dark, 0xA0A0A0, 0x6A4A2A, 0x2A1A1A));
        list.add(NpcDef.of("mg_fanellaman", "Fanellaman", PLACE, -114, -24, dark, 0x6A8A3A, 0x6A4A2A, 0x2A1A1A));
        list.add(NpcDef.of("mg_jagbakoba", "Jagbakoba", PLACE, -34, 74, dark, 0xA05A2A, 0x6A4A2A, 0x2A1A1A));
        return list;
    }
}
