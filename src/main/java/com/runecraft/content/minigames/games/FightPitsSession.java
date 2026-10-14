package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.content.minigames.PitChampionService;
import com.runecraft.osrs.content.minigames.FightPits;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.TzHaarWaves;
import com.runecraft.osrs.data.Boss;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Msg;
import net.minecraft.block.Blocks;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * The TzHaar Fight Pit: a safe last-man-standing free-for-all (see {@link FightPits}). Defeated players
 * are sent back out; the last one standing wins tokkul equal to the defeated players' combined combat
 * levels and the champion's red skull ({@link PitChampionService}: shown above their head for an hour, lost when
 * they are defeated or leave a game early, teleport, change armour, pray overhead or take monster damage). Fight Cave
 * monsters join as the match drags on and TzHaar gas ends stalemates.
 */
public class FightPitsSession extends MinigameSession {
    private final List<Integer> defeated = new ArrayList<>();
    private int starters;
    private java.util.UUID champion;

    public FightPitsSession(int id, int slot, ServerWorld world, String variant) {
        super(Minigame.FIGHT_PITS, id, slot, world, variant);
    }

    @Override
    protected int halfX() {
        return 18;
    }

    @Override
    protected int halfZ() {
        return 18;
    }

    @Override
    protected void build() {
        room(Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.MAGMA_BLOCK);
        // Rock pillars to hide behind, and lava pools in the corners.
        for (int[] p : new int[][]{{-8, -8}, {8, -8}, {-8, 8}, {8, 8}, {0, -12}, {0, 12}, {-12, 0}, {12, 0}}) {
            for (int y = 1; y <= 3; y++) {
                set(p[0], y, p[1], Blocks.BASALT.getDefaultState());
                set(p[0] + 1, y, p[1], Blocks.BASALT.getDefaultState());
            }
        }
        for (int[] c : new int[][]{{-16, -16}, {16, -16}, {-16, 16}, {16, 16}}) {
            set(c[0], 0, c[1], Blocks.LAVA.getDefaultState());
        }
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayerEntity player, int index) {
        double a = index * 2.399963; // spread the players around the pit
        return rel((int) Math.round(Math.cos(a) * 13), 1, (int) Math.round(Math.sin(a) * 13));
    }

    @Override
    protected void start() {
        starters = playerCount();
        if (starters < FightPits.MIN_PLAYERS) {
            broadcast("TzHaar-Mej-Kah: Fight Pit needs at least two JalYt. Wait for another challenger.");
            end(false);
            return;
        }
        broadcastRed("FIGHT! The last JalYt standing is champion of the Fight Pit.");
    }

    @Override
    protected boolean canAttackPlayer(ServerPlayerEntity attacker, ServerPlayerEntity target) {
        return true;
    }

    @Override
    protected void gameTick() {
        if (starters >= FightPits.MIN_PLAYERS && playerCount() <= 1) {
            end(true);
            return;
        }
        List<String> types = FightPits.monsters(ticks);
        if (!types.isEmpty() && ticks % FightPits.SPAWN_INTERVAL == 0 && mobsAlive() < 2 + playerCount()) {
            String id = types.get(random.nextInt(types.size()));
            if (id.equals(TzHaarWaves.JAD)) {
                if (living(null).stream().noneMatch(e -> e.getType() == ModEntities.BOSS_TYPES.get(Boss.TZTOK_JAD))) {
                    broadcastRed("TzTok-Jad joins the fight!");
                    MobEntity jad = spawnType(ModEntities.BOSS_TYPES.get(Boss.TZTOK_JAD), randomFloor(0, 0, 6));
                    if (jad != null) {
                        aggro(jad);
                    }
                }
            } else {
                MobEntity m = spawn(id, randomFloor(0, 0, 14));
                if (m != null) {
                    aggro(m);
                }
            }
        }
        if (ticks == FightPits.GAS_TICK) {
            broadcastRed("TzHaar gas begins to fill the Fight Pit!");
        }
        if (ticks >= FightPits.GAS_TICK) {
            for (ServerPlayerEntity p : online()) {
                hurt(p, FightPits.GAS_DAMAGE);
            }
        }
    }

    @Override
    protected void onLeave(ServerPlayerEntity player) {
        // Defeated, or left before the end: a reigning champion loses the red skull.
        if (starters >= FightPits.MIN_PLAYERS && !player.getUuid().equals(champion)) {
            PitChampionService.clear(player, FightPits.SkullLoss.DEFEATED);
        }
    }

    @Override
    protected void onDeath(ServerPlayerEntity player) {
        defeated.add(Players.data(player).skills().combatLevel());
        Msg.red(player, "You have been defeated! You return to the waiting area.");
        removePlayer(player, true);
    }

    @Override
    protected void onEnd(boolean success) {
        if (!success) {
            return;
        }
        List<ServerPlayerEntity> left = online();
        if (left.size() != 1) {
            return;
        }
        ServerPlayerEntity champion = left.get(0);
        this.champion = champion.getUuid();
        int tokkul = FightPits.tokkul(defeated);
        reward(champion, "tokkul", tokkul);
        MinigameData.of(champion).incrementStat(champion.getUuid(), "kc:" + game.id());
        PitChampionService.crown(champion);
        com.runecraft.content.GameEvents.fireActivity(champion, "fight_pit_champion");
        Msg.game(champion, "You are the champion! You receive " + tokkul + " tokkul.");
        champion.getServer().getPlayerManager().broadcast(Text.literal(champion.getName().getString()
                + " is the new champion of the TzHaar Fight Pit!").formatted(Formatting.RED), false);
    }

    @Override
    protected List<String> hud(ServerPlayerEntity player) {
        return List.of("TzHaar Fight Pit", "Players left: " + playerCount(), "Tokkul at stake: " + FightPits.tokkul(defeated),
                "Time: " + time(ticks));
    }
}
