package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.minigames.KetRakChallenges;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.TzHaarWaves;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

/**
 * TzHaar-Ket-Rak's Challenges (see {@link KetRakChallenges}): challenge N spawns N JalTok-Jads at once (the Inferno's Jad with
 * its telegraphed magic/ranged attacks and stab); each summons Yt-HurKot healers at half hitpoints. Killing every Jad completes
 * the challenge: tokkul, a completion on the scoreboard and the personal best time (from the Jads emerging to the last one's
 * death). The final challenge never ends: every Jad that dies is replaced until the player falls; the most Jads killed is
 * recorded. Dying or leaving fails the challenge (safe: no items are lost).
 */
public class KetRakSession extends TzHaarSession {
    private final int challenge;
    private long jadsAt = -1;
    private int jadsKilled;
    private boolean completed;
    private long finishTicks;
    private boolean emptyInventory = true;
    private boolean fluke;
    private int respawnIn = -1;
    private final Map<Integer, Set<String>> stylesPerJad = new LinkedHashMap<>();

    public KetRakSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.TZHAAR_KET_RAK, id, slot, world, variant);
        this.challenge = KetRakChallenges.challengeOf(variant);
        this.nextWaveIn = KetRakChallenges.START_DELAY;
    }

    @Override
    protected int totalWaves() {
        return 1;
    }

    @Override
    protected List<String> waveMonsters(int wave) {
        // The final challenge starts with one Jad and replaces each one that dies. // approx: starting count undocumented
        int n = challenge == KetRakChallenges.FINAL ? 1 : KetRakChallenges.jads(challenge);
        return Collections.nCopies(n, TzHaarWaves.JALTOK_JAD);
    }

    @Override
    protected BlockPos[] spawnPoints() {
        return new BlockPos[]{rel(0, 1, -12), rel(-10, 1, -10), rel(10, 1, -10), rel(-15, 1, -2), rel(15, 1, -2), rel(-5, 1, -15),
                rel(5, 1, -15)};
    }

    @Override
    protected Mob spawnWaveCreature(String creature, BlockPos at) {
        // One Jad per spawn point, in order, so they never stack on one tile.
        int index = living(TzHaarWaves.JALTOK_JAD).size();
        BlockPos[] points = spawnPoints();
        return super.spawnWaveCreature(creature, points[index % points.length]);
    }

    @Override
    protected void build() {
        caveRoom();
    }

    @Override
    protected void start() {
        for (ServerPlayer p : online()) {
            Msg.game(p, "TzHaar-Ket-Rak: " + KetRakChallenges.name(challenge) + " begins. "
                    + (challenge == KetRakChallenges.FINAL ? "They will keep coming, JalYt." : "Slay " + KetRakChallenges.jads(challenge)
                    + " JalTok-Jad" + (challenge == 1 ? "" : "s") + "."));
        }
    }

    @Override
    protected void gameTick() {
        super.gameTick();
        if (wave >= 1 && jadsAt < 0) {
            jadsAt = ticks;
        }
        checkJadHealers(TzHaarWaves.JALTOK_JAD, KetRakChallenges.HEALERS_PER_JAD);
        for (ServerPlayer p : online()) {
            if (emptyInventory && !inventoryEmpty(p)) {
                emptyInventory = false;
            }
        }
        if (respawnIn > 0 && --respawnIn == 0) {
            BlockPos[] points = spawnPoints();
            Mob jad = spawnWaveCreature(TzHaarWaves.JALTOK_JAD, points[random.nextInt(points.length)]);
            if (jad != null) {
                aggro(jad);
            }
            broadcastRed("Another JalTok-Jad emerges!");
        }
    }

    /** Nothing in the inventory but the wielded weapon (equipped items don't count). */
    private static boolean inventoryEmpty(ServerPlayer p) {
        List<ItemStack> main = p.getInventory().items;
        int held = p.getInventory().selected;
        for (int i = 0; i < main.size(); i++) {
            if (i != held && !main.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected double damageMultiplier(ServerPlayer player, LivingEntity target, AttackType type) {
        if (MinigameManager.creatureId(target).equals(TzHaarWaves.JALTOK_JAD)) {
            stylesPerJad.computeIfAbsent(target.getId(), k -> new HashSet<>()).add(KetRakChallenges.styleClass(type.name()));
        }
        return 1.0;
    }

    @Override
    protected void onKill(ServerPlayer player, LivingEntity entity, String creatureId) {
        if (!creatureId.equals(TzHaarWaves.JALTOK_JAD)) {
            return;
        }
        jadsKilled++;
        if (challenge == KetRakChallenges.FINAL) {
            respawnIn = KetRakChallenges.FINAL_RESPAWN_DELAY;
            Msg.game(player, "JalTok-Jads slain: " + jadsKilled + ".");
            return;
        }
        boolean others = living(TzHaarWaves.JALTOK_JAD).stream().anyMatch(e -> e != entity);
        if (others) {
            return;
        }
        completed = true;
        finishTicks = ticks - Math.max(0, jadsAt);
        for (LivingEntity healer : living(TzHaarWaves.HURKOT_INFERNO)) {
            healer.discard();
        }
        end(true);
    }

    @Override
    protected void onDeath(ServerPlayer player) {
        MinigameData data = MinigameData.of(player);
        data.incrementStat(player.getUUID(), KetRakChallenges.DEATHS_PREFIX + challenge);
        if (challenge == KetRakChallenges.FINAL) {
            int record = data.stat(player.getUUID(), KetRakChallenges.FINAL_RECORD);
            Msg.game(player, "You slew " + jadsKilled + " JalTok-Jad" + (jadsKilled == 1 ? "" : "s") + " in the final challenge.");
            if (jadsKilled > record) {
                data.setStat(player.getUUID(), KetRakChallenges.FINAL_RECORD, jadsKilled);
                Msg.game(player, "That's a new personal record!");
            }
        }
        super.onDeath(player);
    }

    @Override
    protected void onLeave(ServerPlayer player) {
        if (!completed) {
            // Dying or leaving fails the challenge: "It Wasn't a Fluke" needs the fifth and sixth back to back.
            MinigameData.of(player).setStat(player.getUUID(), KetRakChallenges.LAST_RESULT, -challenge);
        }
    }

    @Override
    protected void onEnd(boolean success) {
        if (!success || !completed) {
            return;
        }
        for (ServerPlayer p : online()) {
            MinigameData data = MinigameData.of(p);
            java.util.UUID u = p.getUUID();
            fluke = KetRakChallenges.fluke(data.stat(u, KetRakChallenges.LAST_RESULT), challenge);
            data.setStat(u, KetRakChallenges.LAST_RESULT, challenge);
            int tokkul = KetRakChallenges.tokkul(challenge);
            reward(p, "tokkul", tokkul);
            int kc = data.incrementStat(u, KetRakChallenges.KC_PREFIX + challenge);
            data.incrementStat(u, "kc:" + game.id());
            Msg.game(p, "TzHaar-Ket-Rak: Impressive, JalYt. Take this tokkul. (" + tokkul + ")");
            Msg.game(p, "Challenge duration: " + time(finishTicks) + ". Completions of this challenge: " + kc + ".");
            int best = data.stat(u, KetRakChallenges.BEST_PREFIX + challenge);
            if (KetRakChallenges.newBest(best, finishTicks)) {
                data.setStat(u, KetRakChallenges.BEST_PREFIX + challenge, (int) finishTicks);
                Msg.game(p, best > 0 ? "New personal best! (previous: " + time(best) + ")" : "Personal best: " + time(finishTicks) + ".");
            } else {
                Msg.game(p, "Personal best: " + time(best) + ".");
            }
            if (!data.flag(u, KetRakChallenges.METAMORPHOSIS)
                    && KetRakChallenges.allComplete(c -> data.stat(u, KetRakChallenges.KC_PREFIX + c))) {
                data.setFlag(u, KetRakChallenges.METAMORPHOSIS);
                Msg.game(p, "You have completed all of TzHaar-Ket-Rak's challenges! Your TzRek-Jad can now metamorphose into JalRek-Jad.");
            }
            if (Players.data(p).collectionLog().contains("tzrek_jad") || Inv.has(p, "tzrek_jad", 1)) {
                Msg.game(p, "Your TzRek-Jad looks on proudly.");
            }
        }
    }

    @Override
    public int caStage() {
        return challenge;
    }

    @Override
    public Set<String> caFlags(ServerPlayer player) {
        Set<String> flags = new HashSet<>();
        if (!completed) {
            return flags;
        }
        if (emptyInventory) {
            flags.add(KetRakChallenges.FLAG_EMPTY_INVENTORY);
        }
        if (KetRakChallenges.multiStyle(new ArrayList<>(stylesPerJad.values()), KetRakChallenges.jads(challenge))) {
            flags.add(KetRakChallenges.FLAG_MULTI_STYLE);
        }
        if (fluke) {
            flags.add(KetRakChallenges.FLAG_FLUKE);
        }
        return flags;
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        lines.add(KetRakChallenges.name(challenge));
        if (challenge == KetRakChallenges.FINAL) {
            lines.add("Jads slain: " + jadsKilled);
        } else {
            lines.add("Jads left: " + (jadsAt < 0 ? KetRakChallenges.jads(challenge) : living(TzHaarWaves.JALTOK_JAD).size()));
        }
        lines.add("Time: " + time(jadsAt < 0 ? 0 : ticks - jadsAt));
        return lines;
    }
}
