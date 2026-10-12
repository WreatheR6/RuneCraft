package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Recipes;
import com.runecraft.osrs.Rune;
import com.runecraft.osrs.TreeTimers;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.data.AgilityCourse;
import com.runecraft.osrs.data.Allotment;
import com.runecraft.osrs.data.Bar;
import com.runecraft.osrs.data.Bone;
import com.runecraft.osrs.data.Fish;
import com.runecraft.osrs.data.Herb;
import com.runecraft.osrs.data.HunterCreature;
import com.runecraft.osrs.data.Potion;
import com.runecraft.osrs.data.Rock;
import com.runecraft.osrs.data.RuneAltar;
import com.runecraft.osrs.data.Thieving;
import com.runecraft.osrs.data.ToolTier;
import com.runecraft.osrs.data.Tree;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Key OSRS values for the non-combat skills (levels, XP, tool tiers, depletion, burn levels). */
class SkillingFidelityTest {
    private static Recipe recipe(String id) {
        Recipe r = Recipes.get(id);
        assertNotNull(r, "missing recipe " + id);
        return r;
    }

    private static void level(int expected, String recipeId) {
        assertEquals(expected, recipe(recipeId).level(), recipeId + " level");
    }

    private static void xp(double expected, String recipeId) {
        assertEquals(expected, recipe(recipeId).xpTenths() / 10.0, 0.05, recipeId + " xp");
    }

    @Test
    void woodcuttingAndFiremaking() {
        assertEquals(1, Tree.TREE.level());
        assertEquals(25.0, Tree.TREE.xp());
        assertEquals(60, Tree.YEW.level());
        assertEquals(175.0, Tree.YEW.xp());
        assertEquals(250.0, Tree.MAGIC.xp());
        assertEquals(380.0, Tree.REDWOOD.xp());
        assertEquals(64, Tree.TREE.low());
        assertEquals(200, Tree.TREE.high());
        assertEquals(4, Tree.YEW.low());
        assertEquals(12, Tree.YEW.high());
        // Forestry timers (game ticks): normal trees fall after one log.
        assertEquals(0, Tree.TREE.despawnTicks());
        assertEquals(45, Tree.OAK.despawnTicks());
        assertEquals(50, Tree.WILLOW.despawnTicks());
        assertEquals(100, Tree.MAPLE.despawnTicks());
        assertEquals(190, Tree.YEW.despawnTicks());
        assertEquals(390, Tree.MAGIC.despawnTicks());
        assertEquals(440, Tree.REDWOOD.despawnTicks());
        // Firemaking
        assertEquals(40.0, Tree.TREE.firemakingXp());
        assertEquals(60.0, Tree.OAK.firemakingXp());
        assertEquals(202.5, Tree.YEW.firemakingXp());
        assertEquals(303.8, Tree.MAGIC.firemakingXp());
        assertEquals(350.0, Tree.REDWOOD.firemakingXp());
        assertEquals(125.0, GatheringData.TREES.stream().filter(t -> t.blockId().equals("arctic_pine_tree")).findFirst()
                .orElseThrow().fmXp());
    }

    @Test
    void axeAndPickaxeTiers() {
        // Rune axe on a normal tree: 224-700/256 (3.5x bronze); dragon 240-750 (3.75x).
        assertEquals(224.0, Tree.TREE.low() * ToolTier.RUNE.woodcuttingMultiplier(), 1e-9);
        assertEquals(750.0, Tree.TREE.high() * ToolTier.DRAGON.woodcuttingMultiplier(), 1e-9);
        assertEquals(1, ToolTier.IRON.level());
        assertEquals(6, ToolTier.STEEL.level());
        assertEquals(21, ToolTier.MITHRIL.level());
        assertEquals(41, ToolTier.RUNE.level());
        assertEquals(61, ToolTier.DRAGON.level());
        assertEquals(8, ToolTier.BRONZE.miningTicks());
        assertEquals(3, ToolTier.RUNE.miningTicks());
    }

