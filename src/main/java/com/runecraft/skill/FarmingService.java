package com.runecraft.skill;

import com.runecraft.block.FarmingPatchBlock;
import com.runecraft.collection.CollectionLog;
import com.runecraft.osrs.FarmingDisease;
import com.runecraft.osrs.FarmingYield;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
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

/**
 * Planting seeds (with a seed dibber), composting, curing disease, waiting, and harvesting herb and
 * allotment patches. Disease rules are in {@link FarmingDisease}; the patch block rolls them as it grows.
 */
public final class FarmingService {
    /** Compost items by level (1 compost, 2 supercompost, 3 ultracompost); XP for applying each. */
    private static final String[] COMPOST = {null, "compost", "supercompost", "ultracompost"};
    private static final double[] COMPOST_XP = {0, 18.0, 26.0, 36.0};

    private FarmingService() {
    }

    public static void use(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state, FarmingPatchBlock patch) {
        int crop = state.get(patch.cropProperty());
        int stage = state.get(FarmingPatchBlock.STAGE);
        int level = Players.data(player).skills().currentLevel(Skill.FARMING);
        String held = player.getMainHandStack().isEmpty() ? "" : Inv.idOf(player.getMainHandStack());
        int compost = compostLevel(held);
        if (compost > 0 && (crop == 0 || stage < 4) && state.get(FarmingPatchBlock.DISEASE) < 2) {
            applyCompost(player, world, pos, state, compost, held);
            return;
        }
        if (crop == 0) {
            plant(player, world, pos, state, patch, level);
            return;
        }
        int disease = state.get(FarmingPatchBlock.DISEASE);
        if (disease == 2) {
            if (!Inv.has(player, "spade", 1)) {
                Msg.game(player, "The " + cropName(patch, crop) + " has died. You need a spade to clear the patch.");
                return;
            }
            world.setBlockState(pos, empty(state, patch), Block.NOTIFY_ALL);
            world.playSound(null, pos, SoundEvents.BLOCK_GRAVEL_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);
            Msg.action(player, "You dig up the dead plants and clear the patch.");
            return;
        }
        if (disease == 1) {
            if (!Inv.has(player, "plant_cure", 1)) {
                Msg.game(player, "The " + cropName(patch, crop) + " is diseased! Treat it with plant cure before it dies.");
                return;
            }
            Inv.remove(player, "plant_cure", 1);
            Inv.give(player, "vial", 1);
            world.setBlockState(pos, state.with(FarmingPatchBlock.DISEASE, 0), Block.NOTIFY_ALL);
            world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0f, 1.0f);
            Msg.action(player, "You treat the " + cropName(patch, crop) + " with plant cure. It is restored to health.");
            return;
        }
        if (stage < 4) {
            Msg.game(player, "The " + cropName(patch, crop) + " is still growing"
                    + (state.get(FarmingPatchBlock.PROTECTED) ? " (protected by the gardener)." : "."));
            return;
        }
        // Harvest lives (OSRS): 3 + compost lives, each pick has a level-based chance to save a life.
        int lives = FarmingYield.lives(state.get(FarmingPatchBlock.COMPOST));
        double bonus = Inv.has(player, "magic_secateurs", 1) ? FarmingYield.MAGIC_SECATEURS : 0;
        if ("attas".equals(FarmingPatchBlock.anima.apply(world))) {
            bonus += FarmingYield.ATTAS;
        }
        int yield;
        String item;
        double xp;
        int chance;
        if (patch.isHerbPatch()) {
            Herb herb = Herb.values()[crop - 1];
            item = herb.grimyItem();
            xp = herb.harvestXp();
            String cape = com.runecraft.content.systems.SysState.of(player)
                    .wornId(com.runecraft.osrs.content.systems.Jewellery.Slot.CAPE);
            if (cape.startsWith("farming_cape") || cape.equals("max_cape")) {
                bonus += FarmingYield.FARMING_CAPE;
            }
            chance = FarmingYield.saveChance(herb.harvestLow(), herb.harvestHigh(), level, bonus, 0);
        } else {
            Allotment a = Allotment.values()[crop - 1];
            item = a.produceItem();
            xp = a.harvestXp();
            chance = FarmingYield.saveChance(a.harvestLow(), a.harvestHigh(), level, bonus, 0);
        }
        yield = FarmingYield.harvest(lives, chance, player.getRandom()::nextInt);
        Inv.give(player, item, yield);
        com.runecraft.content.skilling.FarmingContractService.harvested(player,
                patch.isHerbPatch() ? Herb.values()[crop - 1].cleanItem() : item);
        CollectionLog.obtain(player, item);
        XpService.add(player, Skill.FARMING, xp * yield);
        Msg.game(player, "You harvest the patch.");
        world.setBlockState(pos, empty(state, patch), Block.NOTIFY_ALL);
        world.playSound(null, pos, SoundEvents.BLOCK_CROP_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);
    }

    private static BlockState empty(BlockState state, FarmingPatchBlock patch) {
        return state.with(patch.cropProperty(), 0).with(FarmingPatchBlock.STAGE, 0).with(FarmingPatchBlock.DISEASE, 0)
                .with(FarmingPatchBlock.COMPOST, 0).with(FarmingPatchBlock.PROTECTED, false);
    }

    private static int compostLevel(String item) {
        for (int i = 1; i < COMPOST.length; i++) {
            if (COMPOST[i].equals(item)) {
                return i;
            }
        }
        return 0;
    }

    private static void applyCompost(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state, int compost, String item) {
        if (state.get(FarmingPatchBlock.COMPOST) > 0) {
            Msg.game(player, "This patch has already been treated with compost.");
            return;
        }
        Inv.remove(player, item, 1);
        Inv.give(player, "minecraft:bucket", 1);
        world.setBlockState(pos, state.with(FarmingPatchBlock.COMPOST, compost), Block.NOTIFY_ALL);
        XpService.add(player, Skill.FARMING, COMPOST_XP[compost]);
        ItemDef def = ItemCatalog.get(item);
        Msg.action(player, "You treat the patch with " + (def == null ? item : def.name().toLowerCase(Locale.ROOT)) + ".");
        world.playSound(null, pos, SoundEvents.BLOCK_COMPOSTER_FILL, SoundCategory.BLOCKS, 1.0f, 1.0f);
    }

    /**
     * A gardener protecting this patch's crop for the OSRS payment (allotments only; herbs can't be
     * protected). Returns a status: 1 protected now, 0 nothing to protect, -1 can't pay (message sent).
     */
    public static int protect(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state, FarmingPatchBlock patch) {
        int crop = state.get(patch.cropProperty());
        if (crop == 0 || patch.isHerbPatch() || state.get(FarmingPatchBlock.STAGE) >= 4 || state.get(FarmingPatchBlock.DISEASE) > 0
                || state.get(FarmingPatchBlock.PROTECTED)) {
            return 0;
        }
        Allotment a = Allotment.values()[crop - 1];
        FarmingDisease.Payment pay = FarmingDisease.payment(a.produceItem());
        if (pay == null) {
            return 0;
        }
        if (!Inv.has(player, pay.item(), pay.count())) {
            ItemDef def = ItemCatalog.get(pay.item());
            Msg.game(player, "I'll look after your " + a.displayName().toLowerCase(Locale.ROOT) + " for " + pay.count() + " "
                    + (def == null ? pay.item() : def.name().toLowerCase(Locale.ROOT)) + ".");
            return -1;
        }
        Inv.remove(player, pay.item(), pay.count());
        world.setBlockState(pos, state.with(FarmingPatchBlock.PROTECTED, true), Block.NOTIFY_ALL);
        Msg.game(player, "That'll do nicely. I'll keep an eye on your " + a.displayName().toLowerCase(Locale.ROOT) + " for you.");
        return 1;
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
        // Hosidius: disease-free for players who completed the easy Kourend & Kebos diary.
        boolean diaryFree = com.runecraft.world.Gielinor.is(world) && FarmingDisease.hosidiusFarm(
                com.runecraft.osrs.GielinorMap.toOsrsX(pos.getX()), com.runecraft.osrs.GielinorMap.toOsrsY(pos.getZ()))
                && com.runecraft.osrs.content.systems.Diaries.tierComplete(com.runecraft.content.systems.SysState.of(player).diaryDone,
                "kourend", com.runecraft.osrs.content.systems.Diaries.Tier.EASY);
        world.setBlockState(pos, state.with(patch.cropProperty(), index + 1).with(FarmingPatchBlock.STAGE, 1)
                .with(FarmingPatchBlock.DISEASE, 0).with(FarmingPatchBlock.PROTECTED, diaryFree), Block.NOTIFY_ALL);
        XpService.add(player, Skill.FARMING, xp);
        Msg.game(player, "You plant " + (seeds > 1 ? seeds + " " : "a ") + name + " seed" + (seeds > 1 ? "s" : "") + " in the patch.");
        world.playSound(null, pos, SoundEvents.ITEM_CROP_PLANT, SoundCategory.BLOCKS, 1.0f, 1.0f);
    }

    // ------------------------------------------------------------------ farming spells (Lunar / Arceuus)

    /** One-line state of a herb or allotment patch (Geomancy). */
    public static String status(BlockState state, FarmingPatchBlock patch) {
        int crop = state.get(patch.cropProperty());
        String kind = patch.isHerbPatch() ? "Herb patch" : "Allotment";
        if (crop == 0) {
            return kind + ": empty" + (state.get(FarmingPatchBlock.COMPOST) > 0 ? " (composted)." : ".");
        }
        String name = cropName(patch, crop);
        return switch (state.get(FarmingPatchBlock.DISEASE)) {
            case 2 -> kind + ": the " + name + " is dead.";
            case 1 -> kind + ": the " + name + " is diseased.";
            default -> kind + ": " + name + (state.get(FarmingPatchBlock.STAGE) >= 4 ? ", ready to harvest." : ", growing.");
        };
    }

    /** Cure Plant: cures a diseased patch. Returns false (nothing to cure) without changing anything. */
    public static boolean curePlant(ServerWorld world, BlockPos pos, BlockState state, FarmingPatchBlock patch) {
        if (state.get(patch.cropProperty()) == 0 || state.get(FarmingPatchBlock.DISEASE) != 1) {
            return false;
        }
        world.setBlockState(pos, state.with(FarmingPatchBlock.DISEASE, 0), Block.NOTIFY_ALL);
        world.playSound(null, pos, SoundEvents.BLOCK_AZALEA_LEAVES_BREAK, SoundCategory.BLOCKS, 1.0f, 1.2f);
        return true;
    }

    /** Fertile Soil: treats the patch with supercompost. Returns false if it can't be treated. */
    public static boolean fertilise(ServerWorld world, BlockPos pos, BlockState state, FarmingPatchBlock patch) {
        int crop = state.get(patch.cropProperty());
        if (state.get(FarmingPatchBlock.COMPOST) > 0 || state.get(FarmingPatchBlock.DISEASE) > 0
                || (crop > 0 && state.get(FarmingPatchBlock.STAGE) >= 4)) {
            return false;
        }
        world.setBlockState(pos, state.with(FarmingPatchBlock.COMPOST, 2), Block.NOTIFY_ALL);
        world.playSound(null, pos, SoundEvents.BLOCK_COMPOSTER_FILL, SoundCategory.BLOCKS, 1.0f, 1.0f);
        return true;
    }

    /** True if the patch holds a dead crop (Resurrect Crops target). */
    public static boolean isDead(BlockState state, FarmingPatchBlock patch) {
        return state.get(patch.cropProperty()) > 0 && state.get(FarmingPatchBlock.DISEASE) == 2;
    }

    /** Resurrect Crops: brings a dead crop back to health, or clears the patch when the spell fails. */
    public static void resurrect(ServerWorld world, BlockPos pos, BlockState state, FarmingPatchBlock patch, boolean success) {
        if (success) {
            world.setBlockState(pos, state.with(FarmingPatchBlock.DISEASE, 0), Block.NOTIFY_ALL);
            world.playSound(null, pos, SoundEvents.ITEM_BONE_MEAL_USE, SoundCategory.BLOCKS, 1.0f, 1.0f);
        } else {
            world.setBlockState(pos, empty(state, patch), Block.NOTIFY_ALL);
            world.playSound(null, pos, SoundEvents.BLOCK_GRAVEL_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);
        }
    }

    private static String cropName(FarmingPatchBlock patch, int crop) {
        return patch.isHerbPatch() ? "herb" : Allotment.values()[crop - 1].displayName().toLowerCase(Locale.ROOT);
    }
}
