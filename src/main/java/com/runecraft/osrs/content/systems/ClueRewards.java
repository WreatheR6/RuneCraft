package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentDef.Slot;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.systems.ClueScrolls.Tier;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.runecraft.osrs.content.systems.Defs.req;
import static com.runecraft.osrs.content.systems.Defs.st;

/**
 * Treasure trail reward items and casket tables. The tables are the OSRS Wiki's reward casket pages, per reward roll
 * ({@link ClueRewardData}: uniques, mega-rares, the standard table and the shared rewards, Mod Tide's 2019 rates);
 * a casket rolls the tier's number of rewards (beginner 1-3, easy 2-4, medium 3-5, hard 4-6, elite 4-6, master 5-7)
 * plus the tertiary master clue (easy 1/50, medium 1/30, hard 1/15, elite 1/5) and, from master caskets, the
 * Bloodhound (1/1,000). Rows whose item this world lacks are left out and the rest of the roll scaled up.
 */
public final class ClueRewards {
    public record Loot(String item, int min, int max, int weight) {
    }

    public record Reward(String item, int count, boolean noted) {
        public Reward(String item, int count) {
            this(item, count, false);
        }
    }

    /** One row of a casket table: item, quantity range, noted, the probability per roll and the wiki section. */
    public record Row(String section, String item, int min, int max, boolean noted, double chance) {
    }

    /** Items the item definitions below declare as a tier's uniques (checked against the wiki tables by the tests). */
    private static final Map<Tier, List<String>> DECLARED = new EnumMap<>(Tier.class);
    private static final Map<Tier, List<Row>> TABLES = new EnumMap<>(Tier.class);

    static {
        for (Tier t : Tier.values()) {
            DECLARED.put(t, new ArrayList<>());
        }
    }

    private ClueRewards() {
    }

    // ---------------- Item definitions ----------------

    private static void unique(Tier t, String id) {
        if (!DECLARED.get(t).contains(id)) {
            DECLARED.get(t).add(id);
        }
    }

    private static void mega(Tier t, String id, int unused) {
        unique(t, id);
    }

    /** The Mimic casket (elite 1/35, master 1/15 when enabled at Watson's strange casket); its casket is saved on the stack. */
    public static final String MIMIC_ITEM = "mimic";
    /** Bloodhound: master casket tertiary, 1/1,000 (OSRS Wiki "Reward casket (master)"). */
    public static final int BLOODHOUND_ONE_IN = 1000;

    private static final EquipmentStats ZERO = EquipmentStats.NONE;

    private static void cosmeticArmour(ItemSink items, Tier t, String id, String name, String shape, int color, Slot slot, int value) {
        Defs.armour(items, id, name, shape, color, "leather", value, slot, ZERO, Map.of()).examine("A treasure trail reward.");
        unique(t, id);
    }

    private static void cosmeticWorn(ItemSink items, Tier t, String id, String name, String shape, int color, Jewellery.Slot slot, int value) {
        Defs.worn(items, id, name, shape, color, value, slot, ZERO, Map.of(), Jewellery.Perk.NONE, List.of()).examine("A treasure trail reward.");
        unique(t, id);
    }

    private static void trimmedPlate(ItemSink items, Tier t, String metal, int color, int value) {
        for (String trim : new String[]{"t", "g"}) {
            for (Defs.Piece p : Defs.Piece.values()) {
                String id = metal + "_" + Defs.suffix(p) + "_" + trim;
                String name = Defs.cap(metal) + " " + Defs.label(p) + " (" + trim + ")";
                Defs.armour(items, id, name, Defs.shape(p), trim.equals("g") ? 0xD8B030 : color, metal, value,
                        Defs.slot(p), Defs.plate(metal, p), req(Skill.DEFENCE, Defs.plateLevel(metal)))
                        .examine(trim.equals("t") ? "Trimmed armour from a treasure trail." : "Gold-trimmed armour from a treasure trail.");
                unique(t, id);
            }
        }
    }

    private static void godPlate(ItemSink items, Tier t, String god, int color) {
        for (Defs.Piece p : Defs.Piece.values()) {
            String id = god + "_" + Defs.suffix(p);
            Defs.armour(items, id, Defs.cap(god) + " " + Defs.label(p), Defs.shape(p), color, "rune", 60000, Defs.slot(p),
                    Defs.plate("rune", p), req(Skill.DEFENCE, 40)).examine("Rune armour blessed by " + Defs.cap(god) + ".");
            unique(t, id);
        }
    }

    private static void dhideTrim(ItemSink items, Tier t, String colour) {
        for (String trim : new String[]{"t", "g"}) {
            String body = colour + "_dhide_body_" + trim;
            String chaps = colour + "_dhide_chaps_" + trim;
            int c = trim.equals("g") ? 0xD8B030 : Defs.dhideColor(colour);
            Defs.armour(items, body, Defs.cap(colour) + " d'hide body (" + trim + ")", "leather_body", c, "leather", 8000, Slot.CHEST,
                    Defs.dhide(colour, true), req(Skill.RANGED, Defs.dhideLevel(colour), Skill.DEFENCE, 40));
            Defs.armour(items, chaps, Defs.cap(colour) + " d'hide chaps (" + trim + ")", "chaps", c, "leather", 4000, Slot.LEGS,
                    Defs.dhide(colour, false), req(Skill.RANGED, Defs.dhideLevel(colour)));
            unique(t, body);
            unique(t, chaps);
        }
    }

