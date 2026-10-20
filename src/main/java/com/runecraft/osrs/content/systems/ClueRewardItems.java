package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef.Slot;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ItemSink;

import java.util.List;
import java.util.Map;

import static com.runecraft.osrs.content.systems.Defs.req;
import static com.runecraft.osrs.content.systems.Defs.st;

/**
 * Treasure trail rewards the casket tables needed that RuneCraft lacked (OSRS Wiki reward casket pages): heraldic
 * armour, trimmed plateskirts, elegant clothes, boaters, headbands, Kourend banners/scarves/hoods, dragon masks,
 * gilded pieces, tuxedos, ornament kits, team capes and the shared rewards (purple sweets, firelighters, blessings,
 * the master scroll book and the dragonstone charge scroll). Equipment stats are the base item's (the reference table
 * in osrs/data/EquipmentBonuses corrects any listed there); cosmetics have none.
 */
final class ClueRewardItems {
    private ClueRewardItems() {
    }

    private static final EquipmentStats ZERO = EquipmentStats.NONE;

    private static void cosmetic(ItemSink items, String id, String name, String shape, int color, Slot slot, int value) {
        Defs.armour(items, id, name, shape, color, "leather", value, slot, ZERO, Map.of()).examine("A treasure trail reward.");
    }

    private static void cosmeticWorn(ItemSink items, String id, String name, String shape, int color, Jewellery.Slot slot, int value) {
        Defs.worn(items, id, name, shape, color, value, slot, ZERO, Map.of(), Jewellery.Perk.NONE, List.of())
                .examine("A treasure trail reward.");
    }

