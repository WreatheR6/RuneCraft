package com.runecraft.osrs.content.npcs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.runecraft.osrs.content.npcs.ShopDef.general;
import static com.runecraft.osrs.content.npcs.ShopDef.specialist;

/**
 * Every ordinary shop with its OSRS stock (normal stock amounts) and item values. Stock amounts the
 * wiki lists exactly are used as-is; a few small shops whose exact numbers aren't known are close
 * estimates and marked "approx.". The shopkeeper of each shop is an NPC in {@link Townsfolk} (or an
 * existing character, see {@link Townsfolk#EXTRA_TRADERS}).
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

    private static ShopDef.Builder runes(ShopDef.Builder b, int elemental) {
        return b.sell("fire_rune", elemental).sell("water_rune", elemental).sell("air_rune", elemental)
                .sell("earth_rune", elemental).sell("mind_rune", elemental).sell("body_rune", elemental);
    }

    static {
        // ------------------------------------------------------------- general stores
        add(standard(general("lumbridge_general_store", "Lumbridge General Store", "npc_lumbridge_shopkeeper")));
        add(standard(general("varrock_general_store", "Varrock General Store", "npc_varrock_shopkeeper")));
        add(standard(general("falador_general_store", "Falador General Store", "npc_falador_shopkeeper")));
        add(standard(general("edgeville_general_store", "Edgeville General Store", "npc_edgeville_shopkeeper")));
        add(standard(general("al_kharid_general_store", "Al Kharid General Store", "npc_al_kharid_shopkeeper")));
        add(standard(general("rimmington_general_store", "Rimmington General Store", "npc_rimmington_shopkeeper")));
        add(standard(general("karamja_general_store", "Karamja General Store", "npc_karamja_shopkeeper"))
                .sell("rope", 5).sell("fishing_bait", 50));
        add(standard(general("west_ardougne_general_store", "West Ardougne General Store", "npc_chadwell"))
                .greeting("What do you want?"));
        add(standard(general("port_khazard_general_store", "Port Khazard General Store", "npc_port_khazard_shopkeeper")));
        add(standard(general("canifis_general_store", "Canifis General Store", "npc_fidelio"))
                .sell("rope", 5).greeting("Welcome to my humble store. Please buy something, I need the money."));
        add(standard(general("port_phasmatys_general_store", "Port Phasmatys General Store", "npc_phasmatys_shopkeeper"))
                .sell("empty_candle_lantern", 5).greeting("Wooooo... welcome to my shop."));
        add(standard(general("nardah_general_store", "Kazemde's General Store", "npc_kazemde"))
                .sell("waterskin_4", 10));
        add(standard(general("pollnivneach_general_store", "Pollnivneach General Store", "npc_pollnivneach_shopkeeper"))
                .sell("waterskin_4", 10));
        add(standard(general("lletya_general_store", "Lletya General Store", "npc_lletya_shopkeeper"))
                .sell("rope", 5).greeting("Greetings, traveller. Can I interest you in anything?"));
        add(standard(general("gnome_general_store", "Gnome General Store", "npc_gnome_shopkeeper"))
                .greeting("Hello, human. Have a look at my wares."));
        add(standard(general("shilo_general_store", "Obli's General Store", "npc_obli"))
                .sell("machete", 10).sell("rope", 5).sell("candle", 10, 3).greeting("Welcome to Obli's General Store, Bwana!"));
        add(standard(general("bandit_camp_general_store", "Bandit Duty Free", "npc_bandit_shopkeeper"))
                .greeting("Er... what do you want?"));
        add(general("jiminuas_jungle_store", "Jiminua's Jungle Store", "npc_jiminua")
                .sell("empty_pot", 5).sell("jug", 3).sell("minecraft:shears", 2, 1).sell("minecraft:bucket", 2, 2).sell("bowl", 2)
                .sell("cake_tin", 2).sell("tinderbox", 4).sell("chisel", 2).sell("hammer", 5).sell("candle", 10, 3)
                .sell("rope", 10).sell("knife", 10).sell("machete", 10).sell("bronze_pickaxe", 10, 1).sell("spade", 5)
                .sell("empty_candle_lantern", 10).sell("empty_oil_lamp", 10).sell("vial", 50).sell("pestle_and_mortar", 5)
                .greeting("Welcome to the Jungle Store, can I help you at all?"));
        add(general("arheins_store", "Arhein's Store", "npc_arhein")
                .sell("minecraft:bucket", 10, 2).sell("bowl", 4).sell("cake_tin", 2).sell("tinderbox", 2).sell("chisel", 2)
                .sell("hammer", 10).sell("rope", 7).sell("empty_pot", 5).sell("jug", 2).sell("knife", 3).sell("spade", 5)
                .greeting("Hello! Would you like to trade?"));

        // ------------------------------------------------------------- Lumbridge, Draynor, Varrock
        add(specialist("bobs_brilliant_axes", "Bob's Brilliant Axes", "npc_bob")
                .sell("bronze_pickaxe", 5, 1).sell("bronze_axe", 10).sell("iron_axe", 5).sell("steel_axe", 3)
                .sell("iron_battleaxe", 5).sell("steel_battleaxe", 2).sell("mithril_battleaxe", 1)
                .greeting("Hello. How can I help you?"));
        add(runes(specialist("auburys_rune_shop", "Aubury's Rune Shop", "AUBURY"), 5000)
                .sell("chaos_rune", 250).sell("death_rune", 250).greeting("Do you want to buy some runes?"));
        add(specialist("zaffs_superior_staffs", "Zaff's Superior Staffs!", "npc_zaff")
                .sell("battlestaff", 5).sell("staff", 5).sell("magic_staff", 5).sell("staff_of_air", 2).sell("staff_of_water", 2)
                .sell("staff_of_earth", 2).sell("staff_of_fire", 2)
                .greeting("Would you like to buy or sell some staffs?"));
        add(specialist("horviks_armour_shop", "Horvik's Armour Shop", "npc_horvik")
                .sell("bronze_chainbody", 5).sell("iron_chainbody", 3).sell("steel_chainbody", 3).sell("mithril_chainbody", 1)
                .sell("bronze_platebody", 3).sell("iron_platebody", 1).sell("steel_platebody", 1).sell("black_platebody", 1)
                .sell("mithril_platebody", 1).sell("iron_platelegs", 1).sell("studded_body", 1).sell("studded_chaps", 1)
                .greeting("Hello, do you need any help?"));
        add(specialist("lowes_archery_emporium", "Lowe's Archery Emporium", "npc_lowe")
                .sell("bronze_arrow", 2000).sell("iron_arrow", 1500).sell("steel_arrow", 1000).sell("mithril_arrow", 800)
                .sell("shortbow", 4).sell("longbow", 4).sell("oak_shortbow", 3).sell("oak_longbow", 3)
                .sell("willow_shortbow", 2).sell("willow_longbow", 2).sell("maple_shortbow", 1).sell("maple_longbow", 1)
                .greeting("Welcome to Lowe's Archery Emporium. Do you want to see my wares?"));
        add(specialist("varrock_swordshop", "Varrock Swordshop", "npc_varrock_sword_shopkeeper")
                .sell("bronze_sword", 5).sell("iron_sword", 4).sell("steel_sword", 4).sell("black_sword", 3).sell("mithril_sword", 3)
                .sell("adamant_sword", 2).sell("bronze_longsword", 4).sell("iron_longsword", 3).sell("steel_longsword", 3)
                .sell("black_longsword", 2).sell("mithril_longsword", 2).sell("adamant_longsword", 1).sell("bronze_dagger", 10)
                .sell("iron_dagger", 6).sell("steel_dagger", 5).sell("black_dagger", 4).sell("mithril_dagger", 3)
                .sell("adamant_dagger", 2).greeting("Hello, adventurer. Can I interest you in some swords?"));
        add(specialist("thessalias_fine_clothes", "Thessalia's Fine Clothes", "npc_thessalia")
                .sell("white_apron", 3).sell("leather_body", 12).sell("leather_gloves", 10).sell("leather_boots", 10)
                .sell("brown_apron", 1).sell("chefs_hat", 2).sell("pink_skirt", 5, 2).sell("black_skirt", 3).sell("blue_skirt", 2).sell("red_cape", 4)
                .sell("silk", 5).sell("priest_gown_top", 3).sell("priest_gown_bottom", 3)
                .greeting("Would you like to buy any fine clothes?"));
        add(specialist("varrock_tea_stall", "Tea Stall", "npc_tea_seller")
                .sell("cup_of_tea", 20).greeting("Hot tea! Get your hot tea here!"));
        add(specialist("peksas_helmet_shop", "Helmet Shop", "npc_peksa")
                .sell("bronze_med_helm", 5).sell("iron_med_helm", 3).sell("steel_med_helm", 3).sell("mithril_med_helm", 2)
                .sell("adamant_med_helm", 1).sell("bronze_full_helm", 4).sell("iron_full_helm", 2).sell("steel_full_helm", 2)
                .sell("mithril_full_helm", 1).sell("adamant_full_helm", 1).greeting("Are you interested in buying or selling a helmet?"));
        add(specialist("fortunatos_fine_wine", "Fortunato's Fine Wine", "npc_fortunato")
                .sell("jug_of_wine", 6).sell("jug", 10).sell("grapes", 10).greeting("Can I help you at all?"));
        add(specialist("draynor_seed_market", "Draynor Seed Market", "npc_olivia")
                .sell("potato_seed", 100).sell("onion_seed", 100).sell("cabbage_seed", 100).sell("tomato_seed", 100)
                .sell("sweetcorn_seed", 100).sell("strawberry_seed", 100).sell("barley_seed", 100).sell("marrentill_seed", 0)
                .sell("hammerstone_seed", 100).sell("asgarnian_seed", 100).sell("jute_seed", 100).sell("yanillian_seed", 100)
                .sell("krandorian_seed", 0).sell("wildblood_seed", 0).sell("redberry_seed", 100).sell("cadavaberry_seed", 100)
                .sell("dwellberry_seed", 100).greeting("Would you like to trade in seeds?"));

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
        add(specialist("karamja_spirits_bar", "Karamja Spirits Bar", "zambo")
                .sell("karamjan_rum", 10, 30).greeting("Hey, are you wanting to try some of my fine wines and spirits?"));

        // ------------------------------------------------------------- Port Sarim, Rimmington
        add(specialist("gerrants_fishy_business", "Gerrant's Fishy Business", "npc_gerrant")
                .sell("small_fishing_net", 5).sell("fishing_rod", 5).sell("fly_fishing_rod", 5).sell("harpoon", 2)
                .sell("lobster_pot", 2).sell("fishing_bait", 1500).sell("minecraft:feather", 1000, 2)
                .sell("raw_shrimps", 0).sell("raw_sardine", 0).sell("raw_herring", 0).sell("raw_anchovies", 0)
                .sell("raw_trout", 0).sell("raw_pike", 0).sell("raw_salmon", 0).sell("raw_tuna", 0).sell("raw_lobster", 0)
                .sell("raw_swordfish", 0).greeting("Welcome! You can buy fishing equipment at my store."));
        add(runes(specialist("bettys_magic_emporium", "Betty's Magic Emporium", "npc_betty"), 1000)
                .sell("eye_of_newt", 300).sell("wizard_hat", 1).greeting("Welcome to the magic emporium."));
        add(specialist("wydins_food_store", "Wydin's Food Store", "npc_wydin")
                .sell("pot_of_flour", 3).sell("minecraft:beef", 1, 1).sell("minecraft:chicken", 1, 1).sell("bread", 10)
                .sell("cheese", 3).sell("tomato", 3).sell("banana", 3).sell("chocolate_bar", 3).sell("cake_tin", 2)
                .sell("redberries", 1).greeting("Welcome to my food store! Would you like to buy anything?"));
        add(specialist("brians_battleaxe_bazaar", "Brian's Battleaxe Bazaar", "npc_brian")
                .sell("bronze_battleaxe", 4).sell("iron_battleaxe", 3).sell("steel_battleaxe", 2).sell("black_battleaxe", 1)
                .sell("mithril_battleaxe", 1).sell("adamant_battleaxe", 1).greeting("ello"));
        add(specialist("grums_gold_exchange", "Grum's Gold Exchange", "npc_grum")
                .sell("gold_ring", 0).sell("sapphire_ring", 0).sell("emerald_ring", 0).sell("ruby_ring", 0).sell("diamond_ring", 0)
                .sell("gold_necklace", 0).sell("sapphire_necklace", 0).sell("gold_amulet", 0).sell("amulet_of_magic", 0)
                .sell("amulet_of_defence", 0).sell("amulet_of_strength", 0).sell("amulet_of_power", 0)
                .greeting("Would you like to buy or sell some gold jewellery?"));
        add(specialist("brians_archery_supplies", "Brian's Archery Supplies", "npc_brian_archery")
                .sell("bronze_arrow", 1000).sell("iron_arrow", 800).sell("steel_arrow", 500).sell("mithril_arrow", 200)
                .sell("shortbow", 4).sell("longbow", 4).sell("oak_shortbow", 3).sell("oak_longbow", 3)
                .sell("willow_shortbow", 2).sell("willow_longbow", 2)
                .greeting("Would you like to buy some archery equipment?"));

        // ------------------------------------------------------------- Falador, Taverley, Dwarven Mine
        add(specialist("waynes_chains", "Wayne's Chains - Chainmail Specialist", "npc_wayne")
                .sell("bronze_chainbody", 3).sell("iron_chainbody", 2).sell("steel_chainbody", 1).sell("black_chainbody", 1)
                .sell("mithril_chainbody", 1).sell("adamant_chainbody", 1).greeting("Welcome to Wayne's Chains. Do you wanna buy or sell some chain mail?"));
        add(specialist("cassies_shield_shop", "Cassie's Shield Shop", "npc_cassie")
                .sell("wooden_shield", 5).sell("bronze_sq_shield", 3).sell("bronze_kiteshield", 3).sell("iron_sq_shield", 2)
                .sell("iron_kiteshield", 0).sell("steel_sq_shield", 0).sell("steel_kiteshield", 0).sell("mithril_sq_shield", 0)
                .greeting("I buy and sell shields. Do you want to trade?"));
        add(specialist("herquins_gems", "Herquin's Gems", "npc_herquin")
                .sell("uncut_sapphire", 1).sell("uncut_emerald", 0).sell("uncut_ruby", 0).sell("uncut_diamond", 0)
                .sell("sapphire", 1).sell("emerald", 0).sell("ruby", 0).sell("diamond", 0)
                .greeting("Do you wish to trade? I have the finest gems."));
        add(specialist("flynns_mace_market", "Flynn's Mace Market", "npc_flynn")
                .sell("bronze_mace", 5).sell("iron_mace", 4).sell("steel_mace", 4).sell("mithril_mace", 3).sell("adamant_mace", 2)
                .greeting("Hello. Do you want to buy or sell any maces?"));
        add(specialist("gaius_two_handed_shop", "Gaius' Two-Handed Shop", "npc_gaius")
                .sell("bronze_2h_sword", 4).sell("iron_2h_sword", 3).sell("steel_2h_sword", 2).sell("black_2h_sword", 1)
                .sell("mithril_2h_sword", 1).sell("adamant_2h_sword", 1).greeting("Welcome to my two-handed sword shop."));
        add(specialist("jatix_herblore_shop", "Jatix's Herblore Shop", "npc_jatix")
                .sell("vial", 300).sell("pestle_and_mortar", 10).sell("eye_of_newt", 300)
                .greeting("Hello, how can I help you?"));
        add(specialist("nurmofs_pickaxe_shop", "Nurmof's Pickaxe Shop", "npc_nurmof")
                .sell("bronze_pickaxe", 6, 1).sell("iron_pickaxe", 5).sell("steel_pickaxe", 4).sell("mithril_pickaxe", 3)
                .sell("adamant_pickaxe", 2).sell("rune_pickaxe", 1).greeting("Greetings, stranger! Can I interest you in a pickaxe?"));
        add(specialist("drogos_mining_emporium", "Drogo's Mining Emporium", "npc_drogo")
                .sell("hammer", 4).sell("bronze_bar", 0).sell("iron_bar", 0).sell("steel_bar", 0).sell("gold_bar", 0)
                .sell("mithril_bar", 0).sell("copper_ore", 0).sell("tin_ore", 0).sell("iron_ore", 0).sell("coal", 0)
                .sell("gold_ore", 0).sell("mithril_ore", 0).greeting("'Ello. Welcome to my Mining shop, friend."));

        // ------------------------------------------------------------- Al Kharid, desert
        add(specialist("zekes_superior_scimitars", "Zeke's Superior Scimitars", "npc_zeke")
                .sell("bronze_scimitar", 5).sell("iron_scimitar", 3).sell("steel_scimitar", 2).sell("mithril_scimitar", 1)
                .greeting("A thousand greetings, sir."));
        add(specialist("louies_armoured_legs_bazaar", "Louie's Armoured Legs Bazaar", "npc_louie_legs")
                .sell("bronze_platelegs", 5).sell("iron_platelegs", 3).sell("steel_platelegs", 2).sell("black_platelegs", 1)
                .sell("mithril_platelegs", 1).sell("adamant_platelegs", 1).greeting("Hey, wanna buy some armour?"));
        add(specialist("ranaels_super_skirt_store", "Ranael's Super Skirt Store", "npc_ranael")
                .sell("bronze_plateskirt", 5).sell("iron_plateskirt", 3).sell("steel_plateskirt", 2).sell("black_plateskirt", 1)
                .sell("mithril_plateskirt", 1).sell("adamant_plateskirt", 1).greeting("Do you want to buy any armoured skirts? Designed especially for ladies who like to fight."));
        add(specialist("dommiks_crafting_store", "Dommik's Crafting Store", "npc_dommik")
                .sell("chisel", 2).sell("ring_mould", 4).sell("necklace_mould", 2).sell("amulet_mould", 2).sell("needle", 3)
                .sell("thread", 100).sell("holy_mould", 3).sell("sickle_mould", 3).sell("tiara_mould", 10).sell("bracelet_mould", 5)
                .greeting("Would you like to buy some crafting equipment?"));
        add(specialist("al_kharid_gem_trader", "Gem Trader", "npc_gem_trader")
                .sell("uncut_sapphire", 1).sell("uncut_emerald", 1).sell("uncut_ruby", 0).sell("uncut_diamond", 0)
                .sell("sapphire", 1).sell("emerald", 1).sell("ruby", 0).sell("diamond", 0)
                .greeting("Good day to you, traveller. Would you be interested in buying some gems?"));
        add(specialist("karims_kebabs", "Kebab Shop", "npc_karim")
                .sell("kebab", 10).greeting("Would you like to buy a nice kebab? Only one gold."));
        add(specialist("shantay_pass_shop", "Shantay Pass Shop", "npc_shantay")
                .sell("waterskin_4", 20).sell("shantay_pass_ticket", 100).sell("knife", 10).sell("rope", 10).sell("minecraft:bucket", 5, 2)
                .sell("desert_shirt", 20).sell("desert_robe", 20).sell("desert_boots", 20)
                .greeting("Hello effendi, I am Shantay. I see you're new. Make sure you read the poster before going into the desert."));
        add(specialist("rokuhs_choc_ice_emporium", "Rokuh's Choc-Ice Emporium", "npc_rokuh")
                .sell("choc_ice", 10).greeting("Choc-ice! Get your choc-ice here! Best way to cool off in the desert!"));
        add(specialist("seddus_adventurers_store", "Seddu's Adventurers' Store", "npc_seddu") // approx. stock
                .sell("mithril_med_helm", 1).sell("mithril_chainbody", 1).sell("mithril_platelegs", 1).sell("mithril_scimitar", 1)
                .sell("adamant_med_helm", 1).sell("adamant_chainbody", 1).sell("adamant_platelegs", 1).sell("adamant_scimitar", 1)
                .greeting("Hello, would you like to see what I have for sale?"));

        // ------------------------------------------------------------- Kandarin
        add(specialist("harrys_fishing_shop", "Harry's Fishing Shop", "npc_harry")
                .sell("small_fishing_net", 5).sell("fishing_rod", 5).sell("fly_fishing_rod", 5).sell("harpoon", 5)
                .sell("lobster_pot", 5).sell("fishing_bait", 200).sell("minecraft:feather", 200, 2).sell("empty_fishbowl", 5)
                .sell("raw_shrimps", 0).sell("raw_sardine", 0).sell("raw_herring", 0).sell("raw_anchovies", 0)
                .sell("raw_trout", 0).sell("raw_pike", 0).sell("raw_salmon", 0).sell("raw_tuna", 0).sell("raw_lobster", 0)
                .sell("raw_swordfish", 0).greeting("Welcome! You can buy fishing equipment at my store."));
        add(specialist("hicktons_archery_emporium", "Hickton's Archery Emporium", "npc_hickton")
                .sell("bronze_arrow", 1000).sell("iron_arrow", 800).sell("steel_arrow", 0).sell("bronze_arrowtips", 1000)
                .sell("iron_arrowtips", 800).sell("steel_arrowtips", 0).sell("headless_arrow", 1000).sell("arrow_shaft", 1000)
                .sell("minecraft:feather", 1000, 2).sell("bow_string", 50).sell("shortbow", 4).sell("longbow", 4)
                .sell("oak_shortbow", 3).sell("oak_longbow", 3).sell("willow_shortbow", 2).sell("maple_shortbow", 1)
                .sell("studded_body", 1).sell("coif", 1).greeting("Welcome to Hickton's Archery Emporium. Do you want to see my wares?"));
        add(specialist("candle_shop", "Candle Shop", "candle_maker")
                .sell("candle", 10, 3).sell("black_candle", 10, 4).greeting("Hi! Would you like a candle?"));
        add(specialist("vanessas_farming_shop", "Vanessa's Farming Shop", "npc_vanessa")
                .sell("rake", 10).sell("seed_dibber", 10).sell("spade", 10).sell("secateurs", 10).sell("watering_can", 10)
                .sell("gardening_trowel", 10).sell("plant_pot", 50).sell("compost", 100).sell("minecraft:bucket", 10, 2)
                .greeting("Hello. I sell farming equipment."));
        add(specialist("aemads_adventuring_supplies", "Aemad's Adventuring Supplies", "npc_aemad")
                .sell("candle", 10, 3).sell("rope", 10).sell("tinderbox", 10).sell("empty_candle_lantern", 3)
                .sell("empty_oil_lamp", 3).sell("knife", 5).sell("spade", 5).sell("minecraft:bucket", 5, 2)
                .greeting("Hello, adventurer. Are you here to stock up?"));
        add(specialist("zeneshas_plate_mail", "Zenesha's Plate Mail Body Shop", "npc_zenesha")
                .sell("bronze_platebody", 3).sell("iron_platebody", 1).sell("steel_platebody", 1).sell("black_platebody", 1)
                .sell("mithril_platebody", 1).greeting("Hello. Would you like to buy a platebody?"));
        add(specialist("ardougne_baker", "Baker's Stall", "npc_ardougne_baker")
                .sell("bread", 10).sell("cake", 3).sell("chocolate_slice", 8).greeting("Fresh bread, cakes and chocolate slices!"));
        add(specialist("ardougne_silk_trader", "Silk Merchant", "npc_ardougne_silk_merchant")
                .sell("silk", 10).greeting("Silk! The finest silk in Ardougne!"));
        add(specialist("ardougne_fur_trader", "Fur Trader", "npc_ardougne_fur_trader")
                .sell("grey_wolf_fur", 3).sell("bear_fur", 3).greeting("Would you like to buy some fur?"));
        add(specialist("ardougne_silver_merchant", "Silver Merchant", "npc_ardougne_silver_merchant")
                .sell("silver_ore", 2).sell("silver_bar", 2).sell("unstrung_symbol", 2).sell("tiara", 2)
                .greeting("Silver! I sell the best silver in Ardougne."));
        add(specialist("ardougne_spice_seller", "Spice Merchant", "npc_ardougne_spice_seller")
                .sell("spice", 20).greeting("Spice! Exotic spice from the desert!"));
        add(specialist("ardougne_gem_merchant", "Gem Merchant", "npc_ardougne_gem_merchant")
                .sell("sapphire", 2).sell("emerald", 1).sell("ruby", 1).sell("diamond", 0)
                .greeting("Here, look at my lovely gems."));
        add(specialist("davons_amulet_store", "Davon's Amulet Store", "npc_davon")
                .sell("amulet_of_magic", 0).sell("amulet_of_defence", 0).sell("amulet_of_strength", 0).sell("amulet_of_power", 0)
                .sell("amulet_of_accuracy", 0).sell("amulet_of_glory", 0).greeting("Pssst! Come here if you want to do some amulet trading."));
        add(specialist("frenitas_cookery_shop", "Frenita's Cookery Shop", "npc_frenita")
                .sell("empty_pot", 8).sell("jug", 5).sell("pie_dish", 5).sell("cake_tin", 5).sell("bowl", 5)
                .sell("pot_of_flour", 8).sell("minecraft:bucket", 5, 2).greeting("Would you like to buy some cooking equipment?"));
        add(specialist("alecks_hunter_emporium", "Aleck's Hunter Emporium", "npc_aleck")
                .sell("bird_snare", 50).sell("box_trap", 50).sell("butterfly_net", 25).sell("impling_jar", 50)
                .sell("noose_wand", 25).sell("rabbit_snare", 25).sell("teasing_stick", 25)
                .greeting("Welcome to my Hunter Emporium. Can I interest you in some hunting gear?"));
        add(specialist("gulluck_and_sons", "Gulluck and Sons", "npc_gulluck")
                .sell("bronze_arrow", 1000).sell("iron_arrow", 800).sell("steel_arrow", 500).sell("mithril_arrow", 300)
                .sell("adamant_arrow", 100).sell("shortbow", 4).sell("longbow", 4).sell("oak_shortbow", 3).sell("oak_longbow", 3)
                .sell("bronze_battleaxe", 4).sell("iron_battleaxe", 3).sell("steel_battleaxe", 2).sell("mithril_battleaxe", 1)
                .sell("bronze_2h_sword", 4).sell("iron_2h_sword", 3).sell("steel_2h_sword", 2).sell("mithril_2h_sword", 1)
                .sell("bronze_longsword", 4).sell("iron_longsword", 3).sell("steel_longsword", 2).sell("mithril_longsword", 1)
                .greeting("Hello, human. We sell all sorts of weapons."));
        add(specialist("grand_tree_groceries", "Grand Tree Groceries", "npc_heckel_funch")
                .sell("gianne_dough", 10).sell("equa_leaves", 20).sell("gnome_spice", 20).sell("lemon", 20).sell("lime", 20)
                .sell("orange", 20).sell("pineapple", 10).sell("potato", 10).sell("onion", 10).sell("cabbage", 10)
                .sell("cocktail_shaker", 5).sell("pot_of_cream", 5).sell("chocolate_bar", 10)
                .greeting("Good day to you. Are you after some fine gnome produce?"));

        // ------------------------------------------------------------- Karamja
        add(specialist("fernaheis_fishing_hut", "Fernahei's Fishing Hut", "npc_fernahei")
                .sell("fishing_rod", 10).sell("fly_fishing_rod", 10).sell("fishing_bait", 300).sell("minecraft:feather", 300, 2)
                .sell("raw_trout", 0).sell("raw_pike", 0).sell("raw_salmon", 0)
                .greeting("Welcome to Fernahei's Fishing Hut, Bwana! Would you like to see my items?"));

        // ------------------------------------------------------------- Fremennik Province
        add(specialist("fremennik_fish_monger", "Fremennik Fish Monger", "npc_fremennik_fishmonger")
                .sell("raw_cod", 5).sell("raw_mackerel", 5).sell("raw_bass", 5).sell("raw_tuna", 5).sell("raw_lobster", 3)
                .sell("raw_swordfish", 1).greeting("Fresh fish! Caught this very morning."));
        add(specialist("fremennik_fur_trader", "Fremennik Fur Trader", "npc_fremennik_fur_trader")
                .sell("grey_wolf_fur", 5).sell("bear_fur", 5).greeting("Furs! Warm furs for the Fremennik winter!"));

        // ------------------------------------------------------------- Morytania
        add(specialist("rufus_meat_emporium", "Rufus' Meat Emporium", "npc_rufus")
                .sell("minecraft:beef", 5, 1).sell("minecraft:chicken", 5, 1).sell("minecraft:rabbit", 5, 1)
                .greeting("Grrr... meat! Fresh meat for sale! Raw, just how you like it."));
        add(specialist("barkers_haberdashery", "Barker's Haberdashery", "npc_barker") // approx. stock
                .sell("leather_gloves", 5).sell("leather_boots", 5).sell("black_skirt", 3).sell("blue_skirt", 3).sell("red_cape", 3)
                .greeting("Grrr... hello, would you like to buy some clothes?"));

        // ------------------------------------------------------------- Lletya
        add(specialist("lletya_food_store", "Lletya Food Store", "npc_lletya_food_seller")
                .sell("bread", 10).sell("cheese", 5).sell("cabbage", 10).sell("potato", 10).sell("banana", 5).sell("orange", 5)
                .greeting("Would you like to buy some food?"));
        add(specialist("lletya_archery_shop", "Lletya Archery Shop", "npc_lletya_fletcher")
                .sell("mithril_arrow", 300).sell("adamant_arrow", 200).sell("rune_arrow", 100).sell("maple_shortbow", 2)
                .sell("maple_longbow", 2).sell("yew_shortbow", 1).sell("yew_longbow", 1).sell("magic_shortbow", 1)
                .greeting("Welcome, traveller. Can I interest you in the finest bows in Tirannwn?"));

        // ------------------------------------------------------------- Wilderness, Mage Arena
        add(runes(specialist("lundails_rune_shop", "Lundail's Arena-side Rune Shop", "npc_lundail"), 1000)
                .sell("chaos_rune", 250).sell("death_rune", 250).sell("blood_rune", 250).sell("nature_rune", 100)
                .sell("law_rune", 100).sell("cosmic_rune", 100).greeting("Can I help you, adventurer? Runes for the battlefield?"));
        add(specialist("mage_arena_staffs", "Mage Arena Staffs", "npc_chamber_guardian")
                .sell("saradomin_staff", 5).sell("guthix_staff", 5).sell("zamorak_staff", 5)
                .greeting("Greetings. I can supply the staves of the gods to those who have proven themselves."));
    }
}
