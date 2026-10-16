package com.runecraft.selftest;

import com.runecraft.action.ActionManager;
import com.runecraft.block.AgilityObstacleBlock;
import com.runecraft.block.FarmingPatchBlock;
import com.runecraft.block.FishingSpotBlock;
import com.runecraft.block.HerbPatchBlock;
import com.runecraft.block.StallBlock;
import com.runecraft.block.TrapBlock;
import com.runecraft.menu.MenuService;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Recipes;
import com.runecraft.osrs.Region;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.Station;
import com.runecraft.osrs.data.AgilityCourse;
import com.runecraft.osrs.data.Bone;
import com.runecraft.osrs.data.FishingMethod;
import com.runecraft.osrs.data.FishingSpot;
import com.runecraft.osrs.data.Herb;
import com.runecraft.osrs.data.HunterCreature;
import com.runecraft.osrs.data.Monster;
import com.runecraft.osrs.data.Rock;
import com.runecraft.osrs.data.RuneAltar;
import com.runecraft.osrs.data.Thieving;
import com.runecraft.osrs.data.Tree;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModBlocks;
import com.runecraft.registry.ModEntities;
import com.runecraft.skill.HunterService;
import com.runecraft.util.Inv;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Self-test checks for the gathering and support skills: each does one real action the way a player does it, through
 * the server's own interaction code ({@code interactionManager.interactBlock/interactItem}, the use-entity callback, a
 * block's step hook), on a block placed next to the test platform (removed again afterwards), and checks the XP gained
 * and the items produced. Artisan menus are opened from their station or tool and one recipe is made.
 */
final class SkillChecks {
    /** Game ticks a gathering action may take before the check gives up. */
    private static final int GATHER_TICKS = 80;

    private final TestContext c;

    private SkillChecks(TestContext c) {
        this.c = c;
    }

    static List<SelfTest.Check> all(TestContext context) {
        SkillChecks g = new SkillChecks(context);
        List<SelfTest.Check> checks = new ArrayList<>();
        checks.add(new SelfTest.Check("skill:woodcutting", g::woodcutting));
        checks.add(new SelfTest.Check("skill:mining", g::mining));
        checks.add(new SelfTest.Check("skill:fishing", g::fishing));
        checks.add(new SelfTest.Check("skill:firemaking", g::firemaking));
        checks.add(new SelfTest.Check("skill:cooking_range", g::cooking));
        checks.add(new SelfTest.Check("skill:agility_lap", g::agility));
        checks.add(new SelfTest.Check("skill:thieving_pickpocket", g::pickpocket));
        checks.add(new SelfTest.Check("skill:thieving_stall", g::stall));
        checks.add(new SelfTest.Check("skill:farming_herb", g::farming));
        checks.add(new SelfTest.Check("skill:hunter_snare", g::hunter));
        checks.add(new SelfTest.Check("skill:runecrafting", g::runecrafting));
        checks.add(new SelfTest.Check("skill:prayer", g::prayer));
        checks.add(new SelfTest.Check("skill:herblore", g::herblore));
        checks.add(new SelfTest.Check("skill:crafting", g::crafting));
        checks.add(new SelfTest.Check("skill:fletching", g::fletching));
        checks.add(new SelfTest.Check("skill:smithing", g::smithing));
        checks.add(new SelfTest.Check("skill:construction", g::construction));
        return checks;
    }

    private static AssertionError failure(String message) {
        return new AssertionError(message);
    }

    private ServerPlayerEntity p() {
        return c.player;
    }

    private PlayerData data() {
        return Players.data(c.player);
    }

    private int xp(Skill skill) {
        return data().skills().xpTenths(skill);
    }

    /** Puts a registered item in the main hand (before anything else is given, so it lands in the selected slot). */
    private void hold(String id, int count) {
        ItemStack s = Inv.stack(id, count);
        if (s.isEmpty()) {
            throw failure("item " + id + " is not registered");
        }
        // The fake player is never ticked, so a use delay from an earlier run would never wear off.
        p().getItemCooldownManager().remove(s.getItem());
        p().setStackInHand(Hand.MAIN_HAND, s);
    }

    private void give(String id, int count) {
        if (!id.equals(Inv.COINS) && !id.startsWith("minecraft:") && Inv.stack(id, 1).isEmpty()) {
            throw failure("item " + id + " is not registered");
        }
        Inv.give(p(), id, count);
    }

