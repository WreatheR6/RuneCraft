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
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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
                !(world instanceof ServerLevel sw) || !inHouseZone(sw, pos) || player.getAbilities().instabuild);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 40 == 0) {
                clearMonsters(server);
            }
            if (server.getTickCount() % 200 == 100) {
                ensurePortals(server);
            }
        });
    }

    /**
     * Town structures are placed chunk by chunk and may flatten the spot after the portal was built;
     * re-place a missing portal whenever a player is close to its town.
     */
    private static void ensurePortals(MinecraftServer server) {
        ServerLevel world = Gielinor.world(server);
        if (world == null) {
            return;
        }
        for (ServerPlayer p : world.players()) {
            for (ExtrasContent.PortalTown t : ExtrasContent.PORTAL_TOWNS) {
                int x = GielinorMap.toBlockX(t.x());
                int z = GielinorMap.toBlockZ(t.y());
                if (Math.abs(p.getX() - x) > 48 || Math.abs(p.getZ() - z) > 48
                        || !world.getChunkSource().hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                buildPortal(world, t);
            }
        }
    }

    // ------------------------------------------------------------------ plots

    static boolean inHouseZone(ServerLevel world, BlockPos pos) {
        return Gielinor.is(world) && pos.getZ() >= BASE_Z - 16;
    }

    private static int plotIndex(MinecraftServer server, UUID player) {
        ExtrasState state = ExtrasState.get(server);
        CompoundTag plots = state.section("house_plots");
        String key = player.toString();
        if (!plots.contains(key)) {
            CompoundTag meta = state.section("house_meta");
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
        return origin.offset(cx * Poh.ROOM, 0, cz * Poh.ROOM);
    }

    static HouseLayout layout(MinecraftServer server, UUID owner) {
        CompoundTag house = ExtrasState.get(server).entry("houses", owner.toString());
        return HouseLayout.decode(house.contains("layout") ? house.getString("layout") : null);
    }

    static void save(MinecraftServer server, UUID owner, HouseLayout layout) {
        ExtrasState.get(server).entry("houses", owner.toString()).putString("layout", layout.encode());
    }

    /** The house (owner, cell, hotspot) at a block position, or null. */
    static Spot locate(ServerLevel world, BlockPos pos) {
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

    static boolean building(ServerPlayer player) {
        return BUILDING.contains(player.getUUID());
    }

    // ------------------------------------------------------------------ portals in towns

    private static void buildPortal(ServerLevel world, ExtrasContent.PortalTown t) {
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
        world.setBlockAndUpdate(new BlockPos(x, y - 1, z), Blocks.POLISHED_BLACKSTONE.defaultBlockState());
        world.setBlockAndUpdate(new BlockPos(x, y, z), portal);
        world.setBlockAndUpdate(new BlockPos(x, y + 1, z), Blocks.PURPLE_STAINED_GLASS.defaultBlockState());
    }

    private static BlockPos findPortal(ServerLevel world, BlockPos centre, int r) {
        BlockState portal = ExtrasUtil.block(ExtrasContent.HOUSE_PORTAL);
        if (portal == null) {
            return null;
        }
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-r, -r, -r), centre.offset(r, r, r))) {
            if (world.getBlockState(p).is(portal.getBlock())) {
                return p.immutable();
            }
        }
        return null;
    }

    private static boolean nearPortal(ServerPlayer player) {
        return player.getAbilities().instabuild
                || findPortal((ServerLevel) player.level(), player.blockPosition(), 5) != null;
    }

    /** The town a player's house is in (OSRS: Rimmington until moved by an estate agent). */
    static String location(MinecraftServer server, UUID owner) {
        String town = ExtrasState.get(server).entry("houses", owner.toString()).getString("location");
        return ExtrasContent.portalTown(town) == null ? ExtrasContent.START_TOWN : town;
    }

    /** The house-portal town the player is standing next to, or null. */
    private static ExtrasContent.PortalTown townHere(ServerPlayer player) {
        for (ExtrasContent.PortalTown t : ExtrasContent.PORTAL_TOWNS) {
            double dx = player.getX() - GielinorMap.toBlockX(t.x());
            double dz = player.getZ() - GielinorMap.toBlockZ(t.y());
            if (dx * dx + dz * dz <= 12 * 12) {
                return t;
            }
        }
        return null;
    }

    private static void portalOptions(ServerPlayer player) {
        ExtrasContent.PortalTown here = townHere(player);
        ExtrasContent.PortalTown home = ExtrasContent.portalTown(location(player.getServer(), player.getUUID()));
        List<MutableComponent> links = new ArrayList<>();
        if (here == null || here == home) {
            links.add(ExtrasUtil.link("Go to your house", "/house enter", "Enter your house", ChatFormatting.GOLD));
            links.add(ExtrasUtil.link("Building mode", "/house enter build", "Enter your house in building mode", ChatFormatting.GOLD));
        } else {
            Msg.game(player, "Your house is in " + home.name() + ". The estate agent can move it here.");
            links.add(ExtrasUtil.link("Move your house here - " + ExtrasUtil.coins(here.moveCost()), "/house move " + here.id(),
                    "Move your house to " + here.name() + "\nLevel " + here.level() + " Construction\n" + ExtrasUtil.coins(here.moveCost())
                            + " coins", ChatFormatting.YELLOW));
        }
        ExtrasUtil.options(player, "House portal:", links);
        Msg.game(player, "To visit a friend's house, type /house visit <name> next to the portal of the town their house is in.");
    }

    /** /house move: the estate agent relocates the player's house to the portal town they are standing at. */
    static void move(ServerPlayer player, String townId) {
        ExtrasContent.PortalTown to = ExtrasContent.portalTown(townId);
        if (to == null || (townHere(player) != to && !player.getAbilities().instabuild)) {
            Msg.game(player, "You need to be at the house portal of the town you want to move to.");
            return;
        }
        MinecraftServer server = player.getServer();
        int level = com.runecraft.player.Players.data(player).skills().level(Skill.CONSTRUCTION); // boosts don't count
        boolean quest = to.quest() == null || com.runecraft.quest.QuestApi.isComplete(player, to.quest());
        String why = ExtrasContent.canMove(to, location(server, player.getUUID()), level, quest, Inv.count(player, Inv.COINS));
        if (why != null) {
            Msg.game(player, why);
            return;
        }
        Inv.remove(player, Inv.COINS, to.moveCost());
        ExtrasState.get(server).entry("houses", player.getStringUUID()).putString("location", to.id());
        ServerNetworking.sync(player);
        Msg.action(player, "Your house has been moved to " + to.name() + ".");
    }

    // ------------------------------------------------------------------ entering and leaving

    static void enter(ServerPlayer player, UUID owner, boolean build, boolean fromPortal) {
        if (fromPortal && !nearPortal(player)) {
            Msg.game(player, "You need to be next to a house portal.");
            return;
        }
        MinecraftServer server = player.getServer();
        ServerLevel world = Gielinor.world(server);
        if (world == null) {
            return;
        }
        boolean own = owner.equals(player.getUUID());
        if (!own && !ExtrasState.get(server).has("houses", owner.toString())) {
            Msg.game(player, "That player doesn't have a house.");
            return;
        }
        if (fromPortal && !player.getAbilities().instabuild) {
            ExtrasContent.PortalTown here = townHere(player);
            ExtrasContent.PortalTown home = ExtrasContent.portalTown(location(server, owner));
            if (here != null && here != home) {
                Msg.game(player, (own ? "Your house is" : "That house is") + " in " + home.name() + ", not " + here.name() + ".");
                return;
            }
        }
        // Remember where to come back to (the portal / place the player left from).
        if (!inHouseZone(world, player.blockPosition()) || player.level() != world) {
            CompoundTag ret = ExtrasState.get(server).entry("house_return", player.getStringUUID());
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
            BUILDING.add(player.getUUID());
        } else {
            BUILDING.remove(player.getUUID());
        }
        BlockPos garden = cellCorner(origin(server, owner), HouseLayout.START_X, HouseLayout.START_Z);
        player.stopRiding();
        player.teleportTo(world, garden.getX() + 5.5, garden.getY(), garden.getZ() + 8.5, 180f, 0f);
        world.playSound(null, player.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.25f, 1.4f);
        Msg.action(player, (own ? "Welcome to your house" : "Welcome to " + nameOf(server, owner) + "'s house")
                + (BUILDING.contains(player.getUUID()) ? " (building mode)." : "."));
    }

    private static String nameOf(MinecraftServer server, UUID id) {
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        return p == null ? "a friend" : p.getName().getString();
    }

    static void leave(ServerPlayer player) {
        BUILDING.remove(player.getUUID());
        CompoundTag ret = ExtrasState.get(player.getServer()).entry("house_return", player.getStringUUID());
        ServerLevel world = Gielinor.world(player.getServer());
        if (world == null) {
            return;
        }
        Spot in = locate(world, player.blockPosition());
        if (in != null && player.level() == world) {
            // OSRS: the exit portal leads out to the portal of the town the house is in.
            ExtrasContent.PortalTown town = ExtrasContent.portalTown(location(player.getServer(), in.owner()));
            ExtrasUtil.teleport(player, town.x(), town.y() - 2);
        } else if (ret.contains("x")) {
            int x = ret.getInt("x");
            int z = ret.getInt("z");
            BlockPos pos = Gielinor.safeSpot(world, x, z);
            player.teleportTo(world, x + 0.5, Math.max(pos.getY(), ret.getInt("y")), z + 0.5, player.getYRot(), player.getXRot());
        } else {
            ExtrasContent.PortalTown rimmington = ExtrasContent.portalTown("rimmington");
            ExtrasUtil.teleport(player, rimmington.x(), rimmington.y() - 2);
        }
        Msg.game(player, "You leave the house.");
    }

    /** House teleport (tablet / spell): straight into the player's own house. */
    static void teleportHome(ServerPlayer player) {
        enter(player, player.getUUID(), false, false);
    }

    /**
     * Fairy ring code DIQ: into the player's own house beside its fairy ring (OSRS: the house ring, not the portal). Falls
     * back to the house's entrance when no ring stands on the ground floor.
     */
    static void teleportToFairyRing(ServerPlayer player) {
        enter(player, player.getUUID(), false, false);
        ServerLevel world = Gielinor.world(player.getServer());
        if (world == null || player.level() != world) {
            return;
        }
        BlockPos o = origin(player.getServer(), player.getUUID());
        int size = HouseLayout.GRID * Poh.ROOM;
        for (BlockPos p : BlockPos.betweenClosed(o, o.offset(size - 1, 3, size - 1))) {
            String id = ExtrasUtil.blockId(world.getBlockState(p));
            Poh.Furniture f = id == null ? null : Poh.byBlock(id);
            if (f == null || f.effect() != Poh.Effect.FAIRY_RING) {
                continue;
            }
            BlockPos ring = p.immutable();
            for (BlockPos side : new BlockPos[]{ring.east(), ring.west(), ring.south(), ring.north()}) {
                if (world.getBlockState(side).isAir() && world.getBlockState(side.above()).isAir()) {
                    player.teleportTo(world, side.getX() + 0.5, side.getY(), side.getZ() + 0.5, player.getYRot(), player.getXRot());
                    return;
                }
            }
            return;
        }
    }

    static void toggleBuilding(ServerPlayer player) {
        Spot here = locate((ServerLevel) player.level(), player.blockPosition());
        if (here == null || !here.owner().equals(player.getUUID())) {
            Msg.game(player, "You can only switch building mode in your own house.");
            return;
        }
        if (BUILDING.remove(player.getUUID())) {
            Msg.game(player, "Building mode is now off.");
        } else {
            BUILDING.add(player.getUUID());
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

    private static Spot ownSpot(ServerPlayer player, boolean needBuilding) {
        Spot here = locate((ServerLevel) player.level(), player.blockPosition());
        if (here == null || !here.owner().equals(player.getUUID())) {
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
    static void roomInfo(ServerPlayer player) {
        Spot here = ownSpot(player, false);
        if (here == null) {
            return;
        }
        Msg.game(player, "You are in your " + here.room().name().toLowerCase(Locale.ROOT) + " (" + here.layout().roomCount() + "/"
                + Poh.maxRooms(ExtrasUtil.level(player, Skill.CONSTRUCTION)) + " rooms).");
        List<MutableComponent> built = new ArrayList<>();
        for (Map.Entry<String, String> e : here.layout().furnitureIn(here.cx(), here.cz()).entrySet()) {
            Poh.Furniture f = Poh.furniture(e.getValue());
            if (f != null) {
                built.add(ExtrasUtil.link("Remove " + f.name(), "/house unfurnish " + here.cx() + " " + here.cz() + " " + e.getKey(),
                        "Remove it (materials are not returned)", ChatFormatting.RED));
            }
        }
        if (!built.isEmpty() && building(player)) {
            ExtrasUtil.options(player, "Furniture:", built);
        }
        if (!building(player)) {
            Msg.game(player, "Enter building mode (/house build) to add rooms and furniture.");
            return;
        }
        List<MutableComponent> dirs = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int nx = here.cx() + DIRS[i][0];
            int nz = here.cz() + DIRS[i][1];
            if (HouseLayout.inGrid(nx, nz) && here.layout().roomAt(nx, nz) == null) {
                dirs.add(ExtrasUtil.link("Build " + DIR_NAMES[i], "/house room " + DIR_NAMES[i], "Choose a room to build " + DIR_NAMES[i],
                        ChatFormatting.GOLD));
            }
        }
        if (here.cx() != HouseLayout.START_X || here.cz() != HouseLayout.START_Z) {
            dirs.add(ExtrasUtil.link("Remove this room", "/house removeroom", "Delete this room and its furniture", ChatFormatting.RED));
        }
        ExtrasUtil.options(player, "Rooms:", dirs);
    }

    /** /house room <dir>: rooms that can be built there. */
    static void roomChoices(ServerPlayer player, String dir) {
        Spot here = ownSpot(player, true);
        int d = dirIndex(dir);
        if (here == null || d < 0) {
            return;
        }
        int nx = here.cx() + DIRS[d][0];
        int nz = here.cz() + DIRS[d][1];
        int level = ExtrasUtil.level(player, Skill.CONSTRUCTION);
        List<MutableComponent> links = new ArrayList<>();
        for (Poh.Room r : Poh.ROOMS) {
            boolean ok = here.layout().canBuildRoom(r, nx, nz, level) == null && Inv.has(player, Inv.COINS, r.cost());
            links.add(ExtrasUtil.link(r.name() + " (" + r.level() + ")", "/house room " + dir + " " + r.id(),
                    r.name() + "\nLevel " + r.level() + " Construction\n" + ExtrasUtil.coins(r.cost()) + " coins",
                    ok ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        }
        ExtrasUtil.options(player, "Build a room " + dir + ":", links);
    }

    static void buildRoom(ServerPlayer player, String dir, String roomId) {
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
        save(player.getServer(), player.getUUID(), layout);
        ServerLevel world = (ServerLevel) player.level();
        BlockPos o = origin(player.getServer(), player.getUUID());
        buildCellAndNeighbours(world, o, layout, nx, nz);
        markBuilt(player.getServer(), player.getUUID(), layout);
        world.playSound(null, player.blockPosition(), SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1f, 0.8f);
        Msg.action(player, "You build " + Poh.article(room.name().toLowerCase(Locale.ROOT)) + " to the " + dir + ".");
    }

    static void removeRoom(ServerPlayer player) {
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
        save(player.getServer(), player.getUUID(), layout);
        ServerLevel world = (ServerLevel) player.level();
        BlockPos o = origin(player.getServer(), player.getUUID());
        clearCell(world, o, here.cx(), here.cz());
        buildCellAndNeighbours(world, o, layout, here.cx(), here.cz());
        markBuilt(player.getServer(), player.getUUID(), layout);
        BlockPos garden = cellCorner(o, HouseLayout.START_X, HouseLayout.START_Z);
        player.teleportTo(world, garden.getX() + 5.5, garden.getY(), garden.getZ() + 8.5, 180f, 0f);
        Msg.game(player, "You remove the room.");
    }

    // ------------------------------------------------------------------ furniture

    private static void hotspot(ServerPlayer player, BlockPos pos, BlockState state) {
        Spot spot = locate((ServerLevel) player.level(), pos);
        if (spot == null || spot.hotspot() == null) {
            Msg.game(player, "Nothing interesting happens.");
            return;
        }
        if (!spot.owner().equals(player.getUUID()) || !building(player)) {
            Msg.game(player, "A " + spot.hotspot().name().toLowerCase(Locale.ROOT) + " space. You need to be in building mode in your "
                    + "own house to build here.");
            return;
        }
        int level = ExtrasUtil.level(player, Skill.CONSTRUCTION);
        List<MutableComponent> links = new ArrayList<>();
        for (Poh.Furniture f : spot.hotspot().options()) {
            int other = f.otherSkill() == null ? 99 : ExtrasUtil.level(player, f.otherSkill());
            boolean ok = spot.layout().canBuildFurniture(spot.cx(), spot.cz(), spot.hotspot().id(), f.id(), level, other) == null
                    && ExtrasUtil.hasAll(player, f.materials());
            String hover = f.name() + "\nLevel " + f.level() + " Construction" + (f.otherSkill() == null ? ""
                    : ", " + f.otherLevel() + " " + f.otherSkill().displayName()) + "\n" + ExtrasUtil.materials(f.materials()) + "\n"
                    + f.xp() + " XP";
            links.add(ExtrasUtil.link(f.name() + " (" + f.level() + ")", "/house furnish " + spot.cx() + " " + spot.cz() + " "
                    + spot.hotspot().id() + " " + f.id(), hover, ok ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        }
        ExtrasUtil.options(player, "Build a " + spot.hotspot().name().toLowerCase(Locale.ROOT) + ":", links);
    }

    static void furnish(ServerPlayer player, int cx, int cz, String hotspotId, String furnitureId) {
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
        if (!Inv.has(player, "hammer", 1) && !player.getAbilities().instabuild) {
            Msg.game(player, "You need a hammer to build furniture.");
            return;
        }
        Map<String, Integer> missing = HouseLayout.missing(f, item -> Inv.count(player, item));
        if (!missing.isEmpty() && !player.getAbilities().instabuild) {
            Msg.game(player, "You still need: " + ExtrasUtil.materials(missing) + ".");
            return;
        }
        if (!player.getAbilities().instabuild) {
            ExtrasUtil.removeAll(player, f.materials());
        }
        layout.buildFurniture(cx, cz, hotspotId, furnitureId);
        save(player.getServer(), player.getUUID(), layout);
        ServerLevel world = (ServerLevel) player.level();
        BlockPos o = origin(player.getServer(), player.getUUID());
        placeHotspot(world, cellCorner(o, cx, cz), layout.room(cx, cz).hotspot(hotspotId), furnitureId);
        markBuilt(player.getServer(), player.getUUID(), layout);
        XpService.add(player, Skill.CONSTRUCTION, f.xp());
        if (f.otherSkill() == Skill.FARMING) {
            XpService.add(player, Skill.FARMING, f.xp()); // OSRS: house spirit trees give the same XP in Farming
        }
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        world.playSound(null, player.blockPosition(), SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1f, 1f);
        Msg.action(player, "You build " + Poh.article(f.name().toLowerCase(Locale.ROOT)) + ".");
        ServerNetworking.sync(player);
    }

    static void unfurnish(ServerPlayer player, int cx, int cz, String hotspotId) {
        Spot here = ownSpot(player, true);
        if (here == null) {
            return;
        }
        HouseLayout layout = here.layout();
        if (!layout.removeFurniture(cx, cz, hotspotId)) {
            Msg.game(player, "There's nothing built there.");
            return;
        }
        save(player.getServer(), player.getUUID(), layout);
        Poh.Room room = layout.room(cx, cz);
        if (room != null) {
            placeHotspot((ServerLevel) player.level(), cellCorner(origin(player.getServer(), player.getUUID()), cx, cz),
                    room.hotspot(hotspotId), null);
        }
        markBuilt(player.getServer(), player.getUUID(), layout);
        Msg.game(player, "You remove the furniture.");
    }

    // ------------------------------------------------------------------ physical house

    private static void markBuilt(MinecraftServer server, UUID owner, HouseLayout layout) {
        ExtrasState.get(server).entry("houses", owner.toString()).putString("built", layout.encode());
    }

    /** Builds (or brings up to date) the whole house if it doesn't match the saved layout. */
    private static void ensureBuilt(ServerLevel world, UUID owner, HouseLayout layout) {
        CompoundTag house = ExtrasState.get(world.getServer()).entry("houses", owner.toString());
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

    private static void buildCellAndNeighbours(ServerLevel world, BlockPos o, HouseLayout layout, int cx, int cz) {
        if (layout.roomAt(cx, cz) != null) {
            buildCell(world, o, layout, cx, cz);
        }
        for (int[] d : DIRS) {
            if (layout.roomAt(cx + d[0], cz + d[1]) != null) {
                buildCell(world, o, layout, cx + d[0], cz + d[1]);
            }
        }
    }

    private static void set(ServerLevel world, BlockPos pos, BlockState state) {
        world.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    private static void clearCell(ServerLevel world, BlockPos o, int cx, int cz) {
        BlockPos c = cellCorner(o, cx, cz);
        for (int dx = 0; dx < Poh.ROOM; dx++) {
            for (int dz = 0; dz < Poh.ROOM; dz++) {
                for (int y = -2; y <= 6; y++) {
                    set(world, c.offset(dx, y, dz), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static BlockState floorFor(Poh.Room room) {
        return switch (room.id()) {
            case "garden", "superior_garden", "menagerie" -> Blocks.GRASS_BLOCK.defaultBlockState();
            case "kitchen", "workshop" -> Blocks.SMOOTH_STONE.defaultBlockState();
            case "chapel" -> Blocks.POLISHED_DIORITE.defaultBlockState();
            case "dungeon", "oubliette", "treasure_room" -> Blocks.DEEPSLATE_TILES.defaultBlockState();
            case "throne_room", "achievement_gallery" -> Blocks.POLISHED_ANDESITE.defaultBlockState();
            case "portal_chamber", "portal_nexus" -> Blocks.PURPUR_BLOCK.defaultBlockState();
            case "bedroom", "costume_room" -> Blocks.SPRUCE_PLANKS.defaultBlockState();
            case "study", "dining_room" -> Blocks.DARK_OAK_PLANKS.defaultBlockState();
            default -> Blocks.OAK_PLANKS.defaultBlockState();
        };
    }

    private static BlockState wallFor(Poh.Room room) {
        return switch (room.id()) {
            case "dungeon", "oubliette", "treasure_room" -> Blocks.DEEPSLATE_BRICKS.defaultBlockState();
            case "chapel", "throne_room", "portal_chamber", "achievement_gallery" -> Blocks.STONE_BRICKS.defaultBlockState();
            default -> Blocks.STRIPPED_OAK_LOG.defaultBlockState();
        };
    }

    /** Builds one room: floor, walls (doorways towards neighbouring rooms), ceiling, hotspots and furniture. */
    private static void buildCell(ServerLevel world, BlockPos o, HouseLayout layout, int cx, int cz) {
        Poh.Room room = layout.room(cx, cz);
        if (room == null) {
            return;
        }
        BlockPos c = cellCorner(o, cx, cz);
        int last = Poh.ROOM - 1;
        BlockState floor = floorFor(room);
        BlockState wall = wallFor(room);
        BlockState hedge = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        boolean north = layout.roomAt(cx, cz - 1) != null;
        boolean south = layout.roomAt(cx, cz + 1) != null;
        boolean west = layout.roomAt(cx - 1, cz) != null;
        boolean east = layout.roomAt(cx + 1, cz) != null;
        for (int dx = 0; dx <= last; dx++) {
            for (int dz = 0; dz <= last; dz++) {
                BlockPos col = c.offset(dx, 0, dz);
                set(world, col.below(2), Blocks.STONE_BRICKS.defaultBlockState());
                set(world, col.below(), floor);
                for (int y = 0; y <= 5; y++) {
                    set(world, col.above(y), Blocks.AIR.defaultBlockState());
                }
                boolean edge = dx == 0 || dz == 0 || dx == last || dz == last;
                boolean door = (dz == 0 && north || dz == last && south) && (dx == 5 || dx == 6)
                        || (dx == 0 && west || dx == last && east) && (dz == 5 || dz == 6);
                if (edge && !door) {
                    if (room.outdoor()) {
                        set(world, col, hedge);
                        set(world, col.above(), hedge);
                        set(world, col.above(2), Blocks.BARRIER.defaultBlockState());
                        set(world, col.above(3), Blocks.BARRIER.defaultBlockState());
                    } else {
                        for (int y = 0; y <= 3; y++) {
                            set(world, col.above(y), wall);
                        }
                    }
                }
                if (!room.outdoor()) {
                    boolean light = dx % 4 == 2 && dz % 4 == 2;
                    set(world, col.above(4), light ? Blocks.GLOWSTONE.defaultBlockState() : Blocks.DARK_OAK_PLANKS.defaultBlockState());
                } else if (door && edge) {
                    set(world, col.below(), Blocks.DIRT_PATH.defaultBlockState());
                }
            }
        }
        for (Poh.Hotspot h : room.hotspots()) {
            String built = layout.furnitureAt(cx, cz, h.id());
            placeHotspot(world, c, h, built);
        }
    }

    /** Places a hotspot marker or the built furniture block at its spot in a room. */
    private static void placeHotspot(ServerLevel world, BlockPos corner, Poh.Hotspot h, String furnitureId) {
        if (h == null) {
            return;
        }
        BlockPos pos = corner.offset(h.dx(), 0, h.dz());
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
        ServerLevel world = Gielinor.world(server);
        if (world == null) {
            return;
        }
        for (ServerPlayer p : world.players()) {
            if (inHouseZone(world, p.blockPosition())) {
                for (Monster e : world.getEntitiesOfClass(Monster.class, new AABB(p.blockPosition()).inflate(96), x -> true)) {
                    e.discard();
                }
            }
        }
    }
}
