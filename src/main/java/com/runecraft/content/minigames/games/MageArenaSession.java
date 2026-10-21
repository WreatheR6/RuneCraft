package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.content.systems.SysState;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.MageTrainingArena;
import com.runecraft.osrs.content.minigames.MageTrainingArena.Dir;
import com.runecraft.osrs.content.minigames.MageTrainingArena.Spell;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameCurrencies;
import com.runecraft.osrs.content.minigames.MinigameRules.MageArena;
import com.runecraft.network.ServerNetworking;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

/**
 * The Mage Training Arena's four rooms after the OSRS Wiki ({@link MageTrainingArena}), each entered from its own portal:
 * <ul>
 * <li>Telekinetic Theatre (33 Magic): Telekinetic Grab pulls the maze guardian statue towards the side of the maze the caster
 * stands on (not a corner), sliding it until a wall stops it; stopping it on the exit solves the maze: 2 points and 250 Magic
 * XP, and on every fifth maze in a row 8 more points, 10 law runes, 1,000 XP and full run energy (the streak survives leaving).
 * </li>
 * <li>Alchemists' Playground (21 Magic): five items sit in six cupboards in a fixed clockwise order; every minute the items
 * move along and their alchemy values (1, 5, 8, 15, 30) are reshuffled, sometimes with one item free to alch. Alchemy turns a
 * carried item into coins at its value of the moment; deposited coins give 1 point and 10 coins per 100 (the rest as a chance
 * of one more) and 2 Magic XP per coin. Leaving deposits what the player holds.</li>
 * <li>Enchanting Chamber (7 Magic): each corner pile gives its shape (1/32 a dragonstone; more appear in the middle); the best
 * enchantment spell the player has the runes for turns one into an orb for points equal to its level, doubled every tenth
 * shape or on a dragonstone, +2 on the shape of the moment (changing every 24 seconds). Orbs deposited in the hole give 20 XP each and 3
 * blood, death or cosmic runes per 20 on leaving.</li>
 * <li>Creature Graveyard (15 Magic): each pile hands out four bones of one kind (1 to 4 fruit each) before the next kind;
 * Bones to Bananas (or Bones to Peaches, once bought) turns them into fruit; 16 bananas or 8 peaches deposited in the chute are a
 * point (fractions a chance, at most 3 a deposit), 5 / 10 XP per fruit, and one water, earth, nature, death or blood rune per
 * point on leaving. Falling bones hit for 2; dying costs 10 points.</li>
 * </ul>
 * Spells are cast by using the room's items (or clicking the statue) with the runes in the inventory (staves count); without the
 * best spell's runes the next spell down is cast (Lvl-6 Enchant, Bones to Bananas, Low Level Alchemy).
 */
public class MageArenaSession extends MinigameSession {
    private static final String[] NAMES = {"Telekinetic Theatre", "Alchemists' Playground", "Enchanting Chamber", "Creature Graveyard"};
    private static final String TELE_STREAK = "mta_tele_streak";
    private static final String ORBS = "mta_orbs";
    /** The maze's north-west cell, relative to the arena origin. */
    private static final int MAZE_X = -5;
    private static final int MAZE_Z = -5;

    private final int room;
    // ---- Telekinetic Theatre
    private MageTrainingArena.Maze maze;
    private int statueX;
    private int statueZ;
    private int mazes;
    // ---- Alchemists' Playground
    private MageTrainingArena.Cupboards cupboards;
    private final List<BlockPos> cupboardAt = new ArrayList<>();
    private final Map<UUID, Integer> coins = new HashMap<>();
    // ---- Enchanting Chamber
    private final List<BlockPos> pileAt = new ArrayList<>();
    private int bonusShape;
    private final Map<UUID, Integer> shapesEnchanted = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> owedRunes = new HashMap<>();
    // ---- Creature Graveyard
    private final int[] pileStart = new int[4];
    private final int[] pileTaken = new int[4];
    private final Map<UUID, int[]> fruit = new HashMap<>(); // bananas, peaches

