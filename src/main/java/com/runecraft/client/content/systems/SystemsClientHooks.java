package com.runecraft.client.content.systems;

import com.mojang.blaze3d.platform.InputConstants;
import com.runecraft.client.RuneCraftClient;
import com.runecraft.client.content.ClientModuleHooks;
import com.runecraft.content.systems.SysNet;
import com.runecraft.item.ItemCharges;
import com.runecraft.osrs.content.systems.Charges;
import com.runecraft.platform.client.ItemTooltipCallback;
import com.runecraft.platform.client.ClientTickEvents;
import com.runecraft.platform.client.KeyBindingHelper;
import com.runecraft.platform.client.ClientPlayNetworking;
import com.runecraft.platform.client.HudRenderCallback;
import com.runecraft.platform.PacketByteBufs;
import com.runecraft.util.StackData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Client side of the systems module: keys (special attack, spellbook, adventurer's log), HUD and screens. */
public final class SystemsClientHooks implements ClientModuleHooks {
    public static KeyMapping specKey;
    public static KeyMapping spellbookKey;
    public static KeyMapping hubKey;

    @Override
    public void initClient() {
        specKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("RuneCraft Special Attack", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V, "key.categories.runecraft"));
        spellbookKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("RuneCraft Spellbook", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B, "key.categories.runecraft"));
        hubKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("RuneCraft Adventurer's Log", InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(), "key.categories.runecraft"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (specKey.consumeClick()) {
                sendKey(SysNet.KEY_SPEC);
            }
            while (spellbookKey.consumeClick()) {
                sendKey(SysNet.KEY_SPELLBOOK);
            }
            while (hubKey.consumeClick()) {
                // One action per key: on the full interface's exact binding (options saved with both on J), only the
                // interface opens.
                KeyMapping ui = RuneCraftClient.interfaceKey;
                if (!hubKey.getKey().equals(ui.getKey()) || hubKey.getKeyModifier() != ui.getKeyModifier()) {
                    sendKey(SysNet.KEY_HUB);
                }
            }
        });
        HudRenderCallback.EVENT.register(SystemsHud::render);
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!id.getNamespace().equals("runecraft")) {
                return;
            }
            Charges.Spec spec = Charges.of(id.getPath());
            if (spec == null) {
                return;
            }
            CompoundTag nbt = StackData.get(stack);
            int charges = nbt != null && nbt.contains(ItemCharges.KEY) ? Charges.clamp(spec, nbt.getInt(ItemCharges.KEY)) : spec.initial();
            ChatFormatting color = Charges.depleted(spec, charges) ? ChatFormatting.RED
                    : charges * 4 < spec.max() ? ChatFormatting.GOLD : ChatFormatting.DARK_AQUA;
            lines.add(Component.literal(Charges.describe(spec, charges)).withStyle(color));
        });

        ClientPlayNetworking.registerGlobalReceiver(SysNet.STATE, (client, handler, buf, sender) -> {
            int spec = buf.readVarInt();
            boolean armed = buf.readBoolean();
            int run = buf.readVarInt();
            boolean stamina = buf.readBoolean();
            String book = buf.readUtf();
            String autocast = buf.readUtf();
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
            String id = buf.readUtf();
            String title = buf.readUtf();
            int nl = buf.readVarInt();
            List<SystemsScreen.Line> lines = new ArrayList<>();
            for (int i = 0; i < nl; i++) {
                lines.add(new SystemsScreen.Line(buf.readUtf(), buf.readInt()));
            }
            int nf = buf.readVarInt();
            List<SystemsScreen.Field> fields = new ArrayList<>();
            for (int i = 0; i < nf; i++) {
                fields.add(new SystemsScreen.Field(buf.readUtf(), buf.readUtf(), buf.readUtf()));
            }
            int nb = buf.readVarInt();
            List<SystemsScreen.Btn> buttons = new ArrayList<>();
            for (int i = 0; i < nb; i++) {
                buttons.add(new SystemsScreen.Btn(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readBoolean()));
            }
            client.execute(() -> client.setScreen(new SystemsScreen(id, title, lines, fields, buttons)));
        });
    }

    static void sendKey(int key) {
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(key);
        ClientPlayNetworking.send(SysNet.KEY, buf);
    }
}
