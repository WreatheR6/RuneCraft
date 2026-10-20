package com.runecraft.content.minigames.games;

import com.runecraft.osrs.CombatFormulas;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.MonsterStats;
import com.runecraft.combat.PoisonService;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.bosses.ColosseumRewards;
import com.runecraft.osrs.content.minigames.FortisColosseum;
import com.runecraft.osrs.content.minigames.FortisColosseum.Modifier;
import com.runecraft.osrs.content.minigames.FortisColosseum.Offer;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.SolHeredit;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Fortis Colosseum (see {@link FortisColosseum}): before every wave Minimus offers three modifiers (chat links, or
 * {@code /minigame colosseum pick N}); the wave's monsters then spawn at random spots, reinforcements arrive from the gate
 * nearest the player after 40 seconds, and clearing the wave adds a chest roll ({@link ColosseumRewards}) and Glory.
 * Between waves the player may end the run ({@code /minigame colosseum leave}, or the exit portal): the Rewards Chest then
 * appears in the centre of the arena and must be searched before leaving. Dying, logging out or leaving any other way
 * forfeits it. Wave 12 is Sol Heredit; his death ends the run with Dizana's quiver in the chest.
 *
 * <p>The arena is built one block per OSRS tile. Monster mechanics: the warband attacks only from melee range (and always
 * takes a max hit from its weak style); Javelin Colossi replace every fifth attack with a javelin landing on the player's
 * tile 6 ticks later; manticores fire their own three-orb pattern a tick apart (a second manticore copies it and the two
 * stagger their volleys 5 ticks apart); minotaurs out of melee reach heal wounded monsters in sight to full.
 *
 * <p>Modifiers modelled: Bees!, Blasphemy, Doom (any damage taken, hazards included), Dynamic Duo, Frailty, Mantimayhem
 * (I: each orb hits twice, II: unprotected orbs venom, III: any order of the three orbs), Myopia (ranged and autocast
 * attacks beyond the weapon's attack range less 2/4/6 tiles do nothing), Reentry, Red Flag (without it minotaurs walk
 * straight at the player and can be safespotted behind a pillar; with it they path-find), Relentless (enemy accuracy as if
 * the player's Defence were 33%/66% lower, or no accuracy roll at III, and +1/+3/+6 max hit), Solarflare (four orbs around
 * the pillars on wave 12), Quartet (also beside Sol Heredit), Totemic and Volatility (Sol Heredit explodes too).
 */
public class ColosseumSession extends MinigameSession {
    private enum State { CHOOSING, STARTING, FIGHTING, REWARDS, DONE }

    /** The Rewards Chest block, placed in the centre of the arena when the run ends. */
    public static final String REWARDS_CHEST = FortisColosseum.REWARDS_CHEST;
    /** MinigameData flag: the player has spoken to Minimus (needed before entering the arena, as in OSRS). */
    public static final String FLAG_BRIEFED = "colosseum_briefed";

    private State state = State.CHOOSING;
    private int wave;
    private int countdown;
    private long waveStart;
    private long totalTicks;
    private boolean reinforced;
    private final Map<Modifier, Integer> active = FortisColosseum.newModifiers();
    private List<Offer> offers = new ArrayList<>();
    private final List<ColosseumRewards.Reward> chest = new ArrayList<>();
    private int glory;
    private boolean cashedOut;
    private boolean died;
    private boolean claimed;
    // Per-wave tracking
    private boolean damaged;
    private boolean manticoreDamage;
    private boolean minotaurHealed;
    private int doom;
    private boolean dooming;
    private final Set<String> waveFlags = new HashSet<>();
    private int flagsWave;
    // Modifier objects
    private final List<LivingEntity> bees = new ArrayList<>();
    private int beeRespawn;
    private final List<Totem> totems = new ArrayList<>();
    private final Map<Integer, Long> totemCooldown = new HashMap<>();
    private long nextSolTotem;
    private final List<Sand> sand = new ArrayList<>();
    private final int[] flareIndex = new int[4];
    private final int[] flarePause = new int[4];
    private final long[] flareMoved = new long[4];
    // Monster mechanics
    private final Map<Integer, Integer> javelinAttacks = new HashMap<>();
    private final List<Javelin> javelins = new ArrayList<>();
    private final Map<Integer, List<AttackType>> manticorePatterns = new HashMap<>();
    private final Map<Integer, Long> manticoreVolleys = new HashMap<>();
    /** Manticore orbs still to land. */
    private final List<Orb> orbs = new ArrayList<>();
    /** Blocks of the gladiator wall around Sol Heredit's arena (relative), removed when the fight ends. */
    private final List<BlockPos> wall = new ArrayList<>();
    private boolean echoing;
    private long lastMyopiaMessage;
    /** Showboating: the sides of the arena saluted while Sol Heredit was below 10% hitpoints. */
    private final Set<String> salutes = new HashSet<>();
    private BlockPos solDiedAt;
    /**
     * The wave's monsters as spawned. Tracked here rather than looked up in the world: an entity added to a chunk that
     * isn't entity-ticking yet (a fresh instance slot, or the self-test running inside one server tick) is invisible to
     * box queries until the chunk becomes accessible.
     */
    private final List<LivingEntity> spawned = new ArrayList<>();
    private final List<Long> forcedChunks = new ArrayList<>();

    private record Sand(BlockPos pos, boolean permanent) {
    }

    private record Orb(LivingEntity mob, ServerPlayer player, AttackType type, long due, int hits) {
    }

    private record Javelin(LivingEntity colossus, BlockPos target, long lands) {
    }

    private record Totem(LivingEntity totem, LivingEntity enemy, long born) {
    }

    private static final int HALF = 18;
    private static final int PILLAR = FortisColosseum.PILLAR_OFFSET;

