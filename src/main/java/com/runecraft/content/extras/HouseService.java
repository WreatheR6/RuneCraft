package com.runecraft.content.extras;

import com.runecraft.content.BlockHooks;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.extras.ExtrasContent;
import com.runecraft.osrs.content.extras.HouseLayout;
import com.runecraft.osrs.content.extras.Poh;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import com.runecraft.world.LandmarkBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The player-owned house: each player gets a plot far south in the Gielinor dimension
 * (z = 900,000 + plot offset) where their 9x9 room grid is built from their {@link HouseLayout}.
 * House portals in the eight OSRS house towns lead there; building mode lets the owner add rooms
 * (in the direction they face) and build furniture in hotspots with materials from their
 * inventory, for Construction XP.
 */
final class HouseService {
    static final int BASE_Z = 900_000;
    static final int PLOT = 160;
    static final int PLOTS_PER_ROW = 50;
    static final int ORIGIN_X = -4000;
    static final int FLOOR_Y = 120;

    /** Players in building mode. */
    private static final Set<UUID> BUILDING = new HashSet<>();

    private HouseService() {
    }

    /** A position inside a house: owner, layout, room cell and the hotspot there (if any). */
    record Spot(UUID owner, HouseLayout layout, int cx, int cz, Poh.Room room, Poh.Hotspot hotspot) {
    }

