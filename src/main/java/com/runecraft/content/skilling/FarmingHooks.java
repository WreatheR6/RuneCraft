package com.runecraft.content.skilling;

import com.runecraft.content.BlockHooks;
import com.runecraft.osrs.Chance;
import com.runecraft.osrs.FarmingDisease;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.skilling.CraftingData;
import com.runecraft.osrs.content.skilling.FarmingData;
import com.runecraft.osrs.content.skilling.FarmingData.Crop;
import com.runecraft.osrs.content.skilling.FarmingData.Patch;
import com.runecraft.osrs.content.skilling.Loot;
import com.runecraft.osrs.data.ToolTier;
import com.runecraft.osrs.data.Tree;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestScripts;
import com.runecraft.skill.Tools;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

/**
 * Tree/fruit/bush/hops/special patches, compost bins and bird houses. Patch state lives in
 * {@link SkillingState} ("patches"); growth is computed from world time so patches grow while
 * nobody is around, exactly like OSRS. Growing crops are shown with vanilla decoration above the patch.
 */
final class FarmingHooks {
    /** 1.0 = real OSRS growth times. */
    static final double GROWTH_SPEED = 1.0;
    private static final long TICKS_PER_MINUTE = 1200;

    private static final Landmark[] SPIRIT_DESTINATIONS = {Landmark.GNOME_STRONGHOLD, Landmark.VARROCK, Landmark.PORT_SARIM,
            Landmark.BRIMHAVEN, Landmark.YANILLE, Landmark.CATHERBY};

    static final List<String> BIRDHOUSE_SEEDS = List.of("barley_seed", "hammerstone_seed", "asgarnian_seed", "jute_seed",
            "yanillian_seed", "krandorian_seed", "wildblood_seed", "guam_seed", "marrentill_seed", "tarromin_seed", "harralander_seed");

    private FarmingHooks() {
    }

    static void init() {
        for (Patch p : Patch.values()) {
            BlockHooks.register(p.blockId, (player, pos, state) -> usePatch(player, pos, p));
        }
        BlockHooks.register("compost_bin", (player, pos, state) -> useBin(player, pos));
        BlockHooks.register("birdhouse_space", (player, pos, state) -> buildBirdhouse(player, pos));
        BlockHooks.register("birdhouse", (player, pos, state) -> useBirdhouse(player, pos));
        for (FarmingData.Gardener g : FarmingData.GARDENERS) {
            QuestScripts.onTalk(g.id(), (player, npc) -> gardener(player, npc, g));
        }
        for (FarmingData.Leprechaun l : FarmingData.LEPRECHAUNS) {
            QuestScripts.onTalk(l.id(), (player, npc) -> leprechaun(player));
        }
        QuestScripts.onTalk("solihib", (player, npc) -> {
            com.runecraft.quest.QuestApi.say(player, "Solihib", "Ook! Food for sale, the finest on Ape Atoll.");
            com.runecraft.content.npcs.ShopService.open(player, "solihibs_food_stall", npc.getPos());
        });
        // Herb and allotment patch blocks ask here for flowers next to them and the living anima plant.
        com.runecraft.block.FarmingPatchBlock.flowers = FarmingHooks::flowerProtects;
        com.runecraft.block.FarmingPatchBlock.anima = FarmingHooks::activeAnima;
        // Hespori: killing the one grown from a seed pays the Farming XP; its summoning stone only works
        // for a player whose Hespori is fully grown.
        com.runecraft.content.GameEvents.onKill((player, entity, stats) -> {
            if (entity instanceof com.runecraft.boss.DataBossEntity boss && boss.def() != null && boss.def().id.equals("hespori")) {
                hesporiKilled(player);
            }
        });
        com.runecraft.boss.DataBossService.requirement("hespori", (player, def) -> hesporiFromStone(player));
    }

    // ------------------------------------------------------------------ anima plants

    /** The living anima plant on the server ("kronos", "iasor" or "attas"), or null. */
    static String activeAnima(ServerWorld world) {
        SkillingState state = SkillingState.get(world.getServer());
        if (!state.has("anima", "active")) {
            return null;
        }
        NbtCompound a = state.entry("anima", "active");
        Crop c = FarmingData.byId(a.getString("crop"));
        if (c == null || c.patch() != Patch.ANIMA || world.getTime() - a.getLong("planted") >= growthTicks(c)) {
            return null;
        }
        return c.id();
    }

    private static long growthTicks(Crop c) {
        return FarmingData.stages(c) * cycleTicks(c);
    }

    private static String animaName(String id) {
        return switch (id) {
            case "kronos" -> "patches have a chance to grow an extra stage";
            case "iasor" -> "crops are much less likely to become diseased";
            default -> "crops give a larger harvest";
        };
    }

    // ------------------------------------------------------------------ flowers protecting allotments

