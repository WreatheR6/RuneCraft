package com.runecraft.content.minigames.games;

import com.runecraft.boss.Mechanics;
import com.runecraft.combat.MonsterStats;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.TzHaarWaves;
import com.runecraft.osrs.content.systems.CombatAchievements;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;

/**
 * Shared logic of the TzHaar minigames (the Fight Cave, the Inferno, TzHaar-Ket-Rak's challenges): waves that spawn once the
 * previous one is dead, Jads that summon Yt-HurKot at half hitpoints (healing by the wiki's rules: {@link TzHaarWaves#hurKotHeals}),
 * Yt-MejKot that heal hurt allies, and creatures whose attacks the session makes itself ("driven": a JalTok-Jad's attack
 * rhythm, a Jal-Ak's prayer read, a Jal-Zek's revivals) while their AI still moves them.
 */
public abstract class TzHaarSession extends MinigameSession {
    protected int wave;
    protected int nextWaveIn = 8;
    /** Waves cleared this run (tokkul on death or leaving). */
    protected int completedWaves;
    /** Combat Achievement conditions judged over the run (healers healing, Jal-ImKot digging, pillars falling). */
    protected final Set<String> caRunFlags = new HashSet<>();
    /** A healer, Jal-MejJak or shield attacker a player has attacked. */
    protected static final String DISTRACTED = "rcmg_distracted";
    /** Healers carry the entity id of the Jad they tend: {@code rcmg_heals_<id>}. */
    private static final String HEALS = "rcmg_heals_";
    /** Per Jad (entity id): whether its healers are out (false again when Fight Cave healers restored him to full). */
    private final Map<Integer, Boolean> healersOut = new HashMap<>();
    /** Driven creatures (entity id): the session tick of their next attack, and of their last one. */
    protected final Map<Integer, Long> nextAttack = new HashMap<>();
    protected final Map<Integer, Long> lastAttack = new HashMap<>();
    /** Attack speed the driven creature attacks at (game ticks). */
    private final Map<Integer, Integer> drivenSpeed = new HashMap<>();
    /** An attack speed so long that a driven creature's own attack goal never fires. */
    private static final int NO_AUTO_ATTACK = 1_000_000;

    protected TzHaarSession(Minigame game, int id, int slot, ServerLevel world, String variant) {
        super(game, id, slot, world, variant);
    }

    protected abstract int totalWaves();

    protected abstract List<String> waveMonsters(int wave);

    protected abstract BlockPos[] spawnPoints();

    /** Spawns one wave creature; subclasses override for bosses. */
    protected Mob spawnWaveCreature(String id, BlockPos at) {
        return spawn(id, at);
    }

    /** Spawns a whole wave: by default each creature at a random spawn point. */
    protected void spawnWave(int wave, List<String> monsters) {
        BlockPos[] points = spawnPoints();
        for (int i = 0; i < monsters.size(); i++) {
            BlockPos at = points[(i + random.nextInt(points.length)) % points.length];
            Mob mob = spawnWaveCreature(monsters.get(i), at);
            if (mob != null && mob.getTarget() == null) {
                aggro(mob);
            }
        }
    }

    /** A wave was cleared (its last creature died). */
    protected void onWaveCleared(int wave) {
    }

    @Override
    protected int halfX() {
        return 20;
    }

    @Override
    protected int halfZ() {
        return 20;
    }

    @Override
    protected int height() {
        return 12;
    }

