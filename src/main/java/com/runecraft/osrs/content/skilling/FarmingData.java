package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.FarmingDisease;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.Station;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Farming: tree, fruit tree, bush, hops, hardwood, calquat, celastrus, redwood, spirit tree, cactus,
 * seaweed, mushroom, belladonna, grape, flower, hespori, anima and crystal tree patches; compost bins (compost, supercompost,
 * ultracompost); tree seeds grown into saplings in plant pots; and bird house spaces. Levels, XP
 * and growth times are OSRS values (growth is real-time, measured in world time).
 */
public final class FarmingData extends SkillingPart {

    /**
     * Patch types with their growth cycle length (minutes), whether crops in them can get diseased
     * (the chance per crop is {@link #diseaseBase}) and whether a diseased crop is pruned with
     * secateurs (trees, fruit trees, bushes) instead of treated with plant cure.
     */
    public enum Patch {
        TREE("tree_patch", "Tree patch", 40, true, true),
        FRUIT_TREE("fruit_tree_patch", "Fruit tree patch", 160, true, true),
        BUSH("bush_patch", "Bush patch", 20, true, true),
        HOPS("hops_patch", "Hops patch", 10, true, false),
        HARDWOOD("hardwood_patch", "Hardwood tree patch", 640, true, false),
        CALQUAT("calquat_patch", "Calquat patch", 160, true, true),
        CELASTRUS("celastrus_patch", "Celastrus patch", 160, true, false),
        REDWOOD("redwood_patch", "Redwood tree patch", 640, true, true),
        SPIRIT("spirit_tree_patch", "Spirit tree patch", 320, true, true),
        CACTUS("cactus_patch", "Cactus patch", 80, true, false),
        SEAWEED("seaweed_patch", "Seaweed patch", 10, true, false),
        MUSHROOM("mushroom_patch", "Mushroom patch", 40, true, false),
        BELLADONNA("belladonna_patch", "Belladonna patch", 80, true, false),
        GRAPE("grape_patch", "Grapevine patch", 5, false, false),          // grapevines never get diseased
        FLOWER("flower_patch", "Flower patch", 5, true, false),
        HESPORI("hespori_patch", "Hespori patch", 640, false, false),      // Hespori can't get diseased
        ANIMA("anima_patch", "Anima patch", 640, false, false),            // anima plants are immune to disease
        CRYSTAL("crystal_tree_patch", "Crystal tree patch", 80, false, false), // crystal trees are immune to disease
        /** Coral nursery (the Coral Nurseries off the Great Conch): frags grow in 4 x 40-minute cycles, no compost. */
        CORAL("coral_nursery_patch", "Coral nursery", 40, true, false);

        public final String blockId;
        public final String displayName;
        public final int cycleMinutes;
        public final boolean diseases;
        public final boolean pruned;

