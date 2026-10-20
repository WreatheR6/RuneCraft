package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.RewardShop.Cost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.runecraft.osrs.content.minigames.MinigameCurrencies.*;

/**
 * Every minigame reward shop with OSRS prices. Prices marked "approx." aren't known exactly and
 * are close estimates. Item ids starting with '#' are packs rolled when bought (#herbs, #seeds,
 * #ores).
 */
public final class RewardShops {
    /** Agility XP per Brimhaven Agility Arena ticket (flat since the May 2024 rebalance). */
    public static final double ARENA_TICKET_XP = 345.0;
    /**
     * Item id prefix of a Graceful recolour entry: buying it turns the full plain Graceful set the
     * player carries into the set with this suffix (e.g. {@code #graceful_agility_arena}).
     */
    public static final String GRACEFUL_RECOLOUR = "#graceful";

    /** Items a shop hands out as bank notes: Petrified Pete's ores and minerals (OSRS Wiki: "noted"). */
    private static final Map<String, List<String>> NOTED = Map.of("volcanic_mine", List.of("iron_ore", "silver_ore", "coal",
            "gold_ore", "mithril_ore", "adamantite_ore", "runite_ore", "volcanic_ash", "calcite", "pyrophosphite"));

    /** True when {@code shopId} sells {@code item} noted. */
    public static boolean noted(String shopId, String item) {
        return NOTED.getOrDefault(shopId, List.of()).contains(item);
    }

    /** Rewards with a daily purchase limit (OSRS Wiki "Herb box": 15 a day from the Nightmare Zone), 0 for none. */
    public static int dailyLimit(String shopId, String item) {
        return shopId.equals("nightmare_zone") && item.equals("herb_box") ? 15 : 0;
    }
    private static final Map<String, RewardShop> SHOPS = new LinkedHashMap<>();

    static {
        // Void Knight squire / Void Knights' reward shop (Pest Control commendation points).
        RewardShop pc = shop("pest_control", "Void Knights' Rewards");
        for (Skill s : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.HITPOINTS}) {
            pc.xp(s, 35, PEST_CONTROL, 10, 25).xp(s, 35, PEST_CONTROL, 100, 25);
        }
        for (Skill s : new Skill[]{Skill.RANGED, Skill.MAGIC}) {
            pc.xp(s, 32, PEST_CONTROL, 10, 25).xp(s, 32, PEST_CONTROL, 100, 25);
        }
        pc.xp(Skill.PRAYER, 18, PEST_CONTROL, 10, 25).xp(Skill.PRAYER, 18, PEST_CONTROL, 100, 25);
        pc.item("void_knight_mace", PEST_CONTROL, 250)
                .item("void_knight_top", PEST_CONTROL, 250)
                .item("void_knight_robe", PEST_CONTROL, 250)
                .item("void_knight_gloves", PEST_CONTROL, 150)
                .item("void_mage_helm", PEST_CONTROL, 200)
                .item("void_ranger_helm", PEST_CONTROL, 200)
                .item("void_melee_helm", PEST_CONTROL, 200)
                .itemReq("elite_void_top", PEST_CONTROL, 200, "void_knight_top")
                .itemReq("elite_void_robe", PEST_CONTROL, 200, "void_knight_robe")
                .item("void_seal", PEST_CONTROL, 10)
                .itemSkill("#herbs", 1, PEST_CONTROL, 30, Skill.HERBLORE, 25)
                .itemSkill("#seeds", 1, PEST_CONTROL, 15, Skill.FARMING, 25)
                .itemSkill("#ores", 1, PEST_CONTROL, 15, Skill.MINING, 25);

