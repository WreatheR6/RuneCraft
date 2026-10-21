package com.runecraft.client;

import com.runecraft.combat.AttackTimer;
import com.runecraft.osrs.AttackClock;
import com.runecraft.platform.client.ClientPlayConnectionEvents;
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
 *   <li>Round 43: food added to a running weapon delay ({@code HudNet.FOOD_DELAY}) shows as its own amber segment after the
 *       weapon's ({@link com.runecraft.osrs.FoodDelayBar}) in an amber frame, instead of the bar restarting as if the player
 *       had attacked.</li>
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
    /** Food's delay added to the running weapon delay (MC), drawn after the weapon's part until the clock is ready. */
    private static int foodMc;
    /** MC a food delay packet announced: the next clock packet carries its new wait (round 43). */
    private static int foodAnnounced;

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
        ClientPlayNetworking.registerGlobalReceiver(com.runecraft.network.HudNet.FOOD_DELAY, (client, handler, buf, sender) -> {
            int added = buf.readVarInt();
            client.execute(() -> foodAnnounced += Math.max(0, added));
        });
        ClientTickEvents.END_CLIENT_TICK.register(AttackClient::tick);
        // The server forgets a buffered click on logout without a clock packet: start the next session clean, or the
        // queued dot would stay on screen and hold-to-attack would wait for an answer that never comes.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
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

    /** Back to the state of a fresh session: nothing queued, no reply awaited, the timer idle (hidden). */
    private static void reset() {
        readyAtTick = Long.MIN_VALUE / 4;
        periodMc = 48;
        kind = AttackClock.Kind.MELEE;
        queued = false;
        lastPacketTick = Long.MIN_VALUE / 4;
        lastSendTick = Long.MIN_VALUE / 4;
        awaitingReply = false;
        foodMc = 0;
        foodAnnounced = 0;
    }

    private static void onClock(int remaining, int period, int k, boolean q) {
        if (foodAnnounced > 0) {
            // Food added to the running delay: the weapon's part keeps its period and progress, the food's wait is drawn after
            // it; vanilla's indicator is not restarted (no attack was made).
            foodMc = (foodLeft() > 0 ? foodMc : 0) + foodAnnounced;
            foodAnnounced = 0;
            readyAtTick = clientTick + remaining;
            kind = k >= 0 && k < AttackClock.Kind.values().length ? AttackClock.Kind.values()[k] : AttackClock.Kind.MELEE;
            queued = q;
            lastPacketTick = clientTick;
            awaitingReply = false;
            return;
        }
        if (!q) {
            foodMc = 0; // an attack accepted: the food's wait is over (a click buffered during it keeps the food look)
        }
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

    /** MC of food delay still to run on the timer (0: the bar shows no food segment). Read by the HUD probe. */
    public static int foodLeft() {
        return foodMc <= 0 ? 0 : com.runecraft.osrs.FoodDelayBar.foodLeft(foodMc, remaining());
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
        if (foodLeft() > 0) {
            renderFood(g, x, y, left);
            return;
        }
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

    /**
     * The timer while food's delay runs (round 43): an amber frame, the weapon's part in the style colour, then the food's part
     * (dim amber still to come, amber as it runs), and an amber dot on the left; the queued dot as usual.
     */
    private static void renderFood(GuiGraphics g, int x, int y, float left) {
        com.runecraft.osrs.FoodDelayBar.Look look = com.runecraft.osrs.FoodDelayBar.look(periodMc, foodMc, left);
        int food = 0xFF000000 | com.runecraft.osrs.FoodDelayBar.FOOD_RGB;
        g.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xC0000000 | (com.runecraft.osrs.FoodDelayBar.FOOD_EMPTY_RGB & 0xFFFFFF));
        g.renderOutline(x - 1, y - 1, BAR_WIDTH + 2, BAR_HEIGHT + 2, food);
        int split = Math.round(BAR_WIDTH * look.split());
        int filled = Math.round(BAR_WIDTH * look.fill());
        g.fill(x, y, x + Math.min(filled, split), y + BAR_HEIGHT, 0xFF000000 | colour());
        g.fill(x + split, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF000000 | com.runecraft.osrs.FoodDelayBar.FOOD_EMPTY_RGB);
        if (filled > split) {
            g.fill(x + split, y, x + filled, y + BAR_HEIGHT, food);
        }
        g.fill(x - 5, y, x - 2, y + BAR_HEIGHT, food); // the food mark
        if (queued) {
            int dx = x + BAR_WIDTH + 3;
            g.fill(dx, y, dx + BAR_HEIGHT, y + BAR_HEIGHT, 0xFFFFFFFF);
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
