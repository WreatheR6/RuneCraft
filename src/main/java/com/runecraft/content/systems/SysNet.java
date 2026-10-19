package com.runecraft.content.systems;

import com.runecraft.RuneCraft;
import com.runecraft.osrs.content.systems.Spellbooks;
import com.runecraft.platform.PacketByteBufs;
import com.runecraft.platform.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Networking for the systems module. Screens are server-driven: the server sends a title, text lines,
 * input fields and buttons; the client shows them and sends back the clicked button with the field values.
 */
public final class SysNet {
    /** Server -> client: spec energy, run energy, spellbook... */
    public static final ResourceLocation STATE = RuneCraft.id("systems/state");
    /** Server -> client: a generic screen. */
    public static final ResourceLocation SCREEN = RuneCraft.id("systems/screen");
    /** Client -> server: a key press (see KEY_*). */
    public static final ResourceLocation KEY = RuneCraft.id("systems/key");
    /** Client -> server: a screen button was clicked. */
    public static final ResourceLocation ACTION = RuneCraft.id("systems/action");

    public static final int KEY_SPEC = 0;
    public static final int KEY_SPELLBOOK = 1;
    public static final int KEY_HUB = 2;
    public static final int KEY_WORN = 3;

    /** Handles a button click on a screen. */
    @FunctionalInterface
    public interface ActionHandler {
        void handle(ServerPlayer player, String action, Map<String, String> fields);
    }

    private static final Map<String, ActionHandler> HANDLERS = new HashMap<>();

    private SysNet() {
    }

    public static void onAction(String screen, ActionHandler handler) {
        HANDLERS.put(screen, handler);
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(KEY, (server, player, handler, buf, sender) -> {
            int key = buf.readVarInt();
            server.execute(() -> {
                switch (key) {
                    case KEY_SPEC -> SpecialAttackService.toggle(player);
                    case KEY_SPELLBOOK -> SysMenus.spellbook(player, 0);
                    case KEY_HUB -> SysMenus.hub(player);
                    case KEY_WORN -> SysMenus.worn(player);
                    default -> {
                    }
                }
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(ACTION, (server, player, handler, buf, sender) -> {
            String screen = buf.readUtf(64);
            String action = buf.readUtf(256);
            int n = Math.min(buf.readVarInt(), 16);
            Map<String, String> fields = new HashMap<>();
            for (int i = 0; i < n; i++) {
                fields.put(buf.readUtf(64), buf.readUtf(256));
            }
            server.execute(() -> {
                ActionHandler h = HANDLERS.get(screen);
                if (h != null) {
                    try {
                        h.handle(player, action, fields);
                    } catch (RuntimeException e) {
                        RuneCraft.LOGGER.error("Systems screen action {}/{} failed", screen, action, e);
                    }
                }
            });
        });
    }

    /** Sends the HUD state (special attack, run energy, spellbook, autocast). */
    public static void sendState(ServerPlayer player) {
        SysState.SysPlayer p = SysState.of(player);
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(p.specEnergy);
        buf.writeBoolean(p.specArmed);
        buf.writeVarInt(p.runEnergy);
        buf.writeBoolean(p.staminaUntil > com.runecraft.action.ActionManager.gameTick());
        buf.writeUtf(p.book.displayName);
        Spellbooks.Spell auto = p.autocast.isEmpty() ? null : Spellbooks.get(p.autocast);
        buf.writeUtf(auto == null ? "" : auto.name());
        buf.writeBoolean(p.vengeance);
        buf.writeVarInt(p.thrallTier);
        String weapon = com.runecraft.util.Inv.idOf(player.getMainHandItem());
        com.runecraft.osrs.content.systems.SpecialAttacks.Spec spec = com.runecraft.osrs.content.systems.SpecialAttacks.of(weapon);
        buf.writeVarInt(spec == null ? -1 : SpecialAttackService.cost(player, spec));
        ServerPlayNetworking.send(player, STATE, buf);
    }

    /** A server-built screen. */
    public static final class Screen {
        private final String id;
        private final String title;
        private final List<String[]> lines = new ArrayList<>();
        private final List<Object[]> buttons = new ArrayList<>();
        private final List<String[]> fields = new ArrayList<>();

        public Screen(String id, String title) {
            this.id = id;
            this.title = title;
        }

        public Screen line(String text) {
            return line(text, 0xFFFFFF);
        }

        public Screen line(String text, int color) {
            lines.add(new String[]{text, Integer.toString(color)});
            return this;
        }

        public Screen button(String action, String label) {
            return button(action, label, "", true);
        }

        public Screen button(String action, String label, String tooltip, boolean enabled) {
            buttons.add(new Object[]{action, label, tooltip == null ? "" : tooltip, enabled});
            return this;
        }

        public Screen field(String key, String label, String value) {
            fields.add(new String[]{key, label, value == null ? "" : value});
            return this;
        }

        public void send(ServerPlayer player) {
            FriendlyByteBuf buf = PacketByteBufs.create();
            buf.writeUtf(id);
            buf.writeUtf(title);
            buf.writeVarInt(lines.size());
            for (String[] l : lines) {
                buf.writeUtf(l[0].length() > 300 ? l[0].substring(0, 300) : l[0]);
                buf.writeInt(Integer.parseInt(l[1]));
            }
            buf.writeVarInt(fields.size());
            for (String[] f : fields) {
                buf.writeUtf(f[0]);
                buf.writeUtf(f[1]);
                buf.writeUtf(f[2]);
            }
            buf.writeVarInt(buttons.size());
            for (Object[] b : buttons) {
                buf.writeUtf((String) b[0]);
                buf.writeUtf((String) b[1]);
                buf.writeUtf((String) b[2]);
                buf.writeBoolean((Boolean) b[3]);
            }
            ServerPlayNetworking.send(player, SCREEN, buf);
        }
    }
}
