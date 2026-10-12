package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Recipes;
import com.runecraft.osrs.data.AmmoRules;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Enchanted bolts: Enchant Crossbow Bolt table and the (e) bolt effects (OSRS wiki / DPS calculator). */
class BoltEffectsTest {
    private static BoltEffects.Enchant en(String prefix) {
        return BoltEffects.ENCHANTS.stream().filter(e -> e.prefix().equals(prefix)).findFirst().orElseThrow();
    }

    @Test
    void enchantSpellTable() {
        assertEquals(10, BoltEffects.ENCHANTS.size());
        assertEquals(4, en("opal").level());
        assertEquals(9.0, en("opal").xp());
        assertEquals(Map.of("cosmic_rune", 1, "air_rune", 2), en("opal").runes());
        assertEquals(87, en("onyx").level());
        assertEquals(97.0, en("onyx").xp());
        assertEquals(Map.of("cosmic_rune", 1, "fire_rune", 20, "death_rune", 1), en("onyx").runes());
        assertEquals(49, en("ruby").level());
        assertEquals(Map.of("cosmic_rune", 1, "fire_rune", 5, "blood_rune", 1), en("ruby").runes());
        assertEquals(68, en("dragonstone").level());
        Spellbooks.Spell spell = Spellbooks.get("enchant_crossbow_bolt");
        assertNotNull(spell);
        assertEquals(4, spell.level());
        assertFalse(Spellbooks.UNIMPLEMENTED.containsKey("Enchant Crossbow Bolt"));
    }

    @Test
    void chancesAndKandarinDiary() {
        assertEquals(0.05, en("opal").chance());
        assertEquals(0.06, en("ruby").chance());
        assertEquals(0.10, en("diamond").chance());
        assertEquals(0.11, en("onyx").chance());
        assertEquals(0.55, en("emerald").chance());
        assertEquals(0.121, BoltEffects.chance(en("onyx"), true), 1e-9);
        assertEquals(0.05, BoltEffects.chance(en("opal"), false), 1e-9);
    }

    @Test
    void itemsResolveToTheirEnchantment() {
        assertEquals("ruby_bolts_e", BoltEffects.enchantedOf("ruby_bolts"));
        assertEquals("ruby_dragon_bolts_e", BoltEffects.enchantedOf("ruby_dragon_bolts"));
        assertEquals("topaz_bolts_e", BoltEffects.enchantedOf("topaz_bolts"));
        assertNull(BoltEffects.enchantedOf("rune_bolts"));
        assertEquals(BoltEffects.Effect.LIFE_LEECH, BoltEffects.ofItem("onyx_dragon_bolts_e").effect());
        assertNull(BoltEffects.ofItem("onyx_bolts"));
    }

    private static BoltEffects.Context ctx(boolean accurate, int damage, int maxHit) {
        return new BoltEffects.Context(accurate, damage, maxHit, 99, 400, 90, false, false, false, false, false);
    }

    @Test
    void effectsWhenActivated() {
        Random r = new Random(1);
        // Opal: +ranged/10, even on a miss.
        assertEquals(9, BoltEffects.roll(en("opal"), 1.0, ctx(false, 0, 40), r).damage());
        // Dragonstone: +ranged*2/10 but only on an accurate hit, and never against dragons.
        assertEquals(29, BoltEffects.roll(en("dragonstone"), 1.0, ctx(true, 10, 40), r).damage());
        assertFalse(BoltEffects.roll(en("dragonstone"), 1.0, ctx(false, 0, 40), r).activated());
        BoltEffects.Context dragon = new BoltEffects.Context(true, 10, 40, 99, 400, 90, false, false, true, false, false);
        assertFalse(BoltEffects.roll(en("dragonstone"), 1.0, dragon, r).activated());
        // Ruby: 20% of current hitpoints (cap 100), costs 10% of the shooter's.
        BoltEffects.Outcome ruby = BoltEffects.roll(en("ruby"), 1.0, ctx(false, 0, 40), r);
        assertEquals(80, ruby.damage());
        assertEquals(9, ruby.selfDamage());
        BoltEffects.Context big = new BoltEffects.Context(false, 0, 40, 99, 2000, 90, false, false, false, false, false);
        assertEquals(100, BoltEffects.roll(en("ruby"), 1.0, big, r).damage());
        // Diamond: ignores accuracy, up to 115% of the max hit.
        for (int i = 0; i < 200; i++) {
            int d = BoltEffects.roll(en("diamond"), 1.0, ctx(false, 0, 40), r).damage();
            assertTrue(d >= 0 && d <= 46);
        }
        // Onyx: up to 120% max hit on an accurate hit, heals a quarter; nothing against undead.
        BoltEffects.Outcome onyx = BoltEffects.roll(en("onyx"), 1.0, ctx(true, 5, 40), r);
        assertTrue(onyx.damage() <= 48);
        assertEquals(onyx.damage() / 4, onyx.heal());
        BoltEffects.Context undead = new BoltEffects.Context(true, 5, 40, 99, 400, 90, false, false, false, true, false);
        assertFalse(BoltEffects.roll(en("onyx"), 1.0, undead, r).activated());
        // Emerald poisons for 5; sapphire/topaz only work on players.
        assertEquals(5, BoltEffects.roll(en("emerald"), 1.0, ctx(true, 5, 40), r).poison());
        assertFalse(BoltEffects.roll(en("sapphire"), 1.0, ctx(true, 5, 40), r).activated());
        // Never activates at 0% chance.
        assertFalse(BoltEffects.roll(en("opal"), 0.0, ctx(true, 5, 40), r).activated());
    }

    @Test
    void enchantedAndDragonBoltItemsExist() {
        for (BoltEffects.Enchant e : BoltEffects.ENCHANTS) {
            for (String id : new String[]{e.bolts(), e.enchanted(), e.dragonBolts(), e.dragonEnchanted()}) {
                ItemDef d = ItemCatalog.get(id);
                assertNotNull(d, id);
                assertTrue(d.rangedStrength() > 0, id);
            }
            assertEquals(ItemCatalog.get(e.bolts()).rangedStrength(), ItemCatalog.get(e.enchanted()).rangedStrength(), e.prefix());
            assertEquals(122, ItemCatalog.get(e.dragonEnchanted()).rangedStrength(), e.prefix());
            assertTrue(AmmoRules.canFire("rune_crossbow", e.enchanted()), e.prefix());
            assertFalse(AmmoRules.canFire("rune_crossbow", e.dragonEnchanted()), e.prefix());
            assertTrue(AmmoRules.canFire("armadyl_crossbow", e.dragonEnchanted()), e.prefix());
            Recipe r = Recipes.get("fletch_" + e.dragonBolts());
            assertNotNull(r, e.prefix());
            assertEquals(84, r.level());
        }
        assertEquals(48, ItemCatalog.get("pearl_bolts").rangedStrength());
        assertEquals(122, ItemCatalog.get("dragon_bolts").rangedStrength());
        assertNotNull(Recipes.get("fletch_dragon_bolts"));
        assertEquals(6, Recipes.get("cut_pearl_bolt_tips").outputs().get("pearl_bolt_tips"));
    }
}
