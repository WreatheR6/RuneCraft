package com.runecraft.client.content.skilling;

import com.runecraft.client.content.ClientModuleHooks;
import com.runecraft.content.skilling.SeaNet;
import com.runecraft.osrs.content.skilling.RunecraftData;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * Client-side behaviour for the skilling content module: tooltips for essence pouches and poisoned weapons, and the
 * Sailing hull speed (the rider's client moves the boat, so the server sends the velocity multiplier of its hull).
 */
public final class SkillingClientHooks implements ClientModuleHooks {
    /** Per-tick velocity multiplier of the boat being ridden (1 = vanilla). */
    private static double boatSpeed = 1;
    private static boolean riding;

    @Override
    public void initClient() {
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            Identifier id = Registries.ITEM.getId(stack.getItem());
            if (!id.getNamespace().equals("runecraft")) {
                return;
            }
            NbtCompound nbt = stack.getNbt();
            RunecraftData.Pouch pouch = RunecraftData.pouch(id.getPath());
            if (pouch != null) {
                int held = nbt == null ? 0 : nbt.getInt("Essence");
                lines.add(Text.literal("Essence: " + held + "/" + pouch.capacity()).formatted(Formatting.GRAY));
            }
            if (nbt != null && nbt.contains("RunecraftPoison")) {
                lines.add(Text.literal("Poisoned (" + nbt.getInt("RunecraftPoison") + ")").formatted(Formatting.DARK_GREEN));
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(SeaNet.BOAT_SPEED, (client, handler, buf, sender) -> {
            double speed = buf.readDouble();
            client.execute(() -> boatSpeed = speed);
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
            if (vehicle instanceof BoatEntity && vehicle.isLogicalSideForUpdatingMovement()) {
                Vec3d v = vehicle.getVelocity();
                vehicle.setVelocity(v.x * boatSpeed, v.y, v.z * boatSpeed);
            }
        });
    }
}