        Patch(String blockId, String displayName, int cycleMinutes, boolean diseases, boolean pruned) {
            this.blockId = blockId;
            this.displayName = displayName;
            this.cycleMinutes = cycleMinutes;
            this.diseases = diseases;
            this.pruned = pruned;
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

    /** Length of one growth cycle of a crop in minutes (potato cacti grow in 10-minute cycles). */
    public static int cycleMinutes(Crop c) {
        return c.id().equals("potato_cactus") ? 10 : c.patch().cycleMinutes;
    }

    /** Number of growth cycles of a crop (e.g. oak 5, magic 12, fruit trees 6, redberry 5). */
    public static int stages(Crop c) {
        return Math.max(1, c.growthMinutes() / cycleMinutes(c));
    }

    /**
     * Disease chance per growth cycle out of 128 before compost: fruit trees 18 and herbs 27 (wiki),
     * trees by tree ({@link FarmingDisease#treeChance}), every other crop estimated from its number of
     * growth cycles ({@link FarmingDisease#estimatedBase}, approx.). 0 = immune (poison ivy, grapevines,
     * Hespori, anima plants, crystal trees).
     */
    public static int diseaseBase(Crop c) {
        if (!c.patch().diseases || c.id().equals("poison_ivy") || c.harvest() == Harvest.SCARECROW) {
            return 0;
        }
        return switch (c.patch()) {
            case FRUIT_TREE -> FarmingDisease.FRUIT_TREE_CHANCE;
            case TREE -> FarmingDisease.treeChance(c.id());
            default -> FarmingDisease.estimatedBase(stages(c) - 1);
        };
    }

    /** Disease chance per growth cycle out of 128 with the given compost level (0..3); 0 = disease-free. */
    public static int diseaseChance(Crop c, int compost) {
        return diseaseChance(c, compost, false);
    }

    /** As {@link #diseaseChance(Crop, int)}, with a living iasor anima plant if {@code iasor}. */
    public static int diseaseChance(Crop c, int compost, boolean iasor) {
        return FarmingDisease.chance(diseaseBase(c), compost, iasor);
    }

    /** Anima plants: the crop id of each and its effect. */
    public static final List<String> ANIMA = List.of("kronos", "iasor", "attas");

    /** Seeds defined by another module (anima seeds drop from Hespori, see the bosses module). */
    private static final List<String> EXTERNAL_SEEDS = List.of("kronos_seed", "iasor_seed", "attas_seed",
            // Sailing seeds and coral frags (salvage, sea creatures; items defined in SailingData).
            "flax_seed", "hemp_seed", "cotton_seed", "camphor_seed", "ironwood_seed", "rosewood_seed", "elkhorn_frag", "pillar_frag",
            "umbral_frag");

    /** Hardwood trees grown from Sailing seeds: their saplings are made here (the seeds are SailingData's). */
    public static final List<String> SAILING_TREES = List.of("camphor", "ironwood", "rosewood");

    /**
     * Gardeners who look after the patches next to them for a payment (OSRS names; the Farming
     * Guild's patches are all handled by one gardener here).
     */
    public record Gardener(String id, String name, String place, int dx, int dz) {
    }

    public static final List<Gardener> GARDENERS = List.of(
            new Gardener("gardener_fayeth", "Fayeth", "lumbridge_tree_patch", 2, 2),
            new Gardener("gardener_treznor", "Treznor", "varrock_tree_patch", 2, 2),
            new Gardener("gardener_heskel", "Heskel", "falador_tree_patch", 2, 2),
            new Gardener("gardener_alain", "Alain", "taverley_tree_patch", 2, 2),
            new Gardener("gardener_prissy_scilla", "Prissy Scilla", "gnome_stronghold_patches", 2, 2),
            new Gardener("gardener_bolongo", "Bolongo", "gnome_stronghold_fruit_patch", 2, 2),
            new Gardener("gardener_frizzy_skernip", "Frizzy Skernip", "port_sarim_spirit_patch", 2, 2),
            new Gardener("gardener_ellena", "Ellena", "catherby_fruit_patch", 2, 2),
            new Gardener("gardener_gileth", "Gileth", "tree_gnome_village_patch", 2, 2),
            new Gardener("gardener_garth", "Garth", "brimhaven_patches", 2, 2),
            new Gardener("gardener_liliwen", "Liliwen", "lletya_fruit_patch", 2, 2),
            new Gardener("gardener_dreven", "Dreven", "champions_guild_bush", 2, 2),
            new Gardener("gardener_taria", "Taria", "rimmington_bush", 2, 2),
            new Gardener("gardener_torrell", "Torrell", "ardougne_bush", 2, 2),
            new Gardener("gardener_rhazien", "Rhazien", "etceteria_patches", 2, 2),
            new Gardener("gardener_vasquen", "Vasquen", "lumbridge_hops", 2, 2),
            new Gardener("gardener_rhonen", "Rhonen", "seers_hops", 2, 2),
            new Gardener("gardener_selena", "Selena", "yanille_hops", 2, 2),
            new Gardener("gardener_francis", "Francis", "entrana_hops", 2, 2),
            new Gardener("gardener_imiago", "Imiago", "tai_bwo_wannai_calquat", 2, 2),
            new Gardener("gardener_rosie", "Rosie", "farming_guild", 3, 3),
            new Gardener("gardener_squirrel", "Squirrel", "fossil_island_hardwood", 2, 2),
            // Allotment gardeners at the real farming patches (OSRS coordinates).
            new Gardener("gardener_elstan", "Elstan", "falador_farm", 0, 0),
            new Gardener("gardener_dantaera", "Dantaera", "catherby_farm", 0, 0),
            new Gardener("gardener_kragen", "Kragen", "ardougne_farm", 2, 2),
            new Gardener("gardener_lyra", "Lyra", "morytania_farm", 2, 2),
            new Gardener("gardener_marisi", "Marisi", "hosidius_farm", 0, 0),
            new Gardener("gardener_oswallt", "Oswallt", "prifddinas_farm", -3, 2),
            // Varlamore (OSRS Wiki gardener pages): Harminia at Ortus Farm, Ehecatl at Kastori's fruit tree patch, Ercos at
            // Aldarin's hops patch, Marcellus at the Locus Oasis hardwood patch; Chet in the Coral Nurseries.
            new Gardener("gardener_harminia", "Harminia", "civitas_farm", -2, 1),
            new Gardener("gardener_ehecatl", "Ehecatl", "kastori_patches", 2, 2),
            new Gardener("gardener_ercos", "Ercos", "aldarin_hops", 2, -3),
            new Gardener("gardener_marcellus", "Marcellus", "locus_oasis_hardwood", 2, 2),
            new Gardener("gardener_chet", "Chet", "coral_nurseries", 2, 2));

    /**
     * Tool leprechauns stand by the allotment patches: talking to one notes the harvested produce in
     * the player's inventory, as in OSRS.
     */
    public record Leprechaun(String id, String place, int dx, int dz) {
    }

    public static final List<Leprechaun> LEPRECHAUNS = List.of(
            new Leprechaun("tool_leprechaun_falador", "falador_farm", 3, -2),
            new Leprechaun("tool_leprechaun_catherby", "catherby_farm", 3, -2),
            new Leprechaun("tool_leprechaun_ardougne", "ardougne_farm", -2, 2),
            new Leprechaun("tool_leprechaun_morytania", "morytania_farm", -2, 2),
            new Leprechaun("tool_leprechaun_hosidius", "hosidius_farm", 3, -2),
            new Leprechaun("tool_leprechaun_farming_guild", "farming_guild", 4, -3),
            new Leprechaun("tool_leprechaun_harmony", "harmony_herb_patch", 2, 2),
            new Leprechaun("tool_leprechaun_weiss", "weiss_herb_patch", 2, 2),
            new Leprechaun("tool_leprechaun_prifddinas", "prifddinas_farm", 3, 2),
            new Leprechaun("tool_leprechaun_civitas", "civitas_farm", 3, -2),
            new Leprechaun("tool_leprechaun_coral_nurseries", "coral_nurseries", -2, 2));

    /** Whether a tool leprechaun will note this item: harvested farming produce (not seeds, saplings or tools). */
    public static boolean leprechaunNotes(String item) {
        if (item == null || item.isEmpty()) {
            return false;
        }
        for (com.runecraft.osrs.data.Herb h : com.runecraft.osrs.data.Herb.values()) {
            if (h.grimyItem().equals(item) || h.cleanItem().equals(item)) {
                return true;
            }
        }
        for (com.runecraft.osrs.data.Allotment a : com.runecraft.osrs.data.Allotment.values()) {
            if (a.produceItem().equals(item)) {
                return true;
            }
        }
        for (Crop c : CROPS) {
            if (c.harvest() != Harvest.CHOP && c.produce() != null && c.produce().equals(item)) {
                return true;
            }
        }
        return false;
    }

    public static Gardener gardener(String id) {
        for (Gardener g : GARDENERS) {
            if (g.id().equals(id)) {
                return g;
            }
        }
        return null;
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
        SPIRIT,
        /** Check health for XP, then harvest min..max produce at once and clear the patch (crystal tree). */
        CHECK_ALL,
        /** Fully grown Hespori: disturbing it starts the fight; killing it pays the "check" XP. */
        BOSS,
        /** Anima plant: its effect lasts from planting until it withers at the end of its growth. */
        ANIMA,
        /** A scarecrow placed in a flower patch (protects sweetcorn next to it). */
        SCARECROW
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
                1, produce, 6, 6, 960, 45, null, color));
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

