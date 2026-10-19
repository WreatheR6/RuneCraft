package com.runecraft.client;

import com.runecraft.combat.Equipment;
import com.runecraft.network.Net;
import com.runecraft.osrs.CollectionLogData;
import com.runecraft.osrs.CombatStyle;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Quest;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.Spell;
import com.runecraft.osrs.ui.SkillPanel;
import com.runecraft.util.Inv;
import com.runecraft.platform.client.ClientPlayNetworking;
import com.runecraft.platform.PacketByteBufs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.Locale;

/** The OSRS side-panel, as one screen with tabs. */
public class RuneScapeScreen extends Screen {
    public enum Tab { COMBAT("Combat", "combat"), SKILLS("Skills", "skills"), QUESTS("Quests", "quests"), PRAYER("Prayer", "prayer"),
        MAGIC("Magic", "magic"), EQUIPMENT("Gear", "gear"), COLLECTION("Log", "log");
        final String label;
        final String icon;

        Tab(String label, String icon) {
            this.label = label;
            this.icon = icon;
        }
    }

    private static final int PANEL_W = 300;
    /** Fits a 480x270 GUI (1080p at auto GUI scale) together with the tab row. */
    private static final int PANEL_H = 230;
    private static final int TAB_H = 20;
    /** The OSRS skills grid is CONTENT_H tall; shrink it slightly to fit this panel. */
    private static final float SKILLS_SCALE = Math.min(1f, (PANEL_H - 6) / (float) SkillPanel.CONTENT_H);
    private final SkillsTab skillsTab = new SkillsTab();
    private Tab tab;
    private int page;
    private int left;
    private int top;
    private com.runecraft.osrs.QuestInfo selectedQuest;
    private int selectedIndex;

    public RuneScapeScreen(Tab tab) {
        super(Component.literal("RuneScape"));
        this.tab = tab;
    }

    /** Opens the quest journal of {@code ContentRegistry.quests().get(index)} (Quests tab). */
    public void selectQuest(int index) {
        java.util.List<com.runecraft.osrs.QuestInfo> all = com.runecraft.osrs.content.ContentRegistry.quests();
        if (index >= 0 && index < all.size()) {
            selectedQuest = all.get(index);
            selectedIndex = index;
            page = index / 12;
        }
    }

    /** Shows collection log page {@code index} (Collection tab). */
    public void selectPage(int index) {
        page = Math.max(0, Math.min(index, CollectionLogData.pages().size() - 1));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        left = (width - PANEL_W) / 2;
        top = Math.max(TAB_H + 4, (height - PANEL_H + TAB_H) / 2);
        switch (tab) {
            case COMBAT -> initCombat();
            case PRAYER -> initPrayer();
            case MAGIC -> initMagic();
            case QUESTS -> initQuests();
            case COLLECTION -> initCollection();
            default -> {
            }
        }
    }

    private void switchTab(Tab t) {
        if (t == Tab.SKILLS && tab == Tab.SKILLS) {
            skillsTab.closeGuide();
        }
        tab = t;
        page = 0;
        rebuildWidgets();
    }

    private int tabWidth() {
        return PANEL_W / Tab.values().length;
    }

    /** The OSRS-style icon tab under the mouse, or null. */
    private Tab tabAt(double mouseX, double mouseY) {
        if (mouseY < top - TAB_H || mouseY >= top || mouseX < left || mouseX >= left + tabWidth() * Tab.values().length) {
            return null;
        }
        return Tab.values()[(int) ((mouseX - left) / tabWidth())];
    }

    private int skillsX() {
        return left + (int) ((PANEL_W - SkillPanel.CONTENT_W * SKILLS_SCALE) / 2);
    }

    private double skillsLocalX(double mouseX) {
        return (mouseX - skillsX()) / SKILLS_SCALE;
    }

    private double skillsLocalY(double mouseY) {
        return (mouseY - skillsY()) / SKILLS_SCALE;
    }