        // The Mor Ul Rek stores in one (TzHaar-Hur-Zal's equipment, TzHaar-Mej-Roh's runes, TzHaar-Hur-Rin's ores and gems):
        // tokkul prices are 150% of the item's value (OSRS Wiki store pages, sell multiplier 1500). Round 34: the obsidian
        // platebody and platelegs cost 126,000 and 100,500 (were 1,331,200 and 768,000); nature runes aren't sold there.
        shop("tzhaar", "TzHaar-Hur-Tel's Equipment Store")
                .item("obsidian_helmet", TOKKUL, 84_480)
                .item("obsidian_platebody", TOKKUL, 126_000)
                .item("obsidian_platelegs", TOKKUL, 100_500)
                .item("toktz_ket_xil", TOKKUL, 67_500)
                .item("tzhaar_ket_om", TOKKUL, 75_001)
                .item("tzhaar_ket_em", TOKKUL, 45_000)
                .item("toktz_xil_ak", TOKKUL, 60_000)
                .item("toktz_xil_ek", TOKKUL, 37_500)
                .item("toktz_mej_tal", TOKKUL, 52_500)
                .item("toktz_xil_ul", TOKKUL, 375) // per ring
                .item("obsidian_cape", TOKKUL, 90_000)
                .item("fire_rune", 100, TOKKUL, 600)
                .item("water_rune", 100, TOKKUL, 600)
                .item("air_rune", 100, TOKKUL, 600)
                .item("earth_rune", 100, TOKKUL, 600)
                .item("mind_rune", 100, TOKKUL, 400)
                .item("body_rune", 100, TOKKUL, 400)
                .item("chaos_rune", 10, TOKKUL, 1_350)
                .item("death_rune", 10, TOKKUL, 2_700)
                .item("tin_ore", TOKKUL, 4)
                .item("copper_ore", TOKKUL, 4)
                .item("iron_ore", TOKKUL, 25)
                .item("silver_ore", TOKKUL, 112)
                .item("coal", 10, TOKKUL, 670)
                .item("gold_ore", TOKKUL, 225)
                .item("mithril_ore", TOKKUL, 243)
                .item("adamantite_ore", TOKKUL, 600)
                .item("runite_ore", TOKKUL, 4_800)
                .item("uncut_sapphire", TOKKUL, 37)
                .item("uncut_emerald", TOKKUL, 75)
                .item("uncut_ruby", TOKKUL, 150)
                .item("uncut_diamond", TOKKUL, 300)
                .item("uncut_dragonstone", TOKKUL, 1_500)
                .item("uncut_onyx", TOKKUL, 300_000)
                .item("onyx_bolt_tips", TOKKUL, 1_500);

        // Guardians of the Rift: Apprentice Felix's Temple Supplies (abyssal pearls; OSRS Wiki). The elemental and
        // catalytic talismans aren't sold. Approx: the colossal pouch stands in for using the abyssal needle on the pouches.
        shop("guardians_of_the_rift", "Temple Supplies")
                .item("hat_of_the_eye", PEARLS, 400)
                .item("robe_top_of_the_eye", PEARLS, 350)
                .item("robe_bottoms_of_the_eye", PEARLS, 350)
                .item("boots_of_the_eye", PEARLS, 250) // full outfit totals 1,350 pearls
                .item("abyssal_lantern", PEARLS, 1500)
                .item("ring_of_the_elements", PEARLS, 400)
                .itemReq("colossal_pouch", PEARLS, 100, "abyssal_needle")
                .item("air_talisman", PEARLS, 10).item("water_talisman", PEARLS, 10).item("earth_talisman", PEARLS, 10)
                .item("fire_talisman", PEARLS, 10).item("mind_talisman", PEARLS, 10).item("body_talisman", PEARLS, 10)
                .item("chaos_talisman", PEARLS, 30).item("cosmic_talisman", PEARLS, 30)
                .item("nature_talisman", PEARLS, 50).item("law_talisman", PEARLS, 50)
                .item("death_talisman", PEARLS, 80).item("blood_talisman", PEARLS, 100);

        // Barbarian Assault: Commander Connad (honour points of every role; OSRS Wiki). Items marked
        // "Queen" on the wiki also spend one Penance Queen kill; the granite body costs coins only.
        // Tai Bwo Wannai: Gabooty's Cooperative (trading sticks).
        TaiBwoWannai.shop(shop(TaiBwoWannai.SHOP, "Gabooty's Tai Bwo Wannai Cooperative"));

