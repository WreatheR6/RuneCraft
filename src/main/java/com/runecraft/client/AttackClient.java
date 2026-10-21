package com.runecraft.client;

import com.runecraft.combat.AttackTimer;
import com.runecraft.osrs.AttackClock;
import com.runecraft.platform.client.ClientPlayNetworking;
import com.runecraft.platform.client.ClientTickEvents;
import com.runecraft.platform.client.HudRenderCallback;
import com.runecraft.world.Gielinor;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/**
 * Client side of the attack clock (round 41, {@link AttackTimer}): an OSRS attack timer under the crosshair, the vanilla
 * indicator kept in step with the server, and hold-to-attack.
 * <ul>
 *   <li>{@link AttackTimer#CLOCK} sets the local ready tick. On an accepted attack the vanilla attack-strength ticker is
 *       reset, so vanilla's indicator (filled at the OSRS speed, see ModItems / AttackTimer.UNARMED_SPEED) restarts with the
 *       server instead of on every click.</li>
 *   <li>The timer: a 20x3 bar 6 px under the crosshair in Gielinor, filling over the attack period in the style colour
 *       (melee red, ranged green, magic blue), flashing white on the ready tick, with a dot while a click is queued; hidden
 *       after 3 s idle. Gielinor hides vanilla's crosshair indicator, which this replaces.</li>
 *   <li>{@code // playability} hold-to-attack (OSRS: one click keeps attacking the NPC): with the attack key held on a
 *       monster and no screen open, the client sends the next attack {@value AttackClock#HOLD_SEND_EARLY_MC} MC before
 *       the clock is ready; the server's buffer fires it on the exact ready tick.</li>
 * </ul>
 */
public final class AttackClient {
    private static final int BAR_WIDTH = 20;
    private static final int BAR_HEIGHT = 3;
    /** Crosshair half-height (vanilla's crosshair sprite is 15 px) plus the 6 px gap. */
    private static final int BAR_OFFSET_Y = 7 + 6;
    private static final int IDLE_HIDE_TICKS = 60;
    private static final int FLASH_TICKS = 4;
    /**
     * Hold-to-attack waits for the server's answer this long before sending again (a refused click, e.g. single-way
     * combat, has no answer: its message then repeats at most twice a second while the key is held).
     */
    private static final int REPLY_WAIT_TICKS = 10;

    private static long clientTick;
    private static long readyAtTick = Long.MIN_VALUE / 4;
    private static int periodMc = 48;
    private static AttackClock.Kind kind = AttackClock.Kind.MELEE;
    private static boolean queued;
    private static long lastPacketTick = Long.MIN_VALUE / 4;
    private static long lastSendTick = Long.MIN_VALUE / 4;
    private static boolean awaitingReply;
    /** The player's attack-indicator option while the crosshair layer draws with it switched off, else null. */
    private static AttackIndicatorStatus savedIndicator;

