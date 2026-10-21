package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameItems;
import com.runecraft.osrs.content.minigames.MinigameRules;
import com.runecraft.osrs.content.minigames.MinigameRules.GnomeRestaurant;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

/**
 * Gnome Restaurant (OSRS Wiki; numbers in {@link GnomeRestaurant}). Gianne jnr. hands out a delivery: click for an easy
 * job (a customer around the Stronghold, 6 minutes), sneak-click for a hard one (anywhere in Gielinor, 11 minutes);
 * sneak-clicking again while a hard job is open declines it, and no hard job is offered for 5.5 minutes. The ordered dish
 * may be cooked in advance (gnome cooking) or baked at the gnome oven from the ingredients Gianne supplies, for the dish's
 * full Cooking XP; deliveries themselves give none. The customer's door takes the dish: an easy customer tips 2-500 coins,
 * a hard one an item (with their own specials). Easy jobs score 1 point, hard 3; every 12 points make a reward token, worth
 * a random gnome dish (at most 10 held). A job not delivered in time is lost.
 */
public class GnomeRestaurantSession extends MinigameSession {
    private String order;
    private String customer;
    private boolean hard;
    private long deadline = -1;
    private long noHardUntil;
    private int delivered;

    public GnomeRestaurantSession(int id, int slot, ServerLevel world, String variant) {
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
        place(0, 6, "gnome_gianne");
        place(10, -10, "gnome_customer");
        place(0, 13, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return rel(index % 3 - 1, 1, 10);
    }

    @Override
    protected void start() {
        broadcast("Gianne jnr.: Welcome to Aluft Aloft! Ask me for a delivery job (sneak for a hard one).");
    }

    private void newJob(ServerPlayer p, boolean wantHard) {
        if (order != null) {
            if (wantHard && hard) {
                order = null;
                deadline = -1;
                noHardUntil = ticks + GnomeRestaurant.HARD_DECLINE_TICKS;
                Inv.remove(p, "gnome_ingredients", Inv.count(p, "gnome_ingredients"));
                Msg.game(p, "Gianne jnr.: Fine, I'll find someone else. No hard jobs for you for a while.");
            } else {
                Msg.game(p, "Gianne jnr.: You still have a " + dishName() + " to deliver to " + customer + ".");
            }
            return;
        }
        if (wantHard && ticks < noHardUntil) {
            Msg.game(p, "Gianne jnr.: You turned down a hard job; come back in " + time(noHardUntil - ticks) + ".");
            return;
        }
        Object[] dish = MinigameItems.GNOME_DISHES[random.nextInt(MinigameItems.GNOME_DISHES.length)];
        order = (String) dish[0];
        hard = wantHard;
        List<String> customers = hard ? GnomeRestaurant.HARD_CUSTOMERS : GnomeRestaurant.EASY_CUSTOMERS;
        customer = customers.get(random.nextInt(customers.size()));
        deadline = ticks + (hard ? GnomeRestaurant.HARD_TICKS : GnomeRestaurant.EASY_TICKS);
        if (!Inv.has(p, "gnome_ingredients", 1)) {
            Inv.give(p, "gnome_ingredients", 1);
        }
        Msg.game(p, "Gianne jnr.: " + customer + " wants " + Msg.article(dishName().toLowerCase(java.util.Locale.ROOT))
                + " within " + time(deadline - ticks) + ". Here are the ingredients if you need to make it.");
    }

    private String dishName() {
        return order == null ? "-" : ItemCatalog.get(order).name();
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        switch (blockId) {
            case "gnome_gianne" -> newJob(player, player.isShiftKeyDown());
            case "gnome_oven" -> {
                if (order == null) {
                    Msg.game(player, "You have no order to cook for.");
                } else if (Inv.remove(player, "gnome_ingredients", 1) == 0) {
                    Msg.game(player, "You need the ingredients Gianne gave you.");
                } else {
                    Inv.give(player, order, 1);
                    xp(player, Skill.COOKING, GnomeRestaurant.dishXp(order));
                    Msg.game(player, "You bake the " + dishName().toLowerCase(java.util.Locale.ROOT) + " and add the toppings.");
                }
            }
            case "gnome_customer" -> deliver(player);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void deliver(ServerPlayer player) {
        if (order == null) {
            Msg.game(player, "Nobody here is waiting for food.");
            return;
        }
        if (Inv.remove(player, order, 1) == 0) {
            Msg.game(player, customer + ": Where's my " + dishName().toLowerCase(java.util.Locale.ROOT) + "?");
            return;
        }
        delivered++;
        String key = "gnome_restaurant";
        int before = MinigameData.of(player).points(player.getUUID(), key);
        int after = Math.min(GnomeRestaurant.MAX_POINTS, before + (hard ? GnomeRestaurant.HARD_POINTS : GnomeRestaurant.EASY_POINTS));
        MinigameData.of(player).addPoints(player.getUUID(), key, after - before);
        reward(player, "gnome_restaurant_token", GnomeRestaurant.tokens(before, after));
        if (hard) {
            MinigameRules.Loot tip = GnomeRestaurant.hardTip(customer, random::nextDouble);
            reward(player, tip.item(), tip.min() + random.nextInt(tip.max() - tip.min() + 1));
        } else {
            reward(player, "coins", GnomeRestaurant.TIP_MIN + random.nextInt(GnomeRestaurant.TIP_MAX - GnomeRestaurant.TIP_MIN + 1));
        }
        MinigameData.of(player).incrementStat(player.getUUID(), "kc:" + game.id());
        Inv.remove(player, "gnome_ingredients", Inv.count(player, "gnome_ingredients"));
        Msg.game(player, customer + ": Thank you! Here's a little something for your trouble.");
        order = null;
        deadline = -1;
    }

    @Override
    protected void gameTick() {
        if (order != null && ticks > deadline) {
            broadcastRed(customer + " got tired of waiting and found something else to eat. Ask Gianne for a new job.");
            order = null;
            deadline = -1;
        }
    }

    @Override
    protected void onLeave(ServerPlayer player) {
        Inv.remove(player, "gnome_ingredients", Inv.count(player, "gnome_ingredients"));
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        return List.of("Gnome Restaurant - " + MinigameData.of(player).points(player.getUUID(), "gnome_restaurant") + " points",
                order == null ? "Ask Gianne jnr. for a job" : (hard ? "Hard: " : "Easy: ") + dishName() + " for " + customer,
                order == null ? "" : "Time left: " + time(deadline - ticks), "Deliveries this visit: " + delivered);
    }

    // ------------------------------------------------------------------ self-test hooks

    public record View(String order, String customer, boolean hard, long ticksLeft) {
    }

    public View selfTestView() {
        return new View(order, customer, hard, deadline - ticks);
    }

    /** Self-test: runs the clock to just before the open job's deadline. */
    public void selfTestNearDeadline() {
        if (deadline > 0) {
            ticks = Math.max(ticks, deadline - 2);
        }
    }

    public BlockPos selfTestBlock(String id) {
        return switch (id) {
            case "gnome_oven" -> rel(-6, 1, -6);
            case "gnome_gianne" -> rel(0, 1, 6);
            case "gnome_customer" -> rel(10, 1, -10);
            default -> rel(0, 1, 13);
        };
    }
}
