package com.runecraft.platform.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;

/** What a world render listener gets: the stage's pose stack (camera-relative), the camera, a buffer source and the partial tick. */
public record WorldRenderContext(PoseStack matrixStack, Camera camera, MultiBufferSource consumers, float tickDelta) {
}
