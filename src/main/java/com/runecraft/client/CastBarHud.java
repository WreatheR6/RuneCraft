package com.runecraft.client;

import com.runecraft.network.HudNet;
import com.runecraft.network.HudProbe;
import com.runecraft.osrs.CastBar;
import com.runecraft.osrs.PrayerGrace;
import com.runecraft.platform.client.ClientPlayNetworking;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * The telegraph cast bar (round 42, {@code // playability}; rules in {@link CastBar}): one row per telegraphed attack on its
 * way to the player, stacked soonest first under the centre-screen warning line. Each row shows the style tag in its colour
 * (melee red, ranged green, magic blue, area orange), the attacker, the key of the protection prayer that blocks it (under
 * the Nightmare's curse the key that really switches that prayer, in purple), a seconds countdown and a bar emptying until
 * the hit lands. The frame is green while the right overhead is up, red while it isn't, orange when no prayer helps (a spot
 * to step out of: "MOVE"; a script's unprayable cast: "no prayer"). A hit the prayer only lowers (dragonfire, a
 * prayer-piercing hit: {@link CastBar#FLAG_REDUCED}) shows its key with "part". OSRS shows these telegraphs only as the
 * monster's animation.
 */
public final class CastBarHud {
    private static final int WIDTH = 140;
    private static final int ROW = 17;
    /** Below the vanilla subtitle (drawn at 2x scale just under the screen centre). */
    private static final int TOP_BELOW_CENTRE = 32;
    private static final List<CastBar.Cast> CASTS = new ArrayList<>();

    private CastBarHud() {
    }

    static void init() {
        ClientPlayNetworking.registerGlobalReceiver(HudNet.CAST, (client, handler, buf, sender) -> {
            int source = buf.readVarInt();
            int style = buf.readByte();
            int flags = buf.readByte();
            int landIn = buf.readVarInt();
            String label = buf.readUtf(HudNet.LABEL_MAX * 4);
            client.execute(() -> CastBar.add(CASTS, source, style, flags, CombatHudClient.clientTick(), landIn, label));
        });
    }

    static void reset() {
        CASTS.clear();
    }

    /** Every client tick: drops landed casts and the casts of attackers that died or left (a dead caster's telegraph never lands). */
    static void tick(Minecraft mc) {
        long now = CombatHudClient.clientTick();
        CastBar.expire(CASTS, now);
        if (mc.level != null) {
            CASTS.removeIf(c -> {
                if (c.source() < 0) {
                    return false;
                }
                Entity e = mc.level.getEntity(c.source());
                return e == null ? now - c.startMc() > SOURCE_GRACE_TICKS : e instanceof LivingEntity l && !l.isAlive();
            });
        }
    }

    /** Ticks a cast waits for its source entity to be known before a missing source counts as gone. */
    private static final int SOURCE_GRACE_TICKS = 10;

    /** The active protection overhead (index into {@link PrayerGrace#PROTECTIONS}), or -1. */
    static int activeProtection() {
        for (int i = 0; i < PrayerGrace.PROTECTIONS.length; i++) {
            if (ClientState.prayerActive(PrayerGrace.PROTECTIONS[i])) {
                return i;
            }
        }
        return -1;
    }

    /** The name a row shows: the label, else the attacker's name. */
    private static String name(Minecraft mc, CastBar.Cast c) {
        if (!c.label().isEmpty()) {
            return c.label();
        }
        Entity e = mc.level == null || c.source() < 0 ? null : mc.level.getEntity(c.source());
        return e == null ? "Attack" : e.getName().getString();
    }

    /**
     * The key text of a row: the protection key (cursed: the key that reaches it; "part" after it when the prayer only lowers
     * the hit), MOVE, or "no prayer".
     */
    private static String key(CastBar.Cast c) {
        if ((c.flags() & CastBar.FLAG_DODGE) != 0 || c.style() == CastBar.TYPELESS) {
            return "MOVE";
        }
        if (!c.prayable()) {
            return "no prayer";
        }
        String key = CombatKeys.keyName(CastBar.keyFor(c.protection(), ClientState.CURSE));
        return c.reduced() ? key + CastBar.REDUCED_SUFFIX : key;
    }

    /** Every row held, soonest first, as the probe reports them (self-tests; only the first {@link CastBar#MAX_SHOWN} draw). */
    static List<HudProbe.Cast> probe(Minecraft mc) {
        List<HudProbe.Cast> out = new ArrayList<>();
        long now = CombatHudClient.clientTick();
        int active = activeProtection();
        for (CastBar.Cast c : CastBar.shown(CASTS, Integer.MAX_VALUE)) {
            out.add(new HudProbe.Cast(c.source(), name(mc, c), c.style(), c.protection(), key(c), CastBar.rightOverhead(c, active),
                    c.left(now) / 20f));
        }
        return out;
    }

    public static void render(GuiGraphics g, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || CASTS.isEmpty()) {
            return;
        }
        Font tr = mc.font;
        float now = CombatHudClient.clientTick() + tickDelta;
        int x = g.guiWidth() / 2 - WIDTH / 2;
        int y = g.guiHeight() / 2 + TOP_BELOW_CENTRE;
        // Rows stop above the action bar line (68 px over the bottom), so the stack never covers the hotbar.
        int room = Math.max(1, (g.guiHeight() - 72 - y) / ROW);
        int active = activeProtection();
        boolean cursed = CastBar.cursed(ClientState.CURSE);
        for (CastBar.Cast c : CastBar.shown(CASTS, Math.min(CastBar.MAX_SHOWN, room))) {
            row(g, tr, mc, c, x, y, now, active, cursed);
            y += ROW;
        }
    }

    private static void row(GuiGraphics g, Font tr, Minecraft mc, CastBar.Cast c, int x, int y, float now, int active, boolean cursed) {
        Boolean right = CastBar.rightOverhead(c, active);
        long left = c.left((long) now);
        boolean blink = right != null && !right && left < 12 && ((long) now / 3) % 2 == 0; // the last 0.6 s with the wrong prayer
        int frame = right == null ? 0xFFFFA020 : right ? 0xFF30D030 : blink ? 0xFFFFFFFF : 0xFFE03030;
        int h = ROW - 2;
        g.fill(x, y, x + WIDTH, y + h, 0xC0101010);
        g.renderOutline(x, y, WIDTH, h, frame);
        int rgb = 0xFF000000 | c.rgb();
        String tag = CastBar.tag(c.style());
        g.fill(x + 2, y + 2, x + 26, y + 11, rgb);
        g.drawString(tr, tag, x + 14 - tr.width(tag) / 2, y + 3, 0xFFFFFFFF, false);
        String secs = CastBar.seconds(left);
        String key = key(c);
        boolean keyCursed = cursed && c.prayable();
        int secsX = x + WIDTH - 3 - tr.width(secs);
        int keyW = tr.width(key) + 4;
        int keyX = secsX - 4 - keyW;
        String name = name(mc, c);
        int nameRoom = keyX - (x + 30) - 2;
        if (tr.width(name) > nameRoom) {
            name = tr.plainSubstrByWidth(name, Math.max(0, nameRoom - 6)) + "..";
        }
        g.drawString(tr, name, x + 30, y + 3, 0xFFFFFFFF, false);
        g.fill(keyX, y + 2, keyX + keyW, y + 11, keyCursed ? 0xFF7030A0 : 0xFF404040);
        g.drawString(tr, key, keyX + 2, y + 3, 0xFFFFFF60, false);
        g.drawString(tr, secs, secsX, y + 3, left < 12 ? 0xFFFF6060 : 0xFFE0E0E0, false);
        // The bar empties until the hit lands.
        float remaining = 1f - c.progress(now);
        int barW = Math.round((WIDTH - 4) * remaining);
        g.fill(x + 2, y + h - 3, x + WIDTH - 2, y + h - 1, 0xFF303030);
        g.fill(x + 2, y + h - 3, x + 2 + barW, y + h - 1, rgb);
    }
}