    private AttackClient() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(AttackTimer.CLOCK, (client, handler, buf, sender) -> {
            int remaining = buf.readVarInt();
            int period = buf.readVarInt();
            int k = buf.readVarInt();
            boolean q = buf.readBoolean();
            client.execute(() -> onClock(remaining, period, k, q));
        });
        ClientTickEvents.END_CLIENT_TICK.register(AttackClient::tick);
        HudRenderCallback.EVENT.register(AttackClient::render);
        NeoForge.EVENT_BUS.addListener(RenderGuiLayerEvent.Pre.class, AttackClient::beforeLayer);
        NeoForge.EVENT_BUS.addListener(RenderGuiLayerEvent.Post.class, AttackClient::afterLayer);
        // Any attack the local player sends (a vanilla click or hold-to-attack) waits for the server's answer.
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, AttackEntityEvent.class, e -> {
            if (e.getEntity().level().isClientSide() && e.getEntity() == Minecraft.getInstance().player) {
                lastSendTick = clientTick;
                awaitingReply = true;
            }
        });
    }

    private static void onClock(int remaining, int period, int k, boolean q) {
        readyAtTick = clientTick + remaining;
        periodMc = Math.max(1, period);
        kind = k >= 0 && k < AttackClock.Kind.values().length ? AttackClock.Kind.values()[k] : AttackClock.Kind.MELEE;
        queued = q;
        lastPacketTick = clientTick;
        awaitingReply = false;
        Minecraft mc = Minecraft.getInstance();
        if (!q && period > 0 && remaining == period && mc.player != null) {
            mc.player.resetAttackStrengthTicker(); // an accepted attack: vanilla's indicator restarts with the server
        }
    }

    /** MC ticks until the server's clock is ready, as last told. */
    public static long remaining() {
        return Math.max(0, readyAtTick - clientTick);
    }

    private static boolean gielinor(Minecraft mc) {
        return mc.level != null && Gielinor.is(mc.level);
    }

    private static void tick(Minecraft mc) {
        clientTick++;
        restoreIndicator(mc);
        if (mc.player == null || mc.gameMode == null || !gielinor(mc) || mc.screen != null || mc.player.isSpectator()
                || mc.player.isUsingItem() || !mc.options.keyAttack.isDown()) {
            return;
        }
        if (!(mc.hitResult instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity target)
                || target instanceof Player || !target.isAlive()) {
            return;
        }
        if (queued || remaining() > AttackClock.HOLD_SEND_EARLY_MC || awaitingReply && clientTick - lastSendTick < REPLY_WAIT_TICKS) {
            return;
        }
        mc.gameMode.attack(mc.player, target);
        mc.player.swing(InteractionHand.MAIN_HAND);
        lastSendTick = clientTick;
        awaitingReply = true;
    }

    private static int colour() {
        return switch (kind) {
            case MELEE -> 0xE03030;
            case RANGED, THROWN -> 0x40C040;
            case SPELL -> 0x4080FF;
        };
    }

    private static void render(GuiGraphics g, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        restoreIndicator(mc);
        if (mc.player == null || mc.options.hideGui || !gielinor(mc)) {
            return;
        }
        long sinceReady = clientTick - readyAtTick;
        if (!queued && sinceReady > IDLE_HIDE_TICKS && clientTick - lastPacketTick > IDLE_HIDE_TICKS) {
            return;
        }
        int x = g.guiWidth() / 2 - BAR_WIDTH / 2;
        int y = g.guiHeight() / 2 + BAR_OFFSET_Y;
        float left = Math.max(0f, readyAtTick - clientTick - tickDelta); // smooth between client ticks
        float fill = left <= 0 ? 1f : Math.max(0f, Math.min(1f, 1f - left / periodMc));
        g.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xA0000000);
        boolean flash = sinceReady >= 0 && sinceReady < FLASH_TICKS;
        int filled = Math.round(BAR_WIDTH * fill);
        g.fill(x, y, x + filled, y + BAR_HEIGHT, 0xFF000000 | (flash ? 0xFFFFFF : colour()));
        if (queued) {
            int dx = x + BAR_WIDTH + 3;
            g.fill(dx, y, dx + BAR_HEIGHT, y + BAR_HEIGHT, 0xFFFFFFFF); // a click is buffered
        }
    }

    // ---------------- Vanilla's crosshair indicator ----------------

    private static void beforeLayer(RenderGuiLayerEvent.Pre e) {
        if (!e.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        OptionInstance<AttackIndicatorStatus> option = mc.options.attackIndicator();
        if (savedIndicator == null && gielinor(mc) && option.get() == AttackIndicatorStatus.CROSSHAIR) {
            savedIndicator = option.get();
            option.set(AttackIndicatorStatus.OFF); // the OSRS timer under the crosshair replaces it
        }
    }

    private static void afterLayer(RenderGuiLayerEvent.Post e) {
        if (e.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
            restoreIndicator(Minecraft.getInstance());
        }
    }

    /** Puts the player's indicator option back (also every frame and tick, should a cancelled layer skip the Post event). */
    private static void restoreIndicator(Minecraft mc) {
        if (savedIndicator != null) {
            AttackIndicatorStatus s = savedIndicator;
            savedIndicator = null;
            mc.options.attackIndicator().set(s);
        }
    }
}
