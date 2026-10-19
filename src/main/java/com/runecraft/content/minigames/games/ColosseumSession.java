package com.runecraft.content.minigames.games;

import com.runecraft.osrs.CombatFormulas;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.MonsterStats;
import com.runecraft.combat.PoisonService;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.bosses.ColosseumRewards;
import com.runecraft.osrs.content.minigames.FortisColosseum;
import com.runecraft.osrs.content.minigames.FortisColosseum.Modifier;
import com.runecraft.osrs.content.minigames.FortisColosseum.Offer;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
 * Between waves the player may leave with the chest ({@code /minigame colosseum leave}, or the exit portal); dying,
 * logging out or leaving any other way forfeits it. Wave 12 is Sol Heredit; his death ends the run with Dizana's quiver in the chest.
 *
 * <p>Modifiers modelled: Bees!, Blasphemy, Doom, Dynamic Duo, Frailty, Mantimayhem (manticores fire three orbs a tick
 * apart, magic/ranged then melee; I: each orb hits twice, II: unprotected orbs venom, III: the melee orb anywhere), Myopia (ranged and autocast attacks beyond the weapon's attack range less 2/4/6 tiles do nothing), Reentry,
 * Red Flag (without it minotaurs walk straight at the player and can be safespotted behind a pillar; with it they
 * path-find), Relentless (enemy accuracy as if the player's Defence were 33%/66% lower, or no accuracy roll at III, and
 * +1/+3/+6 max hit), Solarflare, Quartet, Totemic and Volatility. Javelin Colossi throw a prayer-ignoring javelin at the player's tile every fifth attack; minotaurs out of
 * melee reach heal wounded monsters to full.
 */
public class ColosseumSession extends MinigameSession {
    private enum State { CHOOSING, STARTING, FIGHTING, DONE }

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
    // Per-wave tracking
    private boolean damaged;
    private boolean manticoreDamage;
    private boolean minotaurHealed;
    private int doom;
    private final Set<String> waveFlags = new HashSet<>();
    private int flagsWave;
    // Modifier objects
    private final List<LivingEntity> bees = new ArrayList<>();
    private int beeRespawn;
    private final Map<Integer, LivingEntity> totems = new HashMap<>();
    private final Map<Integer, Long> totemCooldown = new HashMap<>();
    private final List<Sand> sand = new ArrayList<>();
    private final Map<Integer, Integer> javelinTimers = new HashMap<>();
    private final List<long[]> javelins = new ArrayList<>();
    /** Manticore orbs still to land: the second and third of each triple. */
    private final List<Orb> orbs = new ArrayList<>();
    /** Blocks of the gladiator wall around Sol Heredit's arena (relative), removed when the fight ends. */
    private final List<BlockPos> wall = new ArrayList<>();
    private int orbIndex;
    private int orbPause;
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

    private static final int HALF = 18;
    private static final int PILLAR = 8;

    public ColosseumSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.FORTIS_COLOSSEUM, id, slot, world, variant);
    }

    /** Children of the Sun opens Varlamore and the Colosseum. */
    public static String requirement(ServerPlayer player) {
        if (!QuestApi.isComplete(player, "children_of_the_sun")) {
            return "You need to complete Children of the Sun to fight in the Fortis Colosseum.";
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

    // ------------------------------------------------------------------ modifier choice and cashing out

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
            line.append(link("Leave with the loot", "/minigame colosseum leave", "End the run and claim the chest", ChatFormatting.GREEN));
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

    /** Ends the run between waves and hands over the chest. */
    public boolean cashOut(ServerPlayer p) {
        if (state != State.CHOOSING || wave < 1) {
            Msg.game(p, "You can only leave with your loot between waves.");
            return false;
        }
        cashedOut = true;
        claim(p);
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
            case DONE -> {
            }
        }
        if (state != State.DONE) {
            sandTick();
            frailtyTick();
        }
        if (solDiedAt != null) {
            // Sol drops nothing in OSRS: the chest holds the loot.
            for (ItemEntity item : world.getEntitiesOfClass(ItemEntity.class, box(), e -> true)) {
                item.discard();
            }
            solDiedAt = null;
            completeWave();
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
        javelinTimers.clear();
        javelins.clear();
        orbs.clear();
        if (wave == FortisColosseum.WAVES) {
            gladiatorWall();
        }
        broadcastRed("Wave: " + wave + (wave == FortisColosseum.WAVES ? " - Sol Heredit leaps into the arena!" : ""));
        List<String> monsters = FortisColosseum.startingMonsters(wave, active, random);
        for (String id : monsters) {
            BlockPos at = id.equals(FortisColosseum.SOL_HEREDIT) ? rel(0, 1, -6) : spawnSpot();
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
        spawned.add(mob);
        aggro(mob);
    }

    private void fightTick() {
        long waveTicks = ticks - waveStart;
        if (!reinforced && wave < FortisColosseum.WAVES && waveTicks >= FortisColosseum.REINFORCEMENT_TICKS) {
            reinforced = true;
            List<ServerPlayer> online = online();
            boolean north = !online.isEmpty() && relPos(online.get(0)).getZ() < 0;
            for (String id : FortisColosseum.wave(wave).reinforcements()) {
                spawnWaveMonster(id, rel(random.nextInt(5) - 2, 1, north ? -HALF + 2 : HALF - 2));
            }
            broadcastRed("Reinforcements arrive from the " + (north ? "north" : "south") + " gate!");
        }
        List<LivingEntity> monsters = waveMonsters();
        if (waveTicks > 2 && monsters.isEmpty() && wave < FortisColosseum.WAVES) {
            completeWave();
            return;
        }
        orbTick();
        javelinTick(monsters);
        minotaurTick(monsters);
        totemTick(monsters);
        beeTick();
        solarflareTick();
    }

    private void completeWave() {
        long waveTicks = ticks - waveStart;
        totalTicks += waveTicks;
        int gained = FortisColosseum.glory(wave, waveTicks, !damaged, active);
        glory += gained;
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
        // Leftover floor items (none drop in OSRS), molten sand that isn't permanent, bees and totems go.
        for (ItemEntity item : world.getEntitiesOfClass(ItemEntity.class, box(), e -> true)) {
            item.discard();
        }
        clearSand(false);
        for (LivingEntity t : totems.values()) {
            t.discard();
        }
        totems.clear();
        totemCooldown.clear();
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
            state = State.DONE;
            end(true);
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
        int volatility = active.getOrDefault(Modifier.VOLATILITY, 0);
        if (volatility > 0 && !creatureId.equals(FortisColosseum.BEE_SWARM) && !creatureId.equals(FortisColosseum.HEALING_TOTEM)
                && !creatureId.equals(FortisColosseum.SOL_HEREDIT)) {
            int size = entity.getBbWidth() > 2.5f ? 3 : entity.getBbWidth() > 1.2f ? 2 : 1;
            int radius = FortisColosseum.volatilityRadius(size, volatility) * 2 + 1;
            BlockPos at = entity.blockPosition();
            world.sendParticles(ParticleTypes.EXPLOSION, entity.getX(), entity.getY() + 1, entity.getZ(), 4, radius / 2.0, 0.5,
                    radius / 2.0, 0);
            for (ServerPlayer p : online()) {
                if (Math.abs(p.getBlockX() - at.getX()) <= radius && Math.abs(p.getBlockZ() - at.getZ()) <= radius) {
                    hurt(p, 1 + random.nextInt(FortisColosseum.VOLATILITY_DAMAGE));
                }
            }
            if (volatility >= 3) {
                addSand(at.subtract(origin), active.getOrDefault(Modifier.REENTRY, 0) >= 2);
            }
        }
        if (creatureId.equals(FortisColosseum.BEE_SWARM)) {
            beeRespawn = FortisColosseum.BEE_RESPAWN_TICKS;
        }
        if (creatureId.equals(FortisColosseum.HEALING_TOTEM)) {
            totems.entrySet().removeIf(e -> {
                if (e.getValue() == entity) {
                    totemCooldown.put(e.getKey(), ticks + FortisColosseum.TOTEM_RESPAWN_TICKS);
                    return true;
                }
                return false;
            });
        }
        if (creatureId.equals(FortisColosseum.SOL_HEREDIT) && state == State.FIGHTING) {
            solDiedAt = entity.blockPosition();
        }
    }

    // ------------------------------------------------------------------ monster mechanics

    /** Every fifth Javelin Colossus attack is a javelin thrown at the player's tile; it ignores prayer. */
    private void javelinTick(List<LivingEntity> monsters) {
        List<ServerPlayer> online = online();
        if (!online.isEmpty()) {
            for (LivingEntity e : monsters) {
                if (!MinigameManager.creatureId(e).equals(FortisColosseum.JAVELIN)) {
                    continue;
                }
                int t = javelinTimers.merge(e.getId(), 1, Integer::sum);
                if (t % (FortisColosseum.JAVELIN_EVERY * 5) == 0) {
                    ServerPlayer target = online.get(random.nextInt(online.size()));
                    BlockPos at = target.blockPosition();
                    javelins.add(new long[]{ticks + FortisColosseum.JAVELIN_FLIGHT_TICKS, at.getX(), at.getY(), at.getZ()});
                    Msg.action(target, "A javelin is launched high into the air!");
                }
            }
        }
        javelins.removeIf(j -> {
            world.sendParticles(ParticleTypes.CRIT, j[1] + 0.5, j[2] + 0.2, j[3] + 0.5, 3, 0.4, 0.1, 0.4, 0);
            if (ticks < j[0]) {
                return false;
            }
            for (ServerPlayer p : online()) {
                if (Math.abs(p.getBlockX() - j[1]) <= 1 && Math.abs(p.getBlockZ() - j[3]) <= 1) {
                    hurt(p, random.nextInt(49)); // approx: up to the colossus's 48
                }
            }
            int reentry = active.getOrDefault(Modifier.REENTRY, 0);
            if (reentry > 0) {
                BlockPos d = new BlockPos((int) j[1], (int) j[2], (int) j[3]).subtract(origin);
                addSand(d, reentry >= 2);
                if (reentry >= 2) {
                    addSand(d.offset(-2, 0, 2), true); // south-west
                }
                if (reentry >= 3) {
                    addSand(d.offset(-2, 0, 0), true); // west
                }
            }
            return true;
        });
    }

    /** A minotaur out of melee reach heals a wounded monster (below 75%) within 7 tiles to full. */
    private void minotaurTick(List<LivingEntity> monsters) {
        if (ticks % 5 != 0) {
            return;
        }
        for (LivingEntity mino : monsters) {
            if (!MinigameManager.creatureId(mino).equals(FortisColosseum.MINOTAUR)) {
                continue;
            }
            // Red Flag gives the minotaurs smart pathing; otherwise they walk straight at the player (safespottable).
            if (mino instanceof com.runecraft.entity.OsrsMonsterEntity m) {
                m.directPathing = !active.containsKey(Modifier.RED_FLAG);
            }
            boolean inReach = online().stream().anyMatch(p -> p.distanceToSqr(mino) < 16);
            if (inReach) {
                continue;
            }
            double range = FortisColosseum.MINOTAUR_HEAL_RANGE * 2;
            for (LivingEntity other : monsters) {
                if (other != mino && !MinigameManager.creatureId(other).equals(FortisColosseum.MINOTAUR)
                        && other.distanceToSqr(mino) <= range * range
                        && other.getHealth() < other.getMaxHealth() * FortisColosseum.MINOTAUR_HEAL_BELOW_PERCENT / 100f) {
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

    /** Totemic: a healing totem rises beside a monster at half health and heals it 30% every few ticks. */
    private void totemTick(List<LivingEntity> monsters) {
        if (!active.containsKey(Modifier.TOTEMIC)) {
            return;
        }
        for (LivingEntity e : monsters) {
            int key = e.getId();
            if (e.getHealth() > e.getMaxHealth() / 2 || totems.containsKey(key) || ticks < totemCooldown.getOrDefault(key, 0L)) {
                continue;
            }
            Mob totem = spawn(FortisColosseum.HEALING_TOTEM, e.blockPosition().offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2));
            if (totem != null) {
                totem.setNoAi(true);
                totems.put(key, totem);
                broadcastRed("A healing totem rises beside the " + e.getName().getString() + "!");
            }
        }
        if (ticks % FortisColosseum.TOTEM_HEAL_TICKS != 0) {
            return;
        }
        for (LivingEntity e : monsters) {
            LivingEntity totem = totems.get(e.getId());
            if (totem != null && totem.isAlive()) {
                if (e instanceof com.runecraft.boss.DataBossEntity sol && MinigameManager.creatureId(e).equals(FortisColosseum.SOL_HEREDIT)) {
                    // Sol Heredit: 75 of his 1,500 hitpoints per heal (Minecraft health is scaled to his true hitpoints).
                    e.heal(com.runecraft.osrs.content.minigames.SolHeredit.TOTEM_HEAL * sol.getMaxHealth() / Math.max(1, sol.trueHitpoints()));
                } else {
                    e.heal(e.getMaxHealth() * FortisColosseum.TOTEM_HEAL_PERCENT / 100f);
                }
                world.sendParticles(ParticleTypes.HAPPY_VILLAGER, e.getX(), e.getY() + e.getBbHeight(), e.getZ(), 5, 0.4, 0.3, 0.4, 0);
            }
        }
    }

    private void spawnBee() {
        Mob bee = spawn(FortisColosseum.BEE_SWARM, randomFloor(0, 0, HALF - 3));
        if (bee != null) {
            bee.setNoAi(true);
            bees.add(bee);
        }
    }

    /** Bees!: the swarms drift a tile towards the player every 12 ticks and sting whoever stands under them. */
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
        for (LivingEntity bee : bees) {
            if (ticks % FortisColosseum.BEE_MOVE_TICKS == 0) {
                double dx = Math.signum(target.getX() - bee.getX()) * 2;
                double dz = Math.signum(target.getZ() - bee.getZ()) * 2;
                bee.moveTo(bee.getX() + dx, bee.getY(), bee.getZ() + dz, bee.getYRot(), 0);
            }
            if (Math.abs(target.getX() - bee.getX()) < 1.5 && Math.abs(target.getZ() - bee.getZ()) < 1.5) {
                hurt(target, random.nextInt(FortisColosseum.BEE_DAMAGE + 1));
                PoisonService.poison(target, 1, com.runecraft.action.ActionManager.gameTick());
            }
        }
    }

    /** Solarflare: an orb circles the pillars, pausing at the corners, and burns (III: and disables prayers) on contact. */
    private void solarflareTick() {
        int tier = active.getOrDefault(Modifier.SOLARFLARE, 0);
        if (tier <= 0) {
            return;
        }
        int side = PILLAR * 2;
        int perimeter = side * 4;
        if (orbPause > 0) {
            orbPause--;
        } else if (ticks % FortisColosseum.solarflareMoveTicks(tier) == 0) {
            orbIndex = (orbIndex + 1) % perimeter;
            if (orbIndex % side == 0) {
                orbPause = FortisColosseum.solarflareCornerPause(tier);
            }
        }
        int leg = orbIndex / side;
        int along = orbIndex % side;
        int ox = switch (leg) {
            case 0 -> -PILLAR + along;
            case 1 -> PILLAR;
            case 2 -> PILLAR - along;
            default -> -PILLAR;
        };
        int oz = switch (leg) {
            case 0 -> -PILLAR;
            case 1 -> -PILLAR + along;
            case 2 -> PILLAR;
            default -> PILLAR - along;
        };
        BlockPos orb = rel(ox, 1, oz);
        world.sendParticles(ParticleTypes.FLAME, orb.getX() + 0.5, orb.getY() + 0.8, orb.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.01);
        for (ServerPlayer p : online()) {
            if (Math.abs(p.getBlockX() - orb.getX()) <= 1 && Math.abs(p.getBlockZ() - orb.getZ()) <= 1 && ticks % 2 == 0) {
                hurt(p, 1 + random.nextInt(FortisColosseum.solarflareDamage(tier)));
                if (tier >= 3) {
                    Players.data(p).activePrayers().clear();
                    Msg.red(p, "The solar flare scorches away your prayers!");
                }
            }
        }
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

    /** Molten sand: up to 15 damage every two ticks. */
    private void sandTick() {
        if (sand.isEmpty() || ticks % 2 != 0) {
            return;
        }
        for (ServerPlayer p : online()) {
            BlockPos d = relPos(p);
            for (Sand s : sand) {
                if (s.pos().getX() == d.getX() && s.pos().getZ() == d.getZ()) {
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

    /** Registers the Colosseum's combat hooks (called once from the module's init). */
    public static void register() {
        CombatHooks.addDamageListener((player, attacker, attack, style, damage) -> {
            if (MinigameManager.session(player) instanceof ColosseumSession s) {
                s.manticoreOrb(player, attacker, attack, style);
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
            @Override
            public double accuracy(ServerPlayer player, LivingEntity target, AttackType type) {
                // Each warband member always takes a max hit from its weak style (approx: always hits).
                String weak = FortisColosseum.warbandWeakness(MinigameManager.creatureId(target));
                return weak != null && weak.equals(styleClass(type)) && MinigameManager.owner(target) instanceof ColosseumSession ? 100.0
                        : 1.0;
            }
        });
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

    private void playerDamaged(ServerPlayer player, LivingEntity attacker, AttackType style, int damage) {
        if (attacker == null || MinigameManager.owner(attacker) != this || damage <= 0) {
            return;
        }
        damaged = true;
        String id = MinigameManager.creatureId(attacker);
        if (id.equals(FortisColosseum.MANTICORE)) {
            manticoreDamage = true;
        }
        PlayerData data = Players.data(player);
        int blasphemy = active.getOrDefault(Modifier.BLASPHEMY, 0);
        if (blasphemy > 0) {
            data.setPrayerPoints(data.prayerPoints() - damage * FortisColosseum.blasphemyPercent(blasphemy) / 100);
        }
        int doomTier = active.getOrDefault(Modifier.DOOM, 0);
        if (doomTier > 0) {
            doom++;
            int limit = FortisColosseum.doomLimit(doomTier);
            Msg.red(player, "Doom: " + doom + " / " + limit);
            if (doom >= limit) {
                Msg.red(player, "The doom upon you is complete.");
                hurt(player, (int) Math.ceil(player.getHealth()) + 1);
                return;
            }
        }
        if (echoing || style == null) {
            return;
        }
        if (id.equals(FortisColosseum.JAGUAR)) {
            // Jaguar warriors claw three times per attack.
            extraHit(attacker, player, style, 47);
            extraHit(attacker, player, style, 47);
        }
    }

    /**
     * A manticore's own attack is the first orb of its triple (hits and misses alike): Mantimayhem I adds its second hit,
     * II venoms an unprotected orb, and the other two orbs of the pattern are queued a tick apart.
     */
    private void manticoreOrb(ServerPlayer player, LivingEntity attacker, String attack, AttackType style) {
        if (echoing || attacker == null || style == null || attack == null || !attack.endsWith("_orb")
                || MinigameManager.owner(attacker) != this || !MinigameManager.creatureId(attacker).equals(FortisColosseum.MANTICORE)) {
            return;
        }
        int tier = active.getOrDefault(Modifier.MANTIMAYHEM, 0);
        List<AttackType> pattern = FortisColosseum.manticorePattern(tier, style, random);
        int hits = FortisColosseum.manticoreHitsPerOrb(tier);
        mayhemVenom(player, style, tier);
        for (int i = 1; i < hits; i++) {
            extraHit(attacker, player, style, FortisColosseum.manticoreOrbMaxHit(style));
        }
        for (int i = 1; i < pattern.size(); i++) {
            orbs.add(new Orb(attacker, player, pattern.get(i), ticks + i, hits));
        }
    }

    private void mayhemVenom(ServerPlayer player, AttackType style, int tier) {
        if (tier >= 2 && !CombatHandler.isProtected(Players.data(player), style)) {
            PoisonService.venom(player, com.runecraft.action.ActionManager.gameTick());
        }
    }

    private void orbTick() {
        int tier = active.getOrDefault(Modifier.MANTIMAYHEM, 0);
        for (java.util.Iterator<Orb> it = orbs.iterator(); it.hasNext(); ) {
            Orb o = it.next();
            if (ticks < o.due()) {
                continue;
            }
            it.remove();
            if (!o.mob().isAlive() || !o.player().isAlive() || MinigameManager.session(o.player()) != this) {
                continue;
            }
            world.sendParticles(o.type() == AttackType.MAGIC ? ParticleTypes.WITCH : ParticleTypes.CRIT,
                    o.player().getX(), o.player().getY(0.6), o.player().getZ(), 6, 0.3, 0.3, 0.3, 0.0);
            for (int i = 0; i < o.hits(); i++) {
                extraHit(o.mob(), o.player(), o.type(), FortisColosseum.manticoreOrbMaxHit(o.type()));
            }
            mayhemVenom(o.player(), o.type(), tier);
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
    private void extraHit(LivingEntity mob, ServerPlayer player, AttackType type, int max) {
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
            CombatHooks.nameAttack(type.name().toLowerCase(Locale.ROOT), type);
            CombatHandler.applyDamage(mob, player, damage);
        } finally {
            echoing = false;
        }
    }

    @Override
    protected double damageMultiplier(ServerPlayer player, LivingEntity target, AttackType type) {
        int myopia = active.getOrDefault(Modifier.MYOPIA, 0);
        if (myopia > 0 && (type == AttackType.RANGED || type == AttackType.MAGIC)) {
            double tiles = Math.sqrt(player.distanceToSqr(target)) / 2;
            boolean longrange = Players.data(player).style().forRanged() == com.runecraft.osrs.CombatStyle.LONGRANGE;
            int range = FortisColosseum.myopiaRange(FortisColosseum.attackRange(Inv.idOf(player.getMainHandItem()),
                    type == AttackType.MAGIC, longrange), myopia);
            if (tiles > range + 0.75) { // + the target's half width
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
        // The exit portal between waves ends the run with the chest, like telling Minimus you're done.
        if (blockId.equals("minigame_exit") && state == State.CHOOSING && wave >= 1) {
            return cashOut(player);
        }
        return false;
    }

    @Override
    protected void onLeave(ServerPlayer player) {
        // Dying, logging out, teleporting or leaving mid-wave forfeits the chest (only cashing out claims it).
        if (!died && !cashedOut && !chest.isEmpty() && state != State.DONE) {
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
        if (!success) {
            return;
        }
        for (ServerPlayer p : online()) {
            MinigameData data = MinigameData.of(p);
            UUID u = p.getUUID();
            if (wave >= FortisColosseum.WAVES && flagsWave == FortisColosseum.WAVES) {
                int n = data.incrementStat(u, FortisColosseum.STAT_COMPLETIONS);
                Msg.game(p, "Ralos has smiled upon you! Fortis Colosseum completions: " + n + ".");
                int best = data.stat(u, FortisColosseum.STAT_BEST_TIME);
                if (best == 0 || totalTicks < best) {
                    data.setStat(u, FortisColosseum.STAT_BEST_TIME, (int) totalTicks);
                    Msg.game(p, "New personal best: " + time(totalTicks) + ".");
                }
            }
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
        lines.add("Wave: " + Math.max(1, wave) + " / " + FortisColosseum.WAVES + (state == State.CHOOSING ? " (choose a modifier)" : ""));
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

    /** Minimus: how the Colosseum works and the quiver exchange. */
    public static void minimus(ServerPlayer p) {
        QuestApi.say(p, "Minimus", "Welcome to the Fortis Colosseum! Twelve waves stand between you and Sol Heredit. Before each wave "
                + "I'll offer you three modifiers; between waves you may leave with the loot you've earned. Die, and it's lost.");
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
    private LivingEntity sol() {
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
