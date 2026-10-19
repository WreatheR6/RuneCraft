package com.runecraft.client.content.skilling;

import com.runecraft.client.content.ClientModuleHooks;
import com.runecraft.content.skilling.SeaNet;
import com.runecraft.osrs.content.skilling.RunecraftData;
import com.runecraft.platform.client.ClientTickEvents;
import com.runecraft.platform.client.ItemTooltipCallback;
import com.runecraft.platform.client.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side behaviour for the skilling content module: tooltips for essence pouches and poisoned weapons, and the
 * Sailing hull speed (the rider's client moves the boat, so the server sends the velocity multiplier of its hull).
 */
public final class SkillingClientHooks implements ClientModuleHooks {
    /** Per-tick velocity multiplier of the boat being ridden (1 = vanilla). */
    private static double boatSpeed = 1;
    /** The entity id of the boat the hull speed belongs to (-1: none). */
    private static int boatId = -1;
    private static boolean riding;

    @Override
    public void initClient() {
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!id.getNamespace().equals("runecraft")) {
                return;
            }
            CompoundTag nbt = stack.getTag();
            RunecraftData.Pouch pouch = RunecraftData.pouch(id.getPath());
            if (pouch != null) {
                int held = nbt == null ? 0 : nbt.getInt("Essence");
                lines.add(Component.literal("Essence: " + held + "/" + pouch.capacity()).withStyle(ChatFormatting.GRAY));
            }
            if (nbt != null && nbt.contains("RunecraftPoison")) {
                lines.add(Component.literal("Poisoned (" + nbt.getInt("RunecraftPoison") + ")").withStyle(ChatFormatting.DARK_GREEN));
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(SeaNet.BOAT_SPEED, (client, handler, buf, sender) -> {
            double speed = buf.readDouble();
            int id = buf.readInt();
            client.execute(() -> {
                boatSpeed = speed;
                boatId = id;
            });
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || boatSpeed == 1) {
                return;
            }
            Entity vehicle = client.player.getVehicle();
            if (vehicle == null) {
                // The speed can arrive just before the mount does; reset only after a ride has ended.
                if (riding) {
                    riding = false;
                    boatSpeed = 1;
                }
                return;
            }
            riding = true;
            // Only the Sailing boat the server named, and only while it floats (not sliding over ice or land).
            if (vehicle instanceof Boat && vehicle.getId() == boatId && vehicle.isControlledByLocalInstance()
                    && (vehicle.isInWater() || vehicle.level().getFluidState(vehicle.blockPosition()).is(FluidTags.WATER)
                    || vehicle.level().getFluidState(vehicle.blockPosition().below()).is(FluidTags.WATER))) {
                Vec3 v = vehicle.getDeltaMovement();
                vehicle.setDeltaMovement(v.x * boatSpeed, v.y, v.z * boatSpeed);
            }
        });
    }
}
