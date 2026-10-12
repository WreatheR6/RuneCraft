package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.PlaceDef;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Farming: tree, fruit tree, bush, hops, hardwood, calquat, celastrus, redwood, spirit tree, cactus,
 * seaweed, mushroom, belladonna and grape patches; compost bins (compost, supercompost,
 * ultracompost); tree seeds grown into saplings in plant pots; and bird house spaces. Levels, XP
 * and growth times are OSRS values (growth is real-time, measured in world time).
 */
public final class FarmingData extends SkillingPart {

    public enum Patch {
        TREE("tree_patch", "Tree patch"),
        FRUIT_TREE("fruit_tree_patch", "Fruit tree patch"),
        BUSH("bush_patch", "Bush patch"),
        HOPS("hops_patch", "Hops patch"),
        HARDWOOD("hardwood_patch", "Hardwood tree patch"),
        CALQUAT("calquat_patch", "Calquat patch"),
        CELASTRUS("celastrus_patch", "Celastrus patch"),
        REDWOOD("redwood_patch", "Redwood tree patch"),
        SPIRIT("spirit_tree_patch", "Spirit tree patch"),
        CACTUS("cactus_patch", "Cactus patch"),
        SEAWEED("seaweed_patch", "Seaweed patch"),
        MUSHROOM("mushroom_patch", "Mushroom patch"),
        BELLADONNA("belladonna_patch", "Belladonna patch"),
        GRAPE("grape_patch", "Grapevine patch");

        public final String blockId;
        public final String displayName;

        Patch(String blockId, String displayName) {
            this.blockId = blockId;
            this.displayName = displayName;
        }

        public static Patch byBlock(String id) {
            for (Patch p : values()) {
                if (p.blockId.equals(id)) {
                    return p;
                }
            }
            return null;
        }
    }

    /** How a grown crop is harvested. */
    public enum Harvest {
        /** Check health, then chop like a normal tree (woodcutting); dig up the stump for roots. */
        CHOP,
        /** Check health, then pick up to N produce which regrow one every regrowMinutes. */
        PICK,
        /** Harvest everything at once (min..max produce). */
        ALL,
        /** Check health; becomes a spirit tree (teleport network). */
        SPIRIT
    }

    /**
     * One crop. {@code planted} is the item used (sapling for trees, seeds/spores otherwise),
     * {@code count} how many are needed. {@code produce} is the harvested item or, for CHOP crops,
     * the Tree enum name whose logs are chopped.
     */
    public record Crop(String id, String name, Patch patch, Harvest harvest, int level, double plantXp, double checkXp,
                       double harvestXp, String seed, String planted, int count, String produce, int minYield, int maxYield,
                       int growthMinutes, int regrowMinutes, String roots, int color) {
    }

    public static final List<Crop> CROPS = new ArrayList<>();

    private static void tree(String id, String name, String treeEnum, int level, double plant, double check, int minutes,
                             String roots, int color) {
        CROPS.add(new Crop(id, name, Patch.TREE, Harvest.CHOP, level, plant, check, 0, seedOf(id), id + "_sapling", 1,
                treeEnum, 0, 0, minutes, 0, roots, color));
    }

    private static void fruit(String id, String name, String produce, int level, double plant, double check, double harvest,
                              int color) {
        CROPS.add(new Crop(id, name, Patch.FRUIT_TREE, Harvest.PICK, level, plant, check, harvest, seedOf(id), id + "_sapling",
                1, produce, 6, 6, 960, 40, null, color));
    }

    private static void bush(String id, String name, String produce, int level, double plant, double check, double harvest,
                             int minutes, int color) {
        CROPS.add(new Crop(id, name, Patch.BUSH, Harvest.PICK, level, plant, check, harvest, id + "_seed", id + "_seed", 1,
                produce, 4, 4, minutes, 20, null, color));
    }

    private static void hops(String id, String name, String produce, int level, double plant, double harvest, int minutes,
                             int seeds, int color) {
        CROPS.add(new Crop(id, name, Patch.HOPS, Harvest.ALL, level, plant, 0, harvest, id + "_seed", id + "_seed", seeds,
                produce, 3, 10, minutes, 0, null, color));
    }

