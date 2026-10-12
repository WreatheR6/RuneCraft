package com.runecraft.content.skilling;

import com.runecraft.action.Action;
import com.runecraft.action.ActionManager;
import com.runecraft.content.BlockHooks;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.Chance;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.content.skilling.GatheringData;
import com.runecraft.osrs.content.skilling.HunterData;
import com.runecraft.osrs.content.skilling.Loot;
import com.runecraft.osrs.content.skilling.MiningData;
import com.runecraft.osrs.data.SpecialTool;
import com.runecraft.osrs.data.ToolTier;
import com.runecraft.osrs.data.Tree;
import com.runecraft.player.Players;
import com.runecraft.registry.ModBlocks;
import com.runecraft.skill.Stun;
import com.runecraft.skill.Tools;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Woodcutting, Mining (incl. Motherlode Mine), Fishing spots, Hunter traps and implings, Firemaking. */
final class GatheringHooks {
    /** Depleted resource positions -> game tick they come back. */
    private static final Map<String, Long> DEPLETED = new HashMap<>();
    /** Laid hunter traps: position key -> (owner, game tick laid). */
    private static final Map<String, LaidTrap> TRAPS = new HashMap<>();

    private record LaidTrap(UUID owner, long laid) {
    }

    private GatheringHooks() {
    }

    static boolean depleted(ServerWorld world, BlockPos pos) {
        Long until = DEPLETED.get(SkillingUtil.key(world, pos));
        return until != null && ActionManager.gameTick() < until;
    }

    static void deplete(ServerWorld world, BlockPos pos, int ticks) {
        DEPLETED.put(SkillingUtil.key(world, pos), ActionManager.gameTick() + ticks);
    }

