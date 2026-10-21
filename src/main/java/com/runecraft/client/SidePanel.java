package com.runecraft.client;

import com.runecraft.combat.Equipment;
import com.runecraft.network.Net;
import com.runecraft.osrs.CollectionLogData;
import com.runecraft.osrs.CombatStyle;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.PrayerGrace;
import com.runecraft.osrs.Quest;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.Spell;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.ui.SkillPanel;
import com.runecraft.platform.client.ClientPlayNetworking;
import com.runecraft.platform.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/**
 * The OSRS side panel drawn over the world at the bottom-right of the screen: a stone frame with
 * seven icon tabs along the top (Combat, Skills, Quests, Collection log, Equipment, Prayer, Magic),
 * a row along the bottom (open the full interface, hide the panel) and the selected tab's
 * interface in between. Skills is the default tab.
 *
 * <p>It is laid out in OSRS pixels ({@link SkillPanel#PANEL_W} x {@link SkillPanel#PANEL_H}) and
 * scaled so it fills roughly the bottom half of the screen. The HUD draws it every frame while it
 * is visible; holding the "use side panel" key opens {@link SidePanelScreen}, which frees the
 * cursor and makes it interactive (hover tooltips, clicking tabs, skills, prayers, spells...).
 */
public final class SidePanel {
    public enum PanelTab {
        COMBAT("combat", "Combat Options"), SKILLS("skills", "Skills"), QUESTS("quests", "Quest List"),
        LOG("log", "Collection Log"), GEAR("gear", "Worn Equipment"), PRAYER("prayer", "Prayer"), MAGIC("magic", "Magic");

        final String icon;
        final String label;

        PanelTab(String icon, String label) {
            this.icon = icon;
            this.label = label;
        }
    }

    /** Bottom-row tab slots (indices 7-13 count from the left of the bottom row). */
    private static final int EXPAND_TAB = 10;
    private static final int CLOSE_TAB = 13;
    private static final int LIST_ROW = 10;

    public static boolean visible = true;
    public static PanelTab tab = PanelTab.SKILLS;
    static final SkillsTab SKILLS_TAB = new SkillsTab();
    private static int questScroll;
    private static int logScroll;

    private SidePanel() {
    }

