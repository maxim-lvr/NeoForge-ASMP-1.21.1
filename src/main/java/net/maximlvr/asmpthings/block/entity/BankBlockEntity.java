package net.maximlvr.asmpthings.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BankBlockEntity extends BlockEntity {
    public BankBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.BANK.get(), pos, blockState);
    }
}
