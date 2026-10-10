package com.runecraft.osrs.content.raids;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.model.ModelSpec;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaidsContentTest {
    private final RaidsContent module = new RaidsContent();

    @Test
    void roomSequences() {
        for (int seed = 0; seed < 200; seed++) {
            Random r = new Random(seed);
            for (RaidType type : RaidType.values()) {
                List<RoomDef> rooms = RaidLayouts.layout(type, r);
                assertEquals(RoomDef.Kind.LOBBY, rooms.get(0).kind(), type + " starts with a lobby");
                assertEquals(RoomDef.Kind.REWARD, rooms.get(rooms.size() - 1).kind());
                assertEquals(RoomDef.Kind.FINAL, rooms.get(rooms.size() - 2).kind());
                assertTrue(rooms.size() <= 18, "rooms must fit in a 1024-block instance");
                Set<String> ids = new HashSet<>();
                for (RoomDef room : rooms) {
                    assertTrue(ids.add(room.id()), "room repeated: " + room.id());
                    assertEquals(type, room.raid(), room.id());
                }
            }
        }
        List<RoomDef> cox = RaidLayouts.cox(new Random(4));
        assertEquals("olm", cox.get(cox.size() - 2).id());
        assertEquals(4, cox.stream().filter(x -> x.kind() == RoomDef.Kind.COMBAT).count());
        assertEquals(2, cox.stream().filter(x -> x.kind() == RoomDef.Kind.PUZZLE).count());
        List<RoomDef> tob = RaidLayouts.tob();
        assertEquals(List.of("tob_lobby", "maiden", "bloat", "nylocas", "sotetseg", "xarpus", "verzik", "tob_reward"),
                tob.stream().map(RoomDef::id).toList());
        List<RoomDef> toa = RaidLayouts.toa(new Random(9));
        assertEquals(11, toa.size());
        assertEquals("wardens", toa.get(9).id());
        for (int i = 1; i < 9; i += 2) {
            assertEquals(RoomDef.Kind.PUZZLE, toa.get(i).kind(), "each ToA path starts with its puzzle");
        }
    }

    @Test
    void roomBossesExistAndAreRaidOnly() {
        for (RoomDef room : RaidLayouts.all().values()) {
            for (String boss : room.bosses()) {
                BossDef def = ContentRegistry.boss(boss);
                assertNotNull(def, room.id() + " boss " + boss);
                assertTrue(def.raidOnly, boss);
            }
            assertTrue(Set.of("stone", "lava", "ice", "crypt", "jungle", "void", "water", "sand").contains(room.style()), room.id());
        }
        for (BossDef b : module.bosses()) {
            assertTrue(b.raidOnly, b.id);
            assertEquals("Raids", b.category, b.id);
            assertTrue(b.minion == null || ContentRegistry.monster(b.minion) != null || ContentRegistry.boss(b.minion) != null,
                    b.id + " minion " + b.minion);
        }
    }

    @Test
    void rewardTablesSum() {
        assertEquals(69, RaidRewards.totalWeight(RaidRewards.COX_UNIQUES));
        assertEquals(19, RaidRewards.totalWeight(RaidRewards.TOB_UNIQUES));
        assertEquals(240, RaidRewards.totalWeight(RaidRewards.TOA_UNIQUES));
        assertEquals(2, RaidRewards.COX_UNIQUES.stream().filter(u -> u.item().equals("twisted_bow")).findFirst().orElseThrow().weight());
        assertEquals(1, RaidRewards.TOB_UNIQUES.stream().filter(u -> u.item().equals("scythe_of_vitur")).findFirst().orElseThrow().weight());
    }

    @Test
    void rewardItemsExist() {
        for (RaidType t : RaidType.values()) {
            for (RaidRewards.Unique u : RaidRewards.uniques(t)) {
                assertTrue(ItemCatalog.exists(u.item()), u.item());
            }
            assertTrue(ItemCatalog.exists(RaidRewards.pet(t)));
            for (String item : RaidItems.collectionLog(t)) {
                assertTrue(ItemCatalog.exists(item), item);
            }
        }
        for (RaidRewards.PointsCommon c : RaidRewards.COX_COMMONS) {
            assertTrue(ItemCatalog.exists(c.item()), c.item());
            assertTrue(c.divisor() > 0);
        }
        for (RaidRewards.PointsCommon c : RaidRewards.TOA_COMMONS) {
            assertTrue(ItemCatalog.exists(c.item()), c.item());
        }
        for (RaidRewards.RangeCommon c : RaidRewards.TOB_COMMONS) {
            assertTrue(ItemCatalog.exists(c.item()), c.item());
            assertTrue(c.min() <= c.max());
        }
        for (String item : RaidItems.RAID_ONLY_ITEMS) {
            assertTrue(ItemCatalog.exists(item), item);
        }
        assertTrue(ItemCatalog.exists("avernic_defender"));
        assertTrue(ItemCatalog.exists("kodai_wand"));
    }

    @Test
    void uniqueStats() {
        ItemDef tbow = ItemCatalog.get("twisted_bow");
        assertEquals(70, tbow.equipment().stats().rangedAttack());
        assertEquals(20, tbow.equipment().stats().rangedStrength());
        ItemDef scythe = ItemCatalog.get("scythe_of_vitur");
        assertEquals(110, scythe.equipment().stats().slashAttack());
        assertEquals(75, scythe.equipment().stats().meleeStrength());
        assertEquals(147, ItemCatalog.get("elder_maul").equipment().stats().meleeStrength());
        assertEquals(103, ItemCatalog.get("osmumtens_fang").equipment().stats().meleeStrength());
        assertEquals(15, ItemCatalog.get("kodai_wand").equipment().stats().magicDamagePercent());
    }

    @Test
    void rollsAreConsistent() {
        Map<String, Integer> points = new HashMap<>();
        points.put("a", 30_000);
        points.put("b", 10_000);
        int purples = 0;
        Random random = new Random(42);
        for (int seed = 0; seed < 2000; seed++) {
            for (RaidType t : RaidType.values()) {
                Map<String, Integer> pts = t == RaidType.TOB ? Map.of("a", 18, "b", 14) : points;
                RaidRewards.Result r = RaidRewards.roll(t, pts, 300, random);
                assertTrue(r.purpleRecipients().size() <= 1);
                for (String p : pts.keySet()) {
                    List<RaidRewards.Loot> loot = r.lootFor(p);
                    assertFalse(loot.isEmpty(), t + " gives " + p + " something");
                    for (RaidRewards.Loot l : loot) {
                        assertTrue(ItemCatalog.exists(l.item()), l.item());
                        assertTrue(l.count() > 0, l.item());
                    }
                }
                purples += r.purpleRecipients().size();
            }
        }
        assertTrue(purples > 0);
    }

    @Test
    void scalingFormulas() {
        assertEquals(570_000 / 867_600.0, RaidScaling.coxPurpleChance(10_000_000), 1e-9);
        assertEquals(0.01, RaidScaling.coxPurpleChance(8_676), 1e-9);
        assertEquals(1 / 9.1, RaidScaling.tobPurpleChance(90, 5), 1e-9);
        assertEquals(0.55, RaidScaling.toaPurpleChance(1_000_000, 500), 1e-9);
        assertEquals(20_000 / 4_500.0 / 100.0, RaidScaling.toaPurpleChance(20_000, 300), 1e-9);
        assertEquals(1.0, RaidScaling.tobHp(5));
        assertEquals(0.75, RaidScaling.tobHp(2));
        assertEquals(1.0, RaidScaling.toaHp(0, 1), 1e-9);
        assertEquals(2.2 * 2.8, RaidScaling.toaHp(300, 3), 1e-9);
        assertEquals(600, RaidScaling.coxDeathPenalty(1000));
        assertEquals(800, RaidScaling.toaDeathPenalty(1000));
    }

    @Test
    void invocationPresets() {
        int[] expected = {0, 150, 300, 400, 500};
        List<Invocation.Preset> presets = Invocation.presets();
        assertEquals(expected.length, presets.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], presets.get(i).raidLevel(), presets.get(i).name());
            assertTrue(Invocation.valid(presets.get(i).invocations()), presets.get(i).name());
        }
        assertEquals(0, Invocation.roomRetries(presets.get(1).invocations()));
        assertEquals(-1, Invocation.roomRetries(presets.get(0).invocations()));
    }

    @Test
    void blocksPlacesAndModels() {
        Set<String> blockIds = new HashSet<>();
        for (BlockDef b : module.blocks()) {
            blockIds.add(b.id());
            assertEquals(BlockDef.Kind.SCRIPTED, b.kind(), b.id());
        }
        for (RaidType t : RaidType.values()) {
            assertTrue(blockIds.contains(t.entranceBlock()), t.entranceBlock());
            assertNotNull(ContentRegistry.place(t.placeId()), t.placeId());
        }
        Map<String, ModelSpec> models = ContentRegistry.models();
        for (BossDef b : module.bosses()) {
            assertTrue(models.containsKey(b.model != null ? b.model : b.id), "model for " + b.id);
        }
        for (MonsterDef m : module.monsters()) {
            if (m.id.startsWith("deathly_")) {
                continue;
            }
            assertTrue(models.containsKey(m.model != null ? m.model : m.id), "model for " + m.id);
        }
        for (ModelSpec spec : module.models()) {
            int boxes = spec.parts().stream().mapToInt(p -> p.boxes.size()).sum();
            assertTrue(boxes <= 45, spec.id() + " has " + boxes + " boxes");
        }
    }
}
