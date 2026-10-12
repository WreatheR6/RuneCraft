package com.runecraft.osrs;

import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.osrs.data.AmmoRules;
import com.runecraft.osrs.data.EquipmentBonuses;
import com.runecraft.osrs.data.SetEffects;
import com.runecraft.osrs.data.Untradeables;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Equipment bonuses, speeds, requirements, two-handedness, ammo rules and set effects against OSRS. */
class EquipmentReferenceTest {

    private static int[] line(EquipmentStats s) {
        return new int[]{s.stabAttack(), s.slashAttack(), s.crushAttack(), s.magicAttack(), s.rangedAttack(),
                s.stabDefence(), s.slashDefence(), s.crushDefence(), s.magicDefence(), s.rangedDefence(),
                s.meleeStrength(), s.rangedStrength(), s.magicDamagePercent(), s.prayer()};
    }

    /** Asserts an item's full equipment stat line (attack x5, defence x5, str, ranged str, magic dmg %, prayer). */
    private static void stats(String id, int... expected) {
        ItemDef def = ItemCatalog.get(id);
        assertNotNull(def, id);
        assertNotNull(def.equipment(), id);
        assertArrayEquals(expected, line(def.equipment().stats()), id);
    }

    private static void worn(String id, int... expected) {
        Jewellery.Worn w = Jewellery.worn(id);
        assertNotNull(w, id);
        assertArrayEquals(expected, line(w.stats()), id);
    }

    private static int speed(String id) {
        return ItemCatalog.get(id).equipment().attackSpeed();
    }

    private static Map<Skill, Integer> req(String id) {
        return ItemCatalog.get(id).equipment().requirements();
    }

    @Test
    void meleeTiers() {
        stats("bronze_dagger", 4, 2, -4, 1, 0, 0, 0, 0, 1, 0, 3, 0, 0, 0);
        stats("iron_scimitar", 2, 10, -2, 0, 0, 0, 1, 0, 0, 0, 9, 0, 0, 0);
        stats("black_scimitar", 4, 19, -2, 0, 0, 0, 1, 0, 0, 0, 14, 0, 0, 0);
        stats("mithril_battleaxe", -2, 22, 17, 0, 0, 0, 0, 0, 0, -1, 29, 0, 0, 0);
        stats("adamant_platebody", 0, 0, 0, -30, -15, 65, 63, 55, -6, 63, 0, 0, 0, 0);
        stats("rune_dagger", 25, 12, -4, 1, 0, 0, 0, 0, 1, 0, 24, 0, 0, 0);
        stats("rune_scimitar", 7, 45, -2, 0, 0, 0, 1, 0, 0, 0, 44, 0, 0, 0);
        stats("rune_warhammer", -4, -4, 53, -4, 0, 0, 0, 0, 0, 0, 62, 0, 0, 0);
        stats("rune_platebody", 0, 0, 0, -30, -15, 82, 80, 72, -6, 80, 0, 0, 0, 0);
        stats("rune_kiteshield", 0, 0, 0, -8, -3, 44, 48, 46, -1, 46, 0, 0, 0, 0);
        stats("dragon_longsword", 58, 69, -2, 0, 0, 0, 3, 2, 0, 0, 71, 0, 0, 0);
        stats("dragon_kiteshield", 0, 0, 0, -8, -3, 56, 60, 58, -1, 58, 0, 0, 0, 0);
        stats("dragon_mace", 40, -2, 60, 0, 0, 0, 0, 0, 0, 0, 55, 0, 0, 5);
        stats("dragon_defender", 25, 24, 23, -3, -2, 25, 24, 23, -3, -2, 6, 0, 0, 0);
        assertEquals(4, speed("rune_scimitar"));
        assertEquals(5, speed("dragon_longsword"));
        assertEquals(4, speed("rune_mace"));
        assertEquals(7, speed("rune_2h_sword"));
        assertEquals(40, req("rune_scimitar").get(Skill.ATTACK));
        assertEquals(60, req("dragon_med_helm").get(Skill.DEFENCE));
        assertEquals(60, req("dragon_defender").get(Skill.ATTACK));
        assertEquals(40, req("rune_defender").get(Skill.ATTACK));
        assertEquals(40, req("rune_defender").get(Skill.DEFENCE));
    }