    /**
     * Whether a fully grown flower (or a scarecrow) in a flower patch next to an allotment protects
     * its crop (produce id) from disease, as in OSRS.
     */
    static boolean flowerProtects(ServerWorld world, BlockPos pos, String allotment) {
        SkillingState state = SkillingState.get(world.getServer());
        for (BlockPos p : BlockPos.iterate(pos.add(-6, -2, -6), pos.add(6, 2, 6))) {
            if (Patch.byBlock(SkillingUtil.blockId(world.getBlockState(p))) != Patch.FLOWER) {
                continue;
            }
            String key = SkillingUtil.key(world, p);
            if (!state.has("patches", key)) {
                continue;
            }
            NbtCompound nbt = state.entry("patches", key);
            Crop flower = nbt.contains("crop") ? FarmingData.byId(nbt.getString("crop")) : null;
            if (flower == null || !FarmingDisease.flowerProtects(allotment, flower.id())) {
                continue;
            }
            if (flower.harvest() == FarmingData.Harvest.SCARECROW) {
                return true;
            }
            FarmingDisease.Crop h = updateHealth(world, flower, nbt);
            if (h.grown(world.getTime(), FarmingData.stages(flower), cycleTicks(flower))) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ Hespori

    /** A fully grown Hespori is disturbed (from its patch or the summoning stone): it attacks. */
    private static void hesporiKilled(ServerPlayerEntity player) {
        SkillingState state = SkillingState.get(player.getServer());
        String id = player.getUuidAsString();
        if (!state.has("hespori", id) || !state.entry("hespori", id).getBoolean("fighting")) {
            return;
        }
        state.remove("hespori", id);
        Crop c = FarmingData.byId("hespori");
        XpService.add(player, Skill.FARMING, c.checkXp());
        Msg.action(player, "You have defeated the Hespori. Its seed pods burst open.");
    }

    /** Summoning stone check: only a player whose Hespori is fully grown can wake it there. */
    private static boolean hesporiFromStone(ServerPlayerEntity player) {
        ServerWorld world = (ServerWorld) player.getWorld();
        SkillingState state = SkillingState.get(player.getServer());
        String id = player.getUuidAsString();
        if (state.has("hespori", id)) {
            NbtCompound h = state.entry("hespori", id);
            if (h.getBoolean("fighting")) {
                return true;
            }
            String key = h.getString("patch");
            if (state.has("patches", key)) {
                NbtCompound nbt = state.entry("patches", key);
                Crop crop = FarmingData.byId(nbt.getString("crop"));
                if (crop != null && crop.harvest() == FarmingData.Harvest.BOSS) {
                    FarmingDisease.Crop health = updateHealth(world, crop, nbt);
                    if (health.grown(world.getTime(), FarmingData.stages(crop), cycleTicks(crop))) {
                        clear(world, BlockPos.fromLong(h.getLong("pos")), nbt, state, key);
                        h.putBoolean("fighting", true);
                        return true;
                    }
                }
            }
        }
        Msg.game(player, "Hespori only awakens from a fully grown Hespori plant. Plant a Hespori seed in the Farming Guild's"
                + " Hespori patch and come back when it has grown.");
        return false;
    }

    private static void disturbHespori(ServerPlayerEntity player, ServerWorld world, BlockPos pos, Crop crop, NbtCompound nbt,
                                       SkillingState state, String key) {
        com.runecraft.osrs.content.BossDef def = com.runecraft.osrs.content.ContentRegistry.boss("hespori");
        if (def == null) {
            Msg.game(player, "The Hespori stirs, but does not wake.");
            return;
        }
        clear(world, pos, nbt, state, key);
        NbtCompound h = state.entry("hespori", player.getUuidAsString());
        h.putBoolean("fighting", true);
        com.runecraft.boss.DataBossService.spawn(world, pos.up().offset(player.getHorizontalFacing().getOpposite(), 2), def, player);
        world.playSound(null, pos, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.6f, 1.0f);
        Msg.red(player, "You disturb the Hespori! (level-" + def.combatLevel + ")");
    }

    /** A tool leprechaun notes the harvested produce in the player's inventory. */
    static void leprechaun(ServerPlayerEntity player) {
        int noted = 0;
        net.minecraft.entity.player.PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.main.size(); i++) {
            ItemStack s = inv.main.get(i);
            if (s.isEmpty() || s.hasNbt()) {
                continue;
            }
            String id = Inv.idOf(s);
            if (!FarmingData.leprechaunNotes(id) || !com.runecraft.bank.Notes.canNote(id, "")) {
                continue;
            }
            ItemStack note = com.runecraft.bank.Notes.note(id, "", s.getCount());
            if (note.isEmpty()) {
                continue;
            }
            noted += s.getCount();
            inv.main.set(i, ItemStack.EMPTY);
            Inv.give(player, note);
        }
        Msg.game(player, noted > 0 ? "Tool Leprechaun: There ye go, I've noted yer produce for ye."
                : "Tool Leprechaun: Bring me yer harvested produce and I'll note it for ye.");
    }

    /** Length of one growth cycle in world ticks. */
    private static long cycleTicks(Crop c) {
        return Math.max(1, (long) (FarmingData.cycleMinutes(c) * TICKS_PER_MINUTE / GROWTH_SPEED));
    }

    private static FarmingDisease.Crop health(NbtCompound nbt) {
        return new FarmingDisease.Crop(nbt.getLong("planted"), nbt.getInt("rolled"),
                nbt.contains("diseasedAt") ? nbt.getLong("diseasedAt") : -1, nbt.getBoolean("dead"));
    }

    private static void saveHealth(NbtCompound nbt, FarmingDisease.Crop h) {
        nbt.putLong("planted", h.planted);
        nbt.putInt("rolled", h.rolled);
        nbt.putLong("diseasedAt", h.diseasedAt);
        nbt.putBoolean("dead", h.dead);
    }

    /** Rolls disease for every growth cycle since the patch was last looked at and saves the result. */
    private static FarmingDisease.Crop updateHealth(ServerWorld world, Crop crop, NbtCompound nbt) {
        FarmingDisease.Crop h = health(nbt);
        Random random = world.getRandom();
        String anima = activeAnima(world);
        int stages = FarmingData.stages(crop);
        long cycle = cycleTicks(crop);
        h.update(world.getTime(), stages, cycle, FarmingData.diseaseChance(crop, nbt.getInt("compost"), "iasor".equals(anima)),
                nbt.getBoolean("protected"), random::nextInt);
        // Kronos: every growth cycle that passes has a chance to grow the crop an extra stage.
        if ("kronos".equals(anima) && crop.patch() != Patch.ANIMA && !h.dead && !h.diseased()) {
            long cycles = Math.min(stages, Math.max(0, (world.getTime() - h.planted) / cycle));
            int done = nbt.getInt("kronos");
            while (done < cycles) {
                done++;
                if (random.nextInt(100) < FarmingDisease.KRONOS_SKIP_PERCENT && (world.getTime() - h.planted) / cycle < stages) {
                    h.planted -= cycle;
                }
            }
            nbt.putInt("kronos", done);
        }
        saveHealth(nbt, h);
        return h;
    }

    // ------------------------------------------------------------------ patches

    private static void usePatch(ServerPlayerEntity player, BlockPos pos, Patch patch) {
        ServerWorld world = (ServerWorld) player.getWorld();
        SkillingState state = SkillingState.get(player.getServer());
        String key = SkillingUtil.key(world, pos);
        NbtCompound nbt = state.entry("patches", key);
        String held = SkillingUtil.heldId(player);
        Crop crop = nbt.contains("crop") ? FarmingData.byId(nbt.getString("crop")) : null;
        if (crop == null) {
            emptyPatch(player, world, pos, patch, nbt, held);
            return;
        }
        if (crop.harvest() == FarmingData.Harvest.SCARECROW) {
            if (player.isSneaking() && Inv.has(player, "spade", 1)) {
                clear(world, pos, nbt, state, key);
                SkillingUtil.give(player, "scarecrow", 1);
                Msg.action(player, "You take the scarecrow out of the patch.");
            } else {
                Msg.game(player, "A scarecrow keeps the birds off the sweetcorn nearby. Sneak with a spade to remove it.");
            }
            return;
        }
        FarmingDisease.Crop health = updateHealth(world, crop, nbt);
        if (crop.harvest() == FarmingData.Harvest.ANIMA) {
            animaPatch(player, world, pos, crop, nbt, state, key, health);
            return;
        }
        if (health.dead) {
            deadPatch(player, world, pos, crop, nbt, state, key);
            return;
        }
        if (health.diseased()) {
            diseasedPatch(player, world, pos, crop, nbt, health);
            return;
        }
        int stages = FarmingData.stages(crop);
        long cycle = cycleTicks(crop);
        long now = world.getTime();
        if (!health.grown(now, stages, cycle)) {
            long done = Math.max(0, now - health.planted);
            Msg.game(player, "The " + SkillingUtil.lower(crop.name()) + " is still growing (" + Math.min(99, done * 100 / (stages * cycle))
                    + "%)" + (nbt.getBoolean("protected") ? ", protected by the gardener." : "."));
            return;
        }
        setVisual(world, pos, crop, nbt.getBoolean("stump") ? 2 : 1, nbt);
        switch (crop.harvest()) {
            case CHOP -> chopOrDig(player, world, pos, crop, nbt, state, key);
            case PICK -> pick(player, world, pos, crop, nbt, state, key, now - (health.planted + stages * cycle));
            case ALL -> harvestAll(player, world, pos, crop, nbt, state, key);
            case SPIRIT -> spiritTree(player, crop, nbt);
            case CHECK_ALL -> {
                if (!nbt.getBoolean("checked")) {
                    checkHealth(player, crop, nbt);
                } else {
                    harvestAll(player, world, pos, crop, nbt, state, key);
                }
            }
            case BOSS -> disturbHespori(player, world, pos, crop, nbt, state, key);
            default -> {
            }
        }
    }

    /** An anima plant: active from planting until it withers at the end of its growth; then it is dug up. */
    private static void animaPatch(ServerPlayerEntity player, ServerWorld world, BlockPos pos, Crop crop, NbtCompound nbt,
                                   SkillingState state, String key, FarmingDisease.Crop health) {
        long left = growthTicks(crop) - (world.getTime() - health.planted);
        if (left > 0) {
            long hours = Math.max(1, left / (60 * TICKS_PER_MINUTE));
            Msg.game(player, "The " + SkillingUtil.lower(crop.name()) + " plant is thriving: " + animaName(crop.id())
                    + " (about " + hours + " more hour" + (hours == 1 ? "" : "s") + ").");
            return;
        }
        if (!Inv.has(player, "spade", 1)) {
            Msg.game(player, "The " + SkillingUtil.lower(crop.name()) + " plant has withered. You need a spade to clear the patch.");
            return;
        }
        clear(world, pos, nbt, state, key);
        Msg.action(player, "You dig up the withered " + SkillingUtil.lower(crop.name()) + " plant.");
    }

    private static void deadPatch(ServerPlayerEntity player, ServerWorld world, BlockPos pos, Crop crop, NbtCompound nbt,
                                  SkillingState state, String key) {
        if (nbt.getInt("visual") != 4) {
            nbt.putInt("visual", 4);
            clearVisual(world, pos);
            if (world.getBlockState(pos.up()).isAir()) {
                world.setBlockState(pos.up(), Blocks.POTTED_DEAD_BUSH.getDefaultState(), Block.NOTIFY_LISTENERS);
            }
        }
        if (!Inv.has(player, "spade", 1)) {
            Msg.game(player, "The " + SkillingUtil.lower(crop.name()) + " has died. You need a spade to clear the patch.");
            return;
        }
        clear(world, pos, nbt, state, key);
        world.playSound(null, pos, SoundEvents.BLOCK_GRAVEL_BREAK, SoundCategory.BLOCKS, 1f, 1f);
        Msg.action(player, "You dig up the dead " + SkillingUtil.lower(crop.name()) + " and clear the patch.");
    }

    private static void diseasedPatch(ServerPlayerEntity player, ServerWorld world, BlockPos pos, Crop crop, NbtCompound nbt,
                                      FarmingDisease.Crop health) {
        String name = SkillingUtil.lower(crop.name());
        if (crop.patch().pruned) {
            if (!Inv.has(player, "secateurs", 1) && !Inv.has(player, "magic_secateurs", 1)) {
                Msg.game(player, "The " + name + " is diseased! Prune it with secateurs before it dies.");
                return;
            }
            Msg.action(player, "You prune the " + name + " with your secateurs. It is restored to health.");
        } else {
            if (!Inv.has(player, "plant_cure", 1)) {
                Msg.game(player, "The " + name + " is diseased! Treat it with plant cure before it dies.");
                return;
            }
            Inv.remove(player, "plant_cure", 1);
            Inv.give(player, "vial", 1);
            Msg.action(player, "You treat the " + name + " with plant cure. It is restored to health.");
        }
        health.cure(world.getTime(), cycleTicks(crop));
        saveHealth(nbt, health);
        world.playSound(null, pos, SoundEvents.BLOCK_AZALEA_LEAVES_BREAK, SoundCategory.BLOCKS, 1f, 1.2f);
    }

    /**
     * A gardener looking after the planted patches around them for the OSRS protection payment.
     * Talking pays for the first unprotected crop the player can afford; otherwise the gardener
     * names the price.
     */
    static void gardener(ServerPlayerEntity player, com.runecraft.entity.QuestNpcEntity npc, FarmingData.Gardener g) {
        ServerWorld world = (ServerWorld) player.getWorld();
        SkillingState state = SkillingState.get(player.getServer());
        BlockPos centre = npc.getBlockPos();
        List<String> prices = new ArrayList<>();
        boolean any = false;
        for (BlockPos p : BlockPos.iterate(centre.add(-14, -4, -14), centre.add(14, 4, 14))) {
            BlockState bs = world.getBlockState(p);
            if (bs.getBlock() instanceof com.runecraft.block.FarmingPatchBlock core) {
                int r = com.runecraft.skill.FarmingService.protect(player, world, p.toImmutable(), bs, core);
                if (r == 1) {
                    return;
                }
                any |= r != 0;
                continue;
            }
            Patch patch = Patch.byBlock(SkillingUtil.blockId(bs));
            String key = SkillingUtil.key(world, p);
            if (patch == null || !state.has("patches", key)) {
                continue;
            }
            NbtCompound nbt = state.entry("patches", key);
            Crop crop = nbt.contains("crop") ? FarmingData.byId(nbt.getString("crop")) : null;
            if (crop == null || nbt.getBoolean("protected")) {
                continue;
            }
            FarmingDisease.Crop health = updateHealth(world, crop, nbt);
            if (health.dead || health.diseased() || health.grown(world.getTime(), FarmingData.stages(crop), cycleTicks(crop))) {
                continue;
            }
            List<FarmingDisease.Payment> fee = FarmingDisease.paymentsFor(crop.id());
            if (fee.isEmpty()) {
                continue;
            }
            any = true;
            if (fee.stream().allMatch(pay -> Inv.has(player, pay.item(), pay.count()))) {
                fee.forEach(pay -> Inv.remove(player, pay.item(), pay.count()));
                nbt.putBoolean("protected", true);
                Msg.game(player, g.name() + ": That'll do nicely. I'll look after your " + SkillingUtil.lower(crop.name()) + " for you.");
                return;
            }
            List<String> items = new ArrayList<>();
            fee.forEach(pay -> items.add(pay.count() + " " + SkillingUtil.lower(SkillingUtil.name(pay.item()))));
            prices.add(String.join(" and ", items) + " for your " + SkillingUtil.lower(crop.name()));
        }
        if (!prices.isEmpty()) {
            Msg.game(player, g.name() + ": I'll protect it from disease for " + String.join(", ", prices) + ".");
        } else if (!any) {
            Msg.game(player, g.name() + ": Plant something in my patches and I can look after it for you, for a price.");
        }
    }

    private static void emptyPatch(ServerPlayerEntity player, ServerWorld world, BlockPos pos, Patch patch, NbtCompound nbt, String held) {
        FarmingData.Compost compost = FarmingData.Compost.byItem(held);
        if (compost != null) {
            if (nbt.getInt("compost") > 0) {
                Msg.game(player, "This patch has already been treated with compost.");
                return;
            }
            Inv.remove(player, held, 1);
            Inv.give(player, "minecraft:bucket", 1);
            nbt.putInt("compost", compost.bonus);
            XpService.add(player, Skill.FARMING, compost.applyXp);
            Msg.action(player, "You treat the " + SkillingUtil.lower(patch.displayName) + " with " + SkillingUtil.lower(SkillingUtil.name(held)) + ".");
            return;
        }
        Crop crop = FarmingData.byPlanted(patch, held);
        if (crop == null) {
            Msg.game(player, "This is " + Msg.article(SkillingUtil.lower(patch.displayName)) + ". " + plantHint(patch));
            return;
        }
        if (!SkillingUtil.need(player, Skill.FARMING, crop.level(), "plant that")) {
            return;
        }
        boolean sapling = !crop.seed().equals(crop.planted());
        String tool = sapling ? "spade" : "seed_dibber";
        if (!FarmingData.noPlantingTool(crop)
                && !SkillingUtil.needItem(player, tool, "You need " + Msg.article(SkillingUtil.lower(SkillingUtil.name(tool))) + " to plant that.")) {
            return;
        }
        if (crop.patch() == Patch.ANIMA && activeAnima(world) != null) {
            Msg.game(player, "An anima plant is already growing. Only one can thrive at a time.");
            return;
        }
        if (Inv.count(player, held) < crop.count()) {
            Msg.game(player, "You need " + crop.count() + " seeds to plant this patch.");
            return;
        }
        Inv.remove(player, held, crop.count());
        nbt.putString("crop", crop.id());
        nbt.putLong("planted", world.getTime());
        nbt.putBoolean("checked", false);
        nbt.putBoolean("stump", false);
        nbt.putInt("rolled", 0);
        nbt.putLong("diseasedAt", -1);
        nbt.putBoolean("dead", false);
        nbt.putBoolean("protected", hosidiusDiseaseFree(player, world, pos));
        nbt.putInt("kronos", 0);
        nbt.putInt("picks", crop.minYield() + nbt.getInt("compost"));
        if (crop.patch() == Patch.ANIMA) {
            NbtCompound a = state(player).entry("anima", "active");
            a.putString("crop", crop.id());
            a.putLong("planted", world.getTime());
            a.putString("patch", SkillingUtil.key(world, pos));
            Msg.game(player, "The " + SkillingUtil.lower(crop.name()) + " seed takes root: " + animaName(crop.id()) + ".");
        }
        if (crop.harvest() == FarmingData.Harvest.BOSS) {
            NbtCompound h = state(player).entry("hespori", player.getUuidAsString());
            h.putString("patch", SkillingUtil.key(world, pos));
            h.putLong("pos", pos.asLong());
            h.putBoolean("fighting", false);
        }
        if (crop.plantXp() > 0) {
            XpService.add(player, Skill.FARMING, crop.plantXp());
        }
        setVisual(world, pos, crop, 0, nbt);
        world.playSound(null, pos, SoundEvents.ITEM_CROP_PLANT, SoundCategory.BLOCKS, 1f, 1f);
        Msg.action(player, FarmingData.noPlantingTool(crop) ? "You stand the scarecrow in the patch."
                : "You plant the " + SkillingUtil.lower(SkillingUtil.name(held)) + " in the patch.");
    }

    private static SkillingState state(ServerPlayerEntity player) {
        return SkillingState.get(player.getServer());
    }

    /**
     * Crops planted in the Hosidius patches by a player who has completed the easy Kourend &amp; Kebos
     * diary can't get diseased (OSRS).
     */
    static boolean hosidiusDiseaseFree(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        if (!Gielinor.is(world) || !FarmingDisease.hosidiusFarm(com.runecraft.osrs.GielinorMap.toOsrsX(pos.getX()),
                com.runecraft.osrs.GielinorMap.toOsrsY(pos.getZ()))) {
            return false;
        }
        return com.runecraft.osrs.content.systems.Diaries.tierComplete(com.runecraft.content.systems.SysState.of(player).diaryDone,
                "kourend", com.runecraft.osrs.content.systems.Diaries.Tier.EASY);
    }

    private static String plantHint(Patch patch) {
        List<String> names = new ArrayList<>();
        for (Crop c : FarmingData.CROPS) {
            if (c.patch() == patch) {
                names.add(SkillingUtil.lower(SkillingUtil.name(c.planted())));
            }
        }
        return "You can plant: " + String.join(", ", names) + ".";
    }

    private static void checkHealth(ServerPlayerEntity player, Crop crop, NbtCompound nbt) {
        nbt.putBoolean("checked", true);
        XpService.add(player, Skill.FARMING, crop.checkXp());
        FarmingContractService.harvested(player, crop.id());
        Msg.action(player, "You examine the " + SkillingUtil.lower(crop.name()) + " for signs of disease and find that it is in perfect health.");
    }

    private static void chopOrDig(ServerPlayerEntity player, ServerWorld world, BlockPos pos, Crop crop, NbtCompound nbt,
                                  SkillingState state, String key) {
        if (!nbt.getBoolean("checked")) {
            checkHealth(player, crop, nbt);
            return;
        }
        if (nbt.getBoolean("stump")) {
            if (!SkillingUtil.needItem(player, "spade", "You need a spade to dig up the tree stump.")) {
                return;
            }
            if (crop.roots() != null) {
                SkillingUtil.give(player, crop.roots(), 1 + player.getRandom().nextInt(2) + nbt.getInt("compost"));
            }
            clear(world, pos, nbt, state, key);
            Msg.action(player, "You dig up the tree stump.");
            return;
        }
        Tree tree = Tree.valueOf(crop.produce());
        if (!SkillingUtil.need(player, Skill.WOODCUTTING, tree.level(), "chop down this tree")) {
            return;
        }
        if (Tools.bestAxe(player, Players.data(player).skills()) == null) {
            Msg.game(player, "You do not have an axe which you have the woodcutting level to use.");
            return;
        }
        String blockId = SkillingUtil.blockId(world.getBlockState(pos));
        Msg.action(player, "You swing your axe at the tree.");
        SkillingUtil.start(player, new SkillingUtil.BlockAction(pos, blockId, 4) {
            @Override
            boolean roll(ServerPlayerEntity p, Random random) {
                NbtCompound n = SkillingState.get(p.getServer()).entry("patches", key);
                ToolTier axe = Tools.bestAxe(p, Players.data(p).skills());
                if (axe == null || n.getBoolean("stump") || !n.contains("crop")) {
                    return false;
                }
                world.playSound(null, pos, SoundEvents.BLOCK_WOOD_HIT, SoundCategory.PLAYERS, 1f, 0.9f);
                double chance = Chance.lerp(SkillingUtil.level(p, Skill.WOODCUTTING), tree.low(axe.woodcuttingMultiplier()),
                        tree.high(axe.woodcuttingMultiplier()));
                boolean expired = com.runecraft.skill.Gathering.TREE_TIMERS.chop(key, tree.despawnTicks(),
                        com.runecraft.action.ActionManager.gameTick());
                if (random.nextDouble() < chance) {
                    SkillingUtil.give(p, tree.logItem(), 1);
                    XpService.add(p, Skill.WOODCUTTING, tree.xp());
                    Msg.action(p, "You get some " + SkillingUtil.lower(tree.logName()) + ".");
                    if (expired) {
                        com.runecraft.skill.Gathering.TREE_TIMERS.reset(key);
                        n.putBoolean("stump", true);
                        setVisual(world, pos, crop, 2, n);
                        Msg.game(p, "You have chopped down the tree. Dig up the stump with a spade to clear the patch.");
                        return false;
                    }
                }
                return true;
            }
        });
    }

    private static void pick(ServerPlayerEntity player, ServerWorld world, BlockPos pos, Crop crop, NbtCompound nbt, SkillingState state,
                             String key, long sinceGrown) {
        if (!nbt.getBoolean("checked") && crop.checkXp() > 0) {
            checkHealth(player, crop, nbt);
            nbt.putLong("regrow", world.getTime());
            return;
        }
        int max = crop.maxYield() + nbt.getInt("compost");
        long regrowTicks = Math.max(20, (long) (crop.regrowMinutes() * TICKS_PER_MINUTE / GROWTH_SPEED));
        long last = nbt.contains("regrow") ? nbt.getLong("regrow") : world.getTime() - sinceGrown;
        int regrown = (int) ((world.getTime() - last) / regrowTicks);
        int picks = nbt.getInt("picks");
        if (regrown > 0 && picks < max) {
            picks = Math.min(max, picks + regrown);
            nbt.putLong("regrow", world.getTime());
        }
        if (picks <= 0) {
            if (player.isSneaking() && Inv.has(player, "spade", 1)) {
                clear(world, pos, nbt, state, key);
                Msg.action(player, "You dig up the " + SkillingUtil.lower(crop.name()) + ".");
                return;
            }
            Msg.game(player, "There's nothing left to pick. It will regrow, or sneak with a spade to dig it up.");
            nbt.putInt("picks", 0);
            return;
        }
        if (crop.patch() == Patch.CELASTRUS && Tools.bestAxe(player, Players.data(player).skills()) == null) {
            Msg.game(player, "You need an axe to harvest celastrus bark.");
            return;
        }
        // Cactus spines and celastrus bark use harvest lives: a saved pick doesn't use up a spine/bark.
        int[] lives = FarmingData.harvestChance(crop);
        boolean saved = lives != null && player.getRandom().nextInt(256) < com.runecraft.osrs.FarmingYield.saveChance(lives[0], lives[1],
                SkillingUtil.level(player, Skill.FARMING), yieldBonus(player, world, crop), 0);
        nbt.putInt("picks", saved ? picks : picks - 1);
        if (!nbt.contains("regrow")) {
            nbt.putLong("regrow", world.getTime());
        }
        SkillingUtil.give(player, crop.produce(), 1);
        XpService.add(player, Skill.FARMING, crop.harvestXp());
        FarmingContractService.harvested(player, crop.id());
        world.playSound(null, pos, SoundEvents.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, SoundCategory.BLOCKS, 1f, 1f);
        Msg.action(player, "You pick " + Msg.article(SkillingUtil.lower(SkillingUtil.name(crop.produce()))) + ".");
    }

    private static void harvestAll(ServerPlayerEntity player, ServerWorld world, BlockPos pos, Crop crop, NbtCompound nbt,
                                   SkillingState state, String key) {
        if (crop.patch() == Patch.HOPS && !Inv.has(player, "secateurs", 1) && !Inv.has(player, "spade", 1)) {
            Msg.game(player, "You need a spade to harvest the hops.");
            return;
        }
        Random random = player.getRandom();
        int yield = FarmingData.harvestYield(crop, SkillingUtil.level(player, Skill.FARMING), nbt.getInt("compost"),
                yieldBonus(player, world, crop), random::nextInt);
        SkillingUtil.give(player, crop.produce(), yield);
        XpService.add(player, Skill.FARMING, FarmingData.harvestXp(crop, yield));
        FarmingContractService.harvested(player, crop.id());
        clear(world, pos, nbt, state, key);
        world.playSound(null, pos, SoundEvents.BLOCK_CROP_BREAK, SoundCategory.BLOCKS, 1f, 1f);
        Msg.action(player, "You harvest " + yield + " " + SkillingUtil.lower(SkillingUtil.name(crop.produce())) + " from the patch.");
    }

    /** Harvest lives item bonus: magic secateurs (hops, grapes) and a living attas plant. */
    private static double yieldBonus(ServerPlayerEntity player, ServerWorld world, Crop crop) {
        double bonus = 0;
        if (FarmingData.secateursBoost(crop) && Inv.has(player, "magic_secateurs", 1)) {
            bonus += com.runecraft.osrs.FarmingYield.MAGIC_SECATEURS;
        }
        if ("attas".equals(activeAnima(world))) {
            bonus += com.runecraft.osrs.FarmingYield.ATTAS;
        }
        return bonus;
    }

    private static void spiritTree(ServerPlayerEntity player, Crop crop, NbtCompound nbt) {
        if (!nbt.getBoolean("checked")) {
            checkHealth(player, crop, nbt);
            Msg.game(player, "The spirit tree is fully grown. Talk to it to travel; sneak to choose a destination.");
            return;
        }
        int dest = nbt.getInt("dest") % SPIRIT_DESTINATIONS.length;
        if (player.isSneaking()) {
            dest = (dest + 1) % SPIRIT_DESTINATIONS.length;
            nbt.putInt("dest", dest);
            Msg.game(player, "The spirit tree will take you to " + SPIRIT_DESTINATIONS[dest].displayName() + ".");
            return;
        }
        if (!Gielinor.is(player)) {
            Msg.game(player, "The spirit tree can only take you to places in Gielinor.");
            return;
        }
        Msg.action(player, "You feel at one with the spirit tree.");
        Gielinor.teleport(player, SPIRIT_DESTINATIONS[dest]);
    }

    private static void clear(ServerWorld world, BlockPos pos, NbtCompound nbt, SkillingState state, String key) {
        clearVisual(world, pos);
        state.remove("patches", key);
    }

    // ------------------------------------------------------------------ visuals

    /** stage: 0 growing, 1 grown, 2 stump. */
    private static void setVisual(ServerWorld world, BlockPos pos, Crop crop, int stage, NbtCompound nbt) {
        if (nbt.getInt("visual") == stage + 1) {
            return;
        }
        nbt.putInt("visual", stage + 1);
        clearVisual(world, pos);
        List<BlockState> column = new ArrayList<>();
        if (stage == 0) {
            column.add(growingPot(crop).getDefaultState());
        } else if (stage == 2) {
            column.add(log(crop).getDefaultState());
        } else {
            switch (crop.patch()) {
                case TREE, HARDWOOD, REDWOOD, SPIRIT, CALQUAT, CELASTRUS -> {
                    column.add(log(crop).getDefaultState());
                    column.add(log(crop).getDefaultState());
                    column.add(leaves(crop));
                }
                case FRUIT_TREE -> {
                    column.add(log(crop).getDefaultState());
                    column.add(Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true));
                }
                case BUSH -> column.add(Blocks.AZALEA_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true));
                case CACTUS -> column.add(Blocks.POTTED_CACTUS.getDefaultState());
                case MUSHROOM -> column.add(Blocks.POTTED_RED_MUSHROOM.getDefaultState());
                case BELLADONNA -> column.add(Blocks.POTTED_WITHER_ROSE.getDefaultState());
                case FLOWER -> column.add(crop.harvest() == FarmingData.Harvest.SCARECROW ? Blocks.CARVED_PUMPKIN.getDefaultState()
                        : flowerPot(crop).getDefaultState());
                case CRYSTAL -> {
                    column.add(Blocks.AMETHYST_BLOCK.getDefaultState());
                    column.add(Blocks.LARGE_AMETHYST_BUD.getDefaultState());
                }
                case HESPORI -> column.add(Blocks.POTTED_CRIMSON_FUNGUS.getDefaultState());
                case ANIMA -> column.add(Blocks.POTTED_BLUE_ORCHID.getDefaultState());
                default -> column.add(Blocks.POTTED_BAMBOO.getDefaultState());
            }
        }
        for (int i = 0; i < column.size(); i++) {
            BlockPos at = pos.up(i + 1);
            if (world.getBlockState(at).isAir() || isVisual(world.getBlockState(at))) {
                world.setBlockState(at, column.get(i), Block.NOTIFY_LISTENERS);
            }
        }
    }

    private static void clearVisual(ServerWorld world, BlockPos pos) {
        for (int i = 1; i <= 3; i++) {
            BlockPos at = pos.up(i);
            if (isVisual(world.getBlockState(at))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
            }
        }
    }

    private static boolean isVisual(BlockState state) {
        String id = SkillingUtil.blockId(state);
        return id.startsWith("potted_") || id.endsWith("_log") || id.endsWith("_leaves") || id.equals("carved_pumpkin")
                || id.equals("amethyst_block") || id.equals("large_amethyst_bud");
    }

    private static Block growingPot(Crop c) {
        return switch (c.patch()) {
            case TREE -> switch (c.id()) {
                case "willow" -> Blocks.POTTED_MANGROVE_PROPAGULE;
                case "maple" -> Blocks.POTTED_ACACIA_SAPLING;
                case "yew" -> Blocks.POTTED_SPRUCE_SAPLING;
                case "magic" -> Blocks.POTTED_BIRCH_SAPLING;
                default -> Blocks.POTTED_OAK_SAPLING;
            };
            case HARDWOOD, CALQUAT -> Blocks.POTTED_JUNGLE_SAPLING;
            case REDWOOD -> Blocks.POTTED_DARK_OAK_SAPLING;
            case SPIRIT, CELASTRUS -> Blocks.POTTED_CHERRY_SAPLING;
            case FRUIT_TREE -> Blocks.POTTED_FLOWERING_AZALEA_BUSH;
            case BUSH -> Blocks.POTTED_AZALEA_BUSH;
            case CACTUS -> Blocks.POTTED_CACTUS;
            case MUSHROOM -> Blocks.POTTED_BROWN_MUSHROOM;
            case BELLADONNA -> Blocks.POTTED_ALLIUM;
            case FLOWER -> c.harvest() == FarmingData.Harvest.SCARECROW ? Blocks.CARVED_PUMPKIN : Blocks.POTTED_FERN;
            case HESPORI -> Blocks.POTTED_CRIMSON_ROOTS;
            case ANIMA -> Blocks.POTTED_BLUE_ORCHID;
            case CRYSTAL -> Blocks.POTTED_WARPED_ROOTS;
            default -> Blocks.POTTED_FERN;
        };
    }

    private static Block flowerPot(Crop c) {
        return switch (c.id()) {
            case "marigold" -> Blocks.POTTED_ORANGE_TULIP;
            case "nasturtium" -> Blocks.POTTED_RED_TULIP;
            case "woad" -> Blocks.POTTED_CORNFLOWER;
            case "white_lily" -> Blocks.POTTED_LILY_OF_THE_VALLEY;
            case "limpwurt" -> Blocks.POTTED_DANDELION;
            default -> Blocks.POTTED_AZURE_BLUET;
        };
    }

    private static Block log(Crop c) {
        return switch (c.id()) {
            case "willow", "teak" -> Blocks.JUNGLE_LOG;
            case "maple", "redwood" -> Blocks.ACACIA_LOG;
            case "yew" -> Blocks.SPRUCE_LOG;
            case "magic", "spirit" -> Blocks.BIRCH_LOG;
            case "mahogany" -> Blocks.DARK_OAK_LOG;
            case "celastrus" -> Blocks.CHERRY_LOG;
            default -> Blocks.OAK_LOG;
        };
    }

    private static BlockState leaves(Crop c) {
        Block b = switch (c.id()) {
            case "willow", "teak", "calquat" -> Blocks.JUNGLE_LEAVES;
            case "maple", "redwood" -> Blocks.ACACIA_LEAVES;
            case "yew" -> Blocks.SPRUCE_LEAVES;
            case "magic" -> Blocks.BIRCH_LEAVES;
            case "mahogany" -> Blocks.DARK_OAK_LEAVES;
            case "spirit", "celastrus" -> Blocks.CHERRY_LEAVES;
            default -> Blocks.OAK_LEAVES;
        };
        return b.getDefaultState().with(LeavesBlock.PERSISTENT, true);
    }

    // ------------------------------------------------------------------ compost bins

    private static void useBin(ServerPlayerEntity player, BlockPos pos) {
        ServerWorld world = (ServerWorld) player.getWorld();
        SkillingState state = SkillingState.get(player.getServer());
        String key = SkillingUtil.key(world, pos);
        NbtCompound bin = state.entry("bins", key);
        int count = bin.getInt("count");
        String held = SkillingUtil.heldId(player);
        long closedAt = bin.contains("closed") ? bin.getLong("closed") : -1;
        if (closedAt < 0) {
            boolean superItem = FarmingData.SUPER_COMPOSTABLE.contains(held);
            if ((superItem || FarmingData.COMPOSTABLE.contains(held) || held.startsWith("grimy_")) && count < 15) {
                int n = Math.min(15 - count, Inv.count(player, held));
                Inv.remove(player, held, n);
                if (count == 0) {
                    bin.putBoolean("super", true);
                }
                if (!(superItem || held.startsWith("grimy_"))) {
                    bin.putBoolean("super", false);
                }
                bin.putInt("count", count + n);
                Msg.action(player, "You fill the compost bin (" + (count + n) + "/15).");
                return;
            }
            if (count >= 15) {
                bin.putLong("closed", world.getTime());
                Msg.action(player, "You close the compost bin. The contents will begin to rot.");
                world.playSound(null, pos, SoundEvents.BLOCK_BARREL_CLOSE, SoundCategory.BLOCKS, 1f, 1f);
                return;
            }
            Msg.game(player, count == 0 ? "The compost bin is empty. Fill it with 15 vegetables, fruit, hops or weeds."
                    : "The compost bin contains " + count + "/15 items.");
            return;
        }
        boolean superC = bin.getBoolean("super");
        long rot = (long) ((superC ? 90 : 45) * TICKS_PER_MINUTE / GROWTH_SPEED);
        if (world.getTime() - closedAt < rot) {
            Msg.game(player, "The compost is still rotting.");
            return;
        }
        if (held.equals("volcanic_ash") && superC && !bin.getBoolean("ultra")) {
            if (Inv.count(player, "volcanic_ash") < 25) {
                Msg.game(player, "You need 25 volcanic ash to turn this supercompost into ultracompost.");
                return;
            }
            Inv.remove(player, "volcanic_ash", 25);
            bin.putBoolean("ultra", true);
            Msg.action(player, "You mix the volcanic ash into the supercompost, making ultracompost.");
            return;
        }
        String item = bin.getBoolean("ultra") ? "ultracompost" : superC ? "supercompost" : "compost";
        FarmingData.Compost type = FarmingData.Compost.byItem(item);
        int buckets = Math.min(count, Inv.count(player, "minecraft:bucket"));
        if (buckets <= 0) {
            Msg.game(player, "You need empty buckets to take the " + item + " out of the bin.");
            return;
        }
        Inv.remove(player, "minecraft:bucket", buckets);
        SkillingUtil.give(player, item, buckets);
        XpService.add(player, Skill.FARMING, type.binXp * buckets);
        if (buckets >= count) {
            state.remove("bins", key);
            Msg.action(player, "You empty the compost bin.");
        } else {
            bin.putInt("count", count - buckets);
            Msg.action(player, "You take " + buckets + " " + item + " out of the bin.");
        }
    }

    // ------------------------------------------------------------------ bird houses

    private static CraftingData.BirdHouse birdhouse(String item) {
        for (CraftingData.BirdHouse b : CraftingData.BIRD_HOUSES) {
            if (b.id().equals(item)) {
                return b;
            }
        }
        return null;
    }

    private static void buildBirdhouse(ServerPlayerEntity player, BlockPos pos) {
        CraftingData.BirdHouse b = birdhouse(SkillingUtil.heldId(player));
        if (b == null) {
            Msg.game(player, "You can build a bird house here. Use a bird house on the space.");
            return;
        }
        if (!SkillingUtil.need(player, Skill.HUNTER, b.hunterLevel(), "build this bird house")) {
            return;
        }
        BlockState house = SkillingUtil.moduleBlock("birdhouse");
        if (house == null) {
            return;
        }
        ServerWorld world = (ServerWorld) player.getWorld();
        Inv.remove(player, b.id(), 1);
        world.setBlockState(pos, house);
        NbtCompound nbt = SkillingState.get(player.getServer()).entry("birdhouses", SkillingUtil.key(world, pos));
        nbt.putString("type", b.id());
        nbt.putString("owner", player.getUuidAsString());
        Msg.action(player, "You build " + Msg.article(SkillingUtil.lower(b.name())) + ". Fill it with 10 hop or herb seeds.");
    }

    private static void useBirdhouse(ServerPlayerEntity player, BlockPos pos) {
        ServerWorld world = (ServerWorld) player.getWorld();
        SkillingState state = SkillingState.get(player.getServer());
        String key = SkillingUtil.key(world, pos);
        NbtCompound nbt = state.entry("birdhouses", key);
        CraftingData.BirdHouse b = birdhouse(nbt.getString("type"));
        if (b == null) {
            b = CraftingData.BIRD_HOUSES.get(0);
            nbt.putString("type", b.id());
        }
        if (!nbt.contains("seeded")) {
            String held = SkillingUtil.heldId(player);
            if (!BIRDHOUSE_SEEDS.contains(held) || Inv.count(player, held) < 10) {
                Msg.game(player, "The bird house is empty. Use 10 hop seeds or low-level herb seeds on it.");
                return;
            }
            Inv.remove(player, held, 10);
            nbt.putLong("seeded", world.getTime());
            Msg.action(player, "You fill the bird house with seeds.");
            return;
        }
        if (world.getTime() - nbt.getLong("seeded") < (long) (50 * TICKS_PER_MINUTE / GROWTH_SPEED)) {
            Msg.game(player, "The bird house is still trapping birds.");
            return;
        }
        Random random = player.getRandom();
        int tier = CraftingData.BIRD_HOUSES.indexOf(b);
        XpService.add(player, Skill.HUNTER, b.hunterXp());
        SkillingUtil.give(player, "minecraft:feather", 20 + random.nextInt(20 + tier * 10));
        SkillingUtil.give(player, "raw_bird_meat", 1 + random.nextInt(4));
        SkillingUtil.give(player, "bones", 1 + random.nextInt(4));
        int nests = random.nextDouble() < 0.3 + tier * 0.06 ? 1 + (random.nextInt(4) == 0 ? 1 : 0) : 0;
        for (int i = 0; i < nests; i++) {
            int r = random.nextInt(100);
            SkillingUtil.give(player, r < 5 ? "bird_nest_ring" : r < 70 ? "bird_nest_seeds" : "bird_nest", 1);
        }
        state.remove("birdhouses", key);
        BlockState space = SkillingUtil.moduleBlock("birdhouse_space");
        if (space != null) {
            world.setBlockState(pos, space);
        }
        Msg.action(player, "You dismantle the bird house and collect its contents" + (nests > 0 ? ", including a bird's nest!" : "."));
    }

    /** Seeds found in a bird's nest (tree, fruit tree and special seeds, OSRS-like weights). */
    static final List<Loot> NEST_SEEDS = List.of(Loot.of("acorn", 214), Loot.of("willow_seed", 135), Loot.of("maple_seed", 67),
            Loot.of("yew_seed", 18), Loot.of("magic_seed", 3), Loot.of("apple_seed", 60), Loot.of("banana_seed", 40),
            Loot.of("orange_seed", 30), Loot.of("curry_seed", 25), Loot.of("pineapple_seed", 15), Loot.of("papaya_seed", 10),
            Loot.of("palm_seed", 6), Loot.of("calquat_tree_seed", 6), Loot.of("teak_seed", 8), Loot.of("mahogany_seed", 4),
            Loot.of("spirit_seed", 1), Loot.of("dragonfruit_seed", 1), Loot.of("redwood_tree_seed", 1), Loot.of("celastrus_seed", 1));

    static final List<Loot> NEST_RINGS = List.of(Loot.of("gold_ring", 35), Loot.of("sapphire_ring", 40), Loot.of("emerald_ring", 15),
            Loot.of("ruby_ring", 8), Loot.of("diamond_ring", 2));

    /** Tree seed + filled plant pot (with a gardening trowel) = sapling. Returns true if handled. */
    static boolean potSeed(ServerPlayerEntity player, ItemStack stack, String seed) {
        for (Crop c : FarmingData.CROPS) {
            if (c.seed().equals(seed) && !c.seed().equals(c.planted())) {
                if (!SkillingUtil.need(player, Skill.FARMING, c.level(), "plant this seed")) {
                    return true;
                }
                if (!SkillingUtil.needItem(player, "filled_plant_pot", "You need a plant pot filled with soil to plant this seed.")
                        || !SkillingUtil.needItem(player, "gardening_trowel", "You need a gardening trowel to plant this seed in a pot.")) {
                    return true;
                }
                stack.decrement(1);
                Inv.remove(player, "filled_plant_pot", 1);
                SkillingUtil.give(player, c.planted(), 1);
                Msg.action(player, "You plant the seed in the pot. It grows into " + Msg.article(SkillingUtil.lower(SkillingUtil.name(c.planted())))
                        + ".");
                return true;
            }
        }
        return false;
    }
}
