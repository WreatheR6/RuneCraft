package com.runecraft.platform.client;

import com.runecraft.platform.Event;
import net.minecraft.client.gui.GuiGraphics;

/** Fabric's HUD callback, fired from a GUI layer registered above all of vanilla's HUD layers. */
@FunctionalInterface
public interface HudRenderCallback {
    void onHudRender(GuiGraphics drawContext, float tickDelta);

    Event<HudRenderCallback> EVENT = new Event<>(listeners -> (ctx, tickDelta) -> {
        for (HudRenderCallback l : listeners) {
            l.onHudRender(ctx, tickDelta);
        }
    });
}