        RewardShop ba = shop("barbarian_assault", "Commander Connad's Rewards");
        ba.itemMulti("fighter_torso", queen(allRoles(375)));
        ba.itemMulti("penance_gloves", allRoles(150));
        ba.itemMulti("penance_skirt", queen(allRoles(375)));
        ba.itemMulti("fighter_hat", queen(allRoles(275)));
        ba.itemMulti("ranger_hat", queen(allRoles(275)));
        ba.itemMulti("runner_hat", queen(allRoles(275)));
        ba.itemMulti("healer_hat", queen(allRoles(275)));
        ba.itemMulti("runner_boots", allRoles(100));
        ba.itemMulti("granite_body", queen(List.of(new Cost(COINS, 95_000))));
        // Gambles: paid from one role of the player's choice (BaGambles); the high gamble also spends a queen kill.
        String[] roleCurrencies = {BA_ATTACKER, BA_DEFENDER, BA_COLLECTOR, BA_HEALER};
        for (BaGambles.Tier tier : BaGambles.Tier.values()) {
            for (int r = 0; r < roleCurrencies.length; r++) {
                List<Cost> cost = List.of(new Cost(roleCurrencies[r], tier.cost()));
                String role = MinigameRules.BarbarianAssault.ROLES[r];
                ba.labelled(tier.id(), tier.displayName() + " (" + Character.toUpperCase(role.charAt(0)) + role.substring(1) + ")",
                        tier.queen() ? queen(cost) : cost);
            }
        }

        // Castle Wars: Lanthus' Castle Wars Ticket Exchange (OSRS Wiki prices; each decorative tier
        // costs ten times the one before).
        RewardShop cw = shop("castle_wars", "Castle Wars Ticket Exchange");
        String[] tiers = {"red", "white", "gold"};
        for (int i = 0; i < tiers.length; i++) {
            int m = i == 0 ? 1 : i == 1 ? 10 : 100;
            cw.item(tiers[i] + "_decorative_helm", CW_TICKETS, (i == 1 ? 4 : 5) * m) // full helm: 5 / 40 / 500
                    .item(tiers[i] + "_decorative_body", CW_TICKETS, 8 * m)
                    .item(tiers[i] + "_decorative_legs", CW_TICKETS, 6 * m)
                    .item(tiers[i] + "_decorative_shield", CW_TICKETS, 6 * m)
                    .item(tiers[i] + "_decorative_sword", CW_TICKETS, 5 * m);
        }
        cw.item("saradomin_halo", CW_TICKETS, 75).item("zamorak_halo", CW_TICKETS, 75).item("guthix_halo", CW_TICKETS, 75)
                .item("saradomin_hood", CW_TICKETS, 10).item("zamorak_hood", CW_TICKETS, 10);

        // Mage Training Arena: Rewards Guardian (telekinetic / alchemist / enchantment / graveyard pizazz).
        RewardShop mta = shop("mage_training_arena", "Mage Training Arena Rewards");
        mta.itemMulti("infinity_hat", pizazz(350, 400, 3000, 350));
        mta.itemMulti("infinity_top", pizazz(400, 450, 4000, 400));
        mta.itemMulti("infinity_bottoms", pizazz(450, 500, 5000, 450));
        mta.itemMulti("infinity_boots", pizazz(120, 120, 1200, 120));
        mta.itemMulti("infinity_gloves", pizazz(175, 225, 1500, 175));
        mta.itemMulti("beginner_wand", pizazz(30, 30, 300, 30));
        // Each wand is an upgrade of the one before it (the old wand is handed in).
        mta.upgrade("beginner_wand", "apprentice_wand", pizazz(60, 60, 600, 60));
        mta.upgrade("apprentice_wand", "teacher_wand", pizazz(150, 200, 1500, 150));
        mta.upgrade("teacher_wand", "master_wand", pizazz(240, 240, 2400, 240));
        mta.itemMulti("mages_book", pizazz(500, 550, 6000, 500));
        mta.unlock("bones_to_peaches", "Bones to Peaches", pizazz(200, 300, 2000, 200));
        // Runes (per rune; a currency costing 0 points is left out).
        String[][] runes = {{"mist_rune", "1", "1", "15", "1"}, {"dust_rune", "1", "1", "15", "1"}, {"mud_rune", "1", "1", "15", "1"},
                {"smoke_rune", "1", "1", "15", "1"}, {"steam_rune", "1", "1", "15", "1"}, {"lava_rune", "1", "1", "15", "1"},
                {"cosmic_rune", "0", "0", "5", "0"}, {"chaos_rune", "0", "1", "5", "1"}, {"nature_rune", "0", "1", "0", "1"},
                {"death_rune", "2", "1", "20", "1"}, {"law_rune", "2", "0", "0", "0"}, {"soul_rune", "2", "2", "25", "2"},
                {"blood_rune", "2", "2", "25", "2"}};
        for (String[] r : runes) {
            List<Cost> costs = new ArrayList<>();
            String[] cur = {MTA_TELEKINETIC, MTA_ALCHEMIST, MTA_ENCHANTMENT, MTA_GRAVEYARD};
            for (int i = 0; i < 4; i++) {
                int n = Integer.parseInt(r[i + 1]);
                if (n > 0) {
                    costs.add(new Cost(cur[i], n));
                }
            }
            mta.itemMulti(r[0], costs);
        }

