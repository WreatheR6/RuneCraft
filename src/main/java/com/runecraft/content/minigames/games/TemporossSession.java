package com.runecraft.content.minigames.games;

import com.runecraft.action.ActionManager;
import com.runecraft.collection.CollectionLog;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameRules;
import com.runecraft.osrs.content.minigames.MinigameRules.Tempoross;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.Blocks;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Tempoross: harpoon harpoonfish, cook them at the shrine, and load them into the cannons to drain
 * Tempoross's energy. At 0% energy the spirit pool opens; harpoon it to drain his essence before
 * he recovers. Tether to a totem pole when a colossal wave comes or lose your fish. Subduing him
 * with 2,000+ points earns reward permits for the reward pool in the Ruins of Unkah.
 */
public class TemporossSession extends MinigameSession {
    private static final Random POOL_RANDOM = new Random();
    private final Map<UUID, Integer> points = new HashMap<>();
    private final Map<UUID, Long> tethered = new HashMap<>();
    private int energy = 100;
    private int essence = 100;
    private long vulnerableUntil = -1;
    private long waveAt = -1;

    public TemporossSession(int id, int slot, ServerWorld world, String variant) {
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
                set(x, 0, z, Blocks.WATER.getDefaultState());
            }
        }
        for (int[] spot : new int[][]{{-12, 6}, {12, 6}, {-18, -4}, {18, -4}}) {
            place(spot[0], spot[1], "tempoross_fishing_spot");
        }
        place(-6, -10, "tempoross_cannon");
        place(6, -10, "tempoross_cannon");
        place(0, 4, "tempoross_shrine");
        place(-12, 14, "tempoross_totem");
        place(12, 14, "tempoross_totem");
        place(0, 20, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayerEntity player, int index) {
        return rel(index % 5 - 2, 1, 16);
    }

    @Override
    protected void start() {
        MobEntity t = spawn("tempoross", rel(0, 1, -18));
        if (t != null) {
            t.setAiDisabled(true);
        }
        for (ServerPlayerEntity p : online()) {
            if (!Inv.has(p, "harpoon", 1) && !Inv.has(p, "dragon_harpoon", 1)) {
                Inv.give(p, "harpoon", 1);
                Msg.game(p, "First Mate Deri hands you a harpoon.");
            }
        }
        waveAt = ticks + Tempoross.WAVE_INTERVAL;
        broadcastRed("Tempoross rises from the sea!");
    }

    private void addPoints(ServerPlayerEntity p, int amount) {
        points.merge(p.getUuid(), amount, Integer::sum);
    }

    private boolean vulnerable() {
        return vulnerableUntil >= ticks;
    }

    @Override
    protected boolean onBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "tempoross_fishing_spot" -> {
                if (!Inv.has(player, "harpoon", 1) && !Inv.has(player, "dragon_harpoon", 1)) {
                    Msg.game(player, "You need a harpoon to fish here.");
                    return true;
                }
                Msg.action(player, "You start harpooning.");
                repeat(player, p -> {
                    if (!Inv.hasRoomFor(p, "raw_harpoonfish")) {
                        Msg.game(p, "You can't carry any more fish.");
                        return false;
                    }
                    int fishing = level(p, Skill.FISHING);
                    int chance = 30 + fishing / 2 + (Inv.has(p, "dragon_harpoon", 1) ? 10 : 0);
                    if (random.nextInt(100) < chance) {
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
                repeat(player, p -> {
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
                int raw = Inv.remove(player, "raw_harpoonfish", 1000);
                int cooked = Inv.remove(player, "harpoonfish", 1000);
                if (raw + cooked == 0) {
                    Msg.game(player, "You have no harpoonfish to load.");
                    return true;
                }
                addPoints(player, Tempoross.POINTS_LOAD * (raw + cooked));
                if (!vulnerable()) {
                    energy = Math.max(0, energy - raw * Tempoross.RAW_DAMAGE - cooked * Tempoross.COOKED_DAMAGE);
                }
                world.spawnParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0);
                Msg.game(player, "You fire " + (raw + cooked) + " harpoonfish at Tempoross!");
                if (energy <= 0 && !vulnerable()) {
                    vulnerableUntil = ticks + Tempoross.VULNERABLE_TICKS;
                    place(0, -12, "tempoross_spirit_pool");
                    broadcastRed("Tempoross is vulnerable! Harpoon the spirit pool!");
                }
                return true;
            }
            case "tempoross_totem" -> {
                if (waveAt - ticks <= Tempoross.WAVE_WARNING) {
                    tethered.put(player.getUuid(), waveAt + 2);
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
                repeat(player, p -> {
                    if (!vulnerable() || isEnded()) {
                        return false;
                    }
                    essence = Math.max(0, essence - Tempoross.SPIRIT_DAMAGE);
                    addPoints(p, Tempoross.POINTS_SPIRIT);
                    xp(p, Skill.FISHING, level(p, Skill.FISHING) * 0.5);
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
            energy = 100;
            set(0, 1, -12, Blocks.AIR.getDefaultState());
            broadcastRed("Tempoross recovers and dives beneath the waves!");
        }
        if (ticks == waveAt - Tempoross.WAVE_WARNING) {
            broadcastRed("A colossal wave closes in... Tether yourself to a totem pole!");
        }
        if (ticks >= waveAt) {
            waveAt = ticks + Tempoross.WAVE_INTERVAL;
            for (ServerPlayerEntity p : online()) {
                if (tethered.getOrDefault(p.getUuid(), -1L) >= ticks) {
                    Msg.game(p, "You hold on tight as the wave crashes over you.");
                    continue;
                }
                int lost = Inv.remove(p, "raw_harpoonfish", 1 + random.nextInt(10)) + Inv.remove(p, "harpoonfish", random.nextInt(5));
                ActionManager.cancel(p);
                hurt(p, 5 + random.nextInt(6));
                Msg.red(p, "The wave slams into you" + (lost > 0 ? " and washes away " + lost + " fish!" : "!"));
            }
            world.spawnParticles(ParticleTypes.SPLASH, origin.getX(), origin.getY() + 2, origin.getZ(), 200, 15, 1, 15, 0.2);
        }
        if (vulnerable()) {
            BlockPos pool = rel(0, 2, -12);
            world.spawnParticles(ParticleTypes.GLOW, pool.getX() + 0.5, pool.getY(), pool.getZ() + 0.5, 5, 0.6, 0.6, 0.6, 0);
        }
        if (ticks > 3000) {
            broadcast("Tempoross escapes into the storm.");
            end(false);
        }
    }

    @Override
    protected void onEnd(boolean success) {
        if (!success) {
            return;
        }
        broadcastRed("Tempoross has been subdued!");
        for (ServerPlayerEntity p : online()) {
            int pts = points.getOrDefault(p.getUuid(), 0);
            int permits = Tempoross.permits(pts);
            int kc = MinigameData.of(p).incrementStat(p.getUuid(), "kc:" + game.id());
            if (permits > 0) {
                reward(p, "reward_permit", permits);
                xp(p, Skill.FISHING, level(p, Skill.FISHING) * 15.0);
                Msg.game(p, "You earned " + pts + " points and " + permits + " reward permit" + (permits > 1 ? "s" : "")
                        + ". Your Tempoross subdue count is: " + kc + ".");
            } else {
                Msg.game(p, "You needed 2,000 points for a reward permit (you had " + pts + ").");
            }
        }
    }

    @Override
    protected List<String> hud(ServerPlayerEntity player) {
        return List.of("Tempoross", vulnerable() ? "Essence: " + essence + "%  (VULNERABLE)" : "Energy: " + energy + "%",
                "Essence: " + essence + "%", "Points: " + points.getOrDefault(player.getUuid(), 0),
                waveAt - ticks <= Tempoross.WAVE_WARNING ? "WAVE INCOMING!" : "Next wave: " + time(waveAt - ticks));
    }

    /** The reward pool in the Ruins of Unkah: each permit is one roll. */
    public static void rewardPool(ServerPlayerEntity player) {
        int permits = Inv.count(player, "reward_permit");
        if (permits == 0) {
            Msg.game(player, "You need reward permits to search the reward pool. Earn them by subduing Tempoross.");
            return;
        }
        Inv.remove(player, "reward_permit", permits);
        for (int i = 0; i < permits; i++) {
            MinigameRules.Loot l = MinigameRules.pick(Tempoross.POOL, POOL_RANDOM.nextDouble());
            Inv.give(player, l.item(), l.min() + POOL_RANDOM.nextInt(l.max() - l.min() + 1));
            for (Object[] u : Tempoross.UNIQUES) {
                if (POOL_RANDOM.nextInt((Integer) u[1]) == 0) {
                    Inv.give(player, (String) u[0], 1);
                    CollectionLog.obtain(player, (String) u[0]);
                    if (u[0].equals("tiny_tempor")) {
                        player.getServer().getPlayerManager().broadcast(Text.literal(player.getName().getString()
                                + " has a funny feeling like they're being followed.").formatted(Formatting.GOLD), false);
                    }
                }
            }
        }
        Msg.game(player, "You search the reward pool " + permits + " time" + (permits > 1 ? "s" : "") + ".");
    }
}
