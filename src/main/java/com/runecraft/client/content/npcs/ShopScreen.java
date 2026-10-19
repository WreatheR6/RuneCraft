package com.runecraft.client.content.npcs;

import com.runecraft.content.npcs.NpcsNet;
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

/**
 * An OSRS shop: the shop's stock on the left (count and the price of the next one, Buy 1/5/10) and
 * the player's sellable items on the right (what the shop pays for one, Sell 1/5/All).
 */
public class ShopScreen extends Screen {
    public record Line(String icon, String item, String name, int count, int price) {
    }

    public record ShopData(boolean open, String id, String title, String currency, int balance, boolean general,
                           List<Line> stock, List<Line> offers) {
        static ShopData read(FriendlyByteBuf buf) {
            boolean open = buf.readBoolean();
            String id = buf.readUtf();
            String title = buf.readUtf();
            String currency = buf.readUtf();
            int balance = buf.readVarInt();
            boolean general = buf.readBoolean();
            List<Line> stock = readLines(buf);
            List<Line> offers = readLines(buf);
            return new ShopData(open, id, title, currency, balance, general, stock, offers);
        }

        private static List<Line> readLines(FriendlyByteBuf buf) {
            int n = buf.readVarInt();
            List<Line> list = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                list.add(new Line(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readVarInt()));
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

    @Override
    public void removed() {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(data.id());
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
                Button b = Button.builder(Component.literal(String.valueOf(amount)), w -> send(NpcsNet.BUY, l.item(), amount))
                        .bounds(left + HALF - 66 + k * 21, y, 20, 16)
                        .tooltip(Tooltip.create(Component.literal("Buy " + amount + " x " + l.name())))
                        .build();
                b.active = l.count() > 0;
                addRenderableWidget(b);
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
                addRenderableWidget(Button.builder(Component.literal(labels[k]), w -> send(NpcsNet.SELL, l.item(), amount))
                        .bounds(left + W - 76 + k * 23, y, k == 2 ? 26 : 22, 16)
                        .tooltip(Tooltip.create(Component.literal("Sell " + labels[k].toLowerCase() + " " + l.name())))
                        .build());
            }
        }
        int stockPages = pages(data.stock().size());
        if (stockPages > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                stockPage = (stockPage + stockPages - 1) % stockPages;
                rebuildWidgets();
            }).bounds(left + 6, top + H - 22, 20, 16).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                stockPage = (stockPage + 1) % stockPages;
                rebuildWidgets();
            }).bounds(left + 30, top + H - 22, 20, 16).build());
        }
        int offerPages = pages(data.offers().size());
        if (offerPages > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                offerPage = (offerPage + offerPages - 1) % offerPages;
                rebuildWidgets();
            }).bounds(left + HALF + 8, top + H - 22, 20, 16).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                offerPage = (offerPage + 1) % offerPages;
                rebuildWidgets();
            }).bounds(left + HALF + 32, top + H - 22, 20, 16).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(left + W / 2 - 25, top + H - 22, 50, 16).build());
    }

    private void send(ResourceLocation channel, String item, int quantity) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(data.id());
        buf.writeUtf(item);
        buf.writeVarInt(Math.max(1, quantity));
        ClientPlayNetworking.send(channel, buf);
    }

    private void drawIcon(GuiGraphics ctx, String icon, int x, int y) {
        ResourceLocation id = ResourceLocation.tryParse(icon);
        if (id != null) {
            ctx.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(id)), x, y);
        }
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        ctx.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF1A140C);
        ctx.fill(left, top, left + W, top + H, 0xF03E3529);
        ctx.fill(left + HALF, top + 18, left + HALF + 1, top + H - 26, 0xFF1A140C);
        ctx.drawCenteredString(font, title, left + W / 2, top + 6, 0xFFFF9800);
        ctx.drawString(font, "Stock (price of next)", left + 8, top + 20, 0xFFFFFF60);
        String sellHeader = data.general() ? "Sell to shop (any item)" : "Sell to shop";
        ctx.drawString(font, sellHeader, left + HALF + 8, top + 20, 0xFFFFFF60);
        String bal = String.format("%,d", data.balance()) + " " + data.currency();
        ctx.drawString(font, bal, left + W - 8 - font.width(bal), top + 6, 0xFFFFD700);

        int y0 = top + 34;
        int start = stockPage * PER_PAGE;
        for (int i = start; i < Math.min(data.stock().size(), start + PER_PAGE); i++) {
            Line l = data.stock().get(i);
            int y = y0 + (i - start) * 18;
            drawIcon(ctx, l.icon(), left + 6, y);
            int color = l.count() > 0 ? 0xFFFFFFFF : 0xFF908070;
            ctx.drawString(font, font.plainSubstrByWidth(l.name(), 70), left + 26, y + 4, color);
            ctx.drawString(font, String.valueOf(l.count()), left + 98, y + 4, 0xFFB0E0B0);
            if (l.count() > 0) {
                ctx.drawString(font, String.format("%,d", l.price()), left + 120, y + 4, 0xFFFFD700);
            }
        }
        if (data.offers().isEmpty()) {
            ctx.drawString(font, "You have nothing this shop will buy.", left + HALF + 8, y0 + 4, 0xFF908070);
        }
        start = offerPage * PER_PAGE;
        for (int i = start; i < Math.min(data.offers().size(), start + PER_PAGE); i++) {
            Line l = data.offers().get(i);
            int y = y0 + (i - start) * 18;
            drawIcon(ctx, l.icon(), left + HALF + 6, y);
            ctx.drawString(font, font.plainSubstrByWidth(l.name(), 56), left + HALF + 26, y + 4, 0xFFFFFFFF);
            ctx.drawString(font, String.valueOf(l.count()), left + HALF + 84, y + 4, 0xFFB0E0B0);
            ctx.drawString(font, String.format("%,d", l.price()), left + HALF + 104, y + 4, 0xFFFFD700);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }
}
