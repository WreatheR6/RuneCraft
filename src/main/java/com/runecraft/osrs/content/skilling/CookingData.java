package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ItemSink;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Cooking: new fish (fished in {@link GatheringData} and {@link SailingData}), bread, pies, pizzas,
 * stews, cakes, dairy, potatoes and wine. Levels, XP and heals are the OSRS values; stop-burn levels
 * are approximations where the wiki gives no exact figure.
 */
public final class CookingData extends SkillingPart {

    /** A fish added by this module: fishing level/XP and cooking level/XP/heal/stop-burn. */
    public record NewFish(String id, String name, int fishLevel, double fishXp, int cookLevel, double cookXp, int heal,
                          int stopBurn, int color, int value) {
        public String raw() {
            return "raw_" + id;
        }

        public String cooked() {
            return id.equals("karambwan") ? "cooked_karambwan" : id;
        }
    }

    public static final List<NewFish> FISH = List.of(
            new NewFish("mackerel", "Mackerel", 16, 20.0, 10, 60.0, 6, 45, 0x6A8A7A, 17),
            new NewFish("cod", "Cod", 23, 45.0, 18, 75.0, 7, 52, 0x9A9A80, 25),
            new NewFish("slimy_eel", "Slimy eel", 28, 65.0, 28, 95.0, 8, 58, 0x5A6A3A, 40),
            new NewFish("cave_eel", "Cave eel", 38, 80.0, 38, 115.0, 9, 74, 0x7A6A5A, 60),
            new NewFish("bass", "Bass", 46, 100.0, 43, 130.0, 13, 80, 0x7A8A5A, 120),
            // Lava eels can never be burnt (stop-burn 0).
            new NewFish("lava_eel", "Lava eel", 53, 60.0, 53, 30.0, 14, 0, 0xC0501A, 150),
            new NewFish("karambwan", "Karambwan", 65, 50.0, 30, 190.0, 18, 99, 0x4A8A3A, 300),
            new NewFish("sea_turtle", "Sea turtle", 79, 38.0, 82, 211.3, 21, 100, 0x5A7A3A, 900),
            new NewFish("manta_ray", "Manta ray", 81, 46.0, 91, 216.2, 22, 100, 0x3A4A6A, 1100),
            // Sailing deep sea trawling catches (Sailing, Nov 2025): Fishing level/XP per trawling-net catch,
            // Cooking level and heal from the OSRS Wiki; yellowfin/bluefin Cooking XP and stop-burn levels approx.
            new NewFish("giant_krill", "Giant krill", 69, 112.5, 69, 177.5, 17, 89, 0xE07A6A, 10),
            new NewFish("haddock", "Haddock", 73, 128.5, 73, 180.5, 18, 93, 0xB0A080, 50),
            new NewFish("yellowfin", "Yellowfin", 79, 155.5, 79, 195.0, 19, 99, 0xD0B030, 160),
            new NewFish("halibut", "Halibut", 83, 195.5, 83, 212.5, 20, 100, 0xA09080, 300),
            new NewFish("bluefin", "Bluefin", 87, 220.5, 87, 220.0, 22, 100, 0x305080, 600),
            new NewFish("marlin", "Marlin", 91, 265.5, 91, 225.0, 24, 100, 0x3A5AA0, 1200));

    /** Foods eaten in two (or three) bites: eating {@code full} heals {@code heal} and leaves {@code next}. */
    public record Bite(String full, String next, int heal) {
    }

    public static final List<Bite> BITES = new ArrayList<>();

    private record Pie(String id, String name, int level, double xp, int heal, int stopBurn, int color, String[] fill) {
    }