    @Test
    void rangedAndMagicTiers() {
        stats("leather_body", 0, 0, 0, -2, 2, 8, 9, 10, 4, 9, 0, 0, 0, 0);
        stats("black_dhide_body", 0, 0, 0, -15, 30, 30, 38, 45, 45, 50, 0, 0, 0, 0);
        stats("karils_leathertop", 0, 0, 0, -15, 30, 47, 42, 50, 65, 57, 0, 0, 0, 0);
        stats("armadyl_chestplate", -7, -7, -7, -15, 33, 56, 48, 61, 70, 57, 0, 0, 0, 1);
        stats("ahrims_robetop", 0, 0, 0, 30, -10, 52, 37, 63, 30, 0, 0, 0, 1, 0);
        stats("ancestral_robe_top", 0, 0, 0, 35, -8, 42, 31, 51, 28, 0, 0, 0, 3, 0);
        stats("trident_of_the_seas", 0, 0, 0, 15, 0, 2, 3, 1, 15, 0, 0, 0, 0, 0);
        stats("kodai_wand", 0, 0, 0, 28, 0, 0, 3, 3, 20, 0, 0, 0, 15, 0);
        stats("magic_shortbow", 0, 0, 0, 0, 69, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        assertEquals(4, speed("magic_shortbow"));
        assertEquals(6, speed("magic_longbow"));
        assertEquals(49, ItemCatalog.get("rune_arrow").rangedStrength());
        assertEquals(60, ItemCatalog.get("dragon_arrow").rangedStrength());
        assertEquals(26, ItemCatalog.get("rune_dart").rangedStrength());
        assertEquals(124, ItemCatalog.get("rune_javelin").rangedStrength());
    }

    @Test
    void bossUniques() {
        stats("abyssal_whip", 0, 82, 0, 0, 0, 0, 0, 0, 0, 0, 82, 0, 0, 0);
        stats("toxic_blowpipe", 0, 0, 0, 0, 30, 0, 0, 0, 0, 0, 0, 20, 0, 0);
        stats("twisted_bow", 0, 0, 0, 0, 70, 0, 0, 0, 0, 0, 0, 20, 0, 0);
        stats("bow_of_faerdhinen", 0, 0, 0, 0, 128, 0, 0, 0, 0, 0, 0, 106, 0, 0);
        stats("scythe_of_vitur", 70, 125, 30, -6, 0, -2, 8, 10, 0, 0, 75, 0, 0, 0);
        stats("tumekens_shadow", 0, 0, 0, 35, 0, 0, 0, 0, 20, 0, 0, 0, 0, 1);
        stats("torva_platebody", 0, 0, 0, -18, -14, 117, 111, 117, -11, 142, 6, 0, 0, 1);
        stats("bandos_chestplate", 0, 0, 0, -15, -10, 98, 93, 105, -6, 133, 4, 0, 0, 1);
        stats("elder_maul", 0, 0, 135, 0, 0, 0, 0, 0, 0, 0, 147, 0, 0, 0);
        stats("odium_ward", -12, -12, -12, -8, 12, 0, 0, 0, 24, 52, 0, 4, 0, 0);
        // Summer Sweep-Up (July 2026): the set bonus moved onto the mace.
        stats("inquisitors_mace", 52, -4, 102, 0, 0, 0, 0, 0, 0, 0, 96, 0, 0, 2);
        assertEquals(3, speed("toxic_blowpipe"));
        assertEquals(5, speed("scythe_of_vitur"));
        assertEquals(80, req("scythe_of_vitur").get(Skill.ATTACK));
        assertEquals(90, req("scythe_of_vitur").get(Skill.STRENGTH));
        assertEquals(40, req("hill_giant_club").get(Skill.ATTACK));
        assertEquals(55, req("spirit_shield").get(Skill.PRAYER));
        assertEquals(22, req("void_knight_mace").get(Skill.PRAYER));
        assertEquals(42, req("void_knight_mace").get(Skill.HITPOINTS));
    }

    @Test
    void jewelleryAndCapes() {
        worn("amulet_of_fury", 10, 10, 10, 10, 10, 15, 15, 15, 15, 15, 8, 0, 0, 5);
        worn("amulet_of_glory_4", 10, 10, 10, 10, 10, 3, 3, 3, 3, 3, 6, 0, 0, 3);
        worn("occult_necklace", 0, 0, 0, 12, 0, 0, 0, 0, 0, 0, 0, 0, 5, 2);
        worn("berserker_necklace", -10, -10, -10, 0, 0, -20, -20, -20, -20, -20, 7, 0, 0, 3);
        worn("ring_of_the_gods", 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 4);
        worn("fire_cape", 1, 1, 1, 1, 1, 11, 11, 11, 11, 11, 4, 0, 0, 2);
        worn("infernal_cape", 4, 4, 4, 1, 1, 12, 12, 12, 12, 12, 8, 0, 0, 2);
        worn("max_cape", 0, 0, 0, 0, 0, 9, 9, 9, 9, 9, 0, 0, 0, 4);
        worn("attack_cape", 0, 0, 0, 0, 0, 9, 9, 9, 9, 9, 0, 0, 0, 0);
        worn("attack_cape_t", 0, 0, 0, 0, 0, 9, 9, 9, 9, 9, 0, 0, 0, 4);
        worn("barrows_gloves", 12, 12, 12, 6, 12, 12, 12, 12, 6, 12, 12, 0, 0, 0);
        // Capes are worn in the cape slot, never as an off-hand shield.
        assertEquals(ItemDef.Kind.SIMPLE, ItemCatalog.get("fire_cape").kind());
        assertEquals(ItemDef.Kind.SIMPLE, ItemCatalog.get("infernal_cape").kind());
        assertEquals(Jewellery.Slot.CAPE, Jewellery.worn("infernal_cape").slot());
    }

    @Test
    void referenceTableIsApplied() {
        int checked = 0;
        for (Map.Entry<String, EquipmentBonuses.Entry> e : EquipmentBonuses.all().entrySet()) {
            ItemDef def = ItemCatalog.get(e.getKey());
            Jewellery.Worn w = Jewellery.worn(e.getKey());
            if (def != null && def.equipment() != null) {
                assertEquals(e.getValue().stats(), def.equipment().stats(), e.getKey());
                if (e.getValue().speed() > 0) {
                    assertEquals(e.getValue().speed(), def.equipment().attackSpeed(), e.getKey());
                }
                checked++;
            }
            if (w != null) {
                assertEquals(e.getValue().stats(), w.stats(), e.getKey());
            }
        }
        assertTrue(checked > 800, "only " + checked + " reference rows matched catalog items");
    }

    @Test
    void twoHandedWeapons() {
        for (String id : new String[]{"dragon_2h_sword", "magic_shortbow", "twisted_bow", "scythe_of_vitur", "elder_maul",
                "dharoks_greataxe", "armadyl_godsword", "toxic_blowpipe", "dragon_claws", "dragon_halberd", "hill_giant_club"}) {
            assertTrue(EquipmentBonuses.twoHanded(id), id);
        }
        for (String id : new String[]{"dragon_scimitar", "abyssal_whip", "rune_crossbow", "armadyl_crossbow", "dragon_mace",
                "trident_of_the_seas", "rune_kiteshield"}) {
            assertFalse(EquipmentBonuses.twoHanded(id), id);
        }
    }

    @Test
    void ammoRestrictions() {
        assertTrue(AmmoRules.canFire("shortbow", "iron_arrow"));
        assertFalse(AmmoRules.canFire("shortbow", "steel_arrow"));
        assertTrue(AmmoRules.canFire("willow_longbow", "mithril_arrow"));
        assertFalse(AmmoRules.canFire("willow_longbow", "adamant_arrow"));
        assertTrue(AmmoRules.canFire("yew_shortbow", "rune_arrow"));
        assertFalse(AmmoRules.canFire("yew_shortbow", "amethyst_arrow"));
        assertTrue(AmmoRules.canFire("magic_shortbow", "amethyst_arrow"));
        assertFalse(AmmoRules.canFire("magic_shortbow", "dragon_arrow"));
        assertTrue(AmmoRules.canFire("dark_bow", "dragon_arrow"));
        assertTrue(AmmoRules.canFire("twisted_bow", "dragon_arrow"));
        assertFalse(AmmoRules.canFire("magic_longbow", "rune_bolts"), "bows don't fire bolts");
        assertFalse(AmmoRules.canFire("rune_crossbow", "rune_arrow"), "crossbows don't fire arrows");
        assertTrue(AmmoRules.canFire("rune_crossbow", "onyx_bolts"));
        assertFalse(AmmoRules.canFire("adamant_crossbow", "rune_bolts"));
        assertTrue(AmmoRules.canFire("mithril_crossbow", "emerald_bolts"));
        assertFalse(AmmoRules.canFire("steel_crossbow", "ruby_bolts"));
        assertTrue(AmmoRules.canFire("armadyl_crossbow", "dragonstone_bolts"));
        assertFalse(AmmoRules.canFire("bronze_crossbow", "minecraft_arrow_or_other"));
        assertTrue(AmmoRules.canFire("hunters_sunlight_crossbow", "sunlight_antler_bolts"));
        assertFalse(AmmoRules.canFire("hunters_sunlight_crossbow", "rune_bolts"));
        assertFalse(AmmoRules.canFire("rune_crossbow", "sunlight_antler_bolts"));
        assertTrue(AmmoRules.canFire("crystal_bow_quest", null), "unrestricted weapons keep working");
        for (String id : new String[]{"shortbow", "magic_longbow", "rune_crossbow", "dragon_crossbow", "dark_bow"}) {
            assertNotNull(ItemCatalog.get(id), id);
            assertTrue(AmmoRules.restricted(id), id);
        }
    }

    @Test
    void setEffects() {
        assertEquals(1.0, SetEffects.dharokMultiplier(99, 99), 1e-9);
        assertEquals(1.0 + 0.98 * 0.99, SetEffects.dharokMultiplier(1, 99), 1e-9);
        assertEquals(SetEffects.DHAROK, SetEffects.barrows("dharoks_helm", "dharoks_platebody", "dharoks_platelegs", "dharoks_greataxe"));
        assertNull(SetEffects.barrows("dharoks_helm", "dharoks_platebody", "dharoks_platelegs", "abyssal_whip"));
        for (SetEffects.BarrowsSet s : SetEffects.BARROWS) {
            for (String id : new String[]{s.helm(), s.body(), s.legs(), s.weapon()}) {
                assertNotNull(ItemCatalog.get(id), id);
                assertEquals(70, ItemCatalog.get(id).equipment().requirements().values().stream().mapToInt(Integer::intValue).max()
                        .orElse(0), id);
            }
        }
        assertEquals(1.025, SetEffects.inquisitorCrushBonus("inquisitors_great_helm", "inquisitors_hauberk",
                "inquisitors_plateskirt"), 1e-9);
        assertEquals(1.01, SetEffects.inquisitorCrushBonus("", "inquisitors_hauberk", ""), 1e-9);
        assertEquals(1.30, SetEffects.crystalAccuracy("crystal_helm", "crystal_body", "crystal_legs"), 1e-9);
        assertEquals(1.15, SetEffects.crystalDamage("crystal_helm", "crystal_body", "crystal_legs"), 1e-9);
        assertTrue(SetEffects.crystalBow("bow_of_faerdhinen"));
        assertFalse(SetEffects.crystalBow("twisted_bow"));
    }

    @Test
    void untradeables() {
        for (String id : new String[]{"fire_cape", "infernal_cape", "dragon_defender", "void_knight_top", "fighter_torso",
                "barrows_gloves", "max_cape", "attack_cape", "varrock_armour_4", "abyssal_tentacle"}) {
            assertNotNull(ItemCatalog.get(id), id);
            assertTrue(Untradeables.untradeable(id), id);
        }
        assertFalse(Untradeables.untradeable("abyssal_whip"));
        assertFalse(Untradeables.untradeable("rune_platebody"));
    }
}