    private static void flower(String id, String name, String produce, int level, double plant, double harvest, int yield,
                               int color) {
        CROPS.add(new Crop(id, name, Patch.FLOWER, Harvest.ALL, level, plant, 0, harvest, id + "_seed", id + "_seed", 1, produce,
                yield, yield, 20, 0, null, color));
    }

    /**
     * Harvest lives save chances (out of 256, before the +1) at Farming level 1 and 99, for the
     * {@link Harvest#ALL} and picked crops that use harvest lives, or null. Hops are fitted to the
     * wiki's level-requirement and level-99 chances (barley 40.6% at 3, hammerstone 41.4% at 4,
     * Asgarnian 44.1% at 8, jute 47.7% at 13, Yanillian 49.2% at 16, wildblood 55.5% at 28, all 70.7%
     * at 99); Krandorian, seaweed, grapes, cactus and celastrus are approx.
     */
    public static int[] harvestChance(Crop c) {
        return switch (c.id()) {
            case "barley" -> new int[]{102, 180};
            case "hammerstone" -> new int[]{103, 180};
            case "asgarnian" -> new int[]{107, 180};
            case "jute" -> new int[]{113, 180};
            case "yanillian" -> new int[]{116, 180};
            case "krandorian" -> new int[]{121, 180}; // approx
            case "wildblood" -> new int[]{127, 180};
            case "seaweed" -> new int[]{149, 208};    // approx
            case "grape" -> new int[]{139, 206};      // approx
            case "cactus" -> new int[]{-78, 178};     // approx
            case "celastrus" -> new int[]{-27, 63};   // approx
            // Sailing hops-patch crops (wiki Farming yield calculator low/high).
            case "flax" -> new int[]{140, 194};
            case "hemp" -> new int[]{120, 178};
            case "cotton" -> new int[]{82, 142};
            // Coral: at least 4, "a decent chance of over 16 at 99" (wiki; chance values approx).
            case "elkhorn", "pillar", "umbral" -> new int[]{140, 200}; // approx
            default -> null;
        };
    }

    /** Whether magic secateurs raise this crop's harvest lives save chance (herbs, allotments, hops and grapes). */
    public static boolean secateursBoost(Crop c) {
        return c.patch() == Patch.HOPS || c.patch() == Patch.GRAPE;
    }

    /** Coral nurseries always give at least four coral (OSRS Wiki, Coral nursery). */
    public static final int CORAL_MIN_YIELD = 4;

    /** Most belladonna a harvest can give: 6 at level 63, rising to 18 at 99 (OSRS; the curve in between approx.). */
    public static int belladonnaMax(int level) {
        return 6 + Math.max(0, Math.min(99, level) - 63) / 3;
    }

    public static int harvestYield(Crop c, int level, int compost, java.util.function.IntUnaryOperator random) {
        return harvestYield(c, level, compost, 0, random);
    }

