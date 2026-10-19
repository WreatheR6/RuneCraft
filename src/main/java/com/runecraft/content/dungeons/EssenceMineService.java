package com.runecraft.content.dungeons;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.GameEvents;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.dungeons.DungeonBlocks;
import com.runecraft.osrs.content.dungeons.EssenceMine;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import com.runecraft.world.StructurePlacer;
import com.runecraft.platform.CommandRegistrationCallback;
import com.runecraft.platform.UseEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The Rune essence mine ({@link EssenceMine}): right-clicking one of its teleporters after Rune Mysteries offers a
 * clickable "Teleport" option next to their usual dialogue (it runs {@code /essencemine <npc>}, which needs the NPC
 * within reach); the teleport shouts the incantation, sends the player to the hub and fires the diary activity
 * {@code essence_mine:<npc id>}. A portal returns the player to the spot they were teleported from (saved in the
 * world under {@code runecraft_dungeons_essence}), i.e. back beside the NPC.
 */
public final class EssenceMineService {
    /** Blocks from the NPC within which the Teleport option works. */
    private static final double REACH = 8.0;

    private EssenceMineService() {
    }

    static void register() {
        BlockHooks.register(DungeonBlocks.ESSENCE_PORTAL, (player, pos, state) -> leave(player));
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || world.isClientSide() || player.isShiftKeyDown() || !(entity instanceof QuestNpcEntity npc)
                    || !(player instanceof ServerPlayer sp) || !EssenceMine.TELEPORTERS.containsKey(npc.npcId())) {
                return InteractionResult.PASS;
            }
            if (questDone(sp)) {
                String command = "/essencemine " + npc.npcId();
                sp.displayClientMessage(Component.literal("[Teleport to the Rune essence mine]").withStyle(s -> s.withColor(ChatFormatting.GOLD)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Teleport")))), false);
            }
            // The NPC's own dialogue (quests, shop greeting) still runs.
            return InteractionResult.PASS;
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("essencemine")
                        .then(Commands.argument("npc", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    String npcId = StringArgumentType.getString(ctx, "npc");
                                    if (p != null && nextTo(p, npcId)) {
                                        teleport(p, npcId);
                                    }
                                    return 1;
                                }))));
    }

    private static boolean questDone(ServerPlayer player) {
        return ContentRegistry.quest(EssenceMine.QUEST) == null || QuestApi.isComplete(player, EssenceMine.QUEST);
    }

    private static boolean nextTo(ServerPlayer player, String npcId) {
        if (!EssenceMine.TELEPORTERS.containsKey(npcId)) {
            return false;
        }
        boolean near = !player.level().getEntitiesOfClass(QuestNpcEntity.class, player.getBoundingBox().inflate(REACH),
                e -> npcId.equals(e.npcId())).isEmpty();
        if (!near) {
            Msg.game(player, "You need to be next to " + EssenceMine.TELEPORTERS.get(npcId) + " to be teleported.");
        }
        return near;
    }

    /**
     * Teleports the player to the mine as if {@code npcId} cast the spell (no distance check). Returns false (and tells the
     * player) if Rune Mysteries isn't complete or the NPC isn't a teleporter.
     */
    public static boolean teleport(ServerPlayer player, String npcId) {
        String name = EssenceMine.TELEPORTERS.get(npcId);
        if (name == null) {
            return false;
        }
        if (!questDone(player)) {
            Msg.game(player, name + " has no reason to trust you with the incantation. (Complete Rune Mysteries.)");
            return false;
        }
        ServerLevel world = Gielinor.world(player.getServer());
        if (world == null) {
            return false;
        }
        if (!EssenceMine.contains(GielinorMap.toOsrsX(player.getBlockX()), GielinorMap.toOsrsY(player.getBlockZ()), player.getBlockY())
                && Gielinor.is(player)) {
            state(player.getServer()).put(player.getUUID(), npcId, player.blockPosition());
        }
        QuestApi.say(player, name, EssenceMine.INCANTATION);
        int x = GielinorMap.toBlockX(EssenceMine.ARRIVE_X);
        int z = GielinorMap.toBlockZ(EssenceMine.ARRIVE_Y);
        StructurePlacer.applyAround(world, x, z, 48);
        player.stopRiding();
        player.teleportTo(world, x + 0.5, EssenceMine.FLOOR + 1, z + 0.5, player.getYRot(), player.getXRot());
        world.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.2f);
        GameEvents.fireActivity(player, "essence_mine:" + npcId.toLowerCase(Locale.ROOT));
        return true;
    }

    /** Leaves through a portal: back to where the player was teleported from (Varrock if unknown). */
    public static void leave(ServerPlayer player) {
        ServerLevel world = Gielinor.world(player.getServer());
        if (world == null) {
            return;
        }
        Return r = state(player.getServer()).get(player.getUUID());
        Msg.game(player, "You step through the portal.");
        if (r == null) {
            Gielinor.teleport(player, Landmark.VARROCK);
            return;
        }
        StructurePlacer.applyAround(world, r.pos().getX(), r.pos().getZ(), 16);
        player.stopRiding();
        player.teleportTo(world, r.pos().getX() + 0.5, r.pos().getY(), r.pos().getZ() + 0.5, player.getYRot(), player.getXRot());
    }

    /** The NPC (id) who last sent the player to the mine, or null. */
    public static String lastTeleporter(ServerPlayer player) {
        Return r = state(player.getServer()).get(player.getUUID());
        return r == null ? null : r.npcId();
    }

    private static State state(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(State::new, State::fromNbt, null), State.KEY);
    }

    record Return(String npcId, BlockPos pos) {
    }

    /** Where each player entered the mine from. */
    static final class State extends SavedData {
        static final String KEY = "runecraft_dungeons_essence";
        private final Map<UUID, Return> returns = new HashMap<>();

        Return get(UUID uuid) {
            return returns.get(uuid);
        }

        void put(UUID uuid, String npcId, BlockPos pos) {
            returns.put(uuid, new Return(npcId, pos.immutable()));
            setDirty();
        }

        @Override
        public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
            returns.forEach((uuid, r) -> {
                CompoundTag e = new CompoundTag();
                e.putString("npc", r.npcId());
                e.putLong("pos", r.pos().asLong());
                nbt.put(uuid.toString(), e);
            });
            return nbt;
        }

        static State fromNbt(CompoundTag nbt, HolderLookup.Provider registries) {
            State state = new State();
            for (String key : nbt.getAllKeys()) {
                try {
                    CompoundTag e = nbt.getCompound(key);
                    state.returns.put(UUID.fromString(key), new Return(e.getString("npc"), BlockPos.of(e.getLong("pos"))));
                } catch (IllegalArgumentException ignored) {
                    // Not a player entry.
                }
            }
            return state;
        }
    }
}
