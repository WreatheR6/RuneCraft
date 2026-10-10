package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Trouble Brewing on Mos Le'Harmless: gather bitternuts, scrapey tree bark and sweetgrubs, and
 * brew Karamjan rum in the vat (one of each per bottle) before the game ends. Sabotaging monkeys
 * occasionally spoil the brew. Each bottle earns Cooking XP and pieces of eight.
 */
public class TroubleBrewingSession extends MinigameSession {
    private static final int GAME_TICKS = 600;
    private final Map<UUID, Integer> bottles = new HashMap<>();
    private int teamBottles;

    public TroubleBrewingSession(int id, int slot, ServerWorld world, String variant) {
        super(Minigame.TROUBLE_BREWING, id, slot, world, variant);
    }

    @Override
    protected void build() {
        room(Blocks.SAND, Blocks.SPRUCE_LOG, Blocks.SANDSTONE);
        place(-16, -12, "tb_bitternut_tree");
        place(16, -12, "tb_scrapey_tree");
        place(0, -18, "tb_sweetgrub_mound");
        place(0, 0, "tb_brewing_vat");
        place(0, 22, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayerEntity player, int index) {
        return rel(index % 5 - 2, 1, 18);
    }

    private void gather(ServerPlayerEntity player, String item, Skill skill) {
        repeat(player, p -> {
            if (!Inv.hasRoomFor(p, item)) {
                return false;
            }
            if (random.nextInt(3) == 0) {
                Inv.give(p, item, 1);
                xp(p, skill, 5);
            }
            return true;
        });
    }

    @Override
    protected boolean onBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "tb_bitternut_tree" -> gather(player, "bitternut", Skill.WOODCUTTING);
            case "tb_scrapey_tree" -> gather(player, "scrapey_tree_bark", Skill.WOODCUTTING);
            case "tb_sweetgrub_mound" -> gather(player, "sweetgrubs", Skill.HUNTER);
            case "tb_brewing_vat" -> {
                int n = Math.min(Inv.count(player, "bitternut"), Math.min(Inv.count(player, "scrapey_tree_bark"), Inv.count(player, "sweetgrubs")));
                if (n == 0) {
                    Msg.game(player, "You need a bitternut, some scrapey tree bark and sweetgrubs to brew rum.");
                    return true;
                }
                Inv.remove(player, "bitternut", n);
                Inv.remove(player, "scrapey_tree_bark", n);
                Inv.remove(player, "sweetgrubs", n);
                bottles.merge(player.getUuid(), n, Integer::sum);
                teamBottles += n;
                xp(player, Skill.COOKING, n * level(player, Skill.COOKING) * 1.5);
                Msg.game(player, "You brew " + n + " bottle" + (n > 1 ? "s" : "") + " of Karamjan rum.");
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void gameTick() {
        if (ticks % 90 == 0 && teamBottles > 0 && random.nextInt(3) == 0) {
            teamBottles--;
            broadcastRed("A mischievous monkey knocks over a bottle of rum!");
        }
        if (ticks >= GAME_TICKS) {
            end(true);
        }
    }

    @Override
    protected void onEnd(boolean success) {
        for (ServerPlayerEntity p : online()) {
            int b = bottles.getOrDefault(p.getUuid(), 0);
            Inv.remove(p, "bitternut", 1000);
            Inv.remove(p, "scrapey_tree_bark", 1000);
            Inv.remove(p, "sweetgrubs", 1000);
            if (b == 0) {
                Msg.game(p, "You didn't brew any rum this game.");
                continue;
            }
            int eight = Math.max(1, Math.min(b, teamBottles) * 2);
            reward(p, "pieces_of_eight", eight);
            reward(p, "bottle_of_rum", Math.min(3, b));
            MinigameData.of(p).incrementStat(p.getUuid(), "kc:" + game.id());
            Msg.game(p, "The game is over! You brewed " + b + " bottles and earn " + eight + " pieces of eight.");
        }
    }

    @Override
    protected List<String> hud(ServerPlayerEntity player) {
        return List.of("Trouble Brewing", "Team bottles: " + teamBottles, "Your bottles: " + bottles.getOrDefault(player.getUuid(), 0),
                "Time left: " + time(GAME_TICKS - ticks));
    }
}
