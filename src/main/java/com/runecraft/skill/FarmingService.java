package com.runecraft.skill;

import com.runecraft.block.FarmingPatchBlock;
import com.runecraft.collection.CollectionLog;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.data.Allotment;
import com.runecraft.osrs.data.Herb;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

import java.util.Locale;

/** Planting seeds (with a seed dibber), waiting, and harvesting herb and allotment patches. */
public final class FarmingService {
    private FarmingService() {
    }

    public static void use(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state, FarmingPatchBlock patch) {
        int crop = state.get(patch.cropProperty());
        int stage = state.get(FarmingPatchBlock.STAGE);
        int level = Players.data(player).skills().currentLevel(Skill.FARMING);
        if (crop == 0) {
            plant(player, world, pos, state, patch, level);
            return;
        }
        if (stage < 4) {
            Msg.game(player, "The " + cropName(patch, crop) + " is still growing.");
            return;
        }
        // Harvest
        int yield;
        String item;
        double xp;
        if (patch.isHerbPatch()) {
            Herb herb = Herb.values()[crop - 1];
            item = herb.grimyItem();
            xp = herb.harvestXp();
            yield = 3 + player.getRandom().nextInt(4) + level / 33;
        } else {
            Allotment a = Allotment.values()[crop - 1];
            item = a.produceItem();
            xp = a.harvestXp();
            yield = 3 + player.getRandom().nextInt(6) + level / 25;
        }
        Inv.give(player, item, yield);
        CollectionLog.obtain(player, item);
        XpService.add(player, Skill.FARMING, xp * yield);
        Msg.game(player, "You harvest the patch.");
        world.setBlockState(pos, state.with(patch.cropProperty(), 0).with(FarmingPatchBlock.STAGE, 0), Block.NOTIFY_ALL);
        world.playSound(null, pos, SoundEvents.BLOCK_CROP_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);
    }

    private static void plant(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state,
                              FarmingPatchBlock patch, int level) {
        ItemStack held = player.getMainHandStack();
        String id = held.isEmpty() ? "" : Inv.idOf(held);
        int index = -1;
        int required = 0;
        double xp = 0;
        String name = "";
        int seeds = 1;
        if (patch.isHerbPatch()) {
            for (Herb h : Herb.values()) {
                if (h.seedItem().equals(id)) {
                    index = h.ordinal();
                    required = h.farmingLevel();
                    xp = h.plantXp();
                    name = h.shortName().toLowerCase(Locale.ROOT);
                }
            }
        } else {
            seeds = 3;
            for (Allotment a : Allotment.values()) {
                if (a.seedItem().equals(id)) {
                    index = a.ordinal();
                    required = a.level();
                    xp = a.plantXp();
                    name = a.displayName().toLowerCase(Locale.ROOT);
                }
            }
        }
        if (index < 0) {
            Msg.game(player, patch.isHerbPatch() ? "This is a herb patch. Use a herb seed on it to plant it."
                    : "This is an allotment. Use 3 allotment seeds on it to plant it.");
            return;
        }
        if (level < required) {
            Msg.game(player, "You need a Farming level of " + required + " to plant that.");
            return;
        }
        if (!Inv.has(player, "seed_dibber", 1)) {
            Msg.game(player, "You need a seed dibber to plant seeds.");
            return;
        }
        if (held.getCount() < seeds) {
            Msg.game(player, "You need " + seeds + " seeds to plant an allotment.");
            return;
        }
        held.decrement(seeds);
        world.setBlockState(pos, state.with(patch.cropProperty(), index + 1).with(FarmingPatchBlock.STAGE, 1), Block.NOTIFY_ALL);
        XpService.add(player, Skill.FARMING, xp);
        Msg.game(player, "You plant " + (seeds > 1 ? seeds + " " : "a ") + name + " seed" + (seeds > 1 ? "s" : "") + " in the patch.");
        world.playSound(null, pos, SoundEvents.ITEM_CROP_PLANT, SoundCategory.BLOCKS, 1.0f, 1.0f);
    }

    private static String cropName(FarmingPatchBlock patch, int crop) {
        return patch.isHerbPatch() ? "herb" : Allotment.values()[crop - 1].displayName().toLowerCase(Locale.ROOT);
    }
}
