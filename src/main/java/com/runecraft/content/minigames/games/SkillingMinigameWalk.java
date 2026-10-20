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
import com.runecraft.osrs.content.minigames.RewardShop;
import com.runecraft.osrs.content.minigames.RewardShops;
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
