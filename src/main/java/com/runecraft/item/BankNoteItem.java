package com.runecraft.item;

import com.runecraft.bank.Notes;
import com.runecraft.osrs.ItemDef;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A bank note: named after the item it stands for. It can't be eaten, worn or used in skilling (it's a different
 * item to everything that checks item ids); exchange it at a bank booth or banker.
 */
public class BankNoteItem extends OsrsItem {
    public BankNoteItem(ItemDef def, Properties settings) {
        super(def, settings);
    }

    @Override
    public Component getName(ItemStack stack) {
        ItemStack inner = Notes.unnoted(stack, 1);
        if (inner.isEmpty()) {
            return super.getName(stack);
        }
        return inner.getHoverName();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        return InteractionResultHolder.pass(user.getItemInHand(hand));
    }
}
