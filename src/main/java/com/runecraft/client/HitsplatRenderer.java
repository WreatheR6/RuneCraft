package com.runecraft.client;

import com.runecraft.network.Net;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
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
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || ClientState.HITSPLATS.isEmpty()) {
            return;
        }
        MatrixStack matrices = context.matrixStack();
        Camera camera = context.camera();
        VertexConsumerProvider consumers = context.consumers();
        if (matrices == null || consumers == null) {
            return;
        }
        Vec3d cam = camera.getPos();
        float delta = context.tickDelta();
        TextRenderer tr = mc.textRenderer;
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, List<ClientState.Hitsplat>>> it = ClientState.HITSPLATS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, List<ClientState.Hitsplat>> e = it.next();
            Entity entity = mc.world.getEntityById(e.getKey());
            List<ClientState.Hitsplat> splats = e.getValue();
            if (entity == null || splats.isEmpty() || now - splats.get(splats.size() - 1).time() > BAR_MS) {
                it.remove();
                continue;
            }
            double x = MathHelper.lerp(delta, entity.prevX, entity.getX()) - cam.x;
            double y = MathHelper.lerp(delta, entity.prevY, entity.getY()) - cam.y + entity.getHeight() * 0.55;
            double z = MathHelper.lerp(delta, entity.prevZ, entity.getZ()) - cam.z;
            matrices.push();
            matrices.translate(x, y, z);
            matrices.multiply(camera.getRotation());
            matrices.scale(-0.025f, -0.025f, 0.025f);
            Matrix4f m = matrices.peek().getPositionMatrix();
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
                float tx = -tr.getWidth(text) / 2f + (slot % 2 == 0 ? 0 : 12) * (slot > 0 ? 1 : 0);
                float ty = (slot / 2) * -11;
                tr.draw(text, tx, ty, 0xFFFFFFFF, false, m, consumers, TextRenderer.TextLayerType.SEE_THROUGH, bg,
                        LightmapTextureManager.MAX_LIGHT_COORDINATE);
                slot++;
            }
            // Health bar above the head.
            float ratio = splats.get(splats.size() - 1).hpRatio();
            int full = 8;
            int green = Math.round(full * ratio);
            String redBar = " ".repeat(full);
            float barY = -(entity.getHeight() * 0.45f) / 0.025f - 4;
            tr.draw(redBar, -tr.getWidth(redBar) / 2f, barY, 0, false, m, consumers, TextRenderer.TextLayerType.SEE_THROUGH,
                    0xE0C00000, LightmapTextureManager.MAX_LIGHT_COORDINATE);
            if (green > 0) {
                tr.draw(" ".repeat(green), -tr.getWidth(redBar) / 2f, barY, 0, false, m, consumers, TextRenderer.TextLayerType.SEE_THROUGH,
                        0xE000C000, LightmapTextureManager.MAX_LIGHT_COORDINATE);
            }
            matrices.pop();
        }
        if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }
    }
}
