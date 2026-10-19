package com.runecraft.platform.client;

import com.runecraft.platform.Event;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Fabric's tooltip callback, fired from NeoForge's {@code ItemTooltipEvent}; listeners append to {@code lines}. */
@FunctionalInterface
public interface ItemTooltipCallback {
    void getTooltip(ItemStack stack, TooltipFlag context, List<Component> lines);

    Event<ItemTooltipCallback> EVENT = new Event<>(listeners -> (stack, context, lines) -> {
        for (ItemTooltipCallback l : listeners) {
            l.getTooltip(stack, context, lines);
        }
    });
}
