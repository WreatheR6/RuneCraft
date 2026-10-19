package com.runecraft.content.dungeons;

import com.runecraft.content.BlockHooks;
import com.runecraft.content.ModuleHooks;
import com.runecraft.combat.CombatHandler;
import com.runecraft.network.Net;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.CommonItems;
import com.runecraft.osrs.content.dungeons.DungeonBlocks;
import com.runecraft.osrs.content.dungeons.Dungeons;
import com.runecraft.osrs.content.dungeons.DungeonsContent;
import com.runecraft.osrs.content.dungeons.ShadeCatacombs;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/**
 * Minecraft-side behaviour for the dungeons module: the monster camps inside every dungeon, the
 * Stronghold of Security's doors and reward chests, Brimhaven's vines, the Mining Guild door and
 * the Abyss's passages and rifts, the Barrows crypts and tunnels ({@link BarrowsHooks}), the Waterbirth Island
 * Dungeon's circular doors, door-supports, Kings' ladder and Lighthouse ladder, and Ape Atoll's traps and falling rocks.
 */
public final class DungeonsHooks implements ModuleHooks {
    /** Security advice spoken by the Stronghold of Security's doors (one is picked per use). */
    private static final List<String> SECURITY_TIPS = List.of(
            "Never tell anyone your password, not even a Jagex moderator.",
            "Nobody will ever ask for your password in exchange for items or membership.",
            "Use a password you don't use anywhere else.",
            "Turn on two-factor authentication to protect your account.",
            "Don't download programs that promise free gold or items.",
            "Be careful of links in messages: official sites are the only place to log in.",
            "Recovery details are only for you: never share them.",
            "Trading your account is against the rules and can get it stolen.");

    @Override
    public void init() {
        for (int i = 0; i < DungeonBlocks.SOS_DOORS.size(); i++) {
            BlockHooks.register(DungeonBlocks.SOS_DOORS.get(i), DungeonsHooks::securityDoor);
        }
        for (int i = 0; i < DungeonBlocks.SOS_CHESTS.size(); i++) {
            final int chest = i;
            BlockHooks.register(DungeonBlocks.SOS_CHESTS.get(i), (player, pos, state) -> rewardChest(player, chest));
        }
        EssenceMineService.register();
        BlockHooks.register(DungeonBlocks.VINES, DungeonsHooks::vines);
        BlockHooks.register(DungeonBlocks.MINING_GUILD_DOOR, DungeonsHooks::miningGuildDoor);
        BlockHooks.register(DungeonBlocks.ABYSS_PASSAGE, (player, pos, state) -> {
            Msg.game(player, "You squeeze through the passage...");
            passThrough(player, pos);
        });
        BlockHooks.register(DungeonBlocks.RIFT, DungeonsHooks::rift);
        for (String metal : CommonItems.SHADE_KEY_METALS) {
            BlockHooks.register(DungeonBlocks.shadeDoor(metal), DungeonsHooks::shadeDoor);
        }
        BlockHooks.register(DungeonBlocks.KINGS_LADDER, DungeonsHooks::kingsLadder);
        BlockHooks.register(DungeonBlocks.CIRCULAR_DOOR, DungeonsHooks::circularDoor);
        BlockHooks.register(DungeonBlocks.DOOR_SUPPORT, DungeonsHooks::doorSupport);
        BlockHooks.register(DungeonBlocks.LIGHTHOUSE_LADDER, DungeonsHooks::lighthouseLadder);
        BlockHooks.register(DungeonBlocks.ROCK_PILE, DungeonsHooks::rockPile);
        for (String block : DungeonBlocks.PASSAGES) {
            BlockHooks.register(block, DungeonsHooks::passage);
        }
        com.runecraft.world.LandmarkBuilder.registerCustom("troll_stronghold", (world, centre) -> placeTrollPrisoners(world, centre));
        DungeonCamps.register();
        BarrowsHooks.register();
        // Task-only dungeons (the Kalphite Cave): no damage to their monsters without the matching Slayer task, whatever
        // the damage path (melee, ranged, spells, specials, the cannon, recoil).
        com.runecraft.combat.CombatHooks.addMonsterDamageGate(DungeonsHooks::taskOnlyDenied);
        BlockHooks.register(DungeonBlocks.MARIMBO_DOOR, DungeonsHooks::marimboDoor);
        com.runecraft.world.LandmarkBuilder.registerCustom("lighthouse", (world, centre) -> placeLighthouseLadder(world));
        com.runecraft.content.GameEvents.onActivity((p, key) -> {
            if (key.equals("talk:" + DungeonsContent.ASKELADDEN)) {
                askeladden(p);
            } else if (key.equals("talk:" + DungeonsContent.BARDUR)) {
                bardur(p);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 == 0) {
                ServerLevel world = Gielinor.world(server);
                if (world != null) {
                    keepKings(world);
                }
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++hazardTicks % HAZARD_INTERVAL != 0) {
                return;
            }
            ServerLevel world = Gielinor.world(server);
            if (world != null) {
                for (ServerPlayer p : world.players()) {
                    if (p.isAlive() && !p.isSpectator() && !p.isCreative()) {
                        apeAtollHazards(p);
                    }
                }
            }
        });
    }

