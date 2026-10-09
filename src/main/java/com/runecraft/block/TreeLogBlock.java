package com.runecraft.block;

import com.runecraft.osrs.data.Tree;
import com.runecraft.skill.Gathering;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TreeLogBlock extends ResourceBlock {
    private final Tree tree;

    public TreeLogBlock(Tree tree, Settings settings) {
        super(settings);
        this.tree = tree;
    }

    public Tree tree() {
        return tree;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }
        if (!world.isClient() && player instanceof ServerPlayerEntity sp) {
            Gathering.woodcut(sp, pos, tree);
        }
        return ActionResult.success(world.isClient());
    }
}
