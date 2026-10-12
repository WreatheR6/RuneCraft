package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.FarmingDisease;
import com.runecraft.osrs.FishingSpotTimers;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.data.AgilityCourse;
import com.runecraft.osrs.data.Allotment;
import com.runecraft.osrs.data.Bar;
import com.runecraft.osrs.data.Fish;
import com.runecraft.osrs.data.SpecialTool;
import com.runecraft.osrs.data.ToolTier;
import com.runecraft.osrs.data.Tree;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure-Java checks of the moving fishing spots, crop disease, agility, ashes, hunter and tool mechanics. */
class SkillingMechanicsTest {

    private static IntUnaryOperator fixed(int value) {
        return n -> Math.min(value, n - 1);
    }

    // ------------------------------------------------------------------ fishing spots

    @Test
    void fishingSpotTimerStartsThenFires() {
        FishingSpotTimers t = new FishingSpotTimers();
        assertFalse(t.due(1L, 0, 62, 0, 1000, fixed(0)), "first sighting only starts the timer");
        assertEquals(1000 + FishingSpotTimers.MIN_STAY, t.moveAt(1L));
        assertFalse(t.due(1L, 0, 62, 0, 1000 + FishingSpotTimers.MIN_STAY - 1, fixed(0)));
        assertTrue(t.due(1L, 0, 62, 0, 1000 + FishingSpotTimers.MIN_STAY, fixed(0)));
        int stay = FishingSpotTimers.rollStay(n -> n - 1);
        assertEquals(FishingSpotTimers.MAX_STAY, stay);
    }

    @Test
    void fishingSpotMovesWithinItsAreaToValidTiles() {
        FishingSpotTimers t = new FishingSpotTimers();
        // Water tiles form a shoreline along x = 0..20 at z = 5, height 62.
        FishingSpotTimers.TileCheck shore = (x, y, z) -> y == 62 && z == 5 && x >= 0 && x <= 20;
        t.due(10L, 10, 62, 5, 0, fixed(0));
        int[] dest = t.destination(10L, 10, 62, 5, shore, fixed(0));
        assertNotNull(dest);
        assertTrue(shore.valid(dest[0], dest[1], dest[2]));
        assertTrue(Math.abs(dest[0] - 10) >= 2 && Math.abs(dest[0] - 10) <= FishingSpotTimers.AREA_RADIUS);
        // After moving, the home stays the original tile: candidates never drift further than the radius.
        t.moved(10L, 99L, dest[0], dest[1], dest[2], 0, fixed(0));
        for (int i = 0; i < 20; i++) {
            int[] next = t.destination(99L, dest[0], dest[1], dest[2], shore, fixed(i));
            assertTrue(Math.abs(next[0] - 10) <= FishingSpotTimers.AREA_RADIUS, "spot drifted to " + next[0]);
        }
        assertNull(t.destination(5L, 0, 62, 0, (x, y, z) -> false, fixed(0)), "no water: the spot stays");
    }

    // ------------------------------------------------------------------ farming

    @Test
    void compostReducesDiseaseLikeOsrs() {
        assertEquals(27, FarmingDisease.chance(27, 0));
        assertEquals(14, FarmingDisease.chance(27, 1)); // wiki table: 27 -> 14 -> 6 -> 3
        assertEquals(6, FarmingDisease.chance(27, 2));
        assertEquals(3, FarmingDisease.chance(27, 3));
        assertEquals(0, FarmingDisease.chance(0, 0));
        assertEquals(3, FarmingDisease.compostLives(3));
        assertFalse(FarmingDisease.rollDisease(27, true, fixed(0)), "protected crops never get diseased");
        assertTrue(FarmingDisease.rollDisease(27, false, fixed(26)));
        assertFalse(FarmingDisease.rollDisease(27, false, fixed(27)));
    }

    @Test
    void diseasedCropDiesUnlessCured() {
        long cycle = 100;
        int stages = 4;
        // Always diseased on the first roll (end of cycle 1).
        FarmingDisease.Crop c = new FarmingDisease.Crop(0);
        c.update(150, stages, cycle, 27, false, fixed(0));
        assertTrue(c.diseased());
        assertEquals(0, c.stage(150, stages, cycle), "a diseased crop stops growing");
        c.update(199, stages, cycle, 27, false, fixed(0));
        assertFalse(c.dead);
        c.update(200, stages, cycle, 27, false, fixed(0));
        assertTrue(c.dead, "uncured for a whole cycle: dead");

        FarmingDisease.Crop cured = new FarmingDisease.Crop(0);
        cured.update(150, stages, cycle, 27, false, fixed(0));
        cured.cure(150, cycle);
        assertFalse(cured.diseased());
        // Healthy from now on: grows through the remaining cycles.
        cured.update(10_000, stages, cycle, 27, false, fixed(127));
        assertFalse(cured.dead);
        assertTrue(cured.grown(10_000, stages, cycle));
    }

