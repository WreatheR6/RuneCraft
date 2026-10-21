package com.runecraft.client;

import com.runecraft.network.HudNet;
import com.runecraft.osrs.content.minigames.SolHeredit;
import com.runecraft.platform.PacketByteBufs;
import com.runecraft.platform.client.ClientPlayConnectionEvents;
import com.runecraft.platform.client.ClientPlayNetworking;
import com.runecraft.platform.client.HudRenderCallback;
import java.util.Locale;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Sol Heredit's grapple on the HUD (round 43, {@code // playability}): when he calls one ({@link HudNet#GRAPPLE}) a prompt
 * in the middle of the screen reads "GRAPPLE: press &lt;key&gt; for body / back / hands / legs / feet" with the called slot
 * lit in gold and a bar emptying over his 3.6 s window; a grapple key ({@link CombatKeys#grapple}, rebindable, 1-5 by
 * default) answers it ({@link HudNet#GRAPPLE_ANSWER}); the server keeps the window, the one answer and the perfect parry on
 * its last tick. The keys only act while the prompt is up, and then the hotbar slot sharing a key doesn't move, so 1-5 can
 * stay the defaults. Round 42 moved the held slot to 9 and read the hotbar keys instead. OSRS: click the slot in the
 * worn-equipment tab within 4 ticks.
 */
public final class GrappleHud {
    private static final int WIDTH = 240;
    /** Above the crosshair, clear of the warning subtitle and the cast bar's rows below it. */
    private static final int TOP_ABOVE_CENTRE = 64;
    /** Client ticks the prompt stays after its window (a late packet still shows "too late"). */
    private static final int LINGER_TICKS = 4;

    private static int called = -1;
    private static long startTick;
    private static int windowTicks;
    /** The slot the player answered with this grapple (-1: not yet), shown until the prompt goes. */
    private static int answered = -1;

    private GrappleHud() {
    }

    static void init() {
        ClientPlayNetworking.registerGlobalReceiver(HudNet.GRAPPLE, (client, handler, buf, sender) -> {
            int slot = buf.readByte();
            int window = buf.readVarInt();
            client.execute(() -> onPrompt(slot, window));
        });
        HudRenderCallback.EVENT.register(GrappleHud::render);
        // Before vanilla handles its keys this tick: the grapple keys answer, and the hotbar slot on the same key stays put.
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Pre.class, e -> beforeKeys(Minecraft.getInstance()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }

    static void reset() {
        called = -1;
        answered = -1;
    }

    private static void onPrompt(int slot, int window) {
        if (slot < 0) {
            called = -1;
            answered = -1;
            return;
        }
        called = slot;
        startTick = CombatHudClient.clientTick();
        windowTicks = Math.max(1, window);
        answered = -1;
    }

    /** Whether the prompt is up: a grapple called and its window (plus a short linger) not yet over. */
    public static boolean active() {
        return called >= 0 && CombatHudClient.clientTick() - startTick <= windowTicks + LINGER_TICKS;
    }

    /** Client ticks left in the window. */
    static long left() {
        return Math.max(0, startTick + windowTicks - CombatHudClient.clientTick());
    }

    /** The slot called (a {@link SolHeredit.Grapple} ordinal), or -1 when no prompt is up. */
    public static int called() {
        return active() ? called : -1;
    }

    /** The prompt's text as drawn: "GRAPPLE: press 1 body, 2 back, 3 hands, 4 legs, 5 feet" with the bound keys. */
    public static String promptText() {
        StringBuilder sb = new StringBuilder("GRAPPLE: press");
        SolHeredit.Grapple[] all = SolHeredit.Grapple.values();
        for (int i = 0; i < all.length; i++) {
            sb.append(i == 0 ? " " : ", ").append(CombatKeys.grappleKeyName(i)).append(' ').append(all[i].slot);
        }
        return sb.toString();
    }

    private static void beforeKeys(Minecraft mc) {
        KeyMapping[] keys = CombatKeys.grapple;
        if (keys == null) {
            return;
        }
        boolean up = mc.player != null && active() && answered < 0;
        for (int i = 0; i < keys.length; i++) {
            KeyMapping k = keys[i];
            if (k == null) {
                continue;
            }
            boolean pressed = false;
            while (k.consumeClick()) {
                pressed = true;
            }
            if (!pressed || !up) {
                continue;
            }
            // The hotbar key (or any other in-game key) bound to the same key doesn't act on this press.
            for (KeyMapping other : mc.options.keyHotbarSlots) {
                if (other.getKey().equals(k.getKey())) {
                    while (other.consumeClick()) {
                        // dropped: the press answered the grapple
                    }
                }
            }
            answer(i);
            up = false;
        }
    }

    /** Sends the answer for slot {@code index} (also the side panel's cursor screen, where key mappings don't fire). */
    public static void answer(int index) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null || !active() || answered >= 0
                || index < 0 || index >= SolHeredit.Grapple.values().length) {
            return;
        }
        answered = index;
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeByte(index);
        ClientPlayNetworking.send(HudNet.GRAPPLE_ANSWER, buf);
    }

    public static void render(GuiGraphics g, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !active()) {
            return;
        }
        Font tr = mc.font;
        SolHeredit.Grapple[] all = SolHeredit.Grapple.values();
        int chips = 0;
        for (int i = 0; i < all.length; i++) {
            chips += tr.width(CombatKeys.grappleKeyName(i) + " " + all[i].slot) + 9;
        }
        int width = Math.max(WIDTH, chips + 5);
        int x = g.guiWidth() / 2 - width / 2;
        int y = g.guiHeight() / 2 - TOP_ABOVE_CENTRE;
        int h = 27;
        float now = CombatHudClient.clientTick() + tickDelta;
        float remaining = Math.max(0f, Math.min(1f, (startTick + windowTicks - now) / windowTicks));
        g.fill(x, y, x + width, y + h, 0xD0101010);
        g.renderOutline(x, y, width, h, answered >= 0 ? (answered == called ? 0xFF30D030 : 0xFFE03030) : 0xFFFFC040);
        // "GRAPPLE: press 4 for legs" (the called slot), the seconds left on the right
        String head = "GRAPPLE: press " + CombatKeys.grappleKeyName(called) + " for " + all[called].slot;
        g.drawString(tr, head, x + 4, y + 3, 0xFFFFC040, false);
        String secs = remaining <= 0f ? "too late" : String.format(Locale.ROOT, "%.1fs", Math.max(0f, startTick + windowTicks - now) / 20f);
        g.drawString(tr, secs, x + width - 4 - tr.width(secs), y + 3, left() < 12 ? 0xFFFF6060 : 0xFFE0E0E0, false);
        // one "key slot" chip per slot: the called one gold, the answer green (right) or red (wrong)
        int chipX = x + 4;
        int chipsY = y + 13;
        for (int i = 0; i < all.length; i++) {
            String text = CombatKeys.grappleKeyName(i) + " " + all[i].slot;
            int w = tr.width(text) + 6;
            boolean isCalled = i == called;
            boolean isAnswer = i == answered;
            g.fill(chipX, chipsY, chipX + w, chipsY + 10, isAnswer ? (isCalled ? 0xFF207020 : 0xFF802020)
                    : isCalled ? 0xFF8A6A10 : 0xFF303030);
            g.drawString(tr, text, chipX + 3, chipsY + 1, isCalled ? 0xFFFFE080 : 0xFFC0C0C0, false);
            chipX += w + 3;
        }
        // the window's bar, emptying to the hit (its last 0.6 s, the perfect parry, drawn brighter)
        int barW = Math.round((width - 4) * remaining);
        g.fill(x + 2, y + h - 3, x + width - 2, y + h - 1, 0xFF303030);
        g.fill(x + 2, y + h - 3, x + 2 + barW, y + h - 1, left() < 12 ? 0xFFFFF0A0 : 0xFFFFC040);
    }
}
