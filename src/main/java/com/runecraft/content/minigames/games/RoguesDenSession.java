package com.runecraft.content.minigames.games;

import com.runecraft.collection.CollectionLog;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameRules.RoguesDen;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The Rogues' Den maze beneath Burthorpe: cross eight obstacles in order (Agility and Thieving
 * checks alternate; failing hurts and sends you back to the start), then crack the wall safe for a
 * mystic jewel. Brian O'Richard trades the jewel for a piece of rogue equipment.
 */
public class RoguesDenSession extends MinigameSession {
    private static final Random RANDOM = new Random();
    private int next;

    public RoguesDenSession(int id, int slot, ServerWorld world, String variant) {
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
    protected BlockPos spawnPoint(ServerPlayerEntity player, int index) {
        return rel(0, 1, 19);
    }

    @Override
    protected boolean onBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "rd_obstacle" -> {
                int index = (16 - (pos.getZ() - origin.getZ())) / 4;
                if (index != next) {
                    Msg.game(player, next < RoguesDen.OBSTACLES.length ? "You should tackle the " + RoguesDen.OBSTACLES[next] + " next."
                            : "Head for the wall safe.");
                    return true;
                }
                Skill skill = index % 2 == 0 ? Skill.AGILITY : Skill.THIEVING;
                if (random.nextDouble() < RoguesDen.passChance(level(player, skill))) {
                    next++;
                    xp(player, skill, 60);
                    Msg.game(player, "You get past the " + RoguesDen.OBSTACLES[index].toLowerCase() + ".");
                } else {
                    hurt(player, 2 + random.nextInt(5));
                    next = 0;
                    Msg.red(player, "You fail the " + RoguesDen.OBSTACLES[index].toLowerCase() + " and are thrown back to the start!");
                    BlockPos at = spawnPoint(player, 0);
                    player.teleport(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, player.getYaw(), player.getPitch());
                }
            }
            case "rd_safe" -> {
                if (next < RoguesDen.OBSTACLES.length) {
                    Msg.game(player, "You need to get through the maze first.");
                    return true;
                }
                if (random.nextDouble() < RoguesDen.passChance(level(player, Skill.THIEVING))) {
                    reward(player, "mystic_jewel", 1);
                    xp(player, Skill.THIEVING, 300);
                    xp(player, Skill.AGILITY, 300);
                    MinigameData.of(player).incrementStat(player.getUuid(), "kc:" + game.id());
                    Msg.game(player, "You crack the safe and find a mystic jewel!");
                    end(true);
                } else {
                    hurt(player, 1 + random.nextInt(3));
                    Msg.game(player, "You fail to crack the safe and trigger a trap.");
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

    @Override
    protected List<String> hud(ServerPlayerEntity player) {
        return List.of("Rogues' Den", next < RoguesDen.OBSTACLES.length ? "Next: " + RoguesDen.OBSTACLES[next] : "Crack the wall safe!",
                "Obstacles passed: " + next + " / " + RoguesDen.OBSTACLES.length);
    }

    /** Brian O'Richard: a mystic jewel for a piece of rogue equipment you don't have yet. */
    public static void tradeJewel(ServerPlayerEntity player) {
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
