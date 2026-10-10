package com.runecraft.client.content.skilling;

import com.runecraft.client.content.ClientModuleHooks;
import com.runecraft.osrs.content.skilling.RunecraftData;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/** Client-side behaviour for the skilling content module: tooltips for essence pouches and poisoned weapons. */
public final class SkillingClientHooks implements ClientModuleHooks {
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
    }
}
