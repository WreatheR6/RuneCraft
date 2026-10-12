package com.runecraft.content.minigames.games;

import com.runecraft.combat.PoisonService;
import com.runecraft.action.ActionManager;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameItems;
import com.runecraft.osrs.content.minigames.MinigameRules.PyramidPlunder;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pyramid Plunder in Jalsavrah: five minutes to loot up to eight tomb rooms (Thieving 21/31/.../91).
 * Urns give artefacts and Thieving XP but may hide snakes; the Grand Gold Chest gives artefacts and
 * a chance of the Pharaoh's sceptre; the sarcophagus needs Strength. Simon Templeton buys artefacts.
 */
public class PyramidPlunderSession extends MinigameSession {
    private static final int[][] URNS = {{-6, -6}, {-2, -7}, {2, -7}, {6, -6}, {-7, 0}, {7, 0}};
    private int room = 1;
    private final Set<Long> looted = new HashSet<>();
    private boolean chestOpened;
    private boolean sarcophagusOpened;

    public PyramidPlunderSession(int id, int slot, ServerWorld world, String variant) {
        super(Minigame.PYRAMID_PLUNDER, id, slot, world, variant);
    }

    @Override
    protected int halfX() {
        return 9;
    }

    @Override
    protected int halfZ() {
        return 9;
    }

    @Override
    protected int height() {
        return 7;
    }

    @Override
    protected void build() {
        room(Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.CHISELED_SANDSTONE);
        furnish();
    }

    private void furnish() {
        for (int[] u : URNS) {
            place(u[0], u[1], "pp_urn");
        }
        place(-4, 3, "pp_chest");
        place(4, 3, "pp_sarcophagus");
        place(0, -8, "pp_door");
        place(0, 8, "minigame_exit");
        looted.clear();
        chestOpened = false;
        sarcophagusOpened = false;
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayerEntity player, int index) {
        return rel(index % 3 - 1, 1, 6);
    }

    private String artefact() {
        return (String) MinigameItems.ARTEFACTS[PyramidPlunder.artefact(room, random.nextDouble())][0];
    }

    @Override
    protected boolean onBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "pp_urn" -> {
                if (!looted.add(pos.asLong())) {
                    Msg.game(player, "You've already looted this urn.");
                    return true;
                }
                int thieving = level(player, Skill.THIEVING);
                int chance = Math.min(95, 50 + (thieving - PyramidPlunder.ROOM_LEVELS[room - 1]) * 2);
                if (random.nextInt(100) < chance) {
                    reward(player, artefact(), 1);
                    xp(player, Skill.THIEVING, PyramidPlunder.URN_XP[room - 1]);
                    Msg.game(player, "You loot the urn.");
                } else {
                    Msg.red(player, "A snake bites you!");
                    hurt(player, 1 + random.nextInt(4));
                    PoisonService.poison(player, 2, ActionManager.gameTick());
                }
                set(pos.getX() - origin.getX(), 1, pos.getZ() - origin.getZ(), Blocks.DECORATED_POT.getDefaultState());
                return true;
            }
            case "pp_chest" -> {
                if (chestOpened) {
                    Msg.game(player, "The chest is empty.");
                    return true;
                }
                chestOpened = true;
                reward(player, artefact(), 1);
                reward(player, artefact(), 1);
                xp(player, Skill.THIEVING, PyramidPlunder.CHEST_XP[room - 1]);
                if (random.nextInt(PyramidPlunder.SCEPTRE_CHANCE[room - 1]) == 0) {
                    reward(player, "pharaohs_sceptre", 1);
                    player.getServer().getPlayerManager().broadcast(Text.literal(player.getName().getString()
                            + " has found the Pharaoh's sceptre!").formatted(Formatting.GOLD), false);
                }
                if (random.nextInt(5) == 0) {
                    Msg.red(player, "A swarm of scarabs pours out of the chest!");
                    hurt(player, 2 + random.nextInt(4));
                }
                return true;
            }
            case "pp_sarcophagus" -> {
                if (sarcophagusOpened) {
                    Msg.game(player, "The sarcophagus is empty.");
                    return true;
                }
                int str = level(player, Skill.STRENGTH);
                if (random.nextInt(100) < 30 + str / 2) {
                    sarcophagusOpened = true;
                    reward(player, artefact(), 1);
                    xp(player, Skill.STRENGTH, PyramidPlunder.SARCOPHAGUS_XP[room - 1]);
                    Msg.game(player, "You heave the lid off the sarcophagus.");
                } else {
                    Msg.game(player, "You fail to move the lid.");
                }
                return true;
            }
            case "pp_door" -> {
                if (room >= 8) {
                    Msg.game(player, "This is the final room of the pyramid.");
                    return true;
                }
                int need = PyramidPlunder.ROOM_LEVELS[room];
                if (level(player, Skill.THIEVING) < need) {
                    Msg.game(player, "You need a Thieving level of " + need + " to enter the next room.");
                    return true;
                }
                room++;
                build();
                for (ServerPlayerEntity p : online()) {
                    BlockPos at = spawnPoint(p, 0);
                    p.teleport(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYaw(), p.getPitch());
                }
                broadcast("You enter room " + room + " of the pyramid.");
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    protected void gameTick() {
        if (ticks >= PyramidPlunder.GAME_TICKS) {
            broadcast("Your time in the pyramid is up! The mummy guides you out.");
            end(true);
        }
    }

    @Override
    protected void onEnd(boolean success) {
        for (ServerPlayerEntity p : online()) {
            MinigameData.of(p).incrementStat(p.getUuid(), "kc:" + game.id());
        }
    }

    @Override
    protected List<String> hud(ServerPlayerEntity player) {
        return List.of("Pyramid Plunder", "Room: " + room + " / 8", "Time left: " + time(PyramidPlunder.GAME_TICKS - ticks),
                "Thieving level: " + level(player, Skill.THIEVING));
    }

    /** Simon Templeton buys every artefact in the inventory. */
    public static void sellArtefacts(ServerPlayerEntity player) {
        int total = 0;
        for (Object[] a : MinigameItems.ARTEFACTS) {
            int n = Inv.remove(player, (String) a[0], 1000);
            total += n * (Integer) a[2];
        }
        if (total == 0) {
            Msg.game(player, "Simon Templeton: Bring me artefacts from the pyramids and I'll pay you handsomely!");
            return;
        }
        Inv.give(player, "coins", total);
        Msg.game(player, "Simon Templeton pays you " + String.format("%,d", total) + " coins for your artefacts.");
    }
}
