package com.runecraft.content.dungeons;

import com.runecraft.content.BlockHooks;
import com.runecraft.content.ModuleHooks;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.dungeons.DungeonBlocks;
import com.runecraft.osrs.content.dungeons.Dungeons;
import com.runecraft.osrs.content.dungeons.DungeonsContent;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;

/**
 * Minecraft-side behaviour for the dungeons module: the monster camps inside every dungeon, the
 * Stronghold of Security's doors and reward chests, Brimhaven's vines, the Mining Guild door and
 * the Abyss's passages and rifts, and the Barrows crypts and tunnels ({@link BarrowsHooks}).
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
        DungeonCamps.register();
        BarrowsHooks.register();
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