    private static String seedOf(String id) {
        return id.equals("oak") ? "acorn" : id + "_seed";
    }

    static {
        tree("oak", "Oak tree", "OAK", 15, 14.0, 467.3, 200, "oak_roots", 0x8A6A3A);
        tree("willow", "Willow tree", "WILLOW", 30, 25.0, 1456.5, 280, "willow_roots", 0x6E7A3A);
        tree("maple", "Maple tree", "MAPLE", 45, 45.0, 3403.4, 320, "maple_roots", 0xA0502A);
        tree("yew", "Yew tree", "YEW", 60, 81.0, 7069.9, 400, "yew_roots", 0x5A4A2A);
        tree("magic", "Magic tree", "MAGIC", 75, 145.5, 13768.3, 480, "magic_roots", 0x3A8AA8);
        CROPS.add(new Crop("teak", "Teak tree", Patch.HARDWOOD, Harvest.CHOP, 35, 35.0, 7290.0, 0, "teak_seed", "teak_sapling", 1,
                "TEAK", 0, 0, 4480, 0, null, 0x9C7A4A));
        CROPS.add(new Crop("mahogany", "Mahogany tree", Patch.HARDWOOD, Harvest.CHOP, 55, 63.0, 15720.0, 0, "mahogany_seed",
                "mahogany_sapling", 1, "MAHOGANY", 0, 0, 5120, 0, null, 0x6A2A1A));
        CROPS.add(new Crop("redwood", "Redwood tree", Patch.REDWOOD, Harvest.CHOP, 90, 230.0, 22450.0, 0, "redwood_tree_seed",
                "redwood_sapling", 1, "REDWOOD", 0, 0, 6400, 0, null, 0xA03A2A));
        fruit("apple", "Apple tree", "cooking_apple", 27, 22.0, 1199.5, 8.5, 0x9AB040);
        fruit("banana", "Banana tree", "banana", 33, 28.0, 1750.5, 10.5, 0xE0D040);
        fruit("orange", "Orange tree", "orange", 39, 35.5, 2470.2, 13.5, 0xE09030);
        fruit("curry", "Curry tree", "curry_leaf", 42, 40.0, 2906.9, 15.0, 0x6A8A2A);
        fruit("pineapple", "Pineapple plant", "pineapple", 51, 57.0, 4605.7, 21.5, 0xE0C040);
        fruit("papaya", "Papaya tree", "papaya_fruit", 57, 72.0, 6146.4, 27.0, 0xE08030);
        fruit("palm", "Palm tree", "coconut", 68, 110.5, 10150.1, 41.5, 0x8A6A3A);
        fruit("dragonfruit", "Dragonfruit tree", "dragonfruit", 81, 140.0, 17335.0, 70.0, 0xE04080);
        CROPS.add(new Crop("calquat", "Calquat tree", Patch.CALQUAT, Harvest.PICK, 72, 129.5, 12096.0, 48.5, "calquat_tree_seed",
                "calquat_sapling", 1, "calquat_fruit", 6, 6, 1280, 40, null, 0xD06030));
        CROPS.add(new Crop("celastrus", "Celastrus tree", Patch.CELASTRUS, Harvest.PICK, 85, 204.0, 14130.0, 23.5,
                "celastrus_seed", "celastrus_sapling", 1, "celastrus_bark", 3, 5, 800, 40, null, 0xA07A5A));
        CROPS.add(new Crop("spirit", "Spirit tree", Patch.SPIRIT, Harvest.SPIRIT, 83, 199.5, 19301.8, 0, "spirit_seed",
                "spirit_sapling", 1, null, 0, 0, 3520, 0, null, 0x7A9A5A));
        bush("redberry", "Redberry bush", "redberries", 10, 11.5, 64.0, 4.5, 100, 0xC02040);
        bush("cadavaberry", "Cadavaberry bush", "cadava_berries", 22, 18.0, 102.5, 7.0, 120, 0x8A2A8A);
        bush("dwellberry", "Dwellberry bush", "dwellberries", 36, 31.5, 177.5, 12.0, 140, 0x3A5AA0);
        bush("jangerberry", "Jangerberry bush", "jangerberries", 48, 50.5, 284.5, 19.0, 160, 0x8AA030);
        bush("whiteberry", "Whiteberry bush", "white_berries", 59, 78.0, 437.5, 29.0, 160, 0xF0F0E0);
        bush("poison_ivy", "Poison ivy bush", "poison_ivy_berries", 70, 120.0, 675.0, 45.0, 160, 0x3A6A2A);
        hops("barley", "Barley", "barley", 3, 8.5, 9.5, 40, 4, 0xD0B060);
        hops("hammerstone", "Hammerstone hops", "hammerstone_hops", 4, 9.0, 10.0, 40, 4, 0x7A9A4A);
        hops("asgarnian", "Asgarnian hops", "asgarnian_hops", 8, 10.9, 12.0, 50, 4, 0x6A8A4A);
        hops("jute", "Jute", "jute_fibre", 13, 13.0, 14.5, 50, 3, 0x9A8A5A);
        hops("yanillian", "Yanillian hops", "yanillian_hops", 16, 14.5, 16.0, 60, 4, 0x7AAA5A);
        hops("krandorian", "Krandorian hops", "krandorian_hops", 21, 17.5, 19.5, 70, 3, 0x5A8A3A);
        hops("wildblood", "Wildblood hops", "wildblood_hops", 28, 23.0, 26.0, 80, 3, 0xA03A3A);
        CROPS.add(new Crop("cactus", "Cactus", Patch.CACTUS, Harvest.PICK, 55, 66.5, 374.0, 25.0, "cactus_seed", "cactus_seed", 1,
                "cactus_spine", 3, 3, 560, 20, null, 0x5A8A3A));
        CROPS.add(new Crop("potato_cactus", "Potato cactus", Patch.CACTUS, Harvest.PICK, 64, 68.0, 230.0, 68.0, "potato_cactus_seed",
                "potato_cactus_seed", 1, "potato_cactus", 6, 6, 70, 10, null, 0x8AA040));
        CROPS.add(new Crop("seaweed", "Giant seaweed", Patch.SEAWEED, Harvest.ALL, 23, 19.0, 0, 21.0, "seaweed_spore",
                "seaweed_spore", 1, "seaweed", 3, 7, 40, 0, null, 0x3A6A2A));
        CROPS.add(new Crop("bittercap", "Bittercap mushrooms", Patch.MUSHROOM, Harvest.ALL, 53, 61.5, 0, 57.7, "mushroom_spore",
                "mushroom_spore", 1, "mushroom", 6, 9, 240, 0, null, 0xC0A080));
        CROPS.add(new Crop("belladonna", "Belladonna", Patch.BELLADONNA, Harvest.ALL, 63, 91.0, 0, 128.0, "belladonna_seed",
                "belladonna_seed", 1, "cave_nightshade_flower", 1, 1, 320, 0, null, 0x4A2A6A));
        CROPS.add(new Crop("grape", "Grapevine", Patch.GRAPE, Harvest.ALL, 36, 40.0, 0, 40.0, "grape_seed", "grape_seed", 1,
                "grapes", 3, 5, 35, 0, null, 0x6A2A8A));
    }