    protected void caveRoom() {
        room(Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.GILDED_BLACKSTONE);
        for (int i = 0; i < 10; i++) {
            int x = random.nextInt(34) - 17;
            int z = random.nextInt(34) - 17;
            if (Math.abs(x) + Math.abs(z) > 6) {
                set(x, 1, z, Blocks.BASALT.defaultBlockState());
                if (random.nextBoolean()) {
                    set(x, 2, z, Blocks.BASALT.defaultBlockState());
                }
            }
        }
        place(0, 17, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return rel(index % 3 - 1, 1, 15);
    }

    @Override
    protected void gameTick() {
        healers();
        driveTick();
        if (wave > 0 && waveAlive() > 0) {
            return;
        }
        if (wave > completedWaves) {
            completedWaves = wave;
            onWaveCleared(wave);
        }
        if (wave >= totalWaves()) {
            return;
        }
        if (--nextWaveIn > 0) {
            return;
        }
        wave++;
        nextWaveIn = 8;
        broadcastRed("Wave: " + wave);
        spawnWave(wave, waveMonsters(wave));
    }

    /** Creatures that keep the current wave going (everything the session spawned, by default). */
    protected int waveAlive() {
        return mobsAlive();
    }

    @Override
    public Set<String> caFlags(ServerPlayer player) {
        return new HashSet<>(caRunFlags);
    }

    // ------------------------------------------------------------------ healers

    /** Spawns healers for every Jad of {@code jadId} that dropped to half hitpoints and has none out. */
    protected void checkJadHealers(String jadId, int healers) {
        for (LivingEntity jad : living(jadId)) {
            if (!TzHaarWaves.healersDue(jad.getHealth(), jad.getMaxHealth()) || healersOut.getOrDefault(jad.getId(), false)) {
                continue;
            }
            healersOut.put(jad.getId(), true);
            int alive = healersOf(jad).size();
            List<BlockPos> at = healerSpawns(jad, healers);
            for (int i = alive; i < healers; i++) {
                Mob h = spawn(TzHaarWaves.healerOf(jadId), at.get(i % at.size()));
                if (h != null) {
                    h.addTag(HEALS + jad.getId());
                    h.setTarget(null);
                }
            }
            broadcastRed("Yt-HurKot rush to " + jad.getName().getString() + "'s side!");
        }
    }

    /** Where a Jad's healers appear: around the Jad by default. */
    protected List<BlockPos> healerSpawns(LivingEntity jad, int healers) {
        List<BlockPos> out = new ArrayList<>();
        for (int i = 0; i < healers; i++) {
            double a = Math.PI * 2 * i / healers;
            double r = jad.getBbWidth() / 2 + 1.5;
            out.add(BlockPos.containing(jad.getX() + Math.cos(a) * r, jad.getY(), jad.getZ() + Math.sin(a) * r));
        }
        return out;
    }

    /** The healers tending a Jad. */
    protected final List<LivingEntity> healersOf(LivingEntity jad) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : living(null)) {
            if (e.getTags().contains(HEALS + jad.getId())) {
                out.add(e);
            }
        }
        return out;
    }

    /** Tiles between a healer and its Jad's body (1 = adjacent). */
    protected static double tilesFrom(LivingEntity healer, LivingEntity jad) {
        double dx = healer.getX() - jad.getX();
        double dz = healer.getZ() - jad.getZ();
        double edge = Math.sqrt(dx * dx + dz * dz) - (jad.getBbWidth() + healer.getBbWidth()) / 2;
        return 1 + Math.max(0, edge - 0.6);
    }

    /** Yt-HurKot heal their Jad by the wiki's rules; Yt-MejKot heal themselves or an adjacent ally below half hitpoints. */
    protected void healers() {
        if (ticks % TzHaarWaves.HURKOT_HEAL_TICKS != 0) {
            return;
        }
        List<LivingEntity> all = living(null);
        for (LivingEntity e : all) {
            String cid = MinigameManager.creatureId(e);
            if (cid.startsWith(TzHaarWaves.HURKOT) && e instanceof Mob healer) {
                tendJad(healer, all);
            } else if (cid.equals(TzHaarWaves.YT_MEJKOT)) {
                mejKotHeal(e, all);
            }
        }
        // Fight Cave healers that restored their Jad to full come back the next time he drops to half (OSRS Wiki "TzTok-Jad").
        for (LivingEntity jad : all) {
            String cid = MinigameManager.creatureId(jad);
            if (TzHaarWaves.healersRespawn(cid) && healersOut.getOrDefault(jad.getId(), false) && jad.getHealth() >= jad.getMaxHealth()
                    && healersOf(jad).stream().noneMatch(h -> h.getTags().contains(DISTRACTED))) {
                healersOut.put(jad.getId(), false);
            }
        }
    }

    private void tendJad(Mob healer, List<LivingEntity> all) {
        LivingEntity jad = null;
        for (LivingEntity e : all) {
            if (healer.getTags().contains(HEALS + e.getId())) {
                jad = e;
                break;
            }
        }
        if (jad == null) {
            return;
        }
        String jadId = MinigameManager.creatureId(jad);
        boolean tagged = healer.getTags().contains(DISTRACTED);
        double tiles = tilesFrom(healer, jad);
        if (TzHaarWaves.hurKotHeals(jadId, tiles, tagged)) {
            if (jad.getHealth() < jad.getMaxHealth()) {
                jad.heal(TzHaarWaves.hurKotHeal(jadId, random));
                caRunFlags.add(CombatAchievements.FLAG_HURKOT_HEALED);
                world.sendParticles(ParticleTypes.HEART, jad.getX(), jad.getY() + jad.getBbHeight(), jad.getZ(), 2, 0.4, 0.3, 0.4, 0);
            }
            healer.getNavigation().stop();
        } else if (!tagged) {
            healer.setTarget(null);
            healer.getNavigation().moveTo(jad.getX(), jad.getY(), jad.getZ(), 1.0);
        }
    }

    /** Yt-MejKot restore up to 10 hitpoints to themselves or an adjacent monster below half health (OSRS Wiki "Yt-MejKot"). */
    private void mejKotHeal(LivingEntity mejKot, List<LivingEntity> all) {
        for (LivingEntity ally : all) {
            if (ally.distanceToSqr(mejKot) < 9 && ally.getHealth() < ally.getMaxHealth() / 2 && ally.isAlive()) {
                ally.heal(1 + random.nextInt(10));
                caRunFlags.add(CombatAchievements.FLAG_MEJKOT_HEALED);
                world.sendParticles(ParticleTypes.HEART, ally.getX(), ally.getY() + ally.getBbHeight(), ally.getZ(), 2, 0.3, 0.3, 0.3, 0);
                return;
            }
        }
    }

    @Override
    protected void onPlayerHit(ServerPlayer player, LivingEntity target, AttackType type, int dealt) {
        if (target instanceof Mob mob && MinigameManager.creatureId(mob).startsWith(TzHaarWaves.HURKOT)
                && !mob.getTags().contains(DISTRACTED)) {
            mob.addTag(DISTRACTED);
            mob.setTarget(player);
            Msg.action(player, "The Yt-HurKot turns its attention to you.");
        }
    }

    // ------------------------------------------------------------------ driven attacks

    /** Copies a creature's stats with another attack speed. */
    protected static MonsterStats withSpeed(MonsterStats s, int speed) {
        return new MonsterStats(s.name(), s.combatLevel(), s.hitpoints(), s.attack(), s.strength(), s.defence(), s.magic(), s.ranged(),
                s.defenceBonus(), speed, s.style(), s.maxHit(), s.monster(), s.profile(), s.boss());
    }

    /** A creature's own attack speed (game ticks). */
    protected static int baseSpeed(Entity e) {
        MonsterStats s = ModEntities.stats(e.getType());
        return s == null || s.attackSpeed() <= 0 ? 4 : s.attackSpeed();
    }

    /**
     * Takes over a creature's attacks: its AI still chases or keeps range, but only the session attacks, every {@code speed}
     * ticks from {@code firstDelay} ticks from now ({@link #drivenAttack}).
     */
    protected final void drive(Mob mob, int speed, int firstDelay) {
        if (mob instanceof OsrsMonsterEntity m) {
            m.overrideStats(s -> withSpeed(s, NO_AUTO_ATTACK));
            nextAttack.put(mob.getId(), ticks + firstDelay);
            drivenSpeed.put(mob.getId(), speed);
        }
    }

    /** Gives a driven creature its own attacks back. */
    protected final void release(Mob mob) {
        if (mob instanceof OsrsMonsterEntity m) {
            m.overrideStats(s -> s);
        }
        nextAttack.remove(mob.getId());
        drivenSpeed.remove(mob.getId());
    }

    protected final boolean driven(Entity e) {
        return nextAttack.containsKey(e.getId());
    }

    /** Delays a driven creature's next attack to at least {@code ticksFromNow}. */
    protected final void delayAttack(Entity e, int ticksFromNow) {
        if (driven(e)) {
            nextAttack.put(e.getId(), Math.max(nextAttack.get(e.getId()), ticks + ticksFromNow));
        }
    }

    private void driveTick() {
        if (nextAttack.isEmpty()) {
            return;
        }
        Map<Integer, LivingEntity> alive = new HashMap<>();
        for (LivingEntity e : living(null)) {
            alive.put(e.getId(), e);
        }
        nextAttack.keySet().removeIf(k -> !alive.containsKey(k));
        drivenSpeed.keySet().removeIf(k -> !alive.containsKey(k));
        for (Map.Entry<Integer, Long> e : new ArrayList<>(nextAttack.entrySet())) {
            LivingEntity mob = alive.get(e.getKey());
            if (!(mob instanceof OsrsMonsterEntity m) || ticks < e.getValue()) {
                continue;
            }
            if (drivenAttack(m)) {
                lastAttack.put(m.getId(), ticks);
                nextAttack.put(m.getId(), ticks + drivenSpeed.getOrDefault(m.getId(), baseSpeed(m)));
            }
        }
    }

    /** A driven creature's turn to attack; true when it attacked (or used its turn otherwise), false to try again next tick. */
    protected boolean drivenAttack(OsrsMonsterEntity mob) {
        return attack(mob, null);
    }

    /** The creature's target, if it is a player in this session who can be attacked. */
    protected final ServerPlayer targetOf(Mob mob) {
        return mob.getTarget() instanceof ServerPlayer p && p.isAlive() && !p.isSpectator() && has(p) ? p : null;
    }

    /** Distance from a creature's edge to a player (what an {@link AttackDef}'s range is measured against). */
    protected static double reach(Mob mob, LivingEntity target) {
        return Math.sqrt(mob.distanceToSqr(target)) - mob.getBbWidth() / 2;
    }

    /** One of the creature's attacks by style ("melee", "ranged", "magic"), or null. */
    protected static AttackDef attackOf(OsrsMonsterEntity mob, String style) {
        MonsterStats s = mob.stats();
        if (s == null || s.profile() == null) {
            return null;
        }
        for (AttackDef a : s.profile().attacks) {
            String kind = a.melee() ? "melee" : a.style() == AttackType.RANGED ? "ranged" : "magic";
            if (kind.equals(style)) {
                return a;
            }
        }
        return null;
    }

    /** Attacks the creature's target now with {@code forced} (or its usual weighted choice); false if out of range or sight. */
    protected final boolean attack(OsrsMonsterEntity mob, AttackDef forced) {
        ServerPlayer target = targetOf(mob);
        MonsterStats s = mob.stats();
        if (target == null || s == null || s.profile() == null || s.profile().attacks.isEmpty()) {
            return false;
        }
        double d = reach(mob, target);
        AttackDef a = forced != null ? forced : Mechanics.choose(s.profile().attacks, d, mob.getRandom());
        if (a == null || d > a.range() + 0.5 || !mob.getSensing().hasLineOfSight(target)) {
            return false;
        }
        mob.getLookControl().setLookAt(target, 30f, 30f);
        Mechanics.perform(mob, target, a, 1.0);
        return true;
    }

    // ------------------------------------------------------------------ pausing and resuming

    /** Stats key prefix of a paused run: the wave to start again at (OSRS: logging out restarts the wave). */
    private static final String RESUME = "tzhaar_resume:";
    /** Set while the player logs out: the run pauses instead of ending (no tokkul, the Slayer task kept). */
    protected boolean pausing;

    /**
     * Logging out of the Fight Cave or the Inferno pauses the run (OSRS Wiki "TzHaar Fight Cave", Tips; "Inferno"): the wave
     * in progress starts again, with the same spawns, when the player logs back in.
     */
    @Override
    protected void onDisconnect(ServerPlayer player) {
        if (isEnded() || wave < 1 || playerCount() > 1 || !pauses()) {
            return;
        }
        pausing = true;
        MinigameData data = MinigameData.of(player);
        java.util.UUID u = player.getUUID();
        data.setStat(u, RESUME + game.id(), completedWaves + 1);
        BlockPos back = returnPoint(player);
        if (back != null) {
            data.setStat(u, RESUME + "x:" + game.id(), back.getX());
            data.setStat(u, RESUME + "y:" + game.id(), back.getY());
            data.setStat(u, RESUME + "z:" + game.id(), back.getZ());
        }
        savePause(data, u);
    }

    /** Whether logging out pauses this game (the Fight Cave and the Inferno). */
    protected boolean pauses() {
        return false;
    }

    /** Saves what else a paused run needs (Fight Cave rotation, Inferno pillars). */
    protected void savePause(MinigameData data, java.util.UUID player) {
    }

    /** Restores what {@link #savePause} saved. */
    protected void loadPause(MinigameData data, java.util.UUID player) {
    }

    protected static String pauseKey(String what, Minigame game) {
        return RESUME + what + ":" + game.id();
    }

    /** Picks up a paused run at {@code fromWave}. */
    private void resumeAt(int fromWave) {
        wave = fromWave - 1;
        completedWaves = fromWave - 1;
        nextWaveIn = 8;
    }

    /** The wave a paused run of {@code game} picks up at (0 = none paused). */
    public static int pausedWave(ServerPlayer player, Minigame game) {
        return MinigameData.of(player).stat(player.getUUID(), RESUME + game.id());
    }

    /** Called from {@link #start()}: a player who paused a run of this game (by logging out) picks it up where they left. */
    protected final void resumePaused() {
        for (ServerPlayer p : online()) {
            MinigameData data = MinigameData.of(p);
            int from = data.stat(p.getUUID(), RESUME + game.id());
            if (from <= 0) {
                continue;
            }
            data.setStat(p.getUUID(), RESUME + game.id(), 0);
            loadPause(data, p.getUUID());
            resumeAt(from);
            Msg.game(p, "You resume " + game.displayName() + " at wave " + from + ".");
        }
    }

    /** On login: restarts a paused Fight Cave or Inferno run (back where it was entered from); true if one was resumed. */
    public static boolean resumeOnLogin(ServerPlayer player) {
        for (Minigame game : new Minigame[]{Minigame.FIGHT_CAVES, Minigame.INFERNO}) {
            MinigameData data = MinigameData.of(player);
            java.util.UUID u = player.getUUID();
            if (data.stat(u, RESUME + game.id()) <= 0) {
                continue;
            }
            BlockPos back = new BlockPos(data.stat(u, RESUME + "x:" + game.id()), data.stat(u, RESUME + "y:" + game.id()),
                    data.stat(u, RESUME + "z:" + game.id()));
            MinigameSession s = MinigameManager.startFor(game, game.entranceBlock(), player);
            if (!(s instanceof TzHaarSession t)) {
                return false;
            }
            if (back.getY() != 0) {
                t.setReturnPoint(player, back);
            }
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ self-test hooks

    /** The wave being fought (0 before the first). */
    public final int wave() {
        return wave;
    }

    public final int completedWaves() {
        return completedWaves;
    }

    /** Self-test hook: clears the arena and starts {@code target} next tick (accelerated waves). */
    public void selfTestJumpTo(int target) {
        for (LivingEntity e : living(null)) {
            e.discard();
        }
        wave = target - 1;
        completedWaves = target - 1;
        nextWaveIn = 1;
    }

    /** Self-test hook: the session's living creatures. */
    public final List<LivingEntity> selfTestCreatures() {
        return living(null);
    }

    /** Self-test hook: runs one game tick. */
    public final void selfTestTick() {
        MinigameManager.selfTestTick(this);
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        return List.of(game.displayName(), "Wave: " + Math.max(1, wave) + " / " + totalWaves(), "Monsters: " + mobsAlive());
    }

    /** Whether the player completed the elite Karamja Diary (doubles the Fight Cave's and the Inferno's tokkul). */
    protected static boolean eliteKaramja(ServerPlayer p) {
        return com.runecraft.osrs.content.systems.Diaries.tierComplete(com.runecraft.content.systems.SysState.of(p).diaryDone,
                "karamja", com.runecraft.osrs.content.systems.Diaries.Tier.ELITE);
    }
}
