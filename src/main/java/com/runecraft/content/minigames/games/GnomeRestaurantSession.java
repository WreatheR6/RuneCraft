package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameItems;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Gnome Restaurant: Aluft Gianne snr. hands out delivery jobs. Cook the ordered gnome dish at the
 * gnome oven with the ingredients he provides, then deliver it to the customer's door before time
 * runs out. Easy jobs (Cooking 29) and hard jobs (Cooking 42+) pay reward tokens, coins and
 * Cooking experience; a missed delivery ends your shift.
 */
public class GnomeRestaurantSession extends MinigameSession {
    private String order;
    private boolean hard;
    private long deadline;
    private int delivered;

    public GnomeRestaurantSession(int id, int slot, ServerWorld world, String variant) {
        super(Minigame.GNOME_RESTAURANT, id, slot, world, variant);
    }

    @Override
    protected int halfX() {
        return 14;
    }

    @Override
    protected int halfZ() {
        return 14;
    }

    @Override
    protected void build() {
        room(Blocks.OAK_PLANKS, Blocks.OAK_LOG, Blocks.MOSS_BLOCK);
        place(-6, -6, "gnome_oven");
        place(10, -10, "gnome_customer");
        place(0, 13, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayerEntity player, int index) {
        return rel(index % 3 - 1, 1, 10);
    }

    @Override
    protected void start() {
        newOrder();
    }

    private void newOrder() {
        Object[] dish = MinigameItems.GNOME_DISHES[random.nextInt(MinigameItems.GNOME_DISHES.length)];
        order = (String) dish[0];
        ServerPlayerEntity first = online().isEmpty() ? null : online().get(0);
        hard = first != null && level(first, Skill.COOKING) >= 42 && random.nextBoolean();
        deadline = ticks + (hard ? 1100 : 600);
        for (ServerPlayerEntity p : online()) {
            Inv.give(p, "gnome_ingredients", 1);
            Msg.game(p, "Aluft Gianne snr.: I need a " + ItemCatalog.get(order).name() + " delivered" + (hard ? " across Gielinor" : "")
                    + "! Cook it in the gnome oven and take it to the customer.");
        }
    }

    @Override
    protected boolean onBlock(ServerPlayerEntity player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "gnome_oven" -> {
                if (Inv.remove(player, "gnome_ingredients", 1) == 0) {
                    Msg.game(player, "You need gnome ingredients from Aluft.");
                    return true;
                }
                Inv.give(player, order, 1);
                xp(player, Skill.COOKING, hard ? 80 : 50);
                Msg.game(player, "You cook a " + ItemCatalog.get(order).name() + ".");
                return true;
            }
            case "gnome_customer" -> {
                if (Inv.remove(player, order, 1) == 0) {
                    Msg.game(player, "The customer is still waiting for their " + ItemCatalog.get(order).name() + ".");
                    return true;
                }
                delivered++;
                reward(player, "gnome_restaurant_token", hard ? 2 : 1);
                reward(player, "coins", hard ? 400 + random.nextInt(400) : 150 + random.nextInt(200));
                xp(player, Skill.COOKING, hard ? 200 : 100);
                Msg.game(player, "The customer is delighted with their food!");
                newOrder();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    protected void gameTick() {
        if (ticks > deadline) {
            broadcastRed("You took too long! The customer has gone elsewhere.");
            end(false);
        }
    }

    @Override
    protected void onLeave(ServerPlayerEntity player) {
        Inv.remove(player, "gnome_ingredients", 1000);
    }

    @Override
    protected void onEnd(boolean success) {
        for (ServerPlayerEntity p : online()) {
            MinigameData.of(p).setStat(p.getUuid(), "kc:" + game.id(), MinigameData.of(p).stat(p.getUuid(), "kc:" + game.id()) + delivered);
        }
    }

    @Override
    protected List<String> hud(ServerPlayerEntity player) {
        return List.of("Gnome Restaurant", "Order: " + (order == null ? "-" : ItemCatalog.get(order).name()) + (hard ? " (hard)" : " (easy)"),
                "Time left: " + time(deadline - ticks), "Deliveries this shift: " + delivered);
    }
}