    public static Crop byPlanted(Patch patch, String item) {
        for (Crop c : CROPS) {
            if (c.patch() == patch && c.planted().equals(item)) {
                return c;
            }
        }
        return null;
    }

    public static Crop byId(String id) {
        for (Crop c : CROPS) {
            if (c.id().equals(id)) {
                return c;
            }
        }
        return null;
    }

    /** Compost types: XP for applying to a patch and the extra harvest lives/yield. */
    public enum Compost {
        COMPOST("compost", 18.0, 1, 4.5), SUPERCOMPOST("supercompost", 26.0, 2, 8.5), ULTRACOMPOST("ultracompost", 36.0, 3, 10.0);

        public final String item;
        public final double applyXp;
        public final int bonus;
        /** XP for taking one bucket out of a bin. */
        public final double binXp;

        Compost(String item, double applyXp, int bonus, double binXp) {
            this.item = item;
            this.applyXp = applyXp;
            this.bonus = bonus;
            this.binXp = binXp;
        }

        public static Compost byItem(String item) {
            for (Compost c : values()) {
                if (c.item.equals(item)) {
                    return c;
                }
            }
            return null;
        }
    }

    /** Items that make supercompost in a bin (anything else compostable makes regular compost). */
    public static final List<String> SUPER_COMPOSTABLE = List.of("watermelon", "strawberry", "white_berries",
            "poison_ivy_berries", "pineapple", "papaya_fruit", "coconut_shell", "calquat_fruit", "dragonfruit", "snape_grass",
            "potato_cactus", "jangerberries", "curry_leaf");

