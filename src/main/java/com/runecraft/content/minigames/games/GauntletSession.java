package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.Arena;
import com.runecraft.content.minigames.GauntletStash;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Gauntlet;
import com.runecraft.osrs.content.minigames.Gauntlet.Foe;
import com.runecraft.osrs.content.minigames.Gauntlet.Mode;
import com.runecraft.osrs.content.minigames.Gauntlet.Node;
import com.runecraft.osrs.content.minigames.Gauntlet.Piece;
import com.runecraft.osrs.content.minigames.Gauntlet.Tier;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * One run of the Gauntlet or the Corrupted Gauntlet (see {@link Gauntlet}). The 7x7 maze is built as rooms of 13x13 blocks
 * (a room per OSRS 16x16-tile room) around the sealed boss room at the origin; the starting room lies just south of it.
 * A room's nodes and creatures appear when the player first walks in (lighting it with the sceptre in OSRS). The singing bowl
 * lists its recipes as clickable chat lines ({@code /minigame sing <recipe>}).
 *
 * <p>The run ends with the Hunllef's death, the player's death, the barrier's "Escape" during the fight, an escape crystal, the
 * starting room's teleport platform or the timer... no: the timer only forces the fight. Loot always waits in the lobby's
 * Reward Chest (wiki): a Hunllef kill rolls the main table; otherwise the run's points decide, except after leaving by the
 * teleport platform or by an escape crystal used before the fight, which give nothing.
 */
public final class GauntletSession extends MinigameSession {
    /** Blocks per room (13 inside plus a shared wall). */
    private static final int PITCH = 14;
    private static final int HALF = Gauntlet.GRID / 2 * PITCH + 6;
    private static final int HEIGHT = 5;
    private static final List<int[]> SPOTS = List.of(new int[]{-4, -4}, new int[]{4, 4}, new int[]{-4, 4}, new int[]{4, -4},
            new int[]{0, -4}, new int[]{0, 4}, new int[]{-4, 0}, new int[]{4, 0});
    /** Game ticks per resource gathered (approx: the wiki gives no gathering time). */
    private static final int GATHER_TICKS = 3;

    private final Mode mode;
    private final Gauntlet.Room[][] maze;
    private final boolean[][] lit = new boolean[Gauntlet.GRID][Gauntlet.GRID];
    private final Map<BlockPos, Integer> nodes = new HashMap<>();
    private final Map<BlockPos, Node> nodeTypes = new HashMap<>();
    /** Every creature this run woke, tracked directly (an instance chunk may not answer entity box queries yet). */
    private final List<Mob> creatures = new ArrayList<>();
    private final Set<Piece> componentsGiven = EnumSet.noneOf(Piece.class);
    private final Set<String> demisKilled = new HashSet<>();
    /** Loot owed when the player leaves: the Hunllef's chest rolls and anything left on the floor. */
    private final List<ItemStack> lootStacks = new ArrayList<>();
    /** In the order won (the Hunllef's shards first), so a full pack still gets them when the chest is opened. */
    private final Map<String, Integer> lootItems = new java.util.LinkedHashMap<>();
    private int weakKills;
    private int strongKills;
    private boolean weakFrame;
    private boolean strongFrame;
    private int points;
    private boolean armourMade;
    private boolean egniolMade;
    private int attunedWeapons;
    private long prepLeft;
    private long runTicks;
    private boolean bossFight;
    private boolean bossDead;
    /** Left by the teleport platform, or by an escape crystal before the fight: the chest stays empty (wiki). */
    private boolean noLoot;
    private int endIn;
    private final List<ServerPlayer> joining = new ArrayList<>();
    /** The Hunllef of this run once the fight has started. */
    private Mob hunllef;

