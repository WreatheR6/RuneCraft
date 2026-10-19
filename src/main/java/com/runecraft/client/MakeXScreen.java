package com.runecraft.client;

import com.runecraft.network.Net;
import com.runecraft.osrs.Skill;
import com.runecraft.platform.client.ClientPlayNetworking;
import com.runecraft.platform.PacketByteBufs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** The OSRS "Make-X" chatbox interface: pick a quantity, then click what to make or buy. */
public class MakeXScreen extends Screen {
    private static final int[] QUANTITIES = {1, 5, 10, 28, 10_000};
    private static int quantityIndex = 3;
    private static final int PER_PAGE = 18;
    private int page;
    private int left;
    private int top;

    public MakeXScreen() {
        super(Component.literal(ClientState.menuType == null ? "Make" : ClientState.menuType.title()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        left = (width - 330) / 2;
        top = (height - 220) / 2;
        for (int i = 0; i < QUANTITIES.length; i++) {
            int index = i;
            String label = (quantityIndex == i ? "> " : "") + (QUANTITIES[i] == 10_000 ? "All" : String.valueOf(QUANTITIES[i]));
            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                quantityIndex = index;
                rebuildWidgets();
            }).bounds(left + 5 + i * 46, top + 196, 44, 18).build());
        }
        int start = page * PER_PAGE;
        for (int i = start; i < Math.min(ClientState.MENU.size(), start + PER_PAGE); i++) {
            ClientState.MenuEntry e = ClientState.MENU.get(i);
            int slot = i - start;
            int x = left + 5 + (slot % 3) * 108;
            int y = top + 22 + (slot / 3) * 28;
            String req = e.skill() >= 0 ? "Requires level " + e.level() + " " + Skill.byIndex(e.skill()).displayName() : "";
            Button btn = Button.builder(Component.literal("    " + trim(e.name())), b -> choose(e))
                    .bounds(x, y, 104, 24).tooltip(Tooltip.create(Component.literal(e.name() + (req.isEmpty() ? "" : "\n" + req))))
                    .build();
            btn.active = e.available();
            addRenderableWidget(btn);
        }
        int pages = (ClientState.MENU.size() + PER_PAGE - 1) / PER_PAGE;
        if (pages > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                page = (page + pages - 1) % pages;
                rebuildWidgets();
            }).bounds(left + 240, top + 196, 20, 18).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                page = (page + 1) % pages;
                rebuildWidgets();
            }).bounds(left + 300, top + 196, 20, 18).build());
        }
    }

    private String trim(String name) {
        return font.width(name) > 78 ? font.plainSubstrByWidth(name, 74) + "." : name;
    }

    private void choose(ClientState.MenuEntry e) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(e.recipeId());
        buf.writeVarInt(QUANTITIES[quantityIndex]);
        ClientPlayNetworking.send(Net.MENU_CHOICE, buf);
        onClose();
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        ctx.fill(left - 2, top - 2, left + 332, top + 222, 0xFF1A140C);
        ctx.fill(left, top, left + 330, top + 220, 0xF03E3529);
        ctx.drawCenteredString(font, title, left + 165, top + 8, 0xFFFF9800);
        super.render(ctx, mouseX, mouseY, delta);
        int start = page * PER_PAGE;
        for (int i = start; i < Math.min(ClientState.MENU.size(), start + PER_PAGE); i++) {
            ClientState.MenuEntry e = ClientState.MENU.get(i);
            int slot = i - start;
            int x = left + 5 + (slot % 3) * 108;
            int y = top + 22 + (slot / 3) * 28;
            ItemStack icon = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(e.icon())));
            ctx.renderItem(icon, x + 4, y + 4);
        }
    }
}
