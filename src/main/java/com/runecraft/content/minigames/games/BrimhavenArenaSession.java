package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.content.systems.SysState;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameRules.BrimhavenArena;
import com.runecraft.osrs.content.systems.Diaries;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;

/**
 * Brimhaven Agility Arena (the May 2024 rework; see {@link BrimhavenArena}): 25 pillars on a 5x5 grid over a spiked pit, each
 * neighbouring pair joined by an obstacle (rope swings, planks, ledges... pressure pads, floor spikes and hand holds from 20
 * Agility, spinning blades and darts from 40). Crossing gives the obstacle's XP (10% more with Karamja gloves 2-4); a failure
 * drops the player into the pit, hurt (darts drain Agility too), with ladders back up. One of the 24 ticket dispensers is
 * active and moves every minute: tagging it gives a ticket, a voucher (two of each 10% of the time after the elite Karamja
 * diary) and 30 Agility XP per 10 levels. Pirate Jackie the Fruit trades tickets for 345 XP and vouchers for rewards; entry
 * costs Cap'n Izzy No-Beard 200 coins.
 */
public class BrimhavenArenaSession extends MinigameSession {
    private static final int GRID = BrimhavenArena.GRID;
    private static final int SPACING = 6;
    private static final int TOP = 3;
    /** The pillar without a dispenser (approx: the one nearest the entrance ladder). */
    private static final int NO_DISPENSER = GRID * GRID - GRID / 2 - 1;
    private int active = -1;
    private long movedAt;
    private boolean tagged;
    private int tickets;
    /** Obstacle blocks: position -> {from pillar, to pillar}. */
    private final Map<Long, int[]> obstacles = new HashMap<>();

