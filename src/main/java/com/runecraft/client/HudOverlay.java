package com.runecraft.client;

import com.runecraft.RuneCraft;
import com.runecraft.entity.BossEntity;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.Experience;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.data.Boss;
import com.runecraft.osrs.data.Monster;
import com.runecraft.registry.ModEntities;
import java.util.Iterator;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

/** Status orbs, XP drops, the OSRS mouseover text ("Attack Goblin (level-2)") and the side panel. */
public final class HudOverlay {
    private static final long XP_DROP_MS = 1800;

    private HudOverlay() {
    }

    public static void render(GuiGraphics ctx, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        Font tr = mc.font;
        int w = ctx.guiWidth();
        boolean gielinor = mc.level != null && mc.level.dimension().location().equals(RuneCraft.id("gielinor"));

        // Orbs (top right)
        int x = w - 92;
        int y = 4;
        // Boosted hitpoints (Saradomin brews) are held as absorption.
        int hp = (int) Math.ceil(mc.player.getHealth() + mc.player.getAbsorptionAmount());
        int maxHp = gielinor ? ClientState.SKILLS.level(Skill.HITPOINTS) : (int) mc.player.getMaxHealth();
        orb(ctx, tr, x, y, "HP", hp + "/" + maxHp, hp > maxHp ? 0xFFD0A020 : 0xFFB01818, hp / (float) Math.max(1, maxHp));
        int prayerMax = Math.max(1, ClientState.SKILLS.currentLevel(Skill.PRAYER));
        orb(ctx, tr, x, y + 14, "Pr", ClientState.prayerPoints + "/" + prayerMax, 0xFF20A0C0, ClientState.prayerPoints / (float) prayerMax);
        ctx.fill(x, y + 28, x + 88, y + 40, 0xA0201810);
        ctx.drawString(tr, "Cb " + ClientState.SKILLS.combatLevel() + "  " + formatCoins(ClientState.coins) + " gp", x + 3, y + 30, 0xFFFFD700);
        // The tokkul pouch (OSRS's one tokkul stack), once the player has any.
        int below = y + 44;
        if (ClientState.tokkul > 0) {
            ctx.fill(x, y + 40, x + 88, y + 52, 0xA0201810);
            ctx.drawString(tr, formatCoins(ClientState.tokkul) + " tokkul", x + 3, y + 42, 0xFFE08040);
            below += 12;
        }
        // The active overhead and prayers (CombatFeedbackHud draws the row; round 41).
        if (CombatFeedbackHud.prayerRowShown()) {
            CombatFeedbackHud.prayerRowX = x;
            CombatFeedbackHud.prayerRowY = below - 4;
            below += 12;
        } else {
            CombatFeedbackHud.prayerRowX = -1;
        }
        // Multicombat area: OSRS shows crossed swords.
        if (gielinor && ClientState.inMulti) {
            ctx.fill(x - 22, y, x - 2, y + 20, 0xA0201810);
            OsrsUi.icon(ctx, OsrsUi.tabIcon("combat"), x - 21, y + 1, 18);
        }
        // PvP skull: the server puts skulled players on a scoreboard team (SkullService).
        net.minecraft.world.scores.Team skullTeam = mc.player.getTeam();
        if (gielinor && skullTeam != null && com.runecraft.osrs.PvpSkull.TEAM.equals(skullTeam.getName())) {
            ctx.fill(x - 44, y, x - 24, y + 20, 0xA0201810);
            ctx.drawCenteredString(tr, "☠", x - 34, y + 6, 0xFFE8E8E8);
        } else if (gielinor && skullTeam != null && com.runecraft.osrs.PvpSkull.CHAMPION_TEAM.equals(skullTeam.getName())) {
            // The TzHaar Fight Pit champion's red skull.
            ctx.fill(x - 44, y, x - 24, y + 20, 0xA0201810);
            ctx.drawCenteredString(tr, "☠", x - 34, y + 6, 0xFFD02020);
        }

        // XP drops (right side, rising)
        long now = System.currentTimeMillis();
        int dropY = Math.max(y + 60, below + 16);
        Iterator<ClientState.XpDrop> it = ClientState.XP_DROPS.iterator();
        int index = 0;
        while (it.hasNext()) {
            ClientState.XpDrop d = it.next();
            long age = now - d.time();
            if (age > XP_DROP_MS) {
                it.remove();
                continue;
            }
            int rise = (int) (age * 40 / XP_DROP_MS);
            String text = "+" + Experience.format(d.tenths());
            int dy = dropY + index * 12 + 40 - rise;
            int tx = w - tr.width(text) - 6;
            ResourceLocation icon = OsrsUi.skillIcon(d.skill());
            ctx.blit(icon, tx - 12, dy - 1, 10, 10, 0, 0, 32, 32, 32, 32);
            ctx.drawString(tr, text, tx, dy, 0xFFFFFFFF);
            index++;
        }

        // Mouseover text (top left), like OSRS
        if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living && living != mc.player) {
            String name = living.getName().getString();
            int level = combatLevel(living, gielinor);
            String verb = living instanceof com.runecraft.entity.QuestNpcEntity ? "Talk-to " : "Attack ";
            ctx.drawString(tr, verb, 4, 4, 0xFFFFFFFF);
            int vx = 4 + tr.width(verb);
            ctx.drawString(tr, name, vx, 4, 0xFFFFFF00);
            if (level > 0) {
                ctx.drawString(tr, " (level-" + level + ")", vx + tr.width(name), 4, levelColor(level));
            }
        }
        if (!ClientState.slayerTask.isEmpty() && ClientState.slayerRemaining > 0) {
            String task = "Slayer: " + ClientState.slayerRemaining + " x " + ClientState.slayerTask.toLowerCase(Locale.ROOT).replace('_', ' ');
            ctx.drawString(tr, task, w - tr.width(task) - 4, below, 0xFFC0C0C0);
        }