    /**
     * Produce from harvesting a whole patch (Harvest.ALL crops). Flowers give a fixed amount (woad 3
     * leaves; limpwurt 3 roots plus 0..floor((level - 1) / 10) more) and ignore compost. Hops, seaweed
     * and grapes use harvest lives (3 plus compost; {@code bonus} is the save-chance item bonus, e.g.
     * magic secateurs 0.10 and attas 0.05). Bittercap mushrooms give 6 (more only with attas),
     * belladonna 6 to {@link #belladonnaMax}; other crops min..max plus one per compost level.
     * {@code random.applyAsInt(n)} must return 0..n-1.
     */
    public static int harvestYield(Crop c, int level, int compost, double bonus, java.util.function.IntUnaryOperator random) {
        if (c.patch() == Patch.FLOWER) {
            if (c.id().equals("limpwurt")) {
                return 3 + random.applyAsInt(Math.max(0, Math.min(98, level) - 1) / 10 + 1);
            }
            return c.minYield();
        }
        if (c.patch() == Patch.BELLADONNA) {
            return 6 + random.applyAsInt(belladonnaMax(level) - 6 + 1);
        }
        if (c.patch() == Patch.MUSHROOM) {
            return bonus > 0 ? com.runecraft.osrs.FarmingYield.harvest(6,
                    com.runecraft.osrs.FarmingYield.saveChance(0, 0, level, bonus, 0), random) : 6;
        }
        int[] lives = harvestChance(c);
        if (lives != null && c.patch() == Patch.CORAL) {
            // Coral can't be composted: three lives plus one guaranteed coral (minimum 4).
            int chance = com.runecraft.osrs.FarmingYield.saveChance(lives[0], lives[1], level, bonus, 0);
            return CORAL_MIN_YIELD - com.runecraft.osrs.FarmingYield.lives(0)
                    + com.runecraft.osrs.FarmingYield.harvest(com.runecraft.osrs.FarmingYield.lives(0), chance, random);
        }
        if (lives != null) {
            int chance = com.runecraft.osrs.FarmingYield.saveChance(lives[0], lives[1], level, bonus, 0);
            return com.runecraft.osrs.FarmingYield.harvest(com.runecraft.osrs.FarmingYield.lives(compost), chance, random);
        }
        return c.minYield() + random.applyAsInt(c.maxYield() - c.minYield() + 1) + Math.max(0, compost);
    }

    /** Farming XP for a harvest of {@code yield} produce: flowers and belladonna pay their harvest XP once per harvest. */
    public static double harvestXp(Crop c, int yield) {
        return c.patch() == Patch.FLOWER || c.patch() == Patch.BELLADONNA ? c.harvestXp() : c.harvestXp() * yield;
    }

    private static String seedOf(String id) {
        return switch (id) {
            case "oak" -> "acorn";
            case "crystal" -> "crystal_acorn";
            default -> id + "_seed";
        };
    }

