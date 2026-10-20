package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.minigames.GiantsFoundry;
import com.runecraft.osrs.content.minigames.GiantsFoundry.Commission;
import com.runecraft.osrs.content.minigames.GiantsFoundry.Metal;
import com.runecraft.osrs.content.minigames.GiantsFoundry.Mould;
import com.runecraft.osrs.content.minigames.GiantsFoundry.Tool;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameCurrencies;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Giants' Foundry (OSRS Wiki; data in {@link GiantsFoundry}). Kovac (his bench) hands out a commission of two styles; the
 * mould jig takes the best moulds the player may use for it (the defaults plus those bought from Kovac, at their Smithing
 * level); the crucible takes 28 bars' worth of metal (bars, or smithed equipment worth its bars minus one) whose two most
 * abundant metals give the metal score; pouring makes the preform. The sword starts at metal + mould score and is refined
 * through 3-7 sections at the trip hammer (hot), grindstone (warm) and polishing wheel (cold): each machine acts every 5 or
 * 2 game ticks, adding progress and changing the heat, and working at the wrong temperature (or with the wrong machine)
 * costs 10 quality. The lava pool heats and the waterfall cools the preform faster the longer it stays in (sneak-click to
 * dunk or quench); it also cools by itself. Sweet spots open while a machine works: clicking it once in the window adds
 * about 5% progress, doubles the next action's XP and restores some lost quality; clicking it otherwise overworks the
 * preform (the next action is lost). Kovac pays (floor(q^2/73) + floor(1.5q) + 1) x 30 Smithing XP, twice that in coins
 * and q reputation. Each piece of the Smiths' Uniform gives a 20% chance of extra progress per action (the set always).
 */
public class GiantsFoundrySession extends MinigameSession {
    private Commission commission;
    private Mould[] moulds;
    private int mouldScore;
    private final int[] metal = new int[Metal.values().length];
    private int barsIn;
    private boolean poured;
    private int metalScore;
    private Tool[] plan = new Tool[0];
    private int heat;
    private int progress;
    private int quality;
    private int startQuality;
    private int mistakes;
    private int bonusesLeft;
    private int bonusesTotal;
    private long bonusUntil = -1;
    private boolean bonusCaught;
    private boolean overworked;
    /** The machine being worked (null when idle) and the lava/waterfall direction (+1 heating, -1 cooling, 0 idle). */
    private Tool working;
    private int heating;
    private int swords;

