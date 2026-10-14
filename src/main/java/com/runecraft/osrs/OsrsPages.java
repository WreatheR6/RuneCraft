package com.runecraft.osrs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The OSRS collection log layout laid over the generated pages: boss pages with the OSRS name and item list
 * (merging the game's per-form/per-brother pages), extra OSRS items for raid pages, the kill-count keys of
 * each page and the removal of pages OSRS doesn't have. Items the game doesn't define are skipped, so a
 * page lists exactly the OSRS items that exist here.
 */
final class OsrsPages {
    private OsrsPages() {
    }

    /** A page definition: OSRS category/name, generated pages it replaces, kill-count keys and items in OSRS order. */
    private record Def(String category, String name, List<String> replaces, List<String> kc, List<String> items) {
    }

    private static final List<Def> DEFS = new ArrayList<>();
    /** Extra OSRS items appended to a generated page (page name -> items). */
    private static final List<Def> EXTEND = new ArrayList<>();
    /** Generated pages that have no OSRS counterpart. */
    static final Set<String> REMOVED = Set.of("Herbs", "Gems", "Rune altars", "Quest rewards", "Boss keys", "Hunter", "Rooftop Agility",
            "Superior slayer creatures", "Dragons", "Brimstone & Larran's chests", "Gemstone Crab",
            "Fumus", "Umbra", "Cruor", "Glacies");

    private static List<String> l(String... s) {
        return List.of(s);
    }

    private static void boss(String name, List<String> replaces, List<String> kc, String... items) {
        DEFS.add(new Def("Bosses", name, replaces, kc, List.of(items)));
    }

    private static void boss(String name, String... items) {
        DEFS.add(new Def("Bosses", name, l(name), null, List.of(items)));
    }

    static {
        boss("Abyssal Sire", "pet_abyssal_orphan", "unsired", "abyssal_head", "bludgeon_spine", "bludgeon_claw", "bludgeon_axon",
                "jar_of_miasma", "abyssal_dagger", "abyssal_whip");
        boss("Alchemical Hydra", "pet_ikkle_hydra", "hydras_eye", "hydras_fang", "hydras_heart", "hydra_tail", "hydra_leather",
                "hydra_claw", "dragon_thrownaxe", "dragon_knife", "jar_of_chemicals", "alchemical_hydra_heads");
        List<String> barrows = new ArrayList<>();
        for (String b : new String[]{"ahrims_hood", "ahrims_robetop", "ahrims_robeskirt", "ahrims_staff", "dharoks_helm",
                "dharoks_platebody", "dharoks_platelegs", "dharoks_greataxe", "guthans_helm", "guthans_platebody", "guthans_chainskirt",
                "guthans_warspear", "karils_coif", "karils_leathertop", "karils_leatherskirt", "karils_crossbow", "torags_helm",
                "torags_platebody", "torags_platelegs", "torags_hammers", "veracs_helm", "veracs_brassard", "veracs_plateskirt",
                "veracs_flail", "bolt_rack"}) {
            barrows.add(b);
        }
        DEFS.add(new Def("Bosses", "Barrows Chests", l("Ahrim the Blighted", "Dharok the Wretched", "Guthan the Infested",
                "Karil the Tainted", "Torag the Corrupted", "Verac the Defiled"), l("barrows_chests"), barrows));
        boss("Bryophyta", "bryophytas_essence");
        boss("Callisto and Artio", l("Callisto", "Artio"), l("callisto", "artio"), "callisto_cub", "tyrannical_ring", "dragon_pickaxe",
                "dragon_2h_sword", "claws_of_callisto", "voidwaker_hilt");
        boss("Cerberus", "pet_hellpuppy", "eternal_crystal", "pegasian_crystal", "primordial_crystal", "jar_of_souls", "smouldering_stone",
                "key_master_teleport");
        boss("Chaos Elemental", "pet_chaos_elemental", "dragon_pickaxe", "dragon_2h_sword");
        boss("Chaos Fanatic", "pet_chaos_elemental", "odium_shard_1", "malediction_shard_1");
        boss("Commander Zilyana", shards("pet_zilyana", "armadyl_crossbow", "saradomin_hilt", "saradomin_sword", "saradomins_light"));
        boss("Corporeal Beast", "pet_dark_core", "elysian_sigil", "spectral_sigil", "arcane_sigil", "holy_elixir", "spirit_shield",
                "jar_of_spirits");
        boss("Crazy Archaeologist", l("Crazy archaeologist"), null, "odium_shard_2", "malediction_shard_2", "fedora");
        boss("Dagannoth Kings", l("Dagannoth Rex", "Dagannoth Prime", "Dagannoth Supreme"),
                l("dagannoth_rex", "dagannoth_prime", "dagannoth_supreme"), "pet_dagannoth_prime", "pet_dagannoth_supreme",
                "pet_dagannoth_rex", "berserker_ring", "archers_ring", "seers_ring", "warrior_ring", "dragon_axe", "seercull",
                "mud_battlestaff");
        boss("Deranged archaeologist", l("Deranged archaeologist"), l("deranged_archaeologist"), "steel_ring");
        boss("The Gauntlet", l("Crystalline Hunllef", "Corrupted Hunllef"), l("crystalline_hunllef", "corrupted_hunllef"),
                "pet_youngllef", "crystal_armour_seed", "crystal_weapon_seed", "enhanced_crystal_weapon_seed", "gauntlet_cape");
        boss("General Graardor", shards("pet_general_graardor", "bandos_chestplate", "bandos_tassets", "bandos_boots", "bandos_hilt"));
        boss("Giant Mole", "pet_baby_mole", "mole_skin", "mole_claw");
        boss("Grotesque Guardians", l("Dusk", "Dawn"), l("dusk", "dawn"), "pet_noon", "black_tourmaline_core", "granite_gloves",
                "granite_ring", "granite_hammer", "jar_of_stone", "granite_dust");
        boss("Hespori", "bottomless_compost_bucket", "iasor_seed", "kronos_seed", "attas_seed");
        boss("Kalphite Queen", "pet_kalphite_princess", "kq_head", "jar_of_sand", "dragon_2h_sword", "dragon_chainbody", "dragon_pickaxe");
        boss("King Black Dragon", "prince_black_dragon", "kbd_heads", "dragon_pickaxe", "draconic_visage");
        boss("Kraken", "pet_kraken", "kraken_tentacle", "trident_of_the_seas", "jar_of_dirt");
        boss("Kree'arra", shards("pet_kreearra", "armadyl_helmet", "armadyl_chestplate", "armadyl_chainskirt", "armadyl_hilt"));
        boss("K'ril Tsutsaroth", shards("pet_kril_tsutsaroth", "staff_of_the_dead", "zamorakian_spear", "steam_battlestaff", "zamorak_hilt"));
        boss("Nex", "nexling", "ancient_hilt", "nihil_horn", "zaryte_vambraces", "torva_full_helm", "torva_platebody", "torva_platelegs",
                "nihil_shard");
        boss("The Nightmare", l("The Nightmare", "Phosani's Nightmare"), l("the_nightmare", "phosanis_nightmare"),
                "pet_little_nightmare", "inquisitors_mace", "inquisitors_great_helm", "inquisitors_hauberk", "inquisitors_plateskirt",
                "nightmare_staff", "volatile_orb", "harmonised_orb", "eldritch_orb", "jar_of_dreams", "slepey_tablet", "parasitic_egg");
        boss("Moons of Peril", l("Blue Moon", "Eclipse Moon", "Blood Moon"), l("blue_moon", "eclipse_moon", "blood_moon"),
                "eclipse_atlatl", "eclipse_moon_helm", "eclipse_moon_chestplate", "eclipse_moon_tassets", "dual_macuahuitl",
                "blood_moon_helm", "blood_moon_chestplate", "blood_moon_tassets", "blue_moon_spear", "blue_moon_helm",
                "blue_moon_chestplate", "blue_moon_tassets", "atlatl_dart");
        boss("Obor", "hill_giant_club");
        boss("Phantom Muspah", "pet_muphin", "venator_shard", "ancient_icon", "frozen_cache", "ancient_essence");
        boss("Sarachnis", "pet_sraracha", "jar_of_eyes", "giant_egg_sac_full", "sarachnis_cudgel");
        boss("Scorpia", "pet_scorpias_offspring", "odium_shard_3", "malediction_shard_3", "dragon_2h_sword");
        boss("Scurrius", "pet_scurry", "scurrius_spine");
        boss("Skotizo", "pet_skotos", "jar_of_darkness", "dark_claw", "dark_totem", "uncut_onyx", "ancient_shard");
        boss("Thermonuclear Smoke Devil", l("Thermonuclear smoke devil"), null, "pet_smoke_devil", "occult_necklace", "smoke_battlestaff", "dragon_chainbody", "jar_of_smoke");
        boss("Venenatis and Spindel", l("Venenatis", "Spindel"), l("venenatis", "spindel"), "pet_venenatis_spiderling",
                "treasonous_ring", "dragon_pickaxe", "dragon_2h_sword", "fangs_of_venenatis", "voidwaker_gem");
        boss("Vet'ion and Calvar'ion", l("Vet'ion", "Calvar'ion"), l("vetion", "calvarion"), "vetion_jr", "ring_of_the_gods",
                "dragon_pickaxe", "dragon_2h_sword", "skull_of_vetion", "voidwaker_blade");
        boss("Vorkath", "pet_vorki", "vorkaths_head", "draconic_visage", "skeletal_visage", "jar_of_decay", "dragonbone_necklace");
        boss("Zalcano", "pet_smolcano", "crystal_tool_seed", "zalcano_shard", "uncut_onyx");
        boss("Zulrah", "pet_snakeling", "tanzanite_mutagen", "magma_mutagen", "jar_of_swamp", "magic_fang", "serpentine_visage",
                "tanzanite_fang", "zul_andra_teleport", "uncut_onyx", "zulrahs_scales");
        // Newer boss pages (wiki "Collection log" page contents; see CollectionLogSnapshotTest).
        boss("Amoxliatl", "pet_moxi", "glacial_temotli", "pendant_of_ates", "frozen_tear");
        boss("Araxxor", "pet_nid", "araxyte_venom_sac", "spider_cave_teleport", "araxyte_fang", "noxious_point", "noxious_blade",
                "noxious_pommel", "araxyte_head", "jar_of_venom", "coagulated_venom");
        boss("Brutus", l("Brutus", "Demonic Brutus"), null, "pet_beef", "cow_slippers", "bottomless_milk_bucket", "mooleta");
        boss("Doom of Mokhaiotl", "pet_dom", "mokhaiotl_cloth", "eye_of_ayak", "avernic_treads", "demon_tear", "mokhaiotl_waystone");
        for (String[] dt2 : new String[][]{{"Duke Sucellus", "pet_baron", "ice_quartz", "magus_vestige", "eye_of_the_duke", "frozen_tablet"},
                {"The Leviathan", "pet_lilviathan", "smoke_quartz", "venator_vestige", "leviathans_lure", "scarred_tablet"},
                {"The Whisperer", "pet_wisp", "shadow_quartz", "bellator_vestige", "sirens_staff", "sirenic_tablet"},
                {"Vardorvis", "pet_butch", "blood_quartz", "ultor_vestige", "executioners_axe_head", "strangled_tablet"}}) {
            boss(dt2[0], dt2[1], "virtus_mask", "virtus_robe_top", "virtus_robe_bottom", "chromium_ingot", dt2[2], dt2[3], dt2[4], dt2[5],
                    "awakeners_orb");
        }
        boss("Fortis Colosseum", l("Sol Heredit"), null, "pet_smol_heredit", "dizanas_quiver", "sunfire_fanatic_cuirass",
                "sunfire_fanatic_chausses", "sunfire_fanatic_helm", "echo_crystal", "tonalztics_of_ralos", "sunfire_splinters", "uncut_onyx");
        boss("The Hueycoatl", "pet_huberte", "dragon_hunter_wand", "soiled_page", "hueycoatl_hide", "huasca_seed", "tome_of_earth");
        boss("Maggot King", "pet_maggot_marquess", "crimson_kisten", "elder_venator_fang");
        boss("Royal Titans", l("Branda the Fire Queen", "Eldric the Ice King"), null, "pet_bran", "deadeye_prayer_scroll",
                "mystic_vigour_prayer_scroll", "ice_element_staff_crown", "fire_element_staff_crown", "desiccated_page", "giantsoul_amulet");
        boss("Shellbane Gryphon", l("Shellbane gryphon"), null, "pet_gull", "belles_folly_tarnished", "jar_of_feathers", "gryphon_feather");
        boss("Yama", "pet_yami", "soulflame_horn", "oathplate_helm", "oathplate_chest", "oathplate_legs", "oathplate_shards", "dossier",
                "forgotten_lockbox", "chasm_teleport_scroll", "barrel_of_demonic_tallow", "rite_of_vile_transference");
        DEFS.add(new Def("Other", "Revenants", l(), null, l("revenant_cave_teleport", "bracelet_of_ethereum", "revenant_ether",
                "ancient_crystal", "ancient_emblem", "ancient_totem", "ancient_statuette", "ancient_medallion", "ancient_effigy",
                "ancient_relic", "craws_bow", "thammarons_sceptre", "viggoras_chainmace", "amulet_of_avarice")));
        // The Fight Caves are a minigame page in the game; OSRS lists it under Bosses with the same items.
        DEFS.add(new Def("Bosses", "The Fight Caves", l("TzTok-Jad", "Fight Caves"), l("tztok_jad"), l("fire_cape", "tzrek_jad")));
        DEFS.add(new Def("Bosses", "The Inferno", l("The Inferno"), l("tzkal_zuk"), l("infernal_cape", "jal_nib_rek")));
        DEFS.add(new Def("Other", "Slayer", l("Slayer"), null, l("crawling_hand", "cockatrice_head", "basilisk_head", "kurask_head",
                "abyssal_head", "imbued_heart", "eternal_gem", "dust_battlestaff", "mist_battlestaff", "abyssal_whip", "granite_maul",
                "granite_longsword", "leaf_bladed_sword", "leaf_bladed_battleaxe", "black_mask", "granite_boots", "granite_legs",
                "brine_sabre", "draconic_visage", "wyvern_visage", "dragon_boots", "dragon_chainbody", "occult_necklace",
                "smoke_battlestaff", "dark_bow", "abyssal_dagger", "warped_sceptre", "basilisk_jaw", "drakes_tooth", "drakes_claw",
                "dragon_harpoon", "dragon_sword", "mystic_robe_top_dark", "leaf_bladed_spear", "rune_full_helm", "dragon_warhammer",
                "dragon_platelegs", "dragon_full_helm", "uncut_onyx")));
        DEFS.add(new Def("Other", "Miscellaneous", l(), null, l("dragon_limbs", "dragon_metal_slice", "dragon_metal_lump",
                "draconic_visage", "lava_scale", "brimstone_key", "larran_key")));

        EXTEND.add(new Def("Raids", "Chambers of Xeric", null, l("great_olm"), l("metamorphic_dust", "twisted_ancestral_colour_kit")));
        EXTEND.add(new Def("Raids", "Theatre of Blood", null, l("verzik_vitur", "verzik_vitur_p2", "verzik_vitur_p3"),
                l("sanguine_dust", "holy_ornament_kit", "sanguine_ornament_kit")));
        EXTEND.add(new Def("Raids", "Tombs of Amascut", null, l("tumekens_warden"), l("thread_of_elidinis", "eye_of_the_corruptor",
                "jewel_of_the_sun", "breach_of_the_scarab", "cursed_phalanx", "masori_crafting_kit", "menaphite_ornament_kit",
                "remnant_of_akkha", "remnant_of_baba", "remnant_of_kephri", "remnant_of_zebak", "ancient_remnant")));
        for (String tier : new String[]{"Beginner", "Easy", "Medium", "Hard", "Elite", "Master"}) {
            EXTEND.add(new Def("Clues", tier + " Treasure Trails", null, l("clue_" + tier.toLowerCase(java.util.Locale.ROOT)), l()));
        }
        EXTEND.add(new Def("Minigames", "Barbarian Assault", null, l("penance_queen"), l()));
    }

    /** Items followed by the three godsword shards (God Wars Dungeon general pages). */
    private static String[] shards(String... items) {
        List<String> out = new ArrayList<>(List.of(items));
        out.addAll(List.of("godsword_shard_1", "godsword_shard_2", "godsword_shard_3"));
        return out.toArray(new String[0]);
    }

    private static List<String> existing(List<String> items) {
        Set<String> out = new LinkedHashSet<>();
        for (String i : items) {
            if (ItemCatalog.exists(i)) {
                out.add(i);
            }
        }
        return new ArrayList<>(out);
    }

    static void apply(List<CollectionLogData.Page> pages, Map<String, List<String>> kc) {
        pages.removeIf(p -> REMOVED.contains(p.name()));
        for (Def d : DEFS) {
            List<String> kcKeys = new ArrayList<>();
            int at = -1;
            for (Iterator<CollectionLogData.Page> it = pages.iterator(); it.hasNext(); ) {
                CollectionLogData.Page p = it.next();
                if (d.replaces().contains(p.name()) || p.name().equals(d.name())) {
                    if (at < 0) {
                        at = pages.indexOf(p);
                    }
                    kcKeys.addAll(kc.getOrDefault(p.name(), List.of()));
                    it.remove();
                }
            }
            for (String r : d.replaces()) {
                for (String k : kc.getOrDefault(r, List.of())) {
                    if (!kcKeys.contains(k)) {
                        kcKeys.add(k);
                    }
                }
            }
            CollectionLogData.Page page = new CollectionLogData.Page(d.category(), d.name(), List.copyOf(existing(d.items())));
            if (page.items().isEmpty()) {
                continue;
            }
            if (at < 0 || at > pages.size()) {
                pages.add(page);
            } else {
                pages.add(at, page);
            }
            kc.put(d.name(), d.kc() != null ? d.kc() : kcKeys);
        }
        for (Def e : EXTEND) {
            for (int i = 0; i < pages.size(); i++) {
                CollectionLogData.Page p = pages.get(i);
                if (p.name().equals(e.name())) {
                    Set<String> items = new LinkedHashSet<>(p.items());
                    items.addAll(existing(e.items()));
                    pages.set(i, new CollectionLogData.Page(p.category(), p.name(), List.copyOf(items)));
                    kc.put(p.name(), e.kc());
                }
            }
        }
    }
}
