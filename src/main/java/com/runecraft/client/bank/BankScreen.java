package com.runecraft.client.bank;

import com.runecraft.bank.BankService;
import com.runecraft.bank.Notes;
import com.runecraft.client.ClientState;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.bank.BankModel;
import com.runecraft.osrs.bank.BankQuantity;
import com.runecraft.osrs.content.systems.GrandExchange;
import com.runecraft.util.Inv;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The OSRS bank interface: tabs along the top (drag an item onto a tab to move it there, onto "+" for a new tab),
 * an 8-wide item grid (click to withdraw with the selected quantity, right-click for All, shift+right-click for
 * All-but-1, drag onto another item to swap), the inventory beside it (click to deposit), and the Item/Note,
 * 1/5/10/X/All, placeholder, search, deposit inventory and deposit worn items controls. Deposit boxes show the
 * same screen without the bank. Everything is a request to the server, which sends the new state back.
 */
public class BankScreen extends Screen {
    private static final int CELL = 20;
    private static final int COLS = 8;
    private static final int INV_COLS = 6;
    private static final int PANEL_BG = 0xF0302418;
    private static final int GRID_BG = 0xFF1E170F;

    /** View state kept across refreshes and re-openings. */
    private static int tab;
    private static String search = "";
    private static int scroll;

    private final Map<String, ItemStack> stacks = new HashMap<>();
    private TextFieldWidget searchField;
    private TextFieldWidget xField;
    private int left;
    private int top;
    private int panelW;
    private int panelH;
    private int rows;
    private int gridX;
    private int gridY;
    private int invX;
    private int invY;
    private List<BankModel.Entry> view = List.of();

    // Mouse press on a bank slot (for click vs drag).
    private BankModel.Entry pressed;
    private int pressButton;
    private double pressX;
    private double pressY;
    private boolean dragging;

