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
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

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
        DungeonCamps.register();
        BarrowsHooks.register();
        // Task-only dungeons (the Kalphite Cave): no damage to their monsters without the matching Slayer task.
        com.runecraft.combat.CombatHooks.addModifier(new com.runecraft.combat.CombatHooks.PlayerAttackModifier() {
            @Override
            public int afterRoll(ServerPlayerEntity player, net.minecraft.entity.LivingEntity target, com.runecraft.osrs.AttackType type,
                                 int damage) {
                if (!(target instanceof com.runecraft.entity.OsrsMonsterEntity)) {
                    return damage;
                }
                BlockPos at = target.getBlockPos();
                String needed = Dungeons.taskOnly(at.getX(), at.getY(), at.getZ());
                if (needed == null) {
                    return damage;
                }
                var profile = com.runecraft.content.slayer.SlayerService.profile(player);
                if (profile.hasTask() && needed.equals(profile.task)) {
                    return damage;
                }
                Msg.game(player, "You can only fight the creatures of this cave while on a Slayer task to kill them.");
                return 0;
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++hazardTicks % HAZARD_INTERVAL != 0) {
                return;
            }
            ServerWorld world = Gielinor.world(server);
            if (world != null) {
                for (ServerPlayerEntity p : world.getPlayers()) {
                    if (p.isAlive() && !p.isSpectator() && !p.isCreative()) {
                        apeAtollHazards(p);
                    }
                }
            }
        });
    }

    /** How often (Minecraft ticks) Ape Atoll's traps and falling rocks are checked. */
    private static final int HAZARD_INTERVAL = 20;
    private static long hazardTicks;

    /**
     * The Temple of Marimbo Dungeon's floor traps spring under a player standing on them; in the Ape Atoll Dungeon rocks
     * fall now and then on anyone away from the rock ledges (damage approx).
     */
    private static void apeAtollHazards(ServerPlayerEntity p) {
        BlockPos at = p.getBlockPos();
        if (DungeonBlocks.apeAtollTrap(at.getX(), at.getY(), at.getZ())) {
            Msg.game(p, "You trigger a trap! Spikes shoot up from the floor.");
            hazardHit(p, 2 + p.getRandom().nextInt(5));
        } else if (DungeonBlocks.apeAtollFallingRocks(at.getX(), at.getY(), at.getZ()) && p.getRandom().nextInt(8) == 0) {
            Msg.game(p, "Rocks fall from the ceiling and hit you!");
            hazardHit(p, 1 + p.getRandom().nextInt(4));
        }
    }

    private static void hazardHit(ServerPlayerEntity p, int damage) {
        CombatHandler.applyDamage(null, p, damage);
        ServerNetworking.hitsplat(p, damage, Net.SPLAT_DAMAGE);
    }

    /**
     * Moves the player through the block at {@code pos} to the first free spot on its far side
     * (the side away from the player).
     */
    static boolean passThrough(ServerPlayerEntity player, BlockPos pos) {
        if (!(player.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        double dx = pos.getX() + 0.5 - player.getX();
        double dz = pos.getZ() + 0.5 - player.getZ();
        Direction dir = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST)
                : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
        BlockPos base = new BlockPos(pos.getX(), player.getBlockY(), pos.getZ());
        for (int k = 1; k <= 10; k++) {
            BlockPos target = base.offset(dir, k);
            if (world.getBlockState(target).isAir() && world.getBlockState(target.up()).isAir()
                    && !world.getBlockState(target.down()).isAir()) {
                player.teleport(world, target.getX() + 0.5, target.getY(), target.getZ() + 0.5, player.getYaw(), player.getPitch());
                return true;
            }
        }
        Msg.game(player, "The way beyond is blocked.");
        return false;
    }

    private static void securityDoor(ServerPlayerEntity player, BlockPos pos, BlockState state) {
        String tip = SECURITY_TIPS.get(player.getRandom().nextInt(SECURITY_TIPS.size()));
        Msg.game(player, "The door asks you a question about keeping your account safe.");
        Msg.game(player, tip);
        passThrough(player, pos);
    }

    private static void rewardChest(ServerPlayerEntity player, int chest) {
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
        if (state.hasClaimed(player.getUuid(), chest)) {
            Msg.game(player, "The " + name + " is empty.");
            return;
        }
        state.claim(player.getUuid(), chest);
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
    private static void cradleOfLife(ServerPlayerEntity player, DungeonState state) {
        boolean has = Inv.has(player, DungeonsContent.FANCY_BOOTS, 1) || Inv.has(player, DungeonsContent.FIGHTING_BOOTS, 1);
        if (state.hasClaimed(player.getUuid(), 3) && has) {
            Msg.game(player, "The Cradle of Life is empty. You already have a pair of boots from it.");
            return;
        }
        String boots = player.isSneaking() ? DungeonsContent.FIGHTING_BOOTS : DungeonsContent.FANCY_BOOTS;
        state.claim(player.getUuid(), 3);
        Inv.give(player, boots, 1);
        Msg.game(player, "You take a pair of " + (player.isSneaking() ? "fighting" : "fancy") + " boots from the Cradle of Life.");
        if (!player.isSneaking()) {
            Msg.game(player, "(Sneak while opening the cradle for fighting boots instead.)");
        }
    }

    private static boolean hasAxe(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
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

    private static void vines(ServerPlayerEntity player, BlockPos pos, BlockState state) {
        if (!hasAxe(player)) {
            Msg.game(player, "You need an axe to chop through these vines.");
            return;
        }
        Msg.game(player, "You chop your way through the vines.");
        passThrough(player, pos);
    }

    private static void miningGuildDoor(ServerPlayerEntity player, BlockPos pos, BlockState state) {
        if (Players.data(player).skills().level(Skill.MINING) < 60) {
            Msg.game(player, "Sorry, but you need level 60 Mining to go in there.");
            return;
        }
        passThrough(player, pos);
    }

    /** A Mort'ton catacomb door: entering the crypt needs any shade key of the door's metal (kept); leaving is free. */
    private static void shadeDoor(ServerPlayerEntity player, BlockPos pos, BlockState state) {
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
    private static void kingsLadder(ServerPlayerEntity player, BlockPos pos, BlockState state) {
        if (!(player.getWorld() instanceof ServerWorld world)) {
            return;
        }
        boolean up = DungeonBlocks.inKingsLair(pos.getX(), pos.getY(), pos.getZ());
        int[] to = DungeonBlocks.kingsLadderTarget(pos.getX(), pos.getY(), pos.getZ());
        Msg.game(player, up ? "You climb back up to the dungeon." : "You climb down into the lair of the Dagannoth Kings...");
        player.teleport(world, to[0] + 0.5, to[1], to[2] + 0.5, player.getYaw(), player.getPitch());
    }

    /**
     * A Waterbirth circular door: from the entrance hall it opens only while another player stands on the pressure pad
     * beside it, or for a player with a pet rock to put on the pad; from the far side it opens freely.
     */
    private static void circularDoor(ServerPlayerEntity player, BlockPos pos, BlockState state) {
        if (!(player.getWorld() instanceof ServerWorld world)) {
            return;
        }
        if (!DungeonBlocks.outsideCircularDoor(player.getX())) {
            passThrough(player, pos);
            return;
        }
        if (padHeld(world, player, pos)) {
            Msg.game(player, "Your companion holds the pressure pad down, and the door swings open.");
            passThrough(player, pos);
        } else if (Inv.has(player, DungeonsContent.PET_ROCK, 1)) {
            Msg.game(player, "You put your pet rock on the pressure pad. The door swings open, and you pick the rock back up.");
            passThrough(player, pos);
        } else {
            Msg.game(player, "The door won't budge. Someone needs to stand on the pressure pad - or put a pet rock on it.");
        }
    }

    /** True if another player stands on a pressure pad near the door. */
    private static boolean padHeld(ServerWorld world, ServerPlayerEntity player, BlockPos door) {
        for (ServerPlayerEntity other : world.getPlayers()) {
            if (other == player || !other.isAlive() || other.isSpectator()) {
                continue;
            }
            BlockPos at = other.getBlockPos();
            if (Math.abs(at.getX() - door.getX()) <= 6 && Math.abs(at.getZ() - door.getZ()) <= 6 && Math.abs(at.getY() - door.getY()) <= 2
                    && world.getBlockState(at).isOf(Blocks.STONE_PRESSURE_PLATE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A door-support around Waterbirth's central room: knocked down from a distance by a ranged or magic weapon, or by
     * a rune thrownaxe's chain-hit (approx: it lets the player through and stands again); leaving the room is free.
     */
    private static void doorSupport(ServerPlayerEntity player, BlockPos pos, BlockState state) {
        if (DungeonBlocks.insideCentralRoom(player.getX(), player.getZ(), pos.getX(), pos.getZ())) {
            passThrough(player, pos);
            return;
        }
        ItemStack held = player.getMainHandStack();
        String heldId = held.isEmpty() ? "" : Inv.idOf(held);
        if (heldId.equals("rune_thrownaxe") || Inv.has(player, "rune_thrownaxe", 1)) {
            Msg.game(player, "Your rune thrownaxe chain-hits the door-supports, and they collapse.");
        } else if (DungeonBlocks.breaksDoorSupport(heldId)) {
            Msg.game(player, "You knock the door-support down from a safe distance.");
        } else {
            Msg.game(player, "The door-support is too sturdy to push past. Knock it down from a distance with a ranged or magic attack.");
            return;
        }
        passThrough(player, pos);
    }

    /** The iron ladder from Waterbirth sublevel 2 up to the Lighthouse. */
    private static void lighthouseLadder(ServerPlayerEntity player, BlockPos pos, BlockState state) {
        if (!(player.getWorld() instanceof ServerWorld world)) {
            return;
        }
        int[] l = DungeonBlocks.lighthouseExit();
        BlockPos to = Gielinor.safeSpot(world, l[0], l[1]);
        Msg.game(player, "You climb the long iron ladder up into the Lighthouse.");
        player.teleport(world, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, player.getYaw(), player.getPitch());
    }

    /** The rock pile by Waterbirth's arrival ladder: one pet rock per player. */
    private static void rockPile(ServerPlayerEntity player, BlockPos pos, BlockState state) {
        if (Inv.has(player, DungeonsContent.PET_ROCK, 1)) {
            Msg.game(player, "You already have a pet rock.");
            return;
        }
        Inv.give(player, DungeonsContent.PET_ROCK, 1);
        Msg.game(player, "You pick a friendly-looking rock out of the pile. It would sit nicely on a pressure pad.");
        ServerNetworking.sync(player);
    }

    private static void rift(ServerPlayerEntity player, BlockPos pos, BlockState state) {
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
