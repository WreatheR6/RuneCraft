package com.runecraft.content.minigames.games;

import com.runecraft.action.ActionManager;
import com.runecraft.collection.CollectionLog;
import com.runecraft.content.minigames.Arena;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameReward;
import com.runecraft.osrs.content.minigames.MinigameRules.Tempoross;
import com.runecraft.osrs.content.minigames.TemporossRewards;
import com.runecraft.osrs.content.minigames.TemporossStorm;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;

/**
 * Tempoross: harpoon harpoonfish, cook them at the shrine, and load them into the cannons to drain
 * Tempoross's energy. At 0% energy the spirit pool opens; harpoon it to drain his essence before
 * he recovers. His attacks deal no damage (OSRS Wiki): tether to a totem pole with a rope when a colossal
 * wave comes or lose fish and tools, repair totems the wave breaks with a hammer, and douse the fires his
 * lightning lights with buckets of water (filled at the pump) before they spread and burn your supplies, and step away
 * from a cannon a torrent surges towards (it knocks the cannon out and stuns whoever is loading it). Repairing a totem
 * gives 4 x Construction level XP; energy and essence are sized by the crew ({@link TemporossStorm}).
 * Subduing him with 2,000+ points earns reward permits for the reward pool in the Ruins of Unkah.
 */
public class TemporossSession extends MinigameSession {
    private static final Random POOL_RANDOM = new Random();
    private static final String BUCKET = Tempoross.BUCKET;
    private static final String WATER_BUCKET = Tempoross.WATER_BUCKET;
    private final Map<UUID, Integer> points = new HashMap<>();
    private final Map<UUID, Long> tethered = new HashMap<>();
    /** Combat Achievement conditions per player: cannons loaded (by x position), wave hits, permits earned. */
    private final Map<UUID, java.util.Set<String>> caPlayerFlags = new HashMap<>();
    private final Map<UUID, java.util.Set<Integer>> cannonsLoaded = new HashMap<>();
    /** Burning fires and the tick each was lit (it spreads every {@code FIRE_SPREAD_TICKS}). */
    private final Map<BlockPos, Long> fires = new HashMap<>();
    private final Map<UUID, Integer> doused = new HashMap<>();
    /** Totem poles (arena x offsets) broken by a wave until someone repairs them. */
    private final java.util.Set<Integer> brokenTotems = new java.util.HashSet<>();
    private static final int[] TOTEM_X = {-12, 12};
    private static final int TOTEM_Z = 14;
    /** Energy and essence in hitsplat units, sized by the crew at the start (OSRS Wiki; {@link TemporossStorm}). */
    private int maxEnergy = 1;
    private int energy = 1;
    private int maxEssence = 1;
    private int essence = 1;
    private long vulnerableUntil = -1;
    private long waveAt = -1;
    /** Torrent attack: when the next one surges, at which cannon (x offset), and cannons disabled until a tick. */
    private long torrentAt = -1;
    private long torrentLands = -1;
    private int torrentCannon;
    private final Map<Integer, Long> cannonDownUntil = new HashMap<>();
    private static final int[] CANNON_X = {-6, 6};
    private static final int CANNON_Z = -10;
    /** How often each fire has spread (at most twice). */
    private final Map<BlockPos, Integer> fireSpreads = new HashMap<>();

