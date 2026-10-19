package com.runecraft.platform.client;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;

/** Fabric's block render layer map, on vanilla's {@link ItemBlockRenderTypes} table. */
public final class BlockRenderLayerMap {
    public static final BlockRenderLayerMap INSTANCE = new BlockRenderLayerMap();

    private BlockRenderLayerMap() {
    }

    @SuppressWarnings("deprecation")
    public void putBlock(Block block, RenderType layer) {
        ItemBlockRenderTypes.setRenderLayer(block, layer);
    }
}
