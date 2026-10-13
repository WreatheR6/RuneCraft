package com.runecraft.osrs;

import com.runecraft.osrs.content.CommonEquipment;
import com.runecraft.osrs.content.npcs.Shops;
import com.runecraft.osrs.content.quests.QuestBook;
import com.runecraft.osrs.content.slayer.SlayerChests;
import com.runecraft.osrs.content.systems.ClueRewards;
import com.runecraft.osrs.content.systems.ClueScrolls.Tier;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.osrs.data.AmmoRules;
import com.runecraft.osrs.data.Drop;
import com.runecraft.osrs.data.EquipmentBonuses;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Mystic robes, god vestments and books, ballistae and salve imbues added to match OSRS (wiki values). */
class MissingWearablesTest {
    @Test
    void mysticRobesInEveryColour() {
        for (String colour : new String[]{"", "_light", "_dark", "_dusk"}) {
            for (String piece : new String[]{"mystic_hat", "mystic_robe_top", "mystic_robe_bottom", "mystic_gloves", "mystic_boots"}) {
                assertTrue(ItemCatalog.exists(piece + colour), piece + colour);
            }
            assertEquals(Jewellery.Slot.HANDS, Jewellery.worn("mystic_gloves" + colour).slot(), colour);
        }
        assertEquals(40, ItemCatalog.get("mystic_hat").equipment().requirements().get(Skill.MAGIC));
        assertEquals(20, ItemCatalog.get("mystic_boots_dusk").equipment().requirements().get(Skill.DEFENCE));
        assertTrue(Shops.get("magic_guild_store_mystic").stocks("mystic_gloves"));
        assertTrue(SlayerChests.BRIMSTONE_RARE.contains(Drop.of("mystic_robe_top_dusk", 1, 1, 1000)));
    }

    @Test
    void godVestmentsComeFromTheirClueTiers() {
        for (String god : new String[]{"saradomin", "zamorak", "guthix", "armadyl", "bandos", "ancient"}) {
            boolean classic = god.equals("saradomin") || god.equals("zamorak") || god.equals("guthix");
            assertTrue(ClueRewards.uniques(Tier.EASY).contains(god + "_robe_top"), god);
            assertTrue(ClueRewards.uniques(Tier.EASY).contains(god + "_robe_legs"), god);
            assertTrue(ClueRewards.uniques(Tier.MEDIUM).contains(god + "_mitre"), god);
            assertTrue(ClueRewards.uniques(Tier.MEDIUM).contains(god + "_cloak"), god);
            assertTrue(ClueRewards.uniques(classic ? Tier.HARD : Tier.MEDIUM).contains(god + "_crozier"), god);
            assertTrue(ClueRewards.uniques(classic ? Tier.HARD : Tier.MEDIUM).contains(god + "_stole"), god);
            assertEquals(Jewellery.Slot.NECK, Jewellery.worn(god + "_stole").slot());
            assertEquals(10, Jewellery.worn(god + "_stole").stats().prayer());
            assertEquals(Jewellery.Slot.CAPE, Jewellery.worn(god + "_cloak").slot());
            assertEquals(60, ItemCatalog.get(god + "_crozier").equipment().requirements().get(Skill.PRAYER));
            assertEquals(5, ItemCatalog.get(god + "_crozier").equipment().attackSpeed());
        }
    }

    @Test
    void godBooksAreCompletedWithFourPages() {
        for (String[] book : CommonEquipment.GOD_BOOKS) {
            Recipe r = Recipes.get("complete_" + book[1]);
            assertNotNull(r, book[1]);
            assertEquals(1, r.inputs().get(book[0]));
            assertEquals(5, r.inputs().size(), "the damaged book and four pages");
            assertEquals(Map.of(book[1], 1), r.outputs());
            assertEquals(5, ItemCatalog.get(book[0]).equipment().stats().prayer());
            assertEquals(5, ItemCatalog.get(book[1]).equipment().stats().prayer());
        }
        assertEquals(8, ItemCatalog.get("holy_book").equipment().stats().stabDefence());
        assertEquals(10, ItemCatalog.get("book_of_law").equipment().stats().rangedAttack());
        long jossik = QuestBook.get().shops.stream().filter(s -> s.npc().equals("jossik") && s.price() == 5000).count();
        assertEquals(5, jossik, "Jossik sells the other five damaged books");
    }

    @Test
    void ballistaeFireJavelinsOnly() {
        for (String b : new String[]{"light_ballista", "heavy_ballista"}) {
            assertTrue(AmmoRules.restricted(b));
            assertTrue(AmmoRules.canFire(b, "bronze_javelin"), b);
            assertTrue(AmmoRules.canFire(b, "dragon_javelin"), b);
            assertFalse(AmmoRules.canFire(b, "rune_arrow"), b);
            assertFalse(AmmoRules.canFire(b, "rune_bolts"), b);
            assertTrue(EquipmentBonuses.twoHanded(b), b);
            assertEquals(7, ItemCatalog.get(b).equipment().attackSpeed(), b);
        }
        assertFalse(AmmoRules.canFire("magic_shortbow", "rune_javelin"));
        assertFalse(AmmoRules.canFire("rune_crossbow", "rune_javelin"));
        assertEquals(65, ItemCatalog.get("light_ballista").equipment().requirements().get(Skill.RANGED));
        assertEquals(75, ItemCatalog.get("heavy_ballista").equipment().requirements().get(Skill.RANGED));
        assertEquals(15, ItemCatalog.get("heavy_ballista").equipment().stats().rangedStrength());
        assertEquals("monkey_madness_ii", QuestBook.get().weaponGates.get("heavy_ballista"));
        Recipe heavy = Recipes.get("fletch_heavy_ballista");
        assertEquals(72, heavy.level());
        assertEquals(6000, heavy.xpTenths());
        assertEquals(1, heavy.inputs().get("monkey_tail"));
        Recipe light = Recipes.get("fletch_incomplete_light_ballista");
        assertEquals(47, light.level());
        assertEquals(150, light.xpTenths());
        assertEquals(1, light.inputs().get("light_frame"));
        assertEquals(1, light.inputs().get("ballista_limbs"));
        assertEquals(1, Recipes.get("fletch_unstrung_heavy_ballista").inputs().get("ballista_spring"));
    }

    @Test
    void salveAmuletImbues() {
        Recipe ei = Recipes.get("make_salve_amulet_ei");
        assertNotNull(ei);
        assertEquals(1, ei.inputs().get("salve_amulet_i"));
        assertEquals("tarns_diary", ei.requiredTool());
        assertEquals(Jewellery.Slot.NECK, Jewellery.worn("salve_amulet_ei").slot());
    }
}
