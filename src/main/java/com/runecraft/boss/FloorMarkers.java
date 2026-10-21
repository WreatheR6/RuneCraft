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
 *   <li>only plain full blocks are marked (no block entity, no fluid, a full collision box, breakable): anything else (a lair's
 *   chest or a slab, an unloaded chunk) keeps its particle ring only.</li>
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

    /** Releases the marks whose owner left the world (killed, discarded, unloaded with its chunk). */
    public static void sweepOwners() {
        for (Mark m : new ArrayList<>(OWNED)) {
            if (m.released || m.owner.isRemoved()) {
                release(m);
            }
        }
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
        BlockPos pos = BlockPos.containing(at.x, at.y - 0.5, at.z);
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
            world.setBlock(pos, placed, Block.UPDATE_CLIENTS);
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

    /** Whether a plain full block, safe to swap and put back. */
    static boolean markable(ServerLevel world, BlockPos pos, BlockState s) {
        return !s.isAir() && !s.hasBlockEntity() && s.getFluidState().isEmpty() && s.isCollisionShapeFullBlock(world, pos)
                && s.getDestroySpeed(world, pos) >= 0;
    }

    private static void putBack(Key key, Entry e, boolean loadedNow) {
        if (!loadedNow && !e.world.isLoaded(e.pos)) {
            WAITING.put(key, e);
            return;
        }
        if (e.world.getBlockState(e.pos) == e.placed) {
            e.world.setBlock(e.pos, e.original, Block.UPDATE_CLIENTS);
        }
        State.get(e.world.getServer()).forget(key);
    }

    /** Self-test: marked blocks now. */
    public static int live() {
        return LIVE.size();
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
                    world.setBlock(pos, original, Block.UPDATE_CLIENTS);
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
