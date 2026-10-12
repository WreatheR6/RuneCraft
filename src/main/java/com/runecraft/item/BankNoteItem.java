package com.runecraft.item;

import com.runecraft.bank.Notes;
import com.runecraft.osrs.ItemDef;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * A bank note: named after the item it stands for. It can't be eaten, worn or used in skilling (it's a different
 * item to everything that checks item ids); exchange it at a bank booth or banker.
 */
public class BankNoteItem extends OsrsItem {
    public BankNoteItem(ItemDef def, Settings settings) {
        super(def, settings);
    }

    @Override
    public Text getName(ItemStack stack) {
        ItemStack inner = Notes.unnoted(stack, 1);
        if (inner.isEmpty()) {
            return super.getName(stack);
        }
        return inner.getName();
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return TypedActionResult.pass(user.getStackInHand(hand));
    }
}