    static {
        tree("oak", "Oak tree", "OAK", 15, 14.0, 467.3, 160, "oak_roots", 0x8A6A3A);
        tree("willow", "Willow tree", "WILLOW", 30, 25.0, 1456.5, 240, "willow_roots", 0x6E7A3A);
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
                "spirit_sapling", 1, null, 0, 0, 3840, 0, null, 0x7A9A5A));
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
        // Sailing crops in hops patches (OSRS Wiki, 3 seeds each): flax 18 (3 x 20 min), hemp 37 (4 x 20), cotton 71 (5 x 20).
        hops("flax", "Flax", "flax", 18, 16.0, 17.5, 60, 3, 0x8A9AC0);
        hops("hemp", "Hemp", "hemp", 37, 33.0, 37.0, 80, 3, 0x7A8A3A);
        hops("cotton", "Cotton", "cotton_boll", 71, 72.0, 82.0, 100, 3, 0xE0E0D0);
        // Sailing hardwood trees (OSRS Wiki): camphor 66 (8 x 640 min), ironwood 80 (8 x 640), rosewood 92 (9 x 640);
        // chopped like their wild trees (GatheringData.TREES block ids).
        CROPS.add(new Crop("camphor", "Camphor tree", Patch.HARDWOOD, Harvest.CHOP, 66, 88.0, 17840.0, 0, "camphor_seed",
                "camphor_sapling", 1, "camphor_tree", 0, 0, 5120, 0, null, 0x8A6A4A));
        CROPS.add(new Crop("ironwood", "Ironwood tree", Patch.HARDWOOD, Harvest.CHOP, 80, 145.0, 20380.0, 0, "ironwood_seed",
                "ironwood_sapling", 1, "ironwood_tree", 0, 0, 5120, 0, null, 0x5A4A40));
        CROPS.add(new Crop("rosewood", "Rosewood tree", Patch.HARDWOOD, Harvest.CHOP, 92, 252.0, 23100.0, 0, "rosewood_seed",
                "rosewood_sapling", 1, "rosewood_tree", 0, 0, 5760, 0, null, 0x9A4A4A));
        // Coral frags placed in a coral nursery (OSRS Wiki): 4 x 40 minutes, at least 4 coral, harvest XP per coral.
        CROPS.add(new Crop("elkhorn", "Elkhorn coral", Patch.CORAL, Harvest.ALL, 28, 20.5, 0, 24.0, "elkhorn_frag", "elkhorn_frag",
                1, "elkhorn_coral", 4, 4, 160, 0, null, 0xD0C0A0));
        CROPS.add(new Crop("pillar", "Pillar coral", Patch.CORAL, Harvest.ALL, 52, 52.0, 0, 60.0, "pillar_frag", "pillar_frag",
                1, "pillar_coral", 4, 4, 160, 0, null, 0xE0A0B0));
        CROPS.add(new Crop("umbral", "Umbral coral", Patch.CORAL, Harvest.ALL, 77, 136.0, 0, 159.0, "umbral_frag", "umbral_frag",
                1, "umbral_coral", 4, 4, 160, 0, null, 0x5A3A7A));
        CROPS.add(new Crop("cactus", "Cactus", Patch.CACTUS, Harvest.PICK, 55, 66.5, 374.0, 25.0, "cactus_seed", "cactus_seed", 1,
                "cactus_spine", 3, 3, 560, 20, null, 0x5A8A3A));
        CROPS.add(new Crop("potato_cactus", "Potato cactus", Patch.CACTUS, Harvest.PICK, 64, 68.0, 230.0, 68.0, "potato_cactus_seed",
                "potato_cactus_seed", 1, "potato_cactus", 6, 6, 70, 5, null, 0x8AA040));
        // Seaweed spores grow into giant seaweed (OSRS Wiki: 23 Farming, 19 XP to plant, 21 per giant seaweed).
        CROPS.add(new Crop("seaweed", "Giant seaweed", Patch.SEAWEED, Harvest.ALL, 23, 19.0, 0, 21.0, "seaweed_spore",
                "seaweed_spore", 1, "giant_seaweed", 3, 7, 40, 0, null, 0x3A6A2A));
        CROPS.add(new Crop("bittercap", "Bittercap mushrooms", Patch.MUSHROOM, Harvest.ALL, 53, 61.5, 0, 57.7, "mushroom_spore",
                "mushroom_spore", 1, "mushroom", 6, 6, 240, 0, null, 0xC0A080));
        // Belladonna gives cave nightshade: 6 to 18 by level, 512 XP per harvest.
        CROPS.add(new Crop("belladonna", "Belladonna", Patch.BELLADONNA, Harvest.ALL, 63, 91.0, 0, 512.0, "belladonna_seed",
                "belladonna_seed", 1, "nightshade", 6, 18, 320, 0, null, 0x4A2A6A));
        CROPS.add(new Crop("grape", "Grapevine", Patch.GRAPE, Harvest.ALL, 36, 40.0, 0, 40.0, "grape_seed", "grape_seed", 1,
                "grapes", 3, 5, 35, 0, null, 0x6A2A8A));
        // Flowers: 4 x 5-minute cycles; harvest XP is per harvest, not per item (woad 3 leaves, limpwurt 3+).
        flower("marigold", "Marigold", "marigolds", 2, 8.5, 47.0, 1, 0xF0A020);
        flower("rosemary", "Rosemary", "rosemary", 11, 12.0, 66.5, 1, 0x6A9A6A);
        flower("nasturtium", "Nasturtium", "nasturtiums", 24, 19.5, 111.0, 1, 0xE05020);
        flower("woad", "Woad plant", "woad_leaf", 25, 20.5, 115.5, 3, 0x3A5AB0);
        flower("limpwurt", "Limpwurt plant", "limpwurt_root", 26, 21.5, 120.0, 3, 0xC09050);
        flower("white_lily", "White lily", "white_lily", 58, 42.0, 250.0, 1, 0xF0F0F0);
        // A scarecrow stands in a flower patch to keep birds off the sweetcorn next to it (made at Farming 23).
        CROPS.add(new Crop("scarecrow", "Scarecrow", Patch.FLOWER, Harvest.SCARECROW, 23, 0, 0, 0, "scarecrow", "scarecrow", 1,
                null, 0, 0, 5, 0, null, 0xC0A060));
        // Hespori: 3 x 640-minute cycles in the Farming Guild; killing it pays 12,662 Farming XP.
        CROPS.add(new Crop("hespori", "Hespori", Patch.HESPORI, Harvest.BOSS, 65, 62.0, 12662.0, 0, "hespori_seed", "hespori_seed",
                1, null, 0, 0, 1920, 0, null, 0x8A3A6A));
        // Anima plants (76 Farming, 100 XP for planting): their effect lasts their whole 8 x 640-minute life.
        CROPS.add(new Crop("kronos", "Kronos", Patch.ANIMA, Harvest.ANIMA, 76, 100.0, 0, 0, "kronos_seed", "kronos_seed", 1, null,
                0, 0, 5120, 0, null, 0x8A4A2A));
        CROPS.add(new Crop("iasor", "Iasor", Patch.ANIMA, Harvest.ANIMA, 76, 100.0, 0, 0, "iasor_seed", "iasor_seed", 1, null,
                0, 0, 5120, 0, null, 0xC04A8A));
        CROPS.add(new Crop("attas", "Attas", Patch.ANIMA, Harvest.ANIMA, 76, 100.0, 0, 0, "attas_seed", "attas_seed", 1, null,
                0, 0, 5120, 0, null, 0x4A8AC0));
        // Crystal tree (Prifddinas, 74 Farming): 6 x 80 minutes; checked for 13,240 XP, then cut for crystal shards (8-10 approx).
        CROPS.add(new Crop("crystal", "Crystal tree", Patch.CRYSTAL, Harvest.CHECK_ALL, 74, 126.0, 13240.0, 0, "crystal_acorn",
                "crystal_sapling", 1, "crystal_shard", 8, 10, 480, 0, null, 0x9AD8F0));
    }

    /** Whether a crop needs no tool to be put in its patch (a scarecrow is simply stood in it). */
    public static boolean noPlantingTool(Crop c) {
        return c.harvest() == Harvest.SCARECROW || c.patch() == Patch.CORAL;
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
            "asgarnian_hops", "jute_fibre", "yanillian_hops", "krandorian_hops", "wildblood_hops", "weeds", "seaweed", "giant_seaweed",
            "mushroom", "minecraft:wheat", "minecraft:carrot", "minecraft:beetroot", "minecraft:apple");

    @Override
    public void items(ItemSink s) {
        tool(s, "spade", "Spade", "spade", 0x8A8A8A, 3);
        tool(s, "rake", "Rake", "rake", 0x8A6A3A, 6);
        tool(s, "secateurs", "Secateurs", "secateurs", 0x9A9AA0, 5);
        tool(s, "gardening_trowel", "Gardening trowel", "trowel", 0x8A8A8A, 5);
        simple(s, "weeds", "Weeds", "produce", 0x5A7A2A, 1);
        simple(s, "plant_cure", "Plant cure", "potion", 0x8AB060, 25)
                .examine("Use this on a diseased plant to cure it.");
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
            if (EXTERNAL_SEEDS.contains(c.seed()) || c.harvest() == Harvest.SCARECROW) {
                continue;
            }
            if (!c.seed().equals(c.planted())) {
                s.add(c.seed(), seedName(c), Kind.SEED, "seed", c.color()).value(Math.max(5, c.level() * 4));
                simple(s, c.planted(), title(c.id()) + " sapling", "sapling", c.color(), Math.max(10, c.level() * 6));
            } else {
                s.add(c.seed(), seedName(c), Kind.SEED, "seed", c.color()).value(Math.max(2, c.level() * 2));
            }
        }
        for (String t : SAILING_TREES) {
            Crop c = byId(t);
            simple(s, c.planted(), c.name().replace(" tree", "") + " sapling", "sapling", c.color(), c.level() * 6);
        }
        simple(s, "giant_seaweed", "Giant seaweed", "secondary", 0x2A5A2A, 2).examine("Seaweed of large size.");
        simple(s, "hemp", "Hemp", "flax", 0x7A8A3A, 30).examine("I should use this with a spinning wheel.");
        simple(s, "cotton_boll", "Cotton boll", "wool", 0xF0F0E8, 60).examine("I should use this with a spinning wheel.");
        simple(s, "linen_yarn", "Linen yarn", "string", 0xD8D0B8, 20).examine("Spun from flax, ready for the loom.");
        simple(s, "hemp_yarn", "Hemp yarn", "string", 0x9AA060, 60).examine("Spun from hemp, ready for the loom.");
        simple(s, "cotton_yarn", "Cotton yarn", "string", 0xF4F0E8, 120).examine("Spun from cotton, ready for the loom.");
        simple(s, "elkhorn_coral", "Elkhorn coral", "shard", 0xD0C0A0, 50).examine("A branch of elkhorn coral.");
        simple(s, "pillar_coral", "Pillar coral", "shard", 0xE0A0B0, 150).examine("A column of pillar coral.");
        simple(s, "umbral_coral", "Umbral coral", "shard", 0x5A3A7A, 600).examine("A dark piece of umbral coral.");
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
        simple(s, "marigolds", "Marigolds", "produce", 0xF0A020, 1).examine("A bunch of marigolds.");
        simple(s, "rosemary", "Rosemary", "herb", 0x6A9A6A, 1).examine("A sprig of rosemary.");
        simple(s, "nasturtiums", "Nasturtiums", "produce", 0xE05020, 1).examine("A bunch of nasturtiums.");
        simple(s, "woad_leaf", "Woad leaf", "herb", 0x3A5AB0, 10).examine("Slightly bluish leaves.");
        simple(s, "white_lily", "White lily", "produce", 0xF0F0F0, 10).examine("A beautiful white lily.");
        simple(s, "seed_pack", "Seed pack", "package", 0x8A6A3A, 1)
                .examine("A pack of seeds, rewarded for completing a farming contract.");
        // Protection payments and the scarecrow.
        simple(s, "barley_malt", "Barley malt", "produce", 0xC8A050, 5).examine("Barley that has been cooked on a range.");
        simple(s, "monkey_nuts", "Monkey nuts", "produce", 0xB08A50, 4).examine("Nuts that monkeys love.");
        food(s, "monkey_bar", "Monkey bar", "cake", 0x9A7040, 5, 5);
        simple(s, "ground_tooth", "Ground tooth", "spice", 0xE8E0D0, 10).examine("A ground-up suqah tooth.");
        simple(s, "empty_sack", "Empty sack", "package", 0xB09A6A, 2).examine("An empty sack, for hay or produce.");
        simple(s, "hay_sack", "Hay sack", "package", 0xD0B060, 3).examine("A sack full of hay.");
        simple(s, "scarecrow", "Scarecrow", "stake", 0xC0A060, 10)
                .examine("Stand it in a flower patch to keep birds off the sweetcorn next to it.");
    }

    private static String seedName(Crop c) {
        return switch (c.id()) {
            case "oak" -> "Acorn";
            case "crystal" -> "Crystal acorn";
            case "hespori" -> "Hespori seed";
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
        // Payments: barley malt is barley cooked on a range (no XP); the ground suqah tooth for the spirit
        // tree's fee; a monkey bar (approx. recipe: banana, monkey nuts and a bucket of milk).
        out.accept(r("cook_barley_malt", MenuType.COOKING, "Barley malt", Skill.COOKING, 1, 0).in("barley", 1)
                .out("barley_malt", 1).ticks(2).build());
        out.accept(r("make_monkey_bar", MenuType.COOKING, "Monkey bar", Skill.COOKING, 1, 0).in("banana", 1).in("monkey_nuts", 1)
                .in("minecraft:milk_bucket", 1).out("monkey_bar", 1).out("minecraft:bucket", 1).ticks(2).build());
        out.accept(r("grind_suqah_tooth", MenuType.HERBLORE, "Ground tooth", Skill.HERBLORE, 1, 0)
                .in("suqah_tooth", 1).out("ground_tooth", 1).tool("pestle_and_mortar").ticks(1).build());
        // Giant seaweed burns into 6 soda ash (OSRS Wiki; the fail case below 51 Cooking is not modelled).
        out.accept(r("burn_giant_seaweed", MenuType.COOKING, "Soda ash (giant seaweed)", Skill.COOKING, 1, 0).in("giant_seaweed", 1)
                .out("soda_ash", 6).ticks(3).build());
        // Sailing fibres (OSRS Wiki): spun at a spinning wheel, then two yarn woven into a bolt at a loom (3 ticks each).
        String[][] fibres = {{"flax", "linen_yarn", "12", "16", "bolt_of_linen", "20"}, {"hemp", "hemp_yarn", "39", "60", "bolt_of_canvas", "75"},
                {"cotton_boll", "cotton_yarn", "73", "105", "bolt_of_cotton", "132"}};
        for (String[] f : fibres) {
            int lvl = Integer.parseInt(f[2]);
            out.accept(r("spin_" + f[1], MenuType.SPINNING, title(f[1]), Skill.CRAFTING, lvl, Double.parseDouble(f[3])).in(f[0], 1)
                    .out(f[1], 1).ticks(3).build());
            out.accept(r("weave_" + f[4], MenuType.SPINNING, title(f[4]), Skill.CRAFTING, lvl, Double.parseDouble(f[5])).in(f[1], 2)
                    .out(f[4], 1).ticks(3).build());
        }
        // Plant cure is sold for 25 gp by farming shops and general stores near patches.
        out.accept(plain("shop_general_plant_cure", MenuType.SHOP_GENERAL, "Plant cure - 25 gp").in("coins", 25)
                .out("plant_cure", 1).ticks(0).build());
    }

    @Override
    public List<NpcDef> npcs() {
        List<NpcDef> list = new ArrayList<>();
        for (Gardener g : GARDENERS) {
            list.add(NpcDef.of(g.id(), g.name(), g.place(), g.dx(), g.dz(), 0xD8A880, 0x5A7A3A, 0x4A3A2A, 0x8A6A3A));
        }
        // Solihib runs the food stall in the Ape Atoll market (monkey nuts and monkey bars for the spirit tree fee).
        list.add(NpcDef.of("solihib", "Solihib", "ape_atoll_stalls", 3, -2, 0x6A4A2A, 0xC0A060, 0x8A6A3A, 0xC0A060));
        for (Leprechaun l : LEPRECHAUNS) {
            list.add(NpcDef.of(l.id(), "Tool Leprechaun", l.place(), l.dx(), l.dz(), 0xC8E0A0, 0x2A8A2A, 0x2A6A2A, 0x2A8A2A));
        }
        return list;
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
                        List.of("tree_patch", "fruit_tree_patch")),
                PlaceDef.of("gnome_stronghold_fruit_patch", "Gnome Stronghold fruit tree patch", 2476, 3446, PlaceDef.Style.CAMP,
                        List.of("fruit_tree_patch")),
                PlaceDef.of("catherby_fruit_patch", "Catherby fruit tree patch", 2860, 3433, PlaceDef.Style.CAMP,
                        List.of("fruit_tree_patch", "compost_bin")),
                PlaceDef.of("tree_gnome_village_patch", "Tree Gnome Village fruit patch", 2490, 3179, PlaceDef.Style.CAMP,
                        List.of("fruit_tree_patch")),
                PlaceDef.of("brimhaven_patches", "Brimhaven fruit tree patch", 2764, 3212, PlaceDef.Style.CAMP,
                        List.of("fruit_tree_patch")),
                // OSRS spirit tree patch at 2801,3203 is against the Agility Arena's wall; the camp is set
                // 4 tiles west so its plaza doesn't cut into the arena.
                PlaceDef.of("brimhaven_spirit_patch", "Brimhaven spirit tree patch", 2797, 3203, PlaceDef.Style.CAMP,
                        List.of("spirit_tree_patch")),
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
                new PlaceDef("farming_guild", "Farming Guild", 1249, 3726, PlaceDef.Style.TOWN,
                        List.of(Station.ALLOTMENT_PATCH, Station.ALLOTMENT_PATCH, Station.HERB_PATCH),
                        List.of("flower_patch", "tree_patch", "fruit_tree_patch", "bush_patch", "cactus_patch", "celastrus_patch",
                                "redwood_patch", "spirit_tree_patch", "hespori_patch", "anima_patch", "compost_bin")),
                // Prifddinas: the allotments, flower patch, compost bin and crystal tree patch are part of
                // the city structure (KandarinElves); this marker places the gardener and leprechaun.
                PlaceDef.of("prifddinas_farm", "Prifddinas farming patches", 3292, 6118, PlaceDef.Style.NONE, List.of()),
                // Spirit tree patches (Port Sarim, Hosidius; Etceteria, Brimhaven and the Farming Guild are above).
                PlaceDef.of("port_sarim_spirit_patch", "Port Sarim spirit tree patch", 3059, 3257, PlaceDef.Style.CAMP,
                        List.of("spirit_tree_patch")),
                PlaceDef.of("hosidius_spirit_patch", "Hosidius spirit tree patch", 1693, 3542, PlaceDef.Style.CAMP,
                        List.of("spirit_tree_patch")),
                // Allotment / flower / herb farms. Falador, Catherby and Hosidius are built by their towns'
                // structures (these markers only place the gardener and leprechaun); Ardougne and Port
                // Phasmatys get a small farm here.
                PlaceDef.of("falador_farm", "Falador farming patches", 3053, 3308, PlaceDef.Style.NONE, List.of()),
                PlaceDef.of("catherby_farm", "Catherby farming patches", 2809, 3463, PlaceDef.Style.NONE, List.of()),
                PlaceDef.of("hosidius_farm", "Hosidius farming patches", 1741, 3577, PlaceDef.Style.NONE, List.of()),
                new PlaceDef("ardougne_farm", "Ardougne farming patches", 2666, 3374, PlaceDef.Style.CAMP,
                        List.of(Station.ALLOTMENT_PATCH, Station.ALLOTMENT_PATCH, Station.HERB_PATCH), List.of("flower_patch", "compost_bin")),
                new PlaceDef("morytania_farm", "Port Phasmatys farming patches", 3601, 3525, PlaceDef.Style.CAMP,
                        List.of(Station.ALLOTMENT_PATCH, Station.ALLOTMENT_PATCH, Station.HERB_PATCH), List.of("flower_patch", "compost_bin")),
                // Herb-only patches.
                new PlaceDef("weiss_herb_patch", "Weiss herb patch", 2847, 3931, PlaceDef.Style.CAMP, List.of(Station.HERB_PATCH),
                        List.of()),
                PlaceDef.of("harmony_herb_patch", "Harmony Island herb patch", 3787, 2850, PlaceDef.Style.NONE, List.of()),
                PlaceDef.of("fossil_island_hardwood", "Fossil Island hardwood patches", 3715, 3835, PlaceDef.Style.CAMP,
                        List.of("hardwood_patch", "hardwood_patch", "hardwood_patch")),
                PlaceDef.of("fossil_island_verdant_valley", "Verdant Valley bird houses", 3764, 3756, PlaceDef.Style.CAMP,
                        List.of("birdhouse_space", "birdhouse_space")),
                PlaceDef.of("fossil_island_meadow", "Fossil Island meadow bird houses", 3677, 3882, PlaceDef.Style.CAMP,
                        List.of("birdhouse_space", "birdhouse_space")),
                PlaceDef.of("underwater_seaweed", "Seaweed patches", 3731, 3894, PlaceDef.Style.CAMP,
                        List.of("seaweed_patch", "seaweed_patch")),
                PlaceDef.of("canifis_mushroom", "Canifis mushroom patch", 3451, 3473, PlaceDef.Style.CAMP, List.of("mushroom_patch")),
                PlaceDef.of("draynor_belladonna", "Draynor Manor belladonna patch", 3087, 3355, PlaceDef.Style.CAMP,
                        List.of("belladonna_patch")),
                PlaceDef.of("al_kharid_cactus", "Al Kharid cactus patch", 3315, 3202, PlaceDef.Style.CAMP, List.of("cactus_patch")),
                PlaceDef.of("hosidius_vinery", "Hosidius vinery", 1806, 3554, PlaceDef.Style.CAMP,
                        List.of("grape_patch", "grape_patch", "grape_patch")),
                // Varlamore patches (OSRS Wiki patch tables): Ortus Farm west of Civitas illa Fortis (two allotments, herb,
                // flower), Kastori (fruit tree, calquat, flower), Aldarin hops, Locus Oasis hardwood.
                new PlaceDef("civitas_farm", "Civitas illa Fortis farming patches", 1586, 3099, PlaceDef.Style.CAMP,
                        List.of(Station.ALLOTMENT_PATCH, Station.ALLOTMENT_PATCH, Station.HERB_PATCH), List.of("flower_patch", "compost_bin")),
                // Approx: Kastori's calquat patch and its flower patch (1353,3023, too close to the town's plaza for a camp of
                // its own) stand with the fruit tree patch.
                PlaceDef.of("kastori_patches", "Kastori farming patches", 1350, 3057, PlaceDef.Style.CAMP,
                        List.of("fruit_tree_patch", "calquat_patch", "flower_patch")),
                PlaceDef.of("aldarin_hops", "Aldarin hops patch", 1365, 2939, PlaceDef.Style.CAMP, List.of("hops_patch")),
                PlaceDef.of("locus_oasis_hardwood", "Locus Oasis hardwood patch", 1684, 2974, PlaceDef.Style.CAMP,
                        List.of("hardwood_patch")),
                // The Coral Nurseries lie under water off the Great Conch's east coast (wiki map 3298,8860, entered by the steps
                // at 3272,2463); approx: the two nurseries stand at the steps.
                PlaceDef.of("coral_nurseries", "Coral Nurseries", 3272, 2463, PlaceDef.Style.CAMP,
                        List.of("coral_nursery_patch", "coral_nursery_patch")));
    }
}