    static void init() {
        for (GatheringData.ModTree t : GatheringData.TREES) {
            BlockHooks.register(t.blockId(), (player, pos, state) -> chop(player, pos, t));
        }
        for (MiningData.MineRock r : MiningData.ROCKS) {
            BlockHooks.register(r.blockId(), (player, pos, state) -> mine(player, pos, r));
        }
        BlockHooks.register("pay_dirt_hopper", (player, pos, state) -> hopper(player));
        BlockHooks.register("pay_dirt_sack", (player, pos, state) -> sack(player));
        for (GatheringData.Spot s : GatheringData.SPOTS) {
            BlockHooks.register(s.blockId(), (player, pos, state) -> fish(player, pos, s));
        }
        for (HunterData.TrapSpot t : HunterData.TRAPS) {
            BlockHooks.register(t.blockId(), (player, pos, state) -> trap(player, pos, t));
        }
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (hand != Hand.MAIN_HAND || !(entity instanceof OsrsMonsterEntity monster)) {
                return ActionResult.PASS;
            }
            if (world.isClient()) {
                ItemStack held = player.getMainHandStack();
                return !held.isEmpty() && Inv.idOf(held).equals("butterfly_net") ? ActionResult.SUCCESS : ActionResult.PASS;
            }
            if (monster.stats() == null || monster.stats().profile() == null) {
                return ActionResult.PASS;
            }
            HunterData.Impling imp = HunterData.impling(monster.stats().profile().id);
            if (imp == null || !(player instanceof ServerPlayerEntity sp)) {
                return ActionResult.PASS;
            }
            catchImpling(sp, monster, imp);
            return ActionResult.SUCCESS;
        });
    }

    // ------------------------------------------------------------------ Woodcutting

    private static void chop(ServerPlayerEntity player, BlockPos pos, GatheringData.ModTree tree) {
        if (!Stun.check(player) || !SkillingUtil.need(player, Skill.WOODCUTTING, tree.level(), "chop down this tree")) {
            return;
        }
        ServerWorld world = (ServerWorld) player.getWorld();
        if (depleted(world, pos)) {
            Msg.game(player, "This tree has been cut down. It will grow back soon.");
            return;
        }
        SkillSet skills = Players.data(player).skills();
        if (Tools.bestAxe(player, skills) == null) {
            Msg.game(player, "You do not have an axe which you have the woodcutting level to use.");
            return;
        }
        Msg.action(player, tree.blockId().equals("sulliuscep") ? "You swing your axe at the sulliuscep." : "You swing your axe at the tree.");
        SkillingUtil.start(player, new SkillingUtil.BlockAction(pos, tree.blockId(), 4) {
            @Override
            boolean roll(ServerPlayerEntity p, Random random) {
                Tools.Tool axe = Tools.bestAxeTool(p, Players.data(p).skills());
                if (axe == null || depleted(world, pos)) {
                    return false;
                }
                world.playSound(null, pos, SoundEvents.BLOCK_WOOD_HIT, SoundCategory.PLAYERS, 1.0f, 0.9f);
                double chance = Chance.lerp(SkillingUtil.level(p, Skill.WOODCUTTING), tree.low() * axe.woodcuttingMultiplier(),
                        tree.high() * axe.woodcuttingMultiplier());
                if (random.nextDouble() < chance) {
                    SkillingUtil.give(p, tree.log(), 1);
                    XpService.add(p, Skill.WOODCUTTING, tree.xp());
                    Msg.action(p, "You get some " + SkillingUtil.lower(SkillingUtil.name(tree.log())) + ".");
                    if (random.nextInt(256) == 0) {
                        Msg.red(p, "A bird's nest falls out of the tree.");
                        SkillingUtil.give(p, "bird_nest_seeds", 1);
                    }
                    if (random.nextDouble() < tree.depleteChance()) {
                        deplete(world, pos, tree.respawnTicks());
                        world.playSound(null, pos, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 1.0f, 0.8f);
                        return false;
                    }
                }
                return true;
            }
        });
    }

    // ------------------------------------------------------------------ Mining

    private static void mine(ServerPlayerEntity player, BlockPos pos, MiningData.MineRock rock) {
        if (!Stun.check(player) || !SkillingUtil.need(player, Skill.MINING, rock.level(), "mine this rock")) {
            return;
        }
        ServerWorld world = (ServerWorld) player.getWorld();
        if (depleted(world, pos)) {
            Msg.game(player, "There is currently no ore available in this rock.");
            return;
        }
        ToolTier pick = Tools.bestPickaxe(player, Players.data(player).skills());
        if (pick == null) {
            Msg.game(player, "You need a pickaxe to mine this rock. You do not have a pickaxe which you have the Mining level to use.");
            return;
        }
        if (rock.blockId().equals("pay_dirt_vein") && Inv.count(player, "pay_dirt") >= 28 * 4) {
            Msg.game(player, "You should deposit your pay-dirt in the hopper first.");
            return;
        }
        Msg.action(player, "You swing your pickaxe at the rock.");
        SkillingUtil.start(player, new SkillingUtil.BlockAction(pos, rock.blockId(), pick.miningTicks()) {
            @Override
            boolean roll(ServerPlayerEntity p, Random random) {
                if (Tools.bestPickaxe(p, Players.data(p).skills()) == null || depleted(world, pos)) {
                    return false;
                }
                world.playSound(null, pos, SoundEvents.BLOCK_STONE_HIT, SoundCategory.PLAYERS, 1.0f, 0.9f);
                if (random.nextDouble() >= Chance.lerp(SkillingUtil.level(p, Skill.MINING), rock.low(), rock.high())) {
                    return true;
                }
                int total = 0;
                for (MiningData.Yield y : rock.yields()) {
                    total += y.weight();
                }
                int r = random.nextInt(total);
                MiningData.Yield got = rock.yields().get(0);
                for (MiningData.Yield y : rock.yields()) {
                    if (r < y.weight()) {
                        got = y;
                        break;
                    }
                    r -= y.weight();
                }
                SkillingUtil.give(p, got.item(), 1);
                XpService.add(p, Skill.MINING, got.xp());
                Msg.action(p, "You manage to mine some " + SkillingUtil.lower(SkillingUtil.name(got.item())) + ".");
                if (rock.respawnTicks() > 0) {
                    deplete(world, pos, rock.respawnTicks());
                    return false;
                }
                return true;
            }
        });
    }

    private static void hopper(ServerPlayerEntity player) {
        int carried = Inv.count(player, "pay_dirt");
        if (carried == 0) {
            Msg.game(player, "You don't have any pay-dirt to deposit.");
            return;
        }
        NbtCompound sacks = SkillingState.get(player.getServer()).section("sacks");
        String key = player.getUuidAsString();
        int inSack = sacks.getInt(key);
        int room = MiningData.SACK_CAPACITY - inSack;
        if (room <= 0) {
            Msg.game(player, "The sack is full. Collect some ore before depositing more pay-dirt.");
            return;
        }
        int n = Math.min(room, carried);
        Inv.remove(player, "pay_dirt", n);
        sacks.putInt(key, inSack + n);
        Msg.game(player, "You deposit " + n + " pay-dirt into the hopper. The sack now holds " + (inSack + n) + ".");
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_GRAVEL_BREAK, SoundCategory.BLOCKS, 1f, 0.8f);
    }

    private static void sack(ServerPlayerEntity player) {
        NbtCompound sacks = SkillingState.get(player.getServer()).section("sacks");
        String key = player.getUuidAsString();
        int inSack = sacks.getInt(key);
        if (inSack <= 0) {
            Msg.game(player, "The sack is empty.");
            return;
        }
        Random random = player.getRandom();
        int level = SkillingUtil.level(player, Skill.MINING);
        int nuggets = 0;
        Map<String, Integer> ore = new HashMap<>();
        for (int i = 0; i < inSack; i++) {
            Loot l = Loot.pick(MiningData.payDirtTable(level), random.nextInt(Loot.totalWeight(MiningData.payDirtTable(level))));
            ore.merge(l.item(), 1, Integer::sum);
            if (random.nextInt(40) == 0) {
                nuggets++;
            }
        }
        ore.forEach((item, n) -> SkillingUtil.give(player, item, n));
        if (nuggets > 0) {
            SkillingUtil.give(player, "golden_nugget", nuggets);
        }
        sacks.putInt(key, 0);
        Msg.game(player, "You collect your ore from the sack" + (nuggets > 0 ? " and find " + nuggets + " golden nugget"
                + (nuggets > 1 ? "s" : "") : "") + ".");
    }

    // ------------------------------------------------------------------ Fishing

    private static void fish(ServerPlayerEntity player, BlockPos pos, GatheringData.Spot spot) {
        if (!Stun.check(player)) {
            return;
        }
        if (!hasFishingTool(player, spot)) {
            Msg.game(player, "You need " + Msg.article(SkillingUtil.lower(SkillingUtil.name(spot.tool()))) + " to fish here.");
            return;
        }
        int lowest = spot.catches().stream().mapToInt(GatheringData.Catch::level).min().orElse(1);
        if (!SkillingUtil.need(player, Skill.FISHING, lowest, "fish here")) {
            return;
        }
        GatheringData.Catch first = spot.catches().get(spot.catches().size() - 1);
        if (SkillingUtil.level(player, Skill.AGILITY) < first.agilityReq() || SkillingUtil.level(player, Skill.STRENGTH) < first.strengthReq()) {
            Msg.game(player, "You need Agility and Strength levels of " + first.agilityReq() + " to fish here.");
            return;
        }
        if (spot.bait() != null && !Inv.has(player, spot.bait(), 1)) {
            Msg.game(player, "You don't have any " + SkillingUtil.lower(SkillingUtil.name(spot.bait())) + " left.");
            return;
        }
        Msg.action(player, spot.tool().contains("net") ? "You cast out your net..." : spot.tool().equals("harpoon")
                ? "You start harpooning fish." : "You cast out your line...");
        SkillingUtil.start(player, new SkillingUtil.BlockAction(pos, spot.blockId(), 5) {
            @Override
            boolean roll(ServerPlayerEntity p, Random random) {
                if (!hasFishingTool(p, spot)) {
                    return false;
                }
                ServerWorld w = (ServerWorld) p.getWorld();
                if (com.runecraft.skill.FishingSpots.tick(w, pos, w.getBlockState(pos)) != null) {
                    Msg.game(p, "The fishing spot has moved.");
                    return false;
                }
                if (spot.bait() != null && !Inv.has(p, spot.bait(), 1)) {
                    Msg.game(p, "You don't have any " + SkillingUtil.lower(SkillingUtil.name(spot.bait())) + " left.");
                    return false;
                }
                p.getWorld().playSound(null, pos, SoundEvents.ENTITY_FISHING_BOBBER_SPLASH, SoundCategory.PLAYERS, 0.4f, 1.0f);
                if (spot.junkChance() > 0 && random.nextInt(100) < spot.junkChance()) {
                    Loot junk = SkillingUtil.roll(p, spot.junk(), random);
                    if (junk != null) {
                        Msg.action(p, "You catch " + Msg.article(SkillingUtil.lower(SkillingUtil.name(junk.item()))) + ".");
                    }
                    return true;
                }
                int fishing = SkillingUtil.level(p, Skill.FISHING);
                String harpoon = spot.tool().equals("harpoon") ? Tools.bestHarpoon(p, Players.data(p).skills()) : null;
                double boost = harpoon == null ? 1.0 : SpecialTool.harpoonMultiplier(harpoon);
                int agility = SkillingUtil.level(p, Skill.AGILITY);
                int strength = SkillingUtil.level(p, Skill.STRENGTH);
                for (GatheringData.Catch c : spot.catches()) {
                    if (fishing < c.level() || agility < c.agilityReq() || strength < c.strengthReq()) {
                        continue;
                    }
                    if (random.nextDouble() < Chance.lerp(fishing, c.low(), c.high()) * boost) {
                        if (spot.bait() != null) {
                            Inv.remove(p, spot.bait(), 1);
                        }
                        SkillingUtil.give(p, c.item(), 1);
                        XpService.add(p, Skill.FISHING, c.xp());
                        if (c.agilityXp() > 0) {
                            XpService.add(p, Skill.AGILITY, c.agilityXp());
                            XpService.add(p, Skill.STRENGTH, c.strengthXp());
                        }
                        Msg.action(p, "You catch " + Msg.article(SkillingUtil.lower(SkillingUtil.name(c.item()).replace("Raw ", ""))) + ".");
                        return true;
                    }
                }
                return true;
            }
        });
    }

    /** The spot's tool; for harpoon spots any usable harpoon (dragon, infernal, crystal) works. */
    private static boolean hasFishingTool(ServerPlayerEntity player, GatheringData.Spot spot) {
        if (spot.tool().equals("harpoon")) {
            return Tools.bestHarpoon(player, Players.data(player).skills()) != null;
        }
        return Inv.has(player, spot.tool(), 1);
    }

    // ------------------------------------------------------------------ Hunter traps

    private static void trap(ServerPlayerEntity player, BlockPos pos, HunterData.TrapSpot spot) {
        ServerWorld world = (ServerWorld) player.getWorld();
        if (spot.kind() == HunterData.TrapKind.TRACKING) {
            track(player, world, pos, spot);
            return;
        }
        String key = SkillingUtil.key(world, pos);
        LaidTrap laid = TRAPS.get(key);
        long now = ActionManager.gameTick();
        if (laid != null) {
            if (!laid.owner().equals(player.getUuid())) {
                Msg.game(player, "This isn't your trap.");
                return;
            }
            if (now - laid.laid() < spot.catchTicks()) {
                Msg.game(player, "Nothing has been caught yet. Wait a little longer.");
                return;
            }
            TRAPS.remove(key);
            int level = SkillingUtil.level(player, Skill.HUNTER);
            double chance = Math.min(0.95, 0.55 + (level - spot.level()) * 0.02);
            if (player.getRandom().nextDouble() < chance) {
                for (String item : spot.loot()) {
                    SkillingUtil.give(player, item, 1);
                }
                XpService.add(player, Skill.HUNTER, spot.xp());
                Msg.action(player, "You've caught " + Msg.article(SkillingUtil.lower(spot.creature())) + "!");
                world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1f, 1f);
            } else {
                Msg.game(player, "The " + SkillingUtil.lower(spot.creature()) + " escaped from your trap.");
            }
            return;
        }
        if (!SkillingUtil.need(player, Skill.HUNTER, spot.level(), "hunt " + SkillingUtil.lower(spot.creature()) + "s")) {
            return;
        }
        for (String tool : spot.tools()) {
            if (!SkillingUtil.needItem(player, tool, "You need " + Msg.article(SkillingUtil.lower(SkillingUtil.name(tool)))
                    + " to set this trap.")) {
                return;
            }
        }
        if (spot.bait() != null) {
            if (!SkillingUtil.needItem(player, spot.bait(), "You need some " + SkillingUtil.lower(SkillingUtil.name(spot.bait()))
                    + " to set this trap.")) {
                return;
            }
            Inv.remove(player, spot.bait(), 1);
        }
        player.swingHand(Hand.MAIN_HAND, true);
        TRAPS.put(key, new LaidTrap(player.getUuid(), now));
        Msg.action(player, switch (spot.kind()) {
            case NET -> "You set up the net trap.";
            case DEADFALL -> "You set up the deadfall.";
            case PITFALL -> "You dig a pit and cover it with logs.";
            case TRACKING -> "You search the burrow.";
        });
    }

    /** Noose-wand tracking: search the burrow and try to catch the kebbit or weasel hiding in it. */
    private static void track(ServerPlayerEntity player, ServerWorld world, BlockPos pos, HunterData.TrapSpot spot) {
        if (!Stun.check(player) || !SkillingUtil.need(player, Skill.HUNTER, spot.level(), "hunt " + SkillingUtil.lower(spot.creature()) + "s")) {
            return;
        }
        for (String tool : spot.tools()) {
            if (!SkillingUtil.needItem(player, tool, "You need " + Msg.article(SkillingUtil.lower(SkillingUtil.name(tool)))
                    + " to catch " + SkillingUtil.lower(spot.creature()) + "s.")) {
                return;
            }
        }
        if (depleted(world, pos)) {
            Msg.game(player, "The tracks lead nowhere. Wait for another " + SkillingUtil.lower(spot.creature()) + " to come by.");
            return;
        }
        player.swingHand(Hand.MAIN_HAND, true);
        int level = SkillingUtil.level(player, Skill.HUNTER);
        double chance = Math.min(0.95, 0.55 + (level - spot.level()) * 0.02);   // approx.
        if (player.getRandom().nextDouble() < chance) {
            for (String item : spot.loot()) {
                SkillingUtil.give(player, item, 1);
            }
            XpService.add(player, Skill.HUNTER, spot.xp());
            Msg.action(player, "You've caught " + Msg.article(SkillingUtil.lower(spot.creature())) + "!");
            world.playSound(null, pos, SoundEvents.ENTITY_RABBIT_HURT, SoundCategory.NEUTRAL, 0.8f, 1.2f);
        } else {
            Msg.game(player, "The " + SkillingUtil.lower(spot.creature()) + " escapes your noose.");
        }
        deplete(world, pos, 8);
    }

    // ------------------------------------------------------------------ Implings

    private static void catchImpling(ServerPlayerEntity player, OsrsMonsterEntity entity, HunterData.Impling imp) {
        if (!Inv.idOf(player.getMainHandStack()).equals("butterfly_net")) {
            Msg.game(player, "You need to wield a butterfly net to catch implings.");
            return;
        }
        if (!SkillingUtil.need(player, Skill.HUNTER, imp.level(), "catch this impling")) {
            return;
        }
        if (!SkillingUtil.needItem(player, "impling_jar", "You need an empty impling jar to catch implings.")) {
            return;
        }
        player.swingHand(Hand.MAIN_HAND, true);
        int level = SkillingUtil.level(player, Skill.HUNTER);
        double chance = Math.min(0.95, 0.45 + (level - imp.level()) * 0.025);
        if (player.getRandom().nextDouble() >= chance) {
            Msg.game(player, "You fail to catch the impling.");
            return;
        }
        Inv.remove(player, "impling_jar", 1);
        SkillingUtil.give(player, imp.jar(), 1);
        XpService.add(player, Skill.HUNTER, imp.xp());
        Msg.action(player, "You manage to catch the impling and squeeze it into a jar.");
        entity.discard();
    }

    static void openJar(ServerPlayerEntity player, ItemStack stack, HunterData.Impling imp) {
        stack.decrement(1);
        Loot l = SkillingUtil.roll(player, imp.loot(), player.getRandom());
        if (player.getRandom().nextInt(10) != 0) {
            Inv.give(player, "impling_jar", 1);
        } else {
            Msg.game(player, "You break the jar as you take the loot out.");
        }
        if (l != null) {
            Msg.action(player, "You find " + Msg.article(SkillingUtil.lower(SkillingUtil.name(l.item()))) + " in the jar.");
        }
    }

    // ------------------------------------------------------------------ Firemaking

    /** Lights one of this module's logs if the player has one and no built-in logs are preferred. Returns true if handled. */
    static boolean lightModLogs(ServerPlayerEntity player) {
        ItemStack off = player.getOffHandStack();
        GatheringData.ModTree logs = off.isEmpty() ? null : GatheringData.treeByLog(Inv.idOf(off));
        int level = SkillingUtil.level(player, Skill.FIREMAKING);
        if (logs == null) {
            for (Tree t : Tree.values()) {
                if (Inv.has(player, t.logItem(), 1) && level >= t.firemakingLevel()) {
                    return false;   // built-in logs: let the normal tinderbox handler do it
                }
            }
            for (GatheringData.ModTree t : GatheringData.TREES) {
                if (t.fmLevel() > 0 && Inv.has(player, t.log(), 1)) {
                    logs = t;
                }
            }
        }
        if (logs == null) {
            return false;
        }
        if (!SkillingUtil.need(player, Skill.FIREMAKING, logs.fmLevel(), "light these logs")) {
            return true;
        }
        BlockPos at = player.getBlockPos();
        ServerWorld world = (ServerWorld) player.getWorld();
        if (!world.getBlockState(at).isAir() || !ModBlocks.FIRE.getDefaultState().canPlaceAt(world, at)) {
            Msg.game(player, "You can't light a fire here.");
            return true;
        }
        GatheringData.ModTree burning = logs;
        Msg.action(player, "You attempt to light the logs.");
        ActionManager.start(player, new Action() {
            @Override
            public boolean tick(ServerPlayerEntity p, long gameTick) {
                if (!Inv.has(p, burning.log(), 1)) {
                    return false;
                }
                p.swingHand(Hand.MAIN_HAND, true);
                if (p.getRandom().nextDouble() >= Chance.lerp(SkillingUtil.level(p, Skill.FIREMAKING), 65, 513)) {
                    return true;
                }
                BlockPos here = p.getBlockPos();
                if (!world.getBlockState(here).isAir()) {
                    return false;
                }
                Inv.remove(p, burning.log(), 1);
                world.setBlockState(here, ModBlocks.FIRE.getDefaultState());
                XpService.add(p, Skill.FIREMAKING, burning.fmXp());
                Msg.action(p, "The fire catches and the logs begin to burn.");
                BlockPos west = here.offset(Direction.WEST);
                BlockState w = world.getBlockState(west);
                if (w.getCollisionShape(world, west).isEmpty()) {
                    p.requestTeleport(west.getX() + 0.5, p.getY(), west.getZ() + 0.5);
                }
                return false;
            }
        }, true);
        return true;
    }

    /** Checks an item def is one of the throwable kinds (helper for ItemHooks). */
    static boolean isArrowKind(ItemDef def) {
        return def != null && def.kind() == ItemDef.Kind.ARROW;
    }
}
