package com.runecraft.item;

import com.runecraft.osrs.ItemDef;
import com.runecraft.skill.ItemUses;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Generic catalog item; right-click behaviour (eat, bury, drink, clean, open menus...) lives in {@link ItemUses}. */
public class OsrsItem extends Item implements OsrsItemLike {
    private final ItemDef def;

    public OsrsItem(ItemDef def, Settings settings) {
        super(settings);
        this.def = def;
    }

    @Override
    public ItemDef def() {
        return def;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!ItemUses.hasUse(def)) {
            return TypedActionResult.pass(stack);
        }
        if (!world.isClient() && user instanceof ServerPlayerEntity player) {
            ItemUses.use(player, stack, hand, def);
        }
        return TypedActionResult.success(user.getStackInHand(hand), world.isClient());
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (def.kind() == ItemDef.Kind.TRAP) {
            if (!context.getWorld().isClient() && context.getPlayer() instanceof ServerPlayerEntity player) {
                ItemUses.layTrap(player, context.getStack(), def, context.getBlockPos(), context.getSide());
            }
            return ActionResult.success(context.getWorld().isClient());
        }
        return super.useOnBlock(context);
    }
}
