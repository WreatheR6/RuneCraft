package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameCurrencies;
import com.runecraft.osrs.content.minigames.VolcanicMine;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;

/**
 * The Volcanic Mine: Petrified Pete takes 30 numulite (unless permanent access was bought for
 * 3,000), then the team has ten minutes to mine the boulder through its five sizes before the
 * volcano erupts. Each successful hit gives Mining XP and Ore Shop points, more as the boulder
 * shrinks; mining it away gives everyone 100 points. Keep the three gas chambers' pressure within
 * 40-60% (block or unblock a chamber by using it, 50 points a time up to 300) or the mine's stability
 * falls and it collapses. A chamber's pressure is unknown until someone checks one of its vents; the
 * mine shifts after five minutes, randomising the pressures and hiding them again. Lava beasts climb
 * out of the lava and attack, and earthquakes and meteorites (3x3, a few ticks' warning) strike more
 * often as stability falls. The last 30 seconds are for leaving.
 *
 * <p>A lava channel cuts the cavern in two: harden it with the heat-proof vessel Petrified Pete hands out
 * (36 throws; platforms melt after 90 seconds) or feel a gas hole to be shot across for a little damage.
 * Ten or more miners get a second boulder (western channel), twenty or more a third (eastern).
 */
public class VolcanicMineSession extends MinigameSession {
    private static final int[] VENT_X = {-12, 0, 12};
    /** Boulder positions: central, western and eastern channel. */
    private static final int[][] BOULDER_AT = {{-18, 0}, {-18, -8}, {18, 0}};
    /** The lava channel (arena z rows, floor level) and the gas holes on either side of it. */
    private static final int[] LAVA_Z = {3, 4};
    private static final int[][] GAS_HOLES = {{-10, 6}, {-10, 1}, {10, 6}, {10, 1}};
    private static final String LARGE_VESSEL_FLAG = "volcanic_mine_large_vessel";
    private final Map<UUID, Integer> water = new HashMap<>();
    /** Hardened lava platforms and the game tick they melt. */
    private final Map<BlockPos, Long> platforms = new HashMap<>();
    /** Falling meteorites: landing spot and game tick. */
    private final Map<BlockPos, Long> meteors = new HashMap<>();
    private int boulders = 1;
    private final int[] stages = {1, 1, 1};
    private final int[] hits = new int[3];
    private static final String ACCESS_FLAG = "volcanic_mine_access";
    private final Map<UUID, Integer> points = new HashMap<>();
    private final Map<UUID, Integer> capped = new HashMap<>();
    private final int[] vents = new int[3];
    private final boolean[] blocked = new boolean[3];
    /** Chambers whose pressure a vent check has revealed (since the start or the shift). */
    private final boolean[] known = new boolean[3];
    private final List<Mob> beasts = new ArrayList<>();
    private int stability = VolcanicMine.START_STABILITY;