    public static final List<String> COMPOSTABLE = List.of("potato", "onion", "cabbage", "tomato", "sweetcorn", "garlic",
            "redberries", "cadava_berries", "dwellberries", "cooking_apple", "banana", "orange", "barley", "hammerstone_hops",
            "asgarnian_hops", "jute_fibre", "yanillian_hops", "krandorian_hops", "wildblood_hops", "weeds", "seaweed",
            "mushroom", "minecraft:wheat", "minecraft:carrot", "minecraft:beetroot", "minecraft:apple");

    @Override
    public void items(ItemSink s) {
        tool(s, "spade", "Spade", "spade", 0x8A8A8A, 3);
        tool(s, "rake", "Rake", "rake", 0x8A6A3A, 6);
        tool(s, "secateurs", "Secateurs", "secateurs", 0x9A9AA0, 5);
        tool(s, "gardening_trowel", "Gardening trowel", "trowel", 0x8A8A8A, 5);
        simple(s, "weeds", "Weeds", "produce", 0x5A7A2A, 1);
        for (Compost c : Compost.values()) {
            simple(s, c.item, title(c.item), "bucket", c == Compost.COMPOST ? 0x5A4A2A : c == Compost.SUPERCOMPOST ? 0x3A3A1A : 0x2A2A2A,
                    c == Compost.COMPOST ? 2 : c == Compost.SUPERCOMPOST ? 30 : 200);
        }
        simple(s, "coconut_shell", "Coconut shell", "produce", 0x6A4A2A, 1);
        simple(s, "bird_nest", "Bird nest", "nest", 0x8A6A3A, 5);
        simple(s, "bird_nest_seeds", "Bird nest (seeds)", "nest", 0x8A7A3A, 30).examine("It's a bird's nest with some seeds in it.");
        simple(s, "bird_nest_ring", "Bird nest (ring)", "nest", 0xB09030, 200).examine("It's a bird's nest with a ring in it.");
        // Seeds, saplings and produce
        for (Crop c : CROPS) {
            if (!c.seed().equals(c.planted())) {
                s.add(c.seed(), seedName(c), Kind.SEED, "seed", c.color()).value(Math.max(5, c.level() * 4));
                simple(s, c.planted(), title(c.id()) + " sapling", "sapling", c.color(), Math.max(10, c.level() * 6));
            } else {
                s.add(c.seed(), seedName(c), Kind.SEED, "seed", c.color()).value(Math.max(2, c.level() * 2));
            }
        }
        food(s, "redberries", "Redberries", "produce", 0xC02040, 3, 2);
        simple(s, "dwellberries", "Dwellberries", "produce", 0x3A5AA0, 4);
        food(s, "cooking_apple", "Cooking apple", "produce", 0x9AB040, 3, 2);
        food(s, "banana", "Banana", "produce", 0xE0D040, 2, 2);
        food(s, "orange", "Orange", "produce", 0xE09030, 2, 2);
        simple(s, "curry_leaf", "Curry leaf", "herb", 0x6A8A2A, 5);
        food(s, "pineapple", "Pineapple", "produce", 0xE0C040, 5, 2);
        food(s, "papaya_fruit", "Papaya fruit", "produce", 0xE08030, 8, 8);
        simple(s, "coconut", "Coconut", "produce", 0x8A6A3A, 10);
        food(s, "dragonfruit", "Dragonfruit", "produce", 0xE04080, 50, 10);
        simple(s, "calquat_fruit", "Calquat fruit", "produce", 0xD06030, 10);
        simple(s, "barley", "Barley", "produce", 0xD0B060, 3);
        simple(s, "hammerstone_hops", "Hammerstone hops", "produce", 0x7A9A4A, 3);
        simple(s, "asgarnian_hops", "Asgarnian hops", "produce", 0x6A8A4A, 4);
        simple(s, "jute_fibre", "Jute fibre", "flax", 0x9A8A5A, 4);
        simple(s, "yanillian_hops", "Yanillian hops", "produce", 0x7AAA5A, 5);
        simple(s, "krandorian_hops", "Krandorian hops", "produce", 0x5A8A3A, 6);
        simple(s, "wildblood_hops", "Wildblood hops", "produce", 0xA03A3A, 8);
        simple(s, "mushroom", "Bittercap mushroom", "produce", 0xC0A080, 8);
        simple(s, "cave_nightshade_flower", "Belladonna flower", "herb", 0x4A2A6A, 30);
    }

