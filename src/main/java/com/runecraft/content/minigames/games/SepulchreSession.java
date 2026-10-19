package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameRules.Sepulchre;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

/**
 * The Hallowed Sepulchre: race down each floor's corridor (Agility 52/62/72/82/92) while the
 * crossbow statues fire across marked lanes; a hit knocks you back to the start of the floor.
 * Coffins hold hallowed marks; the stairs give the floor's Agility XP and lead deeper; the Grand
 * Hallowed Coffin on floor 5 can hold the ring of endurance. Valindor trades hallowed marks.
 */
public class SepulchreSession extends MinigameSession {
    private int floor = 1;
    private long floorStart;
    private final Set<Long> opened = new HashSet<>();
    private int dangerRow = Integer.MIN_VALUE;
    private long fireAt = -1;

    public SepulchreSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.HALLOWED_SEPULCHRE, id, slot, world, variant);
    }

    @Override
    protected int halfX() {
        return 6;
    }

    @Override
    protected int halfZ() {
        return 26;
    }

    @Override
    protected int height() {
        return 8;
    }

    @Override
    protected void build() {
        room(Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.SCULK);
        for (int z = -20; z <= 20; z += 8) {
            place(-5, z, "sep_coffin");
            place(5, z + 4, "sep_coffin");
            place(-6, z + 2, "sep_statue");
        }
        place(0, -25, floor == 5 ? "sep_grand_coffin" : "sep_stairs");
        if (floor == 5) {
            place(3, -25, "sep_stairs");
        }
        place(0, 25, "minigame_exit");
        opened.clear();
        floorStart = ticks;
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return rel(index % 3 - 1, 1, 23);
    }

    @Override
    protected void gameTick() {
        if (ticks - floorStart >= Sepulchre.GAME_TICKS) {
            broadcast("You run out of time and are teleported out of the Sepulchre.");
            end(true);
            return;
        }
        // Crossbow statues: telegraph a lane, then fire across it.
        int interval = Math.max(3, 8 - floor);
        if (fireAt < 0 && ticks % interval == 0) {
            dangerRow = -22 + random.nextInt(45);
            fireAt = ticks + 2;
        }
        if (fireAt >= 0) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos lane = rel(0, 1, dangerRow + dz);
                world.sendParticles(ParticleTypes.CRIT, lane.getX() + 0.5, lane.getY() + 0.5, lane.getZ() + 0.5, 12, 5, 0.1, 0.1, 0);
            }
            if (ticks >= fireAt) {
                fireAt = -1;
                for (ServerPlayer p : online()) {
                    if (Math.abs(relPos(p).getZ() - dangerRow) <= 1) {
                        hurt(p, floor + random.nextInt(2 + floor));
                        Msg.red(p, "You're struck by a crossbow bolt and stagger back to the start!");
                        BlockPos at = spawnPoint(p, 0);
                        p.teleportTo(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYRot(), p.getXRot());
                    }
                }
            }
        }
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "sep_coffin" -> {
                if (!opened.add(pos.asLong())) {
                    Msg.game(player, "This coffin is empty.");
                    return true;
                }
                int marks = Sepulchre.coffinMarks(floor, random.nextDouble());
                reward(player, "hallowed_mark", marks);
                xp(player, Skill.THIEVING, 30 * floor);
                if (random.nextInt(150) == 0) {
                    reward(player, "mysterious_page", 1);
                }
                Msg.game(player, "You loot " + marks + " hallowed mark" + (marks > 1 ? "s" : "") + " from the coffin.");
                return true;
            }
            case "sep_grand_coffin" -> {
                if (!opened.add(pos.asLong())) {
                    Msg.game(player, "The Grand Hallowed Coffin is empty.");
                    return true;
                }
                boolean lockpick = Inv.has(player, "strange_old_lockpick", 1);
                int marks = 10 + random.nextInt(11) + (lockpick ? 5 : 0);
                reward(player, "hallowed_mark", marks);
                reward(player, "coins", 10_000 + random.nextInt(20_000));
                if (random.nextInt(Sepulchre.RING_CHANCE) == 0) {
                    reward(player, "ring_of_endurance", 1);
                }
                if (random.nextInt(30) == 0) {
                    reward(player, "mysterious_page", 1);
                }
                xp(player, Skill.THIEVING, 300);
                Msg.game(player, "You open the Grand Hallowed Coffin and find " + marks + " hallowed marks!");
                return true;
            }
            case "sep_stairs" -> {
                xp(player, Skill.AGILITY, Sepulchre.FLOOR_XP[floor - 1]);
                if (floor >= 5 || level(player, Skill.AGILITY) < Sepulchre.FLOOR_LEVELS[floor]) {
                    Msg.game(player, floor >= 5 ? "You have completed the Hallowed Sepulchre!"
                            : "You need an Agility level of " + Sepulchre.FLOOR_LEVELS[floor] + " to descend further.");
                    end(true);
                    return true;
                }
                floor++;
                build();
                for (ServerPlayer p : online()) {
                    BlockPos at = spawnPoint(p, 0);
                    p.teleportTo(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYRot(), p.getXRot());
                }
                broadcast("You descend to floor " + floor + ".");
                return true;
            }
            case "sep_statue" -> {
                Msg.game(player, "A crossbow statue. Watch for the bolts!");
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    protected void onEnd(boolean success) {
        for (ServerPlayer p : online()) {
            MinigameData data = MinigameData.of(p);
            data.incrementStat(p.getUUID(), "kc:" + game.id());
            if (floor > data.stat(p.getUUID(), "sepulchre_floor")) {
                data.setStat(p.getUUID(), "sepulchre_floor", floor);
            }
        }
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        return List.of("Hallowed Sepulchre", "Floor: " + floor + " / 5", "Time left: " + time(Sepulchre.GAME_TICKS - (ticks - floorStart)),
                "Hallowed marks: " + Inv.count(player, "hallowed_mark"));
    }
}
