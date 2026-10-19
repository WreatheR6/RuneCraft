package com.runecraft.client.content.systems;

import com.runecraft.content.systems.SysNet;
import com.runecraft.platform.client.ClientPlayNetworking;
import com.runecraft.platform.PacketByteBufs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A generic server-driven screen (adventurer's log, spellbook, worn equipment, diaries, Grand Exchange,
 * clue scrolls, capes, hiscores, teleport menus...). Text lines on top, then input fields, then buttons.
 */
public class SystemsScreen extends Screen {
    public record Line(String text, int color) {
    }

    public record Field(String key, String label, String value) {
    }

    public record Btn(String action, String label, String tooltip, boolean enabled) {
    }

    private static final int LINE_H = 10;
    private static final int BUTTON_H = 18;
    private static final int GAP = 4;

    private final String id;
    private final List<Line> lines;
    private final List<Field> fields;
    private final List<Btn> buttons;
    private final Map<String, EditBox> fieldWidgets = new LinkedHashMap<>();
    private final Map<String, String> fieldValues = new LinkedHashMap<>();
    private final List<FormattedCharSequence> wrapped = new ArrayList<>();
    private final List<Integer> wrappedColors = new ArrayList<>();
    private int panelW;
    private int left;
    private int top;
    private int scroll;
    private int contentH;

    public SystemsScreen(String id, String title, List<Line> lines, List<Field> fields, List<Btn> buttons) {
        super(Component.literal(title));
        this.id = id;
        this.lines = lines;
        this.fields = fields;
        this.buttons = buttons;
        for (Field f : fields) {
            fieldValues.put(f.key(), f.value());
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int columns() {
        // Puzzle grids ("grid<n>_..."): n buttons per row (sliding puzzles, light boxes).
        if (id.length() > 5 && id.startsWith("grid") && Character.isDigit(id.charAt(4))) {
            return id.charAt(4) - '0';
        }
        if (buttons.size() > 16) {
            return 3;
        }
        return buttons.size() > 1 ? 2 : 1;
    }

    @Override
    protected void init() {
        for (Map.Entry<String, EditBox> e : fieldWidgets.entrySet()) {
            fieldValues.put(e.getKey(), e.getValue().getValue());
        }
        fieldWidgets.clear();
        panelW = Math.min(380, width - 16);
        left = (width - panelW) / 2;
        wrapped.clear();
        wrappedColors.clear();
        for (Line l : lines) {
            for (FormattedCharSequence t : font.split(Component.literal(l.text()), panelW - 16)) {
                wrapped.add(t);
                wrappedColors.add(0xFF000000 | l.color());
            }
        }
        int cols = columns();
        int rows = (buttons.size() + cols - 1) / cols;
        contentH = 24 + wrapped.size() * LINE_H + GAP + fields.size() * 22 + GAP + rows * (BUTTON_H + GAP) + 8;
        int maxScroll = Math.max(0, contentH - (height - 16));
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        top = Math.max(8, (height - contentH) / 2) - scroll;

        int y = top + 24 + wrapped.size() * LINE_H + GAP;
        int labelW = 70;
        for (Field f : fields) {
            EditBox w = new EditBox(font, left + 8 + labelW, y, panelW - 16 - labelW, 16, Component.literal(f.label()));
            w.setMaxLength(64);
            w.setValue(fieldValues.getOrDefault(f.key(), ""));
            addRenderableWidget(w);
            fieldWidgets.put(f.key(), w);
            y += 22;
        }
        y += GAP;
        int bw = (panelW - 16 - GAP * (cols - 1)) / cols;
        for (int i = 0; i < buttons.size(); i++) {
            Btn b = buttons.get(i);
            int col = i % cols;
            int row = i / cols;
            Button.Builder builder = Button.builder(Component.literal(b.label()), btn -> click(b.action()))
                    .bounds(left + 8 + col * (bw + GAP), y + row * (BUTTON_H + GAP), bw, BUTTON_H);
            if (!b.tooltip().isEmpty()) {
                builder.tooltip(Tooltip.create(Component.literal(b.tooltip())));
            }
            Button w = builder.build();
            w.active = b.enabled();
            addRenderableWidget(w);
        }
    }

    private void click(String action) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(id);
        buf.writeUtf(action);
        buf.writeVarInt(fieldWidgets.size());
        for (Map.Entry<String, EditBox> e : fieldWidgets.entrySet()) {
            buf.writeUtf(e.getKey());
            String v = e.getValue().getValue();
            buf.writeUtf(v.length() > 200 ? v.substring(0, 200) : v);
        }
        ClientPlayNetworking.send(SysNet.ACTION, buf);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (contentH > height - 16) {
            scroll -= (int) (amount * 16);
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        ctx.fill(left, top, left + panelW, top + contentH, 0xE0302418);
        ctx.fill(left, top, left + panelW, top + 18, 0xF04A3A28);
        ctx.drawCenteredString(font, getTitle(), left + panelW / 2, top + 5, 0xFFFF981F);
        int y = top + 24;
        for (int i = 0; i < wrapped.size(); i++) {
            ctx.drawString(font, wrapped.get(i), left + 8, y, wrappedColors.get(i));
            y += LINE_H;
        }
        y += GAP;
        for (Field f : fields) {
            ctx.drawString(font, f.label(), left + 8, y + 4, 0xFFE0D0B0);
            y += 22;
        }
        super.render(ctx, mouseX, mouseY, delta);
    }
}
