package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentDef.Slot;
import com.runecraft.osrs.EquipmentDef.WeaponClass;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ItemSink;

import java.util.EnumMap;
import java.util.Map;

/**
 * Slayer equipment, rewards and the unique drops of slayer creatures. Bonuses follow the OSRS
 * equipment screens. There is no hands, neck or ring slot, so gloves, the witchwood icon and the
 * occult necklace work from the inventory (or are plain items).
 */
public final class SlayerItems {
    private SlayerItems() {
    }

    private static EquipmentStats s(int sa, int sl, int cr, int ma, int ra, int sd, int sld, int cd, int md, int rd,
                                    int str, int rstr, int mdmg, int prayer) {
        return new EquipmentStats(sa, sl, cr, ma, ra, sd, sld, cd, md, rd, str, rstr, mdmg, prayer);
    }

    private static Map<Skill, Integer> req(Object... pairs) {
        Map<Skill, Integer> map = new EnumMap<>(Skill.class);
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((Skill) pairs[i], (Integer) pairs[i + 1]);
        }
        return map;
    }

    private static EquipmentDef wear(Slot slot, EquipmentStats stats, Object... reqs) {
        return new EquipmentDef(slot, stats, req(reqs), 0, AttackType.CRUSH, WeaponClass.NONE);
    }

    private static EquipmentDef weapon(EquipmentStats stats, int speed, AttackType type, WeaponClass cls, Object... reqs) {
        return new EquipmentDef(Slot.WEAPON, stats, req(reqs), speed, type, cls);
    }

    private static final EquipmentStats NONE = s(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    private static final EquipmentStats SLAYER_HELM = s(0, 0, 0, -6, -2, 30, 32, 27, -1, 30, 0, 0, 0, 0);
    private static final EquipmentStats SLAYER_HELM_I = s(0, 0, 0, -6, -2, 30, 32, 27, -1, 30, 0, 0, 0, 0);

    // Shared slayer-boss uniques (hydra parts, trident, occult, abyssal dagger/head, granite maul,
    // dragon chainbody, brimstone ring, ferocious gloves, smouldering stone) are defined by the bosses module.
    public static void add(ItemSink items) {
        // --- Protective equipment (slayer equipment shop) ---
        items.add("earmuffs", "Earmuffs", Kind.ARMOUR, "full_helm", 0xC8A050).value(200).stack(1).tier("leather")
                .equipment(wear(Slot.HEAD, NONE, Skill.SLAYER, 15)).examine("These will protect me from banshees' screams.");
        items.add("nose_peg", "Nose peg", Kind.ARMOUR, "full_helm", 0xD8C8B0).value(200).stack(1).tier("leather")
                .equipment(wear(Slot.HEAD, NONE)).examine("Protects you from aberrant spectres' stench.");
        items.add("facemask", "Facemask", Kind.ARMOUR, "full_helm", 0xA8A8A0).value(200).stack(1).tier("leather")
                .equipment(wear(Slot.HEAD, NONE)).examine("Stops me from breathing nasty stuff!");
        items.add("spiny_helmet", "Spiny helmet", Kind.ARMOUR, "full_helm", 0x8A7A5A).value(650).stack(1).tier("leather")
                .equipment(wear(Slot.HEAD, s(0, 0, 0, -3, -1, 6, 7, 5, 0, 6, 0, 0, 0, 0), Skill.SLAYER, 5, Skill.DEFENCE, 5))
                .examine("You don't want to wear it inside-out.");
        items.add("mirror_shield", "Mirror shield", Kind.SHIELD, "kiteshield", 0xD0D8E0).value(5000).stack(1)
                .equipment(wear(Slot.SHIELD, s(0, 0, 0, -6, 0, 10, 15, 12, 6, 10, 0, 0, 0, 0), Skill.DEFENCE, 20, Skill.SLAYER, 25))
                .examine("A very shiny shield. Basilisks and cockatrice can't stand their own reflection.");
        items.add("insulated_boots", "Insulated boots", Kind.ARMOUR, "boots", 0x4A3A2A).value(200).stack(1).tier("leather")
                .equipment(wear(Slot.FEET, s(0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0), Skill.SLAYER, 37))
                .examine("Protects me from killerwatts' electric shocks.");
        items.add("boots_of_stone", "Boots of stone", Kind.ARMOUR, "boots", 0x7A7A70).value(2000).stack(1).tier("leather")
                .equipment(wear(Slot.FEET, s(0, 0, 0, 0, 0, 2, 2, 2, 0, 2, 0, 0, 0, 0), Skill.SLAYER, 44))
                .examine("Protects my feet from the heat of the Karuulm Slayer Dungeon.");
        items.add("boots_of_brimstone", "Boots of brimstone", Kind.ARMOUR, "boots", 0x9A4A2A).value(80000).stack(1).tier("rune")
                .equipment(wear(Slot.FEET, s(0, 0, 0, 3, 3, 12, 12, 12, 4, 12, 1, 1, 0, 0), Skill.DEFENCE, 70, Skill.MAGIC, 70,
                        Skill.RANGED, 70, Skill.SLAYER, 44))
                .examine("Smouldering boots made with a drake's claw.");
        items.add("slayer_gloves", "Slayer gloves", Kind.SIMPLE, "leather", 0x6A5A3A).value(200).stack(1)
                .examine("Protects me from fever spiders (works from the inventory).");
        items.add("witchwood_icon", "Witchwood icon", Kind.SIMPLE, "amulet", 0x6A4A2A).value(900).stack(1)
                .examine("Protects me from cave horrors' howls (works from the inventory).");
        items.add("rock_hammer", "Rock hammer", Kind.TOOL, "hammer", 0x8A8A8A).value(500).stack(1)
                .examine("Used for smashing gargoyles.");
        items.add("bag_of_salt", "Bag of salt", Kind.SIMPLE, "package", 0xE8E8E0).value(10).stack(64)
                .examine("Rockslugs hate salt.");
        items.add("ice_cooler", "Ice cooler", Kind.SIMPLE, "pot_item", 0xB0D8F0).value(1).stack(64)
                .examine("Desert lizards can't stand the cold.");
        items.add("fungicide_spray", "Fungicide spray", Kind.TOOL, "spray", 0x6AA05A).value(300).stack(1)
                .examine("Kills mutated zygomites.");
        items.add("unlit_bug_lantern", "Unlit bug lantern", Kind.SIMPLE, "lantern", 0x6A6A6A).value(130).stack(1)
                .examine("Light it with a tinderbox (33 Firemaking).");
        items.add("bug_lantern", "Lit bug lantern", Kind.SIMPLE, "lantern_lit", 0xF0C040).value(130).stack(1)
                .examine("Harpie bug swarms are drawn to the light.");
        items.add("slayer_bell", "Slayer bell", Kind.SIMPLE, "bell", 0xC8A030).value(150).stack(1)
                .examine("Used to lure out wall beasts... or so they say.");
        items.add("slayer_ring_8", "Slayer ring (8)", Kind.SLAYER_GEM, "ring", 0x8A30C0).value(87).stack(1)
                .examine("A ring that tells me my slayer task.");
        items.add("herb_sack", "Herb sack", Kind.SIMPLE, "package", 0x5A7A3A).value(750).stack(1)
                .examine("A sack for storing grimy herbs.");

        // --- Leaf-bladed weapons & broad ammunition ---
        items.add("leaf_bladed_spear", "Leaf-bladed spear", Kind.MELEE_WEAPON, "spear", 0x8AA060).value(31000).stack(1)
                .tier("rune").equipment(weapon(s(47, 42, 36, 0, 0, 1, 5, 2, 0, 0, 50, 0, 0, 0), 5, AttackType.STAB,
                        WeaponClass.MELEE, Skill.ATTACK, 50, Skill.SLAYER, 55))
                .examine("A razor sharp spear designed to hurt kurask and turoth.");
        items.add("leaf_bladed_sword", "Leaf-bladed sword", Kind.MELEE_WEAPON, "longsword", 0x8AA060).value(68000).stack(1)
                .tier("rune").equipment(weapon(s(67, 68, -2, 0, 0, 0, 0, 0, 0, 0, 70, 0, 0, 0), 4, AttackType.SLASH,
                        WeaponClass.MELEE, Skill.ATTACK, 50, Skill.SLAYER, 55))
                .examine("A razor sharp sword designed to hurt kurask and turoth.");
        items.add("leaf_bladed_battleaxe", "Leaf-bladed battleaxe", Kind.MELEE_WEAPON, "battleaxe", 0x8AA060).value(150000)
                .stack(1).tier("dragon").equipment(weapon(s(-2, 72, 69, 0, 0, 0, 0, 0, -1, -1, 92, 0, 0, 0), 5, AttackType.SLASH,
                        WeaponClass.MELEE, Skill.ATTACK, 65, Skill.SLAYER, 55))
                .examine("A razor sharp battleaxe designed to hurt kurask and turoth.");
        items.add("broad_arrowheads", "Broad arrowheads", Kind.SIMPLE, "arrowtips", 0x9A9A8A).value(20).stack(1000)
                .examine("Fletch them onto headless arrows (52 Fletching).");
        items.add("broad_arrows", "Broad arrows", Kind.ARROW, "arrow", 0x9A9A8A).value(60).stack(1000).rangedStrength(28)
                .tier("broad").examine("Arrows with broad heads that can harm kurask and turoth.");
        items.add("slayer_staff", "Slayer's staff", Kind.STAFF, "staff", 0x4A5A3A).value(21000).stack(1)
                .equipment(weapon(s(7, -1, 7, 12, 0, 2, 3, 1, 12, 0, 7, 0, 0, 0), 4, AttackType.MAGIC, WeaponClass.STAFF,
                        Skill.MAGIC, 50, Skill.SLAYER, 55))
                .examine("Casts Magic Dart, which can harm kurask and turoth.");

        // --- Headgear rewards ---
        items.add("black_mask", "Black mask", Kind.ARMOUR, "full_helm", 0x202020).value(1500).stack(1).tier("iron")
                .equipment(wear(Slot.HEAD, SLAYER_HELM, Skill.DEFENCE, 10))
                .examine("+16.67% melee accuracy and damage against your slayer task.");
        items.add("black_mask_i", "Black mask (i)", Kind.ARMOUR, "full_helm", 0x302030).value(1500).stack(1).tier("iron")
                .equipment(wear(Slot.HEAD, SLAYER_HELM_I, Skill.DEFENCE, 10))
                .examine("+16.67% melee and +15% ranged and magic accuracy and damage on task.");
        items.add("slayer_helmet", "Slayer helmet", Kind.ARMOUR, "full_helm", 0x3A3A3A).value(1000).stack(1).tier("rune")
                .equipment(wear(Slot.HEAD, SLAYER_HELM, Skill.DEFENCE, 10))
                .examine("Black mask bonus plus every slayer head protection in one helm.");
        items.add("slayer_helmet_i", "Slayer helmet (i)", Kind.ARMOUR, "full_helm", 0x4A3A4A).value(1000).stack(1).tier("rune")
                .equipment(wear(Slot.HEAD, SLAYER_HELM_I, Skill.DEFENCE, 10))
                .examine("An imbued slayer helmet: also boosts ranged and magic on task.");

        // --- Unique drops ---
        items.add("abyssal_ashes", "Abyssal ashes", Kind.SIMPLE, "ashes", 0x4A2A4A).value(100)
                .examine("The ashes of an abyssal demon.");
        items.add("dark_bow", "Dark bow", Kind.BOW, "longbow", 0x2A2A3A).value(120000).stack(1).tier("dragon")
                .equipment(weapon(s(0, 0, 0, 0, 95, 0, 0, 0, 0, 0, 0, 0, 0, 0), 9, AttackType.RANGED, WeaponClass.BOW,
                        Skill.RANGED, 60))
                .examine("A bow from a darker dimension.");
        items.add("granite_legs", "Granite legs", Kind.ARMOUR, "platelegs", 0x7A7A7A).value(41000).stack(1).tier("rune")
                .equipment(wear(Slot.LEGS, s(0, 0, 0, -31, -18, 43, 45, 41, -4, 68, 0, 0, 0, 0), Skill.DEFENCE, 50,
                        Skill.STRENGTH, 50));
        items.add("draconic_visage", "Draconic visage", Kind.SIMPLE, "visage", 0x6A5A4A).value(10000).stack(1)
                .examine("It looks like this could be attached to a shield somehow.");
        items.add("wyvern_visage", "Wyvern visage", Kind.SIMPLE, "visage", 0x8AA0B0).value(10000).stack(1)
                .examine("It looks like this could be attached to a shield somehow.");
        // Fossil Island wyverns' granite pieces (stats approx).
        items.add("granite_boots", "Granite boots", Kind.ARMOUR, "boots", 0x7A7A7A).value(31000).stack(1).tier("rune")
                .equipment(wear(Slot.FEET, s(0, 0, 0, -3, -1, 7, 8, 9, 0, 6, 3, 0, 0, 0), Skill.DEFENCE, 50,
                        Skill.STRENGTH, 50));
        items.add("granite_longsword", "Granite longsword", Kind.MELEE_WEAPON, "longsword", 0x7A7A7A).value(42000).stack(1)
                .tier("rune").equipment(weapon(s(45, 60, -2, 0, 0, 0, 3, 2, 0, 0, 64, 0, 0, 0), 5, AttackType.SLASH,
                        WeaponClass.MELEE, Skill.ATTACK, 50, Skill.STRENGTH, 50))
                .examine("A longsword carved from granite.");
        // Thrown weapons from drakes, wyrms and hydras (OSRS ranged strength: knife 25, thrownaxe 42).
        items.add("dragon_knife", "Dragon knife", Kind.ARROW, "throwing_knife", 0xB02020).value(1200).rangedStrength(25)
                .tier("dragon").examine("A finely balanced throwing knife.");
        items.add("dragon_thrownaxe", "Dragon thrownaxe", Kind.ARROW, "thrownaxe", 0xB02020).value(1500).rangedStrength(42)
                .tier("dragon").examine("A finely balanced throwing axe.");
        items.add("dragon_boots", "Dragon boots", Kind.ARMOUR, "boots", 0xA02020).value(40000).stack(1).tier("dragon")
                .equipment(wear(Slot.FEET, s(0, 0, 0, -3, -1, 16, 17, 18, 0, 0, 4, 0, 0, 0), Skill.DEFENCE, 60));
        items.add("brine_sabre", "Brine sabre", Kind.MELEE_WEAPON, "scimitar", 0x4A8A9A).value(15000).stack(1).tier("mithril")
                .equipment(weapon(s(7, 47, -2, 0, 0, 0, 1, 0, 0, 0, 46, 0, 0, 0), 4, AttackType.SLASH, WeaponClass.MELEE,
                        Skill.ATTACK, 40, Skill.SLAYER, 47));
        items.add("dragon_sword", "Dragon sword", Kind.MELEE_WEAPON, "longsword", 0xC02020).value(100000).stack(1).tier("dragon")
                .equipment(weapon(s(65, 55, -2, 0, 0, 0, 2, 1, 0, 0, 63, 0, 0, 0), 4, AttackType.STAB, WeaponClass.MELEE,
                        Skill.ATTACK, 60));
        items.add("dragon_harpoon", "Dragon harpoon", Kind.TOOL, "harpoon", 0xC02020).value(100000).stack(1).tier("dragon")
                .examine("A very sharp harpoon.");
        items.add("dust_battlestaff", "Dust battlestaff", Kind.STAFF, "staff_orb", 0xB0A080).value(40000).stack(1)
                .equipment(weapon(s(10, -1, 40, 12, 0, 2, 3, 1, 12, 0, 50, 0, 0, 0), 4, AttackType.MAGIC, WeaponClass.STAFF,
                        Skill.MAGIC, 30, Skill.ATTACK, 30));
        items.add("mist_battlestaff", "Mist battlestaff", Kind.STAFF, "staff_orb", 0xA0B0C0).value(40000).stack(1)
                .equipment(weapon(s(10, -1, 40, 12, 0, 2, 3, 1, 12, 0, 50, 0, 0, 0), 4, AttackType.MAGIC, WeaponClass.STAFF,
                        Skill.MAGIC, 30, Skill.ATTACK, 30));
        items.add("basilisk_jaw", "Basilisk jaw", Kind.SIMPLE, "skull", 0x7A8A5A).value(5000000).stack(1)
                .examine("Could be fused with a helm of neitiznot.");
        items.add("drakes_tooth", "Drake's tooth", Kind.SIMPLE, "tooth", 0xE8E0C0).value(30000).stack(1);
        items.add("drakes_claw", "Drake's claw", Kind.SIMPLE, "claw", 0x9A6A3A).value(30000).stack(1)
                .examine("Use it on boots of stone to make boots of brimstone.");
        items.add("lava_scale", "Lava scale", Kind.SIMPLE, "secondary", 0xE06020).value(4000)
                .examine("Grind it into shards for extended antifires.");
        items.add("dragon_limbs", "Dragon limbs", Kind.SIMPLE, "shaft", 0xA02020).value(500000).stack(1);
        items.add("dragon_metal_slice", "Dragon metal slice", Kind.SIMPLE, "bar", 0xB02020).value(1000000).stack(1);
        items.add("dragon_metal_lump", "Dragon metal lump", Kind.SIMPLE, "ore", 0xB02020).value(1000000).stack(1);
        items.add("rune_boots", "Rune boots", Kind.ARMOUR, "boots", 0x3A6A8A).value(12500).stack(1).tier("rune")
                .equipment(wear(Slot.FEET, s(0, 0, 0, -3, -1, 12, 12, 12, 0, 0, 2, 0, 0, 0), Skill.DEFENCE, 40))
                .examine("These will protect my feet.");
        items.add("mystic_robe_top_dark", "Mystic robe top (dark)", Kind.ARMOUR, "leather_body", 0x2A2A3A).value(120000).stack(1)
                .tier("leather").equipment(wear(Slot.CHEST, s(0, 0, 0, 20, 0, 0, 0, 0, 20, 0, 0, 0, 0, 0), Skill.MAGIC, 40,
                        Skill.DEFENCE, 20))
                .examine("The upper half of a dark magical robe.");
        // Key halves from the gem and rare drop tables (combine into a crystal key in OSRS).
        items.add("loop_half_of_key", "Loop half of key", Kind.SIMPLE, "key", 0xB0A060).value(1).stack(1)
                .examine("The loop end of a key that has been broken in half.");
        items.add("tooth_half_of_key", "Tooth half of key", Kind.SIMPLE, "key", 0xB0A060).value(1).stack(1)
                .examine("The tooth end of a key that has been broken in half.");
        items.add("imbued_heart","Imbued heart", Kind.SIMPLE, "gem", 0xD02060).value(1).stack(1)
                .examine("Temporarily boosts your Magic level (from superior slayer creatures).");
        items.add("eternal_gem", "Eternal gem", Kind.SIMPLE, "gem", 0x8A30C0).value(1).stack(1)
                .examine("An enchanted gem that never runs out of charges.");
        items.add("brimstone_key", "Brimstone key", Kind.SIMPLE, "key", 0xC06020).value(1).stack(64)
                .examine("Opens the brimstone chest at Mount Karuulm.");
        items.add("larran_key", "Larran's key", Kind.SIMPLE, "key", 0xB09060).value(1).stack(64)
                .examine("Opens Larran's big chest in the Wilderness pirates' hideout.");
    }
}