    private static String seedName(Crop c) {
        return switch (c.id()) {
            case "oak" -> "Acorn";
            case "seaweed" -> "Seaweed spore";
            case "bittercap" -> "Mushroom spore";
            case "redwood" -> "Redwood tree seed";
            case "calquat" -> "Calquat tree seed";
            case "grape" -> "Grape seed";
            default -> c.patch() == Patch.FRUIT_TREE && !c.id().equals("pineapple") ? title(c.id()) + " tree seed" : title(c.seed());
        };
    }

    @Override
    public void recipes(Consumer<Recipe> out) {
        // Belladonna flowers are ground into cave nightshade for weapon poison(++).
        out.accept(r("grind_belladonna", MenuType.HERBLORE, "Cave nightshade", Skill.HERBLORE, 1, 0)
                .in("cave_nightshade_flower", 1).out("nightshade", 1).tool("pestle_and_mortar").ticks(1).build());
    }

    @Override
    public List<BlockDef> blocks() {
        List<BlockDef> list = new ArrayList<>();
        for (Patch p : Patch.values()) {
            list.add(BlockDef.scripted(p.blockId, p.displayName, 0x5A3A1A, "farming_patch"));
        }
        list.add(BlockDef.scripted("compost_bin", "Compost bin", 0x6A4A2A, "crate"));
        list.add(BlockDef.scripted("birdhouse_space", "Space (bird house)", 0x7A6A4A, "stump"));
        list.add(BlockDef.scripted("birdhouse", "Bird house", 0x8A6A3A, "birdhouse"));
        return list;
    }

