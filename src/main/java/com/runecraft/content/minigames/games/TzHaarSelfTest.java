package com.runecraft.content.minigames.games;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHandler;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.entity.BossEntity;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.TzHaarWaves;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Self-test driver for whole TzHaar runs ({@code com.runecraft.selftest}, "minigame:" checks in real-player mode): walks the
 * Fight Cave and the Inferno wave by wave through the real session, spawn and kill paths (kills are the player's, so splits,
 * revivals and rewards run), checking each wave's monsters, the healers, the pillars, the Inferno's special monsters, TzKal-Zuk's
 * phases, the rewards, a death's tokkul, pausing by logging out (between waves or out of combat) and dropping out in combat (a
 * death), and the TzHaar Slayer task taken as a TzTok-Jad task. Every broken step throws an {@link AssertionError}.
 */
public final class TzHaarSelfTest {
    private final ServerPlayer player;
    private final StringBuilder log = new StringBuilder();

    private TzHaarSelfTest(ServerPlayer player) {
        this.player = player;
    }

    private static void expect(boolean ok, String what) {
        if (!ok) {
            throw new AssertionError(what);
        }
    }

    private void heal() {
        if (player.getHealth() < player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        PlayerData data = Players.data(player);
        if (data.prayerPoints() < 50) {
            data.setPrayerPoints(99);
        }
    }

    private void tick(TzHaarSession s, int n) {
        for (int i = 0; i < n && !s.isEnded(); i++) {
            s.selfTestTick();
            heal();
        }
    }

    /** A player hit through the kill path (the death runs the session's onKill) and the after-hit hooks. */
    private int hit(LivingEntity target, int damage, AttackType type) {
        int dealt = CombatHandler.applyDamage(player, target, damage);
        CombatHandler.hookAfterHit(player, target, type, dealt);
        return dealt;
    }

    private void kill(LivingEntity e) {
        if (e.isAlive()) {
            hit(e, (int) Math.ceil(e.getHealth()) + 50, AttackType.RANGED);
        }
    }

    /** Kills everything the session spawned (splits included) until the arena is empty or only {@code keep} is left. */
    private void clear(TzHaarSession s, String keep) {
        for (int round = 0; round < 10; round++) {
            List<LivingEntity> left = new ArrayList<>();
            for (LivingEntity e : s.selfTestCreatures()) {
                String id = MinigameManager.creatureId(e);
                if (!id.equals(keep) && !id.equals("ancestral_glyph")) {
                    left.add(e);
                }
            }
            if (left.isEmpty() || s.isEnded()) {
                return;
            }
            for (LivingEntity e : left) {
                kill(e);
            }
        }
        expect(false, "wave " + s.wave() + ": creatures keep coming back after ten rounds of kills");
    }

    private static Map<String, Integer> count(List<String> ids) {
        Map<String, Integer> out = new TreeMap<>();
        for (String id : ids) {
            out.merge(id, 1, Integer::sum);
        }
        return out;
    }

    private Map<String, Integer> alive(TzHaarSession s) {
        List<String> ids = new ArrayList<>();
        for (LivingEntity e : s.selfTestCreatures()) {
            String id = MinigameManager.creatureId(e);
            if (!id.equals("ancestral_glyph")) {
                ids.add(id);
            }
        }
        return count(ids);
    }

    /** Ticks until wave {@code w} has spawned (at most 20 ticks). */
    private void reach(TzHaarSession s, int w) {
        for (int i = 0; i < 20 && s.wave() < w && !s.isEnded(); i++) {
            s.selfTestTick();
            heal();
        }
        expect(s.wave() == w, "wave " + w + " did not start (the session is on wave " + s.wave() + ")");
    }

    private <T> T start(Minigame game, Class<T> type) {
        MinigameSession s = MinigameManager.selfTestStart(game, game.entranceBlock(), player);
        if (!type.isInstance(s)) {
            s.end(false);
            throw new AssertionError(game.id() + " started a " + s.getClass().getSimpleName());
        }
        return type.cast(s);
    }

    /** An x {@code blocks} from {@code x} towards the arena's centre (so a spot beside a monster stays inside the walls). */
    private static double towardCentre(MinigameSession s, double x, double blocks) {
        return x + (x > s.origin.getX() + 0.5 ? -blocks : blocks);
    }

    /** Back to where the player entered (the south of the arena). */
    private void home(MinigameSession s) {
        player.teleportTo(s.origin.getX() + 0.5, s.origin.getY() + 1, s.origin.getZ() + 16.5);
    }

    private static void end(MinigameSession s) {
        if (!s.isEnded()) {
            s.end(false);
        }
    }

    // ------------------------------------------------------------------ the Fight Cave

    /** A whole Fight Cave run, a death's tokkul and pausing by logging out. */
    public static String fightCaveRun(ServerPlayer player) {
        return new TzHaarSelfTest(player).fightCave();
    }

    private String fightCave() {
        FightCaveSession s = start(Minigame.FIGHT_CAVES, FightCaveSession.class);
        int rotation = s.rotation();
        try {
            for (int w = 1; w < TzHaarWaves.FIGHT_CAVE_WAVES; w++) {
                reach(s, w);
                expect(alive(s).equals(count(TzHaarWaves.fightCave(w))), "Fight Cave wave " + w + " spawned " + alive(s)
                        + " instead of " + count(TzHaarWaves.fightCave(w)));
                if (w <= 3) {
                    // The rotation's spawn point for the wave's strongest monster.
                    BlockPos want = s.spawnAt(TzHaarWaves.fightCaveSpawns(rotation, w).get(0));
                    boolean there = s.selfTestCreatures().stream().anyMatch(e -> e.blockPosition().distSqr(want) <= 9);
                    expect(there, "rotation " + rotation + " wave " + w + ": nothing spawned at " + want);
                }
                clear(s, null);
                expect(s.selfTestCreatures().isEmpty(), "Fight Cave wave " + w + " could not be cleared");
            }
            log.append("rotation ").append(rotation).append(", waves 1-62 spawned and cleared");
            reach(s, TzHaarWaves.FIGHT_CAVE_WAVES);
            expect(s.completedWaves() == 62, "62 waves cleared but " + s.completedWaves() + " counted");
            List<LivingEntity> jads = s.selfTestCreatures(TzHaarWaves.JAD);
            expect(jads.size() == 1 && jads.get(0) instanceof BossEntity, "wave 63 has no TzTok-Jad");
            BossEntity jad = (BossEntity) jads.get(0);
            BlockPos wave3 = s.spawnAt(TzHaarWaves.fightCaveSpawns(rotation, 3).get(0));
            expect(jad.blockPosition().distSqr(wave3) <= 9, "TzTok-Jad did not appear where wave 3's Tz-Kek did");
            // Jad: magic max 95, a stab bite, never a crush.
            expect(jad.maxHitAgainst(player, AttackType.MAGIC) == TzHaarWaves.JAD_MAGIC_MAX, "Jad's magic max hit is "
                    + jad.maxHitAgainst(player, AttackType.MAGIC));
            for (int i = 0; i < 30; i++) {
                expect(jad.chooseStyle(player, 1.0) != AttackType.CRUSH, "Jad chose a crush attack");
            }
            // Healers at half hitpoints, from their four spawn points.
            jad.setHealth(jad.getMaxHealth() / 2);
            tick(s, 1);
            List<LivingEntity> healers = s.selfTestCreatures(TzHaarWaves.HURKOT);
            expect(healers.size() == TzHaarWaves.FIGHT_CAVE_JAD_HEALERS, "Jad at half health brought " + healers.size() + " healers");
            for (TzHaarWaves.CaveSpawn p : TzHaarWaves.HEALER_SPAWNS) {
                BlockPos at = s.spawnAt(p);
                expect(healers.stream().anyMatch(h -> h.blockPosition().distSqr(at) <= 4), "no healer spawned at " + p);
            }
            // An untagged healer next to Jad heals him 5 every 4 ticks.
            LivingEntity healer = healers.get(0);
            healer.teleportTo(jad.getX() + jad.getBbWidth() / 2 + 0.6, jad.getY(), jad.getZ());
            float before = jad.getHealth();
            tick(s, TzHaarWaves.HURKOT_HEAL_TICKS);
            expect(jad.getHealth() > before, "a healer beside Jad did not heal him (" + before + " -> " + jad.getHealth() + ")");
            // Healers that restored him to full come back when he drops to half again.
            kill(healers.get(1));
            kill(healers.get(2));
            jad.setHealth(jad.getMaxHealth());
            tick(s, TzHaarWaves.HURKOT_HEAL_TICKS);
            jad.setHealth(jad.getMaxHealth() / 2);
            tick(s, 1);
            expect(s.selfTestCreatures(TzHaarWaves.HURKOT).size() == TzHaarWaves.FIGHT_CAVE_JAD_HEALERS,
                    "healers that healed Jad to full did not come back (" + s.selfTestCreatures(TzHaarWaves.HURKOT).size() + ")");
            // Tagging a healer turns it on the player.
            LivingEntity tagged = s.selfTestCreatures(TzHaarWaves.HURKOT).get(0);
            hit(tagged, 1, AttackType.RANGED);
            expect(tagged instanceof Mob m && m.getTarget() == player, "a healer the player hit did not turn on them");
            log.append("; Jad at wave 3's point with healers that heal and return");
            int tokkulBefore = Inv.count(player, "tokkul");
            int kc = MinigameData.of(player).stat(player.getUUID(), "kc:" + Minigame.FIGHT_CAVES.id());
            kill(jad);
            expect(s.isEnded(), "killing TzTok-Jad did not end the run");
            expect(Inv.has(player, "fire_cape", 1), "no fire cape after killing TzTok-Jad");
            int tokkul = Inv.count(player, "tokkul") - tokkulBefore;
            expect(tokkul >= TzHaarWaves.FIGHT_CAVE_TOKKUL, "the run paid " + tokkul + " tokkul (expected 8,032)");
            expect(MinigameData.of(player).stat(player.getUUID(), "kc:" + Minigame.FIGHT_CAVES.id()) == kc + 1,
                    "no Fight Cave completion counted");
            log.append("; fire cape and ").append(tokkul).append(" tokkul");
        } finally {
            end(s);
        }
        expect(MinigameManager.session(player) == null, "the player is still in the Fight Cave after it ended");
        deathAndPause();
        return log.toString();
    }

    /**
     * Dying after four waves pays 4 x 5 = 20 tokkul; logging out between waves 6 and 7 picks up at wave 7 with the same
     * rotation; logging out mid-wave 10 seconds out of combat repeats the wave; dropping out in combat is a death. A TzHaar
     * Slayer task is only fought as a TzTok-Jad task when the player takes it, and only that one is cancelled by a death.
     */
    private void deathAndPause() {
        com.runecraft.content.slayer.SlayerState.Profile task = com.runecraft.content.slayer.SlayerService.profile(player);
        String savedTask = task.task;
        int savedAmount = task.amount;
        int savedRemaining = task.remaining;
        String key = "tzhaar_boss_task:" + Minigame.FIGHT_CAVES.id();
        int savedKey = MinigameData.of(player).stat(player.getUUID(), key);
        try {
            task.task = "tzhaar";
            task.amount = 150;
            task.remaining = 150;
            death();
            expect("tzhaar".equals(task.task) && task.amount == 150 && task.remaining == 150,
                    "entering and dying in the Fight Cave changed an ordinary TzHaar task to " + task.task + " " + task.remaining + "/"
                            + task.amount);
            pauseBetweenWaves();
            pauseOutOfCombat();
            droppedInCombat(task);
        } finally {
            task.task = savedTask;
            task.amount = savedAmount;
            task.remaining = savedRemaining;
            MinigameData.of(player).setStat(player.getUUID(), key, savedKey);
        }
        log.append("; a death after 4 waves paid 20 tokkul and kept an ordinary TzHaar task; logging out between waves resumed at"
                + " the next, mid-wave out of combat repeated it, in combat was a death that cancelled a TzTok-Jad task");
    }

    private void death() {
        FightCaveSession s = start(Minigame.FIGHT_CAVES, FightCaveSession.class);
        try {
            expect(!TzHaarSlayer.onTask(player, Minigame.FIGHT_CAVES), "an ordinary TzHaar task counts as a TzTok-Jad task");
            s.selfTestJumpTo(5);
            reach(s, 5);
            int before = Inv.count(player, "tokkul");
            expect(MinigameManager.selfTestDeath(player) == false, "a Fight Cave death was a real death");
            int paid = Inv.count(player, "tokkul") - before;
            expect(paid == TzHaarWaves.fightCaveTokkul(4, false), "dying after 4 waves paid " + paid + " tokkul (expected 20)");
            expect(MinigameManager.session(player) == null, "the player stayed in the cave after dying");
        } finally {
            end(s);
        }
    }

    /** Logs back in and checks the run resumed at {@code wave} with the same rotation. */
    private void resumes(int rotation, int wave) {
        expect(TzHaarSession.pausedWave(player, Minigame.FIGHT_CAVES) == wave, "the paused run saved wave "
                + TzHaarSession.pausedWave(player, Minigame.FIGHT_CAVES) + " instead of " + wave);
        MinigameManager.selfTestLogin(player);
        MinigameSession resumed = MinigameManager.session(player);
        expect(resumed instanceof FightCaveSession, "logging back in did not resume the Fight Cave");
        FightCaveSession r = (FightCaveSession) resumed;
        try {
            expect(r.rotation() == rotation, "the resumed run spawns by rotation " + r.rotation() + ", not " + rotation);
            reach(r, wave);
            expect(alive(r).equals(count(TzHaarWaves.fightCave(wave))), "the resumed run's wave " + wave + " spawned " + alive(r));
            expect(r.completedWaves() == wave - 1, "the resumed run counts " + r.completedWaves() + " waves cleared");
            expect(TzHaarSession.pausedWave(player, Minigame.FIGHT_CAVES) == 0, "the paused run was not cleared");
        } finally {
            end(r);
        }
    }

    private void pauseBetweenWaves() {
        FightCaveSession s = start(Minigame.FIGHT_CAVES, FightCaveSession.class);
        int rotation = s.rotation();
        try {
            s.selfTestJumpTo(6);
            reach(s, 6);
            clear(s, null);
            int before = Inv.count(player, "tokkul");
            MinigameManager.selfTestLogout(player);
            expect(s.isEnded() && MinigameManager.session(player) == null, "logging out did not leave the cave");
            expect(Inv.count(player, "tokkul") == before, "pausing the run paid tokkul");
        } finally {
            end(s);
        }
        resumes(rotation, 7);
    }

    private void pauseOutOfCombat() {
        FightCaveSession s = start(Minigame.FIGHT_CAVES, FightCaveSession.class);
        int rotation = s.rotation();
        try {
            s.selfTestJumpTo(6);
            reach(s, 6);
            // Nothing attacks the player (all lose their target) and no hit lands for 10 seconds.
            for (LivingEntity e : s.selfTestCreatures()) {
                ((Mob) e).setTarget(null);
            }
            tick(s, TzHaarWaves.LOGOUT_COMBAT_TICKS + 1);
            MinigameManager.selfTestLogout(player);
            expect(s.isEnded() && MinigameManager.session(player) == null, "logging out mid-wave did not leave the cave");
        } finally {
            end(s);
        }
        resumes(rotation, 6);
    }

    private void droppedInCombat(com.runecraft.content.slayer.SlayerState.Profile task) {
        FightCaveSession s = start(Minigame.FIGHT_CAVES, FightCaveSession.class);
        try {
            expect(TzHaarSlayer.take(player, Minigame.FIGHT_CAVES), "the TzHaar task could not be taken as a TzTok-Jad task");
            expect(TzHaarSlayer.onTask(player, Minigame.FIGHT_CAVES) && task.amount == 1 && task.remaining == 1,
                    "taking TzTok-Jad did not make the task one Jad (" + task.remaining + "/" + task.amount + ")");
            s.selfTestJumpTo(5);
            reach(s, 5);
            expect(!TzHaarSlayer.take(player, Minigame.FIGHT_CAVES), "a boss task could be taken again mid-run");
            // A Tz-Kek's hit lands, then the player drops out.
            for (LivingEntity e : s.selfTestCreatures()) {
                ((Mob) e).setTarget(null);
            }
            LivingEntity attacker = s.selfTestCreatures().get(0);
            CombatHandler.applyDamage(attacker, player, 1);
            heal();
            int before = Inv.count(player, "tokkul");
            MinigameManager.selfTestLogout(player);
            expect(s.isEnded() && MinigameManager.session(player) == null, "dropping out in combat did not end the run");
            expect(TzHaarSession.pausedWave(player, Minigame.FIGHT_CAVES) == 0, "dropping out in combat paused the run at wave "
                    + TzHaarSession.pausedWave(player, Minigame.FIGHT_CAVES));
            int paid = Inv.count(player, "tokkul") - before;
            expect(paid == TzHaarWaves.fightCaveTokkul(4, false), "dropping out in combat after 4 waves paid " + paid + " tokkul");
            expect(task.task == null, "dropping out in combat kept the TzTok-Jad task");
            MinigameManager.selfTestLogin(player);
            expect(MinigameManager.session(player) == null, "logging back in after dropping out in combat resumed a run");
        } finally {
            end(s);
        }
    }

    // ------------------------------------------------------------------ the Inferno

    /** A whole Inferno run: every wave's monsters, the nibblers and pillars, Jal-Ak, Jal-ImKot, Jal-Zek, the Jads and Zuk. */
    public static String infernoRun(ServerPlayer player) {
        return new TzHaarSelfTest(player).inferno();
    }

    private String inferno() {
        Inv.give(player, "fire_cape", 1);
        InfernoSession s = start(Minigame.INFERNO, InfernoSession.class);
        try {
            expect(!Inv.has(player, "fire_cape", 1) && MinigameData.of(player).flag(player.getUUID(), "inferno_cape_sacrificed"),
                    "entering did not sacrifice the fire cape");
            for (int w = 1; w <= 66; w++) {
                reach(s, w);
                expect(alive(s).equals(count(TzHaarWaves.inferno(w))), "Inferno wave " + w + " spawned " + alive(s) + " instead of "
                        + count(TzHaarWaves.inferno(w)));
                switch (w) {
                    case 1 -> nibblers(s);
                    case 4 -> blob(s);
                    case 9 -> imkot(s);
                    case 36 -> revive(s);
                    default -> {
                    }
                }
                clear(s, null);
            }
            tick(s, 1);
            int[] pillars = s.pillars();
            expect(pillars[0] == 0 && pillars[1] == 0 && pillars[2] == 0, "pillars still standing after wave 66: "
                    + java.util.Arrays.toString(pillars));
            log.append("waves 1-66 spawned and cleared, pillars down after 66");
            jads(s);
            zuk(s);
        } finally {
            end(s);
        }
        expect(MinigameManager.session(player) == null, "the player is still in the Inferno after it ended");
        infernoDroppedInCombat();
        return log.toString();
    }

    /** Dropping out of the Inferno with a monster attacking on wave 10 ends the run as a death: 9 waves' tokkul, no pause. */
    private void infernoDroppedInCombat() {
        InfernoSession s = start(Minigame.INFERNO, InfernoSession.class);
        try {
            s.selfTestJumpTo(10);
            reach(s, 10);
            // A monster that targets the player, beside them and in sight, is attacking them.
            boolean attacked = false;
            for (LivingEntity e : s.selfTestCreatures()) {
                if (attacked || !(e instanceof Mob m) || MinigameManager.creatureId(e).equals(TzHaarWaves.JAL_NIB)) {
                    continue;
                }
                m.setTarget(player);
                player.teleportTo(towardCentre(s, e.getX(), e.getBbWidth() / 2 + 0.8), e.getY(), e.getZ());
                attacked = s.selfTestUnderAttack(player);
            }
            expect(attacked, "no wave 10 monster beside the player and targeting them counts as attacking");
            int before = Inv.count(player, "tokkul");
            MinigameManager.selfTestLogout(player);
            expect(s.isEnded() && MinigameManager.session(player) == null, "dropping out of the Inferno in combat did not end the run");
            expect(TzHaarSession.pausedWave(player, Minigame.INFERNO) == 0, "dropping out of the Inferno in combat paused the run");
            int paid = Inv.count(player, "tokkul") - before;
            int due = TzHaarWaves.infernoTokkul(9);
            expect(paid == due || paid == due * 2, "dropping out of the Inferno after 9 waves paid " + paid + " tokkul (expected " + due + ")");
            MinigameManager.selfTestLogin(player);
            expect(MinigameManager.session(player) == null, "logging back in after dropping out of the Inferno resumed a run");
        } finally {
            end(s);
        }
        log.append("; dropping out in combat on wave 10 was a death");
    }

    /** Wave 1: nibblers go for a pillar and chew it 2-4 every 4 ticks; a pillar knocked down falls. */
    private void nibblers(InfernoSession s) {
        tick(s, 1);
        int before = s.pillars()[0] + s.pillars()[1] + s.pillars()[2];
        for (LivingEntity nib : s.selfTestCreatures(TzHaarWaves.JAL_NIB)) {
            expect(!(nib instanceof Mob m) || m.getTarget() == null, "a nibbler targets the player while pillars stand");
            BlockPos p = s.selfTestNibblerPillar(nib);
            expect(p != null, "a nibbler has no pillar to chew");
            nib.teleportTo(p.getX() + 0.5 + 2.0, nib.getY(), p.getZ() + 0.5);
        }
        tick(s, 2 * TzHaarWaves.NIBBLER_ATTACK_TICKS);
        int after = s.pillars()[0] + s.pillars()[1] + s.pillars()[2];
        int nibs = s.selfTestCreatures(TzHaarWaves.JAL_NIB).size();
        expect(after < before && before - after <= nibs * 2 * TzHaarWaves.NIBBLER_PILLAR_MAX,
                "three nibblers at their pillars for 8 ticks took " + (before - after) + " pillar hitpoints");
        s.selfTestDamagePillar(0, TzHaarWaves.PILLAR_HP);
        expect(s.pillars()[0] == 0 && !s.selfTestPillarStanding(0), "a pillar at 0 hitpoints did not fall");
        log.append("nibblers chewed ").append(before - after).append(" pillar hitpoints; ");
    }

    private void pray(Prayer prayer) {
        PlayerData data = Players.data(player);
        data.activePrayers().remove(Prayer.PROTECT_FROM_MAGIC);
        data.activePrayers().remove(Prayer.PROTECT_FROM_MISSILES);
        data.activePrayers().remove(Prayer.PROTECT_FROM_MELEE);
        if (prayer != null) {
            data.activePrayers().add(prayer);
        }
    }

    /** Wave 4: Jal-Ak reads Protect from Magic and attacks with ranged, Protect from Missiles and attacks with magic. */
    private void blob(InfernoSession s) {
        LivingEntity blob = s.selfTestCreatures(TzHaarWaves.JAL_AK).get(0);
        player.teleportTo(towardCentre(s, blob.getX(), 5), blob.getY(), blob.getZ());
        ((Mob) blob).setTarget(player);
        try {
            pray(Prayer.PROTECT_FROM_MAGIC);
            String read = null;
            for (int i = 0; i < 12 && read == null; i++) {
                tick(s, 1);
                read = s.selfTestBlobRead(blob);
            }
            expect("ranged".equals(read), "Jal-Ak read Protect from Magic as " + read);
            tick(s, TzHaarWaves.BLOB_READ_TICKS);
            pray(Prayer.PROTECT_FROM_MISSILES);
            read = null;
            for (int i = 0; i < 12 && read == null; i++) {
                tick(s, 1);
                read = s.selfTestBlobRead(blob);
            }
            expect("magic".equals(read), "Jal-Ak read Protect from Missiles as " + read);
        } finally {
            pray(null);
            home(s);
        }
        // It splits into the three Jal-AkRek.
        kill(blob);
        expect(s.selfTestCreatures("jal_akrek_ket").size() == 1 && s.selfTestCreatures("jal_akrek_xil").size() == 1 && s.selfTestCreatures("jal_akrek_mej").size() == 1,
                "Jal-Ak did not split into the three Jal-AkRek");
        log.append("Jal-Ak reads prayers and splits; ");
    }

    /** Wave 9: a Jal-ImKot that can't reach the player digs up beside them 50 ticks into the wave. */
    private void imkot(InfernoSession s) {
        LivingEntity imkot = s.selfTestCreatures(TzHaarWaves.JAL_IMKOT).get(0);
        ((Mob) imkot).setTarget(player);
        player.teleportTo(towardCentre(s, imkot.getX(), 12), imkot.getY(), imkot.getZ());
        tick(s, TzHaarWaves.IMKOT_FIRST_DIG + 2);
        double away = imkot.distanceTo(player);
        home(s);
        expect(away < 3.5, "Jal-ImKot did not dig up beside the player (" + away + " blocks)");
        expect(s.caFlags(player).contains(com.runecraft.osrs.content.systems.CombatAchievements.FLAG_IMKOT_DUG),
                "the dig was not flagged for the Combat Achievements");
        log.append("Jal-ImKot digs; ");
    }

    /** Wave 36: a Jal-Zek revives the slain Jal-MejRah once, at half hitpoints. */
    private void revive(InfernoSession s) {
        LivingEntity bat = s.selfTestCreatures(TzHaarWaves.JAL_MEJRAH).get(0);
        kill(bat);
        expect(s.fallenThisWave() == 1, "the slain bat is not revivable (" + s.fallenThisWave() + ")");
        Mob back = s.revive(s.selfTestCreatures(TzHaarWaves.JAL_ZEK).get(0));
        expect(back != null && MinigameManager.creatureId(back).equals(TzHaarWaves.JAL_MEJRAH), "Jal-Zek did not revive the bat");
        expect(Math.abs(back.getHealth() - back.getMaxHealth() / 2) < 1, "the revived bat has " + back.getHealth() + " hitpoints");
        kill(back);
        expect(s.fallenThisWave() == 0, "a revived monster can be revived again");
        log.append("Jal-Zek revives once; ");
    }

    /** Waves 67 and 68: the JalTok-Jads and their level-141 healers. */
    private void jads(InfernoSession s) {
        reach(s, 67);
        List<LivingEntity> jads = s.selfTestCreatures(TzHaarWaves.JALTOK_JAD);
        expect(jads.size() == 1, "wave 67 has " + jads.size() + " JalTok-Jads");
        LivingEntity jad = jads.get(0);
        jad.setHealth(jad.getMaxHealth() / 2);
        tick(s, 1);
        List<LivingEntity> healers = s.selfTestCreatures(TzHaarWaves.HURKOT_INFERNO);
        expect(healers.size() == 5, "wave 67's Jad brought " + healers.size() + " healers (expected five level-141 Yt-HurKot)");
        LivingEntity h = healers.get(0);
        h.teleportTo(jad.getX() + jad.getBbWidth() / 2 + 0.6, jad.getY(), jad.getZ());
        float before = jad.getHealth();
        tick(s, TzHaarWaves.HURKOT_HEAL_TICKS);
        expect(jad.getHealth() > before, "an untagged healer beside JalTok-Jad did not heal it");
        hit(h, 1, AttackType.RANGED);
        clear(s, null);
        reach(s, 68);
        jads = s.selfTestCreatures(TzHaarWaves.JALTOK_JAD);
        expect(jads.size() == 3, "wave 68 has " + jads.size() + " JalTok-Jads");
        for (LivingEntity j : jads) {
            j.setHealth(j.getMaxHealth() / 2);
        }
        tick(s, 1);
        expect(s.selfTestCreatures(TzHaarWaves.HURKOT_INFERNO).size() == 9, "wave 68's Jads brought " + s.selfTestCreatures(TzHaarWaves.HURKOT_INFERNO).size()
                + " healers (expected three each)");
        clear(s, null);
        log.append("; Jads with five and three healers");
    }

    /** Wave 69: TzKal-Zuk, the glyph, the first set, the Jad at 480, the enrage and healers at 240, the kill and rewards. */
    private void zuk(InfernoSession s) {
        reach(s, 69);
        List<LivingEntity> zuks = s.selfTestCreatures(TzHaarWaves.ZUK);
        expect(zuks.size() == 1 && zuks.get(0) instanceof DataBossEntity, "wave 69 has no TzKal-Zuk");
        DataBossEntity zuk = (DataBossEntity) zuks.get(0);
        expect(s.glyphStanding(), "no Ancestral Glyph on wave 69");
        expect(s.zukHitpoints() == TzHaarWaves.ZUK_HP, "TzKal-Zuk starts on " + s.zukHitpoints() + " hitpoints");
        // The glyph blocks his blast for a player behind it, not for one beside it.
        LivingEntity glyph = s.selfTestGlyph();
        player.teleportTo(glyph.getX(), glyph.getY(), glyph.getZ() + 3);
        expect(zuk.script().onHitPlayer(zuk, player, 100, AttackType.MAGIC) == 0, "the glyph did not block Zuk's blast");
        player.teleportTo(glyph.getX() + 6, glyph.getY(), glyph.getZ() + 3);
        expect(zuk.script().onHitPlayer(zuk, player, 100, AttackType.MAGIC) == 100, "Zuk's blast was blocked without the glyph");
        // The first set after the glyph's full sweep, attacking the glyph.
        for (int i = 0; i < 300 && s.sets() == 0; i++) {
            tick(s, 1);
        }
        expect(s.sets() == 1, "no Jal-Xil + Jal-Zek set after the glyph's first sweep");
        expect(s.selfTestCreatures(TzHaarWaves.JAL_XIL).size() == 1 && s.selfTestCreatures(TzHaarWaves.JAL_ZEK).size() == 1, "the set is not a Jal-Xil and a Jal-Zek");
        tick(s, 20);
        expect(s.glyphHp() < TzHaarWaves.GLYPH_HP, "the set did not attack the glyph (" + s.glyphHp() + ")");
        LivingEntity xil = s.selfTestCreatures(TzHaarWaves.JAL_XIL).get(0);
        hit(xil, 1, AttackType.RANGED);
        expect(xil instanceof Mob m && m.getTarget() == player && !s.selfTestShieldAttacker(xil), "the tagged Jal-Xil did not turn on the player");
        clear(s, TzHaarWaves.ZUK);
        // 480: a JalTok-Jad that attacks the glyph.
        s.selfTestZukHitpoints(470);
        tick(s, 1);
        expect(s.zukJadSpawned() && s.selfTestCreatures(TzHaarWaves.JALTOK_JAD).size() == 1, "no JalTok-Jad at 480 hitpoints");
        expect(s.selfTestShieldAttacker(s.selfTestCreatures(TzHaarWaves.JALTOK_JAD).get(0)), "Zuk's Jad does not attack the glyph");
        clear(s, TzHaarWaves.ZUK);
        // 240: enrage and four Jal-MejJak healing him.
        s.selfTestZukHitpoints(230);
        tick(s, 1);
        zuk.script().gameTick(zuk, 0);
        expect(s.zukHealersSpawned() && s.selfTestCreatures("jal_mejjak").size() == 4, "no four Jal-MejJak at 240 hitpoints");
        expect(((OsrsMonsterEntity) zuk).stats().attackSpeed() == TzHaarWaves.ZUK_ENRAGED_SPEED, "Zuk did not enrage to speed 7");
        int hp = s.zukHitpoints();
        tick(s, TzHaarWaves.MEJJAK_HEAL_TICKS);
        expect(s.zukHitpoints() > hp, "the Jal-MejJak did not heal Zuk (" + hp + " -> " + s.zukHitpoints() + ")");
        log.append("; Zuk: glyph, set, Jad at 480, enrage and healers at 240");
        int tokkulBefore = Inv.count(player, "tokkul");
        kill(zuk);
        expect(s.isEnded(), "killing TzKal-Zuk did not end the run");
        expect(Inv.has(player, "infernal_cape", 1), "no infernal cape after killing TzKal-Zuk");
        int tokkul = Inv.count(player, "tokkul") - tokkulBefore;
        expect(tokkul >= TzHaarWaves.INFERNO_TOKKUL, "the Inferno paid " + tokkul + " tokkul (expected 16,440)");
        expect(MinigameData.of(player).stat(player.getUUID(), "kc:" + Minigame.INFERNO.id()) >= 1, "no Inferno completion counted");
        log.append("; infernal cape and ").append(tokkul).append(" tokkul");
    }
}
