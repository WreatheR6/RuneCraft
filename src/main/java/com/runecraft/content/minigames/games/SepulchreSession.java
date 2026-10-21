package com.runecraft.content.minigames.games;

import com.runecraft.collection.CollectionLog;
import com.runecraft.combat.PoisonService;
import com.runecraft.action.ActionManager;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.content.systems.SysState;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.minigames.HallowedSepulchre;
import com.runecraft.osrs.content.minigames.HallowedSepulchre.Challenge;
import com.runecraft.osrs.content.minigames.HallowedSepulchre.Hazard;
import com.runecraft.osrs.content.minigames.HallowedSepulchre.Kind;
import com.runecraft.osrs.content.minigames.HallowedSepulchre.Layout;
import com.runecraft.osrs.content.minigames.HallowedSepulchre.Section;
import com.runecraft.osrs.content.minigames.HallowedSepulchre.Trap;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.systems.ClueScrolls;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/**
 * The Hallowed Sepulchre (see {@link HallowedSepulchre}): five floors, one at a time, each a route picked from its wiki
 * entrances and laid out as one long crypt: trap corridors (wizard statues' flames, knight statues' swords, crossbowman statues'
 * bolts, strange tiles, priest statues' lightning, all timed by the pure rules), coffins in alcoves behind their skill challenge
 * (bridge, brazier and holy barrier, portal frame, grappling pillar), middle stairs, the platform jump and at the end the
 * stairs down and a magical obelisk (floor 5: the Grand Hallowed Coffin behind its platform).
 *
 * <p>A trap sends the player back to the start of its corridor, with damage unless they wear the hallowed ring (which also
 * shortens the recovery). The clock starts at 2 minutes, each floor descended adds 2, hallowed tokens add one; it pauses past a
 * floor's last trap. Out of time, coffins and challenges stop working and the stairs are sealed, but the obelisk still pays the
 * floor's XP. Leaving through an obelisk restores run energy with Saradomin items on (other gods' items drain it) and rolls the
 * giant squirrel.
 */
public class SepulchreSession extends MinigameSession {
    private static final int HALF_X = 8;
    private static final int HALF_Z = 78;
    private static final int HEIGHT = 6;
    /** Route position u = 0 is at relative z {@code Z0}; the route runs towards -z. */
    private static final int Z0 = HALF_Z - 2;
    /** Corridor tiles each section kind takes along the route. */
    private static final int TRAP_BUFFER = 2;
    private static final int ALCOVE_X = 4;
    private static final int COFFIN_X = 6;
    /** The attack name a trap's damage carries ({@code CombatHooks.nameAttack}): damage listeners (the self-test) read it. */
    public static final String TRAP_ATTACK = "sepulchre_trap";

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState WALL = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    private static final BlockState BARRIER = Blocks.BARRIER.defaultBlockState();

    private int floor = 1;
    private String entrance = "";
    private List<Section> sections = List.of();
    private int[] starts = new int[0];
    private Layout[] layouts = new Layout[0];
    private int endU;
    private int pauseU;
    /** Interactive blocks of the floor: position -> section index ({@code END} for the end room's stairs and obelisk). */
    private final Map<Long, Integer> blocks = new HashMap<>();
    private static final int END = -1;

    private int timeLeft = HallowedSepulchre.START_TICKS;
    private boolean paused;
    private boolean timeUp;
    private long overtime;
    private long floorStart;
    private long runStart;
    private int marksThisRun;

    // Per-floor obstacle state.
    private final boolean[] looted = new boolean[32];
    private final boolean[] done = new boolean[32];
    private final int[] crossings = new int[32];
    private final int[] portalFails = new int[32];
    private final boolean[] climbed = new boolean[32];
    private boolean grandOpen;
    private boolean grandLooted;

    // The player's movement through the traps.
    private int lastU = Integer.MIN_VALUE;
    private long recoverUntil;
    private long graceUntil;
    private int hits;