    private static final Pie[] PIES = {
            new Pie("redberry", "Redberry", 10, 78.0, 5, 44, 0xC02040, new String[]{"redberries"}),
            new Pie("meat", "Meat", 20, 110.0, 6, 54, 0x8A4A2A, new String[]{"minecraft:cooked_beef"}),
            new Pie("apple", "Apple", 30, 130.0, 7, 64, 0x9AB040, new String[]{"cooking_apple"}),
            new Pie("garden", "Garden", 34, 138.0, 6, 68, 0x6AA040, new String[]{"tomato", "onion", "cabbage"}),
            new Pie("fish", "Fish", 47, 164.0, 6, 81, 0xA0A0B0, new String[]{"trout", "cod", "potato"}),
            new Pie("mushroom", "Mushroom", 60, 200.0, 8, 94, 0xC0A080, new String[]{"sulliuscep_cap"}),
            new Pie("admiral", "Admiral", 70, 210.0, 8, 94, 0xD08060, new String[]{"salmon", "tuna", "potato"}),
            new Pie("dragonfruit", "Dragonfruit", 73, 220.0, 10, 93, 0xE04080, new String[]{"dragonfruit"}),
            // Wild pie uses raw bear meat and raw chompy in-game; vanilla beef and raw bird meat stand in.
            new Pie("wild", "Wild", 85, 240.0, 11, 100, 0x6A3A2A, new String[]{"minecraft:beef", "raw_bird_meat", "minecraft:rabbit"}),
            new Pie("summer", "Summer", 95, 260.0, 11, 100, 0xE0607A, new String[]{"strawberry", "watermelon", "cooking_apple"})};

    private record Pizza(String id, String name, int level, double toppingXp, int heal, String topping) {
    }

    private static final Pizza[] PIZZAS = {
            new Pizza("plain", "Plain", 35, 0, 7, null),
            new Pizza("meat", "Meat", 45, 26.0, 8, "minecraft:cooked_beef"),
            new Pizza("anchovy", "Anchovy", 55, 39.0, 9, "anchovies"),
            new Pizza("pineapple", "Pineapple", 65, 45.0, 11, "pineapple_ring")};

    static {
        for (Pie p : PIES) {
            BITES.add(new Bite(p.id + "_pie", "half_a_" + p.id + "_pie", p.heal));
            BITES.add(new Bite("half_a_" + p.id + "_pie", "pie_dish", p.heal));
        }
        for (Pizza p : PIZZAS) {
            BITES.add(new Bite(p.id + "_pizza", "half_" + p.id + "_pizza", p.heal));
            BITES.add(new Bite("half_" + p.id + "_pizza", null, p.heal));
        }
        BITES.add(new Bite("chocolate_cake", "two_thirds_chocolate_cake", 5));
        BITES.add(new Bite("two_thirds_chocolate_cake", "chocolate_slice", 5));
    }

