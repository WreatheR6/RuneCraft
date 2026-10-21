package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameCurrencies;
import com.runecraft.osrs.content.minigames.MinigameItems;
import com.runecraft.osrs.content.minigames.MinigameRules.TitheFarm;
import com.runecraft.osrs.content.minigames.SkillingOutfits;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Tithe Farm in Hosidius (OSRS Wiki): take the seeds for your Farming level from the table (golovanova 34, bologano 54,
 * logavano 74), plant them with a seed dibber (no XP), water each plant once in each of its three one-minute stages (it
 * dies if left dry) with watering cans (8 doses each) or Gricoller's can (1,000), and harvest one fruit from each grown
 * plant (6/14/23 Farming XP). Gricoller's fertiliser, handed out on entry, halves a plant's growth time. Fruit go into the
 * sack in batches of 100: 10x the harvest XP each, doubled for the 75th to 100th fruit plus 250x on the 75th, a point for
 * every third fruit and 2 more for the 100th (at most 16,000 points held). The farmer's outfit adds up to 2.5% to the
 * harvest and deposit XP (not the bonus). Leaving takes the seeds and fruit away.
 */
public class TitheFarmSession extends MinigameSession {
    private static final int PATCHES = 20;
    /** Per patch: seed type (-1 empty), stage (0..2 growing, 3 grown, -2 dead), watered this stage, fertilised. */
    private final int[] type = new int[PATCHES];
    private final int[] stage = new int[PATCHES];
    private final boolean[] watered = new boolean[PATCHES];
    private final boolean[] fertilised = new boolean[PATCHES];
    private final long[] stageAt = new long[PATCHES];
    private final Map<UUID, Integer> water = new HashMap<>();
    private final Map<UUID, Integer> batch = new HashMap<>();
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
        return new int[]{-10 + (i % 5) * 5, -10 + (i / 5) * 5};
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

    @Override
    protected void start() {
        for (ServerPlayer p : online()) {
            water.put(p.getUUID(), fullDoses(p));
            if (Inv.hasRoomFor(p, "gricollers_fertiliser") && !Inv.has(p, "gricollers_fertiliser", 1)) {
                Inv.give(p, "gricollers_fertiliser", 1);
            }
            Msg.game(p, "Farmer Gricoller: Take seeds from the table, then plant, water and harvest. Fruit goes in the sack.");
        }
    }

    /** Doses the player's cans hold when full: 8 per watering can, 1,000 in Gricoller's can. */
    private static int fullDoses(ServerPlayer p) {
        return TitheFarm.CAN_DOSES * Inv.count(p, "watering_can") + (Inv.has(p, "gricollers_can", 1) ? TitheFarm.GRICOLLER_DOSES : 0);
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
        Block b = type[i] < 0 ? Blocks.AIR : stage[i] == -2 ? Blocks.BROWN_CARPET : stage[i] >= TitheFarm.WATERINGS
                ? Blocks.FLOWERING_AZALEA_LEAVES : stage[i] >= 1 ? Blocks.AZALEA_LEAVES : Blocks.MOSS_CARPET;
        net.minecraft.world.level.block.state.BlockState state = b.defaultBlockState();
        if (b instanceof net.minecraft.world.level.block.LeavesBlock) {
            state = state.setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
        }
        set(p[0], 2, p[1], state);
    }

    private static List<String> worn(ServerPlayer p) {
        List<String> ids = new ArrayList<>();
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ids.add(Inv.idOf(p.getItemBySlot(slot)));
        }
        return ids;
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "tithe_seed_table" -> takeSeeds(player);
            case "tithe_water_barrel" -> {
                int doses = fullDoses(player);
                if (doses == 0) {
                    Msg.game(player, "You need a watering can or Gricoller's can to fill.");
                } else {
                    water.put(player.getUUID(), doses);
                    Msg.game(player, "You fill your watering cans (" + doses + " doses).");
                }
            }
            case "tithe_patch" -> patch(player, patchAt(pos));
            case "tithe_sack" -> deposit(player);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void takeSeeds(ServerPlayer player) {
        int s = TitheFarm.seedFor(Players.data(player).skills().currentLevel(Skill.FARMING));
        if (s < 0) {
            Msg.game(player, "You need a Farming level of " + MinigameItems.TITHE_LEVELS[0] + " to plant Tithe Farm seeds.");
            return;
        }
        for (int t = 0; t < MinigameItems.TITHE_PLANTS.length; t++) {
            if (t != s && Inv.has(player, MinigameItems.TITHE_PLANTS[t] + "_seed", 1)) {
                Msg.game(player, "You can only use one kind of seed at a time.");
                return;
            }
        }
        String seed = MinigameItems.TITHE_PLANTS[s] + "_seed";
        int n = Math.min(TitheFarm.SEEDS_PER_TAKE, TitheFarm.MAX_SEEDS - Inv.count(player, seed));
        if (n <= 0) {
            Msg.game(player, "You already have as many seeds as you can use.");
            return;
        }
        Inv.give(player, seed, n);
        Msg.game(player, "You take " + n + " " + MinigameItems.TITHE_PLANTS[s] + " seeds.");
    }

