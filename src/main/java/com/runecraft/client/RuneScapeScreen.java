package com.runecraft.client;

import com.runecraft.RuneCraft;
import com.runecraft.combat.Equipment;
import com.runecraft.network.Net;
import com.runecraft.osrs.CollectionLogData;
import com.runecraft.osrs.CombatStyle;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.Experience;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Quest;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.Spell;
import com.runecraft.util.Inv;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The OSRS side-panel, as one screen with tabs. */
public class RuneScapeScreen extends Screen {
    public enum Tab { COMBAT("Combat"), SKILLS("Skills"), QUESTS("Quests"), PRAYER("Prayer"), MAGIC("Magic"),
        EQUIPMENT("Gear"), COLLECTION("Log");
        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private static final int PANEL_W = 300;
    private static final int PANEL_H = 230;
    private Tab tab;
    private int page;
    private int left;
    private int top;
    private Quest selectedQuest;

    public RuneScapeScreen(Tab tab) {
        super(Text.literal("RuneScape"));
        this.tab = tab;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2;
        int tw = PANEL_W / Tab.values().length;
        for (Tab t : Tab.values()) {
            addDrawableChild(ButtonWidget.builder(Text.literal(t.label), b -> switchTab(t))
                    .dimensions(left + t.ordinal() * tw, top - 20, tw, 18).build());
        }
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
        tab = t;
        page = 0;
        clearAndInit();
    }

