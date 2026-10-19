package com.runecraft.content.systems;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.systems.AccessGates;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModItems;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Enforces {@link AccessGates}: every other server tick each player's OSRS tile is looked up; entering
 * a gated area without the requirements pushes the player back to their last allowed position (or the
 * gate's exit when there is none, e.g. after logging in inside) with the gate's message. Requirements are
 * checked on entry only, so a boost wearing off inside a guild doesn't throw anyone out. Also enforces
 * the Farming Guild's 65/85 tiers on its patches.
 */
public final class AccessGateService {
    private static final class State {
        Vec3 lastSafe;
        String admitted;
        long lastMessage = -1000;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();
    private static int counter;

    private AccessGateService() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(AccessGateService::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> STATES.remove(handler.player.getUUID()));
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer sp) || sp.isCreative() || !Gielinor.is(sp)) {
                return InteractionResult.PASS;
            }
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(world.getBlockState(hit.getBlockPos()).getBlock());
            int tier = AccessGates.farmingGuildTier(id.getPath());
            if (tier <= 45) {
                return InteractionResult.PASS;
            }
            BlockPos pos = hit.getBlockPos();
            AccessGates.Gate guild = AccessGates.byId("farming_guild");
            if (guild == null || !guild.contains(tileX(pos.getX() + 0.5), tileY(pos.getZ() + 0.5))) {
                return InteractionResult.PASS;
            }
            if (Players.data(sp).skills().currentLevel(Skill.FARMING) < tier) {
                Msg.game(sp, "You need a Farming level of " + tier + " to use the " + (tier >= 85 ? "advanced" : "intermediate")
                        + " tier of the Farming Guild.");
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }

    /** Fractional OSRS tile x of a block x coordinate. */
    static double tileX(double blockX) {
        return GielinorMap.ORIGIN_X + blockX / GielinorMap.SCALE;
    }

    /** Fractional OSRS tile y of a block z coordinate (tile y spans [y, y+1)). */
    static double tileY(double blockZ) {
        return GielinorMap.ORIGIN_Y + 1 - blockZ / GielinorMap.SCALE;
    }

    private static void tick(MinecraftServer server) {
        if ((counter++ & 1) != 0) {
            return;
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            try {
                check(p);
            } catch (RuntimeException e) {
                com.runecraft.RuneCraft.LOGGER.error("Access gate check failed", e);
            }
        }
    }

    private static void check(ServerPlayer p) {
        if (!Gielinor.is(p) || p.isCreative() || p.isSpectator() || !p.isAlive()) {
            STATES.remove(p.getUUID());
            return;
        }
        State st = STATES.computeIfAbsent(p.getUUID(), u -> new State());
        double x = tileX(p.getX());
        double y = tileY(p.getZ());
        AccessGates.Gate gate = AccessGates.at(x, y);
        if (gate == null) {
            st.admitted = null;
            st.lastSafe = p.position();
            return;
        }
        if (gate.id().equals(st.admitted)) {
            st.lastSafe = p.position();
            return;
        }
        String refusal = gate.check(new View(p));
        if (refusal == null) {
            st.admitted = gate.id();
            st.lastSafe = p.position();
            return;
        }
        long now = p.getServer().getTickCount();
        if (now - st.lastMessage > 40) {
            Msg.game(p, refusal);
            st.lastMessage = now;
        }
        ServerLevel world = (ServerLevel) p.level();
        Vec3 back = st.lastSafe;
        if (back == null || gate.contains(tileX(back.x), tileY(back.z))) {
            double[] out = gate.exit(x, y);
            int bx = (int) Math.floor((out[0] - GielinorMap.ORIGIN_X) * GielinorMap.SCALE);
            int bz = (int) Math.floor((GielinorMap.ORIGIN_Y + 1 - out[1]) * GielinorMap.SCALE);
            BlockPos safe = Gielinor.safeSpot(world, bx, bz);
            back = new Vec3(safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5);
            st.lastSafe = back;
        }
        if (p.isPassenger()) {
            p.stopRiding();
        }
        p.teleportTo(world, back.x, back.y, back.z, p.getYRot(), p.getXRot());
        p.setDeltaMovement(Vec3.ZERO);
        p.hurtMarked = true;
    }

    /** The requirement view of a live player. */
    private record View(ServerPlayer p) implements AccessGates.PlayerView {
        private static final EquipmentSlot[] WORN = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

        private PlayerData data() {
            return Players.data(p);
        }

        @Override
        public int level(Skill skill) {
            return data().skills().currentLevel(skill);
        }

        @Override
        public int questStage(String questId) {
            QuestInfo q = ContentRegistry.quest(questId);
            return q == null ? 0 : data().questStage(q);
        }

        @Override
        public boolean questComplete(String questId) {
            QuestInfo q = ContentRegistry.quest(questId);
            return q != null && data().questComplete(q);
        }

        @Override
        public int questPoints() {
            return data().questPoints();
        }

        @Override
        public boolean wearing(String itemId) {
            for (EquipmentSlot slot : WORN) {
                ItemStack s = p.getItemBySlot(slot);
                if (!s.isEmpty() && Inv.idOf(s).equals(itemId)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean carrying(String itemId) {
            return Inv.count(p, itemId) > 0;
        }

        @Override
        public boolean diaryComplete(String area, com.runecraft.osrs.content.systems.Diaries.Tier tier) {
            return com.runecraft.osrs.content.systems.Diaries.tierComplete(SysState.of(p).diaryDone, area, tier);
        }

        @Override
        public boolean hasCombatGear() {
            Inventory inv = p.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (s.isEmpty()) {
                    continue;
                }
                ItemDef def = ModItems.def(s);
                if (AccessGates.entranaBanned(def)) {
                    return true;
                }
            }
            return false;
        }
    }
}
