package com.runecraft.client;

import com.mojang.math.Axis;
import com.runecraft.network.Net;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.PrayerGrace;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Combat feedback on the HUD (round 41 playability):
 * <ul>
 *   <li>under the HP and prayer orbs, the active overhead as a style-coloured tag plus the initials of the other active
 *   prayers, flashing for 0.5 s when they change;</li>
 *   <li>the hits the player takes as a hitsplat stack just below and right of the crosshair (first person can't see the
 *   splats over its own body): red damage, blue 0, and a BLOCKED splat (blue 0 with the overhead's tag);</li>
 *   <li>a red chevron on a ring round the crosshair pointing at an attacker out of view, for a second.</li>
 * </ul>
 * OSRS shows hitsplats on the player model, which its top-down camera always shows.
 */
public final class CombatFeedbackHud {
    private static final long SPLAT_MS = 1200;
    private static final int MAX_SPLATS = 4;
    private static final long FLASH_MS = 500;
    private static final long CHEVRON_MS = 1000;
    /** Degrees off the look direction past which an attacker counts as out of view. */
    private static final float OFF_SCREEN_DEG = 50f;

    /** Where HudOverlay leaves room for the prayer row (GUI coordinates), set every frame; -1 when there is none. */
    static int prayerRowX = -1;
    static int prayerRowY = -1;

    private static int lastMask;
    private static long maskChangedAt;

    private CombatFeedbackHud() {
    }

    /** A hitsplat landed on the local player. */
    static void selfSplat(ClientState.Hitsplat splat) {
        ClientState.SELF_SPLATS.add(splat);
        while (ClientState.SELF_SPLATS.size() > MAX_SPLATS) {
            ClientState.SELF_SPLATS.remove(0);
        }
    }

    /** True when the prayer row should be drawn (some prayer on, or the flash of a change still running). */
    static boolean prayerRowShown() {
        return ClientState.prayerMask != 0 || System.currentTimeMillis() - maskChangedAt < FLASH_MS;
    }

    public static void render(GuiGraphics ctx, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        long now = System.currentTimeMillis();
        if (ClientState.prayerMask != lastMask) {
            lastMask = ClientState.prayerMask;
            maskChangedAt = now;
        }
        Font tr = mc.font;
        if (prayerRowX >= 0 && prayerRowShown()) {
            prayerRow(ctx, tr, prayerRowX, prayerRowY, now);
        }
        selfSplats(ctx, tr, mc, now);
    }

    /** The overhead tag (coloured box) and the initials of the other active prayers, 88 x 12 like the orbs. */
    private static void prayerRow(GuiGraphics ctx, Font tr, int x, int y, long now) {
        boolean flash = now - maskChangedAt < FLASH_MS;
        ctx.fill(x, y, x + 88, y + 12, flash ? 0xC0605030 : 0xA0201810);
        if (flash) {
            ctx.renderOutline(x, y, 88, 12, 0xFFFFFFFF);
        }
        int tx = x + 2;
        StringBuilder rest = new StringBuilder();
        for (Prayer p : Prayer.values()) {
            if (!ClientState.prayerActive(p)) {
                continue;
            }
            String tag = PrayerGrace.overheadTag(p);
            if (tag != null) {
                int w = tr.width(tag) + 4;
                ctx.fill(tx, y + 1, tx + w, y + 11, PrayerGrace.overheadColor(p));
                ctx.drawString(tr, tag, tx + 2, y + 2, 0xFFFFFFFF);
                tx += w + 2;
            } else {
                if (rest.length() > 0) {
                    rest.append(' ');
                }
                rest.append(SidePanel.initials(p.displayName()));
            }
        }
        String text = rest.toString();
        int room = x + 86 - tx;
        if (tr.width(text) > room) {
            text = tr.plainSubstrByWidth(text, Math.max(0, room - 6)) + "..";
        }
        ctx.drawString(tr, text, tx + 1, y + 2, 0xFFE0E0A0);
    }

    private static void selfSplats(GuiGraphics ctx, Font tr, Minecraft mc, long now) {
        List<ClientState.Hitsplat> splats = ClientState.SELF_SPLATS;
        splats.removeIf(s -> now - s.time() > SPLAT_MS);
        if (splats.isEmpty()) {
            return;
        }
        int cx = ctx.guiWidth() / 2;
        int cy = ctx.guiHeight() / 2;
        int x = cx + 12;
        int y = cy + 4;
        List<ClientState.Hitsplat> newestFirst = new ArrayList<>(splats);
        java.util.Collections.reverse(newestFirst);
        for (ClientState.Hitsplat s : newestFirst) {
            long age = now - s.time();
            int alpha = age > SPLAT_MS - 300 ? (int) (255 * (SPLAT_MS - age) / 300.0) : 255;
            alpha = Mth.clamp(alpha, 16, 255);
            String text = String.valueOf(s.damage());
            int bg = switch (s.type()) {
                case Net.SPLAT_MISS, Net.SPLAT_BLOCKED -> 0x1060C0;
                case Net.SPLAT_HEAL -> 0x20A020;
                case Net.SPLAT_POISON -> 0x309030;
                case Net.SPLAT_VENOM -> 0x105020;
                default -> 0xB01010;
            };
            int w = tr.width(text) + 6;
            ctx.fill(x, y, x + w, y + 11, (alpha * 7 / 8) << 24 | bg);
            ctx.drawString(tr, text, x + 3, y + 2, alpha << 24 | 0xFFFFFF);
            if (s.type() == Net.SPLAT_BLOCKED) {
                // BLOCKED: the 0 plus the tag of the overhead that took it.
                Prayer overhead = activeProtection();
                String tag = overhead == null ? "BLOCKED" : PrayerGrace.overheadTag(overhead) + " BLOCKED";
                int color = overhead == null ? 0xFFFFFF : PrayerGrace.overheadColor(overhead) & 0xFFFFFF;
                ctx.drawString(tr, tag, x + w + 3, y + 2, alpha << 24 | color);
            }
            y += 12;
        }
        chevron(ctx, tr, mc, now, newestFirst.get(0), cx, cy);
    }

    private static Prayer activeProtection() {
        for (Prayer p : PrayerGrace.PROTECTIONS) {
            if (ClientState.prayerActive(p)) {
                return p;
            }
        }
        return null;
    }

    /** Points at the attacker of the newest hit when it's out of view. */
    private static void chevron(GuiGraphics ctx, Font tr, Minecraft mc, long now, ClientState.Hitsplat newest, int cx, int cy) {
        if (newest.attacker() < 0 || now - newest.time() > CHEVRON_MS || mc.level == null) {
            return;
        }
        Entity attacker = mc.level.getEntity(newest.attacker());
        if (attacker == null || attacker == mc.player) {
            return;
        }
        double dx = attacker.getX() - mc.player.getX();
        double dz = attacker.getZ() - mc.player.getZ();
        float yawTo = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float delta = Mth.wrapDegrees(yawTo - mc.player.getYRot());
        if (Math.abs(delta) < OFF_SCREEN_DEG) {
            return;
        }
        double r = 34;
        double rad = Math.toRadians(delta);
        float px = (float) (cx + Math.sin(rad) * r);
        float py = (float) (cy - Math.cos(rad) * r);
        ctx.pose().pushPose();
        ctx.pose().translate(px, py, 0);
        ctx.pose().mulPose(Axis.ZP.rotationDegrees(delta));
        ctx.pose().scale(1.5f, 1.5f, 1f);
        ctx.drawString(tr, "^", -tr.width("^") / 2, -4, 0xFFFF3030);
        ctx.pose().popPose();
    }
}
