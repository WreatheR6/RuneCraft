package com.runecraft.item;

import com.runecraft.osrs.ItemDef;
import com.runecraft.skill.ItemUses;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Generic catalog item; right-click behaviour (eat, bury, drink, clean, open menus...) lives in {@link ItemUses}. */
public class OsrsItem extends Item implements OsrsItemLike {
    private final ItemDef def;

    public OsrsItem(ItemDef def, Properties settings) {
        super(settings);
        this.def = def;
    }

    @Override
    public ItemDef def() {
        return def;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (!ItemUses.hasUse(def)) {
            return InteractionResultHolder.pass(stack);
        }
        if (!world.isClientSide() && user instanceof ServerPlayer player) {
            ItemUses.use(player, stack, hand, def);
        }
        return InteractionResultHolder.sidedSuccess(user.getItemInHand(hand), world.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (def.kind() == ItemDef.Kind.TRAP) {
            if (!context.getLevel().isClientSide() && context.getPlayer() instanceof ServerPlayer player) {
                ItemUses.layTrap(player, context.getItemInHand(), def, context.getClickedPos(), context.getClickedFace());
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
        }
        return super.useOn(context);
    }
}