    static void items(ItemSink items) {
        // Heraldic armour (h1-h5): black (easy), adamant (medium) and rune (hard) shields, helms and platebodies.
        int[] heraldry = {0xC03030, 0x3050C0, 0x30A030, 0xE0C030, 0x7A3AA0};
        for (String metal : new String[]{"black", "adamant", "rune"}) {
            int value = metal.equals("rune") ? 60000 : metal.equals("adamant") ? 10000 : 2500;
            for (int h = 1; h <= 5; h++) {
                String m = Defs.cap(metal);
                Defs.armour(items, metal + "_shield_h" + h, m + " shield (h" + h + ")", "kiteshield", heraldry[h - 1], metal, value,
                        Slot.SHIELD, Defs.plate(metal, Defs.Piece.SHIELD), req(Skill.DEFENCE, Defs.plateLevel(metal)))
                        .examine("A shield with a heraldic design.");
                Defs.armour(items, metal + "_helm_h" + h, m + " helm (h" + h + ")", "full_helm", heraldry[h - 1], metal, value,
                        Slot.HEAD, Defs.plate(metal, Defs.Piece.HELM), req(Skill.DEFENCE, Defs.plateLevel(metal)))
                        .examine("A helmet with a heraldic design.");
                Defs.armour(items, metal + "_platebody_h" + h, m + " platebody (h" + h + ")", "platebody", heraldry[h - 1], metal,
                        value, Slot.CHEST, Defs.plate(metal, Defs.Piece.BODY), req(Skill.DEFENCE, Defs.plateLevel(metal)))
                        .examine("A platebody with a heraldic design.");
            }
        }
        // Trimmed and gold-trimmed plateskirts, god plateskirts, gilded and 3rd age plateskirts.
        for (String metal : new String[]{"bronze", "iron", "steel", "black", "mithril", "adamant", "rune"}) {
            for (String trim : new String[]{"t", "g"}) {
                Defs.armour(items, metal + "_plateskirt_" + trim, Defs.cap(metal) + " plateskirt (" + trim + ")", "platelegs",
                        trim.equals("g") ? 0xD8B030 : 0x6A6A70, metal, metal.equals("rune") ? 50000 : 3000, Slot.LEGS,
                        Defs.plate(metal, Defs.Piece.LEGS), req(Skill.DEFENCE, Defs.plateLevel(metal)))
                        .examine(trim.equals("t") ? "Trimmed armour from a treasure trail." : "Gold-trimmed armour from a treasure trail.");
            }
        }
        for (String god : new String[]{"saradomin", "zamorak", "guthix", "ancient", "armadyl", "bandos"}) {
            Defs.armour(items, god + "_plateskirt", Defs.cap(god) + " plateskirt", "platelegs", 0xC0C0C8, "rune", 60000, Slot.LEGS,
                    Defs.plate("rune", Defs.Piece.LEGS), req(Skill.DEFENCE, 40)).examine("Rune armour blessed by " + Defs.cap(god) + ".");
        }
        Defs.armour(items, "gilded_plateskirt", "Gilded plateskirt", "platelegs", 0xE8C030, "rune", 300000, Slot.LEGS,
                Defs.plate("rune", Defs.Piece.LEGS), req(Skill.DEFENCE, 40)).examine("Gilded rune armour from a treasure trail.");
        Defs.armour(items, "3rd_age_plateskirt", "3rd age plateskirt", "platelegs", 0xE8E8E0, "dragon", 5000000, Slot.LEGS,
                st(0, 0, 0, -15, 0, 78, 76, 83, -4, 75, 0, 0, 0, 0), req(Skill.DEFENCE, 65)).examine("Ancient armour beaten from magical silver.");
        // The rest of the gilded set (elite/master mega-rares; the platebody, legs, helm, shield, scimitar and boots exist).
        Defs.armour(items, "gilded_med_helm", "Gilded med helm", "full_helm", 0xE8C030, "rune", 200000, Slot.HEAD,
                st(0, 0, 0, -3, -1, 22, 23, 21, -1, 22, 0, 0, 0, 0), req(Skill.DEFENCE, 40));
        Defs.armour(items, "gilded_chainbody", "Gilded chainbody", "platebody", 0xE8C030, "rune", 300000, Slot.CHEST,
                st(0, 0, 0, -15, 0, 63, 72, 78, -3, 65, 0, 0, 0, 0), req(Skill.DEFENCE, 40));
        Defs.armour(items, "gilded_sq_shield", "Gilded sq shield", "kiteshield", 0xE8C030, "rune", 250000, Slot.SHIELD,
                st(0, 0, 0, -6, -2, 38, 40, 36, 0, 38, 0, 0, 0, 0), req(Skill.DEFENCE, 40));
        Defs.melee(items, "gilded_2h_sword", "Gilded 2h sword", "2h_sword", 0xE8C030, "rune", 300000,
                st(-4, 69, 50, -4, 0, 0, 0, 0, 0, -1, 70, 0, 0, 0), req(Skill.ATTACK, 40), 7, AttackType.SLASH);
        Defs.melee(items, "gilded_spear", "Gilded spear", "spear", 0xE8C030, "rune", 200000,
                st(36, 36, 36, 0, 0, 1, 1, 0, 0, 0, 42, 0, 0, 0), req(Skill.ATTACK, 40), 5, AttackType.STAB);
        Defs.melee(items, "gilded_hasta", "Gilded hasta", "hasta", 0xE8C030, "rune", 200000,
                st(35, 31, 31, 0, 0, 0, 0, 0, 0, 0, 40, 0, 0, 0), req(Skill.ATTACK, 40), 4, AttackType.STAB);
        Defs.armour(items, "gilded_coif", "Gilded coif", "coif", 0xE8C030, "leather", 100000, Slot.HEAD,
                st(0, 0, 0, -1, 7, 4, 7, 10, 4, 8, 0, 0, 0, 0), req(Skill.RANGED, 40));
        Defs.worn(items, "gilded_dhide_vambraces", "Gilded d'hide vambraces", "vambraces", 0xE8C030, 100000, Jewellery.Slot.HANDS,
                st(0, 0, 0, -10, 11, 6, 5, 7, 8, 5, 0, 0, 0, 0), req(Skill.RANGED, 40), Jewellery.Perk.NONE, List.of());
        Defs.armour(items, "gilded_dhide_body", "Gilded d'hide body", "leather_body", 0xE8C030, "leather", 200000, Slot.CHEST,
                st(0, 0, 0, -15, 30, 40, 32, 45, 40, 40, 0, 0, 0, 0), req(Skill.RANGED, 70, Skill.DEFENCE, 40));
        Defs.armour(items, "gilded_dhide_chaps", "Gilded d'hide chaps", "chaps", 0xE8C030, "leather", 150000, Slot.LEGS,
                st(0, 0, 0, -10, 17, 31, 25, 33, 28, 31, 0, 0, 0, 0), req(Skill.RANGED, 70));
        items.add("gilded_pickaxe", "Gilded pickaxe", Kind.PICKAXE, "pickaxe", 0xE8C030).value(100000).stack(1).tier("rune")
                .equipment(new com.runecraft.osrs.EquipmentDef(Slot.WEAPON, st(26, -2, 24, 0, 0, 0, 1, 0, 0, 0, 29, 0, 0, 0),
                        req(Skill.ATTACK, 40), 5, AttackType.STAB, com.runecraft.osrs.EquipmentDef.WeaponClass.MELEE));
        items.add("gilded_axe", "Gilded axe", Kind.AXE, "axe", 0xE8C030).value(100000).stack(1).tier("rune")
                .equipment(new com.runecraft.osrs.EquipmentDef(Slot.WEAPON, st(-2, 26, 24, 0, 0, 0, 1, 0, 0, 0, 29, 0, 0, 0),
                        req(Skill.ATTACK, 40), 5, AttackType.SLASH, com.runecraft.osrs.EquipmentDef.WeaponClass.MELEE));
        Defs.simple(items, "gilded_spade", "Gilded spade", "tool_generic", 0xE8C030, 50000).stack(1).examine("A gilded spade.");
        // Elegant clothes (easy red/green/blue; medium purple/pink/gold).
        String[][] elegant = {{"red", "C03030"}, {"green", "30A030"}, {"blue", "3050C0"}, {"purple", "7A3AA0"}, {"pink", "E080B0"},
                {"gold", "D8B030"}};
        for (String[] c : elegant) {
            int col = Integer.parseInt(c[1], 16);
            String n = Defs.cap(c[0]);
            if (!c[0].equals("red") && !c[0].equals("purple")) {
                cosmetic(items, c[0] + "_elegant_shirt", n + " elegant shirt", "leather_body", col, Slot.CHEST, 3000);
                cosmetic(items, c[0] + "_elegant_legs", n + " elegant legs", "chaps", col, Slot.LEGS, 3000);
            }
            cosmetic(items, c[0] + "_elegant_blouse", n + " elegant blouse", "leather_body", col, Slot.CHEST, 3000);
            cosmetic(items, c[0] + "_elegant_skirt", n + " elegant skirt", "chaps", col, Slot.LEGS, 3000);
        }
        // Boaters, headbands, the Kourend house banners, scarves and hoods.
        String[][] boaters = {{"red", "C03030"}, {"green", "30A030"}, {"orange", "E08030"}, {"black", "202020"}, {"blue", "3050C0"},
                {"pink", "E080B0"}, {"purple", "7A3AA0"}, {"white", "E0E0E0"}};
        for (String[] b : boaters) {
            cosmetic(items, b[0] + "_boater", Defs.cap(b[0]) + " boater", "cowl", Integer.parseInt(b[1], 16), Slot.HEAD, 3000);
        }
        for (String[] h : new String[][]{{"white", "E0E0E0"}, {"blue", "3050C0"}, {"gold", "D8B030"}, {"pink", "E080B0"},
                {"green", "30A030"}}) {
            Defs.armour(items, h[0] + "_headband", Defs.cap(h[0]) + " headband", "cowl", Integer.parseInt(h[1], 16), "leather", 5000,
                    Slot.HEAD, st(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0), Map.of());
        }
        String[][] houses = {{"arceuus", "6A3A8A"}, {"hosidius", "3A8A3A"}, {"lovakengj", "8A6A3A"}, {"piscarilius", "3A6A9A"},
                {"shayzien", "A03030"}};
        for (String[] h : houses) {
            int col = Integer.parseInt(h[1], 16);
            String n = Defs.cap(h[0]);
            Defs.melee(items, h[0] + "_banner", n + " banner", "staff", col, "iron", 5000, ZERO, Map.of(), 4, AttackType.CRUSH)
                    .examine("A banner of the House of " + n + ".");
            cosmeticWorn(items, h[0] + "_scarf", n + " scarf", "necklace", col, Jewellery.Slot.NECK, 5000);
            cosmetic(items, h[0] + "_hood", n + " hood", "hood", col, Slot.HEAD, 5000);
        }
        // Team capes and the cape of skulls (easy).
        for (String[] c : new String[][]{{"team_cape_zero", "Team cape zero", "F0F0F0"}, {"team_cape_i", "Team cape i", "C03030"},
                {"team_cape_x", "Team cape x", "202020"}, {"cape_of_skulls", "Cape of skulls", "3A3A3A"}}) {
            cosmeticWorn(items, c[0], c[1], "cape", Integer.parseInt(c[2], 16), Jewellery.Slot.CAPE, 3000);
        }
        // Other cosmetics.
        Defs.armour(items, "wooden_shield_g", "Wooden shield (g)", "kiteshield", 0xD8B030, "wood", 2000, Slot.SHIELD,
                st(0, 0, 0, 0, 0, 4, 5, 3, 1, 4, 0, 0, 0, 0), Map.of());
        Defs.armour(items, "monks_robe_top_g", "Monk's robe top (g)", "leather_body", 0xD8B030, "leather", 5000, Slot.CHEST,
                st(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6), Map.of());
        Defs.armour(items, "monks_robe_g", "Monk's robe (g)", "chaps", 0xD8B030, "leather", 5000, Slot.LEGS,
                st(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5), Map.of());
        for (String colour : new String[]{"blue", "black"}) {
            for (String trim : new String[]{"t", "g"}) {
                Defs.armour(items, colour + "_skirt_" + trim, Defs.cap(colour) + " skirt (" + trim + ")", "chaps",
                        trim.equals("g") ? 0xD8B030 : colour.equals("blue") ? 0x3050C0 : 0x202020, "leather", 2000, Slot.LEGS,
                        st(0, 0, 0, 2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0), Map.of());
            }
        }
        Defs.armour(items, "blue_wizard_robe", "Blue wizard robe", "leather_body", 0x3050C0, "leather", 15, Slot.CHEST,
                st(0, 0, 0, 3, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0), Map.of()).examine("I can do magic better in this.");
        cosmetic(items, "beanie", "Beanie", "cowl", 0x6A4A2A, Slot.HEAD, 2000);
        cosmetic(items, "imp_mask", "Imp mask", "cowl", 0xC03030, Slot.HEAD, 2000);
        cosmetic(items, "goblin_mask", "Goblin mask", "cowl", 0x5A8A3A, Slot.HEAD, 2000);
        cosmetic(items, "black_leprechaun_hat", "Black leprechaun hat", "cowl", 0x202020, Slot.HEAD, 5000);
        cosmetic(items, "zombie_head_tt", "Zombie head", "full_helm", 0x6A8A5A, Slot.HEAD, 20000);
        cosmetic(items, "cyclops_head", "Cyclops head", "full_helm", 0xA08060, Slot.HEAD, 20000);
        for (String[] c : new String[][]{{"red", "C03030"}, {"white", "E0E0E0"}, {"navy", "203060"}}) {
            cosmetic(items, c[0] + "_cavalier", Defs.cap(c[0]) + " cavalier", "cowl", Integer.parseInt(c[1], 16), Slot.HEAD, 20000);
        }
        for (String[] m : new String[][]{{"green", "30A030"}, {"blue", "3050C0"}, {"red", "C03030"}, {"black", "202020"},
                {"bronze", "8A5A2A"}, {"iron", "6A6A6A"}, {"steel", "9A9AA0"}, {"mithril", "4A4A7A"}, {"adamant", "3A6A4A"},
                {"rune", "3A6A8A"}}) {
            Defs.armour(items, m[0] + "_dragon_mask", Defs.cap(m[0]) + " dragon mask", "full_helm", Integer.parseInt(m[1], 16), "leather",
                    30000, Slot.HEAD, st(0, 0, 0, -1, -1, 1, 1, 1, 0, 0, 0, 0, 0, 0), Map.of());
        }
        Defs.melee(items, "staff_of_bob_the_cat", "Staff of bob the cat", "staff", 0x8A6A3A, "iron", 2000, ZERO, Map.of(), 4,
                AttackType.CRUSH);
        Defs.bow(items, "rain_bow", "Rain bow", "shortbow", 0xE080B0, 2000, ZERO, Map.of(), 5);
        Defs.bow(items, "yew_comp_bow", "Yew comp bow", "shortbow", 0x8A6A3A, 20000, st(0, 0, 0, 0, 49, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                req(Skill.RANGED, 40), 5);
        Defs.melee(items, "nunchaku", "Nunchaku", "flail", 0x6A4A2A, "steel", 30000, st(0, 0, 9, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0),
                Map.of(), 4, AttackType.CRUSH);
        Defs.melee(items, "dual_sai", "Dual sai", "dagger", 0xA0A0A8, "steel", 30000, st(8, 6, 0, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0),
                Map.of(), 4, AttackType.STAB);
        Defs.simple(items, "thieving_bag", "Thieving bag", "sack", 0x5A3A2A, 20000).stack(1).examine("A bag to keep stolen goods in.");
        cosmetic(items, "musketeer_tabard", "Musketeer tabard", "leather_body", 0x5A3A6A, Slot.CHEST, 30000);
        cosmetic(items, "musketeer_pants", "Musketeer pants", "chaps", 0x5A3A6A, Slot.LEGS, 30000);
        cosmetic(items, "bucket_helm", "Bucket helm", "full_helm", 0x8A8A90, Slot.HEAD, 30000);
        cosmetic(items, "blacksmiths_helm", "Blacksmith's helm", "full_helm", 0x6A6A70, Slot.HEAD, 30000);
        cosmetic(items, "bowl_wig", "Bowl wig", "cowl", 0x8A6A3A, Slot.HEAD, 80000);
        for (String shade : new String[]{"dark", "light"}) {
            int col = shade.equals("dark") ? 0x202020 : 0xE0E0E0;
            String s = Defs.cap(shade);
            cosmeticWorn(items, shade + "_bow_tie", s + " bow tie", "necklace", col, Jewellery.Slot.NECK, 30000);
            cosmetic(items, shade + "_tuxedo_jacket", s + " tuxedo jacket", "leather_body", col, Slot.CHEST, 30000);
            cosmeticWorn(items, shade + "_tuxedo_cuffs", s + " tuxedo cuffs", "gloves", col, Jewellery.Slot.HANDS, 30000);
            cosmetic(items, shade + "_trousers", s + " trousers", "chaps", col, Slot.LEGS, 30000);
            cosmetic(items, shade + "_tuxedo_shoes", s + " tuxedo shoes", "boots", col, Slot.FEET, 30000);
        }
        // Ornament kits.
        for (String[] kit : new String[][]{{"dragon_boots", "Dragon boots"}, {"rune_defender", "Rune defender"},
                {"berserker_necklace", "Berserker necklace"}, {"tzhaar_ket_om", "Tzhaar-ket-om"}, {"fury", "Fury"},
                {"dragon_kiteshield", "Dragon kiteshield"}, {"dragon_platebody", "Dragon platebody"}}) {
            Defs.simple(items, kit[0] + "_ornament_kit", kit[1] + " ornament kit", "package", 0xE8C030, 100000).stack(1)
                    .examine("Use on " + kit[1].toLowerCase(java.util.Locale.ROOT) + " to change its appearance.");
        }
        // Shared rewards.
        items.add("purple_sweets", "Purple sweets", Kind.FOOD, "cake", 0x8A3AA0).value(10).heal(2) // approx: OSRS heals 1-3
                .examine("Sweet and fluffy.");
        for (String[] f : new String[][]{{"red", "C03030"}, {"green", "30A030"}, {"blue", "3050C0"}, {"purple", "7A3AA0"},
                {"white", "E0E0E0"}, {"gnomish", "C0A040"}}) {
            Defs.simple(items, f[0] + "_firelighter", Defs.cap(f[0]) + " firelighter", "tinderbox", Integer.parseInt(f[1], 16), 50)
                    .examine("Makes firelighting a lot easier.");
        }
        for (String[] b : new String[][]{{"holy", "E0E0F0"}, {"unholy", "A02020"}, {"peaceful", "30A030"}, {"war", "6A5A3A"},
                {"honourable", "E0E0D0"}, {"ancient", "8A3A3A"}}) {
            Defs.worn(items, b[0] + "_blessing", Defs.cap(b[0]) + " blessing", "insignia", Integer.parseInt(b[1], 16), 1000,
                    Jewellery.Slot.AMMO, st(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1), Map.of(), Jewellery.Perk.NONE, List.of())
                    .examine("A stole imbued with the power of a god.");
        }
        Defs.simple(items, "master_scroll_book_empty", "Master scroll book (empty)", "book", 0x8A5A3A, 2500).stack(1)
                .examine("A book for storing treasure trail teleport scrolls.");
        Defs.simple(items, "charge_dragonstone_jewellery_scroll", "Charge dragonstone jewellery scroll", "scroll", 0xC060C0, 500)
                .examine("Recharges dragonstone jewellery.");
    }
}