    @Test
    void lastCycleNeverDiseasesAndProtectionWorks() {
        FarmingDisease.Crop c = new FarmingDisease.Crop(0);
        int[] rolls = {0};
        c.update(1_000, 4, 100, 128, true, n -> {
            rolls[0]++;
            return 0;
        });
        assertTrue(c.grown(1_000, 4, 100));
        FarmingDisease.Crop unprotected = new FarmingDisease.Crop(0);
        unprotected.update(400, 4, 100, 27, false, n -> {
            rolls[0]++;
            return 127;
        });
        assertEquals(3, rolls[0], "4 growth cycles give 3 disease rolls");
        assertTrue(unprotected.grown(400, 4, 100));
    }

    @Test
    void cropStagesAndDiseaseData() {
        assertEquals(4, FarmingData.stages(FarmingData.byId("oak")));
        assertEquals(12, FarmingData.stages(FarmingData.byId("magic")));
        assertEquals(6, FarmingData.stages(FarmingData.byId("apple")));
        assertEquals(5, FarmingData.stages(FarmingData.byId("redberry")));
        assertEquals(4, FarmingData.stages(FarmingData.byId("barley")));
        assertEquals(7, FarmingData.stages(FarmingData.byId("potato_cactus")));
        assertEquals(0, FarmingData.diseaseChance(FarmingData.byId("poison_ivy"), 0), "poison ivy is disease-free");
        assertEquals(0, FarmingData.diseaseChance(FarmingData.byId("grape"), 0), "grapevines are disease-free");
        assertEquals(2, FarmingData.diseaseChance(FarmingData.byId("yew"), 3));
        for (FarmingData.Crop c : FarmingData.CROPS) {
            assertTrue(c.growthMinutes() % FarmingData.cycleMinutes(c) == 0, c.id() + " growth isn't whole cycles");
        }
    }

    @Test
    void protectionPaymentsExist() {
        assertEquals(new FarmingDisease.Payment("compost", 2), FarmingDisease.payment("potato"));
        assertEquals(new FarmingDisease.Payment("coconut", 25), FarmingDisease.payment("magic"));
        assertEquals(new FarmingDisease.Payment("limpwurt_root", 15), FarmingDisease.payment("teak"));
        assertNull(FarmingDisease.payment("poison_ivy"));
        for (Allotment a : Allotment.values()) {
            assertNotNull(FarmingDisease.payment(a.produceItem()), a.name());
        }
        for (Map.Entry<String, java.util.List<FarmingDisease.Payment>> e : FarmingDisease.payments().entrySet()) {
            for (FarmingDisease.Payment pay : e.getValue()) {
                assertNotNull(ItemCatalog.get(pay.item()), e.getKey() + " payment " + pay.item());
            }
            assertTrue(FarmingData.byId(e.getKey()) != null || isAllotment(e.getKey()), "unknown crop " + e.getKey());
        }
        assertNotNull(ItemCatalog.get("plant_cure"));
        for (FarmingData.Gardener g : FarmingData.GARDENERS) {
            assertNotNull(g.place());
        }
    }

