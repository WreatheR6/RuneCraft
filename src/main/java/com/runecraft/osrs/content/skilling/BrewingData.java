package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.PlaceDef;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Cooking's brewing and gnome cooking.
 *
 * <p><b>Brewing</b> (fermenting vats at the Port Phasmatys and Keldagrim breweries, menu BREWING): two buckets of
 * water, two barley malt (not for cider), the brew's ingredient and a pot of ale yeast make a vat that fills
 * eight beer glasses. Levels and XP are the OSRS Wiki values (XP per vat). A vat has a {@link #MATURE_CHANCE}
 * chance to turn mature (the (m) ales: stronger boosts). Approx: the brew is ready at once instead of
 * fermenting for several hours. Ale yeast comes from The Green Ghost in Port Phasmatys (sold by the Port
 * Phasmatys general store here) and the Grand Exchange.
 *
 * <p><b>Gnome cooking</b>: Gianne dough baked in a batta tin, crunchy tray or gnomebowl mould (Grand Tree
 * Groceries) is topped and baked again; cocktails are shaken in a cocktail shaker and poured into a cocktail
 * glass with their garnish. The half-baked base gives 30 XP and the cocktail mix 30 XP (OSRS Wiki); the
 * finishing step gives the rest of the dish's OSRS total. Approx: toppings and garnish are added in one step.
 *
 * <p>Drinks with stat effects ({@link #DRINKS}) are applied by SkillingHooks' ItemHooks.
 */
public final class BrewingData extends SkillingPart {
    /** Chance that a vat turns mature. Approx: the wiki gives no exact rate (higher with "the stuff"). */
    public static final double MATURE_CHANCE = 0.05;
    /** Pints from one vat. */
    public static final int PINTS = 8;

    /** A skill change from a drink: positive boosts by {@code flat + pct * level}, negative drains by as much. */
    public record Mod(Skill skill, int flat, double pct) {
        int amount(int level) {
            return Math.abs(flat) + (int) Math.floor(level * pct);
        }
    }

    /** A drink: Hitpoints healed and its boosts and drains. */
    public record Drink(String id, String name, int heal, List<Mod> mods) {
        /** Applies the boosts and drains to {@code s}. Boosts never stack above their own amount; drains stack. */
        public void apply(SkillSet s) {
            for (Mod m : mods) {
                if (m.flat() >= 0) {
                    s.applyBoost(m.skill(), m.flat(), m.pct());
                } else {
                    int drain = m.amount(s.currentLevel(m.skill()));
                    s.setBoost(m.skill(), Math.max(-s.level(m.skill()), s.boost(m.skill()) - drain));
                }
            }
        }
    }

    /** A brew: ale id, mature id, level, XP per vat, ingredient and how many, and whether it needs barley malt. */
    public record Brew(String id, String name, String mature, String matureName, int level, double xp, String ingredient,
                       int amount, boolean malt, int color) {
    }

    public static final List<Brew> BREWS = List.of(
            new Brew("cider", "Cider", "mature_cider", "Mature cider", 14, 182.0, "apple_mush", 4, false, 0xD0A040),
            new Brew("dwarven_stout", "Dwarven stout", "dwarven_stout_m", "Dwarven stout(m)", 19, 215.0, "hammerstone_hops", 4, true,
                    0x3A2A1A),
            new Brew("asgarnian_ale", "Asgarnian ale", "asgarnian_ale_m", "Asgarnian ale(m)", 24, 248.0, "asgarnian_hops", 4, true,
                    0xA06A2A),
            new Brew("greenmans_ale", "Greenman's ale", "greenmans_ale_m", "Greenman's ale(m)", 29, 281.0, "harralander", 4, true,
                    0x5A7A2A),
            new Brew("wizards_mind_bomb", "Wizard's mind bomb", "wizards_mind_bomb_m", "Mature wmb", 34, 314.0, "yanillian_hops", 4,
                    true, 0xC0A040),
            new Brew("dragon_bitter", "Dragon bitter", "dragon_bitter_m", "Dragon bitter(m)", 39, 347.0, "krandorian_hops", 4, true,
                    0xC06A2A),
            new Brew("moonlight_mead", "Moonlight mead", "moonlight_mead_m", "Moonlight mead(m)", 44, 380.0, "mushroom", 4, true,
                    0x8AA0C0),
            new Brew("axemans_folly", "Axeman's folly", "axemans_folly_m", "Axeman's folly(m)", 49, 413.0, "oak_roots", 1, true,
                    0x8A6A3A),
            new Brew("chefs_delight", "Chef's delight", "chefs_delight_m", "Chef's delight(m)", 54, 446.0, "chocolate_dust", 4, true,
                    0x6A3A1A),
            new Brew("slayers_respite", "Slayer's respite", "slayers_respite_m", "Slayer's respite(m)", 59, 479.0, "wildblood_hops", 4,
                    true, 0x8A1A1A));

    /** Brews whose normal pint another module already defines (shop and quest items). */
    private static final List<String> DEFINED_ELSEWHERE = List.of("cider", "dwarven_stout", "asgarnian_ale", "wizards_mind_bomb");

    public static Brew brewByRecipe(String recipeId) {
        for (Brew b : BREWS) {
            if (("brew_" + b.id()).equals(recipeId)) {
                return b;
            }
        }
        return null;
    }

    private static final Skill ATT = Skill.ATTACK, STR = Skill.STRENGTH, DEF = Skill.DEFENCE;

    private static Mod up(Skill s, int flat) {
        return new Mod(s, flat, 0);
    }

    private static Mod up(Skill s, int flat, double pct) {
        return new Mod(s, flat, pct);
    }

    private static Mod down(Skill s, int flat) {
        return new Mod(s, -flat, 0);
    }

    private static Mod down(Skill s, int flat, double pct) {
        return new Mod(s, -flat, pct);
    }

    /** Drinkable items with effects (OSRS Wiki "Ale" and "Drinks"; values marked approx are best guesses). */
    public static final List<Drink> DRINKS = List.of(
            new Drink("cider", "Cider", 2, List.of(up(Skill.FARMING, 1), down(ATT, 2), down(STR, 2))),
            new Drink("mature_cider", "Mature cider", 2, List.of(up(Skill.FARMING, 2), down(ATT, 5), down(STR, 5))), // approx
            new Drink("dwarven_stout", "Dwarven stout", 1, List.of(up(Skill.MINING, 1), up(Skill.SMITHING, 1), down(ATT, 2),
                    down(STR, 2), down(DEF, 2))),
            new Drink("dwarven_stout_m", "Dwarven stout(m)", 2, List.of(up(Skill.MINING, 2), up(Skill.SMITHING, 2),
                    down(ATT, 3, 0.04), down(STR, 3, 0.04), down(DEF, 3, 0.04))),
            new Drink("asgarnian_ale", "Asgarnian ale", 2, List.of(up(STR, 2), down(ATT, 4))),
            new Drink("asgarnian_ale_m", "Asgarnian ale(m)", 2, List.of(up(STR, 3), down(ATT, 6))),
            new Drink("greenmans_ale", "Greenman's ale", 1, List.of(up(Skill.HERBLORE, 1), down(ATT, 3), down(STR, 3), down(DEF, 3))),
            new Drink("greenmans_ale_m", "Greenman's ale(m)", 2, List.of(up(Skill.HERBLORE, 2), down(ATT, 3, 0.04), down(STR, 3, 0.04),
                    down(DEF, 3, 0.04))),
            // +2 Magic, +3 from level 50 (2% of the level stands in for the step).
            new Drink("wizards_mind_bomb", "Wizard's mind bomb", 1, List.of(up(Skill.MAGIC, 2, 0.02), down(ATT, 3), down(STR, 4),
                    down(DEF, 3))),
            new Drink("wizards_mind_bomb_m", "Mature wmb", 2, List.of(up(Skill.MAGIC, 3, 0.02), down(ATT, 5), down(STR, 5),
                    down(DEF, 5))), // approx
            new Drink("dragon_bitter", "Dragon bitter", 1, List.of(up(STR, 2), down(ATT, 6))),
            new Drink("dragon_bitter_m", "Dragon bitter(m)", 2, List.of(up(STR, 3), down(ATT, 6))), // approx
            new Drink("moonlight_mead", "Moonlight mead", 4, List.of()),
            new Drink("moonlight_mead_m", "Moonlight mead(m)", 6, List.of()),
            new Drink("axemans_folly", "Axeman's folly", 1, List.of(up(Skill.WOODCUTTING, 1), down(ATT, 3), down(STR, 3))),
            new Drink("axemans_folly_m", "Axeman's folly(m)", 2, List.of(up(Skill.WOODCUTTING, 2), down(ATT, 4), down(STR, 4))), // approx
            new Drink("chefs_delight", "Chef's delight", 1, List.of(up(Skill.COOKING, 1, 0.05), down(ATT, 3), down(STR, 3))),
            new Drink("chefs_delight_m", "Chef's delight(m)", 2, List.of(up(Skill.COOKING, 2, 0.05), down(ATT, 3), down(STR, 3))),
            new Drink("slayers_respite", "Slayer's respite", 1, List.of(up(Skill.SLAYER, 2), down(ATT, 2), down(STR, 2))),
            new Drink("slayers_respite_m", "Slayer's respite(m)", 2, List.of(up(Skill.SLAYER, 4), down(ATT, 2), down(STR, 2))),
            // Gnome cocktails: the fruit ones only heal; the alcoholic ones boost Strength by 1 + 6% and lower Attack
            // by 3 + 2% (approx: RSC/RS wiki figures).
            new Drink("fruit_blast", "Fruit blast", 9, List.of()),
            new Drink("pineapple_punch", "Pineapple punch", 9, List.of()),
            new Drink("wizard_blizzard", "Wizard blizzard", 5, List.of(up(STR, 1, 0.06), down(ATT, 3, 0.02))),
            new Drink("short_green_guy", "Short green guy", 5, List.of(up(STR, 1, 0.06), down(ATT, 3, 0.02))),
            new Drink("drunk_dragon", "Drunk dragon", 5, List.of(up(STR, 1, 0.06), down(ATT, 3, 0.02))),
            new Drink("choc_saturday", "Choc saturday", 7, List.of(up(STR, 1, 0.06), down(ATT, 3, 0.02))),
            new Drink("blurberry_special", "Blurberry special", 7, List.of(up(STR, 1, 0.06), down(ATT, 3, 0.02))),
            new Drink("brandy", "Brandy", 5, List.of(up(STR, 1, 0.06), down(ATT, 3, 0.02)))); // approx

    public static Drink drink(String id) {
        for (Drink d : DRINKS) {
            if (d.id().equals(id)) {
                return d;
            }
        }
        return null;
    }

    /** Drinks that leave an empty beer glass (ales) or cocktail glass (cocktails). */
    public static String emptyGlass(String id) {
        for (Brew b : BREWS) {
            if (b.id().equals(id) || b.mature().equals(id)) {
                return "beer_glass";
            }
        }
        for (Cocktail c : COCKTAILS) {
            if (c.id().equals(id)) {
                return "cocktail_glass";
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ gnome cooking

    /** A gnome dish: base (batta, crunchies or bowl), level, OSRS total XP (incl. the half-baked base), heal, toppings. */
    public record GnomeDish(String id, String name, String base, int level, double totalXp, int heal, Map<String, Integer> toppings) {
        public String halfBaked() {
            return switch (base) {
                case "batta" -> "half_baked_batta";
                case "crunchies" -> "half_baked_crunchies";
                default -> "half_baked_bowl";
            };
        }

        public String burnt() {
            return switch (base) {
                case "batta" -> "burnt_batta";
                case "crunchies" -> "burnt_crunchies";
                default -> "burnt_gnomebowl";
            };
        }
    }

    /** XP for baking a raw batta, crunchies or gnomebowl (OSRS Wiki). */
    public static final double HALF_BAKE_XP = 30.0;
    /** XP for shaking a cocktail mix (OSRS Wiki). */
    public static final double COCKTAIL_MIX_XP = 30.0;

    private static Map<String, Integer> m(Object... kv) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], (Integer) kv[i + 1]);
        }
        return map;
    }

    /** OSRS levels/total XP (RuneLite / wiki); toppings include the garnish. Heals: battas 11, crunchies 7-8, bowls 12-15. */
    public static final List<GnomeDish> GNOME_DISHES = List.of(
            new GnomeDish("toad_crunchies", "Toad crunchies", "crunchies", 10, 100.0, 8,
                    m("gnome_spice", 2, "toads_legs", 2, "equa_leaves", 1)),
            new GnomeDish("spicy_crunchies", "Spicy crunchies", "crunchies", 12, 100.0, 7, m("gnome_spice", 3, "equa_leaves", 1)),
            new GnomeDish("worm_crunchies", "Worm crunchies", "crunchies", 14, 104.0, 8,
                    m("king_worm", 2, "gnome_spice", 2, "equa_leaves", 1)),
            new GnomeDish("chocchip_crunchies", "Chocchip crunchies", "crunchies", 16, 100.0, 7,
                    m("chocolate_bar", 2, "gnome_spice", 1, "chocolate_dust", 1)),
            new GnomeDish("fruit_batta", "Fruit batta", "batta", 25, 150.0, 11,
                    m("equa_leaves", 4, "lime_chunks", 1, "orange_chunks", 1, "pineapple_chunks", 1, "gnome_spice", 1)),
            new GnomeDish("toad_batta", "Toad batta", "batta", 26, 152.0, 11,
                    m("equa_leaves", 1, "gnome_spice", 1, "cheese", 1, "toads_legs", 1)),
            new GnomeDish("worm_batta", "Worm batta", "batta", 27, 154.0, 11,
                    m("king_worm", 1, "cheese", 1, "gnome_spice", 1, "equa_leaves", 1)),
            new GnomeDish("vegetable_batta", "Vegetable batta", "batta", 28, 156.0, 11,
                    m("tomato", 2, "cheese", 1, "dwellberries", 1, "onion", 1, "cabbage", 1, "equa_leaves", 1)),
            new GnomeDish("cheese_and_tomato_batta", "Cheese+tom batta", "batta", 29, 158.0, 11,
                    m("cheese", 1, "tomato", 1, "equa_leaves", 1)),
            new GnomeDish("worm_hole", "Worm hole", "bowl", 30, 170.0, 12,
                    m("king_worm", 4, "onion", 2, "gnome_spice", 1, "equa_leaves", 1)),
            new GnomeDish("veg_ball", "Veg ball", "bowl", 35, 175.0, 12,
                    m("onion", 2, "potato", 2, "gnome_spice", 1, "equa_leaves", 1)),
            new GnomeDish("tangled_toads_legs", "Tangled toad's legs", "bowl", 40, 185.0, 15,
                    m("toads_legs", 4, "gnome_spice", 2, "cheese", 2, "dwellberries", 1, "equa_leaves", 3)),
            new GnomeDish("chocolate_bomb", "Chocolate bomb", "bowl", 42, 190.0, 15,
                    m("chocolate_bar", 4, "equa_leaves", 1, "pot_of_cream", 2, "chocolate_dust", 1)));

    /** Dishes another module (the Gnome Restaurant minigame) already defines as items. */
    private static final List<String> DISHES_ELSEWHERE = List.of("fruit_batta", "toad_batta", "worm_hole", "chocolate_bomb",
            "tangled_toads_legs", "veg_ball");

    /** A cocktail: the shaken mix (made with a cocktail shaker) and the garnish poured into a cocktail glass. */
    public record Cocktail(String id, String name, String mixId, String mixName, int level, double totalXp,
                           Map<String, Integer> mix, Map<String, Integer> garnish, int color) {
    }

    public static final List<Cocktail> COCKTAILS = List.of(
            new Cocktail("fruit_blast", "Fruit blast", "mixed_blast", "Mixed blast", 6, 50.0,
                    m("pineapple", 1, "lemon", 1, "orange", 1), m("lemon_slices", 1), 0xE0C040),
            new Cocktail("pineapple_punch", "Pineapple punch", "mixed_punch", "Mixed punch", 8, 70.0,
                    m("pineapple", 2, "lemon", 1, "orange", 1), m("lime_chunks", 1, "pineapple_chunks", 1, "orange_slices", 1), 0xE0B030),
            new Cocktail("wizard_blizzard", "Wizard blizzard", "mixed_blizzard", "Mixed blizzard", 18, 110.0,
                    m("vodka", 2, "gin", 1, "lime", 1, "lemon", 1, "orange", 1), m("pineapple_chunks", 1, "lime_slices", 1), 0xA0D0E0),
            new Cocktail("short_green_guy", "Short green guy", "mixed_sgg", "Mixed sgg", 20, 120.0,
                    m("vodka", 1, "lime", 3), m("lime_slices", 1, "equa_leaves", 1), 0x60C040),
            new Cocktail("drunk_dragon", "Drunk dragon", "mixed_dragon", "Mixed dragon", 32, 160.0,
                    m("vodka", 1, "gin", 1, "dwellberries", 1), m("pineapple_chunks", 1, "pot_of_cream", 1), 0xC04060),
            new Cocktail("choc_saturday", "Choc saturday", "mixed_saturday", "Mixed saturday", 33, 170.0,
                    m("whisky", 1, "chocolate_bar", 1, "equa_leaves", 1), m("chocolate_dust", 1, "pot_of_cream", 1), 0x6A3A1A),
            new Cocktail("blurberry_special", "Blurberry special", "mixed_special", "Mixed special", 37, 180.0,
                    m("vodka", 1, "brandy", 1, "gin", 1, "lemon", 2, "orange", 1),
                    m("lemon_chunks", 1, "orange_chunks", 1, "equa_leaves", 1, "lime_slices", 1), 0x5A6AC0));

    // ------------------------------------------------------------------ data

    @Override
    public void items(ItemSink s) {
        simple(s, "ale_yeast", "Ale yeast", "pot_item", 0xE0D090, 1).examine("A pot filled with ale yeast.");
        simple(s, "apple_mush", "Apple mush", "bucket", 0xA0B040, 13).examine("A bucket of apple mush.");
        for (Brew b : BREWS) {
            Drink d = drink(b.id());
            if (!DEFINED_ELSEWHERE.contains(b.id())) {
                s.add(b.id(), b.name(), Kind.FOOD, "beer", b.color()).value(3).heal(d.heal()).stack(1);
            }
            Drink dm = drink(b.mature());
            s.add(b.mature(), b.matureName(), Kind.FOOD, "beer", darker(b.color())).value(4).heal(dm.heal()).stack(1)
                    .examine("This looks a good deal stronger than normal " + lower(b.name()) + ".");
        }
        // Gnome cooking tools and ingredients (Grand Tree Groceries).
        tool(s, "batta_tin", "Batta tin", "cake_tin", 0xA0A0A8, 10).examine("A deep tin used for baking gnome battas in.");
        tool(s, "crunchy_tray", "Crunchy tray", "cake_tin", 0x9A9AA0, 10).examine("A shallow tray used for baking crunchies in.");
        tool(s, "gnomebowl_mould", "Gnomebowl mould", "bowl", 0xA0A0A8, 10).examine("A large ovenproof bowl.");
        food(s, "king_worm", "King worm", "bait", 0xC08080, 2, 2).examine("They're a gnome delicacy apparently.");
        simple(s, "raw_batta", "Raw batta", "dough", 0xE0D0A0, 2);
        simple(s, "raw_crunchies", "Raw crunchies", "dough", 0xE0D0A0, 2);
        simple(s, "raw_gnomebowl", "Raw gnomebowl", "dough", 0xE0D0A0, 2);
        simple(s, "half_baked_batta", "Half baked batta", "bread", 0xD0B070, 2);
        simple(s, "half_baked_crunchies", "Half baked crunchy", "bread", 0xD0B070, 2);
        simple(s, "half_baked_bowl", "Half baked bowl", "bowl", 0xD0B070, 2);
        simple(s, "burnt_batta", "Burnt batta", "bread", 0x2A2010, 1);
        simple(s, "burnt_crunchies", "Burnt crunchies", "bread", 0x2A2010, 1);
        simple(s, "burnt_gnomebowl", "Burnt gnomebowl", "bowl", 0x2A2010, 1);
        for (GnomeDish d : GNOME_DISHES) {
            if (!DISHES_ELSEWHERE.contains(d.id())) {
                s.add(d.id(), d.name(), Kind.FOOD, d.base().equals("bowl") ? "bowl" : "cake", 0xC09050).value(d.level() * 3)
                        .heal(d.heal()).stack(1);
            }
        }
        // Cocktails
        simple(s, "cocktail_glass", "Cocktail glass", "glass", 0xD0E8F0, 1).examine("For sipping cocktails.");
        String[][] spirits = {{"vodka", "Vodka", "E8E8F0"}, {"gin", "Gin", "D0E8E0"}, {"whisky", "Whisky", "C08030"}};
        for (String[] sp : spirits) {
            simple(s, sp[0], sp[1], "beer", Integer.parseInt(sp[2], 16), 5);
        }
        food(s, "brandy", "Brandy", "beer", 0x9A4A1A, 5, 5).examine("A strong spirit best served in a large glass.");
        String[][] fruit = {{"lemon", "E8E040"}, {"lime", "60C040"}, {"orange", "E09030"}};
        for (String[] f : fruit) {
            simple(s, f[0] + "_slices", title(f[0]) + " slices", "produce", Integer.parseInt(f[1], 16), 1);
            simple(s, f[0] + "_chunks", title(f[0]) + " chunks", "produce", Integer.parseInt(f[1], 16), 1);
        }
        simple(s, "pineapple_chunks", "Pineapple chunks", "produce", 0xE0C040, 1);
        for (Cocktail c : COCKTAILS) {
            simple(s, c.mixId(), c.mixName(), "glass", c.color(), 2).stack(1);
            s.add(c.id(), c.name(), Kind.FOOD, "glass", c.color()).value(30).heal(drink(c.id()).heal()).stack(1);
        }
    }

    private static int darker(int c) {
        return ((c >> 16 & 0xFF) * 3 / 4) << 16 | ((c >> 8 & 0xFF) * 3 / 4) << 8 | (c & 0xFF) * 3 / 4;
    }

    @Override
    public void recipes(Consumer<Recipe> out) {
        MenuType B = MenuType.BREWING;
        out.accept(r("press_apple_mush", B, "Apple mush", Skill.COOKING, 14, 0).in("cooking_apple", 4).in("minecraft:bucket", 1)
                .out("apple_mush", 1).ticks(4).build());
        for (Brew b : BREWS) {
            Recipe.Builder rb = r("brew_" + b.id(), B, b.name() + " (8 pints)", Skill.COOKING, b.level(), b.xp())
                    .in("minecraft:water_bucket", 2).in(b.ingredient(), b.amount()).in("ale_yeast", 1).in("beer_glass", PINTS)
                    .out(b.id(), PINTS).out("minecraft:bucket", 2).out("empty_pot", 1).ticks(5);
            if (b.malt()) {
                rb.in("barley_malt", 2);
            }
            out.accept(rb.build());
        }

        MenuType C = MenuType.COOKING;
        // Knife work for garnishes.
        for (String f : List.of("lemon", "lime", "orange")) {
            out.accept(plain("slice_" + f, C, title(f) + " slices").in(f, 1).out(f + "_slices", 1).tool("knife").ticks(2).build());
            out.accept(plain("dice_" + f, C, title(f) + " chunks").in(f, 1).out(f + "_chunks", 1).tool("knife").ticks(2).build());
        }
        out.accept(plain("dice_pineapple", C, "Pineapple chunks").in("pineapple", 1).out("pineapple_chunks", 1).tool("knife")
                .ticks(2).build());
        // Bases
        out.accept(plain("make_raw_batta", C, "Raw batta").in("gianne_dough", 1).out("raw_batta", 1).tool("batta_tin").ticks(1).build());
        out.accept(plain("make_raw_crunchies", C, "Raw crunchies").in("gianne_dough", 1).out("raw_crunchies", 1).tool("crunchy_tray")
                .ticks(1).build());
        out.accept(plain("make_raw_gnomebowl", C, "Raw gnomebowl").in("gianne_dough", 1).out("raw_gnomebowl", 1)
                .tool("gnomebowl_mould").ticks(1).build());
        out.accept(r("bake_half_baked_batta", C, "Half baked batta", Skill.COOKING, 1, HALF_BAKE_XP).in("raw_batta", 1)
                .out("half_baked_batta", 1).burn(40, "burnt_batta").ticks(4).build()); // stop-burn approx
        out.accept(r("bake_half_baked_crunchies", C, "Half baked crunchy", Skill.COOKING, 1, HALF_BAKE_XP).in("raw_crunchies", 1)
                .out("half_baked_crunchies", 1).burn(40, "burnt_crunchies").ticks(4).build()); // stop-burn approx
        out.accept(r("bake_half_baked_bowl", C, "Half baked bowl", Skill.COOKING, 1, HALF_BAKE_XP).in("raw_gnomebowl", 1)
                .out("half_baked_bowl", 1).burn(40, "burnt_gnomebowl").ticks(4).build()); // stop-burn approx
        for (GnomeDish d : GNOME_DISHES) {
            Recipe.Builder rb = r("cook_" + d.id(), C, d.name(), Skill.COOKING, d.level(), d.totalXp() - HALF_BAKE_XP)
                    .in(d.halfBaked(), 1).out(d.id(), 1).burn(d.level() + 30, d.burnt()).ticks(4); // stop-burn approx
            d.toppings().forEach(rb::in);
            out.accept(rb.build());
        }
        for (Cocktail c : COCKTAILS) {
            Recipe.Builder mix = r("shake_" + c.mixId(), C, c.mixName(), Skill.COOKING, c.level(), COCKTAIL_MIX_XP)
                    .out(c.mixId(), 1).tool("cocktail_shaker").ticks(2);
            c.mix().forEach(mix::in);
            out.accept(mix.build());
            Recipe.Builder pour = r("pour_" + c.id(), C, c.name(), Skill.COOKING, c.level(), c.totalXp() - COCKTAIL_MIX_XP)
                    .in(c.mixId(), 1).in("cocktail_glass", 1).out(c.id(), 1).ticks(2);
            c.garnish().forEach(pour::in);
            out.accept(pour.build());
        }
    }

    @Override
    public List<BlockDef> blocks() {
        return List.of(BlockDef.menu("fermenting_vat", "Fermenting vat", MenuType.BREWING, 0x7A5A3A, "vat"));
    }

    @Override
    public List<PlaceDef> places() {
        List<PlaceDef> list = new ArrayList<>();
        // Port Phasmatys brewery (north-west of the town) and Keldagrim's brewery, next to the market stand-in.
        list.add(PlaceDef.of("port_phasmatys_brewery", "Port Phasmatys brewery", 3678, 3490, PlaceDef.Style.CAMP,
                List.of("fermenting_vat", "fermenting_vat"))); // approx tile
        list.add(PlaceDef.of("keldagrim_brewery", "Keldagrim brewery", 2772, 3712, PlaceDef.Style.CAVE,
                List.of("fermenting_vat", "fermenting_vat"))); // approx: beside the Keldagrim market stand-in
        return list;
    }
}
