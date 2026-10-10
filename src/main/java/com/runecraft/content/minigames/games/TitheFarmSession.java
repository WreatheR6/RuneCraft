package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameCurrencies;
import com.runecraft.osrs.content.minigames.MinigameItems;
import com.runecraft.osrs.content.minigames.MinigameRules.TitheFarm;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tithe Farm in Hosidius: plant the seed for your Farming level (golovanova 34, bologano 54,
 * logavano 74), water it at each of its three growth stages (it dies if left dry), harvest the
 * fruit and deposit it in the sack for points and bonus Farming XP.
 */
public class TitheFarmSession extends MinigameSession {
    private static final int PATCHES = 12;
    /** Per patch: seed type (-1 empty), stage (0..2 growing, 4 = ready, -2 dead) and whether it was watered this stage. */
    private final int[] type = new int[PATCHES];
    private final int[] stage = new int[PATCHES];
    private final boolean[] watered = new boolean[PATCHES];
    private final long[] stageAt = new long[PATCHES];
    private final Map<UUID, Integer> water = new HashMap<>();
    private final Map<UUID, Integer> delivered = new HashMap<>();

    public TitheFarmSession(int id, int slot, ServerWorld world, String variant) {
        super(Minigame.TITHE_FARM, id, slot, world, variant);
        java.util.Arrays.fill(type, -1);
    }

    @Override
    protected int halfX() {
        return 14;
    }

    @Override
    protected int halfZ() {
        return 14;
    }

    private int[] patchPos(int i) {
        return new int[]{-9 + (i % 4) * 6, -9 + (i / 4) * 6};
    }

