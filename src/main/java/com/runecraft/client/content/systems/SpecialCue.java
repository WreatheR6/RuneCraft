package com.runecraft.client.content.systems;

import com.runecraft.osrs.content.systems.SpecialAttacks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

/**
 * The special attack on a weapon switch (round 42, {@code // playability}): switching to a weapon whose special the energy
 * covers shows "Special ready (key)" by the special attack bar for two seconds with a soft chime, so a spec switch in a
 * real-time fight doesn't need a look at the bar. The server owns the special: it deselects an armed special the moment
 * another weapon is in hand (OSRS deselects the special attack bar on a weapon switch; {@code SpecialAttackService.heldChanged})
 * and sends its state with the new weapon's effective cost (ToA's invocation and Liquid adrenaline included). This cue only
 * reads that state: after a switch it waits up to {@value #AWAIT_TICKS} ticks for the state that follows and compares the
 * energy with the cost the server sent, never with the weapon's base cost, and it never changes the armed flag itself.
 */
public final class SpecialCue {
    private static final int SHOW_TICKS = 40;
    /** Client ticks a switch waits for the server's state (round trip) before giving up on the cue. */
    static final int AWAIT_TICKS = 20;
    private static String held = "";
    private static long ticks;
    private static long readyUntil = -1;
    private static String readyName = "";
    /** The weapon switched to, while its state is awaited (null otherwise). */
    private static SpecialAttacks.Spec awaited;
    private static long awaitUntil = -1;
    private static boolean chimed;
    private static int statesSeen;

    private SpecialCue() {
    }

    static void tick(Minecraft mc) {
        ticks++;
        // a state packet since the last tick: one that arrived with the switch (same tick) or after it counts for the cue
        boolean newState = SysClientState.states != statesSeen;
        statesSeen = SysClientState.states;
        if (mc.player == null) {
            held = "";
            awaited = null;
            return;
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(mc.player.getMainHandItem().getItem());
        String id = mc.player.getMainHandItem().isEmpty() || !key.getNamespace().equals("runecraft") ? "" : key.getPath();
        if (!id.equals(held)) {
            held = id;
            readyUntil = -1;
            chimed = false;
            awaited = id.isEmpty() ? null : SpecialAttacks.of(id);
            awaitUntil = awaited == null ? -1 : ticks + AWAIT_TICKS;
        }
        if (awaited != null && newState) {
            evaluate(mc);
        }
        if (awaited != null && ticks > awaitUntil) {
            awaited = null;
        }
    }

    /** The server's state for the weapon switched to: the cue shows while its energy covers the cost the server sent. */
    private static void evaluate(Minecraft mc) {
        int cost = SysClientState.specCost;
        if (cost >= 0 && SysClientState.spec >= cost) {
            if (!showing()) {
                readyUntil = ticks + SHOW_TICKS;
            }
            readyName = awaited.name();
            if (!chimed) {
                chimed = true;
                mc.player.playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.35f, 1.6f);
            }
        } else {
            readyUntil = -1; // an older state arrived with the switch; this one carries the new weapon's cost
        }
    }

    /** Whether "Special ready" is showing (self-tests read it through the HUD probe). */
    public static boolean showing() {
        return ticks < readyUntil;
    }

    static void render(GuiGraphics g, Font tr, int barX, int barY, int barW) {
        if (!showing()) {
            return;
        }
        boolean pulse = (ticks / 4) % 2 == 0;
        g.renderOutline(barX - 1, barY - 1, barW + 2, 14, pulse ? 0xFFFFFF60 : 0xFF30D030);
        String key = SystemsClientHooks.specKey == null ? "" : " (" + SystemsClientHooks.specKey.getTranslatedKeyMessage().getString() + ")";
        String text = "Special ready" + key;
        int w = tr.width(text);
        g.fill(barX - w - 8, barY, barX - 2, barY + 12, 0xC0103010);
        g.drawString(tr, text, barX - w - 5, barY + 2, 0xFF60FF60, false);
        if (!readyName.isEmpty()) {
            int nw = tr.width(readyName);
            g.drawString(tr, readyName, barX - nw - 5, barY + 14, 0xFFB0FFB0, true);
        }
    }
}