    private int skillsY() {
        return top + 3;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Tab t = tabAt(mouseX, mouseY);
        if (t != null && button == 0) {
            switchTab(t);
            return true;
        }
        if (tab == Tab.SKILLS && button == 0 && skillsTab.click(skillsLocalX(mouseX), skillsLocalY(mouseY))) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (tab == Tab.SKILLS && skillsTab.scroll(amount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    private static void send(ResourceLocation channel, int value) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(value);
        ClientPlayNetworking.send(channel, buf);
    }

    // ---------------- Combat ----------------

    private void initCombat() {
        CombatStyle[] melee = {CombatStyle.ACCURATE, CombatStyle.AGGRESSIVE, CombatStyle.CONTROLLED, CombatStyle.DEFENSIVE};
        CombatStyle[] ranged = {CombatStyle.RANGED_ACCURATE, CombatStyle.RAPID, CombatStyle.LONGRANGE};
        String[] meleeInfo = {"Accurate: +3 Attack, trains Attack", "Aggressive: +3 Strength, trains Strength",
                "Controlled: +1 all, trains Atk/Str/Def", "Defensive: +3 Defence, trains Defence"};
        String[] rangedInfo = {"Accurate: +3 Ranged, trains Ranged", "Rapid: faster, trains Ranged", "Longrange: trains Ranged + Defence"};
        for (int i = 0; i < melee.length; i++) {
            CombatStyle s = melee[i];
            addRenderableWidget(Button.builder(Component.literal((ClientState.style == s ? "> " : "") + s.displayName()), b -> {
                send(Net.SET_STYLE, s.ordinal());
                ClientState.style = s;
                rebuildWidgets();
            }).bounds(left + 10, top + 40 + i * 24, 135, 20).tooltip(Tooltip.create(Component.literal(meleeInfo[i]))).build());
        }
        for (int i = 0; i < ranged.length; i++) {
            CombatStyle s = ranged[i];
            addRenderableWidget(Button.builder(Component.literal((ClientState.style == s ? "> " : "") + "Bow: " + s.displayName()), b -> {
                send(Net.SET_STYLE, s.ordinal());
                ClientState.style = s;
                rebuildWidgets();
            }).bounds(left + 155, top + 40 + i * 24, 135, 20).tooltip(Tooltip.create(Component.literal(rangedInfo[i]))).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Auto Retaliate: " + (ClientState.autoRetaliate ? "On" : "Off")), b -> {
            ClientPlayNetworking.send(Net.TOGGLE_RETALIATE, PacketByteBufs.create());
            ClientState.autoRetaliate = !ClientState.autoRetaliate;
            rebuildWidgets();
        }).bounds(left + 155, top + 40 + ranged.length * 24, 135, 20)
                .tooltip(Tooltip.create(Component.literal("Fight back automatically when attacked."))).build());
    }

    // ---------------- Prayer ----------------

    private void initPrayer() {
        Prayer[] all = Prayer.values();
        int cols = 5;
        int bw = 56;
        for (int i = 0; i < all.length; i++) {
            Prayer p = all[i];
            boolean unlocked = ClientState.SKILLS.level(Skill.PRAYER) >= p.level();
            String label = (ClientState.prayerActive(p) ? "*" : "") + shortName(p.displayName());
            Button btn = Button.builder(Component.literal(label), b -> send(Net.TOGGLE_PRAYER, p.ordinal()))
                    .bounds(left + 5 + (i % cols) * (bw + 3), top + 22 + (i / cols) * 22, bw, 20)
                    .tooltip(Tooltip.create(Component.literal(p.displayName() + "\nLevel " + p.level() + "\nDrain: " + p.drainEffect())))
                    .build();
            btn.active = unlocked;
            addRenderableWidget(btn);
        }
    }

    private static String shortName(String name) {
        String[] words = name.split(" ");
        if (words.length == 1) {
            return name.length() > 9 ? name.substring(0, 8) + "." : name;
        }
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.equals("from") && !w.equals("of")) {
                sb.append(w, 0, Math.min(w.length(), 4)).append(' ');
            }
        }
        return sb.toString().trim();
    }

    // ---------------- Magic ----------------

    private void initMagic() {
        Spell[] all = Spell.values();
        int cols = 3;
        int bw = 94;
        for (int i = 0; i < all.length; i++) {
            Spell s = all[i];
            boolean unlocked = ClientState.SKILLS.level(Skill.MAGIC) >= s.level();
            StringBuilder runes = new StringBuilder();
            s.runes().forEach((r, n) -> runes.append(n).append(" x ").append(r.displayName()).append('\n'));
            String desc = s.displayName() + " (level " + s.level() + ")\n" + runes
                    + switch (s.kind()) {
                case COMBAT -> "Max hit " + s.maxHit() + ". Click to autocast, then right-click with a staff.";
                case TELEPORT -> "Teleport.";
                case ALCHEMY -> "Converts the item in your main hand to coins.";
                case UTILITY -> "Smelts ore in your inventory without a furnace.";
            };
            String label = (ClientState.autocast == s ? "> " : "") + s.displayName().replace("Teleport", "Tele").replace("Lumbridge Home", "Home");
            Button btn = Button.builder(Component.literal(label), b -> {
                send(Net.CAST_SPELL, s.ordinal());
                if (s.kind() != Spell.Kind.COMBAT) {
                    onClose();
                } else {
                    ClientState.autocast = s;
                    rebuildWidgets();
                }
            }).bounds(left + 5 + (i % cols) * (bw + 3), top + 6 + (i / cols) * 20, bw, 18)
                    .tooltip(Tooltip.create(Component.literal(desc))).build();
            btn.active = unlocked || s == Spell.HOME_TELEPORT;
            addRenderableWidget(btn);
        }
    }

    // ---------------- Quests ----------------

    private void initQuests() {
        java.util.List<com.runecraft.osrs.QuestInfo> all = com.runecraft.osrs.content.ContentRegistry.quests();
        int perPage = 12;
        int pages = Math.max(1, (all.size() + perPage - 1) / perPage);
        page = Math.min(page, pages - 1);
        for (int i = page * perPage; i < Math.min(all.size(), (page + 1) * perPage); i++) {
            com.runecraft.osrs.QuestInfo q = all.get(i);
            int index = i;
            int stage = ClientState.QUEST_STAGES[i];
            String prefix = stage == 0 ? "\u00a7c" : stage >= q.finalStage() ? "\u00a7a" : "\u00a7e";
            String name = q.displayName().length() > 20 ? q.displayName().substring(0, 19) + "." : q.displayName();
            addRenderableWidget(Button.builder(Component.literal(prefix + name), b -> {
                selectedQuest = q;
                selectedIndex = index;
            }).bounds(left + 5, top + 6 + (i - page * perPage) * 16, 130, 15).build());
        }
        if (pages > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                page = (page + pages - 1) % pages;
                rebuildWidgets();
            }).bounds(left + 5, top + PANEL_H - 22, 20, 18).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                page = (page + 1) % pages;
                rebuildWidgets();
            }).bounds(left + 115, top + PANEL_H - 22, 20, 18).build());
        }
    }

    // ---------------- Collection log ----------------

    private void initCollection() {
        int pages = CollectionLogData.pages().size();
        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            page = (page + pages - 1) % pages;
        }).bounds(left + 5, top + PANEL_H - 24, 20, 18).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            page = (page + 1) % pages;
        }).bounds(left + PANEL_W - 25, top + PANEL_H - 24, 20, 18).build());
    }

    // ---------------- Rendering ----------------

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        // OSRS stone frame with a row of icon tabs along the top.
        OsrsUi.stonePanel(ctx, left - 4, top - TAB_H - 4, PANEL_W + 8, PANEL_H + TAB_H + 8);
        Tab hoveredTab = tabAt(mouseX, mouseY);
        int tw = tabWidth();
        for (Tab t : Tab.values()) {
            int x = left + t.ordinal() * tw;
            OsrsUi.button(ctx, x, top - TAB_H, tw - 1, TAB_H - 1, t == tab, t == hoveredTab);
            OsrsUi.icon(ctx, OsrsUi.tabIcon(t.icon), x + (tw - 17) / 2, top - TAB_H + 2, 16);
        }
        ctx.fill(left - 1, top - 1, left + PANEL_W + 1, top + PANEL_H + 1, OsrsUi.EDGE);
        OsrsUi.stone(ctx, left, top, PANEL_W, PANEL_H);
        List<String> skillTip = null;
        switch (tab) {
            case SKILLS -> {
                ctx.pose().pushPose();
                ctx.pose().translate(skillsX(), skillsY(), 0);
                ctx.pose().scale(SKILLS_SCALE, SKILLS_SCALE, 1);
                skillTip = skillsTab.render(ctx, font, skillsLocalX(mouseX), skillsLocalY(mouseY));
                ctx.pose().popPose();
            }
            case COMBAT -> renderCombat(ctx);
            case PRAYER -> ctx.drawString(font, "Prayer points: " + ClientState.prayerPoints + " / "
                    + ClientState.SKILLS.currentLevel(Skill.PRAYER), left + 6, top + 8, 0xFF40E0FF);
            case MAGIC -> {
            }
            case QUESTS -> renderQuests(ctx);
            case EQUIPMENT -> renderEquipment(ctx);
            case COLLECTION -> renderCollection(ctx, mouseX, mouseY);
        }
        super.render(ctx, mouseX, mouseY, delta);
        if (skillTip != null) {
            ctx.pose().pushPose();
            ctx.pose().translate(skillsX(), skillsY(), 0);
            ctx.pose().scale(SKILLS_SCALE, SKILLS_SCALE, 1);
            OsrsUi.tooltip(ctx, font, skillTip, (int) skillsLocalX(mouseX), (int) skillsLocalY(mouseY),
                    (int) ((left + PANEL_W - skillsX()) / SKILLS_SCALE), (int) (PANEL_H / SKILLS_SCALE));
            ctx.pose().popPose();
        }
        if (hoveredTab != null) {
            ctx.renderTooltip(font, Component.literal(hoveredTab.label), mouseX, mouseY);
        }
    }

    private void renderCombat(GuiGraphics ctx) {
        ItemStack held = minecraft.player.getMainHandItem();
        String weapon = held.isEmpty() ? "Unarmed" : held.getHoverName().getString();
        ctx.drawString(font, weapon + "   Combat level: " + ClientState.SKILLS.combatLevel(), left + 8, top + 10, 0xFFFFD700);
        ctx.drawString(font, "Melee styles", left + 10, top + 28, 0xFFE0C080);
        ctx.drawString(font, "Ranged styles", left + 155, top + 28, 0xFFE0C080);
        ctx.drawString(font, "Autocast: " + (ClientState.autocast == null ? "none" : ClientState.autocast.displayName()),
                left + 10, top + 150, 0xFF80A0FF);
        ctx.drawString(font, "Attack speed: " + Equipment.weaponSpeed(minecraft.player, ClientState.SKILLS) + " ticks",
                left + 10, top + 164, 0xFFC0C0C0);
    }

    private void renderQuests(GuiGraphics ctx) {
        ctx.drawString(font, "Quest points: " + ClientState.questPoints + "/" + Quest.totalQuestPoints(),
                left + 140, top + 8, 0xFFFFD700);
        if (selectedQuest != null) {
            int stage = ClientState.QUEST_STAGES[selectedIndex];
            ctx.drawString(font, selectedQuest.displayName(), left + 140, top + 26, 0xFFFF9800);
            List<net.minecraft.util.FormattedCharSequence> wrapped = font.split(Component.literal(selectedQuest.journalFor(stage)), 150);
            int y = top + 42;
            for (net.minecraft.util.FormattedCharSequence line : wrapped) {
                ctx.drawString(font, line, left + 140, y, 0xFFE0E0E0);
                y += 10;
            }
            y += 6;
            ctx.drawString(font, "Rewards: " + selectedQuest.questPoints() + " QP", left + 140, y, 0xFFA0FFA0);
            for (var e : selectedQuest.xpRewards().entrySet()) {
                y += 10;
                ctx.drawString(font, String.format(Locale.ROOT, "%,.0f %s XP", e.getValue(), e.getKey().displayName()),
                        left + 140, y, 0xFFA0FFA0);
            }
        } else {
            ctx.drawString(font, "Select a quest.", left + 140, top + 26, 0xFFC0C0C0);
        }
    }

    private void renderEquipment(GuiGraphics ctx) {
        EquipmentStats s = Equipment.total(minecraft.player, ClientState.SKILLS);
        String[] lines = {
                "Attack bonus", "Stab: " + s.stabAttack(), "Slash: " + s.slashAttack(), "Crush: " + s.crushAttack(),
                "Magic: " + s.magicAttack(), "Range: " + s.rangedAttack(), "",
                "Other bonuses", "Melee strength: " + s.meleeStrength(), "Ranged strength: " + s.rangedStrength(),
                "Magic damage: " + s.magicDamagePercent() + "%", "Prayer: " + s.prayer()};
        String[] def = {"Defence bonus", "Stab: " + s.stabDefence(), "Slash: " + s.slashDefence(), "Crush: " + s.crushDefence(),
                "Magic: " + s.magicDefence(), "Range: " + s.rangedDefence()};
        for (int i = 0; i < lines.length; i++) {
            ctx.drawString(font, lines[i], left + 10, top + 10 + i * 12, i == 0 || i == 7 ? 0xFFFF9800 : 0xFFE0E0E0);
        }
        for (int i = 0; i < def.length; i++) {
            ctx.drawString(font, def[i], left + 160, top + 10 + i * 12, i == 0 ? 0xFFFF9800 : 0xFFE0E0E0);
        }
    }

    private void renderCollection(GuiGraphics ctx, int mouseX, int mouseY) {
        List<CollectionLogData.Page> pages = CollectionLogData.pages();
        CollectionLogData.Page p = pages.get(Math.max(0, Math.min(page, pages.size() - 1)));
        long obtained = p.items().stream().filter(ClientState.COLLECTION_LOG::contains).count();
        ctx.drawString(font, p.category() + " - " + p.name(), left + 8, top + 8, 0xFFFF9800);
        if (!CollectionLogData.kcKeys(p).isEmpty()) {
            String kc = (p.category().equals("Clues") || p.category().equals("Raids") ? "Completions: " : "Kills: ")
                    + CollectionLogData.killCount(p, ClientState.KILL_COUNTS);
            ctx.drawString(font, kc, left + PANEL_W - 10 - font.width(kc), top + 8, 0xFFE0E0E0);
        }
        ctx.drawString(font, "Obtained: " + obtained + "/" + p.items().size() + "   Total: "
                + ClientState.COLLECTION_LOG.size() + "/" + CollectionLogData.totalSlots(), left + 8, top + 20, 0xFFE0E0E0);
        int cols = 8;
        String hovered = null;
        for (int i = 0; i < p.items().size(); i++) {
            String item = p.items().get(i);
            int x = left + 10 + (i % cols) * 34;
            int y = top + 36 + (i / cols) * 34;
            boolean has = ClientState.COLLECTION_LOG.contains(item);
            ctx.fill(x, y, x + 30, y + 30, has ? 0xFF2E4A1E : 0xFF2A231A);
            ItemStack stack = Inv.stack(item, 1);
            ctx.renderItem(stack, x + 7, y + 7);
            if (!has) {
                ctx.fill(x, y, x + 30, y + 30, 0x90000000);
            }
            if (mouseX >= x && mouseX < x + 30 && mouseY >= y && mouseY < y + 30) {
                hovered = item;
            }
        }
        ctx.drawCenteredString(font, (page + 1) + " / " + pages.size(), left + PANEL_W / 2, top + PANEL_H - 19, 0xFFC0C0C0);
        if (hovered != null) {
            ItemDef def = ItemCatalog.get(hovered);
            ctx.renderTooltip(font, Component.literal(def == null ? hovered : def.name()), mouseX, mouseY);
        }
    }
}