    private static void send(Identifier channel, int value) {
        PacketByteBuf buf = PacketByteBufs.create();
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
            addDrawableChild(ButtonWidget.builder(Text.literal((ClientState.style == s ? "> " : "") + s.displayName()), b -> {
                send(Net.SET_STYLE, s.ordinal());
                ClientState.style = s;
                clearAndInit();
            }).dimensions(left + 10, top + 40 + i * 24, 135, 20).tooltip(Tooltip.of(Text.literal(meleeInfo[i]))).build());
        }
        for (int i = 0; i < ranged.length; i++) {
            CombatStyle s = ranged[i];
            addDrawableChild(ButtonWidget.builder(Text.literal((ClientState.style == s ? "> " : "") + "Bow: " + s.displayName()), b -> {
                send(Net.SET_STYLE, s.ordinal());
                ClientState.style = s;
                clearAndInit();
            }).dimensions(left + 155, top + 40 + i * 24, 135, 20).tooltip(Tooltip.of(Text.literal(rangedInfo[i]))).build());
        }
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
            ButtonWidget btn = ButtonWidget.builder(Text.literal(label), b -> send(Net.TOGGLE_PRAYER, p.ordinal()))
                    .dimensions(left + 5 + (i % cols) * (bw + 3), top + 22 + (i / cols) * 22, bw, 20)
                    .tooltip(Tooltip.of(Text.literal(p.displayName() + "\nLevel " + p.level() + "\nDrain: " + p.drainEffect())))
                    .build();
            btn.active = unlocked;
            addDrawableChild(btn);
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
            ButtonWidget btn = ButtonWidget.builder(Text.literal(label), b -> {
                send(Net.CAST_SPELL, s.ordinal());
                if (s.kind() != Spell.Kind.COMBAT) {
                    close();
                } else {
                    ClientState.autocast = s;
                    clearAndInit();
                }
            }).dimensions(left + 5 + (i % cols) * (bw + 3), top + 6 + (i / cols) * 20, bw, 18)
                    .tooltip(Tooltip.of(Text.literal(desc))).build();
            btn.active = unlocked || s == Spell.HOME_TELEPORT;
            addDrawableChild(btn);
        }
    }

    // ---------------- Quests ----------------

    private void initQuests() {
        Quest[] all = Quest.values();
        for (int i = 0; i < all.length; i++) {
            Quest q = all[i];
            int stage = ClientState.QUEST_STAGES[q.ordinal()];
            String prefix = stage == 0 ? "§c" : stage >= q.finalStage() ? "§a" : "§e";
            addDrawableChild(ButtonWidget.builder(Text.literal(prefix + q.displayName()), b -> {
                selectedQuest = q;
            }).dimensions(left + 5, top + 6 + i * 16, 130, 15).build());
        }
    }

    // ---------------- Collection log ----------------

    private void initCollection() {
        int pages = CollectionLogData.pages().size();
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
            page = (page + pages - 1) % pages;
        }).dimensions(left + 5, top + PANEL_H - 24, 20, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
            page = (page + 1) % pages;
        }).dimensions(left + PANEL_W - 25, top + PANEL_H - 24, 20, 18).build());
    }

    // ---------------- Rendering ----------------

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        ctx.fill(left - 2, top - 2, left + PANEL_W + 2, top + PANEL_H + 2, 0xFF1A140C);
        ctx.fill(left, top, left + PANEL_W, top + PANEL_H, 0xF03E3529);
        switch (tab) {
            case SKILLS -> renderSkills(ctx, mouseX, mouseY);
            case COMBAT -> renderCombat(ctx);
            case PRAYER -> ctx.drawTextWithShadow(textRenderer, "Prayer points: " + ClientState.prayerPoints + " / "
                    + ClientState.SKILLS.currentLevel(Skill.PRAYER), left + 6, top + 8, 0xFF40E0FF);
            case MAGIC -> {
            }
            case QUESTS -> renderQuests(ctx);
            case EQUIPMENT -> renderEquipment(ctx);
            case COLLECTION -> renderCollection(ctx, mouseX, mouseY);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderSkills(DrawContext ctx, int mouseX, int mouseY) {
        SkillSet s = ClientState.SKILLS;
        int cw = 96;
        int ch = 24;
        Skill hovered = null;
        for (Skill skill : Skill.values()) {
            int i = skill.ordinal();
            int x = left + 6 + (i % 3) * cw;
            int y = top + 6 + (i / 3) * ch;
            ctx.fill(x, y, x + cw - 4, y + ch - 3, 0xFF2A231A);
            ctx.drawTexture(RuneCraft.id("textures/gui/skill/" + skill.id() + ".png"), x + 3, y + 2, 0, 0, 16, 16, 16, 16);
            int cur = s.currentLevel(skill);
            int real = s.level(skill);
            int color = cur > real ? 0xFF00FF00 : cur < real ? 0xFFFF4040 : 0xFFFFFF00;
            ctx.drawTextWithShadow(textRenderer, cur + "/" + real, x + 24, y + 6, color);
            if (mouseX >= x && mouseX < x + cw - 4 && mouseY >= y && mouseY < y + ch - 3) {
                hovered = skill;
            }
        }
        ctx.drawTextWithShadow(textRenderer, "Total level: " + s.totalLevel() + "   Combat: " + s.combatLevel()
                + "   QP: " + ClientState.questPoints, left + 6, top + PANEL_H - 12, 0xFFFFD700);
        if (hovered != null) {
            int xp = s.xp(hovered);
            int level = s.virtualLevel(hovered);
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal(hovered.displayName() + " XP: " + Experience.format(s.xpTenths(hovered))));
            if (level < Experience.MAX_VIRTUAL_LEVEL) {
                int next = Experience.xpForLevel(level + 1);
                lines.add(Text.literal("Next level at: " + String.format(Locale.ROOT, "%,d", next)));
                lines.add(Text.literal("Remaining XP: " + String.format(Locale.ROOT, "%,d", next - xp)));
            }
            ctx.drawTooltip(textRenderer, lines, mouseX, mouseY);
        }
    }

    private void renderCombat(DrawContext ctx) {
        ItemStack held = client.player.getMainHandStack();
        String weapon = held.isEmpty() ? "Unarmed" : held.getName().getString();
        ctx.drawTextWithShadow(textRenderer, weapon + "   Combat level: " + ClientState.SKILLS.combatLevel(), left + 8, top + 10, 0xFFFFD700);
        ctx.drawTextWithShadow(textRenderer, "Melee styles", left + 10, top + 28, 0xFFE0C080);
        ctx.drawTextWithShadow(textRenderer, "Ranged styles", left + 155, top + 28, 0xFFE0C080);
        ctx.drawTextWithShadow(textRenderer, "Autocast: " + (ClientState.autocast == null ? "none" : ClientState.autocast.displayName()),
                left + 10, top + 150, 0xFF80A0FF);
        ctx.drawTextWithShadow(textRenderer, "Attack speed: " + Equipment.weaponSpeed(client.player, ClientState.SKILLS) + " ticks",
                left + 10, top + 164, 0xFFC0C0C0);
    }

    private void renderQuests(DrawContext ctx) {
        ctx.drawTextWithShadow(textRenderer, "Quest points: " + ClientState.questPoints + "/" + Quest.totalQuestPoints(),
                left + 140, top + 8, 0xFFFFD700);
        if (selectedQuest != null) {
            int stage = ClientState.QUEST_STAGES[selectedQuest.ordinal()];
            ctx.drawTextWithShadow(textRenderer, selectedQuest.displayName(), left + 140, top + 26, 0xFFFF9800);
            List<net.minecraft.text.OrderedText> wrapped = textRenderer.wrapLines(Text.literal(selectedQuest.journalFor(stage)), 150);
            int y = top + 42;
            for (net.minecraft.text.OrderedText line : wrapped) {
                ctx.drawTextWithShadow(textRenderer, line, left + 140, y, 0xFFE0E0E0);
                y += 10;
            }
            y += 6;
            ctx.drawTextWithShadow(textRenderer, "Rewards: " + selectedQuest.questPoints() + " QP", left + 140, y, 0xFFA0FFA0);
            for (var e : selectedQuest.xpRewards().entrySet()) {
                y += 10;
                ctx.drawTextWithShadow(textRenderer, String.format(Locale.ROOT, "%,.0f %s XP", e.getValue(), e.getKey().displayName()),
                        left + 140, y, 0xFFA0FFA0);
            }
        } else {
            ctx.drawTextWithShadow(textRenderer, "Select a quest.", left + 140, top + 26, 0xFFC0C0C0);
        }
    }

    private void renderEquipment(DrawContext ctx) {
        EquipmentStats s = Equipment.total(client.player, ClientState.SKILLS);
        String[] lines = {
                "Attack bonus", "Stab: " + s.stabAttack(), "Slash: " + s.slashAttack(), "Crush: " + s.crushAttack(),
                "Magic: " + s.magicAttack(), "Range: " + s.rangedAttack(), "",
                "Other bonuses", "Melee strength: " + s.meleeStrength(), "Ranged strength: " + s.rangedStrength(),
                "Magic damage: " + s.magicDamagePercent() + "%", "Prayer: " + s.prayer()};
        String[] def = {"Defence bonus", "Stab: " + s.stabDefence(), "Slash: " + s.slashDefence(), "Crush: " + s.crushDefence(),
                "Magic: " + s.magicDefence(), "Range: " + s.rangedDefence()};
        for (int i = 0; i < lines.length; i++) {
            ctx.drawTextWithShadow(textRenderer, lines[i], left + 10, top + 10 + i * 12, i == 0 || i == 7 ? 0xFFFF9800 : 0xFFE0E0E0);
        }
        for (int i = 0; i < def.length; i++) {
            ctx.drawTextWithShadow(textRenderer, def[i], left + 160, top + 10 + i * 12, i == 0 ? 0xFFFF9800 : 0xFFE0E0E0);
        }
    }

    private void renderCollection(DrawContext ctx, int mouseX, int mouseY) {
        List<CollectionLogData.Page> pages = CollectionLogData.pages();
        CollectionLogData.Page p = pages.get(Math.max(0, Math.min(page, pages.size() - 1)));
        long obtained = p.items().stream().filter(ClientState.COLLECTION_LOG::contains).count();
        ctx.drawTextWithShadow(textRenderer, p.category() + " - " + p.name(), left + 8, top + 8, 0xFFFF9800);
        ctx.drawTextWithShadow(textRenderer, "Obtained: " + obtained + "/" + p.items().size() + "   Total: "
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
            ctx.drawItem(stack, x + 7, y + 7);
            if (!has) {
                ctx.fill(x, y, x + 30, y + 30, 0x90000000);
            }
            if (mouseX >= x && mouseX < x + 30 && mouseY >= y && mouseY < y + 30) {
                hovered = item;
            }
        }
        ctx.drawCenteredTextWithShadow(textRenderer, (page + 1) + " / " + pages.size(), left + PANEL_W / 2, top + PANEL_H - 19, 0xFFC0C0C0);
        if (hovered != null) {
            ItemDef def = ItemCatalog.get(hovered);
            ctx.drawTooltip(textRenderer, Text.literal(def == null ? hovered : def.name()), mouseX, mouseY);
        }
    }
}
