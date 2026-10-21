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

    public static String fishingTrawler(ServerPlayer p) {
        return run(p, SkillingMinigameWalk::trawlerRound);
    }

    public static String troubleBrewing(ServerPlayer p) {
        return run(p, SkillingMinigameWalk::brewingRound);
    }

    public static String gnomeRestaurant(ServerPlayer p) {
        return run(p, SkillingMinigameWalk::gnomeRound);
    }

    public static String sorceressGarden(ServerPlayer p) {
        return run(p, SkillingMinigameWalk::gardenRound);
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

        // Wiki: the player may leave and come back anytime; the commission and the crucible are kept.
        step = "storage";
        MinigameManager.leave(player);
        expect(MinigameManager.session(player) == null, "the exit didn't take the player out of the foundry");
        gf = (GiantsFoundrySession) start(Minigame.GIANTS_FOUNDRY);
        v = gf.selfTestView();
        expect(commission.equals(v.commission()) && v.mouldScore() == mould && v.barsIn() == GiantsFoundry.CRUCIBLE
                && v.metalScore() == 130 && !v.poured(), "back in the foundry: " + v + " (the commission, moulds and 28 bars were kept)");
        notes.add("left and came back: commission, moulds and crucible kept");

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

        step = "preform storage";
        GiantsFoundrySession.View kept = gf.selfTestView();
        MinigameManager.leave(player);
        gf = (GiantsFoundrySession) start(Minigame.GIANTS_FOUNDRY);
        GiantsFoundrySession.View back = gf.selfTestView();
        expect(back.poured() && commission.equals(back.commission()) && back.sections() == kept.sections() && back.progress() == kept.progress()
                && back.heat() == kept.heat() && back.quality() == kept.quality() && back.startQuality() == kept.startQuality()
                && back.tool() == kept.tool() && back.metalScore() == kept.metalScore(), "the preform after leaving: " + back + ", left as " + kept);
        notes.add("left with the preform poured and came back to it (quality " + back.quality() + ")");

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

    // ------------------------------------------------------------------ Fishing Trawler

    private void trawlerRound() {
        step = "board";
        player.getInventory().clearContent();
        Inv.give(player, "swamp_paste", 60);
        Inv.give(player, "rope", 10);
        FishingTrawlerSession ft = (FishingTrawlerSession) start(Minigame.FISHING_TRAWLER);
        use(ft.selfTestBlock("trawler_box"), "trawler_box");
        expect(Inv.has(player, "hammer", 1) && Inv.has(player, "bronze_axe", 1), "the deck box handed out no hammer and axe");

        step = "leak";
        ft.selfTestLeak();
        BlockPos leak = ft.selfTestView(player).leaks().get(0);
        double con = xp(Skill.CONSTRUCTION);
        use(leak, "trawler_leak");
        expect(!ft.selfTestView(player).leaks().contains(leak) && ft.selfTestView(player).contribution() == 5
                && xp(Skill.CONSTRUCTION) - con == 5, "patching a leak: contribution " + ft.selfTestView(player).contribution());

        step = "bail";
        for (int i = 0; i < 6; i++) {
            ft.selfTestLeak();
        }
        until(30, () -> ft.selfTestView(player).water() >= 30);
        int water = ft.selfTestView(player).water();
        expect(water >= 30, "six open leaks let in only " + water + "% water in 30 ticks");
        for (BlockPos l : ft.selfTestView(player).leaks()) {
            use(l, "trawler_leak");
        }
        int before = ft.selfTestView(player).contribution();
        BlockPos bucket = ft.selfTestBlock("trawler_bucket");
        use(bucket, "trawler_bucket");
        expect(ActionManager.isBusy(player), "clicking the bailing bucket at " + water + "% water didn't start bailing");
        // Leaks keep springing while the player bails (one every 18 ticks alone), and an open leak lets its water in on the
        // very tick the bucket empties the hold, so the hold only runs dry with the leaks patched: patch each as it springs and
        // go back to the bucket (walking to a leak stops the bailing), as a player does. (Round 40's walk stood at the bucket
        // and let the new leaks sink the trawler, then read the 100% of the wreck as "bailing left 100% water".)
        int[] patched = {0};
        until(200, () -> {
            FishingTrawlerSession.View v = ft.selfTestView(player);
            if (v.water() == 0 || ft.isEnded()) {
                return true;
            }
            if (!v.leaks().isEmpty() || !ActionManager.isBusy(player)) {
                for (BlockPos l : v.leaks()) {
                    use(l, "trawler_leak");
                    patched[0]++;
                }
                use(bucket, "trawler_bucket");
            }
            return false;
        });
        FishingTrawlerSession.View bailed = ft.selfTestView(player);
        expect(!ft.isEnded() && bailed.water() == 0, "bailing left " + bailed.water() + "% water (from " + water + "%, "
                + patched[0] + " new leaks patched" + (ft.isEnded() ? ", the trawler sank" : "") + ")");
        int bails = (bailed.contribution() - before - patched[0] * MinigameRules.Trawler.LEAK_POINTS) / MinigameRules.Trawler.BAIL_POINTS;
        expect(bails >= 1, "bailing " + water + "% of water earned no contribution (" + (bailed.contribution() - before)
                + " points with " + patched[0] + " leaks patched)");
        notes.add("leaks patched (5 each), water " + water + "% bailed out (" + bails + " bails, " + patched[0] + " new leaks patched)");

        step = "net";
        ft.selfTestTearNet();
        double craft = xp(Skill.CRAFTING);
        before = ft.selfTestView(player).contribution();
        for (int i = 0; i < 30 && ft.selfTestView(player).netTorn(); i++) {
            use(ft.selfTestBlock("trawler_net"), "trawler_net");
        }
        expect(!ft.selfTestView(player).netTorn() && xp(Skill.CRAFTING) - craft == 5
                && ft.selfTestView(player).contribution() - before == Math.min(7, 255 - before), "mending the net: torn "
                + ft.selfTestView(player).netTorn() + ", Crafting +" + (xp(Skill.CRAFTING) - craft));

        step = "kraken";
        ft.selfTestTentacle();
        int side = ft.selfTestView(player).tentacleSide();
        expect(side != 0 && !ft.selfTestView(player).brokenRails().isEmpty(), "the tentacle didn't come up and break a rail");
        double wc = xp(Skill.WOODCUTTING);
        for (int i = 0; i < 40 && ft.selfTestView(player).tentacleSide() != 0; i++) {
            use(ft.selfTestTentaclePos(), "trawler_tentacle");
        }
        expect(ft.selfTestView(player).tentacleSide() == 0 && xp(Skill.WOODCUTTING) - wc == 5 * MinigameRules.Trawler.TENTACLE_HP,
                "chopping the tentacle off: side " + ft.selfTestView(player).tentacleSide() + ", Woodcutting +" + (xp(Skill.WOODCUTTING) - wc));
        for (BlockPos rail : ft.selfTestView(player).brokenRails()) {
            use(rail, "trawler_rail");
        }
        expect(ft.selfTestView(player).brokenRails().isEmpty(), "the broken rails weren't fixed");
        notes.add("net mended, tentacle chopped off, rails fixed (contribution " + ft.selfTestView(player).contribution() + ")");

        step = "trawl";
        double fishing = xp(Skill.FISHING);
        int kc = MinigameData.of(player).stat(player.getUUID(), "kc:fishing_trawler");
        expect(until(MinigameRules.Trawler.GAME_TICKS + 5, () -> {
            if (ft.isEnded()) {
                return true;
            }
            FishingTrawlerSession.View v = ft.selfTestView(player);
            for (BlockPos l : v.leaks()) {
                use(l, "trawler_leak");
            }
            if (v.netTorn()) {
                use(ft.selfTestBlock("trawler_net"), "trawler_net");
            }
            if (v.tentacleSide() != 0) {
                use(ft.selfTestTentaclePos(), "trawler_tentacle");
            }
            for (BlockPos rail : v.brokenRails()) {
                use(rail, "trawler_rail");
            }
            return false;
        }), "the trawl didn't end after " + MinigameRules.Trawler.GAME_TICKS + " ticks");
        expect(MinigameManager.session(player) == null, "the player is still aboard after the trawl");
        expect(MinigameData.of(player).stat(player.getUUID(), "kc:fishing_trawler") == kc + 1, "the trawl wasn't counted");
        int fish = 0;
        for (MinigameRules.Trawler.Catch c : MinigameRules.Trawler.CATCHES) {
            fish += Inv.count(player, c.item());
        }
        expect(fish >= 10 && Inv.count(player, "raw_manta_ray") > 0 && xp(Skill.FISHING) > fishing, "a level-99 angler's share: "
                + fish + " fish (" + Inv.count(player, "raw_manta_ray") + " manta rays), Fishing +" + (xp(Skill.FISHING) - fishing));
        notes.add("trawl survived: " + fish + " fish, " + (xp(Skill.FISHING) - fishing) + " Fishing XP");

        step = "angler";
        session = null;
        FishingTrawlerSession sink = (FishingTrawlerSession) start(Minigame.FISHING_TRAWLER);
        // Successful 1/12 rolls: the first set completes before the second starts, and nothing comes after two sets.
        String[] outfit = MinigameRules.Trawler.OUTFIT;
        int[] have = new int[outfit.length];
        int owned = 0;
        for (int i = 0; i < outfit.length; i++) {
            have[i] = Math.max(MinigameData.of(player).stat(player.getUUID(), "trawler_angler:" + outfit[i]),
                    Players.data(player).collectionLog().contains(outfit[i]) ? 1 : 0);
            owned += have[i];
        }
        int given = 0;
        String piece;
        while ((piece = sink.selfTestAnglerRoll(player)) != null && given < 12) {
            int i = java.util.Arrays.asList(outfit).indexOf(piece);
            expect(i >= 0, "the angler roll gave " + piece);
            have[i]++;
            given++;
            int lo = java.util.Arrays.stream(have).min().getAsInt();
            int hi = java.util.Arrays.stream(have).max().getAsInt();
            expect(hi - lo <= 1 && hi <= MinigameRules.Trawler.ANGLER_SETS, "angler pieces out of set order: "
                    + java.util.Arrays.toString(have));
        }
        expect(piece == null && given == outfit.length * MinigameRules.Trawler.ANGLER_SETS - owned
                && sink.selfTestAnglerRoll(player) == null, "angler rolls gave " + given + " pieces after " + owned + " owned: "
                + java.util.Arrays.toString(have) + " (two full sets, then nothing)");
        notes.add("angler rolls: " + given + " pieces, two full sets in order, then none");

        step = "sink";
        for (int i = 0; i < 16; i++) {
            sink.selfTestLeak();
        }
        expect(until(200, sink::isEnded), "16 open leaks didn't sink the trawler in 200 ticks (water " + sink.selfTestView(player).water() + ")");
        expect(MinigameManager.session(player) == null, "the player is still aboard the sunk trawler");
        notes.add("16 leaks sank the trawler");
    }

    // ------------------------------------------------------------------ Trouble Brewing

    private void brewingRound() {
        step = "entry";
        // Wiki: no items in the inventory and nothing on the head; a wielded weapon (the player's own axe) may come along.
        player.getInventory().clearContent();
        Inv.give(player, "rune_axe", 1);
        hold("rune_axe");
        Inv.give(player, "logs", 1);
        expect(SkillingMinigameRules.requirement(player, Minigame.TROUBLE_BREWING) != null, "the brewery let in a player carrying logs");
        Inv.remove(player, "logs", 1);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, Inv.stack("rune_full_helm", 1));
        expect(SkillingMinigameRules.requirement(player, Minigame.TROUBLE_BREWING) != null, "the brewery let in a player in a helmet");
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.item.ItemStack.EMPTY);
        String entry = SkillingMinigameRules.requirement(player, Minigame.TROUBLE_BREWING);
        expect(entry == null, "an empty inventory with a wielded axe was refused: " + entry);
        notes.add("entry: empty inventory and no hat, a wielded axe allowed");

        step = "workbench";
        TroubleBrewingSession tb = (TroubleBrewingSession) start(Minigame.TROUBLE_BREWING);
        use(tb.selfTestBlock("tb_workbench"), "tb_workbench");
        for (String tool : new String[]{"knife", "tinderbox", "bronze_axe", "minecraft:bucket", "minecraft:bowl", "minecraft:beef"}) {
            expect(Inv.has(player, tool, 1), "the workbench handed out no " + tool);
        }

        step = "water";
        use(tb.selfTestBlock("tb_water_pump"), "tb_water_pump");
        expect(Inv.count(player, "minecraft:water_bucket") == 5, "the pump filled " + Inv.count(player, "minecraft:water_bucket")
                + " buckets");

        step = "coloured water";
        use(tb.selfTestBlock("tb_flowers"), "tb_flowers");
        expect(Inv.count(player, "tb_red_flowers") == MinigameRules.TroubleBrewing.MAX_FLOWERS, "picked "
                + Inv.count(player, "tb_red_flowers") + " flowers");
        use(tb.selfTestBlock("tb_kettle"), "tb_kettle");
        expect(Inv.count(player, "bowl_of_red_water") == 3, "the kettle filled " + Inv.count(player, "bowl_of_red_water") + " bowls");

        step = "bitternut";
        double hunter = xp(Skill.HUNTER);
        use(tb.selfTestBlock("tb_bitternut_tree"), "tb_bitternut_tree");
        expect(until(10, () -> Inv.has(player, "bitternut", 1)), "the dyed monkey brought no bitternut");
        expect(xp(Skill.HUNTER) - hunter == 65, "a monkey and a bitternut gave " + (xp(Skill.HUNTER) - hunter) + " Hunter XP, not 15 + 50");
        use(tb.selfTestBlock("tb_kettle"), "tb_kettle");
        expect(Inv.count(player, "bowl_of_red_water") == 3, "refilling the monkey's bowl at the kettle failed");

        step = "sweetgrubs";
        for (int i = 0; i < 12 && !Inv.has(player, "sweetgrubs", 1); i++) {
            if (!Inv.has(player, "minecraft:beef", 1)) {
                use(tb.selfTestBlock("tb_workbench"), "tb_workbench");
            }
            use(tb.selfTestBlock("tb_sweetgrub_mound"), "tb_sweetgrub_mound");
            if (!Inv.has(player, "sweetgrubs", 1)) {
                until(60, () -> false); // a collapsed mound fills again
            }
        }
        expect(Inv.has(player, "sweetgrubs", 1), "12 tries at the mound brought no sweetgrubs");

        step = "scrapey bark";
        use(tb.selfTestBlock("tb_scrapey_tree"), "tb_scrapey_tree");
        expect(until(40, () -> Inv.has(player, "scrapey_tree_logs", 1)), "40 ticks chopping the scrapey tree gave no logs");
        ActionManager.cancel(player);
        double fletching = xp(Skill.FLETCHING);
        int logs = Inv.count(player, "scrapey_tree_logs");
        expect(TroubleBrewingSession.useKnife(player, "knife"), "the knife didn't work on the scrapey logs");
        expect(Inv.count(player, "scrapey_tree_bark") == logs && xp(Skill.FLETCHING) - fletching == 50.0 * logs,
                "scraping " + logs + " logs: " + Inv.count(player, "scrapey_tree_bark") + " bark, Fletching +" + (xp(Skill.FLETCHING) - fletching));
        while (Inv.count(player, "scrapey_tree_bark") > 1) {
            Inv.remove(player, "scrapey_tree_bark", 1);
        }
        notes.add("water, red water, a bitternut, sweetgrubs and bark gathered");

        step = "boilers";
        double fm = xp(Skill.FIREMAKING);
        for (int i = 0; i < 3; i++) {
            use(tb.selfTestBlock("tb_log_store"), "tb_log_store");
            use(tb.selfTestBoiler(i), "tb_boiler");
        }
        expect(tb.selfTestView(player).litBoilers() == 3 && xp(Skill.FIREMAKING) - fm == 300, "three boilers: "
                + tb.selfTestView(player).litBoilers() + " ready, Firemaking +" + (xp(Skill.FIREMAKING) - fm));


        step = "hopper";
        double cooking = xp(Skill.COOKING);
        while (Inv.count(player, "sweetgrubs") > 1) {
            Inv.remove(player, "sweetgrubs", 1);
        }
        while (Inv.count(player, "bitternut") > 1) {
            Inv.remove(player, "bitternut", 1);
        }
        use(tb.selfTestBlock("tb_brewing_vat"), "tb_brewing_vat");
        expect(xp(Skill.COOKING) - cooking == 200 + 100 + 100 + 300, "the hopper gave " + (xp(Skill.COOKING) - cooking)
                + " Cooking XP, not 700 (bitternut 200, grubs and bark 100, 3 bowls 300; water none)");
        expect(tb.selfTestView(player).contribution() == 3 + 1 + 1 + 1 + 3 + 5, "contribution " + tb.selfTestView(player).contribution()
                + " (3 boilers lit + 11 resources)");

        step = "brew";
        expect(until(5, () -> tb.selfTestView(player).brewing()), "a full hopper with three lit boilers didn't start a bottle");
        expect(until(MinigameRules.TroubleBrewing.BREW_TICKS + 3, () -> tb.selfTestView(player).bottleWaiting()),
                "the bottle didn't come off the line in 38 seconds");
        use(tb.selfTestBlock("tb_crate"), "tb_crate");
        expect(tb.selfTestView(player).teamBottles() == 1 && !tb.selfTestView(player).bottleWaiting(), "packing the bottle failed");
        notes.add("a bottle brewed in " + MinigameRules.TroubleBrewing.BREW_TICKS + " ticks and packed");

        step = "end";
        int contribution = tb.selfTestView(player).contribution();
        double craft = xp(Skill.CRAFTING);
        cooking = xp(Skill.COOKING);
        fm = xp(Skill.FIREMAKING);
        int eight = Inv.count(player, "pieces_of_eight");
        tb.selfTestNearEnd();
        expect(until(10, tb::isEnded), "the game didn't end at 20 minutes");
        int got = Inv.count(player, "pieces_of_eight") - eight;
        expect(got == MinigameRules.TroubleBrewing.piecesOfEight(1, contribution), "pieces of eight " + got + ", not "
                + MinigameRules.TroubleBrewing.piecesOfEight(1, contribution));
        // Four rum credits: the bitternut, the grubs, the bark and three bowls of red water.
        expect(xp(Skill.CRAFTING) - craft == 400 && xp(Skill.COOKING) - cooking == 1000 && xp(Skill.FIREMAKING) - fm == 600,
                "rum rewards: Crafting +" + (xp(Skill.CRAFTING) - craft) + ", Cooking +" + (xp(Skill.COOKING) - cooking) + ", Firemaking +"
                        + (xp(Skill.FIREMAKING) - fm));
        expect(!Inv.has(player, "minecraft:bucket", 1) && !Inv.has(player, "logs", 1), "brewery supplies left the game");
        expect(!Inv.has(player, "knife", 1) && !Inv.has(player, "tinderbox", 1) && !Inv.has(player, "bronze_axe", 1),
                "workbench tools left the game: knife " + Inv.count(player, "knife") + ", tinderbox " + Inv.count(player, "tinderbox")
                        + ", bronze axe " + Inv.count(player, "bronze_axe"));
        expect(Inv.has(player, "rune_axe", 1), "the player's own wielded rune axe was taken on leaving");
        notes.add(got + " pieces of eight and 4 rum rewards");

        step = "shop";
        Inv.give(player, "pieces_of_eight", 3000);
        int before = Inv.count(player, "pieces_of_eight");
        RewardShopService.buy(player, "trouble_brewing", entry("trouble_brewing", "cutthroat_flag"), 1);
        RewardShopService.buy(player, "trouble_brewing", entry("trouble_brewing", "rum_red"), 1);
        expect(Inv.has(player, "cutthroat_flag", 1) && Inv.has(player, "rum_red", 1) && before - Inv.count(player, "pieces_of_eight") == 2020,
                "Honest Jimmy: flag " + Inv.has(player, "cutthroat_flag", 1) + ", rum " + Inv.has(player, "rum_red", 1) + ", paid "
                        + (before - Inv.count(player, "pieces_of_eight")));
        notes.add("bought the cutthroat flag (2,000) and red rum (20)");
    }

    // ------------------------------------------------------------------ Gnome Restaurant

    private void gnomeRound() {
        step = "easy job";
        player.getInventory().clearContent();
        GnomeRestaurantSession gr = (GnomeRestaurantSession) start(Minigame.GNOME_RESTAURANT);
        use(gr.selfTestBlock("gnome_gianne"), "gnome_gianne");
        GnomeRestaurantSession.View v = gr.selfTestView();
        expect(v.order() != null && !v.hard() && MinigameRules.GnomeRestaurant.EASY_CUSTOMERS.contains(v.customer())
                && v.ticksLeft() == MinigameRules.GnomeRestaurant.EASY_TICKS, "an easy job: " + v);
        // The oven checks the dish's Cooking level (every restaurant dish needs 25 or more).
        int cookingTenths = Players.data(player).skills().xpTenths(Skill.COOKING);
        Players.data(player).skills().setXpTenths(Skill.COOKING, 0);
        use(gr.selfTestBlock("gnome_oven"), "gnome_oven");
        Players.data(player).skills().setXpTenths(Skill.COOKING, cookingTenths);
        expect(!Inv.has(player, v.order(), 1) && Inv.has(player, "gnome_ingredients", 1), "a level-1 cook baked the "
                + v.order() + " (needs " + MinigameRules.GnomeRestaurant.dishLevel(v.order()) + " Cooking)");
        double cooking = xp(Skill.COOKING);
        use(gr.selfTestBlock("gnome_oven"), "gnome_oven");
        expect(Inv.has(player, v.order(), 1) && xp(Skill.COOKING) - cooking == MinigameRules.GnomeRestaurant.dishXp(v.order()),
                "baking the " + v.order() + ": Cooking +" + (xp(Skill.COOKING) - cooking));
        int coins = Inv.count(player, "coins");
        cooking = xp(Skill.COOKING);
        use(gr.selfTestBlock("gnome_customer"), "gnome_customer");
        int tip = Inv.count(player, "coins") - coins;
        expect(gr.selfTestView().order() == null && points("gnome_restaurant") == 1, "the easy delivery: points "
                + points("gnome_restaurant"));
        expect(tip >= 2 && tip <= 500 && xp(Skill.COOKING) == cooking, "an easy tip of " + tip + " coins, Cooking +"
                + (xp(Skill.COOKING) - cooking) + " (deliveries give no XP)");
        notes.add("easy " + v.order() + " for " + v.customer() + ": " + tip + " coins");

        step = "hard job";
        player.setShiftKeyDown(true);
        use(gr.selfTestBlock("gnome_gianne"), "gnome_gianne");
        player.setShiftKeyDown(false);
        v = gr.selfTestView();
        expect(v.order() != null && v.hard() && MinigameRules.GnomeRestaurant.HARD_CUSTOMERS.contains(v.customer())
                && v.ticksLeft() == MinigameRules.GnomeRestaurant.HARD_TICKS, "a hard job: " + v);
        Inv.give(player, v.order(), 1); // made earlier with gnome cooking
        Inv.remove(player, "gnome_ingredients", Inv.count(player, "gnome_ingredients"));
        coins = Inv.count(player, "coins");
        int items = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            items += player.getInventory().getItem(i).isEmpty() ? 0 : 1;
        }
        use(gr.selfTestBlock("gnome_customer"), "gnome_customer");
        int after = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            after += player.getInventory().getItem(i).isEmpty() ? 0 : 1;
        }
        expect(points("gnome_restaurant") == 4 && Inv.count(player, "coins") == coins && after >= items,
                "the hard delivery: points " + points("gnome_restaurant") + ", coins " + (Inv.count(player, "coins") - coins)
                        + " (hard customers tip items)");
        notes.add("hard delivery for " + v.customer() + " (item tip)");

        step = "decline";
        player.setShiftKeyDown(true);
        use(gr.selfTestBlock("gnome_gianne"), "gnome_gianne");
        use(gr.selfTestBlock("gnome_gianne"), "gnome_gianne");
        expect(gr.selfTestView().order() == null, "sneak-clicking Gianne with a hard job didn't decline it");
        use(gr.selfTestBlock("gnome_gianne"), "gnome_gianne");
        player.setShiftKeyDown(false);
        expect(gr.selfTestView().order() == null, "a hard job was offered right after declining one");

        step = "late";
        use(gr.selfTestBlock("gnome_gianne"), "gnome_gianne");
        expect(gr.selfTestView().order() != null && !gr.selfTestView().hard(), "no easy job after declining a hard one");
        gr.selfTestNearDeadline();
        until(5, () -> gr.selfTestView().order() == null);
        expect(gr.selfTestView().order() == null && MinigameManager.session(player) == gr, "a late job: order "
                + gr.selfTestView().order() + " (it is lost, the shift goes on)");
        notes.add("declined a hard job (5.5-minute wait), let an easy one expire");

        step = "token";
        MinigameData.of(player).addPoints(player.getUUID(), "gnome_restaurant", 7);
        use(gr.selfTestBlock("gnome_gianne"), "gnome_gianne");
        Inv.remove(player, "gnome_ingredients", Inv.count(player, "gnome_ingredients"));
        Inv.give(player, gr.selfTestView().order(), 1);
        use(gr.selfTestBlock("gnome_customer"), "gnome_customer");
        expect(Inv.count(player, "gnome_restaurant_token") == 1 && points("gnome_restaurant") == 12,
                "12 points made " + Inv.count(player, "gnome_restaurant_token") + " reward tokens");
        int dishes = 0;
        for (Object[] d : com.runecraft.osrs.content.minigames.MinigameItems.GNOME_DISHES) {
            dishes += Inv.count(player, (String) d[0]);
        }
        RewardShopService.buy(player, "gnome_restaurant", entry("gnome_restaurant", MinigameRules.GnomeRestaurant.RANDOM_DISH), 1);
        int now = 0;
        for (Object[] d : com.runecraft.osrs.content.minigames.MinigameItems.GNOME_DISHES) {
            now += Inv.count(player, (String) d[0]);
        }
        expect(!Inv.has(player, "gnome_restaurant_token", 1) && now == dishes + 1, "the token bought " + (now - dishes) + " dishes");
        notes.add("a reward token at 12 points, cashed in for a random dish");

        // Ten unused tokens fill it (120 points at once): the next 12 points make no dish; once one is spent they do again.
        step = "full token";
        int cap = MinigameRules.GnomeRestaurant.MAX_TOKENS;
        Inv.give(player, "gnome_restaurant_token", cap);
        MinigameData.of(player).addPoints(player.getUUID(), "gnome_restaurant", 11);
        easyDelivery(gr);
        expect(Inv.count(player, "gnome_restaurant_token") == cap && points("gnome_restaurant") == 24,
                "a delivery with a full token: " + Inv.count(player, "gnome_restaurant_token") + " tokens, points "
                        + points("gnome_restaurant"));
        Inv.remove(player, "gnome_restaurant_token", 1);
        MinigameData.of(player).addPoints(player.getUUID(), "gnome_restaurant", 11);
        easyDelivery(gr);
        expect(Inv.count(player, "gnome_restaurant_token") == cap && points("gnome_restaurant") == 36,
                "after using a token the next 12 points made " + (Inv.count(player, "gnome_restaurant_token") - cap + 1)
                        + " tokens (points " + points("gnome_restaurant") + ")");
        notes.add("ten unused tokens held: no 11th; one used, the next 12 points refilled it");
    }

    /** An easy job from Gianne, its dish made in advance, delivered to the customer. */
    private void easyDelivery(GnomeRestaurantSession gr) {
        use(gr.selfTestBlock("gnome_gianne"), "gnome_gianne");
        expect(gr.selfTestView().order() != null, "Gianne gave no job");
        Inv.remove(player, "gnome_ingredients", Inv.count(player, "gnome_ingredients"));
        Inv.give(player, gr.selfTestView().order(), 1);
        use(gr.selfTestBlock("gnome_customer"), "gnome_customer");
        expect(gr.selfTestView().order() == null, "the delivery wasn't taken");
    }

    // ------------------------------------------------------------------ Sorceress's Garden

    /** Through a garden's gate and along an unseen route to the clearing; fails if an elemental catches the player. */
    private void sneakIn(SorceressGardenSession sg, com.runecraft.osrs.content.minigames.SorceressGarden.Season s) {
        use(sg.selfTestGate(s), s.gate());
        expect(sg.selfTestGarden(player) == s, "the " + s.displayName() + " gate didn't let the player in");
        List<int[]> route = com.runecraft.osrs.content.minigames.SorceressGarden.route(s, sg.selfTestTicks(), 2,
                com.runecraft.osrs.content.minigames.SorceressGarden.CORRIDOR + 2, 400);
        expect(route != null, "no unseen route through the " + s.displayName() + " garden from tick " + sg.selfTestTicks());
        for (int[] sq : route) {
            moveTo(sg.selfTestSquare(s, sq[0], sq[1]));
            tick();
            expect(sg.selfTestGarden(player) == s, "an elemental caught the player at square " + sq[0] + "," + sq[1]
                    + " of the planned route (tick " + sg.selfTestTicks() + ")");
        }
    }

    private void gardenRound() {
        com.runecraft.osrs.content.minigames.SorceressGarden.Season winter = com.runecraft.osrs.content.minigames.SorceressGarden.Season.WINTER;
        step = "enter";
        player.getInventory().clearContent();
        SorceressGardenSession sg = (SorceressGardenSession) start(Minigame.SORCERESS_GARDEN);
        expect(sg.selfTestGarden(player) == null, "the player didn't start in the central garden");

        step = "sq'irks";
        for (int trip = 1; trip <= winter.perGlass(); trip++) {
            sneakIn(sg, winter);
            double farming = xp(Skill.FARMING);
            use(sg.selfTestTree(winter), winter.tree());
            expect(Inv.count(player, winter.fruit()) == trip && xp(Skill.FARMING) - farming == winter.farmingXp(),
                    "trip " + trip + ": " + Inv.count(player, winter.fruit()) + " sq'irks, Farming +" + (xp(Skill.FARMING) - farming));
            expect(sg.selfTestGarden(player) == null, "picking a sq'irk didn't send the player back to the fountain");
        }
        notes.add(winter.perGlass() + " winter sq'irks picked unseen (" + winter.farmingXp() + " Farming XP each)");

        step = "herbs";
        sneakIn(sg, winter);
        double farming = xp(Skill.FARMING);
        use(sg.selfTestHerbs(winter), winter.herbs());
        int herbs = 0;
        for (com.runecraft.osrs.content.minigames.MinigameRules.Loot l : winter.herbTable()) {
            herbs += Inv.count(player, l.item());
        }
        expect(herbs == 2 && xp(Skill.FARMING) - farming == 50, "the herb patch gave " + herbs + " herbs and "
                + (xp(Skill.FARMING) - farming) + " Farming XP (wiki: 2 and 50)");

        step = "caught";
        use(sg.selfTestGate(winter), winter.gate());
        long next = sg.selfTestTicks() + 1;
        int[] watched = null;
        for (int u = 1; u <= com.runecraft.osrs.content.minigames.SorceressGarden.CORRIDOR && watched == null; u++) {
            for (int v = -6; v <= 6 && watched == null; v++) {
                if (com.runecraft.osrs.content.minigames.SorceressGarden.walkable(winter, u, v)
                        && com.runecraft.osrs.content.minigames.SorceressGarden.seen(winter, next, u, v)) {
                    watched = new int[]{u, v};
                }
            }
        }
        expect(watched != null, "no square of the winter garden is watched at tick " + next);
        moveTo(sg.selfTestSquare(winter, watched[0], watched[1]));
        tick();
        expect(sg.selfTestGarden(player) == null, "standing next to an elemental didn't get the player teleported out");
        notes.add("2 herbs (50 Farming XP); an elemental caught the player in its sight");

        step = "fountain";
        use(sg.selfTestFountain(), "sg_fountain");
        expect(MinigameManager.session(player) == null, "drinking from the fountain didn't leave the garden");

        step = "juice";
        Inv.give(player, "pestle_and_mortar", 1);
        Inv.give(player, "beer_glass", 1);
        double cooking = xp(Skill.COOKING);
        com.runecraft.content.minigames.SorceressGardenService.selfTestJuice(player, winter);
        ActionManager.cancel(player);
        expect(Inv.count(player, winter.juice()) == 1 && !Inv.has(player, winter.fruit(), 1) && xp(Skill.COOKING) - cooking == 5,
                "crushing 5 winter sq'irks: " + Inv.count(player, winter.juice()) + " juice, Cooking +" + (xp(Skill.COOKING) - cooking));
        double thieving = xp(Skill.THIEVING);
        expect(com.runecraft.content.minigames.SorceressGardenService.selfTestOsman(player)
                && xp(Skill.THIEVING) - thieving == winter.osmanXp(), "Osman gave " + (xp(Skill.THIEVING) - thieving)
                + " Thieving XP for a winter sq'irkjuice, not " + winter.osmanXp());
        notes.add("a winter sq'irkjuice (5 Cooking XP) for " + winter.osmanXp() + " Thieving XP from Osman");
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