    private void patch(ServerPlayer player, int i) {
        if (i < 0) {
            return;
        }
        if (type[i] < 0) {
            plant(player, i);
            return;
        }
        if (stage[i] == -2) {
            type[i] = -1;
            show(i);
            Msg.game(player, "You clear the blighted plant.");
            return;
        }
        if (stage[i] >= TitheFarm.WATERINGS) {
            int t = type[i];
            Inv.give(player, MinigameItems.TITHE_PLANTS[t] + "_fruit", TitheFarm.YIELD);
            xp(player, Skill.FARMING, TitheFarm.YIELD * SkillingOutfits.boosted(TitheFarm.HARVEST_XP[t],
                    SkillingOutfits.multiplier(SkillingOutfits.FARMER, worn(player))));
            type[i] = -1;
            show(i);
            Msg.action(player, "You harvest the " + MinigameItems.TITHE_PLANTS[t] + " fruit.");
            return;
        }
        if (Inv.idOf(player.getMainHandItem()).equals("gricollers_fertiliser")) {
            if (fertilised[i]) {
                Msg.action(player, "This plant has already been fertilised.");
            } else {
                fertilised[i] = true;
                Msg.action(player, "You treat the plant with Gricoller's fertiliser.");
            }
            return;
        }
        if (watered[i]) {
            Msg.action(player, "This plant has already been watered.");
            return;
        }
        int left = water.getOrDefault(player.getUUID(), 0);
        if (left <= 0) {
            Msg.game(player, fullDoses(player) == 0 ? "You need a watering can to water the plant."
                    : "Your watering cans are empty. Fill them at the water barrel.");
            return;
        }
        water.put(player.getUUID(), left - 1);
        watered[i] = true;
        Msg.action(player, "You water the plant.");
    }

    private void plant(ServerPlayer player, int i) {
        if (!Inv.has(player, "seed_dibber", 1)) {
            Msg.game(player, "You need a seed dibber to plant seeds.");
            return;
        }
        int level = Players.data(player).skills().currentLevel(Skill.FARMING);
        for (int t = MinigameItems.TITHE_PLANTS.length - 1; t >= 0; t--) {
            if (Inv.has(player, MinigameItems.TITHE_PLANTS[t] + "_seed", 1) && level >= MinigameItems.TITHE_LEVELS[t]) {
                Inv.remove(player, MinigameItems.TITHE_PLANTS[t] + "_seed", 1);
                type[i] = t;
                stage[i] = 0;
                watered[i] = false;
                fertilised[i] = false;
                stageAt[i] = ticks;
                show(i);
                Msg.action(player, "You plant the seed. Water it!");
                return;
            }
        }
        Msg.game(player, "You need a seed from the seed table.");
    }

