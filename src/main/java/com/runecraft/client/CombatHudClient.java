package com.runecraft.client;

import com.runecraft.network.HudNet;
import com.runecraft.network.HudProbe;
import com.runecraft.platform.client.ClientPlayConnectionEvents;
import com.runecraft.platform.client.ClientPlayNetworking;
import com.runecraft.platform.client.ClientTickEvents;
import com.runecraft.platform.client.HudRenderCallback;
import com.runecraft.platform.client.WorldRenderEvents;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/**
 * Round 42 combat HUD, client side ({@code // playability}): wires the telegraph cast bar ({@link CastBarHud}), the opponent
 * info box, outline and auto-retaliate hint ({@link OpponentHud}), the curse map and status packets ({@link HudNet}), and
 * publishes what the HUD shows to {@link HudProbe} once a client tick for the real-player self-tests.
 */
public final class CombatHudClient {
    private static long clientTick;
    /** Whether the systems module's "Special ready" cue is showing (set by its client hooks; false without them). */
    public static BooleanSupplier specialReady = () -> false;

    private CombatHudClient() {
    }

    /** Client ticks since start (the cast bar's clock). */
    static long clientTick() {
        return clientTick;
    }

    public static void init() {
        CastBarHud.init();
        ClientPlayNetworking.registerGlobalReceiver(HudNet.CURSE, (client, handler, buf, sender) -> {
            int[] curse = new int[ClientState.CURSE.length];
            for (int i = 0; i < curse.length; i++) {
                int c = buf.isReadable() ? buf.readByte() : i;
                curse[i] = c >= 0 && c < curse.length ? c : i;
            }
            client.execute(() -> System.arraycopy(curse, 0, ClientState.CURSE, 0, curse.length));
        });
        ClientPlayNetworking.registerGlobalReceiver(HudNet.STATUS, (client, handler, buf, sender) -> {
            int poison = buf.readByte();
            int retaliate = buf.readVarInt();
            client.execute(() -> {
                ClientState.poison = poison;
                ClientState.retaliateTarget = retaliate;
            });
        });
        ClientTickEvents.END_CLIENT_TICK.register(CombatHudClient::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        HudRenderCallback.EVENT.register(CastBarHud::render);
        HudRenderCallback.EVENT.register(OpponentHud::render);
        WorldRenderEvents.AFTER_ENTITIES.register(OpponentHud::renderWorld);
        // The player's own attack (a click or hold-to-attack) makes the creature the opponent at once.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, AttackEntityEvent.class, e -> {
            if (e.getEntity().level().isClientSide() && e.getEntity() == Minecraft.getInstance().player) {
                OpponentHud.onAttack(e.getTarget());
            }
        });
    }

    private static void reset() {
        CastBarHud.reset();
        OpponentHud.reset();
        ClientState.poison = 0;
        ClientState.retaliateTarget = -1;
        for (int i = 0; i < ClientState.CURSE.length; i++) {
            ClientState.CURSE[i] = i;
        }
        HudProbe.latest = null;
    }

    private static void tick(Minecraft mc) {
        clientTick++;
        if (mc.player == null || mc.level == null) {
            return;
        }
        CastBarHud.tick(mc);
        publish(mc);
    }

    /** The probe snapshot of this tick (read by real-player self-tests on the integrated server). */
    private static void publish(Minecraft mc) {
        LivingEntity opp = OpponentHud.current(mc);
        int[] hp = opp == null ? new int[]{0, 0} : OpponentHud.hitpoints(opp);
        LivingEntity ret = OpponentHud.retaliating(mc);
        String[] keys = {CombatKeys.keyName(0), CombatKeys.keyName(1), CombatKeys.keyName(2)};
        HudProbe.latest = new HudProbe.Snapshot(java.util.List.copyOf(CastBarHud.probe(mc)), opp == null ? -1 : opp.getId(),
                opp == null ? "" : opp.getName().getString(), opp == null ? 0 : HudOverlay.combatLevel(opp, true), hp[0], hp[1],
                ret == null ? "" : "Auto-retaliating: " + ret.getName().getString(), ClientState.poison, ClientState.CURSE.clone(),
                keys, specialReady.getAsBoolean(), held(mc), clientTick);
    }

    private static String held(Minecraft mc) {
        net.minecraft.world.item.ItemStack stack = mc.player.getMainHandItem();
        return stack.isEmpty() ? "" : net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }
}