    @Test
    void treeTimersCountDownOnlyWhileChopping() {
        TreeTimers timers = new TreeTimers();
        assertTrue(timers.chop("t", 0, 0), "normal trees fall after one log");
        // An oak (45 ticks) chopped continuously, rolling every 4 ticks, runs out after ~45 ticks.
        long tick = 100;
        int rolls = 0;
        while (!timers.chop("oak", 45, tick)) {
            tick += 4;
            rolls++;
            assertTrue(rolls < 20, "oak timer never ran out");
        }
        assertTrue(tick - 100 >= 40 && tick - 100 <= 48, "oak fell after " + (tick - 100) + " ticks");
        // A yew chopped for a while and then left alone regenerates.
        timers.chop("yew", 190, 0);
        for (int t = 4; t <= 100; t += 4) {
            timers.chop("yew", 190, t);
        }
        long left = timers.remaining("yew", 190);
        assertTrue(left < 100, "yew timer did not count down: " + left);
        assertFalse(timers.chop("yew", 190, 1000));
        assertTrue(timers.remaining("yew", 190) > 180, "yew timer did not regenerate");
        timers.reset("yew");
        assertEquals(190, timers.remaining("yew", 190));
    }

    @Test
    void miningRocks() {
        assertEquals(17.5, Rock.COPPER.xp());
        assertEquals(35.0, Rock.IRON.xp());
        assertEquals(15, Rock.IRON.level());
        assertEquals(50.0, Rock.COAL.xp());
        assertEquals(55, Rock.MITHRIL.level());
        assertEquals(85, Rock.RUNITE.level());
        assertEquals(125.0, Rock.RUNITE.xp());
        assertEquals(1200, Rock.RUNITE.respawnTicks());
        assertEquals(30, Rock.PURE_ESSENCE.level());
        assertFalse(Rock.PURE_ESSENCE.depletes());
        assertEquals(240.0, Rock.AMETHYST.xp());
        MiningData.MineRock lime = MiningData.rock("limestone_rock");
        assertNotNull(lime);
        assertEquals(10, lime.level());
        assertEquals(26.5, lime.yields().get(0).xp());
        level(12, "chisel_limestone_brick");
    }

    @Test
    void fishingAndCooking() {
        assertEquals(40, Fish.LOBSTER.level());
        assertEquals(90.0, Fish.LOBSTER.xp());
        assertEquals(76, Fish.SHARK.level());
        assertEquals(110.0, Fish.SHARK.xp());
        assertEquals(80, Fish.SHARK.cookLevel());
        assertEquals(210.0, Fish.SHARK.cookXp());
        assertEquals(20, Fish.SHARK.heal());
        assertEquals(74, Fish.LOBSTER.stopBurn());
        assertEquals(86, Fish.SWORDFISH.stopBurn());
        assertEquals(64, Fish.TUNA.stopBurn());
        // Cooking gauntlets: lobster 74 -> 64, swordfish 86 -> 81.
        Recipe lobster = recipe("cook_lobster");
        assertTrue(lobster.cookingSuccess(70, false) < 1.0);
        assertEquals(1.0, lobster.cookingSuccess(64, false, true), 1e-9);
        assertEquals(81, recipe("cook_swordfish").gauntletStopBurn());
        // Sharks never stop burning without gauntlets, even on a range at 99.
        Recipe shark = recipe("cook_shark");
        assertTrue(shark.cookingSuccess(99, true) < 1.0);
        assertEquals(1.0, shark.cookingSuccess(94, false, true), 1e-9);
        // Trawled Sailing fish (OSRS Wiki).
        CookingData.NewFish marlin = CookingData.FISH.stream().filter(f -> f.id().equals("marlin")).findFirst().orElseThrow();
        assertEquals(91, marlin.fishLevel());
        assertEquals(265.5, marlin.fishXp());
        assertEquals(24, marlin.heal());
        CookingData.NewFish krill = CookingData.FISH.stream().filter(f -> f.id().equals("giant_krill")).findFirst().orElseThrow();
        assertEquals(69, krill.fishLevel());
        assertEquals(112.5, krill.fishXp());
        assertNotNull(ItemCatalog.get("cooking_gauntlets"));
    }

