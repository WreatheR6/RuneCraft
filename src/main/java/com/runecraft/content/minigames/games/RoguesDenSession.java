package com.runecraft.content.minigames.games;

import com.runecraft.collection.CollectionLog;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameRules.RoguesDen;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Rogues' Den maze beneath Burthorpe (50 Agility and 50 Thieving; see {@link RoguesDen}): the wiki's route as a run of
 * walls, each with its obstacle. Agility obstacles drain Agility and Thieving traps drain Thieving (searched, never failed); the
 * mosaic door takes the right tile, the grills the right path (a wrong grill sends you back), the rogue guard needs flash
 * powder picked up first, and 80 Thieving picks the locked doors past the long route. Nothing in the maze deals damage, but
 * running out of levels or walking into the guard throws you out. A mystic jewel takes you out at any time. The wall safe at
 * the end gives a rogue kit or a rogue's equipment crate (Mod Ash's rule); leaving pays 12 XP per drained level (all of it
 * only after cracking the safe).
 */
public class RoguesDenSession extends MinigameSession {
    private static final int STEP = 3;
    private static final int START_Z = 34;
    /** Block roles at an obstacle: the obstacle itself, the guard's flash powder, the locked doors (shortcut). */
    private static final int MAIN = 0;
    private static final int POWDER = 1;
    private static final int LOCKED = 2;
    private static final int CHOICE = 10;

    private int next;
    /** Levels drained so far this run: Agility, Thieving. */
    private final int[] drained = new int[2];
    private boolean cracked;
    private boolean paid;
    private boolean powder;
    private final Map<Long, int[]> blocks = new HashMap<>();

