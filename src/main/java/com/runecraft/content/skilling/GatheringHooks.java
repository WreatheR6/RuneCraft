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
import com.runecraft.osrs.content.skilling.Motherlode;
import com.runecraft.osrs.data.HunterRates;
import com.runecraft.osrs.data.SpecialTool;
import com.runecraft.osrs.data.ToolTier;
import com.runecraft.osrs.data.Tree;
import com.runecraft.player.Players;
import com.runecraft.registry.ModBlocks;
import com.runecraft.skill.HunterService;
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
    /** Motherlode Mine veins mined since they last respawned: position key -> game tick they deplete. */
    private static final Map<String, Long> VEIN_EXPIRY = new HashMap<>();
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
        BlockHooks.register("pay_dirt_hopper", (player, pos, state) -> hopper(player, false));
        BlockHooks.register(MiningData.UPPER_HOPPER, (player, pos, state) -> hopper(player, true));
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
                String heldId = held.isEmpty() ? "" : Inv.idOf(held);
                return NETS.contains(heldId) || heldId.equals("magic_box") ? ActionResult.SUCCESS : ActionResult.PASS;
            }
            if (monster.stats() == null || monster.stats().profile() == null || !(player instanceof ServerPlayerEntity sp)) {
                return ActionResult.PASS;
            }
            String id = monster.stats().profile().id;
            if ("imp".equals(id) && SkillingUtil.heldId(sp).equals("magic_box")) {
                catchImp(sp, monster);
                return ActionResult.SUCCESS;
            }
            HunterData.Impling imp = HunterData.impling(id);
            if (imp == null) {
                return ActionResult.PASS;
            }
            // Barehanded catches are allowed 10 levels above the impling's; otherwise a net must be held.
            if (!NETS.contains(SkillingUtil.heldId(sp)) && !sp.getMainHandStack().isEmpty()) {
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
        boolean vein = MiningData.isPayDirtVein(rock.blockId());
        boolean upper = rock.blockId().equals(MiningData.UPPER_VEIN);
        boolean inner = rock.blockId().equals(MiningData.INNER_VEIN);
        if (upper && !Motherlode.canUseUpperLevel(Players.data(player).skills().level(Skill.MINING),
                SkillingState.get(player.getServer()).section("sacks").getBoolean(player.getUuidAsString() + ":upper"))) {
            Msg.game(player, "You need " + Motherlode.UPPER_LEVEL_MINING + " Mining (unboosted) and Prospector Percy's permission ("
                    + Motherlode.UPPER_LEVEL_NUGGETS + " golden nuggets, sneak-use the hopper) to mine the upper level.");
            return;
        }
        if (vein && sackFull(player)) {
            Msg.game(player, "You have enough pay-dirt to fill your sack. Deposit it in the hopper and empty the sack first.");
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
                MiningData.Yield got = rock.attempt(SkillingUtil.level(p, Skill.MINING), random::nextDouble);
                if (got == null) {
                    return true;
                }
                SkillingUtil.give(p, got.item(), 1);
                XpService.add(p, Skill.MINING, got.xp());
                if (vein) {
                    // The pay-dirt's ore is decided now, at the current (boosted) level; the hopper only washes it.
                    NbtCompound sacks = SkillingState.get(p.getServer()).section("sacks");
                    String qk = p.getUuidAsString() + ":pending";
                    String ore = Motherlode.wash(SkillingUtil.level(p, Skill.MINING), random::nextDouble);
                    String q = sacks.getString(qk) + Motherlode.code(ore);
                    int held = Inv.count(p, "pay_dirt");
                    sacks.putString(qk, q.length() > held ? q.substring(q.length() - held) : q);
                }
                Msg.action(p, "You manage to mine some " + SkillingUtil.lower(SkillingUtil.name(got.item())) + ".");
                if (vein) {
                    // A vein's timer starts with its first pay-dirt; one more can be mined once it has run out.
                    String key = SkillingUtil.key(world, pos);
                    long now = ActionManager.gameTick();
                    Long until = VEIN_EXPIRY.get(key);
                    if (until == null) {
                        VEIN_EXPIRY.put(key, now + Motherlode.veinLife(upper, random.nextDouble()));
                    } else if (now >= until) {
                        VEIN_EXPIRY.remove(key);
                        deplete(world, pos, Motherlode.veinRespawn(inner));
                        Msg.action(p, "The vein is depleted.");
                        return false;
                    }
                    return !sackFull(p);
                }
                if (rock.respawnTicks() > 0) {
                    deplete(world, pos, rock.respawnTicks());
                    return false;
                }
                return true;
            }
        });
    }

    /** True when the pay-dirt carried already fills what's left of the player's sack. */
    private static boolean sackFull(ServerPlayerEntity player) {
        NbtCompound sacks = SkillingState.get(player.getServer()).section("sacks");
        String key = player.getUuidAsString();
        return Inv.count(player, "pay_dirt") + sacks.getInt(key) >= Motherlode.sackCapacity(sacks.getBoolean(key + ":upgraded"));
    }

    /** Game tick each water wheel strut breaks (shared by every player, as in OSRS); -1 until first seen. */
    private static final long[] STRUT_BREAKS = {-1, -1};

    /**
     * The water wheel struts. The struts are not separate objects here, so they are repaired at the hopper: holding a
     * hammer repairs a broken strut instead of depositing. While both are broken the wheel stops and nothing is
     * deposited. Returns true when the click was used up. // approx (OSRS: click the strut by the wheel)
     */
    private static boolean struts(ServerPlayerEntity player) {
        long now = ActionManager.gameTick();
        int broken = 0;
        int first = -1;
        for (int i = 0; i < Motherlode.STRUTS; i++) {
            if (STRUT_BREAKS[i] < 0) {
                STRUT_BREAKS[i] = Motherlode.strutBreaksAt(now, player.getRandom().nextInt(3));
            }
            if (now >= STRUT_BREAKS[i]) {
                broken++;
                first = first < 0 ? i : first;
            }
        }
        if (broken > 0 && "hammer".equals(Inv.idOf(player.getMainHandStack()))) {
            STRUT_BREAKS[first] = Motherlode.strutBreaksAt(now, player.getRandom().nextInt(3));
            XpService.addTenths(player, Skill.SMITHING, Motherlode.strutRepairXpTenths(
                    com.runecraft.player.Players.data(player).skills().currentLevel(Skill.SMITHING)));
            Msg.action(player, "You hammer the broken strut back into place.");
            com.runecraft.content.GameEvents.fireActivity(player, "strut_repair");
            return true;
        }
        if (broken == Motherlode.STRUTS) {
            Msg.game(player, "Both water wheel struts are broken, so the water has stopped. Repair one with a hammer (hold it and use the hopper).");
            return true;
        }
        if (broken > 0) {
            Msg.game(player, "One of the water wheel struts is broken. Hold a hammer and use the hopper to repair it.");
        }
        return false;
    }

    private static void hopper(ServerPlayerEntity player, boolean upper) {
        NbtCompound sacks = SkillingState.get(player.getServer()).section("sacks");
        String key = player.getUuidAsString();
        if (player.isSneaking()) {
            if (upper) {
                buyUpperHopper(player, sacks, key);
            } else {
                buyUpperLevel(player, sacks, key);
            }
            return;
        }
        if (upper && !sacks.getBoolean(key + ":upper_hopper")) {
            Msg.game(player, "Prospector Percy wants " + Motherlode.UPPER_HOPPER_NUGGETS
                    + " golden nuggets before you can use this hopper (sneak-use it to pay).");
            return;
        }
        if (struts(player)) {
            return;
        }
        int carried = Inv.count(player, "pay_dirt");
        if (carried == 0) {
            Msg.game(player, "You don't have any pay-dirt to deposit.");
            return;
        }
        int inSack = sacks.getInt(key);
        boolean upgraded = sacks.getBoolean(key + ":upgraded");
        int n = Motherlode.deposit(carried, inSack, upgraded);
        if (n <= 0) {
            Msg.game(player, "The sack is full. Collect some ore before depositing more pay-dirt.");
            return;
        }
        Inv.remove(player, "pay_dirt", n);
        // The water wheel washes each pay-dirt into the ore (or golden nugget) rolled when it was mined. Pay-dirt
        // with no recorded roll (mined before this was tracked, or bought) rolls at the current level.
        String qk = key + ":pending";
        String pending = sacks.getString(qk);
        int level = SkillingUtil.level(player, Skill.MINING);
        Random random = player.getRandom();
        int nuggets = 0;
        int lost = 0;
        double xp = 0;
        for (int i = 0; i < n; i++) {
            String ore = i < pending.length() ? Motherlode.decode(pending.charAt(i)) : Motherlode.wash(level, random::nextDouble);
            if (ore.equals("golden_nugget") && ++nuggets > Motherlode.MAX_NUGGETS_PER_DEPOSIT) {
                lost++;
                continue;
            }
            xp += Motherlode.collectXp(ore);
            sacks.putInt(key + ":" + ore, sacks.getInt(key + ":" + ore) + 1);
        }
        // Rolls left over belong to pay-dirt still carried; with none left (dropped or banked) they are discarded.
        String rest = pending.length() > n ? pending.substring(n) : "";
        int still = Inv.count(player, "pay_dirt");
        if (rest.length() > still) {
            rest = rest.substring(rest.length() - still);
        }
        sacks.putString(qk, rest);
        // The bonus XP is awarded as the washed ore reaches the sack, not when it is collected.
        if (xp > 0) {
            XpService.add(player, Skill.MINING, xp);
        }
        sacks.putInt(key, inSack + n - lost);
        Msg.game(player, "You deposit " + n + " pay-dirt into the hopper. The sack now holds " + (inSack + n - lost) + "/"
                + Motherlode.sackCapacity(upgraded) + ".");
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_GRAVEL_BREAK, SoundCategory.BLOCKS, 1f, 0.8f);
    }

    /** Prospector Percy's one-off upgrades (sneak-use the hopper / sack): upper level access and the bigger sack. */
    private static void buyUpperLevel(ServerPlayerEntity player, NbtCompound sacks, String key) {
        if (sacks.getBoolean(key + ":upper")) {
            Msg.game(player, "You already have access to the upper level of the mine.");
            return;
        }
        if (Players.data(player).skills().level(Skill.MINING) < Motherlode.UPPER_LEVEL_MINING) {
            Msg.game(player, "You need a Mining level of " + Motherlode.UPPER_LEVEL_MINING + " (unboosted) to use the upper level.");
            return;
        }
        if (!Inv.has(player, "golden_nugget", Motherlode.UPPER_LEVEL_NUGGETS)) {
            Msg.game(player, "Prospector Percy wants " + Motherlode.UPPER_LEVEL_NUGGETS + " golden nuggets for access to the upper level.");
            return;
        }
        Inv.remove(player, "golden_nugget", Motherlode.UPPER_LEVEL_NUGGETS);
        sacks.putBoolean(key + ":upper", true);
        Msg.game(player, "You pay " + Motherlode.UPPER_LEVEL_NUGGETS + " golden nuggets for permanent access to the upper level.");
    }

    private static void buyUpperHopper(ServerPlayerEntity player, NbtCompound sacks, String key) {
        if (sacks.getBoolean(key + ":upper_hopper")) {
            Msg.game(player, "You can already use the upper level's hopper.");
            return;
        }
        if (!sacks.getBoolean(key + ":upper")) {
            Msg.game(player, "You need access to the upper level first (sneak-use the lower hopper).");
            return;
        }
        if (!Inv.has(player, "golden_nugget", Motherlode.UPPER_HOPPER_NUGGETS)) {
            Msg.game(player, "Prospector Percy wants " + Motherlode.UPPER_HOPPER_NUGGETS + " golden nuggets for the upper hopper.");
            return;
        }
        Inv.remove(player, "golden_nugget", Motherlode.UPPER_HOPPER_NUGGETS);
        sacks.putBoolean(key + ":upper_hopper", true);
        Msg.game(player, "You pay " + Motherlode.UPPER_HOPPER_NUGGETS + " golden nuggets: you can now use the upper level's hopper.");
    }

    private static void buyBiggerSack(ServerPlayerEntity player, NbtCompound sacks, String key) {
        if (sacks.getBoolean(key + ":upgraded")) {
            Msg.game(player, "Your sack is already as big as it gets (" + Motherlode.SACK_CAPACITY_UPGRADED + " pay-dirt).");
            return;
        }
        if (!Inv.has(player, "golden_nugget", Motherlode.SACK_UPGRADE_NUGGETS)) {
            Msg.game(player, "Prospector Percy will enlarge your sack for " + Motherlode.SACK_UPGRADE_NUGGETS + " golden nuggets.");
            return;
        }
        Inv.remove(player, "golden_nugget", Motherlode.SACK_UPGRADE_NUGGETS);
        sacks.putBoolean(key + ":upgraded", true);
        Msg.game(player, "Your sack can now hold " + Motherlode.SACK_CAPACITY_UPGRADED + " pay-dirt.");
    }

    private static void sack(ServerPlayerEntity player) {
        NbtCompound sacks = SkillingState.get(player.getServer()).section("sacks");
        String key = player.getUuidAsString();
        if (player.isSneaking()) {
            buyBiggerSack(player, sacks, key);
            return;
        }
        int inSack = sacks.getInt(key);
        if (inSack <= 0) {
            Msg.game(player, "The sack is empty.");
            return;
        }
        int nuggets = 0;
        int washed = 0;
        double xp = 0;
        String[] results = {"golden_nugget", "coal", "gold_ore", "mithril_ore", "adamantite_ore", "runite_ore"};
        for (String item : results) {
            int n = sacks.getInt(key + ":" + item);
            sacks.remove(key + ":" + item);
            if (n <= 0) {
                continue;
            }
            washed += n;
            SkillingUtil.give(player, item, n);
            if (item.equals("golden_nugget")) {
                nuggets = n;
            }
        }
        // Sacks filled before ores were washed at the hopper: wash the rest now.
        Random random = player.getRandom();
        int level = SkillingUtil.level(player, Skill.MINING);
        for (int i = washed; i < inSack; i++) {
            String item = Motherlode.wash(level, random::nextDouble);
            SkillingUtil.give(player, item, 1);
            xp += Motherlode.collectXp(item);
            if (item.equals("golden_nugget")) {
                nuggets++;
            }
        }
        if (xp > 0) {
            XpService.add(player, Skill.MINING, xp);
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
        if (!spot.kind().laid()) {
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
            HunterService.release(world, pos);
            int level = SkillingUtil.level(player, Skill.HUNTER);
            double chance = HunterRates.catchChance(spot.creature(), spot.level(), level);
            if (player.getRandom().nextDouble() < chance) {
                giveCatch(player, spot);
                Msg.action(player, "You've caught " + Msg.article(SkillingUtil.lower(spot.creature())) + "!");
                world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1f, 1f);
            } else {
                Msg.game(player, "The " + SkillingUtil.lower(spot.creature()) + " escaped from your trap.");
            }
            return;
        }
        if (spot.blockId().equals(HunterData.MANIACAL_MONKEY_BOULDER)
                && !com.runecraft.quest.QuestApi.isComplete(player, HunterData.MANIACAL_MONKEY_QUEST)) {
            Msg.game(player, "You need to complete Monkey Madness II to hunt the maniacal monkeys.");
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
        if (HunterService.atTrapLimit(player)) {
            return;
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
        HunterService.claim(player, world, pos, false);
        Msg.action(player, switch (spot.kind()) {
            case NET -> "You set up the net trap.";
            case DEADFALL -> "You set up the deadfall.";
            case PITFALL -> "You dig a pit and cover it with logs.";
            default -> "You search the area.";
        });
    }

    /** The catch's loot (plus its rare extra, e.g. a tatty fur) and XP. */
    private static void giveCatch(ServerPlayerEntity player, HunterData.TrapSpot spot) {
        for (String item : spot.loot()) {
            SkillingUtil.give(player, item, 1);
        }
        if (spot.rareLoot() != null && spot.rareOneIn() > 0 && player.getRandom().nextInt(spot.rareOneIn()) == 0) {
            SkillingUtil.give(player, spot.rareLoot(), 1);
        }
        XpService.add(player, Skill.HUNTER, spot.xp());
    }

    /**
     * Immediate catches: noose-wand tracking (search the burrow for the kebbit or weasel), butterflies
     * (butterfly net, one jar per catch) and falconry (the falcon swoops on the kebbit).
     */
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
            Msg.game(player, spot.kind() == HunterData.TrapKind.TRACKING
                    ? "The tracks lead nowhere. Wait for another " + SkillingUtil.lower(spot.creature()) + " to come by."
                    : "There's nothing here right now. Wait for another " + SkillingUtil.lower(spot.creature()) + " to come by.");
            return;
        }
        if (spot.bait() != null && !SkillingUtil.needItem(player, spot.bait(), "You need " + Msg.article(SkillingUtil.lower(
                SkillingUtil.name(spot.bait()))) + " to catch " + SkillingUtil.lower(spot.creature()) + "s.")) {
            return;
        }
        if (spot.kind() == HunterData.TrapKind.FALCONRY && !player.getMainHandStack().isEmpty()) {
            Msg.game(player, "You need your hands free to handle the falcon.");
            return;
        }
        player.swingHand(Hand.MAIN_HAND, true);
        int level = SkillingUtil.level(player, Skill.HUNTER);
        double chance = HunterRates.catchChance(spot.creature(), spot.level(), level);
        if (player.getRandom().nextDouble() < chance) {
            if (spot.bait() != null) {
                Inv.remove(player, spot.bait(), 1);
            }
            giveCatch(player, spot);
            Msg.action(player, "You've caught " + Msg.article(SkillingUtil.lower(spot.creature())) + "!");
            world.playSound(null, pos, SoundEvents.ENTITY_RABBIT_HURT, SoundCategory.NEUTRAL, 0.8f, 1.2f);
        } else {
            Msg.game(player, switch (spot.kind()) {
                case BUTTERFLY -> "You fail to catch the butterfly.";
                case FALCONRY -> "The falcon swoops down on the kebbit, but just misses catching it.";
                default -> "The " + SkillingUtil.lower(spot.creature()) + " escapes your noose.";
            });
        }
        deplete(world, pos, spot.kind() == HunterData.TrapKind.BUTTERFLY ? 4 : 8);
    }

    // ------------------------------------------------------------------ Implings

    /** Nets that catch implings (the magic one adds 20/256 to the catch chance). */
    private static final java.util.Set<String> NETS = java.util.Set.of("butterfly_net", "magic_butterfly_net");
    /** Jar generator charges, kept in the stack's NBT (a stack without the tag is new and full). */
    private static final String JAR_GENERATOR_NBT = "RcJarCharges";

    private static void catchImpling(ServerPlayerEntity player, OsrsMonsterEntity entity, HunterData.Impling imp) {
        String held = SkillingUtil.heldId(player);
        boolean barehanded = held.isEmpty();
        if (!barehanded && !NETS.contains(held)) {
            Msg.game(player, "You need to wield a butterfly net to catch implings.");
            return;
        }
        int needed = imp.level() + (barehanded ? HunterData.BAREHANDED_EXTRA_LEVELS : 0);
        if (!SkillingUtil.need(player, Skill.HUNTER, needed, barehanded ? "catch this impling barehanded" : "catch this impling")) {
            return;
        }
        if (!SkillingUtil.needItem(player, "impling_jar", "You need an empty impling jar to catch implings.")) {
            return;
        }
        player.swingHand(Hand.MAIN_HAND, true);
        int level = SkillingUtil.level(player, Skill.HUNTER);
        double chance = HunterData.catchChance(imp, level, barehanded || held.equals("magic_butterfly_net"));
        if (player.getRandom().nextDouble() >= chance) {
            Msg.game(player, "You fail to catch the impling.");
            return;
        }
        Inv.remove(player, "impling_jar", 1);
        SkillingUtil.give(player, imp.jar(), 1);
        boolean puro = com.runecraft.osrs.content.dungeons.PuroPuro.contains(entity.getBlockX(), entity.getBlockY(), entity.getBlockZ());
        XpService.add(player, Skill.HUNTER, imp.xpFor(puro));
        Msg.action(player, "You manage to catch the impling and squeeze it into a jar.");
        entity.discard();
    }

    static void openJar(ServerPlayerEntity player, ItemStack stack, HunterData.Impling imp) {
        stack.decrement(1);
        Loot l;
        if (imp.id().equals(HunterData.LUCKY)) {
            l = luckyLoot(player, imp);
        } else {
            l = SkillingUtil.roll(player, imp.loot(), player.getRandom());
        }
        if (player.getRandom().nextInt(10) != 0) {
            Inv.give(player, "impling_jar", 1);
        } else {
            Msg.game(player, "You break the jar as you take the loot out.");
        }
        if (l != null) {
            Msg.action(player, "You find " + Msg.article(SkillingUtil.lower(SkillingUtil.name(l.item()))) + " in the jar.");
        }
    }

    /** A lucky impling jar: one roll on a random clue tier's reward table (its loot lines name the tiers' caskets). */
    private static Loot luckyLoot(ServerPlayerEntity player, HunterData.Impling imp) {
        Loot tierLine = Loot.pick(imp.loot(), player.getRandom().nextInt(Loot.totalWeight(imp.loot())));
        com.runecraft.osrs.content.systems.ClueScrolls.Tier tier = com.runecraft.osrs.content.systems.ClueScrolls.Tier.byCasket(tierLine.item());
        if (tier == null) {
            return null;
        }
        com.runecraft.osrs.content.systems.ClueRewards.Reward r = com.runecraft.osrs.content.systems.ClueRewards.singleRoll(tier,
                new java.util.Random(player.getRandom().nextLong()));
        SkillingUtil.give(player, r.item(), r.count());
        return Loot.of(r.item(), r.count(), r.count(), 1);
    }

    /** Jar generator: an impling jar for 3 charges (sneak: a butterfly jar for 1). */
    static void useJarGenerator(ServerPlayerEntity player, ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        int charges = nbt.contains(JAR_GENERATOR_NBT) ? nbt.getInt(JAR_GENERATOR_NBT) : HunterData.JAR_GENERATOR_CHARGES;
        boolean butterfly = player.isSneaking();
        int cost = butterfly ? HunterData.JAR_GENERATOR_BUTTERFLY_COST : HunterData.JAR_GENERATOR_IMPLING_COST;
        if (charges < cost) {
            Msg.game(player, "Your jar generator doesn't have enough charges left.");
            if (charges <= 0) {
                stack.decrement(1);
            }
            return;
        }
        nbt.putInt(JAR_GENERATOR_NBT, charges - cost);
        SkillingUtil.give(player, butterfly ? "butterfly_jar" : "impling_jar", 1);
        Msg.action(player, "Your jar generator makes " + (butterfly ? "a butterfly jar" : "an impling jar") + " (" + (charges - cost)
                + " charges left).");
        if (charges - cost <= 0) {
            stack.decrement(1);
            Msg.game(player, "Your jar generator crumbles to dust.");
        }
    }

    // ------------------------------------------------------------------ Imps (magic box)

    /**
     * A magic box set beside an imp catches it (wiki: 71 Hunter, 450 XP; a bead adds 3% to the chance). The trap is
     * set and checked at once here rather than left on the ground (approx).
     */
    private static void catchImp(ServerPlayerEntity player, OsrsMonsterEntity imp) {
        if (!SkillingUtil.need(player, Skill.HUNTER, HunterData.IMP_LEVEL, "catch imps")) {
            return;
        }
        player.swingHand(Hand.MAIN_HAND, true);
        int level = SkillingUtil.level(player, Skill.HUNTER);
        String bead = null;
        for (String b : new String[]{"red_bead", "yellow_bead", "black_bead", "white_bead"}) {
            if (Inv.has(player, b, 1)) {
                bead = b;
                break;
            }
        }
        double chance = HunterRates.catchChance("imp", HunterData.IMP_LEVEL, level) + (bead != null ? HunterData.IMP_BEAD_BONUS : 0);
        if (player.getRandom().nextDouble() >= chance) {
            Msg.game(player, "The imp dodges round your magic box.");
            return;
        }
        if (bead != null) {
            Inv.remove(player, bead, 1);
        }
        Inv.remove(player, "magic_box", 1);
        SkillingUtil.give(player, "imp_in_a_box_2", 1);
        XpService.add(player, Skill.HUNTER, HunterData.IMP_XP);
        Msg.action(player, "You catch the imp in your magic box.");
        imp.discard();
    }

    /** Imp-in-a-box: the imp takes the item stack in your other hand to your bank. */
    static void useImpBox(ServerPlayerEntity player, ItemStack box, Hand hand) {
        String next = HunterData.impBoxAfterUse(Inv.idOf(box));
        Hand other = hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
        ItemStack item = player.getStackInHand(other);
        if (next == null || item.isEmpty()) {
            Msg.game(player, "Hold the item for the imp to bank in your other hand.");
            return;
        }
        com.runecraft.osrs.bank.BankModel bank = Players.data(player).bank();
        long moved = com.runecraft.bank.BankService.depositStack(bank, item, item.getCount(), 0);
        if (moved <= 0) {
            Msg.game(player, "Your bank is full.");
            return;
        }
        item.decrement((int) moved);
        box.decrement(1);
        Inv.give(player, next, 1);
        Msg.action(player, next.equals("magic_box") ? "The imp takes your items to the bank and leaves the box."
                : "The imp takes your items to the bank.");
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
                if (p.getRandom().nextDouble() >= Chance.lerp(SkillingUtil.level(p, Skill.FIREMAKING), 64, 512)) {
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