    @Override
    public List<PlaceDef> places() {
        return List.of(
                // Real patch locations (OSRS coordinates).
                PlaceDef.of("lumbridge_tree_patch", "Lumbridge tree patch", 3193, 3231, PlaceDef.Style.CAMP,
                        List.of("tree_patch", "compost_bin")),
                PlaceDef.of("varrock_tree_patch", "Varrock tree patch", 3229, 3459, PlaceDef.Style.CAMP, List.of("tree_patch")),
                PlaceDef.of("falador_tree_patch", "Falador tree patch", 3004, 3373, PlaceDef.Style.CAMP, List.of("tree_patch", "compost_bin")),
                PlaceDef.of("taverley_tree_patch", "Taverley tree patch", 2936, 3438, PlaceDef.Style.CAMP, List.of("tree_patch")),
                PlaceDef.of("gnome_stronghold_patches", "Gnome Stronghold patches", 2436, 3415, PlaceDef.Style.CAMP,
                        List.of("tree_patch", "fruit_tree_patch", "spirit_tree_patch")),
                PlaceDef.of("catherby_fruit_patch", "Catherby fruit tree patch", 2860, 3433, PlaceDef.Style.CAMP,
                        List.of("fruit_tree_patch", "compost_bin")),
                PlaceDef.of("tree_gnome_village_patch", "Tree Gnome Village fruit patch", 2490, 3179, PlaceDef.Style.CAMP,
                        List.of("fruit_tree_patch")),
                PlaceDef.of("brimhaven_patches", "Brimhaven patches", 2764, 3212, PlaceDef.Style.CAMP,
                        List.of("fruit_tree_patch", "spirit_tree_patch")),
                PlaceDef.of("lletya_fruit_patch", "Lletya fruit tree patch", 2346, 3162, PlaceDef.Style.CAMP, List.of("fruit_tree_patch")),
                PlaceDef.of("champions_guild_bush", "Champions' Guild bush patch", 3181, 3357, PlaceDef.Style.CAMP, List.of("bush_patch")),
                PlaceDef.of("rimmington_bush", "Rimmington bush patch", 2940, 3221, PlaceDef.Style.CAMP, List.of("bush_patch", "compost_bin")),
                PlaceDef.of("ardougne_bush", "Ardougne bush patch", 2617, 3225, PlaceDef.Style.CAMP, List.of("bush_patch")),
                PlaceDef.of("etceteria_patches", "Etceteria patches", 2591, 3863, PlaceDef.Style.CAMP,
                        List.of("bush_patch", "spirit_tree_patch")),
                PlaceDef.of("lumbridge_hops", "Lumbridge hops patch", 3229, 3315, PlaceDef.Style.CAMP, List.of("hops_patch")),
                PlaceDef.of("seers_hops", "Seers' Village hops patch", 2666, 3523, PlaceDef.Style.CAMP, List.of("hops_patch")),
                PlaceDef.of("yanille_hops", "Yanille hops patch", 2576, 3103, PlaceDef.Style.CAMP, List.of("hops_patch")),
                PlaceDef.of("entrana_hops", "Entrana hops patch", 2810, 3336, PlaceDef.Style.CAMP, List.of("hops_patch")),
                PlaceDef.of("tai_bwo_wannai_calquat", "Tai Bwo Wannai calquat patch", 2796, 3101, PlaceDef.Style.CAMP,
                        List.of("calquat_patch")),
                PlaceDef.of("farming_guild", "Farming Guild", 1249, 3726, PlaceDef.Style.TOWN,
                        List.of("tree_patch", "fruit_tree_patch", "bush_patch", "cactus_patch", "celastrus_patch", "redwood_patch",
                                "spirit_tree_patch", "compost_bin")),
                PlaceDef.of("fossil_island_hardwood", "Fossil Island hardwood patches", 3715, 3835, PlaceDef.Style.CAMP,
                        List.of("hardwood_patch", "hardwood_patch", "hardwood_patch")),
                PlaceDef.of("fossil_island_verdant_valley", "Verdant Valley bird houses", 3764, 3756, PlaceDef.Style.CAMP,
                        List.of("birdhouse_space", "birdhouse_space")),
                PlaceDef.of("fossil_island_meadow", "Fossil Island meadow bird houses", 3677, 3882, PlaceDef.Style.CAMP,
                        List.of("birdhouse_space", "birdhouse_space")),
                PlaceDef.of("underwater_seaweed", "Seaweed patches", 3740, 3900, PlaceDef.Style.CAMP,
                        List.of("seaweed_patch", "seaweed_patch")),
                PlaceDef.of("canifis_mushroom", "Canifis mushroom patch", 3451, 3473, PlaceDef.Style.CAMP, List.of("mushroom_patch")),
                PlaceDef.of("draynor_belladonna", "Draynor Manor belladonna patch", 3087, 3355, PlaceDef.Style.CAMP,
                        List.of("belladonna_patch")),
                PlaceDef.of("al_kharid_cactus", "Al Kharid cactus patch", 3315, 3202, PlaceDef.Style.CAMP, List.of("cactus_patch")),
                PlaceDef.of("hosidius_vinery", "Hosidius vinery", 1806, 3554, PlaceDef.Style.CAMP,
                        List.of("grape_patch", "grape_patch", "grape_patch")));
    }
}