    /** Where the last open tab is remembered between sessions (a client-only convenience; any failure is ignored). */
    private static java.nio.file.Path tabFile() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("runecraft-client.properties");
    }

    /** Opens the panel on the tab that was open last time (round 41: it always opened on Skills). */
    public static void loadTab() {
        try {
            java.nio.file.Path file = tabFile();
            if (!java.nio.file.Files.exists(file)) {
                return;
            }
            java.util.Properties props = new java.util.Properties();
            try (java.io.Reader r = java.nio.file.Files.newBufferedReader(file)) {
                props.load(r);
            }
            String name = props.getProperty("sidePanelTab");
            if (name != null) {
                tab = PanelTab.valueOf(name);
            }
        } catch (Exception | LinkageError e) {
            // Keep the default tab.
        }
    }

    private static void saveTab() {
        try {
            java.nio.file.Path file = tabFile();
            java.nio.file.Files.createDirectories(file.getParent());
            java.util.Properties props = new java.util.Properties();
            props.setProperty("sidePanelTab", tab.name());
            try (java.io.Writer w = java.nio.file.Files.newBufferedWriter(file)) {
                props.store(w, "RuneCraft client settings");
            }
        } catch (Exception e) {
            // Not remembered this time.
        }
    }

    /** Where the panel sits on screen (GUI units) and how many GUI units one OSRS pixel takes. */
    record Layout(double x, double y, double scale) {
        double localX(double mouseX) {
            return (mouseX - x) / scale;
        }

        double localY(double mouseY) {
            return (mouseY - y) / scale;
        }
    }

    static Layout layout(int screenW, int screenH) {
        Minecraft mc = Minecraft.getInstance();
        double gui = Math.max(1.0, mc.getWindow().getGuiScale());
        double scale = SkillPanel.pixelsPerUnit(mc.getWindow().getHeight()) / gui;
        // Tiny windows: never let the panel climb into the HUD orbs at the top.
        double maxScale = Math.max(0.2, (screenH - 52) / (double) SkillPanel.PANEL_H);
        scale = Math.min(scale, maxScale);
        return new Layout(screenW - SkillPanel.PANEL_W * scale, screenH - SkillPanel.PANEL_H * scale, scale);
    }

    public static boolean contains(int screenW, int screenH, double mouseX, double mouseY) {
        Layout l = layout(screenW, screenH);
        double x = l.localX(mouseX);
        double y = l.localY(mouseY);
        return x >= 0 && y >= 0 && x < SkillPanel.PANEL_W && y < SkillPanel.PANEL_H;
    }

    // ------------------------------------------------------------------ rendering

    /** Draws the panel. Pass NaN mouse coordinates when the cursor is captured (no hover). */
    public static void render(GuiGraphics ctx, int screenW, int screenH, double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        Font tr = mc.font;
        Layout l = layout(screenW, screenH);
        double mx = l.localX(mouseX);
        double my = l.localY(mouseY);

        ctx.pose().pushPose();
        ctx.pose().translate(l.x(), l.y(), 0);
        ctx.pose().scale((float) l.scale(), (float) l.scale(), 1);

        OsrsUi.stonePanel(ctx, 0, 0, SkillPanel.PANEL_W, SkillPanel.PANEL_H);
        int hoveredTab = SkillPanel.tabAt(mx, my);
        for (int i = 0; i < SkillPanel.TABS_PER_ROW * 2; i++) {
            int x = SkillPanel.tabX(i);
            int y = SkillPanel.tabY(i);
            PanelTab t = i < SkillPanel.TABS_PER_ROW ? PanelTab.values()[i] : null;
            OsrsUi.button(ctx, x, y + 2, SkillPanel.TAB_W - 1, SkillPanel.TAB_H - 4, t != null && t == tab, i == hoveredTab);
            String icon = t != null ? t.icon : i == EXPAND_TAB ? "expand" : i == CLOSE_TAB ? "close" : null;
            if (icon != null) {
                OsrsUi.icon(ctx, OsrsUi.tabIcon(icon), x + 4, y + 6, 24);
            }
        }
        // Content area frame
        int cx = SkillPanel.CONTENT_X;
        int cy = SkillPanel.CONTENT_Y;
        ctx.renderOutline(cx - 1, cy - 1, SkillPanel.CONTENT_W + 2, SkillPanel.CONTENT_H + 2, OsrsUi.EDGE);

        ctx.pose().pushPose();
        ctx.pose().translate(cx, cy, 0);
        double lx = mx - cx;
        double ly = my - cy;
        List<String> tip = switch (tab) {
            case SKILLS -> SKILLS_TAB.render(ctx, tr, lx, ly);
            case COMBAT -> renderCombat(ctx, tr, lx, ly);
            case PRAYER -> renderPrayer(ctx, tr, lx, ly);
            case MAGIC -> renderMagic(ctx, tr, lx, ly);
            case QUESTS -> renderQuests(ctx, tr, lx, ly);
            case GEAR -> renderGear(ctx, tr);
            case LOG -> renderLog(ctx, tr, lx, ly);
        };
        if (tip != null && !Double.isNaN(lx)) {
            OsrsUi.tooltip(ctx, tr, tip, (int) lx, (int) ly, SkillPanel.CONTENT_W, SkillPanel.CONTENT_H);
        }
        ctx.pose().popPose();

        if (hoveredTab >= 0) {
            String label = hoveredTab < SkillPanel.TABS_PER_ROW ? PanelTab.values()[hoveredTab].label
                    : hoveredTab == EXPAND_TAB ? "Open full interface" : hoveredTab == CLOSE_TAB ? "Hide side panel" : null;
            if (label != null) {
                OsrsUi.tooltip(ctx, tr, List.of(label), (int) mx, (int) my, SkillPanel.PANEL_W, SkillPanel.PANEL_H);
            }
        }
        ctx.pose().popPose();
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static int center() {
        return SkillPanel.CONTENT_W / 2;
    }

    // ---- Combat ----

    private static final CombatStyle[] MELEE = {CombatStyle.ACCURATE, CombatStyle.AGGRESSIVE, CombatStyle.CONTROLLED, CombatStyle.DEFENSIVE};
    private static final CombatStyle[] RANGED = {CombatStyle.RANGED_ACCURATE, CombatStyle.RAPID, CombatStyle.LONGRANGE};
    private static final String[] MELEE_INFO = {"Accurate: +3 Attack", "Aggressive: +3 Strength", "Controlled: +1 all",
            "Defensive: +3 Defence"};
    private static final String[] RANGED_INFO = {"Accurate: +3 Ranged", "Rapid: faster attacks", "Longrange: Ranged + Defence"};

    private static int meleeX(int i) {
        return 2 + (i % 2) * 94;
    }

    private static int meleeY(int i) {
        return 30 + (i / 2) * 40;
    }

    private static int rangedX(int i) {
        return 2 + i * 63;
    }

    private static final int RANGED_Y = 126;

    private static List<String> renderCombat(GuiGraphics ctx, Font tr, double mx, double my) {
        Minecraft mc = Minecraft.getInstance();
        ItemStack held = mc.player.getMainHandItem();
        String weapon = held.isEmpty() ? "Unarmed" : held.getHoverName().getString();
        OsrsUi.centered(ctx, tr, weapon, center(), 4, OsrsUi.ORANGE);
        OsrsUi.centered(ctx, tr, "Combat Lvl: " + ClientState.SKILLS.combatLevel(), center(), 16, OsrsUi.YELLOW);
        List<String> tip = null;
        for (int i = 0; i < MELEE.length; i++) {
            boolean active = ClientState.style == MELEE[i];
            boolean hover = in(mx, my, meleeX(i), meleeY(i), 92, 36);
            OsrsUi.button(ctx, meleeX(i), meleeY(i), 92, 36, active, hover);
            OsrsUi.centered(ctx, tr, MELEE[i].displayName(), meleeX(i) + 46, meleeY(i) + 14, active ? OsrsUi.WHITE : OsrsUi.ORANGE);
            if (hover) {
                tip = List.of(MELEE_INFO[i]);
            }
        }
        ctx.drawString(tr, "Bow styles", 4, RANGED_Y - 12, OsrsUi.ORANGE);
        for (int i = 0; i < RANGED.length; i++) {
            boolean active = ClientState.style == RANGED[i];
            boolean hover = in(mx, my, rangedX(i), RANGED_Y, 61, 30);
            OsrsUi.button(ctx, rangedX(i), RANGED_Y, 61, 30, active, hover);
            String name = RANGED[i].displayName();
            if (tr.width(name) > 57) {
                name = tr.plainSubstrByWidth(name, 57);
            }
            OsrsUi.centered(ctx, tr, name, rangedX(i) + 30, RANGED_Y + 11, active ? OsrsUi.WHITE : OsrsUi.ORANGE);
            if (hover) {
                tip = List.of(RANGED_INFO[i]);
            }
        }
        ctx.drawString(tr, "Autocast: " + (ClientState.autocast == null ? "none" : ClientState.autocast.displayName()),
                4, 172, 0xFF80A0FF);
        ctx.drawString(tr, "Attack speed: " + Equipment.weaponSpeed(mc.player, ClientState.SKILLS) + " ticks",
                4, 186, OsrsUi.WHITE);
        boolean hoverRetaliate = in(mx, my, 2, RETALIATE_Y, 186, 30);
        OsrsUi.button(ctx, 2, RETALIATE_Y, 186, 30, ClientState.autoRetaliate, hoverRetaliate);
        OsrsUi.centered(ctx, tr, "Auto Retaliate (" + (ClientState.autoRetaliate ? "On" : "Off") + ")", 95, RETALIATE_Y + 11,
                ClientState.autoRetaliate ? OsrsUi.WHITE : OsrsUi.ORANGE);
        if (hoverRetaliate) {
            tip = List.of("Auto Retaliate", "Fight back automatically when attacked.");
        }
        return tip;
    }

    private static final int RETALIATE_Y = 204;

    private static boolean clickCombat(double mx, double my) {
        for (int i = 0; i < MELEE.length; i++) {
            if (in(mx, my, meleeX(i), meleeY(i), 92, 36)) {
                setStyle(MELEE[i]);
                return true;
            }
        }
        for (int i = 0; i < RANGED.length; i++) {
            if (in(mx, my, rangedX(i), RANGED_Y, 61, 30)) {
                setStyle(RANGED[i]);
                return true;
            }
        }
        if (in(mx, my, 2, RETALIATE_Y, 186, 30)) {
            ClientPlayNetworking.send(Net.TOGGLE_RETALIATE, PacketByteBufs.create());
            ClientState.autoRetaliate = !ClientState.autoRetaliate;
            return true;
        }
        return false;
    }

    private static void setStyle(CombatStyle s) {
        send(Net.SET_STYLE, s.ordinal());
        ClientState.style = s;
    }

    // ---- Prayer ----

    private static final int PRAYER_BOX = 37;
    /** Row height: 31 prayers make 7 rows, which must fit the 261-pixel content area with the points line. */
    private static final int PRAYER_ROW = 34;

    private static Skill prayerIcon(Prayer p) {
        return switch (p.group()) {
            case ATTACK, MELEE_ALL -> Skill.ATTACK;
            case STRENGTH -> Skill.STRENGTH;
            case DEFENCE -> Skill.DEFENCE;
            case RANGED, RANGED_ALL -> Skill.RANGED;
            case MAGIC, MAGIC_ALL -> Skill.MAGIC;
            default -> Skill.PRAYER;
        };
    }

    static String initials(String name) {
        StringBuilder sb = new StringBuilder();
        for (String w : name.split(" ")) {
            if (!w.isEmpty() && !w.equals("of") && !w.equals("from") && !w.equals("the")) {
                sb.append(Character.toUpperCase(w.charAt(0)));
            }
        }
        return sb.length() > 3 ? sb.substring(0, 3) : sb.toString();
    }

    private static List<String> renderPrayer(GuiGraphics ctx, Font tr, double mx, double my) {
        Prayer[] all = Prayer.values();
        int level = ClientState.SKILLS.level(Skill.PRAYER);
        List<String> tip = null;
        for (int i = 0; i < all.length; i++) {
            Prayer p = all[i];
            int x = 2 + (i % 5) * PRAYER_BOX;
            int y = 2 + (i / 5) * PRAYER_ROW;
            boolean active = ClientState.prayerActive(p);
            boolean unlocked = level >= p.level();
            boolean hover = in(mx, my, x, y, PRAYER_BOX - 1, PRAYER_ROW - 1);
            if (active) {
                ctx.fill(x + 2, y + 2, x + PRAYER_BOX - 3, y + PRAYER_ROW - 3, 0x90F0E0A0);
            } else if (hover && unlocked) {
                ctx.fill(x + 2, y + 2, x + PRAYER_BOX - 3, y + PRAYER_ROW - 3, 0x20FFFFFF);
            }
            OsrsUi.icon(ctx, OsrsUi.skillIcon(prayerIcon(p)), x + 8, y + 2, 20);
            // Overheads get distinct, style-coloured labels (OSRS draws a different icon for each); others their initials.
            String tag = PrayerGrace.overheadTag(p);
            int labelColor = !unlocked ? OsrsUi.GREY : tag != null ? PrayerGrace.overheadColor(p) : OsrsUi.WHITE;
            OsrsUi.centered(ctx, tr, tag != null ? tag : initials(p.displayName()), x + 18, y + 23, labelColor);
            if (active && p.isOverhead()) {
                ctx.renderOutline(x + 1, y + 1, PRAYER_BOX - 2, PRAYER_ROW - 2, 0xFFFFFFA0);
                ctx.renderOutline(x + 2, y + 2, PRAYER_BOX - 4, PRAYER_ROW - 4, 0xFFFFFFA0);
            }
            if ((ClientState.quickPrayerMask & (1L << p.ordinal())) != 0) {
                ctx.fill(x + PRAYER_BOX - 9, y + 3, x + PRAYER_BOX - 4, y + 8, 0xFFFFD700);
            }
            if (!unlocked) {
                ctx.fill(x + 1, y + 1, x + PRAYER_BOX - 2, y + PRAYER_ROW - 2, 0x90000000);
            }
            if (hover) {
                List<String> lines = new ArrayList<>(List.of(p.displayName(), "Level " + p.level(), "Drain: " + p.drainEffect()));
                int key = PrayerGrace.index(p);
                if (key >= 0) {
                    lines.add("Key: " + CombatKeys.keyName(key));
                }
                lines.add("Shift-click: " + ((ClientState.quickPrayerMask & (1L << p.ordinal())) != 0 ? "remove from" : "add to")
                        + " quick-prayers");
                tip = lines;
            }
        }
        OsrsUi.centered(ctx, tr, "Prayer points: " + ClientState.prayerPoints + " / " + ClientState.SKILLS.currentLevel(Skill.PRAYER),
                center(), 2 + ((all.length + 4) / 5) * PRAYER_ROW + 4, 0xFF40E0FF);
        return tip;
    }

    private static boolean clickPrayer(double mx, double my) {
        Prayer[] all = Prayer.values();
        for (int i = 0; i < all.length; i++) {
            int x = 2 + (i % 5) * PRAYER_BOX;
            int y = 2 + (i / 5) * PRAYER_ROW;
            if (in(mx, my, x, y, PRAYER_BOX - 1, PRAYER_ROW - 1)) {
                if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                    // OSRS Quick-prayers: shift-click picks the prayers the quick-prayer key switches.
                    long mask = ClientState.quickPrayerMask ^ (1L << all[i].ordinal());
                    ClientState.quickPrayerMask = mask;
                    FriendlyByteBuf buf = PacketByteBufs.create();
                    buf.writeVarLong(mask);
                    ClientPlayNetworking.send(Net.PRAYER_QUICK_SET, buf);
                } else if (ClientState.SKILLS.level(Skill.PRAYER) >= all[i].level()) {
                    CombatKeys.set(all[i], !ClientState.prayerActive(all[i]));
                }
                return true;
            }
        }
        return false;
    }

    // ---- Magic ----

    private static final int SPELL_BOX = 31;

    private static int spellColor(Spell s) {
        String n = s.displayName();
        if (s.kind() == Spell.Kind.TELEPORT) return 0xFF8060E0;
        if (s.kind() == Spell.Kind.ALCHEMY) return 0xFFE0C040;
        if (n.startsWith("Wind")) return 0xFFD8D8D8;
        if (n.startsWith("Water")) return 0xFF3070E0;
        if (n.startsWith("Earth")) return 0xFF5A9A3A;
        if (n.startsWith("Fire")) return 0xFFE05020;
        return 0xFFB09070;
    }

    private static List<String> spellTooltip(Spell s) {
        List<String> lines = new ArrayList<>();
        lines.add(s.displayName() + " (level " + s.level() + ")");
        s.runes().forEach((r, n) -> lines.add(n + " x " + r.displayName()));
        lines.add(switch (s.kind()) {
            case COMBAT -> "Max hit " + s.maxHit() + ". Click to autocast.";
            case TELEPORT -> "Teleport.";
            case ALCHEMY -> "Alchs the item in your main hand.";
            case UTILITY -> "Smelts ore without a furnace.";
        });
        return lines;
    }

    private static List<String> renderMagic(GuiGraphics ctx, Font tr, double mx, double my) {
        Spell[] all = Spell.values();
        int level = ClientState.SKILLS.level(Skill.MAGIC);
        List<String> tip = null;
        for (int i = 0; i < all.length; i++) {
            Spell s = all[i];
            int x = 2 + (i % 6) * SPELL_BOX;
            int y = 2 + (i / 6) * SPELL_BOX;
            boolean unlocked = level >= s.level() || s == Spell.HOME_TELEPORT;
            boolean hover = in(mx, my, x, y, SPELL_BOX - 2, SPELL_BOX - 2);
            OsrsUi.button(ctx, x, y, SPELL_BOX - 2, SPELL_BOX - 2, ClientState.autocast == s, hover && unlocked);
            ctx.fill(x + 6, y + 4, x + SPELL_BOX - 8, y + 16, spellColor(s));
            OsrsUi.centered(ctx, tr, initials(s.displayName().replace("Lumbridge Home", "Home")), x + (SPELL_BOX - 2) / 2, y + 18,
                    unlocked ? OsrsUi.WHITE : OsrsUi.GREY);
            if (!unlocked) {
                ctx.fill(x + 1, y + 1, x + SPELL_BOX - 3, y + SPELL_BOX - 3, 0x90000000);
            }
            if (hover) {
                tip = spellTooltip(s);
            }
        }
        int rows = (all.length + 5) / 6;
        ctx.drawString(tr, "Autocast: " + (ClientState.autocast == null ? "none" : ClientState.autocast.displayName()),
                4, 4 + rows * SPELL_BOX, 0xFF80A0FF);
        return tip;
    }

    private static boolean clickMagic(double mx, double my) {
        Spell[] all = Spell.values();
        int level = ClientState.SKILLS.level(Skill.MAGIC);
        for (int i = 0; i < all.length; i++) {
            Spell s = all[i];
            int x = 2 + (i % 6) * SPELL_BOX;
            int y = 2 + (i / 6) * SPELL_BOX;
            if (in(mx, my, x, y, SPELL_BOX - 2, SPELL_BOX - 2)) {
                if (level >= s.level() || s == Spell.HOME_TELEPORT) {
                    send(Net.CAST_SPELL, s.ordinal());
                    if (s.kind() == Spell.Kind.COMBAT) {
                        ClientState.autocast = s;
                    } else {
                        closeCursorScreen();
                    }
                }
                return true;
            }
        }
        return false;
    }

    // ---- Quests ----

    private static int listRows(int top) {
        return (SkillPanel.CONTENT_H - top - 2) / LIST_ROW;
    }

    private static final int QUEST_TOP = 16;

    private static List<String> renderQuests(GuiGraphics ctx, Font tr, double mx, double my) {
        List<QuestInfo> all = ContentRegistry.quests();
        OsrsUi.centered(ctx, tr, "Quest Points: " + ClientState.questPoints + "/" + Quest.totalQuestPoints(), center(), 3, OsrsUi.ORANGE);
        int rows = listRows(QUEST_TOP);
        questScroll = Math.max(0, Math.min(questScroll, all.size() - rows));
        OsrsUi.inset(ctx, 0, QUEST_TOP - 2, SkillPanel.CONTENT_W, SkillPanel.CONTENT_H - QUEST_TOP + 2);
        List<String> tip = null;
        for (int i = 0; i < rows && questScroll + i < all.size(); i++) {
            int index = questScroll + i;
            QuestInfo q = all.get(index);
            int stage = index < ClientState.QUEST_STAGES.length ? ClientState.QUEST_STAGES[index] : 0;
            int color = stage == 0 ? 0xFFFF0000 : stage >= q.finalStage() ? 0xFF00FF00 : OsrsUi.YELLOW;
            int y = QUEST_TOP + i * LIST_ROW;
            String name = q.displayName();
            String shown = tr.width(name) > 176 ? tr.plainSubstrByWidth(name, 170) + ".." : name;
            ctx.drawString(tr, shown, 4, y, color);
            if (in(mx, my, 2, y - 1, SkillPanel.CONTENT_W - 4, LIST_ROW)) {
                tip = List.of(name, stage == 0 ? "Not started" : stage >= q.finalStage() ? "Completed" : "In progress", "Click to read");
            }
        }
        return tip;
    }

    private static boolean clickQuests(double mx, double my) {
        int row = (int) Math.floor((my - QUEST_TOP + 1) / LIST_ROW);
        int index = questScroll + row;
        if (my >= QUEST_TOP - 1 && row >= 0 && row < listRows(QUEST_TOP) && index < ContentRegistry.quests().size()) {
            RuneScapeScreen screen = new RuneScapeScreen(RuneScapeScreen.Tab.QUESTS);
            screen.selectQuest(index);
            Minecraft.getInstance().setScreen(screen);
            return true;
        }
        return false;
    }

    // ---- Equipment ----

    private static String signed(int v) {
        return v >= 0 ? "+" + v : String.valueOf(v);
    }

    private static List<String> renderGear(GuiGraphics ctx, Font tr) {
        Minecraft mc = Minecraft.getInstance();
        EquipmentStats s = Equipment.total(mc.player, ClientState.SKILLS);
        String[][] rows = {
                {"Attack bonus", null},
                {"Stab", signed(s.stabAttack())}, {"Slash", signed(s.slashAttack())}, {"Crush", signed(s.crushAttack())},
                {"Magic", signed(s.magicAttack())}, {"Range", signed(s.rangedAttack())},
                {"Defence bonus", null},
                {"Stab", signed(s.stabDefence())}, {"Slash", signed(s.slashDefence())}, {"Crush", signed(s.crushDefence())},
                {"Magic", signed(s.magicDefence())}, {"Range", signed(s.rangedDefence())},
                {"Other bonuses", null},
                {"Melee strength", signed(s.meleeStrength())}, {"Ranged strength", signed(s.rangedStrength())},
                {"Magic damage", s.magicDamagePercent() + "%"}, {"Prayer", signed(s.prayer())}};
        int y = 3;
        for (String[] r : rows) {
            if (r[1] == null) {
                if (y > 3) {
                    y += 3;
                }
                ctx.drawString(tr, r[0], 4, y, OsrsUi.ORANGE);
            } else {
                ctx.drawString(tr, r[0] + ":", 10, y, OsrsUi.YELLOW);
                ctx.drawString(tr, r[1], SkillPanel.CONTENT_W - 6 - tr.width(r[1]), y, OsrsUi.YELLOW);
            }
            y += 13;
        }
        return null;
    }

    // ---- Collection log ----

    private static final int LOG_TOP = 28;

    private static List<String> renderLog(GuiGraphics ctx, Font tr, double mx, double my) {
        List<CollectionLogData.Page> pages = CollectionLogData.pages();
        OsrsUi.centered(ctx, tr, "Collection Log", center(), 3, OsrsUi.ORANGE);
        OsrsUi.centered(ctx, tr, "Obtained: " + ClientState.COLLECTION_LOG.size() + "/" + CollectionLogData.totalSlots(), center(), 14,
                OsrsUi.YELLOW);
        int rows = listRows(LOG_TOP);
        logScroll = Math.max(0, Math.min(logScroll, pages.size() - rows));
        OsrsUi.inset(ctx, 0, LOG_TOP - 2, SkillPanel.CONTENT_W, SkillPanel.CONTENT_H - LOG_TOP + 2);
        List<String> tip = null;
        for (int i = 0; i < rows && logScroll + i < pages.size(); i++) {
            CollectionLogData.Page p = pages.get(logScroll + i);
            long got = p.items().stream().filter(ClientState.COLLECTION_LOG::contains).count();
            int color = got == p.items().size() ? 0xFF00FF00 : got > 0 ? OsrsUi.YELLOW : OsrsUi.ORANGE;
            int y = LOG_TOP + i * LIST_ROW;
            String count = got + "/" + p.items().size();
            int maxW = SkillPanel.CONTENT_W - 12 - tr.width(count);
            String name = tr.width(p.name()) > maxW ? tr.plainSubstrByWidth(p.name(), maxW - 6) + ".." : p.name();
            ctx.drawString(tr, name, 4, y, color);
            ctx.drawString(tr, count, SkillPanel.CONTENT_W - 4 - tr.width(count), y, color);
            if (in(mx, my, 2, y - 1, SkillPanel.CONTENT_W - 4, LIST_ROW)) {
                tip = List.of(p.category() + " - " + p.name(), "Click to open");
            }
        }
        return tip;
    }

    private static boolean clickLog(double mx, double my) {
        int row = (int) Math.floor((my - LOG_TOP + 1) / LIST_ROW);
        int index = logScroll + row;
        if (my >= LOG_TOP - 1 && row >= 0 && row < listRows(LOG_TOP) && index < CollectionLogData.pages().size()) {
            RuneScapeScreen screen = new RuneScapeScreen(RuneScapeScreen.Tab.COLLECTION);
            screen.selectPage(index);
            Minecraft.getInstance().setScreen(screen);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ input

    /** Handles a click (screen coordinates). Returns true when the click landed on the panel. */
    public static boolean mouseClicked(int screenW, int screenH, double mouseX, double mouseY, int button) {
        if (!visible) {
            return false;
        }
        Layout l = layout(screenW, screenH);
        double mx = l.localX(mouseX);
        double my = l.localY(mouseY);
        if (mx < 0 || my < 0 || mx >= SkillPanel.PANEL_W || my >= SkillPanel.PANEL_H) {
            return false;
        }
        if (button != 0) {
            return true;
        }
        int t = SkillPanel.tabAt(mx, my);
        if (t >= 0) {
            if (t < SkillPanel.TABS_PER_ROW) {
                PanelTab next = PanelTab.values()[t];
                if (next == PanelTab.SKILLS && tab == PanelTab.SKILLS) {
                    SKILLS_TAB.closeGuide();
                }
                if (tab != next) {
                    tab = next;
                    saveTab();
                }
            } else if (t == EXPAND_TAB) {
                Minecraft.getInstance().setScreen(new RuneScapeScreen(fullTab(tab)));
            } else if (t == CLOSE_TAB) {
                visible = false;
                closeCursorScreen();
            }
            return true;
        }
        double lx = mx - SkillPanel.CONTENT_X;
        double ly = my - SkillPanel.CONTENT_Y;
        if (lx < 0 || ly < 0 || lx >= SkillPanel.CONTENT_W || ly >= SkillPanel.CONTENT_H) {
            return true;
        }
        switch (tab) {
            case SKILLS -> SKILLS_TAB.click(lx, ly);
            case COMBAT -> clickCombat(lx, ly);
            case PRAYER -> clickPrayer(lx, ly);
            case MAGIC -> clickMagic(lx, ly);
            case QUESTS -> clickQuests(lx, ly);
            case LOG -> clickLog(lx, ly);
            case GEAR -> {
            }
        }
        return true;
    }

    public static boolean mouseScrolled(int screenW, int screenH, double mouseX, double mouseY, double amount) {
        if (!visible || !contains(screenW, screenH, mouseX, mouseY)) {
            return false;
        }
        int step = (int) Math.signum(amount) * 3;
        switch (tab) {
            case SKILLS -> SKILLS_TAB.scroll(amount);
            case QUESTS -> questScroll = Math.max(0, questScroll - step);
            case LOG -> logScroll = Math.max(0, logScroll - step);
            default -> {
            }
        }
        return true;
    }

    static RuneScapeScreen.Tab fullTab(PanelTab t) {
        return switch (t) {
            case COMBAT -> RuneScapeScreen.Tab.COMBAT;
            case SKILLS -> RuneScapeScreen.Tab.SKILLS;
            case QUESTS -> RuneScapeScreen.Tab.QUESTS;
            case LOG -> RuneScapeScreen.Tab.COLLECTION;
            case GEAR -> RuneScapeScreen.Tab.EQUIPMENT;
            case PRAYER -> RuneScapeScreen.Tab.PRAYER;
            case MAGIC -> RuneScapeScreen.Tab.MAGIC;
        };
    }

    private static void closeCursorScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof SidePanelScreen) {
            mc.setScreen(null);
        }
    }

    private static void send(ResourceLocation channel, int value) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(value);
        ClientPlayNetworking.send(channel, buf);
    }
}