    private void deposit(ServerPlayer player) {
        UUID id = player.getUUID();
        int inBatch = batch.getOrDefault(id, 0);
        double mult = SkillingOutfits.multiplier(SkillingOutfits.FARMER, worn(player));
        int fruit = 0;
        int pts = 0;
        double xp = 0;
        for (int t = 0; t < MinigameItems.TITHE_PLANTS.length; t++) {
            String fruitId = MinigameItems.TITHE_PLANTS[t] + "_fruit";
            int n = Math.min(Inv.count(player, fruitId), TitheFarm.SACK_SIZE - inBatch - fruit);
            if (n <= 0) {
                continue;
            }
            Inv.remove(player, fruitId, n);
            for (int k = 1; k <= n; k++) {
                int index = inBatch + fruit + k;
                double bonus = TitheFarm.bonusXp(t, index);
                xp += SkillingOutfits.boosted(TitheFarm.depositXp(t, index) - bonus, mult) + bonus;
                pts += TitheFarm.depositPoints(index);
            }
            fruit += n;
        }
        if (fruit == 0) {
            Msg.game(player, "You have no fruit to deposit.");
            return;
        }
        int have = MinigameData.of(player).points(id, "tithe_farm");
        pts = Math.max(0, Math.min(pts, TitheFarm.MAX_POINTS - have));
        if (pts > 0) {
            addPoints(player, MinigameCurrencies.TITHE, pts);
        }
        xp(player, Skill.FARMING, xp);
        delivered.merge(id, fruit, Integer::sum);
        int now = inBatch + fruit;
        Msg.game(player, "You deposit " + fruit + " fruit and earn " + pts + " points (" + now + " / " + TitheFarm.SACK_SIZE + ").");
        if (now >= TitheFarm.SACK_SIZE) {
            now = 0;
            Msg.game(player, "The sack is full: 100 fruit delivered! Gricoller empties it for the next batch.");
        }
        batch.put(id, now);
    }

    @Override
    protected void gameTick() {
        for (int i = 0; i < PATCHES; i++) {
            if (type[i] < 0 || stage[i] < 0 || stage[i] >= TitheFarm.WATERINGS) {
                continue;
            }
            if (ticks - stageAt[i] >= (fertilised[i] ? TitheFarm.FERTILISED_GROW_TICKS : TitheFarm.GROW_TICKS)) {
                stageAt[i] = ticks;
                if (!watered[i]) {
                    stage[i] = -2;
                } else {
                    stage[i]++;
                    watered[i] = false;
                }
                show(i);
            }
        }
    }

    @Override
    protected void onLeave(ServerPlayer player) {
        for (int t = 0; t < MinigameItems.TITHE_PLANTS.length; t++) {
            Inv.remove(player, MinigameItems.TITHE_PLANTS[t] + "_seed", Inv.count(player, MinigameItems.TITHE_PLANTS[t] + "_seed"));
            Inv.remove(player, MinigameItems.TITHE_PLANTS[t] + "_fruit", Inv.count(player, MinigameItems.TITHE_PLANTS[t] + "_fruit"));
        }
        Inv.remove(player, "gricollers_fertiliser", Inv.count(player, "gricollers_fertiliser"));
        MinigameData.of(player).setStat(player.getUUID(), "kc:" + game.id(),
                MinigameData.of(player).stat(player.getUUID(), "kc:" + game.id()) + delivered.getOrDefault(player.getUUID(), 0));
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        return List.of("Tithe Farm", "Points: " + MinigameData.of(player).points(player.getUUID(), "tithe_farm"),
                "Sack: " + batch.getOrDefault(player.getUUID(), 0) + " / " + TitheFarm.SACK_SIZE + "  (total "
                        + delivered.getOrDefault(player.getUUID(), 0) + ")",
                "Water: " + water.getOrDefault(player.getUUID(), 0) + " doses");
    }

    // ------------------------------------------------------------------ self-test hooks

    public static int selfTestPatches() {
        return PATCHES;
    }

    public BlockPos selfTestPatch(int i) {
        int[] p = patchPos(i);
        return rel(p[0], 1, p[1]);
    }

    /** Self-test: a patch's seed type (-1 empty), stage (3 grown, -2 dead), and whether it is watered / fertilised. */
    public record Patch(int type, int stage, boolean watered, boolean fertilised) {
    }

    public Patch selfTestState(int i) {
        return new Patch(type[i], stage[i], watered[i], fertilised[i]);
    }

    public int selfTestWater(ServerPlayer p) {
        return water.getOrDefault(p.getUUID(), 0);
    }

    public int selfTestBatch(ServerPlayer p) {
        return batch.getOrDefault(p.getUUID(), 0);
    }

    public BlockPos selfTestBlock(String id) {
        return switch (id) {
            case "tithe_seed_table" -> rel(-12, 1, 10);
            case "tithe_water_barrel" -> rel(-8, 1, 10);
            case "tithe_sack" -> rel(8, 1, 10);
            default -> rel(0, 1, 13);
        };
    }
}
