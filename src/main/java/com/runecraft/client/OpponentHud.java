package com.runecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.platform.client.WorldRenderContext;
import com.runecraft.world.Gielinor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The opponent info box (round 42, {@code // playability}): the monster the player is fighting, with its name, combat level
 * and exact hitpoints, top left under the mouseover text as in OSRS; a subtle outline round that monster in the world; and
 * the "Auto-retaliating: &lt;target&gt;" hint.
 * <ul>
 *   <li>The opponent is the creature the player last hit (a hitsplat naming the player as attacker, or the player's own
 *       attack click), or the one that hit the player while no fight was going on (hitsplats name their attacker).</li>
 *   <li>Hitpoints are OSRS hitpoints: each hitsplat carries the target's exact value; between hits the creature's live
 *       health ratio is scaled to that maximum.</li>
 *   <li>The box hides 10 s after the last hit either way, or 1.5 s after the opponent dies.</li>
 * </ul>
 */
public final class OpponentHud {
    private static final long SHOWN_MS = 10_000;
    private static final long DEAD_MS = 1_500;
    private static final int WIDTH = 124;

    private static int opponent = -1;
    private static long lastCombatMs;
    private static long diedAtMs;
    /** Exact OSRS hitpoints of the opponent from its last hitsplat (0 / 0 for none yet). */
    private static int splatHp;
    private static int splatMax;
    private static long splatMs;
    /** After a hit its splat's exact hitpoints are shown this long; the creature's synced health takes over after. */
    private static final long SPLAT_TRUST_MS = 1500;

    private OpponentHud() {
    }

    static void reset() {
        opponent = -1;
        splatHp = 0;
        splatMax = 0;
        splatMs = 0;
        diedAtMs = 0;
    }

    /** A hitsplat arrived: the player's own hit picks the opponent; a hit on the player does when no fight is going on. */
    static void onSplat(int target, ClientState.Hitsplat splat) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        int self = mc.player.getId();
        long now = System.currentTimeMillis();
        if (splat.attacker() == self && target != self) {
            choose(target, now);
        } else if (target == self && splat.attacker() >= 0 && splat.attacker() != self) {
            if (opponent < 0 || now - lastCombatMs > SHOWN_MS / 2 || current(mc) == null) {
                choose(splat.attacker(), now);
            } else if (splat.attacker() == opponent) {
                lastCombatMs = now;
            }
        }
        if (target == opponent && splat.maxHp() > 0) {
            splatHp = splat.hp();
            splatMax = splat.maxHp();
            splatMs = now;
            lastCombatMs = now;
        }
    }

    /** The player attacked a creature (a click or hold-to-attack): it's the opponent now. */
    static void onAttack(Entity target) {
        if (target instanceof LivingEntity && !(target instanceof Player)) {
            choose(target.getId(), System.currentTimeMillis());
        }
    }

    private static void choose(int id, long now) {
        if (id != opponent) {
            opponent = id;
            splatHp = 0;
            splatMax = 0;
            splatMs = 0;
            diedAtMs = 0;
        }
        lastCombatMs = now;
    }

    /** The opponent while its box is shown, else null. */
    static LivingEntity current(Minecraft mc) {
        if (opponent < 0 || mc.level == null || !Gielinor.is(mc.level)) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (now - lastCombatMs > SHOWN_MS || !(mc.level.getEntity(opponent) instanceof LivingEntity e) || e.isRemoved()) {
            reset();
            return null;
        }
        if (!e.isAlive()) {
            if (diedAtMs == 0) {
                diedAtMs = now;
            } else if (now - diedAtMs > DEAD_MS) {
                reset();
                return null;
            }
        }
        return e;
    }

    /** The opponent's OSRS hitpoints now and at most. */
    static int[] hitpoints(LivingEntity e) {
        int max = splatMax > 0 ? splatMax : e instanceof DataBossEntity boss ? Math.max(1, boss.trueHitpoints())
                : Math.max(1, Math.round(e.getMaxHealth()));
        float ratio = e.isAlive() ? Mth.clamp(e.getHealth() / Math.max(1f, e.getMaxHealth()), 0f, 1f) : 0f;
        int hp = ratio <= 0 ? 0 : Math.max(1, (int) Math.ceil(ratio * max - 1e-3));
        if (splatMax > 0 && (System.currentTimeMillis() - splatMs < SPLAT_TRUST_MS || Math.abs(hp - splatHp) <= Math.max(1, max / 100))) {
            // The exact value of the last hit: right after it (the creature's synced health arrives with the server's next
            // entity update) and while the synced health still agrees with it.
            hp = splatHp;
        }
        return new int[]{Math.min(hp, max), max};
    }

    /** The creature auto-retaliate is fighting, while it's alive and loaded. */
    static LivingEntity retaliating(Minecraft mc) {
        if (!ClientState.autoRetaliate || ClientState.retaliateTarget < 0 || mc.level == null) {
            return null;
        }
        return mc.level.getEntity(ClientState.retaliateTarget) instanceof LivingEntity e && e.isAlive() ? e : null;
    }

    static int opponentId() {
        return opponent;
    }

    public static void render(GuiGraphics g, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        Font tr = mc.font;
        int x = 4;
        int y = 16; // under the mouseover text ("Attack Goblin (level-2)")
        LivingEntity e = current(mc);
        if (e != null) {
            box(g, tr, e, x, y);
            y += 30;
        }
        LivingEntity target = retaliating(mc);
        if (target != null) {
            String hint = "Auto-retaliating: ";
            g.fill(x, y, x + tr.width(hint + target.getName().getString()) + 6, y + 11, 0x90201810);
            g.drawString(tr, hint, x + 3, y + 2, 0xFFC0C0C0, false);
            g.drawString(tr, target.getName().getString(), x + 3 + tr.width(hint), y + 2, 0xFFFFFF00, false);
        }
    }

    /** OSRS's opponent box: the name and level, then the green-on-red hitpoints bar with the exact numbers. */
    private static void box(GuiGraphics g, Font tr, LivingEntity e, int x, int y) {
        g.fill(x, y, x + WIDTH, y + 27, 0xB0201810);
        g.renderOutline(x, y, WIDTH, 27, 0xFF5A4A30);
        String name = e.getName().getString();
        int level = HudOverlay.combatLevel(e, true);
        String lv = level > 0 ? " (level-" + level + ")" : "";
        int room = WIDTH - 6 - tr.width(lv);
        if (tr.width(name) > room) {
            name = tr.plainSubstrByWidth(name, Math.max(0, room - 6)) + "..";
        }
        g.drawString(tr, name, x + 3, y + 3, 0xFFFFFFFF, false);
        if (!lv.isEmpty()) {
            g.drawString(tr, lv, x + 3 + tr.width(name), y + 3, HudOverlay.levelColor(level), false);
        }
        int[] hp = hitpoints(e);
        int barX = x + 3;
        int barY = y + 14;
        int barW = WIDTH - 6;
        g.fill(barX, barY, barX + barW, barY + 10, 0xFFB01010);
        g.fill(barX, barY, barX + Math.round(barW * hp[0] / (float) hp[1]), barY + 10, 0xFF10A010);
        String text = hp[0] + " / " + hp[1];
        g.drawString(tr, text, barX + barW / 2 - tr.width(text) / 2, barY + 1, 0xFFFFFFFF, true);
    }

    /** A thin, pale outline round the opponent's hitbox, so the creature being fought stands out in a crowd. */
    public static void renderWorld(WorldRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        LivingEntity e = current(mc);
        PoseStack pose = context.matrixStack();
        MultiBufferSource consumers = context.consumers();
        if (e == null || !e.isAlive() || pose == null || consumers == null || e.isInvisible()) {
            return;
        }
        Camera camera = context.camera();
        Vec3 cam = camera.getPosition();
        float delta = context.tickDelta();
        double x = Mth.lerp(delta, e.xo, e.getX());
        double y = Mth.lerp(delta, e.yo, e.getY());
        double z = Mth.lerp(delta, e.zo, e.getZ());
        AABB box = e.getBoundingBox().move(x - e.getX(), y - e.getY(), z - e.getZ()).inflate(0.05).move(-cam.x, -cam.y, -cam.z);
        LevelRenderer.renderLineBox(pose, consumers.getBuffer(RenderType.lines()), box, 0.85f, 0.95f, 1.0f, 0.45f);
        if (consumers instanceof MultiBufferSource.BufferSource immediate) {
            immediate.endBatch(RenderType.lines());
        }
    }
}