    public GiantsFoundrySession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.GIANTS_FOUNDRY, id, slot, world, variant);
    }

    /** Entry check: Sleeping Giants opens the foundry. */
    public static String requirement(ServerPlayer player) {
        if (ContentRegistry.quest(GiantsFoundry.QUEST) != null && !QuestApi.isComplete(player, GiantsFoundry.QUEST)) {
            return "Kovac: Only those who helped wake the giants may work in the foundry. (Complete Sleeping Giants.)";
        }
        return null;
    }

    @Override
    protected int halfX() {
        return 14;
    }

    @Override
    protected int halfZ() {
        return 14;
    }

    @Override
    protected void build() {
        room(Blocks.POLISHED_BASALT, Blocks.BLACKSTONE, Blocks.MAGMA_BLOCK);
        place(-8, -8, "foundry_crucible");
        place(-2, -8, "foundry_mould");
        place(6, -8, Tool.TRIP_HAMMER.block());
        place(9, -3, Tool.GRINDSTONE.block());
        place(9, 3, Tool.POLISHING_WHEEL.block());
        place(-10, 2, "foundry_lava_pool");
        place(-10, 6, "foundry_waterfall");
        place(4, 8, "foundry_handin");
        place(0, 13, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return rel(index % 3 - 1, 1, 10);
    }

    @Override
    protected void start() {
        broadcast("Kovac: Speak to me at my bench for a commission, then set the mould and fill the crucible with 28 bars.");
    }

    // ------------------------------------------------------------------ the sword

    private void newCommission(ServerPlayer player) {
        commission = GiantsFoundry.COMMISSIONS.get(random.nextInt(GiantsFoundry.COMMISSIONS.size()));
        moulds = null;
        mouldScore = 0;
        poured = false;
        progress = 0;
        heat = 0;
        mistakes = 0;
        working = null;
        heating = 0;
        Msg.game(player, "Kovac: I need a " + commission.display().toLowerCase(java.util.Locale.ROOT)
                + " sword. Set the mould jig for it, then fill the crucible.");
    }

    private int smithing(ServerPlayer p) {
        return Players.data(p).skills().currentLevel(Skill.SMITHING);
    }

    /** A mould the player may use: a default one, or a bought one at its Smithing level. */
    private boolean usable(ServerPlayer p, Mould m) {
        return smithing(p) >= m.level() && (!m.purchasable() || MinigameData.of(p).flag(p.getUUID(), m.flag()));
    }

    private void setMoulds(ServerPlayer p) {
        moulds = GiantsFoundry.bestMoulds(commission, m -> usable(p, m));
        mouldScore = GiantsFoundry.mouldScore(commission, moulds);
        Msg.game(p, "You set the mould jig: " + moulds[0].name() + ", " + moulds[1].name() + " and " + moulds[2].name()
                + " (mould score " + mouldScore + " for a " + commission.display() + " sword).");
    }

    /** Adds every carried bar and metal item to the crucible, best metal first, up to 28 bars' worth. */
    private void fillCrucible(ServerPlayer p) {
        if (poured) {
            Msg.game(p, "The crucible is empty; the preform has already been poured.");
            return;
        }
        int added = 0;
        int lost = 0;
        int level = smithing(p);
        boolean tooLow = false;
        for (int t = Metal.values().length - 1; t >= 0 && barsIn < GiantsFoundry.CRUCIBLE; t--) {
            Metal m = Metal.values()[t];
            List<String> items = new ArrayList<>();
            items.add(m.bar());
            for (List<String> list : List.of(GiantsFoundry.EQUIPMENT_1, GiantsFoundry.EQUIPMENT_2, GiantsFoundry.EQUIPMENT_4)) {
                for (String suffix : list) {
                    items.add(m.prefix() + "_" + suffix);
                }
            }
            for (String item : items) {
                while (barsIn < GiantsFoundry.CRUCIBLE && Inv.has(p, item, 1)) {
                    if (level < m.level()) {
                        tooLow = true;
                        break;
                    }
                    Inv.remove(p, item, 1);
                    int value = GiantsFoundry.barValue(item);
                    int fits = Math.min(value, GiantsFoundry.CRUCIBLE - barsIn);
                    lost += value - fits; // OSRS destroys metal beyond the crucible's space
                    metal[t] += fits;
                    barsIn += fits;
                    added += fits;
                }
            }
        }
        if (added == 0) {
            Msg.game(p, tooLow ? "You need a higher Smithing level to melt that metal." : barsIn >= GiantsFoundry.CRUCIBLE
                    ? "The crucible is full." : "You have no metal bars or metal equipment to put in the crucible.");
            return;
        }
        metalScore = GiantsFoundry.metalScore(metal);
        Msg.game(p, "You add " + added + " bars' worth of metal to the crucible (" + barsIn + " / " + GiantsFoundry.CRUCIBLE
                + ", metal score " + metalScore + ")." + (lost > 0 ? " " + lost + " bars' worth overflowed and was lost." : ""));
    }

    private void pour(ServerPlayer p) {
        for (Mould m : moulds) {
            if (!usable(p, m)) {
                Msg.game(p, "You need a Smithing level of " + m.level() + " to use the " + m.name() + " mould.");
                moulds = null;
                return;
            }
        }
        metalScore = GiantsFoundry.metalScore(metal);
        poured = true;
        int sections = GiantsFoundry.sections(metalScore);
        plan = GiantsFoundry.plan(sections, random);
        startQuality = Math.min(199, metalScore + mouldScore);
        quality = startQuality;
        progress = 0;
        heat = 0;
        bonusesTotal = GiantsFoundry.bonuses(sections);
        bonusesLeft = bonusesTotal;
        bonusUntil = -1;
        bonusCaught = false;
        java.util.Arrays.fill(metal, 0);
        barsIn = 0;
        Msg.game(p, "You pour the molten metal into the mould and pick up the preform: quality " + quality + ", " + sections
                + " sections. First: the " + plan[0].display() + ".");
    }

    private Tool currentTool() {
        return plan[GiantsFoundry.section(progress, plan.length)];
    }

    private boolean finished() {
        return poured && progress >= GiantsFoundry.MAX;
    }

    private boolean ruined() {
        return poured && quality <= 0;
    }

    private void mistake(ServerPlayer p, String why) {
        mistakes++;
        quality = Math.max(0, quality - GiantsFoundry.MISTAKE);
        Msg.red(p, why + " The sword loses quality (" + quality + ").");
        if (quality <= 0) {
            Msg.red(p, "This sword is too damaged to keep working. You should give it to Kovac and try again.");
        }
    }

    private int uniformPieces(ServerPlayer p) {
        List<String> worn = new ArrayList<>();
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            worn.add(Inv.idOf(p.getItemBySlot(slot)));
        }
        for (ItemStack st : com.runecraft.content.systems.SysState.of(p).worn.values()) {
            if (st != null && !st.isEmpty()) {
                worn.add(Inv.idOf(st));
            }
        }
        int n = 0;
        for (String piece : GiantsFoundry.UNIFORM) {
            if (worn.contains(piece)) {
                n++;
            }
        }
        return n;
    }

    /** Starts working a machine; clicking the machine already being worked catches a sweet spot or overworks the preform. */
    private void work(ServerPlayer player, Tool tool, BlockPos pos) {
        if (!poured || finished() || ruined()) {
            Msg.game(player, !poured ? "There's no preform to work on." : ruined() ? "The sword is ruined. Give it to Kovac."
                    : "The sword is finished. Hand it to Kovac.");
            return;
        }
        if (working == tool && com.runecraft.action.ActionManager.isBusy(player)) {
            if (bonusUntil >= ticks && !bonusCaught) {
                bonusCaught = true;
                Msg.action(player, "You hit the sweet spot!");
            } else {
                overworked = true;
                Msg.action(player, "You overwork the preform.");
            }
            return;
        }
        working = tool;
        heating = 0;
        overworked = false;
        int[] at = {0};
        repeat(player, p -> {
            if (isEnded() || working != tool || !poured || finished() || ruined()) {
                return false;
            }
            at[0]++;
            if (at[0] % tool.cycle() != 0) {
                return true;
            }
            if (overworked) {
                overworked = false;
                return true;
            }
            if (currentTool() != tool) {
                mistake(p, "This section needs the " + currentTool().display() + ", not the " + tool.display() + "!");
                return false;
            }
            if (!tool.heat().contains(heat, metalScore)) {
                int[] r = tool.heat().range(metalScore);
                mistake(p, "The preform is too " + (heat < r[0] ? "cold" : "hot") + " for the " + tool.display() + "!");
                return false;
            }
            int section = GiantsFoundry.section(progress, plan.length);
            int gain = tool.progress();
            if (random.nextDouble() < GiantsFoundry.uniformChance(uniformPieces(p))) {
                gain++;
            }
            double xp = tool.xp();
            if (bonusCaught) {
                gain += GiantsFoundry.BONUS_PROGRESS;
                xp *= 2;
                quality = Math.min(startQuality, quality + GiantsFoundry.MISTAKE);
                bonusCaught = false;
                bonusUntil = -1;
            }
            progress = Math.min(GiantsFoundry.MAX, progress + gain);
            heat = Math.max(0, Math.min(GiantsFoundry.MAX, heat + tool.heatChange()));
            xp(p, Skill.SMITHING, xp);
            world.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0);
            maybeOpenBonus(p);
            if (finished()) {
                Msg.game(p, "The sword is complete! Hand it to Kovac.");
                return false;
            }
            int next = GiantsFoundry.section(progress, plan.length);
            if (next != section && plan[next] != tool) {
                Msg.game(p, "That section is done. Next: the " + plan[next].display() + ".");
                return false;
            }
            return true;
        });
    }

    /** Sweet spots open, evenly spread over the sword, right after a machine acts. */
    private void maybeOpenBonus(ServerPlayer p) {
        if (bonusesLeft <= 0 || bonusUntil >= ticks) {
            return;
        }
        int used = bonusesTotal - bonusesLeft;
        int at = GiantsFoundry.MAX * (2 * used + 1) / (2 * bonusesTotal) - GiantsFoundry.MAX / (4 * bonusesTotal); // approx
        if (progress >= at) {
            bonusesLeft--;
            bonusUntil = ticks + GiantsFoundry.BONUS_TICKS;
            bonusCaught = false;
            Msg.game(p, "A sweet spot appears! Click the machine again to catch it.");
        }
    }

    /** Heats (lava pool) or cools (waterfall) the preform, faster the longer it stays in; sneak-click to dunk or quench. */
    private void temperature(ServerPlayer player, int direction) {
        if (!poured || finished() || ruined()) {
            Msg.game(player, "You have no preform to " + (direction > 0 ? "heat" : "cool") + ".");
            return;
        }
        working = null;
        heating = direction;
        int[] step = {player.isShiftKeyDown() ? GiantsFoundry.FAST_START : 0};
        Msg.action(player, direction > 0 ? (step[0] > 0 ? "You dunk the preform in the lava." : "You heat the preform in the lava.")
                : (step[0] > 0 ? "You quench the preform in the waterfall." : "You cool the preform in the waterfall."));
        repeat(player, p -> {
            if (isEnded() || heating != direction || !poured) {
                return false;
            }
            heat = Math.max(0, Math.min(GiantsFoundry.MAX, heat + direction * GiantsFoundry.heatStep(step[0]++)));
            return heat > 0 && heat < GiantsFoundry.MAX;
        });
    }

    private void handIn(ServerPlayer player) {
        if (commission == null) {
            newCommission(player);
            return;
        }
        if (!finished() && !ruined()) {
            Msg.game(player, poured ? "Kovac: That sword isn't finished yet (" + progress / 10 + "%)."
                    : "Kovac: I'm waiting on a " + commission.display().toLowerCase(java.util.Locale.ROOT) + " sword.");
            return;
        }
        com.runecraft.action.ActionManager.cancel(player);
        int q = ruined() ? 0 : quality;
        int xp = GiantsFoundry.xp(q);
        int rep = GiantsFoundry.reputation(q);
        xp(player, Skill.SMITHING, xp);
        reward(player, "coins", GiantsFoundry.coins(q));
        if (rep > 0) {
            addPoints(player, MinigameCurrencies.FOUNDRY, rep);
        }
        swords++;
        int total = MinigameData.of(player).incrementStat(player.getUUID(), "kc:" + game.id());
        Msg.game(player, "Kovac: " + (q > 0 ? "A fine sword!" : "Bah, ruined.") + " Quality " + q + " (" + mistakes + " mistake"
                + (mistakes == 1 ? "" : "s") + "): " + xp + " Smithing XP, " + GiantsFoundry.coins(q) + " coins and " + rep
                + " reputation. Swords made: " + total + ".");
        commission = null;
        poured = false;
        moulds = null;
        progress = 0;
        heat = 0;
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        Tool tool = Tool.byBlock(blockId);
        if (tool != null) {
            work(player, tool, pos);
            return true;
        }
        switch (blockId) {
            case "foundry_crucible" -> fillCrucible(player);
            case "foundry_mould" -> {
                if (commission == null) {
                    Msg.game(player, "Speak to Kovac for a commission before setting the mould.");
                } else if (poured) {
                    Msg.game(player, "The preform has already been poured.");
                } else if (moulds == null) {
                    setMoulds(player);
                } else if (barsIn < GiantsFoundry.CRUCIBLE) {
                    Msg.game(player, "The crucible needs " + GiantsFoundry.CRUCIBLE + " bars' worth of metal before you can pour ("
                            + barsIn + " so far).");
                } else {
                    pour(player);
                }
            }
            case "foundry_lava_pool" -> temperature(player, 1);
            case "foundry_waterfall" -> temperature(player, -1);
            case "foundry_handin" -> handIn(player);
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void gameTick() {
        if (poured && !finished() && heating == 0) {
            heat = Math.max(0, heat - GiantsFoundry.PASSIVE_COOLING);
        }
        for (ServerPlayer p : online()) {
            if (!com.runecraft.action.ActionManager.isBusy(p)) {
                working = null;
                heating = 0;
            }
        }
    }

    @Override
    protected void onLeave(ServerPlayer player) {
        com.runecraft.action.ActionManager.cancel(player);
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        lines.add("Giants' Foundry - reputation " + MinigameData.of(player).points(player.getUUID(), "giants_foundry"));
        lines.add(commission == null ? "Speak to Kovac for a commission" : "Commission: " + commission.display()
                + (moulds == null ? " (set the mould)" : " (moulds " + mouldScore + ")"));
        if (!poured) {
            lines.add("Crucible: " + barsIn + " / " + GiantsFoundry.CRUCIBLE + " bars, metal score " + GiantsFoundry.metalScore(metal));
            return lines;
        }
        if (finished() || ruined()) {
            lines.add(ruined() ? "Ruined: give it to Kovac" : "Finished: hand it to Kovac");
        } else {
            Tool t = currentTool();
            int[] r = t.heat().range(metalScore);
            lines.add("Section " + (GiantsFoundry.section(progress, plan.length) + 1) + "/" + plan.length + ": " + t.display()
                    + " (" + progress / 10 + "%)");
            lines.add("Heat " + heat / 10 + "% (needs " + r[0] / 10 + "-" + r[1] / 10 + "%)"
                    + (bonusUntil >= ticks && !bonusCaught ? "  SWEET SPOT!" : ""));
        }
        lines.add("Quality: " + quality + " / " + startQuality);
        return lines;
    }

    // ------------------------------------------------------------------ self-test hooks

    /** Self-test view: commission, moulds, bars, metal score, poured, sections, progress, heat, quality, swords. */
    public record View(Commission commission, int mouldScore, int barsIn, int metalScore, boolean poured, int sections, int progress,
                       int heat, int quality, int startQuality, Tool tool, boolean bonusOpen, int swords) {
    }

    public View selfTestView() {
        return new View(commission, mouldScore, barsIn, poured ? metalScore : GiantsFoundry.metalScore(metal), poured, plan.length,
                progress, heat, quality, startQuality, poured && !finished() && !ruined() ? currentTool() : null,
                bonusUntil >= ticks && !bonusCaught, swords);
    }

    /** Self-test: the arena position of a block of this game. */
    public BlockPos selfTestBlock(String id) {
        return switch (id) {
            case "foundry_crucible" -> rel(-8, 1, -8);
            case "foundry_mould" -> rel(-2, 1, -8);
            case "foundry_trip_hammer" -> rel(6, 1, -8);
            case "foundry_grindstone" -> rel(9, 1, -3);
            case "foundry_polishing_wheel" -> rel(9, 1, 3);
            case "foundry_lava_pool" -> rel(-10, 1, 2);
            case "foundry_waterfall" -> rel(-10, 1, 6);
            default -> rel(4, 1, 8);
        };
    }
}
