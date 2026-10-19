package com.runecraft.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

/**
 * RuneCraft's per-stack data, kept in the stack's {@code minecraft:custom_data} component (Minecraft 1.20.5+ has no
 * free-form stack NBT). Replaces the old {@code stack.getTag()} / {@code getOrCreateTag()} / {@code setTag()} calls:
 * reads return a copy, writes go through {@link #set} or {@link #update}.
 */
public final class StackData {
    private StackData() {
    }

    /** A copy of the stack's custom data; an empty tag when it has none. */
    public static CompoundTag get(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    /** Whether the stack has any custom data. */
    public static boolean has(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && !data.isEmpty();
    }

    /** Whether the stack's custom data has the key. */
    public static boolean contains(ItemStack stack, String key) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(key);
    }

    /** Replaces the stack's custom data; an empty or null tag removes it. */
    public static void set(ItemStack stack, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    /** Edits the stack's custom data in place (on a copy that is then stored). */
    public static void update(ItemStack stack, Consumer<CompoundTag> edit) {
        CompoundTag tag = get(stack);
        edit.accept(tag);
        set(stack, tag);
    }

    /** Removes one key from the stack's custom data. */
    public static void remove(ItemStack stack, String key) {
        update(stack, tag -> tag.remove(key));
    }
}
