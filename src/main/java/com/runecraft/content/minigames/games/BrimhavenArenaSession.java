package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

/**
 * Brimhaven Agility Arena: a grid of pillars joined by planks over a spiked pit. The ticket
 * dispenser moves to a new pillar every minute; tag it (click) before it moves for an agility
 * arena ticket and a Brimhaven voucher. Dart and spike traps punish missteps. Pirate Jackie the Fruit
 * trades tickets for Agility XP (345 each) and vouchers for rewards. Entry costs Cap'n Izzy No-Beard
 * 200 coins.
 */
public class BrimhavenArenaSession extends MinigameSession {
    private static final int GRID = 4;
    private static final int SPACING = 7;
    private int active = -1;
    private long movedAt;
    private boolean tagged;

    public BrimhavenArenaSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.BRIMHAVEN_AGILITY_ARENA, id, slot, world, variant);
    }

    @Override
    protected int halfX() {
        return 14;
    }

    @Override
    protected int halfZ() {
        return 14;
    }

    private BlockPos pillar(int i) {
        int gx = i % GRID;
        int gz = i / GRID;
        return new BlockPos(-10 + gx * SPACING, 0, -10 + gz * SPACING);
    }

    @Override
    protected void build() {
        room(Blocks.MUD, Blocks.JUNGLE_LOG, Blocks.MOSS_BLOCK);
        for (int i = 0; i < GRID * GRID; i++) {
            BlockPos p = pillar(i);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int y = 1; y <= 3; y++) {
                        set(p.getX() + dx, y, p.getZ() + dz, Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
                    }
                }
            }
            // Planks to the next pillar east and south.
            if (i % GRID < GRID - 1) {
                for (int s = 2; s < SPACING - 1; s++) {
                    set(p.getX() + s, 3, p.getZ(), (s % 3 == 0 ? Blocks.AIR : Blocks.JUNGLE_PLANKS).defaultBlockState());
                }
            }
            if (i / GRID < GRID - 1) {
                for (int s = 2; s < SPACING - 1; s++) {
                    set(p.getX(), 3, p.getZ() + s, Blocks.JUNGLE_SLAB.defaultBlockState());
                }
            }
            set(p.getX() + 2, 1, p.getZ() + 2, Blocks.LADDER.defaultBlockState());
        }
        place(0, 13, "minigame_exit");
        moveDispenser();
    }

    private void moveDispenser() {
        if (active >= 0) {
            BlockPos old = pillar(active);
            set(old.getX(), 4, old.getZ(), Blocks.AIR.defaultBlockState());
        }
        int next;
        do {
            next = random.nextInt(GRID * GRID);
        } while (next == active);
        active = next;
        BlockPos p = pillar(active);
        set(p.getX(), 4, p.getZ(), com.runecraft.content.minigames.Arena.module("agility_ticket_dispenser"));
        movedAt = ticks;
        tagged = false;
        broadcast("The ticket dispenser moves to a new pillar.");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return rel(index % 3 - 1, 1, 11);
    }

    @Override
    protected void gameTick() {
        if (ticks - movedAt >= 100) {
            moveDispenser();
        }
        BlockPos p = pillar(active);
        BlockPos at = rel(p.getX(), 5, p.getZ());
        world.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0);
        if (ticks % 6 == 0) {
            for (ServerPlayer pl : online()) {
                if (relPos(pl).getY() <= 1 && random.nextInt(4) == 0) {
                    hurt(pl, 1 + random.nextInt(3));
                    Msg.action(pl, "You step on a spike trap!");
                } else if (random.nextInt(25) == 0) {
                    hurt(pl, 1 + random.nextInt(2));
                    Msg.action(pl, "A dart hits you!");
                }
            }
        }
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        if (!blockId.equals("agility_ticket_dispenser")) {
            return false;
        }
        if (tagged) {
            Msg.game(player, "You've already tagged this pillar. Wait for the dispenser to move.");
            return true;
        }
        tagged = true;
        int agility = level(player, Skill.AGILITY);
        reward(player, "agility_arena_ticket", 1);
        reward(player, "brimhaven_voucher", 1);
        xp(player, Skill.AGILITY, agility < 20 ? 7 : agility < 40 ? 15 : 20);
        Msg.game(player, "You tag the pillar and receive an agility arena ticket and a Brimhaven voucher.");
        return true;
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        return List.of("Brimhaven Agility Arena", "Tickets: " + Inv.count(player, "agility_arena_ticket"),
                "Dispenser moves in: " + time(100 - (ticks - movedAt)), tagged ? "Pillar tagged!" : "Find the glowing pillar");
    }
}
