package com.runecraft.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

/**
 * A transparent screen that only frees the mouse cursor so the bottom-right {@link SidePanel} can
 * be used (hover tooltips, clicking tabs/skills/prayers/spells). The world stays visible and the
 * game keeps running. Opened while the "use side panel" key (default Left Alt) is held and closed
 * when it is released, or with Escape.
 */
public class SidePanelScreen extends Screen {
    public SidePanelScreen() {
        super(Text.literal("Side panel"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
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
        if (RuneCraftClient.sidePanelKey != null && RuneCraftClient.sidePanelKey.matchesKey(keyCode, scanCode)) {
            SidePanel.visible = !SidePanel.visible;
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (RuneCraftClient.panelCursorKey != null && RuneCraftClient.panelCursorKey.matchesKey(keyCode, scanCode)) {
            close();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        // Safety net in case the release event went elsewhere (e.g. window lost focus).
        if (client == null || RuneCraftClient.panelCursorKey == null) {
            return;
        }
        InputUtil.Key key = KeyBindingHelper.getBoundKeyOf(RuneCraftClient.panelCursorKey);
        if (key.getCategory() == InputUtil.Type.KEYSYM && key.getCode() != InputUtil.UNKNOWN_KEY.getCode()
                && !InputUtil.isKeyPressed(client.getWindow().getHandle(), key.getCode())) {
            close();
        }
        if (!SidePanel.visible) {
            close();
        }
    }
}
