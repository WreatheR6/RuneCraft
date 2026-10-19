package com.runecraft.client.bank;

import com.runecraft.bank.BankService;
import com.runecraft.osrs.bank.BankModel;
import com.runecraft.osrs.bank.BankQuantity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

/** Client side of the bank: the last state the server sent, and requests back to it. */
public final class BankClient {
    /** The bank as last sent by the server (display order). */
    public static final BankModel BANK = new BankModel();
    public static boolean depositOnly;
    public static long value;

    private BankClient() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(BankService.STATE, (client, handler, buf, sender) -> {
            boolean open = buf.readBoolean();
            boolean deposit = buf.readBoolean();
            boolean noted = buf.readBoolean();
            boolean placeholders = buf.readBoolean();
            BankQuantity.Mode mode = BankQuantity.Mode.byOrdinal(buf.readVarInt());
            long x = buf.readVarLong();
            long total = buf.readVarLong();
            int n = buf.readVarInt();
            String[] items = new String[n];
            String[] tags = new String[n];
            long[] counts = new long[n];
            int[] tabs = new int[n];
            for (int i = 0; i < n; i++) {
                items[i] = buf.readUtf();
                tags[i] = buf.readUtf();
                counts[i] = buf.readVarLong();
                tabs[i] = buf.readVarInt();
            }
            client.execute(() -> {
                BANK.clear();
                for (int i = 0; i < n; i++) {
                    BANK.load(items[i], tags[i], counts[i], tabs[i]);
                }
                BANK.noted = noted;
                BANK.placeholders = placeholders;
                BANK.quantity = mode;
                BANK.x = x;
                depositOnly = deposit;
                value = total;
                if (client.screen instanceof BankScreen screen) {
                    screen.refresh();
                } else if (open) {
                    client.setScreen(new BankScreen());
                }
            });
        });
        NoteModel.register();
    }

    /** Sends a bank request (see {@link BankService} OP_*). */
    public static void send(int op, String key, String key2, int slot, BankQuantity.Mode mode, long x, int tab) {
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(op);
        buf.writeUtf(key == null ? "" : key);
        buf.writeUtf(key2 == null ? "" : key2);
        buf.writeVarInt(slot);
        buf.writeVarInt(mode == null ? 0 : mode.ordinal());
        buf.writeVarLong(Math.max(0, x));
        buf.writeVarInt(Math.max(0, tab));
        ClientPlayNetworking.send(BankService.ACTION, buf);
    }
}
