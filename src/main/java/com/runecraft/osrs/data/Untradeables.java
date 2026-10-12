package com.runecraft.osrs.data;

import com.runecraft.osrs.Skill;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Equipment that OSRS does not let you trade (Grand Exchange or player trade): minigame and boss rewards earned
 * in person (fire/infernal cape, defenders, void, fighter torso), skill and achievement capes, diary rewards,
 * quest rewards, skilling outfits and untradeable boss uniques.
 */
public final class Untradeables {
    private static final Set<String> IDS = new HashSet<>();

    static {
        for (Skill s : Skill.values()) {
            IDS.add(s.id() + "_cape");
            IDS.add(s.id() + "_cape_t");
        }
        add("max_cape", "quest_point_cape", "achievement_diary_cape", "fire_cape", "infernal_cape",
                "saradomin_cape", "guthix_cape", "zamorak_cape", "imbued_saradomin_cape", "imbued_guthix_cape",
                "imbued_zamorak_cape", "avas_attractor", "avas_accumulator", "cape_of_legends");
        // Warriors' Guild defenders, Theatre of Blood's avernic defender.
        add("bronze_defender", "iron_defender", "steel_defender", "black_defender", "mithril_defender", "adamant_defender",
                "rune_defender", "dragon_defender", "avernic_defender");
        // Pest Control and Barbarian Assault.
        add("void_knight_top", "void_knight_robe", "void_melee_helm", "void_ranger_helm", "void_mage_helm", "elite_void_top",
                "elite_void_robe", "void_knight_mace", "void_knight_gloves", "fighter_torso", "penance_skirt", "fighter_hat",
                "ranger_hat", "runner_hat", "healer_hat", "runner_boots");
        // Recipe for Disaster gloves.
        add("rfd_bronze_gloves", "rfd_iron_gloves", "rfd_steel_gloves", "rfd_black_gloves", "rfd_mithril_gloves",
                "rfd_adamant_gloves", "rfd_rune_gloves", "rfd_dragon_gloves", "barrows_gloves");
        // Achievement diary rewards.
        for (String d : new String[]{"ardougne_cloak", "desert_amulet", "falador_shield", "fremennik_sea_boots",
                "kandarin_headgear", "karamja_gloves", "radas_blessing", "explorers_ring", "morytania_legs", "varrock_armour",
                "western_banner", "wilderness_sword"}) {
            for (int tier = 1; tier <= 4; tier++) {
                IDS.add(d + "_" + tier);
            }
        }
        // Quest rewards.
        add("silverlight", "darklight", "excalibur", "dramen_staff", "lunar_staff", "ibans_staff", "ghostspeak_amulet",
                "ivandis_flail", "blisterwood_flail", "xerics_talisman", "drakans_medallion");
        // Boss/slayer items that become untradeable once made.
        add("slayer_helmet", "slayer_helmet_i", "abyssal_tentacle", "ferocious_gloves", "crystal_helm", "crystal_body",
                "crystal_legs", "harmonised_nightmare_staff", "volatile_nightmare_staff", "eldritch_nightmare_staff",
                "colossal_blade", "saradomin_halo", "zamorak_halo");
        // Skilling outfits.
        add("pyromancer_hood", "pyromancer_garb", "pyromancer_robe", "pyromancer_boots", "bruma_torch", "hat_of_the_eye",
                "robe_top_of_the_eye", "robe_bottoms_of_the_eye", "boots_of_the_eye", "prospector_helmet", "prospector_jacket",
                "prospector_legs", "prospector_boots", "angler_hat", "angler_top", "angler_waders", "angler_boots",
                "carpenters_helmet", "carpenters_shirt", "carpenters_trousers", "carpenters_boots", "farmers_strawhat",
                "farmers_jacket", "farmers_boro_trousers", "farmers_boots", "smiths_tunic", "smiths_trousers", "smiths_boots",
                "rogue_mask", "rogue_top", "rogue_trousers", "rogue_boots", "guild_hunter_headwear", "guild_hunter_top",
                "guild_hunter_legs", "guild_hunter_boots", "shayzien_helm_5", "shayzien_platebody_5", "shayzien_greaves_5",
                "shayzien_boots_5");
    }

    private Untradeables() {
    }

    private static void add(String... ids) {
        Collections.addAll(IDS, ids);
    }

    public static boolean untradeable(String id) {
        return IDS.contains(id);
    }

    public static Set<String> all() {
        return Collections.unmodifiableSet(IDS);
    }
}
