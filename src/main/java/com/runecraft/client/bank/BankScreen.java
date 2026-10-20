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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

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
    /** Inventory panel codes of the pouch cells under the pack (OSRS's coin and tokkul stacks), and their columns. */
    private static final int SLOT_COINS = -2;
    private static final int SLOT_TOKKUL = -3;
    private static final int COL_COINS = 0;
    private static final int COL_TOKKUL = 3;

    /** View state kept across refreshes and re-openings. */
    private static int tab;
    private static String search = "";
    private static int scroll;

    private final Map<String, ItemStack> stacks = new HashMap<>();
    private EditBox searchField;
    private EditBox xField;
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
        super(Component.literal("The Bank of Gielinor"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean depositOnly() {
        return BankClient.depositOnly;
    }

    /** New state from the server. */
    void refresh() {
        stacks.clear();
        rebuildWidgets();
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
            searchField = new EditBox(font, left + 8, y, 110, 16, Component.literal("Search"));
            searchField.setMaxLength(40);
            searchField.setValue(search);
            searchField.setHint(Component.literal("Search...").withStyle(ChatFormatting.DARK_GRAY));
            searchField.setResponder(t -> {
                search = t;
                scroll = 0;
                rebuildView();
            });
            addRenderableWidget(searchField);
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
            xField = new EditBox(font, x, y + 1, 48, 16, Component.literal("X"));
            xField.setMaxLength(12);
            xField.setValue(Long.toString(bank.x));
            addRenderableWidget(xField);
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
        Button b = Button.builder(Component.literal(label).withStyle(active ? ChatFormatting.YELLOW : ChatFormatting.WHITE), btn -> click.run())
                .bounds(x, y, w, 18).tooltip(Tooltip.create(Component.literal(tip))).build();
        addRenderableWidget(b);
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
        addRenderableWidget(Button.builder(Component.literal("Deposit inv."), b -> send(BankService.OP_DEPOSIT_INVENTORY, 0))
                .bounds(x, y, 62, 18).tooltip(Tooltip.create(Component.literal("Deposit inventory (and the coin and tokkul pouches)"))).build());
        addRenderableWidget(Button.builder(Component.literal("Deposit worn"), b -> send(BankService.OP_DEPOSIT_WORN, 0))
                .bounds(x + 64, y, 62, 18).tooltip(Tooltip.create(Component.literal("Deposit worn items"))).build());
    }

    private long xAmount() {
        long x = xField == null ? -1 : BankQuantity.parseX(xField.getValue());
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
            ItemStack s = Notes.stackOf(minecraft.level.registryAccess(), e.item, e.tag, 1);
            return s.isEmpty() ? new ItemStack(net.minecraft.world.item.Items.BARRIER) : s;
        });
    }

    private String nameOf(String item) {
        ItemDef def = ItemCatalog.get(item);
        if (def != null) {
            return def.name();
        }
        ItemStack s = Notes.stackOf(null, item, "", 1);
        return s.isEmpty() ? item : s.getHoverName().getString();
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

    /**
     * Inventory panel cell -> main inventory slot (rows of the backpack first, hotbar last); {@link #SLOT_COINS} and
     * {@link #SLOT_TOKKUL} are the pouches, -1 nothing.
     */
    private int invSlotAt(double mx, double my) {
        if (mx < invX || mx >= invX + INV_COLS * CELL || my < invY) {
            return -1;
        }
        int col = (int) ((mx - invX) / CELL);
        int row = (int) ((my - invY) / CELL);
        if (row < 6) {
            return displayToSlot(row * INV_COLS + col);
        }
        if (row == 6 && my >= invY + 6 * CELL + 2) {
            return col == COL_COINS ? SLOT_COINS : col == COL_TOKKUL ? SLOT_TOKKUL : -1;
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
                    searchField.setValue("");
                }
                search = "";
                rebuildView();
            }
            return true;
        }
        int slot = invSlotAt(mx, my);
        if (slot != -1) {
            BankQuantity.Mode mode = button == 1 ? BankQuantity.Mode.ALL : BankClient.BANK.quantity;
            if (slot == SLOT_COINS || slot == SLOT_TOKKUL) {
                BankClient.send(BankService.OP_DEPOSIT_POUCH, "", "", slot == SLOT_TOKKUL ? BankService.POUCH_TOKKUL : BankService.POUCH_COINS,
                        mode, xAmount(), depositTab());
            } else if (minecraft != null && minecraft.player != null && !minecraft.player.getInventory().items.get(slot).isEmpty()) {
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
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (!depositOnly() && mx >= gridX && mx < gridX + COLS * CELL && my >= gridY && my < gridY + rows * CELL) {
            scroll -= (int) Math.signum(scrollY);
            rebuildView();
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public void removed() {
        BankClient.send(BankService.OP_CLOSE, "", "", 0, BankQuantity.Mode.ONE, 0, 0);
        super.removed();
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void renderBackground(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);
        ctx.fill(left - 1, top - 1, left + panelW + 1, top + panelH + 1, 0xFF100C08);
        ctx.fill(left, top, left + panelW, top + panelH, PANEL_BG);
        String title = depositOnly() ? "The Bank of Gielinor - Deposit Box" : getTitle().getString();
        ctx.drawString(font, title, left + 8, top + 5, 0xFFFF981F);
        if (!depositOnly()) {
            String value = "Value: " + GrandExchange.formatGp(BankClient.value) + " gp";
            ctx.drawString(font, value, left + panelW - 8 - font.width(value), top + 5, 0xFFFFD700);
            drawTabs(ctx, mouseX, mouseY);
            drawGrid(ctx, mouseX, mouseY);
            String used = BankClient.BANK.slotsUsed() + "/" + BankModel.CAPACITY;
            ctx.drawString(font, used, left + 8 + 114, gridY + Math.max(rows * CELL, 6 * CELL + CELL + 2) + 8, 0xFFC0C0C0);
        }
        drawInventory(ctx);
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        if (dragging && pressed != null) {
            ctx.pose().pushPose();
            ctx.pose().translate(0, 0, 300);
            ctx.renderItem(stackOf(pressed), mouseX - 8, mouseY - 8);
            ctx.pose().popPose();
        } else {
            drawTooltip(ctx, mouseX, mouseY);
        }
    }

    private void drawTabs(GuiGraphics ctx, int mouseX, int mouseY) {
        BankModel bank = BankClient.BANK;
        int n = tabButtons();
        int hovered = tabAt(mouseX, mouseY);
        for (int i = 0; i < n; i++) {
            int x = left + 8 + i * (CELL + 2);
            int y = top + 18;
            boolean selected = i == tab && search.isBlank();
            ctx.fill(x, y, x + CELL, y + CELL, selected ? 0xFF6A2A1E : hovered == i ? 0xFF4A3E2E : 0xFF3A3024);
            ctx.renderOutline(x, y, CELL, CELL, 0xFF100C08);
            if (i == 0) {
                ctx.drawCenteredString(font, "All", x + CELL / 2, y + 6, 0xFFFFFFFF);
            } else if (i > bank.tabCount()) {
                ctx.drawCenteredString(font, "+", x + CELL / 2, y + 6, 0xFFFFFF60);
            } else {
                List<BankModel.Entry> inTab = bank.view(i, "", null);
                if (!inTab.isEmpty()) {
                    ctx.renderItem(stackOf(inTab.get(0)), x + 2, y + 2);
                }
            }
        }
    }

    private void drawGrid(GuiGraphics ctx, int mouseX, int mouseY) {
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
            ctx.renderItem(stackOf(e), x + 2, y + 2);
            ctx.pose().pushPose();
            ctx.pose().translate(0, 0, 200);
            if (e.placeholder()) {
                ctx.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, 0xA01E170F);
            } else if (e.count > 1 || ItemCatalog.get(e.item) == null || ItemCatalog.get(e.item).maxStack() > 1) {
                countText(ctx, e.count, x, y);
            }
            ctx.pose().popPose();
        }
        int total = (view.size() + COLS - 1) / COLS;
        if (total > rows) {
            int barH = Math.max(8, rows * CELL * rows / total);
            int barY = gridY + (rows * CELL - barH) * scroll / Math.max(1, total - rows);
            ctx.fill(gridX + COLS * CELL - 2, barY, gridX + COLS * CELL, barY + barH, 0xFF8A7A5A);
        }
        if (view.isEmpty()) {
            ctx.drawCenteredString(font, search.isBlank() ? "This tab is empty." : "No matches.",
                    gridX + COLS * CELL / 2, gridY + 8, 0xFF9F9F9F);
        }
    }

    private void countText(GuiGraphics ctx, long count, int x, int y) {
        ctx.pose().pushPose();
        ctx.pose().translate(x + 1, y + 1, 0);
        ctx.pose().scale(0.75f, 0.75f, 1f);
        ctx.drawString(font, BankQuantity.shortCount(count), 0, 0, 0xFF000000 | BankQuantity.countColor(count), true);
        ctx.pose().popPose();
    }

    private void drawInventory(GuiGraphics ctx) {
        ctx.fill(invX - 1, invY - 1, invX + INV_COLS * CELL + 1, invY + 6 * CELL + 1, GRID_BG);
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        Inventory inv = minecraft.player.getInventory();
        for (int d = 0; d < 36; d++) {
            ItemStack s = inv.items.get(displayToSlot(d));
            int x = invX + (d % INV_COLS) * CELL;
            int y = invY + (d / INV_COLS) * CELL;
            if (d >= 27) {
                ctx.fill(x, y, x + CELL, y + CELL, 0x18FFFFFF);
            }
            if (!s.isEmpty()) {
                ctx.renderItem(s, x + 2, y + 2);
                ctx.renderItemDecorations(font, s, x + 2, y + 2);
            }
        }
        // The coin and tokkul pouches (click deposits like a stack: the selected quantity, right-click All).
        pouch(ctx, COL_COINS, Inv.COINS, ClientState.coins, "Coins");
        pouch(ctx, COL_TOKKUL, Inv.TOKKUL, ClientState.tokkul, "Tokkul");
    }

    private void pouch(GuiGraphics ctx, int col, String item, long count, String label) {
        int px = invX + col * CELL;
        int py = invY + 6 * CELL + 2;
        ctx.fill(px - 1, py - 1, px + CELL + 1, py + CELL + 1, GRID_BG);
        ItemStack icon = Inv.stack(item, 1);
        if (!icon.isEmpty()) {
            ctx.renderItem(icon, px + 2, py + 2);
        }
        ctx.pose().pushPose();
        ctx.pose().translate(0, 0, 200);
        countText(ctx, count, px, py);
        ctx.pose().popPose();
        ctx.drawString(font, label, px + CELL + 4, py + 6, 0xFFC0C0C0);
    }

    private void drawTooltip(GuiGraphics ctx, int mouseX, int mouseY) {
        BankModel.Entry e = entryAt(mouseX, mouseY);
        if (e != null) {
            List<Component> lines = new ArrayList<>();
            lines.add(stackOf(e).getHoverName().copy().withStyle(ChatFormatting.GOLD));
            if (e.placeholder()) {
                lines.add(Component.literal("Placeholder").withStyle(ChatFormatting.GRAY));
            } else {
                lines.add(Component.literal(String.format(Locale.ROOT, "%,d", e.count)).withStyle(ChatFormatting.YELLOW));
                long each = BankService.priceOf(e.item);
                if (each > 0 && !e.item.equals(Inv.COINS)) {
                    lines.add(Component.literal("GE: " + GrandExchange.formatGp(each) + " gp ea, "
                            + GrandExchange.formatGp(BankModel.value(List.of(e), BankService::priceOf)) + " gp total").withStyle(ChatFormatting.GRAY));
                }
                lines.add(Component.literal("Click: withdraw " + BankClient.BANK.quantity.label + (BankClient.BANK.noted ? " (as note)" : "")
                        + ", right-click: All").withStyle(ChatFormatting.DARK_GRAY));
            }
            ctx.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        int t = tabAt(mouseX, mouseY);
        if (t >= 0) {
            String label = t == 0 ? "View all items" : t > BankClient.BANK.tabCount() ? "New tab (drag an item here)" : "Tab " + t;
            ctx.renderTooltip(font, Component.literal(label), mouseX, mouseY);
            return;
        }
        int slot = invSlotAt(mouseX, mouseY);
        if (slot >= 0 && minecraft != null && minecraft.player != null) {
            ItemStack s = minecraft.player.getInventory().items.get(slot);
            if (!s.isEmpty()) {
                ctx.renderComponentTooltip(font, List.of(s.getHoverName().copy(),
                        Component.literal("Click: deposit " + BankClient.BANK.quantity.label + ", right-click: All").withStyle(ChatFormatting.DARK_GRAY)), mouseX, mouseY);
            }
        } else if (slot == SLOT_COINS || slot == SLOT_TOKKUL) {
            boolean coins = slot == SLOT_COINS;
            ctx.renderComponentTooltip(font, List.of(Component.literal((coins ? "Coins: " : "Tokkul: ")
                            + String.format(Locale.ROOT, "%,d", coins ? ClientState.coins : ClientState.tokkul)),
                    Component.literal("Click: deposit " + BankClient.BANK.quantity.label + ", right-click: All").withStyle(ChatFormatting.DARK_GRAY)),
                    mouseX, mouseY);
        }
    }
}
