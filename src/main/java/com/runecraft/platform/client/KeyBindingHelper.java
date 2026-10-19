package com.runecraft.platform.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Fabric's key binding helper. Mappings are kept here and handed to NeoForge's {@code RegisterKeyMappingsEvent},
 * which fires after the client init that creates them (see {@link ClientPlatformEvents}).
 */
public final class KeyBindingHelper {
    private KeyBindingHelper() {
    }

    static final List<KeyMapping> PENDING = new CopyOnWriteArrayList<>();

    public static KeyMapping registerKeyBinding(KeyMapping key) {
        PENDING.add(key);
        return key;
    }

    public static InputConstants.Key getBoundKeyOf(KeyMapping key) {
        return key.getKey();
    }
}
