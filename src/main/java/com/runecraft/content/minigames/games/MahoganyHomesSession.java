package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameCurrencies;
import com.runecraft.osrs.content.minigames.MinigameRules.MahoganyHomes;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mahogany Homes: Amy hands out contracts for your Construction level (Beginner planks, Novice
 * oak, Adept teak, Expert mahogany). Repair or build each furniture hotspot in the client's house
 * with planks (and the occasional steel bar); finishing the house earns carpenter points, coins
 * and bonus XP, then a new contract begins.
 */
public class MahoganyHomesSession extends MinigameSession {
    private static final int[][] HOTSPOTS = {{-6, -6}, {6, -6}, {-6, 4}, {6, 4}};
    private int tier;
    private final int[] planksNeeded = new int[MahoganyHomes.HOTSPOTS];
    private final Set<Integer> done = new HashSet<>();
    private int contracts;

    public MahoganyHomesSession(int id, int slot, ServerWorld world, String variant) {
        super(Minigame.MAHOGANY_HOMES, id, slot, world, variant);
    }

    @Override
    protected int halfX() {
        return 12;
    }

    @Override
    protected int halfZ() {
        return 12;
    }

    private static final net.minecraft.block.Block[] FLOORS = {Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS,
            Blocks.DARK_OAK_PLANKS, Blocks.ACACIA_PLANKS};

    @Override
    protected void build() {
        room(FLOORS[random.nextInt(FLOORS.length)], Blocks.BRICKS, Blocks.WHITE_WOOL);
        for (int[] h : HOTSPOTS) {
            place(h[0], h[1], "mh_hotspot");
        }
        place(0, 11, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayerEntity player, int index) {
        return rel(index % 3 - 1, 1, 9);
    }

    private void newContract() {
        ServerPlayerEntity p = online().isEmpty() ? null : online().get(0);
        tier = p == null ? 0 : MahoganyHomes.tierFor(level(p, Skill.CONSTRUCTION));
        done.clear();
        for (int i = 0; i < planksNeeded.length; i++) {
            planksNeeded[i] = 2 + random.nextInt(3);
        }
        broadcast("Amy: Here's a " + MahoganyHomes.TIERS[tier] + " contract. You'll need "
                + java.util.Arrays.stream(planksNeeded).sum() + " " + ItemCatalog.get(MahoganyHomes.PLANKS[tier]).name().toLowerCase() + "s.");
    }

    @Override
    protected void start() {
        newContract();
    }

    @Override
    protected boolean onBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
        if (!blockId.equals("mh_hotspot")) {
            return false;
        }
        BlockPos r = pos.subtract(origin);
        int i = -1;
        for (int k = 0; k < HOTSPOTS.length; k++) {
            if (HOTSPOTS[k][0] == r.getX() && HOTSPOTS[k][1] == r.getZ()) {
                i = k;
            }
        }
        if (i < 0 || done.contains(i)) {
            Msg.game(player, "This furniture is in perfect condition.");
            return true;
        }
        String plank = MahoganyHomes.PLANKS[tier];
        if (!Inv.has(player, plank, planksNeeded[i]) || !Inv.has(player, "hammer", 1)) {
            Msg.game(player, "You need a hammer and " + planksNeeded[i] + " " + ItemCatalog.get(plank).name().toLowerCase() + "s.");
            return true;
        }
        Inv.remove(player, plank, planksNeeded[i]);
        xp(player, Skill.CONSTRUCTION, planksNeeded[i] * MahoganyHomes.XP_PER_PLANK[tier]);
        done.add(i);
        set(r.getX(), 2, r.getZ(), (tier >= 2 ? Blocks.BOOKSHELF : Blocks.CRAFTING_TABLE).getDefaultState());
        Msg.game(player, "You fix the furniture.");
        if (done.size() == HOTSPOTS.length) {
            contracts++;
            for (ServerPlayerEntity p : online()) {
                addPoints(p, MinigameCurrencies.CARPENTER, MahoganyHomes.POINTS[tier]);
                reward(p, "coins", 500 * (tier + 1));
                xp(p, Skill.CONSTRUCTION, 100 * (tier + 1));
                Msg.game(p, "Contract complete! You earn " + MahoganyHomes.POINTS[tier] + " carpenter points.");
            }
            build();
            newContract();
        }
        return true;
    }

    @Override
    protected void gameTick() {
    }

    @Override
    protected void onLeave(ServerPlayerEntity player) {
        MinigameData.of(player).setStat(player.getUuid(), "kc:" + game.id(),
                MinigameData.of(player).stat(player.getUuid(), "kc:" + game.id()) + contracts);
    }

    @Override
    protected List<String> hud(ServerPlayerEntity player) {
        return List.of("Mahogany Homes - " + MahoganyHomes.TIERS[tier], "Hotspots fixed: " + done.size() + " / " + HOTSPOTS.length,
                "Carpenter points: " + MinigameData.of(player).points(player.getUuid(), "mahogany_homes"),
                "Contracts this visit: " + contracts);
    }
}
