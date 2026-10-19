package com.runecraft.content.skilling;

import com.runecraft.osrs.content.skilling.RunecraftData;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.util.StackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Essence pouch decay and repair. A pouch counts the essence stored in it since its last repair (NBT
 * {@value #USED_NBT}); {@link RunecraftData#capacity} turns that into its current capacity. The Dark Mage repairs
 * every pouch the player carries for free, talked to directly or through the Lunar spell NPC Contact.
 */
public final class EssencePouches {
    /** NBT key on a pouch stack: essence stored since the last repair. */
    public static final String USED_NBT = "EssenceUsed";

    private EssencePouches() {
    }

    /** Repairs every essence pouch in the inventory; returns how many were degraded. */
    public static int repairAll(ServerPlayer player) {
        int repaired = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            RunecraftData.Pouch p = s.isEmpty() ? null : RunecraftData.pouch(Inv.idOf(s));
            CompoundTag nbt = p == null ? null : StackData.get(s);
            if (nbt == null || !nbt.contains(USED_NBT)) {
                continue;
            }
            if (RunecraftData.degraded(p, nbt.getInt(USED_NBT))) {
                repaired++;
            }
            StackData.update(s, t -> t.putInt(USED_NBT, 0));
        }
        return repaired;
    }

    /** The Dark Mage's repair, with his lines. */
    public static void darkMage(ServerPlayer player) {
        int n = repairAll(player);
        if (n > 0) {
            Msg.game(player, "Dark Mage: Fine. I'll repair your pouches. There. Now leave me alone; I'm concentrating.");
            Msg.action(player, "The Dark Mage repairs " + (n == 1 ? "your pouch." : "your " + n + " pouches."));
        } else {
            Msg.game(player, "Dark Mage: Your pouches don't need repairing. Now go away, I'm keeping the Abyss stable.");
        }
    }
}
