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
import com.runecraft.platform.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Woodcutting, Mining (incl. Motherlode Mine), Fishing spots, Hunter traps and implings, Firemaking. */
public final class GatheringHooks {
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

    static boolean depleted(ServerLevel world, BlockPos pos) {
        Long until = DEPLETED.get(SkillingUtil.key(world, pos));
        return until != null && ActionManager.gameTick() < until;
    }

    static void deplete(ServerLevel world, BlockPos pos, int ticks) {
        DEPLETED.put(SkillingUtil.key(world, pos), ActionManager.gameTick() + ticks);
    }

    static void init() {
        com.runecraft.platform.ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 30 == 0) {
                ServerLevel world = com.runecraft.world.Gielinor.world(server);
                if (world != null) {
                    roamImplings(world);
                }
            }
        });
        for (GatheringData.ModTree t : GatheringData.TREES) {
            BlockHooks.register(t.blockId(), (player, pos, state) -> chop(player, pos, t));
        }
        for (MiningData.MineRock r : MiningData.ROCKS) {
            BlockHooks.register(r.blockId(), (player, pos, state) -> mine(player, pos, r));
        }
        BlockHooks.register("pay_dirt_hopper", (player, pos, state) -> hopper(player, false));
        BlockHooks.register(MiningData.UPPER_HOPPER, (player, pos, state) -> hopper(player, true));
        BlockHooks.register("pay_dirt_sack", (player, pos, state) -> sack(player));
        BlockHooks.register(MiningData.BARRONITE_CRUSHER, (player, pos, state) -> crushBarronite(player, pos));
        for (GatheringData.Spot s : GatheringData.SPOTS) {
            BlockHooks.register(s.blockId(), (player, pos, state) -> fish(player, pos, s));
        }
        for (HunterData.TrapSpot t : HunterData.TRAPS) {
            BlockHooks.register(t.blockId(), (player, pos, state) -> trap(player, pos, t));
        }
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || !(entity instanceof OsrsMonsterEntity monster)) {
                return InteractionResult.PASS;
            }
            if (world.isClientSide()) {
                ItemStack held = player.getMainHandItem();
                String heldId = held.isEmpty() ? "" : Inv.idOf(held);
                return NETS.contains(heldId) || heldId.equals("magic_box") ? InteractionResult.SUCCESS : InteractionResult.PASS;
            }
            if (monster.stats() == null || monster.stats().profile() == null || !(player instanceof ServerPlayer sp)) {
                return InteractionResult.PASS;
            }
            String id = monster.stats().profile().id;
            if ("imp".equals(id) && SkillingUtil.heldId(sp).equals("magic_box")) {
                catchImp(sp, monster);
                return InteractionResult.SUCCESS;
            }
            HunterData.Impling imp = HunterData.impling(id);
            if (imp == null) {
                return InteractionResult.PASS;
            }
            // Barehanded catches are allowed 10 levels above the impling's; otherwise a net must be held.
            if (!NETS.contains(SkillingUtil.heldId(sp)) && !sp.getMainHandItem().isEmpty()) {
                return InteractionResult.PASS;
            }
            catchImpling(sp, monster, imp);
            return InteractionResult.SUCCESS;
        });
    }

    // ------------------------------------------------------------------ Woodcutting

    private static void chop(ServerPlayer player, BlockPos pos, GatheringData.ModTree tree) {
        if (!Stun.check(player) || !SkillingUtil.need(player, Skill.WOODCUTTING, tree.level(), "chop down this tree")) {
            return;
        }
        ServerLevel world = (ServerLevel) player.level();
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
            boolean roll(ServerPlayer p, RandomSource random) {
                Tools.Tool axe = Tools.bestAxeTool(p, Players.data(p).skills());
                if (axe == null || depleted(world, pos)) {
                    return false;
                }
                world.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.PLAYERS, 1.0f, 0.9f);
                double chance = Chance.lerp(SkillingUtil.level(p, Skill.WOODCUTTING), tree.low(axe.woodcuttingMultiplier()),
                        tree.high(axe.woodcuttingMultiplier()));
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
                        world.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0f, 0.8f);
                        return false;
                    }
                }
                return true;
            }
        });
    }

    // ------------------------------------------------------------------ Mining

    private static void mine(ServerPlayer player, BlockPos pos, MiningData.MineRock rock) {
        if (!Stun.check(player) || !SkillingUtil.need(player, Skill.MINING, rock.level(), "mine this rock")) {
            return;
        }
        ServerLevel world = (ServerLevel) player.level();
        if (depleted(world, pos)) {
            Msg.game(player, "There is currently no ore available in this rock.");
            return;
        }
        ToolTier pick = Tools.bestPickaxe(player, Players.data(player).skills());
        if (pick == null) {
            Msg.game(player, "You need a pickaxe to mine this rock. You do not have a pickaxe which you have the Mining level to use.");
            return;
        }
        if (rock.blockId().equals(MiningData.SULPHUR)) {
            ItemStack head = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
            if (head.isEmpty() || !MiningData.BREATHING_FILTERS.contains(Inv.idOf(head))) {
                Msg.game(player, "You'll choke on the fumes if you try to mine without protection.");
                return;
            }
        }
        boolean barronite = rock.blockId().equals(MiningData.BARRONITE);
        boolean vein = MiningData.isPayDirtVein(rock.blockId());
        boolean upper = rock.blockId().equals(MiningData.UPPER_VEIN);
        boolean inner = rock.blockId().equals(MiningData.INNER_VEIN);
        if (upper && !Motherlode.canUseUpperLevel(Players.data(player).skills().level(Skill.MINING),
                SkillingState.get(player.getServer()).section("sacks").getBoolean(player.getStringUUID() + ":upper"))) {
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
            boolean roll(ServerPlayer p, RandomSource random) {
                if (Tools.bestPickaxe(p, Players.data(p).skills()) == null || depleted(world, pos)) {
                    return false;
                }
                world.playSound(null, pos, SoundEvents.STONE_HIT, SoundSource.PLAYERS, 1.0f, 0.9f);
                MiningData.Yield got = rock.attempt(SkillingUtil.level(p, Skill.MINING), random::nextDouble);
                if (got == null) {
                    return true;
                }
                SkillingUtil.give(p, got.item(), got.amount(random.nextDouble()));
                XpService.add(p, Skill.MINING, got.xp());
                if (vein) {
                    // The pay-dirt's ore is decided now, at the current (boosted) level; the hopper only washes it.
                    CompoundTag sacks = SkillingState.get(p.getServer()).section("sacks");
                    String qk = p.getStringUUID() + ":pending";
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
                if (barronite) {
                    // A barronite rock lasts 20 seconds from its first success, then respawns after a minute.
                    String key = SkillingUtil.key(world, pos);
                    long now = ActionManager.gameTick();
                    Long until = VEIN_EXPIRY.get(key);
                    if (until == null) {
                        VEIN_EXPIRY.put(key, now + MiningData.BARRONITE_LIFE_TICKS);
                    } else if (now >= until) {
                        VEIN_EXPIRY.remove(key);
                        deplete(world, pos, rock.respawnTicks());
                        return false;
                    }
                    return true;
                }
                if (rock.respawnTicks() > 0) {
                    deplete(world, pos, rock.respawnTicks());
                    return false;
                }
                return true;
            }
        });
    }

    /** The Barronite Crusher: crushes the barronite deposits carried with a hammer (14 Smithing, 30 XP each). */
    private static void crushBarronite(ServerPlayer player, BlockPos pos) {
        if (!SkillingUtil.need(player, Skill.SMITHING, MiningData.CRUSH_LEVEL, "crush barronite deposits")) {
            return;
        }
        if (!Inv.has(player, "hammer", 1)) {
            Msg.game(player, "You need a hammer to crush the deposits.");
            return;
        }
        if (!Inv.has(player, "barronite_deposit", 1)) {
            Msg.game(player, "You have no barronite deposits to crush.");
            return;
        }
        Msg.action(player, "You place a deposit in the crusher.");
        SkillingUtil.start(player, new SkillingUtil.BlockAction(pos, MiningData.BARRONITE_CRUSHER, 4) {
            @Override
            boolean roll(ServerPlayer p, RandomSource random) {
                if (!Inv.has(p, "barronite_deposit", 1) || !Inv.has(p, "hammer", 1)) {
                    return false;
                }
                Inv.remove(p, "barronite_deposit", 1);
                Loot got = SkillingUtil.roll(p, MiningData.CRUSH_TABLE, random);
                XpService.add(p, Skill.SMITHING, MiningData.CRUSH_XP);
                p.level().playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.5f, 1.2f);
                if (got != null && !got.item().equals("barronite_shards")) {
                    Msg.red(p, "You find " + Msg.article(SkillingUtil.lower(SkillingUtil.name(got.item()))) + " in the deposit!");
                }
                return Inv.has(p, "barronite_deposit", 1);
            }
        });
    }

    /** True when the pay-dirt carried already fills what's left of the player's sack. */
    private static boolean sackFull(ServerPlayer player) {
        CompoundTag sacks = SkillingState.get(player.getServer()).section("sacks");
        String key = player.getStringUUID();
        return Inv.count(player, "pay_dirt") + sacks.getInt(key) >= Motherlode.sackCapacity(sacks.getBoolean(key + ":upgraded"));
    }

    /** Game tick each water wheel strut breaks (shared by every player, as in OSRS); -1 until first seen. */
    private static final long[] STRUT_BREAKS = {-1, -1};

    /**
     * The water wheel struts. The struts are not separate objects here, so they are repaired at the hopper: holding a
     * hammer repairs a broken strut instead of depositing. While both are broken the wheel stops and nothing is
     * deposited. Returns true when the click was used up. // approx (OSRS: click the strut by the wheel)
     */
    private static boolean struts(ServerPlayer player) {
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
        if (broken > 0 && "hammer".equals(Inv.idOf(player.getMainHandItem()))) {
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

    private static void hopper(ServerPlayer player, boolean upper) {
        CompoundTag sacks = SkillingState.get(player.getServer()).section("sacks");
        String key = player.getStringUUID();
        if (player.isShiftKeyDown()) {
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
        RandomSource random = player.getRandom();
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
        player.level().playSound(null, player.blockPosition(), SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 1f, 0.8f);
    }

    /** Prospector Percy's one-off upgrades (sneak-use the hopper / sack): upper level access and the bigger sack. */
    private static void buyUpperLevel(ServerPlayer player, CompoundTag sacks, String key) {
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

    private static void buyUpperHopper(ServerPlayer player, CompoundTag sacks, String key) {
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

    private static void buyBiggerSack(ServerPlayer player, CompoundTag sacks, String key) {
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

    private static void sack(ServerPlayer player) {
        CompoundTag sacks = SkillingState.get(player.getServer()).section("sacks");
        String key = player.getStringUUID();
        if (player.isShiftKeyDown()) {
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
        RandomSource random = player.getRandom();
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

    private static void fish(ServerPlayer player, BlockPos pos, GatheringData.Spot clicked) {
        if (!Stun.check(player)) {
            return;
        }
        // A spot that takes two nets (Camdozaal): the net held in hand, else whichever net is carried.
        GatheringData.Spot other = com.runecraft.osrs.content.skilling.Camdozaal.otherNet(clicked);
        GatheringData.Spot spot = other != null && hasFishingTool(player, other)
                && (!hasFishingTool(player, clicked) || SkillingUtil.heldId(player).equals(other.tool())) ? other : clicked;
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
        if (spot.blockId().equals(GatheringData.LANTERN_SPOT) && GatheringData.LIT_LANTERNS.stream().noneMatch(l -> Inv.has(player, l, 1))) {
            Msg.game(player, "You need a lit lantern to lure the squid to the surface.");
            return;
        }
        Msg.action(player, spot.tool().contains("net") ? "You cast out your net..." : spot.tool().equals("harpoon")
                ? "You start harpooning fish." : "You cast out your line...");
        SkillingUtil.start(player, new SkillingUtil.BlockAction(pos, spot.blockId(), 5) {
            @Override
            boolean roll(ServerPlayer p, RandomSource random) {
                if (!hasFishingTool(p, spot)) {
                    return false;
                }
                ServerLevel w = (ServerLevel) p.level();
                if (com.runecraft.skill.FishingSpots.tick(w, pos, w.getBlockState(pos)) != null) {
                    Msg.game(p, "The fishing spot has moved.");
                    return false;
                }
                if (spot.bait() != null && !Inv.has(p, spot.bait(), 1)) {
                    Msg.game(p, "You don't have any " + SkillingUtil.lower(SkillingUtil.name(spot.bait())) + " left.");
                    return false;
                }
                p.level().playSound(null, pos, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.PLAYERS, 0.4f, 1.0f);
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
                    if (random.nextDouble() < c.chance(fishing, boost)) {
                        if (spot.bait() != null) {
                            Inv.remove(p, spot.bait(), 1);
                        }
                        boolean minnows = c.item().equals(GatheringData.MINNOW);
                        SkillingUtil.give(p, c.item(), minnows ? GatheringData.minnowsPerCatch(fishing) : 1);
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
    private static boolean hasFishingTool(ServerPlayer player, GatheringData.Spot spot) {
        if (spot.tool().equals("harpoon")) {
            return Tools.bestHarpoon(player, Players.data(player).skills()) != null;
        }
        return Inv.has(player, spot.tool(), 1);
    }

    // ------------------------------------------------------------------ Hunter traps

    private static void trap(ServerPlayer player, BlockPos pos, HunterData.TrapSpot spot) {
        ServerLevel world = (ServerLevel) player.level();
        if (!spot.kind().laid()) {
            track(player, world, pos, spot);
            return;
        }
        String key = SkillingUtil.key(world, pos);
        LaidTrap laid = TRAPS.get(key);
        long now = ActionManager.gameTick();
        if (laid != null) {
            if (!laid.owner().equals(player.getUUID())) {
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
                world.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1f, 1f);
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
        player.swing(InteractionHand.MAIN_HAND, true);
        TRAPS.put(key, new LaidTrap(player.getUUID(), now));
        HunterService.claim(player, world, pos, false);
        Msg.action(player, switch (spot.kind()) {
            case NET -> "You set up the net trap.";
            case DEADFALL -> "You set up the deadfall.";
            case PITFALL -> "You dig a pit and cover it with logs.";
            default -> "You search the area.";
        });
    }

    /** The catch's loot (plus its rare extra, e.g. a tatty fur) and XP. */
    private static void giveCatch(ServerPlayer player, HunterData.TrapSpot spot) {
        for (String item : spot.loot()) {
            SkillingUtil.give(player, item, 1);
        }
        if (spot.rareLoot() != null && spot.rareOneIn() > 0 && player.getRandom().nextInt(spot.rareOneIn()) == 0) {
            SkillingUtil.give(player, spot.rareLoot(), 1);
        }
        XpService.add(player, Skill.HUNTER, spot.xp());
    }

    /**
     * Ring of pursuit (systems module sets this): true when the player wears one, after spending one of its charges. OSRS: it
     * reveals the whole track of a noose-wand creature at once; RuneCraft folds a track into one search, so the ring halves
     * how long the burrow stays empty after a successful catch instead. // approx
     */
    public static java.util.function.Predicate<ServerPlayer> pursuitTrack = p -> false;

    /**
     * Immediate catches: noose-wand tracking (search the burrow for the kebbit or weasel), butterflies
     * (butterfly net, one jar per catch) and falconry (the falcon swoops on the kebbit).
     */
    private static void track(ServerPlayer player, ServerLevel world, BlockPos pos, HunterData.TrapSpot spot) {
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
        if (spot.kind() == HunterData.TrapKind.FALCONRY && !player.getMainHandItem().isEmpty()) {
            Msg.game(player, "You need your hands free to handle the falcon.");
            return;
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        int level = SkillingUtil.level(player, Skill.HUNTER);
        double chance = HunterRates.catchChance(spot.creature(), spot.level(), level);
        int emptyFor = spot.kind() == HunterData.TrapKind.BUTTERFLY ? 4 : 8;
        if (player.getRandom().nextDouble() < chance) {
            if (spot.bait() != null) {
                Inv.remove(player, spot.bait(), 1);
            }
            if (spot.kind() == HunterData.TrapKind.TRACKING && pursuitTrack.test(player)) {
                emptyFor = 4;
            }
            giveCatch(player, spot);
            Msg.action(player, "You've caught " + Msg.article(SkillingUtil.lower(spot.creature())) + "!");
            world.playSound(null, pos, SoundEvents.RABBIT_HURT, SoundSource.NEUTRAL, 0.8f, 1.2f);
        } else {
            Msg.game(player, switch (spot.kind()) {
                case BUTTERFLY -> "You fail to catch the butterfly.";
                case FALCONRY -> "The falcon swoops down on the kebbit, but just misses catching it.";
                default -> "The " + SkillingUtil.lower(spot.creature()) + " escapes your noose.";
            });
        }
        deplete(world, pos, emptyFor);
    }

    // ------------------------------------------------------------------ Implings

    /**
     * Implings never stand still in OSRS: an idle impling flits off to a random spot up to 12 tiles away now and then
     * (the spawner's leash still brings strays back toward their spawn area). // approx distances and timing
     */
    private static void roamImplings(ServerLevel world) {
        for (net.minecraft.world.entity.Entity e : world.getAllEntities()) {
            if (!(e instanceof OsrsMonsterEntity m) || !m.isAlive() || m.getTarget() != null || !m.getNavigation().isDone()
                    || m.stats() == null || m.stats().profile() == null || HunterData.impling(m.stats().profile().id) == null
                    || m.getRandom().nextInt(3) != 0) {
                continue;
            }
            int x = m.getBlockX() + m.getRandom().nextInt(49) - 24;
            int z = m.getBlockZ() + m.getRandom().nextInt(49) - 24;
            if (!world.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            int y = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(y - m.getBlockY()) > 12) {
                continue; // underground (Puro-Puro, dungeons): stay on this floor
            }
            m.getNavigation().moveTo(x + 0.5, y, z + 0.5, 1.0);
        }
    }

    /** Nets that catch implings (the magic one adds 20/256 to the catch chance). */
    private static final java.util.Set<String> NETS = java.util.Set.of("butterfly_net", "magic_butterfly_net");
    /** Jar generator charges, kept in the stack's NBT (a stack without the tag is new and full). */
    private static final String JAR_GENERATOR_NBT = "RcJarCharges";

    private static void catchImpling(ServerPlayer player, OsrsMonsterEntity entity, HunterData.Impling imp) {
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
        player.swing(InteractionHand.MAIN_HAND, true);
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

    static void openJar(ServerPlayer player, ItemStack stack, HunterData.Impling imp) {
        stack.shrink(1);
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
    private static Loot luckyLoot(ServerPlayer player, HunterData.Impling imp) {
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
    static void useJarGenerator(ServerPlayer player, ItemStack stack) {
        CompoundTag nbt = stack.getOrCreateTag();
        int charges = nbt.contains(JAR_GENERATOR_NBT) ? nbt.getInt(JAR_GENERATOR_NBT) : HunterData.JAR_GENERATOR_CHARGES;
        boolean butterfly = player.isShiftKeyDown();
        int cost = butterfly ? HunterData.JAR_GENERATOR_BUTTERFLY_COST : HunterData.JAR_GENERATOR_IMPLING_COST;
        if (charges < cost) {
            Msg.game(player, "Your jar generator doesn't have enough charges left.");
            if (charges <= 0) {
                stack.shrink(1);
            }
            return;
        }
        nbt.putInt(JAR_GENERATOR_NBT, charges - cost);
        SkillingUtil.give(player, butterfly ? "butterfly_jar" : "impling_jar", 1);
        Msg.action(player, "Your jar generator makes " + (butterfly ? "a butterfly jar" : "an impling jar") + " (" + (charges - cost)
                + " charges left).");
        if (charges - cost <= 0) {
            stack.shrink(1);
            Msg.game(player, "Your jar generator crumbles to dust.");
        }
    }

    // ------------------------------------------------------------------ Imps (magic box)

    /**
     * A magic box set beside an imp catches it (wiki: 71 Hunter, 450 XP; a bead adds 3% to the chance). The trap is
     * set and checked at once here rather than left on the ground (approx).
     */
    private static void catchImp(ServerPlayer player, OsrsMonsterEntity imp) {
        if (!SkillingUtil.need(player, Skill.HUNTER, HunterData.IMP_LEVEL, "catch imps")) {
            return;
        }
        player.swing(InteractionHand.MAIN_HAND, true);
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
    static void useImpBox(ServerPlayer player, ItemStack box, InteractionHand hand) {
        String next = HunterData.impBoxAfterUse(Inv.idOf(box));
        InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack item = player.getItemInHand(other);
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
        item.shrink((int) moved);
        box.shrink(1);
        Inv.give(player, next, 1);
        Msg.action(player, next.equals("magic_box") ? "The imp takes your items to the bank and leaves the box."
                : "The imp takes your items to the bank.");
    }

    // ------------------------------------------------------------------ Firemaking

    /** Lights one of this module's logs if the player has one and no built-in logs are preferred. Returns true if handled. */
    static boolean lightModLogs(ServerPlayer player) {
        ItemStack off = player.getOffhandItem();
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
        BlockPos at = player.blockPosition();
        ServerLevel world = (ServerLevel) player.level();
        if (!world.getBlockState(at).isAir() || !ModBlocks.FIRE.defaultBlockState().canSurvive(world, at)) {
            Msg.game(player, "You can't light a fire here.");
            return true;
        }
        GatheringData.ModTree burning = logs;
        Msg.action(player, "You attempt to light the logs.");
        ActionManager.start(player, new Action() {
            @Override
            public boolean tick(ServerPlayer p, long gameTick) {
                if (!Inv.has(p, burning.log(), 1)) {
                    return false;
                }
                p.swing(InteractionHand.MAIN_HAND, true);
                if (p.getRandom().nextDouble() >= Chance.lerp(SkillingUtil.level(p, Skill.FIREMAKING), 64, 512)) {
                    return true;
                }
                BlockPos here = p.blockPosition();
                if (!world.getBlockState(here).isAir()) {
                    return false;
                }
                Inv.remove(p, burning.log(), 1);
                world.setBlockAndUpdate(here, ModBlocks.FIRE.defaultBlockState());
                XpService.add(p, Skill.FIREMAKING, burning.fmXp());
                Msg.action(p, "The fire catches and the logs begin to burn.");
                BlockPos west = here.relative(Direction.WEST);
                BlockState w = world.getBlockState(west);
                if (w.getCollisionShape(world, west).isEmpty()) {
                    p.teleportTo(west.getX() + 0.5, p.getY(), west.getZ() + 0.5);
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