        // OSRS side panel (bottom right). While the cursor is free SidePanelScreen draws it instead,
        // with hover tooltips.
        if (SidePanel.visible && !(mc.screen instanceof SidePanelScreen)) {
            SidePanel.render(ctx, w, ctx.guiHeight(), Double.NaN, Double.NaN);
        }
    }

    private static void orb(GuiGraphics ctx, Font tr, int x, int y, String label, String value, int color, float fill) {
        ctx.fill(x, y, x + 88, y + 12, 0xA0201810);
        ctx.fill(x + 1, y + 1, x + 1 + (int) (86 * Math.max(0, Math.min(1, fill))), y + 11, color);
        ctx.drawString(tr, label + " " + value, x + 3, y + 2, 0xFFFFFFFF);
    }

    private static String formatCoins(long coins) {
        if (coins >= 10_000_000) {
            return (coins / 1_000_000) + "M";
        }
        if (coins >= 100_000) {
            return (coins / 1000) + "K";
        }
        return String.format(Locale.ROOT, "%,d", coins);
    }

    public static int combatLevel(Entity entity, boolean gielinor) {
        if (entity instanceof BossEntity boss) {
            Boss b = ModEntities.boss(boss.getType());
            return b == null ? 0 : b.combatLevel();
        }
        if (entity instanceof OsrsMonsterEntity monster) {
            return monster.stats() == null ? 0 : monster.stats().combatLevel();
        }
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            return 0;
        }
        Monster m = Monster.byEntity(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        return gielinor && m != null ? m.combatLevel() : 0;
    }

    /** OSRS level colours: green if weaker than you, yellow if equal, red if stronger. */
    public static int levelColor(int level) {
        int diff = level - ClientState.SKILLS.combatLevel();
        if (diff > 9) return 0xFFFF0000;
        if (diff > 6) return 0xFFFF3000;
        if (diff > 3) return 0xFFFF7000;
        if (diff > 0) return 0xFFFFB000;
        if (diff == 0) return 0xFFFFFF00;
        if (diff > -4) return 0xFFC0FF00;
        if (diff > -7) return 0xFF80FF00;
        if (diff > -10) return 0xFF40FF00;
        return 0xFF00FF00;
    }
}
