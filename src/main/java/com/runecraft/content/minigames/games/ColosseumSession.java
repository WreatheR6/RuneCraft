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
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Fortis Colosseum (see {@link FortisColosseum}): before every wave Minimus offers three modifiers (chat links, or
 * {@code /minigame colosseum pick N}); the wave's monsters then spawn at random spots, reinforcements arrive from the gate
 * nearest the player after 40 seconds, and clearing the wave adds a chest roll ({@link ColosseumRewards}) and Glory.
 * Between waves the player may leave with the chest ({@code /minigame colosseum leave}, or the exit portal); dying,
 * logging out or leaving any other way forfeits it. Wave 12 is Sol Heredit; his death ends the run with Dizana's quiver in the chest.
 *
 * <p>Modifiers modelled: Bees!, Blasphemy, Doom, Dynamic Duo, Frailty, Mantimayhem I-II (III: approx, the orbs already come
 * in any order), Myopia (approx: ranged and magic attacks beyond the reduced range miss), Reentry, Red Flag (approx: our
 * minotaurs always path-find), Relentless (approx: extra max hit on landed hits only), Solarflare, Quartet, Totemic and
 * Volatility. Javelin Colossi throw a prayer-ignoring javelin at the player's tile every fifth attack; minotaurs out of
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
    private int orbIndex;
    private int orbPause;
    private boolean echoing;
    private long lastMyopiaMessage;
    private BlockPos solDiedAt;

    private record Sand(BlockPos pos, boolean permanent) {
    }

    private static final int HALF = 18;
    private static final int PILLAR = 8;

    public ColosseumSession(int id, int slot, ServerWorld world, String variant) {
        super(Minigame.FORTIS_COLOSSEUM, id, slot, world, variant);
    }

    /** Children of the Sun opens Varlamore and the Colosseum. */
    public static String requirement(ServerPlayerEntity player) {
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
        room(Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.CHISELED_SANDSTONE);
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        for (int y = 1; y <= 6; y++) {
                            set(sx * PILLAR + dx, y, sz * PILLAR + dz, (y == 6 ? Blocks.GOLD_BLOCK : Blocks.SANDSTONE_WALL)
                                    .getDefaultState());
                        }
                    }
                }
            }
        }
        // The north and south gates the reinforcements come through.
        for (int dx = -2; dx <= 2; dx++) {
            set(dx, 0, -HALF, Blocks.RED_SANDSTONE.getDefaultState());
            set(dx, 0, HALF, Blocks.RED_SANDSTONE.getDefaultState());
        }
        place(4, HALF - 2, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayerEntity player, int index) {
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
        for (ServerPlayerEntity p : online()) {
            showOffers(p);
        }
    }

    private void showOffers(ServerPlayerEntity p) {
        Msg.game(p, "Minimus: Choose a modifier for wave " + (wave + 1) + ".");
        MutableText line = Text.literal("").formatted(Formatting.GRAY);
        for (int i = 0; i < offers.size(); i++) {
            Offer o = offers.get(i);
            line.append(link(o.label() + " (+" + o.modifier().glory(o.tier()) + " Glory/wave)", "/minigame colosseum pick " + (i + 1),
                    "Pick " + o.label(), Formatting.GOLD)).append(Text.literal(" "));
        }
        if (wave >= 1) {
            line.append(link("Leave with the loot", "/minigame colosseum leave", "End the run and claim the chest", Formatting.GREEN));
        }
        p.sendMessage(line, false);
    }

    private static MutableText link(String label, String command, String hover, Formatting color) {
        return Text.literal("[" + label + "]").styled(s -> s.withColor(color)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hover))));
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
    public boolean pick(ServerPlayerEntity p, int index) {
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
    public boolean cashOut(ServerPlayerEntity p) {
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
    public void claim(ServerPlayerEntity p) {
        if (chest.isEmpty()) {
            return;
        }
        Msg.game(p, "You search the Rewards Chest.");
        for (ColosseumRewards.Reward r : chest) {
            reward(p, r.item(), r.count());
            if (ColosseumRewards.unique(r.item())) {
                p.getServer().getPlayerManager().broadcast(Text.literal(p.getName().getString() + " received a drop: "
                        + displayName(r.item()) + " from the Fortis Colosseum.").formatted(Formatting.GOLD), false);
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
                    for (ServerPlayerEntity p : online()) {
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
            for (ItemEntity item : world.getEntitiesByClass(ItemEntity.class, box(), e -> true)) {
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
        broadcastRed("Wave: " + wave + (wave == FortisColosseum.WAVES ? " - Sol Heredit leaps into the arena!" : ""));
        List<String> monsters = FortisColosseum.startingMonsters(wave, active, random);
        for (String id : monsters) {
            BlockPos at = id.equals(FortisColosseum.SOL_HEREDIT) ? rel(0, 1, -6) : spawnSpot();
            MobEntity mob = spawn(id, at);
            if (mob != null) {
                aggro(mob);
            }
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
        List<LivingEntity> list = new ArrayList<>();
        for (LivingEntity e : living(null)) {
            String id = MinigameManager.creatureId(e);
            if (!id.equals(FortisColosseum.BEE_SWARM) && !id.equals(FortisColosseum.HEALING_TOTEM)) {
                list.add(e);
            }
        }
        return list;
    }

    private void fightTick() {
        long waveTicks = ticks - waveStart;
        if (!reinforced && wave < FortisColosseum.WAVES && waveTicks >= FortisColosseum.REINFORCEMENT_TICKS) {
            reinforced = true;
            List<ServerPlayerEntity> online = online();
            boolean north = !online.isEmpty() && relPos(online.get(0)).getZ() < 0;
            for (String id : FortisColosseum.wave(wave).reinforcements()) {
                MobEntity mob = spawn(id, rel(random.nextInt(5) - 2, 1, north ? -HALF + 2 : HALF - 2));
                if (mob != null) {
                    aggro(mob);
                }
            }
            broadcastRed("Reinforcements arrive from the " + (north ? "north" : "south") + " gate!");
        }
        List<LivingEntity> monsters = waveMonsters();
        if (waveTicks > 2 && monsters.isEmpty() && wave < FortisColosseum.WAVES) {
            completeWave();
            return;
        }
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
        }
        // Leftover floor items (none drop in OSRS), molten sand that isn't permanent, bees and totems go.
        for (ItemEntity item : world.getEntitiesByClass(ItemEntity.class, box(), e -> true)) {
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
        for (ServerPlayerEntity p : online()) {
            owned.addAll(Players.data(p).collectionLog());
        }
        for (ColosseumRewards.Reward r : ColosseumRewards.roll(wave, owned, random)) {
            ColosseumRewards.add(chest, r);
        }
        for (ServerPlayerEntity p : online()) {
            completeWaveFor(p, waveTicks, gained);
        }
        if (wave >= FortisColosseum.WAVES) {
            state = State.DONE;
            end(true);
        } else {
            offer();
        }
    }

    private void completeWaveFor(ServerPlayerEntity p, long waveTicks, int gained) {
        UUID u = p.getUuid();
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
    }

    private String describeChest() {
        List<String> parts = new ArrayList<>();
        for (ColosseumRewards.Reward r : chest) {
            parts.add(r.count() + " x " + displayName(r.item()));
        }
        return parts.isEmpty() ? "nothing" : String.join(", ", parts);
    }

    @Override
    protected void onKill(ServerPlayerEntity player, LivingEntity entity, String creatureId) {
        int volatility = active.getOrDefault(Modifier.VOLATILITY, 0);
        if (volatility > 0 && !creatureId.equals(FortisColosseum.BEE_SWARM) && !creatureId.equals(FortisColosseum.HEALING_TOTEM)
                && !creatureId.equals(FortisColosseum.SOL_HEREDIT)) {
            int size = entity.getWidth() > 2.5f ? 3 : entity.getWidth() > 1.2f ? 2 : 1;
            int radius = FortisColosseum.volatilityRadius(size, volatility) * 2 + 1;
            BlockPos at = entity.getBlockPos();
            world.spawnParticles(ParticleTypes.EXPLOSION, entity.getX(), entity.getY() + 1, entity.getZ(), 4, radius / 2.0, 0.5,
                    radius / 2.0, 0);
            for (ServerPlayerEntity p : online()) {
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
            solDiedAt = entity.getBlockPos();
        }
    }

    // ------------------------------------------------------------------ monster mechanics

    /** Every fifth Javelin Colossus attack is a javelin thrown at the player's tile; it ignores prayer. */
    private void javelinTick(List<LivingEntity> monsters) {
        List<ServerPlayerEntity> online = online();
        if (!online.isEmpty()) {
            for (LivingEntity e : monsters) {
                if (!MinigameManager.creatureId(e).equals(FortisColosseum.JAVELIN)) {
                    continue;
                }
                int t = javelinTimers.merge(e.getId(), 1, Integer::sum);
                if (t % (FortisColosseum.JAVELIN_EVERY * 5) == 0) {
                    ServerPlayerEntity target = online.get(random.nextInt(online.size()));
                    BlockPos at = target.getBlockPos();
                    javelins.add(new long[]{ticks + FortisColosseum.JAVELIN_FLIGHT_TICKS, at.getX(), at.getY(), at.getZ()});
                    Msg.action(target, "A javelin is launched high into the air!");
                }
            }
        }
        javelins.removeIf(j -> {
            world.spawnParticles(ParticleTypes.CRIT, j[1] + 0.5, j[2] + 0.2, j[3] + 0.5, 3, 0.4, 0.1, 0.4, 0);
            if (ticks < j[0]) {
                return false;
            }
            for (ServerPlayerEntity p : online()) {
                if (Math.abs(p.getBlockX() - j[1]) <= 1 && Math.abs(p.getBlockZ() - j[3]) <= 1) {
                    hurt(p, random.nextInt(49)); // approx: up to the colossus's 48
                }
            }
            int reentry = active.getOrDefault(Modifier.REENTRY, 0);
            if (reentry > 0) {
                BlockPos d = new BlockPos((int) j[1], (int) j[2], (int) j[3]).subtract(origin);
                addSand(d, reentry >= 2);
                if (reentry >= 2) {
                    addSand(d.add(-2, 0, 2), true); // south-west
                }
                if (reentry >= 3) {
                    addSand(d.add(-2, 0, 0), true); // west
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
            boolean inReach = online().stream().anyMatch(p -> p.squaredDistanceTo(mino) < 16);
            if (inReach) {
                continue;
            }
            double range = FortisColosseum.MINOTAUR_HEAL_RANGE * 2;
            for (LivingEntity other : monsters) {
                if (other != mino && !MinigameManager.creatureId(other).equals(FortisColosseum.MINOTAUR)
                        && other.squaredDistanceTo(mino) <= range * range
                        && other.getHealth() < other.getMaxHealth() * FortisColosseum.MINOTAUR_HEAL_BELOW_PERCENT / 100f) {
                    other.setHealth(other.getMaxHealth());
                    minotaurHealed = true;
                    world.spawnParticles(ParticleTypes.HEART, other.getX(), other.getY() + other.getHeight(), other.getZ(), 4, 0.4, 0.3,
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
            MobEntity totem = spawn(FortisColosseum.HEALING_TOTEM, e.getBlockPos().add(random.nextInt(5) - 2, 0, random.nextInt(5) - 2));
            if (totem != null) {
                totem.setAiDisabled(true);
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
                e.heal(e.getMaxHealth() * FortisColosseum.TOTEM_HEAL_PERCENT / 100f);
                world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, e.getX(), e.getY() + e.getHeight(), e.getZ(), 5, 0.4, 0.3, 0.4, 0);
            }
        }
    }

    private void spawnBee() {
        MobEntity bee = spawn(FortisColosseum.BEE_SWARM, randomFloor(0, 0, HALF - 3));
        if (bee != null) {
            bee.setAiDisabled(true);
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
        List<ServerPlayerEntity> online = online();
        if (online.isEmpty()) {
            return;
        }
        ServerPlayerEntity target = online.get(0);
        for (LivingEntity bee : bees) {
            if (ticks % FortisColosseum.BEE_MOVE_TICKS == 0) {
                double dx = Math.signum(target.getX() - bee.getX()) * 2;
                double dz = Math.signum(target.getZ() - bee.getZ()) * 2;
                bee.refreshPositionAndAngles(bee.getX() + dx, bee.getY(), bee.getZ() + dz, bee.getYaw(), 0);
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
        world.spawnParticles(ParticleTypes.FLAME, orb.getX() + 0.5, orb.getY() + 0.8, orb.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.01);
        for (ServerPlayerEntity p : online()) {
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
        set(floor.getX(), 0, floor.getZ(), Blocks.MAGMA_BLOCK.getDefaultState());
    }

    private void clearSand(boolean all) {
        sand.removeIf(s -> {
            if (all || !s.permanent()) {
                set(s.pos().getX(), 0, s.pos().getZ(), Blocks.SMOOTH_SANDSTONE.getDefaultState());
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
        for (ServerPlayerEntity p : online()) {
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
        for (ServerPlayerEntity p : online()) {
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
                s.playerDamaged(player, attacker, style, damage);
            }
        });
        CombatHooks.addIncomingModifier((player, attacker, damage) -> {
            if (MinigameManager.session(player) instanceof ColosseumSession s && damage > 0) {
                int bonus = FortisColosseum.relentlessMaxHitBonus(s.active.getOrDefault(Modifier.RELENTLESS, 0));
                if (bonus > 0) {
                    return damage + s.random.nextInt(bonus + 1); // approx: a higher max hit on hits that land
                }
            }
            return damage;
        });
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            @Override
            public double accuracy(ServerPlayerEntity player, LivingEntity target, AttackType type) {
                // Each warband member always takes a max hit from its weak style (approx: always hits).
                String weak = FortisColosseum.warbandWeakness(MinigameManager.creatureId(target));
                return weak != null && weak.equals(styleClass(type)) && MinigameManager.owner(target) instanceof ColosseumSession ? 100.0
                        : 1.0;
            }
        });
    }

    private static String styleClass(AttackType type) {
        return type == AttackType.MAGIC ? "magic" : type == AttackType.RANGED ? "ranged" : "melee";
    }

    private void playerDamaged(ServerPlayerEntity player, LivingEntity attacker, AttackType style, int damage) {
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
        int mayhem = active.getOrDefault(Modifier.MANTIMAYHEM, 0);
        if (id.equals(FortisColosseum.MANTICORE)) {
            if (mayhem >= 2 && !CombatHandler.isProtected(data, style)) {
                PoisonService.venom(player, com.runecraft.action.ActionManager.gameTick());
            }
            if (mayhem >= 1) {
                extraHit(attacker, player, style, style == AttackType.RANGED ? 36 : 31);
            }
        } else if (id.equals(FortisColosseum.JAGUAR)) {
            // Jaguar warriors claw three times per attack.
            extraHit(attacker, player, style, 47);
            extraHit(attacker, player, style, 47);
        }
    }

    /** A follow-up hit of a multi-hit attack (own accuracy roll, blocked by the matching protection prayer). */
    private void extraHit(LivingEntity mob, ServerPlayerEntity player, AttackType type, int max) {
        MonsterStats st = MonsterStats.of(mob);
        int level = type == AttackType.MAGIC ? st.magic() : type == AttackType.RANGED ? st.ranged() : st.attack();
        long attack = CombatHandler.npcAttackRoll(mob, st, type, level);
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
    protected double damageMultiplier(ServerPlayerEntity player, LivingEntity target, AttackType type) {
        int myopia = active.getOrDefault(Modifier.MYOPIA, 0);
        if (myopia > 0 && (type == AttackType.RANGED || type == AttackType.MAGIC)) {
            double tiles = Math.sqrt(player.squaredDistanceTo(target)) / 2;
            int range = Math.max(1, 10 - FortisColosseum.myopiaReduction(myopia)); // approx: a 10-tile weapon range
            if (tiles > range) {
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
    protected void onDeath(ServerPlayerEntity player) {
        died = true;
        Msg.red(player, "You have been defeated in the Fortis Colosseum. The loot in the chest is lost.");
        chest.clear();
        super.onDeath(player);
    }

    @Override
    protected boolean onBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
        // The exit portal between waves ends the run with the chest, like telling Minimus you're done.
        if (blockId.equals("minigame_exit") && state == State.CHOOSING && wave >= 1) {
            return cashOut(player);
        }
        return false;
    }

    @Override
    protected void onLeave(ServerPlayerEntity player) {
        // Dying, logging out, teleporting or leaving mid-wave forfeits the chest (only cashing out claims it).
        if (!died && !cashedOut && !chest.isEmpty() && state != State.DONE) {
            Msg.red(player, "You leave the arena without claiming the Rewards Chest; its loot is lost.");
            chest.clear();
        }
        PoisonService.cure(player, true, 0, com.runecraft.action.ActionManager.gameTick());
        clearSand(true);
    }

    @Override
    protected void onEnd(boolean success) {
        if (!success) {
            return;
        }
        for (ServerPlayerEntity p : online()) {
            MinigameData data = MinigameData.of(p);
            UUID u = p.getUuid();
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
    public Set<String> caFlags(ServerPlayerEntity player) {
        return new HashSet<>(waveFlags);
    }

    @Override
    protected List<String> hud(ServerPlayerEntity player) {
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
    public static void scoreboard(ServerPlayerEntity p) {
        MinigameData data = MinigameData.of(p);
        UUID u = p.getUuid();
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
    public static void minimus(ServerPlayerEntity p) {
        QuestApi.say(p, "Minimus", "Welcome to the Fortis Colosseum! Twelve waves stand between you and Sol Heredit. Before each wave "
                + "I'll offer you three modifiers; between waves you may leave with the loot you've earned. Die, and it's lost.");
        if (Inv.has(p, ColosseumRewards.QUIVER, 1)) {
            MutableText line = Text.literal("Trade a Dizana's quiver: ").formatted(Formatting.GRAY)
                    .append(link(FortisColosseum.QUIVER_SPLINTERS + " sunfire splinters", "/minigame colosseum quiver splinters",
                            "Give Minimus a quiver for 4,000 splinters", Formatting.GOLD));
            if (!Players.data(p).collectionLog().contains(ColosseumRewards.PET)) {
                line.append(Text.literal(" ")).append(link("A chance at Smol heredit", "/minigame colosseum quiver pet",
                        "Give Minimus a quiver for a 1/200 chance at the pet", Formatting.LIGHT_PURPLE));
            }
            p.sendMessage(line, false);
        }
    }

    /** Gives Minimus a Dizana's quiver for 4,000 splinters or another 1/200 roll at the pet. */
    public static void tradeQuiver(ServerPlayerEntity p, boolean pet) {
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
            p.getServer().getPlayerManager().broadcast(Text.literal(p.getName().getString()
                    + " has a funny feeling like they're being followed.").formatted(Formatting.GOLD), false);
        } else {
            Msg.game(p, "Minimus takes the quiver. Ralos does not smile upon you this time.");
        }
    }
}
