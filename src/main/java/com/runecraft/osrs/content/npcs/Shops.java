package com.runecraft.osrs.content.npcs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.runecraft.osrs.content.npcs.ShopDef.general;
import static com.runecraft.osrs.content.npcs.ShopDef.specialist;

/**
 * Every ordinary shop with its OSRS stock (normal stock amounts) and price rates. Shops marked
 * "OSRS Wiki: <name>" were synced with the wiki's shop data (Flipping-Utilities/parsed-osrs dump):
 * stock lines, normal stock, and the shop's sell %, buy % and per-item change %. Stock the wiki lists
 * that RuneCraft has no item for is named in that comment. Inns keep their own stock (OSRS bartenders
 * sell through dialogue, the wiki has no shop rates for them). The shopkeeper of each shop is an NPC in
 * {@link Townsfolk} (or an existing character, see {@link Townsfolk#EXTRA_TRADERS}).
 *
 * <p>Not repeated here because other modules already provide them: TzHaar shops (minigames reward
 * shops), Slayer equipment (slayer masters), Culinaromancer's chest (Recipe for Disaster gypsy),
 * Grand Exchange.
 */
public final class Shops {
    private static final Map<String, ShopDef> SHOPS = new LinkedHashMap<>();

    private Shops() {
    }

    public static List<ShopDef> all() {
        return Collections.unmodifiableList(new ArrayList<>(SHOPS.values()));
    }

    public static ShopDef get(String id) {
        return SHOPS.get(id);
    }

    /** The shop an NPC runs, or null. */
    public static ShopDef byNpc(String npcId) {
        for (ShopDef s : SHOPS.values()) {
            if (s.npc().equals(npcId)) {
                return s;
            }
        }
        return null;
    }

    private static void add(ShopDef.Builder b) {
        ShopDef def = b.build();
        if (SHOPS.put(def.id(), def) != null) {
            throw new IllegalStateException("Duplicate shop " + def.id());
        }
    }

    /** The standard general store stock (Lumbridge, Varrock, Falador, Edgeville...). */
    private static ShopDef.Builder standard(ShopDef.Builder b) {
        return b.sell("empty_pot", 5).sell("jug", 2).sell("minecraft:shears", 2, 1).sell("minecraft:bucket", 3, 2)
                .sell("bowl", 2).sell("cake_tin", 2).sell("tinderbox", 2).sell("chisel", 2).sell("hammer", 5)
                .sell("spade", 5).sell("knife", 5);
    }

