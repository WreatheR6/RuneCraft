package com.runecraft.client;

import com.runecraft.RuneCraft;
import com.runecraft.entity.BossEntity;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.osrs.Experience;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.data.Boss;
import com.runecraft.osrs.data.Monster;
import com.runecraft.registry.ModEntities;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;

import java.util.Iterator;
import java.util.Locale;

/** Status orbs, XP drops and the OSRS mouseover text ("Attack Goblin (level-2)"). */
public final class HudOverlay {
    private static final long XP_DROP_MS = 1800;

    private HudOverlay() {
    }

    public static void render(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) {
            return;
        }
        TextRenderer tr = mc.textRenderer;
        int w = ctx.getScaledWindowWidth();
        boolean gielinor = mc.world != null && mc.world.getRegistryKey().getValue().equals(RuneCraft.id("gielinor"));

        // Orbs (top right)
        int x = w - 92;
        int y = 4;
        int hp = (int) Math.ceil(mc.player.getHealth());
        int maxHp = gielinor ? ClientState.SKILLS.level(Skill.HITPOINTS) : (int) mc.player.getMaxHealth();
        orb(ctx, tr, x, y, "HP", hp + "/" + maxHp, 0xFFB01818, hp / (float) Math.max(1, maxHp));
        int prayerMax = Math.max(1, ClientState.SKILLS.currentLevel(Skill.PRAYER));
        orb(ctx, tr, x, y + 14, "Pr", ClientState.prayerPoints + "/" + prayerMax, 0xFF20A0C0, ClientState.prayerPoints / (float) prayerMax);
        ctx.fill(x, y + 28, x + 88, y + 40, 0xA0201810);
        ctx.drawTextWithShadow(tr, "Cb " + ClientState.SKILLS.combatLevel() + "  " + formatCoins(ClientState.coins) + " gp", x + 3, y + 30, 0xFFFFD700);

        // XP drops (right side, rising)
        long now = System.currentTimeMillis();
        int dropY = y + 60;
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
            int tx = w - tr.getWidth(text) - 6;
            Identifier icon = RuneCraft.id("textures/gui/skill/" + d.skill().id() + ".png");
            ctx.drawTexture(icon, tx - 12, dy - 2, 0, 0, 10, 10, 10, 10);
            ctx.drawTextWithShadow(tr, text, tx, dy, 0xFFFFFFFF);
            index++;
        }

        // Mouseover text (top left), like OSRS
        if (mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living && living != mc.player) {
            String name = living.getName().getString();
            int level = combatLevel(living, gielinor);
            String verb = living instanceof com.runecraft.entity.QuestNpcEntity ? "Talk-to " : "Attack ";
            ctx.drawTextWithShadow(tr, verb, 4, 4, 0xFFFFFFFF);
            int vx = 4 + tr.getWidth(verb);
            ctx.drawTextWithShadow(tr, name, vx, 4, 0xFFFFFF00);
            if (level > 0) {
                ctx.drawTextWithShadow(tr, " (level-" + level + ")", vx + tr.getWidth(name), 4, levelColor(level));
            }
        }
        if (!ClientState.slayerTask.isEmpty() && ClientState.slayerRemaining > 0) {
            String task = "Slayer: " + ClientState.slayerRemaining + " x " + ClientState.slayerTask.toLowerCase(Locale.ROOT).replace('_', ' ');
            ctx.drawTextWithShadow(tr, task, w - tr.getWidth(task) - 4, y + 44, 0xFFC0C0C0);
        }
    }

    private static void orb(DrawContext ctx, TextRenderer tr, int x, int y, String label, String value, int color, float fill) {
        ctx.fill(x, y, x + 88, y + 12, 0xA0201810);
        ctx.fill(x + 1, y + 1, x + 1 + (int) (86 * Math.max(0, Math.min(1, fill))), y + 11, color);
        ctx.drawTextWithShadow(tr, label + " " + value, x + 3, y + 2, 0xFFFFFFFF);
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
        if (entity instanceof net.minecraft.entity.player.PlayerEntity) {
            return 0;
        }
        Monster m = Monster.byEntity(Registries.ENTITY_TYPE.getId(entity.getType()).toString());
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