    public BankScreen() {
        super(Text.literal("The Bank of Gielinor"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private boolean depositOnly() {
        return BankClient.depositOnly;
    }

    /** New state from the server. */
    void refresh() {
        stacks.clear();
        clearAndInit();
    }

    @Override
    protected void init() {
        boolean searchFocused = searchField != null && searchField.isFocused();
        boolean bankShown = !depositOnly();
        int bankW = bankShown ? COLS * CELL + 10 : 0;
        panelW = 8 + bankW + INV_COLS * CELL + 8;
        if (!bankShown) {
            panelW = Math.max(panelW, 8 + 4 * 62 + 8);
        }
        int invH = 6 * CELL + CELL + 2;
        rows = bankShown ? Math.max(3, Math.min(12, (height - 24 - 104) / CELL)) : 0;
        int contentH = Math.max(rows * CELL, invH);
        panelH = 42 + contentH + 4 + (bankShown ? 20 + 44 : 22) + 6;
        left = (width - panelW) / 2;
        top = Math.max(4, (height - panelH) / 2);
        gridX = left + 8;
        gridY = top + 42;
        invX = left + 8 + bankW;
        invY = top + 42;
        if (tab > BankClient.BANK.tabCount()) {
            tab = 0;
        }
        rebuildView();

        int y = top + 42 + contentH + 4;
        if (bankShown) {
            searchField = new TextFieldWidget(textRenderer, left + 8, y, 110, 16, Text.literal("Search"));
            searchField.setMaxLength(40);
            searchField.setText(search);
            searchField.setPlaceholder(Text.literal("Search...").formatted(Formatting.DARK_GRAY));
            searchField.setChangedListener(t -> {
                search = t;
                scroll = 0;
                rebuildView();
            });
            addDrawableChild(searchField);
            if (searchFocused) {
                setFocused(searchField);
                searchField.setFocused(true);
            }
            y += 20;
            BankModel bank = BankClient.BANK;
            int x = left + 8;
            x = toggle(x, y, 34, "Item", !bank.noted, "Withdraw items as items.", () -> send(BankService.OP_SET_NOTED, 0));
            x = toggle(x, y, 34, "Note", bank.noted, "Withdraw tradeable items as bank notes.", () -> send(BankService.OP_SET_NOTED, 1)) + 6;
            x = quantityButtons(x, y);
            xField = new TextFieldWidget(textRenderer, x, y + 1, 48, 16, Text.literal("X"));
            xField.setMaxLength(12);
            xField.setText(Long.toString(bank.x));
            addDrawableChild(xField);
            y += 22;
            x = left + 8;
            x = toggle(x, y, 92, "Placeholders: " + (bank.placeholders ? "On" : "Off"), bank.placeholders,
                    "Leave a placeholder when you withdraw the last of an item.",
                    () -> send(BankService.OP_SET_PLACEHOLDERS, bank.placeholders ? 0 : 1));
            x = toggle(x, y, 52, "Release", false, "Release all placeholders.", () -> send(BankService.OP_RELEASE_PLACEHOLDERS, 0));
            depositButtons(Math.max(x + 4, left + panelW - 8 - 2 * 62 - 4), y);
        } else {
            searchField = null;
            xField = null;
            int x = left + 8;
            x = quantityButtons(x, y);
            depositButtons(left + panelW - 8 - 2 * 62 - 4, y);
        }
    }

    private int toggle(int x, int y, int w, String label, boolean active, String tip, Runnable click) {
        ButtonWidget b = ButtonWidget.builder(Text.literal(label).formatted(active ? Formatting.YELLOW : Formatting.WHITE), btn -> click.run())
                .dimensions(x, y, w, 18).tooltip(Tooltip.of(Text.literal(tip))).build();
        addDrawableChild(b);
        return x + w + 2;
    }

    private int quantityButtons(int x, int y) {
        BankQuantity.Mode current = BankClient.BANK.quantity;
        for (BankQuantity.Mode m : new BankQuantity.Mode[]{BankQuantity.Mode.ONE, BankQuantity.Mode.FIVE, BankQuantity.Mode.TEN,
                BankQuantity.Mode.X, BankQuantity.Mode.ALL}) {
            int w = m == BankQuantity.Mode.ALL ? 24 : m == BankQuantity.Mode.TEN ? 22 : 18;
            x = toggle(x, y, w, m.label, current == m, "Default quantity: " + m.label + (m == BankQuantity.Mode.X ? " (amount typed beside)" : ""),
                    () -> BankClient.send(BankService.OP_SET_QUANTITY, "", "", 0, m, xAmount(), 0));
        }
        return x + 2;
    }

    private void depositButtons(int x, int y) {
        addDrawableChild(ButtonWidget.builder(Text.literal("Deposit inv."), b -> send(BankService.OP_DEPOSIT_INVENTORY, 0))
                .dimensions(x, y, 62, 18).tooltip(Tooltip.of(Text.literal("Deposit inventory (and coin pouch)"))).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Deposit worn"), b -> send(BankService.OP_DEPOSIT_WORN, 0))
                .dimensions(x + 64, y, 62, 18).tooltip(Tooltip.of(Text.literal("Deposit worn items"))).build());
    }

    private long xAmount() {
        long x = xField == null ? -1 : BankQuantity.parseX(xField.getText());
        return x > 0 ? x : BankClient.BANK.x;
    }

    private int depositTab() {
        return search.isBlank() ? tab : 0;
    }

    private void send(int op, int slot) {
        BankClient.send(op, "", "", slot, BankClient.BANK.quantity, xAmount(), depositTab());
    }

    private void rebuildView() {
        view = BankClient.BANK.view(tab, search, this::nameOf);
        int maxScroll = Math.max(0, (view.size() + COLS - 1) / COLS - rows);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
    }

    private ItemStack stackOf(BankModel.Entry e) {
        return stacks.computeIfAbsent(e.key(), k -> {
            ItemStack s = Notes.stackOf(e.item, e.tag, 1);
            return s.isEmpty() ? new ItemStack(net.minecraft.item.Items.BARRIER) : s;
        });
    }

    private String nameOf(String item) {
        ItemDef def = ItemCatalog.get(item);
        if (def != null) {
            return def.name();
        }
        ItemStack s = Notes.stackOf(item, "", 1);
        return s.isEmpty() ? item : s.getName().getString();
    }

    // ------------------------------------------------------------------ geometry

    /** Tab buttons: 0 = view all, 1..n, then "+" (n+1) for a new tab. */
    private int tabButtons() {
        int n = BankClient.BANK.tabCount();
        return n + 1 + (n < BankModel.MAX_TABS ? 1 : 0);
    }

    private int tabAt(double mx, double my) {
        if (depositOnly() || my < top + 18 || my >= top + 38) {
            return -1;
        }
        int i = (int) Math.floor((mx - (left + 8)) / (CELL + 2));
        return mx >= left + 8 && i >= 0 && i < tabButtons() ? i : -1;
    }

    private BankModel.Entry entryAt(double mx, double my) {
        if (depositOnly() || mx < gridX || my < gridY || mx >= gridX + COLS * CELL || my >= gridY + rows * CELL) {
            return null;
        }
        int col = (int) ((mx - gridX) / CELL);
        int row = (int) ((my - gridY) / CELL);
        int idx = (row + scroll) * COLS + col;
        return idx < view.size() ? view.get(idx) : null;
    }

    /** Inventory panel cell -> main inventory slot (rows of the backpack first, hotbar last); -2 = coin pouch. */
    private int invSlotAt(double mx, double my) {
        if (mx < invX || mx >= invX + INV_COLS * CELL || my < invY) {
            return -1;
        }
        int col = (int) ((mx - invX) / CELL);
        int row = (int) ((my - invY) / CELL);
        if (row < 6) {
            return displayToSlot(row * INV_COLS + col);
        }
        if (row == 6 && my >= invY + 6 * CELL + 2 && col == 0) {
            return -2;
        }
        return -1;
    }

    private static int displayToSlot(int d) {
        return d < 27 ? d + 9 : d - 27;
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        BankModel.Entry e = entryAt(mx, my);
        if (e != null) {
            pressed = e;
            pressButton = button;
            pressX = mx;
            pressY = my;
            dragging = false;
            return true;
        }
        int t = tabAt(mx, my);
        if (t >= 0 && button == 0) {
            if (t <= BankClient.BANK.tabCount()) {
                tab = t;
                scroll = 0;
                if (searchField != null) {
                    searchField.setText("");
                }
                search = "";
                rebuildView();
            }
            return true;
        }
        int slot = invSlotAt(mx, my);
        if (slot != -1) {
            BankQuantity.Mode mode = button == 1 ? BankQuantity.Mode.ALL : BankClient.BANK.quantity;
            if (slot == -2) {
                BankClient.send(BankService.OP_DEPOSIT_POUCH, "", "", 0, mode, xAmount(), depositTab());
            } else if (client != null && client.player != null && !client.player.getInventory().main.get(slot).isEmpty()) {
                BankClient.send(BankService.OP_DEPOSIT, "", "", slot, mode, xAmount(), depositTab());
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (pressed != null && pressButton == 0 && !dragging && Math.abs(mx - pressX) + Math.abs(my - pressY) > 4) {
            dragging = true;
        }
        return pressed != null || super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        BankModel.Entry e = pressed;
        pressed = null;
        if (e == null) {
            return super.mouseReleased(mx, my, button);
        }
        if (dragging) {
            dragging = false;
            int t = tabAt(mx, my);
            if (t >= 0) {
                BankClient.send(BankService.OP_MOVE_TAB, e.key(), "", 0, BankQuantity.Mode.ONE, 0, t);
                return true;
            }
            BankModel.Entry other = entryAt(mx, my);
            if (other != null && other != e) {
                BankClient.send(BankService.OP_SWAP, e.key(), other.key(), 0, BankQuantity.Mode.ONE, 0, 0);
            }
            return true;
        }
        if (e.placeholder()) {
            return true;
        }
        BankQuantity.Mode mode = BankClient.BANK.quantity;
        if (button == 1) {
            mode = hasShiftDown() ? BankQuantity.Mode.ALL_BUT_ONE : BankQuantity.Mode.ALL;
        }
        BankClient.send(BankService.OP_WITHDRAW, e.key(), "", 0, mode, xAmount(), 0);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!depositOnly() && mx >= gridX && mx < gridX + COLS * CELL && my >= gridY && my < gridY + rows * CELL) {
            scroll -= (int) Math.signum(amount);
            rebuildView();
            return true;
        }
        return super.mouseScrolled(mx, my, amount);
    }

    @Override
    public void removed() {
        BankClient.send(BankService.OP_CLOSE, "", "", 0, BankQuantity.Mode.ONE, 0, 0);
        super.removed();
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        ctx.fill(left - 1, top - 1, left + panelW + 1, top + panelH + 1, 0xFF100C08);
        ctx.fill(left, top, left + panelW, top + panelH, PANEL_BG);
        String title = depositOnly() ? "The Bank of Gielinor - Deposit Box" : getTitle().getString();
        ctx.drawTextWithShadow(textRenderer, title, left + 8, top + 5, 0xFFFF981F);
        if (!depositOnly()) {
            String value = "Value: " + GrandExchange.formatGp(BankClient.value) + " gp";
            ctx.drawTextWithShadow(textRenderer, value, left + panelW - 8 - textRenderer.getWidth(value), top + 5, 0xFFFFD700);
            drawTabs(ctx, mouseX, mouseY);
            drawGrid(ctx, mouseX, mouseY);
            String used = BankClient.BANK.slotsUsed() + "/" + BankModel.CAPACITY;
            ctx.drawTextWithShadow(textRenderer, used, left + 8 + 114, gridY + Math.max(rows * CELL, 6 * CELL + CELL + 2) + 8, 0xFFC0C0C0);
        }
        drawInventory(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        if (dragging && pressed != null) {
            ctx.getMatrices().push();
            ctx.getMatrices().translate(0, 0, 300);
            ctx.drawItem(stackOf(pressed), mouseX - 8, mouseY - 8);
            ctx.getMatrices().pop();
        } else {
            drawTooltip(ctx, mouseX, mouseY);
        }
    }

    private void drawTabs(DrawContext ctx, int mouseX, int mouseY) {
        BankModel bank = BankClient.BANK;
        int n = tabButtons();
        int hovered = tabAt(mouseX, mouseY);
        for (int i = 0; i < n; i++) {
            int x = left + 8 + i * (CELL + 2);
            int y = top + 18;
            boolean selected = i == tab && search.isBlank();
            ctx.fill(x, y, x + CELL, y + CELL, selected ? 0xFF6A2A1E : hovered == i ? 0xFF4A3E2E : 0xFF3A3024);
            ctx.drawBorder(x, y, CELL, CELL, 0xFF100C08);
            if (i == 0) {
                ctx.drawCenteredTextWithShadow(textRenderer, "All", x + CELL / 2, y + 6, 0xFFFFFFFF);
            } else if (i > bank.tabCount()) {
                ctx.drawCenteredTextWithShadow(textRenderer, "+", x + CELL / 2, y + 6, 0xFFFFFF60);
            } else {
                List<BankModel.Entry> inTab = bank.view(i, "", null);
                if (!inTab.isEmpty()) {
                    ctx.drawItem(stackOf(inTab.get(0)), x + 2, y + 2);
                }
            }
        }
    }

    private void drawGrid(DrawContext ctx, int mouseX, int mouseY) {
        ctx.fill(gridX - 1, gridY - 1, gridX + COLS * CELL + 1, gridY + rows * CELL + 1, GRID_BG);
        BankModel.Entry hovered = entryAt(mouseX, mouseY);
        int start = scroll * COLS;
        for (int i = start; i < Math.min(view.size(), start + rows * COLS); i++) {
            BankModel.Entry e = view.get(i);
            int x = gridX + ((i - start) % COLS) * CELL;
            int y = gridY + ((i - start) / COLS) * CELL;
            if (e == hovered) {
                ctx.fill(x, y, x + CELL, y + CELL, 0x30FFFFFF);
            }
            if (dragging && e == pressed) {
                continue;
            }
            ctx.drawItem(stackOf(e), x + 2, y + 2);
            ctx.getMatrices().push();
            ctx.getMatrices().translate(0, 0, 200);
            if (e.placeholder()) {
                ctx.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, 0xA01E170F);
            } else if (e.count > 1 || ItemCatalog.get(e.item) == null || ItemCatalog.get(e.item).maxStack() > 1) {
                countText(ctx, e.count, x, y);
            }
            ctx.getMatrices().pop();
        }
        int total = (view.size() + COLS - 1) / COLS;
        if (total > rows) {
            int barH = Math.max(8, rows * CELL * rows / total);
            int barY = gridY + (rows * CELL - barH) * scroll / Math.max(1, total - rows);
            ctx.fill(gridX + COLS * CELL - 2, barY, gridX + COLS * CELL, barY + barH, 0xFF8A7A5A);
        }
        if (view.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, search.isBlank() ? "This tab is empty." : "No matches.",
                    gridX + COLS * CELL / 2, gridY + 8, 0xFF9F9F9F);
        }
    }

    private void countText(DrawContext ctx, long count, int x, int y) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x + 1, y + 1, 0);
        ctx.getMatrices().scale(0.75f, 0.75f, 1f);
        ctx.drawText(textRenderer, BankQuantity.shortCount(count), 0, 0, 0xFF000000 | BankQuantity.countColor(count), true);
        ctx.getMatrices().pop();
    }

    private void drawInventory(DrawContext ctx) {
        ctx.fill(invX - 1, invY - 1, invX + INV_COLS * CELL + 1, invY + 6 * CELL + 1, GRID_BG);
        if (client == null || client.player == null) {
            return;
        }
        PlayerInventory inv = client.player.getInventory();
        for (int d = 0; d < 36; d++) {
            ItemStack s = inv.main.get(displayToSlot(d));
            int x = invX + (d % INV_COLS) * CELL;
            int y = invY + (d / INV_COLS) * CELL;
            if (d >= 27) {
                ctx.fill(x, y, x + CELL, y + CELL, 0x18FFFFFF);
            }
            if (!s.isEmpty()) {
                ctx.drawItem(s, x + 2, y + 2);
                ctx.drawItemInSlot(textRenderer, s, x + 2, y + 2);
            }
        }
        // Coin pouch.
        int py = invY + 6 * CELL + 2;
        ctx.fill(invX - 1, py - 1, invX + CELL + 1, py + CELL + 1, GRID_BG);
        ItemStack coins = Inv.stack(Inv.COINS, 1);
        if (!coins.isEmpty()) {
            ctx.drawItem(coins, invX + 2, py + 2);
        }
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 200);
        countText(ctx, ClientState.coins, invX, py);
        ctx.getMatrices().pop();
        ctx.drawTextWithShadow(textRenderer, "Coin pouch", invX + CELL + 4, py + 6, 0xFFC0C0C0);
    }