    @Override
    public void items(ItemSink s) {
        for (NewFish f : FISH) {
            simple(s, f.raw(), "Raw " + lower(f.name()), "fish_raw", f.color(), f.value());
            food(s, f.cooked(), f.id().equals("karambwan") ? "Cooked karambwan" : f.name(), "fish_cooked", f.color(),
                    f.value(), f.heal());
        }
        // Containers and basic ingredients
        simple(s, "jug", "Jug", "jug", 0xC0C8D0, 1);
        simple(s, "jug_of_water", "Jug of water", "jug", 0x5080D0, 1);
        simple(s, "empty_pot", "Pot", "pot_item", 0xA07850, 1);
        simple(s, "bowl_of_water", "Bowl of water", "bowl", 0x5080D0, 3);
        simple(s, "cake_tin", "Cake tin", "cake_tin", 0xA0A0A8, 10);
        simple(s, "empty_cup", "Empty cup", "cup", 0xE0E0E0, 2);
        simple(s, "cup_of_hot_water", "Cup of hot water", "cup", 0x90B0E0, 2);
        food(s, "cup_of_tea", "Cup of tea", "cup", 0xA06030, 10, 3);
        simple(s, "grapes", "Grapes", "produce", 0x6A2A8A, 1);
        simple(s, "unfermented_wine", "Unfermented wine", "jug", 0x7A2A5A, 1).examine("It needs some time to ferment.");
        food(s, "jug_of_wine", "Jug of wine", "jug", 0x8A1A3A, 1, 11);
        simple(s, "jug_of_bad_wine", "Jug of bad wine", "jug", 0x4A3A2A, 1);
        simple(s, "seaweed", "Seaweed", "secondary", 0x3A6A2A, 2);
        simple(s, "soda_ash", "Soda ash", "secondary", 0xE0E0D0, 2);
        simple(s, "sinew", "Sinew", "string", 0xD0A090, 3);
        simple(s, "chocolate_bar", "Chocolate bar", "bar", 0x6A3A1A, 10);
        simple(s, "pineapple_ring", "Pineapple ring", "produce", 0xE0C040, 1);
        simple(s, "pot_of_cream", "Pot of cream", "pot_item", 0xF0F0E0, 10);
        simple(s, "pat_of_butter", "Pat of butter", "produce", 0xF0E070, 10);
        simple(s, "cheese", "Cheese", "produce", 0xF0D040, 4);
        // Burnt food
        simple(s, "burnt_bread", "Burnt bread", "bread", 0x2A2010, 1);
        simple(s, "burnt_pie", "Burnt pie", "pie", 0x2A2010, 1);
        simple(s, "burnt_pizza", "Burnt pizza", "pizza", 0x2A2010, 1);
        simple(s, "burnt_cake", "Burnt cake", "cake", 0x2A2010, 1);
        simple(s, "burnt_stew", "Burnt stew", "bowl", 0x2A2010, 1);
        // Bread, dough
        simple(s, "bread_dough", "Bread dough", "dough", 0xE8D8B0, 1);
        simple(s, "pastry_dough", "Pastry dough", "dough", 0xF0E0C0, 1);
        simple(s, "pizza_base", "Pizza base", "pizza", 0xF0E0C0, 4);
        food(s, "bread", "Bread", "bread", 0xC09050, 12, 5);
        // Pies
        simple(s, "pie_shell", "Pie shell", "pie", 0xE8D0A0, 2);
        for (Pie p : PIES) {
            simple(s, "uncooked_" + p.id + "_pie", "Uncooked " + lower(p.name) + " pie", "pie", p.color, 10);
            food(s, p.id + "_pie", p.name + " pie", "pie", p.color, p.level * 3, p.heal).stack(16);
            food(s, "half_a_" + p.id + "_pie", "Half a " + lower(p.name) + " pie", "pie", p.color, p.level, p.heal).stack(16);
        }
        // Pizzas
        simple(s, "incomplete_pizza", "Incomplete pizza", "pizza", 0xE06040, 4);
        simple(s, "uncooked_pizza", "Uncooked pizza", "pizza", 0xE0A040, 5);
        for (Pizza p : PIZZAS) {
            food(s, p.id + "_pizza", p.name + " pizza", "pizza", p.id.equals("plain") ? 0xE0A040 : 0xC07040,
                    p.level * 2, p.heal).stack(16);
            food(s, "half_" + p.id + "_pizza", "1/2 " + lower(p.name) + " pizza", "pizza", 0xC08040, p.level, p.heal).stack(16);
        }
        // Stews and curry
        simple(s, "incomplete_stew", "Incomplete stew", "bowl", 0xB09060, 3);
        simple(s, "uncooked_stew", "Uncooked stew", "bowl", 0xA07040, 5);
        simple(s, "uncooked_curry", "Uncooked curry", "bowl", 0xC08020, 10);
        food(s, "stew", "Stew", "bowl", 0x8A5A2A, 20, 11);
        food(s, "curry", "Curry", "bowl", 0xC08020, 40, 19);
        // Cakes
        simple(s, "uncooked_cake", "Uncooked cake", "cake", 0xF0E8C0, 5);
        food(s, "chocolate_cake", "Chocolate cake", "cake", 0x5A2A10, 70, 5).stack(16);
        food(s, "two_thirds_chocolate_cake", "2/3 chocolate cake", "cake", 0x5A2A10, 50, 5).stack(16);
        // Potatoes
        food(s, "potato_with_butter", "Potato with butter", "produce", 0xE0C060, 40, 14);
        food(s, "potato_with_cheese", "Potato with cheese", "produce", 0xE0B030, 60, 16);
        simple(s, "tuna_and_corn", "Tuna and corn", "bowl", 0xD0C060, 40);
        food(s, "tuna_potato", "Tuna potato", "produce", 0xD0B060, 100, 22);
        // Barbarian fishing products
        food(s, "roe", "Roe", "secondary", 0xE07030, 5, 3);
        food(s, "caviar", "Caviar", "secondary", 0x202020, 20, 5);
        // Family Crest reward: lowers the burn level of lobster, swordfish, monkfish, shark and anglerfish.
        s.add("cooking_gauntlets", "Cooking gauntlets", com.runecraft.osrs.ItemDef.Kind.SIMPLE, "gloves", 0xC8C8D0).value(1).stack(1)
                .examine("These gauntlets empower me when cooking. (They work while carried.)");
    }

