package com.runecraft.boss;

import com.runecraft.platform.PlayerBlockBreakEvents;
import com.runecraft.platform.ServerChunkEvents;
import com.runecraft.platform.ServerLifecycleEvents;
import com.runecraft.platform.ServerTickEvents;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

/**
 * Temporary coloured floor blocks under ground hazards and boss floor marks (round 42, // playability: OSRS tints the tiles; a
 * few particles are easy to miss in real time). The floor block under a mark is swapped for its {@link HazardKind}'s block and
 * put back when the mark is released: when its hazard expires or is cleared, when its boss dies or leaves, when the server
 * stops. Never left behind:
 * <ul>
 *   <li>the original block is written to the world save before it changes, so a crash is undone when the server starts again;</li>
 *   <li>a mark released while its chunk is unloaded is put back as soon as the chunk loads;</li>
 *   <li>a block is only put back while it is still the mark (a lair rebuilt or cleared meanwhile keeps what it has), and marked
 *   blocks can't be broken;</li>
 *   <li>only plain full blocks are marked (no block entity, no fluid, a full collision box, breakable, not magma, which is a hazard
 *   floor already): anything else (a lair's chest or a slab, molten sand, an unloaded chunk) keeps its particle ring only.</li>
 * </ul>
 * Several marks on one block share it (the first one's colour shows) and it goes back when the last is released.
 */
public final class FloorMarkers {
    private record Key(ResourceKey<Level> dimension, long pos) {
    }

    /** A marked floor block, shared by every mark on it. */
    private static final class Entry {
        final ServerLevel world;
        final BlockPos pos;
        final BlockState original;
        final BlockState placed;
        int refs;

        Entry(ServerLevel world, BlockPos pos, BlockState original, BlockState placed) {
            this.world = world;
            this.pos = pos;
            this.original = original;
            this.placed = placed;
        }
    }

    /** One mark on a floor block: release it when its hazard or mark ends. */
    public static final class Mark {
        private final Entry entry;
        /** The boss or monster it belongs to: released when that leaves the world (unloaded with its chunk too). */
        private final Entity owner;
        /** The server session it was placed in: a mark from before the server last stopped is released already. */
        private final int session = FloorMarkers.session;
        private boolean released;

        private Mark(Entry entry, Entity owner) {
            this.entry = entry;
            this.owner = owner;
        }

        public BlockPos pos() {
            return entry.pos;
        }

        public boolean released() {
            return released || session != FloorMarkers.session;
        }
    }

    /**
     * How a mark swaps a block: sent to the clients, with no neighbour or shape updates (round 43: a flower or tall grass on a
     * marked block would otherwise pop off the concrete, and is never put back).
     */
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private static final Map<Key, Entry> LIVE = new HashMap<>();
    /** Released while their chunk was unloaded: put back when it loads. */
    private static final Map<Key, Entry> WAITING = new HashMap<>();
    /** Marks with an owner, checked every second for an owner that left the world. */
    private static final List<Mark> OWNED = new ArrayList<>();
    /**
     * Bumped when the server stops. Every block went back then, so a mark from an earlier session (a hazard or script still
     * holding it in the same JVM: single player quit and reopened) is released already and releasing it does nothing: it can't
     * drop a newer mark's block or park the old, closed world's block as waiting.
     */
    private static int session;
    private static boolean registered;

    private FloorMarkers() {
    }

