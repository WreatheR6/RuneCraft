package com.runecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.runecraft.network.Net;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Floating OSRS hitsplats (red damage, blue zeroes) and the green/red health bar above entities. */
public final class HitsplatRenderer {
    private static final long SPLAT_MS = 1200;
    private static final long BAR_MS = 6000;

    private HitsplatRenderer() {
    }

    public static void render(WorldRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ClientState.HITSPLATS.isEmpty()) {
            return;
        }
        PoseStack matrices = context.matrixStack();
        Camera camera = context.camera();
        MultiBufferSource consumers = context.consumers();
        if (matrices == null || consumers == null) {
            return;
        }
        Vec3 cam = camera.getPosition();
        float delta = context.tickDelta();
        Font tr = mc.font;
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, List<ClientState.Hitsplat>>> it = ClientState.HITSPLATS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, List<ClientState.Hitsplat>> e = it.next();
            Entity entity = mc.level.getEntity(e.getKey());
            List<ClientState.Hitsplat> splats = e.getValue();
            if (entity == null || splats.isEmpty() || now - splats.get(splats.size() - 1).time() > BAR_MS) {
                it.remove();
                continue;
            }
            double x = Mth.lerp(delta, entity.xo, entity.getX()) - cam.x;
            double y = Mth.lerp(delta, entity.yo, entity.getY()) - cam.y + entity.getBbHeight() * 0.55;
            double z = Mth.lerp(delta, entity.zo, entity.getZ()) - cam.z;
            matrices.pushPose();
            matrices.translate(x, y, z);
            matrices.mulPose(camera.rotation());
            matrices.scale(-0.025f, -0.025f, 0.025f);
            Matrix4f m = matrices.last().pose();
            int slot = 0;
            for (ClientState.Hitsplat s : splats) {
                if (now - s.time() > SPLAT_MS) {
                    continue;
                }
                String text = String.valueOf(s.damage());
                int bg = switch (s.type()) {
                    case Net.SPLAT_MISS -> 0xE01060C0;
                    case Net.SPLAT_HEAL -> 0xE020A020;
                    case Net.SPLAT_POISON -> 0xE0309030;
                    case Net.SPLAT_VENOM -> 0xE0105020;
                    default -> 0xE0B01010;
                };
                float tx = -tr.width(text) / 2f + (slot % 2 == 0 ? 0 : 12) * (slot > 0 ? 1 : 0);
                float ty = (slot / 2) * -11;
                tr.drawInBatch(text, tx, ty, 0xFFFFFFFF, false, m, consumers, Font.DisplayMode.SEE_THROUGH, bg,
                        LightTexture.FULL_BRIGHT);
                slot++;
            }
            // Health bar above the head.
            float ratio = splats.get(splats.size() - 1).hpRatio();
            int full = 8;
            int green = Math.round(full * ratio);
            String redBar = " ".repeat(full);
            float barY = -(entity.getBbHeight() * 0.45f) / 0.025f - 4;
            tr.drawInBatch(redBar, -tr.width(redBar) / 2f, barY, 0, false, m, consumers, Font.DisplayMode.SEE_THROUGH,
                    0xE0C00000, LightTexture.FULL_BRIGHT);
            if (green > 0) {
                tr.drawInBatch(" ".repeat(green), -tr.width(redBar) / 2f, barY, 0, false, m, consumers, Font.DisplayMode.SEE_THROUGH,
                        0xE000C000, LightTexture.FULL_BRIGHT);
            }
            matrices.popPose();
        }
        if (consumers instanceof MultiBufferSource.BufferSource immediate) {
            immediate.endBatch();
        }
    }
}
