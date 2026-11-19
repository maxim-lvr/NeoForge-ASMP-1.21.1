package net.maximlvr.asmpthings.block.entity;

import net.maximlvr.asmpthings.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class VoidWindowBlockEntity extends TheEndPortalBlockEntity {
    public VoidWindowBlockEntity(BlockPos pos, BlockState state) {
        super(state.is(ModBlocks.MOUNTAIN_WINDOW.get()) ? ModBlockEntities.MOUNTAIN_WINDOW.get()
                : ModBlockEntities.VOID_WINDOW.get(), pos, state);
    }

    @Override public boolean shouldRenderFace(Direction face) {
        return level == null || !level.getBlockState(worldPosition.relative(face)).is(getBlockState().getBlock());
    }
}