    /** Puts the iron ladder down to the Waterbirth Island Dungeon on the Lighthouse tower's ground floor. */
    private static void placeLighthouseLadder(ServerLevel world) {
        net.minecraft.world.level.block.Block ladder = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(com.runecraft.RuneCraft.id(DungeonBlocks.LIGHTHOUSE_LADDER));
        if (ladder == Blocks.AIR) {
            return;
        }
        int[] c = DungeonBlocks.lighthouseTowerCentre();
        com.runecraft.world.StructurePlacer.applyIfLoaded(world, c[0] >> 4, c[1] >> 4);
        for (int[] o : new int[][]{{3, 0}, {-3, 0}, {0, 3}, {0, -3}, {2, 2}, {-2, -2}}) {
            for (int y = world.getSeaLevel(); y < world.getSeaLevel() + 12; y++) {
                BlockPos at = new BlockPos(c[0] + o[0], y, c[1] + o[1]);
                if (world.getBlockState(at).isAir() && world.getBlockState(at.above()).isAir()
                        && world.getBlockState(at.below()).isRedstoneConductor(world, at.below())) {
                    world.setBlock(at, ladder.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                    return;
                }
            }
        }
    }

    /** Game tick each missing King is due back (0 = present); the Kings return 90 seconds after dying (OSRS). */
    private static final long[] KING_DUE = new long[3];
    /** Whether each King has been seen alive since the server started (until then a missing King comes almost at once). */
    private static final boolean[] KING_SEEN = new boolean[3];
    /** Game ticks before a never-seen King appears (lets the lair's saved entities load first). */
    private static final long KING_FIRST_DELAY = 5;

    /**
     * The Dagannoth Kings are always in their lair in OSRS: while anyone is down there, each missing King comes back
     * {@link DungeonBlocks#KING_RESPAWN_TICKS} after it is found missing (the summoning stone still wakes them early).
     */
    private static void keepKings(ServerLevel world) {
        boolean visited = false;
        for (ServerPlayer p : world.players()) {
            if (p.isAlive() && DungeonBlocks.inKingsLair(p.getBlockX(), p.getBlockY(), p.getBlockZ())) {
                visited = true;
                break;
            }
        }
        if (!visited) {
            return;
        }
        long now = com.runecraft.action.ActionManager.gameTick();
        int[] centre = DungeonBlocks.kingSpot(0);
        net.minecraft.world.phys.AABB lair = new net.minecraft.world.phys.AABB(new BlockPos(centre[0], centre[1], centre[2])).inflate(40, 12, 40);
        for (int i = 0; i < DungeonBlocks.KINGS.size(); i++) {
            String id = DungeonBlocks.KINGS.get(i);
            net.minecraft.world.entity.EntityType<com.runecraft.boss.DataBossEntity> type = com.runecraft.registry.ModEntities.DATA_BOSS_TYPES.get(id);
            com.runecraft.osrs.content.BossDef def = com.runecraft.osrs.content.ContentRegistry.boss(id);
            if (type == null || def == null) {
                continue;
            }
            if (!world.getEntities(type, lair, e -> e.isAlive()).isEmpty()) {
                KING_DUE[i] = 0;
                KING_SEEN[i] = true;
                continue;
            }
            if (KING_DUE[i] == 0) {
                // A King that died waits 90 seconds; one never seen since the server started comes a few seconds after the first visit.
                KING_DUE[i] = now + (KING_SEEN[i] ? DungeonBlocks.KING_RESPAWN_TICKS : KING_FIRST_DELAY);
                continue;
            }
            if (now >= KING_DUE[i]) {
                int[] s = DungeonBlocks.kingSpot(i);
                com.runecraft.boss.DataBossService.spawn(world, new BlockPos(s[0], s[1], s[2]), def, null);
                KING_DUE[i] = 0;
                KING_SEEN[i] = true; // a just-spawned King may not show up in box queries at once: never re-spawn it early
            }
        }
    }

    /** How often (Minecraft ticks) Ape Atoll's traps and falling rocks are checked. */
    private static final int HAZARD_INTERVAL = 20;
    private static long hazardTicks;

    /**
     * The Temple of Marimbo Dungeon's floor traps spring under a player standing on them; in the Ape Atoll Dungeon rocks
     * fall now and then on anyone away from the rock ledges (damage approx).
     */
    private static void apeAtollHazards(ServerPlayer p) {
        BlockPos at = p.blockPosition();
        switch (DungeonBlocks.apeAtollHazard(at.getX(), at.getY(), at.getZ())) {
            case TUNNEL_TRAP -> {
                Msg.game(p, "You trigger a trap! Spikes shoot up from the floor.");
                hazardHit(p, 2 + p.getRandom().nextInt(5));
            }
            case SPIKE_TRAP -> {
                Msg.game(p, "You step on a spike trap!");
                hazardHit(p, 4);
            }
            case STONE_TRAP -> {
                Msg.game(p, "The rock shifts under your feet, and the stalagmites beside it lash out!");
                hazardHit(p, Math.max(1, (int) Math.ceil(p.getHealth() * 0.2)));
            }
            case HOT_ROCKS -> {
                String held = p.getMainHandItem().isEmpty() ? "" : Inv.idOf(p.getMainHandItem());
                if (!held.contains("zombie_monkey_greegree")) {
                    Msg.game(p, "The hot rocks burn your feet!");
                    hazardHit(p, 10);
                }
            }
            case FALLING_ROCKS -> {
                if (p.getRandom().nextInt(8) == 0) {
                    Msg.game(p, "Rocks fall from the ceiling and hit you!");
                    hazardHit(p, 1 + p.getRandom().nextInt(4));
                }
            }
            default -> {
            }
        }
    }

    /** The Kalphite Cave's monsters can only be damaged on a matching Slayer task (null = allowed). */
    private static String taskOnlyDenied(ServerPlayer player, net.minecraft.world.entity.LivingEntity target) {
        if (!(target instanceof com.runecraft.entity.OsrsMonsterEntity)) {
            return null;
        }
        BlockPos at = target.blockPosition();
        String needed = Dungeons.taskOnly(at.getX(), at.getY(), at.getZ());
        if (needed == null) {
            return null;
        }
        var profile = com.runecraft.content.slayer.SlayerService.profile(player);
        if (profile.hasTask() && needed.equals(profile.task)) {
            return null;
        }
        return "You can only fight the creatures of this cave while on a Slayer task to kill them.";
    }

    /**
     * The Temple of Marimbo Dungeon's wooden door to the great western cave and its monkey skull: locked unless the
     * hard Western Provinces Diary is complete; leaving the cave is free.
     */
    private static void marimboDoor(ServerPlayer player, BlockPos pos, BlockState state) {
        if (DungeonBlocks.insideMarimboDoor(player.getX())) {
            passThrough(player, pos);
            return;
        }
        if (com.runecraft.osrs.content.systems.Diaries.tierComplete(com.runecraft.content.systems.SysState.of(player).diaryDone, "western",
                com.runecraft.osrs.content.systems.Diaries.Tier.HARD)) {
            Msg.game(player, "The wooden door creaks open.");
            passThrough(player, pos);
        } else {
            Msg.game(player, "The wooden door won't open. (Completing the hard Western Provinces Diary gives access.)");
        }
    }

    private static void hazardHit(ServerPlayer p, int damage) {
        CombatHandler.applyDamage(null, p, damage);
        ServerNetworking.hitsplat(p, damage, Net.SPLAT_DAMAGE);
    }

    /**
     * Moves the player through the block at {@code pos} to the first free spot on its far side
     * (the side away from the player).
     */
    static boolean passThrough(ServerPlayer player, BlockPos pos) {
        if (!(player.level() instanceof ServerLevel world)) {
            return false;
        }
        double dx = pos.getX() + 0.5 - player.getX();
        double dz = pos.getZ() + 0.5 - player.getZ();
        Direction dir = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST)
                : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
        BlockPos base = new BlockPos(pos.getX(), player.getBlockY(), pos.getZ());
        for (int k = 1; k <= 10; k++) {
            BlockPos target = base.relative(dir, k);
            if (world.getBlockState(target).isAir() && world.getBlockState(target.above()).isAir()
                    && !world.getBlockState(target.below()).isAir()) {
                player.teleportTo(world, target.getX() + 0.5, target.getY(), target.getZ() + 0.5, player.getYRot(), player.getXRot());
                return true;
            }
        }
        Msg.game(player, "The way beyond is blocked.");
        return false;
    }

