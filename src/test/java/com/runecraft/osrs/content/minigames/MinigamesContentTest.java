package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.CollectionLogData;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.PlaceDef;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinigamesContentTest {
    private static int count(List<String> wave, String id) {
        return (int) wave.stream().filter(id::equals).count();
    }

    @Test
    void fightCaveHas63RealWaves() {
        List<List<String>> waves = TzHaarWaves.fightCaveWaves();
        assertEquals(63, waves.size());
        assertEquals(List.of("tz_kih"), TzHaarWaves.fightCave(1));
        assertEquals(List.of("tz_kih", "tz_kih"), TzHaarWaves.fightCave(2));
        assertEquals(List.of("tz_kek"), TzHaarWaves.fightCave(3));
        assertEquals(List.of("tz_kek", "tz_kih"), TzHaarWaves.fightCave(4));
        assertEquals(List.of("tz_kek", "tz_kek"), TzHaarWaves.fightCave(6));
        assertEquals(List.of("tok_xil"), TzHaarWaves.fightCave(7));
        assertEquals(List.of("tok_xil", "tok_xil"), TzHaarWaves.fightCave(14));
        assertEquals(List.of("yt_mejkot"), TzHaarWaves.fightCave(15));
        assertEquals(List.of("yt_mejkot", "yt_mejkot"), TzHaarWaves.fightCave(30));
        assertEquals(List.of("ket_zek"), TzHaarWaves.fightCave(31));
        assertEquals(List.of("ket_zek", "ket_zek"), TzHaarWaves.fightCave(62));
        assertEquals(List.of("tztok_jad"), TzHaarWaves.fightCave(63));
        // Wave 61: Ket-Zek with the whole of wave 30's predecessor (wave 29's predecessor chain).
        assertEquals(1, count(TzHaarWaves.fightCave(61), "ket_zek"));
        for (List<String> w : waves) {
            assertTrue(w.size() <= 6, "too many monsters " + w);
        }
    }

    @Test
    void infernoHas69Waves() {
        List<List<String>> waves = TzHaarWaves.infernoWaves();
        assertEquals(69, waves.size());
        Set<Integer> nibblerOnly = Set.of(3, 8, 17, 34);
        for (int i = 1; i <= 66; i++) {
            assertEquals(nibblerOnly.contains(i) ? 6 : 3, count(TzHaarWaves.inferno(i), "jal_nib"), "nibblers on wave " + i);
            assertEquals(nibblerOnly.contains(i), TzHaarWaves.inferno(i).stream().allMatch("jal_nib"::equals), "nibbler-only wave " + i);
        }
        assertEquals(List.of("jal_nib", "jal_nib", "jal_nib", "jal_mejrah"), TzHaarWaves.inferno(1));
        assertEquals(List.of("jal_nib", "jal_nib", "jal_nib", "jal_mejrah", "jal_mejrah"), TzHaarWaves.inferno(2));
        assertEquals(List.of("jal_nib", "jal_nib", "jal_nib", "jal_ak"), TzHaarWaves.inferno(4));
        // The wiki's double-blob waves.
        Set<Integer> doubleBlob = Set.of(7, 15, 24, 31, 41, 48, 56, 63);
        for (int i = 1; i <= 66; i++) {
            assertEquals(doubleBlob.contains(i), count(TzHaarWaves.inferno(i), "jal_ak") == 2, "double blob on wave " + i);
        }
        assertEquals(List.of("jal_nib", "jal_nib", "jal_nib", "jal_imkot", "jal_imkot"), TzHaarWaves.inferno(16));
        assertEquals(List.of("jal_nib", "jal_nib", "jal_nib", "jal_xil", "jal_xil"), TzHaarWaves.inferno(33));
        assertEquals(List.of("jal_nib", "jal_nib", "jal_nib", "jal_zek", "jal_zek"), TzHaarWaves.inferno(66));
        assertEquals(List.of("jal_nib", "jal_nib", "jal_nib", "jal_zek", "jal_xil", "jal_imkot", "jal_ak", "jal_ak"),
                TzHaarWaves.inferno(63));
        assertEquals(5, TzHaarWaves.infernoJadHealers(67));
        assertEquals(3, TzHaarWaves.infernoJadHealers(68));
        assertEquals(480, TzHaarWaves.ZUK_JAD_HP);
        assertEquals(240, TzHaarWaves.ZUK_HEALER_HP);
        assertEquals(0, waves.subList(0, 8).stream().mapToInt(w -> count(w, "jal_imkot")).sum());
        assertEquals(List.of("jal_nib", "jal_nib", "jal_nib", "jal_imkot"), TzHaarWaves.inferno(9));
        assertTrue(TzHaarWaves.inferno(18).contains("jal_xil"));
        assertFalse(TzHaarWaves.inferno(17).contains("jal_xil"));
        assertTrue(TzHaarWaves.inferno(35).contains("jal_zek"));
        assertFalse(TzHaarWaves.inferno(34).contains("jal_zek"));
        assertEquals(List.of("jaltok_jad"), TzHaarWaves.inferno(67));
        assertEquals(3, count(TzHaarWaves.inferno(68), "jaltok_jad"));
        assertEquals(List.of("tzkal_zuk"), TzHaarWaves.inferno(69));
    }

    @Test
    void waveCreaturesExist() {
        for (List<String> w : TzHaarWaves.fightCaveWaves()) {
            for (String id : w) {
                assertTrue(id.equals("tztok_jad") || ContentRegistry.monster(id) != null, id);
            }
        }
        for (List<String> w : TzHaarWaves.infernoWaves()) {
            for (String id : w) {
                assertTrue(ContentRegistry.monster(id) != null || ContentRegistry.boss(id) != null, id);
            }
        }
        for (String id : MinigameRules.PestControl.PESTS) {
            assertNotNull(ContentRegistry.monster(id), id);
        }
        for (String id : MinigameRules.PestControl.PORTALS) {
            assertNotNull(ContentRegistry.monster(id), id);
        }
        for (String id : MinigameRules.BarbarianAssault.PENANCE) {
            assertNotNull(ContentRegistry.monster(id), id);
        }
        for (Object[] a : MinigameRules.WarriorsGuild.ARMOUR) {
            assertNotNull(ContentRegistry.monster("animated_" + a[0] + "_armour"));
            for (String piece : new String[]{"_full_helm", "_platebody", "_platelegs"}) {
                assertTrue(ItemCatalog.exists(a[0] + piece), a[0] + piece);
            }
        }
        assertNotNull(ContentRegistry.boss("tzkal_zuk"));
        assertNotNull(ContentRegistry.boss("penance_queen"));
    }

    @Test
    void everyCreatureHasACustomOrBipedModel() {
        for (MonsterDef m : MinigameMonsters.monsters()) {
            if (m.model != null) {
                assertTrue(ContentRegistry.models().containsKey(m.model), m.id + " -> " + m.model);
            }
        }
        for (String boss : List.of("tzkal_zuk", "penance_queen", "wintertodt", "tempoross", "pest_portal", "jaltok_jad")) {
            assertTrue(ContentRegistry.models().containsKey(boss), boss);
        }
        for (var spec : MinigameModels.models()) {
            int boxes = spec.parts().stream().mapToInt(p -> p.boxes.size()).sum();
            assertTrue(boxes <= 40, spec.id() + " has " + boxes + " boxes");
        }
    }

    @Test
    void rewardShopsAreValid() {
        assertTrue(RewardShops.all().size() >= 12);
        for (RewardShop shop : RewardShops.all().values()) {
            assertFalse(shop.entries().isEmpty(), shop.id());
            for (RewardShop.Entry e : shop.entries()) {
                assertFalse(e.costs().isEmpty());
                for (RewardShop.Cost c : e.costs()) {
                    assertTrue(c.amount() > 0, shop.id() + " " + e.item());
                    assertTrue(c.currency().startsWith("item:") || c.currency().startsWith("pts:"), c.currency());
                    if (c.isItem()) {
                        assertTrue(ItemCatalog.exists(c.key()), "currency item " + c.key());
                    }
                    assertTrue(MinigameCurrencies.all().containsKey(c.currency()), "unnamed currency " + c.currency());
                }
                if (e.reward() == RewardShop.Reward.ITEM && !e.item().startsWith("#")) {
                    assertTrue(ItemCatalog.exists(e.item()), shop.id() + " sells unknown " + e.item());
                }
                if (e.reward() == RewardShop.Reward.XP) {
                    assertNotNull(Skill.valueOf(e.item()));
                }
                if (e.requiresItem() != null) {
                    assertTrue(ItemCatalog.exists(e.requiresItem()));
                }
            }
        }
    }

    @Test
    void realRewardCosts() {
        RewardShop pc = RewardShops.get("pest_control");
        assertEquals(250, cost(pc, "void_knight_top"));
        assertEquals(250, cost(pc, "void_knight_robe"));
        assertEquals(150, cost(pc, "void_knight_gloves"));
        assertEquals(200, cost(pc, "void_mage_helm"));
        assertEquals(200, cost(pc, "elite_void_top"));
        RewardShop ba = RewardShops.get("barbarian_assault");
        RewardShop.Entry torso = entry(ba, "fighter_torso");
        assertEquals(5, torso.costs().size()); // four roles + one Penance Queen kill
        torso.costs().stream().filter(c -> !c.currency().equals(MinigameCurrencies.BA_QUEEN)).forEach(c -> assertEquals(375, c.amount()));
        RewardShop mta = RewardShops.get("mage_training_arena");
        assertEquals(List.of(240, 240, 2400, 240), entry(mta, "master_wand").costs().stream().map(RewardShop.Cost::amount).toList());
        assertEquals(800, cost(RewardShops.get("brimhaven_agility_arena"), "pirates_hook"));
        assertEquals(75, cost(RewardShops.get("tithe_farm"), "farmers_strawhat"));
        assertEquals(400, cost(RewardShops.get("mahogany_homes"), "carpenters_helmet"));
        assertEquals(250, cost(RewardShops.get("hallowed_sepulchre"), "hallowed_ring"));
        assertEquals(1, cost(RewardShops.get("hallowed_sepulchre"), "hallowed_crystal_shard"));
        assertEquals(4000, cost(RewardShops.get("giants_foundry"), "smiths_tunic"));
        assertEquals(3500, cost(RewardShops.get("giants_foundry"), "smiths_gloves"));
        assertEquals(5000, cost(RewardShops.get("giants_foundry"), "colossal_blade"));
        assertEquals(1350, RewardShops.get("guardians_of_the_rift").entries().stream()
                .filter(e -> e.item().endsWith("_of_the_eye")).mapToInt(e -> e.costs().get(0).amount()).sum());
        // Affordability uses every currency.
        Map<String, Integer> bal = new HashMap<>();
        bal.put(MinigameCurrencies.BA_ATTACKER, 375);
        bal.put(MinigameCurrencies.BA_DEFENDER, 375);
        bal.put(MinigameCurrencies.BA_COLLECTOR, 375);
        assertFalse(torso.affordable(bal, 1));
        bal.put(MinigameCurrencies.BA_HEALER, 400);
        assertFalse(torso.affordable(bal, 1));
        bal.put(MinigameCurrencies.BA_QUEEN, 1);
        assertTrue(torso.affordable(bal, 1));
        assertFalse(torso.affordable(bal, 2));
    }

    private static RewardShop.Entry entry(RewardShop shop, String item) {
        return shop.entries().stream().filter(e -> e.item().equals(item)).findFirst().orElseThrow();
    }

    private static int cost(RewardShop shop, String item) {
        return entry(shop, item).costs().get(0).amount();
    }

    @Test
    void pestControlFormulas() {
        // 99 attack: floor(9801/600) = 16 * 35 = 560 xp per point.
        assertEquals(560, RewardShop.pestControlXpPerPoint(99, 35));
        assertEquals(5600 + 56, MinigameRules.PestControl.xpFor(99, 35, 10));
        assertEquals(61600, MinigameRules.PestControl.xpFor(99, 35, 100));
        assertEquals(0, MinigameRules.PestControl.tierFor(40));
        assertEquals(1, MinigameRules.PestControl.tierFor(85));
        assertEquals(2, MinigameRules.PestControl.tierFor(126));
        assertEquals(1.45, MinigameRules.PestControl.voidAccuracy(2));
        assertEquals(1.125, MinigameRules.PestControl.voidDamage(1, true));
    }

    @Test
    void skillingMinigameFormulas() {
        assertEquals(0, MinigameRules.Wintertodt.rolls(499));
        assertEquals(2, MinigameRules.Wintertodt.rolls(500));
        assertEquals(101, MinigameRules.Wintertodt.rolls(50000)); // no cap since the reward cart (the crate had 28)
        assertEquals(9900.0, MinigameRules.Wintertodt.completionXp(99));
        assertTrue(MinigameRules.Wintertodt.standardDamage(99, 0, 0) > MinigameRules.Wintertodt.standardDamage(99, 4, 4));
        // Warmth lost to a standard attack (since October 2024): floor((16 - W - min(2B, 6)) * 100 / FM).
        assertEquals(16, MinigameRules.Wintertodt.standardDamage(99, 0, 0));
        assertEquals(10, MinigameRules.Wintertodt.standardDamage(99, 4, 1));
        assertEquals(6, MinigameRules.Wintertodt.standardDamage(99, 4, 4));
        assertEquals(24, MinigameRules.Wintertodt.standardDamage(50, 4, 0));
        assertEquals(100, MinigameRules.Wintertodt.standardDamage(10, 4, 1));
        // Tithe Farm: a full sack of 100 pays 35 points; golovanova deposit XP 74*60 + 26*120 + 1,500 = 9,060.
        assertEquals(35, MinigameRules.TitheFarm.points(100));
        assertEquals(1, MinigameRules.TitheFarm.points(3));
        double sack = 0;
        for (int n = 1; n <= 100; n++) {
            sack += MinigameRules.TitheFarm.depositXp(0, n);
        }
        assertEquals(9060.0, sack);
        assertEquals(460.0 + 5750.0, MinigameRules.TitheFarm.depositXp(2, 75));
        assertEquals(100, MinigameRules.WarriorsGuild.CYCLOPS_ENTRY_TOKENS);
        assertEquals(2750.0, MinigameRules.MahoganyHomes.COMPLETION_XP[3]);
        assertEquals(500.0, MinigameRules.MahoganyHomes.COMPLETION_XP[0]);
        assertEquals(825.0, MinigameRules.PyramidPlunder.URN_XP[7]);
        assertEquals(60.0, MinigameRules.PyramidPlunder.URN_XP[0]);
        assertEquals(550.0, MinigameRules.PyramidPlunder.CHEST_XP[7]);
        assertEquals(275.0, MinigameRules.PyramidPlunder.SARCOPHAGUS_XP[7]);
        assertEquals(0, MinigameRules.Tempoross.permits(1999));
        assertEquals(1, MinigameRules.Tempoross.permits(2000));
        // Tempoross points (OSRS Wiki): 5 per catch, 20 per raw / 65 per cooked fish deposited, 55 per spirit pool strike.
        assertEquals(5, MinigameRules.Tempoross.POINTS_CATCH);
        assertEquals(65, MinigameRules.Tempoross.POINTS_LOAD_COOKED);
        assertEquals(20, MinigameRules.Tempoross.POINTS_LOAD_RAW);
        assertEquals(55, MinigameRules.Tempoross.POINTS_SPIRIT);
        assertEquals(0.6, MinigameRules.Guardians.raimentBonus(4));
        assertEquals(3, MinigameRules.Guardians.searches(3, 5));
        assertEquals(0, MinigameRules.Guardians.searches(0, 9));
        assertTrue(MinigameRules.WarriorsGuild.canEnter(65, 65));
        assertTrue(MinigameRules.WarriorsGuild.canEnter(99, 1));
        assertFalse(MinigameRules.WarriorsGuild.canEnter(70, 59));
        assertEquals(0, MinigameRules.WarriorsGuild.nextDefender(-1));
        assertEquals(7, MinigameRules.WarriorsGuild.nextDefender(6));
        assertEquals(40, MinigameRules.WarriorsGuild.tokens("rune"));
        assertEquals(0, MinigameRules.PyramidPlunder.maxRoom(20));
        assertEquals(8, MinigameRules.PyramidPlunder.maxRoom(99));
        assertEquals(2, MinigameRules.TitheFarm.seedFor(80));
        assertEquals(-1, MinigameRules.TitheFarm.seedFor(30));
        assertEquals(3, MinigameRules.MahoganyHomes.tierFor(70));
        assertEquals(10, MinigameRules.BarbarianAssault.WAVES);
        assertEquals(370, MinigameRules.BarbarianAssault.fullGameHonour(0));
        for (int r = 1; r <= 8; r++) {
            for (double x = 0; x < 1; x += 0.1) {
                int a = MinigameRules.PyramidPlunder.artefact(r, x);
                assertTrue(a >= 0 && a < MinigameItems.ARTEFACTS.length);
            }
        }
    }

    @Test
    void lootTablesReferenceRealItems() {
        for (List<MinigameRules.Loot> table : List.of(TemporossRewards.OTHER)) {
            for (MinigameRules.Loot l : table) {
                assertTrue(ItemCatalog.exists(l.item()), l.item());
                assertTrue(l.weight() > 0 && l.min() <= l.max(), l.item());
            }
            assertNotNull(MinigameRules.pick(table, 0.0));
            assertNotNull(MinigameRules.pick(table, 0.9999));
        }
        for (TemporossRewards.Unique u : TemporossRewards.UNIQUES) {
            assertTrue(ItemCatalog.exists(u.item()), u.item());
        }
        for (Object[] a : MinigameRules.Guardians.ALTARS) {
            assertTrue(ItemCatalog.exists((String) a[1]));
        }
        for (String id : MinigameItems.PETS) {
            assertNotNull(ItemCatalog.get(id), id);
        }
        for (String f : MinigameRules.Trawler.OUTFIT) {
            assertTrue(ItemCatalog.exists(f));
        }
        assertTrue(ItemCatalog.exists(MinigameRules.Trawler.fishFor(99)));
        for (String b : MinigameRules.GiantsFoundry.BARS) {
            assertTrue(ItemCatalog.exists(b), b);
        }
        for (String p : MinigameRules.MahoganyHomes.PLANKS) {
            assertTrue(ItemCatalog.exists(p), p);
        }
    }

    @Test
    void placesBlocksAndNpcs() {
        Set<String> places = new HashSet<>();
        for (PlaceDef p : MinigameWorld.places()) {
            assertTrue(places.add(p.id()));
            assertFalse(p.blocks().isEmpty(), p.id());
        }
        for (Minigame m : Minigame.values()) {
            assertTrue(places.contains(m.place()), m.place());
            assertTrue(ContentRegistry.blocks().stream().anyMatch(b -> b.id().equals(m.entranceBlock())), m.entranceBlock());
        }
        Set<String> blocks = new HashSet<>();
        for (BlockDef b : MinigameWorld.blocks()) {
            assertTrue(blocks.add(b.id()), "duplicate block " + b.id());
        }
        for (String shop : MinigameWorld.SHOP_BLOCKS.values()) {
            assertNotNull(RewardShops.get(shop), shop);
        }
        for (String shop : MinigameWorld.NPC_SHOPS.values()) {
            assertNotNull(RewardShops.get(shop), shop);
        }
        for (String npc : MinigameWorld.NPC_SHOPS.keySet()) {
            assertNotNull(ContentRegistry.npc(npc), npc);
        }
        for (CollectionLogData.Page page : MinigameWorld.collectionLog()) {
            for (String item : page.items()) {
                assertTrue(ItemCatalog.exists(item), page.name() + " " + item);
            }
        }
    }

    @Test
    void equipmentIsSane() {
        ItemDef dragon = ItemCatalog.get("dragon_defender");
        assertEquals(25, dragon.equipment().stats().stabAttack());
        assertEquals(6, dragon.equipment().stats().meleeStrength());
        assertEquals(60, dragon.equipment().requirements().get(Skill.DEFENCE));
        assertEquals(4, ItemCatalog.get("fighter_torso").equipment().stats().meleeStrength());
        assertEquals(8, ItemCatalog.get("infernal_cape").equipment().stats().meleeStrength());
        assertEquals(42, ItemCatalog.get("void_knight_top").equipment().requirements().get(Skill.ATTACK));
    }
}
