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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Tithe Farm in Hosidius: plant the seed for your Farming level (golovanova 34, bologano 54,
 * logavano 74), water it at each of its three growth stages (it dies if left dry), harvest the
 * fruit and deposit it in the sack for Farming XP (10x the harvest XP per fruit, doubled for fruit
 * 75-100 plus a bonus on the 75th) and a point for every third fruit (35 for a full sack of 100).
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

    public TitheFarmSession(int id, int slot, ServerLevel world, String variant) {
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
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
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
        net.minecraft.world.level.block.state.BlockState state = b.defaultBlockState();
        if (b instanceof net.minecraft.world.level.block.LeavesBlock) {
            state = state.setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
        }
        set(p[0], 2, p[1], state);
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
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
                water.put(player.getUUID(), Inv.has(player, "gricollers_can", 1) ? 1000 : 40);
                Msg.game(player, "You fill your watering can.");
            }
            case "tithe_patch" -> patch(player, patchAt(pos));
            case "tithe_sack" -> {
                int already = delivered.getOrDefault(player.getUUID(), 0);
                int fruit = 0;
                int pts = 0;
                double bonusXp = 0;
                for (int t = 0; t < 3; t++) {
                    String id = MinigameItems.TITHE_PLANTS[t] + "_fruit";
                    int n = Math.min(Inv.count(player, id), TitheFarm.SACK_SIZE - already - fruit);
                    if (n <= 0) {
                        continue;
                    }
                    Inv.remove(player, id, n);
                    for (int k = 0; k < n; k++) {
                        int index = already + fruit + k + 1;
                        bonusXp += TitheFarm.depositXp(t, index);
                        pts += TitheFarm.depositPoints(index);
                    }
                    fruit += n;
                }
                if (fruit == 0) {
                    Msg.game(player, already >= TitheFarm.SACK_SIZE ? "The sack is full. Leave the farm to start a new run."
                            : "You have no fruit to deposit.");
                    return true;
                }
                int have = MinigameData.of(player).points(player.getUUID(), "tithe_farm");
                pts = Math.max(0, Math.min(pts, TitheFarm.MAX_POINTS - have));
                if (pts > 0) {
                    addPoints(player, MinigameCurrencies.TITHE, pts);
                }
                xp(player, Skill.FARMING, bonusXp);
                delivered.merge(player.getUUID(), fruit, Integer::sum);
                Msg.game(player, "You deposit " + fruit + " fruit and earn " + pts + " points. ("
                        + (already + fruit) + " / " + TitheFarm.SACK_SIZE + " this run)");
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void patch(ServerPlayer player, int i) {
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
            int fruit = 3; // approx: harvest yield
            Inv.give(player, MinigameItems.TITHE_PLANTS[t] + "_fruit", fruit);
            xp(player, Skill.FARMING, TitheFarm.HARVEST_XP[t] * fruit);
            type[i] = -1;
            show(i);
            Msg.action(player, "You harvest some fruit.");
            return;
        }
        if (watered[i]) {
            Msg.action(player, "This plant has already been watered.");
            return;
        }
        int left = water.getOrDefault(player.getUUID(), 0);
        if (left <= 0) {
            Msg.game(player, "Your watering can is empty. Fill it at the water barrel.");
            return;
        }
        water.put(player.getUUID(), left - 1);
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
    protected void onLeave(ServerPlayer player) {
        for (int t = 0; t < 3; t++) {
            Inv.remove(player, MinigameItems.TITHE_PLANTS[t] + "_seed", 1000);
            Inv.remove(player, MinigameItems.TITHE_PLANTS[t] + "_fruit", 1000);
        }
        MinigameData.of(player).setStat(player.getUUID(), "kc:" + game.id(),
                MinigameData.of(player).stat(player.getUUID(), "kc:" + game.id()) + delivered.getOrDefault(player.getUUID(), 0));
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        return List.of("Tithe Farm", "Points: " + MinigameData.of(player).points(player.getUUID(), "tithe_farm"),
                "Fruit deposited: " + delivered.getOrDefault(player.getUUID(), 0),
                "Watering can: " + water.getOrDefault(player.getUUID(), 0) + " uses");
    }
}
