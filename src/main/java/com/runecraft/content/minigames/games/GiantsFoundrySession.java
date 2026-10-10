package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameCurrencies;
import com.runecraft.osrs.content.minigames.MinigameRules.GiantsFoundry;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Giants' Foundry: fill the crucible with 14 bars (better metals make better swords), pour it into
 * the mould, then work the sword through six stages at the trip hammer (hot), grindstone (warm)
 * and polishing wheel (cool) while managing its heat with the lava pool and waterfall. Working at
 * the wrong temperature costs quality. Hand the sword to Kovac for reputation and Smithing XP.
 */
public class GiantsFoundrySession extends MinigameSession {
    private static final String[] TOOLS = {"foundry_trip_hammer", "foundry_grindstone", "foundry_polishing_wheel"};
    private static final String[] TOOL_NAMES = {"trip hammer", "grindstone", "polishing wheel"};
    private int bars;
    private int metalTotal;
    private boolean poured;
    private int heat = 50;
    private int stage;
    private int progress;
    private int quality = 100;
    private int[] plan = new int[GiantsFoundry.STAGES];
    private int swords;

    public GiantsFoundrySession(int id, int slot, ServerWorld world, String variant) {
        super(Minigame.GIANTS_FOUNDRY, id, slot, world, variant);
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
        place(6, -8, "foundry_trip_hammer");
        place(9, -3, "foundry_grindstone");
        place(9, 3, "foundry_polishing_wheel");
        place(-10, 2, "foundry_lava_pool");
        place(-10, 6, "foundry_waterfall");
        place(4, 8, "foundry_handin");
        place(0, 13, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayerEntity player, int index) {
        return rel(index % 3 - 1, 1, 10);
    }

    private void newCommission() {
        bars = 0;
        metalTotal = 0;
        poured = false;
        stage = 0;
        progress = 0;
        quality = 100;
        for (int i = 0; i < plan.length; i++) {
            plan[i] = random.nextInt(3);
        }
    }

    @Override
    protected void start() {
        newCommission();
        broadcast("Kovac: Fill the crucible with 14 bars and make me a giant's sword!");
    }

    private static int[] range(int tool) {
        return switch (tool) {
            case 0 -> new int[]{66, 100};
            case 1 -> new int[]{33, 66};
            default -> new int[]{0, 33};
        };
    }

    @Override
    protected boolean onBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "foundry_crucible" -> {
                if (poured) {
                    Msg.game(player, "The crucible is empty; finish the sword first.");
                    return true;
                }
                for (int i = GiantsFoundry.BARS.length - 1; i >= 0 && bars < GiantsFoundry.BARS_NEEDED; i--) {
                    int n = Inv.remove(player, GiantsFoundry.BARS[i], GiantsFoundry.BARS_NEEDED - bars);
                    bars += n;
                    metalTotal += n * GiantsFoundry.BAR_VALUE[i];
                }
                Msg.game(player, "The crucible holds " + bars + " / " + GiantsFoundry.BARS_NEEDED + " bars.");
            }
            case "foundry_mould" -> {
                if (poured) {
                    Msg.game(player, "The sword is already cast.");
                } else if (bars < GiantsFoundry.BARS_NEEDED) {
                    Msg.game(player, "The crucible needs " + GiantsFoundry.BARS_NEEDED + " bars before you can pour.");
                } else {
                    poured = true;
                    heat = 90;
                    Msg.game(player, "You pour the molten metal into the mould. Next: the " + TOOL_NAMES[plan[0]] + ".");
                }
            }
            case "foundry_lava_pool" -> {
                heat = Math.min(100, heat + 20);
                Msg.action(player, "You heat the sword in the lava. Heat: " + heat);
            }
            case "foundry_waterfall" -> {
                heat = Math.max(0, heat - 20);
                Msg.action(player, "You cool the sword in the waterfall. Heat: " + heat);
            }
            case "foundry_trip_hammer", "foundry_grindstone", "foundry_polishing_wheel" -> work(player, blockId, pos);
            case "foundry_handin" -> {
                if (!poured || stage < GiantsFoundry.STAGES) {
                    Msg.game(player, "Kovac: That sword isn't finished yet.");
                    return true;
                }
                int metalScore = Math.max(1, metalTotal / GiantsFoundry.BARS_NEEDED);
                int rep = GiantsFoundry.reputation(quality, metalScore);
                for (ServerPlayerEntity p : online()) {
                    addPoints(p, MinigameCurrencies.FOUNDRY, rep);
                    xp(p, Skill.SMITHING, GiantsFoundry.xp(quality, metalScore));
                    reward(p, "coins", rep * 10);
                    Msg.game(p, "Kovac: A fine sword! Quality " + quality + ". You earn " + rep + " reputation.");
                }
                swords++;
                newCommission();
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void work(ServerPlayerEntity player, String blockId, BlockPos pos) {
        if (!poured || stage >= GiantsFoundry.STAGES) {
            Msg.game(player, poured ? "The sword is finished. Hand it to Kovac." : "There's no sword to work on.");
            return;
        }
        int tool = java.util.Arrays.asList(TOOLS).indexOf(blockId);
        if (tool != plan[stage]) {
            Msg.game(player, "This stage needs the " + TOOL_NAMES[plan[stage]] + ".");
            return;
        }
        int[] r = range(tool);
        if (heat < r[0] || heat > r[1]) {
            quality = Math.max(0, quality - 4);
            Msg.red(player, "The sword is " + (heat < r[0] ? "too cold" : "too hot") + " to work here! Its quality suffers.");
            return;
        }
        progress++;
        heat = Math.max(0, heat - 3);
        xp(player, Skill.SMITHING, 6);
        world.spawnParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0);
        if (progress >= 5) {
            progress = 0;
            stage++;
            broadcast(stage >= GiantsFoundry.STAGES ? "The sword is complete! Hand it to Kovac."
                    : "Stage " + stage + " done. Next: the " + TOOL_NAMES[plan[stage]] + ".");
        }
    }

    @Override
    protected void gameTick() {
        if (poured && ticks % 3 == 0) {
            heat = Math.max(0, heat - 1);
        }
    }

    @Override
    protected void onLeave(ServerPlayerEntity player) {
        MinigameData.of(player).setStat(player.getUuid(), "kc:" + game.id(), MinigameData.of(player).stat(player.getUuid(), "kc:" + game.id()) + swords);
    }

    @Override
    protected List<String> hud(ServerPlayerEntity player) {
        String step = !poured ? "Crucible: " + bars + " / " + GiantsFoundry.BARS_NEEDED + " bars"
                : stage >= GiantsFoundry.STAGES ? "Hand in to Kovac" : "Stage " + (stage + 1) + "/6: " + TOOL_NAMES[plan[stage]] + " (" + progress + "/5)";
        return List.of("Giants' Foundry", step, "Heat: " + heat + "  Quality: " + quality,
                "Reputation: " + MinigameData.of(player).points(player.getUuid(), "giants_foundry"));
    }
}
