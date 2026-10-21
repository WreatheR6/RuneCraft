package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.Skill;

import java.util.List;

/** Slayer reward shop (points), slayer equipment shop (coins) and task management costs. */
public final class SlayerRewards {
    private SlayerRewards() {
    }

    /** Cost (points) to skip a task. */
    public static final int SKIP_COST = 30;
    /** Cost (points) to block a task at Duradel and Krystilia (see {@link #blockCost}). */
    public static final int BLOCK_COST = 100;
    /** Most blocks a master's list can hold: 6 quest-point slots plus the elite Lumbridge &amp; Draynor Diary's. */
    public static final int MAX_BLOCKS = 7;

    /**
     * Block slots per master's list (OSRS Wiki "Slayer Rewards"): one for every 50 quest points, up to 6 at 300, plus one
     * once the elite Lumbridge &amp; Draynor Diary is done.
     */
    public static int blockSlots(int questPoints, boolean eliteLumbridgeDiary) {
        return Math.min(6, Math.max(0, questPoints) / 50) + (eliteLumbridgeDiary ? 1 : 0);
    }
    /** Superior slayer creatures spawn at 1/200 on task once "Bigger and Badder" is unlocked. */
    public static final int SUPERIOR_CHANCE = 200;

    public enum Kind {
        /** Toggles a feature (Gargoyle smasher, Malevolent masquerade...). */
        FEATURE,
        /** Allows a task to be assigned (Seeing red, Watch the birdie...). */
        TASK_UNLOCK,
        /** Extends a task's amount ("Augment my abbies"). */
        EXTEND,
        /** Learns a slayer helmet recolour (the wiki's "Cosmetics" tab: King Black Bonnet...). */
        COSMETIC
    }

    /** @param target the category for EXTEND/TASK_UNLOCK rewards, or null */
    public record Unlock(String id, String name, int cost, Kind kind, String target, String description) {
    }

    /** Something bought with slayer points. */
    public record PointBuy(String item, int amount, int cost, String requiresUnlock) {
    }

    /**
     * Slayer equipment sold for coins by every slayer master. {@code slayerLevel} is the level needed to buy it: 1 for
     * everything, as in OSRS (wearing or using the item has its own requirement).
     */
    public record ShopItem(String item, int price, int slayerLevel) {
    }

    /** The "Task Storage" unlock: one task can be stored for later, and swapped back in for free. */
    public static final String TASK_STORAGE = "task_storage";
    /** The "Duly Noted" unlock: mithril dragons killed on task drop their mithril bars as banknotes. */
    public static final String DULY_NOTED = "duly_noted";

