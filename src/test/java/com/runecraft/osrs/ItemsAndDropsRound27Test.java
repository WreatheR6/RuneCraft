package com.runecraft.osrs;

import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.minigames.TzHaarWaves;
import com.runecraft.osrs.content.systems.Charges;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.osrs.content.systems.LootingBag;
import com.runecraft.osrs.data.Boss;
import com.runecraft.osrs.data.Bone;
import com.runecraft.osrs.data.Drop;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Round 27 items and drops: new bones, slayer monster tables, semi-precious jewellery, the looting bag, Jad's tokkul. */
class ItemsAndDropsRound27Test {
    private static List<Drop> drops(String id) {
        for (MonsterDef m : ContentRegistry.monsters()) {
            if (m.id.equals(id)) {
                return m.drops;
            }
        }
        throw new AssertionError("no monster " + id);
    }

    @Test
    void newBonesHaveWikiPrayerXp() {
        assertEquals(5.3, Bone.byItem("bat_bones").xp());
        assertEquals(4.5, Bone.byItem("wolf_bones").xp());
        assertEquals(21.0, Bone.byItem("wyrmling_bones").xp());
        assertEquals(22.5, Bone.byItem("zogre_bones").xp());
        // Bone is ordinal-sensitive: the old values keep their place.
        assertEquals(0, Bone.BONES.ordinal());
        assertEquals(Bone.DRAKE_BONES.ordinal() + 1, Bone.BAT_BONES.ordinal());
        assertEquals(ItemDef.Kind.BONES, ItemCatalog.get("wolf_bones").kind());
    }

    @Test
    void slayerMonstersThatDroppedNothingNowHaveTheirWikiTables() {
        assertTrue(drops("vampyre_juvenile").contains(new Drop("vampyre_dust", 1, 1, 1)));
        assertTrue(drops("zogre").contains(new Drop("zogre_bones", 1, 1, 1)));
        assertTrue(drops("zogre").contains(new Drop("ogre_coffin_key", 1, 1, 1)));
        assertTrue(drops("skogre").contains(new Drop("zogre_bones", 1, 1, 1)));
        assertTrue(drops("wyrmling").contains(new Drop("wyrmling_bones", 1, 1, 1)));
        assertTrue(drops("cave_lizard").contains(new Drop("bones", 1, 1, 1)));
        assertTrue(drops("araxyte").contains(new Drop("araxyte_venom_sac", 1, 1, 1)));
        assertTrue(drops("earthen_nagua").contains(new Drop("blessed_bone_shards", 20, 40, 16)));
        assertTrue(drops("blue_dragon").contains(new Drop("scaly_blue_dragonhide", 1, 1, 50)));
        // OSRS crypt spiders drop nothing.
        assertTrue(drops("crypt_spider").isEmpty());
    }

    @Test
    void fightCaveJadPaysTheCompletionTokkul() {
        assertTrue(Boss.TZTOK_JAD.drops().contains(new Drop("tokkul", TzHaarWaves.FIGHT_CAVE_TOKKUL, TzHaarWaves.FIGHT_CAVE_TOKKUL, 1)));
        assertTrue(Boss.TZTOK_JAD.drops().contains(Drop.always("fire_cape")));
    }

    @Test
    void semiPreciousJewellery() {
        assertEquals("ring_of_pursuit", Jewellery.ENCHANT.get("opal_ring"));
        assertEquals("amulet_of_bounty", Jewellery.ENCHANT.get("opal_amulet"));
        assertEquals("expeditious_bracelet", Jewellery.ENCHANT.get("opal_bracelet"));
        assertEquals("amulet_of_chemistry", Jewellery.ENCHANT.get("jade_amulet"));
        assertEquals("flamtaer_bracelet", Jewellery.ENCHANT.get("jade_bracelet"));
        assertEquals("necklace_of_faith", Jewellery.ENCHANT.get("topaz_necklace"));
        assertEquals("bracelet_of_slaughter", Jewellery.ENCHANT.get("topaz_bracelet"));
        assertEquals(3, Jewellery.ENCHANT_LEVEL.get("topaz_bracelet"));
        for (String id : List.of("ring_of_pursuit", "amulet_of_bounty", "expeditious_bracelet", "amulet_of_chemistry",
                "flamtaer_bracelet", "necklace_of_faith", "bracelet_of_slaughter", "bracelet_of_ethereum")) {
            assertNotNull(Jewellery.worn(id), id);
            assertTrue(ItemCatalog.exists(id), id);
        }
        assertEquals(30, Jewellery.chargePool("expeditious_bracelet"));
        assertEquals(30, Jewellery.chargePool("bracelet_of_slaughter"));
        assertEquals(10, Jewellery.chargePool("amulet_of_bounty"));
        assertEquals(5, Jewellery.chargePool("amulet_of_chemistry"));
        assertEquals(80, Jewellery.chargePool("flamtaer_bracelet"));
        assertEquals(2, Jewellery.slayerCredit(1, "expeditious_bracelet", 0.1, false));
        assertEquals(1, Jewellery.slayerCredit(1, "expeditious_bracelet", 0.3, false));
        assertEquals(0, Jewellery.slayerCredit(1, "bracelet_of_slaughter", 0.1, false));
        assertEquals(1, Jewellery.slayerCredit(1, "bracelet_of_slaughter", 0.1, true));
        assertEquals(24, Jewellery.faithRestore(99));
        assertEquals(10, Jewellery.ethereumDamage(40));
        assertEquals(Charges.REVENANT_ETHER_MAX, Charges.of("bracelet_of_ethereum").max());
    }

    @Test
    void lootingBagRate() {
        assertEquals(1.0 / 15, LootingBag.chance(7));
        assertEquals(1.0 / 3, LootingBag.chance(124));
        assertEquals(30 / 150.0, LootingBag.chance(30));
        assertTrue(LootingBag.excluded("abyssal_demon"));
        assertFalse(LootingBag.excluded("green_dragon"));
        assertTrue(ItemCatalog.exists(LootingBag.ID));
    }
}
