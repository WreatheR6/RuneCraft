package com.runecraft.registry;

import com.runecraft.RuneCraft;
import com.runecraft.osrs.ItemDef;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ModItemGroups {
    private ModItemGroups() {
    }

    private static boolean isEquipment(ItemDef def) {
        return def != null && def.equipment() != null;
    }

    public static void register() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, RuneCraft.id("items"), FabricItemGroup.builder()
                .icon(() -> new ItemStack(ModItems.get("air_rune")))
                .title(Component.translatable("itemGroup.runecraft.items"))
                .displayItems((context, entries) -> {
                    for (Item item : ModItems.all()) {
                        if (!isEquipment(ModItems.def(item))) {
                            entries.accept(item);
                        }
                    }
                }).build());
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, RuneCraft.id("equipment"), FabricItemGroup.builder()
                .icon(() -> new ItemStack(ModItems.get("rune_scimitar")))
                .title(Component.translatable("itemGroup.runecraft.equipment"))
                .displayItems((context, entries) -> {
                    for (Item item : ModItems.all()) {
                        if (isEquipment(ModItems.def(item))) {
                            entries.accept(item);
                        }
                    }
                }).build());
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, RuneCraft.id("blocks"), FabricItemGroup.builder()
                .icon(() -> new ItemStack(ModBlocks.TREES.values().iterator().next()))
                .title(Component.translatable("itemGroup.runecraft.blocks"))
                .displayItems((context, entries) -> ModBlocks.items().forEach(entries::accept)).build());
    }
}
