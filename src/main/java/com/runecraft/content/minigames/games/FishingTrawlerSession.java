package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameRules.Trawler;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fishing Trawler: keep the trawler afloat while the net fills. Leaks spring in the hull (patch
 * them with swamp paste) and water rises with every open leak (bail it out with the bucket). If
 * the water fills the hold the boat sinks; otherwise everyone shares the catch, with a chance of a
 * piece of the angler outfit.
 */
public class FishingTrawlerSession extends MinigameSession {
    private final List<BlockPos> leaks = new ArrayList<>();
    private final Map<UUID, Integer> contribution = new HashMap<>();
    private int water;

    public FishingTrawlerSession(int id, int slot, ServerWorld world, String variant) {
        super(Minigame.FISHING_TRAWLER, id, slot, world, variant);
    }

    @Override
    protected int halfX() {
        return 7;
    }

    @Override
    protected int halfZ() {
        return 16;
    }

    @Override
    protected int height() {
        return 7;
    }

    @Override
    protected void build() {
        room(Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.STRIPPED_SPRUCE_LOG);
        place(0, -14, "trawler_bucket");
        place(0, 15, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayerEntity player, int index) {
        return rel(index % 3 - 1, 1, 12);
    }

    @Override
    protected void start() {
        for (ServerPlayerEntity p : online()) {
            Inv.give(p, "swamp_paste", 15);
        }
        broadcast("Murphy: All aboard! Keep her afloat, lads!");
    }

    private void add(ServerPlayerEntity p, int amount) {
        contribution.merge(p.getUuid(), amount, Integer::sum);
    }

    @Override
    protected boolean onBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "trawler_leak" -> {
                if (Inv.remove(player, "swamp_paste", 1) == 0) {
                    Msg.game(player, "You need swamp paste to patch the leak.");
                    return true;
                }
                leaks.removeIf(l -> l.equals(pos));
                world.setBlockState(pos, Blocks.DARK_OAK_PLANKS.getDefaultState());
                add(player, 10);
                Msg.action(player, "You patch the leak.");
            }
            case "trawler_bucket" -> repeat(player, p -> {
                if (water <= 0 || isEnded()) {
                    return false;
                }
                water = Math.max(0, water - 3);
                add(p, 2);
                return true;
            });
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void gameTick() {
        if (ticks % Trawler.LEAK_INTERVAL == 0 && leaks.size() < 8) {
            int side = random.nextBoolean() ? -8 : 8;
            BlockPos pos = rel(side, 1 + random.nextInt(2), random.nextInt(27) - 13);
            world.setBlockState(pos, com.runecraft.content.minigames.Arena.module("trawler_leak"));
            leaks.add(pos);
            broadcast("A leak springs in the hull!");
        }
        if (ticks % 3 == 0) {
            water = Math.min(Trawler.MAX_WATER, water + leaks.size());
        }
        if (water >= Trawler.MAX_WATER) {
            broadcastRed("The trawler sinks! You swim back to shore with nothing.");
            end(false);
            return;
        }
        if (ticks >= Trawler.GAME_TICKS) {
            end(true);
        }
    }

    @Override
    protected void onEnd(boolean success) {
        for (ServerPlayerEntity p : online()) {
            Inv.remove(p, "swamp_paste", 1000);
            if (!success) {
                continue;
            }
            int c = contribution.getOrDefault(p.getUuid(), 0);
            if (c < 20) {
                Msg.game(p, "Murphy: You barely lifted a finger! No share of the catch for you.");
                continue;
            }
            int fishing = level(p, Skill.FISHING);
            int fish = Math.min(50, c / 6);
            reward(p, Trawler.fishFor(fishing), fish);
            xp(p, Skill.FISHING, fish * fishing * 0.4);
            if (random.nextInt(4) == 0) {
                List<String> missing = new ArrayList<>();
                for (String piece : Trawler.OUTFIT) {
                    if (!Inv.has(p, piece, 1)) {
                        missing.add(piece);
                    }
                }
                String piece = missing.isEmpty() ? Trawler.OUTFIT[random.nextInt(4)] : missing.get(random.nextInt(missing.size()));
                reward(p, piece, 1);
                Msg.game(p, "You find a piece of the angler outfit in the net!");
            }
            MinigameData.of(p).incrementStat(p.getUuid(), "kc:" + game.id());
            Msg.game(p, "The trawler returns to port. You receive " + fish + " fish from the net.");
        }
    }

    @Override
    protected List<String> hud(ServerPlayerEntity player) {
        return List.of("Fishing Trawler", "Water level: " + water + "%", "Leaks: " + leaks.size(),
                "Contribution: " + contribution.getOrDefault(player.getUuid(), 0), "Time left: " + time(Trawler.GAME_TICKS - ticks));
    }
}
