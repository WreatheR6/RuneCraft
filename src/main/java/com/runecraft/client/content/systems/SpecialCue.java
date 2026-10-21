package com.runecraft.client.content.systems;

import com.runecraft.osrs.content.systems.SpecialAttacks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

/**
 * The special attack on a weapon switch (round 42, {@code // playability}): switching the held weapon clears the armed
 * special at once (OSRS deselects the special attack bar when another weapon is equipped; the server does the same, see
 * {@code SpecialAttackService.disarmedBySwitch}), and switching to a weapon whose special the energy covers shows
 * "Special ready (key)" by the special attack bar for two seconds with a soft chime, so a spec switch in a real-time
 * fight doesn't need a look at the bar.
 */
public final class SpecialCue {
    private static final int SHOW_TICKS = 40;
    private static String held = "";
    private static long ticks;
    private static long readyUntil = -1;
    private static String readyName = "";

    private SpecialCue() {
    }

    static void tick(Minecraft mc) {
        ticks++;
        if (mc.player == null) {
            held = "";
            return;
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(mc.player.getMainHandItem().getItem());
        String id = mc.player.getMainHandItem().isEmpty() || !key.getNamespace().equals("runecraft") ? "" : key.getPath();
        if (id.equals(held)) {
            return;
        }
        held = id;
        if (SysClientState.specArmed) {
            SysClientState.specArmed = false; // the server deselects it too and its next state confirms
        }
        SpecialAttacks.Spec spec = id.isEmpty() ? null : SpecialAttacks.of(id);
        SysClientState.specCost = spec == null ? -1 : spec.cost();
        if (spec != null && SysClientState.received && SysClientState.spec >= spec.cost()) {
            readyUntil = ticks + SHOW_TICKS;
            readyName = spec.name();
            mc.player.playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.35f, 1.6f);
        } else {
            readyUntil = -1;
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