    /** Hooks: leftovers of a crash put back on start, every mark put back on stop, waiting ones on chunk load, no breaking. */
    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        ServerLifecycleEvents.SERVER_STARTED.register(FloorMarkers::restoreLeftovers);
        ServerLifecycleEvents.SERVER_STOPPING.register(FloorMarkers::restoreAll);
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (WAITING.isEmpty()) {
                return;
            }
            Iterator<Map.Entry<Key, Entry>> it = WAITING.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Key, Entry> e = it.next();
                Entry entry = e.getValue();
                if (entry.world == world && (entry.pos.getX() >> 4) == chunk.getPos().x && (entry.pos.getZ() >> 4) == chunk.getPos().z) {
                    it.remove();
                    putBack(e.getKey(), entry, true);
                }
            }
        });
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
                !(world instanceof ServerLevel sl) || !LIVE.containsKey(new Key(sl.dimension(), pos.asLong())));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!OWNED.isEmpty() && server.getTickCount() % 20 == 0) {
                sweepOwners();
            }
        });
    }

    /**
     * Releases the marks whose owner left the world (discarded, unloaded with its chunk) or died (round 43: a boss's marks go
     * with its death, not only when its body is removed a second later).
     */
    public static void sweepOwners() {
        for (Mark m : new ArrayList<>(OWNED)) {
            if (m.released || gone(m.owner)) {
                release(m);
            }
        }
    }

    /** Whether a mark's owner has left the fight: removed from the world, or dead. */
    private static boolean gone(Entity owner) {
        return owner != null && (owner.isRemoved() || owner instanceof net.minecraft.world.entity.LivingEntity le && le.isDeadOrDying());
    }

    /** {@link #place(ServerLevel, Vec3, HazardKind, Entity)} with no owner: its caller releases it. */
    public static Mark place(ServerLevel world, Vec3 at, HazardKind kind) {
        return place(world, at, kind, null);
    }

    /**
     * Marks the floor block under {@code at} (the block just below that point) with {@code kind}'s block; returns the mark, or
     * null when that block can't be marked (see the class notes). A mark with an {@code owner} is also released when the owner
     * leaves the world.
     */
    public static Mark place(ServerLevel world, Vec3 at, HazardKind kind, Entity owner) {
        if (world == null || at == null || kind == null) {
            return null;
        }
        return placeBlock(world, BlockPos.containing(at.x, at.y - 0.5, at.z), kind, owner);
    }

    /**
     * Marks the floor block at {@code pos} itself with {@code kind}'s block (see {@link #place(ServerLevel, Vec3, HazardKind,
     * Entity)}); null when it can't be marked, or when its owner has already left the fight.
     */
    public static Mark placeBlock(ServerLevel world, BlockPos pos, HazardKind kind, Entity owner) {
        if (world == null || pos == null || kind == null || gone(owner)) {
            return null;
        }
        pos = pos.immutable();
        Key key = new Key(world.dimension(), pos.asLong());
        Entry e = LIVE.get(key);
        if (e == null) {
            if (WAITING.containsKey(key) || !world.isLoaded(pos)) {
                return null;
            }
            BlockState original = world.getBlockState(pos);
            BlockState placed = kind.block.defaultBlockState();
            if (!markable(world, pos, original) || original.is(kind.block)) {
                return null;
            }
            e = new Entry(world, pos, original, placed);
            State.get(world.getServer()).record(key, original, placed); // in the save before the world changes
            world.setBlock(pos, placed, FLAGS);
            LIVE.put(key, e);
        }
        e.refs++;
        Mark m = new Mark(e, owner);
        if (owner != null) {
            OWNED.add(m);
        }
        return m;
    }

    /** Releases a mark (null and repeats are ignored); the block goes back when no other mark holds it. */
    public static void release(Mark m) {
        if (m == null) {
            return;
        }
        if (m.owner != null) {
            OWNED.remove(m);
        }
        if (m.released()) {
            m.released = true;
            return;
        }
        m.released = true;
        Entry e = m.entry;
        if (--e.refs > 0) {
            return;
        }
        Key key = new Key(e.world.dimension(), e.pos.asLong());
        LIVE.remove(key);
        putBack(key, e, false);
    }

    /**
     * The floor block of column ({@code x}, {@code z}) near feet height {@code y} (round 43): the solid block a player standing
     * there at about that height stands on, looking one block down and up for a step, or null (a hole, a wall).
     */
    public static BlockPos floorAt(ServerLevel world, int x, int z, double y) {
        int fy = (int) Math.floor(y - 0.5);
        for (int dy : new int[]{0, -1, 1, -2}) {
            BlockPos pos = new BlockPos(x, fy + dy, z);
            if (!world.isLoaded(pos)) {
                return null;
            }
            BlockState s = world.getBlockState(pos);
            BlockPos above = pos.above();
            if (!s.isAir() && s.getFluidState().isEmpty() && world.getBlockState(above).getCollisionShape(world, above).isEmpty()) {
                return pos;
            }
        }
        return null;
    }

    /**
     * Floor blocks to mark, each in its kind, built from shapes on the floor ({@link com.runecraft.osrs.FloorShapes}) at a
     * height; a block added twice keeps the later kind (add a safe area after the danger around it).
     */
    public static final class Shape {
        private final ServerLevel world;
        private final Map<BlockPos, HazardKind> blocks = new java.util.LinkedHashMap<>();

        public Shape(ServerLevel world) {
            this.world = world;
        }

        /** The columns {@code {x, z}} on the floor near feet height {@code y}. */
        public Shape columns(List<int[]> columns, double y, HazardKind kind) {
            for (int[] c : columns) {
                BlockPos f = floorAt(world, c[0], c[1], y);
                if (f != null) {
                    blocks.put(f, kind);
                }
            }
            return this;
        }

        /** A strike's disc: the floor round {@code centre} within {@code radius} ({@link com.runecraft.osrs.FloorShapes#disc}). */
        public Shape disc(Vec3 centre, double radius, HazardKind kind) {
            return columns(com.runecraft.osrs.FloorShapes.disc(centre.x, centre.z, radius), centre.y, kind);
        }

        /** A ring between two radii round {@code centre}. */
        public Shape ring(Vec3 centre, double inner, double outer, HazardKind kind) {
            return columns(com.runecraft.osrs.FloorShapes.annulus(centre.x, centre.z, inner, outer), centre.y, kind);
        }

        /** The square area {@code half} blocks either side of {@code centre}. */
        public Shape box(Vec3 centre, double half, HazardKind kind) {
            return columns(com.runecraft.osrs.FloorShapes.box(centre.x, centre.z, half), centre.y, kind);
        }

        /** A path {@code halfWidth} blocks either side of the line from {@code from} to {@code to}. */
        public Shape line(Vec3 from, Vec3 to, double halfWidth, HazardKind kind) {
            return columns(com.runecraft.osrs.FloorShapes.line(from.x, from.z, to.x, to.z, halfWidth), from.y, kind);
        }

        /** One floor block as it is (a tile a script tracks by block). */
        public Shape block(BlockPos floor, HazardKind kind) {
            if (floor != null && kind != null) {
                blocks.put(floor.immutable(), kind);
            }
            return this;
        }

        public Map<BlockPos, HazardKind> blocks() {
            return blocks;
        }

        public boolean isEmpty() {
            return blocks.isEmpty();
        }
    }

    /**
     * A set of floor marks one mechanic shows and changes as it goes (round 43): {@link #show} swaps the marked blocks for a new
     * set (marks no longer wanted go back, new ones are placed, a block whose kind changed is re-marked), {@link #clear} puts them
     * all back. Its marks carry the owner, so they also go when the owner dies or leaves the world, and they go back when the
     * server stops like every mark. A one-off mark set (a strike's disc) is a layer shown once and cleared when it lands.
     */
    public static final class Layer {
        private final Entity owner;
        private final Map<BlockPos, Mark> marks = new HashMap<>();
        private final Map<BlockPos, HazardKind> kinds = new HashMap<>();
        private ServerLevel world;

        public Layer(Entity owner) {
            this.owner = owner;
        }

        /** Shows a shape's blocks (and only those). */
        public void show(Shape shape) {
            show(shape.world, shape.blocks);
        }

        /** Shows exactly these floor blocks, each in its kind (empty: none). */
        public void show(ServerLevel world, Map<BlockPos, HazardKind> want) {
            if (this.world != world) {
                clear();
                this.world = world;
            }
            Iterator<Map.Entry<BlockPos, Mark>> it = marks.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<BlockPos, Mark> e = it.next();
                HazardKind k = want.get(e.getKey());
                if (e.getValue().released() || k != kinds.get(e.getKey())) {
                    release(e.getValue());
                    kinds.remove(e.getKey());
                    it.remove();
                }
            }
            if (world == null) {
                return;
            }
            for (Map.Entry<BlockPos, HazardKind> e : want.entrySet()) {
                if (marks.containsKey(e.getKey())) {
                    continue;
                }
                Mark m = placeBlock(world, e.getKey(), e.getValue(), owner);
                if (m != null) {
                    marks.put(e.getKey(), m);
                    kinds.put(e.getKey(), e.getValue());
                }
            }
        }

        /** Puts every block back. */
        public void clear() {
            for (Mark m : marks.values()) {
                release(m);
            }
            marks.clear();
            kinds.clear();
        }

        /** Blocks marked now (self-test). */
        public int size() {
            int n = 0;
            for (Mark m : marks.values()) {
                if (!m.released()) {
                    n++;
                }
            }
            return n;
        }

        /** Whether this layer marks the floor block {@code pos} now (self-test). */
        public boolean marks(BlockPos pos) {
            Mark m = marks.get(pos);
            return m != null && !m.released();
        }
    }

    /** Shows {@code shape} on a new layer owned by {@code owner} (null when it marks nothing): a one-off mark set. */
    public static Layer show(Shape shape, Entity owner) {
        Layer l = new Layer(owner);
        l.show(shape);
        return l.marks.isEmpty() ? null : l;
    }

    /** Whether a plain full block, safe to swap and put back. */
    static boolean markable(ServerLevel world, BlockPos pos, BlockState s) {
        // magma is a hazard floor itself (molten sand, burning walls): it keeps showing what it is
        return !s.isAir() && !s.hasBlockEntity() && !s.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK) && s.getFluidState().isEmpty() && s.isCollisionShapeFullBlock(world, pos)
                && s.getDestroySpeed(world, pos) >= 0;
    }

    private static void putBack(Key key, Entry e, boolean loadedNow) {
        if (!loadedNow && !e.world.isLoaded(e.pos)) {
            WAITING.put(key, e);
            return;
        }
        if (e.world.getBlockState(e.pos) == e.placed) {
            e.world.setBlock(e.pos, e.original, FLAGS);
        }
        State.get(e.world.getServer()).forget(key);
    }

    /** Self-test: marked blocks now. */
    public static int live() {
        return LIVE.size();
    }

    /** Self-test: blocks marked now with {@code kind}'s block. */
    public static int live(HazardKind kind) {
        int n = 0;
        for (Entry e : LIVE.values()) {
            if (e.placed.is(kind.block)) {
                n++;
            }
        }
        return n;
    }

    /** Self-test: whether a block is marked now. */
    public static boolean marked(ServerLevel world, BlockPos pos) {
        return LIVE.containsKey(new Key(world.dimension(), pos.asLong()));
    }

    /** Server stopping: every mark goes back (one in an unloaded chunk stays in the save for the next start). */
    private static void restoreAll(MinecraftServer server) {
        endSession();
    }

    /** Self-test: what the server stopping does to the marks (every block put back, every mark held anywhere released). */
    public static void stopForSelfTest() {
        endSession();
    }

    private static void endSession() {
        List<Map.Entry<Key, Entry>> all = new ArrayList<>(LIVE.entrySet());
        all.addAll(WAITING.entrySet());
        LIVE.clear();
        WAITING.clear();
        session++; // every mark placed so far, owned or not, counts as released from now on
        OWNED.clear();
        for (Map.Entry<Key, Entry> e : all) {
            Entry entry = e.getValue();
            if (entry.world.isLoaded(entry.pos)) {
                putBack(e.getKey(), entry, true);
            }
        }
    }

    /** Server started: marks the save still lists (the server stopped without putting them back) go back now. */
    private static void restoreLeftovers(MinecraftServer server) {
        State state = State.get(server);
        if (state.marks.isEmpty()) {
            return;
        }
        HolderLookup<Block> blocks = server.registryAccess().lookupOrThrow(Registries.BLOCK);
        for (Map.Entry<String, CompoundTag> e : new ArrayList<>(state.marks.entrySet())) {
            String[] parts = e.getKey().split("\\|", 2);
            ServerLevel world = parts.length == 2
                    ? server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(parts[0]))) : null;
            if (world != null) {
                BlockPos pos = BlockPos.of(Long.parseLong(parts[1]));
                BlockState original = NbtUtils.readBlockState(blocks, e.getValue().getCompound("Original"));
                BlockState placed = NbtUtils.readBlockState(blocks, e.getValue().getCompound("Placed"));
                if (world.getBlockState(pos) == placed) {
                    world.setBlock(pos, original, FLAGS);
                }
            }
            state.marks.remove(e.getKey());
            state.setDirty();
        }
    }

    /** The world save's list of marked blocks and what they were. */
    static final class State extends SavedData {
        static final String KEY = "runecraft_floor_markers";
        final Map<String, CompoundTag> marks = new HashMap<>();

        static State get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(State::new, State::fromNbt, null), KEY);
        }

        private static String id(Key key) {
            return key.dimension().location() + "|" + key.pos();
        }

        void record(Key key, BlockState original, BlockState placed) {
            CompoundTag t = new CompoundTag();
            t.put("Original", NbtUtils.writeBlockState(original));
            t.put("Placed", NbtUtils.writeBlockState(placed));
            marks.put(id(key), t);
            setDirty();
        }

        void forget(Key key) {
            if (marks.remove(id(key)) != null) {
                setDirty();
            }
        }

        @Override
        public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
            marks.forEach(nbt::put);
            return nbt;
        }

        static State fromNbt(CompoundTag nbt, HolderLookup.Provider registries) {
            State s = new State();
            for (String k : nbt.getAllKeys()) {
                s.marks.put(k, nbt.getCompound(k));
            }
            return s;
        }
    }
}