    @Override
    public void recipes(Consumer<Recipe> out) {
        MenuType C = MenuType.COOKING;
        for (NewFish f : FISH) {
            Recipe.Builder b = r("cook_" + f.cooked(), C, f.id().equals("karambwan") ? "Cooked karambwan" : f.name(), Skill.COOKING,
                    f.cookLevel(), f.cookXp()).in(f.raw(), 1).out(f.cooked(), 1).ticks(4);
            if (f.stopBurn() > 0) {
                b.burn(f.stopBurn(), "burnt_fish");
            }
            out.accept(b.build());
        }
        // Doughs (flour + water)
        out.accept(r("mix_bread_dough", C, "Bread dough", Skill.COOKING, 1, 0).in("pot_of_flour", 1).in("jug_of_water", 1)
                .out("bread_dough", 1).out("empty_pot", 1).out("jug", 1).ticks(2).build());
        out.accept(r("mix_pastry_dough", C, "Pastry dough", Skill.COOKING, 1, 0).in("pot_of_flour", 1).in("jug_of_water", 1)
                .out("pastry_dough", 1).out("empty_pot", 1).out("jug", 1).ticks(2).build());
        out.accept(r("mix_pizza_base", C, "Pizza base", Skill.COOKING, 35, 0).in("pot_of_flour", 1).in("jug_of_water", 1)
                .out("pizza_base", 1).out("empty_pot", 1).out("jug", 1).ticks(2).build());
        out.accept(r("cook_bread", C, "Bread", Skill.COOKING, 1, 40.0).in("bread_dough", 1).out("bread", 1)
                .burn(35, "burnt_bread").ticks(4).build());
        // Pies
        out.accept(r("make_pie_shell", C, "Pie shell", Skill.COOKING, 1, 0).in("pastry_dough", 1).in("pie_dish", 1)
                .out("pie_shell", 1).ticks(2).build());
        for (Pie p : PIES) {
            Recipe.Builder b = r("make_uncooked_" + p.id + "_pie", C, "Uncooked " + lower(p.name) + " pie", Skill.COOKING, p.level, 0)
                    .in("pie_shell", 1).out("uncooked_" + p.id + "_pie", 1).ticks(2);
            for (String f : p.fill) {
                b.in(f, 1);
            }
            out.accept(b.build());
            out.accept(r("cook_" + p.id + "_pie", C, p.name + " pie", Skill.COOKING, p.level, p.xp)
                    .in("uncooked_" + p.id + "_pie", 1).out(p.id + "_pie", 1).burn(p.stopBurn, "burnt_pie").ticks(4).build());
        }
        // Pizzas
        out.accept(r("make_incomplete_pizza", C, "Incomplete pizza", Skill.COOKING, 35, 0).in("pizza_base", 1).in("tomato", 1)
                .out("incomplete_pizza", 1).ticks(2).build());
        out.accept(r("make_uncooked_pizza", C, "Uncooked pizza", Skill.COOKING, 35, 0).in("incomplete_pizza", 1).in("cheese", 1)
                .out("uncooked_pizza", 1).ticks(2).build());
        out.accept(r("cook_plain_pizza", C, "Plain pizza", Skill.COOKING, 35, 143.0).in("uncooked_pizza", 1).out("plain_pizza", 1)
                .burn(68, "burnt_pizza").ticks(4).build());
        for (Pizza p : PIZZAS) {
            if (p.topping != null) {
                out.accept(r("make_" + p.id + "_pizza", C, p.name + " pizza", Skill.COOKING, p.level, p.toppingXp)
                        .in("plain_pizza", 1).in(p.topping, 1).out(p.id + "_pizza", 1).ticks(2).build());
            }
        }
        out.accept(r("slice_pineapple", C, "Pineapple rings", Skill.COOKING, 1, 0).in("pineapple", 1).out("pineapple_ring", 4)
                .tool("knife").ticks(1).build());
        // Stew and curry
        out.accept(r("fill_bowl", C, "Bowl of water", Skill.COOKING, 1, 0).in("bowl", 1).in("jug_of_water", 1)
                .out("bowl_of_water", 1).out("jug", 1).ticks(1).build());
        out.accept(r("make_incomplete_stew", C, "Incomplete stew", Skill.COOKING, 25, 0).in("bowl_of_water", 1).in("potato", 1)
                .out("incomplete_stew", 1).ticks(2).build());
        out.accept(r("make_uncooked_stew", C, "Uncooked stew", Skill.COOKING, 25, 0).in("incomplete_stew", 1)
                .in("minecraft:cooked_beef", 1).out("uncooked_stew", 1).ticks(2).build());
        out.accept(r("cook_stew", C, "Stew", Skill.COOKING, 25, 117.0).in("uncooked_stew", 1).out("stew", 1)
                .burn(58, "burnt_stew").ticks(4).build());
        out.accept(r("make_uncooked_curry", C, "Uncooked curry (spice)", Skill.COOKING, 60, 0).in("uncooked_stew", 1).in("spice", 1)
                .out("uncooked_curry", 1).ticks(2).build());
        out.accept(r("make_uncooked_curry_leaf", C, "Uncooked curry (curry leaves)", Skill.COOKING, 60, 0).in("uncooked_stew", 1)
                .in("curry_leaf", 3).out("uncooked_curry", 1).ticks(2).build());
        out.accept(r("cook_curry", C, "Curry", Skill.COOKING, 60, 280.0).in("uncooked_curry", 1).out("curry", 1)
                .burn(74, "burnt_stew").ticks(4).build());
        // Cakes
        out.accept(r("make_uncooked_cake", C, "Uncooked cake", Skill.COOKING, 40, 0).in("cake_tin", 1).in("pot_of_flour", 1)
                .in("minecraft:egg", 1).in("minecraft:milk_bucket", 1).out("uncooked_cake", 1).out("empty_pot", 1)
                .out("minecraft:bucket", 1).ticks(2).build());
        out.accept(r("cook_cake", C, "Cake", Skill.COOKING, 40, 180.0).in("uncooked_cake", 1).out("cake", 1).out("cake_tin", 1)
                .burn(74, "burnt_cake").ticks(4).build());
        out.accept(r("make_chocolate_cake", C, "Chocolate cake", Skill.COOKING, 50, 30.0).in("cake", 1).in("chocolate_bar", 1)
                .out("chocolate_cake", 1).ticks(2).build());
        out.accept(r("grind_chocolate", C, "Chocolate dust", Skill.COOKING, 1, 0).in("chocolate_bar", 1).out("chocolate_dust", 1)
                .tool("pestle_and_mortar").ticks(1).build());
        // Dairy (churn) - milk buckets keep their bucket
        out.accept(r("churn_cream", C, "Pot of cream", Skill.COOKING, 21, 18.0).in("minecraft:milk_bucket", 1).in("empty_pot", 1)
                .out("pot_of_cream", 1).out("minecraft:bucket", 1).ticks(4).build());
        out.accept(r("churn_butter", C, "Pat of butter", Skill.COOKING, 38, 40.5).in("minecraft:milk_bucket", 1)
                .out("pat_of_butter", 1).out("minecraft:bucket", 1).ticks(4).build());
        out.accept(r("churn_cheese", C, "Cheese", Skill.COOKING, 48, 64.0).in("minecraft:milk_bucket", 1)
                .out("cheese", 1).out("minecraft:bucket", 1).ticks(4).build());
        // Potatoes
        out.accept(r("make_potato_with_butter", C, "Potato with butter", Skill.COOKING, 39, 40.5).in("minecraft:baked_potato", 1)
                .in("pat_of_butter", 1).out("potato_with_butter", 1).ticks(2).build());
        out.accept(r("make_potato_with_cheese", C, "Potato with cheese", Skill.COOKING, 47, 10.0).in("potato_with_butter", 1)
                .in("cheese", 1).out("potato_with_cheese", 1).ticks(2).build());
        out.accept(r("make_tuna_and_corn", C, "Tuna and corn", Skill.COOKING, 67, 204.0).in("tuna", 1).in("sweetcorn", 1)
                .in("bowl", 1).out("tuna_and_corn", 1).ticks(2).build());
        out.accept(r("make_tuna_potato", C, "Tuna potato", Skill.COOKING, 68, 10.0).in("potato_with_butter", 1)
                .in("tuna_and_corn", 1).out("tuna_potato", 1).out("bowl", 1).ticks(2).build());
        // Caleb (Family Crest) enchants the steel gauntlets into cooking gauntlets, and back.
        out.accept(plain("enchant_cooking_gauntlets", C, "Cooking gauntlets (Caleb's enchantment)").in("steel_gauntlets", 1)
                .out("cooking_gauntlets", 1).ticks(1).build());
        out.accept(plain("unenchant_cooking_gauntlets", C, "Steel gauntlets (remove enchantment)").in("cooking_gauntlets", 1)
                .out("steel_gauntlets", 1).ticks(1).build());
        // Wine: ferments in the inventory for 200 XP (see SkillingHooks); making it gives none.
        out.accept(r("make_unfermented_wine", C, "Unfermented wine", Skill.COOKING, 35, 0).in("grapes", 1).in("jug_of_water", 1)
                .out("unfermented_wine", 1).ticks(2).build());
        // Tea / hot water
        out.accept(r("boil_water", C, "Cup of hot water", Skill.COOKING, 1, 0).in("empty_cup", 1).in("jug_of_water", 1)
                .out("cup_of_hot_water", 1).out("jug", 1).ticks(2).build());
        // Misc fire uses
        out.accept(r("burn_seaweed", C, "Soda ash (seaweed)", Skill.COOKING, 1, 0).in("seaweed", 1).out("soda_ash", 1).ticks(3).build());
        out.accept(r("burn_kelp", C, "Soda ash (kelp)", Skill.COOKING, 1, 0).in("minecraft:kelp", 1).out("soda_ash", 1).ticks(3).build());
        out.accept(r("cook_sinew", C, "Sinew", Skill.COOKING, 1, 3.0).in("minecraft:beef", 1).out("sinew", 1).ticks(3).build());
        // Barbarian fishing: gutting leaping fish (knife)
        out.accept(r("gut_leaping_trout", C, "Roe (gut leaping trout)", Skill.COOKING, 15, 10.0).in("leaping_trout", 1)
                .out("roe", 1).tool("knife").ticks(2).build());
        out.accept(r("gut_leaping_salmon", C, "Roe (gut leaping salmon)", Skill.COOKING, 15, 10.0).in("leaping_salmon", 1)
                .out("roe", 1).tool("knife").ticks(2).build());
        out.accept(r("gut_leaping_sturgeon", C, "Caviar (gut leaping sturgeon)", Skill.COOKING, 15, 15.0).in("leaping_sturgeon", 1)
                .out("caviar", 1).tool("knife").ticks(2).build());
        // Sacred eels are dissected for Zulrah's scales (3-10 in-game; fixed 6 here).
        out.accept(r("dissect_sacred_eel", C, "Zulrah's scales (dissect sacred eel)", Skill.COOKING, 72, 105.0).in("sacred_eel", 1)
                .out("zulrahs_scales", 6).tool("knife").ticks(3).build());
    }

    @Override
    public List<BlockDef> blocks() {
        return List.of(BlockDef.menu("dairy_churn", "Dairy churn", MenuType.COOKING, 0xA08060, "churn"));
    }
}
