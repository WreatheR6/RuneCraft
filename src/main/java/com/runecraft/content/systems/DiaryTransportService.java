package com.runecraft.content.systems;

import com.runecraft.RuneCraft;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.GameEvents;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.systems.DiaryTransport;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import com.runecraft.world.LandmarkBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The diary transport objects ({@link DiaryTransport}): the eagles' eyries (a rope for each flight, after Eagles' Peak) and the
 * Dorgesh-Kaan train (after Another Slice of H.A.M.). Using a stop's block travels to the destination chosen for it;
 * sneak-using it picks the next destination where several leave from it, like the mushtrees.
 */
final class DiaryTransportService {
    private DiaryTransportService() {
    }

    /** The destination each player has chosen at each stop (stop id -> link index). */
    private static final Map<UUID, Map<String, Integer>> CHOSEN = new HashMap<>();

    static void register() {
        BlockHooks.register(DiaryTransport.EYRIE, (p, pos, state) -> use(p, pos, DiaryTransport.EYRIE));
        BlockHooks.register(DiaryTransport.TRAIN, (p, pos, state) -> use(p, pos, DiaryTransport.TRAIN));
        for (DiaryTransport.Stop s : DiaryTransport.STOPS) {
            LandmarkBuilder.registerCustom("diary_stop_" + s.id(), (world, centre) -> {
                if (s.under() == null) {
                    DiaryObjectsService.place(world, centre, s.block(), false);
                } else {
                    placeUnder(world, s);
                }
            });
        }
    }

    /** Puts an underground stop's block at its exact position, once the dungeon around it is carved. */
    private static void placeUnder(ServerLevel world, DiaryTransport.Stop s) {
        Block b = BuiltInRegistries.BLOCK.get(RuneCraft.id(s.block()));
        if (b == Blocks.AIR) {
            return;
        }
        BlockPos at = new BlockPos(s.under()[0], s.under()[1], s.under()[2]);
        world.getChunk(at.getX() >> 4, at.getZ() >> 4);
        com.runecraft.world.StructurePlacer.applyChunk(world, at.getX() >> 4, at.getZ() >> 4);
        if (world.getBlockState(at).isAir() && !world.getBlockState(at.below()).isAir()) {
            world.setBlock(at, b.defaultBlockState(), Block.UPDATE_CLIENTS);
        } else {
            RuneCraft.LOGGER.info("No room for {} at {}", s.block(), at);
        }
    }

    /** Uses the stop at {@code pos}: sneak picks the next destination, otherwise the player travels to the chosen one. */
    static void use(ServerPlayer p, BlockPos pos, String block) {
        DiaryTransport.Stop here = DiaryTransport.at(block, pos.getX(), pos.getY(), pos.getZ());
        if (here == null) {
            Msg.game(p, "Nothing interesting happens.");
            return;
        }
        List<DiaryTransport.Link> links = DiaryTransport.from(here.id());
        if (links.isEmpty()) {
            return;
        }
        Map<String, Integer> mine = CHOSEN.computeIfAbsent(p.getUUID(), k -> new HashMap<>());
        int index = Math.floorMod(mine.getOrDefault(here.id(), 0), links.size());
        boolean eagle = block.equals(DiaryTransport.EYRIE);
        if (p.isShiftKeyDown() && links.size() > 1) {
            index = (index + 1) % links.size();
            mine.put(here.id(), index);
            Msg.game(p, (eagle ? "The eagle will carry you to " : "The train will take you to ")
                    + DiaryTransport.stop(links.get(index).to()).name() + ". (Use it again to travel.)");
            return;
        }
        travel(p, links.get(index));
    }

    /** One trip, if the player meets its quest and carries its item. Returns whether the player travelled. */
    static boolean travel(ServerPlayer p, DiaryTransport.Link link) {
        DiaryTransport.Stop to = DiaryTransport.stop(link.to());
        boolean eagle = DiaryTransport.stop(link.from()).block().equals(DiaryTransport.EYRIE);
        QuestInfo q = link.quest() == null ? null : ContentRegistry.quest(link.quest());
        if (q != null && !QuestApi.isComplete(p, link.quest())) {
            Msg.game(p, eagle ? "The eagle ignores you. Perhaps you should help the eagles first (" + q.displayName() + ")."
                    : "You need to complete " + q.displayName() + " to ride the train.");
            return false;
        }
        if (link.item() != null && !Inv.has(p, link.item(), 1)) {
            Msg.game(p, "You need " + Msg.article(ItemCatalog.get(link.item()) == null ? link.item()
                    : ItemCatalog.get(link.item()).name().toLowerCase(java.util.Locale.ROOT)) + " to travel this way.");
            return false;
        }
        ServerLevel world = Gielinor.world(p.getServer());
        if (world == null) {
            return false;
        }
        if (link.item() != null) {
            Inv.remove(p, link.item(), 1);
        }
        BlockPos land;
        if (to.under() != null) {
            com.runecraft.world.StructurePlacer.applyChunk(world, to.under()[0] >> 4, to.under()[2] >> 4);
            land = new BlockPos(to.under()[0] - 1, to.under()[1], to.under()[2]);
        } else {
            com.runecraft.world.StructurePlacer.applyAround(world, to.blockX(), to.blockZ(), 8);
            land = Gielinor.safeSpot(world, to.blockX() + 2, to.blockZ() + 1);
        }
        p.teleportTo(world, land.getX() + 0.5, land.getY(), land.getZ() + 0.5, p.getYRot(), p.getXRot());
        world.playSound(null, land, eagle ? SoundEvents.PARROT_FLY : SoundEvents.MINECART_RIDING, SoundSource.PLAYERS, 1f, 1f);
        Msg.action(p, eagle ? "You tie your rope to the eagle's harness and it carries you to " + to.name() + "."
                : "You board the train and ride the tracks to " + to.name() + ".");
        GameEvents.fireActivity(p, DiaryTransport.key(link));
        return true;
    }
}
