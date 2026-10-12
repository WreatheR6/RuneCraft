package com.runecraft.client.content.systems;

import com.runecraft.content.systems.SysNet;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

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
    private final Map<String, TextFieldWidget> fieldWidgets = new LinkedHashMap<>();
    private final Map<String, String> fieldValues = new LinkedHashMap<>();
    private final List<OrderedText> wrapped = new ArrayList<>();
    private final List<Integer> wrappedColors = new ArrayList<>();
    private int panelW;
    private int left;
    private int top;
    private int scroll;
    private int contentH;

    public SystemsScreen(String id, String title, List<Line> lines, List<Field> fields, List<Btn> buttons) {
        super(Text.literal(title));
        this.id = id;
        this.lines = lines;
        this.fields = fields;
        this.buttons = buttons;
        for (Field f : fields) {
            fieldValues.put(f.key(), f.value());
        }
    }

    @Override
    public boolean shouldPause() {
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
        for (Map.Entry<String, TextFieldWidget> e : fieldWidgets.entrySet()) {
            fieldValues.put(e.getKey(), e.getValue().getText());
        }
        fieldWidgets.clear();
        panelW = Math.min(380, width - 16);
        left = (width - panelW) / 2;
        wrapped.clear();
        wrappedColors.clear();
        for (Line l : lines) {
            for (OrderedText t : textRenderer.wrapLines(Text.literal(l.text()), panelW - 16)) {
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
            TextFieldWidget w = new TextFieldWidget(textRenderer, left + 8 + labelW, y, panelW - 16 - labelW, 16, Text.literal(f.label()));
            w.setMaxLength(64);
            w.setText(fieldValues.getOrDefault(f.key(), ""));
            addDrawableChild(w);
            fieldWidgets.put(f.key(), w);
            y += 22;
        }
        y += GAP;
        int bw = (panelW - 16 - GAP * (cols - 1)) / cols;
        for (int i = 0; i < buttons.size(); i++) {
            Btn b = buttons.get(i);
            int col = i % cols;
            int row = i / cols;
            ButtonWidget.Builder builder = ButtonWidget.builder(Text.literal(b.label()), btn -> click(b.action()))
                    .dimensions(left + 8 + col * (bw + GAP), y + row * (BUTTON_H + GAP), bw, BUTTON_H);
            if (!b.tooltip().isEmpty()) {
                builder.tooltip(Tooltip.of(Text.literal(b.tooltip())));
            }
            ButtonWidget w = builder.build();
            w.active = b.enabled();
            addDrawableChild(w);
        }
    }

    private void click(String action) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(id);
        buf.writeString(action);
        buf.writeVarInt(fieldWidgets.size());
        for (Map.Entry<String, TextFieldWidget> e : fieldWidgets.entrySet()) {
            buf.writeString(e.getKey());
            String v = e.getValue().getText();
            buf.writeString(v.length() > 200 ? v.substring(0, 200) : v);
        }
        ClientPlayNetworking.send(SysNet.ACTION, buf);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (contentH > height - 16) {
            scroll -= (int) (amount * 16);
            clearAndInit();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        ctx.fill(left, top, left + panelW, top + contentH, 0xE0302418);
        ctx.fill(left, top, left + panelW, top + 18, 0xF04A3A28);
        ctx.drawCenteredTextWithShadow(textRenderer, getTitle(), left + panelW / 2, top + 5, 0xFFFF981F);
        int y = top + 24;
        for (int i = 0; i < wrapped.size(); i++) {
            ctx.drawTextWithShadow(textRenderer, wrapped.get(i), left + 8, y, wrappedColors.get(i));
            y += LINE_H;
        }
        y += GAP;
        for (Field f : fields) {
            ctx.drawTextWithShadow(textRenderer, f.label(), left + 8, y + 4, 0xFFE0D0B0);
            y += 22;
        }
        super.render(ctx, mouseX, mouseY, delta);
    }
}
