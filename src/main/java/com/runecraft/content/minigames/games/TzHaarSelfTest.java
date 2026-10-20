package com.runecraft.content.minigames.games;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHandler;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.content.slayer.SlayerService;
import com.runecraft.content.slayer.SlayerState;
import com.runecraft.content.systems.CombatAchievementService;
import com.runecraft.content.systems.SysState;
import com.runecraft.entity.BossEntity;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.TzHaarWaves;
import com.runecraft.osrs.content.slayer.SlayerMasters;
import com.runecraft.osrs.content.systems.CombatAchievements;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

/**
 * Self-test driver for whole TzHaar runs ({@code com.runecraft.selftest}, "minigame:" checks in real-player mode): walks the
 * Fight Cave and the Inferno wave by wave through the real session, spawn and kill paths (kills are the player's, so splits,
 * revivals and rewards run), checking each wave's monsters, the healers, the pillars, the Inferno's special monsters, TzKal-Zuk's
 * phases, the rewards, a death's tokkul, pausing by logging out (between waves or out of combat) and dropping out in combat (a
 * death), and the TzHaar Slayer task taken as a TzTok-Jad / TzKal-Zuk task, sized by the Combat Achievement tier
 * ("minigame:tzhaar_task"). Every broken step throws an {@link AssertionError}.
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
        settle(s);
        return type.cast(s);
    }

    /** How long {@link #settle} waits for an arena's chunks (the server needs a few ticks at most). */
    private static final long SETTLE_NANOS = 10_000_000_000L;

    /**
     * Lets the server finish loading a freshly built arena, as it does between ticks: in a live run the first wave comes 8
     * game ticks after the entrance, but a check walks the whole run inside one server tick. A chunk lists its entities to box
     * queries (the session's wave count and creature lookups, Jad's healers, the floor sweep at the end, this test's
     * lookups) only once the server has run its queued full-status promotion, a main-thread task. A chunk the arena's build
     * loads from disk gets it while the build waits for the next chunk; one the server demoted but has not unloaded yet (this
     * slot's arena, built by the check two ticks earlier, whose one-tick load tickets just ran out) is handed back at once,
     * its promotion still queued, so everything the run spawned there was invisible to the session and to this test (round
     * 36: "wave 63 has 0 TzTok-Jads" though Jad had spawned). Runs the server's chunk tasks until every arena chunk is
     * accessible, then the promotions those queued.
     */
    private static void settle(MinigameSession s) {
        ServerChunkCache chunks = s.world.getChunkSource();
        drainChunkTasks(chunks);
        AABB box = s.box();
        int full = ChunkLevel.byStatus(FullChunkStatus.FULL);
        List<ChunkHolder> arena = new ArrayList<>();
        for (int cx = Mth.floor(box.minX) >> 4; cx <= Mth.floor(box.maxX) >> 4; cx++) {
            for (int cz = Mth.floor(box.minZ) >> 4; cz <= Mth.floor(box.maxZ) >> 4; cz++) {
                ChunkHolder holder = chunks.chunkMap.getVisibleChunkIfPresent(ChunkPos.asLong(cx, cz));
                if (holder != null && holder.getTicketLevel() <= full) {
                    arena.add(holder); // a chunk the build loaded (one the box only grazes holds no part of the arena)
                }
            }
        }
        expect(!arena.isEmpty(), s.game.id() + ": none of the arena's chunks is loaded after its build");
        long deadline = System.nanoTime() + SETTLE_NANOS;
        while (arena.stream().anyMatch(h -> !h.getFullChunkFuture().isDone())) {
            expect(System.nanoTime() < deadline, s.game.id() + ": the arena's chunks were still loading after "
                    + SETTLE_NANOS / 1_000_000_000L + " s");
            if (!chunks.pollTask()) {
                LockSupport.parkNanos(100_000L); // the rest arrives from the chunk worker threads
            }
        }
        drainChunkTasks(chunks); // the promotions the completed chunks queued
    }

    /** Runs the server's queued chunk tasks (bounded, in case loading keeps queuing more). */
    private static void drainChunkTasks(ServerChunkCache chunks) {
        for (int i = 0; i < 100_000 && chunks.pollTask(); i++) {
            // each call runs one task
        }
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

    // ------------------------------------------------------------------ a full pack

    /** Fills the main inventory with sharks, leaving {@code free} slots empty; returns what it held, for {@link #restorePack}. */
    private List<ItemStack> fillPack(int free) {
        List<ItemStack> kept = new ArrayList<>();
        for (ItemStack s : player.getInventory().items) {
            kept.add(s.copy());
        }
        ItemStack junk = Inv.stack("shark", 1);
        expect(!junk.isEmpty(), "no shark item to fill the inventory with");
        int size = player.getInventory().items.size();
        for (int i = 0; i < size; i++) {
            player.getInventory().items.set(i, i < size - free ? junk.copy() : ItemStack.EMPTY);
        }
        return kept;
    }

    private void restorePack(List<ItemStack> kept) {
        for (int i = 0; i < kept.size(); i++) {
            player.getInventory().items.set(i, kept.get(i));
        }
    }

    /** Takes {@code item} off the ground at the player's feet (where a reward a full pack can't take is placed); true if it lay there. */
    private boolean takeFromGround(String item) {
        for (ItemEntity e : player.serverLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(3),
                x -> x.isAlive() && item.equals(Inv.idOf(x.getItem())))) {
            e.discard();
            return true;
        }
        return false;
    }

    /** Nothing of a finished run may be left on its arena floor (it would be lost with the arena). */
    private void floorEmpty(MinigameSession s, String run) {
        List<ItemEntity> left = s.world.getEntitiesOfClass(ItemEntity.class, s.box(), ItemEntity::isAlive);
        expect(left.isEmpty(), run + " left " + left.size() + " item stack(s) on the arena floor, e.g. "
                + (left.isEmpty() ? "" : left.get(0).getItem().getCount() + " x " + Inv.idOf(left.get(0).getItem())));
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
            // Jad dies with one free slot (a run ends with the pack full of supplies): the fire cape takes it, and the 8,032
            // tokkul are one stack, in the tokkul pouch, not 126 64-item stacks spilling onto the floor and lost with it.
            int kc = MinigameData.of(player).stat(player.getUUID(), "kc:" + Minigame.FIGHT_CAVES.id());
            List<ItemStack> kept = fillPack(1);
            int tokkulBefore = Inv.count(player, "tokkul");
            boolean cape;
            int tokkul;
            try {
                kill(jad);
                expect(s.isEnded(), "killing TzTok-Jad did not end the run");
                floorEmpty(s, "the Fight Cave");
                // A pet (1/200) swept up before the cape takes the slot; the cape then lies at the player's feet outside.
                cape = Inv.has(player, "fire_cape", 1) || takeFromGround("fire_cape");
                tokkul = Inv.count(player, "tokkul") - tokkulBefore;
            } finally {
                restorePack(kept);
            }
            expect(cape, "no fire cape after killing TzTok-Jad with one free slot");
            expect(tokkul >= TzHaarWaves.FIGHT_CAVE_TOKKUL, "the run paid " + tokkul + " tokkul (expected 8,032)");
            expect(MinigameData.of(player).stat(player.getUUID(), "kc:" + Minigame.FIGHT_CAVES.id()) == kc + 1,
                    "no Fight Cave completion counted");
            log.append("; fire cape and ").append(tokkul).append(" tokkul with one free slot");
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
        log.append("; a death after 4 waves with a full pack paid 20 tokkul and kept an ordinary TzHaar task; logging out between waves resumed at"
                + " the next, mid-wave out of combat repeated it, in combat was a death that cancelled a TzTok-Jad task");
    }

    private void death() {
        FightCaveSession s = start(Minigame.FIGHT_CAVES, FightCaveSession.class);
        try {
            expect(!TzHaarSlayer.onTask(player, Minigame.FIGHT_CAVES), "an ordinary TzHaar task counts as a TzTok-Jad task");
            s.selfTestJumpTo(5);
            reach(s, 5);
            // With a full pack: tokkul needs no free slot (one stack, in the tokkul pouch), so none of it is lost.
            List<ItemStack> kept = fillPack(0);
            int before = Inv.count(player, "tokkul");
            int paid;
            try {
                expect(MinigameManager.selfTestDeath(player) == false, "a Fight Cave death was a real death");
                paid = Inv.count(player, "tokkul") - before;
            } finally {
                restorePack(kept);
            }
            expect(paid == TzHaarWaves.fightCaveTokkul(4, false), "dying after 4 waves with a full pack paid " + paid
                    + " tokkul (expected 20)");
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
        settle(resumed);
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
            // As many Jads as the player's Combat Achievement tier allows (one without; minigame:tzhaar_task checks 2 and 3).
            int size = SlayerService.tzhaarBossTaskKills(player);
            expect(TzHaarSlayer.onTask(player, Minigame.FIGHT_CAVES) && task.amount == size && task.remaining == size,
                    "taking TzTok-Jad did not make the task " + size + " Jad (" + task.remaining + "/" + task.amount + ")");
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
        // Zuk dies with a full pack: the infernal cape is placed on the ground for the player outside (OSRS Wiki "Infernal
        // cape"), never left on the arena floor; the 16,440 tokkul are one stack, in the tokkul pouch.
        List<ItemStack> kept = fillPack(0);
        int tokkulBefore = Inv.count(player, "tokkul");
        boolean cape;
        int tokkul;
        try {
            kill(zuk);
            expect(s.isEnded(), "killing TzKal-Zuk did not end the run");
            floorEmpty(s, "the Inferno");
            cape = Inv.has(player, "infernal_cape", 1) || takeFromGround("infernal_cape");
            tokkul = Inv.count(player, "tokkul") - tokkulBefore;
        } finally {
            restorePack(kept);
        }
        expect(cape, "killing TzKal-Zuk with a full pack gave no infernal cape (none at the player's feet outside)");
        expect(tokkul >= TzHaarWaves.INFERNO_TOKKUL, "the Inferno paid " + tokkul + " tokkul (expected 16,440)");
        expect(MinigameData.of(player).stat(player.getUUID(), "kc:" + Minigame.INFERNO.id()) >= 1, "no Inferno completion counted");
        log.append("; infernal cape (on the ground: full pack) and ").append(tokkul).append(" tokkul");
    }

    // ------------------------------------------------------------------ the TzHaar Slayer task with Combat Achievements

    /**
     * "minigame:tzhaar_task": a TzHaar Slayer task taken as a TzTok-Jad / TzKal-Zuk task, sized by the Combat Achievement tier
     * (OSRS Wiki "TzTok-Jad", Slayer, and the master / grandmaster rewards on "Combat Achievements"): one kill up to the elite
     * tier, two from master, three from grandmaster, the player picking 1 up to that and never more. A master-tier player's
     * two Jads are two Fight Cave runs: the first kill ends its run with one to go and the task still on, only the second
     * completes it (completion, streak, points), each kill giving Jad's 25,250 Slayer experience. Dying in the second run,
     * one Jad already killed, cancels the whole task without costing points or the streak ("Slayer task/TzHaar": "If the
     * player dies or leaves, the task is cancelled and no points are used"). Picking one Jad completes on its kill; a
     * grandmaster-tier TzKal-Zuk task is three, and an Inferno kill leaves two. The player's Combat Achievements, Slayer
     * profile, minigame data and pack are put back afterwards; the Slayer, Ranged and Hitpoints experience the kills give needs
     * no putting back (every check starts from {@code TestContext.reset}'s level 99 in every skill, and the self-test's
     * teardown restores the real player's own PlayerData, skills included).
     */
    public static String tzhaarTaskRun(ServerPlayer player) {
        return new TzHaarSelfTest(player).tzhaarTask();
    }

    private String tzhaarTask() {
        MinecraftServer server = player.getServer();
        UUID id = player.getUUID();
        Object savedSys = SysState.get(server).selfTestSave(id);
        Object savedSlayer = SlayerState.of(server).selfTestSave(id);
        Object savedMinigames = MinigameData.get(server).selfTestSave(id);
        List<ItemStack> pack = packCopy();
        try {
            caTier(null);
            caTier(CombatAchievements.Tier.ELITE);
            caTier(CombatAchievements.Tier.GRANDMASTER);
            caTier(CombatAchievements.Tier.MASTER);
            log.append("Jad/Zuk task size 1 (no tier, elite), 2 (master), 3 (grandmaster)");
            masterJads();
            cancelledAfterOneKill();
            oneJadPicked();
            grandmasterZuk();
        } finally {
            SysState.get(server).selfTestRestore(id, savedSys);
            SlayerState.of(server).selfTestRestore(id, savedSlayer);
            MinigameData.get(server).selfTestRestore(id, savedMinigames);
            restorePack(pack);
        }
        return log.toString();
    }

    private List<ItemStack> packCopy() {
        List<ItemStack> kept = new ArrayList<>();
        for (ItemStack s : player.getInventory().items) {
            kept.add(s.copy());
        }
        return kept;
    }

    /**
     * Gives the player every Combat Achievement up to {@code tier} (none for null) and checks the TzTok-Jad / TzKal-Zuk task
     * size it allows against the wiki: 1, 2 from the master tier, 3 from the grandmaster tier.
     */
    private void caTier(CombatAchievements.Tier tier) {
        Set<String> done = SysState.of(player).caDone;
        done.clear();
        if (tier != null) {
            for (CombatAchievements.Task t : CombatAchievements.tasks()) {
                if (t.tier().ordinal() <= tier.ordinal()) {
                    done.add(t.id());
                }
            }
        }
        expect(CombatAchievementService.tier(player) == tier, "every Combat Achievement up to " + tier + " unlocks the "
                + CombatAchievementService.tier(player) + " tier");
        int want = tier == CombatAchievements.Tier.GRANDMASTER ? 3 : tier == CombatAchievements.Tier.MASTER ? 2 : 1;
        int got = SlayerService.tzhaarBossTaskKills(player);
        expect(got == want, "a player with the " + tier + " Combat Achievement tier may take " + got + " TzTok-Jad per task"
                + " (the wiki: " + want + ")");
    }

    /** A fresh TzHaar task from Duradel (130-199 TzHaar), whose completion is the fifth in a row (Duradel's 15 points). */
    private SlayerState.Profile tzhaarAssignment() {
        SlayerState.Profile p = SlayerService.profile(player);
        p.task = "tzhaar";
        p.amount = 150;
        p.remaining = 150;
        p.master = "duradel";
        p.region = null;
        p.chooseAmount = false;
        p.streak = 4;
        return p;
    }

    private int bossTaskFlag(Minigame game) {
        return MinigameData.of(player).stat(player.getUUID(), "tzhaar_boss_task:" + game.id());
    }

    private int slayerXp() {
        return Players.data(player).skills().xp(Skill.SLAYER);
    }

    /**
     * One Fight Cave run on the TzTok-Jad task, straight to wave 63 and Jad's kill; returns the Slayer XP it gave. {@code run}
     * names the run in a failure (the check makes four).
     */
    private int jadRun(String run) {
        int xp = slayerXp();
        FightCaveSession s = start(Minigame.FIGHT_CAVES, FightCaveSession.class);
        try {
            expect(TzHaarSlayer.onTask(player, Minigame.FIGHT_CAVES), run + ": the Fight Cave run does not count as the TzTok-Jad task");
            s.selfTestJumpTo(TzHaarWaves.FIGHT_CAVE_WAVES);
            reach(s, TzHaarWaves.FIGHT_CAVE_WAVES);
            List<LivingEntity> jads = s.selfTestCreatures(TzHaarWaves.JAD);
            expect(jads.size() == 1, run + ": wave 63 (rotation " + s.rotation() + ") has " + jads.size() + " TzTok-Jads");
            kill(jads.get(0));
            expect(s.isEnded(), run + ": killing TzTok-Jad did not end the run");
        } finally {
            end(s);
        }
        expect(MinigameManager.session(player) == null, run + ": the player is still in the Fight Cave after Jad's kill");
        return slayerXp() - xp;
    }

    /** One Inferno run on the TzKal-Zuk task, straight to wave 69 and Zuk's kill; returns the Slayer XP it gave. */
    private int zukRun() {
        int xp = slayerXp();
        InfernoSession s = start(Minigame.INFERNO, InfernoSession.class);
        try {
            expect(TzHaarSlayer.onTask(player, Minigame.INFERNO), "the Inferno run does not count as the TzKal-Zuk task");
            s.selfTestJumpTo(TzHaarWaves.INFERNO_WAVES);
            reach(s, TzHaarWaves.INFERNO_WAVES);
            List<LivingEntity> zuks = s.selfTestCreatures(TzHaarWaves.ZUK);
            expect(zuks.size() == 1, "wave 69 has " + zuks.size() + " TzKal-Zuks");
            kill(zuks.get(0));
            expect(s.isEnded(), "killing TzKal-Zuk did not end the run");
        } finally {
            end(s);
        }
        expect(MinigameManager.session(player) == null, "the player is still in the Inferno after Zuk's kill");
        return slayerXp() - xp;
    }

    /** Master tier: more than two refused, two by default; the first kill leaves one to go, the second completes the task. */
    private void masterJads() {
        SlayerState.Profile task = tzhaarAssignment();
        expect(!TzHaarSlayer.take(player, Minigame.FIGHT_CAVES, 3), "a master-tier player could take three TzTok-Jad");
        expect(!TzHaarSlayer.onTask(player, Minigame.FIGHT_CAVES) && task.amount == 150, "a refused take changed the TzHaar task to "
                + task.remaining + "/" + task.amount);
        expect(TzHaarSlayer.take(player, Minigame.FIGHT_CAVES), "the TzHaar task could not be taken as a TzTok-Jad task");
        expect(TzHaarSlayer.onTask(player, Minigame.FIGHT_CAVES) && task.amount == 2 && task.remaining == 2,
                "a master-tier player's TzTok-Jad task is " + task.remaining + "/" + task.amount + " (expected two kills)");
        int streak = task.streak;
        int points = task.points;
        int completed = task.completed;
        int xp = jadRun("the first of a master-tier task's two Jads");
        expect(xp >= TzHaarWaves.JAD_SLAYER_XP, "the first Jad gave " + xp + " Slayer XP (expected 25,250)");
        expect("tzhaar".equals(task.task) && task.amount == 2 && task.remaining == 1 && TzHaarSlayer.onTask(player, Minigame.FIGHT_CAVES),
                "after the first of two Jads the task is " + task.task + " " + task.remaining + "/" + task.amount
                        + (TzHaarSlayer.onTask(player, Minigame.FIGHT_CAVES) ? "" : ", no longer a TzTok-Jad task") + " (expected one to go)");
        expect(task.completed == completed && task.streak == streak && task.points == points, "the first of two Jads completed the"
                + " task (completed " + completed + " -> " + task.completed + ", streak " + streak + " -> " + task.streak + ")");
        expect(!TzHaarSlayer.take(player, Minigame.FIGHT_CAVES, 2), "the TzTok-Jad task could be taken again between its kills");
        expect(bossTaskFlag(Minigame.FIGHT_CAVES) == 2, "the TzTok-Jad flag holds " + bossTaskFlag(Minigame.FIGHT_CAVES)
                + " between the kills (expected the task's size, 2)");
        xp = jadRun("the second of a master-tier task's two Jads");
        expect(xp >= TzHaarWaves.JAD_SLAYER_XP, "the second Jad gave " + xp + " Slayer XP (expected 25,250)");
        expect(task.remaining == 0 && !task.hasTask() && !TzHaarSlayer.onTask(player, Minigame.FIGHT_CAVES),
                "the second of two Jads left the task at " + task.remaining + "/" + task.amount);
        int due = SlayerMasters.get("duradel").pointsFor(streak + 1);
        expect(task.completed == completed + 1 && task.streak == streak + 1 && task.points - points >= due,
                "the second Jad did not complete the task: completed " + completed + " -> " + task.completed + ", streak " + streak
                        + " -> " + task.streak + ", points +" + (task.points - points) + " (expected +" + due + ")");
        expect(bossTaskFlag(Minigame.FIGHT_CAVES) == 0, "the completed task left its TzTok-Jad flag set");
        log.append("; a master-tier TzTok-Jad task is two runs, one to go after the first kill, completed after the second (+")
                .append(task.points - points).append(" points), 25,250 Slayer XP each");
    }

    /** Dying in the second run, one Jad already killed, cancels the whole task: no points spent, the streak kept. */
    private void cancelledAfterOneKill() {
        SlayerState.Profile task = tzhaarAssignment();
        expect(TzHaarSlayer.take(player, Minigame.FIGHT_CAVES) && task.remaining == 2, "the TzTok-Jad task was not taken as two kills");
        jadRun("the first Jad of a task cancelled by a death");
        expect(task.remaining == 1 && TzHaarSlayer.onTask(player, Minigame.FIGHT_CAVES), "one of two Jads left the task at "
                + task.remaining + "/" + task.amount);
        int points = task.points;
        int streak = task.streak;
        FightCaveSession s = start(Minigame.FIGHT_CAVES, FightCaveSession.class);
        try {
            s.selfTestJumpTo(5);
            reach(s, 5);
            expect(!MinigameManager.selfTestDeath(player), "a Fight Cave death was a real death");
        } finally {
            end(s);
        }
        expect(task.task == null && !task.hasTask() && bossTaskFlag(Minigame.FIGHT_CAVES) == 0,
                "dying in the second run (one Jad killed) kept the task: " + task.task + " " + task.remaining + "/" + task.amount);
        expect(task.points == points && task.streak == streak, "cancelling the TzTok-Jad task cost points (" + points + " -> "
                + task.points + ") or the streak (" + streak + " -> " + task.streak + ")");
        log.append("; dying in the second run after one Jad cancelled the whole task (no points or streak lost)");
    }

    /** The player may pick fewer than allowed (a pick that is not a number is refused): one Jad, completed on its one kill. */
    private void oneJadPicked() {
        SlayerState.Profile task = tzhaarAssignment();
        expect(!TzHaarSlayer.choose(player, "task_jad_x") && !TzHaarSlayer.onTask(player, Minigame.FIGHT_CAVES) && task.amount == 150,
                "a TzTok-Jad pick that is not a number changed the TzHaar task to " + task.remaining + "/" + task.amount);
        expect(TzHaarSlayer.choose(player, "task_jad_1"), "a master-tier player could not pick one TzTok-Jad");
        expect(TzHaarSlayer.onTask(player, Minigame.FIGHT_CAVES) && task.amount == 1 && task.remaining == 1,
                "picking one TzTok-Jad made the task " + task.remaining + "/" + task.amount);
        int completed = task.completed;
        jadRun("a one-Jad task");
        expect(!task.hasTask() && task.completed == completed + 1 && bossTaskFlag(Minigame.FIGHT_CAVES) == 0,
                "a one-Jad task did not complete on its kill (" + task.remaining + "/" + task.amount + ")");
        log.append("; picking one Jad completed on its kill");
    }

    /** Grandmaster tier: TzKal-Zuk (only once killed) as three kills; one Inferno kill leaves two. */
    private void grandmasterZuk() {
        caTier(CombatAchievements.Tier.GRANDMASTER);
        SlayerState.Profile task = tzhaarAssignment();
        String kc = "kc:" + Minigame.INFERNO.id();
        MinigameData.of(player).setStat(player.getUUID(), kc, 0);
        expect(!TzHaarSlayer.take(player, Minigame.INFERNO), "TzKal-Zuk was offered as a task before the player killed him");
        MinigameData.of(player).setStat(player.getUUID(), kc, 1);
        expect(TzHaarSlayer.take(player, Minigame.INFERNO), "the TzHaar task could not be taken as a TzKal-Zuk task");
        expect(TzHaarSlayer.onTask(player, Minigame.INFERNO) && !TzHaarSlayer.onTask(player, Minigame.FIGHT_CAVES)
                        && task.amount == 3 && task.remaining == 3,
                "a grandmaster-tier player's TzKal-Zuk task is " + task.remaining + "/" + task.amount + " (expected three kills)");
        int completed = task.completed;
        int xp = zukRun();
        expect(xp >= TzHaarWaves.ZUK_SLAYER_XP, "the Zuk kill gave " + xp + " Slayer XP (expected 101,890)");
        expect("tzhaar".equals(task.task) && task.remaining == 2 && task.completed == completed
                        && TzHaarSlayer.onTask(player, Minigame.INFERNO),
                "after one of three Zuks the task is " + task.task + " " + task.remaining + "/" + task.amount + " (expected two to go)");
        log.append("; a grandmaster-tier TzKal-Zuk task is three, two to go after an Inferno kill");
    }
}
