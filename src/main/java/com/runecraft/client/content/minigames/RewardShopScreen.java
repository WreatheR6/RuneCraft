package com.runecraft.client.content.minigames;

import com.runecraft.content.minigames.MinigameNet;
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

import java.util.ArrayList;
import java.util.List;

/** A minigame reward shop: balances at the top, a page of rewards with their costs, Buy 1 / Buy 10. */
public class RewardShopScreen extends Screen {
    public record Entry(String label, String icon, String cost, boolean available, String locked) {
    }

    public record ShopData(String id, String title, List<String> currencies, List<Integer> balances, List<Entry> entries) {
        static ShopData read(PacketByteBuf buf) {
            String id = buf.readString();
            String title = buf.readString();
            int nc = buf.readVarInt();
            List<String> names = new ArrayList<>();
            List<Integer> bal = new ArrayList<>();
            for (int i = 0; i < nc; i++) {
                names.add(buf.readString());
                bal.add(buf.readVarInt());
            }
            int ne = buf.readVarInt();
            List<Entry> entries = new ArrayList<>();
            for (int i = 0; i < ne; i++) {
                entries.add(new Entry(buf.readString(), buf.readString(), buf.readString(), buf.readBoolean(), buf.readString()));
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
        super(Text.literal(data.title()));
        this.data = data;
    }

    public String shopId() {
        return data.id();
    }

    void update(ShopData fresh) {
        this.data = fresh;
        clearAndInit();
    }

    @Override
    public boolean shouldPause() {
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
            ButtonWidget one = ButtonWidget.builder(Text.literal("Buy"), b -> buy(index, 1))
                    .dimensions(left + W - 86, y, 38, 16)
                    .tooltip(Tooltip.of(Text.literal(e.label() + "\n" + e.cost() + (e.locked().isEmpty() ? "" : "\n" + e.locked()))))
                    .build();
            one.active = e.available();
            addDrawableChild(one);
            ButtonWidget ten = ButtonWidget.builder(Text.literal("x10"), b -> buy(index, 10)).dimensions(left + W - 44, y, 38, 16).build();
            ten.active = e.available();
            addDrawableChild(ten);
        }
        int pages = (data.entries().size() + PER_PAGE - 1) / PER_PAGE;
        if (pages > 1) {
            addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
                page = (page + pages - 1) % pages;
                clearAndInit();
            }).dimensions(left + 6, top + H - 20, 20, 16).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
                page = (page + 1) % pages;
                clearAndInit();
            }).dimensions(left + 30, top + H - 20, 20, 16).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close()).dimensions(left + W - 56, top + H - 20, 50, 16).build());
    }

    private void buy(int index, int quantity) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(data.id());
        buf.writeVarInt(index);
        buf.writeVarInt(quantity);
        ClientPlayNetworking.send(MinigameNet.BUY, buf);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        ctx.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF1A140C);
        ctx.fill(left, top, left + W, top + H, 0xF03E3529);
        ctx.drawCenteredTextWithShadow(textRenderer, title, left + W / 2, top + 6, 0xFFFF9800);
        for (int i = 0; i < data.currencies().size(); i++) {
            String s = data.currencies().get(i) + ": " + String.format("%,d", data.balances().get(i));
            ctx.drawTextWithShadow(textRenderer, s, left + 8 + (i % 2) * (W / 2), top + 20 + (i / 2) * 10, 0xFFFFFF60);
        }
        int start = page * PER_PAGE;
        int y0 = listTop();
        for (int i = start; i < Math.min(data.entries().size(), start + PER_PAGE); i++) {
            Entry e = data.entries().get(i);
            int y = y0 + (i - start) * 18;
            Identifier id = Identifier.tryParse(e.icon());
            if (id != null) {
                ctx.drawItem(new ItemStack(Registries.ITEM.get(id)), left + 6, y);
            }
            int color = e.available() ? 0xFFFFFFFF : e.locked().isEmpty() ? 0xFFB0A090 : 0xFFE05050;
            String label = textRenderer.trimToWidth(e.label(), 130);
            ctx.drawTextWithShadow(textRenderer, label, left + 26, y + 4, color);
            String cost = textRenderer.trimToWidth(e.locked().isEmpty() ? e.cost() : e.locked(), W - 26 - 136 - 92);
            ctx.drawTextWithShadow(textRenderer, cost, left + 160, y + 4, 0xFFFFD700);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }
}