    public RoguesDenSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.ROGUES_DEN, id, slot, world, variant);
    }

    @Override
    protected int halfX() {
        return 5;
    }

    @Override
    protected int halfZ() {
        return 38;
    }

    @Override
    protected int height() {
        return 6;
    }

    /** Relative z of obstacle {@code i}'s wall. */
    private static int wallZ(int i) {
        return START_Z - 3 - i * STEP;
    }

    private void mark(int x, int z, int index, int role) {
        place(x, z, "rd_obstacle");
        blocks.put(rel(x, 1, z).asLong(), new int[]{index, role});
    }

    @Override
    protected void build() {
        room(Blocks.COBBLED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.POLISHED_DEEPSLATE);
        BlockState wall = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        blocks.clear();
        for (int i = 0; i < RoguesDen.obstacles(); i++) {
            int z = wallZ(i);
            for (int x = -5; x <= 5; x++) {
                for (int y = 1; y <= 6; y++) {
                    set(x, y, z, wall);
                }
            }
            RoguesDen.Obstacle o = RoguesDen.ROUTE.get(i);
            switch (o.kind()) {
                case TILES -> {
                    for (int t = 0; t < 4; t++) {
                        mark(-3 + 2 * t, z, i, CHOICE + t);
                    }
                }
                case GRILLS -> {
                    for (int g = 0; g < 3; g++) {
                        mark(-2 + 2 * g, z, i, CHOICE + g);
                    }
                }
                case GUARD -> {
                    mark(0, z, i, MAIN);
                    mark(-3, z + 1, i, POWDER);
                }
                case SHORTCUT -> {
                    mark(2, z, i, MAIN);
                    mark(-2, z, i, LOCKED);
                }
                default -> mark(0, z, i, MAIN);
            }
        }
        place(0, wallZ(RoguesDen.obstacles()) + 1, "rd_safe");
        place(2, START_Z + 2, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return rel(0, 1, START_Z);
    }

    @Override
    protected void start() {
        for (ServerPlayer p : online()) {
            Inv.give(p, "mystic_jewel", 1);
            Msg.game(p, "Brian O'Richard hands you a mystic jewel: use it to leave the maze at any time.");
        }
    }

    private int remaining(ServerPlayer p, int s) {
        return level(p, s == 0 ? Skill.AGILITY : Skill.THIEVING) - drained[s];
    }

    /** Puts the player just past obstacle {@code i}'s wall. */
    private void past(ServerPlayer p, int i) {
        BlockPos to = rel(0, 1, wallZ(i) - 1);
        p.teleportTo(world, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, p.getYRot(), p.getXRot());
    }

    private void before(ServerPlayer p, int i) {
        BlockPos to = rel(0, 1, wallZ(i) + 1);
        p.teleportTo(world, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, p.getYRot(), p.getXRot());
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        if (blockId.equals("rd_safe")) {
            safe(player);
            return true;
        }
        int[] at = blocks.get(pos.asLong());
        if (at == null || !blockId.equals("rd_obstacle")) {
            return false;
        }
        int index = at[0];
        RoguesDen.Obstacle o = RoguesDen.ROUTE.get(index);
        if (index != next) {
            Msg.game(player, index < next ? "You're already past that." : "You should tackle the "
                    + RoguesDen.ROUTE.get(next).name().toLowerCase() + " first.");
            return true;
        }
        switch (o.kind()) {
            case AGILITY -> {
                if (remaining(player, 0) < o.drain()) {
                    Msg.red(player, "You're too worn out to tackle the " + o.name().toLowerCase() + " and fall out of the maze.");
                    end(false);
                    return true;
                }
                drained[0] += o.drain();
                advance(player, "You get past the " + o.name().toLowerCase() + ".");
            }
            case TRAP -> {
                if (remaining(player, 1) < o.drain()) {
                    Msg.red(player, "You can't concentrate enough to disarm the " + o.name().toLowerCase() + " and set it off!");
                    end(false);
                    return true;
                }
                drained[1] += o.drain();
                advance(player, "You search the " + o.name().toLowerCase() + " and disarm them.");
            }
            case TILES -> {
                if (at[1] - CHOICE == RoguesDen.RIGHT_TILE) {
                    advance(player, "The tile fits and the mosaic door swings open.");
                } else {
                    Msg.game(player, "That tile doesn't fit the mosaic.");
                }
            }
            case GRILLS -> {
                if (at[1] - CHOICE == RoguesDen.RIGHT_GRILL) {
                    advance(player, "You squeeze through the grill.");
                } else {
                    before(player, index);
                    Msg.game(player, "A dead end. You find your way back to the first grill.");
                }
            }
            case GUARD -> {
                if (at[1] == POWDER) {
                    powder = true;
                    Msg.game(player, "You pick up a handful of flash powder.");
                } else if (powder) {
                    powder = false;
                    advance(player, "You blind the guard with flash powder and slip past him.");
                } else {
                    Msg.red(player, "The guard spots you and throws you out of the maze!");
                    end(false);
                }
            }
            case SHORTCUT -> {
                if (at[1] == LOCKED) {
                    if (level(player, Skill.THIEVING) < RoguesDen.SHORTCUT_THIEVING) {
                        Msg.game(player, "You need a Thieving level of " + RoguesDen.SHORTCUT_THIEVING + " to pick this lock.");
                        return true;
                    }
                    next = RoguesDen.SHORTCUT_TO;
                    past(player, RoguesDen.SHORTCUT_TO - 1);
                    Msg.game(player, "You pick the locks and take the shortcut.");
                } else {
                    advance(player, "You take the passageway into the long route.");
                }
            }
        }
        return true;
    }

    private void advance(ServerPlayer p, String message) {
        past(p, next);
        next++;
        Msg.game(p, message);
    }

    private void safe(ServerPlayer player) {
        if (next < RoguesDen.obstacles()) {
            Msg.game(player, "You need to get through the maze first.");
            return;
        }
        RoguesDen.Crack c = RoguesDen.crack(Math.max(0, remaining(player, 1)), random.nextDouble(), random.nextDouble(),
                random.nextDouble());
        if (c.result() == RoguesDen.Safe.EJECTED) {
            Msg.red(player, "You fail to crack the safe and are ejected from the maze.");
            end(false);
            return;
        }
        if (c.drained()) {
            drained[1]++;
            Msg.game(player, "You fumble the lock; your Thieving is drained a little, but the safe opens.");
        }
        cracked = true;
        reward(player, c.result() == RoguesDen.Safe.KIT ? "rogue_kit" : "rogues_equipment_crate", 1);
        MinigameData.of(player).incrementStat(player.getUUID(), "kc:" + game.id());
        Msg.game(player, c.result() == RoguesDen.Safe.KIT ? "You crack the safe and find a rogue kit!"
                : "You crack the safe and find a rogue's equipment crate!");
        end(true);
    }

    @Override
    protected void gameTick() {
    }

    /** Leaving the maze (cracked, failed or walked out): 12 XP per drained level, scaled by progress unless cracked. */
    private void pay(ServerPlayer player) {
        if (paid) {
            return;
        }
        paid = true;
        Inv.remove(player, "mystic_jewel", Inv.count(player, "mystic_jewel"));
        double agility = RoguesDen.xp(drained[0], next, cracked);
        double thieving = RoguesDen.xp(drained[1], next, cracked);
        if (agility > 0) {
            xp(player, Skill.AGILITY, agility);
        }
        if (thieving > 0) {
            xp(player, Skill.THIEVING, thieving);
        }
    }

    @Override
    protected void onEnd(boolean success) {
        for (ServerPlayer p : online()) {
            pay(p);
        }
    }

    @Override
    protected void onLeave(ServerPlayer player) {
        pay(player);
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        return List.of("Rogues' Den", next < RoguesDen.obstacles() ? "Next: " + RoguesDen.ROUTE.get(next).name() : "Crack the wall safe!",
                "Agility left: " + remaining(player, 0) + "  Thieving left: " + remaining(player, 1));
    }

    // ------------------------------------------------------------------ items and Brian

    /** The mystic jewel: out of the maze. */
    public static boolean useJewel(ServerPlayer player) {
        if (MinigameManager.session(player) instanceof RoguesDenSession) {
            Msg.game(player, "The mystic jewel whisks you out of the maze.");
            MinigameManager.leave(player);
            return true;
        }
        return false;
    }

    /** A rogue's equipment crate: the first piece of rogue equipment not yet in the collection log (else a rogue kit). */
    public static void openCrate(ServerPlayer player) {
        var log = Players.data(player).collectionLog();
        for (String piece : RoguesDen.OUTFIT) {
            if (!log.contains(piece) && !Inv.has(player, piece, 1)) {
                Inv.give(player, piece, 1);
                CollectionLog.obtain(player, piece);
                Msg.game(player, "You take a piece of rogue equipment from the crate.");
                return;
            }
        }
        Inv.give(player, "rogue_kit", 1);
        Msg.game(player, "You have the whole set already, so you take a rogue kit from the crate.");
    }

    /** A rogue kit turns once into a tool the player lacks (rope, chisel, tinderbox or spade). */
    public static void useKit(ServerPlayer player) {
        for (String tool : RoguesDen.KIT_TOOLS) {
            if (!Inv.has(player, tool, 1)) {
                Inv.give(player, tool, 1);
                Msg.game(player, "You turn the rogue kit into a " + tool + ".");
                return;
            }
        }
        Inv.give(player, RoguesDen.KIT_TOOLS[0], 1);
        Msg.game(player, "You turn the rogue kit into a rope.");
    }

    /** Brian O'Richard: explains the maze; an old mystic jewel (from before the reward change) buys a crate. */
    public static void tradeJewel(ServerPlayer player) {
        if (!(MinigameManager.session(player) instanceof RoguesDenSession) && Inv.remove(player, "mystic_jewel", 1) > 0) {
            Inv.give(player, "rogues_equipment_crate", 1);
            Msg.game(player, "Brian O'Richard: One of my old jewels! Here, have a rogue's equipment crate for it.");
            return;
        }
        Msg.game(player, "Brian O'Richard: Get through the maze and crack a wall safe at the end: a rogue kit or a crate of my "
                + "rogue equipment awaits. You'll need 50 Agility and 50 Thieving.");
    }

    // ------------------------------------------------------------------ self-test hooks

    public int selfTestNext() {
        return next;
    }

    public int[] selfTestDrained() {
        return drained.clone();
    }

    /** The blocks of obstacle {@code i}: position -> role (0 main, 1 flash powder, 2 locked doors, 10+ the choices). */
    public Map<BlockPos, Integer> selfTestObstacle(int i) {
        Map<BlockPos, Integer> out = new java.util.LinkedHashMap<>();
        blocks.forEach((pos, at) -> {
            if (at[0] == i) {
                out.put(BlockPos.of(pos), at[1]);
            }
        });
        return out;
    }

    public BlockPos selfTestBefore(int i) {
        return rel(0, 1, wallZ(i) + 1);
    }

    public BlockPos selfTestSafe() {
        return rel(0, 1, wallZ(RoguesDen.obstacles()) + 1);
    }
}