    public BrimhavenArenaSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.BRIMHAVEN_AGILITY_ARENA, id, slot, world, variant);
    }

    @Override
    protected int halfX() {
        return 16;
    }

    @Override
    protected int halfZ() {
        return 16;
    }

    @Override
    protected int height() {
        return 9;
    }

    private static int px(int i) {
        return -12 + (i % GRID) * SPACING;
    }

    private static int pz(int i) {
        return -12 + (i / GRID) * SPACING;
    }

    @Override
    protected void build() {
        room(Blocks.MUD, Blocks.JUNGLE_LOG, Blocks.MOSS_BLOCK);
        obstacles.clear();
        for (int i = 0; i < GRID * GRID; i++) {
            int x = px(i);
            int z = pz(i);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int y = 1; y <= TOP; y++) {
                        set(x + dx, y, z + dz, Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
                    }
                }
            }
            for (int y = 1; y <= TOP; y++) {
                set(x, y, z + 2, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
            }
            int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] d : dirs) {
                int gx = i % GRID + d[0];
                int gz = i / GRID + d[1];
                if (gx < 0 || gz < 0 || gx >= GRID || gz >= GRID || (d[1] == 1)) {
                    continue; // no obstacle over the south face, where the ladder is
                }
                int j = gz * GRID + gx;
                BlockPos at = rel(x + d[0], TOP + 1, z + d[1]);
                com.runecraft.content.minigames.Arena.set(world, at, com.runecraft.content.minigames.Arena.module("brimhaven_obstacle"));
                obstacles.put(at.asLong(), new int[]{i, j});
            }
            // The southern neighbour is reached from its own north face: give the link a second block on this pillar's
            // south-west corner so it can be crossed both ways.
            if (i / GRID < GRID - 1) {
                BlockPos at = rel(x - 1, TOP + 1, z + 1);
                com.runecraft.content.minigames.Arena.set(world, at, com.runecraft.content.minigames.Arena.module("brimhaven_obstacle"));
                obstacles.put(at.asLong(), new int[]{i, i + GRID});
            }
        }
        place(0, 15, "minigame_exit");
        moveDispenser();
    }

    private void moveDispenser() {
        if (active >= 0) {
            set(px(active), TOP + 1, pz(active), Blocks.AIR.defaultBlockState());
        }
        int next;
        do {
            next = random.nextInt(GRID * GRID);
        } while (next == active || next == NO_DISPENSER);
        active = next;
        set(px(active), TOP + 1, pz(active), com.runecraft.content.minigames.Arena.module("agility_ticket_dispenser"));
        movedAt = ticks;
        tagged = false;
        broadcast("The ticket dispenser moves to a new pillar.");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return rel(index % 3 - 1, 1, 14);
    }

    @Override
    protected void gameTick() {
        if (ticks - movedAt >= BrimhavenArena.MOVE_TICKS) {
            moveDispenser();
        }
        BlockPos at = rel(px(active), TOP + 2, pz(active));
        world.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0);
        if (ticks % 6 == 0) {
            for (ServerPlayer pl : online()) {
                if (relPos(pl).getY() <= 1 && random.nextInt(4) == 0) {
                    hurt(pl, 1 + random.nextInt(3));
                    Msg.action(pl, "You step on a spike in the pit!");
                }
            }
        }
    }

    private static boolean gloves(ServerPlayer p) {
        ItemStack hands = SysState.of(p).worn.get(com.runecraft.osrs.content.systems.Jewellery.Slot.HANDS);
        return hands != null && BrimhavenArena.BONUS_GLOVES.contains(Inv.idOf(hands));
    }

    private static boolean eliteKaramja(ServerPlayer p) {
        return Diaries.tierComplete(SysState.of(p).diaryDone, "karamja", Diaries.Tier.ELITE);
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        if (blockId.equals("brimhaven_obstacle")) {
            int[] link = obstacles.get(pos.asLong());
            if (link != null) {
                cross(player, link[0], link[1]);
            }
            return true;
        }
        if (!blockId.equals("agility_ticket_dispenser")) {
            return false;
        }
        if (tagged) {
            Msg.game(player, "You've already tagged this dispenser. Wait for it to move.");
            return true;
        }
        tagged = true;
        int n = eliteKaramja(player) && random.nextDouble() < BrimhavenArena.DOUBLE_TICKET_CHANCE ? 2 : 1;
        reward(player, "agility_arena_ticket", n);
        reward(player, "brimhaven_voucher", n);
        tickets += n;
        xp(player, Skill.AGILITY, BrimhavenArena.tagXp(Players.data(player).skills().currentLevel(Skill.AGILITY)));
        com.runecraft.content.GameEvents.fireActivity(player, "brimhaven_ticket");
        Msg.game(player, n == 2 ? "You tag the dispenser and receive two tickets and two Brimhaven vouchers!"
                : "You tag the dispenser and receive an agility arena ticket and a Brimhaven voucher.");
        return true;
    }

    /** Crosses the obstacle between two pillars: XP on success, a fall into the pit on a failure. */
    private void cross(ServerPlayer p, int from, int to) {
        BrimhavenArena.Obstacle o = BrimhavenArena.link(from, to);
        var skills = Players.data(p).skills();
        int agility = skills.currentLevel(Skill.AGILITY);
        if (agility < o.level) {
            Msg.game(p, "You need an Agility level of " + o.level + " to attempt the " + o.title.toLowerCase() + ".");
            return;
        }
        if (random.nextDouble() < BrimhavenArena.failChance(o, agility)) {
            BlockPos pit = rel((px(from) + px(to)) / 2, 1, (pz(from) + pz(to)) / 2);
            p.teleportTo(world, pit.getX() + 0.5, pit.getY(), pit.getZ() + 0.5, p.getYRot(), p.getXRot());
            hurt(p, Math.max(1, (int) Math.ceil(p.getHealth() / 10)));
            if (o == BrimhavenArena.Obstacle.DARTS) {
                skills.setBoost(Skill.AGILITY, skills.boost(Skill.AGILITY) - BrimhavenArena.DART_DRAIN);
                Msg.red(p, "A dart hits you and drains your Agility! You fall into the pit.");
            } else {
                Msg.red(p, "You fail the " + o.title.toLowerCase() + " and fall into the pit!");
            }
            return;
        }
        // Land on a free corner of the far pillar on the near side (edges and the south-west corner hold obstacles).
        int dx = Integer.signum(px(from) - px(to));
        int dz = Integer.signum(pz(from) - pz(to));
        BlockPos land = dx != 0 ? rel(px(to) + dx, TOP + 1, pz(to) - 1)
                : rel(px(to) + 1, TOP + 1, pz(to) + (dz < 0 ? -1 : 1));
        p.teleportTo(world, land.getX() + 0.5, land.getY(), land.getZ() + 0.5, p.getYRot(), p.getXRot());
        p.fallDistance = 0;
        xp(p, Skill.AGILITY, gloves(p) ? o.xp * BrimhavenArena.GLOVES_BONUS : o.xp);
        Msg.game(p, "You cross the " + o.title.toLowerCase() + ".");
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        return List.of("Brimhaven Agility Arena", "Tickets this visit: " + tickets,
                "Dispenser moves in: " + time(BrimhavenArena.MOVE_TICKS - (ticks - movedAt)), tagged ? "Dispenser tagged!" : "Find the glowing pillar");
    }

    // ------------------------------------------------------------------ self-test hooks

    public BlockPos selfTestDispenser() {
        return rel(px(active), TOP + 1, pz(active));
    }

    public BlockPos selfTestStand(int pillar) {
        return rel(px(pillar) + 1, TOP + 1, pz(pillar) + 1);
    }

    public int selfTestActive() {
        return active;
    }

    /** The obstacle block on {@code from} leading to {@code to}, or null. */
    public BlockPos selfTestObstacle(int from, int to) {
        for (Map.Entry<Long, int[]> e : obstacles.entrySet()) {
            if (e.getValue()[0] == from && e.getValue()[1] == to) {
                return BlockPos.of(e.getKey());
            }
        }
        return null;
    }
}
