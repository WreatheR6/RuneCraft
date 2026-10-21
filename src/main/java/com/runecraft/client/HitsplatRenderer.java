package com.runecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.runecraft.network.Net;
import com.runecraft.platform.client.WorldRenderContext;
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

/**
 * Floating OSRS hitsplats (red damage, blue zeroes) and the green/red health bar above entities.
 *
 * <p>// playability (round 41): splats grow with distance (up to 3x at 18 blocks) so they stay readable, misses and
 * blocks are 1.2x larger, the local player's own hits get a white outline, and in first person the splats on the
 * player's own body (out of view) are drawn by {@link CombatFeedbackHud} beside the crosshair instead.
 */
public final class HitsplatRenderer {
    private static final long SPLAT_MS = 1200;
    private static final long BAR_MS = 6000;
    private static final float BASE_SCALE = 0.025f;

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
        int self = mc.player == null ? Integer.MIN_VALUE : mc.player.getId();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        Iterator<Map.Entry<Integer, List<ClientState.Hitsplat>>> it = ClientState.HITSPLATS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, List<ClientState.Hitsplat>> e = it.next();
            Entity entity = mc.level.getEntity(e.getKey());
            List<ClientState.Hitsplat> splats = e.getValue();
            if (entity == null || splats.isEmpty() || now - splats.get(splats.size() - 1).time() > BAR_MS) {
                it.remove();
                continue;
            }
            if (firstPerson && entity.getId() == self) {
                continue; // drawn on the HUD (CombatFeedbackHud)
            }
            double x = Mth.lerp(delta, entity.xo, entity.getX()) - cam.x;
            double y = Mth.lerp(delta, entity.yo, entity.getY()) - cam.y + entity.getBbHeight() * 0.55;
            double z = Mth.lerp(delta, entity.zo, entity.getZ()) - cam.z;
            float scale = BASE_SCALE * (float) Mth.clamp(Math.sqrt(x * x + y * y + z * z) / 6.0, 1.0, 3.0);
            matrices.pushPose();
            matrices.translate(x, y, z);
            matrices.mulPose(camera.rotation());
            matrices.scale(-scale, -scale, scale);
            int slot = 0;
            for (ClientState.Hitsplat s : splats) {
                if (now - s.time() > SPLAT_MS) {
                    continue;
                }
                String text = String.valueOf(s.damage());
                int bg = switch (s.type()) {
                    case Net.SPLAT_MISS, Net.SPLAT_BLOCKED -> 0xE01060C0;
                    case Net.SPLAT_HEAL -> 0xE020A020;
                    case Net.SPLAT_POISON -> 0xE0309030;
                    case Net.SPLAT_VENOM -> 0xE0105020;
                    default -> 0xE0B01010;
                };
                boolean zero = s.type() == Net.SPLAT_MISS || s.type() == Net.SPLAT_BLOCKED;
                float tx = -tr.width(text) / 2f + (slot % 2 == 0 ? 0 : 12) * (slot > 0 ? 1 : 0);
                float ty = (slot / 2) * -11;
                matrices.pushPose();
                if (zero) {
                    matrices.scale(1.2f, 1.2f, 1f);
                }
                Matrix4f m = matrices.last().pose();
                if (s.attacker() == self && self != Integer.MIN_VALUE) {
                    // The player's own hits: a white frame behind the splat.
                    String pad = " " + text + " ";
                    float px = tx - tr.width(" ");
                    for (int[] d : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
                        tr.drawInBatch(pad, px + d[0], ty + d[1], 0x00FFFFFF, false, m, consumers, Font.DisplayMode.SEE_THROUGH,
                                0xF0FFFFFF, LightTexture.FULL_BRIGHT);
                    }
                }
                tr.drawInBatch(text, tx, ty, 0xFFFFFFFF, false, m, consumers, Font.DisplayMode.SEE_THROUGH, bg,
                        LightTexture.FULL_BRIGHT);
                matrices.popPose();
                slot++;
            }
            // Health bar above the head.
            Matrix4f m = matrices.last().pose();
            float ratio = splats.get(splats.size() - 1).hpRatio();
            int full = 8;
            int green = Math.round(full * ratio);
            String redBar = " ".repeat(full);
            float barY = -(entity.getBbHeight() * 0.45f) / scale - 4;
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
