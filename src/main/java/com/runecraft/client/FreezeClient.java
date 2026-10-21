package com.runecraft.client;

import com.runecraft.RuneCraft;
import com.runecraft.platform.client.HudRenderCallback;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.Input;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * OSRS freezes on the client. Slowness with amplifier 6 or more in Gielinor is a freeze (combat.FreezeRules): the player
 * can't walk or jump at all and the view doesn't zoom. // playability: vanilla Slowness halves the FOV and still lets the
 * player hop out of a freeze. A HUD icon shows the seconds left.
 */
public final class FreezeClient {
    private static final int FREEZE_AMPLIFIER = 6;

    private FreezeClient() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(ComputeFovModifierEvent.class, FreezeClient::onFov);
        NeoForge.EVENT_BUS.addListener(MovementInputUpdateEvent.class, FreezeClient::onInput);
        HudRenderCallback.EVENT.register(FreezeClient::render);
    }

    /** The freeze on a player, or null (only in Gielinor). */
    public static MobEffectInstance freeze(Player player) {
        if (player == null || !player.level().dimension().location().equals(RuneCraft.id("gielinor"))) {
            return null;
        }
        MobEffectInstance slow = player.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        return slow != null && slow.getAmplifier() >= FREEZE_AMPLIFIER ? slow : null;
    }

    public static boolean frozen(Player player) {
        return freeze(player) != null;
    }

    /** Takes the speed term out of the FOV while frozen (vanilla zooms in as speed drops to zero). */
    private static void onFov(ComputeFovModifierEvent e) {
        Player p = e.getPlayer();
        if (!frozen(p)) {
            return;
        }
        float walk = p.getAbilities().getWalkingSpeed();
        if (walk == 0f) {
            return;
        }
        float term = ((float) p.getAttributeValue(Attributes.MOVEMENT_SPEED) / walk + 1f) / 2f;
        if (term > 0.01f && term < 1f) {
            e.setNewFovModifier(e.getNewFovModifier() / term);
        }
    }

    /** No walking, strafing or jumping while frozen (sneaking still works). */
    private static void onInput(MovementInputUpdateEvent e) {
        if (!frozen(e.getEntity())) {
            return;
        }
        Input in = e.getInput();
        in.forwardImpulse = 0f;
        in.leftImpulse = 0f;
        in.up = false;
        in.down = false;
        in.left = false;
        in.right = false;
        in.jumping = false;
    }

    private static void render(GuiGraphics ctx, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        MobEffectInstance f = freeze(mc.player);
        if (f == null) {
            return;
        }
        String text = f.isInfiniteDuration() ? "Frozen"
                : String.format(Locale.ROOT, "Frozen %.1fs", Math.max(0, f.getDuration()) / 20.0);
        int w = mc.font.width(text) + 8;
        int x = ctx.guiWidth() / 2 - w / 2;
        int y = ctx.guiHeight() / 2 - 30;
        ctx.fill(x, y, x + w, y + 12, 0xB0102040);
        ctx.renderOutline(x, y, w, 12, 0xFF80C0FF);
        ctx.drawString(mc.font, text, x + 4, y + 2, 0xFFC0E8FF);
    }
}
