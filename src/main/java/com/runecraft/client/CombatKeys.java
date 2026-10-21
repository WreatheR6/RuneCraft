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

    private static final long[] LAST_SENT = {-100, -100, -100, -100};
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
    }

    /** Index of the combat key bound to this key code (0-2 protections, 3 quick-prayers), or -1. */
    public static int keyIndex(int keyCode, int scanCode) {
        KeyMapping[] keys = {protectMagic, protectMissiles, protectMelee, quickPrayers};
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null && keys[i].matches(keyCode, scanCode)) {
                return i;
            }
        }
        return -1;
    }

    /** The bound key of combat key {@code index} as shown in Controls (e.g. "R"). */
    public static String keyName(int index) {
        KeyMapping[] keys = {protectMagic, protectMissiles, protectMelee, quickPrayers};
        return index < 0 || index >= keys.length || keys[index] == null ? "?" : keys[index].getTranslatedKeyMessage().getString();
    }

    /** Presses combat key {@code index} (also used by the side panel's cursor screen, where key mappings don't fire). */
    public static void press(int index) {
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

    /** Sends a prayer set and shows it at once when the client can tell the server will accept it. */
    public static void set(Prayer p, boolean on) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(p.ordinal());
        buf.writeBoolean(on);
        ClientPlayNetworking.send(Net.PRAYER_SET, buf);
        if (!on || likelyAllowed(p)) {
            ClientState.prayerMask = (int) PrayerGrace.switched(ClientState.prayerMask, p, on);
        }
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
        boolean anyOn = (mask & quick) != 0;
        for (Prayer p : PrayerGrace.fromMask(quick)) {
            if (anyOn) {
                mask = PrayerGrace.switched(mask, p, false);
            } else if (likelyAllowed(p)) {
                mask = PrayerGrace.switched(mask, p, true);
            }
        }
        ClientState.prayerMask = (int) mask;
    }
}