    public static void items(ItemSink items) {
        // ---------------- Reward caskets ----------------
        for (Tier t : Tier.values()) {
            items.add("reward_casket_" + t.id(), "Reward casket (" + t.id() + ")", Kind.SIMPLE, "casket", t.color()).value(50).stack(1)
                    .examine("Open it to receive your treasure trail reward.");
            // Scroll boxes (after X Marks the Spot): stackable clue scrolls, opened one at a time.
            items.add(scrollBox(t), "Scroll box (" + t.id() + ")", Kind.SIMPLE, "casket", t.color()).value(0)
                    .examine("A box containing a clue scroll.");
        }
        items.add(MIMIC_ITEM, "Mimic", Kind.SIMPLE, "casket", 0x8A2A2A).value(0).stack(1)
                .examine("Present it to the strange casket in Watson's house to fight the Mimic.");
        ClueRewardItems.items(items);
        // ---------------- Beginner ----------------
        Tier B = Tier.BEGINNER;
        cosmeticArmour(items, B, "mole_slippers", "Mole slippers", "boots", 0x5A4A3A, Slot.FEET, 5000);
        cosmeticArmour(items, B, "frog_slippers", "Frog slippers", "boots", 0x40A040, Slot.FEET, 5000);
        cosmeticArmour(items, B, "bear_feet", "Bear feet", "boots", 0x6A4A2A, Slot.FEET, 5000);
        cosmeticArmour(items, B, "demon_feet", "Demon feet", "boots", 0xA02020, Slot.FEET, 5000);
        cosmeticWorn(items, B, "jester_cape", "Jester cape", "cape", 0xC03080, Jewellery.Slot.CAPE, 5000);
        cosmeticWorn(items, B, "shoulder_parrot", "Shoulder parrot", "cape", 0x30C040, Jewellery.Slot.CAPE, 5000);
        Defs.armour(items, "monks_robe_top_t", "Monk's robe top (t)", "leather_body", 0x7A5A3A, "leather", 5000, Slot.CHEST,
                st(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6), Map.of());
        Defs.armour(items, "monks_robe_t", "Monk's robe (t)", "chaps", 0x7A5A3A, "leather", 5000, Slot.LEGS,
                st(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5), Map.of());
        unique(B, "monks_robe_top_t");
        unique(B, "monks_robe_t");
        unique(B, "amulet_of_defence_t");
        cosmeticArmour(items, B, "sandwich_lady_hat", "Sandwich lady hat", "cowl", 0xE0E0E0, Slot.HEAD, 5000);
        cosmeticArmour(items, B, "sandwich_lady_top", "Sandwich lady top", "leather_body", 0xE0E0E0, Slot.CHEST, 5000);
        cosmeticArmour(items, B, "sandwich_lady_bottom", "Sandwich lady bottom", "chaps", 0x303060, Slot.LEGS, 5000);
        Defs.simple(items, "rune_scimitar_ornament_kit_guthix", "Rune scimitar ornament kit (guthix)", "package", 0x30A030, 5000);
        Defs.simple(items, "rune_scimitar_ornament_kit_saradomin", "Rune scimitar ornament kit (saradomin)", "package", 0x3050C0, 5000);
        Defs.simple(items, "rune_scimitar_ornament_kit_zamorak", "Rune scimitar ornament kit (zamorak)", "package", 0xA02020, 5000);
        unique(B, "rune_scimitar_ornament_kit_guthix");
        unique(B, "rune_scimitar_ornament_kit_saradomin");
        unique(B, "rune_scimitar_ornament_kit_zamorak");

        // ---------------- Easy ----------------
        Tier E = Tier.EASY;
        trimmedPlate(items, E, "black", 0x2A2A2A, 3500);
        for (String trim : new String[]{"t", "g"}) {
            int c = trim.equals("g") ? 0xD8B030 : 0x8A6A3A;
            Defs.armour(items, "studded_body_" + trim, "Studded body (" + trim + ")", "leather_body", c, "leather", 3000, Slot.CHEST,
                    st(0, 0, 0, -4, 8, 18, 25, 22, 8, 25, 0, 0, 0, 0), req(Skill.RANGED, 20, Skill.DEFENCE, 20));
            Defs.armour(items, "studded_chaps_" + trim, "Studded chaps (" + trim + ")", "chaps", c, "leather", 2000, Slot.LEGS,
                    st(0, 0, 0, -5, 6, 15, 16, 17, 6, 16, 0, 0, 0, 0), req(Skill.RANGED, 20));
            unique(E, "studded_body_" + trim);
            unique(E, "studded_chaps_" + trim);
            int w = trim.equals("g") ? 0xD8B030 : 0x3050C0;
            Defs.armour(items, "blue_wizard_hat_" + trim, "Blue wizard hat (" + trim + ")", "cowl", w, "leather", 2000, Slot.HEAD,
                    st(0, 0, 0, 2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0), Map.of());
            Defs.armour(items, "blue_wizard_robe_" + trim, "Blue wizard robe (" + trim + ")", "leather_body", w, "leather", 2000, Slot.CHEST,
                    st(0, 0, 0, 3, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0), Map.of());
            unique(E, "blue_wizard_hat_" + trim);
            unique(E, "blue_wizard_robe_" + trim);
        }
        Defs.armour(items, "highwayman_mask", "Highwayman mask", "cowl", 0x202020, "leather", 3000, Slot.HEAD, ZERO, Map.of());
        unique(E, "highwayman_mask");
        for (String c : new String[]{"blue", "black", "white", "red"}) {
            int col = switch (c) {
                case "blue" -> 0x3050C0;
                case "black" -> 0x202020;
                case "white" -> 0xE0E0E0;
                default -> 0xC03030;
            };
            cosmeticArmour(items, E, c + "_beret", Defs.cap(c) + " beret", "cowl", col, Slot.HEAD, 3000);
        }
        unique(E, "amulet_of_magic_t");
        cosmeticArmour(items, E, "red_elegant_shirt", "Red elegant shirt", "leather_body", 0xC03030, Slot.CHEST, 3000);
        cosmeticArmour(items, E, "red_elegant_legs", "Red elegant legs", "chaps", 0xC03030, Slot.LEGS, 3000);
        cosmeticArmour(items, E, "golden_chef_hat", "Golden chef's hat", "cowl", 0xE8C030, Slot.HEAD, 3000);
        cosmeticArmour(items, E, "golden_apron", "Golden apron", "leather_body", 0xE8C030, Slot.CHEST, 3000);
        Defs.melee(items, "black_cane", "Black cane", "club", 0x202020, "black", 3000, st(0, 0, 10, 0, 0, 0, 0, 0, 0, 0, 12, 0, 0, 0),
                req(Skill.ATTACK, 10), 4, AttackType.CRUSH);
        unique(E, "black_cane");
        Defs.bow(items, "willow_comp_bow", "Willow comp bow", "shortbow", 0x9A8A4A, 3000, st(0, 0, 0, 0, 22, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                req(Skill.RANGED, 20), 5);
        unique(E, "willow_comp_bow");
        unique(E, "black_pickaxe");
        trimmedPlate(items, E, "bronze", 0x8A5A2A, 500);
        trimmedPlate(items, E, "iron", 0x6A6A6A, 800);
        trimmedPlate(items, E, "steel", 0x9A9AA0, 1500);
        for (String trim : new String[]{"t", "g"}) {
            int w = trim.equals("g") ? 0xD8B030 : 0x202020;
            Defs.armour(items, "black_wizard_hat_" + trim, "Black wizard hat (" + trim + ")", "cowl", w, "leather", 2000, Slot.HEAD,
                    st(0, 0, 0, 2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0), Map.of());
            Defs.armour(items, "black_wizard_robe_" + trim, "Black wizard robe (" + trim + ")", "leather_body", w, "leather", 2000,
                    Slot.CHEST, st(0, 0, 0, 3, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0), Map.of());
            unique(E, "black_wizard_hat_" + trim);
            unique(E, "black_wizard_robe_" + trim);
        }
        Defs.armour(items, "leather_body_g", "Leather body (g)", "leather_body", 0xD8B030, "leather", 1000, Slot.CHEST,
                st(0, 0, 0, -2, 2, 8, 9, 10, 4, 9, 0, 0, 0, 0), req(Skill.DEFENCE, 10));
        Defs.armour(items, "leather_chaps_g", "Leather chaps (g)", "chaps", 0xD8B030, "leather", 1000, Slot.LEGS,
                st(0, 0, 0, -2, 4, 2, 2, 2, 3, 2, 0, 0, 0, 0), Map.of());
        unique(E, "leather_body_g");
        unique(E, "leather_chaps_g");
        for (String[] shirt : new String[][]{{"red", "C03030"}, {"blue", "3050C0"}, {"green", "30A030"}, {"black", "202020"},
                {"purple", "7A3AA0"}}) {
            cosmeticArmour(items, E, "bobs_" + shirt[0] + "_shirt", "Bob's " + shirt[0] + " shirt", "leather_body",
                    Integer.parseInt(shirt[1], 16), Slot.CHEST, 2000);
        }
        cosmeticArmour(items, E, "a_powdered_wig", "A powdered wig", "cowl", 0xF0F0F0, Slot.HEAD, 2000);
        cosmeticArmour(items, E, "flared_trousers", "Flared trousers", "chaps", 0x6A4AA0, Slot.LEGS, 2000);
        cosmeticArmour(items, E, "pantaloons", "Pantaloons", "chaps", 0xE0D0A0, Slot.LEGS, 2000);
        cosmeticArmour(items, E, "sleeping_cap", "Sleeping cap", "cowl", 0x3050C0, Slot.HEAD, 2000);
        Defs.melee(items, "ham_joint", "Ham joint", "club", 0xC08070, "iron", 2000, st(0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0),
                Map.of(), 4, AttackType.CRUSH);
        unique(E, "ham_joint");

        // ---------------- Medium ----------------
        Tier M = Tier.MEDIUM;
        trimmedPlate(items, M, "mithril", 0x4A4A7A, 6000);
        trimmedPlate(items, M, "adamant", 0x3A6A4A, 12000);
        dhideTrim(items, M, "green");
        Defs.armour(items, "ranger_boots", "Ranger boots", "boots", 0x3A7A3A, "leather", 200000, Slot.FEET,
                st(0, 0, 0, 0, 8, 2, 3, 4, 2, 0, 0, 0, 0, 0), req(Skill.RANGED, 40));
        mega(M, "ranger_boots", 1_133); // approx: OSRS ~1/1,133 per casket
        Defs.armour(items, "wizard_boots", "Wizard boots", "boots", 0x3050C0, "leather", 40000, Slot.FEET,
                st(0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), req(Skill.MAGIC, 20));
        Defs.armour(items, "holy_sandals", "Holy sandals", "boots", 0xE0D090, "leather", 20000, Slot.FEET,
                st(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3), Map.of());
        mega(M, "wizard_boots", 1_133); // approx
        mega(M, "holy_sandals", 1_133); // approx
        for (String c : new String[]{"red", "black", "brown"}) {
            int col = c.equals("red") ? 0xC03030 : c.equals("black") ? 0x202020 : 0x6A4A2A;
            Defs.armour(items, c + "_headband", Defs.cap(c) + " headband", "cowl", col, "leather", 5000, Slot.HEAD,
                    st(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0), Map.of());
            unique(M, c + "_headband");
        }
        unique(M, "amulet_of_strength_t");
        cosmeticArmour(items, M, "black_elegant_shirt", "Black elegant shirt", "leather_body", 0x202020, Slot.CHEST, 5000);
        cosmeticArmour(items, M, "black_elegant_legs", "Black elegant legs", "chaps", 0x202020, Slot.LEGS, 5000);
        cosmeticArmour(items, M, "white_elegant_blouse", "White elegant blouse", "leather_body", 0xE0E0E0, Slot.CHEST, 5000);
        cosmeticArmour(items, M, "white_elegant_skirt", "White elegant skirt", "chaps", 0xE0E0E0, Slot.LEGS, 5000);
        Defs.armour(items, "climbing_boots_g", "Climbing boots (g)", "boots", 0xD8B030, "leather", 10000, Slot.FEET,
                st(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0), Map.of());
        mega(M, "climbing_boots_g", 1_133); // approx
        Defs.armour(items, "spiked_manacles", "Spiked manacles", "boots", 0x6A6A6A, "leather", 10000, Slot.FEET,
                st(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0), Map.of());
        mega(M, "spiked_manacles", 1_133); // approx
        Defs.melee(items, "adamant_cane", "Adamant cane", "club", 0x3A6A4A, "adamant", 10000, st(0, 0, 25, 0, 0, 0, 0, 0, 0, 0, 23, 0, 0, 0),
                req(Skill.ATTACK, 30), 4, AttackType.CRUSH);
        unique(M, "adamant_cane");
        cosmeticArmour(items, M, "cat_mask", "Cat mask", "cowl", 0x303030, Slot.HEAD, 5000);
        cosmeticArmour(items, M, "penguin_mask", "Penguin mask", "cowl", 0x202020, Slot.HEAD, 5000);
        cosmeticArmour(items, M, "leprechaun_hat", "Leprechaun hat", "cowl", 0x30A030, Slot.HEAD, 5000);
        cosmeticArmour(items, M, "crier_hat", "Crier hat", "cowl", 0x6A2A2A, Slot.HEAD, 5000);
        cosmeticArmour(items, M, "crier_coat", "Crier coat", "leather_body", 0x6A2A2A, Slot.CHEST, 5000);
        Defs.melee(items, "crier_bell", "Crier bell", "club", 0xC0A040, "iron", 5000, ZERO, Map.of(), 4, AttackType.CRUSH);
        unique(M, "crier_bell");
        cosmeticArmour(items, M, "black_unicorn_mask", "Black unicorn mask", "cowl", 0x202020, Slot.HEAD, 5000);
        cosmeticArmour(items, M, "white_unicorn_mask", "White unicorn mask", "cowl", 0xF0F0F0, Slot.HEAD, 5000);
        cosmeticArmour(items, M, "wolf_mask", "Wolf mask", "cowl", 0x6A6A6A, Slot.HEAD, 5000);
        cosmeticWorn(items, M, "wolf_cloak", "Wolf cloak", "cape", 0x6A6A6A, Jewellery.Slot.CAPE, 5000);
        cosmeticArmour(items, M, "purple_elegant_shirt", "Purple elegant shirt", "leather_body", 0x7A3AA0, Slot.CHEST, 5000);
        cosmeticArmour(items, M, "purple_elegant_legs", "Purple elegant legs", "chaps", 0x7A3AA0, Slot.LEGS, 5000);
        Defs.armour(items, "cabbage_round_shield", "Cabbage round shield", "kiteshield", 0x6AA03A, "leather", 5000, Slot.SHIELD,
                st(0, 0, 0, 0, 0, 3, 4, 2, 0, 3, 0, 0, 0, 0), Map.of());
        unique(M, "cabbage_round_shield");

        // ---------------- Hard ----------------
        Tier H = Tier.HARD;
        trimmedPlate(items, H, "rune", 0x3A6A8A, 80000);
        godPlate(items, H, "saradomin", 0xE0E0F0);
        godPlate(items, H, "zamorak", 0xA02020);
        godPlate(items, H, "guthix", 0x30A030);
        dhideTrim(items, H, "blue");
        dhideTrim(items, H, "red");
        Defs.armour(items, "robin_hood_hat", "Robin hood hat", "cowl", 0x3A7A3A, "leather", 100000, Slot.HEAD,
                st(0, 0, 0, -10, 8, 4, 6, 8, 4, 4, 0, 0, 0, 0), req(Skill.RANGED, 40));
        unique(H, "robin_hood_hat");
        Defs.armour(items, "enchanted_hat", "Enchanted hat", "cowl", 0x303080, "leather", 30000, Slot.HEAD,
                st(0, 0, 0, 4, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0), req(Skill.MAGIC, 40));
        Defs.armour(items, "enchanted_top", "Enchanted top", "leather_body", 0x303080, "leather", 30000, Slot.CHEST,
                st(0, 0, 0, 20, 0, 0, 0, 0, 20, 0, 0, 0, 0, 0), req(Skill.MAGIC, 40, Skill.DEFENCE, 20));
        Defs.armour(items, "enchanted_robe", "Enchanted robe", "chaps", 0x303080, "leather", 30000, Slot.LEGS,
                st(0, 0, 0, 15, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0), req(Skill.MAGIC, 40, Skill.DEFENCE, 20));
        unique(H, "enchanted_hat");
        unique(H, "enchanted_top");
        unique(H, "enchanted_robe");
        unique(H, "amulet_of_glory_t_4");
        cosmeticArmour(items, H, "pirate_hat", "Pirate's hat", "cowl", 0x202020, Slot.HEAD, 20000);
        for (String c : new String[]{"tan", "dark", "black"}) {
            cosmeticArmour(items, H, c + "_cavalier", Defs.cap(c) + " cavalier", "cowl", c.equals("tan") ? 0xC0A070 : c.equals("dark") ? 0x5A4A3A : 0x202020,
                    Slot.HEAD, 20000);
        }
        Defs.bow(items, "magic_comp_bow", "Magic comp bow", "shortbow", 0x3A3A8A, 50000, st(0, 0, 0, 0, 71, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                req(Skill.RANGED, 50), 5);
        unique(H, "magic_comp_bow");
        Defs.melee(items, "rune_cane", "Rune cane", "club", 0x3A6A8A, "rune", 30000, st(0, 0, 38, 0, 0, 0, 0, 0, 0, 0, 37, 0, 0, 0),
                req(Skill.ATTACK, 40), 4, AttackType.CRUSH);
        unique(H, "rune_cane");
        for (String god : new String[]{"saradomin", "zamorak", "guthix", "bandos", "armadyl", "ancient"}) {
            for (int i = 1; i <= 4; i++) {
                String id = god + "_page_" + i;
                Defs.simple(items, id, Defs.cap(god) + " page " + i, "page", 0xE8D8A0, 15000).stack(1)
                        .examine("A page from the holy book of " + Defs.cap(god) + ".");
                unique(H, id);
            }
        }
        godPlate(items, H, "ancient", 0x8A3A3A);
        godPlate(items, H, "armadyl", 0xE0E0D0);
        godPlate(items, H, "bandos", 0x6A5A3A);
        // God d'hide (blessed dragonhide): body, chaps, coif, bracers and shield are hard rewards, boots elite.
        for (String god : new String[]{"saradomin", "zamorak", "guthix", "bandos", "armadyl", "ancient"}) {
            int c = switch (god) {
                case "saradomin" -> 0x3050C0;
                case "zamorak" -> 0xA02020;
                case "guthix" -> 0x30A030;
                case "bandos" -> 0x6A5A3A;
                case "armadyl" -> 0xE0E0D0;
                default -> 0x8A3A3A;
            };
            Map<Skill, Integer> r = req(Skill.RANGED, 70, Skill.DEFENCE, 40);
            Defs.armour(items, god + "_dhide_body", Defs.cap(god) + " d'hide body", "leather_body", c, "leather", 30000, Slot.CHEST,
                    st(0, 0, 0, -15, 30, 55, 47, 60, 50, 55, 0, 0, 0, 1), r);
            Defs.armour(items, god + "_chaps", Defs.cap(god) + " chaps", "chaps", c, "leather", 20000, Slot.LEGS,
                    st(0, 0, 0, -10, 17, 31, 25, 33, 28, 31, 0, 0, 0, 1), req(Skill.RANGED, 70));
            Defs.armour(items, god + "_coif", Defs.cap(god) + " coif", "cowl", c, "leather", 15000, Slot.HEAD,
                    st(0, 0, 0, -1, 7, 4, 7, 10, 4, 8, 0, 0, 0, 1), r);
            Defs.worn(items, god + "_bracers", Defs.cap(god) + " bracers", "vambraces", c, 4000, Jewellery.Slot.HANDS,
                    st(0, 0, 0, -10, 11, 6, 5, 7, 8, 0, 0, 0, 0, 1), r, Jewellery.Perk.NONE, List.of());
            Defs.armour(items, god + "_dhide_shield", Defs.cap(god) + " d'hide shield", "shield_round", c, "leather", 28334,
                    Slot.SHIELD, st(-15, -15, -11, -10, 7, 21, 18, 16, 15, 14, 0, 0, 0, 1), r);
            Defs.armour(items, god + "_dhide_boots", Defs.cap(god) + " d'hide boots", "boots", c, "leather", 9300, Slot.FEET,
                    st(0, 0, 0, -10, 7, 4, 4, 4, 4, 4, 0, 0, 0, 1), r);
            unique(H, god + "_dhide_body");
            unique(H, god + "_chaps");
            unique(H, god + "_coif");
            unique(H, god + "_bracers");
            unique(H, god + "_dhide_shield");
            unique(Tier.ELITE, god + "_dhide_boots");
        }
        cosmeticArmour(items, H, "pith_helmet", "Pith helmet", "cowl", 0xD0C090, Slot.HEAD, 20000);
        cosmeticWorn(items, H, "explorer_backpack", "Explorer backpack", "cape", 0x8A6A3A, Jewellery.Slot.CAPE, 20000);
        thirdAge(items);
        vestments(items);

        // ---------------- Elite ----------------
        Tier L = Tier.ELITE;
        for (Defs.Piece p : Defs.Piece.values()) {
            String id = "gilded_" + Defs.suffix(p);
            Defs.armour(items, id, "Gilded " + Defs.label(p), Defs.shape(p), 0xE8C030, "rune", 400000, Defs.slot(p),
                    Defs.plate("rune", p), req(Skill.DEFENCE, 40)).examine("Gilded rune armour from a treasure trail.");
            mega(L, id, 20_000); // approx
            mega(H, id, 30_000); // approx
        }
        Defs.melee(items, "gilded_scimitar", "Gilded scimitar", "scimitar", 0xE8C030, "rune", 300000,
                st(7, 45, -2, 0, 0, 0, 1, 0, 0, 0, 44, 0, 0, 0), req(Skill.ATTACK, 40), 4, AttackType.SLASH);
        Defs.armour(items, "gilded_boots", "Gilded boots", "boots", 0xE8C030, "rune", 200000, Slot.FEET,
                st(0, 0, 0, -3, -1, 12, 13, 14, 0, 0, 1, 0, 0, 0), req(Skill.DEFENCE, 40));
        mega(L, "gilded_scimitar", 20_000); // approx
        mega(L, "gilded_boots", 20_000); // approx
        dhideTrim(items, L, "black");
        Defs.armour(items, "rangers_tunic", "Rangers' tunic", "leather_body", 0x3A7A3A, "leather", 100000, Slot.CHEST,
                st(0, 0, 0, -10, 14, 22, 24, 26, 22, 30, 0, 0, 0, 0), req(Skill.RANGED, 40));
        Defs.armour(items, "rangers_tights", "Ranger's tights", "chaps", 0x3A7A3A, "leather", 100000, Slot.LEGS,
                st(0, 0, 0, -10, 9, 10, 12, 14, 10, 12, 0, 0, 0, 0), req(Skill.RANGED, 40));
        unique(L, "rangers_tunic");
        unique(L, "rangers_tights");
        Defs.worn(items, "ranger_gloves", "Ranger gloves", "gloves", 0x3A7A3A, 100000, Jewellery.Slot.HANDS,
                st(0, 0, 0, 0, 11, 0, 0, 0, 0, 0, 0, 0, 0, 0), req(Skill.RANGED, 40), Jewellery.Perk.NONE, List.of());
        Defs.worn(items, "holy_wraps", "Holy wraps", "gloves", 0xE0D090, 80000, Jewellery.Slot.HANDS,
                st(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3), Map.of(), Jewellery.Perk.NONE, List.of());
        unique(L, "ranger_gloves");
        unique(L, "holy_wraps");
        cosmeticArmour(items, L, "fremennik_kilt", "Fremennik kilt", "chaps", 0x6A3A2A, Slot.LEGS, 20000);
        Defs.armour(items, "lava_dragon_mask", "Lava dragon mask", "full_helm", 0x202020, "leather", 50000, Slot.HEAD,
                st(0, 0, 0, -1, -1, 1, 1, 1, 0, 0, 0, 0, 0, 0), Map.of());
        unique(L, "lava_dragon_mask");
        cosmeticArmour(items, L, "royal_crown", "Royal crown", "full_helm", 0xE8C030, Slot.HEAD, 30000);
        cosmeticArmour(items, L, "royal_gown_top", "Royal gown top", "leather_body", 0x8A2080, Slot.CHEST, 30000);
        cosmeticArmour(items, L, "royal_gown_bottom", "Royal gown bottom", "chaps", 0x8A2080, Slot.LEGS, 30000);
        Defs.melee(items, "royal_sceptre", "Royal sceptre", "club", 0xE8C030, "rune", 30000, st(0, 0, 10, 0, 0, 0, 0, 0, 0, 0, 8, 0, 0, 0),
                Map.of(), 4, AttackType.CRUSH);
        unique(L, "royal_sceptre");
        cosmeticArmour(items, L, "musketeer_hat", "Musketeer hat", "cowl", 0x5A3A6A, Slot.HEAD, 30000);
        cosmeticArmour(items, L, "deerstalker", "Deerstalker", "cowl", 0x8A6A3A, Slot.HEAD, 30000);
        cosmeticArmour(items, L, "afro", "Afro", "cowl", 0x2A1A0A, Slot.HEAD, 30000);
        cosmeticArmour(items, L, "big_pirate_hat", "Big pirate hat", "cowl", 0x202020, Slot.HEAD, 30000);
        unique(L, "ring_of_nature");
        Defs.simple(items, "dragon_scimitar_ornament_kit", "Dragon scimitar ornament kit", "package", 0xC03030, 200000).stack(1);
        unique(L, "dragon_scimitar_ornament_kit");
        for (String kit : new String[]{"dragon_full_helm", "dragon_chainbody", "dragon_legs_skirt", "dragon_sq_shield"}) {
            Defs.simple(items, kit + "_ornament_kit", Defs.cap(kit.replace('_', ' ')) + " ornament kit", "package", 0xC03030, 200000)
                    .stack(1);
            unique(L, kit + "_ornament_kit");
        }
        Defs.melee(items, "dragon_cane", "Dragon cane", "club", 0xC03030, "dragon", 60000,
                st(0, 0, 45, 0, 0, 0, 0, 0, 0, 0, 44, 0, 0, 0), req(Skill.ATTACK, 60), 4, AttackType.CRUSH); // approx stats
        unique(L, "dragon_cane");
        Defs.melee(items, "katana", "Katana", "longsword", 0xC0C0C8, "steel", 30000, st(4, 8, -2, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0),
                Map.of(), 4, AttackType.SLASH); // approx stats
        unique(L, "katana");
        Defs.armour(items, "briefcase", "Briefcase", "package", 0x5A3A2A, "iron", 30000, Slot.SHIELD, ZERO, Map.of()); // off-hand slot
        unique(L, "briefcase");
        Defs.armour(items, "giant_boot", "Giant boot", "boots", 0x6A4A2A, "iron", 30000, Slot.HEAD, ZERO, Map.of()); // worn on the head
        unique(L, "giant_boot");
        cosmeticArmour(items, L, "sagacious_spectacles", "Sagacious spectacles", "cowl", 0xC0C0C0, Slot.HEAD, 30000);
        cosmeticArmour(items, L, "top_hat", "Top hat", "cowl", 0x202020, Slot.HEAD, 30000);
        cosmeticArmour(items, L, "monocle", "Monocle", "cowl", 0xC0C0C0, Slot.HEAD, 30000);
        cosmeticArmour(items, L, "uris_hat", "Uri's hat", "cowl", 0x5A3A6A, Slot.HEAD, 30000);

        // ---------------- Master ----------------
        Tier S = Tier.MASTER;
        cosmeticArmour(items, S, "ankou_mask", "Ankou mask", "full_helm", 0x6A3A8A, Slot.HEAD, 80000);
        cosmeticArmour(items, S, "ankou_top", "Ankou top", "leather_body", 0x6A3A8A, Slot.CHEST, 80000);
        cosmeticArmour(items, S, "ankou_legs", "Ankou's leggings", "chaps", 0x6A3A8A, Slot.LEGS, 80000);
        cosmeticArmour(items, S, "ankou_socks", "Ankou socks", "boots", 0x6A3A8A, Slot.FEET, 80000);
        cosmeticWorn(items, S, "ankou_gloves", "Ankou gloves", "gloves", 0x6A3A8A, Jewellery.Slot.HANDS, 80000);
        cosmeticArmour(items, S, "mummys_head", "Mummy's head", "full_helm", 0xD8D0B0, Slot.HEAD, 80000);
        cosmeticArmour(items, S, "mummys_body", "Mummy's body", "leather_body", 0xD8D0B0, Slot.CHEST, 80000);
        cosmeticArmour(items, S, "mummys_legs", "Mummy's legs", "chaps", 0xD8D0B0, Slot.LEGS, 80000);
        cosmeticArmour(items, S, "mummys_feet", "Mummy's feet", "boots", 0xD8D0B0, Slot.FEET, 80000);
        cosmeticWorn(items, S, "mummys_hands", "Mummy's hands", "gloves", 0xD8D0B0, Jewellery.Slot.HANDS, 80000);
        items.add("bloodhound", "Bloodhound", Kind.SIMPLE, "pet", 0x8A5A3A).value(1).stack(1).examine("A loyal treasure-seeking hound.");
        mega(S, "bloodhound", 1_000);
        cosmeticArmour(items, S, "half_moon_spectacles", "Half moon spectacles", "cowl", 0xC0C0C0, Slot.HEAD, 80000);
        Defs.melee(items, "ale_of_the_gods", "Ale of the gods", "beer", 0xC08030, "rune", 80000, ZERO, Map.of(), 4, AttackType.CRUSH);
        unique(S, "ale_of_the_gods");
        cosmeticArmour(items, S, "fancy_tiara", "Fancy tiara", "cowl", 0xE8C030, Slot.HEAD, 80000);
        cosmeticArmour(items, S, "left_eye_patch", "Left eye patch", "cowl", 0x202020, Slot.HEAD, 80000);
        Defs.armour(items, "bucket_helm_g", "Bucket helm (g)", "full_helm", 0xE8C030, "leather", 80000, Slot.HEAD,
                st(0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0), Map.of());
        unique(S, "bucket_helm_g");
        unique(S, "ring_of_coins");
        Defs.worn(items, "obsidian_cape_r", "Obsidian cape (r)", "cape", 0x8A2020, 100000, Jewellery.Slot.CAPE,
                st(0, 0, 0, 0, 0, 9, 9, 9, 9, 9, 0, 0, 0, 0), Map.of(), Jewellery.Perk.NONE, List.of());
        unique(S, "obsidian_cape_r");
        cosmeticArmour(items, S, "samurai_kasa", "Samurai kasa", "cowl", 0xC0A070, Slot.HEAD, 80000);
        cosmeticArmour(items, S, "samurai_shirt", "Samurai shirt", "leather_body", 0x803030, Slot.CHEST, 80000);
        cosmeticArmour(items, S, "samurai_greaves", "Samurai greaves", "chaps", 0x803030, Slot.LEGS, 80000);
        cosmeticArmour(items, S, "samurai_boots", "Samurai boots", "boots", 0x803030, Slot.FEET, 80000);
        cosmeticWorn(items, S, "samurai_gloves", "Samurai gloves", "gloves", 0x803030, Jewellery.Slot.HANDS, 80000);
        for (String[] mask : new String[][]{{"old_demon", "Old demon", "6A6A5A"}, {"lesser_demon", "Lesser demon", "C02020"},
                {"greater_demon", "Greater demon", "801818"}, {"black_demon", "Black demon", "202020"},
                {"jungle_demon", "Jungle demon", "3A6A2A"}}) {
            cosmeticArmour(items, S, mask[0] + "_mask", mask[1] + " mask", "full_helm", Integer.parseInt(mask[2], 16), Slot.HEAD, 80000);
        }
        int dark = 0x2A2A3A;
        Map<Skill, Integer> darkReq = req(Skill.MAGIC, 40, Skill.DEFENCE, 20);
        Defs.armour(items, "hood_of_darkness", "Hood of darkness", "cowl", dark, "leather", 80000, Slot.HEAD,
                st(0, 0, 0, 4, 0, 0, 0, 0, 4, 0, 0, 0, 0, 1), darkReq); // approx stats
        Defs.armour(items, "robe_top_of_darkness", "Robe top of darkness", "leather_body", dark, "leather", 80000, Slot.CHEST,
                st(0, 0, 0, 20, 0, 0, 0, 0, 20, 0, 0, 0, 0, 2), darkReq); // approx stats
        Defs.armour(items, "robe_bottom_of_darkness", "Robe bottom of darkness", "chaps", dark, "leather", 80000, Slot.LEGS,
                st(0, 0, 0, 15, 0, 0, 0, 0, 15, 0, 0, 0, 0, 1), darkReq); // approx stats
        Defs.armour(items, "boots_of_darkness", "Boots of darkness", "boots", dark, "leather", 80000, Slot.FEET,
                st(0, 0, 0, 2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0), darkReq); // approx stats
        Defs.worn(items, "gloves_of_darkness", "Gloves of darkness", "gloves", dark, 80000, Jewellery.Slot.HANDS,
                st(0, 0, 0, 2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0), darkReq, Jewellery.Perk.NONE, List.of());
        for (String id : new String[]{"hood_of_darkness", "robe_top_of_darkness", "robe_bottom_of_darkness", "boots_of_darkness",
                "gloves_of_darkness"}) {
            unique(S, id);
        }
        for (String kit : new String[]{"armadyl_godsword", "bandos_godsword", "saradomin_godsword", "zamorak_godsword", "occult",
                "torture", "anguish", "tormented", "dragon_defender", "dark_infinity_colour", "light_infinity_colour"}) {
            String id = kit + "_ornament_kit";
            Defs.simple(items, id, Defs.cap(kit.replace('_', ' ')) + " ornament kit", "package", 0xE8C030, 500000).stack(1);
            unique(S, id);
        }
        // Treasure trail teleport scrolls are defined with the teleport tablets.
    }

    private static void thirdAge(ItemSink items) {
        int white = 0xE8E8E0;
        Map<Skill, Integer> rngReq = req(Skill.RANGED, 65, Skill.DEFENCE, 45);
        // The Mimic's unique (OSRS Wiki "Ring of 3rd age"): no bonuses, transforms the wearer's look.
        items.add("ring_of_3rd_age", "Ring of 3rd age", Kind.SIMPLE, "ring", 0xE8E8F0).value(12000).stack(1).weight(0.004)
                .examine("A ring of the 3rd age.");
        Defs.armour(items, "3rd_age_range_coif", "3rd age range coif", "cowl", white, "dragon", 5000000, Slot.HEAD,
                st(0, 0, 0, -2, 9, 4, 7, 10, 5, 8, 0, 0, 0, 0), rngReq);
        Defs.armour(items, "3rd_age_range_top", "3rd age range top", "leather_body", white, "dragon", 5000000, Slot.CHEST,
                st(0, 0, 0, -15, 30, 55, 47, 60, 60, 55, 0, 0, 0, 0), rngReq);
        Defs.armour(items, "3rd_age_range_legs", "3rd age range legs", "chaps", white, "dragon", 5000000, Slot.LEGS,
                st(0, 0, 0, -8, 17, 31, 25, 33, 30, 31, 0, 0, 0, 0), rngReq);
        Defs.worn(items, "3rd_age_vambraces", "3rd age vambraces", "gloves", white, 5000000, Jewellery.Slot.HANDS,
                st(0, 0, 0, -11, 11, 6, 6, 6, 8, 6, 0, 0, 0, 0), req(Skill.RANGED, 65), Jewellery.Perk.NONE, List.of());
        Map<Skill, Integer> mageReq = req(Skill.MAGIC, 65, Skill.DEFENCE, 30);
        Defs.armour(items, "3rd_age_mage_hat", "3rd age mage hat", "cowl", 0x3050C0, "dragon", 5000000, Slot.HEAD,
                st(0, 0, 0, 8, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0), mageReq);
        Defs.armour(items, "3rd_age_robe_top", "3rd age robe top", "leather_body", 0x3050C0, "dragon", 5000000, Slot.CHEST,
                st(0, 0, 0, 24, 0, 0, 0, 0, 24, 0, 0, 0, 0, 0), mageReq);
        Defs.armour(items, "3rd_age_robe", "3rd age robe", "chaps", 0x3050C0, "dragon", 5000000, Slot.LEGS,
                st(0, 0, 0, 19, 0, 0, 0, 0, 19, 0, 0, 0, 0, 0), mageReq);
        Defs.worn(items, "3rd_age_amulet", "3rd age amulet", "amulet", 0xE8C030, 5000000, Jewellery.Slot.NECK,
                st(0, 0, 0, 15, 0, 0, 0, 0, 10, 0, 0, 0, 0, 3), Map.of(), Jewellery.Perk.NONE, List.of());
        Map<Skill, Integer> meleeReq = req(Skill.DEFENCE, 65);
        Defs.armour(items, "3rd_age_full_helmet", "3rd age full helmet", "full_helm", white, "dragon", 5000000, Slot.HEAD,
                st(0, 0, 0, -5, -2, 47, 49, 43, -3, 48, 0, 0, 0, 0), meleeReq);
        Defs.armour(items, "3rd_age_platebody", "3rd age platebody", "platebody", white, "dragon", 5000000, Slot.CHEST,
                st(0, 0, 0, -15, 0, 96, 108, 90, -4, 105, 0, 0, 0, 0), meleeReq);
        Defs.armour(items, "3rd_age_platelegs", "3rd age platelegs", "platelegs", white, "dragon", 5000000, Slot.LEGS,
                st(0, 0, 0, -15, 0, 78, 76, 83, -4, 75, 0, 0, 0, 0), meleeReq);
        Defs.armour(items, "3rd_age_kiteshield", "3rd age kiteshield", "kiteshield", white, "dragon", 5000000, Slot.SHIELD,
                st(0, 0, 0, -8, -2, 63, 65, 61, -3, 63, 0, 0, 0, 0), meleeReq);
        for (String id : new String[]{"3rd_age_range_coif", "3rd_age_range_top", "3rd_age_range_legs", "3rd_age_vambraces",
                "3rd_age_mage_hat", "3rd_age_robe_top", "3rd_age_robe", "3rd_age_amulet", "3rd_age_full_helmet",
                "3rd_age_platebody", "3rd_age_platelegs", "3rd_age_kiteshield"}) {
            mega(Tier.HARD, id, 0);
            mega(Tier.ELITE, id, 0);
            mega(Tier.MASTER, id, 0);
        }
        Defs.worn(items, "3rd_age_cloak", "3rd age cloak", "cape", white, 5000000, Jewellery.Slot.CAPE,
                st(0, 0, 0, 0, 0, 3, 3, 3, 3, 3, 0, 0, 0, 3), Map.of(), Jewellery.Perk.NONE, List.of());
        Defs.staff(items, "3rd_age_wand", "3rd age wand", "staff", white, 5000000, st(0, 0, 0, 15, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0),
                req(Skill.MAGIC, 65));
        mega(Tier.ELITE, "3rd_age_cloak", 0);
        mega(Tier.ELITE, "3rd_age_wand", 0);
        mega(Tier.MASTER, "3rd_age_cloak", 0);
        mega(Tier.MASTER, "3rd_age_wand", 0);
        Defs.bow(items, "3rd_age_bow", "3rd age bow", "shortbow", white, 20000000, st(0, 0, 0, 0, 80, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                req(Skill.RANGED, 65), 4);
        Defs.melee(items, "3rd_age_longsword", "3rd age longsword", "longsword", white, "dragon", 20000000,
                st(55, 73, -2, 0, 0, 0, 3, 2, 0, 0, 71, 0, 0, 0), req(Skill.ATTACK, 65), 5, AttackType.SLASH);
        items.add("3rd_age_pickaxe", "3rd age pickaxe", Kind.PICKAXE, "pickaxe", white).value(20000000).stack(1).tier("dragon")
                .equipment(new EquipmentDef(Slot.WEAPON, st(38, -2, 32, 0, 0, 0, 1, 0, 0, 0, 42, 0, 0, 0), req(Skill.ATTACK, 65), 5,
                        AttackType.STAB, EquipmentDef.WeaponClass.MELEE));
        items.add("3rd_age_axe", "3rd age axe", Kind.AXE, "axe", white).value(20000000).stack(1).tier("dragon")
                .equipment(new EquipmentDef(Slot.WEAPON, st(-2, 38, 32, 0, 0, 0, 1, 0, 0, 0, 42, 0, 0, 0), req(Skill.ATTACK, 65), 5,
                        AttackType.SLASH, EquipmentDef.WeaponClass.MELEE));
        Map<Skill, Integer> druid = req(Skill.PRAYER, 65);
        Defs.armour(items, "3rd_age_druidic_robe_top", "3rd age druidic robe top", "leather_body", 0x30A030, "dragon", 10000000, Slot.CHEST,
                st(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6), druid);
        Defs.armour(items, "3rd_age_druidic_robe_bottoms", "3rd age druidic robe bottoms", "chaps", 0x30A030, "dragon", 10000000, Slot.LEGS,
                st(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5), druid);
        Defs.worn(items, "3rd_age_druidic_cloak", "3rd age druidic cloak", "cape", 0x30A030, 10000000, Jewellery.Slot.CAPE,
                st(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4), druid, Jewellery.Perk.NONE, List.of());
        Defs.staff(items, "3rd_age_druidic_staff", "3rd age druidic staff", "staff", 0x30A030, 10000000,
                st(0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 30, 0, 0, 6), druid);
        for (String id : new String[]{"3rd_age_bow", "3rd_age_longsword", "3rd_age_pickaxe", "3rd_age_axe", "3rd_age_druidic_robe_top",
                "3rd_age_druidic_robe_bottoms", "3rd_age_druidic_cloak", "3rd_age_druidic_staff"}) {
            mega(Tier.MASTER, id, 0);
        }
    }

    /**
     * God vestments (prayer armour) for the six gods: robe top and legs are easy rewards; mitre and cloak medium;
     * crozier and stole hard for Saradomin, Zamorak and Guthix but medium for Armadyl, Bandos and Ancient (OSRS Wiki
     * vestment set pages). Bonuses and requirements are the wiki's.
     */
    private static void vestments(ItemSink items) {
        for (String god : new String[]{"saradomin", "zamorak", "guthix", "armadyl", "bandos", "ancient"}) {
            int c = switch (god) {
                case "saradomin" -> 0xE0E0F0;
                case "zamorak" -> 0xA02020;
                case "guthix" -> 0x30A030;
                case "armadyl" -> 0xD8D8C8;
                case "bandos" -> 0x6A5A3A;
                default -> 0x8A3A3A;
            };
            String g = Defs.cap(god);
            String a = god.startsWith("a") ? "An " : "A ";
            boolean classic = god.equals("saradomin") || god.equals("zamorak") || god.equals("guthix");
            Tier top = classic ? Tier.HARD : Tier.MEDIUM;
            Defs.armour(items, god + "_robe_top", g + " robe top", "robe_top", c, "leather", 7000, Slot.CHEST,
                    st(0, 0, 0, 4, 0, 0, 0, 0, 4, 0, 0, 0, 0, 6), req(Skill.PRAYER, 20)).examine(g + " Vestments.");
            Defs.armour(items, god + "_robe_legs", g + " robe legs", "robe_bottom", c, "leather", 7000, Slot.LEGS,
                    st(0, 0, 0, 4, 0, 0, 0, 0, 4, 0, 0, 0, 0, 5), req(Skill.PRAYER, 20)).examine("Leggings from the " + g + " Vestments.");
            Defs.armour(items, god + "_mitre", g + " mitre", "hood", c, "leather", 5000, Slot.HEAD,
                    st(0, 0, 0, 4, 0, 0, 0, 0, 4, 0, 0, 0, 0, 5), req(Skill.PRAYER, 40, Skill.MAGIC, 40)).examine(a + g + " mitre.");
            Defs.worn(items, god + "_cloak", g + " cloak", "cape", c, 2000, Jewellery.Slot.CAPE,
                    st(0, 0, 0, 1, 0, 3, 3, 3, 3, 3, 0, 0, 0, 3), req(Skill.PRAYER, 40), Jewellery.Perk.NONE, List.of())
                    .examine(a + g + " cloak.");
            Defs.worn(items, god + "_stole", g + " stole", "necklace", c, 2500, Jewellery.Slot.NECK,
                    st(0, 0, 0, 2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 10), req(Skill.PRAYER, 60), Jewellery.Perk.NONE, List.of())
                    .examine(a + g + " stole.");
            Defs.melee(items, god + "_crozier", g + " crozier", "staff", c, "rune", 5000,
                    st(7, -1, 25, 10, 0, 2, 3, 1, 10, 0, 32, 0, 0, 6), req(Skill.PRAYER, 60), 5, AttackType.CRUSH)
                    .examine(a + g + " crozier.");
            unique(Tier.EASY, god + "_robe_top");
            unique(Tier.EASY, god + "_robe_legs");
            unique(Tier.MEDIUM, god + "_mitre");
            unique(Tier.MEDIUM, god + "_cloak");
            unique(top, god + "_crozier");
            unique(top, god + "_stole");
        }
    }

    // ---------------- Rolling ----------------

    /** The tier's table (rows whose item exists), filled after the item catalog is built. */
    public static synchronized List<Row> table(Tier t) {
        if (TABLES.isEmpty()) {
            com.runecraft.osrs.ItemCatalog.exists("coins");
            Map<Tier, List<Row>> all = new EnumMap<>(Tier.class);
            for (Tier tier : Tier.values()) {
                all.put(tier, new ArrayList<>());
            }
            for (String line : ClueRewardData.ROWS.split("\n")) {
                String[] f = java.util.Arrays.copyOf(line.split("\t", -1), 8);
                if (f[0] == null || f[0].isBlank()) {
                    continue;
                }
                Tier tier = Tier.valueOf(f[0].toUpperCase(java.util.Locale.ROOT));
                if (!com.runecraft.osrs.ItemCatalog.exists(f[2])) {
                    continue;
                }
                double chance = Double.parseDouble(f[6]) / Double.parseDouble(f[7]);
                all.get(tier).add(new Row(f[1], f[2], Integer.parseInt(f[3]), Integer.parseInt(f[4]), "noted".equals(f[5]), chance));
            }
            all.forEach((tier, rows) -> TABLES.put(tier, List.copyOf(rows)));
        }
        return TABLES.get(t);
    }

    /** Chance per reward roll of an item on a tier's table (0 = not on it), summed over its rows, after scaling. */
    public static double perRoll(Tier t, String item) {
        double total = 0;
        double sum = 0;
        for (Row r : table(t)) {
            total += r.chance();
            if (r.item().equals(item)) {
                sum += r.chance();
            }
        }
        return total <= 0 ? 0 : sum / total;
    }

    /** One in how many reward rolls gives this item (0 = never), for display and tests. */
    public static int megaOneIn(Tier t, String item) {
        double p = perRoll(t, item);
        return p <= 0 ? 0 : (int) Math.round(1 / p);
    }

    /** Chance (1 in n) per casket of a master clue scroll (0 = none). */
    public static int masterClueOneIn(Tier t) {
        return switch (t) {
            case EASY -> 50;
            case MEDIUM -> 30;
            case HARD -> 15;
            case ELITE -> 5;
            default -> 0;
        };
    }

    /** Chance (1 in n) that an opened casket is a Mimic (when enabled): elite 1/35, master 1/15. */
    public static int mimicOneIn(Tier t) {
        return t == Tier.ELITE ? 35 : t == Tier.MASTER ? 15 : 0;
    }

    /** The scroll box item of a tier. */
    public static String scrollBox(Tier t) {
        return "scroll_box_" + t.id();
    }

    /** Completed-clue counts at which a tier's minor and major scroll cases raise its stack limit (OSRS Wiki). */
    public static int[] scrollCaseMilestones(Tier t) {
        return switch (t) {
            case BEGINNER -> new int[]{50, 100};
            case EASY -> new int[]{100, 200};
            case MEDIUM -> new int[]{100, 250};
            case HARD, ELITE -> new int[]{50, 150};
            case MASTER -> new int[]{25, 75};
        };
    }

    /** How many clues of a tier (scroll plus scroll boxes) a player can hold: 2, +1 per scroll case milestone. */
    public static int stackLimit(Tier t, int completed) {
        int[] m = scrollCaseMilestones(t);
        return 2 + (completed >= m[0] ? 1 : 0) + (completed >= m[1] ? 1 : 0);
    }

    /**
     * The stack limit with the Mimic scroll case (OSRS Wiki "Scroll box": rewarded on defeating the Mimic, +1 for every tier),
     * so at most 5.
     */
    public static int stackLimit(Tier t, int completed, boolean mimicCase) {
        return stackLimit(t, completed) + (mimicCase ? 1 : 0);
    }

    /** How many item rolls a casket of the tier holds (beginner 1-3 ... master 5-7). */
    public static int rollCount(Tier tier, Random random) {
        return tier.minRolls() + random.nextInt(tier.maxRolls() - tier.minRolls() + 1);
    }

    public static List<Reward> roll(Tier tier, Random random) {
        return roll(tier, rollCount(tier, random), random);
    }

    /** A casket's rewards with a given number of item rolls (a Mimic killed after failed attempts has fewer), plus tertiaries. */
    public static List<Reward> roll(Tier tier, int rolls, Random random) {
        Map<String, Integer> out = new LinkedHashMap<>();
        Map<String, Boolean> noted = new LinkedHashMap<>();
        for (int i = 0; i < rolls; i++) {
            Row r = pick(table(tier), random);
            if (r != null) {
                out.merge(r.item(), r.min() + random.nextInt(r.max() - r.min() + 1), Integer::sum);
                noted.merge(r.item(), r.noted(), Boolean::logicalOr);
            }
        }
        int master = masterClueOneIn(tier);
        if (master > 0 && random.nextInt(master) == 0) {
            out.merge("clue_scroll_master", 1, Integer::sum);
        }
        if (tier == Tier.MASTER && random.nextInt(BLOODHOUND_ONE_IN) == 0) {
            out.merge("bloodhound", 1, Integer::sum);
        }
        List<Reward> list = new ArrayList<>();
        out.forEach((k, v) -> list.add(new Reward(k, v, noted.getOrDefault(k, false))));
        return list;
    }

    /** One roll on a tier's reward table, with no tertiary rolls: what a lucky impling jar holds. */
    public static Reward singleRoll(Tier tier, Random random) {
        Row r = pick(table(tier), random);
        return r == null ? new Reward("coins", 1) : new Reward(r.item(), r.min() + random.nextInt(r.max() - r.min() + 1), r.noted());
    }

    private static Row pick(List<Row> table, Random random) {
        double total = 0;
        for (Row r : table) {
            total += r.chance();
        }
        double x = random.nextDouble() * total;
        for (Row r : table) {
            x -= r.chance();
            if (x < 0) {
                return r;
            }
        }
        return table.isEmpty() ? null : table.get(table.size() - 1);
    }

    private static List<String> section(Tier t, String section) {
        List<String> out = new ArrayList<>();
        for (Row r : table(t)) {
            if (r.section().equals(section) && !out.contains(r.item())) {
                out.add(r.item());
            }
        }
        return out;
    }

    /** The tier's uniques (wiki "clue uniques" table). */
    public static List<String> uniques(Tier t) {
        return section(t, "unique");
    }

    /** The tier's mega-rare table (gilded, 3rd age; hard, elite and master) and the master Bloodhound. */
    public static List<String> megaRares(Tier t) {
        List<String> out = section(t, "mega");
        if (t == Tier.MASTER && com.runecraft.osrs.ItemCatalog.exists("bloodhound")) {
            out.add("bloodhound");
        }
        return out;
    }

    /** The standard and shared rows of a tier, for tests (weights are chances per million rolls). */
    public static List<Loot> standard(Tier t) {
        List<Loot> out = new ArrayList<>();
        for (Row r : table(t)) {
            if (r.section().equals("standard") || r.section().equals("shared")) {
                out.add(new Loot(r.item(), r.min(), r.max(), (int) Math.max(1, Math.round(r.chance() * 1_000_000))));
            }
        }
        return out;
    }

    /** Items the definitions declare as the tier's uniques (for the tests' diff against the wiki tables). */
    public static List<String> declared(Tier t) {
        com.runecraft.osrs.ItemCatalog.exists("coins");
        return DECLARED.get(t);
    }

    /** All reward item ids of a tier, for the collection log: its uniques and mega-rares. */
    public static List<String> logItems(Tier t) {
        List<String> all = new ArrayList<>(uniques(t));
        for (String m : megaRares(t)) {
            if (!all.contains(m)) {
                all.add(m);
            }
        }
        return all;
    }
}