        // Hallowed Sepulchre: the Mysterious Stranger's Mysterious Hallowed Goods (wiki stock and prices, in hallowed marks). The
        // strange old lockpick is a coffin drop, not stock.
        shop("hallowed_sepulchre", "Mysterious Hallowed Goods")
                .item("hallowed_crystal_shard", MARKS, 1)
                .item("hallowed_token", MARKS, 10)
                .item("hallowed_grapple", MARKS, 100)
                .item("hallowed_focus", MARKS, 100)
                .item("hallowed_symbol", MARKS, 100)
                .item("hallowed_hammer", MARKS, 100)
                .item("hallowed_ring", MARKS, 250)
                .item("dark_dye", MARKS, 300)
                .item("dark_acorn", MARKS, 3000)
                .item("hallowed_sack", MARKS, 100);

        // Brimhaven Agility Arena: Pirate Jackie the Fruit. Since the May 2024 rebalance tickets only buy Agility XP,
        // at a flat 345 each, and the items cost Brimhaven vouchers (pirate's hook 800, the Agility Arena Graceful
        // recolour 250 with a full set; the herbs' voucher prices approx., their old ticket prices).
        shop("brimhaven_agility_arena", "Pirate Jackie's Ticket Exchange")
                .xpFlat(Skill.AGILITY, ARENA_TICKET_XP, ARENA_TICKETS, 1)
                .xpFlat(Skill.AGILITY, ARENA_TICKET_XP, ARENA_TICKETS, 10)
                .xpFlat(Skill.AGILITY, ARENA_TICKET_XP, ARENA_TICKETS, 100)
                .item("pirates_hook", BRIMHAVEN_VOUCHERS, 800)
                .item(GRACEFUL_RECOLOUR + "_agility_arena", BRIMHAVEN_VOUCHERS, 250)
                .item("toadflax", BRIMHAVEN_VOUCHERS, 3)
                .item("snapdragon", BRIMHAVEN_VOUCHERS, 10);

        // Trouble Brewing: Honest Jimmy's House of Stuff (pieces of eight; OSRS Wiki: ten times each item's value,
        // rum 20).
        RewardShop tb = shop("trouble_brewing", "Honest Jimmy's House of Stuff");
        for (String c : MinigameItems.NAVAL_COLOURS) {
            tb.item(c + "_naval_shirt", EIGHT, 1000).item(c + "_tricorn_hat", EIGHT, 500).item(c + "_navy_slacks", EIGHT, 1000);
        }
        tb.item("bottle_of_rum", EIGHT, 20).item("the_stuff", EIGHT, 50);

        // Volcanic Mine: Petrified Pete's Ore Shop (OSRS Wiki prices; ores and minerals come noted, see NOTED). The large water
        // container (10,000 points) doubles the heat-proof vessel's throws (VolcanicMine.LARGE_VESSEL_FLAG). Not stocked here:
        // the volcanic mine teleport, the ash covered tome and ore packs.
        shop("volcanic_mine", "Petrified Pete's Ore Shop")
                .unlock(VolcanicMine.LARGE_VESSEL_FLAG, "Large water container",
                        List.of(new Cost(VOLCANIC_MINE, VolcanicMine.LARGE_VESSEL_POINTS)))
                .item("iron_ore", VOLCANIC_MINE, 30)
                .item("silver_ore", VOLCANIC_MINE, 55)
                .item("coal", VOLCANIC_MINE, 60)
                .item("gold_ore", VOLCANIC_MINE, 150)
                .item("mithril_ore", VOLCANIC_MINE, 150)
                .item("adamantite_ore", VOLCANIC_MINE, 300)
                .item("runite_ore", VOLCANIC_MINE, 855)
                .item("volcanic_ash", VOLCANIC_MINE, 40)
                .item("calcite", VOLCANIC_MINE, 70)
                .item("pyrophosphite", VOLCANIC_MINE, 70)
                .item("prospector_helmet", VOLCANIC_MINE, 26_000)
                .item("prospector_jacket", VOLCANIC_MINE, 39_000)
                .item("prospector_legs", VOLCANIC_MINE, 34_000)
                .item("prospector_boots", VOLCANIC_MINE, 21_000);