    /**
     * The OSRS Wiki's Slayer Rewards "Unlock", "Extend" and "Cosmetics" tabs (July 2026; snapshot in
     * {@code src/test/resources/osrs/slayer/reward_shop.txt}), every one of them.
     */
    public static final List<Unlock> UNLOCKS = List.of(
            new Unlock("gargoyle_smasher", "Gargoyle smasher", 120, Kind.FEATURE, null,
                    "Automatically smash gargoyles with a rock hammer when they reach 9 Hitpoints or fewer."),
            new Unlock("slug_salter", "Slug salter", 10, Kind.FEATURE, null,
                    "Automatically salt rockslugs when they're weak enough, as long as you carry a bag of salt."),
            new Unlock("reptile_freezer", "Reptile freezer", 10, Kind.FEATURE, null,
                    "Automatically freeze desert lizards with an ice cooler when they're weak enough."),
            new Unlock("shroom_sprayer", "'Shroom sprayer", 110, Kind.FEATURE, null,
                    "Automatically spray mutated zygomites with fungicide when they're weak enough."),
            new Unlock("broader_fletching", "Broader fletching", 300, Kind.FEATURE, null,
                    "Learn to fletch broad arrows (52 Fletching) and broad bolts (55 Fletching)."),
            new Unlock("malevolent_masquerade", "Malevolent masquerade", 400, Kind.FEATURE, null,
                    "Learn to combine slayer protective gear into a slayer helmet (55 Crafting)."),
            new Unlock("ring_bling", "Ring bling", 150, Kind.FEATURE, null,
                    "Learn to craft slayer rings from a gold bar and an enchanted or eternal gem (75 Crafting)."),
            new Unlock("seeing_red", "Seeing red", 50, Kind.TASK_UNLOCK, "red_dragons",
                    "Konar, Nieve and Duradel may assign red dragons."),
            new Unlock("watch_the_birdie", "Watch the birdie", 80, Kind.TASK_UNLOCK, "aviansies",
                    "Konar, Duradel, Nieve, Chaeldar and Krystilia may assign aviansies."),
            new Unlock("hot_stuff", "Hot stuff", 100, Kind.TASK_UNLOCK, "tzhaar",
                    "Chaeldar, Nieve and Duradel may assign TzHaar."),
            new Unlock("like_a_boss", "Like a Boss", 200, Kind.TASK_UNLOCK, SlayerBossTasks.CATEGORY,
                    "Konar, Nieve, Steve, Duradel and Krystilia may assign boss monsters as your task."),
            new Unlock("reptile_got_ripped", "Reptile got ripped", 75, Kind.TASK_UNLOCK, "lizardmen",
                    "Konar, Chaeldar, Nieve and Duradel may assign lizardmen."),
            new Unlock("bigger_and_badder", "Bigger and Badder", 50, Kind.FEATURE, null,
                    "Superior slayer creatures may appear while you're on task."),
            new Unlock(DULY_NOTED, "Duly Noted", 200, Kind.FEATURE, "metal_dragons",
                    "Mithril dragons drop mithril bars in banknote form while killed on assignment."),
            new Unlock("stop_the_wyvern", "Stop the Wyvern", 500, Kind.FEATURE, "fossil_island_wyverns",
                    "Never be assigned Fossil Island wyverns, without using a block slot."),
            new Unlock("double_trouble", "Double Trouble", 500, Kind.FEATURE, "boss_grotesque_guardians",
                    "Slaying the Grotesque Guardians counts for two kills towards your task."),
            new Unlock("basilocked", "Basilocked", 80, Kind.TASK_UNLOCK, "basilisks",
                    "Konar, Duradel and Nieve may assign basilisks (basilisk knights count)."),
            new Unlock("actual_vampyre_slayer", "Actual Vampyre Slayer", 80, Kind.TASK_UNLOCK, "vampyres",
                    "Chaeldar, Konar, Nieve and Duradel may assign vampyres."),
            new Unlock(TASK_STORAGE, "Task Storage", 500, Kind.FEATURE, null,
                    "Store your current task for free and take it up again later (one stored task at a time)."),
            new Unlock("i_wildy_more_slayer", "I Wildy More Slayer", 0, Kind.TASK_UNLOCK, null,
                    "Krystilia may assign jellies, dust devils, nechryael and abyssal demons."),
            new Unlock("warped_reality", "Warped Reality", 60, Kind.TASK_UNLOCK, "warped_creatures",
                    "Konar, Duradel, Nieve and Chaeldar may assign warped creatures."),
            new Unlock("lured_in", "Lured In", 80, Kind.TASK_UNLOCK, "aquanites", "Nieve and Duradel may assign aquanites."),
            new Unlock("wings_spread", "Wings Spread", 80, Kind.TASK_UNLOCK, "gryphons", "Nieve and Duradel may assign gryphons."),
            new Unlock("chance_of_heavy_frost", "Chance of Heavy Frost", 100, Kind.FEATURE, "frost_dragons",
                    "Frost dragon tasks from Nieve and Duradel are more likely (weight 5 to 8)."),
            new Unlock("need_more_darkness", "Need more darkness", 100, Kind.EXTEND, "dark_beasts",
                    "Dark beast tasks are longer (110-135)."),
            new Unlock("ankou_very_much", "Ankou very much", 100, Kind.EXTEND, "ankou", "Ankou tasks are longer (91-150)."),
            new Unlock("suq_a_nother_one", "Suq-a-nother one", 100, Kind.EXTEND, "suqahs", "Suqah tasks are longer (186-250)."),
            new Unlock("fire_and_darkness", "Fire & Darkness", 50, Kind.EXTEND, "black_dragons",
                    "Black dragon tasks are longer (40-60)."),
            new Unlock("pedal_to_the_metals", "Pedal to the metals", 200, Kind.EXTEND, "metal_dragons",
                    "Metal dragon tasks are longer (150-200)."),
            new Unlock("spiritual_fervour", "Spiritual fervour", 100, Kind.EXTEND, "spiritual_creatures",
                    "Spiritual creature tasks are longer (181-250)."),
            new Unlock("augment_my_abbies", "Augment my abbies", 100, Kind.EXTEND, "abyssal_demons",
                    "Abyssal demon tasks are longer (200-250)."),
            new Unlock("its_dark_in_here", "It's dark in here", 100, Kind.EXTEND, "black_demons",
                    "Black demon tasks are longer (200-250)."),
            new Unlock("greater_challenge", "Greater challenge", 100, Kind.EXTEND, "greater_demons",
                    "Greater demon tasks are longer (200-250)."),
            new Unlock("bleed_me_dry", "Bleed me dry", 75, Kind.EXTEND, "bloodveld", "Bloodveld tasks are longer (200-250)."),
            new Unlock("smell_ya_later", "Smell ya later", 100, Kind.EXTEND, "aberrant_spectres",
                    "Aberrant spectre tasks are longer (200-250)."),
            new Unlock("birds_of_a_feather", "Birds of a feather", 100, Kind.EXTEND, "aviansies",
                    "Aviansie tasks are longer (200-250)."),
            new Unlock("horrorific", "Horrorific", 100, Kind.EXTEND, "cave_horrors", "Cave horror tasks are longer (200-250)."),
            new Unlock("to_dust_you_shall_return", "To dust you shall return", 100, Kind.EXTEND, "dust_devils",
                    "Dust devil tasks are longer (200-250)."),
            new Unlock("wyver_nother_one", "Wyver-nother one", 100, Kind.EXTEND, "skeletal_wyverns",
                    "Skeletal wyvern tasks are longer (50-70)."),
            new Unlock("get_smashed", "Get smashed", 100, Kind.EXTEND, "gargoyles", "Gargoyle tasks are longer (200-250)."),
            new Unlock("nechs_please", "Nechs please", 100, Kind.EXTEND, "nechryael", "Nechryael tasks are longer (200-250)."),
            new Unlock("krack_on", "Krack on", 100, Kind.EXTEND, "cave_kraken", "Cave kraken tasks are longer (150-200)."),
            new Unlock("get_scabaright_on_it", "Get scabaright on it", 50, Kind.EXTEND, "scabarites",
                    "Scabarite tasks are longer (130-170)."),
            new Unlock("wyver_nother_two", "Wyver-nother two", 100, Kind.EXTEND, "fossil_island_wyverns",
                    "Fossil Island wyvern tasks are longer (55-75)."),
            new Unlock("basilonger", "Basilonger", 100, Kind.EXTEND, "basilisks", "Basilisk tasks are longer (200-250)."),
            new Unlock("more_at_stake", "More at stake", 100, Kind.EXTEND, "vampyres", "Vampyre tasks are longer (200-250)."),
            new Unlock("revenenenenenants", "Revenenenenenants", 100, Kind.EXTEND, "revenants",
                    "Revenant tasks are longer (100-150)."),
            new Unlock("more_eyes_than_sense", "More eyes than sense", 150, Kind.EXTEND, "araxytes",
                    "Araxyte tasks are longer (200-250)."),
            new Unlock("un_restraining_order", "Un-restraining Order", 100, Kind.EXTEND, "custodian_stalkers",
                    "Custodian stalker tasks are longer (200-250)."),
            new Unlock("lets_stay_all_aquanite", "Let's Stay All Aquanite", 100, Kind.EXTEND, "aquanites",
                    "Aquanite tasks are longer (150-200)."),
            new Unlock("can_of_wyrms", "Can of Wyrms", 100, Kind.EXTEND, "wyrms", "Wyrm tasks are longer (200-250)."),
            new Unlock("gryphon_and_on", "Gryphon and on", 50, Kind.EXTEND, "gryphons", "Gryphon tasks are longer (+80)."),
            new Unlock("i_see_dragons", "I see Dragons", 100, Kind.EXTEND, "frost_dragons",
                    "Frost dragon tasks are longer (180-240)."),
            new Unlock("king_black_bonnet", "King Black Bonnet", 1000, Kind.COSMETIC, null,
                    "Combine KBD heads with a slayer helmet to colour it black."),
            new Unlock("kalphite_khat", "Kalphite Khat", 1000, Kind.COSMETIC, null,
                    "Combine a Kalphite Queen head with a slayer helmet to colour it green."),
            new Unlock("unholy_helmet", "Unholy Helmet", 1000, Kind.COSMETIC, null,
                    "Combine an abyssal head with a slayer helmet to colour it red."),
            new Unlock("dark_mantle", "Dark Mantle", 1000, Kind.COSMETIC, null,
                    "Combine a dark claw with a slayer helmet to colour it purple."),
            new Unlock("undead_head", "Undead Head", 1000, Kind.COSMETIC, null,
                    "Combine Vorkath's head with a slayer helmet to colour it turquoise."),
            new Unlock("use_more_head", "Use More Head", 1000, Kind.COSMETIC, null,
                    "Combine Alchemical Hydra heads with a slayer helmet to theme it like the Alchemical Hydra."),
            new Unlock("eye_see_you", "Eye see you", 1000, Kind.COSMETIC, null,
                    "Combine an araxyte head with a slayer helmet to theme it like Araxxor."),
            new Unlock("twisted_vision", "Twisted Vision", 200, Kind.COSMETIC, null,
                    "Combine twisted horns with a slayer helmet to theme it like the Great Olm."),
            new Unlock("absolutely_slayin", "Absolutely Slayin'", 1000, Kind.COSMETIC, null,
                    "Combine the Slayer skillcape hood with a slayer helmet to show your mastery (99 Slayer)."),
            new Unlock("oath_breaker", "Oath Breaker", 200, Kind.COSMETIC, null,
                    "Combine a demonic quill with a slayer helmet to theme it like Oathplate."));

