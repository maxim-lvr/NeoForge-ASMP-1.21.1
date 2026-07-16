package net.maximlvr.asmpthings.block.custom;

import net.maximlvr.asmpthings.block.entity.SkeleteShopTestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SkeleteShopTestBlock extends HorizontalFacingBlock implements EntityBlock {
    public SkeleteShopTestBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SkeleteShopTestBlockEntity(pos, state);
    }
}
