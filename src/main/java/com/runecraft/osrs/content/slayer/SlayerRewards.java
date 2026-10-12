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
    /** Block list size (OSRS: 6 with enough quest points). */
    public static final int MAX_BLOCKS = 6;
    /** Superior slayer creatures spawn at 1/200 on task once "Bigger and Badder" is unlocked. */
    public static final int SUPERIOR_CHANCE = 200;

    public enum Kind {
        /** Toggles a feature (Gargoyle smasher, Malevolent masquerade...). */
        FEATURE,
        /** Allows a task to be assigned (Seeing red, Watch the birdie...). */
        TASK_UNLOCK,
        /** Extends a task's amount ("Augment my abbies"). */
        EXTEND
    }

    /** @param target the category for EXTEND/TASK_UNLOCK rewards, or null */
    public record Unlock(String id, String name, int cost, Kind kind, String target, String description) {
    }

    /** Something bought with slayer points. */
    public record PointBuy(String item, int amount, int cost, String requiresUnlock) {
    }

    /** Slayer equipment sold for coins by every slayer master. */
    public record ShopItem(String item, int price, int slayerLevel) {
    }

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
                    "Learn to fletch broad arrows (52 Fletching) and buy broad arrowheads."),
            new Unlock("malevolent_masquerade", "Malevolent masquerade", 400, Kind.FEATURE, null,
                    "Learn to combine slayer protective gear into a slayer helmet (55 Crafting)."),
            new Unlock("ring_bling", "Ring bling", 150, Kind.FEATURE, null,
                    "Learn to craft slayer rings from a gold bar and an enchanted gem (75 Crafting)."),
            new Unlock("bigger_and_badder", "Bigger and Badder", 50, Kind.FEATURE, null,
                    "Superior slayer creatures may appear while you're on task."),
            new Unlock("seeing_red", "Seeing red", 50, Kind.TASK_UNLOCK, "red_dragons",
                    "Konar, Nieve and Duradel may assign red dragons."),
            new Unlock("i_hope_you_mith_me", "I hope you mith me!", 80, Kind.TASK_UNLOCK, "mithril_dragons",
                    "Konar, Nieve and Duradel may assign mithril dragons."),
            new Unlock("watch_the_birdie", "Watch the birdie", 80, Kind.TASK_UNLOCK, "aviansies",
                    "Chaeldar, Nieve and Duradel may assign aviansies."),
            new Unlock("hot_stuff", "Hot stuff", 100, Kind.TASK_UNLOCK, "tzhaar",
                    "Chaeldar, Nieve and Duradel may assign TzHaar."),
            new Unlock("reptile_got_ripped", "Reptile got ripped", 75, Kind.TASK_UNLOCK, "lizardmen",
                    "Konar, Chaeldar, Nieve and Duradel may assign lizardmen."),
            new Unlock("basilocked", "Basilocked", 80, Kind.TASK_UNLOCK, "basilisk_knights",
                    "Konar, Nieve and Duradel may assign basilisk knights."),
            new Unlock("augment_my_abbies", "Augment my abbies", 100, Kind.EXTEND, "abyssal_demons",
                    "Abyssal demon tasks are longer."),
            new Unlock("need_more_darkness", "Need more darkness", 100, Kind.EXTEND, "dark_beasts",
                    "Dark beast tasks are longer."),
            new Unlock("ankou_very_much", "Ankou very much", 100, Kind.EXTEND, "ankou", "Ankou tasks are longer."),
            new Unlock("fire_and_darkness", "Fire & Darkness", 50, Kind.EXTEND, "black_dragons",
                    "Black dragon tasks are longer."),
            new Unlock("pedal_to_the_metals", "Pedal to the metals", 100, Kind.EXTEND, "bronze_dragons",
                    "Bronze, iron and steel dragon tasks are longer."),
            new Unlock("i_really_mith_you", "I really mith you", 120, Kind.EXTEND, "mithril_dragons",
                    "Mithril dragon tasks are longer."),
            new Unlock("adamind_some_more", "Adamind some more", 100, Kind.EXTEND, "adamant_dragons",
                    "Adamant dragon tasks are longer."),
            new Unlock("ruuuuune", "RUUUUUNE", 100, Kind.EXTEND, "rune_dragons", "Rune dragon tasks are longer."),
            new Unlock("spiritual_fervour", "Spiritual fervour", 100, Kind.EXTEND, "spiritual_creatures",
                    "Spiritual creature tasks are longer."),
            new Unlock("greater_challenge", "Greater challenge", 100, Kind.EXTEND, "greater_demons",
                    "Greater demon tasks are longer."),
            new Unlock("its_dark_in_here", "It's dark in here", 100, Kind.EXTEND, "black_demons",
                    "Black demon tasks are longer."),
            new Unlock("bleed_me_dry", "Bleed me dry", 75, Kind.EXTEND, "bloodveld", "Bloodveld tasks are longer."),
            new Unlock("smell_ya_later", "Smell ya later", 100, Kind.EXTEND, "aberrant_spectres",
                    "Aberrant spectre tasks are longer."),
            new Unlock("horrorific", "Horrorific", 100, Kind.EXTEND, "cave_horrors", "Cave horror tasks are longer."),
            new Unlock("to_dust_you_shall_return", "To dust you shall return", 100, Kind.EXTEND, "dust_devils",
                    "Dust devil tasks are longer."),
            new Unlock("wyver_nother_one", "Wyver-nother one", 100, Kind.EXTEND, "skeletal_wyverns",
                    "Skeletal wyvern tasks are longer."),
            new Unlock("wyver_nother_two", "Wyver-nother two", 100, Kind.EXTEND, "fossil_island_wyverns",
                    "Fossil Island wyvern tasks are longer."),
            new Unlock("get_smashed", "Get smashed", 100, Kind.EXTEND, "gargoyles", "Gargoyle tasks are longer."),
            new Unlock("nechs_please", "Nechs please", 100, Kind.EXTEND, "nechryael", "Nechryael tasks are longer."),
            new Unlock("krack_on", "Krack on", 100, Kind.EXTEND, "cave_kraken", "Cave kraken tasks are longer."),
            new Unlock("basilonger", "Basilonger", 100, Kind.EXTEND, "basilisks", "Basilisk tasks are longer."));

    public static final List<PointBuy> BUY = List.of(
            new PointBuy("slayer_ring_8", 1, 75, null),
            new PointBuy("broad_arrows", 250, 35, null),
            new PointBuy("broad_arrowheads", 100, 25, "broader_fletching"),
            new PointBuy("herb_sack", 1, 750, null));

    public static final List<ShopItem> SHOP = List.of(
            new ShopItem("enchanted_gem", 1, 1),
            new ShopItem("rock_hammer", 500, 1),
            new ShopItem("bag_of_salt", 10, 1),
            new ShopItem("ice_cooler", 1, 1),
            new ShopItem("fungicide_spray", 300, 1),
            new ShopItem("insulated_boots", 200, 1),
            new ShopItem("slayer_gloves", 200, 1),
            new ShopItem("mirror_shield", 5000, 25),
            new ShopItem("spiny_helmet", 650, 5),
            new ShopItem("earmuffs", 200, 15),
            new ShopItem("nose_peg", 200, 1),
            new ShopItem("facemask", 200, 1),
            new ShopItem("witchwood_icon", 900, 1),
            new ShopItem("slayer_bell", 150, 1),
            new ShopItem("leaf_bladed_spear", 31000, 55),
            new ShopItem("leaf_bladed_sword", 68000, 55),
            new ShopItem("broad_arrows", 60, 55),
            new ShopItem("unlit_bug_lantern", 130, 1),
            // Approximation: OSRS boots of stone come from the Karuulm dungeon; sold here so the dungeon is accessible.
            new ShopItem("boots_of_stone", 2000, 44),
            new ShopItem("slayer_staff", 21000, 55),
            // Not sold by slayer masters in OSRS (Duke Horacio / potion shops); offered here for convenience.
            new ShopItem("antifire_potion", 1500, 1),
            new ShopItem("anti_dragon_shield", 1000, 1));

    /** Items combined into a slayer helmet (55 Crafting, "Malevolent masquerade"). */
    public static final List<String> HELMET_PARTS = List.of("black_mask", "earmuffs", "facemask", "nose_peg",
            "spiny_helmet", "enchanted_gem");

    /** One input of a {@link Craft}. */
    public record Input(String item, int count) {
    }

    /**
     * Slayer item assembly done through the master ("/slayer craft"): needs the skill level, the
     * unlock (if any) and the tool (kept).
     */
    public record Craft(String product, int count, List<Input> inputs, Skill skill, int level, double xp, String unlock,
                        String tool) {
    }

    public static final List<Craft> CRAFTS = List.of(
            new Craft("slayer_helmet", 1, HELMET_PARTS.stream().map(i -> new Input(i, 1)).toList(), Skill.CRAFTING, 55, 0,
                    "malevolent_masquerade", null),
            new Craft("slayer_helmet_i", 1, List.of(new Input("black_mask_i", 1), new Input("earmuffs", 1),
                    new Input("facemask", 1), new Input("nose_peg", 1), new Input("spiny_helmet", 1),
                    new Input("enchanted_gem", 1)), Skill.CRAFTING, 55, 0, "malevolent_masquerade", null),
            new Craft("slayer_ring_8", 1, List.of(new Input("gold_bar", 1), new Input("enchanted_gem", 1)), Skill.CRAFTING,
                    75, 75, "ring_bling", "ring_mould"),
            new Craft("bug_lantern", 1, List.of(new Input("unlit_bug_lantern", 1)), Skill.FIREMAKING, 33, 0, null, "tinderbox"),
            new Craft("boots_of_brimstone", 1, List.of(new Input("boots_of_stone", 1), new Input("drakes_claw", 1)),
                    Skill.SLAYER, 44, 0, null, null),
            new Craft("brimstone_ring", 1, List.of(new Input("hydras_eye", 1), new Input("hydras_fang", 1),
                    new Input("hydras_heart", 1)), Skill.CRAFTING, 1, 0, null, null),
            new Craft("ferocious_gloves", 1, List.of(new Input("hydra_leather", 1)), Skill.CRAFTING, 1, 0, null, "hammer"));

    /**
     * Points to block the current task, by the master who assigned it (OSRS, August 2025): Turael and
     * Spria 40, Mazchna 50, Vannaka 60, Chaeldar 70, Konar 80, Nieve 90, Duradel and Krystilia 100.
     * OSRS keeps a block list per master; here one shared list is used.
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
            case "nieve" -> 90;
            default -> BLOCK_COST;
        };
    }

    public static Unlock unlock(String id) {
        for (Unlock u : UNLOCKS) {
            if (u.id().equals(id)) {
                return u;
            }
        }
        return null;
    }

    /** The extension unlock for a category (pedal to the metals covers bronze, iron and steel). */
    public static Unlock extensionFor(String category) {
        for (Unlock u : UNLOCKS) {
            if (u.kind() == Kind.EXTEND && (category.equals(u.target())
                    || u.id().equals("pedal_to_the_metals") && (category.equals("iron_dragons") || category.equals("steel_dragons")))) {
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
        for (Craft c : CRAFTS) {
            if (c.product().equals(product)) {
                return c;
            }
        }
        return null;
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
