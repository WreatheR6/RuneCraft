package com.runecraft.osrs.content;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentDef.Slot;
import com.runecraft.osrs.EquipmentDef.WeaponClass;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.Skill;

import java.util.EnumMap;
import java.util.Map;

/**
 * OSRS equipment that RuneCraft was missing although the content that yields it already exists: metal boots, spears,
 * halberds and thrownaxes dropped by slayer monsters, the Dagannoth Kings' armour, mystic robes and staves, the salve
 * amulet (Haunted Mine) and the dragonfire shield (draconic visage). God d'hide lives in ClueRewards and the dragon dart
 * in FletchingData.
 *
 * <p>Bonuses, attack speed, requirements, value and examine text are the OSRS Wiki values (weirdgloop/osrs-dps-calc
 * {@code equipment.json}, Flipping-Utilities/parsed-osrs {@code all-items.json}). Two-handed spears and halberds are
 * flagged in {@link com.runecraft.osrs.data.EquipmentBonuses}. Every item here has a source (monster and boss drop
 * tables, the TzHaar and Betty's shops, smithing/crafting/fletching recipes); {@code EquipmentWikiSyncTest} checks it.
 */
public final class CommonEquipment {
    private CommonEquipment() {
    }

    private static EquipmentStats s(int sa, int sl, int cr, int ma, int ra, int ds, int dsl, int dc, int dm, int dr,
                                    int str, int rstr, int mdmg, int pray) {
        return new EquipmentStats(sa, sl, cr, ma, ra, ds, dsl, dc, dm, dr, str, rstr, mdmg, pray);
    }

    private static Map<Skill, Integer> reqs(Object... pairs) {
        Map<Skill, Integer> m = new EnumMap<>(Skill.class);
        for (int i = 0; i < pairs.length; i += 2) {
            m.put((Skill) pairs[i], (Integer) pairs[i + 1]);
        }
        return m;
    }

    private static EquipmentDef wear(Slot slot, EquipmentStats stats, Object... req) {
        return new EquipmentDef(slot, stats, reqs(req), 0, AttackType.CRUSH, WeaponClass.NONE);
    }

    private static EquipmentDef weapon(EquipmentStats stats, int speed, AttackType type, WeaponClass cls, Object... req) {
        return new EquipmentDef(Slot.WEAPON, stats, reqs(req), speed, type, cls);
    }

