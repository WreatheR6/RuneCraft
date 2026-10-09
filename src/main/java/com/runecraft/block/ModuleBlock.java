package com.runecraft.block;

import com.runecraft.content.BlockHooks;
import com.runecraft.menu.MenuService;
import com.runecraft.osrs.content.BlockDef;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** A block defined by a content module's {@link BlockDef}. */
public class ModuleBlock extends Block {
    private final BlockDef def;

    public ModuleBlock(BlockDef def, Settings settings) {
        super(settings);
        this.def = def;
    }

    public BlockDef def() {
        return def;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND || def.kind() == BlockDef.Kind.DECOR) {
            return ActionResult.PASS;
        }
        if (!world.isClient() && player instanceof ServerPlayerEntity sp) {
            if (def.kind() == BlockDef.Kind.MENU) {
                MenuService.open(sp, def.menu(), pos, def.look() != null && def.look().contains("range"));
            } else {
                BlockHooks.use(sp, def.id(), pos, state);
            }
        }
        return ActionResult.success(world.isClient());
    }
}
