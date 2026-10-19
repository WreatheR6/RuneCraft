package com.runecraft.block;

import com.runecraft.content.BlockHooks;
import com.runecraft.menu.MenuService;
import com.runecraft.osrs.content.BlockDef;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A block defined by a content module's {@link BlockDef}. */
public class ModuleBlock extends Block {
    private final BlockDef def;

    public ModuleBlock(BlockDef def, Properties settings) {
        super(settings);
        this.def = def;
    }

    public BlockDef def() {
        return def;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || def.kind() == BlockDef.Kind.DECOR) {
            return InteractionResult.PASS;
        }
        if (!world.isClientSide() && player instanceof ServerPlayer sp) {
            if (def.kind() == BlockDef.Kind.MENU) {
                MenuService.open(sp, def.menu(), pos, def.look() != null && def.look().contains("range"));
            } else {
                BlockHooks.use(sp, def.id(), pos, state);
            }
        }
        return InteractionResult.sidedSuccess(world.isClientSide());
    }
}
