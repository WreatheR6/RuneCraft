package com.runecraft.content.minigames.games;

import com.runecraft.collection.CollectionLog;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameRules.RoguesDen;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

/**
 * The Rogues' Den maze beneath Burthorpe (50 Agility and 50 Thieving): cross eight obstacles in
 * order. Agility obstacles drain Agility and Thieving traps drain Thieving; nothing in the maze
 * deals damage, but a failed trap or running out of levels throws you out. Crack the wall safe for
 * a mystic jewel, which Brian O'Richard trades for a piece of rogue equipment. Leaving pays 12 XP
 * per level drained from each skill (the full amount only after cracking the safe).
 */
public class RoguesDenSession extends MinigameSession {
    private static final Random RANDOM = new Random();
    private int next;
    /** Levels drained so far this run: Agility, Thieving. */
    private final int[] drained = new int[2];
    private boolean cracked;
    private boolean paid;

    public RoguesDenSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.ROGUES_DEN, id, slot, world, variant);
    }

    @Override
    protected int halfX() {
        return 5;
    }

    @Override
    protected int halfZ() {
        return 22;
    }

    @Override
    protected int height() {
        return 6;
    }

    @Override
    protected void build() {
        room(Blocks.COBBLED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.POLISHED_DEEPSLATE);
        for (int i = 0; i < RoguesDen.OBSTACLES.length; i++) {
            place(i % 2 == 0 ? -2 : 2, 16 - i * 4, "rd_obstacle");
        }
        place(0, -20, "rd_safe");
        place(0, 21, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return rel(0, 1, 19);
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "rd_obstacle" -> {
                int index = (16 - (pos.getZ() - origin.getZ())) / 4;
                if (index != next) {
                    Msg.game(player, next < RoguesDen.OBSTACLES.length ? "You should tackle the " + RoguesDen.OBSTACLES[next] + " next."
                            : "Head for the wall safe.");
                    return true;
                }
                boolean agility = RoguesDen.AGILITY[index];
                Skill skill = agility ? Skill.AGILITY : Skill.THIEVING;
                int s = agility ? 0 : 1;
                int remaining = level(player, skill) - drained[s];
                boolean pass = remaining >= RoguesDen.DRAIN[index]
                        && (agility || random.nextDouble() < RoguesDen.passChance(remaining));
                if (pass) {
                    next++;
                    drained[s] += RoguesDen.DRAIN[index];
                    Msg.game(player, "You get past the " + RoguesDen.OBSTACLES[index].toLowerCase() + ".");
                } else {
                    Msg.red(player, remaining < RoguesDen.DRAIN[index]
                            ? "You're too worn out to tackle the " + RoguesDen.OBSTACLES[index].toLowerCase() + " and leave the maze."
                            : "You set off the " + RoguesDen.OBSTACLES[index].toLowerCase() + " and are thrown out of the maze!");
                    end(false);
                }
            }
            case "rd_safe" -> {
                if (next < RoguesDen.OBSTACLES.length) {
                    Msg.game(player, "You need to get through the maze first.");
                    return true;
                }
                if (random.nextDouble() < RoguesDen.passChance(Math.max(1, level(player, Skill.THIEVING) - drained[1]))) {
                    reward(player, "mystic_jewel", 1);
                    cracked = true;
                    MinigameData.of(player).incrementStat(player.getUUID(), "kc:" + game.id());
                    Msg.game(player, "You crack the safe and find a mystic jewel!");
                    end(true);
                } else if (random.nextBoolean()) {
                    Msg.red(player, "You fail to crack the safe and are ejected from the maze.");
                    end(false);
                } else {
                    drained[1]++;
                    Msg.game(player, "You fumble the lock; your Thieving is drained a little.");
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void gameTick() {
    }

    /** Leaving the maze (cracked, failed or walked out): 12 XP per drained level, scaled by progress unless cracked. */
    private void pay(ServerPlayer player) {
        if (paid) {
            return;
        }
        paid = true;
        double agility = RoguesDen.xp(drained[0], next, cracked);
        double thieving = RoguesDen.xp(drained[1], next, cracked);
        if (agility > 0) {
            xp(player, Skill.AGILITY, agility);
        }
        if (thieving > 0) {
            xp(player, Skill.THIEVING, thieving);
        }
    }

    @Override
    protected void onEnd(boolean success) {
        for (ServerPlayer p : online()) {
            pay(p);
        }
    }

    @Override
    protected void onLeave(ServerPlayer player) {
        pay(player);
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        return List.of("Rogues' Den", next < RoguesDen.OBSTACLES.length ? "Next: " + RoguesDen.OBSTACLES[next] : "Crack the wall safe!",
                "Obstacles passed: " + next + " / " + RoguesDen.OBSTACLES.length);
    }

    /** Brian O'Richard: a mystic jewel for a piece of rogue equipment you don't have yet. */
    public static void tradeJewel(ServerPlayer player) {
        if (Inv.remove(player, "mystic_jewel", 1) == 0) {
            Msg.game(player, "Brian O'Richard: Bring me a mystic jewel from the maze and I'll reward you with rogue equipment.");
            return;
        }
        List<String> missing = new ArrayList<>();
        for (String piece : RoguesDen.OUTFIT) {
            if (!Inv.has(player, piece, 1)) {
                missing.add(piece);
            }
        }
        if (missing.isEmpty()) {
            Inv.give(player, "coins", 500);
            Msg.game(player, "Brian O'Richard: You've got the whole set already! Here's some coin instead.");
            return;
        }
        String piece = missing.get(RANDOM.nextInt(missing.size()));
        Inv.give(player, piece, 1);
        CollectionLog.obtain(player, piece);
        Msg.game(player, "Brian O'Richard hands you a piece of rogue equipment.");
    }
}
