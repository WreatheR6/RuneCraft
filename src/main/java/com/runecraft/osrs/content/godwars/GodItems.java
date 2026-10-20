package com.runecraft.osrs.content.godwars;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Items devoted to each god (OSRS Wiki "God Wars Dungeon", God-related protection: snapshot in
 * {@code src/test/resources/osrs/godwars/god_items.tsv}, item ids derived from the names, diffed by {@code GwdDungeonTest}).
 * A God Wars follower leaves alone a player wearing at least one item of its god; a general's altar gives one Prayer
 * point above the maximum per item of its god worn; a Zarosian item worn in the Ancient Prison can earn extra essence.
 * Ids of items RuneCraft doesn't have yet never match.
 */
public final class GodItems {
    private GodItems() {
    }

    private static final Map<String, Set<String>> ITEMS = new LinkedHashMap<>();

    static {
        ITEMS.put("armadyl", Set.of(
            "armadyl_godsword", "armadyl_godsword_deadman", "armadyl_crossbow", "armadyl_crozier", "craws_bow",
            "webweaver_bow", "book_of_law", "armadyl_kiteshield", "armadyl_dhide_shield", "armadyl_helmet", "armadyl_coif",
            "armadyl_full_helm", "armadyl_mitre", "armadyl_halo", "armadyl_cloak", "armadyl_chestplate",
            "armadyl_platebody", "armadyl_dhide_body", "armadyl_robe_top", "armadyl_chainskirt", "armadyl_platelegs",
            "armadyl_chaps", "armadyl_plateskirt", "armadyl_robe_legs", "armadyl_bracers", "armadyl_dhide_boots",
            "honourable_blessing", "armadyl_pendant", "armadyl_stole"));
        ITEMS.put("bandos", Set.of(
            "ancient_mace", "bandos_godsword", "bandos_crozier", "book_of_war", "bandos_kiteshield", "bandos_dhide_shield",
            "bandos_full_helm", "bandos_coif", "bandos_mitre", "bandos_halo", "bandos_cloak", "bandos_chestplate",
            "bandos_platebody", "bandos_dhide_body", "bandos_robe_top", "bandos_tassets", "bandos_platelegs",
            "bandos_chaps", "bandos_plateskirt", "bandos_robe_legs", "bandos_bracers", "bandos_dhide_boots",
            "bandos_boots", "guardian_boots", "echo_boots", "war_blessing", "bandos_stole"));
        ITEMS.put("saradomin", Set.of(
            "saradomin_godsword", "saradomin_sword", "rune_scimitar_saradomin", "staff_of_light",
            "saradomins_blessed_sword", "saradomin_staff", "saradomin_crozier", "saradomin_mjolnir", "hallowed_hammer",
            "holy_scythe_of_vitur", "holy_ghrazi_rapier", "holy_sanguinesti_staff", "sunspear", "holy_book",
            "saradomin_kiteshield", "rune_kiteshield_saradomin", "saradomin_dhide_shield", "hallowed_focus",
            "saradomin_coif", "saradomin_mitre", "saradomin_full_helm", "saradomin_halo", "saradomin_max_hood",
            "imbued_saradomin_max_hood", "justiciar_faceguard", "rune_heraldic_helm_saradomin", "saradomin_cape",
            "saradomin_cloak", "saradomin_max_cape", "imbued_saradomin_cape", "imbued_saradomin_max_cape",
            "imbued_saradomin_cape_deadman", "saradomin_robe_top", "saradomin_dhide_body", "monks_robe_top",
            "saradomin_platebody", "justiciar_chestguard", "saradomin_robe_legs", "saradomin_chaps", "monks_robe",
            "saradomin_platelegs", "saradomin_plateskirt", "justiciar_legguards", "holy_wraps", "saradomin_bracers",
            "saradomin_dhide_boots", "holy_sandals", "holy_moleys", "devout_boots", "holy_blessing", "hallowed_grapple",
            "holy_symbol", "saradomin_stole", "hallowed_symbol", "hallowed_ring", "ring_of_endurance"));
        ITEMS.put("zamorak", Set.of(
            "zamorak_godsword", "zamorakian_spear", "zamorakian_hasta", "zamorak_staff", "rune_scimitar_zamorak",
            "zamorak_crozier", "zamorak_mjolnir", "staff_of_the_dead", "staff_of_light", "staff_of_balance",
            "toxic_staff_of_the_dead", "dragon_hunter_lance", "thammarons_sceptre_a", "thammarons_sceptre",
            "accursed_sceptre_a", "accursed_sceptre", "viggoras_chainmace", "ursine_chainmace", "inquisitors_mace",
            "unholy_book", "zamorak_kiteshield", "rune_kiteshield_zamorak", "zamorak_dhide_shield", "zamorak_coif",
            "zamorak_mitre", "elder_chaos_hood", "zamorak_full_helm", "zamorak_halo", "zamorak_max_hood",
            "imbued_zamorak_max_hood", "inquisitors_great_helm", "oathplate_helm", "rune_heraldic_helm_zamorak",
            "dagonhai_hat", "elite_black_full_helm", "dark_squall_hood", "zamorak_cape", "zamorak_cloak",
            "zamorak_max_cape", "imbued_zamorak_cape", "imbued_zamorak_max_cape", "imbued_zamorak_cape_deadman",
            "zamorak_robe_top", "zamorak_monk_top", "elder_chaos_top", "zamorak_dhide_body", "zamorak_platebody",
            "inquisitors_hauberk", "oathplate_chest", "dagonhai_robe_top", "elite_black_platebody", "dark_squall_robe_top",
            "zamorak_robe_legs", "zamorak_monk_bottom", "elder_chaos_robe", "zamorak_chaps", "zamorak_platelegs",
            "zamorak_plateskirt", "inquisitors_plateskirt", "oathplate_legs", "dagonhai_robe_bottom",
            "elite_black_platelegs", "dark_squall_robe_bottom", "zamorak_bracers", "zamorak_dhide_boots",
            "unholy_blessing", "unholy_symbol", "zamorak_stole"));
        ITEMS.put("zaros", Set.of(
            "ancient_godsword", "ancient_staff", "ancient_sceptre", "ancient_crozier", "venator_bow", "zaryte_crossbow",
            "book_of_darkness", "ancient_kiteshield", "ancient_dhide_shield", "ancient_coif", "ancient_full_helm",
            "ancient_mitre", "ancient_halo", "ancient_ceremonial_mask", "torva_full_helm", "virtus_mask",
            "hood_of_darkness", "ancient_cloak", "ancient_platebody", "ancient_dhide_body", "ancient_robe_top",
            "torva_platebody", "virtus_robe_top", "ancient_ceremonial_top", "robe_top_of_darkness", "ancient_platelegs",
            "ancient_chaps", "ancient_plateskirt", "ancient_robe_legs", "torva_platelegs", "virtus_robe_bottom",
            "ancient_ceremonial_legs", "robe_bottom_of_darkness", "ancient_bracers", "zaryte_vambraces",
            "ancient_ceremonial_gloves", "gloves_of_darkness", "ancient_dhide_boots", "ancient_ceremonial_boots",
            "boots_of_darkness", "ancient_blessing", "ancient_stole"));
    }

    /** The item ids devoted to a god ({@code "armadyl"}, {@code "bandos"}, {@code "saradomin"}, {@code "zamorak"} or {@code "zaros"}). */
    public static Set<String> items(String god) {
        return ITEMS.getOrDefault(god, Set.of());
    }

    /** Every god's items. */
    public static Map<String, Set<String>> all() {
        return java.util.Collections.unmodifiableMap(ITEMS);
    }

    /** How many of the worn item ids are devoted to the god. */
    public static int count(String god, Collection<String> worn) {
        Set<String> set = items(god);
        int n = 0;
        for (String id : worn) {
            if (id != null && set.contains(id)) {
                n++;
            }
        }
        return n;
    }

    /** True if any worn item is devoted to the god. */
    public static boolean wears(String god, Collection<String> worn) {
        return count(god, worn) > 0;
    }
}