        // Tithe Farm reward shop.
        shop("tithe_farm", "Tithe Farm Rewards")
                .item("farmers_strawhat", TITHE, 75)
                .item("farmers_jacket", TITHE, 150)
                .item("farmers_boro_trousers", TITHE, 125)
                .item("farmers_boots", TITHE, 50)
                .item("gricollers_can", TITHE, 200)
                .item("seed_box", TITHE, 250)
                .item("herb_sack", TITHE, 250);

        // Giants' Foundry: Kovac (reputation, per the wiki reward shop).
        shop("giants_foundry", "Kovac's Rewards")
                .item("smiths_tunic", FOUNDRY, 4000)
                .item("smiths_trousers", FOUNDRY, 4000)
                .item("smiths_boots", FOUNDRY, 3500)
                .item("smiths_gloves", FOUNDRY, 3500)
                .item("colossal_blade", FOUNDRY, 5000)
                .item("double_ammo_mould", FOUNDRY, 2000)
                .item("kovacs_grog", FOUNDRY, 300); // approx

        // Mahogany Homes: Amy's carpenter points shop.
        shop("mahogany_homes", "Amy's Rewards")
                .item("carpenters_helmet", CARPENTER, 400)
                .item("carpenters_shirt", CARPENTER, 800)
                .item("carpenters_trousers", CARPENTER, 600)
                .item("carpenters_boots", CARPENTER, 200)
                .item("plank_sack", CARPENTER, 350)
                .item("amys_saw", CARPENTER, 500)
                .item("hosidius_blueprints", CARPENTER, 2000)
                .item("steel_nails", 100, CARPENTER, 5);

        // Gnome Restaurant: a reward token (12 delivery points) is cashed in for one gnome dish delivered to you.
        // The scarf, goggles and mint cake are tips from customers (MinigameRules.GnomeRestaurant.TIPS).
        RewardShop gnome = shop("gnome_restaurant", "Aluft Aloft reward token");
        for (Object[] dish : MinigameItems.GNOME_DISHES) {
            gnome.item((String) dish[0], GNOME_TOKENS, 1);
        }

        // Nightmare Zone: Dom Onion's Reward Shop (OSRS Wiki): resources, upgrades (imbues) and benefits. Benefits are sold
        // by the dose in OSRS and kept in barrels; here they come as 4-dose potions at four times the dose price.
        RewardShop nmz = shop("nightmare_zone", "Dom Onion's Reward Shop")
                .item("flax", NIGHTMARE_ZONE, 75)
                .item("bucket_of_sand", NIGHTMARE_ZONE, 200)
                .item("seaweed", NIGHTMARE_ZONE, 200)
                .item("compost_potion", NIGHTMARE_ZONE, 5000)
                .item("air_rune", NIGHTMARE_ZONE, 25)
                .item("water_rune", NIGHTMARE_ZONE, 25)
                .item("earth_rune", NIGHTMARE_ZONE, 25)
                .item("fire_rune", NIGHTMARE_ZONE, 25)
                .item("rune_essence", NIGHTMARE_ZONE, 60)
                .item("pure_essence", NIGHTMARE_ZONE, 70)
                .item("vial_of_water", NIGHTMARE_ZONE, 145)
                .item("herb_box", NIGHTMARE_ZONE, 9500)
                .item("scroll_of_redirection", NIGHTMARE_ZONE, 775);
        for (String potion : MinigameRules.NightmareZone.POTIONS) {
            // Sold by the dose into the lobby's potion barrels (RewardShopService), not as 4-dose potions.
            nmz.item(potion, NIGHTMARE_ZONE, MinigameRules.NightmareZone.doseCost(potion));
        }
        for (Object[] imbue : MinigameRules.Imbues.ITEMS) {
            nmz.upgrade((String) imbue[0], (String) imbue[1], NIGHTMARE_ZONE, (Integer) imbue[2]);
        }

