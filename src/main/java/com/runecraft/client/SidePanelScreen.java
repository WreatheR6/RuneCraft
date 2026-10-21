package com.runecraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.runecraft.platform.client.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A transparent screen that only frees the mouse cursor so the bottom-right {@link SidePanel} can
 * be used (hover tooltips, clicking tabs/skills/prayers/spells). The world stays visible and the
 * game keeps running. Opened while the "use side panel" key (default Left Alt) is held and closed
 * when it is released, or with Escape.
 *
 * <p>// playability: the player keeps moving while it is open (vanilla releases every key when a screen opens, so the
 * movement keys' physical state is copied back every tick), and the prayer hotkeys still work. OSRS lets you walk and
 * click the prayer book at the same time.
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
    public void renderBackground(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        // No background: the world and HUD stay visible behind the panel.
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (SidePanel.mouseScrolled(width, height, mouseX, mouseY, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (RuneCraftClient.sidePanelKey != null && RuneCraftClient.sidePanelKey.matches(keyCode, scanCode)) {
            SidePanel.visible = !SidePanel.visible;
            onClose();
            return true;
        }
        int combat = CombatKeys.keyIndex(keyCode, scanCode);
        if (combat >= 0) {
            CombatKeys.press(combat);
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
        if (minecraft == null) {
            return;
        }
        copyMovementKeys();
        // Safety net in case the release event went elsewhere (e.g. window lost focus).
        if (RuneCraftClient.panelCursorKey == null) {
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

    @Override
    public void removed() {
        copyMovementKeys();
        super.removed();
    }

    /** Copies the physical state of the movement keys into their mappings, so walking continues behind the screen. */
    private void copyMovementKeys() {
        if (minecraft == null) {
            return;
        }
        Options o = minecraft.options;
        long window = minecraft.getWindow().getWindow();
        for (KeyMapping m : new KeyMapping[]{o.keyUp, o.keyDown, o.keyLeft, o.keyRight, o.keyJump, o.keySprint, o.keyShift}) {
            // Toggle-mode sprint/sneak flip on every press, so they keep their own state.
            if (m == o.keySprint && o.toggleSprint().get() || m == o.keyShift && o.toggleCrouch().get()) {
                continue;
            }
            InputConstants.Key k = m.getKey();
            if (k.getType() == InputConstants.Type.KEYSYM && k.getValue() != InputConstants.UNKNOWN.getValue()) {
                KeyMapping.set(k, InputConstants.isKeyDown(window, k.getValue()));
            }
        }
    }
}
