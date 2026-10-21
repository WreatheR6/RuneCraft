package com.runecraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.runecraft.network.Net;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.PrayerGrace;
import com.runecraft.osrs.Skill;
import com.runecraft.platform.PacketByteBufs;
import com.runecraft.platform.client.ClientPlayNetworking;
import com.runecraft.platform.client.ClientTickEvents;
import com.runecraft.platform.client.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import org.lwjgl.glfw.GLFW;

/**
 * Rebindable combat keys: the three protection prayers and the quick-prayers. // playability: OSRS players switch prayers
 * by clicking the always-open prayer book; here the only other way is the side panel behind a cursor screen, so
 * protections get keys. Each press sends an idempotent "set" ({@link Net#PRAYER_SET}) and updates the HUD at once; the
 * next sync corrects it if the server refused.
 */
public final class CombatKeys {
    /** Client ticks between two sends of one key (a held key or a double tap can't flip it twice). */
    private static final int DEBOUNCE_TICKS = 4;

    public static KeyMapping protectMagic;
    public static KeyMapping protectMissiles;
    public static KeyMapping protectMelee;
    public static KeyMapping quickPrayers;
    /** Round 42: eat the best food / drink the best potion in the pack (server: skill.QuickConsume). */
    public static KeyMapping quickEat;
    public static KeyMapping quickDrink;
    /**
     * Round 43: Sol Heredit's grapple, one key per slot in {@code SolHeredit.Grapple} order (body, back, hands, legs, feet),
     * 1-5 by default. They only act while the grapple prompt is up ({@link GrappleHud}), so they don't clash with the hotbar
     * keys the rest of the time (their conflict context, {@link #GRAPPLE_CONTEXT}).
     */
    public static KeyMapping[] grapple;

    /** Active only while a grapple prompt is on screen with no screen open; conflicts with no other context. */
    static final net.neoforged.neoforge.client.settings.IKeyConflictContext GRAPPLE_CONTEXT =
            new net.neoforged.neoforge.client.settings.IKeyConflictContext() {
                @Override
                public boolean isActive() {
                    return GrappleHud.active() && Minecraft.getInstance().screen == null;
                }

                @Override
                public boolean conflicts(net.neoforged.neoforge.client.settings.IKeyConflictContext other) {
                    return other == this;
                }
            };

    private static final long[] LAST_SENT = {-100, -100, -100, -100, -100, -100};
    private static long ticks;

    private CombatKeys() {
    }

    public static void init() {
        // Literal names (as the systems module's keys), so no language file is needed. R, G, Z and ` are unbound in vanilla
        // 1.21.1 and free in RuneCraft (K, Left Alt, J, V, B and H are taken).
        protectMagic = register("RuneCraft Protect from Magic", GLFW.GLFW_KEY_R);
        protectMissiles = register("RuneCraft Protect from Missiles", GLFW.GLFW_KEY_G);
        protectMelee = register("RuneCraft Protect from Melee", GLFW.GLFW_KEY_Z);
        quickPrayers = register("RuneCraft Quick-prayers", GLFW.GLFW_KEY_GRAVE_ACCENT);
        // Round 42 // playability: OSRS players click food and potions in the inventory, which a real-time player aiming with
        // the mouse can't; the side mouse buttons are unbound in vanilla and RuneCraft (rebind in Controls).
        quickEat = KeyBindingHelper.registerKeyBinding(new KeyMapping("RuneCraft Quick-eat", InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_4, "key.categories.runecraft"));
        quickDrink = KeyBindingHelper.registerKeyBinding(new KeyMapping("RuneCraft Quick-drink", InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_5, "key.categories.runecraft"));
        // Round 43 // playability: the grapple's five slots on keys (OSRS clicks them in the worn-equipment tab).
        com.runecraft.osrs.content.minigames.SolHeredit.Grapple[] slots = com.runecraft.osrs.content.minigames.SolHeredit.Grapple.values();
        grapple = new KeyMapping[slots.length];
        for (int i = 0; i < slots.length; i++) {
            String slot = slots[i].slot.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + slots[i].slot.substring(1);
            grapple[i] = KeyBindingHelper.registerKeyBinding(new KeyMapping("RuneCraft Grapple: defend " + slot, GRAPPLE_CONTEXT,
                    InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_1 + com.runecraft.osrs.content.minigames.SolHeredit.hotbarKey(slots[i]) - 1,
                    "key.categories.runecraft"));
        }
        ClientTickEvents.END_CLIENT_TICK.register(CombatKeys::tick);
    }