    @Test
    void smithingLevelsAndBars() {
        assertEquals(1, Bar.BRONZE.level());
        assertEquals(0.5, Bar.IRON.successChance());
        assertEquals(2, Bar.STEEL.ores().get("coal"));
        assertEquals(8, Bar.RUNITE.ores().get("coal"));
        assertEquals(50.0, Bar.RUNITE.xp());
        // Bronze items start at level 1 (dagger/axe), not 2; platebody 18.
        level(1, "smith_bronze_dagger");
        level(1, "smith_bronze_axe");
        level(2, "smith_bronze_mace");
        level(3, "smith_bronze_med_helm");
        level(5, "smith_bronze_scimitar");
        level(5, "smith_bronze_pickaxe");
        level(7, "smith_bronze_full_helm");
        level(14, "smith_bronze_2h_sword");
        level(18, "smith_bronze_platebody");
        level(3, "smith_bronze_bolts_unf");
        level(15, "smith_iron_dagger");
        level(33, "smith_iron_platebody");
        level(48, "smith_steel_platebody");
        level(85, "smith_rune_dagger");
        level(90, "smith_rune_pickaxe");
        level(99, "smith_rune_platebody");
        level(54, "smith_mithril_dart_tip");
        xp(12.5, "smith_bronze_dagger");
        xp(62.5, "smith_bronze_platebody");
        xp(375.0, "smith_rune_platebody");
    }

    @Test
    void herbloreLevels() {
        // Unfinished potions need the level of the potion they make, not the cleaning level.
        level(3, "unf_guam_potion_unf");
        level(12, "unf_tarromin_potion_unf");
        level(30, "unf_ranarr_potion_unf");
        level(78, "unf_torstol_potion_unf");
        assertEquals(11, Herb.TARROMIN.cleanLevel());
        assertEquals(38, Potion.PRAYER.level());
        assertEquals("snape_grass", Potion.PRAYER.secondary());
        assertEquals(Herb.RANARR, Potion.PRAYER.herb());
        assertEquals("red_spiders_eggs", Potion.SUPER_RESTORE.secondary());
        assertEquals(142.5, Potion.SUPER_RESTORE.xp());
        assertEquals("crushed_nest", Potion.SARADOMIN_BREW.secondary());
        level(90, "mix_super_combat_potion");
        xp(150.0, "mix_super_combat_potion");
        level(77, "mix_stamina_potion");
        level(87, "mix_anti_venom");
    }

    @Test
    void runecrafting() {
        RuneAltar air = RuneAltar.of(Rune.AIR);
        assertEquals(1, air.level());
        assertEquals(5.0, air.xp());
        assertEquals(3, RuneAltar.WATER.runesPerEssence(38));
        assertEquals(2, RuneAltar.CHAOS.runesPerEssence(74));
        assertEquals(1, RuneAltar.LAW.runesPerEssence(99));
        assertTrue(RuneAltar.COSMIC.pureOnly());
        assertFalse(RuneAltar.BODY.pureOnly());
        assertEquals(10.5, RuneAltar.BLOOD.xp());
        assertEquals(77, RuneAltar.BLOOD.level());
        assertEquals(95, RuneAltar.WRATH.level());
    }

