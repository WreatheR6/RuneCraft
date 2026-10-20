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
                    // CoX floors each have their own resource and scavenger rooms (OSRS); nothing else repeats.
                    assertTrue(ids.add(room.id()) || room.kind() == RoomDef.Kind.RESOURCE, "room repeated: " + room.id());
                    assertEquals(type, room.raid(), room.id());
                }
            }
        }
        List<RoomDef> cox = RaidLayouts.cox(new Random(4));
        assertEquals("olm", cox.get(cox.size() - 2).id());
        long combats = cox.stream().filter(x -> x.kind() == RoomDef.Kind.COMBAT).count();
        assertTrue(combats >= 3 && combats <= 5, "combat rooms " + combats);
        long puzzles = cox.stream().filter(x -> x.kind() == RoomDef.Kind.PUZZLE).count();
        assertTrue(puzzles >= 2 && puzzles <= 3, "puzzle rooms " + puzzles);
        List<RoomDef> tob = RaidLayouts.tob();
        assertEquals(List.of("tob_lobby", "maiden", "bloat", "nylocas", "sotetseg", "xarpus", "verzik", "tob_reward"),
                tob.stream().map(RoomDef::id).toList());
        List<RoomDef> toa = RaidLayouts.toa(new Random(9));
        assertEquals(12, toa.size());
        assertEquals("toa_nexus", toa.get(1).id());
        assertEquals(RoomDef.Kind.NEXUS, toa.get(1).kind());
        assertEquals("wardens", toa.get(10).id());
        for (int i = 2; i < 10; i += 2) {
            assertEquals(RoomDef.Kind.PUZZLE, toa.get(i).kind(), "each ToA path starts with its puzzle");
            assertEquals(RoomDef.Kind.COMBAT, toa.get(i + 1).kind(), "each ToA path ends with its boss");
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
        assertEquals(60, RaidRewards.totalWeight(RaidRewards.COX_UNIQUES));
        assertEquals(56, RaidRewards.totalWeight(RaidRewards.COX_CM_UNIQUES));
        assertEquals(19, RaidRewards.totalWeight(RaidRewards.TOB_UNIQUES));
        assertEquals(18, RaidRewards.totalWeight(RaidRewards.TOB_HARD_UNIQUES));
        assertEquals(99, RaidRewards.COX_COMMONS.stream().mapToInt(RaidRewards.PointsCommon::weight).sum());
        assertEquals(30, RaidRewards.TOB_COMMONS.stream().mapToInt(RaidRewards.RangeCommon::weight).sum());
        assertEquals(27, RaidRewards.TOA_COMMONS.size());
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
        for (RaidType t : RaidType.values()) {
            for (RaidMode m : RaidMode.values()) {
                for (RaidRewards.ExtraReward x : RaidRewards.extras(t, m)) {
                    assertTrue(ItemCatalog.exists(x.item()), x.item());
                }
                for (RaidRewards.Unique u : RaidRewards.uniques(t, m)) {
                    assertTrue(ItemCatalog.exists(u.item()), u.item());
                }
            }
        }
        assertTrue(ItemCatalog.exists("cabbage"));
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
        assertEquals(125, scythe.equipment().stats().slashAttack());
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
        assertEquals(570_000 / 867_500.0, RaidScaling.coxPurpleChance(10_000_000), 1e-9);
        assertEquals(0.01, RaidScaling.coxPurpleChance(8_675), 1e-9);
        assertEquals(1 / 9.1, RaidScaling.tobPurpleChance(104, 5), 1e-9);
        assertEquals(0.55, RaidScaling.toaPurpleChance(1_000_000, 500), 1e-9);
        assertEquals(20_000 / 4_500.0 / 100.0, RaidScaling.toaPurpleChance(20_000, 300), 1e-9);
        assertEquals(1.0, RaidScaling.tobHp(5));
        assertEquals(0.75, RaidScaling.tobHp(2));
        assertEquals(1.0, RaidScaling.toaHp(0, 1), 1e-9);
        assertEquals(2.2 * 2.8, RaidScaling.toaHp(300, 3), 1e-9);
        assertEquals(600, RaidScaling.coxDeathPenalty(1000));
        // OSRS: a ToA death costs 20% of the total reward points, at least 1,000.
        assertEquals(0, RaidScaling.toaDeathPenalty(1000));
        assertEquals(4000, RaidScaling.toaDeathPenalty(5000));
        assertEquals(16_000, RaidScaling.toaDeathPenalty(20_000));
    }

    @Test
    void coxScaling() {
        assertEquals(1.0, RaidScaling.coxHp(1));
        assertEquals(2.0, RaidScaling.coxHp(2));
        assertEquals(2.0, RaidScaling.coxHp(3));
        assertEquals(3.0, RaidScaling.coxHp(4));
        assertEquals(300, RaidScaling.coxScaledHp("tekton", 300, 1, false));
        assertEquals(600, RaidScaling.coxScaledHp("tekton", 300, 3, false));
        assertEquals(450, RaidScaling.coxScaledHp("tekton", 300, 1, true));
        assertEquals(120, RaidScaling.coxScaledHp("vasa_crystal", 120, 1, true), "crystals ignore Challenge Mode");
        // Great Olm: 800/600 solo; at 8 players the extra phase takes 3 players' worth (factor 7 - 3 = 4).
        assertEquals(800, RaidScaling.coxScaledHp("great_olm", 800, 1, false));
        assertEquals(800, RaidScaling.coxScaledHp("great_olm", 800, 1, true), "Olm ignores Challenge Mode");
        assertEquals(1200, RaidScaling.coxScaledHp("great_olm", 800, 2, false));
        assertEquals(2400, RaidScaling.coxScaledHp("great_olm", 800, 8, false));
        assertEquals(1800, RaidScaling.coxScaledHp("olm_left_claw", 600, 8, false));
        assertEquals(3, RaidScaling.olmPhases(1));
        assertEquals(3, RaidScaling.olmPhases(7));
        assertEquals(4, RaidScaling.olmPhases(8));
        assertEquals(5, RaidScaling.olmPhases(16));
        assertEquals(1.0, RaidScaling.coxOffence(1, false), 1e-9);
        assertEquals(1.18, RaidScaling.coxOffence(5, false), 1e-9);
        assertEquals(1.5, RaidScaling.coxOffence(1, true), 1e-9);
    }

    @Test
    void coxUniqueRolls() {
        assertEquals(List.of(100_000 / 867_500.0), RaidScaling.coxUniqueRolls(100_000));
        List<Double> big = RaidScaling.coxUniqueRolls(1_200_000);
        assertEquals(3, big.size());
        assertEquals(570_000 / 867_500.0, big.get(0), 1e-9);
        assertEquals(60_000 / 867_500.0, big.get(2), 1e-9);
        assertEquals(6, RaidScaling.coxUniqueRolls(4_000_000).size(), "at most six unique rolls");
        assertEquals(RaidScaling.COX_MAX_UNIQUES, RaidScaling.coxUniqueRolls(50_000_000).size());
        Map<String, Integer> points = new HashMap<>();
        for (int i = 0; i < 20; i++) {
            points.put("p" + i, 120_000);
        }
        Random random = new Random(7);
        int multi = 0;
        for (int i = 0; i < 500; i++) {
            RaidRewards.Result r = RaidRewards.roll(RaidType.COX, points, 0, random);
            assertTrue(r.purpleRecipients().size() <= RaidScaling.COX_MAX_UNIQUES);
            assertEquals(r.purpleRecipients().size(), new HashSet<>(r.purpleRecipients()).size(), "one unique per player");
            if (r.purpleRecipients().size() > 1) {
                multi++;
            }
        }
        assertTrue(multi > 0, "2.4m points gives several unique rolls");
    }

    @Test
    void tobModes() {
        assertEquals(3062, RaidScaling.tobScaledHp("the_maiden_of_sugadinti", 3500, 4, RaidMode.NORMAL));
        assertEquals(2625, RaidScaling.tobScaledHp("the_maiden_of_sugadinti", 3500, 1, RaidMode.NORMAL));
        assertEquals(500, RaidScaling.tobScaledHp("the_maiden_of_sugadinti", 3500, 1, RaidMode.ENTRY));
        assertEquals(2000, RaidScaling.tobScaledHp("the_maiden_of_sugadinti", 3500, 5, RaidMode.ENTRY));
        assertEquals(0, RaidScaling.tobPurpleChance(104, 5, RaidMode.ENTRY));
        assertEquals(1 / 7.7, RaidScaling.tobPurpleChance(104, 5, RaidMode.HARD), 1e-9);
        assertEquals(0.5 / 9.1, RaidScaling.tobPurpleChance(52, 5, RaidMode.NORMAL), 1e-9);
        Random random = new Random(3);
        for (int i = 0; i < 300; i++) {
            RaidRewards.Result r = RaidRewards.roll(RaidType.TOB, RaidMode.ENTRY, Map.of("a", 18, "b", 18), 0, random);
            assertTrue(r.purpleRecipients().isEmpty() && r.petRecipients().isEmpty(), "entry mode has no uniques or pet");
        }
        assertTrue(RaidMode.CHALLENGE.availableFor(RaidType.COX));
        assertFalse(RaidMode.CHALLENGE.availableFor(RaidType.TOB));
        assertTrue(RaidMode.HARD.availableFor(RaidType.TOB));
        assertEquals(RaidMode.CHALLENGE, RaidMode.byName("cm"));
    }

    @Test
    void toaScaling() {
        assertEquals(400, RaidScaling.toaScaledHp(400, 0, 0, 1, true));
        assertEquals(880, RaidScaling.toaScaledHp(400, 300, 0, 1, true));
        // Zebak at raid level 150, path level 1, duo: 580 -> 928 -> 1002 -> 1903 -> rounded 1900.
        assertEquals(1900, RaidScaling.toaScaledHp(580, 150, 1, 2, true));
        assertEquals(1760, RaidScaling.toaScaledHp(580, 150, 1, 2, false), "path level only affects path monsters");
        assertEquals(1.08, RaidScaling.toaPathMultiplier(1), 1e-9);
        assertEquals(1.33, RaidScaling.toaPathMultiplier(6), 1e-9);
        assertEquals(0, RaidScaling.toaPathLevel(Set.of(), 3));
        assertEquals(1, RaidScaling.toaPathLevel(Set.of(Invocation.PATHSEEKER), 3));
        assertEquals(5, RaidScaling.toaPathLevel(Set.of(Invocation.PATHMASTER, Invocation.WALK_THE_PATH), 2));
        assertEquals(6, RaidScaling.toaPathLevel(Set.of(Invocation.PATHMASTER, Invocation.WALK_THE_PATH), 3));
        RaidRewards.Unique shadow = RaidRewards.TOA_UNIQUES.stream().filter(u -> u.item().equals("tumekens_shadow")).findFirst().orElseThrow();
        RaidRewards.Unique fang = RaidRewards.TOA_UNIQUES.get(0);
        assertEquals(70, RaidRewards.toaWeight(fang, 0), "fang is 50x rarer below raid level 50");
        assertEquals(3500, RaidRewards.toaWeight(fang, 50));
        assertEquals(10, RaidRewards.toaWeight(shadow, 0), "shadow is 50x rarer below raid level 150");
        assertEquals(10, RaidRewards.toaWeight(shadow, 100));
        assertEquals(500, RaidRewards.toaWeight(shadow, 150));
        assertEquals(500, RaidRewards.toaWeight(shadow, 300));
        double purple = RaidScaling.toaPurpleChance(20_000, 300);
        assertTrue(RaidScaling.toaPetChance(20_000, 300) < purple / 30);
    }

    @Test
    void bossStatsMatchOsrs() {
        assertEquals(3500, ContentRegistry.boss("verzik_vitur_p2").hitpoints);
        assertEquals(3500, ContentRegistry.boss("verzik_vitur_p3").hitpoints);
        assertEquals(5000, ContentRegistry.boss("xarpus").hitpoints);
        assertEquals(800, ContentRegistry.boss("great_olm").hitpoints);
        assertEquals(250, ContentRegistry.boss("muttadile").hitpoints);
        assertEquals(100, ContentRegistry.monster("vespine_soldier").hitpoints);
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
        // Normal, the starting preset, keeps unlimited attempts, food and no time limit (round 33).
        assertEquals(-1, Invocation.roomRetries(presets.get(1).invocations()));
        assertTrue(Invocation.foodAllowed(presets.get(1).invocations()));
        assertEquals(0, Invocation.timeLimitMinutes(presets.get(1).invocations()));
        assertFalse(presets.get(1).invocations().contains(Invocation.MIND_THE_GAP));
        assertEquals(-1, Invocation.roomRetries(presets.get(0).invocations()));
        assertEquals(9, Invocation.roomRetries(presets.get(3).invocations()));
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

    @Test
    void coxPointsMath() {
        assertEquals(50, RaidScaling.coxDamagePoints(5 * 300, 300, 10), "a fresh pool gives 5 points per damage");
        assertEquals(20, RaidScaling.coxDamagePoints(300, 300, 10), "a drained pool gives pool * dmg / (hp / 2)");
        assertEquals(1, RaidScaling.coxDamagePoints(10, 300, 1), "a damaging hit is always worth a point");
        assertEquals(0, RaidScaling.coxDamagePoints(0, 300, 10));
        assertEquals(0, RaidScaling.coxDamagePoints(1500, 300, 0));
        int hp = 1000;
        int pool = hp * RaidScaling.COX_DAMAGE_POOL_PER_HP;
        int total = 0;
        for (int dealt = 0; dealt < hp; dealt += 10) {
            int pr = RaidScaling.coxDamagePoints(pool, hp, 10);
            pool -= pr;
            total += pr;
        }
        assertTrue(total > 3_900 && total < 4_500, "about 4.1 points per hitpoint over a kill: " + total);
        assertEquals(3, RaidScaling.coxMysticCount(1));
        assertEquals(4, RaidScaling.coxMysticCount(3));
        assertEquals(12, RaidScaling.coxMysticCount(27));
        assertEquals(12, RaidScaling.coxMysticCount(60));
        assertEquals(2, RaidScaling.coxShamanCount(1));
        assertEquals(3, RaidScaling.coxShamanCount(5));
        assertEquals(5, RaidScaling.coxShamanCount(15));
        assertEquals(5, RaidScaling.coxShamanCount(100));
        assertEquals(6_000, RaidScaling.coxIceDemonPointsCap(450));
        assertEquals(9_000, RaidScaling.coxIceDemonPointsCap(900));
        assertEquals(4_500, RaidScaling.coxThievingPointsCap(99));
        assertEquals(11_100, RaidScaling.coxThievingPointsCap(447));
        assertEquals(30, RaidScaling.olmClenchThreshold(600));
    }

    @Test
    void coxCommonLoot() {
        RaidRewards.PointsCommon death = RaidRewards.COX_COMMONS.get(0);
        assertEquals("death_rune", death.item());
        assertEquals(3_640, RaidRewards.coxCommonQuantity(death, 500_000), "capped at 131,071 points");
        assertEquals(833, RaidRewards.coxCommonQuantity(death, 30_000));
        RaidRewards.PointsCommon relic = RaidRewards.COX_COMMONS.get(RaidRewards.COX_COMMONS.size() - 1);
        assertEquals("dark_relic", relic.item());
        assertEquals(1, RaidRewards.coxCommonQuantity(relic, 100_000));
        RaidRewards.PointsCommon essence = RaidRewards.COX_COMMONS.stream().filter(c -> c.item().equals("pure_essence")).findFirst()
                .orElseThrow();
        assertEquals(65_535, RaidRewards.coxCommonQuantity(essence, 131_071));
        Random random = new Random(11);
        for (int i = 0; i < 200; i++) {
            List<RaidRewards.PointsCommon> two = RaidRewards.pickWeighted(RaidRewards.COX_COMMONS, 2, random);
            assertEquals(2, two.size());
            assertFalse(two.get(0).equals(two.get(1)), "two different resources");
        }
    }

    @Test
    void tobPointsAndLoot() {
        assertEquals(104, RaidScaling.tobMaxPoints(5));
        assertEquals(32, RaidScaling.tobMaxPoints(1));
        assertEquals(4, RaidScaling.tobMvpPoints("verzik"));
        assertEquals(2, RaidScaling.tobMvpPoints("maiden"));
        assertEquals(0.5, RaidScaling.tobPointsRatio(52, 5), 1e-9);
        assertEquals(List.of(500, 600), toList(RaidScaling.tobCommonRange(500, 1.0, RaidMode.NORMAL, false)));
        assertEquals(List.of(575, 690), toList(RaidScaling.tobCommonRange(500, 1.0, RaidMode.HARD, false)));
        assertEquals(List.of(650, 780), toList(RaidScaling.tobCommonRange(500, 1.0, RaidMode.HARD, true)));
        assertEquals(List.of(250, 300), toList(RaidScaling.tobCommonRange(500, 0.5, RaidMode.NORMAL, false)));
        Random random = new Random(5);
        for (int i = 0; i < 200; i++) {
            RaidRewards.Result r = RaidRewards.roll(RaidType.TOB, RaidMode.NORMAL, Map.of("a", 32, "b", 0), 0, random);
            if (!r.purple("b")) {
                assertEquals(List.of(new RaidRewards.Loot("cabbage", 1)), r.lootFor("b"), "no points, only a cabbage");
            }
            for (RaidRewards.Loot l : r.lootFor("a")) {
                RaidRewards.RangeCommon c = RaidRewards.TOB_COMMONS.stream().filter(x -> x.item().equals(l.item())).findFirst()
                        .orElse(null);
                if (c != null) {
                    // Three rolls with replacement: a common rolled twice or three times stacks.
                    int[] range = RaidScaling.tobCommonRange(c.min(), 32 / 50.0, RaidMode.NORMAL, false);
                    assertTrue(l.count() >= Math.max(1, range[0]) && l.count() <= 3 * Math.max(1, range[1]), l.item() + " " + l.count());
                }
            }
        }
    }

    @Test
    void tobCommonsRepeatAndTertiariesArePerPlayer() {
        // OSRS wiki: three rolls of the common table, "possibly granting the same reward multiple times".
        Random random = new Random(11);
        int repeats = 0;
        int clues = 0;
        int zik = 0;
        int n = 40_000;
        Map<String, Integer> five = Map.of("a", 18, "b", 18, "c", 18, "d", 18, "e", 18);
        for (int i = 0; i < n; i++) {
            RaidRewards.Result r = RaidRewards.roll(RaidType.TOB, RaidMode.NORMAL, five, 0, random);
            for (String p : five.keySet()) {
                long commons = r.lootFor(p).stream().filter(l -> RaidRewards.TOB_COMMONS.stream().anyMatch(c -> c.item().equals(l.item())))
                        .count();
                if (!r.purple(p) && commons < 3) {
                    repeats++;
                }
                clues += (int) r.lootFor(p).stream().filter(l -> l.item().equals("clue_scroll_elite")).count();
                zik += (int) r.lootFor(p).stream().filter(l -> l.item().equals("lil_zik")).count();
            }
        }
        assertTrue(repeats > 0, "the same common can come up twice");
        // 3/25 per deathless player's chest in Normal mode: 24,000 expected over 200,000 chests.
        assertTrue(clues > 22_000 && clues < 26_000, "elite clues " + clues);
        // Lil' Zik 1/650 per player: ~308 over 200,000 chests.
        assertTrue(zik > 220 && zik < 400, "Lil' Zik " + zik);
        assertEquals(1 / 25.0, RaidRewards.tobEliteClueChance(RaidMode.ENTRY), 1e-9);
        assertEquals(3.5 / 25.0, RaidRewards.tobEliteClueChance(RaidMode.HARD), 1e-9);
        // Deaths scale a player's tertiaries down; a player with no points gets only the cabbage.
        // OSRS wiki "Lil' zik": full rate at 10 individual points (two deaths), down to a tenth (1/6,500).
        assertEquals(1.0, RaidScaling.tobPerformance(32), 1e-9);
        assertEquals(1.0, RaidScaling.tobPerformance(14), 1e-9);
        assertEquals(1.0, RaidScaling.tobPerformance(10), 1e-9);
        assertEquals(0.5, RaidScaling.tobPerformance(5), 1e-9);
        assertEquals(0.1, RaidScaling.tobPerformance(1), 1e-9);
        assertEquals(0.0, RaidScaling.tobPerformance(0), 1e-9);
    }

    @Test
    void modeExtraRewards() {
        Random random = new Random(21);
        Map<String, Integer> cox = Map.of("a", 50_000);
        int dust = 0;
        int kit = 0;
        int kitLate = 0;
        for (int i = 0; i < 20_000; i++) {
            for (RaidRewards.Loot l : RaidRewards.roll(RaidType.COX, RaidMode.CHALLENGE, cox, 0, true, random).lootFor("a")) {
                dust += l.item().equals("metamorphic_dust") ? 1 : 0;
                kit += l.item().equals("twisted_ancestral_colour_kit") ? 1 : 0;
            }
            for (RaidRewards.Loot l : RaidRewards.roll(RaidType.COX, RaidMode.CHALLENGE, cox, 0, false, random).lootFor("a")) {
                kitLate += l.item().equals("twisted_ancestral_colour_kit") ? 1 : 0;
            }
            for (RaidRewards.Loot l : RaidRewards.roll(RaidType.COX, RaidMode.NORMAL, cox, 0, true, random).lootFor("a")) {
                assertFalse(l.item().equals("metamorphic_dust") || l.item().equals("twisted_ancestral_colour_kit"));
            }
        }
        assertTrue(dust > 20 && dust < 90, "metamorphic dust about 1/400: " + dust);
        assertTrue(kit > 170 && kit < 380, "colour kit about 1/75: " + kit);
        assertEquals(0, kitLate, "the colour kit needs the target time");
        int holy = 0;
        int hardLate = 0;
        Map<String, Integer> tob = Map.of("a", 32);
        for (int i = 0; i < 20_000; i++) {
            for (RaidRewards.Loot l : RaidRewards.roll(RaidType.TOB, RaidMode.HARD, tob, 0, true, random).lootFor("a")) {
                holy += l.item().equals("holy_ornament_kit") ? 1 : 0;
            }
            for (RaidRewards.Loot l : RaidRewards.roll(RaidType.TOB, RaidMode.HARD, tob, 0, false, random).lootFor("a")) {
                hardLate += RaidRewards.TOB_HARD_EXTRAS.stream().anyMatch(x -> x.item().equals(l.item())) ? 1 : 0;
            }
        }
        assertTrue(holy > 120 && holy < 290, "holy ornament kit about 1/100: " + holy);
        assertEquals(0, hardLate, "hard mode cosmetics need the challenge time");
    }

    @Test
    void toaPointsAndLoot() {
        assertEquals(0.75, RaidScaling.toaCommonScale(100), 1e-9);
        assertEquals(1.0, RaidScaling.toaCommonScale(200), 1e-9);
        assertEquals(1.15, RaidScaling.toaCommonScale(300), 1e-9);
        assertEquals(1.55, RaidScaling.toaCommonScale(500), 1e-9);
        assertEquals(1150, RaidScaling.toaCommonQuantity(20_000, 20, 300));
        assertEquals(2, RaidScaling.toaCommonQuantity(20_000, 7_500, 300));
        assertEquals(1, RaidScaling.toaCommonQuantity(20_000, 999_999, 300), "one cache of runes");
        assertEquals(48_000, RaidScaling.toaCommonQuantity(80_000, 1, 100), "points capped at 64,000");
        assertEquals(361, RaidScaling.toaPurpleLevel(500));
        assertEquals(378, RaidScaling.toaPurpleLevel(600));
        assertEquals(300, RaidScaling.toaPurpleLevel(300));
        assertEquals(10_000 / 328_000.0, RaidScaling.toaPurpleChance(10_000, 500), 1e-12);
        assertEquals(10_000 / 1_050_000.0, RaidScaling.toaPurpleChance(10_000, 0), 1e-12);
        assertEquals(433, RaidScaling.toaPetLevel(500));
        assertEquals(450, RaidScaling.toaPetLevel(600));
        assertEquals(20_000 / (100.0 * (350_000 - 700 * 300)), RaidScaling.toaPetChance(20_000, 300), 1e-12);
        assertEquals(0.1, RaidScaling.toaEliteClueChance(20_000), 1e-12);
        assertEquals(2.0, RaidScaling.toaPointsPerDamage("baba", false));
        assertEquals(2.5, RaidScaling.toaPointsPerDamage("tumekens_warden", true));
        assertEquals(0.5, RaidScaling.toaPointsPerDamage("scarab_swarm", false));
        assertEquals(0.5, RaidScaling.toaPointsPerDamage("toa_soldier_scarab", false));
        assertEquals(1.0, RaidScaling.toaPointsPerDamage("toa_scarab_swarm", false));
        assertEquals(1.2, RaidScaling.toaPointsPerDamage("toa_baboon_brawler", false));
        assertEquals(1.0, RaidScaling.toaPointsPerDamage("akkha", false));
        RaidRewards.Unique fang = RaidRewards.TOA_UNIQUES.get(0);
        RaidRewards.Unique lightbearer = RaidRewards.TOA_UNIQUES.get(1);
        assertEquals(30 * 50, RaidRewards.toaWeight(fang, 550));
        assertEquals(35 * 50, RaidRewards.toaWeight(lightbearer, 500));
        assertEquals(45 * 50, RaidRewards.toaWeight(lightbearer, 425));
        assertEquals(66 * 50, RaidRewards.toaWeight(fang, 320));
        assertEquals(70 * 50, RaidRewards.toaWeight(fang, 300));
        // OSRS wiki: phase 2 Warden 4,500 hitpoints ("core-ejected"), body 140 ("active"), both scaled by raid level; the
        // core-ejected pool only by +0.1% per raid level (DPS calculator: 5,180 at 150, 5,850 at 300, 6,080 at 350).
        assertEquals(140, RaidScaling.toaCoreChargeNeeded(0, 1));
        assertEquals(310, RaidScaling.toaCoreChargeNeeded(300, 1));
        assertEquals(4500, RaidScaling.toaWardenP2Hp(0, 1));
        assertEquals(5180, RaidScaling.toaWardenP2Hp(150, 1));
        assertEquals(5850, RaidScaling.toaWardenP2Hp(300, 1));
        assertEquals(6080, RaidScaling.toaWardenP2Hp(350, 1));
        assertEquals(16380, RaidScaling.toaWardenP2Hp(300, 3));
        assertEquals(21, RaidScaling.toaCoreExposureTicks(1.0));
        assertEquals(21, RaidScaling.toaCoreExposureTicks(0.8));
        assertEquals(29, RaidScaling.toaCoreExposureTicks(0.79));
        assertEquals(37, RaidScaling.toaCoreExposureTicks(0.59));
        Random random = new Random(8);
        Map<String, Integer> pts = Map.of("a", 30_000, "b", 30_000);
        for (int i = 0; i < 500; i++) {
            RaidRewards.Result r = RaidRewards.roll(RaidType.TOA, RaidMode.NORMAL, pts, 400, false, random);
            assertTrue(r.purpleRecipients().size() <= 1, "only one ToA unique per raid");
            for (String p : pts.keySet()) {
                if (!r.purple(p)) {
                    long commons = r.lootFor(p).stream().filter(l -> RaidRewards.TOA_COMMONS.stream()
                            .anyMatch(c -> c.item().equals(l.item()))).count();
                    assertEquals(3, commons, "three common rolls");
                }
            }
        }
    }

    private static List<Integer> toList(int[] a) {
        return List.of(a[0], a[1]);
    }
}
