package com.runecraft.client.content.systems;

import com.runecraft.client.content.ClientModuleHooks;
import com.runecraft.content.systems.SysNet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Client side of the systems module: keys (special attack, spellbook, adventurer's log), HUD and screens. */
public final class SystemsClientHooks implements ClientModuleHooks {
    public static KeyBinding specKey;
    public static KeyBinding spellbookKey;
    public static KeyBinding hubKey;

    @Override
    public void initClient() {
        specKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("RuneCraft Special Attack", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V, "key.categories.runecraft"));
        spellbookKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("RuneCraft Spellbook", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B, "key.categories.runecraft"));
        hubKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("RuneCraft Adventurer's Log", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J, "key.categories.runecraft"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (specKey.wasPressed()) {
                sendKey(SysNet.KEY_SPEC);
            }
            while (spellbookKey.wasPressed()) {
                sendKey(SysNet.KEY_SPELLBOOK);
            }
            while (hubKey.wasPressed()) {
                sendKey(SysNet.KEY_HUB);
            }
        });
        HudRenderCallback.EVENT.register(SystemsHud::render);

        ClientPlayNetworking.registerGlobalReceiver(SysNet.STATE, (client, handler, buf, sender) -> {
            int spec = buf.readVarInt();
            boolean armed = buf.readBoolean();
            int run = buf.readVarInt();
            boolean stamina = buf.readBoolean();
            String book = buf.readString();
            String autocast = buf.readString();
            boolean veng = buf.readBoolean();
            int thrall = buf.readVarInt();
            int specCost = buf.readVarInt();
            client.execute(() -> {
                SysClientState.spec = spec;
                SysClientState.specArmed = armed;
                SysClientState.run = run;
                SysClientState.stamina = stamina;
                SysClientState.book = book;
                SysClientState.autocast = autocast;
                SysClientState.vengeance = veng;
                SysClientState.thrall = thrall;
                SysClientState.specCost = specCost;
                SysClientState.received = true;
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(SysNet.SCREEN, (client, handler, buf, sender) -> {
            String id = buf.readString();
            String title = buf.readString();
            int nl = buf.readVarInt();
            List<SystemsScreen.Line> lines = new ArrayList<>();
            for (int i = 0; i < nl; i++) {
                lines.add(new SystemsScreen.Line(buf.readString(), buf.readInt()));
            }
            int nf = buf.readVarInt();
            List<SystemsScreen.Field> fields = new ArrayList<>();
            for (int i = 0; i < nf; i++) {
                fields.add(new SystemsScreen.Field(buf.readString(), buf.readString(), buf.readString()));
            }
            int nb = buf.readVarInt();
            List<SystemsScreen.Btn> buttons = new ArrayList<>();
            for (int i = 0; i < nb; i++) {
                buttons.add(new SystemsScreen.Btn(buf.readString(), buf.readString(), buf.readString(), buf.readBoolean()));
            }
            client.execute(() -> client.setScreen(new SystemsScreen(id, title, lines, fields, buttons)));
        });
    }

    static void sendKey(int key) {
        if (MinecraftClient.getInstance().getNetworkHandler() == null) {
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(key);
        ClientPlayNetworking.send(SysNet.KEY, buf);
    }
}
