package com.runecraft.client;

import com.runecraft.network.Net;
import com.runecraft.osrs.Skill;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** The OSRS "Make-X" chatbox interface: pick a quantity, then click what to make or buy. */
public class MakeXScreen extends Screen {
    private static final int[] QUANTITIES = {1, 5, 10, 28, 10_000};
    private static int quantityIndex = 3;
    private static final int PER_PAGE = 18;
    private int page;
    private int left;
    private int top;

    public MakeXScreen() {
        super(Text.literal(ClientState.menuType == null ? "Make" : ClientState.menuType.title()));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        left = (width - 330) / 2;
        top = (height - 220) / 2;
        for (int i = 0; i < QUANTITIES.length; i++) {
            int index = i;
            String label = (quantityIndex == i ? "> " : "") + (QUANTITIES[i] == 10_000 ? "All" : String.valueOf(QUANTITIES[i]));
            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
                quantityIndex = index;
                clearAndInit();
            }).dimensions(left + 5 + i * 46, top + 196, 44, 18).build());
        }
        int start = page * PER_PAGE;
        for (int i = start; i < Math.min(ClientState.MENU.size(), start + PER_PAGE); i++) {
            ClientState.MenuEntry e = ClientState.MENU.get(i);
            int slot = i - start;
            int x = left + 5 + (slot % 3) * 108;
            int y = top + 22 + (slot / 3) * 28;
            String req = e.skill() >= 0 ? "Requires level " + e.level() + " " + Skill.byIndex(e.skill()).displayName() : "";
            ButtonWidget btn = ButtonWidget.builder(Text.literal("    " + trim(e.name())), b -> choose(e))
                    .dimensions(x, y, 104, 24).tooltip(Tooltip.of(Text.literal(e.name() + (req.isEmpty() ? "" : "\n" + req))))
                    .build();
            btn.active = e.available();
            addDrawableChild(btn);
        }
        int pages = (ClientState.MENU.size() + PER_PAGE - 1) / PER_PAGE;
        if (pages > 1) {
            addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
                page = (page + pages - 1) % pages;
                clearAndInit();
            }).dimensions(left + 240, top + 196, 20, 18).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
                page = (page + 1) % pages;
                clearAndInit();
            }).dimensions(left + 300, top + 196, 20, 18).build());
        }
    }

    private String trim(String name) {
        return textRenderer.getWidth(name) > 78 ? textRenderer.trimToWidth(name, 74) + "." : name;
    }

    private void choose(ClientState.MenuEntry e) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(e.recipeId());
        buf.writeVarInt(QUANTITIES[quantityIndex]);
        ClientPlayNetworking.send(Net.MENU_CHOICE, buf);
        close();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        ctx.fill(left - 2, top - 2, left + 332, top + 222, 0xFF1A140C);
        ctx.fill(left, top, left + 330, top + 220, 0xF03E3529);
        ctx.drawCenteredTextWithShadow(textRenderer, title, left + 165, top + 8, 0xFFFF9800);
        super.render(ctx, mouseX, mouseY, delta);
        int start = page * PER_PAGE;
        for (int i = start; i < Math.min(ClientState.MENU.size(), start + PER_PAGE); i++) {
            ClientState.MenuEntry e = ClientState.MENU.get(i);
            int slot = i - start;
            int x = left + 5 + (slot % 3) * 108;
            int y = top + 22 + (slot / 3) * 28;
            ItemStack icon = new ItemStack(Registries.ITEM.get(new Identifier(e.icon())));
            ctx.drawItem(icon, x + 4, y + 4);
        }
    }
}