    public VolcanicMineSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.VOLCANIC_MINE, id, slot, world, variant);
    }

    @Override
    protected int height() {
        return 12;
    }

    @Override
    protected void build() {
        room(Blocks.BASALT, Blocks.BLACKSTONE, Blocks.MAGMA_BLOCK);
        for (int x = -20; x <= 20; x++) {
            set(x, 0, -2, Blocks.MAGMA_BLOCK.defaultBlockState());
        }
        for (int i = 0; i < 3; i++) {
            place(VENT_X[i], -16, "vm_chamber");
            place(VENT_X[i] + 2, 8, "vm_vent");
        }
        place(BOULDER_AT[0][0], BOULDER_AT[0][1], "vm_boulder");
        for (int x = -halfX(); x <= halfX(); x++) {
            for (int z : LAVA_Z) {
                set(x, 0, z, Blocks.LAVA.defaultBlockState());
            }
        }
        for (int[] g : GAS_HOLES) {
            place(g[0], g[1], "vm_gas_hole");
        }
        place(0, 20, "minigame_exit");
    }

    private int boulderIndex(BlockPos pos) {
        int best = 0;
        for (int i = 1; i < BOULDER_AT.length; i++) {
            if (dist(pos, BOULDER_AT[i]) < dist(pos, BOULDER_AT[best])) {
                best = i;
            }
        }
        return best;
    }

    private int dist(BlockPos pos, int[] at) {
        return Math.abs(pos.getX() - origin.getX() - at[0]) + Math.abs(pos.getZ() - origin.getZ() - at[1]);
    }

    /** The heat-proof vessel, used from the item callback: true when the item was the vessel. */
    public static boolean useVessel(ServerPlayer p, String id) {
        if (!VolcanicMine.VESSEL.equals(id)) {
            return false;
        }
        if (com.runecraft.content.minigames.MinigameManager.session(p) instanceof VolcanicMineSession vm) {
            vm.throwWater(p);
        } else {
            Msg.game(p, "The vessel is only any use in the Volcanic Mine.");
        }
        return true;
    }

    /** Throws water ahead (the way the player faces): the first lava within reach hardens into a platform. */
    private void throwWater(ServerPlayer p) {
        int left = water.getOrDefault(p.getUUID(), 0);
        if (left <= 0) {
            Msg.game(p, "Your heat-proof vessel is empty.");
            return;
        }
        Direction d = p.getDirection();
        for (int i = 1; i <= VolcanicMine.THROW_RANGE; i++) {
            BlockPos at = p.blockPosition().relative(d, i).below();
            if (world.getBlockState(at).is(Blocks.LAVA)) {
                world.setBlockAndUpdate(at, Blocks.BLACKSTONE.defaultBlockState());
                platforms.put(at.immutable(), ticks + VolcanicMine.PLATFORM_TICKS);
                water.put(p.getUUID(), left - 1);
                world.playSound(null, at, SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 1f, 1f);
                Msg.action(p, "You throw water onto the lava and it hardens. (" + (left - 1) + " uses left)");
                return;
            }
        }
        Msg.game(p, "There's no lava to cool in that direction.");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return rel(index % 5 - 2, 1, 16);
    }

    @Override
    protected void start() {
        for (int i = 0; i < 3; i++) {
            vents[i] = 30 + random.nextInt(41);
        }
        boulders = VolcanicMine.boulders(playerCount());
        for (int b = 1; b < boulders; b++) {
            place(BOULDER_AT[b][0], BOULDER_AT[b][1], "vm_boulder");
        }
        for (ServerPlayer p : online()) {
            MinigameData data = MinigameData.of(p);
            if (data.flag(p.getUUID(), ACCESS_FLAG)) {
                continue;
            }
            if (Inv.has(p, "numulite", VolcanicMine.PERMANENT_ACCESS_NUMULITE)) {
                Inv.remove(p, "numulite", VolcanicMine.PERMANENT_ACCESS_NUMULITE);
                data.setFlag(p.getUUID(), ACCESS_FLAG);
                Msg.game(p, "Petrified Pete takes 3,000 numulite: you now have permanent access to the Volcanic Mine.");
            } else if (Inv.has(p, "numulite", VolcanicMine.ENTRY_NUMULITE)) {
                Inv.remove(p, "numulite", VolcanicMine.ENTRY_NUMULITE);
                Msg.game(p, "Petrified Pete takes 30 numulite for your entry.");
            } else {
                Msg.game(p, "Petrified Pete wants 30 numulite to let you into the mine.");
                removePlayer(p, true);
            }
        }
        for (ServerPlayer p : online()) {
            boolean large = MinigameData.of(p).flag(p.getUUID(), LARGE_VESSEL_FLAG);
            water.put(p.getUUID(), VolcanicMine.vesselUses(large));
            if (!Inv.has(p, VolcanicMine.VESSEL, 1)) {
                Inv.give(p, VolcanicMine.VESSEL, 1);
            }
        }
        broadcastRed("The volcano rumbles. You have ten minutes before it erupts!"
                + (boulders > 1 ? " " + boulders + " boulders are drifting down the channels." : ""));
    }

    private int ventIndex(BlockPos pos) {
        int dx = pos.getX() - origin.getX();
        int best = 0;
        for (int i = 1; i < 3; i++) {
            if (Math.abs(VENT_X[i] - dx) < Math.abs(VENT_X[best] - dx)) {
                best = i;
            }
        }
        return best;
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "vm_vent" -> {
                int v = ventIndex(pos);
                known[v] = true;
                broadcast(player.getName().getString() + " checks vent " + VolcanicMine.VENTS[v] + ": chamber "
                        + VolcanicMine.VENTS[v] + " pressure is " + vents[v] + "% and " + (blocked[v] ? "rising (blocked)." : "falling (open)."));
                return true;
            }
            case "vm_chamber" -> {
                if (!VolcanicMine.workable(ticks)) {
                    Msg.game(player, "There's no time left to fix the chambers. Get out!");
                    return true;
                }
                int v = ventIndex(pos);
                blocked[v] = !blocked[v];
                int cap = VolcanicMine.capPoints(capped.getOrDefault(player.getUUID(), 0));
                capped.merge(player.getUUID(), cap, Integer::sum);
                points.merge(player.getUUID(), cap, Integer::sum);
                Msg.game(player, (blocked[v] ? "You block" : "You clear") + " gas chamber " + VolcanicMine.VENTS[v]
                        + "." + (known[v] ? " Pressure: " + vents[v] + "%." : " Check a vent to see its pressure.")
                        + (cap > 0 ? " (+" + cap + " points)" : ""));
                return true;
            }
            case "vm_gas_hole" -> {
                // Shot across the lava channel to the gas hole on the other side, for a little damage.
                int dx = pos.getX() - origin.getX();
                boolean south = pos.getZ() - origin.getZ() > LAVA_Z[1];
                BlockPos to = rel(dx, 1, south ? LAVA_Z[0] - 3 : LAVA_Z[1] + 3);
                player.teleportTo(world, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, player.getYRot(), player.getXRot());
                hurt(player, 1 + random.nextInt(VolcanicMine.GAS_HOLE_MAX_HIT));
                Msg.game(player, "You feel the gas hole and are shot across the lava channel!");
                return true;
            }
            case "vm_boulder" -> {
                if (!hasPickaxe(player)) {
                    Msg.game(player, "You need a pickaxe to mine the boulder.");
                    return true;
                }
                int b = boulderIndex(pos);
                if (b >= boulders) {
                    Msg.game(player, "This boulder is stuck fast; it only drifts with more miners in the mine.");
                    return true;
                }
                repeat(player, p -> {
                    int stage = stages[b];
                    if (isEnded() || stage > VolcanicMine.BOULDER_STAGES) {
                        return false;
                    }
                    if (!VolcanicMine.workable(ticks)) {
                        Msg.game(p, "The volcano is about to erupt! There's no time to mine.");
                        return false;
                    }
                    int mining = level(p, Skill.MINING);
                    if (random.nextDouble() >= VolcanicMine.hitChance(mining)) {
                        return true;
                    }
                    VolcanicMine.Hit hit = VolcanicMine.hit(mining, stage, random::nextDouble);
                    points.merge(p.getUUID(), hit.points(), Integer::sum);
                    xp(p, Skill.MINING, hit.xp());
                    for (String byproduct : new String[]{"calcite", "pyrophosphite"}) {
                        if (random.nextInt(VolcanicMine.BYPRODUCT_CHANCE) == 0) {
                            reward(p, byproduct, 1 + random.nextInt(4));
                        }
                    }
                    if (hit.tier() < 0) {
                        Msg.action(p, "You chip off a part of the boulder.");
                    } else {
                        Msg.action(p, "You mine " + Msg.article(VolcanicMine.FRAGMENT_NAMES[hit.tier()]) + " fragment from the boulder.");
                        if (random.nextInt(VolcanicMine.petChance(mining)) == 0) {
                            reward(p, "pet_rock_golem", 1);
                            p.getServer().getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString()
                                    + " has a funny feeling like they're being followed.").withStyle(ChatFormatting.GOLD), false);
                        }
                    }
                    if (++hits[b] >= VolcanicMine.stageHp(stage, playerCount())) {
                        stages[b]++;
                        hits[b] = 0;
                        if (stages[b] > VolcanicMine.BOULDER_STAGES) {
                            broadcastRed("The boulder falls into the lava vortex! Everyone earns "
                                    + VolcanicMine.COMPLETION_POINTS + " points.");
                            for (ServerPlayer q : online()) {
                                points.merge(q.getUUID(), VolcanicMine.COMPLETION_POINTS, Integer::sum);
                            }
                            return false;
                        }
                        broadcast("The boulder shrinks and floats further down the channel: "
                                + VolcanicMine.BOULDER_NAMES[stages[b] - 1] + " boulder.");
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

    private static boolean hasPickaxe(ServerPlayer p) {
        for (String tier : new String[]{"bronze", "iron", "steel", "black", "mithril", "adamant", "rune", "dragon", "crystal"}) {
            if (Inv.has(p, tier + "_pickaxe", 1)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void gameTick() {
        if (ticks % 5 == 0) {
            for (int i = 0; i < 3; i++) {
                vents[i] = VolcanicMine.ventStep(vents[i], blocked[i]);
            }
        }
        if (ticks == VolcanicMine.SHIFT_TICK) {
            for (int i = 0; i < 3; i++) {
                vents[i] = 20 + random.nextInt(61);
            }
            java.util.Arrays.fill(known, false);
            broadcastRed("The mine shifts! Check the vents: the chamber pressures have changed.");
        }
        // Hardened lava melts after 90 seconds; anyone standing on it falls into the lava.
        platforms.entrySet().removeIf(e -> {
            if (ticks < e.getValue()) {
                return false;
            }
            world.setBlockAndUpdate(e.getKey(), Blocks.LAVA.defaultBlockState());
            for (ServerPlayer p : online()) {
                if (p.blockPosition().below().equals(e.getKey())) {
                    Msg.red(p, "The hardened lava melts beneath you!");
                    hurt(p, VolcanicMine.MELT_DAMAGE);
                }
            }
            return true;
        });
        // Meteorites land a few ticks after they are seen, hitting everyone in a 3x3 area.
        meteors.entrySet().removeIf(e -> {
            if (ticks < e.getValue()) {
                return false;
            }
            BlockPos at = e.getKey();
            world.sendParticles(ParticleTypes.LAVA, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 12, 1, 0.3, 1, 0);
            for (ServerPlayer p : online()) {
                BlockPos q = p.blockPosition();
                if (Math.abs(q.getX() - at.getX()) <= VolcanicMine.METEOR_RADIUS && Math.abs(q.getZ() - at.getZ()) <= VolcanicMine.METEOR_RADIUS) {
                    hurt(p, 1 + random.nextInt(VolcanicMine.METEOR_MAX_HIT));
                    Msg.red(p, "You are struck by a meteorite!");
                }
            }
            return true;
        });
        if (ticks % 10 == 0) {
            stability = VolcanicMine.stability(stability, vents, ticks);
            if (VolcanicMine.workable(ticks) && random.nextDouble() < VolcanicMine.meteorChance(stability)) {
                List<ServerPlayer> in = online();
                if (!in.isEmpty()) {
                    BlockPos at = in.get(random.nextInt(in.size())).blockPosition().immutable();
                    meteors.put(at, ticks + VolcanicMine.METEOR_FALL_TICKS);
                    world.sendParticles(ParticleTypes.FLAME, at.getX() + 0.5, at.getY() + 4, at.getZ() + 0.5, 20, 0.5, 0.5, 0.5, 0);
                    broadcastRed("A meteorite streaks down from the cavern roof!");
                }
            }
            BlockPos c = rel(0, 2, -16);
            world.sendParticles(ParticleTypes.LAVA, c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 4, 8, 0.5, 1, 0);
            if (VolcanicMine.workable(ticks) && random.nextDouble() < VolcanicMine.earthquakeChance(stability)) {
                List<ServerPlayer> in = online();
                if (!in.isEmpty()) {
                    broadcastRed("A sudden earthquake strikes the cavern!");
                    hurt(in.get(random.nextInt(in.size())), 1 + random.nextInt(VolcanicMine.EARTHQUAKE_MAX_HIT));
                }
            }
            if (stability <= 0) {
                broadcastRed("The mine collapses! You scramble out with what you've earned.");
                end(true);
                return;
            }
        }
        if (ticks > 0 && ticks % VolcanicMine.LAVA_BEAST_INTERVAL == 0 && VolcanicMine.workable(ticks)) {
            beasts.removeIf(b -> !b.isAlive());
            if (beasts.size() < VolcanicMine.maxLavaBeasts(playerCount())) {
                Mob b = spawn(VolcanicMine.LAVA_BEAST, rel(random.nextInt(31) - 15, 1, -4));
                if (b != null) {
                    beasts.add(b);
                }
            }
        }
        if (ticks == VolcanicMine.GAME_TICKS - VolcanicMine.ERUPTION_WARNING) {
            broadcastRed("The volcano is about to erupt! One minute left.");
        }
        if (ticks >= VolcanicMine.GAME_TICKS) {
            broadcastRed("The volcano erupts! Petrified Pete pulls everyone out.");
            end(true);
        }
    }

    @Override
    protected void onKill(ServerPlayer player, LivingEntity entity, String creatureId) {
        if (VolcanicMine.LAVA_BEAST.equals(creatureId)) {
            int pts = VolcanicMine.lavaBeastPoints();
            points.merge(player.getUUID(), pts, Integer::sum);
            Msg.game(player, "You slay the lava beast (+" + pts + " points).");
        }
    }

    private String vent(int i) {
        return VolcanicMine.VENTS[i] + " " + (known[i] ? vents[i] + "%" : "?") + (blocked[i] ? "*" : "");
    }

    @Override
    protected void onEnd(boolean success) {
        for (ServerPlayer p : online()) {
            Inv.remove(p, VolcanicMine.VESSEL, 1000);
            int pts = points.getOrDefault(p.getUUID(), 0);
            if (pts <= 0) {
                Msg.game(p, "You didn't mine anything from the boulder.");
                continue;
            }
            addPoints(p, MinigameCurrencies.VOLCANIC_MINE, pts);
            int kc = MinigameData.of(p).incrementStat(p.getUUID(), "kc:" + game.id());
            Msg.game(p, "You earned " + pts + " Volcanic Mine points. Games played: " + kc + ".");
        }
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        return List.of("Volcanic Mine", "Stability: " + stability + "%",
                "Chambers: " + vent(0) + "  " + vent(1) + "  " + vent(2),
                stages[0] > VolcanicMine.BOULDER_STAGES ? "Boulder: mined away!" : "Boulder: " + VolcanicMine.BOULDER_NAMES[stages[0] - 1]
                        + " (" + hits[0] + "/" + VolcanicMine.stageHp(stages[0], playerCount()) + ")"
                        + (boulders > 1 ? " +" + (boulders - 1) + " more" : ""),
                "Points: " + points.getOrDefault(player.getUUID(), 0) + "  Water: " + water.getOrDefault(player.getUUID(), 0),
                "Eruption in: " + time(VolcanicMine.GAME_TICKS - ticks));
    }
}