    public static final List<PointBuy> BUY = List.of(
            new PointBuy("slayer_ring_8", 1, 75, null),
            new PointBuy("broad_arrows", 250, 35, null),
            new PointBuy("broad_bolts", 250, 35, null),
            new PointBuy("broad_arrowheads", 100, 25, "broader_fletching"),
            new PointBuy("herb_sack", 1, 750, null),
            new PointBuy("rune_pouch", 1, 750, null),
            new PointBuy("looting_bag", 1, 10, null));

    /** Point-shop items a player may own only one of (OSRS: the rune pouch). */
    public static final List<String> ONE_ONLY = List.of("rune_pouch");

    /**
     * The Slayer Equipment shop (OSRS Wiki "Slayer Equipment (shop)", July 2026): prices are the items' values and nothing
     * needs a Slayer level to buy (the reinforced goggles need A Porcine of Interest, see {@link #SHOP_QUESTS}).
     */
    public static final List<ShopItem> SHOP = List.of(
            new ShopItem("enchanted_gem", 1, 1),
            new ShopItem("mirror_shield", 5000, 1),
            new ShopItem("leaf_bladed_spear", 31000, 1),
            new ShopItem("broad_arrows", 60, 1),
            new ShopItem("bag_of_salt", 10, 1),
            new ShopItem("rock_hammer", 500, 1),
            new ShopItem("facemask", 200, 1),
            new ShopItem("earmuffs", 200, 1),
            new ShopItem("nose_peg", 200, 1),
            new ShopItem("slayer_staff", 21000, 1),
            new ShopItem("spiny_helmet", 650, 1),
            new ShopItem("fishing_explosive", 60, 1),
            new ShopItem("ice_cooler", 1, 1),
            new ShopItem("slayer_gloves", 200, 1),
            new ShopItem("unlit_bug_lantern", 130, 1),
            new ShopItem("insulated_boots", 200, 1),
            new ShopItem("fungicide_spray", 300, 1),
            new ShopItem("fungicide", 10, 1),
            new ShopItem("witchwood_icon", 900, 1),
            new ShopItem("slayer_bell", 150, 1),
            new ShopItem("broad_arrowheads", 55, 1),
            new ShopItem("broad_arrowhead_pack", 5500, 1),
            new ShopItem("unfinished_broad_bolts", 55, 1),
            new ShopItem("unfinished_broad_bolt_pack", 5500, 1),
            new ShopItem("rock_thrownhammer", 200, 1),
            new ShopItem("boots_of_stone", 200, 1),
            new ShopItem("reinforced_goggles", 100, 1),
            // Not sold by slayer masters in OSRS (Duke Horacio / potion shops); offered here for convenience.
            new ShopItem("antifire_potion", 1500, 1),
            new ShopItem("anti_dragon_shield", 1000, 1));

