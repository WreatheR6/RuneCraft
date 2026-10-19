package com.runecraft.client;

import com.runecraft.RuneCraft;
import com.runecraft.registry.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.item.Item;

/**
 * Coins show the OSRS stack sprite for their amount (1, 2, 3, 4, 5, 25, 100, 250, 1000, 10000): the item model
 * selects coins_&lt;n&gt; through the {@code runecraft:coin_stack} predicate (stage index / 10).
 */
public final class CoinStacks {
    private static final int[] STAGES = {1, 2, 3, 4, 5, 25, 100, 250, 1000, 10000};

    private CoinStacks() {
    }

    public static float stage(int count) {
        int idx = 0;
        for (int i = 0; i < STAGES.length; i++) {
            if (count >= STAGES[i]) {
                idx = i;
            }
        }
        return idx / 10f;
    }

    public static void register() {
        Item coins = ModItems.get("coins");
        if (coins == null) {
            return;
        }
        ItemProperties.register(coins, RuneCraft.id("coin_stack"),
                (stack, world, entity, seed) -> stage(stack.getCount()));
    }
}
