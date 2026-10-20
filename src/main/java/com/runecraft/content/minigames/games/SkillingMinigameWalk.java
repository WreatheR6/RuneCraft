package com.runecraft.content.minigames.games;

import com.runecraft.action.ActionManager;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.content.minigames.RewardShopService;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.GiantsFoundry;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameRules;
import com.runecraft.osrs.content.minigames.RewardShop;
import com.runecraft.osrs.content.minigames.RewardShops;
import com.runecraft.osrs.content.minigames.SkillingOutfits;
import com.runecraft.player.Players;
import com.runecraft.selftest.ChunkSettle;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Real-player self-test walks of the skilling minigames (round 40; {@code com.runecraft.selftest} "minigame:foundry_round",
 * ... in real-player mode, since minigame sessions only see players in the server's player list). Each plays one round
 * through the blocks a player clicks ({@link BlockHooks}), ticking the session and the player's actions by hand, checks the
 * rewards against the wiki formulas, then spends the points in the minigame's reward shop; any broken step throws an
 * {@link AssertionError} naming it.
 */
public final class SkillingMinigameWalk {
    private final ServerPlayer player;
    private final ServerLevel world;
    private final List<String> notes = new ArrayList<>();
    private String step = "start";
    private MinigameSession session;
    private int ticks;

    private SkillingMinigameWalk(ServerPlayer player) {
        this.player = player;
        this.world = (ServerLevel) player.level();
    }

    private interface Body {
        void run(SkillingMinigameWalk w);
    }

    private static String run(ServerPlayer p, Body body) {
        SkillingMinigameWalk w = new SkillingMinigameWalk(p);
        try {
            body.run(w);
            return w.ticks + " game ticks: " + String.join("; ", w.notes);
        } catch (AssertionError e) {
            throw new AssertionError("[" + w.step + "] " + e.getMessage() + " (after " + w.ticks + " ticks; done: "
                    + String.join("; ", w.notes) + ")");
        } finally {
            ActionManager.cancel(p);
            p.setShiftKeyDown(false);
            if (w.session != null && !w.session.isEnded()) {
                w.session.end(false);
            }
        }
    }

    public static String giantsFoundry(ServerPlayer p) {
        return run(p, SkillingMinigameWalk::foundryRound);
    }

    public static String titheFarm(ServerPlayer p) {
        return run(p, SkillingMinigameWalk::titheRound);
    }

    public static String mahoganyHomes(ServerPlayer p) {
        return run(p, SkillingMinigameWalk::mahoganyRound);
    }

    // ------------------------------------------------------------------ helpers

    private static void expect(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
    }

    private String blockId(BlockPos pos) {
        return BuiltInRegistries.BLOCK.getKey(world.getBlockState(pos).getBlock()).getPath();
    }

    /** Clicks a block as a player would, after walking next to it; checks the block is there. */
    private void use(BlockPos pos, String id) {
        expect(id.equals(blockId(pos)), "no " + id + " at " + pos + " (found " + blockId(pos) + ")");
        moveTo(pos.east());
        BlockHooks.use(player, id, pos, world.getBlockState(pos));
    }

    private void moveTo(BlockPos stand) {
        player.teleportTo(world, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, player.getYRot(), 0);
        player.fallDistance = 0;
    }

    /** One game tick: the player's action, then the session; the player is kept alive. */
    private void tick() {
        for (int m = 0; m < ActionManager.MC_TICKS_PER_GAME_TICK; m++) {
            ActionManager.advanceForSelfTest();
        }
        ActionManager.tickNow(player);
        if (session != null && !session.isEnded()) {
            MinigameManager.selfTestTick(session);
        }
        if (player.getHealth() < player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        ticks++;
    }

    /** Ticks until {@code done} holds, at most {@code max} ticks; false when it never did. */
    private boolean until(int max, BooleanSupplier done) {
        for (int i = 0; i < max; i++) {
            if (done.getAsBoolean()) {
                return true;
            }
            tick();
        }
        return done.getAsBoolean();
    }

    private double xp(Skill s) {
        return Players.data(player).skills().xpTenths(s) / 10.0;
    }

    private int points(String key) {
        return MinigameData.of(player).points(player.getUUID(), key);
    }

    /** Selects the hotbar slot holding an item (moving it into the hotbar if needed), as a player picks it up to use it. */
    private void hold(String id) {
        net.minecraft.world.entity.player.Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (Inv.idOf(inv.getItem(i)).equals(id)) {
                if (i > 8) {
                    net.minecraft.world.item.ItemStack swap = inv.getItem(8);
                    inv.setItem(8, inv.getItem(i));
                    inv.setItem(i, swap);
                    i = 8;
                }
                inv.selected = i;
                return;
            }
        }
        throw new AssertionError("no " + id + " to hold");
    }

    private MinigameSession start(Minigame game) {
        session = MinigameManager.selfTestStart(game, game.entranceBlock(), player);
        expect(session != null && MinigameManager.session(player) == session, game.displayName() + " didn't start a session");
        ChunkSettle.settle(world, session.box());
        return session;
    }

    private static int entry(String shop, String item) {
        List<RewardShop.Entry> entries = RewardShops.get(shop).entries();
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).item().equals(item)) {
                return i;
            }
        }
        throw new AssertionError("the " + shop + " shop has no " + item);
    }

    /** Buys one of an item; checks the points it cost and returns them. */
    private int buy(String shop, String key, String item, int cost) {
        int before = points(key);
        RewardShopService.buy(player, shop, entry(shop, item), 1);
        int spent = before - points(key);
        expect(spent == cost, "buying " + item + " cost " + spent + " points, not " + cost);
        return spent;
    }

    // ------------------------------------------------------------------ Giants' Foundry

    private void foundryRound() {
        step = "commission";
        player.getInventory().clearContent();
        GiantsFoundrySession gf = (GiantsFoundrySession) start(Minigame.GIANTS_FOUNDRY);
        use(gf.selfTestBlock("foundry_mould"), "foundry_mould");
        expect(gf.selfTestView().commission() == null && gf.selfTestView().mouldScore() == 0, "the mould jig worked without a commission");
        use(gf.selfTestBlock("foundry_handin"), "foundry_handin");
        GiantsFoundry.Commission commission = gf.selfTestView().commission();
        expect(commission != null, "Kovac gave no commission");

        step = "mould";
        use(gf.selfTestBlock("foundry_mould"), "foundry_mould");
        int mould = gf.selfTestView().mouldScore();
        int best = GiantsFoundry.mouldScore(commission, GiantsFoundry.bestMoulds(commission, m -> !m.purchasable()));
        expect(mould == best, "the jig set moulds scoring " + mould + ", the best default moulds score " + best);
        notes.add("commission " + commission.display() + ", moulds " + mould);

        step = "crucible";
        Inv.give(player, "adamantite_bar", 14);
        Inv.give(player, "runite_bar", 13);
        Inv.give(player, "rune_scimitar", 1); // a scimitar is worth one bar (two to smith, minus one)
        use(gf.selfTestBlock("foundry_crucible"), "foundry_crucible");
        GiantsFoundrySession.View v = gf.selfTestView();
        expect(v.barsIn() == GiantsFoundry.CRUCIBLE && v.metalScore() == 130, "14 adamantite and 14 rune bars' worth gave "
                + v.barsIn() + " bars and metal score " + v.metalScore() + " (wiki: 130)");
        expect(!Inv.has(player, "adamantite_bar", 1) && !Inv.has(player, "runite_bar", 1) && !Inv.has(player, "rune_scimitar", 1),
                "the crucible left metal behind");
        Inv.give(player, "rune_platebody", 1);
        use(gf.selfTestBlock("foundry_crucible"), "foundry_crucible");
        expect(Inv.has(player, "rune_platebody", 1) && gf.selfTestView().barsIn() == GiantsFoundry.CRUCIBLE,
                "a full crucible took a platebody");
        notes.add("crucible 28 bars (13 rune bars, a rune scimitar, 14 adamantite), metal score 130");

        step = "pour";
        use(gf.selfTestBlock("foundry_mould"), "foundry_mould");
        v = gf.selfTestView();
        expect(v.poured() && v.sections() == 7 && v.quality() == 130 + mould, "pouring: poured " + v.poured() + ", "
                + v.sections() + " sections, quality " + v.quality());
        int start = v.quality();

        step = "mistake";
        GiantsFoundry.Tool wrong = null;
        for (GiantsFoundry.Tool t : GiantsFoundry.Tool.values()) {
            if (t != v.tool()) {
                wrong = t;
            }
        }
        use(gf.selfTestBlock(wrong.block()), wrong.block());
        until(wrong.cycle() + 1, () -> !ActionManager.isBusy(player));
        expect(gf.selfTestView().quality() == start - GiantsFoundry.MISTAKE, "working the " + wrong.display() + " on a "
                + v.tool().display() + " section left quality " + gf.selfTestView().quality());

        step = "refine";
        double smithing = xp(Skill.SMITHING);
        int caught = 0;
        int guard = 0;
        while (gf.selfTestView().progress() < GiantsFoundry.MAX && guard++ < 200) {
            GiantsFoundrySession.View now = gf.selfTestView();
            GiantsFoundry.Tool tool = now.tool();
            expect(tool != null, "no tool for an unfinished sword (quality " + now.quality() + ")");
            int[] r = tool.heat().range(130);
            boolean coolsWhileWorking = tool.heatChange() < 0;
            int lo = coolsWhileWorking ? r[1] - 45 : r[0] + 8;
            int hi = coolsWhileWorking ? r[1] - 8 : r[0] + 45;
            bringHeat(gf, lo, hi);
            int q = gf.selfTestView().quality();
            use(gf.selfTestBlock(tool.block()), tool.block());
            expect(ActionManager.isBusy(player), "clicking the " + tool.display() + " didn't start working it");
            for (int i = 0; i < 400 && ActionManager.isBusy(player); i++) {
                if (gf.selfTestView().bonusOpen()) {
                    use(gf.selfTestBlock(tool.block()), tool.block());
                    caught++;
                }
                int heat = gf.selfTestView().heat();
                // Step away before the next action would leave the heat range (as a player watching the bar does).
                if (coolsWhileWorking ? heat + tool.heatChange() - tool.cycle() < r[0] + 3 : heat + tool.heatChange() > r[1] - 3) {
                    ActionManager.cancel(player);
                    break;
                }
                tick();
            }
            expect(gf.selfTestView().quality() >= q, "working the " + tool.display() + " in range lost quality: "
                    + q + " -> " + gf.selfTestView().quality() + " (heat " + gf.selfTestView().heat() + ")");
        }
        v = gf.selfTestView();
        expect(v.progress() >= GiantsFoundry.MAX, "the sword wasn't finished after 200 rounds (" + v.progress() + ")");
        expect(caught >= 1 && v.quality() == start, "sweet spots caught " + caught + ", quality " + v.quality() + " of " + start
                + " (a caught sweet spot restores lost quality)");
        expect(xp(Skill.SMITHING) > smithing, "the machines gave no Smithing XP");
        notes.add("7 sections refined, " + caught + " sweet spots, the mistake repaired (quality " + v.quality() + ")");

        step = "hand in";
        double before = xp(Skill.SMITHING);
        int coins = Inv.count(player, "coins");
        int rep = points("giants_foundry");
        int kc = MinigameData.of(player).stat(player.getUUID(), "kc:giants_foundry");
        use(gf.selfTestBlock("foundry_handin"), "foundry_handin");
        int q = v.quality();
        expect(Math.abs(xp(Skill.SMITHING) - before - GiantsFoundry.xp(q)) < 0.01 || xp(Skill.SMITHING) >= 200_000_000,
                "a quality-" + q + " sword gave " + (xp(Skill.SMITHING) - before) + " XP, not " + GiantsFoundry.xp(q));
        expect(Inv.count(player, "coins") - coins == GiantsFoundry.coins(q), "coins " + (Inv.count(player, "coins") - coins)
                + ", not " + GiantsFoundry.coins(q));
        expect(points("giants_foundry") - rep == q, "reputation " + (points("giants_foundry") - rep) + ", not " + q);
        expect(MinigameData.of(player).stat(player.getUUID(), "kc:giants_foundry") == kc + 1, "the sword count didn't go up");
        expect(gf.selfTestView().commission() == null && !gf.selfTestView().poured(), "the commission wasn't cleared");
        notes.add("handed in: " + GiantsFoundry.xp(q) + " XP, " + GiantsFoundry.coins(q) + " coins, " + q + " reputation");

        step = "shop";
        MinigameData.of(player).addPoints(player.getUUID(), "giants_foundry", 5000);
        GiantsFoundry.Mould point = GiantsFoundry.mould("the_point");
        buy("giants_foundry", "giants_foundry", point.flag(), point.cost());
        expect(MinigameData.of(player).flag(player.getUUID(), point.flag()), "buying The Point! didn't unlock it");
        buy("giants_foundry", "giants_foundry", GiantsFoundry.ORE_PACK_ITEM, 200);
        buy("giants_foundry", "giants_foundry", "smiths_tunic", 4000);
        expect(Inv.has(player, "smiths_tunic", 1), "the smiths tunic wasn't handed over");
        int left = points("giants_foundry");
        RewardShopService.buy(player, "giants_foundry", entry("giants_foundry", "colossal_blade"), 1);
        expect(points("giants_foundry") == left && !Inv.has(player, "colossal_blade", 1), "the colossal blade was sold for " + left);
        // A Flat commission now takes The Point! (22 Flat) for its tip.
        GiantsFoundry.Commission flat = new GiantsFoundry.Commission(GiantsFoundry.Style.NARROW, GiantsFoundry.Style.FLAT);
        GiantsFoundry.Mould[] withPoint = GiantsFoundry.bestMoulds(flat, m -> !m.purchasable() || m == point);
        expect(withPoint[GiantsFoundry.Part.TIP.ordinal()] == point || withPoint[GiantsFoundry.Part.TIP.ordinal()].score(flat) >= point.score(flat),
                "the best tip for Narrow Flat ignores The Point!");
        notes.add("bought The Point! mould, an ore pack and the smiths tunic; the colossal blade was refused at " + left);
    }

    // ------------------------------------------------------------------ Tithe Farm

    private void titheRound() {
        step = "seeds";
        player.getInventory().clearContent();
        Inv.give(player, "seed_dibber", 1);
        Inv.give(player, "watering_can", 4);
        TitheFarmSession tf = (TitheFarmSession) start(Minigame.TITHE_FARM);
        expect(Inv.has(player, "gricollers_fertiliser", 1), "entering the farm handed out no Gricoller's fertiliser");
        expect(tf.selfTestWater(player) == 4 * MinigameRules.TitheFarm.CAN_DOSES, "4 full watering cans hold "
                + tf.selfTestWater(player) + " doses, not 32");
        use(tf.selfTestBlock("tithe_seed_table"), "tithe_seed_table");
        expect(Inv.count(player, "logavano_seed") == MinigameRules.TitheFarm.SEEDS_PER_TAKE, "a level-99 farmer took "
                + Inv.count(player, "logavano_seed") + " logavano seeds");

        step = "plant";
        int patches = TitheFarmSession.selfTestPatches();
        int dead = patches - 1;
        int fert = 0;
        double farming = xp(Skill.FARMING);
        for (int i = 0; i < patches; i++) {
            use(tf.selfTestPatch(i), "tithe_patch");
            expect(tf.selfTestState(i).type() == 2 && tf.selfTestState(i).stage() == 0, "patch " + i + " wasn't planted");
        }
        expect(xp(Skill.FARMING) == farming, "planting gave Farming XP (OSRS gives none)");
        // Gricoller's fertiliser on the first plant: held in hand and used on the plant.
        hold("gricollers_fertiliser");
        use(tf.selfTestPatch(fert), "tithe_patch");
        expect(tf.selfTestState(fert).fertilised(), "the fertiliser didn't take");
        expect(Inv.has(player, "gricollers_fertiliser", 1), "the fertiliser was used up (Gricoller's bucket never runs out)");
        player.getInventory().selected = 8;
        notes.add(patches + " logavano planted (no XP), one fertilised");

        step = "grow";
        int refills = 0;
        int fertGrownAt = -1;
        for (int t = 0; t < 320; t++) {
            boolean allDone = true;
            for (int i = 0; i < patches; i++) {
                TitheFarmSession.Patch s = tf.selfTestState(i);
                if (i == dead) {
                    continue;
                }
                if (s.stage() == -2) {
                    throw new AssertionError("watered patch " + i + " died");
                }
                if (s.stage() < MinigameRules.TitheFarm.WATERINGS) {
                    allDone = false;
                    if (!s.watered()) {
                        if (tf.selfTestWater(player) == 0) {
                            use(tf.selfTestBlock("tithe_water_barrel"), "tithe_water_barrel");
                            refills++;
                        }
                        use(tf.selfTestPatch(i), "tithe_patch");
                        expect(tf.selfTestState(i).watered(), "watering patch " + i + " failed (" + tf.selfTestWater(player) + " doses)");
                    }
                }
            }
            if (fertGrownAt < 0 && tf.selfTestState(fert).stage() == MinigameRules.TitheFarm.WATERINGS) {
                fertGrownAt = t;
            }
            if (allDone) {
                break;
            }
            tick();
        }
        for (int i = 0; i < patches; i++) {
            expect(i == dead || tf.selfTestState(i).stage() == MinigameRules.TitheFarm.WATERINGS, "patch " + i + " didn't grow in 320 ticks "
                    + "(stage " + tf.selfTestState(i).stage() + ")");
        }
        expect(tf.selfTestState(dead).stage() == -2, "the unwatered plant didn't die");
        expect(fertGrownAt >= 3 * MinigameRules.TitheFarm.FERTILISED_GROW_TICKS - 2 && fertGrownAt < 3 * MinigameRules.TitheFarm.GROW_TICKS - 50,
                "the fertilised plant grew in " + fertGrownAt + " ticks (half of 300 expected)");
        expect(refills >= 1, "57 waterings never needed the barrel with 32 doses");
        notes.add("watered 3 times each (" + refills + " barrel refills), the fertilised plant grew in " + fertGrownAt
                + " ticks, the dry one died");

        step = "harvest";
        farming = xp(Skill.FARMING);
        for (int i = 0; i < patches; i++) {
            use(tf.selfTestPatch(i), "tithe_patch");
            expect(tf.selfTestState(i).type() == -1, "patch " + i + " wasn't harvested or cleared");
        }
        int fruit = Inv.count(player, "logavano_fruit");
        expect(fruit == patches - 1, "harvested " + fruit + " fruit from " + (patches - 1) + " grown plants (one each)");
        expect(Math.abs(xp(Skill.FARMING) - farming - fruit * 23.0) < 0.05, "harvest XP " + (xp(Skill.FARMING) - farming)
                + ", not 23 per fruit");

        step = "sack";
        farming = xp(Skill.FARMING);
        int pts = points("tithe_farm");
        use(tf.selfTestBlock("tithe_sack"), "tithe_sack");
        expect(points("tithe_farm") - pts == MinigameRules.TitheFarm.points(fruit), "depositing " + fruit + " fruit gave "
                + (points("tithe_farm") - pts) + " points");
        expect(Math.abs(xp(Skill.FARMING) - farming - fruit * 230.0) < 0.05, "deposit XP " + (xp(Skill.FARMING) - farming));
        // The rest of a 100-fruit batch (given, not grown): the 75th fruit's bonus, double XP from it on, 2 points for the 100th.
        Inv.give(player, "logavano_fruit", 100);
        farming = xp(Skill.FARMING);
        use(tf.selfTestBlock("tithe_sack"), "tithe_sack");
        int rest = 100 - fruit;
        double want = 0;
        for (int n = fruit + 1; n <= 100; n++) {
            want += MinigameRules.TitheFarm.depositXp(2, n);
        }
        expect(points("tithe_farm") - pts == 35, "a full batch of 100 gave " + (points("tithe_farm") - pts) + " points, not 35");
        expect(Math.abs(xp(Skill.FARMING) - farming - want) < 0.05, "the rest of the batch gave " + (xp(Skill.FARMING) - farming)
                + " XP, not " + want);
        expect(Inv.count(player, "logavano_fruit") == 100 - rest && tf.selfTestBatch(player) == 0,
                "the sack took " + (100 - Inv.count(player, "logavano_fruit")) + " of " + 100 + " (batch now " + tf.selfTestBatch(player) + ")");
        notes.add("a batch of 100: 35 points, " + Math.round(want + fruit * 230.0) + " deposit XP with the 5,750 bonus");

        step = "shop";
        MinigameData.of(player).addPoints(player.getUUID(), "tithe_farm", 1000);
        for (String piece : SkillingOutfits.FARMER) {
            RewardShop.Entry e = RewardShops.get("tithe_farm").entries().get(entry("tithe_farm", piece));
            buy("tithe_farm", "tithe_farm", piece, e.costs().get(0).amount());
            expect(Inv.has(player, piece, 1), piece + " wasn't handed over");
        }
        buy("tithe_farm", "tithe_farm", "gricollers_can", 200);
        buy("tithe_farm", "tithe_farm", "seed_pack", 30);
        expect(Inv.has(player, "seed_pack", 1) && Inv.has(player, "gricollers_can", 1), "the seed pack or the can wasn't handed over");
        // The full farmer's outfit adds 2.5% to deposit XP.
        net.minecraft.world.entity.EquipmentSlot[] slots = {net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET};
        for (int i = 0; i < 4; i++) {
            Inv.remove(player, SkillingOutfits.FARMER.get(i), 1);
            player.setItemSlot(slots[i], Inv.stack(SkillingOutfits.FARMER.get(i), 1));
        }
        farming = xp(Skill.FARMING);
        Inv.remove(player, "logavano_fruit", Inv.count(player, "logavano_fruit"));
        Inv.give(player, "logavano_fruit", 3);
        use(tf.selfTestBlock("tithe_sack"), "tithe_sack");
        double boosted = 3 * SkillingOutfits.boosted(230, 1.025);
        expect(Math.abs(xp(Skill.FARMING) - farming - boosted) < 0.05, "3 fruit in the full farmer's outfit gave "
                + (xp(Skill.FARMING) - farming) + " XP, not " + boosted);
        for (net.minecraft.world.entity.EquipmentSlot s : slots) {
            player.setItemSlot(s, net.minecraft.world.item.ItemStack.EMPTY);
        }
        notes.add("bought the farmer's outfit, Gricoller's can and a seed pack; the outfit adds 2.5% to deposits");

        step = "leave";
        use(tf.selfTestBlock("minigame_exit"), "minigame_exit");
        expect(MinigameManager.session(player) == null, "the exit didn't take the player out");
        expect(!Inv.has(player, "logavano_seed", 1) && !Inv.has(player, "gricollers_fertiliser", 1), "seeds or fertiliser left the farm");
        notes.add("left: seeds and fertiliser stayed behind");
    }

    // ------------------------------------------------------------------ Mahogany Homes

    private void mahoganyRound() {
        step = "contract";
        player.getInventory().clearContent();
        Inv.give(player, "hammer", 1);
        Inv.give(player, "amys_saw", 1);
        Inv.give(player, "mahogany_plank", 10);
        Inv.give(player, "steel_bar", 2);
        // Ten more planks in the plank sack: using it with planks carried fills it.
        Inv.give(player, "plank_sack", 1);
        net.minecraft.world.item.ItemStack sack = null;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (Inv.idOf(player.getInventory().getItem(i)).equals("plank_sack")) {
                sack = player.getInventory().getItem(i);
            }
        }
        expect(sack != null, "no plank sack in the pack");
        com.runecraft.content.minigames.PlankSack.use(player, sack);
        expect(!Inv.has(player, "mahogany_plank", 1) && com.runecraft.content.minigames.PlankSack.count(player, "mahogany_plank") == 10,
                "using the plank sack didn't take the 10 planks");
        Inv.give(player, "mahogany_plank", 10);
        MahoganyHomesSession mh = (MahoganyHomesSession) start(Minigame.MAHOGANY_HOMES);
        use(mh.selfTestContractor(), "mh_contractor");
        com.runecraft.osrs.content.minigames.MahoganyHomesLayouts.Layout layout = mh.selfTestLayout();
        expect(layout != null && layout.tier() == 3, "a level-99 builder got " + (layout == null ? "no contract" : "tier " + layout.tier()));
        notes.add("Expert contract for " + layout.owner() + " (" + layout.region() + "): " + layout.planks() + " planks, "
                + layout.bars() + " bars");

        step = "furniture";
        double con = xp(Skill.CONSTRUCTION);
        int planksBefore = Inv.count(player, "mahogany_plank") + com.runecraft.content.minigames.PlankSack.count(player, "mahogany_plank");
        for (com.runecraft.osrs.content.minigames.MahoganyHomesLayouts.Hotspot h : layout.hotspots()) {
            int k = h.slot();
            expect(mh.selfTestState(k) == (h.repair() ? 1 : 3), h.name() + " starts in state " + mh.selfTestState(k));
            if (!h.repair()) {
                use(mh.selfTestSlot(k), "mh_hotspot");
                expect(mh.selfTestState(k) == 2, "removing the old " + h.name() + " left state " + mh.selfTestState(k));
            }
            double before = xp(Skill.CONSTRUCTION);
            use(mh.selfTestSlot(k), "mh_hotspot");
            expect(mh.selfTestState(k) == 0, h.name() + " wasn't " + (h.repair() ? "repaired" : "built"));
            double want = h.count() * MinigameRules.MahoganyHomes.hotspotXp(3, h.steelBar(), h.repair());
            expect(Math.abs(xp(Skill.CONSTRUCTION) - before - want) < 0.05, h.name() + " gave " + (xp(Skill.CONSTRUCTION) - before)
                    + " XP, not " + want);
        }
        int planksUsed = planksBefore - Inv.count(player, "mahogany_plank")
                - com.runecraft.content.minigames.PlankSack.count(player, "mahogany_plank");
        expect(planksUsed == layout.planks() && 2 - Inv.count(player, "steel_bar") == layout.bars(), "the house took " + planksUsed
                + " planks and " + (2 - Inv.count(player, "steel_bar")) + " bars, not " + layout.planks() + " and " + layout.bars());
        expect(Math.abs(xp(Skill.CONSTRUCTION) - con - layout.furnitureXp()) < 0.05, "the furniture gave " + (xp(Skill.CONSTRUCTION) - con)
                + " XP, not " + layout.furnitureXp());
        if (layout.planks() > 10) {
            expect(com.runecraft.content.minigames.PlankSack.count(player, "mahogany_plank") < 10, "no plank came out of the sack");
        }

        step = "homeowner";
        con = xp(Skill.CONSTRUCTION);
        int pts = points("mahogany_homes");
        use(mh.selfTestHomeowner(), "mh_homeowner");
        expect(Math.abs(xp(Skill.CONSTRUCTION) - con - 2750) < 0.05, "completing an Expert contract gave " + (xp(Skill.CONSTRUCTION) - con)
                + " XP, not 2,750");
        expect(points("mahogany_homes") - pts == 5, "an Expert contract gave " + (points("mahogany_homes") - pts) + " points, not 5");
        expect(mh.selfTestLayout() == null && MinigameData.of(player).stat(player.getUUID(), "kc:mahogany_homes") == 1,
                "the contract wasn't closed and counted");
        notes.add("furniture " + layout.furnitureXp() + " XP + 2,750 completion XP, 5 points");

        step = "lower tier";
        player.setShiftKeyDown(true);
        use(mh.selfTestContractor(), "mh_contractor");
        player.setShiftKeyDown(false);
        expect(mh.selfTestLayout() != null && mh.selfTestLayout().tier() == 2, "a sneak-click at the desk didn't give an Adept contract");

        step = "shop";
        MinigameData.of(player).addPoints(player.getUUID(), "mahogany_homes", 1000);
        buy("mahogany_homes", "mahogany_homes", MinigameRules.MahoganyHomes.SUPPLY_CRATE, 25);
        buy("mahogany_homes", "mahogany_homes", "carpenters_helmet", 400);
        buy("mahogany_homes", "mahogany_homes", "plank_sack", 350);
        expect(Inv.has(player, "carpenters_helmet", 1), "the carpenter's helmet wasn't handed over");
        notes.add("Adept contract by sneak-click; bought a supply crate, the carpenter's helmet and a plank sack");
    }

    /** Heats or cools the preform into [lo, hi], restarting the lava or waterfall near the goal to keep the steps small. */
    private void bringHeat(GiantsFoundrySession gf, int lo, int hi) {
        for (int guard = 0; guard < 300; guard++) {
            int heat = gf.selfTestView().heat();
            if (heat >= lo && heat <= hi) {
                ActionManager.cancel(player);
                return;
            }
            boolean up = heat < lo;
            int distance = up ? lo - heat : heat - hi;
            if (!ActionManager.isBusy(player) || distance < 60) {
                player.setShiftKeyDown(distance > 400);
                use(gf.selfTestBlock(up ? "foundry_lava_pool" : "foundry_waterfall"), up ? "foundry_lava_pool" : "foundry_waterfall");
                player.setShiftKeyDown(false);
            }
            tick();
        }
        throw new AssertionError("couldn't bring the heat into " + lo + "-" + hi + " (at " + gf.selfTestView().heat() + ")");
    }
}