    public TemporossSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.TEMPOROSS, id, slot, world, variant);
    }

    @Override
    protected int height() {
        return 14;
    }

    @Override
    protected void build() {
        room(Blocks.PRISMARINE_BRICKS, Blocks.DARK_PRISMARINE, Blocks.SEA_LANTERN);
        // The sea in front of Tempoross.
        for (int x = -20; x <= 20; x++) {
            for (int z = -22; z <= -14; z++) {
                set(x, 0, z, Blocks.WATER.defaultBlockState());
            }
        }
        for (int[] spot : new int[][]{{-12, 6}, {12, 6}, {-18, -4}, {18, -4}}) {
            place(spot[0], spot[1], "tempoross_fishing_spot");
        }
        for (int x : CANNON_X) {
            place(x, CANNON_Z, "tempoross_cannon");
        }
        place(0, 4, "tempoross_shrine");
        for (int x : TOTEM_X) {
            place(x, TOTEM_Z, "tempoross_totem");
        }
        place(-3, 10, "tempoross_supplies");
        place(3, 10, "tempoross_water_pump");
        place(0, 20, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return rel(index % 5 - 2, 1, 16);
    }

    @Override
    protected void start() {
        Mob t = spawn("tempoross", rel(0, 1, -18));
        if (t != null) {
            t.setNoAi(true);
        }
        for (ServerPlayer p : online()) {
            if (!Inv.has(p, "harpoon", 1) && !Inv.has(p, "dragon_harpoon", 1)) {
                Inv.give(p, "harpoon", 1);
                Msg.game(p, "First Mate Deri hands you a harpoon.");
            }
        }
        int crew = Math.max(1, online().size());
        maxEnergy = energy = TemporossStorm.energy(crew);
        maxEssence = essence = TemporossStorm.essence(crew);
        waveAt = ticks + Tempoross.WAVE_INTERVAL;
        torrentAt = ticks + TemporossStorm.TORRENT_INTERVAL;
        broadcastRed("Tempoross rises from the sea!");
        broadcast("Ropes, hammers and buckets are in the supply crate; fill buckets at the water pump.");
    }

    private void addPoints(ServerPlayer p, int amount) {
        points.merge(p.getUUID(), amount, Integer::sum);
    }

    private boolean vulnerable() {
        return vulnerableUntil >= ticks;
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "tempoross_fishing_spot" -> {
                if (!Inv.has(player, "harpoon", 1) && !Inv.has(player, "dragon_harpoon", 1)) {
                    Msg.game(player, "You need a harpoon to fish here.");
                    return true;
                }
                Msg.action(player, "You start harpooning.");
                int[] wait = {TemporossStorm.CATCH_TICKS};
                repeat(player, p -> {
                    if (!Inv.hasRoomFor(p, "raw_harpoonfish")) {
                        Msg.game(p, "You can't carry any more fish.");
                        return false;
                    }
                    if (--wait[0] > 0) {
                        return true;
                    }
                    wait[0] = TemporossStorm.CATCH_TICKS;
                    int fishing = level(p, Skill.FISHING);
                    // 170/256 at level 1 to 255/256 at 99 (OSRS Wiki); approx: the dragon harpoon's 20% better roll.
                    double chance = com.runecraft.osrs.data.Fish.catchChance(fishing, TemporossStorm.CATCH_LOW,
                            TemporossStorm.CATCH_HIGH, Inv.has(p, "dragon_harpoon", 1) ? 1.2 : 1.0);
                    if (random.nextDouble() < chance) {
                        int n = Inv.remove(p, "spirit_flakes", 1) > 0 && random.nextBoolean() ? 2 : 1;
                        Inv.give(p, "raw_harpoonfish", n);
                        addPoints(p, Tempoross.POINTS_CATCH * n);
                        xp(p, Skill.FISHING, Tempoross.catchXp(fishing) * n);
                    }
                    return true;
                });
                return true;
            }
            case "tempoross_shrine" -> {
                int[] wait = {TemporossStorm.COOK_TICKS};
                repeat(player, p -> {
                    if (!Inv.has(p, "raw_harpoonfish", 1)) {
                        return false;
                    }
                    if (--wait[0] > 0) {
                        return true; // 3 ticks a fish; harpoonfish never burn
                    }
                    wait[0] = TemporossStorm.COOK_TICKS;
                    if (Inv.remove(p, "raw_harpoonfish", 1) == 0) {
                        return false;
                    }
                    Inv.give(p, "harpoonfish", 1);
                    addPoints(p, Tempoross.POINTS_COOK);
                    xp(p, Skill.COOKING, Tempoross.cookXp(level(p, Skill.COOKING)));
                    return true;
                });
                return true;
            }
            case "tempoross_cannon" -> {
                if (essence <= 0 || isEnded()) {
                    Msg.game(player, "The cannons aren't needed anymore.");
                    return true;
                }
                if (cannonDownUntil.getOrDefault(pos.getX() - origin.getX(), -1L) >= ticks) {
                    Msg.game(player, "The torrent has knocked this cannon out for the moment.");
                    return true;
                }
                int raw = Inv.remove(player, "raw_harpoonfish", 1000);
                int cooked = Inv.remove(player, "harpoonfish", 1000);
                if (raw + cooked == 0) {
                    Msg.game(player, "You have no harpoonfish to load.");
                    return true;
                }
                addPoints(player, Tempoross.POINTS_LOAD_RAW * raw + Tempoross.POINTS_LOAD_COOKED * cooked);
                if (cannonsLoaded.computeIfAbsent(player.getUUID(), u -> new java.util.HashSet<>()).add(pos.getX())
                        && cannonsLoaded.get(player.getUUID()).size() >= 2) {
                    caFlag(player, com.runecraft.osrs.content.systems.CombatAchievements.FLAG_BOTH_CANNONS);
                }
                xp(player, Skill.FISHING, TemporossRewards.cannonXp(level(player, Skill.FISHING)) * (raw + cooked));
                if (!vulnerable()) {
                    energy = Math.max(0, energy - raw * TemporossStorm.RAW_ENERGY - cooked * TemporossStorm.COOKED_ENERGY);
                }
                world.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0);
                Msg.game(player, "You fire " + (raw + cooked) + " harpoonfish at Tempoross!");
                if (energy <= 0 && !vulnerable()) {
                    vulnerableUntil = ticks + Tempoross.VULNERABLE_TICKS;
                    place(0, -12, "tempoross_spirit_pool");
                    broadcastRed("Tempoross is vulnerable! Harpoon the spirit pool!");
                }
                return true;
            }
            case "tempoross_supplies" -> {
                int given = 0;
                for (String tool : new String[]{"rope", "hammer", BUCKET}) {
                    if (!Inv.has(player, tool, 1) && !(tool.equals(BUCKET) && Inv.has(player, WATER_BUCKET, 1))) {
                        Inv.give(player, tool, 1);
                        given++;
                    }
                }
                if (!Inv.has(player, "harpoon", 1) && !Inv.has(player, "dragon_harpoon", 1)) {
                    Inv.give(player, "harpoon", 1);
                    given++;
                }
                Msg.game(player, given > 0 ? "You take some supplies from the crate." : "You already have everything you need.");
                return true;
            }
            case "tempoross_water_pump" -> {
                int n = Inv.remove(player, BUCKET, 28);
                if (n == 0) {
                    Msg.game(player, "You need an empty bucket to fill.");
                } else {
                    Inv.give(player, WATER_BUCKET, n);
                    Msg.game(player, "You fill " + (n == 1 ? "the bucket" : n + " buckets") + " with water.");
                }
                return true;
            }
            case "tempoross_fire" -> {
                if (Inv.remove(player, WATER_BUCKET, 1) == 0) {
                    Msg.game(player, "You need a bucket of water to douse the fire.");
                    return true;
                }
                Inv.give(player, BUCKET, 1);
                douse(pos);
                addPoints(player, Tempoross.POINTS_DOUSE);
                int n = doused.merge(player.getUUID(), 1, Integer::sum);
                if (n >= Tempoross.MASTER_OF_BUCKETS) {
                    caFlag(player, com.runecraft.osrs.content.systems.CombatAchievements.FLAG_FIVE_FIRES);
                }
                Msg.game(player, "You douse the fire.");
                return true;
            }
            case "tempoross_broken_totem" -> {
                if (!Inv.has(player, "hammer", 1) && !Inv.has(player, "imcando_hammer", 1)) {
                    Msg.game(player, "You need a hammer to repair this.");
                    return true;
                }
                int dx = pos.getX() - origin.getX();
                brokenTotems.remove(dx);
                place(dx, pos.getZ() - origin.getZ(), "tempoross_totem");
                addPoints(player, Tempoross.POINTS_REPAIR);
                // approx: OSRS gives no Construction XP to players without a house.
                xp(player, Skill.CONSTRUCTION, TemporossStorm.repairXp(level(player, Skill.CONSTRUCTION)));
                caFlag(player, com.runecraft.osrs.content.systems.CombatAchievements.FLAG_TETHER_REPAIRED);
                Msg.game(player, "You repair the totem pole.");
                return true;
            }
            case "tempoross_totem" -> {
                if (!Inv.has(player, "rope", 1)) {
                    Msg.game(player, "You need a rope to tether yourself.");
                    return true;
                }
                if (waveAt - ticks <= Tempoross.WAVE_WARNING) {
                    tethered.put(player.getUUID(), waveAt + 2);
                    addPoints(player, Tempoross.POINTS_TETHER);
                    Msg.game(player, "You tether yourself to the totem pole.");
                } else {
                    Msg.game(player, "There's no wave coming right now.");
                }
                return true;
            }
            case "tempoross_spirit_pool" -> {
                if (!vulnerable()) {
                    return true;
                }
                int[] wait = {TemporossStorm.SCATTER_TICKS};
                repeat(player, p -> {
                    if (!vulnerable() || isEnded()) {
                        return false;
                    }
                    if (--wait[0] > 0) {
                        return true;
                    }
                    wait[0] = TemporossStorm.SCATTER_TICKS;
                    int max = TemporossStorm.scatterMax(level(p, Skill.FISHING));
                    essence = Math.max(0, essence - (max / 2 + random.nextInt(max - max / 2 + 1)));
                    Msg.game(p, "The glowing fish scatter, shedding their magical scales.");
                    addPoints(p, Tempoross.POINTS_SPIRIT);
                    xp(p, Skill.FISHING, TemporossRewards.spiritPoolXp(level(p, Skill.FISHING)));
                    if (essence <= 0) {
                        end(true);
                        return false;
                    }
                    return true;
                });
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    protected void gameTick() {
        if (vulnerableUntil >= 0 && ticks > vulnerableUntil) {
            vulnerableUntil = -1;
            energy = maxEnergy;
            set(0, 1, -12, Blocks.AIR.defaultBlockState());
            broadcastRed("Tempoross recovers and dives beneath the waves!");
        }
        if (ticks == waveAt - Tempoross.WAVE_WARNING) {
            broadcastRed("A colossal wave closes in... Tether yourself to a totem pole!");
        }
        boolean attacking = percent(energy, maxEnergy) > Tempoross.ATTACKS_STOP_ENERGY && !vulnerable();
        tickTorrent(attacking);
        if (ticks >= waveAt && !attacking) {
            waveAt = ticks + Tempoross.WAVE_INTERVAL; // no attacks at 10% energy or below
        } else if (ticks >= waveAt) {
            waveAt = ticks + Tempoross.WAVE_INTERVAL;
            for (ServerPlayer p : online()) {
                if (tethered.getOrDefault(p.getUUID(), -1L) >= ticks) {
                    Msg.game(p, "You hold on tight as the wave crashes over you.");
                    continue;
                }
                // No damage (OSRS Wiki): the wave washes fish and minigame tools away.
                int lost = Inv.remove(p, "raw_harpoonfish", 1 + random.nextInt(10)) + Inv.remove(p, "harpoonfish", random.nextInt(5));
                for (String tool : new String[]{"hammer", "rope", BUCKET, WATER_BUCKET}) {
                    if (random.nextInt(3) == 0) { // approx
                        lost += Inv.remove(p, tool, 1);
                    }
                }
                ActionManager.cancel(p);
                caFlag(p, com.runecraft.osrs.content.systems.CombatAchievements.FLAG_WAVE_HIT);
                Msg.red(p, "The wave slams into you" + (lost > 0 ? " and washes away " + lost + " item" + (lost > 1 ? "s!" : "!") : "!"));
            }
            for (BlockPos f : new java.util.ArrayList<>(fires.keySet())) {
                douse(f); // the wave puts out every fire
            }
            int chance = Tempoross.tetherBreakChance(Math.min(1.0, ticks / 3000.0));
            for (int x : TOTEM_X) {
                if (!brokenTotems.contains(x) && random.nextInt(100) < chance) {
                    brokenTotems.add(x);
                    place(x, TOTEM_Z, "tempoross_broken_totem");
                    broadcastRed("The wave smashes a totem pole! Repair it with a hammer.");
                }
            }
            world.sendParticles(ParticleTypes.SPLASH, origin.getX(), origin.getY() + 2, origin.getZ(), 200, 15, 1, 15, 0.2);
        }
        if (attacking && ticks % Tempoross.LIGHTNING_INTERVAL == Tempoross.LIGHTNING_INTERVAL / 2) {
            lightning();
        }
        tickFires();
        if (vulnerable()) {
            BlockPos pool = rel(0, 2, -12);
            world.sendParticles(ParticleTypes.GLOW, pool.getX() + 0.5, pool.getY(), pool.getZ() + 0.5, 5, 0.6, 0.6, 0.6, 0);
        }
        if (ticks > 3000) {
            broadcast("Tempoross escapes into the storm.");
            end(false);
        }
    }

    private static int percent(int value, int max) {
        return (int) Math.ceil(value * 100.0 / Math.max(1, max));
    }

    /**
     * Torrent attack (OSRS Wiki): a charged torrent of water surges towards a cannon; when it lands the cannon is out of
     * action for a while and anyone loading it is stunned. Step away or use the other cannon.
     */
    private void tickTorrent(boolean attacking) {
        if (torrentLands >= 0 && ticks >= torrentLands) {
            torrentLands = -1;
            cannonDownUntil.put(torrentCannon, ticks + TemporossStorm.TORRENT_DISABLE_TICKS);
            BlockPos c = rel(torrentCannon, 1, CANNON_Z);
            world.sendParticles(ParticleTypes.SPLASH, c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5, 60, 0.6, 1.2, 0.6, 0.2);
            for (ServerPlayer p : online()) {
                if (p.distanceToSqr(c.getX() + 0.5, c.getY(), c.getZ() + 0.5) <= 2.5 * 2.5) {
                    ActionManager.cancel(p);
                    p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                            TemporossStorm.TORRENT_STUN_TICKS * 12, 10, false, true));
                    Msg.red(p, "The torrent slams into the cannon and stuns you!");
                }
            }
        }
        if (torrentLands < 0 && ticks >= torrentAt) {
            torrentAt = ticks + TemporossStorm.TORRENT_INTERVAL;
            if (attacking) {
                torrentCannon = CANNON_X[random.nextInt(CANNON_X.length)];
                torrentLands = ticks + TemporossStorm.TORRENT_WARNING;
                broadcastRed("A charged torrent of water surges towards a nearby cannon...");
                BlockPos c = rel(torrentCannon, 1, CANNON_Z);
                world.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5, 30, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }

    /** Lightning strikes near the players and lights fires; anyone standing where one lands is caught in it. */
    private void lightning() {
        List<ServerPlayer> on = online();
        if (on.isEmpty()) {
            return;
        }
        broadcastRed("A strong wind blows as clouds roll in... lightning strikes!");
        for (int i = 0; i < Tempoross.FIRES_PER_STRIKE && fires.size() < Tempoross.MAX_FIRES; i++) {
            BlockPos near = relPos(on.get(random.nextInt(on.size())));
            BlockPos at = randomFloor(near.getX(), near.getZ(), 4);
            light(at);
        }
    }

    private void light(BlockPos at) {
        if (fires.containsKey(at) || !world.getBlockState(at).isAir()) {
            return;
        }
        fires.put(at, ticks);
        Arena.set(world, at, Arena.module("tempoross_fire"));
        world.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 20, 0.3, 1.0, 0.3, 0.1);
    }

    private void douse(BlockPos at) {
        fireSpreads.remove(at);
        if (fires.remove(at) != null) {
            Arena.set(world, at, Blocks.AIR.defaultBlockState());
            world.sendParticles(ParticleTypes.CLOUD, at.getX() + 0.5, at.getY() + 0.8, at.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.02);
        }
    }

    /** Fires spread when left alone and burn the supplies of anyone standing in them every 2 ticks. */
    private void tickFires() {
        if (fires.isEmpty()) {
            return;
        }
        for (Map.Entry<BlockPos, Long> f : new java.util.ArrayList<>(fires.entrySet())) {
            BlockPos at = f.getKey();
            world.sendParticles(ParticleTypes.FLAME, at.getX() + 0.5, at.getY() + 1.1, at.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.01);
            // Each fire spreads at most twice, then stabilises (OSRS Wiki "Fire (Tempoross Cove)").
            if ((ticks - f.getValue()) > 0 && (ticks - f.getValue()) % Tempoross.FIRE_SPREAD_TICKS == 0 && fires.size() < Tempoross.MAX_FIRES
                    && fireSpreads.getOrDefault(at, 0) < TemporossStorm.FIRE_SPREADS) {
                fireSpreads.merge(at, 1, Integer::sum);
                BlockPos rel = at.subtract(origin);
                light(randomFloor(rel.getX(), rel.getZ(), 2));
            }
        }
        if (ticks % Tempoross.FIRE_BURN_INTERVAL != 0) {
            return;
        }
        for (ServerPlayer p : online()) {
            for (BlockPos at : fires.keySet()) {
                double dx = p.getX() - (at.getX() + 0.5);
                double dz = p.getZ() - (at.getZ() + 0.5);
                if (dx * dx + dz * dz < 1.0 && Math.abs(p.getY() - at.getY()) < 1.5) {
                    caFlag(p, com.runecraft.osrs.content.systems.CombatAchievements.FLAG_FIRE_HIT);
                    for (String item : Tempoross.LOSABLE) {
                        if (Inv.remove(p, item, 1) > 0) {
                            Msg.red(p, "The fire burns your " + item.substring(item.indexOf(':') + 1).replace('_', ' ') + "!");
                            break;
                        }
                    }
                    break;
                }
            }
        }
    }

    private void caFlag(ServerPlayer p, String flag) {
        caPlayerFlags.computeIfAbsent(p.getUUID(), u -> new java.util.HashSet<>()).add(flag);
    }

    @Override
    public java.util.Set<String> caFlags(ServerPlayer player) {
        return new java.util.HashSet<>(caPlayerFlags.getOrDefault(player.getUUID(), java.util.Set.of()));
    }

    @Override
    protected void onEnd(boolean success) {
        if (!success) {
            return;
        }
        broadcastRed("Tempoross has been subdued!");
        for (ServerPlayer p : online()) {
            int pts = points.getOrDefault(p.getUUID(), 0);
            int permits = Tempoross.permits(pts, random.nextDouble());
            int kc = MinigameData.of(p).incrementStat(p.getUUID(), "kc:" + game.id());
            if (permits >= 10) {
                caFlag(p, com.runecraft.osrs.content.systems.CombatAchievements.FLAG_TEN_PERMITS);
            }
            if (permits > 0) {
                reward(p, "reward_permit", permits);
                Msg.game(p, "You earned " + pts + " points and " + permits + " reward permit" + (permits > 1 ? "s" : "")
                        + ". Your Tempoross subdue count is: " + kc + ".");
            } else {
                Msg.game(p, "You needed 2,000 points for a reward permit (you had " + pts + ").");
            }
        }
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        int en = percent(energy, maxEnergy);
        int es = percent(essence, maxEssence);
        return List.of("Tempoross", vulnerable() ? "Essence: " + es + "%  (VULNERABLE)" : "Energy: " + en + "%",
                "Essence: " + es + "%", "Points: " + points.getOrDefault(player.getUUID(), 0),
                waveAt - ticks <= Tempoross.WAVE_WARNING ? "WAVE INCOMING!" : "Next wave: " + time(waveAt - ticks));
    }

    /** The reward pool in the Ruins of Unkah: each permit is one roll. */
    public static void rewardPool(ServerPlayer player) {
        int permits = Inv.count(player, "reward_permit");
        if (permits == 0) {
            Msg.game(player, "You need reward permits to search the reward pool. Earn them by subduing Tempoross.");
            return;
        }
        Inv.remove(player, "reward_permit", permits);
        int fishing = com.runecraft.player.Players.data(player).skills().level(Skill.FISHING);
        java.util.Set<String> log = com.runecraft.player.Players.data(player).collectionLog();
        List<String> owned = new java.util.ArrayList<>();
        for (String u : TemporossRewards.REPLACED_WHEN_OWNED) {
            if (log.contains(u) || Inv.has(player, u, 1)) {
                owned.add(u);
            }
        }
        for (int i = 0; i < permits; i++) {
            MinigameReward r = TemporossRewards.roll(fishing, owned, POOL_RANDOM::nextDouble);
            Inv.give(player, r.item(), r.amount());
            CollectionLog.obtain(player, r.item());
            if (TemporossRewards.REPLACED_WHEN_OWNED.contains(r.item()) && !owned.contains(r.item())) {
                owned.add(r.item());
            }
            if (r.item().equals("tiny_tempor")) {
                player.getServer().getPlayerList().broadcastSystemMessage(Component.literal(player.getName().getString()
                        + " has a funny feeling like they're being followed.").withStyle(ChatFormatting.GOLD), false);
            }
        }
        Msg.game(player, "You search the reward pool " + permits + " time" + (permits > 1 ? "s" : "") + ".");
    }
}