    @Test
    void agility() {
        assertEquals(86.5, AgilityCourse.GNOME_STRONGHOLD.lapXp());
        assertEquals(10, AgilityCourse.DRAYNOR.level());
        assertEquals(120.0, AgilityCourse.DRAYNOR.lapXp());
        assertEquals(216.0, AgilityCourse.AL_KHARID.lapXp());
        assertEquals(586.0, AgilityCourse.FALADOR.lapXp());
        assertEquals(570.0, AgilityCourse.SEERS.lapXp());
        assertEquals(889.0, AgilityCourse.ARDOUGNE.lapXp());
        for (AgilityCourse c : AgilityCourse.values()) {
            double sum = 0;
            for (int i = 0; i < c.obstacles(); i++) {
                sum += c.obstacleXp(i);
            }
            assertEquals(c.lapXp(), sum, 1e-6, c.name());
            assertTrue(c.obstacles() <= AgilityCourse.MAX_OBSTACLES, c + " has more obstacles than the block's step range");
        }
    }

    @Test
    void thieving() {
        assertEquals(8.0, Thieving.Pickpocket.MAN.xp());
        assertEquals(40, Thieving.Pickpocket.GUARD.level());
        assertEquals(273.3, Thieving.Pickpocket.HERO.xp());
        assertEquals(75, Thieving.Stall.GEM.level());
        assertEquals(160.0, Thieving.Stall.GEM.xp());
        ThievingData.Target farmer = ThievingData.target("master_farmer");
        assertEquals(38, farmer.level());
        assertEquals(43.0, farmer.xp());
        ThievingData.Container fruit = ThievingData.container("fruit_stall");
        assertNotNull(fruit);
        assertEquals(25, fruit.level());
        assertEquals(28.5, fruit.xp());
        assertTrue(ContentRegistry.blocks().stream().anyMatch(b -> b.id().equals("fruit_stall")));
        assertEquals(84, ThievingData.container("rogues_castle_chest").level());
    }

    @Test
    void farming() {
        assertEquals(160, FarmingData.byId("oak").growthMinutes());
        assertEquals(480, FarmingData.byId("magic").growthMinutes());
        assertEquals(960, FarmingData.byId("palm").growthMinutes());
        assertEquals(1280, FarmingData.byId("calquat").growthMinutes());
        assertEquals(3840, FarmingData.byId("spirit").growthMinutes());
        assertEquals(13768.3, FarmingData.byId("magic").checkXp());
        assertEquals(4, FarmingData.byId("barley").count());
        assertEquals(3, FarmingData.byId("jute").count());
        assertEquals(3, FarmingData.byId("wildblood").count());
        assertEquals(85, Herb.TORSTOL.farmingLevel());
        assertEquals(224.5, Herb.TORSTOL.harvestXp());
        assertEquals(47, Allotment.WATERMELON.level());
        assertEquals(26.0, FarmingData.Compost.SUPERCOMPOST.applyXp);
    }

    @Test
    void hunterCraftingFletchingPrayer() {
        assertEquals(63, HunterCreature.RED_CHINCHOMPA.level());
        assertEquals(265.0, HunterCreature.RED_CHINCHOMPA.xp());
        assertEquals(59, HunterData.trap("red_salamander_tree").level());
        assertEquals(89, HunterData.impling("lucky_impling").level());
        level(43, "cut_diamond");
        xp(107.5, "cut_diamond");
        level(84, "craft_black_dhide_body");
        level(33, "blow_vial");
        level(70, "fletch_yew_longbow_u");
        xp(75.0, "fletch_yew_longbow_u");
        level(15, "fletch_arrow_shaft_oak");
        assertEquals(30, recipe("fletch_arrow_shaft_oak").outputs().get("arrow_shaft"));
        assertEquals(105, recipe("fletch_arrow_shaft_redwood").outputs().get("arrow_shaft"));
        xp(187.5, "fletch_rune_arrow");
        assertEquals(72.0, Bone.DRAGON_BONES.xp());
        assertEquals(4.5, Bone.BONES.xp());
        assertEquals(3.5, Bone.ALTAR_MULTIPLIER);
    }
}