    private static KeyMapping register(String name, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping(name, InputConstants.Type.KEYSYM, key, "key.categories.runecraft"));
    }

    private static void tick(Minecraft mc) {
        ticks++;
        while (protectMagic.consumeClick()) {
            press(0);
        }
        while (protectMissiles.consumeClick()) {
            press(1);
        }
        while (protectMelee.consumeClick()) {
            press(2);
        }
        while (quickPrayers.consumeClick()) {
            press(3);
        }
        while (quickEat.consumeClick()) {
            consume(4);
        }
        while (quickDrink.consumeClick()) {
            consume(5);
        }
    }

    /** Quick-eat (4) or quick-drink (5): asks the server to use the best food / potion (debounced like the prayer keys). */
    public static void consume(int index) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null || ticks - LAST_SENT[index] < DEBOUNCE_TICKS) {
            return;
        }
        LAST_SENT[index] = ticks;
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeByte(index == 5 ? 1 : 0);
        ClientPlayNetworking.send(com.runecraft.network.HudNet.QUICK_CONSUME, buf);
    }

    /**
     * Index of the combat key bound to this key code (0-2 protections, 3 quick-prayers, 4 quick-eat, 5 quick-drink, 6-10 the
     * grapple slots while a grapple prompt is up), or -1.
     */
    public static int keyIndex(int keyCode, int scanCode) {
        if (grapple != null && GrappleHud.active()) {
            for (int i = 0; i < grapple.length; i++) {
                if (grapple[i] != null && grapple[i].matches(keyCode, scanCode)) {
                    return GRAPPLE_INDEX + i;
                }
            }
        }
        KeyMapping[] keys = {protectMagic, protectMissiles, protectMelee, quickPrayers, quickEat, quickDrink};
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null && keys[i].matches(keyCode, scanCode)) {
                return i;
            }
        }
        return -1;
    }

    /** {@link #keyIndex} of the first grapple key (body); the others follow in {@code SolHeredit.Grapple} order. */
    public static final int GRAPPLE_INDEX = 6;

    /** The bound key of grapple slot {@code index} (body 0 ... feet 4) as shown in Controls (e.g. "4"). */
    public static String grappleKeyName(int index) {
        return grapple == null || index < 0 || index >= grapple.length || grapple[index] == null ? "?"
                : grapple[index].getTranslatedKeyMessage().getString();
    }

    /** The bound key of combat key {@code index} as shown in Controls (e.g. "R"). */
    public static String keyName(int index) {
        KeyMapping[] keys = {protectMagic, protectMissiles, protectMelee, quickPrayers, quickEat, quickDrink};
        return index < 0 || index >= keys.length || keys[index] == null ? "?" : keys[index].getTranslatedKeyMessage().getString();
    }

    /** Presses combat key {@code index} (also used by the side panel's cursor screen, where key mappings don't fire). */
    public static void press(int index) {
        if (index >= GRAPPLE_INDEX) {
            GrappleHud.answer(index - GRAPPLE_INDEX);
            return;
        }
        if (index == 4 || index == 5) {
            consume(index);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || index < 0 || index > 3 || ticks - LAST_SENT[index] < DEBOUNCE_TICKS) {
            return;
        }
        LAST_SENT[index] = ticks;
        if (index == 3) {
            ClientPlayNetworking.send(Net.PRAYER_QUICK, PacketByteBufs.create());
            quickOptimistic();
            return;
        }
        Prayer p = PrayerGrace.PROTECTIONS[index];
        set(p, !ClientState.prayerActive(p));
    }

    /**
     * Sends a prayer set and shows it at once when the client can tell the server will accept it. Under the Nightmare's curse
     * ({@link ClientState#CURSE}, round 42) the server toggles the prayer the click is redirected to, so the HUD shows that
     * one switching instead of flashing the prayer clicked until the next sync.
     */
    public static void set(Prayer p, boolean on) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(p.ordinal());
        buf.writeBoolean(on);
        ClientPlayNetworking.send(Net.PRAYER_SET, buf);
        Prayer target = cursed(p);
        if (target != p) {
            boolean targetOn = !ClientState.prayerActive(target); // a redirected click always toggles its target
            if (!targetOn || likelyAllowed(target)) {
                ClientState.prayerMask = (int) PrayerGrace.switched(ClientState.prayerMask, target, targetOn);
            }
            return;
        }
        if (!on || likelyAllowed(p)) {
            ClientState.prayerMask = (int) PrayerGrace.switched(ClientState.prayerMask, p, on);
        }
    }

    /** The prayer a click on {@code p} switches under the curse map the server last sent (itself when uncursed). */
    public static Prayer cursed(Prayer p) {
        int i = PrayerGrace.index(p);
        if (i < 0) {
            return p;
        }
        int to = ClientState.CURSE[i];
        return to >= 0 && to < PrayerGrace.PROTECTIONS.length ? PrayerGrace.PROTECTIONS[to] : p;
    }

    private static boolean likelyAllowed(Prayer p) {
        return ClientState.SKILLS.level(Skill.PRAYER) >= p.level() && ClientState.prayerPoints > 0;
    }

    /** The quick-prayer switch as the server will do it: all off if any is on, else each one on. */
    private static void quickOptimistic() {
        long quick = ClientState.quickPrayerMask;
        if (quick == 0) {
            return;
        }
        long mask = ClientState.prayerMask;
        boolean anyOn = false;
        for (Prayer p : PrayerGrace.fromMask(quick)) {
            anyOn |= (mask & (1L << p.ordinal())) != 0 || (mask & (1L << cursed(p).ordinal())) != 0;
        }
        for (Prayer p : PrayerGrace.fromMask(quick)) {
            Prayer target = cursed(p); // as the server's toggleQuick: each quick prayer clicked, curse and all
            if (anyOn) {
                mask = PrayerGrace.switched(PrayerGrace.switched(mask, p, false), target, false);
            } else if (likelyAllowed(target)) {
                mask = PrayerGrace.switched(mask, target, true);
            }
        }
        ClientState.prayerMask = (int) mask;
    }
}
