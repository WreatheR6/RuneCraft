package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.TzHaarWaves;
import com.runecraft.osrs.data.Boss;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Msg;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * The TzHaar Fight Cave: 63 waves (see {@link TzHaarWaves}) ending with the built-in TzTok-Jad,
 * who drops the fire cape and the 8,032 tokkul completion reward ({@link TzHaarWaves#FIGHT_CAVE_TOKKUL}). Tz-Kek split into two smaller Tz-Kek when killed and four
 * Yt-HurKot heal Jad once he drops below half health. Leaving early pays tokkul for the waves
 * completed.
 */
public class FightCaveSession extends TzHaarSession {
    private boolean jadDead;

    public FightCaveSession(int id, int slot, ServerLevel world, String variant) {
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
    protected Mob spawnWaveCreature(String id, BlockPos at) {
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
    protected void onKill(ServerPlayer player, LivingEntity entity, String creatureId) {
        if (creatureId.equals(TzHaarWaves.TZ_KEK)) {
            for (int i = 0; i < 2; i++) {
                Mob small = spawn("tz_kek_small", entity.blockPosition().offset(i * 2 - 1, 0, 0));
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
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        if (blockId.equals("minigame_exit")) {
            int tokkul = TzHaarWaves.fightCaveTokkul(Math.max(0, wave - 1), false);
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
        // TzTok-Jad's kill (fire cape, the 8,032 tokkul reward, pet roll) and its kill count message come from BossEntity;
        // this stat only records completions for the Inferno's fire cape check, so the kill isn't announced twice.
        for (ServerPlayer p : online()) {
            MinigameData.of(p).incrementStat(p.getUUID(), "kc:" + game.id());
        }
    }
}