        // Soul Wars: Nomad's Soul Wars Reward Shop (zeal; OSRS Wiki). Experience needs level 30 and has no bulk bonus.
        RewardShop sw = shop("soul_wars", "Soul Wars Reward Shop");
        for (Skill s : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.HITPOINTS}) {
            sw.xp(s, 30, ZEAL, 1, 30).xp(s, 30, ZEAL, 10, 30).xp(s, 30, ZEAL, 100, 30);
        }
        for (Skill s : new Skill[]{Skill.RANGED, Skill.MAGIC}) {
            sw.xp(s, 27, ZEAL, 1, 30).xp(s, 27, ZEAL, 10, 30).xp(s, 27, ZEAL, 100, 30);
        }
        sw.xp(Skill.PRAYER, 14, ZEAL, 1, 30).xp(Skill.PRAYER, 14, ZEAL, 10, 30).xp(Skill.PRAYER, 14, ZEAL, 100, 30);
        // The soul cape needs 70 Prayer to buy and to wear (OSRS Wiki "Soul cape"; the wear check is Jewellery.wearLevel).
        sw.itemSkill("soul_cape", 1, ZEAL, 2500, Skill.PRAYER, 70)
                .itemSkill("ectoplasmator", 1, ZEAL, 250, Skill.PRAYER, 40)
                .item(SpoilsOfWar.ITEM, ZEAL, SpoilsOfWar.ZEAL_COST)
                .item("blighted_entangle_sack", 70, ZEAL, 10)
                .item("blighted_teleport_spell_sack", 50, ZEAL, 10)
                .item("blighted_vengeance_sack", 50, ZEAL, 10)
                .item("blighted_ancient_ice_sack", 30, ZEAL, 10)
                .item("blighted_manta_ray", 15, ZEAL, 10)
                .item("blighted_anglerfish", 15, ZEAL, 10)
                .item("blighted_karambwan", 12, ZEAL, 10)
                .item("blighted_super_restore", 4, ZEAL, 10);
        for (Object[] imbue : MinigameRules.Imbues.ITEMS) {
            sw.upgrade((String) imbue[0], (String) imbue[1], ZEAL, (Integer) imbue[3]);
        }

        // Last Man Standing: Justine's stuff for the Last Shopper Standing (points; OSRS Wiki).
        LastManStanding.shop(shop(LastManStanding.SHOP, "Justine's stuff for the Last Shopper Standing"));

        // Emir's Arena: Mubariz's PvP Arena Rewards (points; OSRS Wiki).
        EmirsArena.shop(shop(EmirsArena.SHOP, "PvP Arena Rewards"));
    }

    /** Adds the one Penance Queen kill spent on the queen-locked Barbarian Assault rewards. */
    private static List<Cost> queen(List<Cost> costs) {
        List<Cost> list = new ArrayList<>(costs);
        list.add(new Cost(BA_QUEEN, 1));
        return list;
    }

    private RewardShops() {
    }

    private static RewardShop shop(String id, String title) {
        RewardShop s = new RewardShop(id, title);
        SHOPS.put(id, s);
        return s;
    }

    private static List<Cost> allRoles(int each) {
        return List.of(new Cost(BA_ATTACKER, each), new Cost(BA_DEFENDER, each), new Cost(BA_COLLECTOR, each),
                new Cost(BA_HEALER, each));
    }

    private static List<Cost> pizazz(int t, int a, int e, int g) {
        return List.of(new Cost(MTA_TELEKINETIC, t), new Cost(MTA_ALCHEMIST, a), new Cost(MTA_ENCHANTMENT, e),
                new Cost(MTA_GRAVEYARD, g));
    }

    public static RewardShop get(String id) {
        return SHOPS.get(id);
    }

    public static Map<String, RewardShop> all() {
        return Collections.unmodifiableMap(SHOPS);
    }
}