    private void drawTooltip(DrawContext ctx, int mouseX, int mouseY) {
        BankModel.Entry e = entryAt(mouseX, mouseY);
        if (e != null) {
            List<Text> lines = new ArrayList<>();
            lines.add(stackOf(e).getName().copy().formatted(Formatting.GOLD));
            if (e.placeholder()) {
                lines.add(Text.literal("Placeholder").formatted(Formatting.GRAY));
            } else {
                lines.add(Text.literal(String.format(Locale.ROOT, "%,d", e.count)).formatted(Formatting.YELLOW));
                long each = BankService.priceOf(e.item);
                if (each > 0 && !e.item.equals(Inv.COINS)) {
                    lines.add(Text.literal("GE: " + GrandExchange.formatGp(each) + " gp ea, "
                            + GrandExchange.formatGp(BankModel.value(List.of(e), BankService::priceOf)) + " gp total").formatted(Formatting.GRAY));
                }
                lines.add(Text.literal("Click: withdraw " + BankClient.BANK.quantity.label + (BankClient.BANK.noted ? " (as note)" : "")
                        + ", right-click: All").formatted(Formatting.DARK_GRAY));
            }
            ctx.drawTooltip(textRenderer, lines, mouseX, mouseY);
            return;
        }
        int t = tabAt(mouseX, mouseY);
        if (t >= 0) {
            String label = t == 0 ? "View all items" : t > BankClient.BANK.tabCount() ? "New tab (drag an item here)" : "Tab " + t;
            ctx.drawTooltip(textRenderer, Text.literal(label), mouseX, mouseY);
            return;
        }
        int slot = invSlotAt(mouseX, mouseY);
        if (slot >= 0 && client != null && client.player != null) {
            ItemStack s = client.player.getInventory().main.get(slot);
            if (!s.isEmpty()) {
                ctx.drawTooltip(textRenderer, List.of(s.getName().copy(),
                        Text.literal("Click: deposit " + BankClient.BANK.quantity.label + ", right-click: All").formatted(Formatting.DARK_GRAY)), mouseX, mouseY);
            }
        } else if (slot == -2) {
            ctx.drawTooltip(textRenderer, Text.literal("Coins: " + String.format(Locale.ROOT, "%,d", ClientState.coins)), mouseX, mouseY);
        }
    }
}