    private static boolean isAllotment(String id) {
        for (Allotment a : Allotment.values()) {
            if (a.produceItem().equals(id)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ agility

    @Test
    void agilityObstaclesMatchRealCourses() {
        assertEquals(7, AgilityCourse.GNOME_STRONGHOLD.obstacles());
        assertEquals(9, AgilityCourse.VARROCK.obstacles());
        assertEquals(13, AgilityCourse.FALADOR.obstacles());
        assertEquals(9, AgilityCourse.POLLNIVNEACH.obstacles());
        assertEquals(6, AgilityCourse.SEERS.obstacles());
        assertEquals(7.5, AgilityCourse.GNOME_STRONGHOLD.obstacleXp(0));
        assertEquals(46.5, AgilityCourse.GNOME_STRONGHOLD.lapBonusXp());
        assertEquals(45.0, AgilityCourse.SEERS.obstacleXp(0));
        assertEquals(435.0, AgilityCourse.SEERS.lapBonusXp());
        assertEquals(540.0, AgilityCourse.POLLNIVNEACH.lapBonusXp());
        assertEquals(475.0, AgilityCourse.RELLEKKA.lapBonusXp());
        for (AgilityCourse c : AgilityCourse.values()) {
            double sum = 0;
            for (int i = 0; i < c.obstacles(); i++) {
                assertTrue(c.obstacleXp(i) > 0, c + " obstacle " + i);
                sum += c.obstacleXp(i);
            }
            assertEquals(c.lapXp(), sum, 1e-6, c.name());
            assertTrue(c.obstacles() <= AgilityCourse.MAX_OBSTACLES);
            // The pet system recognises a finished lap by its XP drop: no other obstacle may give the same XP.
            for (AgilityCourse o : AgilityCourse.values()) {
                for (int i = 0; i < o.obstacles() - 1; i++) {
                    assertTrue(Math.abs(o.obstacleXp(i) - c.lapBonusXp()) > 1e-9, c + " lap XP equals " + o + " obstacle " + i);
                }
            }
        }
        assertEquals(66.0, AgilityCourse.AL_KHARID.lapBonusXp());
        assertEquals(625.0, AgilityCourse.ARDOUGNE.lapBonusXp());
    }

    @Test
    void marksOfGraceRates() {
        assertEquals(0, AgilityCourse.GNOME_STRONGHOLD.markChance(50));
        assertEquals(1.0 / 3, AgilityCourse.DRAYNOR.markChance(10), 1e-9);
        assertEquals(1.0 / 3 * 0.2, AgilityCourse.DRAYNOR.markChance(30), 1e-9);
        assertEquals(2.0 / 3, AgilityCourse.CANIFIS.markChance(99), 1e-9, "Canifis is never penalised");
        assertEquals(1.0 / 3, AgilityCourse.ARDOUGNE.markChance(99), 1e-9);
        assertEquals(300, AgilityCourse.MARK_COOLDOWN_TICKS);
        int total = 0;
        for (AgilityData.Piece p : AgilityData.GRACEFUL) {
            assertNotNull(ItemCatalog.get(p.id()), p.id());
            total += p.marks();
        }
        assertEquals(260, total);
        assertTrue(AgilityData.fullGraceful("graceful_hood", "graceful_top", "graceful_legs", "graceful_boots", "agility_cape",
                "graceful_gloves"));
        assertFalse(AgilityData.fullGraceful("graceful_hood", "graceful_top", "graceful_legs", "graceful_boots", "",
                "graceful_gloves"));
    }

    // ------------------------------------------------------------------ prayer, hunter, runecraft

    @Test
    void ashesGivePrayerXp() {
        assertEquals(10.0, PrayerData.ashes("fiendish_ashes").xp());
        assertEquals(25.0, PrayerData.ashes("vile_ashes").xp());
        assertEquals(65.0, PrayerData.ashes("malicious_ashes").xp());
        assertEquals(85.0, PrayerData.ashes("abyssal_ashes").xp());
        for (PrayerData.Ashes a : PrayerData.ASHES) {
            assertNotNull(ItemCatalog.get(a.id()), a.id());
        }
    }

    @Test
    void trackingCreatures() {
        Object[][] expected = {{"polar_kebbit_burrow", 1, 30.0}, {"common_kebbit_burrow", 3, 36.0}, {"feldip_weasel_burrow", 7, 48.0},
                {"desert_devil_burrow", 13, 66.0}, {"razor_backed_kebbit_burrow", 49, 348.0}};
        for (Object[] e : expected) {
            HunterData.TrapSpot t = HunterData.trap((String) e[0]);
            assertNotNull(t, (String) e[0]);
            assertEquals(HunterData.TrapKind.TRACKING, t.kind());
            assertEquals(e[1], t.level());
            assertEquals(e[2], t.xp());
            assertTrue(t.tools().contains("noose_wand"));
            for (String item : t.loot()) {
                assertNotNull(ItemCatalog.get(item), item);
            }
        }
    }

    @Test
    void combinationRunes() {
        assertEquals(16, RunecraftData.BINDING_CHARGES);
        List<String> ids = RunecraftData.COMBINATIONS.stream().map(RunecraftData.Combination::id).distinct().toList();
        assertEquals(List.of("mist_rune", "dust_rune", "mud_rune", "smoke_rune", "steam_rune", "lava_rune"), ids);
        for (RunecraftData.Combination c : RunecraftData.COMBINATIONS) {
            assertNotNull(ItemCatalog.get(c.talisman()), c.talisman());
            assertNotNull(ItemCatalog.get(c.rune()), c.rune());
            assertNotNull(ItemCatalog.get(c.id()), c.id());
        }
    }

    // ------------------------------------------------------------------ tools and cooking

    @Test
    void specialToolBonuses() {
        assertEquals(71, SpecialTool.levelFor(ToolTier.DRAGON, "crystal_axe"));
        assertEquals(61, SpecialTool.levelFor(ToolTier.DRAGON, "infernal_axe"));
        assertEquals(61, SpecialTool.levelFor(ToolTier.DRAGON, "dragon_axe"));
        assertTrue(SpecialTool.woodcuttingMultiplier(ToolTier.DRAGON, "crystal_axe")
                > SpecialTool.woodcuttingMultiplier(ToolTier.DRAGON, "dragon_axe"));
        assertEquals(ToolTier.DRAGON.woodcuttingMultiplier(), SpecialTool.woodcuttingMultiplier(ToolTier.DRAGON, "infernal_axe"));
        assertEquals(1.0 / 6, SpecialTool.twoTickChance(ToolTier.DRAGON, "dragon_pickaxe"), 1e-9);
        assertEquals(1.0 / 6, SpecialTool.twoTickChance(ToolTier.DRAGON, "infernal_pickaxe"), 1e-9);
        assertEquals(0.25, SpecialTool.twoTickChance(ToolTier.DRAGON, "crystal_pickaxe"), 1e-9);
        assertEquals(0, SpecialTool.twoTickChance(ToolTier.RUNE, "rune_pickaxe"));
        assertEquals(2, SpecialTool.miningTicks(ToolTier.DRAGON, "crystal_pickaxe", 0.2));
        assertEquals(3, SpecialTool.miningTicks(ToolTier.DRAGON, "crystal_pickaxe", 0.3));
        assertEquals(3, SpecialTool.miningTicks(ToolTier.RUNE, "rune_pickaxe", 0.0));
        assertEquals(1.2, SpecialTool.harpoonMultiplier("dragon_harpoon"));
        assertEquals(1.35, SpecialTool.harpoonMultiplier("crystal_harpoon"));
        assertEquals(1.0, SpecialTool.harpoonMultiplier("harpoon"));
        assertTrue(SpecialTool.isHarpoon("infernal_harpoon"));
        assertFalse(SpecialTool.isHarpoon("lobster_pot"));
        assertEquals(Bar.RUNITE, SpecialTool.infernalBar("runite_ore"));
        assertNull(SpecialTool.infernalBar("coal"));
        assertEquals(25.0, SpecialTool.infernalSmithingXp("runite_ore"));
        assertEquals(Tree.MAGIC.firemakingXp() / 2, SpecialTool.infernalFiremakingXp(Tree.MAGIC));
        assertEquals(Fish.SHARK.cookXp() / 2, SpecialTool.infernalCookingXp(Fish.SHARK));
        for (SpecialTool t : SpecialTool.values()) {
            assertNotNull(ItemCatalog.get(t.itemId()), t.itemId());
        }
    }

    @Test
    void hosidiusRangeBurnsLess() {
        assertEquals(0.05, CookingData.rangeBurnReduction(1762, 3616));
        assertEquals(0, CookingData.rangeBurnReduction(3222, 3218));
        Recipe lobster = Recipe.builder("t_lobster", MenuType.COOKING, "Lobster").skill(Skill.COOKING, 40, 120)
                .in("raw_lobster", 1).out("lobster", 1).burn(74, "burnt_fish").build();
        double normal = lobster.cookingSuccess(50, true, false);
        double hosidius = lobster.cookingSuccess(50, true, false, 0.05);
        assertEquals(normal + 0.05, hosidius, 1e-9);
        assertEquals(1.0, lobster.cookingSuccess(80, true, false, 0.05));
        Recipe shark = Recipe.builder("t_shark", MenuType.COOKING, "Shark").skill(Skill.COOKING, 80, 210)
                .in("raw_shark", 1).out("shark", 1).burn(100, "burnt_fish").build();
        assertTrue(shark.cookingSuccess(99, true, false, 0.05) < 1.0, "sharks still burn without gauntlets");
        // Smoke test that the random-based chance helpers are deterministic with java.util.Random too.
        Random r = new Random(1);
        assertTrue(FishingSpotTimers.rollStay(r::nextInt) >= FishingSpotTimers.MIN_STAY);
    }
}