    /** Shop items sold only after a quest (OSRS: spare reinforced goggles after A Porcine of Interest). */
    public static final java.util.Map<String, String> SHOP_QUESTS = java.util.Map.of("reinforced_goggles", "a_porcine_of_interest");

    /** Item packs bought from the shop and opened in the inventory: pack id to {item, count} (OSRS: 100 each). */
    public static final java.util.Map<String, Input> PACKS = java.util.Map.of(
            "broad_arrowhead_pack", new Input("broad_arrowheads", 100),
            "unfinished_broad_bolt_pack", new Input("unfinished_broad_bolts", 100));

    /** Fletching recipes (skilling module) that need the "Broader fletching" unlock first (OSRS). */
    public static final List<String> BROADER_FLETCHING_RECIPES = List.of("fletch_broad_arrows", "fletch_broad_bolts",
            "fletch_amethyst_broad_bolts");

    /** Items combined into a slayer helmet (55 Crafting, "Malevolent masquerade"). */
    public static final List<String> HELMET_PARTS = List.of("black_mask", "earmuffs", "facemask", "nose_peg",
            "spiny_helmet", "enchanted_gem");

    /** One input of a {@link Craft}. */
    public record Input(String item, int count) {
    }

    /**
     * Slayer item assembly done through the master ("/slayer craft"): needs the skill level, the
     * unlock (if any), the tool (kept) and, for the Combat Achievement helmets, the CA tier claimed
     * ({@code caTier}: a {@code CombatAchievements.Tier} name, or null).
     */
    public record Craft(String product, int count, List<Input> inputs, Skill skill, int level, double xp, String unlock,
                        String tool, String caTier) {
        public Craft(String product, int count, List<Input> inputs, Skill skill, int level, double xp, String unlock,
                     String tool) {
            this(product, count, inputs, skill, level, xp, unlock, tool, null);
        }
    }

