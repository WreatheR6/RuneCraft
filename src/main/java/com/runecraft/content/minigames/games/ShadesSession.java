package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameItems;
import com.runecraft.osrs.content.minigames.MinigameRules.Shades;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.Blocks;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Shades of Mort'ton: fight the shades in the catacombs beneath Mort'ton, burn their remains on a
 * funeral pyre (with logs) for Prayer XP, and use the keys they drop on the shade chest. Shades
 * match your combat level (Loar 40 ... Fiyr 120).
 */
public class ShadesSession extends MinigameSession {
    private static final String[] LOGS = {"logs", "oak_logs", "willow_logs", "maple_logs", "yew_logs", "magic_logs", "teak_logs",
            "mahogany_logs"};

    public ShadesSession(int id, int slot, ServerWorld world, String variant) {
        super(Minigame.SHADES_OF_MORTTON, id, slot, world, variant);
    }

    @Override
    protected void build() {
        room(Blocks.TUFF, Blocks.DEEPSLATE_BRICKS, Blocks.SOUL_SOIL);
        place(-18, 18, "funeral_pyre");
        place(18, 18, "shade_chest");
        place(0, 22, "minigame_exit");
        for (int i = 0; i < 12; i++) {
            int x = random.nextInt(40) - 20;
            int z = random.nextInt(30) - 20;
            set(x, 1, z, Blocks.COBWEB.getDefaultState());
        }
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayerEntity player, int index) {
        return rel(index % 3 - 1, 1, 19);
    }

    private int tier() {
        int best = 0;
        for (ServerPlayerEntity p : online()) {
            int cmb = com.runecraft.player.Players.data(p).skills().combatLevel();
            best = Math.max(best, Math.min(4, Math.max(0, (cmb - 30) / 20)));
        }
        return best;
    }

    @Override
    protected void gameTick() {
        if (ticks % 15 == 0 && mobsAlive() < 3 + playerCount()) {
            int t = Math.max(0, tier() - random.nextInt(2));
            MobEntity shade = spawn(MinigameItems.SHADES[t][0] + "_shade", randomFloor(0, -8, 12));
            if (shade != null) {
                aggro(shade);
            }
        }
    }

    @Override
    protected boolean onBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "funeral_pyre" -> {
                String log = null;
                for (String l : LOGS) {
                    if (Inv.has(player, l, 1)) {
                        log = l;
                        break;
                    }
                }
                if (log == null) {
                    Msg.game(player, "You need logs to build a funeral pyre.");
                    return true;
                }
                for (int i = MinigameItems.SHADES.length - 1; i >= 0; i--) {
                    String remains = MinigameItems.SHADES[i][0] + "_remains";
                    if (Inv.remove(player, remains, 1) > 0) {
                        Inv.remove(player, log, 1);
                        xp(player, Skill.PRAYER, (Double) MinigameItems.SHADES[i][3]);
                        xp(player, Skill.FIREMAKING, 10 + i * 10);
                        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 20, 0.3, 0.5, 0.3, 0.02);
                        Msg.game(player, "You lay the " + MinigameItems.SHADES[i][1] + " remains to rest.");
                        return true;
                    }
                }
                Msg.game(player, "You have no shade remains to burn.");
                return true;
            }
            case "shade_chest" -> {
                for (int i = Shades.KEYS.length - 1; i >= 0; i--) {
                    if (Inv.remove(player, Shades.KEYS[i], 1) > 0) {
                        for (int r = 0; r <= i; r++) {
                            rollLoot(player, Shades.CHEST);
                        }
                        Msg.game(player, "You unlock the chest.");
                        return true;
                    }
                }
                Msg.game(player, "The chest is locked. Shades sometimes drop keys.");
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    protected List<String> hud(ServerPlayerEntity player) {
        int remains = 0;
        for (Object[] s : MinigameItems.SHADES) {
            remains += Inv.count(player, s[0] + "_remains");
        }
        return List.of("Shades of Mort'ton", "Shades: " + mobsAlive(), "Remains carried: " + remains);
    }
}