    @Override
    protected void build() {
        room(Blocks.GRASS_BLOCK, Blocks.SPRUCE_PLANKS, Blocks.COARSE_DIRT);
        for (int i = 0; i < PATCHES; i++) {
            int[] p = patchPos(i);
            place(p[0], p[1], "tithe_patch");
        }
        place(-12, 10, "tithe_seed_table");
        place(-8, 10, "tithe_water_barrel");
        place(8, 10, "tithe_sack");
        place(0, 13, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayerEntity player, int index) {
        return rel(index % 3 - 1, 1, 11);
    }

    private int patchAt(BlockPos pos) {
        BlockPos r = pos.subtract(origin);
        for (int i = 0; i < PATCHES; i++) {
            int[] p = patchPos(i);
            if (p[0] == r.getX() && p[1] == r.getZ()) {
                return i;
            }
        }
        return -1;
    }

    private void show(int i) {
        int[] p = patchPos(i);
        Block b = type[i] < 0 ? Blocks.AIR : stage[i] == -2 ? Blocks.BROWN_CARPET : stage[i] >= 4 ? Blocks.FLOWERING_AZALEA_LEAVES
                : stage[i] >= 2 ? Blocks.AZALEA_LEAVES : Blocks.MOSS_CARPET;
        net.minecraft.block.BlockState state = b.getDefaultState();
        if (b instanceof net.minecraft.block.LeavesBlock) {
            state = state.with(net.minecraft.block.LeavesBlock.PERSISTENT, true);
        }
        set(p[0], 2, p[1], state);
    }

    @Override
    protected boolean onBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "tithe_seed_table" -> {
                int s = TitheFarm.seedFor(level(player, Skill.FARMING));
                if (s < 0) {
                    Msg.game(player, "You need a Farming level of 34 to plant Tithe Farm seeds.");
                    return true;
                }
                Inv.give(player, MinigameItems.TITHE_PLANTS[s] + "_seed", 10);
                Msg.game(player, "You take some " + MinigameItems.TITHE_PLANTS[s] + " seeds.");
            }
            case "tithe_water_barrel" -> {
                water.put(player.getUuid(), Inv.has(player, "gricollers_can", 1) ? 1000 : 40);
                Msg.game(player, "You fill your watering can.");
            }
            case "tithe_patch" -> patch(player, patchAt(pos));
            case "tithe_sack" -> {
                int fruit = 0;
                double bonusXp = 0;
                for (int t = 0; t < 3; t++) {
                    int n = Inv.remove(player, MinigameItems.TITHE_PLANTS[t] + "_fruit", 1000);
                    fruit += n;
                    bonusXp += n * TitheFarm.DEPOSIT_XP[t];
                }
                if (fruit == 0) {
                    Msg.game(player, "You have no fruit to deposit.");
                    return true;
                }
                int pts = TitheFarm.points(fruit);
                addPoints(player, MinigameCurrencies.TITHE, pts);
                xp(player, Skill.FARMING, bonusXp);
                delivered.merge(player.getUuid(), fruit, Integer::sum);
                Msg.game(player, "You deposit " + fruit + " fruit and earn " + pts + " points.");
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void patch(ServerPlayerEntity player, int i) {
        if (i < 0) {
            return;
        }
        if (type[i] < 0) {
            for (int t = 2; t >= 0; t--) {
                if (Inv.has(player, MinigameItems.TITHE_PLANTS[t] + "_seed", 1)) {
                    if (level(player, Skill.FARMING) < MinigameItems.TITHE_LEVELS[t]) {
                        continue;
                    }
                    Inv.remove(player, MinigameItems.TITHE_PLANTS[t] + "_seed", 1);
                    type[i] = t;
                    stage[i] = 0;
                    watered[i] = false;
                    stageAt[i] = ticks;
                    xp(player, Skill.FARMING, TitheFarm.PLANT_XP[t]);
                    show(i);
                    Msg.action(player, "You plant the seed. Water it!");
                    return;
                }
            }
            Msg.game(player, "You need a seed from the seed table.");
            return;
        }
        if (stage[i] == -2) {
            type[i] = -1;
            show(i);
            Msg.game(player, "You clear the blighted plant.");
            return;
        }
        if (stage[i] >= 4) {
            int t = type[i];
            Inv.give(player, MinigameItems.TITHE_PLANTS[t] + "_fruit", 3);
            xp(player, Skill.FARMING, TitheFarm.HARVEST_XP[t]);
            type[i] = -1;
            show(i);
            Msg.action(player, "You harvest some fruit.");
            return;
        }
        if (watered[i]) {
            Msg.action(player, "This plant has already been watered.");
            return;
        }
        int left = water.getOrDefault(player.getUuid(), 0);
        if (left <= 0) {
            Msg.game(player, "Your watering can is empty. Fill it at the water barrel.");
            return;
        }
        water.put(player.getUuid(), left - 1);
        watered[i] = true;
        Msg.action(player, "You water the plant.");
    }

    @Override
    protected void gameTick() {
        for (int i = 0; i < PATCHES; i++) {
            if (type[i] < 0 || stage[i] < 0 || stage[i] >= 4) {
                continue;
            }
            if (ticks - stageAt[i] >= TitheFarm.GROW_TICKS) {
                stageAt[i] = ticks;
                if (!watered[i]) {
                    stage[i] = -2;
                } else {
                    stage[i]++;
                    watered[i] = false;
                    if (stage[i] >= TitheFarm.WATERINGS) {
                        stage[i] = 4;
                    }
                }
                show(i);
            }
        }
    }

    @Override
    protected void onLeave(ServerPlayerEntity player) {
        for (int t = 0; t < 3; t++) {
            Inv.remove(player, MinigameItems.TITHE_PLANTS[t] + "_seed", 1000);
            Inv.remove(player, MinigameItems.TITHE_PLANTS[t] + "_fruit", 1000);
        }
        MinigameData.of(player).setStat(player.getUuid(), "kc:" + game.id(),
                MinigameData.of(player).stat(player.getUuid(), "kc:" + game.id()) + delivered.getOrDefault(player.getUuid(), 0));
    }

    @Override
    protected List<String> hud(ServerPlayerEntity player) {
        return List.of("Tithe Farm", "Points: " + MinigameData.of(player).points(player.getUuid(), "tithe_farm"),
                "Fruit deposited: " + delivered.getOrDefault(player.getUuid(), 0),
                "Watering can: " + water.getOrDefault(player.getUuid(), 0) + " uses");
    }
}