    /**
     * Slayer helmet recolours (OSRS Wiki "Slayer helmet"): colour id, the head combined with the helmet, and the unlock
     * that teaches it. Each makes {@code <colour>_slayer_helmet} from a slayer helmet and {@code <colour>_slayer_helmet_i}
     * from an imbued one.
     */
    public record Recolour(String colour, String head, String unlock) {
    }

    public static final List<Recolour> RECOLOURS = List.of(
            new Recolour("black", "kbd_heads", "king_black_bonnet"),
            new Recolour("green", "kq_head", "kalphite_khat"),
            new Recolour("red", "abyssal_head", "unholy_helmet"),
            new Recolour("purple", "dark_claw", "dark_mantle"),
            new Recolour("turquoise", "vorkaths_head", "undead_head"),
            new Recolour("hydra", "alchemical_hydra_heads", "use_more_head"),
            new Recolour("araxyte", "araxyte_head", "eye_see_you"),
            new Recolour("twisted", "twisted_horns", "twisted_vision"),
            new Recolour("hooded", "slayer_hood", "absolutely_slayin"),
            new Recolour("oathplate", "demonic_quill", "oath_breaker"));

    /** The hooded slayer helmet needs 99 Slayer to make (its hood is the Slayer skillcape's). */
    public static final int HOODED_SLAYER_LEVEL = 99;

    /**
     * The Combat Achievement helmets Ghommal and Laidee Gnonock style a slayer helmet into, with no other item (OSRS Wiki):
     * TzTok after the elite tier, vampyric after the master tier, TzKal after the grandmaster tier ({colour, tier name}).
     */
    public static final List<String[]> CA_HELMETS = List.of(new String[]{"tztok", "ELITE"}, new String[]{"vampyric", "MASTER"},
            new String[]{"tzkal", "GRANDMASTER"});

