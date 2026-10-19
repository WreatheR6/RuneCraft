package com.runecraft.client.content.minigames;

import com.runecraft.content.minigames.MinigameNet;
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
import java.util.ArrayList;
import java.util.List;

/** A minigame reward shop: balances at the top, a page of rewards with their costs, Buy 1 / Buy 10. */
public class RewardShopScreen extends Screen {
    public record Entry(String label, String icon, String cost, boolean available, String locked) {
    }

    public record ShopData(String id, String title, List<String> currencies, List<Integer> balances, List<Entry> entries) {
        static ShopData read(FriendlyByteBuf buf) {
            String id = buf.readUtf();
            String title = buf.readUtf();
            int nc = buf.readVarInt();
            List<String> names = new ArrayList<>();
            List<Integer> bal = new ArrayList<>();
            for (int i = 0; i < nc; i++) {
                names.add(buf.readUtf());
                bal.add(buf.readVarInt());
            }
            int ne = buf.readVarInt();
            List<Entry> entries = new ArrayList<>();
            for (int i = 0; i < ne; i++) {
                entries.add(new Entry(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readBoolean(), buf.readUtf()));
            }
            return new ShopData(id, title, names, bal, entries);
        }
    }

    private static final int PER_PAGE = 10;
    private static final int W = 360;
    private static final int H = 236;
    private ShopData data;
    private int page;
    private int left;
    private int top;

    public RewardShopScreen(ShopData data) {
        super(Component.literal(data.title()));
        this.data = data;
    }

    public String shopId() {
        return data.id();
    }

    void update(ShopData fresh) {
        this.data = fresh;
        rebuildWidgets();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int listTop() {
        return top + 22 + ((data.currencies().size() + 1) / 2) * 10;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int start = page * PER_PAGE;
        int y0 = listTop();
        for (int i = start; i < Math.min(data.entries().size(), start + PER_PAGE); i++) {
            Entry e = data.entries().get(i);
            int index = i;
            int y = y0 + (i - start) * 18;
            Button one = Button.builder(Component.literal("Buy"), b -> buy(index, 1))
                    .bounds(left + W - 86, y, 38, 16)
                    .tooltip(Tooltip.create(Component.literal(e.label() + "\n" + e.cost() + (e.locked().isEmpty() ? "" : "\n" + e.locked()))))
                    .build();
            one.active = e.available();
            addRenderableWidget(one);
            Button ten = Button.builder(Component.literal("x10"), b -> buy(index, 10)).bounds(left + W - 44, y, 38, 16).build();
            ten.active = e.available();
            addRenderableWidget(ten);
        }
        int pages = (data.entries().size() + PER_PAGE - 1) / PER_PAGE;
        if (pages > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                page = (page + pages - 1) % pages;
                rebuildWidgets();
            }).bounds(left + 6, top + H - 20, 20, 16).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                page = (page + 1) % pages;
                rebuildWidgets();
            }).bounds(left + 30, top + H - 20, 20, 16).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose()).bounds(left + W - 56, top + H - 20, 50, 16).build());
    }

    private void buy(int index, int quantity) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(data.id());
        buf.writeVarInt(index);
        buf.writeVarInt(quantity);
        ClientPlayNetworking.send(MinigameNet.BUY, buf);
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        ctx.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF1A140C);
        ctx.fill(left, top, left + W, top + H, 0xF03E3529);
        ctx.drawCenteredString(font, title, left + W / 2, top + 6, 0xFFFF9800);
        for (int i = 0; i < data.currencies().size(); i++) {
            String s = data.currencies().get(i) + ": " + String.format("%,d", data.balances().get(i));
            ctx.drawString(font, s, left + 8 + (i % 2) * (W / 2), top + 20 + (i / 2) * 10, 0xFFFFFF60);
        }
        int start = page * PER_PAGE;
        int y0 = listTop();
        for (int i = start; i < Math.min(data.entries().size(), start + PER_PAGE); i++) {
            Entry e = data.entries().get(i);
            int y = y0 + (i - start) * 18;
            ResourceLocation id = ResourceLocation.tryParse(e.icon());
            if (id != null) {
                ctx.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(id)), left + 6, y);
            }
            int color = e.available() ? 0xFFFFFFFF : e.locked().isEmpty() ? 0xFFB0A090 : 0xFFE05050;
            String label = font.plainSubstrByWidth(e.label(), 130);
            ctx.drawString(font, label, left + 26, y + 4, color);
            String cost = font.plainSubstrByWidth(e.locked().isEmpty() ? e.cost() : e.locked(), W - 26 - 136 - 92);
            ctx.drawString(font, cost, left + 160, y + 4, 0xFFFFD700);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }
}