    public static void add(ItemSink items) {
        items.add("bronze_boots", "Bronze boots", Kind.ARMOUR, "boots", 0xB08D57).value(24).stack(1).tier("bronze")
                .equipment(wear(Slot.FEET, s(0, 0, 0, -3, -1, 1, 2, 3, 0, 0, 0, 0, 0, 0)))
                .examine("These will protect my feet.");
        items.add("iron_boots", "Iron boots", Kind.ARMOUR, "boots", 0x8A8A8E).value(84).stack(1).tier("iron")
                .equipment(wear(Slot.FEET, s(0, 0, 0, -3, -1, 2, 3, 4, 0, 0, 0, 0, 0, 0)))
                .examine("These will protect my feet.");
        items.add("steel_boots", "Steel boots", Kind.ARMOUR, "boots", 0xB8B8C0).value(300).stack(1).tier("steel")
                .equipment(wear(Slot.FEET, s(0, 0, 0, -3, -1, 5, 6, 7, 0, 0, 0, 0, 0, 0), Skill.DEFENCE, 5))
                .examine("These will protect my feet.");
        items.add("black_boots", "Black boots", Kind.ARMOUR, "boots", 0x2E2E34).value(576).stack(1).tier("black")
                .equipment(wear(Slot.FEET, s(0, 0, 0, -3, -1, 7, 8, 9, 0, 0, 0, 0, 0, 0), Skill.DEFENCE, 10))
                .examine("These will protect my feet.");
        items.add("mithril_boots", "Mithril boots", Kind.ARMOUR, "boots", 0x4A5A9C).value(780).stack(1).tier("mithril")
                .equipment(wear(Slot.FEET, s(0, 0, 0, -3, -1, 8, 9, 10, 0, 0, 0, 0, 0, 0), Skill.DEFENCE, 20))
                .examine("These will protect my feet.");
        items.add("adamant_boots", "Adamant boots", Kind.ARMOUR, "boots", 0x4C7A4C).value(1920).stack(1).tier("adamant")
                .equipment(wear(Slot.FEET, s(0, 0, 0, -3, -1, 10, 11, 12, 0, 0, 1, 0, 0, 0), Skill.DEFENCE, 30))
                .examine("These will protect my feet.");
        items.add("bronze_spear", "Bronze spear", Kind.MELEE_WEAPON, "spear", 0xB08D57).value(26).stack(1).tier("bronze")
                .equipment(weapon(s(5, 5, 5, 0, 0, 1, 1, 0, 0, 0, 6, 0, 0, 0), 4, AttackType.STAB, WeaponClass.MELEE))
                .examine("A bronze tipped spear.");
        items.add("iron_spear", "Iron spear", Kind.MELEE_WEAPON, "spear", 0x8A8A8E).value(91).stack(1).tier("iron")
                .equipment(weapon(s(8, 8, 8, 0, 0, 1, 1, 0, 0, 0, 10, 0, 0, 0), 4, AttackType.STAB, WeaponClass.MELEE))
                .examine("An iron tipped spear.");
        items.add("steel_spear", "Steel spear", Kind.MELEE_WEAPON, "spear", 0xB8B8C0).value(325).stack(1).tier("steel")
                .equipment(weapon(s(12, 12, 12, 0, 0, 1, 1, 0, 0, 0, 12, 0, 0, 0), 4, AttackType.STAB, WeaponClass.MELEE, Skill.ATTACK, 5))
                .examine("A steel tipped spear.");
        items.add("mithril_spear", "Mithril spear", Kind.MELEE_WEAPON, "spear", 0x4A5A9C).value(845).stack(1).tier("mithril")
                .equipment(weapon(s(17, 17, 17, 0, 0, 1, 1, 0, 0, 0, 18, 0, 0, 0), 4, AttackType.STAB, WeaponClass.MELEE, Skill.ATTACK, 20))
                .examine("A mithril tipped spear.");
        items.add("adamant_spear", "Adamant spear", Kind.MELEE_WEAPON, "spear", 0x4C7A4C).value(2080).stack(1).tier("adamant")
                .equipment(weapon(s(24, 24, 24, 0, 0, 1, 1, 0, 0, 0, 28, 0, 0, 0), 4, AttackType.STAB, WeaponClass.MELEE, Skill.ATTACK, 30))
                .examine("An adamantite tipped spear.");
        items.add("mithril_hasta", "Mithril hasta", Kind.MELEE_WEAPON, "hasta", 0x4A5A9C).value(845).stack(1).tier("mithril")
                .equipment(weapon(s(17, 17, 17, 0, 0, -5, -5, -4, 0, -5, 18, 0, 0, 0), 4, AttackType.STAB, WeaponClass.MELEE, Skill.ATTACK, 20))
                .examine("A mithril-tipped, one-handed hasta.");
        items.add("bronze_halberd", "Bronze halberd", Kind.MELEE_WEAPON, "halberd", 0xB08D57).value(80).stack(1).tier("bronze")
                .equipment(weapon(s(7, 8, 0, -4, 0, -1, 1, 2, 0, 0, 8, 0, 0, 0), 7, AttackType.SLASH, WeaponClass.MELEE))
                .examine("A bronze halberd.");
        items.add("iron_halberd", "Iron halberd", Kind.MELEE_WEAPON, "halberd", 0x8A8A8E).value(280).stack(1).tier("iron")
                .equipment(weapon(s(9, 12, 0, -4, 0, -1, 1, 2, 0, 0, 12, 0, 0, 0), 7, AttackType.SLASH, WeaponClass.MELEE))
                .examine("An iron halberd.");
        items.add("rune_halberd", "Rune halberd", Kind.MELEE_WEAPON, "halberd", 0x4FB8C8).value(64000).stack(1).tier("rune")
                .equipment(weapon(s(48, 67, 0, -4, 0, -1, 4, 5, 0, 0, 68, 0, 0, 0), 7, AttackType.SLASH, WeaponClass.MELEE, Skill.ATTACK, 40, Skill.STRENGTH, 20))
                .examine("A rune halberd.");
        items.add("fremennik_blade", "Fremennik blade", Kind.MELEE_WEAPON, "scimitar", 0x9A8A6A).value(5000).stack(1).tier("steel")
                .equipment(weapon(s(6, 29, -2, 0, 0, 0, 1, 0, 0, 0, 28, 0, 0, 0), 4, AttackType.SLASH, WeaponClass.MELEE, Skill.ATTACK, 30))
                .examine("A sword used only by Fremennik warriors.");
        items.add("fremennik_helm", "Fremennik helm", Kind.ARMOUR, "full_helm", 0x9A8A6A).value(5000).stack(1).tier("steel")
                .equipment(wear(Slot.HEAD, s(0, 0, 0, -6, -2, 19, 21, 16, -1, 19, 0, 0, 0, 0), Skill.DEFENCE, 30))
                .examine("A sturdy helm worn only by Fremennik clan members.");
        items.add("fremennik_shield", "Fremennik shield", Kind.SHIELD, "shield_round", 0x9A8A6A).value(5000).stack(1).tier("steel")
                .equipment(wear(Slot.SHIELD, s(0, 0, 0, -8, -2, 27, 31, 29, -1, 29, 0, 0, 0, 0), Skill.DEFENCE, 30))
                .examine("A shield worn by Fremennik warriors.");
        items.add("farseer_helm", "Farseer helm", Kind.ARMOUR, "full_helm", 0x6A7AA0).value(60000).stack(1).tier("rune")
                .equipment(wear(Slot.HEAD, s(-5, -5, -5, 6, -5, 8, 10, 12, 6, 0, 0, 0, 0, 0), Skill.DEFENCE, 45))
                .examine("This helmet is worn by farseers.");
        items.add("archer_helm", "Archer helm", Kind.ARMOUR, "full_helm", 0x6A8A5A).value(60000).stack(1).tier("rune")
                .equipment(wear(Slot.HEAD, s(-5, -5, -5, -5, 6, 6, 8, 10, 6, 6, 0, 0, 0, 0), Skill.DEFENCE, 45))
                .examine("This helmet is worn by archers.");
        items.add("rock_shell_plate", "Rock-shell plate", Kind.ARMOUR, "platebody", 0x8A7A5A).value(65000).stack(1).tier("rune")
                .equipment(wear(Slot.CHEST, s(0, 0, 0, -30, -10, 82, 80, 72, -6, 80, 0, 0, 0, 0), Skill.DEFENCE, 40))
                .examine("A sturdy body armour made from rock crab pieces.");
        items.add("rock_shell_legs", "Rock-shell legs", Kind.ARMOUR, "platelegs", 0x8A7A5A).value(64000).stack(1).tier("rune")
                .equipment(wear(Slot.LEGS, s(0, 0, 0, -21, -7, 51, 49, 47, -4, 49, 0, 0, 0, 0), Skill.DEFENCE, 40))
                .examine("Some tough leggings made from rock crab parts.");
        items.add("spined_body", "Spined body", Kind.ARMOUR, "leather_body", 0x6A8A6A).value(7800).stack(1).tier("leather")
                .equipment(wear(Slot.CHEST, s(0, 0, 0, -15, 15, 40, 32, 45, 20, 40, 0, 0, 0, 0), Skill.DEFENCE, 40, Skill.RANGED, 40))
                .examine("A constant reminder that I'm above a Dagannoth in the food chain.");
        items.add("spined_chaps", "Spined chaps", Kind.ARMOUR, "chaps", 0x6A8A6A).value(3900).stack(1).tier("leather")
                .equipment(wear(Slot.LEGS, s(0, 0, 0, -10, 8, 22, 16, 24, 8, 22, 0, 0, 0, 0), Skill.DEFENCE, 40, Skill.RANGED, 40))
                .examine("Stylish leg armour for rangers with a lingering smell of raw fish...");
        items.add("skeletal_top", "Skeletal top", Kind.ARMOUR, "robe_top", 0xD8D0B8).value(45000).stack(1).tier("mystic")
                .equipment(wear(Slot.CHEST, s(0, 0, 0, 8, -10, 35, 25, 42, 15, 0, 0, 0, 0, 0), Skill.DEFENCE, 40, Skill.MAGIC, 40))
                .examine("The bones in this armour seem to vibrate with a magical quality...");
        items.add("skeletal_bottoms", "Skeletal bottoms", Kind.ARMOUR, "robe_bottom", 0xD8D0B8).value(40000).stack(1).tier("mystic")
                .equipment(wear(Slot.LEGS, s(0, 0, 0, 6, -7, 22, 20, 24, 10, 0, 0, 0, 0, 0), Skill.DEFENCE, 40, Skill.MAGIC, 40))
                .examine("A superior set of strengthened slacks for any self respecting seer.");
        items.add("mystic_robe_top", "Mystic robe top", Kind.ARMOUR, "robe_top", 0x3050C0).value(120000).stack(1).tier("mystic")
                .equipment(wear(Slot.CHEST, s(0, 0, 0, 20, 0, 0, 0, 0, 20, 0, 0, 0, 0, 0), Skill.MAGIC, 40, Skill.DEFENCE, 20))
                .examine("The upper half of a magical robe.");
        items.add("mystic_robe_bottom", "Mystic robe bottom", Kind.ARMOUR, "robe_bottom", 0x3050C0).value(80000).stack(1).tier("mystic")
                .equipment(wear(Slot.LEGS, s(0, 0, 0, 15, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0), Skill.MAGIC, 40, Skill.DEFENCE, 20))
                .examine("The lower half of a magical robe.");
        items.add("mystic_hat_light", "Mystic hat (light)", Kind.ARMOUR, "hood", 0xD8D0E8).value(15000).stack(1).tier("mystic")
                .equipment(wear(Slot.HEAD, s(0, 0, 0, 4, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0), Skill.MAGIC, 40, Skill.DEFENCE, 20))
                .examine("A bright magical hat.");
        items.add("mystic_robe_top_light", "Mystic robe top (light)", Kind.ARMOUR, "robe_top", 0xD8D0E8).value(120000).stack(1).tier("mystic")
                .equipment(wear(Slot.CHEST, s(0, 0, 0, 20, 0, 0, 0, 0, 20, 0, 0, 0, 0, 0), Skill.MAGIC, 40, Skill.DEFENCE, 20))
                .examine("The upper half of a bright magical robe.");
        items.add("mystic_robe_bottom_light", "Mystic robe bottom (light)", Kind.ARMOUR, "robe_bottom", 0xD8D0E8).value(80000).stack(1).tier("mystic")
                .equipment(wear(Slot.LEGS, s(0, 0, 0, 15, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0), Skill.MAGIC, 40, Skill.DEFENCE, 20))
                .examine("The lower half of a bright magical robe.");
        items.add("mystic_boots_light", "Mystic boots (light)", Kind.ARMOUR, "boots", 0xD8D0E8).value(10000).stack(1).tier("mystic")
                .equipment(wear(Slot.FEET, s(0, 0, 0, 3, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0), Skill.MAGIC, 40, Skill.DEFENCE, 20))
                .examine("Bright magical boots.");
        items.add("mystic_hat_dark", "Mystic hat (dark)", Kind.ARMOUR, "hood", 0x2A2A3A).value(15000).stack(1).tier("mystic")
                .equipment(wear(Slot.HEAD, s(0, 0, 0, 4, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0), Skill.MAGIC, 40, Skill.DEFENCE, 20))
                .examine("A dark magical hat.");
        items.add("mystic_robe_bottom_dark", "Mystic robe bottom (dark)", Kind.ARMOUR, "robe_bottom", 0x2A2A3A).value(80000).stack(1).tier("mystic")
                .equipment(wear(Slot.LEGS, s(0, 0, 0, 15, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0), Skill.MAGIC, 40, Skill.DEFENCE, 20))
                .examine("The lower half of a dark magical robe.");
        items.add("mystic_boots_dark", "Mystic boots (dark)", Kind.ARMOUR, "boots", 0x2A2A3A).value(10000).stack(1).tier("mystic")
                .equipment(wear(Slot.FEET, s(0, 0, 0, 3, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0), Skill.MAGIC, 40, Skill.DEFENCE, 20))
                .examine("Dark magical boots.");
        items.add("mystic_air_staff", "Mystic air staff", Kind.STAFF, "staff_orb", 0xD0E8F0).value(42500).stack(1).tier("rune")
                .equipment(weapon(s(10, -1, 40, 14, 0, 2, 3, 1, 14, 0, 50, 0, 0, 0), 5, AttackType.CRUSH, WeaponClass.STAFF, Skill.MAGIC, 40, Skill.ATTACK, 40))
                .examine("It's a slightly magical stick.");
        items.add("mystic_water_staff", "Mystic water staff", Kind.STAFF, "staff_orb", 0x3060E0).value(42500).stack(1).tier("rune")
                .equipment(weapon(s(10, -1, 40, 14, 0, 2, 3, 1, 14, 0, 50, 0, 0, 0), 5, AttackType.CRUSH, WeaponClass.STAFF, Skill.MAGIC, 40, Skill.ATTACK, 40))
                .examine("It's a slightly magical stick.");
        items.add("mystic_earth_staff", "Mystic earth staff", Kind.STAFF, "staff_orb", 0x6A4A2A).value(42500).stack(1).tier("rune")
                .equipment(weapon(s(10, -1, 40, 14, 0, 2, 3, 1, 14, 0, 50, 0, 0, 0), 5, AttackType.CRUSH, WeaponClass.STAFF, Skill.MAGIC, 40, Skill.ATTACK, 40))
                .examine("It's a slightly magical stick.");
        items.add("mystic_fire_staff", "Mystic fire staff", Kind.STAFF, "staff_orb", 0xE04020).value(42500).stack(1).tier("rune")
                .equipment(weapon(s(10, -1, 40, 14, 0, 2, 3, 1, 14, 0, 50, 0, 0, 0), 5, AttackType.CRUSH, WeaponClass.STAFF, Skill.MAGIC, 40, Skill.ATTACK, 40))
                .examine("It's a slightly magical stick.");
        items.add("lava_battlestaff", "Lava battlestaff", Kind.STAFF, "staff_orb", 0xC05020).value(17000).stack(1).tier("adamant")
                .equipment(weapon(s(7, -1, 28, 12, 0, 2, 3, 1, 12, 0, 35, 0, 0, 0), 5, AttackType.CRUSH, WeaponClass.STAFF, Skill.MAGIC, 30, Skill.ATTACK, 30))
                .examine("It's a slightly magical stick.");
        items.add("blue_wizard_hat", "Blue wizard hat", Kind.ARMOUR, "hood", 0x3050C0).value(2).stack(1).tier("cloth")
                .equipment(wear(Slot.HEAD, s(0, 0, 0, 2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0)))
                .examine("A silly pointed hat.");
        items.add("flippers", "Flippers", Kind.ARMOUR, "boots", 0x3A8A8A).value(1).stack(1).tier("leather")
                .equipment(wear(Slot.FEET, s(-2, -2, -2, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0)))
                .examine("Strangely uncomfortable flippers.");
        items.add("dragonfire_shield", "Dragonfire shield", Kind.SHIELD, "kiteshield", 0x8A3A2A).value(2000000).stack(1).tier("dragon")
                .equipment(wear(Slot.SHIELD, s(0, 0, 0, -10, -5, 70, 75, 72, 10, 72, 7, 0, 0, 0), Skill.DEFENCE, 75))
                .examine("A heavy shield with a snarling, draconic visage.");
        // Worn in the systems module's extra slots (see Jewellery): SIMPLE items whose HEAD-slot equipment only feeds tooltips.
        items.add("mystic_gloves_light", "Mystic gloves (light)", Kind.SIMPLE, "gloves", 0xD8D0E8).value(10000).stack(1)
                .equipment(wear(Slot.HEAD, s(0, 0, 0, 3, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0)))
                .examine("Bright magical gloves.");
        items.add("mystic_gloves_dark", "Mystic gloves (dark)", Kind.SIMPLE, "gloves", 0x2A2A3A).value(10000).stack(1)
                .equipment(wear(Slot.HEAD, s(0, 0, 0, 3, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0)))
                .examine("Dark magical gloves.");
        items.add("black_cape", "Black cape", Kind.SIMPLE, "cape", 0x202020).value(7).stack(1)
                .equipment(wear(Slot.HEAD, s(0, 0, 0, 0, 0, 0, 1, 1, 0, 2, 0, 0, 0, 0)))
                .examine("A warm black cape.");
        items.add("salve_amulet", "Salve amulet", Kind.SIMPLE, "amulet", 0x9AE0C0).value(1).stack(1)
                .equipment(wear(Slot.HEAD, s(0, 0, 0, 0, 0, 3, 3, 3, 0, 0, 0, 0, 0, 3)))
                .examine("Increases the wearer's strength and accuracy by 15%{{Sic|actually 1/6th}} when fighting the undead.");
        items.add("salve_amulet_e", "Salve amulet (e)", Kind.SIMPLE, "amulet", 0x6AF0B0).value(300).stack(1)
                .equipment(wear(Slot.HEAD, s(0, 0, 0, 0, 0, 3, 3, 3, 0, 0, 0, 0, 0, 3)))
                .examine("Increases the wearer's Strength and accuracy by 20% when fighting the undead.");
        // Thrown weapons: Kind.ARROW items thrown on right-click (FletchingData.THROWN).
        items.add("steel_thrownaxe", "Steel thrownaxe", Kind.ARROW, "throwing_axe", 0xB8B8C0).value(26).rangedStrength(11).tier("steel")
                .examine("A finely balanced throwing axe.");
        items.add("mithril_thrownaxe", "Mithril thrownaxe", Kind.ARROW, "throwing_axe", 0x4A5A9C).value(70).rangedStrength(16).tier("mithril")
                .examine("A finely balanced throwing axe.");
        items.add("rune_thrownaxe", "Rune thrownaxe", Kind.ARROW, "throwing_axe", 0x4FB8C8).value(440).rangedStrength(36).tier("rune")
                .examine("A finely balanced throwing axe.");
        items.add("black_knife", "Black knife", Kind.ARROW, "throwing_knife", 0x2E2E34).value(19).rangedStrength(8).tier("black")
                .examine("A finely balanced throwing knife.");
        items.add("toktz_xil_ul", "Toktz-xil-ul", Kind.ARROW, "chakram", 0x3A2A2A).value(250).rangedStrength(49).tier("obsidian")
                .examine("A razor sharp ring of obsidian.");
    }
}
