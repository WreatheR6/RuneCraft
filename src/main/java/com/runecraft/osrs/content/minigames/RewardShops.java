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
                .item("#herbs", 1, PEST_CONTROL, 30)
                .item("#seeds", 1, PEST_CONTROL, 15)
                .item("#ores", 1, PEST_CONTROL, 15);

        // TzHaar shops (TzHaar-Hur-Tel's equipment and TzHaar-Hur-Lek's ore and gem store), tokkul prices.
        shop("tzhaar", "TzHaar-Hur-Tel's Equipment Store")
                .item("obsidian_helmet", TOKKUL, 84_480)
                .item("obsidian_platebody", TOKKUL, 1_331_200)
                .item("obsidian_platelegs", TOKKUL, 768_000)
                .item("toktz_ket_xil", TOKKUL, 67_500)
                .item("tzhaar_ket_om", TOKKUL, 75_000)
                .item("tzhaar_ket_em", TOKKUL, 45_000)
                .item("toktz_xil_ak", TOKKUL, 60_000)
                .item("toktz_xil_ek", TOKKUL, 37_500)
                .item("toktz_mej_tal", TOKKUL, 52_500)
                .item("toktz_xil_ul", TOKKUL, 375) // per ring
                .item("obsidian_cape", TOKKUL, 90_000)
                .item("uncut_onyx", TOKKUL, 300_000)
                .item("chaos_rune", 10, TOKKUL, 1_350)
                .item("death_rune", 10, TOKKUL, 2_700)
                .item("nature_rune", 10, TOKKUL, 1_350)
                .item("coal", 10, TOKKUL, 670)
                .item("runite_ore", TOKKUL, 4_800);

        // Guardians of the Rift: the Rewards Guardian (abyssal pearls; non-outfit prices approx.).
        shop("guardians_of_the_rift", "Rewards Guardian")
                .item("hat_of_the_eye", PEARLS, 350)
                .item("robe_top_of_the_eye", PEARLS, 350)
                .item("robe_bottoms_of_the_eye", PEARLS, 350)
                .item("boots_of_the_eye", PEARLS, 300) // full outfit totals 1,350 pearls
                .item("abyssal_lantern", PEARLS, 100)
                .itemReq("colossal_pouch", PEARLS, 100, "abyssal_needle")
                .item("catalytic_talisman", PEARLS, 200)
                .item("elemental_talisman", PEARLS, 200)
                .item("pure_essence", 50, PEARLS, 20);

        // Barbarian Assault: Commander Connad (honour points of every role).
        RewardShop ba = shop("barbarian_assault", "Commander Connad's Rewards");
        ba.itemMulti("fighter_torso", allRoles(375));
        ba.itemMulti("penance_gloves", allRoles(150));
        ba.itemMulti("penance_skirt", allRoles(375));
        ba.itemMulti("fighter_hat", allRoles(275));
        ba.itemMulti("ranger_hat", allRoles(275));
        ba.itemMulti("runner_hat", allRoles(275));
        ba.itemMulti("healer_hat", allRoles(275));
        ba.itemMulti("runner_boots", allRoles(100));
        List<Cost> granite = new ArrayList<>(allRoles(95));
        granite.add(new Cost(COINS, 95_000));
        ba.itemMulti("granite_body", granite);

        // Castle Wars: Lanthus (tickets; decorative tiers approx.).
        RewardShop cw = shop("castle_wars", "Lanthus' Castle Wars Rewards");
        String[] tiers = {"red", "white", "gold"};
        for (int i = 0; i < tiers.length; i++) {
            int m = i + 1;
            cw.item(tiers[i] + "_decorative_helm", CW_TICKETS, 60 * m)
                    .item(tiers[i] + "_decorative_body", CW_TICKETS, 120 * m)
                    .item(tiers[i] + "_decorative_legs", CW_TICKETS, 80 * m)
                    .item(tiers[i] + "_decorative_shield", CW_TICKETS, 100 * m)
                    .item(tiers[i] + "_decorative_sword", CW_TICKETS, 50 * m);
        }
        cw.item("saradomin_halo", CW_TICKETS, 75).item("zamorak_halo", CW_TICKETS, 75)
                .item("saradomin_hood", CW_TICKETS, 10).item("zamorak_hood", CW_TICKETS, 10)
                .item("castlewars_brace", CW_TICKETS, 30);

        // Mage Training Arena: Rewards Guardian (telekinetic / alchemist / enchantment / graveyard pizazz).
        RewardShop mta = shop("mage_training_arena", "Mage Training Arena Rewards");
        mta.itemMulti("infinity_hat", pizazz(350, 400, 3000, 350));
        mta.itemMulti("infinity_top", pizazz(400, 450, 4000, 400));
        mta.itemMulti("infinity_bottoms", pizazz(450, 500, 5000, 450));
        mta.itemMulti("infinity_boots", pizazz(120, 120, 1200, 120));
        mta.itemMulti("infinity_gloves", pizazz(175, 225, 1500, 175));
        mta.itemMulti("beginner_wand", pizazz(30, 30, 300, 30));
        mta.itemMulti("apprentice_wand", pizazz(60, 60, 600, 60));
        mta.itemMulti("teacher_wand", pizazz(150, 200, 1500, 150));
        mta.itemMulti("master_wand", pizazz(240, 240, 2400, 240));
        mta.itemMulti("mages_book", pizazz(500, 550, 6000, 500));
        mta.unlock("bones_to_peaches", "Bones to Peaches", pizazz(200, 300, 2000, 200));

        // Hallowed Sepulchre: Mysterious Hallowed Goods (hallowed marks; crystal shard 1, ring 250 per wiki; others approx.).
        shop("hallowed_sepulchre", "Mysterious Hallowed Goods")
                .item("hallowed_crystal_shard", MARKS, 1)
                .item("hallowed_focus", MARKS, 100)
                .item("hallowed_symbol", MARKS, 100)
                .item("hallowed_hammer", MARKS, 100)
                .item("hallowed_grapple", MARKS, 100)
                .item("hallowed_ring", MARKS, 250)
                .item("dark_dye", MARKS, 300)
                .item("dark_acorn", MARKS, 3000)
                .item("strange_old_lockpick", MARKS, 1000);

        // Brimhaven Agility Arena: Pirate Jackie the Fruit.
        shop("brimhaven_agility_arena", "Pirate Jackie's Ticket Exchange")
                .xp(Skill.AGILITY, 7, ARENA_TICKETS, 1, 1)
                .xp(Skill.AGILITY, 7, ARENA_TICKETS, 10, 1)
                .item("pirates_hook", ARENA_TICKETS, 800)
                .item("toadflax", ARENA_TICKETS, 3)
                .item("snapdragon", ARENA_TICKETS, 10);

        // Trouble Brewing: Honest Jimmy (pieces of eight; approx.).
        RewardShop tb = shop("trouble_brewing", "Honest Jimmy's Rewards");
        for (String c : new String[]{"blue", "green", "red", "purple"}) {
            tb.item(c + "_naval_shirt", EIGHT, 10).item(c + "_tricorn_hat", EIGHT, 5).item(c + "_navy_slacks", EIGHT, 10);
        }
        tb.item("bottle_of_rum", EIGHT, 20).item("the_stuff", EIGHT, 50);

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

        // Gnome Restaurant: Aluft Gianne snr.
        shop("gnome_restaurant", "Aluft Gianne's Rewards")
                .item("aluft_aloft_box", GNOME_TOKENS, 1)
                .item("gnome_scarf", GNOME_TOKENS, 10)
                .item("gnome_goggles", GNOME_TOKENS, 10)
                .item("mint_cake", GNOME_TOKENS, 1);
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
