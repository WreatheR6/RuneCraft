package com.runecraft.client.content.npcs;

import com.runecraft.content.npcs.NpcsNet;
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

/**
 * An OSRS shop: the shop's stock on the left (count and the price of the next one, Buy 1/5/10) and
 * the player's sellable items on the right (what the shop pays for one, Sell 1/5/All).
 */
public class ShopScreen extends Screen {
    public record Line(String icon, String item, String name, int count, int price) {
    }

    public record ShopData(boolean open, String id, String title, String currency, int balance, boolean general,
                           List<Line> stock, List<Line> offers) {
        static ShopData read(PacketByteBuf buf) {
            boolean open = buf.readBoolean();
            String id = buf.readString();
            String title = buf.readString();
            String currency = buf.readString();
            int balance = buf.readVarInt();
            boolean general = buf.readBoolean();
            List<Line> stock = readLines(buf);
            List<Line> offers = readLines(buf);
            return new ShopData(open, id, title, currency, balance, general, stock, offers);
        }

        private static List<Line> readLines(PacketByteBuf buf) {
            int n = buf.readVarInt();
            List<Line> list = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                list.add(new Line(buf.readString(), buf.readString(), buf.readString(), buf.readVarInt(), buf.readVarInt()));
            }
            return list;
        }
    }

    private static final int PER_PAGE = 10;
    private static final int W = 440;
    private static final int H = 246;
    private static final int HALF = 226;
    private ShopData data;
    private int stockPage;
    private int offerPage;
    private int left;
    private int top;

    public ShopScreen(ShopData data) {
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

    @Override
    public void removed() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(data.id());
        ClientPlayNetworking.send(NpcsNet.CLOSE, buf);
        super.removed();
    }

    private static int pages(int n) {
        return Math.max(1, (n + PER_PAGE - 1) / PER_PAGE);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        stockPage = Math.min(stockPage, pages(data.stock().size()) - 1);
        offerPage = Math.min(offerPage, pages(data.offers().size()) - 1);
        int y0 = top + 34;
        int start = stockPage * PER_PAGE;
        for (int i = start; i < Math.min(data.stock().size(), start + PER_PAGE); i++) {
            Line l = data.stock().get(i);
            int y = y0 + (i - start) * 18;
            int[] amounts = {1, 5, 10};
            for (int k = 0; k < amounts.length; k++) {
                int amount = amounts[k];
                ButtonWidget b = ButtonWidget.builder(Text.literal(String.valueOf(amount)), w -> send(NpcsNet.BUY, l.item(), amount))
                        .dimensions(left + HALF - 66 + k * 21, y, 20, 16)
                        .tooltip(Tooltip.of(Text.literal("Buy " + amount + " x " + l.name())))
                        .build();
                b.active = l.count() > 0;
                addDrawableChild(b);
            }
        }
        start = offerPage * PER_PAGE;
        for (int i = start; i < Math.min(data.offers().size(), start + PER_PAGE); i++) {
            Line l = data.offers().get(i);
            int y = y0 + (i - start) * 18;
            int[] amounts = {1, 5, l.count()};
            String[] labels = {"1", "5", "All"};
            for (int k = 0; k < amounts.length; k++) {
                int amount = amounts[k];
                addDrawableChild(ButtonWidget.builder(Text.literal(labels[k]), w -> send(NpcsNet.SELL, l.item(), amount))
                        .dimensions(left + W - 76 + k * 23, y, k == 2 ? 26 : 22, 16)
                        .tooltip(Tooltip.of(Text.literal("Sell " + labels[k].toLowerCase() + " " + l.name())))
                        .build());
            }
        }
        int stockPages = pages(data.stock().size());
        if (stockPages > 1) {
            addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
                stockPage = (stockPage + stockPages - 1) % stockPages;
                clearAndInit();
            }).dimensions(left + 6, top + H - 22, 20, 16).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
                stockPage = (stockPage + 1) % stockPages;
                clearAndInit();
            }).dimensions(left + 30, top + H - 22, 20, 16).build());
        }
        int offerPages = pages(data.offers().size());
        if (offerPages > 1) {
            addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
                offerPage = (offerPage + offerPages - 1) % offerPages;
                clearAndInit();
            }).dimensions(left + HALF + 8, top + H - 22, 20, 16).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
                offerPage = (offerPage + 1) % offerPages;
                clearAndInit();
            }).dimensions(left + HALF + 32, top + H - 22, 20, 16).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
                .dimensions(left + W / 2 - 25, top + H - 22, 50, 16).build());
    }

    private void send(Identifier channel, String item, int quantity) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(data.id());
        buf.writeString(item);
        buf.writeVarInt(Math.max(1, quantity));
        ClientPlayNetworking.send(channel, buf);
    }

    private void drawIcon(DrawContext ctx, String icon, int x, int y) {
        Identifier id = Identifier.tryParse(icon);
        if (id != null) {
            ctx.drawItem(new ItemStack(Registries.ITEM.get(id)), x, y);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        ctx.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF1A140C);
        ctx.fill(left, top, left + W, top + H, 0xF03E3529);
        ctx.fill(left + HALF, top + 18, left + HALF + 1, top + H - 26, 0xFF1A140C);
        ctx.drawCenteredTextWithShadow(textRenderer, title, left + W / 2, top + 6, 0xFFFF9800);
        ctx.drawTextWithShadow(textRenderer, "Stock (price of next)", left + 8, top + 20, 0xFFFFFF60);
        String sellHeader = data.general() ? "Sell to shop (any item)" : "Sell to shop";
        ctx.drawTextWithShadow(textRenderer, sellHeader, left + HALF + 8, top + 20, 0xFFFFFF60);
        String bal = String.format("%,d", data.balance()) + " " + data.currency();
        ctx.drawTextWithShadow(textRenderer, bal, left + W - 8 - textRenderer.getWidth(bal), top + 6, 0xFFFFD700);

        int y0 = top + 34;
        int start = stockPage * PER_PAGE;
        for (int i = start; i < Math.min(data.stock().size(), start + PER_PAGE); i++) {
            Line l = data.stock().get(i);
            int y = y0 + (i - start) * 18;
            drawIcon(ctx, l.icon(), left + 6, y);
            int color = l.count() > 0 ? 0xFFFFFFFF : 0xFF908070;
            ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(l.name(), 70), left + 26, y + 4, color);
            ctx.drawTextWithShadow(textRenderer, String.valueOf(l.count()), left + 98, y + 4, 0xFFB0E0B0);
            if (l.count() > 0) {
                ctx.drawTextWithShadow(textRenderer, String.format("%,d", l.price()), left + 120, y + 4, 0xFFFFD700);
            }
        }
        if (data.offers().isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "You have nothing this shop will buy.", left + HALF + 8, y0 + 4, 0xFF908070);
        }
        start = offerPage * PER_PAGE;
        for (int i = start; i < Math.min(data.offers().size(), start + PER_PAGE); i++) {
            Line l = data.offers().get(i);
            int y = y0 + (i - start) * 18;
            drawIcon(ctx, l.icon(), left + HALF + 6, y);
            ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(l.name(), 56), left + HALF + 26, y + 4, 0xFFFFFFFF);
            ctx.drawTextWithShadow(textRenderer, String.valueOf(l.count()), left + HALF + 84, y + 4, 0xFFB0E0B0);
            ctx.drawTextWithShadow(textRenderer, String.format("%,d", l.price()), left + HALF + 104, y + 4, 0xFFFFD700);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }
}
