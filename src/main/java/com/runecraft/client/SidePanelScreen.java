package com.runecraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.runecraft.platform.client.KeyBindingHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A transparent screen that only frees the mouse cursor so the bottom-right {@link SidePanel} can
 * be used (hover tooltips, clicking tabs/skills/prayers/spells). The world stays visible and the
 * game keeps running. Opened while the "use side panel" key (default Left Alt) is held and closed
 * when it is released, or with Escape.
 */
public class SidePanelScreen extends Screen {
    public SidePanelScreen() {
        super(Component.literal("Side panel"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        // No background: the world and HUD stay visible behind the panel.
        SidePanel.render(ctx, width, height, mouseX, mouseY);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (SidePanel.mouseClicked(width, height, mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (SidePanel.mouseScrolled(width, height, mouseX, mouseY, amount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (RuneCraftClient.sidePanelKey != null && RuneCraftClient.sidePanelKey.matches(keyCode, scanCode)) {
            SidePanel.visible = !SidePanel.visible;
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (RuneCraftClient.panelCursorKey != null && RuneCraftClient.panelCursorKey.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        // Safety net in case the release event went elsewhere (e.g. window lost focus).
        if (minecraft == null || RuneCraftClient.panelCursorKey == null) {
            return;
        }
        InputConstants.Key key = KeyBindingHelper.getBoundKeyOf(RuneCraftClient.panelCursorKey);
        if (key.getType() == InputConstants.Type.KEYSYM && key.getValue() != InputConstants.UNKNOWN.getValue()
                && !InputConstants.isKeyDown(minecraft.getWindow().getWindow(), key.getValue())) {
            onClose();
        }
        if (!SidePanel.visible) {
            onClose();
        }
    }
}
