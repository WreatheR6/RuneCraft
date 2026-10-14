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
        // The other barbarian-smithed hastae (Otto's Grotto, Barbarian Training); bonuses from the wiki DPS calculator.
        items.add("iron_hasta", "Iron hasta", Kind.MELEE_WEAPON, "hasta", 0x8A8A8E).value(91).stack(1).tier("iron")
                .equipment(weapon(s(8, 8, 8, 0, 0, -2, -2, -2, 0, -2, 10, 0, 0, 0), 4, AttackType.STAB, WeaponClass.MELEE))
                .examine("An iron-tipped, one-handed hasta.");
        items.add("steel_hasta", "Steel hasta", Kind.MELEE_WEAPON, "hasta", 0xB8B8C0).value(325).stack(1).tier("steel")
                .equipment(weapon(s(12, 12, 12, 0, 0, -3, -3, -3, 0, -3, 12, 0, 0, 0), 4, AttackType.STAB, WeaponClass.MELEE, Skill.ATTACK, 5))
                .examine("A steel-tipped, one-handed hasta.");
        items.add("adamant_hasta", "Adamant hasta", Kind.MELEE_WEAPON, "hasta", 0x4C7A4C).value(2080).stack(1).tier("adamant")
                .equipment(weapon(s(24, 24, 24, 0, 0, -6, -7, -5, 0, -6, 28, 0, 0, 0), 4, AttackType.STAB, WeaponClass.MELEE, Skill.ATTACK, 30))
                .examine("An adamant-tipped, one-handed hasta.");
        items.add("rune_hasta", "Rune hasta", Kind.MELEE_WEAPON, "hasta", 0x4FB8C8).value(20800).stack(1).tier("rune")
                .equipment(weapon(s(36, 36, 36, 0, 0, -10, -10, -9, 0, -10, 42, 0, 0, 0), 4, AttackType.STAB, WeaponClass.MELEE, Skill.ATTACK, 40))
                .examine("A rune-tipped, one-handed hasta.");
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
        // Imbued salve amulets: Nightmare Zone / Soul Wars imbues (minigames) of the salve amulet (e), and Tarn's diary
        // on the salve amulet(i) (bosses). Worn in the systems module's neck slot.
        items.add("salve_amulet_i", "Salve amulet(i)", Kind.SIMPLE, "amulet", 0x7AE0B0).value(1).stack(1)
                .equipment(wear(Slot.HEAD, s(0, 0, 0, 0, 0, 3, 3, 3, 0, 0, 0, 0, 0, 3)))
                .examine("An imbued salve amulet: also boosts Ranged and Magic against the undead.");
        items.add("salve_amulet_ei", "Salve amulet(ei)", Kind.SIMPLE, "amulet", 0x5AF0A0).value(1).stack(1)
                .equipment(wear(Slot.HEAD, s(0, 0, 0, 0, 0, 3, 3, 3, 0, 0, 0, 0, 0, 3)))
                .examine("An imbued, enchanted salve amulet: +20% in every style against the undead.");
        mystic(items);
        godBooks(items);
        ballistae(items);
        fremennikDrops(items);
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

    /**
     * The mystic pieces the mod lacked: blue hat, gloves and boots (Magic Guild Store, Wizards' Guild) and the dusk set
     * (Brimstone chest, 1/1,000 each). Every mystic piece needs 40 Magic and 20 Defence. Gloves are worn in the systems
     * module's hands slot (Jewellery); their HEAD-slot equipment only feeds tooltips.
     */
    private static void mystic(ItemSink items) {
        EquipmentStats hat = s(0, 0, 0, 4, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0);
        EquipmentStats top = s(0, 0, 0, 20, 0, 0, 0, 0, 20, 0, 0, 0, 0, 0);
        EquipmentStats bottom = s(0, 0, 0, 15, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0);
        EquipmentStats hands = s(0, 0, 0, 3, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0);
        items.add("mystic_hat", "Mystic hat", Kind.ARMOUR, "hood", 0x3050C0).value(15000).stack(1).tier("mystic")
                .equipment(wear(Slot.HEAD, hat, Skill.MAGIC, 40, Skill.DEFENCE, 20))
                .examine("A magical hat.");
        items.add("mystic_gloves", "Mystic gloves", Kind.SIMPLE, "gloves", 0x3050C0).value(10000).stack(1)
                .equipment(wear(Slot.HEAD, hands, Skill.MAGIC, 40, Skill.DEFENCE, 20))
                .examine("Magical gloves.");
        items.add("mystic_boots", "Mystic boots", Kind.ARMOUR, "boots", 0x3050C0).value(10000).stack(1).tier("mystic")
                .equipment(wear(Slot.FEET, hands, Skill.MAGIC, 40, Skill.DEFENCE, 20))
                .examine("Magical boots.");
        int dusk = 0x5A4A6A;
        items.add("mystic_hat_dusk", "Mystic hat (dusk)", Kind.ARMOUR, "hood", dusk).value(15000).stack(1).tier("mystic")
                .equipment(wear(Slot.HEAD, hat, Skill.MAGIC, 40, Skill.DEFENCE, 20))
                .examine("A dusky magical hat.");
        items.add("mystic_robe_top_dusk", "Mystic robe top (dusk)", Kind.ARMOUR, "robe_top", dusk).value(120000).stack(1).tier("mystic")
                .equipment(wear(Slot.CHEST, top, Skill.MAGIC, 40, Skill.DEFENCE, 20))
                .examine("The upper half of a dusky magical robe.");
        items.add("mystic_robe_bottom_dusk", "Mystic robe bottom (dusk)", Kind.ARMOUR, "robe_bottom", dusk).value(80000).stack(1).tier("mystic")
                .equipment(wear(Slot.LEGS, bottom, Skill.MAGIC, 40, Skill.DEFENCE, 20))
                .examine("The lower half of a dusky magical robe.");
        items.add("mystic_gloves_dusk", "Mystic gloves (dusk)", Kind.SIMPLE, "gloves", dusk).value(10000).stack(1)
                .equipment(wear(Slot.HEAD, hands, Skill.MAGIC, 40, Skill.DEFENCE, 20))
                .examine("Dusky magical gloves.");
        items.add("mystic_boots_dusk", "Mystic boots (dusk)", Kind.ARMOUR, "boots", dusk).value(10000).stack(1).tier("mystic")
                .equipment(wear(Slot.FEET, hands, Skill.MAGIC, 40, Skill.DEFENCE, 20))
                .examine("Dusky magical boots.");
    }

    /** God book ids: damaged book, completed book, display name of the completed book, god, examine, colour. */
    public static final String[][] GOD_BOOKS = {
            {"damaged_holy_book", "holy_book", "Holy book", "Saradomin", "The holy book of Saradomin.", "E0E0F0"},
            {"damaged_unholy_book", "unholy_book", "Unholy book", "Zamorak", "The unholy book of Zamorak.", "A02020"},
            {"damaged_book_of_balance", "book_of_balance", "Book of balance", "Guthix", "The holy book of Guthix.", "30A030"},
            {"damaged_book_of_war", "book_of_war", "Book of war", "Bandos", "The short book of the wisdom of Bandos.", "6A5A3A"},
            {"damaged_book_of_law", "book_of_law", "Book of law", "Armadyl", "The book of Armadyl's Law.", "C0C8D8"},
            {"damaged_book_of_darkness", "book_of_darkness", "Book of darkness", "Ancient", "The dark book of a forgotten god.", "5A2A6A"}};

    /**
     * God books (shield slot). The damaged books come from Horror from the Deep and Jossik (quests module); adding the
     * god's four pages (clue scroll rewards, systems module) completes them. Bonuses are the wiki's; no requirements.
     */
    private static void godBooks(ItemSink items) {
        for (String[] b : GOD_BOOKS) {
            int color = Integer.parseInt(b[5], 16);
            items.add(b[0], "Damaged book (" + b[3] + ")", Kind.SHIELD, "book", color).value(200).stack(1)
                    .equipment(wear(Slot.SHIELD, s(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5)))
                    .examine("An incomplete book of " + (b[3].equals("Ancient") ? "a forgotten god" : b[3]) + ".");
            EquipmentStats full = switch (b[1]) {
                case "holy_book" -> s(0, 0, 0, 0, 0, 8, 8, 8, 8, 8, 0, 0, 0, 5);
                case "unholy_book" -> s(8, 8, 8, 8, 8, 0, 0, 0, 0, 0, 0, 0, 0, 5);
                case "book_of_balance" -> s(4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 0, 0, 0, 5);
                case "book_of_war" -> s(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 5);
                case "book_of_law" -> s(0, 0, 0, 0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 5);
                default -> s(0, 0, 0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5);
            };
            items.add(b[1], b[2], Kind.SHIELD, "book", color).value(200).stack(1)
                    .equipment(wear(Slot.SHIELD, full))
                    .examine(b[4]);
        }
    }

    /**
     * Ballistae (Monkey Madness II): ballista limbs on a light/heavy frame (47/72 Fletching), then a ballista spring,
     * then a monkey tail (skilling module recipes). The frames, limbs and spring are defined in {@link CommonItems} so a
     * demonic gorilla drop table can use them. They fire javelins only ({@code AmmoRules}); the heavy ballista needs
     * Monkey Madness II to wield (quests module gate). Two-handed, speed 7 ({@code EquipmentBonuses}).
     */
    private static void ballistae(ItemSink items) {
        items.add("incomplete_light_ballista", "Incomplete light ballista", Kind.SIMPLE, "crossbow_u", 0x8A6A3A).value(50000).stack(1)
                .examine("The limbs have been attached to the frame.");
        items.add("incomplete_heavy_ballista", "Incomplete heavy ballista", Kind.SIMPLE, "crossbow_u", 0x5A4A3A).value(50000).stack(1)
                .examine("The limbs have been attached to the frame.");
        items.add("unstrung_light_ballista", "Unstrung light ballista", Kind.SIMPLE, "crossbow_u", 0x8A6A3A).value(50000).stack(1)
                .examine("Needs a suitably simian rope.");
        items.add("unstrung_heavy_ballista", "Unstrung heavy ballista", Kind.SIMPLE, "crossbow_u", 0x5A4A3A).value(50000).stack(1)
                .examine("Needs a suitably simian rope.");
        items.add("light_ballista", "Light ballista", Kind.BOW, "crossbow", 0x8A6A3A).value(125000).stack(1).tier("magic")
                .equipment(weapon(s(0, 0, 0, 0, 110, 0, 0, 0, 0, 0, 0, 0, 0, 0), 7, AttackType.RANGED, WeaponClass.BOW, Skill.RANGED, 65))
                .examine("A weapon forged from the wreckage of an airship.");
        items.add("heavy_ballista", "Heavy ballista", Kind.BOW, "crossbow", 0x5A4A3A).value(950000).stack(1).tier("magic")
                .equipment(weapon(s(0, 0, 0, 0, 125, 0, 0, 0, 0, 0, 0, 15, 0, 0), 7, AttackType.RANGED, WeaponClass.BOW, Skill.RANGED, 75))
                .examine("A powerful weapon forged from the wreckage of an airship.");
    }

    /**
     * Wearables dropped by monsters whose drop lines were skipped until now (OSRS Wiki drop tables, see
     * {@link com.runecraft.osrs.data.MonsterDropsTable}): the Waterbirth dagannoth's Fremennik clothing, spined pieces and
     * Neitiznot helms, the rock-shell and skeletal gloves and boots of giant rock crabs and wallasalki, the Ancient
     * Prison's ceremonial robes, the terror dog's granite helm, the dark wizards' black robe and the crawling hands'
     * coloured gloves. Bonuses are the wiki's ({@code EquipmentWikiSyncTest}); gloves and capes are worn in the systems
     * module's hands/cape slots (Jewellery). The Fremennik Trials requirement of the helms is not enforced (approx). The
     * spined, rock-shell and skeletal helms are made from dagannoth hide by Rellekka's craftsmen (CraftingData, at the
     * tanner); their materials are in {@link CommonItems}.
     */
    private static void fremennikDrops(ItemSink items) {
        items.add("berserker_helm", "Berserker helm", Kind.ARMOUR, "full_helm", 0x8A8A9A).value(60000).stack(1).tier("rune")
                .equipment(wear(Slot.HEAD, s(0, 0, 0, -5, -5, 31, 29, 33, 0, 30, 3, 0, 0, 0), Skill.DEFENCE, 45))
                .examine("This helmet is worn by berserkers.");
        items.add("warrior_helm", "Warrior helm", Kind.ARMOUR, "full_helm", 0x9A7A5A).value(60000).stack(1).tier("rune")
                .equipment(wear(Slot.HEAD, s(0, 5, 0, -5, -5, 31, 33, 29, 0, 30, 0, 0, 0, 0), Skill.DEFENCE, 45))
                .examine("This helmet is worn by warriors.");
        EquipmentStats hands = s(0, 0, 0, 0, 0, 0, 1, 2, 0, 0, 0, 0, 0, 0);
        EquipmentStats feet = s(0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0);
        String[][] pieces = {
                {"spined", "Spined", "6A8A6A", "spined dagannoth hide", "Some finely crafted Fremennik boots, made from spined dagannoth hide."},
                {"rock_shell", "Rock-shell", "8A7A5A", "rock crab shell shards", "Some Fremennik boots, made from the shards of a rock crab's shell."},
                {"skeletal", "Skeletal", "D8D0B8", "wallasalki bones fragments", "Some finely crafted Fremennik boots, made from the bones of a wallasalki."}};
        for (String[] f : pieces) {
            int rgb = Integer.parseInt(f[2], 16);
            items.add(f[0] + "_gloves", f[1] + " gloves", Kind.SIMPLE, "gloves", rgb).value(650).stack(1)
                    .equipment(wear(Slot.HEAD, hands)).examine("Fremennik gloves stitched together from " + f[3] + ".");
            items.add(f[0] + "_boots", f[1] + " boots", Kind.ARMOUR, "boots", rgb).value(650).stack(1).tier("leather")
                    .equipment(wear(Slot.FEET, feet)).examine(f[4]);
        }
        items.add("spined_helm", "Spined helm", Kind.ARMOUR, "full_helm", 0x6A8A6A).value(60000).stack(1).tier("leather")
                .equipment(wear(Slot.HEAD, s(-6, -6, -6, -6, 6, 6, 6, 6, 6, 0, 0, 0, 0, 0), Skill.DEFENCE, 40, Skill.RANGED, 40))
                .examine("A helm fit for any Fremennik ranger.");
        items.add("rock_shell_helm", "Rock-shell helm", Kind.ARMOUR, "full_helm", 0x8A7A5A).value(35200).stack(1).tier("rune")
                .equipment(wear(Slot.HEAD, s(0, 0, 0, -6, -2, 30, 32, 27, -1, 30, 0, 0, 0, 0), Skill.DEFENCE, 40))
                .examine("Protective headwear made from crabs. Better than that sounds.");
        items.add("skeletal_helm", "Skeletal helm", Kind.ARMOUR, "full_helm", 0xD8D0B8).value(10000).stack(1).tier("mystic")
                .equipment(wear(Slot.HEAD, s(0, 0, 0, 2, -2, 10, 9, 11, 3, 0, 0, 0, 0, 0), Skill.DEFENCE, 40, Skill.MAGIC, 40))
                .examine("Make your foes cower by wearing a skull as a helmet!");
        items.add("fremennik_boots", "Fremennik boots", Kind.ARMOUR, "boots", 0x7A5A3A).value(500).stack(1).tier("leather")
                .equipment(wear(Slot.FEET, feet)).examine("Very stylish!");
        items.add("fremennik_gloves", "Fremennik gloves", Kind.SIMPLE, "gloves", 0x8A6A4A).value(500).stack(1)
                .equipment(wear(Slot.HEAD, hands)).examine("These will keep my hands warm!");
        items.add("fremennik_brown_shirt", "Fremennik brown shirt", Kind.ARMOUR, "robe_top", 0x7A5A3A).value(250).stack(1).tier("leather")
                .equipment(wear(Slot.CHEST, s(0, 0, 0, 0, 0, 0, 2, 2, 0, 0, 0, 0, 0, 0))).examine("The latest in Fremennik fashion.");
        items.add("fremennik_brown_cloak", "Fremennik brown cloak", Kind.SIMPLE, "cape", 0x7A5A3A).value(250).stack(1)
                .equipment(wear(Slot.HEAD, s(0, 0, 0, 0, 0, 0, 1, 1, 0, 2, 0, 0, 0, 0))).examine("The latest fashion in Rellekka.");
        int ceremonial = 0x5A3A6A;
        items.add("ancient_ceremonial_mask", "Ancient ceremonial mask", Kind.ARMOUR, "hood", ceremonial).value(50000).stack(1).tier("mystic")
                .equipment(wear(Slot.HEAD, s(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2)))
                .examine("A ceremonial mask dedicated to an ancient god.");
        items.add("ancient_ceremonial_top", "Ancient ceremonial top", Kind.ARMOUR, "robe_top", ceremonial).value(100000).stack(1).tier("mystic")
                .equipment(wear(Slot.CHEST, s(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6)))
                .examine("Some ceremonial robes dedicated to an ancient god.");
        items.add("ancient_ceremonial_legs", "Ancient ceremonial legs", Kind.ARMOUR, "robe_bottom", ceremonial).value(80000).stack(1).tier("mystic")
                .equipment(wear(Slot.LEGS, s(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5)))
                .examine("Some ceremonial robes dedicated to an ancient god.");
        items.add("ancient_ceremonial_gloves", "Ancient ceremonial gloves", Kind.SIMPLE, "gloves", ceremonial).value(40000).stack(1)
                .equipment(wear(Slot.HEAD, s(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1)))
                .examine("Some ceremonial gloves dedicated to an ancient god.");
        items.add("ancient_ceremonial_boots", "Ancient ceremonial boots", Kind.ARMOUR, "boots", ceremonial).value(40000).stack(1).tier("mystic")
                .equipment(wear(Slot.FEET, s(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1)))
                .examine("Some ceremonial boots dedicated to an ancient god.");
        items.add("granite_helm", "Granite helm", Kind.ARMOUR, "full_helm", 0x7A7A80).value(46000).stack(1).tier("rune")
                .equipment(wear(Slot.HEAD, s(0, 0, 0, -9, -7, 31, 33, 29, -1, 39, 0, 0, 0, 0), Skill.DEFENCE, 50, Skill.STRENGTH, 50))
                .examine("A stone helmet.");
        items.add("black_robe", "Black robe", Kind.ARMOUR, "robe_top", 0x2A2A2A).value(13).stack(1).tier("mystic")
                .equipment(wear(Slot.CHEST, s(0, 0, 0, 3, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0))).examine("I can do magic better in this.");
        for (String[] g : new String[][]{{"purple", "Purple", "6A3A8A"}, {"red", "Red", "A02A2A"}, {"teal", "Teal", "2A8A8A"},
                {"yellow", "Yellow", "D0B030"}}) {
            items.add(g[0] + "_gloves", g[1] + " gloves", Kind.SIMPLE, "gloves", Integer.parseInt(g[2], 16)).value(500).stack(1)
                    .equipment(wear(Slot.HEAD, hands)).examine("These will keep my hands warm!");
        }
    }
}