    private static void securityDoor(ServerPlayer player, BlockPos pos, BlockState state) {
        String tip = SECURITY_TIPS.get(player.getRandom().nextInt(SECURITY_TIPS.size()));
        Msg.game(player, "The door asks you a question about keeping your account safe.");
        Msg.game(player, tip);
        passThrough(player, pos);
    }

    private static void rewardChest(ServerPlayer player, int chest) {
        DungeonState state = DungeonState.get(player.getServer());
        String name = switch (chest) {
            case 0 -> "Gift of Peace";
            case 1 -> "Grain of Plenty";
            case 2 -> "Box of Health";
            default -> "Cradle of Life";
        };
        if (chest == 3) {
            cradleOfLife(player, state);
            return;
        }
        if (state.hasClaimed(player.getUUID(), chest)) {
            Msg.game(player, "The " + name + " is empty.");
            return;
        }
        state.claim(player.getUUID(), chest);
        int coins = DungeonBlocks.SOS_COINS.get(chest);
        Inv.give(player, Inv.COINS, coins);
        Msg.game(player, "You open the " + name + " and find " + coins + " coins!");
        if (chest == 2) {
            player.setHealth(player.getMaxHealth());
            PlayerData data = Players.data(player);
            data.setPrayerPoints(data.skills().level(Skill.PRAYER));
            Msg.game(player, "You feel refreshed and renewed.");
        }
        ServerNetworking.sync(player);
    }