    public static final List<Craft> CRAFTS = List.of(
            new Craft("slayer_helmet", 1, HELMET_PARTS.stream().map(i -> new Input(i, 1)).toList(), Skill.CRAFTING, 55, 0,
                    "malevolent_masquerade", null),
            new Craft("slayer_helmet_i", 1, List.of(new Input("black_mask_i", 1), new Input("earmuffs", 1),
                    new Input("facemask", 1), new Input("nose_peg", 1), new Input("spiny_helmet", 1),
                    new Input("enchanted_gem", 1)), Skill.CRAFTING, 55, 0, "malevolent_masquerade", null),
            new Craft("slayer_ring_8", 1, List.of(new Input("gold_bar", 1), new Input("enchanted_gem", 1)), Skill.CRAFTING,
                    75, 75, "ring_bling", "ring_mould"),
            new Craft("slayer_ring_eternal", 1, List.of(new Input("gold_bar", 1), new Input("eternal_gem", 1)), Skill.CRAFTING,
                    75, 75, "ring_bling", "ring_mould"),
            new Craft("bug_lantern", 1, List.of(new Input("unlit_bug_lantern", 1)), Skill.FIREMAKING, 33, 0, null, "tinderbox"),
            new Craft("boots_of_brimstone", 1, List.of(new Input("boots_of_stone", 1), new Input("drakes_claw", 1)),
                    Skill.SLAYER, 44, 0, null, null),
            new Craft("brimstone_ring", 1, List.of(new Input("hydras_eye", 1), new Input("hydras_fang", 1),
                    new Input("hydras_heart", 1)), Skill.CRAFTING, 1, 0, null, null),
            new Craft("ferocious_gloves", 1, List.of(new Input("hydra_leather", 1)), Skill.CRAFTING, 1, 0, null, "hammer"),
            // approx: Otto Godblessed repairs a broken dragon hasta for free in OSRS; here the slayer master does.
            new Craft("dragon_hasta", 1, List.of(new Input("broken_dragon_hasta", 1)), Skill.ATTACK, 1, 0, null, null));

    /** The recolour crafts: plain and imbued helmet for every {@link #RECOLOURS} line and {@link #CA_HELMETS} entry. */
    public static final List<Craft> HELMET_CRAFTS = helmetCrafts();

    private static List<Craft> helmetCrafts() {
        List<Craft> out = new java.util.ArrayList<>();
        for (Recolour r : RECOLOURS) {
            boolean hood = r.colour().equals("hooded");
            Skill skill = hood ? Skill.SLAYER : Skill.CRAFTING;
            int level = hood ? HOODED_SLAYER_LEVEL : 1;
            out.add(new Craft(r.colour() + "_slayer_helmet", 1, List.of(new Input("slayer_helmet", 1), new Input(r.head(), 1)),
                    skill, level, 0, r.unlock(), null));
            out.add(new Craft(r.colour() + "_slayer_helmet_i", 1, List.of(new Input("slayer_helmet_i", 1), new Input(r.head(), 1)),
                    skill, level, 0, r.unlock(), null));
        }
        for (String[] ca : CA_HELMETS) {
            out.add(new Craft(ca[0] + "_slayer_helmet", 1, List.of(new Input("slayer_helmet", 1)), Skill.CRAFTING, 1, 0, null, null,
                    ca[1]));
            out.add(new Craft(ca[0] + "_slayer_helmet_i", 1, List.of(new Input("slayer_helmet_i", 1)), Skill.CRAFTING, 1, 0, null,
                    null, ca[1]));
        }
        return List.copyOf(out);
    }

    /** Every slayer helmet id, plain or imbued, recoloured or not. */
    public static List<String> slayerHelmets(boolean imbued) {
        List<String> out = new java.util.ArrayList<>();
        out.add(imbued ? "slayer_helmet_i" : "slayer_helmet");
        for (Recolour r : RECOLOURS) {
            out.add(r.colour() + "_slayer_helmet" + (imbued ? "_i" : ""));
        }
        for (String[] ca : CA_HELMETS) {
            out.add(ca[0] + "_slayer_helmet" + (imbued ? "_i" : ""));
        }
        return List.copyOf(out);
    }