    public GauntletSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.GAUNTLET, id, slot, world, variant);
        this.mode = Gauntlet.CORRUPTED_ENTRANCE.equals(variant) ? Mode.CORRUPTED : Mode.NORMAL;
        this.maze = Gauntlet.generate(mode, random);
        this.prepLeft = mode.prepTicks;
    }

    public Mode mode() {
        return mode;
    }

    public boolean inBossFight() {
        return bossFight;
    }

    public int points() {
        return points;
    }

    /** Entry requirement: Song of the Elves; the Corrupted Gauntlet also needs one Gauntlet completed. */
    public static String requirement(ServerPlayer p, String variant) {
        if (!com.runecraft.quest.QuestApi.isComplete(p, "song_of_the_elves")) {
            return "You need to complete Song of the Elves to enter the Gauntlet.";
        }
        if (Gauntlet.CORRUPTED_ENTRANCE.equals(variant) && Players.data(p).killCount(Mode.NORMAL.boss) < 1) {
            return "You must complete the Gauntlet before you can attempt the Corrupted Gauntlet.";
        }
        return null;
    }

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
        return HEIGHT;
    }

    private static int centre(int cell) {
        return (cell - Gauntlet.BOSS) * PITCH;
    }

    @Override
    protected void build() {
        boolean c = mode == Mode.CORRUPTED;
        room(c ? Blocks.BLACKSTONE : Blocks.CALCITE, c ? Blocks.RED_NETHER_BRICKS : Blocks.PRISMARINE_BRICKS,
                c ? Blocks.SHROOMLIGHT : Blocks.SEA_LANTERN);
        net.minecraft.world.level.block.state.BlockState wall = (c ? Blocks.RED_NETHER_BRICKS : Blocks.PRISMARINE_BRICKS).defaultBlockState();
        net.minecraft.world.level.block.state.BlockState air = Blocks.AIR.defaultBlockState();
        int last = Gauntlet.GRID / 2;
        for (int k = -last; k < last; k++) {
            int w = PITCH / 2 + k * PITCH;
            for (int t = -HALF; t <= HALF; t++) {
                for (int y = 1; y <= HEIGHT; y++) {
                    set(w, y, t, wall);
                    set(t, y, w, wall);
                }
            }
            // A doorway in the middle of every wall between two rooms, except into the boss room.
            for (int cell = -last; cell <= last; cell++) {
                boolean bossX = cell == 0 && Math.abs(w) == PITCH / 2;
                for (int d = -1; d <= 1; d++) {
                    for (int y = 1; y <= 3; y++) {
                        if (!bossX) {
                            set(w, y, cell * PITCH + d, air);
                            set(cell * PITCH + d, y, w, air);
                        }
                    }
                }
            }
        }
        // The barrier between the starting room and the boss room.
        for (int d = -1; d <= 1; d++) {
            for (int y = 1; y <= 3; y++) {
                set(d, y, PITCH / 2, Arena.module(Gauntlet.BARRIER));
            }
        }
        int sx = centre(Gauntlet.START_X);
        int sz = centre(Gauntlet.START_Y);
        place(sx - 4, sz - 3, Gauntlet.SINGING_BOWL);
        place(sx - 4, sz + 1, Gauntlet.RECIPES_BOOK);
        place(sx - 4, sz + 4, Gauntlet.EGNIOL_BOOK);
        place(sx + 4, sz - 3, Gauntlet.RANGE);
        place(sx + 4, sz + 1, Gauntlet.WATER_PUMP);
        place(sx + 4, sz + 4, Gauntlet.TOOL_STORAGE);
        place(sx, sz + 5, Gauntlet.TELEPORT_PLATFORM);
        lit[Gauntlet.START_X][Gauntlet.START_Y] = true;
        lit[Gauntlet.BOSS][Gauntlet.BOSS] = true;
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        // Called once per joining player before start(): remember them, since online() only sees players in the server's
        // player list (a player not listed, such as the self-test's fake player, would otherwise keep their items).
        if (!joining.contains(player)) {
            joining.add(player);
        }
        return startPoint();
    }

    private BlockPos startPoint() {
        return rel(centre(Gauntlet.START_X), 1, centre(Gauntlet.START_Y) + 2);
    }

    @Override
    protected void start() {
        List<ServerPlayer> entering = new ArrayList<>(joining);
        joining.clear();
        for (ServerPlayer p : entering) {
            GauntletStash.stash(p);
            for (String item : Gauntlet.startingItems(mode)) {
                Inv.give(p, item, 1);
            }
            Msg.game(p, "You have " + time(mode.prepTicks) + " to prepare before you are pulled in to face the "
                    + (mode == Mode.NORMAL ? "Crystalline" : "Corrupted") + " Hunllef. Walk into a room to light it.");
        }
    }

    // ------------------------------------------------------------------ ticking

    private int[] cellOf(ServerPlayer p) {
        BlockPos r = relPos(p);
        int cx = Math.floorDiv(r.getX() + PITCH / 2, PITCH) + Gauntlet.BOSS;
        int cy = Math.floorDiv(r.getZ() + PITCH / 2, PITCH) + Gauntlet.BOSS;
        if (cx < 0 || cy < 0 || cx >= Gauntlet.GRID || cy >= Gauntlet.GRID) {
            return null;
        }
        return new int[]{cx, cy};
    }

    @Override
    protected void gameTick() {
        if (bossDead) {
            sweepLoot();
            if (--endIn <= 0) {
                end(true);
            }
            return;
        }
        List<ServerPlayer> on = online();
        if (on.isEmpty()) {
            return;
        }
        ServerPlayer p = on.get(0);
        runTicks++;
        creatures.removeIf(m -> !m.isAlive() || m.isRemoved());
        if (!bossFight) {
            prepLeft--;
            if (prepLeft == 100) {
                Msg.red(p, "You have one minute left to prepare.");
            }
            if (prepLeft <= 0) {
                Msg.red(p, "Your time is up! You are pulled into the Hunllef's lair.");
                enterBoss(p);
                return;
            }
            int[] cell = cellOf(p);
            if (cell != null && !lit[cell[0]][cell[1]]) {
                populate(p, cell[0], cell[1]);
            }
        }
        topUpArrows(p);
    }

    /** Lights a room: its resource nodes appear and its creatures wake. */
    private void populate(ServerPlayer p, int cx, int cy) {
        lit[cx][cy] = true;
        Gauntlet.Room room = maze[cx][cy];
        int x0 = centre(cx);
        int z0 = centre(cy);
        int spot = 0;
        for (Node n : room.nodes()) {
            if (spot >= SPOTS.size()) {
                break;
            }
            int[] s = SPOTS.get(spot++);
            BlockPos pos = rel(x0 + s[0], 1, z0 + s[1]);
            Arena.set(world, pos, Arena.module(n.block));
            nodes.put(pos, n.yield);
            nodeTypes.put(pos, n);
        }
        for (String kind : room.creatures()) {
            Mob mob = spawn(Gauntlet.creature(mode, kind), rel(x0 + random.nextInt(5) - 2, 1, z0 + random.nextInt(5) - 2));
            if (mob != null) {
                mob.setTarget(p);
                creatures.add(mob);
            }
        }
        Msg.action(p, "Your sceptre lights the room.");
    }

    private boolean holdsBow(ServerPlayer p) {
        for (Tier t : Tier.values()) {
            if (Inv.has(p, Gauntlet.equipment(mode, Piece.BOW, t), 1)
                    || Gauntlet.equipment(mode, Piece.BOW, t).equals(Inv.idOf(p.getMainHandItem()))) {
                return true;
            }
        }
        return false;
    }

    /** Minecraft bows need ammunition; the Gauntlet's need none, so a quiver of crystal arrows is kept topped up. */
    private void topUpArrows(ServerPlayer p) {
        if (holdsBow(p)) {
            int have = Inv.count(p, Gauntlet.ARROWS);
            if (have < 16) {
                Inv.give(p, Gauntlet.ARROWS, 32 - have);
            }
        }
    }

    private void sweepLoot() {
        for (ItemEntity item : world.getEntitiesOfClass(ItemEntity.class, box(), e -> true)) {
            if (!GauntletStash.runItem(Inv.idOf(item.getItem()))) {
                lootStacks.add(item.getItem().copy());
            }
            item.discard();
        }
    }

    // ------------------------------------------------------------------ the boss

    private void enterBoss(ServerPlayer p) {
        if (bossFight) {
            return;
        }
        bossFight = true;
        BlockPos at = rel(0, 1, 4);
        p.teleportTo(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180f, 0f);
        hunllef = spawn(mode.boss, rel(0, 1, -3));
        if (hunllef != null) {
            hunllef.setTarget(p);
        } else {
            com.runecraft.RuneCraft.LOGGER.warn("The Gauntlet could not spawn {}", mode.boss);
        }
        Msg.red(p, "The " + (mode == Mode.NORMAL ? "Crystalline" : "Corrupted") + " Hunllef awakens!");
    }

    /** Self-test hook: skips the preparation and starts the Hunllef fight. */
    public void selfTestEnterBoss(ServerPlayer p) {
        enterBoss(p);
    }

    /**
     * Self-test hook: whether this run's Hunllef exists and is alive. Checked on the entity itself: a mob spawned in an
     * instance chunk the player only just reached is not visible to entity box queries until that chunk tracks entities.
     */
    public boolean selfTestHunllefAlive() {
        return hunllef != null && hunllef.isAlive() && !hunllef.isRemoved();
    }

    /** This run's Hunllef (null before the fight). */
    public Mob hunllef() {
        return hunllef;
    }

    public boolean bossDead() {
        return bossDead;
    }

    /** The run's maze (self-test and the room walk). */
    public Gauntlet.Room room(int cx, int cy) {
        return maze[cx][cy];
    }

    public boolean lit(int cx, int cy) {
        return lit[cx][cy];
    }

    /** A standing spot near the middle of a room (one block off the centre, clear of the nodes). */
    public BlockPos roomCentre(int cx, int cy) {
        return rel(centre(cx) + 1, 1, centre(cy) + 1);
    }

    /** The creatures this run woke that are still alive. */
    public List<Mob> creatures() {
        creatures.removeIf(m -> !m.isAlive() || m.isRemoved());
        return new ArrayList<>(creatures);
    }

    /** Resource nodes still standing: position to node type. */
    public Map<BlockPos, Node> nodesLeft() {
        Map<BlockPos, Node> left = new HashMap<>();
        for (Map.Entry<BlockPos, Node> e : nodeTypes.entrySet()) {
            Integer n = nodes.get(e.getKey());
            if (n != null && n > 0) {
                left.put(e.getKey(), e.getValue());
            }
        }
        return left;
    }

    /** Where the starting room's objects stand (self-test). */
    public BlockPos startObject(String blockId) {
        int sx = centre(Gauntlet.START_X);
        int sz = centre(Gauntlet.START_Y);
        return switch (blockId) {
            case Gauntlet.SINGING_BOWL -> rel(sx - 4, 1, sz - 3);
            case Gauntlet.RANGE -> rel(sx + 4, 1, sz - 3);
            case Gauntlet.WATER_PUMP -> rel(sx + 4, 1, sz + 1);
            case Gauntlet.TOOL_STORAGE -> rel(sx + 4, 1, sz + 4);
            case Gauntlet.TELEPORT_PLATFORM -> rel(sx, 1, sz + 5);
            case Gauntlet.BARRIER -> rel(0, 1, PITCH / 2);
            default -> null;
        };
    }

    @Override
    protected void onKill(ServerPlayer player, LivingEntity entity, String creatureId) {
        if (creatureId.equals(mode.boss)) {
            bossDead = true;
            endIn = 3;
            MinigameData md = MinigameData.of(player);
            int kc = md.incrementStat(player.getUUID(), "kc:" + game.id());
            String best = "best:" + mode.boss;
            int was = md.stat(player.getUUID(), best);
            if (was <= 0 || runTicks < was) {
                md.setStat(player.getUUID(), best, (int) runTicks);
            }
            Msg.game(player, "Your " + (mode == Mode.NORMAL ? "Gauntlet" : "Corrupted Gauntlet") + " completion count is: "
                    + Players.data(player).killCount(mode.boss) + " (" + kc + " in all). Duration: " + time(runTicks)
                    + (was > 0 && runTicks < was ? " (new personal best)" : "") + ".");
            return;
        }
        String kind = Gauntlet.kindOf(creatureId);
        Foe foe = kind == null ? null : Foe.of(kind);
        if (foe == null) {
            return;
        }
        points += Gauntlet.killPoints(foe);
        // 100%: shards straight into the pack (wiki: placed in the inventory since July 2026).
        int[] shards = Gauntlet.creatureShards(foe);
        Inv.give(player, Gauntlet.shards(mode), shards[0] + random.nextInt(shards[1] - shards[0] + 1));
        // Weapon frames: the first weak kill, 1/4 after; strong 2/7, the second strong kill without one; demi-bosses always.
        int kill = foe == Foe.WEAK ? ++weakKills : foe == Foe.STRONG ? ++strongKills : 1;
        boolean had = foe == Foe.WEAK ? weakFrame : foe == Foe.STRONG && strongFrame;
        if (Gauntlet.dropsFrame(foe, kill, had, random)) {
            Inv.give(player, Gauntlet.frame(mode), 1);
            if (foe == Foe.WEAK) {
                weakFrame = true;
            } else if (foe == Foe.STRONG) {
                strongFrame = true;
            }
        }
        if (foe == Foe.DEMI) {
            // Its own component if not yet owned, otherwise one of the others not yet owned (wiki, 8 August 2019).
            Piece own = Gauntlet.componentOf(kind);
            Piece give = null;
            if (!componentsGiven.contains(own)) {
                give = own;
            } else {
                List<Piece> left = new ArrayList<>();
                for (Piece pc : new Piece[]{Piece.HALBERD, Piece.STAFF, Piece.BOW}) {
                    if (!componentsGiven.contains(pc)) {
                        left.add(pc);
                    }
                }
                if (!left.isEmpty()) {
                    give = left.get(random.nextInt(left.size()));
                }
            }
            if (give != null) {
                componentsGiven.add(give);
                Inv.give(player, Gauntlet.component(mode, give), 1);
            }
            demisKilled.add(kind);
        }
        // One resource roll (wiki drop log): more shards, raw paddlefish, a grym leaf or a teleport crystal, or nothing.
        Gauntlet.Loot res = Gauntlet.resources(foe).roll(random);
        String item = Gauntlet.resourceItem(mode, res.item());
        if (item != null) {
            Inv.give(player, item, res.min() + random.nextInt(res.max() - res.min() + 1));
        }
    }

    // ------------------------------------------------------------------ objects

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        Node node = Node.byBlock(blockId);
        if (node != null) {
            gather(player, node, pos.immutable());
            return true;
        }
        switch (blockId) {
            case Gauntlet.SINGING_BOWL -> showBowl(player);
            case Gauntlet.RECIPES_BOOK -> recipes(player);
            case Gauntlet.EGNIOL_BOOK -> egniolBook(player);
            case Gauntlet.RANGE -> cook(player);
            case Gauntlet.WATER_PUMP -> fillVials(player, true);
            case Gauntlet.TOOL_STORAGE -> toolStorage(player);
            case Gauntlet.TELEPORT_PLATFORM -> {
                Msg.game(player, "You abandon the challenge. The reward chest will be empty.");
                abandon(true);
            }
            case Gauntlet.BARRIER -> {
                if (!bossFight) {
                    enterBoss(player);
                } else {
                    // "Escape" during the fight: the run's points still fill the chest (wiki).
                    Msg.red(player, "You escape from the Hunllef's lair.");
                    abandon(false);
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /** Crystal Singing Recipes: the bowl's costs. */
    public static void recipes(ServerPlayer player) {
        Msg.game(player, "Crystal Singing Recipes: armour needs shards, ore, phren bark and linum tirinum (tier 1: 50 shards and "
                + "one of each; body tier 2: 100 and two of each; tier 3: 100 and two of each). Weapons need a weapon frame, then 50 "
                + "shards, then a spike (halberd), orb (staff) or bowstring (bow) from a demi-boss.");
        Msg.game(player, "Vial: 10 shards. Teleport crystal: 50 shards. Escape crystal: 200 shards. Crystal paddlefish: a cooked "
                + "paddlefish and 10 shards.");
    }

    /** The Egniol Potions bookstand. */
    public static void egniolBook(ServerPlayer player) {
        Msg.game(player, "Egniol potion: fill a vial at the water pump or a fishing spot, add a grym leaf, then ten crystal dust "
                + "(grind ten shards with the pestle and mortar). It restores prayer and run energy and acts as a stamina potion.");
    }

    /** Fills every empty vial (water pump, or a fishing spot): one tick, doesn't stop fishing. */
    private int fillVials(ServerPlayer player, boolean tell) {
        int n = Inv.count(player, Gauntlet.VIAL);
        if (n == 0) {
            if (tell) {
                Msg.game(player, "You need an empty vial to fill.");
            }
            return 0;
        }
        Inv.remove(player, Gauntlet.VIAL, n);
        Inv.give(player, Gauntlet.WATER_VIAL, n);
        Msg.action(player, "You fill " + n + " vial" + (n > 1 ? "s" : "") + " with water.");
        return n;
    }

    /** The Tool Storage gives back the sceptre, the three tools and the pestle and mortar, one of each (wiki). */
    private void toolStorage(ServerPlayer player) {
        List<String> tools = List.of(Gauntlet.sceptre(mode), Node.BARK.tool(mode), Node.ORE.tool(mode), Node.FISH.tool(mode),
                Gauntlet.PESTLE);
        int given = 0;
        for (String t : tools) {
            boolean held = Inv.has(player, t, 1) || t.equals(Inv.idOf(player.getMainHandItem()));
            if (!held) {
                Inv.give(player, t, 1);
                given++;
            }
        }
        Msg.game(player, given == 0 ? "You already have all the tools you need." : "You take the tools you were missing.");
    }

    private void gather(ServerPlayer player, Node node, BlockPos pos) {
        Integer left = nodes.get(pos);
        if (left == null || left <= 0) {
            Msg.game(player, "There is nothing left to gather here.");
            return;
        }
        String tool = node.tool(mode);
        if (tool != null && !Inv.has(player, tool, 1)) {
            Msg.game(player, "You need a " + Inv.stack(tool, 1).getHoverName().getString().toLowerCase() + " to gather from the "
                    + node.displayName.toLowerCase() + ".");
            return;
        }
        if (node == Node.FISH) {
            fillVials(player, false); // a fishing spot is also a water source
        }
        Msg.action(player, "You start gathering from the " + node.displayName.toLowerCase() + ".");
        int[] wait = {0};
        repeat(player, p -> {
            if (isEnded() || p.blockPosition().distSqr(pos) > 25) {
                return false;
            }
            if (++wait[0] < GATHER_TICKS) {
                return true;
            }
            wait[0] = 0;
            return gatherOnce(p, node, pos);
        });
    }

    /**
     * Gathers one resource from a node with its experience and the 1/3 chance of 10-30 shards (wiki node pages); removes the
     * node when it is depleted. Returns whether the node has more.
     */
    public boolean gatherOnce(ServerPlayer p, Node node, BlockPos pos) {
        Integer remaining = nodes.get(pos);
        if (remaining == null || remaining <= 0) {
            return false;
        }
        Inv.give(p, node.item(mode), 1);
        xp(p, node.skill, node.xp);
        if (random.nextInt(Gauntlet.NODE_SHARD_CHANCE) == 0) {
            Inv.give(p, Gauntlet.shards(mode), Gauntlet.NODE_SHARD_MIN + random.nextInt(Gauntlet.NODE_SHARD_MAX - Gauntlet.NODE_SHARD_MIN + 1));
        }
        if (remaining <= 1) {
            nodes.remove(pos);
            nodeTypes.remove(pos);
            Arena.set(world, pos, Blocks.AIR.defaultBlockState());
            Msg.action(p, "The " + node.displayName.toLowerCase() + " is depleted.");
            return false;
        }
        nodes.put(pos, remaining - 1);
        return true;
    }

    /** Cooks every raw paddlefish (wiki success chart: low 155, high 368, never burnt from level 47; 15 Cooking XP each). */
    private void cook(ServerPlayer player) {
        int raw = Inv.count(player, Gauntlet.RAW_PADDLEFISH);
        if (raw == 0) {
            Msg.game(player, "You have no raw paddlefish to cook.");
            return;
        }
        int level = Players.data(player).skills().currentLevel(Skill.COOKING);
        int cooked = 0;
        Inv.remove(player, Gauntlet.RAW_PADDLEFISH, raw);
        for (int i = 0; i < raw; i++) {
            if (random.nextDouble() >= Gauntlet.cookChance(level)) {
                Inv.give(player, Gauntlet.BURNT_PADDLEFISH, 1);
            } else {
                Inv.give(player, Gauntlet.PADDLEFISH, 1);
                xp(player, Skill.COOKING, Gauntlet.PADDLEFISH_COOK_XP);
                cooked++;
            }
        }
        points += raw * Gauntlet.COOK_POINTS;
        Msg.action(player, "You cook " + cooked + " paddlefish" + (raw > cooked ? " and burn " + (raw - cooked) : "") + ".");
    }

    private void showBowl(ServerPlayer p) {
        Msg.game(p, "The singing bowl hums. Click a recipe to sing it:");
        for (Gauntlet.Sing s : Gauntlet.recipes(mode)) {
            boolean can = canSing(p, s);
            StringBuilder cost = new StringBuilder();
            for (Map.Entry<String, Integer> e : s.inputs(mode).entrySet()) {
                if (cost.length() > 0) {
                    cost.append(", ");
                }
                cost.append(e.getValue()).append(" ").append(Inv.stack(e.getKey(), 1).getHoverName().getString());
            }
            MutableComponent line = Component.literal((can ? "[Sing] " : "[----] ") + s.name()).withStyle(st -> st
                    .withColor(can ? ChatFormatting.AQUA : ChatFormatting.GRAY)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/minigame sing " + s.id())));
            line.append(Component.literal(" - " + cost).withStyle(ChatFormatting.DARK_GRAY));
            p.displayClientMessage(line, false);
        }
    }

    private boolean canSing(ServerPlayer p, Gauntlet.Sing s) {
        for (Map.Entry<String, Integer> e : s.inputs(mode).entrySet()) {
            if (!Inv.has(p, e.getKey(), e.getValue())) {
                return false;
            }
        }
        return true;
    }

    /** Right-clicking the pestle and mortar: grinds ten shards into ten crystal dust; returns how many. */
    public static int grind(ServerPlayer p) {
        if (!(MinigameManager.session(p) instanceof GauntletSession g)) {
            return 0;
        }
        String shards = Gauntlet.shards(g.mode);
        if (!Inv.has(p, shards, Gauntlet.DUST_PER_GRIND)) {
            Msg.game(p, "You need " + Gauntlet.DUST_PER_GRIND + " shards to grind.");
            return 0;
        }
        Inv.remove(p, shards, Gauntlet.DUST_PER_GRIND);
        Inv.give(p, Gauntlet.dust(g.mode), Gauntlet.DUST_PER_GRIND);
        Msg.action(p, "You grind the shards into dust.");
        return Gauntlet.DUST_PER_GRIND;
    }

    /** Right-clicking a spike, orb or bowstring: asks to crush it into 80 shards with the pestle and mortar (wiki). */
    public static void offerCrush(ServerPlayer p, String componentId) {
        if (!(MinigameManager.session(p) instanceof GauntletSession g)) {
            return;
        }
        Piece piece = Gauntlet.pieceOfComponent(g.mode, componentId);
        if (piece == null) {
            return;
        }
        if (!Inv.has(p, Gauntlet.PESTLE, 1)) {
            Msg.game(p, "You need a pestle and mortar to crush that.");
            return;
        }
        MutableComponent line = Component.literal("[Crush] ").withStyle(st -> st.withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/minigame sing crush_" + piece.key())));
        line.append(Component.literal("the " + Inv.stack(componentId, 1).getHoverName().getString().toLowerCase() + " into "
                + Gauntlet.COMPONENT_SHARDS + " shards? You won't be able to perfect a " + piece.key() + " with it.")
                .withStyle(ChatFormatting.GRAY));
        p.displayClientMessage(line, false);
    }

    /** Crushes a weapon component into 80 shards; returns whether it was crushed. */
    public boolean crush(ServerPlayer p, Piece piece) {
        String comp = Gauntlet.component(mode, piece);
        if (comp == null || !Inv.has(p, comp, 1) || !Inv.has(p, Gauntlet.PESTLE, 1)) {
            Msg.game(p, "You need the component and a pestle and mortar to crush it.");
            return false;
        }
        Inv.remove(p, comp, 1);
        Inv.give(p, Gauntlet.shards(mode), Gauntlet.COMPONENT_SHARDS);
        Msg.action(p, "You crush it into " + Gauntlet.COMPONENT_SHARDS + " shards.");
        return true;
    }

    /** Right-clicking a grym leaf: mixes it into a water-filled vial (grym potion (unf)). */
    public static boolean mixGrym(ServerPlayer p) {
        if (!(MinigameManager.session(p) instanceof GauntletSession g)) {
            return false;
        }
        if (!Inv.has(p, Gauntlet.WATER_VIAL, 1)) {
            Msg.game(p, "You need a water-filled vial to mix the grym leaf into.");
            return false;
        }
        Inv.remove(p, Gauntlet.grym(g.mode), 1);
        Inv.remove(p, Gauntlet.WATER_VIAL, 1);
        Inv.give(p, Gauntlet.GRYM_UNF, 1);
        Msg.action(p, "You put the grym leaf into the vial.");
        return true;
    }

    /** Right-clicking a grym potion (unf): ten crystal dust finish an egniol potion (3 doses, 10 Herblore XP). */
    public static boolean finishEgniol(ServerPlayer p) {
        if (!(MinigameManager.session(p) instanceof GauntletSession g)) {
            return false;
        }
        String dust = Gauntlet.dust(g.mode);
        if (!Inv.has(p, dust, Gauntlet.EGNIOL_DUST)) {
            Msg.game(p, "You need " + Gauntlet.EGNIOL_DUST + " crystal dust to finish the potion.");
            return false;
        }
        Inv.remove(p, dust, Gauntlet.EGNIOL_DUST);
        Inv.remove(p, Gauntlet.GRYM_UNF, 1);
        Inv.give(p, com.runecraft.skill.Drops.withDoses(Inv.stack(Gauntlet.EGNIOL, 1), Gauntlet.EGNIOL_DOSES));
        xp(p, Skill.HERBLORE, Gauntlet.EGNIOL_XP);
        g.egniolMade = true;
        Msg.action(p, "You mix the crystal dust into the potion.");
        return true;
    }

    /** {@code /minigame sing <recipe>}: sings one recipe at the bowl (or crushes a component: {@code crush_<piece>}). */
    public static void sing(ServerPlayer p, String recipeId) {
        if (!(MinigameManager.session(p) instanceof GauntletSession g)) {
            Msg.game(p, "You can only sing crystal at the Gauntlet's singing bowl.");
            return;
        }
        if (recipeId.startsWith("crush_")) {
            for (Piece piece : new Piece[]{Piece.HALBERD, Piece.STAFF, Piece.BOW}) {
                if (recipeId.equals("crush_" + piece.key())) {
                    g.crush(p, piece);
                }
            }
            return;
        }
        g.singRecipe(p, recipeId);
    }

    /** Sings a recipe; returns whether it was made. */
    public boolean singRecipe(ServerPlayer p, String recipeId) {
        Gauntlet.Sing s = Gauntlet.recipe(mode, recipeId);
        if (s == null || bossFight) {
            Msg.game(p, "Nothing interesting happens.");
            return false;
        }
        if (!canSing(p, s)) {
            Msg.game(p, "You don't have the materials to sing " + s.name().toLowerCase() + ".");
            return false;
        }
        for (Map.Entry<String, Integer> e : s.inputs(mode).entrySet()) {
            Inv.remove(p, e.getKey(), e.getValue());
        }
        Inv.give(p, s.out(), s.outCount());
        xp(p, Skill.SMITHING, s.xpEach());
        xp(p, Skill.CRAFTING, s.xpEach());
        points += s.points();
        if (s.piece() != null && s.piece().armour()) {
            armourMade = true;
        } else if (s.piece() != null && s.tier() == Tier.ATTUNED) {
            attunedWeapons++;
        }
        Msg.action(p, "You sing the crystal into " + Msg.article(s.name().toLowerCase()) + ".");
        return true;
    }

    /**
     * The teleport crystal: back to the starting room (wiki messages: not needed in the starting room, useless in the boss
     * room). Returns whether the crystal was used.
     */
    public boolean teleportToStart(ServerPlayer p) {
        if (bossFight) {
            Msg.game(p, "That won't help you now. At this point in the Gauntlet, you win or you die.");
            return false;
        }
        int[] cell = cellOf(p);
        if (cell != null && cell[0] == Gauntlet.START_X && cell[1] == Gauntlet.START_Y) {
            Msg.game(p, "You don't need to use that in this room.");
            return false;
        }
        BlockPos at = startPoint();
        p.teleportTo(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYRot(), p.getXRot());
        return true;
    }

    // ------------------------------------------------------------------ the end

    /** Main-table loot of a Hunllef kill (rolled by the minigames hooks), handed out when the player leaves. */
    public void owe(String item, int count) {
        lootItems.merge(item, count, Integer::sum);
    }

    /**
     * Ends the run without the Hunllef: {@code empty} for the teleport platform and an escape crystal used before the fight,
     * which leave the chest empty; otherwise (escaping the fight, dying) the run's points decide (wiki Reward Chest).
     */
    public void abandon(boolean empty) {
        if (empty) {
            noLoot = true;
        }
        end(false);
    }

    @Override
    protected void onDeath(ServerPlayer player) {
        Msg.red(player, "Oh dear, you are dead! The Gauntlet spits you out.");
        player.setHealth(player.getMaxHealth());
        MinigameData.of(player).incrementStat(player.getUUID(), "deaths:" + mode.boss);
        end(false);
    }

    @Override
    protected void onEnd(boolean success) {
        if (bossDead) {
            sweepLoot();
            return;
        }
        // An unfinished run: the chest rolls by points (50+: incomplete table, 1-49: junk, 0: nothing).
        Gauntlet.Table t = noLoot ? null : Gauntlet.unfinished(points);
        if (t != null) {
            Gauntlet.Loot l = t.roll(random);
            owe(l.item(), l.min() + random.nextInt(l.max() - l.min() + 1));
        }
        for (ServerPlayer p : online()) {
            Msg.game(p, "You earned " + points + " points" + (t == null ? "; the reward chest is empty." : "."));
        }
    }

    @Override
    protected void onLeave(ServerPlayer player) {
        GauntletStash.restore(player);
        // The run's loot waits in the Reward Chest in the lobby (OSRS), saved with the world.
        List<ItemStack> loot = new ArrayList<>();
        for (ItemStack s : lootStacks) {
            loot.add(s.copy());
        }
        lootStacks.clear();
        for (Map.Entry<String, Integer> e : lootItems.entrySet()) {
            ItemStack s = Inv.stack(e.getKey(), e.getValue());
            if (!s.isEmpty()) {
                loot.add(s);
            }
        }
        lootItems.clear();
        if (!loot.isEmpty()) {
            GauntletStash.addToChest(player, loot);
            Msg.game(player, "Your loot awaits you in the Reward Chest in the Gauntlet lobby.");
        }
    }

    /** The lobby's Reward Chest: claims the loot of past runs. */
    public static void rewardChest(ServerPlayer p) {
        int n = GauntletStash.claimChest(p);
        Msg.game(p, n == 0 ? "The reward chest is empty." : "You take your rewards from the chest.");
    }

    /** The lobby's scoreboard: completions, deaths and best times of both versions (wiki Scoreboard (The Gauntlet)). */
    public static void scoreboard(ServerPlayer p) {
        MinigameData md = MinigameData.of(p);
        for (Mode m : Mode.values()) {
            int best = md.stat(p.getUUID(), "best:" + m.boss);
            Msg.game(p, (m == Mode.NORMAL ? "The Gauntlet" : "The Corrupted Gauntlet") + ": " + Players.data(p).killCount(m.boss)
                    + " completions, " + md.stat(p.getUUID(), "deaths:" + m.boss) + " deaths, best time "
                    + (best > 0 ? time(best) : "-") + ".");
        }
    }

    @Override
    public int caStage() {
        return mode.stage();
    }

    @Override
    public Set<String> caFlags(ServerPlayer player) {
        Set<String> flags = new HashSet<>();
        if (!armourMade) {
            flags.add(com.runecraft.osrs.content.systems.CombatAchievements.FLAG_GAUNTLET_NO_ARMOUR);
        }
        if (!egniolMade) {
            flags.add(com.runecraft.osrs.content.systems.CombatAchievements.FLAG_GAUNTLET_NO_EGNIOL);
        }
        if (attunedWeapons <= 1) {
            flags.add(com.runecraft.osrs.content.systems.CombatAchievements.FLAG_GAUNTLET_ONE_WEAPON);
        }
        return flags;
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        lines.add(mode == Mode.NORMAL ? "The Gauntlet" : "The Corrupted Gauntlet");
        lines.add(bossFight ? "Hunllef fight" : "Preparation: " + time(prepLeft));
        lines.add("Points: " + points);
        return lines;
    }
}