    public ColosseumSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.FORTIS_COLOSSEUM, id, slot, world, variant);
    }

    /** Children of the Sun opens Varlamore and the Colosseum; Minimus must have briefed the player once. */
    public static String requirement(ServerPlayer player) {
        if (!QuestApi.isComplete(player, "children_of_the_sun")) {
            return "You need to complete Children of the Sun to fight in the Fortis Colosseum.";
        }
        if (!MinigameData.of(player).flag(player.getUUID(), FLAG_BRIEFED)) {
            return "A guard stops you. You should speak to Minimus, the Colosseum Master, before entering the arena.";
        }
        return null;
    }

    // ------------------------------------------------------------------ arena

    @Override
    protected int halfX() {
        return HALF;
    }

    @Override
    protected int halfZ() {
        return HALF;
    }

    @Override
    protected int height() {
        return 12;
    }

    @Override
    protected void build() {
        // Keep the arena loaded (and its entities ticking) for the whole run; released in onClose.
        for (int cx = (origin.getX() - HALF - 2) >> 4; cx <= (origin.getX() + HALF + 2) >> 4; cx++) {
            for (int cz = (origin.getZ() - HALF - 2) >> 4; cz <= (origin.getZ() + HALF + 2) >> 4; cz++) {
                world.getChunk(cx, cz);
                world.setChunkForced(cx, cz, true);
                forcedChunks.add(net.minecraft.world.level.ChunkPos.asLong(cx, cz));
            }
        }
        room(Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.CHISELED_SANDSTONE);
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        for (int y = 1; y <= 6; y++) {
                            set(sx * PILLAR + dx, y, sz * PILLAR + dz, (y == 6 ? Blocks.GOLD_BLOCK : Blocks.SANDSTONE_WALL)
                                    .defaultBlockState());
                        }
                    }
                }
            }
        }
        // The north and south gates the reinforcements come through.
        for (int dx = -2; dx <= 2; dx++) {
            set(dx, 0, -HALF, Blocks.RED_SANDSTONE.defaultBlockState());
            set(dx, 0, HALF, Blocks.RED_SANDSTONE.defaultBlockState());
        }
        place(4, HALF - 2, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return rel(0, 1, 4);
    }

    @Override
    protected void start() {
        offer();
    }

    // ------------------------------------------------------------------ modifier choice and ending the run

    private void offer() {
        state = State.CHOOSING;
        offers = FortisColosseum.offers(wave + 1, active, random);
        for (ServerPlayer p : online()) {
            showOffers(p);
        }
    }

    private void showOffers(ServerPlayer p) {
        Msg.game(p, "Minimus: Choose a modifier for wave " + (wave + 1) + ".");
        MutableComponent line = Component.literal("").withStyle(ChatFormatting.GRAY);
        for (int i = 0; i < offers.size(); i++) {
            Offer o = offers.get(i);
            line.append(link(o.label() + " (+" + o.modifier().glory(o.tier()) + " Glory/wave)", "/minigame colosseum pick " + (i + 1),
                    "Pick " + o.label(), ChatFormatting.GOLD)).append(Component.literal(" "));
        }
        if (wave >= 1) {
            line.append(link("Leave with the loot", "/minigame colosseum leave", "End the run: the Rewards Chest appears in the arena",
                    ChatFormatting.GREEN));
        }
        p.displayClientMessage(line, false);
    }

    private static MutableComponent link(String label, String command, String hover, ChatFormatting color) {
        return Component.literal("[" + label + "]").withStyle(s -> s.withColor(color)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover))));
    }

    /** The modifiers on offer (self-test and HUD). */
    public List<Offer> offers() {
        return List.copyOf(offers);
    }

    public Map<Modifier, Integer> modifiers() {
        return Map.copyOf(active);
    }

    public int wave() {
        return wave;
    }

    public int glory() {
        return glory;
    }

    public List<ColosseumRewards.Reward> chest() {
        return List.copyOf(chest);
    }

    /** A player picked offer {@code index} (1-3). */
    public boolean pick(ServerPlayer p, int index) {
        if (state != State.CHOOSING || index < 1 || index > offers.size()) {
            Msg.game(p, "There is no modifier to choose right now.");
            return false;
        }
        Offer o = offers.get(index - 1);
        FortisColosseum.choose(active, o);
        Msg.game(p, "Modifier chosen: " + o.label() + ".");
        offers = new ArrayList<>();
        state = State.STARTING;
        countdown = FortisColosseum.WAVE_START_DELAY;
        return true;
    }

    /**
     * Ends the run between waves (OSRS: telling Minimus you're done): the Rewards Chest appears in the centre of the arena
     * with everything earned so far.
     */
    public boolean cashOut(ServerPlayer p) {
        if (state != State.CHOOSING || wave < 1) {
            Msg.game(p, "You can only leave with your loot between waves.");
            return false;
        }
        cashedOut = true;
        offers = new ArrayList<>();
        Msg.game(p, "Minimus: Wise choice. Your loot awaits you in the Rewards Chest.");
        toRewards();
        return true;
    }

    /** The run is over (cashed out or Sol Heredit slain): the Rewards Chest appears in the centre of the arena. */
    private void toRewards() {
        state = State.REWARDS;
        removeWall();
        clearSand(true);
        com.runecraft.boss.Mechanics.clearHazards(world, box()); // Sol Heredit's molten sand subsides
        javelins.clear();
        orbs.clear();
        for (LivingEntity e : spawned) {
            e.discard();
        }
        spawned.clear();
        for (LivingEntity b : bees) {
            b.discard();
        }
        bees.clear();
        for (Totem t : totems) {
            t.totem().discard();
        }
        totems.clear();
        place(0, 0, REWARDS_CHEST);
        broadcast("The Rewards Chest appears in the centre of the arena. Search it to claim your loot.");
    }

    /** Whether the run is over and the Rewards Chest waits in the arena (self-test). */
    public boolean rewardsReady() {
        return state == State.REWARDS;
    }

    /** Where the Rewards Chest stands once the run is over (self-test). */
    public BlockPos chestPos() {
        return rel(0, 1, 0);
    }

    /** Searching the Rewards Chest: hands over the loot and ends the run (Minimus sees the player out). */
    public boolean claimChest(ServerPlayer p) {
        if (state != State.REWARDS) {
            Msg.game(p, "The Rewards Chest is empty.");
            return false;
        }
        claim(p);
        claimed = true;
        state = State.DONE;
        set(0, 1, 0, Blocks.AIR.defaultBlockState());
        Msg.game(p, "Minimus escorts you back to the lobby.");
        end(true);
        return true;
    }

    /** Gives the chest's contents to a player (once). */
    public void claim(ServerPlayer p) {
        if (chest.isEmpty()) {
            return;
        }
        Msg.game(p, "You search the Rewards Chest.");
        for (ColosseumRewards.Reward r : chest) {
            reward(p, r.item(), r.count());
            if (ColosseumRewards.unique(r.item())) {
                p.getServer().getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString() + " received a drop: "
                        + displayName(r.item()) + " from the Fortis Colosseum.").withStyle(ChatFormatting.GOLD), false);
            }
        }
        chest.clear();
    }

    private static String displayName(String item) {
        String s = item.replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ------------------------------------------------------------------ waves

    @Override
    protected void gameTick() {
        switch (state) {
            case CHOOSING -> {
                if (ticks % 100 == 0) {
                    for (ServerPlayer p : online()) {
                        showOffers(p);
                    }
                }
            }
            case STARTING -> {
                if (--countdown <= 0) {
                    startWave();
                }
            }
            case FIGHTING -> fightTick();
            case REWARDS, DONE -> {
            }
        }
        if (state == State.FIGHTING) {
            sandTick(); // molten sand only burns during a wave
        }
        if (state != State.DONE) {
            frailtyTick();
        }
        if (solDiedAt != null) {
            // Sol drops nothing in OSRS: the chest holds the loot.
            for (ItemEntity item : world.getEntitiesOfClass(ItemEntity.class, box(), e -> true)) {
                item.discard();
            }
            solDiedAt = null;
            if (state == State.FIGHTING) {
                completeWave();
            }
        }
    }

    /** Starts the next wave now (self-test and the countdown). */
    public void startWave() {
        wave++;
        state = State.FIGHTING;
        waveStart = ticks;
        reinforced = false;
        damaged = false;
        manticoreDamage = false;
        minotaurHealed = false;
        doom = 0;
        javelinAttacks.clear();
        javelins.clear();
        orbs.clear();
        manticorePatterns.clear();
        manticoreVolleys.clear();
        java.util.Arrays.fill(flareIndex, 0);
        java.util.Arrays.fill(flarePause, 0);
        java.util.Arrays.fill(flareMoved, 0L);
        boolean solWave = wave == FortisColosseum.WAVES;
        if (solWave) {
            gladiatorWall();
            nextSolTotem = 0;
            // The swarms re-form inside the gladiators' ring.
            for (LivingEntity b : bees) {
                b.discard();
            }
            bees.clear();
        }
        broadcastRed("Wave: " + wave + (solWave ? " - Sol Heredit leaps into the arena!" : ""));
        List<String> monsters = FortisColosseum.startingMonsters(wave, active, random);
        BlockPos shockwave = null;
        for (String id : monsters) {
            BlockPos at;
            if (id.equals(FortisColosseum.SOL_HEREDIT)) {
                at = rel(0, 1, -6);
            } else if (solWave) {
                at = rel(random.nextInt(9) - 4, 1, -4); // Quartet's warbander, inside the ring
            } else if (id.equals(FortisColosseum.SHOCKWAVE) && shockwave != null && active.containsKey(Modifier.DYNAMIC_DUO)) {
                at = shockwave.offset(random.nextBoolean() ? 3 : -3, 0, random.nextInt(3) - 1); // Dynamic Duo: near its partner
            } else {
                at = spawnSpot();
            }
            if (id.equals(FortisColosseum.SHOCKWAVE) && shockwave == null) {
                shockwave = at;
            }
            spawnWaveMonster(id, at);
        }
        int beesWanted = active.getOrDefault(Modifier.BEES, 0);
        bees.removeIf(b -> !b.isAlive());
        for (int i = bees.size(); i < beesWanted; i++) {
            spawnBee();
        }
    }

    /** One of the arena's spawn spots, away from the pillars and the player's start. */
    private BlockPos spawnSpot() {
        for (int i = 0; i < 20; i++) {
            BlockPos p = randomFloor(0, -4, HALF - 3);
            BlockPos d = p.subtract(origin);
            boolean pillar = Math.abs(Math.abs(d.getX()) - PILLAR) <= 2 && Math.abs(Math.abs(d.getZ()) - PILLAR) <= 2;
            if (!pillar && d.getZ() < 2) {
                return p;
            }
        }
        return rel(0, 1, -10);
    }

    /** How many monsters of the current wave are alive (self-test). */
    public int monstersAlive() {
        return waveMonsters().size();
    }

    /** The current wave's monsters (self-test: killed through the player's real hit path). */
    public List<LivingEntity> waveMonstersForTest() {
        return waveMonsters();
    }

    /** Removes the current wave's monsters (self-test: the wave then completes on the next tick). */
    public void clearWaveForTest() {
        for (LivingEntity e : waveMonsters()) {
            e.discard();
        }
    }

    /** Whether the run is between waves, waiting for a modifier choice. */
    public boolean choosing() {
        return state == State.CHOOSING;
    }

    /** Whether a wave is being fought (self-test). */
    public boolean fighting() {
        return state == State.FIGHTING;
    }

    /** Monsters that hold up a wave (not bee swarms or healing totems). */
    private List<LivingEntity> waveMonsters() {
        spawned.removeIf(e -> !e.isAlive() || e.isRemoved());
        return new ArrayList<>(spawned);
    }

    private void spawnWaveMonster(String id, BlockPos at) {
        Mob mob = spawn(id, at);
        if (mob == null) {
            com.runecraft.RuneCraft.LOGGER.warn("Fortis Colosseum: could not spawn {}", id);
            return;
        }
        if (mob instanceof OsrsMonsterEntity m) {
            m.meleeRangeOnly = FortisColosseum.attacksFromMelee(id);
            switch (id) {
                case FortisColosseum.SHAMAN -> m.rangedReach = FortisColosseum.SHAMAN_RANGE;
                case FortisColosseum.SHOCKWAVE -> m.rangedReach = FortisColosseum.COLOSSUS_RANGE;
                case FortisColosseum.JAVELIN -> {
                    m.rangedReach = FortisColosseum.COLOSSUS_RANGE;
                    m.attackInterceptor = this::javelinAttack;
                }
                case FortisColosseum.MANTICORE -> {
                    m.rangedReach = FortisColosseum.MANTICORE_RANGE;
                    m.attackInterceptor = this::manticoreVolley;
                }
                case FortisColosseum.MINOTAUR -> m.directPathing = !active.containsKey(Modifier.RED_FLAG);
                default -> {
                }
            }
        }
        spawned.add(mob);
        aggro(mob);
    }

    private void fightTick() {
        long waveTicks = ticks - waveStart;
        List<LivingEntity> monsters = waveMonsters();
        // A wave cleared on the tick its reinforcements are due is complete (OSRS kills them as they arrive).
        if (waveTicks > 2 && monsters.isEmpty() && wave < FortisColosseum.WAVES) {
            completeWave();
            return;
        }
        if (!reinforced && wave < FortisColosseum.WAVES && waveTicks >= FortisColosseum.REINFORCEMENT_TICKS) {
            reinforced = true;
            List<ServerPlayer> online = online();
            boolean north = !online.isEmpty() && relPos(online.get(0)).getZ() < 0;
            for (String id : FortisColosseum.wave(wave).reinforcements()) {
                spawnWaveMonster(id, rel(random.nextInt(5) - 2, 1, north ? -HALF + 2 : HALF - 2));
            }
            broadcastRed("Reinforcements arrive from the " + (north ? "north" : "south") + " gate!");
            monsters = waveMonsters();
        }
        // Each step can end the run (a Doom death), after which the rest has nothing left to act on.
        for (Runnable step : new Runnable[]{this::orbTick, this::javelinTick, () -> minotaurTick(waveMonsters()),
                () -> totemTick(waveMonsters()), this::beeTick, this::solarflareTick}) {
            if (isEnded() || state != State.FIGHTING) {
                return;
            }
            step.run();
        }
    }

    private void completeWave() {
        long waveTicks = ticks - waveStart;
        totalTicks += waveTicks;
        int gained = FortisColosseum.glory(wave, waveTicks, !damaged, active);
        glory += gained;
        doom = 0;
        waveFlags.clear();
        flagsWave = wave;
        if (!manticoreDamage) {
            waveFlags.add(FortisColosseum.FLAG_NO_MANTICORE_DAMAGE);
        }
        if (!minotaurHealed) {
            waveFlags.add(FortisColosseum.FLAG_MINOTAUR_NEVER_HEALED);
        }
        if (wave == 11 && FortisColosseum.oneOff(active)) {
            waveFlags.add(FortisColosseum.FLAG_ONE_OFF);
        }
        if (wave == FortisColosseum.WAVES) {
            if (FortisColosseum.reinforcements(active)) {
                waveFlags.add(FortisColosseum.FLAG_REINFORCEMENTS);
            }
            if (totalTicks <= FortisColosseum.SPEED_CHASER_TICKS) {
                waveFlags.add(FortisColosseum.FLAG_SPEED_CHASER);
            }
            if (totalTicks <= FortisColosseum.SPEED_RUNNER_TICKS) {
                waveFlags.add(FortisColosseum.FLAG_SPEED_RUNNER);
            }
            if (salutes.size() == 4) {
                waveFlags.add(FortisColosseum.FLAG_SHOWBOATING);
            }
        }
        // Leftover floor items (none drop in OSRS), molten sand that isn't permanent and totems go.
        for (ItemEntity item : world.getEntitiesOfClass(ItemEntity.class, box(), e -> true)) {
            item.discard();
        }
        clearSand(false);
        for (Totem t : totems) {
            t.totem().discard();
        }
        totems.clear();
        totemCooldown.clear();
        javelins.clear();
        orbs.clear();
        // One chest per run (solo): duplicate protection looks at the sunfire pieces in the players' collection logs.
        Set<String> owned = new HashSet<>();
        for (ServerPlayer p : online()) {
            owned.addAll(Players.data(p).collectionLog());
        }
        for (ColosseumRewards.Reward r : ColosseumRewards.roll(wave, owned, random)) {
            ColosseumRewards.add(chest, r);
        }
        for (ServerPlayer p : online()) {
            completeWaveFor(p, waveTicks, gained);
        }
        if (wave >= FortisColosseum.WAVES) {
            toRewards();
        } else {
            offer();
        }
    }

    private void completeWaveFor(ServerPlayer p, long waveTicks, int gained) {
        UUID u = p.getUUID();
        if (active.getOrDefault(Modifier.MANTIMAYHEM, 0) >= 2) {
            PoisonService.cure(p, true, 0, com.runecraft.action.ActionManager.gameTick());
        }
        MinigameData data = MinigameData.of(p);
        // "kc:" counts waves cleared (the CA run of each wave completes on it).
        data.incrementStat(u, "kc:" + game.id());
        if (wave > data.stat(u, FortisColosseum.STAT_BEST_WAVE)) {
            data.setStat(u, FortisColosseum.STAT_BEST_WAVE, wave);
        }
        Msg.game(p, "Wave " + wave + " completed! Wave duration: " + time(waveTicks) + ". Total: " + time(totalTicks) + ".");
        Msg.game(p, "Glory earned: " + gained + " (run total " + glory + ").");
        int best = data.stat(u, FortisColosseum.STAT_GLORY);
        if (glory > best) {
            data.setStat(u, FortisColosseum.STAT_GLORY, glory);
            String before = FortisColosseum.rank(best);
            String now = FortisColosseum.rank(glory);
            if (now != null && !now.equals(before)) {
                Msg.game(p, "You have earned the rank of " + now + " in the Fortis Colosseum!");
            }
        }
        Msg.game(p, "The Rewards Chest now holds: " + describeChest() + ".");
        if (wave == FortisColosseum.TOKEN_WAVE) {
            varlamoreToken(p);
        }
        if (wave == FortisColosseum.WAVES) {
            int n = data.incrementStat(u, FortisColosseum.STAT_COMPLETIONS);
            Msg.game(p, "Ralos has smiled upon you! Fortis Colosseum completions: " + n + ".");
            int bestTime = data.stat(u, FortisColosseum.STAT_BEST_TIME);
            if (bestTime == 0 || totalTicks < bestTime) {
                data.setStat(u, FortisColosseum.STAT_BEST_TIME, (int) totalTicks);
                Msg.game(p, "New personal best: " + time(totalTicks) + ".");
            }
        }
    }

    /** MinigameData stat: the epoch day (+1) of the last Varlamore token from the Colosseum. */
    public static final String STAT_TOKEN_DAY = "colosseum_token_day";

    /**
     * Wave 3 hands out a Varlamore token once a day when the player's name length is assigned the Colosseum that day, and
     * no token is already in the inventory (OSRS also needs a partyhat, which RuneCraft doesn't have - approx).
     */
    public static boolean varlamoreToken(ServerPlayer p) {
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
        String activity = FortisColosseum.tokenActivity(p.getName().getString().length(), today.getDayOfWeek().getValue());
        MinigameData data = MinigameData.of(p);
        int day = (int) today.toEpochDay() + 1;
        if (!activity.equals("colosseum") || data.stat(p.getUUID(), STAT_TOKEN_DAY) == day
                || Inv.has(p, FortisColosseum.TOKEN, 1)) {
            return false;
        }
        data.setStat(p.getUUID(), STAT_TOKEN_DAY, day);
        reward(p, FortisColosseum.TOKEN, 1);
        Msg.game(p, "As the crowd cheers, you spot a small token in the sand and pick it up.");
        return true;
    }

    private String describeChest() {
        List<String> parts = new ArrayList<>();
        for (ColosseumRewards.Reward r : chest) {
            parts.add(r.count() + " x " + displayName(r.item()));
        }
        return parts.isEmpty() ? "nothing" : String.join(", ", parts);
    }

    @Override
    protected void onKill(ServerPlayer player, LivingEntity entity, String creatureId) {
        if (!creatureId.equals(FortisColosseum.BEE_SWARM) && !creatureId.equals(FortisColosseum.HEALING_TOTEM)) {
            explode(entity, creatureId);
        }
        if (creatureId.equals(FortisColosseum.BEE_SWARM)) {
            beeRespawn = FortisColosseum.BEE_RESPAWN_TICKS;
        }
        if (creatureId.equals(FortisColosseum.SOL_HEREDIT) && state == State.FIGHTING) {
            solDiedAt = entity.blockPosition();
        }
    }

    /**
     * Volatility: an enemy explodes on death one (I) or two (II, III) tiles beyond its size, Sol Heredit included (which can
     * still kill the player and lose the loot); III leaves molten sand on the centre tile.
     */
    private void explode(LivingEntity entity, String creatureId) {
        int tier = active.getOrDefault(Modifier.VOLATILITY, 0);
        if (tier <= 0) {
            return;
        }
        int size = FortisColosseum.size(creatureId);
        BlockPos at = entity.blockPosition();
        int r = FortisColosseum.volatilityRadius(size, tier);
        world.sendParticles(ParticleTypes.EXPLOSION, entity.getX(), entity.getY() + 1, entity.getZ(), 4 + r, r / 2.0, 0.5, r / 2.0, 0);
        for (ServerPlayer p : online()) {
            if (FortisColosseum.volatilityHits(size, tier, p.getBlockX() - at.getX(), p.getBlockZ() - at.getZ())) {
                CombatHooks.nameAttack("volatility", null);
                hurt(p, 1 + random.nextInt(FortisColosseum.VOLATILITY_DAMAGE));
            }
        }
        if (tier >= 3) {
            addSand(at.subtract(origin), active.getOrDefault(Modifier.REENTRY, 0) >= 2);
        }
    }

    // ------------------------------------------------------------------ monster mechanics

    /**
     * The Javelin Colossus's attacks: after four auto-attacks the fifth is a javelin launched high into the air at the tile
     * the player stands on, landing 6 ticks later (up to 40 typeless damage; prayer doesn't help).
     */
    private boolean javelinAttack(OsrsMonsterEntity mob, ServerPlayer target, AttackDef attack) {
        if (MinigameManager.owner(mob) != this || state != State.FIGHTING) {
            return false;
        }
        int n = javelinAttacks.merge(mob.getId(), 1, Integer::sum);
        if (!FortisColosseum.javelinThrow(n)) {
            return false;
        }
        mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        javelins.add(new Javelin(mob, target.blockPosition(), ticks + FortisColosseum.JAVELIN_FLIGHT_TICKS));
        Msg.action(target, "The Javelin Colossus launches a javelin high into the air!");
        return true;
    }

    private void javelinTick() {
        // Landed javelins are taken off the list first: their damage can end the run (Doom), which clears it.
        List<Javelin> landed = new ArrayList<>();
        javelins.removeIf(j -> {
            BlockPos t = j.target();
            world.sendParticles(ParticleTypes.CRIT, t.getX() + 0.5, t.getY() + 0.2, t.getZ() + 0.5, 3, 0.25, 0.05, 0.25, 0);
            if (ticks < j.lands()) {
                return false;
            }
            landed.add(j);
            return true;
        });
        for (Javelin j : landed) {
            if (isEnded()) {
                return;
            }
            BlockPos t = j.target();
            world.sendParticles(ParticleTypes.EXPLOSION, t.getX() + 0.5, t.getY() + 0.5, t.getZ() + 0.5, 1, 0, 0, 0, 0);
            LivingEntity source = j.colossus().isAlive() ? j.colossus() : null;
            for (ServerPlayer p : online()) {
                if (p.getBlockX() == t.getX() && p.getBlockZ() == t.getZ()) {
                    CombatHooks.nameAttack("sky_javelin", null);
                    int dmg = random.nextInt(FortisColosseum.JAVELIN_MAX_HIT + 1);
                    if (source != null) {
                        CombatHandler.applyDamage(source, p, dmg);
                    } else {
                        hurt(p, dmg);
                    }
                }
            }
            int reentry = active.getOrDefault(Modifier.REENTRY, 0);
            BlockPos d = t.subtract(origin);
            for (int[] off : FortisColosseum.reentryTiles(reentry)) {
                addSand(d.offset(off[0], 0, off[1]), reentry >= 2);
            }
        }
    }

    /** A manticore's charged volley: its three orbs, in its own pattern, a tick apart (staggered behind another's). */
    private boolean manticoreVolley(OsrsMonsterEntity mob, ServerPlayer target, AttackDef attack) {
        if (MinigameManager.owner(mob) != this || state != State.FIGHTING) {
            return false;
        }
        int id = mob.getId();
        if (manticoreVolleys.getOrDefault(id, Long.MIN_VALUE / 2) > ticks) {
            return true; // still waiting to launch the last one
        }
        int tier = active.getOrDefault(Modifier.MANTIMAYHEM, 0);
        List<AttackType> pattern = manticorePatterns.get(id);
        long other = Long.MIN_VALUE / 2;
        List<AttackType> copy = null;
        for (LivingEntity e : waveMonsters()) {
            if (e != mob && MinigameManager.creatureId(e).equals(FortisColosseum.MANTICORE)) {
                copy = copy != null ? copy : manticorePatterns.get(e.getId());
                other = Math.max(other, manticoreVolleys.getOrDefault(e.getId(), Long.MIN_VALUE / 2));
            }
        }
        if (pattern == null) {
            pattern = FortisColosseum.manticorePattern(tier, copy, random);
            manticorePatterns.put(id, pattern);
        }
        long at = FortisColosseum.manticoreVolleyTick(ticks, other);
        manticoreVolleys.put(id, at);
        int hits = FortisColosseum.manticoreHitsPerOrb(tier);
        for (int i = 0; i < pattern.size(); i++) {
            orbs.add(new Orb(mob, target, pattern.get(i), at + i, hits));
        }
        world.sendParticles(ParticleTypes.ENCHANT, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 20, 0.5, 0.4, 0.5, 0.2);
        return true;
    }

    /** The tick a manticore's last volley was (or will be) launched (self-test). */
    public long manticoreVolleyAt(LivingEntity manticore) {
        return manticoreVolleys.getOrDefault(manticore.getId(), Long.MIN_VALUE / 2);
    }

    /** Self-test: sets a modifier now, so its mechanics can be walked whatever Minimus offered. */
    public void selfTestForce(Modifier modifier, int tier) {
        active.merge(modifier, Math.max(1, Math.min(tier, modifier.maxTier())), Math::max); // never a downgrade
    }

    /** Self-test: the player dies in the arena (the minigame death path: the chest is lost, back to the lobby). */
    public void selfTestDie(ServerPlayer p) {
        p.setHealth(p.getMaxHealth());
        onDeath(p);
    }

    /** The orb pattern a manticore was given (self-test), or null before it first charged. */
    public List<AttackType> manticorePattern(LivingEntity manticore) {
        return manticorePatterns.get(manticore.getId());
    }

    /** How many manticore orbs and sky javelins are on their way (self-test). */
    public int orbsPending() {
        return orbs.size();
    }

    public int javelinsPending() {
        return javelins.size();
    }

    private void mayhemVenom(ServerPlayer player, AttackType style, int tier) {
        if (tier >= 2 && !CombatHandler.isProtected(Players.data(player), style)) {
            PoisonService.venom(player, com.runecraft.action.ActionManager.gameTick());
        }
    }

    private void orbTick() {
        int tier = active.getOrDefault(Modifier.MANTIMAYHEM, 0);
        // Due orbs are taken off the list first: a hit can end the run (Doom), which clears it.
        List<Orb> due = new ArrayList<>();
        orbs.removeIf(o -> {
            if (ticks < o.due()) {
                return false;
            }
            due.add(o);
            return true;
        });
        for (Orb o : due) {
            if (isEnded() || !o.mob().isAlive() || !o.player().isAlive() || MinigameManager.session(o.player()) != this) {
                continue;
            }
            world.sendParticles(o.type() == AttackType.MAGIC ? ParticleTypes.WITCH : o.type() == AttackType.RANGED ? ParticleTypes.CRIT
                    : ParticleTypes.FLAME, o.player().getX(), o.player().getY(0.6), o.player().getZ(), 6, 0.3, 0.3, 0.3, 0.0);
            String name = o.type() == AttackType.MAGIC ? "magic_orb" : o.type() == AttackType.RANGED ? "ranged_orb" : "melee_orb";
            for (int i = 0; i < o.hits(); i++) {
                extraHit(o.mob(), o.player(), o.type(), FortisColosseum.manticoreOrbMaxHit(o.type()), name);
            }
            mayhemVenom(o.player(), o.type(), tier);
        }
    }

    /**
     * A minotaur out of melee reach heals a monster below 75% to full if that monster is within 7 tiles and in its line of
     * sight (checked on its 5-tick attack timer).
     */
    private void minotaurTick(List<LivingEntity> monsters) {
        if (ticks % 5 != 0) {
            return;
        }
        for (LivingEntity mino : monsters) {
            if (!MinigameManager.creatureId(mino).equals(FortisColosseum.MINOTAUR)) {
                continue;
            }
            // Red Flag gives the minotaurs smart pathing; otherwise they walk straight at the player (safespottable).
            if (mino instanceof OsrsMonsterEntity m) {
                m.directPathing = !active.containsKey(Modifier.RED_FLAG);
            }
            double reach = mino.getBbWidth() * 1.4 + 1.2;
            boolean inReach = online().stream().anyMatch(p -> p.distanceToSqr(mino) <= reach * reach);
            if (inReach) {
                continue;
            }
            for (LivingEntity other : monsters) {
                if (other == mino || MinigameManager.creatureId(other).equals(FortisColosseum.MINOTAUR)) {
                    continue;
                }
                double tiles = Math.sqrt(other.distanceToSqr(mino));
                if (FortisColosseum.minotaurHeals(other.getHealth(), other.getMaxHealth(), tiles, mino.hasLineOfSight(other))) {
                    other.setHealth(other.getMaxHealth());
                    minotaurHealed = true;
                    world.sendParticles(ParticleTypes.HEART, other.getX(), other.getY() + other.getBbHeight(), other.getZ(), 4, 0.4, 0.3,
                            0.4, 0);
                    broadcast("The minotaur heals the " + other.getName().getString() + "!");
                    break;
                }
            }
        }
    }

    /**
     * Totemic: a healing totem rises to the south-west of a monster at half health and, after about 5 ticks, heals it 30%
     * (Sol Heredit 75) every 7 ticks until it is back to full. A destroyed totem comes back two minutes later; on wave 12
     * another totem joins every two minutes after the first.
     */
    private void totemTick(List<LivingEntity> monsters) {
        if (!active.containsKey(Modifier.TOTEMIC)) {
            return;
        }
        totems.removeIf(t -> {
            if (!t.enemy().isAlive() || t.enemy().isRemoved()) {
                t.totem().discard();
                return true;
            }
            if (!t.totem().isAlive() || t.totem().isRemoved()) {
                totemCooldown.put(t.enemy().getId(), ticks + FortisColosseum.TOTEM_RESPAWN_TICKS);
                return true;
            }
            return false;
        });
        for (LivingEntity e : monsters) {
            boolean sol = MinigameManager.creatureId(e).equals(FortisColosseum.SOL_HEREDIT);
            boolean has = totems.stream().anyMatch(t -> t.enemy() == e);
            if (!has && e.getHealth() <= e.getMaxHealth() / 2 && ticks >= totemCooldown.getOrDefault(e.getId(), 0L)) {
                raiseTotem(e);
                if (sol && nextSolTotem == 0) {
                    nextSolTotem = ticks + SolHeredit.TOTEM_EVERY_TICKS;
                }
            } else if (sol && has && nextSolTotem > 0 && ticks >= nextSolTotem) {
                raiseTotem(e);
                nextSolTotem = ticks + SolHeredit.TOTEM_EVERY_TICKS;
            }
        }
        if (ticks % FortisColosseum.TOTEM_HEAL_TICKS != 0) {
            return;
        }
        for (Totem t : totems) {
            LivingEntity e = t.enemy();
            if (ticks - t.born() < FortisColosseum.TOTEM_FIRST_HEAL_TICKS || !FortisColosseum.totemHeals(e.getHealth(), e.getMaxHealth())) {
                continue;
            }
            if (e instanceof com.runecraft.boss.DataBossEntity sol && MinigameManager.creatureId(e).equals(FortisColosseum.SOL_HEREDIT)) {
                // Sol Heredit: 75 of his 1,500 hitpoints per heal (Minecraft health is scaled to his true hitpoints).
                e.heal(SolHeredit.TOTEM_HEAL * sol.getMaxHealth() / Math.max(1, sol.trueHitpoints()));
            } else {
                e.heal(e.getMaxHealth() * FortisColosseum.TOTEM_HEAL_PERCENT / 100f);
            }
            world.sendParticles(ParticleTypes.HAPPY_VILLAGER, e.getX(), e.getY() + e.getBbHeight(), e.getZ(), 5, 0.4, 0.3, 0.4, 0);
        }
    }

    private void raiseTotem(LivingEntity enemy) {
        int off = (FortisColosseum.size(MinigameManager.creatureId(enemy)) + 1) / 2 + 1;
        BlockPos at = enemy.blockPosition().offset(-off, 0, off); // south-west of it
        BlockPos d = at.subtract(origin);
        if (Math.abs(d.getX()) >= HALF - 1 || Math.abs(d.getZ()) >= HALF - 1) {
            at = enemy.blockPosition().offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
        }
        Mob totem = spawn(FortisColosseum.HEALING_TOTEM, at);
        if (totem != null) {
            totem.setNoAi(true);
            totems.add(new Totem(totem, enemy, ticks));
            broadcastRed("A healing totem rises beside the " + enemy.getName().getString() + "!");
        }
    }

    /** The healing totems standing (self-test). */
    public int totemCount() {
        totems.removeIf(t -> !t.totem().isAlive());
        return totems.size();
    }

    private void spawnBee() {
        int radius = wave >= FortisColosseum.WAVES ? PILLAR - 2 : HALF - 3;
        BlockPos p = randomFloor(0, 0, radius);
        Mob bee = spawn(FortisColosseum.BEE_SWARM, p);
        if (bee != null) {
            bee.setNoAi(true);
            bee.moveTo(p.getX(), p.getY(), p.getZ(), 0, 0); // a 2x2 swarm: centred on a block corner
            bees.add(bee);
        }
    }

    /** The bee swarms in the arena (self-test). */
    public int beeCount() {
        bees.removeIf(b -> !b.isAlive());
        return bees.size();
    }

    /** Bees!: the 2x2 swarms drift a tile towards the player every 12 ticks and sting whoever stands under them every tick. */
    private void beeTick() {
        int wanted = active.getOrDefault(Modifier.BEES, 0);
        if (wanted <= 0) {
            return;
        }
        bees.removeIf(b -> !b.isAlive());
        if (bees.size() < wanted && --beeRespawn <= 0) {
            spawnBee();
            beeRespawn = FortisColosseum.BEE_RESPAWN_TICKS;
        }
        List<ServerPlayer> online = online();
        if (online.isEmpty()) {
            return;
        }
        ServerPlayer target = online.get(0);
        int limit = wave >= FortisColosseum.WAVES ? PILLAR - 1 : HALF - 2;
        for (LivingEntity bee : bees) {
            if (ticks % FortisColosseum.BEE_MOVE_TICKS == 0) {
                double step = FortisColosseum.BEE_STEP_TILES;
                double nx = bee.getX() + Math.signum(Math.round(target.getX() - bee.getX())) * step;
                double nz = bee.getZ() + Math.signum(Math.round(target.getZ() - bee.getZ())) * step;
                nx = Math.max(origin.getX() - limit, Math.min(origin.getX() + limit, nx));
                nz = Math.max(origin.getZ() - limit, Math.min(origin.getZ() + limit, nz));
                bee.moveTo(nx, bee.getY(), nz, bee.getYRot(), 0);
            }
            if (Math.abs(target.getX() - bee.getX()) < 1.0 && Math.abs(target.getZ() - bee.getZ()) < 1.0) {
                CombatHooks.nameAttack("bees", null);
                hurt(target, random.nextInt(FortisColosseum.BEE_DAMAGE + 1));
                PoisonService.poison(target, 1, com.runecraft.action.ActionManager.gameTick());
            }
        }
    }

    /**
     * Solarflare: an orb circles the pillars (four orbs, one around each pillar, on wave 12), moving every 2 ticks (III:
     * every tick) and pausing at corners; it burns the tile it stands on (I/II: only on the tick after it arrives) and III
     * also scorches away prayers.
     */
    private void solarflareTick() {
        int tier = active.getOrDefault(Modifier.SOLARFLARE, 0);
        if (tier <= 0) {
            return;
        }
        boolean solWave = wave >= FortisColosseum.WAVES;
        for (int o = 0; o < FortisColosseum.solarflareOrbs(solWave); o++) {
            List<int[]> path = FortisColosseum.solarflarePath(solWave, o);
            if (flarePause[o] > 0) {
                flarePause[o]--;
            } else if (ticks % FortisColosseum.solarflareMoveTicks(tier) == 0) {
                flareIndex[o] = (flareIndex[o] + 1) % path.size();
                flareMoved[o] = ticks;
                if (FortisColosseum.solarflareCorner(solWave, flareIndex[o])) {
                    flarePause[o] = FortisColosseum.solarflareCornerPause(tier);
                }
            }
            int[] t = path.get(flareIndex[o] % path.size());
            BlockPos orb = rel(t[0], 1, t[1]);
            world.sendParticles(ParticleTypes.FLAME, orb.getX() + 0.5, orb.getY() + 0.8, orb.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.01);
            boolean burning = tier >= 3 || ticks > flareMoved[o];
            if (!burning) {
                continue;
            }
            for (ServerPlayer p : online()) {
                if (p.getBlockX() == orb.getX() && p.getBlockZ() == orb.getZ()) {
                    CombatHooks.nameAttack("solarflare", null);
                    hurt(p, 1 + random.nextInt(FortisColosseum.solarflareDamage(tier)));
                    if (tier >= 3) {
                        Players.data(p).activePrayers().clear();
                        com.runecraft.network.ServerNetworking.sync(p);
                        Msg.red(p, "The solar flare scorches away your prayers!");
                    }
                }
            }
        }
    }

    /** Where Solarflare orb {@code o} burns now, relative to the arena centre (self-test). */
    public int[] solarflareTile(int o) {
        return FortisColosseum.solarflarePath(wave >= FortisColosseum.WAVES, o).get(flareIndex[o]);
    }

    // ------------------------------------------------------------------ molten sand

    private void addSand(BlockPos relative, boolean permanent) {
        BlockPos floor = new BlockPos(relative.getX(), 0, relative.getZ());
        if (Math.abs(floor.getX()) >= HALF || Math.abs(floor.getZ()) >= HALF) {
            return;
        }
        sand.removeIf(s -> s.pos().equals(floor));
        sand.add(new Sand(floor, permanent));
        set(floor.getX(), 0, floor.getZ(), Blocks.MAGMA_BLOCK.defaultBlockState());
    }

    private void clearSand(boolean all) {
        sand.removeIf(s -> {
            if (all || !s.permanent()) {
                set(s.pos().getX(), 0, s.pos().getZ(), Blocks.SMOOTH_SANDSTONE.defaultBlockState());
                return true;
            }
            return false;
        });
    }

    /** Molten sand tiles (self-test). */
    public int sandTiles() {
        return sand.size();
    }

    /** Molten sand: up to 15 damage every two ticks. */
    private void sandTick() {
        if (sand.isEmpty() || ticks % 2 != 0) {
            return;
        }
        for (ServerPlayer p : online()) {
            BlockPos d = relPos(p);
            for (Sand s : sand) {
                if (s.pos().getX() == d.getX() && s.pos().getZ() == d.getZ()) {
                    CombatHooks.nameAttack("molten_sand", null);
                    hurt(p, 1 + random.nextInt(FortisColosseum.MOLTEN_SAND_DAMAGE));
                    break;
                }
            }
        }
    }

    /** Frailty: no healing above the reduced base Hitpoints. */
    private void frailtyTick() {
        int tier = active.getOrDefault(Modifier.FRAILTY, 0);
        if (tier <= 0) {
            return;
        }
        for (ServerPlayer p : online()) {
            int cap = FortisColosseum.frailtyCap(Players.data(p).skills().level(Skill.HITPOINTS), tier);
            if (p.getHealth() > cap) {
                p.setHealth(Math.max(1, cap));
            }
        }
    }

    // ------------------------------------------------------------------ player combat hooks

    /** Registers the Colosseum's combat hooks and blocks (called once from the module's init). */
    public static void register() {
        // Sol Heredit drops nothing: the Rewards Chest holds the loot (his pet roll and kill count still come with the kill).
        com.runecraft.boss.DataBossService.loot(FortisColosseum.SOL_HEREDIT, (killer, boss, table) -> List.of());
        CombatHooks.addDamageListener((player, attacker, attack, style, damage) -> {
            if (MinigameManager.session(player) instanceof ColosseumSession s) {
                s.playerDamaged(player, attacker, style, damage);
            }
        });
        CombatHooks.addNpcVersusPlayer(new CombatHooks.NpcVersusPlayer() {
            @Override
            public double accuracy(LivingEntity mob, ServerPlayer target, AttackType type) {
                if (MinigameManager.owner(mob) instanceof ColosseumSession s) {
                    return s.relentlessAccuracy(target, type);
                }
                return 1.0;
            }

            @Override
            public int maxHitBonus(LivingEntity mob, ServerPlayer target, AttackType type) {
                if (MinigameManager.owner(mob) instanceof ColosseumSession s) {
                    return FortisColosseum.relentlessMaxHitBonus(s.active.getOrDefault(Modifier.RELENTLESS, 0));
                }
                return 0;
            }
        });
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            private boolean weak(LivingEntity target, AttackType type) {
                String weak = FortisColosseum.warbandWeakness(MinigameManager.creatureId(target));
                return weak != null && weak.equals(styleClass(type)) && MinigameManager.owner(target) instanceof ColosseumSession;
            }

            @Override
            public double accuracy(ServerPlayer player, LivingEntity target, AttackType type) {
                return weak(target, type) ? 100.0 : 1.0;
            }

            @Override
            public boolean alwaysMaxHit(ServerPlayer player, LivingEntity target, AttackType type) {
                // Each warband member always takes a max hit from its weak style (wiki).
                return weak(target, type);
            }
        });
        com.runecraft.content.BlockHooks.register(REWARDS_CHEST, (player, pos, state) -> {
            if (MinigameManager.session(player) instanceof ColosseumSession s) {
                s.claimChest(player);
            } else {
                Msg.game(player, "The Rewards Chest is empty.");
            }
        });
        // The lobby under the Colosseum: the entrance on the surface leads down, the stairs back up, the doorway into the arena.
        com.runecraft.content.BlockHooks.register(Minigame.FORTIS_COLOSSEUM.entranceBlock(), (player, pos, state) -> toLobby(player));
        com.runecraft.content.BlockHooks.register(FortisColosseum.LOBBY_EXIT, (player, pos, state) -> toSurface(player));
        com.runecraft.content.BlockHooks.register(FortisColosseum.ARENA_ENTRANCE, (player, pos, state) -> enterArena(player));
        // Minimus is the zeah module's NPC, whose hooks start after this module's and give every zeah NPC its generic
        // dialogue: his Colosseum handler (briefing, quiver trade, Glory unlocks) is put back once the server has started.
        com.runecraft.platform.ServerLifecycleEvents.SERVER_STARTED.register(server ->
                com.runecraft.quest.QuestScripts.onTalk(FortisColosseum.MINIMUS, (player, npc) -> minimus(player)));
        com.runecraft.quest.QuestScripts.onTalk(com.runecraft.osrs.content.minigames.ColosseumLobby.GLORIA, (player, npc) -> gloria(player));
        com.runecraft.quest.QuestScripts.onTalk(com.runecraft.osrs.content.minigames.ColosseumLobby.UEMAN, (player, npc) ->
                QuestApi.say(player, "Ueman", "You stand in one of the holiest sites in all of Varlamore. Each drop of blood spilt "
                        + "here is a glorious offering to Ralos. It is these offerings that sustain him."));
        com.runecraft.quest.QuestScripts.onTalk(com.runecraft.osrs.content.minigames.ColosseumLobby.SEIA, (player, npc) ->
                QuestApi.say(player, "Seia", "Ranul watches over the fallen. Fight well, and may the moon guide you home."));
        for (String g : new String[]{com.runecraft.osrs.content.minigames.ColosseumLobby.GLADIATOR,
                com.runecraft.osrs.content.minigames.ColosseumLobby.GLADIATOR_2}) {
            com.runecraft.quest.QuestScripts.onTalk(g, (player, npc) ->
                    QuestApi.say(player, "Gladiator", "Sol Heredit is waiting for you up there. Most never make it past the manticores."));
        }
        for (String g : new String[]{com.runecraft.osrs.content.minigames.ColosseumLobby.GUARD,
                com.runecraft.osrs.content.minigames.ColosseumLobby.GUARD_2}) {
            com.runecraft.quest.QuestScripts.onTalk(g, (player, npc) ->
                    QuestApi.say(player, "Guard", "Keep moving. Speak to Minimus if you want to fight."));
        }
    }

    // ------------------------------------------------------------------ the lobby

    /** Where the stairs from the surface come down: 1799, 9506. */
    public static BlockPos lobbyArrival() {
        int[] s = com.runecraft.osrs.content.minigames.ColosseumLobby.stand(FortisColosseum.LOBBY_X, FortisColosseum.LOBBY_Y);
        return new BlockPos(s[0], s[1], s[2]);
    }

    /** Whether a player stands in the lobby (the arena doorway and Minimus' "Enter" only work there). */
    public static boolean inLobby(ServerPlayer p) {
        if (!com.runecraft.world.Gielinor.is(p)) {
            return false;
        }
        BlockPos at = p.blockPosition();
        for (int[] h : com.runecraft.osrs.content.minigames.ColosseumLobby.HALLS) {
            int x1 = com.runecraft.osrs.content.minigames.ColosseumLobby.x(h[0]) - 1;
            int x2 = com.runecraft.osrs.content.minigames.ColosseumLobby.x(h[2]) + 2;
            int z1 = com.runecraft.osrs.content.minigames.ColosseumLobby.z(h[3]) - 1;
            int z2 = com.runecraft.osrs.content.minigames.ColosseumLobby.z(h[1]) + 2;
            int f = com.runecraft.osrs.content.minigames.ColosseumLobby.FLOOR;
            if (at.getX() >= x1 && at.getX() <= x2 && at.getZ() >= z1 && at.getZ() <= z2 && at.getY() > f - 2
                    && at.getY() <= f + com.runecraft.osrs.content.minigames.ColosseumLobby.HEIGHT + 1) {
                return true;
            }
        }
        return false;
    }

    /** The Colosseum entrance on the surface: down the stairs into the lobby (Varlamore needs Children of the Sun). */
    public static boolean toLobby(ServerPlayer p) {
        if (!com.runecraft.world.Gielinor.is(p)) {
            return false;
        }
        if (!QuestApi.isComplete(p, "children_of_the_sun")) {
            Msg.game(p, "You need to complete Children of the Sun to enter the Fortis Colosseum.");
            return false;
        }
        ServerLevel world = (ServerLevel) p.level();
        BlockPos at = lobbyArrival();
        com.runecraft.world.StructurePlacer.applyAround(world, at.getX(), at.getZ(), 72); // carve the lobby if not yet built
        p.teleportTo(world, at.getX() + 0.5, at.getY(), at.getZ() + 1.0, -90f, 0f);
        p.fallDistance = 0;
        Msg.game(p, "You descend into the Fortis Colosseum.");
        return true;
    }

    /**
     * The lobby's stairs back up: out beside the entrance on the surface (OSRS: 1795, 3106 inside the west gate; RuneCraft's
     * entrance stands just outside the gate, so the stairs come up next to it). // approx
     */
    public static boolean toSurface(ServerPlayer p) {
        ServerLevel world = com.runecraft.world.Gielinor.world(p.getServer());
        if (world == null) {
            return false;
        }
        BlockPos at = com.runecraft.world.Gielinor.safeSpot(world, com.runecraft.osrs.GielinorMap.toBlockX(FortisColosseum.ENTRANCE_X),
                com.runecraft.osrs.GielinorMap.toBlockZ(FortisColosseum.ENTRANCE_Y + 1));
        p.teleportTo(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYRot(), 0f);
        p.fallDistance = 0;
        Msg.game(p, "You climb the stairs out of the Colosseum.");
        return true;
    }

    /** The lobby's doorway into the arena (Minimus must have briefed the player first). */
    public static void enterArena(ServerPlayer p) {
        if (!inLobby(p)) {
            Msg.game(p, "You need to be in the Colosseum lobby to enter the arena.");
            return;
        }
        MinigameManager.enter(p, Minigame.FORTIS_COLOSSEUM, Minigame.FORTIS_COLOSSEUM.entranceBlock());
    }

    /** Gloria: Glory and the scoreboard. */
    public static void gloria(ServerPlayer p) {
        int g = MinigameData.of(p).stat(p.getUUID(), FortisColosseum.STAT_GLORY);
        String rank = FortisColosseum.rank(g);
        QuestApi.say(p, "Gloria", "Every wave you clear earns you Glory: more for speed, for taking no damage and for the modifiers "
                + "you dare to take. Only your best run counts. Your best is " + g + " Glory" + (rank != null ? ", " + rank : "")
                + ". The scoreboard behind me keeps the records.");
    }

    /** Relentless: the factor on an enemy's attack roll against this player (see FortisColosseum.relentlessAccuracy). */
    public double relentlessAccuracy(ServerPlayer target, AttackType type) {
        int tier = active.getOrDefault(Modifier.RELENTLESS, 0);
        if (tier <= 0) {
            return 1.0;
        }
        PlayerData d = Players.data(target);
        return FortisColosseum.relentlessAccuracy(tier, d.skills().currentLevel(Skill.DEFENCE), d.skills().currentLevel(Skill.MAGIC),
                type == AttackType.MAGIC);
    }

    private static String styleClass(AttackType type) {
        return type == AttackType.MAGIC ? "magic" : type == AttackType.RANGED ? "ranged" : "melee";
    }

    /** Doom stacks this wave (self-test and HUD). */
    public int doom() {
        return doom;
    }

    /**
     * Damage taken in the arena: any damage counts as taken (the no-damage Glory bonus) and adds a stack of Doom (hazards
     * included: wiki "Molten Sand" increments Doom); only enemies' damage drains prayer under Blasphemy; self-inflicted
     * damage does neither.
     */
    private void playerDamaged(ServerPlayer player, LivingEntity attacker, AttackType style, int damage) {
        if (damage <= 0 || dooming || state != State.FIGHTING || attacker == player) {
            return;
        }
        boolean ours = attacker != null && MinigameManager.owner(attacker) == this;
        if (attacker != null && !ours) {
            return;
        }
        damaged = true;
        PlayerData data = Players.data(player);
        if (ours) {
            if (MinigameManager.creatureId(attacker).equals(FortisColosseum.MANTICORE)) {
                manticoreDamage = true;
            }
            int blasphemy = active.getOrDefault(Modifier.BLASPHEMY, 0);
            if (blasphemy > 0) {
                data.setPrayerPoints(data.prayerPoints() - damage * FortisColosseum.blasphemyPercent(blasphemy) / 100);
            }
        }
        int doomTier = active.getOrDefault(Modifier.DOOM, 0);
        if (doomTier > 0) {
            doom++;
            int limit = FortisColosseum.doomLimit(doomTier);
            Msg.red(player, "Doom: " + doom + " / " + limit);
            if (doom >= limit) {
                Msg.red(player, "The doom upon you is complete.");
                dooming = true;
                try {
                    hurt(player, (int) Math.ceil(player.getHealth()) + 1);
                } finally {
                    dooming = false;
                }
                return;
            }
        }
        if (!ours || echoing || style == null) {
            return;
        }
        if (MinigameManager.creatureId(attacker).equals(FortisColosseum.JAGUAR)) {
            // Jaguar warriors claw three times per attack, each with its own roll.
            extraHit(attacker, player, style, 47, "claw");
            extraHit(attacker, player, style, 47, "claw");
        }
    }

    /**
     * Wave 12: gladiators with shields close the arena down to the square between the four pillars (wiki: "roughly a 16x15
     * arena"); here a sandstone wall ring on the pillars' line. Wall blocks join the sandstone-wall pillars (a fence would leave
     * a gap a player fits through at each pillar), so nobody can stand outside where Sol can't reach. The player is moved
     * inside it.
     */
    private void gladiatorWall() {
        removeWall();
        BlockState fence = Blocks.SANDSTONE_WALL.defaultBlockState();
        int r = PILLAR;
        for (int i = -r + 2; i <= r - 2; i++) {
            for (int[] d : new int[][]{{i, -r}, {i, r}, {-r, i}, {r, i}}) {
                BlockPos at = new BlockPos(d[0], 1, d[1]);
                set(at.getX(), at.getY(), at.getZ(), fence);
                wall.add(at);
            }
        }
        for (ServerPlayer p : online()) {
            BlockPos rp = relPos(p);
            if (Math.abs(rp.getX()) >= r || Math.abs(rp.getZ()) >= r) {
                BlockPos in = rel(0, 1, 4);
                p.teleportTo(world, in.getX() + 0.5, in.getY(), in.getZ() + 0.5, p.getYRot(), p.getXRot());
            }
        }
    }

    private void removeWall() {
        for (BlockPos at : wall) {
            set(at.getX(), at.getY(), at.getZ(), Blocks.AIR.defaultBlockState());
        }
        wall.clear();
    }

    /** Whether the gladiator wall stands (self-test). */
    public int wallBlocks() {
        return wall.size();
    }

    /**
     * Whether every joint along the gladiator wall (ring block to ring block, end block to pillar) is closed: one side's
     * collision box reaches the shared face, so what is left between them is narrower than a player (self-test).
     */
    public boolean wallSealed() {
        if (wall.isEmpty()) {
            return false;
        }
        for (BlockPos at : wall) {
            BlockPos a = rel(at.getX(), at.getY(), at.getZ());
            boolean alongX = Math.abs(at.getZ()) == PILLAR;
            for (Direction d : alongX ? new Direction[]{Direction.EAST, Direction.WEST} : new Direction[]{Direction.SOUTH, Direction.NORTH}) {
                BlockPos b = a.relative(d);
                VoxelShape sa = world.getBlockState(a).getCollisionShape(world, a);
                VoxelShape sb = world.getBlockState(b).getCollisionShape(world, b);
                if (sa.isEmpty() || sb.isEmpty()) {
                    return false;
                }
                boolean positive = d.getStepX() + d.getStepZ() > 0;
                boolean joined = positive
                        ? sa.max(d.getAxis()) >= 1.0 || sb.min(d.getAxis()) <= 0.0
                        : sa.min(d.getAxis()) <= 0.0 || sb.max(d.getAxis()) >= 1.0;
                if (!joined) {
                    return false;
                }
            }
        }
        return true;
    }

    /** A follow-up hit of a multi-hit attack (own accuracy roll, blocked by the matching protection prayer). */
    private void extraHit(LivingEntity mob, ServerPlayer player, AttackType type, int max, String name) {
        MonsterStats st = MonsterStats.of(mob);
        int level = type == AttackType.MAGIC ? st.magic() : type == AttackType.RANGED ? st.ranged() : st.attack();
        long attack = CombatHooks.versusAttackRoll(mob, player, type, CombatHandler.npcAttackRoll(mob, st, type, level));
        long defence = CombatHandler.defenceRoll(player, type);
        int damage = CombatFormulas.roll(new java.util.Random(random.nextLong()), attack, defence,
                max + FortisColosseum.relentlessMaxHitBonus(active.getOrDefault(Modifier.RELENTLESS, 0)));
        if (CombatHandler.isProtected(Players.data(player), type)) {
            damage = 0;
        }
        echoing = true;
        try {
            CombatHooks.nameAttack(name, type);
            CombatHandler.applyDamage(mob, player, damage);
        } finally {
            echoing = false;
        }
    }

    @Override
    protected double damageMultiplier(ServerPlayer player, LivingEntity target, AttackType type) {
        int myopia = active.getOrDefault(Modifier.MYOPIA, 0);
        if (myopia > 0 && (type == AttackType.RANGED || type == AttackType.MAGIC)) {
            // One block per tile in the arena: the distance to the target's nearest tile against the reduced attack range.
            double tiles = Math.sqrt(player.distanceToSqr(target)) - FortisColosseum.size(MinigameManager.creatureId(target)) / 2.0;
            boolean longrange = Players.data(player).style().forRanged() == com.runecraft.osrs.CombatStyle.LONGRANGE;
            int range = FortisColosseum.myopiaRange(FortisColosseum.attackRange(Inv.idOf(player.getMainHandItem()),
                    type == AttackType.MAGIC, longrange), myopia);
            if (tiles > range + 0.5) {
                if (ticks - lastMyopiaMessage > 10) {
                    lastMyopiaMessage = ticks;
                    Msg.action(player, "Myopia: your target is too far away to hit.");
                }
                return 0.0;
            }
        }
        return 1.0;
    }

    // ------------------------------------------------------------------ leaving and the end

    @Override
    protected void onDeath(ServerPlayer player) {
        died = true;
        Msg.red(player, "You have been defeated in the Fortis Colosseum. The loot in the chest is lost.");
        chest.clear();
        super.onDeath(player);
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        if (blockId.equals(REWARDS_CHEST)) {
            claimChest(player);
            return true;
        }
        if (blockId.equals("minigame_exit")) {
            // The exit between waves ends the run, like telling Minimus you're done; with the chest waiting it must be searched.
            if (state == State.CHOOSING && wave >= 1) {
                return cashOut(player);
            }
            if (state == State.REWARDS) {
                Msg.game(player, "Minimus: Don't forget your loot! Search the Rewards Chest first.");
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onLeave(ServerPlayer player) {
        // Dying, logging out, teleporting or leaving without searching the chest forfeits it.
        if (!died && !claimed && !chest.isEmpty()) {
            Msg.red(player, "You leave the arena without claiming the Rewards Chest; its loot is lost.");
            chest.clear();
        }
        PoisonService.cure(player, true, 0, com.runecraft.action.ActionManager.gameTick());
        clearSand(true);
    }

    @Override
    protected void onClose() {
        removeWall();
        orbs.clear();
        javelins.clear();
        com.runecraft.boss.Mechanics.clearHazards(world, box()); // Sol Heredit's molten sand
        for (LivingEntity e : spawned) {
            e.discard();
        }
        spawned.clear();
        for (long c : forcedChunks) {
            world.setChunkForced(net.minecraft.world.level.ChunkPos.getX(c), net.minecraft.world.level.ChunkPos.getZ(c), false);
        }
        forcedChunks.clear();
    }

    @Override
    protected void onEnd(boolean success) {
        if (!success || claimed) {
            return;
        }
        // A run ended with the chest still unsearched (only a self-test or shutdown path): hand it over.
        for (ServerPlayer p : online()) {
            claim(p);
        }
    }

    @Override
    public int caStage() {
        return flagsWave;
    }

    @Override
    public Set<String> caFlags(ServerPlayer player) {
        return new HashSet<>(waveFlags);
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        lines.add(game.displayName());
        if (state == State.REWARDS) {
            lines.add("Search the Rewards Chest");
        } else {
            lines.add("Wave: " + Math.max(1, wave) + " / " + FortisColosseum.WAVES + (state == State.CHOOSING ? " (choose a modifier)" : ""));
        }
        if (state == State.FIGHTING) {
            lines.add("Monsters: " + waveMonsters().size() + "  Time: " + time(ticks - waveStart));
        }
        if (!active.isEmpty()) {
            List<String> mods = new ArrayList<>();
            active.forEach((m, t) -> mods.add(m.label(t)));
            lines.add(String.join(", ", mods));
        }
        if (active.containsKey(Modifier.DOOM)) {
            lines.add("Doom: " + doom + " / " + FortisColosseum.doomLimit(active.get(Modifier.DOOM)));
        }
        lines.add("Glory: " + glory);
        return lines;
    }

    // ------------------------------------------------------------------ outside the arena

    /** The scoreboard in the lobby. */
    public static void scoreboard(ServerPlayer p) {
        MinigameData data = MinigameData.of(p);
        UUID u = p.getUUID();
        int g = data.stat(u, FortisColosseum.STAT_GLORY);
        String rank = FortisColosseum.rank(g);
        int best = data.stat(u, FortisColosseum.STAT_BEST_TIME);
        Msg.game(p, "--- Fortis Colosseum ---");
        Msg.game(p, "Glory: " + g + (rank != null ? " (" + rank + ")" : "") + ". Waves cleared: " + data.stat(u, "kc:fortis_colosseum")
                + ", best wave " + data.stat(u, FortisColosseum.STAT_BEST_WAVE) + ".");
        Msg.game(p, "Completions: " + data.stat(u, FortisColosseum.STAT_COMPLETIONS) + ", best time "
                + (best > 0 ? time(best) : "-") + ". Sol Heredit kills: " + Players.data(p).killCount(FortisColosseum.SOL_HEREDIT) + ".");
    }

    /** Minimus: how the Colosseum works (he must be spoken to once before the arena), the quiver exchange and unlocks. */
    public static void minimus(ServerPlayer p) {
        QuestApi.say(p, "Minimus", "Welcome to the Fortis Colosseum! Twelve waves stand between you and Sol Heredit. Before each wave "
                + "I'll offer you three modifiers; between waves you may leave with the loot you've earned. Die, and it's lost.");
        MinigameData.of(p).setFlag(p.getUUID(), FLAG_BRIEFED);
        if (MinigameManager.session(p) == null) {
            p.displayClientMessage(Component.literal("").withStyle(ChatFormatting.GRAY).append(link("Enter the arena",
                    "/minigame colosseum enter", "Step through the entrance into the arena", ChatFormatting.GOLD)), false);
        }
        int glory = MinigameData.of(p).stat(p.getUUID(), FortisColosseum.STAT_GLORY);
        if (glory >= FortisColosseum.GLORY_RESPAWN) {
            p.displayClientMessage(Component.literal("Challenger: ").withStyle(ChatFormatting.GRAY).append(link("Respawn in Civitas illa Fortis",
                    "/minigame colosseum respawn", "Set your respawn point to Civitas illa Fortis", ChatFormatting.GOLD)), false);
        }
        if (glory >= FortisColosseum.GLORY_SALUTE) {
            p.displayClientMessage(Component.literal("Grand Champion: ").withStyle(ChatFormatting.GRAY).append(link("Fortis Salute",
                    "/minigame colosseum salute", "Perform the Fortis Salute emote", ChatFormatting.GOLD)), false);
        }
        if (Inv.has(p, ColosseumRewards.QUIVER, 1)) {
            MutableComponent line = Component.literal("Trade a Dizana's quiver: ").withStyle(ChatFormatting.GRAY)
                    .append(link(FortisColosseum.QUIVER_SPLINTERS + " sunfire splinters", "/minigame colosseum quiver splinters",
                            "Give Minimus a quiver for 4,000 splinters", ChatFormatting.GOLD));
            if (!Players.data(p).collectionLog().contains(ColosseumRewards.PET)) {
                line.append(Component.literal(" ")).append(link("A chance at Smol heredit", "/minigame colosseum quiver pet",
                        "Give Minimus a quiver for a 1/200 chance at the pet", ChatFormatting.LIGHT_PURPLE));
            }
            p.displayClientMessage(line, false);
        }
    }

    // ------------------------------------------------------------------ Showboating and the Glory unlocks

    /** Sol Heredit while he is alive in this run's arena, or null. */
    public LivingEntity sol() {
        for (LivingEntity e : spawned) {
            if (e.isAlive() && MinigameManager.creatureId(e).equals(FortisColosseum.SOL_HEREDIT)) {
                return e;
            }
        }
        return null;
    }

    /**
     * The Fortis Salute (unlocked at 20,000 Glory): facing north, east, south or west while Sol Heredit is below 10%
     * hitpoints sets that side of the crowd off ("Showboating").
     */
    public static void salute(ServerPlayer p) {
        if (MinigameData.of(p).stat(p.getUUID(), FortisColosseum.STAT_GLORY) < FortisColosseum.GLORY_SALUTE) {
            Msg.game(p, "You need " + FortisColosseum.GLORY_SALUTE + " Glory (Grand Champion) to perform the Fortis Salute.");
            return;
        }
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        if (MinigameManager.session(p) instanceof ColosseumSession s) {
            s.saluteIn(p);
        }
    }

    /** Records a salute towards the side the player faces (self-test entry point). Returns the side, or null. */
    public String saluteIn(ServerPlayer p) {
        LivingEntity sol = sol();
        String side = FortisColosseum.saluteDirection(p.getYRot());
        if (sol == null || side == null
                || sol.getHealth() >= sol.getMaxHealth() * FortisColosseum.SHOWBOATING_BELOW_PERCENT / 100f) {
            return null;
        }
        if (salutes.add(side)) {
            p.displayClientMessage(Component.literal("The crowd to the " + side + " goes wild.").withStyle(ChatFormatting.GREEN), false);
        }
        return side;
    }

    /** Self-test: drops Sol Heredit to a fraction of his hitpoints; false when he isn't in the arena. */
    public boolean selfTestWoundSol(float fraction) {
        LivingEntity sol = sol();
        if (sol == null) {
            return false;
        }
        sol.setHealth(Math.max(1f, sol.getMaxHealth() * fraction));
        return true;
    }

    /** The sides saluted so far this run. */
    public Set<String> salutes() {
        return Set.copyOf(salutes);
    }

    /** Arena-relative block position (self-test). */
    public BlockPos arena(int dx, int dy, int dz) {
        return rel(dx, dy, dz);
    }

    /** The lobby bank chest: usable from 2,000 Glory (Brawler). */
    public static void bankChest(ServerPlayer p) {
        if (MinigameData.of(p).stat(p.getUUID(), FortisColosseum.STAT_GLORY) < FortisColosseum.GLORY_BANK) {
            Msg.game(p, "The chest is reserved for the Colosseum's Brawlers. Earn " + FortisColosseum.GLORY_BANK + " Glory to use it.");
            return;
        }
        com.runecraft.bank.BankService.open(p);
    }

    /** Sets the player's respawn point to Civitas illa Fortis (5,000 Glory, Challenger). */
    public static boolean setRespawn(ServerPlayer p) {
        if (MinigameData.of(p).stat(p.getUUID(), FortisColosseum.STAT_GLORY) < FortisColosseum.GLORY_RESPAWN) {
            Msg.game(p, "Only Challengers (" + FortisColosseum.GLORY_RESPAWN + " Glory) may call Civitas illa Fortis home.");
            return false;
        }
        ServerLevel world = com.runecraft.world.Gielinor.world(p.getServer());
        if (world == null) {
            return false;
        }
        BlockPos at = com.runecraft.world.Gielinor.safeSpot(world, com.runecraft.osrs.GielinorMap.toBlockX(FortisColosseum.RESPAWN_X),
                com.runecraft.osrs.GielinorMap.toBlockZ(FortisColosseum.RESPAWN_Y));
        p.setRespawnPosition(com.runecraft.world.Gielinor.WORLD, at, 0f, true, false);
        Msg.game(p, "You will now respawn in Civitas illa Fortis.");
        return true;
    }

    /** The ring of dueling's Fortis Colosseum teleport needs 12,000 Glory (Hero); null when allowed. */
    public static String duelingGate(ServerPlayer p) {
        return MinigameData.of(p).stat(p.getUUID(), FortisColosseum.STAT_GLORY) >= FortisColosseum.GLORY_DUELING ? null
                : "You need " + FortisColosseum.GLORY_DUELING + " Glory (Hero) to teleport to the Fortis Colosseum.";
    }

    /** Gives Minimus a Dizana's quiver for 4,000 splinters or another 1/200 roll at the pet. */
    public static void tradeQuiver(ServerPlayer p, boolean pet) {
        if (!Inv.has(p, ColosseumRewards.QUIVER, 1)) {
            Msg.game(p, "You don't have a Dizana's quiver to give Minimus.");
            return;
        }
        if (pet && Players.data(p).collectionLog().contains(ColosseumRewards.PET)) {
            Msg.game(p, "Minimus: You already have a Smol heredit following you around.");
            return;
        }
        Inv.remove(p, ColosseumRewards.QUIVER, 1);
        if (!pet) {
            reward(p, ColosseumRewards.SPLINTERS, FortisColosseum.QUIVER_SPLINTERS);
            Msg.game(p, "Minimus takes the quiver and hands you " + FortisColosseum.QUIVER_SPLINTERS + " sunfire splinters.");
        } else if (p.getRandom().nextInt(FortisColosseum.QUIVER_PET_RATE) == 0) {
            reward(p, ColosseumRewards.PET, 1);
            p.getServer().getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString()
                    + " has a funny feeling like they're being followed.").withStyle(ChatFormatting.GOLD), false);
        } else {
            Msg.game(p, "Minimus takes the quiver. Ralos does not smile upon you this time.");
        }
    }
}
