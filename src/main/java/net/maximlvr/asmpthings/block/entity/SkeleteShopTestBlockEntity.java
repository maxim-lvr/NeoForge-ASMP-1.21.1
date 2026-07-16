package net.maximlvr.asmpthings.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SkeleteShopTestBlockEntity extends BlockEntity {
    public SkeleteShopTestBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.SKELETE_SHOP_TEST.get(), pos, blockState);
    }
}