    public SepulchreSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.HALLOWED_SEPULCHRE, id, slot, world, variant);
    }

    // ------------------------------------------------------------------ entry and lobby

    /** Sins of the Father and 52 Agility (base level: boosts don't count). */
    public static String requirement(ServerPlayer player) {
        if (ContentRegistry.quest("sins_of_the_father") != null && !QuestApi.isComplete(player, "sins_of_the_father")) {
            return "A strange force keeps you out of the Sepulchre. (Complete Sins of the Father.)";
        }
        int agility = Players.data(player).skills().level(Skill.AGILITY);
        if (agility < HallowedSepulchre.FLOOR_LEVELS[0]) {
            return "You need an Agility level of " + HallowedSepulchre.FLOOR_LEVELS[0] + " to enter the Hallowed Sepulchre.";
        }
        return null;
    }

    /** The Mysterious Stranger: personal bests, then Mysterious Hallowed Goods. */
    public static void stranger(ServerPlayer player) {
        MinigameData data = MinigameData.of(player);
        StringBuilder best = new StringBuilder();
        for (int f = 1; f <= HallowedSepulchre.FLOORS; f++) {
            int t = data.stat(player.getUUID(), "sepulchre_pb_f" + f);
            best.append(f == 1 ? "" : ", ").append("floor ").append(f).append(" ").append(t > 0 ? time(t) : "-");
        }
        int total = data.stat(player.getUUID(), "sepulchre_pb_total");
        QuestApi.say(player, "Mysterious Stranger", "Hallowed marks, friend? I'll take them. Your best times: " + best
                + (total > 0 ? "; all five floors " + time(total) : "") + ". Runs: " + data.stat(player.getUUID(), "kc:hallowed_sepulchre")
                + ".");
        com.runecraft.content.minigames.RewardShopService.open(player, "hallowed_sepulchre");
    }

    /** Using a hallowed token: one more minute on the clock, only inside and only while time remains. */
    public static boolean useToken(ServerPlayer player) {
        if (!(MinigameManager.session(player) instanceof SepulchreSession s)) {
            Msg.game(player, "The Hallowed token can only be used inside the Hallowed Sepulchre minigame.");
            return false;
        }
        if (s.timeUp) {
            Msg.game(player, "The Hallowed token is unresponsive as you have run out of time.");
            return false;
        }
        s.timeLeft += HallowedSepulchre.TOKEN_TICKS;
        Msg.game(player, "The token crumbles and the Sepulchre grants you another minute.");
        return true;
    }

    /** A hallowed sack: five low, one mid and two high-tier coffin rolls. */
    public static void openSack(ServerPlayer player) {
        java.util.Random r = new java.util.Random();
        HallowedSepulchre.Tier[] tiers = HallowedSepulchre.Tier.values();
        for (int t = 0; t < tiers.length; t++) {
            for (int i = 0; i < HallowedSepulchre.SACK_ROLLS[t]; i++) {
                give(player, HallowedSepulchre.roll(tiers[t], r.nextDouble()), r);
            }
        }
        Msg.game(player, "You open the hallowed sack.");
    }

    private static void give(ServerPlayer p, HallowedSepulchre.Loot l, java.util.Random r) {
        int n = l.min() + r.nextInt(l.max() - l.min() + 1);
        if (l.doses() == 0 && MinigameManager.session(p) instanceof SepulchreSession) {
            // Coffin loot a full pack can't take goes out with the player (MinigameSession.reward).
            reward(p, l.item(), n);
            return;
        }
        ItemStack stack = Inv.stack(l.item(), n);
        if (stack.isEmpty()) {
            return;
        }
        if (l.doses() > 0) {
            stack = com.runecraft.skill.Drops.withDoses(stack, l.doses());
        }
        if (MinigameManager.session(p) instanceof SepulchreSession) {
            Inv.give(p, stack, rewardMark(p)); // a full pack's overflow goes out with the player, as reward() does
        } else {
            Inv.give(p, stack);
        }
        CollectionLog.obtain(p, l.item());
    }

    // ------------------------------------------------------------------ layout

    @Override
    protected int halfX() {
        return HALF_X;
    }

    @Override
    protected int halfZ() {
        return HALF_Z;
    }

    @Override
    protected int height() {
        return HEIGHT;
    }

    private static int length(Section s) {
        return switch (s.kind()) {
            case TRAPS -> HallowedSepulchre.LEN + TRAP_BUFFER;
            case COFFIN -> 5;
            case STAIRS -> 3;
            case UPPER_PLATFORMS -> 6;
            case PLATFORM -> 5;
            case GRAND_COFFIN -> 6;
        };
    }

    private int relZ(int u) {
        return Z0 - u;
    }

    private BlockPos at(int x, int y, int u) {
        return rel(x, y, relZ(u));
    }

    private void put(int x, int y, int u, BlockState state) {
        set(x, y, relZ(u), state);
    }

    private void mark(int x, int y, int u, String block, int section) {
        put(x, y, u, com.runecraft.content.minigames.Arena.module(block));
        blocks.put(at(x, y, u).asLong(), section);
    }

    /** Builds the current floor: picks the entrance, lays the route out and resets the floor's obstacles. */
    @Override
    protected void build() {
        List<HallowedSepulchre.Route> routes = HallowedSepulchre.routes(floor);
        HallowedSepulchre.Route route = routes.get(random.nextInt(routes.size()));
        entrance = route.entrance();
        sections = route.sections();
        starts = new int[sections.size()];
        layouts = new Layout[sections.size()];
        blocks.clear();
        java.util.Arrays.fill(looted, false);
        java.util.Arrays.fill(done, false);
        java.util.Arrays.fill(crossings, 0);
        java.util.Arrays.fill(portalFails, 0);
        java.util.Arrays.fill(climbed, false);
        grandOpen = false;
        grandLooted = false;
        paused = false;
        lastU = Integer.MIN_VALUE;

        room(Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.CHISELED_DEEPSLATE);
        // Solid rock either side of the three-lane corridor; alcoves are carved out below.
        for (int x = 2; x <= HALF_X; x++) {
            for (int z = -HALF_Z; z <= HALF_Z; z++) {
                for (int y = 1; y <= HEIGHT; y++) {
                    set(x, y, z, WALL);
                    set(-x, y, z, WALL);
                }
            }
        }
        int u = 3;
        boolean below = false;
        int lastTrap = -1;
        for (int i = 0; i < sections.size(); i++) {
            Section s = sections.get(i);
            starts[i] = u;
            if (s.kind() == Kind.STAIRS) {
                below = true;
            }
            if (s.isTrap()) {
                layouts[i] = HallowedSepulchre.layout(s, floor, below, random.nextLong());
                lastTrap = i;
            }
            buildSection(i, s, u);
            u += length(s);
        }
        endU = u;
        pauseU = lastTrap < 0 ? 0 : starts[lastTrap] + TRAP_BUFFER + HallowedSepulchre.LEN;
        // The end room: the obelisk, the stairs down (sealed on floor 5) and the back wall.
        mark(-1, 1, endU + 2, "sep_obelisk", END);
        if (floor < HallowedSepulchre.FLOORS) {
            mark(1, 1, endU + 2, "sep_stairs", END);
        }
        for (int x = -1; x <= 1; x++) {
            for (int y = 1; y <= HEIGHT; y++) {
                put(x, y, endU + 5, WALL);
            }
        }
        // The start of the floor: a wall behind the spawn.
        for (int x = -1; x <= 1; x++) {
            for (int y = 1; y <= HEIGHT; y++) {
                put(x, y, -1, WALL);
            }
        }
        floorStart = ticks;
    }

    private void buildSection(int i, Section s, int u0) {
        switch (s.kind()) {
            case TRAPS -> buildTraps(i, s, u0 + TRAP_BUFFER);
            case COFFIN -> {
                for (int x = 3; x <= HALF_X - 1; x++) {
                    for (int du = 0; du <= 4; du++) {
                        for (int y = 1; y <= 4; y++) {
                            put(x, y, u0 + du, AIR);
                        }
                    }
                }
                String obstacle = switch (s.challenge()) {
                    case BRIDGE -> "sep_bridge";
                    case BRAZIER -> "sep_brazier";
                    case PORTAL -> "sep_portal";
                    case PILLAR -> "sep_pillar";
                };
                mark(2, 1, u0 + 2, obstacle, i);
                mark(COFFIN_X, 1, u0 + 2, "sep_coffin", i);
                if (s.challenge() == Challenge.BRAZIER) {
                    // The holy barrier beside the brazier.
                    put(2, 2, u0 + 2, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
                }
            }
            case STAIRS -> {
                for (int x = -1; x <= 1; x++) {
                    for (int y = 1; y <= HEIGHT; y++) {
                        put(x, y, u0 + 1, WALL);
                    }
                }
                mark(0, 1, u0 + 1, "sep_stairs", i);
            }
            case UPPER_PLATFORMS, PLATFORM -> {
                int gap = s.kind() == Kind.PLATFORM ? 3 : 4;
                for (int du = 1; du <= gap; du++) {
                    for (int x = -1; x <= 1; x++) {
                        put(x, 0, u0 + du, AIR);
                        put(x, 1, u0 + du, BARRIER);
                        put(x, 2, u0 + du, BARRIER);
                    }
                }
                mark(-1, 1, u0, "sep_platform", i);
            }
            case GRAND_COFFIN -> mark(0, 1, u0 + 3, "sep_grand_coffin", i);
        }
    }

    private void buildTraps(int i, Section s, int t0) {
        Layout l = layouts[i];
        BlockState burn = Blocks.BLACKSTONE.defaultBlockState();
        for (int lz = 0; lz < HallowedSepulchre.LEN; lz++) {
            boolean flameRow = false;
            for (long t = 0; t < 6 && s.traps().contains(Trap.FLAME); t++) {
                flameRow |= HallowedSepulchre.flameAt(l.tierOf(Trap.FLAME), floor, lz, t);
            }
            for (int lane = -1; lane <= 1; lane++) {
                BlockState floorBlock = null;
                if (flameRow) {
                    floorBlock = burn;
                }
                if (s.traps().contains(Trap.TILES) && (lz == 3 || lz == 4 || lz == 7 || lz == 8)) {
                    boolean blue = l.tierOf(Trap.TILES) >= 2 && Math.floorMod(lane + lz, 2) == 0;
                    floorBlock = (blue ? Blocks.LAPIS_BLOCK : Blocks.YELLOW_TERRACOTTA).defaultBlockState();
                }
                if (s.traps().contains(Trap.SWORD) && lz == HallowedSepulchre.LEN - 2) {
                    floorBlock = Blocks.GILDED_BLACKSTONE.defaultBlockState();
                }
                if (floorBlock != null) {
                    put(lane, 0, t0 + lz, floorBlock);
                }
            }
            if (flameRow) {
                put(-2, 1, t0 + lz, com.runecraft.content.minigames.Arena.module("sep_wizard_statue"));
                put(2, 1, t0 + lz, com.runecraft.content.minigames.Arena.module("sep_wizard_statue"));
            }
        }
        if (s.traps().contains(Trap.SWORD)) {
            put(-2, 1, t0, com.runecraft.content.minigames.Arena.module("sep_knight_statue"));
        }
        if (s.traps().contains(Trap.LIGHTNING)) {
            put(2, 2, t0 + 1, com.runecraft.content.minigames.Arena.module("sep_priest_statue"));
        }
        if (s.traps().contains(Trap.ARROW)) {
            // The crossbowman statues stand on a ledge over the corridor's far end, one above each lane.
            for (int lane = -1; lane <= 1; lane++) {
                put(lane, 4, t0 + HallowedSepulchre.LEN, WALL);
                put(lane, 3, t0 + HallowedSepulchre.LEN, com.runecraft.content.minigames.Arena.module("sep_statue"));
            }
        }
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return at(0, 1, 1);
    }

    @Override
    protected void start() {
        runStart = ticks;
        broadcast("You enter floor 1 of the Hallowed Sepulchre by the " + entrance + " stairs. You have "
                + time(timeLeft) + " to reach the end.");
    }

    // ------------------------------------------------------------------ the clock and the traps

    @Override
    protected void gameTick() {
        if (!paused && !timeUp && timeLeft > 0) {
            timeLeft--;
            if (timeLeft == 0) {
                timeUp = true;
                broadcastRed("You have run out of time! The coffins seal shut, but you can still finish the floor.");
            }
        }
        if (timeUp && ++overtime > HallowedSepulchre.OVERTIME_TICKS) {
            broadcast("The Sepulchre's magic ejects you.");
            end(false);
            return;
        }
        for (ServerPlayer p : online()) {
            playerTick(p);
        }
        effects();
    }

    private int uOf(ServerPlayer p) {
        return Z0 - relPos(p).getZ();
    }

    private int laneOf(ServerPlayer p) {
        return relPos(p).getX();
    }

    /** The trap section containing route position u, or -1. */
    private int trapAt(int u) {
        for (int i = 0; i < sections.size(); i++) {
            if (layouts[i] != null) {
                int t0 = starts[i] + TRAP_BUFFER;
                if (u >= t0 - 1 && u <= t0 + HallowedSepulchre.LEN) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void playerTick(ServerPlayer p) {
        int u = uOf(p);
        int lane = laneOf(p);
        if (!paused && u >= pauseU) {
            paused = true;
            Msg.game(p, "You pass the last of the floor's traps. The timer pauses.");
        }
        if (Math.abs(lane) > 1 || ticks < recoverUntil) {
            lastU = u;
            return;
        }
        int i = trapAt(u);
        if (i < 0) {
            lastU = u;
            return;
        }
        int t0 = starts[i] + TRAP_BUFFER;
        int z1 = u - t0;
        int z0 = lastU == Integer.MIN_VALUE || trapAt(lastU) != i ? Math.min(z1, -1) : lastU - t0;
        lastU = u;
        Hazard h = HallowedSepulchre.hazard(layouts[i], lane, z0, z1, ticks);
        if (h == Hazard.NONE || ticks < graceUntil && h == Hazard.HIT) {
            return;
        }
        switch (h) {
            case HIT -> {
                hits++;
                boolean ring = wears(p, "hallowed_ring");
                if (!ring) {
                    // Wiki: "a small amount of damage unless the hallowed ring is equipped" (the amount is approx).
                    com.runecraft.combat.CombatHooks.nameAttack(TRAP_ATTACK, null);
                    hurt(p, HallowedSepulchre.trapDamage(floor, random.nextDouble()));
                }
                Msg.red(p, ring ? "Your hallowed ring whisks you back to the start of the obstacle."
                        : "The trap catches you and you stagger back to the start of the obstacle!");
                moveTo(p, t0 - 1, 0);
                recoverUntil = ticks + (ring ? HallowedSepulchre.RING_RECOVER_TICKS : HallowedSepulchre.RECOVER_TICKS);
                com.runecraft.content.systems.SysCombat.freeze(p, ring ? HallowedSepulchre.RING_RECOVER_TICKS
                        : HallowedSepulchre.RECOVER_TICKS);
            }
            case YELLOW -> {
                moveTo(p, Math.max(t0 - 1, u - HallowedSepulchre.TILE_JUMP), lane);
                graceUntil = ticks + HallowedSepulchre.TILE_GRACE_TICKS;
                Msg.action(p, "A yellow tile flings you backwards!");
            }
            case BLUE -> {
                moveTo(p, Math.min(t0 + HallowedSepulchre.LEN, u + HallowedSepulchre.TILE_JUMP), lane);
                graceUntil = ticks + HallowedSepulchre.TILE_GRACE_TICKS;
                Msg.action(p, "A blue tile carries you forwards.");
            }
            case STUN -> {
                // Stunned, not safe: the other traps still catch a player standing in the lightning (wiki).
                com.runecraft.content.systems.SysCombat.freeze(p, HallowedSepulchre.STUN_TICKS);
                Msg.red(p, "Lightning strikes you and you're stunned!");
            }
            default -> {
            }
        }
    }

    private void moveTo(ServerPlayer p, int u, int lane) {
        BlockPos to = at(lane, 1, u);
        p.teleportTo(world, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, p.getYRot(), p.getXRot());
        p.fallDistance = 0;
        lastU = u;
    }

    private static final ParticleOptions YELLOW = new DustParticleOptions(new Vector3f(1.0f, 0.85f, 0.2f), 1.4f);

    /** Shows the traps of the corridors around each player. */
    private void effects() {
        for (ServerPlayer p : online()) {
            int here = trapAt(uOf(p));
            int u = uOf(p);
            for (int i = 0; i < sections.size(); i++) {
                if (layouts[i] == null) {
                    continue;
                }
                int t0 = starts[i] + TRAP_BUFFER;
                if (i != here && Math.abs(u - (t0 + HallowedSepulchre.LEN / 2)) > 24) {
                    continue;
                }
                show(layouts[i], t0);
            }
        }
    }

    private void show(Layout l, int t0) {
        for (int k = 0; k < l.traps().size(); k++) {
            Trap trap = l.traps().get(k);
            int tier = l.tiers()[k];
            if (trap == Trap.SWORD) {
                int s = HallowedSepulchre.swordAt(tier, ticks);
                if (s != Integer.MIN_VALUE) {
                    BlockPos c = at(0, 1, t0 + s);
                    world.sendParticles(ParticleTypes.SWEEP_ATTACK, c.getX() + 0.5, c.getY() + 0.8, c.getZ() + 0.5, 3, 1.0, 0.3, 0.4, 0);
                }
                continue;
            }
            for (int lz = 0; lz < HallowedSepulchre.LEN; lz++) {
                for (int lane = -1; lane <= 1; lane++) {
                    ParticleOptions fx = switch (trap) {
                        case FLAME -> lane == 0 && HallowedSepulchre.flameAt(tier, floor, lz, ticks) ? ParticleTypes.FLAME : null;
                        case ARROW -> HallowedSepulchre.boltHits(tier, l.seed(), lane, lz, lz, ticks) ? ParticleTypes.END_ROD : null;
                        case TILES -> switch (HallowedSepulchre.tileAt(tier, l.seed(), lane, lz, ticks)) {
                            case YELLOW -> YELLOW;
                            case BLUE -> ParticleTypes.SOUL_FIRE_FLAME;
                            case NONE -> null;
                        };
                        case LIGHTNING -> HallowedSepulchre.lightningAt(lane, lz, ticks) ? ParticleTypes.ELECTRIC_SPARK : null;
                        default -> null;
                    };
                    if (fx == null) {
                        continue;
                    }
                    BlockPos c = at(lane, 1, t0 + lz);
                    double spread = trap == Trap.FLAME ? 1.4 : 0.2;
                    world.sendParticles(fx, c.getX() + 0.5, c.getY() + 0.4, c.getZ() + 0.5, trap == Trap.FLAME ? 10 : 3, spread, 0.3,
                            0.15, 0.01);
                }
            }
        }
    }

    // ------------------------------------------------------------------ clicks

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        Integer section = blocks.get(pos.asLong());
        if (section == null) {
            if (blockId.startsWith("sep_") && blockId.endsWith("statue")) {
                Msg.game(player, "A statue of one of Saradomin's faithful. Watch it closely.");
                return true;
            }
            return false;
        }
        if (section == END) {
            if (blockId.equals("sep_obelisk")) {
                obelisk(player);
            } else if (blockId.equals("sep_stairs")) {
                descend(player);
            }
            return true;
        }
        Section s = sections.get(section);
        switch (blockId) {
            case "sep_coffin" -> coffin(player, section, s);
            case "sep_grand_coffin" -> grandCoffin(player);
            case "sep_stairs" -> {
                moveTo(player, starts[section] + 2, 0);
                if (!climbed[section]) {
                    climbed[section] = true;
                    xp(player, Skill.AGILITY, HallowedSepulchre.MIDDLE_STAIRS_XP);
                }
                Msg.game(player, "You climb down the stairs.");
            }
            case "sep_platform" -> platform(player, section, s);
            case "sep_bridge", "sep_brazier", "sep_portal", "sep_pillar" -> challenge(player, section, s);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void platform(ServerPlayer player, int section, Section s) {
        int gap = s.kind() == Kind.PLATFORM ? 3 : 4;
        int u = uOf(player);
        boolean forward = u <= starts[section];
        moveTo(player, forward ? starts[section] + gap + 1 : starts[section], 0);
        if (forward && !climbed[section]) {
            climbed[section] = true;
            double xp = s.kind() == Kind.UPPER_PLATFORMS ? HallowedSepulchre.UPPER_PLATFORM_XP * HallowedSepulchre.UPPER_PLATFORMS
                    : HallowedSepulchre.PLATFORM_XP[floor - 1];
            xp(player, Skill.AGILITY, xp);
            if (floor == HallowedSepulchre.FLOORS && !timeUp) {
                grandOpen = true;
            }
        }
        Msg.game(player, forward ? "You leap across to the platform." : "You leap back across.");
    }

    private static boolean wears(ServerPlayer p, String id) {
        return worn(p).contains(id);
    }

    /** Item ids the player wears: armour, hands and the systems module's extra slots (ring, neck, cape...). */
    private static List<String> worn(ServerPlayer p) {
        List<String> ids = new ArrayList<>();
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
                EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
            ItemStack st = p.getItemBySlot(slot);
            if (!st.isEmpty()) {
                ids.add(Inv.idOf(st));
            }
        }
        for (ItemStack st : SysState.of(p).worn.values()) {
            if (st != null && !st.isEmpty()) {
                ids.add(Inv.idOf(st));
            }
        }
        return ids;
    }

    private boolean inAlcove(ServerPlayer p) {
        return laneOf(p) >= 3;
    }

    /**
     * A coffin's skill challenge: unlock it, then cross to the coffin and back. Out of time nothing takes the player over to a
     * coffin, a pillar included (the wiki's changelog); the way back out of the alcove stays open.
     */
    private void challenge(ServerPlayer p, int i, Section s) {
        boolean inside = inAlcove(p);
        Challenge c = s.challenge();
        if (!inside && timeUp) {
            Msg.game(p, "You've run out of time to do that.");
            return;
        }
        if (!inside && !done[i] && c != Challenge.PILLAR) {
            if (!unlock(p, i, c)) {
                return;
            }
        }
        if (c == Challenge.PILLAR) {
            if (!inside && !grapple(p)) {
                return;
            }
        }
        int u = starts[i] + 2;
        if (inside) {
            moveTo(p, u, 1);
        } else {
            BlockPos to = at(ALCOVE_X, 1, u);
            p.teleportTo(world, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, p.getYRot(), p.getXRot());
        }
        if (crossings[i] < 2) {
            crossings[i]++;
            xp(p, Skill.AGILITY, HallowedSepulchre.CROSSING_XP[floor - 1]);
        }
        Msg.game(p, switch (c) {
            case BRIDGE -> "You walk across the bridge.";
            case BRAZIER -> "You pass through the holy barrier.";
            case PORTAL -> "You step through the portal.";
            case PILLAR -> "You swing across on your grapple.";
        });
    }

    /** Grappling a pillar: a crossbow wielded and a grapple carried; the hallowed grapple never fails. */
    private boolean grapple(ServerPlayer p) {
        if (Players.data(p).skills().currentLevel(Skill.RANGED) < Challenge.PILLAR.level) {
            Msg.game(p, "You need a Ranged level of " + Challenge.PILLAR.level + " to grapple the pillar.");
            return false;
        }
        String weapon = Inv.idOf(p.getMainHandItem());
        if (!weapon.contains("crossbow") || weapon.endsWith("_u") || weapon.equals("crossbow_string")) {
            Msg.game(p, "You need to wield a crossbow to grapple the pillar.");
            return false;
        }
        boolean hallowed = Inv.has(p, "hallowed_grapple", 1) || wears(p, "hallowed_grapple");
        if (!hallowed && !Inv.has(p, "mith_grapple", 1)) {
            Msg.game(p, "You need a mith grapple to grapple the pillar.");
            return false;
        }
        if (!hallowed && random.nextDouble() >= HallowedSepulchre.pillarChance(Players.data(p).skills().currentLevel(Skill.RANGED))) {
            Msg.red(p, "Your grapple slips and you fall!");
            recoverUntil = ticks + HallowedSepulchre.RECOVER_TICKS;
            com.runecraft.content.systems.SysCombat.freeze(p, HallowedSepulchre.RECOVER_TICKS);
            return false;
        }
        return true;
    }

    /** Builds the bridge, lights the brazier or conjures the portal. */
    private boolean unlock(ServerPlayer p, int i, Challenge c) {
        var skills = Players.data(p).skills();
        switch (c) {
            case BRIDGE -> {
                int con = skills.currentLevel(Skill.CONSTRUCTION) + (Inv.has(p, "amys_saw", 1) ? 3 : 0);
                if (con < c.level) {
                    Msg.game(p, "You need a Construction level of " + c.level + " to fix the bridge.");
                    return false;
                }
                boolean hallowedHammer = Inv.has(p, "hallowed_hammer", 1);
                if (!hallowedHammer && !Inv.has(p, "hammer", 1)) {
                    Msg.game(p, "You need a hammer to fix the bridge.");
                    return false;
                }
                boolean sawExists = com.runecraft.osrs.ItemCatalog.get("saw") != null;
                if (sawExists && !Inv.has(p, "saw", 1) && !Inv.has(p, "amys_saw", 1)) {
                    Msg.game(p, "You need a saw to fix the bridge.");
                    return false;
                }
                HallowedSepulchre.BridgeKit kit = null;
                for (HallowedSepulchre.BridgeKit k : HallowedSepulchre.BRIDGE_KITS) {
                    if (Inv.has(p, k.plank(), HallowedSepulchre.BRIDGE_PLANKS) && Inv.has(p, k.nails(), HallowedSepulchre.BRIDGE_NAILS)) {
                        kit = k;
                        break;
                    }
                }
                if (kit == null) {
                    Msg.game(p, "You need 2 planks and 5 matching nails (plank and steel, oak and mithril, teak and adamant or "
                            + "mahogany and rune) to fix the bridge.");
                    return false;
                }
                if (!hallowedHammer && random.nextDouble() < HallowedSepulchre.BRIDGE_BEND_CHANCE) {
                    Inv.remove(p, kit.nails(), HallowedSepulchre.BRIDGE_NAILS);
                    Msg.red(p, "You bend the nails.");
                    return false;
                }
                Inv.remove(p, kit.plank(), HallowedSepulchre.BRIDGE_PLANKS);
                Inv.remove(p, kit.nails(), HallowedSepulchre.BRIDGE_NAILS);
                xp(p, Skill.CONSTRUCTION, kit.xp());
                Msg.game(p, "You fix the bridge.");
            }
            case BRAZIER -> {
                if (skills.level(Skill.PRAYER) < c.level) {
                    Msg.game(p, "You need a Prayer level of " + c.level + " to light the brazier.");
                    return false;
                }
                int dust = wears(p, "hallowed_symbol") ? HallowedSepulchre.BRAZIER_DUST / 2 : HallowedSepulchre.BRAZIER_DUST;
                if (!Inv.has(p, "vampyre_dust", dust)) {
                    Msg.game(p, "The brazier needs " + dust + " vampyre dust.");
                    return false;
                }
                Inv.remove(p, "vampyre_dust", dust);
                xp(p, Skill.PRAYER, HallowedSepulchre.BRAZIER_XP);
                Msg.game(p, "You offer the vampyre dust and the holy barrier comes alive.");
            }
            case PORTAL -> {
                if (portalFails[i] >= HallowedSepulchre.PORTAL_BREAK_FAILURES) {
                    Msg.game(p, "The portal frame is broken.");
                    return false;
                }
                if (SysState.of(p).book != com.runecraft.osrs.content.systems.Spellbooks.Book.STANDARD) {
                    Msg.game(p, "You're on the wrong spellbook to cast enchantment spells!");
                    return false;
                }
                boolean focus = Inv.idOf(p.getOffhandItem()).equals("hallowed_focus") || wears(p, "hallowed_focus");
                int magic = skills.currentLevel(Skill.MAGIC);
                int tier = 0;
                Map<String, Integer> cost = null;
                for (int k = 0; k < HallowedSepulchre.PORTAL_SPELLS.length; k++) {
                    int idx = focus ? k : HallowedSepulchre.PORTAL_SPELLS.length - 1 - k;
                    var spell = com.runecraft.osrs.content.systems.Spellbooks.get(HallowedSepulchre.PORTAL_SPELLS[idx]);
                    if (spell == null || magic < spell.level()) {
                        continue;
                    }
                    Map<String, Integer> plan = com.runecraft.content.systems.SpellService.runeCost(p, spell.runes());
                    if (plan != null) {
                        tier = idx + 1;
                        cost = plan;
                        break;
                    }
                }
                if (cost == null) {
                    Msg.game(p, "You don't have the runes for any enchantment spell you can cast.");
                    return false;
                }
                cost.forEach((item, n) -> Inv.remove(p, item, n));
                if (!focus && random.nextDouble() >= HallowedSepulchre.portalChance(tier)) {
                    portalFails[i]++;
                    Msg.red(p, portalFails[i] >= HallowedSepulchre.PORTAL_BREAK_FAILURES
                            ? "The enchantment fails again and the portal frame breaks!" : "You fail to conjure the portal.");
                    return false;
                }
                portalFails[i] = 0;
                xp(p, Skill.MAGIC, HallowedSepulchre.PORTAL_XP);
                Msg.game(p, "You conjure a portal with Lvl-" + tier + " Enchant.");
            }
            default -> {
                return false;
            }
        }
        done[i] = true;
        return true;
    }

    private HallowedSepulchre.Tool tool(ServerPlayer p) {
        if (Inv.has(p, "strange_old_lockpick", 1)) {
            return HallowedSepulchre.Tool.STRANGE_OLD_LOCKPICK;
        }
        return Inv.has(p, "lockpick", 1) ? HallowedSepulchre.Tool.LOCKPICK : HallowedSepulchre.Tool.NONE;
    }

    /** Searching a coffin: 66 Thieving; a failure poisons; success gives marks, the floor's table roll(s) and the tertiaries. */
    private boolean search(ServerPlayer p) {
        int thieving = Players.data(p).skills().currentLevel(Skill.THIEVING);
        if (thieving < HallowedSepulchre.COFFIN_THIEVING) {
            Msg.game(p, "You need a Thieving level of " + HallowedSepulchre.COFFIN_THIEVING + " to search the coffin.");
            return false;
        }
        if (random.nextDouble() >= HallowedSepulchre.coffinChance(thieving, tool(p))) {
            Msg.red(p, "You set off a trap in the coffin and are poisoned!");
            PoisonService.poison(p, 2, ActionManager.gameTick());
            return false;
        }
        xp(p, Skill.THIEVING, HallowedSepulchre.COFFIN_XP);
        return true;
    }

    private void coffin(ServerPlayer p, int i, Section s) {
        if (looted[i]) {
            Msg.game(p, "This coffin has already been looted.");
            return;
        }
        if (timeUp) {
            Msg.game(p, "You've run out of time to loot the coffin.");
            return;
        }
        if (!search(p)) {
            return;
        }
        looted[i] = true;
        int marks = HallowedSepulchre.marks(floor, random.nextDouble());
        reward(p, "hallowed_mark", marks);
        marksThisRun += marks;
        for (int r = 0; r < s.rolls(); r++) {
            give(p, HallowedSepulchre.roll(HallowedSepulchre.tier(floor, random.nextDouble()), random.nextDouble()), random);
        }
        if (random.nextInt(HallowedSepulchre.LOCKPICK_RATE[floor - 1]) == 0) {
            reward(p, "strange_old_lockpick", 1);
            Msg.game(p, "You find a strange old lockpick in the coffin!");
        }
        String page = HallowedSepulchre.PAGES[floor - 1];
        if (!Players.data(p).collectionLog().contains(page) && !Inv.has(p, page, 1)
                && random.nextInt(HallowedSepulchre.PAGE_RATE) == 0) {
            reward(p, page, 1);
            Msg.game(p, "You find a mysterious page tucked into the coffin.");
        }
        if (random.nextInt(HallowedSepulchre.CLUE_RATE[floor - 1]) == 0) {
            clue(p, HallowedSepulchre.CLUES[floor - 1]);
        }
        Msg.game(p, "You loot the coffin and find " + marks + " hallowed mark" + (marks == 1 ? "" : "s") + ".");
    }

    private static void clue(ServerPlayer p, String scroll) {
        ClueScrolls.Tier tier = ClueScrolls.Tier.valueOf(scroll.substring("clue_scroll_".length()).toUpperCase(java.util.Locale.ROOT));
        com.runecraft.content.systems.ClueService.giveClue(p, tier);
    }

    private void grandCoffin(ServerPlayer p) {
        if (grandLooted) {
            Msg.game(p, "The Grand Hallowed Coffin is empty.");
            return;
        }
        if (!grandOpen) {
            Msg.game(p, "You didn't reach the Grand Hallowed Coffin in time.");
            return;
        }
        if (!search(p)) {
            return;
        }
        grandLooted = true;
        int marks = HallowedSepulchre.grandMarks(random.nextDouble());
        reward(p, "hallowed_mark", marks);
        marksThisRun += marks;
        give(p, HallowedSepulchre.roll(HallowedSepulchre.Tier.HIGH, random.nextDouble()), random);
        if (random.nextInt(HallowedSepulchre.RING_RATE) == 0) {
            reward(p, "ring_of_endurance", 1);
            p.getServer().getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal(
                    p.getName().getString() + " has found a ring of endurance in the Grand Hallowed Coffin!")
                    .withStyle(net.minecraft.ChatFormatting.GOLD), false);
        }
        if (random.nextInt(HallowedSepulchre.GRAND_CLUE_RATE) == 0) {
            clue(p, "clue_scroll_elite");
        }
        Msg.game(p, "You open the Grand Hallowed Coffin and find " + marks + " hallowed marks!");
    }

    /** Records the floor's time (personal best) and pays its XP. */
    private void finishFloor(ServerPlayer p) {
        xp(p, Skill.AGILITY, HallowedSepulchre.FLOOR_XP[floor - 1]);
        MinigameData data = MinigameData.of(p);
        int took = (int) (ticks - floorStart);
        String key = "sepulchre_pb_f" + floor;
        int best = data.stat(p.getUUID(), key);
        if (best == 0 || took < best) {
            data.setStat(p.getUUID(), key, took);
            Msg.game(p, "Floor " + floor + " completed in " + time(took) + " (new personal best).");
        } else {
            Msg.game(p, "Floor " + floor + " completed in " + time(took) + ". Personal best: " + time(best) + ".");
        }
        if (floor > data.stat(p.getUUID(), "sepulchre_floor")) {
            data.setStat(p.getUUID(), "sepulchre_floor", floor);
        }
    }

    private void descend(ServerPlayer p) {
        if (floor >= HallowedSepulchre.FLOORS) {
            Msg.game(p, "These stairs are fenced off.");
            return;
        }
        if (timeUp) {
            Msg.game(p, "The stairs are sealed: you ran out of time. Use the obelisk to leave.");
            return;
        }
        int need = HallowedSepulchre.FLOOR_LEVELS[floor];
        if (level(p, Skill.AGILITY) < need) {
            Msg.game(p, "You need an Agility level of " + need + " to go any deeper. Use the obelisk to leave.");
            return;
        }
        finishFloor(p);
        floor++;
        timeLeft += HallowedSepulchre.FLOOR_TICKS;
        build();
        for (ServerPlayer q : online()) {
            moveTo(q, 1, 0);
        }
        broadcast("You descend to floor " + floor + " by the " + entrance + " stairs. Time left: " + time(timeLeft) + ".");
    }

    /** Leaving through a magical obelisk: the floor's XP, run energy by the gods' items worn, the pet roll. */
    private void obelisk(ServerPlayer p) {
        finishFloor(p);
        List<String> worn = worn(p);
        com.runecraft.content.systems.SysState.SysPlayer sp = SysState.of(p);
        if (com.runecraft.osrs.content.godwars.GodItems.wears("zamorak", worn)) {
            com.runecraft.content.systems.RunEnergyService.restore(p, -sp.runEnergy);
            hurt(p, 3);
            Msg.red(p, "The obelisk reacts violently to your Zamorakian items.");
        } else if (com.runecraft.osrs.content.godwars.GodItems.wears("armadyl", worn)
                || com.runecraft.osrs.content.godwars.GodItems.wears("bandos", worn)) {
            com.runecraft.content.systems.RunEnergyService.restore(p, -Math.min(sp.runEnergy, 2500));
            hurt(p, 1);
            Msg.red(p, "The obelisk rejects your other gods' items.");
        } else if (com.runecraft.osrs.content.godwars.GodItems.wears("saradomin", worn)) {
            com.runecraft.content.systems.RunEnergyService.restore(p, 10000);
            Msg.game(p, "The obelisk restores your energy.");
        }
        if (random.nextInt(HallowedSepulchre.PET_RATE[floor - 1]) == 0) {
            reward(p, HallowedSepulchre.PET, 1);
            Msg.game(p, "You have a funny feeling like you're being followed.");
        }
        if (floor == HallowedSepulchre.FLOORS) {
            MinigameData data = MinigameData.of(p);
            int total = (int) (ticks - runStart);
            int best = data.stat(p.getUUID(), "sepulchre_pb_total");
            if (best == 0 || total < best) {
                data.setStat(p.getUUID(), "sepulchre_pb_total", total);
            }
        }
        end(true);
    }

    @Override
    protected void onEnd(boolean success) {
        for (ServerPlayer p : online()) {
            MinigameData.of(p).incrementStat(p.getUUID(), "kc:" + game.id());
            if (marksThisRun > 0) {
                Msg.game(p, "You leave the Sepulchre with " + marksThisRun + " hallowed marks from this run.");
            }
        }
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        String clock = timeUp ? "Out of time" : paused ? "Timer paused (" + time(timeLeft) + ")" : "Time left: " + time(timeLeft);
        return List.of("Hallowed Sepulchre", "Floor: " + floor + " / " + HallowedSepulchre.FLOORS, clock,
                "Hallowed marks this run: " + marksThisRun);
    }

    // ------------------------------------------------------------------ self-test hooks

    /** One section of the floor as the self-test walks it. */
    public record SelfTestSection(Section section, int u0, BlockPos block, BlockPos coffin, Layout layout, int trapStart) {
    }

    public List<SelfTestSection> selfTestSections() {
        List<SelfTestSection> out = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) {
            Section s = sections.get(i);
            int u0 = starts[i];
            BlockPos block = switch (s.kind()) {
                case COFFIN -> at(2, 1, u0 + 2);
                case STAIRS -> at(0, 1, u0 + 1);
                case UPPER_PLATFORMS, PLATFORM -> at(-1, 1, u0);
                case GRAND_COFFIN -> at(0, 1, u0 + 3);
                case TRAPS -> null;
            };
            BlockPos coffin = s.kind() == Kind.COFFIN ? at(COFFIN_X, 1, u0 + 2) : null;
            out.add(new SelfTestSection(s, u0, block, coffin, layouts[i], u0 + TRAP_BUFFER));
        }
        return out;
    }

    /** Where a player stands at route position u in a lane (-1..1), or in a coffin alcove (lane {@code ALCOVE_X}). */
    public BlockPos selfTestSpot(int u, int lane) {
        return at(lane, 1, u);
    }

    public static int selfTestAlcoveLane() {
        return ALCOVE_X;
    }

    public BlockPos selfTestObelisk() {
        return at(-1, 1, endU + 2);
    }

    public BlockPos selfTestEndStairs() {
        return at(1, 1, endU + 2);
    }

    public int selfTestEndU() {
        return endU;
    }

    public int selfTestFloor() {
        return floor;
    }

    public int selfTestTimeLeft() {
        return timeLeft;
    }

    public boolean selfTestPaused() {
        return paused;
    }

    public boolean selfTestTimeUp() {
        return timeUp;
    }

    /** Sets the clock (the out-of-time walk). */
    public void selfTestSetTime(int gameTicks) {
        timeLeft = gameTicks;
    }

    public long selfTestTicks() {
        return ticks;
    }

    public int selfTestMarks() {
        return marksThisRun;
    }

    public int selfTestHits() {
        return hits;
    }

    public String selfTestEntrance() {
        return entrance;
    }

    public boolean selfTestLooted(int section) {
        return looted[section];
    }

    public boolean selfTestGrandLooted() {
        return grandLooted;
    }

    /** The route position and lane the session reads for a player (what the trap checks see). */
    public int[] selfTestWhere(ServerPlayer p) {
        return new int[]{uOf(p), laneOf(p)};
    }

    /** Block in the arena at an absolute position, for the walk's assertions. */
    public Block selfTestBlock(BlockPos pos) {
        return world.getBlockState(pos).getBlock();
    }
}
