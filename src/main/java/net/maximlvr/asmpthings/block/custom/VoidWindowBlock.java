package net.maximlvr.asmpthings.block.custom;

import net.maximlvr.asmpthings.block.entity.VoidWindowBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A solid decorative cube whose faces display the sky. */
public final class VoidWindowBlock extends Block implements EntityBlock {
    public VoidWindowBlock(Properties properties) {
        super(properties);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VoidWindowBlockEntity(pos, state);
    }

    @Override protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
}