    public MageArenaSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.MAGE_TRAINING_ARENA, id, slot, world, variant);
        int r = 0;
        for (int i = 0; i < MageArena.ROOMS.length; i++) {
            if (variant.contains(MageArena.ROOMS[i])) {
                r = i;
            }
        }
        this.room = r;
    }

    /** The room: 0 Telekinetic Theatre, 1 Alchemists' Playground, 2 Enchanting Chamber, 3 Creature Graveyard. */
    public int room() {
        return room;
    }

    @Override
    protected int halfX() {
        return 14;
    }

    @Override
    protected int halfZ() {
        return 14;
    }

    @Override
    protected void build() {
        room(Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_TILES, Blocks.AMETHYST_BLOCK);
        switch (room) {
            case 0 -> newMaze();
            case 1 -> {
                int[][] spots = {{-10, -10}, {0, -12}, {10, -10}, {10, 6}, {0, 8}, {-10, 6}};
                for (int[] s : spots) {
                    place(s[0], s[1], "mta_cupboard");
                    cupboardAt.add(rel(s[0], 1, s[1]));
                }
                place(0, -2, "mta_coin_collector");
                cupboards = MageTrainingArena.rotate(random);
            }
            case 2 -> {
                int[][] spots = {{-10, -10}, {10, -10}, {10, 6}, {-10, 6}};
                for (int[] s : spots) {
                    place(s[0], s[1], "mta_shape_pile");
                    pileAt.add(rel(s[0], 1, s[1]));
                }
                place(0, -2, "mta_enchant_hole");
                bonusShape = random.nextInt(MageTrainingArena.SHAPES.size());
            }
            default -> {
                int[][] spots = {{-10, -10}, {10, -10}, {10, 4}, {-10, 4}};
                for (int i = 0; i < spots.length; i++) {
                    place(spots[i][0], spots[i][1], "mta_bone_pile");
                    pileAt.add(rel(spots[i][0], 1, spots[i][1]));
                    pileStart[i] = random.nextInt(MageTrainingArena.BONES.size());
                }
                place(0, -12, "mta_food_chute");
            }
        }
        place(0, 13, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return rel(index % 3 - 1, 1, 11);
    }

    @Override
    protected void start() {
        for (ServerPlayer p : online()) {
            if (level(p, Skill.MAGIC) < MageArena.ROOM_MAGIC[room]) {
                Msg.game(p, "You need a Magic level of " + MageArena.ROOM_MAGIC[room] + " to train in this room.");
                removePlayer(p, true);
                continue;
            }
            int have = MinigameData.of(p).points(p.getUUID(), MinigameCurrencies.key(currency()));
            if (have >= MageArena.MAX_POINTS[room]) {
                Msg.red(p, "Warning: you already hold the most " + MinigameCurrencies.name(currency()) + " you can; you won't earn more here.");
            }
            switch (room) {
                case 0 -> {
                    Msg.game(p, "Telekinetic Guardian: Stand on a side of the maze and cast Telekinetic Grab on the statue to pull it "
                            + "towards you. Guide it onto the gold exit square.");
                    theatreOptions(p);
                }
                case 1 -> Msg.game(p, "Alchemy Guardian: Search the cupboards, alchemise what you find and deposit the coins. Values change "
                        + "every minute: " + values() + ".");
                case 2 -> Msg.game(p, "Enchantment Guardian: Enchant the shapes from the piles and the dragonstones. Bonus shape: "
                        + shapeName(bonusShape) + ".");
                default -> Msg.game(p, "Graveyard Guardian: Turn the bones into fruit and put it down the chute. Mind the falling bones!");
            }
        }
    }

    private String currency() {
        return switch (room) {
            case 0 -> MinigameCurrencies.MTA_TELEKINETIC;
            case 1 -> MinigameCurrencies.MTA_ALCHEMIST;
            case 2 -> MinigameCurrencies.MTA_ENCHANTMENT;
            default -> MinigameCurrencies.MTA_GRAVEYARD;
        };
    }

    private void points(ServerPlayer p, int amount) {
        String currency = currency();
        int have = MinigameData.of(p).points(p.getUUID(), MinigameCurrencies.key(currency));
        amount = Math.max(0, Math.min(amount, MageArena.MAX_POINTS[room] - have));
        if (amount == 0) {
            Msg.game(p, "You can't hold any more " + MinigameCurrencies.name(currency) + ".");
            return;
        }
        addPoints(p, currency, amount);
        Msg.action(p, "+" + amount + " " + MinigameCurrencies.name(currency));
    }

    /** Casts an arena spell: the Magic level and the runes (staves count); false with a message if it can't be cast. */
    private boolean cast(ServerPlayer p, Spell spell, boolean free) {
        if (level(p, Skill.MAGIC) < spell.level()) {
            Msg.game(p, "You need a Magic level of " + spell.level() + " to cast " + spell.name() + ".");
            return false;
        }
        if (!free && !com.runecraft.content.systems.SpellService.payRunes(p, spell.runes())) {
            Msg.game(p, "You do not have enough runes to cast " + spell.name() + ".");
            return false;
        }
        xp(p, Skill.MAGIC, spell.xp());
        return true;
    }

    /**
     * Casts the best spell of {@code bestFirst} the player has the Magic level and runes for (a free cast needs only the level),
     * the way a player picks a cheaper spell when the best one's runes are missing; null, with a message, if none can be cast.
     */
    private Spell castBest(ServerPlayer p, List<Spell> bestFirst, boolean free) {
        int magic = level(p, Skill.MAGIC);
        Spell spell = MageTrainingArena.bestCastable(magic, bestFirst,
                s -> free || com.runecraft.content.systems.SpellService.canPayRunes(p, s.runes()));
        if (spell == null) {
            Spell allowed = MageTrainingArena.bestCastable(magic, bestFirst, s -> true);
            Spell easiest = bestFirst.get(bestFirst.size() - 1);
            Msg.game(p, allowed == null ? "You need a Magic level of " + easiest.level() + " to cast " + easiest.name() + "."
                    : "You do not have enough runes to cast " + allowed.name() + (allowed == easiest ? "." : " or a lower spell."));
            return null;
        }
        return cast(p, spell, free) ? spell : null;
    }

    private static MutableComponent link(String label, String command, String hover) {
        return Component.literal("[" + label + "]").withStyle(s -> s.withColor(ChatFormatting.GOLD)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover))));
    }

    // ================================================================== Telekinetic Theatre

    private void newMaze() {
        if (maze != null) {
            for (int x = 0; x < maze.size(); x++) {
                for (int z = 0; z < maze.size(); z++) {
                    set(MAZE_X + x, 1, MAZE_Z + z, Blocks.AIR.defaultBlockState());
                    set(MAZE_X + x, 0, MAZE_Z + z, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                }
            }
        }
        maze = MageTrainingArena.maze(random);
        for (int x = 0; x < maze.size(); x++) {
            for (int z = 0; z < maze.size(); z++) {
                set(MAZE_X + x, 0, MAZE_Z + z, Blocks.SMOOTH_STONE.defaultBlockState());
                if (maze.walls()[x][z]) {
                    set(MAZE_X + x, 1, MAZE_Z + z, Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState());
                }
            }
        }
        set(MAZE_X + maze.goalX(), 0, MAZE_Z + maze.goalZ(), Blocks.GOLD_BLOCK.defaultBlockState());
        statueX = maze.startX();
        statueZ = maze.startZ();
        place(MAZE_X + statueX, MAZE_Z + statueZ, "mta_statue");
    }

    /** The maze being played (public for the self-test). */
    public MageTrainingArena.Maze maze() {
        return maze;
    }

    /** The statue's cell {x, z} (public for the self-test). */
    public int[] statue() {
        return new int[]{statueX, statueZ};
    }

    /** Where a player must stand to pull the statue in {@code d}: the middle of that side, outside the maze (self-test). */
    public BlockPos standFor(Dir d) {
        int mid = MageTrainingArena.MAZE_SIZE / 2;
        return switch (d) {
            case NORTH -> rel(MAZE_X + mid, 1, MAZE_Z - 2);
            case SOUTH -> rel(MAZE_X + mid, 1, MAZE_Z + MageTrainingArena.MAZE_SIZE + 1);
            case WEST -> rel(MAZE_X - 2, 1, MAZE_Z + mid);
            case EAST -> rel(MAZE_X + MageTrainingArena.MAZE_SIZE + 1, 1, MAZE_Z + mid);
        };
    }

    /** The side of the maze the player stands on, or null on a corner or inside (the grab is wasted). */
    private Dir side(ServerPlayer p) {
        BlockPos r = relPos(p);
        int lo = 0;
        int hi = MageTrainingArena.MAZE_SIZE - 1;
        int cx = r.getX() - MAZE_X;
        int cz = r.getZ() - MAZE_Z;
        boolean inX = cx >= lo && cx <= hi;
        boolean inZ = cz >= lo && cz <= hi;
        if (cz < lo && inX) {
            return Dir.NORTH;
        }
        if (cz > hi && inX) {
            return Dir.SOUTH;
        }
        if (cx < lo && inZ) {
            return Dir.WEST;
        }
        if (cx > hi && inZ) {
            return Dir.EAST;
        }
        return null;
    }

    /** Telekinetic Grab on the statue (public for the self-test). Returns whether the statue moved. */
    public boolean grab(ServerPlayer p) {
        if (room != 0 || !cast(p, MageTrainingArena.TELEKINETIC_GRAB, false)) {
            return false;
        }
        Dir d = side(p);
        if (d == null) {
            Msg.game(p, "You can't pull the statue from here: stand on one side of the maze, not a corner.");
            return false;
        }
        int[] to = maze.slide(statueX, statueZ, d);
        boolean moved = to[0] != statueX || to[1] != statueZ;
        set(MAZE_X + statueX, 1, MAZE_Z + statueZ, Blocks.AIR.defaultBlockState());
        statueX = to[0];
        statueZ = to[1];
        place(MAZE_X + statueX, MAZE_Z + statueZ, "mta_statue");
        BlockPos at = rel(MAZE_X + statueX, 1, MAZE_Z + statueZ);
        world.sendParticles(ParticleTypes.ENCHANT, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 20, 0.4, 0.6, 0.4, 0.3);
        if (statueX == maze.goalX() && statueZ == maze.goalZ()) {
            solved(p);
        }
        return moved;
    }

    private void solved(ServerPlayer p) {
        MinigameData data = MinigameData.of(p);
        int streak = data.stat(p.getUUID(), TELE_STREAK) + 1;
        data.setStat(p.getUUID(), TELE_STREAK, streak % MageArena.TELEKINETIC_BONUS_EVERY);
        mazes++;
        Msg.game(p, "Congratulations, you have solved the maze! The guardian comes back to life.");
        xp(p, Skill.MAGIC, MageTrainingArena.MAZE_XP);
        points(p, MageTrainingArena.mazePoints(streak));
        if (streak % MageArena.TELEKINETIC_BONUS_EVERY == 0) {
            xp(p, Skill.MAGIC, MageTrainingArena.STREAK_BONUS_XP);
            reward(p, "law_rune", MageArena.TELEKINETIC_BONUS_LAWS);
            SysState.of(p).runEnergy = 10000;
            Msg.game(p, "Five mazes in a row! You receive bonus points, law runes and your run energy is restored.");
        }
        newMaze();
    }

    /** Mazes solved this visit (public for the self-test). */
    public int mazes() {
        return mazes;
    }

    private void resetMaze(ServerPlayer p) {
        set(MAZE_X + statueX, 1, MAZE_Z + statueZ, Blocks.AIR.defaultBlockState());
        statueX = maze.startX();
        statueZ = maze.startZ();
        place(MAZE_X + statueX, MAZE_Z + statueZ, "mta_statue");
        Msg.game(p, "The statue returns to its starting square.");
    }

    // ================================================================== Alchemists' Playground

    private String values() {
        StringBuilder sb = new StringBuilder();
        for (String item : MageTrainingArena.ALCHEMY_ITEMS) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(itemName(item)).append(' ').append(cupboards.value(item)).append(cupboards.free(item) ? " (free)" : "");
        }
        return sb.toString();
    }

    private static String itemName(String id) {
        var def = com.runecraft.osrs.ItemCatalog.get(id);
        return def == null ? id : def.name();
    }

    /** The cupboards' state (public for the self-test). */
    public MageTrainingArena.Cupboards cupboards() {
        return cupboards;
    }

    /** Where cupboard {@code i} stands (public for the self-test). */
    public BlockPos cupboardAt(int i) {
        return cupboardAt.get(i);
    }

    /** Coins a player has made and not yet deposited (public for the self-test). */
    public int coinsHeld(ServerPlayer p) {
        return coins.getOrDefault(p.getUUID(), 0);
    }

    /** Alchemy on one carried cupboard item at its value of the moment (public for the self-test). */
    public boolean alch(ServerPlayer p, String item) {
        if (room != 1 || !MageTrainingArena.ALCHEMY_ITEMS.contains(item) || !Inv.has(p, item, 1)) {
            return false;
        }
        // High Level Alchemy, or Low Level Alchemy below 55 Magic or without High's runes: the coins are the same.
        if (castBest(p, List.of(MageTrainingArena.HIGH_ALCHEMY, MageTrainingArena.LOW_ALCHEMY), cupboards.free(item)) == null) {
            return false;
        }
        Inv.remove(p, item, 1);
        int value = cupboards.value(item);
        coins.merge(p.getUUID(), value, Integer::sum);
        Msg.game(p, "You alchemise the " + itemName(item) + " into " + value + " coin" + (value == 1 ? "" : "s") + " (holding "
                + coinsHeld(p) + ").");
        ServerNetworking.sync(p);
        return true;
    }

    /** Deposits the coins a player holds in the coin collector (public for the self-test). Returns the points earned. */
    public int deposit(ServerPlayer p) {
        int c = coins.getOrDefault(p.getUUID(), 0);
        if (c <= 0) {
            return 0;
        }
        coins.remove(p.getUUID());
        xp(p, Skill.MAGIC, MageArena.ALCHEMY_XP_PER_COIN * c);
        int pts = MageArena.alchemistPoints(c, random.nextDouble());
        points(p, pts);
        reward(p, "coins", pts * MageArena.ALCHEMY_COINS_REWARD);
        Msg.game(p, "You deposit " + c + " coins: " + pts + " Alchemist point" + (pts == 1 ? "" : "s") + " and "
                + pts * MageArena.ALCHEMY_COINS_REWARD + " coins for your bank.");
        return pts;
    }

    // ================================================================== Enchanting Chamber

    private static String shapeName(int index) {
        return itemName(MageTrainingArena.SHAPES.get(index));
    }

    /** The shape that gives 2 bonus points right now (public for the self-test). */
    public String bonusShape() {
        return MageTrainingArena.SHAPES.get(bonusShape);
    }

    /** Where shape pile {@code i} (cube, cylinder, pentamid, icosahedron) stands (public for the self-test). */
    public BlockPos pileAt(int i) {
        return pileAt.get(i);
    }

    /**
     * Enchants one carried shape or dragonstone into an orb with the best spell the player has the level and runes for (public
     * for the self-test); the points earned, which go by the spell actually cast.
     */
    public int enchant(ServerPlayer p, String item) {
        boolean dragonstone = item.equals(MageTrainingArena.DRAGONSTONE);
        if (room != 2 || !dragonstone && !MageTrainingArena.SHAPES.contains(item) || !Inv.has(p, item, 1)) {
            return 0;
        }
        Spell spell = castBest(p, MageTrainingArena.enchantsBestFirst(), false);
        if (spell == null) {
            return 0;
        }
        int level = MageTrainingArena.enchantLevel(spell);
        Inv.remove(p, item, 1);
        Inv.give(p, MageTrainingArena.ORB, 1);
        int nth = 0;
        if (!dragonstone) {
            nth = shapesEnchanted.merge(p.getUUID(), 1, Integer::sum);
        }
        int pts = MageArena.enchantPoints(level, nth, item.equals(bonusShape()), dragonstone);
        points(p, pts);
        ServerNetworking.sync(p);
        return pts;
    }

    private void depositOrbs(ServerPlayer p) {
        int orbs = Inv.remove(p, MageTrainingArena.ORB, 10_000);
        if (orbs <= 0) {
            Msg.game(p, "Enchant the shapes into orbs first.");
            return;
        }
        MinigameData data = MinigameData.of(p);
        int before = data.stat(p.getUUID(), ORBS);
        int counted = Math.max(0, Math.min(orbs, MageTrainingArena.ORB_CAP - before));
        data.setStat(p.getUUID(), ORBS, before + counted);
        xp(p, Skill.MAGIC, MageTrainingArena.ORB_XP * orbs);
        int rewards = (before + counted) / MageTrainingArena.ORBS_PER_REWARD - before / MageTrainingArena.ORBS_PER_REWARD;
        if (rewards > 0) {
            Map<String, Integer> owed = owedRunes.computeIfAbsent(p.getUUID(), u -> new HashMap<>());
            MageTrainingArena.randomRunes(MageTrainingArena.ORB_RUNES, rewards, random)
                    .forEach((rune, n) -> owed.merge(rune, n * MageTrainingArena.ORB_REWARD_RUNES, Integer::sum));
        }
        Msg.game(p, "You deposit " + orbs + " orb" + (orbs == 1 ? "" : "s") + ".");
    }

    // ================================================================== Creature Graveyard

    /** Takes one bone from pile {@code i}: four of one kind, then the next kind (public for the self-test). */
    public String takeBone(ServerPlayer p, int pile) {
        String bone = MageTrainingArena.pileBone(pileStart[pile], pileTaken[pile]++);
        Inv.give(p, bone, 1);
        return bone;
    }

    /**
     * Bones to Peaches once bought (at 60 Magic, with its runes), otherwise Bones to Bananas, on every carried bone (public for the
     * self-test).
     */
    public boolean castBones(ServerPlayer p) {
        if (room != 3) {
            return false;
        }
        int total = 0;
        for (String b : MageTrainingArena.BONES) {
            total += Inv.count(p, b);
        }
        if (total == 0) {
            Msg.game(p, "You have no bones to convert.");
            return false;
        }
        List<Spell> spells = MinigameData.of(p).flag(p.getUUID(), "bones_to_peaches")
                ? List.of(MageTrainingArena.BONES_TO_PEACHES, MageTrainingArena.BONES_TO_BANANAS) : List.of(MageTrainingArena.BONES_TO_BANANAS);
        Spell spell = castBest(p, spells, false);
        if (spell == null) {
            return false;
        }
        boolean peaches = spell == MageTrainingArena.BONES_TO_PEACHES;
        int made = 0;
        for (String b : MageTrainingArena.BONES) {
            made += Inv.remove(p, b, 10_000) * MageTrainingArena.fruit(b);
        }
        fruit.computeIfAbsent(p.getUUID(), u -> new int[2])[peaches ? 1 : 0] += made;
        Msg.game(p, "Your bones turn into " + made + (peaches ? " peaches." : " bananas."));
        ServerNetworking.sync(p);
        return true;
    }

    /** Fruit a player carries {bananas, peaches} (public for the self-test). */
    public int[] fruit(ServerPlayer p) {
        return fruit.getOrDefault(p.getUUID(), new int[2]).clone();
    }

    /** Puts the carried fruit down the chute (public for the self-test); the points earned. */
    public int depositFruit(ServerPlayer p) {
        int[] f = fruit.remove(p.getUUID());
        if (f == null || f[0] + f[1] == 0) {
            Msg.game(p, "Turn the bones into fruit first.");
            return 0;
        }
        xp(p, Skill.MAGIC, f[0] * MageTrainingArena.BANANA_XP + f[1] * MageTrainingArena.PEACH_XP);
        // A peach counts as two bananas: one point per 16 bananas' worth, a fraction as a chance, at most 3 a deposit.
        int pts = MageArena.graveyardPoints(f[0] + 2 * f[1], false, random.nextDouble());
        points(p, pts);
        if (pts > 0) {
            Map<String, Integer> owed = owedRunes.computeIfAbsent(p.getUUID(), u -> new HashMap<>());
            MageTrainingArena.randomRunes(MageTrainingArena.GRAVEYARD_RUNES, pts, random).forEach((rune, n) -> owed.merge(rune, n, Integer::sum));
        }
        Msg.game(p, "You deposit " + f[0] + " bananas and " + f[1] + " peaches.");
        return pts;
    }

    // ================================================================== clicks, item use, ticks

    /** Using a room item: alchemy, enchantment or the bone spell. True if the click was the arena's. */
    public static boolean useItem(ServerPlayer p, String id) {
        boolean ours = MageTrainingArena.ALCHEMY_ITEMS.contains(id) || MageTrainingArena.SHAPES.contains(id)
                || id.equals(MageTrainingArena.DRAGONSTONE) || MageTrainingArena.BONES.contains(id);
        if (!ours) {
            return false;
        }
        if (!(MinigameManager.session(p) instanceof MageArenaSession mta)) {
            Msg.game(p, "You can only do that in the Mage Training Arena.");
            return true;
        }
        if (MageTrainingArena.ALCHEMY_ITEMS.contains(id)) {
            mta.alch(p, id);
        } else if (MageTrainingArena.BONES.contains(id)) {
            mta.castBones(p);
        } else {
            mta.enchant(p, id);
        }
        return true;
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "mta_statue" -> {
                grab(player);
                return true;
            }
            case "mta_cupboard" -> {
                int i = cupboardAt.indexOf(pos);
                String item = i < 0 ? null : cupboards.item(i);
                if (item == null) {
                    Msg.game(player, "The cupboard is empty.");
                    return true;
                }
                int n = player.isShiftKeyDown() ? MageTrainingArena.TAKE_MAX : 1;
                Inv.give(player, item, n);
                Msg.game(player, "You found: " + itemName(item) + (n > 1 ? " x" + n : "") + " (worth " + cupboards.value(item) + ").");
                return true;
            }
            case "mta_coin_collector" -> {
                if (coinsHeld(player) == 0) {
                    Msg.game(player, "Alchemise the cupboard items first, then deposit the coins here.");
                } else {
                    deposit(player);
                }
                return true;
            }
            case "mta_shape_pile" -> {
                int i = pileAt.indexOf(pos);
                if (i >= 0) {
                    if (random.nextInt(MageTrainingArena.PILE_DRAGONSTONE_CHANCE) == 0) {
                        Inv.give(player, MageTrainingArena.DRAGONSTONE, 1);
                        Msg.game(player, "You find a dragonstone in the pile!");
                    } else {
                        Inv.give(player, MageTrainingArena.SHAPES.get(i), 1);
                    }
                }
                return true;
            }
            case "mta_dragonstone_spot" -> {
                Inv.give(player, MageTrainingArena.DRAGONSTONE, 1);
                set(pos.getX() - origin.getX(), pos.getY() - origin.getY(), pos.getZ() - origin.getZ(), Blocks.AIR.defaultBlockState());
                return true;
            }
            case "mta_enchant_hole" -> {
                depositOrbs(player);
                return true;
            }
            case "mta_bone_pile" -> {
                int i = pileAt.indexOf(pos);
                if (i >= 0) {
                    int n = player.isShiftKeyDown() ? 4 : 1;
                    for (int k = 0; k < n; k++) {
                        takeBone(player, i);
                    }
                }
                return true;
            }
            case "mta_food_chute" -> {
                depositFruit(player);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    protected boolean onAction(ServerPlayer player, String action, String arg) {
        if (action.equals("reset") && room == 0) {
            resetMaze(player);
            return true;
        }
        if (action.equals("newmaze") && room == 0) {
            newMaze();
            Msg.game(player, "The guardian sets up a new maze.");
            return true;
        }
        return false;
    }

    @Override
    protected void gameTick() {
        if (room == 1 && ticks % MageArena.ALCHEMY_ROTATE_TICKS == 0) {
            cupboards = MageTrainingArena.rotate(random);
            broadcast("The Alchemy Guardian moves the items around. Values now: " + values() + ".");
        }
        if (room == 2) {
            if (ticks % MageTrainingArena.BONUS_SHAPE_TICKS == 0) {
                bonusShape = random.nextInt(MageTrainingArena.SHAPES.size());
                broadcast("The bonus shape is now the " + shapeName(bonusShape).toLowerCase() + ".");
            }
            if (ticks % MageTrainingArena.DRAGONSTONE_RESPAWN_TICKS == 0) {
                place(random.nextInt(9) - 4, random.nextInt(5) + 1, "mta_dragonstone_spot");
            }
        }
        if (room == 3 && ticks % 8 == 0) {
            for (ServerPlayer p : online()) {
                if (random.nextInt(3) == 0) {
                    world.sendParticles(ParticleTypes.WHITE_ASH, p.getX(), p.getY() + 3, p.getZ(), 15, 0.5, 0.5, 0.5, 0);
                    hurt(p, MageArena.GRAVEYARD_BONE_DAMAGE);
                    Msg.action(p, "You're hit by falling bones!");
                }
            }
        }
    }

    @Override
    protected void onDeath(ServerPlayer player) {
        if (room == 3) {
            int have = MinigameData.of(player).points(player.getUUID(), "mta_graveyard");
            addPoints(player, MinigameCurrencies.MTA_GRAVEYARD, -Math.min(have, MageArena.GRAVEYARD_DEATH_PENALTY));
        }
        super.onDeath(player);
    }

    @Override
    protected void onLeave(ServerPlayer player) {
        if (room == 1 && coinsHeld(player) > 0) {
            // Leaving deposits the coins the player made (OSRS Wiki).
            deposit(player);
        }
        for (String item : MageTrainingArena.ALCHEMY_ITEMS) {
            Inv.remove(player, item, 10_000);
        }
        for (String item : MageTrainingArena.SHAPES) {
            Inv.remove(player, item, 10_000);
        }
        Inv.remove(player, MageTrainingArena.DRAGONSTONE, 10_000);
        Inv.remove(player, MageTrainingArena.ORB, 10_000);
        for (String b : MageTrainingArena.BONES) {
            Inv.remove(player, b, 10_000);
        }
        Inv.remove(player, "alchemy_item", 1000);
        Inv.remove(player, "enchantment_shape", 1000);
        fruit.remove(player.getUUID());
        coins.remove(player.getUUID());
        Map<String, Integer> owed = owedRunes.remove(player.getUUID());
        if (owed != null) {
            owed.forEach((rune, n) -> reward(player, rune, n));
            Msg.game(player, "The guardian rewards you with runes for your work.");
        }
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        MinigameData d = MinigameData.of(player);
        String line = switch (room) {
            case 0 -> "Mazes solved: " + mazes + "  Streak: " + d.stat(player.getUUID(), TELE_STREAK) + "/5";
            case 1 -> "Coins held: " + coinsHeld(player);
            case 2 -> "Bonus shape: " + shapeName(bonusShape);
            default -> "Bananas: " + fruit(player)[0] + "  Peaches: " + fruit(player)[1];
        };
        return List.of("Mage Training Arena - " + NAMES[room],
                "Telekinetic: " + d.points(player.getUUID(), "mta_telekinetic") + "  Alchemist: " + d.points(player.getUUID(), "mta_alchemist"),
                "Enchantment: " + d.points(player.getUUID(), "mta_enchantment") + "  Graveyard: " + d.points(player.getUUID(), "mta_graveyard"),
                line);
    }

    /** Chat links of the Telekinetic Theatre (reset the statue, a new maze). */
    public static void theatreOptions(ServerPlayer p) {
        p.displayClientMessage(Component.literal("Maze guardian: ").withStyle(ChatFormatting.GRAY)
                .append(link("Reset", "/minigame act reset", "Put the statue back on its starting square")).append(Component.literal(" "))
                .append(link("New maze", "/minigame act newmaze", "Start another maze")), false);
    }
}
