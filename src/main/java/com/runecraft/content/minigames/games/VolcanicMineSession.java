package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameCurrencies;
import com.runecraft.osrs.content.minigames.VolcanicMine;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Volcanic Mine: Petrified Pete takes 30 numulite (unless permanent access was bought for
 * 3,000), then the team has ten minutes to mine the boulder through its five sizes before the
 * volcano erupts. Each successful hit gives Mining XP and Ore Shop points, more as the boulder
 * shrinks; mining it away gives everyone 100 points. Keep the three gas chambers' pressure within
 * 40-60% (block or unblock a chamber by using it, 50 points a time up to 300) or the mine's stability
 * falls and it collapses. The mine shifts after five minutes; the last 30 seconds are for leaving.
 */
public class VolcanicMineSession extends MinigameSession {
    private static final int[] VENT_X = {-12, 0, 12};
    private static final String ACCESS_FLAG = "volcanic_mine_access";
    private final Map<UUID, Integer> points = new HashMap<>();
    private final Map<UUID, Integer> capped = new HashMap<>();
    private final int[] vents = new int[3];
    private final boolean[] blocked = new boolean[3];
    private int stability = VolcanicMine.START_STABILITY;
    private int stage = 1;
    private int stageHits;

    public VolcanicMineSession(int id, int slot, ServerWorld world, String variant) {
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
            set(x, 0, -2, Blocks.MAGMA_BLOCK.getDefaultState());
        }
        for (int i = 0; i < 3; i++) {
            place(VENT_X[i], -16, "vm_vent");
        }
        place(-18, 0, "vm_boulder");
        place(0, 20, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayerEntity player, int index) {
        return rel(index % 5 - 2, 1, 16);
    }

    @Override
    protected void start() {
        for (int i = 0; i < 3; i++) {
            vents[i] = 30 + random.nextInt(41);
        }
        for (ServerPlayerEntity p : online()) {
            MinigameData data = MinigameData.of(p);
            if (data.flag(p.getUuid(), ACCESS_FLAG)) {
                continue;
            }
            if (Inv.has(p, "numulite", VolcanicMine.PERMANENT_ACCESS_NUMULITE)) {
                Inv.remove(p, "numulite", VolcanicMine.PERMANENT_ACCESS_NUMULITE);
                data.setFlag(p.getUuid(), ACCESS_FLAG);
                Msg.game(p, "Petrified Pete takes 3,000 numulite: you now have permanent access to the Volcanic Mine.");
            } else if (Inv.has(p, "numulite", VolcanicMine.ENTRY_NUMULITE)) {
                Inv.remove(p, "numulite", VolcanicMine.ENTRY_NUMULITE);
                Msg.game(p, "Petrified Pete takes 30 numulite for your entry.");
            } else {
                Msg.game(p, "Petrified Pete wants 30 numulite to let you into the mine.");
                removePlayer(p, true);
            }
        }
        broadcastRed("The volcano rumbles. You have ten minutes before it erupts!");
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
    protected boolean onBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "vm_vent" -> {
                if (!VolcanicMine.workable(ticks)) {
                    Msg.game(player, "There's no time left to fix the chambers. Get out!");
                    return true;
                }
                int v = ventIndex(pos);
                blocked[v] = !blocked[v];
                int cap = VolcanicMine.capPoints(capped.getOrDefault(player.getUuid(), 0));
                capped.merge(player.getUuid(), cap, Integer::sum);
                points.merge(player.getUuid(), cap, Integer::sum);
                Msg.game(player, (blocked[v] ? "You block" : "You clear") + " gas chamber " + VolcanicMine.VENTS[v]
                        + ". Vent " + VolcanicMine.VENTS[v] + ": " + vents[v] + "%." + (cap > 0 ? " (+" + cap + " points)" : ""));
                return true;
            }
            case "vm_boulder" -> {
                if (!hasPickaxe(player)) {
                    Msg.game(player, "You need a pickaxe to mine the boulder.");
                    return true;
                }
                repeat(player, p -> {
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
                    points.merge(p.getUuid(), hit.points(), Integer::sum);
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
                            p.getServer().getPlayerManager().broadcast(Text.literal(p.getName().getString()
                                    + " has a funny feeling like they're being followed.").formatted(Formatting.GOLD), false);
                        }
                    }
                    if (++stageHits >= VolcanicMine.stageHp(stage, playerCount())) {
                        stage++;
                        stageHits = 0;
                        if (stage > VolcanicMine.BOULDER_STAGES) {
                            broadcastRed("The boulder falls into the lava vortex! Everyone earns "
                                    + VolcanicMine.COMPLETION_POINTS + " points.");
                            for (ServerPlayerEntity q : online()) {
                                points.merge(q.getUuid(), VolcanicMine.COMPLETION_POINTS, Integer::sum);
                            }
                            return false;
                        }
                        broadcast("The boulder shrinks and floats further down the channel: "
                                + VolcanicMine.BOULDER_NAMES[stage - 1] + " boulder.");
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

    private static boolean hasPickaxe(ServerPlayerEntity p) {
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
            broadcastRed("The mine shifts! Check the vents: the chamber pressures have changed.");
        }
        if (ticks % 10 == 0) {
            stability = VolcanicMine.stability(stability, vents, ticks);
            BlockPos c = rel(0, 2, -16);
            world.spawnParticles(ParticleTypes.LAVA, c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 4, 8, 0.5, 1, 0);
            if (stability <= 0) {
                broadcastRed("The mine collapses! You scramble out with what you've earned.");
                end(true);
                return;
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
    protected void onEnd(boolean success) {
        for (ServerPlayerEntity p : online()) {
            int pts = points.getOrDefault(p.getUuid(), 0);
            if (pts <= 0) {
                Msg.game(p, "You didn't mine anything from the boulder.");
                continue;
            }
            addPoints(p, MinigameCurrencies.VOLCANIC_MINE, pts);
            int kc = MinigameData.of(p).incrementStat(p.getUuid(), "kc:" + game.id());
            Msg.game(p, "You earned " + pts + " Volcanic Mine points. Games played: " + kc + ".");
        }
    }

    @Override
    protected List<String> hud(ServerPlayerEntity player) {
        return List.of("Volcanic Mine", "Stability: " + stability + "%",
                "Vents: A " + vents[0] + "%" + (blocked[0] ? "*" : "") + "  B " + vents[1] + "%" + (blocked[1] ? "*" : "")
                        + "  C " + vents[2] + "%" + (blocked[2] ? "*" : ""),
                stage > VolcanicMine.BOULDER_STAGES ? "Boulder: mined away!" : "Boulder: " + VolcanicMine.BOULDER_NAMES[stage - 1]
                        + " (" + stageHits + "/" + VolcanicMine.stageHp(stage, playerCount()) + ")", "Points: " + points.getOrDefault(player.getUuid(), 0),
                "Eruption in: " + time(VolcanicMine.GAME_TICKS - ticks));
    }
}