    /** Right-clicks a block face as a player does (use-block callbacks, the block's onUse, then the held item's useOnBlock). */
    private ActionResult useBlock(BlockPos pos, Direction side) {
        p().setSneaking(false);
        Vec3d at = Vec3d.ofCenter(pos).add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5);
        return p().interactionManager.interactBlock(p(), c.world, p().getMainHandStack(), Hand.MAIN_HAND,
                new BlockHitResult(at, side, pos, false));
    }

    /** Right-clicks with the held item as a player does (use-item callbacks, then the item's use). */
    private ActionResult useItem() {
        p().setSneaking(false);
        return p().interactionManager.interactItem(p(), c.world, p().getMainHandStack(), Hand.MAIN_HAND);
    }

    /** Advances the player's action one game tick at a time until {@code done} or the action ends. */
    private void tickUntil(java.util.function.BooleanSupplier done) {
        for (int i = 0; i < GATHER_TICKS && !done.getAsBoolean() && ActionManager.tickNow(p()); i++) {
            // one game tick per call
        }
        ActionManager.cancel(p());
    }

    /** Checks an action gave {@code skill} XP (at least {@code minTenths}) and returns the gain in tenths. */
    private int gained(Skill skill, int before, int minTenths, String what) {
        int got = xp(skill) - before;
        if (got < Math.max(1, minTenths)) {
            throw failure(what + " gave " + got / 10.0 + " " + skill.displayName() + " XP, expected " + Math.max(1, minTenths) / 10.0);
        }
        return got;
    }

    private BlockPos placeNear(BlockState state) {
        return c.place(c.near(2, 0), state);
    }

    // ------------------------------------------------------------------ gathering

    /** A gathering node: hold the tool, click the node, let the action run until the product arrives. */
    private String gather(BlockState node, String tool, String product, Skill skill, int xpTenths, String what) {
        hold(tool, 1);
        BlockPos pos = placeNear(node);
        int before = Inv.count(p(), product);
        int xp0 = xp(skill);
        useBlock(pos, Direction.WEST);
        if (!ActionManager.isBusy(p())) {
            throw failure("clicking the " + what + " with " + tool + " started no action");
        }
        tickUntil(() -> Inv.count(p(), product) > before);
        int got = Inv.count(p(), product) - before;
        if (got <= 0) {
            throw failure("no " + product + " after " + GATHER_TICKS + " game ticks at the " + what);
        }
        int gained = gained(skill, xp0, xpTenths, "the " + what);
        return got + " " + product + ", +" + gained / 10.0 + " xp";
    }

    private String woodcutting() {
        Tree tree = Tree.TREE;
        return gather(ModBlocks.TREES.get(tree).getDefaultState(), "rune_axe", tree.logItem(), Skill.WOODCUTTING,
                (int) Math.round(tree.xp() * 10), "tree");
    }

    private String mining() {
        Rock rock = Rock.COPPER;
        return gather(ModBlocks.ROCKS.get(rock).getDefaultState(), "rune_pickaxe", rock.oreItem(), Skill.MINING,
                (int) Math.round(rock.xp() * 10), "copper rocks");
    }

    private String fishing() {
        FishingSpot spot = FishingSpot.SEA_NET_BAIT;
        String fish = spot.catches().get(FishingMethod.NET).get(spot.catches().get(FishingMethod.NET).size() - 1).rawItem();
        // Not waterlogged: on the dry platform the spot can't wander off and spreads no water.
        BlockState state = ModBlocks.FISHING_SPOTS.get(spot).getDefaultState().with(FishingSpotBlock.WATERLOGGED, false);
        hold(FishingMethod.NET.tool(), 1);
        BlockPos pos = placeNear(state);
        int xp0 = xp(Skill.FISHING);
        useBlock(pos, Direction.UP);
        tickUntil(() -> xp(Skill.FISHING) > xp0);
        List<String> caught = new ArrayList<>();
        for (FishingMethod m : spot.catches().keySet()) {
            spot.catches().get(m).forEach(f -> {
                if (Inv.count(p(), f.rawItem()) > 0) {
                    caught.add(f.rawItem());
                }
            });
        }
        if (caught.isEmpty()) {
            throw failure("no fish after " + GATHER_TICKS + " game ticks netting at a " + spot.name() + " spot (e.g. " + fish + ")");
        }
        return "caught " + caught + ", +" + gained(Skill.FISHING, xp0, 1, "netting") / 10.0 + " xp";
    }

    private String firemaking() {
        Tree logs = Tree.TREE;
        hold("tinderbox", 1);
        give(logs.logItem(), 1);
        BlockPos at = p().getBlockPos();
        c.track(at);
        int xp0 = xp(Skill.FIREMAKING);
        useItem();
        tickUntil(() -> Inv.count(p(), logs.logItem()) == 0);
        if (Inv.count(p(), logs.logItem()) != 0) {
            throw failure("the logs were not burnt after " + GATHER_TICKS + " game ticks");
        }
        if (c.world.getBlockState(at).getBlock() != ModBlocks.FIRE) {
            throw failure("no fire where the logs were lit, found " + c.world.getBlockState(at));
        }
        return "fire lit, +" + gained(Skill.FIREMAKING, xp0, (int) Math.round(logs.firemakingXp() * 10), "lighting logs") / 10.0 + " xp";
    }

    // ------------------------------------------------------------------ menus (artisan skills)

    /** The first recipe of a menu that takes {@code input} (or makes {@code output}, when input is null). */
    private static Recipe recipe(MenuType menu, String input, String output) {
        for (Recipe r : Recipes.menu(menu)) {
            if (input != null ? r.inputs().containsKey(input) : r.outputs().containsKey(output)) {
                return r;
            }
        }
        throw failure("no " + menu + " recipe " + (input != null ? "takes " + input : "makes " + output));
    }

    /**
     * Opens a menu the way a player does ({@code open}), makes one {@code recipe} through {@link MenuService#choose} and
     * checks its product and XP. The inputs and tool are handed out first (the held item is kept).
     */
    private String make(Recipe r, MenuType menu, String how, Runnable open) {
        if (r.requiredTool() != null && !Inv.has(p(), r.requiredTool(), 1)) {
            give(r.requiredTool(), 1);
        }
        r.inputs().forEach(this::give);
        Map<String, Integer> before = new LinkedHashMap<>();
        r.outputs().keySet().forEach(item -> before.put(item, Inv.count(p(), item)));
        Skill skill = r.skill();
        int xp0 = skill == null ? 0 : xp(skill);
        data().openMenu = null;
        open.run();
        if (data().openMenu != menu) {
            throw failure(how + " did not open the " + menu + " menu (the inputs of " + r.id() + " were carried)");
        }
        MenuService.choose(p(), r.id(), 1);
        tickUntil(() -> before.entrySet().stream().anyMatch(e -> Inv.count(p(), e.getKey()) > e.getValue()));
        List<String> made = new ArrayList<>();
        before.forEach((item, n) -> {
            if (Inv.count(p(), item) > n) {
                made.add(item);
            }
        });
        if (made.isEmpty() && !r.outputs().isEmpty()) {
            throw failure(how + ": " + r.id() + " made nothing");
        }
        String xpNote = "";
        if (skill != null && r.xpTenths() > 0) {
            xpNote = ", +" + gained(skill, xp0, r.xpTenths(), r.id()) / 10.0 + " xp";
        }
        return r.id() + " via " + how + xpNote;
    }

    /** A station block placed next to the player ({@code dz} blocks aside) that the returned action right-clicks. */
    private Runnable station(Station station, int dz) {
        BlockPos pos = c.place(c.near(2, dz), ModBlocks.STATIONS.get(station).getDefaultState());
        return () -> useBlock(pos, Direction.WEST);
    }

    private Runnable station(Station station) {
        return station(station, 0);
    }

    private String cooking() {
        hold("raw_shrimps", 1);
        Recipe r = recipe(MenuType.COOKING, "raw_shrimps", null);
        return make(r, MenuType.COOKING, "a range", station(Station.RANGE));
    }

    private String crafting() {
        hold("chisel", 1);
        String gems = make(recipe(MenuType.GEM_CUTTING, "uncut_sapphire", null), MenuType.GEM_CUTTING, "a chisel", this::useItem);
        p().getInventory().clear();
        hold("needle", 1);
        String leather = make(recipe(MenuType.LEATHERWORKING, null, "leather_gloves"), MenuType.LEATHERWORKING, "a needle",
                this::useItem);
        return gems + "; " + leather;
    }

    private String fletching() {
        hold("knife", 1);
        return make(recipe(MenuType.FLETCHING, null, "arrow_shaft"), MenuType.FLETCHING, "a knife", this::useItem);
    }

    private String smithing() {
        String bar = make(recipe(MenuType.SMELTING, null, "bronze_bar"), MenuType.SMELTING, "a furnace", station(Station.FURNACE));
        p().getInventory().clear();
        hold("hammer", 1);
        String dagger = make(recipe(MenuType.SMITHING, null, "bronze_dagger"), MenuType.SMITHING, "an anvil", station(Station.ANVIL, 2));
        return bar + "; " + dagger;
    }

    private String construction() {
        hold("hammer", 1);
        return make(recipe(MenuType.CONSTRUCTION, null, "crude_wooden_chair_flatpack"), MenuType.CONSTRUCTION, "a workbench",
                station(Station.WORKBENCH));
    }

    private String herblore() {
        c.completeQuests(null); // Druidic Ritual
        Herb herb = Herb.GUAM;
        hold(herb.grimyItem(), 1);
        int xp0 = xp(Skill.HERBLORE);
        useItem();
        if (Inv.count(p(), herb.cleanItem()) != 1 || Inv.count(p(), herb.grimyItem()) != 0) {
            throw failure("cleaning a " + herb.grimyItem() + " left " + Inv.count(p(), herb.grimyItem()) + " grimy and "
                    + Inv.count(p(), herb.cleanItem()) + " clean");
        }
        int clean = gained(Skill.HERBLORE, xp0, (int) Math.round(herb.cleanXp() * 10), "cleaning");
        p().getInventory().clear();
        hold("vial_of_water", 1);
        String unf = make(recipe(MenuType.HERBLORE, null, "guam_potion_unf"), MenuType.HERBLORE, "a vial of water", this::useItem);
        String potion = make(recipe(MenuType.HERBLORE, null, "attack_potion"), MenuType.HERBLORE, "an apothecary table",
                station(Station.HERBLORE_TABLE, 2));
        return "cleaned +" + clean / 10.0 + " xp; " + unf + "; " + potion;
    }

    // ------------------------------------------------------------------ support skills

    private String agility() {
        AgilityCourse course = AgilityCourse.GNOME_STRONGHOLD;
        int n = course.obstacles();
        List<BlockPos> obstacles = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            BlockState state = ModBlocks.AGILITY_OBSTACLE.getDefaultState().with(AgilityObstacleBlock.COURSE, course.ordinal())
                    .with(AgilityObstacleBlock.STEP, i);
            obstacles.add(c.place(c.near(i - n / 2, 4).down(), state));
        }
        int expected = 0;
        for (int i = 0; i < n; i++) {
            expected += (int) Math.round(course.obstacleXp(i) * 10);
        }
        int laps = data().agilityLaps(course);
        int xp0 = xp(Skill.AGILITY);
        for (BlockPos pos : obstacles) {
            p().teleport(c.world, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, p().getYaw(), p().getPitch());
            BlockState state = c.world.getBlockState(pos);
            state.getBlock().onSteppedOn(c.world, pos, state, p());
        }
        c.home();
        if (data().agilityLaps(course) != laps + 1) {
            throw failure("stepping on the " + n + " obstacles in order did not complete a lap (laps " + laps + " -> "
                    + data().agilityLaps(course) + ")");
        }
        int got = gained(Skill.AGILITY, xp0, expected - n, "a lap");
        return n + " obstacles, +" + got / 10.0 + " xp (expected " + expected / 10.0 + ")";
    }

    private String pickpocket() {
        EntityType<?> type = ModEntities.typeById(Monster.MAN.entity());
        if (type == null) {
            throw failure("no entity type " + Monster.MAN.entity());
        }
        Entity e = type.create(c.world);
        if (!(e instanceof LivingEntity man)) {
            throw failure(Monster.MAN.entity() + " is not a living entity");
        }
        BlockPos pos = c.near(2, 0);
        man.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        if (man instanceof MobEntity mob) {
            mob.initialize(c.world, c.world.getLocalDifficulty(pos), SpawnReason.SPAWNER, null, null);
            mob.setPersistent();
            mob.setAiDisabled(true);
        }
        c.world.spawnEntity(man);
        Thieving.Pickpocket kind = Thieving.Pickpocket.MAN;
        String pouch = Thieving.pouchId(kind.pouch());
        int xp0 = xp(Skill.THIEVING);
        int tries = 0;
        while (xp(Skill.THIEVING) == xp0 && tries < 10) {
            tries++;
            data().stunnedUntil = 0; // a failed attempt stuns
            c.heal();
            UseEntityCallback.EVENT.invoker().interact(p(), c.world, Hand.MAIN_HAND, man, null);
        }
        int got = gained(Skill.THIEVING, xp0, (int) Math.round(kind.xp() * 10), "pickpocketing a man (" + tries + " tries)");
        if (Inv.count(p(), pouch) <= 0) {
            throw failure("a successful pickpocket gave no " + pouch);
        }
        return "+" + got / 10.0 + " xp, " + Inv.count(p(), pouch) + " " + pouch + " in " + tries + " tries";
    }

    private String stall() {
        Thieving.Stall stall = Thieving.Stall.CAKE;
        BlockPos pos = placeNear(ModBlocks.STALLS.get(stall).getDefaultState());
        int xp0 = xp(Skill.THIEVING);
        useBlock(pos, Direction.WEST);
        int got = gained(Skill.THIEVING, xp0, (int) Math.round(stall.xp() * 10), "stealing from the " + stall.displayName());
        List<String> loot = new ArrayList<>();
        stall.loot().forEach(l -> {
            if (Inv.count(p(), l.item()) > 0) {
                loot.add(l.item());
            }
        });
        if (loot.isEmpty()) {
            throw failure("stealing from the " + stall.displayName() + " gave none of its loot");
        }
        if (!c.world.getBlockState(pos).get(StallBlock.EMPTY)) {
            throw failure("the stall was not emptied");
        }
        return loot + ", +" + got / 10.0 + " xp";
    }

    private String farming() {
        Herb herb = Herb.GUAM;
        BlockPos pos = placeNear(ModBlocks.HERB_PATCH.getDefaultState());
        int xp0 = xp(Skill.FARMING);
        hold("compost", 1);
        useBlock(pos, Direction.UP);
        if (c.world.getBlockState(pos).get(FarmingPatchBlock.COMPOST) != 1) {
            throw failure("using compost on the patch did not compost it");
        }
        p().getInventory().clear();
        hold(herb.seedItem(), 1);
        give("seed_dibber", 1);
        useBlock(pos, Direction.UP);
        BlockState planted = c.world.getBlockState(pos);
        if (planted.get(HerbPatchBlock.CROP) != herb.ordinal() + 1 || planted.get(FarmingPatchBlock.STAGE) != 1) {
            throw failure("planting a " + herb.seedItem() + " left the patch at " + planted);
        }
        // Growing: random ticks (the gardener's protection keeps disease out of the check).
        c.world.setBlockState(pos, planted.with(FarmingPatchBlock.PROTECTED, true));
        int ticks = 0;
        while (c.world.getBlockState(pos).get(FarmingPatchBlock.STAGE) < 4 && ticks < 20_000) {
            c.world.getBlockState(pos).randomTick(c.world, pos, c.world.getRandom());
            ticks++;
        }
        BlockState grown = c.world.getBlockState(pos);
        if (grown.get(FarmingPatchBlock.STAGE) < 4) {
            throw failure("the herb did not grow in " + ticks + " random ticks: " + grown);
        }
        p().getInventory().clear();
        useBlock(pos, Direction.UP);
        int picked = Inv.count(p(), herb.grimyItem());
        if (picked <= 0) {
            throw failure("harvesting the grown patch gave no " + herb.grimyItem());
        }
        if (c.world.getBlockState(pos).get(HerbPatchBlock.CROP) != 0) {
            throw failure("the patch was not cleared after the harvest");
        }
        return "composted, planted, grown in " + ticks + " random ticks, picked " + picked + ", +"
                + gained(Skill.FARMING, xp0, 1, "farming") / 10.0 + " xp";
    }

    /**
     * Bird snare: laid by using it on the floor, rolled by the trap's scheduled check and collected. The platform is in
     * Misthalin, where no birds live, so the snare goes on a floor block placed high above the first landmark in a bird's
     * region.
     */
    private String hunter() {
        HunterCreature.Trap trap = HunterCreature.Trap.BIRD_SNARE;
        BlockPos floor = null;
        HunterCreature bird = null;
        for (Landmark l : Landmark.values()) {
            int x = l.blockX() + 2;
            int z = l.blockZ() + 2;
            Region region = GielinorMap.regionAtBlock(x, z);
            HunterCreature best = HunterCreature.best(trap, region, 99);
            if (best != null) {
                floor = new BlockPos(x, TestContext.PLATFORM.getY() - 1, z);
                bird = best;
                break;
            }
        }
        if (floor == null) {
            throw failure("no landmark lies in a region with birds to snare");
        }
        c.world.getChunk(floor.getX() >> 4, floor.getZ() >> 4);
        c.place(floor, Blocks.SMOOTH_STONE.getDefaultState());
        BlockPos at = floor.up();
        c.track(at);
        c.place(at, Blocks.AIR.getDefaultState());
        hold("bird_snare", 1);
        useBlock(floor, Direction.UP);
        BlockState laid = c.world.getBlockState(at);
        if (!(laid.getBlock() instanceof TrapBlock block)) {
            throw failure("using a bird snare on the floor laid nothing (found " + laid + ")");
        }
        if (Inv.count(p(), "bird_snare") != 0) {
            throw failure("the snare was not used up when laid");
        }
        int rolls = 0;
        while (!c.world.getBlockState(at).get(TrapBlock.CAUGHT) && rolls < 40) {
            HunterService.checkTrap(c.world, at, c.world.getBlockState(at), block);
            rolls++;
        }
        String how = rolls + " checks";
        if (!c.world.getBlockState(at).get(TrapBlock.CAUGHT)) {
            if (c.realPlayer()) {
                throw failure("no " + bird.displayName() + " caught in " + rolls + " trap checks at 99 Hunter");
            }
            // The trap looks its hunter up in the world's player list, which the fake player isn't in.
            c.world.setBlockState(at, c.world.getBlockState(at).with(TrapBlock.CAUGHT, true));
            how = "catch set by hand (the fake player is not in the world's player list)";
        }
        int xp0 = xp(Skill.HUNTER);
        p().getInventory().clear();
        useBlock(at, Direction.UP);
        if (c.world.getBlockState(at).getBlock() instanceof TrapBlock) {
            throw failure("collecting did not take the trap down");
        }
        List<String> loot = new ArrayList<>();
        for (String item : bird.loot()) {
            if (Inv.count(p(), item) <= 0) {
                throw failure("a caught " + bird.displayName() + " gave no " + item);
            }
            loot.add(item);
        }
        if (Inv.count(p(), "bird_snare") != 1) {
            throw failure("the snare did not come back");
        }
        int got = gained(Skill.HUNTER, xp0, (int) Math.round(bird.xp() * 10), "a " + bird.displayName());
        return bird.displayName() + " (" + how + "), " + loot + ", +" + got / 10.0 + " xp";
    }

    private String runecrafting() {
        RuneAltar altar = RuneAltar.AIR;
        BlockPos pos = placeNear(ModBlocks.ALTARS.get(altar).getDefaultState());
        give("pure_essence", 5);
        String rune = altar.rune().itemId();
        int xp0 = xp(Skill.RUNECRAFT);
        useBlock(pos, Direction.WEST);
        int runes = Inv.count(p(), rune);
        int want = 5 * altar.runesPerEssence(99);
        if (runes != want || Inv.count(p(), "pure_essence") != 0) {
            throw failure("5 pure essence at the " + altar.name() + " altar made " + runes + " " + rune + " (expected " + want + "), "
                    + Inv.count(p(), "pure_essence") + " essence left");
        }
        return runes + " " + rune + ", +" + gained(Skill.RUNECRAFT, xp0, (int) Math.round(altar.xp() * 50), "binding") / 10.0 + " xp";
    }

    private String prayer() {
        Bone bone = Bone.byItem("bones");
        if (bone == null) {
            throw failure("bones are not a Bone");
        }
        hold("bones", 2);
        int xp0 = xp(Skill.PRAYER);
        useItem();
        if (Inv.count(p(), "bones") != 1) {
            throw failure("burying left " + Inv.count(p(), "bones") + " of 2 bones");
        }
        int got = gained(Skill.PRAYER, xp0, (int) Math.round(bone.xp() * 10), "burying bones");
        p().getInventory().clear();
        data().setPrayerPoints(1);
        Runnable altar = station(Station.PRAYER_ALTAR);
        altar.run();
        int max = data().skills().level(Skill.PRAYER);
        if (data().prayerPoints() != max) {
            throw failure("praying at an altar left " + data().prayerPoints() + "/" + max + " Prayer points");
        }
        return "buried, +" + got / 10.0 + " xp; altar recharged to " + max;
    }
}
