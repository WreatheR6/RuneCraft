package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.TzHaarWaves;
import com.runecraft.osrs.data.Boss;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * The TzHaar Fight Cave: 63 waves (see {@link TzHaarWaves}) spawning by one of the wiki's 15 rotations (the highest-level
 * monster at the next point of the spawn cycle, the rest after it), ending with the built-in TzTok-Jad where wave 3's Tz-Kek
 * appeared. Jad drops the fire cape and the 8,032 tokkul completion reward ({@link TzHaarWaves#FIGHT_CAVE_TOKKUL}, doubled by
 * the elite Karamja Diary); at half hitpoints his four Yt-HurKot come from their fixed spawn points and come back if they
 * heal him to full. Tz-Kek recoil 1 damage at melee attackers and split into two smaller Tz-Kek. Dying or leaving early pays
 * N(N+1) tokkul for the N waves completed. A TzHaar Slayer task the player took as a TzTok-Jad task makes the run count for it
 * (see {@link TzHaarSlayer}); logging out pauses or ends the run by {@link TzHaarSession#onDisconnect}.
 */
public class FightCaveSession extends TzHaarSession {
    private boolean jadDead;
    /** The wiki rotation (1-15) this run spawns by. */
    private int rotation;

    public FightCaveSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.FIGHT_CAVES, id, slot, world, variant);
        this.rotation = 1 + random.nextInt(TzHaarWaves.FIGHT_CAVE_ROTATIONS);
    }

    public final int rotation() {
        return rotation;
    }

    @Override
    protected int totalWaves() {
        return TzHaarWaves.FIGHT_CAVE_WAVES;
    }

    @Override
    protected List<String> waveMonsters(int wave) {
        return TzHaarWaves.fightCave(wave);
    }

    /** The arena position of one of the wiki's five spawn points (north is -z; the entrance is at the south wall). */
    public final BlockPos spawnAt(TzHaarWaves.CaveSpawn point) {
        return switch (point) {
            case NW -> rel(-13, 1, -13);
            case C -> rel(0, 1, -3);
            case SW -> rel(-13, 1, 9);
            case SE -> rel(13, 1, 9);
            case S -> rel(4, 1, 11);
        };
    }

    @Override
    protected BlockPos[] spawnPoints() {
        List<BlockPos> out = new ArrayList<>();
        for (TzHaarWaves.CaveSpawn p : TzHaarWaves.CaveSpawn.values()) {
            out.add(spawnAt(p));
        }
        return out.toArray(new BlockPos[0]);
    }

    @Override
    protected void spawnWave(int wave, List<String> monsters) {
        List<TzHaarWaves.CaveSpawn> at = TzHaarWaves.fightCaveSpawns(rotation, wave);
        for (int i = 0; i < monsters.size(); i++) {
            // Two monsters of one wave at the same point stand a little apart.
            BlockPos p = spawnAt(at.get(i));
            long before = at.subList(0, i).stream().filter(at.get(i)::equals).count();
            Mob mob = spawnWaveCreature(monsters.get(i), p.offset((int) before * 2, 0, 0));
            if (mob != null) {
                aggro(mob);
            }
        }
    }

    @Override
    protected void build() {
        caveRoom();
    }

    @Override
    protected Mob spawnWaveCreature(String id, BlockPos at) {
        if (id.equals(TzHaarWaves.JAD)) {
            broadcastRed("TzTok-Jad emerges from the lava!");
            return spawnType(ModEntities.BOSS_TYPES.get(Boss.TZTOK_JAD), at);
        }
        return super.spawnWaveCreature(id, at);
    }

    /** TzTok-Jad's healers always come from the same four spawn points (OSRS Wiki "Yt-HurKot"). */
    @Override
    protected List<BlockPos> healerSpawns(LivingEntity jad, int healers) {
        List<BlockPos> out = new ArrayList<>();
        for (TzHaarWaves.CaveSpawn p : TzHaarWaves.HEALER_SPAWNS) {
            out.add(spawnAt(p));
        }
        return out;
    }

    @Override
    protected void gameTick() {
        super.gameTick();
        if (wave == TzHaarWaves.FIGHT_CAVE_WAVES) {
            checkJadHealers(TzHaarWaves.JAD, TzHaarWaves.FIGHT_CAVE_JAD_HEALERS);
        }
    }

    @Override
    protected void start() {
        resumePaused();
        for (ServerPlayer p : online()) {
            TzHaarSlayer.enter(p, Minigame.FIGHT_CAVES, wave < 1);
        }
    }

    @Override
    protected void onPlayerHit(ServerPlayer player, LivingEntity target, AttackType type, int dealt) {
        super.onPlayerHit(player, target, type, dealt);
        // The level-45 Tz-Kek reflects 1 damage at whoever hits it with melee (OSRS Wiki "Tz-Kek").
        if (type != null && type.isMelee() && MinigameManager.creatureId(target).equals(TzHaarWaves.TZ_KEK) && dealt >= 0) {
            hurt(player, 1);
        }
    }

    @Override
    protected void onKill(ServerPlayer player, LivingEntity entity, String creatureId) {
        TzHaarSlayer.kill(player, Minigame.FIGHT_CAVES, creatureId, entity);
        if (creatureId.equals(TzHaarWaves.TZ_KEK)) {
            for (int i = 0; i < 2; i++) {
                Mob small = spawn("tz_kek_small", entity.blockPosition().offset(i * 2 - 1, 0, 0));
                if (small != null) {
                    small.setTarget(player);
                }
            }
        } else if (creatureId.equals(TzHaarWaves.JAD)) {
            jadDead = true;
            for (LivingEntity healer : living(TzHaarWaves.HURKOT)) {
                healer.discard();
            }
            broadcast("TzHaar-Mej-Jal: You even defeated TzTok-Jad, I am most impressed! Please accept this gift as a reward.");
            end(true);
        }
    }

    /** Tokkul for the waves completed when a run ends early (death or the exit), doubled by the elite Karamja Diary. */
    private void payForfeit(ServerPlayer player) {
        int tokkul = TzHaarWaves.diaryTokkul(TzHaarWaves.fightCaveTokkul(completedWaves, false), eliteKaramja(player));
        if (tokkul > 0) {
            reward(player, "tokkul", tokkul);
            Msg.game(player, "TzHaar-Mej-Jal: Well done in the cave, here, take TokKul as reward. (" + tokkul + ")");
        }
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        if (blockId.equals("minigame_exit")) {
            payForfeit(player);
            removePlayer(player, true);
            return true;
        }
        return false;
    }

    @Override
    protected void onDeath(ServerPlayer player) {
        Msg.red(player, "You have been defeated!");
        payForfeit(player);
        removePlayer(player, true);
    }

    @Override
    protected void onLeave(ServerPlayer player) {
        if (!jadDead && !pausing) {
            TzHaarSlayer.leave(player, Minigame.FIGHT_CAVES);
        }
    }

    @Override
    protected boolean pauses() {
        return true;
    }

    /** A paused run keeps its rotation: the same spawns when it picks up again (OSRS Wiki "TzHaar Fight Cave", Changes). */
    @Override
    protected void savePause(MinigameData data, java.util.UUID player) {
        data.setStat(player, pauseKey("rotation", game), rotation);
    }

    @Override
    protected void loadPause(MinigameData data, java.util.UUID player) {
        int r = data.stat(player, pauseKey("rotation", game));
        if (r >= 1 && r <= TzHaarWaves.FIGHT_CAVE_ROTATIONS) {
            rotation = r;
        }
    }

    @Override
    protected void onEnd(boolean success) {
        if (!success || !jadDead) {
            return;
        }
        // TzTok-Jad's kill (fire cape, the 8,032 tokkul reward, pet roll) and its kill count message come from BossEntity;
        // the elite Karamja Diary doubles the tokkul and a TzHaar Slayer task doubles the pet chance.
        for (ServerPlayer p : online()) {
            MinigameData.of(p).incrementStat(p.getUUID(), "kc:" + game.id());
            if (eliteKaramja(p)) {
                reward(p, "tokkul", TzHaarWaves.FIGHT_CAVE_TOKKUL);
                Msg.game(p, "Your Karamja Diary doubles your TokKul reward.");
            }
            if (TzHaarSlayer.onTask(p, Minigame.FIGHT_CAVES)
                    && random.nextInt(TzHaarWaves.JAD_PET_CHANCE) == 0) {
                // 1/200 from the drop table plus 1/200 here: about the wiki's 1/100 on task.
                TzHaarSlayer.pet(p, "tzrek_jad");
            }
            TzHaarSlayer.complete(p, Minigame.FIGHT_CAVES);
        }
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        List<String> lines = new ArrayList<>(super.hud(player));
        if (TzHaarSlayer.onTask(player, Minigame.FIGHT_CAVES)) {
            lines.add("Slayer task: TzTok-Jad");
        }
        return lines;
    }
}