    static {
        // ------------------------------------------------------------- general stores
        // OSRS Wiki: Lumbridge General Store (not in RuneCraft: Empty jug pack, Empty bucket pack, Newcomer map, Security book)
        add(general("lumbridge_general_store", "Lumbridge General Store", "npc_lumbridge_shopkeeper").sell("empty_pot", 5)
                .sell("jug", 2).sell("minecraft:shears", 2, 1).sell("knife", 5).sell("minecraft:bucket", 3, 2).sell("bowl", 2)
                .sell("cake_tin", 2).sell("tinderbox", 2).sell("chisel", 2).sell("spade", 5).sell("hammer", 5)
                .greeting("Can I help you at all?"));
        // OSRS Wiki: Varrock General Store (not in RuneCraft: Empty jug pack, Empty bucket pack, Newcomer map, Security book)
        add(general("varrock_general_store", "Varrock General Store", "npc_varrock_shopkeeper").sell("empty_pot", 5)
                .sell("jug", 2).sell("minecraft:shears", 2, 1).sell("minecraft:bucket", 3, 2).sell("bowl", 2)
                .sell("cake_tin", 2).sell("tinderbox", 2).sell("chisel", 2).sell("hammer", 5).greeting("Can I help you at all?"));
        // OSRS Wiki: Falador General Store (not in RuneCraft: Empty jug pack, Empty bucket pack, Newcomer map, Security book)
        add(general("falador_general_store", "Falador General Store", "npc_falador_shopkeeper").sell("empty_pot", 5)
                .sell("jug", 2).sell("minecraft:shears", 2, 1).sell("minecraft:bucket", 3, 2).sell("bowl", 2)
                .sell("cake_tin", 2).sell("tinderbox", 2).sell("chisel", 2).sell("hammer", 5).greeting("Can I help you at all?"));
        // OSRS Wiki: Edgeville General Store (not in RuneCraft: Empty jug pack, Empty bucket pack, Newcomer map, Security book)
        add(general("edgeville_general_store", "Edgeville General Store", "npc_edgeville_shopkeeper").sell("empty_pot", 5)
                .sell("jug", 2).sell("minecraft:shears", 2, 1).sell("minecraft:bucket", 3, 2).sell("bowl", 2)
                .sell("cake_tin", 2).sell("tinderbox", 2).sell("chisel", 2).sell("hammer", 5).sell("knife", 1)
                .greeting("Can I help you at all?"));
        // OSRS Wiki: Al Kharid General Store (not in RuneCraft: Empty jug pack, Empty bucket pack, Newcomer map, Security book)
        add(general("al_kharid_general_store", "Al Kharid General Store", "npc_al_kharid_shopkeeper").sell("empty_pot", 5)
                .sell("jug", 2).sell("minecraft:shears", 2, 1).sell("minecraft:bucket", 3, 2).sell("bowl", 2)
                .sell("cake_tin", 2).sell("tinderbox", 2).sell("chisel", 2).sell("hammer", 5).greeting("Can I help you at all?"));
        // OSRS Wiki: Rimmington General Store (not in RuneCraft: Empty jug pack, Empty bucket pack, Newcomer map, Security book)
        add(general("rimmington_general_store", "Rimmington General Store", "npc_rimmington_shopkeeper").sell("empty_pot", 5)
                .sell("jug", 2).sell("minecraft:shears", 2, 1).sell("minecraft:bucket", 3, 2).sell("bowl", 2)
                .sell("cake_tin", 2).sell("tinderbox", 2).sell("chisel", 2).sell("hammer", 5).greeting("Can I help you at all?"));
        // OSRS Wiki: Karamja General Store (not in RuneCraft: Empty jug pack, Empty bucket pack)
        add(general("karamja_general_store", "Karamja General Store", "npc_karamja_shopkeeper").sell("empty_pot", 5)
                .sell("jug", 2).sell("minecraft:shears", 2, 1).sell("minecraft:bucket", 3, 2).sell("bowl", 2)
                .sell("cake_tin", 2).sell("tinderbox", 2).sell("chisel", 2).sell("hammer", 5).greeting("Can I help you at all?"));
        // OSRS Wiki: West Ardougne General Store (not in RuneCraft: Empty bucket pack)
        add(general("west_ardougne_general_store", "West Ardougne General Store", "npc_chadwell").prices(1200, 550, 20)
                .sell("empty_pot", 3).sell("rope", 3).sell("bronze_pickaxe", 2).sell("minecraft:bucket", 2, 2)
                .sell("tinderbox", 2).sell("hammer", 3).sell("leather_boots", 2).sell("longbow", 2).sell("bronze_arrow", 20)
                .sell("salmon", 10).sell("meat_pie", 10).sell("bread", 5).sell("minecraft:cooked_beef", 10, 4)
                .greeting("What do you want?"));
        // OSRS Wiki: Khazard General Store (not in RuneCraft: Empty jug pack, Empty bucket pack, Bailing bucket)
        add(general("port_khazard_general_store", "Port Khazard General Store", "npc_port_khazard_shopkeeper").prices(1400, 400, 10)
                .sell("bronze_pickaxe", 5).sell("empty_pot", 3).sell("jug", 2).sell("minecraft:shears", 2, 1)
                .sell("minecraft:bucket", 2, 2).sell("tinderbox", 2).sell("chisel", 2).sell("hammer", 5).sell("rope", 30)
                .sell("pot_of_flour", 30).sell("swamp_paste", 5000).sell("knife", 2).sell("bronze_axe", 5)
                .greeting("Can I help you at all?"));
        // OSRS Wiki: General Store (Canifis) (not in RuneCraft: Empty bucket pack, Empty jug pack, Sample bottle)
        add(general("canifis_general_store", "Canifis General Store", "npc_fidelio").prices(2000, 100, 30).sell("needle", 2)
                .sell("thread", 50).sell("empty_pot", 3).sell("minecraft:bucket", 2, 2).sell("jug", 2).sell("tinderbox", 2)
                .sell("chisel", 2).sell("hammer", 5).sell("knife", 2).sell("blood_talisman", 1)
                .greeting("Welcome to my humble store. Please buy something, I need the money."));
        // OSRS Wiki: Port Phasmatys General Store (not in RuneCraft: Empty bucket pack, Empty jug pack)
        add(general("port_phasmatys_general_store", "Port Phasmatys General Store", "npc_phasmatys_shopkeeper")
                .sell("empty_pot", 100).sell("minecraft:bucket", 100, 2).sell("minecraft:shears", 2, 1).sell("jug", 2)
                .sell("tinderbox", 2).sell("chisel", 2).sell("hammer", 5)
                // Approx: in OSRS ale yeast is sold by Metarialus at The Green Ghost inn next door (brewing).
                .sell("ale_yeast", 20).greeting("Wooooo... welcome to my shop."));
        // OSRS Wiki: Nardah General Store (not in RuneCraft: Empty jug pack, Empty bucket pack)
        add(general("nardah_general_store", "Kazemde's General Store", "npc_kazemde").sell("empty_pot", 5).sell("jug", 2)
                .sell("minecraft:shears", 2, 1).sell("minecraft:bucket", 3, 2).sell("bowl", 2).sell("cake_tin", 2)
                .sell("tinderbox", 2).sell("chisel", 2).sell("hammer", 5).sell("minecraft:cooked_beef", 4, 4)
                .greeting("Can I help you at all?"));
        // OSRS Wiki: Pollnivneach general store (not in RuneCraft: Empty jug pack, Waterskin(3), Empty bucket pack, Fake beard)
        add(general("pollnivneach_general_store", "Pollnivneach General Store", "npc_pollnivneach_shopkeeper").prices(1000, 550, 10)
                .sell("empty_pot", 3).sell("jug", 2).sell("desert_shirt", 3).sell("desert_boots", 2)
                .sell("minecraft:bucket", 12, 2).sell("desert_disguise_feud", 12).sell("cheese", 5).sell("lime", 5)
                .sell("orange", 5).sell("tomato", 5).sell("jug_of_water", 5).sell("bowl_of_water", 7)
                .sell("minecraft:water_bucket", 8, 6).greeting("Can I help you at all?"));
        // OSRS Wiki: Lletya General Store (not in RuneCraft: Empty jug pack, Empty bucket pack)
        add(general("lletya_general_store", "Lletya General Store", "npc_lletya_shopkeeper").sell("empty_pot", 3).sell("jug", 2)
                .sell("minecraft:shears", 2, 1).sell("minecraft:bucket", 2, 2).sell("tinderbox", 2).sell("chisel", 2)
                .sell("hammer", 5).sell("spade", 5).sell("knife", 4)
                .greeting("Greetings, traveller. Can I interest you in anything?"));
        add(standard(general("gnome_general_store", "Gnome General Store", "npc_gnome_shopkeeper"))
                .greeting("Hello, human. Have a look at my wares."));
        // OSRS Wiki: Obli's General Store (not in RuneCraft: Empty vial pack, Unlit torch, Papyrus, Charcoal, Water-filled vial pack)
        add(general("shilo_general_store", "Obli's General Store", "npc_obli").prices(1500, 500, 20).sell("tinderbox", 2)
                .sell("vial", 10).sell("pestle_and_mortar", 3).sell("empty_pot", 3).sell("bronze_axe", 3)
                .sell("bronze_pickaxe", 2).sell("iron_axe", 5).sell("leather_body", 12).sell("leather_gloves", 10)
                .sell("leather_boots", 10).sell("minecraft:cooked_beef", 2, 4).sell("bread", 10).sell("bronze_bar", 10)
                .sell("spade", 10).sell("candle", 10).sell("chisel", 10).sell("hammer", 10).sell("vial_of_water", 50)
                .sell("machete", 50).sell("rope", 10).greeting("Welcome to Obli's General Store, Bwana!"));
        // OSRS Wiki: Bandit Duty Free (not in RuneCraft: Empty jug pack)
        add(general("bandit_camp_general_store", "Bandit Duty Free", "npc_bandit_shopkeeper").prices(900, 600, 30)
                .sell("empty_pot", 5).sell("jug", 2).sell("tinderbox", 2).sell("chisel", 2).sell("hammer", 5)
                .sell("bronze_pickaxe", 5).sell("bronze_axe", 10).greeting("Er... what do you want?"));
        // OSRS Wiki: Jiminua's Jungle Store (not in RuneCraft: Unlit torch, Empty vial pack, Water-filled vial pack, Antipoison(3), Papyrus, Charcoal)
        add(general("jiminuas_jungle_store", "Jiminua's Jungle Store", "npc_jiminua").prices(1500, 500, 20).sell("tinderbox", 2)
                .sell("candle", 10).sell("empty_pot", 3).sell("rope", 10).sell("leather_body", 12).sell("leather_gloves", 10)
                .sell("leather_boots", 10).sell("minecraft:cooked_beef", 2, 4).sell("bread", 10).sell("vial", 10)
                .sell("vial_of_water", 50).sell("pestle_and_mortar", 3).sell("knife", 2).sell("hammer", 10).sell("machete", 50)
                .sell("chisel", 10).sell("spade", 10).sell("bronze_axe", 2).sell("bronze_pickaxe", 2).sell("iron_axe", 5)
                .sell("bronze_bar", 10).sell("pure_essence", 0).greeting("Welcome to the Jungle Store, can I help you at all?"));
        // OSRS Wiki: Arhein's General Goods (not in RuneCraft: Empty bucket pack)
        add(general("arheins_store", "Arhein's Store", "npc_arhein").sell("minecraft:bucket", 10, 2).sell("bronze_pickaxe", 2)
                .sell("bowl", 2).sell("cake_tin", 2).sell("tinderbox", 2).sell("chisel", 2).sell("hammer", 5).sell("rope", 2)
                .sell("empty_pot", 2).sell("knife", 2).greeting("Hello! Would you like to trade?"));

        // ------------------------------------------------------------- Lumbridge, Draynor, Varrock
        // OSRS Wiki: Bob's Brilliant Axes
        add(specialist("bobs_brilliant_axes", "Bob's Brilliant Axes", "npc_bob").sell("bronze_pickaxe", 5)
                .sell("bronze_axe", 10).sell("iron_axe", 5).sell("steel_axe", 3).sell("iron_battleaxe", 5)
                .sell("steel_battleaxe", 2).sell("mithril_battleaxe", 1).greeting("Hello. How can I help you?"));
        // OSRS Wiki: Aubury's Rune Shop (not in RuneCraft: Fire rune pack, Water rune pack, Air rune pack, Earth rune pack, Mind rune pack, Chaos rune pack)
        add(specialist("auburys_rune_shop", "Aubury's Rune Shop", "AUBURY").prices(1000, 600, 1).sell("air_rune", 5000)
                .sell("fire_rune", 5000).sell("water_rune", 5000).sell("earth_rune", 5000).sell("mind_rune", 5000)
                .sell("body_rune", 5000).sell("chaos_rune", 250).sell("death_rune", 250)
                .greeting("Do you want to buy some runes?"));
        // OSRS Wiki: Zaff's Superior Staffs!
        add(specialist("zaffs_superior_staffs", "Zaff's Superior Staffs!", "npc_zaff").prices(1000, 550, 20)
                .sell("battlestaff", 5).sell("staff", 5).sell("magic_staff", 5).sell("staff_of_air", 2)
                .sell("staff_of_water", 2).sell("staff_of_earth", 2).sell("staff_of_fire", 2)
                .greeting("Would you like to buy or sell some staffs?"));
        // OSRS Wiki: Horvik's Armour Shop
        add(specialist("horviks_armour_shop", "Horvik's Armour Shop", "npc_horvik").sell("bronze_chainbody", 5)
                .sell("iron_chainbody", 3).sell("steel_chainbody", 3).sell("mithril_chainbody", 1).sell("bronze_platebody", 3)
                .sell("iron_platebody", 1).sell("steel_platebody", 1).sell("black_platebody", 1).sell("mithril_platebody", 1)
                .sell("iron_platelegs", 1).sell("studded_body", 1).sell("studded_chaps", 1)
                .greeting("Hello, do you need any help?"));
        // OSRS Wiki: Lowe's Archery Emporium (not in RuneCraft: Crossbow)
        add(specialist("lowes_archery_emporium", "Lowe's Archery Emporium", "npc_lowe").prices(1000, 550, 10)
                .sell("bronze_arrow", 2000).sell("iron_arrow", 1500).sell("steel_arrow", 1000).sell("mithril_arrow", 800)
                .sell("adamant_arrow", 600).sell("bronze_bolts", 1500).sell("shortbow", 4).sell("longbow", 4)
                .sell("oak_shortbow", 3).sell("oak_longbow", 3).sell("willow_shortbow", 2).sell("willow_longbow", 2)
                .sell("maple_shortbow", 1).sell("maple_longbow", 1)
                .greeting("Welcome to Lowe's Archery Emporium. Do you want to see my wares?"));
        // OSRS Wiki: Varrock Swordshop
        add(specialist("varrock_swordshop", "Varrock Swordshop", "npc_varrock_sword_shopkeeper").sell("bronze_sword", 5)
                .sell("iron_sword", 4).sell("steel_sword", 4).sell("black_sword", 3).sell("mithril_sword", 3)
                .sell("adamant_sword", 2).sell("bronze_longsword", 4).sell("iron_longsword", 3).sell("steel_longsword", 3)
                .sell("black_longsword", 2).sell("mithril_longsword", 2).sell("adamant_longsword", 1).sell("bronze_dagger", 10)
                .sell("iron_dagger", 6).sell("steel_dagger", 5).sell("black_dagger", 4).sell("mithril_dagger", 3)
                .sell("adamant_dagger", 2).greeting("Hello, adventurer. Can I interest you in some swords?"));
        // OSRS Wiki: Thessalia's Fine Clothes
        add(specialist("thessalias_fine_clothes", "Thessalia's Fine Clothes", "npc_thessalia").prices(1000, 550, 10)
                .sell("white_apron", 3).sell("leather_body", 12).sell("leather_gloves", 10).sell("leather_boots", 10)
                .sell("brown_apron", 1).sell("pink_skirt", 5).sell("black_skirt", 3).sell("blue_skirt", 2).sell("silk", 5)
                .sell("priest_gown_bottom", 3).sell("priest_gown_top", 3).greeting("Would you like to buy any fine clothes?"));
        // OSRS Wiki: Ye olde Tea Shoppe
        add(specialist("varrock_tea_stall", "Tea Stall", "npc_tea_seller").sell("cup_of_tea", 20)
                .greeting("Hot tea! Get your hot tea here!"));
        // OSRS Wiki: Helmet Shop
        add(specialist("peksas_helmet_shop", "Helmet Shop", "npc_peksa").prices(1000, 600, 10).sell("bronze_med_helm", 5)
                .sell("iron_med_helm", 3).sell("steel_med_helm", 3).sell("mithril_med_helm", 1).sell("adamant_med_helm", 1)
                .sell("bronze_full_helm", 4).sell("iron_full_helm", 3).sell("steel_full_helm", 2).sell("mithril_full_helm", 1)
                .sell("adamant_full_helm", 1).greeting("Are you interested in buying or selling a helmet?"));
        // OSRS Wiki: Wine Shop (not in RuneCraft: Empty jug pack, Bottle of wine, Jug of vinegar)
        add(specialist("fortunatos_fine_wine", "Fortunato's Fine Wine", "npc_fortunato").sell("jug_of_wine", 5).sell("jug", 3)
                .greeting("Can I help you at all?"));
        // OSRS Wiki: Draynor Seed Market (not in RuneCraft: Rosemary seed, Marigold seed)
        add(specialist("draynor_seed_market", "Draynor Seed Market", "npc_olivia").prices(1200, 600, 30).sell("potato_seed", 20)
                .sell("onion_seed", 10).sell("cabbage_seed", 5).sell("tomato_seed", 0).sell("sweetcorn_seed", 0)
                .sell("strawberry_seed", 0).sell("watermelon_seed", 0).sell("barley_seed", 20).sell("jute_seed", 5)
                .sell("hammerstone_seed", 20).sell("asgarnian_seed", 10).sell("yanillian_seed", 5).sell("krandorian_seed", 0)
                .sell("wildblood_seed", 0).greeting("Would you like to trade in seeds?"));

        // ------------------------------------------------------------- inns (bartenders)
        add(specialist("blue_moon_inn", "Blue Moon Inn", "npc_blue_moon_bartender")
                .sell("beer", 10).sell("asgarnian_ale", 10).sell("wizards_mind_bomb", 10, 3).sell("dwarven_stout", 10)
                .greeting("What can I get you?"));
        add(specialist("jolly_boar_inn", "Jolly Boar Inn", "npc_jolly_boar_bartender")
                .sell("beer", 10).greeting("Can I help you?"));
        add(specialist("rusty_anchor_inn", "Rusty Anchor Inn", "npc_rusty_anchor_bartender")
                .sell("beer", 10).sell("asgarnian_ale", 10).sell("wizards_mind_bomb", 10, 3).sell("dwarven_stout", 10)
                .greeting("Yes? What would you like?"));
        add(specialist("dragon_inn", "Dragon Inn", "npc_dragon_inn_bartender")
                .sell("beer", 10).sell("dwarven_stout", 10).greeting("What can I get you?"));
        add(specialist("foresters_arms", "Forester's Arms", "npc_foresters_arms_bartender")
                .sell("beer", 10).sell("cider", 10).greeting("Good day to you! What can I get you?"));
        add(specialist("flying_horse_inn", "Flying Horse Inn", "npc_flying_horse_bartender")
                .sell("beer", 10).greeting("Would you like to buy a drink?"));
        // OSRS Wiki: Karamja Wines, Spirits, and Beers
        add(specialist("karamja_spirits_bar", "Karamja Spirits Bar", "zambo").prices(1000, 700, 20).sell("beer", 3)
                .sell("karamjan_rum", 3).sell("jug_of_wine", 1)
                .greeting("Hey, are you wanting to try some of my fine wines and spirits?"));

        // ------------------------------------------------------------- Port Sarim, Rimmington
        // OSRS Wiki: Gerrant's Fishy Business (not in RuneCraft: Bait pack, Feather pack)
        add(specialist("gerrants_fishy_business", "Gerrant's Fishy Business", "npc_gerrant").prices(1000, 700, 10)
                .sell("small_fishing_net", 5).sell("fishing_rod", 5).sell("fly_fishing_rod", 5).sell("harpoon", 2)
                .sell("lobster_pot", 2).sell("fishing_bait", 1500).sell("minecraft:feather", 1000, 2).sell("raw_shrimps", 0)
                .sell("raw_sardine", 200).sell("raw_herring", 0).sell("raw_anchovies", 0).sell("raw_trout", 0)
                .sell("raw_pike", 0).sell("raw_salmon", 0).sell("raw_tuna", 0).sell("raw_lobster", 0).sell("raw_swordfish", 0)
                .greeting("Welcome! You can buy fishing equipment at my store."));
        // OSRS Wiki: Betty's Magic Emporium (not in RuneCraft: Fire rune pack, Water rune pack, Air rune pack, Earth rune pack, Mind rune pack, Chaos rune pack, Eye of newt pack)
        add(specialist("bettys_magic_emporium", "Betty's Magic Emporium", "npc_betty").prices(1000, 600, 1)
                .sell("fire_rune", 5000).sell("water_rune", 5000).sell("air_rune", 5000).sell("earth_rune", 5000)
                .sell("mind_rune", 5000).sell("body_rune", 5000).sell("chaos_rune", 250).sell("death_rune", 250)
                .sell("eye_of_newt", 300).sell("wizard_hat", 1).sell("blue_wizard_hat", 1).greeting("Welcome to the magic emporium."));
        // OSRS Wiki: Food Store
        add(specialist("wydins_food_store", "Wydin's Food Store", "npc_wydin").prices(1000, 700, 10).sell("pot_of_flour", 3)
                .sell("minecraft:beef", 1, 1).sell("minecraft:chicken", 1, 1).sell("cabbage", 3).sell("banana", 3)
                .sell("redberries", 1).sell("bread", 0).sell("chocolate_bar", 1).sell("cheese", 3).sell("tomato", 3)
                .sell("potato", 1).greeting("Welcome to my food store! Would you like to buy anything?"));
        // OSRS Wiki: Brian's Battleaxe Bazaar
        add(specialist("brians_battleaxe_bazaar", "Brian's Battleaxe Bazaar", "npc_brian").prices(1000, 550, 10)
                .sell("bronze_battleaxe", 4).sell("iron_battleaxe", 3).sell("steel_battleaxe", 2).sell("black_battleaxe", 1)
                .sell("mithril_battleaxe", 1).sell("adamant_battleaxe", 1).greeting("ello"));
        // OSRS Wiki: Grum's Gold Exchange
        add(specialist("grums_gold_exchange", "Grum's Gold Exchange", "npc_grum").prices(1000, 700, 20).sell("gold_ring", 0)
                .sell("sapphire_ring", 0).sell("emerald_ring", 0).sell("ruby_ring", 0).sell("diamond_ring", 0)
                .sell("gold_necklace", 0).sell("sapphire_necklace", 0).sell("emerald_necklace", 0).sell("ruby_necklace", 0)
                .sell("diamond_necklace", 0).sell("gold_amulet", 0).sell("sapphire_amulet", 0).sell("emerald_amulet", 0)
                .sell("ruby_amulet", 0).sell("diamond_amulet", 0).greeting("Would you like to buy or sell some gold jewellery?"));
        // OSRS Wiki: Brian's Archery Supplies
        add(specialist("brians_archery_supplies", "Brian's Archery Supplies", "npc_brian_archery").prices(1000, 650, 20)
                .sell("steel_arrow", 1500).sell("mithril_arrow", 1000).sell("adamant_arrow", 800).sell("oak_shortbow", 4)
                .sell("oak_longbow", 4).sell("willow_shortbow", 3).sell("willow_longbow", 3).sell("maple_shortbow", 2)
                .sell("maple_longbow", 2).greeting("Would you like to buy some archery equipment?"));

        // ------------------------------------------------------------- Falador, Taverley, Dwarven Mine
        // OSRS Wiki: Wayne's Chains! - Chainmail specialist
        add(specialist("waynes_chains", "Wayne's Chains - Chainmail Specialist", "npc_wayne").prices(1000, 650, 10)
                .sell("bronze_chainbody", 3).sell("iron_chainbody", 2).sell("steel_chainbody", 1).sell("black_chainbody", 1)
                .sell("mithril_chainbody", 1).sell("adamant_chainbody", 1)
                .greeting("Welcome to Wayne's Chains. Do you wanna buy or sell some chain mail?"));
        // OSRS Wiki: Cassie's Shield Shop
        add(specialist("cassies_shield_shop", "Cassie's Shield Shop", "npc_cassie").sell("wooden_shield", 5)
                .sell("bronze_sq_shield", 3).sell("bronze_kiteshield", 3).sell("iron_sq_shield", 2).sell("iron_kiteshield", 0)
                .sell("steel_sq_shield", 0).sell("steel_kiteshield", 0).sell("mithril_sq_shield", 0)
                .greeting("I buy and sell shields. Do you want to trade?"));
        // OSRS Wiki: Herquin's Gems
        add(specialist("herquins_gems", "Herquin's Gems", "npc_herquin").prices(1000, 700, 30).sell("uncut_sapphire", 1)
                .sell("uncut_emerald", 0).sell("uncut_ruby", 0).sell("uncut_diamond", 0).sell("sapphire", 1).sell("emerald", 0)
                .sell("ruby", 0).sell("diamond", 0).greeting("Do you wish to trade? I have the finest gems."));
        // OSRS Wiki: Flynn's Mace Market
        add(specialist("flynns_mace_market", "Flynn's Mace Market", "npc_flynn").prices(1000, 600, 10).sell("bronze_mace", 5)
                .sell("iron_mace", 4).sell("steel_mace", 4).sell("mithril_mace", 3).sell("adamant_mace", 2)
                .greeting("Hello. Do you want to buy or sell any maces?"));
        // OSRS Wiki: Gaius' Two Handed Shop
        add(specialist("gaius_two_handed_shop", "Gaius' Two-Handed Shop", "npc_gaius").sell("bronze_2h_sword", 4)
                .sell("iron_2h_sword", 3).sell("steel_2h_sword", 2).sell("black_2h_sword", 1).sell("mithril_2h_sword", 1)
                .sell("adamant_2h_sword", 1).greeting("Welcome to my two-handed sword shop."));
        // OSRS Wiki: Jatix's Herblore Shop (not in RuneCraft: Empty vial pack, Water-filled vial pack, Eye of newt pack)
        add(specialist("jatix_herblore_shop", "Jatix's Herblore Shop", "npc_jatix").prices(1000, 700, 30).sell("vial", 800)
                .sell("vial_of_water", 750).sell("pestle_and_mortar", 3).sell("eye_of_newt", 800)
                .greeting("Hello, how can I help you?"));
        // OSRS Wiki: Nurmof's Pickaxe Shop
        add(specialist("nurmofs_pickaxe_shop", "Nurmof's Pickaxe Shop", "npc_nurmof").sell("bronze_pickaxe", 6)
                .sell("iron_pickaxe", 5).sell("steel_pickaxe", 4).sell("mithril_pickaxe", 3).sell("adamant_pickaxe", 2)
                .sell("rune_pickaxe", 1).greeting("Greetings, stranger! Can I interest you in a pickaxe?"));
        // OSRS Wiki: Drogo's Mining Emporium
        add(specialist("drogos_mining_emporium", "Drogo's Mining Emporium", "npc_drogo").prices(1000, 700, 20).sell("hammer", 4)
                .sell("bronze_pickaxe", 4).sell("copper_ore", 0).sell("tin_ore", 0).sell("iron_ore", 0).sell("coal", 0)
                .sell("bronze_bar", 0).sell("iron_bar", 0).sell("gold_bar", 0)
                .greeting("'Ello. Welcome to my Mining shop, friend."));

        // ------------------------------------------------------------- Al Kharid, desert
        // OSRS Wiki: Zeke's Superior Scimitars
        add(specialist("zekes_superior_scimitars", "Zeke's Superior Scimitars", "npc_zeke").prices(1000, 550, 20)
                .sell("bronze_scimitar", 5).sell("iron_scimitar", 3).sell("steel_scimitar", 2).sell("mithril_scimitar", 1)
                .greeting("A thousand greetings, sir."));
        // OSRS Wiki: Louie's Armoured Legs Bazaar
        add(specialist("louies_armoured_legs_bazaar", "Louie's Armoured Legs Bazaar", "npc_louie_legs").prices(1000, 650, 10)
                .sell("bronze_platelegs", 5).sell("iron_platelegs", 3).sell("steel_platelegs", 2).sell("black_platelegs", 1)
                .sell("mithril_platelegs", 1).sell("adamant_platelegs", 1).greeting("Hey, wanna buy some armour?"));
        // OSRS Wiki: Ranael's Super Skirt Store
        add(specialist("ranaels_super_skirt_store", "Ranael's Super Skirt Store", "npc_ranael").prices(1000, 650, 10)
                .sell("bronze_plateskirt", 5).sell("iron_plateskirt", 3).sell("steel_plateskirt", 2).sell("black_plateskirt", 1)
                .sell("mithril_plateskirt", 1).sell("adamant_plateskirt", 1)
                .greeting("Do you want to buy any armoured skirts? Designed especially for ladies who like to fight."));
        // OSRS Wiki: Dommik's Crafting Store
        add(specialist("dommiks_crafting_store", "Dommik's Crafting Store", "npc_dommik").prices(1000, 650, 20)
                .sell("chisel", 2).sell("ring_mould", 4).sell("necklace_mould", 2).sell("amulet_mould", 2).sell("needle", 3)
                .sell("thread", 100).sell("holy_mould", 3).sell("sickle_mould", 6).sell("tiara_mould", 10)
                .sell("bolt_mould", 10).sell("bracelet_mould", 5).greeting("Would you like to buy some crafting equipment?"));
        // OSRS Wiki: Gem Trader
        add(specialist("al_kharid_gem_trader", "Gem Trader", "npc_gem_trader").prices(1000, 700, 30).sell("uncut_sapphire", 1)
                .sell("uncut_emerald", 1).sell("uncut_ruby", 0).sell("uncut_diamond", 0).sell("sapphire", 1).sell("emerald", 1)
                .sell("ruby", 0).sell("diamond", 0)
                .greeting("Good day to you, traveller. Would you be interested in buying some gems?"));
        add(specialist("karims_kebabs", "Kebab Shop", "npc_karim")
                .sell("kebab", 10).greeting("Would you like to buy a nice kebab? Only one gold."));
        // OSRS Wiki: Shantay Pass Shop (not in RuneCraft: Waterskin(0), Feather pack, Empty bucket pack, Empty jug pack)
        add(specialist("shantay_pass_shop", "Shantay Pass Shop", "npc_shantay").prices(1000, 700, 10).sell("waterskin_4", 100)
                .sell("jug_of_water", 10).sell("bowl_of_water", 10).sell("minecraft:water_bucket", 10, 6).sell("knife", 10)
                .sell("desert_shirt", 10).sell("desert_robe", 10).sell("desert_boots", 10).sell("bronze_bar", 10)
                .sell("minecraft:feather", 500, 2).sell("hammer", 10).sell("minecraft:bucket", 0, 2).sell("bowl", 0)
                .sell("jug", 0).sell("shantay_pass_ticket", 500).sell("rope", 20)
                .greeting("Hello effendi, I am Shantay. I see you're new. Make sure you read the poster before going into the desert."));
        // OSRS Wiki: Rok's Chocs Box
        add(specialist("rokuhs_choc_ice_emporium", "Rokuh's Choc-Ice Emporium", "npc_rokuh").prices(1000, 650, 20)
                .sell("choc_ice", 30).sell("chocolate_bar", 25)
                .greeting("Choc-ice! Get your choc-ice here! Best way to cool off in the desert!"));
        // OSRS Wiki: Seddu's Adventurer's Store
        add(specialist("seddus_adventurers_store", "Seddu's Adventurers' Store", "npc_seddu").prices(950, 650, 20)
                .sell("rune_plateskirt", 1).sell("rune_platelegs", 1).sell("rune_chainbody", 1).sell("green_dhide_chaps", 1)
                .sell("green_dhide_vambraces", 1).sell("steel_kiteshield", 1).sell("black_med_helm", 1)
                .greeting("Hello, would you like to see what I have for sale?"));

        // ------------------------------------------------------------- Kandarin
        // OSRS Wiki: Harry's Fishing Shop (not in RuneCraft: Bait pack)
        add(specialist("harrys_fishing_shop", "Harry's Fishing Shop", "npc_harry").prices(1000, 700, 10)
                .sell("small_fishing_net", 5).sell("fishing_rod", 5).sell("harpoon", 2).sell("lobster_pot", 2)
                .sell("fishing_bait", 1200).sell("big_fishing_net", 5).sell("raw_shrimps", 0).sell("raw_sardine", 0)
                .sell("raw_herring", 0).sell("raw_mackerel", 0).sell("raw_cod", 0).sell("raw_anchovies", 0).sell("raw_tuna", 0)
                .sell("raw_lobster", 0).sell("raw_bass", 0).sell("raw_swordfish", 0).sell("raw_shark", 0)
                .greeting("Welcome! You can buy fishing equipment at my store."));
        // OSRS Wiki: Hickton's Archery Emporium (not in RuneCraft: Bronze brutal, Iron brutal, Steel brutal, Black brutal, Mithril brutal, Adamant brutal, Rune brutal, Crossbow, Comp ogre bow)
        add(specialist("hicktons_archery_emporium", "Hickton's Archery Emporium", "npc_hickton").prices(1000, 500, 10)
                .sell("bronze_bolts", 200).sell("bronze_arrow", 1000).sell("iron_arrow", 800).sell("steel_arrow", 0)
                .sell("mithril_arrow", 0).sell("adamant_arrow", 0).sell("rune_arrow", 0).sell("bronze_arrowtips", 1000)
                .sell("iron_arrowtips", 800).sell("steel_arrowtips", 600).sell("mithril_arrowtips", 400)
                .sell("adamant_arrowtips", 200).sell("rune_arrowtips", 100).sell("shortbow", 4).sell("longbow", 2)
                .sell("oak_shortbow", 4).sell("oak_longbow", 4).sell("studded_body", 2).sell("studded_chaps", 2)
                .greeting("Welcome to Hickton's Archery Emporium. Do you want to see my wares?"));
        // OSRS Wiki: Candle Shop
        add(specialist("candle_shop", "Candle Shop", "candle_maker").prices(1300, 400, 30).sell("candle", 10)
                .greeting("Hi! Would you like a candle?"));
        // OSRS Wiki: Vanessa's Farming shop (not in RuneCraft: Plant pot pack, Compost pack, Sack pack, Basket, Basket pack, Empty bucket pack)
        add(specialist("vanessas_farming_shop", "Vanessa's Farming Shop", "npc_vanessa").prices(1000, 700, 10).sell("rake", 500)
                .sell("seed_dibber", 500).sell("secateurs", 500).sell("spade", 500).sell("gardening_trowel", 500)
                .sell("watering_can", 500, 8).sell("filled_plant_pot", 500).sell("compost", 500).sell("empty_sack", 500)
                .sell("minecraft:bucket", 100, 2).sell("potato", 0).sell("onion", 0).sell("cabbage", 0).sell("tomato", 0)
                .sell("sweetcorn", 0).sell("strawberry", 0).sell("watermelon", 0).sell("hammerstone_hops", 0)
                .sell("asgarnian_hops", 0).sell("yanillian_hops", 0).sell("krandorian_hops", 0).sell("wildblood_hops", 0)
                .sell("jute_fibre", 0).sell("barley", 0).sell("plant_cure", 100).greeting("Hello. I sell farming equipment."));
        // OSRS Wiki: Aemad's Adventuring Supplies (not in RuneCraft: Water-filled vial pack, Papyrus)
        add(specialist("aemads_adventuring_supplies", "Aemad's Adventuring Supplies", "npc_aemad").prices(1300, 400, 20)
                .sell("vial_of_water", 500).sell("bronze_pickaxe", 2).sell("iron_axe", 2).sell("minecraft:cooked_beef", 2, 4)
                .sell("tinderbox", 2).sell("ball_of_wool", 30).sell("bronze_arrow", 500).sell("rope", 20).sell("knife", 2)
                .sell("wrath_talisman", 1).sell("raw_cave_eel", 100).greeting("Hello, adventurer. Are you here to stock up?"));
        // OSRS Wiki: Zenesha's Plate Mail Body Shop
        add(specialist("zeneshas_plate_mail", "Zenesha's Plate Mail Body Shop", "npc_zenesha").sell("bronze_platebody", 3)
                .sell("iron_platebody", 1).sell("steel_platebody", 1).sell("black_platebody", 1).sell("mithril_platebody", 1)
                .greeting("Hello. Would you like to buy a platebody?"));
        // OSRS Wiki: Ardougne Baker's Stall
        add(specialist("ardougne_baker", "Baker's Stall", "npc_ardougne_baker").prices(1000, 800, 20).sell("bread", 10)
                .sell("cake", 3).sell("chocolate_slice", 6).sell("chocolate_bar", 9)
                .greeting("Fresh bread, cakes and chocolate slices!"));
        add(specialist("ardougne_silk_trader", "Silk Merchant", "npc_ardougne_silk_merchant")
                .sell("silk", 10).greeting("Silk! The finest silk in Ardougne!"));
        // OSRS Wiki: Ardougne Fur Stall (not in RuneCraft: Tatty larupia fur, Tatty graahk fur, Tatty kyatt fur)
        add(specialist("ardougne_fur_trader", "Fur Trader", "npc_ardougne_fur_trader").prices(1200, 950, 20).sell("bear_fur", 3)
                .sell("grey_wolf_fur", 3).sell("polar_kebbit_fur", 0).sell("common_kebbit_fur", 0).sell("feldip_weasel_fur", 0)
                .sell("desert_devil_fur", 0).sell("larupia_fur", 0).sell("graahk_fur", 0).sell("kyatt_fur", 0)
                .greeting("Would you like to buy some fur?"));
        // OSRS Wiki: Ardougne Silver Stall
        add(specialist("ardougne_silver_merchant", "Silver Merchant", "npc_ardougne_silver_merchant").prices(1000, 700, 20)
                .sell("unstrung_symbol", 2).sell("silver_ore", 1).sell("silver_bar", 1)
                .greeting("Silver! I sell the best silver in Ardougne."));
        // OSRS Wiki: Ardougne Spice Stall
        add(specialist("ardougne_spice_seller", "Spice Merchant", "npc_ardougne_spice_seller").sell("spice", 1).sell("knife", 1)
                .sell("garlic", 2).greeting("Spice! Exotic spice from the desert!"));
        // OSRS Wiki: Ardougne Gem Stall
        add(specialist("ardougne_gem_merchant", "Gem Merchant", "npc_ardougne_gem_merchant").prices(1500, 800, 30)
                .sell("sapphire", 2).sell("emerald", 1).sell("ruby", 1).sell("diamond", 0)
                .greeting("Here, look at my lovely gems."));
        // OSRS Wiki: Davon's Amulet Store
        add(specialist("davons_amulet_store", "Davon's Amulet Store", "npc_davon").prices(1200, 900, 20).sell("holy_symbol", 0)
                .sell("amulet_of_magic", 0).sell("amulet_of_defence", 0).sell("amulet_of_strength", 0)
                .sell("amulet_of_power", 0).greeting("Pssst! Come here if you want to do some amulet trading."));
        // OSRS Wiki: Frenita's Cookery Shop (not in RuneCraft: Empty jug pack)
        add(specialist("frenitas_cookery_shop", "Frenita's Cookery Shop", "npc_frenita").prices(1000, 550, 10)
                .sell("pie_dish", 5).sell("cooking_apple", 2).sell("cake_tin", 2).sell("bowl", 2).sell("potato", 5)
                .sell("tinderbox", 4).sell("jug", 1).sell("empty_pot", 8).sell("chocolate_bar", 2).sell("pot_of_flour", 8)
                .sell("empty_cup", 20).greeting("Would you like to buy some cooking equipment?"));
        // OSRS Wiki: Aleck's Hunter Emporium (not in RuneCraft: Magic box, Unlit torch, Bird snare pack, Box trap pack, Magic imp box pack)
        add(specialist("alecks_hunter_emporium", "Aleck's Hunter Emporium", "npc_aleck").prices(1200, 700, 20)
                .sell("butterfly_net", 5).sell("butterfly_jar", 100).sell("noose_wand", 50).sell("bird_snare", 50)
                .sell("box_trap", 25).sell("teasing_stick", 5).sell("rabbit_snare", 10)
                .greeting("Welcome to my Hunter Emporium. Can I interest you in some hunting gear?"));
        // OSRS Wiki: Gulluck and Sons (not in RuneCraft: Pearl bolts, Crossbow)
        add(specialist("gulluck_and_sons", "Gulluck and Sons", "npc_gulluck").prices(1000, 250, 10).sell("bronze_arrow", 200)
                .sell("bronze_bolts", 150).sell("shortbow", 4).sell("longbow", 2).sell("bronze_arrowtips", 200)
                .sell("iron_arrowtips", 180).sell("steel_arrowtips", 160).sell("mithril_arrowtips", 140).sell("iron_axe", 5)
                .sell("steel_axe", 3).sell("iron_battleaxe", 5).sell("steel_battleaxe", 2).sell("mithril_battleaxe", 1)
                .sell("bronze_2h_sword", 4).sell("iron_2h_sword", 3).sell("steel_2h_sword", 2).sell("black_2h_sword", 1)
                .sell("mithril_2h_sword", 1).sell("adamant_2h_sword", 1).greeting("Hello, human. We sell all sorts of weapons."));
        // OSRS Wiki: Grand Tree Groceries (not in RuneCraft: Spider on stick, Spider on shaft, Bucket of milk, Gianne's cook book)
        // plus Funch's Fine Groceries' spirits and cocktail glasses (Heckel Funch runs both here). Approx: king worms are a
        // ground spawn in the Gnome Stronghold in OSRS.
        add(specialist("grand_tree_groceries", "Grand Tree Groceries", "npc_heckel_funch").prices(1000, 550, 10)
                .sell("gianne_dough", 50).sell("equa_leaves", 20).sell("pot_of_flour", 10).sell("gnome_spice", 10)
                .sell("onion", 10).sell("potato", 10).sell("cabbage", 10).sell("tomato", 10).sell("cheese", 10).sell("lime", 10)
                .sell("orange", 10).sell("lemon", 10).sell("pineapple", 10).sell("dwellberries", 10).sell("cocktail_shaker", 10)
                .sell("chocolate_bar", 20).sell("chocolate_dust", 10).sell("pot_of_cream", 5).sell("knife", 5)
                .sell("batta_tin", 5).sell("crunchy_tray", 5).sell("gnomebowl_mould", 5).sell("brandy", 10).sell("gin", 10)
                .sell("vodka", 10).sell("whisky", 10).sell("cocktail_glass", 20).sell("king_worm", 20)
                .greeting("Good day to you. Are you after some fine gnome produce?"));

        // ------------------------------------------------------------- Karamja
        // Solihib's Food Stall, Ape Atoll (keeper defined by the skilling module; stock approx.): monkey nuts
        // and monkey bars for the spirit tree protection fee.
        add(specialist("solihibs_food_stall", "Solihib's Food Stall", "solihib").prices(1000, 600, 10).sell("banana", 10)
                .sell("monkey_nuts", 10).sell("monkey_bar", 5)
                .greeting("Ook! Food for sale, the finest on Ape Atoll."));
        // OSRS Wiki: Fernahei's Fishing Hut (not in RuneCraft: Bait pack, Feather pack)
        add(specialist("fernaheis_fishing_hut", "Fernahei's Fishing Hut", "npc_fernahei").prices(1000, 700, 10)
                .sell("fishing_rod", 5).sell("fly_fishing_rod", 5).sell("fishing_bait", 200).sell("minecraft:feather", 800, 2)
                .sell("raw_trout", 0).sell("raw_pike", 0).sell("raw_salmon", 0)
                .greeting("Welcome to Fernahei's Fishing Hut, Bwana! Would you like to see my items?"));

        // ------------------------------------------------------------- Fremennik Province
        // OSRS Wiki: Fremennik Fish Monger (not in RuneCraft: Bait pack, Feather pack)
        add(specialist("fremennik_fish_monger", "Fremennik Fish Monger", "npc_fremennik_fishmonger").prices(1300, 700, 30)
                .sell("small_fishing_net", 5).sell("fishing_rod", 5).sell("fly_fishing_rod", 5).sell("harpoon", 2)
                .sell("lobster_pot", 2).sell("fishing_bait", 1500).sell("minecraft:feather", 1000, 2).sell("big_fishing_net", 5)
                .sell("raw_shrimps", 0).sell("raw_sardine", 200).sell("raw_herring", 0).sell("raw_mackerel", 0)
                .sell("raw_cod", 0).sell("raw_anchovies", 0).sell("raw_trout", 0).sell("raw_pike", 0).sell("raw_salmon", 0)
                .sell("raw_tuna", 0).sell("raw_lobster", 0).sell("raw_bass", 0).sell("raw_swordfish", 0).sell("raw_shark", 0)
                .greeting("Fresh fish! Caught this very morning."));
        // OSRS Wiki: Fremennik Fur Trader (not in RuneCraft: Tatty larupia fur, Tatty graahk fur, Tatty kyatt fur)
        add(specialist("fremennik_fur_trader", "Fremennik Fur Trader", "npc_fremennik_fur_trader").prices(1200, 950, 20)
                .sell("bear_fur", 3).sell("grey_wolf_fur", 3).sell("polar_kebbit_fur", 0).sell("common_kebbit_fur", 0)
                .sell("feldip_weasel_fur", 0).sell("desert_devil_fur", 0).sell("larupia_fur", 0).sell("graahk_fur", 0)
                .sell("kyatt_fur", 0).greeting("Furs! Warm furs for the Fremennik winter!"));

        // ------------------------------------------------------------- Morytania
        // OSRS Wiki: Rufus' Meat Emporium (not in RuneCraft: Raw rat meat, Raw bear meat)
        add(specialist("rufus_meat_emporium", "Rufus' Meat Emporium", "npc_rufus").prices(1300, 700, 30)
                .sell("minecraft:beef", 10, 1).sell("minecraft:chicken", 10, 1).sell("raw_trout", 5).sell("raw_pike", 5)
                .sell("raw_salmon", 5).sell("raw_shark", 1)
                .greeting("Grrr... meat! Fresh meat for sale! Raw, just how you like it."));
        // OSRS Wiki: Barkers' Haberdashery; stock approx. (not in RuneCraft: Grey boots, Grey robe top, Grey robe bottoms, Grey hat, Grey gloves, Red boots, Red robe top, Red robe bottoms, Red hat, Red gloves, Yellow boots, Yellow robe top, Yellow robe bottoms, Yellow hat, Yellow gloves, Teal boots, Teal robe top, Teal robe bottoms, Teal hat, Teal gloves, Purple boots, Purple robe top, Purple robe bottoms, Purple hat, Purple gloves, Blue cape, Yellow cape, Green cape)
        add(specialist("barkers_haberdashery", "Barker's Haberdashery", "npc_barker").prices(1300, 400, 30)
                .sell("leather_gloves", 5).sell("leather_boots", 5).sell("black_skirt", 3).sell("blue_skirt", 3)
                .sell("red_cape", 3).sell("black_cape", 3).greeting("Grrr... hello, would you like to buy some clothes?"));

        // ------------------------------------------------------------- Lletya
        // OSRS Wiki: Lletya Food Store
        add(specialist("lletya_food_store", "Lletya Food Store", "npc_lletya_food_seller").prices(1300, 400, 30)
                .sell("bread", 10).sell("lobster", 15).sell("jug_of_wine", 3).sell("cheese", 10).sell("cake", 5)
                .greeting("Would you like to buy some food?"));
        // OSRS Wiki: Lletya Archery Shop (not in RuneCraft: Crossbow)
        add(specialist("lletya_archery_shop", "Lletya Archery Shop", "npc_lletya_fletcher").prices(1300, 400, 30)
                .sell("iron_arrow", 2000).sell("steel_arrow", 500).sell("mithril_arrow", 500).sell("adamant_arrow", 450)
                .sell("rune_arrow", 400).sell("bronze_bolts", 1500).sell("oak_shortbow", 5).sell("oak_longbow", 5)
                .sell("willow_shortbow", 5).sell("willow_longbow", 5)
                .greeting("Welcome, traveller. Can I interest you in the finest bows in Tirannwn?"));

        // ------------------------------------------------------------- Wilderness, Mage Arena
        // OSRS Wiki: Lundail's Arena-side Rune Shop (not in RuneCraft: Fire rune pack, Water rune pack, Air rune pack, Earth rune pack, Mind rune pack, Chaos rune pack)
        add(specialist("lundails_rune_shop", "Lundail's Arena-side Rune Shop", "npc_lundail").prices(1000, 600, 1)
                .sell("fire_rune", 200).sell("water_rune", 200).sell("air_rune", 200).sell("earth_rune", 200)
                .sell("mind_rune", 140).sell("body_rune", 140).sell("nature_rune", 250).sell("chaos_rune", 250)
                .sell("law_rune", 250).sell("cosmic_rune", 20).sell("death_rune", 250)
                .greeting("Can I help you, adventurer? Runes for the battlefield?"));
        add(specialist("mage_arena_staffs", "Mage Arena Staffs", "npc_chamber_guardian")
                .sell("saradomin_staff", 5).sell("guthix_staff", 5).sell("zamorak_staff", 5)
                .greeting("Greetings. I can supply the staves of the gods to those who have proven themselves."));
    }
}