    /**
     * What a slayer helmet's "Disassemble" option gives back (OSRS Wiki "Slayer helmet"): its components (the black mask,
     * imbued for an imbued helmet, earmuffs, facemask, nose peg, spiny helmet and enchanted gem) and, for a recolour, the
     * head or item that coloured it (a Combat Achievement helmet gives nothing more). Empty for anything else.
     */
    public static List<Input> disassembly(String helmet) {
        if (helmet == null || !helmet.contains("slayer_helmet")) {
            return List.of();
        }
        boolean imbued = helmet.endsWith("_i");
        String base = imbued ? helmet.substring(0, helmet.length() - 2) : helmet;
        String colour = base.equals("slayer_helmet") ? null : base.substring(0, base.length() - "_slayer_helmet".length());
        String head = null;
        if (colour != null) {
            for (Recolour r : RECOLOURS) {
                if (r.colour().equals(colour)) {
                    head = r.head();
                }
            }
            boolean ca = CA_HELMETS.stream().anyMatch(c -> c[0].equals(colour));
            if (head == null && !ca) {
                return List.of();
            }
        }
        List<Input> out = new java.util.ArrayList<>();
        for (String part : HELMET_PARTS) {
            out.add(new Input(imbued && part.equals("black_mask") ? "black_mask_i" : part, 1));
        }
        if (head != null) {
            out.add(new Input(head, 1));
        }
        return List.copyOf(out);
    }

    /** "Duly Noted": whether a drop of {@code item} by {@code monsterId}, killed on task with the unlock, comes noted. */
    public static boolean dulyNoted(String monsterId, String item) {
        return "mithril_bar".equals(item) && monsterId != null && monsterId.startsWith("mithril_dragon");
    }

    /**
     * Points to block the current task, by the master who assigned it (OSRS, August 2025): Turael and
     * Spria 40, Mazchna 50, Vannaka 60, Chaeldar 70, Konar 80, Nieve 90, Duradel and Krystilia 100.
     */
    public static int blockCost(String masterId) {
        if (masterId == null) {
            return BLOCK_COST;
        }
        return switch (masterId) {
            case "turael", "spria" -> 40;
            case "mazchna" -> 50;
            case "vannaka" -> 60;
            case "chaeldar" -> 70;
            case "konar" -> 80;
            case "nieve", "steve" -> 90;
            default -> BLOCK_COST;
        };
    }

    /**
     * The block list a master uses: since August 2025 every master has its own, except that Turael and Spria
     * share one (and Steve uses Nieve's).
     */
    public static String blockList(String masterId) {
        if (masterId == null) {
            return "turael";
        }
        return switch (masterId) {
            case "spria" -> "turael";
            case "steve" -> "nieve";
            default -> masterId;
        };
    }

    /** A stored block entry: {@code <list>:<category>}. Entries without a list (older saves) block everywhere. */
    public static String blockKey(String masterId, String category) {
        return blockList(masterId) + ":" + category;
    }

    /** The categories blocked on {@code masterId}'s list, given every stored entry. */
    public static List<String> blockedFor(java.util.Collection<String> entries, String masterId) {
        String prefix = blockList(masterId) + ":";
        List<String> out = new java.util.ArrayList<>();
        for (String e : entries) {
            if (e.startsWith(prefix)) {
                out.add(e.substring(prefix.length()));
            } else if (e.indexOf(':') < 0) {
                out.add(e);
            }
        }
        return out;
    }

    public static Unlock unlock(String id) {
        for (Unlock u : UNLOCKS) {
            if (u.id().equals(id)) {
                return u;
            }
        }
        return null;
    }

    /** The extension unlock for a category. */
    public static Unlock extensionFor(String category) {
        for (Unlock u : UNLOCKS) {
            if (u.kind() == Kind.EXTEND && category.equals(u.target())) {
                return u;
            }
        }
        return null;
    }

    public static ShopItem shopItem(String item) {
        for (ShopItem s : SHOP) {
            if (s.item().equals(item)) {
                return s;
            }
        }
        return null;
    }

    public static Craft craft(String product) {
        for (Craft c : allCrafts()) {
            if (c.product().equals(product)) {
                return c;
            }
        }
        return null;
    }

    /** Everything the master can assemble: {@link #CRAFTS} then the helmet recolours. */
    public static List<Craft> allCrafts() {
        List<Craft> out = new java.util.ArrayList<>(CRAFTS);
        out.addAll(HELMET_CRAFTS);
        return out;
    }

    public static PointBuy pointBuy(String item) {
        for (PointBuy b : BUY) {
            if (b.item().equals(item)) {
                return b;
            }
        }
        return null;
    }
}
