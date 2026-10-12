package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.TzHaarWaves;
import com.runecraft.osrs.data.Boss;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Msg;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * The TzHaar Fight Cave: 63 waves (see {@link TzHaarWaves}) ending with the built-in TzTok-Jad,
 * who drops the fire cape and tokkul. Tz-Kek split into two smaller Tz-Kek when killed and four
 * Yt-HurKot heal Jad once he drops below half health. Leaving early pays tokkul for the waves
 * completed.
 */
public class FightCaveSession extends TzHaarSession {
    private boolean jadDead;

    public FightCaveSession(int id, int slot, ServerWorld world, String variant) {
        super(Minigame.FIGHT_CAVES, id, slot, world, variant);
    }

    @Override
    protected int totalWaves() {
        return TzHaarWaves.FIGHT_CAVE_WAVES;
    }

    @Override
    protected List<String> waveMonsters(int wave) {
        return TzHaarWaves.fightCave(wave);
    }

    @Override
    protected BlockPos[] spawnPoints() {
        return new BlockPos[]{rel(-15, 1, -15), rel(15, 1, -15), rel(0, 1, -16), rel(-16, 1, 0), rel(16, 1, 0)};
    }

    @Override
    protected void build() {
        caveRoom();
    }

    @Override
    protected MobEntity spawnWaveCreature(String id, BlockPos at) {
        if (id.equals(TzHaarWaves.JAD)) {
            broadcastRed("TzTok-Jad emerges from the lava!");
            return spawnType(ModEntities.BOSS_TYPES.get(Boss.TZTOK_JAD), rel(0, 1, -12));
        }
        return super.spawnWaveCreature(id, at);
    }

    @Override
    protected void gameTick() {
        super.gameTick();
        if (wave == TzHaarWaves.FIGHT_CAVE_WAVES) {
            checkJadHealers(TzHaarWaves.JAD, TzHaarWaves.FIGHT_CAVE_JAD_HEALERS);
        }
    }

    @Override
    protected void onKill(ServerPlayerEntity player, LivingEntity entity, String creatureId) {
        if (creatureId.equals(TzHaarWaves.TZ_KEK)) {
            for (int i = 0; i < 2; i++) {
                MobEntity small = spawn("tz_kek_small", entity.getBlockPos().add(i * 2 - 1, 0, 0));
                if (small != null) {
                    small.setTarget(player);
                }
            }
        } else if (creatureId.equals(TzHaarWaves.JAD)) {
            jadDead = true;
            for (LivingEntity healer : living("yt_hurkot")) {
                healer.discard();
            }
            broadcast("You have defeated TzTok-Jad, I am most impressed! Please accept this gift as a reward.");
            end(true);
        }
    }

    @Override
    protected boolean onBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
        if (blockId.equals("minigame_exit")) {
            int tokkul = TzHaarWaves.fightCaveForfeitTokkul(Math.max(0, wave - 1));
            if (tokkul > 0) {
                reward(player, "tokkul", tokkul);
                Msg.game(player, "TzHaar-Mej-Jal: Well done in the cave, here, take TokKul as reward. (" + tokkul + ")");
            }
            removePlayer(player, true);
            return true;
        }
        return false;
    }

    @Override
    protected void onEnd(boolean success) {
        if (!success || !jadDead) {
            return;
        }
        for (ServerPlayerEntity p : online()) {
            int kc = MinigameData.of(p).incrementStat(p.getUuid(), "kc:" + game.id());
            Msg.game(p, "Your TzTok-Jad kill count is: " + kc + ".");
        }
    }
}
