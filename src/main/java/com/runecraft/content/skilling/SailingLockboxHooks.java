package com.runecraft.content.skilling;

import com.runecraft.content.BlockHooks;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.skilling.SailingLockboxes;
import com.runecraft.osrs.content.skilling.SailingTraining;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.LandmarkBuilder;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Set;

/**
 * Lost schematic lockboxes (see {@link SailingLockboxes}): the lockbox on each island opens with the small key dug up
 * (with a spade) on another island and holds a schematic; reading it unlocks building that part at the boat schematics.
 */
public final class SailingLockboxHooks {
    private SailingLockboxHooks() {
    }

    private static final String SECTION = "sailing_schematics";

    static void init() {
        BlockHooks.register(SailingLockboxes.BLOCK, (player, pos, state) -> open(player, pos));
        for (SailingLockboxes.Lockbox l : SailingLockboxes.LOCKBOXES) {
            LandmarkBuilder.registerCustom(l.placeId(), (world, centre) -> SailingHooks.placeAtSea(world, centre, SailingLockboxes.BLOCK, 0));
        }
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (world.isClient() || stack.isEmpty() || !(player instanceof ServerPlayerEntity p)) {
                return TypedActionResult.pass(stack);
            }
            String id = Inv.idOf(stack);
            SailingLockboxes.Lockbox schematic = SailingLockboxes.bySchematic(id);
            if (schematic != null) {
                read(p, stack, schematic);
                return TypedActionResult.success(stack, false);
            }
            if ("spade".equals(id) && dig(p)) {
                return TypedActionResult.success(stack, false);
            }
            return TypedActionResult.pass(stack);
        });
    }

    private static NbtCompound state(ServerPlayerEntity player) {
        return SkillingState.get(player.getServer()).entry(SECTION, player.getUuidAsString());
    }

    /** The schematics a player has read. */
    public static Set<String> schematics(ServerPlayerEntity player) {
        Set<String> out = new HashSet<>();
        NbtCompound s = state(player);
        for (String k : s.getKeys()) {
            if (s.getBoolean(k)) {
                out.add(k);
            }
        }
        return out;
    }

    /** Forgets every schematic read (self-test reset). */
    public static void forget(ServerPlayerEntity player) {
        SkillingState.get(player.getServer()).remove(SECTION, player.getUuidAsString());
    }

    /** Why the part or facility can't be built yet for want of its schematic, or null. */
    public static String missing(ServerPlayerEntity player, Object partOrFacility) {
        if (player.getAbilities().creativeMode) {
            return null;
        }
        SailingLockboxes.Lockbox needed = partOrFacility instanceof SailingTraining.Part p ? SailingLockboxes.forPart(p.type(), p.tier())
                : SailingLockboxes.forFacility(((SailingTraining.Facility) partOrFacility).id());
        return SailingLockboxes.missingSchematic(needed, schematics(player));
    }

    /** Reads a schematic: the part or facility can be built from now on. */
    public static void read(ServerPlayerEntity player, ItemStack stack, SailingLockboxes.Lockbox l) {
        if (state(player).getBoolean(l.schematic())) {
            Msg.game(player, "You already know how to build what this schematic shows.");
            return;
        }
        stack.decrement(1);
        state(player).putBoolean(l.schematic(), true);
        Msg.action(player, "You study the " + SkillingUtil.lower(l.schematicName()) + ". You can now build it at the boat schematics.");
    }

    /** Opens the lockbox at a block: with its small key it gives the schematic, without it the directions. */
    public static boolean open(ServerPlayerEntity player, BlockPos pos) {
        SailingLockboxes.Lockbox l = SailingLockboxes.nearestLockbox(GielinorMap.toOsrsX(pos.getX()), GielinorMap.toOsrsY(pos.getZ()));
        if (l == null) {
            Msg.game(player, "The lockbox is rusted shut.");
            return false;
        }
        if (!SkillingUtil.need(player, Skill.SAILING, l.level(), "open the lockbox on " + l.islandName())) {
            return false;
        }
        if (!Inv.has(player, l.keyId(), 1)) {
            Msg.game(player, "The lockbox is locked. A note is pinned to it: \"" + l.directions() + "\"");
            return false;
        }
        if (Inv.has(player, l.schematic(), 1) || state(player).getBoolean(l.schematic())) {
            Msg.game(player, "The lockbox is empty; you've already taken its schematic.");
            return false;
        }
        Inv.remove(player, l.keyId(), 1);
        SkillingUtil.give(player, l.schematic(), 1);
        player.getWorld().playSound(null, pos, SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.6f, 1.0f);
        Msg.action(player, "You unlock the lockbox and find " + Msg.article(SkillingUtil.lower(l.schematicName())) + ".");
        return true;
    }

    /** Digging with a spade on a small key's spot digs up the key (once per lockbox). */
    public static boolean dig(ServerPlayerEntity player) {
        BlockPos p = player.getBlockPos();
        SailingLockboxes.Lockbox l = SailingLockboxes.keyAt(GielinorMap.toOsrsX(p.getX()), GielinorMap.toOsrsY(p.getZ()));
        if (l == null) {
            return false;
        }
        if (Inv.has(player, l.keyId(), 1) || Inv.has(player, l.schematic(), 1) || state(player).getBoolean(l.schematic())) {
            Msg.game(player, "You dig, but find nothing more here.");
            return true;
        }
        SkillingUtil.give(player, l.keyId(), 1);
        player.getWorld().playSound(null, p, SoundEvents.ITEM_SHOVEL_FLATTEN, SoundCategory.PLAYERS, 0.8f, 1.0f);
        Msg.action(player, "You dig up a small key.");
        return true;
    }
}
