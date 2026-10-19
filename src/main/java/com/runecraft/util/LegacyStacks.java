package com.runecraft.util;

import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.ItemStack;

/**
 * Item stacks read back from RuneCraft's own saved data. The 1.20.1 build wrote them as {@code {id, Count, tag}};
 * Minecraft upgrades the stacks in chunks and player files but not in mod SavedData, so old ones go through the
 * item stack data fixer here (stack NBT becomes components, unknown keys land in custom_data, renamed ids resolve).
 */
public final class LegacyStacks {
    /** Data version of Minecraft 1.20.1, the version the Fabric build saved with. */
    private static final int V1_20_1 = 3465;
    /** Largest count ItemStack.CODEC accepts; the old byte Count kept up to 127. */
    private static final int CODEC_MAX_COUNT = 99;

    private LegacyStacks() {
    }

    /** Whether a stack tag is in the 1.20.1 format. */
    public static boolean isLegacy(CompoundTag tag) {
        return tag.contains("Count", Tag.TAG_ANY_NUMERIC) && !tag.contains("count");
    }

    /** The stack tag in today's format: a 1.20.1 tag is data-fixed, others are returned as they are. Extra keys (Slot) stay. */
    public static CompoundTag upgrade(CompoundTag tag) {
        if (!isLegacy(tag)) {
            return tag;
        }
        int now = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
        Tag fixed = DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, tag), V1_20_1, now).getValue();
        return fixed instanceof CompoundTag c ? c : tag;
    }

    /** A list of stack tags (e.g. {@code Inventory.save} output) in today's format. */
    public static ListTag upgrade(ListTag list) {
        ListTag out = new ListTag();
        for (int i = 0; i < list.size(); i++) {
            out.add(upgrade(list.getCompound(i)));
        }
        return out;
    }

    /**
     * A saved stack in either format (EMPTY if it can't be read). An old stack of 100-127 keeps its count, which can then be
     * above the item's stack size: hand it out with {@link Inv#give}, which splits it.
     */
    public static ItemStack parse(HolderLookup.Provider registries, CompoundTag tag) {
        CompoundTag t = upgrade(tag);
        int count = t.getInt("count");
        if (count <= CODEC_MAX_COUNT) {
            return ItemStack.parseOptional(registries, t);
        }
        t = t.copy();
        t.putInt("count", 1);
        ItemStack s = ItemStack.parseOptional(registries, t);
        if (!s.isEmpty()) {
            s.setCount(count);
        }
        return s;
    }
}