    /** The Cradle of Life: fancy boots (or fighting boots while sneaking); a lost pair is replaced. */
    private static void cradleOfLife(ServerPlayer player, DungeonState state) {
        boolean has = Inv.has(player, DungeonsContent.FANCY_BOOTS, 1) || Inv.has(player, DungeonsContent.FIGHTING_BOOTS, 1);
        if (state.hasClaimed(player.getUUID(), 3) && has) {
            Msg.game(player, "The Cradle of Life is empty. You already have a pair of boots from it.");
            return;
        }
        String boots = player.isShiftKeyDown() ? DungeonsContent.FIGHTING_BOOTS : DungeonsContent.FANCY_BOOTS;
        state.claim(player.getUUID(), 3);
        Inv.give(player, boots, 1);
        Msg.game(player, "You take a pair of " + (player.isShiftKeyDown() ? "fighting" : "fancy") + " boots from the Cradle of Life.");
        if (!player.isShiftKeyDown()) {
            Msg.game(player, "(Sneak while opening the cradle for fighting boots instead.)");
        }
    }

    private static boolean hasAxe(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            String id = Inv.idOf(stack);
            if (id.endsWith("_axe") && !id.endsWith("pickaxe")) {
                return true;
            }
        }
        return false;
    }

    private static void vines(ServerPlayer player, BlockPos pos, BlockState state) {
        if (!hasAxe(player)) {
            Msg.game(player, "You need an axe to chop through these vines.");
            return;
        }
        Msg.game(player, "You chop your way through the vines.");
        passThrough(player, pos);
    }

    private static void miningGuildDoor(ServerPlayer player, BlockPos pos, BlockState state) {
        if (Players.data(player).skills().level(Skill.MINING) < 60) {
            Msg.game(player, "Sorry, but you need level 60 Mining to go in there.");
            return;
        }
        passThrough(player, pos);
    }

    /** A Mort'ton catacomb door: entering the crypt needs any shade key of the door's metal (kept); leaving is free. */
    private static void shadeDoor(ServerPlayer player, BlockPos pos, BlockState state) {
        ShadeCatacombs.Door door = ShadeCatacombs.doorAt(pos.getX(), pos.getZ());
        if (door == null) {
            passThrough(player, pos);
            return;
        }
        if (!door.inside(player.getX(), player.getZ())) {
            boolean hasKey = false;
            for (String colour : CommonItems.SHADE_KEY_COLOURS) {
                if (Inv.has(player, CommonItems.shadeKey(door.metal(), colour), 1)) {
                    hasKey = true;
                    break;
                }
            }
            if (!hasKey) {
                Msg.game(player, "The doors are locked. You need " + Msg.article(door.metal()) + " key to open them.");
                return;
            }
            Msg.game(player, "You unlock the doors with your " + door.metal() + " key.");
        }
        passThrough(player, pos);
    }

    /**
     * The Waterbirth Island Dungeon's Kings' ladder: from the great hall (sublevel 5) down into the Kings' lair, where
     * their summoning stone stands, and from the lair back up.
     */
    private static void kingsLadder(ServerPlayer player, BlockPos pos, BlockState state) {
        if (!(player.level() instanceof ServerLevel world)) {
            return;
        }
        boolean up = DungeonBlocks.inKingsLair(pos.getX(), pos.getY(), pos.getZ());
        int[] to = DungeonBlocks.kingsLadderTarget(pos.getX(), pos.getY(), pos.getZ());
        Msg.game(player, up ? "You climb back up to the dungeon." : "You climb down into the lair of the Dagannoth Kings...");
        player.teleportTo(world, to[0] + 0.5, to[1], to[2] + 0.5, player.getYRot(), player.getXRot());
    }

    /**
     * A Waterbirth circular door: from the entrance hall it opens only while another player stands on the pressure pad
     * beside it, or for a player with a pet rock to put on the pad; from the far side it opens freely.
     */
    private static void circularDoor(ServerPlayer player, BlockPos pos, BlockState state) {
        if (!(player.level() instanceof ServerLevel world)) {
            return;
        }
        if (!DungeonBlocks.outsideCircularDoor(player.getX()) || isOpen(pos)) {
            passThrough(player, pos);
            return;
        }
        if (padHeld(world, player, pos)) {
            Msg.game(player, "Your companion holds the pressure pad down, and the door swings open.");
            open(pos, 0);
            passThrough(player, pos);
        } else if (Inv.has(player, DungeonsContent.PET_ROCK, 1)) {
            Msg.game(player, "You put your pet rock on the pressure pad. The door swings open, and you pick the rock back up.");
            open(pos, 0);
            passThrough(player, pos);
        } else {
            Msg.game(player, "The door won't budge. Someone needs to stand on the pressure pad - or put a pet rock on it.");
        }
    }

    /**
     * Waterbirth's opened circular doors and knocked-down door-supports, by block column: the game tick until which
     * anyone may pass (one minute, approx: in OSRS a door stays open while its pad is held, a support until it respawns).
     */
    private static final java.util.Map<Long, Long> OPEN_UNTIL = new java.util.HashMap<>();
    private static final long OPEN_TICKS = 100;

    private static boolean isOpen(BlockPos pos) {
        Long until = OPEN_UNTIL.get(BlockPos.asLong(pos.getX(), 0, pos.getZ()));
        return until != null && com.runecraft.action.ActionManager.gameTick() < until;
    }

    /** Opens the door or support column at pos and every column within {@code radius} blocks of it (the same barrier's other blocks). */
    private static void open(BlockPos pos, int radius) {
        long until = com.runecraft.action.ActionManager.gameTick() + OPEN_TICKS;
        OPEN_UNTIL.entrySet().removeIf(e -> e.getValue() < com.runecraft.action.ActionManager.gameTick());
        for (int dx = -Math.max(radius, 1); dx <= Math.max(radius, 1); dx++) {
            for (int dz = -Math.max(radius, 1); dz <= Math.max(radius, 1); dz++) {
                OPEN_UNTIL.put(BlockPos.asLong(pos.getX() + dx, 0, pos.getZ() + dz), until);
            }
        }
    }

    /** True if another player stands on a pressure pad near the door. */
    private static boolean padHeld(ServerLevel world, ServerPlayer player, BlockPos door) {
        for (ServerPlayer other : world.players()) {
            if (other == player || !other.isAlive() || other.isSpectator()) {
                continue;
            }
            BlockPos at = other.blockPosition();
            if (Math.abs(at.getX() - door.getX()) <= 6 && Math.abs(at.getZ() - door.getZ()) <= 6 && Math.abs(at.getY() - door.getY()) <= 2
                    && world.getBlockState(at).is(Blocks.STONE_PRESSURE_PLATE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A door-support around Waterbirth's central room: knocked down from a distance by a ranged or magic weapon, or by
     * a rune thrownaxe's chain-hit (approx: it lets the player through and stands again); leaving the room is free.
     */
    private static void doorSupport(ServerPlayer player, BlockPos pos, BlockState state) {
        if (DungeonBlocks.insideCentralRoom(player.getX(), player.getZ(), pos.getX(), pos.getZ()) || isOpen(pos)) {
            passThrough(player, pos);
            return;
        }
        ItemStack held = player.getMainHandItem();
        String heldId = held.isEmpty() ? "" : Inv.idOf(held);
        if (heldId.equals("rune_thrownaxe") || Inv.has(player, "rune_thrownaxe", 1)) {
            Msg.game(player, "Your rune thrownaxe chain-hits the door-supports, and they collapse.");
            open(pos, 24); // the special bounces between all three supports
        } else if (DungeonBlocks.breaksDoorSupport(heldId)) {
            Msg.game(player, "You knock the door-support down from a safe distance.");
            open(pos, 2);
        } else {
            Msg.game(player, "The door-support is too sturdy to push past. Knock it down from a distance with a ranged or magic attack.");
            return;
        }
        passThrough(player, pos);
    }

    /** The iron ladder from Waterbirth sublevel 2 up to the Lighthouse. */
    private static void lighthouseLadder(ServerPlayer player, BlockPos pos, BlockState state) {
        if (!(player.level() instanceof ServerLevel world)) {
            return;
        }
        if (pos.getY() > com.runecraft.osrs.MultiCombat.UNDERGROUND_TOP) {
            // the iron ladder in the Lighthouse leads down to the Waterbirth Island Dungeon's rock lobsters
            int[] b = DungeonBlocks.lighthouseLadderBelow();
            Msg.game(player, "You climb down the long iron ladder into the dungeon below.");
            player.teleportTo(world, b[0] + 0.5, b[1], b[2] + 0.5, player.getYRot(), player.getXRot());
            return;
        }
        int[] l = DungeonBlocks.lighthouseExit();
        BlockPos to = Gielinor.safeSpot(world, l[0], l[1]);
        Msg.game(player, "You climb the long iron ladder up into the Lighthouse.");
        player.teleportTo(world, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, player.getYRot(), player.getXRot());
    }

    /**
     * A dungeon passage ({@link Dungeons.Passage}): the Varlamore lift platforms, Amoxliatl's imposing door, the way into
     * the Ruins of Mokhaiotl and the Troll Stronghold prison's secret exit.
     */
    private static void passage(ServerPlayer player, BlockPos pos, BlockState state) {
        if (!(player.level() instanceof ServerLevel world)) {
            return;
        }
        Dungeons.Passage p = DungeonBlocks.passageAt(pos.getX(), pos.getY(), pos.getZ());
        if (p == null) {
            Msg.game(player, "Nothing interesting happens.");
            return;
        }
        if (p.arena() != null) {
            com.runecraft.osrs.world.ArenaLayout.Arena a = com.runecraft.osrs.world.ArenaLayout.of(p.arena());
            if (a == null) {
                Msg.game(player, "The way is blocked.");
                return;
            }
            BlockPos to = Gielinor.safeSpot(world, a.x(), a.z() + Math.max(2, a.radius() / 2));
            Msg.game(player, p.message());
            player.teleportTo(world, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, player.getYRot(), player.getXRot());
            return;
        }
        Msg.game(player, p.message());
        if (p.toY() == Dungeons.SURFACE) {
            BlockPos to = Gielinor.safeSpot(world, p.toX(), p.toZ());
            player.teleportTo(world, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, player.getYRot(), player.getXRot());
            return;
        }
        com.runecraft.world.StructurePlacer.applyIfLoaded(world, p.toX() >> 4, p.toZ() >> 4);
        player.teleportTo(world, p.toX() + 0.5, p.toY(), p.toZ() + 0.5, player.getYRot(), player.getXRot());
    }

    /**
     * Puts the Troll Stronghold's prisoners in their cells when the stronghold place is built: the quests module's Godric
     * (spawned at the place) is moved into his cell and a Mad Eadgar stands in the next one (OSRS: both are freed in
     * the Troll Stronghold quest). Skipped while the prison's chunk is not loaded.
     */
    private static void placeTrollPrisoners(ServerLevel world, BlockPos centre) {
        int[] godric = DungeonBlocks.trollPrisoner(0);
        int[] eadgar = DungeonBlocks.trollPrisoner(1);
        if (!world.hasChunk(godric[0] >> 4, godric[2] >> 4) || !world.hasChunk(eadgar[0] >> 4, eadgar[2] >> 4)) {
            return;
        }
        com.runecraft.world.StructurePlacer.applyIfLoaded(world, godric[0] >> 4, godric[2] >> 4);
        com.runecraft.world.StructurePlacer.applyIfLoaded(world, eadgar[0] >> 4, eadgar[2] >> 4);
        net.minecraft.world.phys.AABB around = new net.minecraft.world.phys.AABB(centre.getX() - 48, world.getMinBuildHeight(), centre.getZ() - 48,
                centre.getX() + 48, centre.getY() + 64, centre.getZ() + 48);
        for (com.runecraft.entity.QuestNpcEntity npc : world.getEntitiesOfClass(com.runecraft.entity.QuestNpcEntity.class, around,
                e -> "godric".equals(e.npcId()))) {
            npc.moveTo(godric[0] + 0.5, godric[1], godric[2] + 0.5, 0f, 0f);
        }
        com.runecraft.osrs.content.NpcDef def = com.runecraft.osrs.content.ContentRegistry.npc("mad_eadgar");
        if (def == null) {
            return;
        }
        com.runecraft.entity.QuestNpcEntity entity = com.runecraft.registry.ModEntities.QUEST_NPC.create(world);
        if (entity == null) {
            return;
        }
        entity.moveTo(eadgar[0] + 0.5, eadgar[1], eadgar[2] + 0.5, 180f, 0f);
        entity.setNpc(def);
        entity.finalizeSpawn(world, world.getCurrentDifficultyAt(entity.blockPosition()), net.minecraft.world.entity.MobSpawnType.STRUCTURE, null, null);
        world.addFreshEntity(entity);
    }

    /** The rock pile by Waterbirth's arrival ladder: just rocks (pet rocks come from Askeladden in Rellekka, as in OSRS). */
    private static void rockPile(ServerPlayer player, BlockPos pos, BlockState state) {
        Msg.game(player, Inv.has(player, DungeonsContent.PET_ROCK, 1) ? "Your pet rock would sit nicely on a pressure pad."
                : "Just a pile of rocks. Askeladden in Rellekka gives out pet rocks that would sit nicely on a pressure pad.");
    }

    /**
     * Bardur, deep in the Waterbirth Island Dungeon, trades a cooked shark for a Fremennik blade, shield or helm once The
     * Fremennik Trials is complete (wiki).
     */
    private static void bardur(ServerPlayer player) {
        com.runecraft.osrs.QuestInfo trials = com.runecraft.osrs.content.ContentRegistry.quest("fremennik_trials");
        if (trials != null && !Players.data(player).questComplete(trials)) {
            com.runecraft.quest.QuestApi.say(player, "Bardur", "Outerlander! Leave me to my work - these fledgelings won't kill themselves.");
            return;
        }
        for (String gear : new String[]{"fremennik_blade", "fremennik_shield", "fremennik_helm"}) {
            if (Inv.has(player, gear, 1)) {
                Inv.remove(player, gear, 1);
                Inv.give(player, "shark", 1);
                com.runecraft.quest.QuestApi.say(player, "Bardur", "Fine Fremennik craftsmanship! Here, take this shark for it.");
                ServerNetworking.sync(player);
                return;
            }
        }
        com.runecraft.quest.QuestApi.say(player, "Bardur", "Bring me Fremennik blades, shields or helms from the dagannoth, and I'll give you a shark for each.");
    }

    /** Askeladden gives a pet rock once The Fremennik Trials is started (and more after it, OSRS). */
    private static void askeladden(ServerPlayer player) {
        com.runecraft.osrs.QuestInfo trials = com.runecraft.osrs.content.ContentRegistry.quest("fremennik_trials");
        if (trials != null && Players.data(player).questStage(trials) <= 0 && !Players.data(player).questComplete(trials)) {
            com.runecraft.quest.QuestApi.say(player, "Askeladden", "Hey, outerlander! Come back when you're trying to become one of us.");
            return;
        }
        if (Inv.has(player, DungeonsContent.PET_ROCK, 1)) {
            com.runecraft.quest.QuestApi.say(player, "Askeladden", "You've already got a pet rock, buddy. It never needs feeding!");
            return;
        }
        Inv.give(player, DungeonsContent.PET_ROCK, 1);
        com.runecraft.quest.QuestApi.say(player, "Askeladden", "Here, have a pet ROCK! A faithful companion for life.");
        ServerNetworking.sync(player);
    }

    private static void rift(ServerPlayer player, BlockPos pos, BlockState state) {
        Dungeons.Rift best = null;
        double bestD = Double.MAX_VALUE;
        for (Dungeons.Rift r : Dungeons.rifts()) {
            double ddx = pos.getX() - r.x();
            double ddy = pos.getY() - r.y();
            double ddz = pos.getZ() - r.z();
            double d = ddx * ddx + ddy * ddy + ddz * ddz;
            if (d < bestD) {
                bestD = d;
                best = r;
            }
        }
        if (best == null || bestD > 9) {
            Msg.game(player, "The rift is closed.");
            return;
        }
        Landmark altar = Landmark.byName(best.altar());
        if (altar == null) {
            Msg.game(player, "The rift is closed.");
            return;
        }
        Msg.game(player, "You step into the " + best.rune() + " rift...");
        Gielinor.teleport(player, altar);
    }
}
