package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameRules.TroubleBrewing;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Trouble Brewing on Mos Le'Harmless: gather bitternuts, scrapey tree bark and sweetgrubs and
 * deposit them in the brewery's hopper. Whenever the hopper holds one of each, the brewery makes a
 * bottle of Karamjan rum (38 seconds). Sabotaging monkeys occasionally knock a bottle over.
 * After the 20-minute game every player gets 10 pieces of eight per bottle the team made plus one
 * per resource they deposited (at most 100), and the rum contribution XP for each bottle their
 * ingredients went into (OSRS Wiki).
 */
public class TroubleBrewingSession extends MinigameSession {
    private static final String[] INGREDIENTS = {"bitternut", "scrapey_tree_bark", "sweetgrubs"};
    private final Map<UUID, Integer> deposited = new HashMap<>();
    private final Map<UUID, Integer> rumHelped = new HashMap<>();
    /** Players whose ingredients sit in the hopper waiting for the next bottle. */
    private final Set<UUID> contributors = new HashSet<>();
    private final int[] hopper = new int[INGREDIENTS.length];
    private long brewingUntil = -1;
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

    private void gather(ServerPlayerEntity player, String item, Skill skill, double xp) {
        repeat(player, p -> {
            if (!Inv.hasRoomFor(p, item)) {
                return false;
            }
            if (random.nextInt(3) == 0) {
                Inv.give(p, item, 1);
                xp(p, skill, xp);
            }
            return true;
        });
    }

    @Override
    protected boolean onBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "tb_bitternut_tree" -> gather(player, "bitternut", Skill.WOODCUTTING, 5); // approx (monkeys fetch the nuts in OSRS)
            case "tb_scrapey_tree" -> gather(player, "scrapey_tree_bark", Skill.WOODCUTTING, 50);
            case "tb_sweetgrub_mound" -> gather(player, "sweetgrubs", Skill.HUNTER, 40);
            case "tb_brewing_vat" -> {
                int total = 0;
                for (int i = 0; i < INGREDIENTS.length; i++) {
                    int n = Inv.remove(player, INGREDIENTS[i], 1000);
                    if (n > 0) {
                        hopper[i] += n;
                        total += n;
                        xp(player, Skill.COOKING, n * (i == 0 ? TroubleBrewing.BITTERNUT_XP : TroubleBrewing.DEPOSIT_XP));
                    }
                }
                if (total == 0) {
                    Msg.game(player, "Bring bitternuts, scrapey tree bark or sweetgrubs to put in the hopper.");
                    return true;
                }
                deposited.merge(player.getUuid(), total, Integer::sum);
                contributors.add(player.getUuid());
                Msg.game(player, "You put " + total + " ingredient" + (total > 1 ? "s" : "") + " in the hopper.");
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void gameTick() {
        if (brewingUntil < 0 && hopper[0] > 0 && hopper[1] > 0 && hopper[2] > 0) {
            for (int i = 0; i < hopper.length; i++) {
                hopper[i]--;
            }
            brewingUntil = ticks + TroubleBrewing.BREW_TICKS;
        }
        if (brewingUntil >= 0 && ticks >= brewingUntil) {
            brewingUntil = -1;
            teamBottles++;
            for (UUID u : contributors) {
                rumHelped.merge(u, 1, Integer::sum);
            }
            if (hopper[0] == 0 || hopper[1] == 0 || hopper[2] == 0) {
                contributors.clear();
            }
            broadcast("A bottle of Karamjan rum rolls off the line! Team bottles: " + teamBottles + ".");
        }
        if (ticks % 90 == 0 && teamBottles > 0 && random.nextInt(4) == 0) {
            teamBottles--;
            broadcastRed("A mischievous monkey knocks over a bottle of rum!"); // approx sabotage
        }
        if (ticks >= TroubleBrewing.GAME_TICKS) {
            end(true);
        }
    }

    @Override
    protected void onEnd(boolean success) {
        for (ServerPlayerEntity p : online()) {
            for (String ingredient : INGREDIENTS) {
                Inv.remove(p, ingredient, 1000);
            }
            int resources = deposited.getOrDefault(p.getUuid(), 0);
            if (resources == 0) {
                Msg.game(p, "You didn't contribute anything this game.");
                continue;
            }
            int eight = TroubleBrewing.piecesOfEight(teamBottles, resources);
            reward(p, "pieces_of_eight", eight);
            int helped = Math.min(rumHelped.getOrDefault(p.getUuid(), 0), teamBottles);
            if (helped > 0) {
                xp(p, Skill.CRAFTING, helped * TroubleBrewing.RUM_CRAFTING_XP);
                xp(p, Skill.COOKING, helped * TroubleBrewing.RUM_COOKING_XP);
                xp(p, Skill.FIREMAKING, helped * TroubleBrewing.RUM_FIREMAKING_XP);
            }
            MinigameData.of(p).incrementStat(p.getUuid(), "kc:" + game.id());
            Msg.game(p, "The game is over! Your team brewed " + teamBottles + " bottles and you earn " + eight + " pieces of eight.");
        }
    }

    @Override
    protected List<String> hud(ServerPlayerEntity player) {
        return List.of("Trouble Brewing", "Team bottles: " + teamBottles + (brewingUntil >= 0 ? " (brewing...)" : ""),
                "Hopper: " + hopper[0] + " nuts, " + hopper[1] + " bark, " + hopper[2] + " grubs",
                "Your contribution: " + Math.min(TroubleBrewing.MAX_CONTRIBUTION, deposited.getOrDefault(player.getUuid(), 0)),
                "Time left: " + time(TroubleBrewing.GAME_TICKS - ticks));
    }
}