    static void init() {
        BlockHooks.register(ExtrasContent.HOUSE_PORTAL, (player, pos, state) -> portalOptions(player));
        BlockHooks.register(ExtrasContent.HOTSPOT, HouseService::hotspot);
        for (ExtrasContent.PortalTown t : ExtrasContent.PORTAL_TOWNS) {
            LandmarkBuilder.registerCustom(t.place(), (world, centre) -> buildPortal(world, t));
        }
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
                !(world instanceof ServerWorld sw) || !inHouseZone(sw, pos) || player.getAbilities().creativeMode);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 40 == 0) {
                clearMonsters(server);
            }
            if (server.getTicks() % 200 == 100) {
                ensurePortals(server);
            }
        });
    }

    /**
     * Town structures are placed chunk by chunk and may flatten the spot after the portal was built;
     * re-place a missing portal whenever a player is close to its town.
     */
    private static void ensurePortals(MinecraftServer server) {
        ServerWorld world = Gielinor.world(server);
        if (world == null) {
            return;
        }
        for (ServerPlayerEntity p : world.getPlayers()) {
            for (ExtrasContent.PortalTown t : ExtrasContent.PORTAL_TOWNS) {
                int x = GielinorMap.toBlockX(t.x());
                int z = GielinorMap.toBlockZ(t.y());
                if (Math.abs(p.getX() - x) > 48 || Math.abs(p.getZ() - z) > 48
                        || !world.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }
                buildPortal(world, t);
            }
        }
    }

    // ------------------------------------------------------------------ plots

    static boolean inHouseZone(ServerWorld world, BlockPos pos) {
        return Gielinor.is(world) && pos.getZ() >= BASE_Z - 16;
    }

    private static int plotIndex(MinecraftServer server, UUID player) {
        ExtrasState state = ExtrasState.get(server);
        NbtCompound plots = state.section("house_plots");
        String key = player.toString();
        if (!plots.contains(key)) {
            NbtCompound meta = state.section("house_meta");
            int idx = meta.getInt("next");
            meta.putInt("next", idx + 1);
            plots.putInt(key, idx);
            state.section("house_owners").putString(Integer.toString(idx), key);
        }
        return plots.getInt(key);
    }

    static BlockPos origin(int plot) {
        return new BlockPos(ORIGIN_X + (plot % PLOTS_PER_ROW) * PLOT, FLOOR_Y, BASE_Z + (plot / PLOTS_PER_ROW) * PLOT);
    }

    static BlockPos origin(MinecraftServer server, UUID owner) {
        return origin(plotIndex(server, owner));
    }

    /** North-west floor corner of a room cell. */
    static BlockPos cellCorner(BlockPos origin, int cx, int cz) {
        return origin.add(cx * Poh.ROOM, 0, cz * Poh.ROOM);
    }

    static HouseLayout layout(MinecraftServer server, UUID owner) {
        NbtCompound house = ExtrasState.get(server).entry("houses", owner.toString());
        return HouseLayout.decode(house.contains("layout") ? house.getString("layout") : null);
    }

    static void save(MinecraftServer server, UUID owner, HouseLayout layout) {
        ExtrasState.get(server).entry("houses", owner.toString()).putString("layout", layout.encode());
    }

    /** The house (owner, cell, hotspot) at a block position, or null. */
    static Spot locate(ServerWorld world, BlockPos pos) {
        if (!inHouseZone(world, pos)) {
            return null;
        }
        int px = Math.floorDiv(pos.getX() - ORIGIN_X, PLOT);
        int pz = Math.floorDiv(pos.getZ() - BASE_Z, PLOT);
        if (px < 0 || px >= PLOTS_PER_ROW || pz < 0) {
            return null;
        }
        int plot = pz * PLOTS_PER_ROW + px;
        String owner = ExtrasState.get(world.getServer()).section("house_owners").getString(Integer.toString(plot));
        if (owner.isEmpty()) {
            return null;
        }
        UUID uuid = UUID.fromString(owner);
        BlockPos o = origin(plot);
        int lx = pos.getX() - o.getX();
        int lz = pos.getZ() - o.getZ();
        int cx = Math.floorDiv(lx, Poh.ROOM);
        int cz = Math.floorDiv(lz, Poh.ROOM);
        if (!HouseLayout.inGrid(cx, cz)) {
            return null;
        }
        HouseLayout layout = layout(world.getServer(), uuid);
        Poh.Room room = layout.room(cx, cz);
        Poh.Hotspot hotspot = room == null ? null : room.hotspotAt(lx - cx * Poh.ROOM, lz - cz * Poh.ROOM);
        return new Spot(uuid, layout, cx, cz, room, hotspot);
    }

    static boolean building(ServerPlayerEntity player) {
        return BUILDING.contains(player.getUuid());
    }

    // ------------------------------------------------------------------ portals in towns

    private static void buildPortal(ServerWorld world, ExtrasContent.PortalTown t) {
        BlockState portal = ExtrasUtil.block(ExtrasContent.HOUSE_PORTAL);
        if (portal == null) {
            return;
        }
        int x = GielinorMap.toBlockX(t.x());
        int z = GielinorMap.toBlockZ(t.y());
        // Don't build a second portal if one is already within a few blocks.
        if (findPortal(world, new BlockPos(x, ExtrasUtil.surfaceY(world, x, z), z), 4) != null) {
            return;
        }
        int y = ExtrasUtil.surfaceY(world, x, z);
        world.setBlockState(new BlockPos(x, y - 1, z), Blocks.POLISHED_BLACKSTONE.getDefaultState());
        world.setBlockState(new BlockPos(x, y, z), portal);
        world.setBlockState(new BlockPos(x, y + 1, z), Blocks.PURPLE_STAINED_GLASS.getDefaultState());
    }

    private static BlockPos findPortal(ServerWorld world, BlockPos centre, int r) {
        BlockState portal = ExtrasUtil.block(ExtrasContent.HOUSE_PORTAL);
        if (portal == null) {
            return null;
        }
        for (BlockPos p : BlockPos.iterate(centre.add(-r, -r, -r), centre.add(r, r, r))) {
            if (world.getBlockState(p).isOf(portal.getBlock())) {
                return p.toImmutable();
            }
        }
        return null;
    }

    private static boolean nearPortal(ServerPlayerEntity player) {
        return player.getAbilities().creativeMode
                || findPortal((ServerWorld) player.getWorld(), player.getBlockPos(), 5) != null;
    }

    private static void portalOptions(ServerPlayerEntity player) {
        List<MutableText> links = new ArrayList<>();
        links.add(ExtrasUtil.link("Go to your house", "/house enter", "Enter your house", Formatting.GOLD));
        links.add(ExtrasUtil.link("Building mode", "/house enter build", "Enter your house in building mode", Formatting.GOLD));
        ExtrasUtil.options(player, "House portal:", links);
        Msg.game(player, "To visit a friend's house, type /house visit <name> next to the portal.");
    }

    // ------------------------------------------------------------------ entering and leaving

    static void enter(ServerPlayerEntity player, UUID owner, boolean build, boolean fromPortal) {
        if (fromPortal && !nearPortal(player)) {
            Msg.game(player, "You need to be next to a house portal.");
            return;
        }
        MinecraftServer server = player.getServer();
        ServerWorld world = Gielinor.world(server);
        if (world == null) {
            return;
        }
        boolean own = owner.equals(player.getUuid());
        if (!own && !ExtrasState.get(server).has("houses", owner.toString())) {
            Msg.game(player, "That player doesn't have a house.");
            return;
        }
        // Remember where to come back to (the portal / place the player left from).
        if (!inHouseZone(world, player.getBlockPos()) || player.getWorld() != world) {
            NbtCompound ret = ExtrasState.get(server).entry("house_return", player.getUuidAsString());
            if (Gielinor.is(player)) {
                ret.putInt("x", player.getBlockX());
                ret.putInt("y", player.getBlockY());
                ret.putInt("z", player.getBlockZ());
            } else {
                ret.remove("x");
            }
        }
        HouseLayout layout = layout(server, owner);
        if (own && !ExtrasState.get(server).entry("houses", owner.toString()).contains("layout")) {
            save(server, owner, layout);
            Msg.game(player, "Welcome to your new house! Use /house help to see how to build.");
        }
        ensureBuilt(world, owner, layout);
        if (build && own) {
            BUILDING.add(player.getUuid());
        } else {
            BUILDING.remove(player.getUuid());
        }
        BlockPos garden = cellCorner(origin(server, owner), HouseLayout.START_X, HouseLayout.START_Z);
        player.stopRiding();
        player.teleport(world, garden.getX() + 5.5, garden.getY(), garden.getZ() + 8.5, 180f, 0f);
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.PLAYERS, 0.25f, 1.4f);
        Msg.action(player, (own ? "Welcome to your house" : "Welcome to " + nameOf(server, owner) + "'s house")
                + (BUILDING.contains(player.getUuid()) ? " (building mode)." : "."));
    }

    private static String nameOf(MinecraftServer server, UUID id) {
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
        return p == null ? "a friend" : p.getName().getString();
    }

    static void leave(ServerPlayerEntity player) {
        BUILDING.remove(player.getUuid());
        NbtCompound ret = ExtrasState.get(player.getServer()).entry("house_return", player.getUuidAsString());
        ServerWorld world = Gielinor.world(player.getServer());
        if (world == null) {
            return;
        }
        if (ret.contains("x")) {
            int x = ret.getInt("x");
            int z = ret.getInt("z");
            BlockPos pos = Gielinor.safeSpot(world, x, z);
            player.teleport(world, x + 0.5, Math.max(pos.getY(), ret.getInt("y")), z + 0.5, player.getYaw(), player.getPitch());
        } else {
            ExtrasContent.PortalTown rimmington = ExtrasContent.portalTown("rimmington");
            ExtrasUtil.teleport(player, rimmington.x(), rimmington.y() - 2);
        }
        Msg.game(player, "You leave the house.");
    }

    /** House teleport (tablet / spell): straight into the player's own house. */
    static void teleportHome(ServerPlayerEntity player) {
        enter(player, player.getUuid(), false, false);
    }

    static void toggleBuilding(ServerPlayerEntity player) {
        Spot here = locate((ServerWorld) player.getWorld(), player.getBlockPos());
        if (here == null || !here.owner().equals(player.getUuid())) {
            Msg.game(player, "You can only switch building mode in your own house.");
            return;
        }
        if (BUILDING.remove(player.getUuid())) {
            Msg.game(player, "Building mode is now off.");
        } else {
            BUILDING.add(player.getUuid());
            Msg.game(player, "Building mode is now on. Click hotspots to build furniture; use /house room to add rooms.");
        }
    }

    // ------------------------------------------------------------------ rooms

    private static final String[] DIR_NAMES = {"north", "east", "south", "west"};
    private static final int[][] DIRS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    private static int dirIndex(String name) {
        for (int i = 0; i < DIR_NAMES.length; i++) {
            if (DIR_NAMES[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }

    private static Spot ownSpot(ServerPlayerEntity player, boolean needBuilding) {
        Spot here = locate((ServerWorld) player.getWorld(), player.getBlockPos());
        if (here == null || !here.owner().equals(player.getUuid())) {
            Msg.game(player, "You need to be in your own house to do that.");
            return null;
        }
        if (needBuilding && !building(player)) {
            Msg.game(player, "You need to be in building mode (/house build).");
            return null;
        }
        if (here.room() == null) {
            Msg.game(player, "You need to be standing in a room.");
            return null;
        }
        return here;
    }

    /** /house room: the current room, its furniture (with remove links) and where new rooms can go. */
    static void roomInfo(ServerPlayerEntity player) {
        Spot here = ownSpot(player, false);
        if (here == null) {
            return;
        }
        Msg.game(player, "You are in your " + here.room().name().toLowerCase(Locale.ROOT) + " (" + here.layout().roomCount() + "/"
                + Poh.maxRooms(ExtrasUtil.level(player, Skill.CONSTRUCTION)) + " rooms).");
        List<MutableText> built = new ArrayList<>();
        for (Map.Entry<String, String> e : here.layout().furnitureIn(here.cx(), here.cz()).entrySet()) {
            Poh.Furniture f = Poh.furniture(e.getValue());
            if (f != null) {
                built.add(ExtrasUtil.link("Remove " + f.name(), "/house unfurnish " + here.cx() + " " + here.cz() + " " + e.getKey(),
                        "Remove it (materials are not returned)", Formatting.RED));
            }
        }
        if (!built.isEmpty() && building(player)) {
            ExtrasUtil.options(player, "Furniture:", built);
        }
        if (!building(player)) {
            Msg.game(player, "Enter building mode (/house build) to add rooms and furniture.");
            return;
        }
        List<MutableText> dirs = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int nx = here.cx() + DIRS[i][0];
            int nz = here.cz() + DIRS[i][1];
            if (HouseLayout.inGrid(nx, nz) && here.layout().roomAt(nx, nz) == null) {
                dirs.add(ExtrasUtil.link("Build " + DIR_NAMES[i], "/house room " + DIR_NAMES[i], "Choose a room to build " + DIR_NAMES[i],
                        Formatting.GOLD));
            }
        }
        if (here.cx() != HouseLayout.START_X || here.cz() != HouseLayout.START_Z) {
            dirs.add(ExtrasUtil.link("Remove this room", "/house removeroom", "Delete this room and its furniture", Formatting.RED));
        }
        ExtrasUtil.options(player, "Rooms:", dirs);
    }

    /** /house room <dir>: rooms that can be built there. */
    static void roomChoices(ServerPlayerEntity player, String dir) {
        Spot here = ownSpot(player, true);
        int d = dirIndex(dir);
        if (here == null || d < 0) {
            return;
        }
        int nx = here.cx() + DIRS[d][0];
        int nz = here.cz() + DIRS[d][1];
        int level = ExtrasUtil.level(player, Skill.CONSTRUCTION);
        List<MutableText> links = new ArrayList<>();
        for (Poh.Room r : Poh.ROOMS) {
            boolean ok = here.layout().canBuildRoom(r, nx, nz, level) == null && Inv.has(player, Inv.COINS, r.cost());
            links.add(ExtrasUtil.link(r.name() + " (" + r.level() + ")", "/house room " + dir + " " + r.id(),
                    r.name() + "\nLevel " + r.level() + " Construction\n" + ExtrasUtil.coins(r.cost()) + " coins",
                    ok ? Formatting.GREEN : Formatting.DARK_GRAY));
        }
        ExtrasUtil.options(player, "Build a room " + dir + ":", links);
    }

    static void buildRoom(ServerPlayerEntity player, String dir, String roomId) {
        Spot here = ownSpot(player, true);
        int d = dirIndex(dir);
        Poh.Room room = Poh.room(roomId);
        if (here == null || d < 0) {
            return;
        }
        int nx = here.cx() + DIRS[d][0];
        int nz = here.cz() + DIRS[d][1];
        String why = here.layout().canBuildRoom(room, nx, nz, ExtrasUtil.level(player, Skill.CONSTRUCTION));
        if (why != null) {
            Msg.game(player, why);
            return;
        }
        if (!Inv.has(player, Inv.COINS, room.cost())) {
            Msg.game(player, "You need " + ExtrasUtil.coins(room.cost()) + " coins to build " + Poh.article(room.name().toLowerCase(Locale.ROOT))
                    + ".");
            return;
        }
        Inv.remove(player, Inv.COINS, room.cost());
        ServerNetworking.sync(player);
        HouseLayout layout = here.layout();
        layout.buildRoom(room, nx, nz);
        save(player.getServer(), player.getUuid(), layout);
        ServerWorld world = (ServerWorld) player.getWorld();
        BlockPos o = origin(player.getServer(), player.getUuid());
        buildCellAndNeighbours(world, o, layout, nx, nz);
        markBuilt(player.getServer(), player.getUuid(), layout);
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 1f, 0.8f);
        Msg.action(player, "You build " + Poh.article(room.name().toLowerCase(Locale.ROOT)) + " to the " + dir + ".");
    }

    static void removeRoom(ServerPlayerEntity player) {
        Spot here = ownSpot(player, true);
        if (here == null) {
            return;
        }
        String why = here.layout().canRemoveRoom(here.cx(), here.cz());
        if (why != null) {
            Msg.game(player, why);
            return;
        }
        HouseLayout layout = here.layout();
        layout.removeRoom(here.cx(), here.cz());
        save(player.getServer(), player.getUuid(), layout);
        ServerWorld world = (ServerWorld) player.getWorld();
        BlockPos o = origin(player.getServer(), player.getUuid());
        clearCell(world, o, here.cx(), here.cz());
        buildCellAndNeighbours(world, o, layout, here.cx(), here.cz());
        markBuilt(player.getServer(), player.getUuid(), layout);
        BlockPos garden = cellCorner(o, HouseLayout.START_X, HouseLayout.START_Z);
        player.teleport(world, garden.getX() + 5.5, garden.getY(), garden.getZ() + 8.5, 180f, 0f);
        Msg.game(player, "You remove the room.");
    }

    // ------------------------------------------------------------------ furniture

    private static void hotspot(ServerPlayerEntity player, BlockPos pos, BlockState state) {
        Spot spot = locate((ServerWorld) player.getWorld(), pos);
        if (spot == null || spot.hotspot() == null) {
            Msg.game(player, "Nothing interesting happens.");
            return;
        }
        if (!spot.owner().equals(player.getUuid()) || !building(player)) {
            Msg.game(player, "A " + spot.hotspot().name().toLowerCase(Locale.ROOT) + " space. You need to be in building mode in your "
                    + "own house to build here.");
            return;
        }
        int level = ExtrasUtil.level(player, Skill.CONSTRUCTION);
        List<MutableText> links = new ArrayList<>();
        for (Poh.Furniture f : spot.hotspot().options()) {
            int other = f.otherSkill() == null ? 99 : ExtrasUtil.level(player, f.otherSkill());
            boolean ok = spot.layout().canBuildFurniture(spot.cx(), spot.cz(), spot.hotspot().id(), f.id(), level, other) == null
                    && ExtrasUtil.hasAll(player, f.materials());
            String hover = f.name() + "\nLevel " + f.level() + " Construction" + (f.otherSkill() == null ? ""
                    : ", " + f.otherLevel() + " " + f.otherSkill().displayName()) + "\n" + ExtrasUtil.materials(f.materials()) + "\n"
                    + f.xp() + " XP";
            links.add(ExtrasUtil.link(f.name() + " (" + f.level() + ")", "/house furnish " + spot.cx() + " " + spot.cz() + " "
                    + spot.hotspot().id() + " " + f.id(), hover, ok ? Formatting.GREEN : Formatting.DARK_GRAY));
        }
        ExtrasUtil.options(player, "Build a " + spot.hotspot().name().toLowerCase(Locale.ROOT) + ":", links);
    }

    static void furnish(ServerPlayerEntity player, int cx, int cz, String hotspotId, String furnitureId) {
        Spot here = ownSpot(player, true);
        if (here == null) {
            return;
        }
        if (Math.abs(here.cx() - cx) > 1 || Math.abs(here.cz() - cz) > 1) {
            Msg.game(player, "You're too far away from that hotspot.");
            return;
        }
        HouseLayout layout = here.layout();
        Poh.Furniture f = Poh.furniture(furnitureId);
        int other = f == null || f.otherSkill() == null ? 99 : ExtrasUtil.level(player, f.otherSkill());
        String why = layout.canBuildFurniture(cx, cz, hotspotId, furnitureId, ExtrasUtil.level(player, Skill.CONSTRUCTION), other);
        if (why != null) {
            Msg.game(player, why);
            return;
        }
        if (!Inv.has(player, "hammer", 1) && !player.getAbilities().creativeMode) {
            Msg.game(player, "You need a hammer to build furniture.");
            return;
        }
        Map<String, Integer> missing = HouseLayout.missing(f, item -> Inv.count(player, item));
        if (!missing.isEmpty() && !player.getAbilities().creativeMode) {
            Msg.game(player, "You still need: " + ExtrasUtil.materials(missing) + ".");
            return;
        }
        if (!player.getAbilities().creativeMode) {
            ExtrasUtil.removeAll(player, f.materials());
        }
        layout.buildFurniture(cx, cz, hotspotId, furnitureId);
        save(player.getServer(), player.getUuid(), layout);
        ServerWorld world = (ServerWorld) player.getWorld();
        BlockPos o = origin(player.getServer(), player.getUuid());
        placeHotspot(world, cellCorner(o, cx, cz), layout.room(cx, cz).hotspot(hotspotId), furnitureId);
        markBuilt(player.getServer(), player.getUuid(), layout);
        XpService.add(player, Skill.CONSTRUCTION, f.xp());
        player.swingHand(net.minecraft.util.Hand.MAIN_HAND, true);
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 1f, 1f);
        Msg.action(player, "You build " + Poh.article(f.name().toLowerCase(Locale.ROOT)) + ".");
        ServerNetworking.sync(player);
    }

    static void unfurnish(ServerPlayerEntity player, int cx, int cz, String hotspotId) {
        Spot here = ownSpot(player, true);
        if (here == null) {
            return;
        }
        HouseLayout layout = here.layout();
        if (!layout.removeFurniture(cx, cz, hotspotId)) {
            Msg.game(player, "There's nothing built there.");
            return;
        }
        save(player.getServer(), player.getUuid(), layout);
        Poh.Room room = layout.room(cx, cz);
        if (room != null) {
            placeHotspot((ServerWorld) player.getWorld(), cellCorner(origin(player.getServer(), player.getUuid()), cx, cz),
                    room.hotspot(hotspotId), null);
        }
        markBuilt(player.getServer(), player.getUuid(), layout);
        Msg.game(player, "You remove the furniture.");
    }

    // ------------------------------------------------------------------ physical house

    private static void markBuilt(MinecraftServer server, UUID owner, HouseLayout layout) {
        ExtrasState.get(server).entry("houses", owner.toString()).putString("built", layout.encode());
    }

    /** Builds (or brings up to date) the whole house if it doesn't match the saved layout. */
    private static void ensureBuilt(ServerWorld world, UUID owner, HouseLayout layout) {
        NbtCompound house = ExtrasState.get(world.getServer()).entry("houses", owner.toString());
        String built = house.getString("built");
        if (built.equals(layout.encode())) {
            return;
        }
        BlockPos o = origin(world.getServer(), owner);
        if (!built.isEmpty()) {
            HouseLayout old = HouseLayout.decode(built);
            for (Integer k : old.rooms().keySet()) {
                clearCell(world, o, k / HouseLayout.GRID, k % HouseLayout.GRID);
            }
        }
        for (Integer k : layout.rooms().keySet()) {
            buildCell(world, o, layout, k / HouseLayout.GRID, k % HouseLayout.GRID);
        }
        house.putString("built", layout.encode());
    }

    private static void buildCellAndNeighbours(ServerWorld world, BlockPos o, HouseLayout layout, int cx, int cz) {
        if (layout.roomAt(cx, cz) != null) {
            buildCell(world, o, layout, cx, cz);
        }
        for (int[] d : DIRS) {
            if (layout.roomAt(cx + d[0], cz + d[1]) != null) {
                buildCell(world, o, layout, cx + d[0], cz + d[1]);
            }
        }
    }

    private static void set(ServerWorld world, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
    }

    private static void clearCell(ServerWorld world, BlockPos o, int cx, int cz) {
        BlockPos c = cellCorner(o, cx, cz);
        for (int dx = 0; dx < Poh.ROOM; dx++) {
            for (int dz = 0; dz < Poh.ROOM; dz++) {
                for (int y = -2; y <= 6; y++) {
                    set(world, c.add(dx, y, dz), Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    private static BlockState floorFor(Poh.Room room) {
        return switch (room.id()) {
            case "garden", "superior_garden", "menagerie" -> Blocks.GRASS_BLOCK.getDefaultState();
            case "kitchen", "workshop" -> Blocks.SMOOTH_STONE.getDefaultState();
            case "chapel" -> Blocks.POLISHED_DIORITE.getDefaultState();
            case "dungeon", "oubliette", "treasure_room" -> Blocks.DEEPSLATE_TILES.getDefaultState();
            case "throne_room", "achievement_gallery" -> Blocks.POLISHED_ANDESITE.getDefaultState();
            case "portal_chamber" -> Blocks.PURPUR_BLOCK.getDefaultState();
            case "bedroom", "costume_room" -> Blocks.SPRUCE_PLANKS.getDefaultState();
            case "study", "dining_room" -> Blocks.DARK_OAK_PLANKS.getDefaultState();
            default -> Blocks.OAK_PLANKS.getDefaultState();
        };
    }

    private static BlockState wallFor(Poh.Room room) {
        return switch (room.id()) {
            case "dungeon", "oubliette", "treasure_room" -> Blocks.DEEPSLATE_BRICKS.getDefaultState();
            case "chapel", "throne_room", "portal_chamber", "achievement_gallery" -> Blocks.STONE_BRICKS.getDefaultState();
            default -> Blocks.STRIPPED_OAK_LOG.getDefaultState();
        };
    }

    /** Builds one room: floor, walls (doorways towards neighbouring rooms), ceiling, hotspots and furniture. */
    private static void buildCell(ServerWorld world, BlockPos o, HouseLayout layout, int cx, int cz) {
        Poh.Room room = layout.room(cx, cz);
        if (room == null) {
            return;
        }
        BlockPos c = cellCorner(o, cx, cz);
        int last = Poh.ROOM - 1;
        BlockState floor = floorFor(room);
        BlockState wall = wallFor(room);
        BlockState hedge = Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
        boolean north = layout.roomAt(cx, cz - 1) != null;
        boolean south = layout.roomAt(cx, cz + 1) != null;
        boolean west = layout.roomAt(cx - 1, cz) != null;
        boolean east = layout.roomAt(cx + 1, cz) != null;
        for (int dx = 0; dx <= last; dx++) {
            for (int dz = 0; dz <= last; dz++) {
                BlockPos col = c.add(dx, 0, dz);
                set(world, col.down(2), Blocks.STONE_BRICKS.getDefaultState());
                set(world, col.down(), floor);
                for (int y = 0; y <= 5; y++) {
                    set(world, col.up(y), Blocks.AIR.getDefaultState());
                }
                boolean edge = dx == 0 || dz == 0 || dx == last || dz == last;
                boolean door = (dz == 0 && north || dz == last && south) && (dx == 5 || dx == 6)
                        || (dx == 0 && west || dx == last && east) && (dz == 5 || dz == 6);
                if (edge && !door) {
                    if (room.outdoor()) {
                        set(world, col, hedge);
                        set(world, col.up(), hedge);
                        set(world, col.up(2), Blocks.BARRIER.getDefaultState());
                        set(world, col.up(3), Blocks.BARRIER.getDefaultState());
                    } else {
                        for (int y = 0; y <= 3; y++) {
                            set(world, col.up(y), wall);
                        }
                    }
                }
                if (!room.outdoor()) {
                    boolean light = dx % 4 == 2 && dz % 4 == 2;
                    set(world, col.up(4), light ? Blocks.GLOWSTONE.getDefaultState() : Blocks.DARK_OAK_PLANKS.getDefaultState());
                } else if (door && edge) {
                    set(world, col.down(), Blocks.DIRT_PATH.getDefaultState());
                }
            }
        }
        for (Poh.Hotspot h : room.hotspots()) {
            String built = layout.furnitureAt(cx, cz, h.id());
            placeHotspot(world, c, h, built);
        }
    }

    /** Places a hotspot marker or the built furniture block at its spot in a room. */
    private static void placeHotspot(ServerWorld world, BlockPos corner, Poh.Hotspot h, String furnitureId) {
        if (h == null) {
            return;
        }
        BlockPos pos = corner.add(h.dx(), 0, h.dz());
        BlockState state = null;
        if (furnitureId != null) {
            Poh.Furniture f = Poh.furniture(furnitureId);
            if (f != null) {
                state = ExtrasUtil.block(f.block());
            }
        }
        if (state == null) {
            state = ExtrasUtil.block(ExtrasContent.HOTSPOT);
        }
        if (state != null) {
            set(world, pos, state);
        }
    }

    private static void clearMonsters(MinecraftServer server) {
        ServerWorld world = Gielinor.world(server);
        if (world == null) {
            return;
        }
        for (ServerPlayerEntity p : world.getPlayers()) {
            if (inHouseZone(world, p.getBlockPos())) {
                for (HostileEntity e : world.getEntitiesByClass(HostileEntity.class, new Box(p.getBlockPos()).expand(96), x -> true)) {
                    e.discard();
                }
            }
        }
    }
}
