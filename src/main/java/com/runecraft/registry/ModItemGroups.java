package com.runecraft.registry;

import com.runecraft.RuneCraft;
import com.runecraft.osrs.ItemDef;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

public final class ModItemGroups {
    private ModItemGroups() {
    }

    private static boolean isEquipment(ItemDef def) {
        return def != null && def.equipment() != null;
    }

    public static void register() {
        Registry.register(Registries.ITEM_GROUP, RuneCraft.id("items"), FabricItemGroup.builder()
                .icon(() -> new ItemStack(ModItems.get("air_rune")))
                .displayName(Text.translatable("itemGroup.runecraft.items"))
                .entries((context, entries) -> {
                    for (Item item : ModItems.all()) {
                        if (!isEquipment(ModItems.def(item))) {
                            entries.add(item);
                        }
                    }
                }).build());
        Registry.register(Registries.ITEM_GROUP, RuneCraft.id("equipment"), FabricItemGroup.builder()
                .icon(() -> new ItemStack(ModItems.get("rune_scimitar")))
                .displayName(Text.translatable("itemGroup.runecraft.equipment"))
                .entries((context, entries) -> {
                    for (Item item : ModItems.all()) {
                        if (isEquipment(ModItems.def(item))) {
                            entries.add(item);
                        }
                    }
                }).build());
        Registry.register(Registries.ITEM_GROUP, RuneCraft.id("blocks"), FabricItemGroup.builder()
                .icon(() -> new ItemStack(ModBlocks.TREES.values().iterator().next()))
                .displayName(Text.translatable("itemGroup.runecraft.blocks"))
                .entries((context, entries) -> ModBlocks.items().forEach(entries::add)).build());
    }
}
